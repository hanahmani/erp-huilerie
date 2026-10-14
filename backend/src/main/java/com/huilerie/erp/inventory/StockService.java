package com.huilerie.erp.inventory;

import com.huilerie.erp.audit.AuditService;
import com.huilerie.erp.catalog.Article;
import com.huilerie.erp.catalog.ArticleRepository;
import com.huilerie.erp.catalog.ArticleType;
import com.huilerie.erp.clients.Client;
import com.huilerie.erp.clients.ClientRepository;
import com.huilerie.erp.common.Money;
import com.huilerie.erp.common.SecurityUtils;
import com.huilerie.erp.common.Text;
import com.huilerie.erp.common.exception.BusinessException;
import com.huilerie.erp.common.exception.NotFoundException;
import com.huilerie.erp.common.response.PageResponse;
import com.huilerie.erp.inventory.dto.StockDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;

@Service
public class StockService {

    private final StockBalanceRepository balances;
    private final StockMovementRepository movements;
    private final StockLotRepository lots;
    private final ArticleRepository articles;
    private final ClientRepository clients;
    private final AuditService audit;

    public StockService(StockBalanceRepository balances, StockMovementRepository movements, StockLotRepository lots,
                        ArticleRepository articles, ClientRepository clients, AuditService audit) {
        this.balances = balances;
        this.movements = movements;
        this.lots = lots;
        this.articles = articles;
        this.clients = clients;
        this.audit = audit;
    }

    // =============================================================== cœur : appliquer un mouvement

    /**
     * Enregistre un mouvement et met à jour la projection, dans la transaction de l'appelant.
     * La ligne de stock est verrouillée ; un stock négatif est refusé ici ET par le CHECK en base.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public StockMovement apply(Article article, StockLot lot, MovementType type, BigDecimal quantite, String motif,
                               String documentType, Long documentId) {
        BigDecimal qty = Money.strict(quantite, "Quantité");
        if (!Money.isPositive(qty)) {
            throw new BusinessException("La quantité doit être positive.");
        }
        if (lot != null && !lot.getArticle().getId().equals(article.getId())) {
            throw new BusinessException("Ce lot n'appartient pas à l'article choisi.");
        }
        if (article.isSuiviLot() && lot == null) {
            throw new BusinessException("« " + article.getNom() + " » est suivi par lot : choisissez un lot.");
        }
        if (!article.isSuiviLot() && lot != null) {
            throw new BusinessException("« " + article.getNom() + " » n'est pas suivi par lot.");
        }

        long ref = StockBalance.refOf(lot);
        StockBalance balance = balances.lockFor(article.getId(), ref).orElse(null);
        if (type == MovementType.STOCK_INITIAL && balance != null) {
            throw new BusinessException("Le stock initial a déjà été saisi. Utilisez un ajustement d'inventaire.");
        }
        if (balance == null) {
            balances.saveAndFlush(new StockBalance(article, lot));
            balance = balances.lockFor(article.getId(), ref).orElseThrow();
        }

        BigDecimal next = balance.getQuantite().add(qty.multiply(BigDecimal.valueOf(type.sens())));
        if (next.signum() < 0) {
            throw new BusinessException("Stock insuffisant pour « " + article.getNom() + " »"
                    + (lot == null ? "" : " (lot " + lot.getNumero() + ")")
                    + " : disponible " + balance.getQuantite().toPlainString() + " " + article.getUnite().getCode() + ".");
        }
        balance.setQuantite(next);
        return movements.save(new StockMovement(article, lot, type, qty, Text.clean(motif), documentType, documentId,
                SecurityUtils.username()));
    }

    // =============================================================== mouvements manuels

    @PreAuthorize("hasAuthority('STOCK_ADJUST')")
    @Transactional
    public StockDto.MovementResponse manualMovement(StockDto.MovementRequest r) {
        if (!r.type().isManual()) {
            throw new BusinessException("Ce type de mouvement est créé automatiquement par les documents.");
        }
        Article article = articles.findById(r.articleId()).orElseThrow(() -> new NotFoundException("Article introuvable."));
        if (!article.isActif()) {
            throw new BusinessException("Cet article est désactivé.");
        }
        StockLot lot = r.lotId() == null ? null : lot(r.lotId());
        StockMovement m = apply(article, lot, r.type(), r.quantite(), r.motif().trim(), "MANUEL", null);
        audit.log(SecurityUtils.username(), "STOCK_" + r.type().name(), "StockMovement", String.valueOf(m.getId()),
                article.getReference() + " " + m.getQuantite().toPlainString() + " : " + r.motif().trim());
        return StockDto.MovementResponse.from(m);
    }

    @PreAuthorize("hasAuthority('STOCK_VIEW')")
    @Transactional(readOnly = true)
    public PageResponse<StockDto.MovementResponse> searchMovements(Long articleId, Long lotId, MovementType type,
                                                                   Instant from, Instant to, int page, int size) {
        PageRequest pr = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100),
                Sort.by(Sort.Direction.DESC, "id"));
        return PageResponse.of(movements.search(articleId, lotId, type, from, to, pr), StockDto.MovementResponse::from);
    }

    // =============================================================== état du stock

    @PreAuthorize("hasAuthority('STOCK_VIEW')")
    @Transactional(readOnly = true)
    public PageResponse<StockDto.ArticleStock> stockState(String q, ArticleType type, boolean alertOnly,
                                                          int page, int size) {
        return stockStateUnchecked(q, type, alertOnly, page, size);
    }

    /** Sans contrôle de permission : réservé aux services (rapports, tableau de bord) qui vérifient les leurs. */
    @Transactional(readOnly = true)
    public PageResponse<StockDto.ArticleStock> stockStateUnchecked(String q, ArticleType type, boolean alertOnly,
                                                                   int page, int size) {
        if (alertOnly) {
            List<StockDto.ArticleStock> alerts = lowStock();
            return new PageResponse<>(alerts, 0, alerts.size(), alerts.size(), 1);
        }
        PageRequest pr = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100), Sort.by("nom"));
        Page<Article> p = articles.search(Text.search(q), type, null, true, pr);
        Map<Long, List<StockBalance>> byArticle = group(p.getContent());
        return PageResponse.of(p, a -> summarize(a, byArticle.getOrDefault(a.getId(), List.of())));
    }

    /** Articles dont le stock disponible est sous le seuil minimal (calculé, jamais stocké). */
    @Transactional(readOnly = true)
    public List<StockDto.ArticleStock> lowStock() {
        List<Article> withThreshold = articles.findByActifTrueAndSeuilMinIsNotNull();
        Map<Long, List<StockBalance>> byArticle = group(withThreshold);
        return withThreshold.stream()
                .map(a -> summarize(a, byArticle.getOrDefault(a.getId(), List.of())))
                .filter(StockDto.ArticleStock::alerte)
                .sorted(Comparator.comparing(StockDto.ArticleStock::nom))
                .toList();
    }

    private Map<Long, List<StockBalance>> group(List<Article> list) {
        if (list.isEmpty()) {
            return Map.of();
        }
        Map<Long, List<StockBalance>> map = new HashMap<>();
        balances.findByArticleIdIn(list.stream().map(Article::getId).toList())
                .forEach(b -> map.computeIfAbsent(b.getArticle().getId(), k -> new ArrayList<>()).add(b));
        return map;
    }

    private StockDto.ArticleStock summarize(Article a, List<StockBalance> list) {
        BigDecimal dispo = Money.zero();
        BigDecimal bloque = Money.zero();
        BigDecimal clientsQty = Money.zero();
        List<StockDto.LotLine> lines = new ArrayList<>();
        for (StockBalance b : list) {
            StockLot l = b.getLot();
            if (l == null || l.isSellable()) {
                dispo = dispo.add(b.getQuantite());
            } else if (l.getProprietaire() == LotOwner.CLIENT) {
                clientsQty = clientsQty.add(b.getQuantite());
            } else {
                bloque = bloque.add(b.getQuantite());
            }
            if (l != null && b.getQuantite().signum() > 0) {
                lines.add(new StockDto.LotLine(l.getId(), l.getNumero(), l.getProprietaire(), l.getStatut(), b.getQuantite()));
            }
        }
        boolean alerte = a.getSeuilMin() != null && dispo.compareTo(a.getSeuilMin()) < 0;
        return new StockDto.ArticleStock(a.getId(), a.getReference(), a.getNom(), a.getUnite().getCode(),
                a.getType().name(), dispo, bloque, clientsQty, a.getSeuilMin(), alerte, lines);
    }

    // =============================================================== lots

    @PreAuthorize("hasAuthority('STOCK_VIEW')")
    @Transactional(readOnly = true)
    public PageResponse<StockDto.LotResponse> searchLots(String q, Long articleId, LotOwner owner, LotStatus statut,
                                                         int page, int size) {
        PageRequest pr = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100),
                Sort.by(Sort.Direction.DESC, "id"));
        return PageResponse.of(lots.search(Text.search(q), articleId, owner, statut, pr),
                l -> StockDto.LotResponse.from(l, Money.of(balances.quantityOfLot(l.getId()))));
    }

    @PreAuthorize("hasAuthority('STOCK_ADJUST')")
    @Transactional
    public StockDto.LotResponse createLot(StockDto.LotRequest r) {
        Article article = articles.findById(r.articleId()).orElseThrow(() -> new NotFoundException("Article introuvable."));
        if (!article.isSuiviLot()) {
            throw new BusinessException("Cet article n'est pas suivi par lot.");
        }
        String numero = r.numero().trim().toUpperCase();
        if (lots.existsByArticleIdAndNumeroIgnoreCase(article.getId(), numero)) {
            throw new BusinessException("Ce numéro de lot existe déjà pour cet article.");
        }
        Client client = null;
        if (r.proprietaire() == LotOwner.CLIENT) {
            if (r.clientId() == null) {
                throw new BusinessException("Choisissez le client propriétaire de cette huile.");
            }
            client = clients.findById(r.clientId()).orElseThrow(() -> new NotFoundException("Client introuvable."));
        } else if (r.clientId() != null) {
            throw new BusinessException("Un lot de l'entreprise ne peut pas avoir de client propriétaire.");
        }
        StockLot lot = lots.save(new StockLot(article, numero, r.proprietaire(), client));
        audit.log(SecurityUtils.username(), "LOT_CREATE", "StockLot", String.valueOf(lot.getId()), numero);
        return StockDto.LotResponse.from(lot, Money.zero());
    }

    @PreAuthorize("hasAuthority('STOCK_ADJUST')")
    @Transactional
    public StockDto.LotResponse setLotStatus(Long id, StockDto.LotStatusRequest r) {
        StockLot lot = lot(id);
        if (r.statut() == LotStatus.BLOQUE) {
            String motif = Text.clean(r.motif());
            if (motif == null) {
                throw new BusinessException("Indiquez le motif du blocage.");
            }
            lot.block(motif);
        } else {
            lot.unblock();
        }
        audit.log(SecurityUtils.username(), r.statut() == LotStatus.BLOQUE ? "LOT_BLOCK" : "LOT_UNBLOCK",
                "StockLot", String.valueOf(id), Text.clean(r.motif()));
        return StockDto.LotResponse.from(lot, Money.of(balances.quantityOfLot(id)));
    }

    public StockLot lot(Long id) {
        return lots.findById(id).orElseThrow(() -> new NotFoundException("Lot introuvable."));
    }
}

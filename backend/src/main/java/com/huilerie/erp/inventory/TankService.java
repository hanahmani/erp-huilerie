package com.huilerie.erp.inventory;

import com.huilerie.erp.audit.AuditService;
import com.huilerie.erp.catalog.Article;
import com.huilerie.erp.catalog.ArticleRepository;
import com.huilerie.erp.common.Money;
import com.huilerie.erp.common.SecurityUtils;
import com.huilerie.erp.common.exception.BusinessException;
import com.huilerie.erp.common.exception.NotFoundException;
import com.huilerie.erp.inventory.dto.TankDto;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/**
 * Puits, citernes et fûts de stockage d'huile. Chaque puits porte un lot (même numéro que le code) : ENTREPRISE pour un puits
 * de l'huilerie (ventes en caisse), CLIENT pour un puits réservé à un client (remises). La capacité n'est jamais dépassée.
 */
@Service
public class TankService {

    private final StorageTankRepository tanks;
    private final StockLotRepository lots;
    private final StockBalanceRepository balances;
    private final ArticleRepository articles;
    private final com.huilerie.erp.clients.ClientRepository clients;
    private final StockService stock;
    private final AuditService audit;

    public TankService(StorageTankRepository tanks, StockLotRepository lots, StockBalanceRepository balances,
                       ArticleRepository articles, com.huilerie.erp.clients.ClientRepository clients, StockService stock,
                       AuditService audit) {
        this.clients = clients;
        this.tanks = tanks;
        this.lots = lots;
        this.balances = balances;
        this.articles = articles;
        this.stock = stock;
        this.audit = audit;
    }

    @PreAuthorize("hasAuthority('STOCK_VIEW') or hasAuthority('PRESSING_VIEW')")
    @Transactional(readOnly = true)
    public List<TankDto.Response> list() {
        return tanks.findAllByOrderByCodeAsc().stream().map(this::toDto).toList();
    }

    @PreAuthorize("hasAuthority('STOCK_ADJUST')")
    @Transactional
    public TankDto.Response create(TankDto.Request r) {
        if (r.code() == null || r.code().isBlank()) {
            throw new BusinessException("Le code du contenant est obligatoire.");
        }
        if (r.articleId() == null) {
            throw new BusinessException("Choisissez l'huile stockée dans ce contenant.");
        }
        String code = r.code().trim().replaceAll("\\s+", " ").toUpperCase();
        if (tanks.existsByCodeIgnoreCase(code)) {
            throw new BusinessException("Un contenant porte déjà le code " + code + ".");
        }
        Article article = articles.findById(r.articleId()).orElseThrow(() -> new NotFoundException("Article introuvable."));
        if (!article.isActif()) {
            throw new BusinessException("Cet article est désactivé.");
        }
        if (!article.isSuiviLot()) {
            throw new BusinessException("L'huile d'un contenant doit être un article suivi par lot.");
        }
        if (lots.existsByArticleIdAndNumeroIgnoreCase(article.getId(), code)) {
            throw new BusinessException("Un lot " + code + " existe déjà pour cet article : choisissez un autre code.");
        }
        com.huilerie.erp.clients.Client client = r.clientId() == null ? null : activeClient(r.clientId());
        StockLot lot = lots.save(client == null ? new StockLot(article, code, LotOwner.ENTREPRISE, null)
                : new StockLot(article, code, LotOwner.CLIENT, client));
        StorageTank t = tanks.save(new StorageTank(code, r.nom().trim(), r.type(), article, client, lot,
                Money.strict(r.capacite(), "Capacité")));
        audit.log(SecurityUtils.username(), "TANK_CREATE", "StorageTank", String.valueOf(t.getId()),
                code + " " + t.getCapacite().toPlainString() + (client == null ? "" : " — client " + client.getNom()));
        return toDto(t);
    }

    @PreAuthorize("hasAuthority('STOCK_ADJUST')")
    @Transactional
    public TankDto.Response update(Long id, TankDto.Request r) {
        StorageTank t = tanks.lockById(id).orElseThrow(() -> new NotFoundException("Contenant introuvable."));
        BigDecimal capacite = Money.strict(r.capacite(), "Capacité");
        BigDecimal quantite = quantity(t);
        if (capacite.compareTo(quantite) < 0) {
            throw new BusinessException("Le contenant contient déjà " + quantite.toPlainString() + " : la capacité ne peut pas être inférieure.");
        }
        t.update(r.nom().trim(), r.type(), capacite);
        Long current = t.getClient() == null ? null : t.getClient().getId();
        if (!java.util.Objects.equals(current, r.clientId())) {
            changeOwner(t, r.clientId() == null ? null : activeClient(r.clientId()));
        }
        audit.log(SecurityUtils.username(), "TANK_UPDATE", "StorageTank", String.valueOf(id),
                t.getCode() + " " + capacite.toPlainString());
        return toDto(t);
    }

    @PreAuthorize("hasAuthority('STOCK_ADJUST')")
    @Transactional
    public TankDto.Response setStatus(Long id, boolean actif) {
        StorageTank t = tanks.findById(id).orElseThrow(() -> new NotFoundException("Contenant introuvable."));
        t.setActif(actif);
        audit.log(SecurityUtils.username(), actif ? "ACTIVATE" : "DEACTIVATE", "StorageTank", String.valueOf(id), t.getCode());
        return toDto(t);
    }

    /**
     * Transvasement d'un contenant de l'huilerie vers un autre (ex. le reste d'un fût versé dans une citerne) : même huile,
     * sans dépasser le contenu de l'un ni la place de l'autre.
     * Retourne les deux contenants mis à jour (source puis destination).
     */
    @PreAuthorize("hasAuthority('STOCK_ADJUST')")
    @Transactional
    public List<TankDto.Response> transfer(Long fromId, Long toId, BigDecimal quantite) {
        if (fromId.equals(toId)) {
            throw new BusinessException("Choisissez un autre contenant de destination.");
        }
        // Verrouillage dans l'ordre des identifiants pour éviter les interblocages.
        StorageTank first = tanks.lockById(Math.min(fromId, toId)).orElseThrow(() -> new NotFoundException("Contenant introuvable."));
        StorageTank second = tanks.lockById(Math.max(fromId, toId)).orElseThrow(() -> new NotFoundException("Contenant introuvable."));
        StorageTank from = first.getId().equals(fromId) ? first : second;
        StorageTank to = from == first ? second : first;
        if (!from.isActif() || !to.isActif()) {
            throw new BusinessException("Le contenant " + (from.isActif() ? to : from).getCode() + " est désactivé.");
        }
        if (!from.getArticle().getId().equals(to.getArticle().getId())) {
            throw new BusinessException(to.getCode() + " contient « " + to.getArticle().getNom() + " », pas « "
                    + from.getArticle().getNom() + " » : transvasement impossible.");
        }
        // L'huile d'un client est rattachée au contenant de son pressage (ses remises y sont prises) : elle ne bouge pas.
        if (from.getClient() != null || to.getClient() != null) {
            throw new BusinessException("Seule l'huile de l'huilerie se transvase : l'huile d'un client reste dans son contenant.");
        }
        BigDecimal q = Money.strict(quantite, "Quantité");
        String unite = from.getArticle().getUnite().getCode();
        BigDecimal contenu = quantity(from);
        if (q.compareTo(contenu) > 0) {
            throw new BusinessException(from.getCode() + " ne contient que " + contenu.toPlainString() + " " + unite + ".");
        }
        BigDecimal place = to.getCapacite().subtract(quantity(to));
        if (q.compareTo(place) > 0) {
            throw new BusinessException(to.getCode() + " est plein : place restante " + place.toPlainString() + " " + unite + ".");
        }
        String motif = "Transvasement " + from.getCode() + " → " + to.getCode();
        stock.apply(from.getArticle(), from.getLot(), MovementType.TRANSFERT_SORTIE, q, motif, "TRANSVASEMENT", from.getId());
        stock.apply(to.getArticle(), to.getLot(), MovementType.TRANSFERT_ENTREE, q, motif, "TRANSVASEMENT", to.getId());
        audit.log(SecurityUtils.username(), "TANK_TRANSFER", "StorageTank", String.valueOf(from.getId()),
                motif + " : " + q.toPlainString() + " " + unite);
        return List.of(toDto(from), toDto(to));
    }

    /**
     * Verse de l'huile dans un puits, dans la transaction de l'appelant. Le puits est verrouillé :
     * refus s'il est inactif ou si la quantité dépasse la place restante.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public StorageTank fill(Long tankId, BigDecimal quantite, MovementType type, String motif, String documentType,
                            Long documentId) {
        StorageTank t = tanks.lockById(tankId).orElseThrow(() -> new NotFoundException("Contenant introuvable."));
        if (!t.isActif()) {
            throw new BusinessException(t.getCode() + " est désactivé.");
        }
        BigDecimal place = t.getCapacite().subtract(quantity(t));
        if (quantite.compareTo(place) > 0) {
            throw new BusinessException(t.getCode() + " est plein : place restante "
                    + place.toPlainString() + " " + t.getArticle().getUnite().getCode() + ".");
        }
        stock.apply(t.getArticle(), t.getLot(), type, quantite, motif, documentType, documentId);
        return t;
    }

    /**
     * Réserve un contenant vide de l'huilerie au client, dans la transaction de l'appelant (fin d'un pressage à façon).
     * Déjà réservé à ce client : rien à faire. Retourne le contenant verrouillé.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public StorageTank reserveFor(Long tankId, com.huilerie.erp.clients.Client client) {
        StorageTank t = tanks.lockById(tankId).orElseThrow(() -> new NotFoundException("Contenant introuvable."));
        if (t.getClient() != null && t.getClient().getId().equals(client.getId())) {
            return t;
        }
        if (t.getClient() != null) {
            throw new BusinessException(t.getCode() + " est réservé à " + t.getClient().getNom() + ".");
        }
        if (!t.isActif()) {
            throw new BusinessException(t.getCode() + " est désactivé.");
        }
        changeOwner(t, client);
        return t;
    }

    /**
     * Contenant d'un client vidé par une remise : rendu à l'huilerie (nouveau lot ENTREPRISE), réutilisable pour un autre
     * client ou pour l'huile de l'huilerie. Sans effet s'il appartient déjà à l'huilerie ou contient encore de l'huile.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void releaseIfEmpty(Long tankId) {
        StorageTank t = tanks.lockById(tankId).orElseThrow(() -> new NotFoundException("Contenant introuvable."));
        if (t.getClient() != null && quantity(t).signum() == 0) {
            changeOwner(t, null);
        }
    }

    /**
     * Change le propriétaire d'un contenant vide (huilerie ↔ client, ou d'un client à un autre). Un nouveau lot est
     * ouvert pour le nouveau propriétaire : l'ancien lot, vide, garde l'historique de l'ancien propriétaire.
     */
    private void changeOwner(StorageTank t, com.huilerie.erp.clients.Client client) {
        BigDecimal contenu = quantity(t);
        if (contenu.signum() != 0) {
            throw new BusinessException(t.getCode() + " contient encore " + contenu.toPlainString() + " "
                    + t.getArticle().getUnite().getCode() + " : videz-le avant de changer de propriétaire.");
        }
        String numero = t.getCode();
        for (int i = 2; lots.existsByArticleIdAndNumeroIgnoreCase(t.getArticle().getId(), numero); i++) {
            numero = t.getCode() + "-" + i;
        }
        StockLot lot = lots.save(client == null ? new StockLot(t.getArticle(), numero, LotOwner.ENTREPRISE, null)
                : new StockLot(t.getArticle(), numero, LotOwner.CLIENT, client));
        String avant = t.getClient() == null ? "l'huilerie" : t.getClient().getNom();
        t.changeOwner(client, lot);
        tanks.flush();
        audit.log(SecurityUtils.username(), "TANK_OWNER", "StorageTank", String.valueOf(t.getId()),
                t.getCode() + " : " + avant + " → " + (client == null ? "l'huilerie" : client.getNom()) + " (lot " + numero + ")");
    }

    private com.huilerie.erp.clients.Client activeClient(Long clientId) {
        com.huilerie.erp.clients.Client c = clients.findById(clientId).orElseThrow(() -> new NotFoundException("Client introuvable."));
        if (!c.isActif()) {
            throw new BusinessException("Ce client est désactivé.");
        }
        return c;
    }

    private BigDecimal quantity(StorageTank t) {
        return Money.of(balances.quantityOfLot(t.getLot().getId()));
    }

    private TankDto.Response toDto(StorageTank t) {
        return TankDto.Response.from(t, quantity(t));
    }
}

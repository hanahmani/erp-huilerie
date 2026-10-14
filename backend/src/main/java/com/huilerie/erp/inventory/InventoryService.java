package com.huilerie.erp.inventory;

import com.huilerie.erp.audit.AuditService;
import com.huilerie.erp.catalog.Article;
import com.huilerie.erp.catalog.ArticleRepository;
import com.huilerie.erp.common.Money;
import com.huilerie.erp.common.SecurityUtils;
import com.huilerie.erp.common.exception.BusinessException;
import com.huilerie.erp.common.exception.NotFoundException;
import com.huilerie.erp.common.numbering.DocumentNumberService;
import com.huilerie.erp.common.response.PageResponse;
import com.huilerie.erp.inventory.dto.InventoryDto;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;

/**
 * Inventaire physique.
 * 1. Création : photographie du stock théorique par article et par lot.
 * 2. Saisie des quantités comptées.
 * 3. Validation : pour chaque ligne comptée, un mouvement INVENTAIRE_PLUS/MOINS ramène le stock
 *    au comptage. L'écart est recalculé sur le stock du moment, au cas où des ventes ont eu lieu pendant le comptage.
 */
@Service
public class InventoryService {

    public static final String DOC_TYPE = "INVENTAIRE";

    private final InventoryRepository inventories;
    private final ArticleRepository articles;
    private final StockBalanceRepository balances;
    private final StockService stock;
    private final DocumentNumberService numbers;
    private final AuditService audit;

    public InventoryService(InventoryRepository inventories, ArticleRepository articles, StockBalanceRepository balances,
                            StockService stock, DocumentNumberService numbers, AuditService audit) {
        this.inventories = inventories;
        this.articles = articles;
        this.balances = balances;
        this.stock = stock;
        this.numbers = numbers;
        this.audit = audit;
    }

    @PreAuthorize("hasAuthority('STOCK_VIEW')")
    @Transactional(readOnly = true)
    public PageResponse<InventoryDto.Summary> list(int page, int size) {
        return PageResponse.of(inventories.findAllByOrderByIdDesc(PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100))),
                InventoryDto.Summary::from);
    }

    @PreAuthorize("hasAuthority('STOCK_VIEW')")
    @Transactional(readOnly = true)
    public InventoryDto.Detail get(Long id) {
        return InventoryDto.Detail.from(inventories.findById(id).orElseThrow(() -> new NotFoundException("Inventaire introuvable.")));
    }

    @PreAuthorize("hasAuthority('STOCK_ADJUST')")
    @Transactional
    public InventoryDto.Detail create(InventoryDto.CreateRequest r) {
        List<Article> list = (r.articleIds() == null || r.articleIds().isEmpty())
                ? articles.findAll().stream().filter(Article::isActif).sorted(Comparator.comparing(Article::getNom)).toList()
                : articles.findAllById(r.articleIds());
        if (list.isEmpty()) {
            throw new BusinessException("Aucun article à inventorier.");
        }
        Inventory inv = new Inventory(numbers.nextYearly("INVENTAIRE", "INV"), r.libelle().trim(), SecurityUtils.username());
        Map<Long, List<StockBalance>> byArticle = new HashMap<>();
        balances.findByArticleIdIn(list.stream().map(Article::getId).toList())
                .forEach(b -> byArticle.computeIfAbsent(b.getArticle().getId(), k -> new ArrayList<>()).add(b));
        for (Article a : list) {
            List<StockBalance> bs = byArticle.getOrDefault(a.getId(), List.of());
            if (a.isSuiviLot()) {
                bs.stream().filter(b -> b.getLot() != null && b.getQuantite().signum() > 0)
                        .sorted(Comparator.comparing(b -> b.getLot().getNumero()))
                        .forEach(b -> inv.addLine(a, b.getLot(), b.getQuantite()));
            } else {
                BigDecimal q = bs.stream().filter(b -> b.getLot() == null).map(StockBalance::getQuantite)
                        .findFirst().orElse(Money.zero());
                inv.addLine(a, null, q);
            }
        }
        if (inv.getLignes().isEmpty()) {
            throw new BusinessException("Aucune ligne de stock à compter pour cette sélection.");
        }
        Inventory saved = inventories.save(inv);
        audit.log(SecurityUtils.username(), "INVENTORY_CREATE", "Inventory", String.valueOf(saved.getId()), saved.getNumero());
        return InventoryDto.Detail.from(saved);
    }

    @PreAuthorize("hasAuthority('STOCK_ADJUST')")
    @Transactional
    public InventoryDto.Detail count(Long id, InventoryDto.CountRequest r) {
        Inventory inv = lock(id);
        requireOpen(inv);
        Map<Long, Inventory.Line> byId = new HashMap<>();
        inv.getLignes().forEach(l -> byId.put(l.getId(), l));
        for (InventoryDto.CountLine c : r.lignes()) {
            Inventory.Line line = byId.get(c.lineId());
            if (line == null) {
                throw new BusinessException("Ligne d'inventaire inconnue.");
            }
            line.count(c.quantiteComptee() == null ? null : Money.strict(c.quantiteComptee(), "Quantité comptée"));
        }
        return InventoryDto.Detail.from(inv);
    }

    @PreAuthorize("hasAuthority('STOCK_ADJUST')")
    @Transactional
    public InventoryDto.Detail validate(Long id) {
        Inventory inv = lock(id);
        requireOpen(inv);
        if (inv.getLignes().stream().noneMatch(l -> l.getQuantiteComptee() != null)) {
            throw new BusinessException("Saisissez au moins une quantité comptée avant de valider.");
        }
        int adjustments = 0;
        for (Inventory.Line l : inv.getLignes()) {
            if (l.getQuantiteComptee() == null) {
                continue;
            }
            BigDecimal current = balances.findByArticleIdAndLotRef(l.getArticle().getId(), StockBalance.refOf(l.getLot()))
                    .map(StockBalance::getQuantite).orElse(Money.zero());
            BigDecimal diff = l.getQuantiteComptee().subtract(current);
            if (diff.signum() == 0) {
                continue;
            }
            MovementType type = diff.signum() > 0 ? MovementType.INVENTAIRE_PLUS : MovementType.INVENTAIRE_MOINS;
            stock.apply(l.getArticle(), l.getLot(), type, diff.abs(), "Inventaire " + inv.getNumero(), DOC_TYPE, inv.getId());
            adjustments++;
        }
        inv.validate(SecurityUtils.username());
        audit.log(SecurityUtils.username(), "INVENTORY_VALIDATE", "Inventory", String.valueOf(id),
                inv.getNumero() + " : " + adjustments + " ajustement(s)");
        return InventoryDto.Detail.from(inv);
    }

    @PreAuthorize("hasAuthority('STOCK_ADJUST')")
    @Transactional
    public InventoryDto.Detail cancel(Long id) {
        Inventory inv = lock(id);
        requireOpen(inv);
        inv.cancel();
        audit.log(SecurityUtils.username(), "INVENTORY_CANCEL", "Inventory", String.valueOf(id), inv.getNumero());
        return InventoryDto.Detail.from(inv);
    }

    private void requireOpen(Inventory inv) {
        if (!Inventory.EN_COURS.equals(inv.getStatut())) {
            throw new BusinessException("Cet inventaire est clôturé.");
        }
    }

    private Inventory lock(Long id) {
        return inventories.lockById(id).orElseThrow(() -> new NotFoundException("Inventaire introuvable."));
    }
}

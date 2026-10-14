package com.huilerie.erp.production;

import com.huilerie.erp.audit.AuditService;
import com.huilerie.erp.catalog.Article;
import com.huilerie.erp.catalog.ArticleRepository;
import com.huilerie.erp.catalog.ArticleType;
import com.huilerie.erp.common.Money;
import com.huilerie.erp.common.SecurityUtils;
import com.huilerie.erp.common.Text;
import com.huilerie.erp.common.exception.BusinessException;
import com.huilerie.erp.common.exception.NotFoundException;
import com.huilerie.erp.common.numbering.DocumentNumberService;
import com.huilerie.erp.common.response.PageResponse;
import com.huilerie.erp.inventory.*;
import com.huilerie.erp.production.dto.ProductionDto;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Trituration des olives appartenant à l'huilerie.
 * Démarrage : sortie de stock des lots d'olives. Fin : entrée de l'huile (nouveau lot) et des sous-produits.
 * Les olives des clients ne passent jamais par ici : elles relèvent du pressage à façon.
 */
@Service
public class ProductionService {

    public static final String DOC_TYPE = "PRODUCTION";

    private final ProductionOrderRepository orders;
    private final ArticleRepository articles;
    private final StockLotRepository lots;
    private final StockService stock;
    private final DocumentNumberService numbers;
    private final AuditService audit;

    public ProductionService(ProductionOrderRepository orders, ArticleRepository articles, StockLotRepository lots,
                             StockService stock, DocumentNumberService numbers, AuditService audit) {
        this.orders = orders;
        this.articles = articles;
        this.lots = lots;
        this.stock = stock;
        this.numbers = numbers;
        this.audit = audit;
    }

    @PreAuthorize("hasAuthority('PRODUCTION_VIEW')")
    @Transactional(readOnly = true)
    public PageResponse<ProductionDto.Summary> search(String q, ProductionOrder.Status statut, LocalDate from, LocalDate to,
                                                      int page, int size) {
        PageRequest pr = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100), Sort.by(Sort.Direction.DESC, "id"));
        return PageResponse.of(orders.search(Text.search(q), statut, from, to, pr), ProductionDto.Summary::from);
    }

    @PreAuthorize("hasAuthority('PRODUCTION_VIEW')")
    @Transactional(readOnly = true)
    public ProductionDto.Detail get(Long id) {
        return ProductionDto.Detail.from(orders.findById(id).orElseThrow(() -> new NotFoundException("Ordre introuvable.")));
    }

    @PreAuthorize("hasAuthority('PRODUCTION_MANAGE')")
    @Transactional
    public ProductionDto.Detail plan(Long id, ProductionDto.PlanRequest r) {
        ProductionOrder o;
        if (id == null) {
            o = new ProductionOrder(numbers.nextYearly("PRODUCTION", "PRD"), SecurityUtils.username());
        } else {
            o = lock(id);
            if (o.getStatut() != ProductionOrder.Status.PLANIFIE) {
                throw new BusinessException("Un ordre démarré ne peut plus être modifié.");
            }
        }
        Article huile = article(r.huileArticleId());
        if (!huile.isSuiviLot()) {
            throw new BusinessException("L'huile produite doit être un article suivi par lot.");
        }
        o.plan(r.datePrevue(), Text.clean(r.ligne()), huile, Text.clean(r.notes()));
        o.clearInputs();
        BigDecimal total = Money.zero();
        for (ProductionDto.InputRequest in : r.entrees()) {
            Article a = article(in.articleId());
            if (a.getType() != ArticleType.OLIVES) {
                throw new BusinessException("« " + a.getNom() + " » n'est pas un article de type olives.");
            }
            StockLot lot = null;
            if (in.lotId() != null) {
                lot = lots.findById(in.lotId()).orElseThrow(() -> new NotFoundException("Lot introuvable."));
                if (lot.getProprietaire() != LotOwner.ENTREPRISE) {
                    throw new BusinessException("Le lot " + lot.getNumero() + " appartient à un client : utilisez le pressage.");
                }
            }
            BigDecimal q = Money.strict(in.quantite(), "Quantité d'olives");
            o.addInput(a, lot, q);
            total = total.add(q);
        }
        o.setOlivesKg(total);
        ProductionOrder saved = orders.save(o);
        audit.log(SecurityUtils.username(), id == null ? "PRODUCTION_PLAN" : "PRODUCTION_UPDATE", "ProductionOrder",
                String.valueOf(saved.getId()), saved.getNumero() + " " + total.toPlainString() + " kg");
        return ProductionDto.Detail.from(saved);
    }

    /** Démarrage : les olives sortent du stock (refus si le stock est insuffisant). */
    @PreAuthorize("hasAuthority('PRODUCTION_MANAGE')")
    @Transactional
    public ProductionDto.Detail start(Long id) {
        ProductionOrder o = lock(id);
        if (o.getStatut() != ProductionOrder.Status.PLANIFIE) {
            throw new BusinessException("Seul un ordre planifié peut démarrer.");
        }
        for (ProductionOrder.Input in : o.getEntrees()) {
            stock.apply(in.getArticle(), in.getLot(), MovementType.CONSOMMATION_PRODUCTION, in.getQuantite(),
                    "Trituration " + o.getNumero(), DOC_TYPE, o.getId());
        }
        o.start();
        audit.log(SecurityUtils.username(), "PRODUCTION_START", "ProductionOrder", String.valueOf(id), o.getNumero());
        return ProductionDto.Detail.from(o);
    }

    /** Fin : huile en stock dans un nouveau lot, sous-produits en stock, rendement et catégorie calculés. */
    @PreAuthorize("hasAuthority('PRODUCTION_MANAGE')")
    @Transactional
    public ProductionDto.Detail finish(Long id, ProductionDto.FinishRequest r) {
        ProductionOrder o = lock(id);
        if (o.getStatut() != ProductionOrder.Status.EN_COURS) {
            throw new BusinessException("L'ordre doit être en cours pour être terminé.");
        }
        BigDecimal huile = Money.strict(r.huileLitres(), "Huile obtenue");
        BigDecimal pertes = Money.strict(r.pertesKg(), "Pertes");
        String numero = Text.clean(r.lotNumero()) == null ? o.getNumero() : Text.clean(r.lotNumero()).toUpperCase();
        Article oil = o.getHuileArticle();
        if (lots.existsByArticleIdAndNumeroIgnoreCase(oil.getId(), numero)) {
            throw new BusinessException("Le lot " + numero + " existe déjà pour cet article.");
        }
        StockLot lot = lots.save(new StockLot(oil, numero, LotOwner.ENTREPRISE, null));
        if (r.bloquerLot()) {
            lot.block("En attente d'analyse qualité (" + o.getNumero() + ")");
        }
        stock.apply(oil, lot, MovementType.PRODUCTION_SORTIE, huile, "Trituration " + o.getNumero(), DOC_TYPE, o.getId());
        if (r.sousProduits() != null) {
            for (ProductionDto.ByProductRequest b : r.sousProduits()) {
                Article a = article(b.articleId());
                if (a.isSuiviLot()) {
                    throw new BusinessException("Les sous-produits (« " + a.getNom() + " ») ne doivent pas être suivis par lot.");
                }
                BigDecimal q = Money.strict(b.quantite(), "Quantité de sous-produit");
                stock.apply(a, null, MovementType.PRODUCTION_SORTIE, q, "Sous-produit " + o.getNumero(), DOC_TYPE, o.getId());
                o.addByProduct(a, q);
            }
        }
        o.finish(lot, huile, pertes, r.acidite(), SecurityUtils.username());
        audit.log(SecurityUtils.username(), "PRODUCTION_FINISH", "ProductionOrder", String.valueOf(id),
                o.getNumero() + " : " + huile.toPlainString() + " L, rendement " + o.getRendement().toPlainString());
        return ProductionDto.Detail.from(o);
    }

    @PreAuthorize("hasAuthority('PRODUCTION_MANAGE')")
    @Transactional
    public ProductionDto.Detail cancel(Long id, String motif) {
        ProductionOrder o = lock(id);
        if (o.getStatut() != ProductionOrder.Status.PLANIFIE) {
            throw new BusinessException("Seul un ordre non démarré peut être annulé.");
        }
        o.cancel(motif.trim());
        audit.log(SecurityUtils.username(), "PRODUCTION_CANCEL", "ProductionOrder", String.valueOf(id), motif.trim());
        return ProductionDto.Detail.from(o);
    }

    private Article article(Long id) {
        Article a = articles.findById(id).orElseThrow(() -> new NotFoundException("Article introuvable."));
        if (!a.isActif()) {
            throw new BusinessException("« " + a.getNom() + " » est désactivé.");
        }
        return a;
    }

    private ProductionOrder lock(Long id) {
        return orders.lockById(id).orElseThrow(() -> new NotFoundException("Ordre introuvable."));
    }
}

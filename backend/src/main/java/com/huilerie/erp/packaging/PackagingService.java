package com.huilerie.erp.packaging;

import com.huilerie.erp.audit.AuditService;
import com.huilerie.erp.catalog.Article;
import com.huilerie.erp.catalog.ArticleRepository;
import com.huilerie.erp.common.Money;
import com.huilerie.erp.common.SecurityUtils;
import com.huilerie.erp.common.Text;
import com.huilerie.erp.common.exception.BusinessException;
import com.huilerie.erp.common.exception.NotFoundException;
import com.huilerie.erp.common.numbering.DocumentNumberService;
import com.huilerie.erp.common.response.PageResponse;
import com.huilerie.erp.inventory.*;
import com.huilerie.erp.packaging.dto.PackagingDto;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;

/**
 * Conditionnement en une opération atomique :
 * sortie du vrac (quantité × contenance), sortie des emballages, entrée des produits finis.
 * Le vrac doit appartenir à l'huilerie et ne pas être bloqué. L'annulation passe des mouvements inverses.
 */
@Service
public class PackagingService {

    public static final String DOC_TYPE = "CONDITIONNEMENT";

    private final PackagingOrderRepository orders;
    private final ArticleRepository articles;
    private final StockLotRepository lots;
    private final StockService stock;
    private final DocumentNumberService numbers;
    private final AuditService audit;

    public PackagingService(PackagingOrderRepository orders, ArticleRepository articles, StockLotRepository lots,
                            StockService stock, DocumentNumberService numbers, AuditService audit) {
        this.orders = orders;
        this.articles = articles;
        this.lots = lots;
        this.stock = stock;
        this.numbers = numbers;
        this.audit = audit;
    }

    @PreAuthorize("hasAuthority('PACKAGING_VIEW')")
    @Transactional(readOnly = true)
    public PageResponse<PackagingDto.Detail> search(String q, String statut, Instant from, Instant to, int page, int size) {
        PageRequest pr = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100), Sort.by(Sort.Direction.DESC, "id"));
        return PageResponse.of(orders.search(Text.search(q), Text.clean(statut), from, to, pr), PackagingDto.Detail::from);
    }

    @PreAuthorize("hasAuthority('PACKAGING_MANAGE')")
    @Transactional
    public PackagingDto.Detail create(PackagingDto.Request r) {
        Article source = article(r.sourceArticleId());
        Article produit = article(r.produitArticleId());
        if (produit.getContenance() == null) {
            throw new BusinessException("Indiquez la contenance en litres de « " + produit.getNom() + " » dans le catalogue.");
        }
        if (source.getId().equals(produit.getId())) {
            throw new BusinessException("Le produit fini doit être différent de l'huile en vrac.");
        }
        StockLot sourceLot = null;
        if (r.sourceLotId() != null) {
            sourceLot = lots.findById(r.sourceLotId()).orElseThrow(() -> new NotFoundException("Lot introuvable."));
            if (sourceLot.getProprietaire() != LotOwner.ENTREPRISE) {
                throw new BusinessException("Le lot " + sourceLot.getNumero() + " appartient à un client : il ne peut pas être conditionné pour la vente.");
            }
            if (sourceLot.getStatut() == LotStatus.BLOQUE) {
                throw new BusinessException("Le lot " + sourceLot.getNumero() + " est bloqué.");
            }
        }
        BigDecimal qty = Money.strict(r.quantiteProduite(), "Quantité produite");
        BigDecimal volume = qty.multiply(produit.getContenance()).setScale(Money.SCALE, RoundingMode.HALF_UP);

        // Le produit fini hérite du numéro de lot du vrac (traçabilité) s'il est suivi par lot.
        StockLot produitLot = null;
        if (produit.isSuiviLot()) {
            if (sourceLot == null) {
                throw new BusinessException("« " + produit.getNom() + " » est suivi par lot : choisissez le lot de vrac d'origine.");
            }
            String numero = sourceLot.getNumero();
            produitLot = lots.search(numero.toLowerCase(), produit.getId(), LotOwner.ENTREPRISE, null, PageRequest.of(0, 20))
                    .stream().filter(l -> l.getNumero().equalsIgnoreCase(numero)).findFirst()
                    .orElseGet(() -> lots.save(new StockLot(produit, numero, LotOwner.ENTREPRISE, null)));
        }

        String numero = numbers.nextYearly("CONDITIONNEMENT", "CND");
        PackagingOrder o = orders.save(new PackagingOrder(numero, source, sourceLot, produit, produitLot, qty, volume,
                Text.clean(r.notes()), SecurityUtils.username()));
        Long docId = o.getId();
        stock.apply(source, sourceLot, MovementType.CONDITIONNEMENT_SORTIE, volume, "Conditionnement " + numero, DOC_TYPE, docId);
        if (r.consommations() != null) {
            for (PackagingDto.ConsumptionRequest c : r.consommations()) {
                Article a = article(c.articleId());
                if (a.isSuiviLot()) {
                    throw new BusinessException("Les emballages (« " + a.getNom() + " ») ne doivent pas être suivis par lot.");
                }
                BigDecimal q = Money.strict(c.quantite(), "Quantité d'emballage");
                stock.apply(a, null, MovementType.CONDITIONNEMENT_SORTIE, q, "Emballage " + numero, DOC_TYPE, docId);
                o.addConsumption(a, q);
            }
        }
        stock.apply(produit, produitLot, MovementType.CONDITIONNEMENT_ENTREE, qty, "Conditionnement " + numero, DOC_TYPE, docId);
        PackagingOrder saved = orders.save(o);
        audit.log(SecurityUtils.username(), "PACKAGING_CREATE", "PackagingOrder", String.valueOf(docId),
                numero + " : " + qty.toPlainString() + " × " + produit.getNom() + " (" + volume.toPlainString() + " L)");
        return PackagingDto.Detail.from(saved);
    }

    /** Annulation : mouvements inverses, refusée si les produits finis ont déjà été vendus. */
    @PreAuthorize("hasAuthority('PACKAGING_MANAGE')")
    @Transactional
    public PackagingDto.Detail cancel(Long id, String motif) {
        PackagingOrder o = orders.lockById(id).orElseThrow(() -> new NotFoundException("Conditionnement introuvable."));
        if (!PackagingOrder.VALIDE.equals(o.getStatut())) {
            throw new BusinessException("Ce conditionnement est déjà annulé.");
        }
        String why = "Annulation " + o.getNumero() + " : " + motif.trim();
        stock.apply(o.getProduit(), o.getProduitLot(), MovementType.ANNULATION_MOINS, o.getQuantiteProduite(), why, DOC_TYPE, o.getId());
        for (PackagingOrder.Consumption c : o.getConsommations()) {
            stock.apply(c.getArticle(), null, MovementType.ANNULATION_PLUS, c.getQuantite(), why, DOC_TYPE, o.getId());
        }
        stock.apply(o.getSource(), o.getSourceLot(), MovementType.ANNULATION_PLUS, o.getVolumeUtilise(), why, DOC_TYPE, o.getId());
        o.cancel(SecurityUtils.username(), motif.trim());
        audit.log(SecurityUtils.username(), "PACKAGING_CANCEL", "PackagingOrder", String.valueOf(id), why);
        return PackagingDto.Detail.from(o);
    }

    private Article article(Long id) {
        Article a = articles.findById(id).orElseThrow(() -> new NotFoundException("Article introuvable."));
        if (!a.isActif()) {
            throw new BusinessException("« " + a.getNom() + " » est désactivé.");
        }
        return a;
    }
}

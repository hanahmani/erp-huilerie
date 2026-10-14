package com.huilerie.erp.purchases;

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
import com.huilerie.erp.finance.CashMovementType;
import com.huilerie.erp.finance.FinanceService;
import com.huilerie.erp.inventory.*;
import com.huilerie.erp.purchases.dto.PurchaseDto;
import com.huilerie.erp.settings.Tax;
import com.huilerie.erp.settings.TaxRepository;
import com.huilerie.erp.suppliers.Supplier;
import com.huilerie.erp.suppliers.SupplierRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;

/**
 * Cycle d'achat : brouillon → validation → réceptions (partielles) → paiements.
 * Une commande ne modifie le stock qu'au moment de la réception effective.
 */
@Service
public class PurchaseService {

    public static final String DOC_TYPE = "ACHAT";
    private static final BigDecimal HUNDRED = new BigDecimal("100");

    private final PurchaseRepository purchases;
    private final GoodsReceiptRepository receipts;
    private final SupplierPaymentRepository payments;
    private final SupplierRepository suppliers;
    private final ArticleRepository articles;
    private final TaxRepository taxes;
    private final StockLotRepository lots;
    private final StockService stock;
    private final FinanceService finance;
    private final DocumentNumberService numbers;
    private final AuditService audit;

    public PurchaseService(PurchaseRepository purchases, GoodsReceiptRepository receipts,
                           SupplierPaymentRepository payments, SupplierRepository suppliers, ArticleRepository articles,
                           TaxRepository taxes, StockLotRepository lots, StockService stock, FinanceService finance,
                           DocumentNumberService numbers, AuditService audit) {
        this.purchases = purchases;
        this.receipts = receipts;
        this.payments = payments;
        this.suppliers = suppliers;
        this.articles = articles;
        this.taxes = taxes;
        this.lots = lots;
        this.stock = stock;
        this.finance = finance;
        this.numbers = numbers;
        this.audit = audit;
    }

    // ------------------------------------------------------------------ calcul

    public static BigDecimal lineHt(BigDecimal qty, BigDecimal pu, BigDecimal remisePct) {
        BigDecimal gross = qty.multiply(pu);
        BigDecimal factor = HUNDRED.subtract(remisePct).divide(HUNDRED, 6, RoundingMode.HALF_UP);
        return gross.multiply(factor).setScale(Money.SCALE, RoundingMode.HALF_UP);
    }

    public static BigDecimal lineTva(BigDecimal ht, BigDecimal taux) {
        return ht.multiply(taux).divide(HUNDRED, Money.SCALE, RoundingMode.HALF_UP);
    }

    // ------------------------------------------------------------------ consultation

    @PreAuthorize("hasAuthority('PURCHASE_VIEW')")
    @Transactional(readOnly = true)
    public PageResponse<PurchaseDto.Summary> search(String q, Long supplierId, PurchaseStatus statut, LocalDate from,
                                                    LocalDate to, boolean unpaid, int page, int size) {
        PageRequest pr = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100),
                Sort.by(Sort.Direction.DESC, "id"));
        return PageResponse.of(purchases.search(Text.search(q), supplierId, statut, from, to, unpaid, pr),
                PurchaseDto.Summary::from);
    }

    @PreAuthorize("hasAuthority('PURCHASE_VIEW')")
    @Transactional(readOnly = true)
    public PurchaseDto.Detail get(Long id) {
        return PurchaseDto.Detail.from(find(id));
    }

    @PreAuthorize("hasAuthority('PURCHASE_VIEW') or hasAuthority('FINANCE_VIEW')")
    @Transactional(readOnly = true)
    public List<PurchaseDto.Payable> payables() {
        return purchases.payables().stream().map(r -> new PurchaseDto.Payable((Long) r[0], (String) r[1], (String) r[2],
                ((Number) r[3]).longValue(), Money.ofAny(r[4]))).toList();
    }

    // ------------------------------------------------------------------ brouillon

    @PreAuthorize("hasAuthority('PURCHASE_CREATE')")
    @Transactional
    public PurchaseDto.Detail create(PurchaseDto.Request r) {
        Purchase p = new Purchase(numbers.nextYearly("ACHAT", "ACH"), SecurityUtils.username());
        fill(p, r);
        p = purchases.save(p);
        audit.log(SecurityUtils.username(), "PURCHASE_CREATE", "Purchase", String.valueOf(p.getId()), p.getNumero());
        return PurchaseDto.Detail.from(p);
    }

    @PreAuthorize("hasAuthority('PURCHASE_CREATE')")
    @Transactional
    public PurchaseDto.Detail update(Long id, PurchaseDto.Request r) {
        Purchase p = lock(id);
        if (p.getStatut() != PurchaseStatus.BROUILLON) {
            throw new BusinessException("Une commande validée ne peut plus être modifiée.");
        }
        fill(p, r);
        audit.log(SecurityUtils.username(), "PURCHASE_UPDATE", "Purchase", String.valueOf(id), p.getNumero());
        return PurchaseDto.Detail.from(p);
    }

    private void fill(Purchase p, PurchaseDto.Request r) {
        Supplier s = suppliers.findById(r.fournisseurId()).orElseThrow(() -> new NotFoundException("Fournisseur introuvable."));
        if (!s.isActif()) {
            throw new BusinessException("Ce fournisseur est désactivé.");
        }
        p.setHeader(s, r.dateCommande(), Text.clean(r.refFacture()), Text.clean(r.notes()));
        p.clearLines();
        BigDecimal ht = Money.zero();
        BigDecimal tva = Money.zero();
        for (PurchaseDto.LineRequest l : r.lignes()) {
            Article a = articles.findById(l.articleId()).orElseThrow(() -> new NotFoundException("Article introuvable."));
            if (!a.isActif()) {
                throw new BusinessException("« " + a.getNom() + " » est désactivé.");
            }
            BigDecimal qty = Money.strict(l.quantite(), "Quantité");
            BigDecimal pu = Money.strict(l.prixUnitaire(), "Prix unitaire");
            BigDecimal remise = l.remisePct() == null ? BigDecimal.ZERO.setScale(2) : l.remisePct().setScale(2, RoundingMode.HALF_UP);
            BigDecimal taux = BigDecimal.ZERO.setScale(2);
            if (l.taxId() != null) {
                Tax t = taxes.findById(l.taxId()).orElseThrow(() -> new NotFoundException("Taxe introuvable."));
                if (!t.isActif()) {
                    throw new BusinessException("La taxe « " + t.getNom() + " » est désactivée.");
                }
                taux = t.getTaux();
            }
            BigDecimal lht = lineHt(qty, pu, remise);
            BigDecimal ltva = lineTva(lht, taux);
            p.addLine(a, qty, pu, remise, taux, lht, ltva);
            ht = ht.add(lht);
            tva = tva.add(ltva);
        }
        p.setTotals(ht, tva);
    }

    // ------------------------------------------------------------------ validation / annulation

    @PreAuthorize("hasAuthority('PURCHASE_VALIDATE')")
    @Transactional
    public PurchaseDto.Detail validate(Long id) {
        Purchase p = lock(id);
        if (p.getStatut() != PurchaseStatus.BROUILLON) {
            throw new BusinessException("Seul un brouillon peut être validé.");
        }
        p.validate(SecurityUtils.username());
        audit.log(SecurityUtils.username(), "PURCHASE_VALIDATE", "Purchase", String.valueOf(id), p.getNumero());
        return PurchaseDto.Detail.from(p);
    }

    @PreAuthorize("hasAuthority('PURCHASE_VALIDATE')")
    @Transactional
    public PurchaseDto.Detail cancel(Long id, String motif) {
        Purchase p = lock(id);
        if (p.getStatut() != PurchaseStatus.BROUILLON && p.getStatut() != PurchaseStatus.VALIDEE) {
            throw new BusinessException("Une commande déjà réceptionnée ne peut pas être annulée.");
        }
        if (p.getMontantPaye().signum() > 0) {
            throw new BusinessException("Cette commande a déjà des paiements : elle ne peut pas être annulée.");
        }
        p.cancel(SecurityUtils.username(), motif.trim());
        audit.log(SecurityUtils.username(), "PURCHASE_CANCEL", "Purchase", String.valueOf(id), p.getNumero() + " : " + motif.trim());
        return PurchaseDto.Detail.from(p);
    }

    @PreAuthorize("hasAuthority('PURCHASE_CREATE') or hasAuthority('FINANCE_MANAGE')")
    @Transactional
    public PurchaseDto.Detail setInvoiceRef(Long id, String ref) {
        Purchase p = lock(id);
        if (p.getStatut() == PurchaseStatus.ANNULEE) {
            throw new BusinessException("Commande annulée.");
        }
        p.setRefFacture(Text.clean(ref));
        audit.log(SecurityUtils.username(), "PURCHASE_INVOICE_REF", "Purchase", String.valueOf(id), Text.clean(ref));
        return PurchaseDto.Detail.from(p);
    }

    // ------------------------------------------------------------------ réception

    /** Réception (éventuellement partielle) : entrée en stock, création du lot si l'article est suivi par lot. */
    @PreAuthorize("hasAuthority('PURCHASE_RECEIVE')")
    @Transactional
    public PurchaseDto.Detail receive(Long id, PurchaseDto.ReceiveRequest r) {
        Purchase p = lock(id);
        if (p.getStatut() != PurchaseStatus.VALIDEE && p.getStatut() != PurchaseStatus.PARTIELLEMENT_RECUE) {
            throw new BusinessException("Seule une commande validée et non soldée peut être réceptionnée.");
        }
        Map<Long, PurchaseLine> byId = new HashMap<>();
        p.getLignes().forEach(l -> byId.put(l.getId(), l));
        p.getReceptions().size();   // charge la collection avant d'y ajouter la nouvelle réception

        GoodsReceipt receipt = receipts.save(new GoodsReceipt(numbers.nextYearly("RECEPTION", "REC"), p,
                SecurityUtils.username(), Text.clean(r.notes())));
        Set<Long> seen = new HashSet<>();
        for (PurchaseDto.ReceiveLine rl : r.lignes()) {
            PurchaseLine line = byId.get(rl.lineId());
            if (line == null) {
                throw new BusinessException("Ligne de commande inconnue.");
            }
            if (!seen.add(rl.lineId())) {
                throw new BusinessException("Une ligne est présente deux fois dans la réception.");
            }
            BigDecimal q = Money.strict(rl.quantite(), "Quantité reçue");
            if (q.compareTo(line.remaining()) > 0) {
                throw new BusinessException("« " + line.getDesignation() + " » : " + q.toPlainString()
                        + " reçus pour " + line.remaining().toPlainString() + " restant à recevoir.");
            }
            Article a = line.getArticle();
            StockLot lot = null;
            if (a.isSuiviLot()) {
                String numero = Text.clean(rl.lotNumero());
                if (numero == null) {
                    throw new BusinessException("« " + a.getNom() + " » est suivi par lot : indiquez le numéro de lot.");
                }
                String n = numero.toUpperCase();
                lot = lots.search(n.toLowerCase(), a.getId(), LotOwner.ENTREPRISE, null, PageRequest.of(0, 50))
                        .stream().filter(x -> x.getNumero().equalsIgnoreCase(n)).findFirst()
                        .orElseGet(() -> {
                            if (lots.existsByArticleIdAndNumeroIgnoreCase(a.getId(), n)) {
                                throw new BusinessException("Le lot " + n + " existe déjà pour un client : choisissez un autre numéro.");
                            }
                            return lots.save(new StockLot(a, n, LotOwner.ENTREPRISE, null));
                        });
            }
            line.addReceived(q);
            receipt.addLine(line, lot, q);
            stock.apply(a, lot, MovementType.RECEPTION_ACHAT, q, "Réception " + receipt.getNumero(), DOC_TYPE, p.getId());
        }
        p.getReceptions().add(receipt);
        p.refreshReceptionStatus();
        audit.log(SecurityUtils.username(), "PURCHASE_RECEIVE", "Purchase", String.valueOf(id), receipt.getNumero());
        return PurchaseDto.Detail.from(p);
    }

    // ------------------------------------------------------------------ paiement

    @PreAuthorize("hasAuthority('FINANCE_MANAGE')")
    @Transactional
    public PurchaseDto.Detail pay(Long id, PurchaseDto.PaymentRequest r) {
        Purchase p = lock(id);
        if (p.getStatut() == PurchaseStatus.BROUILLON || p.getStatut() == PurchaseStatus.ANNULEE) {
            throw new BusinessException("Seule une commande validée peut être payée.");
        }
        BigDecimal amount = Money.strict(r.montant(), "Montant");
        if (amount.compareTo(p.reste()) > 0) {
            throw new BusinessException("Le paiement dépasse le reste dû (" + p.reste().toPlainString() + " DT).");
        }
        p.getPaiements().size();   // charge la collection avant l'ajout
        SupplierPayment sp = payments.save(new SupplierPayment(numbers.nextYearly("PAIEMENT_FRN", "PFR"), p, r.mode(),
                amount, Text.clean(r.reference()), SecurityUtils.username()));
        p.getPaiements().add(sp);
        p.addPaid(amount);
        finance.record(CashMovementType.DECAISSEMENT, r.mode(), amount,
                "Paiement " + p.getFournisseur().getNom() + " (" + p.getNumero() + ")", DOC_TYPE, p.getId());
        audit.log(SecurityUtils.username(), "SUPPLIER_PAYMENT", "Purchase", String.valueOf(id),
                sp.getNumero() + " " + amount.toPlainString() + " DT");
        return PurchaseDto.Detail.from(p);
    }

    private Purchase find(Long id) {
        return purchases.findById(id).orElseThrow(() -> new NotFoundException("Commande introuvable."));
    }

    private Purchase lock(Long id) {
        return purchases.lockById(id).orElseThrow(() -> new NotFoundException("Commande introuvable."));
    }
}

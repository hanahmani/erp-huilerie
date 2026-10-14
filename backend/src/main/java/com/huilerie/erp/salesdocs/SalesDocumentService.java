package com.huilerie.erp.salesdocs;

import com.huilerie.erp.audit.AuditService;
import com.huilerie.erp.catalog.Article;
import com.huilerie.erp.catalog.ArticleRepository;
import com.huilerie.erp.clients.Client;
import com.huilerie.erp.clients.ClientRepository;
import com.huilerie.erp.common.Money;
import com.huilerie.erp.common.SecurityUtils;
import com.huilerie.erp.common.Text;
import com.huilerie.erp.common.exception.BusinessException;
import com.huilerie.erp.common.exception.NotFoundException;
import com.huilerie.erp.common.numbering.DocumentNumberService;
import com.huilerie.erp.common.response.PageResponse;
import com.huilerie.erp.finance.CashMovementType;
import com.huilerie.erp.finance.FinanceService;
import com.huilerie.erp.finance.PaymentMode;
import com.huilerie.erp.inventory.*;
import com.huilerie.erp.purchases.PurchaseService;
import com.huilerie.erp.sales.SaleRepository;
import com.huilerie.erp.salesdocs.SalesDocument.Status;
import com.huilerie.erp.salesdocs.SalesDocument.Type;
import com.huilerie.erp.salesdocs.dto.SalesDocDto;
import com.huilerie.erp.settings.SettingsService;
import com.huilerie.erp.settings.Tax;
import com.huilerie.erp.settings.TaxRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Map;

/**
 * Cycle de vente : devis → commande → livraison → facture → paiement ; avoir pour annuler une facture.
 * Le stock sort à la validation du bon de livraison, ou de la facture si elle n'est pas issue d'une livraison.
 */
@Service
public class SalesDocumentService {

    public static final String DOC_TYPE = "DOC_VENTE";
    private static final Map<Type, String> PREFIX = Map.of(Type.DEVIS, "DEV", Type.COMMANDE, "CMD",
            Type.LIVRAISON, "BL", Type.FACTURE, "FAC", Type.AVOIR, "AVR");

    private final SalesDocumentRepository docs;
    private final ClientRepository clients;
    private final ArticleRepository articles;
    private final TaxRepository taxes;
    private final StockLotRepository lots;
    private final StockService stock;
    private final FinanceService finance;
    private final SettingsService settings;
    private final SaleRepository sales;
    private final DocumentNumberService numbers;
    private final AuditService audit;

    public SalesDocumentService(SalesDocumentRepository docs, ClientRepository clients, ArticleRepository articles,
                                TaxRepository taxes, StockLotRepository lots, StockService stock, FinanceService finance,
                                SettingsService settings, SaleRepository sales, DocumentNumberService numbers,
                                AuditService audit) {
        this.docs = docs;
        this.clients = clients;
        this.articles = articles;
        this.taxes = taxes;
        this.lots = lots;
        this.stock = stock;
        this.finance = finance;
        this.settings = settings;
        this.sales = sales;
        this.numbers = numbers;
        this.audit = audit;
    }

    // ---------------------------------------------------------------- consultation

    @PreAuthorize("hasAuthority('SALES_VIEW')")
    @Transactional(readOnly = true)
    public PageResponse<SalesDocDto.Summary> search(Type type, String q, Status statut, Long clientId, LocalDate from,
                                                    LocalDate to, boolean unpaid, int page, int size) {
        PageRequest pr = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100), Sort.by(Sort.Direction.DESC, "id"));
        return PageResponse.of(docs.search(type, Text.search(q), statut, clientId, from, to, unpaid, pr), SalesDocDto.Summary::from);
    }

    @PreAuthorize("hasAuthority('SALES_VIEW')")
    @Transactional(readOnly = true)
    public SalesDocDto.Detail get(Long id) {
        return detail(docs.findById(id).orElseThrow(() -> new NotFoundException("Document introuvable.")));
    }

    private SalesDocDto.Detail detail(SalesDocument d) {
        return SalesDocDto.Detail.from(d, d.getId() == null ? java.util.List.of() : docs.findBySourceIdOrderByIdAsc(d.getId()));
    }

    // ---------------------------------------------------------------- brouillons

    @PreAuthorize("hasAuthority('SALESDOC_MANAGE')")
    @Transactional
    public SalesDocDto.Detail create(SalesDocDto.Request r) {
        if (r.type() == Type.AVOIR) {
            throw new BusinessException("Un avoir se crée depuis la facture qu'il annule.");
        }
        SalesDocument d = new SalesDocument(r.type(), null, null, SecurityUtils.username());
        fill(d, r);
        d = docs.save(d);
        audit.log(SecurityUtils.username(), "SALESDOC_CREATE", d.getType().name(), String.valueOf(d.getId()), null);
        return detail(d);
    }

    @PreAuthorize("hasAuthority('SALESDOC_MANAGE')")
    @Transactional
    public SalesDocDto.Detail update(Long id, SalesDocDto.Request r) {
        SalesDocument d = lock(id);
        if (d.getStatut() != Status.BROUILLON) {
            throw new BusinessException("Un document validé ne peut plus être modifié.");
        }
        if (r.type() != d.getType()) {
            throw new BusinessException("Le type d'un document ne peut pas changer.");
        }
        fill(d, r);
        audit.log(SecurityUtils.username(), "SALESDOC_UPDATE", d.getType().name(), String.valueOf(id), null);
        return detail(d);
    }

    private void fill(SalesDocument d, SalesDocDto.Request r) {
        Client c = clients.findById(r.clientId()).orElseThrow(() -> new NotFoundException("Client introuvable."));
        if (!c.isActif()) {
            throw new BusinessException("Ce client est désactivé.");
        }
        if (r.echeance() != null && r.echeance().isBefore(r.dateDoc())) {
            throw new BusinessException("L'échéance ne peut pas précéder la date du document.");
        }
        d.header(c, r.dateDoc(), r.echeance(), Text.clean(r.refClient()), Text.clean(r.notes()));
        d.clearLines();
        BigDecimal ht = Money.zero();
        BigDecimal tva = Money.zero();
        for (SalesDocDto.LineRequest l : r.lignes()) {
            Article a = articles.findById(l.articleId()).orElseThrow(() -> new NotFoundException("Article introuvable."));
            if (!a.isActif()) {
                throw new BusinessException("« " + a.getNom() + " » est désactivé.");
            }
            StockLot lot = l.lotId() == null ? null : lots.findById(l.lotId()).orElseThrow(() -> new NotFoundException("Lot introuvable."));
            BigDecimal pu = l.prixUnitaire() != null ? Money.strict(l.prixUnitaire(), "Prix") : a.getPrixVente();
            if (pu == null) {
                throw new BusinessException("Indiquez un prix pour « " + a.getNom() + " ».");
            }
            BigDecimal remise = l.remisePct() == null ? BigDecimal.ZERO.setScale(2) : l.remisePct().setScale(2, RoundingMode.HALF_UP);
            BigDecimal rate = BigDecimal.ZERO.setScale(2);
            if (l.taxId() != null) {
                Tax t = taxes.findById(l.taxId()).orElseThrow(() -> new NotFoundException("Taxe introuvable."));
                rate = t.getTaux();
            }
            BigDecimal q = Money.strict(l.quantite(), "Quantité");
            BigDecimal lht = PurchaseService.lineHt(q, pu, remise);
            BigDecimal ltva = PurchaseService.lineTva(lht, rate);
            d.addLine(a, lot, q, pu, remise, rate, lht, ltva);
            ht = ht.add(lht);
            tva = tva.add(ltva);
        }
        d.totals(ht, tva, Money.zero());
    }

    // ---------------------------------------------------------------- validation

    @PreAuthorize("hasAuthority('SALESDOC_MANAGE')")
    @Transactional
    public SalesDocDto.Detail validate(Long id) {
        SalesDocument d = lock(id);
        if (d.getStatut() != Status.BROUILLON) {
            throw new BusinessException("Seul un brouillon peut être validé.");
        }
        if (d.getType() == Type.FACTURE && !SecurityUtils.has("INVOICE_VALIDATE")) {
            throw new AccessDeniedException("La validation d'une facture exige la permission correspondante.");
        }
        if (d.getType() == Type.FACTURE) {
            BigDecimal timbre = settings.find().map(s -> s.getTimbreFiscal()).orElse(null);
            d.totals(d.getTotalHt(), d.getTotalTva(), timbre == null ? Money.zero() : timbre);
        }
        if (d.getType() == Type.COMMANDE || d.getType() == Type.FACTURE) {
            checkCreditLimit(d);
        }
        String numero = numbers.nextYearly("DOC_" + d.getType().name(), PREFIX.get(d.getType()));
        boolean movesStock = d.getType() == Type.LIVRAISON
                || (d.getType() == Type.FACTURE && (d.getSource() == null || d.getSource().getType() != Type.LIVRAISON));
        if (movesStock) {
            for (SalesDocument.Line l : d.getLignes()) {
                if (l.getLot() != null && !l.getLot().isSellable()) {
                    throw new BusinessException("Le lot " + l.getLot().getNumero() + " n'est pas vendable (client ou bloqué).");
                }
                stock.apply(l.getArticle(), l.getLot(), MovementType.LIVRAISON, l.getQuantite(),
                        d.getType().name() + " " + numero + " — " + d.getClient().getNom(), DOC_TYPE, d.getId());
            }
        }
        d.validate(numero, SecurityUtils.username());
        audit.log(SecurityUtils.username(), "SALESDOC_VALIDATE", d.getType().name(), String.valueOf(id),
                numero + " " + d.getTotalTtc().toPlainString() + " DT");
        return detail(d);
    }

    private void checkCreditLimit(SalesDocument d) {
        BigDecimal limit = d.getClient().getPlafondCredit();
        if (limit == null) {
            return;
        }
        BigDecimal due = Money.ofAny(sales.outstandingByClient(d.getClient().getId()))
                .add(Money.ofAny(docs.outstandingByClient(d.getClient().getId())));
        if (due.add(d.getTotalTtc()).compareTo(limit) > 0) {
            throw new BusinessException("Plafond de crédit dépassé pour " + d.getClient().getNom() + " : déjà dû "
                    + due.toPlainString() + " DT, plafond " + limit.toPlainString() + " DT.");
        }
    }

    // ---------------------------------------------------------------- transformation

    @PreAuthorize("hasAuthority('SALESDOC_MANAGE')")
    @Transactional
    public SalesDocDto.Detail transform(Long id, Type target) {
        SalesDocument src = lock(id);
        boolean allowed = src.getStatut() == Status.VALIDE && switch (src.getType()) {
            case DEVIS -> target == Type.COMMANDE || target == Type.FACTURE;
            case COMMANDE -> target == Type.LIVRAISON || target == Type.FACTURE;
            case LIVRAISON -> target == Type.FACTURE;
            default -> false;
        };
        if (!allowed) {
            throw new BusinessException("Transformation impossible depuis ce document.");
        }
        SalesDocument d = new SalesDocument(target, src.getClient(), src, SecurityUtils.username());
        d.header(src.getClient(), LocalDate.now(), src.getEcheance(), src.getRefClient(), src.getNotes());
        for (SalesDocument.Line l : src.getLignes()) {
            d.addLine(l.getArticle(), l.getLot(), l.getQuantite(), l.getPrixUnitaire(), l.getRemisePct(), l.getTauxTva(),
                    l.getTotalHt(), l.getTotalTva());
        }
        d.totals(src.getTotalHt(), src.getTotalTva(), Money.zero());
        src.markTransformed();
        d = docs.save(d);
        audit.log(SecurityUtils.username(), "SALESDOC_TRANSFORM", target.name(), String.valueOf(d.getId()),
                "depuis " + src.getNumero());
        return detail(d);
    }

    // ---------------------------------------------------------------- paiement, avoir, annulation

    @PreAuthorize("hasAuthority('SALESDOC_MANAGE') or hasAuthority('FINANCE_MANAGE')")
    @Transactional
    public SalesDocDto.Detail pay(Long id, SalesDocDto.PaymentRequest r) {
        SalesDocument d = lock(id);
        if (d.getType() != Type.FACTURE || d.getStatut() != Status.VALIDE) {
            throw new BusinessException("Seule une facture validée peut être réglée.");
        }
        BigDecimal amount = Money.strict(r.montant(), "Montant");
        if (amount.compareTo(d.reste()) > 0) {
            throw new BusinessException("Le paiement dépasse le reste dû (" + d.reste().toPlainString() + " DT).");
        }
        d.getPaiements().size();
        String numero = numbers.nextYearly("PAIEMENT_FACTURE", "PFC");
        d.addPayment(numero, r.mode(), amount, Text.clean(r.reference()), SecurityUtils.username());
        docs.saveAndFlush(d);
        finance.record(CashMovementType.ENCAISSEMENT, r.mode(), amount, "Facture " + d.getNumero() + " (" + d.getClient().getNom() + ")",
                DOC_TYPE, d.getId());
        audit.log(SecurityUtils.username(), "INVOICE_PAYMENT", "FACTURE", String.valueOf(id), numero + " " + amount.toPlainString() + " DT");
        return detail(d);
    }

    /** Avoir total : annule la facture, rembourse ce qui a été payé et, si demandé, réintègre la marchandise. */
    @PreAuthorize("hasAuthority('INVOICE_VALIDATE')")
    @Transactional
    public SalesDocDto.Detail creditNote(Long id, SalesDocDto.CreditNoteRequest r) {
        SalesDocument f = lock(id);
        if (f.getType() != Type.FACTURE || f.getStatut() != Status.VALIDE) {
            throw new BusinessException("Un avoir ne peut être émis que sur une facture validée.");
        }
        if (f.getMontantPaye().signum() > 0 && r.modeRemboursement() == null) {
            throw new BusinessException("Indiquez le mode de remboursement des " + f.getMontantPaye().toPlainString() + " DT déjà payés.");
        }
        SalesDocument a = new SalesDocument(Type.AVOIR, f.getClient(), f, SecurityUtils.username());
        a.header(f.getClient(), LocalDate.now(), null, f.getRefClient(), r.motif().trim());
        for (SalesDocument.Line l : f.getLignes()) {
            a.addLine(l.getArticle(), l.getLot(), l.getQuantite(), l.getPrixUnitaire(), l.getRemisePct(), l.getTauxTva(),
                    l.getTotalHt(), l.getTotalTva());
        }
        a.totals(f.getTotalHt(), f.getTotalTva(), f.getTimbre());
        String numero = numbers.nextYearly("DOC_AVOIR", "AVR");
        a.validate(numero, SecurityUtils.username());
        a = docs.save(a);
        if (r.retourStock()) {
            for (SalesDocument.Line l : f.getLignes()) {
                stock.apply(l.getArticle(), l.getLot(), MovementType.RETOUR_CLIENT, l.getQuantite(), "Avoir " + numero, DOC_TYPE, a.getId());
            }
        }
        if (f.getMontantPaye().signum() > 0) {
            finance.record(CashMovementType.DECAISSEMENT, r.modeRemboursement(), f.getMontantPaye(),
                    "Remboursement avoir " + numero + " (" + f.getClient().getNom() + ")", DOC_TYPE, a.getId());
        }
        f.cancel("Annulée par l'avoir " + numero + " : " + r.motif().trim());
        audit.log(SecurityUtils.username(), "CREDIT_NOTE", "AVOIR", String.valueOf(a.getId()), numero + " sur " + f.getNumero());
        return detail(a);
    }

    @PreAuthorize("hasAuthority('SALESDOC_MANAGE')")
    @Transactional
    public SalesDocDto.Detail cancel(Long id, String motif) {
        SalesDocument d = lock(id);
        if (d.getStatut() == Status.ANNULE || d.getStatut() == Status.TRANSFORME) {
            throw new BusinessException("Ce document ne peut pas être annulé dans son état actuel.");
        }
        if (d.getType() == Type.FACTURE && d.getStatut() == Status.VALIDE) {
            throw new BusinessException("Une facture validée s'annule uniquement par un avoir.");
        }
        if (d.getType() == Type.AVOIR) {
            throw new BusinessException("Un avoir ne peut pas être annulé.");
        }
        if (d.getType() == Type.LIVRAISON && d.getStatut() == Status.VALIDE) {
            for (SalesDocument.Line l : d.getLignes()) {
                stock.apply(l.getArticle(), l.getLot(), MovementType.ANNULATION_PLUS, l.getQuantite(),
                        "Annulation " + d.getNumero(), DOC_TYPE, d.getId());
            }
        }
        if (d.getSource() != null && d.getSource().getStatut() == Status.TRANSFORME) {
            d.getSource().reopen();
        }
        d.cancel(motif.trim());
        audit.log(SecurityUtils.username(), "SALESDOC_CANCEL", d.getType().name(), String.valueOf(id), motif.trim());
        return detail(d);
    }

    private SalesDocument lock(Long id) {
        return docs.lockById(id).orElseThrow(() -> new NotFoundException("Document introuvable."));
    }
}

package com.huilerie.erp.sales;

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
import com.huilerie.erp.sales.dto.SaleDto;
import com.huilerie.erp.settings.SettingsService;
import com.huilerie.erp.users.AppUser;
import com.huilerie.erp.users.UserRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;

@Service
public class SaleService {

    public static final String DOC_TYPE = "VENTE";

    private final SaleRepository sales;
    private final ArticleRepository articles;
    private final ClientRepository clients;
    private final UserRepository users;
    private final StockLotRepository lots;
    private final StockBalanceRepository balances;
    private final StockService stock;
    private final FinanceService finance;
    private final SettingsService settings;
    private final DocumentNumberService numbers;
    private final AuditService audit;
    private ClientPaymentRepository clientPayments;

    @org.springframework.beans.factory.annotation.Autowired
    void setClientPayments(ClientPaymentRepository clientPayments) {
        this.clientPayments = clientPayments;
    }

    public SaleService(SaleRepository sales, ArticleRepository articles, ClientRepository clients,
                       UserRepository users, StockLotRepository lots, StockBalanceRepository balances,
                       StockService stock, FinanceService finance, SettingsService settings,
                       DocumentNumberService numbers, AuditService audit) {
        this.sales = sales;
        this.articles = articles;
        this.clients = clients;
        this.users = users;
        this.lots = lots;
        this.balances = balances;
        this.stock = stock;
        this.finance = finance;
        this.settings = settings;
        this.numbers = numbers;
        this.audit = audit;
    }

    // ================================================================ catalogue de caisse

    @PreAuthorize("hasAuthority('SALES_CREATE')")
    @Transactional(readOnly = true)
    public List<SaleDto.SellableArticle> sellableCatalog() {
        List<Article> list = articles.findByVendableTrueAndActifTrueOrderByNomAsc();
        if (list.isEmpty()) {
            return List.of();
        }
        Map<Long, List<StockBalance>> byArticle = new HashMap<>();
        balances.sellable(list.stream().map(Article::getId).toList(), LotOwner.ENTREPRISE, LotStatus.DISPONIBLE)
                .forEach(b -> byArticle.computeIfAbsent(b.getArticle().getId(), k -> new ArrayList<>()).add(b));
        return list.stream().map(a -> {
            List<StockBalance> bs = byArticle.getOrDefault(a.getId(), List.of());
            BigDecimal dispo = bs.stream().map(StockBalance::getQuantite).reduce(Money.zero(), BigDecimal::add);
            List<SaleDto.SellableLot> lotLines = bs.stream().filter(b -> b.getLot() != null)
                    .sorted(Comparator.comparing(b -> b.getLot().getId()))          // FIFO : plus ancien lot d'abord
                    .map(b -> new SaleDto.SellableLot(b.getLot().getId(), b.getLot().getNumero(), b.getQuantite()))
                    .toList();
            return new SaleDto.SellableArticle(a.getId(), a.getReference(), a.getNom(), a.getCodeBarres(),
                    a.getUnite().getCode(), a.getCategorie() == null ? null : a.getCategorie().getNom(),
                    a.getPrixVente(), a.getContenance(), a.isSuiviLot(), dispo, lotLines);
        }).toList();
    }

    // ================================================================ vente rapide

    /**
     * Valide une vente en UNE transaction : numéro, lignes, sorties de stock, paiements et caisse.
     * Toute erreur annule l'ensemble (aucune vente partielle, aucun numéro consommé).
     */
    @PreAuthorize("hasAuthority('SALES_CREATE')")
    @Transactional
    public SaleDto.Detail createQuickSale(SaleDto.QuickSaleRequest r) {
        Optional<Sale> replay = sales.findByIdempotencyKey(r.idempotencyKey());
        if (replay.isPresent()) {
            return SaleDto.Detail.from(replay.get());   // double clic ou nouvel envoi : même vente
        }

        String username = SecurityUtils.username();
        AppUser cashier = users.findByUsername(username)
                .orElseThrow(() -> new AccessDeniedException("Caissier introuvable."));
        Client client = null;
        if (r.clientId() != null) {
            client = clients.findById(r.clientId()).orElseThrow(() -> new NotFoundException("Client introuvable."));
            if (!client.isActif()) {
                throw new BusinessException("Ce client est désactivé.");
            }
        }

        boolean canOverridePrice = SecurityUtils.has("SALES_PRICE_OVERRIDE");
        BigDecimal remise = Money.strict(r.remise(), "Remise");
        if (Money.isPositive(remise) && !SecurityUtils.has("SALES_DISCOUNT")) {
            throw new AccessDeniedException("Vous n'êtes pas autorisé à accorder une remise.");
        }

        // ---- préparation des lignes (contrôles avant toute écriture)
        record Prepared(Article article, StockLot lot, BigDecimal qty, BigDecimal price, BigDecimal total) {
        }
        List<Prepared> prepared = new ArrayList<>();
        for (SaleDto.LineRequest l : r.lignes()) {
            Article a = articles.findById(l.articleId()).orElseThrow(() -> new NotFoundException("Produit introuvable."));
            if (!a.isActif() || !a.isVendable()) {
                throw new BusinessException("« " + a.getNom() + " » n'est pas vendable.");
            }
            StockLot lot = null;
            if (l.lotId() != null) {
                lot = lots.findById(l.lotId()).orElseThrow(() -> new NotFoundException("Lot introuvable."));
                if (lot.getProprietaire() == LotOwner.CLIENT) {
                    throw new BusinessException("Le lot " + lot.getNumero()
                            + " appartient à un client (pressage) : il ne peut pas être vendu.");
                }
                if (lot.getStatut() == LotStatus.BLOQUE) {
                    throw new BusinessException("Le lot " + lot.getNumero() + " est bloqué : vente impossible.");
                }
            }
            BigDecimal qty = Money.strict(l.quantite(), "Quantité");
            BigDecimal catalogPrice = a.getPrixVente();
            BigDecimal price = l.prixUnitaire() == null ? catalogPrice : Money.strict(l.prixUnitaire(), "Prix unitaire");
            if (price.compareTo(catalogPrice) != 0 && !canOverridePrice) {
                throw new AccessDeniedException("Vous n'êtes pas autorisé à modifier le prix de vente.");
            }
            prepared.add(new Prepared(a, lot, qty, price, SaleCalculator.lineTotal(qty, price)));
        }

        Map<PaymentMode, BigDecimal> payments = new EnumMap<>(PaymentMode.class);
        if (r.paiements() != null) {
            for (SaleDto.PaymentRequest p : r.paiements()) {
                payments.merge(p.mode(), Money.strict(p.montant(), "Montant payé"), BigDecimal::add);
            }
        }
        SaleCalculator.Totals t = SaleCalculator.compute(prepared.stream().map(Prepared::total).toList(), remise, payments);

        // ---- crédit (paiement partiel) : seulement si autorisé et pour un client identifié
        if (t.reste().signum() > 0) {
            if (client == null) {
                throw new BusinessException("Paiement incomplet : il manque " + t.reste().toPlainString()
                        + " DT. Une vente à crédit nécessite un client identifié.");
            }
            if (!settings.creditAllowed()) {
                throw new BusinessException("Paiement incomplet : il manque " + t.reste().toPlainString()
                        + " DT. Les ventes à crédit sont désactivées dans les paramètres.");
            }
            if (client.getPlafondCredit() != null) {
                BigDecimal after = Money.of(sales.outstandingByClient(client.getId())).add(t.reste());
                if (after.compareTo(client.getPlafondCredit()) > 0) {
                    throw new BusinessException("Plafond de crédit dépassé pour " + client.getNom() + " ("
                            + client.getPlafondCredit().toPlainString() + " DT).");
                }
            }
        }

        // ---- écritures
        String numero = numbers.nextYearly("TICKET", "TCK");
        Sale sale = new Sale(numero, r.idempotencyKey(), client, cashier);
        prepared.forEach(p -> sale.addLine(p.article(), p.lot(), p.qty(), p.price(), p.total()));
        t.affecte().forEach(sale::addPayment);
        sale.setTotals(t.sousTotal(), t.remise(), t.total(), t.recu(), t.paye(), t.reste(), t.monnaie());
        Sale saved = sales.save(sale);

        for (Prepared p : prepared) {
            stock.apply(p.article(), p.lot(), MovementType.VENTE, p.qty(), null, DOC_TYPE, saved.getId());
        }
        t.affecte().forEach((mode, amount) -> finance.record(CashMovementType.ENCAISSEMENT, mode, amount,
                "Vente " + numero, DOC_TYPE, saved.getId()));

        audit.log(username, "SALE_CREATE", "Sale", String.valueOf(saved.getId()),
                numero + " total " + t.total().toPlainString() + " DT");
        return SaleDto.Detail.from(saved);
    }

    // ================================================================ consultation

    @PreAuthorize("hasAuthority('SALES_VIEW')")
    @Transactional(readOnly = true)
    public PageResponse<SaleDto.Summary> search(Instant from, Instant to, String numero, Long clientId,
                                                Long cashierId, SaleStatus statut, int page, int size) {
        PageRequest pr = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100),
                Sort.by(Sort.Direction.DESC, "dateVente"));
        return PageResponse.of(sales.search(from, to, Text.search(numero), clientId, cashierId, statut, pr),
                SaleDto.Summary::from);
    }

    @PreAuthorize("hasAuthority('SALES_VIEW')")
    @Transactional(readOnly = true)
    public SaleDto.Detail get(Long id) {
        return detail(sale(id));
    }

    private SaleDto.Detail detail(Sale s) {
        List<SaleDto.Settlement> settlements = clientPayments.findBySaleIdOrderByIdAsc(s.getId()).stream()
                .map(p -> new SaleDto.Settlement(p.getNumero(), p.getDate(), p.getMode(), p.getMontant(),
                        p.getReference(), p.getUsername()))
                .toList();
        return SaleDto.Detail.from(s, settlements);
    }

    // ================================================================ créances clients

    @PreAuthorize("hasAuthority('SALES_VIEW') or hasAuthority('FINANCE_VIEW')")
    @Transactional(readOnly = true)
    public List<SaleDto.Receivable> receivables() {
        return sales.receivables().stream().map(r -> new SaleDto.Receivable((Long) r[0], (String) r[1], (String) r[2],
                (String) r[3], ((Number) r[4]).longValue(), Money.ofAny(r[5]), (Instant) r[6])).toList();
    }

    @PreAuthorize("hasAuthority('SALES_VIEW') or hasAuthority('FINANCE_VIEW')")
    @Transactional(readOnly = true)
    public List<SaleDto.Summary> openCredits(Long clientId) {
        return sales.openCreditsOf(clientId).stream().map(SaleDto.Summary::from).toList();
    }

    /** Règlement d'une vente à crédit : encaissement en caisse, total de la vente inchangé. */
    @PreAuthorize("hasAuthority('SALES_CREATE') or hasAuthority('FINANCE_MANAGE')")
    @Transactional
    public SaleDto.Detail settle(Long id, SaleDto.SettlementRequest r) {
        Sale s = sales.lockById(id).orElseThrow(() -> new NotFoundException("Vente introuvable."));
        if (s.getStatut() != SaleStatus.VALIDEE) {
            throw new BusinessException("Cette vente est annulée.");
        }
        BigDecimal amount = Money.strict(r.montant(), "Montant");
        if (amount.compareTo(s.getResteAPayer()) > 0) {
            throw new BusinessException("Le règlement dépasse le reste dû (" + s.getResteAPayer().toPlainString() + " DT).");
        }
        ClientPayment cp = clientPayments.save(new ClientPayment(numbers.nextYearly("REGLEMENT", "RGL"), s, r.mode(), amount,
                Text.clean(r.reference()), SecurityUtils.username()));
        s.applySettlement(amount);
        finance.record(CashMovementType.ENCAISSEMENT, r.mode(), amount, "Règlement " + s.getNumeroTicket()
                + (s.getClient() == null ? "" : " (" + s.getClient().getNom() + ")"), "REGLEMENT", s.getId());
        audit.log(SecurityUtils.username(), "SALE_SETTLEMENT", "Sale", String.valueOf(id),
                cp.getNumero() + " " + amount.toPlainString() + " DT");
        return detail(s);
    }

    @PreAuthorize("hasAuthority('SALES_VIEW')")
    @Transactional(readOnly = true)
    public List<SaleDto.Cashier> cashiers() {
        return sales.cashiers().stream()
                .map(u -> new SaleDto.Cashier(u.getId(), u.fullName()))
                .sorted(Comparator.comparing(SaleDto.Cashier::nom))
                .toList();
    }

    /** Impression ou réimpression : compte et trace, sans jamais toucher au stock ni à la caisse. */
    @PreAuthorize("hasAuthority('SALES_VIEW')")
    @Transactional
    public SaleDto.Detail recordPrint(Long id) {
        Sale s = sales.lockById(id).orElseThrow(() -> new NotFoundException("Vente introuvable."));
        s.markPrinted();
        audit.log(SecurityUtils.username(), s.getNbImpressions() == 1 ? "TICKET_PRINT" : "TICKET_REPRINT",
                "Sale", String.valueOf(id), s.getNumeroTicket() + " impression n° " + s.getNbImpressions());
        return detail(s);
    }

    // ================================================================ annulation

    /** Annulation autorisée et tracée : remise en stock et décaissement compensatoires. */
    @PreAuthorize("hasAuthority('SALES_CANCEL')")
    @Transactional
    public SaleDto.Detail cancel(Long id, String motif) {
        Sale s = sales.lockById(id).orElseThrow(() -> new NotFoundException("Vente introuvable."));
        if (s.getStatut() == SaleStatus.ANNULEE) {
            throw new BusinessException("Cette vente est déjà annulée.");
        }
        if (!clientPayments.findBySaleIdOrderByIdAsc(s.getId()).isEmpty()) {
            throw new BusinessException("Cette vente a reçu des règlements ultérieurs : elle ne peut plus être annulée.");
        }
        String reason = motif.trim();
        for (SaleLine l : s.getLignes()) {
            stock.apply(l.getArticle(), l.getLot(), MovementType.ANNULATION_VENTE, l.getQuantite(),
                    "Annulation " + s.getNumeroTicket() + " : " + reason, DOC_TYPE, s.getId());
        }
        for (SalePayment p : s.getPaiements()) {
            finance.record(CashMovementType.DECAISSEMENT, p.getMode(), p.getMontant(),
                    "Annulation vente " + s.getNumeroTicket(), DOC_TYPE, s.getId());
        }
        s.cancel(SecurityUtils.username(), reason);
        audit.log(SecurityUtils.username(), "SALE_CANCEL", "Sale", String.valueOf(id), s.getNumeroTicket() + " : " + reason);
        return detail(s);
    }

    private Sale sale(Long id) {
        return sales.findById(id).orElseThrow(() -> new NotFoundException("Vente introuvable."));
    }
}

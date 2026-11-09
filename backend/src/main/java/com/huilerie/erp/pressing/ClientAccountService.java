package com.huilerie.erp.pressing;

import com.huilerie.erp.audit.AuditService;
import com.huilerie.erp.catalog.OilUnits;
import com.huilerie.erp.clients.Client;
import com.huilerie.erp.clients.ClientRepository;
import com.huilerie.erp.common.Money;
import com.huilerie.erp.common.SecurityUtils;
import com.huilerie.erp.common.Text;
import com.huilerie.erp.common.exception.BusinessException;
import com.huilerie.erp.common.exception.NotFoundException;
import com.huilerie.erp.common.numbering.DocumentNumberService;
import com.huilerie.erp.finance.CashMovementType;
import com.huilerie.erp.finance.FinanceService;
import com.huilerie.erp.inventory.MovementType;
import com.huilerie.erp.inventory.StockBalanceRepository;
import com.huilerie.erp.inventory.StockService;
import com.huilerie.erp.inventory.StorageTank;
import com.huilerie.erp.inventory.StorageTankRepository;
import com.huilerie.erp.pressing.dto.PressingDto;
import com.huilerie.erp.settings.SettingsService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

/**
 * Comptes des clients du pressage, pour que chaque opération reste traçable :
 * <ul>
 *   <li>compte d'huile (pressage à façon) : entrées à la fin de chaque pressage, remises sur le solde global,
 *       en kg ou en litres, seulement quand le service est payé ;</li>
 *   <li>compte vendeur (achat d'olives) : avances versées avant les achats, déduites automatiquement
 *       des achats suivants, et reste dû au vendeur.</li>
 * </ul>
 */
@Service
public class ClientAccountService {

    private final PressingRepository pressings;
    private final ClientOilReturnRepository returns;
    private final SellerAdvanceRepository advances;
    private final ClientRepository clients;
    private final StorageTankRepository tanks;
    private final StockBalanceRepository balances;
    private final StockService stock;
    private final FinanceService finance;
    private final SettingsService settings;
    private final DocumentNumberService numbers;
    private final AuditService audit;

    public ClientAccountService(PressingRepository pressings, ClientOilReturnRepository returns,
                                SellerAdvanceRepository advances, ClientRepository clients, StorageTankRepository tanks,
                                StockBalanceRepository balances, StockService stock, FinanceService finance,
                                SettingsService settings, DocumentNumberService numbers, AuditService audit) {
        this.pressings = pressings;
        this.returns = returns;
        this.advances = advances;
        this.clients = clients;
        this.tanks = tanks;
        this.balances = balances;
        this.stock = stock;
        this.finance = finance;
        this.settings = settings;
        this.numbers = numbers;
        this.audit = audit;
    }

    // ================================================================ liste des comptes

    @PreAuthorize("hasAuthority('PRESSING_VIEW')")
    @Transactional(readOnly = true)
    public List<PressingDto.AccountRow> accounts() {
        Map<Long, BigDecimal> available = new HashMap<>();
        advances.availableByClient().forEach(r -> available.put((Long) r[0], Money.ofAny(r[1])));
        Map<Long, PressingDto.AccountRow> rows = new LinkedHashMap<>();
        for (Object[] r : pressings.accounts()) {
            Long id = (Long) r[0];
            rows.put(id, new PressingDto.AccountRow(id, (String) r[1], (String) r[2], (String) r[3], Money.ofAny(r[4]),
                    Money.ofAny(r[5]), Money.ofAny(r[6]), available.getOrDefault(id, Money.zero())));
        }
        // Vendeurs qui n'ont qu'une avance, sans achat enregistré.
        available.forEach((id, amount) -> {
            if (!rows.containsKey(id) && amount.signum() > 0) {
                Client c = clients.findById(id).orElseThrow();
                rows.put(id, new PressingDto.AccountRow(id, c.getNom(), c.getCode(), c.getTelephone(), Money.zero(),
                        Money.zero(), Money.zero(), amount));
            }
        });
        return rows.values().stream()
                .filter(a -> a.huile().signum() > 0 || a.montantDu().signum() > 0 || a.resteVendeur().signum() > 0
                        || a.avanceDisponible().signum() > 0)
                .sorted(Comparator.comparing(PressingDto.AccountRow::clientNom, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    // ================================================================ compte d'huile

    @PreAuthorize("hasAuthority('PRESSING_VIEW')")
    @Transactional(readOnly = true)
    public PressingDto.OilAccount oilAccount(Long clientId) {
        Client c = client(clientId);
        List<Pressing> list = pressings.findByClientIdAndModeOrderByIdAsc(clientId, Pressing.Mode.SERVICE);

        record Event(java.time.Instant date, String type, String numero, String detail, BigDecimal in, BigDecimal out,
                     BigDecimal litres, String username) {
        }
        List<Event> events = new ArrayList<>();
        String unite = null;
        BigDecimal du = Money.zero();
        for (Pressing p : list) {
            if (p.getStatut() == Pressing.Status.ANNULE) {
                continue;
            }
            du = du.add(p.reste());
            if (p.getHuileObtenue() == null) {
                continue;
            }
            unite = unite == null ? p.getArticle().getUnite().getCode() : unite;
            events.add(new Event(p.getDateFin(), "ENTREE", p.getNumero(),
                    "Bulletin " + p.getNumeroBulletin() + " · " + p.getPoidsNet().toPlainString() + " kg d'olives"
                            + (p.getTank() == null ? "" : " · " + p.getTank().getCode()),
                    p.getHuileObtenue(), null, null, p.getRecuPar()));
        }
        for (ClientOilReturn r : returns.findByClientIdOrderByIdAsc(clientId)) {
            events.add(new Event(r.getDate(), "SORTIE", r.getNumero(), "Remis à " + r.getRemisA(), null, r.getQuantite(),
                    r.getLitres(), r.getUsername()));
        }
        // Remises faites pressage par pressage avant le compte global (sans remise client) : reprises telles quelles.
        for (Pressing p : list) {
            for (Pressing.Return r : p.getRemises()) {
                if (r.getClientReturn() == null) {
                    events.add(new Event(r.getDate(), "SORTIE", r.getNumero(), "Remis à " + r.getRemisA(), null,
                            r.getQuantite(), null, r.getUsername()));
                }
            }
        }
        events.sort(Comparator.comparing(Event::date));

        BigDecimal solde = Money.zero();
        List<PressingDto.OilMovement> mouvements = new ArrayList<>();
        for (Event e : events) {
            solde = e.in() != null ? solde.add(e.in()) : solde.subtract(e.out());
            mouvements.add(new PressingDto.OilMovement(e.date(), e.type(), e.numero(), e.detail(), e.in(), e.out(),
                    e.litres(), solde, e.username()));
        }
        BigDecimal density = settings.oilDensity();
        List<PressingDto.TankLine> puits = tanks.findByClientIdOrderByCodeAsc(clientId).stream()
                .map(t -> new PressingDto.TankLine(t.getId(), t.getCode(), t.getNom(),
                        Money.of(balances.quantityOfLot(t.getLot().getId())), t.getCapacite()))
                .toList();
        BigDecimal litres = "kg".equalsIgnoreCase(unite) || unite == null
                ? solde.divide(density, Money.SCALE, RoundingMode.HALF_UP) : null;
        List<PressingDto.ServiceLine> services = new ArrayList<>();
        List<PressingDto.ServicePaymentLine> paiements = new ArrayList<>();
        for (Pressing p : list) {
            if (p.getStatut() == Pressing.Status.ANNULE) {
                continue;
            }
            services.add(new PressingDto.ServiceLine(p.getId(), p.getNumero(), p.getNumeroBulletin(), p.getDateReception(),
                    p.getStatut(), p.getPoidsNet(), p.getMontant(), p.getMontantPaye(), p.reste()));
            for (Pressing.Payment x : p.getPaiements()) {
                paiements.add(new PressingDto.ServicePaymentLine(x.getNumero(), x.getDate(), x.getMode(), x.getMontant(),
                        x.getReference(), p.getNumero(), p.getNumeroBulletin(), x.getUsername()));
            }
        }
        paiements.sort(Comparator.comparing(PressingDto.ServicePaymentLine::date).reversed());
        return new PressingDto.OilAccount(c.getId(), c.getNom(), c.getCode(), unite == null ? "kg" : unite, solde,
                litres, density, du, puits, mouvements, services, paiements);
    }

    /**
     * Paiement du service sur le compte du client (pressage à façon), sans passer par un bulletin : le montant est
     * imputé sur ses pressages non payés, les plus anciens d'abord, sans dépasser le total dû.
     * Une ligne de paiement et une entrée en caisse par pressage réglé.
     */
    @PreAuthorize("hasAuthority('PRESSING_MANAGE') or hasAuthority('SALES_CREATE')")
    @Transactional
    public PressingDto.OilAccount payService(Long clientId, PressingDto.PaymentRequest r) {
        Client c = client(clientId);
        List<Pressing> unpaid = pressings.lockUnpaidServiceOf(clientId);
        BigDecimal due = unpaid.stream().map(Pressing::reste).reduce(Money.zero(), BigDecimal::add);
        if (due.signum() == 0) {
            throw new BusinessException(c.getNom() + " n'a aucun service de pressage à payer.");
        }
        BigDecimal amount = Money.strict(r.montant(), "Montant");
        if (amount.compareTo(due) > 0) {
            throw new BusinessException("Le paiement dépasse le service dû par " + c.getNom() + " ("
                    + due.toPlainString() + " DT).");
        }
        String reference = Text.clean(r.reference());
        BigDecimal left = amount;
        List<String> parts = new ArrayList<>();
        for (Pressing p : unpaid) {
            if (left.signum() == 0) {
                break;
            }
            BigDecimal part = left.min(p.reste());
            p.getPaiements().size();
            String numero = numbers.nextYearly("PAIEMENT_PRESSAGE", "PPR");
            p.addPayment(numero, r.mode(), part, reference, SecurityUtils.username());
            pressings.saveAndFlush(p);
            finance.record(CashMovementType.ENCAISSEMENT, r.mode(), part,
                    "Pressage " + p.getNumero() + " (" + c.getNom() + ") — règlement du compte", PressingService.DOC_TYPE,
                    p.getId());
            parts.add(numero + " " + p.getNumero() + " " + part.toPlainString() + " DT");
            left = left.subtract(part);
        }
        audit.log(SecurityUtils.username(), "PRESSING_ACCOUNT_PAYMENT", "Client", String.valueOf(clientId),
                amount.toPlainString() + " DT : " + String.join(", ", parts));
        return oilAccount(clientId);
    }

    /** Remise sur le solde global du client : prise sur ses pressages, les plus anciens d'abord. */
    @PreAuthorize("hasAuthority('PRESSING_MANAGE')")
    @Transactional
    public PressingDto.OilAccount returnToClient(Long clientId, PressingDto.ClientReturnRequest r) {
        Client c = client(clientId);
        giveOil(c, pressings.lockOilOf(clientId), r.quantite(), r.enLitres(), r.remisA());
        return oilAccount(clientId);
    }

    /**
     * Sort l'huile des pressages donnés (déjà verrouillés), FIFO, sous un seul numéro de remise.
     * Refus si le service de l'un de ces pressages n'est pas entièrement payé.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    ClientOilReturn giveOil(Client c, List<Pressing> sources, BigDecimal quantite, boolean enLitres, String remisAInput) {
        List<Pressing> withOil = sources.stream().filter(p -> p.huileARemettre().signum() > 0).toList();
        if (withOil.isEmpty()) {
            throw new BusinessException(c.getNom() + " n'a pas d'huile à reprendre.");
        }
        BigDecimal due = withOil.stream().map(Pressing::reste).reduce(Money.zero(), BigDecimal::add);
        if (due.signum() > 0) {
            throw new BusinessException("Le service n'est pas entièrement payé (reste " + due.toPlainString()
                    + " DT) : encaissez-le avant de remettre l'huile.");
        }
        BigDecimal q = Money.strict(quantite, "Quantité");
        BigDecimal litres = null;
        if (enLitres) {
            if (!"kg".equalsIgnoreCase(withOil.get(0).getArticle().getUnite().getCode())) {
                throw new BusinessException("L'huile de ce client n'est pas comptée en kg : remise en litres impossible.");
            }
            litres = q;
            q = OilUnits.litresToKg(q, settings.oilDensity());
        }
        BigDecimal available = withOil.stream().map(Pressing::huileARemettre).reduce(Money.zero(), BigDecimal::add);
        if (q.compareTo(available) > 0) {
            throw new BusinessException("Il reste " + available.toPlainString() + " "
                    + withOil.get(0).getArticle().getUnite().getCode() + " à remettre à " + c.getNom() + ".");
        }
        String remisA = Text.clean(remisAInput) == null ? c.getNom() : Text.clean(remisAInput);
        ClientOilReturn ret = returns.save(new ClientOilReturn(numbers.nextYearly("REMISE_HUILE", "RMH"), c, q, litres,
                remisA, SecurityUtils.username()));
        BigDecimal left = q;
        for (Pressing p : withOil) {
            if (left.signum() == 0) {
                break;
            }
            BigDecimal part = left.min(p.huileARemettre());
            p.getRemises().size();
            stock.apply(p.getArticle(), p.getLot(), MovementType.PRESSAGE_REMISE, part,
                    "Remise " + ret.getNumero() + " à " + remisA, PressingService.DOC_TYPE, p.getId());
            p.addReturn(ret, part);
            left = left.subtract(part);
        }
        audit.log(SecurityUtils.username(), "PRESSING_RETURN", "Client", String.valueOf(c.getId()),
                ret.getNumero() + " " + q.toPlainString() + (litres == null ? "" : " (" + litres.toPlainString() + " L)"));
        return ret;
    }

    /** Puits du client pour la fin d'un pressage à façon : réservé à ce client et contenant la même huile. */
    StorageTank clientTank(Pressing p, Long tankId) {
        StorageTank t = tanks.findById(tankId).orElseThrow(() -> new NotFoundException("Contenant introuvable."));
        if (t.getClient() == null || !t.getClient().getId().equals(p.getClient().getId())) {
            throw new BusinessException(t.getCode() + " n'est pas réservé à " + p.getClient().getNom() + ".");
        }
        if (!t.getArticle().getId().equals(p.getArticle().getId())) {
            throw new BusinessException(t.getCode() + " contient « " + t.getArticle().getNom()
                    + " », pas « " + p.getArticle().getNom() + " ».");
        }
        return t;
    }

    // ================================================================ compte vendeur et avances

    @PreAuthorize("hasAuthority('PRESSING_VIEW')")
    @Transactional(readOnly = true)
    public PressingDto.SellerAccount sellerAccount(Long clientId) {
        Client c = client(clientId);
        List<PressingDto.AdvanceLine> avances = advances.findByClientIdOrderByIdAsc(clientId).stream()
                .map(a -> new PressingDto.AdvanceLine(a.getNumero(), a.getDate(), a.getMode(), a.getMontant(),
                        a.getMontantImpute(), a.reste(), a.getReference(), a.getUsername()))
                .toList();
        List<PressingDto.PurchaseLine> achats = pressings.findByClientIdAndModeOrderByIdAsc(clientId, Pressing.Mode.ACHAT)
                .stream()
                .map(p -> new PressingDto.PurchaseLine(p.getId(), p.getNumero(), p.getNumeroBulletin(),
                        p.getDateReception(), p.getStatut(), p.getPoidsNet(), p.getPrixOlivesKg(), p.getMontantAchat(),
                        p.getMontantRegle(), p.getStatut() == Pressing.Status.ANNULE ? Money.zero() : p.resteVendeur()))
                .toList();
        BigDecimal reste = achats.stream().map(PressingDto.PurchaseLine::reste).reduce(Money.zero(), BigDecimal::add);
        BigDecimal dispo = avances.stream().map(PressingDto.AdvanceLine::reste).reduce(Money.zero(), BigDecimal::add);
        return new PressingDto.SellerAccount(c.getId(), c.getNom(), c.getCode(), reste, dispo, avances, achats);
    }

    /**
     * Avance au vendeur, avant ses achats : sortie de caisse maintenant, puis déduction automatique.
     * Elle est d'abord déduite de ses achats déjà enregistrés et pas encore payés.
     */
    @PreAuthorize("hasAuthority('PRESSING_MANAGE') or hasAuthority('FINANCE_MANAGE')")
    @Transactional
    public PressingDto.SellerAccount giveAdvance(Long clientId, PressingDto.PaymentRequest r) {
        Client c = client(clientId);
        if (!c.isActif()) {
            throw new BusinessException("Ce client est désactivé.");
        }
        BigDecimal amount = Money.strict(r.montant(), "Montant");
        SellerAdvance a = advances.save(new SellerAdvance(numbers.nextYearly("AVANCE_OLIVES", "AVO"), c, r.mode(), amount,
                Text.clean(r.reference()), SecurityUtils.username()));
        finance.record(CashMovementType.DECAISSEMENT, r.mode(), amount,
                "Avance olives " + a.getNumero() + " (" + c.getNom() + ")", "AVANCE_OLIVES", a.getId());
        audit.log(SecurityUtils.username(), "OLIVE_ADVANCE", "Client", String.valueOf(clientId),
                a.getNumero() + " " + amount.toPlainString() + " DT");
        for (Pressing p : pressings.findByClientIdAndModeOrderByIdAsc(clientId, Pressing.Mode.ACHAT)) {
            if (p.getStatut() != Pressing.Status.ANNULE && p.resteVendeur().signum() > 0) {
                imputeAdvances(pressings.lockById(p.getId()).orElseThrow());
            }
        }
        return sellerAccount(clientId);
    }

    /** Déduit d'un achat les avances disponibles du vendeur, les plus anciennes d'abord. */
    @Transactional(propagation = Propagation.MANDATORY)
    void imputeAdvances(Pressing p) {
        for (SellerAdvance a : advances.lockAvailable(p.getClient().getId())) {
            BigDecimal part = a.reste().min(p.resteVendeur());
            if (part.signum() <= 0) {
                break;
            }
            p.getReglementsVendeur().size();
            p.addSellerPayment(numbers.nextYearly("PAIEMENT_OLIVES", "PAO"), a.getMode(), part, "Avance " + a.getNumero(),
                    SecurityUtils.username(), a);
            a.impute(part);
            audit.log(SecurityUtils.username(), "OLIVE_ADVANCE_USE", "Pressing", String.valueOf(p.getId()),
                    a.getNumero() + " " + part.toPlainString() + " DT sur " + p.getNumero());
        }
    }

    /** Achat annulé : les avances qui y étaient déduites redeviennent disponibles. */
    @Transactional(propagation = Propagation.MANDATORY)
    void releaseAdvances(Pressing p) {
        for (Pressing.SellerPayment sp : p.getReglementsVendeur()) {
            if (sp.getAdvance() != null) {
                advances.findById(sp.getAdvance().getId()).orElseThrow().release(sp.getMontant());
            }
        }
    }

    private Client client(Long id) {
        return clients.findById(id).orElseThrow(() -> new NotFoundException("Client introuvable."));
    }
}

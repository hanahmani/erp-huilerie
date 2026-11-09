package com.huilerie.erp.pressing;

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
import com.huilerie.erp.pressing.dto.PressingDto;
import com.huilerie.erp.settings.CompanySettings;
import com.huilerie.erp.settings.SettingsService;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;

/**
 * Bulletin de pressage.
 * SERVICE : l'huile obtenue entre dans un lot appartenant au client (le puits qui lui est réservé, sinon un lot propre
 * au pressage), jamais vendable, et n'en sort que par une remise ; le service est facturé au kilo d'olives net et doit
 * être payé avant la remise. Les remises se font sur le compte d'huile du client ({@link ClientAccountService}).
 * ACHAT : l'huilerie achète les olives au kilo net ; l'huile obtenue est versée dans un puits (vendable en caisse)
 * et le vendeur est payé, en une ou plusieurs fois, jusqu'au montant d'achat ; ses avances en sont déduites.
 */
@Service
public class PressingService {

    public static final String DOC_TYPE = "PRESSAGE";
    /** Séquence permanente des bulletins de pressage (000001, 000002…). */
    public static final String BULLETIN_SEQUENCE = "BULLETIN_PRESSAGE";
    /** Tarif du pressage à façon quand aucun n'est saisi ni défini dans Paramètres : 0,200 DT par kg d'olives net. */
    public static final BigDecimal TARIF_PAR_DEFAUT = new BigDecimal("0.200");

    private final PressingRepository pressings;
    private final ClientRepository clients;
    private final ArticleRepository articles;
    private final StockLotRepository lots;
    private final StockService stock;
    private final FinanceService finance;
    private final SettingsService settings;
    private final DocumentNumberService numbers;
    private final AuditService audit;
    private final TankService tanks;
    private final ClientAccountService accounts;
    private final com.huilerie.erp.inventory.StorageTankRepository tankRepository;

    public PressingService(PressingRepository pressings, ClientRepository clients, ArticleRepository articles,
                           StockLotRepository lots, StockService stock, FinanceService finance, SettingsService settings,
                           DocumentNumberService numbers, AuditService audit, TankService tanks,
                           ClientAccountService accounts, com.huilerie.erp.inventory.StorageTankRepository tankRepository) {
        this.tankRepository = tankRepository;
        this.tanks = tanks;
        this.accounts = accounts;
        this.pressings = pressings;
        this.clients = clients;
        this.articles = articles;
        this.lots = lots;
        this.stock = stock;
        this.finance = finance;
        this.settings = settings;
        this.numbers = numbers;
        this.audit = audit;
    }

    @PreAuthorize("hasAuthority('PRESSING_VIEW')")
    @Transactional(readOnly = true)
    public PageResponse<PressingDto.Summary> search(String q, Pressing.Status statut, Long clientId, Instant from,
                                                    Instant to, int page, int size) {
        PageRequest pr = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100), Sort.by(Sort.Direction.DESC, "id"));
        return PageResponse.of(pressings.search(Text.search(q), statut, clientId, from, to, pr), PressingDto.Summary::from);
    }

    @PreAuthorize("hasAuthority('PRESSING_VIEW')")
    @Transactional(readOnly = true)
    public PressingDto.Detail get(Long id) {
        return PressingDto.Detail.from(pressings.findById(id).orElseThrow(() -> new NotFoundException("Pressage introuvable.")));
    }

    /** Réception et pesée des olives (bulletin de pressage), en service ou en achat. */
    @PreAuthorize("hasAuthority('PRESSING_MANAGE')")
    @Transactional
    public PressingDto.Detail receive(PressingDto.ReceiveRequest r) {
        Client client = clients.findById(r.clientId()).orElseThrow(() -> new NotFoundException("Client introuvable."));
        if (!client.isActif()) {
            throw new BusinessException("Ce client est désactivé.");
        }
        boolean purchase = r.mode() == Pressing.Mode.ACHAT;
        BigDecimal brut = Money.strict(r.poidsBrut(), "Poids brut");
        BigDecimal tare = Money.strict(r.tare(), "Tare");
        if (tare.compareTo(brut) >= 0) {
            throw new BusinessException("La tare doit être inférieure au poids brut.");
        }
        BigDecimal net = brut.subtract(tare);

        Article article = null;
        BigDecimal tarif = Money.zero();
        BigDecimal prixAchat = null;
        if (purchase) {
            if (r.prixOlivesKg() == null) {
                throw new BusinessException("Indiquez le prix d'achat des olives au kilo.");
            }
            prixAchat = Money.strict(r.prixOlivesKg(), "Prix d'achat");
        } else {
            CompanySettings cfg = settings.find().orElse(null);
            if (r.articleId() != null) {
                article = articles.findById(r.articleId()).orElseThrow(() -> new NotFoundException("Article introuvable."));
            } else if (cfg != null && cfg.getPressageArticle() != null) {
                article = cfg.getPressageArticle();
            } else {
                throw new BusinessException("Choisissez l'article « huile des clients » (ou définissez-le dans Paramètres).");
            }
            if (!article.isSuiviLot()) {
                throw new BusinessException("L'article de l'huile des clients doit être suivi par lot.");
            }
            tarif = r.tarifKg() != null ? Money.strict(r.tarifKg(), "Tarif")
                    : cfg == null || cfg.getTarifPressageKg() == null ? TARIF_PAR_DEFAUT : cfg.getTarifPressageKg();
        }
        BigDecimal montant = net.multiply(tarif).setScale(Money.SCALE, RoundingMode.HALF_UP);
        Pressing p = new Pressing(numbers.nextYearly("PRESSAGE", "PRS"), numbers.nextPlain(BULLETIN_SEQUENCE, 6),
                client, article, Text.clean(r.variete()), Text.clean(r.provenance()), r.etatOlives(), brut, tare, tarif,
                montant, Text.clean(r.notes()), SecurityUtils.username());
        if (purchase) {
            p.markPurchase(prixAchat, net.multiply(prixAchat).setScale(Money.SCALE, RoundingMode.HALF_UP));
        }
        String adresse = Text.clean(r.adresse());
        p.setTransport(Text.clean(r.blNumero()), Text.clean(r.transporteur()), vehicle(r.vehicule()),
                adresse != null ? adresse : Text.clean(client.getAdresse()));
        if (r.dateReception() != null) {
            p.setDateReception(receptionDate(r.dateReception()));
        }
        p = pressings.save(p);
        audit.log(SecurityUtils.username(), purchase ? "PRESSING_PURCHASE" : "PRESSING_RECEIVE", "Pressing",
                String.valueOf(p.getId()), p.getNumero() + " bulletin " + p.getNumeroBulletin() + " " + net.toPlainString()
                        + " kg net" + (purchase ? ", achat " + p.getMontantAchat().toPlainString() + " DT" : ""));
        if (purchase) {
            accounts.imputeAdvances(p);
            BigDecimal avance = Money.strict(r.avance(), "Avance");
            if (avance != null && avance.signum() > 0) {
                if (r.modeAvance() == null) {
                    throw new BusinessException("Indiquez le mode de paiement de l'avance.");
                }
                payToSeller(p, r.modeAvance(), avance, "Avance à la réception");
            }
        }
        return PressingDto.Detail.from(p);
    }

    /** Correction du bulletin (date de réception, bon de livraison, transporteur, véhicule, adresse) avant le pressage. */
    @PreAuthorize("hasAuthority('PRESSING_MANAGE')")
    @Transactional
    public PressingDto.Detail updateTransport(Long id, PressingDto.TransportRequest r) {
        Pressing p = lock(id);
        if (p.getStatut() != Pressing.Status.RECU) {
            throw new BusinessException("Le bulletin ne peut plus être modifié : le pressage a déjà commencé.");
        }
        p.setTransport(Text.clean(r.blNumero()), Text.clean(r.transporteur()), vehicle(r.vehicule()),
                Text.clean(r.adresse()));
        if (r.dateReception() != null) {
            p.setDateReception(receptionDate(r.dateReception()));
        }
        audit.log(SecurityUtils.username(), "PRESSING_UPDATE", "Pressing", String.valueOf(id),
                "Bulletin " + p.getNumeroBulletin());
        return PressingDto.Detail.from(p);
    }

    /**
     * Modification du prix au kg sur le bulletin, à tout statut sauf annulé : prix d'achat des olives (le montant dû au
     * vendeur est recalculé ; les avances disponibles sont déduites si le montant augmente) ou tarif du service.
     * Le nouveau montant ne peut pas être inférieur à ce qui a déjà été payé.
     */
    @PreAuthorize("hasAuthority('PRESSING_MANAGE')")
    @Transactional
    public PressingDto.Detail updatePrice(Long id, BigDecimal prixKg) {
        Pressing p = lock(id);
        if (p.getStatut() == Pressing.Status.ANNULE) {
            throw new BusinessException("Ce pressage est annulé.");
        }
        BigDecimal prix = Money.strict(prixKg, "Prix");
        BigDecimal montant = p.getPoidsNet().multiply(prix).setScale(Money.SCALE, RoundingMode.HALF_UP);
        BigDecimal dejaPaye = p.isPurchase() ? p.getMontantRegle() : p.getMontantPaye();
        if (montant.compareTo(dejaPaye) < 0) {
            throw new BusinessException("Le nouveau montant (" + montant.toPlainString() + " DT) est inférieur à ce qui a "
                    + "déjà été payé (" + dejaPaye.toPlainString() + " DT).");
        }
        BigDecimal ancienPrix = p.isPurchase() ? p.getPrixOlivesKg() : p.getTarifKg();
        p.changePrice(prix, montant);
        audit.log(SecurityUtils.username(), "PRESSING_UPDATE_PRICE", "Pressing", String.valueOf(id),
                "Bulletin " + p.getNumeroBulletin() + (p.isPurchase() ? " : prix d'achat " : " : tarif ")
                        + ancienPrix.toPlainString() + " → " + prix.toPlainString() + " DT/kg, montant "
                        + montant.toPlainString() + " DT");
        if (p.isPurchase()) {
            accounts.imputeAdvances(p);
        }
        return PressingDto.Detail.from(p);
    }

    /**
     * Correction de la date de réception, à tout moment (même après le pressage) : elle ne change ni le stock ni la caisse.
     * Elle ne peut pas être postérieure au début du pressage.
     */
    @PreAuthorize("hasAuthority('PRESSING_MANAGE')")
    @Transactional
    public PressingDto.Detail updateDate(Long id, Instant dateReception) {
        Pressing p = lock(id);
        Instant date = receptionDate(dateReception);
        if (p.getDateDebut() != null && date.isAfter(p.getDateDebut())) {
            throw new BusinessException("La date de réception ne peut pas être après le début du pressage.");
        }
        Instant ancienne = p.getDateReception();
        p.setDateReception(date);
        audit.log(SecurityUtils.username(), "PRESSING_UPDATE_DATE", "Pressing", String.valueOf(id),
                "Bulletin " + p.getNumeroBulletin() + " : " + ancienne + " → " + date);
        return PressingDto.Detail.from(p);
    }

    /** Date de réception saisie : jamais dans le futur (quelques minutes de tolérance pour l'horloge du poste). */
    private static Instant receptionDate(Instant date) {
        if (date.isAfter(Instant.now().plus(java.time.Duration.ofMinutes(5)))) {
            throw new BusinessException("La date de réception ne peut pas être dans le futur.");
        }
        return date;
    }

    /** Immatriculation en majuscules, espaces superflus retirés. */
    private static String vehicle(String raw) {
        String v = Text.clean(raw);
        return v == null ? null : v.replaceAll("\\s+", " ").toUpperCase();
    }

    @PreAuthorize("hasAuthority('PRESSING_MANAGE')")
    @Transactional
    public PressingDto.Detail start(Long id) {
        Pressing p = lock(id);
        if (p.getStatut() != Pressing.Status.RECU) {
            throw new BusinessException("Seules les olives reçues peuvent passer au pressage.");
        }
        p.start();
        audit.log(SecurityUtils.username(), "PRESSING_START", "Pressing", String.valueOf(id), p.getNumero());
        return PressingDto.Detail.from(p);
    }

    /** Fin du pressage : l'huile du client entre en stock dans un lot qui lui appartient. */
    @PreAuthorize("hasAuthority('PRESSING_MANAGE')")
    @Transactional
    public PressingDto.Detail finish(Long id, PressingDto.FinishRequest r) {
        Pressing p = lock(id);
        if (p.getStatut() != Pressing.Status.EN_COURS) {
            throw new BusinessException("Le pressage doit être en cours pour être terminé.");
        }
        BigDecimal huile = Money.strict(r.huileObtenue(), "Huile obtenue");
        BigDecimal pertes = Money.strict(r.pertesKg(), "Pertes");
        if (p.isPurchase()) {
            if (r.tankId() == null) {
                throw new BusinessException("Choisissez la pile, la citerne ou le fût où l'huile est stockée.");
            }
            if (tankRepository.findById(r.tankId()).map(t -> t.getClient() != null).orElse(false)) {
                throw new BusinessException("Ce contenant est réservé à un client : l'huile achetée va dans un contenant de l'huilerie.");
            }
            StorageTank tank = tanks.fill(r.tankId(), huile, MovementType.PRESSAGE_ACHAT,
                    "Achat olives " + p.getNumero() + " — " + p.getClient().getNom(), DOC_TYPE, p.getId());
            p.finishPurchase(tank, huile, pertes);
            audit.log(SecurityUtils.username(), "PRESSING_FINISH", "Pressing", String.valueOf(id),
                    p.getNumero() + " : " + huile.toPlainString() + " " + tank.getArticle().getUnite().getCode()
                            + " dans " + tank.getCode());
            return PressingDto.Detail.from(p);
        }
        if (r.priseDirecte() && r.tankId() != null) {
            throw new BusinessException("Prise directe : l'huile part avec le client, ne choisissez pas de citerne ni de fût.");
        }
        if (r.tankId() != null) {
            StorageTank own = accounts.clientTank(p, r.tankId());
            tanks.fill(own.getId(), huile, MovementType.PRESSAGE_ENTREE,
                    "Pressage " + p.getNumero() + " — " + p.getClient().getNom(), DOC_TYPE, p.getId());
            p.finishInTank(own, huile, pertes);
            audit.log(SecurityUtils.username(), "PRESSING_FINISH", "Pressing", String.valueOf(id),
                    p.getNumero() + " : " + huile.toPlainString() + " " + p.getArticle().getUnite().getCode()
                            + " dans " + own.getCode());
            return PressingDto.Detail.from(p);
        }
        if (lots.existsByArticleIdAndNumeroIgnoreCase(p.getArticle().getId(), p.getNumero())) {
            throw new BusinessException("Un lot porte déjà le numéro " + p.getNumero() + ".");
        }
        StockLot lot = lots.save(new StockLot(p.getArticle(), p.getNumero(), LotOwner.CLIENT, p.getClient()));
        stock.apply(p.getArticle(), lot, MovementType.PRESSAGE_ENTREE, huile,
                "Pressage " + p.getNumero() + " — " + p.getClient().getNom(), DOC_TYPE, p.getId());
        p.finish(lot, huile, pertes);
        audit.log(SecurityUtils.username(), "PRESSING_FINISH", "Pressing", String.valueOf(id),
                p.getNumero() + " : " + huile.toPlainString() + " " + p.getArticle().getUnite().getCode()
                        + (r.priseDirecte() ? " (prise directe)" : ""));
        if (r.priseDirecte()) {
            // Le client emporte son huile : le reste du service est encaissé maintenant, puis toute l'huile est remise.
            if (p.reste().signum() > 0) {
                if (r.modePaiement() == null) {
                    throw new BusinessException("Prise directe : choisissez le mode de paiement du service ("
                            + p.reste().toPlainString() + " DT).");
                }
                recordPayment(p, r.modePaiement(), p.reste(), Text.clean(r.reference()));
            }
            accounts.giveOil(p.getClient(), List.of(p), huile, false, r.remisA());
            pressings.saveAndFlush(p);
        }
        return PressingDto.Detail.from(p);
    }

    /** Remise de l'huile d'un seul pressage (en une ou plusieurs fois), après paiement complet du service. */
    @PreAuthorize("hasAuthority('PRESSING_MANAGE')")
    @Transactional
    public PressingDto.Detail returnOil(Long id, PressingDto.ReturnRequest r) {
        Pressing p = lock(id);
        if (p.isPurchase()) {
            throw new BusinessException("Olives achetées : l'huile appartient à l'huilerie, elle n'est pas remise au vendeur.");
        }
        if (p.getStatut() != Pressing.Status.TERMINE) {
            throw new BusinessException("L'huile ne peut être remise qu'après la fin du pressage.");
        }
        accounts.giveOil(p.getClient(), List.of(p), r.quantite(), false, r.remisA());
        pressings.saveAndFlush(p);
        return PressingDto.Detail.from(p);
    }

    @PreAuthorize("hasAuthority('PRESSING_MANAGE') or hasAuthority('SALES_CREATE')")
    @Transactional
    public PressingDto.Detail pay(Long id, PressingDto.PaymentRequest r) {
        Pressing p = lock(id);
        if (p.isPurchase()) {
            throw new BusinessException("Olives achetées : aucun frais de pressage à encaisser. Utilisez « Payer le vendeur ».");
        }
        if (p.getStatut() == Pressing.Status.ANNULE) {
            throw new BusinessException("Ce pressage est annulé.");
        }
        BigDecimal amount = Money.strict(r.montant(), "Montant");
        if (amount.compareTo(p.reste()) > 0) {
            throw new BusinessException("Le paiement dépasse le reste dû (" + p.reste().toPlainString() + " DT).");
        }
        recordPayment(p, r.mode(), amount, Text.clean(r.reference()));
        return PressingDto.Detail.from(p);
    }

    /** Encaissement du service : paiement sur le bulletin et entrée en caisse. */
    private void recordPayment(Pressing p, PaymentMode mode, BigDecimal amount, String reference) {
        p.getPaiements().size();
        String numero = numbers.nextYearly("PAIEMENT_PRESSAGE", "PPR");
        p.addPayment(numero, mode, amount, reference, SecurityUtils.username());
        pressings.saveAndFlush(p);
        finance.record(CashMovementType.ENCAISSEMENT, mode, amount,
                "Pressage " + p.getNumero() + " (" + p.getClient().getNom() + ")", DOC_TYPE, p.getId());
        audit.log(SecurityUtils.username(), "PRESSING_PAYMENT", "Pressing", String.valueOf(p.getId()),
                numero + " " + amount.toPlainString() + " DT");
    }

    /** Paiement au vendeur des olives achetées : sortie de caisse, plafonnée au reste dû. */
    @PreAuthorize("hasAuthority('PRESSING_MANAGE') or hasAuthority('FINANCE_MANAGE')")
    @Transactional
    public PressingDto.Detail paySeller(Long id, PressingDto.PaymentRequest r) {
        Pressing p = lock(id);
        if (!p.isPurchase()) {
            throw new BusinessException("Ce bulletin n'est pas un achat d'olives.");
        }
        if (p.getStatut() == Pressing.Status.ANNULE) {
            throw new BusinessException("Ce pressage est annulé.");
        }
        payToSeller(p, r.mode(), Money.strict(r.montant(), "Montant"), Text.clean(r.reference()));
        return PressingDto.Detail.from(p);
    }

    private void payToSeller(Pressing p, PaymentMode mode, BigDecimal amount, String reference) {
        if (amount.compareTo(p.resteVendeur()) > 0) {
            throw new BusinessException("Le paiement dépasse le reste dû au vendeur (" + p.resteVendeur().toPlainString() + " DT).");
        }
        p.getReglementsVendeur().size();
        String numero = numbers.nextYearly("PAIEMENT_OLIVES", "PAO");
        p.addSellerPayment(numero, mode, amount, reference, SecurityUtils.username(), null);
        pressings.saveAndFlush(p);
        finance.record(CashMovementType.DECAISSEMENT, mode, amount,
                "Achat olives " + p.getNumero() + " (" + p.getClient().getNom() + ")", "ACHAT_OLIVES", p.getId());
        audit.log(SecurityUtils.username(), "OLIVE_PURCHASE_PAYMENT", "Pressing", String.valueOf(p.getId()),
                numero + " " + amount.toPlainString() + " DT");
    }

    @PreAuthorize("hasAuthority('PRESSING_MANAGE')")
    @Transactional
    public PressingDto.Detail cancel(Long id, String motif) {
        Pressing p = lock(id);
        if (p.getStatut() != Pressing.Status.RECU && p.getStatut() != Pressing.Status.EN_COURS) {
            throw new BusinessException("Un pressage terminé ne peut plus être annulé.");
        }
        // Les avances déduites ne sont pas de l'argent sorti pour cet achat : elles sont rendues au vendeur.
        boolean cashPaid = p.getReglementsVendeur().stream().anyMatch(sp -> sp.getAdvance() == null);
        if (p.getMontantPaye().signum() > 0 || cashPaid) {
            throw new BusinessException("Ce pressage a déjà été payé : il ne peut pas être annulé.");
        }
        accounts.releaseAdvances(p);
        p.cancel(motif.trim());
        audit.log(SecurityUtils.username(), "PRESSING_CANCEL", "Pressing", String.valueOf(id), p.getNumero() + " : " + motif.trim());
        return PressingDto.Detail.from(p);
    }

    /**
     * Suppression définitive d'un bulletin saisi par erreur : possible tant que l'huile n'est pas entrée en stock
     * (reçu, en cours ou annulé) et que rien n'a été encaissé ni payé en caisse. Les avances déduites sont rendues
     * au vendeur (déjà fait si le pressage était annulé).
     */
    @PreAuthorize("hasAuthority('PRESSING_MANAGE')")
    @Transactional
    public void delete(Long id) {
        Pressing p = lock(id);
        if (p.getStatut() == Pressing.Status.TERMINE || p.getStatut() == Pressing.Status.REMIS) {
            throw new BusinessException("Un pressage terminé ne peut pas être supprimé : son huile est déjà en stock.");
        }
        boolean cashPaid = p.getReglementsVendeur().stream().anyMatch(sp -> sp.getAdvance() == null);
        if (p.getMontantPaye().signum() > 0 || cashPaid) {
            throw new BusinessException("Ce pressage a déjà été payé : il ne peut pas être supprimé.");
        }
        if (p.getStatut() != Pressing.Status.ANNULE) {
            accounts.releaseAdvances(p);
        }
        pressings.delete(p);
        audit.log(SecurityUtils.username(), "PRESSING_DELETE", "Pressing", String.valueOf(id),
                p.getNumero() + " bulletin " + p.getNumeroBulletin() + " (" + p.getClient().getNom() + ")");
    }

    private Pressing lock(Long id) {
        return pressings.lockById(id).orElseThrow(() -> new NotFoundException("Pressage introuvable."));
    }
}

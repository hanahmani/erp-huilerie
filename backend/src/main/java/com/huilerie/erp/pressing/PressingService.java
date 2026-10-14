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

/**
 * Service de pressage pour les clients.
 * Règle centrale : l'huile obtenue entre dans un lot appartenant au client (jamais vendable) et n'en sort
 * que par une remise au client. Le service est facturé au kilo d'olives net et doit être payé avant la remise.
 */
@Service
public class PressingService {

    public static final String DOC_TYPE = "PRESSAGE";

    private final PressingRepository pressings;
    private final ClientRepository clients;
    private final ArticleRepository articles;
    private final StockLotRepository lots;
    private final StockService stock;
    private final FinanceService finance;
    private final SettingsService settings;
    private final DocumentNumberService numbers;
    private final AuditService audit;

    public PressingService(PressingRepository pressings, ClientRepository clients, ArticleRepository articles,
                           StockLotRepository lots, StockService stock, FinanceService finance, SettingsService settings,
                           DocumentNumberService numbers, AuditService audit) {
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

    /** Réception et pesée des olives du client (bon de pesée). */
    @PreAuthorize("hasAuthority('PRESSING_MANAGE')")
    @Transactional
    public PressingDto.Detail receive(PressingDto.ReceiveRequest r) {
        Client client = clients.findById(r.clientId()).orElseThrow(() -> new NotFoundException("Client introuvable."));
        if (!client.isActif()) {
            throw new BusinessException("Ce client est désactivé.");
        }
        CompanySettings cfg = settings.find().orElse(null);
        Article article;
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
        BigDecimal tarif = r.tarifKg() != null ? Money.strict(r.tarifKg(), "Tarif")
                : cfg == null || cfg.getTarifPressageKg() == null ? null : cfg.getTarifPressageKg();
        if (tarif == null) {
            throw new BusinessException("Indiquez le tarif du pressage au kilo (ou définissez-le dans Paramètres).");
        }
        BigDecimal brut = Money.strict(r.poidsBrut(), "Poids brut");
        BigDecimal tare = Money.strict(r.tare(), "Tare");
        if (tare.compareTo(brut) >= 0) {
            throw new BusinessException("La tare doit être inférieure au poids brut.");
        }
        BigDecimal net = brut.subtract(tare);
        BigDecimal montant = net.multiply(tarif).setScale(Money.SCALE, RoundingMode.HALF_UP);
        Pressing p = pressings.save(new Pressing(numbers.nextYearly("PRESSAGE", "PRS"), client, article,
                Text.clean(r.variete()), Text.clean(r.provenance()), r.etatOlives(), brut, tare, tarif, montant,
                Text.clean(r.notes()), SecurityUtils.username()));
        audit.log(SecurityUtils.username(), "PRESSING_RECEIVE", "Pressing", String.valueOf(p.getId()),
                p.getNumero() + " " + net.toPlainString() + " kg net");
        return PressingDto.Detail.from(p);
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
        if (lots.existsByArticleIdAndNumeroIgnoreCase(p.getArticle().getId(), p.getNumero())) {
            throw new BusinessException("Un lot porte déjà le numéro " + p.getNumero() + ".");
        }
        StockLot lot = lots.save(new StockLot(p.getArticle(), p.getNumero(), LotOwner.CLIENT, p.getClient()));
        stock.apply(p.getArticle(), lot, MovementType.PRESSAGE_ENTREE, huile,
                "Pressage " + p.getNumero() + " — " + p.getClient().getNom(), DOC_TYPE, p.getId());
        p.finish(lot, huile, pertes);
        audit.log(SecurityUtils.username(), "PRESSING_FINISH", "Pressing", String.valueOf(id),
                p.getNumero() + " : " + huile.toPlainString() + " " + p.getArticle().getUnite().getCode());
        return PressingDto.Detail.from(p);
    }

    /** Remise de l'huile au client (en une ou plusieurs fois), après paiement complet du service. */
    @PreAuthorize("hasAuthority('PRESSING_MANAGE')")
    @Transactional
    public PressingDto.Detail returnOil(Long id, PressingDto.ReturnRequest r) {
        Pressing p = lock(id);
        if (p.getStatut() != Pressing.Status.TERMINE) {
            throw new BusinessException("L'huile ne peut être remise qu'après la fin du pressage.");
        }
        if (p.reste().signum() > 0) {
            throw new BusinessException("Le service n'est pas entièrement payé (reste " + p.reste().toPlainString()
                    + " DT) : encaissez-le avant de remettre l'huile.");
        }
        BigDecimal q = Money.strict(r.quantite(), "Quantité");
        if (q.compareTo(p.huileARemettre()) > 0) {
            throw new BusinessException("Il reste " + p.huileARemettre().toPlainString() + " à remettre.");
        }
        p.getRemises().size();
        String remisA = Text.clean(r.remisA()) == null ? p.getClient().getNom() : Text.clean(r.remisA());
        String numero = numbers.nextYearly("REMISE_HUILE", "RMH");
        stock.apply(p.getArticle(), p.getLot(), MovementType.PRESSAGE_REMISE, q, "Remise " + numero + " à " + remisA,
                DOC_TYPE, p.getId());
        p.addReturn(numero, q, remisA, SecurityUtils.username());
        pressings.saveAndFlush(p);
        audit.log(SecurityUtils.username(), "PRESSING_RETURN", "Pressing", String.valueOf(id), numero + " " + q.toPlainString());
        return PressingDto.Detail.from(p);
    }

    @PreAuthorize("hasAuthority('PRESSING_MANAGE') or hasAuthority('SALES_CREATE')")
    @Transactional
    public PressingDto.Detail pay(Long id, PressingDto.PaymentRequest r) {
        Pressing p = lock(id);
        if (p.getStatut() == Pressing.Status.ANNULE) {
            throw new BusinessException("Ce pressage est annulé.");
        }
        BigDecimal amount = Money.strict(r.montant(), "Montant");
        if (amount.compareTo(p.reste()) > 0) {
            throw new BusinessException("Le paiement dépasse le reste dû (" + p.reste().toPlainString() + " DT).");
        }
        p.getPaiements().size();
        String numero = numbers.nextYearly("PAIEMENT_PRESSAGE", "PPR");
        p.addPayment(numero, r.mode(), amount, Text.clean(r.reference()), SecurityUtils.username());
        pressings.saveAndFlush(p);
        finance.record(CashMovementType.ENCAISSEMENT, r.mode(), amount,
                "Pressage " + p.getNumero() + " (" + p.getClient().getNom() + ")", DOC_TYPE, p.getId());
        audit.log(SecurityUtils.username(), "PRESSING_PAYMENT", "Pressing", String.valueOf(id), numero + " " + amount.toPlainString() + " DT");
        return PressingDto.Detail.from(p);
    }

    @PreAuthorize("hasAuthority('PRESSING_MANAGE')")
    @Transactional
    public PressingDto.Detail cancel(Long id, String motif) {
        Pressing p = lock(id);
        if (p.getStatut() != Pressing.Status.RECU && p.getStatut() != Pressing.Status.EN_COURS) {
            throw new BusinessException("Un pressage terminé ne peut plus être annulé.");
        }
        if (p.getMontantPaye().signum() > 0) {
            throw new BusinessException("Ce pressage a déjà été payé : il ne peut pas être annulé.");
        }
        p.cancel(motif.trim());
        audit.log(SecurityUtils.username(), "PRESSING_CANCEL", "Pressing", String.valueOf(id), p.getNumero() + " : " + motif.trim());
        return PressingDto.Detail.from(p);
    }

    private Pressing lock(Long id) {
        return pressings.lockById(id).orElseThrow(() -> new NotFoundException("Pressage introuvable."));
    }
}

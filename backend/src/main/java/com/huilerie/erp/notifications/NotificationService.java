package com.huilerie.erp.notifications;

import com.huilerie.erp.common.SecurityUtils;
import com.huilerie.erp.common.exception.NotFoundException;
import com.huilerie.erp.hr.EmployeeRepository;
import com.huilerie.erp.hr.LeaveRequestRepository;
import com.huilerie.erp.inventory.LotStatus;
import com.huilerie.erp.inventory.StockLotRepository;
import com.huilerie.erp.inventory.StockService;
import com.huilerie.erp.pressing.Pressing;
import com.huilerie.erp.pressing.PressingRepository;
import com.huilerie.erp.purchases.PurchaseRepository;
import com.huilerie.erp.purchases.PurchaseStatus;
import com.huilerie.erp.quality.QualityAnomalyRepository;
import com.huilerie.erp.salesdocs.SalesDocumentRepository;
import com.huilerie.erp.users.AppUser;
import com.huilerie.erp.users.UserRepository;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Alertes calculées à partir des données réelles, filtrées selon les permissions de l'utilisateur.
 * Seul l'état « lu » est stocké. Une alerte disparaît d'elle-même quand sa cause est réglée.
 * L'envoi par e-mail / SMS / WhatsApp pourra s'appuyer sur ce même calcul (évolution prévue).
 */
@Service
public class NotificationService {

    public record Notification(String key, String type, String niveau, String titre, String message, String lien, boolean lu) {
    }

    private final NotificationReadRepository reads;
    private final UserRepository users;
    private final StockService stock;
    private final StockLotRepository lots;
    private final SalesDocumentRepository invoices;
    private final PressingRepository pressings;
    private final PurchaseRepository purchases;
    private final LeaveRequestRepository leaves;
    private final EmployeeRepository employees;
    private final QualityAnomalyRepository anomalies;

    public NotificationService(NotificationReadRepository reads, UserRepository users, StockService stock,
                               StockLotRepository lots, SalesDocumentRepository invoices, PressingRepository pressings,
                               PurchaseRepository purchases, LeaveRequestRepository leaves, EmployeeRepository employees,
                               QualityAnomalyRepository anomalies) {
        this.reads = reads;
        this.users = users;
        this.stock = stock;
        this.lots = lots;
        this.invoices = invoices;
        this.pressings = pressings;
        this.purchases = purchases;
        this.leaves = leaves;
        this.employees = employees;
        this.anomalies = anomalies;
    }

    @PreAuthorize("isAuthenticated()")
    @Transactional(readOnly = true)
    public List<Notification> list() {
        AppUser me = current();
        Set<String> read = new HashSet<>();
        reads.findByUserId(me.getId()).forEach(r -> read.add(r.getKey()));
        LocalDate today = LocalDate.now(ZoneId.of("Africa/Tunis"));
        List<Notification> out = new ArrayList<>();

        if (SecurityUtils.has("STOCK_VIEW")) {
            stock.lowStock().forEach(a -> add(out, read, "STOCK:" + a.articleId() + ":" + a.disponible().toPlainString(), "STOCK",
                    a.disponible().signum() == 0 ? "ALERTE" : "AVERTISSEMENT",
                    a.disponible().signum() == 0 ? "Rupture de stock" : "Stock faible",
                    a.nom() + " : " + a.disponible().toPlainString() + " " + a.unite() + " (seuil " + a.seuilMin().toPlainString() + ")",
                    "/stock"));
            long blocked = lots.countByStatut(LotStatus.BLOQUE);
            if (blocked > 0) {
                add(out, read, "LOTS_BLOQUES:" + blocked + ":" + today, "QUALITE", "INFO", "Lots bloqués",
                        blocked + " lot(s) en attente de décision qualité", "/stock");
            }
        }
        if (SecurityUtils.has("SALES_VIEW")) {
            invoices.overdue(today).forEach(f -> add(out, read, "FACTURE_ECHUE:" + f.getId() + ":" + f.getMontantPaye().toPlainString(),
                    "PAIEMENT", "ALERTE", "Facture échue", f.getNumero() + " — " + f.getClient().getNom() + " : reste "
                            + f.reste().toPlainString() + " DT depuis le " + f.getEcheance(), "/ventes/factures"));
        }
        if (SecurityUtils.has("PRESSING_VIEW")) {
            long toReturn = pressings.countByStatutIn(List.of(Pressing.Status.TERMINE));
            if (toReturn > 0) {
                add(out, read, "PRESSAGE_A_REMETTRE:" + toReturn + ":" + today, "PRODUCTION", "INFO", "Huile des clients à remettre",
                        toReturn + " pressage(s) terminé(s) en attente de remise", "/pressage");
            }
        }
        if (SecurityUtils.has("PURCHASE_RECEIVE")) {
            long pending = purchases.countByStatutIn(List.of(PurchaseStatus.VALIDEE, PurchaseStatus.PARTIELLEMENT_RECUE));
            if (pending > 0) {
                add(out, read, "RECEPTIONS:" + pending + ":" + today, "VALIDATION", "INFO", "Réceptions en attente",
                        pending + " commande(s) fournisseur à réceptionner", "/achats");
            }
        }
        if (SecurityUtils.has("PURCHASE_VALIDATE")) {
            long drafts = purchases.countByStatutIn(List.of(PurchaseStatus.BROUILLON));
            if (drafts > 0) {
                add(out, read, "ACHATS_A_VALIDER:" + drafts + ":" + today, "VALIDATION", "INFO", "Commandes à valider",
                        drafts + " brouillon(s) de commande fournisseur", "/achats");
            }
        }
        if (SecurityUtils.has("LEAVE_VALIDATE")) {
            long pending = leaves.countByStatut("EN_ATTENTE");
            if (pending > 0) {
                add(out, read, "CONGES:" + pending + ":" + today, "VALIDATION", "INFO", "Demandes de congé",
                        pending + " demande(s) en attente de décision", "/rh");
            }
        }
        if (SecurityUtils.has("HR_VIEW")) {
            employees.contractsEndingBefore(today.plusDays(30)).forEach(e -> add(out, read,
                    "CONTRAT:" + e.getId() + ":" + e.getDateFinContrat(), "RH", "AVERTISSEMENT", "Fin de contrat",
                    e.fullName() + " (" + e.getTypeContrat() + ") le " + e.getDateFinContrat(), "/rh"));
        }
        if (SecurityUtils.has("QUALITY_VIEW")) {
            anomalies.findByStatutNotOrderByIdDesc("CLOTUREE").stream().limit(10).forEach(a -> add(out, read,
                    "ANOMALIE:" + a.getId() + ":" + a.getStatut(), "QUALITE", "CRITIQUE".equals(a.getGravite()) ? "ALERTE" : "AVERTISSEMENT",
                    "Anomalie " + a.getGravite().toLowerCase(), a.getNumero() + " : " + a.getDescription(), "/qualite"));
        }
        return out;
    }

    private static void add(List<Notification> out, Set<String> read, String key, String type, String niveau, String titre,
                            String message, String lien) {
        String k = key.length() > 120 ? key.substring(0, 120) : key;
        out.add(new Notification(k, type, niveau, titre, message, lien, read.contains(k)));
    }

    @PreAuthorize("isAuthenticated()")
    @Transactional
    public void markRead(List<String> keys) {
        AppUser me = current();
        for (String k : keys) {
            if (k != null && !k.isBlank() && k.length() <= 120 && !reads.existsByUserIdAndKey(me.getId(), k)) {
                reads.save(new NotificationRead(me.getId(), k));
            }
        }
    }

    private AppUser current() {
        return users.findByUsername(SecurityUtils.username()).orElseThrow(() -> new NotFoundException("Utilisateur introuvable."));
    }
}

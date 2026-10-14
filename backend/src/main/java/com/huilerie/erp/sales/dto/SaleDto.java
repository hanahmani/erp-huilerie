package com.huilerie.erp.sales.dto;

import com.huilerie.erp.finance.PaymentMode;
import com.huilerie.erp.sales.Sale;
import com.huilerie.erp.sales.SaleLine;
import com.huilerie.erp.sales.SalePayment;
import com.huilerie.erp.sales.SaleStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public final class SaleDto {

    private SaleDto() {
    }

    // ------------------------------------------------------------ requêtes

    public record LineRequest(
            @NotNull(message = "Produit manquant.") Long articleId,
            Long lotId,
            @NotNull(message = "Quantité manquante.") @DecimalMin(value = "0.001", message = "La quantité doit être positive.")
            BigDecimal quantite,
            @DecimalMin(value = "0", message = "Le prix ne peut pas être négatif.") BigDecimal prixUnitaire) {
    }

    public record PaymentRequest(
            @NotNull(message = "Mode de paiement manquant.") PaymentMode mode,
            @NotNull @DecimalMin(value = "0.001", message = "Le montant doit être positif.") BigDecimal montant) {
    }

    /** idempotencyKey : généré par l'écran de caisse ; renvoyer la même clé ne crée jamais une 2e vente. */
    public record QuickSaleRequest(
            @NotBlank(message = "Clé de vente manquante.") @Size(max = 64) String idempotencyKey,
            Long clientId,
            @DecimalMin(value = "0", message = "La remise ne peut pas être négative.") BigDecimal remise,
            @NotEmpty(message = "Le panier est vide.") @Size(max = 100) List<@Valid LineRequest> lignes,
            @Size(max = 10) List<@Valid PaymentRequest> paiements) {
    }

    public record CancelRequest(@NotBlank(message = "Le motif d'annulation est obligatoire.") @Size(max = 255) String motif) {
    }

    // ------------------------------------------------------------ réponses

    public record Line(Long articleId, Long lotId, String lotNumero, String designation, String unite,
                       BigDecimal quantite, BigDecimal prixUnitaire, BigDecimal total) {
        static Line from(SaleLine l) {
            return new Line(l.getArticle().getId(), l.getLot() == null ? null : l.getLot().getId(),
                    l.getLot() == null ? null : l.getLot().getNumero(), l.getDesignation(), l.getUnite(),
                    l.getQuantite(), l.getPrixUnitaire(), l.getTotalLigne());
        }
    }

    public record Payment(PaymentMode mode, BigDecimal montant) {
        static Payment from(SalePayment p) {
            return new Payment(p.getMode(), p.getMontant());
        }
    }

    public record Detail(Long id, String numeroTicket, Instant date, SaleStatus statut, String paiementStatut,
                         Long clientId, String clientNom, Long caissierId, String caissierNom,
                         BigDecimal sousTotal, BigDecimal remise, BigDecimal total, BigDecimal montantRecu,
                         BigDecimal montantPaye, BigDecimal resteAPayer, BigDecimal monnaieRendue, int nbImpressions,
                         String annuleePar, Instant annuleeLe, String motifAnnulation,
                         List<Line> lignes, List<Payment> paiements, List<Settlement> reglements) {

        public static Detail from(Sale s) {
            return from(s, List.of());
        }

        public static Detail from(Sale s, List<Settlement> reglements) {
            return new Detail(s.getId(), s.getNumeroTicket(), s.getDateVente(), s.getStatut(), paymentStatus(s),
                    s.getClient() == null ? null : s.getClient().getId(),
                    s.getClient() == null ? null : s.getClient().getNom(),
                    s.getCaissier().getId(), s.getCaissier().fullName(),
                    s.getSousTotal(), s.getRemise(), s.getTotal(), s.getMontantRecu(), s.getMontantPaye(),
                    s.getResteAPayer(), s.getMonnaieRendue(), s.getNbImpressions(),
                    s.getAnnuleePar(), s.getAnnuleeLe(), s.getMotifAnnulation(),
                    s.getLignes().stream().map(Line::from).toList(),
                    s.getPaiements().stream().map(Payment::from).toList(), reglements);
        }
    }

    public record Summary(Long id, String numeroTicket, Instant date, String clientNom, String caissierNom,
                          BigDecimal total, BigDecimal montantPaye, BigDecimal resteAPayer, SaleStatus statut,
                          String paiementStatut) {
        public static Summary from(Sale s) {
            return new Summary(s.getId(), s.getNumeroTicket(), s.getDateVente(),
                    s.getClient() == null ? null : s.getClient().getNom(), s.getCaissier().fullName(),
                    s.getTotal(), s.getMontantPaye(), s.getResteAPayer(), s.getStatut(), paymentStatus(s));
        }
    }

    public record Cashier(Long id, String nom) {
    }

    public record SettlementRequest(
            @NotNull(message = "Mode de paiement manquant.") PaymentMode mode,
            @NotNull @DecimalMin(value = "0.001", message = "Le montant doit être positif.") BigDecimal montant,
            @Size(max = 80) String reference) {
    }

    public record Settlement(String numero, Instant date, PaymentMode mode, BigDecimal montant, String reference, String username) {
    }

    public record Receivable(Long clientId, String code, String nom, String telephone, long nbVentes, BigDecimal reste,
                             Instant plusAncienne) {
    }

    /** Article proposé en caisse, avec le stock réellement vendable. */
    public record SellableArticle(Long id, String reference, String nom, String codeBarres, String unite,
                                  String categorie, BigDecimal prixVente, BigDecimal contenance, boolean suiviLot,
                                  BigDecimal disponible, List<SellableLot> lots) {
    }

    public record SellableLot(Long lotId, String numero, BigDecimal quantite) {
    }

    static String paymentStatus(Sale s) {
        if (s.getStatut() == SaleStatus.ANNULEE) {
            return "ANNULEE";
        }
        if (s.getResteAPayer().signum() == 0) {
            return "PAYEE";
        }
        return s.getMontantPaye().signum() == 0 ? "NON_PAYEE" : "PARTIELLE";
    }
}

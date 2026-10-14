package com.huilerie.erp.purchases.dto;

import com.huilerie.erp.finance.PaymentMode;
import com.huilerie.erp.purchases.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public final class PurchaseDto {

    private PurchaseDto() {
    }

    public record LineRequest(
            @NotNull(message = "Article manquant.") Long articleId,
            @NotNull @DecimalMin(value = "0.001", message = "Quantité positive.") BigDecimal quantite,
            @NotNull @DecimalMin(value = "0", message = "Prix positif ou nul.") BigDecimal prixUnitaire,
            @DecimalMin("0") @DecimalMax("100") BigDecimal remisePct,
            Long taxId) {
    }

    public record Request(
            @NotNull(message = "Le fournisseur est obligatoire.") Long fournisseurId,
            @NotNull(message = "La date est obligatoire.") LocalDate dateCommande,
            @Size(max = 60) String refFacture,
            @Size(max = 500) String notes,
            @NotEmpty(message = "Ajoutez au moins une ligne.") @Size(max = 200) List<@Valid LineRequest> lignes) {
    }

    public record ReceiveLine(@NotNull Long lineId,
                              @NotNull @DecimalMin(value = "0.001", message = "Quantité positive.") BigDecimal quantite,
                              @Size(max = 50) String lotNumero) {
    }

    public record ReceiveRequest(@NotEmpty(message = "Indiquez les quantités reçues.") List<@Valid ReceiveLine> lignes,
                                 @Size(max = 255) String notes) {
    }

    public record PaymentRequest(@NotNull(message = "Mode obligatoire.") PaymentMode mode,
                                 @NotNull @DecimalMin(value = "0.001", message = "Montant positif.") BigDecimal montant,
                                 @Size(max = 80) String reference) {
    }

    public record CancelRequest(@NotBlank(message = "Le motif est obligatoire.") @Size(max = 255) String motif) {
    }

    public record InvoiceRefRequest(@Size(max = 60) String refFacture) {
    }

    // ------------------------------------------------------------ réponses

    public record Line(Long id, Long articleId, String designation, String unite, boolean suiviLot,
                       BigDecimal quantite, BigDecimal quantiteRecue, BigDecimal prixUnitaire, BigDecimal remisePct,
                       BigDecimal tauxTva, BigDecimal totalHt, BigDecimal totalTva) {
        static Line from(PurchaseLine l) {
            return new Line(l.getId(), l.getArticle().getId(), l.getDesignation(), l.getArticle().getUnite().getCode(),
                    l.getArticle().isSuiviLot(), l.getQuantite(), l.getQuantiteRecue(), l.getPrixUnitaire(),
                    l.getRemisePct(), l.getTauxTva(), l.getTotalHt(), l.getTotalTva());
        }
    }

    public record Receipt(Long id, String numero, Instant date, String username, String notes, List<ReceiptLine> lignes) {
        static Receipt from(GoodsReceipt r) {
            return new Receipt(r.getId(), r.getNumero(), r.getDate(), r.getUsername(), r.getNotes(),
                    r.getLignes().stream().map(l -> new ReceiptLine(l.getPurchaseLine().getDesignation(),
                            l.getLot() == null ? null : l.getLot().getNumero(), l.getQuantite())).toList());
        }
    }

    public record ReceiptLine(String designation, String lot, BigDecimal quantite) {
    }

    public record Payment(String numero, Instant date, PaymentMode mode, BigDecimal montant, String reference, String username) {
        static Payment from(SupplierPayment p) {
            return new Payment(p.getNumero(), p.getDate(), p.getMode(), p.getMontant(), p.getReference(), p.getUsername());
        }
    }

    public record Detail(Long id, String numero, Long fournisseurId, String fournisseurNom, LocalDate dateCommande,
                         PurchaseStatus statut, String refFacture, String notes, BigDecimal totalHt, BigDecimal totalTva,
                         BigDecimal totalTtc, BigDecimal montantPaye, BigDecimal reste, String creePar, Instant createdAt,
                         String validePar, Instant valideLe, String annulePar, String motifAnnulation,
                         List<Line> lignes, List<Receipt> receptions, List<Payment> paiements) {
        public static Detail from(Purchase p) {
            return new Detail(p.getId(), p.getNumero(), p.getFournisseur().getId(), p.getFournisseur().getNom(),
                    p.getDateCommande(), p.getStatut(), p.getRefFacture(), p.getNotes(), p.getTotalHt(), p.getTotalTva(),
                    p.getTotalTtc(), p.getMontantPaye(), p.reste(), p.getCreePar(), p.getCreatedAt(), p.getValidePar(),
                    p.getValideLe(), p.getAnnulePar(), p.getMotifAnnulation(),
                    p.getLignes().stream().map(Line::from).toList(),
                    p.getReceptions().stream().map(Receipt::from).toList(),
                    p.getPaiements().stream().map(Payment::from).toList());
        }
    }

    public record Summary(Long id, String numero, String fournisseurNom, LocalDate dateCommande, PurchaseStatus statut,
                          String refFacture, BigDecimal totalTtc, BigDecimal montantPaye, BigDecimal reste) {
        public static Summary from(Purchase p) {
            return new Summary(p.getId(), p.getNumero(), p.getFournisseur().getNom(), p.getDateCommande(), p.getStatut(),
                    p.getRefFacture(), p.getTotalTtc(), p.getMontantPaye(), p.reste());
        }
    }

    public record Payable(Long fournisseurId, String code, String nom, long nbCommandes, BigDecimal reste) {
    }
}

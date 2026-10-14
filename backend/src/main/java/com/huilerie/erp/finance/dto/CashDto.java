package com.huilerie.erp.finance.dto;

import com.huilerie.erp.common.response.PageResponse;
import com.huilerie.erp.finance.CashMovement;
import com.huilerie.erp.finance.CashMovementType;
import com.huilerie.erp.finance.PaymentMode;

import java.math.BigDecimal;
import java.time.Instant;

public final class CashDto {

    private CashDto() {
    }

    public record Line(Long id, Instant date, CashMovementType type, PaymentMode mode, BigDecimal montant,
                       String libelle, String referenceType, Long referenceId, String username) {
        public static Line from(CashMovement m) {
            return new Line(m.getId(), m.getCreatedAt(), m.getType(), m.getMode(), m.getMontant(), m.getLibelle(),
                    m.getReferenceType(), m.getReferenceId(), m.getUsername());
        }
    }

    /** Journal paginé + totaux de la période filtrée. */
    public record Journal(PageResponse<Line> lignes, BigDecimal encaissements, BigDecimal decaissements,
                          BigDecimal solde, BigDecimal especes) {
    }

    public record ExpenseRequest(
            @jakarta.validation.constraints.NotNull(message = "La catégorie est obligatoire.") Long categorieId,
            @jakarta.validation.constraints.NotBlank(message = "Le libellé est obligatoire.")
            @jakarta.validation.constraints.Size(max = 255) String libelle,
            @jakarta.validation.constraints.NotNull
            @jakarta.validation.constraints.DecimalMin(value = "0.001", message = "Montant positif.") BigDecimal montant,
            @jakarta.validation.constraints.NotNull(message = "Le mode de paiement est obligatoire.") PaymentMode mode,
            @jakarta.validation.constraints.Size(max = 80) String reference,
            @jakarta.validation.constraints.NotNull(message = "La date est obligatoire.") java.time.LocalDate dateDepense) {
    }

    public record ExpenseLine(Long id, String numero, java.time.LocalDate date, String categorie, String libelle,
                              BigDecimal montant, PaymentMode mode, String reference, String statut, String username,
                              String motifAnnulation) {
        public static ExpenseLine from(com.huilerie.erp.finance.Expense e) {
            return new ExpenseLine(e.getId(), e.getNumero(), e.getDateDepense(), e.getCategorie().getNom(), e.getLibelle(),
                    e.getMontant(), e.getMode(), e.getReference(), e.getStatut(), e.getUsername(), e.getMotifAnnulation());
        }
    }

    public record CancelRequest(@jakarta.validation.constraints.NotBlank(message = "Le motif est obligatoire.")
                                @jakarta.validation.constraints.Size(max = 255) String motif) {
    }

    public record OpenSessionRequest(@jakarta.validation.constraints.NotNull
                                     @jakarta.validation.constraints.DecimalMin(value = "0", message = "Fond positif ou nul.")
                                     BigDecimal fondInitial) {
    }

    public record CloseSessionRequest(@jakarta.validation.constraints.NotNull
                                      @jakarta.validation.constraints.DecimalMin(value = "0", message = "Montant positif ou nul.")
                                      BigDecimal especesComptees,
                                      @jakarta.validation.constraints.Size(max = 255) String justification) {
    }

    /** especesTheoriques = fond initial + espèces encaissées − espèces décaissées depuis l'ouverture. */
    public record Session(Long id, boolean ouverte, String ouvertePar, Instant ouverteLe, BigDecimal fondInitial,
                          BigDecimal entreesEspeces, BigDecimal sortiesEspeces, BigDecimal especesTheoriques,
                          String fermeePar, Instant fermeeLe, BigDecimal especesComptees, BigDecimal ecart,
                          String justification) {
    }
}

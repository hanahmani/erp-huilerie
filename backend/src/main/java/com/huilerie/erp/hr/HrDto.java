package com.huilerie.erp.hr;

import com.huilerie.erp.finance.PaymentMode;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public final class HrDto {

    private HrDto() {
    }

    public record EmployeeRequest(
            @NotBlank(message = "Le prénom est obligatoire.") @Size(max = 100) String prenom,
            @NotBlank(message = "Le nom est obligatoire.") @Size(max = 100) String nom,
            @Size(max = 20) String cin,
            @Size(max = 50) String telephone,
            @Size(max = 255) String adresse,
            @NotNull(message = "Le TJM est obligatoire.")
            @DecimalMin(value = "0", message = "Le TJM ne peut pas être négatif.") BigDecimal tjm) {
    }

    /**
     * Fiche avec le compte : journées pointées × TJM figé − paiements = reste dû
     * (négatif quand les avances dépassent ce qui est gagné). enCours : journée du jour démarrée et non arrêtée.
     */
    public record EmployeeResponse(Long id, String matricule, String prenom, String nom, String cin, String telephone,
                                   String adresse, BigDecimal tjm, boolean actif, long jours, BigDecimal gagne,
                                   BigDecimal paye, BigDecimal reste, WorkDayLine aujourdhui, boolean enCours) {
    }

    /** Correction ou saisie manuelle d'une journée ; heureFin vide = journée encore en cours. */
    public record WorkDayRequest(Long employeeId,
                                 @NotNull(message = "L'heure de début est obligatoire.") Instant heureDebut,
                                 Instant heureFin) {
    }

    public record WorkDayLine(Long id, Long employeeId, String employe, String matricule, LocalDate jour,
                              Instant heureDebut, Instant heureFin, Long minutes, BigDecimal tjm, String saisiPar,
                              String modifiePar) {
        static WorkDayLine from(WorkDay w) {
            Long minutes = w.getHeureFin() == null ? null
                    : java.time.Duration.between(w.getHeureDebut(), w.getHeureFin()).toMinutes();
            return new WorkDayLine(w.getId(), w.getEmployee().getId(), w.getEmployee().fullName(),
                    w.getEmployee().getMatricule(), w.getJour(), w.getHeureDebut(), w.getHeureFin(), minutes, w.getTjm(),
                    w.getSaisiPar(), w.getModifiePar());
        }
    }

    public record PaymentRequest(@NotNull(message = "Le type est obligatoire.") EmployeePayment.Type type,
                                 @NotNull(message = "Le mode est obligatoire.") PaymentMode mode,
                                 @NotNull @DecimalMin(value = "0.001", message = "Le montant doit être positif.") BigDecimal montant,
                                 @Size(max = 120) String reference) {
    }

    public record PaymentLine(Long id, String numero, Long employeeId, String employe, String matricule,
                              EmployeePayment.Type type, PaymentMode mode, BigDecimal montant, String reference,
                              Instant date, String username) {
        static PaymentLine from(EmployeePayment p) {
            return new PaymentLine(p.getId(), p.getNumero(), p.getEmployee().getId(), p.getEmployee().fullName(),
                    p.getEmployee().getMatricule(), p.getType(), p.getMode(), p.getMontant(), p.getReference(),
                    p.getDate(), p.getUsername());
        }
    }
}

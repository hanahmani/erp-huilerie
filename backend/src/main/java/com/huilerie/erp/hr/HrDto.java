package com.huilerie.erp.hr;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public final class HrDto {

    private HrDto() {
    }

    public record RefRequest(@NotBlank(message = "Le nom est obligatoire.") @Size(max = 100) String nom) {
    }

    public record Ref(Long id, String nom, boolean actif) {
    }

    public record EmployeeRequest(
            @NotBlank(message = "Le nom est obligatoire.") @Size(max = 100) String nom,
            @NotBlank(message = "Le prénom est obligatoire.") @Size(max = 100) String prenom,
            LocalDate dateNaissance, @Size(max = 20) String cin, @Size(max = 50) String telephone,
            @Email(message = "Adresse e-mail invalide.") @Size(max = 120) String email, @Size(max = 255) String adresse,
            Long serviceId, Long posteId,
            @NotNull(message = "La date d'embauche est obligatoire.") LocalDate dateEmbauche,
            @NotNull @Pattern(regexp = "CDI|CDD|SAISONNIER|STAGE|AUTRE", message = "Type de contrat invalide.") String typeContrat,
            LocalDate dateFinContrat,
            @NotNull @Pattern(regexp = "ACTIF|SUSPENDU|SORTI") String statut,
            @NotNull @DecimalMin(value = "0", message = "Solde positif ou nul.") BigDecimal soldeConges,
            Long userId) {
    }

    public record EmployeeResponse(Long id, String matricule, String nom, String prenom, LocalDate dateNaissance, String cin,
                                   String telephone, String email, String adresse, Long serviceId, String service,
                                   Long posteId, String poste, LocalDate dateEmbauche, String typeContrat,
                                   LocalDate dateFinContrat, String statut, BigDecimal soldeConges, Long userId,
                                   String username) {
        static EmployeeResponse from(Employee e) {
            return new EmployeeResponse(e.getId(), e.getMatricule(), e.getNom(), e.getPrenom(), e.getDateNaissance(),
                    e.getCin(), e.getTelephone(), e.getEmail(), e.getAdresse(),
                    e.getService() == null ? null : e.getService().getId(), e.getService() == null ? null : e.getService().getNom(),
                    e.getPoste() == null ? null : e.getPoste().getId(), e.getPoste() == null ? null : e.getPoste().getNom(),
                    e.getDateEmbauche(), e.getTypeContrat(), e.getDateFinContrat(), e.getStatut(), e.getSoldeConges(),
                    e.getUser() == null ? null : e.getUser().getId(), e.getUser() == null ? null : e.getUser().getUsername());
        }
    }

    public record LeaveRequestBody(Long employeeId,
                                   @NotNull @Pattern(regexp = "CONGE_ANNUEL|MALADIE|SANS_SOLDE|EXCEPTIONNEL") String type,
                                   @NotNull(message = "Date de début obligatoire.") LocalDate dateDebut,
                                   @NotNull(message = "Date de fin obligatoire.") LocalDate dateFin,
                                   @Size(max = 255) String motif) {
    }

    public record DecisionRequest(boolean accepter, @Size(max = 255) String motifRefus) {
    }

    public record Leave(Long id, Long employeeId, String employe, String matricule, String type, LocalDate dateDebut,
                        LocalDate dateFin, BigDecimal jours, String motif, String statut, String demandePar,
                        Instant demandeLe, String decidePar, Instant decideLe, String motifRefus, BigDecimal soldeActuel) {
        static Leave from(LeaveRequest l) {
            return new Leave(l.getId(), l.getEmployee().getId(), l.getEmployee().fullName(), l.getEmployee().getMatricule(),
                    l.getType(), l.getDateDebut(), l.getDateFin(), l.getJours(), l.getMotif(), l.getStatut(), l.getDemandePar(),
                    l.getDemandeLe(), l.getDecidePar(), l.getDecideLe(), l.getMotifRefus(), l.getEmployee().getSoldeConges());
        }
    }

    public record AbsenceRequest(@NotNull Long employeeId, @NotNull LocalDate jour,
                                 @NotNull @DecimalMin("0.5") @DecimalMax("1") BigDecimal duree,
                                 @Size(max = 255) String motif, boolean justifiee) {
    }

    public record AbsenceResponse(Long id, Long employeeId, String employe, LocalDate jour, BigDecimal duree, String motif,
                                  boolean justifiee, String saisiPar) {
        static AbsenceResponse from(Absence a) {
            return new AbsenceResponse(a.getId(), a.getEmployee().getId(), a.getEmployee().fullName(), a.getJour(),
                    a.getDuree(), a.getMotif(), a.isJustifiee(), a.getSaisiPar());
        }
    }

    public record HolidayRequest(@NotNull LocalDate jour, @NotBlank @Size(max = 100) String libelle) {
    }

    public record Holiday(Long id, LocalDate jour, String libelle) {
    }
}

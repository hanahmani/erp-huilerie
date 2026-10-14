package com.huilerie.erp.quality;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public final class QualityDto {

    private QualityDto() {
    }

    public record CriterionRequest(@NotBlank(message = "Le nom est obligatoire.") @Size(max = 100) String nom,
                                   @Size(max = 30) String unite, BigDecimal valeurMin, BigDecimal valeurMax) {
    }

    public record Criterion(Long id, String nom, String unite, BigDecimal valeurMin, BigDecimal valeurMax, boolean actif) {
        static Criterion from(QualityCriterion c) {
            return new Criterion(c.getId(), c.getNom(), c.getUnite(), c.getValeurMin(), c.getValeurMax(), c.isActif());
        }
    }

    public record ResultRequest(@NotNull Long criterionId, @NotNull BigDecimal valeur) {
    }

    public record ControlRequest(@NotNull(message = "Le lot est obligatoire.") Long lotId,
                                 @NotEmpty(message = "Saisissez au moins un résultat.") List<@Valid ResultRequest> resultats,
                                 @Size(max = 500) String observations, boolean appliquerDecision) {
    }

    public record ResultLine(String critere, String unite, BigDecimal min, BigDecimal max, BigDecimal valeur, boolean conforme) {
    }

    public record Control(Long id, String numero, Long lotId, String lotNumero, String article, Instant date,
                          String controleur, String resultat, String observations, List<ResultLine> resultats) {
        static Control from(QualityControl c) {
            return new Control(c.getId(), c.getNumero(), c.getLot().getId(), c.getLot().getNumero(),
                    c.getLot().getArticle().getNom(), c.getDate(), c.getControleur(), c.getResultat(), c.getObservations(),
                    c.getResultats().stream().map(r -> new ResultLine(r.getCriterion().getNom(), r.getCriterion().getUnite(),
                            r.getCriterion().getValeurMin(), r.getCriterion().getValeurMax(), r.getValeur(), r.isConforme())).toList());
        }
    }

    public record AnomalyRequest(Long lotId, @NotBlank(message = "La description est obligatoire.") @Size(max = 500) String description,
                                 @NotNull @Pattern(regexp = "MINEURE|MAJEURE|CRITIQUE") String gravite) {
    }

    public record AnomalyUpdate(@Size(max = 500) String actionCorrective,
                                @NotNull @Pattern(regexp = "OUVERTE|EN_COURS|CLOTUREE") String statut) {
    }

    public record Anomaly(Long id, String numero, String lotNumero, String controle, String description, String gravite,
                          String actionCorrective, String statut, String creePar, Instant createdAt, Instant clotureLe) {
        static Anomaly from(QualityAnomaly a) {
            return new Anomaly(a.getId(), a.getNumero(), a.getLot() == null ? null : a.getLot().getNumero(),
                    a.getControl() == null ? null : a.getControl().getNumero(), a.getDescription(), a.getGravite(),
                    a.getActionCorrective(), a.getStatut(), a.getCreePar(), a.getCreatedAt(), a.getClotureLe());
        }
    }
}

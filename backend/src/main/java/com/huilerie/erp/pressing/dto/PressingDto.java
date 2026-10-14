package com.huilerie.erp.pressing.dto;

import com.huilerie.erp.finance.PaymentMode;
import com.huilerie.erp.pressing.Pressing;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public final class PressingDto {

    private PressingDto() {
    }

    /** Bon de pesée. tarifKg et articleId : facultatifs, sinon valeurs des paramètres. */
    public record ReceiveRequest(
            @NotNull(message = "Le client est obligatoire.") Long clientId,
            Long articleId,
            @Size(max = 80) String variete,
            @Size(max = 120) String provenance,
            @NotNull(message = "L'état des olives est obligatoire.") Pressing.OliveState etatOlives,
            @NotNull @DecimalMin(value = "0.001", message = "Le poids brut doit être positif.") BigDecimal poidsBrut,
            @NotNull @DecimalMin(value = "0", message = "La tare ne peut pas être négative.") BigDecimal tare,
            @DecimalMin(value = "0", message = "Le tarif ne peut pas être négatif.") BigDecimal tarifKg,
            @Size(max = 255) String notes) {
    }

    public record FinishRequest(
            @NotNull @DecimalMin(value = "0.001", message = "La quantité d'huile doit être positive.") BigDecimal huileObtenue,
            @DecimalMin(value = "0", message = "Les pertes ne peuvent pas être négatives.") BigDecimal pertesKg) {
    }

    public record ReturnRequest(
            @NotNull @DecimalMin(value = "0.001", message = "La quantité doit être positive.") BigDecimal quantite,
            @Size(max = 150) String remisA) {
    }

    public record PaymentRequest(@NotNull(message = "Mode obligatoire.") PaymentMode mode,
                                 @NotNull @DecimalMin(value = "0.001", message = "Montant positif.") BigDecimal montant,
                                 @Size(max = 80) String reference) {
    }

    public record CancelRequest(@NotBlank(message = "Le motif est obligatoire.") @Size(max = 255) String motif) {
    }

    public record ReturnLine(String numero, Instant date, BigDecimal quantite, String remisA, String username) {
    }

    public record PaymentLine(String numero, Instant date, PaymentMode mode, BigDecimal montant, String reference, String username) {
    }

    public record Detail(Long id, String numero, Pressing.Status statut, Long clientId, String clientNom, String clientCode,
                         String clientTelephone, Long articleId, String articleNom, String unite, String lotNumero,
                         Instant dateReception, String variete, String provenance, Pressing.OliveState etatOlives,
                         BigDecimal poidsBrut, BigDecimal tare, BigDecimal poidsNet, BigDecimal tarifKg,
                         BigDecimal montant, BigDecimal montantPaye, BigDecimal reste,
                         BigDecimal huileObtenue, BigDecimal huileRemise, BigDecimal huileARemettre, BigDecimal pertesKg,
                         BigDecimal rendement, Instant dateDebut, Instant dateFin, String notes, String recuPar,
                         String motifAnnulation, List<ReturnLine> remises, List<PaymentLine> paiements) {

        public static Detail from(Pressing p) {
            return new Detail(p.getId(), p.getNumero(), p.getStatut(), p.getClient().getId(), p.getClient().getNom(),
                    p.getClient().getCode(), p.getClient().getTelephone(), p.getArticle().getId(), p.getArticle().getNom(),
                    p.getArticle().getUnite().getCode(), p.getLot() == null ? null : p.getLot().getNumero(),
                    p.getDateReception(), p.getVariete(), p.getProvenance(), p.getEtatOlives(), p.getPoidsBrut(),
                    p.getTare(), p.getPoidsNet(), p.getTarifKg(), p.getMontant(), p.getMontantPaye(), p.reste(),
                    p.getHuileObtenue(), p.getHuileRemise(), p.huileARemettre(), p.getPertesKg(), p.rendement(),
                    p.getDateDebut(), p.getDateFin(), p.getNotes(), p.getRecuPar(), p.getMotifAnnulation(),
                    p.getRemises().stream().map(r -> new ReturnLine(r.getNumero(), r.getDate(), r.getQuantite(),
                            r.getRemisA(), r.getUsername())).toList(),
                    p.getPaiements().stream().map(x -> new PaymentLine(x.getNumero(), x.getDate(), x.getMode(),
                            x.getMontant(), x.getReference(), x.getUsername())).toList());
        }
    }

    public record Summary(Long id, String numero, Pressing.Status statut, String clientNom, Instant dateReception,
                          BigDecimal poidsNet, BigDecimal huileObtenue, BigDecimal huileARemettre, BigDecimal rendement,
                          BigDecimal montant, BigDecimal reste) {
        public static Summary from(Pressing p) {
            return new Summary(p.getId(), p.getNumero(), p.getStatut(), p.getClient().getNom(), p.getDateReception(),
                    p.getPoidsNet(), p.getHuileObtenue(), p.huileARemettre(), p.rendement(), p.getMontant(), p.reste());
        }
    }
}

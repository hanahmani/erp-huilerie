package com.huilerie.erp.packaging.dto;

import com.huilerie.erp.packaging.PackagingOrder;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public final class PackagingDto {

    private PackagingDto() {
    }

    public record ConsumptionRequest(@NotNull Long articleId,
                                     @NotNull @DecimalMin(value = "0.001", message = "Quantité positive.") BigDecimal quantite) {
    }

    public record Request(@NotNull(message = "L'huile en vrac est obligatoire.") Long sourceArticleId,
                          Long sourceLotId,
                          @NotNull(message = "Le produit fini est obligatoire.") Long produitArticleId,
                          @NotNull @DecimalMin(value = "0.001", message = "Quantité produite positive.") BigDecimal quantiteProduite,
                          @Size(max = 20) List<@Valid ConsumptionRequest> consommations,
                          @Size(max = 255) String notes) {
    }

    public record CancelRequest(@NotBlank(message = "Le motif est obligatoire.") @Size(max = 255) String motif) {
    }

    public record Consumption(String article, String unite, BigDecimal quantite) {
    }

    public record Detail(Long id, String numero, String statut, String source, String sourceUnite, String sourceLot,
                         String produit, String produitUnite, String produitLot, BigDecimal quantiteProduite,
                         BigDecimal volumeUtilise, String notes, String username, Instant createdAt, String annulePar,
                         String motifAnnulation, List<Consumption> consommations) {
        public static Detail from(PackagingOrder o) {
            return new Detail(o.getId(), o.getNumero(), o.getStatut(), o.getSource().getNom(),
                    o.getSource().getUnite().getCode(), o.getSourceLot() == null ? null : o.getSourceLot().getNumero(),
                    o.getProduit().getNom(), o.getProduit().getUnite().getCode(),
                    o.getProduitLot() == null ? null : o.getProduitLot().getNumero(), o.getQuantiteProduite(),
                    o.getVolumeUtilise(), o.getNotes(), o.getUsername(), o.getCreatedAt(), o.getAnnulePar(),
                    o.getMotifAnnulation(), o.getConsommations().stream().map(c -> new Consumption(c.getArticle().getNom(),
                            c.getArticle().getUnite().getCode(), c.getQuantite())).toList());
        }
    }
}

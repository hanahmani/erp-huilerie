package com.huilerie.erp.production.dto;

import com.huilerie.erp.production.OilCategory;
import com.huilerie.erp.production.ProductionOrder;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public final class ProductionDto {

    private ProductionDto() {
    }

    public record InputRequest(@NotNull Long articleId, Long lotId,
                               @NotNull @DecimalMin(value = "0.001", message = "Quantité positive.") BigDecimal quantite) {
    }

    public record PlanRequest(@NotNull(message = "La date est obligatoire.") LocalDate datePrevue,
                              @Size(max = 60) String ligne,
                              @NotNull(message = "L'article huile produit est obligatoire.") Long huileArticleId,
                              @NotEmpty(message = "Ajoutez au moins un lot d'olives.") List<@Valid InputRequest> entrees,
                              @Size(max = 255) String notes) {
    }

    public record ByProductRequest(@NotNull Long articleId,
                                   @NotNull @DecimalMin(value = "0.001", message = "Quantité positive.") BigDecimal quantite) {
    }

    /** bloquerLot : l'huile reste bloquée jusqu'à l'analyse qualité complète. */
    public record FinishRequest(@NotNull @DecimalMin(value = "0.001", message = "Quantité d'huile positive.") BigDecimal huileLitres,
                                @DecimalMin(value = "0", message = "Pertes positives ou nulles.") BigDecimal pertesKg,
                                @DecimalMin(value = "0", message = "Acidité positive ou nulle.") @DecimalMax("100") BigDecimal acidite,
                                @Size(max = 50) String lotNumero,
                                boolean bloquerLot,
                                List<@Valid ByProductRequest> sousProduits) {
    }

    public record CancelRequest(@NotBlank(message = "Le motif est obligatoire.") @Size(max = 255) String motif) {
    }

    public record Input(Long articleId, String article, String unite, Long lotId, String lot, BigDecimal quantite) {
    }

    public record ByProduct(String article, String unite, BigDecimal quantite) {
    }

    public record Detail(Long id, String numero, LocalDate datePrevue, ProductionOrder.Status statut, String ligne,
                         Long huileArticleId, String huileArticle, String huileUnite, String huileLot,
                         BigDecimal olivesKg, BigDecimal huileLitres, BigDecimal pertesKg, BigDecimal rendement,
                         BigDecimal acidite, OilCategory categorie, String notes, String creePar, Instant createdAt,
                         Instant demarreLe, Instant termineLe, String terminePar, String motifAnnulation,
                         List<Input> entrees, List<ByProduct> sousProduits) {
        public static Detail from(ProductionOrder o) {
            return new Detail(o.getId(), o.getNumero(), o.getDatePrevue(), o.getStatut(), o.getLigne(),
                    o.getHuileArticle().getId(), o.getHuileArticle().getNom(), o.getHuileArticle().getUnite().getCode(),
                    o.getHuileLot() == null ? null : o.getHuileLot().getNumero(), o.getOlivesKg(), o.getHuileLitres(),
                    o.getPertesKg(), o.getRendement(), o.getAcidite(), o.getCategorie(), o.getNotes(), o.getCreePar(),
                    o.getCreatedAt(), o.getDemarreLe(), o.getTermineLe(), o.getTerminePar(), o.getMotifAnnulation(),
                    o.getEntrees().stream().map(i -> new Input(i.getArticle().getId(), i.getArticle().getNom(),
                            i.getArticle().getUnite().getCode(), i.getLot() == null ? null : i.getLot().getId(),
                            i.getLot() == null ? null : i.getLot().getNumero(),
                            i.getQuantite())).toList(),
                    o.getSousProduits().stream().map(b -> new ByProduct(b.getArticle().getNom(),
                            b.getArticle().getUnite().getCode(), b.getQuantite())).toList());
        }
    }

    public record Summary(Long id, String numero, LocalDate datePrevue, ProductionOrder.Status statut, String ligne,
                          String huileArticle, BigDecimal olivesKg, BigDecimal huileLitres, BigDecimal rendement,
                          OilCategory categorie) {
        public static Summary from(ProductionOrder o) {
            return new Summary(o.getId(), o.getNumero(), o.getDatePrevue(), o.getStatut(), o.getLigne(),
                    o.getHuileArticle().getNom(), o.getOlivesKg(), o.getHuileLitres(), o.getRendement(), o.getCategorie());
        }
    }
}

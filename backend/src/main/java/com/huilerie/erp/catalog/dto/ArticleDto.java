package com.huilerie.erp.catalog.dto;

import com.huilerie.erp.catalog.Article;
import com.huilerie.erp.catalog.ArticleType;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public final class ArticleDto {

    private ArticleDto() {
    }

    public record Request(
            @NotBlank(message = "La référence est obligatoire.") @Size(max = 50) String reference,
            @NotBlank(message = "Le nom est obligatoire.") @Size(max = 150) String nom,
            @Size(max = 500) String description,
            @NotNull(message = "Le type est obligatoire.") ArticleType type,
            Long categorieId,
            @NotNull(message = "L'unité est obligatoire.") Long uniteId,
            @DecimalMin(value = "0.001", message = "La contenance doit être positive.") BigDecimal contenance,
            @DecimalMin(value = "0", message = "Le prix d'achat ne peut pas être négatif.") BigDecimal prixAchat,
            @DecimalMin(value = "0", message = "Le prix de vente ne peut pas être négatif.") BigDecimal prixVente,
            @DecimalMin(value = "0", message = "Le seuil ne peut pas être négatif.") BigDecimal seuilMin,
            @DecimalMin(value = "0", message = "Le stock maximum ne peut pas être négatif.") BigDecimal stockMax,
            @Size(max = 64) String codeBarres,
            boolean vendable,
            boolean suiviLot) {
    }

    public record Response(Long id, String reference, String nom, String description, ArticleType type,
                           Long categorieId, String categorieNom, Long uniteId, String uniteCode,
                           BigDecimal contenance, BigDecimal prixAchat, BigDecimal prixVente, BigDecimal seuilMin,
                           String codeBarres, boolean vendable, boolean suiviLot, boolean actif, BigDecimal stockMax) {

        public static Response from(Article a) {
            return new Response(a.getId(), a.getReference(), a.getNom(), a.getDescription(), a.getType(),
                    a.getCategorie() == null ? null : a.getCategorie().getId(),
                    a.getCategorie() == null ? null : a.getCategorie().getNom(),
                    a.getUnite().getId(), a.getUnite().getCode(), a.getContenance(), a.getPrixAchat(),
                    a.getPrixVente(), a.getSeuilMin(), a.getCodeBarres(), a.isVendable(), a.isSuiviLot(), a.isActif(),
                    a.getStockMax());
        }
    }
}

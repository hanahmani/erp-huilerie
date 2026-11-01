package com.huilerie.erp.inventory.dto;

import com.huilerie.erp.inventory.StorageTank;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public final class TankDto {

    private TankDto() {
    }

    /** articleId et code : pris en compte à la création seulement (ils définissent le lot du puits). */
    public record Request(
            @Pattern(regexp = "^[A-Za-z0-9][A-Za-z0-9 _-]{0,29}$", message = "Code : lettres, chiffres, espace, - ou _ (30 au maximum).") String code,
            @NotBlank(message = "Le nom est obligatoire.") @Size(max = 100) String nom,
            @NotNull(message = "Le type est obligatoire.") StorageTank.Type type,
            Long articleId,
            @NotNull(message = "La capacité est obligatoire.")
            @DecimalMin(value = "0.001", message = "La capacité doit être positive.") BigDecimal capacite) {
    }

    public record Response(Long id, String code, String nom, StorageTank.Type type, Long articleId, String articleNom,
                           String unite, Long lotId, String lotNumero, BigDecimal capacite, BigDecimal quantite,
                           BigDecimal placeRestante, boolean bloque, boolean actif) {

        public static Response from(StorageTank t, BigDecimal quantite) {
            return new Response(t.getId(), t.getCode(), t.getNom(), t.getType(), t.getArticle().getId(),
                    t.getArticle().getNom(), t.getArticle().getUnite().getCode(), t.getLot().getId(),
                    t.getLot().getNumero(), t.getCapacite(), quantite, t.getCapacite().subtract(quantite),
                    !t.getLot().isSellable(), t.isActif());
        }
    }
}

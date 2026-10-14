package com.huilerie.erp.settings.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/** DTO des petits référentiels de paramétrage. */
public final class RefDto {

    private RefDto() {
    }

    public record TaxRequest(@NotBlank(message = "Le nom est obligatoire.") @Size(max = 60) String nom,
                             @NotNull(message = "Le taux est obligatoire.")
                             @DecimalMin(value = "0", message = "Taux entre 0 et 100.")
                             @DecimalMax(value = "100", message = "Taux entre 0 et 100.") BigDecimal taux) {
    }

    public record TaxResponse(Long id, String nom, BigDecimal taux, boolean actif) {
    }

    public record ExpenseCategoryRequest(@NotBlank(message = "Le nom est obligatoire.") @Size(max = 100) String nom) {
    }

    public record ExpenseCategoryResponse(Long id, String nom, boolean actif) {
    }
}

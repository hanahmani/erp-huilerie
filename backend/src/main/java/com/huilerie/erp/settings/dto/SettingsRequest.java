package com.huilerie.erp.settings.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SettingsRequest(
        @NotBlank(message = "La raison sociale est obligatoire.") @Size(max = 150) String raisonSociale,
        @Size(max = 255) String adresse,
        @Size(max = 50) String telephone,
        @Email(message = "Adresse e-mail invalide.") @Size(max = 120) String email,
        @Size(max = 50) String matriculeFiscal,
        @Size(max = 255) String messageTicket,
        boolean venteCreditAutorisee,
        @jakarta.validation.constraints.DecimalMin(value = "0", message = "Le tarif ne peut pas être négatif.")
        java.math.BigDecimal tarifPressageKg,
        Long pressageArticleId,
        @jakarta.validation.constraints.DecimalMin(value = "0", message = "Le timbre ne peut pas être négatif.")
        java.math.BigDecimal timbreFiscal,
        @jakarta.validation.constraints.Min(value = 5, message = "5 minutes minimum.")
        @jakarta.validation.constraints.Max(value = 480, message = "480 minutes maximum.") Integer inactiviteMinutes
) {
}

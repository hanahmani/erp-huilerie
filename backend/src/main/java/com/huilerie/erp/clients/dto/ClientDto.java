package com.huilerie.erp.clients.dto;

import com.huilerie.erp.clients.Client;
import com.huilerie.erp.clients.ClientType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;

public final class ClientDto {

    private ClientDto() {
    }

    public record Request(
            @NotNull(message = "Le type est obligatoire.") ClientType type,
            @NotBlank(message = "Le nom est obligatoire.") @Size(max = 150) String nom,
            @Size(max = 50) String telephone,
            @Size(max = 255) String adresse,
            @Size(max = 20) String cin,
            @Size(max = 50) String matriculeFiscal,
            @DecimalMin(value = "0", message = "Le plafond ne peut pas être négatif.") BigDecimal plafondCredit,
            @jakarta.validation.constraints.Email(message = "Adresse e-mail invalide.") @Size(max = 120) String email,
            @Size(max = 100) String conditionsPaiement) {
    }

    /** solde = total restant à payer sur les ventes validées. */
    public record Response(Long id, String code, ClientType type, String nom, String telephone, String adresse,
                           String cin, String matriculeFiscal, BigDecimal plafondCredit, BigDecimal solde,
                           boolean actif, Instant createdAt, String email, String conditionsPaiement) {

        public static Response from(Client c, BigDecimal solde) {
            return new Response(c.getId(), c.getCode(), c.getType(), c.getNom(), c.getTelephone(), c.getAdresse(),
                    c.getCin(), c.getMatriculeFiscal(), c.getPlafondCredit(), solde, c.isActif(), c.getCreatedAt(),
                    c.getEmail(), c.getConditionsPaiement());
        }
    }
}

package com.huilerie.erp.suppliers.dto;

import com.huilerie.erp.suppliers.Supplier;
import com.huilerie.erp.suppliers.SupplierType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;

public final class SupplierDto {

    private SupplierDto() {
    }

    public record Request(
            @NotNull(message = "Le type est obligatoire.") SupplierType type,
            @NotBlank(message = "Le nom est obligatoire.") @Size(max = 150) String nom,
            @Size(max = 50) String telephone,
            @Size(max = 255) String adresse,
            @Size(max = 20) String cin,
            @Size(max = 50) String matriculeFiscal,
            @jakarta.validation.constraints.Email(message = "Adresse e-mail invalide.") @Size(max = 120) String email,
            @Size(max = 100) String conditionsPaiement,
            @Size(max = 100) String banque,
            @jakarta.validation.constraints.Pattern(regexp = "^$|^[0-9 ]{20,34}$", message = "RIB : 20 chiffres.") String rib) {
    }

    public record Response(Long id, String code, SupplierType type, String nom, String telephone, String adresse,
                           String cin, String matriculeFiscal, boolean actif, Instant createdAt, String email,
                           String conditionsPaiement, String banque, String rib) {

        public static Response from(Supplier s) {
            return new Response(s.getId(), s.getCode(), s.getType(), s.getNom(), s.getTelephone(), s.getAdresse(),
                    s.getCin(), s.getMatriculeFiscal(), s.isActif(), s.getCreatedAt(), s.getEmail(),
                    s.getConditionsPaiement(), s.getBanque(), s.getRib());
        }
    }
}

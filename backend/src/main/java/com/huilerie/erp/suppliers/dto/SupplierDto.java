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
            @Size(max = 50) String matriculeFiscal) {
    }

    public record Response(Long id, String code, SupplierType type, String nom, String telephone, String adresse,
                           String cin, String matriculeFiscal, boolean actif, Instant createdAt) {

        public static Response from(Supplier s) {
            return new Response(s.getId(), s.getCode(), s.getType(), s.getNom(), s.getTelephone(), s.getAdresse(),
                    s.getCin(), s.getMatriculeFiscal(), s.isActif(), s.getCreatedAt());
        }
    }
}

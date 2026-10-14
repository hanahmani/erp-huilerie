package com.huilerie.erp.catalog.dto;

import com.huilerie.erp.catalog.Unit;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public final class UnitDto {

    private UnitDto() {
    }

    public record Request(
            @NotBlank(message = "Le code est obligatoire.") @Size(max = 20) String code,
            @NotBlank(message = "Le libellé est obligatoire.") @Size(max = 80) String libelle) {
    }

    public record Response(Long id, String code, String libelle, boolean actif) {
        public static Response from(Unit u) {
            return new Response(u.getId(), u.getCode(), u.getLibelle(), u.isActif());
        }
    }
}

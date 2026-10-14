package com.huilerie.erp.catalog.dto;

import com.huilerie.erp.catalog.Category;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public final class CategoryDto {

    private CategoryDto() {
    }

    public record Request(
            @NotBlank(message = "Le nom est obligatoire.") @Size(max = 100) String nom,
            @Size(max = 255) String description) {
    }

    public record Response(Long id, String nom, String description, boolean actif) {
        public static Response from(Category c) {
            return new Response(c.getId(), c.getNom(), c.getDescription(), c.isActif());
        }
    }
}

package com.huilerie.erp.users.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.Set;

public record RoleRequest(
        @Pattern(regexp = "^[A-Z][A-Z0-9_]{2,63}$", message = "Code en majuscules, chiffres et _ (ex. MAGASINIER).") String code,
        @NotBlank(message = "Le nom est obligatoire.") @Size(max = 100) String name,
        @Size(max = 255) String description,
        Set<String> permissions) {
}

package com.huilerie.erp.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(
        @NotBlank(message = "L'identifiant est obligatoire.") @Size(max = 64) String username,
        @NotBlank(message = "Le mot de passe est obligatoire.") @Size(max = 72) String password
) {
}

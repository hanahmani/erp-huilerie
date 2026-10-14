package com.huilerie.erp.users.dto;

import jakarta.validation.constraints.NotBlank;

public record ResetPasswordRequest(@NotBlank(message = "Le mot de passe provisoire est obligatoire.") String temporaryPassword) {
}

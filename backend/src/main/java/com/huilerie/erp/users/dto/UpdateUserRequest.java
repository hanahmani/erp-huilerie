package com.huilerie.erp.users.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.Set;

public record UpdateUserRequest(
        @NotBlank(message = "Le prénom est obligatoire.") @Size(max = 100) String firstName,
        @NotBlank(message = "Le nom est obligatoire.") @Size(max = 100) String lastName,
        @Email(message = "Adresse e-mail invalide.") @Size(max = 120) String email,
        @Size(max = 50) String telephone,
        @Size(max = 100) String service,
        @NotEmpty(message = "Choisissez au moins un rôle.") Set<String> roles) {
}

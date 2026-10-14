package com.huilerie.erp.users.dto;

import com.huilerie.erp.common.validation.ValidationPatterns;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.Set;

public record CreateUserRequest(
        @NotBlank(message = "L'identifiant est obligatoire.")
        @Pattern(regexp = ValidationPatterns.USERNAME,
                message = "3 à 64 caractères : minuscules, chiffres, point, tiret ou souligné.")
        String username,

        @NotBlank(message = "Le prénom est obligatoire.") @Size(max = 100) String firstName,
        @NotBlank(message = "Le nom est obligatoire.") @Size(max = 100) String lastName,

        @NotBlank(message = "Le mot de passe provisoire est obligatoire.") String temporaryPassword,

        @NotEmpty(message = "Choisissez au moins un rôle.") Set<String> roles,

        @jakarta.validation.constraints.Email(message = "Adresse e-mail invalide.") @Size(max = 120) String email,
        @Size(max = 50) String telephone,
        @Size(max = 100) String service
) {
}

package com.huilerie.erp.users.dto;

import jakarta.validation.constraints.NotNull;

public record UpdateUserStatusRequest(@NotNull(message = "Le statut est obligatoire.") Boolean active) {
}

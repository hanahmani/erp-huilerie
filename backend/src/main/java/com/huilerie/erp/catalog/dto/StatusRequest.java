package com.huilerie.erp.catalog.dto;

import jakarta.validation.constraints.NotNull;

/** Activation / désactivation (suppression logique). */
public record StatusRequest(@NotNull(message = "Le statut est obligatoire.") Boolean actif) {
}

package com.huilerie.erp.purchases;

/** Cycle : BROUILLON → VALIDEE → PARTIELLEMENT_RECUE → RECUE ; ANNULEE possible avant toute réception. */
public enum PurchaseStatus {
    BROUILLON,
    VALIDEE,
    PARTIELLEMENT_RECUE,
    RECUE,
    ANNULEE
}

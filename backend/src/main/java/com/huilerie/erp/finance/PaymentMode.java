package com.huilerie.erp.finance;

/** Moyens de paiement acceptés. Seules les espèces permettent de rendre la monnaie. */
public enum PaymentMode {
    ESPECES,
    CARTE,
    VIREMENT,
    CHEQUE
}

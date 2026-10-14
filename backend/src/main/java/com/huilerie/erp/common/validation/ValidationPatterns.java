package com.huilerie.erp.common.validation;

public final class ValidationPatterns {

    /** Identifiant : minuscules, chiffres, point, tiret et souligné. */
    public static final String USERNAME = "^[a-z0-9._-]{3,64}$";

    private ValidationPatterns() {
    }
}

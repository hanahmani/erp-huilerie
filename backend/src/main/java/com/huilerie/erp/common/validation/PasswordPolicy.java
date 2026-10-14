package com.huilerie.erp.common.validation;

import com.huilerie.erp.common.exception.BusinessException;

/** Règles minimales des mots de passe (BCrypt limite la longueur à 72 octets). */
public final class PasswordPolicy {

    public static final int MIN_LENGTH = 10;
    public static final int MAX_LENGTH = 72;

    private PasswordPolicy() {
    }

    public static void check(String password) {
        if (password == null || password.length() < MIN_LENGTH) {
            throw new BusinessException("Le mot de passe doit contenir au moins " + MIN_LENGTH + " caractères.");
        }
        if (password.length() > MAX_LENGTH) {
            throw new BusinessException("Le mot de passe ne doit pas dépasser " + MAX_LENGTH + " caractères.");
        }
        boolean hasLetter = password.chars().anyMatch(Character::isLetter);
        boolean hasDigit = password.chars().anyMatch(Character::isDigit);
        if (!hasLetter || !hasDigit) {
            throw new BusinessException("Le mot de passe doit contenir au moins une lettre et un chiffre.");
        }
    }
}

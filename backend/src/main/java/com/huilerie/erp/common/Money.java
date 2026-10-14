package com.huilerie.erp.common;

import com.huilerie.erp.common.exception.BusinessException;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Montants en dinar tunisien et quantités : toujours BigDecimal, 3 décimales. */
public final class Money {

    public static final int SCALE = 3;

    private Money() {
    }

    public static BigDecimal zero() {
        return BigDecimal.ZERO.setScale(SCALE);
    }

    public static BigDecimal of(BigDecimal value) {
        return value == null ? zero() : value.setScale(SCALE, RoundingMode.HALF_UP);
    }

    /** Refuse plus de 3 décimales au lieu d'arrondir silencieusement une saisie. */
    public static BigDecimal strict(BigDecimal value, String label) {
        if (value == null) {
            return null;
        }
        if (value.stripTrailingZeros().scale() > SCALE) {
            throw new BusinessException(label + " : 3 décimales au maximum.");
        }
        return value.setScale(SCALE, RoundingMode.UNNECESSARY);
    }

    /** Résultat d'agrégat JPA (BigDecimal, Long, Integer ou null) converti en montant à 3 décimales. */
    public static BigDecimal ofAny(Object value) {
        if (value == null) {
            return zero();
        }
        if (value instanceof BigDecimal b) {
            return of(b);
        }
        return of(new BigDecimal(value.toString()));
    }

    public static boolean isPositive(BigDecimal value) {
        return value != null && value.signum() > 0;
    }
}

package com.huilerie.erp.catalog;

import com.huilerie.erp.common.Money;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Huile stockée en kg, vendue au kg ou au litre : 1 L = densité kg (0,916 par défaut, dans Paramètres).
 * Le prix est fixé au litre ; le prix au kg en est déduit.
 */
public final class OilUnits {

    public static final BigDecimal DEFAULT_DENSITY = new BigDecimal("0.9160");

    private OilUnits() {
    }

    /** Huile en vrac stockée en kg : seule cette combinaison peut être vendue au litre. */
    public static boolean litreEligible(ArticleType type, Unit unit) {
        return type == ArticleType.HUILE_VRAC && "kg".equalsIgnoreCase(unit.getCode());
    }

    public static boolean soldByLitre(Article a) {
        return a.getPrixVenteLitre() != null;
    }

    /** Prix au kg = prix au litre / densité. */
    public static BigDecimal kgPrice(BigDecimal litrePrice, BigDecimal density) {
        return litrePrice.divide(density, Money.SCALE, RoundingMode.HALF_UP);
    }

    /** Litres vendus → kg sortis du stock. */
    public static BigDecimal litresToKg(BigDecimal litres, BigDecimal density) {
        return litres.multiply(density).setScale(Money.SCALE, RoundingMode.HALF_UP);
    }
}

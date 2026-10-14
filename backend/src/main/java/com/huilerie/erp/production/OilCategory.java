package com.huilerie.erp.production;

import java.math.BigDecimal;

/**
 * Classement indicatif de l'huile selon l'acidité libre (% acide oléique), d'après les seuils usuels
 * de la norme commerciale du COI. L'analyse complète (peroxyde, K232, K270, organoleptique) reste
 * nécessaire pour un classement officiel.
 */
public enum OilCategory {
    EXTRA_VIERGE("0.80"),
    VIERGE("2.00"),
    COURANTE("3.30"),
    LAMPANTE(null);

    private final BigDecimal max;

    OilCategory(String max) {
        this.max = max == null ? null : new BigDecimal(max);
    }

    public static OilCategory fromAcidity(BigDecimal acidity) {
        if (acidity == null) {
            return null;
        }
        for (OilCategory c : values()) {
            if (c.max == null || acidity.compareTo(c.max) <= 0) {
                return c;
            }
        }
        return LAMPANTE;
    }
}

package com.huilerie.erp.common;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Montant en toutes lettres, en français, pour la ligne « Arrêté(e) … à la somme de : ».
 * <p>
 * Règles d'accord (orthographe traditionnelle, traits d'union sous cent) :
 * <ul>
 *   <li>« vingt » et « cent » prennent un s quand ils sont multipliés et terminent le nombre
 *       (quatre-vingts, deux cents), mais pas devant « mille » (quatre-vingt mille, deux cent mille) ;</li>
 *   <li>devant « million » et « milliard », qui sont des noms, l'accord se fait (deux cents millions) ;</li>
 *   <li>« mille » est invariable ; « million » et « milliard » prennent un s au pluriel ;</li>
 *   <li>« et » seulement pour 21, 31, 41, 51, 61 et 71 (vingt et un, soixante et onze, mais quatre-vingt-un) ;</li>
 *   <li>« de » avant la devise après un nombre rond de millions (deux millions de dinars).</li>
 * </ul>
 */
public final class MontantEnLettres {

    private static final String[] UNITES = {"", "un", "deux", "trois", "quatre", "cinq", "six", "sept", "huit", "neuf",
            "dix", "onze", "douze", "treize", "quatorze", "quinze", "seize", "dix-sept", "dix-huit", "dix-neuf"};
    private static final String[] DIZAINES = {"", "", "vingt", "trente", "quarante", "cinquante", "soixante"};
    private static final long MAX = 999_999_999_999L;

    private MontantEnLettres() {
    }

    /** Ex. 1200,500 TND → « mille deux cents dinars et cinq cents millimes ». Arrondi à la précision de la devise. */
    public static String enLettres(BigDecimal montant, Devise devise) {
        if (montant == null || devise == null) {
            throw new IllegalArgumentException("Montant et devise obligatoires.");
        }
        if (montant.signum() < 0) {
            throw new IllegalArgumentException("Montant négatif : " + montant.toPlainString());
        }
        BigDecimal arrondi = montant.setScale(devise.decimales(), RoundingMode.HALF_UP);
        BigDecimal partieEntiere = arrondi.setScale(0, RoundingMode.DOWN);
        if (partieEntiere.compareTo(BigDecimal.valueOf(MAX)) > 0) {
            throw new IllegalArgumentException("Montant trop élevé : " + montant.toPlainString());
        }
        long entier = partieEntiere.longValueExact();
        long fraction = arrondi.subtract(partieEntiere).movePointRight(devise.decimales()).longValueExact();

        String partieUnite = nombre(entier) + (rondEnMillions(entier) ? " de " : " ") + devise.unite(entier);
        if (fraction == 0) {
            return partieUnite;
        }
        String partieSousUnite = nombre(fraction) + " " + devise.sousUnite(fraction);
        return entier == 0 ? partieSousUnite : partieUnite + " et " + partieSousUnite;
    }

    /** Nombre entier en lettres, de zéro à 999 999 999 999. */
    public static String nombre(long n) {
        if (n < 0 || n > MAX) {
            throw new IllegalArgumentException("Nombre hors limites : " + n);
        }
        if (n == 0) {
            return "zéro";
        }
        int milliards = (int) (n / 1_000_000_000L);
        int millions = (int) (n / 1_000_000L % 1000);
        int milliers = (int) (n / 1000 % 1000);
        int reste = (int) (n % 1000);
        StringBuilder sb = new StringBuilder();
        if (milliards > 0) {
            append(sb, moinsDeMille(milliards, true) + " milliard" + (milliards > 1 ? "s" : ""));
        }
        if (millions > 0) {
            append(sb, moinsDeMille(millions, true) + " million" + (millions > 1 ? "s" : ""));
        }
        if (milliers > 0) {
            append(sb, milliers == 1 ? "mille" : moinsDeMille(milliers, false) + " mille");
        }
        if (reste > 0) {
            append(sb, moinsDeMille(reste, true));
        }
        return sb.toString();
    }

    private static boolean rondEnMillions(long n) {
        return n >= 1_000_000 && n % 1_000_000 == 0;
    }

    private static void append(StringBuilder sb, String s) {
        if (!sb.isEmpty()) {
            sb.append(' ');
        }
        sb.append(s);
    }

    /** n de 1 à 999 ; accord = false devant « mille » (vingt et cent restent invariables). */
    private static String moinsDeMille(int n, boolean accord) {
        int centaines = n / 100;
        int r = n % 100;
        String c = switch (centaines) {
            case 0 -> "";
            case 1 -> "cent";
            default -> UNITES[centaines] + " cent" + (r == 0 && accord ? "s" : "");
        };
        if (r == 0) {
            return c;
        }
        String d = moinsDeCent(r, accord);
        return c.isEmpty() ? d : c + " " + d;
    }

    private static String moinsDeCent(int n, boolean accord) {
        if (n < 20) {
            return UNITES[n];
        }
        int t = n / 10;
        int u = n % 10;
        return switch (t) {
            case 7 -> "soixante" + (u == 1 ? " et onze" : "-" + UNITES[10 + u]);
            case 8 -> u == 0 ? "quatre-vingt" + (accord ? "s" : "") : "quatre-vingt-" + UNITES[u];
            case 9 -> "quatre-vingt-" + UNITES[10 + u];
            default -> DIZAINES[t] + (u == 0 ? "" : u == 1 ? " et un" : "-" + UNITES[u]);
        };
    }
}

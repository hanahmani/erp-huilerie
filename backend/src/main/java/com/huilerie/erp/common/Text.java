package com.huilerie.erp.common;

public final class Text {

    private Text() {
    }

    /** Chaîne nettoyée, ou null si vide. */
    public static String clean(String value) {
        if (value == null) {
            return null;
        }
        String t = value.trim();
        return t.isEmpty() ? null : t;
    }

    /** Terme de recherche : null si vide (les requêtes ignorent alors le filtre). */
    public static String search(String value) {
        String t = clean(value);
        return t == null ? null : t.toLowerCase();
    }
}

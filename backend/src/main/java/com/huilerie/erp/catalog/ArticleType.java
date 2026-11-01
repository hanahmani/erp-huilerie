package com.huilerie.erp.catalog;

/** Familles d'articles d'une huilerie ; le préfixe sert à la référence automatique (ex. HVR-0001). */
public enum ArticleType {
    HUILE_VRAC("HVR"),
    HUILE_CONDITIONNEE("HCD"),
    OLIVES("OLV"),
    EMBALLAGE("EMB"),
    CONSOMMABLE("CSM"),
    SOUS_PRODUIT("SPR"),
    AUTRE("AUT");

    private final String prefix;

    ArticleType(String prefix) {
        this.prefix = prefix;
    }

    public String prefix() {
        return prefix;
    }
}

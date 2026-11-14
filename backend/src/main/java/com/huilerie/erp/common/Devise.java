package com.huilerie.erp.common;

/**
 * Devise d'un document commercial. Le dinar a 3 décimales (millimes), l'euro et le dollar 2 (centimes / cents).
 * Pas de taux de change : un document reste dans sa devise.
 */
public enum Devise {

    TND(3, "DT", "dinar", "dinars", "millime", "millimes"),
    EUR(2, "€", "euro", "euros", "centime", "centimes"),
    USD(2, "$", "dollar", "dollars", "cent", "cents");

    private final int decimales;
    private final String symbole;
    private final String unite;
    private final String unites;
    private final String sousUnite;
    private final String sousUnites;

    Devise(int decimales, String symbole, String unite, String unites, String sousUnite, String sousUnites) {
        this.decimales = decimales;
        this.symbole = symbole;
        this.unite = unite;
        this.unites = unites;
        this.sousUnite = sousUnite;
        this.sousUnites = sousUnites;
    }

    public int decimales() { return decimales; }
    public String symbole() { return symbole; }
    /** Singulier pour 0 et 1 (« zéro dinar », « un dinar »), pluriel à partir de 2. */
    public String unite(long n) { return n >= 2 ? unites : unite; }
    public String sousUnite(long n) { return n >= 2 ? sousUnites : sousUnite; }
}

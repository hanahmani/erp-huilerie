package com.huilerie.erp.inventory;

/** Types de mouvement et leur sens (+1 entrée, -1 sortie). */
public enum MovementType {
    STOCK_INITIAL(1, true),
    ENTREE(1, true),
    SORTIE(-1, true),
    AJUSTEMENT_PLUS(1, true),
    AJUSTEMENT_MOINS(-1, true),
    VENTE(-1, false),
    ANNULATION_VENTE(1, false),
    RECEPTION_ACHAT(1, false),
    INVENTAIRE_PLUS(1, false),
    INVENTAIRE_MOINS(-1, false),
    PRESSAGE_ENTREE(1, false),
    PRESSAGE_REMISE(-1, false),
    CONSOMMATION_PRODUCTION(-1, false),
    PRODUCTION_SORTIE(1, false),
    CONDITIONNEMENT_SORTIE(-1, false),
    CONDITIONNEMENT_ENTREE(1, false),
    ANNULATION_PLUS(1, false),
    ANNULATION_MOINS(-1, false),
    LIVRAISON(-1, false),
    RETOUR_CLIENT(1, false);

    private final int sens;
    private final boolean manual;

    MovementType(int sens, boolean manual) {
        this.sens = sens;
        this.manual = manual;
    }

    public int sens() {
        return sens;
    }

    /** Saisissable à la main (avec motif) ; les autres sont générés par les documents. */
    public boolean isManual() {
        return manual;
    }
}

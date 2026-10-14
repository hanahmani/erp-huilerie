// Libellés lisibles des valeurs techniques renvoyées par l'API.
export const ARTICLE_TYPES = {
  HUILE_VRAC: 'Huile en vrac',
  HUILE_CONDITIONNEE: 'Huile conditionnée',
  OLIVES: 'Olives',
  EMBALLAGE: 'Emballage',
  CONSOMMABLE: 'Consommable',
  SOUS_PRODUIT: 'Sous-produit',
  AUTRE: 'Autre',
};

export const CLIENT_TYPES = { PARTICULIER: 'Particulier', AGRICULTEUR: 'Agriculteur', PROFESSIONNEL: 'Professionnel' };
export const SUPPLIER_TYPES = { AGRICULTEUR: 'Agriculteur', ENTREPRISE: 'Entreprise' };

export const MOVEMENT_TYPES = {
  STOCK_INITIAL: 'Stock initial',
  ENTREE: 'Entrée',
  SORTIE: 'Sortie',
  AJUSTEMENT_PLUS: 'Ajustement +',
  AJUSTEMENT_MOINS: 'Ajustement −',
  VENTE: 'Vente',
  ANNULATION_VENTE: 'Annulation de vente',
  RECEPTION_ACHAT: "Réception d'achat",
  INVENTAIRE_PLUS: 'Inventaire +',
  INVENTAIRE_MOINS: 'Inventaire −',
  PRESSAGE_ENTREE: 'Pressage (huile client)',
  PRESSAGE_REMISE: 'Remise au client',
  CONSOMMATION_PRODUCTION: 'Consommation production',
  PRODUCTION_SORTIE: 'Sortie de production',
  CONDITIONNEMENT_SORTIE: 'Conditionnement (sortie)',
  CONDITIONNEMENT_ENTREE: 'Conditionnement (entrée)',
  ANNULATION_PLUS: 'Annulation +',
  ANNULATION_MOINS: 'Annulation −',
};
export const MANUAL_MOVEMENTS = ['STOCK_INITIAL', 'ENTREE', 'SORTIE', 'AJUSTEMENT_PLUS', 'AJUSTEMENT_MOINS'];

export const LOT_OWNERS = { ENTREPRISE: "Huilerie", CLIENT: 'Client (pressage)' };
export const LOT_STATUS = { DISPONIBLE: 'Disponible', BLOQUE: 'Bloqué' };

export const PAYMENT_MODES = { ESPECES: 'Espèces', CARTE: 'Carte', VIREMENT: 'Virement', CHEQUE: 'Chèque' };
export const PAYMENT_STATUS = { PAYEE: 'Payée', PARTIELLE: 'Partielle', NON_PAYEE: 'Non payée', ANNULEE: 'Annulée' };

export const toOptions = (map) => Object.entries(map).map(([value, label]) => ({ value, label }));

export const PURCHASE_STATUS = {
  BROUILLON: 'Brouillon', VALIDEE: 'Validée', PARTIELLEMENT_RECUE: 'Partiellement reçue', RECUE: 'Reçue', ANNULEE: 'Annulée',
};
export const PURCHASE_STATUS_CLASS = {
  BROUILLON: '', VALIDEE: 'badge-role', PARTIELLEMENT_RECUE: 'badge-warn', RECUE: 'badge-ok', ANNULEE: 'badge-off',
};
export const INVENTORY_STATUS = { EN_COURS: 'En cours', VALIDE: 'Validé', ANNULE: 'Annulé' };

export const PRESSING_STATUS = { RECU: 'Olives reçues', EN_COURS: 'Pressage en cours', TERMINE: 'Huile à remettre', REMIS: 'Huile remise', ANNULE: 'Annulé' };
export const PRESSING_STATUS_CLASS = { RECU: '', EN_COURS: 'badge-role', TERMINE: 'badge-warn', REMIS: 'badge-ok', ANNULE: 'badge-off' };
export const OLIVE_STATES = { SAINES: 'Saines', TOMBEES: 'Tombées (ramassées au sol)', ABIMEES: 'Abîmées' };
export const PRODUCTION_STATUS = { PLANIFIE: 'Planifié', EN_COURS: 'En cours', TERMINE: 'Terminé', ANNULE: 'Annulé' };
export const PRODUCTION_STATUS_CLASS = { PLANIFIE: '', EN_COURS: 'badge-warn', TERMINE: 'badge-ok', ANNULE: 'badge-off' };
export const OIL_CATEGORIES = { EXTRA_VIERGE: 'Extra vierge', VIERGE: 'Vierge', COURANTE: 'Courante', LAMPANTE: 'Lampante', NON_CLASSEE: 'Non classée' };

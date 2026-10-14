// Libellés lisibles des rôles techniques définis dans la migration V2.
export const ROLE_LABELS = {
  ADMIN: 'Administrateur',
  CAISSIER: 'Caissier / vendeur',
  RESP_STOCK: 'Responsable de stock',
  RESP_PRODUCTION: 'Responsable de production',
  RESP_ACHATS: 'Responsable achats',
  RESP_FINANCIER: 'Responsable financier',
};

export const roleLabel = (code) => ROLE_LABELS[code] ?? code;

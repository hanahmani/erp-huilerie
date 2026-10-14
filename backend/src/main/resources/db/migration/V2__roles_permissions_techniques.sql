-- =====================================================================
-- V2 : référentiel TECHNIQUE des rôles et permissions.
-- Ce ne sont pas des données métier : aucun client, article, stock ou
-- utilisateur n'est créé ici. Le premier administrateur est créé au
-- démarrage par AdminBootstrap à partir de variables d'environnement.
-- =====================================================================

INSERT INTO permissions (code, description) VALUES
 ('USER_MANAGE',          'Gérer les utilisateurs et leurs rôles'),
 ('AUDIT_VIEW',           'Consulter l''historique des actions'),
 ('SETTINGS_MANAGE',      'Modifier les paramètres de l''huilerie'),
 ('DASHBOARD_VIEW',       'Consulter le tableau de bord'),
 ('CLIENT_VIEW',          'Consulter les clients'),
 ('CLIENT_MANAGE',        'Créer et modifier les clients'),
 ('SUPPLIER_VIEW',        'Consulter les fournisseurs'),
 ('SUPPLIER_MANAGE',      'Créer et modifier les fournisseurs'),
 ('CATALOG_VIEW',         'Consulter le catalogue'),
 ('CATALOG_MANAGE',       'Gérer articles, catégories, unités et formats'),
 ('PURCHASE_VIEW',        'Consulter les achats'),
 ('PURCHASE_CREATE',      'Créer et valider des achats et réceptions'),
 ('PRODUCTION_VIEW',      'Consulter la production'),
 ('PRODUCTION_MANAGE',    'Gérer les lots et la fabrication'),
 ('PRESSING_VIEW',        'Consulter le pressage client'),
 ('PRESSING_MANAGE',      'Gérer réceptions, pressages et remises d''huile'),
 ('STOCK_VIEW',           'Consulter les stocks et mouvements'),
 ('STOCK_ADJUST',         'Corriger le stock et valider les inventaires'),
 ('PACKAGING_VIEW',       'Consulter le conditionnement'),
 ('PACKAGING_MANAGE',     'Gérer les ordres de conditionnement'),
 ('SALES_VIEW',           'Consulter les ventes et tickets'),
 ('SALES_CREATE',         'Créer des ventes, encaisser et imprimer'),
 ('SALES_CANCEL',         'Annuler une vente validée'),
 ('SALES_PRICE_OVERRIDE', 'Modifier le prix unitaire en caisse'),
 ('SALES_DISCOUNT',       'Accorder une remise'),
 ('FINANCE_VIEW',         'Consulter caisse, paiements et rapports'),
 ('FINANCE_MANAGE',       'Enregistrer dépenses et règlements');

INSERT INTO roles (code, nom, description) VALUES
 ('ADMIN',           'Administrateur',            'Accès complet'),
 ('CAISSIER',        'Caissier / vendeur',        'Ventes, encaissement et tickets'),
 ('RESP_STOCK',      'Responsable de stock',      'Réceptions, articles, mouvements et inventaires'),
 ('RESP_PRODUCTION', 'Responsable de production', 'Lots d''olives, production et conditionnement'),
 ('RESP_ACHATS',     'Responsable achats',        'Fournisseurs, achats et règlements'),
 ('RESP_FINANCIER',  'Responsable financier',     'Consultation des ventes, paiements et rapports');

-- Administrateur : toutes les permissions
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r CROSS JOIN permissions p WHERE r.code = 'ADMIN';

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r CROSS JOIN permissions p
WHERE r.code = 'CAISSIER'
  AND p.code IN ('DASHBOARD_VIEW', 'CLIENT_VIEW', 'CATALOG_VIEW', 'SALES_VIEW', 'SALES_CREATE');

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r CROSS JOIN permissions p
WHERE r.code = 'RESP_STOCK'
  AND p.code IN ('DASHBOARD_VIEW', 'CATALOG_VIEW', 'CATALOG_MANAGE', 'STOCK_VIEW', 'STOCK_ADJUST',
                 'PURCHASE_VIEW', 'PACKAGING_VIEW');

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r CROSS JOIN permissions p
WHERE r.code = 'RESP_PRODUCTION'
  AND p.code IN ('DASHBOARD_VIEW', 'CATALOG_VIEW', 'STOCK_VIEW', 'PRODUCTION_VIEW', 'PRODUCTION_MANAGE',
                 'PRESSING_VIEW', 'PRESSING_MANAGE', 'PACKAGING_VIEW', 'PACKAGING_MANAGE');

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r CROSS JOIN permissions p
WHERE r.code = 'RESP_ACHATS'
  AND p.code IN ('DASHBOARD_VIEW', 'SUPPLIER_VIEW', 'SUPPLIER_MANAGE', 'CATALOG_VIEW', 'PURCHASE_VIEW',
                 'PURCHASE_CREATE', 'STOCK_VIEW');

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r CROSS JOIN permissions p
WHERE r.code = 'RESP_FINANCIER'
  AND p.code IN ('DASHBOARD_VIEW', 'CLIENT_VIEW', 'SUPPLIER_VIEW', 'SALES_VIEW', 'PURCHASE_VIEW',
                 'FINANCE_VIEW', 'AUDIT_VIEW');

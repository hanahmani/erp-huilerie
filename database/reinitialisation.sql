-- =====================================================================
-- Réinitialisation de la base de l'ERP Huilerie Ben Chaaben (MySQL 8).
--
-- SUPPRIME TOUTES LES DONNÉES, sauf :
--   * le compte admin et ses rôles (users, user_roles)
--   * les rôles et permissions (roles, permissions, role_permissions)
--   * l'historique Flyway (flyway_schema_history) : le schéma reste à jour
--
-- Puis crée les données de base :
--   * unités : kg, L
--   * articles (huile en vrac, stockée en kg, suivie par lot, sans prix) :
--       HVR-0001 Huile en vrac
--       HVR-0002 Huile en vrac extra vierge
--     → dans le Catalogue : saisir le prix au litre et cocher « Vendable en caisse »
--   * puits et silo de 10 000 kg :
--       P1 Puits 1 → Huile en vrac
--       S1 Silo 1  → Huile en vrac extra vierge
--
-- IRRÉVERSIBLE : faire d'abord une sauvegarde (scripts/sauvegarde.sh).
-- Exécution (depuis la racine du projet) :
--   docker compose exec -T mysql sh -c 'exec mysql -uroot -p"$MYSQL_ROOT_PASSWORD" "$MYSQL_DATABASE"' < database/reinitialisation.sql
--
-- TRUNCATE ne déclenche pas les triggers « no_delete » des journaux
-- (mouvements de stock, de caisse, audit) : ils restent en place.
-- =====================================================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ---------------------------------------------------------------- données métier
TRUNCATE TABLE notification_reads;
TRUNCATE TABLE audit_logs;

TRUNCATE TABLE sales_document_payments;
TRUNCATE TABLE sales_document_lines;
TRUNCATE TABLE sales_documents;
TRUNCATE TABLE sale_payments;
TRUNCATE TABLE sale_lines;
TRUNCATE TABLE sales;
TRUNCATE TABLE client_payments;

TRUNCATE TABLE pressing_seller_payments;
TRUNCATE TABLE pressing_payments;
TRUNCATE TABLE pressing_returns;
TRUNCATE TABLE pressings;

TRUNCATE TABLE production_byproducts;
TRUNCATE TABLE production_inputs;
TRUNCATE TABLE production_orders;

TRUNCATE TABLE goods_receipt_lines;
TRUNCATE TABLE goods_receipts;
TRUNCATE TABLE supplier_payments;
TRUNCATE TABLE purchase_lines;
TRUNCATE TABLE purchases;

TRUNCATE TABLE inventory_lines;
TRUNCATE TABLE inventories;
TRUNCATE TABLE storage_tanks;
TRUNCATE TABLE stock_movements;
TRUNCATE TABLE stock_balances;
TRUNCATE TABLE stock_lots;

TRUNCATE TABLE cash_movements;
TRUNCATE TABLE cash_sessions;
TRUNCATE TABLE expenses;
TRUNCATE TABLE expense_categories;
TRUNCATE TABLE taxes;

TRUNCATE TABLE employee_payments;
TRUNCATE TABLE work_days;
TRUNCATE TABLE employees;

TRUNCATE TABLE articles;
TRUNCATE TABLE categories;
TRUNCATE TABLE units;
TRUNCATE TABLE clients;
TRUNCATE TABLE suppliers;
TRUNCATE TABLE company_settings;
TRUNCATE TABLE document_sequences;

-- ---------------------------------------------------------------- utilisateurs : seul admin reste
DELETE FROM user_roles WHERE user_id NOT IN (SELECT id FROM (SELECT id FROM users WHERE username = 'admin') AS a);
DELETE FROM users WHERE username <> 'admin';

SET FOREIGN_KEY_CHECKS = 1;

-- ---------------------------------------------------------------- données de base
INSERT INTO units (code, libelle, actif) VALUES
    ('kg', 'Kilogramme', TRUE),
    ('L',  'Litre',      TRUE);

INSERT INTO articles (reference, nom, type, unite_id, vendable, suivi_lot, actif, created_at, updated_at)
SELECT 'HVR-0001', 'Huile en vrac', 'HUILE_VRAC', u.id, FALSE, TRUE, TRUE, NOW(6), NOW(6) FROM units u WHERE u.code = 'kg';
INSERT INTO articles (reference, nom, type, unite_id, vendable, suivi_lot, actif, created_at, updated_at)
SELECT 'HVR-0002', 'Huile en vrac extra vierge', 'HUILE_VRAC', u.id, FALSE, TRUE, TRUE, NOW(6), NOW(6) FROM units u WHERE u.code = 'kg';

-- Prochaine référence automatique d'huile en vrac : HVR-0003
INSERT INTO document_sequences (type, annee, prochain) VALUES ('ARTICLE_HUILE_VRAC', 0, 3);

-- Chaque puits porte un lot ENTREPRISE de même numéro que son code (comme à la création dans l'ERP).
INSERT INTO stock_lots (article_id, numero, proprietaire, statut, created_at)
SELECT a.id, 'P1', 'ENTREPRISE', 'DISPONIBLE', NOW(6) FROM articles a WHERE a.reference = 'HVR-0001';
INSERT INTO stock_lots (article_id, numero, proprietaire, statut, created_at)
SELECT a.id, 'S1', 'ENTREPRISE', 'DISPONIBLE', NOW(6) FROM articles a WHERE a.reference = 'HVR-0002';

INSERT INTO storage_tanks (code, nom, type, article_id, lot_id, capacite, actif, created_at)
SELECT 'P1', 'Puits 1', 'PUITS', l.article_id, l.id, 10000.000, TRUE, NOW(6)
FROM stock_lots l JOIN articles a ON a.id = l.article_id WHERE a.reference = 'HVR-0001' AND l.numero = 'P1';
INSERT INTO storage_tanks (code, nom, type, article_id, lot_id, capacite, actif, created_at)
SELECT 'S1', 'Silo 1', 'SILO', l.article_id, l.id, 10000.000, TRUE, NOW(6)
FROM stock_lots l JOIN articles a ON a.id = l.article_id WHERE a.reference = 'HVR-0002' AND l.numero = 'S1';

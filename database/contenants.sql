-- =====================================================================
-- Contenants de l'huilerie Ben Chaaben (MySQL 8) — à exécuter UNE FOIS sur la base en service.
--
-- 1. Retire l'ancien « Puits 1 » (P1) et l'ancien « Silo 1 » (S1) créés par la réinitialisation :
--      supprimés s'ils n'ont jamais servi (aucun mouvement de stock),
--      sinon seulement désactivés (leur historique est conservé).
-- 2. Crée les contenants réels, tous pour « Huile en vrac » (HVR-0001), propriétaire l'huilerie.
--    Capacités en kg ; citernes converties depuis les litres avec la densité 0,916 :
--      PILE-1 … PILE-3   3 piles de 9 t                 9 000 kg chacune
--      CIT-P1, CIT-P2    2 citernes plastique alimentaire 3 000 L ≈ 2 748 kg
--      CIT-I1, CIT-I2    2 citernes inox                  2 500 L ≈ 2 290 kg
--      CIT-I3            1 citerne inox                   1 600 L ≈ 1 465,6 kg
--      FUT-1 … FUT-4     4 fûts                             200 kg chacun
--    Un contenant dont le code existe déjà n'est pas recréé : le script peut être relancé sans risque.
--
-- Faire d'abord une sauvegarde (scripts/sauvegarde.sh). Exécution (depuis la racine du projet) :
--   docker compose exec -T mysql sh -c 'exec mysql -uroot -p"$MYSQL_ROOT_PASSWORD" "$MYSQL_DATABASE"' < database/contenants.sql
-- =====================================================================

SET NAMES utf8mb4;

-- ---------------------------------------------------------------- 1. anciens P1 et S1
CREATE TEMPORARY TABLE anciens_contenants AS
SELECT t.id, t.lot_id FROM storage_tanks t
WHERE t.code IN ('P1', 'S1') AND NOT EXISTS (SELECT 1 FROM stock_movements m WHERE m.lot_id = t.lot_id);

DELETE FROM storage_tanks WHERE id IN (SELECT id FROM anciens_contenants);
DELETE FROM stock_balances WHERE lot_id IN (SELECT lot_id FROM anciens_contenants);
DELETE FROM stock_lots WHERE id IN (SELECT lot_id FROM anciens_contenants);
DROP TEMPORARY TABLE anciens_contenants;

-- Ceux qui ont déjà servi restent dans l'historique, désactivés.
UPDATE storage_tanks SET actif = FALSE WHERE code IN ('P1', 'S1');

-- ---------------------------------------------------------------- 2. contenants réels
-- Chaque contenant porte un lot ENTREPRISE de même numéro que son code (comme à la création dans l'ERP).
INSERT INTO stock_lots (article_id, numero, proprietaire, statut, created_at)
SELECT a.id, 'PILE-1', 'ENTREPRISE', 'DISPONIBLE', NOW(6) FROM articles a
WHERE a.reference = 'HVR-0001' AND NOT EXISTS (SELECT 1 FROM stock_lots l WHERE l.article_id = a.id AND l.numero = 'PILE-1');
INSERT INTO storage_tanks (code, nom, type, article_id, lot_id, capacite, actif, created_at)
SELECT 'PILE-1', 'Pile 1 (9 t)', 'PILE', l.article_id, l.id, 9000.000, TRUE, NOW(6)
FROM stock_lots l JOIN articles a ON a.id = l.article_id
WHERE a.reference = 'HVR-0001' AND l.numero = 'PILE-1' AND NOT EXISTS (SELECT 1 FROM storage_tanks t WHERE t.code = 'PILE-1');

INSERT INTO stock_lots (article_id, numero, proprietaire, statut, created_at)
SELECT a.id, 'PILE-2', 'ENTREPRISE', 'DISPONIBLE', NOW(6) FROM articles a
WHERE a.reference = 'HVR-0001' AND NOT EXISTS (SELECT 1 FROM stock_lots l WHERE l.article_id = a.id AND l.numero = 'PILE-2');
INSERT INTO storage_tanks (code, nom, type, article_id, lot_id, capacite, actif, created_at)
SELECT 'PILE-2', 'Pile 2 (9 t)', 'PILE', l.article_id, l.id, 9000.000, TRUE, NOW(6)
FROM stock_lots l JOIN articles a ON a.id = l.article_id
WHERE a.reference = 'HVR-0001' AND l.numero = 'PILE-2' AND NOT EXISTS (SELECT 1 FROM storage_tanks t WHERE t.code = 'PILE-2');

INSERT INTO stock_lots (article_id, numero, proprietaire, statut, created_at)
SELECT a.id, 'PILE-3', 'ENTREPRISE', 'DISPONIBLE', NOW(6) FROM articles a
WHERE a.reference = 'HVR-0001' AND NOT EXISTS (SELECT 1 FROM stock_lots l WHERE l.article_id = a.id AND l.numero = 'PILE-3');
INSERT INTO storage_tanks (code, nom, type, article_id, lot_id, capacite, actif, created_at)
SELECT 'PILE-3', 'Pile 3 (9 t)', 'PILE', l.article_id, l.id, 9000.000, TRUE, NOW(6)
FROM stock_lots l JOIN articles a ON a.id = l.article_id
WHERE a.reference = 'HVR-0001' AND l.numero = 'PILE-3' AND NOT EXISTS (SELECT 1 FROM storage_tanks t WHERE t.code = 'PILE-3');

INSERT INTO stock_lots (article_id, numero, proprietaire, statut, created_at)
SELECT a.id, 'CIT-P1', 'ENTREPRISE', 'DISPONIBLE', NOW(6) FROM articles a
WHERE a.reference = 'HVR-0001' AND NOT EXISTS (SELECT 1 FROM stock_lots l WHERE l.article_id = a.id AND l.numero = 'CIT-P1');
INSERT INTO storage_tanks (code, nom, type, article_id, lot_id, capacite, actif, created_at)
SELECT 'CIT-P1', 'Citerne plastique alimentaire 1 (3000 L)', 'CITERNE', l.article_id, l.id, 2748.000, TRUE, NOW(6)
FROM stock_lots l JOIN articles a ON a.id = l.article_id
WHERE a.reference = 'HVR-0001' AND l.numero = 'CIT-P1' AND NOT EXISTS (SELECT 1 FROM storage_tanks t WHERE t.code = 'CIT-P1');

INSERT INTO stock_lots (article_id, numero, proprietaire, statut, created_at)
SELECT a.id, 'CIT-P2', 'ENTREPRISE', 'DISPONIBLE', NOW(6) FROM articles a
WHERE a.reference = 'HVR-0001' AND NOT EXISTS (SELECT 1 FROM stock_lots l WHERE l.article_id = a.id AND l.numero = 'CIT-P2');
INSERT INTO storage_tanks (code, nom, type, article_id, lot_id, capacite, actif, created_at)
SELECT 'CIT-P2', 'Citerne plastique alimentaire 2 (3000 L)', 'CITERNE', l.article_id, l.id, 2748.000, TRUE, NOW(6)
FROM stock_lots l JOIN articles a ON a.id = l.article_id
WHERE a.reference = 'HVR-0001' AND l.numero = 'CIT-P2' AND NOT EXISTS (SELECT 1 FROM storage_tanks t WHERE t.code = 'CIT-P2');

INSERT INTO stock_lots (article_id, numero, proprietaire, statut, created_at)
SELECT a.id, 'CIT-I1', 'ENTREPRISE', 'DISPONIBLE', NOW(6) FROM articles a
WHERE a.reference = 'HVR-0001' AND NOT EXISTS (SELECT 1 FROM stock_lots l WHERE l.article_id = a.id AND l.numero = 'CIT-I1');
INSERT INTO storage_tanks (code, nom, type, article_id, lot_id, capacite, actif, created_at)
SELECT 'CIT-I1', 'Citerne inox 1 (2500 L)', 'CITERNE', l.article_id, l.id, 2290.000, TRUE, NOW(6)
FROM stock_lots l JOIN articles a ON a.id = l.article_id
WHERE a.reference = 'HVR-0001' AND l.numero = 'CIT-I1' AND NOT EXISTS (SELECT 1 FROM storage_tanks t WHERE t.code = 'CIT-I1');

INSERT INTO stock_lots (article_id, numero, proprietaire, statut, created_at)
SELECT a.id, 'CIT-I2', 'ENTREPRISE', 'DISPONIBLE', NOW(6) FROM articles a
WHERE a.reference = 'HVR-0001' AND NOT EXISTS (SELECT 1 FROM stock_lots l WHERE l.article_id = a.id AND l.numero = 'CIT-I2');
INSERT INTO storage_tanks (code, nom, type, article_id, lot_id, capacite, actif, created_at)
SELECT 'CIT-I2', 'Citerne inox 2 (2500 L)', 'CITERNE', l.article_id, l.id, 2290.000, TRUE, NOW(6)
FROM stock_lots l JOIN articles a ON a.id = l.article_id
WHERE a.reference = 'HVR-0001' AND l.numero = 'CIT-I2' AND NOT EXISTS (SELECT 1 FROM storage_tanks t WHERE t.code = 'CIT-I2');

INSERT INTO stock_lots (article_id, numero, proprietaire, statut, created_at)
SELECT a.id, 'CIT-I3', 'ENTREPRISE', 'DISPONIBLE', NOW(6) FROM articles a
WHERE a.reference = 'HVR-0001' AND NOT EXISTS (SELECT 1 FROM stock_lots l WHERE l.article_id = a.id AND l.numero = 'CIT-I3');
INSERT INTO storage_tanks (code, nom, type, article_id, lot_id, capacite, actif, created_at)
SELECT 'CIT-I3', 'Citerne inox 3 (1600 L)', 'CITERNE', l.article_id, l.id, 1465.600, TRUE, NOW(6)
FROM stock_lots l JOIN articles a ON a.id = l.article_id
WHERE a.reference = 'HVR-0001' AND l.numero = 'CIT-I3' AND NOT EXISTS (SELECT 1 FROM storage_tanks t WHERE t.code = 'CIT-I3');

INSERT INTO stock_lots (article_id, numero, proprietaire, statut, created_at)
SELECT a.id, 'FUT-1', 'ENTREPRISE', 'DISPONIBLE', NOW(6) FROM articles a
WHERE a.reference = 'HVR-0001' AND NOT EXISTS (SELECT 1 FROM stock_lots l WHERE l.article_id = a.id AND l.numero = 'FUT-1');
INSERT INTO storage_tanks (code, nom, type, article_id, lot_id, capacite, actif, created_at)
SELECT 'FUT-1', 'Fût 1 (200 kg)', 'FUT', l.article_id, l.id, 200.000, TRUE, NOW(6)
FROM stock_lots l JOIN articles a ON a.id = l.article_id
WHERE a.reference = 'HVR-0001' AND l.numero = 'FUT-1' AND NOT EXISTS (SELECT 1 FROM storage_tanks t WHERE t.code = 'FUT-1');

INSERT INTO stock_lots (article_id, numero, proprietaire, statut, created_at)
SELECT a.id, 'FUT-2', 'ENTREPRISE', 'DISPONIBLE', NOW(6) FROM articles a
WHERE a.reference = 'HVR-0001' AND NOT EXISTS (SELECT 1 FROM stock_lots l WHERE l.article_id = a.id AND l.numero = 'FUT-2');
INSERT INTO storage_tanks (code, nom, type, article_id, lot_id, capacite, actif, created_at)
SELECT 'FUT-2', 'Fût 2 (200 kg)', 'FUT', l.article_id, l.id, 200.000, TRUE, NOW(6)
FROM stock_lots l JOIN articles a ON a.id = l.article_id
WHERE a.reference = 'HVR-0001' AND l.numero = 'FUT-2' AND NOT EXISTS (SELECT 1 FROM storage_tanks t WHERE t.code = 'FUT-2');

INSERT INTO stock_lots (article_id, numero, proprietaire, statut, created_at)
SELECT a.id, 'FUT-3', 'ENTREPRISE', 'DISPONIBLE', NOW(6) FROM articles a
WHERE a.reference = 'HVR-0001' AND NOT EXISTS (SELECT 1 FROM stock_lots l WHERE l.article_id = a.id AND l.numero = 'FUT-3');
INSERT INTO storage_tanks (code, nom, type, article_id, lot_id, capacite, actif, created_at)
SELECT 'FUT-3', 'Fût 3 (200 kg)', 'FUT', l.article_id, l.id, 200.000, TRUE, NOW(6)
FROM stock_lots l JOIN articles a ON a.id = l.article_id
WHERE a.reference = 'HVR-0001' AND l.numero = 'FUT-3' AND NOT EXISTS (SELECT 1 FROM storage_tanks t WHERE t.code = 'FUT-3');

INSERT INTO stock_lots (article_id, numero, proprietaire, statut, created_at)
SELECT a.id, 'FUT-4', 'ENTREPRISE', 'DISPONIBLE', NOW(6) FROM articles a
WHERE a.reference = 'HVR-0001' AND NOT EXISTS (SELECT 1 FROM stock_lots l WHERE l.article_id = a.id AND l.numero = 'FUT-4');
INSERT INTO storage_tanks (code, nom, type, article_id, lot_id, capacite, actif, created_at)
SELECT 'FUT-4', 'Fût 4 (200 kg)', 'FUT', l.article_id, l.id, 200.000, TRUE, NOW(6)
FROM stock_lots l JOIN articles a ON a.id = l.article_id
WHERE a.reference = 'HVR-0001' AND l.numero = 'FUT-4' AND NOT EXISTS (SELECT 1 FROM storage_tanks t WHERE t.code = 'FUT-4');

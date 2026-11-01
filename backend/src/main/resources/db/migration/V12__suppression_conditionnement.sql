-- =====================================================================
-- V12 : suppression du module Conditionnement.
--  * permissions PACKAGING_VIEW / PACKAGING_MANAGE retirées de tous les rôles
--  * tables packaging_consumptions et packaging_orders supprimées
--  * compteur de numérotation CONDITIONNEMENT retiré
-- Les mouvements de stock déjà écrits (CONDITIONNEMENT_SORTIE / _ENTREE) et
-- les lots de produits finis sont conservés : le stock reste juste.
-- Les traces déjà écrites dans audit_logs sont conservées (journal non modifiable).
-- SQL commun à MySQL et H2 (tests).
-- =====================================================================

DELETE FROM role_permissions
WHERE permission_id IN (SELECT id FROM permissions WHERE code IN ('PACKAGING_VIEW', 'PACKAGING_MANAGE'));

DELETE FROM permissions WHERE code IN ('PACKAGING_VIEW', 'PACKAGING_MANAGE');

DELETE FROM document_sequences WHERE type = 'CONDITIONNEMENT';

DROP TABLE packaging_consumptions;
DROP TABLE packaging_orders;

-- =====================================================================
-- V11 : suppression du module Contrôle qualité.
--  * permissions QUALITY_VIEW / QUALITY_MANAGE retirées de tous les rôles
--  * tables qualité supprimées (critères, contrôles, résultats, anomalies)
--  * compteurs de numérotation et états « lu » des alertes d'anomalie retirés
-- Les lots restent gérés dans Stock (blocage / déblocage manuel).
-- Les traces déjà écrites dans audit_logs sont conservées (journal non modifiable).
-- SQL commun à MySQL et H2 (tests).
-- =====================================================================

DELETE FROM role_permissions
WHERE permission_id IN (SELECT id FROM permissions WHERE code IN ('QUALITY_VIEW', 'QUALITY_MANAGE'));

DELETE FROM permissions WHERE code IN ('QUALITY_VIEW', 'QUALITY_MANAGE');

DELETE FROM notification_reads WHERE notif_key LIKE 'ANOMALIE:%';

DELETE FROM document_sequences WHERE type IN ('CONTROLE', 'ANOMALIE');

DROP TABLE quality_results;
DROP TABLE quality_anomalies;
DROP TABLE quality_controls;
DROP TABLE quality_criteria;

-- =====================================================================
-- V18 : contenants de l'huile des clients conservée à l'huilerie.
--  * storage_tanks.type : ajout de CITERNE et FUT (réservés à un client ou à l'huilerie,
--    gérés comme un puits : code, capacité, un lot par contenant).
-- SQL commun à MySQL et H2 (tests).
-- =====================================================================

ALTER TABLE storage_tanks DROP CONSTRAINT ck_storage_tanks;
ALTER TABLE storage_tanks ADD CONSTRAINT ck_storage_tanks
    CHECK (capacite > 0 AND type IN ('PUITS', 'SILO', 'CUVE', 'CITERNE', 'FUT', 'AUTRE'));

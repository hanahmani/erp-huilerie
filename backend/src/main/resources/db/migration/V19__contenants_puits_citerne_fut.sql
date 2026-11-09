-- =====================================================================
-- V19 : l'huile n'est stockée que dans un puits, une citerne ou un fût.
--  * les contenants SILO, CUVE ou AUTRE déjà créés deviennent des CITERNES ;
--  * storage_tanks.type limité à PUITS, CITERNE, FUT.
-- SQL commun à MySQL et H2 (tests).
-- =====================================================================

UPDATE storage_tanks SET type = 'CITERNE' WHERE type IN ('SILO', 'CUVE', 'AUTRE');

ALTER TABLE storage_tanks DROP CONSTRAINT ck_storage_tanks;
ALTER TABLE storage_tanks ADD CONSTRAINT ck_storage_tanks
    CHECK (capacite > 0 AND type IN ('PUITS', 'CITERNE', 'FUT'));

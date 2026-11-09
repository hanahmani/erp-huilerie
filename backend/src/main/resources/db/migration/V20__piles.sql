-- =====================================================================
-- V20 : les puits s'appellent des piles. Types de contenant : PILE, CITERNE, FUT.
-- SQL commun à MySQL et H2 (tests).
-- =====================================================================

ALTER TABLE storage_tanks DROP CONSTRAINT ck_storage_tanks;
UPDATE storage_tanks SET type = 'PILE' WHERE type = 'PUITS';
ALTER TABLE storage_tanks ADD CONSTRAINT ck_storage_tanks
    CHECK (capacite > 0 AND type IN ('PILE', 'CITERNE', 'FUT'));

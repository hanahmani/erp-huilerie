-- =====================================================================
-- V21 : n° du carnet de bulletins (bulletin papier correspondant), saisi à la main.
-- Facultatif, unique quand il est renseigné (contrôlé par l'application).
-- SQL commun à MySQL et H2 (tests).
-- =====================================================================

ALTER TABLE pressings ADD COLUMN numero_carnet VARCHAR(20);
CREATE INDEX idx_pressings_carnet ON pressings (numero_carnet);

-- =====================================================================
-- V17 : tarif du pressage à façon par défaut : 0,200 DT par kg d'olives net
-- (proposé à chaque réception, modifiable sur le bulletin et dans Paramètres).
-- SQL commun à MySQL et H2 (tests).
-- =====================================================================

UPDATE company_settings SET tarif_pressage_kg = 0.200 WHERE tarif_pressage_kg IS NULL;

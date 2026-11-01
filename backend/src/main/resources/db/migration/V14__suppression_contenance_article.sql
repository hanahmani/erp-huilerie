-- =====================================================================
-- V14 : suppression du champ « Contenance en litres » des articles.
-- Plus utilisé depuis la suppression du module Conditionnement (V12).
-- La contrainte CHECK est retirée avant la colonne (exigé par MySQL 8).
-- SQL commun à MySQL et H2 (tests).
-- =====================================================================

ALTER TABLE articles DROP CONSTRAINT ck_articles_contenance;
ALTER TABLE articles DROP COLUMN contenance;

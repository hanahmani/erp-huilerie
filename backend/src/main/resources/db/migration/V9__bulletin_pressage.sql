-- =====================================================================
-- V9 : bulletin de pressage, rempli à l'arrivée des olives d'un client.
--  * numero_bulletin : 000001, 000002… permanent, sans préfixe, sans trou
--    (séquence document_sequences type BULLETIN_PRESSAGE, annee = 0)
--  * informations de transport : bon de livraison, transporteur, véhicule, adresse
-- Aucune donnée fictive : seules les réceptions déjà saisies reçoivent un numéro.
-- SQL commun à MySQL et H2 (tests).
-- =====================================================================

ALTER TABLE pressings ADD COLUMN numero_bulletin VARCHAR(6);
ALTER TABLE pressings ADD COLUMN bl_numero VARCHAR(50);
ALTER TABLE pressings ADD COLUMN transporteur VARCHAR(120);
ALTER TABLE pressings ADD COLUMN vehicule VARCHAR(30);
ALTER TABLE pressings ADD COLUMN adresse VARCHAR(255);

-- Réceptions existantes : numéro séquentiel dans l'ordre des id.
-- Table de travail : MySQL refuse un UPDATE qui relit la table modifiée dans une sous-requête.
CREATE TABLE tmp_bulletin_rang (
    id   BIGINT NOT NULL PRIMARY KEY,
    rang BIGINT NOT NULL
);

INSERT INTO tmp_bulletin_rang (id, rang)
SELECT p.id, (SELECT COUNT(*) FROM pressings p2 WHERE p2.id <= p.id) FROM pressings p;

UPDATE pressings
SET numero_bulletin = (SELECT LPAD(CONCAT('', t.rang), 6, '0') FROM tmp_bulletin_rang t WHERE t.id = pressings.id);

DROP TABLE tmp_bulletin_rang;

ALTER TABLE pressings MODIFY numero_bulletin VARCHAR(6) NOT NULL;
ALTER TABLE pressings ADD CONSTRAINT uk_pressings_bulletin UNIQUE (numero_bulletin);

-- La séquence reprend après le dernier numéro attribué (1 si aucune réception).
INSERT INTO document_sequences (type, annee, prochain)
SELECT 'BULLETIN_PRESSAGE', 0, COUNT(*) + 1 FROM pressings;

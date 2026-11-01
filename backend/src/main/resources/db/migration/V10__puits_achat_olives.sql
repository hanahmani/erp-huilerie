-- =====================================================================
-- V10 : puits / silos de stockage et achat d'olives au bulletin de pressage.
--  * storage_tanks : un puits = un lot ENTREPRISE (numéro = code du puits),
--    donc un seul stock vendable en caisse, borné par une capacité.
--  * pressings.mode : SERVICE (le client reprend son huile) ou ACHAT
--    (l'huilerie achète les olives au kg ; l'huile obtenue entre dans un puits).
--  * pressing_seller_payments : paiements au vendeur des olives (sorties de caisse).
-- Aucune donnée créée : les puits sont saisis par l'utilisateur.
-- SQL commun à MySQL et H2 (tests).
-- =====================================================================

CREATE TABLE storage_tanks (
    id         BIGINT        NOT NULL AUTO_INCREMENT PRIMARY KEY,
    code       VARCHAR(30)   NOT NULL,
    nom        VARCHAR(100)  NOT NULL,
    type       VARCHAR(20)   NOT NULL,
    article_id BIGINT        NOT NULL,
    lot_id     BIGINT        NOT NULL,
    capacite   DECIMAL(15,3) NOT NULL,
    actif      BOOLEAN       NOT NULL DEFAULT TRUE,
    created_at DATETIME(6)   NOT NULL,
    CONSTRAINT uk_storage_tanks_code UNIQUE (code),
    CONSTRAINT uk_storage_tanks_lot UNIQUE (lot_id),
    CONSTRAINT fk_storage_tanks_article FOREIGN KEY (article_id) REFERENCES articles (id),
    CONSTRAINT fk_storage_tanks_lot FOREIGN KEY (lot_id) REFERENCES stock_lots (id),
    CONSTRAINT ck_storage_tanks CHECK (capacite > 0 AND type IN ('PUITS', 'SILO', 'CUVE', 'AUTRE'))
);

ALTER TABLE pressings ADD COLUMN mode VARCHAR(20) NOT NULL DEFAULT 'SERVICE';
ALTER TABLE pressings ADD COLUMN prix_olives_kg DECIMAL(12,3);
ALTER TABLE pressings ADD COLUMN montant_achat DECIMAL(12,3);
ALTER TABLE pressings ADD COLUMN montant_regle DECIMAL(12,3) NOT NULL DEFAULT 0;
ALTER TABLE pressings ADD COLUMN tank_id BIGINT;
-- Achat : l'article (huile du puits) n'est connu qu'à la fin du pressage.
ALTER TABLE pressings MODIFY article_id BIGINT NULL;

ALTER TABLE pressings ADD CONSTRAINT fk_pressings_tank FOREIGN KEY (tank_id) REFERENCES storage_tanks (id);
ALTER TABLE pressings ADD CONSTRAINT ck_pressings_mode CHECK (mode IN ('SERVICE', 'ACHAT'));
ALTER TABLE pressings ADD CONSTRAINT ck_pressings_article CHECK (mode = 'ACHAT' OR article_id IS NOT NULL);
ALTER TABLE pressings ADD CONSTRAINT ck_pressings_achat CHECK (
    (mode = 'SERVICE' AND montant_achat IS NULL AND montant_regle = 0)
    OR (mode = 'ACHAT' AND prix_olives_kg >= 0 AND montant_achat >= 0
        AND montant_regle >= 0 AND montant_regle <= montant_achat));

CREATE TABLE pressing_seller_payments (
    id          BIGINT        NOT NULL AUTO_INCREMENT PRIMARY KEY,
    numero      VARCHAR(30)   NOT NULL,
    pressing_id BIGINT        NOT NULL,
    mode        VARCHAR(20)   NOT NULL,
    montant     DECIMAL(12,3) NOT NULL,
    reference   VARCHAR(80),
    date_paie   DATETIME(6)   NOT NULL,
    username    VARCHAR(64)   NOT NULL,
    CONSTRAINT uk_pressing_seller_payments_numero UNIQUE (numero),
    CONSTRAINT fk_pressing_seller_payments_pressing FOREIGN KEY (pressing_id) REFERENCES pressings (id),
    CONSTRAINT ck_pressing_seller_payments CHECK (montant > 0)
);

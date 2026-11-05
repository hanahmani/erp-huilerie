-- =====================================================================
-- V16 : compte d'huile des clients et avances aux vendeurs d'olives.
--  * storage_tanks.client_id : puits ou silo réservé à un client (pressage à façon) ;
--    NULL = puits de l'huilerie (huile achetée, vendable)
--  * client_oil_returns : remise d'huile à un client, sur l'ensemble de ses pressages
--    (quantité en kg ; litres renseignés quand la remise a été saisie en litres)
--  * pressing_returns.client_return_id : part de la remise prise sur chaque pressage ;
--    une remise peut couvrir plusieurs pressages, d'où la fin de l'unicité du numéro
--  * seller_advances : avances payées à un vendeur d'olives avant ses achats ;
--    pressing_seller_payments.advance_id : part d'une avance déduite d'un achat
--    (aucune nouvelle sortie de caisse : l'argent est sorti au versement de l'avance)
-- SQL commun à MySQL et H2 (tests).
-- =====================================================================

ALTER TABLE storage_tanks ADD COLUMN client_id BIGINT;
ALTER TABLE storage_tanks ADD CONSTRAINT fk_storage_tanks_client FOREIGN KEY (client_id) REFERENCES clients (id);

CREATE TABLE client_oil_returns (
    id          BIGINT        NOT NULL AUTO_INCREMENT PRIMARY KEY,
    numero      VARCHAR(30)   NOT NULL,
    client_id   BIGINT        NOT NULL,
    quantite    DECIMAL(15,3) NOT NULL,
    litres      DECIMAL(15,3),
    remis_a     VARCHAR(150)  NOT NULL,
    date_remise DATETIME(6)   NOT NULL,
    username    VARCHAR(64)   NOT NULL,
    CONSTRAINT uk_client_oil_returns_numero UNIQUE (numero),
    CONSTRAINT fk_client_oil_returns_client FOREIGN KEY (client_id) REFERENCES clients (id),
    CONSTRAINT ck_client_oil_returns CHECK (quantite > 0 AND (litres IS NULL OR litres > 0))
);
CREATE INDEX idx_client_oil_returns_client ON client_oil_returns (client_id);

ALTER TABLE pressing_returns DROP CONSTRAINT uk_pressing_returns_numero;
ALTER TABLE pressing_returns ADD COLUMN client_return_id BIGINT;
ALTER TABLE pressing_returns ADD CONSTRAINT fk_pressing_returns_client_return
    FOREIGN KEY (client_return_id) REFERENCES client_oil_returns (id);

CREATE TABLE seller_advances (
    id             BIGINT        NOT NULL AUTO_INCREMENT PRIMARY KEY,
    numero         VARCHAR(30)   NOT NULL,
    client_id      BIGINT        NOT NULL,
    mode           VARCHAR(20)   NOT NULL,
    montant        DECIMAL(12,3) NOT NULL,
    montant_impute DECIMAL(12,3) NOT NULL DEFAULT 0,
    reference      VARCHAR(80),
    date_avance    DATETIME(6)   NOT NULL,
    username       VARCHAR(64)   NOT NULL,
    CONSTRAINT uk_seller_advances_numero UNIQUE (numero),
    CONSTRAINT fk_seller_advances_client FOREIGN KEY (client_id) REFERENCES clients (id),
    CONSTRAINT ck_seller_advances CHECK (montant > 0 AND montant_impute >= 0 AND montant_impute <= montant)
);
CREATE INDEX idx_seller_advances_client ON seller_advances (client_id);

ALTER TABLE pressing_seller_payments ADD COLUMN advance_id BIGINT;
ALTER TABLE pressing_seller_payments ADD CONSTRAINT fk_pressing_seller_payments_advance
    FOREIGN KEY (advance_id) REFERENCES seller_advances (id);

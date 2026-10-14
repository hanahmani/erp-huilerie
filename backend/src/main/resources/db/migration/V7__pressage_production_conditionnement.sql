-- =====================================================================
-- V7 : pressage pour les clients, production (trituration) et conditionnement.
-- Aucune donnée métier : le tarif et l'article « huile client » sont saisis
-- par l'administrateur dans les paramètres.
-- =====================================================================

ALTER TABLE company_settings ADD COLUMN tarif_pressage_kg DECIMAL(12,3);
ALTER TABLE company_settings ADD COLUMN pressage_article_id BIGINT;
ALTER TABLE company_settings ADD CONSTRAINT fk_settings_pressage_article FOREIGN KEY (pressage_article_id) REFERENCES articles (id);

-- ---------- pressage client : l'huile reste la propriété du client
CREATE TABLE pressings (
    id               BIGINT        NOT NULL AUTO_INCREMENT PRIMARY KEY,
    numero           VARCHAR(30)   NOT NULL,
    client_id        BIGINT        NOT NULL,
    article_id       BIGINT        NOT NULL,      -- article « huile client » suivi par lot
    lot_id           BIGINT,                      -- lot CLIENT créé à la fin du pressage
    statut           VARCHAR(20)   NOT NULL,
    date_reception   DATETIME(6)   NOT NULL,
    variete          VARCHAR(80),
    provenance       VARCHAR(120),
    etat_olives      VARCHAR(20)   NOT NULL,
    poids_brut       DECIMAL(15,3) NOT NULL,
    tare             DECIMAL(15,3) NOT NULL,
    poids_net        DECIMAL(15,3) NOT NULL,
    tarif_kg         DECIMAL(12,3) NOT NULL,
    montant          DECIMAL(12,3) NOT NULL,
    montant_paye     DECIMAL(12,3) NOT NULL,
    huile_obtenue    DECIMAL(15,3),
    huile_remise     DECIMAL(15,3) NOT NULL,
    pertes_kg        DECIMAL(15,3),
    date_debut       DATETIME(6),
    date_fin         DATETIME(6),
    notes            VARCHAR(255),
    recu_par         VARCHAR(64)   NOT NULL,
    motif_annulation VARCHAR(255),
    CONSTRAINT uk_pressings_numero UNIQUE (numero),
    CONSTRAINT fk_pressings_client FOREIGN KEY (client_id) REFERENCES clients (id),
    CONSTRAINT fk_pressings_article FOREIGN KEY (article_id) REFERENCES articles (id),
    CONSTRAINT fk_pressings_lot FOREIGN KEY (lot_id) REFERENCES stock_lots (id),
    CONSTRAINT ck_pressings_statut CHECK (statut IN ('RECU','EN_COURS','TERMINE','REMIS','ANNULE')),
    CONSTRAINT ck_pressings_etat CHECK (etat_olives IN ('SAINES','TOMBEES','ABIMEES')),
    CONSTRAINT ck_pressings_poids CHECK (poids_brut > 0 AND tare >= 0 AND poids_net = poids_brut - tare AND poids_net > 0),
    CONSTRAINT ck_pressings_montants CHECK (tarif_kg >= 0 AND montant >= 0 AND montant_paye >= 0 AND montant_paye <= montant),
    CONSTRAINT ck_pressings_huile CHECK (huile_remise >= 0 AND (huile_obtenue IS NULL OR huile_remise <= huile_obtenue))
);
CREATE INDEX idx_pressings_client ON pressings (client_id);
CREATE INDEX idx_pressings_date ON pressings (date_reception);

CREATE TABLE pressing_returns (
    id          BIGINT        NOT NULL AUTO_INCREMENT PRIMARY KEY,
    numero      VARCHAR(30)   NOT NULL,
    pressing_id BIGINT        NOT NULL,
    quantite    DECIMAL(15,3) NOT NULL,
    remis_a     VARCHAR(150)  NOT NULL,
    date_remise DATETIME(6)   NOT NULL,
    username    VARCHAR(64)   NOT NULL,
    CONSTRAINT uk_pressing_returns_numero UNIQUE (numero),
    CONSTRAINT fk_pressing_returns_pressing FOREIGN KEY (pressing_id) REFERENCES pressings (id),
    CONSTRAINT ck_pressing_returns CHECK (quantite > 0)
);

CREATE TABLE pressing_payments (
    id          BIGINT        NOT NULL AUTO_INCREMENT PRIMARY KEY,
    numero      VARCHAR(30)   NOT NULL,
    pressing_id BIGINT        NOT NULL,
    mode        VARCHAR(20)   NOT NULL,
    montant     DECIMAL(12,3) NOT NULL,
    reference   VARCHAR(80),
    date_paie   DATETIME(6)   NOT NULL,
    username    VARCHAR(64)   NOT NULL,
    CONSTRAINT uk_pressing_payments_numero UNIQUE (numero),
    CONSTRAINT fk_pressing_payments_pressing FOREIGN KEY (pressing_id) REFERENCES pressings (id),
    CONSTRAINT ck_pressing_payments CHECK (montant > 0)
);

-- ---------- production : trituration des olives de l'huilerie
CREATE TABLE production_orders (
    id                BIGINT        NOT NULL AUTO_INCREMENT PRIMARY KEY,
    numero            VARCHAR(30)   NOT NULL,
    date_prevue       DATE          NOT NULL,
    statut            VARCHAR(20)   NOT NULL,
    ligne             VARCHAR(60),
    huile_article_id  BIGINT        NOT NULL,
    huile_lot_id      BIGINT,
    olives_kg         DECIMAL(15,3) NOT NULL,
    huile_litres      DECIMAL(15,3),
    pertes_kg         DECIMAL(15,3),
    rendement         DECIMAL(7,3),              -- litres d'huile pour 100 kg d'olives
    acidite           DECIMAL(5,2),
    categorie         VARCHAR(30),
    notes             VARCHAR(255),
    cree_par          VARCHAR(64)   NOT NULL,
    created_at        DATETIME(6)   NOT NULL,
    demarre_le        DATETIME(6),
    termine_le        DATETIME(6),
    termine_par       VARCHAR(64),
    motif_annulation  VARCHAR(255),
    CONSTRAINT uk_production_orders_numero UNIQUE (numero),
    CONSTRAINT fk_production_huile_article FOREIGN KEY (huile_article_id) REFERENCES articles (id),
    CONSTRAINT fk_production_huile_lot FOREIGN KEY (huile_lot_id) REFERENCES stock_lots (id),
    CONSTRAINT ck_production_statut CHECK (statut IN ('PLANIFIE','EN_COURS','TERMINE','ANNULE')),
    CONSTRAINT ck_production_quantites CHECK (olives_kg > 0 AND (huile_litres IS NULL OR huile_litres >= 0)
        AND (pertes_kg IS NULL OR pertes_kg >= 0) AND (acidite IS NULL OR acidite >= 0))
);
CREATE INDEX idx_production_date ON production_orders (date_prevue);

CREATE TABLE production_inputs (
    id          BIGINT        NOT NULL AUTO_INCREMENT PRIMARY KEY,
    order_id    BIGINT        NOT NULL,
    article_id  BIGINT        NOT NULL,
    lot_id      BIGINT,
    quantite    DECIMAL(15,3) NOT NULL,
    CONSTRAINT fk_production_inputs_order FOREIGN KEY (order_id) REFERENCES production_orders (id),
    CONSTRAINT fk_production_inputs_article FOREIGN KEY (article_id) REFERENCES articles (id),
    CONSTRAINT fk_production_inputs_lot FOREIGN KEY (lot_id) REFERENCES stock_lots (id),
    CONSTRAINT ck_production_inputs CHECK (quantite > 0)
);

CREATE TABLE production_byproducts (
    id          BIGINT        NOT NULL AUTO_INCREMENT PRIMARY KEY,
    order_id    BIGINT        NOT NULL,
    article_id  BIGINT        NOT NULL,
    quantite    DECIMAL(15,3) NOT NULL,
    CONSTRAINT fk_production_byproducts_order FOREIGN KEY (order_id) REFERENCES production_orders (id),
    CONSTRAINT fk_production_byproducts_article FOREIGN KEY (article_id) REFERENCES articles (id),
    CONSTRAINT ck_production_byproducts CHECK (quantite > 0)
);

-- ---------- conditionnement : vrac -> bouteilles / bidons
CREATE TABLE packaging_orders (
    id                 BIGINT        NOT NULL AUTO_INCREMENT PRIMARY KEY,
    numero             VARCHAR(30)   NOT NULL,
    statut             VARCHAR(20)   NOT NULL,
    source_article_id  BIGINT        NOT NULL,
    source_lot_id      BIGINT,
    produit_article_id BIGINT        NOT NULL,
    produit_lot_id     BIGINT,
    quantite_produite  DECIMAL(15,3) NOT NULL,
    volume_utilise     DECIMAL(15,3) NOT NULL,
    notes              VARCHAR(255),
    username           VARCHAR(64)   NOT NULL,
    created_at         DATETIME(6)   NOT NULL,
    annule_par         VARCHAR(64),
    motif_annulation   VARCHAR(255),
    CONSTRAINT uk_packaging_orders_numero UNIQUE (numero),
    CONSTRAINT fk_packaging_source_article FOREIGN KEY (source_article_id) REFERENCES articles (id),
    CONSTRAINT fk_packaging_source_lot FOREIGN KEY (source_lot_id) REFERENCES stock_lots (id),
    CONSTRAINT fk_packaging_produit_article FOREIGN KEY (produit_article_id) REFERENCES articles (id),
    CONSTRAINT fk_packaging_produit_lot FOREIGN KEY (produit_lot_id) REFERENCES stock_lots (id),
    CONSTRAINT ck_packaging_statut CHECK (statut IN ('VALIDE','ANNULE')),
    CONSTRAINT ck_packaging_quantites CHECK (quantite_produite > 0 AND volume_utilise > 0)
);

CREATE TABLE packaging_consumptions (
    id          BIGINT        NOT NULL AUTO_INCREMENT PRIMARY KEY,
    order_id    BIGINT        NOT NULL,
    article_id  BIGINT        NOT NULL,
    quantite    DECIMAL(15,3) NOT NULL,
    CONSTRAINT fk_packaging_cons_order FOREIGN KEY (order_id) REFERENCES packaging_orders (id),
    CONSTRAINT fk_packaging_cons_article FOREIGN KEY (article_id) REFERENCES articles (id),
    CONSTRAINT ck_packaging_cons CHECK (quantite > 0)
);

-- Le caissier peut encaisser un pressage
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r CROSS JOIN permissions p
WHERE r.code = 'CAISSIER' AND p.code IN ('PRESSING_VIEW');

-- =====================================================================
-- V5 : ventes rapides, lignes, paiements et journal de caisse.
-- =====================================================================

CREATE TABLE sales (
    id                BIGINT        NOT NULL AUTO_INCREMENT PRIMARY KEY,
    numero_ticket     VARCHAR(30)   NOT NULL,
    idempotency_key   VARCHAR(64)   NOT NULL,     -- empêche une double vente (double clic, réseau)
    client_id         BIGINT,
    caissier_id       BIGINT        NOT NULL,
    date_vente        DATETIME(6)   NOT NULL,
    statut            VARCHAR(20)   NOT NULL,
    sous_total        DECIMAL(12,3) NOT NULL,
    remise            DECIMAL(12,3) NOT NULL,
    total             DECIMAL(12,3) NOT NULL,
    montant_recu      DECIMAL(12,3) NOT NULL,
    montant_paye      DECIMAL(12,3) NOT NULL,
    reste_a_payer     DECIMAL(12,3) NOT NULL,
    monnaie_rendue    DECIMAL(12,3) NOT NULL,
    nb_impressions    INT           NOT NULL DEFAULT 0,
    annulee_par       VARCHAR(64),
    annulee_le        DATETIME(6),
    motif_annulation  VARCHAR(255),
    CONSTRAINT uk_sales_numero UNIQUE (numero_ticket),
    CONSTRAINT uk_sales_idempotency UNIQUE (idempotency_key),
    CONSTRAINT fk_sales_client FOREIGN KEY (client_id) REFERENCES clients (id),
    CONSTRAINT fk_sales_caissier FOREIGN KEY (caissier_id) REFERENCES users (id),
    CONSTRAINT ck_sales_statut CHECK (statut IN ('VALIDEE', 'ANNULEE')),
    CONSTRAINT ck_sales_montants CHECK (sous_total >= 0 AND remise >= 0 AND remise <= sous_total
        AND total = sous_total - remise AND montant_paye >= 0 AND reste_a_payer >= 0
        AND montant_paye + reste_a_payer = total AND monnaie_rendue >= 0),
    CONSTRAINT ck_sales_credit_client CHECK (reste_a_payer = 0 OR client_id IS NOT NULL)
);
CREATE INDEX idx_sales_date ON sales (date_vente);
CREATE INDEX idx_sales_client ON sales (client_id);
CREATE INDEX idx_sales_caissier ON sales (caissier_id);

CREATE TABLE sale_lines (
    id             BIGINT        NOT NULL AUTO_INCREMENT PRIMARY KEY,
    sale_id        BIGINT        NOT NULL,
    article_id     BIGINT        NOT NULL,
    lot_id         BIGINT,
    designation    VARCHAR(200)  NOT NULL,       -- copie au moment de la vente (ticket fidèle)
    unite          VARCHAR(20)   NOT NULL,
    quantite       DECIMAL(15,3) NOT NULL,
    prix_unitaire  DECIMAL(12,3) NOT NULL,
    total_ligne    DECIMAL(12,3) NOT NULL,
    CONSTRAINT fk_sale_lines_sale FOREIGN KEY (sale_id) REFERENCES sales (id),
    CONSTRAINT fk_sale_lines_article FOREIGN KEY (article_id) REFERENCES articles (id),
    CONSTRAINT fk_sale_lines_lot FOREIGN KEY (lot_id) REFERENCES stock_lots (id),
    CONSTRAINT ck_sale_lines CHECK (quantite > 0 AND prix_unitaire >= 0 AND total_ligne >= 0)
);

CREATE TABLE sale_payments (
    id         BIGINT        NOT NULL AUTO_INCREMENT PRIMARY KEY,
    sale_id    BIGINT        NOT NULL,
    mode       VARCHAR(20)   NOT NULL,
    montant    DECIMAL(12,3) NOT NULL,
    created_at DATETIME(6)   NOT NULL,
    CONSTRAINT fk_sale_payments_sale FOREIGN KEY (sale_id) REFERENCES sales (id),
    CONSTRAINT ck_sale_payments CHECK (montant > 0)
);

CREATE TABLE cash_movements (
    id             BIGINT        NOT NULL AUTO_INCREMENT PRIMARY KEY,
    type           VARCHAR(20)   NOT NULL,
    mode           VARCHAR(20)   NOT NULL,
    montant        DECIMAL(12,3) NOT NULL,
    libelle        VARCHAR(255)  NOT NULL,
    reference_type VARCHAR(30),
    reference_id   BIGINT,
    username       VARCHAR(64)   NOT NULL,
    created_at     DATETIME(6)   NOT NULL,
    CONSTRAINT ck_cash_movements_type CHECK (type IN ('ENCAISSEMENT', 'DECAISSEMENT')),
    CONSTRAINT ck_cash_movements_montant CHECK (montant > 0)
);
CREATE INDEX idx_cash_movements_date ON cash_movements (created_at);

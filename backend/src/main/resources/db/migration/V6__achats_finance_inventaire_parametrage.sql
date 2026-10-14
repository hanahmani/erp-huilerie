-- =====================================================================
-- V6 : paramétrage (taxes, rôles personnalisés, fiche utilisateur),
--      achats, réceptions, paiements fournisseurs, règlements clients,
--      dépenses, sessions de caisse, inventaires.
-- Aucune donnée métier : seules de nouvelles permissions techniques.
-- =====================================================================

-- ---------- paramétrage
CREATE TABLE taxes (
    id     BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    nom    VARCHAR(60)  NOT NULL,
    taux   DECIMAL(5,2) NOT NULL,
    actif  BOOLEAN      NOT NULL DEFAULT TRUE,
    CONSTRAINT uk_taxes_nom UNIQUE (nom),
    CONSTRAINT ck_taxes_taux CHECK (taux >= 0 AND taux <= 100)
);

CREATE TABLE expense_categories (
    id    BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    nom   VARCHAR(100) NOT NULL,
    actif BOOLEAN      NOT NULL DEFAULT TRUE,
    CONSTRAINT uk_expense_categories_nom UNIQUE (nom)
);

ALTER TABLE roles ADD COLUMN systeme BOOLEAN NOT NULL DEFAULT FALSE;
UPDATE roles SET systeme = TRUE;

ALTER TABLE users ADD COLUMN email VARCHAR(120);
ALTER TABLE users ADD COLUMN telephone VARCHAR(50);
ALTER TABLE users ADD COLUMN service VARCHAR(100);
ALTER TABLE users ADD COLUMN derniere_connexion DATETIME(6);

-- ---------- achats
CREATE TABLE purchases (
    id                 BIGINT        NOT NULL AUTO_INCREMENT PRIMARY KEY,
    numero             VARCHAR(30)   NOT NULL,
    fournisseur_id     BIGINT        NOT NULL,
    date_commande      DATE          NOT NULL,
    statut             VARCHAR(30)   NOT NULL,
    ref_facture        VARCHAR(60),
    notes              VARCHAR(500),
    total_ht           DECIMAL(12,3) NOT NULL,
    total_tva          DECIMAL(12,3) NOT NULL,
    total_ttc          DECIMAL(12,3) NOT NULL,
    montant_paye       DECIMAL(12,3) NOT NULL,
    cree_par           VARCHAR(64)   NOT NULL,
    created_at         DATETIME(6)   NOT NULL,
    valide_par         VARCHAR(64),
    valide_le          DATETIME(6),
    annule_par         VARCHAR(64),
    motif_annulation   VARCHAR(255),
    CONSTRAINT uk_purchases_numero UNIQUE (numero),
    CONSTRAINT fk_purchases_supplier FOREIGN KEY (fournisseur_id) REFERENCES suppliers (id),
    CONSTRAINT ck_purchases_statut CHECK (statut IN ('BROUILLON','VALIDEE','PARTIELLEMENT_RECUE','RECUE','ANNULEE')),
    CONSTRAINT ck_purchases_montants CHECK (total_ht >= 0 AND total_tva >= 0 AND total_ttc = total_ht + total_tva
        AND montant_paye >= 0 AND montant_paye <= total_ttc)
);
CREATE INDEX idx_purchases_date ON purchases (date_commande);
CREATE INDEX idx_purchases_supplier ON purchases (fournisseur_id);

CREATE TABLE purchase_lines (
    id             BIGINT        NOT NULL AUTO_INCREMENT PRIMARY KEY,
    purchase_id    BIGINT        NOT NULL,
    article_id     BIGINT        NOT NULL,
    designation    VARCHAR(200)  NOT NULL,
    quantite       DECIMAL(15,3) NOT NULL,
    quantite_recue DECIMAL(15,3) NOT NULL,
    prix_unitaire  DECIMAL(12,3) NOT NULL,
    remise_pct     DECIMAL(5,2)  NOT NULL,
    taux_tva       DECIMAL(5,2)  NOT NULL,
    total_ht       DECIMAL(12,3) NOT NULL,
    total_tva      DECIMAL(12,3) NOT NULL,
    CONSTRAINT fk_purchase_lines_purchase FOREIGN KEY (purchase_id) REFERENCES purchases (id),
    CONSTRAINT fk_purchase_lines_article FOREIGN KEY (article_id) REFERENCES articles (id),
    CONSTRAINT ck_purchase_lines CHECK (quantite > 0 AND quantite_recue >= 0 AND quantite_recue <= quantite
        AND prix_unitaire >= 0 AND remise_pct >= 0 AND remise_pct <= 100 AND taux_tva >= 0)
);

CREATE TABLE goods_receipts (
    id          BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    numero      VARCHAR(30)  NOT NULL,
    purchase_id BIGINT       NOT NULL,
    date_recep  DATETIME(6)  NOT NULL,
    username    VARCHAR(64)  NOT NULL,
    notes       VARCHAR(255),
    CONSTRAINT uk_goods_receipts_numero UNIQUE (numero),
    CONSTRAINT fk_goods_receipts_purchase FOREIGN KEY (purchase_id) REFERENCES purchases (id)
);

CREATE TABLE goods_receipt_lines (
    id               BIGINT        NOT NULL AUTO_INCREMENT PRIMARY KEY,
    receipt_id       BIGINT        NOT NULL,
    purchase_line_id BIGINT        NOT NULL,
    lot_id           BIGINT,
    quantite         DECIMAL(15,3) NOT NULL,
    CONSTRAINT fk_grl_receipt FOREIGN KEY (receipt_id) REFERENCES goods_receipts (id),
    CONSTRAINT fk_grl_line FOREIGN KEY (purchase_line_id) REFERENCES purchase_lines (id),
    CONSTRAINT fk_grl_lot FOREIGN KEY (lot_id) REFERENCES stock_lots (id),
    CONSTRAINT ck_grl_quantite CHECK (quantite > 0)
);

CREATE TABLE supplier_payments (
    id          BIGINT        NOT NULL AUTO_INCREMENT PRIMARY KEY,
    numero      VARCHAR(30)   NOT NULL,
    purchase_id BIGINT        NOT NULL,
    mode        VARCHAR(20)   NOT NULL,
    montant     DECIMAL(12,3) NOT NULL,
    reference   VARCHAR(80),
    date_paie   DATETIME(6)   NOT NULL,
    username    VARCHAR(64)   NOT NULL,
    CONSTRAINT uk_supplier_payments_numero UNIQUE (numero),
    CONSTRAINT fk_supplier_payments_purchase FOREIGN KEY (purchase_id) REFERENCES purchases (id),
    CONSTRAINT ck_supplier_payments CHECK (montant > 0)
);

-- ---------- règlements de ventes à crédit
CREATE TABLE client_payments (
    id        BIGINT        NOT NULL AUTO_INCREMENT PRIMARY KEY,
    numero    VARCHAR(30)   NOT NULL,
    sale_id   BIGINT        NOT NULL,
    mode      VARCHAR(20)   NOT NULL,
    montant   DECIMAL(12,3) NOT NULL,
    reference VARCHAR(80),
    date_paie DATETIME(6)   NOT NULL,
    username  VARCHAR(64)   NOT NULL,
    CONSTRAINT uk_client_payments_numero UNIQUE (numero),
    CONSTRAINT fk_client_payments_sale FOREIGN KEY (sale_id) REFERENCES sales (id),
    CONSTRAINT ck_client_payments CHECK (montant > 0)
);

-- ---------- dépenses
CREATE TABLE expenses (
    id            BIGINT        NOT NULL AUTO_INCREMENT PRIMARY KEY,
    numero        VARCHAR(30)   NOT NULL,
    categorie_id  BIGINT        NOT NULL,
    libelle       VARCHAR(255)  NOT NULL,
    montant       DECIMAL(12,3) NOT NULL,
    mode          VARCHAR(20)   NOT NULL,
    reference     VARCHAR(80),
    date_depense  DATE          NOT NULL,
    statut        VARCHAR(20)   NOT NULL,
    username      VARCHAR(64)   NOT NULL,
    created_at    DATETIME(6)   NOT NULL,
    motif_annulation VARCHAR(255),
    CONSTRAINT uk_expenses_numero UNIQUE (numero),
    CONSTRAINT fk_expenses_categorie FOREIGN KEY (categorie_id) REFERENCES expense_categories (id),
    CONSTRAINT ck_expenses CHECK (montant > 0 AND statut IN ('VALIDEE','ANNULEE'))
);
CREATE INDEX idx_expenses_date ON expenses (date_depense);

-- ---------- sessions de caisse (une seule ouverte : ouverte_flag = 1 unique, NULL une fois fermée)
CREATE TABLE cash_sessions (
    id                  BIGINT        NOT NULL AUTO_INCREMENT PRIMARY KEY,
    ouverte_flag        INT,
    ouverte_par         VARCHAR(64)   NOT NULL,
    ouverte_le          DATETIME(6)   NOT NULL,
    fond_initial        DECIMAL(12,3) NOT NULL,
    fermee_par          VARCHAR(64),
    fermee_le           DATETIME(6),
    especes_theoriques  DECIMAL(12,3),
    especes_comptees    DECIMAL(12,3),
    ecart               DECIMAL(12,3),
    justification       VARCHAR(255),
    CONSTRAINT uk_cash_sessions_open UNIQUE (ouverte_flag),
    CONSTRAINT ck_cash_sessions CHECK (fond_initial >= 0 AND (ouverte_flag IS NULL OR ouverte_flag = 1))
);

-- ---------- inventaires physiques
CREATE TABLE inventories (
    id           BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    numero       VARCHAR(30)  NOT NULL,
    libelle      VARCHAR(150) NOT NULL,
    statut       VARCHAR(20)  NOT NULL,
    cree_par     VARCHAR(64)  NOT NULL,
    created_at   DATETIME(6)  NOT NULL,
    valide_par   VARCHAR(64),
    valide_le    DATETIME(6),
    CONSTRAINT uk_inventories_numero UNIQUE (numero),
    CONSTRAINT ck_inventories_statut CHECK (statut IN ('EN_COURS','VALIDE','ANNULE'))
);

CREATE TABLE inventory_lines (
    id                  BIGINT        NOT NULL AUTO_INCREMENT PRIMARY KEY,
    inventory_id        BIGINT        NOT NULL,
    article_id          BIGINT        NOT NULL,
    lot_id              BIGINT,
    quantite_theorique  DECIMAL(15,3) NOT NULL,
    quantite_comptee    DECIMAL(15,3),
    CONSTRAINT fk_inventory_lines_inv FOREIGN KEY (inventory_id) REFERENCES inventories (id),
    CONSTRAINT fk_inventory_lines_article FOREIGN KEY (article_id) REFERENCES articles (id),
    CONSTRAINT fk_inventory_lines_lot FOREIGN KEY (lot_id) REFERENCES stock_lots (id),
    CONSTRAINT ck_inventory_lines CHECK (quantite_comptee IS NULL OR quantite_comptee >= 0)
);

-- ---------- nouvelles permissions techniques
INSERT INTO permissions (code, description) VALUES
 ('ROLE_MANAGE',        'Créer et modifier les rôles et leurs permissions'),
 ('PURCHASE_VALIDATE',  'Valider ou annuler une commande fournisseur'),
 ('PURCHASE_RECEIVE',   'Réceptionner les marchandises'),
 ('CASH_SESSION',       'Ouvrir et fermer la caisse'),
 ('REPORT_VIEW',        'Consulter les rapports et statistiques');

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r CROSS JOIN permissions p
WHERE r.code = 'ADMIN' AND p.code IN ('ROLE_MANAGE','PURCHASE_VALIDATE','PURCHASE_RECEIVE','CASH_SESSION','REPORT_VIEW');

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r CROSS JOIN permissions p
WHERE (r.code = 'CAISSIER' AND p.code IN ('CASH_SESSION'))
   OR (r.code = 'RESP_STOCK' AND p.code IN ('PURCHASE_RECEIVE','REPORT_VIEW'))
   OR (r.code = 'RESP_ACHATS' AND p.code IN ('PURCHASE_VALIDATE','PURCHASE_RECEIVE','REPORT_VIEW','FINANCE_VIEW'))
   OR (r.code = 'RESP_PRODUCTION' AND p.code IN ('REPORT_VIEW'))
   OR (r.code = 'RESP_FINANCIER' AND p.code IN ('FINANCE_MANAGE','REPORT_VIEW'));

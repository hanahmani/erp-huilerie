-- =====================================================================
-- V3 : paramètres de l'huilerie, numérotation, catalogue, clients, fournisseurs.
-- Tables créées VIDES : aucune donnée métier n'est insérée.
-- =====================================================================

CREATE TABLE company_settings (
    id                      BIGINT       NOT NULL PRIMARY KEY,   -- ligne unique, id = 1
    raison_sociale          VARCHAR(150) NOT NULL,
    adresse                 VARCHAR(255),
    telephone               VARCHAR(50),
    email                   VARCHAR(120),
    matricule_fiscal        VARCHAR(50),
    message_ticket          VARCHAR(255),
    vente_credit_autorisee  BOOLEAN      NOT NULL DEFAULT FALSE,
    updated_at              DATETIME(6)  NOT NULL,
    CONSTRAINT ck_company_settings_single CHECK (id = 1)
);

-- Numérotation séquentielle sans trou : la ligne est verrouillée dans la transaction du document.
CREATE TABLE document_sequences (
    id       BIGINT      NOT NULL AUTO_INCREMENT PRIMARY KEY,
    type     VARCHAR(30) NOT NULL,
    annee    INT         NOT NULL,
    prochain BIGINT      NOT NULL,
    CONSTRAINT uk_document_sequences UNIQUE (type, annee),
    CONSTRAINT ck_document_sequences_prochain CHECK (prochain >= 1)
);

CREATE TABLE units (
    id      BIGINT      NOT NULL AUTO_INCREMENT PRIMARY KEY,
    code    VARCHAR(20) NOT NULL,
    libelle VARCHAR(80) NOT NULL,
    actif   BOOLEAN     NOT NULL DEFAULT TRUE,
    CONSTRAINT uk_units_code UNIQUE (code)
);

CREATE TABLE categories (
    id          BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    nom         VARCHAR(100) NOT NULL,
    description VARCHAR(255),
    actif       BOOLEAN      NOT NULL DEFAULT TRUE,
    CONSTRAINT uk_categories_nom UNIQUE (nom)
);

CREATE TABLE articles (
    id           BIGINT         NOT NULL AUTO_INCREMENT PRIMARY KEY,
    reference    VARCHAR(50)    NOT NULL,
    nom          VARCHAR(150)   NOT NULL,
    description  VARCHAR(500),
    type         VARCHAR(30)    NOT NULL,
    categorie_id BIGINT,
    unite_id     BIGINT         NOT NULL,
    contenance   DECIMAL(12,3),               -- litres par unité (ex. bouteille 1 L), facultatif
    prix_achat   DECIMAL(12,3),
    prix_vente   DECIMAL(12,3),
    seuil_min    DECIMAL(15,3),
    code_barres  VARCHAR(64),
    vendable     BOOLEAN        NOT NULL DEFAULT FALSE,
    suivi_lot    BOOLEAN        NOT NULL DEFAULT FALSE,
    actif        BOOLEAN        NOT NULL DEFAULT TRUE,
    created_at   DATETIME(6)    NOT NULL,
    updated_at   DATETIME(6)    NOT NULL,
    CONSTRAINT uk_articles_reference UNIQUE (reference),
    CONSTRAINT uk_articles_code_barres UNIQUE (code_barres),
    CONSTRAINT fk_articles_categorie FOREIGN KEY (categorie_id) REFERENCES categories (id),
    CONSTRAINT fk_articles_unite FOREIGN KEY (unite_id) REFERENCES units (id),
    CONSTRAINT ck_articles_prix CHECK ((prix_achat IS NULL OR prix_achat >= 0) AND (prix_vente IS NULL OR prix_vente >= 0)),
    CONSTRAINT ck_articles_seuil CHECK (seuil_min IS NULL OR seuil_min >= 0),
    CONSTRAINT ck_articles_contenance CHECK (contenance IS NULL OR contenance > 0),
    CONSTRAINT ck_articles_vendable_prix CHECK (vendable = FALSE OR prix_vente IS NOT NULL)
);
CREATE INDEX idx_articles_nom ON articles (nom);

CREATE TABLE clients (
    id               BIGINT        NOT NULL AUTO_INCREMENT PRIMARY KEY,
    code             VARCHAR(20)   NOT NULL,
    type             VARCHAR(20)   NOT NULL,
    nom              VARCHAR(150)  NOT NULL,
    telephone        VARCHAR(50),
    adresse          VARCHAR(255),
    cin              VARCHAR(20),
    matricule_fiscal VARCHAR(50),
    plafond_credit   DECIMAL(12,3),
    actif            BOOLEAN       NOT NULL DEFAULT TRUE,
    created_at       DATETIME(6)   NOT NULL,
    updated_at       DATETIME(6)   NOT NULL,
    CONSTRAINT uk_clients_code UNIQUE (code),
    CONSTRAINT ck_clients_plafond CHECK (plafond_credit IS NULL OR plafond_credit >= 0)
);
CREATE INDEX idx_clients_nom ON clients (nom);
CREATE INDEX idx_clients_telephone ON clients (telephone);

CREATE TABLE suppliers (
    id               BIGINT        NOT NULL AUTO_INCREMENT PRIMARY KEY,
    code             VARCHAR(20)   NOT NULL,
    type             VARCHAR(20)   NOT NULL,
    nom              VARCHAR(150)  NOT NULL,
    telephone        VARCHAR(50),
    adresse          VARCHAR(255),
    cin              VARCHAR(20),
    matricule_fiscal VARCHAR(50),
    actif            BOOLEAN       NOT NULL DEFAULT TRUE,
    created_at       DATETIME(6)   NOT NULL,
    updated_at       DATETIME(6)   NOT NULL,
    CONSTRAINT uk_suppliers_code UNIQUE (code)
);
CREATE INDEX idx_suppliers_nom ON suppliers (nom);

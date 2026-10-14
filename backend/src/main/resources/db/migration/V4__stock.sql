-- =====================================================================
-- V4 : stock calculé par mouvements.
--  * stock_movements : journal (source de vérité), jamais modifié.
--  * stock_balances  : projection mise à jour dans la même transaction,
--                      CHECK quantite >= 0 => stock négatif impossible en base.
--  * stock_lots      : propriétaire ENTREPRISE ou CLIENT (huile de pressage).
-- =====================================================================

CREATE TABLE stock_lots (
    id             BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    article_id     BIGINT       NOT NULL,
    numero         VARCHAR(50)  NOT NULL,
    proprietaire   VARCHAR(20)  NOT NULL,
    client_id      BIGINT,
    statut         VARCHAR(20)  NOT NULL,
    motif_blocage  VARCHAR(255),
    created_at     DATETIME(6)  NOT NULL,
    CONSTRAINT uk_stock_lots UNIQUE (article_id, numero),
    CONSTRAINT fk_stock_lots_article FOREIGN KEY (article_id) REFERENCES articles (id),
    CONSTRAINT fk_stock_lots_client FOREIGN KEY (client_id) REFERENCES clients (id),
    CONSTRAINT ck_stock_lots_proprietaire CHECK (
        (proprietaire = 'ENTREPRISE' AND client_id IS NULL) OR
        (proprietaire = 'CLIENT' AND client_id IS NOT NULL)),
    CONSTRAINT ck_stock_lots_statut CHECK (statut IN ('DISPONIBLE', 'BLOQUE'))
);

CREATE TABLE stock_balances (
    id          BIGINT        NOT NULL AUTO_INCREMENT PRIMARY KEY,
    article_id  BIGINT        NOT NULL,
    lot_id      BIGINT,
    lot_ref     BIGINT        NOT NULL,          -- id du lot, ou 0 pour un article sans lot
    quantite    DECIMAL(15,3) NOT NULL,
    updated_at  DATETIME(6)   NOT NULL,
    CONSTRAINT uk_stock_balances UNIQUE (article_id, lot_ref),
    CONSTRAINT fk_stock_balances_article FOREIGN KEY (article_id) REFERENCES articles (id),
    CONSTRAINT fk_stock_balances_lot FOREIGN KEY (lot_id) REFERENCES stock_lots (id),
    CONSTRAINT ck_stock_balances_positif CHECK (quantite >= 0)
);

CREATE TABLE stock_movements (
    id            BIGINT        NOT NULL AUTO_INCREMENT PRIMARY KEY,
    article_id    BIGINT        NOT NULL,
    lot_id        BIGINT,
    type          VARCHAR(30)   NOT NULL,
    sens          INT           NOT NULL,
    quantite      DECIMAL(15,3) NOT NULL,
    motif         VARCHAR(255),
    document_type VARCHAR(30),
    document_id   BIGINT,
    username      VARCHAR(64)   NOT NULL,
    created_at    DATETIME(6)   NOT NULL,
    CONSTRAINT fk_stock_mvt_article FOREIGN KEY (article_id) REFERENCES articles (id),
    CONSTRAINT fk_stock_mvt_lot FOREIGN KEY (lot_id) REFERENCES stock_lots (id),
    CONSTRAINT ck_stock_mvt_sens CHECK (sens IN (1, -1)),
    CONSTRAINT ck_stock_mvt_quantite CHECK (quantite > 0)
);
CREATE INDEX idx_stock_mvt_article_date ON stock_movements (article_id, created_at);
CREATE INDEX idx_stock_mvt_document ON stock_movements (document_type, document_id);
CREATE INDEX idx_stock_mvt_date ON stock_movements (created_at);

-- =====================================================================
-- V8 : alignement sur le cahier des charges
--  * fiches clients / fournisseurs / articles complétées, IP dans l'audit
--  * cycle commercial : devis → commande → livraison → facture → paiement, avoirs
--  * contrôle qualité : critères, contrôles, anomalies
--  * ressources humaines : services, postes, employés, congés, absences, jours fériés
--  * notifications (statut de lecture)
-- Aucune donnée métier fictive : seuls des rôles et permissions techniques.
-- =====================================================================

ALTER TABLE clients ADD COLUMN email VARCHAR(120);
ALTER TABLE clients ADD COLUMN conditions_paiement VARCHAR(100);
ALTER TABLE suppliers ADD COLUMN email VARCHAR(120);
ALTER TABLE suppliers ADD COLUMN conditions_paiement VARCHAR(100);
ALTER TABLE suppliers ADD COLUMN banque VARCHAR(100);
ALTER TABLE suppliers ADD COLUMN rib VARCHAR(34);
ALTER TABLE articles ADD COLUMN stock_max DECIMAL(15,3);
ALTER TABLE audit_logs ADD COLUMN ip VARCHAR(45);
ALTER TABLE company_settings ADD COLUMN timbre_fiscal DECIMAL(12,3);
ALTER TABLE company_settings ADD COLUMN inactivite_minutes INT;

-- ---------- cycle commercial
CREATE TABLE sales_documents (
    id               BIGINT        NOT NULL AUTO_INCREMENT PRIMARY KEY,
    type             VARCHAR(20)   NOT NULL,
    numero           VARCHAR(30),                -- attribué à la validation (sans trou)
    client_id        BIGINT        NOT NULL,
    source_id        BIGINT,
    date_doc         DATE          NOT NULL,
    echeance         DATE,
    statut           VARCHAR(20)   NOT NULL,
    ref_client       VARCHAR(60),
    notes            VARCHAR(500),
    total_ht         DECIMAL(12,3) NOT NULL,
    total_tva        DECIMAL(12,3) NOT NULL,
    timbre           DECIMAL(12,3) NOT NULL,
    total_ttc        DECIMAL(12,3) NOT NULL,
    montant_paye     DECIMAL(12,3) NOT NULL,
    cree_par         VARCHAR(64)   NOT NULL,
    created_at       DATETIME(6)   NOT NULL,
    valide_par       VARCHAR(64),
    valide_le        DATETIME(6),
    motif_annulation VARCHAR(255),
    CONSTRAINT uk_sales_documents_numero UNIQUE (numero),
    CONSTRAINT fk_sales_documents_client FOREIGN KEY (client_id) REFERENCES clients (id),
    CONSTRAINT fk_sales_documents_source FOREIGN KEY (source_id) REFERENCES sales_documents (id),
    CONSTRAINT ck_sales_documents_type CHECK (type IN ('DEVIS','COMMANDE','LIVRAISON','FACTURE','AVOIR')),
    CONSTRAINT ck_sales_documents_statut CHECK (statut IN ('BROUILLON','VALIDE','TRANSFORME','ANNULE')),
    CONSTRAINT ck_sales_documents_montants CHECK (total_ht >= 0 AND total_tva >= 0 AND timbre >= 0
        AND total_ttc = total_ht + total_tva + timbre AND montant_paye >= 0 AND montant_paye <= total_ttc),
    CONSTRAINT ck_sales_documents_numero CHECK (statut = 'BROUILLON' OR numero IS NOT NULL OR statut = 'ANNULE')
);
CREATE INDEX idx_sales_documents_type_date ON sales_documents (type, date_doc);
CREATE INDEX idx_sales_documents_client ON sales_documents (client_id);

CREATE TABLE sales_document_lines (
    id             BIGINT        NOT NULL AUTO_INCREMENT PRIMARY KEY,
    document_id    BIGINT        NOT NULL,
    article_id     BIGINT        NOT NULL,
    lot_id         BIGINT,
    designation    VARCHAR(200)  NOT NULL,
    unite          VARCHAR(20)   NOT NULL,
    quantite       DECIMAL(15,3) NOT NULL,
    prix_unitaire  DECIMAL(12,3) NOT NULL,
    remise_pct     DECIMAL(5,2)  NOT NULL,
    taux_tva       DECIMAL(5,2)  NOT NULL,
    total_ht       DECIMAL(12,3) NOT NULL,
    total_tva      DECIMAL(12,3) NOT NULL,
    CONSTRAINT fk_sdl_document FOREIGN KEY (document_id) REFERENCES sales_documents (id),
    CONSTRAINT fk_sdl_article FOREIGN KEY (article_id) REFERENCES articles (id),
    CONSTRAINT fk_sdl_lot FOREIGN KEY (lot_id) REFERENCES stock_lots (id),
    CONSTRAINT ck_sdl CHECK (quantite > 0 AND prix_unitaire >= 0 AND remise_pct >= 0 AND remise_pct <= 100 AND taux_tva >= 0)
);

CREATE TABLE sales_document_payments (
    id          BIGINT        NOT NULL AUTO_INCREMENT PRIMARY KEY,
    numero      VARCHAR(30)   NOT NULL,
    document_id BIGINT        NOT NULL,
    mode        VARCHAR(20)   NOT NULL,
    montant     DECIMAL(12,3) NOT NULL,
    reference   VARCHAR(80),
    date_paie   DATETIME(6)   NOT NULL,
    username    VARCHAR(64)   NOT NULL,
    CONSTRAINT uk_sdp_numero UNIQUE (numero),
    CONSTRAINT fk_sdp_document FOREIGN KEY (document_id) REFERENCES sales_documents (id),
    CONSTRAINT ck_sdp CHECK (montant > 0)
);

-- ---------- contrôle qualité
CREATE TABLE quality_criteria (
    id          BIGINT        NOT NULL AUTO_INCREMENT PRIMARY KEY,
    nom         VARCHAR(100)  NOT NULL,
    unite       VARCHAR(30),
    valeur_min  DECIMAL(12,4),
    valeur_max  DECIMAL(12,4),
    actif       BOOLEAN       NOT NULL DEFAULT TRUE,
    CONSTRAINT uk_quality_criteria_nom UNIQUE (nom),
    CONSTRAINT ck_quality_criteria CHECK (valeur_min IS NOT NULL OR valeur_max IS NOT NULL)
);

CREATE TABLE quality_controls (
    id            BIGINT        NOT NULL AUTO_INCREMENT PRIMARY KEY,
    numero        VARCHAR(30)   NOT NULL,
    lot_id        BIGINT        NOT NULL,
    date_controle DATETIME(6)   NOT NULL,
    controleur    VARCHAR(64)   NOT NULL,
    resultat      VARCHAR(20)   NOT NULL,
    observations  VARCHAR(500),
    CONSTRAINT uk_quality_controls_numero UNIQUE (numero),
    CONSTRAINT fk_quality_controls_lot FOREIGN KEY (lot_id) REFERENCES stock_lots (id),
    CONSTRAINT ck_quality_controls CHECK (resultat IN ('CONFORME','NON_CONFORME'))
);

CREATE TABLE quality_results (
    id           BIGINT        NOT NULL AUTO_INCREMENT PRIMARY KEY,
    control_id   BIGINT        NOT NULL,
    criterion_id BIGINT        NOT NULL,
    valeur       DECIMAL(12,4) NOT NULL,
    conforme     BOOLEAN       NOT NULL,
    CONSTRAINT fk_quality_results_control FOREIGN KEY (control_id) REFERENCES quality_controls (id),
    CONSTRAINT fk_quality_results_criterion FOREIGN KEY (criterion_id) REFERENCES quality_criteria (id)
);

CREATE TABLE quality_anomalies (
    id                BIGINT        NOT NULL AUTO_INCREMENT PRIMARY KEY,
    numero            VARCHAR(30)   NOT NULL,
    lot_id            BIGINT,
    control_id        BIGINT,
    description       VARCHAR(500)  NOT NULL,
    gravite           VARCHAR(20)   NOT NULL,
    action_corrective VARCHAR(500),
    statut            VARCHAR(20)   NOT NULL,
    cree_par          VARCHAR(64)   NOT NULL,
    created_at        DATETIME(6)   NOT NULL,
    cloture_le        DATETIME(6),
    CONSTRAINT uk_quality_anomalies_numero UNIQUE (numero),
    CONSTRAINT fk_quality_anomalies_lot FOREIGN KEY (lot_id) REFERENCES stock_lots (id),
    CONSTRAINT fk_quality_anomalies_control FOREIGN KEY (control_id) REFERENCES quality_controls (id),
    CONSTRAINT ck_quality_anomalies CHECK (gravite IN ('MINEURE','MAJEURE','CRITIQUE') AND statut IN ('OUVERTE','EN_COURS','CLOTUREE'))
);

-- ---------- ressources humaines
CREATE TABLE hr_services (
    id    BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    nom   VARCHAR(100) NOT NULL,
    actif BOOLEAN      NOT NULL DEFAULT TRUE,
    CONSTRAINT uk_hr_services UNIQUE (nom)
);

CREATE TABLE hr_positions (
    id    BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    nom   VARCHAR(100) NOT NULL,
    actif BOOLEAN      NOT NULL DEFAULT TRUE,
    CONSTRAINT uk_hr_positions UNIQUE (nom)
);

CREATE TABLE employees (
    id               BIGINT        NOT NULL AUTO_INCREMENT PRIMARY KEY,
    matricule        VARCHAR(20)   NOT NULL,
    nom              VARCHAR(100)  NOT NULL,
    prenom           VARCHAR(100)  NOT NULL,
    date_naissance   DATE,
    cin              VARCHAR(20),
    telephone        VARCHAR(50),
    email            VARCHAR(120),
    adresse          VARCHAR(255),
    service_id       BIGINT,
    poste_id         BIGINT,
    date_embauche    DATE          NOT NULL,
    type_contrat     VARCHAR(20)   NOT NULL,
    date_fin_contrat DATE,
    statut           VARCHAR(20)   NOT NULL,
    solde_conges     DECIMAL(5,1)  NOT NULL,
    user_id          BIGINT,
    created_at       DATETIME(6)   NOT NULL,
    updated_at       DATETIME(6)   NOT NULL,
    CONSTRAINT uk_employees_matricule UNIQUE (matricule),
    CONSTRAINT uk_employees_user UNIQUE (user_id),
    CONSTRAINT fk_employees_service FOREIGN KEY (service_id) REFERENCES hr_services (id),
    CONSTRAINT fk_employees_poste FOREIGN KEY (poste_id) REFERENCES hr_positions (id),
    CONSTRAINT fk_employees_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT ck_employees CHECK (type_contrat IN ('CDI','CDD','SAISONNIER','STAGE','AUTRE')
        AND statut IN ('ACTIF','SUSPENDU','SORTI') AND solde_conges >= 0
        AND (date_fin_contrat IS NULL OR date_fin_contrat >= date_embauche))
);

CREATE TABLE public_holidays (
    id      BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    jour    DATE         NOT NULL,
    libelle VARCHAR(100) NOT NULL,
    CONSTRAINT uk_public_holidays UNIQUE (jour)
);

CREATE TABLE leave_requests (
    id           BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    employee_id  BIGINT       NOT NULL,
    type         VARCHAR(20)  NOT NULL,
    date_debut   DATE         NOT NULL,
    date_fin     DATE         NOT NULL,
    jours        DECIMAL(5,1) NOT NULL,
    motif        VARCHAR(255),
    statut       VARCHAR(20)  NOT NULL,
    demande_par  VARCHAR(64)  NOT NULL,
    demande_le   DATETIME(6)  NOT NULL,
    decide_par   VARCHAR(64),
    decide_le    DATETIME(6),
    motif_refus  VARCHAR(255),
    CONSTRAINT fk_leave_requests_employee FOREIGN KEY (employee_id) REFERENCES employees (id),
    CONSTRAINT ck_leave_requests CHECK (date_fin >= date_debut AND jours > 0
        AND type IN ('CONGE_ANNUEL','MALADIE','SANS_SOLDE','EXCEPTIONNEL')
        AND statut IN ('EN_ATTENTE','VALIDEE','REFUSEE','ANNULEE'))
);
CREATE INDEX idx_leave_requests_employee ON leave_requests (employee_id, date_debut);

CREATE TABLE absences (
    id          BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    employee_id BIGINT       NOT NULL,
    jour        DATE         NOT NULL,
    duree       DECIMAL(3,1) NOT NULL,
    motif       VARCHAR(255),
    justifiee   BOOLEAN      NOT NULL,
    saisi_par   VARCHAR(64)  NOT NULL,
    CONSTRAINT fk_absences_employee FOREIGN KEY (employee_id) REFERENCES employees (id),
    CONSTRAINT uk_absences UNIQUE (employee_id, jour),
    CONSTRAINT ck_absences CHECK (duree IN (0.5, 1.0))
);

-- ---------- notifications : les alertes sont calculées, seul l'état « lu » est stocké
CREATE TABLE notification_reads (
    id        BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    user_id   BIGINT       NOT NULL,
    notif_key VARCHAR(120) NOT NULL,
    read_at   DATETIME(6)  NOT NULL,
    CONSTRAINT uk_notification_reads UNIQUE (user_id, notif_key),
    CONSTRAINT fk_notification_reads_user FOREIGN KEY (user_id) REFERENCES users (id)
);

-- ---------- permissions et rôles techniques
INSERT INTO permissions (code, description) VALUES
 ('SALESDOC_MANAGE',  'Créer devis, commandes, livraisons et factures'),
 ('INVOICE_VALIDATE', 'Valider les factures et émettre des avoirs'),
 ('QUALITY_VIEW',     'Consulter les contrôles qualité'),
 ('QUALITY_MANAGE',   'Saisir les contrôles, critères et anomalies'),
 ('HR_VIEW',          'Consulter les dossiers du personnel'),
 ('HR_MANAGE',        'Gérer employés, contrats et absences'),
 ('LEAVE_REQUEST',    'Demander un congé pour soi'),
 ('LEAVE_VALIDATE',   'Valider ou refuser les congés');

INSERT INTO roles (code, nom, description, systeme) VALUES
 ('RESP_COMMERCIAL', 'Responsable commercial', 'Clients, devis, commandes, livraisons, facturation', TRUE),
 ('RESP_QUALITE',    'Responsable qualité',    'Critères, contrôles, anomalies et blocage des lots', TRUE),
 ('RESP_RH',         'Responsable RH',         'Employés, contrats, congés et absences', TRUE),
 ('EMPLOYE',         'Employé',                'Consultation de son activité et demandes de congé', TRUE);

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r CROSS JOIN permissions p
WHERE r.code = 'ADMIN' AND p.code IN ('SALESDOC_MANAGE','INVOICE_VALIDATE','QUALITY_VIEW','QUALITY_MANAGE',
                                      'HR_VIEW','HR_MANAGE','LEAVE_REQUEST','LEAVE_VALIDATE');

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r CROSS JOIN permissions p
WHERE (r.code = 'RESP_COMMERCIAL' AND p.code IN ('DASHBOARD_VIEW','CLIENT_VIEW','CLIENT_MANAGE','CATALOG_VIEW','STOCK_VIEW',
          'SALES_VIEW','SALESDOC_MANAGE','INVOICE_VALIDATE','REPORT_VIEW','LEAVE_REQUEST'))
   OR (r.code = 'RESP_QUALITE' AND p.code IN ('DASHBOARD_VIEW','STOCK_VIEW','STOCK_ADJUST','PRODUCTION_VIEW','QUALITY_VIEW',
          'QUALITY_MANAGE','REPORT_VIEW','LEAVE_REQUEST'))
   OR (r.code = 'RESP_RH' AND p.code IN ('DASHBOARD_VIEW','HR_VIEW','HR_MANAGE','LEAVE_REQUEST','LEAVE_VALIDATE'))
   OR (r.code = 'EMPLOYE' AND p.code IN ('DASHBOARD_VIEW','LEAVE_REQUEST'))
   OR (r.code IN ('CAISSIER','RESP_STOCK','RESP_PRODUCTION','RESP_ACHATS','RESP_FINANCIER') AND p.code = 'LEAVE_REQUEST')
   OR (r.code = 'RESP_PRODUCTION' AND p.code IN ('QUALITY_VIEW','QUALITY_MANAGE'))
   OR (r.code = 'RESP_FINANCIER' AND p.code IN ('SALESDOC_MANAGE','INVOICE_VALIDATE'));

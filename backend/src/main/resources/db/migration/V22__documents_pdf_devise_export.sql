-- =====================================================================
-- V22 : documents commerciaux en PDF.
--  * Paramètres société : RNE, mentions des factures et devis, mentions export
--    et échantillon, logo téléversé (stocké en base, donc inclus dans la sauvegarde).
--  * Client : pays.
--  * Document : devise (TND par défaut, 3 décimales ; EUR / USD, 2 décimales),
--    régime (local, export, échantillons) et informations export.
--  * Ligne : code SH (nomenclature douanière).
-- SQL commun à MySQL et H2 (tests).
-- =====================================================================

ALTER TABLE company_settings ADD COLUMN rne VARCHAR(50);
ALTER TABLE company_settings ADD COLUMN mentions_facture VARCHAR(1000);
ALTER TABLE company_settings ADD COLUMN mentions_devis VARCHAR(1000);
ALTER TABLE company_settings ADD COLUMN mention_export VARCHAR(255);
ALTER TABLE company_settings ADD COLUMN mention_echantillon VARCHAR(255);
ALTER TABLE company_settings ADD COLUMN logo_data LONGBLOB;
ALTER TABLE company_settings ADD COLUMN logo_type VARCHAR(30);

-- Mention légale proposée par défaut, modifiable dans Paramètres.
UPDATE company_settings SET mention_export = 'Exonéré de TVA – export' WHERE mention_export IS NULL;

ALTER TABLE clients ADD COLUMN pays VARCHAR(60);

ALTER TABLE sales_documents ADD COLUMN devise VARCHAR(3) NOT NULL DEFAULT 'TND';
ALTER TABLE sales_documents ADD COLUMN regime VARCHAR(20) NOT NULL DEFAULT 'LOCAL';
ALTER TABLE sales_documents ADD COLUMN pays_origine VARCHAR(60);
ALTER TABLE sales_documents ADD COLUMN incoterm VARCHAR(20);
ALTER TABLE sales_documents ADD COLUMN poids_net DECIMAL(12,3);
ALTER TABLE sales_documents ADD COLUMN poids_brut DECIMAL(12,3);
ALTER TABLE sales_documents ADD CONSTRAINT ck_sales_documents_devise CHECK (devise IN ('TND','EUR','USD'));
ALTER TABLE sales_documents ADD CONSTRAINT ck_sales_documents_regime CHECK (regime IN ('LOCAL','EXPORT','ECHANTILLON'));
ALTER TABLE sales_documents ADD CONSTRAINT ck_sales_documents_poids
    CHECK ((poids_net IS NULL OR poids_net >= 0) AND (poids_brut IS NULL OR poids_brut >= 0)
        AND (poids_net IS NULL OR poids_brut IS NULL OR poids_brut >= poids_net));

ALTER TABLE sales_document_lines ADD COLUMN code_sh VARCHAR(20);

-- =====================================================================
-- V15 : vente de l'huile au kg ou au litre.
--  * company_settings.densite_huile : kg par litre, la même pour toutes les huiles
--    (0,916 donnée par l'huilerie)
--  * articles.prix_vente_litre : prix fixé au litre pour une huile stockée en kg ;
--    le prix au kg (prix_vente) en est déduit : prix au litre / densité
--  * sale_lines.quantite_stock, sales_document_lines.quantite_stock : quantité sortie du stock (kg)
--    quand la ligne est vendue en litres ; NULL = même valeur que quantite
--    (lignes existantes et lignes dans l'unité de l'article)
-- SQL commun à MySQL et H2 (tests).
-- =====================================================================

ALTER TABLE company_settings ADD COLUMN densite_huile DECIMAL(6,4) NOT NULL DEFAULT 0.9160;
ALTER TABLE company_settings ADD CONSTRAINT ck_settings_densite CHECK (densite_huile > 0);

ALTER TABLE articles ADD COLUMN prix_vente_litre DECIMAL(12,3);
ALTER TABLE articles ADD CONSTRAINT ck_articles_prix_litre CHECK (prix_vente_litre IS NULL OR prix_vente_litre >= 0);

ALTER TABLE sale_lines ADD COLUMN quantite_stock DECIMAL(15,3);
ALTER TABLE sales_document_lines ADD COLUMN quantite_stock DECIMAL(15,3);

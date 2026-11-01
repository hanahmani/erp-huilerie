-- =====================================================================
-- V13 : refonte RH — employés simplifiés, pointage Play / Stop, paiements.
--  * suppression des congés, absences, jours fériés, services et postes
--    (tables, permissions LEAVE_REQUEST / LEAVE_VALIDATE)
--  * employees : identité, TJM (taux journalier), actif ; les fiches existantes
--    sont conservées (actif = statut ACTIF), les champs de contrat sont retirés
--  * work_days : une journée de travail par employé et par jour, TJM figé
--  * employee_payments : salaire, journées ou avance, sortie de caisse
-- Reste dû = somme des TJM des journées pointées − somme des paiements.
-- SQL commun à MySQL et H2 (tests).
-- =====================================================================

-- ---------- suppression congés / absences / référentiels
DROP TABLE absences;
DROP TABLE leave_requests;
DROP TABLE public_holidays;

DELETE FROM role_permissions
WHERE permission_id IN (SELECT id FROM permissions WHERE code IN ('LEAVE_REQUEST', 'LEAVE_VALIDATE'));
DELETE FROM permissions WHERE code IN ('LEAVE_REQUEST', 'LEAVE_VALIDATE');

DELETE FROM notification_reads WHERE notif_key LIKE 'CONGES:%' OR notif_key LIKE 'CONTRAT:%';

-- ---------- employés simplifiés
ALTER TABLE employees ADD COLUMN tjm DECIMAL(12,3) NOT NULL DEFAULT 0;
ALTER TABLE employees ADD COLUMN actif BOOLEAN NOT NULL DEFAULT TRUE;
UPDATE employees SET actif = (statut = 'ACTIF');

ALTER TABLE employees DROP CONSTRAINT ck_employees;
ALTER TABLE employees DROP CONSTRAINT fk_employees_service;
ALTER TABLE employees DROP CONSTRAINT fk_employees_poste;
ALTER TABLE employees DROP CONSTRAINT fk_employees_user;
ALTER TABLE employees DROP CONSTRAINT uk_employees_user;

ALTER TABLE employees DROP COLUMN service_id;
ALTER TABLE employees DROP COLUMN poste_id;
ALTER TABLE employees DROP COLUMN user_id;
ALTER TABLE employees DROP COLUMN date_naissance;
ALTER TABLE employees DROP COLUMN email;
ALTER TABLE employees DROP COLUMN date_embauche;
ALTER TABLE employees DROP COLUMN type_contrat;
ALTER TABLE employees DROP COLUMN date_fin_contrat;
ALTER TABLE employees DROP COLUMN statut;
ALTER TABLE employees DROP COLUMN solde_conges;

ALTER TABLE employees ADD CONSTRAINT ck_employees_tjm CHECK (tjm >= 0);

DROP TABLE hr_services;
DROP TABLE hr_positions;

-- ---------- pointage : une journée par employé et par jour
CREATE TABLE work_days (
    id          BIGINT        NOT NULL AUTO_INCREMENT PRIMARY KEY,
    employee_id BIGINT        NOT NULL,
    jour        DATE          NOT NULL,
    heure_debut DATETIME(6)   NOT NULL,
    heure_fin   DATETIME(6),
    tjm         DECIMAL(12,3) NOT NULL,
    saisi_par   VARCHAR(64)   NOT NULL,
    modifie_par VARCHAR(64),
    CONSTRAINT uk_work_days UNIQUE (employee_id, jour),
    CONSTRAINT fk_work_days_employee FOREIGN KEY (employee_id) REFERENCES employees (id),
    CONSTRAINT ck_work_days CHECK (tjm >= 0 AND (heure_fin IS NULL OR heure_fin >= heure_debut))
);
CREATE INDEX idx_work_days_jour ON work_days (jour);

-- ---------- paiements aux employés (sortie de caisse)
CREATE TABLE employee_payments (
    id          BIGINT        NOT NULL AUTO_INCREMENT PRIMARY KEY,
    numero      VARCHAR(30)   NOT NULL,
    employee_id BIGINT        NOT NULL,
    type        VARCHAR(20)   NOT NULL,
    mode        VARCHAR(20)   NOT NULL,
    montant     DECIMAL(12,3) NOT NULL,
    reference   VARCHAR(120),
    date_paie   DATETIME(6)   NOT NULL,
    username    VARCHAR(64)   NOT NULL,
    CONSTRAINT uk_employee_payments_numero UNIQUE (numero),
    CONSTRAINT fk_employee_payments_employee FOREIGN KEY (employee_id) REFERENCES employees (id),
    CONSTRAINT ck_employee_payments CHECK (montant > 0 AND type IN ('SALAIRE', 'TJM', 'AVANCE'))
);
CREATE INDEX idx_employee_payments_date ON employee_payments (date_paie);

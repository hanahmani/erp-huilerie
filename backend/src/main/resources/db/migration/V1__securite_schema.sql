-- =====================================================================
-- V1 : schéma de sécurité (utilisateurs, rôles, permissions, audit)
-- Le moteur InnoDB et l'encodage utf8mb4 sont définis au niveau du serveur.
-- =====================================================================

CREATE TABLE permissions (
    id          BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    code        VARCHAR(64)  NOT NULL,
    description VARCHAR(255) NOT NULL,
    CONSTRAINT uk_permissions_code UNIQUE (code)
);

CREATE TABLE roles (
    id          BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    code        VARCHAR(64)  NOT NULL,
    nom         VARCHAR(100) NOT NULL,
    description VARCHAR(255),
    CONSTRAINT uk_roles_code UNIQUE (code)
);

CREATE TABLE users (
    id                   BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    username             VARCHAR(64)  NOT NULL,
    prenom               VARCHAR(100) NOT NULL,
    nom                  VARCHAR(100) NOT NULL,
    password_hash        VARCHAR(100) NOT NULL,
    actif                BOOLEAN      NOT NULL DEFAULT TRUE,
    must_change_password BOOLEAN      NOT NULL DEFAULT FALSE,
    token_version        INT          NOT NULL DEFAULT 0,
    created_at           DATETIME(6)  NOT NULL,
    updated_at           DATETIME(6)  NOT NULL,
    CONSTRAINT uk_users_username UNIQUE (username)
);

CREATE TABLE user_roles (
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    PRIMARY KEY (user_id, role_id),
    CONSTRAINT fk_user_roles_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_user_roles_role FOREIGN KEY (role_id) REFERENCES roles (id)
);

CREATE TABLE role_permissions (
    role_id       BIGINT NOT NULL,
    permission_id BIGINT NOT NULL,
    PRIMARY KEY (role_id, permission_id),
    CONSTRAINT fk_role_perm_role FOREIGN KEY (role_id) REFERENCES roles (id),
    CONSTRAINT fk_role_perm_perm FOREIGN KEY (permission_id) REFERENCES permissions (id)
);

CREATE TABLE audit_logs (
    id          BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    username    VARCHAR(64),
    action      VARCHAR(64)  NOT NULL,
    entity_type VARCHAR(64),
    entity_id   VARCHAR(64),
    details     VARCHAR(1000),
    created_at  DATETIME(6)  NOT NULL
);

CREATE INDEX idx_audit_logs_created_at ON audit_logs (created_at);
CREATE INDEX idx_audit_logs_username ON audit_logs (username);

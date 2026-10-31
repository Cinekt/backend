-- Baseline del esquema de IAM. Refleja el esquema que generaba Hibernate con
-- ddl-auto=update, antes de adoptar Flyway. Verificado contra el DDL que emite
-- Hibernate 6.6 para estas entidades, de modo que ddl-auto=validate pase.
-- No editar este archivo: los cambios de esquema van en V2__, V3__, etc.

-- roles.name es un ENUM nativo de MySQL porque Hibernate 6 mapea asi
-- @Enumerated(EnumType.STRING) en este dialecto (el @Column(length = 20) de la
-- entidad se ignora). Agregar un rol nuevo implica un ALTER TABLE en una migracion.
CREATE TABLE roles (
    id   BIGINT NOT NULL AUTO_INCREMENT,
    name ENUM ('ROLE_ADMIN', 'ROLE_USER') NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_roles_name UNIQUE (name)
) ENGINE = InnoDB;

CREATE TABLE users (
    id         BIGINT       NOT NULL AUTO_INCREMENT,
    email      VARCHAR(50)  NOT NULL,
    password   VARCHAR(120) NOT NULL,
    created_at DATETIME(6)  NOT NULL,
    updated_at DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_users_email UNIQUE (email)
) ENGINE = InnoDB;

CREATE TABLE user_roles (
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    PRIMARY KEY (user_id, role_id),
    CONSTRAINT fk_user_roles_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_user_roles_role FOREIGN KEY (role_id) REFERENCES roles (id)
) ENGINE = InnoDB;

CREATE TABLE refresh_tokens (
    id         BIGINT      NOT NULL AUTO_INCREMENT,
    token_hash VARCHAR(64) NOT NULL,
    user_id    BIGINT      NOT NULL,
    expires_at DATETIME(6) NOT NULL,
    revoked    BIT         NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_refresh_tokens_token_hash UNIQUE (token_hash),
    CONSTRAINT fk_refresh_tokens_user FOREIGN KEY (user_id) REFERENCES users (id)
) ENGINE = InnoDB;

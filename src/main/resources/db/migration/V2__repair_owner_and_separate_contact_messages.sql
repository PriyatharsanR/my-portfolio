ALTER TABLE admin MODIFY id BIGINT NOT NULL AUTO_INCREMENT;

-- Invalid legacy identities must not acquire admin privileges during migration.
UPDATE admin SET role = 'ROLE_DISABLED' WHERE role IS NULL OR TRIM(role) = '';
ALTER TABLE admin MODIFY role VARCHAR(255) NOT NULL;
ALTER TABLE admin ADD COLUMN version BIGINT NOT NULL DEFAULT 0;

-- Legacy contact.message data remains in its original column for explicit later reconciliation.
-- It is excluded from the public-link entity; missing sender details cannot be inferred safely.
CREATE TABLE contact_messages (
    id BIGINT NOT NULL AUTO_INCREMENT,
    sender_name VARCHAR(120) NOT NULL,
    sender_email VARCHAR(254) NOT NULL,
    body VARCHAR(5000) NOT NULL,
    status VARCHAR(16) NOT NULL,
    submitted_at DATETIME(6) NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id)
) ENGINE=InnoDB;

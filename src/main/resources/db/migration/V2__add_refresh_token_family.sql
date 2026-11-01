DELETE FROM refresh_tokens;

ALTER TABLE refresh_tokens
    ADD COLUMN family_id VARCHAR(36) NOT NULL;

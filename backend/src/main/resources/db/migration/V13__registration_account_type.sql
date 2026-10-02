ALTER TABLE app_user
    ADD COLUMN account_type VARCHAR(20) DEFAULT 'ATHLETE' CHECK (account_type IN ('TRAINER', 'ATHLETE', 'PARENT'));

UPDATE app_user
SET account_type = 'ATHLETE'
WHERE account_type IS NULL;

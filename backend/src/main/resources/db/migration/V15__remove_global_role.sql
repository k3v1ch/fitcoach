UPDATE app_user
SET app_role = 'USER'
WHERE app_role <> 'USER';

ALTER TABLE app_user
    DROP CONSTRAINT IF EXISTS app_user_app_role_check;

ALTER TABLE app_user
    ADD CONSTRAINT app_user_app_role_check CHECK (app_role = 'USER');
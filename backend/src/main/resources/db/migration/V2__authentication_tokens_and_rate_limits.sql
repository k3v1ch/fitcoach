CREATE TABLE auth_token (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES app_user(id),
    purpose VARCHAR(30) NOT NULL CHECK (purpose IN ('REGISTRATION', 'PASSWORD_RESET')),
    token_hash CHAR(64) NOT NULL UNIQUE,
    expires_at TIMESTAMPTZ NOT NULL,
    used_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX auth_token_user_purpose_idx ON auth_token (user_id, purpose, expires_at);

CREATE TABLE auth_rate_limit (
    purpose VARCHAR(30) NOT NULL,
    request_key_hash CHAR(64) NOT NULL,
    requested_at TIMESTAMPTZ NOT NULL,
    PRIMARY KEY (purpose, request_key_hash)
);
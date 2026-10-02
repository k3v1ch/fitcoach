CREATE TABLE app_user (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email VARCHAR(320) NOT NULL,
    normalized_email VARCHAR(320) NOT NULL UNIQUE,
    password_hash VARCHAR(255),
    full_name VARCHAR(200) NOT NULL,
    app_role VARCHAR(20) NOT NULL DEFAULT 'USER' CHECK (app_role = 'USER'),
    status VARCHAR(20) NOT NULL CHECK (status IN ('PENDING_EMAIL', 'ACTIVE', 'BLOCKED')),
    email_verified_at TIMESTAMPTZ,
    last_login_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT app_user_password_state CHECK (
        (status = 'PENDING_EMAIL' AND password_hash IS NULL)
        OR (status IN ('ACTIVE', 'BLOCKED') AND password_hash IS NOT NULL)
    ),
    CONSTRAINT app_user_verified_state CHECK (
        (status = 'PENDING_EMAIL' AND email_verified_at IS NULL)
        OR (status IN ('ACTIVE', 'BLOCKED') AND email_verified_at IS NOT NULL)
    )
);

CREATE TABLE organization (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(200) NOT NULL,
    description TEXT,
    address TEXT,
    timezone VARCHAR(100) NOT NULL DEFAULT 'Europe/Moscow',
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE membership (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES app_user(id),
    organization_id UUID NOT NULL REFERENCES organization(id),
    roles JSONB NOT NULL DEFAULT '[]'::jsonb,
    permissions JSONB NOT NULL DEFAULT '[]'::jsonb,
    status VARCHAR(20) NOT NULL CHECK (status IN ('ACTIVE', 'BLOCKED')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT membership_user_organization_unique UNIQUE (user_id, organization_id),
    CONSTRAINT membership_roles_array CHECK (jsonb_typeof(roles) = 'array'),
    CONSTRAINT membership_permissions_array CHECK (jsonb_typeof(permissions) = 'array')
);

CREATE INDEX membership_organization_status_idx ON membership (organization_id, status);
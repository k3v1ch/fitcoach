CREATE TABLE athlete (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL REFERENCES organization(id),
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    middle_name VARCHAR(100),
    birth_date DATE NOT NULL,
    user_id UUID REFERENCES app_user(id),
    status VARCHAR(20) NOT NULL CHECK (status IN ('ACTIVE', 'ARCHIVED')),
    enrolled_on DATE NOT NULL,
    note TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT athlete_organization_user_unique UNIQUE (organization_id, user_id)
);

CREATE INDEX athlete_organization_status_idx ON athlete (organization_id, status);
CREATE INDEX athlete_organization_name_idx ON athlete (organization_id, last_name, first_name);

CREATE TABLE parent_link (
    organization_id UUID NOT NULL REFERENCES organization(id),
    athlete_id UUID NOT NULL REFERENCES athlete(id) ON DELETE CASCADE,
    parent_user_id UUID NOT NULL REFERENCES app_user(id),
    relationship VARCHAR(100),
    PRIMARY KEY (athlete_id, parent_user_id)
);

CREATE INDEX parent_link_parent_idx ON parent_link (organization_id, parent_user_id);
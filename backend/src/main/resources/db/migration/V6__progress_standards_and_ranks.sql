CREATE TABLE athlete_result (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL REFERENCES organization(id),
    athlete_id UUID NOT NULL REFERENCES athlete(id),
    metric_name VARCHAR(200) NOT NULL,
    value DECIMAL(18, 6) NOT NULL,
    unit VARCHAR(50) NOT NULL,
    measured_on DATE NOT NULL,
    comment TEXT,
    is_personal_best BOOLEAN NOT NULL DEFAULT FALSE,
    created_by UUID NOT NULL REFERENCES app_user(id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX athlete_result_scope_idx ON athlete_result (organization_id, athlete_id, measured_on, id);

CREATE TABLE athlete_standard (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL REFERENCES organization(id),
    athlete_id UUID NOT NULL REFERENCES athlete(id),
    name VARCHAR(200) NOT NULL,
    target_text TEXT NOT NULL,
    result_text TEXT,
    assessed_on DATE NOT NULL,
    status VARCHAR(20) NOT NULL CHECK (status IN ('NOT_ASSESSED', 'MET', 'NOT_MET')),
    comment TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE athlete_rank (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL REFERENCES organization(id),
    athlete_id UUID NOT NULL REFERENCES athlete(id),
    name VARCHAR(200) NOT NULL,
    sport_type_id UUID NOT NULL,
    assigned_on DATE NOT NULL,
    valid_until DATE,
    comment TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
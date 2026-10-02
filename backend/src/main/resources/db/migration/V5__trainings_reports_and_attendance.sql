CREATE TABLE training (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL REFERENCES organization(id),
    title VARCHAR(200) NOT NULL,
    group_id UUID NOT NULL REFERENCES sport_group(id),
    venue_id UUID NOT NULL,
    type_id UUID NOT NULL,
    starts_at TIMESTAMPTZ NOT NULL,
    ends_at TIMESTAMPTZ NOT NULL,
    plan JSONB NOT NULL DEFAULT '[]'::jsonb,
    comment TEXT,
    status VARCHAR(20) NOT NULL CHECK (status IN ('PLANNED', 'COMPLETED', 'CANCELLED')),
    cancel_reason TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT training_time_check CHECK (ends_at > starts_at)
);

CREATE INDEX training_organization_time_idx ON training (organization_id, starts_at, id);

CREATE TABLE training_coach (
    training_id UUID NOT NULL REFERENCES training(id) ON DELETE CASCADE,
    coach_id UUID NOT NULL REFERENCES app_user(id),
    PRIMARY KEY (training_id, coach_id)
);

CREATE TABLE training_report (
    training_id UUID PRIMARY KEY REFERENCES training(id) ON DELETE CASCADE,
    topic VARCHAR(200) NOT NULL,
    actual_content TEXT NOT NULL,
    comment TEXT,
    status VARCHAR(20) NOT NULL CHECK (status IN ('DRAFT', 'CLOSED')),
    author_id UUID NOT NULL REFERENCES app_user(id),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    closed_at TIMESTAMPTZ
);

CREATE TABLE attendance (
    training_id UUID NOT NULL REFERENCES training(id) ON DELETE CASCADE,
    athlete_id UUID NOT NULL REFERENCES athlete(id),
    status VARCHAR(20) NOT NULL CHECK (status IN ('UNMARKED', 'PRESENT', 'SICK', 'ABSENT')),
    reason TEXT,
    comment TEXT,
    marked_by UUID REFERENCES app_user(id),
    marked_at TIMESTAMPTZ,
    PRIMARY KEY (training_id, athlete_id)
);
CREATE TABLE event (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL REFERENCES organization(id),
    title VARCHAR(200) NOT NULL,
    type VARCHAR(30) NOT NULL CHECK (type IN ('CAMP', 'COMPETITION', 'MEDICAL_EXAM', 'FUNDRAISER')),
    section_id UUID NOT NULL REFERENCES section(id),
    description TEXT,
    starts_at TIMESTAMPTZ,
    ends_at TIMESTAMPTZ,
    location TEXT,
    cost_per_athlete DECIMAL(18, 2),
    target_amount DECIMAL(18, 2),
    collection_due_on DATE,
    response_deadline TIMESTAMPTZ,
    required_document_types JSONB NOT NULL DEFAULT '[]'::jsonb,
    status VARCHAR(20) NOT NULL CHECK (status IN ('DRAFT', 'PUBLISHED', 'CANCELLED', 'COMPLETED')),
    created_by UUID NOT NULL REFERENCES app_user(id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT event_time_check CHECK (ends_at IS NULL OR starts_at IS NULL OR ends_at > starts_at)
);

CREATE INDEX event_organization_status_idx ON event (organization_id, status);
CREATE INDEX event_organization_dates_idx ON event (organization_id, starts_at, collection_due_on);

CREATE TABLE event_participant (
    event_id UUID NOT NULL REFERENCES event(id) ON DELETE CASCADE,
    athlete_id UUID NOT NULL REFERENCES athlete(id),
    response VARCHAR(20) NOT NULL DEFAULT 'PENDING' CHECK (response IN ('PENDING', 'ACCEPTED', 'DECLINED')),
    responded_by UUID REFERENCES app_user(id),
    responded_at TIMESTAMPTZ,
    comment TEXT,
    document_ids JSONB NOT NULL DEFAULT '[]'::jsonb,
    PRIMARY KEY (event_id, athlete_id)
);
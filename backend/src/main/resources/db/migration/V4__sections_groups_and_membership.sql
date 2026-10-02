CREATE TABLE section (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL REFERENCES organization(id),
    name VARCHAR(200) NOT NULL,
    sport_type_id UUID NOT NULL,
    description TEXT,
    status VARCHAR(20) NOT NULL CHECK (status IN ('ACTIVE', 'ARCHIVED')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT section_organization_name_unique UNIQUE (organization_id, name)
);

CREATE INDEX section_organization_status_idx ON section (organization_id, status);

CREATE TABLE sport_group (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL REFERENCES organization(id),
    section_id UUID NOT NULL REFERENCES section(id),
    name VARCHAR(200) NOT NULL,
    description TEXT,
    status VARCHAR(20) NOT NULL CHECK (status IN ('ACTIVE', 'ARCHIVED')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT group_organization_name_unique UNIQUE (organization_id, name)
);

CREATE INDEX group_organization_status_idx ON sport_group (organization_id, status);
CREATE INDEX group_section_idx ON sport_group (section_id);

CREATE TABLE group_coach (
    group_id UUID NOT NULL REFERENCES sport_group(id) ON DELETE CASCADE,
    coach_id UUID NOT NULL REFERENCES app_user(id),
    PRIMARY KEY (group_id, coach_id)
);

CREATE TABLE group_athlete (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL REFERENCES organization(id),
    group_id UUID NOT NULL REFERENCES sport_group(id),
    athlete_id UUID NOT NULL REFERENCES athlete(id),
    joined_on DATE NOT NULL,
    left_on DATE,
    CONSTRAINT group_athlete_dates_check CHECK (left_on IS NULL OR left_on >= joined_on)
);

CREATE UNIQUE INDEX group_athlete_open_unique ON group_athlete (group_id, athlete_id) WHERE left_on IS NULL;
CREATE INDEX group_athlete_organization_idx ON group_athlete (organization_id, athlete_id);
CREATE TABLE dictionary_item (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL REFERENCES organization(id),
    dictionary_type VARCHAR(30) NOT NULL CHECK (dictionary_type IN ('sport-types', 'training-types', 'venues')),
    name VARCHAR(200) NOT NULL,
    description TEXT,
    address TEXT,
    sort_order INTEGER NOT NULL DEFAULT 0,
    status VARCHAR(20) NOT NULL CHECK (status IN ('ACTIVE', 'ARCHIVED')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT dictionary_name_unique UNIQUE (organization_id, dictionary_type, name)
);

CREATE INDEX dictionary_scope_idx ON dictionary_item (organization_id, dictionary_type, status, sort_order);
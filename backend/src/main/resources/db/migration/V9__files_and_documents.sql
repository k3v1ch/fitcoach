CREATE TABLE stored_file (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL REFERENCES organization(id),
    original_name VARCHAR(255) NOT NULL,
    content_type VARCHAR(100) NOT NULL,
    size_bytes BIGINT NOT NULL CHECK (size_bytes > 0),
    storage_name VARCHAR(100) NOT NULL UNIQUE,
    uploaded_by UUID NOT NULL REFERENCES app_user(id),
    uploaded_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE document (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL REFERENCES organization(id),
    athlete_id UUID REFERENCES athlete(id),
    type VARCHAR(30) NOT NULL CHECK (type IN ('MEDICAL_CERTIFICATE', 'CONSENT', 'OTHER')),
    title VARCHAR(200) NOT NULL,
    file_id UUID NOT NULL UNIQUE REFERENCES stored_file(id),
    issued_on DATE,
    valid_until DATE,
    created_by UUID NOT NULL REFERENCES app_user(id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT document_owner_check CHECK (athlete_id IS NOT NULL OR type <> 'MEDICAL_CERTIFICATE')
);

CREATE INDEX stored_file_organization_idx ON stored_file (organization_id, uploaded_at);
CREATE INDEX document_scope_idx ON document (organization_id, athlete_id, valid_until);
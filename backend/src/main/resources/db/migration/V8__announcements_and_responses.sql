CREATE TABLE announcement (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL REFERENCES organization(id),
    title VARCHAR(200) NOT NULL,
    text TEXT NOT NULL,
    category VARCHAR(100),
    requires_response BOOLEAN NOT NULL DEFAULT FALSE,
    response_deadline TIMESTAMPTZ,
    attachment_file_ids JSONB NOT NULL DEFAULT '[]'::jsonb,
    status VARCHAR(20) NOT NULL CHECK (status IN ('DRAFT', 'PUBLISHED', 'ARCHIVED')),
    created_by UUID NOT NULL REFERENCES app_user(id),
    published_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE announcement_recipient (
    announcement_id UUID NOT NULL REFERENCES announcement(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES app_user(id),
    read_at TIMESTAMPTZ,
    PRIMARY KEY (announcement_id, user_id)
);

CREATE TABLE announcement_response (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    announcement_id UUID NOT NULL REFERENCES announcement(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES app_user(id),
    response VARCHAR(20) NOT NULL CHECK (response IN ('ACCEPTED', 'DECLINED')),
    comment TEXT,
    responded_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX announcement_recipient_user_idx ON announcement_recipient (user_id, announcement_id);
CREATE INDEX announcement_response_lookup_idx ON announcement_response (announcement_id, user_id, responded_at);
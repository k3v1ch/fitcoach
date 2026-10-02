CREATE TABLE charge (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL REFERENCES organization(id),
    athlete_id UUID NOT NULL REFERENCES athlete(id),
    section_id UUID NOT NULL REFERENCES section(id),
    type VARCHAR(20) NOT NULL CHECK (type IN ('SUBSCRIPTION', 'TRAINING', 'EVENT')),
    title VARCHAR(200) NOT NULL,
    amount DECIMAL(18,2) NOT NULL CHECK (amount > 0),
    due_on DATE NOT NULL,
    period_from DATE,
    period_to DATE,
    training_id UUID REFERENCES training(id),
    event_id UUID REFERENCES event(id),
    comment TEXT,
    status VARCHAR(20) NOT NULL CHECK (status IN ('ACTIVE', 'CANCELLED')),
    cancel_reason TEXT,
    cancelled_by UUID REFERENCES app_user(id),
    cancelled_at TIMESTAMPTZ,
    created_by UUID NOT NULL REFERENCES app_user(id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE payment (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL REFERENCES organization(id),
    charge_id UUID NOT NULL REFERENCES charge(id),
    athlete_id UUID NOT NULL REFERENCES athlete(id),
    section_id UUID NOT NULL REFERENCES section(id),
    amount DECIMAL(18,2) NOT NULL CHECK (amount > 0),
    paid_on DATE NOT NULL,
    method VARCHAR(20) NOT NULL CHECK (method IN ('SBP', 'TRANSFER', 'CASH', 'OTHER')),
    comment TEXT,
    status VARCHAR(20) NOT NULL CHECK (status IN ('ACTIVE', 'VOIDED')),
    idempotency_key VARCHAR(200) NOT NULL,
    created_by UUID NOT NULL REFERENCES app_user(id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    voided_by UUID REFERENCES app_user(id),
    voided_at TIMESTAMPTZ,
    void_reason TEXT,
    CONSTRAINT payment_idempotency_unique UNIQUE (organization_id, idempotency_key)
);

CREATE INDEX charge_scope_idx ON charge (organization_id, athlete_id, due_on, status);
CREATE INDEX payment_scope_idx ON payment (organization_id, charge_id, status);
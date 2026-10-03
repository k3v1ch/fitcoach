-- Уведомления пользователей в веб-приложении: перенос и отмена тренировок, мероприятия, объявления
CREATE TABLE notification (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL REFERENCES organization(id),
    user_id UUID NOT NULL REFERENCES app_user(id),
    type VARCHAR(40) NOT NULL,
    title VARCHAR(255) NOT NULL,
    text TEXT NOT NULL,
    entity_type VARCHAR(40) NOT NULL,
    entity_id UUID NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    read_at TIMESTAMPTZ
);

CREATE INDEX notification_user_idx ON notification (user_id, organization_id, created_at DESC, id DESC);
CREATE INDEX notification_unread_idx ON notification (user_id, organization_id) WHERE read_at IS NULL;
CREATE INDEX notification_entity_idx ON notification (entity_id, type, user_id);

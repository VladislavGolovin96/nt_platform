CREATE TABLE webhooks (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    url VARCHAR(500) NOT NULL,
    events JSONB NOT NULL DEFAULT '[]',
    active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMPTZ DEFAULT now()
);

CREATE TABLE notification_history (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID,
    event VARCHAR(100) NOT NULL,
    channel VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL,
    payload JSONB,
    sent_at TIMESTAMPTZ DEFAULT now()
);

CREATE INDEX idx_webhooks_user_id ON webhooks (user_id);
CREATE INDEX idx_webhooks_active ON webhooks (active) WHERE active = true;
CREATE INDEX idx_notification_history_user_id ON notification_history (user_id);
CREATE INDEX idx_notification_history_sent_at ON notification_history (sent_at DESC);

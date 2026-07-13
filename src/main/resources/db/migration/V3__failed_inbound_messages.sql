CREATE TABLE IF NOT EXISTS failed_inbound_messages (
    id BIGINT PRIMARY KEY DEFAULT nextval('primary_sequence'),
    queue_name VARCHAR(120) NOT NULL,
    exchange_name VARCHAR(120) NOT NULL,
    routing_key VARCHAR(120) NOT NULL,
    reference VARCHAR(160),
    correlation_id VARCHAR(160),
    message_id VARCHAR(160),
    failure_class VARCHAR(120),
    failure_reason VARCHAR(1000),
    payload JSONB,
    status VARCHAR(40) NOT NULL,
    date_created TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    replayed_at TIMESTAMP WITH TIME ZONE
);

CREATE INDEX IF NOT EXISTS idx_failed_inbound_messages_status ON failed_inbound_messages (status);
CREATE INDEX IF NOT EXISTS idx_failed_inbound_messages_reference ON failed_inbound_messages (reference);
CREATE INDEX IF NOT EXISTS idx_failed_inbound_messages_correlation_id ON failed_inbound_messages (correlation_id);

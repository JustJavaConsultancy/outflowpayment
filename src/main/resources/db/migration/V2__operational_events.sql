CREATE TABLE IF NOT EXISTS operational_events (
    id BIGINT PRIMARY KEY DEFAULT nextval('primary_sequence'),
    service_name VARCHAR(80) NOT NULL,
    event_type VARCHAR(120) NOT NULL,
    severity VARCHAR(20) NOT NULL,
    reference_type VARCHAR(80),
    reference VARCHAR(160),
    correlation_id VARCHAR(160),
    message_id VARCHAR(160),
    merchant_id VARCHAR(160),
    process_instance_id VARCHAR(160),
    failure_class VARCHAR(120),
    summary VARCHAR(500),
    details JSONB,
    date_created TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_operational_events_reference ON operational_events (reference);
CREATE INDEX IF NOT EXISTS idx_operational_events_correlation_id ON operational_events (correlation_id);
CREATE INDEX IF NOT EXISTS idx_operational_events_event_type ON operational_events (event_type);
CREATE INDEX IF NOT EXISTS idx_operational_events_date_created ON operational_events (date_created);

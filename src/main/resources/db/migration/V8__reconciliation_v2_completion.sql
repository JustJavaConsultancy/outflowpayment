CREATE TABLE IF NOT EXISTS reconciliation_statement_rejected_items (
    id BIGINT PRIMARY KEY DEFAULT nextval('primary_sequence'),
    import_batch_id BIGINT NOT NULL,
    row_number INTEGER NOT NULL,
    raw_line VARCHAR(4000),
    reason VARCHAR(1000) NOT NULL,
    raw_payload JSONB,
    date_created TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_reconciliation_rejected_import FOREIGN KEY (import_batch_id) REFERENCES reconciliation_statement_imports (id)
);

CREATE INDEX IF NOT EXISTS idx_reconciliation_rejected_items_import
    ON reconciliation_statement_rejected_items (import_batch_id, row_number);

ALTER TABLE reconciliation_statement_items
    ADD COLUMN IF NOT EXISTS match_strategy VARCHAR(80),
    ADD COLUMN IF NOT EXISTS matched_reference_type VARCHAR(80),
    ADD COLUMN IF NOT EXISTS matched_at TIMESTAMP WITH TIME ZONE;

ALTER TABLE reconciliation_item
    ADD COLUMN IF NOT EXISTS assigned_to VARCHAR(255),
    ADD COLUMN IF NOT EXISTS assigned_at TIMESTAMP WITH TIME ZONE,
    ADD COLUMN IF NOT EXISTS resolution_note VARCHAR(1000),
    ADD COLUMN IF NOT EXISTS resolved_by VARCHAR(255),
    ADD COLUMN IF NOT EXISTS resolved_at TIMESTAMP WITH TIME ZONE,
    ADD COLUMN IF NOT EXISTS reopened_by VARCHAR(255),
    ADD COLUMN IF NOT EXISTS reopened_at TIMESTAMP WITH TIME ZONE,
    ADD COLUMN IF NOT EXISTS last_action_by VARCHAR(255),
    ADD COLUMN IF NOT EXISTS last_action_at TIMESTAMP WITH TIME ZONE;

CREATE TABLE IF NOT EXISTS reconciliation_audit_event (
    id BIGINT PRIMARY KEY DEFAULT nextval('primary_sequence'),
    reconciliation_item_id BIGINT,
    run_id BIGINT,
    reference VARCHAR(255),
    event_type VARCHAR(80) NOT NULL,
    actor VARCHAR(120) NOT NULL DEFAULT 'system',
    note VARCHAR(1000),
    previous_status VARCHAR(80),
    new_status VARCHAR(80),
    details JSONB,
    date_created TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_reconciliation_audit_item
    ON reconciliation_audit_event (reconciliation_item_id, date_created);

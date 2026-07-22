CREATE TABLE IF NOT EXISTS reconciliation_statement_imports (
    id BIGINT PRIMARY KEY DEFAULT nextval('primary_sequence'),
    source_type VARCHAR(40) NOT NULL,
    source_name VARCHAR(120) NOT NULL,
    profile_name VARCHAR(80) NOT NULL,
    original_filename VARCHAR(255),
    status VARCHAR(40) NOT NULL,
    total_rows INTEGER NOT NULL DEFAULT 0,
    accepted_rows INTEGER NOT NULL DEFAULT 0,
    rejected_rows INTEGER NOT NULL DEFAULT 0,
    failure_reason VARCHAR(1000),
    date_created TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_updated TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS reconciliation_statement_items (
    id BIGINT PRIMARY KEY DEFAULT nextval('primary_sequence'),
    import_batch_id BIGINT NOT NULL,
    row_number INTEGER NOT NULL,
    transaction_reference VARCHAR(160),
    external_reference VARCHAR(160),
    amount NUMERIC(14, 2) NOT NULL,
    currency VARCHAR(8),
    direction VARCHAR(20) NOT NULL,
    transaction_date TIMESTAMP WITH TIME ZONE,
    value_date TIMESTAMP WITH TIME ZONE,
    provider_status VARCHAR(80),
    narration VARCHAR(1000),
    raw_payload JSONB,
    match_status VARCHAR(40) NOT NULL DEFAULT 'UNMATCHED',
    matched_reference VARCHAR(160),
    match_confidence VARCHAR(20) NOT NULL DEFAULT 'NONE',
    mismatch_reason VARCHAR(1000),
    CONSTRAINT fk_reconciliation_statement_items_import FOREIGN KEY (import_batch_id) REFERENCES reconciliation_statement_imports (id)
);

CREATE INDEX IF NOT EXISTS idx_reconciliation_statement_imports_status ON reconciliation_statement_imports (status);
CREATE INDEX IF NOT EXISTS idx_reconciliation_statement_items_import ON reconciliation_statement_items (import_batch_id);
CREATE INDEX IF NOT EXISTS idx_reconciliation_statement_items_transaction_reference ON reconciliation_statement_items (transaction_reference);
CREATE INDEX IF NOT EXISTS idx_reconciliation_statement_items_external_reference ON reconciliation_statement_items (external_reference);
CREATE INDEX IF NOT EXISTS idx_reconciliation_statement_items_match_status ON reconciliation_statement_items (match_status);

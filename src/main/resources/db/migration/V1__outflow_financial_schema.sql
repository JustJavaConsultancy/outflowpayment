CREATE SEQUENCE IF NOT EXISTS primary_sequence START WITH 10000 INCREMENT BY 1;

CREATE TABLE IF NOT EXISTS transfer_instructions (
    id BIGINT PRIMARY KEY DEFAULT nextval('primary_sequence'),
    reference VARCHAR(255) NOT NULL UNIQUE,
    correlation_id VARCHAR(255) UNIQUE,
    merchant_id VARCHAR(255) NOT NULL,
    amount NUMERIC(12, 2) NOT NULL,
    beneficiary_account VARCHAR(255) NOT NULL,
    status VARCHAR(40) NOT NULL,
    process_instance_id VARCHAR(120),
    raw_message JSONB,
    date_created TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    last_updated TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS accounting_posting_records (
    id BIGINT PRIMARY KEY DEFAULT nextval('primary_sequence'),
    operation VARCHAR(80) NOT NULL,
    posting_key VARCHAR(160) NOT NULL,
    transfer_reference VARCHAR(120),
    transaction_reference VARCHAR(120),
    process_instance_id VARCHAR(120),
    activity_id VARCHAR(120),
    amount NUMERIC(12, 2) NOT NULL,
    status VARCHAR(40) NOT NULL,
    metadata JSONB,
    response_payload JSONB,
    date_created TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    last_updated TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_accounting_posting_operation_key UNIQUE (operation, posting_key)
);

CREATE TABLE IF NOT EXISTS reconciliation_run (
    id BIGINT PRIMARY KEY DEFAULT nextval('primary_sequence'),
    run_date DATE,
    started_at TIMESTAMP WITH TIME ZONE,
    completed_at TIMESTAMP WITH TIME ZONE,
    status VARCHAR(40) NOT NULL,
    summary JSONB
);

CREATE TABLE IF NOT EXISTS reconciliation_item (
    id BIGINT PRIMARY KEY DEFAULT nextval('primary_sequence'),
    run_id BIGINT NOT NULL,
    reference VARCHAR(255) NOT NULL,
    reference_type VARCHAR(255) NOT NULL,
    expected_amount NUMERIC(12, 2),
    actual_amount NUMERIC(12, 2),
    expected_status VARCHAR(255),
    actual_status VARCHAR(255),
    issue_type VARCHAR(80) NOT NULL,
    resolution_status VARCHAR(80) NOT NULL,
    details VARCHAR(1000),
    CONSTRAINT fk_reconciliation_item_run FOREIGN KEY (run_id) REFERENCES reconciliation_run (id)
);

CREATE INDEX IF NOT EXISTS idx_transfer_instructions_reference ON transfer_instructions (reference);
CREATE INDEX IF NOT EXISTS idx_transfer_instructions_correlation_id ON transfer_instructions (correlation_id);
CREATE INDEX IF NOT EXISTS idx_transfer_instructions_process_instance_id ON transfer_instructions (process_instance_id);
CREATE INDEX IF NOT EXISTS idx_transfer_instructions_status ON transfer_instructions (status);
CREATE INDEX IF NOT EXISTS idx_accounting_posting_operation_key ON accounting_posting_records (operation, posting_key);
CREATE INDEX IF NOT EXISTS idx_accounting_posting_transfer_reference ON accounting_posting_records (transfer_reference);
CREATE INDEX IF NOT EXISTS idx_reconciliation_item_run_id ON reconciliation_item (run_id);
CREATE INDEX IF NOT EXISTS idx_reconciliation_item_reference ON reconciliation_item (reference);

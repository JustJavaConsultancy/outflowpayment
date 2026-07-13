ALTER TABLE transfer_instructions
    ADD COLUMN IF NOT EXISTS settlement_reference VARCHAR(255),
    ADD COLUMN IF NOT EXISTS settlement_batch_id VARCHAR(255),
    ADD COLUMN IF NOT EXISTS settlement_date VARCHAR(40),
    ADD COLUMN IF NOT EXISTS settlement_item_count INTEGER;

CREATE UNIQUE INDEX IF NOT EXISTS idx_transfer_instructions_settlement_reference
    ON transfer_instructions (settlement_reference)
    WHERE settlement_reference IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_transfer_instructions_settlement_batch_id
    ON transfer_instructions (settlement_batch_id);

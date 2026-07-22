ALTER TABLE transfer_instructions
    ADD COLUMN IF NOT EXISTS instruction_type VARCHAR(40) NOT NULL DEFAULT 'TRANSFER',
    ADD COLUMN IF NOT EXISTS refund_reference VARCHAR(255),
    ADD COLUMN IF NOT EXISTS original_transaction_id VARCHAR(255),
    ADD COLUMN IF NOT EXISTS refund_status VARCHAR(40);

CREATE UNIQUE INDEX IF NOT EXISTS idx_transfer_instructions_refund_reference
    ON transfer_instructions (refund_reference)
    WHERE refund_reference IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_transfer_instructions_instruction_type
    ON transfer_instructions (instruction_type);

CREATE INDEX IF NOT EXISTS idx_transfer_instructions_refund_status
    ON transfer_instructions (refund_status);

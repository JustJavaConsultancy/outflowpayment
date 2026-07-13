ALTER TABLE transfer_instructions
    ADD COLUMN IF NOT EXISTS payout_group_reference VARCHAR(255);

CREATE UNIQUE INDEX IF NOT EXISTS uk_transfer_instructions_payout_group_reference
    ON transfer_instructions (payout_group_reference)
    WHERE payout_group_reference IS NOT NULL;

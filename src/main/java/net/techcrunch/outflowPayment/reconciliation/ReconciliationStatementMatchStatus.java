package net.techcrunch.outflowPayment.reconciliation;

public enum ReconciliationStatementMatchStatus {
    UNMATCHED,
    MATCHED,
    MISSING_INTERNAL,
    AMOUNT_MISMATCH,
    STATUS_MISMATCH,
    MANUAL_REVIEW
}

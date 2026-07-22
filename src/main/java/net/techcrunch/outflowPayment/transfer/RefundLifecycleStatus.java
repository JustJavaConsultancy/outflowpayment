package net.techcrunch.outflowPayment.transfer;

public enum RefundLifecycleStatus {
    REQUESTED,
    APPROVED,
    PROCESSING,
    SUCCESSFUL,
    FAILED,
    REVERSAL_PENDING,
    REVERSED
}

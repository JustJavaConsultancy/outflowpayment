package net.techcrunch.outflowPayment.transfer;

public enum TransferInstructionStatus {
    RECEIVED,
    PROCESSING,
    DEBITED,
    TRANSFER_PROCESSING,
    COMPLETED,
    FAILED,
    REVERSAL_PENDING,
    REVERSED
}

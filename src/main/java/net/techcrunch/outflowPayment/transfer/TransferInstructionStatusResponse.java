package net.techcrunch.outflowPayment.transfer;

import java.math.BigDecimal;

public record TransferInstructionStatusResponse(
        String reference,
        String correlationId,
        String merchantId,
        BigDecimal amount,
        String beneficiaryAccount,
        TransferInstructionType instructionType,
        String refundReference,
        String originalTransactionId,
        RefundLifecycleStatus refundStatus,
        String settlementReference,
        String payoutGroupReference,
        String settlementBatchId,
        String settlementDate,
        Integer settlementItemCount,
        TransferInstructionStatus status,
        String processInstanceId
) {

    public static TransferInstructionStatusResponse from(TransferInstruction instruction) {
        return new TransferInstructionStatusResponse(
                instruction.getReference(),
                instruction.getCorrelationId(),
                instruction.getMerchantId(),
                instruction.getAmount(),
                instruction.getBeneficiaryAccount(),
                instruction.getInstructionType(),
                instruction.getRefundReference(),
                instruction.getOriginalTransactionId(),
                instruction.getRefundStatus(),
                instruction.getSettlementReference(),
                instruction.getPayoutGroupReference(),
                instruction.getSettlementBatchId(),
                instruction.getSettlementDate(),
                instruction.getSettlementItemCount(),
                instruction.getStatus(),
                instruction.getProcessInstanceId()
        );
    }
}

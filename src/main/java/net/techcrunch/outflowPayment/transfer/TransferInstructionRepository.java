package net.techcrunch.outflowPayment.transfer;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TransferInstructionRepository extends JpaRepository<TransferInstruction, Long> {

    Optional<TransferInstruction> findByReference(String reference);

    Optional<TransferInstruction> findByCorrelationId(String correlationId);

    Optional<TransferInstruction> findByRefundReference(String refundReference);

    Optional<TransferInstruction> findBySettlementReference(String settlementReference);

    Optional<TransferInstruction> findByPayoutGroupReference(String payoutGroupReference);

    Optional<TransferInstruction> findByProcessInstanceId(String processInstanceId);
}

package net.techcrunch.outflowPayment.transfer;

import net.techcrunch.outflowPayment.observability.OperationalEventService;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TransferInstructionServiceTest {

    private final TransferInstructionRepository transferInstructionRepository =
            mock(TransferInstructionRepository.class);
    private final OperationalEventService operationalEventService = mock(OperationalEventService.class);
    private final SettlementResultPublisher settlementResultPublisher = mock(SettlementResultPublisher.class);
    private final TransferInstructionService transferInstructionService =
            new TransferInstructionService(
                    transferInstructionRepository,
                    operationalEventService,
                    settlementResultPublisher
            );

    @Test
    void prepareInstructionPersistsNewTransferInstruction() {
        Map<String, Object> message = transferMessage("trf_123", "corr_123");
        when(transferInstructionRepository.findByReference("trf_123")).thenReturn(Optional.empty());
        when(transferInstructionRepository.findByCorrelationId("corr_123")).thenReturn(Optional.empty());
        when(transferInstructionRepository.save(any(TransferInstruction.class)))
                .thenAnswer(invocation -> {
                    TransferInstruction instruction = invocation.getArgument(0);
                    instruction.setId(1000L);
                    return instruction;
                });

        TransferInstructionResult result = transferInstructionService.prepareInstruction(message);

        assertThat(result.duplicate()).isFalse();
        assertThat(result.shouldStartProcess()).isTrue();
        assertThat(result.instruction().getReference()).isEqualTo("trf_123");
        assertThat(result.instruction().getCorrelationId()).isEqualTo("corr_123");
        assertThat(result.instruction().getMerchantId()).isEqualTo("merchant-1");
        assertThat(result.instruction().getAmount()).isEqualByComparingTo(new BigDecimal("2500.00"));
        assertThat(result.instruction().getStatus()).isEqualTo(TransferInstructionStatus.RECEIVED);
        assertThat(result.variables()).containsEntry("transferReference", "trf_123");
        verify(transferInstructionRepository).save(any(TransferInstruction.class));
    }

    @Test
    void prepareInstructionPersistsRefundLifecycleMetadata() {
        Map<String, Object> message = refundMessage("ref_123", "corr_ref_123");
        when(transferInstructionRepository.findByReference("ref_123")).thenReturn(Optional.empty());
        when(transferInstructionRepository.findByRefundReference("ref_123")).thenReturn(Optional.empty());
        when(transferInstructionRepository.findByCorrelationId("corr_ref_123")).thenReturn(Optional.empty());
        when(transferInstructionRepository.save(any(TransferInstruction.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        TransferInstructionResult result = transferInstructionService.prepareInstruction(message);

        assertThat(result.instruction().getInstructionType()).isEqualTo(TransferInstructionType.REFUND);
        assertThat(result.instruction().getRefundReference()).isEqualTo("ref_123");
        assertThat(result.instruction().getOriginalTransactionId()).isEqualTo("txn_123");
        assertThat(result.instruction().getRefundStatus()).isEqualTo(RefundLifecycleStatus.REQUESTED);
        assertThat(result.variables()).containsEntry("instructionType", "REFUND");
    }

    @Test
    void prepareInstructionPersistsSettlementBatchMetadata() {
        Map<String, Object> message = settlementMessage("set_20260711_abc", "corr_set_123");
        when(transferInstructionRepository.findByReference("set_20260711_abc")).thenReturn(Optional.empty());
        when(transferInstructionRepository.findBySettlementReference("set_20260711_abc")).thenReturn(Optional.empty());
        when(transferInstructionRepository.findByCorrelationId("corr_set_123")).thenReturn(Optional.empty());
        when(transferInstructionRepository.save(any(TransferInstruction.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        TransferInstructionResult result = transferInstructionService.prepareInstruction(message);

        assertThat(result.instruction().getInstructionType()).isEqualTo(TransferInstructionType.SETTLEMENT);
        assertThat(result.instruction().getReference()).isEqualTo("set_20260711_abc");
        assertThat(result.instruction().getSettlementReference()).isEqualTo("set_20260711_abc");
        assertThat(result.instruction().getSettlementBatchId()).isEqualTo("1000");
        assertThat(result.instruction().getSettlementDate()).isEqualTo("2026-07-11");
        assertThat(result.instruction().getSettlementItemCount()).isEqualTo(1);
        assertThat(result.instruction().getMerchantId()).isEqualTo("SETTLEMENT_BATCH");
        assertThat(result.instruction().getBeneficiaryAccount()).isEqualTo("set_20260711_abc");
        assertThat(result.instruction().getAmount()).isEqualByComparingTo("1180.00");
        assertThat(result.variables()).containsEntry("instructionType", "SETTLEMENT");
    }

    @Test
    void prepareInstructionSkipsDuplicateSettlementBySettlementReference() {
        Map<String, Object> message = settlementMessage("set_20260711_abc", "corr_set_123");
        TransferInstruction existingInstruction = new TransferInstruction();
        existingInstruction.setReference("set_20260711_abc");
        existingInstruction.setSettlementReference("set_20260711_abc");
        existingInstruction.setInstructionType(TransferInstructionType.SETTLEMENT);
        existingInstruction.setProcessInstanceId("process-123");

        when(transferInstructionRepository.findByReference("set_20260711_abc")).thenReturn(Optional.empty());
        when(transferInstructionRepository.findBySettlementReference("set_20260711_abc"))
                .thenReturn(Optional.of(existingInstruction));

        TransferInstructionResult result = transferInstructionService.prepareInstruction(message);

        assertThat(result.duplicate()).isTrue();
        assertThat(result.shouldStartProcess()).isFalse();
        assertThat(result.instruction()).isSameAs(existingInstruction);
    }

    @Test
    void prepareInstructionSkipsDuplicateWhenProcessAlreadyStarted() {
        Map<String, Object> message = transferMessage("trf_123", "corr_123");
        TransferInstruction existingInstruction = new TransferInstruction();
        existingInstruction.setReference("trf_123");
        existingInstruction.setProcessInstanceId("process-123");

        when(transferInstructionRepository.findByReference("trf_123"))
                .thenReturn(Optional.of(existingInstruction));

        TransferInstructionResult result = transferInstructionService.prepareInstruction(message);

        assertThat(result.duplicate()).isTrue();
        assertThat(result.shouldStartProcess()).isFalse();
        assertThat(result.instruction()).isSameAs(existingInstruction);
    }

    @Test
    void prepareInstructionRejectsUnsupportedEventVersion() {
        Map<String, Object> message = transferMessage("trf_123", "corr_123");
        message.put("eventVersion", "2");

        assertThatThrownBy(() -> transferInstructionService.prepareInstruction(message))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unsupported outflow eventVersion");
    }

    @Test
    void prepareInstructionRejectsMissingMessageMetadata() {
        Map<String, Object> message = transferMessage("trf_123", "corr_123");
        message.remove("messageId");

        assertThatThrownBy(() -> transferInstructionService.prepareInstruction(message))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("messageId");
    }

    @Test
    void markCompletedIfPresentUpdatesInstructionStatus() {
        TransferInstruction existingInstruction = new TransferInstruction();
        existingInstruction.setReference("trf_123");
        existingInstruction.setStatus(TransferInstructionStatus.PROCESSING);

        when(transferInstructionRepository.findByReference("trf_123"))
                .thenReturn(Optional.of(existingInstruction));
        when(transferInstructionRepository.save(any(TransferInstruction.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Optional<TransferInstruction> result = transferInstructionService.markCompletedIfPresent(
                Map.of("transferReference", "trf_123"),
                "process-123",
                Map.of("providerReference", "devout_trf_123")
        );

        assertThat(result).isPresent();
        assertThat(result.get().getStatus()).isEqualTo(TransferInstructionStatus.COMPLETED);
        Map<?, ?> providerEvent = (Map<?, ?>) result.get().getRawMessage().get("latestProviderEvent");
        assertThat(providerEvent.get("providerReference")).isEqualTo("devout_trf_123");
    }

    @Test
    void markCompletedIfPresentUpdatesRefundLifecycleStatus() {
        TransferInstruction existingInstruction = new TransferInstruction();
        existingInstruction.setReference("ref_123");
        existingInstruction.setInstructionType(TransferInstructionType.REFUND);
        existingInstruction.setRefundReference("ref_123");
        existingInstruction.setRefundStatus(RefundLifecycleStatus.PROCESSING);
        existingInstruction.setStatus(TransferInstructionStatus.TRANSFER_PROCESSING);

        when(transferInstructionRepository.findByReference("ref_123"))
                .thenReturn(Optional.of(existingInstruction));
        when(transferInstructionRepository.save(any(TransferInstruction.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Optional<TransferInstruction> result = transferInstructionService.markCompletedIfPresent(
                Map.of("transferReference", "ref_123"),
                "process-123",
                Map.of("providerReference", "devout_ref_123")
        );

        assertThat(result).isPresent();
        assertThat(result.get().getStatus()).isEqualTo(TransferInstructionStatus.COMPLETED);
        assertThat(result.get().getRefundStatus()).isEqualTo(RefundLifecycleStatus.SUCCESSFUL);
    }

    @Test
    void markCompletedIfPresentPublishesSettlementResultForSettlementInstruction() {
        TransferInstruction existingInstruction = settlementInstruction();
        existingInstruction.setStatus(TransferInstructionStatus.TRANSFER_PROCESSING);

        when(transferInstructionRepository.findByReference("set_20260711_abc"))
                .thenReturn(Optional.of(existingInstruction));
        when(transferInstructionRepository.save(any(TransferInstruction.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Optional<TransferInstruction> result = transferInstructionService.markCompletedIfPresent(
                Map.of("transferReference", "set_20260711_abc"),
                "process-set-123",
                Map.of("providerReference", "devout_set_20260711_abc")
        );

        assertThat(result).isPresent();
        assertThat(result.get().getStatus()).isEqualTo(TransferInstructionStatus.COMPLETED);
        verify(settlementResultPublisher).publish(
                result.get(),
                TransferInstructionStatus.COMPLETED,
                Map.of("providerReference", "devout_set_20260711_abc")
        );
    }

    @Test
    void markDebitedAndReversedUpdateInstructionStatus() {
        TransferInstruction existingInstruction = new TransferInstruction();
        existingInstruction.setReference("trf_123");
        existingInstruction.setStatus(TransferInstructionStatus.PROCESSING);

        when(transferInstructionRepository.findByReference("trf_123"))
                .thenReturn(Optional.of(existingInstruction));
        when(transferInstructionRepository.save(any(TransferInstruction.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Optional<TransferInstruction> debited = transferInstructionService.markDebitedIfPresent(
                Map.of("transferReference", "trf_123"),
                "process-123",
                Map.of("debitTransaction", "tx-1")
        );
        Optional<TransferInstruction> reversed = transferInstructionService.markReversedIfPresent(
                Map.of("transferReference", "trf_123"),
                "process-123",
                Map.of("reversalTransaction", "tx-2")
        );

        assertThat(debited).isPresent();
        assertThat(reversed).isPresent();
        assertThat(reversed.get().getStatus()).isEqualTo(TransferInstructionStatus.REVERSED);
        Map<?, ?> providerEvent = (Map<?, ?>) reversed.get().getRawMessage().get("latestProviderEvent");
        assertThat(providerEvent.get("reversalTransaction")).isEqualTo("tx-2");
    }

    private Map<String, Object> transferMessage(String transferReference, String correlationId) {
        Map<String, Object> transferDTO = new HashMap<>();
        transferDTO.put("merchantId", "merchant-1");
        transferDTO.put("amountToSend", "2500.00");
        transferDTO.put("accNumber", "0123456789");
        transferDTO.put("duration", "");

        Map<String, Object> message = new HashMap<>();
        message.put("eventType", "OUTFLOW_TRANSFER_REQUESTED");
        message.put("eventVersion", "1");
        message.put("sourceService", "bluepay");
        message.put("messageId", "msg-123");
        message.put("transferReference", transferReference);
        message.put("correlationId", correlationId);
        message.put("createdAt", OffsetDateTime.now().toString());
        message.put("TransferDTO", transferDTO);
        return message;
    }

    private Map<String, Object> refundMessage(String refundReference, String correlationId) {
        Map<String, Object> transferDTO = new HashMap<>();
        transferDTO.put("merchantId", "merchant-1");
        transferDTO.put("amountToSend", "1200.00");
        transferDTO.put("accNumber", "0123456789");
        transferDTO.put("refundReference", refundReference);
        transferDTO.put("transactionId", "txn_123");
        transferDTO.put("duration", "");

        Map<String, Object> message = new HashMap<>();
        message.put("eventType", "OUTFLOW_REFUND_REQUESTED");
        message.put("eventVersion", "1");
        message.put("sourceService", "bluepay");
        message.put("messageId", "msg-ref-123");
        message.put("transferReference", refundReference);
        message.put("correlationId", correlationId);
        message.put("createdAt", OffsetDateTime.now().toString());
        message.put("TransferDTO", transferDTO);
        return message;
    }

    private Map<String, Object> settlementMessage(String settlementReference, String correlationId) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("batchId", 1000L);
        payload.put("batchReference", settlementReference);
        payload.put("settlementDate", "2026-07-11");
        payload.put("grossAmount", "1200.00");
        payload.put("feeAmount", "20.00");
        payload.put("netAmount", "1180.00");
        payload.put("merchantCount", 1);
        payload.put("itemCount", 1);

        Map<String, Object> message = new HashMap<>();
        message.put("eventType", "OUTFLOW_SETTLEMENT_REQUESTED");
        message.put("eventVersion", "1");
        message.put("sourceService", "bluepay");
        message.put("messageId", "msg-set-123");
        message.put("transferReference", settlementReference);
        message.put("correlationId", correlationId);
        message.put("createdAt", OffsetDateTime.now().toString());
        message.put("payload", payload);
        return message;
    }

    private TransferInstruction settlementInstruction() {
        TransferInstruction instruction = new TransferInstruction();
        instruction.setReference("set_20260711_abc");
        instruction.setCorrelationId("corr-set-123");
        instruction.setInstructionType(TransferInstructionType.SETTLEMENT);
        instruction.setSettlementReference("set_20260711_abc");
        instruction.setSettlementBatchId("1000");
        instruction.setSettlementDate("2026-07-11");
        instruction.setSettlementItemCount(1);
        instruction.setMerchantId("SETTLEMENT_BATCH");
        instruction.setBeneficiaryAccount("set_20260711_abc");
        instruction.setAmount(new BigDecimal("1180.00"));
        return instruction;
    }
}

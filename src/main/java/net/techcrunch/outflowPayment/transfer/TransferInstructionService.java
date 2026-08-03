package net.techcrunch.outflowPayment.transfer;

import net.techcrunch.outflowPayment.observability.OperationalEventService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
public class TransferInstructionService {

    private final TransferInstructionRepository transferInstructionRepository;
    private final OperationalEventService operationalEventService;
    private final SettlementResultPublisher settlementResultPublisher;
    private final OutflowResultPublisher outflowResultPublisher;

    public TransferInstructionService(TransferInstructionRepository transferInstructionRepository,
                                      OperationalEventService operationalEventService,
                                      SettlementResultPublisher settlementResultPublisher,
                                      OutflowResultPublisher outflowResultPublisher) {
        this.transferInstructionRepository = transferInstructionRepository;
        this.operationalEventService = operationalEventService;
        this.settlementResultPublisher = settlementResultPublisher;
        this.outflowResultPublisher = outflowResultPublisher;
    }

    public TransferInstructionResult prepareInstruction(Map<String, Object> message) {
        OutflowTransferMessage transferMessage;
        try {
            transferMessage = OutflowTransferMessage.from(message);
        } catch (IllegalArgumentException exception) {
            operationalEventService.error(
                    "OUTFLOW_MESSAGE_REJECTED",
                    "TRANSFER",
                    stringValue(message.get("transferReference")),
                    "Outflow transfer message failed contract validation",
                    withFailure(message, "INVALID_MESSAGE_CONTRACT", exception.getMessage())
            );
            throw exception;
        }
        Map<String, Object> transferVariables = transferMessage.toProcessVariables();
        applyRecurrenceDefaults(transferVariables);
        transferVariables.putIfAbsent("isRecurrent", false);

        String reference = resolveReference(transferMessage.toRawMessage(), transferVariables);
        String correlationId = resolveCorrelationId(transferMessage.toRawMessage(), transferVariables);
        transferVariables.put("transferReference", reference);
        if (correlationId != null) {
            transferVariables.put("correlationId", correlationId);
        }

        TransferInstruction existingInstruction = findExistingInstruction(
                reference,
                correlationId,
                firstNullableNonBlank(transferVariables.get("refundReference")),
                firstNullableNonBlank(transferVariables.get("settlementReference"), transferVariables.get("batchReference")),
                firstNullableNonBlank(transferVariables.get("payoutGroupReference"))
        );
        if (existingInstruction != null) {
            operationalEventService.warning(
                    duplicateEventType(existingInstruction),
                    referenceType(existingInstruction),
                    existingInstruction.getReference(),
                    "Duplicate outflow instruction received",
                    eventDetails(existingInstruction, transferVariables)
            );
            return new TransferInstructionResult(
                    existingInstruction,
                    transferVariables,
                    true,
                    existingInstruction.getProcessInstanceId() == null
            );
        }

        TransferInstruction instruction = new TransferInstruction();
        instruction.setReference(reference);
        instruction.setCorrelationId(correlationId);
        instruction.setMerchantId(requiredString(transferVariables, "merchantId"));
        instruction.setAmount(new BigDecimal(String.valueOf(transferVariables.get("amountToSend"))));
        instruction.setBeneficiaryAccount(requiredString(transferVariables, "accNumber"));
        instruction.setInstructionType(resolveInstructionType(transferVariables));
        if (instructionType(instruction) == TransferInstructionType.REFUND) {
            instruction.setRefundReference(requiredString(transferVariables, "refundReference"));
            instruction.setOriginalTransactionId(requiredString(transferVariables, "transactionId"));
            instruction.setRefundStatus(RefundLifecycleStatus.REQUESTED);
        }
        if (instructionType(instruction) == TransferInstructionType.SETTLEMENT) {
            instruction.setSettlementReference(requiredString(transferVariables, "settlementReference"));
            instruction.setPayoutGroupReference(optionalString(transferVariables, "payoutGroupReference"));
            instruction.setSettlementBatchId(optionalString(transferVariables, "batchId"));
            instruction.setSettlementDate(optionalString(transferVariables, "settlementDate"));
            instruction.setSettlementItemCount(optionalInteger(transferVariables, "itemCount"));
        }
        instruction.setStatus(TransferInstructionStatus.RECEIVED);
        instruction.setRawMessage(transferMessage.toRawMessage());

        TransferInstruction savedInstruction = transferInstructionRepository.save(instruction);
        operationalEventService.info(
                receivedEventType(savedInstruction),
                referenceType(savedInstruction),
                savedInstruction.getReference(),
                receivedSummary(savedInstruction),
                eventDetails(savedInstruction, transferVariables)
        );
        return new TransferInstructionResult(savedInstruction, transferVariables, false, true);
    }

    public TransferInstruction markProcessStarted(TransferInstruction instruction, String processInstanceId) {
        instruction.setProcessInstanceId(processInstanceId);
        instruction.setStatus(TransferInstructionStatus.PROCESSING);
        if (instructionType(instruction) == TransferInstructionType.REFUND) {
            instruction.setRefundStatus(RefundLifecycleStatus.PROCESSING);
        }
        TransferInstruction savedInstruction = transferInstructionRepository.save(instruction);
        operationalEventService.info(
                instructionType(savedInstruction) == TransferInstructionType.REFUND
                        ? "OUTFLOW_REFUND_PROCESSING"
                        : instructionType(savedInstruction) == TransferInstructionType.SETTLEMENT
                        ? "OUTFLOW_SETTLEMENT_PROCESSING"
                        : "OUTFLOW_PROCESS_STARTED",
                referenceType(savedInstruction),
                savedInstruction.getReference(),
                instructionType(savedInstruction) == TransferInstructionType.REFUND
                        ? "Outflow refund process started"
                        : instructionType(savedInstruction) == TransferInstructionType.SETTLEMENT
                        ? "Outflow settlement process started"
                        : "Outflow Flowable process started",
                eventDetails(savedInstruction, Map.of("processInstanceId", processInstanceId))
        );
        return savedInstruction;
    }

    public Optional<TransferInstruction> markDebitedIfPresent(Map<String, Object> processVariables,
                                                              String processInstanceId,
                                                              Map<String, Object> debitResult) {
        return markStatusIfPresent(
                processVariables,
                processInstanceId,
                TransferInstructionStatus.DEBITED,
                debitResult
        );
    }

    public Optional<TransferInstruction> markTransferProcessingIfPresent(Map<String, Object> processVariables,
                                                                         String processInstanceId) {
        return markStatusIfPresent(
                processVariables,
                processInstanceId,
                TransferInstructionStatus.TRANSFER_PROCESSING,
                Map.of()
        );
    }

    public Optional<TransferInstruction> markReversalPendingIfPresent(Map<String, Object> processVariables,
                                                                      String processInstanceId,
                                                                      Map<String, Object> providerResult) {
        return markStatusIfPresent(
                processVariables,
                processInstanceId,
                TransferInstructionStatus.REVERSAL_PENDING,
                providerResult
        );
    }

    public Optional<TransferInstruction> markReversedIfPresent(Map<String, Object> processVariables,
                                                               String processInstanceId,
                                                               Map<String, Object> reversalResult) {
        return markStatusIfPresent(
                processVariables,
                processInstanceId,
                TransferInstructionStatus.REVERSED,
                reversalResult
        );
    }

    @Transactional(readOnly = true)
    public TransferInstruction getByReference(String reference) {
        return transferInstructionRepository.findByReference(reference)
                .orElseThrow(() -> new IllegalStateException("Transfer instruction not found: " + reference));
    }

    public Optional<TransferInstruction> markCompletedIfPresent(Map<String, Object> processVariables,
                                                                String processInstanceId) {
        return markCompletedIfPresent(processVariables, processInstanceId, Map.of());
    }

    public Optional<TransferInstruction> markCompletedIfPresent(Map<String, Object> processVariables,
                                                                String processInstanceId,
                                                                Map<String, Object> providerResult) {
        return markStatusIfPresent(
                processVariables,
                processInstanceId,
                TransferInstructionStatus.COMPLETED,
                providerResult
        );
    }

    public Optional<TransferInstruction> markFailedIfPresent(Map<String, Object> processVariables,
                                                             String processInstanceId) {
        return markFailedIfPresent(processVariables, processInstanceId, Map.of());
    }

    public Optional<TransferInstruction> markFailedIfPresent(Map<String, Object> processVariables,
                                                             String processInstanceId,
                                                             Map<String, Object> providerResult) {
        return markStatusIfPresent(
                processVariables,
                processInstanceId,
                TransferInstructionStatus.FAILED,
                providerResult
        );
    }

    private Optional<TransferInstruction> markStatusIfPresent(Map<String, Object> processVariables,
                                                              String processInstanceId,
                                                              TransferInstructionStatus status,
                                                              Map<String, Object> eventDetails) {
        return findProcessInstruction(processVariables, processInstanceId)
                .map(instruction -> {
                    instruction.setStatus(status);
                    updateRefundStatus(instruction, status);
                    mergeProviderResult(instruction, eventDetails);
                    TransferInstruction savedInstruction = transferInstructionRepository.save(instruction);
                    recordStatusEvent(savedInstruction, status, processInstanceId, eventDetails);
                    settlementResultPublisher.publish(savedInstruction, status, eventDetails);
                    outflowResultPublisher.publish(savedInstruction, status, eventDetails);
                    return savedInstruction;
                });
    }

    private void recordStatusEvent(TransferInstruction instruction,
                                   TransferInstructionStatus status,
                                   String processInstanceId,
                                   Map<String, Object> details) {
        Map<String, Object> eventDetails = eventDetails(instruction, details);
        eventDetails.put("processInstanceId", processInstanceId);
        eventDetails.put("status", status.name());
        if (status == TransferInstructionStatus.REVERSAL_PENDING || status == TransferInstructionStatus.FAILED) {
            eventDetails.put("failureClass", status == TransferInstructionStatus.REVERSAL_PENDING
                    ? "PAYOUT_PROVIDER_FAILED"
                    : "OUTFLOW_FAILED");
            operationalEventService.warning(
                    statusEventType(instruction, status),
                    referenceType(instruction),
                    instruction.getReference(),
                    statusSummary(instruction, status),
                    eventDetails
            );
            return;
        }
        operationalEventService.info(
                statusEventType(instruction, status),
                referenceType(instruction),
                instruction.getReference(),
                statusSummary(instruction, status),
                eventDetails
        );
    }

    private Optional<TransferInstruction> findProcessInstruction(Map<String, Object> processVariables,
                                                                 String processInstanceId) {
        Object reference = processVariables.get("transferReference");
        if (reference != null && !reference.toString().isBlank()) {
            return transferInstructionRepository.findByReference(reference.toString());
        }
        return transferInstructionRepository.findByProcessInstanceId(processInstanceId);
    }

    private void mergeProviderResult(TransferInstruction instruction, Map<String, Object> providerResult) {
        if (providerResult == null || providerResult.isEmpty()) {
            return;
        }
        Map<String, Object> rawMessage = new HashMap<>();
        if (instruction.getRawMessage() != null) {
            rawMessage.putAll(instruction.getRawMessage());
        }
        rawMessage.put("latestProviderEvent", new HashMap<>(providerResult));
        instruction.setRawMessage(rawMessage);
    }

    private void applyRecurrenceDefaults(Map<String, Object> transferVariables) {
        Object duration = transferVariables.get("duration");
        if (duration == null || duration.toString().isBlank()) {
            return;
        }

        DurationType.fromValue(duration.toString()).ifPresent(period -> {
            LocalDate nextDate = period.nextBillingDate(LocalDate.now());
            transferVariables.put("nextBillingDate", nextDate.toString());
            transferVariables.put("isRecurrent", true);
        });
    }

    private TransferInstruction findExistingInstruction(String reference,
                                                        String correlationId,
                                                        String refundReference,
                                                        String settlementReference,
                                                        String payoutGroupReference) {
        TransferInstruction byReference = transferInstructionRepository.findByReference(reference).orElse(null);
        if (byReference != null) {
            return byReference;
        }
        if (refundReference != null) {
            TransferInstruction byRefundReference = transferInstructionRepository.findByRefundReference(refundReference).orElse(null);
            if (byRefundReference != null) {
                return byRefundReference;
            }
        }
        if (payoutGroupReference != null) {
            TransferInstruction byPayoutGroupReference = transferInstructionRepository
                    .findByPayoutGroupReference(payoutGroupReference)
                    .orElse(null);
            if (byPayoutGroupReference != null) {
                return byPayoutGroupReference;
            }
        }
        if (settlementReference != null && payoutGroupReference == null) {
            TransferInstruction bySettlementReference = transferInstructionRepository
                    .findBySettlementReference(settlementReference)
                    .orElse(null);
            if (bySettlementReference != null) {
                return bySettlementReference;
            }
        }
        if (correlationId == null) {
            return null;
        }
        TransferInstruction byCorrelationId = transferInstructionRepository.findByCorrelationId(correlationId).orElse(null);
        if (byCorrelationId != null) {
            return byCorrelationId;
        }
        return null;
    }

    private String resolveReference(Map<String, Object> message, Map<String, Object> transferVariables) {
        return firstNonBlank(
                message.get("transferReference"),
                transferVariables.get("transferReference"),
                message.get("messageId"),
                message.get("correlationId"),
                "out_" + UUID.randomUUID().toString().replace("-", "")
        );
    }

    private String resolveCorrelationId(Map<String, Object> message, Map<String, Object> transferVariables) {
        return firstNullableNonBlank(
                message.get("correlationId"),
                transferVariables.get("correlationId"),
                message.get("messageId")
        );
    }

    private String requiredString(Map<String, Object> transferVariables, String key) {
        Object value = transferVariables.get(key);
        if (value == null || value.toString().isBlank()) {
            throw new IllegalArgumentException("Transfer instruction is missing " + key);
        }
        return value.toString();
    }

    private String optionalString(Map<String, Object> transferVariables, String key) {
        Object value = transferVariables.get(key);
        return value == null || value.toString().isBlank() ? null : value.toString();
    }

    private Integer optionalInteger(Map<String, Object> transferVariables, String key) {
        Object value = transferVariables.get(key);
        if (value == null || value.toString().isBlank()) {
            return null;
        }
        return Integer.valueOf(value.toString());
    }

    private TransferInstructionType resolveInstructionType(Map<String, Object> transferVariables) {
        Object value = transferVariables.get("instructionType");
        if (value == null || value.toString().isBlank()) {
            return TransferInstructionType.TRANSFER;
        }
        return TransferInstructionType.valueOf(value.toString().trim().toUpperCase());
    }

    private void updateRefundStatus(TransferInstruction instruction, TransferInstructionStatus status) {
        if (instructionType(instruction) != TransferInstructionType.REFUND) {
            return;
        }
        switch (status) {
            case RECEIVED -> instruction.setRefundStatus(RefundLifecycleStatus.REQUESTED);
            case PROCESSING, DEBITED, TRANSFER_PROCESSING -> instruction.setRefundStatus(RefundLifecycleStatus.PROCESSING);
            case COMPLETED -> instruction.setRefundStatus(RefundLifecycleStatus.SUCCESSFUL);
            case FAILED -> instruction.setRefundStatus(RefundLifecycleStatus.FAILED);
            case REVERSAL_PENDING -> instruction.setRefundStatus(RefundLifecycleStatus.REVERSAL_PENDING);
            case REVERSED -> instruction.setRefundStatus(RefundLifecycleStatus.REVERSED);
        }
    }

    private String referenceType(TransferInstruction instruction) {
        return switch (instructionType(instruction)) {
            case REFUND -> "REFUND";
            case SETTLEMENT -> "SETTLEMENT";
            case TRANSFER -> "TRANSFER";
        };
    }

    private String refundEventStatus(TransferInstruction instruction) {
        return instruction.getRefundStatus() == null
                ? instruction.getStatus().name()
                : instruction.getRefundStatus().name();
    }

    private String statusSummary(TransferInstruction instruction, TransferInstructionStatus status) {
        if (instructionType(instruction) == TransferInstructionType.REFUND) {
            return "Outflow refund moved to " + refundEventStatus(instruction);
        }
        if (instructionType(instruction) == TransferInstructionType.SETTLEMENT) {
            return "Outflow settlement moved to " + status.name();
        }
        return "Outflow transfer moved to " + status.name();
    }

    private TransferInstructionType instructionType(TransferInstruction instruction) {
        return instruction.getInstructionType() == null
                ? TransferInstructionType.TRANSFER
                : instruction.getInstructionType();
    }

    private String firstNonBlank(Object... values) {
        String value = firstNullableNonBlank(values);
        return Objects.requireNonNull(value);
    }

    private String firstNullableNonBlank(Object... values) {
        for (Object value : values) {
            if (value != null && !value.toString().isBlank()) {
                return value.toString().trim();
            }
        }
        return null;
    }

    private Map<String, Object> eventDetails(TransferInstruction instruction, Map<String, Object> details) {
        Map<String, Object> eventDetails = new HashMap<>();
        if (details != null) {
            eventDetails.putAll(details);
        }
        eventDetails.put("transferReference", instruction.getReference());
        eventDetails.put("instructionType", instructionType(instruction).name());
        eventDetails.put("refundReference", instruction.getRefundReference());
        eventDetails.put("originalTransactionId", instruction.getOriginalTransactionId());
        eventDetails.put("refundStatus", instruction.getRefundStatus() == null ? null : instruction.getRefundStatus().name());
        eventDetails.put("settlementReference", instruction.getSettlementReference());
        eventDetails.put("payoutGroupReference", instruction.getPayoutGroupReference());
        eventDetails.put("settlementBatchId", instruction.getSettlementBatchId());
        eventDetails.put("settlementDate", instruction.getSettlementDate());
        eventDetails.put("settlementItemCount", instruction.getSettlementItemCount());
        eventDetails.put("correlationId", instruction.getCorrelationId());
        eventDetails.put("merchantId", instruction.getMerchantId());
        eventDetails.put("amount", instruction.getAmount());
        eventDetails.put("processInstanceId", instruction.getProcessInstanceId());
        return eventDetails;
    }

    private Map<String, Object> withFailure(Map<String, Object> message, String failureClass, String failureMessage) {
        Map<String, Object> details = new HashMap<>();
        if (message != null) {
            details.putAll(message);
        }
        details.put("failureClass", failureClass);
        details.put("failureMessage", failureMessage);
        return details;
    }

    private String stringValue(Object value) {
        return value == null || value.toString().isBlank() ? null : value.toString();
    }

    private String receivedEventType(TransferInstruction instruction) {
        return switch (instructionType(instruction)) {
            case REFUND -> "OUTFLOW_REFUND_REQUESTED";
            case SETTLEMENT -> "OUTFLOW_SETTLEMENT_REQUESTED";
            case TRANSFER -> "OUTFLOW_TRANSFER_RECEIVED";
        };
    }

    private String duplicateEventType(TransferInstruction instruction) {
        return switch (instructionType(instruction)) {
            case REFUND -> "OUTFLOW_REFUND_DUPLICATE";
            case SETTLEMENT -> "OUTFLOW_SETTLEMENT_DUPLICATE";
            case TRANSFER -> "OUTFLOW_TRANSFER_DUPLICATE";
        };
    }

    private String statusEventType(TransferInstruction instruction, TransferInstructionStatus status) {
        return switch (instructionType(instruction)) {
            case REFUND -> "OUTFLOW_REFUND_" + refundEventStatus(instruction);
            case SETTLEMENT -> "OUTFLOW_SETTLEMENT_" + status.name();
            case TRANSFER -> "OUTFLOW_TRANSFER_" + status.name();
        };
    }

    private String receivedSummary(TransferInstruction instruction) {
        return switch (instructionType(instruction)) {
            case REFUND -> "Outflow refund instruction accepted";
            case SETTLEMENT -> "Outflow settlement instruction accepted";
            case TRANSFER -> "Outflow transfer instruction accepted";
        };
    }
}

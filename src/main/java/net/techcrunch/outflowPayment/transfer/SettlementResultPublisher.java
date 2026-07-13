package net.techcrunch.outflowPayment.transfer;

import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Component
public class SettlementResultPublisher {

    public static final String EVENT_TYPE = "OUTFLOW_SETTLEMENT_RESULT";

    private final AmqpTemplate rabbitTemplate;
    private final String exchangeName;
    private final String routingKey;

    public SettlementResultPublisher(AmqpTemplate rabbitTemplate,
                                     @Value("${message.flowable.message.exchange}") String exchangeName,
                                     @Value("${message.bluepay.settlement-result-routing-key}") String routingKey) {
        this.rabbitTemplate = rabbitTemplate;
        this.exchangeName = exchangeName;
        this.routingKey = routingKey;
    }

    public void publish(TransferInstruction instruction, TransferInstructionStatus status, Map<String, Object> details) {
        if (instruction.getInstructionType() != TransferInstructionType.SETTLEMENT || !isTerminal(status)) {
            return;
        }

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("settlementReference", instruction.getSettlementReference());
        payload.put("payoutGroupReference", instruction.getPayoutGroupReference());
        payload.put("batchReference", instruction.getSettlementReference());
        payload.put("settlementBatchId", instruction.getSettlementBatchId());
        payload.put("settlementDate", instruction.getSettlementDate());
        payload.put("settlementItemCount", instruction.getSettlementItemCount());
        payload.put("amount", instruction.getAmount());
        payload.put("outflowStatus", status.name());
        payload.put("settlementStatus", settlementStatus(status));
        payload.put("processInstanceId", instruction.getProcessInstanceId());
        payload.put("providerResult", details == null ? Map.of() : new LinkedHashMap<>(details));
        payload.put("items", itemResults(instruction, status, details));

        Map<String, Object> message = new LinkedHashMap<>();
        message.put("eventType", EVENT_TYPE);
        message.put("eventVersion", "1");
        message.put("sourceService", "outflowpayment");
        message.put("messageId", "msg_" + UUID.randomUUID().toString().replace("-", ""));
        message.put("correlationId", instruction.getCorrelationId());
        message.put("reference", instruction.getPayoutGroupReference() == null
                ? instruction.getSettlementReference()
                : instruction.getPayoutGroupReference());
        message.put("createdAt", OffsetDateTime.now().toString());
        message.put("payload", payload);

        rabbitTemplate.convertAndSend(exchangeName, routingKey, message);
    }

    private boolean isTerminal(TransferInstructionStatus status) {
        return status == TransferInstructionStatus.COMPLETED
                || status == TransferInstructionStatus.FAILED
                || status == TransferInstructionStatus.REVERSED;
    }

    private String settlementStatus(TransferInstructionStatus status) {
        return status == TransferInstructionStatus.COMPLETED ? "SETTLED" : "FAILED";
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> itemResults(TransferInstruction instruction,
                                                  TransferInstructionStatus status,
                                                  Map<String, Object> details) {
        Map<String, Object> transferPayload = transferPayload(instruction);
        Object items = transferPayload.get("items");
        if (!(items instanceof List<?> list)) {
            return List.of();
        }
        List<Map<String, Object>> results = new ArrayList<>();
        for (Object item : list) {
            if (!(item instanceof Map<?, ?> itemMap)) {
                continue;
            }
            Map<String, Object> result = new LinkedHashMap<>();
            Object transactionReference = itemMap.get("transactionReference");
            if (transactionReference != null) {
                result.put("transactionReference", transactionReference.toString());
            }
            result.put("status", settlementStatus(status));
            if (details != null && details.get("providerReference") != null) {
                result.put("providerReference", details.get("providerReference"));
            }
            if (status != TransferInstructionStatus.COMPLETED) {
                result.put("failureCode", details == null ? null : details.get("failureCode"));
                result.put("failureReason", details == null ? status.name() : details.getOrDefault("failureReason", status.name()));
            }
            results.add(result);
        }
        return results;
    }

    private Map<String, Object> transferPayload(TransferInstruction instruction) {
        if (instruction.getRawMessage() == null) {
            return Map.of();
        }
        Object payload = instruction.getRawMessage().get("TransferDTO");
        if (!(payload instanceof Map<?, ?> payloadMap)) {
            return Map.of();
        }
        Map<String, Object> normalized = new LinkedHashMap<>();
        payloadMap.forEach((key, value) -> normalized.put(String.valueOf(key), value));
        return normalized;
    }
}

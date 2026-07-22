package net.techcrunch.outflowPayment.transfer;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public record OutflowTransferMessage(
        String eventType,
        String eventVersion,
        String sourceService,
        String messageId,
        String correlationId,
        OffsetDateTime createdAt,
        Map<String, Object> payload
) {

    public static final String SUPPORTED_EVENT_TYPE = "OUTFLOW_TRANSFER_REQUESTED";
    public static final String SUPPORTED_REFUND_EVENT_TYPE = "OUTFLOW_REFUND_REQUESTED";
    public static final String SUPPORTED_SETTLEMENT_EVENT_TYPE = "OUTFLOW_SETTLEMENT_REQUESTED";
    public static final String SUPPORTED_EVENT_VERSION = "1";

    public static OutflowTransferMessage from(Map<String, Object> message) {
        require(message, "eventType");
        require(message, "eventVersion");
        require(message, "sourceService");
        require(message, "messageId");
        require(message, "correlationId");
        require(message, "createdAt");

        String eventType = message.get("eventType").toString();
        String eventVersion = message.get("eventVersion").toString();
        if (!SUPPORTED_EVENT_TYPE.equals(eventType)
                && !SUPPORTED_REFUND_EVENT_TYPE.equals(eventType)
                && !SUPPORTED_SETTLEMENT_EVENT_TYPE.equals(eventType)) {
            throw new IllegalArgumentException("Unsupported outflow eventType: " + eventType);
        }
        if (!SUPPORTED_EVENT_VERSION.equals(eventVersion)) {
            throw new IllegalArgumentException("Unsupported outflow eventVersion: " + eventVersion);
        }

        OffsetDateTime createdAt = parseCreatedAt(message.get("createdAt"));
        Map<String, Object> payload = extractPayload(message);
        validatePayload(eventType, payload);

        payload.putIfAbsent("transferReference", firstNonBlank(
                message.get("transferReference"),
                payload.get("transferReference"),
                payload.get("settlementReference"),
                payload.get("batchReference"),
                payload.get("refundReference"),
                message.get("messageId")
        ));
        payload.putIfAbsent("instructionType", instructionType(eventType));
        payload.putIfAbsent("correlationId", message.get("correlationId").toString());
        payload.putIfAbsent("eventVersion", eventVersion);
        payload.putIfAbsent("sourceService", message.get("sourceService").toString());
        payload.putIfAbsent("createdAt", createdAt.toString());
        if (SUPPORTED_SETTLEMENT_EVENT_TYPE.equals(eventType)) {
            normalizeSettlementPayload(payload);
        }

        return new OutflowTransferMessage(
                eventType,
                eventVersion,
                message.get("sourceService").toString(),
                message.get("messageId").toString(),
                message.get("correlationId").toString(),
                createdAt,
                payload
        );
    }

    public Map<String, Object> toProcessVariables() {
        Map<String, Object> variables = new HashMap<>(payload);
        variables.put("eventType", eventType);
        variables.put("eventVersion", eventVersion);
        variables.put("sourceService", sourceService);
        variables.put("messageId", messageId);
        variables.put("correlationId", correlationId);
        variables.put("createdAt", createdAt.toString());
        return variables;
    }

    public Map<String, Object> toRawMessage() {
        Map<String, Object> rawMessage = new HashMap<>();
        rawMessage.put("eventType", eventType);
        rawMessage.put("eventVersion", eventVersion);
        rawMessage.put("sourceService", sourceService);
        rawMessage.put("messageId", messageId);
        rawMessage.put("correlationId", correlationId);
        rawMessage.put("createdAt", createdAt.toString());
        rawMessage.put("TransferDTO", new HashMap<>(payload));
        return rawMessage;
    }

    private static Map<String, Object> extractPayload(Map<String, Object> message) {
        Object payload = message.get("payload");
        if (payload == null) {
            payload = message.get("TransferDTO");
        }
        if (!(payload instanceof Map<?, ?> payloadMap)) {
            throw new IllegalArgumentException("Outflow message is missing transfer payload");
        }

        Map<String, Object> normalizedPayload = new HashMap<>();
        payloadMap.forEach((key, value) -> normalizedPayload.put(String.valueOf(key), value));
        return normalizedPayload;
    }

    private static void validatePayload(String eventType, Map<String, Object> payload) {
        if (SUPPORTED_SETTLEMENT_EVENT_TYPE.equals(eventType)) {
            validateSettlementPayload(payload);
            return;
        }
        require(payload, "merchantId");
        require(payload, "amountToSend");
        require(payload, "accNumber");
        if (SUPPORTED_REFUND_EVENT_TYPE.equals(eventType)) {
            require(payload, "refundReference");
            require(payload, "transactionId");
        }

        try {
            BigDecimal amount = new BigDecimal(payload.get("amountToSend").toString());
            if (amount.signum() <= 0) {
                throw new IllegalArgumentException("Transfer amount must be positive");
            }
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("Transfer amount is invalid", exception);
        }
    }

    private static void validateSettlementPayload(Map<String, Object> payload) {
        require(payload, "batchReference");
        require(payload, "netAmount");
        require(payload, "itemCount");
        try {
            BigDecimal amount = new BigDecimal(payload.get("netAmount").toString());
            if (amount.signum() <= 0) {
                throw new IllegalArgumentException("Settlement netAmount must be positive");
            }
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("Settlement netAmount is invalid", exception);
        }
    }

    private static void normalizeSettlementPayload(Map<String, Object> payload) {
        Object batchReference = firstNonBlank(
                payload.get("settlementReference"),
                payload.get("batchReference"),
                payload.get("transferReference")
        );
        payload.putIfAbsent("settlementReference", batchReference);
        payload.putIfAbsent("transferReference", firstNonBlank(
                payload.get("payoutGroupReference"),
                payload.get("transferReference"),
                batchReference
        ));
        payload.putIfAbsent("merchantId", "SETTLEMENT_BATCH");
        payload.putIfAbsent("accNumber", batchReference);
        payload.putIfAbsent("amountToSend", payload.get("netAmount"));
    }

    private static String instructionType(String eventType) {
        if (SUPPORTED_REFUND_EVENT_TYPE.equals(eventType)) {
            return "REFUND";
        }
        if (SUPPORTED_SETTLEMENT_EVENT_TYPE.equals(eventType)) {
            return "SETTLEMENT";
        }
        return "TRANSFER";
    }

    private static void require(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value == null || value.toString().isBlank()) {
            throw new IllegalArgumentException("Outflow message is missing " + key);
        }
    }

    private static OffsetDateTime parseCreatedAt(Object createdAt) {
        try {
            return OffsetDateTime.parse(createdAt.toString());
        } catch (DateTimeParseException exception) {
            throw new IllegalArgumentException("Outflow message createdAt is invalid", exception);
        }
    }

    private static Object firstNonBlank(Object... values) {
        for (Object value : values) {
            if (value != null && !value.toString().isBlank()) {
                return value.toString().trim();
            }
        }
        return Objects.requireNonNull(values[values.length - 1]);
    }
}

package net.techcrunch.outflowPayment.transfer;

import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OutflowTransferMessageTest {

    @Test
    void fromAcceptsFormalPayloadEnvelope() {
        Map<String, Object> message = envelope();
        message.put("payload", payload());

        OutflowTransferMessage transferMessage = OutflowTransferMessage.from(message);

        assertThat(transferMessage.eventType()).isEqualTo("OUTFLOW_TRANSFER_REQUESTED");
        assertThat(transferMessage.payload()).containsEntry("merchantId", "merchant-1");
        assertThat(transferMessage.toProcessVariables()).containsEntry("messageId", "msg-123");
    }

    @Test
    void fromAcceptsLegacyTransferDtoEnvelope() {
        Map<String, Object> message = envelope();
        message.put("TransferDTO", payload());

        OutflowTransferMessage transferMessage = OutflowTransferMessage.from(message);

        assertThat(transferMessage.payload()).containsEntry("accNumber", "0123456789");
    }

    @Test
    void fromRejectsMissingRequiredPayloadField() {
        Map<String, Object> payload = payload();
        payload.remove("accNumber");
        Map<String, Object> message = envelope();
        message.put("payload", payload);

        assertThatThrownBy(() -> OutflowTransferMessage.from(message))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("accNumber");
    }

    @Test
    void fromAcceptsSettlementBatchEnvelopeAndNormalizesProcessVariables() {
        Map<String, Object> message = envelope();
        message.put("eventType", "OUTFLOW_SETTLEMENT_REQUESTED");
        message.put("transferReference", "set_20260711_abc");
        Map<String, Object> payload = new HashMap<>();
        payload.put("batchId", 1000L);
        payload.put("batchReference", "set_20260711_abc");
        payload.put("settlementDate", "2026-07-11");
        payload.put("netAmount", "1180.00");
        payload.put("itemCount", 1);
        message.put("payload", payload);

        OutflowTransferMessage transferMessage = OutflowTransferMessage.from(message);

        assertThat(transferMessage.eventType()).isEqualTo("OUTFLOW_SETTLEMENT_REQUESTED");
        assertThat(transferMessage.payload())
                .containsEntry("instructionType", "SETTLEMENT")
                .containsEntry("settlementReference", "set_20260711_abc")
                .containsEntry("merchantId", "SETTLEMENT_BATCH")
                .containsEntry("amountToSend", "1180.00")
                .containsEntry("accNumber", "set_20260711_abc");
    }

    @Test
    void fromRejectsSettlementWithoutBatchReference() {
        Map<String, Object> message = envelope();
        message.put("eventType", "OUTFLOW_SETTLEMENT_REQUESTED");
        message.put("payload", Map.of("netAmount", "1180.00", "itemCount", 1));

        assertThatThrownBy(() -> OutflowTransferMessage.from(message))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("batchReference");
    }

    private Map<String, Object> envelope() {
        Map<String, Object> message = new HashMap<>();
        message.put("eventType", "OUTFLOW_TRANSFER_REQUESTED");
        message.put("eventVersion", "1");
        message.put("sourceService", "bluepay");
        message.put("messageId", "msg-123");
        message.put("correlationId", "corr-123");
        message.put("createdAt", OffsetDateTime.now().toString());
        return message;
    }

    private Map<String, Object> payload() {
        Map<String, Object> payload = new HashMap<>();
        payload.put("merchantId", "merchant-1");
        payload.put("amountToSend", "2500.00");
        payload.put("accNumber", "0123456789");
        return payload;
    }
}

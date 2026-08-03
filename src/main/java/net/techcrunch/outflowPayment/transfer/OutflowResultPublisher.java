package net.techcrunch.outflowPayment.transfer;

import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Component
public class OutflowResultPublisher {

    private static final String TRANSFER_RESULT_EVENT = "OUTFLOW_TRANSFER_RESULT";
    private static final String REFUND_RESULT_EVENT = "OUTFLOW_REFUND_RESULT";

    private final AmqpTemplate rabbitTemplate;
    private final String exchangeName;
    private final String routingKey;

    public OutflowResultPublisher(AmqpTemplate rabbitTemplate,
                                  @Value("${message.flowable.message.exchange}") String exchangeName,
                                  @Value("${message.bluepay.outflow-result-routing-key}") String routingKey) {
        this.rabbitTemplate = rabbitTemplate;
        this.exchangeName = exchangeName;
        this.routingKey = routingKey;
    }

    public void publish(TransferInstruction instruction, TransferInstructionStatus status, Map<String, Object> details) {
        if (instruction.getInstructionType() == TransferInstructionType.SETTLEMENT || !isTerminal(status)) {
            return;
        }

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("instructionType", instruction.getInstructionType().name());
        payload.put("transferReference", instruction.getReference());
        payload.put("merchantId", instruction.getMerchantId());
        payload.put("amount", instruction.getAmount());
        payload.put("outflowStatus", status.name());
        payload.put("processInstanceId", instruction.getProcessInstanceId());
        payload.put("providerResult", details == null ? Map.of() : new LinkedHashMap<>(details));

        if (instruction.getInstructionType() == TransferInstructionType.REFUND) {
            payload.put("refundReference", instruction.getRefundReference());
            payload.put("transactionId", instruction.getOriginalTransactionId());
            payload.put("refundStatus", instruction.getRefundStatus() == null
                    ? status.name()
                    : instruction.getRefundStatus().name());
        }

        Map<String, Object> message = new LinkedHashMap<>();
        message.put("eventType", instruction.getInstructionType() == TransferInstructionType.REFUND
                ? REFUND_RESULT_EVENT
                : TRANSFER_RESULT_EVENT);
        message.put("eventVersion", "1");
        message.put("sourceService", "outflowpayment");
        message.put("messageId", "msg_" + UUID.randomUUID().toString().replace("-", ""));
        message.put("correlationId", instruction.getCorrelationId());
        message.put("reference", instruction.getInstructionType() == TransferInstructionType.REFUND
                ? instruction.getRefundReference()
                : instruction.getReference());
        message.put("createdAt", OffsetDateTime.now().toString());
        message.put("payload", payload);

        rabbitTemplate.convertAndSend(exchangeName, routingKey, message);
    }

    private boolean isTerminal(TransferInstructionStatus status) {
        return status == TransferInstructionStatus.COMPLETED
                || status == TransferInstructionStatus.FAILED
                || status == TransferInstructionStatus.REVERSED;
    }
}

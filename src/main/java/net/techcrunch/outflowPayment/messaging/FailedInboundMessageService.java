package net.techcrunch.outflowPayment.messaging;

import net.techcrunch.outflowPayment.observability.OperationalEventService;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional
public class FailedInboundMessageService {

    private final FailedInboundMessageRepository failedInboundMessageRepository;
    private final RabbitTemplate rabbitTemplate;
    private final OperationalEventService operationalEventService;

    public FailedInboundMessageService(FailedInboundMessageRepository failedInboundMessageRepository,
                                       RabbitTemplate rabbitTemplate,
                                       OperationalEventService operationalEventService) {
        this.failedInboundMessageRepository = failedInboundMessageRepository;
        this.rabbitTemplate = rabbitTemplate;
        this.operationalEventService = operationalEventService;
    }

    public FailedInboundMessage recordFailure(String queueName,
                                              String exchangeName,
                                              String routingKey,
                                              Map<String, Object> payload,
                                              String failureClass,
                                              String failureReason) {
        Map<String, Object> safePayload = payload == null ? Map.of() : new HashMap<>(payload);
        FailedInboundMessage message = new FailedInboundMessage();
        message.setQueueName(queueName);
        message.setExchangeName(exchangeName);
        message.setRoutingKey(routingKey);
        message.setPayload(safePayload);
        message.setReference(firstNonBlank(safePayload.get("transferReference"), safePayload.get("reference")));
        message.setCorrelationId(stringValue(safePayload.get("correlationId")));
        message.setMessageId(stringValue(safePayload.get("messageId")));
        message.setFailureClass(failureClass);
        message.setFailureReason(failureReason);

        FailedInboundMessage savedMessage = failedInboundMessageRepository.save(message);
        operationalEventService.error(
                "RABBITMQ_MESSAGE_REJECTED",
                "RABBITMQ_MESSAGE",
                savedMessage.getReference(),
                "Inbound RabbitMQ message failed and was recorded for replay",
                eventDetails(savedMessage)
        );
        return savedMessage;
    }

    @Transactional(readOnly = true)
    public List<FailedInboundMessage> listFailed() {
        return failedInboundMessageRepository.findByStatusOrderByDateCreatedDesc(FailedMessageStatus.FAILED);
    }

    public FailedInboundMessage replay(Long id) {
        FailedInboundMessage message = failedInboundMessageRepository.findById(id)
                .orElseThrow(() -> new IllegalStateException("Failed inbound message not found: " + id));
        if (message.getStatus() != FailedMessageStatus.FAILED) {
            throw new IllegalStateException("Only FAILED messages can be replayed");
        }

        rabbitTemplate.convertAndSend(message.getExchangeName(), message.getRoutingKey(), message.getPayload());
        message.setStatus(FailedMessageStatus.REPLAYED);
        message.setReplayedAt(OffsetDateTime.now());
        FailedInboundMessage savedMessage = failedInboundMessageRepository.save(message);
        operationalEventService.info(
                "RABBITMQ_MESSAGE_REPLAYED",
                "RABBITMQ_MESSAGE",
                savedMessage.getReference(),
                "Failed RabbitMQ message replayed",
                eventDetails(savedMessage)
        );
        return savedMessage;
    }

    public FailedInboundMessage ignore(Long id) {
        FailedInboundMessage message = failedInboundMessageRepository.findById(id)
                .orElseThrow(() -> new IllegalStateException("Failed inbound message not found: " + id));
        message.setStatus(FailedMessageStatus.IGNORED);
        FailedInboundMessage savedMessage = failedInboundMessageRepository.save(message);
        operationalEventService.warning(
                "RABBITMQ_MESSAGE_IGNORED",
                "RABBITMQ_MESSAGE",
                savedMessage.getReference(),
                "Failed RabbitMQ message marked ignored",
                eventDetails(savedMessage)
        );
        return savedMessage;
    }

    private Map<String, Object> eventDetails(FailedInboundMessage message) {
        Map<String, Object> details = new HashMap<>();
        details.put("queueName", message.getQueueName());
        details.put("exchangeName", message.getExchangeName());
        details.put("routingKey", message.getRoutingKey());
        details.put("reference", message.getReference());
        details.put("correlationId", message.getCorrelationId());
        details.put("messageId", message.getMessageId());
        details.put("failureClass", message.getFailureClass());
        details.put("failureReason", message.getFailureReason());
        details.put("status", message.getStatus().name());
        return details;
    }

    private String firstNonBlank(Object... values) {
        for (Object value : values) {
            String result = stringValue(value);
            if (result != null) {
                return result;
            }
        }
        return null;
    }

    private String stringValue(Object value) {
        return value == null || value.toString().isBlank() ? null : value.toString();
    }
}

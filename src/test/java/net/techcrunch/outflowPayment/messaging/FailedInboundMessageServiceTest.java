package net.techcrunch.outflowPayment.messaging;

import net.techcrunch.outflowPayment.observability.OperationalEventService;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FailedInboundMessageServiceTest {

    private final FailedInboundMessageRepository repository = mock(FailedInboundMessageRepository.class);
    private final RabbitTemplate rabbitTemplate = mock(RabbitTemplate.class);
    private final OperationalEventService operationalEventService = mock(OperationalEventService.class);
    private final FailedInboundMessageService service = new FailedInboundMessageService(
            repository,
            rabbitTemplate,
            operationalEventService
    );

    @Test
    void recordFailurePersistsReplayableMessage() {
        when(repository.save(org.mockito.ArgumentMatchers.any(FailedInboundMessage.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        FailedInboundMessage message = service.recordFailure(
                "outflowPayment.queue",
                "flowable.message.exchange",
                "outflowPayment.routingKey",
                Map.of("transferReference", "trf_123", "correlationId", "corr_123"),
                "IllegalArgumentException",
                "Invalid message"
        );

        assertThat(message.getStatus()).isEqualTo(FailedMessageStatus.FAILED);
        assertThat(message.getReference()).isEqualTo("trf_123");
        assertThat(message.getCorrelationId()).isEqualTo("corr_123");
    }

    @Test
    void replayPublishesOriginalPayloadAndMarksReplayed() {
        FailedInboundMessage message = new FailedInboundMessage();
        message.setId(1000L);
        message.setExchangeName("flowable.message.exchange");
        message.setRoutingKey("outflowPayment.routingKey");
        message.setPayload(Map.of("transferReference", "trf_123"));
        message.setStatus(FailedMessageStatus.FAILED);

        when(repository.findById(1000L)).thenReturn(Optional.of(message));
        when(repository.save(message)).thenReturn(message);

        FailedInboundMessage replayed = service.replay(1000L);

        verify(rabbitTemplate).convertAndSend(
                "flowable.message.exchange",
                "outflowPayment.routingKey",
                Map.of("transferReference", "trf_123")
        );
        assertThat(replayed.getStatus()).isEqualTo(FailedMessageStatus.REPLAYED);
        assertThat(replayed.getReplayedAt()).isNotNull();
    }
}

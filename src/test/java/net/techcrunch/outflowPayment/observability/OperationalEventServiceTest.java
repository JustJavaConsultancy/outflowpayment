package net.techcrunch.outflowPayment.observability;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class OperationalEventServiceTest {

    private final OperationalEventRepository repository = mock(OperationalEventRepository.class);
    private final OperationalEventService service = new OperationalEventService(repository);

    @Test
    void recordPersistsNormalizedOperationalEvent() {
        when(repository.save(any(OperationalEvent.class))).thenAnswer(invocation -> invocation.getArgument(0));

        OperationalEvent event = service.warning(
                "OUTFLOW_TRANSFER_DUPLICATE",
                "TRANSFER",
                "trf-123",
                "Duplicate transfer",
                Map.of(
                        "correlationId", "corr-123",
                        "messageId", "msg-123",
                        "merchantId", "merchant-1",
                        "processInstanceId", "process-1",
                        "failureClass", "DUPLICATE_MESSAGE"
                )
        );

        assertThat(event.getServiceName()).isEqualTo("outflowpayment");
        assertThat(event.getEventType()).isEqualTo("OUTFLOW_TRANSFER_DUPLICATE");
        assertThat(event.getSeverity()).isEqualTo(OperationalEventSeverity.WARNING);
        assertThat(event.getReference()).isEqualTo("trf-123");
        assertThat(event.getCorrelationId()).isEqualTo("corr-123");
        assertThat(event.getMessageId()).isEqualTo("msg-123");
        assertThat(event.getMerchantId()).isEqualTo("merchant-1");
        assertThat(event.getProcessInstanceId()).isEqualTo("process-1");
        assertThat(event.getFailureClass()).isEqualTo("DUPLICATE_MESSAGE");
    }
}

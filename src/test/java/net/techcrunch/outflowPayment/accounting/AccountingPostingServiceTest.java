package net.techcrunch.outflowPayment.accounting;

import net.techcrunch.outflowPayment.observability.OperationalEventService;
import org.flowable.engine.delegate.DelegateExecution;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AccountingPostingServiceTest {

    private final AccountingPostingRecordRepository repository = mock(AccountingPostingRecordRepository.class);
    private final OperationalEventService operationalEventService = mock(OperationalEventService.class);
    private final AccountingPostingService service = new AccountingPostingService(repository, operationalEventService);

    @Test
    void beginCreatesOutflowPostingRecordWhenNoRecordExists() {
        DelegateExecution execution = mockExecution();
        when(repository.findByOperationAndPostingKey("MERCHANT_OUTFLOW_DEBIT", "trf_123:MERCHANT_OUTFLOW_DEBIT"))
                .thenReturn(Optional.empty());
        when(repository.save(any(AccountingPostingRecord.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        AccountingPostingRecord record = service.begin(
                "MERCHANT_OUTFLOW_DEBIT",
                "trf_123:MERCHANT_OUTFLOW_DEBIT",
                BigDecimal.valueOf(1000),
                execution,
                Map.of("transferReference", "trf_123")
        );

        assertThat(record.getStatus()).isEqualTo(AccountingPostingStatus.IN_PROGRESS);
        assertThat(record.getTransferReference()).isEqualTo("trf_123");
        assertThat(record.getProcessInstanceId()).isEqualTo("process-1");
    }

    @Test
    void findCompletedResponseReturnsSavedPayloadForRetry() {
        AccountingPostingRecord record = new AccountingPostingRecord();
        record.setStatus(AccountingPostingStatus.COMPLETED);
        record.setResponsePayload(Map.of("reference", "out_123"));
        when(repository.findByOperationAndPostingKey("MERCHANT_OUTFLOW_DEBIT", "trf_123:MERCHANT_OUTFLOW_DEBIT"))
                .thenReturn(Optional.of(record));

        Optional<Map<String, Object>> response = service.findCompletedResponse(
                "MERCHANT_OUTFLOW_DEBIT",
                "trf_123:MERCHANT_OUTFLOW_DEBIT"
        );

        assertThat(response).isPresent();
        assertThat(response.get()).containsEntry("reference", "out_123");
    }

    @Test
    void merchantOutflowPostingKeyPrefersTransferReference() {
        DelegateExecution execution = mockExecution();
        when(execution.getVariable("transferReference")).thenReturn("trf_123");

        String postingKey = service.merchantOutflowPostingKey(execution);

        assertThat(postingKey).isEqualTo("trf_123:MERCHANT_OUTFLOW_DEBIT");
    }

    @Test
    void outflowReversalPostingKeyPrefersTransferReference() {
        DelegateExecution execution = mockExecution();
        when(execution.getVariable("transferReference")).thenReturn("trf_123");

        String postingKey = service.outflowReversalPostingKey(execution);

        assertThat(postingKey).isEqualTo("trf_123:OUTFLOW_REVERSAL");
    }

    private DelegateExecution mockExecution() {
        DelegateExecution execution = mock(DelegateExecution.class);
        when(execution.getProcessInstanceId()).thenReturn("process-1");
        when(execution.getCurrentActivityId()).thenReturn("activity-1");
        return execution;
    }
}

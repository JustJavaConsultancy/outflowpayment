package net.techcrunch.outflowPayment.reconciliation;

import net.techcrunch.outflowPayment.accounting.AccountingPostingRecordRepository;
import net.techcrunch.outflowPayment.observability.OperationalEventService;
import net.techcrunch.outflowPayment.transfer.RefundLifecycleStatus;
import net.techcrunch.outflowPayment.transfer.TransferInstruction;
import net.techcrunch.outflowPayment.transfer.TransferInstructionRepository;
import net.techcrunch.outflowPayment.transfer.TransferInstructionStatus;
import net.techcrunch.outflowPayment.transfer.TransferInstructionType;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OutflowReconciliationServiceTest {

    private final TransferInstructionRepository transferInstructionRepository = mock(TransferInstructionRepository.class);
    private final AccountingPostingRecordRepository postingRecordRepository = mock(AccountingPostingRecordRepository.class);
    private final ReconciliationRunRepository runRepository = mock(ReconciliationRunRepository.class);
    private final ReconciliationItemRepository itemRepository = mock(ReconciliationItemRepository.class);
    private final OperationalEventService operationalEventService = mock(OperationalEventService.class);
    private final GenericCsvStatementParser statementParser = mock(GenericCsvStatementParser.class);
    private final ReconciliationStatementImportRepository importRepository = mock(ReconciliationStatementImportRepository.class);
    private final ReconciliationStatementItemRepository statementItemRepository = mock(ReconciliationStatementItemRepository.class);
    private final ReconciliationStatementRejectedItemRepository rejectedItemRepository =
            mock(ReconciliationStatementRejectedItemRepository.class);
    private final ReconciliationAuditEventRepository auditEventRepository = mock(ReconciliationAuditEventRepository.class);
    private final OutflowReconciliationService service = new OutflowReconciliationService(
            transferInstructionRepository,
            postingRecordRepository,
            runRepository,
            itemRepository,
            operationalEventService,
            statementParser,
            importRepository,
            statementItemRepository,
            rejectedItemRepository,
            auditEventRepository
    );

    @Test
    void runCreatesIssueForCompletedTransferWithoutDebitPosting() {
        TransferInstruction instruction = new TransferInstruction();
        instruction.setReference("trf_123");
        instruction.setAmount(BigDecimal.valueOf(2500));
        instruction.setStatus(TransferInstructionStatus.COMPLETED);
        instruction.setDateCreated(OffsetDateTime.now());

        when(transferInstructionRepository.findAll()).thenReturn(List.of(instruction));
        when(postingRecordRepository.findByOperationAndPostingKey(
                "MERCHANT_OUTFLOW_DEBIT",
                "trf_123:MERCHANT_OUTFLOW_DEBIT"
        )).thenReturn(Optional.empty());
        when(runRepository.save(any(ReconciliationRun.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(itemRepository.save(any(ReconciliationItem.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ReconciliationRun run = service.run();

        assertThat(run.getStatus()).isEqualTo(ReconciliationRunStatus.COMPLETED);
        assertThat(run.getSummary()).containsEntry("issueCount", 1);
        verify(itemRepository).save(any(ReconciliationItem.class));
    }

    @Test
    void runCreatesRefundIssueForSuccessfulRefundWithoutDebitPosting() {
        TransferInstruction instruction = new TransferInstruction();
        instruction.setReference("ref_123");
        instruction.setRefundReference("ref_123");
        instruction.setInstructionType(TransferInstructionType.REFUND);
        instruction.setRefundStatus(RefundLifecycleStatus.SUCCESSFUL);
        instruction.setAmount(BigDecimal.valueOf(1200));
        instruction.setStatus(TransferInstructionStatus.COMPLETED);
        instruction.setDateCreated(OffsetDateTime.now());

        when(transferInstructionRepository.findAll()).thenReturn(List.of(instruction));
        when(postingRecordRepository.findByOperationAndPostingKey(
                "MERCHANT_OUTFLOW_DEBIT",
                "ref_123:MERCHANT_OUTFLOW_DEBIT"
        )).thenReturn(Optional.empty());
        when(runRepository.save(any(ReconciliationRun.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(itemRepository.save(any(ReconciliationItem.class))).thenAnswer(invocation -> {
            ReconciliationItem item = invocation.getArgument(0);
            assertThat(item.getReferenceType()).isEqualTo("REFUND");
            assertThat(item.getIssueType()).isEqualTo(ReconciliationIssueType.REFUND_SUCCESSFUL_WITHOUT_DEBIT);
            return item;
        });

        ReconciliationRun run = service.run();

        assertThat(run.getStatus()).isEqualTo(ReconciliationRunStatus.COMPLETED);
        assertThat(run.getSummary()).containsEntry("issueCount", 1);
        verify(itemRepository).save(any(ReconciliationItem.class));
    }
}

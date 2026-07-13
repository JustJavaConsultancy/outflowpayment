package net.techcrunch.outflowPayment.accounting;

import net.techcrunch.outflowPayment.observability.OperationalEventService;
import org.flowable.engine.delegate.DelegateExecution;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Service
@Transactional
public class AccountingPostingService {

    public static final String MERCHANT_OUTFLOW_DEBIT = "MERCHANT_OUTFLOW_DEBIT";
    public static final String OUTFLOW_REVERSAL = "OUTFLOW_REVERSAL";

    private final AccountingPostingRecordRepository accountingPostingRecordRepository;
    private final OperationalEventService operationalEventService;

    public AccountingPostingService(AccountingPostingRecordRepository accountingPostingRecordRepository,
                                    OperationalEventService operationalEventService) {
        this.accountingPostingRecordRepository = accountingPostingRecordRepository;
        this.operationalEventService = operationalEventService;
    }

    public Optional<Map<String, Object>> findCompletedResponse(String operation, String postingKey) {
        return accountingPostingRecordRepository.findByOperationAndPostingKey(operation, postingKey)
                .filter(record -> record.getStatus() == AccountingPostingStatus.COMPLETED)
                .map(AccountingPostingRecord::getResponsePayload);
    }

    public AccountingPostingRecord begin(String operation,
                                         String postingKey,
                                         BigDecimal amount,
                                         DelegateExecution execution,
                                         Map<String, Object> metadata) {
        AccountingPostingRecord existingRecord = accountingPostingRecordRepository
                .findByOperationAndPostingKey(operation, postingKey)
                .orElse(null);
        if (existingRecord != null) {
            operationalEventService.warning(
                    "ACCOUNTING_POSTING_DUPLICATE",
                    "ACCOUNTING_POSTING",
                    existingRecord.getTransferReference(),
                    "Duplicate accounting posting request reused existing posting record",
                    eventDetails(existingRecord)
            );
            return existingRecord;
        }

        AccountingPostingRecord record = new AccountingPostingRecord();
        record.setOperation(operation);
        record.setPostingKey(postingKey);
        record.setTransferReference(transferReference(metadata));
        record.setProcessInstanceId(execution.getProcessInstanceId());
        record.setActivityId(execution.getCurrentActivityId());
        record.setAmount(amount);
        record.setStatus(AccountingPostingStatus.IN_PROGRESS);
        record.setMetadata(metadata == null ? Map.of() : new HashMap<>(metadata));
        AccountingPostingRecord savedRecord = accountingPostingRecordRepository.save(record);
        operationalEventService.info(
                "ACCOUNTING_POSTING_STARTED",
                "ACCOUNTING_POSTING",
                savedRecord.getTransferReference(),
                "Outflow accounting posting started",
                eventDetails(savedRecord)
        );
        return savedRecord;
    }

    public AccountingPostingRecord markCompleted(AccountingPostingRecord record,
                                                 String transactionReference,
                                                 Map<String, Object> responsePayload) {
        record.setTransactionReference(transactionReference);
        record.setResponsePayload(responsePayload == null ? Map.of() : new HashMap<>(responsePayload));
        record.setStatus(AccountingPostingStatus.COMPLETED);
        AccountingPostingRecord savedRecord = accountingPostingRecordRepository.save(record);
        operationalEventService.info(
                "ACCOUNTING_POSTING_COMPLETED",
                "ACCOUNTING_POSTING",
                savedRecord.getTransferReference(),
                "Outflow accounting posting completed",
                eventDetails(savedRecord)
        );
        return savedRecord;
    }

    public AccountingPostingRecord markFailed(AccountingPostingRecord record, Map<String, Object> responsePayload) {
        record.setResponsePayload(responsePayload == null ? Map.of() : new HashMap<>(responsePayload));
        record.setStatus(AccountingPostingStatus.FAILED);
        AccountingPostingRecord savedRecord = accountingPostingRecordRepository.save(record);
        Map<String, Object> details = eventDetails(savedRecord);
        details.put("failureClass", "LEDGER_POSTING_FAILED");
        operationalEventService.error(
                "ACCOUNTING_POSTING_FAILED",
                "ACCOUNTING_POSTING",
                savedRecord.getTransferReference(),
                "Outflow accounting posting failed",
                details
        );
        return savedRecord;
    }

    public String merchantOutflowPostingKey(DelegateExecution execution) {
        Object reference = firstNonBlank(
                execution.getVariable("transferReference"),
                execution.getVariable("correlationId"),
                execution.getProcessInstanceId()
        );
        return reference + ":MERCHANT_OUTFLOW_DEBIT";
    }

    public String outflowReversalPostingKey(DelegateExecution execution) {
        Object reference = firstNonBlank(
                execution.getVariable("transferReference"),
                execution.getVariable("correlationId"),
                execution.getProcessInstanceId()
        );
        return reference + ":OUTFLOW_REVERSAL";
    }

    public String stableReference(String prefix, String postingKey) {
        return prefix + "_" + Integer.toUnsignedString(postingKey.hashCode());
    }

    private Object firstNonBlank(Object... values) {
        for (Object value : values) {
            if (value != null && !value.toString().isBlank()) {
                return value.toString().trim();
            }
        }
        throw new IllegalStateException("Unable to resolve accounting posting key");
    }

    private String transferReference(Map<String, Object> metadata) {
        if (metadata == null) {
            return null;
        }
        Object reference = metadata.get("transferReference");
        return reference == null || reference.toString().isBlank() ? null : reference.toString();
    }

    private Map<String, Object> eventDetails(AccountingPostingRecord record) {
        Map<String, Object> details = new HashMap<>();
        details.put("operation", record.getOperation());
        details.put("postingKey", record.getPostingKey());
        details.put("transferReference", record.getTransferReference());
        details.put("transactionReference", record.getTransactionReference());
        details.put("processInstanceId", record.getProcessInstanceId());
        details.put("activityId", record.getActivityId());
        details.put("amount", record.getAmount());
        details.put("status", record.getStatus().name());
        return details;
    }
}

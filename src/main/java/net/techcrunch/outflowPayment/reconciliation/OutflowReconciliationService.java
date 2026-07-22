package net.techcrunch.outflowPayment.reconciliation;

import net.techcrunch.outflowPayment.accounting.AccountingPostingRecordRepository;
import net.techcrunch.outflowPayment.accounting.AccountingPostingService;
import net.techcrunch.outflowPayment.accounting.AccountingPostingStatus;
import net.techcrunch.outflowPayment.observability.OperationalEventService;
import net.techcrunch.outflowPayment.transfer.TransferInstruction;
import net.techcrunch.outflowPayment.transfer.TransferInstructionRepository;
import net.techcrunch.outflowPayment.transfer.TransferInstructionStatus;
import net.techcrunch.outflowPayment.transfer.TransferInstructionType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

@Service
@Transactional
public class OutflowReconciliationService {

    private static final Logger log = LoggerFactory.getLogger(OutflowReconciliationService.class);
    private static final int STALE_MINUTES = 30;

    private final TransferInstructionRepository transferInstructionRepository;
    private final AccountingPostingRecordRepository accountingPostingRecordRepository;
    private final ReconciliationRunRepository reconciliationRunRepository;
    private final ReconciliationItemRepository reconciliationItemRepository;
    private final OperationalEventService operationalEventService;
    private final GenericCsvStatementParser statementParser;
    private final ReconciliationStatementImportRepository statementImportRepository;
    private final ReconciliationStatementItemRepository statementItemRepository;
    private final ReconciliationStatementRejectedItemRepository rejectedItemRepository;
    private final ReconciliationAuditEventRepository auditEventRepository;

    public OutflowReconciliationService(TransferInstructionRepository transferInstructionRepository,
                                        AccountingPostingRecordRepository accountingPostingRecordRepository,
                                        ReconciliationRunRepository reconciliationRunRepository,
                                        ReconciliationItemRepository reconciliationItemRepository,
                                        OperationalEventService operationalEventService,
                                        GenericCsvStatementParser statementParser,
                                        ReconciliationStatementImportRepository statementImportRepository,
                                        ReconciliationStatementItemRepository statementItemRepository,
                                        ReconciliationStatementRejectedItemRepository rejectedItemRepository,
                                        ReconciliationAuditEventRepository auditEventRepository) {
        this.transferInstructionRepository = transferInstructionRepository;
        this.accountingPostingRecordRepository = accountingPostingRecordRepository;
        this.reconciliationRunRepository = reconciliationRunRepository;
        this.reconciliationItemRepository = reconciliationItemRepository;
        this.operationalEventService = operationalEventService;
        this.statementParser = statementParser;
        this.statementImportRepository = statementImportRepository;
        this.statementItemRepository = statementItemRepository;
        this.rejectedItemRepository = rejectedItemRepository;
        this.auditEventRepository = auditEventRepository;
    }

    public ReconciliationStatementImportResponse importStatement(MultipartFile file,
                                                                 ReconciliationStatementSourceType sourceType,
                                                                 String sourceName,
                                                                 String profileName) {
        ReconciliationStatementImport statementImport = new ReconciliationStatementImport();
        statementImport.setSourceType(sourceType == null ? ReconciliationStatementSourceType.BANK_STATEMENT : sourceType);
        statementImport.setSourceName(blankToDefault(sourceName, "generic-bank"));
        statementImport.setProfileName(blankToDefault(profileName, "GENERIC_BANK_CSV"));
        statementImport.setOriginalFilename(file == null ? null : file.getOriginalFilename());
        statementImport.setStatus(ReconciliationStatementImportStatus.UPLOADED);
        statementImport = statementImportRepository.save(statementImport);

        try {
            if (file == null || file.isEmpty()) {
                throw new IllegalArgumentException("Statement file is required");
            }
            StatementParseResult parseResult = statementParser.parse(file);
            for (ParsedStatementRow parsedRow : parseResult.acceptedRows()) {
                statementItemRepository.save(toStatementItem(statementImport, parsedRow));
            }
            for (RejectedStatementRow rejectedRow : parseResult.rejectedRows()) {
                rejectedItemRepository.save(toRejectedItem(statementImport, rejectedRow));
            }
            statementImport.setTotalRows(parseResult.acceptedRows().size() + parseResult.rejectedRows().size());
            statementImport.setAcceptedRows(parseResult.acceptedRows().size());
            statementImport.setRejectedRows(parseResult.rejectedRows().size());
            statementImport.setStatus(parseResult.acceptedRows().isEmpty()
                    ? ReconciliationStatementImportStatus.FAILED
                    : ReconciliationStatementImportStatus.VALIDATED);
            if (parseResult.acceptedRows().isEmpty() && !parseResult.rejectedRows().isEmpty()) {
                statementImport.setFailureReason("All statement rows were rejected");
            }
            log.info("outflow_statement_import_validated importId={} sourceType={} sourceName={} acceptedRows={} rejectedRows={}",
                    statementImport.getId(), statementImport.getSourceType(), statementImport.getSourceName(),
                    parseResult.acceptedRows().size(), parseResult.rejectedRows().size());
            operationalEventService.info(
                    "OUTFLOW_STATEMENT_IMPORTED",
                    "RECONCILIATION_IMPORT",
                    String.valueOf(statementImport.getId()),
                    "Outflow bank/provider statement imported",
                    Map.of("importId", statementImport.getId(),
                            "acceptedRows", parseResult.acceptedRows().size(),
                            "rejectedRows", parseResult.rejectedRows().size())
            );
        } catch (RuntimeException exception) {
            statementImport.setStatus(ReconciliationStatementImportStatus.FAILED);
            statementImport.setFailureReason(limit(exception.getMessage()));
            log.warn("outflow_statement_import_failed importId={} sourceType={} sourceName={} error={}",
                    statementImport.getId(), statementImport.getSourceType(), statementImport.getSourceName(), exception.getMessage());
        }
        return ReconciliationStatementImportResponse.from(statementImportRepository.save(statementImport));
    }

    public ReconciliationRun run() {
        return run(null);
    }

    public ReconciliationRun run(Long importId) {
        operationalEventService.info(
                "OUTFLOW_RECONCILIATION_STARTED",
                "RECONCILIATION_RUN",
                null,
                "Outflow reconciliation run started",
                startDetails(importId)
        );
        ReconciliationRun run = new ReconciliationRun();
        run.setRunDate(LocalDate.now());
        run.setStartedAt(OffsetDateTime.now());
        run.setStatus(ReconciliationRunStatus.RUNNING);
        run = reconciliationRunRepository.save(run);

        int issueCount = reconcileInternalConsistency(run);
        int statementRows = 0;
        int matchedRows = 0;
        if (importId != null) {
            List<ReconciliationStatementItem> items = statementItemRepository.findByImportBatch_IdOrderByRowNumberAsc(importId);
            statementRows = items.size();
            for (ReconciliationStatementItem item : items) {
                if (reconcileStatementItem(run, item)) {
                    matchedRows++;
                } else {
                    issueCount++;
                }
            }
            statementImportRepository.findById(importId).ifPresent(statementImport -> {
                statementImport.setStatus(ReconciliationStatementImportStatus.PROCESSED);
                statementImportRepository.save(statementImport);
            });
        }

        run.setCompletedAt(OffsetDateTime.now());
        run.setStatus(ReconciliationRunStatus.COMPLETED);
        run.setSummary(runSummary(issueCount, statementRows, matchedRows, importId));
        ReconciliationRun savedRun = reconciliationRunRepository.save(run);
        log.info("outflow_reconciliation_run_completed runId={} importId={} issueCount={} statementRows={} matchedStatementRows={}",
                savedRun.getId(), importId, issueCount, statementRows, matchedRows);
        Map<String, Object> completedDetails = new HashMap<>();
        completedDetails.put("runId", savedRun.getId());
        completedDetails.put("issueCount", issueCount);
        completedDetails.put("statementRows", statementRows);
        operationalEventService.info(
                "OUTFLOW_RECONCILIATION_COMPLETED",
                "RECONCILIATION_RUN",
                String.valueOf(savedRun.getId()),
                "Outflow reconciliation run completed",
                completedDetails
        );
        return savedRun;
    }

    @Transactional(readOnly = true)
    public List<ReconciliationStatementImportResponse> listImports() {
        return statementImportRepository.findAllByOrderByDateCreatedDesc().stream()
                .map(ReconciliationStatementImportResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ReconciliationRun> listRuns() {
        return reconciliationRunRepository.findAllByOrderByStartedAtDesc();
    }

    @Transactional(readOnly = true)
    public List<ReconciliationItem> listOpenItems() {
        return reconciliationItemRepository.findByResolutionStatusInOrderByIdDesc(List.of(
                ReconciliationItemStatus.OPEN,
                ReconciliationItemStatus.ASSIGNED,
                ReconciliationItemStatus.IN_REVIEW,
                ReconciliationItemStatus.REOPENED
        ));
    }

    @Transactional(readOnly = true)
    public List<ReconciliationStatementItem> listStatementItems(Long importId) {
        return statementItemRepository.findByImportBatch_IdOrderByRowNumberAsc(importId);
    }

    @Transactional(readOnly = true)
    public List<ReconciliationStatementRejectedItem> listRejectedItems(Long importId) {
        return rejectedItemRepository.findByImportBatch_IdOrderByRowNumberAsc(importId);
    }

    @Transactional(readOnly = true)
    public List<ReconciliationAuditEvent> auditEvents(Long exceptionId) {
        return auditEventRepository.findByReconciliationItemIdOrderByDateCreatedAsc(exceptionId);
    }

    public ReconciliationItem assignException(Long id, AssignReconciliationItemRequest request) {
        ReconciliationItem item = exceptionItem(id);
        ReconciliationItemStatus previous = item.getResolutionStatus();
        item.setResolutionStatus(ReconciliationItemStatus.ASSIGNED);
        item.setAssignedTo(requiredText(request.assignedTo(), "assignedTo"));
        item.setAssignedAt(OffsetDateTime.now());
        touch(item, actor(request.actor()));
        ReconciliationItem saved = reconciliationItemRepository.save(item);
        audit(saved, "EXCEPTION_ASSIGNED", previous, saved.getResolutionStatus(), actor(request.actor()), request.note());
        return saved;
    }

    public ReconciliationItem addNote(Long id, ReconciliationNoteRequest request) {
        ReconciliationItem item = exceptionItem(id);
        touch(item, actor(request.actor()));
        ReconciliationItem saved = reconciliationItemRepository.save(item);
        audit(saved, "NOTE_ADDED", item.getResolutionStatus(), item.getResolutionStatus(), actor(request.actor()), requiredText(request.note(), "note"));
        return saved;
    }

    public ReconciliationItem acceptDifference(Long id, AcceptDifferenceRequest request) {
        ReconciliationItem item = exceptionItem(id);
        ReconciliationItemStatus previous = item.getResolutionStatus();
        item.setResolutionStatus(ReconciliationItemStatus.ACCEPTED_DIFFERENCE);
        item.setResolutionNote(requiredText(request.reason(), "reason"));
        item.setResolvedBy(actor(request.actor()));
        item.setResolvedAt(OffsetDateTime.now());
        touch(item, actor(request.actor()));
        ReconciliationItem saved = reconciliationItemRepository.save(item);
        audit(saved, "DIFFERENCE_ACCEPTED", previous, saved.getResolutionStatus(), actor(request.actor()), request.reason());
        return saved;
    }

    public ReconciliationItem resolveException(Long id, ResolveReconciliationItemRequest request) {
        ReconciliationItem item = exceptionItem(id);
        ReconciliationItemStatus previous = item.getResolutionStatus();
        item.setResolutionStatus(ReconciliationItemStatus.RESOLVED);
        item.setResolutionNote(requiredText(request.resolutionNote(), "resolutionNote"));
        item.setResolvedBy(actor(request.actor()));
        item.setResolvedAt(OffsetDateTime.now());
        touch(item, actor(request.actor()));
        ReconciliationItem saved = reconciliationItemRepository.save(item);
        audit(saved, "EXCEPTION_RESOLVED", previous, saved.getResolutionStatus(), actor(request.actor()), request.resolutionNote());
        return saved;
    }

    public ReconciliationItem reopenException(Long id, ReopenReconciliationItemRequest request) {
        ReconciliationItem item = exceptionItem(id);
        ReconciliationItemStatus previous = item.getResolutionStatus();
        item.setResolutionStatus(ReconciliationItemStatus.REOPENED);
        item.setReopenedBy(actor(request.actor()));
        item.setReopenedAt(OffsetDateTime.now());
        touch(item, actor(request.actor()));
        ReconciliationItem saved = reconciliationItemRepository.save(item);
        audit(saved, "EXCEPTION_REOPENED", previous, saved.getResolutionStatus(), actor(request.actor()), request.reason());
        return saved;
    }

    private Map<String, Object> startDetails(Long importId) {
        Map<String, Object> details = new HashMap<>();
        details.put("runDate", LocalDate.now());
        if (importId != null) {
            details.put("importId", importId);
        }
        return details;
    }

    private Map<String, Object> runSummary(int issueCount, int statementRows, int matchedRows, Long importId) {
        Map<String, Object> summary = new HashMap<>();
        summary.put("issueCount", issueCount);
        summary.put("statementRows", statementRows);
        summary.put("matchedStatementRows", matchedRows);
        if (importId != null) {
            summary.put("importId", importId);
        }
        return summary;
    }

    private int reconcileInternalConsistency(ReconciliationRun run) {
        List<TransferInstruction> instructions = transferInstructionRepository.findAll();
        int issueCount = 0;
        for (TransferInstruction instruction : instructions) {
            issueCount += reconcileInstruction(run, instruction);
        }
        return issueCount;
    }

    private int reconcileInstruction(ReconciliationRun run, TransferInstruction instruction) {
        int issues = 0;
        if (isStale(instruction)) {
            saveIssue(run, instruction.getReference(), referenceType(instruction), instruction.getAmount(), instruction.getAmount(),
                    "CONSISTENT", instruction.getStatus().name(), ReconciliationIssueType.STALE_TRANSFER,
                    "Transfer is still in an active state beyond the reconciliation threshold");
            issues++;
        }
        if (instruction.getStatus() == TransferInstructionStatus.COMPLETED && !hasCompletedDebit(instruction)) {
            saveIssue(run, instruction.getReference(), referenceType(instruction), instruction.getAmount(), instruction.getAmount(),
                    "CONSISTENT", instruction.getStatus().name(),
                    instructionType(instruction) == TransferInstructionType.REFUND
                            ? ReconciliationIssueType.REFUND_SUCCESSFUL_WITHOUT_DEBIT
                            : ReconciliationIssueType.COMPLETED_TRANSFER_WITHOUT_DEBIT,
                    instructionType(instruction) == TransferInstructionType.REFUND
                            ? "Refund is successful but debit posting is not completed"
                            : "Transfer is completed but debit posting is not completed");
            issues++;
        }
        if (instruction.getStatus() == TransferInstructionStatus.FAILED && !hasCompletedReversal(instruction)) {
            saveIssue(run, instruction.getReference(), referenceType(instruction), instruction.getAmount(), instruction.getAmount(),
                    "CONSISTENT", instruction.getStatus().name(),
                    instructionType(instruction) == TransferInstructionType.REFUND
                            ? ReconciliationIssueType.REFUND_FAILED_WITHOUT_REVERSAL
                            : ReconciliationIssueType.FAILED_TRANSFER_WITHOUT_REVERSAL,
                    instructionType(instruction) == TransferInstructionType.REFUND
                            ? "Refund failed but reversal posting is not completed"
                            : "Transfer failed but reversal posting is not completed");
            issues++;
        }
        if (instruction.getStatus() == TransferInstructionStatus.REVERSAL_PENDING) {
            saveIssue(run, instruction.getReference(), referenceType(instruction), instruction.getAmount(), instruction.getAmount(),
                    "CONSISTENT", instruction.getStatus().name(),
                    instructionType(instruction) == TransferInstructionType.REFUND
                            ? ReconciliationIssueType.REFUND_REVERSAL_PENDING_STALE
                            : ReconciliationIssueType.REVERSAL_PENDING_STALE,
                    instructionType(instruction) == TransferInstructionType.REFUND
                            ? "Refund reversal is pending and needs operational review"
                            : "Transfer reversal is pending and needs operational review");
            issues++;
        }
        return issues;
    }

    private boolean reconcileStatementItem(ReconciliationRun run, ReconciliationStatementItem item) {
        Optional<TransferMatchCandidate> matchedInstruction = findTransferInstruction(item);
        if (matchedInstruction.isEmpty()) {
            item.setMatchStatus(ReconciliationStatementMatchStatus.MISSING_INTERNAL);
            item.setMatchConfidence(ReconciliationMatchConfidence.NONE);
            item.setMatchStrategy("NO_MATCH");
            item.setMismatchReason("Bank/provider statement row does not match an internal transfer instruction");
            statementItemRepository.save(item);
            saveIssue(run, firstReference(item), "BANK_STATEMENT", item.getAmount(), null,
                    "INTERNAL_TRANSFER", item.getProviderStatus(),
                    ReconciliationIssueType.EXTERNAL_STATEMENT_WITHOUT_TRANSFER,
                    item.getMismatchReason());
            return false;
        }

        TransferMatchCandidate candidate = matchedInstruction.get();
        TransferInstruction instruction = candidate.instruction();
        item.setMatchedReference(instruction.getReference());
        item.setMatchedReferenceType(referenceType(instruction));
        item.setMatchConfidence(candidate.confidence());
        item.setMatchStrategy(candidate.strategy());
        item.setMatchedAt(OffsetDateTime.now());
        if (instruction.getAmount().compareTo(item.getAmount()) != 0) {
            item.setMatchStatus(ReconciliationStatementMatchStatus.AMOUNT_MISMATCH);
            item.setMismatchReason("Internal transfer amount does not match bank/provider statement amount");
            statementItemRepository.save(item);
            saveIssue(run, instruction.getReference(), referenceType(instruction), instruction.getAmount(), item.getAmount(),
                    instruction.getStatus().name(), item.getProviderStatus(), ReconciliationIssueType.TRANSFER_AMOUNT_MISMATCH,
                    item.getMismatchReason());
            return false;
        }
        if (isSuccessfulExternalStatus(item.getProviderStatus()) && instruction.getStatus() != TransferInstructionStatus.COMPLETED) {
            item.setMatchStatus(ReconciliationStatementMatchStatus.STATUS_MISMATCH);
            item.setMismatchReason("Provider/bank reports success but internal transfer is not COMPLETED");
            statementItemRepository.save(item);
            saveIssue(run, instruction.getReference(), referenceType(instruction), instruction.getAmount(), item.getAmount(),
                    "COMPLETED", instruction.getStatus().name(), ReconciliationIssueType.TRANSFER_STATUS_MISMATCH,
                    item.getMismatchReason());
            return false;
        }
        item.setMatchStatus(ReconciliationStatementMatchStatus.MATCHED);
        item.setMismatchReason(null);
        statementItemRepository.save(item);
        return true;
    }

    private Optional<TransferMatchCandidate> findTransferInstruction(ReconciliationStatementItem item) {
        String reference = firstReference(item);
        if (hasText(reference)) {
            Optional<TransferInstruction> byReference = transferInstructionRepository.findByReference(reference);
            if (byReference.isPresent()) {
                return Optional.of(new TransferMatchCandidate(byReference.get(), ReconciliationMatchConfidence.EXACT, "TRANSFER_REFERENCE"));
            }
            Optional<TransferInstruction> byRefund = transferInstructionRepository.findByRefundReference(reference);
            if (byRefund.isPresent()) {
                return Optional.of(new TransferMatchCandidate(byRefund.get(), ReconciliationMatchConfidence.EXACT, "REFUND_REFERENCE"));
            }
            Optional<TransferInstruction> byPayoutGroup = transferInstructionRepository.findByPayoutGroupReference(reference);
            if (byPayoutGroup.isPresent()) {
                return Optional.of(new TransferMatchCandidate(byPayoutGroup.get(), ReconciliationMatchConfidence.EXACT, "PAYOUT_GROUP_REFERENCE"));
            }
            Optional<TransferInstruction> bySettlement = transferInstructionRepository.findBySettlementReference(reference);
            if (bySettlement.isPresent()) {
                return Optional.of(new TransferMatchCandidate(bySettlement.get(), ReconciliationMatchConfidence.EXACT, "SETTLEMENT_REFERENCE"));
            }
        }
        Optional<TransferInstruction> narration = referenceCandidates(item.getNarration()).stream()
                .map(this::findInstructionByAnyReference)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .findFirst();
        if (narration.isPresent()) {
            return Optional.of(new TransferMatchCandidate(narration.get(), ReconciliationMatchConfidence.LOW, "NARRATION_REFERENCE"));
        }
        return transferInstructionRepository.findAll().stream()
                .filter(instruction -> instruction.getAmount().compareTo(item.getAmount()) == 0)
                .filter(instruction -> withinDateWindow(instruction.getDateCreated(), item.getTransactionDate()))
                .findFirst()
                .map(instruction -> new TransferMatchCandidate(instruction, ReconciliationMatchConfidence.MEDIUM, "AMOUNT_DATE"));
    }

    private boolean isStale(TransferInstruction instruction) {
        if (!EnumSet.of(
                TransferInstructionStatus.RECEIVED,
                TransferInstructionStatus.PROCESSING,
                TransferInstructionStatus.DEBITED,
                TransferInstructionStatus.TRANSFER_PROCESSING
        ).contains(instruction.getStatus())) {
            return false;
        }
        OffsetDateTime updatedAt = instruction.getLastUpdated() == null
                ? instruction.getDateCreated()
                : instruction.getLastUpdated();
        return updatedAt != null && updatedAt.isBefore(OffsetDateTime.now().minusMinutes(STALE_MINUTES));
    }

    private boolean hasCompletedDebit(TransferInstruction instruction) {
        return hasCompletedPosting(AccountingPostingService.MERCHANT_OUTFLOW_DEBIT,
                instruction.getReference() + ":MERCHANT_OUTFLOW_DEBIT");
    }

    private boolean hasCompletedReversal(TransferInstruction instruction) {
        return hasCompletedPosting(AccountingPostingService.OUTFLOW_REVERSAL,
                instruction.getReference() + ":OUTFLOW_REVERSAL");
    }

    private boolean hasCompletedPosting(String operation, String postingKey) {
        return accountingPostingRecordRepository.findByOperationAndPostingKey(operation, postingKey)
                .map(record -> record.getStatus() == AccountingPostingStatus.COMPLETED)
                .orElse(false);
    }

    private void saveIssue(ReconciliationRun run,
                           String reference,
                           String referenceType,
                           BigDecimal expectedAmount,
                           BigDecimal actualAmount,
                           String expectedStatus,
                           String actualStatus,
                           ReconciliationIssueType issueType,
                           String details) {
        ReconciliationItem item = new ReconciliationItem();
        item.setRun(run);
        item.setReference(reference == null ? "UNKNOWN" : reference);
        item.setReferenceType(referenceType);
        item.setExpectedAmount(expectedAmount);
        item.setActualAmount(actualAmount);
        item.setExpectedStatus(expectedStatus);
        item.setActualStatus(actualStatus);
        item.setIssueType(issueType);
        item.setResolutionStatus(ReconciliationItemStatus.OPEN);
        item.setDetails(limit(details));
        reconciliationItemRepository.save(item);
        audit(item, "ISSUE_CREATED", null, item.getResolutionStatus(), "system", details);
        operationalEventService.warning(
                "OUTFLOW_RECONCILIATION_ISSUE",
                referenceType,
                reference,
                details,
                Map.of("reference", reference == null ? "UNKNOWN" : reference, "issueType", issueType.name(), "failureClass", "RECONCILIATION_MISMATCH")
        );
    }

    private ReconciliationStatementRejectedItem toRejectedItem(ReconciliationStatementImport statementImport,
                                                               RejectedStatementRow rejectedRow) {
        ReconciliationStatementRejectedItem item = new ReconciliationStatementRejectedItem();
        item.setImportBatch(statementImport);
        item.setRowNumber(rejectedRow.rowNumber());
        item.setRawLine(limit(rejectedRow.rawLine()));
        item.setReason(limit(rejectedRow.reason()));
        item.setRawPayload(rejectedRow.rawPayload());
        return item;
    }

    private ReconciliationStatementItem toStatementItem(ReconciliationStatementImport statementImport, ParsedStatementRow parsedRow) {
        ReconciliationStatementItem item = new ReconciliationStatementItem();
        item.setImportBatch(statementImport);
        item.setRowNumber(parsedRow.rowNumber());
        item.setTransactionReference(parsedRow.transactionReference());
        item.setExternalReference(parsedRow.externalReference());
        item.setAmount(parsedRow.amount());
        item.setCurrency(parsedRow.currency());
        item.setDirection(parsedRow.direction());
        item.setTransactionDate(parsedRow.transactionDate());
        item.setValueDate(parsedRow.valueDate());
        item.setProviderStatus(parsedRow.providerStatus());
        item.setNarration(parsedRow.narration());
        item.setRawPayload(parsedRow.rawPayload());
        return item;
    }

    private boolean isSuccessfulExternalStatus(String status) {
        if (status == null || status.isBlank()) {
            return false;
        }
        String normalized = status.toUpperCase(Locale.ROOT);
        return normalized.contains("SUCCESS") || normalized.contains("COMPLETED") || normalized.contains("SETTLED");
    }

    private String referenceType(TransferInstruction instruction) {
        if (instructionType(instruction) == TransferInstructionType.REFUND) {
            return "REFUND";
        }
        if (instructionType(instruction) == TransferInstructionType.SETTLEMENT) {
            return "SETTLEMENT";
        }
        return "TRANSFER";
    }

    private TransferInstructionType instructionType(TransferInstruction instruction) {
        return instruction.getInstructionType() == null
                ? TransferInstructionType.TRANSFER
                : instruction.getInstructionType();
    }

    private String firstReference(ReconciliationStatementItem item) {
        return item.getTransactionReference() == null || item.getTransactionReference().isBlank()
                ? item.getExternalReference()
                : item.getTransactionReference();
    }

    private Optional<TransferInstruction> findInstructionByAnyReference(String reference) {
        return transferInstructionRepository.findByReference(reference)
                .or(() -> transferInstructionRepository.findByRefundReference(reference))
                .or(() -> transferInstructionRepository.findByPayoutGroupReference(reference))
                .or(() -> transferInstructionRepository.findBySettlementReference(reference));
    }

    private boolean withinDateWindow(OffsetDateTime left, OffsetDateTime right) {
        if (left == null || right == null) {
            return false;
        }
        return !left.isBefore(right.minusDays(1)) && !left.isAfter(right.plusDays(1));
    }

    private List<String> referenceCandidates(String narration) {
        if (!hasText(narration)) {
            return List.of();
        }
        return java.util.Arrays.stream(narration.split("[^A-Za-z0-9_-]+"))
                .filter(token -> token.length() >= 6)
                .toList();
    }

    private ReconciliationItem exceptionItem(Long id) {
        return reconciliationItemRepository.findById(id)
                .orElseThrow(() -> new IllegalStateException("Reconciliation exception not found: " + id));
    }

    private void touch(ReconciliationItem item, String actor) {
        item.setLastActionBy(actor);
        item.setLastActionAt(OffsetDateTime.now());
    }

    private void audit(ReconciliationItem item,
                       String eventType,
                       ReconciliationItemStatus previousStatus,
                       ReconciliationItemStatus newStatus,
                       String actor,
                       String note) {
        ReconciliationAuditEvent event = new ReconciliationAuditEvent();
        event.setReconciliationItemId(item.getId());
        event.setRunId(item.getRun() == null ? null : item.getRun().getId());
        event.setReference(item.getReference());
        event.setEventType(eventType);
        event.setActor(actor);
        event.setNote(limit(note));
        event.setPreviousStatus(previousStatus);
        event.setNewStatus(newStatus);
        event.setDetails(Map.of("referenceType", item.getReferenceType(), "issueType", item.getIssueType().name()));
        auditEventRepository.save(event);
    }

    private String actor(String actor) {
        return hasText(actor) ? actor.trim() : "system";
    }

    private String requiredText(String value, String field) {
        if (!hasText(value)) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value.trim();
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private String blankToDefault(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private String limit(String value) {
        if (value == null || value.length() <= 1000) {
            return value;
        }
        return value.substring(0, 1000);
    }

    private record TransferMatchCandidate(
            TransferInstruction instruction,
            ReconciliationMatchConfidence confidence,
            String strategy
    ) {
    }
}




package net.techcrunch.outflowPayment.reconciliation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/reconciliation/outflow")
public class OutflowReconciliationController {

    private static final Logger log = LoggerFactory.getLogger(OutflowReconciliationController.class);

    private final OutflowReconciliationService outflowReconciliationService;

    public OutflowReconciliationController(OutflowReconciliationService outflowReconciliationService) {
        this.outflowReconciliationService = outflowReconciliationService;
    }

    @PostMapping("/run")
    public ReconciliationRun run(@RequestParam(required = false) Long importId) {
        log.info("outflow_reconciliation_run_requested importId={}", importId);
        return outflowReconciliationService.run(importId);
    }

    @PostMapping(value = "/imports", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ReconciliationStatementImportResponse importStatement(@RequestParam("file") MultipartFile file,
                                                                 @RequestParam(defaultValue = "BANK_STATEMENT") ReconciliationStatementSourceType sourceType,
                                                                 @RequestParam(defaultValue = "generic-bank") String sourceName,
                                                                 @RequestParam(defaultValue = "GENERIC_BANK_CSV") String profileName) {
        log.info("outflow_statement_import_requested sourceType={} sourceName={} profileName={} fileName={} fileSize={}",
                sourceType, sourceName, profileName, file.getOriginalFilename(), file.getSize());
        return outflowReconciliationService.importStatement(file, sourceType, sourceName, profileName);
    }

    @GetMapping("/imports")
    public List<ReconciliationStatementImportResponse> imports() {
        return outflowReconciliationService.listImports();
    }

    @GetMapping("/imports/{id}/items")
    public List<ReconciliationStatementItem> importItems(@PathVariable Long id) {
        return outflowReconciliationService.listStatementItems(id);
    }

    @GetMapping("/imports/{id}/rejected-items")
    public List<ReconciliationStatementRejectedItem> rejectedItems(@PathVariable Long id) {
        return outflowReconciliationService.listRejectedItems(id);
    }

    @GetMapping("/runs")
    public List<ReconciliationRun> runs() {
        return outflowReconciliationService.listRuns();
    }

    @GetMapping("/exceptions")
    public List<ReconciliationItem> exceptions() {
        return outflowReconciliationService.listOpenItems();
    }

    @PostMapping("/exceptions/{id}/assign")
    public ReconciliationItem assignException(@PathVariable Long id,
                                              @org.springframework.web.bind.annotation.RequestBody AssignReconciliationItemRequest request) {
        return outflowReconciliationService.assignException(id, request);
    }

    @PostMapping("/exceptions/{id}/notes")
    public ReconciliationItem addNote(@PathVariable Long id,
                                      @org.springframework.web.bind.annotation.RequestBody ReconciliationNoteRequest request) {
        return outflowReconciliationService.addNote(id, request);
    }

    @PostMapping("/exceptions/{id}/accept-difference")
    public ReconciliationItem acceptDifference(@PathVariable Long id,
                                               @org.springframework.web.bind.annotation.RequestBody AcceptDifferenceRequest request) {
        return outflowReconciliationService.acceptDifference(id, request);
    }

    @PostMapping("/exceptions/{id}/resolve")
    public ReconciliationItem resolveException(@PathVariable Long id,
                                               @org.springframework.web.bind.annotation.RequestBody ResolveReconciliationItemRequest request) {
        return outflowReconciliationService.resolveException(id, request);
    }

    @PostMapping("/exceptions/{id}/reopen")
    public ReconciliationItem reopenException(@PathVariable Long id,
                                              @org.springframework.web.bind.annotation.RequestBody ReopenReconciliationItemRequest request) {
        return outflowReconciliationService.reopenException(id, request);
    }

    @GetMapping("/exceptions/{id}/audit")
    public List<ReconciliationAuditEvent> audit(@PathVariable Long id) {
        return outflowReconciliationService.auditEvents(id);
    }
}



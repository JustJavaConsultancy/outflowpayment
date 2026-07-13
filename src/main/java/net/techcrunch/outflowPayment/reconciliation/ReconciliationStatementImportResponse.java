package net.techcrunch.outflowPayment.reconciliation;

public record ReconciliationStatementImportResponse(
        Long id,
        ReconciliationStatementSourceType sourceType,
        String sourceName,
        String profileName,
        String originalFilename,
        ReconciliationStatementImportStatus status,
        int totalRows,
        int acceptedRows,
        int rejectedRows,
        String failureReason
) {
    public static ReconciliationStatementImportResponse from(ReconciliationStatementImport statementImport) {
        return new ReconciliationStatementImportResponse(
                statementImport.getId(),
                statementImport.getSourceType(),
                statementImport.getSourceName(),
                statementImport.getProfileName(),
                statementImport.getOriginalFilename(),
                statementImport.getStatus(),
                statementImport.getTotalRows(),
                statementImport.getAcceptedRows(),
                statementImport.getRejectedRows(),
                statementImport.getFailureReason()
        );
    }
}

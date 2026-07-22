package net.techcrunch.outflowPayment.reconciliation;

import java.util.List;

public record StatementParseResult(
        List<ParsedStatementRow> acceptedRows,
        List<RejectedStatementRow> rejectedRows
) {
}

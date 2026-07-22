package net.techcrunch.outflowPayment.reconciliation;

import java.util.Map;

public record RejectedStatementRow(
        int rowNumber,
        String rawLine,
        String reason,
        Map<String, Object> rawPayload
) {
}

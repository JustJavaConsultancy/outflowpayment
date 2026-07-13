package net.techcrunch.outflowPayment.reconciliation;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Map;

public record ParsedStatementRow(
        int rowNumber,
        String transactionReference,
        String externalReference,
        BigDecimal amount,
        String currency,
        ReconciliationStatementDirection direction,
        OffsetDateTime transactionDate,
        OffsetDateTime valueDate,
        String providerStatus,
        String narration,
        Map<String, Object> rawPayload
) {
}

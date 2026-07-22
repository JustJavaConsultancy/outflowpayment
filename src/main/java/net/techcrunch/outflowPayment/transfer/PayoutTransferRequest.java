package net.techcrunch.outflowPayment.transfer;

import java.math.BigDecimal;
import java.util.Map;

public record PayoutTransferRequest(
        String transferReference,
        String processInstanceId,
        String merchantId,
        BigDecimal amount,
        String beneficiaryAccount,
        String bankName,
        String narration,
        Map<String, Object> variables
) {
}

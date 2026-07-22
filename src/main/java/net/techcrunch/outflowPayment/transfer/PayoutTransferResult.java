package net.techcrunch.outflowPayment.transfer;

import java.util.Map;

public record PayoutTransferResult(
        boolean successful,
        String providerReference,
        String providerStatus,
        String responseCode,
        String message
) {

    public Map<String, Object> toMetadata() {
        return Map.of(
                "providerReference", providerReference,
                "providerStatus", providerStatus,
                "responseCode", responseCode,
                "message", message
        );
    }
}

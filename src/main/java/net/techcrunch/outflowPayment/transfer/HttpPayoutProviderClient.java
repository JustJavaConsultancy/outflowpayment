package net.techcrunch.outflowPayment.transfer;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

@Component
public class HttpPayoutProviderClient implements PayoutProviderClient {

    private final RestTemplate restTemplate;
    private final String transferUrl;
    private final String apiKey;

    public HttpPayoutProviderClient(RestTemplateBuilder restTemplateBuilder,
                                    @Value("${payouts.http.transfer-url:}") String transferUrl,
                                    @Value("${payouts.http.api-key:}") String apiKey) {
        this.restTemplate = restTemplateBuilder.build();
        this.transferUrl = transferUrl;
        this.apiKey = apiKey;
    }

    @Override
    public String providerName() {
        return "http";
    }

    @Override
    public PayoutTransferResult transfer(PayoutTransferRequest request) {
        requireConfigured();
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("reference", request.transferReference());
        payload.put("idempotencyKey", request.variables().getOrDefault("idempotencyKey", request.transferReference()));
        payload.put("merchantId", request.merchantId());
        payload.put("amount", request.amount());
        payload.put("currency", Objects.toString(request.variables().getOrDefault("currency", "NGN")));
        payload.put("beneficiaryAccount", request.beneficiaryAccount());
        payload.put("bankName", request.bankName());
        payload.put("bankCode", request.variables().get("bankCode"));
        payload.put("narration", request.narration());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);

        Map<?, ?> response = restTemplate.postForObject(transferUrl, new HttpEntity<>(payload, headers), Map.class);
        return normalize(request.transferReference(), response == null ? Map.of() : response);
    }

    private PayoutTransferResult normalize(String fallbackReference, Map<?, ?> response) {
        Object dataObject = response.get("data");
        Map<?, ?> data = dataObject instanceof Map<?, ?> dataMap ? dataMap : response;
        String status = Objects.toString(firstNonNull(data.get("status"), response.get("status"), ""), "");
        String reference = Objects.toString(firstNonNull(data.get("reference"),
                data.get("providerReference"), fallbackReference), fallbackReference);
        String code = Objects.toString(firstNonNull(data.get("responseCode"),
                successfulStatus(status) ? "00" : "05"), successfulStatus(status) ? "00" : "05");
        String message = Objects.toString(firstNonNull(response.get("message"),
                data.get("message"), status), status);
        return new PayoutTransferResult(
                successfulStatus(status),
                reference,
                status.isBlank() ? "UNKNOWN" : status.toUpperCase(),
                code,
                message
        );
    }

    private boolean successfulStatus(String status) {
        return "success".equalsIgnoreCase(status)
                || "successful".equalsIgnoreCase(status)
                || "completed".equalsIgnoreCase(status)
                || "00".equals(status);
    }

    private Object firstNonNull(Object... values) {
        for (Object value : values) {
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    private void requireConfigured() {
        if (transferUrl == null || transferUrl.isBlank()) {
            throw new IllegalStateException("HTTP payout provider selected but payouts.http.transfer-url is not configured");
        }
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("HTTP payout provider selected but payouts.http.api-key is not configured");
        }
    }
}

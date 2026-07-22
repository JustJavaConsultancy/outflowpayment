package net.techcrunch.outflowPayment.transfer;

import org.junit.jupiter.api.Test;
import org.springframework.boot.web.client.RestTemplateBuilder;

import java.math.BigDecimal;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class HttpPayoutProviderClientTest {

    @Test
    void failsWhenSelectedWithoutEndpoint() {
        HttpPayoutProviderClient client = new HttpPayoutProviderClient(new RestTemplateBuilder(), "", "secret");

        assertThatThrownBy(() -> client.transfer(request()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("transfer-url");
    }

    @Test
    void failsWhenSelectedWithoutApiKey() {
        HttpPayoutProviderClient client = new HttpPayoutProviderClient(new RestTemplateBuilder(), "https://provider.example/transfers", "");

        assertThatThrownBy(() -> client.transfer(request()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("api-key");
    }

    private PayoutTransferRequest request() {
        return new PayoutTransferRequest(
                "trf-123",
                "process-123",
                "merchant-1",
                new BigDecimal("1000"),
                "0123456789",
                "Test Bank",
                "Settlement",
                Map.of()
        );
    }
}

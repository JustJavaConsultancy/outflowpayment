package net.techcrunch.outflowPayment.transfer;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PayoutProviderRegistryTest {

    @Test
    void usesRequestedProviderWhenPresent() {
        PayoutProviderRegistry registry = new PayoutProviderRegistry(
                List.of(new NamedProvider("dev"), new NamedProvider("http")),
                "dev"
        );

        PayoutTransferResult result = registry.transfer("http", request());

        assertThat(result.providerReference()).isEqualTo("http-trf-123");
    }

    @Test
    void failsClosedForUnsupportedProvider() {
        PayoutProviderRegistry registry = new PayoutProviderRegistry(List.of(new NamedProvider("dev")), "dev");

        assertThatThrownBy(() -> registry.transfer("missing", request()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Unsupported payout provider");
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

    private record NamedProvider(String providerName) implements PayoutProviderClient {
        @Override
        public PayoutTransferResult transfer(PayoutTransferRequest request) {
            return new PayoutTransferResult(true, providerName + "-" + request.transferReference(), "COMPLETED", "00", "ok");
        }
    }
}

package net.techcrunch.outflowPayment.transfer;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class DevPayoutProviderClientTest {

    private final DevPayoutProviderClient client = new DevPayoutProviderClient();

    @Test
    void transferSucceedsByDefaultInDevelopment() {
        PayoutTransferResult result = client.transfer(request(Map.of()));

        assertThat(result.successful()).isTrue();
        assertThat(result.responseCode()).isEqualTo("00");
        assertThat(result.providerReference()).isEqualTo("devout_trf_123");
    }

    @Test
    void transferCanBeForcedToFailForDevelopmentTesting() {
        PayoutTransferResult result = client.transfer(request(Map.of("forceProviderFailure", true)));

        assertThat(result.successful()).isFalse();
        assertThat(result.responseCode()).isEqualTo("05");
        assertThat(result.providerStatus()).isEqualTo("FAILED");
    }

    private PayoutTransferRequest request(Map<String, Object> variables) {
        return new PayoutTransferRequest(
                "trf_123",
                "process-1",
                "merchant-1",
                BigDecimal.valueOf(2500),
                "0123456789",
                "Test Bank",
                "Test payout",
                variables
        );
    }
}

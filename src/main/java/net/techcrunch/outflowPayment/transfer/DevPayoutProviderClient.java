package net.techcrunch.outflowPayment.transfer;

import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
public class DevPayoutProviderClient implements PayoutProviderClient {

    @Override
    public String providerName() {
        return "dev";
    }

    @Override
    public PayoutTransferResult transfer(PayoutTransferRequest request) {
        boolean forcedFailure = Objects.equals(request.variables().get("forceProviderFailure"), true)
                || "FAILED".equalsIgnoreCase(Objects.toString(request.variables().get("providerScenario"), ""));

        String providerReference = "devout_" + request.transferReference();
        if (forcedFailure) {
            return new PayoutTransferResult(
                    false,
                    providerReference,
                    "FAILED",
                    "05",
                    "Payout transfer failed"
            );
        }

        return new PayoutTransferResult(
                true,
                providerReference,
                "COMPLETED",
                "00",
                "Successful"
        );
    }
}

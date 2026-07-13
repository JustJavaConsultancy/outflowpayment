package net.techcrunch.outflowPayment.transfer;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class PayoutProviderRegistry {

    private final Map<String, PayoutProviderClient> providers;
    private final String defaultProvider;

    public PayoutProviderRegistry(List<PayoutProviderClient> providers,
                                  @Value("${payouts.default-provider:dev}") String defaultProvider) {
        this.providers = providers.stream()
                .collect(Collectors.toMap(
                        provider -> normalize(provider.providerName()),
                        Function.identity()
                ));
        this.defaultProvider = normalize(defaultProvider);
    }

    public PayoutTransferResult transfer(String requestedProvider, PayoutTransferRequest request) {
        PayoutProviderClient provider = provider(requestedProvider);
        return provider.transfer(request);
    }

    public PayoutProviderClient provider(String requestedProvider) {
        String providerName = normalize(Objects.toString(requestedProvider, ""));
        if (providerName.isBlank()) {
            providerName = defaultProvider;
        }
        PayoutProviderClient provider = providers.get(providerName);
        if (provider == null) {
            throw new IllegalStateException("Unsupported payout provider: " + providerName);
        }
        return provider;
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}

# Payout Provider Adapters

`outflowpayment` executes bank payouts through `PayoutProviderRegistry`.

## Providers

- `dev`: local deterministic adapter for development and tests.
- `http`: configurable real-provider HTTP adapter for payout processors or bank middleware.

## Selection

Provider resolution order:

1. `payoutProvider` in transfer variables.
2. `provider` in transfer variables.
3. `payouts.default-provider`.

Unsupported providers fail closed with an error instead of falling back silently.

## Configuration

```properties
payouts.default-provider=${PAYOUTS_DEFAULT_PROVIDER:dev}
payouts.http.transfer-url=${PAYOUTS_HTTP_TRANSFER_URL:}
payouts.http.api-key=${PAYOUTS_HTTP_API_KEY:}
```

Use `PAYOUTS_DEFAULT_PROVIDER=http` only when `PAYOUTS_HTTP_TRANSFER_URL` and `PAYOUTS_HTTP_API_KEY` are configured. The HTTP payout adapter fails fast if selected without endpoint or credentials.

## HTTP Adapter Payload

The HTTP adapter posts a normalized JSON payload containing `reference`, `idempotencyKey`, `merchantId`, `amount`, `currency`, `beneficiaryAccount`, `bankName`, `bankCode`, and `narration`.

Provider responses are normalized from `status`, `reference` or `providerReference`, `responseCode`, and `message`.

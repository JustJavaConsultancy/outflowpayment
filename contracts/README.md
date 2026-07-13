# Outflow Payment Contracts

Committed REST and RabbitMQ contracts for `outflowpayment`.

- `openapi/openapi.yaml`: outflowpayment REST API contract.
- `asyncapi/asyncapi.yaml`: RabbitMQ channels consumed and published by outflowpayment.
- `schemas/messages/*.schema.json`: inbound and outbound message schemas.

Export runtime OpenAPI when the service is running:

```powershell
.\scripts\export-openapi.ps1 -BaseUrl http://localhost:9091
```

Validate local contract files:

```powershell
.\scripts\verify-contracts.ps1
```

The reconciliation contract includes V2 statement rejected-row review, exception assignment, notes, accepted differences, resolution, reopening, and audit history.

Real payout provider selection is documented in `docs/provider-adapters.md`. `dev` remains the default adapter; `http` is selectable with `PAYOUTS_DEFAULT_PROVIDER=http`, `PAYOUTS_HTTP_TRANSFER_URL`, and `PAYOUTS_HTTP_API_KEY`.

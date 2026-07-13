$ErrorActionPreference = "Stop"
$required = @(
    "contracts/openapi/openapi.yaml",
    "contracts/asyncapi/asyncapi.yaml",
    "contracts/schemas/messages/outflow-transfer-request.schema.json",
    "contracts/schemas/messages/settlement-payout-request.schema.json",
    "contracts/schemas/messages/settlement-result.schema.json"
)
foreach ($path in $required) {
    if (-not (Test-Path $path)) { throw "Missing contract artifact: $path" }
}
Get-ChildItem contracts/schemas/messages -Filter *.json | ForEach-Object { Get-Content $_.FullName -Raw | ConvertFrom-Json | Out-Null }
Write-Host "Contract files are present and JSON schemas are parseable."

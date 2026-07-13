param(
    [string]$BaseUrl = "http://localhost:9091",
    [string]$OutputPath = "contracts/openapi/openapi.yaml"
)
$ErrorActionPreference = "Stop"
New-Item -ItemType Directory -Force (Split-Path $OutputPath) | Out-Null
Invoke-WebRequest -Uri "$BaseUrl/v3/api-docs.yaml" -OutFile $OutputPath
Write-Host "Exported OpenAPI contract to $OutputPath"

#!/usr/bin/env pwsh
# Start AegisTerra backend on :8080 (requires PostGIS: docker compose up -d)
$ErrorActionPreference = "Stop"
Set-Location $PSScriptRoot\backend
& .\mvnw.cmd spring-boot:run @args

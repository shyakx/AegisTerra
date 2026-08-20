#!/usr/bin/env pwsh
# Start AegisTerra frontend on :3000
$ErrorActionPreference = "Stop"
Set-Location $PSScriptRoot\frontend
npm run dev @args

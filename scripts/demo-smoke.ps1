#Requires -Version 5.1
<#
.SYNOPSIS
  API smoke for AegisTerra 1.0
.DESCRIPTION
  Logs in as admin and hits the critical platform endpoints. Exit 1 if any check fails.
#>
param(
  [string]$BaseUrl = "http://localhost:8080",
  [string]$Username = "admin",
  [string]$Password = "Admin@1234!Aa"
)

$ErrorActionPreference = "Stop"
$fail = 0
$session = New-Object Microsoft.PowerShell.Commands.WebRequestSession

function Write-Check([string]$name, [bool]$ok, [string]$detail = "") {
  if ($ok) {
    Write-Host ("PASS  {0} {1}" -f $name, $detail)
  } else {
    Write-Host ("FAIL  {0} {1}" -f $name, $detail) -ForegroundColor Red
    $script:fail++
  }
}

function Invoke-Api([string]$method, [string]$path, [object]$body = $null) {
  $uri = "$BaseUrl$path"
  $params = @{
    Uri = $uri
    Method = $method
    WebSession = $session
    TimeoutSec = 30
    UseBasicParsing = $true
  }
  if ($null -ne $body) {
    $params.ContentType = "application/json"
    $params.Body = ($body | ConvertTo-Json -Compress)
  }
  return Invoke-WebRequest @params
}

Write-Host "AegisTerra smoke → $BaseUrl"
Write-Host ""

try {
  $health = Invoke-WebRequest -Uri "$BaseUrl/api/v1/health" -UseBasicParsing -TimeoutSec 10
  Write-Check "health" ($health.StatusCode -eq 200)
} catch {
  Write-Check "health" $false $_.Exception.Message
  Write-Host ""
  Write-Host "Backend not reachable. Start with .\start-backend.ps1" -ForegroundColor Yellow
  exit 1
}

try {
  $login = Invoke-Api POST "/api/v1/auth/login" @{ username = $Username; password = $Password }
  $loginJson = $login.Content | ConvertFrom-Json
  Write-Check "login" ($login.StatusCode -eq 200 -and $loginJson.user.username -eq $Username) "as $Username"
} catch {
  Write-Check "login" $false $_.Exception.Message
  exit 1
}

$checks = @(
  @{ Name = "executive/overview"; Path = "/api/v1/executive/overview"; Assert = {
      param($j)
      return ($j.farmers.total -ge 8 -and $j.farms.total -ge 8 -and $j.climate.stations -ge 1)
    }; Detail = { param($j) "farmers=$($j.farmers.total) farms=$($j.farms.total) stations=$($j.climate.stations) alerts=$($j.climate.openAlerts)" }
  },
  @{ Name = "farmers search FRM-2026"; Path = "/api/v1/farmers?q=FRM-2026&page=0&size=20"; Assert = {
      param($j)
      $n = if ($j.content) { $j.content.Count } elseif ($j.items) { $j.items.Count } else { 0 }
      return $n -ge 1
    }; Detail = { param($j) "rows=$($(if ($j.content) { $j.content.Count } else { 0 }))" }
  },
  @{ Name = "policies search POL-2026"; Path = "/api/v1/policies?q=POL-2026&page=0&size=20"; Assert = {
      param($j)
      return ($j.content -and $j.content.Count -ge 1)
    }; Detail = { param($j) "rows=$($j.content.Count)" }
  },
  @{ Name = "claims search CLM-2026"; Path = "/api/v1/claims?q=CLM-2026&page=0&size=20"; Assert = {
      param($j)
      return ($j.content -and $j.content.Count -ge 1)
    }; Detail = { param($j) "rows=$($j.content.Count)" }
  },
  @{ Name = "settlements"; Path = "/api/v1/settlements?q=SET-2026&page=0&size=20"; Assert = {
      param($j)
      return ($j.content -and $j.content.Count -ge 1)
    }; Detail = { param($j) "rows=$($j.content.Count)" }
  },
  @{ Name = "climate stations map"; Path = "/api/v1/climate/map/stations"; Assert = {
      param($j)
      return ($j.features -and $j.features.Count -ge 1)
    }; Detail = { param($j) "features=$($j.features.Count)" }
  },
  @{ Name = "climate-intel national"; Path = "/api/v1/climate-intel/national/dashboard"; Assert = {
      param($j)
      return ($null -ne $j)
    }; Detail = { param($j) "ok" }
  },
  @{ Name = "climate alerts OPEN"; Path = "/api/v1/climate-intel/alerts?status=OPEN&page=0&size=10"; Assert = {
      param($j)
      return ($j.content -and $j.content.Count -ge 1)
    }; Detail = { param($j) "rows=$($j.content.Count)" }
  },
  @{ Name = "notifications unread"; Path = "/api/v1/notifications/unread-count"; Assert = {
      param($j)
      return ($j.count -ge 0)
    }; Detail = { param($j) "count=$($j.count)" }
  }
)

foreach ($c in $checks) {
  try {
    $resp = Invoke-Api GET $c.Path
    $json = $resp.Content | ConvertFrom-Json
    $ok = & $c.Assert $json
    $detail = & $c.Detail $json
    Write-Check $c.Name $ok $detail
  } catch {
    Write-Check $c.Name $false $_.Exception.Message
  }
}

Write-Host ""
if ($fail -gt 0) {
  Write-Host "RESULT: $fail check(s) failed" -ForegroundColor Red
  exit 1
}
Write-Host "RESULT: all checks passed"
exit 0

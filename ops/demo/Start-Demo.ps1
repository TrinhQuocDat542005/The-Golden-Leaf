param([ValidateSet(8080, 18082)][int]$Port = 8080)
$ErrorActionPreference = 'Stop'
Set-Location -LiteralPath $PSScriptRoot
& "$PSScriptRoot/Verify-Demo.ps1"
if (-not (Get-Command java -ErrorAction SilentlyContinue)) { throw 'Install JDK 17 first; java must be on PATH.' }
Write-Host "Local portfolio only: http://127.0.0.1:$Port/demo.html — do not transfer money. Ctrl+C stops the demo."
& java -jar golden-leaf-demo.jar --spring.profiles.active=demo "--server.port=$Port"
if ($LASTEXITCODE -ne 0) { throw "Demo exited ($LASTEXITCODE). Check JDK 17, port availability and startup log." }

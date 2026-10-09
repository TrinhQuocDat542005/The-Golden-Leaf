$ErrorActionPreference = 'Stop'
$bundlePath = $PSScriptRoot
$expected = @('golden-leaf-demo.jar', 'golden-leaf-demo.apk', 'README.md', 'REVISION', 'manifest.json', 'SECURITY.md', 'security-exceptions.json', 'Start-Demo.ps1', 'Start-Demo.sh', 'Verify-Demo.ps1')
$seen = @()
foreach ($line in Get-Content -LiteralPath "$bundlePath/SHA256SUMS") {
    if ($line -notmatch '^([a-f0-9]{64})  ([A-Za-z0-9.-]+)$') { throw 'Invalid checksum entry' }
    $digest = $Matches[1]; $fileName = $Matches[2]
    if ($fileName -notin $expected -or $fileName -in $seen) { throw 'Unexpected/duplicate checksum filename' }
    $seen += $fileName
    if ((Get-FileHash -LiteralPath "$bundlePath/$fileName" -Algorithm SHA256).Hash.ToLowerInvariant() -ne $digest) {
        throw "Checksum mismatch: $fileName. Re-download the trusted release; do not run it."
    }
}
if ($seen.Count -ne $expected.Count) { throw 'Incomplete checksum manifest' }
foreach ($exception in Get-Content -Raw -LiteralPath "$bundlePath/security-exceptions.json" | ConvertFrom-Json) {
    if ([DateTimeOffset]::UtcNow -ge [DateTimeOffset]$exception.expires) {
        throw 'Security exception expired. Obtain a newly reviewed release before starting this bundle.'
    }
}
Write-Host 'Checksums and security review window verified. Checksums do not authenticate the publisher.'

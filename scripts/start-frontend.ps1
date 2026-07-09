. "$PSScriptRoot\common.ps1"

$tools = Initialize-ProjectTools
$frontendDir = Join-Path $ProjectRoot "frontend"
$logPath = Join-Path $frontendDir "frontend-run.log"
$errPath = Join-Path $frontendDir "frontend-run.err.log"

Set-Location $frontendDir
"Starting frontend with NodeBin=$($tools.NodeBin)" | Out-File -FilePath $logPath -Encoding utf8
"" | Out-File -FilePath $errPath -Encoding utf8

if (-not (Test-Path -LiteralPath (Join-Path $frontendDir "node_modules"))) {
    & $tools.NpmCommand install 2>&1 |
        ForEach-Object { $_.ToString() } |
        Out-File -FilePath $logPath -Encoding utf8 -Append
    if ($LASTEXITCODE -ne 0) {
        exit $LASTEXITCODE
    }
}

& $tools.NpmCommand run dev 2>&1 |
    ForEach-Object { $_.ToString() } |
    Out-File -FilePath $logPath -Encoding utf8 -Append

exit $LASTEXITCODE

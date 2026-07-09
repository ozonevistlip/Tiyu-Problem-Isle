. "$PSScriptRoot\common.ps1"

$tools = Initialize-ProjectTools
$backendDir = Join-Path $ProjectRoot "backend"
$logPath = Join-Path $backendDir "backend-run.log"
$errPath = Join-Path $backendDir "backend-run.err.log"

Set-Location $backendDir
"Starting backend with JAVA_HOME=$($tools.JavaHome)" | Out-File -FilePath $logPath -Encoding utf8
"" | Out-File -FilePath $errPath -Encoding utf8

& $tools.MavenCommand spring-boot:run 2>&1 |
    ForEach-Object { $_.ToString() } |
    Out-File -FilePath $logPath -Encoding utf8 -Append

exit $LASTEXITCODE

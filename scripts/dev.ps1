param(
    [switch]$SkipRedis
)

. "$PSScriptRoot\common.ps1"

$tools = Initialize-ProjectTools

Write-Host "Using JAVA_HOME: $($tools.JavaHome)"
Write-Host "Using Maven: $($tools.MavenCommand)"
Write-Host "Using npm: $($tools.NpmCommand)"

if (-not $SkipRedis) {
    $redisStartScript = Get-ProjectConfigValue -Name "ProjectRedisStartScript"
    $redisCli = Get-ProjectConfigValue -Name "ProjectRedisCli"

    if (-not (Test-ProjectPortListening -Port 6379)) {
        if (-not [string]::IsNullOrWhiteSpace($redisStartScript) -and (Test-Path -LiteralPath $redisStartScript)) {
            Write-Host "Starting Redis..."
            & powershell.exe -NoProfile -ExecutionPolicy Bypass -File $redisStartScript
            Start-Sleep -Seconds 2
        } else {
            Write-Warning "Redis is not listening on 6379 and ProjectRedisStartScript is not configured."
        }
    }

    if (-not [string]::IsNullOrWhiteSpace($redisCli) -and (Test-Path -LiteralPath $redisCli)) {
        $pong = & $redisCli -h 127.0.0.1 -p 6379 PING 2>$null
        if ($pong -eq "PONG") {
            Write-Host "Redis: PONG"
        } else {
            Write-Warning "Redis did not return PONG. Backend can start, but judging queue may not work."
        }
    }
}

$backendScript = Join-Path $PSScriptRoot "start-backend.ps1"
$frontendScript = Join-Path $PSScriptRoot "start-frontend.ps1"

if (Test-ProjectPortListening -Port 8080) {
    Write-Host "Backend port 8080 is already listening; skipped starting backend."
} else {
    $backend = Start-Process -FilePath "powershell.exe" `
        -ArgumentList @("-NoProfile", "-ExecutionPolicy", "Bypass", "-File", $backendScript) `
        -WorkingDirectory $ProjectRoot `
        -WindowStyle Hidden `
        -PassThru
    Write-ProjectPid -Name "backend" -ProcessId $backend.Id
    Write-Host "Backend starting, PID $($backend.Id). Log: backend\backend-run.log"
}

if (Test-ProjectPortListening -Port 5173) {
    Write-Host "Frontend port 5173 is already listening; skipped starting frontend."
} else {
    $frontend = Start-Process -FilePath "powershell.exe" `
        -ArgumentList @("-NoProfile", "-ExecutionPolicy", "Bypass", "-File", $frontendScript) `
        -WorkingDirectory $ProjectRoot `
        -WindowStyle Hidden `
        -PassThru
    Write-ProjectPid -Name "frontend" -ProcessId $frontend.Id
    Write-Host "Frontend starting, PID $($frontend.Id). Log: frontend\frontend-run.log"
}

Write-Host ""
Write-Host "Backend:  http://localhost:8080"
Write-Host "Frontend: http://127.0.0.1:5173"
Write-Host "Stop with: powershell -ExecutionPolicy Bypass -File .\scripts\stop-dev.ps1"

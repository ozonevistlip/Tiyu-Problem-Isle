. "$PSScriptRoot\common.ps1"

function Test-ProjectProcessFamily {
    param(
        [Parameter(Mandatory = $true)]
        [int]$ProcessId
    )

    $currentPid = $ProcessId
    while ($currentPid -gt 0) {
        $processInfo = Get-CimInstance Win32_Process -Filter "ProcessId = $currentPid" -ErrorAction SilentlyContinue
        if ($null -eq $processInfo) {
            return $false
        }

        if ($processInfo.CommandLine -like "*$ProjectRoot*") {
            return $true
        }

        $currentPid = [int]$processInfo.ParentProcessId
    }

    return $false
}

function Stop-ProcessTree {
    param(
        [Parameter(Mandatory = $true)]
        [int]$RootProcessId,
        [Parameter(Mandatory = $true)]
        [string]$Label,
        [switch]$RequireProjectCommandLine
    )

    $processInfo = Get-CimInstance Win32_Process -Filter "ProcessId = $RootProcessId" -ErrorAction SilentlyContinue
    if ($null -eq $processInfo) {
        Write-Host "${Label}: process $RootProcessId is not running."
        return
    }

    if ($RequireProjectCommandLine -and -not (Test-ProjectProcessFamily -ProcessId $RootProcessId)) {
        Write-Warning "${Label}: skipped process $RootProcessId because it does not look like this project."
        return
    }

    $children = Get-CimInstance Win32_Process -Filter "ParentProcessId = $RootProcessId" -ErrorAction SilentlyContinue
    foreach ($child in $children) {
        Stop-ProcessTree -RootProcessId $child.ProcessId -Label $Label
    }

    Stop-Process -Id $RootProcessId -Force -ErrorAction SilentlyContinue
    Write-Host "${Label}: stopped process $RootProcessId."
}

foreach ($name in @("backend", "frontend")) {
    $storedPid = Get-ProjectPid -Name $name
    if ($null -eq $storedPid) {
        Write-Host "${name}: no pid file."
        continue
    }

    Stop-ProcessTree -RootProcessId $storedPid -Label $name
    Clear-ProjectPid -Name $name
}

$ports = @(
    @{ Name = "backend port 8080"; Port = 8080 },
    @{ Name = "frontend port 5173"; Port = 5173 }
)

foreach ($entry in $ports) {
    $owners = Get-NetTCPConnection -LocalPort $entry.Port -State Listen -ErrorAction SilentlyContinue |
        Select-Object -ExpandProperty OwningProcess -Unique

    foreach ($owner in $owners) {
        Stop-ProcessTree -RootProcessId $owner -Label $entry.Name -RequireProjectCommandLine
    }
}

Set-StrictMode -Version Latest

$ProjectRoot = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
$ProjectLocalEnvPath = Join-Path $PSScriptRoot "local.env.ps1"
$ProjectLocalEnvExamplePath = Join-Path $PSScriptRoot "local.env.example.ps1"

if (Test-Path -LiteralPath $ProjectLocalEnvPath) {
    . $ProjectLocalEnvPath
}

function Get-ProjectConfigValue {
    param(
        [Parameter(Mandatory = $true)]
        [string]$Name,
        [string]$Fallback = ""
    )

    $variable = Get-Variable -Name $Name -Scope Script -ErrorAction SilentlyContinue
    if ($null -ne $variable -and -not [string]::IsNullOrWhiteSpace([string]$variable.Value)) {
        return [string]$variable.Value
    }

    return $Fallback
}

function Add-ProjectPathPrefix {
    param(
        [Parameter(Mandatory = $true)]
        [string[]]$Paths
    )

    $validPaths = @()
    foreach ($path in $Paths) {
        if (-not [string]::IsNullOrWhiteSpace($path) -and (Test-Path -LiteralPath $path)) {
            $validPaths += (Resolve-Path -LiteralPath $path).Path
        }
    }

    if ($validPaths.Count -gt 0) {
        $env:Path = ($validPaths + @($env:Path)) -join ";"
    }
}

function Initialize-ProjectTools {
    $javaHome = Get-ProjectConfigValue -Name "ProjectJavaHome" -Fallback $env:JAVA_HOME
    if ([string]::IsNullOrWhiteSpace($javaHome)) {
        throw "ProjectJavaHome is not set. Copy scripts\local.env.example.ps1 to scripts\local.env.ps1 and set JDK 17."
    }

    $javaExe = Join-Path $javaHome "bin\java.exe"
    if (-not (Test-Path -LiteralPath $javaExe)) {
        throw "JDK java.exe was not found: $javaExe"
    }

    $mavenBin = Get-ProjectConfigValue -Name "ProjectMavenBin"
    $mvnCommand = ""
    if (-not [string]::IsNullOrWhiteSpace($mavenBin)) {
        $candidate = Join-Path $mavenBin "mvn.cmd"
        if (-not (Test-Path -LiteralPath $candidate)) {
            throw "Maven mvn.cmd was not found: $candidate"
        }
        $mvnCommand = $candidate
    } else {
        $command = Get-Command "mvn.cmd" -ErrorAction SilentlyContinue
        if ($null -eq $command) {
            throw "Maven was not found. Set ProjectMavenBin in scripts\local.env.ps1."
        }
        $mvnCommand = $command.Source
        $mavenBin = Split-Path -Parent $mvnCommand
    }

    $nodeBin = Get-ProjectConfigValue -Name "ProjectNodeBin"
    $npmCommand = ""
    if (-not [string]::IsNullOrWhiteSpace($nodeBin)) {
        $candidate = Join-Path $nodeBin "npm.cmd"
        if (-not (Test-Path -LiteralPath $candidate)) {
            throw "npm.cmd was not found: $candidate"
        }
        $npmCommand = $candidate
    } else {
        $command = Get-Command "npm.cmd" -ErrorAction SilentlyContinue
        if ($null -eq $command) {
            throw "npm was not found. Set ProjectNodeBin in scripts\local.env.ps1."
        }
        $npmCommand = $command.Source
        $nodeBin = Split-Path -Parent $npmCommand
    }

    $env:JAVA_HOME = (Resolve-Path -LiteralPath $javaHome).Path
    Add-ProjectPathPrefix -Paths @(
        (Join-Path $env:JAVA_HOME "bin"),
        $mavenBin,
        $nodeBin
    )

    return @{
        JavaHome = $env:JAVA_HOME
        JavaExe = $javaExe
        MavenCommand = $mvnCommand
        NpmCommand = $npmCommand
        NodeBin = $nodeBin
    }
}

function Test-ProjectPortListening {
    param(
        [Parameter(Mandatory = $true)]
        [int]$Port
    )

    $connection = Get-NetTCPConnection -LocalPort $Port -State Listen -ErrorAction SilentlyContinue |
        Select-Object -First 1
    return $null -ne $connection
}

function Write-ProjectPid {
    param(
        [Parameter(Mandatory = $true)]
        [string]$Name,
        [Parameter(Mandatory = $true)]
        [int]$ProcessId
    )

    $tmpDir = Join-Path $ProjectRoot ".tmp"
    New-Item -ItemType Directory -Force -Path $tmpDir | Out-Null
    Set-Content -Path (Join-Path $tmpDir "$Name.pid") -Value $ProcessId
}

function Get-ProjectPid {
    param(
        [Parameter(Mandatory = $true)]
        [string]$Name
    )

    $pidPath = Join-Path $ProjectRoot ".tmp\$Name.pid"
    if (-not (Test-Path -LiteralPath $pidPath)) {
        return $null
    }

    $raw = Get-Content -Path $pidPath -ErrorAction SilentlyContinue | Select-Object -First 1
    $value = 0
    if ([int]::TryParse($raw, [ref]$value)) {
        return $value
    }

    return $null
}

function Clear-ProjectPid {
    param(
        [Parameter(Mandatory = $true)]
        [string]$Name
    )

    $pidPath = Join-Path $ProjectRoot ".tmp\$Name.pid"
    if (Test-Path -LiteralPath $pidPath) {
        Remove-Item -LiteralPath $pidPath -Force
    }
}

param(
    [string]$MavenCommand = "mvn",
    [string]$RepoRoot = "",
    [string]$MavenRepoLocal = "",
    [string[]]$Suites = @(
        "CostAuthorizationManualIT",
        "CostPublishControllerManualIT",
        "CostRunControllerManualIT",
        "CostGovernanceControllerManualIT"
    ),
    [string]$MysqlHost = "127.0.0.1",
    [int]$MysqlPort = 3306,
    [string]$MysqlUser = "root",
    [string]$MysqlPassword = "",
    [string]$Database = "cost_platform",
    [string]$MysqlContainer = "",
    [switch]$UseDockerMysql,
    [switch]$SkipTargetVerify,
    [string]$OutputDir = ""
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

if ([string]::IsNullOrWhiteSpace($RepoRoot)) {
    $RepoRoot = (Resolve-Path (Join-Path $PSScriptRoot "..\..")).Path
}
$BackendRoot = if (Test-Path (Join-Path $RepoRoot "cost_platform_back\pom.xml")) {
    Join-Path $RepoRoot "cost_platform_back"
}
else {
    $RepoRoot
}
if ([string]::IsNullOrWhiteSpace($OutputDir)) {
    $OutputDir = Join-Path $RepoRoot ("release-regression-evidence\" + (Get-Date).ToString("yyyyMMdd-HHmmss"))
}

New-Item -ItemType Directory -Force -Path $OutputDir | Out-Null
$script:StepIndex = 0
$script:HasFailure = $false
$summaryItems = New-Object System.Collections.Generic.List[object]

function ConvertTo-Slug {
    param([string]$Value)
    $slug = $Value.ToLowerInvariant() -replace "[^a-z0-9]+", "-"
    return $slug.Trim("-")
}

function Resolve-Application {
    param([string]$Name)
    if (Test-Path -LiteralPath $Name) {
        return (Resolve-Path -LiteralPath $Name).Path
    }
    $command = @((Get-Command $Name -CommandType Application -ErrorAction Stop))[0]
    return $command.Source
}

function Resolve-PowerShellExecutable {
    foreach ($name in @("pwsh", "powershell")) {
        $command = Get-Command $name -CommandType Application -ErrorAction SilentlyContinue
        if ($command) {
            return @($command)[0].Source
        }
    }
    throw "PowerShell executable not found."
}

function Mask-Arguments {
    param([string[]]$Arguments)

    $masked = New-Object System.Collections.Generic.List[string]
    for ($i = 0; $i -lt $Arguments.Count; $i++) {
        $argument = $Arguments[$i]
        $masked.Add($argument)
        if ($argument -eq "-MysqlPassword" -and $i + 1 -lt $Arguments.Count) {
            $i++
            $masked.Add("******")
        }
    }
    return $masked.ToArray()
}

function Invoke-NativeStep {
    param(
        [string]$Name,
        [string]$Command,
        [string[]]$Arguments,
        [string]$WorkingDirectory
    )

    $script:StepIndex++
    $logPath = Join-Path $OutputDir ("{0:D2}-{1}.log" -f $script:StepIndex, (ConvertTo-Slug $Name))
    $startedAt = Get-Date
    $exitCode = 0
    $lines = @()
    $resolvedCommand = Resolve-Application -Name $Command

    Push-Location $WorkingDirectory
    $previousErrorActionPreference = $ErrorActionPreference
    try {
        $ErrorActionPreference = "Continue"
        $lines = & $resolvedCommand @Arguments 2>&1 | ForEach-Object { $_.ToString() }
        $exitCode = $LASTEXITCODE
    }
    catch {
        $lines += $_.Exception.Message
        $exitCode = 1
    }
    finally {
        $ErrorActionPreference = $previousErrorActionPreference
        Pop-Location
    }

    $finishedAt = Get-Date
    $lines | Set-Content -Path $logPath -Encoding UTF8
    $passed = $exitCode -eq 0
    if (-not $passed) {
        $script:HasFailure = $true
    }
    $summaryItems.Add([ordered]@{
        name = $Name
        status = if ($passed) { "PASSED" } else { "FAILED" }
        exitCode = $exitCode
        startedAt = $startedAt.ToString("yyyy-MM-dd HH:mm:ss zzz")
        finishedAt = $finishedAt.ToString("yyyy-MM-dd HH:mm:ss zzz")
        durationSeconds = [Math]::Round(($finishedAt - $startedAt).TotalSeconds, 2)
        workingDirectory = $WorkingDirectory
        command = $resolvedCommand
        arguments = (Mask-Arguments -Arguments $Arguments)
        logPath = $logPath
    }) | Out-Null
}

if (-not $SkipTargetVerify.IsPresent) {
    $verifyScript = Join-Path $RepoRoot "cost_platform_back\bin\verify-target-release.ps1"
    $verifyOutputDir = Join-Path $OutputDir "target-release"
    $verifyArgs = @(
        "-NoProfile",
        "-ExecutionPolicy", "Bypass",
        "-File", $verifyScript,
        "-MysqlHost", $MysqlHost,
        "-MysqlPort", [string]$MysqlPort,
        "-MysqlUser", $MysqlUser,
        "-Database", $Database,
        "-OutputDir", $verifyOutputDir
    )
    if ($MysqlPassword) {
        $verifyArgs += @("-MysqlPassword", $MysqlPassword)
    }
    if ($MysqlContainer) {
        $verifyArgs += @("-MysqlContainer", $MysqlContainer)
    }
    if ($UseDockerMysql.IsPresent) {
        $verifyArgs += "-UseDockerMysql"
    }
    Invoke-NativeStep -Name "Target Release Verification" -Command (Resolve-PowerShellExecutable) -Arguments $verifyArgs -WorkingDirectory $RepoRoot
}

if ($Suites.Count -gt 0) {
    $mvnArgs = @(
        "-q",
        "-pl", "cost_admin",
        "-am",
        "-Dtest=$($Suites -join ',')",
        "-DfailIfNoTests=false",
        "-Dsurefire.failIfNoSpecifiedTests=false",
        "test"
    )
    if ($MavenRepoLocal) {
        $mvnArgs = @("-Dmaven.repo.local=$MavenRepoLocal") + $mvnArgs
    }
    Invoke-NativeStep -Name "Backend Manual Integration Tests" -Command $MavenCommand -Arguments $mvnArgs -WorkingDirectory $BackendRoot
}

$status = if ($script:HasFailure) { "FAILED" } else { "PASSED" }
$generatedAt = (Get-Date).ToString("yyyy-MM-dd HH:mm:ss zzz")
$summaryPath = Join-Path $OutputDir "summary.md"
$jsonPath = Join-Path $OutputDir "summary.json"

$markdown = New-Object System.Collections.Generic.List[string]
$markdown.Add("# Go-Live Regression Evidence")
$markdown.Add("")
$markdown.Add("- GeneratedAt: $generatedAt")
$markdown.Add("- Status: $status")
$markdown.Add("- BackendRoot: $BackendRoot")
$markdown.Add("- Database: $MysqlHost`:$MysqlPort/$Database")
if ($MysqlContainer) {
    $markdown.Add("- MysqlContainer: $MysqlContainer")
}
$markdown.Add("- Suites: $($Suites -join ', ')")
$markdown.Add("")
$markdown.Add("| Step | Status | ExitCode | Duration(s) | Log |")
$markdown.Add("| --- | --- | ---: | ---: | --- |")
foreach ($item in $summaryItems) {
    $markdown.Add("| $($item.name) | $($item.status) | $($item.exitCode) | $($item.durationSeconds) | ``$($item.logPath)`` |")
}
$markdown.Add("")
$markdown.Add("## Notes")
$markdown.Add("")
$markdown.Add("- This script produces repeatable API/database evidence for release regression.")
$markdown.Add("- Page screenshots, target environment performance reports, and executor sign-off still need to be attached to the formal regression workbook.")

$payload = [ordered]@{
    generatedAt = $generatedAt
    status = $status
    backendRoot = $BackendRoot
    outputDir = $OutputDir
    database = "$MysqlHost`:$MysqlPort/$Database"
    mysqlContainer = $MysqlContainer
    suites = $Suites
    steps = $summaryItems
}

$markdown | Set-Content -Path $summaryPath -Encoding UTF8
Set-Content -Path $jsonPath -Value ($payload | ConvertTo-Json -Depth 10) -Encoding UTF8

Write-Host "Go-live regression summary: $summaryPath"
Write-Host "Go-live regression json: $jsonPath"
Write-Host "Status: $status"
if ($script:HasFailure) {
    exit 1
}

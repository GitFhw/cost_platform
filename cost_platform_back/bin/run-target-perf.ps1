param(
    [string]$MavenCommand = "mvn",
    [string]$RepoRoot = "",
    [string]$MavenRepoLocal = "",
    [string]$SceneCode = "SHOUGANG-ORE-HR-001",
    [long]$SceneId = 0,
    [long]$VersionId = 0,
    [string]$BillMonth = "",
    [int[]]$SingleRecordCounts = @(500000, 1000000),
    [string[]]$ConcurrentMatrices = @("4x10000", "8x10000", "4x10000@same"),
    [switch]$SkipConcurrent,
    [int]$SoakRounds = 1,
    [long]$TimeoutMs = 7200000,
    [string]$OutputDir = ""
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

if ([string]::IsNullOrWhiteSpace($RepoRoot)) {
    $RepoRoot = (Resolve-Path (Join-Path $PSScriptRoot "..\..")).Path
}
$BackendRoot = if (Test-Path (Join-Path $RepoRoot "cost_platform_back\\pom.xml")) {
    Join-Path $RepoRoot "cost_platform_back"
}
else {
    $RepoRoot
}
if ([string]::IsNullOrWhiteSpace($MavenRepoLocal)) {
    $MavenRepoLocal = Join-Path $RepoRoot ".m2-manual"
}
if ([string]::IsNullOrWhiteSpace($BillMonth)) {
    $BillMonth = (Get-Date).AddMonths(6).ToString("yyyy-MM")
}
if ([string]::IsNullOrWhiteSpace($OutputDir)) {
    $OutputDir = Join-Path $RepoRoot ("perf-target-env\" + (Get-Date).ToString("yyyyMMdd-HHmmss"))
}

New-Item -ItemType Directory -Force -Path $OutputDir | Out-Null
$summaryItems = New-Object System.Collections.Generic.List[object]

function Save-JsonFile {
    param(
        [Parameter(Mandatory = $true)][string]$Path,
        [Parameter(Mandatory = $true)]$Value
    )
    $json = $Value | ConvertTo-Json -Depth 20
    Set-Content -Path $Path -Value $json -Encoding UTF8
}

function Parse-PerfPayload {
    param(
        [Parameter(Mandatory = $true)][string[]]$Lines,
        [Parameter(Mandatory = $true)][string]$Prefix
    )
    $line = $Lines | Where-Object { $_ -like "$Prefix*" } | Select-Object -Last 1
    if ([string]::IsNullOrWhiteSpace($line)) {
        throw "Missing $Prefix output. Check the scenario log."
    }
    $payload = $line.Substring($Prefix.Length)
    return $payload | ConvertFrom-Json
}

function Invoke-PerfScenario {
    param(
        [Parameter(Mandatory = $true)][string]$Label,
        [Parameter(Mandatory = $true)][int]$RecordCount,
        [int]$ConcurrentTasks = 1,
        [switch]$SameBillMonth
    )

    $requestPrefix = "TARGETPERF-" + $Label.ToUpperInvariant().Replace("_", "-")
    $logPath = Join-Path $OutputDir ($Label + ".log")
    $jsonPath = Join-Path $OutputDir ($Label + ".json")

    $mvnArgs = @(
        "-q",
        "-pl", "cost_admin",
        "-am",
        "-Dmaven.repo.local=$MavenRepoLocal",
        "-Dtest=CostRunPerformanceManualIT",
        "-Dcost.perf.enabled=true",
        "-Dcost.perf.recordCount=$RecordCount",
        "-Dcost.perf.billMonth=$BillMonth",
        "-Dcost.perf.timeoutMs=$TimeoutMs",
        "-Dcost.perf.scenarioLabel=$Label",
        "-Dcost.perf.requestNoPrefix=$requestPrefix",
        "-DfailIfNoTests=false",
        "-Dsurefire.failIfNoSpecifiedTests=false",
        "test"
    )
    if ($SceneId -gt 0) {
        $mvnArgs += "-Dcost.perf.sceneId=$SceneId"
    }
    else {
        $mvnArgs += "-Dcost.perf.sceneCode=$SceneCode"
    }
    if ($VersionId -gt 0) {
        $mvnArgs += "-Dcost.perf.versionId=$VersionId"
    }
    if ($ConcurrentTasks -gt 1) {
        $mvnArgs += "-Dcost.perf.concurrentTasks=$ConcurrentTasks"
    }
    if ($SameBillMonth.IsPresent) {
        $mvnArgs += "-Dcost.perf.sameBillMonth=true"
    }

    Write-Host ""
    Write-Host "==== Running scenario: $Label ====" -ForegroundColor Cyan
    Write-Host "$MavenCommand $($mvnArgs -join ' ')"
    $mavenExecutable = @((Get-Command $MavenCommand -CommandType Application -ErrorAction Stop))[0].Source
    $previousErrorActionPreference = $ErrorActionPreference
    Push-Location $BackendRoot
    try {
        $ErrorActionPreference = "Continue"
        $lines = & $mavenExecutable @mvnArgs 2>&1 | ForEach-Object { $_.ToString() }
        $exitCode = $LASTEXITCODE
    }
    finally {
        Pop-Location
        $ErrorActionPreference = $previousErrorActionPreference
    }
    $lines | Set-Content -Path $logPath -Encoding UTF8
    if ($exitCode -ne 0) {
        throw "Scenario $Label failed. See $logPath"
    }

    $prefix = if ($ConcurrentTasks -gt 1) {
        "COST_PERF_CONCURRENT_BASELINE="
    }
    else {
        "COST_PERF_BASELINE="
    }
    $payload = Parse-PerfPayload -Lines $lines -Prefix $prefix
    Save-JsonFile -Path $jsonPath -Value $payload

    $summaryItems.Add([pscustomobject]@{
        label = $Label
        concurrentTasks = $ConcurrentTasks
        sameBillMonth = [bool]$SameBillMonth
        recordCount = $RecordCount
        jsonPath = $jsonPath
        logPath = $logPath
        payload = $payload
    }) | Out-Null
}

foreach ($recordCount in $SingleRecordCounts) {
    Invoke-PerfScenario -Label ("single_{0}" -f $recordCount) -RecordCount $recordCount
}

if (-not $SkipConcurrent.IsPresent) {
    foreach ($matrix in $ConcurrentMatrices) {
        if ([string]::IsNullOrWhiteSpace($matrix)) {
            continue
        }
        $parts = $matrix.Split("@", 2)
        $core = $parts[0]
        $sameBillMonth = $parts.Length -gt 1 -and $parts[1].Trim().ToLowerInvariant() -eq "same"
        if ($core -notmatch "^(?<tasks>\d+)x(?<records>\d+)$") {
            throw "Invalid concurrent matrix: $matrix. Use 4x10000 or 4x10000@same."
        }
        $tasks = [int]$Matches["tasks"]
        $records = [int]$Matches["records"]
        for ($round = 1; $round -le [Math]::Max($SoakRounds, 1); $round++) {
            $label = if ($sameBillMonth) {
                "concurrent_same_{0}x{1}_r{2}" -f $tasks, $records, $round
            }
            else {
                "concurrent_multi_{0}x{1}_r{2}" -f $tasks, $records, $round
            }
            Invoke-PerfScenario -Label $label -RecordCount $records -ConcurrentTasks $tasks -SameBillMonth:$sameBillMonth
        }
    }
}

$summaryMarkdown = @()
$summaryMarkdown += "# Target Environment Performance Results"
$summaryMarkdown += ""
$summaryMarkdown += "| Scenario | Concurrent Tasks | Records Per Task | Total Records | Wall Clock (ms) | Throughput (records/s) | JSON Output | Log Output |"
$summaryMarkdown += "| --- | ---: | ---: | ---: | ---: | ---: | --- | --- |"
foreach ($item in $summaryItems) {
    $payload = $item.payload
    $totalRecordCount = if ($item.concurrentTasks -gt 1) {
        [int]$payload.totalRecordCount
    }
    else {
        [int]$payload.recordCount
    }
    $wallClockMs = if ($item.concurrentTasks -gt 1) {
        [long]$payload.wallClockMs
    }
    else {
        [long]$payload.taskDurationMs
    }
    $throughput = if ($item.concurrentTasks -gt 1) {
        [double]$payload.aggregateRecordsPerSecond
    }
    else {
        [double]$payload.recordsPerSecond
    }
    $summaryMarkdown += "| {0} | {1} | {2} | {3} | {4} | {5} | `{6}` | `{7}` |" -f `
        $item.label, `
        $item.concurrentTasks, `
        $item.recordCount, `
        $totalRecordCount, `
        $wallClockMs, `
        $throughput, `
        (Split-Path -Leaf $item.jsonPath), `
        (Split-Path -Leaf $item.logPath)
}

$summaryMarkdown += ""
$summaryMarkdown += "## Runtime Parameters"
$summaryMarkdown += ""
$summaryMarkdown += "- SceneCode: $SceneCode"
$summaryMarkdown += "- SceneId: $SceneId"
$summaryMarkdown += "- VersionId: $VersionId"
$summaryMarkdown += "- BillMonth: $BillMonth"
$summaryMarkdown += "- MavenRepoLocal: $MavenRepoLocal"
$summaryMarkdown += "- SoakRounds: $SoakRounds"
$summaryMarkdown += "- TimeoutMs: $TimeoutMs"

$summaryPath = Join-Path $OutputDir "summary.md"
$summaryMarkdown | Set-Content -Path $summaryPath -Encoding UTF8

Write-Host ""
Write-Host "Performance run completed. Output directory: $OutputDir" -ForegroundColor Green
Write-Host "Summary file: $summaryPath" -ForegroundColor Green

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
    [string]$Executor = "",
    [string]$EnvironmentName = "",
    [string]$BuildVersion = "",
    [string]$AttachmentDir = "",
    [string]$EvidenceNote = "",
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

function ConvertTo-RelativeEvidencePath {
    param(
        [string]$Root,
        [string]$Path
    )

    $rootPath = [System.IO.Path]::GetFullPath($Root)
    $filePath = [System.IO.Path]::GetFullPath($Path)
    if (-not $rootPath.EndsWith([System.IO.Path]::DirectorySeparatorChar)) {
        $rootPath += [System.IO.Path]::DirectorySeparatorChar
    }
    $rootUri = [System.Uri]::new($rootPath)
    $fileUri = [System.Uri]::new($filePath)
    return [System.Uri]::UnescapeDataString($rootUri.MakeRelativeUri($fileUri).ToString()).Replace("/", [System.IO.Path]::DirectorySeparatorChar)
}

function Get-ManualRegressionCases {
    return @(
        [ordered]@{ code = "GLR-001"; name = "Select target work scene and verify status, bill month, and default version"; evidence = "scene page screenshot, top work-context screenshot, scene detail or default version screenshot" },
        [ordered]@{ code = "GLR-002"; name = "Create one formal publish version and verify version number, publisher, publish time, and snapshot summary"; evidence = "publish success screenshot, version detail screenshot, publish log or API response" },
        [ordered]@{ code = "GLR-003"; name = "Verify variables, rules, price configuration, and dependencies are included in publish snapshot"; evidence = "snapshot detail screenshot, fee variable rule summary screenshot" },
        [ordered]@{ code = "GLR-004"; name = "Submit one formal calculation task through JSON input and generate result"; evidence = "task submit screenshot, successful task screenshot, result ledger screenshot" },
        [ordered]@{ code = "GLR-005"; name = "Submit batch JSON input and verify task details, result ledger, and trace ledger"; evidence = "batch task detail screenshot, detail statistics screenshot, trace detail screenshot" },
        [ordered]@{ code = "GLR-006"; name = "Create import batch and verify batch ledger summary, sample detail, and status"; evidence = "batch list screenshot, batch detail screenshot, sample item screenshot" },
        [ordered]@{ code = "GLR-007"; name = "Submit formal calculation task from import batch and verify sourceBatchNo"; evidence = "batch refill entry screenshot, task detail sourceBatchNo screenshot, result ledger screenshot" },
        [ordered]@{ code = "GLR-008"; name = "Submit enough data to create multiple partitions and verify partition list, range, duration, and success or failure counts"; evidence = "partition monitor screenshot, partition statistics screenshot" },
        [ordered]@{ code = "GLR-009"; name = "Create at least one failed detail and verify failure summary and top error reason"; evidence = "failed task detail screenshot, failure summary screenshot, error detail sample" },
        [ordered]@{ code = "GLR-010"; name = "Retry one failed partition and verify partition status, task statistics, and task detail refresh"; evidence = "before and after partition status screenshot, task statistics screenshot, operation log" },
        [ordered]@{ code = "GLR-015"; name = "Filter result by scene, bill month, and task, then verify amount, object identity, and runtime version"; evidence = "result ledger filter screenshot, amount and object sample screenshot" },
        [ordered]@{ code = "GLR-016"; name = "Open result detail and trace detail, then verify hit rule, variable snapshot, and explanation chain"; evidence = "result detail screenshot, trace chain screenshot, variable snapshot screenshot" },
        [ordered]@{ code = "GLR-017"; name = "Verify import-batch task and JSON task share the same result-ledger query semantics"; evidence = "two task type filter screenshots, result field comparison screenshot" },
        [ordered]@{ code = "GLR-018"; name = "Verify admin can see import batch, formal calculation, result ledger, and alarm center menus"; evidence = "admin menu screenshot, user role screenshot" },
        [ordered]@{ code = "GLR-019"; name = "Verify unauthorized normal role cannot see restricted menus or operation buttons"; evidence = "normal role menu screenshot, hidden button screenshot, API access-denied log" },
        [ordered]@{ code = "GLR-021"; name = "Verify target environment database initialization, menus, dictionaries, permissions, and seed data"; evidence = "verify-target-release report, target database SQL evidence, initialization log" }
    )
}

function Resolve-ManualCaseAttachments {
    param(
        [string]$EvidenceAttachmentDir,
        [array]$ManualCases
    )

    $result = @{}
    foreach ($case in $ManualCases) {
        $result[$case.code] = @()
    }
    if ([string]::IsNullOrWhiteSpace($EvidenceAttachmentDir) -or -not (Test-Path -LiteralPath $EvidenceAttachmentDir)) {
        return $result
    }

    $resolvedAttachmentDir = (Resolve-Path -LiteralPath $EvidenceAttachmentDir).Path
    $files = Get-ChildItem -LiteralPath $resolvedAttachmentDir -File -Recurse -ErrorAction SilentlyContinue
    foreach ($case in $ManualCases) {
        $matched = @($files | Where-Object { $_.Name -match [regex]::Escape($case.code) } | Select-Object -First 20)
        $result[$case.code] = @($matched | ForEach-Object { ConvertTo-RelativeEvidencePath -Root $resolvedAttachmentDir -Path $_.FullName })
    }
    return $result
}

function Write-ManualEvidenceChecklist {
    param(
        [string]$Directory,
        [string]$GeneratedAt,
        [array]$ManualCases,
        [hashtable]$Attachments
    )

    $markdownPath = Join-Path $Directory "manual-evidence-checklist.md"
    $jsonPath = Join-Path $Directory "manual-evidence-checklist.json"
    $attachmentLabel = if ($AttachmentDir) { $AttachmentDir } else { "not provided" }
    $manualItems = New-Object System.Collections.Generic.List[object]

    $lines = New-Object System.Collections.Generic.List[string]
    $lines.Add("# Manual Regression Evidence Checklist")
    $lines.Add("")
    $lines.Add("- GeneratedAt: $GeneratedAt")
    $lines.Add("- Executor: $(if ($Executor) { $Executor } else { 'to be filled' })")
    $lines.Add("- Environment: $(if ($EnvironmentName) { $EnvironmentName } else { 'to be filled' })")
    $lines.Add("- BuildVersion: $(if ($BuildVersion) { $BuildVersion } else { 'to be filled' })")
    $lines.Add("- AttachmentDir: $attachmentLabel")
    if ($EvidenceNote) {
        $lines.Add("- EvidenceNote: $EvidenceNote")
    }
    $lines.Add("")
    $lines.Add("| Case | Manual Result | Required Evidence | Matched Attachments |")
    $lines.Add("| --- | --- | --- | --- |")
    foreach ($case in $ManualCases) {
        $matchedAttachments = @($Attachments[$case.code])
        $attachmentText = if ($matchedAttachments.Count -gt 0) { ($matchedAttachments -join "<br>") } else { "to be attached" }
        $lines.Add("| $($case.code) $($case.name) | to be filled | $($case.evidence) | $attachmentText |")
        $manualItems.Add([ordered]@{
            code = $case.code
            name = $case.name
            manualResult = "to be filled"
            requiredEvidence = $case.evidence
            matchedAttachments = $matchedAttachments
        }) | Out-Null
    }
    $lines.Add("")
    $lines.Add("## Fill Rules")
    $lines.Add("")
    $lines.Add("- Attach at least one screenshot, log, SQL evidence, or API evidence for every `GLR-*` case.")
    $lines.Add("- When a defect is found, record defect id, owner action, and retest result in the formal workbook.")
    $lines.Add("- The version can be marked production-ready only after all blocking P0 cases pass.")

    $payload = [ordered]@{
        generatedAt = $GeneratedAt
        executor = $Executor
        environmentName = $EnvironmentName
        buildVersion = $BuildVersion
        attachmentDir = $AttachmentDir
        evidenceNote = $EvidenceNote
        cases = $manualItems
    }

    $lines | Set-Content -Path $markdownPath -Encoding UTF8
    Set-Content -Path $jsonPath -Value ($payload | ConvertTo-Json -Depth 8) -Encoding UTF8
    return [ordered]@{
        markdownPath = $markdownPath
        jsonPath = $jsonPath
    }
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
$manualCases = @(Get-ManualRegressionCases)
$manualAttachments = Resolve-ManualCaseAttachments -EvidenceAttachmentDir $AttachmentDir -ManualCases $manualCases
$manualEvidence = Write-ManualEvidenceChecklist -Directory $OutputDir -GeneratedAt $generatedAt -ManualCases $manualCases -Attachments $manualAttachments

$markdown = New-Object System.Collections.Generic.List[string]
$markdown.Add("# Go-Live Regression Evidence")
$markdown.Add("")
$markdown.Add("- GeneratedAt: $generatedAt")
$markdown.Add("- Status: $status")
$markdown.Add("- Executor: $(if ($Executor) { $Executor } else { 'to be filled' })")
$markdown.Add("- Environment: $(if ($EnvironmentName) { $EnvironmentName } else { 'to be filled' })")
$markdown.Add("- BuildVersion: $(if ($BuildVersion) { $BuildVersion } else { 'to be filled' })")
$markdown.Add("- BackendRoot: $BackendRoot")
$markdown.Add("- Database: $MysqlHost`:$MysqlPort/$Database")
if ($MysqlContainer) {
    $markdown.Add("- MysqlContainer: $MysqlContainer")
}
$markdown.Add("- Suites: $($Suites -join ', ')")
$markdown.Add("- ManualEvidenceChecklist: ``$($manualEvidence.markdownPath)``")
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
$markdown.Add("- Manual page screenshots and sign-off should be attached against ``manual-evidence-checklist.md`` and then copied into the formal regression workbook.")
$markdown.Add("- Target environment performance reports still need to be attached separately when closing PERF-006.")

$payload = [ordered]@{
    generatedAt = $generatedAt
    status = $status
    backendRoot = $BackendRoot
    outputDir = $OutputDir
    database = "$MysqlHost`:$MysqlPort/$Database"
    mysqlContainer = $MysqlContainer
    executor = $Executor
    environmentName = $EnvironmentName
    buildVersion = $BuildVersion
    evidenceNote = $EvidenceNote
    manualEvidenceChecklist = $manualEvidence
    suites = $Suites
    steps = $summaryItems
}

$markdown | Set-Content -Path $summaryPath -Encoding UTF8
Set-Content -Path $jsonPath -Value ($payload | ConvertTo-Json -Depth 10) -Encoding UTF8

Write-Host "Go-live regression summary: $summaryPath"
Write-Host "Go-live regression json: $jsonPath"
Write-Host "Manual evidence checklist: $($manualEvidence.markdownPath)"
Write-Host "Status: $status"
if ($script:HasFailure) {
    exit 1
}

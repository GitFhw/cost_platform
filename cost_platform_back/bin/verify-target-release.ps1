param(
    [string]$MysqlHost = "127.0.0.1",
    [int]$MysqlPort = 3306,
    [string]$MysqlUser = "root",
    [string]$MysqlPassword = "",
    [string]$Database = "cost_platform",
    [string]$OutputDir = ""
)

$ErrorActionPreference = "Stop"

function Resolve-ProjectRoot {
    $scriptDir = Split-Path -Parent $PSCommandPath
    return (Resolve-Path (Join-Path $scriptDir "..\..")).Path
}

function Invoke-MysqlScalar {
    param([string]$Sql)

    $previousPassword = $env:MYSQL_PWD
    try {
        if ($MysqlPassword) {
            $env:MYSQL_PWD = $MysqlPassword
        }
        $args = @(
            "--host=$MysqlHost",
            "--port=$MysqlPort",
            "--user=$MysqlUser",
            "--database=$Database",
            "--batch",
            "--raw",
            "--skip-column-names",
            "--execute=$Sql"
        )
        $result = & mysql @args
        if ($LASTEXITCODE -ne 0) {
            throw "mysql exited with code $LASTEXITCODE"
        }
        return ($result -join "`n").Trim()
    } finally {
        $env:MYSQL_PWD = $previousPassword
    }
}

function Add-Check {
    param(
        [System.Collections.Generic.List[object]]$Checks,
        [string]$Code,
        [string]$Name,
        [string]$Sql,
        [scriptblock]$PassWhen,
        [string]$Expected
    )

    $value = Invoke-MysqlScalar -Sql $Sql
    $passed = & $PassWhen $value
    $Checks.Add([ordered]@{
        code = $Code
        name = $Name
        passed = [bool]$passed
        expected = $Expected
        actual = $value
        sql = $Sql
    })
}

function Write-Report {
    param(
        [System.Collections.Generic.List[object]]$Checks,
        [string]$MarkdownPath,
        [string]$JsonPath
    )

    $failed = @($Checks | Where-Object { -not $_.passed })
    $status = if ($failed.Count -eq 0) { "PASSED" } else { "FAILED" }
    $generatedAt = (Get-Date).ToString("yyyy-MM-dd HH:mm:ss zzz")

    $lines = New-Object System.Collections.Generic.List[string]
    $lines.Add("# Target Release Verification Report")
    $lines.Add("")
    $lines.Add("- GeneratedAt: $generatedAt")
    $lines.Add("- Database: $MysqlHost`:$MysqlPort/$Database")
    $lines.Add("- Status: $status")
    $lines.Add("")
    $lines.Add("| Code | Check | Expected | Actual | Result |")
    $lines.Add("| --- | --- | --- | --- | --- |")
    foreach ($check in $Checks) {
        $result = if ($check.passed) { "PASS" } else { "FAIL" }
        $actual = [string]$check.actual
        $actual = $actual.Replace("|", "\|").Replace("`r", " ").Replace("`n", "<br>")
        $lines.Add("| $($check.code) | $($check.name) | $($check.expected) | $actual | $result |")
    }
    $lines.Add("")
    $lines.Add("## Failed Checks")
    if ($failed.Count -eq 0) {
        $lines.Add("")
        $lines.Add("None.")
    } else {
        foreach ($check in $failed) {
            $lines.Add("")
            $lines.Add("- $($check.code) $($check.name): expected $($check.expected), actual $($check.actual)")
        }
    }

    $payload = [ordered]@{
        generatedAt = $generatedAt
        database = "$MysqlHost`:$MysqlPort/$Database"
        status = $status
        checks = $Checks
    }

    Set-Content -Path $MarkdownPath -Value $lines -Encoding UTF8
    Set-Content -Path $JsonPath -Value ($payload | ConvertTo-Json -Depth 8) -Encoding UTF8
    return $status
}

$mysqlCommand = Get-Command mysql -ErrorAction SilentlyContinue
if (-not $mysqlCommand) {
    throw "mysql command not found. Install MySQL client or add mysql.exe to PATH."
}

$projectRoot = Resolve-ProjectRoot
if (-not $OutputDir) {
    $OutputDir = Join-Path $projectRoot "target-release-evidence"
}
New-Item -ItemType Directory -Force -Path $OutputDir | Out-Null

$timestamp = Get-Date -Format "yyyyMMdd-HHmmss"
$markdownPath = Join-Path $OutputDir "target-release-verification-$timestamp.md"
$jsonPath = Join-Path $OutputDir "target-release-verification-$timestamp.json"
$checks = New-Object System.Collections.Generic.List[object]

Add-Check $checks "REL-002-TABLES" "core table count" `
    "select count(*) from information_schema.tables where table_schema = database() and table_type = 'BASE TABLE';" `
    { param($value) [int]$value -ge 55 } ">= 55"

Add-Check $checks "REL-002-COST-TABLES" "cost domain table count" `
    "select count(*) from information_schema.tables where table_schema = database() and table_name like 'cost\_%';" `
    { param($value) [int]$value -ge 30 } ">= 30"

Add-Check $checks "TASK-014-REQUEST-NO" "unique request_no index" `
    "select count(*) from information_schema.statistics where table_schema = database() and table_name = 'cost_calc_task' and column_name = 'request_no' and non_unique = 0;" `
    { param($value) [int]$value -ge 1 } ">= 1"

Add-Check $checks "AUTH-001-MENUS" "core cost menu permissions" `
    "select count(*) from sys_menu where perms in ('cost:access:list','cost:batch:list','cost:task:list','cost:result:list','cost:alarm:list','cost:result:export');" `
    { param($value) [int]$value -ge 6 } ">= 6"

Add-Check $checks "AUTH-001-ADMIN" "admin role core permissions" `
    "select count(*) from sys_role_menu rm join sys_menu m on rm.menu_id = m.menu_id join sys_role r on rm.role_id = r.role_id where r.role_key = 'admin' and m.perms in ('cost:access:list','cost:batch:list','cost:task:list','cost:result:list','cost:alarm:list','cost:result:export');" `
    { param($value) [int]$value -ge 6 } ">= 6"

Add-Check $checks "AUTH-003-BUTTONS" "governance button permissions" `
    "select count(*) from sys_menu where perms in ('cost:task:cancel','cost:task:retry','cost:alarm:ack','cost:alarm:resolve','cost:cache:refresh');" `
    { param($value) [int]$value -ge 5 } ">= 5"

Add-Check $checks "BASE-002-DICTS" "cost dictionary types" `
    "select count(*) from sys_dict_type where dict_type like 'cost\_%';" `
    { param($value) [int]$value -ge 10 } ">= 10"

Add-Check $checks "BASE-002-SCENE" "seed scene data" `
    "select count(*) from cost_scene;" `
    { param($value) [int]$value -ge 1 } ">= 1"

$status = Write-Report -Checks $checks -MarkdownPath $markdownPath -JsonPath $jsonPath
Write-Host "Verification report: $markdownPath"
Write-Host "Verification json: $jsonPath"
Write-Host "Status: $status"
if ($status -ne "PASSED") {
    exit 1
}

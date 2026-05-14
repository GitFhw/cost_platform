param(
    [string]$MysqlHost = "127.0.0.1",
    [int]$MysqlPort = 3306,
    [string]$MysqlUser = "root",
    [string]$MysqlPassword = "",
    [string]$Database = "cost_platform",
    [string]$MysqlContainer = "",
    [switch]$UseDockerMysql,
    [string]$OutputDir = ""
)

$ErrorActionPreference = "Stop"
$script:MysqlClientMode = "local"

function Resolve-ProjectRoot {
    $scriptDir = Split-Path -Parent $PSCommandPath
    return (Resolve-Path (Join-Path $scriptDir "..\..")).Path
}

function Test-CommandExists {
    param([string]$Name)
    return [bool](Get-Command $Name -ErrorAction SilentlyContinue)
}

function Resolve-DockerMysqlContainer {
    if ($MysqlContainer) {
        return
    }

    $candidates = @(& docker ps --format "{{.Names}}" | Where-Object { $_ -match "mysql" })
    if ($candidates.Count -eq 0) {
        throw "docker mysql container not found. Pass -MysqlContainer or install MySQL client."
    }
    if ($candidates -contains "mysql-dev") {
        $script:MysqlContainer = "mysql-dev"
        return
    }
    if ($candidates.Count -eq 1) {
        $script:MysqlContainer = $candidates[0]
        return
    }

    throw "multiple mysql containers found: $($candidates -join ', '). Pass -MysqlContainer explicitly."
}

function Resolve-MysqlClient {
    if ($MysqlContainer -or $UseDockerMysql) {
        if (-not (Test-CommandExists "docker")) {
            throw "docker command not found. Install MySQL client or add docker.exe to PATH."
        }
        Resolve-DockerMysqlContainer
        $script:MysqlClientMode = "docker"
        return
    }

    if (Test-CommandExists "mysql") {
        $script:MysqlClientMode = "local"
        return
    }

    if (Test-CommandExists "docker") {
        Resolve-DockerMysqlContainer
        $script:MysqlClientMode = "docker"
        return
    }

    throw "mysql command not found. Install MySQL client, add mysql.exe to PATH, or pass -MysqlContainer."
}

function Invoke-MysqlScalar {
    param([string]$Sql)

    $previousPassword = $env:MYSQL_PWD
    try {
        if ($MysqlPassword) {
            $env:MYSQL_PWD = $MysqlPassword
        }
        $mysqlArgs = @(
            "--host=$MysqlHost",
            "--port=$MysqlPort",
            "--user=$MysqlUser",
            "--database=$Database",
            "--batch",
            "--raw",
            "--skip-column-names",
            "--execute=$Sql"
        )
        if ($script:MysqlClientMode -eq "docker") {
            $dockerArgs = @("exec")
            if ($MysqlPassword) {
                $dockerArgs += @("-e", "MYSQL_PWD=$MysqlPassword")
            }
            $dockerArgs += @($MysqlContainer, "mysql")
            $dockerArgs += $mysqlArgs
            $result = & docker @dockerArgs
        } else {
            $result = & mysql @mysqlArgs
        }
        if ($LASTEXITCODE -ne 0) {
            throw "mysql exited with code $LASTEXITCODE"
        }
        return ($result -join "`n").Trim()
    } finally {
        $env:MYSQL_PWD = $previousPassword
    }
}

function Resolve-DatabaseLabel {
    if ($script:MysqlClientMode -eq "docker") {
        return "docker:$MysqlContainer/$Database"
    }
    return "$MysqlHost`:$MysqlPort/$Database"
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
    $databaseLabel = Resolve-DatabaseLabel
    $lines.Add("- Database: $databaseLabel")
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
        database = $databaseLabel
        status = $status
        checks = $Checks
    }

    Set-Content -Path $MarkdownPath -Value $lines -Encoding UTF8
    Set-Content -Path $JsonPath -Value ($payload | ConvertTo-Json -Depth 8) -Encoding UTF8
    return $status
}

Resolve-MysqlClient

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
    "select concat(count(t.table_name), '/', count(r.table_name)) from (select 'cost_access_profile' table_name union all select 'cost_alarm_record' union all select 'cost_audit_log' union all select 'cost_bill_period' union all select 'cost_calc_input_batch' union all select 'cost_calc_input_batch_item' union all select 'cost_calc_task' union all select 'cost_calc_task_detail' union all select 'cost_calc_task_partition' union all select 'cost_fee_item' union all select 'cost_fee_variable_rel' union all select 'cost_formula' union all select 'cost_formula_version' union all select 'cost_open_app' union all select 'cost_publish_snapshot' union all select 'cost_publish_version' union all select 'cost_recalc_order' union all select 'cost_result_ledger' union all select 'cost_result_trace' union all select 'cost_rule' union all select 'cost_rule_condition' union all select 'cost_rule_tier' union all select 'cost_scene' union all select 'cost_simulation_record' union all select 'cost_variable' union all select 'cost_variable_group') r left join information_schema.tables t on t.table_schema = database() and t.table_name = r.table_name;" `
    { param($value) $parts = $value -split '/'; [int]$parts[0] -eq [int]$parts[1] -and [int]$parts[1] -ge 26 } "all required cost tables"

Add-Check $checks "TASK-014-REQUEST-NO" "unique request_no index" `
    "select count(distinct index_name) from information_schema.statistics where table_schema = database() and table_name = 'cost_calc_task' and index_name = 'uk_cost_calc_task_request_no' and non_unique = 0 and column_name in ('scene_id','version_id','bill_month','request_no_key') group by index_name having count(distinct column_name) = 4;" `
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

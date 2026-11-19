#Requires -Version 7.0
<#
.SYNOPSIS
    CEAMS BPM 会签节点 50 并发压测脚本（PowerShell 7）。

.DESCRIPTION
    验证同一会签任务在高并发重复提交下的幂等与一致性：
      1. 管理员登录并绑定设备，获取 HMAC 签名密钥；
      2. 幂等创建测试审批用户、创建并发布「start -> COUNTERSIGN -> end」测试模板；
      3. 管理员发起实例，管理员与测试用户分别拉取各自待办；
      4. 管理员正常同意自己的任务，测试用户对同一任务并发提交 N 次 approve（含相同 taskVersion）；
      5. 断言实例最终恰好 APPROVED 一次、节点无重复流转、任务无丢失/无重复生成。

    签名约定（高危写接口：模板发布 / 实例终止 / 任务改派）：
      X-Device-Id / X-Timestamp(epoch 毫秒) / X-Nonce / X-Sign
      X-Sign = HMAC-SHA256(secret, timestamp + nonce + sha256hex(原始请求体))

.PARAMETER BaseUrl
    服务地址，默认 http://localhost:8080

.PARAMETER AdminUser / AdminPassword
    管理员账号，默认 admin / admin123

.PARAMETER TestUsername / TestPassword
    测试审批用户，不存在时由脚本以管理员身份创建

.PARAMETER Concurrency
    并发提交次数，默认 50

.NOTES
    运行前提：服务已启动、数据库已迁移到 V20；本脚本成功时退出码 0，失败时退出码 1。
    示例：pwsh -File scripts/bpm-concurrency-test.ps1 -BaseUrl http://localhost:8080
#>

[CmdletBinding()]
param(
    [string]$BaseUrl = 'http://localhost:8080',
    [string]$AdminUser = 'admin',
    [string]$AdminPassword = 'admin123',
    [string]$TestUsername = 'bpm_press_user',
    [string]$TestPassword = 'bpm_press123',
    [string]$TemplateCode = 'BPM_PRESS_COUNTERSIGN',
    [string]$BizType = 'EXPENSE',
    [int]$Concurrency = 50,
    [int]$TimeoutSec = 60
)

$ErrorActionPreference = 'Stop'
$ProgressPreference = 'SilentlyContinue'
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8
$OutputEncoding = [System.Text.Encoding]::UTF8

$script:Stats = [ordered]@{
    Success     = 0
    Conflict    = 0
    ClientError = 0
    ServerError = 0
    Other       = 0
}

$script:Failures = [System.Collections.Generic.List[string]]::new()

# ----------------------------------------------------------------------
# 基础工具
# ----------------------------------------------------------------------
function Write-Step([string]$Message) {
    Write-Host "`n==> $Message" -ForegroundColor Cyan
}

function Write-Pass([string]$Message) {
    Write-Host "[PASS] $Message" -ForegroundColor Green
}

function Write-Fail([string]$Message) {
    Write-Host "[FAIL] $Message" -ForegroundColor Red
}

function Assert-True([bool]$Condition, [string]$Message) {
    if (-not $Condition) {
        throw $Message
    }
    Write-Pass $Message
}

function Add-Stat([int]$Status, [int]$Code, [string]$Message) {
    if ($Status -ge 500) {
        $script:Stats.ServerError++
    } elseif ($Code -eq 409011 -or ($Message -and $Message -like '*BPM_TASK_CONFLICT*')) {
        $script:Stats.Conflict++
    } elseif ($Status -ge 400) {
        $script:Stats.ClientError++
    } elseif ($Code -eq 0) {
        $script:Stats.Success++
    } else {
        $script:Stats.Other++
    }
}

function Get-Sha256Hex([string]$Text) {
    $bytes = [System.Text.Encoding]::UTF8.GetBytes($Text)
    $hash = [System.Security.Cryptography.SHA256]::HashData($bytes)
    return ([System.BitConverter]::ToString($hash) -replace '-', '').ToLowerInvariant()
}

function Get-HmacSha256Hex([string]$Payload, [string]$Secret) {
    $hmac = [System.Security.Cryptography.HMACSHA256]::new(
        [System.Text.Encoding]::UTF8.GetBytes($Secret))
    try {
        $raw = $hmac.ComputeHash([System.Text.Encoding]::UTF8.GetBytes($Payload))
        return ([System.BitConverter]::ToString($raw) -replace '-', '').ToLowerInvariant()
    } finally {
        $hmac.Dispose()
    }
}

# ----------------------------------------------------------------------
# 统一请求封装：带分类统计，失败不抛异常，便于压测统计 4xx/5xx
# ----------------------------------------------------------------------
function Invoke-Api {
    param(
        [Parameter(Mandatory = $true)][string]$Method,
        [Parameter(Mandatory = $true)][string]$Path,
        $Body,
        [string]$Token,
        [string]$DeviceId,
        [string]$Secret,
        [switch]$Signed
    )

    $bodyText = $null
    if ($null -ne $Body) {
        if ($Body -is [string]) {
            $bodyText = $Body
        } else {
            $bodyText = ($Body | ConvertTo-Json -Depth 12 -Compress)
        }
    }

    $headers = @{ Accept = 'application/json' }
    if ($Token) {
        $headers['Authorization'] = "Bearer $Token"
    }
    if ($Signed) {
        if (-not $Secret) {
            throw '签名请求缺少 secret，请先调用 /api/v1/auth/sign-secret'
        }
        $timestamp = [string][DateTimeOffset]::UtcNow.ToUnixTimeMilliseconds()
        $nonce = [Guid]::NewGuid().ToString('N')
        $bodyHash = if ($null -eq $bodyText -or $bodyText.Length -eq 0) { '' } else { Get-Sha256Hex $bodyText }
        $sign = Get-HmacSha256Hex ($timestamp + $nonce + $bodyHash) $Secret
        $headers['X-Device-Id'] = $DeviceId
        $headers['X-Timestamp'] = $timestamp
        $headers['X-Nonce'] = $nonce
        $headers['X-Sign'] = $sign
    }

    $invokeParams = @{
        Uri               = "$BaseUrl$Path"
        Method            = $Method
        Headers           = $headers
        SkipHttpErrorCheck = $true
        TimeoutSec        = $TimeoutSec
    }
    if ($null -ne $bodyText) {
        $invokeParams['ContentType'] = 'application/json; charset=utf-8'
        $invokeParams['Body'] = $bodyText
    }

    $stopwatch = [System.Diagnostics.Stopwatch]::StartNew()
    $response = Invoke-WebRequest @invokeParams
    $stopwatch.Stop()

    $status = [int]$response.StatusCode
    $raw = [string]$response.Content
    $json = $null
    try {
        if ($raw) { $json = $raw | ConvertFrom-Json }
    } catch {
        $json = $null
    }

    $code = if ($null -ne $json -and $null -ne $json.code) { [int]$json.code } else { -1 }
    $message = if ($null -ne $json) { [string]$json.message } else { '' }
    Add-Stat $status $code $message

    return [pscustomobject]@{
        Status    = $status
        Code      = $code
        Message   = $message
        Data      = if ($null -ne $json) { $json.data } else { $null }
        Raw       = $raw
        Json      = $json
        ElapsedMs = [int]$stopwatch.ElapsedMilliseconds
        Ok        = ($status -ge 200 -and $status -lt 300 -and $code -eq 0)
    }
}

function Get-ApiData {
    param($Response, [string]$Step)
    if (-not $Response.Ok) {
        throw "$Step 失败：HTTP=$($Response.Status) code=$($Response.Code) body=$($Response.Raw)"
    }
    return $Response.Data
}

function Invoke-Login {
    param([string]$Username, [string]$Password, [string]$DeviceId)
    $resp = Invoke-Api -Method 'Post' -Path '/api/v1/auth/login' -Body @{
        username   = $Username
        password   = $Password
        deviceId   = $DeviceId
        deviceName = "bpm-concurrency-test-$DeviceId"
    }
    $data = Get-ApiData $resp "登录 $Username"
    return [pscustomobject]@{
        Token = [string]$data.tokenPair.accessToken
        User  = $data.user
        UserId = [string]$data.user.id
    }
}

# ----------------------------------------------------------------------
# 主流程
# ----------------------------------------------------------------------
$exitCode = 0
$startedAt = Get-Date

try {
    Write-Step "1/5 管理员登录并获取 HMAC 签名密钥（BaseUrl=$BaseUrl）"
    $adminDeviceId = 'bpm-concurrency-test-admin'
    $admin = Invoke-Login $AdminUser $AdminPassword $adminDeviceId
    Assert-True ($admin.Token.Length -gt 0) "管理员登录成功：userId=$($admin.UserId)"

    $secretResp = Invoke-Api -Method 'Get' -Path "/api/v1/auth/sign-secret?deviceId=$adminDeviceId" -Token $admin.Token
    $signSecret = [string](Get-ApiData $secretResp '获取签名密钥').secret
    Assert-True ($signSecret.Length -gt 0) '获取设备 AppSecret 成功（可用于模板发布等 HMAC 写接口）'

    Write-Step "2/5 幂等准备测试审批用户与会签模板"
    $userPage = Invoke-Api -Method 'Get' -Path "/api/v1/users?page=1&size=20&keyword=$TestUsername" -Token $admin.Token
    $existingUser = @((Get-ApiData $userPage '查询测试用户').records) |
        Where-Object { $_.username -eq $TestUsername } |
        Select-Object -First 1

    if ($null -ne $existingUser) {
        $testUserId = [string]$existingUser.id
        Write-Host "测试用户已存在，跳过创建：$TestUsername（id=$testUserId）"
    } else {
        $createResp = Invoke-Api -Method 'Post' -Path '/api/v1/users' -Token $admin.Token -Body @{
            username = $TestUsername
            name     = 'BPM 压测用户'
            password = $TestPassword
            status   = 'ACTIVE'
            roleIds  = @(3)
        }
        $testUserId = [string](Get-ApiData $createResp '创建测试用户')
        Write-Host "测试用户创建成功：$TestUsername（id=$testUserId）"
    }

    # 模板：start -> 会签（admin + 测试用户）-> end
    $templateId = $null
    $createTemplate = Invoke-Api -Method 'Post' -Path '/api/v1/bpm/templates' -Token $admin.Token -Body @{
        code    = $TemplateCode
        name    = 'BPM 并发压测会签模板'
        bizType = $BizType
    }
    if ($createTemplate.Ok) {
        $templateId = [string]$createTemplate.Data
        Write-Host "模板创建成功：$TemplateCode（id=$templateId）"
    } elseif ($createTemplate.Code -eq 409001) {
        $templatePage = Invoke-Api -Method 'Get' -Path "/api/v1/bpm/templates?page=1&size=20&keyword=$TemplateCode" -Token $admin.Token
        $matched = @((Get-ApiData $templatePage '查询模板').records) |
            Where-Object { $_.code -eq $TemplateCode } |
            Select-Object -First 1
        if ($null -eq $matched) {
            throw "模板编码 $TemplateCode 已存在但未能查询到，请检查权限或分页参数"
        }
        $templateId = [string]$matched.id
        Write-Host "模板已存在，复用：$TemplateCode（id=$templateId）"
    } else {
        throw "创建模板失败：HTTP=$($createTemplate.Status) code=$($createTemplate.Code) body=$($createTemplate.Raw)"
    }

    $schemaJson = (@{
        nodes = @(
            @{ nodeKey = 'start'; seqNo = 1; nodeType = 'START'; nodeName = '发起' },
            @{
                nodeKey     = 'countersign'
                seqNo       = 2
                nodeType    = 'COUNTERSIGN'
                nodeName    = '会签审批'
                approveMode = 'ALL'
                assignees   = @(
                    @{ type = 'USER'; value = $admin.UserId },
                    @{ type = 'USER'; value = $testUserId }
                )
                dueHours     = 24
                rejectTarget = 'INITIATOR'
            },
            @{ nodeKey = 'end'; seqNo = 3; nodeType = 'END'; nodeName = '结束' }
        )
    } | ConvertTo-Json -Depth 10 -Compress)

    # 模板发布为 HMAC 签名写接口
    $publishResp = Invoke-Api -Method 'Post' -Path "/api/v1/bpm/templates/$templateId/publish" `
        -Token $admin.Token -DeviceId $adminDeviceId -Secret $signSecret -Signed `
        -Body @{ schemaJson = $schemaJson }
    $versionId = [string](Get-ApiData $publishResp '发布模板')
    Assert-True ($versionId.Length -gt 0) "模板发布成功（versionId=$versionId，HMAC 签名通过）"

    Write-Step "3/5 发起实例并分别拉取管理员 / 测试用户待办"
    $bizId = [long]([DateTimeOffset]::UtcNow.ToUnixTimeMilliseconds() % 900000)
    $startResp = Invoke-Api -Method 'Post' -Path '/api/v1/bpm/instances' -Token $admin.Token -Body @{
        templateCode = $TemplateCode
        bizType      = $BizType
        bizId        = $bizId
    }
    $instanceId = [string](Get-ApiData $startResp '发起实例')
    Write-Host "实例发起成功：instanceId=$instanceId，bizId=$bizId"

    $tester = Invoke-Login $TestUsername $TestPassword 'bpm-concurrency-test-tester'
    Assert-True ($tester.Token.Length -gt 0) "测试用户登录成功：userId=$($tester.UserId)"

    $adminPending = Invoke-Api -Method 'Get' -Path '/api/v1/bpm/tasks/pending?page=1&size=100' -Token $admin.Token
    $adminTask = @((Get-ApiData $adminPending '查询管理员待办').records) |
        Where-Object { [string]$_.instanceId -eq $instanceId -and $_.status -eq 'PENDING' -and [string]$_.assigneeId -eq $admin.UserId } |
        Select-Object -First 1

    $testerPending = Invoke-Api -Method 'Get' -Path '/api/v1/bpm/tasks/pending?page=1&size=100' -Token $tester.Token
    $testerTask = @((Get-ApiData $testerPending '查询测试用户待办').records) |
        Where-Object { [string]$_.instanceId -eq $instanceId -and $_.status -eq 'PENDING' -and [string]$_.assigneeId -eq $tester.UserId } |
        Select-Object -First 1

    if ($null -eq $adminTask) { throw '管理员未查到实例的待办任务，请检查会签节点审批人配置' }
    if ($null -eq $testerTask) { throw '测试用户未查到实例的待办任务，请检查会签节点审批人配置' }
    $adminTaskId = [string]$adminTask.id
    $testerTaskId = [string]$testerTask.id
    $testerTaskVersion = [int]$testerTask.taskVersion
    Write-Pass "待办拉取成功：adminTaskId=$adminTaskId，testerTaskId=$testerTaskId"

    Write-Step "4/5 管理员正常同意 + 测试用户对同一任务并发同意 $Concurrency 次"
    $adminApprove = Invoke-Api -Method 'Post' -Path "/api/v1/bpm/tasks/$adminTaskId/approve" -Token $admin.Token -Body @{
        taskVersion = [int]$adminTask.taskVersion
        comment     = '并发压测：管理员正常同意'
    }
    Assert-True ($adminApprove.Ok) "管理员同意成功（HTTP=$($adminApprove.Status)）"

    # 并发提交：同一 taskId + 同一 taskVersion，多请求竞争
    $approveBody = (@{ taskVersion = $testerTaskVersion; comment = '并发压测：同意' } | ConvertTo-Json -Compress)
    $parallelStart = Get-Date
    $batchResults = 1..$Concurrency | ForEach-Object -Parallel {
        $uri = "$($using:BaseUrl)/api/v1/bpm/tasks/$($using:testerTaskId)/approve"
        try {
            $resp = Invoke-WebRequest -Uri $uri -Method 'Post' `
                -Headers @{ Accept = 'application/json'; Authorization = "Bearer $($using:tester.Token)" } `
                -ContentType 'application/json; charset=utf-8' `
                -Body $using:approveBody `
                -SkipHttpErrorCheck -TimeoutSec $using:TimeoutSec
            [pscustomobject]@{ Index = $_; Status = [int]$resp.StatusCode; Raw = [string]$resp.Content }
        } catch {
            [pscustomobject]@{ Index = $_; Status = -1; Raw = $_.Exception.Message }
        }
    } -ThrottleLimit ([Math]::Min($Concurrency, 100))
    $parallelElapsedMs = [int]((Get-Date) - $parallelStart).TotalMilliseconds

    $approveSuccess = 0
    $approveConflict = 0
    $approveUnexpected = [System.Collections.Generic.List[string]]::new()
    foreach ($r in $batchResults) {
        $code = -1
        $msg = ''
        try {
            $parsed = $r.Raw | ConvertFrom-Json
            $code = [int]$parsed.code
            $msg = [string]$parsed.message
        } catch {
            $code = -1
        }
        Add-Stat $r.Status $code $msg
        if ($r.Status -ge 200 -and $r.Status -lt 300 -and $code -eq 0) {
            $approveSuccess++
        } elseif ($code -eq 409011 -or $msg -like '*BPM_TASK_CONFLICT*') {
            $approveConflict++
        } else {
            $approveUnexpected.Add("#$($r.Index) HTTP=$($r.Status) code=$code body=$($r.Raw)")
            if ($r.Status -ge 400 -or $r.Status -lt 0) {
                Write-Host "异常响应：HTTP=$($r.Status) code=$code body=$($r.Raw)" -ForegroundColor Yellow
            }
        }
    }

    Write-Host ("并发提交完成：成功={0}，冲突={1}，异常={2}，耗时={3}ms" -f `
        $approveSuccess, $approveConflict, $approveUnexpected.Count, $parallelElapsedMs)
    Assert-True ($approveSuccess -ge 1) '并发提交中至少有一次成功同意（首次提交生效）'
    Assert-True ($approveUnexpected.Count -eq 0) '并发提交结果均为「成功」或「BPM_TASK_CONFLICT」，无 4xx/5xx 异常'

    Write-Step '5/5 断言实例/节点/任务一致性'
    # 等待异步落地（正常情况下接口同步完成，这里仅做短暂轮询兜底）
    $detail = $null
    for ($i = 0; $i -lt 10; $i++) {
        $detailResp = Invoke-Api -Method 'Get' -Path "/api/v1/bpm/instances/$instanceId" -Token $admin.Token
        $detail = Get-ApiData $detailResp '查询实例详情'
        if ($detail.instance.status -ne 'RUNNING') { break }
        Start-Sleep -Milliseconds 200
    }

    $instance = $detail.instance
    $nodes = @($detail.nodes)
    Assert-True ([string]$instance.id -eq $instanceId) '实例 ID 与发起实例一致'
    Assert-True ($instance.status -eq 'APPROVED') "实例最终状态为 APPROVED（实际=$($instance.status)）"

    $csNodes = @($nodes | Where-Object { $_.nodeKey -eq 'countersign' })
    Assert-True ($csNodes.Count -eq 1) "会签节点恰好存在 1 条节点记录，无重复流转（实际=$($csNodes.Count)）"
    $csNode = $csNodes[0]
    Assert-True ($csNode.status -eq 'COMPLETED') "会签节点状态为 COMPLETED（实际=$($csNode.status)）"

    $csTasks = @($csNode.tasks)
    Assert-True ($csTasks.Count -eq 2) "会签节点任务数为 2，无丢失/无重复生成（实际=$($csTasks.Count)）"
    $allApproved = ($csTasks | Where-Object { $_.status -ne 'APPROVED' }).Count -eq 0
    Assert-True $allApproved '会签节点两条任务均为 APPROVED'

    $csTaskIds = @($csTasks | ForEach-Object { [string]$_.id })
    Assert-True ($csTaskIds -contains $adminTaskId) '管理员任务存在于会签节点中（任务未丢失）'
    Assert-True ($csTaskIds -contains $testerTaskId) '测试用户任务存在于会签节点中（任务未丢失）'

    $allTaskIds = @($nodes | ForEach-Object { $_.tasks } | ForEach-Object { [string]$_.id } | Where-Object { $_ })
    $duplicatedTaskIds = @($allTaskIds | Group-Object | Where-Object { $_.Count -gt 1 })
    Assert-True ($duplicatedTaskIds.Count -eq 0) "全流程任务 ID 无重复（实际重复=$($duplicatedTaskIds.Count)）"

    $nodeKeyDuplicates = @($nodes | Group-Object nodeKey | Where-Object { $_.Count -gt 1 })
    Assert-True ($nodeKeyDuplicates.Count -eq 0) "节点 key 无重复流转（重复=$($nodeKeyDuplicates.Count)）"

    $approvedTaskCount = ($allTaskIds.Count)
    Assert-True ($approvedTaskCount -eq 2) "全流程任务总数恰为 2（实际=$approvedTaskCount）"

    $elapsedTotal = [int]((Get-Date) - $startedAt).TotalMilliseconds
    Write-Host ''
    Write-Host '================ 压测结果 ================' -ForegroundColor Green
    Write-Host "实例 ID      : $instanceId"
    Write-Host "并发数       : $Concurrency"
    Write-Host "并发耗时     : $parallelElapsedMs ms"
    Write-Host "总耗时       : $elapsedTotal ms"
    Write-Host "请求统计     : 成功=$($script:Stats.Success)，冲突=$($script:Stats.Conflict)，4xx=$($script:Stats.ClientError)，5xx=$($script:Stats.ServerError)，其他=$($script:Stats.Other)"
    Write-Host '结论         : PASS' -ForegroundColor Green
    Write-Host '=========================================='
} catch {
    $exitCode = 1
    Write-Fail $_.Exception.Message
    Write-Host ''
    Write-Host "请求统计：成功=$($script:Stats.Success)，冲突=$($script:Stats.Conflict)，4xx=$($script:Stats.ClientError)，5xx=$($script:Stats.ServerError)，其他=$($script:Stats.Other)"
    Write-Host '结论：FAIL' -ForegroundColor Red
}

exit $exitCode

# 核算治理开放接口设计与 Apifox 联调手册

## 1. 目标说明

本手册用于说明企业级成本核算平台对第三方系统开放的接口口径、鉴权模式、时效设计与 Apifox 联调方式。

当前开放接口采用 **应用级鉴权 + 短时访问令牌** 模式，不建议第三方在每次业务调用时直接传递长期 `appSecret`。

推荐流程如下：

1. 第三方先使用 `appCode + appSecret` 申请访问令牌。
2. 平台返回短时有效的 `accessToken`。
3. 第三方在后续业务接口中通过 `Authorization: Bearer <accessToken>` 调用。
4. 当 `accessToken` 过期后，第三方重新申请令牌。
5. 如果开放应用本身超过平台配置的有效期，则即使重新申请令牌也会被拒绝，需要平台管理员续期后再调用。

---

## 2. 应用权限维护设计

后续平台应提供独立的“开放应用管理”功能，用于维护第三方接入方的权限。当前后端能力已经按照该模型设计。

### 2.1 开放应用核心字段

| 字段 | 含义 |
|---|---|
| `appCode` | 第三方应用编码，作为公开标识 |
| `appName` | 第三方应用名称 |
| `appSecretHash` | 应用密钥摘要值，平台不直接明文存储密钥 |
| `sceneScopeType` | 场景授权范围，支持 `ALL` 或 `LIST` |
| `sceneIdsJson` | 当范围为 `LIST` 时，可访问场景主键列表 |
| `allowDraftSnapshot` | 是否允许联调草稿配置 |
| `tokenTtlSeconds` | 访问令牌有效期，单位秒 |
| `effectiveStartTime` | 应用生效时间 |
| `effectiveEndTime` | 应用失效时间 |
| `status` | 应用状态，正常/停用 |

### 2.2 权限控制规则

1. 应用停用后，不允许申请令牌。
2. 应用未到生效时间前，不允许申请令牌。
3. 应用过了失效时间后，不允许申请令牌。
4. 应用只允许访问已授权场景。
5. 未开通草稿权限的应用，只允许使用已发布生效版本，不允许联调 `DRAFT` 快照。

### 2.3 时效性设计

平台侧有两层时效控制：

1. **应用有效期**
   - 由平台管理员维护。
   - 控制第三方系统在什么时间段内具备总体调用资格。

2. **访问令牌有效期**
   - 由 `tokenTtlSeconds` 控制。
   - 例如 7200 秒表示令牌 2 小时后失效。
   - 失效后第三方需重新调用令牌申请接口。

这意味着：
- `accessToken` 过期：第三方自己重新申请即可。
- `应用有效期` 过期：第三方重新申请也会失败，需管理员续期。

---

## 3. 鉴权流程

### 3.1 申请访问令牌

**接口**

`POST /cost/open/auth/token`

**请求体**

```json
{
  "appCode": "DEMO_OPEN_APP",
  "appSecret": "demo-open-secret"
}
```

**成功返回示例**

```json
{
  "code": 200,
  "msg": "开放接口访问令牌申请成功",
  "data": {
    "appCode": "DEMO_OPEN_APP",
    "appName": "开放联调演示应用",
    "tokenType": "Bearer",
    "accessToken": "coa_xxxxxxxxxxxxxxxxxxxx",
    "expiresInSeconds": 7200,
    "issuedAt": "2026-04-27 12:00:00",
    "expiresAt": "2026-04-27 14:00:00",
    "draftSnapshotAllowed": true,
    "sceneScopeType": "ALL",
    "authorizedSceneIds": []
  }
}
```

**失败场景示例**

- `appCode` 或 `appSecret` 错误
- 应用已停用
- 应用尚未生效
- 应用已过期

---

## 4. 业务调用时的请求头

申请到令牌后，所有开放接口均通过如下请求头调用：

```http
Authorization: Bearer {{accessToken}}
```

如果未携带令牌或令牌失效，平台会返回类似结果：

```json
{
  "code": 401,
  "msg": "开放接口访问令牌已失效，请重新申请 accessToken",
  "data": {
    "tokenType": "Bearer",
    "tokenApplyPath": "/cost/open/auth/token"
  }
}
```

---

## 5. 开放接口列表

### 5.1 查询可访问场景

**接口**

`GET /cost/open/scenes`

**用途**

返回当前开放应用可访问的场景列表。

---

### 5.2 查询场景版本口径

**接口**

`GET /cost/open/scenes/{sceneId}/versions`

**用途**

返回：
- 当前场景信息
- 默认快照模式
- 支持的快照模式
- 已发布版本列表

---

### 5.3 查询场景运行费用

**接口**

`GET /cost/open/scenes/{sceneId}/fees?versionId=xxx&snapshotMode=ACTIVE`

**用途**

查询当前版本/快照下可执行的费用列表。

---

### 5.4 生成费用接入模板

**接口**

`GET /cost/open/fee-template`

**主要参数**

| 参数 | 说明 |
|---|---|
| `sceneId` | 场景主键，必填 |
| `versionId` | 版本主键，可选 |
| `snapshotMode` | `ACTIVE` 或 `DRAFT` |
| `feeId` | 单费用主键，可选 |
| `feeIds` | 多费用主键列表，逗号分隔，可选 |
| `feeCode` | 费用编码，可选 |
| `taskType` | 任务类型，可选 |

**用途**

用于告诉第三方：
- 当前费用口径需要哪些变量
- 每个变量对应什么中文名称
- 来源路径是什么
- 是否必填
- 是否有默认值
- 示例值是什么

这一步是第三方联调的核心入口。

---

### 5.5 单费用 / 多费用核算

**接口**

`POST /cost/open/fee/calculate`

**请求体示例**

```json
{
  "sceneId": 1,
  "versionId": 2,
  "snapshotMode": "ACTIVE",
  "feeIds": [12, 13],
  "inputJson": "[{\"bizNo\":\"SIM-001\",\"oddWork\":{\"quantity\":1},\"cover\":{\"action\":\"moor\"}}]"
}
```

**说明**

- 支持单对象
- 支持对象数组
- 支持单费用、多费用、全费用
- 多费用时系统会自动补齐依赖费用链

---

## 6. 输入校验设计

开放接口不是直接“算错就结束”，而是先做模板口径校验。

当第三方少传了必填字段时，返回会明确指出：
- 哪一条记录有问题
- 哪个变量缺失
- 中文变量名是什么
- 要走哪个来源路径补齐

**示例返回**

```json
{
  "code": 500,
  "msg": "输入数据缺少模板要求字段，请补齐后再重新取价",
  "data": {
    "validationPassed": false,
    "missingFieldCount": 2,
    "validationMessages": [
      {
        "recordIndex": 1,
        "bizNo": "SIM-001",
        "variableCode": "COVER_ACTION",
        "variableName": "苦盖动作",
        "path": "cover.action",
        "message": "缺少必填字段 苦盖动作，请按来源路径补齐：cover.action"
      }
    ]
  }
}
```

这类提示信息就是给第三方联调时定位问题用的，避免只返回“参数错误”而没有业务语义。

---

## 7. 快照模式说明

### 7.1 ACTIVE

- 表示按已发布生效版本执行。
- 适合正式接入与生产调用。

### 7.2 DRAFT

- 表示按草稿配置执行。
- 适合第三方联调、预验证、开发阶段核对。
- 只有被授予 `allowDraftSnapshot = true` 的开放应用才允许调用。

如果第三方没有草稿联调权限，却传了 `snapshotMode=DRAFT`，会收到 `403` 级业务错误提示。

---

## 8. 推荐给第三方的调用顺序

建议第三方系统严格按以下顺序接入：

1. 申请访问令牌
2. 查询可访问场景
3. 查询场景可用版本
4. 查询场景费用列表
5. 生成目标费用模板
6. 按模板准备 JSON 数据
7. 调用费用核算接口
8. 根据返回结果修正字段或正式接入

这样能显著减少第三方拍脑袋组 JSON 的试错成本。

---

## 9. Apifox 联调建议

### 9.1 环境变量建议

在 Apifox 中配置如下环境变量：

| 变量名 | 示例值 |
|---|---|
| `baseUrl` | `http://localhost:8080` |
| `appCode` | `DEMO_OPEN_APP` |
| `appSecret` | `demo-open-secret` |
| `accessToken` | 通过令牌接口动态写入 |
| `sceneId` | `1` |
| `versionId` | `1` |

### 9.2 令牌接口调试

先创建一个“申请访问令牌”接口，请求成功后将 `data.accessToken` 写入环境变量 `accessToken`。

### 9.3 后续接口统一请求头

在 Apifox 全局鉴权或单接口请求头中配置：

```http
Authorization: Bearer {{accessToken}}
```

### 9.4 令牌过期处理

如果接口返回：
- `开放接口访问令牌已失效`
- `缺少开放接口访问令牌`

则说明应重新调用 `/cost/open/auth/token` 获取新令牌。

---

## 10. 演示应用账号

当前初始化 SQL 中已提供两套演示应用：

### 10.1 开放联调演示应用

- `appCode`: `DEMO_OPEN_APP`
- `appSecret`: `demo-open-secret`
- 权限：允许 `ACTIVE` + `DRAFT`

### 10.2 开放生产演示应用

- `appCode`: `DEMO_PUBLISHED_APP`
- `appSecret`: `demo-published-secret`
- 权限：仅允许 `ACTIVE`

---

## 11. 企业级建议

从企业级产品角度，建议后续继续完善以下能力：

1. 开放应用管理页面
   - 应用新增、停用、续期、重置密钥、场景授权、草稿权限维护

2. 调用审计台账
   - 记录第三方应用的模板拉取、核算调用、错误分布与高频费用口径

3. 密钥轮换机制
   - 支持管理员重置 `appSecret`
   - 旧密钥失效，新密钥重新分发

4. 调用频控与告警
   - 防止单应用异常高频压测拖垮服务

5. 文档自动化
   - 后续可把开放接口整理为在线 Swagger / OpenAPI 文档，便于 Apifox 一键导入

---

## 12. 结论

当前开放接口设计推荐采用：

- **应用级长期身份**：`appCode + appSecret`
- **接口级短期访问凭证**：`accessToken`
- **平台时效控制**：应用有效期 + 令牌有效期双层限制

这套方式兼顾了：
- 企业级安全性
- 第三方实际可用性
- 联调效率
- 后续治理扩展能力
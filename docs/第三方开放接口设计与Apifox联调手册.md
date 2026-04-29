# 核算治理开放接口调用手册

## 1. 文档定位

本手册面向以下两类角色：

- 平台管理员：负责创建开放应用、分配场景权限、控制草稿联调范围、维护应用有效期与密钥。
- 第三方系统：负责按平台提供的接入契约申请令牌、查询模板、组装输入 JSON、执行单费用或多费用核算。

本文档对应当前 Spring Boot 版企业级核算平台的开放接口体系，目标不是“给第三方一个随便调的接口”，而是提供一套可治理、可续期、可限权、可定位问题的企业级接入模型。

---

## 2. 总体设计原则

### 2.1 应用级鉴权，而不是后台账号共享

平台为每个第三方系统维护独立的开放应用，开放应用拥有自己的：

- `appCode`
- `appSecret`
- 场景授权范围
- 草稿联调权限
- 令牌时长
- 生效时间与失效时间

第三方不再共享内部登录账号，也不直接持有平台后台 JWT。

### 2.2 双层时效控制

平台同时控制两层时效：

1. **开放应用有效期**
   - 由管理员在开放应用配置中心维护。
   - 控制第三方在什么时间段内具备调用资格。

2. **访问令牌有效期**
   - 由 `tokenTtlSeconds` 控制。
   - 令牌过期后，第三方需重新申请访问令牌。

### 2.3 先查模板，再组装数据，再取价

企业级对接不建议第三方直接猜测字段结构。标准顺序是：

1. 查询可访问场景
2. 查询版本口径
3. 查询费用范围
4. 生成费用接入模板
5. 按模板构造 JSON
6. 执行核算

### 2.4 错误提示必须可定位

如果第三方缺字段、传错口径或无权访问，平台不会只返回“参数错误”，而是尽量返回：

- 变量编码
- 中文变量名
- 来源路径
- 第几条记录出错
- 业务建议

这点是开放接口可持续联调的关键。

---

## 3. 开放应用配置说明

开放应用配置中心菜单：`系统管理 -> 开放应用`

### 3.1 核心字段

| 字段 | 含义 | 说明 |
|---|---|---|
| `appCode` | 开放应用编码 | 第三方公开标识，申请 token 时使用 |
| `appName` | 开放应用名称 | 例如“首钢人力外部联调应用” |
| `appSecret` | 开放应用密钥 | 仅在创建或重置时明文展示一次 |
| `sceneScopeType` | 场景授权范围 | `ALL` 或 `LIST` |
| `sceneIds` | 授权场景列表 | 仅当 `sceneScopeType=LIST` 时必填 |
| `allowDraftSnapshot` | 草稿联调权限 | 允许第三方调用 `DRAFT` 快照 |
| `tokenTtlSeconds` | 令牌有效期 | 推荐 1800、7200、21600、86400 秒 |
| `effectiveStartTime` | 生效开始时间 | 为空表示不限开始时间 |
| `effectiveEndTime` | 生效结束时间 | 为空表示不限结束时间 |
| `status` | 应用状态 | `0=正常`，`1=停用` |
| `remark` | 管理备注 | 建议记录对接方、负责人、特殊限制 |

### 3.2 管理规则

1. 应用停用后，不允许再申请 token。
2. 当前时间早于生效开始时间时，不允许申请 token。
3. 当前时间晚于失效时间时，不允许申请 token。
4. 指定场景应用只能查询和调用已授权场景。
5. 未开通草稿权限时，不允许使用 `snapshotMode=DRAFT`。
6. 重置密钥后，旧密钥立即失效，第三方必须立刻替换。

---

## 4. 鉴权流程

### 4.1 令牌申请接口

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

### 4.2 令牌使用方式

后续所有开放接口，统一通过以下请求头传递：

```http
Authorization: Bearer {{accessToken}}
```

### 4.3 令牌失效时的标准响应

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

## 5. 快照模式说明

### 5.1 ACTIVE

- 表示按已发布生效版本执行。
- 适合正式接入、稳定压测、生产调用。

### 5.2 DRAFT

- 表示按当前草稿快照执行。
- 适合第三方联调、未发布规则预验证、上线前校对。
- 只有 `allowDraftSnapshot=true` 的开放应用才允许调用。

### 5.3 推荐口径

- 内部联调阶段：可使用 `DRAFT`
- 第三方准生产联调：优先使用 `ACTIVE`
- 正式生产：只建议使用 `ACTIVE`

---

## 6. 开放接口清单

| 接口 | 方法 | 是否需要 Bearer | 用途 |
|---|---|---|---|
| `/cost/open/auth/token` | POST | 否 | 申请访问令牌 |
| `/cost/open/scenes` | GET | 是 | 查询可访问场景 |
| `/cost/open/scenes/{sceneId}/versions` | GET | 是 | 查询场景版本口径 |
| `/cost/open/scenes/{sceneId}/fees` | GET | 是 | 查询场景运行费用 |
| `/cost/open/fee-template` | GET | 是 | 生成费用接入模板 |
| `/cost/open/fee/calculate` | POST | 是 | 执行单费用/多费用/全费用核算 |

---

## 7. 接口详细说明

### 7.1 查询可访问场景

**接口**

`GET /cost/open/scenes`

**用途**

返回当前开放应用可访问的场景列表。

**Apifox 填写方式**

- 方法：`GET`
- 地址：`{{baseUrl}}/cost/open/scenes`
- Headers：

```http
Authorization: Bearer {{accessToken}}
```

- Params：无
- Body：无

> 这是最容易误填的接口之一。该接口不需要任何 Query 参数，也不需要 Body；如果返回 `401`，优先检查 `accessToken` 是否已过期，或 Apifox 的 Bearer Token 是否重复拼接了 `Bearer` 前缀。

**成功返回要点**

- `openApp`：当前开放应用摘要
- `sceneCount`：可访问场景数
- `scenes`：场景列表

**返回示例**

```json
{
  "code": 200,
  "msg": "操作成功",
  "data": {
    "openApp": {
      "appCode": "DEMO_OPEN_APP",
      "appName": "开放联调演示应用",
      "sceneScopeType": "ALL",
      "draftSnapshotAllowed": true,
      "authorizedSceneIds": []
    },
    "sceneCount": 1,
    "scenes": [
      {
        "sceneId": 1,
        "sceneCode": "SHOUGANG-ORE-HR-001",
        "sceneName": "首钢矿石人力费用",
        "businessDomain": "LABOR_COST",
        "defaultObjectDimension": "协力单位",
        "activeVersionId": 2,
        "activeVersionNo": "V2026.04.002",
        "status": "0"
      }
    ]
  }
}
```

### 7.2 查询场景版本口径

**接口**

`GET /cost/open/scenes/{sceneId}/versions`

**用途**

返回：

- 场景信息
- 默认快照模式
- 支持的快照模式
- 已发布版本列表

**使用建议**

- 如果第三方没有草稿联调权限，就按返回的 `supportedSnapshotModes` 只走 `ACTIVE`
- 如果是平台和第三方联合调试未发布规则，可在允许时使用 `DRAFT`

**Apifox 填写示例**

- 方法：`GET`
- 地址：`{{baseUrl}}/cost/open/scenes/{{sceneId}}/versions`
- Headers：

```http
Authorization: Bearer {{accessToken}}
```

- Params：无
- Body：无

### 7.3 查询场景运行费用

**接口**

`GET /cost/open/scenes/{sceneId}/fees?versionId=2&snapshotMode=ACTIVE`

**用途**

获取当前版本或快照下可执行的费用列表，供第三方：

- 单费用调用时选择目标费用
- 多费用调用时组合费用范围
- 全费用调用前确认费用数量

**Apifox 填写示例**

- 方法：`GET`
- 地址：`{{baseUrl}}/cost/open/scenes/{{sceneId}}/fees`
- Headers：

```http
Authorization: Bearer {{accessToken}}
```

- Params（正式生效版）：

| 参数 | 示例值 |
|---|---|
| `versionId` | `{{versionId}}` |

- Params（草稿联调）：

| 参数 | 示例值 |
|---|---|
| `snapshotMode` | `DRAFT` |

> 如果同时传了 `versionId` 和 `snapshotMode`，第三方应优先明确自己想走哪种口径，避免在联调与正式版本之间混淆。

### 7.4 生成费用接入模板

**接口**

`GET /cost/open/fee-template`

**主要参数**

| 参数 | 是否必填 | 说明 |
|---|---|---|
| `sceneId` | 是 | 场景主键 |
| `versionId` | 否 | 发布版本主键 |
| `snapshotMode` | 否 | `ACTIVE` 或 `DRAFT`，默认 `ACTIVE` |
| `feeId` | 否 | 单费用主键 |
| `feeIds` | 否 | 多费用主键列表，逗号分隔 |
| `feeCode` | 否 | 费用编码 |
| `taskType` | 否 | 接入任务类型 |

**费用范围选择规则**

- `feeId`：按单费用模板生成
- `feeIds`：按多费用模板生成
- 全部都不传：按全费用执行链生成模板

**模板返回核心字段**

| 字段 | 说明 |
|---|---|
| `inputContractFieldCount` | 模板字段总数 |
| `requiredFieldCount` | 必填字段数 |
| `inputContractFields` | 输入契约字段列表 |
| `integrationAdvice` | 平台给第三方的接入建议 |

**字段对象说明**

| 字段 | 说明 |
|---|---|
| `variableCode` | 变量编码 |
| `variableName` | 变量中文名 |
| `sourceType` | 来源类型 |
| `dataType` | 数据类型 |
| `path` | 来源路径 |
| `pathLabel` | 展示用路径 |
| `required` | 是否必填 |
| `defaultValue` | 默认值 |
| `exampleValue` | 示例值 |
| `includedInTemplate` | 是否纳入模板 |
| `templateRoles` | 字段角色说明 |
| `sourceRuleCodes` | 来源规则编码 |
| `dependsOn` | 依赖字段说明 |

**最关键的企业级价值**

第三方不需要再猜字段，而是直接拿到：

- 中文变量名
- 对应 JSON 路径
- 是否必填
- 示例值

**Apifox 示例 1：单费用模板**

- 方法：`GET`
- 地址：`{{baseUrl}}/cost/open/fee-template`
- Headers：

```http
Authorization: Bearer {{accessToken}}
```

- Params（按费用编码）：

| 参数 | 示例值 |
|---|---|
| `sceneId` | `{{sceneId}}` |
| `versionId` | `{{versionId}}` |
| `feeCode` | `SG_FEMALE_SHIFT_LABOR` |

- Params（按费用主键）：

| 参数 | 示例值 |
|---|---|
| `sceneId` | `{{sceneId}}` |
| `versionId` | `{{versionId}}` |
| `feeId` | `{{feeId}}` |

**Apifox 示例 2：多费用模板**

- 方法：`GET`
- 地址：`{{baseUrl}}/cost/open/fee-template`
- Params：

| 参数 | 示例值 |
|---|---|
| `sceneId` | `{{sceneId}}` |
| `versionId` | `{{versionId}}` |
| `feeIds` | `12,13` |

> `feeIds` 是**逗号分隔的数字主键字符串**，不是 JSON 数组。

**Apifox 示例 3：全费用模板**

- 方法：`GET`
- 地址：`{{baseUrl}}/cost/open/fee-template`
- Params：

| 参数 | 示例值 |
|---|---|
| `sceneId` | `{{sceneId}}` |
| `versionId` | `{{versionId}}` |

> 不传 `feeId`、`feeIds`、`feeCode`，系统就会按当前场景和运行快照生成全费用模板。

### 7.5 执行费用核算

**接口**

`POST /cost/open/fee/calculate`

**请求体字段**

| 字段 | 是否必填 | 说明 |
|---|---|---|
| `sceneId` | 是 | 场景主键 |
| `versionId` | 否 | 发布版本主键 |
| `snapshotMode` | 否 | `ACTIVE` 或 `DRAFT` |
| `feeId` | 否 | 单费用主键 |
| `feeIds` | 否 | 多费用主键数组 |
| `feeCode` | 否 | 单费用编码 |
| `billMonth` | 否 | 账期上下文 |
| `includeExplain` | 否 | 是否返回轻量解释信息 |
| `inputJson` | 是 | JSON 对象或对象数组字符串 |

**单费用示例**

```json
{
  "sceneId": 1,
  "snapshotMode": "DRAFT",
  "feeCode": "SG_FEMALE_SHIFT_LABOR",
  "billMonth": "2026-04",
  "includeExplain": true,
  "inputJson": "[{\"bizNo\":\"SIM-001\",\"femaleTeam\":{\"headcount\":6},\"attendance\":{\"femaleActual\":5,\"femaleRequired\":6}}]"
}
```

**多费用示例**

```json
{
  "sceneId": 1,
  "versionId": 2,
  "feeIds": [12, 13],
  "includeExplain": false,
  "billMonth": "2026-04",
  "inputJson": "[{\"bizNo\":\"SIM-002\",\"femaleTeam\":{\"headcount\":6},\"attendance\":{\"femaleActual\":5,\"femaleRequired\":6},\"coverWork\":{\"action\":\"moor\",\"cargoType\":\"块矿\",\"workloadTon\":1200}}]"
}
```

**全费用调用说明**

如果 `feeId`、`feeIds`、`feeCode` 都不传，则系统按当前场景运行快照做全费用执行链核算。

**全费用示例**

```json
{
  "sceneId": 1,
  "versionId": 2,
  "billMonth": "2026-04",
  "includeExplain": false,
  "inputJson": "[{\"bizNo\":\"SIM-003\",\"femaleTeam\":{\"headcount\":6},\"attendance\":{\"femaleActual\":5,\"femaleRequired\":6},\"coverWork\":{\"action\":\"moor\",\"cargoType\":\"块矿\",\"workloadTon\":1200},\"mooring\":{\"headcount\":4},\"oddWork\":{\"hours\":8},\"seasonal\":{\"subsidyEquiv\":1},\"overtime\":{\"days\":2}}]"
}
```

**Apifox 填写提醒**

1. `inputJson` 是一个**字符串字段**
   - 不是直接把 JSON 数组贴成对象
   - 而是把业务 JSON 序列化后，放进 `inputJson`
2. 单费用有三种指定方式，通常三选一：
   - `feeId`
   - `feeIds`
   - `feeCode`
3. 如果三者都不传，默认按全费用执行链处理
4. `billMonth` 只在公式或变量依赖账期上下文时才需要显式传入

---

## 8. 输入校验与友好提示

开放接口在执行核算前，会先根据模板校验必填字段。

如果输入缺少字段，平台会返回：

- 第几条记录有问题：`recordIndex`
- 业务编号：`bizNo`
- 变量编码：`variableCode`
- 变量中文名：`variableName`
- 来源路径：`path`
- 中文提示：`message`

**示例**

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
        "variableName": "苫盖动作",
        "path": "cover.action",
        "message": "缺少必填字段 苫盖动作，请按来源路径补齐：cover.action"
      }
    ]
  }
}
```

**第三方处理建议**

1. 先按 `path` 补齐 JSON。
2. 再按 `variableName` 给业务同学定位字段口径。
3. 补齐后重新调用即可，不需要人工逐条追 SQL 或看后台日志。

---

## 9. 典型错误与处理方式

| code | 场景 | 含义 | 建议处理 |
|---|---|---|---|
| `200` | 正常 | 请求成功 | 继续处理业务结果 |
| `401` | token 无效/过期 | 访问令牌不可用 | 重新申请 accessToken |
| `403` | 无场景权限 / 无草稿权限 | 开放应用权限不足 | 联系平台管理员调整授权 |
| `500` | 缺少模板字段 / JSON 非法 / 业务校验失败 | 请求口径或数据有问题 | 按返回中文提示修正数据 |

---

## 10. Apifox 联调建议

### 10.1 推荐环境变量

| 变量名 | 示例值 |
|---|---|
| `baseUrl` | `http://localhost:8080` |
| `appCode` | `DEMO_OPEN_APP` |
| `appSecret` | `demo-open-secret` |
| `accessToken` | 通过令牌接口写入 |
| `sceneId` | `1` |
| `versionId` | `2` |
| `feeId` | `12` |
| `feeIds` | `12,13` |

推荐额外增加 2 个临时变量，方便联调时切换：

| 变量名 | 示例值 |
|---|---|
| `feeCode` | `SG_FEMALE_SHIFT_LABOR` |
| `snapshotMode` | `ACTIVE` 或 `DRAFT` |

### 10.2 令牌接口用例建议

请求：

- 方法：`POST`
- 地址：`{{baseUrl}}/cost/open/auth/token`
- Body：

```json
{
  "appCode": "{{appCode}}",
  "appSecret": "{{appSecret}}"
}
```

Tests 脚本建议：

```javascript
const body = pm.response.json();
if (body && body.code === 200 && body.data && body.data.accessToken) {
  pm.environment.set('accessToken', body.data.accessToken);
}
```

### 10.3 Bearer 头建议

后续所有接口统一：

```http
Authorization: Bearer {{accessToken}}
```

> 如果你在 Apifox 的 `Auth` 页签里选择了 `Bearer Token`，那么 Token 输入框里只填写 `{{accessToken}}` 即可，不要再手动写 `Bearer ` 前缀，否则会形成双前缀，导致认证失败。

### 10.4 推荐联调顺序

1. 先调 `/auth/token`
2. 再调 `/scenes`
3. 再调 `/scenes/{sceneId}/versions`
4. 再调 `/scenes/{sceneId}/fees`
5. 再调 `/fee-template`
6. 最后调 `/fee/calculate`

### 10.5 一次跑通的最小示例

**步骤 1：申请 token**

```json
POST {{baseUrl}}/cost/open/auth/token
{
  "appCode": "{{appCode}}",
  "appSecret": "{{appSecret}}"
}
```

**步骤 2：查询可访问场景**

```http
GET {{baseUrl}}/cost/open/scenes
Authorization: Bearer {{accessToken}}
```

**步骤 3：按正式生效版生成单费用模板**

```http
GET {{baseUrl}}/cost/open/fee-template?sceneId={{sceneId}}&versionId={{versionId}}&feeCode=SG_FEMALE_SHIFT_LABOR
Authorization: Bearer {{accessToken}}
```

**步骤 4：执行单费用核算**

```json
POST {{baseUrl}}/cost/open/fee/calculate
{
  "sceneId": {{sceneId}},
  "versionId": {{versionId}},
  "feeCode": "SG_FEMALE_SHIFT_LABOR",
  "billMonth": "2026-04",
  "includeExplain": true,
  "inputJson": "[{\"bizNo\":\"BIZ-001\",\"femaleTeam\":{\"headcount\":6},\"attendance\":{\"femaleActual\":5,\"femaleRequired\":6}}]"
}
```

### 10.6 最容易填错的 4 个地方

1. `GET /cost/open/scenes` 不需要 Body，也不需要 Params
2. `accessToken` 过期后，必须重新调用 `/cost/open/auth/token`
3. `fee-template` 的 `feeIds` 要传逗号分隔字符串，不是 JSON 数组
4. `fee/calculate` 的 `inputJson` 必须是字符串，不是直接贴对象或数组

---

## 11. 企业级接入建议

### 11.1 不建议长期缓存 token

因为 token 有明确过期时间，第三方应支持：

- 快过期前主动刷新
- 401 后自动重试换 token

### 11.2 不建议跳过模板直接调用核算

因为实际费用、变量、来源路径会随着版本和规则变化。模板接口是第三方和平台之间的“接入契约”，应该成为正式依赖入口。

### 11.3 区分联调应用与生产应用

推荐至少拆成两类开放应用：

- 联调应用：允许 `DRAFT`
- 生产应用：仅允许 `ACTIVE`

### 11.4 多费用优先用于业务联调，全费用优先用于整场景压测

- 单费用：适合字段定位和规则校验
- 多费用：适合一组业务费用联调
- 全费用：适合整体压测、数据验收、全场景核算验证

---

## 12. 文档与契约交付物

建议每次对外接入至少交付以下内容：

1. 本调用手册
2. 开放应用 `appCode`
3. 创建或重置时生成的 `appSecret`
4. Apifox 环境变量模板
5. OpenAPI 契约文件
6. 场景与费用样例数据

这样第三方拿到后可以直接进入联调，不需要再二次口头解释。

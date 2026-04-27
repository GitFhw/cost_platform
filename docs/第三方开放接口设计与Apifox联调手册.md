# 第三方开放接口设计与 Apifox 联调手册

## 1. 文档定位

本文档用于说明企业级成本核算平台第一版第三方开放接口的设计口径、调用顺序、请求参数、响应结构与联调建议，适合第三方系统、实施顾问与接口测试人员直接在 Apifox 中建模调用。

本版开放接口遵循以下原则：

- 接入中心是内部联调与模板准备工作台
- 开放接口是面向第三方系统的标准调用合同
- 二者共用同一套核算内核、运行快照与模板口径
- 第三方既可以按已发布生效版本调用，也可以按未发布草稿配置提前联调

## 2. 鉴权说明

第一版开放接口暂复用平台现有 `Bearer Token` 鉴权，不单独引入 `AppKey / AppSecret`。

Apifox 调用时请在全局请求头带上：

```http
Authorization: Bearer {token}
Content-Type: application/json
```

建议后续第二版再升级为独立应用级鉴权。

## 3. 快照口径说明

开放接口新增 `snapshotMode` 参数，用于控制当 `versionId` 未指定时走哪套配置：

| snapshotMode | 含义 | 使用场景 |
|---|---|---|
| `ACTIVE` | 按当前场景生效版本执行 | 正式联调、生产调用 |
| `DRAFT` | 按当前场景草稿配置执行 | 发布前联调、业务预演 |

规则如下：

- 如果传了 `versionId`，优先按该发布版本执行
- 如果没传 `versionId`：
  - `snapshotMode=ACTIVE`，按场景当前生效版本执行
  - `snapshotMode=DRAFT`，按场景当前草稿配置执行

## 4. 推荐调用顺序

第三方系统推荐按下面顺序接入：

1. 查询可接入场景
2. 查询场景发布版本与可用快照模式
3. 查询指定场景在指定快照下可核算费用
4. 查询某个或某些费用的输入模板
5. 按模板准备输入 JSON
6. 调用单费用或多费用核算接口
7. 如果报错，根据返回的中文字段校验提示补齐数据后重试

## 5. 开放接口清单

### 5.1 查询可接入场景

- 方法：`GET`
- 地址：`/cost/open/scenes`

#### 响应示例

```json
{
  "code": 200,
  "msg": "操作成功",
  "data": {
    "sceneCount": 2,
    "scenes": [
      {
        "sceneId": 1,
        "sceneCode": "SHOUGANG-ORE-HR-001",
        "sceneName": "首钢矿石人力费用",
        "businessDomain": "材料成本",
        "defaultObjectDimension": "协力单位",
        "activeVersionId": 12,
        "activeVersionNo": "V2026.04.002",
        "status": "0"
      }
    ]
  }
}
```

### 5.2 查询场景版本与快照模式

- 方法：`GET`
- 地址：`/cost/open/scenes/{sceneId}/versions`

#### 路径参数

| 参数 | 类型 | 必填 | 说明 |
|---|---|---|---|
| `sceneId` | Long | 是 | 场景主键 |

#### 响应示例

```json
{
  "code": 200,
  "msg": "操作成功",
  "data": {
    "scene": {
      "sceneId": 1,
      "sceneCode": "SHOUGANG-ORE-HR-001",
      "sceneName": "首钢矿石人力费用",
      "businessDomain": "材料成本",
      "defaultObjectDimension": "协力单位",
      "activeVersionId": 12,
      "activeVersionNo": "V2026.04.002",
      "status": "0"
    },
    "defaultSnapshotMode": "ACTIVE",
    "supportedSnapshotModes": [
      {
        "code": "ACTIVE",
        "label": "生效版本",
        "description": "未指定 versionId 时，按场景当前生效版本执行。"
      },
      {
        "code": "DRAFT",
        "label": "草稿配置",
        "description": "未指定 versionId 时，按场景当前草稿配置执行，适合联调未发布口径。"
      }
    ],
    "publishedVersions": [
      {
        "versionId": 12,
        "versionNo": "V2026.04.002",
        "versionStatus": "ACTIVE",
        "publishedTime": "2026-04-24 10:18:22"
      }
    ]
  }
}
```

### 5.3 查询指定快照下可核算费用

- 方法：`GET`
- 地址：`/cost/open/scenes/{sceneId}/fees`

#### 路径参数

| 参数 | 类型 | 必填 | 说明 |
|---|---|---|---|
| `sceneId` | Long | 是 | 场景主键 |

#### 查询参数

| 参数 | 类型 | 必填 | 说明 |
|---|---|---|---|
| `versionId` | Long | 否 | 指定发布版本主键 |
| `snapshotMode` | String | 否 | `ACTIVE` 或 `DRAFT`，默认 `ACTIVE` |

#### 响应示例

```json
{
  "code": 200,
  "msg": "操作成功",
  "data": {
    "scene": {
      "sceneId": 1,
      "sceneCode": "SHOUGANG-ORE-HR-001",
      "sceneName": "首钢矿石人力费用"
    },
    "requestedVersionId": 12,
    "snapshotMode": "ACTIVE",
    "feeCount": 14,
    "fees": [
      {
        "sceneId": 1,
        "sceneCode": "SHOUGANG-ORE-HR-001",
        "sceneName": "首钢矿石人力费用",
        "versionId": 12,
        "versionNo": "V2026.04.002",
        "snapshotSource": "PUBLISHED",
        "feeId": 101,
        "feeCode": "SG_FEMALE_SHIFT_LABOR",
        "feeName": "女工固定类劳务费",
        "unitCode": "元",
        "objectDimension": "协力单位",
        "sortNo": 20,
        "ruleCount": 1,
        "executionVariableCount": 3
      }
    ]
  }
}
```

### 5.4 查询费用输入模板

- 方法：`GET`
- 地址：`/cost/open/fee-template`

#### 查询参数

| 参数 | 类型 | 必填 | 说明 |
|---|---|---|---|
| `sceneId` | Long | 是 | 场景主键 |
| `versionId` | Long | 否 | 发布版本主键 |
| `snapshotMode` | String | 否 | `ACTIVE` 或 `DRAFT` |
| `feeIds` | String | 否 | 多个费用主键，逗号分隔 |
| `feeId` | Long | 否 | 单个费用主键 |
| `feeCode` | String | 否 | 单个费用编码 |
| `taskType` | String | 否 | 单笔或批量模板，可选 `FORMAL_SINGLE`、`FORMAL_BATCH`、`SIMULATION_SINGLE`、`SIMULATION_BATCH` |

#### 说明

- `feeIds`、`feeId`、`feeCode` 三者可任选其一
- 如果三者都不传，则按当前场景全费用执行链生成模板
- 多费用时会自动补齐依赖费用链，不需要第三方自行分析前置费用

#### 响应重点字段

| 字段 | 说明 |
|---|---|
| `snapshotSource` | 当前模板来自 `PUBLISHED` 还是 `DRAFT` |
| `fee` | 当前目标费用视图 |
| `targetFeeCodes` | 目标费用编码列表 |
| `executionFeeCodes` | 实际执行费用编码列表，已自动补齐依赖费用 |
| `dependentFeeCodes` | 依赖费用编码列表 |
| `inputJson` | 系统自动生成的示例输入 JSON |
| `inputContractFields` | 适合第三方消费的字段合同清单 |
| `requiredFieldCount` | 真正需要第三方显式传值的字段数；如果变量配置了默认值或兜底策略，则可能出现在模板里但不是必填 |

#### 响应示例

以下示例使用“苫盖零工劳务费”说明一个存在明确必填字段的费用模板：

```json
{
  "code": 200,
  "msg": "操作成功",
  "data": {
    "sceneId": 1,
    "sceneCode": "SHOUGANG-ORE-HR-001",
    "sceneName": "首钢矿石人力费用",
    "versionId": null,
    "versionNo": "草稿版本",
    "snapshotSource": "DRAFT",
    "snapshotMode": "DRAFT",
    "taskType": "FORMAL_SINGLE",
    "fee": {
      "feeCode": "SG_COVER_ODD_JOB_LABOR",
      "feeName": "苫盖零工劳务费"
    },
    "targetFeeCount": 1,
    "executionFeeCount": 1,
    "inputContractFieldCount": 3,
    "requiredFieldCount": 3,
    "inputContractFields": [
      {
        "variableCode": "COVER_ACTION",
        "variableName": "苫盖动作",
        "sourceType": "INPUT",
        "dataType": "STRING",
        "path": "coverWork.action",
        "pathLabel": "coverWork.action",
        "required": true,
        "defaultValue": null,
        "exampleValue": "COVER",
        "includedInTemplate": true,
        "templateRoles": ["CONDITION"]
      }
    ],
    "inputJson": "{...}",
    "integrationAdvice": [
      "当前模板来自草稿配置，适合联调未发布规则；正式接入上线前建议再按生效版本回归一次。",
      "当前模板已按目标费用及其依赖费用执行链收敛，只需要准备相关字段。"
    ]
  }
}
```

### 5.5 单费用或多费用核算

- 方法：`POST`
- 地址：`/cost/open/fee/calculate`

#### 请求体

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| `sceneId` | Long | 是 | 场景主键 |
| `versionId` | Long | 否 | 发布版本主键 |
| `snapshotMode` | String | 否 | `ACTIVE` 或 `DRAFT` |
| `feeId` | Long | 否 | 单个费用主键 |
| `feeIds` | Array<Long> | 否 | 多个费用主键 |
| `feeCode` | String | 否 | 单个费用编码 |
| `billMonth` | String | 否 | 账期，格式 `yyyy-MM` |
| `includeExplain` | Boolean | 否 | 是否返回轻量解释信息 |
| `inputJson` | String | 是 | JSON 对象或 JSON 对象数组的字符串 |

#### 单费用请求示例

```json
{
  "sceneId": 1,
  "snapshotMode": "DRAFT",
  "feeCode": "SG_FEMALE_SHIFT_LABOR",
  "includeExplain": true,
  "inputJson": "{\"bizNo\":\"OPEN-001\",\"femaleTeam\":{\"headcount\":6,\"actualAttendance\":6,\"requiredAttendance\":6}}"
}
```

#### 多费用请求示例

```json
{
  "sceneId": 1,
  "versionId": 12,
  "feeIds": [101, 102],
  "includeExplain": false,
  "inputJson": "[{\"bizNo\":\"OPEN-001\",\"femaleTeam\":{\"headcount\":6,\"actualAttendance\":6,\"requiredAttendance\":6},\"coverWork\":{\"action\":\"COVER\",\"cargoType\":\"COAL\",\"workloadTon\":1000}}]"
}
```

#### 成功响应示例

```json
{
  "code": 200,
  "msg": "操作成功",
  "data": {
    "sceneId": 1,
    "sceneCode": "SHOUGANG-ORE-HR-001",
    "sceneName": "首钢矿石人力费用",
    "versionId": 12,
    "versionNo": "V2026.04.002",
    "snapshotSource": "PUBLISHED",
    "snapshotMode": "ACTIVE",
    "fee": {
      "scope": "MULTI",
      "feeCodes": ["SG_FEMALE_SHIFT_LABOR", "SG_COVER_ODD_JOB_LABOR"]
    },
    "targetFeeCount": 2,
    "executionFeeCount": 2,
    "includeExplain": false,
    "inputCount": 1,
    "recordCount": 2,
    "successCount": 2,
    "noMatchCount": 0,
    "failedCount": 0,
    "durationMs": 12,
    "validationPassed": true,
    "records": [
      {
        "recordIndex": 1,
        "bizNo": "OPEN-001",
        "feeCode": "SG_FEMALE_SHIFT_LABOR",
        "feeName": "女工固定类劳务费",
        "status": "SUCCESS",
        "ruleCode": "SG_FEMALE_SHIFT_FORMULA_01",
        "amountValue": 7233.33
      }
    ]
  }
}
```

#### 缺字段错误响应示例

```json
{
  "code": 500,
  "msg": "输入数据缺少模板要求字段，请先补齐后再核算",
  "data": {
    "sceneId": 1,
    "sceneCode": "SHOUGANG-ORE-HR-001",
    "sceneName": "首钢矿石人力费用",
    "versionNo": "草稿版本",
    "snapshotSource": "DRAFT",
    "snapshotMode": "DRAFT",
    "validationPassed": false,
    "missingFieldCount": 2,
    "validationMessages": [
      {
        "recordIndex": 1,
        "bizNo": "OPEN-VAL-001",
        "variableCode": "FEMALE_ACTUAL_ATTENDANCE",
        "variableName": "女工实际出勤",
        "path": "femaleTeam.actualAttendance",
        "message": "缺少必需字段 女工实际出勤，请按来源路径补齐：femaleTeam.actualAttendance"
      }
    ]
  }
}
```

## 6. Apifox 建议配置

### 6.1 环境变量

建议在 Apifox 配置以下环境变量：

- `{{host}}`：接口域名，例如 `http://127.0.0.1:8080`
- `{{token}}`：登录后 Bearer Token
- `{{sceneId}}`：联调场景主键
- `{{versionId}}`：发布版本主键

### 6.2 全局请求头

```http
Authorization: Bearer {{token}}
Content-Type: application/json
```

### 6.3 调用分组建议

Apifox 中建议按下列分组建接口：

1. 开放场景
2. 开放版本
3. 开放费用
4. 开放模板
5. 开放核算

## 7. 联调建议

- 第三方不要手工猜字段，请先调用费用模板接口，再按 `inputContractFields` 组织请求
- 如果业务尚未发布，请统一使用 `snapshotMode=DRAFT`
- 如果准备上线，请改用 `versionId` 或 `snapshotMode=ACTIVE` 再回归
- 多费用核算时，只关心目标费用即可，平台会自动补齐依赖费用执行链
- 字段缺失时优先看 `validationMessages`，里面已经带了中文名称和来源路径

## 8. 当前压测基线

以下为当前环境手工压测基线，可作为第三方联调预期参考：

### 8.1 全费用同步取价

- 输入记录数：1000
- 目标费用数：14
- 实际执行费用数：14
- 总耗时：1723ms
- 吞吐：580.38 records/s

### 8.2 单费用同步取价

- 输入记录数：1000
- 目标费用数：1
- 实际执行费用数：1
- 总耗时：417ms
- 吞吐：2398.08 records/s

### 8.3 多费用同步取价

- 输入记录数：1000
- 目标费用数：2
- 实际执行费用数：11
- 总耗时：2058ms
- 吞吐：485.91 records/s

说明：

- 多费用场景中执行费用数大于目标费用数，是因为平台自动补齐了依赖费用链
- 这是企业级核算产品的预期行为，有助于保证第三方请求结果完整、稳定、可解释

## 9. 后续规划

后续建议继续演进：

1. 第二版开放接口鉴权升级为 `AppKey / AppSecret / 签名`
2. 增加开放接口调用日志与限流能力
3. 增加按场景导出标准字段字典与对象结构示意
4. 提供 OpenAPI/Swagger JSON，便于 Apifox 一键导入

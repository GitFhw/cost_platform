# cost_platform

企业级核算平台重构底座。

## 目录结构

- `cost_platform_back`：基于若依后端底座的核算平台服务端工程
- `cost_platform_front`：基于若依 Vue3 的核算平台前端工程
- 根目录文档：产品设计、SQL、ER 图、页面原型、缓存规范和开发验证规范

## 当前目标

基于若依前后端分离底座，逐步重构一套企业级核算平台，先把业务人员最常维护的主链做清楚：

- 场景中心
- 费用中心
- 规则中心
- 阶梯规则
- 发布、追溯与治理能力

## 当前重构主线

1. 独立系统字典维护
2. 配置中心业务域字典化
3. 费用中心对齐老项目“基础费目维护”
4. 规则中心逐步改造成“费用 -> 规则 -> 阶梯”的维护模式

## 核心文档

- `项目说明.md`
- `核算平台设计基线.md`
- `企业级核算平台详细设计.md`
- `核算模块开发规范.md`
- `核算平台缓存设计与Redis规范.md`
- `核算平台开发与验证规范.md`
- `首页与底座裁剪设计.md`
- `企业级核算平台完整数据库设计.sql`
- `企业级核算平台ER图.md`
- `企业级核算平台页面原型设计.md`
- `成本核算平台需求跟踪矩阵.xlsx`

## 数据初始化

- 若依基础库脚本：`cost_platform_back/sql/ry_20260320.sql`
- 核算平台业务表基线：`企业级核算平台完整数据库设计.sql`
- 线程一基础治理增量脚本：`cost_platform_back/sql/cost_thread1_governance_20260330.sql`
- 平台必要初始化快照：`cost_platform_back/cost_admin/src/main/resources/db/cost_init.sql`
- 带测试配置初始化快照：`cost_platform_back/cost_admin/src/main/resources/db/cost_init_demo.sql`
- 初始化快照生成脚本：`cost_platform_back/bin/build-cost-init.ps1`

## Flyway 迁移

- Flyway 迁移目录：`cost_platform_back/cost_admin/src/main/resources/db/migration`
- `B20260330_000__platform_baseline.sql`：整合若依基础库和核算平台业务表基线，面向空库初始化
- `V20260330_001__thread1_governance.sql`：线程一基础治理增量，包含核算菜单前置、若依官网默认隐藏
- `V20260330_002__demo_configuration_examples.sql`：面向业务熟悉场景配置的示例数据，包含示例场景、费用、变量、规则和阶梯
- `V20260330_003__quartz_schema.sql`：Quartz 调度表迁移，对应 `cost_platform_back/sql/quartz.sql`

推荐使用方式：

- 全新测试库或培训库：直接创建空库，启动应用后由 Flyway 自动完成基线和增量迁移
- 已有开发库：先确认库内已经具备若依基础表和核算平台业务表基线，再让 Flyway 做 baseline 和后续增量
- 不要只手工导入若依原始库后再执行 Flyway。当前基线脚本把若依基础表和核算平台业务表放在同一个 baseline 里，只导若依原始库会导致 Flyway 跳过业务表基线
- 如果需要交付“一次性初始化 SQL”给空库环境，可使用：
- `cost_init.sql`：只包含支撑核算平台运行的必要表、菜单、字典、治理配置等
- `cost_init_demo.sql`：在 `cost_init.sql` 基础上，额外带测试场景、费用、变量、公式、规则等配置数据，但不带发布版本、账期、试算、任务、结果、告警等运行态记录
- 两份快照都由 `build-cost-init.ps1` 从当前 migration 快照统一生成，生成后应与最新 migration 目录一起复核

补充说明：

- `cost_platform_back/sql/cost_thread1_governance_20260330.sql` 仍保留为历史参考和人工补库兜底脚本，但后续新增 SQL 以 Flyway migration 为主
- 示例数据主要用于帮助业务人员熟悉场景中心的配置方式，建议优先投放到测试、培训或演示环境

## 说明

当前仓库是新的产品重构工作区。后续开发、构建验证、提交和交付，都以根目录文档为统一基线。

## 页面数据源约定

- 页面列表、详情、下拉和指标卡默认直接查询后台接口
- 需要共享缓存时，统一由后端接入 Redis，前端只消费后端返回结果
- 不再使用前端本地 store、localStorage 或 sessionStorage 把字典和主数据缓存成事实来源，避免多人同时操作时口径不一致

## 后端持久层约定

- 当前后端已集成 MyBatis-Plus，采用“若依原生 MyBatis XML + MyBatis-Plus”渐进混合模式
- 单表 CRUD、唯一校验、条件构造优先使用 `BaseMapper`、`LambdaQueryWrapper`
- 跨表统计、治理预检查、复杂报表 SQL 继续保留在 XML Mapper 中
- 同一个分页查询链路不要同时混用 `PageHelper` 和 MyBatis-Plus `Page`，避免重复分页
- 旧模块不做一次性推翻重写，按线程逐步改造成 MyBatis-Plus 样式
- 基础审计字段统一沿用若依原生 `BaseEntity`，新 MP 模块通过 `SecurityUtils` 自动填充创建人、修改人、创建时间、修改时间

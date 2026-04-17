<template>
  <div class="app-container architecture-page" :class="{ 'is-compact-mode': isCompactMode }">
    <section class="architecture-page__hero">
      <div class="architecture-page__hero-copy">
        <div class="architecture-page__eyebrow">数据架构</div>
        <h2 class="architecture-page__title">核算平台数据架构视图</h2>
        <p class="architecture-page__subtitle">
          把数据接入、标准对象、场景建模、发布快照、执行运行和结果追溯放进同一张结构图，
          让研发、产品、实施和运维在同一页里对齐“数据从哪来、在哪定价、按什么版本执行、最终落到哪里”。
        </p>
        <div class="architecture-page__hero-actions">
          <el-button type="primary" icon="Connection" @click="openRoute(COST_MENU_ROUTES.access)">打开数据接入</el-button>
          <el-button icon="MagicStick" @click="openRoute(COST_MENU_ROUTES.simulation)">打开试算中心</el-button>
          <el-button icon="Promotion" @click="openRoute(COST_MENU_ROUTES.publish)">打开发布中心</el-button>
        </div>
      </div>

      <div class="architecture-page__hero-card">
        <span class="architecture-page__hero-label">架构主线</span>
        <strong class="architecture-page__hero-value">输入 → 建模 → 快照 → 执行 → 追溯</strong>
        <small class="architecture-page__hero-desc">当前页面基于项目实际表结构与运行链路整理，不是脱离系统的静态原型图。</small>
        <div class="architecture-page__hero-tags">
          <el-tag effect="plain">MySQL 8 优先</el-tag>
          <el-tag effect="plain">场景级版本快照</el-tag>
          <el-tag effect="plain">结果可解释回放</el-tag>
        </div>
      </div>
    </section>

    <section v-show="!isCompactMode" class="architecture-page__metric-grid">
      <article v-for="item in metrics" :key="item.label" class="architecture-page__metric">
        <span>{{ item.label }}</span>
        <strong>{{ item.value }}</strong>
        <small>{{ item.desc }}</small>
      </article>
    </section>

    <section class="architecture-page__panel">
      <div class="architecture-page__section-head">
        <div>
          <div class="architecture-page__eyebrow architecture-page__eyebrow--sub">主数据流</div>
          <h3>从业务输入到结果台账的主链路</h3>
          <p>用分层方式把当前平台的关键数据面拆开，避免场景配置、发布快照和执行结果混成一团。</p>
        </div>
        <span class="architecture-page__section-badge">共 {{ architectureLayers.length }} 层</span>
      </div>

      <div class="architecture-page__flow">
        <article v-for="(layer, index) in architectureLayers" :key="layer.title" class="architecture-page__flow-card">
          <div class="architecture-page__flow-top">
            <span class="architecture-page__flow-index">L{{ `${index + 1}`.padStart(2, '0') }}</span>
            <span class="architecture-page__flow-type">{{ layer.type }}</span>
          </div>
          <h4>{{ layer.title }}</h4>
          <p>{{ layer.desc }}</p>
          <div class="architecture-page__chips">
            <span v-for="chip in layer.items" :key="`${layer.title}-${chip}`" class="architecture-page__chip">{{ chip }}</span>
          </div>
        </article>
      </div>
    </section>

    <section class="architecture-page__duo">
      <article class="architecture-page__panel">
        <div class="architecture-page__section-head">
          <div>
            <h3>数据库支持</h3>
            <p>沿用老项目的数据库选择思路，但改成更贴近当前单体 SpringBoot 版本的落地建议。</p>
          </div>
          <span class="architecture-page__section-badge">部署口径</span>
        </div>

        <div class="architecture-page__database-grid">
          <div v-for="item in databaseOptions" :key="item.name" class="architecture-page__database-card">
            <div class="architecture-page__database-name">{{ item.name }}</div>
            <div class="architecture-page__database-role">{{ item.role }}</div>
            <ul>
              <li v-for="point in item.points" :key="`${item.name}-${point}`">{{ point }}</li>
            </ul>
          </div>
        </div>
      </article>

      <article class="architecture-page__panel">
        <div class="architecture-page__section-head">
          <div>
            <h3>关键关系</h3>
            <p>这里不是 ER 图全量展开，而是把最关键的主外键依赖和运行依赖先拉直。</p>
          </div>
          <span class="architecture-page__section-badge">依赖线</span>
        </div>

        <div class="architecture-page__relations">
          <div v-for="item in relationChains" :key="item.title" class="architecture-page__relation">
            <div class="architecture-page__relation-title">{{ item.title }}</div>
            <div class="architecture-page__relation-track">
              <span v-for="(node, idx) in item.nodes" :key="`${item.title}-${node}`" class="architecture-page__relation-node">
                {{ node }}
                <i v-if="idx < item.nodes.length - 1" class="architecture-page__relation-arrow">→</i>
              </span>
            </div>
            <div class="architecture-page__relation-note">{{ item.note }}</div>
          </div>
        </div>
      </article>
    </section>

    <section class="architecture-page__panel">
      <div class="architecture-page__section-head">
        <div>
          <div class="architecture-page__eyebrow architecture-page__eyebrow--sub">核心表</div>
          <h3>核心表分组</h3>
          <p>把表按职责拆成建模、版本、运行和追溯四个面，方便快速判断字段应该放在哪里维护。</p>
        </div>
        <span class="architecture-page__section-badge">真实表结构</span>
      </div>

      <el-table :data="tableGroups" border class="architecture-page__table">
        <el-table-column label="模块" prop="module" width="180" />
        <el-table-column label="表清单" min-width="520">
          <template #default="{ row }">
            <div class="architecture-page__table-tags">
              <el-tag v-for="tableName in row.tables" :key="`${row.module}-${tableName}`" size="small" effect="plain">
                {{ tableName }}
              </el-tag>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="说明" prop="desc" min-width="260" show-overflow-tooltip />
      </el-table>
    </section>

    <section class="architecture-page__duo">
      <article class="architecture-page__panel">
        <div class="architecture-page__section-head">
          <div>
            <h3>快照与追溯边界</h3>
            <p>企业级核算平台最怕的是“结果能算出来，但回不去”，所以发布快照和解释回放必须有清晰边界。</p>
          </div>
          <span class="architecture-page__section-badge">治理重点</span>
        </div>

        <div class="architecture-page__checklist">
          <div v-for="item in governanceChecks" :key="item.title" class="architecture-page__check-item">
            <strong>{{ item.title }}</strong>
            <p>{{ item.desc }}</p>
          </div>
        </div>
      </article>

      <article class="architecture-page__panel">
        <div class="architecture-page__section-head">
          <div>
            <h3>页面入口</h3>
            <p>这页不是孤立文档，而是为建模、试算、发布和结果追溯提供统一认知底图。</p>
          </div>
          <span class="architecture-page__section-badge">工作台联动</span>
        </div>

        <div class="architecture-page__entry-grid">
          <button
            v-for="item in entryActions"
            :key="item.title"
            type="button"
            class="architecture-page__entry-card"
            @click="openRoute(item.route)"
          >
            <span>{{ item.title }}</span>
            <strong>{{ item.meta }}</strong>
            <small>{{ item.desc }}</small>
          </button>
        </div>
      </article>
    </section>
  </div>
</template>

<script setup name="CostDataArchitecture">
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import useSettingsStore from '@/store/modules/settings'
import { COST_MENU_ROUTES } from '@/utils/costMenuRoutes'

const router = useRouter()
const settingsStore = useSettingsStore()
const isCompactMode = computed(() => settingsStore.costPageMode === 'COMPACT')

const metrics = [
  {
    label: '架构层级',
    value: '6 层',
    desc: '输入、标准对象、建模、快照、执行、追溯六层分开设计'
  },
  {
    label: '核心主表',
    value: '18+',
    desc: '覆盖场景、费用、变量、规则、发布、任务、结果与审计'
  },
  {
    label: '主治理口径',
    value: '场景级',
    desc: '配置、发布、执行和回看都围绕场景建立闭环'
  },
  {
    label: '追溯能力',
    value: '可回放',
    desc: '试算、结果台账和命中解释支持回到版本与规则链路'
  }
]

const architectureLayers = [
  {
    type: '输入层',
    title: '业务输入与接入',
    desc: '承接业务系统宽表、HTTP 接口、手工 JSON、导入批次等原始输入，先解决“数据从哪来”。',
    items: ['cost_access_profile', '原始 JSON', 'HTTP 拉取', '导入批次', '映射配置']
  },
  {
    type: '标准层',
    title: '标准计费对象',
    desc: '把不同来源统一成标准计费对象，确保 objectDimension、objectCode、objectName、bizNo 等基础语义一致。',
    items: ['标准计费对象', '模板字段', 'objectDimension', 'objectCode', 'bizNo']
  },
  {
    type: '建模层',
    title: '场景建模与规则配置',
    desc: '围绕场景、费用、变量、公式和规则完成口径建模，是整个平台最核心的配置面。',
    items: ['cost_scene', 'cost_fee_item', 'cost_variable_group', 'cost_variable', 'cost_formula', 'cost_rule']
  },
  {
    type: '版本层',
    title: '发布版本与快照冻结',
    desc: '把配置固化成场景级版本，运行时不直接认在线配置，而是认快照，保证可复现。',
    items: ['cost_publish_version', 'cost_publish_snapshot', 'active_version_id', '版本差异']
  },
  {
    type: '执行层',
    title: '试算、任务与分片执行',
    desc: '区分单笔试算、批量试算、正式核算与分片任务，把验证链路和生产链路分开。',
    items: ['cost_simulation_record', 'cost_calc_task', 'cost_calc_task_detail', 'cost_calc_task_partition']
  },
  {
    type: '结果层',
    title: '结果台账与解释追溯',
    desc: '结果不是只落金额，还要落命中解释、变量值和追溯信息，才能支撑审计与差异分析。',
    items: ['cost_result_ledger', 'cost_result_trace', 'cost_audit_log', 'cost_alarm_record']
  }
]

const databaseOptions = [
  {
    name: 'MySQL 8',
    role: '当前 SpringBoot 单体版优先',
    points: ['事务一致性成熟', 'JSON 字段可用', '窗口函数和索引能力够用', '本地部署和运维门槛低']
  },
  {
    name: 'PostgreSQL',
    role: '后续微服务和复杂检索可重点评估',
    points: ['JSONB 能力更强', '复杂查询与表达式索引更丰富', '快照检索和结构化搜索更灵活', '适合中长期演进']
  },
  {
    name: 'SQLite',
    role: '原型、离线演示和单机验模可选',
    points: ['零部署', '适合小型原型', '不适合作为正式批量核算主库', '可做本地 demo 或工具页缓存']
  }
]

const relationChains = [
  {
    title: '场景建模链',
    nodes: ['cost_scene', 'cost_fee_item', 'cost_variable / cost_formula', 'cost_rule', 'cost_rule_condition / tier'],
    note: '所有定价能力都以 scene_id 为命名空间锚点，再按 fee_id 组织规则边界。'
  },
  {
    title: '发布执行链',
    nodes: ['cost_publish_version', 'cost_publish_snapshot', 'cost_simulation_record / cost_calc_task', 'cost_result_ledger'],
    note: '发布后形成快照，试算和正式核算都优先认 version_id 对应的快照口径。'
  },
  {
    title: '输入追溯链',
    nodes: ['cost_access_profile', 'cost_calc_input_batch', 'cost_calc_input_batch_item', 'cost_calc_task'],
    note: '把原始输入、标准对象和正式任务串起来，后续问题定位能回到输入批次。'
  }
]

const tableGroups = [
  {
    module: '场景与主数据',
    tables: ['cost_scene', 'cost_fee_item', 'cost_fee_variable_rel'],
    desc: '定义场景边界、费用主数据以及费用与变量的直接引用关系。'
  },
  {
    module: '变量与公式',
    tables: ['cost_variable_group', 'cost_variable', 'cost_formula', 'cost_formula_version'],
    desc: '承载输入变量、公式变量、版本化公式以及变量分组。'
  },
  {
    module: '规则与定价',
    tables: ['cost_rule', 'cost_rule_condition', 'cost_rule_tier'],
    desc: '定义命中条件、阶梯区间、定价方式和优先级。'
  },
  {
    module: '发布治理',
    tables: ['cost_publish_version', 'cost_publish_snapshot'],
    desc: '冻结场景配置快照，支持差异对比、生效切换和历史回看。'
  },
  {
    module: '接入与输入',
    tables: ['cost_access_profile', 'cost_calc_input_batch', 'cost_calc_input_batch_item'],
    desc: '承接业务源接入方案、输入批次与标准对象明细。'
  },
  {
    module: '试算与正式核算',
    tables: ['cost_simulation_record', 'cost_calc_task', 'cost_calc_task_detail', 'cost_calc_task_partition'],
    desc: '区分试算记录、正式任务、任务明细与分片执行面。'
  },
  {
    module: '结果与治理',
    tables: ['cost_result_ledger', 'cost_result_trace', 'cost_bill_period', 'cost_recalc_order', 'cost_audit_log', 'cost_alarm_record'],
    desc: '沉淀结果台账、解释链路、账期治理、重算流程、审计与告警。'
  }
]

const governanceChecks = [
  {
    title: '配置不直接上运行',
    desc: '场景、费用、变量、规则修改后不直接进入生产运行，必须通过发布版本冻结配置快照。'
  },
  {
    title: '试算与正式结果分仓',
    desc: 'cost_simulation_record 只服务验证和回归，正式核算结果进入 cost_result_ledger，避免口径混淆。'
  },
  {
    title: '输入源可追到批次',
    desc: '正式核算建议尽量经过接入方案或输入批次，保留从原始输入到任务执行的完整回放路径。'
  },
  {
    title: '结果必须能解释',
    desc: '结果台账之外，还要有 result trace、命中规则、变量求值和执行步骤，才能支撑审计和差异分析。'
  }
]

const entryActions = [
  {
    title: '数据接入中心',
    meta: '原始输入 → 标准对象',
    desc: '查看字段映射、模板生成、导入批次和预演对象。',
    route: COST_MENU_ROUTES.access
  },
  {
    title: '试算中心',
    meta: '标准对象 → 结果解释',
    desc: '回看变量求值、命中链路、计费展开与批量回归。',
    route: COST_MENU_ROUTES.simulation
  },
  {
    title: '发布中心',
    meta: '配置 → 快照版本',
    desc: '查看发布前检查、版本差异、生效切换与回滚。',
    route: COST_MENU_ROUTES.publish
  },
  {
    title: '结果台账',
    meta: '正式结果 → 追溯治理',
    desc: '按任务、场景、版本和费用口径回看正式核算结果。',
    route: COST_MENU_ROUTES.result
  }
]

function openRoute(path) {
  router.push(path)
}
</script>

<style scoped lang="scss">
.architecture-page {
  --arch-bg: linear-gradient(
    180deg,
    color-mix(in srgb, var(--el-bg-color-page) 82%, #dfe9ff 18%) 0%,
    color-mix(in srgb, var(--el-bg-color-page) 90%, #f0eadb 10%) 260px,
    var(--el-bg-color-page) 100%
  );
  --arch-card-bg: color-mix(in srgb, var(--el-bg-color-overlay) 94%, #fffaf1 6%);
  --arch-panel-bg: color-mix(in srgb, var(--el-bg-color-overlay) 96%, #fffdf8 4%);
  --arch-soft-bg: color-mix(in srgb, var(--el-bg-color-overlay) 90%, #f8ecd9 10%);
  --arch-border: color-mix(in srgb, var(--el-border-color) 74%, #d7b884 26%);
  --arch-shadow: 0 18px 42px rgb(35 47 69 / 0.08);
  --arch-accent: color-mix(in srgb, var(--el-color-primary) 76%, #16837a 24%);
  --arch-text: var(--el-text-color-primary);
  --arch-muted: var(--el-text-color-secondary);
  --arch-link: color-mix(in srgb, var(--el-color-primary) 72%, #4f8df3 28%);
  display: grid;
  gap: 18px;
  min-height: calc(100dvh - 124px);
  padding-bottom: 16px;
  background: var(--arch-bg);
}

:global(html.dark) .architecture-page {
  --arch-bg: linear-gradient(
    180deg,
    color-mix(in srgb, var(--el-bg-color-page) 80%, #2e3947 20%) 0%,
    color-mix(in srgb, var(--el-bg-color-page) 92%, #251d18 8%) 260px,
    var(--el-bg-color-page) 100%
  );
  --arch-card-bg: color-mix(in srgb, var(--el-bg-color-overlay) 94%, #261d16 6%);
  --arch-panel-bg: color-mix(in srgb, var(--el-bg-color-overlay) 96%, #211914 4%);
  --arch-soft-bg: color-mix(in srgb, var(--el-bg-color-overlay) 90%, #31261d 10%);
  --arch-border: color-mix(in srgb, var(--el-border-color) 70%, #806244 30%);
  --arch-shadow: 0 20px 44px rgb(0 0 0 / 0.26);
  --arch-accent: color-mix(in srgb, var(--el-color-primary) 72%, #79e7dd 28%);
  --arch-link: color-mix(in srgb, var(--el-color-primary) 68%, #a7c8ff 32%);
}

.architecture-page__hero,
.architecture-page__panel,
.architecture-page__metric,
.architecture-page__flow-card,
.architecture-page__database-card,
.architecture-page__entry-card {
  border: 1px solid var(--arch-border);
  border-radius: 28px;
  background: var(--arch-card-bg);
  box-shadow: var(--arch-shadow);
}

.architecture-page__hero,
.architecture-page__panel {
  position: relative;
  overflow: hidden;
  padding: 28px 30px;
  background:
    radial-gradient(circle at top right, rgb(92 144 255 / 0.12), transparent 36%),
    radial-gradient(circle at left bottom, rgb(227 173 84 / 0.12), transparent 28%),
    var(--arch-panel-bg);
}

.architecture-page__hero::after,
.architecture-page__panel::after {
  content: '';
  position: absolute;
  inset: 0;
  pointer-events: none;
  background-image: linear-gradient(rgb(255 255 255 / 0.06) 1px, transparent 1px), linear-gradient(90deg, rgb(255 255 255 / 0.05) 1px, transparent 1px);
  background-size: 28px 28px;
  mask-image: linear-gradient(180deg, rgb(0 0 0 / 0.3), transparent 70%);
}

.architecture-page__hero {
  display: grid;
  grid-template-columns: minmax(0, 1.45fr) minmax(320px, 0.75fr);
  gap: 18px;
}

.architecture-page__hero-copy,
.architecture-page__hero-card {
  position: relative;
  z-index: 1;
}

.architecture-page__eyebrow {
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  color: var(--arch-accent);
}

.architecture-page__eyebrow--sub {
  margin-bottom: 8px;
}

.architecture-page__title {
  margin: 10px 0 0;
  font-size: 34px;
  line-height: 1.16;
  color: var(--arch-text);
  font-family: 'Alibaba PuHuiTi 3.0', 'HarmonyOS Sans SC', 'Microsoft YaHei', sans-serif;
}

.architecture-page__subtitle {
  max-width: 840px;
  margin: 14px 0 0;
  color: var(--arch-muted);
  line-height: 1.9;
}

.architecture-page__hero-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  margin-top: 22px;
}

.architecture-page__hero-card {
  display: grid;
  align-content: space-between;
  gap: 14px;
  min-height: 220px;
  padding: 24px;
  border-radius: 24px;
  border: 1px solid var(--arch-border);
  background: linear-gradient(180deg, rgb(255 255 255 / 0.34), rgb(255 255 255 / 0.08)), var(--arch-soft-bg);
}

.architecture-page__hero-label {
  color: var(--arch-muted);
  font-size: 13px;
}

.architecture-page__hero-value {
  color: var(--arch-text);
  font-size: 30px;
  line-height: 1.2;
}

.architecture-page__hero-desc {
  color: var(--arch-muted);
  line-height: 1.8;
}

.architecture-page__hero-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.architecture-page__metric-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 14px;
}

.architecture-page__metric {
  display: grid;
  gap: 8px;
  padding: 18px 20px;
}

.architecture-page__metric span,
.architecture-page__database-role,
.architecture-page__section-badge,
.architecture-page__relation-note,
.architecture-page__check-item p,
.architecture-page__entry-card small {
  color: var(--arch-muted);
  font-size: 13px;
}

.architecture-page__metric strong {
  color: var(--arch-accent);
  font-size: 30px;
  line-height: 1.1;
}

.architecture-page__section-head {
  position: relative;
  z-index: 1;
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 16px;
}

.architecture-page__section-head h3 {
  margin: 0;
  font-size: 24px;
  color: var(--arch-text);
}

.architecture-page__section-head p {
  margin: 10px 0 0;
  color: var(--arch-muted);
  line-height: 1.8;
}

.architecture-page__section-badge {
  display: inline-flex;
  align-items: center;
  min-height: 34px;
  padding: 0 14px;
  border-radius: 999px;
  border: 1px solid var(--arch-border);
  background: color-mix(in srgb, var(--arch-panel-bg) 86%, #fff 14%);
  white-space: nowrap;
}

.architecture-page__flow {
  position: relative;
  z-index: 1;
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 16px;
  margin-top: 22px;
}

.architecture-page__flow-card {
  display: grid;
  gap: 12px;
  padding: 20px;
}

.architecture-page__flow-top {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: center;
}

.architecture-page__flow-index,
.architecture-page__flow-type {
  color: var(--arch-link);
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.06em;
  text-transform: uppercase;
}

.architecture-page__flow-card h4,
.architecture-page__database-name,
.architecture-page__relation-title,
.architecture-page__check-item strong,
.architecture-page__entry-card strong {
  margin: 0;
  color: var(--arch-text);
  font-size: 18px;
  font-weight: 700;
}

.architecture-page__flow-card p,
.architecture-page__check-item p,
.architecture-page__entry-card small {
  margin: 0;
  line-height: 1.8;
}

.architecture-page__chips,
.architecture-page__table-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.architecture-page__chip {
  display: inline-flex;
  align-items: center;
  min-height: 30px;
  padding: 0 12px;
  border-radius: 999px;
  color: var(--arch-link);
  background: color-mix(in srgb, var(--arch-soft-bg) 76%, transparent);
  border: 1px solid color-mix(in srgb, var(--arch-border) 78%, transparent);
  font-size: 12px;
}

.architecture-page__duo {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 18px;
}

.architecture-page__database-grid,
.architecture-page__relations,
.architecture-page__checklist,
.architecture-page__entry-grid {
  position: relative;
  z-index: 1;
  display: grid;
  gap: 14px;
  margin-top: 20px;
}

.architecture-page__database-grid {
  grid-template-columns: repeat(3, minmax(0, 1fr));
}

.architecture-page__database-card {
  padding: 18px 20px;
}

.architecture-page__database-card ul {
  margin: 14px 0 0;
  padding-left: 18px;
  color: var(--arch-muted);
  line-height: 1.9;
}

.architecture-page__relations {
  align-content: start;
}

.architecture-page__relation {
  display: grid;
  gap: 10px;
  padding: 18px 20px;
  border-radius: 20px;
  border: 1px solid var(--arch-border);
  background: color-mix(in srgb, var(--arch-soft-bg) 70%, transparent);
}

.architecture-page__relation-track {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  align-items: center;
}

.architecture-page__relation-node {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  color: var(--arch-text);
  font-weight: 600;
}

.architecture-page__relation-arrow {
  color: var(--arch-link);
  font-style: normal;
}

.architecture-page__table {
  position: relative;
  z-index: 1;
  margin-top: 22px;
}

.architecture-page__check-item {
  padding: 18px 20px;
  border-radius: 20px;
  border: 1px dashed var(--arch-border);
  background: color-mix(in srgb, var(--arch-soft-bg) 68%, transparent);
}

.architecture-page__entry-grid {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.architecture-page__entry-card {
  display: grid;
  gap: 10px;
  padding: 18px 20px;
  text-align: left;
  cursor: pointer;
  transition: transform 0.2s ease, border-color 0.2s ease, box-shadow 0.2s ease;
}

.architecture-page__entry-card:hover {
  transform: translateY(-2px);
  border-color: color-mix(in srgb, var(--arch-accent) 34%, var(--arch-border) 66%);
  box-shadow: 0 22px 36px rgb(40 59 95 / 0.12);
}

.architecture-page :deep(.el-table) {
  --el-table-header-bg-color: color-mix(in srgb, var(--arch-soft-bg) 76%, var(--el-bg-color-page) 24%);
  --el-table-tr-bg-color: color-mix(in srgb, var(--arch-card-bg) 92%, var(--el-bg-color-page) 8%);
  --el-table-border-color: var(--arch-border);
  --el-table-row-hover-bg-color: color-mix(in srgb, var(--arch-soft-bg) 82%, var(--el-bg-color-page) 18%);
}

@media (max-width: 1500px) {
  .architecture-page__flow,
  .architecture-page__database-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 1200px) {
  .architecture-page__hero,
  .architecture-page__duo,
  .architecture-page__metric-grid,
  .architecture-page__flow,
  .architecture-page__database-grid,
  .architecture-page__entry-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 768px) {
  .architecture-page__hero,
  .architecture-page__panel {
    padding: 20px;
    border-radius: 22px;
  }

  .architecture-page__title {
    font-size: 28px;
  }
}
</style>

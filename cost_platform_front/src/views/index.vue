<template>
  <div class="app-container cost-dashboard">
    <section class="dashboard-hero">
      <div class="dashboard-hero__main">
        <div class="dashboard-hero__eyebrow">企业级成本核算平台</div>
        <h1 class="dashboard-hero__title">面向多行业、多场景、多费用的统一核算驾驶舱</h1>
        <p class="dashboard-hero__subtitle">
          统一承接多行业、多场景、多费用对象的规则配置、版本管理、批量核算与结果追溯，帮助企业在同一平台完成成本口径治理与运行管理。
        </p>
        <div class="dashboard-hero__tags">
          <el-tag type="success">多场景统一配置</el-tag>
          <el-tag type="info">版本化运行管理</el-tag>
          <el-tag type="warning">结果追溯与治理联动</el-tag>
        </div>
      </div>
      <div class="dashboard-hero__aside">
        <div class="dashboard-highlight-card">
          <span class="dashboard-highlight-card__label">设计目标</span>
          <strong class="dashboard-highlight-card__value">100万+/月</strong>
          <span class="dashboard-highlight-card__desc">面向高并发计费、发布治理和结果追溯的企业级平台</span>
        </div>
      </div>
    </section>

    <section class="dashboard-metrics">
      <div v-for="item in metricItems" :key="item.label" class="dashboard-metric-card">
        <span class="dashboard-metric-card__label">{{ item.label }}</span>
        <strong class="dashboard-metric-card__value">{{ item.value }}</strong>
        <span class="dashboard-metric-card__desc">{{ item.desc }}</span>
      </div>
    </section>

    <section class="dashboard-grid">
      <el-card shadow="never" class="dashboard-panel dashboard-panel--wide">
        <template #header>
          <div class="dashboard-panel__header">
            <span>核算主线</span>
            <span class="dashboard-panel__tip">围绕业务配置、版本治理与运行追踪组织平台能力</span>
          </div>
        </template>
        <div class="dashboard-shortcuts">
          <div v-for="item in shortcuts" :key="item.title" class="dashboard-shortcut">
            <el-icon class="dashboard-shortcut__icon"><component :is="item.icon" /></el-icon>
            <div class="dashboard-shortcut__body">
              <div class="dashboard-shortcut__title">{{ item.title }}</div>
              <div class="dashboard-shortcut__desc">{{ item.desc }}</div>
              <div class="dashboard-shortcut__meta">{{ item.meta }}</div>
            </div>
          </div>
        </div>
      </el-card>

      <el-card shadow="never" class="dashboard-panel">
        <template #header>
          <div class="dashboard-panel__header">
            <span>核心能力</span>
            <span class="dashboard-panel__tip">统一呈现平台能力边界与关键治理要求</span>
          </div>
        </template>
        <div class="dashboard-list">
          <div v-for="item in designAnchors" :key="item.title" class="dashboard-list-item">
            <div class="dashboard-list-item__title">{{ item.title }}</div>
            <div class="dashboard-list-item__meta">{{ item.desc }}</div>
          </div>
        </div>
      </el-card>

      <el-card shadow="never" class="dashboard-panel">
        <template #header>
          <div class="dashboard-panel__header">
            <span>平台特性</span>
            <span class="dashboard-panel__tip">突出当前平台定位与运行目标，不展示模拟经营数据</span>
          </div>
        </template>
        <div class="dashboard-governance">
          <div v-for="item in governanceItems" :key="item.label" class="dashboard-governance__item">
            <span class="dashboard-governance__label">{{ item.label }}</span>
            <strong class="dashboard-governance__value">{{ item.value }}</strong>
          </div>
        </div>
      </el-card>

      <el-card shadow="never" class="dashboard-panel dashboard-panel--wide">
        <template #header>
          <div class="dashboard-panel__header">
            <span>应用路径</span>
            <span class="dashboard-panel__tip">从业务建模到运行追溯，形成完整使用闭环</span>
          </div>
        </template>
        <div class="dashboard-steps">
          <div v-for="item in roadmapSteps" :key="item.index" class="dashboard-step">
            <span class="dashboard-step__index">{{ item.index }}</span>
            <div class="dashboard-step__body">
              <div class="dashboard-step__title">{{ item.title }}</div>
              <div class="dashboard-step__desc">{{ item.desc }}</div>
            </div>
          </div>
        </div>
      </el-card>
    </section>
  </div>
</template>

<script setup name="Index">
import { DataAnalysis, Tickets, Connection, Setting, Promotion, Histogram, MagicStick, Files } from '@element-plus/icons-vue'

const metricItems = [
  {
    label: '配置层级',
    value: '8 层',
    desc: '覆盖业务域、场景、费用、变量、规则、发布、核算与追溯'
  },
  {
    label: '运行目标',
    value: '100万+/月',
    desc: '面向月度百万级计费处理规模设计批量运行能力'
  },
  {
    label: '版本治理',
    value: '全链路',
    desc: '发布快照、生效切换、结果解释与审计记录统一关联'
  },
  {
    label: '追溯能力',
    value: '可解释',
    desc: '支持按任务、费用、版本和命中规则回看核算过程'
  }
]

const shortcuts = [
  {
    title: '业务域与场景',
    desc: '统一组织跨行业、跨合同、跨公司的核算主题。',
    meta: '通过业务域统一口径，再按场景组织完整核算链。',
    icon: DataAnalysis
  },
  {
    title: '费用主数据',
    desc: '每个场景下维护多费用对象，为规则与发布建立边界。',
    meta: '支持按费用对象组织核算规则、版本与结果追踪。',
    icon: Tickets
  },
  {
    title: '变量中心',
    desc: '变量覆盖字典、接口、公式、业务输入与规则条件来源。',
    meta: '统一管理数据来源、字段映射、缓存与业务影响因素。',
    icon: Connection
  },
  {
    title: '费率规则',
    desc: '支持固定费率、公式、阶梯费率及复杂条件组合。',
    meta: '可按货种、贸易属性、工种、载重吨等因素组合定价。',
    icon: Setting
  },
  {
    title: '发布治理',
    desc: '场景级发布、版本快照、生效切换、差异对比与回滚。',
    meta: '运行时只认发布版本，保障可追溯与可复现。',
    icon: Promotion
  },
  {
    title: '结果台账',
    desc: '统一记录正式核算结果、差异分析、命中解释和追溯链路。',
    meta: '支持按费用口径查看结果，按场景版本回看运行过程。',
    icon: Histogram
  },
  {
    title: '试算与验证',
    desc: '支持试算、导入预览、命中明细和执行步骤验证。',
    meta: '在正式入账前完成样本核验、输入校对和结果确认。',
    icon: MagicStick
  },
  {
    title: '治理规范',
    desc: '统一管理数据结构、版本规则、运行边界与追溯要求。',
    meta: '保障平台配置、运行与治理口径长期一致。',
    icon: Files
  }
]

const designAnchors = [
  {
    title: '业务建模',
    desc: '统一定义业务域、场景、费用对象和变量口径，确保配置边界清晰。'
  },
  {
    title: '数据结构',
    desc: '统一业务表、索引、主外键关系和快照落库边界，保证运行数据可管可查。'
  },
  {
    title: '运行治理',
    desc: '明确缓存刷新、告警联动、任务分片与失败恢复的治理策略。'
  },
  {
    title: '追溯解释',
    desc: '确保结果、命中规则、变量取值和执行时间线能够完整回看。'
  }
]

const governanceItems = [
  {
    label: '首页定位',
    value: '统一核算驾驶舱'
  },
  {
    label: '配置方式',
    value: '多场景、多费用、多规则统一管理'
  },
  {
    label: '运行边界',
    value: '按场景发布、按版本执行、按结果追溯'
  },
  {
    label: '性能重点',
    value: '面向百万级月度计费，优先保障批量执行效率与稳定性'
  }
]

const roadmapSteps = [
  {
    index: '01',
    title: '业务建模',
    desc: '先梳理业务域、场景和费用对象，明确核算主题与口径边界。'
  },
  {
    index: '02',
    title: '规则配置',
    desc: '围绕变量、条件、费率规则和版本快照完成可运营配置。'
  },
  {
    index: '03',
    title: '版本治理',
    desc: '通过发布、生效切换、差异对比和审计记录保障版本一致性。'
  },
  {
    index: '04',
    title: '运行追溯',
    desc: '通过试算、正式核算、批量任务、结果台账和告警中心形成闭环。'
  }
]
</script>

<style scoped lang="scss">
.cost-dashboard {
  display: grid;
  gap: 18px;
}

.dashboard-hero {
  display: grid;
  grid-template-columns: minmax(0, 1.5fr) minmax(280px, 0.7fr);
  gap: 18px;
  padding: 28px 30px;
  border: 1px solid var(--el-border-color);
  border-radius: 18px;
  background: color-mix(in srgb, var(--el-bg-color-overlay) 96%, transparent);
  box-shadow: 0 14px 36px rgba(15, 23, 42, 0.08);
}

.dashboard-hero__eyebrow {
  color: var(--el-color-primary);
  font-size: 13px;
  font-weight: 700;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.dashboard-hero__title {
  margin: 10px 0 0;
  font-size: 32px;
  line-height: 1.24;
  color: var(--el-text-color-primary);
}

.dashboard-hero__subtitle {
  margin: 14px 0 0;
  color: var(--el-text-color-regular);
  font-size: 15px;
  line-height: 1.9;
}

.dashboard-hero__tags {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: 18px;
}

.dashboard-highlight-card {
  height: 100%;
  display: grid;
  align-content: center;
  gap: 10px;
  padding: 20px 22px;
  border-radius: 16px;
  border: 1px solid var(--el-border-color);
  background: color-mix(in srgb, var(--el-color-primary-light-9) 36%, var(--el-bg-color-overlay));
}

.dashboard-highlight-card__label,
.dashboard-metric-card__label,
.dashboard-governance__label,
.dashboard-panel__tip,
.dashboard-shortcut__meta {
  color: var(--el-text-color-secondary);
  font-size: 13px;
}

.dashboard-highlight-card__value,
.dashboard-metric-card__value {
  color: var(--el-color-primary);
  font-size: 34px;
  line-height: 1;
  font-weight: 700;
}

.dashboard-highlight-card__desc,
.dashboard-metric-card__desc,
.dashboard-governance__value,
.dashboard-list-item__meta,
.dashboard-shortcut__desc,
.dashboard-step__desc {
  color: var(--el-text-color-regular);
  font-size: 13px;
  line-height: 1.7;
}

.dashboard-metrics {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 14px;
}

.dashboard-metric-card,
.dashboard-shortcut,
.dashboard-list-item {
  border: 1px solid var(--el-border-color);
  border-radius: 16px;
  background: color-mix(in srgb, var(--el-bg-color-overlay) 96%, transparent);
}

.dashboard-metric-card {
  padding: 18px 20px;
  box-shadow: 0 8px 22px rgba(15, 23, 42, 0.05);
}

.dashboard-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 18px;
}

.dashboard-panel {
  border-radius: 18px;
}

.dashboard-panel--wide {
  grid-column: span 2;
}

.dashboard-panel__header {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: baseline;
}

.dashboard-shortcuts {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 14px;
}

.dashboard-shortcut {
  display: flex;
  gap: 14px;
  padding: 18px;
}

.dashboard-shortcut__icon {
  flex-shrink: 0;
  width: 40px;
  height: 40px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 12px;
  background: color-mix(in srgb, var(--el-color-primary-light-9) 48%, transparent);
  color: var(--el-color-primary);
  font-size: 20px;
}

.dashboard-shortcut__body {
  display: grid;
  gap: 6px;
}

.dashboard-shortcut__title,
.dashboard-list-item__title,
.dashboard-step__title {
  color: var(--el-text-color-primary);
  font-size: 16px;
  font-weight: 700;
}

.dashboard-list {
  display: grid;
  gap: 12px;
}

.dashboard-list-item {
  width: 100%;
  padding: 16px 18px;
  text-align: left;
}

.dashboard-governance {
  display: grid;
  gap: 14px;
}

.dashboard-governance__item {
  display: grid;
  gap: 8px;
  padding: 16px 18px;
  border-radius: 14px;
  border: 1px dashed var(--el-border-color);
  background: color-mix(in srgb, var(--el-fill-color-light) 42%, transparent);
}

.dashboard-steps {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 14px;
}

.dashboard-step {
  display: flex;
  gap: 14px;
  padding: 18px;
  border-radius: 16px;
  border: 1px solid var(--el-border-color);
  background: color-mix(in srgb, var(--el-bg-color-overlay) 96%, transparent);
}

.dashboard-step__index {
  flex-shrink: 0;
  width: 42px;
  height: 42px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  background: color-mix(in srgb, var(--el-color-primary-light-8) 56%, transparent);
  color: var(--el-color-primary);
  font-weight: 700;
}

.dashboard-step__body {
  display: grid;
  gap: 8px;
}

@media (max-width: 1400px) {
  .dashboard-shortcuts {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .dashboard-steps {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 1100px) {
  .dashboard-hero,
  .dashboard-grid,
  .dashboard-metrics,
  .dashboard-steps {
    grid-template-columns: 1fr;
  }

  .dashboard-panel--wide {
    grid-column: span 1;
  }
}

@media (max-width: 768px) {
  .dashboard-shortcuts {
    grid-template-columns: 1fr;
  }

  .dashboard-hero {
    padding: 22px;
  }

  .dashboard-hero__title {
    font-size: 28px;
  }

  .dashboard-panel__header {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>

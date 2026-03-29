<template>
  <div class="app-container cost-dashboard">
    <section class="dashboard-hero">
      <div class="dashboard-hero__main">
        <div class="dashboard-hero__eyebrow">企业级成本核算平台</div>
        <h1 class="dashboard-hero__title">面向多行业、多场景、多费用的统一核算驾驶舱</h1>
        <p class="dashboard-hero__subtitle">
          当前基线先收稳“产品底座 + 首页驾驶舱 + 详细设计文档”，后续严格按“业务域 -> 场景 -> 费用 -> 变量 -> 规则 -> 阶梯 -> 发布 -> 核算 -> 追溯”主线分阶段恢复完整能力。
        </p>
        <div class="dashboard-hero__tags">
          <el-tag type="success">当前阶段：底座产品化</el-tag>
          <el-tag type="info">首页已切换为核算驾驶舱</el-tag>
          <el-tag type="warning">规则、发布、运行链按设计文档分步接入</el-tag>
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
            <span>平台主线</span>
            <span class="dashboard-panel__tip">首页先统一心智模型，再逐步恢复业务模块</span>
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
            <span>设计落点</span>
            <span class="dashboard-panel__tip">这批内容已沉淀为项目基线文档</span>
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
            <span>治理看板</span>
            <span class="dashboard-panel__tip">只展示当前已确认的产品基线，不伪造业务数据</span>
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
            <span>实施路径</span>
            <span class="dashboard-panel__tip">后续开发与验证统一以根目录文档为准</span>
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
    label: '基线文档',
    value: '10 份',
    desc: '详细设计、ER、SQL、缓存规范、开发验证规范已齐备'
  },
  {
    label: '平台主链',
    value: '8 层',
    desc: '业务域、场景、费用、变量、规则、阶梯、发布、核算'
  },
  {
    label: '性能目标',
    value: '100万+/月',
    desc: '按月百万级计费量设计数据与执行链路'
  },
  {
    label: '当前基线',
    value: '已收口',
    desc: '保留首页驾驶舱、品牌裁剪和开发所需规范文档'
  }
]

const shortcuts = [
  {
    title: '业务域与场景',
    desc: '统一组织跨行业、跨合同、跨公司的核算主题。',
    meta: '先按业务域治理，再按场景组织完整核算链。',
    icon: DataAnalysis
  },
  {
    title: '费用主数据',
    desc: '每个场景下维护多费用对象，为规则与发布建立边界。',
    meta: '对齐老项目“基础费目维护”的业务认知。',
    icon: Tickets
  },
  {
    title: '变量中心',
    desc: '变量覆盖字典、接口、公式、业务输入与规则条件来源。',
    meta: '阶梯费率必须明确绑定具体业务变量。',
    icon: Connection
  },
  {
    title: '费率规则',
    desc: '支持固定费率、公式、阶梯费率及复杂条件组合。',
    meta: '受货种、内外贸、工种、载重吨等因素影响。',
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
    desc: '沉淀正式核算结果、差异分析、命中解释和追溯链路。',
    meta: '业务查看按费用，平台追溯按场景版本。',
    icon: Histogram
  },
  {
    title: '试算与验证',
    desc: '支持试算、导入预览、命中明细和执行步骤验证。',
    meta: '开发与交付统一按验证规范执行。',
    icon: MagicStick
  },
  {
    title: '设计文档',
    desc: '所有设计、SQL、ER、缓存规范统一放在仓库根目录。',
    meta: '后续开发、构建、验证必须与文档保持一致。',
    icon: Files
  }
]

const designAnchors = [
  {
    title: '详细设计',
    desc: '定义产品目标、主链模型、模块边界、性能目标与演进路线。'
  },
  {
    title: '完整 SQL 与 ER 图',
    desc: '明确 `cost_` 前缀业务表、主外键关系、索引和注释规范。'
  },
  {
    title: '缓存与 Redis 规范',
    desc: '划分哪些数据可缓存、哪些数据必须以数据库和快照为准。'
  },
  {
    title: '开发与验证规范',
    desc: '要求所有开发从设计对齐、实现、构建、验证到交付闭环。'
  }
]

const governanceItems = [
  {
    label: '首页定位',
    value: '核算驾驶舱，而不是通用后台欢迎页'
  },
  {
    label: '基础要求',
    value: '保留若依治理底座，移除与核算无关的默认宣传内容'
  },
  {
    label: '发布边界',
    value: '后续严格按场景发布、按费用追溯、按结果解释'
  },
  {
    label: '性能重点',
    value: '面向百万级月度计费，优先保障批量执行效率与稳定性'
  }
]

const roadmapSteps = [
  {
    index: '01',
    title: '收稳底座',
    desc: '完成首页、品牌、文档、数据设计和开发规范基线。'
  },
  {
    index: '02',
    title: '恢复主链',
    desc: '按设计恢复业务域、场景、费用、变量、规则、阶梯中心。'
  },
  {
    index: '03',
    title: '接回治理',
    desc: '恢复发布、版本、生效、差异对比、审计和删除保护。'
  },
  {
    index: '04',
    title: '恢复运行',
    desc: '接回试算、正式核算、批量任务、台账和监控告警。'
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

<template>
  <div class="publish-precheck-panel">
    <div v-if="showSummary" class="publish-precheck-panel__summary" :style="{ '--summary-columns': String(columns) }">
      <div class="publish-precheck-panel__card">
        <span>阻断项</span>
        <strong>{{ normalized.blockingCount }}</strong>
        <small>必须处理完成的发布前置问题</small>
      </div>
      <div class="publish-precheck-panel__card">
        <span>告警项</span>
        <strong>{{ normalized.warningCount }}</strong>
        <small>不会阻断发布，但建议业务确认</small>
      </div>
      <div class="publish-precheck-panel__card">
        <span>受影响费用</span>
        <strong>{{ normalized.impactedFeeCount }}</strong>
        <small>本次发布会影响的费用主线数量</small>
      </div>
      <div class="publish-precheck-panel__card">
        <span>{{ versionLabel }}</span>
        <strong>{{ normalized.activeVersionNo || emptyVersionText }}</strong>
        <small>{{ versionDescription }}</small>
      </div>
    </div>

    <div v-if="showGovernanceOverview && hasResult" class="publish-precheck-panel__governance">
      <div
        v-for="item in governanceOverviewItems"
        :key="item.key"
        class="publish-precheck-panel__governance-card"
        :class="{ 'is-clean': item.count === 0 }"
      >
        <div class="publish-precheck-panel__governance-head">
          <span>{{ item.label }}</span>
          <el-tag :type="item.count > 0 ? item.tag : 'success'" size="small">{{ item.count > 0 ? '需处理' : '通过' }}</el-tag>
        </div>
        <strong>{{ item.count }}</strong>
        <small>{{ item.desc }}</small>
        <p v-if="item.sample">{{ item.sample }}</p>
      </div>
    </div>

    <el-alert
      v-if="showConclusion && hasResult"
      :type="normalized.publishable === false ? 'warning' : 'success'"
      :title="normalized.publishable === false ? blockingText : passText"
      :closable="false"
      show-icon
      class="publish-precheck-panel__alert"
    />

    <div v-if="showBlockingSummary && normalized.blockingItems.length" class="publish-precheck-panel__blocking">
      <div class="publish-precheck-panel__blocking-head">
        <strong>阻断项汇总</strong>
        <el-tag type="danger" size="small">{{ normalized.blockingItems.length }} 项</el-tag>
      </div>
      <div v-for="item in normalized.blockingItems" :key="item.code + item.title" class="publish-precheck-panel__blocking-item">
        <strong>{{ item.title }}</strong>
        <p>{{ item.message }}</p>
      </div>
    </div>

    <el-table
      v-if="normalized.items.length"
      :data="normalized.items"
      size="small"
      class="publish-precheck-panel__table"
    >
      <el-table-column label="级别" width="100" align="center">
        <template #default="scope">
          <el-tag :type="resolveCheckLevelMeta(scope.row.level).type">
            {{ resolveCheckLevelMeta(scope.row.level).label }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="检查项" prop="title" min-width="180" />
      <el-table-column label="说明" prop="message" min-width="360" :show-overflow-tooltip="true" />
    </el-table>

    <el-empty
      v-else-if="showEmpty && hasResult"
      description="本次发布检查未发现阻断项或告警项"
      :image-size="72"
      class="publish-precheck-panel__empty"
    />

    <PublishImpactFeeList
      v-if="showImpact"
      :fees="normalized.impactedFees"
      :title="impactTitle"
      :subtitle="impactSubtitle"
      :columns="impactColumns"
      :compact="compactImpact"
      :show-count="showImpactCount"
      class="publish-precheck-panel__impact"
    />
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { resolveCheckLevelMeta } from '@/utils/costDisplayLabels'
import PublishImpactFeeList from './PublishImpactFeeList.vue'

const props = defineProps({
  data: {
    type: Object,
    default: () => ({})
  },
  impactFees: {
    type: Array,
    default: undefined
  },
  showSummary: {
    type: Boolean,
    default: true
  },
  showGovernanceOverview: {
    type: Boolean,
    default: true
  },
  showConclusion: {
    type: Boolean,
    default: false
  },
  showEmpty: {
    type: Boolean,
    default: false
  },
  columns: {
    type: Number,
    default: 4
  },
  versionLabel: {
    type: String,
    default: '当前生效版本'
  },
  versionDescription: {
    type: String,
    default: '当前工作场景正在使用的正式版本'
  },
  emptyVersionText: {
    type: String,
    default: '暂无'
  },
  blockingText: {
    type: String,
    default: '当前仍有阻断项，处理完成后才能发布。'
  },
  passText: {
    type: String,
    default: '当前检查未发现阻断项，可以进入发布确认。'
  },
  impactTitle: {
    type: String,
    default: '受影响费用清单'
  },
  impactSubtitle: {
    type: String,
    default: ''
  },
  impactColumns: {
    type: Number,
    default: 3
  },
  compactImpact: {
    type: Boolean,
    default: false
  },
  showImpactCount: {
    type: Boolean,
    default: true
  },
  showBlockingSummary: {
    type: Boolean,
    default: true
  }
})

const normalized = computed(() => {
  const payload = props.data || {}
  const items = Array.isArray(payload.items) ? payload.items : []
  const blockingItems = Array.isArray(payload.blockingItems) ? payload.blockingItems : items.filter(item => item.level === 'BLOCK')
  const warningItems = Array.isArray(payload.warningItems) ? payload.warningItems : items.filter(item => item.level === 'WARN')
  const impactFees = Array.isArray(props.impactFees) ? props.impactFees : (Array.isArray(payload.impactedFees) ? payload.impactedFees : [])
  const blockingCount = Number(payload.blockingCount ?? items.filter(item => item.level === 'BLOCK').length ?? 0)
  const warningCount = Number(payload.warningCount ?? items.filter(item => item.level === 'WARN').length ?? 0)
  return {
    ...payload,
    items,
    blockingItems,
    warningItems,
    impactedFees: impactFees.filter(Boolean),
    blockingCount,
    warningCount,
    impactedFeeCount: Number(payload.impactedFeeCount ?? impactFees.length ?? 0)
  }
})

const hasResult = computed(() => {
  const payload = props.data || {}
  return Boolean(
    payload.sceneId ||
    payload.checked ||
    payload.publishable !== undefined ||
    normalized.value.items.length ||
    normalized.value.impactedFees.length
  )
})

const problemItems = computed(() => normalized.value.items.filter(item => ['BLOCK', 'WARN'].includes(item.level)))

const governanceOverviewItems = computed(() => [
  buildGovernanceOverviewItem({
    key: 'missingRule',
    label: '缺规则',
    desc: '费用未挂规则、场景无规则或阶梯规则缺明细',
    tag: 'danger',
    matcher: item => ['NO_RULE', 'FEE_RULE_MISSING', 'RULE_TIER_MISSING'].includes(item.code)
  }),
  buildGovernanceOverviewItem({
    key: 'missingVariable',
    label: '缺变量',
    desc: '规则条件、计量字段或公式依赖缺少可发布变量',
    tag: 'danger',
    matcher: item => ['NO_VARIABLE', 'RULE_QUANTITY_VARIABLE_MISSING', 'RULE_CONDITION_VARIABLE_MISSING', 'FORMULA_DEPENDENCY_VARIABLE_MISSING'].includes(item.code)
  }),
  buildGovernanceOverviewItem({
    key: 'formulaIssue',
    label: '公式问题',
    desc: '公式编码缺失、公式资产缺失、依赖缺失或循环依赖',
    tag: 'warning',
    matcher: item => String(item.code || '').startsWith('FORMULA_') || String(item.title || '').includes('公式')
  }),
  buildGovernanceOverviewItem({
    key: 'disabledObject',
    label: '停用对象',
    desc: '场景已停用，或引用对象不存在、未启用',
    tag: 'warning',
    matcher: item => String(item.code || '').includes('DISABLED') || /已停用|未启用|停用/.test(`${item.title || ''}${item.message || ''}`)
  })
])

const showImpact = computed(() => normalized.value.impactedFees.length > 0)

function buildGovernanceOverviewItem({ key, label, desc, tag, matcher }) {
  const matched = problemItems.value.filter(matcher)
  return {
    key,
    label,
    desc,
    tag,
    count: matched.length,
    sample: matched[0]?.message || ''
  }
}
</script>

<style scoped lang="scss">
.publish-precheck-panel {
  display: grid;
  gap: 14px;
}

.publish-precheck-panel__summary {
  display: grid;
  grid-template-columns: repeat(var(--summary-columns), minmax(0, 1fr));
  gap: 12px;
}

.publish-precheck-panel__card {
  display: grid;
  gap: 7px;
  padding: 14px 16px;
  border: 1px solid var(--el-border-color-light);
  border-radius: 14px;
  background: color-mix(in srgb, var(--el-color-success-light-9) 18%, var(--el-bg-color-overlay));
}

.publish-precheck-panel__card span,
.publish-precheck-panel__card small {
  color: var(--el-text-color-secondary);
}

.publish-precheck-panel__card strong {
  color: var(--el-text-color-primary);
  font-size: 24px;
  line-height: 1.2;
}

.publish-precheck-panel__governance {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
}

.publish-precheck-panel__governance-card {
  display: grid;
  gap: 7px;
  min-width: 0;
  padding: 14px 16px;
  border: 1px solid var(--el-color-warning-light-5);
  border-radius: 8px;
  background: color-mix(in srgb, var(--el-color-warning-light-9) 56%, var(--el-bg-color-overlay));
}

.publish-precheck-panel__governance-card.is-clean {
  border-color: var(--el-color-success-light-7);
  background: color-mix(in srgb, var(--el-color-success-light-9) 56%, var(--el-bg-color-overlay));
}

.publish-precheck-panel__governance-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.publish-precheck-panel__governance-head span {
  min-width: 0;
  color: var(--el-text-color-regular);
  font-weight: 700;
}

.publish-precheck-panel__governance-card strong {
  color: var(--el-text-color-primary);
  font-size: 22px;
  line-height: 1.2;
}

.publish-precheck-panel__governance-card small,
.publish-precheck-panel__governance-card p {
  color: var(--el-text-color-secondary);
}

.publish-precheck-panel__governance-card p {
  display: -webkit-box;
  overflow: hidden;
  margin: 0;
  font-size: 12px;
  line-height: 1.5;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
}

.publish-precheck-panel__alert,
.publish-precheck-panel__table,
.publish-precheck-panel__impact {
  margin-top: 2px;
}

.publish-precheck-panel__blocking {
  display: grid;
  gap: 10px;
  padding: 12px;
  border: 1px solid var(--el-color-danger-light-7);
  border-radius: 8px;
  background: color-mix(in srgb, var(--el-color-danger-light-9) 52%, var(--el-bg-color-overlay));
}

.publish-precheck-panel__blocking-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.publish-precheck-panel__blocking-item {
  display: grid;
  gap: 4px;
  padding: 10px 12px;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 8px;
  background: var(--el-bg-color-overlay);
}

.publish-precheck-panel__blocking-item p {
  margin: 0;
  color: var(--el-text-color-secondary);
  line-height: 1.6;
}

@media (max-width: 1280px) {
  .publish-precheck-panel__summary,
  .publish-precheck-panel__governance {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 768px) {
  .publish-precheck-panel__summary,
  .publish-precheck-panel__governance {
    grid-template-columns: 1fr;
  }
}
</style>

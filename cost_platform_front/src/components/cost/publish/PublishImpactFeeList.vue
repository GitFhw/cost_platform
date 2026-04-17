<template>
  <div class="publish-impact-fee-list" :class="{ 'is-compact': compact }">
    <div v-if="title || subtitle" class="publish-impact-fee-list__head">
      <div>
        <div v-if="title" class="publish-impact-fee-list__title">{{ title }}</div>
        <div v-if="subtitle" class="publish-impact-fee-list__subtitle">{{ subtitle }}</div>
      </div>
      <el-tag v-if="showCount" type="info" effect="plain">共 {{ normalizedFees.length }} 项</el-tag>
    </div>

    <el-empty
      v-if="!normalizedFees.length"
      :description="emptyDescription"
      :image-size="72"
      class="publish-impact-fee-list__empty"
    />

    <div
      v-else
      class="publish-impact-fee-list__grid"
      :style="{ '--impact-columns': String(columns) }"
    >
      <div v-for="item in normalizedFees" :key="item.feeCode || item.feeName" class="publish-impact-fee-card">
        <div class="publish-impact-fee-card__head">
          <div class="publish-impact-fee-card__name">
            <strong>{{ item.feeName || item.feeCode || '未命名费用' }}</strong>
            <span>{{ item.feeCode || '-' }}</span>
          </div>
          <el-tag size="small" :type="resolveCostChangeTypeMeta(item.changeType).type">
            {{ resolveCostChangeTypeMeta(item.changeType).label }}
          </el-tag>
        </div>

        <div class="publish-impact-fee-card__metrics">
          <span>规则变化 {{ item.ruleChangeCount || 0 }}</span>
          <span>变量变化 {{ item.variableChangeCount || 0 }}</span>
        </div>

        <div v-if="item.changedVariables?.length" class="publish-impact-fee-card__asset">
          <span>涉及变量</span>
          <small>{{ buildAssetPreview(item.changedVariables, 'variableCode', 'variableName') }}</small>
        </div>

        <div v-if="item.changedRules?.length" class="publish-impact-fee-card__asset">
          <span>涉及规则</span>
          <small>{{ buildAssetPreview(item.changedRules, 'ruleCode', 'ruleName') }}</small>
        </div>

        <div class="publish-impact-fee-card__summary">
          {{ buildFeeSummary(item) }}
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { resolveCostChangeTypeLabel, resolveCostChangeTypeMeta } from '@/utils/costDisplayLabels'

const props = defineProps({
  fees: {
    type: Array,
    default: () => []
  },
  title: {
    type: String,
    default: '受影响费用清单'
  },
  subtitle: {
    type: String,
    default: ''
  },
  emptyDescription: {
    type: String,
    default: '当前没有费用影响明细'
  },
  columns: {
    type: Number,
    default: 3
  },
  compact: {
    type: Boolean,
    default: false
  },
  showCount: {
    type: Boolean,
    default: true
  },
  maxPreview: {
    type: Number,
    default: 3
  }
})

const normalizedFees = computed(() => (Array.isArray(props.fees) ? props.fees : []).filter(Boolean))

function buildAssetPreview(items = [], codeKey, nameKey) {
  const source = Array.isArray(items) ? items : []
  if (!source.length) {
    return ''
  }
  const labels = source.slice(0, props.maxPreview).map((item) => {
    const name = item?.[nameKey]
    const code = item?.[codeKey]
    if (name && code && name !== code) {
      return `${name}（${code}）`
    }
    return name || code || '-'
  })
  const suffix = source.length > props.maxPreview ? ` 等 ${source.length} 项` : ''
  return `${labels.join('、')}${suffix}`
}

function buildFeeSummary(item = {}) {
  if (item.summaryText) {
    return item.summaryText
  }
  if (item.summary) {
    return item.summary
  }
  const changeLabel = resolveCostChangeTypeLabel(item.changeType)
  const ruleCount = item.ruleChangeCount || 0
  const variableCount = item.variableChangeCount || 0
  if (!ruleCount && !variableCount) {
    return `费用主数据发生“${changeLabel}”，未检测到规则或变量引用口径变化。`
  }
  return `费用主数据发生“${changeLabel}”，${ruleCount} 条规则、${variableCount} 个引用变量口径发生变化。`
}
</script>

<style scoped lang="scss">
.publish-impact-fee-list {
  display: grid;
  gap: 12px;
}

.publish-impact-fee-list__head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}

.publish-impact-fee-list__title {
  color: var(--el-text-color-primary);
  font-size: 16px;
  font-weight: 700;
}

.publish-impact-fee-list__subtitle {
  margin-top: 4px;
  color: var(--el-text-color-secondary);
  font-size: 13px;
  line-height: 1.6;
}

.publish-impact-fee-list__grid {
  display: grid;
  grid-template-columns: repeat(var(--impact-columns), minmax(0, 1fr));
  gap: 12px;
}

.publish-impact-fee-card {
  display: grid;
  gap: 9px;
  padding: 14px 16px;
  border: 1px solid var(--el-border-color-light);
  border-radius: 14px;
  background:
    linear-gradient(135deg, color-mix(in srgb, var(--el-color-primary-light-9) 28%, transparent), transparent 56%),
    var(--el-bg-color-overlay);
}

.publish-impact-fee-card__head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 10px;
}

.publish-impact-fee-card__name {
  display: grid;
  gap: 3px;
  min-width: 0;
}

.publish-impact-fee-card__name strong {
  color: var(--el-text-color-primary);
  line-height: 1.35;
}

.publish-impact-fee-card__name span,
.publish-impact-fee-card__asset small,
.publish-impact-fee-card__summary {
  color: var(--el-text-color-secondary);
}

.publish-impact-fee-card__metrics {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.publish-impact-fee-card__metrics span {
  display: inline-flex;
  align-items: center;
  min-height: 24px;
  padding: 2px 10px;
  border-radius: 999px;
  background: color-mix(in srgb, var(--el-color-primary-light-9) 42%, var(--el-bg-color-page));
  color: var(--el-text-color-secondary);
  font-size: 12px;
}

.publish-impact-fee-card__asset {
  display: grid;
  gap: 3px;
}

.publish-impact-fee-card__asset span {
  color: var(--el-text-color-regular);
  font-size: 12px;
  font-weight: 700;
}

.publish-impact-fee-card__asset small,
.publish-impact-fee-card__summary {
  line-height: 1.65;
}

.publish-impact-fee-list.is-compact .publish-impact-fee-list__grid {
  grid-template-columns: 1fr;
}

.publish-impact-fee-list.is-compact .publish-impact-fee-card {
  padding: 12px;
}

@media (max-width: 1280px) {
  .publish-impact-fee-list__grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 768px) {
  .publish-impact-fee-list__grid {
    grid-template-columns: 1fr;
  }
}
</style>

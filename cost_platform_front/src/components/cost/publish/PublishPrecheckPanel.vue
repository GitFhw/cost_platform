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

    <el-alert
      v-if="showConclusion && hasResult"
      :type="normalized.publishable === false ? 'warning' : 'success'"
      :title="normalized.publishable === false ? blockingText : passText"
      :closable="false"
      show-icon
      class="publish-precheck-panel__alert"
    />

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
  }
})

const normalized = computed(() => {
  const payload = props.data || {}
  const items = Array.isArray(payload.items) ? payload.items : []
  const impactFees = Array.isArray(props.impactFees) ? props.impactFees : (Array.isArray(payload.impactedFees) ? payload.impactedFees : [])
  const blockingCount = Number(payload.blockingCount ?? items.filter(item => item.level === 'BLOCK').length ?? 0)
  const warningCount = Number(payload.warningCount ?? items.filter(item => item.level === 'WARN').length ?? 0)
  return {
    ...payload,
    items,
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

const showImpact = computed(() => normalized.value.impactedFees.length > 0)
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

.publish-precheck-panel__alert,
.publish-precheck-panel__table,
.publish-precheck-panel__impact {
  margin-top: 2px;
}

@media (max-width: 1280px) {
  .publish-precheck-panel__summary {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 768px) {
  .publish-precheck-panel__summary {
    grid-template-columns: 1fr;
  }
}
</style>

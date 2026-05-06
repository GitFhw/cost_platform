<template>
  <div class="tier-editor">
    <div class="tier-editor__toolbar">
      <el-button type="primary" plain icon="Plus" @click="handleAdd">新增阶梯</el-button>
      <span class="tier-editor__tip">编辑时实时提示连续性、重叠和空区间；新增行会自动承接上一档截止值。</span>
    </div>
    <el-alert
      v-if="tierIssueSummary.length"
      :title="`当前阶梯存在 ${tierIssueSummary.length} 项待确认提示`"
      type="warning"
      :closable="false"
      show-icon
    >
      <template #default>
        <div v-for="item in tierIssueSummary" :key="item" class="tier-editor__issue-line">{{ item }}</div>
      </template>
    </el-alert>
    <el-alert
      v-else-if="innerValue.length"
      title="阶梯区间连续性检查通过"
      description="当前未发现空区间、区间反向或重叠问题。"
      type="success"
      :closable="false"
      show-icon
    />
    <el-table :data="innerValue" size="small" border>
      <el-table-column label="序号" width="70" align="center">
        <template #default="scope">{{ scope.$index + 1 }}</template>
      </el-table-column>
      <el-table-column label="起始值" min-width="120" align="center">
        <template #default="scope">
          <el-input-number v-model="scope.row.startValue" controls-position="right" style="width: 100%" />
        </template>
      </el-table-column>
      <el-table-column label="截止值" min-width="120" align="center">
        <template #default="scope">
          <el-input-number v-model="scope.row.endValue" controls-position="right" style="width: 100%" />
        </template>
      </el-table-column>
      <el-table-column label="费率/单价" min-width="140" align="center">
        <template #default="scope">
          <el-input-number v-model="scope.row.rateValue" controls-position="right" :precision="6" style="width: 100%" />
        </template>
      </el-table-column>
      <el-table-column label="区间模式" min-width="170" align="center">
        <template #default="scope">
          <el-select v-model="scope.row.intervalMode" style="width: 100%">
            <el-option v-for="item in intervalModeOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </template>
      </el-table-column>
      <el-table-column label="区间摘要" min-width="240" align="center">
        <template #default="scope">
          <div class="tier-editor__summary">
            <strong>{{ buildRangeSummary(scope.row) }}</strong>
            <span>{{ buildHitSummary(scope.row) }}</span>
          </div>
        </template>
      </el-table-column>
      <el-table-column label="校验提示" min-width="220" align="center">
        <template #default="scope">
          <div v-if="resolveRowIssues(scope.$index).length" class="tier-editor__row-issues">
            <el-tag v-for="item in resolveRowIssues(scope.$index)" :key="item" type="warning" effect="light">
              {{ item }}
            </el-tag>
          </div>
          <el-tag v-else type="success" effect="light">通过</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="备注" min-width="180" align="center">
        <template #default="scope">
          <el-input v-model="scope.row.remark" placeholder="补充本档口径" />
        </template>
      </el-table-column>
      <el-table-column label="操作" width="170" align="center" fixed="right">
        <template #default="scope">
          <el-button link type="primary" icon="CopyDocument" :disabled="scope.$index === 0" @click="handleCopyPrevious(scope.$index)">复制上一行</el-button>
          <el-button link type="danger" icon="Delete" @click="handleRemove(scope.$index)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
  </div>
</template>

<script setup>
const props = defineProps({
  modelValue: {
    type: Array,
    default: () => []
  },
  intervalModeOptions: {
    type: Array,
    default: () => []
  }
})

const emit = defineEmits(['update:modelValue'])

const innerValue = computed({
  get: () => props.modelValue || [],
  set: value => emit('update:modelValue', value)
})

const tierIssueResult = computed(() => buildTierIssueResult(innerValue.value))
const tierIssueSummary = computed(() => tierIssueResult.value.summary)

function handleAdd() {
  const last = innerValue.value[innerValue.value.length - 1]
  innerValue.value = [...innerValue.value, {
    tierNo: innerValue.value.length + 1,
    startValue: last?.endValue,
    intervalMode: last?.intervalMode || 'LEFT_CLOSED_RIGHT_OPEN',
    status: '0'
  }]
}

function handleRemove(index) {
  const next = [...innerValue.value]
  next.splice(index, 1)
  next.forEach((item, idx) => {
    item.tierNo = idx + 1
  })
  innerValue.value = next
}

function handleCopyPrevious(index) {
  if (index <= 0) {
    return
  }
  const next = [...innerValue.value]
  const previous = next[index - 1] || {}
  const current = next[index] || {}
  next[index] = {
    ...current,
    startValue: previous.endValue,
    endValue: current.endValue ?? previous.endValue,
    rateValue: previous.rateValue,
    intervalMode: previous.intervalMode || current.intervalMode || 'LEFT_CLOSED_RIGHT_OPEN',
    remark: previous.remark ? `${previous.remark}-复制` : current.remark,
    tierNo: index + 1,
    status: current.status || '0'
  }
  innerValue.value = next
}

function buildRangeSummary(row) {
  const start = row.startValue ?? '-INF'
  const end = row.endValue ?? '+INF'
  return row.intervalMode === 'LEFT_OPEN_RIGHT_CLOSED'
    ? `${start} < x <= ${end}`
    : `${start} <= x < ${end}`
}

function buildHitSummary(row) {
  const price = row.rateValue ?? '-'
  return `命中当前区间时，取费率/单价 ${price}`
}

function resolveRowIssues(index) {
  return tierIssueResult.value.byIndex[index] || []
}

function normalizeNumber(value) {
  if (value === undefined || value === null || value === '') {
    return undefined
  }
  const numberValue = Number(value)
  return Number.isFinite(numberValue) ? numberValue : undefined
}

function isRightClosed(row) {
  return String(row.intervalMode || '').includes('RIGHT_CLOSED')
}

function isLeftClosed(row) {
  const mode = String(row.intervalMode || '')
  return !mode || mode.includes('LEFT_CLOSED')
}

function pushIssue(result, index, message) {
  if (!result.byIndex[index]) {
    result.byIndex[index] = []
  }
  if (!result.byIndex[index].includes(message)) {
    result.byIndex[index].push(message)
  }
  if (!result.summary.includes(message)) {
    result.summary.push(message)
  }
}

function buildTierIssueResult(rows = []) {
  const result = { summary: [], byIndex: {} }
  const normalizedRows = rows.map((row, index) => ({
    row,
    index,
    start: normalizeNumber(row.startValue),
    end: normalizeNumber(row.endValue)
  }))

  normalizedRows.forEach(item => {
    if (item.start === undefined || item.end === undefined) {
      pushIssue(result, item.index, `第 ${item.index + 1} 档起止值未完整配置`)
    }
    if (item.start !== undefined && item.end !== undefined && item.end <= item.start) {
      pushIssue(result, item.index, `第 ${item.index + 1} 档截止值必须大于起始值`)
    }
    if (normalizeNumber(item.row.rateValue) === undefined) {
      pushIssue(result, item.index, `第 ${item.index + 1} 档费率/单价未配置`)
    }
  })

  const comparableRows = normalizedRows
    .filter(item => item.start !== undefined && item.end !== undefined && item.end > item.start)
    .sort((a, b) => a.start - b.start)

  comparableRows.forEach((item, sortedIndex) => {
    const previous = comparableRows[sortedIndex - 1]
    if (!previous) {
      return
    }
    if (item.start < previous.end) {
      pushIssue(result, item.index, `第 ${item.index + 1} 档与第 ${previous.index + 1} 档区间重叠`)
      pushIssue(result, previous.index, `第 ${previous.index + 1} 档与第 ${item.index + 1} 档区间重叠`)
      return
    }
    if (item.start > previous.end) {
      pushIssue(result, item.index, `第 ${previous.index + 1} 档到第 ${item.index + 1} 档存在空档`)
      pushIssue(result, previous.index, `第 ${previous.index + 1} 档到第 ${item.index + 1} 档存在空档`)
      return
    }
    if (isRightClosed(previous.row) && isLeftClosed(item.row)) {
      pushIssue(result, item.index, `第 ${previous.index + 1} 档与第 ${item.index + 1} 档边界值重复命中`)
      pushIssue(result, previous.index, `第 ${previous.index + 1} 档与第 ${item.index + 1} 档边界值重复命中`)
    }
  })

  return result
}
</script>

<style scoped lang="scss">
.tier-editor {
  display: grid;
  gap: 12px;
}

.tier-editor__toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.tier-editor__tip {
  color: var(--el-text-color-secondary);
  font-size: 12px;
}

.tier-editor__issue-line {
  line-height: 1.7;
}

.tier-editor__summary {
  display: grid;
  gap: 4px;
}

.tier-editor__summary strong {
  color: var(--el-text-color-primary);
}

.tier-editor__summary span {
  color: var(--el-text-color-secondary);
  font-size: 12px;
  line-height: 1.6;
}

.tier-editor__row-issues {
  display: flex;
  flex-wrap: wrap;
  justify-content: center;
  gap: 6px;
}
</style>

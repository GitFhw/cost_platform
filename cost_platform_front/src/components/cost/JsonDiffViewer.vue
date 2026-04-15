<template>
  <div class="json-diff-viewer">
    <div class="json-diff-viewer__head">
      <div>
        <strong>{{ title }}</strong>
        <small>{{ subtitleText }}</small>
      </div>
      <div class="json-diff-viewer__tags">
        <el-tag :type="summary.total ? 'warning' : 'success'" effect="plain">差异 {{ summary.total }}</el-tag>
        <el-tag v-if="summary.added" type="success" effect="plain">新增 {{ summary.added }}</el-tag>
        <el-tag v-if="summary.deleted" type="danger" effect="plain">删除 {{ summary.deleted }}</el-tag>
        <el-tag v-if="summary.modified" type="warning" effect="plain">修改 {{ summary.modified }}</el-tag>
      </div>
    </div>

    <el-alert
      v-if="!summary.total"
      title="结构化 JSON 对比结果一致"
      type="success"
      :closable="false"
      class="json-diff-viewer__notice"
    />

    <div v-else-if="showSummary" class="json-diff-viewer__summary">
      <el-table :data="visibleDiffItems" size="small" max-height="260">
        <el-table-column label="类型" width="88" align="center">
          <template #default="scope">
            <el-tag :type="resolveDiffTypeMeta(scope.row.type).type" size="small">
              {{ resolveDiffTypeMeta(scope.row.type).label }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="字段路径" prop="path" min-width="240" show-overflow-tooltip />
        <el-table-column label="基准值" prop="leftText" min-width="180" show-overflow-tooltip />
        <el-table-column label="目标值" prop="rightText" min-width="180" show-overflow-tooltip />
      </el-table>
      <el-alert
        v-if="diffItems.length > maxSummaryRows"
        :title="`差异较多，当前仅展示前 ${maxSummaryRows} 条字段级摘要；完整内容请查看下方左右 JSON。`"
        type="warning"
        :closable="false"
        class="json-diff-viewer__notice"
      />
    </div>

    <div class="json-diff-viewer__grid">
      <div class="json-diff-viewer__side" :class="{ 'is-empty': leftState.empty }">
        <div class="json-diff-viewer__side-head">
          <span>{{ leftTitle }}</span>
          <el-tag v-if="leftState.empty" type="info" effect="plain" size="small">暂无数据</el-tag>
        </div>
        <JsonEditor
          v-if="!leftState.empty"
          :model-value="leftState.text"
          readonly
          :toolbar="false"
          :rows="rows"
          :lang="leftState.lang"
          :allow-fullscreen="false"
        />
        <el-empty v-else description="基准版本暂无数据" />
      </div>

      <div class="json-diff-viewer__side" :class="{ 'is-empty': rightState.empty }">
        <div class="json-diff-viewer__side-head">
          <span>{{ rightTitle }}</span>
          <el-tag v-if="rightState.empty" type="info" effect="plain" size="small">暂无数据</el-tag>
        </div>
        <JsonEditor
          v-if="!rightState.empty"
          :model-value="rightState.text"
          readonly
          :toolbar="false"
          :rows="rows"
          :lang="rightState.lang"
          :allow-fullscreen="false"
        />
        <el-empty v-else description="目标版本暂无数据" />
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import JsonEditor from '@/components/cost/JsonEditor.vue'
import { safeFormatJson } from '@/utils/jsonTools'

const MISSING = Symbol('missing')
const IDENTITY_KEYS = ['feeCode', 'ruleCode', 'variableCode', 'conditionCode', 'tierCode', 'fieldCode', 'groupNo', 'conditionId', 'tierId', 'id', 'code']

const props = defineProps({
  title: {
    type: String,
    default: 'JSON 差异'
  },
  subtitle: {
    type: String,
    default: ''
  },
  leftTitle: {
    type: String,
    default: '基准版本'
  },
  rightTitle: {
    type: String,
    default: '目标版本'
  },
  leftValue: {
    type: [String, Object, Array, Number, Boolean],
    default: undefined
  },
  rightValue: {
    type: [String, Object, Array, Number, Boolean],
    default: undefined
  },
  rows: {
    type: Number,
    default: 14
  },
  showSummary: {
    type: Boolean,
    default: true
  },
  maxSummaryRows: {
    type: Number,
    default: 80
  }
})

const leftState = computed(() => normalizeSide(props.leftValue))
const rightState = computed(() => normalizeSide(props.rightValue))
const diffItems = computed(() => collectDiffs(leftState.value.data, rightState.value.data))
const visibleDiffItems = computed(() => diffItems.value.slice(0, props.maxSummaryRows))
const summary = computed(() => {
  const totals = { total: diffItems.value.length, added: 0, deleted: 0, modified: 0 }
  diffItems.value.forEach((item) => {
    if (item.type === 'ADDED') totals.added += 1
    if (item.type === 'DELETED') totals.deleted += 1
    if (item.type === 'MODIFIED') totals.modified += 1
  })
  return totals
})
const subtitleText = computed(() => props.subtitle || '先看字段级摘要，再看左右 JSON 快照原文。')

function normalizeSide(value) {
  if (value === null || value === undefined || value === '') {
    return { empty: true, data: MISSING, text: '', lang: 'json' }
  }
  if (typeof value === 'string') {
    const trimmed = value.trim()
    if (!trimmed) {
      return { empty: true, data: MISSING, text: '', lang: 'json' }
    }
    try {
      const parsed = JSON.parse(trimmed)
      return { empty: false, data: parsed, text: JSON.stringify(parsed, null, 2), lang: 'json' }
    } catch {
      return { empty: false, data: trimmed, text: trimmed, lang: 'text' }
    }
  }
  return { empty: false, data: value, text: safeFormatJson(value), lang: 'json' }
}

function collectDiffs(leftValue, rightValue, path = '根节点') {
  if (leftValue === MISSING && rightValue === MISSING) {
    return []
  }
  if (leftValue === MISSING) {
    return [buildDiffItem('ADDED', path, leftValue, rightValue)]
  }
  if (rightValue === MISSING) {
    return [buildDiffItem('DELETED', path, leftValue, rightValue)]
  }
  if (isPlainObject(leftValue) && isPlainObject(rightValue)) {
    return collectObjectDiffs(leftValue, rightValue, path)
  }
  if (Array.isArray(leftValue) && Array.isArray(rightValue)) {
    return collectArrayDiffs(leftValue, rightValue, path)
  }
  if (isSameValue(leftValue, rightValue)) {
    return []
  }
  return [buildDiffItem('MODIFIED', path, leftValue, rightValue)]
}

function collectObjectDiffs(leftValue, rightValue, path) {
  const keys = Array.from(new Set([...Object.keys(leftValue), ...Object.keys(rightValue)])).sort()
  return keys.flatMap((key) => {
    const nextPath = path === '根节点' ? key : `${path}.${key}`
    const leftChild = Object.prototype.hasOwnProperty.call(leftValue, key) ? leftValue[key] : MISSING
    const rightChild = Object.prototype.hasOwnProperty.call(rightValue, key) ? rightValue[key] : MISSING
    return collectDiffs(leftChild, rightChild, nextPath)
  })
}

function collectArrayDiffs(leftValue, rightValue, path) {
  const identityKey = resolveArrayIdentityKey(leftValue, rightValue)
  if (identityKey) {
    const leftMap = buildIdentityMap(leftValue, identityKey)
    const rightMap = buildIdentityMap(rightValue, identityKey)
    const identities = Array.from(new Set([...leftMap.keys(), ...rightMap.keys()])).sort((a, b) => String(a).localeCompare(String(b), 'zh-CN'))
    return identities.flatMap((identity) => {
      const nextPath = `${path}[${identityKey}=${identity}]`
      return collectDiffs(leftMap.has(identity) ? leftMap.get(identity) : MISSING, rightMap.has(identity) ? rightMap.get(identity) : MISSING, nextPath)
    })
  }

  const length = Math.max(leftValue.length, rightValue.length)
  const diffs = []
  for (let index = 0; index < length; index += 1) {
    diffs.push(...collectDiffs(index < leftValue.length ? leftValue[index] : MISSING, index < rightValue.length ? rightValue[index] : MISSING, `${path}[${index}]`))
  }
  return diffs
}

function resolveArrayIdentityKey(leftValue, rightValue) {
  const rows = [...leftValue, ...rightValue].filter(isPlainObject)
  if (!rows.length) {
    return ''
  }
  return IDENTITY_KEYS.find((key) => {
    const values = rows.map(item => item[key]).filter(value => value !== null && value !== undefined && value !== '')
    return values.length >= Math.max(Math.ceil(rows.length * 0.6), 1) && new Set(values.map(String)).size === values.length
  }) || ''
}

function buildIdentityMap(rows, key) {
  return new Map(rows.map((item, index) => [item?.[key] ?? `index:${index}`, item]))
}

function buildDiffItem(type, path, leftValue, rightValue) {
  return {
    type,
    path,
    leftText: stringifyDiffValue(leftValue),
    rightText: stringifyDiffValue(rightValue)
  }
}

function stringifyDiffValue(value) {
  if (value === MISSING) return '无'
  if (value === null) return 'null'
  if (value === undefined) return 'undefined'
  if (Array.isArray(value)) return `[数组 ${value.length} 项]`
  if (isPlainObject(value)) return `{对象 ${Object.keys(value).length} 字段}`
  const text = String(value)
  return text.length > 90 ? `${text.slice(0, 90)}...` : text
}

function resolveDiffTypeMeta(type) {
  const map = {
    ADDED: { label: '新增', type: 'success' },
    DELETED: { label: '删除', type: 'danger' },
    MODIFIED: { label: '修改', type: 'warning' }
  }
  return map[type] || { label: type || '-', type: 'info' }
}

function isPlainObject(value) {
  return Object.prototype.toString.call(value) === '[object Object]'
}

function isSameValue(leftValue, rightValue) {
  if (Object.is(leftValue, rightValue)) {
    return true
  }
  return JSON.stringify(leftValue) === JSON.stringify(rightValue)
}
</script>

<style scoped lang="scss">
.json-diff-viewer {
  display: grid;
  gap: 12px;
  padding: 12px;
  border: 1px solid var(--el-border-color-light);
  border-radius: 14px;
  background: var(--el-bg-color-overlay);
}

.json-diff-viewer__head {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 12px;
}

.json-diff-viewer__head > div:first-child {
  display: grid;
  gap: 4px;
}

.json-diff-viewer__head strong {
  color: var(--el-text-color-primary);
  font-size: 15px;
}

.json-diff-viewer__head small {
  color: var(--el-text-color-secondary);
  line-height: 1.5;
}

.json-diff-viewer__tags {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 6px;
}

.json-diff-viewer__notice {
  margin-top: 0;
}

.json-diff-viewer__summary {
  display: grid;
  gap: 8px;
}

.json-diff-viewer__grid {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(0, 1fr);
  gap: 12px;
}

.json-diff-viewer__side {
  min-width: 0;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 10px;
  overflow: hidden;
  background: #fff;
}

.json-diff-viewer__side.is-empty {
  background: var(--el-fill-color-lighter);
}

.json-diff-viewer__side-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  padding: 9px 12px;
  border-bottom: 1px solid var(--el-border-color-lighter);
  background: var(--el-fill-color-light);
  font-weight: 600;
}

.json-diff-viewer__side :deep(.json-editor__body) {
  border: 0;
  border-radius: 0;
}

@media (max-width: 1200px) {
  .json-diff-viewer__head,
  .json-diff-viewer__grid {
    grid-template-columns: 1fr;
  }

  .json-diff-viewer__head {
    display: grid;
  }

  .json-diff-viewer__tags {
    justify-content: flex-start;
  }
}
</style>

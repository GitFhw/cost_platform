<template>
  <div class="app-container audit-page">
    <section v-show="!isCompactMode" class="audit-page__hero">
      <div>
        <div class="audit-page__eyebrow">审计追踪</div>
        <h2 class="audit-page__title">审计台账</h2>
        <p class="audit-page__subtitle">统一记录发布、运行、账期和缓存相关动作，便于审计留痕、问题追踪和过程复盘。</p>
      </div>
      <el-tag type="info">统一记录发布、运行、账期和缓存治理动作，便于问题追溯与复盘</el-tag>
    </section>

    <section v-show="!isCompactMode" class="audit-page__metrics">
      <div v-for="item in metricItems" :key="item.label" class="audit-page__metric-card">
        <span>{{ item.label }}</span>
        <strong>{{ item.value }}</strong>
        <small>{{ item.desc }}</small>
      </div>
    </section>

    <el-form ref="queryRef" :model="queryParams" :inline="true" label-width="88px" v-show="showSearch">
      <el-form-item label="所属场景" prop="sceneId">
        <el-select v-model="queryParams.sceneId" clearable filterable style="width: 240px">
          <el-option v-for="item in sceneOptions" :key="item.sceneId" :label="`${item.sceneName} / ${item.sceneCode}`" :value="item.sceneId" />
        </el-select>
      </el-form-item>
      <el-form-item label="对象类型" prop="objectType">
        <el-input v-model="queryParams.objectType" clearable style="width: 180px" />
      </el-form-item>
      <el-form-item label="动作类型" prop="actionType">
        <el-input v-model="queryParams.actionType" clearable style="width: 180px" />
      </el-form-item>
      <el-form-item label="对象编码" prop="objectCode">
        <el-input v-model="queryParams.objectCode" clearable style="width: 180px" />
      </el-form-item>
      <el-form-item label="操作人" prop="operatorCode">
        <el-input v-model="queryParams.operatorCode" clearable style="width: 180px" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <section class="audit-page__panel">
      <div class="audit-page__section-head">
        <div>
          <h3>审计记录</h3>
          <p>支持按对象、动作和操作人筛选，查看前后快照差异。</p>
        </div>
        <div class="audit-page__section-actions">
          <el-button type="warning" plain icon="Download" @click="handleExport">
            导出审计
          </el-button>
          <right-toolbar v-model:showSearch="showSearch" @queryTable="getList" />
        </div>
      </div>

      <el-table v-loading="loading" :data="auditList">
        <el-table-column label="所属场景" min-width="180">
          <template #default="scope">{{ scope.row.sceneName || '-' }}<span v-if="scope.row.sceneCode"> ({{ scope.row.sceneCode }})</span></template>
        </el-table-column>
        <el-table-column label="对象类型" prop="objectType" width="130" />
        <el-table-column label="对象编码" prop="objectCode" width="180" />
        <el-table-column label="动作类型" prop="actionType" width="120" />
        <el-table-column label="动作摘要" prop="actionSummary" min-width="220" />
        <el-table-column label="责任人" min-width="150">
          <template #default="scope">{{ resolveOperator(scope.row) }}</template>
        </el-table-column>
        <el-table-column label="操作时间" width="180" align="center">
          <template #default="scope">{{ proxy.parseTime(scope.row.operateTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="110" fixed="right" align="center">
          <template #default="scope">
            <el-button link type="primary" icon="View" @click="handleDetail(scope.row)">详情</el-button>
          </template>
        </el-table-column>
      </el-table>

      <pagination v-show="total > 0" :total="total" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" @pagination="getList" />
    </section>

    <el-drawer v-model="detailOpen" title="审计详情" size="1180px" append-to-body>
      <el-descriptions v-if="detailData" :column="2" border>
        <el-descriptions-item label="所属场景">{{ detailData.sceneName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="操作时间">{{ proxy.parseTime(detailData.operateTime) || '-' }}</el-descriptions-item>
        <el-descriptions-item label="对象类型">{{ detailData.objectType || '-' }}</el-descriptions-item>
        <el-descriptions-item label="动作类型">{{ detailData.actionType || '-' }}</el-descriptions-item>
        <el-descriptions-item label="对象编码">{{ detailData.objectCode || '-' }}</el-descriptions-item>
        <el-descriptions-item label="请求号">{{ detailData.requestNo || '-' }}</el-descriptions-item>
      </el-descriptions>

      <div class="audit-page__responsibility" v-if="detailData">
        <div v-for="item in responsibilityItems" :key="item.label" class="audit-page__responsibility-card">
          <span>{{ item.label }}</span>
          <strong>{{ item.value }}</strong>
          <small>{{ item.desc }}</small>
        </div>
      </div>

      <div class="audit-page__field-diff" v-if="detailData">
        <div class="audit-page__section-head audit-page__section-head--tight">
          <div>
            <h3>字段级变更</h3>
            <p>基于变更前后 JSON 快照自动展开字段路径，突出新增、删除和修改项。</p>
          </div>
          <el-tag type="info">差异 {{ fieldDiffRows.length }} 项</el-tag>
        </div>
        <el-table :data="fieldDiffRows" size="small" border max-height="360">
          <el-table-column label="类型" width="96" align="center">
            <template #default="scope">
              <el-tag :type="resolveDiffTagType(scope.row.changeType)" effect="plain">
                {{ resolveDiffLabel(scope.row.changeType) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="字段路径" prop="fieldPath" min-width="220" :show-overflow-tooltip="true" />
          <el-table-column label="变更前" prop="beforeValue" min-width="260" :show-overflow-tooltip="true" />
          <el-table-column label="变更后" prop="afterValue" min-width="260" :show-overflow-tooltip="true" />
        </el-table>
        <el-empty v-if="!fieldDiffRows.length" description="未发现字段级差异" :image-size="72" />
      </div>

      <div class="audit-page__compare">
        <div class="audit-page__compare-panel">
          <h4>变更前</h4>
          <JsonEditor :model-value="formatJson(detailData.beforeJson)" title="变更前" readonly :rows="16" />
        </div>
        <div class="audit-page__compare-panel">
          <h4>变更后</h4>
          <JsonEditor :model-value="formatJson(detailData.afterJson)" title="变更后" readonly :rows="16" />
        </div>
      </div>
    </el-drawer>
  </div>
</template>

<script setup name="CostAudit">
import JsonEditor from '@/components/cost/JsonEditor.vue'
import { listAudit, getAuditStats } from '@/api/cost/governance'
import { optionselectScene } from '@/api/cost/scene'
import useSettingsStore from '@/store/modules/settings'
import { resolveWorkingCostSceneId } from '@/utils/costSceneContext'

const { proxy } = getCurrentInstance()
const settingsStore = useSettingsStore()
const isCompactMode = computed(() => settingsStore.costPageMode === 'COMPACT')

const loading = ref(false)
const showSearch = ref(true)
const total = ref(0)
const auditList = ref([])
const sceneOptions = ref([])
const detailOpen = ref(false)
const detailData = ref(null)
const stats = reactive({ auditCount: 0, sceneCount: 0, operatorCount: 0, todayCount: 0 })

const queryParams = reactive({
  pageNum: 1,
  pageSize: 10,
  sceneId: undefined,
  objectType: '',
  actionType: '',
  objectCode: '',
  operatorCode: ''
})

const metricItems = computed(() => [
  { label: '审计总数', value: stats.auditCount, desc: '当前筛选范围内的审计动作数量' },
  { label: '涉及场景数', value: stats.sceneCount, desc: '本次查询覆盖的场景数量' },
  { label: '操作人数', value: stats.operatorCount, desc: '本次查询中出现过的操作人数量' },
  { label: '今日新增', value: stats.todayCount, desc: '当天新增的审计记录数量' }
])

const responsibilityItems = computed(() => {
  const row = detailData.value || {}
  return [
    { label: '责任人', value: resolveOperator(row), desc: '来自审计记录的 operatorName/operatorCode' },
    { label: '操作时间', value: proxy.parseTime(row.operateTime) || '-', desc: '用于追踪配置变化发生时间' },
    { label: '请求号', value: row.requestNo || '-', desc: '用于串联同一次业务请求' },
    { label: '动作摘要', value: row.actionSummary || '-', desc: '记录本次变更的业务语义' }
  ]
})

const fieldDiffRows = computed(() => buildFieldDiffRows(detailData.value?.beforeJson, detailData.value?.afterJson))

async function loadScenes() {
  const resp = await optionselectScene({ status: '0', pageNum: 1, pageSize: 1000 })
  sceneOptions.value = resp?.data || []
  queryParams.sceneId = resolveWorkingCostSceneId(sceneOptions.value, queryParams.sceneId)
}

async function getList() {
  loading.value = true
  try {
    await loadScenes()
    const [listResp, statsResp] = await Promise.all([
      listAudit(queryParams),
      getAuditStats(queryParams)
    ])
    auditList.value = listResp.rows || []
    total.value = listResp.total || 0
    Object.assign(stats, statsResp.data || {})
  } finally {
    loading.value = false
  }
}

function handleQuery() {
  queryParams.pageNum = 1
  getList()
}

function handleExport() {
  proxy.download('cost/governance/audit/export', {
    ...queryParams
  }, `cost_audit_${Date.now()}.xlsx`)
}

function resetQuery() {
  proxy.resetForm('queryRef')
  queryParams.pageNum = 1
  queryParams.pageSize = 10
  getList()
}

function handleDetail(row) {
  detailData.value = row
  detailOpen.value = true
}

function resolveOperator(row) {
  if (!row) {
    return '-'
  }
  if (row.operatorName && row.operatorCode && row.operatorName !== row.operatorCode) {
    return `${row.operatorName} (${row.operatorCode})`
  }
  return row.operatorName || row.operatorCode || '-'
}

function formatJson(text) {
  if (!text) {
    return '{}'
  }
  try {
    return JSON.stringify(JSON.parse(text), null, 2)
  } catch (error) {
    return text
  }
}

function buildFieldDiffRows(beforeJson, afterJson) {
  const beforeMap = flattenAuditJson(parseAuditJson(beforeJson))
  const afterMap = flattenAuditJson(parseAuditJson(afterJson))
  const fieldPaths = [...new Set([...Object.keys(beforeMap), ...Object.keys(afterMap)])].sort()
  return fieldPaths
    .map(fieldPath => {
      const hasBefore = Object.prototype.hasOwnProperty.call(beforeMap, fieldPath)
      const hasAfter = Object.prototype.hasOwnProperty.call(afterMap, fieldPath)
      const beforeValue = hasBefore ? beforeMap[fieldPath] : ''
      const afterValue = hasAfter ? afterMap[fieldPath] : ''
      const changeType = !hasBefore ? 'ADDED' : (!hasAfter ? 'REMOVED' : (beforeValue === afterValue ? 'UNCHANGED' : 'CHANGED'))
      return { fieldPath, beforeValue, afterValue, changeType }
    })
    .filter(item => item.changeType !== 'UNCHANGED')
}

function parseAuditJson(text) {
  if (!text) {
    return undefined
  }
  try {
    return JSON.parse(text)
  } catch (error) {
    return text
  }
}

function flattenAuditJson(value, fieldPath = '', result = {}) {
  if (value && typeof value === 'object' && !Array.isArray(value)) {
    const entries = Object.entries(value)
    if (!entries.length && fieldPath) {
      result[fieldPath] = '{}'
    }
    entries.forEach(([key, child]) => {
      flattenAuditJson(child, fieldPath ? `${fieldPath}.${key}` : key, result)
    })
    return result
  }
  result[fieldPath || '(根)'] = formatAuditValue(value)
  return result
}

function formatAuditValue(value) {
  if (value === undefined || value === null) {
    return ''
  }
  if (typeof value === 'object') {
    return JSON.stringify(value)
  }
  return String(value)
}

function resolveDiffLabel(type) {
  return { ADDED: '新增', REMOVED: '删除', CHANGED: '修改' }[type] || type
}

function resolveDiffTagType(type) {
  return { ADDED: 'success', REMOVED: 'danger', CHANGED: 'warning' }[type] || 'info'
}

onMounted(() => {
  getList()
})

onActivated(() => {
  getList()
})
</script>

<style lang="scss" scoped>
.audit-page {
  display: flex;
  flex-direction: column;
  gap: 20px;

  &__hero,
  &__metric-card,
  &__panel {
    background: #fff;
    border-radius: 20px;
    border: 1px solid #e7edf7;
    box-shadow: 0 14px 35px rgba(15, 34, 58, 0.06);
  }

  &__hero,
  &__panel {
    padding: 24px 28px;
  }

  &__hero {
    display: flex;
    align-items: flex-start;
    justify-content: space-between;
    gap: 24px;
  }

  &__eyebrow {
    color: #c7872d;
    font-size: 13px;
    font-weight: 600;
    margin-bottom: 10px;
  }

  &__title {
    margin: 0;
    color: #10233e;
    font-size: 24px;
  }

  &__subtitle {
    margin: 12px 0 0;
    color: #64748b;
    line-height: 1.7;
  }

  &__metrics {
    display: grid;
    grid-template-columns: repeat(4, minmax(0, 1fr));
    gap: 16px;
  }

  &__metric-card {
    padding: 18px 22px;
    display: flex;
    flex-direction: column;
    gap: 8px;

    strong {
      color: #3c8cff;
      font-size: 24px;
    }

    span,
    small {
      color: #7c8798;
    }
  }

  &__section-head {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-bottom: 18px;

    h3 {
      margin: 0;
      font-size: 22px;
      color: #10233e;
    }

    p {
      margin: 8px 0 0;
      color: #708198;
    }
  }

  &__section-head--tight {
    margin-bottom: 12px;

    h3 {
      font-size: 18px;
    }
  }

  &__section-actions {
    display: flex;
    align-items: center;
    gap: 10px;
  }

  &__responsibility {
    margin-top: 18px;
    display: grid;
    grid-template-columns: repeat(4, minmax(0, 1fr));
    gap: 12px;
  }

  &__responsibility-card {
    min-width: 0;
    padding: 14px;
    border: 1px solid #e7edf7;
    border-radius: 8px;
    background: #f8fafc;

    span,
    small {
      display: block;
      color: #7c8798;
    }

    strong {
      display: block;
      margin: 6px 0;
      color: #10233e;
      word-break: break-word;
    }
  }

  &__field-diff {
    margin-top: 18px;
    padding: 16px;
    border: 1px solid #e7edf7;
    border-radius: 8px;
    background: #fff;
  }

  &__compare {
    margin-top: 20px;
    display: grid;
    grid-template-columns: repeat(2, minmax(0, 1fr));
    gap: 16px;
  }

  &__compare-panel {
    border: 1px solid #e7edf7;
    border-radius: 18px;
    overflow: hidden;

    h4 {
      margin: 0;
      padding: 16px 18px;
      background: #f8fafc;
      color: #10233e;
    }

    pre {
      margin: 0;
      padding: 18px;
      max-height: 520px;
      overflow: auto;
      background: #0f172a;
      color: #dbeafe;
      white-space: pre-wrap;
      word-break: break-all;
    }
  }
}

@media (max-width: 1360px) {
  .audit-page {
    &__metrics,
    &__responsibility,
    &__compare {
      grid-template-columns: 1fr;
    }
  }
}
</style>

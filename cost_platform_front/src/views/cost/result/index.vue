<template>
  <div class="app-container result-page">
    <section class="result-page__hero">
      <div>
        <div class="result-page__eyebrow">结果追溯</div>
        <h2 class="result-page__title">结果台账与追溯解释</h2>
        <p class="result-page__subtitle">
          按场景、版本、账期、任务和费用维度查看正式核算结果，支持命中规则、变量取值和执行过程追溯。
        </p>
      </div>
      <el-tag type="success">支持结果查询、差异定位和过程解释联动查看</el-tag>
    </section>

    <section class="result-page__metrics">
      <div v-for="item in metricItems" :key="item.label" class="result-page__metric-card">
        <span>{{ item.label }}</span>
        <strong>{{ item.value }}</strong>
        <small>{{ item.desc }}</small>
      </div>
    </section>

    <el-form ref="queryRef" :model="queryParams" :inline="true" label-width="84px" v-show="showSearch">
      <el-form-item label="所属场景" prop="sceneId">
        <el-select v-model="queryParams.sceneId" clearable filterable style="width: 220px" @change="handleSceneChange">
          <el-option v-for="item in sceneOptions" :key="item.sceneId" :label="`${item.sceneName} / ${item.sceneCode}`" :value="item.sceneId" />
        </el-select>
      </el-form-item>
      <el-form-item label="版本号" prop="versionId">
        <el-select v-model="queryParams.versionId" clearable filterable style="width: 220px">
          <el-option v-for="item in versionOptions" :key="item.versionId" :label="item.versionNo" :value="item.versionId" />
        </el-select>
      </el-form-item>
      <el-form-item label="账期" prop="billMonth">
        <el-input v-model="queryParams.billMonth" clearable placeholder="yyyy-MM" style="width: 160px" />
      </el-form-item>
      <el-form-item label="任务ID" prop="taskId">
        <el-input v-model="queryParams.taskId" clearable style="width: 140px" />
      </el-form-item>
      <el-form-item label="任务号" prop="taskNo">
        <el-input v-model="queryParams.taskNo" clearable style="width: 200px" />
      </el-form-item>
      <el-form-item label="费用编码" prop="feeCode">
        <el-input v-model="queryParams.feeCode" clearable style="width: 180px" />
      </el-form-item>
      <el-form-item label="业务单号" prop="bizNo">
        <el-input v-model="queryParams.bizNo" clearable style="width: 180px" />
      </el-form-item>
      <el-form-item label="结果状态" prop="resultStatus">
        <el-select v-model="queryParams.resultStatus" clearable style="width: 160px">
          <el-option v-for="item in resultStatusOptions" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <div class="result-page__table">
      <div class="result-page__section-head">
        <div>
          <h3>结果台账</h3>
          <p>列表用于快速定位任务、费用和业务对象，详情用于查看结果和追溯解释。</p>
        </div>
        <div class="result-page__section-actions">
          <el-button type="warning" plain icon="Download" @click="handleExport">
            导出结果
          </el-button>
          <right-toolbar v-model:showSearch="showSearch" @queryTable="getList" />
        </div>
      </div>

      <el-alert
        v-if="routeContext.taskId"
        class="result-page__context"
        type="info"
        :closable="false"
        :title="`当前正在查看任务 ${routeContext.taskId} 的结果台账，已自动带入场景与账期过滤。`"
      />

      <el-table v-loading="loading" :data="resultList">
        <el-table-column label="任务号" prop="taskNo" width="220" />
        <el-table-column label="场景" min-width="180">
          <template #default="scope">{{ scope.row.sceneName }} ({{ scope.row.sceneCode }})</template>
        </el-table-column>
        <el-table-column label="版本" prop="versionNo" width="150" />
        <el-table-column label="账期" prop="billMonth" width="110" align="center" />
        <el-table-column label="费用" min-width="180">
          <template #default="scope">{{ scope.row.feeName }} ({{ scope.row.feeCode }})</template>
        </el-table-column>
        <el-table-column label="业务单号" prop="bizNo" min-width="160" />
        <el-table-column label="核算对象" min-width="180">
          <template #default="scope">{{ scope.row.objectName || '-' }} / {{ scope.row.objectCode || '-' }}</template>
        </el-table-column>
        <el-table-column label="数量" min-width="140" align="right">
          <template #default="scope">{{ formatQuantity(scope.row) }}</template>
        </el-table-column>
        <el-table-column label="单价" min-width="140" align="right">
          <template #default="scope">{{ formatUnitPrice(scope.row) }}</template>
        </el-table-column>
        <el-table-column label="计价口径" min-width="220" :show-overflow-tooltip="true">
          <template #default="scope">{{ resolveUnitSemantic(scope.row).summary }}</template>
        </el-table-column>
        <el-table-column label="金额" prop="amountValue" width="120" align="right" />
        <el-table-column label="状态" width="110" align="center">
          <template #default="scope">
            <dict-tag :options="resultStatusOptions" :value="scope.row.resultStatus" />
          </template>
        </el-table-column>
        <el-table-column label="操作" width="170" fixed="right" align="center">
          <template #default="scope">
            <el-button link type="primary" icon="View" @click="handleDetail(scope.row)">详情</el-button>
            <el-button link type="success" icon="Connection" @click="handleTrace(scope.row)" :disabled="!scope.row.traceId" v-hasPermi="['cost:result:trace']">追溯</el-button>
          </template>
        </el-table-column>
      </el-table>

      <pagination v-show="total > 0" :total="total" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" @pagination="getList" />
    </div>

    <el-drawer v-model="detailOpen" title="结果详情" size="980px" append-to-body>
      <el-descriptions v-if="detailData.ledger" :column="2" border>
        <el-descriptions-item label="任务号">{{ detailData.ledger.taskNo }}</el-descriptions-item>
        <el-descriptions-item label="版本">{{ detailData.ledger.versionNo }}</el-descriptions-item>
        <el-descriptions-item label="费用">{{ detailData.ledger.feeName }} ({{ detailData.ledger.feeCode }})</el-descriptions-item>
        <el-descriptions-item label="计价单位">{{ resolveUnitLabel(detailData.ledger.unitCode) }}</el-descriptions-item>
        <el-descriptions-item label="业务单号">{{ detailData.ledger.bizNo }}</el-descriptions-item>
        <el-descriptions-item label="计价口径">{{ resolveUnitSemantic(detailData.ledger).summary }}</el-descriptions-item>
        <el-descriptions-item label="账期">{{ detailData.ledger.billMonth }}</el-descriptions-item>
        <el-descriptions-item label="金额">{{ detailData.ledger.amountValue }}</el-descriptions-item>
        <el-descriptions-item label="结果解释" :span="2">{{ resolveUnitSemantic(detailData.ledger).resultHint }}</el-descriptions-item>
      </el-descriptions>

      <el-tabs class="result-page__tabs">
        <el-tab-pane label="结果记录">
          <pre>{{ formatJson(detailData.ledger) }}</pre>
        </el-tab-pane>
        <el-tab-pane label="追溯解释">
          <pre>{{ formatJson(detailData.trace) }}</pre>
        </el-tab-pane>
      </el-tabs>
    </el-drawer>

    <el-drawer v-model="traceOpen" title="追溯解释" size="980px" append-to-body>
      <el-tabs class="result-page__tabs">
        <el-tab-pane label="变量值">
          <pre>{{ formatJson(traceData.variables) }}</pre>
        </el-tab-pane>
        <el-tab-pane label="条件命中">
          <pre>{{ formatJson(traceData.conditions) }}</pre>
        </el-tab-pane>
        <el-tab-pane label="定价过程">
          <pre>{{ formatJson(traceData.pricing) }}</pre>
        </el-tab-pane>
        <el-tab-pane label="执行时间线">
          <pre>{{ formatJson(traceData.timeline) }}</pre>
        </el-tab-pane>
      </el-tabs>
    </el-drawer>
  </div>
</template>

<script setup name="CostResult">
import { getResultDetail, getResultStats, getTraceDetail, listResult, listVersionOptions } from '@/api/cost/run'
import { optionselectScene } from '@/api/cost/scene'
import { resolveWorkingCostSceneId } from '@/utils/costSceneContext'
import { getCostUnitSemantic } from '@/utils/costUnitSemantics'
import { getRemoteDictOptionMap } from '@/utils/dictRemote'

const route = useRoute()
const router = useRouter()
const { proxy } = getCurrentInstance()

const loading = ref(false)
const showSearch = ref(true)
const total = ref(0)
const resultList = ref([])
const sceneOptions = ref([])
const versionOptions = ref([])
const resultStatusOptions = ref([])
const unitCodeOptions = ref([])
const detailOpen = ref(false)
const traceOpen = ref(false)
const detailData = ref({})
const traceData = ref({})
const stats = reactive({ resultCount: 0, taskCount: 0, traceCount: 0, amountTotal: 0 })
const routeContext = reactive({
  taskId: route.query.taskId ? Number(route.query.taskId) : undefined
})
const lastOpenedRouteResultKey = ref('')

const queryParams = reactive({
  pageNum: 1,
  pageSize: 10,
  sceneId: route.query.sceneId ? Number(route.query.sceneId) : undefined,
  versionId: undefined,
  billMonth: route.query.billMonth || '',
  taskId: route.query.taskId ? Number(route.query.taskId) : undefined,
  taskNo: '',
  feeCode: '',
  bizNo: '',
  resultStatus: undefined
})

const metricItems = computed(() => [
  { label: '结果条数', value: stats.resultCount, desc: '当前筛选范围内的结果台账条数' },
  { label: '任务数量', value: stats.taskCount, desc: '结果覆盖的任务数' },
  { label: '追溯记录', value: stats.traceCount, desc: '已生成的追溯解释数量' },
  { label: '金额合计', value: stats.amountTotal, desc: '当前筛选结果的金额汇总' }
])

async function loadBaseOptions() {
  const [dictMap, sceneResp] = await Promise.all([
    getRemoteDictOptionMap(['cost_result_status', 'cost_unit_code']),
    optionselectScene({ status: '0', pageNum: 1, pageSize: 1000 })
  ])
  resultStatusOptions.value = dictMap.cost_result_status || []
  unitCodeOptions.value = dictMap.cost_unit_code || []
  sceneOptions.value = sceneResp?.data || []
  queryParams.sceneId = resolveWorkingCostSceneId(
    sceneOptions.value,
    queryParams.sceneId,
    route.query.sceneId ? Number(route.query.sceneId) : undefined
  )
}

async function loadVersionOptions(sceneId) {
  if (!sceneId) {
    versionOptions.value = []
    return
  }
  const resp = await listVersionOptions(sceneId)
  versionOptions.value = resp.data || []
}

async function getList() {
  loading.value = true
  try {
    await loadBaseOptions()
    await loadVersionOptions(queryParams.sceneId)
    const [listResp, statsResp] = await Promise.all([
      listResult(queryParams),
      getResultStats(queryParams)
    ])
    resultList.value = listResp.rows || []
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
  proxy.download('cost/run/result/export', {
    ...queryParams
  }, `cost_result_${Date.now()}.xlsx`)
}

function resetQuery() {
  proxy.resetForm('queryRef')
  queryParams.pageNum = 1
  queryParams.pageSize = 10
  queryParams.taskId = routeContext.taskId
  queryParams.sceneId = route.query.sceneId ? Number(route.query.sceneId) : queryParams.sceneId
  queryParams.billMonth = route.query.billMonth || ''
  versionOptions.value = []
  getList()
}

async function handleSceneChange(sceneId) {
  queryParams.sceneId = sceneId
  queryParams.versionId = undefined
  await loadVersionOptions(queryParams.sceneId)
}

async function handleDetail(row) {
  const resp = await getResultDetail(row.resultId)
  detailData.value = resp.data || {}
  detailOpen.value = true
}

async function handleTrace(row) {
  const resp = await getTraceDetail(row.traceId)
  traceData.value = resp.data || {}
  traceOpen.value = true
}

async function openFirstResultByRouteContext() {
  if (!routeContext.taskId || !resultList.value.length) return
  const currentKey = `${routeContext.taskId}:${resultList.value[0].resultId}`
  if (lastOpenedRouteResultKey.value === currentKey) return
  await handleDetail(resultList.value[0])
  lastOpenedRouteResultKey.value = currentKey
}

function resolveUnitLabel(unitCode) {
  const match = unitCodeOptions.value.find(item => item.value === unitCode)
  return match ? match.label : (unitCode || '-')
}

function resolveUnitSemantic(row) {
  return getCostUnitSemantic(row?.unitCode, resolveUnitLabel(row?.unitCode))
}

function formatQuantity(row) {
  if (row?.quantityValue == null) {
    return '-'
  }
  return `${row.quantityValue} ${resolveUnitLabel(row.unitCode)}`
}

function formatUnitPrice(row) {
  if (row?.unitPrice == null) {
    return '-'
  }
  const unitLabel = resolveUnitLabel(row.unitCode)
  return unitLabel === '-' ? String(row.unitPrice) : `${row.unitPrice} / ${unitLabel}`
}

function formatJson(value) {
  return JSON.stringify(value || {}, null, 2)
}

watch(
  () => route.query,
  value => {
    routeContext.taskId = value.taskId ? Number(value.taskId) : undefined
    queryParams.taskId = routeContext.taskId
    queryParams.sceneId = value.sceneId ? Number(value.sceneId) : queryParams.sceneId
    queryParams.billMonth = value.billMonth || queryParams.billMonth
  },
  { deep: true }
)

onMounted(async () => {
  await getList()
  await openFirstResultByRouteContext()
})

onActivated(async () => {
  await getList()
  await openFirstResultByRouteContext()
})
</script>

<style scoped lang="scss">
.result-page { display: grid; gap: 16px; }
.result-page__hero, .result-page__metric-card, .result-page__table { border: 1px solid var(--el-border-color); border-radius: 16px; background: var(--el-bg-color-overlay); }
.result-page__hero { display: flex; justify-content: space-between; gap: 16px; padding: 22px 24px; background: linear-gradient(135deg, color-mix(in srgb, var(--el-color-success-light-8) 56%, var(--el-bg-color-overlay)), var(--el-bg-color-overlay)); }
.result-page__eyebrow { font-size: 12px; color: var(--el-color-success-dark-2); font-weight: 700; letter-spacing: .08em; text-transform: uppercase; }
.result-page__title { margin: 8px 0 0; font-size: 28px; }
.result-page__subtitle { margin: 10px 0 0; color: var(--el-text-color-regular); line-height: 1.8; }
.result-page__metrics { display: grid; grid-template-columns: repeat(4, minmax(0, 1fr)); gap: 14px; }
.result-page__metric-card { display: grid; gap: 6px; padding: 14px 16px; }
.result-page__metric-card strong { font-size: 26px; color: var(--el-color-success-dark-2); }
.result-page__table { padding: 16px; }
.result-page__section-head { display: flex; justify-content: space-between; align-items: center; gap: 12px; margin-bottom: 16px; }
.result-page__section-actions { display: flex; align-items: center; gap: 10px; }
.result-page__section-head h3 { margin: 0; font-size: 18px; }
.result-page__section-head p { margin: 6px 0 0; color: var(--el-text-color-secondary); font-size: 13px; }
.result-page__tabs pre { margin: 0; white-space: pre-wrap; word-break: break-all; }
@media (max-width: 1200px) {
  .result-page__metrics { grid-template-columns: 1fr; }
}
</style>

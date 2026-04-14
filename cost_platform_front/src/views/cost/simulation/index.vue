<template>
  <div class="app-container run-page">
    <section class="run-page__hero">
      <div>
        <div class="run-page__eyebrow">试算验证</div>
        <h2 class="run-page__title">试算中心</h2>
        <p class="run-page__subtitle">按指定版本执行单笔或批量试算；未选择版本时，自动按当前配置执行并返回解释过程。</p>
      </div>
      <el-tag type="success">输入示例会按当前场景和指定版本或当前配置自动生成，便于快速开始试算</el-tag>
    </section>

    <section class="run-page__metrics">
      <div v-for="item in metricItems" :key="item.label" class="run-page__metric-card">
        <span>{{ item.label }}</span>
        <strong>{{ item.value }}</strong>
        <small>{{ item.desc }}</small>
      </div>
    </section>

    <el-form ref="queryRef" :model="queryParams" :inline="true" label-width="84px" v-show="showSearch">
      <el-form-item label="所属场景" prop="sceneId">
        <el-select v-model="queryParams.sceneId" clearable filterable style="width: 220px" @change="handleQuerySceneChange">
          <el-option v-for="item in sceneOptions" :key="item.sceneId" :label="`${item.sceneName} / ${item.sceneCode}`" :value="item.sceneId" />
        </el-select>
      </el-form-item>
      <el-form-item label="版本号" prop="versionId">
        <el-select v-model="queryParams.versionId" clearable filterable style="width: 220px" placeholder="全部版本">
          <el-option v-for="item in versionOptions" :key="item.versionId" :label="item.versionNo" :value="item.versionId" />
        </el-select>
      </el-form-item>
      <el-form-item label="账期" prop="billMonth">
        <el-date-picker
          v-model="queryParams.billMonth"
          clearable
          type="month"
          format="YYYY-MM"
          value-format="YYYY-MM"
          placeholder="选择账期"
          style="width: 160px"
        />
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-select v-model="queryParams.status" clearable style="width: 180px">
          <el-option v-for="item in simulationStatusOptions" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <section class="run-page__workspace">
      <div class="run-page__panel">
        <div class="run-page__section-head">
          <div>
            <h3>执行试算</h3>
            <p>试算可基于指定版本执行；未选择版本时，自动按当前配置执行。</p>
          </div>
          <right-toolbar v-model:showSearch="showSearch" @queryTable="getList" />
        </div>

        <el-form :model="form" label-width="92px">
          <el-form-item label="执行模式">
            <el-radio-group v-model="form.executeMode" @change="handleExecuteModeChange">
              <el-radio value="SINGLE">单笔试算</el-radio>
              <el-radio value="BATCH">批量试算</el-radio>
            </el-radio-group>
          </el-form-item>
          <el-form-item label="试算场景" required>
            <el-select v-model="form.sceneId" filterable style="width: 100%" @change="handleFormSceneChange">
              <el-option v-for="item in sceneOptions" :key="item.sceneId" :label="`${item.sceneName} / ${item.sceneCode}`" :value="item.sceneId" />
            </el-select>
          </el-form-item>
          <el-form-item label="执行版本">
            <el-select v-model="form.versionId" clearable filterable style="width: 100%" placeholder="不选则按当前配置">
              <el-option v-for="item in formVersionOptions" :key="item.versionId" :label="`${item.versionNo} / ${item.versionStatus}`" :value="item.versionId" />
            </el-select>
          </el-form-item>
          <el-form-item label="账期" required>
            <el-date-picker
              v-model="form.billMonth"
              type="month"
              format="YYYY-MM"
              value-format="YYYY-MM"
              placeholder="选择账期"
              style="width: 100%"
            />
          </el-form-item>
          <el-form-item label="输入 JSON" required>
            <el-input v-model="form.inputJson" type="textarea" :rows="14" maxlength="10000" show-word-limit />
          </el-form-item>
        </el-form>

        <div class="run-page__action-row">
          <el-button type="primary" icon="Promotion" @click="handleExecute" v-hasPermi="['cost:simulation:execute']">{{ form.executeMode === 'BATCH' ? '执行批量试算' : '执行试算' }}</el-button>
          <el-button icon="RefreshLeft" @click="fillExample">按配置生成模板</el-button>
        </div>

        <el-alert v-if="templateMessage" :title="templateMessage" type="info" :closable="false" class="run-page__template-alert" />
        <el-alert
          v-if="batchResult.totalCount"
          :title="`本次批量试算共 ${batchResult.totalCount} 条，成功 ${batchResult.successCount} 条，失败 ${batchResult.failedCount} 条`"
          :type="batchResult.failedCount ? 'warning' : 'success'"
          :closable="false"
          class="run-page__template-alert"
        />
        <el-table v-if="templateFields.length" :data="templateFields" size="small" border class="run-page__template-table">
          <el-table-column label="输入路径" prop="path" min-width="180" />
          <el-table-column label="变量" min-width="180">
            <template #default="scope">{{ scope.row.variableName }} ({{ scope.row.variableCode }})</template>
          </el-table-column>
          <el-table-column label="来源" prop="sourceType" width="120" />
          <el-table-column label="类型" prop="dataType" width="120" />
          <el-table-column label="模板角色" prop="templateRole" width="140" />
        </el-table>
      </div>

      <div class="run-page__panel">
        <div class="run-page__section-head">
          <div>
            <h3>试算记录</h3>
            <p>保留最近试算输入、变量结果、费用结果和解释信息。</p>
          </div>
        </div>

        <el-table v-loading="loading" :data="recordList">
          <el-table-column label="试算编号" prop="simulationNo" width="220" />
          <el-table-column label="场景" min-width="180">
            <template #default="scope">{{ scope.row.sceneName }} ({{ scope.row.sceneCode }})</template>
          </el-table-column>
          <el-table-column label="版本" prop="versionNo" width="160" />
          <el-table-column label="账期" prop="billMonth" width="110" align="center" />
          <el-table-column label="状态" width="110" align="center">
            <template #default="scope">
              <dict-tag :options="simulationStatusOptions" :value="scope.row.status" />
            </template>
          </el-table-column>
          <el-table-column label="创建时间" width="180" align="center">
            <template #default="scope">{{ parseTime(scope.row.createTime) }}</template>
          </el-table-column>
          <el-table-column label="操作" width="120" fixed="right" align="center">
            <template #default="scope">
              <el-button link type="primary" icon="View" @click="handleDetail(scope.row)">详情</el-button>
            </template>
          </el-table-column>
        </el-table>

        <pagination v-show="total > 0" :total="total" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" @pagination="getList" />
      </div>
    </section>

    <el-drawer v-model="detailOpen" title="试算详情" size="980px" append-to-body>
      <el-descriptions v-if="detailData.record" :column="2" border>
        <el-descriptions-item label="试算编号">{{ detailData.record.simulationNo }}</el-descriptions-item>
        <el-descriptions-item label="状态">{{ resolveSimulationStatus(detailData.record.status) }}</el-descriptions-item>
        <el-descriptions-item label="场景">{{ detailData.record.sceneName }}</el-descriptions-item>
        <el-descriptions-item label="版本">{{ detailData.record.versionNo }}</el-descriptions-item>
        <el-descriptions-item label="账期">{{ detailData.record.billMonth || '-' }}</el-descriptions-item>
        <el-descriptions-item label="创建时间">{{ parseTime(detailData.record.createTime) }}</el-descriptions-item>
        <el-descriptions-item label="异常信息">{{ detailData.record.errorMessage || '-' }}</el-descriptions-item>
      </el-descriptions>

      <el-tabs class="run-page__tabs">
        <el-tab-pane label="输入数据">
          <pre>{{ formatJson(detailData.input) }}</pre>
        </el-tab-pane>
        <el-tab-pane label="变量结果">
          <pre>{{ formatJson(detailData.variables) }}</pre>
        </el-tab-pane>
        <el-tab-pane label="费用结果">
          <pre>{{ formatJson(detailData.result) }}</pre>
        </el-tab-pane>
        <el-tab-pane label="解释时间线">
          <pre>{{ formatJson(detailData.explain) }}</pre>
        </el-tab-pane>
      </el-tabs>
    </el-drawer>

    <el-dialog v-model="batchOpen" title="批量试算结果" width="1100px" append-to-body>
      <el-table :data="batchResult.records || []" size="small">
        <el-table-column label="试算编号" prop="simulationNo" min-width="180" />
        <el-table-column label="状态" width="100" align="center">
          <template #default="scope">
            <dict-tag :options="simulationStatusOptions" :value="scope.row.status" />
          </template>
        </el-table-column>
        <el-table-column label="输入摘要" min-width="320">
          <template #default="scope">{{ summarizeBatchJson(scope.row.input) }}</template>
        </el-table-column>
        <el-table-column label="结果摘要" min-width="240">
          <template #default="scope">{{ summarizeBatchJson(scope.row.result) }}</template>
        </el-table-column>
        <el-table-column label="异常信息" min-width="220" :show-overflow-tooltip="true">
          <template #default="scope">{{ scope.row.errorMessage || '-' }}</template>
        </el-table-column>
        <el-table-column label="操作" width="100" fixed="right" align="center">
          <template #default="scope">
            <el-button link type="primary" icon="View" @click="handleBatchDetail(scope.row)">详情</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-dialog>
  </div>
</template>

<script setup name="CostSimulation">
import { executeSimulation, executeSimulationBatch, getRunInputTemplate, getSimulationDetail, getSimulationStats, listSimulation, listVersionOptions } from '@/api/cost/run'
import { optionselectScene } from '@/api/cost/scene'
import { resolveWorkingCostSceneId } from '@/utils/costSceneContext'
import { clearCostWorkContext, resolveWorkingBillMonth, resolveWorkingVersionId, syncCostWorkContext } from '@/utils/costWorkContext'
import { getRemoteDictOptionMap } from '@/utils/dictRemote'

const route = useRoute()
const { proxy } = getCurrentInstance()

const loading = ref(false)
const showSearch = ref(true)
const total = ref(0)
const recordList = ref([])
const sceneOptions = ref([])
const versionOptions = ref([])
const formVersionOptions = ref([])
const simulationStatusOptions = ref([])
const templateFields = ref([])
const templateMessage = ref('')
const detailOpen = ref(false)
const batchOpen = ref(false)
const batchResult = ref({})
const detailData = ref({})
const stats = reactive({ simulationCount: 0, successCount: 0, failedCount: 0, sceneCount: 0 })

const queryParams = reactive({
  pageNum: 1,
  pageSize: 10,
  sceneId: route.query.sceneId ? Number(route.query.sceneId) : undefined,
  billMonth: resolveWorkingBillMonth(route.query.billMonth),
  versionId: resolveWorkingVersionId(route.query.versionId ? Number(route.query.versionId) : undefined),
  status: undefined
})

const form = reactive({
  executeMode: 'SINGLE',
  sceneId: route.query.sceneId ? Number(route.query.sceneId) : undefined,
  billMonth: resolveWorkingBillMonth(route.query.billMonth),
  versionId: resolveWorkingVersionId(route.query.versionId ? Number(route.query.versionId) : undefined),
  inputJson: ''
})

const metricItems = computed(() => [
  { label: '试算次数', value: stats.simulationCount, desc: '当前筛选范围内试算记录总数' },
  { label: '成功次数', value: stats.successCount, desc: '试算成功的记录数' },
  { label: '失败次数', value: stats.failedCount, desc: '试算失败的记录数' },
  { label: '覆盖场景', value: stats.sceneCount, desc: '已执行试算的场景数量' }
])

async function loadBaseOptions() {
  const [dictMap, sceneResp] = await Promise.all([
    getRemoteDictOptionMap(['cost_simulation_status']),
    optionselectScene({ status: '0', pageNum: 1, pageSize: 1000 })
  ])
  simulationStatusOptions.value = dictMap.cost_simulation_status || []
  sceneOptions.value = sceneResp?.data || []
  queryParams.sceneId = resolveWorkingCostSceneId(sceneOptions.value, queryParams.sceneId)
  form.sceneId = resolveWorkingCostSceneId(sceneOptions.value, form.sceneId, queryParams.sceneId)
}

async function loadVersionOptions(sceneId, target) {
  if (!sceneId) {
    target.value = []
    return
  }
  const resp = await listVersionOptions(sceneId)
  target.value = resp.data || []
}

async function getList() {
  loading.value = true
  try {
    await loadBaseOptions()
    if (queryParams.sceneId) {
      await loadVersionOptions(queryParams.sceneId, versionOptions)
    } else {
      versionOptions.value = []
    }
    if (form.sceneId) {
      await loadVersionOptions(form.sceneId, formVersionOptions)
    } else {
      formVersionOptions.value = []
    }
    const [listResp, statsResp] = await Promise.all([
      listSimulation(queryParams),
      getSimulationStats(queryParams)
    ])
    recordList.value = listResp.rows || []
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

function resetQuery() {
  proxy.resetForm('queryRef')
  queryParams.pageNum = 1
  queryParams.pageSize = 10
  queryParams.billMonth = resolveWorkingBillMonth(route.query.billMonth, form.billMonth)
  versionOptions.value = []
  getList()
}

async function handleQuerySceneChange(sceneId) {
  queryParams.versionId = undefined
  queryParams.sceneId = sceneId
  form.sceneId = sceneId
  form.versionId = undefined
  clearCostWorkContext(['versionId'])
  syncCostWorkContext({ sceneId, billMonth: form.billMonth })
  await loadVersionOptions(sceneId, versionOptions)
  await loadVersionOptions(sceneId, formVersionOptions)
  await fillExample()
}

async function handleFormSceneChange(sceneId) {
  form.versionId = undefined
  queryParams.sceneId = sceneId
  queryParams.versionId = undefined
  form.sceneId = sceneId
  clearCostWorkContext(['versionId'])
  syncCostWorkContext({ sceneId, billMonth: form.billMonth })
  await loadVersionOptions(sceneId, formVersionOptions)
  await loadVersionOptions(sceneId, versionOptions)
  await fillExample()
}

async function fillExample() {
  if (!form.sceneId) {
    templateFields.value = []
    templateMessage.value = '请先选择试算场景，再按当前配置或指定版本生成输入模板。'
    return
  }
  const response = await getRunInputTemplate({
    sceneId: form.sceneId,
    versionId: form.versionId,
    taskType: form.executeMode === 'BATCH' ? 'SIMULATION_BATCH' : 'SIMULATION'
  })
  form.inputJson = response?.data?.inputJson || '{}'
  templateFields.value = response?.data?.fields?.filter(item => item.includedInTemplate) || []
  templateMessage.value = response?.data?.message || ''
}

async function handleExecute() {
  if (!form.sceneId || !form.billMonth || !form.inputJson) {
    proxy.$modal.msgWarning('请选择试算场景、账期并填写输入 JSON')
    return
  }
  if (form.executeMode === 'BATCH') {
    const resp = await executeSimulationBatch({
      sceneId: form.sceneId,
      billMonth: form.billMonth,
      versionId: form.versionId,
      inputJson: form.inputJson
    })
    batchResult.value = resp.data || {}
    batchOpen.value = true
    proxy.$modal.msgSuccess('批量试算执行完成')
  } else {
    const resp = await executeSimulation({
      sceneId: form.sceneId,
      billMonth: form.billMonth,
      versionId: form.versionId,
      inputJson: form.inputJson
    })
    proxy.$modal.msgSuccess('试算执行完成')
    detailData.value = resp.data || {}
    detailOpen.value = true
  }
  getList()
}

async function handleDetail(row) {
  const resp = await getSimulationDetail(row.simulationId)
  detailData.value = resp.data || {}
  detailOpen.value = true
}

async function handleBatchDetail(row) {
  batchOpen.value = false
  await handleDetail(row)
}

async function handleExecuteModeChange() {
  batchResult.value = {}
  await fillExample()
}

function resolveSimulationStatus(value) {
  return simulationStatusOptions.value.find(item => item.value === value)?.label || value
}

function formatJson(value) {
  return JSON.stringify(value || {}, null, 2)
}

function summarizeBatchJson(value) {
  const text = JSON.stringify(value || {})
  return text.length > 120 ? `${text.slice(0, 120)}...` : text
}

watch(
  () => [form.sceneId, form.versionId, form.billMonth],
  ([sceneId, versionId, billMonth]) => {
    syncCostWorkContext({ sceneId, versionId, billMonth })
  },
  { immediate: true }
)

onMounted(async () => {
  await getList()
  await fillExample()
})

onActivated(async () => {
  await getList()
  await fillExample()
})
</script>

<style scoped lang="scss">
.run-page { display: grid; gap: 16px; }
.run-page__hero, .run-page__metric-card, .run-page__panel { border: 1px solid var(--el-border-color); border-radius: 16px; background: var(--el-bg-color-overlay); }
.run-page__hero { display: flex; justify-content: space-between; gap: 16px; padding: 22px 24px; background: color-mix(in srgb, var(--el-color-primary-light-9) 16%, var(--el-bg-color-overlay)); }
.run-page__eyebrow { font-size: 12px; color: var(--el-color-primary); font-weight: 700; letter-spacing: .08em; text-transform: uppercase; }
.run-page__title { margin: 8px 0 0; font-size: 28px; }
.run-page__subtitle { margin: 10px 0 0; color: var(--el-text-color-regular); line-height: 1.8; }
.run-page__metrics { display: grid; grid-template-columns: repeat(4, minmax(0, 1fr)); gap: 14px; }
.run-page__metric-card { display: grid; gap: 6px; padding: 14px 16px; }
.run-page__metric-card strong { font-size: 26px; color: var(--el-color-primary); }
.run-page__workspace { display: grid; grid-template-columns: 420px minmax(0, 1fr); gap: 16px; }
.run-page__panel { padding: 16px; }
.run-page__section-head { display: flex; justify-content: space-between; align-items: center; gap: 12px; margin-bottom: 16px; }
.run-page__section-head h3 { margin: 0; font-size: 18px; }
.run-page__section-head p { margin: 6px 0 0; color: var(--el-text-color-secondary); font-size: 13px; }
.run-page__action-row { display: flex; gap: 10px; margin-top: 8px; }
.run-page__template-alert { margin-top: 14px; }
.run-page__template-table { margin-top: 12px; }
.run-page__tabs pre { margin: 0; white-space: pre-wrap; word-break: break-all; }
@media (max-width: 1200px) {
  .run-page__metrics, .run-page__workspace { grid-template-columns: 1fr; }
}
</style>

<template>
  <div class="app-container run-page">
    <section class="run-page__hero">
      <div>
        <div class="run-page__eyebrow">正式核算</div>
        <h2 class="run-page__title">正式核算与批量任务</h2>
        <p class="run-page__subtitle">按发布快照执行单笔或批量正式核算，异步落任务头、任务明细和结果写入进度，保留失败重试入口。</p>
      </div>
      <el-tag type="success">输入模板会按当前版本快照自动生成，并跟随最近一次所选场景同步</el-tag>
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
          <el-option v-for="item in sceneOptions" :key="item.sceneId" :label="`${item.sceneCode} / ${item.sceneName}`" :value="item.sceneId" />
        </el-select>
      </el-form-item>
      <el-form-item label="版本号" prop="versionId">
        <el-select v-model="queryParams.versionId" clearable filterable style="width: 220px">
          <el-option v-for="item in versionOptions" :key="item.versionId" :label="item.versionNo" :value="item.versionId" />
        </el-select>
      </el-form-item>
      <el-form-item label="任务类型" prop="taskType">
        <el-select v-model="queryParams.taskType" clearable style="width: 180px">
          <el-option v-for="item in taskTypeOptions" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="任务状态" prop="taskStatus">
        <el-select v-model="queryParams.taskStatus" clearable style="width: 180px">
          <el-option v-for="item in taskStatusOptions" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="账期" prop="billMonth">
        <el-input v-model="queryParams.billMonth" clearable placeholder="yyyy-MM" style="width: 160px" />
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
            <h3>提交任务</h3>
            <p>单笔正式核算提交对象 JSON，批量任务提交对象数组 JSON。</p>
          </div>
          <right-toolbar v-model:showSearch="showSearch" @queryTable="getList" />
        </div>

        <el-form :model="form" label-width="92px">
          <el-form-item label="运行场景" required>
            <el-select v-model="form.sceneId" filterable style="width: 100%" @change="handleFormSceneChange">
              <el-option v-for="item in sceneOptions" :key="item.sceneId" :label="`${item.sceneCode} / ${item.sceneName}`" :value="item.sceneId" />
            </el-select>
          </el-form-item>
          <el-form-item label="执行版本">
            <el-select v-model="form.versionId" clearable filterable style="width: 100%">
              <el-option v-for="item in formVersionOptions" :key="item.versionId" :label="`${item.versionNo} / ${item.versionStatus}`" :value="item.versionId" />
            </el-select>
          </el-form-item>
          <el-form-item label="任务类型" required>
            <el-radio-group v-model="form.taskType">
              <el-radio-button v-for="item in taskTypeOptions" :key="item.value" :label="item.value">{{ item.label }}</el-radio-button>
            </el-radio-group>
          </el-form-item>
          <el-form-item label="账期" required>
            <el-input v-model="form.billMonth" placeholder="yyyy-MM" />
          </el-form-item>
          <el-form-item label="请求号">
            <el-input v-model="form.requestNo" placeholder="可选，用于幂等提交" />
          </el-form-item>
          <el-form-item label="备注">
            <el-input v-model="form.remark" type="textarea" :rows="2" maxlength="500" show-word-limit />
          </el-form-item>
          <el-form-item label="输入 JSON" required>
            <el-input v-model="form.inputJson" type="textarea" :rows="14" maxlength="20000" show-word-limit />
          </el-form-item>
        </el-form>

        <div class="run-page__action-row">
          <el-button type="primary" icon="Promotion" @click="handleSubmit" v-hasPermi="['cost:task:execute']">提交任务</el-button>
          <el-button icon="RefreshLeft" @click="fillExample">按配置生成模板</el-button>
        </div>

        <el-alert v-if="templateMessage" :title="templateMessage" type="info" :closable="false" class="run-page__template-alert" />
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
            <h3>任务台账</h3>
            <p>跟踪任务进度、失败明细和重试状态。</p>
          </div>
        </div>

        <el-table v-loading="loading" :data="taskList">
          <el-table-column label="任务编号" prop="taskNo" width="220" />
          <el-table-column label="场景" min-width="180">
            <template #default="scope">{{ scope.row.sceneName }} ({{ scope.row.sceneCode }})</template>
          </el-table-column>
          <el-table-column label="版本" prop="versionNo" width="160" />
          <el-table-column label="任务类型" width="140" align="center">
            <template #default="scope">
              <dict-tag :options="taskTypeOptions" :value="scope.row.taskType" />
            </template>
          </el-table-column>
          <el-table-column label="账期" prop="billMonth" width="110" align="center" />
          <el-table-column label="状态" width="120" align="center">
            <template #default="scope">
              <dict-tag :options="taskStatusOptions" :value="scope.row.taskStatus" />
            </template>
          </el-table-column>
          <el-table-column label="进度" width="120" align="center">
            <template #default="scope">{{ scope.row.progressPercent }}%</template>
          </el-table-column>
          <el-table-column label="成功/失败" width="120" align="center">
            <template #default="scope">{{ scope.row.successCount }}/{{ scope.row.failCount }}</template>
          </el-table-column>
          <el-table-column label="操作" width="170" fixed="right" align="center">
            <template #default="scope">
              <el-button link type="primary" icon="View" @click="handleDetail(scope.row)">详情</el-button>
              <el-button link type="warning" icon="CircleClose" @click="handleCancel(scope.row)" v-hasPermi="['cost:task:cancel']">取消</el-button>
            </template>
          </el-table-column>
        </el-table>

        <pagination v-show="total > 0" :total="total" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" @pagination="getList" />
      </div>
    </section>

    <el-drawer v-model="detailOpen" title="任务详情" size="1080px" append-to-body>
      <el-descriptions v-if="detailData.task" :column="2" border>
        <el-descriptions-item label="任务编号">{{ detailData.task.taskNo }}</el-descriptions-item>
        <el-descriptions-item label="状态">{{ resolveTaskStatus(detailData.task.taskStatus) }}</el-descriptions-item>
        <el-descriptions-item label="场景">{{ detailData.task.sceneName }}</el-descriptions-item>
        <el-descriptions-item label="版本">{{ detailData.task.versionNo }}</el-descriptions-item>
        <el-descriptions-item label="账期">{{ detailData.task.billMonth }}</el-descriptions-item>
        <el-descriptions-item label="执行节点">{{ detailData.task.executeNode || '-' }}</el-descriptions-item>
      </el-descriptions>

      <div class="run-page__summary">
        <div class="run-page__summary-card"><span>输入总量</span><strong>{{ detailData.summary?.sourceCount || 0 }}</strong></div>
        <div class="run-page__summary-card"><span>成功数量</span><strong>{{ detailData.summary?.successCount || 0 }}</strong></div>
        <div class="run-page__summary-card"><span>失败数量</span><strong>{{ detailData.summary?.failCount || 0 }}</strong></div>
        <div class="run-page__summary-card"><span>可重试明细</span><strong>{{ detailData.summary?.retryableCount || 0 }}</strong></div>
      </div>

      <el-table :data="detailData.details || []" size="small">
        <el-table-column label="分片号" prop="partitionNo" width="90" align="center" />
        <el-table-column label="业务单号" prop="bizNo" min-width="180" />
        <el-table-column label="状态" width="120" align="center">
          <template #default="scope">
            <el-tag :type="resolveDetailTag(scope.row.detailStatus)">{{ scope.row.detailStatus }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="重试次数" prop="retryCount" width="100" align="center" />
        <el-table-column label="结果摘要" prop="resultSummary" min-width="240" />
        <el-table-column label="异常信息" prop="errorMessage" min-width="200" />
        <el-table-column label="操作" width="130" fixed="right" align="center">
          <template #default="scope">
            <el-button link type="primary" icon="RefreshRight" :disabled="scope.row.detailStatus !== 'FAILED'" @click="handleRetry(scope.row)" v-hasPermi="['cost:task:retry']">重试</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-drawer>
  </div>
</template>

<script setup name="CostTask">
import { ElMessageBox } from 'element-plus'
import { cancelTask, getRunInputTemplate, getTaskDetail, getTaskStats, listTask, listVersionOptions, retryTaskDetail, submitTask } from '@/api/cost/run'
import { optionselectScene } from '@/api/cost/scene'
import { getCostSceneContextId, resolvePreferredCostSceneId, setCostSceneContextId } from '@/utils/costSceneContext'
import { getRemoteDictOptionMap } from '@/utils/dictRemote'

const route = useRoute()
const { proxy } = getCurrentInstance()

const loading = ref(false)
const showSearch = ref(true)
const total = ref(0)
const taskList = ref([])
const sceneOptions = ref([])
const versionOptions = ref([])
const formVersionOptions = ref([])
const taskTypeOptions = ref([])
const taskStatusOptions = ref([])
const templateFields = ref([])
const templateMessage = ref('')
const detailOpen = ref(false)
const detailData = ref({})
const stats = reactive({ taskCount: 0, runningCount: 0, successCount: 0, failedCount: 0 })
let detailTimer = null
const activeTaskStatuses = ['INIT', 'RUNNING']

const queryParams = reactive({
  pageNum: 1,
  pageSize: 10,
  sceneId: route.query.sceneId ? Number(route.query.sceneId) : undefined,
  versionId: undefined,
  taskType: undefined,
  taskStatus: undefined,
  billMonth: ''
})

const form = reactive({
  sceneId: route.query.sceneId ? Number(route.query.sceneId) : undefined,
  versionId: undefined,
  taskType: 'FORMAL_SINGLE',
  billMonth: resolveCurrentBillMonth(),
  requestNo: '',
  inputJson: '',
  remark: ''
})

const metricItems = computed(() => [
  { label: '任务总数', value: stats.taskCount, desc: '当前筛选范围内的正式核算任务总数' },
  { label: '执行中', value: stats.runningCount, desc: '仍在异步执行中的任务数' },
  { label: '成功任务', value: stats.successCount, desc: '全部执行成功的任务数' },
  { label: '失败/部分成功', value: stats.failedCount, desc: '需要关注或重试的任务数' }
])

async function loadBaseOptions() {
  const [dictMap, sceneResp] = await Promise.all([
    getRemoteDictOptionMap(['cost_calc_task_type', 'cost_calc_task_status']),
    optionselectScene({ status: '0', pageNum: 1, pageSize: 1000 })
  ])
  taskTypeOptions.value = dictMap.cost_calc_task_type || []
  taskStatusOptions.value = dictMap.cost_calc_task_status || []
  sceneOptions.value = sceneResp?.data || []
  const preferredSceneId = resolvePreferredCostSceneId(
    sceneOptions.value,
    form.sceneId,
    queryParams.sceneId,
    route.query.sceneId,
    getCostSceneContextId()
  )
  if (preferredSceneId) {
    queryParams.sceneId = preferredSceneId
    form.sceneId = preferredSceneId
    setCostSceneContextId(preferredSceneId)
  }
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
      listTask(queryParams),
      getTaskStats(queryParams)
    ])
    taskList.value = listResp.rows || []
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
  versionOptions.value = []
  getList()
}

async function handleQuerySceneChange(sceneId) {
  queryParams.versionId = undefined
  queryParams.sceneId = sceneId
  form.sceneId = sceneId
  setCostSceneContextId(sceneId)
  await loadVersionOptions(sceneId, versionOptions)
  await loadVersionOptions(sceneId, formVersionOptions)
  await fillExample()
}

async function handleFormSceneChange(sceneId) {
  form.versionId = undefined
  queryParams.sceneId = sceneId
  setCostSceneContextId(sceneId)
  await loadVersionOptions(sceneId, formVersionOptions)
  await loadVersionOptions(sceneId, versionOptions)
  await fillExample()
}

async function fillExample() {
  if (!form.sceneId) {
    templateFields.value = []
    templateMessage.value = '请先选择运行场景，再按发布快照生成输入模板。'
    return
  }
  const response = await getRunInputTemplate({
    sceneId: form.sceneId,
    versionId: form.versionId,
    taskType: form.taskType
  })
  form.inputJson = response?.data?.inputJson || '{}'
  templateFields.value = response?.data?.fields?.filter(item => item.includedInTemplate) || []
  templateMessage.value = response?.data?.message || ''
}

async function handleSubmit() {
  if (!form.sceneId || !form.billMonth || !form.inputJson) {
    proxy.$modal.msgWarning('请填写运行场景、账期和输入 JSON')
    return
  }
  const resp = await submitTask({ ...form })
  proxy.$modal.msgSuccess('任务已提交')
  detailData.value = resp.data || {}
  detailOpen.value = true
  getList()
}

async function handleDetail(row) {
  const resp = await getTaskDetail(row.taskId)
  detailData.value = resp.data || {}
  detailOpen.value = true
}

async function handleRetry(row) {
  await retryTaskDetail(row.detailId)
  proxy.$modal.msgSuccess('已发起重试')
  if (detailData.value.task?.taskId) {
    const resp = await getTaskDetail(detailData.value.task.taskId)
    detailData.value = resp.data || {}
  }
  getList()
}

async function handleCancel(row) {
  await ElMessageBox.confirm(`确认取消任务 ${row.taskNo} 吗？`, '取消任务', { type: 'warning' })
  await cancelTask(row.taskId)
  proxy.$modal.msgSuccess('任务取消请求已提交')
  getList()
}

function resolveTaskStatus(value) {
  return taskStatusOptions.value.find(item => item.value === value)?.label || value
}

function resolveDetailTag(value) {
  if (value === 'SUCCESS') return 'success'
  if (value === 'FAILED') return 'danger'
  return 'info'
}

function resolveCurrentBillMonth() {
  const current = new Date()
  const month = String(current.getMonth() + 1).padStart(2, '0')
  return `${current.getFullYear()}-${month}`
}

watch(() => form.taskType, () => fillExample(), { immediate: true })

onMounted(async () => {
  await getList()
  await fillExample()
})
</script>

<style scoped lang="scss">
.run-page { display: grid; gap: 16px; }
.run-page__hero, .run-page__metric-card, .run-page__panel, .run-page__summary-card { border: 1px solid var(--el-border-color); border-radius: 16px; background: var(--el-bg-color-overlay); }
.run-page__hero { display: flex; justify-content: space-between; gap: 16px; padding: 22px 24px; background: color-mix(in srgb, var(--el-color-warning-light-8) 28%, var(--el-bg-color-overlay)); }
.run-page__eyebrow { font-size: 12px; color: var(--el-color-warning-dark-2); font-weight: 700; letter-spacing: .08em; text-transform: uppercase; }
.run-page__title { margin: 8px 0 0; font-size: 28px; }
.run-page__subtitle { margin: 10px 0 0; color: var(--el-text-color-regular); line-height: 1.8; }
.run-page__metrics { display: grid; grid-template-columns: repeat(4, minmax(0, 1fr)); gap: 14px; }
.run-page__metric-card { display: grid; gap: 6px; padding: 14px 16px; }
.run-page__metric-card strong { font-size: 26px; color: var(--el-color-warning-dark-2); }
.run-page__workspace { display: grid; grid-template-columns: 440px minmax(0, 1fr); gap: 16px; }
.run-page__panel { padding: 16px; }
.run-page__section-head { display: flex; justify-content: space-between; align-items: center; gap: 12px; margin-bottom: 16px; }
.run-page__section-head h3 { margin: 0; font-size: 18px; }
.run-page__section-head p { margin: 6px 0 0; color: var(--el-text-color-secondary); font-size: 13px; }
.run-page__action-row { display: flex; gap: 10px; margin-top: 8px; }
.run-page__template-alert { margin-top: 14px; }
.run-page__template-table { margin-top: 12px; }
.run-page__summary { display: grid; grid-template-columns: repeat(4, minmax(0, 1fr)); gap: 12px; margin: 16px 0; }
.run-page__summary-card { display: grid; gap: 6px; padding: 12px 14px; }
.run-page__summary-card strong { font-size: 24px; color: var(--el-color-warning-dark-2); }
@media (max-width: 1200px) {
  .run-page__metrics, .run-page__workspace, .run-page__summary { grid-template-columns: 1fr; }
}
</style>

<template>
  <div class="app-container governance-page">
    <section class="governance-page__hero">
      <div>
        <div class="governance-page__eyebrow">账期治理</div>
        <h2 class="governance-page__title">账期治理</h2>
        <p class="governance-page__subtitle">
          统一管理账期台账、封存控制和重算流程，保障历史账期可控、可查并满足持续运营需要。
        </p>
      </div>
      <el-tag type="warning">支持账期封存、重算申请、审批和执行跟踪</el-tag>
    </section>

    <section class="governance-page__metrics">
      <div v-for="item in metricItems" :key="item.label" class="governance-page__metric-card">
        <span>{{ item.label }}</span>
        <strong>{{ item.value }}</strong>
        <small>{{ item.desc }}</small>
      </div>
    </section>

    <el-form ref="queryRef" :model="queryParams" :inline="true" label-width="84px" v-show="showSearch">
      <el-form-item label="所属场景" prop="sceneId">
        <el-select v-model="queryParams.sceneId" clearable filterable style="width: 240px">
          <el-option v-for="item in sceneOptions" :key="item.sceneId" :label="`${item.sceneName} / ${item.sceneCode}`" :value="item.sceneId" />
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
      <el-form-item label="账期状态" prop="periodStatus">
        <el-select v-model="queryParams.periodStatus" clearable style="width: 180px">
          <el-option v-for="item in periodStatusOptions" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <section class="governance-page__workspace">
      <div class="governance-page__panel governance-page__panel--side">
        <div class="governance-page__section-head">
          <div>
            <h3>新建账期</h3>
            <p>账期治理以场景+账期为唯一口径，重算只能在账期范围内发起。</p>
          </div>
          <right-toolbar v-model:showSearch="showSearch" @queryTable="getList" />
        </div>

        <el-form :model="periodForm" label-width="92px">
          <el-form-item label="所属场景" required>
            <el-select v-model="periodForm.sceneId" filterable style="width: 100%" @change="handlePeriodSceneChange">
              <el-option v-for="item in sceneOptions" :key="item.sceneId" :label="`${item.sceneName} / ${item.sceneCode}`" :value="item.sceneId" />
            </el-select>
          </el-form-item>
          <el-form-item label="账期" required>
            <el-date-picker
              v-model="periodForm.billMonth"
              type="month"
              format="YYYY-MM"
              value-format="YYYY-MM"
              placeholder="选择账期"
              style="width: 100%"
            />
          </el-form-item>
          <el-form-item label="默认版本">
            <el-select v-model="periodForm.activeVersionId" clearable filterable style="width: 100%">
              <el-option v-for="item in versionOptions" :key="item.versionId" :label="`${item.versionNo} / ${item.versionStatus}`" :value="item.versionId" />
            </el-select>
          </el-form-item>
          <el-form-item label="备注">
            <el-input v-model="periodForm.remark" type="textarea" :rows="3" maxlength="500" show-word-limit />
          </el-form-item>
        </el-form>

        <div class="governance-page__action-row">
          <el-button type="primary" icon="Plus" @click="handleCreatePeriod" v-hasPermi="['cost:period:add']">新建账期</el-button>
          <el-button icon="RefreshLeft" @click="fillCurrentMonth">带入当前账期</el-button>
        </div>

        <div class="governance-page__section-head governance-page__section-head--sub">
          <div>
            <h3>发起重算</h3>
            <p>先选场景和账期，再选目标版本与基准任务，审核通过后执行。</p>
          </div>
        </div>

        <el-form :model="recalcForm" label-width="92px">
          <el-form-item label="所属场景" required>
            <el-select v-model="recalcForm.sceneId" filterable style="width: 100%" @change="handleRecalcSceneChange">
              <el-option v-for="item in sceneOptions" :key="item.sceneId" :label="`${item.sceneName} / ${item.sceneCode}`" :value="item.sceneId" />
            </el-select>
          </el-form-item>
          <el-form-item label="账期" required>
            <el-date-picker
              v-model="recalcForm.billMonth"
              type="month"
              format="YYYY-MM"
              value-format="YYYY-MM"
              placeholder="选择账期"
              style="width: 100%"
            />
          </el-form-item>
          <el-form-item label="目标版本" required>
            <el-select v-model="recalcForm.versionId" filterable style="width: 100%">
              <el-option v-for="item in recalcVersionOptions" :key="item.versionId" :label="item.versionNo" :value="item.versionId" />
            </el-select>
          </el-form-item>
          <el-form-item label="基准任务" required>
            <el-select v-model="recalcForm.baselineTaskId" filterable clearable style="width: 100%">
              <el-option v-for="item in baselineTaskOptions" :key="item.taskId" :label="`${item.taskNo} / ${item.billMonth}`" :value="item.taskId" />
            </el-select>
          </el-form-item>
          <el-form-item label="申请原因" required>
            <el-input v-model="recalcForm.applyReason" type="textarea" :rows="3" maxlength="500" show-word-limit />
          </el-form-item>
          <el-form-item label="请求号">
            <el-input v-model="recalcForm.requestNo" maxlength="64" />
          </el-form-item>
          <el-form-item label="备注">
            <el-input v-model="recalcForm.remark" type="textarea" :rows="2" maxlength="500" show-word-limit />
          </el-form-item>
        </el-form>

        <div class="governance-page__action-row">
          <el-button type="success" icon="Promotion" @click="handleApplyRecalc" v-hasPermi="['cost:period:recalc']">发起重算</el-button>
        </div>
      </div>

      <div class="governance-page__panel governance-page__panel--main">
        <div class="governance-page__section-head">
          <div>
            <h3>账期台账</h3>
            <p>从账期视角追踪当前默认版本、最近任务结果和重算申请状态。</p>
          </div>
          <el-button type="warning" plain icon="Download" @click="handleExportPeriod">
            导出账期
          </el-button>
        </div>

        <el-table v-loading="loading" :data="periodList">
          <el-table-column label="所属场景" min-width="180">
            <template #default="scope">{{ scope.row.sceneName }} ({{ scope.row.sceneCode }})</template>
          </el-table-column>
          <el-table-column label="账期" prop="billMonth" width="110" align="center" />
          <el-table-column label="状态" width="120" align="center">
            <template #default="scope">
              <dict-tag :options="periodStatusOptions" :value="scope.row.periodStatus" />
            </template>
          </el-table-column>
          <el-table-column label="默认版本" prop="versionNo" width="150" align="center" />
          <el-table-column label="结果条数" prop="resultCount" width="110" align="center" />
          <el-table-column label="金额汇总" width="150" align="right">
            <template #default="scope">{{ formatAmount(scope.row.amountTotal) }}</template>
          </el-table-column>
          <el-table-column label="最近任务" prop="lastTaskNo" width="210" />
          <el-table-column label="封存时间" width="180" align="center">
            <template #default="scope">{{ proxy.parseTime(scope.row.sealedTime) || '-' }}</template>
          </el-table-column>
          <el-table-column label="操作" width="280" fixed="right" align="center">
            <template #default="scope">
              <el-button link type="primary" icon="View" @click="handlePeriodDetail(scope.row)">详情</el-button>
              <el-button link type="warning" icon="Lock" :disabled="scope.row.periodStatus === 'SEALED'" @click="handleSeal(scope.row)" v-hasPermi="['cost:period:seal']">封存</el-button>
              <el-button link type="success" icon="Select" @click="handleOpenApprove(scope.row)" v-hasPermi="['cost:period:approve']">审核重算</el-button>
            </template>
          </el-table-column>
        </el-table>

        <pagination v-show="total > 0" :total="total" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" @pagination="getList" />

        <div class="governance-page__section-head governance-page__section-head--sub">
          <div>
            <h3>重算申请台账</h3>
            <p>支持审核、执行并查看重算前后差异摘要。</p>
          </div>
          <el-button type="warning" plain icon="Download" @click="handleExportRecalc">
            导出差异
          </el-button>
        </div>

        <el-table :data="recalcList">
          <el-table-column label="场景" min-width="180">
            <template #default="scope">{{ scope.row.sceneName }} ({{ scope.row.sceneCode }})</template>
          </el-table-column>
          <el-table-column label="账期" prop="billMonth" width="110" align="center" />
          <el-table-column label="目标版本" prop="versionNo" width="150" align="center" />
          <el-table-column label="状态" width="130" align="center">
            <template #default="scope">
              <dict-tag :options="recalcStatusOptions" :value="scope.row.recalcStatus" />
            </template>
          </el-table-column>
          <el-table-column label="基准任务" prop="baselineTaskNo" width="200" />
          <el-table-column label="重算任务" prop="targetTaskNo" width="200" />
          <el-table-column label="差异金额" width="140" align="right">
            <template #default="scope">{{ formatAmount(scope.row.diffAmount) }}</template>
          </el-table-column>
          <el-table-column label="操作" width="260" fixed="right" align="center">
            <template #default="scope">
              <el-button link type="primary" icon="View" @click="handleRecalcDetail(scope.row)">详情</el-button>
              <el-button link type="primary" icon="Finished" :disabled="scope.row.recalcStatus !== 'PENDING_APPROVAL'" @click="handleApprove(scope.row, true)" v-hasPermi="['cost:period:approve']">通过</el-button>
              <el-button link type="danger" icon="CloseBold" :disabled="scope.row.recalcStatus !== 'PENDING_APPROVAL'" @click="handleApprove(scope.row, false)" v-hasPermi="['cost:period:approve']">驳回</el-button>
              <el-button link type="success" icon="VideoPlay" :disabled="scope.row.recalcStatus !== 'APPROVED'" @click="handleExecuteRecalc(scope.row)" v-hasPermi="['cost:period:execute']">执行</el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>
    </section>

    <el-drawer v-model="detailOpen" title="账期详情" size="920px" append-to-body>
      <el-descriptions v-if="detailData.period" :column="2" border>
        <el-descriptions-item label="所属场景">{{ detailData.period.sceneName }}</el-descriptions-item>
        <el-descriptions-item label="账期状态">{{ resolveDictLabel(periodStatusOptions, detailData.period.periodStatus) }}</el-descriptions-item>
        <el-descriptions-item label="账期">{{ detailData.period.billMonth }}</el-descriptions-item>
        <el-descriptions-item label="默认版本">{{ detailData.period.versionNo || '-' }}</el-descriptions-item>
        <el-descriptions-item label="最近任务">{{ detailData.period.lastTaskNo || '-' }}</el-descriptions-item>
        <el-descriptions-item label="金额汇总">{{ formatAmount(detailData.period.amountTotal) }}</el-descriptions-item>
      </el-descriptions>

      <div class="governance-page__summary">
        <div class="governance-page__summary-card"><span>结果条数</span><strong>{{ detailData.resultStats?.resultCount || 0 }}</strong></div>
        <div class="governance-page__summary-card"><span>金额汇总</span><strong>{{ formatAmount(detailData.resultStats?.amountTotal) }}</strong></div>
        <div class="governance-page__summary-card"><span>费用数</span><strong>{{ detailData.resultStats?.feeCount || 0 }}</strong></div>
      </div>

      <el-table :data="detailData.recalcOrders || []" size="small">
        <el-table-column label="重算状态" width="140" align="center">
          <template #default="scope">
            <dict-tag :options="recalcStatusOptions" :value="scope.row.recalcStatus" />
          </template>
        </el-table-column>
        <el-table-column label="基准任务" prop="baselineTaskNo" width="190" />
        <el-table-column label="目标任务" prop="targetTaskNo" width="190" />
        <el-table-column label="差异金额" width="140" align="right">
          <template #default="scope">{{ formatAmount(scope.row.diffAmount) }}</template>
        </el-table-column>
        <el-table-column label="申请原因" prop="applyReason" min-width="220" />
      </el-table>
    </el-drawer>

    <el-drawer v-model="recalcDetailOpen" title="重算详情" size="860px" append-to-body>
      <el-descriptions v-if="recalcDetail.order" :column="2" border>
        <el-descriptions-item label="所属场景">{{ recalcDetail.order.sceneName }}</el-descriptions-item>
        <el-descriptions-item label="状态">{{ resolveDictLabel(recalcStatusOptions, recalcDetail.order.recalcStatus) }}</el-descriptions-item>
        <el-descriptions-item label="账期">{{ recalcDetail.order.billMonth }}</el-descriptions-item>
        <el-descriptions-item label="目标版本">{{ recalcDetail.order.versionNo }}</el-descriptions-item>
        <el-descriptions-item label="基准任务">{{ recalcDetail.order.baselineTaskNo || '-' }}</el-descriptions-item>
        <el-descriptions-item label="目标任务">{{ recalcDetail.order.targetTaskNo || '-' }}</el-descriptions-item>
      </el-descriptions>
      <JsonEditor :model-value="recalcDetail.diffSummary || {}" title="差异摘要" readonly :rows="12" />
    </el-drawer>
  </div>
</template>

<script setup name="CostPeriod">
import { ElMessageBox } from 'element-plus'
import JsonEditor from '@/components/cost/JsonEditor.vue'
import { optionselectScene } from '@/api/cost/scene'
import { listTask, listVersionOptions } from '@/api/cost/run'
import {
  applyRecalc,
  approveRecalc,
  createPeriod,
  executeRecalc,
  getPeriodDetail,
  getPeriodStats,
  getRecalcDetail,
  listPeriod,
  listRecalc,
  sealPeriod
} from '@/api/cost/governance'
import { getRemoteDictOptionMap } from '@/utils/dictRemote'
import { resolveWorkingCostSceneId } from '@/utils/costSceneContext'
import { clearCostWorkContext, resolveWorkingBillMonth, resolveWorkingVersionId, syncCostWorkContext } from '@/utils/costWorkContext'

const { proxy } = getCurrentInstance()

const loading = ref(false)
const showSearch = ref(true)
const total = ref(0)
const periodList = ref([])
const recalcList = ref([])
const sceneOptions = ref([])
const versionOptions = ref([])
const recalcVersionOptions = ref([])
const baselineTaskOptions = ref([])
const periodStatusOptions = ref([])
const recalcStatusOptions = ref([])
const detailOpen = ref(false)
const detailData = ref({})
const recalcDetailOpen = ref(false)
const recalcDetail = ref({})
const stats = reactive({ periodCount: 0, sealedCount: 0, runningCount: 0, recalcCount: 0 })

const queryParams = reactive({
  pageNum: 1,
  pageSize: 10,
  sceneId: undefined,
  billMonth: resolveWorkingBillMonth(),
  periodStatus: undefined
})

const periodForm = reactive({
  sceneId: undefined,
  billMonth: resolveWorkingBillMonth(),
  activeVersionId: resolveWorkingVersionId(),
  remark: ''
})

const recalcForm = reactive({
  sceneId: undefined,
  billMonth: resolveWorkingBillMonth(),
  versionId: resolveWorkingVersionId(),
  baselineTaskId: undefined,
  applyReason: '',
  requestNo: '',
  remark: ''
})

const metricItems = computed(() => [
  { label: '账期总数', value: stats.periodCount, desc: '当前筛选范围内已纳入治理的账期数量' },
  { label: '已封存账期', value: stats.sealedCount, desc: '已禁止直接再次提交正式核算的账期数量' },
  { label: '进行中账期', value: stats.runningCount, desc: '仍有任务执行中的账期数量' },
  { label: '重算申请数', value: stats.recalcCount, desc: '账期下已累计发起的重算申请数量' }
])

async function loadBaseOptions() {
  const [dictMap, sceneResp] = await Promise.all([
    getRemoteDictOptionMap(['cost_bill_period_status', 'cost_recalc_status']),
    optionselectScene({ status: '0', pageNum: 1, pageSize: 1000 })
  ])
  periodStatusOptions.value = dictMap.cost_bill_period_status || []
  recalcStatusOptions.value = dictMap.cost_recalc_status || []
  sceneOptions.value = sceneResp?.data || []
  queryParams.sceneId = resolveWorkingCostSceneId(sceneOptions.value, queryParams.sceneId)
  periodForm.sceneId = resolveWorkingCostSceneId(sceneOptions.value, periodForm.sceneId, queryParams.sceneId)
  recalcForm.sceneId = resolveWorkingCostSceneId(sceneOptions.value, recalcForm.sceneId, queryParams.sceneId)
}

async function loadVersions(sceneId, target) {
  if (!sceneId) {
    target.value = []
    return
  }
  const resp = await listVersionOptions(sceneId)
  target.value = resp.data || []
}

async function loadBaselineTasks(sceneId, billMonth) {
  if (!sceneId || !billMonth) {
    baselineTaskOptions.value = []
    return
  }
  const resp = await listTask({ pageNum: 1, pageSize: 1000, sceneId, billMonth })
  baselineTaskOptions.value = (resp.rows || []).filter(item => ['SUCCESS', 'PART_SUCCESS', 'FAILED'].includes(item.taskStatus))
}

async function getList() {
  loading.value = true
  try {
    await loadBaseOptions()
    if (periodForm.sceneId) {
      await loadVersions(periodForm.sceneId, versionOptions)
    }
    if (recalcForm.sceneId) {
      await Promise.all([
        loadVersions(recalcForm.sceneId, recalcVersionOptions),
        loadBaselineTasks(recalcForm.sceneId, recalcForm.billMonth)
      ])
    }
    const [periodResp, periodStatsResp, recalcResp] = await Promise.all([
      listPeriod(queryParams),
      getPeriodStats(queryParams.sceneId),
      listRecalc({ pageNum: 1, pageSize: 1000, sceneId: queryParams.sceneId, billMonth: queryParams.billMonth })
    ])
    periodList.value = periodResp.rows || []
    total.value = periodResp.total || 0
    recalcList.value = recalcResp.rows || []
    Object.assign(stats, periodStatsResp.data || {})
  } finally {
    loading.value = false
  }
}

function handleQuery() {
  queryParams.pageNum = 1
  getList()
}

function handleExportPeriod() {
  proxy.download('cost/governance/period/export', {
    ...queryParams
  }, `cost_period_${Date.now()}.xlsx`)
}

function handleExportRecalc() {
  proxy.download('cost/governance/recalc/export', {
    sceneId: queryParams.sceneId,
    billMonth: queryParams.billMonth
  }, `cost_recalc_${Date.now()}.xlsx`)
}

function resetQuery() {
  proxy.resetForm('queryRef')
  queryParams.pageNum = 1
  queryParams.pageSize = 10
  queryParams.billMonth = resolveWorkingBillMonth(periodForm.billMonth, recalcForm.billMonth)
  getList()
}

async function handlePeriodSceneChange(sceneId) {
  const targetSceneId = sceneId
  periodForm.sceneId = targetSceneId
  periodForm.activeVersionId = undefined
  clearCostWorkContext(['versionId'])
  syncCostWorkContext({ sceneId: targetSceneId, billMonth: periodForm.billMonth })
  await loadVersions(targetSceneId, versionOptions)
}

async function handleRecalcSceneChange(sceneId) {
  const targetSceneId = sceneId
  recalcForm.sceneId = targetSceneId
  recalcForm.versionId = undefined
  recalcForm.baselineTaskId = undefined
  clearCostWorkContext(['versionId'])
  syncCostWorkContext({ sceneId: targetSceneId, billMonth: recalcForm.billMonth })
  await Promise.all([
    loadVersions(targetSceneId, recalcVersionOptions),
    loadBaselineTasks(targetSceneId, recalcForm.billMonth)
  ])
}

function fillCurrentMonth() {
  const workingBillMonth = resolveWorkingBillMonth()
  periodForm.billMonth = workingBillMonth
  recalcForm.billMonth = workingBillMonth
}

async function handleCreatePeriod() {
  if (!periodForm.sceneId || !periodForm.billMonth) {
    proxy.$modal.msgWarning('请先选择所属场景并填写账期')
    return
  }
  await createPeriod({ ...periodForm })
  proxy.$modal.msgSuccess('账期创建成功')
  getList()
}

async function handleApplyRecalc() {
  if (!recalcForm.sceneId || !recalcForm.billMonth || !recalcForm.versionId || !recalcForm.baselineTaskId || !recalcForm.applyReason) {
    proxy.$modal.msgWarning('请先补齐重算申请必填项')
    return
  }
  await applyRecalc({ ...recalcForm })
  proxy.$modal.msgSuccess('重算申请已提交')
  getList()
}

async function handleSeal(row) {
  await ElMessageBox.confirm(`确认封存账期 ${row.billMonth} 吗？封存后将阻止直接再次提交正式核算。`, '提示', { type: 'warning' })
  await sealPeriod(row.periodId)
  proxy.$modal.msgSuccess('账期已封存')
  getList()
}

async function handlePeriodDetail(row) {
  const resp = await getPeriodDetail(row.periodId)
  detailData.value = resp.data || {}
  detailOpen.value = true
}

function handleOpenApprove(row) {
  const target = recalcList.value.find(item => item.sceneId === row.sceneId && item.billMonth === row.billMonth && item.recalcStatus === 'PENDING_APPROVAL')
  if (!target) {
    proxy.$modal.msgWarning('该账期暂无待审核的重算申请')
    return
  }
  handleRecalcDetail(target)
}

async function handleApprove(row, approved) {
  const actionText = approved ? '通过' : '驳回'
  const { value } = await ElMessageBox.prompt(`请输入${actionText}意见`, `${actionText}重算`, {
    inputValue: approved ? '同意按目标版本重算' : '请补充重算范围后再提交',
    confirmButtonText: '确定',
    cancelButtonText: '取消'
  })
  await approveRecalc(row.recalcId, { approved, approveOpinion: value })
  proxy.$modal.msgSuccess(`已${actionText}重算申请`)
  getList()
}

async function handleExecuteRecalc(row) {
  await ElMessageBox.confirm(`确认执行重算申请 #${row.recalcId} 吗？`, '提示', { type: 'warning' })
  await executeRecalc(row.recalcId)
  proxy.$modal.msgSuccess('重算任务已发起')
  getList()
}

async function handleRecalcDetail(row) {
  const resp = await getRecalcDetail(row.recalcId)
  recalcDetail.value = resp.data || {}
  recalcDetailOpen.value = true
}

function resolveDictLabel(options, value) {
  return options.find(item => item.value === value)?.label || value || '-'
}

function formatAmount(value) {
  if (value === undefined || value === null || value === '') {
    return '0.00'
  }
  return Number(value).toFixed(2)
}

watch(
  () => [periodForm.sceneId, periodForm.activeVersionId, periodForm.billMonth],
  ([sceneId, versionId, billMonth]) => {
    syncCostWorkContext({ sceneId, versionId, billMonth })
  },
  { immediate: true }
)

watch(
  () => [recalcForm.sceneId, recalcForm.versionId, recalcForm.billMonth],
  ([sceneId, versionId, billMonth]) => {
    syncCostWorkContext({ sceneId, versionId, billMonth })
  },
  { immediate: true }
)

onMounted(() => {
  fillCurrentMonth()
  getList()
})

onActivated(() => {
  fillCurrentMonth()
  getList()
})
</script>

<style lang="scss" scoped>
.governance-page {
  display: flex;
  flex-direction: column;
  gap: 20px;

  &__hero,
  &__panel,
  &__metric-card {
    background: #fff;
    border-radius: 20px;
    border: 1px solid #e7edf7;
    box-shadow: 0 14px 35px rgba(15, 34, 58, 0.06);
  }

  &__hero {
    padding: 28px 32px;
    display: flex;
    justify-content: space-between;
    gap: 24px;
    align-items: flex-start;
  }

  &__eyebrow {
    color: #c7872d;
    font-size: 13px;
    font-weight: 600;
    margin-bottom: 10px;
  }

  &__title {
    margin: 0;
    font-size: 24px;
    color: #10233e;
  }

  &__subtitle {
    margin: 12px 0 0;
    max-width: 880px;
    color: #5d6b82;
    line-height: 1.7;
  }

  &__metrics {
    display: grid;
    grid-template-columns: repeat(4, minmax(0, 1fr));
    gap: 16px;
  }

  &__metric-card {
    padding: 22px 24px;
    display: flex;
    flex-direction: column;
    gap: 10px;

    span {
      color: #7b889b;
      font-size: 14px;
    }

    strong {
      color: #3c8cff;
      font-size: 24px;
      line-height: 1;
    }

    small {
      color: #8d98aa;
      line-height: 1.6;
    }
  }

  &__workspace {
    display: grid;
    grid-template-columns: 360px minmax(0, 1fr);
    gap: 18px;
    align-items: start;
  }

  &__panel {
    padding: 22px 24px;
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
      line-height: 1.7;
    }
  }

  &__section-head--sub {
    margin-top: 28px;
  }

  &__action-row {
    display: flex;
    gap: 12px;
    margin-top: 18px;
  }

  &__summary {
    display: grid;
    grid-template-columns: repeat(3, minmax(0, 1fr));
    gap: 14px;
    margin: 20px 0;
  }

  &__summary-card {
    background: #f7fafc;
    border: 1px solid #e7edf7;
    border-radius: 18px;
    padding: 16px 18px;
    display: flex;
    flex-direction: column;
    gap: 8px;

    span {
      color: #7b889b;
      font-size: 14px;
    }

    strong {
      color: #c7872d;
      font-size: 24px;
      line-height: 1;
    }
  }

  &__json {
    margin: 20px 0 0;
    padding: 18px;
    background: #0f172a;
    color: #dbeafe;
    border-radius: 16px;
    max-height: 420px;
    overflow: auto;
    white-space: pre-wrap;
    word-break: break-all;
  }
}

@media (max-width: 1360px) {
  .governance-page {
    &__metrics,
    &__workspace,
    &__summary {
      grid-template-columns: 1fr;
    }
  }
}
</style>

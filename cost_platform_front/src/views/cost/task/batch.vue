<template>
  <div class="app-container batch-page">
    <section class="batch-page__hero">
      <div>
        <div class="batch-page__eyebrow">导入批次</div>
        <h2 class="batch-page__title">导入批次台账</h2>
        <p class="batch-page__subtitle">统一查看正式核算导入批次、预览样例明细，并从批次回到任务提交链路。</p>
      </div>
      <div class="batch-page__hero-actions">
        <el-button icon="Back" @click="router.push('/cost/task')">返回任务中心</el-button>
        <el-button type="primary" icon="Promotion" @click="router.push({ path: '/cost/task', query: { sceneId: queryParams.sceneId } })">去提交任务</el-button>
      </div>
    </section>

    <el-form ref="queryRef" :model="queryParams" :inline="true" label-width="84px" v-show="showSearch">
      <el-form-item label="所属场景" prop="sceneId">
        <el-select v-model="queryParams.sceneId" clearable filterable style="width: 220px">
          <el-option v-for="item in sceneOptions" :key="item.sceneId" :label="`${item.sceneCode} / ${item.sceneName}`" :value="item.sceneId" />
        </el-select>
      </el-form-item>
      <el-form-item label="账期" prop="billMonth">
        <el-input v-model="queryParams.billMonth" clearable placeholder="yyyy-MM" style="width: 160px" />
      </el-form-item>
      <el-form-item label="批次号" prop="batchNo">
        <el-input v-model="queryParams.batchNo" clearable placeholder="支持模糊查询" style="width: 220px" />
      </el-form-item>
      <el-form-item label="状态" prop="batchStatus">
        <el-select v-model="queryParams.batchStatus" clearable style="width: 180px">
          <el-option label="READY" value="READY" />
          <el-option label="SUBMITTED" value="SUBMITTED" />
          <el-option label="CONSUMED" value="CONSUMED" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <div class="batch-page__table">
      <div class="batch-page__section-head">
        <div>
          <h3>批次列表</h3>
          <p>列表用于挑选历史批次、观察状态变化，并查看样例输入内容。</p>
        </div>
        <right-toolbar v-model:showSearch="showSearch" @queryTable="getList" />
      </div>

      <el-table v-loading="loading" :data="batchList">
        <el-table-column label="批次号" prop="batchNo" width="220" />
        <el-table-column label="场景" min-width="180">
          <template #default="scope">{{ scope.row.sceneName }} ({{ scope.row.sceneCode }})</template>
        </el-table-column>
        <el-table-column label="版本" prop="versionNo" width="150" />
        <el-table-column label="账期" prop="billMonth" width="110" align="center" />
        <el-table-column label="状态" prop="batchStatus" width="120" align="center" />
        <el-table-column label="条数" prop="totalCount" width="90" align="center" />
        <el-table-column label="有效/错误" width="120" align="center">
          <template #default="scope">{{ scope.row.validCount || 0 }}/{{ scope.row.errorCount || 0 }}</template>
        </el-table-column>
        <el-table-column label="备注" prop="remark" min-width="220" />
        <el-table-column label="操作" width="200" fixed="right" align="center">
          <template #default="scope">
            <el-button link type="primary" icon="View" @click="handleDetail(scope.row)">详情</el-button>
            <el-button link type="success" icon="Promotion" @click="handleUseBatch(scope.row)">用于提交</el-button>
          </template>
        </el-table-column>
      </el-table>

      <pagination v-show="total > 0" :total="total" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" @pagination="getList" />
    </div>

    <el-drawer v-model="detailOpen" title="批次详情" size="980px" append-to-body>
      <el-descriptions v-if="detailData.batch" :column="2" border>
        <el-descriptions-item label="批次号">{{ detailData.batch.batchNo }}</el-descriptions-item>
        <el-descriptions-item label="状态">{{ detailData.batch.batchStatus }}</el-descriptions-item>
        <el-descriptions-item label="场景">{{ detailData.batch.sceneName }}</el-descriptions-item>
        <el-descriptions-item label="版本">{{ detailData.batch.versionNo || '-' }}</el-descriptions-item>
        <el-descriptions-item label="账期">{{ detailData.batch.billMonth }}</el-descriptions-item>
        <el-descriptions-item label="来源类型">{{ detailData.batch.sourceType }}</el-descriptions-item>
        <el-descriptions-item label="总量">{{ detailData.batch.totalCount || 0 }}</el-descriptions-item>
        <el-descriptions-item label="有效/错误">{{ detailData.batch.validCount || 0 }}/{{ detailData.batch.errorCount || 0 }}</el-descriptions-item>
      </el-descriptions>

      <div class="batch-page__section-head">
        <div>
          <h3>样例明细</h3>
          <p>展示前 10 条输入样例，便于快速确认批次内容。</p>
        </div>
      </div>
      <el-table :data="detailData.items || []" size="small" border>
        <el-table-column label="序号" prop="itemNo" width="80" align="center" />
        <el-table-column label="业务单号" prop="bizNo" min-width="180" />
        <el-table-column label="状态" prop="itemStatus" width="120" align="center" />
        <el-table-column label="输入摘要" min-width="420">
          <template #default="scope">{{ summarizeJson(scope.row.inputJson) }}</template>
        </el-table-column>
      </el-table>
    </el-drawer>
  </div>
</template>

<script setup name="CostTaskBatchLedger">
import { getTaskInputBatchDetail, listTaskInputBatch } from '@/api/cost/run'
import { optionselectScene } from '@/api/cost/scene'
import { resolveWorkingCostSceneId } from '@/utils/costSceneContext'

const router = useRouter()
const route = useRoute()
const { proxy } = getCurrentInstance()

const loading = ref(false)
const showSearch = ref(true)
const total = ref(0)
const batchList = ref([])
const sceneOptions = ref([])
const detailOpen = ref(false)
const detailData = ref({})

const queryParams = reactive({
  pageNum: 1,
  pageSize: 10,
  sceneId: route.query.sceneId ? Number(route.query.sceneId) : undefined,
  billMonth: '',
  batchNo: '',
  batchStatus: ''
})

async function loadBaseOptions() {
  const sceneResp = await optionselectScene({ status: '0', pageNum: 1, pageSize: 1000 })
  sceneOptions.value = sceneResp?.data || []
  if (!queryParams.sceneId) {
    queryParams.sceneId = resolveWorkingCostSceneId(sceneOptions.value)
  }
}

async function getList() {
  loading.value = true
  try {
    await loadBaseOptions()
    const resp = await listTaskInputBatch(queryParams)
    batchList.value = resp.rows || []
    total.value = resp.total || 0
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
  getList()
}

async function handleDetail(row) {
  const resp = await getTaskInputBatchDetail(row.batchId)
  detailData.value = resp.data || {}
  detailOpen.value = true
}

function handleUseBatch(row) {
  router.push({ path: '/cost/task', query: { sceneId: row.sceneId, sourceBatchNo: row.batchNo, inputSourceType: 'INPUT_BATCH' } })
}

function summarizeJson(value) {
  if (!value) return '-'
  return value.length > 180 ? `${value.slice(0, 180)}...` : value
}

onMounted(getList)
onActivated(getList)
</script>

<style scoped lang="scss">
.batch-page { display: grid; gap: 16px; }
.batch-page__hero, .batch-page__table {
  border: 1px solid var(--el-border-color);
  border-radius: 16px;
  background: var(--el-bg-color-overlay);
}
.batch-page__hero {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  padding: 22px 24px;
  background: color-mix(in srgb, var(--el-color-success-light-8) 22%, var(--el-bg-color-overlay));
}
.batch-page__eyebrow {
  font-size: 12px;
  color: var(--el-color-success-dark-2);
  font-weight: 700;
  letter-spacing: .08em;
  text-transform: uppercase;
}
.batch-page__title { margin: 8px 0 0; font-size: 28px; }
.batch-page__subtitle { margin: 10px 0 0; color: var(--el-text-color-regular); line-height: 1.8; }
.batch-page__hero-actions { display: flex; gap: 10px; align-items: flex-start; }
.batch-page__table { padding: 16px; }
.batch-page__section-head { display: flex; justify-content: space-between; align-items: center; gap: 12px; margin: 0 0 16px; }
.batch-page__section-head h3 { margin: 0; font-size: 18px; }
.batch-page__section-head p { margin: 6px 0 0; color: var(--el-text-color-secondary); font-size: 13px; }
@media (max-width: 1200px) {
  .batch-page__hero { flex-direction: column; }
  .batch-page__hero-actions { align-items: stretch; }
}
</style>

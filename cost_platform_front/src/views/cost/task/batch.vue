<template>
  <div class="app-container batch-page" :class="{ 'is-compact-mode': isCompactMode }">
    <section v-show="!isCompactMode" class="batch-page__hero">
      <div>
        <div class="batch-page__eyebrow">导入批次</div>
        <h2 class="batch-page__title">导入批次台账</h2>
        <p class="batch-page__subtitle">统一查看正式核算导入批次、预览样例明细，并从批次回到任务提交链路。</p>
      </div>
      <div class="batch-page__hero-actions">
        <el-button icon="Back" @click="router.push(COST_MENU_ROUTES.task)">返回任务中心</el-button>
        <el-button type="primary" icon="Promotion" @click="router.push({ path: COST_MENU_ROUTES.task, query: { sceneId: queryParams.sceneId } })">去提交任务</el-button>
      </div>
    </section>

    <section class="batch-page__query-shell">
      <div class="batch-page__section-head">
        <div>
          <h3>批次检索</h3>
          <p>按场景、账期、批次号和状态定位正式核算输入批次，后续可直接带回任务提交。</p>
        </div>
        <right-toolbar v-model:showSearch="showSearch" @queryTable="getList" />
      </div>

      <el-form ref="queryRef" :model="queryParams" :inline="true" label-width="84px" class="batch-page__query-form" v-show="showSearch">
        <el-form-item label="所属场景" prop="sceneId">
          <el-select v-model="queryParams.sceneId" clearable filterable style="width: 220px">
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
    </section>

    <div class="batch-page__table">
      <div class="batch-page__section-head">
        <div>
          <h3>批次列表</h3>
          <p>列表用于挑选历史批次、观察状态变化，并查看样例输入内容。</p>
        </div>
      </div>

      <el-table v-loading="loading" :data="batchList" border :height="batchTableHeight">
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

    <el-drawer v-model="detailOpen" title="批次详情工作台" size="1080px" append-to-body>
      <div class="batch-page__detail-workbench">
        <section v-if="detailData.batch" class="batch-page__detail-hero">
          <div>
            <div class="batch-page__eyebrow">输入批次</div>
            <h3>{{ detailData.batch.batchNo }}</h3>
            <div class="batch-page__detail-meta">
              <span>状态：{{ detailData.batch.batchStatus || '-' }}</span>
              <span>场景：{{ detailData.batch.sceneName || '-' }}</span>
              <span>版本：{{ detailData.batch.versionNo || '-' }}</span>
              <span>账期：{{ detailData.batch.billMonth || '-' }}</span>
              <span>来源：{{ detailData.batch.sourceType || '-' }}</span>
            </div>
          </div>
          <div class="batch-page__detail-actions">
            <el-button type="primary" icon="Promotion" @click="handleUseBatch(detailData.batch)">用于提交任务</el-button>
          </div>
        </section>

        <div class="batch-page__summary">
          <div class="batch-page__summary-card"><span>总量</span><strong>{{ detailData.batch?.totalCount || 0 }}</strong><small>批次输入对象总数</small></div>
          <div class="batch-page__summary-card"><span>有效</span><strong>{{ detailData.batch?.validCount || 0 }}</strong><small>可进入正式核算的记录</small></div>
          <div class="batch-page__summary-card"><span>错误</span><strong>{{ detailData.batch?.errorCount || 0 }}</strong><small>需要先修复的输入记录</small></div>
          <div class="batch-page__summary-card"><span>样例页</span><strong>{{ detailData.items?.length || 0 }}</strong><small>当前页已加载样例明细</small></div>
        </div>

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
        <el-alert
          v-if="detailData.loadingGuide?.title"
          class="batch-page__guide"
          :title="detailData.loadingGuide.title"
          :description="detailData.loadingGuide.description"
          :type="detailData.loadingGuide.type || 'info'"
          :closable="false"
        />

        <section class="batch-page__detail-table">
          <div class="batch-page__section-head">
            <div>
              <h3>样例明细</h3>
              <p>当前按服务端分页读取样例明细，便于在大批量批次下继续快速确认内容。</p>
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
          <pagination
            v-show="detailTotal > 0"
            :total="detailTotal"
            v-model:page="detailQuery.pageNum"
            v-model:limit="detailQuery.pageSize"
            @pagination="getDetail"
          />
        </section>
      </div>
    </el-drawer>
  </div>
</template>

<script setup name="CostTaskBatchLedger">
import { getTaskInputBatchDetail, listTaskInputBatch } from '@/api/cost/run'
import { optionselectScene } from '@/api/cost/scene'
import useSettingsStore from '@/store/modules/settings'
import { resolveWorkingCostSceneId } from '@/utils/costSceneContext'
import { COST_MENU_ROUTES } from '@/utils/costMenuRoutes'

const router = useRouter()
const route = useRoute()
const { proxy } = getCurrentInstance()
const settingsStore = useSettingsStore()
const isCompactMode = computed(() => settingsStore.costPageMode === 'COMPACT')

const loading = ref(false)
const showSearch = ref(true)
const total = ref(0)
const batchList = ref([])
const sceneOptions = ref([])
const detailOpen = ref(false)
const detailData = ref({})
const detailTotal = ref(0)
const currentBatchId = ref(undefined)

const queryParams = reactive({
  pageNum: 1,
  pageSize: 10,
  sceneId: route.query.sceneId ? Number(route.query.sceneId) : undefined,
  billMonth: '',
  batchNo: '',
  batchStatus: ''
})

const detailQuery = reactive({
  pageNum: 1,
  pageSize: 10
})

const batchTableHeight = computed(() => (isCompactMode.value ? 'calc(100dvh - 300px)' : 'calc(100dvh - 430px)'))

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

async function getDetail() {
  if (!currentBatchId.value) return
  const resp = await getTaskInputBatchDetail(currentBatchId.value, detailQuery)
  detailData.value = resp.data || {}
  detailTotal.value = resp.data?.itemTotal || 0
}

async function handleDetail(row) {
  currentBatchId.value = row.batchId
  detailQuery.pageNum = 1
  detailQuery.pageSize = 10
  await getDetail()
  detailOpen.value = true
}

function handleUseBatch(row) {
  router.push({ path: COST_MENU_ROUTES.task, query: { sceneId: row.sceneId, sourceBatchNo: row.batchNo, inputSourceType: 'INPUT_BATCH' } })
}

function summarizeJson(value) {
  if (!value) return '-'
  return value.length > 180 ? `${value.slice(0, 180)}...` : value
}

onMounted(getList)
onActivated(getList)
</script>

<style scoped lang="scss">
.batch-page {
  display: grid;
  gap: 16px;
  min-height: calc(100dvh - 124px);
  background: linear-gradient(
    180deg,
    color-mix(in srgb, var(--el-bg-color-page) 86%, #dbead7 14%) 0%,
    var(--el-bg-color-page) 240px,
    var(--el-bg-color-page) 100%
  );
}
.batch-page__hero,
.batch-page__table,
.batch-page__query-shell,
.batch-page__detail-hero,
.batch-page__summary-card,
.batch-page__detail-table {
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
.batch-page__query-shell {
  position: sticky;
  top: 0;
  z-index: 8;
  padding: 16px;
  background: color-mix(in srgb, var(--el-bg-color-overlay) 94%, #eef8e9 6%);
  backdrop-filter: blur(12px);
}
.batch-page__query-form { margin-top: 14px; }
.batch-page__table { padding: 16px; }
.batch-page__guide { margin: 16px 0; }
.batch-page__section-head { display: flex; justify-content: space-between; align-items: center; gap: 12px; margin: 0 0 16px; }
.batch-page__section-head h3 { margin: 0; font-size: 18px; }
.batch-page__section-head p { margin: 6px 0 0; color: var(--el-text-color-secondary); font-size: 13px; }
.batch-page__detail-workbench { display: grid; gap: 16px; }
.batch-page__detail-hero {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  padding: 16px 18px;
  background: color-mix(in srgb, var(--el-bg-color-overlay) 92%, var(--el-color-success-light-9) 8%);
}
.batch-page__detail-hero h3 {
  margin: 6px 0 0;
  font-size: 22px;
}
.batch-page__detail-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 12px;
}
.batch-page__detail-meta span {
  display: inline-flex;
  align-items: center;
  min-height: 28px;
  padding: 0 10px;
  border: 1px solid var(--el-border-color);
  border-radius: 999px;
  color: var(--el-text-color-secondary);
  background: var(--el-bg-color-overlay);
  font-size: 12px;
}
.batch-page__detail-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  align-content: flex-start;
  justify-content: flex-end;
}
.batch-page__summary {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
}
.batch-page__summary-card {
  display: grid;
  gap: 6px;
  padding: 12px 14px;
}
.batch-page__summary-card strong {
  color: var(--el-color-success-dark-2);
  font-size: 24px;
}
.batch-page__summary-card small {
  color: var(--el-text-color-secondary);
  line-height: 1.5;
}
.batch-page__detail-table { padding: 16px; }
.batch-page.is-compact-mode {
  gap: 12px;
  background: var(--el-bg-color-page);
}
.batch-page.is-compact-mode .batch-page__section-head > div {
  display: none;
}
.batch-page.is-compact-mode .batch-page__table .batch-page__section-head {
  display: none;
}
.batch-page.is-compact-mode .batch-page__query-form {
  margin-top: 0;
}
@media (max-width: 1200px) {
  .batch-page__hero,
  .batch-page__detail-hero,
  .batch-page__section-head {
    flex-direction: column;
    align-items: stretch;
  }
  .batch-page__hero-actions,
  .batch-page__detail-actions {
    align-items: stretch;
    justify-content: flex-start;
  }
  .batch-page__summary {
    grid-template-columns: 1fr;
  }
}
</style>

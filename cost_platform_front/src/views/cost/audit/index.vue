<template>
  <div class="app-container audit-page">
    <section class="audit-page__hero">
      <div>
        <div class="audit-page__eyebrow">审计追踪</div>
        <h2 class="audit-page__title">审计台账</h2>
        <p class="audit-page__subtitle">统一记录发布、运行、账期和缓存相关动作，便于审计留痕、问题追踪和过程复盘。</p>
      </div>
      <el-tag type="info">统一记录发布、运行、账期和缓存治理动作，便于问题追溯与复盘</el-tag>
    </section>

    <section class="audit-page__metrics">
      <div v-for="item in metricItems" :key="item.label" class="audit-page__metric-card">
        <span>{{ item.label }}</span>
        <strong>{{ item.value }}</strong>
        <small>{{ item.desc }}</small>
      </div>
    </section>

    <section class="audit-page__panel">
      <div class="audit-page__section-head">
        <div>
          <h3>运行资料</h3>
          <p>集中提供运行准备、核验记录和平台规则说明，方便业务、运营和交付团队统一查看。</p>
        </div>
        <el-button type="primary" plain icon="Refresh" @click="loadReadiness">刷新校验</el-button>
      </div>

      <div class="audit-page__resource-grid">
        <div v-for="item in resourceItems" :key="item.label" class="audit-page__resource-card">
          <div class="audit-page__resource-head">
            <strong>{{ item.label }}</strong>
            <el-tag size="small" :type="item.tagType">{{ item.tag }}</el-tag>
          </div>
          <p>{{ item.desc }}</p>
          <code>{{ item.path }}</code>
          <div class="audit-page__resource-actions">
            <el-button size="small" type="primary" plain v-copyText="item.path" v-copyText:callback="handleCopySuccess">
              复制路径
            </el-button>
          </div>
        </div>
      </div>
    </section>

    <section class="audit-page__panel">
      <div class="audit-page__section-head">
        <div>
          <h3>运行准备总览</h3>
          <p>统一汇总数据库迁移、关键库表、菜单配置和待补充的人工核验事项。</p>
        </div>
        <el-tag :type="readinessSummary.ready ? 'success' : 'warning'">
          {{ readinessSummary.ready ? '已满足运行准备要求' : '仍有待处理事项' }}
        </el-tag>
      </div>

      <div class="audit-page__readiness-cards">
        <div class="audit-page__readiness-card">
          <span>通过项</span>
          <strong>{{ readinessSummary.passCount }}</strong>
          <small>系统已自动确认的校验项</small>
        </div>
        <div class="audit-page__readiness-card audit-page__readiness-card--danger">
          <span>失败项</span>
          <strong>{{ readinessSummary.failCount }}</strong>
          <small>需要优先处理的环境、数据或配置问题</small>
        </div>
        <div class="audit-page__readiness-card audit-page__readiness-card--warning">
          <span>待人工项</span>
          <strong>{{ readinessSummary.pendingCount }}</strong>
          <small>需要补充业务核验、权限确认或记录留存</small>
        </div>
        <div class="audit-page__readiness-card">
          <span>最近迁移</span>
          <strong>{{ latestMigration.version || '-' }}</strong>
          <small>{{ latestMigration.description || '尚未读取到 Flyway 记录' }}</small>
        </div>
      </div>

      <el-table :data="readinessChecks" size="small">
        <el-table-column label="类别" prop="category" width="120" />
        <el-table-column label="校验项" prop="name" min-width="220" />
        <el-table-column label="状态" width="120" align="center">
          <template #default="scope">
            <el-tag :type="readinessTagType(scope.row.status)">{{ readinessStatusLabel(scope.row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="说明" prop="detail" min-width="280" show-overflow-tooltip />
        <el-table-column label="下一步" prop="nextAction" min-width="260" show-overflow-tooltip />
      </el-table>
    </section>

    <el-form ref="queryRef" :model="queryParams" :inline="true" label-width="88px" v-show="showSearch">
      <el-form-item label="所属场景" prop="sceneId">
        <el-select v-model="queryParams.sceneId" clearable filterable style="width: 240px">
          <el-option v-for="item in sceneOptions" :key="item.sceneId" :label="`${item.sceneCode} / ${item.sceneName}`" :value="item.sceneId" />
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
        <right-toolbar v-model:showSearch="showSearch" @queryTable="getList" />
      </div>

      <el-table v-loading="loading" :data="auditList">
        <el-table-column label="所属场景" min-width="180">
          <template #default="scope">{{ scope.row.sceneName || '-' }}<span v-if="scope.row.sceneCode"> ({{ scope.row.sceneCode }})</span></template>
        </el-table-column>
        <el-table-column label="对象类型" prop="objectType" width="130" />
        <el-table-column label="对象编码" prop="objectCode" width="180" />
        <el-table-column label="动作类型" prop="actionType" width="120" />
        <el-table-column label="动作摘要" prop="actionSummary" min-width="220" />
        <el-table-column label="操作人" prop="operatorCode" width="120" />
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

      <div class="audit-page__compare">
        <div class="audit-page__compare-panel">
          <h4>变更前</h4>
          <pre>{{ formatJson(detailData.beforeJson) }}</pre>
        </div>
        <div class="audit-page__compare-panel">
          <h4>变更后</h4>
          <pre>{{ formatJson(detailData.afterJson) }}</pre>
        </div>
      </div>
    </el-drawer>
  </div>
</template>

<script setup name="CostAudit">
import { listAudit, getAuditStats, getGoLiveReadiness } from '@/api/cost/governance'
import { optionselectScene } from '@/api/cost/scene'
import { resolveWorkingCostSceneId } from '@/utils/costSceneContext'

const { proxy } = getCurrentInstance()

const loading = ref(false)
const showSearch = ref(true)
const total = ref(0)
const auditList = ref([])
const sceneOptions = ref([])
const detailOpen = ref(false)
const detailData = ref(null)
const stats = reactive({ auditCount: 0, sceneCount: 0, operatorCount: 0, todayCount: 0 })
const readinessSummary = reactive({ ready: false, passCount: 0, failCount: 0, pendingCount: 0 })
const readinessChecks = ref([])
const latestMigration = reactive({ version: '', description: '' })

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

const resourceItems = [
  {
    label: '总任务台账',
    tag: '总览',
    tagType: 'info',
    desc: '完整拆细后的平台任务清单，适合排定优先级并持续推进。',
    path: 'D:/Desktop/cost_platform/docs/go_live_task_ledger_utf8.xlsx'
  },
  {
    label: '核验记录模板',
    tag: '执行',
    tagType: 'warning',
    desc: '用于逐条记录核验状态、证明材料、问题编号和处理动作。',
    path: 'D:/Desktop/cost_platform/docs/go_live_regression_execution_template.xlsx'
  },
  {
    label: '运行准入条件',
    tag: '口径',
    tagType: 'success',
    desc: '明确平台进入稳定运行前需要同时满足的关键条件。',
    path: 'D:/Desktop/cost_platform/docs/正式核算上线最小放行条件.md'
  },
  {
    label: '关键核验清单',
    tag: '现场',
    tagType: 'danger',
    desc: '聚焦当前最关键的核验事项，便于按统一顺序完成重点检查。',
    path: 'D:/Desktop/cost_platform/docs/正式核算上线最小执行清单.md'
  }
]

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

async function loadReadiness() {
  const resp = await getGoLiveReadiness()
  const data = resp?.data || {}
  Object.assign(readinessSummary, {
    ready: Boolean(data.summary?.ready),
    passCount: data.summary?.passCount || 0,
    failCount: data.summary?.failCount || 0,
    pendingCount: data.summary?.pendingCount || 0
  })
  readinessChecks.value = data.checks || []
  Object.assign(latestMigration, {
    version: data.latestMigration?.version || '',
    description: data.latestMigration?.description || ''
  })
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

function handleDetail(row) {
  detailData.value = row
  detailOpen.value = true
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

onMounted(() => {
  loadReadiness()
  getList()
})

onActivated(() => {
  loadReadiness()
  getList()
})

function readinessTagType(status) {
  if (status === 'PASS') {
    return 'success'
  }
  if (status === 'FAIL') {
    return 'danger'
  }
  return 'warning'
}

function readinessStatusLabel(status) {
  if (status === 'PASS') {
    return '通过'
  }
  if (status === 'FAIL') {
    return '失败'
  }
  return '待人工'
}

function handleCopySuccess() {
  proxy.$modal.msgSuccess('路径已复制')
}
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

  &__readiness-cards {
    display: grid;
    grid-template-columns: repeat(4, minmax(0, 1fr));
    gap: 16px;
    margin-bottom: 18px;
  }

  &__resource-grid {
    display: grid;
    grid-template-columns: repeat(2, minmax(0, 1fr));
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

  &__readiness-card {
    padding: 18px 22px;
    border-radius: 18px;
    border: 1px solid #e7edf7;
    background: linear-gradient(135deg, #f8fbff, #ffffff);
    display: flex;
    flex-direction: column;
    gap: 8px;

    strong {
      color: #0f766e;
      font-size: 24px;
    }

    span,
    small {
      color: #64748b;
    }
  }

  &__readiness-card--danger strong {
    color: #dc2626;
  }

  &__readiness-card--warning strong {
    color: #d97706;
  }

  &__resource-card {
    padding: 18px 22px;
    border-radius: 18px;
    border: 1px solid #e7edf7;
    background: linear-gradient(135deg, #f9fbff, #ffffff);
    display: flex;
    flex-direction: column;
    gap: 10px;

    p {
      margin: 0;
      color: #64748b;
      line-height: 1.7;
    }

    code {
      padding: 10px 12px;
      border-radius: 12px;
      background: #0f172a;
      color: #dbeafe;
      word-break: break-all;
    }
  }

  &__resource-head {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 12px;

    strong {
      color: #10233e;
      font-size: 16px;
    }
  }

  &__resource-actions {
    display: flex;
    justify-content: flex-end;
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
    &__resource-grid,
    &__readiness-cards,
    &__metrics,
    &__compare {
      grid-template-columns: 1fr;
    }
  }
}
</style>

<template>
  <div class="app-container cost-dashboard">
    <section class="dashboard-head">
      <div>
        <div class="dashboard-head__eyebrow">成本核算平台</div>
        <h1>今日工作台</h1>
        <p>聚合发布、试算、正式核算和治理告警，登录后先看待办，再进入对应业务链路处理。</p>
      </div>
      <div class="dashboard-head__actions">
        <el-button icon="Refresh" :loading="loading" @click="loadDashboard">刷新</el-button>
        <el-button type="primary" icon="Aim" @click="openRoute(COST_MENU_ROUTES.scene)">工作场景</el-button>
      </div>
    </section>

    <section class="dashboard-workbench" v-loading="loading">
      <button
        v-for="item in workItems"
        :key="item.key"
        type="button"
        class="dashboard-work-item"
        :class="`is-${item.tone}`"
        @click="openRoute(item.route, item.query)"
      >
        <span class="dashboard-work-item__label">{{ item.label }}</span>
        <strong>{{ item.value }}</strong>
        <small>{{ item.desc }}</small>
      </button>
    </section>

    <section class="dashboard-layout">
      <div class="dashboard-panel dashboard-panel--wide">
        <div class="dashboard-panel__head">
          <div>
            <h2>核算主线</h2>
            <p>按场景建模、配置费用和规则，发布后先试算，再进入正式核算和结果追踪。</p>
          </div>
        </div>
        <div class="dashboard-shortcuts">
          <button v-for="item in shortcuts" :key="item.title" type="button" class="dashboard-shortcut" @click="openRoute(item.route)">
            <el-icon><component :is="item.icon" /></el-icon>
            <span>{{ item.title }}</span>
            <small>{{ item.desc }}</small>
          </button>
        </div>
      </div>

      <div class="dashboard-panel">
        <div class="dashboard-panel__head">
          <div>
            <h2>最近发布版本</h2>
            <p>用于快速判断是否需要继续发布、试算或回滚。</p>
          </div>
          <el-button link type="primary" @click="openRoute(COST_MENU_ROUTES.publish)">查看全部</el-button>
        </div>
        <div class="dashboard-list">
          <button
            v-for="item in latestVersions"
            :key="item.versionId || item.versionNo"
            type="button"
            class="dashboard-list-item"
            @click="openRoute(COST_MENU_ROUTES.publish, item.sceneId ? { sceneId: item.sceneId } : {})"
          >
            <span>{{ item.versionNo || '-' }}</span>
            <strong>{{ item.sceneName || '未命名场景' }}</strong>
            <small>{{ item.publishDesc || item.versionStatus || '暂无发布说明' }}</small>
          </button>
          <el-empty v-if="!latestVersions.length" description="暂无发布版本" :image-size="72" />
        </div>
      </div>

      <div class="dashboard-panel">
        <div class="dashboard-panel__head">
          <div>
            <h2>高风险任务</h2>
            <p>失败任务和分片异常优先处理，避免结果台账长期缺口。</p>
          </div>
          <el-button link type="primary" @click="openRoute(COST_MENU_ROUTES.task)">任务中心</el-button>
        </div>
        <div class="dashboard-list">
          <button
            v-for="item in topRiskTasks"
            :key="item.taskId"
            type="button"
            class="dashboard-list-item"
            @click="openRoute(COST_MENU_ROUTES.task, { taskId: item.taskId, sceneId: item.sceneId, billMonth: item.billMonth, view: 'partition' })"
          >
            <span>任务 #{{ item.taskId }}</span>
            <strong>{{ item.sceneName || item.taskNo || '-' }}</strong>
            <small>失败明细 {{ item.failCount || 0 }}，失败分片 {{ item.partitionFailCount || 0 }}</small>
          </button>
          <el-empty v-if="!topRiskTasks.length" description="暂无高风险任务" :image-size="72" />
        </div>
      </div>
    </section>

    <section class="dashboard-panel">
      <div class="dashboard-panel__head">
        <div>
          <h2>下一步建议</h2>
          <p>按当前平台状态给出处理顺序，减少在各模块之间来回找入口。</p>
        </div>
      </div>
      <div class="dashboard-advice">
        <button v-for="item in adviceItems" :key="item.title" type="button" class="dashboard-advice-item" @click="openRoute(item.route)">
          <el-icon><component :is="item.icon" /></el-icon>
          <div>
            <strong>{{ item.title }}</strong>
            <span>{{ item.desc }}</span>
          </div>
        </button>
      </div>
    </section>
  </div>
</template>

<script setup name="Index">
import { Aim, Bell, Files, Histogram, MagicStick, Promotion, Refresh, Setting, Tickets } from '@element-plus/icons-vue'
import { getPublishStats, listPublish } from '@/api/cost/publish'
import { getTaskOverview, getTaskStats } from '@/api/cost/run'
import { getAlarmStats } from '@/api/cost/governance'
import { COST_MENU_ROUTES } from '@/utils/costMenuRoutes'

const router = useRouter()
const loading = ref(false)
const publishStats = ref({})
const taskStats = ref({})
const alarmStats = ref({})
const latestVersions = ref([])
const topRiskTasks = ref([])

const workItems = computed(() => [
  {
    key: 'publish',
    label: '待发布治理',
    value: formatNumber(resolvePublishTodoCount()),
    desc: '关注尚未形成生效版本或需要发布检查的场景',
    route: COST_MENU_ROUTES.publish,
    tone: 'primary'
  },
  {
    key: 'simulation',
    label: '待试算验证',
    value: formatNumber(latestVersions.value.length),
    desc: '最近发布版本建议先做试算回归',
    route: COST_MENU_ROUTES.simulation,
    tone: 'success'
  },
  {
    key: 'failedTask',
    label: '失败任务',
    value: formatNumber(resolveNumber(taskStats.value.failedCount)),
    desc: '正式核算失败或部分成功任务需要优先处理',
    route: COST_MENU_ROUTES.task,
    tone: 'warning'
  },
  {
    key: 'alarm',
    label: '治理告警',
    value: formatNumber(resolveAlarmTodoCount()),
    desc: '打开告警中心处理未确认或未关闭告警',
    route: COST_MENU_ROUTES.alert,
    tone: 'danger'
  }
])

const shortcuts = [
  { title: '场景', desc: '统一核算范围', route: COST_MENU_ROUTES.scene, icon: Aim },
  { title: '费用', desc: '维护计费对象', route: COST_MENU_ROUTES.fee, icon: Tickets },
  { title: '规则', desc: '配置计价口径', route: COST_MENU_ROUTES.rule, icon: Setting },
  { title: '发布', desc: '生成版本快照', route: COST_MENU_ROUTES.publish, icon: Promotion },
  { title: '试算', desc: '验证发布结果', route: COST_MENU_ROUTES.simulation, icon: MagicStick },
  { title: '正式核算', desc: '提交生产任务', route: COST_MENU_ROUTES.task, icon: Files },
  { title: '结果', desc: '追踪核算台账', route: COST_MENU_ROUTES.result, icon: Histogram },
  { title: '告警', desc: '处理运行异常', route: COST_MENU_ROUTES.alert, icon: Bell }
]

const adviceItems = computed(() => [
  {
    title: resolvePublishTodoCount() > 0 ? '先补齐发布治理' : '发布状态基本稳定',
    desc: resolvePublishTodoCount() > 0 ? '存在未闭环的发布治理事项，建议先进入发布中心检查。' : '可以进入试算或正式核算链路。',
    route: resolvePublishTodoCount() > 0 ? COST_MENU_ROUTES.publish : COST_MENU_ROUTES.simulation,
    icon: Promotion
  },
  {
    title: resolveNumber(taskStats.value.failedCount) > 0 ? '处理失败任务' : '任务运行暂无明显失败堆积',
    desc: resolveNumber(taskStats.value.failedCount) > 0 ? '失败任务会影响结果台账完整性，建议优先定位。' : '可以继续观察结果台账和告警中心。',
    route: resolveNumber(taskStats.value.failedCount) > 0 ? COST_MENU_ROUTES.task : COST_MENU_ROUTES.result,
    icon: Histogram
  },
  {
    title: resolveAlarmTodoCount() > 0 ? '关闭治理告警' : '告警中心暂无待办压力',
    desc: resolveAlarmTodoCount() > 0 ? '未确认或未关闭告警建议尽快处理，避免运行风险扩散。' : '继续按主线推进配置、发布和核算。',
    route: resolveAlarmTodoCount() > 0 ? COST_MENU_ROUTES.alert : COST_MENU_ROUTES.scene,
    icon: Bell
  }
])

onMounted(() => {
  loadDashboard()
})

async function loadDashboard() {
  loading.value = true
  try {
    const [publishStatsResult, publishListResult, taskStatsResult, taskOverviewResult, alarmStatsResult] = await Promise.allSettled([
      getPublishStats({}),
      listPublish({ pageNum: 1, pageSize: 5 }),
      getTaskStats({}),
      getTaskOverview({}),
      getAlarmStats({})
    ])
    publishStats.value = resolveResultData(publishStatsResult)
    latestVersions.value = resolveResultRows(publishListResult)
    taskStats.value = resolveResultData(taskStatsResult)
    topRiskTasks.value = resolveResultData(taskOverviewResult).topRiskTasks || []
    alarmStats.value = resolveResultData(alarmStatsResult)
  } finally {
    loading.value = false
  }
}

function resolveResultData(result) {
  return result.status === 'fulfilled' ? (result.value?.data || {}) : {}
}

function resolveResultRows(result) {
  return result.status === 'fulfilled' ? (result.value?.rows || []) : []
}

function resolveNumber(value) {
  const numberValue = Number(value)
  return Number.isFinite(numberValue) ? numberValue : 0
}

function resolvePublishTodoCount() {
  return Math.max(
    resolveNumber(publishStats.value.sceneCount) - resolveNumber(publishStats.value.activeVersionCount),
    0
  )
}

function resolveAlarmTodoCount() {
  return resolveNumber(alarmStats.value.openCount)
    || resolveNumber(alarmStats.value.openAlarmCount)
    || resolveNumber(alarmStats.value.unresolvedCount)
    || resolveNumber(alarmStats.value.alarmCount)
}

function formatNumber(value) {
  return resolveNumber(value).toLocaleString('zh-CN')
}

function openRoute(path, query = {}) {
  router.push({ path, query: clearEmptyQuery(query) })
}

function clearEmptyQuery(query) {
  return Object.fromEntries(Object.entries(query || {}).filter(([, value]) => value !== undefined && value !== null && value !== ''))
}
</script>

<style scoped lang="scss">
.cost-dashboard {
  display: grid;
  gap: 16px;
}

.dashboard-head,
.dashboard-panel,
.dashboard-work-item,
.dashboard-shortcut,
.dashboard-list-item,
.dashboard-advice-item {
  border: 1px solid var(--el-border-color);
  background: var(--el-bg-color-overlay);
}

.dashboard-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 18px 20px;
}

.dashboard-head__eyebrow {
  color: var(--el-color-primary);
  font-size: 13px;
  font-weight: 700;
}

.dashboard-head h1,
.dashboard-panel h2 {
  margin: 0;
  color: var(--el-text-color-primary);
}

.dashboard-head h1 {
  margin-top: 6px;
  font-size: 24px;
}

.dashboard-head p,
.dashboard-panel p,
.dashboard-work-item small,
.dashboard-shortcut small,
.dashboard-list-item small,
.dashboard-advice-item span {
  margin: 6px 0 0;
  color: var(--el-text-color-secondary);
  line-height: 1.6;
}

.dashboard-head__actions {
  display: flex;
  gap: 10px;
  flex-shrink: 0;
}

.dashboard-workbench {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
}

.dashboard-work-item,
.dashboard-shortcut,
.dashboard-list-item,
.dashboard-advice-item {
  display: grid;
  min-width: 0;
  border-radius: 6px;
  text-align: left;
  cursor: pointer;
  transition: border-color 0.2s ease, box-shadow 0.2s ease;
}

.dashboard-work-item {
  gap: 6px;
  min-height: 122px;
  padding: 16px;
}

.dashboard-work-item:hover,
.dashboard-shortcut:hover,
.dashboard-list-item:hover,
.dashboard-advice-item:hover {
  border-color: var(--el-color-primary);
  box-shadow: 0 8px 18px rgba(15, 23, 42, 0.08);
}

.dashboard-work-item__label {
  color: var(--el-text-color-regular);
  font-size: 14px;
}

.dashboard-work-item strong {
  font-size: 30px;
  line-height: 1;
}

.dashboard-work-item.is-primary strong { color: var(--el-color-primary); }
.dashboard-work-item.is-success strong { color: var(--el-color-success); }
.dashboard-work-item.is-warning strong { color: var(--el-color-warning); }
.dashboard-work-item.is-danger strong { color: var(--el-color-danger); }

.dashboard-layout {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(320px, 0.72fr);
  gap: 16px;
}

.dashboard-panel {
  border-radius: 6px;
  padding: 16px;
}

.dashboard-panel--wide {
  grid-column: span 2;
}

.dashboard-panel__head {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 14px;
}

.dashboard-panel h2 {
  font-size: 18px;
}

.dashboard-shortcuts {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 10px;
}

.dashboard-shortcut {
  grid-template-columns: 28px minmax(0, 1fr);
  gap: 8px 10px;
  padding: 14px;
}

.dashboard-shortcut .el-icon,
.dashboard-advice-item .el-icon {
  color: var(--el-color-primary);
  font-size: 20px;
}

.dashboard-shortcut span {
  color: var(--el-text-color-primary);
  font-weight: 700;
}

.dashboard-shortcut small {
  grid-column: 2;
  margin-top: 0;
}

.dashboard-list,
.dashboard-advice {
  display: grid;
  gap: 10px;
}

.dashboard-list-item {
  gap: 4px;
  padding: 12px;
}

.dashboard-list-item span {
  color: var(--el-color-primary);
  font-size: 13px;
}

.dashboard-list-item strong,
.dashboard-advice-item strong {
  color: var(--el-text-color-primary);
}

.dashboard-advice {
  grid-template-columns: repeat(3, minmax(0, 1fr));
}

.dashboard-advice-item {
  grid-template-columns: 28px minmax(0, 1fr);
  gap: 10px;
  padding: 14px;
}

@media (max-width: 1280px) {
  .dashboard-workbench,
  .dashboard-shortcuts {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .dashboard-layout,
  .dashboard-advice {
    grid-template-columns: 1fr;
  }

  .dashboard-panel--wide {
    grid-column: span 1;
  }
}

@media (max-width: 768px) {
  .dashboard-head,
  .dashboard-panel__head {
    flex-direction: column;
    align-items: flex-start;
  }

  .dashboard-workbench,
  .dashboard-shortcuts {
    grid-template-columns: 1fr;
  }
}
</style>

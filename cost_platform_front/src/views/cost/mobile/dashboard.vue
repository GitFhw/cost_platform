<template>
  <div class="mobile-dashboard" v-loading="loading">
    <section class="mobile-dashboard__head">
      <div>
        <span>成本核算平台</span>
        <h1>移动只读看板</h1>
        <p>面向管理人员查看任务、金额、异常和结果摘要。</p>
      </div>
      <el-button circle icon="Refresh" :loading="loading" @click="loadDashboard" />
    </section>

    <section class="mobile-dashboard__metrics">
      <div v-for="item in metricItems" :key="item.label" class="mobile-dashboard__metric">
        <el-icon><component :is="item.icon" /></el-icon>
        <span>{{ item.label }}</span>
        <strong>{{ item.value }}</strong>
        <small>{{ item.desc }}</small>
      </div>
    </section>

    <section class="mobile-dashboard__card">
      <div class="mobile-dashboard__section-head">
        <h2>近 7 天任务</h2>
        <el-tag type="info" effect="plain">{{ formatNumber(resolveTrendTotal(taskOverview.recentTaskTrend, 'count')) }} 个</el-tag>
      </div>
      <div class="mobile-dashboard__trend">
        <div v-for="item in taskTrendRows" :key="item.date" class="mobile-dashboard__trend-row">
          <span>{{ item.date }}</span>
          <div>
            <strong>{{ formatNumber(item.count) }}</strong>
            <small>异常 {{ formatNumber(item.failedCount) }}</small>
          </div>
        </div>
      </div>
      <el-empty v-if="!taskTrendRows.length" description="暂无任务趋势" :image-size="64" />
    </section>

    <section class="mobile-dashboard__card">
      <div class="mobile-dashboard__section-head">
        <h2>高风险任务</h2>
        <el-tag type="warning" effect="plain">{{ topRiskTasks.length }} 项</el-tag>
      </div>
      <div class="mobile-dashboard__list">
        <div v-for="item in topRiskTasks" :key="item.taskId" class="mobile-dashboard__list-item">
          <div>
            <strong>{{ item.sceneName || item.taskNo || `任务 #${item.taskId}` }}</strong>
            <small>{{ item.billMonth || '-' }} · {{ item.taskNo || `#${item.taskId}` }}</small>
          </div>
          <span>失败 {{ formatNumber(item.failCount) }}</span>
        </div>
      </div>
      <el-empty v-if="!topRiskTasks.length" description="暂无高风险任务" :image-size="64" />
    </section>

    <section class="mobile-dashboard__card">
      <div class="mobile-dashboard__section-head">
        <h2>金额摘要</h2>
        <el-tag type="success" effect="plain">{{ formatAmount(resultStats.amountTotal) }}</el-tag>
      </div>
      <div class="mobile-dashboard__list">
        <div v-for="item in feeDistribution" :key="item.feeCode || item.feeName" class="mobile-dashboard__list-item">
          <div>
            <strong>{{ item.feeName || item.feeCode || '-' }}</strong>
            <small>{{ item.feeCode || '-' }} · {{ formatNumber(item.resultCount) }} 条</small>
          </div>
          <span>{{ formatAmount(item.amountTotal) }}</span>
        </div>
      </div>
      <el-empty v-if="!feeDistribution.length" description="暂无金额分布" :image-size="64" />
    </section>

    <section class="mobile-dashboard__card">
      <div class="mobile-dashboard__section-head">
        <h2>最新结果</h2>
        <el-tag type="info" effect="plain">{{ recentResults.length }} 条</el-tag>
      </div>
      <div class="mobile-dashboard__list">
        <div v-for="item in recentResults" :key="item.resultId" class="mobile-dashboard__list-item">
          <div>
            <strong>{{ item.feeName || item.feeCode || '-' }}</strong>
            <small>{{ item.bizNo || item.objectCode || '-' }} · {{ item.billMonth || '-' }}</small>
          </div>
          <span>{{ formatAmount(item.amountValue) }}</span>
        </div>
      </div>
      <el-empty v-if="!recentResults.length" description="暂无结果摘要" :image-size="64" />
    </section>
  </div>
</template>

<script setup name="CostMobileDashboard">
import { Finished, Histogram, Money, Refresh, Warning } from '@element-plus/icons-vue'
import { getResultStats, getTaskOverview, getTaskStats, listResult } from '@/api/cost/run'
import { getAlarmStats } from '@/api/cost/governance'

const loading = ref(false)
const taskStats = ref({})
const taskOverview = ref({})
const resultStats = ref({})
const alarmStats = ref({})
const recentResults = ref([])

const metricItems = computed(() => [
  {
    label: '任务总数',
    value: formatNumber(taskStats.value.taskCount),
    desc: `失败 ${formatNumber(taskStats.value.failedCount)} · 运行中 ${formatNumber(taskStats.value.runningCount)}`,
    icon: Histogram
  },
  {
    label: '核算金额',
    value: formatAmount(resultStats.value.amountTotal),
    desc: `${formatNumber(resultStats.value.resultCount)} 条结果`,
    icon: Money
  },
  {
    label: '结果异常',
    value: formatNumber(resultStats.value.abnormalCount),
    desc: '非成功结果记录',
    icon: Warning
  },
  {
    label: '治理告警',
    value: formatNumber(resolveAlarmTodoCount()),
    desc: '未关闭或待处理告警',
    icon: Finished
  }
])

const taskTrendRows = computed(() => taskOverview.value.recentTaskTrend || [])
const topRiskTasks = computed(() => taskOverview.value.topRiskTasks || [])
const feeDistribution = computed(() => resultStats.value.feeDistribution || [])

onMounted(() => {
  loadDashboard()
})

async function loadDashboard() {
  loading.value = true
  try {
    const [taskStatsResult, taskOverviewResult, resultStatsResult, resultListResult, alarmStatsResult] = await Promise.allSettled([
      getTaskStats({}),
      getTaskOverview({}),
      getResultStats({}),
      listResult({ pageNum: 1, pageSize: 6 }),
      getAlarmStats({})
    ])
    taskStats.value = resolveResultData(taskStatsResult)
    taskOverview.value = resolveResultData(taskOverviewResult)
    resultStats.value = resolveResultData(resultStatsResult)
    recentResults.value = resolveResultRows(resultListResult)
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

function resolveTrendTotal(rows, field) {
  return (rows || []).reduce((sum, item) => sum + resolveNumber(item[field]), 0)
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

function formatAmount(value) {
  return `¥${resolveNumber(value).toLocaleString('zh-CN', {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2
  })}`
}
</script>

<style scoped lang="scss">
.mobile-dashboard {
  min-height: 100vh;
  max-width: 720px;
  margin: 0 auto;
  padding: 14px;
  display: grid;
  gap: 12px;
  background: #f5f7fb;
}

.mobile-dashboard__head,
.mobile-dashboard__metric,
.mobile-dashboard__card {
  border: 1px solid var(--el-border-color);
  border-radius: 8px;
  background: var(--el-bg-color-overlay);
}

.mobile-dashboard__head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  padding: 16px;
}

.mobile-dashboard__head span {
  color: var(--el-color-primary);
  font-size: 13px;
  font-weight: 700;
}

.mobile-dashboard__head h1,
.mobile-dashboard__section-head h2 {
  margin: 0;
  color: var(--el-text-color-primary);
}

.mobile-dashboard__head h1 {
  margin-top: 6px;
  font-size: 22px;
}

.mobile-dashboard__head p,
.mobile-dashboard__metric small,
.mobile-dashboard__list-item small,
.mobile-dashboard__trend-row small {
  margin: 6px 0 0;
  color: var(--el-text-color-secondary);
  line-height: 1.5;
}

.mobile-dashboard__metrics {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
}

.mobile-dashboard__metric {
  min-width: 0;
  padding: 14px;
  display: grid;
  gap: 6px;
}

.mobile-dashboard__metric .el-icon {
  color: var(--el-color-primary);
  font-size: 20px;
}

.mobile-dashboard__metric span {
  color: var(--el-text-color-regular);
  font-size: 13px;
}

.mobile-dashboard__metric strong {
  color: var(--el-text-color-primary);
  font-size: 22px;
  line-height: 1.2;
  word-break: break-word;
}

.mobile-dashboard__card {
  padding: 14px;
}

.mobile-dashboard__section-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  margin-bottom: 12px;
}

.mobile-dashboard__section-head h2 {
  font-size: 17px;
}

.mobile-dashboard__list,
.mobile-dashboard__trend {
  display: grid;
  gap: 8px;
}

.mobile-dashboard__list-item,
.mobile-dashboard__trend-row {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  min-width: 0;
  padding: 10px;
  border-radius: 8px;
  background: #f8fafc;
}

.mobile-dashboard__list-item strong,
.mobile-dashboard__trend-row strong {
  color: var(--el-text-color-primary);
}

.mobile-dashboard__list-item span,
.mobile-dashboard__trend-row span {
  flex-shrink: 0;
  color: var(--el-color-primary);
  font-weight: 700;
}

.mobile-dashboard__list-item div,
.mobile-dashboard__trend-row div {
  min-width: 0;
}

@media (max-width: 420px) {
  .mobile-dashboard {
    padding: 10px;
  }

  .mobile-dashboard__metrics {
    grid-template-columns: 1fr;
  }

  .mobile-dashboard__list-item,
  .mobile-dashboard__trend-row {
    flex-direction: column;
  }
}
</style>

<template>
  <div class="app-container alert-page">
    <section v-show="!isCompactMode" class="alert-page__hero">
      <div>
        <div class="alert-page__eyebrow">运行告警</div>
        <h2 class="alert-page__title">告警中心</h2>
        <p class="alert-page__subtitle">
          统一查看任务失败、重试超限、运行缓存异常等治理告警，支持确认、关闭、回跳任务中心，并为值守通知提供统一摘要。
        </p>
      </div>
      <el-tag type="danger">支持按场景、账期、等级与任务维度治理告警</el-tag>
    </section>

    <section v-show="!isCompactMode" class="alert-page__metrics">
      <div v-for="item in metricItems" :key="item.label" class="alert-page__metric-card">
        <span>{{ item.label }}</span>
        <strong>{{ item.value }}</strong>
        <small>{{ item.desc }}</small>
      </div>
    </section>

    <section v-show="!isCompactMode" class="alert-page__panel">
      <div class="alert-page__section-head">
        <div>
          <h3>通知通道</h3>
          <p>当前阶段支持 Webhook 外部通知，适合接入企业值守平台、消息网关或 IM 中转服务。</p>
        </div>
      </div>
      <div class="alert-page__notification-grid">
        <div class="alert-page__notification-card">
          <span>当前状态</span>
          <strong>{{ notificationEnabled ? '已启用' : '未启用' }}</strong>
          <el-tag :type="notificationEnabled ? 'success' : 'info'" size="small">
            {{ notificationSummary.channelType || 'WEBHOOK' }}
          </el-tag>
        </div>
        <div class="alert-page__notification-card">
          <span>目标地址</span>
          <strong class="alert-page__notification-target">{{ notificationSummary.target || '-' }}</strong>
          <small>{{ notificationSummary.configured ? '已配置通知地址' : '尚未配置通知地址' }}</small>
        </div>
        <div class="alert-page__notification-card">
          <span>扩展配置</span>
          <strong>{{ notificationSummary.headersConfigured ? '已配置' : '未配置' }}</strong>
          <small>{{ notificationSummary.secretConfigured ? '已配置签名密钥' : '未配置签名密钥' }}</small>
        </div>
      </div>
      <el-alert
          class="alert-page__notification-summary"
          :closable="false"
          :type="notificationAlertType"
          :title="notificationDescription"
      />
    </section>

    <section v-show="!isCompactMode" class="alert-page__overview-grid">
      <div class="alert-page__panel">
        <div class="alert-page__section-head">
          <div>
            <h3>近 7 天趋势</h3>
            <p>快速查看最近 7 天的告警波动、未关闭数量和严重告警数量。</p>
          </div>
        </div>
        <el-table :data="overview.recentTrend" size="small" border>
          <el-table-column label="日期" prop="date" width="120"/>
          <el-table-column label="累计触发" prop="count" width="100" align="center"/>
          <el-table-column label="未关闭" prop="openCount" width="100" align="center"/>
          <el-table-column label="严重" prop="errorCount" width="90" align="center"/>
        </el-table>
      </div>

      <div class="alert-page__panel">
        <div class="alert-page__section-head">
          <div>
            <h3>高频类型</h3>
            <p>帮助优先处理重复出现次数最多的异常类型。</p>
          </div>
        </div>
        <el-table :data="overview.topAlarmTypes" size="small" border>
          <el-table-column label="告警类型" prop="alarmType" min-width="220"/>
          <el-table-column label="累计触发" prop="count" width="100" align="center"/>
          <el-table-column label="未关闭" prop="openCount" width="90" align="center"/>
          <el-table-column label="最近触发" width="170" align="center">
            <template #default="scope">
              {{ formatDateTime(scope.row.latestTime) }}
            </template>
          </el-table-column>
        </el-table>
      </div>

      <div class="alert-page__panel">
        <div class="alert-page__section-head">
          <div>
            <h3>任务热点</h3>
            <p>定位告警最集中的任务，优先回到任务中心排查分片和失败明细。</p>
          </div>
        </div>
        <el-table :data="overview.topTasks" size="small" border>
          <el-table-column label="任务ID" prop="taskId" width="90" align="center"/>
          <el-table-column label="场景" prop="sceneName" min-width="140"/>
          <el-table-column label="账期" prop="billMonth" width="100" align="center"/>
          <el-table-column label="累计触发" prop="count" width="100" align="center"/>
          <el-table-column label="未关闭" prop="openCount" width="90" align="center"/>
          <el-table-column label="操作" width="130" align="center">
            <template #default="scope">
              <el-button
                  link
                  type="primary"
                  icon="Histogram"
                  @click="openTaskCenter(scope.row.taskId, scope.row.billMonth, 'partition')"
              >
                查看分片
              </el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>

      <div class="alert-page__panel">
        <div class="alert-page__section-head">
          <div>
            <h3>等级分布</h3>
            <p>判断当前告警更偏提示、警告还是严重异常。</p>
          </div>
        </div>
        <div class="alert-page__level-list">
          <div v-for="item in overview.levelDistribution" :key="item.alarmLevel" class="alert-page__level-item">
            <dict-tag :options="alarmLevelOptions" :value="item.alarmLevel"/>
            <strong>{{ item.count }}</strong>
          </div>
          <el-empty v-if="!overview.levelDistribution.length" description="暂无数据" :image-size="60"/>
        </div>
      </div>
    </section>

    <section class="alert-page__panel alert-page__panel--cache">
      <div class="alert-page__section-head">
        <div>
          <h3>运行快照缓存</h3>
          <p>Redis 只承接运行态快照缓存，数据库仍是唯一真实来源；缓存刷新成功会自动关闭历史缓存告警。</p>
        </div>
      </div>

      <el-form :model="cacheForm" :inline="true" label-width="84px">
        <el-form-item label="所属场景">
          <el-select v-model="cacheForm.sceneId" clearable filterable style="width: 240px"
                     @change="handleCacheSceneChange">
            <el-option
                v-for="item in sceneOptions"
                :key="item.sceneId"
                :label="buildSceneLabel(item)"
                :value="item.sceneId"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="发布版本">
          <el-select v-model="cacheForm.versionId" clearable filterable style="width: 220px">
            <el-option
                v-for="item in cacheVersionOptions"
                :key="item.versionId"
                :label="item.versionNo"
                :value="item.versionId"
            />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" icon="Search" @click="loadCacheStats">刷新状态</el-button>
          <el-button type="warning" icon="Refresh" @click="handleRefreshCache" v-hasPermi="['cost:cache:refresh']">
            刷新缓存
          </el-button>
        </el-form-item>
      </el-form>

      <div class="alert-page__cache-grid">
        <div class="alert-page__cache-card">
          <span>缓存总数</span>
          <strong>{{ cacheStats.cacheCount || 0 }}</strong>
        </div>
        <div class="alert-page__cache-card">
          <span>当前键是否存在</span>
          <strong>{{ cacheStats.exists ? '是' : '否' }}</strong>
        </div>
        <div class="alert-page__cache-card">
          <span>剩余秒数</span>
          <strong>{{ cacheStats.expireSeconds ?? '-' }}</strong>
        </div>
        <div class="alert-page__cache-card">
          <span>缓存键</span>
          <strong class="alert-page__cache-key">{{ cacheStats.cacheKey || '-' }}</strong>
        </div>
        <div class="alert-page__cache-card">
          <span>Redis 连接</span>
          <strong>{{ redisStatus.connected ? '已连接' : '未连接' }}</strong>
          <small>{{ redisStatus.message || redisStatus.ping || '-' }}</small>
        </div>
        <div class="alert-page__cache-card">
          <span>Redis 版本 / 模式</span>
          <strong>{{ redisStatus.redisVersion || '-' }}</strong>
          <small>{{ redisStatus.redisMode || '-' }}</small>
        </div>
        <div class="alert-page__cache-card">
          <span>Redis 内存</span>
          <strong>{{ redisStatus.usedMemoryHuman || '-' }}</strong>
          <small>最大 {{ redisStatus.maxMemoryHuman || '-' }}</small>
        </div>
        <div class="alert-page__cache-card">
          <span>Redis DB 键数</span>
          <strong>{{ redisStatus.dbSize ?? '-' }}</strong>
          <small>运行快照 {{ redisStatus.runtimeCacheKeyCount || 0 }} 个</small>
        </div>
        <div class="alert-page__cache-card">
          <span>最近刷新审计</span>
          <strong>{{ formatDateTime(lastCacheAudit.operateTime) }}</strong>
          <small>{{ lastCacheAudit.operatorName || lastCacheAudit.operatorCode || '-' }}</small>
        </div>
        <div class="alert-page__cache-card">
          <span>未关闭缓存告警</span>
          <strong>{{ cacheStats.openCacheAlarmCount || 0 }}</strong>
          <small>{{ latestCacheAlarm.alarmTitle || '刷新成功会自动关闭历史告警' }}</small>
        </div>
      </div>
      <el-alert
          v-if="cacheStats.openCacheAlarmCount"
          class="alert-page__cache-alert"
          type="warning"
          :closable="false"
          :title="`当前条件存在 ${cacheStats.openCacheAlarmCount} 条未关闭缓存刷新告警，刷新成功后会自动关闭。`"
      />
      <el-alert
          v-if="refreshFeedback"
          class="alert-page__cache-alert"
          type="success"
          :closable="false"
          :title="refreshFeedback"
      />
    </section>

    <el-form ref="queryRef" :model="queryParams" :inline="true" label-width="84px" v-show="showSearch">
      <el-form-item label="所属场景" prop="sceneId">
        <el-select v-model="queryParams.sceneId" clearable filterable style="width: 240px">
          <el-option
              v-for="item in sceneOptions"
              :key="item.sceneId"
              :label="buildSceneLabel(item)"
              :value="item.sceneId"
          />
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
      <el-form-item label="告警级别" prop="alarmLevel">
        <el-select v-model="queryParams.alarmLevel" clearable style="width: 180px">
          <el-option v-for="item in alarmLevelOptions" :key="item.value" :label="item.label" :value="item.value"/>
        </el-select>
      </el-form-item>
      <el-form-item label="处理状态" prop="alarmStatus">
        <el-select v-model="queryParams.alarmStatus" clearable style="width: 180px">
          <el-option v-for="item in alarmStatusOptions" :key="item.value" :label="item.label" :value="item.value"/>
        </el-select>
      </el-form-item>
      <el-form-item label="告警标题" prop="alarmTitle">
        <el-input v-model="queryParams.alarmTitle" clearable style="width: 220px"/>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-alert
        v-if="routeContext.taskId"
        class="alert-page__context"
        type="info"
        :closable="false"
        :title="`当前正在查看任务 ${routeContext.taskId} 的告警，已自动带入场景、账期和未关闭状态过滤。`"
    />

    <section class="alert-page__panel">
      <div class="alert-page__section-head">
        <div>
          <h3>治理告警台账</h3>
          <p>承接任务失败、重试超限、缓存刷新异常等治理告警，支持按累计触发次数和最近触发时间判断处置优先级。</p>
        </div>
        <right-toolbar v-model:showSearch="showSearch" @queryTable="getList"/>
      </div>

      <el-table v-loading="loading" :data="alarmList">
        <el-table-column label="所属场景" min-width="180">
          <template #default="scope">
            {{ scope.row.sceneName || '-' }}<span v-if="scope.row.sceneCode"> ({{ scope.row.sceneCode }})</span>
          </template>
        </el-table-column>
        <el-table-column label="账期" prop="billMonth" width="110" align="center"/>
        <el-table-column label="告警类型" prop="alarmType" width="190"/>
        <el-table-column label="告警级别" width="120" align="center">
          <template #default="scope">
            <dict-tag :options="alarmLevelOptions" :value="scope.row.alarmLevel"/>
          </template>
        </el-table-column>
        <el-table-column label="处理状态" width="120" align="center">
          <template #default="scope">
            <dict-tag :options="alarmStatusOptions" :value="scope.row.alarmStatus"/>
          </template>
        </el-table-column>
        <el-table-column label="累计触发" prop="occurrenceCount" width="100" align="center"/>
        <el-table-column label="告警标题" prop="alarmTitle" min-width="200"/>
        <el-table-column label="告警内容" prop="alarmContent" min-width="260" :show-overflow-tooltip="true"/>
        <el-table-column label="最近触发" width="180" align="center">
          <template #default="scope">{{
              formatDateTime(scope.row.latestTriggerTime || scope.row.triggerTime)
            }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="190" fixed="right" align="center">
          <template #default="scope">
            <el-button
                link
                type="primary"
                icon="View"
                :disabled="!scope.row.taskId"
                @click="openTaskCenter(scope.row.taskId, scope.row.billMonth, 'detail')"
            >
              任务详情
            </el-button>
            <el-button
                link
                type="warning"
                icon="Bell"
                :disabled="scope.row.alarmStatus !== 'OPEN'"
                @click="handleAck(scope.row)"
                v-hasPermi="['cost:alarm:ack']"
            >
              确认
            </el-button>
            <el-button
                link
                type="success"
                icon="CircleCheck"
                :disabled="scope.row.alarmStatus === 'RESOLVED'"
                @click="handleResolve(scope.row)"
                v-hasPermi="['cost:alarm:resolve']"
            >
              关闭
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <pagination
          v-show="total > 0"
          :total="total"
          v-model:page="queryParams.pageNum"
          v-model:limit="queryParams.pageSize"
          @pagination="getList"
      />
    </section>
  </div>
</template>

<script setup name="CostAlert">
import {computed, getCurrentInstance, onActivated, onMounted, reactive, ref} from 'vue'
import {useRoute, useRouter} from 'vue-router'
import {ElMessageBox} from 'element-plus'
import {optionselectScene} from '@/api/cost/scene'
import {listVersionOptions} from '@/api/cost/run'
import {
  ackAlarm,
  getAlarmOverview,
  getAlarmStats,
  getRuntimeCacheStats,
  listAlarm,
  refreshRuntimeCache,
  resolveAlarm
} from '@/api/cost/governance'
import {getRemoteDictOptionMap} from '@/utils/dictRemote'
import { COST_MENU_ROUTES } from '@/utils/costMenuRoutes'
import useSettingsStore from '@/store/modules/settings'
import {resolveWorkingCostSceneId} from '@/utils/costSceneContext'
import {resolveWorkingBillMonth, syncCostWorkContext} from '@/utils/costWorkContext'
import { useCostWorkSceneAutoRefresh } from '@/utils/costWorkSceneAutoRefresh'

const route = useRoute()
const router = useRouter()
const {proxy} = getCurrentInstance()
const settingsStore = useSettingsStore()
const isCompactMode = computed(() => settingsStore.costPageMode === 'COMPACT')

const loading = ref(false)
const showSearch = ref(true)
const total = ref(0)
const alarmList = ref([])
const sceneOptions = ref([])
const cacheVersionOptions = ref([])
const alarmLevelOptions = ref([])
const alarmStatusOptions = ref([])
const cacheStats = ref({})
const refreshFeedback = ref('')
const stats = reactive({
  alarmCount: 0,
  occurrenceCount: 0,
  openCount: 0,
  ackedCount: 0,
  resolvedCount: 0
})
const overview = reactive({
  recentTrend: [],
  topAlarmTypes: [],
  topTasks: [],
  levelDistribution: [],
  notificationSummary: {}
})

const queryParams = reactive({
  pageNum: 1,
  pageSize: 10,
  sceneId: undefined,
  taskId: route.query.taskId ? Number(route.query.taskId) : undefined,
  billMonth: resolveWorkingBillMonth(route.query.billMonth),
  alarmLevel: undefined,
  alarmStatus: undefined,
  alarmTitle: ''
})

const routeContext = reactive({
  taskId: route.query.taskId ? Number(route.query.taskId) : undefined
})

const cacheForm = reactive({
  sceneId: undefined,
  versionId: undefined
})

const metricItems = computed(() => [
  {label: '告警记录数', value: stats.alarmCount, desc: '当前筛选范围内的治理告警记录数量。'},
  {label: '累计触发数', value: stats.occurrenceCount, desc: '聚合同源告警后的累计触发次数，更适合生产值守视角。'},
  {label: '未处理', value: stats.openCount, desc: '仍待确认或关闭的告警数量。'},
  {label: '已关闭', value: stats.resolvedCount, desc: '已完成自愈或人工闭环的告警数量。'}
])

const notificationSummary = computed(() => overview.notificationSummary || {})
const redisStatus = computed(() => cacheStats.value.redisStatus || {})
const lastCacheAudit = computed(() => cacheStats.value.lastRefreshAudit || {})
const latestCacheAlarm = computed(() => cacheStats.value.latestCacheAlarm || {})
const notificationEnabled = computed(() => Boolean(notificationSummary.value.enabled))
const notificationAlertType = computed(() => {
  if (!notificationEnabled.value) {
    return 'info'
  }
  return notificationSummary.value.configured ? 'success' : 'warning'
})
const notificationDescription = computed(() => {
  if (!notificationEnabled.value) {
    return '当前未启用外部通知，告警仅保留在平台台账中。'
  }
  if (!notificationSummary.value.configured) {
    return '当前已打开 Webhook 开关，但尚未配置通知地址。'
  }
  return '当前已启用 Webhook 外部通知，可转发到企业值守平台、消息网关或 IM 中转服务。'
})

function buildSceneLabel(item) {
  return `${item.sceneName} / ${item.sceneCode}`
}

function formatDateTime(value) {
  return value ? proxy.parseTime(value) : '-'
}

async function loadBaseOptions() {
  const [dictMap, sceneResp] = await Promise.all([
    getRemoteDictOptionMap(['cost_alarm_level', 'cost_alarm_status']),
    optionselectScene({status: '0', pageNum: 1, pageSize: 1000})
  ])
  alarmLevelOptions.value = dictMap.cost_alarm_level || []
  alarmStatusOptions.value = dictMap.cost_alarm_status || []
  sceneOptions.value = sceneResp?.data || []

  const preferredQuerySceneId = resolveWorkingCostSceneId(
      sceneOptions.value,
      queryParams.sceneId,
      route.query.sceneId ? Number(route.query.sceneId) : undefined
  )
  queryParams.sceneId = preferredQuerySceneId
  queryParams.billMonth = route.query.billMonth || queryParams.billMonth
  queryParams.alarmStatus = route.query.alarmStatus || queryParams.alarmStatus
  cacheForm.sceneId = resolveWorkingCostSceneId(sceneOptions.value, cacheForm.sceneId, preferredQuerySceneId)
}

async function loadOverview() {
  const resp = await getAlarmOverview(queryParams)
  Object.assign(overview, {
    recentTrend: resp?.data?.recentTrend || [],
    topAlarmTypes: resp?.data?.topAlarmTypes || [],
    topTasks: resp?.data?.topTasks || [],
    levelDistribution: resp?.data?.levelDistribution || [],
    notificationSummary: resp?.data?.notificationSummary || {}
  })
}

async function loadCacheStats() {
  const resp = await getRuntimeCacheStats({
    sceneId: cacheForm.sceneId,
    versionId: cacheForm.versionId
  })
  cacheStats.value = resp.data || {}
}

async function getList() {
  loading.value = true
  try {
    await loadBaseOptions()
    if (cacheForm.sceneId) {
      const versionResp = await listVersionOptions(cacheForm.sceneId)
      cacheVersionOptions.value = versionResp.data || []
    } else {
      cacheVersionOptions.value = []
    }

    const [listResp, statsResp] = await Promise.all([
      listAlarm(queryParams),
      getAlarmStats(queryParams)
    ])
    alarmList.value = listResp.rows || []
    total.value = listResp.total || 0
    Object.assign(stats, {
      alarmCount: statsResp?.data?.alarmCount || 0,
      occurrenceCount: statsResp?.data?.occurrenceCount || 0,
      openCount: statsResp?.data?.openCount || 0,
      ackedCount: statsResp?.data?.ackedCount || 0,
      resolvedCount: statsResp?.data?.resolvedCount || 0
    })
    await loadOverview()
  } finally {
    loading.value = false
  }
}

async function handleCacheSceneChange(sceneId) {
  cacheForm.sceneId = sceneId
  cacheForm.versionId = undefined
  if (!sceneId) {
    cacheVersionOptions.value = []
    cacheStats.value = {}
    return
  }
  const resp = await listVersionOptions(sceneId)
  cacheVersionOptions.value = resp.data || []
}

function handleQuery() {
  queryParams.pageNum = 1
  getList()
}

function resetQuery() {
  proxy.resetForm('queryRef')
  queryParams.pageNum = 1
  queryParams.pageSize = 10
  queryParams.taskId = routeContext.taskId
  queryParams.sceneId = route.query.sceneId ? Number(route.query.sceneId) : queryParams.sceneId
  queryParams.billMonth = resolveWorkingBillMonth(route.query.billMonth)
  queryParams.alarmStatus = route.query.alarmStatus || undefined
  getList()
}

async function handleAck(row) {
  await ackAlarm(row.alarmId)
  proxy.$modal.msgSuccess('告警已确认')
  getList()
}

async function handleResolve(row) {
  await resolveAlarm(row.alarmId)
  proxy.$modal.msgSuccess('告警已关闭')
  getList()
}

function openTaskCenter(taskId, billMonth, view) {
  if (!taskId) {
    return
  }
  router.push({
    path: COST_MENU_ROUTES.task,
    query: {
      sceneId: queryParams.sceneId,
      billMonth: billMonth || queryParams.billMonth,
      taskId,
      view
    }
  })
}

watch(
  () => [queryParams.sceneId, queryParams.billMonth],
  ([sceneId, billMonth]) => {
    syncCostWorkContext({sceneId, billMonth})
  },
  {immediate: true}
)

useCostWorkSceneAutoRefresh({
  queryParams,
  sceneOptions,
  beforeRefresh: sceneId => {
    cacheForm.sceneId = sceneId
    cacheForm.versionId = undefined
    cacheVersionOptions.value = []
  },
  refresh: getList
})

async function handleRefreshCache() {
  await ElMessageBox.confirm(
      '确认刷新当前条件下的运行快照缓存吗？数据库仍是真实来源，刷新成功后会自动关闭历史缓存告警。',
      '提示',
      {type: 'warning'}
  )
  const resp = await refreshRuntimeCache({sceneId: cacheForm.sceneId, versionId: cacheForm.versionId})
  const result = resp?.data || {}
  refreshFeedback.value = `运行快照缓存已刷新，删除 ${result.deletedCount || 0} 个缓存键，关闭 ${result.resolvedAlarmCount || 0} 条缓存告警。`
  proxy.$modal.msgSuccess(refreshFeedback.value)
  await loadCacheStats()
  await getList()
}

onMounted(async () => {
  await getList()
  await loadCacheStats()
})

onActivated(async () => {
  await getList()
  await loadCacheStats()
})
</script>

<style lang="scss" scoped>
.alert-page {
  display: flex;
  flex-direction: column;
  gap: 20px;

  &__hero,
  &__panel,
  &__metric-card,
  &__cache-card,
  &__notification-card {
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
    color: #64748b;
    line-height: 1.7;
  }

  &__metrics {
    display: grid;
    grid-template-columns: repeat(4, minmax(0, 1fr));
    gap: 16px;
  }

  &__overview-grid {
    display: grid;
    grid-template-columns: repeat(2, minmax(0, 1fr));
    gap: 16px;
  }

  &__metric-card,
  &__notification-card {
    padding: 18px 22px;
    display: flex;
    flex-direction: column;
    gap: 8px;

    strong {
      color: #ff7a59;
      font-size: 24px;
    }

    span,
    small {
      color: #7c8798;
    }
  }

  &__notification-grid {
    display: grid;
    grid-template-columns: repeat(3, minmax(0, 1fr));
    gap: 16px;
  }

  &__notification-summary {
    margin-top: 16px;
  }

  &__notification-target {
    color: #0f766e !important;
    font-size: 18px !important;
    word-break: break-all;
  }

  &__section-head {
    display: flex;
    justify-content: space-between;
    align-items: center;
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

  &__cache-grid {
    display: grid;
    grid-template-columns: repeat(auto-fit, minmax(190px, 1fr));
    gap: 14px;
    margin-top: 18px;
  }

  &__cache-card {
    padding: 16px 18px;
    display: flex;
    flex-direction: column;
    gap: 8px;

    span {
      color: #7c8798;
    }

    strong {
      color: #0f766e;
      font-size: 24px;
      word-break: break-all;
    }
  }

  &__cache-key {
    font-size: 16px !important;
    line-height: 1.6;
  }

  &__cache-alert {
    margin-top: 14px;
  }

  &__level-list {
    display: grid;
    gap: 12px;
  }

  &__level-item {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 14px 16px;
    border-radius: 14px;
    background: #f8fafc;

    strong {
      color: #10233e;
      font-size: 24px;
    }
  }

  &__context {
    margin-top: -4px;
  }
}

@media (max-width: 1360px) {
  .alert-page {
    &__metrics,
    &__overview-grid,
    &__cache-grid,
    &__notification-grid {
      grid-template-columns: 1fr;
    }
  }
}
</style>

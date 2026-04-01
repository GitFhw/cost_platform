<template>
  <div class="app-container alert-page">
    <section class="alert-page__hero">
      <div>
        <div class="alert-page__eyebrow">线程六 · 步骤 3-4</div>
        <h2 class="alert-page__title">告警中心</h2>
        <p class="alert-page__subtitle">统一承接任务失败、重试超限和运行缓存治理告警，并提供缓存刷新入口，先把治理增强第一层闭环落稳。</p>
      </div>
      <el-tag type="danger">当前阶段先做治理级告警，不提前扩展通知中心和多渠道订阅</el-tag>
    </section>

    <section class="alert-page__metrics">
      <div v-for="item in metricItems" :key="item.label" class="alert-page__metric-card">
        <span>{{ item.label }}</span>
        <strong>{{ item.value }}</strong>
        <small>{{ item.desc }}</small>
      </div>
    </section>

    <section class="alert-page__panel alert-page__panel--cache">
      <div class="alert-page__section-head">
        <div>
          <h3>运行快照缓存</h3>
          <p>Redis 只承接运行态快照缓存，数据库仍是唯一真实来源。</p>
        </div>
      </div>

      <el-form :model="cacheForm" :inline="true" label-width="84px">
        <el-form-item label="所属场景">
          <el-select v-model="cacheForm.sceneId" clearable filterable style="width: 240px" @change="handleCacheSceneChange">
            <el-option v-for="item in sceneOptions" :key="item.sceneId" :label="`${item.sceneCode} / ${item.sceneName}`" :value="item.sceneId" />
          </el-select>
        </el-form-item>
        <el-form-item label="发布版本">
          <el-select v-model="cacheForm.versionId" clearable filterable style="width: 220px">
            <el-option v-for="item in cacheVersionOptions" :key="item.versionId" :label="item.versionNo" :value="item.versionId" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" icon="Search" @click="loadCacheStats">刷新状态</el-button>
          <el-button type="warning" icon="Refresh" @click="handleRefreshCache" v-hasPermi="['cost:cache:refresh']">清理缓存</el-button>
        </el-form-item>
      </el-form>

      <div class="alert-page__cache-grid">
        <div class="alert-page__cache-card"><span>缓存总数</span><strong>{{ cacheStats.cacheCount || 0 }}</strong></div>
        <div class="alert-page__cache-card"><span>当前键存在</span><strong>{{ cacheStats.exists ? '是' : '否' }}</strong></div>
        <div class="alert-page__cache-card"><span>剩余秒数</span><strong>{{ cacheStats.expireSeconds ?? '-' }}</strong></div>
        <div class="alert-page__cache-card"><span>缓存键</span><strong class="alert-page__cache-key">{{ cacheStats.cacheKey || '-' }}</strong></div>
      </div>
    </section>

    <el-form ref="queryRef" :model="queryParams" :inline="true" label-width="84px" v-show="showSearch">
      <el-form-item label="所属场景" prop="sceneId">
        <el-select v-model="queryParams.sceneId" clearable filterable style="width: 240px">
          <el-option v-for="item in sceneOptions" :key="item.sceneId" :label="`${item.sceneCode} / ${item.sceneName}`" :value="item.sceneId" />
        </el-select>
      </el-form-item>
      <el-form-item label="账期" prop="billMonth">
        <el-input v-model="queryParams.billMonth" clearable placeholder="yyyy-MM" style="width: 160px" />
      </el-form-item>
      <el-form-item label="告警级别" prop="alarmLevel">
        <el-select v-model="queryParams.alarmLevel" clearable style="width: 180px">
          <el-option v-for="item in alarmLevelOptions" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="处理状态" prop="alarmStatus">
        <el-select v-model="queryParams.alarmStatus" clearable style="width: 180px">
          <el-option v-for="item in alarmStatusOptions" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="告警标题" prop="alarmTitle">
        <el-input v-model="queryParams.alarmTitle" clearable style="width: 220px" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <section class="alert-page__panel">
      <div class="alert-page__section-head">
        <div>
          <h3>治理告警台账</h3>
          <p>承接任务失败、重试超限和缓存刷新异常。</p>
        </div>
        <right-toolbar v-model:showSearch="showSearch" @queryTable="getList" />
      </div>

      <el-table v-loading="loading" :data="alarmList">
        <el-table-column label="所属场景" min-width="180">
          <template #default="scope">{{ scope.row.sceneName || '-' }}<span v-if="scope.row.sceneCode"> ({{ scope.row.sceneCode }})</span></template>
        </el-table-column>
        <el-table-column label="账期" prop="billMonth" width="110" align="center" />
        <el-table-column label="告警类型" prop="alarmType" width="180" />
        <el-table-column label="告警级别" width="120" align="center">
          <template #default="scope">
            <dict-tag :options="alarmLevelOptions" :value="scope.row.alarmLevel" />
          </template>
        </el-table-column>
        <el-table-column label="处理状态" width="120" align="center">
          <template #default="scope">
            <dict-tag :options="alarmStatusOptions" :value="scope.row.alarmStatus" />
          </template>
        </el-table-column>
        <el-table-column label="告警标题" prop="alarmTitle" min-width="200" />
        <el-table-column label="告警内容" prop="alarmContent" min-width="260" :show-overflow-tooltip="true" />
        <el-table-column label="触发时间" width="180" align="center">
          <template #default="scope">{{ proxy.parseTime(scope.row.triggerTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="190" fixed="right" align="center">
          <template #default="scope">
            <el-button link type="warning" icon="Bell" :disabled="scope.row.alarmStatus !== 'OPEN'" @click="handleAck(scope.row)" v-hasPermi="['cost:alarm:ack']">确认</el-button>
            <el-button link type="success" icon="CircleCheck" :disabled="scope.row.alarmStatus === 'RESOLVED'" @click="handleResolve(scope.row)" v-hasPermi="['cost:alarm:resolve']">关闭</el-button>
          </template>
        </el-table-column>
      </el-table>

      <pagination v-show="total > 0" :total="total" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" @pagination="getList" />
    </section>
  </div>
</template>

<script setup name="CostAlert">
import { ElMessageBox } from 'element-plus'
import { optionselectScene } from '@/api/cost/scene'
import { listVersionOptions } from '@/api/cost/run'
import { ackAlarm, getAlarmStats, getRuntimeCacheStats, listAlarm, refreshRuntimeCache, resolveAlarm } from '@/api/cost/governance'
import { getRemoteDictOptionMap } from '@/utils/dictRemote'

const { proxy } = getCurrentInstance()

const loading = ref(false)
const showSearch = ref(true)
const total = ref(0)
const alarmList = ref([])
const sceneOptions = ref([])
const cacheVersionOptions = ref([])
const alarmLevelOptions = ref([])
const alarmStatusOptions = ref([])
const cacheStats = ref({})
const stats = reactive({ alarmCount: 0, openCount: 0, ackedCount: 0, resolvedCount: 0 })

const queryParams = reactive({
  pageNum: 1,
  pageSize: 10,
  sceneId: undefined,
  billMonth: '',
  alarmLevel: undefined,
  alarmStatus: undefined,
  alarmTitle: ''
})

const cacheForm = reactive({
  sceneId: undefined,
  versionId: undefined
})

const metricItems = computed(() => [
  { label: '告警总数', value: stats.alarmCount, desc: '当前筛选范围内的治理告警数量' },
  { label: '未处理', value: stats.openCount, desc: '仍待确认或关闭的告警数量' },
  { label: '已确认', value: stats.ackedCount, desc: '已人工确认但未彻底关闭的告警数量' },
  { label: '已关闭', value: stats.resolvedCount, desc: '已完成处理闭环的告警数量' }
])

async function loadBaseOptions() {
  const [dictMap, sceneResp] = await Promise.all([
    getRemoteDictOptionMap(['cost_alarm_level', 'cost_alarm_status']),
    optionselectScene({ status: '0', pageNum: 1, pageSize: 1000 })
  ])
  alarmLevelOptions.value = dictMap.cost_alarm_level || []
  alarmStatusOptions.value = dictMap.cost_alarm_status || []
  sceneOptions.value = sceneResp?.data || []
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
    Object.assign(stats, statsResp.data || {})
  } finally {
    loading.value = false
  }
}

async function loadCacheStats() {
  const resp = await getRuntimeCacheStats({ sceneId: cacheForm.sceneId, versionId: cacheForm.versionId })
  cacheStats.value = resp.data || {}
}

async function handleCacheSceneChange(sceneId) {
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

async function handleRefreshCache() {
  await ElMessageBox.confirm('确认清理当前条件下的运行快照缓存吗？数据库仍为真实来源。', '提示', { type: 'warning' })
  await refreshRuntimeCache({ sceneId: cacheForm.sceneId, versionId: cacheForm.versionId })
  proxy.$modal.msgSuccess('运行快照缓存已刷新')
  loadCacheStats()
  getList()
}

onMounted(async () => {
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
  &__cache-card {
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

  &__metric-card {
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
    grid-template-columns: repeat(4, minmax(0, 1fr));
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
}

@media (max-width: 1360px) {
  .alert-page {
    &__metrics,
    &__cache-grid {
      grid-template-columns: 1fr;
    }
  }
}
</style>

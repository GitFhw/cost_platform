<template>
  <div class="app-container publish-audit">
    <section v-show="!isCompactMode" class="publish-audit__hero">
      <div>
        <div class="publish-audit__eyebrow">发布审计</div>
        <h2 class="publish-audit__title">发布审计工作台</h2>
        <p class="publish-audit__subtitle">
          独立查看版本台账、版本详情和版本差异，便于交付、运营和业务侧按“只读审计”方式回看发布历史。
        </p>
      </div>
      <div class="publish-audit__hero-actions">
        <el-button icon="Back" @click="handleBackToPublish">返回发布中心</el-button>
        <el-tag type="info">当前为只读审计视角，不承载发布动作</el-tag>
      </div>
    </section>

    <section v-show="!isCompactMode" class="publish-audit__metrics">
      <div v-for="item in metricItems" :key="item.label" class="publish-audit__metric-card">
        <span>{{ item.label }}</span>
        <strong>{{ item.value }}</strong>
        <small>{{ item.desc }}</small>
      </div>
    </section>

    <el-form ref="queryRef" :model="queryParams" :inline="true" label-width="84px" v-show="showSearch">
      <el-form-item label="所属场景" prop="sceneId">
        <el-select v-model="queryParams.sceneId" clearable filterable placeholder="请选择场景" style="width: 240px">
          <el-option v-for="item in sceneOptions" :key="item.sceneId" :label="`${item.sceneName} / ${item.sceneCode}`" :value="item.sceneId" />
        </el-select>
      </el-form-item>
      <el-form-item label="业务域" prop="businessDomain">
        <el-select v-model="queryParams.businessDomain" clearable placeholder="请选择业务域" style="width: 180px">
          <el-option v-for="item in businessDomainOptions" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="版本状态" prop="versionStatus">
        <el-select v-model="queryParams.versionStatus" clearable placeholder="请选择版本状态" style="width: 180px">
          <el-option v-for="item in versionStatusOptions" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="版本号" prop="versionNo">
        <el-input v-model="queryParams.versionNo" clearable style="width: 180px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <section class="publish-audit__panel">
      <div class="publish-audit__section-head">
        <div>
          <h3>版本台账</h3>
          <p>按场景、业务域、版本状态和版本号检索历史发布版本，进入版本详情和版本差异工作区。</p>
        </div>
        <right-toolbar v-model:showSearch="showSearch" @queryTable="getList" />
      </div>

      <el-table v-loading="loading" :data="versionList">
        <el-table-column label="版本号" prop="versionNo" width="150" align="center" />
        <el-table-column label="场景" min-width="180" align="center">
          <template #default="scope">{{ scope.row.sceneName }} ({{ scope.row.sceneCode }})</template>
        </el-table-column>
        <el-table-column label="状态" width="120" align="center">
          <template #default="scope">
            <dict-tag :options="versionStatusOptions" :value="scope.row.versionStatus" />
          </template>
        </el-table-column>
        <el-table-column label="检查结果" width="170" align="center">
          <template #default="scope">
            <el-tag :type="resolveValidationMeta(scope.row).tag">{{ resolveValidationMeta(scope.row).label }}</el-tag>
            <div class="publish-audit__validation-note">{{ buildValidationNote(scope.row) }}</div>
          </template>
        </el-table-column>
        <el-table-column label="发布说明" prop="publishDesc" min-width="220" :show-overflow-tooltip="true" />
        <el-table-column label="发布人" prop="publishedBy" width="120" align="center" />
        <el-table-column label="发布时间" width="180" align="center">
          <template #default="scope">{{ parseTime(scope.row.publishedTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="180" fixed="right" align="center">
          <template #default="scope">
            <el-button link type="primary" icon="View" @click="handleDetail(scope.row)">详情</el-button>
            <el-button link type="primary" icon="Tickets" @click="handleDiff(scope.row)">差异</el-button>
          </template>
        </el-table-column>
      </el-table>

      <pagination v-show="total > 0" :total="total" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" @pagination="getList" />
    </section>

    <el-drawer v-model="detailOpen" title="版本详情" size="980px" append-to-body>
      <div v-if="detailData.version">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="版本号">{{ detailData.version.versionNo }}</el-descriptions-item>
          <el-descriptions-item label="当前状态">{{ resolveVersionLabel(detailData.version.versionStatus) }}</el-descriptions-item>
          <el-descriptions-item label="场景">{{ detailData.version.sceneName }}</el-descriptions-item>
          <el-descriptions-item label="上一版本">{{ detailData.previousVersionNo || '首发版本' }}</el-descriptions-item>
          <el-descriptions-item label="发布时间">{{ parseTime(detailData.version.publishedTime) }}</el-descriptions-item>
          <el-descriptions-item label="发布人">{{ detailData.version.publishedBy }}</el-descriptions-item>
          <el-descriptions-item label="发布说明" :span="2">{{ detailData.version.publishDesc || '-' }}</el-descriptions-item>
          <el-descriptions-item label="检查结果">
            <el-tag :type="detailValidationMeta.tag">{{ detailValidationMeta.label }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="检查摘要">{{ buildValidationNote(detailData.validationResult) }}</el-descriptions-item>
        </el-descriptions>

        <div class="publish-audit__filter-row">
          <el-select v-model="detailFeeCode" clearable placeholder="按费用筛选快照对象" style="width: 280px" @change="reloadDetail">
            <el-option v-for="item in detailData.impactedFees || []" :key="item.feeCode" :label="`${item.feeName} / ${item.feeCode}`" :value="item.feeCode" />
          </el-select>
        </div>

        <el-tabs>
          <el-tab-pane label="发布检查">
            <div class="publish-audit__summary-grid">
              <div class="publish-audit__summary-card"><span>阻断项</span><strong>{{ detailValidationMeta.blockingCount }}</strong></div>
              <div class="publish-audit__summary-card"><span>告警项</span><strong>{{ detailValidationMeta.warningCount }}</strong></div>
              <div class="publish-audit__summary-card"><span>受影响费用</span><strong>{{ detailValidation.impactedFeeCount || (detailValidation.impactedFees || []).length || 0 }}</strong></div>
              <div class="publish-audit__summary-card"><span>发布资格</span><strong>{{ detailValidation.publishable === false ? '阻断' : '可发布' }}</strong></div>
            </div>
            <el-table v-if="detailValidation.items?.length" :data="detailValidation.items" size="small">
              <el-table-column label="级别" width="100" align="center">
                <template #default="scope">
                  <el-tag :type="resolveCheckLevelMeta(scope.row.level).type">{{ resolveCheckLevelMeta(scope.row.level).label }}</el-tag>
                </template>
              </el-table-column>
              <el-table-column label="检查项" prop="title" min-width="180" />
              <el-table-column label="说明" prop="message" min-width="360" :show-overflow-tooltip="true" />
            </el-table>
            <el-empty v-else description="该版本未记录检查明细或检查项为空" />
          </el-tab-pane>
          <el-tab-pane label="受影响费用">
            <el-table :data="detailData.impactedFees || []" size="small">
              <el-table-column label="费用编码" prop="feeCode" width="160" />
              <el-table-column label="费用名称" prop="feeName" min-width="180" />
              <el-table-column label="变化类型" width="120" align="center">
                <template #default="scope">
                  <el-tag :type="resolveCostChangeTypeMeta(scope.row.changeType).type">{{ resolveCostChangeTypeMeta(scope.row.changeType).label }}</el-tag>
                </template>
              </el-table-column>
              <el-table-column label="摘要" prop="summaryText" min-width="260" />
            </el-table>
          </el-tab-pane>
          <el-tab-pane label="快照对象">
            <el-descriptions :column="3" border>
              <el-descriptions-item label="场景">{{ detailData.snapshotCounts?.scene || 0 }}</el-descriptions-item>
              <el-descriptions-item label="费用">{{ detailData.snapshotCounts?.fee || 0 }}</el-descriptions-item>
              <el-descriptions-item label="变量">{{ detailData.snapshotCounts?.variable || 0 }}</el-descriptions-item>
              <el-descriptions-item label="规则">{{ detailData.snapshotCounts?.rule || 0 }}</el-descriptions-item>
              <el-descriptions-item label="条件">{{ detailData.snapshotCounts?.condition || 0 }}</el-descriptions-item>
              <el-descriptions-item label="阶梯">{{ detailData.snapshotCounts?.tier || 0 }}</el-descriptions-item>
            </el-descriptions>
            <el-collapse class="publish-audit__collapse">
              <el-collapse-item title="费用快照" name="fee"><JsonEditor :model-value="detailData.snapshotGroups?.fees || []" title="费用快照" readonly :rows="10" /></el-collapse-item>
              <el-collapse-item title="变量快照" name="variable"><JsonEditor :model-value="detailData.snapshotGroups?.variables || []" title="变量快照" readonly :rows="10" /></el-collapse-item>
              <el-collapse-item title="规则快照" name="rule"><JsonEditor :model-value="detailData.snapshotGroups?.rules || []" title="规则快照" readonly :rows="10" /></el-collapse-item>
            </el-collapse>
          </el-tab-pane>
        </el-tabs>
      </div>
    </el-drawer>

    <el-drawer v-model="diffOpen" title="版本差异" size="1320px" append-to-body>
      <div v-if="diffForm.toVersionId">
        <div class="publish-audit__filter-row">
          <el-select v-model="diffForm.fromVersionId" placeholder="请选择基准版本" style="width: 220px" @change="loadDiff">
            <el-option v-for="item in diffVersionOptions" :key="item.versionId" :label="item.versionNo" :value="item.versionId" />
          </el-select>
          <el-select v-model="diffFeeCode" clearable placeholder="按费用筛选差异" style="width: 260px" :disabled="!diffForm.fromVersionId" @change="loadDiff">
            <el-option v-for="item in diffData.feeDiffs || []" :key="item.feeCode" :label="`${item.feeName} / ${item.feeCode}`" :value="item.feeCode" />
          </el-select>
        </div>

        <div class="publish-audit__compare-head">
          <div class="publish-audit__compare-card">
            <span class="publish-audit__compare-label">基准版本</span>
            <strong>{{ diffData.fromVersion?.versionNo || '待选择' }}</strong>
            <small>{{ diffData.fromVersion?.publishDesc || '左侧基准版本由用户自行选择后开始对比' }}</small>
          </div>
          <div class="publish-audit__compare-arrow">VS</div>
          <div class="publish-audit__compare-card publish-audit__compare-card--target">
            <span class="publish-audit__compare-label">当前选中版本</span>
            <strong>{{ diffData.toVersion?.versionNo || '-' }}</strong>
            <small>{{ diffData.toVersion?.publishDesc || '当前从版本台账点击差异进入的版本' }}</small>
          </div>
        </div>

        <el-alert
          v-if="!diffForm.fromVersionId"
          title="右侧已锁定当前选中版本，请先在左侧选择一个基准版本，再查看差异结果。"
          type="info"
          :closable="false"
          class="publish-audit__diff-tip"
        />

        <template v-else>
        <el-alert
          v-if="!hasAnyDiff"
          title="当前两个版本的配置快照一致，没有场景、费用或规则差异；费用级筛选为空是正常结果。"
          type="success"
          :closable="false"
          class="publish-audit__diff-tip"
        />
        <el-descriptions :column="4" border>
          <el-descriptions-item label="场景级变化">{{ diffData.summary?.sceneChangeCount || 0 }}</el-descriptions-item>
          <el-descriptions-item label="费用变化">{{ diffData.summary?.feeChangeCount || 0 }}</el-descriptions-item>
          <el-descriptions-item label="规则变化">{{ diffData.summary?.ruleChangeCount || 0 }}</el-descriptions-item>
          <el-descriptions-item label="新增费用">{{ diffData.summary?.addedFeeCount || 0 }}</el-descriptions-item>
        </el-descriptions>

        <el-tabs>
          <el-tab-pane label="场景差异">
            <JsonDiffViewer
              title="场景主数据"
              subtitle="先看字段级差异摘要，再看场景快照的左右并排对比。"
              :left-title="diffData.fromVersion?.versionNo || '-'"
              :right-title="diffData.toVersion?.versionNo || '-'"
              :left-value="sceneDiffLeftValue"
              :right-value="sceneDiffRightValue"
              :rows="12"
            />
          </el-tab-pane>
          <el-tab-pane label="费用级差异">
            <el-table :data="diffData.feeDiffs || []" size="small" highlight-current-row row-key="feeCode" @current-change="handleFeeDiffRowChange">
              <el-table-column label="费用编码" prop="feeCode" width="150" />
              <el-table-column label="费用名称" prop="feeName" min-width="180" />
              <el-table-column label="变化类型" width="110" align="center">
                <template #default="scope">
                  <el-tag :type="resolveCostChangeTypeMeta(scope.row.changeType).type">{{ resolveCostChangeTypeMeta(scope.row.changeType).label }}</el-tag>
                </template>
              </el-table-column>
              <el-table-column label="规则变化数" prop="ruleChangeCount" width="120" />
              <el-table-column label="变量变化数" prop="variableChangeCount" width="120" />
              <el-table-column label="摘要" prop="summaryText" min-width="260" />
            </el-table>

            <div v-if="selectedFeeDiff" class="publish-audit__diff-detail">
              <JsonDiffViewer
                title="费用主数据"
                subtitle="对比费用定义本身的字段变化。"
                :left-title="diffData.fromVersion?.versionNo || '-'"
                :right-title="diffData.toVersion?.versionNo || '-'"
                :left-value="selectedFeeDiff.fromFee"
                :right-value="selectedFeeDiff.toFee"
                :rows="12"
              />

              <JsonDiffViewer
                title="关联规则"
                subtitle="对比费用下挂规则快照的整体变化。"
                :left-title="diffData.fromVersion?.versionNo || '-'"
                :right-title="diffData.toVersion?.versionNo || '-'"
                :left-value="selectedFeeDiff.fromRules"
                :right-value="selectedFeeDiff.toRules"
                :rows="14"
              />

              <JsonDiffViewer
                title="引用变量"
                subtitle="对比该费用关联变量的快照变化。"
                :left-title="diffData.fromVersion?.versionNo || '-'"
                :right-title="diffData.toVersion?.versionNo || '-'"
                :left-value="selectedFeeDiff.fromVariables"
                :right-value="selectedFeeDiff.toVariables"
                :rows="12"
              />
            </div>
          </el-tab-pane>
          <el-tab-pane label="规则级差异">
            <el-table :data="filteredRuleDiffs" size="small" highlight-current-row row-key="ruleCode" @current-change="handleRuleDiffRowChange">
              <el-table-column label="费用编码" prop="feeCode" width="140" />
              <el-table-column label="规则编码" prop="ruleCode" width="180" />
              <el-table-column label="规则名称" prop="ruleName" min-width="180" />
              <el-table-column label="变化类型" width="110" align="center">
                <template #default="scope">
                  <el-tag :type="resolveCostChangeTypeMeta(scope.row.changeType).type">{{ resolveCostChangeTypeMeta(scope.row.changeType).label }}</el-tag>
                </template>
              </el-table-column>
              <el-table-column label="条件变化数" prop="conditionChangeCount" width="120" />
              <el-table-column label="阶梯变化数" prop="tierChangeCount" width="120" />
              <el-table-column label="变更字段" min-width="220"><template #default="scope">{{ (scope.row.changedFields || []).join('、') || '-' }}</template></el-table-column>
            </el-table>

            <div v-if="selectedRuleDiff" class="publish-audit__diff-detail">
              <JsonDiffViewer
                title="规则主数据"
                subtitle="对比规则自身的字段变化。"
                :left-title="diffData.fromVersion?.versionNo || '-'"
                :right-title="diffData.toVersion?.versionNo || '-'"
                :left-value="selectedRuleDiff.fromRule"
                :right-value="selectedRuleDiff.toRule"
                :rows="12"
              />

              <JsonDiffViewer
                title="条件明细"
                subtitle="对比规则条件清单的变化。"
                :left-title="diffData.fromVersion?.versionNo || '-'"
                :right-title="diffData.toVersion?.versionNo || '-'"
                :left-value="selectedRuleDiff.fromConditions"
                :right-value="selectedRuleDiff.toConditions"
                :rows="14"
              />

              <JsonDiffViewer
                title="阶梯明细"
                subtitle="对比规则阶梯配置的变化。"
                :left-title="diffData.fromVersion?.versionNo || '-'"
                :right-title="diffData.toVersion?.versionNo || '-'"
                :left-value="selectedRuleDiff.fromTiers"
                :right-value="selectedRuleDiff.toTiers"
                :rows="14"
              />
            </div>
          </el-tab-pane>
        </el-tabs>
        </template>
      </div>
    </el-drawer>
  </div>
</template>

<script setup name="CostPublishAudit">
import JsonEditor from '@/components/cost/JsonEditor.vue'
import JsonDiffViewer from '@/components/cost/JsonDiffViewer.vue'
import { getPublishDiff, getPublishStats, getPublishVersion, listPublish } from '@/api/cost/publish'
import { optionselectScene } from '@/api/cost/scene'
import useSettingsStore from '@/store/modules/settings'
import { resolveWorkingCostSceneId } from '@/utils/costSceneContext'
import { useCostWorkSceneAutoRefresh } from '@/utils/costWorkSceneAutoRefresh'
import { COST_MENU_ROUTES } from '@/utils/costMenuRoutes'
import { resolveCheckLevelMeta, resolveCostChangeTypeMeta } from '@/utils/costDisplayLabels'
import { getRemoteDictOptionMap } from '@/utils/dictRemote'

const { proxy } = getCurrentInstance()
const route = useRoute()
const router = useRouter()
const settingsStore = useSettingsStore()
const isCompactMode = computed(() => settingsStore.costPageMode === 'COMPACT')

const loading = ref(false)
const showSearch = ref(true)
const total = ref(0)
const versionList = ref([])
const sceneOptions = ref([])
const businessDomainOptions = ref([])
const versionStatusOptions = ref([])
const stats = reactive({ sceneCount: 0, versionCount: 0, activeVersionCount: 0, rolledBackVersionCount: 0 })
const detailOpen = ref(false)
const detailData = ref({})
const detailFeeCode = ref(undefined)
const diffOpen = ref(false)
const diffData = ref({})
const diffVersionOptions = ref([])
const diffFeeCode = ref(undefined)
const selectedFeeDiffCode = ref(undefined)
const selectedRuleDiffCode = ref(undefined)

const queryParams = reactive({
  pageNum: 1,
  pageSize: 10,
  sceneId: route.query.sceneId ? Number(route.query.sceneId) : undefined,
  businessDomain: undefined,
  versionStatus: undefined,
  versionNo: undefined
})

const diffForm = reactive({
  fromVersionId: undefined,
  toVersionId: undefined
})

const metricItems = computed(() => [
  { label: '已发布场景', value: stats.sceneCount, desc: '当前筛选范围内已形成版本的场景数量' },
  { label: '版本总数', value: stats.versionCount, desc: '发布台账中的版本记录数量' },
  { label: '生效版本数', value: stats.activeVersionCount, desc: '当前处于生效中的版本数' },
  { label: '已回滚版本', value: stats.rolledBackVersionCount, desc: '历史上被回滚替换的版本数' }
])

const detailValidation = computed(() => parseValidationResult(detailData.value.validationResult))
const detailValidationMeta = computed(() => resolveValidationMeta(detailData.value.validationResult))
const hasAnyDiff = computed(() => {
  const summary = diffData.value.summary || {}
  return Number(summary.sceneChangeCount || 0) > 0
    || Number(summary.feeChangeCount || 0) > 0
    || Number(summary.ruleChangeCount || 0) > 0
})

const selectedFeeDiff = computed(() => (diffData.value.feeDiffs || []).find(item => item.feeCode === selectedFeeDiffCode.value))
const sceneDiffLeftValue = computed(() => diffData.value.fromScene ?? buildSceneSnapshotFromDiffs(diffData.value.sceneDiffs, 'fromValue'))
const sceneDiffRightValue = computed(() => diffData.value.toScene ?? buildSceneSnapshotFromDiffs(diffData.value.sceneDiffs, 'toValue'))

const filteredRuleDiffs = computed(() => {
  const rows = diffData.value.ruleDiffs || []
  if (!selectedFeeDiffCode.value) {
    return rows
  }
  return rows.filter(item => item.feeCode === selectedFeeDiffCode.value)
})

const selectedRuleDiff = computed(() => filteredRuleDiffs.value.find(item => item.ruleCode === selectedRuleDiffCode.value))

watch(diffOpen, open => {
  if (!open) {
    diffData.value = {}
    diffVersionOptions.value = []
    diffFeeCode.value = undefined
    diffForm.fromVersionId = undefined
    diffForm.toVersionId = undefined
    selectedFeeDiffCode.value = undefined
    selectedRuleDiffCode.value = undefined
  }
})

watch(filteredRuleDiffs, rows => {
  if (!rows.length) {
    selectedRuleDiffCode.value = undefined
    return
  }
  if (!rows.some(item => item.ruleCode === selectedRuleDiffCode.value)) {
    selectedRuleDiffCode.value = rows[0].ruleCode
  }
})

async function loadBaseOptions() {
  const [dictMap, sceneResponse] = await Promise.all([
    getRemoteDictOptionMap(['cost_business_domain', 'cost_publish_version_status']),
    optionselectScene({ status: '0', pageNum: 1, pageSize: 1000 })
  ])
  businessDomainOptions.value = dictMap.cost_business_domain || []
  versionStatusOptions.value = dictMap.cost_publish_version_status || []
  sceneOptions.value = sceneResponse?.data || []
  queryParams.sceneId = resolveWorkingCostSceneId(sceneOptions.value, queryParams.sceneId)
}

async function getList() {
  loading.value = true
  try {
    await loadBaseOptions()
    const [listResponse, statsResponse] = await Promise.all([listPublish(queryParams), getPublishStats(queryParams)])
    versionList.value = listResponse.rows || []
    total.value = listResponse.total || 0
    Object.assign(stats, statsResponse.data || {})
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

function handleBackToPublish() {
  router.push({ path: COST_MENU_ROUTES.publish, query: queryParams.sceneId ? { sceneId: queryParams.sceneId } : {} })
}

async function handleDetail(row) {
  detailFeeCode.value = undefined
  const response = await getPublishVersion(row.versionId)
  detailData.value = response.data || {}
  detailOpen.value = true
}

async function reloadDetail() {
  if (!detailData.value.version?.versionId) return
  const response = await getPublishVersion(detailData.value.version.versionId, { feeCode: detailFeeCode.value })
  detailData.value = response.data || {}
}

async function handleDiff(row) {
  diffData.value = {
    toVersion: row,
    fromVersion: undefined,
    summary: {},
    fromScene: undefined,
    toScene: undefined,
    sceneDiffs: [],
    feeDiffs: [],
    ruleDiffs: []
  }
  diffFeeCode.value = undefined
  selectedFeeDiffCode.value = undefined
  selectedRuleDiffCode.value = undefined
  diffForm.toVersionId = row.versionId
  const response = await listPublish({ sceneId: row.sceneId, pageNum: 1, pageSize: 1000 })
  diffVersionOptions.value = (response.rows || []).filter(item => item.versionId !== row.versionId)
  diffForm.fromVersionId = undefined
  diffOpen.value = true
}

async function loadDiff() {
  if (!diffForm.toVersionId) return
  if (!diffForm.fromVersionId) {
    diffData.value = {
      toVersion: diffData.value.toVersion,
      fromVersion: undefined,
      summary: {},
      fromScene: undefined,
      toScene: undefined,
      sceneDiffs: [],
      feeDiffs: [],
      ruleDiffs: []
    }
    selectedFeeDiffCode.value = undefined
    selectedRuleDiffCode.value = undefined
    return
  }
  const response = await getPublishDiff({ ...diffForm, feeCode: diffFeeCode.value })
  diffData.value = response.data || {}
  selectedFeeDiffCode.value = diffData.value.feeDiffs?.[0]?.feeCode
  selectedRuleDiffCode.value = diffData.value.ruleDiffs?.[0]?.ruleCode
}

function handleFeeDiffRowChange(row) {
  selectedFeeDiffCode.value = row?.feeCode
}

function handleRuleDiffRowChange(row) {
  selectedRuleDiffCode.value = row?.ruleCode
}

function buildSceneSnapshotFromDiffs(rows = [], valueKey) {
  if (!Array.isArray(rows) || !rows.length) {
    return {}
  }
  return rows.reduce((result, item) => {
    if (item?.field) {
      result[item.field] = item?.[valueKey]
    }
    return result
  }, {})
}

function resolveVersionLabel(value) {
  return versionStatusOptions.value.find(item => item.value === value)?.label || value
}

function parseValidationResult(value) {
  if (!value) {
    return {}
  }
  if (typeof value === 'string') {
    try {
      return JSON.parse(value)
    } catch {
      return {}
    }
  }
  return value
}

function resolveValidationMeta(value) {
  const payload = parseValidationResult(value?.validationResultJson || value)
  const items = Array.isArray(payload.items) ? payload.items : []
  const blockingCount = Number(payload.blockingCount ?? items.filter(item => item.level === 'BLOCK').length ?? 0)
  const warningCount = Number(payload.warningCount ?? items.filter(item => item.level === 'WARN').length ?? 0)
  if (!Object.keys(payload).length) {
    return { label: '未留痕', tag: 'info', blockingCount: 0, warningCount: 0 }
  }
  if (blockingCount > 0 || payload.publishable === false) {
    return { label: '阻断', tag: 'danger', blockingCount, warningCount }
  }
  if (warningCount > 0) {
    return { label: '告警通过', tag: 'warning', blockingCount, warningCount }
  }
  return { label: '通过', tag: 'success', blockingCount, warningCount }
}

function buildValidationNote(value) {
  const meta = resolveValidationMeta(value)
  if (meta.label === '未留痕') {
    return '未记录检查快照'
  }
  return `阻断 ${meta.blockingCount} / 告警 ${meta.warningCount}`
}

useCostWorkSceneAutoRefresh({
  queryParams,
  sceneOptions,
  beforeRefresh: () => {
    detailOpen.value = false
    diffOpen.value = false
  },
  refresh: getList
})

onMounted(() => {
  getList()
})
</script>

<style lang="scss" scoped>
.publish-audit {
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

  &__hero-actions {
    display: flex;
    align-items: center;
    gap: 12px;
    flex-wrap: wrap;
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

  &__summary-grid {
    display: grid;
    grid-template-columns: repeat(2, minmax(0, 1fr));
    gap: 10px;
    margin-bottom: 16px;
  }

  &__summary-card {
    display: grid;
    gap: 6px;
    padding: 12px 14px;
    border-radius: 12px;
    border: 1px solid #dbe6f4;
    background: #f8fbff;

    strong {
      color: #3c8cff;
      font-size: 24px;
    }

    span {
      color: #7c8798;
    }
  }

  &__validation-note {
    margin-top: 6px;
    font-size: 12px;
    line-height: 1.4;
    color: #64748b;
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

  &__filter-row {
    display: flex;
    gap: 12px;
    align-items: center;
    margin: 16px 0;
    flex-wrap: wrap;
  }

  &__collapse pre {
    margin: 0;
    max-height: 360px;
    overflow: auto;
    padding: 12px;
    border-radius: 12px;
    background: #0f172a;
    color: #dbeafe;
    white-space: pre-wrap;
    word-break: break-word;
  }

  &__compare-head {
    display: flex;
    align-items: stretch;
    justify-content: space-between;
    gap: 16px;
    margin: 16px 0;
  }

  &__compare-card {
    flex: 1;
    border: 1px solid #dbe6f4;
    border-radius: 18px;
    padding: 18px 20px;
    background: linear-gradient(135deg, #f8fbff, #ffffff);
    display: flex;
    flex-direction: column;
    gap: 8px;

    strong {
      color: #10233e;
      font-size: 22px;
    }

    small,
    span {
      color: #64748b;
    }
  }

  &__compare-card--target {
    border-color: #bfd7ff;
    background: linear-gradient(135deg, #eef5ff, #ffffff);
  }

  &__compare-arrow {
    display: flex;
    align-items: center;
    font-size: 22px;
    color: #3b82f6;
    font-weight: 700;
  }

  &__diff-detail {
    display: flex;
    flex-direction: column;
    gap: 16px;
    margin-top: 16px;
  }

}

@media (max-width: 1200px) {
  .publish-audit {
    &__metrics,
    &__compare-head {
      grid-template-columns: 1fr;
    }

    &__compare-arrow {
      justify-content: center;
    }
  }
}
</style>

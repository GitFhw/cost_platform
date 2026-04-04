
<template>
  <div class="app-container publish-center">
    <section class="publish-center__hero">
      <div>
        <div class="publish-center__eyebrow">发布治理</div>
        <h2 class="publish-center__title">发布中心</h2>
        <p class="publish-center__subtitle">
          统一管理场景版本发布、生效切换、差异对比和历史版本台账，确保运行始终基于明确版本执行。
        </p>
      </div>
      <el-tag type="success">支持发布检查、版本台账、生效切换、回退和差异对比</el-tag>
    </section>

    <section class="publish-center__metrics">
      <div v-for="item in metricItems" :key="item.label" class="publish-center__metric-card">
        <span>{{ item.label }}</span>
        <strong>{{ item.value }}</strong>
        <small>{{ item.desc }}</small>
      </div>
    </section>

    <el-form ref="queryRef" :model="queryParams" :inline="true" label-width="84px" v-show="showSearch">
      <el-form-item label="所属场景" prop="sceneId">
        <el-select v-model="queryParams.sceneId" clearable filterable placeholder="请选择场景" style="width: 240px" @change="handleQuerySceneChange">
          <el-option v-for="item in sceneOptions" :key="item.sceneId" :label="`${item.sceneCode} / ${item.sceneName}`" :value="item.sceneId" />
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

    <section class="publish-center__workspace">
      <div class="publish-center__precheck">
        <div class="publish-center__section-head">
          <div>
            <h3>发布前检查</h3>
            <p>先做阻断校验，再生成版本。当前场景首发时建议直接设为生效。</p>
          </div>
          <right-toolbar v-model:showSearch="showSearch" @queryTable="getList" />
        </div>

        <el-form :model="publishForm" label-width="92px">
          <el-form-item label="发布场景" required>
            <el-select v-model="publishForm.sceneId" filterable placeholder="请选择要发布的场景" style="width: 100%" @change="handlePublishSceneChange">
              <el-option v-for="item in sceneOptions" :key="item.sceneId" :label="`${item.sceneCode} / ${item.sceneName}`" :value="item.sceneId" />
            </el-select>
          </el-form-item>
          <el-form-item label="发布说明" required>
            <el-input v-model="publishForm.publishDesc" type="textarea" :rows="3" maxlength="1000" show-word-limit placeholder="请输入本次发布说明、影响范围与业务口径摘要" />
          </el-form-item>
          <el-form-item label="发布动作">
            <el-checkbox v-model="publishForm.activateNow">生成版本后立即设为生效</el-checkbox>
          </el-form-item>
        </el-form>

        <div class="publish-center__action-row">
          <el-button type="primary" icon="CircleCheck" @click="handlePrecheck">发布前检查</el-button>
          <el-button type="success" icon="Promotion" @click="handlePublish" v-hasPermi="['cost:publish:add']">生成版本</el-button>
        </div>

        <div v-if="precheck.sceneId" class="publish-center__precheck-summary">
          <div class="publish-center__summary-card"><span>阻断项</span><strong>{{ precheck.blockingCount }}</strong></div>
          <div class="publish-center__summary-card"><span>提示项</span><strong>{{ precheck.warningCount }}</strong></div>
          <div class="publish-center__summary-card"><span>受影响费用</span><strong>{{ precheck.impactedFeeCount }}</strong></div>
          <div class="publish-center__summary-card"><span>当前生效版本</span><strong>{{ precheck.activeVersionNo || '暂无' }}</strong></div>
        </div>

        <el-table v-if="precheck.items?.length" :data="precheck.items" size="small">
          <el-table-column label="级别" width="100" align="center">
            <template #default="scope">
              <el-tag :type="resolveCheckTag(scope.row.level)">{{ scope.row.level }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="检查项" prop="title" min-width="160" />
          <el-table-column label="说明" prop="message" min-width="320" :show-overflow-tooltip="true" />
        </el-table>

        <div v-if="precheck.impactedFees?.length" class="publish-center__impact">
          <h4>本次受影响费用</h4>
          <div class="publish-center__impact-list">
            <div v-for="item in precheck.impactedFees" :key="item.feeCode" class="publish-center__impact-item">
              <strong>{{ item.feeName }}</strong>
              <span>{{ item.feeCode }}</span>
              <small>{{ item.summaryText }}</small>
            </div>
          </div>
        </div>
      </div>

      <div class="publish-center__ledger">
        <div class="publish-center__section-head">
          <div>
            <h3>版本台账</h3>
            <p>查看版本详情、快照对象、差异对比、生效切换与回滚。</p>
          </div>
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
          <el-table-column label="发布说明" prop="publishDesc" min-width="220" :show-overflow-tooltip="true" />
          <el-table-column label="发布人" prop="publishedBy" width="120" align="center" />
          <el-table-column label="发布时间" width="180" align="center">
            <template #default="scope">{{ parseTime(scope.row.publishedTime) }}</template>
          </el-table-column>
          <el-table-column label="操作" width="320" fixed="right" align="center">
            <template #default="scope">
              <el-button link type="primary" icon="View" @click="handleDetail(scope.row)">详情</el-button>
              <el-button link type="primary" icon="Tickets" @click="handleDiff(scope.row)">差异</el-button>
              <el-button link type="primary" icon="Select" @click="handleActivate(scope.row)" v-hasPermi="['cost:publish:activate']">设为生效</el-button>
              <el-button link type="warning" icon="RefreshLeft" @click="handleRollback(scope.row)" v-hasPermi="['cost:publish:rollback']">回滚</el-button>
            </template>
          </el-table-column>
        </el-table>

        <pagination v-show="total > 0" :total="total" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" @pagination="getList" />
      </div>
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
        </el-descriptions>

        <div class="publish-center__filter-row">
          <el-select v-model="detailFeeCode" clearable placeholder="按费用筛选快照对象" style="width: 280px" @change="reloadDetail">
            <el-option v-for="item in detailData.impactedFees || []" :key="item.feeCode" :label="`${item.feeCode} / ${item.feeName}`" :value="item.feeCode" />
          </el-select>
        </div>

        <el-tabs>
          <el-tab-pane label="受影响费用">
            <el-table :data="detailData.impactedFees || []" size="small">
              <el-table-column label="费用编码" prop="feeCode" width="160" />
              <el-table-column label="费用名称" prop="feeName" min-width="180" />
              <el-table-column label="变化类型" prop="changeType" width="120" />
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
            <el-collapse class="publish-center__collapse">
              <el-collapse-item title="费用快照" name="fee"><pre>{{ JSON.stringify(detailData.snapshotGroups?.fees || [], null, 2) }}</pre></el-collapse-item>
              <el-collapse-item title="变量快照" name="variable"><pre>{{ JSON.stringify(detailData.snapshotGroups?.variables || [], null, 2) }}</pre></el-collapse-item>
              <el-collapse-item title="规则快照" name="rule"><pre>{{ JSON.stringify(detailData.snapshotGroups?.rules || [], null, 2) }}</pre></el-collapse-item>
            </el-collapse>
          </el-tab-pane>
        </el-tabs>
      </div>
    </el-drawer>
    <el-drawer v-model="diffOpen" title="版本差异" size="1320px" append-to-body>
      <div v-if="diffForm.toVersionId">
        <div class="publish-center__filter-row">
          <el-select v-model="diffForm.fromVersionId" placeholder="基准版本" style="width: 220px" @change="loadDiff">
            <el-option v-for="item in diffVersionOptions" :key="item.versionId" :label="item.versionNo" :value="item.versionId" />
          </el-select>
          <el-select v-model="diffFeeCode" clearable placeholder="按费用筛选差异" style="width: 260px" @change="loadDiff">
            <el-option v-for="item in diffData.feeDiffs || []" :key="item.feeCode" :label="`${item.feeCode} / ${item.feeName}`" :value="item.feeCode" />
          </el-select>
        </div>

        <div class="publish-center__compare-head">
          <div class="publish-center__compare-card">
            <span class="publish-center__compare-label">基准版本</span>
            <strong>{{ diffData.fromVersion?.versionNo || '-' }}</strong>
            <small>{{ diffData.fromVersion?.publishDesc || '默认带出上一个版本，可手动更换' }}</small>
          </div>
          <div class="publish-center__compare-arrow">VS</div>
          <div class="publish-center__compare-card publish-center__compare-card--target">
            <span class="publish-center__compare-label">当前选中版本</span>
            <strong>{{ diffData.toVersion?.versionNo || '-' }}</strong>
            <small>{{ diffData.toVersion?.publishDesc || '当前从版本台账点击差异进入的版本' }}</small>
          </div>
        </div>

        <el-descriptions :column="4" border>
          <el-descriptions-item label="场景级变化">{{ diffData.summary?.sceneChangeCount || 0 }}</el-descriptions-item>
          <el-descriptions-item label="费用变化">{{ diffData.summary?.feeChangeCount || 0 }}</el-descriptions-item>
          <el-descriptions-item label="规则变化">{{ diffData.summary?.ruleChangeCount || 0 }}</el-descriptions-item>
          <el-descriptions-item label="新增费用">{{ diffData.summary?.addedFeeCount || 0 }}</el-descriptions-item>
        </el-descriptions>

        <el-tabs>
          <el-tab-pane label="场景差异">
            <el-table :data="diffData.sceneDiffs || []" size="small">
              <el-table-column label="字段" prop="fieldLabel" width="160" />
              <el-table-column label="旧值" prop="fromValue" min-width="200" />
              <el-table-column label="新值" prop="toValue" min-width="200" />
            </el-table>
          </el-tab-pane>
          <el-tab-pane label="费用级差异">
            <el-alert :title="selectedFeeDiff ? buildFeeDiffNarrative(selectedFeeDiff) : '请先在下方列表选择一条费用差异，下面会按左右两个版本并排展示。'" type="info" :closable="false" class="publish-center__diff-tip" />
            <el-table :data="diffData.feeDiffs || []" size="small" highlight-current-row row-key="feeCode" @current-change="handleFeeDiffRowChange">
              <el-table-column label="费用编码" prop="feeCode" width="150" />
              <el-table-column label="费用名称" prop="feeName" min-width="180" />
              <el-table-column label="变化类型" prop="changeType" width="110" />
              <el-table-column label="规则变化数" prop="ruleChangeCount" width="120" />
              <el-table-column label="变量变化数" prop="variableChangeCount" width="120" />
              <el-table-column label="摘要" prop="summaryText" min-width="260" />
            </el-table>

            <div v-if="selectedFeeDiff" class="publish-center__diff-detail">
              <div class="publish-center__bc-section">
                <div class="publish-center__bc-title">费用主数据 - 费用主数据</div>
                <div class="publish-center__bc-header">
                  <span>{{ diffData.fromVersion?.versionNo || '-' }}</span>
                  <span>{{ diffData.toVersion?.versionNo || '-' }}</span>
                </div>
                <div class="publish-center__bc-body">
                  <div v-for="(row, index) in buildCompareRows(selectedFeeDiff.fromFee, selectedFeeDiff.toFee)" :key="`fee-main-${index}`" class="publish-center__bc-row">
                    <div class="publish-center__bc-cell" :class="row.same ? 'publish-center__bc-cell--same' : 'publish-center__bc-cell--diff'"><pre class="publish-center__bc-line">{{ row.left }}</pre></div>
                    <div class="publish-center__bc-cell" :class="row.same ? 'publish-center__bc-cell--same' : 'publish-center__bc-cell--diff'"><pre class="publish-center__bc-line">{{ row.right }}</pre></div>
                  </div>
                </div>
              </div>

              <div class="publish-center__bc-section">
                <div class="publish-center__bc-title">关联规则 - 关联规则</div>
                <div class="publish-center__bc-header">
                  <span>{{ diffData.fromVersion?.versionNo || '-' }}</span>
                  <span>{{ diffData.toVersion?.versionNo || '-' }}</span>
                </div>
                <div class="publish-center__bc-body">
                  <div v-for="(row, index) in buildCompareRows(selectedFeeDiff.fromRules, selectedFeeDiff.toRules)" :key="`fee-rule-${index}`" class="publish-center__bc-row">
                    <div class="publish-center__bc-cell" :class="row.same ? 'publish-center__bc-cell--same' : 'publish-center__bc-cell--diff'"><pre class="publish-center__bc-line">{{ row.left }}</pre></div>
                    <div class="publish-center__bc-cell" :class="row.same ? 'publish-center__bc-cell--same' : 'publish-center__bc-cell--diff'"><pre class="publish-center__bc-line">{{ row.right }}</pre></div>
                  </div>
                </div>
              </div>

              <div class="publish-center__bc-section">
                <div class="publish-center__bc-title">引用变量 - 引用变量</div>
                <div class="publish-center__bc-header">
                  <span>{{ diffData.fromVersion?.versionNo || '-' }}</span>
                  <span>{{ diffData.toVersion?.versionNo || '-' }}</span>
                </div>
                <div class="publish-center__bc-body">
                  <div v-for="(row, index) in buildCompareRows(selectedFeeDiff.fromVariables, selectedFeeDiff.toVariables)" :key="`fee-variable-${index}`" class="publish-center__bc-row">
                    <div class="publish-center__bc-cell" :class="row.same ? 'publish-center__bc-cell--same' : 'publish-center__bc-cell--diff'"><pre class="publish-center__bc-line">{{ row.left }}</pre></div>
                    <div class="publish-center__bc-cell" :class="row.same ? 'publish-center__bc-cell--same' : 'publish-center__bc-cell--diff'"><pre class="publish-center__bc-line">{{ row.right }}</pre></div>
                  </div>
                </div>
              </div>
            </div>
          </el-tab-pane>
          <el-tab-pane label="规则级差异">
            <el-alert :title="selectedRuleDiff ? buildRuleDiffNarrative(selectedRuleDiff) : '请先选择一条规则差异，下面会按规则主数据、条件明细、阶梯明细左右对齐展示。'" type="info" :closable="false" class="publish-center__diff-tip" />
            <el-table :data="filteredRuleDiffs" size="small" highlight-current-row row-key="ruleCode" @current-change="handleRuleDiffRowChange">
              <el-table-column label="费用编码" prop="feeCode" width="140" />
              <el-table-column label="规则编码" prop="ruleCode" width="180" />
              <el-table-column label="规则名称" prop="ruleName" min-width="180" />
              <el-table-column label="变化类型" prop="changeType" width="110" />
              <el-table-column label="条件变化数" prop="conditionChangeCount" width="120" />
              <el-table-column label="阶梯变化数" prop="tierChangeCount" width="120" />
              <el-table-column label="变更字段" min-width="220"><template #default="scope">{{ (scope.row.changedFields || []).join('、') || '-' }}</template></el-table-column>
            </el-table>

            <div v-if="selectedRuleDiff" class="publish-center__diff-detail">
              <div class="publish-center__explain-panel">
                <div class="publish-center__explain-title">中文差异解释</div>
                <div class="publish-center__explain-list">
                  <div v-for="(line, index) in buildRuleExplainLines(selectedRuleDiff)" :key="`rule-explain-${index}`" class="publish-center__explain-item">
                    {{ line }}
                  </div>
                </div>
              </div>

              <div class="publish-center__bc-section">
                <div class="publish-center__bc-title">规则主数据 - 规则主数据</div>
                <div class="publish-center__bc-header"><span>{{ diffData.fromVersion?.versionNo || '-' }}</span><span>{{ diffData.toVersion?.versionNo || '-' }}</span></div>
                <div class="publish-center__bc-body">
                  <div v-for="(row, index) in buildCompareRows(selectedRuleDiff.fromRule, selectedRuleDiff.toRule)" :key="`rule-main-${index}`" class="publish-center__bc-row">
                    <div class="publish-center__bc-cell" :class="row.same ? 'publish-center__bc-cell--same' : 'publish-center__bc-cell--diff'"><pre class="publish-center__bc-line">{{ row.left }}</pre></div>
                    <div class="publish-center__bc-cell" :class="row.same ? 'publish-center__bc-cell--same' : 'publish-center__bc-cell--diff'"><pre class="publish-center__bc-line">{{ row.right }}</pre></div>
                  </div>
                </div>
              </div>

              <div class="publish-center__bc-section">
                <div class="publish-center__bc-title">条件明细 - 条件明细</div>
                <div class="publish-center__bc-header"><span>{{ diffData.fromVersion?.versionNo || '-' }}</span><span>{{ diffData.toVersion?.versionNo || '-' }}</span></div>
                <div class="publish-center__bc-body">
                  <div v-for="(row, index) in buildCompareRows(selectedRuleDiff.fromConditions, selectedRuleDiff.toConditions)" :key="`rule-condition-${index}`" class="publish-center__bc-row">
                    <div class="publish-center__bc-cell" :class="row.same ? 'publish-center__bc-cell--same' : 'publish-center__bc-cell--diff'"><pre class="publish-center__bc-line">{{ row.left }}</pre></div>
                    <div class="publish-center__bc-cell" :class="row.same ? 'publish-center__bc-cell--same' : 'publish-center__bc-cell--diff'"><pre class="publish-center__bc-line">{{ row.right }}</pre></div>
                  </div>
                </div>
              </div>

              <div class="publish-center__bc-section">
                <div class="publish-center__bc-title">阶梯明细 - 阶梯明细</div>
                <div class="publish-center__bc-header"><span>{{ diffData.fromVersion?.versionNo || '-' }}</span><span>{{ diffData.toVersion?.versionNo || '-' }}</span></div>
                <div class="publish-center__bc-body">
                  <div v-for="(row, index) in buildCompareRows(selectedRuleDiff.fromTiers, selectedRuleDiff.toTiers)" :key="`rule-tier-${index}`" class="publish-center__bc-row">
                    <div class="publish-center__bc-cell" :class="row.same ? 'publish-center__bc-cell--same' : 'publish-center__bc-cell--diff'"><pre class="publish-center__bc-line">{{ row.left }}</pre></div>
                    <div class="publish-center__bc-cell" :class="row.same ? 'publish-center__bc-cell--same' : 'publish-center__bc-cell--diff'"><pre class="publish-center__bc-line">{{ row.right }}</pre></div>
                  </div>
                </div>
              </div>
            </div>
          </el-tab-pane>
        </el-tabs>
      </div>
    </el-drawer>
  </div>
</template>

<script setup name="CostPublish">
import { ElMessageBox } from 'element-plus'
import { activatePublishVersion, addPublishVersion, getPublishDiff, getPublishPrecheck, getPublishStats, getPublishVersion, listPublish, rollbackPublishVersion } from '@/api/cost/publish'
import { optionselectScene } from '@/api/cost/scene'
import { resolveWorkingCostSceneId } from '@/utils/costSceneContext'
import { getRemoteDictOptionMap } from '@/utils/dictRemote'

const { proxy } = getCurrentInstance()
const route = useRoute()

const loading = ref(false)
const showSearch = ref(true)
const total = ref(0)
const versionList = ref([])
const sceneOptions = ref([])
const businessDomainOptions = ref([])
const versionStatusOptions = ref([])
const stats = reactive({ sceneCount: 0, versionCount: 0, activeVersionCount: 0, rolledBackVersionCount: 0 })
const precheck = ref({ items: [], impactedFees: [] })
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

const publishForm = reactive({
  sceneId: route.query.sceneId ? Number(route.query.sceneId) : undefined,
  publishDesc: '',
  activateNow: false
})

const diffForm = reactive({
  fromVersionId: undefined,
  toVersionId: undefined
})

const metricItems = computed(() => [
  { label: '已发布场景', value: stats.sceneCount, desc: '当前筛选范围内已形成版本的场景数量' },
  { label: '版本总数', value: stats.versionCount, desc: '发布台账中的版本记录数量' },
  { label: '生效版本数', value: stats.activeVersionCount, desc: '当前处于 ACTIVE 的版本数' },
  { label: '已回滚版本', value: stats.rolledBackVersionCount, desc: '历史上被回滚替换的版本数' }
])
const selectedFeeDiff = computed(() => {
  return (diffData.value.feeDiffs || []).find(item => item.feeCode === selectedFeeDiffCode.value)
})

const filteredRuleDiffs = computed(() => {
  const rows = diffData.value.ruleDiffs || []
  if (!selectedFeeDiffCode.value) {
    return rows
  }
  return rows.filter(item => item.feeCode === selectedFeeDiffCode.value)
})

const selectedRuleDiff = computed(() => {
  return filteredRuleDiffs.value.find(item => item.ruleCode === selectedRuleDiffCode.value)
})

watch(diffOpen, (open) => {
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

watch(filteredRuleDiffs, (rows) => {
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
  publishForm.sceneId = resolveWorkingCostSceneId(sceneOptions.value, publishForm.sceneId, queryParams.sceneId)
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

function handleQuerySceneChange(sceneId) {
  queryParams.sceneId = sceneId
  publishForm.sceneId = sceneId
}

function handlePublishSceneChange(sceneId) {
  publishForm.sceneId = sceneId
  queryParams.sceneId = sceneId
}

async function handlePrecheck() {
  if (!publishForm.sceneId) {
    proxy.$modal.msgWarning('请先选择要发布的场景')
    return
  }
  const response = await getPublishPrecheck(publishForm.sceneId)
  precheck.value = response.data || { items: [], impactedFees: [] }
  if (precheck.value.suggestActivateNow) {
    publishForm.activateNow = true
  }
}

async function handlePublish() {
  if (!publishForm.sceneId || !publishForm.publishDesc) {
    proxy.$modal.msgWarning('请先选择发布场景并填写发布说明')
    return
  }
  await handlePrecheck()
  if (!precheck.value.publishable) {
    proxy.$modal.msgWarning('当前仍存在阻断项，请先处理后再发布')
    return
  }
  await addPublishVersion({ ...publishForm })
  proxy.$modal.msgSuccess('发布版本生成成功')
  getList()
  handlePrecheck()
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
  diffData.value = {}
  diffFeeCode.value = undefined
  selectedFeeDiffCode.value = undefined
  selectedRuleDiffCode.value = undefined
  diffForm.toVersionId = row.versionId
  const response = await listPublish({ sceneId: row.sceneId, pageNum: 1, pageSize: 1000 })
  diffVersionOptions.value = (response.rows || []).filter(item => item.versionId !== row.versionId)
  diffForm.fromVersionId = row.previousVersionId || diffVersionOptions.value[0]?.versionId
  diffOpen.value = true
  if (diffForm.fromVersionId) {
    await loadDiff()
  }
}

async function loadDiff() {
  if (!diffForm.fromVersionId || !diffForm.toVersionId) return
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

async function handleActivate(row) {
  await ElMessageBox.confirm(`确认将版本 ${row.versionNo} 设为当前生效版本吗？`, '生效切换', { type: 'warning' })
  await activatePublishVersion(row.versionId)
  proxy.$modal.msgSuccess('生效切换成功')
  getList()
}

async function handleRollback(row) {
  await ElMessageBox.confirm(`确认将场景回滚到版本 ${row.versionNo} 吗？`, '版本回滚', { type: 'warning' })
  await rollbackPublishVersion(row.versionId)
  proxy.$modal.msgSuccess('回滚成功')
  getList()
}

function resolveVersionLabel(value) {
  return versionStatusOptions.value.find(item => item.value === value)?.label || value
}

function resolveCheckTag(level) {
  return level === 'BLOCK' ? 'danger' : (level === 'WARN' ? 'warning' : 'success')
}

function normalizeCompareValue(value) {
  if (value === null || value === undefined || value === '') {
    return '暂无数据'
  }
  if (typeof value === 'string') {
    const trimmed = value.trim()
    if (!trimmed) {
      return '暂无数据'
    }
    try {
      return JSON.stringify(JSON.parse(trimmed), null, 2)
    } catch {
      return trimmed
    }
  }
  return JSON.stringify(value, null, 2)
}

function buildCompareRows(leftValue, rightValue) {
  const leftLines = normalizeCompareValue(leftValue).split('\n')
  const rightLines = normalizeCompareValue(rightValue).split('\n')
  const leftLength = leftLines.length
  const rightLength = rightLines.length
  const lcs = Array.from({ length: leftLength + 1 }, () => Array(rightLength + 1).fill(0))

  for (let leftIndex = leftLength - 1; leftIndex >= 0; leftIndex--) {
    for (let rightIndex = rightLength - 1; rightIndex >= 0; rightIndex--) {
      if (leftLines[leftIndex] === rightLines[rightIndex]) {
        lcs[leftIndex][rightIndex] = lcs[leftIndex + 1][rightIndex + 1] + 1
      } else {
        lcs[leftIndex][rightIndex] = Math.max(lcs[leftIndex + 1][rightIndex], lcs[leftIndex][rightIndex + 1])
      }
    }
  }

  const rows = []
  let leftIndex = 0
  let rightIndex = 0
  while (leftIndex < leftLength && rightIndex < rightLength) {
    if (leftLines[leftIndex] === rightLines[rightIndex]) {
      rows.push({ left: leftLines[leftIndex], right: rightLines[rightIndex], same: true })
      leftIndex++
      rightIndex++
      continue
    }
    if (lcs[leftIndex + 1][rightIndex] >= lcs[leftIndex][rightIndex + 1]) {
      rows.push({ left: leftLines[leftIndex], right: '', same: false })
      leftIndex++
    } else {
      rows.push({ left: '', right: rightLines[rightIndex], same: false })
      rightIndex++
    }
  }
  while (leftIndex < leftLength) {
    rows.push({ left: leftLines[leftIndex], right: '', same: false })
    leftIndex++
  }
  while (rightIndex < rightLength) {
    rows.push({ left: '', right: rightLines[rightIndex], same: false })
    rightIndex++
  }
  return rows
}

function buildFeeDiffNarrative(item) {
  return `费用 ${item.feeName || item.feeCode} 在两个发布版本之间发生 ${item.changeType || '差异'}，规则变化 ${item.ruleChangeCount || 0} 处，变量变化 ${item.variableChangeCount || 0} 处。`
}

function buildRuleDiffNarrative(item) {
  const changedFields = (item.changedFields || []).join('、') || '无字段摘要'
  return `规则 ${item.ruleName || item.ruleCode} 在两个发布版本之间发生 ${item.changeType || '差异'}，条件变化 ${item.conditionChangeCount || 0} 处，阶梯变化 ${item.tierChangeCount || 0} 处，主要涉及 ${changedFields}。`
}

function buildRuleExplainLines(item) {
  const lines = []
  const fromRule = item.fromRule || {}
  const toRule = item.toRule || {}
  const fromConditions = item.fromConditions || []
  const toConditions = item.toConditions || []
  const fromTiers = item.fromTiers || []
  const toTiers = item.toTiers || []

  const businessNarrative = buildRuleBusinessNarrative(fromRule, toRule, fromConditions, toConditions, fromTiers, toTiers)
  if (businessNarrative) {
    lines.push(businessNarrative)
  }

  const ruleTypeChanged = stringifyValue(fromRule.ruleType) !== stringifyValue(toRule.ruleType)
  if (ruleTypeChanged) {
    lines.push(`规则类型由“${stringifyValue(fromRule.ruleType)}”调整为“${stringifyValue(toRule.ruleType)}”。`)
  }

  const priorityChanged = stringifyValue(fromRule.priority) !== stringifyValue(toRule.priority)
  if (priorityChanged) {
    lines.push(`优先级由“${stringifyValue(fromRule.priority)}”调整为“${stringifyValue(toRule.priority)}”。`)
  }

  const pricingLines = buildPricingExplainLines(fromRule.pricingJson || {}, toRule.pricingJson || {})
  lines.push(...pricingLines)

  const conditionLines = buildConditionExplainLines(fromConditions, toConditions)
  lines.push(...conditionLines)

  const tierLines = buildTierExplainLines(fromTiers, toTiers)
  lines.push(...tierLines)

  if (!lines.length) {
    lines.push('该规则存在结构变化，但当前无法归纳出更细的中文解释，请以下方左右对照为准。')
  }
  return lines
}

function buildRuleBusinessNarrative(fromRule, toRule, fromConditions, toConditions, fromTiers, toTiers) {
  const parts = []
  const fromScope = buildConditionScopeText(fromConditions)
  const toScope = buildConditionScopeText(toConditions)
  if (fromScope !== toScope) {
    if (fromScope && toScope) {
      parts.push(`命中条件由“${fromScope}”调整为“${toScope}”`)
    } else if (fromScope && !toScope) {
      parts.push(`命中条件由“${fromScope}”调整为“无条件命中”`)
    } else if (!fromScope && toScope) {
      parts.push(`命中条件由“无条件命中”调整为“${toScope}”`)
    }
  }

  const pricingChangeText = buildPricingBusinessText(fromRule.pricingJson || {}, toRule.pricingJson || {})
  if (pricingChangeText) {
    parts.push(pricingChangeText)
  }

  const tierChangeText = buildTierBusinessText(fromTiers, toTiers)
  if (tierChangeText) {
    parts.push(tierChangeText)
  }

  if (!parts.length) {
    return ''
  }
  return `业务解释：${parts.join('；')}。`
}

function buildPricingExplainLines(fromPricing, toPricing) {
  const lines = []
  const fields = [
    ['mode', '计价模式'],
    ['unit', '计价单位'],
    ['basis', '计价依据'],
    ['summary', '计价说明'],
    ['rateValue', '费率值'],
    ['unitPrice', '单价']
  ]
  fields.forEach(([field, label]) => {
    if (stringifyValue(fromPricing[field]) !== stringifyValue(toPricing[field])) {
      lines.push(`${label}由“${stringifyValue(fromPricing[field])}”调整为“${stringifyValue(toPricing[field])}”。`)
    }
  })
  return lines
}

function buildPricingBusinessText(fromPricing, toPricing) {
  const unit = stringifyMeasureUnit(toPricing.unit || fromPricing.unit)
  if (stringifyValue(fromPricing.unitPrice) !== stringifyValue(toPricing.unitPrice)) {
    return `单价由“${formatMeasureValue(fromPricing.unitPrice, unit)}”调整为“${formatMeasureValue(toPricing.unitPrice, unit)}”`
  }
  if (stringifyValue(fromPricing.rateValue) !== stringifyValue(toPricing.rateValue)) {
    return `费率由“${formatMeasureValue(fromPricing.rateValue, unit)}”调整为“${formatMeasureValue(toPricing.rateValue, unit)}”`
  }
  if (stringifyValue(fromPricing.summary) !== stringifyValue(toPricing.summary)) {
    return `计价说明由“${stringifyValue(fromPricing.summary)}”调整为“${stringifyValue(toPricing.summary)}”`
  }
  return ''
}

function buildConditionExplainLines(fromConditions, toConditions) {
  const lines = []
  const fromMap = new Map(fromConditions.map(item => [buildConditionIdentity(item), item]))
  const toMap = new Map(toConditions.map(item => [buildConditionIdentity(item), item]))
  const identities = Array.from(new Set([...fromMap.keys(), ...toMap.keys()]))
  identities.forEach((identity) => {
    const fromItem = fromMap.get(identity)
    const toItem = toMap.get(identity)
    if (fromItem && !toItem) {
      lines.push(`删除条件：${formatConditionText(fromItem)}。`)
      return
    }
    if (!fromItem && toItem) {
      lines.push(`新增条件：${formatConditionText(toItem)}。`)
      return
    }
    if (fromItem && toItem) {
      const fromText = formatConditionText(fromItem)
      const toText = formatConditionText(toItem)
      if (fromText !== toText) {
        lines.push(`条件由“${fromText}”调整为“${toText}”。`)
      }
    }
  })
  return lines
}

function buildConditionScopeText(conditions) {
  if (!conditions?.length) {
    return ''
  }
  const sorted = [...conditions].sort((a, b) => {
    const groupDiff = Number(a.groupNo || 0) - Number(b.groupNo || 0)
    if (groupDiff !== 0) {
      return groupDiff
    }
    return Number(a.sortNo || 0) - Number(b.sortNo || 0)
  })
  const groups = new Map()
  sorted.forEach((item) => {
    const groupKey = String(item.groupNo || 1)
    if (!groups.has(groupKey)) {
      groups.set(groupKey, [])
    }
    groups.get(groupKey).push(formatConditionText(item))
  })
  return Array.from(groups.values())
    .map(groupItems => groupItems.join(' 且 '))
    .join(' 或 ')
}

function buildTierExplainLines(fromTiers, toTiers) {
  const lines = []
  const fromMap = new Map(fromTiers.map(item => [String(item.tierNo), item]))
  const toMap = new Map(toTiers.map(item => [String(item.tierNo), item]))
  const tierNos = Array.from(new Set([...fromMap.keys(), ...toMap.keys()])).sort()
  tierNos.forEach((tierNo) => {
    const fromItem = fromMap.get(tierNo)
    const toItem = toMap.get(tierNo)
    if (fromItem && !toItem) {
      lines.push(`删除阶梯 ${tierNo}：${formatTierText(fromItem)}。`)
      return
    }
    if (!fromItem && toItem) {
      lines.push(`新增阶梯 ${tierNo}：${formatTierText(toItem)}。`)
      return
    }
    if (fromItem && toItem) {
      const fromText = formatTierText(fromItem)
      const toText = formatTierText(toItem)
      if (fromText !== toText) {
        lines.push(`阶梯 ${tierNo} 由“${fromText}”调整为“${toText}”。`)
      }
    }
  })
  return lines
}

function buildTierBusinessText(fromTiers, toTiers) {
  const fromLength = fromTiers?.length || 0
  const toLength = toTiers?.length || 0
  if (!fromLength && !toLength) {
    return ''
  }
  if (!fromLength && toLength) {
    return `新增 ${toLength} 档阶梯规则`
  }
  if (fromLength && !toLength) {
    return `删除原有 ${fromLength} 档阶梯规则`
  }
  if (fromLength !== toLength) {
    return `阶梯档位由 ${fromLength} 档调整为 ${toLength} 档`
  }
  return ''
}

function buildConditionIdentity(item) {
  return [item.variableCode, item.displayName, item.groupNo, item.sortNo].map(stringifyValue).join('|')
}

function formatConditionText(item) {
  return `${item.displayName || item.variableCode || '未知变量'} ${resolveOperatorLabel(item.operatorCode)} ${stringifyValue(item.compareValue)}`
}

function formatTierText(item) {
  return `${stringifyValue(item.startValue)} ~ ${stringifyValue(item.endValue)}，费率 ${stringifyValue(item.rateValue)}`
}

function stringifyMeasureUnit(value) {
  if (value === null || value === undefined || value === '' || value === '空') {
    return ''
  }
  return String(value)
}

function formatMeasureValue(value, unit) {
  const text = stringifyValue(value)
  if (text === '空') {
    return text
  }
  return unit ? `${text} ${unit}` : text
}

function resolveOperatorLabel(operatorCode) {
  const operatorMap = {
    EQ: '=',
    NE: '!=',
    GT: '>',
    GE: '>=',
    LT: '<',
    LE: '<=',
    IN: '属于',
    NOT_IN: '不属于',
    LIKE: '包含',
    BETWEEN: '介于'
  }
  return operatorMap[operatorCode] || operatorCode || '='
}

function stringifyValue(value) {
  if (value === null || value === undefined || value === '') {
    return '空'
  }
  if (typeof value === 'object') {
    return JSON.stringify(value)
  }
  return String(value)
}

onActivated(() => {
  getList()
})

getList()
</script>

<style scoped lang="scss">
.publish-center {
  display: grid;
  gap: 16px;
}

.publish-center__hero,
.publish-center__metric-card,
.publish-center__precheck,
.publish-center__ledger {
  border: 1px solid var(--el-border-color);
  border-radius: 16px;
  background: var(--el-bg-color-overlay);
}

.publish-center__hero {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  padding: 22px 24px;
  background: color-mix(in srgb, var(--el-color-primary-light-9) 18%, var(--el-bg-color-overlay));
}

.publish-center__eyebrow {
  font-size: 12px;
  color: var(--el-color-primary);
  font-weight: 700;
  letter-spacing: .08em;
  text-transform: uppercase;
}

.publish-center__title {
  margin: 8px 0 0;
  font-size: 28px;
}

.publish-center__subtitle {
  margin: 10px 0 0;
  color: var(--el-text-color-regular);
  line-height: 1.8;
}

.publish-center__metrics {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 14px;
}

.publish-center__metric-card {
  display: grid;
  gap: 6px;
  padding: 14px 16px;
}

.publish-center__metric-card strong {
  font-size: 26px;
  color: var(--el-color-primary);
}

.publish-center__metric-card span,
.publish-center__metric-card small {
  color: var(--el-text-color-secondary);
}

.publish-center__workspace {
  display: grid;
  grid-template-columns: 420px minmax(0, 1fr);
  gap: 16px;
}

.publish-center__precheck,
.publish-center__ledger {
  padding: 16px;
}

.publish-center__section-head {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: center;
  margin-bottom: 16px;
}

.publish-center__section-head h3 {
  margin: 0;
  font-size: 18px;
}

.publish-center__section-head p {
  margin: 6px 0 0;
  color: var(--el-text-color-secondary);
  font-size: 13px;
}

.publish-center__action-row,
.publish-center__filter-row {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-bottom: 16px;
}

.publish-center__precheck-summary {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
  margin-bottom: 16px;
}

.publish-center__summary-card {
  display: grid;
  gap: 6px;
  padding: 12px 14px;
  border-radius: 12px;
  border: 1px solid var(--el-border-color-light);
  background: var(--el-fill-color-blank);
}

.publish-center__summary-card strong {
  font-size: 24px;
  color: var(--el-color-primary);
}

.publish-center__impact h4 {
  margin: 0 0 10px;
}

.publish-center__impact-list {
  display: grid;
  gap: 10px;
}

.publish-center__impact-item {
  display: grid;
  gap: 4px;
  padding: 12px;
  border-radius: 12px;
  border: 1px solid var(--el-border-color-light);
  background: color-mix(in srgb, var(--el-color-success-light-9) 20%, var(--el-bg-color-overlay));
}

.publish-center__impact-item span,
.publish-center__impact-item small {
  color: var(--el-text-color-secondary);
}

.publish-center__collapse {
  margin-top: 16px;
}

.publish-center__collapse pre {
  margin: 0;
  white-space: pre-wrap;
  word-break: break-all;
}
.publish-center__compare-head {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 56px minmax(0, 1fr);
  gap: 12px;
  align-items: stretch;
  margin-bottom: 16px;
}

.publish-center__compare-card {
  display: grid;
  gap: 6px;
  padding: 14px 16px;
  border-radius: 14px;
  border: 1px solid var(--el-border-color-light);
  background: color-mix(in srgb, var(--el-color-success-light-9) 35%, var(--el-bg-color-overlay));
}

.publish-center__compare-card--target {
  background: color-mix(in srgb, var(--el-color-danger-light-9) 35%, var(--el-bg-color-overlay));
}

.publish-center__compare-card strong {
  font-size: 24px;
  color: var(--el-text-color-primary);
}

.publish-center__compare-card small,
.publish-center__compare-label {
  color: var(--el-text-color-secondary);
}

.publish-center__compare-arrow {
  display: grid;
  place-items: center;
  font-size: 18px;
  font-weight: 700;
  color: var(--el-color-primary);
}

.publish-center__diff-tip {
  margin-bottom: 12px;
}

.publish-center__diff-detail {
  display: grid;
  gap: 16px;
  margin-top: 16px;
}

.publish-center__explain-panel {
  border: 1px solid var(--el-border-color-light);
  border-radius: 14px;
  background: color-mix(in srgb, var(--el-color-primary-light-9) 35%, var(--el-bg-color-overlay));
}

.publish-center__explain-title {
  padding: 12px 14px;
  font-weight: 700;
  border-bottom: 1px solid var(--el-border-color-lighter);
}

.publish-center__explain-list {
  display: grid;
  gap: 10px;
  padding: 14px;
}

.publish-center__explain-item {
  padding: 10px 12px;
  border-radius: 10px;
  background: var(--el-bg-color-overlay);
  border: 1px solid var(--el-border-color-lighter);
  line-height: 1.7;
}

.publish-center__bc-section {
  border: 1px solid var(--el-border-color-light);
  border-radius: 14px;
  overflow: hidden;
  background: var(--el-bg-color-overlay);
}

.publish-center__bc-title {
  padding: 12px 14px;
  font-weight: 700;
  border-bottom: 1px solid var(--el-border-color-lighter);
  background: var(--el-fill-color-light);
}

.publish-center__bc-header {
  display: grid;
  grid-template-columns: 1fr 1fr;
  border-bottom: 1px solid var(--el-border-color-lighter);
  background: var(--el-fill-color-blank);
}

.publish-center__bc-header span {
  padding: 10px 14px;
  font-weight: 600;
}

.publish-center__bc-header span:last-child {
  border-left: 1px solid var(--el-border-color-lighter);
}

.publish-center__bc-body {
  display: grid;
}

.publish-center__bc-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
}

.publish-center__bc-row + .publish-center__bc-row {
  border-top: 1px solid var(--el-border-color-lighter);
}

.publish-center__bc-cell + .publish-center__bc-cell {
  border-left: 1px solid var(--el-border-color-lighter);
}

.publish-center__bc-cell--same {
  background: #ecf9ef;
}

.publish-center__bc-cell--diff {
  background: #fff1f0;
}

.publish-center__bc-line {
  margin: 0;
  padding: 6px 10px;
  white-space: pre-wrap;
  word-break: break-word;
  font-family: Consolas, 'Courier New', monospace;
  font-size: 12px;
  line-height: 1.5;
}

@media (max-width: 1200px) {
  .publish-center__metrics,
  .publish-center__workspace,
  .publish-center__compare-head,
  .publish-center__bc-row,
  .publish-center__bc-header {
    grid-template-columns: 1fr;
  }

  .publish-center__compare-arrow {
    display: none;
  }

  .publish-center__bc-cell + .publish-center__bc-cell,
  .publish-center__bc-header span:last-child {
    border-left: none;
    border-top: 1px solid var(--el-border-color-lighter);
  }
}
</style>

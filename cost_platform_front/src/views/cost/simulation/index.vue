<template>
  <div class="app-container simulation-page" :class="{ 'is-compact-mode': isCompactMode }">
    <section v-show="!isCompactMode" class="simulation-page__hero">
      <div>
        <div class="simulation-page__eyebrow">试算验证</div>
        <h2 class="simulation-page__title">试算中心</h2>
        <p class="simulation-page__subtitle">
          按照当前场景、版本与账期完成单笔试算和批量回归，把输入准备、计费展开、规则命中链路和历史回看放在同一页核对。
        </p>
      </div>
      <div class="simulation-page__hero-side">
        <el-tag type="success">当前场景：{{ activeSceneLabel }}</el-tag>
        <el-tag effect="plain">{{ activeVersionLabel }}</el-tag>
        <el-tag effect="plain">账期：{{ queryParams.billMonth || '未选择' }}</el-tag>
        <el-button link type="primary" icon="Files" @click="router.push(COST_MENU_ROUTES.architecture)">数据架构</el-button>
      </div>
    </section>

    <section class="simulation-page__entry-guide">
      <div class="simulation-page__entry-card is-active">
        <div>
          <strong>试算验证</strong>
          <p>用于规则联调、样例回归和结果解释，不落正式台账。</p>
        </div>
        <el-tag type="success">当前入口</el-tag>
      </div>
      <div class="simulation-page__entry-card">
        <div>
          <strong>正式核算</strong>
          <p>用于生产账期任务提交，执行后进入结果台账和异常治理。</p>
        </div>
        <el-button type="warning" plain icon="Promotion" @click="goFormalRun">去正式核算</el-button>
      </div>
    </section>

    <section class="simulation-page__query-shell">
      <div class="simulation-page__panel-head">
        <div>
          <div class="simulation-page__eyebrow simulation-page__eyebrow--sub">试算上下文</div>
          <h3>当前口径与试算记录筛选</h3>
          <p>上方选择的是当前试算口径，同时也是下方历史试算记录的默认过滤范围。</p>
        </div>
        <right-toolbar v-model:showSearch="showSearch" @queryTable="handleQuery" />
      </div>

      <el-form v-show="showSearch" :model="queryParams" :inline="true" label-width="84px" class="simulation-page__query-form">
        <el-form-item label="所属场景">
          <el-select v-model="queryParams.sceneId" clearable filterable style="width: 240px" @change="handleSceneChange">
            <el-option
              v-for="item in sceneOptions"
              :key="item.sceneId"
              :label="`${item.sceneName} / ${item.sceneCode}`"
              :value="item.sceneId"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="版本号">
          <el-select
            v-model="queryParams.versionId"
            clearable
            filterable
            placeholder="按当前配置"
            style="width: 220px"
            @change="handleVersionChange"
          >
            <el-option v-for="item in versionOptions" :key="item.versionId" :label="item.versionNo" :value="item.versionId" />
          </el-select>
        </el-form-item>
        <el-form-item label="账期">
          <el-date-picker
            v-model="queryParams.billMonth"
            clearable
            type="month"
            format="YYYY-MM"
            value-format="YYYY-MM"
            placeholder="请选择账期"
            style="width: 160px"
            @change="handleBillMonthChange"
          />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="queryParams.status" clearable style="width: 180px" @change="handleStatusChange">
            <el-option v-for="item in simulationStatusOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
          <el-button icon="Refresh" @click="resetQuery">重置</el-button>
        </el-form-item>
      </el-form>
    </section>

    <section v-show="!isCompactMode" class="simulation-page__summary-grid simulation-page__summary-grid--overview">
      <div v-for="item in overviewItems" :key="item.label" class="simulation-page__summary-card">
        <span>{{ item.label }}</span>
        <strong>{{ item.value }}</strong>
        <small>{{ item.desc }}</small>
      </div>
    </section>

    <section class="simulation-page__panel">
      <div class="simulation-page__panel-head">
        <div>
          <h3>输入准备工具</h3>
          <p>先生成建议入参，再决定进入单笔验证还是批量回归。模板字段会跟随当前场景与版本上下文变化。</p>
        </div>
        <div class="simulation-page__action-row">
          <el-button icon="RefreshLeft" @click="fillTemplates">刷新建议入参</el-button>
          <el-button @click="copySingleToBatch">单笔复制到批量区</el-button>
          <el-button @click="copyFirstBatchToSingle">批量首条回填单笔区</el-button>
        </div>
      </div>

      <div class="simulation-page__summary-grid simulation-page__summary-grid--prep">
        <div v-for="item in prepItems" :key="item.label" class="simulation-page__summary-card simulation-page__summary-card--soft">
          <span>{{ item.label }}</span>
          <strong>{{ item.value }}</strong>
          <small>{{ item.desc }}</small>
        </div>
      </div>

      <el-alert
        v-if="inputPrepMessage"
        :title="inputPrepMessage"
        :type="queryParams.sceneId ? 'info' : 'warning'"
        :closable="false"
        class="simulation-page__alert"
      />

      <el-table
        v-if="templatePreviewFields.length"
        :data="templatePreviewFields"
        border
        size="small"
        max-height="260"
        class="simulation-page__table"
      >
        <el-table-column label="输入路径" prop="path" min-width="180" />
        <el-table-column label="变量" min-width="200">
          <template #default="{ row }">{{ row.variableName }} ({{ row.variableCode }})</template>
        </el-table-column>
        <el-table-column label="来源" width="120">
          <template #default="{ row }">{{ resolveVariableSourceLabel(row.sourceType) }}</template>
        </el-table-column>
        <el-table-column label="类型" prop="dataType" width="120" />
        <el-table-column label="模板角色" width="140">
          <template #default="{ row }">{{ resolveTemplateRoleLabel(row.templateRole) }}</template>
        </el-table-column>
      </el-table>
      <el-empty v-else description="选择试算场景后生成建议入参与模板字段" :image-size="72" />
    </section>

    <section class="simulation-page__workspace">
      <div class="simulation-page__workbench">
        <div class="simulation-page__panel-head">
          <div>
            <h3>单笔试算工作区</h3>
            <p>用于确认单条业务对象的变量求值、规则命中和计费展开，适合规则联调和回归核对。</p>
          </div>
          <span class="simulation-page__panel-badge">单笔</span>
        </div>

        <div class="simulation-page__context-strip">
          <span>场景：{{ activeSceneLabel }}</span>
          <span>版本：{{ activeVersionLabel }}</span>
          <span>账期：{{ queryParams.billMonth || '未选择' }}</span>
        </div>

        <JsonEditor
          v-model="workbench.singleInputJson"
          title="单笔输入 JSON"
          :theme="jsonEditorTheme"
          compact
          :height="singleEditorHeight"
          :max-length="12000"
          :allow-empty="false"
          placeholder="请输入单笔业务对象 JSON，执行后会在下方展示变量求值、规则命中链路和计费展开。"
        />

        <div class="simulation-page__action-row simulation-page__action-row--workbench">
          <el-button type="primary" icon="Promotion" v-hasPermi="['cost:simulation:execute']" @click="handleSingleExecute">执行试算</el-button>
          <el-button icon="RefreshLeft" @click="fillTemplates('SINGLE')">重置单笔模板</el-button>
        </div>
      </div>

      <div class="simulation-page__workbench">
        <div class="simulation-page__panel-head">
          <div>
            <h3>批量试算工作区</h3>
            <p>用于长列表回归校验。建议传入对象数组，并通过业务编号区分每一条记录的回放结果。</p>
          </div>
          <span class="simulation-page__panel-badge simulation-page__panel-badge--warning">批量</span>
        </div>

        <div class="simulation-page__context-strip">
          <span>场景：{{ activeSceneLabel }}</span>
          <span>版本：{{ activeVersionLabel }}</span>
          <span>账期：{{ queryParams.billMonth || '未选择' }}</span>
        </div>

        <JsonEditor
          v-model="workbench.batchInputJson"
          title="批量输入 JSON"
          :theme="jsonEditorTheme"
          compact
          :height="batchEditorHeight"
          :max-length="30000"
          :allow-empty="false"
          placeholder="请输入批量对象数组 JSON。每条对象建议带上 bizNo，便于结果回放和问题定位。"
        />

        <el-alert
          v-if="showBatchSummary"
          :title="`最近一次批量回归共 ${batchResult.totalCount || 0} 条，成功 ${batchResult.successCount || 0} 条，失败 ${batchResult.failedCount || 0} 条。`"
          :type="Number(batchResult.failedCount || 0) > 0 ? 'warning' : 'success'"
          :closable="false"
          class="simulation-page__alert simulation-page__alert--inline"
        />

        <div class="simulation-page__action-row simulation-page__action-row--workbench">
          <el-button type="warning" icon="Operation" v-hasPermi="['cost:simulation:execute']" @click="handleBatchExecute">批量试算</el-button>
          <el-button icon="RefreshLeft" @click="fillTemplates('BATCH')">重置批量模板</el-button>
        </div>
      </div>
    </section>

    <section v-if="showResultSummary" class="simulation-page__panel">
      <div class="simulation-page__panel-head">
        <div>
          <h3>即时结果总览</h3>
          <p>当前展示的是 {{ activeResultSourceLabel }}，先确认总金额、命中费用与解释步骤，再决定是否继续下钻到变量和规则。</p>
        </div>
        <div class="simulation-page__action-row">
          <span class="simulation-page__panel-badge">{{ activeResultBadge }}</span>
          <el-dropdown trigger="click" @command="handleResultExportCommand">
            <el-button type="primary" plain icon="Download">导出结果</el-button>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="evidence-json">试算证据包 JSON</el-dropdown-item>
                <el-dropdown-item command="charge-excel" :disabled="!resultChargeRows.length">计费明细 Excel</el-dropdown-item>
                <el-dropdown-item command="copy-summary">复制试算摘要</el-dropdown-item>
                <el-dropdown-item command="summary-txt">导出试算摘要 TXT</el-dropdown-item>
                <el-dropdown-item command="report-html">试算报告 HTML</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
          <el-button @click="openActiveDetailDrawer">查看原始 JSON</el-button>
        </div>
      </div>

      <div class="simulation-page__summary-grid simulation-page__summary-grid--result">
        <div v-for="item in resultSummaryItems" :key="item.label" class="simulation-page__summary-card">
          <span>{{ item.label }}</span>
          <strong>{{ item.value }}</strong>
          <small>{{ item.desc }}</small>
        </div>
      </div>
    </section>

    <section v-else class="simulation-page__panel simulation-page__panel--placeholder">
      <el-empty :description="resultEmptyDescription" :image-size="72" />
    </section>

    <section v-if="timelineItems.length" class="simulation-page__panel">
      <div class="simulation-page__panel-head">
        <div>
          <h3>执行步骤时间线</h3>
          <p>按照执行链路回放本次试算中实际发生了什么，便于快速确认命中路径和结果来源。</p>
        </div>
        <span class="simulation-page__panel-badge">共 {{ timelineItems.length }} 步</span>
      </div>

      <div class="simulation-page__timeline-grid">
        <article v-for="item in timelineItems" :key="item.id" class="simulation-page__timeline-card">
          <div class="simulation-page__timeline-head">
            <span class="simulation-page__timeline-order">STEP {{ item.orderNo }}</span>
            <span class="simulation-page__timeline-status" :class="`is-${item.statusClass}`">{{ item.statusText }}</span>
          </div>
          <h4>{{ item.title }}</h4>
          <p>{{ item.summary }}</p>
          <div v-if="item.tokens.length" class="simulation-page__token-row">
            <span v-for="token in item.tokens" :key="`${item.id}-${token}`" class="simulation-page__token">{{ token }}</span>
          </div>
        </article>
      </div>
    </section>

    <section v-if="showResultSummary" class="simulation-page__result-grid">
      <div class="simulation-page__panel">
        <div class="simulation-page__panel-head">
          <div>
            <h3>变量求值</h3>
            <p>优先看输入变量和公式变量的最终取值，确认后续规则匹配与金额计算口径是否一致。</p>
          </div>
          <span class="simulation-page__panel-badge">{{ resultVariableRows.length }} 项</span>
        </div>

        <el-table :data="resultVariableRows" border size="small" max-height="420" class="simulation-page__table">
          <el-table-column label="变量编码" prop="code" min-width="160" />
          <el-table-column label="变量名称" prop="name" min-width="160" />
          <el-table-column label="来源" width="120">
            <template #default="{ row }">{{ resolveVariableSourceLabel(row.sourceType) }}</template>
          </el-table-column>
          <el-table-column label="值" prop="displayValue" min-width="180" show-overflow-tooltip />
        </el-table>
      </div>

      <div class="simulation-page__panel">
        <div class="simulation-page__panel-head">
          <div>
            <h3>计费展开</h3>
            <p>按费目查看本次命中的规则、数量来源、单价来源和最终金额，便于业务核对和回归解释。</p>
          </div>
          <span class="simulation-page__panel-badge">{{ resultChargeRows.length }} 条</span>
        </div>

        <el-table :data="resultChargeRows" border size="small" max-height="420" class="simulation-page__table">
          <el-table-column label="费目编码" prop="feeCode" min-width="140" />
          <el-table-column label="费目名称" prop="feeName" min-width="160" />
          <el-table-column label="命中规则" prop="ruleCode" min-width="150" />
          <el-table-column label="数量来源" prop="quantitySource" min-width="180" show-overflow-tooltip />
          <el-table-column label="单价来源" prop="unitPriceSource" min-width="200" show-overflow-tooltip />
          <el-table-column label="金额" prop="amountText" width="120" align="right" />
        </el-table>
      </div>
    </section>

    <section v-if="showResultSummary" class="simulation-page__panel">
      <div class="simulation-page__panel-head">
        <div>
          <h3>规则命中链路</h3>
          <p>按费目回看命中规则、定价来源和分组/阶梯信息，帮助确认为什么会命中这条计费路径。</p>
        </div>
        <span class="simulation-page__panel-badge">{{ resultPolicyRows.length }} 条</span>
      </div>

      <el-table :data="resultPolicyRows" border size="small" max-height="360" class="simulation-page__table">
        <el-table-column label="费目" min-width="180">
          <template #default="{ row }">{{ row.feeName }} ({{ row.feeCode }})</template>
        </el-table-column>
        <el-table-column label="命中规则" prop="ruleCode" min-width="150" />
        <el-table-column label="规则名称" prop="ruleName" min-width="160" />
        <el-table-column label="定价来源" prop="pricingSourceLabel" width="140" />
        <el-table-column label="分组/阶梯" prop="groupLabel" width="140" />
        <el-table-column label="是否命中" width="110" align="center">
          <template #default="{ row }">
            <el-tag :type="row.matched ? 'success' : 'info'" effect="plain">{{ row.matched ? '命中' : '未命中' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="摘要" prop="summary" min-width="220" show-overflow-tooltip />
      </el-table>
    </section>

    <section v-if="showBatchSummary" class="simulation-page__panel">
      <div class="simulation-page__panel-head">
        <div>
          <h3>批量回归摘要</h3>
          <p>批量试算只服务当前回归确认。需要长期跟踪或补偿执行时，再进入任务中心做正式治理。</p>
        </div>
        <div class="simulation-page__action-row">
          <span class="simulation-page__panel-badge simulation-page__panel-badge--warning">
            成功率 {{ batchSuccessRate }}
          </span>
          <el-dropdown trigger="click" @command="handleResultExportCommand">
            <el-button plain icon="Download">导出批量</el-button>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="batch-summary-excel">批量回归摘要 Excel</el-dropdown-item>
                <el-dropdown-item command="batch-failures-excel" :disabled="!batchFailedRecords.length">批量失败清单 Excel</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </div>

      <div class="simulation-page__summary-grid simulation-page__summary-grid--batch">
        <div v-for="item in batchSummaryItems" :key="item.label" class="simulation-page__summary-card simulation-page__summary-card--soft">
          <span>{{ item.label }}</span>
          <strong>{{ item.value }}</strong>
          <small>{{ item.desc }}</small>
        </div>
      </div>

      <el-table :data="batchResultRecords" border size="small" max-height="360" class="simulation-page__table" :row-class-name="getBatchRowClass">
        <el-table-column label="业务编号" prop="bizNo" min-width="140" />
        <el-table-column label="试算编号" prop="simulationNo" min-width="180" />
        <el-table-column label="账期" prop="billMonth" width="110" align="center" />
        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <dict-tag :options="simulationStatusOptions" :value="row.status" />
          </template>
        </el-table-column>
        <el-table-column label="执行时间" width="180" align="center">
          <template #default="{ row }">{{ formatDateTime(row.simulationTime) }}</template>
        </el-table-column>
        <el-table-column label="异常信息" prop="errorMessage" min-width="240" show-overflow-tooltip />
        <el-table-column label="操作" width="120" fixed="right" align="center">
          <template #default="{ row }">
            <el-button link type="primary" icon="View" @click="handleBatchReplay(row)">回看</el-button>
          </template>
        </el-table-column>
      </el-table>
    </section>

    <section class="simulation-page__panel simulation-page__panel--history">
      <div class="simulation-page__panel-head">
        <div>
          <h3>试算记录台账</h3>
          <p>保留最近的试算记录，支持回看原始输入、变量结果、计费结果与解释链路。</p>
        </div>
        <span class="simulation-page__panel-badge">共 {{ total }} 条</span>
      </div>

      <el-table
        v-loading="loading"
        :data="recordList"
        border
        class="simulation-page__table"
        :row-class-name="getHistoryRowClass"
      >
        <el-table-column label="试算编号" prop="simulationNo" width="220" />
        <el-table-column label="场景" min-width="180">
          <template #default="{ row }">{{ row.sceneName }} ({{ row.sceneCode }})</template>
        </el-table-column>
        <el-table-column label="版本" prop="versionNo" width="160" />
        <el-table-column label="账期" prop="billMonth" width="110" align="center" />
        <el-table-column label="状态" width="110" align="center">
          <template #default="{ row }">
            <dict-tag :options="simulationStatusOptions" :value="row.status" />
          </template>
        </el-table-column>
        <el-table-column label="创建时间" width="180" align="center">
          <template #default="{ row }">{{ formatDateTime(row.createTime) }}</template>
        </el-table-column>
        <el-table-column label="异常信息" prop="errorMessage" min-width="220" show-overflow-tooltip />
        <el-table-column label="操作" width="160" fixed="right" align="center">
          <template #default="{ row }">
            <el-button link type="primary" icon="View" @click="handleHistoryReplay(row)">回看</el-button>
            <el-button link type="primary" @click="handleHistoryReplay(row, true)">原始 JSON</el-button>
          </template>
        </el-table-column>
      </el-table>

      <pagination
        v-show="total > 0"
        class="simulation-page__pagination"
        :total="total"
        v-model:page="queryParams.pageNum"
        v-model:limit="queryParams.pageSize"
        @pagination="getList"
      />
    </section>

    <el-drawer v-model="detailDrawerOpen" title="试算原始数据" size="980px" append-to-body>
      <el-descriptions v-if="activeDetailRecord.simulationId" :column="2" border class="simulation-page__drawer-desc">
        <el-descriptions-item label="试算编号">{{ activeDetailRecord.simulationNo }}</el-descriptions-item>
        <el-descriptions-item label="状态">{{ resolveSimulationStatus(activeDetailRecord.status) }}</el-descriptions-item>
        <el-descriptions-item label="场景">{{ activeDetailRecord.sceneName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="版本">{{ activeDetailRecord.versionNo || '-' }}</el-descriptions-item>
        <el-descriptions-item label="账期">{{ activeDetailRecord.billMonth || '-' }}</el-descriptions-item>
        <el-descriptions-item label="创建时间">{{ formatDateTime(activeDetailRecord.createTime) }}</el-descriptions-item>
      </el-descriptions>

      <el-tabs class="simulation-page__drawer-tabs">
        <el-tab-pane label="输入数据">
          <JsonEditor :model-value="activeDetail.input" title="输入数据" :theme="jsonEditorTheme" readonly :rows="14" />
        </el-tab-pane>
        <el-tab-pane label="变量结果">
          <JsonEditor :model-value="activeDetail.variables" title="变量结果" :theme="jsonEditorTheme" readonly :rows="14" />
        </el-tab-pane>
        <el-tab-pane label="费用结果">
          <JsonEditor :model-value="activeDetail.result" title="费用结果" :theme="jsonEditorTheme" readonly :rows="14" />
        </el-tab-pane>
        <el-tab-pane label="解释链路">
          <JsonEditor :model-value="activeDetail.explain" title="解释链路" :theme="jsonEditorTheme" readonly :rows="14" />
        </el-tab-pane>
      </el-tabs>
    </el-drawer>
  </div>
</template>

<script setup name="CostSimulation">
import { computed, getCurrentInstance, onActivated, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import JsonEditor from '@/components/cost/JsonEditor.vue'
import {
  executeSimulation,
  executeSimulationBatch,
  getRunInputTemplate,
  getSimulationDetail,
  getSimulationStats,
  listSimulation,
  listVersionOptions
} from '@/api/cost/run'
import { optionselectScene } from '@/api/cost/scene'
import useSettingsStore from '@/store/modules/settings'
import { COST_MENU_ROUTES } from '@/utils/costMenuRoutes'
import { resolveWorkingCostSceneId } from '@/utils/costSceneContext'
import { confirmCostSceneSwitch } from '@/utils/costSceneSwitchGuard'
import { clearCostWorkContext, resolveWorkingBillMonth, resolveWorkingVersionId, syncCostWorkContext } from '@/utils/costWorkContext'
import { getRemoteDictOptionMap } from '@/utils/dictRemote'

const route = useRoute()
const router = useRouter()
const { proxy } = getCurrentInstance()
const settingsStore = useSettingsStore()

const variableSourceLabelMap = {
  INPUT: '输入变量',
  INPUT_JSON: '输入变量',
  FORMULA: '公式变量',
  RULE_DERIVED: '规则派生',
  DICT: '字典取值',
  DICTIONARY: '字典取值',
  MANUAL: '手工输入',
  MANUAL_INPUT: '手工输入',
  CONSTANT: '常量',
  SYSTEM: '系统生成'
}

const templateRoleLabelMap = {
  REQUIRED: '必填入参',
  OPTIONAL: '可选入参',
  RULE_DERIVED: '规则派生',
  RESULT_RELATED: '结果关联'
}

const pricingSourceLabelMap = {
  FIXED_RATE: '固定费率',
  FIXED_AMOUNT: '固定金额',
  FORMULA: '公式取价',
  TIER_RATE: '阶梯费率',
  GROUPED: '分组费率'
}

const isCompactMode = computed(() => settingsStore.costPageMode === 'COMPACT')
const loading = ref(false)
const showSearch = ref(true)
const total = ref(0)
const recordList = ref([])
const sceneOptions = ref([])
const versionOptions = ref([])
const simulationStatusOptions = ref([])
const templateFieldCatalog = ref([])
const templateHints = reactive({
  single: '',
  batch: ''
})
const batchResult = ref({})
const detailDrawerOpen = ref(false)
const activeDetail = ref(createEmptyDetail())
const activeResultSourceLabel = ref('最近单笔试算结果')
const initialized = ref(false)
const lastSceneId = ref(undefined)
const stats = reactive({
  simulationCount: 0,
  successCount: 0,
  failedCount: 0,
  sceneCount: 0
})

const queryParams = reactive({
  pageNum: 1,
  pageSize: 10,
  sceneId: route.query.sceneId ? Number(route.query.sceneId) : undefined,
  versionId: resolveWorkingVersionId(route.query.versionId ? Number(route.query.versionId) : undefined),
  billMonth: resolveWorkingBillMonth(route.query.billMonth),
  status: undefined
})

const workbench = reactive({
  singleInputJson: '',
  batchInputJson: ''
})

const activeSceneOption = computed(() => sceneOptions.value.find(item => item.sceneId === queryParams.sceneId))
const activeVersionOption = computed(() => versionOptions.value.find(item => item.versionId === queryParams.versionId))
const activeSceneLabel = computed(() => {
  if (!activeSceneOption.value) {
    return '未选择场景'
  }
  return `${activeSceneOption.value.sceneName} / ${activeSceneOption.value.sceneCode}`
})
const activeVersionLabel = computed(() => {
  if (!activeVersionOption.value) {
    return '按当前配置执行'
  }
  return `${activeVersionOption.value.versionNo}${activeVersionOption.value.versionStatus ? ` / ${activeVersionOption.value.versionStatus}` : ''}`
})
const jsonEditorTheme = computed(() => (settingsStore.isDark ? 'tomorrow_night_eighties' : 'chrome'))
const singleEditorHeight = computed(() => (isCompactMode.value ? 360 : 420))
const batchEditorHeight = computed(() => (isCompactMode.value ? 360 : 420))
const templatePreviewFields = computed(() => templateFieldCatalog.value.filter(item => item.includedInTemplate !== false))
const fieldMetaMap = computed(() => {
  const map = {}
  templateFieldCatalog.value.forEach((item, index) => {
    const key = item.variableCode || item.path || `field-${index}`
    if (!map[key]) {
      map[key] = { ...item, __index: index }
    }
  })
  return map
})
const singleInputSummary = computed(() => inspectInputStructure(workbench.singleInputJson))
const batchInputSummary = computed(() => inspectInputStructure(workbench.batchInputJson))
const inputPrepMessage = computed(() => {
  const parts = []
  if (!queryParams.sceneId) {
    parts.push('请先选择试算场景，再按当前版本口径生成建议入参。')
  }
  if (templateHints.single) {
    parts.push(`单笔模板：${templateHints.single}`)
  }
  if (templateHints.batch) {
    parts.push(`批量模板：${templateHints.batch}`)
  }
  return parts.join('；')
})
const overviewItems = computed(() => [
  {
    label: '当前场景',
    value: activeSceneOption.value?.sceneName || '未选择',
    desc: activeSceneOption.value?.sceneCode || '先选择试算场景再生成建议入参'
  },
  {
    label: '当前版本策略',
    value: activeVersionOption.value?.versionNo || '按当前配置',
    desc: activeVersionOption.value?.versionStatus || '未指定版本时按当前口径执行'
  },
  {
    label: '最近回看总金额',
    value: formatAmount(resolveAmountTotal(activeDetail.value.result)),
    desc: activeDetail.value.record?.simulationNo || '执行单笔试算后在这里展示'
  },
  {
    label: '历史试算记录',
    value: `${total.value}`,
    desc: `成功 ${stats.successCount || 0} / 失败 ${stats.failedCount || 0}`
  }
])
const prepItems = computed(() => [
  {
    label: '模板字段',
    value: templatePreviewFields.value.length ? `${templatePreviewFields.value.length} 项` : '待生成',
    desc: templatePreviewFields.value.length ? '按当前场景与版本生成的输入字段清单' : '选择场景后会自动生成建议入参'
  },
  {
    label: '单笔模板',
    value: singleInputSummary.value.value,
    desc: singleInputSummary.value.desc
  },
  {
    label: '批量模板',
    value: batchInputSummary.value.value,
    desc: batchInputSummary.value.desc
  },
  {
    label: '批量回归',
    value: showBatchSummary.value ? `${batchResult.value.successCount || 0}/${batchResult.value.totalCount || 0}` : '未执行',
    desc: showBatchSummary.value ? '成功条数 / 总条数' : '执行批量试算后在这里展示摘要'
  }
])

const activeDetailRecord = computed(() => normalizeObject(activeDetail.value.record))
const detailResult = computed(() => normalizeObject(activeDetail.value.result))
const detailExplain = computed(() => normalizeObject(activeDetail.value.explain))
const matchedFeeRows = computed(() => normalizeArray(detailExplain.value.matchedFees))
const resultChargeRows = computed(() => {
  const feeExplainMap = new Map(
    matchedFeeRows.value.map(item => [`${item.feeCode || ''}|${item.ruleCode || ''}`, normalizeObject(item)])
  )
  return normalizeArray(detailResult.value.feeResults).map((item, index) => {
    const fee = normalizeObject(item)
    const explain = feeExplainMap.get(`${fee.feeCode || ''}|${fee.ruleCode || ''}`) || {}
    const pricing = normalizeObject(explain.pricing)
    return {
      id: `${fee.feeCode || 'fee'}-${index}`,
      feeCode: fee.feeCode || '-',
      feeName: fee.feeName || '-',
      ruleCode: fee.ruleCode || '-',
      quantitySource: resolveQuantitySourceLabel(pricing),
      unitPriceSource: resolvePricingSourceLabel(pricing),
      amountText: formatAmount(fee.amountValue)
    }
  })
})
const resultVariableRows = computed(() => {
  const variables = normalizeObject(activeDetail.value.variables)
  return Object.entries(variables)
    .map(([code, value], index) => {
      const meta = fieldMetaMap.value[code] || {}
      return {
        id: `${code}-${index}`,
        code,
        name: meta.variableName || code,
        sourceType: meta.sourceType || meta.variableSourceType || '',
        sortNo: meta.sortNo ?? meta.__index ?? index,
        displayValue: formatCellValue(value)
      }
    })
    .sort((a, b) => Number(a.sortNo || 0) - Number(b.sortNo || 0))
})
const timelineItems = computed(() => buildTimelineItems(detailExplain.value, matchedFeeRows.value))
const resultPolicyRows = computed(() => {
  if (!matchedFeeRows.value.length && !resultChargeRows.value.length) {
    return []
  }
  if (!matchedFeeRows.value.length) {
    return resultChargeRows.value.map(item => ({
      id: item.id,
      feeCode: item.feeCode,
      feeName: item.feeName,
      ruleCode: item.ruleCode,
      ruleName: '-',
      pricingSourceLabel: '未返回',
      groupLabel: '-',
      matched: true,
      summary: '当前记录只返回了费用结果，未返回规则解释链路'
    }))
  }
  return matchedFeeRows.value.map((item, index) => {
    const fee = normalizeObject(item)
    const pricing = normalizeObject(fee.pricing)
    const groupLabel = pricing.matchedGroupNo || pricing.tierNo || pricing.tierRange || '-'
    return {
      id: `${fee.feeCode || 'policy'}-${index}`,
      feeCode: fee.feeCode || '-',
      feeName: fee.feeName || '-',
      ruleCode: fee.ruleCode || '-',
      ruleName: fee.ruleName || '-',
      pricingSourceLabel: resolvePricingSourceLabel(pricing),
      groupLabel,
      matched: fee.matched !== false,
      summary: buildPolicySummary(pricing)
    }
  })
})
const showResultSummary = computed(() => Boolean(activeDetailRecord.value.simulationId))
const activeResultBadge = computed(() => activeDetailRecord.value.simulationNo || '当前结果')
const batchReplayCandidate = computed(() => pickBatchReplayCandidate(batchResultRecords.value))
const resultEmptyDescription = computed(() => {
  if (showBatchSummary.value) {
    return batchReplayCandidate.value?.simulationId
      ? '本次批量试算已完成，系统会优先回放一条可查看明细；如需切换对象，可在下方批量回归摘要中点击“回看”。'
      : '本次批量试算已完成，但当前返回中没有可自动回放的试算明细，请先查看下方批量回归摘要中的异常信息。'
  }
  return '执行一次单笔试算后，这里会展示结果总览、步骤回放、变量求值和规则命中链路。'
})
const resultSummaryItems = computed(() => [
  {
    label: '总金额',
    value: formatAmount(resolveAmountTotal(activeDetail.value.result)),
    desc: '当前回看结果中的金额合计'
  },
  {
    label: '计费明细数',
    value: `${resultChargeRows.value.length}`,
    desc: '费用结果里返回的计费明细条数'
  },
  {
    label: '变量求值数',
    value: `${resultVariableRows.value.length}`,
    desc: '本次执行求值得到的变量数量'
  },
  {
    label: '命中费用数',
    value: `${matchedFeeRows.value.length || resultChargeRows.value.length}`,
    desc: '已进入解释链路或计费结果的费用条数'
  },
  {
    label: '规则链路数',
    value: `${resultPolicyRows.value.length}`,
    desc: '可回看的命中规则或定价链路条数'
  },
  {
    label: '解释步骤数',
    value: `${timelineItems.value.length}`,
    desc: '当前结果里可用于回放的执行步骤数量'
  }
])
const batchResultRecords = computed(() => normalizeArray(batchResult.value.records))
const batchFailedRecords = computed(() => batchResultRecords.value.filter(isFailedSimulationRecord))
const showBatchSummary = computed(() => Number(batchResult.value.totalCount || 0) > 0)
const batchSuccessRate = computed(() => {
  const totalCount = Number(batchResult.value.totalCount || 0)
  if (!totalCount) {
    return '0%'
  }
  return `${Math.round((Number(batchResult.value.successCount || 0) / totalCount) * 100)}%`
})
const batchSummaryItems = computed(() => [
  {
    label: '总条数',
    value: `${batchResult.value.totalCount || 0}`,
    desc: '本次批量试算接收到的对象总数'
  },
  {
    label: '成功条数',
    value: `${batchResult.value.successCount || 0}`,
    desc: '成功生成试算记录的对象数量'
  },
  {
    label: '失败条数',
    value: `${batchResult.value.failedCount || 0}`,
    desc: Number(batchResult.value.failedCount || 0) > 0 ? '建议优先回看失败记录的异常信息' : '当前批量回归没有失败记录'
  },
  {
    label: '版本口径',
    value: batchResult.value.versionNo || activeVersionOption.value?.versionNo || '按当前配置',
    desc: batchResult.value.snapshotSource || '当前批量试算采用的版本口径'
  }
])

watch(
  () => [queryParams.sceneId, queryParams.versionId, queryParams.billMonth],
  ([sceneId, versionId, billMonth]) => {
    syncCostWorkContext({ sceneId, versionId, billMonth })
  },
  { immediate: true }
)

onMounted(async () => {
  await initPage()
  initialized.value = true
})

onActivated(async () => {
  if (!initialized.value) {
    return
  }
  await getList()
})

async function initPage() {
  await loadBaseOptions()
  await loadVersionOptions(queryParams.sceneId)
  await fillTemplates()
  await getList()
}

async function loadBaseOptions() {
  const [dictMap, sceneResp] = await Promise.all([
    getRemoteDictOptionMap(['cost_simulation_status']),
    optionselectScene({ status: '0', pageNum: 1, pageSize: 1000 })
  ])
  simulationStatusOptions.value = dictMap.cost_simulation_status || []
  sceneOptions.value = sceneResp?.data || []
  const fallbackSceneId = resolveWorkingCostSceneId(sceneOptions.value, queryParams.sceneId) || sceneOptions.value[0]?.sceneId
  queryParams.sceneId = fallbackSceneId
  lastSceneId.value = queryParams.sceneId
}

async function loadVersionOptions(sceneId) {
  if (!sceneId) {
    versionOptions.value = []
    queryParams.versionId = undefined
    return
  }
  const response = await listVersionOptions(sceneId)
  versionOptions.value = response.data || []
  const preferredVersionId = resolveWorkingVersionId(queryParams.versionId)
  if (!versionOptions.value.some(item => item.versionId === preferredVersionId)) {
    queryParams.versionId = undefined
  }
}

async function getList() {
  loading.value = true
  try {
    const [listResp, statsResp] = await Promise.all([
      listSimulation(queryParams),
      getSimulationStats(queryParams)
    ])
    recordList.value = listResp.rows || []
    total.value = Number(listResp.total || 0)
    Object.assign(stats, {
      simulationCount: 0,
      successCount: 0,
      failedCount: 0,
      sceneCount: 0,
      ...(statsResp.data || {})
    })
  } finally {
    loading.value = false
  }
}

function handleQuery() {
  queryParams.pageNum = 1
  getList()
}

function goFormalRun() {
  router.push({
    path: COST_MENU_ROUTES.task,
    query: {
      sceneId: queryParams.sceneId,
      versionId: queryParams.versionId,
      billMonth: queryParams.billMonth
    }
  })
}

async function resetQuery() {
  queryParams.pageNum = 1
  queryParams.pageSize = 10
  queryParams.status = undefined
  queryParams.billMonth = resolveWorkingBillMonth(route.query.billMonth)
  queryParams.sceneId = resolveWorkingCostSceneId(sceneOptions.value, route.query.sceneId ? Number(route.query.sceneId) : undefined) || sceneOptions.value[0]?.sceneId
  lastSceneId.value = queryParams.sceneId
  await loadVersionOptions(queryParams.sceneId)
  await fillTemplates()
  await getList()
}

async function handleSceneChange(sceneId) {
  const confirmed = await confirmCostSceneSwitch({
    currentSceneId: lastSceneId.value,
    nextSceneId: sceneId,
    sceneOptions: sceneOptions.value,
    scope: '试算上下文'
  })
  if (!confirmed) {
    queryParams.sceneId = lastSceneId.value
    return
  }
  queryParams.sceneId = sceneId
  queryParams.versionId = undefined
  lastSceneId.value = sceneId
  clearCostWorkContext(['versionId'])
  await loadVersionOptions(sceneId)
  await fillTemplates()
  await getList()
}
async function handleVersionChange(versionId) {
  queryParams.versionId = versionId
  if (!versionId) {
    clearCostWorkContext(['versionId'])
  }
  await fillTemplates()
  await getList()
}

async function handleBillMonthChange(value) {
  queryParams.billMonth = value || resolveWorkingBillMonth()
  await getList()
}

function handleStatusChange() {
  handleQuery()
}

async function fillTemplates(mode = 'ALL') {
  if (!queryParams.sceneId) {
    templateFieldCatalog.value = []
    templateHints.single = ''
    templateHints.batch = ''
    workbench.singleInputJson = ''
    workbench.batchInputJson = ''
    return
  }
  const requests = []
  if (mode === 'ALL' || mode === 'SINGLE') {
    requests.push(getRunInputTemplate({
      sceneId: queryParams.sceneId,
      versionId: queryParams.versionId,
      taskType: 'SIMULATION'
    }))
  } else {
    requests.push(Promise.resolve({ data: null }))
  }
  if (mode === 'ALL' || mode === 'BATCH') {
    requests.push(getRunInputTemplate({
      sceneId: queryParams.sceneId,
      versionId: queryParams.versionId,
      taskType: 'SIMULATION_BATCH'
    }))
  } else {
    requests.push(Promise.resolve({ data: null }))
  }

  const [singleResp, batchResp] = await Promise.all(requests)
  const singleData = singleResp?.data
  const batchData = batchResp?.data

  if (mode === 'ALL' || mode === 'SINGLE') {
    workbench.singleInputJson = singleData?.inputJson || '{}'
    templateHints.single = singleData?.message || ''
  }
  if (mode === 'ALL' || mode === 'BATCH') {
    workbench.batchInputJson = batchData?.inputJson || '[]'
    templateHints.batch = batchData?.message || ''
  }
  templateFieldCatalog.value = mergeFieldCatalogs(singleData?.fields || [], batchData?.fields || [])
}

function copySingleToBatch() {
  const parsed = parseJsonTextSafely(workbench.singleInputJson)
  if (!parsed.valid) {
    proxy.$modal.msgWarning('单笔输入 JSON 还未通过校验，暂时不能复制到批量区')
    return
  }
  const nextValue = Array.isArray(parsed.value) ? parsed.value : [parsed.value]
  workbench.batchInputJson = JSON.stringify(nextValue, null, 2)
  proxy.$modal.msgSuccess('已把单笔输入回填到批量区')
}

function copyFirstBatchToSingle() {
  const parsed = parseJsonTextSafely(workbench.batchInputJson)
  if (!parsed.valid || !Array.isArray(parsed.value) || !parsed.value.length) {
    proxy.$modal.msgWarning('批量输入必须是非空数组，才能提取首条回填到单笔区')
    return
  }
  workbench.singleInputJson = JSON.stringify(parsed.value[0], null, 2)
  proxy.$modal.msgSuccess('已把批量首条回填到单笔区')
}

async function handleSingleExecute() {
  if (!validateBeforeExecute(workbench.singleInputJson, 'SINGLE')) {
    return
  }
  const response = await executeSimulation({
    sceneId: queryParams.sceneId,
    versionId: queryParams.versionId,
    billMonth: queryParams.billMonth,
    inputJson: workbench.singleInputJson
  })
  setActiveDetail(response.data, '最近单笔试算结果')
  batchResult.value = {}
  proxy.$modal.msgSuccess('单笔试算执行完成')
  await getList()
}

async function handleBatchExecute() {
  if (!validateBeforeExecute(workbench.batchInputJson, 'BATCH')) {
    return
  }
  const response = await executeSimulationBatch({
    sceneId: queryParams.sceneId,
    versionId: queryParams.versionId,
    billMonth: queryParams.billMonth,
    inputJson: workbench.batchInputJson
  })
  batchResult.value = response.data || {}
  const batchReplayState = await syncBatchReplayDetail(batchResult.value.records)
  proxy.$modal.msgSuccess(batchReplayState.loaded ? '批量试算执行完成，已自动回看首条明细记录' : '批量试算执行完成')
  await getList()
}

async function handleHistoryReplay(row, openDrawer = false) {
  const response = await getSimulationDetail(row.simulationId)
  setActiveDetail(response.data, `试算记录 ${row.simulationNo}`)
  if (openDrawer) {
    detailDrawerOpen.value = true
  }
}

async function handleBatchReplay(row) {
  if (!row.simulationId) {
    proxy.$modal.msgWarning('当前批量项没有可回看的试算记录')
    return
  }
  const response = await getSimulationDetail(row.simulationId)
  setActiveDetail(response.data, `批量记录 ${row.bizNo || row.simulationNo}`)
}

function openActiveDetailDrawer() {
  if (!activeDetailRecord.value.simulationId) {
    proxy.$modal.msgWarning('当前还没有可查看的试算结果')
    return
  }
  detailDrawerOpen.value = true
}

async function handleResultExportCommand(command) {
  switch (command) {
    case 'evidence-json':
      exportSimulationEvidenceJson()
      break
    case 'charge-excel':
      exportSimulationChargeExcel()
      break
    case 'copy-summary':
      await copySimulationSummary()
      break
    case 'summary-txt':
      exportSimulationSummaryTxt()
      break
    case 'report-html':
      exportSimulationReportHtml()
      break
    case 'batch-summary-excel':
      exportBatchSummaryExcel()
      break
    case 'batch-failures-excel':
      exportBatchFailuresExcel()
      break
    default:
      break
  }
}

function exportSimulationEvidenceJson() {
  if (!ensureActiveSimulationResult()) {
    return
  }
  downloadTextFile(
    createExportFileName('试算证据包', 'json'),
    JSON.stringify(buildSimulationEvidencePayload(), null, 2),
    'application/json;charset=utf-8'
  )
  proxy.$modal.msgSuccess('已导出试算证据包 JSON')
}

function exportSimulationChargeExcel() {
  if (!ensureActiveSimulationResult()) {
    return
  }
  if (!resultChargeRows.value.length) {
    proxy.$modal.msgWarning('当前试算结果没有可导出的计费明细')
    return
  }
  proxy.download(
    'cost/run/simulation/charge-export',
    {
      simulationId: activeDetailRecord.value.simulationId
    },
    createExportFileName('试算计费明细', 'xlsx')
  )
}

async function copySimulationSummary() {
  if (!ensureActiveSimulationResult()) {
    return
  }
  try {
    await copyTextToClipboard(buildSimulationSummaryText())
    proxy.$modal.msgSuccess('已复制试算摘要')
  } catch (error) {
    proxy.$modal.msgError('复制试算摘要失败，请改用“导出试算摘要 TXT”')
  }
}

function exportSimulationSummaryTxt() {
  if (!ensureActiveSimulationResult()) {
    return
  }
  downloadTextFile(
    createExportFileName('试算摘要', 'txt'),
    buildSimulationSummaryText(),
    'text/plain;charset=utf-8'
  )
  proxy.$modal.msgSuccess('已导出试算摘要 TXT')
}

function exportSimulationReportHtml() {
  if (!ensureActiveSimulationResult()) {
    return
  }
  downloadTextFile(
    createExportFileName('试算报告', 'html'),
    buildSimulationReportHtml(),
    'text/html;charset=utf-8'
  )
  proxy.$modal.msgSuccess('已导出试算报告 HTML')
}

function exportBatchSummaryExcel() {
  if (!showBatchSummary.value) {
    proxy.$modal.msgWarning('当前还没有可导出的批量回归摘要')
    return
  }
  const simulationIds = resolveBatchExportIds(false)
  if (!simulationIds) {
    proxy.$modal.msgWarning('当前批量回归没有可导出的试算记录')
    return
  }
  proxy.download(
    'cost/run/simulation/batch-export',
    {
      simulationIds,
      failedOnly: false
    },
    createBatchExportFileName('批量回归摘要', 'xlsx')
  )
}

function exportBatchFailuresExcel() {
  if (!batchFailedRecords.value.length) {
    proxy.$modal.msgWarning('当前批量回归没有失败记录')
    return
  }
  const simulationIds = resolveBatchExportIds(true)
  if (!simulationIds) {
    proxy.$modal.msgWarning('当前失败记录没有可导出的试算ID')
    return
  }
  proxy.download(
    'cost/run/simulation/batch-export',
    {
      simulationIds,
      failedOnly: true
    },
    createBatchExportFileName('批量失败清单', 'xlsx')
  )
}

function ensureActiveSimulationResult() {
  if (!activeDetailRecord.value.simulationId) {
    proxy.$modal.msgWarning('当前还没有可导出的试算结果')
    return false
  }
  return true
}

function buildSimulationEvidencePayload() {
  return {
    exportMeta: {
      exportType: 'SIMULATION_EVIDENCE',
      exportName: '试算证据包',
      exportTime: formatExportDateTime(new Date()),
      resultSource: activeResultSourceLabel.value
    },
    context: buildSimulationContext(),
    record: activeDetailRecord.value,
    summary: buildSimulationSummaryObject(),
    tables: {
      charges: resultChargeRows.value,
      variables: resultVariableRows.value,
      policies: resultPolicyRows.value,
      timeline: timelineItems.value
    },
    raw: {
      input: activeDetail.value.input,
      variables: activeDetail.value.variables,
      result: activeDetail.value.result,
      explain: activeDetail.value.explain
    }
  }
}

function buildSimulationContext() {
  const record = activeDetailRecord.value
  return {
    sceneId: record.sceneId || queryParams.sceneId || null,
    sceneCode: record.sceneCode || activeSceneOption.value?.sceneCode || '',
    sceneName: record.sceneName || activeSceneOption.value?.sceneName || '',
    versionId: record.versionId || queryParams.versionId || null,
    versionNo: record.versionNo || activeVersionOption.value?.versionNo || '按当前配置',
    billMonth: record.billMonth || queryParams.billMonth || ''
  }
}

function buildSimulationSummaryObject() {
  return {
    totalAmount: resolveAmountTotal(activeDetail.value.result),
    totalAmountText: formatAmount(resolveAmountTotal(activeDetail.value.result)),
    chargeLineCount: resultChargeRows.value.length,
    variableCount: resultVariableRows.value.length,
    matchedFeeCount: matchedFeeRows.value.length || resultChargeRows.value.length,
    policyCount: resultPolicyRows.value.length,
    timelineStepCount: timelineItems.value.length
  }
}

function buildSimulationSummaryText() {
  const record = activeDetailRecord.value
  const context = buildSimulationContext()
  const lines = [
    '试算摘要',
    `生成时间：${formatExportDateTime(new Date())}`,
    `结果来源：${activeResultSourceLabel.value}`,
    `试算编号：${record.simulationNo || '-'}`,
    `场景：${context.sceneName || '-'} (${context.sceneCode || '-'})`,
    `版本：${context.versionNo || '-'}`,
    `账期：${context.billMonth || '-'}`,
    '',
    '结果概览：'
  ]
  resultSummaryItems.value.forEach(item => {
    lines.push(`${item.label}：${item.value}；${item.desc}`)
  })
  if (resultChargeRows.value.length) {
    lines.push('', '计费明细：')
    resultChargeRows.value.forEach(item => {
      lines.push(`${item.feeName} (${item.feeCode})，规则 ${item.ruleCode}，数量 ${item.quantitySource}，单价 ${item.unitPriceSource}，金额 ${item.amountText}`)
    })
  }
  if (timelineItems.value.length) {
    lines.push('', '执行步骤：')
    timelineItems.value.forEach(item => {
      lines.push(`STEP ${item.orderNo} ${item.title}：${item.statusText}，${item.summary}`)
    })
  }
  return lines.join('\n')
}

function buildSimulationReportHtml() {
  const context = buildSimulationContext()
  const summaryRows = resultSummaryItems.value.map(item => [item.label, item.value, item.desc])
  const chargeRows = resultChargeRows.value.map(item => [
    item.feeCode,
    item.feeName,
    item.ruleCode,
    item.quantitySource,
    item.unitPriceSource,
    item.amountText
  ])
  const variableRows = resultVariableRows.value.map(item => [item.code, item.name, resolveVariableSourceLabel(item.sourceType), item.displayValue])
  const policyRows = resultPolicyRows.value.map(item => [item.feeCode, item.feeName, item.ruleCode, item.ruleName, item.pricingSourceLabel, item.summary])
  const timelineRows = timelineItems.value.map(item => [`STEP ${item.orderNo}`, item.title, item.statusText, item.summary])

  return `<!DOCTYPE html>
<html lang="zh-CN">
<head>
  <meta charset="UTF-8" />
  <title>试算报告 - ${escapeHtml(activeDetailRecord.value.simulationNo || '当前结果')}</title>
  <style>
    :root { color-scheme: light; font-family: "Microsoft YaHei", "PingFang SC", Arial, sans-serif; color: #2c2c2c; background: #f7f0e4; }
    body { margin: 0; padding: 32px; background: linear-gradient(180deg, #f6eddf 0%, #fffaf2 260px, #fff 100%); }
    main { max-width: 1280px; margin: 0 auto; }
    header, section { border: 1px solid #e4cfaa; border-radius: 24px; background: rgba(255, 252, 246, 0.94); box-shadow: 0 16px 36px rgba(105, 76, 31, 0.08); padding: 24px 28px; margin-bottom: 18px; }
    h1 { margin: 0; font-size: 30px; }
    h2 { margin: 0 0 16px; font-size: 22px; }
    p { line-height: 1.8; color: #67625b; }
    .meta { display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); gap: 12px; margin-top: 20px; }
    .meta div { border: 1px solid #ead8ba; border-radius: 16px; padding: 14px 16px; background: #fff8ed; }
    .meta span { display: block; color: #80786f; font-size: 12px; margin-bottom: 6px; }
    .meta strong { font-size: 18px; }
    table { width: 100%; border-collapse: collapse; overflow: hidden; border-radius: 16px; }
    th, td { border-bottom: 1px solid #eadfd1; padding: 12px 14px; text-align: left; vertical-align: top; }
    th { background: #f4ead9; color: #5a5248; }
    td { background: rgba(255, 255, 255, 0.58); }
    .empty { color: #8d867d; background: #fff8ed; border-radius: 14px; padding: 18px; }
  </style>
</head>
<body>
  <main>
    <header>
      <h1>试算报告</h1>
      <p>用于试算过程留痕、业务复核和规则联调说明。正式结果归档仍以结果台账导出为准。</p>
      <div class="meta">
        <div><span>试算编号</span><strong>${escapeHtml(activeDetailRecord.value.simulationNo || '-')}</strong></div>
        <div><span>场景</span><strong>${escapeHtml(context.sceneName || '-')}</strong></div>
        <div><span>版本</span><strong>${escapeHtml(context.versionNo || '-')}</strong></div>
        <div><span>账期</span><strong>${escapeHtml(context.billMonth || '-')}</strong></div>
        <div><span>总金额</span><strong>${escapeHtml(formatAmount(resolveAmountTotal(activeDetail.value.result)))}</strong></div>
        <div><span>导出时间</span><strong>${escapeHtml(formatExportDateTime(new Date()))}</strong></div>
      </div>
    </header>
    <section>
      <h2>结果概览</h2>
      ${renderHtmlTable(['指标', '值', '说明'], summaryRows)}
    </section>
    <section>
      <h2>计费明细</h2>
      ${renderHtmlTable(['费目编码', '费目名称', '命中规则', '数量来源', '单价来源', '金额'], chargeRows)}
    </section>
    <section>
      <h2>变量求值</h2>
      ${renderHtmlTable(['变量编码', '变量名称', '来源', '值'], variableRows)}
    </section>
    <section>
      <h2>规则命中链路</h2>
      ${renderHtmlTable(['费目编码', '费目名称', '命中规则', '规则名称', '定价来源', '摘要'], policyRows)}
    </section>
    <section>
      <h2>执行步骤</h2>
      ${renderHtmlTable(['步骤', '名称', '状态', '摘要'], timelineRows)}
    </section>
  </main>
</body>
</html>`
}

function setActiveDetail(detail, sourceLabel) {
  activeDetail.value = normalizeDetail(detail)
  activeResultSourceLabel.value = sourceLabel || '当前结果回看'
}

async function syncBatchReplayDetail(records) {
  const candidate = pickBatchReplayCandidate(records)
  if (!candidate?.simulationId) {
    activeDetail.value = createEmptyDetail()
    activeResultSourceLabel.value = '最近批量试算结果'
    return { loaded: false, candidate: null }
  }
  try {
    const response = await getSimulationDetail(candidate.simulationId)
    setActiveDetail(response.data, `批量回归 ${candidate.bizNo || candidate.simulationNo || candidate.simulationId}`)
    return { loaded: true, candidate }
  } catch (error) {
    activeDetail.value = createEmptyDetail()
    activeResultSourceLabel.value = '最近批量试算结果'
    proxy.$modal.msgWarning('批量试算已完成，但自动加载首条回看明细失败，请在下方批量回归摘要中手动点击“回看”。')
    return { loaded: false, candidate, error }
  }
}

function validateBeforeExecute(inputJson, mode) {
  if (!queryParams.sceneId || !queryParams.billMonth) {
    proxy.$modal.msgWarning('请先选择试算场景和账期')
    return false
  }
  const parsed = parseJsonTextSafely(inputJson)
  if (!parsed.valid) {
    proxy.$modal.msgWarning('输入 JSON 还未通过校验，请先修正格式后再执行试算')
    return false
  }
  if (mode === 'SINGLE' && (Array.isArray(parsed.value) || !isPlainObject(parsed.value))) {
    proxy.$modal.msgWarning('单笔试算要求输入 JSON 对象')
    return false
  }
  if (mode === 'BATCH' && (!Array.isArray(parsed.value) || !parsed.value.length)) {
    proxy.$modal.msgWarning('批量试算要求输入非空 JSON 数组')
    return false
  }
  return true
}

function resolveSimulationStatus(value) {
  return simulationStatusOptions.value.find(item => item.value === value)?.label || value || '-'
}

function resolveVariableSourceLabel(value) {
  return variableSourceLabelMap[value] || value || '-'
}

function resolveTemplateRoleLabel(value) {
  return templateRoleLabelMap[value] || value || '-'
}

function resolvePricingSourceLabel(pricing = {}) {
  const source = pricing.pricingSource || pricing.pricingMode
  if (!source) {
    return '未返回'
  }
  return pricingSourceLabelMap[source] || source
}
function resolveQuantitySourceLabel(pricing = {}) {
  if (pricing.quantityVariableCode) {
    return `取变量 ${pricing.quantityVariableCode}`
  }
  if (pricing.pricingSource === 'FIXED_AMOUNT') {
    return '固定金额不取数量'
  }
  if (pricing.quantityValue !== undefined && pricing.quantityValue !== null) {
    return `数量 ${pricing.quantityValue}`
  }
  return '未返回'
}

function formatDateTime(value) {
  if (!value) {
    return '-'
  }
  return proxy.parseTime ? proxy.parseTime(value) : value
}

function formatAmount(value) {
  if (value === null || value === undefined || value === '') {
    return '-'
  }
  const numberValue = Number(value)
  if (!Number.isFinite(numberValue)) {
    return String(value)
  }
  return new Intl.NumberFormat('zh-CN', {
    minimumFractionDigits: Number.isInteger(numberValue) ? 0 : 2,
    maximumFractionDigits: 2
  }).format(numberValue)
}

function formatCellValue(value) {
  if (value === null || value === undefined || value === '') {
    return '-'
  }
  if (typeof value === 'object') {
    try {
      return JSON.stringify(value)
    } catch {
      return String(value)
    }
  }
  return String(value)
}

function getHistoryRowClass({ row }) {
  return activeDetailRecord.value.simulationId === row.simulationId ? 'is-active-row' : ''
}

function getBatchRowClass({ row }) {
  return activeDetailRecord.value.simulationId === row.simulationId ? 'is-active-row' : ''
}

function buildTimelineItems(explain, matchedFees) {
  const primaryTimeline = normalizeArray(explain.timeline)
  if (primaryTimeline.length) {
    return primaryTimeline.map((item, index) => normalizeTimelineItem(item, index))
  }
  return matchedFees.flatMap((fee, feeIndex) => {
    return normalizeArray(fee.timeline).map((item, stepIndex) => normalizeTimelineItem(item, feeIndex * 100 + stepIndex, fee))
  })
}

function normalizeTimelineItem(item, index, feeContext = {}) {
  const step = normalizeObject(item)
  const title = step.objectName || step.stepName || step.stepType || `步骤 ${index + 1}`
  const statusClass = resolveTimelineStatusClass(step.stepType || step.status)
  const statusText = resolveTimelineStatusText(step.stepType || step.status)
  const tokens = []
  if (step.objectCode) {
    tokens.push(`对象 ${step.objectCode}`)
  }
  if (step.stepType) {
    tokens.push(`类型 ${step.stepType}`)
  }
  if (feeContext.feeCode) {
    tokens.push(`费目 ${feeContext.feeCode}`)
  }
  return {
    id: `${step.objectCode || step.stepType || 'timeline'}-${index}`,
    orderNo: index + 1,
    title,
    summary: step.summary || '暂无补充说明',
    statusClass,
    statusText,
    tokens
  }
}

function resolveTimelineStatusClass(value) {
  const text = String(value || '').toUpperCase()
  if (text.includes('FAIL')) {
    return 'danger'
  }
  if (text.includes('SKIP')) {
    return 'info'
  }
  return 'success'
}

function resolveTimelineStatusText(value) {
  const text = String(value || '').toUpperCase()
  if (text.includes('FAIL')) {
    return '失败'
  }
  if (text.includes('SKIP')) {
    return '跳过'
  }
  return '完成'
}

function inspectInputStructure(value) {
  const text = String(value || '').trim()
  if (!text) {
    return {
      value: '未生成',
      desc: '选择场景后生成建议入参，或手工填写 JSON'
    }
  }
  try {
    const parsed = JSON.parse(text)
    if (Array.isArray(parsed)) {
      return {
        value: `${parsed.length} 条`,
        desc: parsed.length ? '当前是对象数组，适合直接做批量试算' : '当前是空数组，建议先补一条样例'
      }
    }
    if (isPlainObject(parsed)) {
      return {
        value: '1 条',
        desc: '当前是单对象，适合做单笔验证和结果解释'
      }
    }
    return {
      value: '结构异常',
      desc: '试算输入必须是 JSON 对象或对象数组'
    }
  } catch {
    return {
      value: '待校验',
      desc: '当前 JSON 还未通过语法校验'
    }
  }
}

function mergeFieldCatalogs(...lists) {
  const merged = new Map()
  lists.flat().forEach((item, index) => {
    if (!item) {
      return
    }
    const key = `${item.variableCode || ''}|${item.path || ''}|${item.templateRole || ''}`
    if (!merged.has(key)) {
      merged.set(key, { ...item, __sourceIndex: index })
    }
  })
  return Array.from(merged.values())
}

function buildPolicySummary(pricing = {}) {
  const parts = []
  if (pricing.quantityValue !== undefined && pricing.quantityValue !== null) {
    parts.push(`数量 ${pricing.quantityValue}`)
  }
  if (pricing.unitPrice !== undefined && pricing.unitPrice !== null) {
    parts.push(`单价 ${pricing.unitPrice}`)
  }
  if (pricing.amountValue !== undefined && pricing.amountValue !== null) {
    parts.push(`金额 ${pricing.amountValue}`)
  }
  if (pricing.formulaCode) {
    parts.push(`公式 ${pricing.formulaCode}`)
  }
  return parts.join(' / ') || '未返回定价摘要'
}

function resolveAmountTotal(result) {
  const payload = normalizeObject(result)
  if (payload.amountTotal !== undefined && payload.amountTotal !== null) {
    return payload.amountTotal
  }
  return normalizeArray(payload.feeResults).reduce((sum, item) => {
    const amount = Number(normalizeObject(item).amountValue || 0)
    return sum + (Number.isFinite(amount) ? amount : 0)
  }, 0)
}

function normalizeDetail(detail) {
  return {
    record: detail?.record || null,
    input: detail?.input ?? null,
    variables: detail?.variables ?? null,
    result: detail?.result ?? null,
    explain: detail?.explain ?? null
  }
}

function createEmptyDetail() {
  return {
    record: null,
    input: null,
    variables: null,
    result: null,
    explain: null
  }
}

function pickBatchReplayCandidate(records) {
  const items = normalizeArray(records)
  return (
    items.find(item => item?.simulationId && !item?.errorMessage && `${item?.status ?? ''}` !== '1') ||
    items.find(item => item?.simulationId) ||
    null
  )
}

function normalizeArray(value) {
  return Array.isArray(value) ? value : []
}

function normalizeObject(value) {
  return isPlainObject(value) ? value : {}
}

function isPlainObject(value) {
  return Object.prototype.toString.call(value) === '[object Object]'
}

function parseJsonTextSafely(text) {
  try {
    return {
      valid: true,
      value: JSON.parse(String(text || ''))
    }
  } catch {
    return {
      valid: false,
      value: null
    }
  }
}

function isFailedSimulationRecord(record) {
  const status = String(record?.status || '').toUpperCase()
  return Boolean(record?.errorMessage) || ['FAILED', 'FAIL', 'ERROR'].includes(status)
}

function resolveBatchExportIds(failedOnly) {
  const records = failedOnly ? batchFailedRecords.value : batchResultRecords.value
  return records
    .map(item => item?.simulationId)
    .filter(Boolean)
    .join(',')
}

function downloadTextFile(fileName, content, mimeType) {
  if (typeof document === 'undefined') {
    return
  }
  const blob = new Blob([content], { type: mimeType })
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = fileName
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
  window.setTimeout(() => URL.revokeObjectURL(url), 0)
}

async function copyTextToClipboard(text) {
  if (typeof navigator !== 'undefined' && navigator.clipboard?.writeText) {
    await navigator.clipboard.writeText(text)
    return
  }
  if (typeof document === 'undefined') {
    return
  }
  const textarea = document.createElement('textarea')
  textarea.value = text
  textarea.setAttribute('readonly', 'readonly')
  textarea.style.position = 'fixed'
  textarea.style.opacity = '0'
  document.body.appendChild(textarea)
  textarea.select()
  document.execCommand('copy')
  document.body.removeChild(textarea)
}

function renderHtmlTable(headers, rows) {
  if (!rows.length) {
    return '<div class="empty">暂无数据</div>'
  }
  return `<table>
    <thead><tr>${headers.map(item => `<th>${escapeHtml(item)}</th>`).join('')}</tr></thead>
    <tbody>
      ${rows.map(row => `<tr>${row.map(item => `<td>${escapeHtml(item)}</td>`).join('')}</tr>`).join('')}
    </tbody>
  </table>`
}

function escapeHtml(value) {
  const text = value === null || value === undefined ? '' : String(value)
  const replacements = {
    '&': '&amp;',
    '<': '&lt;',
    '>': '&gt;',
    '"': '&quot;',
    "'": '&#39;'
  }
  return text.replace(/[&<>"']/g, char => replacements[char])
}

function createExportFileName(prefix, extension) {
  const record = activeDetailRecord.value
  const parts = [
    prefix,
    record.simulationNo,
    record.billMonth || queryParams.billMonth,
    formatFileTimestamp(new Date())
  ].filter(Boolean)
  return `${parts.map(sanitizeFileName).join('_')}.${extension}`
}

function createBatchExportFileName(prefix, extension) {
  const parts = [
    prefix,
    activeSceneOption.value?.sceneCode,
    queryParams.billMonth,
    formatFileTimestamp(new Date())
  ].filter(Boolean)
  return `${parts.map(sanitizeFileName).join('_')}.${extension}`
}

function sanitizeFileName(value) {
  return String(value || '')
    .trim()
    .replace(/[\\/:*?"<>|\s]+/g, '_')
    .replace(/^_+|_+$/g, '')
}

function formatExportDateTime(value) {
  const date = value instanceof Date ? value : new Date(value)
  if (Number.isNaN(date.getTime())) {
    return '-'
  }
  return date.toLocaleString('zh-CN', { hour12: false })
}

function formatFileTimestamp(value) {
  const date = value instanceof Date ? value : new Date(value)
  if (Number.isNaN(date.getTime())) {
    return 'unknown_time'
  }
  const pad = number => String(number).padStart(2, '0')
  return `${date.getFullYear()}${pad(date.getMonth() + 1)}${pad(date.getDate())}_${pad(date.getHours())}${pad(date.getMinutes())}${pad(date.getSeconds())}`
}
</script>

<style scoped lang="scss">
@use '../../../assets/styles/cost-workbench.scss' as costWorkbench;

.simulation-page {
  --sim-bg: var(--el-bg-color-page);
  --sim-card-bg: var(--el-bg-color-overlay);
  --sim-card-soft: color-mix(in srgb, var(--el-bg-color-overlay) 94%, var(--el-color-primary-light-9) 6%);
  --sim-border: var(--el-border-color);
  --sim-shadow: none;
  --sim-accent: var(--el-color-primary);
  --sim-warning: color-mix(in srgb, var(--el-color-warning) 82%, #dd9c2f 18%);
  --sim-text: var(--el-text-color-primary);
  --sim-muted: var(--el-text-color-secondary);
  --sim-pill-bg: color-mix(in srgb, var(--el-bg-color-overlay) 88%, #fff 12%);
  --sim-query-bg: color-mix(in srgb, var(--el-bg-color-overlay) 96%, var(--el-color-primary-light-9) 4%);
  --sim-table-header-bg: var(--el-fill-color-light);
  --sim-table-row-bg: var(--el-bg-color-overlay);
  --sim-table-row-hover-bg: var(--el-fill-color-lighter);
  @include costWorkbench.page-root(18px);
  padding-bottom: 12px;
}

:global(html.dark) .simulation-page {
  --sim-bg: var(--el-bg-color-page);
  --sim-card-bg: var(--el-bg-color-overlay);
  --sim-card-soft: color-mix(in srgb, var(--el-bg-color-overlay) 92%, var(--el-color-primary-light-9) 8%);
  --sim-border: var(--el-border-color);
  --sim-shadow: none;
  --sim-accent: var(--el-color-primary);
  --sim-warning: color-mix(in srgb, var(--el-color-warning) 78%, #ffd089 22%);
  --sim-pill-bg: color-mix(in srgb, var(--el-bg-color-overlay) 84%, #16110c 16%);
  --sim-query-bg: color-mix(in srgb, var(--el-bg-color-overlay) 94%, var(--el-color-primary-light-9) 6%);
  --sim-table-header-bg: var(--el-fill-color-light);
  --sim-table-row-bg: var(--el-bg-color-overlay);
  --sim-table-row-hover-bg: var(--el-fill-color-lighter);
}

.simulation-page__panel,
.simulation-page__workbench,
.simulation-page__timeline-card {
  @include costWorkbench.surface(var(--sim-card-bg), var(--sim-border));
}

.simulation-page__panel,
.simulation-page__workbench {
  padding: 24px 26px;
}

.simulation-page__hero {
  @include costWorkbench.hero(var(--sim-card-bg));
}

.simulation-page__hero-side {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 10px;
}

.simulation-page__entry-guide {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
}

.simulation-page__entry-card {
  @include costWorkbench.surface(var(--sim-card-bg), var(--sim-border));
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 14px;
  padding: 16px 18px;
}

.simulation-page__entry-card.is-active {
  background: color-mix(in srgb, var(--sim-card-soft) 84%, var(--el-color-primary-light-9) 16%);
}

.simulation-page__entry-card strong {
  display: block;
  margin-bottom: 6px;
  color: var(--sim-text);
  font-size: 16px;
}

.simulation-page__entry-card p {
  margin: 0;
  color: var(--sim-muted);
  line-height: 1.6;
}

.simulation-page__eyebrow {
  @include costWorkbench.eyebrow;
  color: var(--sim-accent);
}

.simulation-page__eyebrow--sub {
  margin-bottom: 8px;
}

.simulation-page__title {
  @include costWorkbench.page-title;
  color: var(--sim-text);
}

.simulation-page__subtitle {
  @include costWorkbench.page-subtitle;
  color: var(--sim-muted);
}

.simulation-page__query-shell {
  @include costWorkbench.query-shell(var(--sim-query-bg));
}

.simulation-page__panel-head {
  @include costWorkbench.section-head;
}

.simulation-page__panel-head h3 {
  @include costWorkbench.section-title;
  color: var(--sim-text);
}

.simulation-page__panel-head p {
  @include costWorkbench.section-desc;
  color: var(--sim-muted);
}

.simulation-page__query-form {
  margin-top: 18px;
}

.simulation-page__summary-grid {
  display: grid;
  gap: 14px;
}

.simulation-page__summary-grid--overview {
  @include costWorkbench.metric-grid;
}

.simulation-page__summary-grid--prep,
.simulation-page__summary-grid--batch {
  @include costWorkbench.metric-grid;
  margin-top: 18px;
}

.simulation-page__summary-grid--result {
  grid-template-columns: repeat(6, minmax(0, 1fr));
  margin-top: 18px;
}

.simulation-page__summary-card {
  @include costWorkbench.metric-card(var(--sim-card-bg), var(--sim-border));
  padding: 18px 20px;
}

.simulation-page__summary-card--soft {
  background: var(--sim-card-soft);
}

.simulation-page__summary-card span {
  color: var(--sim-muted);
  font-size: 13px;
}

.simulation-page__summary-card strong {
  @include costWorkbench.metric-value(28px);
  color: var(--sim-accent);
}

.simulation-page__summary-card small {
  color: var(--sim-muted);
  line-height: 1.7;
}

.simulation-page__alert {
  margin-top: 16px;
}

.simulation-page__alert--inline {
  margin-top: 14px;
}

.simulation-page__table {
  margin-top: 18px;
}

.simulation-page__workspace,
.simulation-page__result-grid {
  display: grid;
  gap: 18px;
}

.simulation-page__workspace {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.simulation-page__result-grid {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.simulation-page__workbench {
  display: flex;
  flex-direction: column;
  gap: 16px;
  min-width: 0;
}

.simulation-page__panel-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-height: 36px;
  padding: 0 14px;
  border-radius: 999px;
  border: 1px solid var(--sim-border);
  color: var(--sim-text);
  background: var(--sim-pill-bg);
  white-space: nowrap;
}

.simulation-page__panel-badge--warning {
  color: var(--sim-warning);
  border-color: color-mix(in srgb, var(--sim-warning) 58%, var(--sim-border) 42%);
  background: color-mix(in srgb, var(--sim-card-soft) 76%, var(--sim-warning) 24%);
}

.simulation-page__context-strip,
.simulation-page__token-row {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.simulation-page__context-strip span,
.simulation-page__token {
  display: inline-flex;
  align-items: center;
  min-height: 30px;
  padding: 0 12px;
  border-radius: 999px;
  background: var(--sim-pill-bg);
  border: 1px solid var(--sim-border);
  color: var(--sim-muted);
  font-size: 12px;
}

.simulation-page__action-row {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.simulation-page__action-row--workbench {
  padding-top: 2px;
}

.simulation-page__timeline-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 14px;
  margin-top: 18px;
}

.simulation-page__timeline-card {
  padding: 18px 20px;
}

.simulation-page__timeline-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
}

.simulation-page__timeline-order {
  font-size: 12px;
  color: var(--sim-muted);
  letter-spacing: 0;
}

.simulation-page__timeline-status {
  display: inline-flex;
  align-items: center;
  min-height: 28px;
  padding: 0 10px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 600;
}

.simulation-page__timeline-status.is-success {
  background: color-mix(in srgb, var(--el-color-success-light-8) 72%, var(--el-bg-color-overlay) 28%);
  color: var(--el-color-success);
}

.simulation-page__timeline-status.is-info {
  background: color-mix(in srgb, var(--el-color-info-light-8) 72%, var(--el-bg-color-overlay) 28%);
  color: var(--el-color-info);
}

.simulation-page__timeline-status.is-danger {
  background: color-mix(in srgb, var(--el-color-danger-light-8) 72%, var(--el-bg-color-overlay) 28%);
  color: var(--el-color-danger);
}

.simulation-page__timeline-card h4 {
  margin: 14px 0 8px;
  font-size: 20px;
  color: var(--sim-text);
}

.simulation-page__timeline-card p {
  margin: 0;
  color: var(--sim-muted);
  line-height: 1.8;
}

.simulation-page__panel--placeholder {
  padding-block: 30px;
}

.simulation-page__panel--history {
  overflow: hidden;
}

.simulation-page__pagination {
  margin-top: 18px;
}

.simulation-page__drawer-desc {
  margin-bottom: 18px;
}

.simulation-page__drawer-tabs {
  min-height: 520px;
}

.simulation-page :deep(.json-editor) {
  max-width: 100%;
  min-width: 0;
  border-color: var(--sim-border);
  background: color-mix(in srgb, var(--sim-card-bg) 92%, var(--el-bg-color-overlay) 8%);
  box-shadow: none;
}

.simulation-page :deep(.json-editor__toolbar),
.simulation-page :deep(.json-editor__footer) {
  background: color-mix(in srgb, var(--sim-card-soft) 82%, transparent);
}

.simulation-page :deep(.el-table) {
  --el-table-header-bg-color: var(--sim-table-header-bg);
  --el-table-tr-bg-color: var(--sim-table-row-bg);
  --el-table-border-color: var(--sim-border);
  --el-table-row-hover-bg-color: var(--sim-table-row-hover-bg);
}

.simulation-page :deep(.el-table .is-active-row td.el-table__cell) {
  background: var(--sim-table-row-hover-bg);
}

.simulation-page.is-compact-mode {
  gap: 12px;
  background: var(--el-bg-color-page);
}

.simulation-page.is-compact-mode .simulation-page__query-shell,
.simulation-page.is-compact-mode .simulation-page__panel,
.simulation-page.is-compact-mode .simulation-page__workbench {
  border-radius: 18px;
  padding: 16px 18px;
  box-shadow: none;
}

.simulation-page.is-compact-mode .simulation-page__query-shell .simulation-page__panel-head > div,
.simulation-page.is-compact-mode .simulation-page__panel-head p,
.simulation-page.is-compact-mode .simulation-page__context-strip,
.simulation-page.is-compact-mode .simulation-page__summary-grid--prep {
  display: none;
}

.simulation-page.is-compact-mode .simulation-page__query-form,
.simulation-page.is-compact-mode .simulation-page__table,
.simulation-page.is-compact-mode .simulation-page__summary-grid--result,
.simulation-page.is-compact-mode .simulation-page__summary-grid--batch {
  margin-top: 12px;
}

.simulation-page.is-compact-mode .simulation-page__panel-head h3 {
  font-size: 18px;
}

.simulation-page.is-compact-mode .simulation-page__summary-card {
  padding: 14px 16px;
}

.simulation-page.is-compact-mode .simulation-page__summary-card strong {
  font-size: 24px;
}

@media (max-width: 1600px) {
  .simulation-page__summary-grid--result {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }

  .simulation-page__timeline-grid {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
}

@media (max-width: 1280px) {
  .simulation-page__hero,
  .simulation-page__entry-guide,
  .simulation-page__workspace,
  .simulation-page__result-grid,
  .simulation-page__summary-grid--overview,
  .simulation-page__summary-grid--prep,
  .simulation-page__summary-grid--batch,
  .simulation-page__summary-grid--result,
  .simulation-page__timeline-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 960px) {
  .simulation-page {
    min-height: auto;
  }

  .simulation-page__hero,
  .simulation-page__entry-guide,
  .simulation-page__workspace,
  .simulation-page__result-grid,
  .simulation-page__summary-grid--overview,
  .simulation-page__summary-grid--prep,
  .simulation-page__summary-grid--batch,
  .simulation-page__summary-grid--result,
  .simulation-page__timeline-grid {
    grid-template-columns: 1fr;
  }

  .simulation-page__panel-head,
  .simulation-page__hero {
    grid-template-columns: 1fr;
    flex-direction: column;
  }

  .simulation-page__hero-side {
    justify-content: flex-start;
  }

  .simulation-page__query-shell {
    position: static;
  }
}

@media (max-width: 768px) {
  .simulation-page__hero,
  .simulation-page__panel,
  .simulation-page__query-shell,
  .simulation-page__workbench {
    padding: 18px;
    border-radius: 22px;
  }

  .simulation-page__title {
    font-size: 28px;
  }

  .simulation-page__summary-card strong {
    font-size: 24px;
  }

  .simulation-page__action-row .el-button {
    flex: 1 1 100%;
  }
}
</style>

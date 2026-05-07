<template>
  <div class="app-container fee-center">
    <section v-show="!isCompactMode" class="fee-center__hero">
      <div>
        <div class="fee-center__eyebrow">费目治理</div>
        <h2 class="fee-center__title">费用中心</h2>
        <p class="fee-center__subtitle">
          围绕场景统一维护费用对象、计费口径和业务说明，为规则配置、版本发布和结果查询提供稳定基础。
        </p>
      </div>
      <el-tag type="info">删除前自动校验关联规则、版本和结果记录，保障费用口径稳定</el-tag>
    </section>

    <section v-show="!isCompactMode" class="fee-center__metrics">
      <div v-for="item in metricItems" :key="item.label" class="fee-center__metric-card">
        <span class="fee-center__metric-label">{{ item.label }}</span>
        <strong class="fee-center__metric-value">{{ item.value }}</strong>
        <span class="fee-center__metric-desc">{{ item.desc }}</span>
      </div>
    </section>

    <el-form ref="queryRef" :model="queryParams" :inline="true" label-width="84px" v-show="showSearch">
      <el-form-item label="所属场景" prop="sceneId">
        <el-select v-model="queryParams.sceneId" placeholder="请选择场景" clearable filterable style="width: 230px" @change="handleSceneChange">
          <el-option v-for="item in sceneOptions" :key="item.sceneId" :label="`${item.sceneName} / ${item.sceneCode}`" :value="item.sceneId" />
        </el-select>
      </el-form-item>
      <el-form-item label="业务域" prop="businessDomain">
        <el-select v-model="queryParams.businessDomain" placeholder="请选择业务域" clearable style="width: 190px">
          <el-option v-for="item in businessDomainOptions" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="费用编码" prop="feeCode">
        <el-input v-model="queryParams.feeCode" placeholder="请输入费用编码" clearable style="width: 200px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="费用名称" prop="feeName">
        <el-input v-model="queryParams.feeName" placeholder="请输入费用名称" clearable style="width: 200px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-select v-model="queryParams.status" placeholder="请选择状态" clearable style="width: 160px">
          <el-option v-for="item in feeStatusOptions" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button type="primary" plain icon="Plus" @click="handleAdd" v-hasPermi="['cost:fee:add']">新增费用</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="success" plain icon="Edit" :disabled="single" @click="handleUpdate" v-hasPermi="['cost:fee:edit']">修改费用</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="danger" plain icon="Delete" :disabled="multiple" @click="handleDelete" v-hasPermi="['cost:fee:remove']">删除费用</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="warning" plain icon="Download" @click="handleExport" v-hasPermi="['cost:fee:export']">导出</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList" />
    </el-row>

    <el-table v-loading="loading" :data="feeList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column type="index" label="序号" width="70" align="center" />
      <el-table-column label="所属场景" min-width="220" align="center">
        <template #default="scope">
          <span>{{ scope.row.sceneCode }} / {{ scope.row.sceneName }}</span>
        </template>
      </el-table-column>
      <el-table-column label="业务域" prop="businessDomain" width="130" align="center">
        <template #default="scope">
          <dict-tag :options="businessDomainOptions" :value="scope.row.businessDomain" />
        </template>
      </el-table-column>
      <el-table-column label="费用编码" prop="feeCode" min-width="150" align="center" />
      <el-table-column label="费用名称" prop="feeName" min-width="160" align="center" :show-overflow-tooltip="true" />
      <el-table-column label="对象维度" min-width="180" align="center">
        <template #default="scope">
          <span>{{ resolveObjectDimensionLabel(scope.row) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="计价单位" prop="unitCode" width="120" align="center">
        <template #default="scope">
          <div class="fee-center__unit-cell">
            <span>{{ resolveUnitLabel(scope.row.unitCode) }}</span>
            <small>{{ resolveUnitSemantic(scope.row.unitCode).summary }}</small>
          </div>
        </template>
      </el-table-column>
      <el-table-column label="费用分类" prop="feeCategory" width="140" align="center" />
      <el-table-column label="影响因素摘要" prop="factorSummary" min-width="180" align="center" :show-overflow-tooltip="true" />
      <el-table-column label="状态" prop="status" width="110" align="center">
        <template #default="scope">
          <dict-tag :options="feeStatusOptions" :value="scope.row.status" />
        </template>
      </el-table-column>
      <el-table-column label="更新时间" prop="updateTime" width="180" align="center">
        <template #default="scope">
          <span>{{ parseTime(scope.row.updateTime) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center" width="210" fixed="right" class-name="small-padding fixed-width">
        <template #default="scope">
          <div class="cost-row-actions">
            <el-button link type="primary" icon="View" @click="handleGovernance(scope.row)">治理</el-button>
            <el-button link type="primary" icon="Edit" @click="handleUpdate(scope.row)" v-hasPermi="['cost:fee:edit']">修改</el-button>
            <el-dropdown trigger="click" @command="command => handleFeeRowCommand(command, scope.row)">
              <el-button link type="primary" icon="MoreFilled">更多</el-button>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item command="delete" icon="Delete" v-hasPermi="['cost:fee:remove']">删除费用</el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </div>
        </template>
      </el-table-column>
      <template #empty>
        <cost-table-empty
          title="当前没有费用数据"
          description="费用是规则、发布和核算结果的主线。可以先新增费用，或清空筛选条件查看全部费用。"
        >
          <el-button type="primary" plain icon="Plus" @click="handleAdd" v-hasPermi="['cost:fee:add']">新增费用</el-button>
          <el-button icon="Refresh" @click="resetQuery">清空筛选</el-button>
        </cost-table-empty>
      </template>
    </el-table>

    <pagination v-show="total > 0" :total="total" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" @pagination="getList" />

    <el-dialog :title="title" v-model="open" width="700px" append-to-body>
      <el-form ref="feeRef" :model="form" :rules="rules" label-width="110px">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="所属场景" prop="sceneId">
              <el-select v-model="form.sceneId" placeholder="请选择场景" filterable style="width: 100%">
                <el-option v-for="item in sceneOptions" :key="item.sceneId" :label="`${item.sceneName} / ${item.sceneCode}`" :value="item.sceneId" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="状态" prop="status">
              <el-radio-group v-model="form.status">
                <el-radio v-for="item in feeStatusOptions" :key="item.value" :value="item.value">{{ item.label }}</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="费用编码" prop="feeCode">
              <el-input v-model="form.feeCode" placeholder="如：PORT_LOAD_FEE" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="费用名称" prop="feeName">
              <el-input v-model="form.feeName" placeholder="请输入费用名称" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="费用分类" prop="feeCategory">
              <el-input v-model="form.feeCategory" placeholder="如：港杂费、固定薪资" />
              <div class="fee-center__field-tip">
                <strong>{{ currentFeeCategoryHint.title }}</strong>
                <span>{{ currentFeeCategoryHint.description }}</span>
              </div>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="计价单位" prop="unitCode">
              <el-select v-model="form.unitCode" filterable clearable style="width: 100%" placeholder="请选择计价单位">
                <el-option v-for="item in unitOptionsForForm" :key="item.value" :label="item.label" :value="item.value" />
              </el-select>
              <div v-if="form.unitCode" class="fee-center__field-tip">
                <strong>{{ currentUnitSemantic.summary }}</strong>
                <span>计量变量建议：{{ currentUnitSemantic.quantityHint }}</span>
                <span>结果解释：{{ currentUnitSemantic.resultHint }}</span>
              </div>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="对象维度" prop="objectDimension">
              <el-select
                v-model="form.objectDimension"
                filterable
                allow-create
                clearable
                default-first-option
                style="width: 100%"
                placeholder="请选择或录入对象维度"
              >
                <el-option v-for="item in objectDimensionOptions" :key="item" :label="item" :value="item" />
              </el-select>
              <div class="fee-center__field-tip">
                <strong>{{ currentObjectDimensionHint.title }}</strong>
                <span>{{ currentObjectDimensionHint.description }}</span>
              </div>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="排序号" prop="sortNo">
              <el-input-number v-model="form.sortNo" :min="1" :max="9999" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="影响因素摘要" prop="factorSummary">
              <el-input v-model="form.factorSummary" placeholder="补充费用依赖的关键变量因素，如重量、作业次数、班次、里程" />
              <div class="fee-center__example-tags">
                <span>常用示例</span>
                <el-tag
                  v-for="item in factorSummaryExamples"
                  :key="item"
                  size="small"
                  effect="plain"
                  @click="applyFactorSummaryExample(item)"
                >
                  {{ item }}
                </el-tag>
              </div>
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="适用范围说明" prop="scopeDescription">
              <el-input v-model="form.scopeDescription" placeholder="补充适用边界，例如客户、合同、业务范围" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="备注" prop="remark">
              <el-input v-model="form.remark" type="textarea" :rows="3" placeholder="补充业务口径说明" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button type="primary" @click="submitForm">确 定</el-button>
          <el-button @click="cancel">取 消</el-button>
        </div>
      </template>
    </el-dialog>

    <el-drawer v-model="governanceOpen" title="费用详情与治理" size="780px" append-to-body>
      <div v-loading="governanceLoading" class="fee-governance" v-if="governanceInfo.feeId">
        <div class="fee-governance__header">
          <div>
            <div class="fee-governance__title">{{ governanceInfo.feeName }}</div>
            <div class="fee-governance__meta">{{ governanceInfo.feeCode }} · {{ governanceInfo.sceneCode }} / {{ governanceInfo.sceneName }}</div>
          </div>
          <dict-tag :options="feeStatusOptions" :value="governanceInfo.status" />
        </div>
        <div class="fee-governance__grid">
          <div class="fee-governance__card"><span>规则引用</span><strong>{{ governanceInfo.ruleCount }}</strong></div>
          <div class="fee-governance__card"><span>变量关系</span><strong>{{ governanceInfo.variableRelCount }}</strong></div>
          <div class="fee-governance__card"><span>版本引用</span><strong>{{ governanceInfo.publishedVersionCount }}</strong></div>
          <div class="fee-governance__card"><span>结果引用</span><strong>{{ governanceInfo.resultLedgerCount }}</strong></div>
        </div>
        <el-descriptions :column="2" border class="fee-governance__detail">
          <el-descriptions-item label="业务域">{{ resolveDictLabel(businessDomainOptions, governanceInfo.businessDomain) }}</el-descriptions-item>
          <el-descriptions-item label="费用分类">{{ governanceInfo.feeCategory || '-' }}</el-descriptions-item>
          <el-descriptions-item label="计价单位">{{ resolveUnitLabel(governanceInfo.unitCode) }}</el-descriptions-item>
          <el-descriptions-item label="对象维度">{{ resolveObjectDimensionLabel(governanceInfo) }}</el-descriptions-item>
          <el-descriptions-item label="影响因素" :span="2">{{ governanceInfo.factorSummary || '-' }}</el-descriptions-item>
          <el-descriptions-item label="适用范围" :span="2">{{ governanceInfo.scopeDescription || '-' }}</el-descriptions-item>
          <el-descriptions-item label="备注" :span="2">{{ governanceInfo.remark || '-' }}</el-descriptions-item>
        </el-descriptions>
        <el-alert :title="governanceInfo.canDelete ? '允许删除' : '当前不允许删除'" :description="governanceInfo.removeBlockingReason" :type="governanceInfo.canDelete ? 'success' : 'warning'" :closable="false" show-icon />
        <el-alert :title="governanceInfo.canDisable ? '允许停用' : '当前不允许停用'" :description="governanceInfo.disableBlockingReason" :type="governanceInfo.canDisable ? 'success' : 'warning'" :closable="false" show-icon />
        <GovernanceImpactList :impacts="governanceInfo.impactItems" :context="governanceInfo" />
        <div class="fee-governance__section">
          <div class="fee-governance__section-head">
            <div>
              <strong>规则汇总</strong>
              <small>当前费用已挂载的规则、条件和阶梯规模</small>
            </div>
            <el-tag size="small" type="info">{{ feeRuleSummary }}</el-tag>
          </div>
          <el-empty v-if="!governanceInfo.ruleSummaries.length" description="当前费用暂无规则" :image-size="56" />
          <div v-else class="fee-governance__rule-list">
            <div v-for="item in governanceInfo.ruleSummaries" :key="item.ruleId" class="fee-governance__rule-item">
              <div class="fee-governance__rule-main">
                <div>
                  <span>{{ item.ruleCode || '-' }}</span>
                  <strong>{{ item.ruleName || '未命名规则' }}</strong>
                </div>
                <dict-tag :options="ruleStatusOptions" :value="item.status" />
              </div>
              <div class="fee-governance__contract-tags">
                <el-tag size="small" effect="plain">{{ resolveRuleTypeLabel(item.ruleType) }}</el-tag>
                <el-tag size="small" effect="plain">优先级 {{ item.priority || '-' }}</el-tag>
                <el-tag size="small" effect="plain">条件 {{ item.conditionCount || 0 }}</el-tag>
                <el-tag size="small" effect="plain">阶梯 {{ item.tierCount || 0 }}</el-tag>
              </div>
              <div class="fee-governance__contract-meta">
                <span v-if="item.quantityVariableCode">计量变量：{{ item.quantityVariableCode }}</span>
                <span v-if="item.amountFormulaCode">金额公式：{{ item.amountFormulaCode }}</span>
                <span v-if="item.pricingMode">定价模式：{{ item.pricingMode }}</span>
              </div>
            </div>
          </div>
        </div>
        <div class="fee-governance__section">
          <div class="fee-governance__section-head">
            <div>
              <strong>费用输入契约</strong>
              <small>当前费用依赖的变量、来源和维护方式</small>
            </div>
            <el-tag size="small" type="info">{{ feeVariableContractSummary }}</el-tag>
          </div>
          <el-empty v-if="!governanceInfo.variableContracts.length" description="当前费用暂无输入契约" :image-size="56" />
          <div v-else class="fee-governance__contract-list">
            <div
              v-for="item in governanceInfo.variableContracts"
              :key="item.relId || `${item.variableCode}-${item.relationType}`"
              class="fee-governance__contract-item"
            >
              <div class="fee-governance__contract-main">
                <span>{{ item.variableCode || '-' }}</span>
                <strong>{{ item.variableName || '未命名变量' }}</strong>
              </div>
              <div class="fee-governance__contract-tags">
                <el-tag size="small" effect="plain">{{ resolveFeeRelationTypeLabel(item.relationType) }}</el-tag>
                <el-tag size="small" effect="plain" :type="item.sourceType === 'RULE_DERIVED' ? 'success' : 'warning'">
                  {{ resolveFeeContractSourceLabel(item.sourceType) }}
                </el-tag>
                <el-tag size="small" effect="plain">{{ resolveVariableSourceTypeLabel(item.variableSourceType) }}</el-tag>
                <el-tag size="small" effect="plain">{{ resolveVariableDataTypeLabel(item.dataType) }}</el-tag>
              </div>
              <div class="fee-governance__contract-meta">
                <span v-if="item.dataPath">取值路径：{{ item.dataPath }}</span>
                <span v-if="item.sourceRuleCode">来源规则：{{ item.sourceRuleCode }} / {{ item.sourceRuleName || '-' }}</span>
                <span v-else-if="item.sourceCode">来源编码：{{ item.sourceCode }}</span>
                <span v-if="item.remark">备注：{{ item.remark }}</span>
              </div>
            </div>
          </div>
        </div>
        <div class="fee-governance__reference-grid">
          <div class="fee-governance__section">
            <div class="fee-governance__section-head">
              <div>
                <strong>发布引用</strong>
                <small>当前费用进入过的发布版本快照</small>
              </div>
              <el-tag size="small" type="info">{{ feePublishSummary }}</el-tag>
            </div>
            <el-empty v-if="!governanceInfo.publishRefs.length" description="暂无发布引用" :image-size="56" />
            <div v-else class="fee-governance__ref-list">
              <div v-for="item in governanceInfo.publishRefs" :key="item.versionId" class="fee-governance__ref-item">
                <div class="fee-governance__ref-main">
                  <strong>{{ item.versionNo || `#${item.versionId}` }}</strong>
                  <dict-tag :options="publishVersionStatusOptions" :value="item.versionStatus" />
                </div>
                <span>{{ parseTime(item.publishedTime) || '-' }}</span>
                <small>{{ item.publishDesc || '无发布说明' }}</small>
              </div>
            </div>
          </div>
          <div class="fee-governance__section">
            <div class="fee-governance__section-head">
              <div>
                <strong>结果引用</strong>
                <small>最近生成的费用结果样例</small>
              </div>
              <el-tag size="small" type="info">{{ feeResultSummary }}</el-tag>
            </div>
            <el-empty v-if="!governanceInfo.resultRefs.length" description="暂无结果引用" :image-size="56" />
            <div v-else class="fee-governance__ref-list">
              <div v-for="item in governanceInfo.resultRefs" :key="item.resultId" class="fee-governance__ref-item">
                <div class="fee-governance__ref-main">
                  <strong>{{ formatMoney(item.amountValue, item.currencyCode) }}</strong>
                  <dict-tag :options="resultStatusOptions" :value="item.resultStatus" />
                </div>
                <span>{{ item.billMonth || '-' }} · {{ item.taskNo || '-' }}</span>
                <small>{{ resolveResultObjectLabel(item) }} / {{ item.bizNo || '-' }}</small>
              </div>
            </div>
          </div>
        </div>
        <div class="fee-governance__advice">
          <p>删除建议：{{ governanceInfo.removeAdvice }}</p>
          <p>停用建议：{{ governanceInfo.disableAdvice }}</p>
        </div>
      </div>
    </el-drawer>
  </div>
</template>

<script setup name="CostFee">
import GovernanceImpactList from '@/components/cost/GovernanceImpactList.vue'
import { addFee, delFee, getFee, getFeeGovernance, getFeeStats, listFee, updateFee } from '@/api/cost/fee'
import { optionselectScene } from '@/api/cost/scene'
import useSettingsStore from '@/store/modules/settings'
import { COST_MENU_ROUTES } from '@/utils/costMenuRoutes'
import { confirmCostDeleteImpact, confirmCostDisableImpact, findFirstDeleteBlockedCheck, findFirstDisableBlockedCheck } from '@/utils/costGovernanceDeletePreview'
import { confirmCostNextAction } from '@/utils/costNextAction'
import { resolveWorkingCostSceneId } from '@/utils/costSceneContext'
import { getCostUnitSemantic } from '@/utils/costUnitSemantics'
import { getRemoteDictOptionMap } from '@/utils/dictRemote'

const { proxy } = getCurrentInstance()
const router = useRouter()
const settingsStore = useSettingsStore()
const isCompactMode = computed(() => settingsStore.costPageMode === 'COMPACT')

const feeList = ref([])
const sceneOptions = ref([])
const businessDomainOptions = ref([])
const feeStatusOptions = ref([])
const unitCodeOptions = ref([])
const variableSourceOptions = ref([])
const variableDataTypeOptions = ref([])
const ruleTypeOptions = ref([])
const ruleStatusOptions = ref([])
const publishVersionStatusOptions = ref([])
const resultStatusOptions = ref([])
const objectDimensionOptions = ['协力队', '协力单位', '班组', '人员', '设备', '船舶', '库区', '订单']
const factorSummaryExamples = ['重量 / 件数 / 作业类型', '班次 / 工时 / 人员级别', '里程 / 车型 / 线路', '设备台时 / 能耗 / 维修类型']
const feeRelationTypeLabels = {
  REQUIRED: '必填输入',
  OPTIONAL: '可选输入',
  TIER_BASIS: '阶梯依据',
  FORMULA_INPUT: '公式输入'
}
const feeContractSourceLabels = {
  RULE_DERIVED: '规则派生',
  MANUAL_REQUIRED: '手工维护'
}
const feeCategoryHints = [
  {
    keyword: '港',
    title: '港杂类费用',
    description: '适合港口作业、堆存、装卸、理货等费用，后续规则通常会依赖重量、箱量、作业类型或库区。'
  },
  {
    keyword: '薪',
    title: '人力薪资类费用',
    description: '适合固定薪资、绩效、补贴等人力成本，后续规则通常会依赖人员、班次、工时或岗位等级。'
  },
  {
    keyword: '设备',
    title: '设备作业类费用',
    description: '适合设备台班、能耗、维修等成本，后续规则通常会依赖设备、台时、能耗或维修类型。'
  },
  {
    keyword: '运输',
    title: '运输配送类费用',
    description: '适合干线、短驳、配送等成本，后续规则通常会依赖里程、车型、线路或装载量。'
  }
]
const open = ref(false)
const loading = ref(true)
const showSearch = ref(true)
const ids = ref([])
const single = ref(true)
const multiple = ref(true)
const total = ref(0)
const title = ref('')
const governanceOpen = ref(false)
const governanceLoading = ref(false)
const initialStatus = ref(undefined)
const governanceInfo = ref({})
const statistics = reactive({ feeCount: 0, enabledFeeCount: 0, sceneCoverageCount: 0 })

const data = reactive({
  form: {},
  queryParams: {
    pageNum: 1,
    pageSize: 10,
    sceneId: undefined,
    businessDomain: undefined,
    feeCode: undefined,
    feeName: undefined,
    status: undefined
  },
  rules: {
    sceneId: [{ required: true, message: '所属场景不能为空', trigger: 'change' }],
    feeCode: [{ required: true, message: '费用编码不能为空', trigger: 'blur' }],
    feeName: [{ required: true, message: '费用名称不能为空', trigger: 'blur' }],
    status: [{ required: true, message: '费用状态不能为空', trigger: 'change' }]
  }
})

const { queryParams, form, rules } = toRefs(data)

const metricItems = computed(() => [
  { label: '费用总数', value: statistics.feeCount, desc: '当前检索条件下的费用数量' },
  { label: '启用费用数', value: statistics.enabledFeeCount, desc: '状态为“正常”的费用数量' },
  { label: '场景覆盖数', value: statistics.sceneCoverageCount, desc: '当前结果覆盖的场景数量' },
  {
    label: '当前检索场景',
    value: currentSceneLabel.value,
    desc: queryParams.value.businessDomain ? `业务域：${resolveDictLabel(businessDomainOptions, queryParams.value.businessDomain)}` : '业务域：全部'
  }
])

const feeVariableContractSummary = computed(() => {
  const contracts = governanceInfo.value.variableContracts || []
  if (!contracts.length) {
    return '0 项输入'
  }
  const derivedCount = contracts.filter(item => item.sourceType === 'RULE_DERIVED').length
  const manualCount = contracts.filter(item => item.sourceType === 'MANUAL_REQUIRED').length
  return `${contracts.length} 项输入 · ${derivedCount} 规则派生 · ${manualCount} 手工维护`
})

const feeRuleSummary = computed(() => {
  const rules = governanceInfo.value.ruleSummaries || []
  const enabledCount = rules.filter(item => item.status === '0').length
  return `${rules.length} 条规则 · ${enabledCount} 启用`
})

const feePublishSummary = computed(() => `${(governanceInfo.value.publishRefs || []).length} 个版本`)

const feeResultSummary = computed(() => `${(governanceInfo.value.resultRefs || []).length} 条样例`)

const unitOptionsForForm = computed(() => {
  const options = [...unitCodeOptions.value]
  const currentValue = form.value?.unitCode
  if (currentValue && !options.some(item => item.value === currentValue)) {
    options.push({ label: currentValue, value: currentValue })
  }
  return options
})
const currentUnitSemantic = computed(() => resolveUnitSemantic(form.value?.unitCode))
const currentSceneDefaultObjectDimension = computed(() => {
  const sceneId = form.value?.sceneId
  if (!sceneId) return ''
  const scene = sceneOptions.value.find(item => item.sceneId === sceneId)
  return scene?.defaultObjectDimension || ''
})
const currentObjectDimensionHint = computed(() => {
  if (form.value?.objectDimension) {
    return {
      title: '当前费用已单独指定对象维度',
      description: `本费用将按“${form.value.objectDimension}”执行，不再继承场景默认维度。`
    }
  }
  if (currentSceneDefaultObjectDimension.value) {
    return {
      title: '当前费用将继承场景默认维度',
      description: `未单独指定时，将继承场景默认对象维度“${currentSceneDefaultObjectDimension.value}”。`
    }
  }
  return {
    title: '建议场景内统一维度口径',
    description: '推荐优先使用：协力队、协力单位、班组、人员、设备、船舶、库区、订单。'
  }
})
const currentFeeCategoryHint = computed(() => {
  const category = form.value?.feeCategory || ''
  const matched = feeCategoryHints.find(item => category.includes(item.keyword))
  return matched || {
    title: '费用分类用于业务归口和后续看板汇总',
    description: '建议填写业务能理解的分类，例如港杂费、固定薪资、设备台班、运输配送，避免只填技术编码。'
  }
})

const currentSceneLabel = computed(() => {
  if (!queryParams.value.sceneId) {
    return '全部场景'
  }
  const scene = sceneOptions.value.find(item => item.sceneId === queryParams.value.sceneId)
  return scene ? scene.sceneName : `#${queryParams.value.sceneId}`
})

async function loadBaseOptions() {
  const [dictMap, sceneResponse] = await Promise.all([
    getRemoteDictOptionMap([
      'cost_business_domain',
      'cost_fee_status',
      'cost_unit_code',
      'cost_variable_source_type',
      'cost_variable_data_type',
      'cost_rule_type',
      'cost_rule_status',
      'cost_publish_version_status',
      'cost_result_status'
    ]),
    optionselectScene({ status: '0', pageNum: 1, pageSize: 1000 })
  ])
  businessDomainOptions.value = dictMap.cost_business_domain || []
  feeStatusOptions.value = dictMap.cost_fee_status || []
  unitCodeOptions.value = dictMap.cost_unit_code || []
  variableSourceOptions.value = dictMap.cost_variable_source_type || []
  variableDataTypeOptions.value = dictMap.cost_variable_data_type || []
  ruleTypeOptions.value = dictMap.cost_rule_type || []
  ruleStatusOptions.value = dictMap.cost_rule_status || []
  publishVersionStatusOptions.value = dictMap.cost_publish_version_status || []
  resultStatusOptions.value = dictMap.cost_result_status || []
  sceneOptions.value = sceneResponse?.data || []
  const preferredSceneId = resolveWorkingCostSceneId(sceneOptions.value, queryParams.value.sceneId)
  queryParams.value.sceneId = preferredSceneId
  if (!form.value.feeId) {
    form.value.sceneId = resolveWorkingCostSceneId(sceneOptions.value, form.value.sceneId, preferredSceneId)
  }
}

function normalizeStats(data = {}) {
  return {
    feeCount: Number(data.feeCount || 0),
    enabledFeeCount: Number(data.enabledFeeCount || 0),
    sceneCoverageCount: Number(data.sceneCoverageCount || 0)
  }
}

async function getList() {
  loading.value = true
  try {
    await loadBaseOptions()
    const [listResponse, statsResponse] = await Promise.all([
      listFee(queryParams.value),
      getFeeStats(queryParams.value)
    ])
    feeList.value = listResponse.rows
    total.value = listResponse.total
    Object.assign(statistics, normalizeStats(statsResponse.data))
  } finally {
    loading.value = false
  }
}

function reset() {
  form.value = {
    feeId: undefined,
    sceneId: undefined,
    feeCode: undefined,
    feeName: undefined,
    feeCategory: undefined,
    unitCode: undefined,
    factorSummary: undefined,
    scopeDescription: undefined,
    objectDimension: undefined,
    sortNo: 10,
    status: '0',
    remark: undefined
  }
  initialStatus.value = undefined
  proxy.resetForm('feeRef')
}

function cancel() {
  open.value = false
  reset()
}

function handleSelectionChange(selection) {
  ids.value = selection.map(item => item.feeId)
  single.value = selection.length !== 1
  multiple.value = !selection.length
}

function handleFeeRowCommand(command, row) {
  if (command === 'delete') {
    handleDelete(row)
  }
}

function applyFactorSummaryExample(value) {
  form.value.factorSummary = value
}

function handleQuery() {
  queryParams.value.pageNum = 1
  getList()
}

function resetQuery() {
  proxy.resetForm('queryRef')
  queryParams.value.pageNum = 1
  queryParams.value.pageSize = 10
  handleQuery()
}

async function handleAdd() {
  await loadBaseOptions()
  reset()
  form.value.sceneId = queryParams.value.sceneId || resolveWorkingCostSceneId(sceneOptions.value)
  open.value = true
  title.value = '新增费用'
}

async function handleUpdate(row) {
  await loadBaseOptions()
  reset()
  const feeId = row?.feeId || ids.value[0]
  const response = await getFee(feeId)
  form.value = { ...response.data }
  initialStatus.value = response.data?.status
  open.value = true
  title.value = '修改费用'
}

function handleSceneChange(sceneId) {
  queryParams.value.sceneId = sceneId
}

function resolveObjectDimensionLabel(row) {
  if (row?.objectDimension) {
    return row.objectDimension
  }
  if (row?.sceneDefaultObjectDimension) {
    return `${row.sceneDefaultObjectDimension}（继承场景）`
  }
  return '-'
}

function submitForm() {
  proxy.$refs.feeRef.validate(async valid => {
    if (!valid) {
      return
    }
    const allowed = await ensureDisableAllowed()
    if (!allowed) {
      return
    }
    const isCreate = !form.value.feeId
    const nextSceneId = form.value.sceneId || queryParams.value.sceneId
    const request = isCreate ? addFee(form.value) : updateFee(form.value)
    await request
    proxy.$modal.msgSuccess(isCreate ? '新增成功' : '修改成功')
    open.value = false
    getList()
    if (isCreate) {
      const goNext = await confirmCostNextAction({
        message: '费用已新增。建议继续为该费用配置规则，否则费用无法进入完整核算链路。',
        confirmButtonText: '去新增规则'
      })
      if (goNext) {
        router.push({ path: COST_MENU_ROUTES.rule, query: nextSceneId ? { sceneId: nextSceneId } : {} })
      }
    }
  })
}

async function handleDelete(row) {
  const targetRows = resolveTargetRows(row)
  if (!targetRows.length) {
    return
  }
  const checks = await Promise.all(targetRows.map(item => fetchFeeGovernance(item.feeId)))
  const allowed = await confirmCostDeleteImpact({
    checks,
    targetLabel: '费用',
    targetNames: targetRows.map(item => item.feeName)
  })
  if (!allowed) {
    const blockedCheck = findFirstDeleteBlockedCheck(checks)
    if (blockedCheck) {
      governanceInfo.value = blockedCheck
      governanceOpen.value = true
    }
    return
  }

  const feeIds = row?.feeId || ids.value
  await delFee(feeIds)
  getList()
  proxy.$modal.msgSuccess('删除成功')
}

function handleExport() {
  proxy.download('cost/fee/export', { ...queryParams.value }, `fee_${new Date().getTime()}.xlsx`)
}

async function handleGovernance(row) {
  governanceLoading.value = true
  governanceOpen.value = true
  try {
    governanceInfo.value = await fetchFeeGovernance(row.feeId)
  } finally {
    governanceLoading.value = false
  }
}

async function fetchFeeGovernance(feeId) {
  const response = await getFeeGovernance(feeId)
  return normalizeGovernanceInfo(response.data)
}

function normalizeGovernanceInfo(data = {}) {
  return {
    feeId: data.feeId,
    sceneId: data.sceneId,
    feeCode: data.feeCode || '',
    feeName: data.feeName || '',
    sceneCode: data.sceneCode || '',
    sceneName: data.sceneName || '',
    businessDomain: data.businessDomain || '',
    sceneDefaultObjectDimension: data.sceneDefaultObjectDimension || '',
    feeCategory: data.feeCategory || '',
    unitCode: data.unitCode || '',
    factorSummary: data.factorSummary || '',
    scopeDescription: data.scopeDescription || '',
    objectDimension: data.objectDimension || '',
    sortNo: data.sortNo,
    status: data.status || '0',
    remark: data.remark || '',
    ruleCount: Number(data.ruleCount || 0),
    variableRelCount: Number(data.variableRelCount || 0),
    publishedVersionCount: Number(data.publishedVersionCount || 0),
    resultLedgerCount: Number(data.resultLedgerCount || 0),
    canDelete: Boolean(data.canDelete),
    canDisable: Boolean(data.canDisable),
    removeBlockingReason: data.removeBlockingReason || '当前费用可以删除',
    disableBlockingReason: data.disableBlockingReason || '当前费用可以停用',
    removeAdvice: data.removeAdvice || '',
    disableAdvice: data.disableAdvice || '',
    impactItems: Array.isArray(data.impactItems) ? data.impactItems : [],
    variableContracts: Array.isArray(data.variableContracts) ? data.variableContracts : [],
    ruleSummaries: Array.isArray(data.ruleSummaries) ? data.ruleSummaries : [],
    publishRefs: Array.isArray(data.publishRefs) ? data.publishRefs : [],
    resultRefs: Array.isArray(data.resultRefs) ? data.resultRefs : []
  }
}

function resolveTargetRows(row) {
  if (row?.feeId) {
    return [row]
  }
  return feeList.value.filter(item => ids.value.includes(item.feeId))
}

function resolveDictLabel(optionsRef, value) {
  const options = Array.isArray(optionsRef) ? optionsRef : (optionsRef?.value || [])
  const match = options.find(item => item.value === value)
  return match ? match.label : value || '-'
}

function resolveLabelByMap(map, value) {
  return map[value] || value || '-'
}

function resolveFeeRelationTypeLabel(value) {
  return resolveLabelByMap(feeRelationTypeLabels, value)
}

function resolveFeeContractSourceLabel(value) {
  return resolveLabelByMap(feeContractSourceLabels, value)
}

function resolveRuleTypeLabel(value) {
  return resolveDictLabel(ruleTypeOptions, value)
}

function resolveVariableSourceTypeLabel(value) {
  return resolveDictLabel(variableSourceOptions, value)
}

function resolveVariableDataTypeLabel(value) {
  return resolveDictLabel(variableDataTypeOptions, value)
}

function resolveUnitLabel(value) {
  return resolveDictLabel(unitCodeOptions, value)
}

function resolveUnitSemantic(value) {
  return getCostUnitSemantic(value, resolveUnitLabel(value))
}

function resolveResultObjectLabel(item = {}) {
  const objectName = item.objectName || item.objectCode
  if (!objectName) {
    return item.objectDimension || '-'
  }
  return item.objectDimension ? `${item.objectDimension}：${objectName}` : objectName
}

function formatMoney(value, currencyCode = 'CNY') {
  const amount = Number(value || 0)
  return `${amount.toFixed(2)} ${currencyCode || 'CNY'}`
}

async function ensureDisableAllowed() {
  if (!form.value.feeId || form.value.status !== '1' || initialStatus.value === '1') {
    return true
  }
  const check = await fetchFeeGovernance(form.value.feeId)
  const checks = [check]
  const allowed = await confirmCostDisableImpact({
    checks,
    targetLabel: '费用',
    targetNames: [form.value.feeName || check.feeName]
  })
  if (!allowed) {
    const blockedCheck = findFirstDisableBlockedCheck(checks)
    if (blockedCheck) {
      governanceInfo.value = blockedCheck
      governanceOpen.value = true
    }
    return false
  }
  return true
}

onActivated(() => {
  getList()
})

getList()
</script>

<style scoped lang="scss">
.fee-center {
  display: grid;
  gap: 16px;
}

.fee-center__hero {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: flex-start;
  padding: 20px 24px;
  border: 1px solid var(--el-border-color);
  border-radius: 16px;
  background: color-mix(in srgb, var(--el-color-primary-light-9) 18%, var(--el-bg-color-overlay));
}

.fee-center__eyebrow { font-size: 12px; color: var(--el-color-primary); font-weight: 700; letter-spacing: 0.08em; text-transform: uppercase; }
.fee-center__title { margin: 8px 0 0; font-size: 26px; }
.fee-center__subtitle { margin: 10px 0 0; color: var(--el-text-color-regular); line-height: 1.8; }
.fee-center__unit-cell { display: grid; gap: 4px; justify-items: center; }
.fee-center__unit-cell small { color: var(--el-text-color-secondary); font-size: 12px; line-height: 1.4; }
.fee-center__field-tip { margin-top: 8px; display: grid; gap: 4px; color: var(--el-text-color-secondary); font-size: 12px; line-height: 1.5; }
.fee-center__field-tip strong { color: var(--el-color-primary-dark-2); font-size: 13px; }
.fee-center__example-tags { display: flex; flex-wrap: wrap; align-items: center; gap: 8px; margin-top: 8px; color: var(--el-text-color-secondary); font-size: 12px; }
.fee-center__example-tags .el-tag { cursor: pointer; }

.fee-center__metrics {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 14px;
}

.fee-center__metric-card {
  display: grid;
  gap: 6px;
  padding: 14px 16px;
  border: 1px solid var(--el-border-color-light);
  border-radius: 12px;
  background: var(--el-bg-color-overlay);
}

.fee-center__metric-label,
.fee-center__metric-desc { font-size: 12px; color: var(--el-text-color-secondary); }
.fee-center__metric-value { font-size: 24px; color: var(--el-color-primary); }

.fee-governance {
  display: grid;
  gap: 12px;
}

.fee-governance__header {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  padding: 14px;
  border: 1px solid var(--el-border-color-light);
  border-radius: 10px;
}

.fee-governance__title { font-size: 16px; font-weight: 700; }
.fee-governance__meta { margin-top: 6px; color: var(--el-text-color-secondary); font-size: 12px; }

.fee-governance__grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
}

.fee-governance__card {
  display: grid;
  gap: 4px;
  padding: 12px;
  border: 1px solid var(--el-border-color-light);
  border-radius: 10px;
  background: var(--el-bg-color-overlay);
}

.fee-governance__card span { color: var(--el-text-color-secondary); font-size: 12px; }
.fee-governance__card strong { color: var(--el-color-primary); font-size: 20px; }
.fee-governance__detail :deep(.el-descriptions__label) {
  width: 116px;
}
.fee-governance__section-head {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: flex-start;
}
.fee-governance__section-head strong { display: block; font-size: 14px; }
.fee-governance__section-head small { display: block; margin-top: 4px; color: var(--el-text-color-secondary); }
.fee-governance__section,
.fee-governance__contract {
  display: grid;
  gap: 10px;
  padding: 12px;
  border: 1px solid var(--el-border-color-light);
  border-radius: 8px;
  background: var(--el-bg-color-overlay);
}
.fee-governance__rule-list,
.fee-governance__contract-list { display: grid; gap: 8px; }
.fee-governance__rule-item,
.fee-governance__contract-item {
  display: grid;
  gap: 8px;
  padding: 10px;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 8px;
  background: var(--el-fill-color-extra-light);
}
.fee-governance__rule-main,
.fee-governance__contract-main {
  display: flex;
  justify-content: space-between;
  gap: 10px;
  align-items: center;
}
.fee-governance__rule-main span,
.fee-governance__contract-main span {
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
  color: var(--el-color-primary);
  word-break: break-all;
}
.fee-governance__rule-main strong,
.fee-governance__contract-main strong {
  text-align: right;
  word-break: break-word;
}
.fee-governance__rule-main strong {
  display: block;
  margin-top: 4px;
  text-align: left;
}
.fee-governance__contract-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}
.fee-governance__contract-meta {
  display: grid;
  gap: 4px;
  color: var(--el-text-color-secondary);
  font-size: 12px;
  line-height: 1.6;
}
.fee-governance__reference-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}
.fee-governance__ref-list {
  display: grid;
  gap: 8px;
}
.fee-governance__ref-item {
  display: grid;
  gap: 5px;
  padding: 10px;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 8px;
  background: var(--el-fill-color-extra-light);
}
.fee-governance__ref-main {
  display: flex;
  justify-content: space-between;
  gap: 8px;
  align-items: center;
}
.fee-governance__ref-item span,
.fee-governance__ref-item small {
  color: var(--el-text-color-secondary);
  font-size: 12px;
  line-height: 1.5;
}
.fee-governance__advice { padding: 12px; border-radius: 10px; background: color-mix(in srgb, var(--el-color-warning-light-9) 40%, var(--el-bg-color-overlay)); }
.fee-governance__advice p { margin: 4px 0; line-height: 1.7; }

@media (max-width: 1200px) {
  .fee-center__metrics { grid-template-columns: repeat(2, minmax(0, 1fr)); }
  .fee-governance__reference-grid { grid-template-columns: 1fr; }
}
</style>

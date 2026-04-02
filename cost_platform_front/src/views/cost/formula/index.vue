<template>
  <div class="app-container formula-lab">
    <section class="formula-lab__hero">
      <div>
        <div class="formula-lab__eyebrow">公式资产工作台</div>
        <h2 class="formula-lab__title">公式实验室</h2>
        <p class="formula-lab__subtitle">
          面向业务人员的公式工作台。先点选变量、条件、函数和模板，再自动生成中文公式与标准表达式，最终以公式编码沉淀为可发布资产。
        </p>
      </div>
      <el-tag type="success">工作台会记住最近一次所选场景，点选变量后可直接生成中文公式和标准表达式</el-tag>
    </section>

    <section class="formula-lab__metrics">
      <div v-for="item in metricItems" :key="item.label" class="formula-lab__metric-card">
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
      <el-form-item label="公式编码" prop="formulaCode">
        <el-input v-model="queryParams.formulaCode" clearable style="width: 180px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="公式名称" prop="formulaName">
        <el-input v-model="queryParams.formulaName" clearable style="width: 180px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-select v-model="queryParams.status" clearable placeholder="请选择状态" style="width: 160px">
          <el-option v-for="item in statusOptions" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="返回类型" prop="returnType">
        <el-select v-model="queryParams.returnType" clearable placeholder="请选择返回类型" style="width: 160px">
          <el-option v-for="item in returnTypeOptions" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <section class="formula-lab__workspace">
      <div class="formula-lab__builder">
        <div class="formula-lab__panel-head">
          <div>
            <h3>公式工作台</h3>
            <p>优先用点选向导生成公式；需要补高级表达式时再切换到专家模式。</p>
          </div>
          <right-toolbar v-model:showSearch="showSearch" @queryTable="getList" />
        </div>

        <div class="formula-lab__toolbar">
          <el-select v-model="form.sceneId" filterable placeholder="请选择场景" style="width: 240px" @change="handleWorkbenchSceneChange">
            <el-option v-for="item in sceneOptions" :key="item.sceneId" :label="`${item.sceneCode} / ${item.sceneName}`" :value="item.sceneId" />
          </el-select>
          <el-segmented v-model="workbench.mode" :options="builderModes" />
          <el-button type="primary" icon="Plus" @click="handleCreate">新建公式</el-button>
          <el-button type="success" icon="Select" @click="handleSave" v-hasPermi="['cost:formula:add']">保存公式</el-button>
          <el-button type="info" icon="Promotion" @click="handleTest" v-hasPermi="['cost:formula:test']">立即试算</el-button>
        </div>

        <el-row :gutter="16">
          <el-col :span="12">
            <el-form ref="formulaRef" :model="form" :rules="rules" label-width="96px">
              <el-form-item label="公式编码" prop="formulaCode"><el-input v-model="form.formulaCode" /></el-form-item>
              <el-form-item label="公式名称" prop="formulaName"><el-input v-model="form.formulaName" /></el-form-item>
              <el-form-item label="返回类型" prop="returnType">
                <el-select v-model="form.returnType" style="width: 100%">
                  <el-option v-for="item in returnTypeOptions" :key="item.value" :label="item.label" :value="item.value" />
                </el-select>
              </el-form-item>
              <el-form-item label="命名空间"><el-input v-model="form.namespaceScope" /></el-form-item>
              <el-form-item label="状态" prop="status">
                <el-radio-group v-model="form.status">
                  <el-radio v-for="item in statusOptions" :key="item.value" :value="item.value">{{ item.label }}</el-radio>
                </el-radio-group>
              </el-form-item>
              <el-form-item label="业务说明"><el-input v-model="form.formulaDesc" type="textarea" :rows="2" /></el-form-item>
            </el-form>
          </el-col>
          <el-col :span="12">
            <div class="formula-lab__preview-card">
              <div class="formula-lab__preview-title">业务公式预览</div>
              <div class="formula-lab__preview-text">{{ derivedFormula.businessFormula || '请从下方工作台点选变量、条件、结果，系统会自动生成中文业务公式。' }}</div>
            </div>
            <div class="formula-lab__preview-card formula-lab__preview-card--code">
              <div class="formula-lab__preview-title">标准表达式预览</div>
              <pre class="formula-lab__code">{{ derivedFormula.formulaExpr || '请先配置公式结构，系统会同步生成标准表达式。' }}</pre>
            </div>
          </el-col>
        </el-row>

        <div class="formula-lab__pattern-bar" v-if="workbench.mode === 'GUIDED'">
          <el-radio-group v-model="workbench.pattern">
            <el-radio-button label="IF_ELSE">条件分支</el-radio-button>
            <el-radio-button label="RANGE_LOOKUP">区间档位</el-radio-button>
          </el-radio-group>
        </div>

        <div v-if="workbench.mode === 'GUIDED'" class="formula-lab__guided">
          <div v-if="workbench.pattern === 'IF_ELSE'">
            <div class="formula-lab__section-title">
              <span>条件配置</span>
              <el-button type="primary" plain icon="Plus" @click="handleAddCondition">新增条件</el-button>
            </div>
            <el-table :data="workbench.conditions" size="small" border>
              <el-table-column label="变量" min-width="220">
                <template #default="scope">
                  <el-select
                    v-model="scope.row.variableCode"
                    filterable
                    :placeholder="resolveVariablePlaceholder()"
                    @change="value => handleConditionVariableChange(scope.row, value)"
                  >
                    <el-option v-for="item in variableOptions" :key="item.variableCode" :label="`${item.variableCode} / ${item.variableName}`" :value="item.variableCode" />
                  </el-select>
                </template>
              </el-table-column>
              <el-table-column label="操作符" width="140">
                <template #default="scope">
                  <el-select v-model="scope.row.operatorCode" placeholder="请选择操作符">
                    <el-option v-for="item in conditionOperators" :key="item.value" :label="item.label" :value="item.value" />
                  </el-select>
                </template>
              </el-table-column>
              <el-table-column label="比较值" min-width="200">
                <template #default="scope">
                  <el-input v-model="scope.row.compareValue" placeholder="如：煤炭 / 白班 / 100" />
                </template>
              </el-table-column>
              <el-table-column label="操作" width="90" align="center">
                <template #default="scope">
                  <el-button link type="danger" icon="Delete" @click="handleRemoveCondition(scope.$index)">删除</el-button>
                </template>
              </el-table-column>
            </el-table>

            <div class="formula-lab__result-grid">
              <div>
                <div class="formula-lab__field-label">命中结果</div>
                <el-input v-model="workbench.trueResultValue" placeholder="如：2 或 V.煤炭单价" />
              </div>
              <div>
                <div class="formula-lab__field-label">未命中结果</div>
                <el-input v-model="workbench.falseResultValue" placeholder="如：1 或 0" />
              </div>
            </div>
          </div>

          <div v-else>
            <div class="formula-lab__result-grid">
              <div>
                <div class="formula-lab__field-label">区间依据变量</div>
                <el-select v-model="workbench.rangeVariableCode" filterable :placeholder="resolveVariablePlaceholder('请选择区间变量')">
                  <el-option v-for="item in numericVariableOptions" :key="item.variableCode" :label="`${item.variableCode} / ${item.variableName}`" :value="item.variableCode" />
                </el-select>
              </div>
              <div>
                <div class="formula-lab__field-label">兜底结果</div>
                <el-input v-model="workbench.defaultResultValue" placeholder="未命中任一档位时返回" />
              </div>
            </div>
            <div class="formula-lab__section-title">
              <span>区间档位</span>
              <el-button type="primary" plain icon="Plus" @click="handleAddRange">新增档位</el-button>
            </div>
            <el-table :data="workbench.ranges" size="small" border>
              <el-table-column label="起始值" min-width="120"><template #default="scope"><el-input v-model="scope.row.startValue" /></template></el-table-column>
              <el-table-column label="结束值" min-width="120"><template #default="scope"><el-input v-model="scope.row.endValue" /></template></el-table-column>
              <el-table-column label="结果值" min-width="160"><template #default="scope"><el-input v-model="scope.row.resultValue" /></template></el-table-column>
              <el-table-column label="操作" width="90" align="center"><template #default="scope"><el-button link type="danger" icon="Delete" @click="handleRemoveRange(scope.$index)">删除</el-button></template></el-table-column>
            </el-table>
          </div>
        </div>

        <div v-else class="formula-lab__expert">
          <el-form label-width="96px">
            <el-form-item label="中文公式"><el-input v-model="form.businessFormula" type="textarea" :rows="3" /></el-form-item>
            <el-form-item label="标准表达式"><el-input v-model="form.formulaExpr" type="textarea" :rows="8" /></el-form-item>
          </el-form>
        </div>

        <div class="formula-lab__test-card">
          <div class="formula-lab__section-title"><span>试算上下文</span><el-button link type="primary" @click="handleGenerateSample">按变量生成示例</el-button></div>
          <el-input v-model="testInputJson" type="textarea" :rows="6" placeholder="请输入测试 JSON，上下文建议按 V/C/I/F/T 命名空间组织。" />
          <div class="formula-lab__test-result">
            <div><strong>试算结果：</strong>{{ testResultDisplay }}</div>
          </div>
        </div>
      </div>

      <aside class="formula-lab__toolbox">
        <div class="formula-lab__panel-head">
          <div>
            <h3>点选工具箱</h3>
            <p>点一下就插入条件、模板、函数或变量，优先服务业务人员配置。</p>
          </div>
        </div>
        <div class="formula-lab__tool-section">
          <div class="formula-lab__tool-title">运算符</div>
          <div class="formula-lab__chip-grid">
            <button v-for="item in operatorButtons" :key="item.value" type="button" class="formula-lab__chip" @click="appendExpertToken(item.value)">{{ item.label }}</button>
          </div>
        </div>
        <div class="formula-lab__tool-section">
          <div class="formula-lab__tool-title">平台模板</div>
          <div class="formula-lab__list">
            <button v-for="item in platformTemplates" :key="item.code" type="button" class="formula-lab__list-item" @click="applyTemplate(item)">
              <strong>{{ item.name }}</strong>
              <span>{{ item.desc }}</span>
            </button>
          </div>
        </div>
        <div class="formula-lab__tool-section">
          <div class="formula-lab__tool-title">场景变量</div>
          <div v-if="variableOptions.length" class="formula-lab__list">
            <button v-for="item in variableOptions" :key="item.variableCode" type="button" class="formula-lab__list-item" @click="appendExpertToken(`V.${item.variableCode}`)">
              <strong>{{ item.variableName }}</strong>
              <span>{{ item.variableCode }}</span>
            </button>
          </div>
          <el-empty v-else :image-size="72" :description="resolveVariablePlaceholder('当前场景暂无变量，请先到变量中心维护')" />
        </div>
        <div class="formula-lab__tool-section">
          <div class="formula-lab__tool-title">函数库</div>
          <div class="formula-lab__list">
            <button v-for="item in functionButtons" :key="item.value" type="button" class="formula-lab__list-item" @click="appendExpertToken(item.value)">
              <strong>{{ item.label }}</strong>
              <span>{{ item.desc }}</span>
            </button>
          </div>
        </div>
        <div class="formula-lab__tool-section">
          <div class="formula-lab__tool-title">已有公式</div>
          <div class="formula-lab__list">
            <button v-for="item in formulaOptionList" :key="item.formulaCode" type="button" class="formula-lab__list-item" @click="handleLoadFormula(item)">
              <strong>{{ item.formulaName }}</strong>
              <span>{{ item.formulaCode }}</span>
            </button>
          </div>
        </div>
      </aside>
    </section>

    <section class="formula-lab__ledger">
      <div class="formula-lab__panel-head">
        <div>
          <h3>公式资产台账</h3>
          <p>台账只负责资产管理，公式生成、预览和试算全部回到上方工作台完成。</p>
        </div>
      </div>

      <el-table v-loading="loading" :data="formulaList" @selection-change="handleSelectionChange">
        <el-table-column type="selection" width="55" align="center" />
        <el-table-column label="场景" min-width="200"><template #default="scope">{{ scope.row.sceneCode }} / {{ scope.row.sceneName }}</template></el-table-column>
        <el-table-column label="公式编码" prop="formulaCode" width="180" />
        <el-table-column label="公式名称" prop="formulaName" min-width="180" />
        <el-table-column label="返回类型" width="120"><template #default="scope"><dict-tag :options="returnTypeOptions" :value="scope.row.returnType" /></template></el-table-column>
        <el-table-column label="状态" width="110"><template #default="scope"><dict-tag :options="statusOptions" :value="scope.row.status" /></template></el-table-column>
        <el-table-column label="变量引用" prop="variableRefCount" width="100" align="center" />
        <el-table-column label="规则引用" prop="ruleRefCount" width="100" align="center" />
        <el-table-column label="操作" width="280" fixed="right" align="center">
          <template #default="scope">
            <el-button link type="primary" icon="Edit" @click="handleEdit(scope.row)">装载编辑</el-button>
            <el-button link type="primary" icon="View" @click="handleGovernance(scope.row)">治理</el-button>
            <el-button link type="danger" icon="Delete" @click="handleDelete(scope.row)" v-hasPermi="['cost:formula:remove']">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <pagination v-show="total > 0" :total="total" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" @pagination="getList" />
    </section>

    <el-drawer v-model="governanceOpen" title="公式治理检查" size="520px" append-to-body>
      <div v-if="governanceInfo.formulaId">
        <el-descriptions :column="1" border>
          <el-descriptions-item label="公式">{{ governanceInfo.formulaCode }} / {{ governanceInfo.formulaName }}</el-descriptions-item>
          <el-descriptions-item label="变量引用">{{ governanceInfo.variableRefCount }}</el-descriptions-item>
          <el-descriptions-item label="规则引用">{{ governanceInfo.ruleRefCount }}</el-descriptions-item>
          <el-descriptions-item label="发布快照引用">{{ governanceInfo.publishedVersionCount }}</el-descriptions-item>
        </el-descriptions>
        <el-alert class="mt12" :title="governanceInfo.canDelete ? '允许删除' : '当前不允许删除'" :description="governanceInfo.removeBlockingReason || '当前公式未被变量、规则和发布快照引用。'" :type="governanceInfo.canDelete ? 'success' : 'warning'" :closable="false" show-icon />
        <el-alert class="mt12" :title="governanceInfo.canDisable ? '允许停用' : '当前不允许停用'" :description="governanceInfo.disableBlockingReason || '当前公式未进入任何发布快照，可以停用。'" :type="governanceInfo.canDisable ? 'success' : 'warning'" :closable="false" show-icon />
      </div>
    </el-drawer>
  </div>
</template>

<script setup name="CostFormula">
import { ElMessageBox } from 'element-plus'
import { addFormula, delFormula, getFormula, getFormulaGovernance, getFormulaStats, listFormula, optionselectFormula, testFormula, updateFormula } from '@/api/cost/formula'
import { optionselectScene } from '@/api/cost/scene'
import { optionselectVariable } from '@/api/cost/variable'
import { resolveWorkingCostSceneId } from '@/utils/costSceneContext'
import { getRemoteDictOptionMap } from '@/utils/dictRemote'

const { proxy } = getCurrentInstance()

const loading = ref(false)
const showSearch = ref(true)
const total = ref(0)
const sceneOptions = ref([])
const variableOptions = ref([])
const formulaOptionList = ref([])
const businessDomainOptions = ref([])
const statusOptions = ref([])
const returnTypeOptions = ref([])
const ids = ref([])
const governanceOpen = ref(false)
const governanceInfo = ref({})
const formulaList = ref([])
const testInputJson = ref('')
const testResult = ref(undefined)

const statistics = reactive({
  formulaCount: 0,
  enabledFormulaCount: 0,
  variableRefCount: 0,
  ruleRefCount: 0
})

const queryParams = reactive({
  pageNum: 1,
  pageSize: 10,
  sceneId: undefined,
  businessDomain: undefined,
  formulaCode: undefined,
  formulaName: undefined,
  status: undefined,
  returnType: undefined
})

const form = reactive({
  formulaId: undefined,
  sceneId: undefined,
  formulaCode: undefined,
  formulaName: undefined,
  formulaDesc: undefined,
  businessFormula: undefined,
  formulaExpr: undefined,
  namespaceScope: 'V,C,I,F,T',
  returnType: 'NUMBER',
  testCaseJson: undefined,
  status: '0',
  sortNo: 10,
  remark: undefined
})

const workbench = reactive({
  mode: 'GUIDED',
  pattern: 'IF_ELSE',
  conditionLogic: 'AND',
  conditions: [],
  trueResultValue: '',
  falseResultValue: '',
  rangeVariableCode: undefined,
  ranges: [],
  defaultResultValue: ''
})

const rules = {
  sceneId: [{ required: true, message: '所属场景不能为空', trigger: 'change' }],
  formulaCode: [{ required: true, message: '公式编码不能为空', trigger: 'blur' }],
  formulaName: [{ required: true, message: '公式名称不能为空', trigger: 'blur' }],
  status: [{ required: true, message: '状态不能为空', trigger: 'change' }],
  returnType: [{ required: true, message: '返回类型不能为空', trigger: 'change' }]
}

const builderModes = [
  { label: '点选向导', value: 'GUIDED' },
  { label: '专家模式', value: 'EXPERT' }
]

const conditionOperators = [
  { label: '等于', value: 'EQ' },
  { label: '不等于', value: 'NE' },
  { label: '大于', value: 'GT' },
  { label: '大于等于', value: 'GE' },
  { label: '小于', value: 'LT' },
  { label: '小于等于', value: 'LE' }
]

const operatorButtons = [
  { label: '+', value: ' + ' },
  { label: '-', value: ' - ' },
  { label: '*', value: ' * ' },
  { label: '/', value: ' / ' },
  { label: '(', value: '(' },
  { label: ')', value: ')' },
  { label: '且', value: ' and ' },
  { label: '或', value: ' or ' }
]

const functionButtons = [
  { label: 'if(cond,a,b)', value: 'if(, , )', desc: '条件判断，适合价格分支。' },
  { label: 'between(x,a,b)', value: 'between(, , )', desc: '区间命中，适合档位判断。' },
  { label: 'round(x,2)', value: 'round(, 2)', desc: '结果保留精度。' },
  { label: 'coalesce(a,b)', value: 'coalesce(, )', desc: '空值兜底。' },
  { label: 'max(a,b)', value: 'max(, )', desc: '取最大值。' },
  { label: 'min(a,b)', value: 'min(, )', desc: '取最小值。' }
]

const platformTemplates = [
  {
    code: 'CARGO_SHIFT_PRICE',
    name: '货种 + 班次计价',
    desc: '适合“货种 = 煤炭 且 班次 = 白班时取 2 元，否则取 1 元”这类业务口径。',
    pattern: 'IF_ELSE',
    conditions: [
      { variableCode: '', operatorCode: 'EQ', compareValue: '' },
      { variableCode: '', operatorCode: 'EQ', compareValue: '' }
    ],
    trueResultValue: '2',
    falseResultValue: '1'
  },
  {
    code: 'RANGE_RATE',
    name: '区间档位取价',
    desc: '适合按天数、重量、面积等数值做区间价。',
    pattern: 'RANGE_LOOKUP',
    ranges: [
      { startValue: '0', endValue: '10', resultValue: '1' },
      { startValue: '10', endValue: '20', resultValue: '2' }
    ],
    defaultResultValue: '0'
  },
  {
    code: 'KEEP_AMOUNT',
    name: '面积 × 天数 × 单价',
    desc: '适合仓储保管费类金额公式，切到专家模式后继续补细节。',
    pattern: 'EXPERT',
    businessFormula: '面积 × 天数 × 单价',
    formulaExpr: 'V.AREA * V.DAYS * V.UNIT_PRICE'
  }
]

const metricItems = computed(() => [
  { label: '公式总数', value: statistics.formulaCount, desc: '当前筛选范围内沉淀的公式资产数' },
  { label: '启用公式', value: statistics.enabledFormulaCount, desc: '状态为正常的公式数量' },
  { label: '变量引用', value: statistics.variableRefCount, desc: '已被变量中心引用的次数' },
  { label: '规则引用', value: statistics.ruleRefCount, desc: '已被规则中心引用的次数' }
])

const variableMetaMap = computed(() => variableOptions.value.reduce((acc, item) => {
  acc[item.variableCode] = item
  return acc
}, {}))

const numericVariableOptions = computed(() => {
  return variableOptions.value.filter(item => ['NUMBER', 'INTEGER', 'DECIMAL', 'LONG'].includes(String(item.dataType || '').toUpperCase()))
})

const derivedFormula = computed(() => {
  if (workbench.mode === 'EXPERT') {
    return {
      businessFormula: (form.businessFormula || '').trim(),
      formulaExpr: (form.formulaExpr || '').trim()
    }
  }
  return workbench.pattern === 'RANGE_LOOKUP' ? buildRangeFormula() : buildIfElseFormula()
})

const testResultDisplay = computed(() => {
  if (typeof testResult.value === 'undefined') {
    return '尚未试算'
  }
  if (typeof testResult.value === 'string') {
    return testResult.value
  }
  return JSON.stringify(testResult.value, null, 2)
})

async function loadBaseOptions() {
  const [dictMap, sceneResponse] = await Promise.all([
    getRemoteDictOptionMap(['cost_business_domain', 'cost_formula_status', 'cost_formula_return_type']),
    optionselectScene({ status: '0', pageNum: 1, pageSize: 1000 })
  ])
  businessDomainOptions.value = dictMap.cost_business_domain || []
  statusOptions.value = dictMap.cost_formula_status || []
  returnTypeOptions.value = dictMap.cost_formula_return_type || []
  sceneOptions.value = sceneResponse?.data || []
  const preferredSceneId = resolveWorkingCostSceneId(sceneOptions.value)
  queryParams.sceneId = preferredSceneId
  form.sceneId = preferredSceneId
}

async function loadSceneAssets(sceneId) {
  if (!sceneId) {
    variableOptions.value = []
    formulaOptionList.value = []
    return
  }
  const [variableResponse, formulaResponse] = await Promise.all([
    optionselectVariable({ sceneId, status: '0', pageNum: 1, pageSize: 1000 }),
    optionselectFormula({ sceneId, status: '0', pageNum: 1, pageSize: 1000 })
  ])
  variableOptions.value = variableResponse?.data || []
  formulaOptionList.value = (formulaResponse?.data || []).filter(item => item.formulaCode !== form.formulaCode)
}

function normalizeStats(data = {}) {
  statistics.formulaCount = Number(data.formulaCount || 0)
  statistics.enabledFormulaCount = Number(data.enabledFormulaCount || 0)
  statistics.variableRefCount = Number(data.variableRefCount || 0)
  statistics.ruleRefCount = Number(data.ruleRefCount || 0)
}

async function getList() {
  loading.value = true
  try {
    const [listResponse, statsResponse] = await Promise.all([
      listFormula(queryParams),
      getFormulaStats(queryParams)
    ])
    formulaList.value = listResponse?.rows || []
    total.value = listResponse?.total || 0
    normalizeStats(statsResponse?.data || {})
  } finally {
    loading.value = false
  }
}

function resetWorkbench() {
  workbench.mode = 'GUIDED'
  workbench.pattern = 'IF_ELSE'
  workbench.conditionLogic = 'AND'
  workbench.conditions = [{ variableCode: '', operatorCode: 'EQ', compareValue: '' }]
  workbench.trueResultValue = ''
  workbench.falseResultValue = ''
  workbench.rangeVariableCode = undefined
  workbench.ranges = [{ startValue: '', endValue: '', resultValue: '' }]
  workbench.defaultResultValue = ''
}

function resetFormModel() {
  const currentSceneId = resolveWorkingCostSceneId(sceneOptions.value) || form.sceneId || queryParams.sceneId
  form.formulaId = undefined
  form.sceneId = currentSceneId
  form.formulaCode = undefined
  form.formulaName = undefined
  form.formulaDesc = undefined
  form.businessFormula = undefined
  form.formulaExpr = undefined
  form.namespaceScope = 'V,C,I,F,T'
  form.returnType = 'NUMBER'
  form.testCaseJson = undefined
  form.status = '0'
  form.sortNo = 10
  form.remark = undefined
  testInputJson.value = ''
  testResult.value = undefined
  resetWorkbench()
  proxy.resetForm('formulaRef')
}

function buildIfElseFormula() {
  const rows = (workbench.conditions || []).filter(item => item.variableCode && item.operatorCode)
  if (!rows.length) {
    return { businessFormula: '', formulaExpr: '' }
  }
  const logicText = workbench.conditionLogic === 'OR' ? '或' : '且'
  const logicExpr = workbench.conditionLogic === 'OR' ? ' or ' : ' and '
  const conditionTexts = rows.map(item => `${resolveVariableLabel(item.variableCode)} ${resolveOperatorLabel(item.operatorCode)} ${formatDisplayValue(item.compareValue)}`)
  const conditionExprs = rows.map(item => buildConditionExpression(item))
  const trueText = formatDisplayValue(workbench.trueResultValue)
  const falseText = formatDisplayValue(workbench.falseResultValue || '0')
  return {
    businessFormula: `当 ${conditionTexts.join(` ${logicText} `)} 时取 ${trueText}，否则取 ${falseText}`,
    formulaExpr: `if(${conditionExprs.join(logicExpr)}, ${normalizeResultToken(workbench.trueResultValue)}, ${normalizeResultToken(workbench.falseResultValue || '0')})`
  }
}

function buildRangeFormula() {
  const variableCode = workbench.rangeVariableCode
  const ranges = (workbench.ranges || []).filter(item => item.startValue !== '' && item.endValue !== '' && item.resultValue !== '')
  if (!variableCode || !ranges.length) {
    return { businessFormula: '', formulaExpr: '' }
  }
  const variableName = resolveVariableLabel(variableCode)
  const businessParts = ranges.map(item => `${item.startValue} - ${item.endValue} 取 ${formatDisplayValue(item.resultValue)}`)
  let expression = normalizeResultToken(workbench.defaultResultValue || '0')
  ;[...ranges].reverse().forEach(item => {
    expression = `if(between(V.${variableCode}, ${item.startValue}, ${item.endValue}), ${normalizeResultToken(item.resultValue)}, ${expression})`
  })
  return {
    businessFormula: `按 ${variableName} 分档：${businessParts.join('；')}；其他取 ${formatDisplayValue(workbench.defaultResultValue || '0')}`,
    formulaExpr: expression
  }
}

function buildConditionExpression(condition) {
  const left = `V.${condition.variableCode}`
  const right = formatExpressionValue(condition.compareValue, condition.variableCode)
  const map = {
    EQ: `${left} == ${right}`,
    NE: `${left} != ${right}`,
    GT: `${left} > ${right}`,
    GE: `${left} >= ${right}`,
    LT: `${left} < ${right}`,
    LE: `${left} <= ${right}`
  }
  return map[condition.operatorCode] || `${left} == ${right}`
}

function formatExpressionValue(value, variableCode) {
  const variableMeta = variableMetaMap.value[variableCode] || {}
  const dataType = String(variableMeta.dataType || '').toUpperCase()
  if (['NUMBER', 'INTEGER', 'DECIMAL', 'LONG'].includes(dataType) && String(value).trim() !== '') {
    return String(value).trim()
  }
  return `'${String(value ?? '').replace(/'/g, "\\'")}'`
}

function normalizeResultToken(value) {
  const text = String(value ?? '').trim()
  if (!text) {
    return '0'
  }
  if (/^(V|C|I|F|T)\./.test(text) || /^if\(/.test(text) || /^between\(/.test(text) || /^round\(/.test(text) || /^coalesce\(/.test(text) || /^max\(/.test(text) || /^min\(/.test(text)) {
    return text
  }
  if (/^-?\d+(\.\d+)?$/.test(text)) {
    return text
  }
  return `'${text.replace(/'/g, "\\'")}'`
}

function formatDisplayValue(value) {
  return String(value ?? '').trim() || '空值'
}

function resolveVariableLabel(variableCode) {
  return variableMetaMap.value[variableCode]?.variableName || variableCode || '未选择变量'
}

function resolveOperatorLabel(operatorCode) {
  return conditionOperators.find(item => item.value === operatorCode)?.label || operatorCode
}

function resolveVariablePlaceholder(emptyText = '当前场景暂无变量，请先到变量中心维护') {
  if (!form.sceneId) {
    return '请先选择场景'
  }
  return variableOptions.value.length ? '请选择变量' : emptyText
}

function appendExpertToken(token) {
  if (workbench.mode !== 'EXPERT') {
    workbench.mode = 'EXPERT'
    form.businessFormula = derivedFormula.value.businessFormula
    form.formulaExpr = derivedFormula.value.formulaExpr
  }
  form.formulaExpr = `${form.formulaExpr || ''}${token}`
}

function applyTemplate(template) {
  if (template.pattern === 'EXPERT') {
    workbench.mode = 'EXPERT'
    form.businessFormula = template.businessFormula
    form.formulaExpr = template.formulaExpr
    return
  }
  workbench.mode = 'GUIDED'
  workbench.pattern = template.pattern
  if (template.pattern === 'IF_ELSE') {
    workbench.conditions = (template.conditions || []).map(item => ({ ...item }))
    workbench.trueResultValue = template.trueResultValue || ''
    workbench.falseResultValue = template.falseResultValue || ''
  } else {
    workbench.ranges = (template.ranges || []).map(item => ({ ...item }))
    workbench.defaultResultValue = template.defaultResultValue || ''
  }
}

function handleAddCondition() {
  workbench.conditions.push({ variableCode: '', operatorCode: 'EQ', compareValue: '' })
}

function handleRemoveCondition(index) {
  workbench.conditions.splice(index, 1)
  if (!workbench.conditions.length) {
    handleAddCondition()
  }
}

function handleConditionVariableChange(row, value) {
  row.variableCode = value
}

function handleAddRange() {
  workbench.ranges.push({ startValue: '', endValue: '', resultValue: '' })
}

function handleRemoveRange(index) {
  workbench.ranges.splice(index, 1)
  if (!workbench.ranges.length) {
    handleAddRange()
  }
}

function buildPayload() {
  const payload = {
    ...form,
    businessFormula: derivedFormula.value.businessFormula || form.businessFormula,
    formulaExpr: derivedFormula.value.formulaExpr || form.formulaExpr,
    testCaseJson: testInputJson.value || undefined
  }
  return payload
}

async function handleSave() {
  await proxy.$refs.formulaRef.validate()
  const payload = buildPayload()
  if (!payload.formulaExpr) {
    proxy.$modal.msgError('请先通过点选向导或专家模式生成标准表达式')
    return
  }
  if (payload.formulaId) {
    await updateFormula(payload)
  } else {
    await addFormula(payload)
  }
  proxy.$modal.msgSuccess(payload.formulaId ? '公式修改成功' : '公式新增成功')
  await getList()
  await loadSceneAssets(payload.sceneId)
}

async function handleTest() {
  const payload = buildPayload()
  if (!payload.formulaExpr) {
    proxy.$modal.msgError('当前没有可测试的公式表达式')
    return
  }
  const response = await testFormula({
    sceneId: payload.sceneId,
    formulaExpr: payload.formulaExpr,
    formulaCode: payload.formulaCode,
    inputJson: testInputJson.value
  })
  testResult.value = response?.data?.result
}

async function handleCreate() {
  resetFormModel()
  if (form.sceneId) {
    queryParams.sceneId = form.sceneId
    await loadSceneAssets(form.sceneId)
  }
}

async function handleEdit(row) {
  const response = await getFormula(row.formulaId)
  Object.assign(form, response.data || {})
  queryParams.sceneId = form.sceneId
  testInputJson.value = form.testCaseJson || ''
  testResult.value = response.data?.sampleResultJson ? safeJsonParse(response.data.sampleResultJson) : undefined
  workbench.mode = 'EXPERT'
  await loadSceneAssets(form.sceneId)
}

async function handleLoadFormula(row) {
  if (!row?.formulaId) {
    return
  }
  await handleEdit(row)
}

async function handleGovernance(row) {
  const response = await getFormulaGovernance(row.formulaId)
  governanceInfo.value = response?.data || {}
  governanceOpen.value = true
}

async function handleDelete(row) {
  await ElMessageBox.confirm(`确认删除公式 ${row.formulaCode} 吗？`, '删除确认', { type: 'warning' })
  await delFormula(row.formulaId)
  proxy.$modal.msgSuccess('删除成功')
  await getList()
  await loadSceneAssets(form.sceneId)
}

function handleSelectionChange(selection) {
  ids.value = selection.map(item => item.formulaId)
}

async function handleGenerateSample() {
  if (!form.sceneId) {
    proxy.$modal.msgWarning('请先选择场景，再按变量生成示例')
    return
  }
  const V = {}
  variableOptions.value.forEach(item => {
    const type = String(item.dataType || '').toUpperCase()
    if (['NUMBER', 'INTEGER', 'DECIMAL', 'LONG'].includes(type)) {
      V[item.variableCode] = 1
    } else if (type === 'BOOLEAN') {
      V[item.variableCode] = true
    } else {
      V[item.variableCode] = item.variableName || item.variableCode
    }
  })
  testInputJson.value = JSON.stringify({ V, C: {}, I: {}, F: {}, T: {} }, null, 2)
}

async function handleWorkbenchSceneChange(sceneId) {
  const workingSceneId = resolveWorkingCostSceneId(sceneOptions.value)
  form.sceneId = workingSceneId
  queryParams.sceneId = workingSceneId
  await loadSceneAssets(workingSceneId)
}

async function handleQuerySceneChange(sceneId) {
  const workingSceneId = resolveWorkingCostSceneId(sceneOptions.value)
  queryParams.sceneId = workingSceneId
  form.sceneId = workingSceneId
  await loadSceneAssets(workingSceneId)
}

function handleQuery() {
  queryParams.pageNum = 1
  getList()
}

function resetQuery() {
  proxy.resetForm('queryRef')
  queryParams.pageNum = 1
  queryParams.pageSize = 10
  handleQuery()
}

function safeJsonParse(text) {
  if (!text) {
    return undefined
  }
  try {
    return typeof text === 'string' ? JSON.parse(text) : text
  } catch (error) {
    return text
  }
}

onMounted(async () => {
  await loadBaseOptions()
  resetFormModel()
  if (queryParams.sceneId) {
    form.sceneId = queryParams.sceneId
    await loadSceneAssets(queryParams.sceneId)
  }
  await getList()
})

onActivated(async () => {
  await loadBaseOptions()
  resetFormModel()
  if (queryParams.sceneId) {
    form.sceneId = queryParams.sceneId
    await loadSceneAssets(queryParams.sceneId)
  }
  await getList()
})
</script>

<style lang="scss" scoped>
.formula-lab {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.formula-lab__hero {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  padding: 28px 32px;
  border-radius: 24px;
  background: linear-gradient(135deg, #f8fbff 0%, #fff7ef 100%);
  border: 1px solid #e7eef7;
}

.formula-lab__eyebrow {
  font-size: 13px;
  font-weight: 600;
  color: #c98a2b;
  margin-bottom: 8px;
}

.formula-lab__title {
  margin: 0 0 10px;
  font-size: 40px;
  line-height: 1.1;
  color: #17233d;
}

.formula-lab__subtitle {
  margin: 0;
  max-width: 880px;
  color: #58627a;
  line-height: 1.8;
}

.formula-lab__metrics {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 16px;
}

.formula-lab__metric-card,
.formula-lab__builder,
.formula-lab__toolbox,
.formula-lab__ledger {
  border-radius: 24px;
  background: #fff;
  border: 1px solid #edf1f7;
  box-shadow: 0 12px 28px rgba(19, 41, 78, 0.06);
}

.formula-lab__metric-card {
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding: 20px 22px;
}

.formula-lab__metric-card span,
.formula-lab__metric-card small {
  color: #6d7890;
}

.formula-lab__metric-card strong {
  font-size: 36px;
  line-height: 1;
  color: #2878ff;
}

.formula-lab__workspace {
  display: grid;
  grid-template-columns: minmax(0, 1.7fr) minmax(320px, 0.9fr);
  gap: 20px;
}

.formula-lab__builder,
.formula-lab__toolbox,
.formula-lab__ledger {
  padding: 24px;
}

.formula-lab__panel-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 20px;
}

.formula-lab__panel-head h3 {
  margin: 0 0 6px;
  font-size: 24px;
  color: #17233d;
}

.formula-lab__panel-head p {
  margin: 0;
  color: #697488;
}

.formula-lab__toolbar,
.formula-lab__pattern-bar,
.formula-lab__result-grid,
.formula-lab__test-result {
  display: flex;
  gap: 12px;
}

.formula-lab__toolbar {
  flex-wrap: wrap;
  margin-bottom: 20px;
}

.formula-lab__preview-card,
.formula-lab__test-card {
  margin-bottom: 16px;
  padding: 18px 20px;
  border-radius: 18px;
  background: #f8fafc;
  border: 1px solid #e8eef7;
}

.formula-lab__preview-card--code {
  background: #0f172a;
  border-color: #152440;
}

.formula-lab__preview-title {
  margin-bottom: 10px;
  font-size: 14px;
  font-weight: 600;
  color: #44516a;
}

.formula-lab__preview-card--code .formula-lab__preview-title {
  color: #cad8ff;
}

.formula-lab__preview-text {
  min-height: 54px;
  color: #1f2937;
  line-height: 1.8;
}

.formula-lab__code {
  margin: 0;
  min-height: 132px;
  color: #f8fafc;
  font-family: 'JetBrains Mono', 'Consolas', monospace;
  white-space: pre-wrap;
  word-break: break-word;
}

.formula-lab__guided,
.formula-lab__expert {
  margin-bottom: 18px;
}

.formula-lab__section-title {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 12px;
  font-size: 15px;
  font-weight: 600;
  color: #1d2a44;
}

.formula-lab__result-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  margin-top: 14px;
}

.formula-lab__field-label,
.formula-lab__tool-title {
  margin-bottom: 8px;
  font-size: 13px;
  font-weight: 600;
  color: #607086;
}

.formula-lab__test-card {
  margin-bottom: 0;
}

.formula-lab__test-result {
  margin-top: 14px;
  padding: 12px 14px;
  border-radius: 14px;
  background: #fff;
  border: 1px dashed #d5dfef;
  color: #32415d;
  white-space: pre-wrap;
}

.formula-lab__toolbox {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.formula-lab__tool-section {
  padding: 16px 18px;
  border-radius: 18px;
  background: linear-gradient(180deg, #fbfdff 0%, #f6f9fe 100%);
  border: 1px solid #e8eef7;
}

.formula-lab__chip-grid {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.formula-lab__chip,
.formula-lab__list-item {
  appearance: none;
  border: 1px solid #d9e2f0;
  background: #fff;
  cursor: pointer;
  transition: all 0.2s ease;
}

.formula-lab__chip {
  padding: 8px 12px;
  border-radius: 999px;
  color: #32415d;
}

.formula-lab__chip:hover,
.formula-lab__list-item:hover {
  border-color: #3b82f6;
  color: #2563eb;
  transform: translateY(-1px);
}

.formula-lab__list {
  display: flex;
  flex-direction: column;
  gap: 10px;
  max-height: 220px;
  overflow: auto;
}

.formula-lab__list-item {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 4px;
  width: 100%;
  padding: 12px 14px;
  border-radius: 16px;
  text-align: left;
}

.formula-lab__list-item strong {
  color: #17233d;
}

.formula-lab__list-item span {
  color: #64748b;
  line-height: 1.6;
}

.mt12 {
  margin-top: 12px;
}

@media (max-width: 1280px) {
  .formula-lab__metrics,
  .formula-lab__workspace {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 768px) {
  .formula-lab__hero {
    flex-direction: column;
    padding: 22px 20px;
  }

  .formula-lab__title {
    font-size: 32px;
  }

  .formula-lab__builder,
  .formula-lab__toolbox,
  .formula-lab__ledger {
    padding: 18px;
  }

  .formula-lab__result-grid {
    grid-template-columns: 1fr;
  }
}
</style>

<template>
  <div class="app-container rule-center">
    <section class="rule-center__hero">
      <div>
        <div class="rule-center__eyebrow">线程三 · 步骤 1-4</div>
        <h2 class="rule-center__title">规则中心</h2>
        <p class="rule-center__subtitle">
          按“先选费用，再维护规则，再维护条件与阶梯”的主线组织页面，当前阶段重点落规则中心、阶梯依据变量、区间校验和复制并改条件值。
        </p>
      </div>
      <el-tag type="warning">当前线程只做规则与阶梯，不越到发布和运行链</el-tag>
    </section>

    <section class="rule-center__metrics">
      <div v-for="item in metricItems" :key="item.label" class="rule-center__metric-card">
        <span class="rule-center__metric-label">{{ item.label }}</span>
        <strong class="rule-center__metric-value">{{ item.value }}</strong>
        <span class="rule-center__metric-desc">{{ item.desc }}</span>
      </div>
    </section>

    <el-form ref="queryRef" :model="queryParams" :inline="true" label-width="84px" v-show="showSearch">
      <el-form-item label="所属场景" prop="sceneId">
        <el-select v-model="queryParams.sceneId" clearable filterable placeholder="请选择场景" style="width: 220px" @change="handleSceneChange">
          <el-option v-for="item in sceneOptions" :key="item.sceneId" :label="`${item.sceneCode} / ${item.sceneName}`" :value="item.sceneId" />
        </el-select>
      </el-form-item>
      <el-form-item label="业务域" prop="businessDomain">
        <el-select v-model="queryParams.businessDomain" clearable placeholder="请选择业务域" style="width: 180px" @change="handleSceneChange">
          <el-option v-for="item in businessDomainOptions" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="规则编码" prop="ruleCode">
        <el-input v-model="queryParams.ruleCode" clearable style="width: 180px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="规则类型" prop="ruleType">
        <el-select v-model="queryParams.ruleType" clearable style="width: 170px">
          <el-option v-for="item in ruleTypeOptions" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-select v-model="queryParams.status" clearable style="width: 150px">
          <el-option v-for="item in ruleStatusOptions" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <section class="rule-center__workspace">
      <aside class="rule-center__fee-panel">
        <div class="rule-center__panel-header">
          <div>
            <h3>费用主线</h3>
            <p>先选费用，再维护该费用下的规则。</p>
          </div>
          <el-input v-model="feeKeyword" clearable placeholder="筛选费用编码/名称" style="width: 220px" />
        </div>
        <div class="rule-center__fee-list" v-loading="feeLoading">
          <button
            v-for="item in filteredFeeOptions"
            :key="item.feeId"
            type="button"
            class="rule-center__fee-item"
            :class="{ 'is-active': selectedFeeId === item.feeId }"
            @click="handleFeeSelect(item)"
          >
            <strong>{{ item.feeName }}</strong>
            <span>{{ item.feeCode }}</span>
            <small>{{ item.sceneName }}</small>
          </button>
          <el-empty v-if="!filteredFeeOptions.length" description="当前筛选下暂无费用" :image-size="80" />
        </div>
      </aside>

      <section class="rule-center__rule-panel">
        <div class="rule-center__panel-header">
          <div>
            <h3>{{ currentFeeTitle }}</h3>
            <p>{{ currentFeeDesc }}</p>
          </div>
          <right-toolbar v-model:showSearch="showSearch" @queryTable="getList" />
        </div>

        <el-row :gutter="10" class="mb8">
          <el-col :span="1.5">
            <el-button type="primary" plain icon="Plus" @click="handleAdd" :disabled="!selectedFeeId" v-hasPermi="['cost:rule:add']">新增规则</el-button>
          </el-col>
          <el-col :span="1.5">
            <el-button type="success" plain icon="Edit" :disabled="single" @click="handleUpdate" v-hasPermi="['cost:rule:edit']">修改规则</el-button>
          </el-col>
          <el-col :span="1.8">
            <el-button type="info" plain icon="CopyDocument" :disabled="single" @click="handleCopy">复制并改条件值</el-button>
          </el-col>
          <el-col :span="1.5">
            <el-button type="danger" plain icon="Delete" :disabled="multiple" @click="handleDelete" v-hasPermi="['cost:rule:remove']">删除规则</el-button>
          </el-col>
          <el-col :span="1.5">
            <el-button type="warning" plain icon="Download" @click="handleExport" v-hasPermi="['cost:rule:export']">导出</el-button>
          </el-col>
        </el-row>

        <el-table v-loading="loading" :data="ruleList" @selection-change="handleSelectionChange">
          <el-table-column type="selection" width="55" align="center" />
          <el-table-column type="index" label="序号" width="70" align="center" />
          <el-table-column label="规则编码" prop="ruleCode" min-width="150" align="center" />
          <el-table-column label="规则名称" prop="ruleName" min-width="160" align="center" :show-overflow-tooltip="true" />
          <el-table-column label="规则类型" prop="ruleType" width="130" align="center">
            <template #default="scope">
              <dict-tag :options="ruleTypeOptions" :value="scope.row.ruleType" />
            </template>
          </el-table-column>
          <el-table-column label="优先级" prop="priority" width="90" align="center" />
          <el-table-column label="计量变量" min-width="150" align="center">
            <template #default="scope">
              <span>{{ scope.row.quantityVariableName || scope.row.quantityVariableCode || '-' }}</span>
            </template>
          </el-table-column>
          <el-table-column label="条件摘要" prop="conditionSummary" min-width="240" align="center" :show-overflow-tooltip="true" />
          <el-table-column label="阶梯数" prop="tierCount" width="90" align="center" />
          <el-table-column label="状态" prop="status" width="100" align="center">
            <template #default="scope">
              <dict-tag :options="ruleStatusOptions" :value="scope.row.status" />
            </template>
          </el-table-column>
          <el-table-column label="更新时间" prop="updateTime" width="180" align="center">
            <template #default="scope">
              <span>{{ parseTime(scope.row.updateTime) }}</span>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="280" fixed="right" align="center">
            <template #default="scope">
              <el-button link type="primary" icon="View" @click="handleGovernance(scope.row)">治理</el-button>
              <el-button link type="primary" icon="Edit" @click="handleUpdate(scope.row)">修改</el-button>
              <el-button link type="primary" icon="CopyDocument" @click="handleCopy(scope.row)">复制</el-button>
              <el-button link type="primary" icon="Delete" @click="handleDelete(scope.row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>

        <pagination v-show="total > 0" :total="total" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" @pagination="getList" />
      </section>
    </section>

    <el-drawer v-model="open" :title="title" size="980px" append-to-body>
      <el-form ref="ruleRef" :model="form" :rules="rules" label-width="108px">
        <el-alert
          :title="form.ruleId ? '正在编辑当前规则' : '正在新增规则'"
          :description="selectedFeeId ? `当前归属费用：${currentFeeTitle}` : '请先选择费用后再维护规则。'"
          type="info"
          :closable="false"
          show-icon
          class="mb16"
        />
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="规则编码" prop="ruleCode">
              <el-input v-model="form.ruleCode" placeholder="如 PORT_WEIGHT_RULE_01" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="规则名称" prop="ruleName">
              <el-input v-model="form.ruleName" placeholder="请输入规则名称" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="规则类型" prop="ruleType">
              <el-select v-model="form.ruleType" style="width: 100%" @change="handleRuleTypeChange">
                <el-option v-for="item in ruleTypeOptions" :key="item.value" :label="item.label" :value="item.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="条件逻辑" prop="conditionLogic">
              <el-radio-group v-model="form.conditionLogic">
                <el-radio v-for="item in conditionLogicOptions" :key="item.value" :value="item.value">{{ item.label }}</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="状态" prop="status">
              <el-radio-group v-model="form.status">
                <el-radio v-for="item in ruleStatusOptions" :key="item.value" :value="item.value">{{ item.label }}</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="优先级" prop="priority">
              <el-input-number v-model="form.priority" :min="0" :max="9999" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="排序号" prop="sortNo">
              <el-input-number v-model="form.sortNo" :min="1" :max="9999" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="计量变量" prop="quantityVariableCode">
              <el-select v-model="form.quantityVariableCode" clearable filterable style="width: 100%" placeholder="公式/阶梯规则必选">
                <el-option v-for="item in variableOptions" :key="item.variableCode" :label="`${item.variableCode} / ${item.variableName}`" :value="item.variableCode" />
              </el-select>
            </el-form-item>
          </el-col>

          <el-col :span="12" v-if="form.ruleType === 'FIXED_RATE'">
            <el-form-item label="固定费率" prop="pricingConfig.rateValue">
              <el-input-number v-model="form.pricingConfig.rateValue" :precision="6" controls-position="right" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12" v-if="form.ruleType === 'FIXED_AMOUNT'">
            <el-form-item label="固定金额" prop="pricingConfig.amountValue">
              <el-input-number v-model="form.pricingConfig.amountValue" :precision="2" controls-position="right" style="width: 100%" />
            </el-form-item>
          </el-col>
          <template v-if="form.ruleType === 'FORMULA'">
            <el-col :span="24">
              <el-form-item label="金额公式" prop="amountFormula">
                <el-input v-model="form.amountFormula" type="textarea" :rows="4" placeholder="示例：if(V.weightTon > 100, V.weightTon * 5, V.weightTon * 6)" />
              </el-form-item>
            </el-col>
            <el-col :span="24">
              <div class="rule-center__formula-helper">
                <span>命名空间提示：`V.` 变量、`I.` 输入、`C.` 上下文、`F.` 费用结果、`T.` 临时值</span>
                <el-button link type="primary" @click="expressionOpen = true">打开表达式助手</el-button>
              </div>
            </el-col>
          </template>
          <el-col :span="24">
            <el-form-item label="说明模板" prop="noteTemplate">
              <el-input v-model="form.noteTemplate" placeholder="例如：按{变量值}命中当前规则" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="备注" prop="remark">
              <el-input v-model="form.remark" type="textarea" :rows="2" placeholder="补充规则业务口径" />
            </el-form-item>
          </el-col>
        </el-row>

        <div class="rule-center__section-title">
          <span>条件编辑区</span>
          <el-button type="primary" plain icon="Plus" @click="handleAddCondition">新增条件</el-button>
        </div>
        <el-table :data="form.conditions" size="small" border>
          <el-table-column label="组号" width="90" align="center">
            <template #default="scope">
              <el-input-number v-model="scope.row.groupNo" :min="1" :max="99" style="width: 100%" />
            </template>
          </el-table-column>
          <el-table-column label="变量" min-width="220" align="center">
            <template #default="scope">
              <el-select v-model="scope.row.variableCode" filterable style="width: 100%" @change="value => handleConditionVariableChange(scope.row, value)">
                <el-option v-for="item in variableOptions" :key="item.variableCode" :label="`${item.variableCode} / ${item.variableName}`" :value="item.variableCode" />
              </el-select>
            </template>
          </el-table-column>
          <el-table-column label="操作符" min-width="150" align="center">
            <template #default="scope">
              <el-select v-model="scope.row.operatorCode" style="width: 100%" @change="value => handleConditionOperatorChange(scope.row, value)">
                <el-option v-for="item in operatorOptions" :key="item.value" :label="item.label" :value="item.value" />
              </el-select>
            </template>
          </el-table-column>
          <el-table-column label="条件值" min-width="240" align="center">
            <template #default="scope">
              <ConditionValueEditor
                v-model="scope.row.compareValue"
                :operator-code="scope.row.operatorCode"
                :variable-meta="variableMetaMap[scope.row.variableCode] || {}"
                :dict-options-map="dynamicDictOptionsMap"
              />
            </template>
          </el-table-column>
          <el-table-column label="显示名称" min-width="160" align="center">
            <template #default="scope">
              <el-input v-model="scope.row.displayName" placeholder="默认带出变量名称" />
            </template>
          </el-table-column>
          <el-table-column label="操作" width="90" align="center" fixed="right">
            <template #default="scope">
              <el-button link type="danger" icon="Delete" @click="handleRemoveCondition(scope.$index)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>

        <template v-if="form.ruleType === 'TIER_RATE'">
          <div class="rule-center__section-title">
            <span>阶梯工作区</span>
            <small>阶梯依据变量：{{ resolveVariableName(form.quantityVariableCode) || '未选择' }}</small>
          </div>
          <div class="rule-center__tier-basis" v-if="selectedTierVariableMeta">
            <div>
              <strong>{{ selectedTierVariableMeta.variableName }}</strong>
              <span>{{ selectedTierVariableMeta.variableCode }}</span>
            </div>
            <div>
              <span>来源：{{ resolveTagLabel(variableSourceOptions, selectedTierVariableMeta.sourceType) || '-' }}</span>
              <span>类型：{{ resolveTagLabel(variableDataTypeOptions, selectedTierVariableMeta.dataType) || '-' }}</span>
              <span>路径：{{ selectedTierVariableMeta.dataPath || '未配置路径' }}</span>
            </div>
          </div>
          <el-alert
            v-if="tierValidationIssues.length"
            :title="`当前阶梯存在 ${tierValidationIssues.length} 项待修正问题`"
            type="warning"
            :closable="false"
            show-icon
            class="mb12"
          >
            <template #default>
              <div v-for="item in tierValidationIssues" :key="item" class="rule-center__issue-item">{{ item }}</div>
            </template>
          </el-alert>
          <RuleTierEditor v-model="form.tiers" :interval-mode-options="intervalModeOptions" />
        </template>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button type="primary" @click="submitForm">确 定</el-button>
          <el-button @click="cancel">取 消</el-button>
        </div>
      </template>
    </el-drawer>

    <el-dialog v-model="copyOpen" title="复制并改条件值" width="920px" append-to-body>
      <el-alert
        title="复制后的新规则会保留原规则结构，仅修改新规则基础信息和条件值。"
        :description="copySourceRule ? `来源规则：${copySourceRule.ruleCode} / ${copySourceRule.ruleName}` : '请先从规则台账中选择要复制的规则。'"
        type="info"
        :closable="false"
        show-icon
        class="mb16"
      />
      <el-form :model="copyForm" :rules="copyRules" ref="copyRef" label-width="120px">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="新规则编码" prop="ruleCode">
              <el-input v-model="copyForm.ruleCode" placeholder="请输入复制后的规则编码" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="新规则名称" prop="ruleName">
              <el-input v-model="copyForm.ruleName" placeholder="请输入复制后的规则名称" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="优先级" prop="priority">
              <el-input-number v-model="copyForm.priority" :min="0" :max="9999" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="排序号" prop="sortNo">
              <el-input-number v-model="copyForm.sortNo" :min="1" :max="9999" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="状态" prop="status">
              <el-radio-group v-model="copyForm.status">
                <el-radio v-for="item in ruleStatusOptions" :key="item.value" :value="item.value">{{ item.label }}</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
          <el-col :span="24" v-if="copySourceRule?.ruleType === 'TIER_RATE'">
            <div class="rule-center__copy-basis">
              <strong>阶梯依据变量</strong>
              <span>{{ resolveVariableName(copySourceRule.quantityVariableCode) || copySourceRule.quantityVariableCode || '未配置' }}</span>
            </div>
          </el-col>
        </el-row>
        <div class="rule-center__section-title">
          <span>条件值调整</span>
          <small>变量、操作符和结构来自原规则，只修改比较值和显示名称。</small>
        </div>
        <el-table :data="copyForm.conditions" size="small" border>
          <el-table-column label="变量" min-width="220" align="center">
            <template #default="scope">
              <div class="rule-center__readonly-cell">
                <strong>{{ resolveVariableName(scope.row.variableCode) }}</strong>
                <span>{{ scope.row.variableCode }}</span>
              </div>
            </template>
          </el-table-column>
          <el-table-column label="操作符" width="120" align="center">
            <template #default="scope">
              <dict-tag :options="operatorOptions" :value="scope.row.operatorCode" />
            </template>
          </el-table-column>
          <el-table-column label="条件值" min-width="240" align="center">
            <template #default="scope">
              <ConditionValueEditor
                v-model="scope.row.compareValue"
                :operator-code="scope.row.operatorCode"
                :variable-meta="variableMetaMap[scope.row.variableCode] || {}"
                :dict-options-map="dynamicDictOptionsMap"
              />
            </template>
          </el-table-column>
          <el-table-column label="显示名称" min-width="180" align="center">
            <template #default="scope">
              <el-input v-model="scope.row.displayName" placeholder="可按新口径覆盖" />
            </template>
          </el-table-column>
        </el-table>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button type="primary" @click="submitCopyForm">生 成 新 规 则</el-button>
          <el-button @click="copyOpen = false">取 消</el-button>
        </div>
      </template>
    </el-dialog>
 
    <el-dialog v-model="expressionOpen" title="表达式助手" width="760px" append-to-body>
      <div class="rule-center__expression-grid">
        <section>
          <h4>命名空间</h4>
          <div class="rule-center__token-list">
            <el-tag v-for="item in namespaceTokens" :key="item.value" class="rule-center__token" type="warning" @click="appendFormula(item.value)">
              {{ item.label }}
            </el-tag>
          </div>
        </section>
        <section>
          <h4>变量引用</h4>
          <div class="rule-center__token-list">
            <el-tag v-for="item in variableOptions" :key="item.variableCode" class="rule-center__token" @click="appendFormula(`V.${item.variableCode}`)">
              V.{{ item.variableCode }}
            </el-tag>
          </div>
        </section>
        <section>
          <h4>常用函数</h4>
          <div class="rule-center__token-list">
            <el-tag v-for="item in formulaFunctions" :key="item.value" class="rule-center__token" type="success" @click="appendFormula(item.value)">
              {{ item.label }}
            </el-tag>
          </div>
        </section>
      </div>
    </el-dialog>

    <el-drawer v-model="governanceOpen" title="规则治理检查" size="500px" append-to-body>
      <div v-loading="governanceLoading" v-if="governanceInfo.ruleId">
        <el-descriptions :column="1" border>
          <el-descriptions-item label="规则">{{ governanceInfo.ruleCode }} / {{ governanceInfo.ruleName }}</el-descriptions-item>
          <el-descriptions-item label="归属费用">{{ governanceInfo.feeCode }} / {{ governanceInfo.feeName }}</el-descriptions-item>
          <el-descriptions-item label="条件数">{{ governanceInfo.conditionCount }}</el-descriptions-item>
          <el-descriptions-item label="阶梯数">{{ governanceInfo.tierCount }}</el-descriptions-item>
          <el-descriptions-item label="发布版本引用">{{ governanceInfo.publishedVersionCount }}</el-descriptions-item>
          <el-descriptions-item label="追溯命中记录">{{ governanceInfo.traceCount }}</el-descriptions-item>
        </el-descriptions>
        <el-alert :title="governanceInfo.canDelete ? '允许删除' : '当前不允许删除'" :description="governanceInfo.removeBlockingReason" :type="governanceInfo.canDelete ? 'success' : 'warning'" :closable="false" show-icon class="mt12" />
        <el-alert :title="governanceInfo.canDisable ? '允许停用' : '当前不允许停用'" :description="governanceInfo.disableBlockingReason" :type="governanceInfo.canDisable ? 'success' : 'warning'" :closable="false" show-icon class="mt12" />
      </div>
    </el-drawer>
  </div>
</template>

<script setup name="CostRule">
import { ElMessageBox } from 'element-plus'
import ConditionValueEditor from '@/components/cost/ConditionValueEditor.vue'
import RuleTierEditor from '@/components/cost/RuleTierEditor.vue'
import { optionselectFee } from '@/api/cost/fee'
import { addRule, copyRule, delRule, getRule, getRuleGovernance, getRuleStats, listRule, updateRule } from '@/api/cost/rule'
import { optionselectScene } from '@/api/cost/scene'
import { optionselectVariable } from '@/api/cost/variable'
import { getRemoteDictOptionMap } from '@/utils/dictRemote'

const { proxy } = getCurrentInstance()

const loading = ref(true)
const feeLoading = ref(false)
const showSearch = ref(true)
const open = ref(false)
const title = ref('')
const total = ref(0)
const ids = ref([])
const single = ref(true)
const multiple = ref(true)
const feeKeyword = ref('')
const selectedFeeId = ref(undefined)
const initialStatus = ref(undefined)
const expressionOpen = ref(false)
const copyOpen = ref(false)
const copySourceRule = ref(undefined)

const ruleList = ref([])
const sceneOptions = ref([])
const feeOptions = ref([])
const variableOptions = ref([])
const businessDomainOptions = ref([])
const ruleStatusOptions = ref([])
const ruleTypeOptions = ref([])
const conditionLogicOptions = ref([])
const operatorOptions = ref([])
const intervalModeOptions = ref([])
const unitCodeOptions = ref([])
const dynamicDictOptionsMap = ref({})

const governanceOpen = ref(false)
const governanceLoading = ref(false)
const governanceInfo = ref({})
const statistics = reactive({ ruleCount: 0, enabledRuleCount: 0, tierRuleCount: 0, formulaRuleCount: 0 })

const data = reactive({
  queryParams: {
    pageNum: 1,
    pageSize: 10,
    sceneId: undefined,
    businessDomain: undefined,
    feeId: undefined,
    ruleCode: undefined,
    ruleType: undefined,
    status: undefined
  },
  form: {},
  copyForm: {
    ruleCode: undefined,
    ruleName: undefined,
    priority: 100,
    sortNo: 10,
    status: '0',
    conditions: []
  },
  rules: {
    ruleCode: [{ required: true, message: '规则编码不能为空', trigger: 'blur' }],
    ruleType: [{ required: true, message: '规则类型不能为空', trigger: 'change' }],
    status: [{ required: true, message: '规则状态不能为空', trigger: 'change' }]
  },
  copyRules: {
    ruleCode: [{ required: true, message: '新规则编码不能为空', trigger: 'blur' }],
    ruleName: [{ required: true, message: '新规则名称不能为空', trigger: 'blur' }],
    status: [{ required: true, message: '状态不能为空', trigger: 'change' }]
  }
})

const { queryParams, form, copyForm, rules, copyRules } = toRefs(data)

const formulaFunctions = [
  { label: 'if(cond,a,b)', value: 'if(, , )' },
  { label: 'max(a,b)', value: 'max(, )' },
  { label: 'min(a,b)', value: 'min(, )' },
  { label: 'round(x,2)', value: 'round(, 2)' },
  { label: 'between(x,a,b)', value: 'between(, , )' },
  { label: 'coalesce(a,b)', value: 'coalesce(, )' }
]
const namespaceTokens = [
  { label: 'V.变量', value: 'V.' },
  { label: 'I.输入', value: 'I.' },
  { label: 'C.上下文', value: 'C.' },
  { label: 'F.费用结果', value: 'F.' },
  { label: 'T.临时值', value: 'T.' }
]

const filteredFeeOptions = computed(() => {
  const keyword = feeKeyword.value?.trim()
  if (!keyword) {
    return feeOptions.value
  }
  return feeOptions.value.filter(item => `${item.feeCode} ${item.feeName}`.toLowerCase().includes(keyword.toLowerCase()))
})

const currentFee = computed(() => feeOptions.value.find(item => item.feeId === selectedFeeId.value))
const currentFeeTitle = computed(() => currentFee.value ? `${currentFee.value.feeName} (${currentFee.value.feeCode})` : '请选择左侧费用')
const currentFeeDesc = computed(() => currentFee.value
  ? `当前归属场景：${currentFee.value.sceneName}，计价单位：${resolveUnitLabel(currentFee.value.unitCode)}`
  : '规则维护严格遵守费用主线工作台，不直接脱离费用新增规则。')
const variableMetaMap = computed(() => variableOptions.value.reduce((acc, item) => {
  acc[item.variableCode] = item
  return acc
}, {}))
const selectedTierVariableMeta = computed(() => variableMetaMap.value[form.value.quantityVariableCode])
const tierValidationIssues = computed(() => buildTierValidationIssues(form.value.tiers || []))
const metricItems = computed(() => [
  { label: '规则总数', value: statistics.ruleCount, desc: '当前检索条件下规则数量' },
  { label: '启用规则数', value: statistics.enabledRuleCount, desc: '状态为正常的规则数量' },
  { label: '阶梯规则数', value: statistics.tierRuleCount, desc: '用于维护阶梯费率的规则数量' },
  { label: '当前费用', value: currentFee.value?.feeName || '未选择', desc: currentFee.value?.feeCode || '请先从左侧费用列表中选择' }
])
const variableSourceOptions = computed(() => proxy.useDict('cost_variable_source_type').cost_variable_source_type || [])
const variableDataTypeOptions = computed(() => proxy.useDict('cost_variable_data_type').cost_variable_data_type || [])

function resetFormModel() {
  form.value = {
    ruleId: undefined,
    sceneId: queryParams.value.sceneId,
    feeId: selectedFeeId.value,
    ruleCode: undefined,
    ruleName: undefined,
    ruleType: 'FIXED_RATE',
    conditionLogic: 'AND',
    priority: 100,
    quantityVariableCode: undefined,
    pricingMode: 'TYPED',
    pricingConfig: { rateValue: undefined },
    amountFormula: undefined,
    noteTemplate: undefined,
    status: '0',
    sortNo: 10,
    remark: undefined,
    conditions: [],
    tiers: []
  }
  initialStatus.value = undefined
  proxy.resetForm('ruleRef')
}

async function loadBaseOptions() {
  const [dictMap, sceneResponse] = await Promise.all([
    getRemoteDictOptionMap(['cost_business_domain', 'cost_rule_status', 'cost_rule_type', 'cost_rule_condition_logic', 'cost_rule_operator', 'cost_rule_interval_mode', 'cost_unit_code']),
    optionselectScene({ status: '0', pageNum: 1, pageSize: 1000 })
  ])
  businessDomainOptions.value = dictMap.cost_business_domain || []
  ruleStatusOptions.value = dictMap.cost_rule_status || []
  ruleTypeOptions.value = dictMap.cost_rule_type || []
  conditionLogicOptions.value = dictMap.cost_rule_condition_logic || []
  operatorOptions.value = dictMap.cost_rule_operator || []
  intervalModeOptions.value = dictMap.cost_rule_interval_mode || []
  unitCodeOptions.value = dictMap.cost_unit_code || []
  sceneOptions.value = sceneResponse?.data || []
}

async function loadFees() {
  feeLoading.value = true
  try {
    const response = await optionselectFee({
      sceneId: queryParams.value.sceneId,
      businessDomain: queryParams.value.businessDomain,
      status: '0',
      pageNum: 1,
      pageSize: 1000
    })
    feeOptions.value = response?.data || []
    if (!feeOptions.value.find(item => item.feeId === selectedFeeId.value)) {
      selectedFeeId.value = feeOptions.value[0]?.feeId
      queryParams.value.feeId = selectedFeeId.value
    }
  } finally {
    feeLoading.value = false
  }
}

async function loadVariables(sceneId) {
  if (!sceneId) {
    variableOptions.value = []
    return
  }
  const response = await optionselectVariable({ sceneId, status: '0', pageNum: 1, pageSize: 1000 })
  variableOptions.value = response?.data || []
  await ensureDynamicDictOptions()
}

async function ensureDynamicDictOptions() {
  const dictTypes = [...new Set(variableOptions.value.map(item => item.dictType).filter(Boolean))]
  if (!dictTypes.length) {
    dynamicDictOptionsMap.value = {}
    return
  }
  dynamicDictOptionsMap.value = await getRemoteDictOptionMap(dictTypes)
}

function normalizeStats(data = {}) {
  return {
    ruleCount: Number(data.ruleCount || 0),
    enabledRuleCount: Number(data.enabledRuleCount || 0),
    tierRuleCount: Number(data.tierRuleCount || 0),
    formulaRuleCount: Number(data.formulaRuleCount || 0)
  }
}

async function getList() {
  loading.value = true
  try {
    await loadBaseOptions()
    await loadFees()
    if (queryParams.value.feeId) {
      const fee = feeOptions.value.find(item => item.feeId === queryParams.value.feeId)
      await loadVariables(fee?.sceneId || queryParams.value.sceneId)
    }
    const [listResponse, statsResponse] = await Promise.all([
      listRule(queryParams.value),
      getRuleStats(queryParams.value)
    ])
    ruleList.value = listResponse.rows
    total.value = listResponse.total
    Object.assign(statistics, normalizeStats(statsResponse.data))
  } finally {
    loading.value = false
  }
}

function handleSelectionChange(selection) {
  ids.value = selection.map(item => item.ruleId)
  single.value = selection.length !== 1
  multiple.value = !selection.length
}

async function handleSceneChange() {
  selectedFeeId.value = undefined
  queryParams.value.feeId = undefined
  feeKeyword.value = ''
  await loadFees()
  handleQuery()
}

async function handleFeeSelect(item) {
  selectedFeeId.value = item.feeId
  queryParams.value.feeId = item.feeId
  queryParams.value.sceneId = item.sceneId
  await loadVariables(item.sceneId)
  handleQuery()
}

function handleQuery() {
  queryParams.value.pageNum = 1
  getList()
}

function resetQuery() {
  proxy.resetForm('queryRef')
  queryParams.value.pageNum = 1
  queryParams.value.pageSize = 10
  selectedFeeId.value = undefined
  handleQuery()
}

async function handleAdd() {
  if (!selectedFeeId.value) {
    proxy.$modal.msgWarning('请先选择左侧费用后再新增规则')
    return
  }
  await loadVariables(currentFee.value?.sceneId)
  resetFormModel()
  form.value.conditions = [createConditionRow()]
  open.value = true
  title.value = '新增规则'
}

async function handleUpdate(row) {
  const ruleId = row?.ruleId || ids.value[0]
  const response = await getRule(ruleId)
  await loadVariables(currentFee.value?.sceneId || queryParams.value.sceneId)
  resetFormModel()
  form.value = normalizeRuleDetail(response.data)
  initialStatus.value = response.data?.status
  open.value = true
  title.value = '修改规则'
}

async function handleCopy(row) {
  const ruleId = row?.ruleId || ids.value[0]
  const response = await getRule(ruleId)
  await loadVariables(currentFee.value?.sceneId || queryParams.value.sceneId)
  const copied = normalizeRuleDetail(response.data)
  copySourceRule.value = copied
  copyForm.value = {
    ruleCode: `${copied.ruleCode || 'RULE'}_COPY`,
    ruleName: copied.ruleName ? `${copied.ruleName}-复制` : '复制规则',
    priority: copied.priority ?? 100,
    sortNo: copied.sortNo ?? 10,
    status: copied.status || '0',
    conditions: (copied.conditions || []).map(item => ({
      ...item,
      conditionId: undefined,
      displayName: item.displayName || resolveVariableName(item.variableCode)
    }))
  }
  proxy.resetForm('copyRef')
  copyOpen.value = true
}

function cancel() {
  open.value = false
  resetFormModel()
}

function handleRuleTypeChange(value) {
  if (value === 'FIXED_RATE') {
    form.value.pricingConfig = { rateValue: form.value.pricingConfig?.rateValue }
    form.value.amountFormula = undefined
    form.value.tiers = []
  } else if (value === 'FIXED_AMOUNT') {
    form.value.pricingConfig = { amountValue: form.value.pricingConfig?.amountValue }
    form.value.amountFormula = undefined
    form.value.tiers = []
  } else if (value === 'FORMULA') {
    form.value.pricingConfig = {}
    form.value.tiers = []
  } else if (value === 'TIER_RATE') {
    form.value.pricingConfig = {}
    if (!form.value.tiers?.length) {
      form.value.tiers = [{ tierNo: 1, intervalMode: 'LEFT_CLOSED_RIGHT_OPEN', status: '0' }]
    }
  }
}

function handleAddCondition() {
  form.value.conditions.push(createConditionRow())
}

function handleRemoveCondition(index) {
  form.value.conditions.splice(index, 1)
}

async function handleConditionVariableChange(row, value) {
  const meta = variableMetaMap.value[value]
  row.displayName = row.displayName || meta?.variableName || value
  row.compareValue = ''
  if (meta?.dictType && !dynamicDictOptionsMap.value[meta.dictType]) {
    const dictMap = await getRemoteDictOptionMap([meta.dictType])
    dynamicDictOptionsMap.value = { ...dynamicDictOptionsMap.value, ...dictMap }
  }
}

function handleConditionOperatorChange(row, value) {
  const normalizedOperator = (value || '').toUpperCase()
  if (!['IN', 'NOT_IN'].includes(normalizedOperator) && typeof row.compareValue === 'string' && row.compareValue.includes(',')) {
    row.compareValue = row.compareValue.split(',').map(item => item.trim()).filter(Boolean)[0] || ''
  }
}

function createConditionRow() {
  return {
    conditionId: undefined,
    sceneId: queryParams.value.sceneId,
    groupNo: 1,
    sortNo: (form.value.conditions?.length || 0) + 1,
    variableCode: undefined,
    displayName: undefined,
    operatorCode: 'EQ',
    compareValue: '',
    status: '0'
  }
}

function normalizeRuleDetail(data = {}) {
  const normalizedPricingConfig = normalizePricingConfig(data.ruleType, data.pricingConfig)
  return {
    ruleId: data.ruleId,
    sceneId: data.sceneId,
    feeId: data.feeId || selectedFeeId.value,
    ruleCode: data.ruleCode,
    ruleName: data.ruleName,
    ruleType: data.ruleType || 'FIXED_RATE',
    conditionLogic: data.conditionLogic || 'AND',
    priority: Number(data.priority ?? 100),
    quantityVariableCode: data.quantityVariableCode,
    pricingMode: data.pricingMode || 'TYPED',
    pricingConfig: normalizedPricingConfig,
    amountFormula: data.amountFormula,
    noteTemplate: data.noteTemplate,
    status: data.status || '0',
    sortNo: Number(data.sortNo ?? 10),
    remark: data.remark,
    conditions: (data.conditions || []).map((item, index) => ({ ...item, sortNo: item.sortNo || index + 1, status: item.status || '0' })),
    tiers: (data.tiers || []).map((item, index) => ({ ...item, tierNo: item.tierNo || index + 1, status: item.status || '0' }))
  }
}

function normalizePricingConfig(ruleType, pricingConfig) {
  const config = pricingConfig ? { ...pricingConfig } : {}
  if (ruleType === 'FIXED_RATE' && config.rateValue == null && config.unitPrice != null) {
    config.rateValue = config.unitPrice
  }
  if (ruleType === 'FIXED_AMOUNT' && config.amountValue == null && config.amount != null) {
    config.amountValue = config.amount
  }
  return config
}

function normalizeSubmitData() {
  return buildSubmitPayload(form.value)
}

function buildSubmitPayload(source) {
  const payload = JSON.parse(JSON.stringify(source))
  const sceneId = currentFee.value?.sceneId || queryParams.value.sceneId
  payload.sceneId = sceneId
  payload.feeId = selectedFeeId.value
  payload.conditions = (payload.conditions || []).filter(item => item.variableCode).map((item, index) => ({
    ...item,
    sceneId,
    sortNo: index + 1,
    displayName: item.displayName || resolveVariableName(item.variableCode),
    compareValue: normalizeConditionCompareValue(item.compareValue)
  }))
  payload.tiers = (payload.tiers || []).map((item, index) => ({
    ...item,
    sceneId,
    tierNo: index + 1,
    status: item.status || '0'
  }))
  return payload
}

function submitForm() {
  proxy.$refs.ruleRef.validate(async valid => {
    if (!valid) {
      return
    }
    if (form.value.ruleType === 'TIER_RATE' && tierValidationIssues.value.length) {
      proxy.$modal.msgWarning(tierValidationIssues.value[0])
      return
    }
    if (!selectedFeeId.value) {
      proxy.$modal.msgWarning('请先选择费用后再保存规则')
      return
    }
    const allowed = await ensureDisableAllowed()
    if (!allowed) {
      return
    }
    const payload = normalizeSubmitData()
    const request = payload.ruleId ? updateRule(payload) : addRule(payload)
    await request
    proxy.$modal.msgSuccess(payload.ruleId ? '修改成功' : '新增成功')
    open.value = false
    getList()
  })
}

function submitCopyForm() {
  proxy.$refs.copyRef.validate(async valid => {
    if (!valid || !copySourceRule.value) {
      return
    }
    if ((copyForm.value.conditions || []).some(item => requiresCompareValue(item.operatorCode) && isBlankValue(item.compareValue))) {
      proxy.$modal.msgWarning('复制规则时请补齐所有条件值')
      return
    }
    await copyRule({
      sourceRuleId: copySourceRule.value.ruleId,
      ruleCode: copyForm.value.ruleCode,
      ruleName: copyForm.value.ruleName,
      priority: copyForm.value.priority,
      sortNo: copyForm.value.sortNo,
      status: copyForm.value.status,
      conditions: (copyForm.value.conditions || []).map((item, index) => ({
        ...item,
        conditionId: undefined,
        sceneId: currentFee.value?.sceneId || queryParams.value.sceneId,
        sortNo: index + 1
      }))
    })
    proxy.$modal.msgSuccess('复制成功')
    copyOpen.value = false
    getList()
  })
}

function resolveTargetRows(row) {
  if (row?.ruleId) {
    return [row]
  }
  return ruleList.value.filter(item => ids.value.includes(item.ruleId))
}

async function handleDelete(row) {
  const targetRows = resolveTargetRows(row)
  if (!targetRows.length) {
    return
  }
  const checks = await Promise.all(targetRows.map(item => fetchGovernance(item.ruleId)))
  const blocked = checks.filter(item => !item.canDelete)
  if (blocked.length) {
    await ElMessageBox.alert(blocked.map(item => `${item.ruleCode}：${item.removeBlockingReason}`).join('<br/>'), '删除前治理检查', { type: 'warning', dangerouslyUseHTMLString: true })
    governanceInfo.value = blocked[0]
    governanceOpen.value = true
    return
  }
  const ruleIds = row?.ruleId || ids.value
  const ruleCodes = targetRows.map(item => item.ruleCode).join('、')
  proxy.$modal.confirm(`是否确认删除规则"${ruleCodes}"的数据项？`).then(function() {
    return delRule(ruleIds)
  }).then(() => {
    getList()
    proxy.$modal.msgSuccess('删除成功')
  }).catch(() => {})
}

function handleExport() {
  proxy.download('cost/rule/export', { ...queryParams.value }, `rule_${new Date().getTime()}.xlsx`)
}

async function fetchGovernance(ruleId) {
  const response = await getRuleGovernance(ruleId)
  return response.data || {}
}

async function handleGovernance(row) {
  governanceLoading.value = true
  governanceOpen.value = true
  try {
    governanceInfo.value = await fetchGovernance(row.ruleId)
  } finally {
    governanceLoading.value = false
  }
}

async function ensureDisableAllowed() {
  if (!form.value.ruleId || form.value.status !== '1' || initialStatus.value === '1') {
    return true
  }
  const check = await fetchGovernance(form.value.ruleId)
  if (!check.canDisable) {
    governanceInfo.value = check
    governanceOpen.value = true
    await ElMessageBox.alert(check.disableBlockingReason, '停用前治理检查', { type: 'warning' })
    return false
  }
  return true
}

function resolveVariableName(variableCode) {
  return variableMetaMap.value[variableCode]?.variableName || variableCode
}

function resolveUnitLabel(unitCode) {
  const match = unitCodeOptions.value.find(item => item.value === unitCode)
  return match ? match.label : (unitCode || '-')
}

function appendFormula(token) {
  form.value.amountFormula = `${form.value.amountFormula || ''}${form.value.amountFormula ? ' ' : ''}${token}`
}

function buildTierValidationIssues(tiers) {
  if (!tiers?.length) {
    return []
  }
  const issues = []
  const normalized = tiers.map((item, index) => ({ ...item, __index: index + 1 }))
    .sort((a, b) => (a.tierNo || a.__index) - (b.tierNo || b.__index))
  let previous = null
  normalized.forEach(item => {
    if (item.rateValue == null || item.rateValue === '') {
      issues.push(`第${item.__index}档未填写费率/单价`)
    }
    if (item.startValue != null && item.endValue != null && Number(item.startValue) >= Number(item.endValue)) {
      issues.push(`第${item.__index}档起始值必须小于截止值`)
    }
    if (previous) {
      if (previous.endValue == null) {
        issues.push(`第${previous.__index}档已是不封顶区间，后续不应继续维护档位`)
      } else if (item.startValue == null || item.startValue === '') {
        issues.push(`第${item.__index}档缺少起始值`)
      } else if (Number(previous.endValue) < Number(item.startValue)) {
        issues.push(`第${previous.__index}档与第${item.__index}档之间存在断档`)
      } else if (Number(previous.endValue) > Number(item.startValue)) {
        issues.push(`第${previous.__index}档与第${item.__index}档区间重叠`)
      }
    }
    previous = item
  })
  return [...new Set(issues)]
}

function requiresCompareValue(operatorCode) {
  return !['IS_NULL', 'IS_NOT_NULL'].includes((operatorCode || '').toUpperCase())
}

function isBlankValue(value) {
  return value == null || value === ''
}

function normalizeConditionCompareValue(value) {
  if (Array.isArray(value)) {
    return value.map(item => `${item ?? ''}`.trim()).filter(Boolean).join(',')
  }
  return typeof value === 'string' ? value.split(',').map(item => item.trim()).filter(Boolean).join(',') : value
}

function resolveTagLabel(options, value) {
  const source = options?.value ?? options ?? []
  const normalized = Array.isArray(source) ? source : []
  const match = normalized.find(item => item.value === value)
  return match ? match.label : value
}

getList()
</script>

<style scoped lang="scss">
.rule-center {
  display: grid;
  gap: 16px;
}

.rule-center__hero,
.rule-center__metric-card,
.rule-center__fee-panel,
.rule-center__rule-panel {
  border: 1px solid var(--el-border-color);
  border-radius: 16px;
  background: var(--el-bg-color-overlay);
}

.rule-center__hero {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: flex-start;
  padding: 20px 24px;
  background: color-mix(in srgb, var(--el-color-warning-light-9) 20%, var(--el-bg-color-overlay));
}

.rule-center__eyebrow { font-size: 12px; color: var(--el-color-warning-dark-2); font-weight: 700; letter-spacing: 0.08em; text-transform: uppercase; }
.rule-center__title { margin: 8px 0 0; font-size: 26px; }
.rule-center__subtitle { margin: 10px 0 0; color: var(--el-text-color-regular); line-height: 1.8; }

.rule-center__metrics {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 14px;
}

.rule-center__metric-card {
  display: grid;
  gap: 6px;
  padding: 14px 16px;
}

.rule-center__metric-label,
.rule-center__metric-desc { font-size: 12px; color: var(--el-text-color-secondary); }
.rule-center__metric-value { font-size: 24px; color: var(--el-color-warning-dark-2); }

.rule-center__workspace {
  display: grid;
  grid-template-columns: 320px minmax(0, 1fr);
  gap: 16px;
}

.rule-center__fee-panel,
.rule-center__rule-panel {
  padding: 16px;
}

.rule-center__panel-header {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: center;
  margin-bottom: 16px;
}

.rule-center__panel-header h3 {
  margin: 0;
  font-size: 18px;
}

.rule-center__panel-header p {
  margin: 6px 0 0;
  color: var(--el-text-color-secondary);
  font-size: 13px;
}

.rule-center__fee-list {
  display: grid;
  gap: 10px;
  max-height: 640px;
  overflow: auto;
}

.rule-center__fee-item {
  display: grid;
  gap: 4px;
  padding: 12px;
  border: 1px solid var(--el-border-color-light);
  border-radius: 12px;
  background: var(--el-fill-color-blank);
  text-align: left;
  cursor: pointer;
}

.rule-center__fee-item strong {
  font-size: 14px;
}

.rule-center__fee-item span,
.rule-center__fee-item small {
  color: var(--el-text-color-secondary);
}

.rule-center__fee-item.is-active {
  border-color: var(--el-color-primary);
  background: color-mix(in srgb, var(--el-color-primary-light-9) 32%, var(--el-bg-color-overlay));
}

.rule-center__section-title {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin: 16px 0 12px;
  font-weight: 700;
}

.rule-center__formula-helper {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
  padding: 12px 14px;
  border-radius: 10px;
  background: color-mix(in srgb, var(--el-color-primary-light-9) 36%, var(--el-bg-color-overlay));
  color: var(--el-text-color-regular);
  font-size: 13px;
}

.rule-center__tier-basis,
.rule-center__copy-basis {
  display: grid;
  gap: 8px;
  padding: 12px 14px;
  border-radius: 10px;
  background: color-mix(in srgb, var(--el-color-success-light-9) 30%, var(--el-bg-color-overlay));
  margin-bottom: 12px;
}

.rule-center__tier-basis > div,
.rule-center__copy-basis {
  display: flex;
  gap: 14px;
  flex-wrap: wrap;
  color: var(--el-text-color-regular);
  font-size: 13px;
}

.rule-center__tier-basis strong,
.rule-center__copy-basis strong {
  color: var(--el-color-success-dark-2);
}

.rule-center__issue-item {
  line-height: 1.8;
}

.rule-center__readonly-cell {
  display: grid;
  gap: 4px;
}

.rule-center__readonly-cell span {
  color: var(--el-text-color-secondary);
  font-size: 12px;
}

.rule-center__expression-grid {
  display: grid;
  gap: 16px;
}

.rule-center__token-list {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.rule-center__token {
  cursor: pointer;
}

.mb8 { margin-bottom: 8px; }
.mb16 { margin-bottom: 16px; }
.mt12 { margin-top: 12px; }

@media (max-width: 1200px) {
  .rule-center__metrics {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .rule-center__workspace {
    grid-template-columns: 1fr;
  }
}
</style>

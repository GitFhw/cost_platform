<template>
  <div class="app-container fee-center">
    <section class="fee-center__hero">
      <div>
        <div class="fee-center__eyebrow">费目治理</div>
        <h2 class="fee-center__title">费用中心</h2>
        <p class="fee-center__subtitle">
          围绕场景统一维护费用对象、计费口径和业务说明，为规则配置、版本发布和结果查询提供稳定基础。
        </p>
      </div>
      <el-tag type="info">删除前自动校验关联规则、版本和结果记录，保障费用口径稳定</el-tag>
    </section>

    <section class="fee-center__metrics">
      <div v-for="item in metricItems" :key="item.label" class="fee-center__metric-card">
        <span class="fee-center__metric-label">{{ item.label }}</span>
        <strong class="fee-center__metric-value">{{ item.value }}</strong>
        <span class="fee-center__metric-desc">{{ item.desc }}</span>
      </div>
    </section>

    <el-form ref="queryRef" :model="queryParams" :inline="true" label-width="84px" v-show="showSearch">
      <el-form-item label="所属场景" prop="sceneId">
        <el-select v-model="queryParams.sceneId" placeholder="请选择场景" clearable filterable style="width: 230px" @change="handleSceneChange">
          <el-option v-for="item in sceneOptions" :key="item.sceneId" :label="`${item.sceneCode} / ${item.sceneName}`" :value="item.sceneId" />
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
      <el-table-column label="操作" align="center" width="250" fixed="right" class-name="small-padding fixed-width">
        <template #default="scope">
          <el-button link type="primary" icon="View" @click="handleGovernance(scope.row)">治理</el-button>
          <el-button link type="primary" icon="Edit" @click="handleUpdate(scope.row)" v-hasPermi="['cost:fee:edit']">修改</el-button>
          <el-button link type="primary" icon="Delete" @click="handleDelete(scope.row)" v-hasPermi="['cost:fee:remove']">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination v-show="total > 0" :total="total" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" @pagination="getList" />

    <el-dialog :title="title" v-model="open" width="700px" append-to-body>
      <el-form ref="feeRef" :model="form" :rules="rules" label-width="110px">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="所属场景" prop="sceneId">
              <el-select v-model="form.sceneId" placeholder="请选择场景" filterable style="width: 100%">
                <el-option v-for="item in sceneOptions" :key="item.sceneId" :label="`${item.sceneCode} / ${item.sceneName}`" :value="item.sceneId" />
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
              <el-input v-model="form.objectDimension" placeholder="如：船舶、人员、库区" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="排序号" prop="sortNo">
              <el-input-number v-model="form.sortNo" :min="1" :max="9999" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="影响因素摘要" prop="factorSummary">
              <el-input v-model="form.factorSummary" placeholder="补充费用依赖的关键变量因素" />
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

    <el-drawer v-model="governanceOpen" title="费用治理检查" size="520px" append-to-body>
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
        <el-alert :title="governanceInfo.canDelete ? '允许删除' : '当前不允许删除'" :description="governanceInfo.removeBlockingReason" :type="governanceInfo.canDelete ? 'success' : 'warning'" :closable="false" show-icon />
        <el-alert :title="governanceInfo.canDisable ? '允许停用' : '当前不允许停用'" :description="governanceInfo.disableBlockingReason" :type="governanceInfo.canDisable ? 'success' : 'warning'" :closable="false" show-icon />
        <div class="fee-governance__advice">
          <p>删除建议：{{ governanceInfo.removeAdvice }}</p>
          <p>停用建议：{{ governanceInfo.disableAdvice }}</p>
        </div>
      </div>
    </el-drawer>
  </div>
</template>

<script setup name="CostFee">
import { ElMessageBox } from 'element-plus'
import { addFee, delFee, getFee, getFeeGovernance, getFeeStats, listFee, updateFee } from '@/api/cost/fee'
import { optionselectScene } from '@/api/cost/scene'
import { resolveWorkingCostSceneId } from '@/utils/costSceneContext'
import { getCostUnitSemantic } from '@/utils/costUnitSemantics'
import { getRemoteDictOptionMap } from '@/utils/dictRemote'

const { proxy } = getCurrentInstance()

const feeList = ref([])
const sceneOptions = ref([])
const businessDomainOptions = ref([])
const feeStatusOptions = ref([])
const unitCodeOptions = ref([])
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

const unitOptionsForForm = computed(() => {
  const options = [...unitCodeOptions.value]
  const currentValue = form.value?.unitCode
  if (currentValue && !options.some(item => item.value === currentValue)) {
    options.push({ label: currentValue, value: currentValue })
  }
  return options
})
const currentUnitSemantic = computed(() => resolveUnitSemantic(form.value?.unitCode))

const currentSceneLabel = computed(() => {
  if (!queryParams.value.sceneId) {
    return '全部场景'
  }
  const scene = sceneOptions.value.find(item => item.sceneId === queryParams.value.sceneId)
  return scene ? scene.sceneName : `#${queryParams.value.sceneId}`
})

async function loadBaseOptions() {
  const [dictMap, sceneResponse] = await Promise.all([
    getRemoteDictOptionMap(['cost_business_domain', 'cost_fee_status', 'cost_unit_code']),
    optionselectScene({ status: '0', pageNum: 1, pageSize: 1000 })
  ])
  businessDomainOptions.value = dictMap.cost_business_domain || []
  feeStatusOptions.value = dictMap.cost_fee_status || []
  unitCodeOptions.value = dictMap.cost_unit_code || []
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

function submitForm() {
  proxy.$refs.feeRef.validate(async valid => {
    if (!valid) {
      return
    }
    const allowed = await ensureDisableAllowed()
    if (!allowed) {
      return
    }
    const request = form.value.feeId ? updateFee(form.value) : addFee(form.value)
    await request
    proxy.$modal.msgSuccess(form.value.feeId ? '修改成功' : '新增成功')
    open.value = false
    getList()
  })
}

async function handleDelete(row) {
  const targetRows = resolveTargetRows(row)
  if (!targetRows.length) {
    return
  }
  const checks = await Promise.all(targetRows.map(item => fetchFeeGovernance(item.feeId)))
  const blockedChecks = checks.filter(item => !item.canDelete)
  if (blockedChecks.length) {
    await ElMessageBox.alert(
      blockedChecks.map(item => `${item.feeName}：${item.removeBlockingReason}`).join('<br/>'),
      '删除前治理检查',
      { type: 'warning', dangerouslyUseHTMLString: true }
    )
    governanceInfo.value = blockedChecks[0]
    governanceOpen.value = true
    return
  }

  const feeIds = row?.feeId || ids.value
  const feeNames = targetRows.map(item => item.feeName).join('、')
  proxy.$modal.confirm(`是否确认删除费用"${feeNames}"的数据项？`).then(function() {
    return delFee(feeIds)
  }).then(() => {
    getList()
    proxy.$modal.msgSuccess('删除成功')
  }).catch(() => {})
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
    feeCode: data.feeCode || '',
    feeName: data.feeName || '',
    sceneCode: data.sceneCode || '',
    sceneName: data.sceneName || '',
    status: data.status || '0',
    ruleCount: Number(data.ruleCount || 0),
    variableRelCount: Number(data.variableRelCount || 0),
    publishedVersionCount: Number(data.publishedVersionCount || 0),
    resultLedgerCount: Number(data.resultLedgerCount || 0),
    canDelete: Boolean(data.canDelete),
    canDisable: Boolean(data.canDisable),
    removeBlockingReason: data.removeBlockingReason || '当前费用可以删除',
    disableBlockingReason: data.disableBlockingReason || '当前费用可以停用',
    removeAdvice: data.removeAdvice || '',
    disableAdvice: data.disableAdvice || ''
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

function resolveUnitLabel(value) {
  return resolveDictLabel(unitCodeOptions, value)
}

function resolveUnitSemantic(value) {
  return getCostUnitSemantic(value, resolveUnitLabel(value))
}

async function ensureDisableAllowed() {
  if (!form.value.feeId || form.value.status !== '1' || initialStatus.value === '1') {
    return true
  }
  const check = await fetchFeeGovernance(form.value.feeId)
  if (!check.canDisable) {
    governanceInfo.value = check
    governanceOpen.value = true
    await ElMessageBox.alert(check.disableBlockingReason, '停用前治理检查', { type: 'warning' })
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
.fee-governance__advice { padding: 12px; border-radius: 10px; background: color-mix(in srgb, var(--el-color-warning-light-9) 40%, var(--el-bg-color-overlay)); }
.fee-governance__advice p { margin: 4px 0; line-height: 1.7; }

@media (max-width: 1200px) {
  .fee-center__metrics { grid-template-columns: repeat(2, minmax(0, 1fr)); }
}
</style>

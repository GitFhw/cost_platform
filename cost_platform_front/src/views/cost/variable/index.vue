<template>
  <div class="app-container variable-center">
    <el-alert title="变量中心（线程二）：支持变量分组、第三方接入变量、接口测试和数据预览。" type="info" :closable="false" show-icon class="mb16" />

    <el-form ref="queryRef" :model="queryParams" :inline="true" label-width="84px" v-show="showSearch">
      <el-form-item label="所属场景" prop="sceneId">
        <el-select v-model="queryParams.sceneId" clearable filterable placeholder="请选择场景" style="width: 220px" @change="handleSceneChange">
          <el-option v-for="item in sceneOptions" :key="item.sceneId" :label="`${item.sceneCode} / ${item.sceneName}`" :value="item.sceneId" />
        </el-select>
      </el-form-item>
      <el-form-item label="变量分组" prop="groupId">
        <el-select v-model="queryParams.groupId" clearable filterable placeholder="请选择分组" style="width: 200px">
          <el-option v-for="item in groupOptions" :key="item.groupId" :label="item.groupName" :value="item.groupId" />
        </el-select>
      </el-form-item>
      <el-form-item label="变量编码" prop="variableCode"><el-input v-model="queryParams.variableCode" clearable style="width: 180px" @keyup.enter="handleQuery" /></el-form-item>
      <el-form-item label="变量名称" prop="variableName"><el-input v-model="queryParams.variableName" clearable style="width: 180px" @keyup.enter="handleQuery" /></el-form-item>
      <el-form-item label="来源" prop="sourceType">
        <el-select v-model="queryParams.sourceType" clearable style="width: 160px">
          <el-option v-for="item in sourceTypeOptions" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5"><el-button type="primary" plain icon="Plus" @click="handleAdd" v-hasPermi="['cost:variable:add']">新增变量</el-button></el-col>
      <el-col :span="1.5"><el-button type="success" plain icon="Edit" :disabled="single" @click="handleUpdate" v-hasPermi="['cost:variable:edit']">修改变量</el-button></el-col>
      <el-col :span="1.5"><el-button type="danger" plain icon="Delete" :disabled="multiple" @click="handleDelete" v-hasPermi="['cost:variable:remove']">删除变量</el-button></el-col>
      <el-col :span="1.5"><el-button type="info" plain icon="Connection" :disabled="single" @click="handleTestRemote">测试接口</el-button></el-col>
      <el-col :span="1.5"><el-button type="info" plain icon="View" :disabled="single" @click="handlePreviewRemote">预览数据</el-button></el-col>
      <el-col :span="1.5"><el-button type="info" plain icon="RefreshRight" @click="handleRefreshRemote">刷新缓存</el-button></el-col>
      <el-col :span="1.5"><el-button type="warning" plain icon="Download" @click="handleExport" v-hasPermi="['cost:variable:export']">导出</el-button></el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList" />
    </el-row>

    <el-table v-loading="loading" :data="variableList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column type="index" label="序号" width="70" align="center" />
      <el-table-column label="场景" min-width="220" align="center"><template #default="scope">{{ scope.row.sceneCode }} / {{ scope.row.sceneName }}</template></el-table-column>
      <el-table-column label="分组" prop="groupName" width="140" align="center" />
      <el-table-column label="变量编码" prop="variableCode" width="150" align="center" />
      <el-table-column label="变量名称" prop="variableName" min-width="150" align="center" :show-overflow-tooltip="true" />
      <el-table-column label="类型" prop="variableType" width="120" align="center"><template #default="scope"><dict-tag :options="variableTypeOptions" :value="scope.row.variableType" /></template></el-table-column>
      <el-table-column label="来源" prop="sourceType" width="120" align="center"><template #default="scope"><dict-tag :options="sourceTypeOptions" :value="scope.row.sourceType" /></template></el-table-column>
      <el-table-column label="第三方接口" prop="remoteApi" min-width="180" align="center" :show-overflow-tooltip="true" />
      <el-table-column label="状态" prop="status" width="100" align="center"><template #default="scope"><dict-tag :options="variableStatusOptions" :value="scope.row.status" /></template></el-table-column>
      <el-table-column label="操作" width="220" fixed="right" align="center">
        <template #default="scope">
          <el-button link type="primary" icon="View" @click="handleGovernance(scope.row)">治理</el-button>
          <el-button link type="primary" icon="Edit" @click="handleUpdate(scope.row)">修改</el-button>
          <el-button link type="primary" icon="Delete" @click="handleDelete(scope.row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination v-show="total > 0" :total="total" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" @pagination="getList" />

    <el-drawer v-model="open" :title="title" size="680px" append-to-body>
      <el-form ref="variableRef" :model="form" :rules="rules" label-width="108px">
        <el-form-item label="所属场景" prop="sceneId">
          <el-select v-model="form.sceneId" filterable style="width: 100%" @change="loadFormGroups">
            <el-option v-for="item in sceneOptions" :key="item.sceneId" :label="`${item.sceneCode} / ${item.sceneName}`" :value="item.sceneId" />
          </el-select>
        </el-form-item>
        <el-form-item label="变量分组" prop="groupId">
          <el-select v-model="form.groupId" clearable filterable style="width: 100%">
            <el-option v-for="item in formGroupOptions" :key="item.groupId" :label="item.groupName" :value="item.groupId" />
          </el-select>
        </el-form-item>
        <el-row :gutter="14">
          <el-col :span="12"><el-form-item label="变量编码" prop="variableCode"><el-input v-model="form.variableCode" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="变量名称" prop="variableName"><el-input v-model="form.variableName" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="变量类型" prop="variableType"><el-select v-model="form.variableType" style="width: 100%"><el-option v-for="item in variableTypeOptions" :key="item.value" :label="item.label" :value="item.value" /></el-select></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="来源类型" prop="sourceType"><el-select v-model="form.sourceType" style="width: 100%"><el-option v-for="item in sourceTypeOptions" :key="item.value" :label="item.label" :value="item.value" /></el-select></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="数据类型" prop="dataType"><el-select v-model="form.dataType" style="width: 100%"><el-option v-for="item in dataTypeOptions" :key="item.value" :label="item.label" :value="item.value" /></el-select></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="状态" prop="status"><el-radio-group v-model="form.status"><el-radio v-for="item in variableStatusOptions" :key="item.value" :value="item.value">{{ item.label }}</el-radio></el-radio-group></el-form-item></el-col>
        </el-row>
        <el-form-item v-if="form.sourceType === 'DICT'" label="字典类型" prop="dictType"><el-input v-model="form.dictType" placeholder="如 cost_cargo_type" /></el-form-item>
        <template v-if="form.sourceType === 'REMOTE'">
          <el-form-item label="第三方接口" prop="remoteApi"><el-input v-model="form.remoteApi" placeholder="http/https 接口地址" /></el-form-item>
          <el-form-item label="字段映射路径" prop="dataPath"><el-input v-model="form.dataPath" placeholder="如 data.items[].value" /></el-form-item>
        </template>
        <el-form-item v-if="form.sourceType === 'FORMULA'" label="公式表达式" prop="formulaExpr"><el-input v-model="form.formulaExpr" type="textarea" :rows="3" /></el-form-item>
        <el-form-item label="备注" prop="remark"><el-input v-model="form.remark" type="textarea" :rows="3" /></el-form-item>
      </el-form>
      <template #footer><div class="dialog-footer"><el-button type="primary" @click="submitForm">确 定</el-button><el-button @click="cancel">取 消</el-button></div></template>
    </el-drawer>

    <el-dialog title="接口测试" v-model="testOpen" width="480px" append-to-body>
      <el-descriptions :column="1" border v-if="testResult">
        <el-descriptions-item label="结果"><el-tag :type="testResult.success ? 'success' : 'danger'">{{ testResult.success ? '通过' : '失败' }}</el-tag></el-descriptions-item>
        <el-descriptions-item label="说明">{{ testResult.message }}</el-descriptions-item>
        <el-descriptions-item label="接口地址">{{ testResult.remoteApi || '-' }}</el-descriptions-item>
      </el-descriptions>
    </el-dialog>

    <el-dialog title="数据预览" v-model="previewOpen" width="880px" append-to-body>
      <el-row :gutter="12" v-if="previewResult">
        <el-col :span="12"><el-table :data="previewResult.rawRows" height="260" size="small"><el-table-column prop="sourceCode" label="源编码" /><el-table-column prop="sourceName" label="源名称" /><el-table-column prop="value" label="源值" /></el-table></el-col>
        <el-col :span="12"><el-table :data="previewResult.mappedRows" height="260" size="small"><el-table-column prop="variableCode" label="变量编码" /><el-table-column prop="mappedValue" label="映射值" /><el-table-column prop="dataPath" label="映射路径" /></el-table></el-col>
      </el-row>
    </el-dialog>

    <el-drawer v-model="governanceOpen" title="变量治理检查" size="500px" append-to-body>
      <div v-loading="governanceLoading" v-if="governanceInfo.variableId">
        <el-descriptions :column="1" border>
          <el-descriptions-item label="变量">{{ governanceInfo.variableCode }} / {{ governanceInfo.variableName }}</el-descriptions-item>
          <el-descriptions-item label="费用关系引用">{{ governanceInfo.feeRelCount }}</el-descriptions-item>
          <el-descriptions-item label="规则条件引用">{{ governanceInfo.ruleConditionCount }}</el-descriptions-item>
          <el-descriptions-item label="规则计量引用">{{ governanceInfo.ruleQuantityCount }}</el-descriptions-item>
          <el-descriptions-item label="发布版本引用">{{ governanceInfo.publishedVersionCount }}</el-descriptions-item>
        </el-descriptions>
        <el-alert :title="governanceInfo.canDelete ? '允许删除' : '当前不允许删除'" :description="governanceInfo.removeBlockingReason" :type="governanceInfo.canDelete ? 'success' : 'warning'" :closable="false" show-icon class="mt12" />
      </div>
    </el-drawer>
  </div>
</template>

<script setup name="CostVariable">
import { ElMessageBox } from 'element-plus'
import { optionselectScene } from '@/api/cost/scene'
import { addVariable, delVariable, getVariable, getVariableGovernance, listVariable, previewVariableRemote, refreshVariableRemote, testVariableRemote, updateVariable } from '@/api/cost/variable'
import { optionselectVariableGroup } from '@/api/cost/variableGroup'
import { getRemoteDictOptionMap } from '@/utils/dictRemote'

const { proxy } = getCurrentInstance()

const loading = ref(true)
const showSearch = ref(true)
const open = ref(false)
const title = ref('')
const total = ref(0)
const ids = ref([])
const single = ref(true)
const multiple = ref(true)
const initialStatus = ref(undefined)

const variableList = ref([])
const sceneOptions = ref([])
const groupOptions = ref([])
const formGroupOptions = ref([])
const variableTypeOptions = ref([])
const sourceTypeOptions = ref([])
const dataTypeOptions = ref([])
const variableStatusOptions = ref([])

const governanceOpen = ref(false)
const governanceLoading = ref(false)
const governanceInfo = ref({})
const testOpen = ref(false)
const testResult = ref(null)
const previewOpen = ref(false)
const previewResult = ref(null)

const data = reactive({
  queryParams: { pageNum: 1, pageSize: 10, sceneId: undefined, groupId: undefined, variableCode: undefined, variableName: undefined, sourceType: undefined },
  form: {},
  rules: {
    sceneId: [{ required: true, message: '所属场景不能为空', trigger: 'change' }],
    variableCode: [{ required: true, message: '变量编码不能为空', trigger: 'blur' }],
    variableName: [{ required: true, message: '变量名称不能为空', trigger: 'blur' }],
    variableType: [{ required: true, message: '变量类型不能为空', trigger: 'change' }],
    sourceType: [{ required: true, message: '来源类型不能为空', trigger: 'change' }],
    status: [{ required: true, message: '状态不能为空', trigger: 'change' }]
  }
})
const { queryParams, form, rules } = toRefs(data)

async function loadBaseOptions() {
  const [dictMap, sceneResponse] = await Promise.all([
    getRemoteDictOptionMap(['cost_variable_type', 'cost_variable_source_type', 'cost_variable_data_type', 'cost_variable_status']),
    optionselectScene({ status: '0', pageNum: 1, pageSize: 1000 })
  ])
  variableTypeOptions.value = dictMap.cost_variable_type || []
  sourceTypeOptions.value = dictMap.cost_variable_source_type || []
  dataTypeOptions.value = dictMap.cost_variable_data_type || []
  variableStatusOptions.value = dictMap.cost_variable_status || []
  sceneOptions.value = sceneResponse?.data || []
}

async function loadGroups(sceneId) {
  const response = await optionselectVariableGroup({ sceneId, status: '0', pageNum: 1, pageSize: 1000 })
  return response?.data || []
}

async function getList() {
  loading.value = true
  try {
    const [, rows] = await Promise.all([loadBaseOptions(), listVariable(queryParams.value)])
    variableList.value = rows.rows
    total.value = rows.total
    groupOptions.value = await loadGroups(queryParams.value.sceneId)
  } finally {
    loading.value = false
  }
}

async function handleSceneChange() {
  queryParams.value.groupId = undefined
  groupOptions.value = await loadGroups(queryParams.value.sceneId)
}

async function loadFormGroups() {
  form.value.groupId = undefined
  formGroupOptions.value = await loadGroups(form.value.sceneId)
}

function resetFormModel() {
  form.value = { variableId: undefined, sceneId: undefined, groupId: undefined, variableCode: undefined, variableName: undefined, variableType: 'TEXT', sourceType: 'INPUT', dictType: undefined, remoteApi: undefined, dataPath: undefined, formulaExpr: undefined, dataType: 'STRING', status: '0', precisionScale: 2, sortNo: 10, remark: undefined }
  initialStatus.value = undefined
  proxy.resetForm('variableRef')
}

function handleSelectionChange(selection) {
  ids.value = selection.map(item => item.variableId)
  single.value = selection.length !== 1
  multiple.value = !selection.length
}

function handleQuery() { queryParams.value.pageNum = 1; getList() }
function resetQuery() { proxy.resetForm('queryRef'); queryParams.value.pageNum = 1; queryParams.value.pageSize = 10; queryParams.value.groupId = undefined; handleQuery() }

async function handleAdd() {
  await loadBaseOptions()
  resetFormModel()
  open.value = true
  title.value = '新增变量'
}

async function handleUpdate(row) {
  await loadBaseOptions()
  resetFormModel()
  const response = await getVariable(row?.variableId || ids.value[0])
  form.value = { ...response.data }
  initialStatus.value = response.data?.status
  formGroupOptions.value = await loadGroups(form.value.sceneId)
  open.value = true
  title.value = '修改变量'
}

function cancel() { open.value = false; resetFormModel() }

function submitForm() {
  proxy.$refs.variableRef.validate(async valid => {
    if (!valid) return
    if (!await ensureDisableAllowed()) return
    const req = form.value.variableId ? updateVariable(form.value) : addVariable(form.value)
    await req
    proxy.$modal.msgSuccess(form.value.variableId ? '修改成功' : '新增成功')
    open.value = false
    getList()
  })
}

function resolveTargetRows(row) {
  if (row?.variableId) return [row]
  return variableList.value.filter(item => ids.value.includes(item.variableId))
}

async function handleDelete(row) {
  const targetRows = resolveTargetRows(row)
  if (!targetRows.length) return
  const checks = await Promise.all(targetRows.map(item => fetchGovernance(item.variableId)))
  const blocked = checks.filter(item => !item.canDelete)
  if (blocked.length) {
    await ElMessageBox.alert(blocked.map(item => `${item.variableName}：${item.removeBlockingReason}`).join('<br/>'), '删除前治理检查', { type: 'warning', dangerouslyUseHTMLString: true })
    governanceInfo.value = blocked[0]
    governanceOpen.value = true
    return
  }
  const variableIds = row?.variableId || ids.value
  const variableNames = targetRows.map(item => item.variableName).join('、')
  proxy.$modal.confirm(`是否确认删除变量"${variableNames}"的数据项？`).then(function() { return delVariable(variableIds) }).then(() => { getList(); proxy.$modal.msgSuccess('删除成功') }).catch(() => {})
}

function handleExport() {
  proxy.download('cost/variable/export', { ...queryParams.value }, `variable_${new Date().getTime()}.xlsx`)
}

async function fetchGovernance(variableId) {
  const response = await getVariableGovernance(variableId)
  return response.data || {}
}

async function handleGovernance(row) {
  governanceLoading.value = true
  governanceOpen.value = true
  try {
    governanceInfo.value = await fetchGovernance(row.variableId)
  } finally {
    governanceLoading.value = false
  }
}

async function ensureDisableAllowed() {
  if (!form.value.variableId || form.value.status !== '1' || initialStatus.value === '1') return true
  const check = await fetchGovernance(form.value.variableId)
  if (!check.canDisable) {
    governanceInfo.value = check
    governanceOpen.value = true
    await ElMessageBox.alert(check.disableBlockingReason, '停用前治理检查', { type: 'warning' })
    return false
  }
  return true
}

function currentRow() { return variableList.value.find(item => item.variableId === ids.value[0]) }

async function handleTestRemote() {
  const row = currentRow(); if (!row) return
  const response = await testVariableRemote({ remoteApi: row.remoteApi, authType: 'NONE' })
  testResult.value = response.data
  testOpen.value = true
}

async function handlePreviewRemote() {
  const row = currentRow(); if (!row) return
  const response = await previewVariableRemote({ variableId: row.variableId, dataPath: row.dataPath, variableCode: row.variableCode })
  previewResult.value = response.data
  previewOpen.value = true
}

async function handleRefreshRemote() {
  const response = await refreshVariableRemote({ sceneId: queryParams.value.sceneId })
  proxy.$modal.msgSuccess(response.data?.message || '刷新成功')
}

getList()
</script>

<style scoped lang="scss">
.variable-center { display: grid; gap: 16px; }
.mb16 { margin-bottom: 0; }
.mt12 { margin-top: 12px; }
</style>

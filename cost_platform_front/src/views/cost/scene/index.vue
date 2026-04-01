<template>
  <div class="app-container scene-center">
    <section class="scene-center__hero">
      <div class="scene-center__hero-main">
        <div class="scene-center__eyebrow">线程一 · 基础治理</div>
        <h2 class="scene-center__title">场景中心</h2>
        <p class="scene-center__subtitle">
          先收稳“业务域字典 -> 场景主数据”第一层治理边界，让后续费用、变量、规则与发布都能围绕场景有序展开。
        </p>
        <div class="scene-center__hero-actions">
          <el-button
            type="primary"
            icon="Tickets"
            @click="handleOpenBusinessDomain"
            v-hasPermi="['system:dict:list']"
          >
            维护业务域
          </el-button>
          <el-button
            type="primary"
            plain
            icon="CollectionTag"
            @click="handleOpenDictView"
            v-hasPermi="['system:dict:list']"
          >
            打开核算字典
          </el-button>
          <el-tag type="info">业务域统一来自系统字典 `cost_business_domain`</el-tag>
        </div>
      </div>
      <div class="scene-center__hero-note">
        <span class="scene-center__hero-note-label">治理说明</span>
        <strong class="scene-center__hero-note-title">场景是费用、变量、规则、发布的上游边界</strong>
        <span class="scene-center__hero-note-desc">
          场景可理解为合同、核算主题或业务方案。当前生效版本仅展示真实发布结果，线程一先收稳场景主数据与字典口径。
        </span>
      </div>
    </section>

    <section class="scene-center__metrics">
      <div v-for="item in metricItems" :key="item.label" class="scene-center__metric-card">
        <span class="scene-center__metric-label">{{ item.label }}</span>
        <strong class="scene-center__metric-value">{{ item.value }}</strong>
        <span class="scene-center__metric-desc">{{ item.desc }}</span>
      </div>
    </section>

      <el-alert
      title="核算字典规划统一挂在系统字典中心，线程一先完成业务域字典与场景核心字典收口，再为后续费用、变量、规则预留规划项。"
      type="info"
      show-icon
      :closable="false"
      class="scene-center__alert"
    />

    <el-form
      ref="queryRef"
      :model="queryParams"
      :inline="true"
      label-width="84px"
      v-show="showSearch"
      class="scene-center__query"
    >
      <el-form-item label="场景检索" prop="keyword">
        <el-input
          v-model="queryParams.keyword"
          placeholder="请输入场景编码或场景名称"
          clearable
          style="width: 240px"
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="业务域" prop="businessDomain">
        <el-select
          v-model="queryParams.businessDomain"
          placeholder="请选择业务域"
          clearable
          style="width: 220px"
        >
          <el-option
            v-for="item in businessDomainOptions"
            :key="item.value"
            :label="item.label"
            :value="item.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="场景类型" prop="sceneType">
        <el-select
          v-model="queryParams.sceneType"
          placeholder="请选择场景类型"
          clearable
          style="width: 220px"
        >
          <el-option
            v-for="item in sceneTypeOptions"
            :key="item.value"
            :label="item.label"
            :value="item.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="适用组织" prop="orgCode">
        <el-input
          v-model="queryParams.orgCode"
          placeholder="请输入组织编码"
          clearable
          style="width: 220px"
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-select
          v-model="queryParams.status"
          placeholder="请选择状态"
          clearable
          style="width: 180px"
        >
          <el-option
            v-for="item in sceneStatusOptions"
            :key="item.value"
            :label="item.label"
            :value="item.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button
          type="primary"
          plain
          icon="Plus"
          @click="handleAdd"
          v-hasPermi="['cost:scene:add']"
        >
          新增
        </el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="success"
          plain
          icon="Edit"
          :disabled="single"
          @click="handleUpdate"
          v-hasPermi="['cost:scene:edit']"
        >
          修改
        </el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="danger"
          plain
          icon="Delete"
          :disabled="multiple"
          @click="handleDelete"
          v-hasPermi="['cost:scene:remove']"
        >
          删除
        </el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="warning"
          plain
          icon="Download"
          @click="handleExport"
          v-hasPermi="['cost:scene:export']"
        >
          导出
        </el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="sceneList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column type="index" label="序号" width="70" align="center" />
      <el-table-column label="场景编码" align="center" prop="sceneCode" min-width="150" />
      <el-table-column label="场景名称" align="center" prop="sceneName" min-width="180" :show-overflow-tooltip="true" />
      <el-table-column label="业务域" align="center" prop="businessDomain" min-width="140">
        <template #default="scope">
          <dict-tag :options="businessDomainOptions" :value="scope.row.businessDomain" />
        </template>
      </el-table-column>
      <el-table-column label="场景类型" align="center" prop="sceneType" min-width="140">
        <template #default="scope">
          <dict-tag :options="sceneTypeOptions" :value="scope.row.sceneType" />
        </template>
      </el-table-column>
      <el-table-column label="适用组织" align="center" prop="orgCode" min-width="140" :show-overflow-tooltip="true" />
      <el-table-column label="当前生效版本" align="center" prop="activeVersionId" width="140">
        <template #default="scope">
          <el-tag v-if="scope.row.activeVersionNo" type="success">{{ scope.row.activeVersionNo }}</el-tag>
          <el-tag v-else-if="scope.row.activeVersionId" type="success">#{{ scope.row.activeVersionId }}</el-tag>
          <span v-else class="scene-center__muted">未生效</span>
        </template>
      </el-table-column>
      <el-table-column label="状态" align="center" prop="status" width="110">
        <template #default="scope">
          <dict-tag :options="sceneStatusOptions" :value="scope.row.status" />
        </template>
      </el-table-column>
      <el-table-column label="说明" align="center" prop="remark" min-width="220" :show-overflow-tooltip="true" />
      <el-table-column label="创建时间" align="center" prop="createTime" width="180">
        <template #default="scope">
          <span>{{ parseTime(scope.row.createTime) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center" width="250" class-name="small-padding fixed-width" fixed="right">
        <template #default="scope">
          <el-button link type="primary" icon="View" @click="handleGovernance(scope.row)" v-hasPermi="['cost:scene:list']">
            治理
          </el-button>
          <el-button link type="primary" icon="Edit" @click="handleUpdate(scope.row)" v-hasPermi="['cost:scene:edit']">
            修改
          </el-button>
          <el-button link type="primary" icon="Tickets" @click="handleOpenPublishCenter(scope.row)" v-hasPermi="['cost:publish:list']">
            查看版本
          </el-button>
          <el-button link type="primary" icon="Delete" @click="handleDelete(scope.row)" v-hasPermi="['cost:scene:remove']">
            删除
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

    <el-dialog :title="title" v-model="open" width="620px" append-to-body>
      <el-form ref="sceneRef" :model="form" :rules="rules" label-width="100px">
        <div class="scene-center__dialog-tip">
          场景是平台第一层业务组织边界，可理解为合同、核算主题、业务方案或公司级核算域。
        </div>
        <el-row :gutter="18">
          <el-col :span="12">
            <el-form-item label="场景编码" prop="sceneCode">
              <el-input v-model="form.sceneCode" placeholder="如：PORT-SETTLEMENT-001" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="场景名称" prop="sceneName">
              <el-input v-model="form.sceneName" placeholder="请输入场景名称" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="业务域" prop="businessDomain">
              <el-select v-model="form.businessDomain" placeholder="请选择业务域" style="width: 100%">
                <el-option
                  v-for="item in businessDomainOptions"
                  :key="item.value"
                  :label="item.label"
                  :value="item.value"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="场景类型" prop="sceneType">
              <el-select v-model="form.sceneType" placeholder="请选择场景类型" style="width: 100%">
                <el-option
                  v-for="item in sceneTypeOptions"
                  :key="item.value"
                  :label="item.label"
                  :value="item.value"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="适用组织" prop="orgCode">
              <el-input v-model="form.orgCode" placeholder="请输入组织编码，如 ORG_PORT_001" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="状态" prop="status">
              <el-radio-group v-model="form.status">
                <el-radio
                  v-for="item in sceneStatusOptions"
                  :key="item.value"
                  :value="item.value"
                >
                  {{ item.label }}
                </el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="场景说明" prop="remark">
              <el-input v-model="form.remark" type="textarea" :rows="4" placeholder="补充业务口径、适用边界或维护说明" />
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

    <el-drawer v-model="governanceOpen" title="场景治理检查" size="520px" append-to-body>
      <div v-loading="governanceLoading" class="scene-governance">
        <template v-if="governanceInfo.sceneId">
          <div class="scene-governance__header">
            <div>
              <div class="scene-governance__title">{{ governanceInfo.sceneName }}</div>
              <div class="scene-governance__meta">
                <span>{{ governanceInfo.sceneCode }}</span>
                <span>业务域：{{ resolveDictLabel(businessDomainOptions, governanceInfo.businessDomain) }}</span>
              </div>
            </div>
            <dict-tag :options="sceneStatusOptions" :value="governanceInfo.status" />
          </div>

          <div class="scene-governance__grid">
            <div class="scene-governance__card">
              <span>费用</span>
              <strong>{{ governanceInfo.feeCount }}</strong>
            </div>
            <div class="scene-governance__card">
              <span>变量组</span>
              <strong>{{ governanceInfo.variableGroupCount }}</strong>
            </div>
            <div class="scene-governance__card">
              <span>变量</span>
              <strong>{{ governanceInfo.variableCount }}</strong>
            </div>
            <div class="scene-governance__card">
              <span>规则</span>
              <strong>{{ governanceInfo.ruleCount }}</strong>
            </div>
            <div class="scene-governance__card">
              <span>发布版本</span>
              <strong>{{ governanceInfo.publishedVersionCount }}</strong>
            </div>
            <div class="scene-governance__card">
              <span>当前生效版本</span>
              <strong>{{ governanceInfo.activeVersionId || '-' }}</strong>
            </div>
          </div>

          <el-alert
            :title="governanceInfo.canDelete ? '允许删除' : '当前不允许删除'"
            :description="governanceInfo.removeBlockingReason"
            :type="governanceInfo.canDelete ? 'success' : 'warning'"
            :closable="false"
            show-icon
          />
          <el-alert
            :title="governanceInfo.canDisable ? '允许停用' : '当前不允许停用'"
            :description="governanceInfo.disableBlockingReason"
            :type="governanceInfo.canDisable ? 'success' : 'warning'"
            :closable="false"
            show-icon
          />

          <div class="scene-governance__advice">
            <div class="scene-governance__advice-title">治理建议</div>
            <p>删除：{{ governanceInfo.removeAdvice }}</p>
            <p>停用：{{ governanceInfo.disableAdvice }}</p>
          </div>
        </template>
      </div>
    </el-drawer>
  </div>
</template>

<script setup name="CostScene">
import { ElMessageBox } from 'element-plus'
import { addScene, delScene, getScene, getSceneGovernance, getSceneStats, listScene, updateScene } from '@/api/cost/scene'
import { getRemoteDictOptionMap } from '@/utils/dictRemote'

const { proxy } = getCurrentInstance()
const router = useRouter()

const sceneList = ref([])
const businessDomainOptions = ref([])
const sceneStatusOptions = ref([])
const sceneTypeOptions = ref([])
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
const statistics = reactive({
  sceneCount: 0,
  enabledSceneCount: 0,
  businessDomainCount: 0
})

const data = reactive({
  form: {},
    queryParams: {
      pageNum: 1,
      pageSize: 10,
      keyword: undefined,
      businessDomain: undefined,
      sceneType: undefined,
      orgCode: undefined,
      status: undefined
    },
  rules: {
    sceneCode: [{ required: true, message: '场景编码不能为空', trigger: 'blur' }],
    sceneName: [{ required: true, message: '场景名称不能为空', trigger: 'blur' }],
    businessDomain: [{ required: true, message: '业务域不能为空', trigger: 'change' }],
    sceneType: [{ required: true, message: '场景类型不能为空', trigger: 'change' }],
    status: [{ required: true, message: '场景状态不能为空', trigger: 'change' }]
  }
})

const { queryParams, form, rules } = toRefs(data)

const filterStatusText = computed(() => {
  if (!queryParams.value.status) {
    return '全部状态'
  }
  const option = sceneStatusOptions.value.find(item => item.value === queryParams.value.status)
  return option ? option.label : queryParams.value.status
})

const filterDomainText = computed(() => {
  if (!queryParams.value.businessDomain) {
    return queryParams.value.orgCode ? `业务域：全部 · 组织：${queryParams.value.orgCode}` : '业务域：全部'
  }
  const option = businessDomainOptions.value.find(item => item.value === queryParams.value.businessDomain)
  const domainText = `业务域：${option ? option.label : queryParams.value.businessDomain}`
  return queryParams.value.orgCode ? `${domainText} · 组织：${queryParams.value.orgCode}` : domainText
})

const metricItems = computed(() => [
  {
    label: '场景总数',
    value: statistics.sceneCount,
    desc: '按当前检索条件汇总得到的场景规模'
  },
  {
    label: '启用场景数',
    value: statistics.enabledSceneCount,
    desc: '状态为“正常”的场景数量'
  },
  {
    label: '业务域覆盖数',
    value: statistics.businessDomainCount,
    desc: '当前结果覆盖的业务域种类'
  },
  {
    label: '当前筛选状态',
    value: filterStatusText.value,
    desc: filterDomainText.value
  }
])

function normalizeStats(data = {}) {
  return {
    sceneCount: Number(data.sceneCount || 0),
    enabledSceneCount: Number(data.enabledSceneCount || 0),
    businessDomainCount: Number(data.businessDomainCount || 0)
  }
}

async function loadSceneDictOptions() {
  const dictMap = await getRemoteDictOptionMap([
    'cost_business_domain',
    'cost_scene_status',
    'cost_scene_type'
  ])
  businessDomainOptions.value = dictMap.cost_business_domain || []
  sceneStatusOptions.value = dictMap.cost_scene_status || []
  sceneTypeOptions.value = dictMap.cost_scene_type || []
}

async function getList() {
  loading.value = true
  try {
    const [, listResponse, statsResponse] = await Promise.all([
      loadSceneDictOptions(),
      listScene(queryParams.value),
      getSceneStats(queryParams.value)
    ])
    sceneList.value = listResponse.rows
    total.value = listResponse.total
    Object.assign(statistics, normalizeStats(statsResponse.data))
  } finally {
    loading.value = false
  }
}

function cancel() {
  open.value = false
  reset()
}

function reset() {
  form.value = {
    sceneId: undefined,
    sceneCode: undefined,
    sceneName: undefined,
    businessDomain: undefined,
    orgCode: '',
    sceneType: 'CONTRACT',
    status: '0',
    remark: undefined
  }
  initialStatus.value = undefined
  proxy.resetForm('sceneRef')
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

function handleSelectionChange(selection) {
  ids.value = selection.map(item => item.sceneId)
  single.value = selection.length !== 1
  multiple.value = !selection.length
}

async function handleAdd() {
  await loadSceneDictOptions()
  reset()
  open.value = true
  title.value = '新增场景'
}

async function handleUpdate(row) {
  reset()
  const sceneId = row.sceneId || ids.value[0]
  const [, response] = await Promise.all([
    loadSceneDictOptions(),
    getScene(sceneId)
  ])
  form.value = {
    ...response.data,
    orgCode: response.data?.orgCode || ''
  }
  initialStatus.value = response.data?.status
  open.value = true
  title.value = '修改场景'
}

function submitForm() {
  proxy.$refs.sceneRef.validate(async valid => {
    if (valid) {
      const allowed = await ensureDisableAllowed()
      if (!allowed) {
        return
      }
      const request = form.value.sceneId ? updateScene(form.value) : addScene(form.value)
      request.then(() => {
        proxy.$modal.msgSuccess(form.value.sceneId ? '修改成功' : '新增成功')
        open.value = false
        getList()
      })
    }
  })
}

async function handleDelete(row) {
  const targetRows = resolveTargetRows(row)
  if (!targetRows.length) {
    return
  }
  const checks = await Promise.all(targetRows.map(item => fetchSceneGovernance(item.sceneId)))
  const blockedChecks = checks.filter(item => !item.canDelete)
  if (blockedChecks.length) {
    await ElMessageBox.alert(
      blockedChecks.map(item => `${item.sceneName}：${item.removeBlockingReason}`).join('<br/>'),
      '删除前治理检查',
      {
        type: 'warning',
        dangerouslyUseHTMLString: true
      }
    )
    if (blockedChecks[0]?.sceneId) {
      openGovernanceDrawer(blockedChecks[0])
    }
    return
  }

  const sceneIds = row?.sceneId || ids.value
  const sceneNames = targetRows.map(item => item.sceneName).join('、')
  proxy.$modal.confirm(`是否确认删除场景"${sceneNames}"的数据项？`).then(function() {
    return delScene(sceneIds)
  }).then(() => {
    getList()
    proxy.$modal.msgSuccess('删除成功')
  }).catch(() => {})
}

function handleExport() {
  proxy.download('cost/scene/export', {
    ...queryParams.value
  }, `scene_${new Date().getTime()}.xlsx`)
}

function handleOpenDictView() {
  router.push({
    path: '/cost/dict',
    query: {
      dictType: 'cost_',
      scope: 'cost'
    }
  })
}

function handleOpenBusinessDomain() {
  router.push({
    path: '/cost/dict',
    query: {
      dictType: 'cost_business_domain',
      scope: 'cost'
    }
  })
}

function handleOpenPublishCenter(row) {
  router.push({
    path: '/cost/publish',
    query: {
      sceneId: row.sceneId
    }
  })
}

async function handleGovernance(row) {
  governanceLoading.value = true
  governanceOpen.value = true
  try {
    governanceInfo.value = await fetchSceneGovernance(row.sceneId)
  } finally {
    governanceLoading.value = false
  }
}

function openGovernanceDrawer(check) {
  governanceInfo.value = check
  governanceOpen.value = true
}

async function fetchSceneGovernance(sceneId) {
  const response = await getSceneGovernance(sceneId)
  return normalizeGovernanceInfo(response.data)
}

function normalizeGovernanceInfo(data = {}) {
  return {
    sceneId: data.sceneId,
    sceneCode: data.sceneCode || '',
    sceneName: data.sceneName || '',
    businessDomain: data.businessDomain || '',
    status: data.status || '0',
    activeVersionId: data.activeVersionId,
    feeCount: Number(data.feeCount || 0),
    variableGroupCount: Number(data.variableGroupCount || 0),
    variableCount: Number(data.variableCount || 0),
    ruleCount: Number(data.ruleCount || 0),
    publishedVersionCount: Number(data.publishedVersionCount || 0),
    totalConfigCount: Number(data.totalConfigCount || 0),
    canDelete: Boolean(data.canDelete),
    canDisable: Boolean(data.canDisable),
    removeBlockingReason: data.removeBlockingReason || '当前场景可以删除',
    disableBlockingReason: data.disableBlockingReason || '当前场景可以停用',
    removeAdvice: data.removeAdvice || '',
    disableAdvice: data.disableAdvice || ''
  }
}

function resolveTargetRows(row) {
  if (row?.sceneId) {
    return [row]
  }
  return sceneList.value.filter(item => ids.value.includes(item.sceneId))
}

function resolveDictLabel(optionsRef, value) {
  const options = Array.isArray(optionsRef) ? optionsRef : (optionsRef?.value || [])
  const match = options.find(item => item.value === value)
  return match ? match.label : value || '-'
}

async function ensureDisableAllowed() {
  if (!form.value.sceneId || form.value.status !== '1' || initialStatus.value === '1') {
    return true
  }
  const check = await fetchSceneGovernance(form.value.sceneId)
  if (!check.canDisable) {
    openGovernanceDrawer(check)
    await ElMessageBox.alert(check.disableBlockingReason, '停用前治理检查', {
      type: 'warning'
    })
    return false
  }
  if (check.totalConfigCount > 0) {
    openGovernanceDrawer(check)
    try {
      await ElMessageBox.confirm(
        `当前场景下已有 ${check.totalConfigCount} 项配置对象，停用后将从业务选择范围中移除，但配置数据会继续保留，是否继续？`,
        '停用前治理检查',
        {
          type: 'warning'
        }
      )
    } catch (error) {
      return false
    }
  }
  return true
}

getList()
</script>

<style scoped lang="scss">
.scene-center {
  display: grid;
  gap: 18px;
}

.scene-center__hero {
  display: grid;
  grid-template-columns: minmax(0, 1.45fr) minmax(280px, 0.75fr);
  gap: 18px;
  padding: 24px 28px;
  border: 1px solid var(--el-border-color);
  border-radius: 18px;
  background: color-mix(in srgb, var(--el-color-primary-light-9) 16%, var(--el-bg-color-overlay));
  box-shadow: 0 14px 30px rgba(15, 23, 42, 0.06);
}

.scene-center__eyebrow {
  color: var(--el-color-primary);
  font-size: 13px;
  font-weight: 700;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.scene-center__title {
  margin: 10px 0 0;
  font-size: 28px;
  color: var(--el-text-color-primary);
}

.scene-center__subtitle {
  margin: 12px 0 0;
  color: var(--el-text-color-regular);
  font-size: 14px;
  line-height: 1.9;
}

.scene-center__hero-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: 18px;
}

.scene-center__hero-note {
  display: grid;
  gap: 10px;
  align-content: center;
  padding: 20px 22px;
  border-radius: 16px;
  border: 1px solid var(--el-border-color);
  background: color-mix(in srgb, var(--el-color-warning-light-9) 48%, var(--el-bg-color-overlay));
}

.scene-center__hero-note-label,
.scene-center__metric-label,
.scene-center__metric-desc {
  color: var(--el-text-color-secondary);
  font-size: 13px;
}

.scene-center__hero-note-title {
  font-size: 18px;
  line-height: 1.5;
  color: var(--el-text-color-primary);
}

.scene-center__hero-note-desc {
  color: var(--el-text-color-regular);
  font-size: 14px;
  line-height: 1.8;
}

.scene-center__metrics {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 16px;
}

.scene-center__metric-card {
  display: grid;
  gap: 8px;
  padding: 18px 20px;
  border: 1px solid var(--el-border-color-light);
  border-radius: 16px;
  background: var(--el-bg-color-overlay);
}

.scene-center__metric-value {
  color: var(--el-color-primary);
  font-size: 30px;
  line-height: 1.1;
  font-weight: 700;
}

.scene-center__alert,
.scene-center__query {
  margin-bottom: 0;
}

.scene-center__dialog-tip {
  margin-bottom: 18px;
  padding: 12px 14px;
  border-radius: 12px;
  color: var(--el-text-color-regular);
  background: color-mix(in srgb, var(--el-color-primary-light-9) 32%, var(--el-bg-color-overlay));
  line-height: 1.8;
}

.scene-center__muted {
  color: var(--el-text-color-secondary);
}

.scene-governance {
  display: grid;
  gap: 16px;
}

.scene-governance__header {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  padding: 18px 18px 14px;
  border: 1px solid var(--el-border-color-light);
  border-radius: 14px;
  background: color-mix(in srgb, var(--el-color-primary-light-9) 18%, var(--el-bg-color-overlay));
}

.scene-governance__title {
  font-size: 18px;
  font-weight: 700;
  color: var(--el-text-color-primary);
}

.scene-governance__meta {
  display: flex;
  flex-wrap: wrap;
  gap: 8px 14px;
  margin-top: 8px;
  color: var(--el-text-color-secondary);
  font-size: 13px;
}

.scene-governance__grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
}

.scene-governance__card {
  display: grid;
  gap: 8px;
  padding: 16px;
  border: 1px solid var(--el-border-color-light);
  border-radius: 12px;
  background: var(--el-bg-color-overlay);
}

.scene-governance__card span {
  font-size: 13px;
  color: var(--el-text-color-secondary);
}

.scene-governance__card strong {
  font-size: 24px;
  color: var(--el-color-primary);
}

.scene-governance__advice {
  padding: 16px 18px;
  border-radius: 12px;
  background: color-mix(in srgb, var(--el-color-warning-light-9) 42%, var(--el-bg-color-overlay));
  color: var(--el-text-color-regular);
}

.scene-governance__advice-title {
  margin-bottom: 8px;
  font-weight: 700;
  color: var(--el-text-color-primary);
}

.scene-governance__advice p {
  margin: 0;
  line-height: 1.9;
}

@media (max-width: 1200px) {
  .scene-center__hero,
  .scene-center__metrics {
    grid-template-columns: 1fr;
  }

  .scene-governance__grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}
</style>

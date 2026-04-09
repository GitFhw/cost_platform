<template>
   <div class="app-container">
      <el-alert
         v-if="isCostScope"
         title="当前已切换到核算字典视角，默认按 cost_ 前缀过滤，用于集中维护业务域字典与系统字典规划。"
         type="info"
         show-icon
         :closable="false"
         style="margin-bottom: 16px"
      />
      <section v-if="isCostScope" class="cost-dict-board">
         <div class="cost-dict-board__stats">
            <div class="cost-dict-board__stat">
               <span>核算字典类型</span>
               <strong>{{ costCatalog.length }}</strong>
               <small>当前已建档的 cost_ 字典类型</small>
            </div>
            <div class="cost-dict-board__stat">
               <span>启用类型</span>
               <strong>{{ enabledCostCatalogCount }}</strong>
               <small>状态为正常的字典类型</small>
            </div>
            <div class="cost-dict-board__stat">
               <span>规划字典</span>
               <strong>{{ planningReadyText }}</strong>
               <small>第一阶段预留的系统字典规划</small>
            </div>
         </div>
         <div class="cost-dict-board__cards">
            <article v-for="item in costFocusCards" :key="item.title" class="cost-dict-card">
               <div class="cost-dict-card__title">{{ item.title }}</div>
               <div class="cost-dict-card__desc">{{ item.desc }}</div>
               <div class="cost-dict-card__meta">{{ item.meta }}</div>
               <div class="cost-dict-card__actions">
                  <el-button link type="primary" @click="handleCostFocus(item.queryType)">查看类型</el-button>
                  <el-button
                     v-if="item.dictType"
                     link
                     type="primary"
                     @click="handleOpenCostDictData(item.dictType)"
                  >
                     维护数据
                  </el-button>
               </div>
            </article>
         </div>
         <div class="cost-dict-board__tags">
            <span class="cost-dict-board__tags-label">规划字典：</span>
            <el-tag v-for="item in planningDictTypes" :key="item.dictType" size="small" effect="plain">
               {{ item.label }}
            </el-tag>
         </div>
      </section>
      <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch" label-width="68px">
         <el-form-item label="字典名称" prop="dictName">
            <el-input
               v-model="queryParams.dictName"
               placeholder="请输入字典名称"
               clearable
               style="width: 240px"
               @keyup.enter="handleQuery"
            />
         </el-form-item>
      <el-form-item label="字典类型" prop="dictType">
            <el-input
               v-if="!isCostScope"
               v-model="queryParams.dictType"
               placeholder="请输入字典类型"
               clearable
               style="width: 240px"
               @keyup.enter="handleQuery"
            />
            <el-input
               v-else
               v-model="costQuerySuffix"
               placeholder="请输入 cost_ 后缀"
               clearable
               style="width: 240px"
               @keyup.enter="handleQuery"
            >
               <template #prepend>cost_</template>
            </el-input>
         </el-form-item>
         <el-form-item label="状态" prop="status">
            <el-select
               v-model="queryParams.status"
               placeholder="字典状态"
               clearable
               style="width: 240px"
            >
               <el-option
                  v-for="dict in statusOptions"
                  :key="dict.value"
                  :label="dict.label"
                  :value="dict.value"
               />
            </el-select>
         </el-form-item>
         <el-form-item label="创建时间" style="width: 308px">
            <el-date-picker
               v-model="dateRange"
               value-format="YYYY-MM-DD"
               type="daterange"
               range-separator="-"
               start-placeholder="开始日期"
               end-placeholder="结束日期"
            ></el-date-picker>
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
               v-hasPermi="['system:dict:add']"
            >新增</el-button>
         </el-col>
         <el-col :span="1.5">
            <el-button
               type="success"
               plain
               icon="Edit"
               :disabled="single"
               @click="handleUpdate"
               v-hasPermi="['system:dict:edit']"
            >修改</el-button>
         </el-col>
         <el-col :span="1.5">
            <el-button
               type="danger"
               plain
               icon="Delete"
               :disabled="multiple"
               @click="handleDelete"
               v-hasPermi="['system:dict:remove']"
            >删除</el-button>
         </el-col>
         <el-col :span="1.5">
            <el-button
               type="warning"
               plain
               icon="Download"
               @click="handleExport"
               v-hasPermi="['system:dict:export']"
            >导出</el-button>
         </el-col>
         <el-col :span="1.5">
            <el-button
               type="danger"
               plain
               icon="Refresh"
               @click="handleRefreshCache"
               v-hasPermi="['system:dict:remove']"
            >刷新缓存</el-button>
         </el-col>
         <right-toolbar v-model:showSearch="showSearch" @queryTable="getList"></right-toolbar>
      </el-row>

      <el-table v-loading="loading" :data="typeList" @selection-change="handleSelectionChange">
         <el-table-column type="selection" width="55" align="center" />
         <el-table-column label="字典编号" align="center" prop="dictId" />
         <el-table-column label="字典名称" align="center" prop="dictName" :show-overflow-tooltip="true"/>
         <el-table-column label="字典类型" align="center" :show-overflow-tooltip="true">
            <template #default="scope">
               <a class="link-type" style="cursor:pointer" @click="handleViewData(scope.row)">{{ scope.row.dictType }}</a>
            </template>
         </el-table-column>
         <el-table-column label="状态" align="center" prop="status">
            <template #default="scope">
               <dict-tag :options="statusOptions" :value="scope.row.status" />
            </template>
         </el-table-column>
         <el-table-column label="备注" align="center" prop="remark" :show-overflow-tooltip="true" />
         <el-table-column label="创建时间" align="center" prop="createTime" width="180">
            <template #default="scope">
               <span>{{ parseTime(scope.row.createTime) }}</span>
            </template>
         </el-table-column>
         <el-table-column label="操作" align="center" width="280" class-name="small-padding fixed-width">
            <template #default="scope">
               <el-button link type="primary" icon="Edit" @click="handleUpdate(scope.row)" v-hasPermi="['system:dict:edit']">修改</el-button>
               <el-button link type="primary" icon="Operation" @click="handleDataList(scope.row)" v-hasPermi="['system:dict:edit']">列表</el-button>
               <el-button link type="primary" icon="Delete" @click="handleDelete(scope.row)" v-hasPermi="['system:dict:remove']">删除</el-button>
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

      <!-- 添加或修改参数配置对话框 -->
      <el-dialog :title="title" v-model="open" width="500px" append-to-body>
         <el-form ref="dictRef" :model="form" :rules="rules" label-width="100px">
            <el-form-item label="字典名称" prop="dictName">
               <el-input v-model="form.dictName" placeholder="请输入字典名称" />
            </el-form-item>
            <el-form-item prop="dictType">
               <el-input
                  v-if="!isCostScope"
                  v-model="form.dictType"
                  placeholder="请输入字典类型"
               />
               <el-input
                  v-else
                  v-model="costFormSuffix"
                  placeholder="请输入 cost_ 后缀"
               >
                  <template #prepend>cost_</template>
               </el-input>
               <template #label>
                 <span>
                   <el-tooltip content='数据存储中的Key值，如：sys_user_sex' placement="top">
                     <el-icon><question-filled /></el-icon>
                   </el-tooltip>
                   字典类型
                 </span>
               </template>
            </el-form-item>
            <el-form-item label="状态" prop="status">
               <el-radio-group v-model="form.status">
                  <el-radio
                     v-for="dict in statusOptions"
                     :key="dict.value"
                     :value="dict.value"
                  >{{ dict.label }}</el-radio>
               </el-radio-group>
            </el-form-item>
            <el-form-item label="备注" prop="remark">
               <el-input v-model="form.remark" type="textarea" placeholder="请输入内容"></el-input>
            </el-form-item>
         </el-form>
         <template #footer>
            <div class="dialog-footer">
               <el-button type="primary" @click="submitForm">确 定</el-button>
               <el-button @click="cancel">取 消</el-button>
            </div>
         </template>
      </el-dialog>

      <dict-data-drawer v-model:visible="drawerVisible" :row="drawerRow" />
   </div>
</template>

<script setup name="Dict">
import DictDataDrawer from './detail'
import { listType, getType, delType, addType, updateType, refreshCache } from "@/api/system/dict/type"
import { getRemoteDictOptions } from '@/utils/dictRemote'

const { proxy } = getCurrentInstance()
const route = useRoute()
const router = useRouter()

const typeList = ref([])
const costCatalog = ref([])
const statusOptions = ref([])
const open = ref(false)
const loading = ref(true)
const showSearch = ref(true)
const ids = ref([])
const single = ref(true)
const multiple = ref(true)
const total = ref(0)
const title = ref("")
const dateRange = ref([])
const drawerVisible = ref(false)
const drawerRow = ref({})
const COST_DICT_PREFIX = "cost_"
const isCostScope = computed(() => route.query.scope === "cost")
const planningDictTypes = [
  { dictType: "cost_cargo_type", label: "货种" },
  { dictType: "cost_trade_type", label: "内外贸" },
  { dictType: "cost_shift_type", label: "班次" },
  { dictType: "cost_job_type", label: "工种" },
  { dictType: "cost_post_type", label: "岗位" },
  { dictType: "cost_customer_level", label: "客户等级" },
  { dictType: "cost_currency", label: "币种" },
  { dictType: "cost_partner_team", label: "协力队" },
  { dictType: "cost_mine_three_flag", label: "是否矿三" }
]

const data = reactive({
  form: {},
  queryParams: {
    pageNum: 1,
    pageSize: 10,
    dictName: undefined,
    dictType: undefined,
    status: undefined
  },
  rules: {
    dictName: [{ required: true, message: "字典名称不能为空", trigger: "blur" }],
    dictType: [{ validator: validateDictType, trigger: "blur" }]
  },
})

const { queryParams, form, rules } = toRefs(data)
const costQuerySuffix = computed({
  get() {
    return stripCostDictPrefix(queryParams.value.dictType)
  },
  set(value) {
    queryParams.value.dictType = normalizeDictTypeValue(value, { forceCostPrefix: true })
  }
})
const costFormSuffix = computed({
  get() {
    return stripCostDictPrefix(form.value.dictType)
  },
  set(value) {
    form.value.dictType = normalizeDictTypeValue(value, { forceCostPrefix: true })
  }
})
const enabledCostCatalogCount = computed(() => costCatalog.value.filter(item => item.status === "0").length)
const planningReadyCount = computed(() =>
  planningDictTypes.filter(item => costCatalog.value.some(dict => dict.dictType === item.dictType)).length
)
const planningReadyText = computed(() => `${planningReadyCount.value}/${planningDictTypes.length}`)
const costFocusCards = computed(() => {
  const businessDomain = findCostDict("cost_business_domain")
  const sceneStatus = findCostDict("cost_scene_status")
  const sceneType = findCostDict("cost_scene_type")
  return [
    {
      title: "业务域字典",
      desc: "维护跨行业业务边界，是场景中心的第一层分类入口。",
      meta: businessDomain ? `${businessDomain.dictName} · ${resolveStatusLabel(businessDomain.status)}` : "尚未建档",
      dictType: "cost_business_domain",
      queryType: "cost_business_domain"
    },
    {
      title: "场景核心字典",
      desc: "统一维护场景状态和场景类型，收稳场景中心的字段口径。",
      meta: `已建档 ${[sceneStatus, sceneType].filter(Boolean).length}/2`,
      dictType: "cost_scene_type",
      queryType: "cost_scene_"
    },
    {
      title: "规划字典",
      desc: "承接后续费用、变量、规则中心的业务口径规划。",
      meta: `已建档 ${planningReadyText.value}`,
      dictType: undefined,
      queryType: "cost_"
    }
  ]
})

function resolveScopedValue(value) {
  return typeof value === "string" && value !== "" ? value : undefined
}

function normalizeDictTypeValue(value, options = {}) {
  const { forceCostPrefix = false } = options
  const compact = String(value ?? "").replace(/[\s\u3000]+/g, "")
  if (!compact) {
    return forceCostPrefix ? COST_DICT_PREFIX : ""
  }
  if (!forceCostPrefix) {
    return compact
  }
  if (compact.startsWith(COST_DICT_PREFIX)) {
    return compact
  }
  return `${COST_DICT_PREFIX}${compact}`
}

function stripCostDictPrefix(value) {
  const normalized = normalizeDictTypeValue(value)
  return normalized.startsWith(COST_DICT_PREFIX)
    ? normalized.slice(COST_DICT_PREFIX.length)
    : normalized
}

function validateDictType(rule, value, callback) {
  const normalized = normalizeDictTypeValue(value, { forceCostPrefix: isCostScope.value })
  if (!normalized) {
    callback(new Error("字典类型不能为空"))
    return
  }
  if (isCostScope.value && normalized === COST_DICT_PREFIX) {
    callback(new Error("请补充 cost_ 后缀，例如 business_domain"))
    return
  }
  callback()
}

function normalizeQueryDictType() {
  queryParams.value.dictType = normalizeDictTypeValue(queryParams.value.dictType, { forceCostPrefix: isCostScope.value })
}

function normalizeFormDictType() {
  form.value.dictType = normalizeDictTypeValue(form.value.dictType, { forceCostPrefix: isCostScope.value })
}

function getScopedQueryDefaults() {
  return {
    dictName: resolveScopedValue(route.query.dictName),
    dictType: normalizeDictTypeValue(resolveScopedValue(route.query.dictType), { forceCostPrefix: isCostScope.value }),
    status: resolveScopedValue(route.query.status)
  }
}

function applyScopedQueryDefaults(resetPage = false) {
  Object.assign(queryParams.value, getScopedQueryDefaults())
  if (resetPage) {
    queryParams.value.pageNum = 1
  }
}

async function loadStatusOptions() {
  statusOptions.value = await getRemoteDictOptions("sys_normal_disable")
}

/** 查询字典类型列表 */
async function getList() {
  loading.value = true
  try {
    const response = await listType(proxy.addDateRange(queryParams.value, dateRange.value))
    typeList.value = response.rows
    total.value = response.total
  } finally {
    loading.value = false
  }
}

async function loadCostCatalog() {
  if (!isCostScope.value) {
    costCatalog.value = []
    return
  }
  try {
    const response = await listType({ pageNum: 1, pageSize: 100, dictType: "cost_" })
    costCatalog.value = response.rows || []
  } catch (error) {
    costCatalog.value = []
  }
}

/** 取消按钮 */
function cancel() {
  open.value = false
  reset()
}

/** 表单重置 */
function reset() {
  form.value = {
    dictId: undefined,
    dictName: undefined,
    dictType: isCostScope.value
      ? normalizeDictTypeValue(resolveScopedValue(route.query.dictType), { forceCostPrefix: true })
      : undefined,
    status: "0",
    remark: undefined
  }
  proxy.resetForm("dictRef")
}

/** 搜索按钮操作 */
function handleQuery() {
  normalizeQueryDictType()
  queryParams.value.pageNum = 1
  getList()
}

/** 重置按钮操作 */
function resetQuery() {
  dateRange.value = []
  proxy.resetForm("queryRef")
  applyScopedQueryDefaults(true)
  handleQuery()
}

/** 新增按钮操作 */
function handleAdd() {
  reset()
  open.value = true
  title.value = "添加字典类型"
}

/** 多选框选中数据 */
function handleSelectionChange(selection) {
  ids.value = selection.map(item => item.dictId)
  single.value = selection.length != 1
  multiple.value = !selection.length
}

/** 字典数据抽屉 */
function handleViewData(row) {
  drawerRow.value = row
  drawerVisible.value = true
}

/** 字典数据列表页面 */
function handleDataList(row) {
  proxy.$tab.openPage("字典数据", '/system/dict-data/index/' + row.dictId)
}

function handleCostFocus(dictType) {
  router.push({
    path: route.path,
    query: {
      ...route.query,
      scope: "cost",
      dictType
    }
  })
}

function handleOpenCostDictData(dictType) {
  const row = findCostDict(dictType)
  if (!row) {
    proxy.$modal.msgError(`未找到字典类型 ${dictType}，请先完成初始化数据。`)
    return
  }
  handleDataList(row)
}

/** 修改按钮操作 */
function handleUpdate(row) {
  reset()
  const dictId = row.dictId || ids.value
  getType(dictId).then(response => {
    form.value = {
      ...response.data,
      dictType: normalizeDictTypeValue(response.data?.dictType, { forceCostPrefix: isCostScope.value })
    }
    open.value = true
    title.value = "修改字典类型"
  })
}

/** 提交按钮 */
function submitForm() {
  normalizeFormDictType()
  proxy.$refs["dictRef"].validate(valid => {
    if (valid) {
      if (form.value.dictId != undefined) {
        updateType(form.value).then(response => {
          proxy.$modal.msgSuccess("修改成功")
          open.value = false
          getList()
          loadCostCatalog()
        })
      } else {
        addType(form.value).then(response => {
          proxy.$modal.msgSuccess("新增成功")
          open.value = false
          getList()
          loadCostCatalog()
        })
      }
    }
  })
}

/** 删除按钮操作 */
function handleDelete(row) {
  const dictIds = row.dictId || ids.value
  proxy.$modal.confirm('是否确认删除字典编号为"' + dictIds + '"的数据项？').then(function() {
    return delType(dictIds)
  }).then(() => {
    getList()
    loadCostCatalog()
    proxy.$modal.msgSuccess("删除成功")
  }).catch(() => {})
}

/** 导出按钮操作 */
function handleExport() {
  proxy.download("system/dict/type/export", {
    ...queryParams.value
  }, `dict_${new Date().getTime()}.xlsx`)
}

/** 刷新缓存按钮操作 */
async function handleRefreshCache() {
  await refreshCache()
  await Promise.all([loadStatusOptions(), getList(), loadCostCatalog()])
  proxy.$modal.msgSuccess("刷新成功")
}

watch(() => route.query, async () => {
  applyScopedQueryDefaults(true)
  await Promise.all([loadStatusOptions(), getList(), loadCostCatalog()])
}, { immediate: true, deep: true })

function findCostDict(dictType) {
  return costCatalog.value.find(item => item.dictType === dictType)
}

function resolveStatusLabel(status) {
  const option = statusOptions.value.find(item => item.value === status)
  return option ? option.label : status || "未知"
}
</script>

<style scoped>
.cost-dict-board {
  display: grid;
  gap: 16px;
  margin-bottom: 18px;
}

.cost-dict-board__stats {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 14px;
}

.cost-dict-board__stat,
.cost-dict-card {
  padding: 16px 18px;
  border: 1px solid var(--el-border-color-light);
  border-radius: 14px;
  background: var(--el-bg-color-overlay);
}

.cost-dict-board__stat {
  display: grid;
  gap: 8px;
}

.cost-dict-board__stat span,
.cost-dict-board__stat small,
.cost-dict-card__desc,
.cost-dict-card__meta,
.cost-dict-board__tags-label {
  color: var(--el-text-color-secondary);
}

.cost-dict-board__stat strong {
  font-size: 28px;
  line-height: 1.1;
  color: var(--el-color-primary);
}

.cost-dict-board__cards {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 14px;
}

.cost-dict-card {
  display: grid;
  gap: 10px;
}

.cost-dict-card__title {
  font-size: 16px;
  font-weight: 700;
  color: var(--el-text-color-primary);
}

.cost-dict-card__desc,
.cost-dict-card__meta {
  font-size: 13px;
  line-height: 1.7;
}

.cost-dict-card__actions {
  display: flex;
  gap: 12px;
  margin-top: 4px;
}

.cost-dict-board__tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  align-items: center;
}

@media (max-width: 1200px) {
  .cost-dict-board__stats,
  .cost-dict-board__cards {
    grid-template-columns: 1fr;
  }
}
</style>

<template>
  <div class="app-container open-app-page">
    <section class="open-app-page__hero">
      <div>
        <div class="open-app-page__eyebrow">开放接入治理</div>
        <h2>应用权限与鉴权配置中心</h2>
        <p>
          面向第三方系统维护独立应用身份、场景授权、草稿联调权限和令牌有效期。
          这里沉淀的是平台级接入契约，不再和单个接入方案混在一起维护。
        </p>
      </div>
      <div class="open-app-page__hero-actions">
        <el-button icon="Connection" @click="router.push(COST_MENU_ROUTES.access)">打开接入中心</el-button>
        <el-button icon="Document" @click="activeDetailTab = 'manual'">查看调用手册</el-button>
        <el-button type="primary" icon="Plus" @click="handleOpenDialog('add')" v-hasPermi="['cost:openApp:add']">新建开放应用</el-button>
      </div>
    </section>

    <section class="open-app-page__metrics">
      <div v-for="item in metricItems" :key="item.label" class="open-app-page__metric-card">
        <span>{{ item.label }}</span>
        <strong>{{ item.value }}</strong>
        <small>{{ item.desc }}</small>
      </div>
    </section>

    <section class="open-app-page__panel">
      <el-form :model="queryParams" inline label-width="84px" class="open-app-page__query-form">
        <el-form-item label="关键字">
          <el-input
            v-model="queryParams.searchValue"
            clearable
            style="width: 260px"
            placeholder="应用名称 / 编码 / 备注"
            @keyup.enter="handleQuery"
          />
        </el-form-item>
        <el-form-item label="场景授权">
          <el-select v-model="queryParams.sceneScopeType" clearable style="width: 180px" placeholder="全部范围">
            <el-option v-for="item in sceneScopeTypeOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="草稿联调">
          <el-select v-model="queryParams.allowDraftSnapshot" clearable style="width: 180px" placeholder="全部权限">
            <el-option label="允许草稿联调" :value="true" />
            <el-option label="仅生效版本" :value="false" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="queryParams.status" clearable style="width: 140px" placeholder="全部状态">
            <el-option v-for="dict in sys_normal_disable" :key="dict.value" :label="dict.label" :value="dict.value" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
          <el-button icon="Refresh" @click="resetQuery">重置</el-button>
        </el-form-item>
      </el-form>

      <div class="open-app-page__toolbar">
        <div class="open-app-page__toolbar-left">
          <el-button type="primary" plain icon="Plus" @click="handleOpenDialog('add')" v-hasPermi="['cost:openApp:add']">新建应用</el-button>
          <el-button plain icon="Edit" :disabled="!currentApp" @click="handleOpenDialog('edit')" v-hasPermi="['cost:openApp:edit']">修改配置</el-button>
          <el-button plain icon="DocumentCopy" :disabled="!currentApp" @click="handleOpenDialog('clone')" v-hasPermi="['cost:openApp:add']">复制应用</el-button>
          <el-button type="warning" plain icon="Key" :disabled="!currentApp" @click="handleResetSecret" v-hasPermi="['cost:openApp:resetSecret']">重置密钥</el-button>
          <el-button type="danger" plain icon="Delete" :disabled="!currentApp" @click="handleDelete" v-hasPermi="['cost:openApp:remove']">删除应用</el-button>
        </div>
        <right-toolbar v-model:showSearch="showSearch" @queryTable="loadList" />
      </div>

      <div class="open-app-page__workspace">
        <div class="open-app-page__ledger">
          <div class="open-app-page__section-head">
            <div>
              <h3>开放应用台账</h3>
              <p>按应用维度统一治理第三方接入身份，不再依赖人工共享通用账号或把权限散落到接入方案内部。</p>
            </div>
          </div>
          <el-table
            ref="tableRef"
            v-loading="loading"
            :data="openAppList"
            border
            highlight-current-row
            row-key="appId"
            @row-click="handleSelectRow"
          >
            <el-table-column label="应用" min-width="220">
              <template #default="scope">
                <div class="open-app-page__app-main">
                  <strong>{{ scope.row.appName }}</strong>
                  <span>{{ scope.row.appCode }}</span>
                </div>
              </template>
            </el-table-column>
            <el-table-column label="场景授权" min-width="170">
              <template #default="scope">
                <div class="open-app-page__scope-cell">
                  <el-tag :type="scope.row.sceneScopeType === 'ALL' ? 'success' : 'warning'" size="small">
                    {{ scope.row.sceneScopeType === 'ALL' ? '全部场景' : '指定场景' }}
                  </el-tag>
                  <span>{{ scope.row.sceneNamesSummary || '未配置' }}</span>
                </div>
              </template>
            </el-table-column>
            <el-table-column label="草稿联调" width="120" align="center">
              <template #default="scope">
                <el-tag :type="scope.row.allowDraftSnapshot ? 'success' : 'info'" size="small">
                  {{ scope.row.allowDraftSnapshot ? '允许' : '关闭' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="令牌时长" width="110" align="center">
              <template #default="scope">{{ formatTokenTtl(scope.row.tokenTtlSeconds) }}</template>
            </el-table-column>
            <el-table-column label="状态" width="90" align="center">
              <template #default="scope">
                <el-tag :type="resolveStatusTagType(scope.row.status)" size="small">{{ resolveStatusLabel(scope.row.status) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="时效" width="110" align="center">
              <template #default="scope">
                <el-tag :type="resolveEffectiveState(scope.row).type" size="small">{{ resolveEffectiveState(scope.row).label }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="更新时间" min-width="160">
              <template #default="scope">{{ scope.row.updateTime || scope.row.createTime || '-' }}</template>
            </el-table-column>
          </el-table>
          <pagination
            v-show="total > 0"
            :total="total"
            v-model:page="queryParams.pageNum"
            v-model:limit="queryParams.pageSize"
            @pagination="loadList"
          />
        </div>

        <div class="open-app-page__detail">
          <div class="open-app-page__section-head">
            <div>
              <h3>配置中心</h3>
              <p>查看当前应用的场景授权、令牌口径、草稿联调边界和对外调用契约。</p>
            </div>
            <div v-if="currentApp" class="open-app-page__detail-actions">
              <el-button text icon="CopyDocument" @click="copyText(currentApp.appCode, '应用编码已复制')">复制 AppCode</el-button>
              <el-button text icon="Key" @click="handleResetSecret" v-hasPermi="['cost:openApp:resetSecret']">重置密钥</el-button>
            </div>
          </div>

          <div v-if="currentApp" class="open-app-page__detail-summary">
            <div v-for="item in currentSummaryItems" :key="item.label" class="open-app-page__summary-card">
              <span>{{ item.label }}</span>
              <strong>{{ item.value }}</strong>
              <small>{{ item.desc }}</small>
            </div>
          </div>

          <el-alert
            v-else
            class="open-app-page__selection-tip"
            :closable="false"
            type="info"
            show-icon
            title="当前尚未选中开放应用"
            description="你可以先在左侧台账里选中一个应用查看授权边界，也可以先阅读下方的调用手册和开放接口契约。"
          />

          <el-tabs v-model="activeDetailTab" class="open-app-page__detail-tabs">
            <el-tab-pane label="基本信息" name="base">
              <el-descriptions v-if="currentApp" :column="1" border>
                <el-descriptions-item label="应用编码">{{ currentApp.appCode }}</el-descriptions-item>
                <el-descriptions-item label="应用名称">{{ currentApp.appName }}</el-descriptions-item>
                <el-descriptions-item label="鉴权方式">AppCode + AppSecret 换取 Bearer Token</el-descriptions-item>
                <el-descriptions-item label="令牌时长">{{ formatTokenTtl(currentApp.tokenTtlSeconds) }}（{{ currentApp.tokenTtlSeconds }} 秒）</el-descriptions-item>
                <el-descriptions-item label="创建人">{{ currentApp.createBy || '-' }}</el-descriptions-item>
                <el-descriptions-item label="更新时间">{{ currentApp.updateTime || currentApp.createTime || '-' }}</el-descriptions-item>
                <el-descriptions-item label="备注">{{ currentApp.remark || '未填写' }}</el-descriptions-item>
              </el-descriptions>
              <el-empty v-else :image-size="56" description="请选择开放应用后查看基础配置。" />
            </el-tab-pane>
            <el-tab-pane label="授权范围" name="scope">
              <template v-if="currentApp">
                <el-alert
                  :closable="false"
                  show-icon
                  type="info"
                  :title="currentApp.sceneScopeType === 'ALL' ? '当前应用已开通全部场景权限' : '当前应用只允许访问指定场景'"
                  :description="currentApp.sceneScopeType === 'ALL'
                    ? '第三方拿到 token 后可查询并调用所有被平台暴露的场景。'
                    : '若第三方需要新增场景，请在这里补充授权并重新发放 token。'"
                />
                <div class="open-app-page__scene-list">
                  <el-tag
                    v-for="item in currentSceneNames"
                    :key="item"
                    class="open-app-page__scene-tag"
                    effect="plain"
                  >
                    {{ item }}
                  </el-tag>
                  <el-empty v-if="!currentSceneNames.length && currentApp.sceneScopeType !== 'ALL'" :image-size="56" description="当前没有配置可访问场景" />
                </div>
              </template>
              <el-empty v-else :image-size="56" description="请选择开放应用后查看场景授权边界。" />
            </el-tab-pane>
            <el-tab-pane label="联调约束" name="governance">
              <div v-if="currentApp" class="open-app-page__advice-list">
                <el-alert
                  :closable="false"
                  show-icon
                  :type="currentApp.allowDraftSnapshot ? 'success' : 'warning'"
                  :title="currentApp.allowDraftSnapshot ? '已允许草稿联调' : '当前只允许使用生效版本'"
                  :description="currentApp.allowDraftSnapshot
                    ? '第三方可以在接口请求中显式切到草稿快照做联调，但仍建议正式接入时使用已发布生效版本。'
                    : '第三方调用时只能基于当前生效版本口径。若需联调草稿规则，请先由管理员开通草稿权限。'"
                />
                <el-alert
                  :closable="false"
                  show-icon
                  type="info"
                  title="时效策略"
                  :description="buildEffectiveDescription(currentApp)"
                />
                <el-alert
                  :closable="false"
                  show-icon
                  type="info"
                  title="对外调用流程"
                  description="1) 用 appCode + appSecret 申请 token；2) 查询可访问场景与费用模板；3) 用 Bearer Token 调用单费用或多费用核算接口；4) token 过期后重新申请。"
                />
              </div>
              <el-empty v-else :image-size="56" description="请选择开放应用后查看联调和时效约束。" />
            </el-tab-pane>
            <el-tab-pane label="调用手册" name="manual">
              <div class="open-app-page__manual-grid">
                <div class="open-app-page__manual-card">
                  <div class="open-app-page__manual-head">
                    <strong>接入总原则</strong>
                    <el-tag size="small" type="success">企业级推荐</el-tag>
                  </div>
                  <ul class="open-app-page__manual-list">
                    <li>开放接口统一采用应用级身份鉴权，不再让第三方共享后台账号。</li>
                    <li>正式联调先查模板、再组 JSON、再做取价，不建议第三方直接猜测字段。</li>
                    <li>生产调用优先使用已发布生效版本，草稿快照仅用于联调验证。</li>
                    <li>若开放应用或 token 超过有效期，第三方必须重新换取访问资格。</li>
                  </ul>
                </div>

                <div class="open-app-page__manual-card">
                  <div class="open-app-page__manual-head">
                    <strong>鉴权与时效</strong>
                    <el-button text icon="CopyDocument" @click="copyText('/cost/open/auth/token', '令牌申请接口已复制')">复制令牌接口</el-button>
                  </div>
                  <div class="open-app-page__contract-pills">
                    <span class="open-app-page__contract-pill">鉴权模式：Bearer Token</span>
                    <span class="open-app-page__contract-pill">申请口径：AppCode + AppSecret</span>
                    <span class="open-app-page__contract-pill">推荐时长：2 小时 / 7200 秒</span>
                    <span class="open-app-page__contract-pill">过期后：重新申请 token</span>
                  </div>
                  <pre class="open-app-page__code-block">{{ authRequestExample }}</pre>
                </div>

                <div class="open-app-page__manual-card open-app-page__manual-card--wide">
                  <div class="open-app-page__manual-head">
                    <strong>典型接入顺序</strong>
                  </div>
                  <div class="open-app-page__step-grid">
                    <div v-for="item in integrationFlowSteps" :key="item.step" class="open-app-page__step-card">
                      <span>{{ item.step }}</span>
                      <strong>{{ item.title }}</strong>
                      <small>{{ item.desc }}</small>
                    </div>
                  </div>
                </div>

                <div class="open-app-page__manual-card open-app-page__manual-card--wide">
                  <div class="open-app-page__manual-head">
                    <strong>开放接口清单</strong>
                    <el-tag size="small" type="info">适合录入 Apifox</el-tag>
                  </div>
                  <div class="open-app-page__endpoint-list">
                    <div v-for="item in endpointItems" :key="item.path" class="open-app-page__endpoint-card">
                      <div class="open-app-page__endpoint-meta">
                        <el-tag :type="item.methodType" effect="light">{{ item.method }}</el-tag>
                        <code>{{ item.path }}</code>
                      </div>
                      <strong>{{ item.title }}</strong>
                      <span>{{ item.desc }}</span>
                      <small>{{ item.note }}</small>
                    </div>
                  </div>
                </div>

                <div class="open-app-page__manual-card">
                  <div class="open-app-page__manual-head">
                    <strong>Apifox 环境变量</strong>
                  </div>
                  <div class="open-app-page__contract-pills">
                    <span v-for="item in apifoxEnvItems" :key="item.name" class="open-app-page__contract-pill">
                      {{ item.name }} = {{ item.example }}
                    </span>
                  </div>
                </div>

                <div class="open-app-page__manual-card">
                  <div class="open-app-page__manual-head">
                    <strong>错误处理约定</strong>
                  </div>
                  <ul class="open-app-page__manual-list">
                    <li v-for="item in errorHandlingItems" :key="item">{{ item }}</li>
                  </ul>
                </div>

                <div class="open-app-page__manual-card open-app-page__manual-card--wide">
                  <div class="open-app-page__manual-head">
                    <strong>单费用 / 多费用调用示例</strong>
                    <el-button text icon="CopyDocument" @click="copyText(calculateRequestExample, '核算示例请求已复制')">复制示例</el-button>
                  </div>
                  <pre class="open-app-page__code-block">{{ calculateRequestExample }}</pre>
                </div>
              </div>
            </el-tab-pane>
          </el-tabs>
        </div>
      </div>
    </section>

    <el-dialog :title="dialogTitle" v-model="dialogOpen" width="760px" append-to-body>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="108px">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="应用编码" prop="appCode">
              <el-input v-model="form.appCode" maxlength="64" placeholder="如：LABOR_HR_APP" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="应用名称" prop="appName">
              <el-input v-model="form.appName" maxlength="128" placeholder="如：首钢人力第三方联调应用" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="场景授权" prop="sceneScopeType">
              <el-radio-group v-model="form.sceneScopeType">
                <el-radio-button v-for="item in sceneScopeTypeOptions" :key="item.value" :value="item.value">{{ item.label }}</el-radio-button>
              </el-radio-group>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="草稿联调">
              <el-switch v-model="form.allowDraftSnapshot" active-text="允许" inactive-text="关闭" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item v-if="form.sceneScopeType === 'LIST'" label="授权场景" prop="sceneIds">
          <el-select
            v-model="form.sceneIds"
            multiple
            filterable
            collapse-tags
            collapse-tags-tooltip
            style="width: 100%"
            placeholder="请选择可访问场景"
          >
            <el-option
              v-for="item in sceneOptions"
              :key="item.sceneId"
              :label="`${item.sceneName} / ${item.sceneCode}`"
              :value="item.sceneId"
            />
          </el-select>
        </el-form-item>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="令牌时长(秒)" prop="tokenTtlSeconds">
              <el-input-number v-model="form.tokenTtlSeconds" :min="300" :max="86400" controls-position="right" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="推荐口径">
              <el-select v-model="tokenPreset" style="width: 100%" placeholder="快速套用常见时长" @change="handleApplyTokenPreset">
                <el-option label="30 分钟 / 1800 秒" :value="1800" />
                <el-option label="2 小时 / 7200 秒" :value="7200" />
                <el-option label="6 小时 / 21600 秒" :value="21600" />
                <el-option label="24 小时 / 86400 秒" :value="86400" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="生效区间">
          <el-date-picker
            v-model="effectiveRange"
            type="datetimerange"
            start-placeholder="生效开始时间"
            end-placeholder="失效时间"
            value-format="YYYY-MM-DD HH:mm:ss"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="form.status">
            <el-radio v-for="dict in sys_normal_disable" :key="dict.value" :value="dict.value">{{ dict.label }}</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="form.remark" type="textarea" :rows="3" maxlength="500" show-word-limit placeholder="说明第三方系统名称、联调负责人或特殊限制。" />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button type="primary" @click="submitForm">确定</el-button>
          <el-button @click="dialogOpen = false">取消</el-button>
        </div>
      </template>
    </el-dialog>

    <el-dialog title="请立即保存应用密钥" v-model="secretDialogOpen" width="620px" append-to-body>
      <el-alert :closable="false" show-icon type="warning" :title="secretPayload.advice || '明文 appSecret 只会展示一次，请立即复制并妥善保存。'" />
      <el-descriptions :column="1" border class="open-app-page__secret-box">
        <el-descriptions-item label="应用编码">{{ secretPayload.app?.appCode || '-' }}</el-descriptions-item>
        <el-descriptions-item label="应用名称">{{ secretPayload.app?.appName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="AppSecret">
          <div class="open-app-page__secret-line">
            <span>{{ secretPayload.appSecret || '-' }}</span>
            <el-button text type="primary" icon="CopyDocument" @click="copyText(secretPayload.appSecret, 'AppSecret 已复制')">复制</el-button>
          </div>
        </el-descriptions-item>
      </el-descriptions>
      <template #footer>
        <div class="dialog-footer">
          <el-button type="primary" @click="secretDialogOpen = false">我已保存</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup name="CostOpenApp">
import { ElMessageBox } from 'element-plus'
import { addOpenApp, delOpenApp, getOpenApp, listOpenApp, listOpenAppSceneOptions, resetOpenAppSecret, updateOpenApp } from '@/api/cost/openApp'
import { COST_MENU_ROUTES } from '@/utils/costMenuRoutes'

const router = useRouter()
const { proxy } = getCurrentInstance()
const { sys_normal_disable } = proxy.useDict('sys_normal_disable')

const loading = ref(false)
const showSearch = ref(true)
const total = ref(0)
const openAppList = ref([])
const sceneOptions = ref([])
const currentApp = ref()
const currentAppId = ref()
const dialogOpen = ref(false)
const dialogMode = ref('add')
const secretDialogOpen = ref(false)
const secretPayload = ref({})
const effectiveRange = ref([])
const tokenPreset = ref()
const activeDetailTab = ref('manual')

const tableRef = ref()
const formRef = ref()

const queryParams = reactive({
  pageNum: 1,
  pageSize: 10,
  searchValue: '',
  sceneScopeType: undefined,
  allowDraftSnapshot: undefined,
  status: undefined
})

const form = reactive({
  appId: undefined,
  appCode: '',
  appName: '',
  sceneScopeType: 'ALL',
  sceneIds: [],
  allowDraftSnapshot: false,
  tokenTtlSeconds: 7200,
  effectiveStartTime: undefined,
  effectiveEndTime: undefined,
  status: '0',
  remark: ''
})

const rules = {
  appCode: [{ required: true, message: '应用编码不能为空', trigger: 'blur' }],
  appName: [{ required: true, message: '应用名称不能为空', trigger: 'blur' }],
  sceneScopeType: [{ required: true, message: '请选择场景授权范围', trigger: 'change' }],
  sceneIds: [{
    validator: (rule, value, callback) => {
      if (form.sceneScopeType === 'LIST' && (!value || !value.length)) {
        callback(new Error('指定场景授权时至少选择一个场景'))
        return
      }
      callback()
    },
    trigger: 'change'
  }],
  tokenTtlSeconds: [{ required: true, message: '令牌时长不能为空', trigger: 'change' }],
  status: [{ required: true, message: '状态不能为空', trigger: 'change' }]
}

const sceneScopeTypeOptions = [
  { label: '全部场景', value: 'ALL' },
  { label: '指定场景', value: 'LIST' }
]

const dialogTitle = computed(() => {
  if (dialogMode.value === 'edit') {
    return '修改开放应用'
  }
  if (dialogMode.value === 'clone') {
    return '复制开放应用'
  }
  return '新建开放应用'
})

const metricItems = computed(() => {
  const enabledCount = openAppList.value.filter(item => item.status === '0').length
  const draftCount = openAppList.value.filter(item => item.allowDraftSnapshot).length
  const expiringSoonCount = openAppList.value.filter(item => {
    const endTime = item.effectiveEndTime ? new Date(item.effectiveEndTime).getTime() : undefined
    if (!endTime || Number.isNaN(endTime)) {
      return false
    }
    const diff = endTime - Date.now()
    return diff > 0 && diff <= 7 * 24 * 60 * 60 * 1000
  }).length
  return [
    { label: '台账总数', value: total.value, desc: '当前筛选范围内的开放应用数量' },
    { label: '本页启用', value: enabledCount, desc: '当前页中状态正常的应用' },
    { label: '草稿联调', value: draftCount, desc: '当前页已开通草稿联调的应用' },
    { label: '即将到期', value: expiringSoonCount, desc: '未来 7 天内需要续期的应用' }
  ]
})

const currentSummaryItems = computed(() => {
  if (!currentApp.value) {
    return []
  }
  return [
    {
      label: '应用编码',
      value: currentApp.value.appCode,
      desc: '第三方申请 token 时使用'
    },
    {
      label: '授权场景',
      value: currentApp.value.sceneScopeType === 'ALL' ? '全部场景' : `${currentApp.value.sceneCount || 0} 个`,
      desc: currentApp.value.sceneNamesSummary || '未配置'
    },
    {
      label: '草稿联调',
      value: currentApp.value.allowDraftSnapshot ? '允许' : '关闭',
      desc: currentApp.value.allowDraftSnapshot ? '支持草稿版本联调' : '仅允许生效版本调用'
    },
    {
      label: '令牌时长',
      value: formatTokenTtl(currentApp.value.tokenTtlSeconds),
      desc: 'access token 过期后需重新申请'
    }
  ]
})

const currentSceneNames = computed(() => currentApp.value?.sceneNames || [])
const integrationFlowSteps = [
  { step: '01', title: '申请 token', desc: '第三方先使用 appCode 和 appSecret 申请短时访问令牌。' },
  { step: '02', title: '查询场景', desc: '按开放应用授权范围获取可访问场景，避免跨场景误调用。' },
  { step: '03', title: '确认版本', desc: '根据生产或联调口径选择 ACTIVE 或 DRAFT，并确认版本。' },
  { step: '04', title: '生成模板', desc: '按目标费用生成字段模板，明确变量中文名、路径和必填字段。' },
  { step: '05', title: '组织输入', desc: '第三方按模板组装对象或对象数组，优先保留 bizNo 方便定位错误。' },
  { step: '06', title: '执行核算', desc: '调用单费用或多费用核算接口，缺字段时按返回提示逐项修正。' }
]

const endpointItems = [
  {
    method: 'POST',
    methodType: 'success',
    path: '/cost/open/auth/token',
    title: '申请访问令牌',
    desc: '用 AppCode + AppSecret 换取短时 Bearer Token。',
    note: '无需 Bearer，成功后返回 accessToken、expiresAt、草稿权限和场景授权范围。'
  },
  {
    method: 'GET',
    methodType: 'primary',
    path: '/cost/open/scenes',
    title: '查询可访问场景',
    desc: '返回当前开放应用可访问的场景列表。',
    note: '只返回授权范围内场景，适合作为第三方选择场景的第一步。'
  },
  {
    method: 'GET',
    methodType: 'primary',
    path: '/cost/open/scenes/{sceneId}/versions',
    title: '查询场景版本口径',
    desc: '返回默认快照模式、支持的快照模式和已发布版本。',
    note: '如果开放应用未开通草稿联调，则只建议走 ACTIVE。'
  },
  {
    method: 'GET',
    methodType: 'primary',
    path: '/cost/open/scenes/{sceneId}/fees',
    title: '查询运行费用',
    desc: '获取当前版本或快照下可执行的费用列表。',
    note: '适合第三方先拉费用清单，再决定单费用、多费用还是全费用调用。'
  },
  {
    method: 'GET',
    methodType: 'warning',
    path: '/cost/open/fee-template',
    title: '生成费用接入模板',
    desc: '返回变量中文名、来源路径、是否必填、示例值和接入建议。',
    note: '这是企业级对接的核心入口，第三方不需要再猜字段。'
  },
  {
    method: 'POST',
    methodType: 'danger',
    path: '/cost/open/fee/calculate',
    title: '执行费用核算',
    desc: '支持单对象、对象数组、单费用、多费用、全费用核算。',
    note: '如果缺少模板要求字段，会返回中文定位消息而不是单纯报错。'
  }
]

const apifoxEnvItems = [
  { name: 'baseUrl', example: 'http://localhost:8080' },
  { name: 'appCode', example: 'DEMO_OPEN_APP' },
  { name: 'appSecret', example: 'demo-open-secret' },
  { name: 'accessToken', example: '通过令牌接口写入' },
  { name: 'sceneId', example: '1' },
  { name: 'versionId', example: '2' },
  { name: 'feeId', example: '12' },
  { name: 'feeIds', example: '12,13' }
]

const errorHandlingItems = [
  '401：token 未传、无效或已过期，第三方需重新申请 accessToken。',
  '403：开放应用未开通草稿联调或未授权访问目标场景。',
  '500：输入数据缺少模板要求字段，平台会返回 variableName 和来源路径 path。',
  '所有业务错误都优先返回中文提示，便于第三方联调和业务排查。'
]

const authRequestExample = JSON.stringify({
  appCode: 'DEMO_OPEN_APP',
  appSecret: 'demo-open-secret'
}, null, 2)

const calculateRequestExample = JSON.stringify({
  sceneId: 1,
  versionId: 2,
  snapshotMode: 'ACTIVE',
  feeIds: [12, 13],
  inputJson: JSON.stringify([
    {
      bizNo: 'SIM-001',
      cover: {
        action: 'moor',
        cargoType: '块矿',
        workloadTon: 1
      },
      shift: {
        name: '白班'
      }
    }
  ])
}, null, 2)

function resetForm() {
  form.appId = undefined
  form.appCode = ''
  form.appName = ''
  form.sceneScopeType = 'ALL'
  form.sceneIds = []
  form.allowDraftSnapshot = false
  form.tokenTtlSeconds = 7200
  form.effectiveStartTime = undefined
  form.effectiveEndTime = undefined
  form.status = '0'
  form.remark = ''
  effectiveRange.value = []
  tokenPreset.value = undefined
  proxy.resetForm('formRef')
}

async function loadSceneOptions() {
  const response = await listOpenAppSceneOptions()
  sceneOptions.value = response.data || []
}

async function loadList(keepSelected = true) {
  loading.value = true
  try {
    const response = await listOpenApp(queryParams)
    openAppList.value = response.rows || []
    total.value = response.total || 0
    await syncCurrentApp(keepSelected)
  } finally {
    loading.value = false
  }
}

async function syncCurrentApp(keepSelected = true) {
  if (!openAppList.value.length) {
    currentAppId.value = undefined
    currentApp.value = undefined
    return
  }
  const targetId = keepSelected && currentAppId.value && openAppList.value.some(item => item.appId === currentAppId.value)
    ? currentAppId.value
    : openAppList.value[0].appId
  await selectApp(targetId)
  nextTick(() => {
    const row = openAppList.value.find(item => item.appId === targetId)
    if (row) {
      tableRef.value?.setCurrentRow(row)
    }
  })
}

async function selectApp(appId) {
  if (!appId) {
    currentAppId.value = undefined
    currentApp.value = undefined
    return
  }
  currentAppId.value = appId
  const response = await getOpenApp(appId)
  currentApp.value = response.data
}

function handleQuery() {
  queryParams.pageNum = 1
  loadList(false)
}

function resetQuery() {
  queryParams.pageNum = 1
  queryParams.pageSize = 10
  queryParams.searchValue = ''
  queryParams.sceneScopeType = undefined
  queryParams.allowDraftSnapshot = undefined
  queryParams.status = undefined
  loadList(false)
}

function handleSelectRow(row) {
  if (!row?.appId) {
    return
  }
  selectApp(row.appId)
}

function fillForm(app, mode) {
  resetForm()
  if (!app) {
    return
  }
  form.appId = mode === 'edit' ? app.appId : undefined
  form.appCode = mode === 'clone' ? `${app.appCode}_COPY` : app.appCode
  form.appName = mode === 'clone' ? `${app.appName}-复制` : app.appName
  form.sceneScopeType = app.sceneScopeType || 'ALL'
  form.sceneIds = [...(app.sceneIds || [])]
  form.allowDraftSnapshot = !!app.allowDraftSnapshot
  form.tokenTtlSeconds = app.tokenTtlSeconds || 7200
  form.effectiveStartTime = app.effectiveStartTime || undefined
  form.effectiveEndTime = app.effectiveEndTime || undefined
  form.status = app.status || '0'
  form.remark = app.remark || ''
  effectiveRange.value = app.effectiveStartTime || app.effectiveEndTime
    ? [app.effectiveStartTime || '', app.effectiveEndTime || '']
    : []
}

function handleOpenDialog(mode) {
  dialogMode.value = mode
  if (mode === 'edit' || mode === 'clone') {
    if (!currentApp.value) {
      proxy.$modal.msgWarning('请先从台账中选择一个开放应用')
      return
    }
    fillForm(currentApp.value, mode)
  } else {
    resetForm()
  }
  dialogOpen.value = true
}

function handleApplyTokenPreset(value) {
  if (!value) {
    return
  }
  form.tokenTtlSeconds = value
}

async function submitForm() {
  await formRef.value.validate()
  form.effectiveStartTime = effectiveRange.value?.[0] || undefined
  form.effectiveEndTime = effectiveRange.value?.[1] || undefined
  const payload = {
    ...form,
    sceneIds: form.sceneScopeType === 'LIST' ? form.sceneIds : []
  }
  if (dialogMode.value === 'edit') {
    await updateOpenApp(payload)
    proxy.$modal.msgSuccess('开放应用修改成功')
    dialogOpen.value = false
    await loadList()
    return
  }
  const response = await addOpenApp(payload)
  secretPayload.value = response.data || {}
  proxy.$modal.msgSuccess(dialogMode.value === 'clone' ? '开放应用复制成功' : '开放应用创建成功')
  dialogOpen.value = false
  secretDialogOpen.value = true
  await loadList(false)
  if (secretPayload.value?.app?.appId) {
    await selectApp(secretPayload.value.app.appId)
  }
}

async function handleResetSecret() {
  if (!currentApp.value?.appId) {
    proxy.$modal.msgWarning('请先选择需要重置密钥的开放应用')
    return
  }
  await ElMessageBox.confirm(
    `确认重置应用 ${currentApp.value.appName} 的 appSecret 吗？旧密钥会立即失效。`,
    '重置密钥确认',
    { type: 'warning' }
  )
  const response = await resetOpenAppSecret(currentApp.value.appId)
  secretPayload.value = response.data || {}
  secretDialogOpen.value = true
  proxy.$modal.msgSuccess('密钥已重置')
  await loadList()
}

async function handleDelete() {
  if (!currentApp.value?.appId) {
    proxy.$modal.msgWarning('请先选择需要删除的开放应用')
    return
  }
  await ElMessageBox.confirm(
    `确认删除开放应用 ${currentApp.value.appName} 吗？`,
    '删除确认',
    { type: 'warning' }
  )
  await delOpenApp(currentApp.value.appId)
  proxy.$modal.msgSuccess('开放应用删除成功')
  await loadList(false)
}

function resolveStatusTagType(status) {
  return status === '0' ? 'success' : 'info'
}

function resolveStatusLabel(status) {
  return status === '0' ? '正常' : '停用'
}

function resolveEffectiveState(app) {
  if (!app) {
    return { label: '-', type: 'info' }
  }
  const now = Date.now()
  const start = app.effectiveStartTime ? new Date(app.effectiveStartTime).getTime() : undefined
  const end = app.effectiveEndTime ? new Date(app.effectiveEndTime).getTime() : undefined
  if (start && start > now) {
    return { label: '未生效', type: 'warning' }
  }
  if (end && end < now) {
    return { label: '已过期', type: 'danger' }
  }
  return { label: '生效中', type: 'success' }
}

function buildEffectiveDescription(app) {
  if (!app) {
    return ''
  }
  const start = app.effectiveStartTime || '不限开始时间'
  const end = app.effectiveEndTime || '不限结束时间'
  return `当前应用的生效窗口为 ${start} 至 ${end}。若第三方申请 token 时超出此范围，平台会直接拒绝。`
}

function formatTokenTtl(value) {
  const seconds = Number(value || 0)
  if (!seconds) {
    return '-'
  }
  if (seconds % 3600 === 0) {
    return `${seconds / 3600} 小时`
  }
  if (seconds % 60 === 0) {
    return `${seconds / 60} 分钟`
  }
  return `${seconds} 秒`
}

async function copyText(text, message = '内容已复制') {
  const content = String(text || '').trim()
  if (!content) {
    proxy.$modal.msgWarning('没有可复制的内容')
    return
  }
  if (navigator?.clipboard?.writeText) {
    await navigator.clipboard.writeText(content)
  } else {
    const input = document.createElement('textarea')
    input.value = content
    document.body.appendChild(input)
    input.select()
    document.execCommand('copy')
    document.body.removeChild(input)
  }
  proxy.$modal.msgSuccess(message)
}

onMounted(async () => {
  await Promise.all([loadSceneOptions(), loadList(false)])
})
</script>

<style lang="scss" scoped>
.open-app-page {
  display: grid;
  gap: 20px;
}

.open-app-page__hero,
.open-app-page__panel,
.open-app-page__metric-card,
.open-app-page__summary-card {
  border-radius: 18px;
  background: var(--el-bg-color);
  border: 1px solid var(--el-border-color-light);
  box-shadow: 0 10px 24px rgba(15, 23, 42, 0.05);
}

.open-app-page__hero {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: flex-start;
  padding: 24px 28px;
}

.open-app-page__eyebrow {
  margin-bottom: 8px;
  color: var(--el-color-primary);
  font-size: 13px;
  font-weight: 600;
}

.open-app-page__hero h2 {
  margin: 0 0 10px;
  font-size: 30px;
}

.open-app-page__hero p,
.open-app-page__section-head p {
  margin: 0;
  color: var(--el-text-color-secondary);
  line-height: 1.75;
}

.open-app-page__hero-actions {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}

.open-app-page__metrics {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 16px;
}

.open-app-page__metric-card,
.open-app-page__summary-card {
  display: grid;
  gap: 8px;
  padding: 18px 20px;
}

.open-app-page__metric-card span,
.open-app-page__summary-card span {
  color: var(--el-text-color-secondary);
}

.open-app-page__metric-card strong,
.open-app-page__summary-card strong {
  font-size: 30px;
  line-height: 1;
  color: var(--el-color-primary);
}

.open-app-page__metric-card small,
.open-app-page__summary-card small {
  color: var(--el-text-color-secondary);
  line-height: 1.6;
}

.open-app-page__panel {
  padding: 22px 24px;
}

.open-app-page__query-form {
  margin-bottom: 8px;
}

.open-app-page__toolbar {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: center;
  margin-bottom: 18px;
}

.open-app-page__toolbar-left,
.open-app-page__detail-actions,
.open-app-page__hero-actions,
.open-app-page__secret-line {
  display: flex;
  gap: 10px;
  align-items: center;
  flex-wrap: wrap;
}

.open-app-page__workspace {
  display: grid;
  grid-template-columns: minmax(0, 1.35fr) minmax(360px, 0.95fr);
  gap: 18px;
}

.open-app-page__ledger,
.open-app-page__detail {
  min-width: 0;
}

.open-app-page__section-head {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: flex-start;
  margin-bottom: 14px;
}

.open-app-page__section-head h3 {
  margin: 0 0 6px;
  font-size: 20px;
  color: var(--el-text-color-primary);
}

.open-app-page__app-main,
.open-app-page__scope-cell {
  display: grid;
  gap: 4px;
}

.open-app-page__app-main strong {
  color: var(--el-text-color-primary);
}

.open-app-page__app-main span,
.open-app-page__scope-cell span {
  color: var(--el-text-color-secondary);
  line-height: 1.6;
}

.open-app-page__detail-summary {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
  margin-bottom: 16px;
}

.open-app-page__detail-tabs,
.open-app-page__advice-list {
  display: grid;
  gap: 14px;
}

.open-app-page__selection-tip {
  margin-bottom: 16px;
}

.open-app-page__manual-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
}

.open-app-page__manual-card {
  display: grid;
  gap: 12px;
  padding: 16px 18px;
  border-radius: 16px;
  border: 1px solid var(--el-border-color-lighter);
  background: var(--el-fill-color-blank);
}

.open-app-page__manual-card--wide {
  grid-column: 1 / -1;
}

.open-app-page__manual-head {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: center;
  flex-wrap: wrap;
}

.open-app-page__manual-head strong {
  color: var(--el-text-color-primary);
}

.open-app-page__manual-list {
  margin: 0;
  padding-left: 18px;
  color: var(--el-text-color-regular);
  line-height: 1.8;
}

.open-app-page__contract-pills {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.open-app-page__contract-pill {
  padding: 6px 12px;
  border-radius: 999px;
  border: 1px solid var(--el-border-color);
  background: var(--el-fill-color-light);
  color: var(--el-text-color-regular);
  line-height: 1.5;
}

.open-app-page__code-block {
  margin: 0;
  padding: 14px 16px;
  border-radius: 14px;
  background: var(--el-fill-color-dark);
  color: var(--el-color-white);
  font-family: 'JetBrains Mono', 'Consolas', monospace;
  font-size: 12px;
  line-height: 1.7;
  white-space: pre-wrap;
  word-break: break-word;
}

.open-app-page__step-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
}

.open-app-page__step-card {
  display: grid;
  gap: 6px;
  padding: 14px;
  border-radius: 14px;
  background: var(--el-fill-color-light);
  border: 1px solid var(--el-border-color-lighter);
}

.open-app-page__step-card span {
  color: var(--el-color-primary);
  font-size: 12px;
  font-weight: 700;
}

.open-app-page__step-card strong {
  color: var(--el-text-color-primary);
}

.open-app-page__step-card small {
  color: var(--el-text-color-secondary);
  line-height: 1.6;
}

.open-app-page__endpoint-list {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.open-app-page__endpoint-card {
  display: grid;
  gap: 8px;
  padding: 14px;
  border-radius: 14px;
  border: 1px solid var(--el-border-color-lighter);
  background: var(--el-bg-color-page);
}

.open-app-page__endpoint-card strong {
  color: var(--el-text-color-primary);
}

.open-app-page__endpoint-card span,
.open-app-page__endpoint-card small {
  color: var(--el-text-color-secondary);
  line-height: 1.6;
}

.open-app-page__endpoint-meta {
  display: flex;
  gap: 10px;
  align-items: center;
  flex-wrap: wrap;
}

.open-app-page__endpoint-meta code {
  color: var(--el-color-primary);
  word-break: break-all;
}

.open-app-page__scene-list {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
  margin-top: 14px;
}

.open-app-page__scene-tag {
  margin-right: 0;
}

.open-app-page__secret-box {
  margin-top: 16px;
}

.open-app-page__secret-line {
  justify-content: space-between;
}

@media (max-width: 1280px) {
  .open-app-page__metrics,
  .open-app-page__workspace,
  .open-app-page__detail-summary,
  .open-app-page__manual-grid,
  .open-app-page__endpoint-list,
  .open-app-page__step-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 768px) {
  .open-app-page__hero,
  .open-app-page__toolbar,
  .open-app-page__section-head {
    flex-direction: column;
  }

  .open-app-page__panel {
    padding: 18px;
  }
}
</style>

<template>
  <div class="app-container access-page" :class="{ 'is-compact-mode': isCompactMode }">
    <section v-show="!isCompactMode" class="access-page__hero">
      <div>
        <div class="access-page__eyebrow">数据接入</div>
        <h2>数据接入与导入工作台</h2>
        <p>先把业务系统宽表载荷整理成标准计费对象，再决定走单费用取价还是正式核算。</p>
      </div>
      <div class="access-page__actions">
        <el-button icon="Files" @click="router.push(COST_MENU_ROUTES.architecture)">数据架构</el-button>
        <el-button icon="Upload" @click="router.push(COST_MENU_ROUTES.taskBatch)">导入批次台账</el-button>
        <el-button type="primary" icon="Promotion" @click="router.push(COST_MENU_ROUTES.task)">正式核算入口</el-button>
      </div>
    </section>

    <section v-show="!isCompactMode" class="access-page__metrics">
      <div v-for="item in metricItems" :key="item.label" class="access-page__metric">
        <span>{{ item.label }}</span>
        <strong>{{ item.value }}</strong>
        <small>{{ item.desc }}</small>
      </div>
    </section>

    <section class="access-page__panel access-page__query-shell">
      <div class="access-page__section-head">
        <div>
          <h3>接入方案</h3>
          <p class="access-page__subtext">把当前场景、费用、版本、映射 JSON 和样例载荷沉成可复用方案，为后续数据源直连打底。</p>
        </div>
        <div class="access-page__actions">
          <el-button icon="Refresh" @click="loadProfiles()">刷新方案</el-button>
          <el-button type="primary" icon="Plus" @click="handleOpenProfileDialog('add')" v-hasPermi="['cost:access:add']">保存为方案</el-button>
          <el-button icon="Edit" :disabled="!selectedProfileId" @click="handleOpenProfileDialog('edit')" v-hasPermi="['cost:access:edit']">更新当前方案</el-button>
          <el-button type="danger" icon="Delete" :disabled="!selectedProfileId" @click="handleDeleteProfile" v-hasPermi="['cost:access:remove']">删除方案</el-button>
        </div>
      </div>
      <el-form inline label-width="88px" class="access-page__inline-form">
        <el-form-item label="当前方案">
          <el-select v-model="selectedProfileId" clearable filterable style="width: 360px" placeholder="请选择接入方案" @change="handleProfileChange">
            <el-option v-for="item in profileOptions" :key="item.profileId" :label="`${item.profileName} / ${item.profileCode}`" :value="item.profileId" />
          </el-select>
        </el-form-item>
        <el-form-item label="来源类型">
          <el-tag size="small" :type="currentProfile?.sourceType === 'HTTP_API' ? 'warning' : 'info'">{{ currentProfile?.sourceType || '未选择' }}</el-tag>
        </el-form-item>
        <el-form-item label="执行模式">
          <el-tag size="small" type="success">{{ currentProfile?.taskType || selectionForm.taskType }}</el-tag>
        </el-form-item>
        <el-form-item label="绑定版本">
          <span>{{ currentProfile?.versionNo || '按当前配置或生效版本' }}</span>
        </el-form-item>
      </el-form>
      <el-alert v-if="profileSummary" class="access-page__alert" :closable="false" :title="profileSummary" type="info" />
      <div class="access-page__summary access-page__summary--context">
        <div v-for="item in contextItems" :key="item.label" class="access-page__card access-page__card--context">
          <span>{{ item.label }}</span>
          <strong>{{ item.value }}</strong>
          <small>{{ item.desc }}</small>
        </div>
      </div>
    </section>

    <section class="access-page__panel access-page__workflow-shell">
      <div class="access-page__section-head">
        <div>
          <h3>接入工作流</h3>
          <p class="access-page__subtext">先锁定模板，再在同页继续做两类处理：一类是直接验证标准计费对象，一类是把原始报文整理为标准对象并沉淀导入批次。</p>
        </div>
      </div>
      <div class="access-page__stage-strip">
        <button
          v-for="stage in workflowStages"
          :key="stage.key"
          type="button"
          class="access-page__stage-chip"
          :class="[`is-${stage.status}`, { 'is-active': activeStageKey === stage.key }]"
          @click="scrollToStage(stage.key)"
        >
          <span class="access-page__stage-order">{{ stage.order }}</span>
          <span class="access-page__stage-main">
            <strong>{{ stage.label }}</strong>
            <small>{{ stage.desc }}</small>
          </span>
        </button>
      </div>

      <div ref="templateStageRef" class="access-page__stage-panel">
        <div class="access-page__pane-head">
          <div>
            <div class="access-page__pane-kicker">阶段 1</div>
            <h4>费用模板准备</h4>
            <p>选择场景、版本、目标费用和任务类型，先把当前费用主线需要的字段模板拉平出来。</p>
          </div>
          <div class="access-page__actions">
            <el-button type="primary" icon="RefreshLeft" @click="handleBuildTemplate" v-hasPermi="['cost:simulation:list', 'cost:task:list']">生成费用模板</el-button>
            <el-button :disabled="!templateData.fee?.feeCode" @click="scrollToStage('fee')">继续验证取价</el-button>
            <el-button :disabled="!templateData.fee?.feeCode" @click="scrollToStage('preview')">继续预演对象</el-button>
            <el-button icon="Edit" @click="router.push({ path: COST_MENU_ROUTES.simulation, query: { sceneId: selectionForm.sceneId, versionId: selectionForm.versionId, billMonth: calcForm.billMonth } })">打开试算中心</el-button>
          </div>
        </div>
        <el-form :model="selectionForm" label-width="96px" class="access-page__form-shell">
          <el-form-item label="所属场景" required>
            <el-select v-model="selectionForm.sceneId" clearable filterable style="width: 100%" placeholder="请选择场景" @change="handleSceneChange">
              <el-option v-for="item in sceneOptions" :key="item.sceneId" :label="`${item.sceneName} / ${item.sceneCode}`" :value="item.sceneId" />
            </el-select>
          </el-form-item>
          <el-form-item label="执行版本">
            <el-select v-model="selectionForm.versionId" clearable filterable style="width: 100%" placeholder="不选则按当前配置或生效版本">
              <el-option v-for="item in versionOptions" :key="item.versionId" :label="`${item.versionNo} / ${item.versionStatusName || item.versionStatus}`" :value="item.versionId" />
            </el-select>
          </el-form-item>
          <el-form-item label="目标费用" required>
            <el-select v-model="selectionForm.feeId" clearable filterable style="width: 100%" placeholder="请选择费用" @change="handleFeeChange">
              <el-option v-for="item in feeOptions" :key="item.feeId" :label="`${item.feeName} / ${item.feeCode}`" :value="item.feeId" />
            </el-select>
          </el-form-item>
          <el-form-item label="任务类型">
            <el-radio-group v-model="selectionForm.taskType">
              <el-radio-button label="SIMULATION">试算</el-radio-button>
              <el-radio-button label="FORMAL_SINGLE">正式单笔</el-radio-button>
              <el-radio-button label="FORMAL_BATCH">正式批量</el-radio-button>
            </el-radio-group>
          </el-form-item>
        </el-form>
        <el-alert v-if="templateData.message" class="access-page__alert" :closable="false" :title="templateData.message" type="info" />
        <el-alert
          v-if="templateData.fee?.feeCode"
          class="access-page__alert"
          :closable="false"
          title="模板已准备完成，下方可以直接继续做单费用验证，或继续把原始报文整理为标准计费对象。"
          type="success"
        />
        <div v-if="templateData.fee?.feeCode" class="access-page__summary">
          <div class="access-page__card"><span>目标费用</span><strong>{{ templateData.fee.feeCode }}</strong><small>{{ templateData.fee.feeName }}</small></div>
          <div class="access-page__card"><span>对象维度</span><strong>{{ templateObjectDimension || '-' }}</strong><small>标准计费对象默认继承该维度</small></div>
          <div class="access-page__card"><span>输入字段数</span><strong>{{ templateData.inputFieldCount || 0 }}</strong><small>模板要求准备的字段数</small></div>
          <div class="access-page__card"><span>执行费用链</span><strong>{{ templateData.executionFeeCount || 0 }}</strong><small>{{ templateData.snapshotSource || '当前配置' }}</small></div>
        </div>
        <el-table v-if="templateFields.length" :data="templateFields" size="small" border class="access-page__table">
          <el-table-column label="模板路径" prop="path" min-width="200" />
          <el-table-column label="变量" min-width="180">
            <template #default="scope">{{ scope.row.variableName }} ({{ scope.row.variableCode }})</template>
          </el-table-column>
          <el-table-column label="来源" prop="sourceType" width="100" />
          <el-table-column label="纳入模板" width="96" align="center">
            <template #default="scope">
              <el-tag :type="scope.row.includedInTemplate ? 'success' : 'info'" size="small">{{ scope.row.includedInTemplate ? '是' : '否' }}</el-tag>
            </template>
          </el-table-column>
        </el-table>
      </div>

      <div ref="feeStageRef" class="access-page__stage-panel">
        <div class="access-page__pane-head">
          <div>
            <div class="access-page__pane-kicker">阶段 2A</div>
            <h4>单费用同步取价</h4>
            <p>如果你手里已经有标准计费对象，可以直接在这里做小样验证，确认规则命中、金额返回和解释信息。</p>
          </div>
          <div class="access-page__actions">
            <el-button type="primary" icon="Promotion" @click="handleCalculateFee" v-hasPermi="['cost:simulation:execute']">执行单费用取价</el-button>
            <el-button @click="scrollToStage('preview')">去对象预演区</el-button>
          </div>
        </div>
        <el-form :model="calcForm" label-width="96px" class="access-page__form-shell">
          <el-form-item label="账期">
            <el-date-picker
              v-model="calcForm.billMonth"
              type="month"
              format="YYYY-MM"
              value-format="YYYY-MM"
              placeholder="选择账期"
              style="width: 100%"
            />
          </el-form-item>
          <el-form-item label="解释信息">
            <el-switch v-model="calcForm.includeExplain" />
          </el-form-item>
          <el-form-item label="输入 JSON" required>
            <JsonEditor v-model="calcForm.inputJson" title="输入 JSON" :rows="10" :max-length="30000" :allow-empty="false" placeholder="请输入标准计费对象数组，建议至少包含 objectDimension、objectCode、objectName。" />
          </el-form-item>
        </el-form>
        <el-alert v-if="feeResult.recordCount" class="access-page__alert" :closable="false" :title="feeResultSummary" :type="feeResult.failedCount ? 'warning' : 'success'" />
        <el-table v-if="feeRecords.length" :data="feeRecords" size="small" border class="access-page__table">
          <el-table-column label="计费对象" min-width="220">
            <template #default="scope">{{ scope.row.objectName || '-' }} / {{ scope.row.objectCode || '-' }}</template>
          </el-table-column>
          <el-table-column label="对象维度" min-width="140">
            <template #default="scope">{{ scope.row.objectDimension || templateObjectDimension || '-' }}</template>
          </el-table-column>
          <el-table-column label="状态" width="100">
            <template #default="scope"><el-tag :type="resolveRecordTagType(scope.row.status)" size="small">{{ scope.row.status }}</el-tag></template>
          </el-table-column>
          <el-table-column label="命中规则" min-width="180">
            <template #default="scope">{{ scope.row.ruleCode || '-' }}</template>
          </el-table-column>
          <el-table-column label="金额" prop="amountValue" min-width="120" />
        </el-table>
        <el-tabs v-if="feeResult.recordCount" class="access-page__tabs">
          <el-tab-pane label="完整结果"><JsonEditor :model-value="feeResult" title="完整结果" readonly :rows="10" /></el-tab-pane>
          <el-tab-pane label="模板示例"><JsonEditor :model-value="formattedTemplateJson" title="模板示例" readonly :rows="10" /></el-tab-pane>
        </el-tabs>
      </div>

      <div ref="previewStageRef" class="access-page__stage-panel">
        <div class="access-page__pane-head">
          <div>
            <div class="access-page__pane-kicker">阶段 2B</div>
            <h4>标准计费对象预演</h4>
            <p>如果手里还是宽表报文或外部接口返回值，就在这里先把它整理成标准计费对象，再回填验证或直接生成导入批次。</p>
          </div>
          <div class="access-page__actions">
            <el-button v-if="currentProfile?.sourceType === 'HTTP_API'" type="warning" icon="Connection" :disabled="!selectedProfileId" @click="handlePreviewBuildByProfile" v-hasPermi="['cost:access:query']">按方案拉取并预演</el-button>
            <el-button v-if="currentProfile?.sourceType === 'HTTP_API'" type="success" icon="Upload" :disabled="!selectedProfileId" @click="handleCreateBatchByProfile" v-hasPermi="['cost:task:execute']">按方案拉取并生成批次</el-button>
            <el-button type="primary" icon="DataAnalysis" @click="handlePreviewBuild" v-hasPermi="['cost:simulation:list', 'cost:task:list']">预演计费对象</el-button>
            <el-button icon="DocumentCopy" :disabled="!buildPreviewRecords.length" @click="handleUseBuildPreview">覆盖取价输入</el-button>
            <el-button type="success" icon="Upload" :disabled="!buildPreviewRecords.length" @click="handleCreateBatchFromPreview" v-hasPermi="['cost:task:execute']">生成导入批次</el-button>
          </div>
        </div>
        <div class="access-page__editor-stack">
          <JsonEditor v-if="currentProfile?.sourceType === 'HTTP_API'" v-model="builderForm.requestPayloadJson" title="请求载荷 JSON" :rows="7" :max-length="30000" placeholder="用于调用业务 HTTP 接口的请求载荷；留空则回退为方案内保存的样例请求。" />
          <JsonEditor v-model="builderForm.rawJson" title="原始 JSON" :rows="9" :max-length="30000" :allow-empty="false" placeholder="支持 JSON 对象或对象数组。" />
          <JsonEditor v-model="builderForm.mappingJson" title="字段映射 JSON" :rows="7" :max-length="20000" placeholder='可选，例如 {"bizNo":"source.bizNo","objectDimension":"source.teamType","objectCode":"source.teamCode","objectName":"source.teamName","attendance.female.headcount":"source.attendance.female.headcount","oddWork.quantity":"source.odd.hours"}。' />
        </div>
        <el-alert v-if="buildPreview.message" class="access-page__alert" :closable="false" :title="buildPreview.message" type="success" />
        <el-alert v-if="buildPreview.fetchMeta" class="access-page__alert" :closable="false" :title="buildPreviewFetchTitle" :description="buildPreviewFetchDescription" type="warning" />
        <el-alert v-if="buildPreviewLoadingGuide.title" class="access-page__alert" :closable="false" :title="buildPreviewLoadingGuide.title" :description="buildPreviewLoadingGuide.description" :type="buildPreviewLoadingGuide.type || 'info'" />
        <div v-if="createdBatch.batch?.batchNo" class="access-page__summary">
          <div class="access-page__card"><span>批次号</span><strong>{{ createdBatch.batch.batchNo }}</strong><small>{{ createdBatch.batch.billMonth || '-' }}</small></div>
          <div class="access-page__card"><span>标准对象数</span><strong>{{ createdBatch.itemTotal || createdBatch.batch.totalCount || 0 }}</strong><small>当前批次对象总数</small></div>
          <div class="access-page__card"><span>预计分片数</span><strong>{{ createdBatch.loadingGuide?.estimatedPartitionCount || 0 }}</strong><small>按正式核算默认口径估算</small></div>
          <div v-if="createdBatch.resumable" class="access-page__card"><span>继续装载</span><strong>{{ createdBatchCheckpoint.nextPageNo || createdBatchCheckpoint.nextCursor || '待继续' }}</strong><small>{{ createdBatchResumeTip }}</small></div>
        </div>
        <div v-if="createdBatch.batch?.batchNo" class="access-page__actions">
          <el-button v-if="createdBatch.resumable" type="warning" icon="RefreshRight" :disabled="!selectedProfileId" @click="handleResumeBatchByProfile" v-hasPermi="['cost:task:execute']">继续装载剩余分页</el-button>
          <el-button type="primary" icon="Promotion" :disabled="createdBatch.resumable" @click="openCreatedBatchTaskCenter">用该批次发起正式核算</el-button>
          <el-button icon="Tickets" @click="openCreatedBatchLedger">查看导入批次台账</el-button>
        </div>
        <div v-if="buildPreview.mappedRecordCount" class="access-page__summary">
          <div class="access-page__card"><span>原始记录数</span><strong>{{ buildPreview.rawRecordCount || 0 }}</strong><small>参与预演的宽表记录数</small></div>
          <div class="access-page__card"><span>标准对象数</span><strong>{{ buildPreview.mappedRecordCount || 0 }}</strong><small>转换后的标准计费对象数</small></div>
          <div class="access-page__card"><span>对象维度</span><strong>{{ buildPreviewObjectDimensions.length || 0 }}</strong><small>{{ buildPreviewObjectDimensions.join(' / ') || templateObjectDimension || '待补映射' }}</small></div>
          <div class="access-page__card"><span>未命中路径</span><strong>{{ buildPreview.missingPaths?.length || 0 }}</strong><small>仍需补映射的模板路径数</small></div>
          <div class="access-page__card"><span>预计分片数</span><strong>{{ buildPreviewLoadingGuide.estimatedPartitionCount || 0 }}</strong><small>按正式核算默认口径估算</small></div>
        </div>
        <div v-if="buildPreviewObjectDimensions.length" class="access-page__tags">
          <span>识别维度：</span>
          <el-tag v-for="dimension in buildPreviewObjectDimensions" :key="dimension" size="small">{{ dimension }}</el-tag>
        </div>
        <div v-if="buildPreview.missingPaths?.length" class="access-page__tags">
          <span>未命中路径：</span>
          <el-tag v-for="path in buildPreview.missingPaths" :key="path" size="small" type="warning">{{ path }}</el-tag>
        </div>
        <el-table v-if="buildPreviewMappings.length" :data="buildPreviewMappings" size="small" border class="access-page__table">
          <el-table-column label="模板路径" prop="path" min-width="220" />
          <el-table-column label="变量" min-width="180">
            <template #default="scope">{{ scope.row.variableName }} ({{ scope.row.variableCode }})</template>
          </el-table-column>
          <el-table-column label="映射说明" prop="mappingHint" min-width="180" />
          <el-table-column label="纳入模板" width="96" align="center">
            <template #default="scope">
              <el-tag :type="scope.row.includedInTemplate ? 'success' : 'info'" size="small">{{ scope.row.includedInTemplate ? '是' : '否' }}</el-tag>
            </template>
          </el-table-column>
        </el-table>
        <el-tabs v-if="buildPreviewRecords.length || buildPreview.fetchedPayloadJson" class="access-page__tabs">
          <el-tab-pane label="标准计费对象"><JsonEditor :model-value="buildPreviewRecords" title="标准计费对象" readonly :rows="10" /></el-tab-pane>
          <el-tab-pane v-if="buildPreview.fetchedPayloadJson" label="接口返回原始报文"><JsonEditor :model-value="buildPreview.fetchedPayloadJson" title="接口返回原始报文" readonly :rows="10" /></el-tab-pane>
          <el-tab-pane label="完整预演结果"><JsonEditor :model-value="formattedBuildPreviewJson" title="完整预演结果" readonly :rows="10" /></el-tab-pane>
        </el-tabs>
      </div>
    </section>

    <el-dialog v-model="profileDialogVisible" :title="profileDialogTitle" width="720px">
      <el-form :model="profileForm" label-width="100px">
        <el-form-item label="方案编码" required>
          <el-input v-model="profileForm.profileCode" maxlength="64" placeholder="请输入方案编码" />
        </el-form-item>
        <el-form-item label="方案名称" required>
          <el-input v-model="profileForm.profileName" maxlength="128" placeholder="请输入方案名称" />
        </el-form-item>
        <el-form-item label="来源类型">
          <el-select v-model="profileForm.sourceType" style="width: 100%">
            <el-option label="原始 JSON / 宽表报文" value="RAW_JSON" />
            <el-option label="外部 HTTP 接口" value="HTTP_API" />
          </el-select>
        </el-form-item>
        <el-form-item label="任务类型">
          <el-radio-group v-model="profileForm.taskType">
            <el-radio-button label="SIMULATION">试算</el-radio-button>
            <el-radio-button label="FORMAL_SINGLE">正式单笔</el-radio-button>
            <el-radio-button label="FORMAL_BATCH">正式批量</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="绑定版本">
          <el-select v-model="profileForm.versionId" clearable filterable style="width: 100%" placeholder="不绑定则按当前配置或生效版本">
            <el-option v-for="item in versionOptions" :key="item.versionId" :label="`${item.versionNo} / ${item.versionStatusName || item.versionStatus}`" :value="item.versionId" />
          </el-select>
        </el-form-item>
        <el-form-item label="请求方法">
          <el-select v-model="profileForm.requestMethod" style="width: 100%">
            <el-option label="GET" value="GET" />
            <el-option label="POST" value="POST" />
          </el-select>
        </el-form-item>
        <el-form-item label="接口地址">
          <el-input v-model="profileForm.endpointUrl" maxlength="255" placeholder="当前阶段可先留空，后续用于外部接口直连" />
        </el-form-item>
        <el-form-item label="鉴权方式">
          <el-select v-model="profileForm.authType" style="width: 100%">
            <el-option label="NONE" value="NONE" />
            <el-option label="BASIC" value="BASIC" />
            <el-option label="BEARER" value="BEARER" />
          </el-select>
        </el-form-item>
        <el-form-item v-if="profileForm.sourceType === 'HTTP_API'" label="鉴权配置 JSON">
          <JsonEditor v-model="profileForm.authConfigJson" title="鉴权配置 JSON" :rows="5" :max-length="4000" placeholder='例如 {"token":"xxx"}、{"username":"u","password":"p"}，也支持 {"headers":{"X-App":"cost"}}。' />
        </el-form-item>
        <el-form-item v-if="profileForm.sourceType === 'HTTP_API'" label="拉取策略 JSON">
          <JsonEditor v-model="profileForm.fetchConfigJson" title="拉取策略 JSON" :rows="6" :max-length="8000" placeholder='例如 {"recordsPath":"data.records","paging":{"mode":"PAGE_NO","pageField":"pageNo","pageSizeField":"pageSize","pageSize":500,"startPage":1,"maxPages":200,"hasMorePath":"data.hasMore"}}。' />
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="profileForm.status">
            <el-radio label="0">正常</el-radio>
            <el-radio label="1">停用</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="profileForm.remark" type="textarea" :rows="3" maxlength="500" show-word-limit placeholder="请输入接入方案说明" />
        </el-form-item>
      </el-form>
      <el-alert :closable="false" type="info" title="保存时会一并固化当前页面上的映射 JSON、请求样例或原始报文样例，以及标准计费对象样例。" />
      <template #footer>
        <div class="access-page__actions">
          <el-button @click="profileDialogVisible = false">取消</el-button>
          <el-button type="primary" @click="handleSubmitProfile">保存方案</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup name="CostDataAccess">
import JsonEditor from '@/components/cost/JsonEditor.vue'
import { addAccessProfile, createInputBatchByAccessProfile, getAccessProfile, optionselectAccessProfile, previewAccessProfileFetch, removeAccessProfile, updateAccessProfile } from '@/api/cost/access'
import { optionselectFee } from '@/api/cost/fee'
import { calculateFee, createTaskInputBatch, getFeeRunInputTemplate, listVersionOptions, previewBuiltInput } from '@/api/cost/run'
import { optionselectScene } from '@/api/cost/scene'
import useSettingsStore from '@/store/modules/settings'
import { resolveWorkingCostSceneId } from '@/utils/costSceneContext'
import { COST_MENU_ROUTES } from '@/utils/costMenuRoutes'
import { clearCostWorkContext, resolveWorkingBillMonth, resolveWorkingVersionId, syncCostWorkContext } from '@/utils/costWorkContext'
import { safeFormatJson } from '@/utils/jsonTools'

const route = useRoute()
const router = useRouter()
const { proxy } = getCurrentInstance()
const settingsStore = useSettingsStore()
const isCompactMode = computed(() => settingsStore.costPageMode === 'COMPACT')

const sceneOptions = ref([])
const versionOptions = ref([])
const feeOptions = ref([])
const profileOptions = ref([])
const templateData = ref({})
const feeResult = ref({})
const buildPreview = ref({})
const createdBatch = ref({})
const autoMappingJson = ref('')
const selectedProfileId = ref(undefined)
const profileDialogVisible = ref(false)
const profileDialogMode = ref('add')
const activeStageKey = ref('template')
const templateStageRef = ref()
const feeStageRef = ref()
const previewStageRef = ref()

const selectionForm = reactive({
  sceneId: route.query.sceneId ? Number(route.query.sceneId) : undefined,
  versionId: resolveWorkingVersionId(route.query.versionId ? Number(route.query.versionId) : undefined),
  feeId: route.query.feeId ? Number(route.query.feeId) : undefined,
  feeCode: route.query.feeCode || '',
  taskType: route.query.taskType || 'FORMAL_BATCH'
})

const calcForm = reactive({
  billMonth: resolveWorkingBillMonth(route.query.billMonth),
  includeExplain: true,
  inputJson: ''
})

const builderForm = reactive({
  requestPayloadJson: '',
  rawJson: '',
  mappingJson: ''
})

const profileForm = reactive({
  profileId: undefined,
  profileCode: '',
  profileName: '',
  sourceType: 'RAW_JSON',
  taskType: 'FORMAL_BATCH',
  versionId: undefined,
  requestMethod: 'GET',
  endpointUrl: '',
  authType: 'NONE',
  authConfigJson: '',
  fetchConfigJson: '',
  status: '0',
  remark: ''
})

const templateFields = computed(() => templateData.value.fields || [])
const feeRecords = computed(() => feeResult.value.records || [])
const buildPreviewRecords = computed(() => buildPreview.value.mappedRecords || [])
const buildPreviewMappings = computed(() => buildPreview.value.fieldMappings || [])
const buildPreviewLoadingGuide = computed(() => buildPreview.value.loadingGuide || {})
const templateObjectDimension = computed(() => templateData.value?.fee?.objectDimension || '')
const buildPreviewObjectDimensions = computed(() => {
  const values = (buildPreviewRecords.value || [])
    .map(item => item?.objectDimension)
    .filter(item => item)
  return Array.from(new Set(values))
})
const createdBatchCheckpoint = computed(() => createdBatch.value.checkpoint || {})
const createdBatchResumeTip = computed(() => {
  if (!createdBatch.value?.resumable) return ''
  const checkpoint = createdBatchCheckpoint.value
  if (checkpoint.nextCursor) {
    return `当前批次仍有后续游标待继续拉取，下一游标：${checkpoint.nextCursor}`
  }
  if (checkpoint.nextPageNo) {
    return `当前批次仍有后续分页待继续拉取，建议从第 ${checkpoint.nextPageNo} 页继续装载`
  }
  return '当前批次仍有后续分页待继续拉取，可直接继续装载'
})
const formattedTemplateJson = computed(() => formatJson(templateData.value.inputJson))
const formattedBuildPreviewJson = computed(() => formatJson(buildPreview.value))
const currentProfile = computed(() => profileOptions.value.find(item => item.profileId === selectedProfileId.value))
const currentScene = computed(() => sceneOptions.value.find(item => item.sceneId === selectionForm.sceneId))
const currentFee = computed(() => {
  if (!selectionForm.feeId && !selectionForm.feeCode) return undefined
  return feeOptions.value.find(item => item.feeId === selectionForm.feeId || item.feeCode === selectionForm.feeCode)
})
const currentVersion = computed(() => versionOptions.value.find(item => item.versionId === selectionForm.versionId))
const profileDialogTitle = computed(() => profileDialogMode.value === 'edit' ? '更新接入方案' : '保存接入方案')
const buildPreviewFetchTitle = computed(() => {
  const fetchMeta = buildPreview.value.fetchMeta
  if (!fetchMeta) return ''
  return `已按 ${fetchMeta.requestMethod || 'GET'} 拉取 ${fetchMeta.endpointUrl || ''} 并完成标准计费对象预演`
})
const buildPreviewFetchDescription = computed(() => {
  const fetchMeta = buildPreview.value.fetchMeta
  if (!fetchMeta) return ''
  const parts = []
  if (fetchMeta.statusCode) parts.push(`响应状态 ${fetchMeta.statusCode}`)
  if (fetchMeta.responseSize >= 0) parts.push(`响应长度 ${fetchMeta.responseSize}`)
  if (fetchMeta.resolvedUrl) parts.push(`实际请求 ${fetchMeta.resolvedUrl}`)
  return parts.join('；')
})
const metricItems = computed(() => [
  { label: '可选场景', value: sceneOptions.value.length, desc: '当前可用于数据接入的场景数量' },
  { label: '费用数量', value: feeOptions.value.length, desc: '当前选中场景下可接入的费用数量' },
  { label: '对象维度', value: templateObjectDimension.value || '-', desc: '当前模板默认继承的计费对象维度' },
  { label: '模板字段', value: templateData.value.inputFieldCount || 0, desc: '当前费用模板要求准备的字段数' },
  { label: '本次记录', value: feeResult.value.recordCount || 0, desc: '最近一次单费用取价返回的记录数' }
])
const contextItems = computed(() => [
  {
    label: '当前场景',
    value: currentScene.value?.sceneName || '未选择场景',
    desc: currentScene.value?.sceneCode || '请选择需要接入的业务场景'
  },
  {
    label: '目标费用',
    value: currentFee.value?.feeName || selectionForm.feeCode || '未选择费用',
    desc: currentFee.value?.feeCode || '生成模板前需要先定位费用主线'
  },
  {
    label: '执行版本',
    value: currentVersion.value?.versionNo || '按当前配置或生效版本',
    desc: currentVersion.value?.versionStatusName || currentVersion.value?.versionStatus || '未强制绑定版本'
  },
  {
    label: '当前方案',
    value: currentProfile.value?.profileName || '未选择复用方案',
    desc: currentProfile.value?.profileCode || '可直接把当前映射和样例沉淀为方案'
  }
])
const workflowStages = computed(() => [
  {
    key: 'template',
    order: '01',
    label: '模板准备',
    desc: templateData.value.fee?.feeCode
      ? `${templateData.value.inputFieldCount || 0} 个模板字段已就绪`
      : '先锁定场景、费用与任务类型',
    status: templateData.value.fee?.feeCode ? 'done' : selectionForm.sceneId && selectionForm.feeId ? 'ready' : 'pending'
  },
  {
    key: 'fee',
    order: '02A',
    label: '单费用验证',
    desc: feeResult.value.recordCount
      ? `最近一次回算 ${feeResult.value.recordCount} 条`
      : calcForm.inputJson?.trim()
        ? '可直接用标准对象验证计价'
        : '等待模板示例或预演结果回填',
    status: feeResult.value.recordCount ? 'done' : calcForm.inputJson?.trim() ? 'ready' : 'pending'
  },
  {
    key: 'preview',
    order: '02B',
    label: '对象预演与入批',
    desc: createdBatch.value?.batch?.batchNo
      ? `已生成批次 ${createdBatch.value.batch.batchNo}`
      : buildPreview.value.mappedRecordCount
        ? `已预演 ${buildPreview.value.mappedRecordCount} 个标准对象`
        : currentProfile.value?.sourceType === 'HTTP_API'
          ? '支持按方案直连接口拉取'
          : '支持原始报文整理与导入批次生成',
    status: createdBatch.value?.batch?.batchNo || buildPreview.value.mappedRecordCount
      ? 'done'
      : builderForm.rawJson?.trim() || currentProfile.value?.sourceType === 'HTTP_API'
        ? 'ready'
        : 'pending'
  }
])
const profileSummary = computed(() => {
  if (!currentProfile.value) return ''
  const sourceLabel = currentProfile.value.sourceType === 'HTTP_API' ? '外部 HTTP 接口' : '原始 JSON / 宽表报文'
  const targetFee = currentProfile.value.feeName ? `${currentProfile.value.feeName} / ${currentProfile.value.feeCode}` : '场景级方案'
  return `当前方案 ${currentProfile.value.profileName}（${currentProfile.value.profileCode}），面向 ${targetFee}，来源为 ${sourceLabel}。保存时会同步固化当前映射和样例载荷。`
})
const feeResultSummary = computed(() => {
  if (!feeResult.value.recordCount) return ''
  return `本次回算 ${feeResult.value.recordCount} 条，成功 ${feeResult.value.successCount || 0} 条，未命中 ${feeResult.value.noMatchCount || 0} 条，失败 ${feeResult.value.failedCount || 0} 条。`
})

function scrollToStage(stageKey) {
  activeStageKey.value = stageKey
  nextTick(() => {
    const stageMap = {
      template: templateStageRef.value,
      fee: feeStageRef.value,
      preview: previewStageRef.value
    }
    stageMap[stageKey]?.scrollIntoView({ behavior: 'smooth', block: 'start' })
  })
}

async function loadScenes() {
  const response = await optionselectScene({ status: '0', pageNum: 1, pageSize: 1000 })
  sceneOptions.value = response?.data || []
  selectionForm.sceneId = resolveWorkingCostSceneId(sceneOptions.value, selectionForm.sceneId)
}

async function loadVersions(sceneId) {
  if (!sceneId) {
    versionOptions.value = []
    selectionForm.versionId = undefined
    return
  }
  const response = await listVersionOptions(sceneId)
  versionOptions.value = response?.data || []
  if (!versionOptions.value.find(item => item.versionId === selectionForm.versionId)) {
    selectionForm.versionId = undefined
  }
}

async function loadFees(sceneId) {
  if (!sceneId) {
    feeOptions.value = []
    selectionForm.feeId = undefined
    selectionForm.feeCode = ''
    return
  }
  const response = await optionselectFee({ sceneId, status: '0', pageNum: 1, pageSize: 1000 })
  feeOptions.value = response?.data || []
  const current = feeOptions.value.find(item => item.feeId === selectionForm.feeId)
  if (current) {
    selectionForm.feeCode = current.feeCode
    return
  }
  const byCode = feeOptions.value.find(item => item.feeCode === selectionForm.feeCode)
  if (byCode) {
    selectionForm.feeId = byCode.feeId
    selectionForm.feeCode = byCode.feeCode
    return
  }
  selectionForm.feeId = undefined
  selectionForm.feeCode = ''
}

async function loadProfiles() {
  if (!selectionForm.sceneId || !selectionForm.feeId) {
    profileOptions.value = []
    selectedProfileId.value = undefined
    return
  }
  const response = await optionselectAccessProfile({
    sceneId: selectionForm.sceneId,
    feeId: selectionForm.feeId,
    status: '0'
  })
  profileOptions.value = response?.data || []
  if (!profileOptions.value.find(item => item.profileId === selectedProfileId.value)) {
    selectedProfileId.value = undefined
  }
}

function resetFeeScopedState(options = {}) {
  const { resetAutoMapping = false } = options
  templateData.value = {}
  feeResult.value = {}
  buildPreview.value = {}
  createdBatch.value = {}
  if (resetAutoMapping && builderForm.mappingJson && builderForm.mappingJson === autoMappingJson.value) {
    builderForm.mappingJson = ''
  }
  if (resetAutoMapping) {
    autoMappingJson.value = ''
  }
}

async function handleSceneChange(sceneId) {
  resetFeeScopedState({ resetAutoMapping: true })
  profileOptions.value = []
  selectedProfileId.value = undefined
  clearCostWorkContext(['versionId'])
  syncCostWorkContext({ sceneId, billMonth: calcForm.billMonth })
  await Promise.all([loadVersions(sceneId), loadFees(sceneId)])
  await loadProfiles()
}

async function handleFeeChange(feeId) {
  const current = feeOptions.value.find(item => item.feeId === feeId)
  selectionForm.feeCode = current?.feeCode || ''
  resetFeeScopedState({ resetAutoMapping: true })
  await loadProfiles()
}

async function handleBuildTemplate() {
  activeStageKey.value = 'template'
  if (!selectionForm.sceneId || (!selectionForm.feeId && !selectionForm.feeCode)) {
    proxy.$modal.msgWarning('请先选择场景和目标费用')
    return
  }
  const response = await getFeeRunInputTemplate({
    sceneId: selectionForm.sceneId,
    versionId: selectionForm.versionId,
    feeId: selectionForm.feeId,
    feeCode: selectionForm.feeCode,
    taskType: selectionForm.taskType
  })
  templateData.value = response.data || {}
  buildPreview.value = {}
  createdBatch.value = {}
  if (templateData.value.inputJson) {
    calcForm.inputJson = formatJson(templateData.value.inputJson)
  }
  if (templateData.value.fields?.length) {
    const draft = formatJson(createMappingDraft(templateData.value.fields))
    if (!builderForm.mappingJson || builderForm.mappingJson === autoMappingJson.value) {
      builderForm.mappingJson = draft
    }
    autoMappingJson.value = draft
  } else {
    autoMappingJson.value = ''
  }
}

async function handleCalculateFee() {
  activeStageKey.value = 'fee'
  if (!selectionForm.sceneId || (!selectionForm.feeId && !selectionForm.feeCode)) {
    proxy.$modal.msgWarning('请先选择场景和目标费用')
    return
  }
  if (!calcForm.inputJson || !calcForm.inputJson.trim()) {
    proxy.$modal.msgWarning('请填写输入 JSON')
    return
  }
  const response = await calculateFee({
    sceneId: selectionForm.sceneId,
    versionId: selectionForm.versionId,
    feeId: selectionForm.feeId,
    feeCode: selectionForm.feeCode,
    billMonth: calcForm.billMonth,
    includeExplain: calcForm.includeExplain,
    inputJson: calcForm.inputJson
  })
  feeResult.value = response.data || {}
}

async function handlePreviewBuild() {
  activeStageKey.value = 'preview'
  if (!selectionForm.sceneId || (!selectionForm.feeId && !selectionForm.feeCode)) {
    proxy.$modal.msgWarning('请先选择场景和目标费用')
    return
  }
  if (!builderForm.rawJson || !builderForm.rawJson.trim()) {
    proxy.$modal.msgWarning('请先填写原始 JSON')
    return
  }
  const response = await previewBuiltInput({
    sceneId: selectionForm.sceneId,
    versionId: selectionForm.versionId,
    feeId: selectionForm.feeId,
    feeCode: selectionForm.feeCode,
    taskType: selectionForm.taskType,
    rawJson: builderForm.rawJson,
    mappingJson: builderForm.mappingJson
  })
  buildPreview.value = response.data || {}
  createdBatch.value = {}
}

function handleUseBuildPreview() {
  if (!buildPreviewRecords.value.length) {
    proxy.$modal.msgWarning('当前没有可回填的标准计费对象')
    return
  }
  calcForm.inputJson = formatJson(buildPreviewRecords.value)
  scrollToStage('fee')
  proxy.$modal.msgSuccess('已用预演结果覆盖单费用取价输入')
}

async function handleCreateBatchFromPreview() {
  activeStageKey.value = 'preview'
  if (!selectionForm.sceneId) {
    proxy.$modal.msgWarning('请先选择场景')
    return
  }
  if (!calcForm.billMonth || !calcForm.billMonth.trim()) {
    proxy.$modal.msgWarning('请先填写账期')
    return
  }
  if (!buildPreviewRecords.value.length) {
    proxy.$modal.msgWarning('当前没有可生成批次的标准计费对象')
    return
  }
  const response = await createTaskInputBatch({
    sceneId: selectionForm.sceneId,
    versionId: selectionForm.versionId,
    billMonth: calcForm.billMonth,
    inputJson: JSON.stringify(buildPreviewRecords.value),
    remark: `${selectionForm.feeCode || 'FEE'} 标准计费对象预演批次`
  })
  createdBatch.value = response?.data || {}
  const batchNo = createdBatch.value?.batch?.batchNo
  proxy.$modal.msgSuccess(batchNo ? `已生成导入批次 ${batchNo}` : '已生成导入批次')
}

function resetProfileForm() {
  profileForm.profileId = undefined
  profileForm.profileCode = ''
  profileForm.profileName = ''
  profileForm.sourceType = 'RAW_JSON'
  profileForm.taskType = selectionForm.taskType || 'FORMAL_BATCH'
  profileForm.versionId = selectionForm.versionId
  profileForm.requestMethod = 'GET'
  profileForm.endpointUrl = ''
  profileForm.authType = 'NONE'
  profileForm.authConfigJson = ''
  profileForm.fetchConfigJson = ''
  profileForm.status = '0'
  profileForm.remark = ''
}

async function handleOpenProfileDialog(mode) {
  if (!selectionForm.sceneId || !selectionForm.feeId) {
    proxy.$modal.msgWarning('请先选择场景和目标费用')
    return
  }
  profileDialogMode.value = mode
  resetProfileForm()
  if (mode === 'edit') {
    if (!selectedProfileId.value) {
      proxy.$modal.msgWarning('请先选择接入方案')
      return
    }
    const response = await getAccessProfile(selectedProfileId.value)
    const detail = response?.data || {}
    profileForm.profileId = detail.profileId
    profileForm.profileCode = detail.profileCode || ''
    profileForm.profileName = detail.profileName || ''
    profileForm.sourceType = detail.sourceType || 'RAW_JSON'
    profileForm.taskType = detail.taskType || selectionForm.taskType || 'FORMAL_BATCH'
    profileForm.versionId = detail.versionId
    profileForm.requestMethod = detail.requestMethod || 'GET'
    profileForm.endpointUrl = detail.endpointUrl || ''
    profileForm.authType = detail.authType || 'NONE'
    profileForm.authConfigJson = detail.authConfigJson || ''
    profileForm.fetchConfigJson = detail.fetchConfigJson || ''
    profileForm.status = detail.status || '0'
    profileForm.remark = detail.remark || ''
  }
  profileDialogVisible.value = true
}

function buildProfilePayload() {
  return {
    profileId: profileForm.profileId,
    sceneId: selectionForm.sceneId,
    feeId: selectionForm.feeId,
    versionId: profileForm.versionId,
    profileCode: profileForm.profileCode?.trim(),
    profileName: profileForm.profileName?.trim(),
    sourceType: profileForm.sourceType,
    taskType: profileForm.taskType,
    requestMethod: profileForm.requestMethod,
    endpointUrl: profileForm.endpointUrl?.trim(),
    authType: profileForm.authType,
    authConfigJson: profileForm.authConfigJson?.trim(),
    fetchConfigJson: profileForm.fetchConfigJson?.trim(),
    mappingJson: builderForm.mappingJson?.trim(),
    samplePayloadJson: profileForm.sourceType === 'HTTP_API' ? builderForm.requestPayloadJson?.trim() : builderForm.rawJson?.trim(),
    sampleInputJson: buildPreviewRecords.value.length ? JSON.stringify(buildPreviewRecords.value) : calcForm.inputJson?.trim(),
    status: profileForm.status,
    remark: profileForm.remark?.trim()
  }
}

async function handleSubmitProfile() {
  if (!profileForm.profileCode?.trim() || !profileForm.profileName?.trim()) {
    proxy.$modal.msgWarning('请先填写方案编码和方案名称')
    return
  }
  if (profileForm.sourceType === 'HTTP_API' && !profileForm.endpointUrl?.trim()) {
    proxy.$modal.msgWarning('HTTP 接口方案必须填写接口地址')
    return
  }
  const payload = buildProfilePayload()
  if (profileDialogMode.value === 'edit' && payload.profileId) {
    await updateAccessProfile(payload)
  } else {
    await addAccessProfile(payload)
  }
  profileDialogVisible.value = false
  await loadProfiles()
  const matched = profileOptions.value.find(item => item.profileCode === payload.profileCode)
  selectedProfileId.value = matched?.profileId
  proxy.$modal.msgSuccess('接入方案已保存')
}

async function handleProfileChange(profileId) {
  if (!profileId) {
    return
  }
  const response = await getAccessProfile(profileId)
  const detail = response?.data || {}
  selectionForm.versionId = detail.versionId
  selectionForm.taskType = detail.taskType || selectionForm.taskType
  if (detail.mappingJson) {
    builderForm.mappingJson = formatJson(detail.mappingJson)
    autoMappingJson.value = ''
  }
  if (detail.sourceType === 'HTTP_API') {
    builderForm.requestPayloadJson = detail.samplePayloadJson ? formatJson(detail.samplePayloadJson) : ''
    builderForm.rawJson = ''
  } else {
    builderForm.requestPayloadJson = ''
    builderForm.rawJson = detail.samplePayloadJson ? formatJson(detail.samplePayloadJson) : ''
  }
  if (detail.sampleInputJson) {
    calcForm.inputJson = formatJson(detail.sampleInputJson)
  }
  if (selectionForm.sceneId && selectionForm.feeId) {
    await handleBuildTemplate()
  }
}

async function handlePreviewBuildByProfile() {
  activeStageKey.value = 'preview'
  if (!selectedProfileId.value) {
    proxy.$modal.msgWarning('请先选择接入方案')
    return
  }
  if (currentProfile.value?.sourceType !== 'HTTP_API') {
    proxy.$modal.msgWarning('当前方案不是 HTTP 接口类型，不能按方案直连预演')
    return
  }
  const response = await previewAccessProfileFetch(selectedProfileId.value, {
    requestPayloadJson: builderForm.requestPayloadJson?.trim()
  })
  buildPreview.value = response.data || {}
  if (buildPreview.value.fetchedPayloadJson) {
    builderForm.rawJson = formatJson(buildPreview.value.fetchedPayloadJson)
  }
  createdBatch.value = {}
}

async function handleCreateBatchByProfile() {
  activeStageKey.value = 'preview'
  if (!selectedProfileId.value) {
    proxy.$modal.msgWarning('请先选择接入方案')
    return
  }
  if (currentProfile.value?.sourceType !== 'HTTP_API') {
    proxy.$modal.msgWarning('当前方案不是 HTTP 接口类型，不能按方案直连生成批次')
    return
  }
  if (!calcForm.billMonth || !calcForm.billMonth.trim()) {
    proxy.$modal.msgWarning('请先填写账期')
    return
  }
  const response = await createInputBatchByAccessProfile(selectedProfileId.value, {
    billMonth: calcForm.billMonth,
    requestPayloadJson: builderForm.requestPayloadJson?.trim(),
    remark: `${currentProfile.value.profileCode} 直连接口导入批次`
  })
  createdBatch.value = response?.data || {}
  if (createdBatch.value.fetchedPayloadJson) {
    builderForm.rawJson = formatJson(createdBatch.value.fetchedPayloadJson)
  }
  buildPreview.value = {
    ...buildPreview.value,
    fetchMeta: createdBatch.value.fetchMeta,
    fetchedPayloadJson: createdBatch.value.fetchedPayloadJson,
    mappedRecords: createdBatch.value.mappedRecords || buildPreview.value.mappedRecords || [],
    fieldMappings: createdBatch.value.fieldMappings || buildPreview.value.fieldMappings || [],
    missingPaths: createdBatch.value.missingPaths || buildPreview.value.missingPaths || [],
    mappedRecordCount: createdBatch.value.mappedRecordCount || buildPreview.value.mappedRecordCount || 0
  }
  const batchNo = createdBatch.value?.batch?.batchNo
  proxy.$modal.msgSuccess(batchNo ? `已按方案生成导入批次 ${batchNo}` : '已按方案生成导入批次')
}

async function handleResumeBatchByProfile() {
  activeStageKey.value = 'preview'
  if (!selectedProfileId.value) {
    proxy.$modal.msgWarning('请先选择接入方案')
    return
  }
  if (!createdBatch.value?.batch?.batchId) {
    proxy.$modal.msgWarning('当前没有可继续装载的导入批次')
    return
  }
  const response = await createInputBatchByAccessProfile(selectedProfileId.value, {
    resumeBatchId: createdBatch.value.batch.batchId,
    requestPayloadJson: builderForm.requestPayloadJson?.trim(),
    remark: `${currentProfile.value?.profileCode || 'PROFILE'} 继续装载导入批次`
  })
  createdBatch.value = response?.data || {}
  if (createdBatch.value.fetchedPayloadJson) {
    builderForm.rawJson = formatJson(createdBatch.value.fetchedPayloadJson)
  }
  buildPreview.value = {
    ...buildPreview.value,
    fetchMeta: createdBatch.value.fetchMeta,
    fetchedPayloadJson: createdBatch.value.fetchedPayloadJson,
    mappedRecords: createdBatch.value.mappedRecords || buildPreview.value.mappedRecords || [],
    fieldMappings: createdBatch.value.fieldMappings || buildPreview.value.fieldMappings || [],
    missingPaths: createdBatch.value.missingPaths || buildPreview.value.missingPaths || [],
    mappedRecordCount: createdBatch.value.mappedRecordCount || buildPreview.value.mappedRecordCount || 0
  }
  const batchNo = createdBatch.value?.batch?.batchNo
  if (createdBatch.value?.resumable) {
    proxy.$modal.msgSuccess(batchNo ? `已继续装载导入批次 ${batchNo}，仍有后续分页待继续拉取` : '已继续装载导入批次，仍有后续分页待继续拉取')
    return
  }
  proxy.$modal.msgSuccess(batchNo ? `已继续装载并完成导入批次 ${batchNo}` : '已继续装载并完成导入批次')
}

async function handleDeleteProfile() {
  if (!selectedProfileId.value) {
    proxy.$modal.msgWarning('请先选择接入方案')
    return
  }
  await proxy.$modal.confirm('确认删除当前接入方案吗？删除后不会影响已发布版本，只会移除接入中心的复用草稿。')
  await removeAccessProfile(selectedProfileId.value)
  selectedProfileId.value = undefined
  await loadProfiles()
  proxy.$modal.msgSuccess('接入方案已删除')
}

function openCreatedBatchLedger() {
  router.push({ path: COST_MENU_ROUTES.taskBatch, query: { sceneId: selectionForm.sceneId, versionId: selectionForm.versionId, billMonth: calcForm.billMonth } })
}

function openCreatedBatchTaskCenter() {
  if (!createdBatch.value?.batch?.batchNo) {
    router.push({ path: COST_MENU_ROUTES.task, query: { sceneId: selectionForm.sceneId, versionId: selectionForm.versionId, billMonth: calcForm.billMonth } })
    return
  }
  router.push({
    path: COST_MENU_ROUTES.task,
    query: {
      sceneId: selectionForm.sceneId,
      versionId: selectionForm.versionId,
      billMonth: calcForm.billMonth,
      inputSourceType: 'INPUT_BATCH',
      sourceBatchNo: createdBatch.value.batch.batchNo
    }
  })
}

function createMappingDraft(fields) {
  const draft = { bizNo: 'bizNo', objectDimension: 'objectDimension', objectCode: 'objectCode', objectName: 'objectName' }
  ;(fields || []).forEach(field => {
    if (!field?.includedInTemplate || !field?.path) return
    draft[field.path] = field.path
  })
  return draft
}

function resolveRecordTagType(status) {
  if (status === 'SUCCESS') return 'success'
  if (status === 'NO_MATCH') return 'warning'
  if (status === 'FAILED') return 'danger'
  return 'info'
}

function formatJson(value) {
  return safeFormatJson(value)
}

async function initializePage() {
  await loadScenes()
  if (selectionForm.sceneId) {
    await handleSceneChange(selectionForm.sceneId)
    if (selectionForm.feeId || selectionForm.feeCode) {
      await handleBuildTemplate()
      await loadProfiles()
    }
  }
}

watch(
  () => [selectionForm.sceneId, selectionForm.versionId, calcForm.billMonth],
  ([sceneId, versionId, billMonth]) => {
    syncCostWorkContext({ sceneId, versionId, billMonth })
  },
  { immediate: true }
)

onMounted(async () => {
  await initializePage()
})

onActivated(async () => {
  await initializePage()
})
</script>

<style scoped lang="scss">
.access-page {
  display: grid;
  gap: 16px;
}

.access-page__hero,
.access-page__metric,
.access-page__panel {
  border: 1px solid var(--el-border-color);
  border-radius: 16px;
  background: var(--el-bg-color-overlay);
}

.access-page__hero {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  padding: 22px 24px;
}

.access-page__eyebrow {
  font-size: 12px;
  color: var(--el-color-success-dark-2);
  font-weight: 700;
  letter-spacing: 0.08em;
}

.access-page__actions {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
  margin-top: 8px;
}

.access-page__metrics {
  display: grid;
  gap: 16px;
}

.access-page__metrics {
  grid-template-columns: repeat(4, minmax(0, 1fr));
}

.access-page__metric,
.access-page__card {
  display: grid;
  gap: 6px;
  padding: 14px 16px;
}

.access-page__metric strong,
.access-page__card strong {
  font-size: 24px;
  color: var(--el-color-success-dark-2);
}

.access-page__panel {
  padding: 18px;
}

.access-page__panel h3 {
  margin: 0 0 12px;
}

.access-page__query-shell {
  background: color-mix(in srgb, var(--el-color-success-light-9) 12%, var(--el-bg-color-overlay));
}

.access-page__section-head {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: flex-start;
}

.access-page__subtext {
  margin: 6px 0 0;
  color: var(--el-text-color-secondary);
}

.access-page__inline-form {
  margin-top: 14px;
}

.access-page__form-shell,
.access-page__work-pane {
  display: grid;
  gap: 14px;
}

.access-page__alert,
.access-page__table,
.access-page__tabs,
.access-page__tags {
  margin-top: 14px;
}

.access-page__summary {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
  gap: 12px;
  margin-top: 14px;
}

.access-page__summary--context {
  grid-template-columns: repeat(4, minmax(0, 1fr));
}

.access-page__card {
  border-radius: 14px;
  background: color-mix(in srgb, var(--el-color-success-light-9) 30%, var(--el-bg-color-overlay));
}

.access-page__card--context strong {
  font-size: 18px;
  line-height: 1.45;
  color: var(--el-text-color-primary);
  word-break: break-word;
}

.access-page__workflow-shell {
  display: grid;
  gap: 16px;
  padding-top: 20px;
}

.access-page__stage-strip {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
}

.access-page__stage-chip {
  display: flex;
  gap: 12px;
  align-items: center;
  width: 100%;
  border: 1px solid var(--el-border-color);
  border-radius: 14px;
  background: color-mix(in srgb, var(--el-color-success-light-9) 20%, var(--el-bg-color-overlay));
  padding: 12px 14px;
  text-align: left;
  cursor: pointer;
  transition: border-color 0.2s ease, transform 0.2s ease, box-shadow 0.2s ease;
}

.access-page__stage-chip:hover {
  border-color: var(--el-color-success-light-3);
  transform: translateY(-1px);
}

.access-page__stage-chip.is-active {
  border-color: var(--el-color-success);
  box-shadow: 0 0 0 1px color-mix(in srgb, var(--el-color-success) 18%, transparent);
}

.access-page__stage-chip.is-done {
  background: color-mix(in srgb, var(--el-color-success-light-8) 35%, var(--el-bg-color-overlay));
}

.access-page__stage-chip.is-ready {
  background: color-mix(in srgb, var(--el-color-warning-light-8) 28%, var(--el-bg-color-overlay));
}

.access-page__stage-order {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 46px;
  height: 46px;
  border-radius: 12px;
  background: color-mix(in srgb, var(--el-color-success-light-7) 65%, var(--el-bg-color));
  color: var(--el-color-success-dark-2);
  font-weight: 700;
  font-size: 12px;
}

.access-page__stage-main {
  display: grid;
  gap: 4px;
}

.access-page__stage-main strong {
  font-size: 15px;
  color: var(--el-text-color-primary);
}

.access-page__stage-main small {
  color: var(--el-text-color-secondary);
  line-height: 1.45;
}

.access-page__stage-panel {
  border: 1px solid var(--el-border-color);
  border-radius: 16px;
  background: color-mix(in srgb, var(--el-color-success-light-9) 14%, var(--el-bg-color-overlay));
  padding: 18px;
}

.access-page__pane-head {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: flex-start;
}

.access-page__pane-kicker {
  display: inline-flex;
  align-items: center;
  padding: 4px 10px;
  border-radius: 999px;
  background: color-mix(in srgb, var(--el-color-success-light-8) 50%, var(--el-bg-color));
  color: var(--el-color-success-dark-2);
  font-size: 12px;
  font-weight: 700;
  margin-bottom: 8px;
}

.access-page__pane-head h4 {
  margin: 0;
  font-size: 18px;
}

.access-page__pane-head p {
  margin: 6px 0 0;
  color: var(--el-text-color-secondary);
}

.access-page__editor-stack {
  display: grid;
  gap: 14px;
}

.access-page__tags {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  align-items: center;
}

.access-page__tabs pre {
  margin: 0;
  white-space: pre-wrap;
  word-break: break-all;
}

.access-page.is-compact-mode {
  gap: 14px;
}

.access-page.is-compact-mode .access-page__panel {
  padding: 14px;
}

.access-page.is-compact-mode .access-page__stage-strip {
  grid-template-columns: 1fr;
}

@media (max-width: 1280px) {
  .access-page__metrics,
  .access-page__summary--context,
  .access-page__stage-strip {
    grid-template-columns: 1fr;
  }

  .access-page__hero {
    flex-direction: column;
  }

  .access-page__section-head,
  .access-page__pane-head {
    flex-direction: column;
  }
}
</style>

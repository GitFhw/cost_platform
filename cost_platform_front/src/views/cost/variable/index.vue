<template>
  <div class="app-container variable-center">
    <section v-show="!isCompactMode" class="variable-center__hero">
      <div>
        <div class="variable-center__eyebrow">输入治理</div>
        <h2 class="variable-center__title">变量中心</h2>
        <p class="variable-center__subtitle">
          统一管理业务输入、字典取值、第三方接入和公式变量，支撑规则配置、批量运行和结果追溯的输入口径。
        </p>
      </div>
      <el-tag type="info">来源路径优先，变量编码仅作兼容兜底</el-tag>
    </section>

    <section v-show="!isCompactMode" class="variable-center__metrics">
      <div v-for="item in metricItems" :key="item.label" class="variable-center__metric-card">
        <span class="variable-center__metric-label">{{ item.label }}</span>
        <strong class="variable-center__metric-value">{{ item.value }}</strong>
        <span class="variable-center__metric-desc">{{ item.desc }}</span>
      </div>
    </section>

    <section v-show="!isCompactMode" class="variable-center__workspace">
      <div class="variable-center__work-card">
        <div class="variable-center__work-head">
          <div>
            <h3>模板治理</h3>
            <p>共享影响因素模板当前仍以内置模板库为主，但已经可以作为跨场景复用入口统一治理。</p>
          </div>
          <el-button type="primary" plain icon="Collection" @click="handleTemplateCenter">打开模板库</el-button>
        </div>
        <div class="variable-center__template-metrics">
          <div class="variable-center__template-metric">
            <span>模板数量</span>
            <strong>{{ templateList.length }}</strong>
          </div>
          <div class="variable-center__template-metric">
            <span>模板变量总数</span>
            <strong>{{ templateVariableCount }}</strong>
          </div>
          <div class="variable-center__template-metric">
            <span>已复用模板数</span>
            <strong>{{ reusedTemplateCount }}</strong>
          </div>
        </div>
        <div class="variable-center__template-tags">
          <el-tag v-for="item in topTemplates" :key="item.templateCode" size="small">
            {{ item.templateName }} / {{ item.appliedSceneCount || 0 }}场景
          </el-tag>
          <span v-if="!topTemplates.length" class="variable-center__muted">暂无模板数据</span>
        </div>
        <div v-if="lastTemplateApplyResult.templateCode" class="variable-center__latest-apply">
          <strong>最近应用</strong>
          <span>{{ lastTemplateApplyResult.templateName }} -> {{ lastTemplateApplyResult.sceneLabel || '-' }}</span>
          <span>{{ lastTemplateApplyResult.summary }}</span>
        </div>
      </div>

      <div class="variable-center__work-card">
        <div class="variable-center__work-head">
          <div>
            <h3>接入动作</h3>
            <p>把新增、导入、模板下载和复用动作集中到工作台，减少只在表格里找入口。</p>
          </div>
        </div>
        <div class="variable-center__action-grid">
          <button class="variable-center__action-card" type="button" @click="handleAdd">
            <strong>新增变量</strong>
            <span>录入输入、字典、第三方接口或公式变量。</span>
          </button>
          <button class="variable-center__action-card" type="button" @click="handleImport">
            <strong>导入变量</strong>
            <span>先做预览校验，再批量导入变量清单。</span>
          </button>
          <button class="variable-center__action-card" type="button" @click="downloadImportTemplate">
            <strong>下载模板</strong>
            <span>按平台模板整理 Excel 后再执行导入。</span>
          </button>
          <button class="variable-center__action-card" type="button" @click="handleTemplateCenter">
            <strong>应用共享模板</strong>
            <span>把内置影响因素模板复制到目标场景。</span>
          </button>
        </div>
      </div>

      <div class="variable-center__work-card">
        <div class="variable-center__work-head">
          <div>
            <h3>来源口径</h3>
            <p>变量中心当前阶段的口径是先标准化变量，再让规则、公式和运行链复用这些标准变量。</p>
          </div>
        </div>
        <div class="variable-center__guide-list">
          <div class="variable-center__guide-item">
            <el-tag type="success" size="small">INPUT</el-tag>
            <span>直接承接业务输入；来源路径可选，未配置时按变量编码接收平铺输入，配置后按多级对象取值，例如 oddWork.quantity。</span>
          </div>
          <div class="variable-center__guide-item">
            <el-tag type="info" size="small">DICT</el-tag>
            <span>字典负责值域约束；来源路径可选，未配置时按变量编码取值，配置后可从对象属性取值，例如 cover.action。</span>
          </div>
          <div class="variable-center__guide-item">
            <el-tag type="warning" size="small">REMOTE</el-tag>
            <span>承接第三方接口变量；可按变量编码平铺落入上下文，也可声明来源路径落入多级对象。</span>
          </div>
          <div class="variable-center__guide-item">
            <el-tag size="small">FORMULA</el-tag>
            <span>复用公式实验室资产，通过 `formulaCode` 统一进入运行链。</span>
          </div>
        </div>
      </div>
    </section>

    <el-alert
      v-show="!isCompactMode"
      title="变量中心统一承接输入变量、字典变量、第三方接入变量和公式变量，并提供导入预览、复制复用与共享模板能力。"
      type="info"
      :closable="false"
      show-icon
    />

    <el-form ref="queryRef" :model="queryParams" :inline="true" label-width="84px" v-show="showSearch">
      <el-form-item label="所属场景" prop="sceneId">
        <el-select v-model="queryParams.sceneId" clearable filterable placeholder="请选择场景" style="width: 220px" @change="handleSceneChange">
          <el-option v-for="item in sceneOptions" :key="item.sceneId" :label="`${item.sceneName} / ${item.sceneCode}`" :value="item.sceneId" />
        </el-select>
      </el-form-item>
      <el-form-item label="变量分组" prop="groupId">
        <el-select v-model="queryParams.groupId" clearable filterable placeholder="请选择分组" style="width: 200px">
          <el-option v-for="item in groupOptions" :key="item.groupId" :label="item.groupName" :value="item.groupId" />
        </el-select>
      </el-form-item>
      <el-form-item label="变量编码" prop="variableCode">
        <el-input v-model="queryParams.variableCode" clearable style="width: 180px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="变量名称" prop="variableName">
        <el-input v-model="queryParams.variableName" clearable style="width: 180px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="来源类型" prop="sourceType">
        <el-select v-model="queryParams.sourceType" clearable style="width: 160px">
          <el-option v-for="item in sourceTypeOptions" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="来源系统" prop="sourceSystem">
        <el-input v-model="queryParams.sourceSystem" clearable placeholder="如 WMS / ERP / TMS" style="width: 180px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5"><el-button type="primary" plain icon="Plus" @click="handleAdd" v-hasPermi="['cost:variable:add']">新增变量</el-button></el-col>
      <el-col :span="1.5"><el-button type="success" plain icon="Edit" :disabled="single" @click="handleUpdate()" v-hasPermi="['cost:variable:edit']">修改变量</el-button></el-col>
      <el-col :span="1.5"><el-button type="danger" plain icon="Delete" :disabled="multiple" @click="handleDelete()" v-hasPermi="['cost:variable:remove']">删除变量</el-button></el-col>
      <el-col :span="1.5"><el-button type="primary" plain icon="FolderOpened" @click="handleGroupCenter" v-hasPermi="['cost:variable:list']">变量组</el-button></el-col>
      <el-col :span="1.5"><el-button type="primary" plain icon="Upload" @click="handleImport" v-hasPermi="['cost:variable:add']">导入变量</el-button></el-col>
      <el-col :span="1.5"><el-button type="success" plain icon="CopyDocument" :disabled="single" @click="handleCopy()">复制变量</el-button></el-col>
      <el-col :span="1.5"><el-button type="primary" plain icon="Collection" @click="handleTemplateCenter">共享模板</el-button></el-col>
      <el-col :span="1.5"><el-button type="info" plain icon="Connection" :disabled="single" @click="handleTestRemote">测试接口</el-button></el-col>
      <el-col :span="1.5"><el-button type="info" plain icon="View" :disabled="single" @click="handlePreviewRemote">预览数据</el-button></el-col>
      <el-col :span="1.5"><el-button type="info" plain icon="RefreshRight" @click="handleRefreshRemote">刷新状态</el-button></el-col>
      <el-col :span="1.5"><el-button type="warning" plain icon="Download" @click="handleExport" v-hasPermi="['cost:variable:export']">导出</el-button></el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList" />
    </el-row>

    <el-table v-loading="loading" :data="variableList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column type="index" label="序号" width="70" align="center" />
      <el-table-column label="场景" min-width="220" align="center">
        <template #default="scope">{{ scope.row.sceneCode }} / {{ scope.row.sceneName }}</template>
      </el-table-column>
      <el-table-column label="分组" prop="groupName" width="140" align="center" />
      <el-table-column label="变量编码" prop="variableCode" width="160" align="center" />
      <el-table-column label="变量名称" prop="variableName" min-width="160" align="center" :show-overflow-tooltip="true" />
      <el-table-column label="类型" prop="variableType" width="120" align="center">
        <template #default="scope"><dict-tag :options="variableTypeOptions" :value="scope.row.variableType" /></template>
      </el-table-column>
      <el-table-column label="来源" prop="sourceType" width="120" align="center">
        <template #default="scope"><dict-tag :options="sourceTypeOptions" :value="scope.row.sourceType" /></template>
      </el-table-column>
      <el-table-column label="来源摘要" min-width="240" :show-overflow-tooltip="true">
        <template #default="scope">
          <div class="variable-center__source-cell">
            <strong>{{ resolveVariableSourceSummary(scope.row) }}</strong>
            <span>{{ resolveVariableSourceHint(scope.row) }}</span>
          </div>
        </template>
      </el-table-column>
      <el-table-column label="远程状态" width="220" align="center">
        <template #default="scope">
          <div v-if="scope.row.sourceType === 'REMOTE'" class="variable-center__remote-status">
            <el-tag :type="resolveRemoteStatusTag(scope.row.remoteTestStatus)" effect="light">
              {{ resolveRemoteStatusText(scope.row.remoteTestStatus) }}
            </el-tag>
            <span>{{ formatDateTime(scope.row.remoteTestStatus?.testedAt) }}</span>
            <small>{{ resolveRemoteStatusReason(scope.row.remoteTestStatus) }}</small>
            <small>缓存：{{ resolveDictLabel(cachePolicyOptions, scope.row.cachePolicy) }}</small>
          </div>
          <span v-else>-</span>
        </template>
      </el-table-column>
      <el-table-column label="状态" prop="status" width="100" align="center">
        <template #default="scope"><dict-tag :options="variableStatusOptions" :value="scope.row.status" /></template>
      </el-table-column>
      <el-table-column label="操作" width="260" fixed="right" align="center">
        <template #default="scope">
          <div class="cost-row-actions">
            <el-button link type="primary" icon="Document" @click="handleDetail(scope.row)">详情</el-button>
            <el-button link type="primary" icon="Edit" @click="handleUpdate(scope.row)">修改</el-button>
            <el-dropdown trigger="click" @command="command => handleVariableRowCommand(command, scope.row)">
              <el-button link type="primary" icon="MoreFilled">更多</el-button>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item command="governance" icon="View">治理检查</el-dropdown-item>
                  <el-dropdown-item command="copy" icon="CopyDocument">复制变量</el-dropdown-item>
                  <el-dropdown-item command="delete" icon="Delete" v-hasPermi="['cost:variable:remove']">删除变量</el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </div>
        </template>
      </el-table-column>
      <template #empty>
        <cost-table-empty
          title="当前没有变量数据"
          description="变量承接业务输入、字典、第三方接口和公式结果。可以新增变量、导入变量，或先套用共享模板。"
        >
          <el-button type="primary" plain icon="Plus" @click="handleAdd" v-hasPermi="['cost:variable:add']">新增变量</el-button>
          <el-button type="primary" plain icon="Upload" @click="handleImport" v-hasPermi="['cost:variable:add']">导入变量</el-button>
          <el-button icon="Refresh" @click="resetQuery">清空筛选</el-button>
        </cost-table-empty>
      </template>
    </el-table>

    <pagination v-show="total > 0" :total="total" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" @pagination="getList" />

    <el-dialog title="变量组维护" v-model="groupOpen" width="820px" append-to-body>
      <div class="variable-group-toolbar">
        <el-select v-model="groupQuery.sceneId" clearable filterable placeholder="请选择场景" style="width: 320px" @change="refreshGroupList">
          <el-option v-for="item in sceneOptions" :key="item.sceneId" :label="`${item.sceneName} / ${item.sceneCode}`" :value="item.sceneId" />
        </el-select>
        <el-input v-model="groupQuery.keyword" clearable placeholder="分组编码/名称" style="width: 220px" @keyup.enter="refreshGroupList" />
        <el-button type="primary" icon="Search" @click="refreshGroupList">搜索</el-button>
        <el-button type="primary" plain icon="Plus" @click="handleAddGroup" v-hasPermi="['cost:variable:add']">新增分组</el-button>
      </div>
      <el-alert
        class="mt12"
        type="info"
        :closable="false"
        show-icon
        title="复制场景配置时会同步复制源场景变量组；变量删除后，空变量组可在这里清理，也不会再阻断场景删除。"
      />
      <el-table v-loading="groupLoading" :data="groupList" size="small" class="mt12" max-height="360">
        <el-table-column prop="sceneName" label="场景" min-width="180" show-overflow-tooltip />
        <el-table-column prop="groupCode" label="分组编码" min-width="150" />
        <el-table-column prop="groupName" label="分组名称" min-width="150" />
        <el-table-column prop="variableCount" label="变量数" width="90" align="center">
          <template #default="scope">{{ scope.row.variableCount || 0 }}</template>
        </el-table-column>
        <el-table-column prop="sortNo" label="排序" width="80" align="center" />
        <el-table-column label="状态" width="100" align="center">
          <template #default="scope"><dict-tag :options="groupStatusOptions" :value="scope.row.status" /></template>
        </el-table-column>
        <el-table-column label="操作" width="160" fixed="right" align="center">
          <template #default="scope">
            <el-button link type="primary" icon="Edit" @click="handleUpdateGroup(scope.row)" v-hasPermi="['cost:variable:edit']">修改</el-button>
            <el-button link type="danger" icon="Delete" :disabled="Number(scope.row.variableCount || 0) > 0" @click="handleDeleteGroup(scope.row)" v-hasPermi="['cost:variable:remove']">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-dialog>

    <el-dialog :title="groupTitle" v-model="groupFormOpen" width="520px" append-to-body>
      <el-form ref="groupRef" :model="groupForm" :rules="groupRules" label-width="96px">
        <el-form-item label="所属场景" prop="sceneId">
          <el-select v-model="groupForm.sceneId" filterable style="width: 100%">
            <el-option v-for="item in sceneOptions" :key="item.sceneId" :label="`${item.sceneName} / ${item.sceneCode}`" :value="item.sceneId" />
          </el-select>
        </el-form-item>
        <el-form-item label="分组编码" prop="groupCode">
          <el-input v-model="groupForm.groupCode" />
        </el-form-item>
        <el-form-item label="分组名称" prop="groupName">
          <el-input v-model="groupForm.groupName" />
        </el-form-item>
        <el-form-item label="排序" prop="sortNo">
          <el-input-number v-model="groupForm.sortNo" :min="0" style="width: 100%" />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="groupForm.status">
            <el-radio v-for="item in groupStatusOptions" :key="item.value" :label="item.value">{{ item.label }}</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="备注" prop="remark">
          <el-input v-model="groupForm.remark" type="textarea" :rows="3" />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button type="primary" @click="submitGroupForm">确 定</el-button>
          <el-button @click="groupFormOpen = false">取 消</el-button>
        </div>
      </template>
    </el-dialog>

    <el-drawer v-model="open" :title="title" size="720px" append-to-body>
      <el-form ref="variableRef" :model="form" :rules="rules" label-width="108px">
        <el-form-item label="所属场景" prop="sceneId">
          <el-select v-model="form.sceneId" filterable style="width: 100%" @change="loadFormGroups">
            <el-option v-for="item in sceneOptions" :key="item.sceneId" :label="`${item.sceneName} / ${item.sceneCode}`" :value="item.sceneId" />
          </el-select>
        </el-form-item>
        <el-form-item label="变量分组" prop="groupId">
          <el-select v-model="form.groupId" clearable filterable style="width: 100%">
            <el-option v-for="item in formGroupOptions" :key="item.groupId" :label="item.groupName" :value="item.groupId" />
          </el-select>
        </el-form-item>
        <div class="variable-center__source-guide">
          <div v-for="item in sourceGuideItems" :key="item.value" class="variable-center__source-guide-item" :class="{ 'is-active': form.sourceType === item.value }">
            <el-tag size="small" :type="item.tag">{{ item.label }}</el-tag>
            <strong>{{ item.title }}</strong>
            <span>{{ item.desc }}</span>
          </div>
        </div>
        <div class="variable-center__form-section">
          <div class="variable-center__form-section-head">
            <strong>基础信息</strong>
            <span>先定义变量身份、数据类型和启停状态，后续配置会随来源类型自动分流。</span>
          </div>
        <el-row :gutter="14">
          <el-col :span="12"><el-form-item label="变量编码" prop="variableCode"><el-input v-model="form.variableCode" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="变量名称" prop="variableName"><el-input v-model="form.variableName" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="变量类型" prop="variableType"><el-select v-model="form.variableType" style="width: 100%"><el-option v-for="item in variableTypeOptions" :key="item.value" :label="item.label" :value="item.value" /></el-select></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="来源类型" prop="sourceType"><el-select v-model="form.sourceType" style="width: 100%"><el-option v-for="item in sourceTypeOptions" :key="item.value" :label="item.label" :value="item.value" /></el-select></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="数据类型" prop="dataType"><el-select v-model="form.dataType" style="width: 100%"><el-option v-for="item in dataTypeOptions" :key="item.value" :label="item.label" :value="item.value" /></el-select></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="状态" prop="status"><el-radio-group v-model="form.status"><el-radio v-for="item in variableStatusOptions" :key="item.value" :label="item.value">{{ item.label }}</el-radio></el-radio-group></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="默认值" prop="defaultValue"><el-input v-model="form.defaultValue" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="精度" prop="precisionScale"><el-input-number v-model="form.precisionScale" :min="0" :max="8" style="width: 100%" /></el-form-item></el-col>
        </el-row>
        </div>
        <div v-if="['INPUT', 'DICT', 'REMOTE'].includes(form.sourceType)" class="variable-center__form-section">
          <div class="variable-center__form-section-head">
            <strong>取值定位</strong>
            <span>{{ currentSourceGuide.pathHint }}</span>
          </div>
          <el-row :gutter="14">
          <el-col v-if="['INPUT', 'DICT', 'REMOTE'].includes(form.sourceType)" :span="24">
            <el-form-item label="来源路径" prop="dataPath">
              <el-input v-model="form.dataPath" placeholder="可选；为空时按变量编码平铺取值，填写后如 cover.action、oddWork.quantity" />
            </el-form-item>
            <div class="variable-center__drawer-tip variable-center__drawer-tip--compact">
              运行时优先按来源路径取值；未配置时自动按变量编码取平铺输入。字典类型只负责值域约束，不代表取值位置。
            </div>
          </el-col>
        </el-row>
        </div>
        <div v-if="form.sourceType === 'DICT'" class="variable-center__form-section">
          <div class="variable-center__form-section-head">
            <strong>字典约束</strong>
            <span>用于限定变量可选值，避免规则条件里出现不可识别的业务值。</span>
          </div>
        <el-form-item v-if="form.sourceType === 'DICT'" label="字典类型" prop="dictType">
          <el-select v-model="form.dictType" clearable filterable style="width: 100%" placeholder="请选择系统字典或核算字典">
            <el-option-group v-for="group in dictTypeOptionGroups" :key="group.label" :label="group.label">
              <el-option
                v-for="item in group.items"
                :key="item.dictId || item.dictType"
                :label="`${item.dictName} (${item.dictType})${item.status === '0' ? '' : ' [已停用]'}`"
                :value="item.dictType"
                :disabled="item.status !== '0'"
              />
            </el-option-group>
          </el-select>
          <div class="variable-center__drawer-tip variable-center__drawer-tip--compact">
            字典来源变量统一从系统字典与核算字典下拉选择；来源路径可选，配置后可绑定对象内属性，例如 cover.action。
          </div>
        </el-form-item>
        </div>
        <template v-if="form.sourceType === 'REMOTE'">
          <div class="variable-center__form-section">
          <div class="variable-center__form-section-head">
            <strong>远程接入</strong>
            <span>第三方接口变量采用请求定义、鉴权定义、响应提取、字段映射、分页策略和特殊适配分区配置。</span>
          </div>
          <el-alert title="先测试接口，再预览映射结果，确认通过后保存变量。" type="info" :closable="false" show-icon class="mb12" />
          <div class="variable-center__drawer-actions variable-center__drawer-actions--remote">
            <el-button type="primary" plain icon="Connection" @click="handleTestRemoteDraft">测试接口</el-button>
            <el-button type="success" plain icon="View" @click="handlePreviewRemoteDraft">预览数据</el-button>
          </div>
          <el-row :gutter="14">
            <el-col :span="8"><el-form-item label="来源系统" prop="sourceSystem"><el-input v-model="form.sourceSystem" placeholder="如 WMS / ERP / TMS" /></el-form-item></el-col>
            <el-col :span="8"><el-form-item label="请求方式" prop="requestMethod"><el-select v-model="form.requestMethod" style="width: 100%"><el-option v-for="item in requestMethodOptions" :key="item.value" :label="item.label" :value="item.value" /></el-select></el-form-item></el-col>
            <el-col :span="8"><el-form-item label="适配器类型" prop="adapterType"><el-select v-model="form.adapterType" style="width: 100%"><el-option v-for="item in adapterTypeOptions" :key="item.value" :label="item.label" :value="item.value" /></el-select></el-form-item></el-col>
            <el-col :span="24"><el-form-item label="第三方接口" prop="remoteApi"><el-input v-model="form.remoteApi" placeholder="http/https 接口地址" /></el-form-item></el-col>
            <el-col :span="24"><el-form-item label="内容类型" prop="contentType"><el-select v-model="form.contentType" style="width: 100%" filterable allow-create default-first-option><el-option v-for="item in contentTypeOptions" :key="item.value" :label="item.label" :value="item.value" /></el-select></el-form-item></el-col>
            <el-col :span="24"><el-form-item label="查询参数JSON" prop="queryConfigJson"><JsonEditor v-model="form.queryConfigJson" title="查询参数 JSON" :rows="3" placeholder='如 {"pageNum":1,"pageSize":20}' /></el-form-item></el-col>
            <el-col :span="24"><el-form-item label="请求头JSON" prop="requestHeadersJson"><JsonEditor v-model="form.requestHeadersJson" title="请求头 JSON" :rows="4" placeholder='如 {"Referer":"...","User-Agent":"...","Cookie":"..."}' /></el-form-item></el-col>
            <el-col :span="24"><el-form-item label="请求体模板" prop="bodyTemplateJson"><JsonEditor v-model="form.bodyTemplateJson" title="请求体模板" :lang="requestBodyLang" :rows="4" :validate-on-blur="false" placeholder='POST/PUT 时可配置 JSON 请求体或原始文本模板' /></el-form-item></el-col>
            <el-col :span="12"><el-form-item label="鉴权方式" prop="authType"><el-select v-model="form.authType" style="width: 100%"><el-option v-for="item in authTypeOptions" :key="item.value" :label="item.label" :value="item.value" /></el-select></el-form-item></el-col>
            <el-col :span="24"><el-form-item label="鉴权配置JSON" prop="authConfigJson"><JsonEditor v-model="form.authConfigJson" title="鉴权配置 JSON" :rows="4" placeholder='如 {"token":"..."} / {"username":"...","password":"..."}' /></el-form-item></el-col>
            <el-col :span="24"><el-form-item label="响应提取配置JSON" prop="responseConfigJson"><JsonEditor v-model="form.responseConfigJson" title="响应提取配置 JSON" :rows="4" placeholder='如 {"successPath":"code","successValues":[200],"messagePath":"msg","listPath":"rows","totalPath":"total"}' /></el-form-item></el-col>
            <el-col :span="24"><el-form-item label="映射配置JSON" prop="mappingConfigJson"><JsonEditor v-model="form.mappingConfigJson" title="映射配置 JSON" :rows="4" placeholder='如 {"sourceCode":"code","sourceName":"name","mappedValue":"price"}' /></el-form-item></el-col>
            <el-col :span="24"><el-form-item label="分页策略JSON" prop="pageConfigJson"><JsonEditor v-model="form.pageConfigJson" title="分页策略 JSON" :rows="3" placeholder='如 {"pageNumKey":"pageNum","pageSizeKey":"pageSize","previewPageNum":1,"previewPageSize":20}' /></el-form-item></el-col>
            <el-col :span="24"><el-form-item label="适配器配置JSON" prop="adapterConfigJson"><JsonEditor v-model="form.adapterConfigJson" title="适配器配置 JSON" :rows="3" placeholder='如 {"listPathCandidates":["data.rows","rows"]}' /></el-form-item></el-col>
            <el-col :span="8"><el-form-item label="同步方式" prop="syncMode"><el-select v-model="form.syncMode" style="width: 100%"><el-option v-for="item in syncModeOptions" :key="item.value" :label="item.label" :value="item.value" /></el-select></el-form-item></el-col>
            <el-col :span="8"><el-form-item label="缓存策略" prop="cachePolicy"><el-select v-model="form.cachePolicy" style="width: 100%"><el-option v-for="item in cachePolicyOptions" :key="item.value" :label="item.label" :value="item.value" /></el-select></el-form-item></el-col>
            <el-col :span="8"><el-form-item label="失败兜底" prop="fallbackPolicy"><el-select v-model="form.fallbackPolicy" style="width: 100%"><el-option v-for="item in fallbackPolicyOptions" :key="item.value" :label="item.label" :value="item.value" /></el-select></el-form-item></el-col>
          </el-row>
          </div>
        </template>
        <template v-if="form.sourceType === 'FORMULA'">
          <div class="variable-center__form-section">
          <div class="variable-center__form-section-head">
            <strong>公式绑定</strong>
            <span>公式变量只绑定公式实验室资产，表达式在公式实验室维护，变量中心负责运行链引用。</span>
          </div>
          <el-form-item label="公式编码" prop="formulaCode">
            <el-select v-model="form.formulaCode" clearable filterable style="width: 100%" placeholder="请选择公式实验室中的公式编码">
              <el-option v-for="item in formulaOptions" :key="item.formulaCode" :label="`${item.formulaName} / ${item.formulaCode}`" :value="item.formulaCode" />
            </el-select>
          </el-form-item>
          <div class="variable-center__drawer-actions">
            <el-button type="primary" link @click="openFormulaWorkbench">前往公式实验室</el-button>
          </div>
          <el-form-item label="中文公式">
            <div class="variable-center__formula-preview">{{ selectedFormulaMeta.businessFormula || '请选择公式编码，系统会自动回填中文公式与标准表达式。' }}</div>
          </el-form-item>
          <el-form-item label="标准表达式" prop="formulaExpr">
            <el-input v-model="form.formulaExpr" type="textarea" :rows="3" readonly placeholder="选择公式编码后自动回填，仅用于查看历史表达式" />
          </el-form-item>
          </div>
        </template>
        <el-form-item label="备注" prop="remark"><el-input v-model="form.remark" type="textarea" :rows="3" /></el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button type="primary" @click="submitForm">确 定</el-button>
          <el-button @click="cancel">取 消</el-button>
        </div>
      </template>
    </el-drawer>

    <el-dialog title="接口测试" v-model="testOpen" width="520px" append-to-body>
      <div v-if="testResult" class="variable-center__test-dialog">
        <el-descriptions :column="1" border>
          <el-descriptions-item label="结果"><el-tag :type="testResult.success ? 'success' : 'danger'">{{ testResult.success ? '通过' : '失败' }}</el-tag></el-descriptions-item>
          <el-descriptions-item label="说明">{{ testResult.message }}</el-descriptions-item>
          <el-descriptions-item label="来源系统">{{ testResult.sourceSystem || '-' }}</el-descriptions-item>
          <el-descriptions-item label="接口地址">{{ testResult.remoteApi || '-' }}</el-descriptions-item>
          <el-descriptions-item label="请求方式">{{ resolveRemoteOptionLabel(requestMethodOptions, testResult.requestMethod) }}</el-descriptions-item>
          <el-descriptions-item label="鉴权方式">{{ resolveDictLabel(authTypeOptions, testResult.authType) }}</el-descriptions-item>
          <el-descriptions-item label="HTTP状态">{{ testResult.statusCode ?? '-' }}</el-descriptions-item>
          <el-descriptions-item label="耗时">{{ testResult.elapsedMs != null ? `${testResult.elapsedMs} ms` : '-' }}</el-descriptions-item>
          <el-descriptions-item v-if="!testResult.success" label="失败阶段">{{ testResult.failureStage || '-' }}</el-descriptions-item>
          <el-descriptions-item v-if="!testResult.success" label="诊断信息">{{ testResult.diagnosticMessage || '-' }}</el-descriptions-item>
        </el-descriptions>
        <div v-if="testResult.responsePreview" class="variable-center__test-preview">
          <div class="variable-center__test-preview-label">响应预览</div>
          <pre class="variable-center__test-preview-content">{{ testResult.responsePreview }}</pre>
        </div>
      </div>
    </el-dialog>

    <el-dialog title="数据预览" v-model="previewOpen" width="880px" append-to-body>
      <el-row :gutter="12" v-if="previewResult">
        <el-col :span="12">
          <el-table :data="previewResult.rawRows" height="260" size="small">
            <el-table-column prop="sourceCode" label="源编码" />
            <el-table-column prop="sourceName" label="源名称" />
            <el-table-column prop="value" label="源值" />
            <el-table-column prop="rawJson" label="原始JSON" min-width="220" show-overflow-tooltip />
          </el-table>
        </el-col>
        <el-col :span="12">
          <el-table :data="previewResult.mappedRows" height="260" size="small">
            <el-table-column prop="variableCode" label="变量编码" />
            <el-table-column prop="mappedValue" label="映射值" />
            <el-table-column prop="dataPath" label="来源路径" />
            <el-table-column prop="rawJson" label="映射依据" min-width="220" show-overflow-tooltip />
          </el-table>
        </el-col>
      </el-row>
      <el-descriptions v-if="previewResult" :column="2" border class="mt12">
        <el-descriptions-item label="来源系统">{{ previewResult.sourceSystem || '-' }}</el-descriptions-item>
        <el-descriptions-item label="请求方式">{{ resolveRemoteOptionLabel(requestMethodOptions, previewResult.requestMethod) }}</el-descriptions-item>
        <el-descriptions-item label="同步方式">{{ resolveDictLabel(syncModeOptions, previewResult.syncMode) }}</el-descriptions-item>
        <el-descriptions-item label="适配器类型">{{ resolveRemoteOptionLabel(adapterTypeOptions, previewResult.adapterType) }}</el-descriptions-item>
        <el-descriptions-item label="缓存策略">{{ resolveDictLabel(cachePolicyOptions, previewResult.cachePolicy) }}</el-descriptions-item>
        <el-descriptions-item label="失败兜底">{{ resolveDictLabel(fallbackPolicyOptions, previewResult.fallbackPolicy) }}</el-descriptions-item>
        <el-descriptions-item label="HTTP状态">{{ previewResult.statusCode ?? '-' }}</el-descriptions-item>
        <el-descriptions-item label="返回行数">{{ previewResult.rowCount ?? 0 }}</el-descriptions-item>
        <el-descriptions-item label="耗时">{{ previewResult.elapsedMs != null ? `${previewResult.elapsedMs} ms` : '-' }}</el-descriptions-item>
        <el-descriptions-item label="响应提示">{{ previewResult.responseMessage || '-' }}</el-descriptions-item>
      </el-descriptions>
    </el-dialog>

    <el-dialog title="导入变量" v-model="importOpen" width="880px" append-to-body>
      <el-upload ref="uploadRef" :limit="1" accept=".xlsx,.xls" :auto-upload="false" :show-file-list="true" :on-change="handleImportFileChange" :on-remove="handleImportFileRemove" drag>
        <el-icon class="el-icon--upload"><UploadFilled /></el-icon>
        <div class="el-upload__text">将文件拖到此处，或<em>点击上传</em></div>
        <template #tip>
          <div class="el-upload__tip text-center">
            <div class="el-upload__tip">
              <el-checkbox v-model="importForm.updateSupport">若变量编码已存在则覆盖更新</el-checkbox>
            </div>
            <span>仅支持 xls / xlsx 格式，建议先下载模板后录入。</span>
            <el-link type="primary" underline="never" style="font-size: 12px; vertical-align: baseline" @click="downloadImportTemplate">下载模板</el-link>
          </div>
        </template>
      </el-upload>
      <el-alert v-if="importPreview.totalRows" :title="`本次导入共 ${importPreview.totalRows} 行，通过 ${importPreview.passRows} 行，失败 ${importPreview.failRows} 行`" :type="importPreview.failRows ? 'warning' : 'success'" :closable="false" show-icon class="mt12" />
      <el-row :gutter="12" v-if="importPreview.totalRows" class="mt12">
        <el-col :span="14">
          <el-table :data="importPreview.previewRows" size="small" max-height="260">
            <el-table-column prop="rowNum" label="行号" width="70" />
            <el-table-column prop="sceneCode" label="场景编码" width="140" />
            <el-table-column prop="variableCode" label="变量编码" width="160" />
            <el-table-column prop="variableName" label="变量名称" min-width="160" />
            <el-table-column prop="importAction" label="动作" width="100" />
          </el-table>
        </el-col>
        <el-col :span="10">
          <el-table :data="importPreview.issues" size="small" max-height="260">
            <el-table-column prop="rowNum" label="行号" width="70" />
            <el-table-column prop="fieldLabel" label="错误字段" width="120">
              <template #default="scope">{{ scope.row.fieldLabel || scope.row.fieldName || '-' }}</template>
            </el-table-column>
            <el-table-column prop="rawValue" label="原始值" width="120" show-overflow-tooltip />
            <el-table-column prop="variableCode" label="变量编码" width="120" />
            <el-table-column prop="message" label="校验问题" min-width="220" show-overflow-tooltip />
          </el-table>
        </el-col>
      </el-row>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="submitImportPreview">导入预览</el-button>
          <el-button type="warning" plain :disabled="!importPreview.failRows" @click="downloadImportIssueReport">下载失败报告</el-button>
          <el-button type="primary" :disabled="!importPreview.importable" @click="submitImportData">确认导入</el-button>
          <el-button @click="importOpen = false">取 消</el-button>
        </div>
      </template>
    </el-dialog>

    <el-dialog title="复制变量" v-model="copyOpen" width="520px" append-to-body>
      <el-form ref="copyRef" :model="copyForm" :rules="copyRules" label-width="108px">
        <el-form-item label="源变量">
          <el-input :model-value="`${copySource.variableCode || ''}${copySource.variableName ? ' / ' + copySource.variableName : ''}`" disabled />
        </el-form-item>
        <el-form-item label="目标场景" prop="targetSceneId">
          <el-select v-model="copyForm.targetSceneId" filterable style="width: 100%" @change="loadCopyGroups">
            <el-option v-for="item in sceneOptions" :key="item.sceneId" :label="`${item.sceneName} / ${item.sceneCode}`" :value="item.sceneId" />
          </el-select>
        </el-form-item>
        <el-form-item label="目标分组" prop="targetGroupId">
          <el-select v-model="copyForm.targetGroupId" clearable filterable style="width: 100%">
            <el-option v-for="item in copyGroupOptions" :key="item.groupId" :label="item.groupName" :value="item.groupId" />
          </el-select>
        </el-form-item>
        <el-form-item label="新变量编码" prop="variableCode">
          <el-input v-model="copyForm.variableCode" />
        </el-form-item>
        <el-form-item label="新变量名称" prop="variableName">
          <el-input v-model="copyForm.variableName" />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button type="primary" @click="submitCopy">确认复制</el-button>
          <el-button @click="copyOpen = false">取 消</el-button>
        </div>
      </template>
    </el-dialog>

    <el-dialog title="共享影响因素模板" v-model="templateOpen" width="980px" append-to-body>
      <el-row :gutter="16">
        <el-col :span="11">
          <el-form :model="templateForm" label-width="96px">
            <el-form-item label="目标场景">
              <el-select v-model="templateForm.sceneId" filterable style="width: 100%" @change="loadTemplateGroups">
                <el-option v-for="item in sceneOptions" :key="item.sceneId" :label="`${item.sceneName} / ${item.sceneCode}`" :value="item.sceneId" />
              </el-select>
            </el-form-item>
            <el-form-item label="目标分组">
              <el-select v-model="templateForm.groupId" clearable filterable style="width: 100%">
                <el-option v-for="item in templateGroupOptions" :key="item.groupId" :label="item.groupName" :value="item.groupId" />
              </el-select>
            </el-form-item>
            <el-form-item label="覆盖更新">
              <el-checkbox v-model="templateForm.updateSupport">已有变量编码时允许更新</el-checkbox>
            </el-form-item>
          </el-form>
          <div class="variable-template-toolbar">
            <el-input v-model="templateKeyword" clearable placeholder="模板编码/名称" />
            <el-select v-model="templateSourceTypeFilter" clearable placeholder="来源类型">
              <el-option v-for="item in templateSourceTypeOptions" :key="item.value" :label="item.label" :value="item.value" />
            </el-select>
          </div>
          <div class="variable-template-list">
            <div v-for="item in filteredTemplateList" :key="item.templateCode" class="variable-template-card" :class="{ 'is-active': templateForm.templateCode === item.templateCode }" @click="templateForm.templateCode = item.templateCode">
              <div class="variable-template-card__title">{{ item.templateName }}</div>
              <div class="variable-template-card__code">{{ item.templateCode }}</div>
              <div class="variable-template-card__desc">{{ item.description }}</div>
              <div class="variable-template-card__meta">{{ item.namespaceHint }}</div>
              <div class="variable-template-card__tags">
                <el-tag type="info">{{ item.variableCount }} 个变量</el-tag>
                <el-tag type="success">{{ item.appliedSceneCount || 0 }} 个场景</el-tag>
                <el-tag type="warning">完整覆盖 {{ item.fullyAppliedSceneCount || 0 }}</el-tag>
              </div>
              <div class="variable-template-card__foot">
                {{ item.latestSceneName ? `最近复用：${item.latestSceneName}` : '暂无复用记录' }}
              </div>
            </div>
            <el-empty v-if="!filteredTemplateList.length" description="暂无匹配模板" />
          </div>
        </el-col>
        <el-col :span="13">
          <el-empty v-if="!currentTemplate" description="请选择左侧共享模板" />
          <template v-else>
            <el-alert :title="currentTemplate.templateName" :description="currentTemplate.namespaceHint" type="info" :closable="false" show-icon />
            <div class="variable-template-summary mt12">
              <div class="variable-template-summary__item">
                <span>模板变量数</span>
                <strong>{{ currentTemplateMetrics.total }}</strong>
              </div>
              <div class="variable-template-summary__item">
                <span>输入变量</span>
                <strong>{{ currentTemplateMetrics.input }}</strong>
              </div>
              <div class="variable-template-summary__item">
                <span>字典变量</span>
                <strong>{{ currentTemplateMetrics.dict }}</strong>
              </div>
              <div class="variable-template-summary__item">
                <span>公式变量</span>
                <strong>{{ currentTemplateMetrics.formula }}</strong>
              </div>
            </div>
            <div class="variable-template-governance mt12">
              <div class="variable-template-governance__item">
                <span>已复用场景</span>
                <strong>{{ currentTemplate.appliedSceneCount || 0 }}</strong>
              </div>
              <div class="variable-template-governance__item">
                <span>完整覆盖场景</span>
                <strong>{{ currentTemplate.fullyAppliedSceneCount || 0 }}</strong>
              </div>
              <div class="variable-template-governance__item">
                <span>已落地变量</span>
                <strong>{{ currentTemplate.matchedVariableCount || 0 }}</strong>
              </div>
              <div class="variable-template-governance__item">
                <span>最近复用时间</span>
                <strong>{{ formatDateTime(currentTemplate.latestAppliedTime) }}</strong>
              </div>
            </div>
            <el-alert
              class="mt12"
              type="success"
              :closable="false"
              show-icon
              :title="currentTemplate.latestSceneName ? `最近复用场景：${currentTemplate.latestSceneName}` : '当前模板还没有复用到具体场景。'"
              :description="currentTemplate.recentSceneNames?.length ? `复用场景示例：${currentTemplate.recentSceneNames.join('、')}` : '模板库目前仍以内置模板为主，可通过左侧选择目标场景直接落地。'"
            />
            <el-alert
              v-if="templateApplyMatchesCurrent"
              class="mt12"
              type="success"
              :closable="false"
              show-icon
              :title="`最近一次应用结果：${lastTemplateApplyResult.summary}`"
              :description="lastTemplateApplyResult.appliedAt ? `应用时间：${formatDateTime(lastTemplateApplyResult.appliedAt)}` : ''"
            />
            <el-table :data="currentTemplate.sceneSummaries || []" size="small" class="mt12" max-height="220" empty-text="当前还没有场景复用记录">
              <el-table-column prop="sceneLabel" label="复用场景" min-width="220" />
              <el-table-column prop="matchedCount" label="已落地变量" width="110" align="center" />
              <el-table-column label="覆盖率" width="110" align="center">
                <template #default="scope">{{ scope.row.coverageRate }}%</template>
              </el-table-column>
              <el-table-column label="最近复用时间" min-width="150" align="center">
                <template #default="scope">{{ formatDateTime(scope.row.latestAppliedTime) }}</template>
              </el-table-column>
            </el-table>
            <div class="variable-template-impact mt12" v-loading="templateImpactLoading">
              <div class="variable-template-impact__item">
                <span>场景现有变量</span>
                <strong>{{ templateImpact.sceneVariableCount }}</strong>
              </div>
              <div class="variable-template-impact__item">
                <span>本次可新增</span>
                <strong>{{ templateImpact.insertableCount }}</strong>
              </div>
              <div class="variable-template-impact__item">
                <span>编码重叠</span>
                <strong>{{ templateImpact.overlapCount }}</strong>
              </div>
              <div class="variable-template-impact__item">
                <span>{{ templateForm.updateSupport ? '将覆盖更新' : '将跳过' }}</span>
                <strong>{{ templateForm.updateSupport ? templateImpact.updateCount : templateImpact.skippedCount }}</strong>
              </div>
            </div>
            <el-alert
              v-if="templateImpact.overlapCount"
              class="mt12"
              :type="templateForm.updateSupport ? 'warning' : 'info'"
              :closable="false"
              show-icon
              :title="templateForm.updateSupport ? '存在同编码变量，应用模板后会覆盖这些变量。' : '存在同编码变量，当前设置下会跳过这些变量。'"
              :description="templateImpact.skippedCodes.length ? `示例编码：${templateImpact.skippedCodes.join('、')}` : ''"
            />
            <el-alert
              v-if="templateImpact.truncated"
              class="mt12"
              type="warning"
              :closable="false"
              show-icon
              title="当前场景变量数量超过本页预演上限，影响预演按前 5000 条变量估算。"
            />
            <el-table :data="currentTemplate.items" size="small" class="mt12" max-height="360">
              <el-table-column prop="variableCode" label="变量编码" min-width="180" />
              <el-table-column prop="variableName" label="变量名称" min-width="140" />
              <el-table-column prop="sourceType" label="来源" width="100" />
              <el-table-column prop="dataType" label="数据类型" width="100" />
            </el-table>
          </template>
        </el-col>
      </el-row>
      <template #footer>
        <div class="dialog-footer">
          <el-button type="primary" :disabled="!templateForm.templateCode || !templateForm.sceneId" @click="submitTemplateApply">应用模板</el-button>
          <el-button @click="templateOpen = false">取 消</el-button>
        </div>
      </template>
    </el-dialog>

    <el-drawer v-model="detailOpen" title="变量详情" size="560px" append-to-body>
      <div v-if="detailInfo.variableId" class="variable-detail">
        <div class="variable-detail__header">
          <div class="variable-detail__title">{{ detailInfo.variableName }}</div>
          <div class="variable-detail__meta">{{ detailInfo.variableCode }} / {{ detailInfo.sceneCode }} / {{ detailInfo.sceneName }}</div>
        </div>
        <el-descriptions :column="1" border>
          <el-descriptions-item label="变量类型">{{ resolveDictLabel(variableTypeOptions, detailInfo.variableType) }}</el-descriptions-item>
          <el-descriptions-item label="来源类型">{{ resolveDictLabel(sourceTypeOptions, detailInfo.sourceType) }}</el-descriptions-item>
          <el-descriptions-item label="数据类型">{{ resolveDictLabel(dataTypeOptions, detailInfo.dataType) }}</el-descriptions-item>
          <el-descriptions-item label="默认值">{{ detailInfo.defaultValue || '-' }}</el-descriptions-item>
          <el-descriptions-item v-if="isPathSourceType(detailInfo.sourceType)" label="来源路径">{{ detailInfo.dataPath || '未配置，按变量编码平铺取值' }}</el-descriptions-item>
          <template v-if="detailInfo.sourceType === 'DICT'">
            <el-descriptions-item label="字典类型">{{ resolveDictTypeLabel(detailInfo.dictType) }}</el-descriptions-item>
          </template>
          <template v-if="detailInfo.sourceType === 'REMOTE'">
            <el-descriptions-item label="来源系统">{{ detailInfo.sourceSystem || '-' }}</el-descriptions-item>
            <el-descriptions-item label="第三方接口">{{ detailInfo.remoteApi || '-' }}</el-descriptions-item>
            <el-descriptions-item label="请求方式">{{ resolveRemoteOptionLabel(requestMethodOptions, detailInfo.requestMethod) }}</el-descriptions-item>
            <el-descriptions-item label="内容类型">{{ detailInfo.contentType || '-' }}</el-descriptions-item>
            <el-descriptions-item label="适配器类型">{{ resolveRemoteOptionLabel(adapterTypeOptions, detailInfo.adapterType) }}</el-descriptions-item>
            <el-descriptions-item label="鉴权方式">{{ resolveDictLabel(authTypeOptions, detailInfo.authType) }}</el-descriptions-item>
            <el-descriptions-item label="同步方式">{{ resolveDictLabel(syncModeOptions, detailInfo.syncMode) }}</el-descriptions-item>
            <el-descriptions-item label="缓存策略">{{ resolveDictLabel(cachePolicyOptions, detailInfo.cachePolicy) }}</el-descriptions-item>
            <el-descriptions-item label="失败兜底">{{ resolveDictLabel(fallbackPolicyOptions, detailInfo.fallbackPolicy) }}</el-descriptions-item>
            <el-descriptions-item v-if="hasDetailValue(detailInfo.queryConfigJson)" label="查询参数JSON"><JsonEditor :model-value="formatJson(detailInfo.queryConfigJson)" title="查询参数 JSON" readonly :rows="4" /></el-descriptions-item>
            <el-descriptions-item v-if="hasDetailValue(detailInfo.requestHeadersJson)" label="请求头JSON"><JsonEditor :model-value="formatJson(detailInfo.requestHeadersJson)" title="请求头 JSON" readonly :rows="4" /></el-descriptions-item>
            <el-descriptions-item v-if="hasDetailValue(detailInfo.bodyTemplateJson)" label="请求体模板"><JsonEditor :model-value="formatJson(detailInfo.bodyTemplateJson)" title="请求体模板" readonly :rows="4" /></el-descriptions-item>
            <el-descriptions-item v-if="hasDetailValue(detailInfo.authConfigJson)" label="鉴权配置JSON"><JsonEditor :model-value="formatJson(detailInfo.authConfigJson)" title="鉴权配置 JSON" readonly :rows="4" /></el-descriptions-item>
            <el-descriptions-item v-if="hasDetailValue(detailInfo.responseConfigJson)" label="响应提取配置JSON"><JsonEditor :model-value="formatJson(detailInfo.responseConfigJson)" title="响应提取配置 JSON" readonly :rows="4" /></el-descriptions-item>
            <el-descriptions-item v-if="hasDetailValue(detailInfo.mappingConfigJson)" label="映射配置JSON"><JsonEditor :model-value="formatJson(detailInfo.mappingConfigJson)" title="映射配置 JSON" readonly :rows="4" /></el-descriptions-item>
            <el-descriptions-item v-if="hasDetailValue(detailInfo.pageConfigJson)" label="分页策略JSON"><JsonEditor :model-value="formatJson(detailInfo.pageConfigJson)" title="分页策略 JSON" readonly :rows="4" /></el-descriptions-item>
            <el-descriptions-item v-if="hasDetailValue(detailInfo.adapterConfigJson)" label="适配器配置JSON"><JsonEditor :model-value="formatJson(detailInfo.adapterConfigJson)" title="适配器配置 JSON" readonly :rows="4" /></el-descriptions-item>
          </template>
          <template v-if="detailInfo.sourceType === 'FORMULA'">
            <el-descriptions-item label="公式编码">{{ detailInfo.formulaCode || '-' }}</el-descriptions-item>
            <el-descriptions-item label="中文公式">{{ detailInfo.businessFormula || '-' }}</el-descriptions-item>
            <el-descriptions-item label="公式表达式">{{ detailInfo.formulaExpr || '-' }}</el-descriptions-item>
          </template>
          <el-descriptions-item label="备注">{{ detailInfo.remark || '-' }}</el-descriptions-item>
        </el-descriptions>
      </div>
    </el-drawer>

    <el-drawer v-model="governanceOpen" title="变量治理检查" size="720px" append-to-body>
      <div v-loading="governanceLoading" v-if="governanceInfo.variableId">
        <div class="variable-governance__summary">
          <div class="variable-governance__item">
            <span>允许删除</span>
            <strong>{{ governanceInfo.canDelete ? '是' : '否' }}</strong>
          </div>
          <div class="variable-governance__item">
            <span>允许停用</span>
            <strong>{{ governanceInfo.canDisable ? '是' : '否' }}</strong>
          </div>
          <div class="variable-governance__item">
            <span>当前状态</span>
            <strong>{{ resolveDictLabel(variableStatusOptions, governanceInfo.status) }}</strong>
          </div>
          <div class="variable-governance__item">
            <span>发布引用</span>
            <strong>{{ governanceInfo.publishedVersionCount }}</strong>
          </div>
        </div>
        <div class="variable-dependency-map">
          <div class="variable-dependency-map__root">
            <span>当前变量</span>
            <strong>{{ governanceInfo.variableName }}</strong>
            <small>{{ governanceInfo.variableCode }}</small>
          </div>
          <div class="variable-dependency-map__arrow">-&gt;</div>
          <div class="variable-dependency-map__nodes">
            <div v-for="node in variableDependencyNodes" :key="node.key" class="variable-dependency-map__node" :class="{ 'is-empty': !node.count }">
              <div class="variable-dependency-map__node-head">
                <span>{{ node.label }}</span>
                <strong>{{ node.count }}</strong>
              </div>
              <p>{{ node.desc }}</p>
              <ul v-if="node.examples.length">
                <li v-for="example in node.examples" :key="example">{{ example }}</li>
              </ul>
              <small v-else>暂无引用样例</small>
            </div>
          </div>
        </div>
        <el-descriptions :column="1" border>
          <el-descriptions-item label="变量">{{ governanceInfo.variableCode }} / {{ governanceInfo.variableName }}</el-descriptions-item>
          <el-descriptions-item label="所属场景">{{ governanceInfo.sceneCode }} / {{ governanceInfo.sceneName }}</el-descriptions-item>
          <el-descriptions-item label="费用关系引用">{{ governanceInfo.feeRelCount }}</el-descriptions-item>
          <el-descriptions-item label="规则条件引用">{{ governanceInfo.ruleConditionCount }}</el-descriptions-item>
          <el-descriptions-item label="规则计量引用">{{ governanceInfo.ruleQuantityCount }}</el-descriptions-item>
          <el-descriptions-item label="公式表达式引用">{{ governanceInfo.formulaRefCount }}</el-descriptions-item>
          <el-descriptions-item label="发布版本引用">{{ governanceInfo.publishedVersionCount }}</el-descriptions-item>
        </el-descriptions>
        <div v-if="governanceInfo.sourceType === 'FORMULA'" class="variable-formula-tree">
          <div class="variable-formula-tree__head">
            <div>
              <strong>公式输入依赖树</strong>
              <span>{{ governanceInfo.formulaName || governanceInfo.formulaCode || '未绑定公式' }}</span>
            </div>
            <el-tag type="info" effect="light">FORMULA</el-tag>
          </div>
          <el-tree
            v-if="hasFormulaDependencyTree"
            :data="governanceInfo.formulaDependencies"
            :props="formulaTreeProps"
            node-key="nodeKey"
            default-expand-all
            class="variable-formula-tree__tree"
          >
            <template #default="{ data }">
              <div class="variable-formula-tree__node">
                <span>{{ data.label }}</span>
                <el-tag v-if="data.missing" size="small" type="danger">缺失</el-tag>
                <el-tag v-else-if="data.circular" size="small" type="warning">循环</el-tag>
                <el-tag v-else size="small" effect="plain">{{ resolveDictLabel(sourceTypeOptions, data.sourceType) }}</el-tag>
              </div>
            </template>
          </el-tree>
          <el-empty v-else description="当前公式没有读取其他变量" :image-size="70" />
        </div>
        <el-alert :title="governanceInfo.canDelete ? '允许删除' : '当前不允许删除'" :description="governanceInfo.removeBlockingReason" :type="governanceInfo.canDelete ? 'success' : 'warning'" :closable="false" show-icon class="mt12" />
        <el-alert :title="governanceInfo.canDisable ? '允许停用' : '当前不允许停用'" :description="governanceInfo.disableBlockingReason" :type="governanceInfo.canDisable ? 'success' : 'warning'" :closable="false" show-icon class="mt12" />
        <el-alert title="删除建议" :description="governanceInfo.removeAdvice" type="info" :closable="false" show-icon class="mt12" />
        <el-alert title="停用建议" :description="governanceInfo.disableAdvice" type="info" :closable="false" show-icon class="mt12" />
        <GovernanceImpactList :impacts="governanceInfo.impactItems" :context="governanceInfo" />
      </div>
    </el-drawer>
  </div>
</template>

<script setup name="CostVariable">
import { computed, getCurrentInstance, reactive, ref, toRefs, watch } from 'vue'
import { UploadFilled } from '@element-plus/icons-vue'
import GovernanceImpactList from '@/components/cost/GovernanceImpactList.vue'
import JsonEditor from '@/components/cost/JsonEditor.vue'
import { optionselectFormula } from '@/api/cost/formula'
import { optionselectScene } from '@/api/cost/scene'
import { optionselect as getDictTypeOptionselect } from '@/api/system/dict/type'
import { COST_MENU_ROUTES } from '@/utils/costMenuRoutes'
import { confirmCostDeleteImpact, confirmCostDisableImpact, findFirstDeleteBlockedCheck, findFirstDisableBlockedCheck } from '@/utils/costGovernanceDeletePreview'
import {
  addVariable,
  applyVariableTemplate,
  copyVariable,
  delVariable,
  getVariable,
  getVariableGovernance,
  getVariableStats,
  importVariableData,
  listVariable,
  listVariableTemplates,
  previewVariableImport,
  previewVariableRemote,
  refreshVariableRemote,
  testVariableRemote,
  updateVariable
} from '@/api/cost/variable'
import {
  addVariableGroup,
  delVariableGroup,
  listVariableGroup,
  optionselectVariableGroup,
  updateVariableGroup
} from '@/api/cost/variableGroup'
import useSettingsStore from '@/store/modules/settings'
import { resolveWorkingCostSceneId } from '@/utils/costSceneContext'
import { useCostWorkSceneAutoRefresh } from '@/utils/costWorkSceneAutoRefresh'
import { getRemoteDictOptionMap } from '@/utils/dictRemote'
import { parseJsonText, safeFormatJson } from '@/utils/jsonTools'

const { proxy } = getCurrentInstance()
const settingsStore = useSettingsStore()
const isCompactMode = computed(() => settingsStore.costPageMode === 'COMPACT')
const REMOTE_TEST_STATUS_KEY = 'cost_variable_remote_test_status'
const loading = ref(true)
const showSearch = ref(true)
const open = ref(false)
const title = ref('')
const total = ref(0)
const ids = ref([])
const single = ref(true)
const multiple = ref(true)
const initialStatus = ref(undefined)
const remoteTestStatusMap = ref(loadRemoteTestStatusMap())

const variableList = ref([])
const sceneOptions = ref([])
const groupOptions = ref([])
const formGroupOptions = ref([])
const copyGroupOptions = ref([])
const templateGroupOptions = ref([])
const templateList = ref([])
const variableTypeOptions = ref([])
const sourceTypeOptions = ref([])
const dataTypeOptions = ref([])
const variableStatusOptions = ref([])
const authTypeOptions = ref([])
const syncModeOptions = ref([])
const cachePolicyOptions = ref([])
const fallbackPolicyOptions = ref([])
const formulaOptions = ref([])
const dictTypeOptions = ref([])
const groupStatusOptions = ref([])
const groupOpen = ref(false)
const groupLoading = ref(false)
const groupList = ref([])
const groupFormOpen = ref(false)
const groupTitle = ref('')
const requestMethodOptions = [
  { label: 'GET', value: 'GET' },
  { label: 'POST', value: 'POST' },
  { label: 'PUT', value: 'PUT' },
  { label: 'DELETE', value: 'DELETE' }
]
const contentTypeOptions = [
  { label: 'application/json', value: 'application/json' },
  { label: 'application/x-www-form-urlencoded', value: 'application/x-www-form-urlencoded' },
  { label: 'text/plain', value: 'text/plain' }
]
const adapterTypeOptions = [
  { label: '标准对象', value: 'STANDARD' },
  { label: '根节点数组', value: 'ROOT_ARRAY' },
  { label: '分页包装对象', value: 'PAGE_ENVELOPE' },
  { label: '单对象响应', value: 'SINGLE_OBJECT' }
]
const sourceGuideItems = [
  {
    value: 'INPUT',
    label: 'INPUT',
    tag: 'success',
    title: '业务输入',
    desc: '承接任务入参或导入批次字段',
    pathHint: '用于声明变量从输入 JSON 的哪个路径取值；为空时按变量编码读取平铺输入。'
  },
  {
    value: 'DICT',
    label: 'DICT',
    tag: 'info',
    title: '字典取值',
    desc: '先定位输入，再用字典限定值域',
    pathHint: '先声明输入路径，再绑定字典类型；字典只负责约束值域，不代表取值位置。'
  },
  {
    value: 'REMOTE',
    label: 'REMOTE',
    tag: 'warning',
    title: '远程接口',
    desc: '从第三方系统测试、预览并映射',
    pathHint: '远程变量可声明映射后的业务路径；接口请求、鉴权、分页和映射在下方远程接入区维护。'
  },
  {
    value: 'FORMULA',
    label: 'FORMULA',
    tag: 'info',
    title: '公式变量',
    desc: '绑定公式实验室资产后参与运行',
    pathHint: '公式变量不需要来源路径，运行时由公式实验室表达式计算生成。'
  }
]

const governanceOpen = ref(false)
const governanceLoading = ref(false)
const governanceInfo = ref({})
const detailOpen = ref(false)
const detailInfo = ref({})
const testOpen = ref(false)
const testResult = ref(null)
const previewOpen = ref(false)
const previewResult = ref(null)
const importOpen = ref(false)
const importFile = ref(null)
const importPreview = ref({ totalRows: 0, passRows: 0, failRows: 0, importable: false, previewRows: [], issues: [] })
const copyOpen = ref(false)
const copySource = ref({})
const templateOpen = ref(false)
const templateImpactLoading = ref(false)
const templateImpact = ref({
  sceneVariableCount: 0,
  templateVariableCount: 0,
  insertableCount: 0,
  overlapCount: 0,
  updateCount: 0,
  skippedCount: 0,
  skippedCodes: [],
  truncated: false
})
const lastTemplateApplyResult = ref({})
const statistics = reactive({ variableCount: 0, enabledVariableCount: 0, remoteVariableCount: 0, formulaVariableCount: 0 })
const selectedFormulaMeta = computed(() => formulaOptions.value.find(item => item.formulaCode === form.value.formulaCode) || {})
const formulaTreeProps = { label: 'label', children: 'children' }
const variableDependencyNodes = computed(() => {
  const info = governanceInfo.value || {}
  return [
    {
      key: 'fee',
      label: '费用契约',
      count: Number(info.feeRelCount || 0),
      desc: '费用输入契约中使用该变量',
      examples: collectImpactExamples(['VARIABLE_FEE_CONTRACT'])
    },
    {
      key: 'rule',
      label: '规则引用',
      count: Number(info.ruleConditionCount || 0) + Number(info.ruleQuantityCount || 0),
      desc: `条件 ${Number(info.ruleConditionCount || 0)} 条，计量字段 ${Number(info.ruleQuantityCount || 0)} 条`,
      examples: collectImpactExamples(['VARIABLE_RULE_CONDITION', 'VARIABLE_RULE_QUANTITY'])
    },
    {
      key: 'formula',
      label: '公式引用',
      count: Number(info.formulaRefCount || 0),
      desc: '公式表达式中读取该变量作为输入',
      examples: collectImpactExamples(['VARIABLE_FORMULA_REF'])
    },
    {
      key: 'publish',
      label: '发布版本',
      count: Number(info.publishedVersionCount || 0),
      desc: '已发布快照中保留该变量定义',
      examples: collectImpactExamples(['VARIABLE_PUBLISH_SNAPSHOT'])
    }
  ]
})
const hasFormulaDependencyTree = computed(() => Array.isArray(governanceInfo.value?.formulaDependencies) && governanceInfo.value.formulaDependencies.length > 0)
const dictTypeOptionGroups = computed(() => {
  const groups = [
    {
      label: '核算字典',
      items: dictTypeOptions.value.filter(item => item.status === '0' && item.dictType?.startsWith('cost_'))
    },
    {
      label: '系统字典',
      items: dictTypeOptions.value.filter(item => item.status === '0' && !item.dictType?.startsWith('cost_'))
    },
    {
      label: '已停用字典',
      items: dictTypeOptions.value.filter(item => item.status && item.status !== '0')
    }
  ]
  return groups.filter(group => group.items.length)
})

function collectImpactExamples(types) {
  const impacts = Array.isArray(governanceInfo.value?.impactItems) ? governanceInfo.value.impactItems : []
  return impacts
    .filter(item => types.includes(item.impactType))
    .flatMap(item => Array.isArray(item.examples) ? item.examples : [])
    .slice(0, 3)
}

const validateDictType = (_rule, value, callback) => {
  if (data.form.sourceType === 'DICT' && !value) {
    callback(new Error('字典类型不能为空'))
    return
  }
  callback()
}

const validateRemoteRequestMethod = (_rule, value, callback) => {
  if (data.form.sourceType === 'REMOTE' && !value) {
    callback(new Error('请求方式不能为空'))
    return
  }
  callback()
}

const validateRemoteJson = fieldLabel => (_rule, value, callback) => {
  if (data.form.sourceType !== 'REMOTE' || !value) {
    callback()
    return
  }
  const result = parseJsonText(value)
  if (result.valid) {
    callback()
    return
  }
  callback(new Error(`${fieldLabel}格式不合法，请输入有效 JSON`))
}

const data = reactive({
  queryParams: { pageNum: 1, pageSize: 10, sceneId: undefined, groupId: undefined, variableCode: undefined, variableName: undefined, sourceType: undefined, sourceSystem: undefined },
  form: {},
  groupQuery: { sceneId: undefined, keyword: undefined, pageNum: 1, pageSize: 1000 },
  groupForm: { groupId: undefined, sceneId: undefined, groupCode: undefined, groupName: undefined, sortNo: 10, status: '0', remark: undefined },
  copyForm: { variableId: undefined, targetSceneId: undefined, targetGroupId: undefined, variableCode: undefined, variableName: undefined },
  templateForm: { sceneId: undefined, groupId: undefined, templateCode: undefined, updateSupport: false },
  importForm: { updateSupport: false },
  rules: {
    sceneId: [{ required: true, message: '所属场景不能为空', trigger: 'change' }],
    variableCode: [{ required: true, message: '变量编码不能为空', trigger: 'blur' }],
    variableName: [{ required: true, message: '变量名称不能为空', trigger: 'blur' }],
    variableType: [{ required: true, message: '变量类型不能为空', trigger: 'change' }],
    sourceType: [{ required: true, message: '来源类型不能为空', trigger: 'change' }],
    requestMethod: [{ validator: validateRemoteRequestMethod, trigger: 'change' }],
    dictType: [{ validator: validateDictType, trigger: 'change' }],
    queryConfigJson: [{ validator: validateRemoteJson('查询参数JSON'), trigger: 'blur' }],
    requestHeadersJson: [{ validator: validateRemoteJson('请求头JSON'), trigger: 'blur' }],
    authConfigJson: [{ validator: validateRemoteJson('鉴权配置JSON'), trigger: 'blur' }],
    responseConfigJson: [{ validator: validateRemoteJson('响应提取配置JSON'), trigger: 'blur' }],
    mappingConfigJson: [{ validator: validateRemoteJson('映射配置JSON'), trigger: 'blur' }],
    pageConfigJson: [{ validator: validateRemoteJson('分页策略JSON'), trigger: 'blur' }],
    adapterConfigJson: [{ validator: validateRemoteJson('适配器配置JSON'), trigger: 'blur' }],
    status: [{ required: true, message: '状态不能为空', trigger: 'change' }]
  },
  groupRules: {
    sceneId: [{ required: true, message: '所属场景不能为空', trigger: 'change' }],
    groupCode: [{ required: true, message: '分组编码不能为空', trigger: 'blur' }],
    groupName: [{ required: true, message: '分组名称不能为空', trigger: 'blur' }],
    status: [{ required: true, message: '状态不能为空', trigger: 'change' }]
  },
  copyRules: {
    targetSceneId: [{ required: true, message: '目标场景不能为空', trigger: 'change' }],
    variableCode: [{ required: true, message: '新变量编码不能为空', trigger: 'blur' }],
    variableName: [{ required: true, message: '新变量名称不能为空', trigger: 'blur' }]
  }
})
const { queryParams, form, groupQuery, groupForm, copyForm, templateForm, importForm, rules, groupRules, copyRules } = toRefs(data)
const requestBodyLang = computed(() => String(form.value.contentType || '').toLowerCase().includes('json') ? 'json' : 'text')
const currentSourceGuide = computed(() => sourceGuideItems.find(item => item.value === form.value.sourceType) || sourceGuideItems[0])

const metricItems = computed(() => [
  { label: '变量总数', value: statistics.variableCount, desc: '当前筛选条件下的变量规模' },
  { label: '启用变量数', value: statistics.enabledVariableCount, desc: '状态为正常的变量数量' },
  { label: '第三方变量数', value: statistics.remoteVariableCount, desc: '来源为第三方接口的变量数量' },
  { label: '公式变量数', value: statistics.formulaVariableCount, desc: '来源为公式派生的变量数量' }
])

const topTemplates = computed(() => templateList.value.slice(0, 4))
const templateVariableCount = computed(() => templateList.value.reduce((total, item) => {
  return total + Number(item.variableCount || item.items?.length || 0)
}, 0))
const reusedTemplateCount = computed(() => templateList.value.filter(item => Number(item.appliedSceneCount || 0) > 0).length)
const templateApplyMatchesCurrent = computed(() => {
  return templateOpen.value
    && lastTemplateApplyResult.value?.templateCode
    && lastTemplateApplyResult.value.templateCode === templateForm.value.templateCode
    && lastTemplateApplyResult.value.sceneId === templateForm.value.sceneId
})
const templateKeyword = ref('')
const templateSourceTypeFilter = ref(undefined)
const templateSourceTypeOptions = computed(() => {
  const values = new Set()
  templateList.value.forEach(template => {
    ;(template.items || []).forEach(item => {
      if (item?.sourceType) {
        values.add(item.sourceType)
      }
    })
  })
  return sourceTypeOptions.value.filter(item => values.has(item.value))
})
const filteredTemplateList = computed(() => {
  const keyword = templateKeyword.value?.trim()?.toLowerCase()
  return templateList.value.filter(item => {
    const matchesKeyword = !keyword || [item.templateCode, item.templateName, item.description]
      .filter(Boolean)
      .some(value => String(value).toLowerCase().includes(keyword))
    const matchesSource = !templateSourceTypeFilter.value || (item.items || []).some(variable => variable.sourceType === templateSourceTypeFilter.value)
    return matchesKeyword && matchesSource
  })
})

const currentTemplate = computed(() => templateList.value.find(item => item.templateCode === templateForm.value.templateCode))
const currentTemplateMetrics = computed(() => {
  const items = currentTemplate.value?.items || []
  return {
    total: items.length,
    input: items.filter(item => item.sourceType === 'INPUT').length,
    dict: items.filter(item => item.sourceType === 'DICT').length,
    formula: items.filter(item => item.sourceType === 'FORMULA').length
  }
})

function resetTemplateImpact() {
  templateImpact.value = {
    sceneVariableCount: 0,
    templateVariableCount: currentTemplate.value?.items?.length || 0,
    insertableCount: 0,
    overlapCount: 0,
    updateCount: 0,
    skippedCount: 0,
    skippedCodes: [],
    truncated: false
  }
}

async function refreshTemplateImpact() {
  if (!templateOpen.value || !templateForm.value.sceneId || !currentTemplate.value) {
    resetTemplateImpact()
    return
  }
  templateImpactLoading.value = true
  try {
    const response = await listVariable({ sceneId: templateForm.value.sceneId, pageNum: 1, pageSize: 5000 })
    const rows = response?.rows || []
    const totalCount = Number(response?.total || rows.length)
    const existingCodes = new Set(rows.map(item => item.variableCode))
    const templateItems = currentTemplate.value?.items || []
    const overlapItems = templateItems.filter(item => existingCodes.has(item.variableCode))
    const insertableItems = templateItems.filter(item => !existingCodes.has(item.variableCode))
    templateImpact.value = {
      sceneVariableCount: totalCount,
      templateVariableCount: templateItems.length,
      insertableCount: insertableItems.length,
      overlapCount: overlapItems.length,
      updateCount: templateForm.value.updateSupport ? overlapItems.length : 0,
      skippedCount: templateForm.value.updateSupport ? 0 : overlapItems.length,
      skippedCodes: overlapItems.slice(0, 8).map(item => item.variableCode),
      truncated: totalCount > rows.length
    }
  } finally {
    templateImpactLoading.value = false
  }
}

async function loadBaseOptions() {
  const dictTypeResponsePromise = dictTypeOptions.value.length
    ? Promise.resolve({ data: dictTypeOptions.value })
    : getDictTypeOptionselect()
  const [dictMap, sceneResponse, templateResponse, dictTypeResponse] = await Promise.all([
    getRemoteDictOptionMap([
      'cost_variable_type',
      'cost_variable_source_type',
      'cost_variable_data_type',
      'cost_variable_status',
      'cost_variable_auth_type',
      'cost_variable_sync_mode',
      'cost_variable_cache_policy',
      'cost_variable_fallback_policy',
      'cost_variable_group_status'
    ]),
    optionselectScene({ status: '0', pageNum: 1, pageSize: 1000 }),
    listVariableTemplates(),
    dictTypeResponsePromise
  ])
  variableTypeOptions.value = dictMap.cost_variable_type || []
  sourceTypeOptions.value = dictMap.cost_variable_source_type || []
  dataTypeOptions.value = dictMap.cost_variable_data_type || []
  variableStatusOptions.value = dictMap.cost_variable_status || []
  authTypeOptions.value = dictMap.cost_variable_auth_type || []
  syncModeOptions.value = dictMap.cost_variable_sync_mode || []
  cachePolicyOptions.value = dictMap.cost_variable_cache_policy || []
  fallbackPolicyOptions.value = dictMap.cost_variable_fallback_policy || []
  groupStatusOptions.value = dictMap.cost_variable_group_status || variableStatusOptions.value
  sceneOptions.value = sceneResponse?.data || []
  templateList.value = templateResponse?.data || []
  dictTypeOptions.value = (dictTypeResponse?.data || []).slice().sort((a, b) => {
    const score = item => {
      if (item.status !== '0') return 2
      return item.dictType?.startsWith('cost_') ? 0 : 1
    }
    return score(a) - score(b) || (a.dictName || '').localeCompare(b.dictName || '')
  })
  const preferredSceneId = resolveWorkingCostSceneId(sceneOptions.value, queryParams.value.sceneId)
  queryParams.value.sceneId = preferredSceneId
  templateForm.value.sceneId = resolveWorkingCostSceneId(sceneOptions.value, templateForm.value.sceneId, preferredSceneId)
}

async function loadGroups(sceneId) {
  if (!sceneId) return []
  const response = await optionselectVariableGroup({ sceneId, status: '0', pageNum: 1, pageSize: 1000 })
  return response?.data || []
}

async function refreshGroupList() {
  groupLoading.value = true
  try {
    const response = await listVariableGroup(groupQuery.value)
    groupList.value = response?.rows || []
  } finally {
    groupLoading.value = false
  }
}

function resetGroupFormModel() {
  groupForm.value = {
    groupId: undefined,
    sceneId: groupQuery.value.sceneId || queryParams.value.sceneId || resolveWorkingCostSceneId(sceneOptions.value),
    groupCode: undefined,
    groupName: undefined,
    sortNo: 10,
    status: '0',
    remark: undefined
  }
  proxy.resetForm('groupRef')
}

async function handleGroupCenter() {
  await loadBaseOptions()
  groupQuery.value.sceneId = queryParams.value.sceneId || resolveWorkingCostSceneId(sceneOptions.value)
  groupQuery.value.keyword = undefined
  groupOpen.value = true
  await refreshGroupList()
}

function handleAddGroup() {
  resetGroupFormModel()
  groupTitle.value = '新增变量组'
  groupFormOpen.value = true
}

function handleUpdateGroup(row) {
  groupForm.value = {
    groupId: row.groupId,
    sceneId: row.sceneId,
    groupCode: row.groupCode,
    groupName: row.groupName,
    sortNo: row.sortNo ?? 10,
    status: row.status || '0',
    remark: row.remark
  }
  groupTitle.value = '修改变量组'
  groupFormOpen.value = true
}

function submitGroupForm() {
  proxy.$refs.groupRef.validate(async valid => {
    if (!valid) return
    const req = groupForm.value.groupId ? updateVariableGroup(groupForm.value) : addVariableGroup(groupForm.value)
    await req
    proxy.$modal.msgSuccess(groupForm.value.groupId ? '修改成功' : '新增成功')
    groupFormOpen.value = false
    await refreshGroupList()
    groupOptions.value = await loadGroups(queryParams.value.sceneId)
  })
}

async function handleDeleteGroup(row) {
  await proxy.$modal.confirm(`确认删除变量组“${row.groupName || row.groupCode}”吗？`)
  await delVariableGroup(row.groupId)
  proxy.$modal.msgSuccess('删除成功')
  await refreshGroupList()
  groupOptions.value = await loadGroups(queryParams.value.sceneId)
  if (queryParams.value.groupId === row.groupId) {
    queryParams.value.groupId = undefined
  }
}

async function getList() {
  loading.value = true
  try {
    await loadBaseOptions()
    const [rows, statsResponse] = await Promise.all([listVariable(queryParams.value), getVariableStats(queryParams.value)])
    variableList.value = (rows.rows || []).map(withRemoteTestStatus)
    total.value = rows.total
    groupOptions.value = await loadGroups(queryParams.value.sceneId)
    Object.assign(statistics, {
      variableCount: Number(statsResponse.data?.variableCount || 0),
      enabledVariableCount: Number(statsResponse.data?.enabledVariableCount || 0),
      remoteVariableCount: Number(statsResponse.data?.remoteVariableCount || 0),
      formulaVariableCount: Number(statsResponse.data?.formulaVariableCount || 0)
    })
  } finally {
    loading.value = false
  }
}

async function handleSceneChange(sceneId = queryParams.value.sceneId) {
  queryParams.value.sceneId = sceneId
  queryParams.value.groupId = undefined
  groupOptions.value = await loadGroups(queryParams.value.sceneId)
}

useCostWorkSceneAutoRefresh({
  queryParams,
  sceneOptions,
  beforeRefresh: async sceneId => {
    queryParams.value.groupId = undefined
    groupQuery.value.sceneId = sceneId
    groupOptions.value = await loadGroups(sceneId)
  },
  refresh: getList
})

async function loadFormulaOptions(sceneId) {
  if (!sceneId) {
    formulaOptions.value = []
    return
  }
  const response = await optionselectFormula({ sceneId, status: '0', pageNum: 1, pageSize: 1000 })
  formulaOptions.value = response?.data || []
}

watch(() => form.value.formulaCode, value => {
  if (form.value.sourceType !== 'FORMULA') {
    return
  }
  const meta = formulaOptions.value.find(item => item.formulaCode === value)
  form.value.formulaExpr = meta?.formulaExpr || ''
})

async function loadFormGroups() {
  form.value.groupId = undefined
  const [groups] = await Promise.all([
    loadGroups(form.value.sceneId),
    loadFormulaOptions(form.value.sceneId)
  ])
  formGroupOptions.value = groups
}

async function loadCopyGroups() {
  copyGroupOptions.value = await loadGroups(copyForm.value.targetSceneId)
  if (!copyGroupOptions.value.find(item => item.groupId === copyForm.value.targetGroupId)) {
    copyForm.value.targetGroupId = undefined
  }
}

async function loadTemplateGroups() {
  templateGroupOptions.value = await loadGroups(templateForm.value.sceneId)
  if (!templateGroupOptions.value.find(item => item.groupId === templateForm.value.groupId)) {
    templateForm.value.groupId = undefined
  }
}

watch(
  [() => templateOpen.value, () => templateForm.value.sceneId, () => templateForm.value.templateCode, () => templateForm.value.updateSupport],
  () => {
    refreshTemplateImpact()
  }
)

function resetFormModel() {
  form.value = {
    variableId: undefined,
    sceneId: undefined,
    groupId: undefined,
    variableCode: undefined,
    variableName: undefined,
    variableType: 'TEXT',
    sourceType: 'INPUT',
    sourceSystem: undefined,
    dictType: undefined,
    remoteApi: undefined,
    requestMethod: 'GET',
    contentType: 'application/json',
    queryConfigJson: undefined,
    requestHeadersJson: undefined,
    bodyTemplateJson: undefined,
    authType: 'NONE',
    authConfigJson: undefined,
    dataPath: undefined,
    responseConfigJson: undefined,
    mappingConfigJson: undefined,
    pageConfigJson: undefined,
    adapterType: 'STANDARD',
    adapterConfigJson: undefined,
    syncMode: 'REALTIME',
    cachePolicy: 'MANUAL_REFRESH',
    fallbackPolicy: 'FAIL_FAST',
    formulaCode: undefined,
    formulaExpr: undefined,
    dataType: 'STRING',
    defaultValue: undefined,
    status: '0',
    precisionScale: 2,
    sortNo: 10,
    remark: undefined
  }
  initialStatus.value = undefined
  proxy.resetForm('variableRef')
}

function resetImportState() {
  importFile.value = null
  importPreview.value = { totalRows: 0, passRows: 0, failRows: 0, importable: false, previewRows: [], issues: [] }
  importForm.value.updateSupport = false
}

function handleSelectionChange(selection) {
  ids.value = selection.map(item => item.variableId)
  single.value = selection.length !== 1
  multiple.value = !selection.length
}

function handleVariableRowCommand(command, row) {
  const handlers = {
    governance: handleGovernance,
    copy: handleCopy,
    delete: handleDelete
  }
  handlers[command]?.(row)
}

function handleQuery() {
  queryParams.value.pageNum = 1
  getList()
}

function resetQuery() {
  proxy.resetForm('queryRef')
  queryParams.value.pageNum = 1
  queryParams.value.pageSize = 10
  queryParams.value.groupId = undefined
  handleQuery()
}

async function handleAdd() {
  await loadBaseOptions()
  resetFormModel()
  form.value.sceneId = queryParams.value.sceneId || resolveWorkingCostSceneId(sceneOptions.value)
  if (form.value.sceneId) {
    await loadFormGroups()
  }
  open.value = true
  title.value = '新增变量'
}

async function handleUpdate(row) {
  await loadBaseOptions()
  resetFormModel()
  const response = await getVariable(row?.variableId || ids.value[0])
  form.value = { ...form.value, ...response.data }
  initialStatus.value = response.data?.status
  formGroupOptions.value = await loadGroups(form.value.sceneId)
  await loadFormulaOptions(form.value.sceneId)
  open.value = true
  title.value = '修改变量'
}

async function handleDetail(row) {
  const response = await getVariable(row.variableId)
  detailInfo.value = response.data || {}
  detailOpen.value = true
}

function cancel() {
  open.value = false
  resetFormModel()
}

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
  const allowed = await confirmCostDeleteImpact({
    checks,
    targetLabel: '变量',
    targetNames: targetRows.map(item => item.variableName)
  })
  if (!allowed) {
    const blockedCheck = findFirstDeleteBlockedCheck(checks)
    if (blockedCheck) {
      governanceInfo.value = blockedCheck
      governanceOpen.value = true
    }
    return
  }
  const variableIds = row?.variableId || ids.value
  await delVariable(variableIds)
  getList()
  proxy.$modal.msgSuccess('删除成功')
}

function handleExport() {
  proxy.download('cost/variable/export', { ...queryParams.value }, `variable_${new Date().getTime()}.xlsx`)
}

async function fetchGovernance(variableId) {
  const response = await getVariableGovernance(variableId)
  return normalizeGovernanceInfo(response.data || {})
}

function normalizeGovernanceInfo(data = {}) {
  return {
    ...data,
    formulaDependencies: normalizeFormulaDependencyNodes(data.formulaDependencies)
  }
}

function normalizeFormulaDependencyNodes(nodes = [], path = 'dep') {
  if (!Array.isArray(nodes)) {
    return []
  }
  return nodes.map((item, index) => {
    const nodePath = `${path}-${index}`
    const code = item.variableCode || `MISSING_${index}`
    return {
      ...item,
      nodeKey: `${nodePath}-${code}`,
      label: formatFormulaDependencyLabel(item),
      children: normalizeFormulaDependencyNodes(item.children, nodePath)
    }
  })
}

function formatFormulaDependencyLabel(item = {}) {
  const name = item.variableName || '未维护变量'
  const code = item.variableCode || '-'
  const formula = item.formulaCode ? ` / 公式 ${item.formulaCode}` : ''
  return `${name}（${code}）${formula}`
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
  const checks = [check]
  const allowed = await confirmCostDisableImpact({
    checks,
    targetLabel: '变量',
    targetNames: [form.value.variableName || check.variableName]
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

function currentRow() {
  return variableList.value.find(item => item.variableId === ids.value[0])
}

function buildRemoteRequestPayload(row) {
  return {
    variableId: row.variableId,
    variableCode: row.variableCode,
    sourceSystem: row.sourceSystem,
    remoteApi: row.remoteApi,
    requestMethod: row.requestMethod,
    contentType: row.contentType,
    queryConfigJson: row.queryConfigJson,
    requestHeadersJson: row.requestHeadersJson,
    bodyTemplateJson: row.bodyTemplateJson,
    authType: row.authType,
    authConfigJson: row.authConfigJson,
    dataPath: row.dataPath,
    responseConfigJson: row.responseConfigJson,
    mappingConfigJson: row.mappingConfigJson,
    pageConfigJson: row.pageConfigJson,
    adapterType: row.adapterType,
    adapterConfigJson: row.adapterConfigJson,
    syncMode: row.syncMode,
    cachePolicy: row.cachePolicy,
    fallbackPolicy: row.fallbackPolicy
  }
}

function loadRemoteTestStatusMap() {
  try {
    const raw = localStorage.getItem(REMOTE_TEST_STATUS_KEY)
    return raw ? JSON.parse(raw) : {}
  } catch {
    return {}
  }
}

function saveRemoteTestStatusMap() {
  try {
    localStorage.setItem(REMOTE_TEST_STATUS_KEY, JSON.stringify(remoteTestStatusMap.value))
  } catch {
    // 忽略本地存储限制，列表仍会展示本次会话内的测试结果。
  }
}

function remoteStatusKey(row = {}) {
  return String(row.variableId || `${row.sceneId || ''}:${row.variableCode || ''}`)
}

function withRemoteTestStatus(row = {}) {
  return {
    ...row,
    remoteTestStatus: remoteTestStatusMap.value[remoteStatusKey(row)]
  }
}

function rememberRemoteTestStatus(row = {}, result = {}) {
  const key = remoteStatusKey(row)
  if (!key || key === ':') {
    return
  }
  remoteTestStatusMap.value = {
    ...remoteTestStatusMap.value,
    [key]: {
      success: Boolean(result.success),
      message: result.message || '',
      diagnosticMessage: result.diagnosticMessage || '',
      failureStage: result.failureStage || '',
      statusCode: result.statusCode,
      elapsedMs: result.elapsedMs,
      testedAt: new Date().toISOString()
    }
  }
  saveRemoteTestStatusMap()
  variableList.value = variableList.value.map(item => remoteStatusKey(item) === key ? withRemoteTestStatus(item) : item)
}

function resolveRemoteStatusTag(status) {
  if (!status) return 'info'
  return status.success ? 'success' : 'danger'
}

function resolveRemoteStatusText(status) {
  if (!status) return '未测试'
  return status.success ? '最近通过' : '最近失败'
}

function resolveRemoteStatusReason(status) {
  if (!status) return '请先测试接口'
  if (status.success) {
    return status.elapsedMs != null ? `耗时 ${status.elapsedMs} ms` : (status.message || '接口连通')
  }
  return status.diagnosticMessage || status.message || status.failureStage || '接口测试失败'
}

async function handleTestRemote() {
  const row = currentRow()
  if (!row) return
  const response = await testVariableRemote(buildRemoteRequestPayload(row))
  testResult.value = response.data
  rememberRemoteTestStatus(row, response.data)
  testOpen.value = true
}

async function handlePreviewRemote() {
  const row = currentRow()
  if (!row) return
  const response = await previewVariableRemote(buildRemoteRequestPayload(row))
  previewResult.value = response.data
  previewOpen.value = true
}

async function validateRemoteFormBeforeInvoke() {
  await proxy.$refs.variableRef.validateField([
    'sourceSystem',
    'remoteApi',
    'requestMethod',
    'authConfigJson',
    'queryConfigJson',
    'requestHeadersJson',
    'bodyTemplateJson',
    'responseConfigJson',
    'mappingConfigJson',
    'pageConfigJson',
    'adapterConfigJson'
  ])
}

async function handleTestRemoteDraft() {
  await validateRemoteFormBeforeInvoke()
  const response = await testVariableRemote(buildRemoteRequestPayload(form.value))
  testResult.value = response.data
  rememberRemoteTestStatus(form.value, response.data)
  testOpen.value = true
}

async function handlePreviewRemoteDraft() {
  await validateRemoteFormBeforeInvoke()
  const response = await previewVariableRemote(buildRemoteRequestPayload(form.value))
  previewResult.value = response.data
  previewOpen.value = true
}

async function handleRefreshRemote() {
  const response = await refreshVariableRemote({ sceneId: queryParams.value.sceneId })
  const message = response.data?.message || '刷新状态已返回'
  if (response.data?.cacheRefreshSupported === false) {
    proxy.$modal.msgWarning(message)
    return
  }
  proxy.$modal.msgSuccess(message)
}

function handleImport() {
  resetImportState()
  importOpen.value = true
}

function handleImportFileChange(file) {
  importFile.value = file.raw
}

function handleImportFileRemove() {
  importFile.value = null
  importPreview.value = { totalRows: 0, passRows: 0, failRows: 0, importable: false, previewRows: [], issues: [] }
}

function buildImportFormData(includeUpdateSupport = false) {
  if (!importFile.value) {
    proxy.$modal.msgWarning('请先选择导入文件')
    return null
  }
  const formData = new FormData()
  formData.append('file', importFile.value)
  if (includeUpdateSupport) {
    formData.append('updateSupport', importForm.value.updateSupport)
  }
  return formData
}

async function submitImportPreview() {
  const formData = buildImportFormData(true)
  if (!formData) return
  const response = await previewVariableImport(formData)
  importPreview.value = response.data || importPreview.value
}

async function submitImportData() {
  const formData = buildImportFormData(true)
  if (!formData) return
  const response = await importVariableData(formData)
  importPreview.value = response.data || importPreview.value
  proxy.$modal.msgSuccess(`导入完成：通过 ${importPreview.value.passRows} 行，失败 ${importPreview.value.failRows} 行`)
  if (importPreview.value.failRows === 0) {
    importOpen.value = false
    resetImportState()
    getList()
  }
}

function downloadImportIssueReport() {
  const formData = buildImportFormData(true)
  if (!formData) return
  proxy.download('cost/variable/importIssueExport', formData, `variable_import_errors_${new Date().getTime()}.xlsx`, {
    transformRequest: [(data) => data],
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

function downloadImportTemplate() {
  proxy.download('cost/variable/importTemplate', {}, 'cost_variable_import_template.xlsx')
}

function resolveRemoteOptionLabel(options, value) {
  const target = options.find(item => item.value === value)
  return target?.label || value || '-'
}

async function handleCopy(row) {
  await loadBaseOptions()
  const target = row || currentRow()
  if (!target) return
  copySource.value = target
  copyForm.value = {
    variableId: target.variableId,
    targetSceneId: target.sceneId,
    targetGroupId: target.groupId,
    variableCode: `${target.variableCode}_COPY`,
    variableName: `${target.variableName}-复制`
  }
  copyGroupOptions.value = await loadGroups(copyForm.value.targetSceneId)
  copyOpen.value = true
}

function submitCopy() {
  proxy.$refs.copyRef.validate(async valid => {
    if (!valid) return
    await copyVariable(copyForm.value)
    proxy.$modal.msgSuccess('复制成功')
    copyOpen.value = false
    getList()
  })
}

async function handleTemplateCenter() {
  await loadBaseOptions()
  templateForm.value.sceneId = queryParams.value.sceneId || resolveWorkingCostSceneId(sceneOptions.value)
  templateForm.value.groupId = queryParams.value.groupId
  templateForm.value.templateCode = templateList.value[0]?.templateCode
  templateForm.value.updateSupport = false
  templateKeyword.value = ''
  templateSourceTypeFilter.value = undefined
  resetTemplateImpact()
  templateGroupOptions.value = await loadGroups(templateForm.value.sceneId)
  templateOpen.value = true
}

async function submitTemplateApply() {
  const response = await applyVariableTemplate(templateForm.value)
  const sceneMeta = sceneOptions.value.find(item => item.sceneId === templateForm.value.sceneId)
  const templateMeta = templateList.value.find(item => item.templateCode === templateForm.value.templateCode)
  const groupMeta = templateGroupOptions.value.find(item => item.groupId === templateForm.value.groupId)
  lastTemplateApplyResult.value = {
    ...response.data,
    sceneId: templateForm.value.sceneId,
    sceneLabel: sceneMeta ? `${sceneMeta.sceneCode} / ${sceneMeta.sceneName}` : '-',
    groupName: groupMeta?.groupName || '未指定分组',
    templateName: templateMeta?.templateName || templateForm.value.templateCode,
    appliedAt: new Date(),
    summary: response.data?.message || '共享模板应用成功'
  }
  proxy.$modal.msgSuccess(response.data?.message || '共享模板应用成功')
  templateOpen.value = false
  getList()
}

function openFormulaWorkbench() {
  proxy.$router.push({ path: COST_MENU_ROUTES.formula, query: { sceneId: form.value.sceneId || queryParams.value.sceneId || '' } })
}

function resolveDictLabel(optionsRef, value) {
  const options = Array.isArray(optionsRef) ? optionsRef : (optionsRef?.value || [])
  const match = options.find(item => item.value === value)
  return match ? match.label : value || '-'
}

function resolveDictTypeLabel(dictType) {
  const match = dictTypeOptions.value.find(item => item.dictType === dictType)
  if (!match) {
    return dictType || '-'
  }
  const suffix = match.status === '0' ? '' : '（已停用）'
  return `${match.dictName} / ${match.dictType}${suffix}`
}

function resolveVariableSourceSummary(row) {
  if (row.sourceType === 'REMOTE') {
    return row.sourceSystem || '第三方接口'
  }
  if (row.sourceType === 'DICT') {
    return resolveDictTypeLabel(row.dictType)
  }
  if (row.sourceType === 'FORMULA') {
    return row.formulaCode || '公式变量'
  }
  return row.dataPath || row.variableCode || '-'
}

function resolveVariableSourceHint(row) {
  if (row.sourceType === 'REMOTE') {
    const method = resolveRemoteOptionLabel(requestMethodOptions, row.requestMethod)
    return `${method} / ${row.dataPath || '按变量编码取值'}`
  }
  if (row.sourceType === 'DICT') {
    return row.dataPath ? `从 ${row.dataPath} 取值后做字典约束` : '按变量编码取值后做字典约束'
  }
  if (row.sourceType === 'FORMULA') {
    return row.businessFormula || row.formulaExpr || '在公式实验室维护计算口径'
  }
  return row.dataPath ? '按来源路径读取业务输入' : '按变量编码读取平铺输入'
}

function isPathSourceType(sourceType) {
  return ['INPUT', 'DICT', 'REMOTE'].includes(sourceType)
}

function hasDetailValue(value) {
  return value !== undefined && value !== null && String(value).trim() !== ''
}

function formatJson(value) {
  return safeFormatJson(value, '-')
}

function formatDateTime(value) {
  if (!value) return '-'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) {
    return value
  }
  const parts = [
    date.getFullYear(),
    String(date.getMonth() + 1).padStart(2, '0'),
    String(date.getDate()).padStart(2, '0')
  ]
  const time = [
    String(date.getHours()).padStart(2, '0'),
    String(date.getMinutes()).padStart(2, '0')
  ]
  return `${parts.join('-')} ${time.join(':')}`
}

onActivated(() => {
  getList()
})

getList()
</script>

<style scoped lang="scss">
.variable-center { display: grid; gap: 16px; }
.variable-center__hero { display: flex; justify-content: space-between; gap: 16px; align-items: flex-start; padding: 20px 24px; border: 1px solid var(--el-border-color); border-radius: 16px; background: color-mix(in srgb, var(--el-color-primary-light-9) 18%, var(--el-bg-color-overlay)); }
.variable-center__eyebrow { font-size: 12px; color: var(--el-color-primary); font-weight: 700; letter-spacing: 0.08em; text-transform: uppercase; }
.variable-center__title { margin: 8px 0 0; font-size: 26px; }
.variable-center__subtitle { margin: 10px 0 0; color: var(--el-text-color-regular); line-height: 1.8; }
.variable-center__metrics { display: grid; grid-template-columns: repeat(4, minmax(0, 1fr)); gap: 14px; }
.variable-center__metric-card { display: grid; gap: 6px; padding: 14px 16px; border: 1px solid var(--el-border-color-light); border-radius: 12px; background: var(--el-bg-color-overlay); }
.variable-center__metric-label, .variable-center__metric-desc { font-size: 12px; color: var(--el-text-color-secondary); }
.variable-center__metric-value { font-size: 24px; color: var(--el-color-primary); }
.variable-center__workspace { display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); gap: 14px; }
.variable-center__work-card { display: grid; gap: 16px; padding: 18px; border: 1px solid var(--el-border-color-light); border-radius: 16px; background: linear-gradient(180deg, color-mix(in srgb, var(--el-color-primary-light-9) 10%, var(--el-bg-color-overlay)), var(--el-bg-color-overlay)); }
.variable-center__work-head { display: flex; align-items: flex-start; justify-content: space-between; gap: 12px; }
.variable-center__work-head h3 { margin: 0; font-size: 16px; }
.variable-center__work-head p { margin: 8px 0 0; color: var(--el-text-color-secondary); line-height: 1.7; }
.variable-center__template-metrics { display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); gap: 10px; }
.variable-center__template-metric { display: grid; gap: 4px; padding: 12px 14px; border-radius: 12px; background: var(--el-fill-color-extra-light); }
.variable-center__template-metric span { font-size: 12px; color: var(--el-text-color-secondary); }
.variable-center__template-metric strong { font-size: 20px; color: var(--el-color-primary); }
.variable-center__template-tags { display: flex; flex-wrap: wrap; gap: 8px; align-items: center; }
.variable-center__latest-apply { display: grid; gap: 4px; padding: 12px 14px; border-radius: 12px; background: color-mix(in srgb, var(--el-color-success-light-9) 34%, var(--el-bg-color-overlay)); }
.variable-center__latest-apply strong { font-size: 13px; color: var(--el-color-success); }
.variable-center__latest-apply span { font-size: 12px; color: var(--el-text-color-secondary); line-height: 1.7; }
.variable-center__muted { font-size: 12px; color: var(--el-text-color-secondary); }
.variable-center__action-grid { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 10px; }
.variable-center__action-card { display: grid; gap: 6px; padding: 14px; border: 1px solid var(--el-border-color-light); border-radius: 14px; background: var(--el-bg-color-overlay); text-align: left; transition: border-color 0.2s ease, transform 0.2s ease, box-shadow 0.2s ease; cursor: pointer; }
.variable-center__action-card strong { font-size: 14px; color: var(--el-text-color-primary); }
.variable-center__action-card span { font-size: 12px; line-height: 1.7; color: var(--el-text-color-secondary); }
.variable-center__action-card:hover { border-color: color-mix(in srgb, var(--el-color-primary) 40%, var(--el-border-color-light)); box-shadow: 0 10px 24px rgba(24, 144, 255, 0.08); transform: translateY(-1px); }
.variable-center__guide-list { display: grid; gap: 10px; }
.variable-center__guide-item { display: grid; grid-template-columns: auto 1fr; align-items: flex-start; gap: 10px; padding: 10px 12px; border-radius: 12px; background: var(--el-fill-color-extra-light); }
.variable-center__guide-item span { color: var(--el-text-color-secondary); line-height: 1.7; }
.variable-center__drawer-tip { margin-bottom: 16px; padding: 12px 14px; border-radius: 12px; color: var(--el-text-color-regular); background: color-mix(in srgb, var(--el-color-primary-light-9) 32%, var(--el-bg-color-overlay)); line-height: 1.8; }
.variable-center__drawer-tip--compact { margin-top: 10px; margin-bottom: 0; padding: 10px 12px; font-size: 12px; }
.variable-center__source-guide { display: grid; grid-template-columns: repeat(4, minmax(0, 1fr)); gap: 10px; margin-bottom: 14px; }
.variable-center__source-guide-item { display: grid; gap: 6px; align-content: flex-start; min-height: 112px; padding: 12px; border: 1px solid var(--el-border-color-light); border-radius: 10px; background: var(--el-fill-color-extra-light); transition: border-color 0.2s ease, box-shadow 0.2s ease, background 0.2s ease; }
.variable-center__source-guide-item.is-active { border-color: color-mix(in srgb, var(--el-color-primary) 55%, var(--el-border-color-light)); background: color-mix(in srgb, var(--el-color-primary-light-9) 28%, var(--el-bg-color-overlay)); box-shadow: 0 0 0 1px color-mix(in srgb, var(--el-color-primary) 14%, transparent); }
.variable-center__source-guide-item strong { font-size: 13px; color: var(--el-text-color-primary); }
.variable-center__source-guide-item span { font-size: 12px; line-height: 1.6; color: var(--el-text-color-secondary); }
.variable-center__form-section { margin-bottom: 14px; padding: 14px 14px 2px; border: 1px solid var(--el-border-color-lighter); border-radius: 12px; background: var(--el-bg-color-overlay); }
.variable-center__form-section-head { display: grid; gap: 4px; margin-bottom: 12px; padding-bottom: 10px; border-bottom: 1px dashed var(--el-border-color-light); }
.variable-center__form-section-head strong { font-size: 14px; color: var(--el-text-color-primary); }
.variable-center__form-section-head span { font-size: 12px; line-height: 1.7; color: var(--el-text-color-secondary); }
.variable-center__drawer-actions { display: flex; justify-content: flex-end; margin: -6px 0 10px; }
.variable-center__source-cell { display: grid; gap: 4px; line-height: 1.5; }
.variable-center__source-cell strong { font-size: 13px; color: var(--el-text-color-primary); }
.variable-center__source-cell span { font-size: 12px; color: var(--el-text-color-secondary); }
.variable-center__remote-status { display: grid; justify-items: center; gap: 4px; line-height: 1.4; }
.variable-center__remote-status span,
.variable-center__remote-status small { max-width: 190px; color: var(--el-text-color-secondary); font-size: 12px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.variable-center__test-dialog { display: grid; gap: 12px; }
.variable-center__test-preview { display: grid; gap: 8px; }
.variable-center__test-preview-label { font-size: 13px; font-weight: 600; color: var(--el-text-color-primary); }
.variable-center__test-preview-content { margin: 0; padding: 12px 14px; max-height: 160px; overflow: auto; border-radius: 12px; background: var(--el-fill-color-light); color: var(--el-text-color-regular); line-height: 1.7; white-space: pre-wrap; word-break: break-word; font-family: Consolas, Monaco, monospace; }
.variable-center__formula-preview { width: 100%; padding: 12px 14px; border-radius: 12px; background: var(--el-fill-color-light); line-height: 1.7; color: var(--el-text-color-regular); }
.variable-group-toolbar { display: flex; flex-wrap: wrap; gap: 10px; align-items: center; }
.variable-detail { display: grid; gap: 14px; }
.variable-detail__header { padding: 14px; border: 1px solid var(--el-border-color-light); border-radius: 12px; }
.variable-detail__title { font-size: 18px; font-weight: 700; }
.variable-detail__meta { margin-top: 6px; color: var(--el-text-color-secondary); font-size: 12px; }
.variable-detail__json { margin: 0; white-space: pre-wrap; word-break: break-word; font-family: Consolas, Monaco, monospace; }
.variable-governance__summary { display: grid; grid-template-columns: repeat(4, minmax(0, 1fr)); gap: 10px; margin-bottom: 12px; }
.variable-governance__item { display: grid; gap: 4px; padding: 12px; border-radius: 12px; background: var(--el-fill-color-extra-light); }
.variable-governance__item span { font-size: 12px; color: var(--el-text-color-secondary); }
.variable-governance__item strong { font-size: 18px; color: var(--el-text-color-primary); }
.variable-dependency-map { display: grid; grid-template-columns: 150px 28px 1fr; align-items: stretch; gap: 10px; margin: 12px 0; }
.variable-dependency-map__root { display: grid; align-content: center; gap: 6px; padding: 14px; border: 1px solid color-mix(in srgb, var(--el-color-primary) 36%, var(--el-border-color-light)); border-radius: 10px; background: color-mix(in srgb, var(--el-color-primary-light-9) 44%, var(--el-bg-color-overlay)); }
.variable-dependency-map__root span,
.variable-dependency-map__root small { color: var(--el-text-color-secondary); }
.variable-dependency-map__root strong { color: var(--el-text-color-primary); word-break: break-word; }
.variable-dependency-map__arrow { display: grid; place-items: center; color: var(--el-text-color-secondary); font-weight: 700; }
.variable-dependency-map__nodes { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 10px; }
.variable-dependency-map__node { min-height: 126px; padding: 12px; border: 1px solid var(--el-border-color-light); border-radius: 10px; background: var(--el-bg-color-overlay); }
.variable-dependency-map__node.is-empty { background: var(--el-fill-color-extra-light); }
.variable-dependency-map__node-head { display: flex; align-items: center; justify-content: space-between; gap: 10px; }
.variable-dependency-map__node-head span { color: var(--el-text-color-primary); font-weight: 700; }
.variable-dependency-map__node-head strong { color: var(--el-color-primary); font-size: 20px; }
.variable-dependency-map__node p { margin: 8px 0; color: var(--el-text-color-secondary); font-size: 12px; line-height: 1.6; }
.variable-dependency-map__node ul { margin: 0; padding-left: 16px; color: var(--el-text-color-regular); font-size: 12px; line-height: 1.7; }
.variable-dependency-map__node small { color: var(--el-text-color-placeholder); }
.variable-formula-tree { margin: 12px 0 0; padding: 12px; border: 1px solid var(--el-border-color-light); border-radius: 10px; background: var(--el-bg-color-overlay); }
.variable-formula-tree__head { display: flex; align-items: center; justify-content: space-between; gap: 12px; margin-bottom: 10px; }
.variable-formula-tree__head div { display: grid; gap: 4px; }
.variable-formula-tree__head strong { color: var(--el-text-color-primary); }
.variable-formula-tree__head span { color: var(--el-text-color-secondary); font-size: 12px; }
.variable-formula-tree__tree { --el-tree-node-hover-bg-color: var(--el-fill-color-extra-light); }
.variable-formula-tree__node { display: flex; align-items: center; justify-content: space-between; gap: 12px; width: 100%; min-width: 0; }
.variable-formula-tree__node span { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.variable-template-toolbar { display: grid; grid-template-columns: 1.3fr 1fr; gap: 10px; margin-bottom: 12px; }
.variable-template-list { display: grid; gap: 12px; max-height: 420px; overflow: auto; }
.variable-template-card { padding: 14px; border: 1px solid var(--el-border-color-light); border-radius: 12px; cursor: pointer; background: var(--el-bg-color-overlay); display: grid; gap: 8px; }
.variable-template-card.is-active { border-color: var(--el-color-primary); box-shadow: 0 0 0 1px color-mix(in srgb, var(--el-color-primary) 24%, transparent); }
.variable-template-card__title { font-weight: 700; }
.variable-template-card__code,
.variable-template-card__desc, .variable-template-card__meta { font-size: 12px; color: var(--el-text-color-secondary); line-height: 1.7; }
.variable-template-card__tags { display: flex; flex-wrap: wrap; gap: 8px; }
.variable-template-card__foot { font-size: 12px; color: var(--el-text-color-secondary); }
.variable-template-summary { display: grid; grid-template-columns: repeat(4, minmax(0, 1fr)); gap: 10px; }
.variable-template-summary__item { display: grid; gap: 4px; padding: 12px; border-radius: 12px; background: var(--el-fill-color-extra-light); }
.variable-template-summary__item span { font-size: 12px; color: var(--el-text-color-secondary); }
.variable-template-summary__item strong { font-size: 18px; color: var(--el-color-primary); }
.variable-template-governance { display: grid; grid-template-columns: repeat(4, minmax(0, 1fr)); gap: 10px; }
.variable-template-governance__item { display: grid; gap: 4px; padding: 12px; border-radius: 12px; border: 1px solid color-mix(in srgb, var(--el-color-success) 24%, var(--el-border-color-light)); background: color-mix(in srgb, var(--el-color-success-light-9) 30%, var(--el-bg-color-overlay)); }
.variable-template-governance__item span { font-size: 12px; color: var(--el-text-color-secondary); }
.variable-template-governance__item strong { font-size: 18px; color: var(--el-text-color-primary); }
.variable-template-impact { display: grid; grid-template-columns: repeat(4, minmax(0, 1fr)); gap: 10px; }
.variable-template-impact__item { display: grid; gap: 4px; padding: 12px; border-radius: 12px; border: 1px solid var(--el-border-color-light); background: var(--el-bg-color-overlay); }
.variable-template-impact__item span { font-size: 12px; color: var(--el-text-color-secondary); }
.variable-template-impact__item strong { font-size: 18px; color: var(--el-text-color-primary); }
.mt12 { margin-top: 12px; }

@media (max-width: 1200px) {
  .variable-center__metrics { grid-template-columns: repeat(2, minmax(0, 1fr)); }
  .variable-center__workspace { grid-template-columns: 1fr; }
  .variable-center__template-metrics,
  .variable-center__action-grid,
  .variable-center__source-guide,
  .variable-governance__summary,
  .variable-dependency-map,
  .variable-template-summary,
  .variable-template-governance,
  .variable-template-impact { grid-template-columns: repeat(2, minmax(0, 1fr)); }
  .variable-dependency-map__arrow { display: none; }
  .variable-dependency-map__nodes { grid-template-columns: 1fr; }
}

@media (max-width: 768px) {
  .variable-center__hero,
  .variable-center__work-head { flex-direction: column; }
  .variable-center__metrics,
  .variable-center__template-metrics,
  .variable-center__action-grid,
  .variable-center__source-guide,
  .variable-governance__summary,
  .variable-dependency-map,
  .variable-dependency-map__nodes,
  .variable-template-toolbar,
  .variable-template-summary,
  .variable-template-governance,
  .variable-template-impact { grid-template-columns: 1fr; }
}
</style>

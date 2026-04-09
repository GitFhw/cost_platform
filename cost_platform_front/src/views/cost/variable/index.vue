<template>
  <div class="app-container variable-center">
    <section class="variable-center__hero">
      <div>
        <div class="variable-center__eyebrow">输入治理</div>
        <h2 class="variable-center__title">变量中心</h2>
        <p class="variable-center__subtitle">
          统一管理业务输入、字典取值、第三方接入和公式变量，支撑规则配置、批量运行和结果追溯的输入口径。
        </p>
      </div>
      <el-tag type="info">支持来源系统、鉴权方式、字段映射、缓存策略和异常兜底配置</el-tag>
    </section>

    <section class="variable-center__metrics">
      <div v-for="item in metricItems" :key="item.label" class="variable-center__metric-card">
        <span class="variable-center__metric-label">{{ item.label }}</span>
        <strong class="variable-center__metric-value">{{ item.value }}</strong>
        <span class="variable-center__metric-desc">{{ item.desc }}</span>
      </div>
    </section>

    <section class="variable-center__workspace">
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
            <span>直接承接业务输入，适合第三方已经整理好的计费对象字段。</span>
          </div>
          <div class="variable-center__guide-item">
            <el-tag type="info" size="small">DICT</el-tag>
            <span>从系统字典或核算字典取值，避免在规则里重复硬编码枚举。</span>
          </div>
          <div class="variable-center__guide-item">
            <el-tag type="warning" size="small">REMOTE</el-tag>
            <span>承接第三方接口变量；当前配置能力已齐，但真实执行链仍属于后续增强项。</span>
          </div>
          <div class="variable-center__guide-item">
            <el-tag size="small">FORMULA</el-tag>
            <span>复用公式实验室资产，通过 `formulaCode` 统一进入运行链。</span>
          </div>
        </div>
      </div>
    </section>

    <el-alert
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
      <el-col :span="1.5"><el-button type="primary" plain icon="Upload" @click="handleImport" v-hasPermi="['cost:variable:add']">导入变量</el-button></el-col>
      <el-col :span="1.5"><el-button type="success" plain icon="CopyDocument" :disabled="single" @click="handleCopy()">复制变量</el-button></el-col>
      <el-col :span="1.5"><el-button type="primary" plain icon="Collection" @click="handleTemplateCenter">共享模板</el-button></el-col>
      <el-col :span="1.5"><el-button type="info" plain icon="Connection" :disabled="single" @click="handleTestRemote">测试接口</el-button></el-col>
      <el-col :span="1.5"><el-button type="info" plain icon="View" :disabled="single" @click="handlePreviewRemote">预览数据</el-button></el-col>
      <el-col :span="1.5"><el-button type="info" plain icon="RefreshRight" @click="handleRefreshRemote">刷新缓存</el-button></el-col>
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
      <el-table-column label="来源系统" prop="sourceSystem" width="140" align="center">
        <template #default="scope">{{ scope.row.sourceSystem || '-' }}</template>
      </el-table-column>
      <el-table-column label="第三方接口" prop="remoteApi" min-width="180" align="center" :show-overflow-tooltip="true" />
      <el-table-column label="状态" prop="status" width="100" align="center">
        <template #default="scope"><dict-tag :options="variableStatusOptions" :value="scope.row.status" /></template>
      </el-table-column>
      <el-table-column label="操作" width="360" fixed="right" align="center">
        <template #default="scope">
          <el-button link type="primary" icon="Document" @click="handleDetail(scope.row)">详情</el-button>
          <el-button link type="primary" icon="View" @click="handleGovernance(scope.row)">治理</el-button>
          <el-button link type="primary" icon="CopyDocument" @click="handleCopy(scope.row)">复制</el-button>
          <el-button link type="primary" icon="Edit" @click="handleUpdate(scope.row)">修改</el-button>
          <el-button link type="primary" icon="Delete" @click="handleDelete(scope.row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination v-show="total > 0" :total="total" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" @pagination="getList" />

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
        <div class="variable-center__drawer-tip">
          先选来源类型，再补齐对应配置。第三方接口变量需明确来源系统、鉴权、字段映射、同步方式、缓存策略和失败兜底。
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
            字典来源变量统一从系统字典与核算字典下拉选择，不再手工录入 `dictType` 编码。
          </div>
        </el-form-item>
        <template v-if="form.sourceType === 'REMOTE'">
          <el-row :gutter="14">
            <el-col :span="12"><el-form-item label="来源系统" prop="sourceSystem"><el-input v-model="form.sourceSystem" placeholder="如 WMS / ERP / TMS" /></el-form-item></el-col>
            <el-col :span="12"><el-form-item label="鉴权方式" prop="authType"><el-select v-model="form.authType" style="width: 100%"><el-option v-for="item in authTypeOptions" :key="item.value" :label="item.label" :value="item.value" /></el-select></el-form-item></el-col>
            <el-col :span="24"><el-form-item label="第三方接口" prop="remoteApi"><el-input v-model="form.remoteApi" placeholder="http/https 接口地址" /></el-form-item></el-col>
            <el-col :span="24"><el-form-item label="鉴权配置JSON" prop="authConfigJson"><el-input v-model="form.authConfigJson" type="textarea" :rows="3" /></el-form-item></el-col>
            <el-col :span="24"><el-form-item label="字段映射路径" prop="dataPath"><el-input v-model="form.dataPath" placeholder="如 data.items[].value" /></el-form-item></el-col>
            <el-col :span="24"><el-form-item label="映射配置JSON" prop="mappingConfigJson"><el-input v-model="form.mappingConfigJson" type="textarea" :rows="4" /></el-form-item></el-col>
            <el-col :span="8"><el-form-item label="同步方式" prop="syncMode"><el-select v-model="form.syncMode" style="width: 100%"><el-option v-for="item in syncModeOptions" :key="item.value" :label="item.label" :value="item.value" /></el-select></el-form-item></el-col>
            <el-col :span="8"><el-form-item label="缓存策略" prop="cachePolicy"><el-select v-model="form.cachePolicy" style="width: 100%"><el-option v-for="item in cachePolicyOptions" :key="item.value" :label="item.label" :value="item.value" /></el-select></el-form-item></el-col>
            <el-col :span="8"><el-form-item label="失败兜底" prop="fallbackPolicy"><el-select v-model="form.fallbackPolicy" style="width: 100%"><el-option v-for="item in fallbackPolicyOptions" :key="item.value" :label="item.label" :value="item.value" /></el-select></el-form-item></el-col>
          </el-row>
        </template>
        <template v-if="form.sourceType === 'FORMULA'">
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

    <el-dialog title="接口测试" v-model="testOpen" width="480px" append-to-body>
      <el-descriptions :column="1" border v-if="testResult">
        <el-descriptions-item label="结果"><el-tag :type="testResult.success ? 'success' : 'danger'">{{ testResult.success ? '通过' : '失败' }}</el-tag></el-descriptions-item>
        <el-descriptions-item label="说明">{{ testResult.message }}</el-descriptions-item>
        <el-descriptions-item label="来源系统">{{ testResult.sourceSystem || '-' }}</el-descriptions-item>
        <el-descriptions-item label="接口地址">{{ testResult.remoteApi || '-' }}</el-descriptions-item>
        <el-descriptions-item label="鉴权方式">{{ resolveDictLabel(authTypeOptions, testResult.authType) }}</el-descriptions-item>
      </el-descriptions>
    </el-dialog>

    <el-dialog title="数据预览" v-model="previewOpen" width="880px" append-to-body>
      <el-row :gutter="12" v-if="previewResult">
        <el-col :span="12">
          <el-table :data="previewResult.rawRows" height="260" size="small">
            <el-table-column prop="sourceCode" label="源编码" />
            <el-table-column prop="sourceName" label="源名称" />
            <el-table-column prop="value" label="源值" />
          </el-table>
        </el-col>
        <el-col :span="12">
          <el-table :data="previewResult.mappedRows" height="260" size="small">
            <el-table-column prop="variableCode" label="变量编码" />
            <el-table-column prop="mappedValue" label="映射值" />
            <el-table-column prop="dataPath" label="映射路径" />
          </el-table>
        </el-col>
      </el-row>
      <el-descriptions v-if="previewResult" :column="2" border class="mt12">
        <el-descriptions-item label="来源系统">{{ previewResult.sourceSystem || '-' }}</el-descriptions-item>
        <el-descriptions-item label="同步方式">{{ resolveDictLabel(syncModeOptions, previewResult.syncMode) }}</el-descriptions-item>
        <el-descriptions-item label="缓存策略">{{ resolveDictLabel(cachePolicyOptions, previewResult.cachePolicy) }}</el-descriptions-item>
        <el-descriptions-item label="失败兜底">{{ resolveDictLabel(fallbackPolicyOptions, previewResult.fallbackPolicy) }}</el-descriptions-item>
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
            <el-table-column prop="variableCode" label="变量编码" width="120" />
            <el-table-column prop="message" label="校验问题" min-width="220" show-overflow-tooltip />
          </el-table>
        </el-col>
      </el-row>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="submitImportPreview">导入预览</el-button>
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
          <el-descriptions-item label="来源系统">{{ detailInfo.sourceSystem || '-' }}</el-descriptions-item>
          <el-descriptions-item label="字典类型">{{ resolveDictTypeLabel(detailInfo.dictType) }}</el-descriptions-item>
          <el-descriptions-item label="第三方接口">{{ detailInfo.remoteApi || '-' }}</el-descriptions-item>
          <el-descriptions-item label="鉴权方式">{{ resolveDictLabel(authTypeOptions, detailInfo.authType) }}</el-descriptions-item>
          <el-descriptions-item label="数据路径">{{ detailInfo.dataPath || '-' }}</el-descriptions-item>
          <el-descriptions-item label="同步方式">{{ resolveDictLabel(syncModeOptions, detailInfo.syncMode) }}</el-descriptions-item>
          <el-descriptions-item label="缓存策略">{{ resolveDictLabel(cachePolicyOptions, detailInfo.cachePolicy) }}</el-descriptions-item>
          <el-descriptions-item label="失败兜底">{{ resolveDictLabel(fallbackPolicyOptions, detailInfo.fallbackPolicy) }}</el-descriptions-item>
          <el-descriptions-item label="默认值">{{ detailInfo.defaultValue || '-' }}</el-descriptions-item>
          <el-descriptions-item label="公式编码">{{ detailInfo.formulaCode || '-' }}</el-descriptions-item>
          <el-descriptions-item label="中文公式">{{ detailInfo.businessFormula || '-' }}</el-descriptions-item>
          <el-descriptions-item label="公式表达式">{{ detailInfo.formulaExpr || '-' }}</el-descriptions-item>
          <el-descriptions-item label="鉴权配置JSON"><pre class="variable-detail__json">{{ formatJson(detailInfo.authConfigJson) }}</pre></el-descriptions-item>
          <el-descriptions-item label="映射配置JSON"><pre class="variable-detail__json">{{ formatJson(detailInfo.mappingConfigJson) }}</pre></el-descriptions-item>
          <el-descriptions-item label="备注">{{ detailInfo.remark || '-' }}</el-descriptions-item>
        </el-descriptions>
      </div>
    </el-drawer>

    <el-drawer v-model="governanceOpen" title="变量治理检查" size="500px" append-to-body>
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
        <el-descriptions :column="1" border>
          <el-descriptions-item label="变量">{{ governanceInfo.variableCode }} / {{ governanceInfo.variableName }}</el-descriptions-item>
          <el-descriptions-item label="所属场景">{{ governanceInfo.sceneCode }} / {{ governanceInfo.sceneName }}</el-descriptions-item>
          <el-descriptions-item label="费用关系引用">{{ governanceInfo.feeRelCount }}</el-descriptions-item>
          <el-descriptions-item label="规则条件引用">{{ governanceInfo.ruleConditionCount }}</el-descriptions-item>
          <el-descriptions-item label="规则计量引用">{{ governanceInfo.ruleQuantityCount }}</el-descriptions-item>
          <el-descriptions-item label="发布版本引用">{{ governanceInfo.publishedVersionCount }}</el-descriptions-item>
        </el-descriptions>
        <el-alert :title="governanceInfo.canDelete ? '允许删除' : '当前不允许删除'" :description="governanceInfo.removeBlockingReason" :type="governanceInfo.canDelete ? 'success' : 'warning'" :closable="false" show-icon class="mt12" />
        <el-alert :title="governanceInfo.canDisable ? '允许停用' : '当前不允许停用'" :description="governanceInfo.disableBlockingReason" :type="governanceInfo.canDisable ? 'success' : 'warning'" :closable="false" show-icon class="mt12" />
        <el-alert title="删除建议" :description="governanceInfo.removeAdvice" type="info" :closable="false" show-icon class="mt12" />
        <el-alert title="停用建议" :description="governanceInfo.disableAdvice" type="info" :closable="false" show-icon class="mt12" />
      </div>
    </el-drawer>
  </div>
</template>

<script setup name="CostVariable">
import { computed, getCurrentInstance, reactive, ref, toRefs, watch } from 'vue'
import { ElMessageBox } from 'element-plus'
import { UploadFilled } from '@element-plus/icons-vue'
import { optionselectFormula } from '@/api/cost/formula'
import { optionselectScene } from '@/api/cost/scene'
import { optionselect as getDictTypeOptionselect } from '@/api/system/dict/type'
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
import { optionselectVariableGroup } from '@/api/cost/variableGroup'
import { resolveWorkingCostSceneId } from '@/utils/costSceneContext'
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

const validateDictType = (_rule, value, callback) => {
  if (data.form.sourceType === 'DICT' && !value) {
    callback(new Error('字典类型不能为空'))
    return
  }
  callback()
}

const data = reactive({
  queryParams: { pageNum: 1, pageSize: 10, sceneId: undefined, groupId: undefined, variableCode: undefined, variableName: undefined, sourceType: undefined, sourceSystem: undefined },
  form: {},
  copyForm: { variableId: undefined, targetSceneId: undefined, targetGroupId: undefined, variableCode: undefined, variableName: undefined },
  templateForm: { sceneId: undefined, groupId: undefined, templateCode: undefined, updateSupport: false },
  importForm: { updateSupport: false },
  rules: {
    sceneId: [{ required: true, message: '所属场景不能为空', trigger: 'change' }],
    variableCode: [{ required: true, message: '变量编码不能为空', trigger: 'blur' }],
    variableName: [{ required: true, message: '变量名称不能为空', trigger: 'blur' }],
    variableType: [{ required: true, message: '变量类型不能为空', trigger: 'change' }],
    sourceType: [{ required: true, message: '来源类型不能为空', trigger: 'change' }],
    dictType: [{ validator: validateDictType, trigger: 'change' }],
    status: [{ required: true, message: '状态不能为空', trigger: 'change' }]
  },
  copyRules: {
    targetSceneId: [{ required: true, message: '目标场景不能为空', trigger: 'change' }],
    variableCode: [{ required: true, message: '新变量编码不能为空', trigger: 'blur' }],
    variableName: [{ required: true, message: '新变量名称不能为空', trigger: 'blur' }]
  }
})
const { queryParams, form, copyForm, templateForm, importForm, rules, copyRules } = toRefs(data)

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
      'cost_variable_fallback_policy'
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

async function getList() {
  loading.value = true
  try {
    await loadBaseOptions()
    const [rows, statsResponse] = await Promise.all([listVariable(queryParams.value), getVariableStats(queryParams.value)])
    variableList.value = rows.rows
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
    authType: 'NONE',
    authConfigJson: undefined,
    dataPath: undefined,
    mappingConfigJson: undefined,
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
  form.value = { ...response.data }
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
  const blocked = checks.filter(item => !item.canDelete)
  if (blocked.length) {
    await ElMessageBox.alert(blocked.map(item => `${item.variableName}：${item.removeBlockingReason}`).join('<br/>'), '删除前治理检查', { type: 'warning', dangerouslyUseHTMLString: true })
    governanceInfo.value = blocked[0]
    governanceOpen.value = true
    return
  }
  const variableIds = row?.variableId || ids.value
  const variableNames = targetRows.map(item => item.variableName).join('、')
  proxy.$modal.confirm(`是否确认删除变量"${variableNames}"的数据项？`).then(function() {
    return delVariable(variableIds)
  }).then(() => {
    getList()
    proxy.$modal.msgSuccess('删除成功')
  }).catch(() => {})
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

function currentRow() {
  return variableList.value.find(item => item.variableId === ids.value[0])
}

async function handleTestRemote() {
  const row = currentRow()
  if (!row) return
  const response = await testVariableRemote({ remoteApi: row.remoteApi, authType: row.authType, sourceSystem: row.sourceSystem })
  testResult.value = response.data
  testOpen.value = true
}

async function handlePreviewRemote() {
  const row = currentRow()
  if (!row) return
  const response = await previewVariableRemote({
    variableId: row.variableId,
    dataPath: row.dataPath,
    variableCode: row.variableCode,
    sourceSystem: row.sourceSystem,
    syncMode: row.syncMode,
    cachePolicy: row.cachePolicy,
    fallbackPolicy: row.fallbackPolicy
  })
  previewResult.value = response.data
  previewOpen.value = true
}

async function handleRefreshRemote() {
  const response = await refreshVariableRemote({ sceneId: queryParams.value.sceneId })
  proxy.$modal.msgSuccess(response.data?.message || '刷新成功')
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

function buildImportFormData() {
  if (!importFile.value) {
    proxy.$modal.msgWarning('请先选择导入文件')
    return null
  }
  const formData = new FormData()
  formData.append('file', importFile.value)
  return formData
}

async function submitImportPreview() {
  const formData = buildImportFormData()
  if (!formData) return
  const response = await previewVariableImport(formData)
  importPreview.value = response.data || importPreview.value
}

async function submitImportData() {
  const formData = buildImportFormData()
  if (!formData) return
  formData.append('updateSupport', importForm.value.updateSupport)
  const response = await importVariableData(formData)
  importPreview.value = response.data || importPreview.value
  proxy.$modal.msgSuccess(`导入完成：通过 ${importPreview.value.passRows} 行，失败 ${importPreview.value.failRows} 行`)
  if (importPreview.value.failRows === 0) {
    importOpen.value = false
    resetImportState()
    getList()
  }
}

function downloadImportTemplate() {
  proxy.download('cost/variable/importTemplate', {}, 'cost_variable_import_template.xlsx')
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
  proxy.$router.push({ path: '/cost/formula', query: { sceneId: form.value.sceneId || queryParams.value.sceneId || '' } })
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

function formatJson(value) {
  if (!value) return '-'
  try {
    return JSON.stringify(typeof value === 'string' ? JSON.parse(value) : value, null, 2)
  } catch (error) {
    return value
  }
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
.variable-center__drawer-actions { display: flex; justify-content: flex-end; margin: -6px 0 10px; }
.variable-center__formula-preview { width: 100%; padding: 12px 14px; border-radius: 12px; background: var(--el-fill-color-light); line-height: 1.7; color: var(--el-text-color-regular); }
.variable-detail { display: grid; gap: 14px; }
.variable-detail__header { padding: 14px; border: 1px solid var(--el-border-color-light); border-radius: 12px; }
.variable-detail__title { font-size: 18px; font-weight: 700; }
.variable-detail__meta { margin-top: 6px; color: var(--el-text-color-secondary); font-size: 12px; }
.variable-detail__json { margin: 0; white-space: pre-wrap; word-break: break-word; font-family: Consolas, Monaco, monospace; }
.variable-governance__summary { display: grid; grid-template-columns: repeat(4, minmax(0, 1fr)); gap: 10px; margin-bottom: 12px; }
.variable-governance__item { display: grid; gap: 4px; padding: 12px; border-radius: 12px; background: var(--el-fill-color-extra-light); }
.variable-governance__item span { font-size: 12px; color: var(--el-text-color-secondary); }
.variable-governance__item strong { font-size: 18px; color: var(--el-text-color-primary); }
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
  .variable-governance__summary,
  .variable-template-summary,
  .variable-template-governance,
  .variable-template-impact { grid-template-columns: repeat(2, minmax(0, 1fr)); }
}

@media (max-width: 768px) {
  .variable-center__hero,
  .variable-center__work-head { flex-direction: column; }
  .variable-center__metrics,
  .variable-center__template-metrics,
  .variable-center__action-grid,
  .variable-governance__summary,
  .variable-template-toolbar,
  .variable-template-summary,
  .variable-template-governance,
  .variable-template-impact { grid-template-columns: 1fr; }
}
</style>

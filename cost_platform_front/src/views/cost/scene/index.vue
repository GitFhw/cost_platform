<template>
  <div class="app-container scene-center">
    <section v-show="!isCompactMode" class="scene-center__hero">
      <div class="scene-center__hero-main">
        <div class="scene-center__eyebrow">场景主数据</div>
        <h2 class="scene-center__title">场景中心</h2>
        <p class="scene-center__subtitle">
          统一维护核算场景、业务域和适用组织，为费用配置、变量治理、版本发布和运行管理建立清晰边界。
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
          <el-tag type="info" class="scene-center__hero-tag">业务域口径统一管理，便于跨场景复用与治理</el-tag>
          <el-tag v-if="currentSceneInfo.sceneId" type="success" class="scene-center__hero-tag">
            当前工作场景：{{ currentSceneInfo.sceneCode }} / {{ currentSceneInfo.sceneName }}
          </el-tag>
          <el-button
            v-if="currentSceneInfo.sceneId"
            plain
            icon="Close"
            @click="handleClearCurrentScene"
          >
            清除工作场景
          </el-button>
        </div>
      </div>
      <div class="scene-center__hero-note">
        <span class="scene-center__hero-note-label">治理说明</span>
        <strong class="scene-center__hero-note-title">场景是费用、变量、规则、发布的上游边界</strong>
        <span class="scene-center__hero-note-desc">
          场景可理解为合同、核算主题或业务方案。当前生效版本仅展示已发布的正式结果，便于统一管理场景口径与适用范围。
        </span>
      </div>
    </section>

    <section v-show="!isCompactMode" class="scene-center__metrics">
      <div v-for="item in metricItems" :key="item.label" class="scene-center__metric-card">
        <span class="scene-center__metric-label">{{ item.label }}</span>
        <strong class="scene-center__metric-value">{{ item.value }}</strong>
        <span class="scene-center__metric-desc">{{ item.desc }}</span>
      </div>
    </section>

    <section v-if="currentSceneInfo.sceneId && !isCompactMode" class="scene-center__publish-workbench">
      <div class="scene-center__publish-workbench-head">
        <div>
          <div class="scene-center__publish-workbench-eyebrow">发布治理</div>
          <div class="scene-center__publish-workbench-title">{{ currentSceneInfo.sceneName }} 发布工作台</div>
          <div class="scene-center__publish-workbench-desc">
            场景中心负责当前工作场景的发布检查、首发与快捷回看；跨场景台账、生效切换、回滚与完整差异仍由发布中心统一治理。
          </div>
        </div>
        <div class="scene-center__publish-workbench-actions">
          <el-button plain icon="Tickets" @click="handleOpenPublishCenter(currentSceneInfo)" v-hasPermi="['cost:publish:list']">
            发布中心总台
          </el-button>
          <el-button plain icon="Document" @click="handleOpenPublishAudit" v-hasPermi="['cost:publish:list']">
            发布审计
          </el-button>
        </div>
      </div>

      <el-tabs v-model="publishWorkbenchTab" class="scene-center__publish-tabs">
        <el-tab-pane label="发布预览" name="publish">
          <div class="scene-center__publish-exec">
            <div class="scene-center__publish-exec-grid">
              <div class="scene-center__publish-exec-card">
                <span>发布责任</span>
                <strong>预览不发布</strong>
                <small>场景中心只看当前场景发布影响，正式生成版本统一进入发布中心。</small>
              </div>
              <div class="scene-center__publish-exec-card">
                <span>最近校验</span>
                <div class="scene-center__publish-exec-status">
                  <el-tag :type="scenePrecheckMeta.tag">{{ scenePrecheckMeta.label }}</el-tag>
                  <small>{{ scenePrecheckMeta.note }}</small>
                </div>
              </div>
              <div class="scene-center__publish-exec-card">
                <span>最近校验告警</span>
                <strong>{{ scenePrecheckMeta.warningCount }}</strong>
                <small>当前工作场景最近一次检查中的告警数量</small>
              </div>
            </div>

            <div class="scene-center__publish-boundary">
              <div>
                <strong>企业级发布边界</strong>
                <span>配置维护和正式发布分离：这里负责发现影响，发布中心负责生成版本、生效切换、回滚和审计留痕。</span>
              </div>
              <el-tag type="primary" effect="plain">只读预览</el-tag>
            </div>

            <div class="scene-center__publish-action-row">
              <el-button
                type="primary"
                icon="CircleCheck"
                :loading="scenePublishLoading"
                @click="handleScenePrecheck"
              >
                查看发布影响
              </el-button>
              <el-button
                type="success"
                icon="Promotion"
                @click="handleOpenPublishCenter(currentSceneInfo)"
                v-hasPermi="['cost:publish:add']"
              >
                去发布中心发布
              </el-button>
            </div>

            <el-alert
              v-if="!scenePrecheck.checked"
              title="建议先执行发布影响预览，再进入发布中心正式发布。"
              type="info"
              :closable="false"
              show-icon
              class="scene-center__publish-exec-alert"
            />
            <el-alert
              v-else-if="scenePrecheck.publishable === false"
              title="当前仍有阻断项，请先处理配置问题，再进入发布中心发布。"
              type="warning"
              :closable="false"
              show-icon
              class="scene-center__publish-exec-alert"
            />
            <el-alert
              v-else
              title="当前检查未发现阻断项，可以进入发布中心发起正式发布。"
              type="success"
              :closable="false"
              show-icon
              class="scene-center__publish-exec-alert"
            />

            <PublishPrecheckPanel
              v-if="scenePrecheck.checked"
              :data="scenePrecheck"
              :columns="4"
              :impact-columns="3"
              show-empty
              impact-subtitle="当前场景发布前的费用、规则和变量影响预览；正式发布仍需进入发布中心执行。"
            />
          </div>
        </el-tab-pane>

        <el-tab-pane label="版本台账" name="ledger">
          <div class="scene-center__ledger-metrics">
            <div class="scene-center__ledger-card">
              <span>版本总数</span>
              <strong>{{ sceneVersionTotal }}</strong>
              <small>当前场景累计形成的发布版本数</small>
            </div>
            <div class="scene-center__ledger-card">
              <span>当前生效版本</span>
              <strong>{{ publishSummary.activeVersionNo || '未生效' }}</strong>
              <small>当前工作场景已经切换到的正式版本</small>
            </div>
            <div class="scene-center__ledger-card">
              <span>最近发布版本</span>
              <strong>{{ publishSummary.latestVersionNo || '暂无版本' }}</strong>
              <small>{{ publishSummary.latestPublishedTime || '尚未产生发布时间' }}</small>
            </div>
            <div class="scene-center__ledger-card">
              <span>最近检查结论</span>
              <strong>{{ publishSummary.validationLabel }}</strong>
              <small>{{ publishSummary.validationNote }}</small>
            </div>
          </div>

          <el-table
            v-if="sceneVersionLedger.length"
            v-loading="sceneVersionLoading"
            :data="sceneVersionLedger"
            size="small"
            class="scene-center__ledger-table"
          >
            <el-table-column label="版本号" prop="versionNo" width="160" align="center" />
            <el-table-column label="状态" width="120" align="center">
              <template #default="scope">
                <dict-tag :options="scenePublishVersionStatusOptions" :value="scope.row.versionStatus" />
              </template>
            </el-table-column>
            <el-table-column label="检查结果" width="150" align="center">
              <template #default="scope">
                <el-tag :type="resolveValidationMeta(scope.row.validationResultJson).tag">
                  {{ resolveValidationMeta(scope.row.validationResultJson).label }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="发布说明" prop="publishDesc" min-width="220" :show-overflow-tooltip="true" />
            <el-table-column label="发布时间" width="180" align="center">
              <template #default="scope">{{ parseTime(scope.row.publishedTime) }}</template>
            </el-table-column>
            <el-table-column label="操作" width="220" align="center" fixed="right">
              <template #default="scope">
                <el-button link type="primary" icon="Tickets" @click="handleUseAsCompareTarget(scope.row)">
                  进入对比
                </el-button>
                <el-button link type="primary" icon="Right" @click="handleOpenPublishCenter(currentSceneInfo)">
                  去发布中心
                </el-button>
              </template>
            </el-table-column>
          </el-table>
          <el-empty
            v-else-if="!sceneVersionLoading"
            description="当前场景还没有发布版本，可先在发布中心完成首发。"
          />
        </el-tab-pane>

        <el-tab-pane label="版本对比" name="compare">
          <template v-if="sceneVersionLedger.length >= 2">
            <div class="scene-center__compare-toolbar">
              <el-select
                v-model="sceneCompareForm.fromVersionId"
                placeholder="请选择基准版本"
                style="width: 240px"
                @change="loadSceneCompareDiff"
              >
                <el-option
                  v-for="item in sceneVersionLedger"
                  :key="item.versionId"
                  :label="item.versionNo"
                  :value="item.versionId"
                />
              </el-select>
              <span class="scene-center__compare-vs">VS</span>
              <el-select
                v-model="sceneCompareForm.toVersionId"
                placeholder="请选择目标版本"
                style="width: 240px"
                @change="loadSceneCompareDiff"
              >
                <el-option
                  v-for="item in sceneVersionLedger"
                  :key="item.versionId"
                  :label="item.versionNo"
                  :value="item.versionId"
                />
              </el-select>
              <el-button plain icon="Refresh" @click="handleSwapSceneCompare">交换版本</el-button>
            </div>

            <el-alert
              v-if="!sceneCompareReady"
              title="请选择两个不同的发布版本后，再查看场景级差异。"
              type="info"
              :closable="false"
              class="scene-center__compare-alert"
            />

            <div v-else v-loading="sceneDiffLoading" class="scene-center__compare-body">
              <el-descriptions :column="4" border class="scene-center__compare-summary">
                <el-descriptions-item label="场景级变化">{{ sceneCompareData.summary?.sceneChangeCount || 0 }}</el-descriptions-item>
                <el-descriptions-item label="费用变化">{{ sceneCompareData.summary?.feeChangeCount || 0 }}</el-descriptions-item>
                <el-descriptions-item label="规则变化">{{ sceneCompareData.summary?.ruleChangeCount || 0 }}</el-descriptions-item>
                <el-descriptions-item label="新增费用">{{ sceneCompareData.summary?.addedFeeCount || 0 }}</el-descriptions-item>
              </el-descriptions>

              <JsonDiffViewer
                title="场景主数据"
                subtitle="先看字段级差异摘要，再看当前场景两个发布版本的快照对比。"
                :left-title="sceneCompareData.fromVersion?.versionNo || '基准版本'"
                :right-title="sceneCompareData.toVersion?.versionNo || '目标版本'"
                :left-value="sceneDiffLeftValue"
                :right-value="sceneDiffRightValue"
                :rows="10"
              />

              <div v-if="sceneCompareFeeHighlights.length" class="scene-center__compare-impact">
                <div class="scene-center__compare-impact-title">受影响费用摘要</div>
                <div class="scene-center__compare-impact-list">
                  <div
                    v-for="item in sceneCompareFeeHighlights"
                    :key="item.feeCode"
                    class="scene-center__compare-impact-card"
                  >
                    <strong>{{ item.feeName || item.feeCode }}</strong>
                    <span>{{ item.feeCode }}</span>
                    <small v-if="item.changedVariables?.length">{{ resolveChangedVariablePreview(item) }}</small>
                    <small>{{ resolveFeeImpactSummary(item) }}</small>
                  </div>
                </div>
              </div>
            </div>
          </template>
          <el-empty v-else description="至少需要两个发布版本，才能在场景中心直接做版本对比。" />
        </el-tab-pane>
      </el-tabs>
    </section>

    <el-alert
      v-show="!isCompactMode"
      title="核算相关字典统一维护在系统字典中心，便于业务域、场景、费用、变量和规则共享同一套基础口径。"
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
        <el-tree-select
          v-model="queryParams.orgCode"
          :data="deptOptions"
          :props="{ value: 'id', label: 'label', children: 'children' }"
          value-key="id"
          placeholder="请选择适用组织"
          clearable
          check-strictly
          filterable
          style="width: 220px"
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
      <el-table-column label="适用组织" align="center" prop="orgCode" min-width="160" :show-overflow-tooltip="true">
        <template #default="scope">
          <span>{{ resolveOrgLabel(scope.row.orgCode) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="默认对象维度" align="center" prop="defaultObjectDimension" min-width="160" :show-overflow-tooltip="true">
        <template #default="scope">
          <span>{{ scope.row.defaultObjectDimension || '-' }}</span>
        </template>
      </el-table-column>
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
          <el-button link type="primary" icon="Select" @click="handleSetCurrentScene(scope.row)">
            设为工作场景
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
        <el-alert
          title="当前阶段业务域用于表达行业/业务归属，场景类型用于表达配置组织方式，两者默认不做强绑定约束；只有后续明确存在固定组合时，再补专门映射规则。"
          type="info"
          :closable="false"
          class="scene-center__dialog-alert"
        />
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
              <el-tree-select
                v-model="form.orgCode"
                :data="deptOptions"
                :props="{ value: 'id', label: 'label', children: 'children' }"
                value-key="id"
                placeholder="请选择适用组织"
                check-strictly
                clearable
                filterable
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="默认对象维度" prop="defaultObjectDimension">
              <el-select
                v-model="form.defaultObjectDimension"
                filterable
                allow-create
                clearable
                default-first-option
                style="width: 100%"
                placeholder="请选择或录入默认对象维度"
              >
                <el-option v-for="item in objectDimensionOptions" :key="item" :label="item" :value="item" />
              </el-select>
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

    <el-drawer v-model="governanceOpen" title="场景详情与治理" size="760px" append-to-body>
      <div v-loading="governanceLoading" class="scene-governance">
        <template v-if="governanceInfo.sceneId">
          <div class="scene-governance__header">
            <div>
              <div class="scene-governance__title">{{ governanceInfo.sceneName }}</div>
              <div class="scene-governance__meta">
                <span>{{ governanceInfo.sceneCode }}</span>
                <span>业务域：{{ resolveDictLabel(businessDomainOptions, governanceInfo.businessDomain) }}</span>
                <span>对象维度：{{ governanceInfo.defaultObjectDimension || '-' }}</span>
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
              <strong>{{ governanceInfo.activeVersionNo || governanceInfo.activeVersionId || '-' }}</strong>
            </div>
            <div class="scene-governance__card">
              <span>核算任务</span>
              <strong>{{ governanceInfo.taskCount }}</strong>
            </div>
            <div class="scene-governance__card">
              <span>运行中任务</span>
              <strong>{{ governanceInfo.runningTaskCount }}</strong>
            </div>
            <div class="scene-governance__card">
              <span>异常任务</span>
              <strong>{{ governanceInfo.failedTaskCount }}</strong>
            </div>
          </div>

          <div class="scene-governance__recent">
            <div class="scene-governance__recent-head">
              <div>
                <strong>最近核算任务</strong>
                <small>按最近启动时间展示，用于判断场景是否正在运行或已有异常。</small>
              </div>
              <el-button link type="primary" icon="Right" @click="handleOpenTaskCenter(governanceInfo)">
                查看全部
              </el-button>
            </div>
            <el-empty v-if="!governanceInfo.recentTasks.length" description="当前场景暂无核算任务" :image-size="56" />
            <div v-else class="scene-governance__task-list">
              <div v-for="item in governanceInfo.recentTasks" :key="item.taskId" class="scene-governance__task-item">
                <div class="scene-governance__task-head">
                  <div>
                    <strong>{{ item.taskNo }}</strong>
                    <span>{{ resolveDictLabel(taskTypeOptions, item.taskType) }} · {{ item.billMonth || '-' }}</span>
                  </div>
                  <dict-tag :options="taskStatusOptions" :value="item.taskStatus" />
                </div>
                <el-progress
                  :percentage="Number(item.progressPercent || 0)"
                  :status="resolveTaskProgressStatus(item.taskStatus)"
                  :stroke-width="8"
                />
                <div class="scene-governance__task-meta">
                  <span>版本：{{ item.versionNo || '-' }}</span>
                  <span>成功/失败：{{ item.successCount || 0 }} / {{ item.failCount || 0 }}</span>
                  <span>启动：{{ parseTime(item.startedTime) || '-' }}</span>
                </div>
                <div v-if="item.errorMessage" class="scene-governance__task-error">{{ item.errorMessage }}</div>
              </div>
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
          <GovernanceImpactList :impacts="governanceInfo.impactItems" :context="governanceInfo" />

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
import GovernanceImpactList from '@/components/cost/GovernanceImpactList.vue'
import JsonDiffViewer from '@/components/cost/JsonDiffViewer.vue'
import PublishPrecheckPanel from '@/components/cost/publish/PublishPrecheckPanel.vue'
import { getPublishDiff, getPublishPrecheck, listPublish } from '@/api/cost/publish'
import { addScene, delScene, getScene, getSceneGovernance, getSceneStats, listScene, updateScene } from '@/api/cost/scene'
import { deptTreeSelect } from '@/api/system/user'
import useSettingsStore from '@/store/modules/settings'
import { getCostSceneContextId, setCostSceneContextId } from '@/utils/costSceneContext'
import { confirmCostDeleteImpact, confirmCostDisableImpact, findFirstDeleteBlockedCheck, findFirstDisableBlockedCheck } from '@/utils/costGovernanceDeletePreview'
import { confirmCostSceneSwitch } from '@/utils/costSceneSwitchGuard'
import { resolveCostChangeTypeLabel } from '@/utils/costDisplayLabels'
import { COST_MENU_ROUTES } from '@/utils/costMenuRoutes'
import { formatLegacyOrgLabel } from '@/utils/costOptionLabel'
import { getRemoteDictOptionMap } from '@/utils/dictRemote'

const { proxy } = getCurrentInstance()
const router = useRouter()
const settingsStore = useSettingsStore()
const isCompactMode = computed(() => settingsStore.costPageMode === 'COMPACT')

const sceneList = ref([])
const deptOptions = ref([])
const deptLabelMap = ref({})
const deptLoadDegraded = ref(false)
const businessDomainOptions = ref([])
const sceneStatusOptions = ref([])
const sceneTypeOptions = ref([])
const taskTypeOptions = ref([])
const taskStatusOptions = ref([])
const objectDimensionOptions = ['协力队', '协力单位', '班组', '人员', '设备', '船舶', '库区', '订单']
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
const currentSceneInfo = ref({})
const publishSummary = reactive(createEmptyPublishSummary())
const publishWorkbenchTab = ref('publish')
const sceneVersionLoading = ref(false)
const sceneDiffLoading = ref(false)
const sceneVersionLedger = ref([])
const sceneVersionTotal = ref(0)
const sceneCompareData = ref(createEmptySceneCompareData())
const scenePublishLoading = ref(false)
const scenePrecheck = ref(createEmptyScenePrecheck())
const sceneCompareForm = reactive({
  fromVersionId: undefined,
  toVersionId: undefined
})
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
    return queryParams.value.orgCode ? `业务域：全部 · 组织：${resolveOrgLabel(queryParams.value.orgCode)}` : '业务域：全部'
  }
  const option = businessDomainOptions.value.find(item => item.value === queryParams.value.businessDomain)
  const domainText = `业务域：${option ? option.label : queryParams.value.businessDomain}`
  return queryParams.value.orgCode ? `${domainText} · 组织：${resolveOrgLabel(queryParams.value.orgCode)}` : domainText
})

const scenePublishVersionStatusOptions = computed(() => [
  { label: '草稿版本', value: 'DRAFT' },
  { label: '生效中', value: 'ACTIVE' },
  { label: '已回滚', value: 'ROLLED_BACK' }
])

const sceneCompareReady = computed(() => {
  return Boolean(
    sceneCompareForm.fromVersionId
      && sceneCompareForm.toVersionId
      && sceneCompareForm.fromVersionId !== sceneCompareForm.toVersionId
  )
})

const sceneDiffLeftValue = computed(() => {
  return sceneCompareData.value.fromScene ?? buildSceneSnapshotFromDiffs(sceneCompareData.value.sceneDiffs, 'fromValue')
})

const sceneDiffRightValue = computed(() => {
  return sceneCompareData.value.toScene ?? buildSceneSnapshotFromDiffs(sceneCompareData.value.sceneDiffs, 'toValue')
})

const sceneCompareFeeHighlights = computed(() => {
  return (sceneCompareData.value.feeDiffs || []).slice(0, 6)
})

const scenePrecheckMeta = computed(() => {
  const payload = scenePrecheck.value || {}
  if (!payload.checked) {
    return {
      label: '未执行',
      tag: 'info',
      note: '建议发布前先执行一次校验',
      warningCount: 0
    }
  }
  const validation = resolveValidationMeta(payload)
  return {
    label: payload.publishable === false ? '阻断' : validation.label,
    tag: payload.publishable === false ? 'danger' : validation.tag,
    note: buildValidationNote(payload),
    warningCount: Number(payload.warningCount || 0)
  }
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
    label: '默认对象维度',
    value: currentSceneInfo.value.defaultObjectDimension || '-',
    desc: currentSceneInfo.value.sceneId ? '当前工作场景的默认计费对象维度' : '设置工作场景后可查看默认维度'
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
    'cost_scene_type',
    'cost_calc_task_type',
    'cost_calc_task_status'
  ])
  businessDomainOptions.value = dictMap.cost_business_domain || []
  sceneStatusOptions.value = dictMap.cost_scene_status || []
  sceneTypeOptions.value = dictMap.cost_scene_type || []
  taskTypeOptions.value = dictMap.cost_calc_task_type || []
  taskStatusOptions.value = dictMap.cost_calc_task_status || []
}

async function loadDeptOptions() {
  try {
    const response = await deptTreeSelect()
    deptOptions.value = normalizeDeptTreeOptions(response.data || [])
    deptLabelMap.value = buildDeptLabelMap(deptOptions.value)
    deptLoadDegraded.value = false
  } catch (error) {
    deptOptions.value = []
    deptLabelMap.value = {}
    if (!deptLoadDegraded.value) {
      proxy.$modal.msgWarning('适用组织树加载失败，已按空树降级处理，不影响场景中心台账查看。')
      deptLoadDegraded.value = true
    }
  }
}

async function getList() {
  loading.value = true
  try {
    const [, , listResponse, statsResponse] = await Promise.all([
      loadSceneDictOptions(),
      loadDeptOptions(),
      listScene(queryParams.value),
      getSceneStats(queryParams.value)
    ])
    sceneList.value = listResponse.rows
    total.value = listResponse.total
    Object.assign(statistics, normalizeStats(statsResponse.data))
    await syncCurrentSceneInfo()
  } finally {
    loading.value = false
  }
}

async function syncCurrentSceneInfo() {
  const currentSceneId = getCostSceneContextId()
  if (!currentSceneId) {
    currentSceneInfo.value = {}
    resetPublishSummary()
    resetSceneWorkbench()
    return
  }
  const matched = sceneList.value.find(item => item.sceneId === currentSceneId)
  if (matched) {
    currentSceneInfo.value = matched
    setCostSceneContextId(matched)
    await loadCurrentScenePublishSummary(matched)
    return
  }
  try {
    const response = await getScene(currentSceneId)
    currentSceneInfo.value = response.data || {}
    setCostSceneContextId(currentSceneInfo.value)
    await loadCurrentScenePublishSummary(currentSceneInfo.value)
  } catch (error) {
    currentSceneInfo.value = {}
    resetPublishSummary()
    resetSceneWorkbench()
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
    orgCode: undefined,
    sceneType: 'CONTRACT',
    defaultObjectDimension: undefined,
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
    defaultObjectDimension: response.data?.defaultObjectDimension || undefined,
    orgCode: response.data?.orgCode ? String(response.data.orgCode) : undefined
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
  const allowed = await confirmCostDeleteImpact({
    checks,
    targetLabel: '场景',
    targetNames: targetRows.map(item => item.sceneName)
  })
  if (!allowed) {
    const blockedCheck = findFirstDeleteBlockedCheck(checks)
    if (blockedCheck?.sceneId) {
      openGovernanceDrawer(blockedCheck)
    }
    return
  }

  const sceneIds = row?.sceneId || ids.value
  await delScene(sceneIds)
  getList()
  proxy.$modal.msgSuccess('删除成功')
}

function handleExport() {
  proxy.download('cost/scene/export', {
    ...queryParams.value
  }, `scene_${new Date().getTime()}.xlsx`)
}

function handleOpenDictView() {
  router.push({
    path: COST_MENU_ROUTES.dict,
    query: {
      dictType: 'cost_',
      scope: 'cost'
    }
  })
}

function handleOpenBusinessDomain() {
  router.push({
    path: COST_MENU_ROUTES.dict,
    query: {
      dictType: 'cost_business_domain',
      scope: 'cost'
    }
  })
}

function handleOpenPublishCenter(row) {
  router.push({
    path: COST_MENU_ROUTES.publish,
    query: {
      sceneId: row.sceneId
    }
  })
}

function handleOpenTaskCenter(row) {
  router.push({
    path: COST_MENU_ROUTES.task,
    query: {
      sceneId: row.sceneId
    }
  })
}

function handleOpenPublishAudit() {
  if (!currentSceneInfo.value.sceneId) {
    return
  }
  router.push({
    path: COST_MENU_ROUTES.publishAudit,
    query: {
      sceneId: currentSceneInfo.value.sceneId
    }
  })
}

async function handleSetCurrentScene(row) {
  const confirmed = await confirmCostSceneSwitch({
    currentSceneId: currentSceneInfo.value.sceneId,
    nextSceneId: row.sceneId,
    sceneOptions: sceneList.value,
    scope: '全局工作场景'
  })
  if (!confirmed) {
    return
  }
  setCostSceneContextId(row)
  currentSceneInfo.value = row
  loadCurrentScenePublishSummary(row)
  proxy.$modal.msgSuccess(`已将 ${row.sceneName} 设为当前工作场景`)
}

function handleClearCurrentScene() {
  setCostSceneContextId(undefined)
  currentSceneInfo.value = {}
  resetPublishSummary()
  resetSceneWorkbench()
  proxy.$modal.msgSuccess('已清除当前工作场景')
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
    defaultObjectDimension: data.defaultObjectDimension || '',
    status: data.status || '0',
    activeVersionId: data.activeVersionId,
    activeVersionNo: data.activeVersionNo || '',
    feeCount: Number(data.feeCount || 0),
    variableGroupCount: Number(data.variableGroupCount || 0),
    variableCount: Number(data.variableCount || 0),
    ruleCount: Number(data.ruleCount || 0),
    publishedVersionCount: Number(data.publishedVersionCount || 0),
    taskCount: Number(data.taskCount || 0),
    runningTaskCount: Number(data.runningTaskCount || 0),
    failedTaskCount: Number(data.failedTaskCount || 0),
    totalConfigCount: Number(data.totalConfigCount || 0),
    canDelete: Boolean(data.canDelete),
    canDisable: Boolean(data.canDisable),
    removeBlockingReason: data.removeBlockingReason || '当前场景可以删除',
    disableBlockingReason: data.disableBlockingReason || '当前场景可以停用',
    removeAdvice: data.removeAdvice || '',
    disableAdvice: data.disableAdvice || '',
    impactItems: Array.isArray(data.impactItems) ? data.impactItems : [],
    recentTasks: Array.isArray(data.recentTasks) ? data.recentTasks : []
  }
}

async function loadCurrentScenePublishSummary(scene) {
  const sceneId = scene?.sceneId
  if (!sceneId) {
    resetPublishSummary()
    resetSceneWorkbench()
    return
  }
  sceneVersionLoading.value = true
  try {
    const [governance, publishResponse, precheckResponse] = await Promise.all([
      fetchSceneGovernance(sceneId),
      listPublish({
        sceneId,
        pageNum: 1,
        pageSize: 8
      }),
      getPublishPrecheck(sceneId)
    ])
    const versionRows = Array.isArray(publishResponse?.rows) ? publishResponse.rows : []
    const latestVersion = versionRows[0]
    const validationMeta = resolveValidationMeta(latestVersion?.validationResultJson)
    Object.assign(publishSummary, {
      sceneId,
      publishedVersionCount: Number(governance.publishedVersionCount || 0),
      activeVersionNo: scene?.activeVersionNo || '',
      latestVersionNo: latestVersion?.versionNo || '',
      latestPublishedTime: latestVersion?.publishedTime ? proxy.parseTime(latestVersion.publishedTime) : '',
      validationLabel: validationMeta.label,
      validationTag: validationMeta.tag,
      validationNote: buildValidationNote(latestVersion?.validationResultJson)
    })
    scenePrecheck.value = normalizeScenePrecheck(precheckResponse?.data)
    sceneVersionLedger.value = versionRows
    sceneVersionTotal.value = Number(publishResponse?.total || versionRows.length || 0)
    initializeSceneCompare(versionRows)
  } catch (error) {
    Object.assign(publishSummary, {
      sceneId,
      publishedVersionCount: 0,
      activeVersionNo: scene?.activeVersionNo || '',
      latestVersionNo: '',
      latestPublishedTime: '',
      validationLabel: '未留痕',
      validationTag: 'info',
      validationNote: '暂未获取到发布校验信息'
    })
    scenePrecheck.value = createEmptyScenePrecheck()
    resetSceneWorkbench()
  } finally {
    sceneVersionLoading.value = false
  }
}

function createEmptyPublishSummary() {
  return {
    sceneId: undefined,
    publishedVersionCount: 0,
    activeVersionNo: '',
    latestVersionNo: '',
    latestPublishedTime: '',
    validationLabel: '未留痕',
    validationTag: 'info',
    validationNote: '请先设置工作场景'
  }
}

function resetPublishSummary() {
  Object.assign(publishSummary, createEmptyPublishSummary())
}

function createEmptyScenePrecheck() {
  return {
    checked: false,
    publishable: true,
    blockingCount: 0,
    warningCount: 0,
    impactedFeeCount: 0,
    items: [],
    impactedFees: []
  }
}

function normalizeScenePrecheck(data = {}) {
  return {
    ...createEmptyScenePrecheck(),
    ...data,
    checked: true
  }
}

function createEmptySceneCompareData() {
  return {
    summary: {},
    fromVersion: undefined,
    toVersion: undefined,
    fromScene: undefined,
    toScene: undefined,
    sceneDiffs: [],
    feeDiffs: [],
    ruleDiffs: []
  }
}

function resetSceneWorkbench() {
  sceneVersionLedger.value = []
  sceneVersionTotal.value = 0
  publishWorkbenchTab.value = 'publish'
  sceneCompareForm.fromVersionId = undefined
  sceneCompareForm.toVersionId = undefined
  sceneCompareData.value = createEmptySceneCompareData()
  scenePrecheck.value = createEmptyScenePrecheck()
}

function initializeSceneCompare(rows = []) {
  if (!rows.length) {
    resetSceneWorkbench()
    return
  }
  sceneVersionLedger.value = rows
  if (rows.length < 2) {
    sceneCompareForm.fromVersionId = undefined
    sceneCompareForm.toVersionId = rows[0]?.versionId
    sceneCompareData.value = createEmptySceneCompareData()
    return
  }

  const targetVersion = rows[0]
  const activeVersion = currentSceneInfo.value?.activeVersionNo
    ? rows.find(item => item.versionNo === currentSceneInfo.value.activeVersionNo)
    : undefined
  const fallbackBase = rows.find(item => item.versionId !== targetVersion.versionId)
  const baseVersion = activeVersion && activeVersion.versionId !== targetVersion.versionId ? activeVersion : fallbackBase

  sceneCompareForm.fromVersionId = baseVersion?.versionId
  sceneCompareForm.toVersionId = targetVersion.versionId
  loadSceneCompareDiff()
}

function handleUseAsCompareTarget(row) {
  publishWorkbenchTab.value = 'compare'
  sceneCompareForm.toVersionId = row.versionId
  if (!sceneCompareForm.fromVersionId || sceneCompareForm.fromVersionId === row.versionId) {
    const fallback = sceneVersionLedger.value.find(item => item.versionId !== row.versionId)
    sceneCompareForm.fromVersionId = fallback?.versionId
  }
  loadSceneCompareDiff()
}

function handleSwapSceneCompare() {
  if (!sceneCompareReady.value) {
    return
  }
  const nextFrom = sceneCompareForm.toVersionId
  sceneCompareForm.toVersionId = sceneCompareForm.fromVersionId
  sceneCompareForm.fromVersionId = nextFrom
  loadSceneCompareDiff()
}

async function handleScenePrecheck() {
  if (!currentSceneInfo.value.sceneId) {
    proxy.$modal.msgWarning('请先设置当前工作场景')
    return
  }
  scenePublishLoading.value = true
  try {
    const response = await getPublishPrecheck(currentSceneInfo.value.sceneId)
    scenePrecheck.value = normalizeScenePrecheck(response?.data)
  } finally {
    scenePublishLoading.value = false
  }
}

async function loadSceneCompareDiff() {
  if (!sceneCompareReady.value) {
    sceneCompareData.value = createEmptySceneCompareData()
    return
  }
  sceneDiffLoading.value = true
  try {
    const response = await getPublishDiff({
      fromVersionId: sceneCompareForm.fromVersionId,
      toVersionId: sceneCompareForm.toVersionId
    })
    sceneCompareData.value = response.data || createEmptySceneCompareData()
  } finally {
    sceneDiffLoading.value = false
  }
}

function buildSceneSnapshotFromDiffs(rows = [], valueKey) {
  if (!Array.isArray(rows) || !rows.length) {
    return {}
  }
  return rows.reduce((result, item) => {
    if (item?.field) {
      result[item.field] = item?.[valueKey]
    }
    return result
  }, {})
}

function parseValidationResult(value) {
  if (!value) {
    return {}
  }
  if (typeof value === 'string') {
    try {
      return JSON.parse(value)
    } catch {
      return {}
    }
  }
  return value
}

function resolveValidationMeta(value) {
  const payload = parseValidationResult(value?.validationResultJson || value)
  const items = Array.isArray(payload.items) ? payload.items : []
  const blockingCount = Number(payload.blockingCount ?? items.filter(item => item.level === 'BLOCK').length ?? 0)
  const warningCount = Number(payload.warningCount ?? items.filter(item => item.level === 'WARN').length ?? 0)
  if (!Object.keys(payload).length) {
    return { label: '未留痕', tag: 'info', blockingCount: 0, warningCount: 0 }
  }
  if (blockingCount > 0 || payload.publishable === false) {
    return { label: '阻断', tag: 'danger', blockingCount, warningCount }
  }
  if (warningCount > 0) {
    return { label: '告警通过', tag: 'warning', blockingCount, warningCount }
  }
  return { label: '通过', tag: 'success', blockingCount, warningCount }
}

function buildValidationNote(value) {
  const meta = resolveValidationMeta(value)
  if (meta.label === '未留痕') {
    return '未记录检查快照'
  }
  return `阻断 ${meta.blockingCount} / 告警 ${meta.warningCount}`
}

function resolveFeeImpactSummary(item = {}) {
  if (item.summaryText) {
    return item.summaryText
  }
  if (item.summary) {
    const details = []
    const changedRules = resolveChangedRulePreview(item)
    const changedVariables = resolveChangedVariablePreview(item)
    if (changedRules) {
      details.push(`规则：${changedRules}`)
    }
    if (changedVariables) {
      details.push(`变量：${changedVariables}`)
    }
    return details.length ? `${item.summary} 涉及${details.join('；')}。` : item.summary
  }
  return `费用 ${item.feeName || item.feeCode} 发生“${resolveCostChangeTypeLabel(item.changeType)}”，规则变化 ${item.ruleChangeCount || 0} 处，变量变化 ${item.variableChangeCount || 0} 处。`
}

function resolveChangedAssetPreview(items = [], codeKey, nameKey) {
  const source = Array.isArray(items) ? items : []
  if (!source.length) {
    return ''
  }
  const labels = source.slice(0, 3).map((item) => {
    const name = item?.[nameKey]
    const code = item?.[codeKey]
    if (name && code && name !== code) {
      return `${name}（${code}）`
    }
    return name || code || '-'
  })
  const suffix = source.length > 3 ? ` 等 ${source.length} 项` : ''
  return `${labels.join('、')}${suffix}`
}

function resolveChangedVariablePreview(item = {}) {
  return resolveChangedAssetPreview(item.changedVariables, 'variableCode', 'variableName')
}

function resolveChangedRulePreview(item = {}) {
  return resolveChangedAssetPreview(item.changedRules, 'ruleCode', 'ruleName')
}

function normalizeDeptTreeOptions(nodes = []) {
  return nodes.map(node => ({
    id: String(node.id),
    label: node.label,
    disabled: Boolean(node.disabled),
    children: Array.isArray(node.children) ? normalizeDeptTreeOptions(node.children) : []
  }))
}

function buildDeptLabelMap(nodes = [], bucket = {}) {
  nodes.forEach(node => {
    bucket[node.id] = node.label
    if (Array.isArray(node.children) && node.children.length) {
      buildDeptLabelMap(node.children, bucket)
    }
  })
  return bucket
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

function resolveTaskProgressStatus(value) {
  if (value === 'SUCCESS') {
    return 'success'
  }
  if (value === 'FAILED' || value === 'CANCELLED') {
    return 'exception'
  }
  if (value === 'PART_SUCCESS') {
    return 'warning'
  }
  return undefined
}

function resolveOrgLabel(value) {
  if (value === null || value === undefined || value === '') {
    return '-'
  }
  return deptLabelMap.value[String(value)] || formatLegacyOrgLabel(value)
}

async function ensureDisableAllowed() {
  if (!form.value.sceneId || form.value.status !== '1' || initialStatus.value === '1') {
    return true
  }
  const check = await fetchSceneGovernance(form.value.sceneId)
  const checks = [check]
  const allowed = await confirmCostDisableImpact({
    checks,
    targetLabel: '场景',
    targetNames: [form.value.sceneName || check.sceneName]
  })
  if (!allowed) {
    const blockedCheck = findFirstDisableBlockedCheck(checks)
    if (blockedCheck) {
      openGovernanceDrawer(blockedCheck)
    }
    return false
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

.scene-center__hero-tag {
  display: inline-flex;
  align-items: center;
  min-height: 32px;
  padding: 0 15px;
  line-height: 1.2;
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

.scene-center__publish-workbench {
  display: grid;
  gap: 18px;
  padding: 20px 22px;
  border: 1px solid var(--el-border-color-light);
  border-radius: 18px;
  background: var(--el-bg-color-overlay);
}

.scene-center__publish-workbench-head {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: flex-start;
}

.scene-center__publish-workbench-eyebrow {
  color: var(--el-color-success);
  font-size: 13px;
  font-weight: 700;
}

.scene-center__publish-workbench-title {
  margin-top: 6px;
  font-size: 20px;
  font-weight: 700;
  color: var(--el-text-color-primary);
}

.scene-center__publish-workbench-desc {
  margin-top: 8px;
  max-width: 760px;
  color: var(--el-text-color-regular);
  line-height: 1.8;
}

.scene-center__publish-workbench-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.scene-center__publish-tabs :deep(.el-tabs__header) {
  margin-bottom: 14px;
}

.scene-center__publish-exec {
  display: grid;
  gap: 16px;
}

.scene-center__publish-exec-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 14px;
}

.scene-center__publish-exec-card {
  display: grid;
  gap: 8px;
  padding: 16px 18px;
  border-radius: 14px;
  border: 1px solid var(--el-border-color-light);
  background: color-mix(in srgb, var(--el-color-primary-light-9) 16%, var(--el-bg-color-overlay));
}

.scene-center__publish-exec-card span,
.scene-center__publish-exec-card small {
  color: var(--el-text-color-secondary);
}

.scene-center__publish-exec-card strong {
  font-size: 24px;
  line-height: 1.2;
  color: var(--el-text-color-primary);
}

.scene-center__publish-exec-status {
  display: grid;
  gap: 8px;
}

.scene-center__publish-form {
  padding: 18px 18px 2px;
  border-radius: 14px;
  border: 1px solid var(--el-border-color-light);
  background: var(--el-bg-color-overlay);
}

.scene-center__publish-action-row {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}

.scene-center__publish-boundary {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 14px;
  padding: 14px 16px;
  border: 1px solid var(--el-border-color-light);
  border-radius: 14px;
  background: color-mix(in srgb, var(--el-color-warning-light-9) 28%, var(--el-bg-color-overlay));
}

.scene-center__publish-boundary div {
  display: grid;
  gap: 5px;
}

.scene-center__publish-boundary strong {
  color: var(--el-text-color-primary);
}

.scene-center__publish-boundary span {
  color: var(--el-text-color-secondary);
  font-size: 13px;
  line-height: 1.7;
}

.scene-center__publish-exec-alert,
.scene-center__publish-check-table {
  margin-top: 4px;
}

.scene-center__publish-check-summary {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 14px;
}

.scene-center__publish-check-card {
  display: grid;
  gap: 8px;
  padding: 16px 18px;
  border-radius: 14px;
  border: 1px solid var(--el-border-color-light);
  background: color-mix(in srgb, var(--el-color-success-light-9) 16%, var(--el-bg-color-overlay));
}

.scene-center__publish-check-card span,
.scene-center__publish-check-card small {
  color: var(--el-text-color-secondary);
}

.scene-center__publish-check-card strong {
  font-size: 24px;
  line-height: 1.2;
  color: var(--el-text-color-primary);
}

.scene-center__publish-impact {
  display: grid;
  gap: 12px;
  padding: 16px 18px;
  border-radius: 14px;
  border: 1px solid var(--el-border-color-light);
  background: color-mix(in srgb, var(--el-color-primary-light-9) 18%, var(--el-bg-color-overlay));
}

.scene-center__publish-impact-title {
  font-size: 16px;
  font-weight: 700;
  color: var(--el-text-color-primary);
}

.scene-center__publish-impact-list {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
}

.scene-center__publish-impact-card {
  display: grid;
  gap: 6px;
  padding: 14px 16px;
  border-radius: 12px;
  border: 1px solid var(--el-border-color-light);
  background: var(--el-bg-color-overlay);
}

.scene-center__publish-impact-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 10px;
}

.scene-center__publish-impact-card strong {
  color: var(--el-text-color-primary);
}

.scene-center__publish-impact-metrics {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.scene-center__publish-impact-metrics span {
  display: inline-flex;
  align-items: center;
  padding: 4px 10px;
  border-radius: 999px;
  background: color-mix(in srgb, var(--el-color-primary-light-9) 36%, var(--el-bg-color-page));
  color: var(--el-text-color-secondary);
  font-size: 12px;
}

.scene-center__publish-impact-details {
  display: grid;
  gap: 4px;
}

.scene-center__publish-impact-detail-label {
  font-size: 12px;
  font-weight: 600;
  color: var(--el-text-color-regular);
}

.scene-center__publish-impact-card span,
.scene-center__publish-impact-card small {
  color: var(--el-text-color-secondary);
}

.scene-center__ledger-metrics {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 14px;
  margin-bottom: 14px;
}

.scene-center__ledger-card {
  display: grid;
  gap: 8px;
  padding: 16px 18px;
  border-radius: 14px;
  border: 1px solid var(--el-border-color-light);
  background: color-mix(in srgb, var(--el-color-success-light-9) 24%, var(--el-bg-color-overlay));
}

.scene-center__ledger-card span,
.scene-center__ledger-card small {
  color: var(--el-text-color-secondary);
}

.scene-center__ledger-card strong {
  font-size: 24px;
  line-height: 1.2;
  color: var(--el-text-color-primary);
}

.scene-center__compare-toolbar {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
}

.scene-center__compare-vs {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 42px;
  height: 40px;
  border-radius: 12px;
  border: 1px solid var(--el-border-color-light);
  color: var(--el-color-primary);
  font-weight: 700;
  background: color-mix(in srgb, var(--el-color-primary-light-9) 24%, var(--el-bg-color-overlay));
}

.scene-center__compare-alert,
.scene-center__compare-summary {
  margin-bottom: 16px;
}

.scene-center__compare-body {
  display: grid;
  gap: 16px;
}

.scene-center__compare-impact {
  display: grid;
  gap: 12px;
  padding: 16px 18px;
  border-radius: 14px;
  border: 1px solid var(--el-border-color-light);
  background: color-mix(in srgb, var(--el-color-warning-light-9) 20%, var(--el-bg-color-overlay));
}

.scene-center__compare-impact-title {
  font-size: 16px;
  font-weight: 700;
  color: var(--el-text-color-primary);
}

.scene-center__compare-impact-list {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
}

.scene-center__compare-impact-card {
  display: grid;
  gap: 6px;
  padding: 14px 16px;
  border-radius: 12px;
  border: 1px solid var(--el-border-color-light);
  background: var(--el-bg-color-overlay);
}

.scene-center__compare-impact-card strong {
  color: var(--el-text-color-primary);
}

.scene-center__compare-impact-card span,
.scene-center__compare-impact-card small {
  color: var(--el-text-color-secondary);
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

.scene-center__dialog-alert {
  margin-bottom: 18px;
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

.scene-governance__recent {
  display: grid;
  gap: 12px;
  padding: 14px;
  border: 1px solid var(--el-border-color-light);
  border-radius: 8px;
  background: var(--el-bg-color-overlay);
}

.scene-governance__recent-head {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: flex-start;
}

.scene-governance__recent-head strong {
  display: block;
  font-size: 15px;
  color: var(--el-text-color-primary);
}

.scene-governance__recent-head small {
  display: block;
  margin-top: 4px;
  color: var(--el-text-color-secondary);
}

.scene-governance__task-list {
  display: grid;
  gap: 10px;
}

.scene-governance__task-item {
  display: grid;
  gap: 8px;
  padding: 12px;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 8px;
  background: var(--el-fill-color-extra-light);
}

.scene-governance__task-head {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: flex-start;
}

.scene-governance__task-head strong,
.scene-governance__task-head span {
  display: block;
}

.scene-governance__task-head span,
.scene-governance__task-meta {
  color: var(--el-text-color-secondary);
  font-size: 12px;
}

.scene-governance__task-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 6px 14px;
}

.scene-governance__task-error {
  padding: 8px 10px;
  border-radius: 6px;
  color: var(--el-color-danger);
  background: color-mix(in srgb, var(--el-color-danger-light-9) 56%, var(--el-bg-color-overlay));
  font-size: 12px;
  line-height: 1.6;
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
  .scene-center__metrics,
  .scene-center__publish-exec-grid,
  .scene-center__publish-check-summary,
  .scene-center__ledger-metrics,
  .scene-center__compare-impact-list,
  .scene-center__publish-impact-list {
    grid-template-columns: 1fr;
  }

  .scene-governance__grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .scene-center__publish-workbench-head {
    flex-direction: column;
  }
}
</style>


<template>
  <div class="app-container publish-center">
    <section v-show="!isCompactMode" class="publish-center__hero">
      <div>
        <div class="publish-center__eyebrow">发布治理</div>
        <h2 class="publish-center__title">发布中心 · 跨场景总台</h2>
        <p class="publish-center__subtitle">
          正式发布、生效切换、回滚、审计和跨版本差异统一在这里执行；场景中心只保留影响预览和版本观察，避免配置维护页直接误发布。
        </p>
      </div>
      <div class="publish-center__hero-side">
        <el-tag type="success">唯一正式发布入口</el-tag>
        <el-tag type="warning" effect="plain">检查先行</el-tag>
        <el-tag type="info" effect="plain">全链路留痕</el-tag>
      </div>
    </section>

    <section v-show="!isCompactMode" class="publish-center__metrics">
      <div v-for="item in metricItems" :key="item.label" class="publish-center__metric-card">
        <span>{{ item.label }}</span>
        <strong>{{ item.value }}</strong>
        <small>{{ item.desc }}</small>
      </div>
    </section>

    <section class="publish-center__console">
      <el-tabs v-model="publishTab" class="publish-center__tabs">
        <el-tab-pane name="release">
          <template #label>
            <span class="publish-center__tab-label">发布执行</span>
          </template>

          <div class="publish-center__precheck">
            <div class="publish-center__section-head">
              <div>
                <h3>发布执行台</h3>
                <p>企业级发布入口收敛在这里：先做阻断校验，再生成版本，最后按权限决定是否立即生效。</p>
              </div>
            </div>

            <div v-show="!isCompactMode" class="publish-center__stage-strip">
              <div class="publish-center__stage-card">
                <span>STEP 1</span>
                <strong>选择场景</strong>
                <small>明确本次发布对应的核算工作场景。</small>
              </div>
              <div class="publish-center__stage-card">
                <span>STEP 2</span>
                <strong>发布检查</strong>
                <small>先看阻断、告警、费用和变量影响。</small>
              </div>
              <div class="publish-center__stage-card">
                <span>STEP 3</span>
                <strong>生成版本</strong>
                <small>正式写入版本台账，按权限决定是否立即生效。</small>
              </div>
            </div>

            <div v-show="!isCompactMode" class="publish-center__boundary">
              <div>
                <strong>职责边界</strong>
                <span>场景中心负责配置维护和发布影响预览；发布中心负责正式版本生成、生效切换、回滚和审计。</span>
              </div>
              <el-tag type="primary" effect="plain">职责分离</el-tag>
            </div>

            <el-form :model="publishForm" label-width="92px" class="publish-center__release-form">
              <el-form-item label="发布场景" required>
                <el-select v-model="publishForm.sceneId" filterable placeholder="请选择要发布的场景" style="width: 100%" @change="handlePublishSceneChange">
                  <el-option v-for="item in sceneOptions" :key="item.sceneId" :label="`${item.sceneName} / ${item.sceneCode}`" :value="item.sceneId" />
                </el-select>
              </el-form-item>
              <el-form-item label="发布说明" required>
                <el-input v-model="publishForm.publishDesc" type="textarea" :rows="4" maxlength="1000" show-word-limit placeholder="请输入本次发布说明、影响范围与业务口径摘要" />
              </el-form-item>
              <el-form-item label="发布动作">
                <el-checkbox v-model="publishForm.activateNow">生成版本后立即设为生效</el-checkbox>
              </el-form-item>
            </el-form>

            <div class="publish-center__action-row">
              <el-button type="primary" icon="CircleCheck" @click="handlePrecheck">发布前检查</el-button>
              <el-button type="success" icon="Promotion" @click="handlePublish" v-hasPermi="['cost:publish:add']">生成版本</el-button>
            </div>

            <PublishPrecheckPanel
              v-if="precheck.sceneId"
              :data="precheck"
              :columns="4"
              :impact-columns="3"
              show-conclusion
              show-empty
              impact-subtitle="发布中心只展示本次发布真正影响到的费用、规则和变量口径。"
            />

            <div v-if="precheck.sceneId" class="publish-center__draft-diff">
              <div class="publish-center__draft-diff-head">
                <div>
                  <h4>发布前差异预览</h4>
                  <p>{{ publishDraftDiffBaselineText }}</p>
                </div>
                <el-tag :type="hasPublishDraftDiff ? 'warning' : 'success'">{{ hasPublishDraftDiff ? '存在变化' : '无变化' }}</el-tag>
              </div>
              <el-alert
                v-if="!hasPublishDraftDiff"
                title="当前草稿与上一发布版本快照一致，本次不会产生费用、变量或规则差异。"
                type="success"
                :closable="false"
                class="publish-center__diff-tip"
              />
              <template v-else>
                <el-descriptions :column="3" border>
                  <el-descriptions-item label="费用变化">{{ publishDraftDiff.summary?.feeChangeCount || 0 }}（新增 {{ publishDraftDiff.summary?.addedFeeCount || 0 }} / 修改 {{ publishDraftDiff.summary?.changedFeeCount || 0 }} / 删除 {{ publishDraftDiff.summary?.removedFeeCount || 0 }}）</el-descriptions-item>
                  <el-descriptions-item label="变量变化">{{ publishDraftDiff.summary?.variableChangeCount || 0 }}（新增 {{ publishDraftDiff.summary?.addedVariableCount || 0 }} / 修改 {{ publishDraftDiff.summary?.changedVariableCount || 0 }} / 删除 {{ publishDraftDiff.summary?.removedVariableCount || 0 }}）</el-descriptions-item>
                  <el-descriptions-item label="规则变化">{{ publishDraftDiff.summary?.ruleChangeCount || 0 }}（新增 {{ publishDraftDiff.summary?.addedRuleCount || 0 }} / 修改 {{ publishDraftDiff.summary?.changedRuleCount || 0 }} / 删除 {{ publishDraftDiff.summary?.removedRuleCount || 0 }}）</el-descriptions-item>
                </el-descriptions>
                <el-tabs class="publish-center__draft-diff-tabs">
                  <el-tab-pane label="费用">
                    <el-table :data="publishDraftDiff.feeDiffs || []" size="small">
                      <el-table-column label="费用编码" prop="feeCode" width="160" />
                      <el-table-column label="费用名称" prop="feeName" min-width="180" />
                      <el-table-column label="变化类型" width="110" align="center">
                        <template #default="scope">
                          <el-tag :type="resolveCostChangeTypeMeta(scope.row.changeType).type">{{ resolveCostChangeTypeLabel(scope.row.changeType) }}</el-tag>
                        </template>
                      </el-table-column>
                      <el-table-column label="字段变化" min-width="220">
                        <template #default="scope">{{ formatChangedFields(scope.row.changedFields) }}</template>
                      </el-table-column>
                    </el-table>
                  </el-tab-pane>
                  <el-tab-pane label="变量">
                    <el-table :data="publishDraftDiff.variableDiffs || []" size="small">
                      <el-table-column label="变量编码" prop="variableCode" width="180" />
                      <el-table-column label="变量名称" prop="variableName" min-width="180" />
                      <el-table-column label="变化类型" width="110" align="center">
                        <template #default="scope">
                          <el-tag :type="resolveCostChangeTypeMeta(scope.row.changeType).type">{{ resolveCostChangeTypeLabel(scope.row.changeType) }}</el-tag>
                        </template>
                      </el-table-column>
                      <el-table-column label="字段变化" min-width="220">
                        <template #default="scope">{{ formatChangedFields(scope.row.changedFields) }}</template>
                      </el-table-column>
                    </el-table>
                  </el-tab-pane>
                  <el-tab-pane label="规则">
                    <el-table :data="publishDraftDiff.ruleDiffs || []" size="small">
                      <el-table-column label="规则编码" prop="ruleCode" width="180" />
                      <el-table-column label="规则名称" prop="ruleName" min-width="180" />
                      <el-table-column label="费用编码" prop="feeCode" width="160" />
                      <el-table-column label="变化类型" width="110" align="center">
                        <template #default="scope">
                          <el-tag :type="resolveCostChangeTypeMeta(scope.row.changeType).type">{{ resolveCostChangeTypeLabel(scope.row.changeType) }}</el-tag>
                        </template>
                      </el-table-column>
                      <el-table-column label="明细变化" min-width="220">
                        <template #default="scope">字段 {{ formatChangedFields(scope.row.changedFields) }}；条件 {{ scope.row.conditionChangeCount || 0 }}；阶梯 {{ scope.row.tierChangeCount || 0 }}</template>
                      </el-table-column>
                    </el-table>
                  </el-tab-pane>
                </el-tabs>
              </template>
            </div>
          </div>
        </el-tab-pane>

        <el-tab-pane name="ledger">
          <template #label>
            <span class="publish-center__tab-label">版本台账</span>
          </template>

          <div class="publish-center__ledger">
            <div class="publish-center__section-head">
              <div>
                <h3>跨场景版本台账</h3>
                <p>统一查看各场景版本详情、快照对象、差异对比、生效切换与回滚。</p>
              </div>
              <div class="publish-center__ledger-actions">
                <right-toolbar v-model:showSearch="showSearch" @queryTable="getList" />
                <el-button plain icon="Histogram" @click="handleOpenAudit" v-hasPermi="['cost:publish:list']">发布审计</el-button>
              </div>
            </div>

            <el-form ref="queryRef" :model="queryParams" :inline="true" label-width="84px" v-show="showSearch" class="publish-center__query">
              <el-form-item label="所属场景" prop="sceneId">
                <el-select v-model="queryParams.sceneId" clearable filterable placeholder="请选择场景" style="width: 240px" @change="handleQuerySceneChange">
                  <el-option v-for="item in sceneOptions" :key="item.sceneId" :label="`${item.sceneName} / ${item.sceneCode}`" :value="item.sceneId" />
                </el-select>
              </el-form-item>
              <el-form-item label="业务域" prop="businessDomain">
                <el-select v-model="queryParams.businessDomain" clearable placeholder="请选择业务域" style="width: 180px">
                  <el-option v-for="item in businessDomainOptions" :key="item.value" :label="item.label" :value="item.value" />
                </el-select>
              </el-form-item>
              <el-form-item label="版本状态" prop="versionStatus">
                <el-select v-model="queryParams.versionStatus" clearable placeholder="请选择版本状态" style="width: 180px">
                  <el-option v-for="item in versionStatusOptions" :key="item.value" :label="item.label" :value="item.value" />
                </el-select>
              </el-form-item>
              <el-form-item label="版本号" prop="versionNo">
                <el-input v-model="queryParams.versionNo" clearable style="width: 180px" @keyup.enter="handleQuery" />
              </el-form-item>
              <el-form-item>
                <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
                <el-button icon="Refresh" @click="resetQuery">重置</el-button>
              </el-form-item>
            </el-form>

            <el-table v-loading="loading" :data="versionList">
              <el-table-column label="版本号" prop="versionNo" width="150" align="center" />
              <el-table-column label="场景" min-width="180" align="center">
                <template #default="scope">{{ scope.row.sceneName }} ({{ scope.row.sceneCode }})</template>
              </el-table-column>
              <el-table-column label="状态" width="120" align="center">
                <template #default="scope">
                  <dict-tag :options="versionStatusOptions" :value="scope.row.versionStatus" />
                </template>
              </el-table-column>
              <el-table-column label="检查结果" width="170" align="center">
                <template #default="scope">
                  <el-tag :type="resolveValidationMeta(scope.row).tag">{{ resolveValidationMeta(scope.row).label }}</el-tag>
                  <div class="publish-center__validation-note">{{ buildValidationNote(scope.row) }}</div>
                </template>
              </el-table-column>
              <el-table-column label="发布说明" prop="publishDesc" min-width="220" :show-overflow-tooltip="true" />
              <el-table-column label="发布人" prop="publishedBy" width="120" align="center" />
              <el-table-column label="发布时间" width="180" align="center">
                <template #default="scope">{{ parseTime(scope.row.publishedTime) }}</template>
              </el-table-column>
              <el-table-column label="操作" width="230" fixed="right" align="center">
                <template #default="scope">
                  <div class="cost-row-actions">
                    <el-button link type="primary" icon="View" @click="handleDetail(scope.row)">详情</el-button>
                    <el-button link type="primary" icon="Tickets" @click="handleDiff(scope.row)">差异</el-button>
                    <el-dropdown trigger="click" @command="command => handlePublishRowCommand(command, scope.row)">
                      <el-button link type="primary" icon="MoreFilled">更多</el-button>
                      <template #dropdown>
                        <el-dropdown-menu>
                          <el-dropdown-item command="activate" icon="Select" v-hasPermi="['cost:publish:activate']">设为生效</el-dropdown-item>
                          <el-dropdown-item command="rollback" icon="RefreshLeft" v-hasPermi="['cost:publish:rollback']">回滚到此版本</el-dropdown-item>
                        </el-dropdown-menu>
                      </template>
                    </el-dropdown>
                  </div>
                </template>
              </el-table-column>
              <template #empty>
                <cost-table-empty
                  title="当前没有发布版本"
                  description="发布版本会固化场景下的费用、变量和规则快照。可以先完成发布前检查，再生成首个版本。"
                >
                  <el-button type="primary" icon="CircleCheck" @click="handlePrecheck">发布前检查</el-button>
                  <el-button type="success" icon="Promotion" @click="handlePublish" v-hasPermi="['cost:publish:add']">生成版本</el-button>
                  <el-button icon="Refresh" @click="resetQuery">清空筛选</el-button>
                </cost-table-empty>
              </template>
            </el-table>

            <pagination v-show="total > 0" :total="total" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" @pagination="getList" />
          </div>
        </el-tab-pane>
      </el-tabs>
    </section>

    <el-drawer v-model="detailOpen" title="版本详情" size="980px" append-to-body>
      <div v-if="detailData.version">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="版本号">{{ detailData.version.versionNo }}</el-descriptions-item>
          <el-descriptions-item label="当前状态">{{ resolveVersionLabel(detailData.version.versionStatus) }}</el-descriptions-item>
          <el-descriptions-item label="场景">{{ detailData.version.sceneName }}</el-descriptions-item>
          <el-descriptions-item label="上一版本">{{ detailData.previousVersionNo || '首发版本' }}</el-descriptions-item>
          <el-descriptions-item label="发布时间">{{ parseTime(detailData.version.publishedTime) }}</el-descriptions-item>
          <el-descriptions-item label="发布人">{{ detailData.version.publishedBy }}</el-descriptions-item>
          <el-descriptions-item label="检查结果">
            <el-tag :type="detailValidationMeta.tag">{{ detailValidationMeta.label }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="检查摘要">{{ buildValidationNote(detailData.validationResult) }}</el-descriptions-item>
        </el-descriptions>

        <div class="publish-center__filter-row">
          <el-select v-model="detailFeeCode" clearable placeholder="按费用筛选快照对象" style="width: 280px" @change="reloadDetail">
            <el-option v-for="item in detailData.impactedFees || []" :key="item.feeCode" :label="`${item.feeName} / ${item.feeCode}`" :value="item.feeCode" />
          </el-select>
        </div>

        <el-tabs>
          <el-tab-pane label="发布检查">
            <PublishPrecheckPanel
              :data="detailValidation"
              :impact-fees="detailData.impactedFees || detailValidation.impactedFees || []"
              version-label="发布资格"
              :empty-version-text="detailValidation.publishable === false ? '阻断' : '可发布'"
              version-description="该版本发布检查留痕中的最终结论"
              show-empty
              impact-subtitle="版本详情中记录的费用影响明细，可用于回看发布当时的业务影响范围。"
            />
          </el-tab-pane>
          <el-tab-pane label="受影响费用">
            <PublishImpactFeeList
              :fees="detailData.impactedFees || []"
              subtitle="按费用主线展示发布快照中记录的规则和变量变化。"
            />
          </el-tab-pane>
          <el-tab-pane label="快照对象">
            <el-descriptions :column="3" border>
              <el-descriptions-item label="场景">{{ detailData.snapshotCounts?.scene || 0 }}</el-descriptions-item>
              <el-descriptions-item label="费用">{{ detailData.snapshotCounts?.fee || 0 }}</el-descriptions-item>
              <el-descriptions-item label="变量">{{ detailData.snapshotCounts?.variable || 0 }}</el-descriptions-item>
              <el-descriptions-item label="规则">{{ detailData.snapshotCounts?.rule || 0 }}</el-descriptions-item>
              <el-descriptions-item label="条件">{{ detailData.snapshotCounts?.condition || 0 }}</el-descriptions-item>
              <el-descriptions-item label="阶梯">{{ detailData.snapshotCounts?.tier || 0 }}</el-descriptions-item>
            </el-descriptions>
            <el-collapse class="publish-center__collapse">
              <el-collapse-item title="费用快照" name="fee"><JsonEditor :model-value="detailData.snapshotGroups?.fees || []" title="费用快照" readonly :rows="10" /></el-collapse-item>
              <el-collapse-item title="变量快照" name="variable"><JsonEditor :model-value="detailData.snapshotGroups?.variables || []" title="变量快照" readonly :rows="10" /></el-collapse-item>
              <el-collapse-item title="规则快照" name="rule"><JsonEditor :model-value="detailData.snapshotGroups?.rules || []" title="规则快照" readonly :rows="10" /></el-collapse-item>
            </el-collapse>
          </el-tab-pane>
        </el-tabs>
      </div>
    </el-drawer>
    <el-drawer v-model="diffOpen" title="版本差异" size="1320px" append-to-body>
      <div v-if="diffForm.toVersionId">
        <div class="publish-center__filter-row">
          <el-select v-model="diffForm.fromVersionId" placeholder="请选择基准版本" style="width: 220px" @change="loadDiff">
            <el-option v-for="item in diffVersionOptions" :key="item.versionId" :label="item.versionNo" :value="item.versionId" />
          </el-select>
          <el-select v-model="diffFeeCode" clearable placeholder="按费用筛选差异" style="width: 260px" :disabled="!diffForm.fromVersionId" @change="loadDiff">
            <el-option v-for="item in diffData.feeDiffs || []" :key="item.feeCode" :label="`${item.feeName} / ${item.feeCode}`" :value="item.feeCode" />
          </el-select>
        </div>

        <div class="publish-center__compare-head">
          <div class="publish-center__compare-card">
            <span class="publish-center__compare-label">基准版本</span>
            <strong>{{ diffData.fromVersion?.versionNo || '待选择' }}</strong>
            <small>{{ diffData.fromVersion?.publishDesc || '左侧基准版本由用户自行选择后开始对比' }}</small>
          </div>
          <div class="publish-center__compare-arrow">VS</div>
          <div class="publish-center__compare-card publish-center__compare-card--target">
            <span class="publish-center__compare-label">当前选中版本</span>
            <strong>{{ diffData.toVersion?.versionNo || '-' }}</strong>
            <small>{{ diffData.toVersion?.publishDesc || '当前从版本台账点击差异进入的版本' }}</small>
          </div>
        </div>

        <el-alert
          v-if="!diffForm.fromVersionId"
          title="右侧已锁定当前选中版本，请先在左侧选择一个基准版本，再查看差异结果。"
          type="info"
          :closable="false"
          class="publish-center__diff-tip"
        />

        <template v-else>
        <el-alert
          v-if="!hasAnyDiff"
          title="当前两个版本的配置快照一致，没有场景、费用或规则差异；费用级筛选为空是正常结果。"
          type="success"
          :closable="false"
          class="publish-center__diff-tip"
        />
        <el-descriptions :column="4" border>
          <el-descriptions-item label="场景级变化">{{ diffData.summary?.sceneChangeCount || 0 }}</el-descriptions-item>
          <el-descriptions-item label="费用变化">{{ diffData.summary?.feeChangeCount || 0 }}</el-descriptions-item>
          <el-descriptions-item label="规则变化">{{ diffData.summary?.ruleChangeCount || 0 }}</el-descriptions-item>
          <el-descriptions-item label="新增费用">{{ diffData.summary?.addedFeeCount || 0 }}</el-descriptions-item>
        </el-descriptions>

        <el-tabs>
          <el-tab-pane label="场景差异">
            <JsonDiffViewer
              title="场景主数据"
              subtitle="先看字段级差异摘要，再看场景快照的左右并排对比。"
              :left-title="diffData.fromVersion?.versionNo || '-'"
              :right-title="diffData.toVersion?.versionNo || '-'"
              :left-value="sceneDiffLeftValue"
              :right-value="sceneDiffRightValue"
              :rows="12"
            />
          </el-tab-pane>
          <el-tab-pane label="费用级差异">
            <el-alert :title="selectedFeeDiff ? buildFeeDiffNarrative(selectedFeeDiff) : '请先在下方列表选择一条费用差异，下面会按左右两个版本并排展示。'" type="info" :closable="false" class="publish-center__diff-tip" />
            <el-table :data="diffData.feeDiffs || []" size="small" highlight-current-row row-key="feeCode" @current-change="handleFeeDiffRowChange">
              <el-table-column label="费用编码" prop="feeCode" width="150" />
              <el-table-column label="费用名称" prop="feeName" min-width="180" />
              <el-table-column label="变化类型" width="110" align="center">
                <template #default="scope">
                  <el-tag :type="resolveCostChangeTypeMeta(scope.row.changeType).type">{{ resolveCostChangeTypeMeta(scope.row.changeType).label }}</el-tag>
                </template>
              </el-table-column>
              <el-table-column label="规则变化数" prop="ruleChangeCount" width="120" />
              <el-table-column label="变量变化数" prop="variableChangeCount" width="120" />
              <el-table-column label="摘要" min-width="260">
                <template #default="scope">{{ resolveFeeImpactSummary(scope.row) }}</template>
              </el-table-column>
            </el-table>

            <div v-if="selectedFeeDiff" class="publish-center__diff-detail">
              <JsonDiffViewer
                title="费用主数据"
                subtitle="对比费用定义本身的字段变化。"
                :left-title="diffData.fromVersion?.versionNo || '-'"
                :right-title="diffData.toVersion?.versionNo || '-'"
                :left-value="selectedFeeDiff.fromFee"
                :right-value="selectedFeeDiff.toFee"
                :rows="12"
              />

              <JsonDiffViewer
                title="关联规则"
                subtitle="对比费用下挂规则快照的整体变化。"
                :left-title="diffData.fromVersion?.versionNo || '-'"
                :right-title="diffData.toVersion?.versionNo || '-'"
                :left-value="selectedFeeDiff.fromRules"
                :right-value="selectedFeeDiff.toRules"
                :rows="14"
              />

              <JsonDiffViewer
                title="引用变量"
                subtitle="对比该费用关联变量的快照变化。"
                :left-title="diffData.fromVersion?.versionNo || '-'"
                :right-title="diffData.toVersion?.versionNo || '-'"
                :left-value="selectedFeeDiff.fromVariables"
                :right-value="selectedFeeDiff.toVariables"
                :rows="12"
              />
            </div>
          </el-tab-pane>
          <el-tab-pane label="规则级差异">
            <el-alert :title="selectedRuleDiff ? buildRuleDiffNarrative(selectedRuleDiff) : '请先选择一条规则差异，下面会按规则主数据、条件明细、阶梯明细左右对齐展示。'" type="info" :closable="false" class="publish-center__diff-tip" />
            <el-table :data="filteredRuleDiffs" size="small" highlight-current-row row-key="ruleCode" @current-change="handleRuleDiffRowChange">
              <el-table-column label="费用编码" prop="feeCode" width="140" />
              <el-table-column label="规则编码" prop="ruleCode" width="180" />
              <el-table-column label="规则名称" prop="ruleName" min-width="180" />
              <el-table-column label="变化类型" width="110" align="center">
                <template #default="scope">
                  <el-tag :type="resolveCostChangeTypeMeta(scope.row.changeType).type">{{ resolveCostChangeTypeMeta(scope.row.changeType).label }}</el-tag>
                </template>
              </el-table-column>
              <el-table-column label="条件变化数" prop="conditionChangeCount" width="120" />
              <el-table-column label="阶梯变化数" prop="tierChangeCount" width="120" />
              <el-table-column label="变更字段" min-width="220"><template #default="scope">{{ (scope.row.changedFields || []).join('、') || '-' }}</template></el-table-column>
            </el-table>

            <div v-if="selectedRuleDiff" class="publish-center__diff-detail">
              <div class="publish-center__explain-panel">
                <div class="publish-center__explain-title">中文差异解释</div>
                <div class="publish-center__explain-list">
                  <div v-for="(line, index) in buildRuleExplainLines(selectedRuleDiff)" :key="`rule-explain-${index}`" class="publish-center__explain-item">
                    {{ line }}
                  </div>
                </div>
              </div>

              <JsonDiffViewer
                title="规则主数据"
                subtitle="对比规则自身的字段变化。"
                :left-title="diffData.fromVersion?.versionNo || '-'"
                :right-title="diffData.toVersion?.versionNo || '-'"
                :left-value="selectedRuleDiff.fromRule"
                :right-value="selectedRuleDiff.toRule"
                :rows="12"
              />

              <JsonDiffViewer
                title="条件明细"
                subtitle="对比规则条件清单的变化。"
                :left-title="diffData.fromVersion?.versionNo || '-'"
                :right-title="diffData.toVersion?.versionNo || '-'"
                :left-value="selectedRuleDiff.fromConditions"
                :right-value="selectedRuleDiff.toConditions"
                :rows="14"
              />

              <JsonDiffViewer
                title="阶梯明细"
                subtitle="对比规则阶梯配置的变化。"
                :left-title="diffData.fromVersion?.versionNo || '-'"
                :right-title="diffData.toVersion?.versionNo || '-'"
                :left-value="selectedRuleDiff.fromTiers"
                :right-value="selectedRuleDiff.toTiers"
                :rows="14"
              />
            </div>
          </el-tab-pane>
        </el-tabs>
        </template>
      </div>
    </el-drawer>
  </div>
</template>

<script setup name="CostPublish">
import { ElMessageBox } from 'element-plus'
import JsonEditor from '@/components/cost/JsonEditor.vue'
import JsonDiffViewer from '@/components/cost/JsonDiffViewer.vue'
import PublishImpactFeeList from '@/components/cost/publish/PublishImpactFeeList.vue'
import PublishPrecheckPanel from '@/components/cost/publish/PublishPrecheckPanel.vue'
import { activatePublishVersion, addPublishVersion, getPublishDiff, getPublishPrecheck, getPublishStats, getPublishVersion, listPublish, rollbackPublishVersion } from '@/api/cost/publish'
import { optionselectScene } from '@/api/cost/scene'
import useSettingsStore from '@/store/modules/settings'
import { resolveWorkingCostSceneId } from '@/utils/costSceneContext'
import { confirmCostSceneSwitch } from '@/utils/costSceneSwitchGuard'
import { COST_MENU_ROUTES } from '@/utils/costMenuRoutes'
import { confirmCostNextAction } from '@/utils/costNextAction'
import { resolveCostChangeTypeLabel, resolveCostChangeTypeMeta } from '@/utils/costDisplayLabels'
import { getRemoteDictOptionMap } from '@/utils/dictRemote'

const { proxy } = getCurrentInstance()
const route = useRoute()
const router = useRouter()
const settingsStore = useSettingsStore()
const isCompactMode = computed(() => settingsStore.costPageMode === 'COMPACT')

const loading = ref(false)
const showSearch = ref(true)
const publishTab = ref('release')
const total = ref(0)
const versionList = ref([])
const sceneOptions = ref([])
const businessDomainOptions = ref([])
const versionStatusOptions = ref([])
const stats = reactive({ sceneCount: 0, versionCount: 0, activeVersionCount: 0, rolledBackVersionCount: 0 })
const precheck = ref({ items: [], impactedFees: [] })
const detailOpen = ref(false)
const detailData = ref({})
const detailFeeCode = ref(undefined)
const diffOpen = ref(false)
const diffData = ref({})
const diffVersionOptions = ref([])
const diffFeeCode = ref(undefined)
const selectedFeeDiffCode = ref(undefined)
const selectedRuleDiffCode = ref(undefined)
const lastQuerySceneId = ref(undefined)
const lastPublishSceneId = ref(undefined)

const queryParams = reactive({
  pageNum: 1,
  pageSize: 10,
  sceneId: route.query.sceneId ? Number(route.query.sceneId) : undefined,
  businessDomain: undefined,
  versionStatus: undefined,
  versionNo: undefined
})

const publishForm = reactive({
  sceneId: route.query.sceneId ? Number(route.query.sceneId) : undefined,
  publishDesc: '',
  activateNow: false
})

const diffForm = reactive({
  fromVersionId: undefined,
  toVersionId: undefined
})

const metricItems = computed(() => [
  { label: '已发布场景', value: stats.sceneCount, desc: '当前筛选范围内已形成版本的场景数量' },
  { label: '版本总数', value: stats.versionCount, desc: '发布台账中的版本记录数量' },
  { label: '生效版本数', value: stats.activeVersionCount, desc: '当前处于生效中的版本数' },
  { label: '已回滚版本', value: stats.rolledBackVersionCount, desc: '历史上被回滚替换的版本数' }
])
const publishDraftDiff = computed(() => precheck.value.diffPreview || { summary: {}, feeDiffs: [], variableDiffs: [], ruleDiffs: [] })
const hasPublishDraftDiff = computed(() => Number(publishDraftDiff.value.summary?.totalChangeCount || 0) > 0)
const publishDraftDiffBaselineText = computed(() => {
  const versionNo = publishDraftDiff.value.previousVersionNo
  return versionNo
    ? `相对上一发布版本 ${versionNo} 展示本次草稿的费用、变量、规则增删改。`
    : '当前场景暂无上一发布版本，本次发布会把现有费用、变量、规则作为首版新增快照。'
})
const detailValidation = computed(() => parseValidationResult(detailData.value.validationResult))
const detailValidationMeta = computed(() => resolveValidationMeta(detailData.value.validationResult))
const hasAnyDiff = computed(() => {
  const summary = diffData.value.summary || {}
  return Number(summary.sceneChangeCount || 0) > 0
    || Number(summary.feeChangeCount || 0) > 0
    || Number(summary.ruleChangeCount || 0) > 0
})
const selectedFeeDiff = computed(() => {
  return (diffData.value.feeDiffs || []).find(item => item.feeCode === selectedFeeDiffCode.value)
})
const sceneDiffLeftValue = computed(() => diffData.value.fromScene ?? buildSceneSnapshotFromDiffs(diffData.value.sceneDiffs, 'fromValue'))
const sceneDiffRightValue = computed(() => diffData.value.toScene ?? buildSceneSnapshotFromDiffs(diffData.value.sceneDiffs, 'toValue'))

const filteredRuleDiffs = computed(() => {
  const rows = diffData.value.ruleDiffs || []
  if (!selectedFeeDiffCode.value) {
    return rows
  }
  return rows.filter(item => item.feeCode === selectedFeeDiffCode.value)
})

const selectedRuleDiff = computed(() => {
  return filteredRuleDiffs.value.find(item => item.ruleCode === selectedRuleDiffCode.value)
})

watch(diffOpen, (open) => {
  if (!open) {
    diffData.value = {}
    diffVersionOptions.value = []
    diffFeeCode.value = undefined
    diffForm.fromVersionId = undefined
    diffForm.toVersionId = undefined
    selectedFeeDiffCode.value = undefined
    selectedRuleDiffCode.value = undefined
  }
})

watch(filteredRuleDiffs, (rows) => {
  if (!rows.length) {
    selectedRuleDiffCode.value = undefined
    return
  }
  if (!rows.some(item => item.ruleCode === selectedRuleDiffCode.value)) {
    selectedRuleDiffCode.value = rows[0].ruleCode
  }
})

async function loadBaseOptions() {
  const [dictMap, sceneResponse] = await Promise.all([
    getRemoteDictOptionMap(['cost_business_domain', 'cost_publish_version_status']),
    optionselectScene({ status: '0', pageNum: 1, pageSize: 1000 })
  ])
  businessDomainOptions.value = dictMap.cost_business_domain || []
  versionStatusOptions.value = dictMap.cost_publish_version_status || []
  sceneOptions.value = sceneResponse?.data || []
  queryParams.sceneId = resolveWorkingCostSceneId(sceneOptions.value, queryParams.sceneId)
  publishForm.sceneId = resolveWorkingCostSceneId(sceneOptions.value, publishForm.sceneId, queryParams.sceneId)
  lastQuerySceneId.value = queryParams.sceneId
  lastPublishSceneId.value = publishForm.sceneId
}

async function getList() {
  loading.value = true
  try {
    await loadBaseOptions()
    const [listResponse, statsResponse] = await Promise.all([listPublish(queryParams), getPublishStats(queryParams)])
    versionList.value = listResponse.rows || []
    total.value = listResponse.total || 0
    Object.assign(stats, statsResponse.data || {})
  } finally {
    loading.value = false
  }
}

function handleQuery() {
  queryParams.pageNum = 1
  getList()
}

function resetQuery() {
  proxy.resetForm('queryRef')
  queryParams.pageNum = 1
  queryParams.pageSize = 10
  getList()
}

async function handleQuerySceneChange(sceneId) {
  const confirmed = await confirmCostSceneSwitch({
    currentSceneId: lastQuerySceneId.value,
    nextSceneId: sceneId,
    sceneOptions: sceneOptions.value,
    scope: '发布台账筛选'
  })
  if (!confirmed) {
    queryParams.sceneId = lastQuerySceneId.value
    return
  }
  queryParams.sceneId = sceneId
  publishForm.sceneId = sceneId
  lastQuerySceneId.value = sceneId
  lastPublishSceneId.value = sceneId
}

function handleOpenAudit() {
  router.push({ path: COST_MENU_ROUTES.publishAudit, query: queryParams.sceneId ? { sceneId: queryParams.sceneId } : {} })
}

async function handlePublishSceneChange(sceneId) {
  const confirmed = await confirmCostSceneSwitch({
    currentSceneId: lastPublishSceneId.value,
    nextSceneId: sceneId,
    sceneOptions: sceneOptions.value,
    scope: '发布执行场景'
  })
  if (!confirmed) {
    publishForm.sceneId = lastPublishSceneId.value
    return
  }
  publishForm.sceneId = sceneId
  queryParams.sceneId = sceneId
  lastPublishSceneId.value = sceneId
  lastQuerySceneId.value = sceneId
}

async function handlePrecheck() {
  if (!publishForm.sceneId) {
    proxy.$modal.msgWarning('请先选择要发布的场景')
    return
  }
  const response = await getPublishPrecheck(publishForm.sceneId)
  precheck.value = response.data || { items: [], impactedFees: [] }
  if (precheck.value.suggestActivateNow) {
    publishForm.activateNow = true
  }
}

async function handlePublish() {
  if (!publishForm.sceneId || !publishForm.publishDesc) {
    proxy.$modal.msgWarning('请先选择发布场景并填写发布说明')
    return
  }
  await handlePrecheck()
  if (!precheck.value.publishable) {
    proxy.$modal.msgWarning('当前仍存在阻断项，请先处理后再发布')
    return
  }
  await confirmPublishDraftDiff()
  await addPublishVersion({ ...publishForm })
  proxy.$modal.msgSuccess('发布版本生成成功')
  publishTab.value = 'ledger'
  getList()
  handlePrecheck()
  const goNext = await confirmCostNextAction({
    message: '发布版本已生成。建议马上进入试算中心，用当前场景和版本做一轮回归验证。',
    confirmButtonText: '去试算中心'
  })
  if (goNext) {
    router.push({ path: COST_MENU_ROUTES.simulation, query: publishForm.sceneId ? { sceneId: publishForm.sceneId } : {} })
  }
}

async function confirmPublishDraftDiff() {
  const summary = publishDraftDiff.value.summary || {}
  const baseline = publishDraftDiff.value.previousVersionNo ? `上一发布版本 ${publishDraftDiff.value.previousVersionNo}` : '空白首版'
  const message = `本次将基于${baseline}生成新版本。费用变化 ${summary.feeChangeCount || 0} 项，变量变化 ${summary.variableChangeCount || 0} 项，规则变化 ${summary.ruleChangeCount || 0} 项。确认继续发布吗？`
  await ElMessageBox.confirm(message, '发布差异确认', { type: hasPublishDraftDiff.value ? 'warning' : 'info' })
}

async function handleDetail(row) {
  detailFeeCode.value = undefined
  const response = await getPublishVersion(row.versionId)
  detailData.value = response.data || {}
  detailOpen.value = true
}

async function reloadDetail() {
  if (!detailData.value.version?.versionId) return
  const response = await getPublishVersion(detailData.value.version.versionId, { feeCode: detailFeeCode.value })
  detailData.value = response.data || {}
}

async function handleDiff(row) {
  diffData.value = {
    toVersion: row,
    fromVersion: undefined,
    summary: {},
    fromScene: undefined,
    toScene: undefined,
    sceneDiffs: [],
    feeDiffs: [],
    ruleDiffs: []
  }
  diffFeeCode.value = undefined
  selectedFeeDiffCode.value = undefined
  selectedRuleDiffCode.value = undefined
  diffForm.toVersionId = row.versionId
  const response = await listPublish({ sceneId: row.sceneId, pageNum: 1, pageSize: 1000 })
  diffVersionOptions.value = (response.rows || []).filter(item => item.versionId !== row.versionId)
  diffForm.fromVersionId = undefined
  diffOpen.value = true
}

async function loadDiff() {
  if (!diffForm.toVersionId) return
  if (!diffForm.fromVersionId) {
    diffData.value = {
      toVersion: diffData.value.toVersion,
      fromVersion: undefined,
      summary: {},
      fromScene: undefined,
      toScene: undefined,
      sceneDiffs: [],
      feeDiffs: [],
      ruleDiffs: []
    }
    selectedFeeDiffCode.value = undefined
    selectedRuleDiffCode.value = undefined
    return
  }
  const response = await getPublishDiff({ ...diffForm, feeCode: diffFeeCode.value })
  diffData.value = response.data || {}
  selectedFeeDiffCode.value = diffData.value.feeDiffs?.[0]?.feeCode
  selectedRuleDiffCode.value = diffData.value.ruleDiffs?.[0]?.ruleCode
}

function handleFeeDiffRowChange(row) {
  selectedFeeDiffCode.value = row?.feeCode
}

function handleRuleDiffRowChange(row) {
  selectedRuleDiffCode.value = row?.ruleCode
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

function handlePublishRowCommand(command, row) {
  const handlers = {
    activate: handleActivate,
    rollback: handleRollback
  }
  handlers[command]?.(row)
}

async function handleActivate(row) {
  await ElMessageBox.confirm(`确认将版本 ${row.versionNo} 设为当前生效版本吗？`, '生效切换', { type: 'warning' })
  await activatePublishVersion(row.versionId)
  proxy.$modal.msgSuccess('生效切换成功')
  getList()
}

async function handleRollback(row) {
  await ElMessageBox.confirm(`确认将场景回滚到版本 ${row.versionNo} 吗？`, '版本回滚', { type: 'warning' })
  await rollbackPublishVersion(row.versionId)
  proxy.$modal.msgSuccess('回滚成功')
  getList()
}

function resolveVersionLabel(value) {
  return versionStatusOptions.value.find(item => item.value === value)?.label || value
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

function formatChangedFields(fields) {
  if (!Array.isArray(fields) || fields.length === 0) {
    return '新增或删除整体对象'
  }
  return fields.slice(0, 6).join('、') + (fields.length > 6 ? ` 等 ${fields.length} 项` : '')
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
  return buildFeeDiffNarrative(item)
}

function buildFeeDiffNarrative(item) {
  return `费用 ${item.feeName || item.feeCode} 在两个发布版本之间发生“${resolveCostChangeTypeLabel(item.changeType)}”，规则变化 ${item.ruleChangeCount || 0} 处，变量变化 ${item.variableChangeCount || 0} 处。`
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

function buildRuleDiffNarrative(item) {
  const changedFields = (item.changedFields || []).join('、') || '无字段摘要'
  return `规则 ${item.ruleName || item.ruleCode} 在两个发布版本之间发生“${resolveCostChangeTypeLabel(item.changeType)}”，条件变化 ${item.conditionChangeCount || 0} 处，阶梯变化 ${item.tierChangeCount || 0} 处，主要涉及 ${changedFields}。`
}

function buildRuleExplainLines(item) {
  const lines = []
  const fromRule = item.fromRule || {}
  const toRule = item.toRule || {}
  const fromConditions = item.fromConditions || []
  const toConditions = item.toConditions || []
  const fromTiers = item.fromTiers || []
  const toTiers = item.toTiers || []

  const businessNarrative = buildRuleBusinessNarrative(fromRule, toRule, fromConditions, toConditions, fromTiers, toTiers)
  if (businessNarrative) {
    lines.push(businessNarrative)
  }

  const ruleTypeChanged = stringifyValue(fromRule.ruleType) !== stringifyValue(toRule.ruleType)
  if (ruleTypeChanged) {
    lines.push(`规则类型由“${stringifyValue(fromRule.ruleType)}”调整为“${stringifyValue(toRule.ruleType)}”。`)
  }

  const priorityChanged = stringifyValue(fromRule.priority) !== stringifyValue(toRule.priority)
  if (priorityChanged) {
    lines.push(`优先级由“${stringifyValue(fromRule.priority)}”调整为“${stringifyValue(toRule.priority)}”。`)
  }

  const pricingLines = buildPricingExplainLines(fromRule.pricingJson || {}, toRule.pricingJson || {})
  lines.push(...pricingLines)

  const conditionLines = buildConditionExplainLines(fromConditions, toConditions)
  lines.push(...conditionLines)

  const tierLines = buildTierExplainLines(fromTiers, toTiers)
  lines.push(...tierLines)

  if (!lines.length) {
    lines.push('该规则存在结构变化，但当前无法归纳出更细的中文解释，请以下方左右对照为准。')
  }
  return lines
}

function buildRuleBusinessNarrative(fromRule, toRule, fromConditions, toConditions, fromTiers, toTiers) {
  const parts = []
  const fromScope = buildConditionScopeText(fromConditions)
  const toScope = buildConditionScopeText(toConditions)
  if (fromScope !== toScope) {
    if (fromScope && toScope) {
      parts.push(`命中条件由“${fromScope}”调整为“${toScope}”`)
    } else if (fromScope && !toScope) {
      parts.push(`命中条件由“${fromScope}”调整为“无条件命中”`)
    } else if (!fromScope && toScope) {
      parts.push(`命中条件由“无条件命中”调整为“${toScope}”`)
    }
  }

  const pricingChangeText = buildPricingBusinessText(fromRule.pricingJson || {}, toRule.pricingJson || {})
  if (pricingChangeText) {
    parts.push(pricingChangeText)
  }

  const tierChangeText = buildTierBusinessText(fromTiers, toTiers)
  if (tierChangeText) {
    parts.push(tierChangeText)
  }

  if (!parts.length) {
    return ''
  }
  return `业务解释：${parts.join('；')}。`
}

function buildPricingExplainLines(fromPricing, toPricing) {
  const lines = []
  const fields = [
    ['mode', '计价模式'],
    ['unit', '计价单位'],
    ['basis', '计价依据'],
    ['summary', '计价说明'],
    ['rateValue', '费率值'],
    ['unitPrice', '单价']
  ]
  fields.forEach(([field, label]) => {
    if (stringifyValue(fromPricing[field]) !== stringifyValue(toPricing[field])) {
      lines.push(`${label}由“${stringifyValue(fromPricing[field])}”调整为“${stringifyValue(toPricing[field])}”。`)
    }
  })
  return lines
}

function buildPricingBusinessText(fromPricing, toPricing) {
  const unit = stringifyMeasureUnit(toPricing.unit || fromPricing.unit)
  if (stringifyValue(fromPricing.unitPrice) !== stringifyValue(toPricing.unitPrice)) {
    return `单价由“${formatMeasureValue(fromPricing.unitPrice, unit)}”调整为“${formatMeasureValue(toPricing.unitPrice, unit)}”`
  }
  if (stringifyValue(fromPricing.rateValue) !== stringifyValue(toPricing.rateValue)) {
    return `费率由“${formatMeasureValue(fromPricing.rateValue, unit)}”调整为“${formatMeasureValue(toPricing.rateValue, unit)}”`
  }
  if (stringifyValue(fromPricing.summary) !== stringifyValue(toPricing.summary)) {
    return `计价说明由“${stringifyValue(fromPricing.summary)}”调整为“${stringifyValue(toPricing.summary)}”`
  }
  return ''
}

function buildConditionExplainLines(fromConditions, toConditions) {
  const lines = []
  const fromMap = new Map(fromConditions.map(item => [buildConditionIdentity(item), item]))
  const toMap = new Map(toConditions.map(item => [buildConditionIdentity(item), item]))
  const identities = Array.from(new Set([...fromMap.keys(), ...toMap.keys()]))
  identities.forEach((identity) => {
    const fromItem = fromMap.get(identity)
    const toItem = toMap.get(identity)
    if (fromItem && !toItem) {
      lines.push(`删除条件：${formatConditionText(fromItem)}。`)
      return
    }
    if (!fromItem && toItem) {
      lines.push(`新增条件：${formatConditionText(toItem)}。`)
      return
    }
    if (fromItem && toItem) {
      const fromText = formatConditionText(fromItem)
      const toText = formatConditionText(toItem)
      if (fromText !== toText) {
        lines.push(`条件由“${fromText}”调整为“${toText}”。`)
      }
    }
  })
  return lines
}

function buildConditionScopeText(conditions) {
  if (!conditions?.length) {
    return ''
  }
  const sorted = [...conditions].sort((a, b) => {
    const groupDiff = Number(a.groupNo || 0) - Number(b.groupNo || 0)
    if (groupDiff !== 0) {
      return groupDiff
    }
    return Number(a.sortNo || 0) - Number(b.sortNo || 0)
  })
  const groups = new Map()
  sorted.forEach((item) => {
    const groupKey = String(item.groupNo || 1)
    if (!groups.has(groupKey)) {
      groups.set(groupKey, [])
    }
    groups.get(groupKey).push(formatConditionText(item))
  })
  return Array.from(groups.values())
    .map(groupItems => groupItems.join(' 且 '))
    .join(' 或 ')
}

function buildTierExplainLines(fromTiers, toTiers) {
  const lines = []
  const fromMap = new Map(fromTiers.map(item => [String(item.tierNo), item]))
  const toMap = new Map(toTiers.map(item => [String(item.tierNo), item]))
  const tierNos = Array.from(new Set([...fromMap.keys(), ...toMap.keys()])).sort()
  tierNos.forEach((tierNo) => {
    const fromItem = fromMap.get(tierNo)
    const toItem = toMap.get(tierNo)
    if (fromItem && !toItem) {
      lines.push(`删除阶梯 ${tierNo}：${formatTierText(fromItem)}。`)
      return
    }
    if (!fromItem && toItem) {
      lines.push(`新增阶梯 ${tierNo}：${formatTierText(toItem)}。`)
      return
    }
    if (fromItem && toItem) {
      const fromText = formatTierText(fromItem)
      const toText = formatTierText(toItem)
      if (fromText !== toText) {
        lines.push(`阶梯 ${tierNo} 由“${fromText}”调整为“${toText}”。`)
      }
    }
  })
  return lines
}

function buildTierBusinessText(fromTiers, toTiers) {
  const fromLength = fromTiers?.length || 0
  const toLength = toTiers?.length || 0
  if (!fromLength && !toLength) {
    return ''
  }
  if (!fromLength && toLength) {
    return `新增 ${toLength} 档阶梯规则`
  }
  if (fromLength && !toLength) {
    return `删除原有 ${fromLength} 档阶梯规则`
  }
  if (fromLength !== toLength) {
    return `阶梯档位由 ${fromLength} 档调整为 ${toLength} 档`
  }
  return ''
}

function buildConditionIdentity(item) {
  return [item.variableCode, item.displayName, item.groupNo, item.sortNo].map(stringifyValue).join('|')
}

function formatConditionText(item) {
  return `${item.displayName || item.variableCode || '未知变量'} ${resolveOperatorLabel(item.operatorCode)} ${stringifyValue(item.compareValue)}`
}

function formatTierText(item) {
  return `${stringifyValue(item.startValue)} ~ ${stringifyValue(item.endValue)}，费率 ${stringifyValue(item.rateValue)}`
}

function stringifyMeasureUnit(value) {
  if (value === null || value === undefined || value === '' || value === '空') {
    return ''
  }
  return String(value)
}

function formatMeasureValue(value, unit) {
  const text = stringifyValue(value)
  if (text === '空') {
    return text
  }
  return unit ? `${text} ${unit}` : text
}

function resolveOperatorLabel(operatorCode) {
  const operatorMap = {
    EQ: '=',
    NE: '!=',
    GT: '>',
    GE: '>=',
    LT: '<',
    LE: '<=',
    IN: '属于',
    NOT_IN: '不属于',
    LIKE: '包含',
    BETWEEN: '介于'
  }
  return operatorMap[operatorCode] || operatorCode || '='
}

function stringifyValue(value) {
  if (value === null || value === undefined || value === '') {
    return '空'
  }
  if (typeof value === 'object') {
    return JSON.stringify(value)
  }
  return String(value)
}

onActivated(() => {
  getList()
})

getList()
</script>

<style scoped lang="scss">
.publish-center {
  display: grid;
  gap: 16px;
}

.publish-center__hero,
.publish-center__metric-card,
.publish-center__console,
.publish-center__precheck,
.publish-center__ledger {
  border: 1px solid var(--el-border-color);
  border-radius: 16px;
  background: var(--el-bg-color-overlay);
}

.publish-center__hero {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 16px;
  padding: 22px 24px;
  background: color-mix(in srgb, var(--el-color-primary-light-9) 18%, var(--el-bg-color-overlay));
}

.publish-center__hero-side {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 8px;
  max-width: 360px;
}

.publish-center__eyebrow {
  font-size: 12px;
  color: var(--el-color-primary);
  font-weight: 700;
  letter-spacing: .08em;
  text-transform: uppercase;
}

.publish-center__title {
  margin: 8px 0 0;
  font-size: 28px;
}

.publish-center__subtitle {
  margin: 10px 0 0;
  color: var(--el-text-color-regular);
  line-height: 1.8;
}

.publish-center__metrics {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 14px;
}

.publish-center__metric-card {
  display: grid;
  gap: 6px;
  padding: 14px 16px;
}

.publish-center__metric-card strong {
  font-size: 26px;
  color: var(--el-color-primary);
}

.publish-center__metric-card span,
.publish-center__metric-card small {
  color: var(--el-text-color-secondary);
}

.publish-center__query {
  padding: 14px 16px 0;
  border: 1px solid var(--el-border-color-light);
  border-radius: 16px;
  background: var(--el-bg-color-overlay);
  margin-bottom: 16px;
}

.publish-center__console {
  padding: 6px 16px 16px;
}

.publish-center__tabs :deep(.el-tabs__header) {
  margin: 0 0 16px;
}

.publish-center__tabs :deep(.el-tabs__nav-wrap::after) {
  height: 1px;
  background-color: var(--el-border-color-lighter);
}

.publish-center__tab-label {
  display: inline-flex;
  align-items: center;
  min-height: 40px;
  font-weight: 700;
}

.publish-center__precheck,
.publish-center__ledger {
  padding: 18px;
  border-color: var(--el-border-color-light);
}

.publish-center__section-head {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: center;
  margin-bottom: 16px;
}

.publish-center__section-head h3 {
  margin: 0;
  font-size: 18px;
}

.publish-center__section-head p {
  margin: 6px 0 0;
  color: var(--el-text-color-secondary);
  font-size: 13px;
}

.publish-center__ledger-actions {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 10px;
}

.publish-center__stage-strip {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
  margin-bottom: 16px;
}

.publish-center__stage-card {
  display: grid;
  gap: 7px;
  padding: 14px 16px;
  border: 1px solid var(--el-border-color-light);
  border-radius: 14px;
  background:
    linear-gradient(135deg, color-mix(in srgb, var(--el-color-primary-light-9) 32%, transparent), transparent 58%),
    var(--el-bg-color-overlay);
}

.publish-center__stage-card span {
  color: var(--el-color-primary);
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.08em;
}

.publish-center__stage-card strong {
  color: var(--el-text-color-primary);
  font-size: 18px;
}

.publish-center__stage-card small {
  color: var(--el-text-color-secondary);
  line-height: 1.6;
}

.publish-center__boundary {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 16px;
  padding: 12px 14px;
  border: 1px solid var(--el-border-color-light);
  border-radius: 14px;
  background: color-mix(in srgb, var(--el-color-warning-light-9) 28%, var(--el-bg-color-overlay));
}

.publish-center__boundary div {
  display: grid;
  gap: 4px;
}

.publish-center__boundary strong {
  color: var(--el-text-color-primary);
}

.publish-center__boundary span {
  color: var(--el-text-color-secondary);
  font-size: 13px;
  line-height: 1.6;
}

.publish-center__action-row,
.publish-center__filter-row {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-bottom: 16px;
}

.publish-center__release-form {
  padding: 16px 18px 2px;
  border: 1px solid var(--el-border-color-light);
  border-radius: 14px;
  background: var(--el-fill-color-blank);
  margin-bottom: 16px;
}

.publish-center__precheck-summary {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
  margin-bottom: 16px;
}

.publish-center__summary-card {
  display: grid;
  gap: 6px;
  padding: 12px 14px;
  border-radius: 12px;
  border: 1px solid var(--el-border-color-light);
  background: var(--el-fill-color-blank);
}

.publish-center__summary-card strong {
  font-size: 24px;
  color: var(--el-color-primary);
}

.publish-center__validation-note {
  margin-top: 6px;
  font-size: 12px;
  color: var(--el-text-color-secondary);
  line-height: 1.4;
}

.publish-center__impact h4 {
  margin: 0 0 10px;
}

.publish-center__impact-list {
  display: grid;
  gap: 10px;
}

.publish-center__impact-item {
  display: grid;
  gap: 4px;
  padding: 12px;
  border-radius: 12px;
  border: 1px solid var(--el-border-color-light);
  background: color-mix(in srgb, var(--el-color-success-light-9) 20%, var(--el-bg-color-overlay));
}

.publish-center__impact-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 10px;
}

.publish-center__impact-metrics {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.publish-center__impact-metrics span {
  display: inline-flex;
  align-items: center;
  padding: 4px 10px;
  border-radius: 999px;
  background: color-mix(in srgb, var(--el-color-primary-light-9) 36%, var(--el-bg-color-page));
  color: var(--el-text-color-secondary);
  font-size: 12px;
}

.publish-center__impact-details {
  display: grid;
  gap: 4px;
}

.publish-center__impact-detail-label {
  font-size: 12px;
  font-weight: 600;
  color: var(--el-text-color-regular);
}

.publish-center__impact-item span,
.publish-center__impact-item small {
  color: var(--el-text-color-secondary);
}

.publish-center__collapse {
  margin-top: 16px;
}

.publish-center__collapse pre {
  margin: 0;
  white-space: pre-wrap;
  word-break: break-all;
}
.publish-center__compare-head {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 56px minmax(0, 1fr);
  gap: 12px;
  align-items: stretch;
  margin-bottom: 16px;
}

.publish-center__compare-card {
  display: grid;
  gap: 6px;
  padding: 14px 16px;
  border-radius: 14px;
  border: 1px solid var(--el-border-color-light);
  background: color-mix(in srgb, var(--el-color-success-light-9) 35%, var(--el-bg-color-overlay));
}

.publish-center__compare-card--target {
  background: color-mix(in srgb, var(--el-color-danger-light-9) 35%, var(--el-bg-color-overlay));
}

.publish-center__compare-card strong {
  font-size: 24px;
  color: var(--el-text-color-primary);
}

.publish-center__compare-card small,
.publish-center__compare-label {
  color: var(--el-text-color-secondary);
}

.publish-center__compare-arrow {
  display: grid;
  place-items: center;
  font-size: 18px;
  font-weight: 700;
  color: var(--el-color-primary);
}

.publish-center__diff-tip {
  margin-bottom: 12px;
}

.publish-center__draft-diff {
  display: grid;
  gap: 12px;
  margin-top: 14px;
  padding: 14px;
  border: 1px solid var(--el-border-color-light);
  border-radius: 8px;
  background: var(--el-fill-color-blank);
}

.publish-center__draft-diff-head {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: flex-start;
}

.publish-center__draft-diff-head h4 {
  margin: 0;
  font-size: 16px;
}

.publish-center__draft-diff-head p {
  margin: 6px 0 0;
  color: var(--el-text-color-secondary);
  font-size: 13px;
}

.publish-center__draft-diff-tabs {
  margin-top: 4px;
}

.publish-center__diff-detail {
  display: grid;
  gap: 16px;
  margin-top: 16px;
}

.publish-center__explain-panel {
  border: 1px solid var(--el-border-color-light);
  border-radius: 14px;
  background: color-mix(in srgb, var(--el-color-primary-light-9) 35%, var(--el-bg-color-overlay));
}

.publish-center__explain-title {
  padding: 12px 14px;
  font-weight: 700;
  border-bottom: 1px solid var(--el-border-color-lighter);
}

.publish-center__explain-list {
  display: grid;
  gap: 10px;
  padding: 14px;
}

.publish-center__explain-item {
  padding: 10px 12px;
  border-radius: 10px;
  background: var(--el-bg-color-overlay);
  border: 1px solid var(--el-border-color-lighter);
  line-height: 1.7;
}

@media (max-width: 1200px) {
  .publish-center__metrics,
  .publish-center__stage-strip,
  .publish-center__compare-head {
    grid-template-columns: 1fr;
  }

  .publish-center__compare-arrow {
    display: none;
  }
}
</style>

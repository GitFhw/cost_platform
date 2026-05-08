<template>
  <div class="app-container result-page" :class="{ 'is-compact-mode': isCompactMode }">
    <section v-show="!isCompactMode" class="result-page__hero">
      <div>
        <div class="result-page__eyebrow">结果追溯</div>
        <h2 class="result-page__title">结果台账与追溯解释</h2>
        <p class="result-page__subtitle">
          按场景、版本、账期、任务和费用维度查看正式核算结果，支持命中规则、变量取值和执行过程追溯。
        </p>
      </div>
      <el-tag type="success">支持结果查询、差异定位和过程解释联动查看</el-tag>
    </section>

    <section v-show="!isCompactMode" class="result-page__metrics">
      <div v-for="item in metricItems" :key="item.label" class="result-page__metric-card">
        <span>{{ item.label }}</span>
        <strong>{{ item.value }}</strong>
        <small>{{ item.desc }}</small>
      </div>
    </section>

    <section v-show="!isCompactMode && hasBusinessSummary" class="result-page__business-summary">
      <div class="result-page__distribution-card">
        <div class="result-page__section-head result-page__section-head--tight">
          <div>
            <h3>费用分布</h3>
            <p>按金额排序展示当前筛选范围内贡献最高的费用。</p>
          </div>
        </div>
        <div v-if="feeDistribution.length" class="result-page__distribution-list">
          <div v-for="item in feeDistribution" :key="item.feeCode || item.feeName" class="result-page__distribution-row">
            <div>
              <strong>{{ item.feeName || item.feeCode || '-' }}</strong>
              <small>{{ item.feeCode || '-' }} · {{ formatInteger(item.resultCount) }} 条</small>
            </div>
            <span>{{ formatAmount(item.amountTotal) }}</span>
          </div>
        </div>
        <el-empty v-else description="暂无费用分布" :image-size="72" />
      </div>

      <div class="result-page__distribution-card">
        <div class="result-page__section-head result-page__section-head--tight">
          <div>
            <h3>对象分布</h3>
            <p>按对象维度汇总结果条数、对象数和金额。</p>
          </div>
        </div>
        <div v-if="objectDistribution.length" class="result-page__distribution-list">
          <div v-for="item in objectDistribution" :key="item.objectDimension" class="result-page__distribution-row">
            <div>
              <strong>{{ item.objectDimension || '-' }}</strong>
              <small>{{ formatInteger(item.objectCount) }} 个对象 · {{ formatInteger(item.resultCount) }} 条</small>
            </div>
            <span>{{ formatAmount(item.amountTotal) }}</span>
          </div>
        </div>
        <el-empty v-else description="暂无对象分布" :image-size="72" />
      </div>
    </section>

    <section class="result-page__query-shell">
      <div class="result-page__section-head">
        <div>
          <h3>查询与导出</h3>
          <p>正式结果建议按账期、任务或业务单号收敛查询范围，避免一次拉取过大的台账数据。</p>
        </div>
        <div class="result-page__section-actions">
          <el-button type="primary" plain icon="DataLine" @click="openCompare">差异对比</el-button>
          <el-button type="warning" plain icon="Download" @click="handleExport" v-hasPermi="['cost:result:export']">导出结果</el-button>
          <right-toolbar v-model:showSearch="showSearch" @queryTable="getList" />
        </div>
      </div>

      <el-form ref="queryRef" :model="queryParams" :inline="true" label-width="84px" class="result-page__query-form" v-show="showSearch">
        <el-form-item label="所属场景" prop="sceneId">
          <el-select v-model="queryParams.sceneId" clearable filterable style="width: 220px" @change="handleSceneChange">
            <el-option
              v-for="item in sceneOptions"
              :key="item.sceneId"
              :label="`${item.sceneName} / ${item.sceneCode}`"
              :value="item.sceneId"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="版本号" prop="versionId">
          <el-select v-model="queryParams.versionId" clearable filterable style="width: 220px">
            <el-option v-for="item in versionOptions" :key="item.versionId" :label="item.versionNo" :value="item.versionId" />
          </el-select>
        </el-form-item>
        <el-form-item label="账期" prop="billMonth">
          <el-date-picker
            v-model="queryParams.billMonth"
            clearable
            type="month"
            format="YYYY-MM"
            value-format="YYYY-MM"
            placeholder="选择账期"
            style="width: 160px"
          />
        </el-form-item>
        <el-form-item label="任务ID" prop="taskId">
          <el-input v-model="queryParams.taskId" clearable style="width: 140px" />
        </el-form-item>
        <el-form-item label="任务号" prop="taskNo">
          <el-input v-model="queryParams.taskNo" clearable style="width: 200px" />
        </el-form-item>
        <el-form-item label="费用编码" prop="feeCode">
          <el-input v-model="queryParams.feeCode" clearable style="width: 180px" />
        </el-form-item>
        <el-form-item label="业务单号" prop="bizNo">
          <el-input v-model="queryParams.bizNo" clearable style="width: 180px" />
        </el-form-item>
        <el-form-item label="结果状态" prop="resultStatus">
          <el-select v-model="queryParams.resultStatus" clearable style="width: 160px">
            <el-option v-for="item in resultStatusOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
          <el-button icon="Refresh" @click="resetQuery">重置</el-button>
        </el-form-item>
      </el-form>

      <el-alert
        v-if="queryGuardTip"
        class="result-page__guard"
        type="warning"
        :closable="false"
        :title="queryGuardTip"
      />
    </section>

    <div class="result-page__table">
      <div class="result-page__section-head">
        <div>
          <h3>结果台账</h3>
          <p>列表用于快速定位任务、费用和业务对象，详情用于查看结果和追溯解释。</p>
        </div>
      </div>

      <el-alert
        v-if="routeContext.taskId"
        class="result-page__context"
        type="info"
        :closable="false"
        :title="`当前正在查看任务 ${routeContext.taskId} 的结果台账，已自动带入路由中的过滤条件。`"
      />

      <el-table v-loading="loading" :data="resultList" border :height="resultTableHeight">
        <el-table-column label="任务号" prop="taskNo" width="220" />
        <el-table-column label="场景" min-width="180">
          <template #default="scope">{{ scope.row.sceneName }} ({{ scope.row.sceneCode }})</template>
        </el-table-column>
        <el-table-column label="版本" prop="versionNo" width="150" />
        <el-table-column label="账期" prop="billMonth" width="110" align="center" />
        <el-table-column label="费用" min-width="180">
          <template #default="scope">{{ scope.row.feeName }} ({{ scope.row.feeCode }})</template>
        </el-table-column>
        <el-table-column label="业务单号" prop="bizNo" min-width="160" />
        <el-table-column label="核算对象" min-width="180">
          <template #default="scope">{{ scope.row.objectName || '-' }} / {{ scope.row.objectCode || '-' }}</template>
        </el-table-column>
        <el-table-column label="数量" min-width="140" align="right">
          <template #default="scope">{{ formatQuantity(scope.row) }}</template>
        </el-table-column>
        <el-table-column label="单价" min-width="140" align="right">
          <template #default="scope">{{ formatUnitPrice(scope.row) }}</template>
        </el-table-column>
        <el-table-column label="计价口径" min-width="220" :show-overflow-tooltip="true">
          <template #default="scope">{{ resolveUnitSemantic(scope.row).summary }}</template>
        </el-table-column>
        <el-table-column label="金额" prop="amountValue" width="120" align="right" />
        <el-table-column label="状态" width="110" align="center">
          <template #default="scope">
            <dict-tag :options="resultStatusOptions" :value="scope.row.resultStatus" />
          </template>
        </el-table-column>
        <el-table-column label="操作" width="170" fixed="right" align="center">
          <template #default="scope">
            <el-button link type="primary" icon="View" @click="handleDetail(scope.row)">详情</el-button>
            <el-button
              link
              type="success"
              icon="Connection"
              @click="handleTrace(scope.row)"
              :disabled="!scope.row.traceId"
              v-hasPermi="['cost:result:trace']"
            >
              追溯
            </el-button>
          </template>
        </el-table-column>
        <template #empty>
          <cost-table-empty
            title="当前没有结果记录"
            description="结果台账只展示正式核算落库结果。可以先去正式核算提交任务，或清空筛选条件查看全部结果。"
          >
            <el-button type="primary" plain icon="Promotion" @click="$router.push('/cost/task')">去正式核算</el-button>
            <el-button icon="Refresh" @click="resetQuery">清空筛选</el-button>
          </cost-table-empty>
        </template>
      </el-table>

      <pagination
        v-show="total > 0"
        :total="total"
        v-model:page="queryParams.pageNum"
        v-model:limit="queryParams.pageSize"
        @pagination="getList"
      />
    </div>

    <el-drawer v-model="compareOpen" title="结果差异对比" size="1080px" append-to-body>
      <div class="result-page__detail-workbench" v-loading="compareLoading">
        <el-form :model="compareForm" label-width="96px" class="result-page__compare-form">
          <section class="result-page__compare-panel">
            <div class="result-page__compare-title">
              <h4>左侧基准</h4>
              <el-tag>{{ resolveCompareSourceLabel(compareForm.leftSourceType) }}</el-tag>
            </div>
            <el-form-item label="结果类型">
              <el-select v-model="compareForm.leftSourceType" style="width: 100%">
                <el-option label="正式结果" value="FORMAL" />
                <el-option label="试算结果" value="SIMULATION" />
              </el-select>
            </el-form-item>
            <template v-if="compareForm.leftSourceType === 'FORMAL'">
              <el-form-item label="任务ID">
                <el-input v-model="compareForm.leftTaskId" clearable placeholder="优先按任务ID对比" />
              </el-form-item>
              <el-form-item label="所属场景">
                <el-select v-model="compareForm.leftSceneId" clearable filterable style="width: 100%" @change="handleCompareSceneChange('left')">
                  <el-option v-for="item in sceneOptions" :key="item.sceneId" :label="`${item.sceneName} / ${item.sceneCode}`" :value="item.sceneId" />
                </el-select>
              </el-form-item>
              <el-form-item label="版本号">
                <el-select v-model="compareForm.leftVersionId" clearable filterable style="width: 100%">
                  <el-option v-for="item in leftCompareVersionOptions" :key="item.versionId" :label="item.versionNo" :value="item.versionId" />
                </el-select>
              </el-form-item>
              <el-form-item label="账期">
                <el-date-picker v-model="compareForm.leftBillMonth" clearable type="month" format="YYYY-MM" value-format="YYYY-MM" style="width: 100%" />
              </el-form-item>
            </template>
            <el-form-item v-else label="试算ID">
              <el-input v-model="compareForm.leftSimulationId" clearable placeholder="输入试算记录ID" />
            </el-form-item>
          </section>

          <section class="result-page__compare-panel">
            <div class="result-page__compare-title">
              <h4>右侧目标</h4>
              <el-tag>{{ resolveCompareSourceLabel(compareForm.rightSourceType) }}</el-tag>
            </div>
            <el-form-item label="结果类型">
              <el-select v-model="compareForm.rightSourceType" style="width: 100%">
                <el-option label="正式结果" value="FORMAL" />
                <el-option label="试算结果" value="SIMULATION" />
              </el-select>
            </el-form-item>
            <template v-if="compareForm.rightSourceType === 'FORMAL'">
              <el-form-item label="任务ID">
                <el-input v-model="compareForm.rightTaskId" clearable placeholder="优先按任务ID对比" />
              </el-form-item>
              <el-form-item label="所属场景">
                <el-select v-model="compareForm.rightSceneId" clearable filterable style="width: 100%" @change="handleCompareSceneChange('right')">
                  <el-option v-for="item in sceneOptions" :key="item.sceneId" :label="`${item.sceneName} / ${item.sceneCode}`" :value="item.sceneId" />
                </el-select>
              </el-form-item>
              <el-form-item label="版本号">
                <el-select v-model="compareForm.rightVersionId" clearable filterable style="width: 100%">
                  <el-option v-for="item in rightCompareVersionOptions" :key="item.versionId" :label="item.versionNo" :value="item.versionId" />
                </el-select>
              </el-form-item>
              <el-form-item label="账期">
                <el-date-picker v-model="compareForm.rightBillMonth" clearable type="month" format="YYYY-MM" value-format="YYYY-MM" style="width: 100%" />
              </el-form-item>
            </template>
            <el-form-item v-else label="试算ID">
              <el-input v-model="compareForm.rightSimulationId" clearable placeholder="输入试算记录ID" />
            </el-form-item>
          </section>
        </el-form>

        <div class="result-page__compare-actions">
          <el-button type="primary" icon="DataAnalysis" :loading="compareLoading" @click="handleCompare">开始对比</el-button>
          <el-button icon="Refresh" @click="resetCompareForm">重置</el-button>
        </div>

        <div v-if="compareData.summary" class="result-page__summary">
          <div class="result-page__summary-card"><span>左侧金额</span><strong>{{ formatAmount(compareSummary.leftAmountTotal) }}</strong><small>{{ compareSourceText(compareData.leftSource) }}</small></div>
          <div class="result-page__summary-card"><span>右侧金额</span><strong>{{ formatAmount(compareSummary.rightAmountTotal) }}</strong><small>{{ compareSourceText(compareData.rightSource) }}</small></div>
          <div class="result-page__summary-card"><span>差异金额</span><strong>{{ formatSignedAmount(compareSummary.diffAmount) }}</strong><small>右侧 - 左侧</small></div>
          <div class="result-page__summary-card"><span>变化费用</span><strong>{{ formatInteger(compareSummary.changedCount + compareSummary.addedCount + compareSummary.removedCount) }}</strong><small>新增 {{ compareSummary.addedCount || 0 }} / 移除 {{ compareSummary.removedCount || 0 }}</small></div>
        </div>

        <el-table v-if="compareData.summary" :data="compareRows" border max-height="460">
          <el-table-column label="费用编码" prop="feeCode" width="160" />
          <el-table-column label="费用名称" prop="feeName" min-width="180" :show-overflow-tooltip="true" />
          <el-table-column label="左侧金额" prop="leftAmount" width="130" align="right">
            <template #default="scope">{{ formatAmount(scope.row.leftAmount) }}</template>
          </el-table-column>
          <el-table-column label="右侧金额" prop="rightAmount" width="130" align="right">
            <template #default="scope">{{ formatAmount(scope.row.rightAmount) }}</template>
          </el-table-column>
          <el-table-column label="差异金额" prop="diffAmount" width="130" align="right">
            <template #default="scope">
              <span :class="{ 'is-negative': Number(scope.row.diffAmount) < 0, 'is-positive': Number(scope.row.diffAmount) > 0 }">{{ formatSignedAmount(scope.row.diffAmount) }}</span>
            </template>
          </el-table-column>
          <el-table-column label="结果数" width="120" align="center">
            <template #default="scope">{{ scope.row.leftCount || 0 }} / {{ scope.row.rightCount || 0 }}</template>
          </el-table-column>
          <el-table-column label="变化类型" prop="changeType" width="120" align="center">
            <template #default="scope">
              <el-tag :type="resolveChangeTypeTag(scope.row.changeType)">{{ resolveChangeTypeLabel(scope.row.changeType) }}</el-tag>
            </template>
          </el-table-column>
        </el-table>
      </div>
    </el-drawer>

    <el-drawer v-model="detailOpen" title="结果详情工作台" size="1080px" append-to-body>
      <div class="result-page__detail-workbench">
        <section v-if="detailData.ledger" class="result-page__detail-hero">
          <div>
            <div class="result-page__eyebrow">正式结果</div>
            <h3>{{ detailData.ledger.feeName }} / {{ detailData.ledger.amountValue || 0 }}</h3>
            <div class="result-page__detail-meta">
              <span>任务：{{ detailData.ledger.taskNo || '-' }}</span>
              <span>业务单号：{{ detailData.ledger.bizNo || '-' }}</span>
              <span>场景：{{ detailData.ledger.sceneName || '-' }}</span>
              <span>版本：{{ detailData.ledger.versionNo || '-' }}</span>
              <span>账期：{{ detailData.ledger.billMonth || '-' }}</span>
              <span>状态：{{ resolveResultStatus(detailData.ledger.resultStatus) }}</span>
            </div>
          </div>
          <div class="result-page__detail-actions">
            <el-button icon="Connection" :disabled="!detailData.ledger.traceId" @click="handleTrace(detailData.ledger)" v-hasPermi="['cost:result:trace']">打开追溯</el-button>
          </div>
        </section>

        <div class="result-page__summary result-page__summary--detail">
          <div class="result-page__summary-card"><span>金额</span><strong>{{ detailData.ledger?.amountValue || 0 }}</strong><small>{{ detailData.ledger?.currencyCode || 'CNY' }}</small></div>
          <div class="result-page__summary-card"><span>数量</span><strong>{{ detailData.ledger ? formatQuantity(detailData.ledger) : '-' }}</strong><small>{{ resolveUnitSemantic(detailData.ledger).quantityHint }}</small></div>
          <div class="result-page__summary-card"><span>单价</span><strong>{{ detailData.ledger ? formatUnitPrice(detailData.ledger) : '-' }}</strong><small>{{ resolveUnitSemantic(detailData.ledger).priceHint }}</small></div>
          <div class="result-page__summary-card"><span>追溯状态</span><strong>{{ detailData.ledger?.traceId ? '已生成' : '未生成' }}</strong><small>{{ detailData.ledger?.traceId ? `Trace #${detailData.ledger.traceId}` : '当前结果没有追溯记录' }}</small></div>
        </div>

        <el-descriptions v-if="detailData.ledger" :column="2" border>
          <el-descriptions-item label="费用编码">{{ detailData.ledger.feeCode }}</el-descriptions-item>
          <el-descriptions-item label="计价单位">{{ resolveUnitLabel(detailData.ledger.unitCode) }}</el-descriptions-item>
          <el-descriptions-item label="核算对象">{{ detailData.ledger.objectName || '-' }} / {{ detailData.ledger.objectCode || '-' }}</el-descriptions-item>
          <el-descriptions-item label="对象维度">{{ detailData.ledger.objectDimension || '-' }}</el-descriptions-item>
          <el-descriptions-item label="计价口径" :span="2">{{ resolveUnitSemantic(detailData.ledger).summary }}</el-descriptions-item>
          <el-descriptions-item label="结果解释" :span="2">{{ resolveUnitSemantic(detailData.ledger).resultHint }}</el-descriptions-item>
        </el-descriptions>

        <el-tabs class="result-page__tabs">
          <el-tab-pane label="结果记录">
            <JsonEditor :model-value="detailData.ledger" title="结果记录" readonly :rows="12" />
          </el-tab-pane>
          <el-tab-pane label="追溯解释">
            <JsonEditor :model-value="detailData.trace" title="追溯解释" readonly :rows="12" />
          </el-tab-pane>
        </el-tabs>
      </div>
    </el-drawer>

    <el-drawer v-model="traceOpen" title="追溯解释工作台" size="1080px" append-to-body>
      <div class="result-page__detail-workbench">
        <section v-if="traceData.traceId" class="result-page__detail-hero">
          <div>
            <div class="result-page__eyebrow">解释链路</div>
            <h3>Trace #{{ traceData.traceId }}</h3>
            <div class="result-page__detail-meta">
              <span>场景ID：{{ traceData.sceneId || '-' }}</span>
              <span>版本ID：{{ traceData.versionId || '-' }}</span>
              <span>规则ID：{{ traceData.ruleId || '-' }}</span>
              <span>阶梯ID：{{ traceData.tierId || '-' }}</span>
              <span>生成时间：{{ traceData.createTime || '-' }}</span>
            </div>
          </div>
        </section>

        <div class="result-page__summary result-page__summary--detail">
          <div v-for="item in traceMetricItems" :key="item.label" class="result-page__summary-card">
            <span>{{ item.label }}</span>
            <strong>{{ item.value }}</strong>
            <small>{{ item.desc }}</small>
          </div>
        </div>

        <section class="result-page__trace-grid">
          <div class="result-page__trace-panel">
            <div class="result-page__trace-head">
              <h4>命中规则</h4>
              <el-tag type="success" effect="plain">Rule #{{ traceData.ruleId || '-' }}</el-tag>
            </div>
            <div class="result-page__trace-kv">
              <span>阶梯ID</span><strong>{{ traceData.tierId || '-' }}</strong>
              <span>定价模式</span><strong>{{ tracePricingSummary.pricingMode || '-' }}</strong>
              <span>定价来源</span><strong>{{ tracePricingSummary.pricingSource || '-' }}</strong>
              <span>组合组</span><strong>{{ tracePricingSummary.matchedGroupNo || '-' }}</strong>
              <span>阶梯范围</span><strong>{{ tracePricingSummary.tierRange || '-' }}</strong>
              <span>公式编码</span><strong>{{ tracePricingSummary.formulaCode || '-' }}</strong>
            </div>
          </div>

          <div class="result-page__trace-panel">
            <div class="result-page__trace-head">
              <h4>公式/定价过程</h4>
              <el-tag type="warning" effect="plain">{{ tracePricingSummary.amountValue ? `金额 ${tracePricingSummary.amountValue}` : '定价解释' }}</el-tag>
            </div>
            <div class="result-page__trace-kv">
              <span>数量</span><strong>{{ tracePricingSummary.quantityValue || '-' }}</strong>
              <span>单价</span><strong>{{ tracePricingSummary.unitPrice || '-' }}</strong>
              <span>阶梯号</span><strong>{{ tracePricingSummary.tierNo || '-' }}</strong>
              <span>结果金额</span><strong>{{ tracePricingSummary.amountValue || '-' }}</strong>
            </div>
          </div>
        </section>

        <section class="result-page__trace-panel">
          <div class="result-page__trace-head">
            <h4>输入变量</h4>
            <el-tag effect="plain">{{ traceVariables.length }} 项</el-tag>
          </div>
          <el-table :data="traceVariables" size="small" border>
            <el-table-column label="变量" min-width="180">
              <template #default="scope">{{ scope.row.variableName || scope.row.variableCode || scope.row.key || '-' }}</template>
            </el-table-column>
            <el-table-column label="来源" width="120">
              <template #default="scope">{{ scope.row.sourceType || scope.row.source || '-' }}</template>
            </el-table-column>
            <el-table-column label="取值" min-width="220" show-overflow-tooltip>
              <template #default="scope">{{ resolveTraceValue(scope.row) }}</template>
            </el-table-column>
          </el-table>
        </section>

        <section class="result-page__trace-panel">
          <div class="result-page__trace-head">
            <h4>条件命中</h4>
            <el-tag effect="plain">{{ traceConditions.length }} 项</el-tag>
          </div>
          <el-table :data="traceConditions" size="small" border>
            <el-table-column label="条件" min-width="220">
              <template #default="scope">{{ scope.row.conditionName || scope.row.variableCode || scope.row.key || scope.row.conditionType || '-' }}</template>
            </el-table-column>
            <el-table-column label="比较" min-width="180" show-overflow-tooltip>
              <template #default="scope">{{ resolveConditionCompare(scope.row) }}</template>
            </el-table-column>
            <el-table-column label="结果" width="100" align="center">
              <template #default="scope">
                <el-tag :type="resolveConditionMatched(scope.row) ? 'success' : 'info'" effect="plain">
                  {{ resolveConditionMatched(scope.row) ? '命中' : '未命中' }}
                </el-tag>
              </template>
            </el-table-column>
          </el-table>
        </section>

        <section class="result-page__trace-panel">
          <div class="result-page__trace-head">
            <h4>执行时间线</h4>
            <el-tag effect="plain">{{ traceTimeline.length }} 步</el-tag>
          </div>
          <el-timeline>
            <el-timeline-item v-for="(item, index) in traceTimeline" :key="item.key || index" :timestamp="item.stepType || item.title || `步骤 ${index + 1}`">
              <div class="result-page__timeline-title">{{ item.stepName || item.objectName || item.objectCode || item.key || '-' }}</div>
              <p>{{ item.resultSummary || item.message || resolveTraceValue(item) }}</p>
            </el-timeline-item>
          </el-timeline>
        </section>

        <el-tabs class="result-page__tabs">
          <el-tab-pane label="变量值">
            <JsonEditor :model-value="traceData.variables" title="变量值" readonly :rows="12" />
          </el-tab-pane>
          <el-tab-pane label="条件命中">
            <JsonEditor :model-value="traceData.conditions" title="条件命中" readonly :rows="12" />
          </el-tab-pane>
          <el-tab-pane label="定价过程">
            <JsonEditor :model-value="traceData.pricing" title="定价过程" readonly :rows="12" />
          </el-tab-pane>
          <el-tab-pane label="执行时间线">
            <JsonEditor :model-value="traceData.timeline" title="执行时间线" readonly :rows="12" />
          </el-tab-pane>
        </el-tabs>
      </div>
    </el-drawer>
  </div>
</template>

<script setup name="CostResult">
import JsonEditor from '@/components/cost/JsonEditor.vue'
import { getResultCompare, getResultDetail, getResultStats, getTraceDetail, listResult, listVersionOptions } from '@/api/cost/run'
import { optionselectScene } from '@/api/cost/scene'
import useSettingsStore from '@/store/modules/settings'
import { resolveWorkingCostSceneId } from '@/utils/costSceneContext'
import { clearCostWorkContext, resolveWorkingBillMonth, resolveWorkingVersionId, syncCostWorkContext } from '@/utils/costWorkContext'
import { useCostWorkSceneAutoRefresh } from '@/utils/costWorkSceneAutoRefresh'
import { getCostUnitSemantic } from '@/utils/costUnitSemantics'
import { getRemoteDictOptionMap } from '@/utils/dictRemote'

const route = useRoute()
const { proxy } = getCurrentInstance()
const settingsStore = useSettingsStore()
const isCompactMode = computed(() => settingsStore.costPageMode === 'COMPACT')

const loading = ref(false)
const showSearch = ref(true)
const total = ref(0)
const resultList = ref([])
const sceneOptions = ref([])
const versionOptions = ref([])
const resultStatusOptions = ref([])
const unitCodeOptions = ref([])
const detailOpen = ref(false)
const traceOpen = ref(false)
const compareOpen = ref(false)
const compareLoading = ref(false)
const detailData = ref({})
const traceData = ref({})
const compareData = ref({})
const leftCompareVersionOptions = ref([])
const rightCompareVersionOptions = ref([])
const stats = reactive({
  resultCount: 0,
  taskCount: 0,
  traceCount: 0,
  abnormalCount: 0,
  amountTotal: 0,
  feeDistribution: [],
  objectDistribution: []
})
const resultQueryGuardMessage = '结果台账数据量较大，请至少补充账期、任务ID、任务号或业务单号后再查询'
const routeContext = reactive({
  taskId: route.query.taskId ? Number(route.query.taskId) : undefined
})
const lastOpenedRouteResultKey = ref('')

const queryParams = reactive({
  pageNum: 1,
  pageSize: 10,
  sceneId: route.query.sceneId ? Number(route.query.sceneId) : undefined,
  versionId: resolveWorkingVersionId(route.query.versionId ? Number(route.query.versionId) : undefined),
  billMonth: resolveWorkingBillMonth(route.query.billMonth),
  taskId: route.query.taskId ? Number(route.query.taskId) : undefined,
  taskNo: '',
  feeCode: '',
  bizNo: '',
  resultStatus: undefined
})

const defaultCompareForm = () => ({
  leftSourceType: 'FORMAL',
  leftTaskId: queryParams.taskId,
  leftSimulationId: undefined,
  leftSceneId: queryParams.sceneId,
  leftVersionId: queryParams.versionId,
  leftBillMonth: queryParams.billMonth,
  rightSourceType: 'FORMAL',
  rightTaskId: undefined,
  rightSimulationId: undefined,
  rightSceneId: queryParams.sceneId,
  rightVersionId: queryParams.versionId,
  rightBillMonth: queryParams.billMonth
})

const compareForm = reactive(defaultCompareForm())

const metricItems = computed(() => [
  { label: '结果条数', value: formatInteger(stats.resultCount), desc: '当前筛选范围内的结果台账条数' },
  { label: '任务数量', value: formatInteger(stats.taskCount), desc: '结果覆盖的正式核算任务数' },
  { label: '异常数量', value: formatInteger(stats.abnormalCount), desc: '非成功状态的结果记录数量' },
  { label: '金额合计', value: formatAmount(stats.amountTotal), desc: '当前筛选结果的金额汇总' }
])

const feeDistribution = computed(() => Array.isArray(stats.feeDistribution) ? stats.feeDistribution : [])
const objectDistribution = computed(() => Array.isArray(stats.objectDistribution) ? stats.objectDistribution : [])
const hasBusinessSummary = computed(() => Number(stats.resultCount || 0) > 0 || feeDistribution.value.length > 0 || objectDistribution.value.length > 0)

const resultTableHeight = computed(() => (isCompactMode.value ? 'calc(100dvh - 320px)' : 'calc(100dvh - 540px)'))

const traceMetricItems = computed(() => [
  { label: '变量值', value: countTraceEntries(traceData.value.variables), desc: '本次计算读取或派生出的变量数量' },
  { label: '条件命中', value: countTraceEntries(traceData.value.conditions), desc: '规则条件判断过程中的命中记录' },
  { label: '定价过程', value: countTraceEntries(traceData.value.pricing), desc: '单价、阶梯或固定金额的取价过程' },
  { label: '执行步骤', value: countTraceEntries(traceData.value.timeline), desc: '从变量求值到结果落账的执行时间线' }
])

const traceVariables = computed(() => normalizeTraceEntries(traceData.value.variables))
const traceConditions = computed(() => normalizeTraceEntries(traceData.value.conditions))
const traceTimeline = computed(() => normalizeTraceEntries(traceData.value.timeline))
const tracePricingSummary = computed(() => normalizePricingSummary(traceData.value.pricing))
const compareRows = computed(() => compareData.value.rows || [])
const compareSummary = computed(() => compareData.value.summary || {})

const queryGuardTip = computed(() => (shouldBlockBroadResultQuery() ? resultQueryGuardMessage : ''))

async function loadBaseOptions() {
  const [dictMap, sceneResp] = await Promise.all([
    getRemoteDictOptionMap(['cost_result_status', 'cost_unit_code']),
    optionselectScene({ status: '0', pageNum: 1, pageSize: 1000 })
  ])
  resultStatusOptions.value = dictMap.cost_result_status || []
  unitCodeOptions.value = dictMap.cost_unit_code || []
  sceneOptions.value = sceneResp?.data || []
  queryParams.sceneId = resolveWorkingCostSceneId(
    sceneOptions.value,
    queryParams.sceneId,
    route.query.sceneId ? Number(route.query.sceneId) : undefined
  )
}

async function loadVersionOptions(sceneId) {
  if (!sceneId) {
    versionOptions.value = []
    return
  }
  const resp = await listVersionOptions(sceneId)
  versionOptions.value = resp.data || []
}

function shouldBlockBroadResultQuery() {
  return !queryParams.billMonth
    && !queryParams.taskId
    && !queryParams.taskNo
    && !queryParams.bizNo
}

function clearResultView() {
  resultList.value = []
  total.value = 0
  Object.assign(stats, {
    resultCount: 0,
    taskCount: 0,
    traceCount: 0,
    abnormalCount: 0,
    amountTotal: 0,
    feeDistribution: [],
    objectDistribution: []
  })
}

async function getList(showWarning = false) {
  loading.value = true
  try {
    await loadBaseOptions()
    await loadVersionOptions(queryParams.sceneId)
    if (shouldBlockBroadResultQuery()) {
      clearResultView()
      if (showWarning) {
        proxy.$modal.msgWarning(resultQueryGuardMessage)
      }
      return
    }
    const [listResp, statsResp] = await Promise.all([
      listResult(queryParams),
      getResultStats(queryParams)
    ])
    resultList.value = listResp.rows || []
    total.value = listResp.total || 0
    Object.assign(stats, statsResp.data || {})
  } finally {
    loading.value = false
  }
}

function handleQuery() {
  queryParams.pageNum = 1
  getList(true)
}

function handleExport() {
  if (shouldBlockBroadResultQuery()) {
    proxy.$modal.msgWarning(resultQueryGuardMessage)
    return
  }
  proxy.download(
    'cost/run/result/export',
    {
      ...queryParams
    },
    `cost_result_${Date.now()}.xlsx`
  )
}

async function openCompare() {
  await loadBaseOptions()
  await resetCompareForm()
  compareOpen.value = true
}

async function resetCompareForm() {
  Object.assign(compareForm, defaultCompareForm())
  compareData.value = {}
  await Promise.all([
    loadCompareVersionOptions('left', compareForm.leftSceneId),
    loadCompareVersionOptions('right', compareForm.rightSceneId)
  ])
}

async function handleCompareSceneChange(side) {
  if (side === 'left') {
    compareForm.leftVersionId = undefined
    await loadCompareVersionOptions('left', compareForm.leftSceneId)
  } else {
    compareForm.rightVersionId = undefined
    await loadCompareVersionOptions('right', compareForm.rightSceneId)
  }
}

async function loadCompareVersionOptions(side, sceneId) {
  if (!sceneId) {
    if (side === 'left') {
      leftCompareVersionOptions.value = []
    } else {
      rightCompareVersionOptions.value = []
    }
    return
  }
  const resp = await listVersionOptions(sceneId)
  if (side === 'left') {
    leftCompareVersionOptions.value = resp.data || []
  } else {
    rightCompareVersionOptions.value = resp.data || []
  }
}

async function handleCompare() {
  compareLoading.value = true
  try {
    const resp = await getResultCompare(compareForm)
    compareData.value = resp.data || {}
  } finally {
    compareLoading.value = false
  }
}

function resetQuery() {
  proxy.resetForm('queryRef')
  queryParams.pageNum = 1
  queryParams.pageSize = 10
  queryParams.taskId = routeContext.taskId
  queryParams.taskNo = ''
  queryParams.versionId = undefined
  queryParams.feeCode = ''
  queryParams.bizNo = ''
  queryParams.resultStatus = undefined
  queryParams.sceneId = route.query.sceneId ? Number(route.query.sceneId) : queryParams.sceneId
  queryParams.billMonth = resolveWorkingBillMonth(route.query.billMonth)
  versionOptions.value = []
  getList(false)
}

async function handleSceneChange(sceneId) {
  queryParams.sceneId = sceneId
  queryParams.versionId = undefined
  clearCostWorkContext(['versionId'])
  syncCostWorkContext({ sceneId, billMonth: queryParams.billMonth })
  await loadVersionOptions(queryParams.sceneId)
}

async function handleDetail(row) {
  const resp = await getResultDetail(row.resultId)
  detailData.value = resp.data || {}
  detailOpen.value = true
}

async function handleTrace(row) {
  const resp = await getTraceDetail(row.traceId)
  traceData.value = resp.data || {}
  traceOpen.value = true
}

async function openFirstResultByRouteContext() {
  if (!routeContext.taskId || !resultList.value.length) return
  const currentKey = `${routeContext.taskId}:${resultList.value[0].resultId}`
  if (lastOpenedRouteResultKey.value === currentKey) return
  await handleDetail(resultList.value[0])
  lastOpenedRouteResultKey.value = currentKey
}

function resolveUnitLabel(unitCode) {
  const match = unitCodeOptions.value.find(item => item.value === unitCode)
  return match ? match.label : (unitCode || '-')
}

function resolveResultStatus(status) {
  const match = resultStatusOptions.value.find(item => item.value === status)
  return match ? match.label : (status || '-')
}

function resolveUnitSemantic(row) {
  return getCostUnitSemantic(row?.unitCode, resolveUnitLabel(row?.unitCode))
}

function countTraceEntries(value) {
  if (!value) return 0
  if (Array.isArray(value)) return value.length
  if (typeof value === 'object') return Object.keys(value).length
  return 1
}

function normalizeTraceEntries(value) {
  if (!value) {
    return []
  }
  if (Array.isArray(value)) {
    return value.map((item, index) => normalizeTraceEntry(item, `item${index + 1}`))
  }
  if (typeof value === 'object') {
    return Object.entries(value).map(([key, item]) => normalizeTraceEntry(item, key))
  }
  return [{ key: 'value', value }]
}

function normalizeTraceEntry(item, key) {
  if (item && typeof item === 'object' && !Array.isArray(item)) {
    return { key, ...item }
  }
  return { key, value: item }
}

function normalizePricingSummary(value) {
  if (!value) {
    return {}
  }
  if (Array.isArray(value)) {
    return value.find(item => item && typeof item === 'object') || {}
  }
  return typeof value === 'object' ? value : { value }
}

function resolveTraceValue(row = {}) {
  const value = row.value ?? row.actualValue ?? row.resolvedValue ?? row.variableValue ?? row.compareValue ?? row.result ?? row.amountValue
  if (value === undefined || value === null || value === '') {
    const keys = Object.keys(row).filter(key => !['key', 'variableCode', 'variableName', 'sourceType', 'source'].includes(key))
    if (!keys.length) {
      return '-'
    }
    return keys.map(key => `${key}=${formatTraceObject(row[key])}`).join('；')
  }
  return formatTraceObject(value)
}

function resolveConditionCompare(row = {}) {
  const operator = row.operator || row.compareOperator || row.conditionOperator || row.conditionType || ''
  const left = row.actualValue ?? row.leftValue ?? row.variableValue ?? row.value ?? '-'
  const right = row.expectedValue ?? row.rightValue ?? row.compareValue ?? row.thresholdValue ?? '-'
  return `${formatTraceObject(left)} ${operator} ${formatTraceObject(right)}`.trim()
}

function resolveConditionMatched(row = {}) {
  if (row.matched !== undefined) return Boolean(row.matched)
  if (row.hit !== undefined) return Boolean(row.hit)
  if (row.passed !== undefined) return Boolean(row.passed)
  if (row.result !== undefined) return Boolean(row.result)
  return true
}

function formatTraceObject(value) {
  if (value === undefined || value === null || value === '') {
    return '-'
  }
  if (typeof value === 'object') {
    return JSON.stringify(value)
  }
  return String(value)
}

function formatAmount(value) {
  if (value === undefined || value === null || value === '') {
    return '0.00'
  }
  return Number(value || 0).toFixed(2)
}

function formatSignedAmount(value) {
  const numberValue = Number(value || 0)
  return `${numberValue > 0 ? '+' : ''}${numberValue.toFixed(2)}`
}

function formatInteger(value) {
  return Number(value || 0).toLocaleString()
}

function resolveCompareSourceLabel(type) {
  return type === 'SIMULATION' ? '试算结果' : '正式结果'
}

function compareSourceText(source = {}) {
  const scope = source.simulationId ? `试算 #${source.simulationId}` : (source.taskId ? `任务 #${source.taskId}` : source.sourceNo)
  return [scope, source.versionNo, source.billMonth].filter(Boolean).join(' / ') || '-'
}

function resolveChangeTypeLabel(type) {
  const labelMap = {
    ADDED: '新增',
    REMOVED: '移除',
    CHANGED: '变化',
    UNCHANGED: '无变化'
  }
  return labelMap[type] || type || '-'
}

function resolveChangeTypeTag(type) {
  const tagMap = {
    ADDED: 'success',
    REMOVED: 'danger',
    CHANGED: 'warning',
    UNCHANGED: 'info'
  }
  return tagMap[type] || 'info'
}

function formatQuantity(row) {
  if (row?.quantityValue == null) {
    return '-'
  }
  return `${row.quantityValue} ${resolveUnitLabel(row.unitCode)}`
}

function formatUnitPrice(row) {
  if (row?.unitPrice == null) {
    return '-'
  }
  const unitLabel = resolveUnitLabel(row.unitCode)
  return unitLabel === '-' ? String(row.unitPrice) : `${row.unitPrice} / ${unitLabel}`
}

watch(
  () => [queryParams.sceneId, queryParams.versionId, queryParams.billMonth],
  ([sceneId, versionId, billMonth]) => {
    syncCostWorkContext({ sceneId, versionId, billMonth })
  },
  { immediate: true }
)

useCostWorkSceneAutoRefresh({
  queryParams,
  sceneOptions,
  beforeRefresh: async sceneId => {
    queryParams.versionId = undefined
    compareForm.leftSceneId = sceneId
    compareForm.rightSceneId = sceneId
    compareForm.leftVersionId = undefined
    compareForm.rightVersionId = undefined
    clearCostWorkContext(['versionId'])
    await loadVersionOptions(sceneId)
  },
  refresh: getList
})

watch(
  () => route.query,
  value => {
    routeContext.taskId = value.taskId ? Number(value.taskId) : undefined
    queryParams.taskId = routeContext.taskId
    queryParams.sceneId = value.sceneId ? Number(value.sceneId) : queryParams.sceneId
    queryParams.versionId = value.versionId ? Number(value.versionId) : queryParams.versionId
    queryParams.billMonth = value.billMonth || queryParams.billMonth
  },
  { deep: true }
)

onMounted(async () => {
  await getList(false)
  await openFirstResultByRouteContext()
})

onActivated(async () => {
  await getList(false)
  await openFirstResultByRouteContext()
})
</script>

<style scoped lang="scss">
@use '../../../assets/styles/cost-workbench.scss' as costWorkbench;

.result-page {
  @include costWorkbench.page-root;
}

.result-page__table,
.result-page__summary-card,
.result-page__detail-hero,
.result-page__tabs {
  @include costWorkbench.surface;
}

.result-page__hero {
  @include costWorkbench.hero;
}

.result-page__eyebrow {
  @include costWorkbench.eyebrow;
}

.result-page__title {
  @include costWorkbench.page-title;
}

.result-page__subtitle {
  @include costWorkbench.page-subtitle;
}

.result-page__metrics {
  @include costWorkbench.metric-grid;
}

.result-page__metric-card {
  @include costWorkbench.metric-card;
}

.result-page__metric-card strong {
  @include costWorkbench.metric-value;
}

.result-page__business-summary {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
}

.result-page__distribution-card {
  min-width: 0;
  padding: 16px;
  @include costWorkbench.surface;
}

.result-page__section-head--tight {
  margin-bottom: 12px;
}

.result-page__distribution-list {
  display: grid;
  gap: 8px;
}

.result-page__distribution-row {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 12px;
  align-items: center;
  padding: 10px 12px;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 10px;
  background: color-mix(in srgb, var(--el-bg-color-overlay) 94%, var(--el-color-primary-light-9) 6%);
}

.result-page__distribution-row strong,
.result-page__distribution-row small {
  display: block;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.result-page__distribution-row small {
  margin-top: 4px;
  color: var(--el-text-color-secondary);
}

.result-page__distribution-row > span {
  color: var(--el-color-primary);
  font-weight: 700;
  font-variant-numeric: tabular-nums;
}

.result-page__query-shell {
  @include costWorkbench.query-shell;
}

.result-page__query-form {
  margin-top: 14px;
}

.result-page__guard,
.result-page__context {
  margin-top: 12px;
}

.result-page__table {
  padding: 16px;
}

.result-page__section-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
}

.result-page__section-actions {
  display: flex;
  align-items: center;
  gap: 10px;
}

.result-page__section-head h3 {
  margin: 0;
  font-size: 18px;
}

.result-page__section-head p {
  margin: 6px 0 0;
  color: var(--el-text-color-secondary);
  font-size: 13px;
}

.result-page__detail-workbench {
  display: grid;
  gap: 16px;
}

.result-page__detail-hero {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  padding: 16px 18px;
  background: color-mix(in srgb, var(--el-bg-color-overlay) 92%, var(--el-color-success-light-9) 8%);
}

.result-page__detail-hero h3 {
  margin: 6px 0 0;
  font-size: 22px;
}

.result-page__detail-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 12px;
}

.result-page__detail-meta span {
  display: inline-flex;
  align-items: center;
  min-height: 28px;
  padding: 0 10px;
  border: 1px solid var(--el-border-color);
  border-radius: 999px;
  color: var(--el-text-color-secondary);
  background: var(--el-bg-color-overlay);
  font-size: 12px;
}

.result-page__detail-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  align-content: flex-start;
  justify-content: flex-end;
}

.result-page__summary {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
}

.result-page__summary-card {
  display: grid;
  gap: 6px;
  padding: 12px 14px;
}

.result-page__summary-card strong {
  color: var(--el-color-success-dark-2);
  font-size: 20px;
  line-height: 1.25;
}

.result-page__summary-card small {
  color: var(--el-text-color-secondary);
  line-height: 1.5;
}

.result-page__summary--detail {
  margin: 0;
}

.result-page__trace-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.result-page__trace-panel {
  min-width: 0;
  padding: 14px 16px;
  border: 1px solid var(--el-border-color);
  border-radius: 16px;
  background: var(--el-bg-color-overlay);
}

.result-page__trace-head {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: center;
  margin-bottom: 12px;
}

.result-page__trace-head h4 {
  margin: 0;
  font-size: 16px;
}

.result-page__trace-kv {
  display: grid;
  grid-template-columns: 92px minmax(0, 1fr);
  gap: 8px 12px;
  align-items: center;
}

.result-page__trace-kv span {
  color: var(--el-text-color-secondary);
  font-size: 13px;
}

.result-page__trace-kv strong {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  color: var(--el-text-color-primary);
}

.result-page__timeline-title {
  font-weight: 700;
  color: var(--el-text-color-primary);
}

.result-page__compare-form {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
}

.result-page__compare-panel {
  min-width: 0;
  padding: 14px 16px 4px;
  border: 1px solid var(--el-border-color);
  border-radius: 8px;
  background: var(--el-bg-color-overlay);
}

.result-page__compare-title {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 12px;
}

.result-page__compare-title h4 {
  margin: 0;
  font-size: 16px;
}

.result-page__compare-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}

.is-negative {
  color: var(--el-color-danger);
  font-weight: 700;
}

.is-positive {
  color: var(--el-color-success);
  font-weight: 700;
}

.result-page__trace-panel :deep(.el-timeline) {
  margin-top: 6px;
  padding-left: 4px;
}

.result-page__trace-panel :deep(.el-timeline-item__content p) {
  margin: 4px 0 0;
  color: var(--el-text-color-secondary);
  line-height: 1.6;
}

.result-page__tabs {
  padding: 14px 16px 16px;
}

.result-page__tabs pre {
  margin: 0;
  white-space: pre-wrap;
  word-break: break-all;
}

.result-page.is-compact-mode {
  gap: 12px;
  background: var(--el-bg-color-page);
}

.result-page.is-compact-mode .result-page__section-head > div {
  display: none;
}

.result-page.is-compact-mode .result-page__table .result-page__section-head {
  display: none;
}

.result-page.is-compact-mode .result-page__query-form {
  margin-top: 0;
}

@media (max-width: 1200px) {
  .result-page__metrics,
  .result-page__business-summary,
  .result-page__compare-form,
  .result-page__trace-grid,
  .result-page__summary {
    grid-template-columns: 1fr;
  }

  .result-page__hero,
  .result-page__detail-hero,
  .result-page__section-head {
    flex-direction: column;
    align-items: stretch;
  }

  .result-page__detail-actions,
  .result-page__section-actions {
    justify-content: flex-start;
  }
}
</style>

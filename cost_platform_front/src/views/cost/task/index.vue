<template>
  <div class="app-container run-page" :class="{ 'is-compact-mode': isCompactMode }">
    <section v-show="!isCompactMode" class="run-page__hero">
      <div>
        <div class="run-page__eyebrow">正式核算</div>
        <h2 class="run-page__title">正式核算与批量任务</h2>
        <p class="run-page__subtitle">
          统一发起正式核算任务，支持在线录入和导入批次两种方式，并提供任务明细、批次管理和分段运行视图。
        </p>
      </div>
      <div class="run-page__hero-side">
        <el-tag type="success">支持在线提交与批量导入，满足不同规模的正式核算需求</el-tag>
        <el-button icon="Tickets" @click="openBatchLedger">导入批次台账</el-button>
      </div>
    </section>

    <section v-show="!isCompactMode" class="run-page__entry-guide">
      <div class="run-page__entry-card">
        <div>
          <strong>试算验证</strong>
          <p>用于规则联调、样例回归和结果解释，不落正式台账。</p>
        </div>
        <el-button type="primary" plain icon="Promotion" @click="goSimulation">去试算中心</el-button>
      </div>
      <div class="run-page__entry-card is-active">
        <div>
          <strong>正式核算</strong>
          <p>用于生产账期任务提交，执行后进入结果台账和异常治理。</p>
        </div>
        <el-tag type="warning">当前入口</el-tag>
      </div>
    </section>

    <section v-show="!isCompactMode" class="run-page__metrics">
      <div v-for="item in metricItems" :key="item.label" class="run-page__metric-card">
        <span>{{ item.label }}</span>
        <strong>{{ item.value }}</strong>
        <small>{{ item.desc }}</small>
      </div>
    </section>

    <section v-show="!isCompactMode" class="run-page__overview-grid">
      <div class="run-page__panel">
        <div class="run-page__section-head">
          <div>
            <h3>近 7 天任务趋势</h3>
            <p>观察正式核算任务提交、执行中和问题任务的波动。</p>
          </div>
        </div>
        <el-table :data="overview.recentTaskTrend" size="small" border>
          <el-table-column label="日期" prop="date" width="120" />
          <el-table-column label="任务总量" prop="count" width="90" align="center" />
          <el-table-column label="执行中" prop="runningCount" width="90" align="center" />
          <el-table-column label="问题任务" prop="failedCount" width="100" align="center" />
        </el-table>
      </div>

      <div class="run-page__panel">
        <div class="run-page__section-head">
          <div>
            <h3>近 7 天分片趋势</h3>
            <p>观察分片失败集中度，提前识别批量任务风险。</p>
          </div>
        </div>
        <el-table :data="overview.recentPartitionTrend" size="small" border>
          <el-table-column label="日期" prop="date" width="120" />
          <el-table-column label="分片总量" prop="count" width="90" align="center" />
          <el-table-column label="失败分片" prop="failedCount" width="90" align="center" />
          <el-table-column label="平均耗时(ms)" prop="avgDurationMs" width="120" align="center" />
        </el-table>
      </div>

      <div class="run-page__panel">
        <div class="run-page__section-head">
          <div>
            <h3>高风险任务</h3>
            <p>优先处理失败量高、失败分片多的正式核算任务。</p>
          </div>
        </div>
        <el-table :data="overview.topRiskTasks" size="small" border>
          <el-table-column label="任务ID" prop="taskId" width="90" align="center" />
          <el-table-column label="场景" prop="sceneName" min-width="140" />
          <el-table-column label="账期" prop="billMonth" width="100" align="center" />
          <el-table-column label="失败明细" prop="failCount" width="90" align="center" />
          <el-table-column label="失败分片" prop="partitionFailCount" width="90" align="center" />
          <el-table-column label="操作" width="130" align="center">
            <template #default="scope">
              <el-button link type="primary" icon="Histogram" @click="openTaskCenterById(scope.row.taskId, scope.row.billMonth, 'partition')">查看分片</el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>

      <div class="run-page__panel">
        <div class="run-page__section-head">
          <div>
            <h3>状态与来源分布</h3>
            <p>快速判断当前任务积压在哪个状态，以及 JSON / 导入批次占比。</p>
          </div>
        </div>
        <div class="run-page__distribution-grid">
          <div>
            <h4>任务状态</h4>
            <div class="run-page__distribution-list">
              <div v-for="item in overview.taskStatusDistribution" :key="item.taskStatus" class="run-page__distribution-item">
                <dict-tag :options="taskStatusOptions" :value="item.taskStatus" />
                <strong>{{ item.count }}</strong>
              </div>
              <el-empty v-if="!overview.taskStatusDistribution.length" description="暂无数据" :image-size="60" />
            </div>
          </div>
          <div>
            <h4>输入来源</h4>
            <div class="run-page__distribution-list">
              <div v-for="item in overview.inputSourceDistribution" :key="item.inputSourceType" class="run-page__distribution-item">
                <span>{{ resolveInputSource(item.inputSourceType) }}</span>
                <strong>{{ item.count }}</strong>
              </div>
              <el-empty v-if="!overview.inputSourceDistribution.length" description="暂无数据" :image-size="60" />
            </div>
          </div>
        </div>
      </div>

      <div class="run-page__panel">
        <div class="run-page__section-head">
          <div>
            <h3>认领节点概览</h3>
            <p>聚焦分片 owner 的认领覆盖、僵尸风险和节点负载，便于发现跨节点调度异常。</p>
          </div>
        </div>
        <div class="run-page__summary run-page__summary--compact">
          <div v-for="item in ownerSummaryItems" :key="item.label" class="run-page__summary-card">
            <span>{{ item.label }}</span>
            <strong>{{ item.value }}</strong>
          </div>
        </div>
        <el-table :data="overview.partitionOwnerDistribution" size="small" border>
          <el-table-column label="认领节点" prop="executeNode" min-width="160" />
          <el-table-column label="认领分片" prop="partitionCount" width="110" align="center" />
          <el-table-column label="运行中" prop="runningCount" width="100" align="center" />
          <el-table-column label="异常分片" prop="problematicCount" width="100" align="center" />
          <el-table-column label="最近认领时间" prop="latestClaimTime" min-width="180" align="center" />
        </el-table>
        <el-empty v-if="!overview.partitionOwnerDistribution.length" description="暂无节点认领数据" :image-size="60" />
      </div>

      <div class="run-page__panel">
        <div class="run-page__section-head">
          <div>
            <h3>Owner 风险任务</h3>
            <p>按任务聚合 owner 异常，优先定位存在僵尸 owner 或无 owner 运行中的任务。</p>
          </div>
        </div>
        <el-table :data="overview.topOwnerRiskTasks" size="small" border>
          <el-table-column label="任务ID" prop="taskId" width="90" align="center" />
          <el-table-column label="场景" prop="sceneName" min-width="140" />
          <el-table-column label="账期" prop="billMonth" width="100" align="center" />
          <el-table-column label="僵尸风险分片" prop="staleRunningOwnerCount" width="120" align="center" />
          <el-table-column label="无 owner 运行中" prop="runningWithoutOwnerCount" width="130" align="center" />
          <el-table-column label="活跃节点数" prop="activeOwnerCount" width="110" align="center" />
          <el-table-column label="操作" width="130" align="center">
            <template #default="scope">
              <el-button link type="primary" icon="Histogram" @click="openTaskCenterById(scope.row.taskId, scope.row.billMonth, 'partition')">查看分片</el-button>
            </template>
          </el-table-column>
        </el-table>
        <el-empty v-if="!overview.topOwnerRiskTasks.length" description="暂无 owner 风险任务" :image-size="60" />
      </div>
    </section>

    <section class="run-page__query-shell">
      <div class="run-page__section-head">
        <div>
          <div class="run-page__eyebrow">任务上下文</div>
          <h3>正式核算任务筛选</h3>
          <p>这里控制下方任务台账、运行概览和治理入口的默认范围，建议先确定场景、版本与账期。</p>
        </div>
        <right-toolbar v-model:showSearch="showSearch" @queryTable="getList" />
      </div>

      <el-form ref="queryRef" :model="queryParams" :inline="true" label-width="84px" v-show="showSearch" class="run-page__query-form">
        <el-form-item label="所属场景" prop="sceneId">
          <el-select v-model="queryParams.sceneId" clearable filterable style="width: 220px" @change="handleQuerySceneChange">
            <el-option v-for="item in sceneOptions" :key="item.sceneId" :label="`${item.sceneName} / ${item.sceneCode}`" :value="item.sceneId" />
          </el-select>
        </el-form-item>
        <el-form-item label="版本号" prop="versionId">
          <el-select v-model="queryParams.versionId" clearable filterable style="width: 220px">
            <el-option v-for="item in versionOptions" :key="item.versionId" :label="item.versionNo" :value="item.versionId" />
          </el-select>
        </el-form-item>
        <el-form-item label="任务类型" prop="taskType">
          <el-select v-model="queryParams.taskType" clearable style="width: 180px">
            <el-option v-for="item in taskTypeOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="任务状态" prop="taskStatus">
          <el-select v-model="queryParams.taskStatus" clearable style="width: 180px">
            <el-option v-for="item in taskStatusOptions" :key="item.value" :label="item.label" :value="item.value" />
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
        <el-form-item>
          <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
          <el-button icon="Refresh" @click="resetQuery">重置</el-button>
        </el-form-item>
      </el-form>
    </section>

    <section class="run-page__workspace">
      <div class="run-page__panel">
        <div class="run-page__section-head">
          <div>
            <h3>提交任务</h3>
            <p>单笔和批量任务继续支持 JSON 直传，也可以先创建或选择导入批次，再引用批次号发起正式核算。</p>
          </div>
          <span class="run-page__panel-badge">生产提交</span>
        </div>

        <el-form :model="form" label-width="92px">
          <el-form-item label="运行场景" required>
            <el-select v-model="form.sceneId" filterable style="width: 100%" @change="handleFormSceneChange">
              <el-option v-for="item in sceneOptions" :key="item.sceneId" :label="`${item.sceneName} / ${item.sceneCode}`" :value="item.sceneId" />
            </el-select>
          </el-form-item>
          <el-form-item label="执行版本">
            <el-select v-model="form.versionId" clearable filterable style="width: 100%">
              <el-option v-for="item in formVersionOptions" :key="item.versionId" :label="`${item.versionNo} / ${item.versionStatus}`" :value="item.versionId" />
            </el-select>
          </el-form-item>
          <el-form-item label="任务类型" required>
            <el-radio-group v-model="form.taskType">
              <el-radio-button v-for="item in taskTypeOptions" :key="item.value" :label="item.value">{{ item.label }}</el-radio-button>
            </el-radio-group>
          </el-form-item>
          <el-form-item label="账期" required>
            <el-date-picker
              v-model="form.billMonth"
              type="month"
              format="YYYY-MM"
              value-format="YYYY-MM"
              placeholder="选择账期"
              style="width: 100%"
            />
          </el-form-item>
          <el-form-item label="请求号">
            <el-input v-model="form.requestNo" placeholder="可选，用于幂等提交" />
          </el-form-item>
          <el-form-item label="输入来源" required>
            <el-radio-group v-model="form.inputSourceType" @change="handleInputSourceChange">
              <el-radio-button label="INLINE_JSON">JSON 直传</el-radio-button>
              <el-radio-button label="INPUT_BATCH">导入批次</el-radio-button>
            </el-radio-group>
          </el-form-item>
          <el-alert
            class="run-page__strategy-alert"
            :title="submissionStrategy.title"
            :description="submissionStrategy.description"
            :type="submissionStrategy.type"
            :closable="false"
          />
          <el-alert
            v-if="taskPrecheck"
            class="run-page__precheck-alert"
            :type="taskPrecheck.passed ? (taskPrecheck.warningCount ? 'warning' : 'success') : 'error'"
            :title="taskPrecheck.message"
            :closable="true"
            @close="taskPrecheck = null"
          >
            <div class="run-page__precheck-meta">
              <span>输入 {{ taskPrecheck.inputCount || 0 }} 条</span>
              <span>阻断 {{ taskPrecheck.blockingCount || 0 }} 项</span>
              <span>提醒 {{ taskPrecheck.warningCount || 0 }} 项</span>
              <span v-if="taskPrecheck.versionNo">版本 {{ taskPrecheck.versionNo }}</span>
            </div>
            <ul v-if="taskPrecheckItems.length" class="run-page__precheck-list">
              <li v-for="item in taskPrecheckItems" :key="`${item.level}-${item.code}-${item.title}`">
                <strong>{{ item.title }}</strong>
                <span>{{ item.description }}</span>
              </li>
            </ul>
          </el-alert>
          <el-form-item label="备注">
            <el-input v-model="form.remark" type="textarea" :rows="2" maxlength="500" show-word-limit />
          </el-form-item>

          <template v-if="form.inputSourceType === 'INLINE_JSON'">
            <el-form-item label="输入 JSON" required>
              <JsonEditor v-model="form.inputJson" title="输入 JSON" :rows="14" :max-length="20000" :allow-empty="false" />
            </el-form-item>
            <el-alert
              v-if="inlineInputInsight"
              class="run-page__template-alert"
              :title="inlineInputInsight.title"
              :description="inlineInputInsight.description"
              :type="inlineInputInsight.type"
              :closable="false"
            />
          </template>

          <template v-else>
            <el-form-item label="导入批次" required>
              <div class="run-page__batch-select">
                <el-input v-model="form.sourceBatchNo" placeholder="请选择或录入导入批次号" />
                <el-button icon="Upload" @click="openBatchDialog">创建批次</el-button>
                <el-button icon="Tickets" @click="openBatchPicker">选择历史批次</el-button>
                <el-button :disabled="!form.sourceBatchNo" @click="loadBatchPreviewByBatchNo(form.sourceBatchNo)">查看批次</el-button>
              </div>
            </el-form-item>
            <el-alert
              v-if="selectedBatch.batchNo"
              :title="`已选择批次 ${selectedBatch.batchNo}，共 ${selectedBatch.totalCount || 0} 条，状态 ${selectedBatch.batchStatus || '-'}。`"
              type="info"
              :closable="false"
            />
            <div v-if="selectedBatch.batchNo" class="run-page__batch-card">
              <span>场景：{{ selectedBatch.sceneName || '-' }}</span>
              <span>版本：{{ selectedBatch.versionNo || '-' }}</span>
              <span>账期：{{ selectedBatch.billMonth || '-' }}</span>
              <span>有效/错误：{{ selectedBatch.validCount || 0 }}/{{ selectedBatch.errorCount || 0 }}</span>
            </div>
            <el-alert
              v-if="batchPreview.loadingGuide?.title"
              class="run-page__template-alert"
              :title="batchPreview.loadingGuide.title"
              :description="batchPreview.loadingGuide.description"
              :type="batchPreview.loadingGuide.type || 'info'"
              :closable="false"
            />
            <div v-if="batchPreview.items?.length" class="run-page__batch-preview">
              <div class="run-page__section-head">
                <div>
                  <h3>批次样例预览</h3>
                  <p>{{ buildSampleRangeText(batchPreviewQuery.pageNum, batchPreviewQuery.pageSize, batchPreview.itemTotal || 0, batchPreview.items?.length || 0) }}</p>
                </div>
              </div>
              <el-table :data="batchPreview.items || []" size="small" border>
                <el-table-column label="序号" prop="itemNo" width="80" align="center" />
                <el-table-column label="业务单号" prop="bizNo" min-width="180" />
                <el-table-column label="状态" prop="itemStatus" width="120" align="center" />
                <el-table-column label="输入摘要" min-width="340">
                  <template #default="scope">{{ summarizeJson(scope.row.inputJson) }}</template>
                </el-table-column>
              </el-table>
              <pagination
                v-show="(batchPreview.itemTotal || 0) > 0"
                :total="batchPreview.itemTotal || 0"
                v-model:page="batchPreviewQuery.pageNum"
                v-model:limit="batchPreviewQuery.pageSize"
                @pagination="handleBatchPreviewPageChange"
              />
            </div>
          </template>
        </el-form>

        <div class="run-page__action-row">
          <el-button type="primary" icon="Promotion" :loading="taskPrechecking" @click="handleSubmit" v-hasPermi="['cost:task:execute']">提交任务</el-button>
          <el-button icon="RefreshLeft" @click="fillExample">按配置生成模板</el-button>
          <el-button v-if="form.inputSourceType === 'INPUT_BATCH'" icon="View" :disabled="!form.sourceBatchNo" @click="loadBatchPreviewByBatchNo(form.sourceBatchNo)">
            刷新批次预览
          </el-button>
        </div>

        <el-alert v-if="templateMessage" :title="templateMessage" type="info" :closable="false" class="run-page__template-alert" />
        <el-table v-if="templateFields.length" :data="templateFields" size="small" border class="run-page__template-table">
          <el-table-column label="输入路径" prop="path" min-width="180" />
          <el-table-column label="变量" min-width="180">
            <template #default="scope">{{ scope.row.variableName }} ({{ scope.row.variableCode }})</template>
          </el-table-column>
          <el-table-column label="来源" prop="sourceType" width="120" />
          <el-table-column label="类型" prop="dataType" width="120" />
          <el-table-column label="模板角色" prop="templateRole" width="140" />
        </el-table>
      </div>

      <div class="run-page__panel">
        <div class="run-page__section-head">
          <div>
            <h3>任务台账</h3>
            <p>跟踪正式核算任务进度、失败明细和分片执行状态。</p>
          </div>
          <span class="run-page__panel-badge">共 {{ total }} 条</span>
        </div>

        <el-table v-loading="loading" :data="taskList" border class="run-page__table">
          <el-table-column label="任务编号" prop="taskNo" width="220" />
          <el-table-column label="场景" min-width="180">
            <template #default="scope">{{ scope.row.sceneName }} ({{ scope.row.sceneCode }})</template>
          </el-table-column>
          <el-table-column label="版本" prop="versionNo" width="160" />
          <el-table-column label="输入来源" width="120" align="center">
            <template #default="scope">{{ resolveInputSource(scope.row.inputSourceType) }}</template>
          </el-table-column>
          <el-table-column label="任务类型" width="140" align="center">
            <template #default="scope">
              <dict-tag :options="taskTypeOptions" :value="scope.row.taskType" />
            </template>
          </el-table-column>
          <el-table-column label="账期" prop="billMonth" width="110" align="center" />
          <el-table-column label="状态" width="120" align="center">
            <template #default="scope">
              <dict-tag :options="taskStatusOptions" :value="scope.row.taskStatus" />
            </template>
          </el-table-column>
          <el-table-column label="进度" width="180" align="center">
            <template #default="scope">
              <el-progress
                :percentage="normalizePercent(scope.row.progressPercent)"
                :status="resolveProgressStatus(scope.row.taskStatus)"
                :stroke-width="8"
              />
            </template>
          </el-table-column>
          <el-table-column label="成功/失败" width="120" align="center">
            <template #default="scope">{{ scope.row.successCount }}/{{ scope.row.failCount }}</template>
          </el-table-column>
          <el-table-column label="执行节点" prop="executeNode" width="120" align="center" />
          <el-table-column label="操作" width="240" fixed="right" align="center">
            <template #default="scope">
              <div class="cost-row-actions">
                <el-button link type="primary" icon="View" @click="handleDetail(scope.row)">详情</el-button>
                <el-button v-if="isTaskFailed(scope.row)" link type="danger" icon="WarningFilled" @click="handleDetail(scope.row, 'overview')">诊断</el-button>
                <el-button link type="success" icon="Histogram" @click="handleOpenPartitionMonitor(scope.row)">分片</el-button>
                <el-dropdown trigger="click" @command="command => handleTaskRowCommand(command, scope.row)">
                  <el-button link type="primary" icon="MoreFilled">更多</el-button>
                  <template #dropdown>
                    <el-dropdown-menu>
                      <el-dropdown-item command="result" icon="List">查看结果</el-dropdown-item>
                      <el-dropdown-item command="alert" icon="Warning">查看告警</el-dropdown-item>
                      <el-dropdown-item command="cancel" icon="CircleClose" v-hasPermi="['cost:task:cancel']">取消任务</el-dropdown-item>
                    </el-dropdown-menu>
                  </template>
                </el-dropdown>
              </div>
            </template>
          </el-table-column>
          <template #empty>
            <cost-table-empty
              title="当前没有正式核算任务"
              description="正式任务用于落库结果和后续追溯。可以在左侧提交任务，或先创建导入批次后再发起核算。"
            >
              <el-button type="primary" icon="Promotion" @click="handleSubmit" v-hasPermi="['cost:task:execute']">提交任务</el-button>
              <el-button plain icon="Upload" @click="openBatchDialog">创建批次</el-button>
              <el-button icon="Refresh" @click="resetQuery">清空筛选</el-button>
            </cost-table-empty>
          </template>
        </el-table>

        <pagination v-show="total > 0" :total="total" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" @pagination="getList" />
      </div>
    </section>

    <el-drawer v-model="detailOpen" title="任务详情工作台" size="1180px" append-to-body>
      <div class="run-page__detail-workbench">
        <section v-if="detailData.task" class="run-page__detail-hero">
          <div>
            <div class="run-page__eyebrow">任务身份</div>
            <h3>{{ detailData.task.taskNo }}</h3>
            <div class="run-page__detail-meta">
              <span>状态：{{ resolveTaskStatus(detailData.task.taskStatus) }}</span>
              <span>场景：{{ detailData.task.sceneName }}</span>
              <span>版本：{{ detailData.task.versionNo || '-' }}</span>
              <span>账期：{{ detailData.task.billMonth || '-' }}</span>
              <span>来源：{{ resolveInputSource(detailData.task.inputSourceType) }}</span>
              <span>批次：{{ detailData.task.sourceBatchNo || '-' }}</span>
              <span>执行节点：{{ detailData.task.executeNode || '-' }}</span>
            </div>
          </div>
          <div class="run-page__action-row run-page__detail-actions">
            <el-button icon="Warning" @click="openAlertCenter(detailData.task)">查看告警</el-button>
            <el-button icon="List" @click="openResultCenter(detailData.task)">查看结果</el-button>
          </div>
        </section>

        <div class="run-page__progress-panel">
          <div class="run-page__progress-title">
            <span>整体进度</span>
            <strong>{{ normalizePercent(partitionMonitorData.summary?.progressPercent) }}%</strong>
          </div>
          <el-progress
            :percentage="normalizePercent(partitionMonitorData.summary?.progressPercent)"
            :status="resolveProgressStatus(partitionMonitorData.task?.taskStatus)"
            :stroke-width="12"
          />
          <div class="run-page__progress-breakdown">
            <span>成功 {{ partitionMonitorData.summary?.successCount || 0 }}</span>
            <span>失败 {{ partitionMonitorData.summary?.failCount || 0 }}</span>
            <span>分片 {{ partitionMonitorData.summary?.partitionCount || 0 }}</span>
            <span>执行节点 {{ partitionMonitorData.summary?.activeOwnerCount || 0 }}</span>
          </div>
        </div>

        <div class="run-page__summary run-page__summary--detail">
          <div class="run-page__summary-card"><span>输入总量</span><strong>{{ detailData.summary?.sourceCount || 0 }}</strong></div>
          <div class="run-page__summary-card"><span>成功数量</span><strong>{{ detailData.summary?.successCount || 0 }}</strong></div>
          <div class="run-page__summary-card"><span>失败数量</span><strong>{{ detailData.summary?.failCount || 0 }}</strong></div>
          <div class="run-page__summary-card"><span>分片数量</span><strong>{{ detailData.summary?.partitionCount || 0 }}</strong></div>
          <div class="run-page__summary-card"><span>已认领分片</span><strong>{{ detailData.summary?.claimedPartitionCount || 0 }}</strong></div>
          <div class="run-page__summary-card"><span>僵尸风险分片</span><strong>{{ detailData.summary?.staleRunningOwnerCount || 0 }}</strong></div>
          <div class="run-page__summary-card"><span>无 owner 运行中</span><strong>{{ detailData.summary?.runningWithoutOwnerCount || 0 }}</strong></div>
        </div>

        <el-tabs v-model="detailActiveTab" class="run-page__detail-tabs">
          <el-tab-pane label="任务概览" name="overview">
            <div v-if="detailData.summary?.topErrors?.length" class="run-page__detail-section">
              <div class="run-page__section-head">
                <div>
                  <h3>失败聚合</h3>
                  <p>聚合展示当前任务最常见的失败原因，便于先处理高频问题。</p>
                </div>
              </div>
              <el-table :data="detailData.summary.topErrors" size="small" border>
                <el-table-column label="失败原因" prop="message" min-width="640" />
                <el-table-column label="出现次数" prop="count" width="120" align="center" />
              </el-table>
            </div>
            <el-empty v-else description="当前任务没有失败聚合信息，可继续查看分片或任务明细。" :image-size="72" />
          </el-tab-pane>

          <el-tab-pane label="关联批次" name="batch">
            <template v-if="detailData.inputBatch?.batch">
              <div class="run-page__batch-card">
                <span>批次号：{{ detailData.inputBatch.batch.batchNo }}</span>
                <span>场景：{{ detailData.inputBatch.batch.sceneName || '-' }}</span>
                <span>版本：{{ detailData.inputBatch.batch.versionNo || '-' }}</span>
                <span>总量：{{ detailData.inputBatch.batch.totalCount || 0 }}</span>
              </div>
              <el-alert
                v-if="detailData.inputBatch.loadingGuide?.title"
                class="run-page__template-alert"
                :title="detailData.inputBatch.loadingGuide.title"
                :description="detailData.inputBatch.loadingGuide.description"
                :type="detailData.inputBatch.loadingGuide.type || 'info'"
                :closable="false"
              />
              <p class="run-page__batch-range-tip">
                {{ buildSampleRangeText(detailBatchQuery.pageNum, detailBatchQuery.pageSize, detailData.inputBatch.itemTotal || 0, detailData.inputBatch.items?.length || 0) }}
              </p>
              <el-table :data="detailData.inputBatch.items || []" size="small" border>
                <el-table-column label="序号" prop="itemNo" width="80" align="center" />
                <el-table-column label="业务单号" prop="bizNo" min-width="180" />
                <el-table-column label="状态" prop="itemStatus" width="120" align="center" />
                <el-table-column label="输入摘要" min-width="320">
                  <template #default="scope">{{ summarizeJson(scope.row.inputJson) }}</template>
                </el-table-column>
              </el-table>
              <pagination
                v-show="(detailData.inputBatch.itemTotal || 0) > 0"
                :total="detailData.inputBatch.itemTotal || 0"
                v-model:page="detailBatchQuery.pageNum"
                v-model:limit="detailBatchQuery.pageSize"
                @pagination="handleDetailBatchPageChange"
              />
            </template>
            <el-empty v-else description="当前任务未关联导入批次，可能是 JSON 直传任务。" :image-size="72" />
          </el-tab-pane>

          <el-tab-pane label="分片执行" name="partition">
            <div class="run-page__detail-section">
              <div class="run-page__section-head">
                <div>
                  <h3>分片执行</h3>
                  <p>面向大批量任务的分片进度、失败摘要与分片级重试。</p>
                </div>
              </div>
              <el-table :data="detailData.partitions || []" size="small" border>
                <el-table-column label="分片号" prop="partitionNo" width="90" align="center" />
                <el-table-column label="范围" min-width="150">
                  <template #default="scope">{{ scope.row.startItemNo }} - {{ scope.row.endItemNo }}</template>
                </el-table-column>
                <el-table-column label="认领节点" width="120" align="center">
                  <template #default="scope">{{ scope.row.executeNode || '-' }}</template>
                </el-table-column>
                <el-table-column label="认领时间" min-width="170" align="center">
                  <template #default="scope">{{ scope.row.claimTime || scope.row.startedTime || '-' }}</template>
                </el-table-column>
                <el-table-column label="状态" width="120" align="center">
                  <template #default="scope">
                    <el-tag :type="resolvePartitionTag(scope.row.partitionStatus)">{{ resolveTaskStatus(scope.row.partitionStatus) }}</el-tag>
                  </template>
                </el-table-column>
                <el-table-column label="落库模式" width="120" align="center">
                  <template #default="scope">
                    <el-tag :type="resolvePartitionPersistModeTag(scope.row.persistMode)">{{ resolvePartitionPersistMode(scope.row.persistMode) }}</el-tag>
                  </template>
                </el-table-column>
                <el-table-column label="总量" prop="totalCount" width="90" align="center" />
                <el-table-column label="成功/失败" width="120" align="center">
                  <template #default="scope">{{ scope.row.successCount || 0 }}/{{ scope.row.failCount || 0 }}</template>
                </el-table-column>
                <el-table-column label="耗时(ms)" prop="durationMs" width="110" align="center" />
                <el-table-column label="恢复提示" prop="recoveryHint" min-width="260" />
                <el-table-column label="错误摘要" prop="lastError" min-width="220" />
                <el-table-column label="操作" width="130" fixed="right" align="center">
                  <template #default="scope">
                    <el-button link type="primary" icon="RefreshRight" :disabled="!canRetryPartition(scope.row)" @click="handleRetryPartition(scope.row)" v-hasPermi="['cost:task:retry']">
                      重试分片
                    </el-button>
                  </template>
                </el-table-column>
              </el-table>
            </div>
          </el-tab-pane>

          <el-tab-pane label="任务明细" name="details">
            <div class="run-page__detail-section">
              <div class="run-page__section-head">
                <div>
                  <h3>任务明细</h3>
                  <p>明细改为分页加载，兼容大批量任务下的失败定位与明细级重试。</p>
                </div>
              </div>
              <p class="run-page__batch-range-tip">
                {{ buildSampleRangeText(detailQuery.pageNum, detailQuery.pageSize, detailData.detailPage?.total || 0, detailData.details?.length || 0) }}
              </p>
              <el-table :data="detailData.details || []" size="small" border>
                <el-table-column label="分片号" prop="partitionNo" width="90" align="center" />
                <el-table-column label="业务单号" prop="bizNo" min-width="180" />
                <el-table-column label="状态" width="120" align="center">
                  <template #default="scope">
                    <el-tag :type="resolveDetailTag(scope.row.detailStatus)">{{ scope.row.detailStatus }}</el-tag>
                  </template>
                </el-table-column>
                <el-table-column label="重试次数" prop="retryCount" width="100" align="center" />
                <el-table-column label="结果摘要" prop="resultSummary" min-width="240" />
                <el-table-column label="异常信息" prop="errorMessage" min-width="200" />
                <el-table-column label="操作" width="130" fixed="right" align="center">
                  <template #default="scope">
                    <el-button link type="primary" icon="RefreshRight" :disabled="scope.row.detailStatus !== 'FAILED'" @click="handleRetry(scope.row)" v-hasPermi="['cost:task:retry']">
                      重试
                    </el-button>
                  </template>
                </el-table-column>
              </el-table>
              <pagination
                v-show="(detailData.detailPage?.total || 0) > 0"
                :total="detailData.detailPage?.total || 0"
                v-model:page="detailQuery.pageNum"
                v-model:limit="detailQuery.pageSize"
                @pagination="handleDetailPageChange"
              />
            </div>
          </el-tab-pane>
        </el-tabs>
      </div>
    </el-drawer>

    <el-dialog v-model="batchDialogOpen" title="创建导入批次" width="920px" append-to-body>
      <el-form :model="batchForm" label-width="92px">
        <el-form-item label="运行场景" required>
          <el-select v-model="batchForm.sceneId" filterable style="width: 100%">
            <el-option v-for="item in sceneOptions" :key="item.sceneId" :label="`${item.sceneName} / ${item.sceneCode}`" :value="item.sceneId" />
          </el-select>
        </el-form-item>
        <el-form-item label="执行版本">
          <el-select v-model="batchForm.versionId" clearable filterable style="width: 100%">
            <el-option v-for="item in formVersionOptions" :key="item.versionId" :label="`${item.versionNo} / ${item.versionStatus}`" :value="item.versionId" />
          </el-select>
        </el-form-item>
        <el-form-item label="账期" required>
          <el-date-picker
            v-model="batchForm.billMonth"
            type="month"
            format="YYYY-MM"
            value-format="YYYY-MM"
            placeholder="选择账期"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="批次备注">
          <el-input v-model="batchForm.remark" type="textarea" :rows="2" maxlength="500" show-word-limit />
        </el-form-item>
        <el-form-item label="批量 JSON" required>
          <JsonEditor v-model="batchForm.inputJson" title="批量 JSON" :rows="16" :max-length="60000" :allow-empty="false" placeholder="请输入 JSON 数组，例如 [{...}, {...}]" />
        </el-form-item>
      </el-form>

      <el-alert v-if="batchPreview.batch?.batchNo" class="run-page__template-alert" type="success" :closable="false" :title="`批次 ${batchPreview.batch.batchNo} 创建成功，共 ${batchPreview.batch.totalCount || 0} 条。`" />
      <el-table v-if="batchPreview.items?.length" :data="batchPreview.items || []" size="small" border>
        <el-table-column label="序号" prop="itemNo" width="80" align="center" />
        <el-table-column label="业务单号" prop="bizNo" min-width="180" />
        <el-table-column label="状态" prop="itemStatus" width="120" align="center" />
        <el-table-column label="输入摘要" min-width="340">
          <template #default="scope">{{ summarizeJson(scope.row.inputJson) }}</template>
          </el-table-column>
      </el-table>
      <pagination
        v-show="(batchPreview.itemTotal || 0) > 0"
        :total="batchPreview.itemTotal || 0"
        v-model:page="batchPreviewQuery.pageNum"
        v-model:limit="batchPreviewQuery.pageSize"
        @pagination="handleBatchPreviewPageChange"
      />

      <template #footer>
        <div class="dialog-footer">
          <el-button @click="batchDialogOpen = false">关闭</el-button>
          <el-button @click="fillBatchExample">带入当前模板</el-button>
          <el-button type="primary" @click="handleCreateBatch">创建批次</el-button>
        </div>
      </template>
    </el-dialog>

    <el-dialog v-model="batchPickerOpen" title="选择历史导入批次" width="1100px" append-to-body>
      <el-form :model="batchQuery" :inline="true" label-width="80px">
        <el-form-item label="场景">
          <el-select v-model="batchQuery.sceneId" clearable filterable style="width: 220px">
            <el-option v-for="item in sceneOptions" :key="item.sceneId" :label="`${item.sceneName} / ${item.sceneCode}`" :value="item.sceneId" />
          </el-select>
        </el-form-item>
        <el-form-item label="账期">
          <el-date-picker
            v-model="batchQuery.billMonth"
            clearable
            type="month"
            format="YYYY-MM"
            value-format="YYYY-MM"
            placeholder="选择账期"
            style="width: 140px"
          />
        </el-form-item>
        <el-form-item label="批次号">
          <el-input v-model="batchQuery.batchNo" placeholder="关键字查询" style="width: 220px" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="batchQuery.batchStatus" clearable style="width: 160px">
            <el-option label="READY" value="READY" />
            <el-option label="SUBMITTED" value="SUBMITTED" />
            <el-option label="CONSUMED" value="CONSUMED" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" icon="Search" @click="loadBatchList">搜索</el-button>
          <el-button icon="Refresh" @click="resetBatchQuery">重置</el-button>
        </el-form-item>
      </el-form>

      <el-table v-loading="batchListLoading" :data="batchList" size="small" border>
        <el-table-column label="批次号" prop="batchNo" width="220" />
        <el-table-column label="场景" min-width="180">
          <template #default="scope">{{ scope.row.sceneName }} ({{ scope.row.sceneCode }})</template>
        </el-table-column>
        <el-table-column label="版本" prop="versionNo" width="140" />
        <el-table-column label="账期" prop="billMonth" width="110" align="center" />
        <el-table-column label="状态" prop="batchStatus" width="120" align="center" />
        <el-table-column label="条数" prop="totalCount" width="90" align="center" />
        <el-table-column label="有效/错误" width="120" align="center">
          <template #default="scope">{{ scope.row.validCount || 0 }}/{{ scope.row.errorCount || 0 }}</template>
        </el-table-column>
        <el-table-column label="操作" width="150" fixed="right" align="center">
          <template #default="scope">
            <el-button link type="primary" @click="handleSelectBatch(scope.row)">选择</el-button>
            <el-button link type="success" @click="previewBatchRow(scope.row)">预览</el-button>
          </template>
        </el-table-column>
      </el-table>

      <pagination v-show="batchTotal > 0" :total="batchTotal" v-model:page="batchQuery.pageNum" v-model:limit="batchQuery.pageSize" @pagination="loadBatchList" />
    </el-dialog>

    <el-dialog v-model="partitionMonitorOpen" title="分片监控" width="1100px" append-to-body>
      <div class="run-page__monitor-workbench">
        <section v-if="partitionMonitorData.task" class="run-page__detail-hero">
          <div>
            <div class="run-page__eyebrow">运行监控</div>
            <h3>{{ partitionMonitorData.task.taskNo }}</h3>
            <div class="run-page__detail-meta">
              <span>状态：{{ resolveTaskStatus(partitionMonitorData.task.taskStatus) }}</span>
              <span>场景：{{ partitionMonitorData.task.sceneName }}</span>
              <span>账期：{{ partitionMonitorData.task.billMonth || '-' }}</span>
              <span>执行节点：{{ partitionMonitorData.task.executeNode || '-' }}</span>
            </div>
          </div>
          <div class="run-page__action-row run-page__detail-actions">
            <el-button icon="Warning" @click="openAlertCenter(partitionMonitorData.task)">查看告警</el-button>
            <el-button icon="List" @click="openResultCenter(partitionMonitorData.task)">查看结果</el-button>
          </div>
        </section>

        <div class="run-page__summary run-page__summary--detail">
          <div class="run-page__summary-card"><span>分片总数</span><strong>{{ partitionMonitorData.summary?.partitionCount || 0 }}</strong></div>
          <div class="run-page__summary-card"><span>输入总量</span><strong>{{ partitionMonitorData.summary?.sourceCount || 0 }}</strong></div>
          <div class="run-page__summary-card"><span>成功数量</span><strong>{{ partitionMonitorData.summary?.successCount || 0 }}</strong></div>
          <div class="run-page__summary-card"><span>失败数量</span><strong>{{ partitionMonitorData.summary?.failCount || 0 }}</strong></div>
          <div class="run-page__summary-card"><span>已认领分片</span><strong>{{ partitionMonitorData.summary?.claimedPartitionCount || 0 }}</strong></div>
          <div class="run-page__summary-card"><span>僵尸风险分片</span><strong>{{ partitionMonitorData.summary?.staleRunningOwnerCount || 0 }}</strong></div>
          <div class="run-page__summary-card"><span>无 owner 运行中</span><strong>{{ partitionMonitorData.summary?.runningWithoutOwnerCount || 0 }}</strong></div>
        </div>
      </div>

      <el-table :data="partitionMonitorData.partitions || []" size="small" border class="run-page__table">
        <el-table-column label="分片号" prop="partitionNo" width="90" align="center" />
        <el-table-column label="范围" min-width="150">
          <template #default="scope">{{ scope.row.startItemNo }} - {{ scope.row.endItemNo }}</template>
        </el-table-column>
        <el-table-column label="认领节点" width="120" align="center">
          <template #default="scope">{{ scope.row.executeNode || '-' }}</template>
        </el-table-column>
        <el-table-column label="认领时间" min-width="170" align="center">
          <template #default="scope">{{ scope.row.claimTime || scope.row.startedTime || '-' }}</template>
        </el-table-column>
        <el-table-column label="状态" width="120" align="center">
          <template #default="scope">
            <el-tag :type="resolvePartitionTag(scope.row.partitionStatus)">{{ resolveTaskStatus(scope.row.partitionStatus) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="分片进度" width="160" align="center">
          <template #default="scope">
            <el-progress
              :percentage="resolvePartitionProgress(scope.row)"
              :status="resolveProgressStatus(scope.row.partitionStatus)"
              :stroke-width="8"
            />
          </template>
        </el-table-column>
        <el-table-column label="落库模式" width="120" align="center">
          <template #default="scope">
            <el-tag :type="resolvePartitionPersistModeTag(scope.row.persistMode)">{{ resolvePartitionPersistMode(scope.row.persistMode) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="总量" prop="totalCount" width="90" align="center" />
        <el-table-column label="成功/失败" width="120" align="center">
          <template #default="scope">{{ scope.row.successCount || 0 }}/{{ scope.row.failCount || 0 }}</template>
        </el-table-column>
        <el-table-column label="耗时(ms)" prop="durationMs" width="110" align="center" />
        <el-table-column label="恢复提示" prop="recoveryHint" min-width="260" />
        <el-table-column label="错误摘要" prop="lastError" min-width="220" />
        <el-table-column label="操作" width="130" fixed="right" align="center">
          <template #default="scope">
            <el-button link type="primary" icon="RefreshRight" :disabled="!canRetryPartition(scope.row)" @click="handleRetryPartition(scope.row)" v-hasPermi="['cost:task:retry']">
              重试分片
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-dialog>
  </div>
</template>

<script setup name="CostTask">
import { ElMessageBox } from 'element-plus'
import JsonEditor from '@/components/cost/JsonEditor.vue'
import {
  cancelTask,
  createTaskInputBatch,
  getRunInputTemplate,
  getTaskDetail,
  getTaskOverview,
  getTaskInputBatchDetail,
  getTaskStats,
  listTask,
  listTaskInputBatch,
  listVersionOptions,
  precheckTask,
  retryTaskDetail,
  retryTaskPartition,
  submitTask
} from '@/api/cost/run'
import { optionselectScene } from '@/api/cost/scene'
import useSettingsStore from '@/store/modules/settings'
import { resolveWorkingCostSceneId } from '@/utils/costSceneContext'
import { confirmCostSceneSwitch } from '@/utils/costSceneSwitchGuard'
import { COST_MENU_ROUTES } from '@/utils/costMenuRoutes'
import { confirmCostNextAction } from '@/utils/costNextAction'
import { clearCostWorkContext, resolveWorkingBillMonth, resolveWorkingVersionId, syncCostWorkContext } from '@/utils/costWorkContext'
import { getRemoteDictOptionMap } from '@/utils/dictRemote'

const route = useRoute()
const router = useRouter()
const { proxy } = getCurrentInstance()
const settingsStore = useSettingsStore()
const isCompactMode = computed(() => settingsStore.costPageMode === 'COMPACT')

const loading = ref(false)
const showSearch = ref(true)
const total = ref(0)
const taskList = ref([])
const sceneOptions = ref([])
const versionOptions = ref([])
const formVersionOptions = ref([])
const taskTypeOptions = ref([])
const taskStatusOptions = ref([])
const templateFields = ref([])
const templateMessage = ref('')
const detailOpen = ref(false)
const detailData = ref({})
const detailActiveTab = ref('overview')
const batchDialogOpen = ref(false)
const batchPickerOpen = ref(false)
const batchPreview = ref({})
const batchListLoading = ref(false)
const batchList = ref([])
const batchTotal = ref(0)
const partitionMonitorOpen = ref(false)
const partitionMonitorData = ref({})
const selectedBatch = reactive({})
const batchPreviewQuery = reactive({
  pageNum: 1,
  pageSize: 10
})
const detailBatchQuery = reactive({
  pageNum: 1,
  pageSize: 10
})
const detailQuery = reactive({
  pageNum: 1,
  pageSize: 20
})
const stats = reactive({ taskCount: 0, runningCount: 0, successCount: 0, failedCount: 0 })
const overview = reactive({
  recentTaskTrend: [],
  recentPartitionTrend: [],
  topRiskTasks: [],
  taskStatusDistribution: [],
  inputSourceDistribution: [],
  ownerSummary: {},
  partitionOwnerDistribution: [],
  topOwnerRiskTasks: []
})
const routeTaskContext = reactive({
  taskId: route.query.taskId ? Number(route.query.taskId) : undefined,
  view: route.query.view || ''
})
const lastOpenedRouteTaskKey = ref('')
const lastQuerySceneId = ref(undefined)
const lastFormSceneId = ref(undefined)
const taskPrechecking = ref(false)
const taskPrecheck = ref(null)

const queryParams = reactive({
  pageNum: 1,
  pageSize: 10,
  taskId: route.query.taskId ? Number(route.query.taskId) : undefined,
  sceneId: route.query.sceneId ? Number(route.query.sceneId) : undefined,
  versionId: resolveWorkingVersionId(route.query.versionId ? Number(route.query.versionId) : undefined),
  taskType: undefined,
  taskStatus: undefined,
  billMonth: resolveWorkingBillMonth(route.query.billMonth)
})

const form = reactive({
  sceneId: route.query.sceneId ? Number(route.query.sceneId) : undefined,
  versionId: resolveWorkingVersionId(route.query.versionId ? Number(route.query.versionId) : undefined),
  taskType: 'FORMAL_SINGLE',
  billMonth: resolveWorkingBillMonth(route.query.billMonth),
  requestNo: '',
  inputSourceType: route.query.inputSourceType || 'INLINE_JSON',
  sourceBatchNo: route.query.sourceBatchNo || '',
  inputJson: '',
  remark: ''
})

const batchForm = reactive({
  sceneId: undefined,
  versionId: resolveWorkingVersionId(route.query.versionId ? Number(route.query.versionId) : undefined),
  billMonth: resolveWorkingBillMonth(route.query.billMonth, form.billMonth),
  inputJson: '',
  remark: ''
})

const batchQuery = reactive({
  pageNum: 1,
  pageSize: 8,
  sceneId: undefined,
  billMonth: resolveWorkingBillMonth(route.query.billMonth),
  batchNo: '',
  batchStatus: ''
})

const metricItems = computed(() => [
  { label: '任务总数', value: stats.taskCount, desc: '当前筛选范围内的正式核算任务总数' },
  { label: '执行中', value: stats.runningCount, desc: '仍在异步执行中的任务数' },
  { label: '成功任务', value: stats.successCount, desc: '全部执行成功的任务数' },
  { label: '失败/部分成功', value: stats.failedCount, desc: '需要关注或重试的任务数' }
])

const ownerSummaryItems = computed(() => [
  { label: '已认领分片', value: overview.ownerSummary?.claimedPartitionCount || 0 },
  { label: '活跃节点数', value: overview.ownerSummary?.activeOwnerCount || 0 },
  { label: '僵尸风险分片', value: overview.ownerSummary?.staleRunningOwnerCount || 0 },
  { label: '无 owner 运行中', value: overview.ownerSummary?.runningWithoutOwnerCount || 0 }
])

const submissionStrategy = computed(() => {
  if (form.inputSourceType === 'INPUT_BATCH') {
    return {
      type: 'success',
      title: '当前使用导入批次模式',
      description: '导入批次更符合企业级正式核算口径，适合生产批量装载、分片执行、失败恢复和台账追溯。'
    }
  }
  return {
    type: 'warning',
    title: '当前使用 JSON 直传模式',
    description: 'JSON 直传更适合联调、补录和小批量验证；生产批量任务建议先创建导入批次，再由任务中心提交正式核算。'
  }
})

const taskPrecheckItems = computed(() => [
  ...(taskPrecheck.value?.blockingItems || []),
  ...(taskPrecheck.value?.warningItems || [])
])

const inlineInputInsight = computed(() => {
  if (form.inputSourceType !== 'INLINE_JSON') return null
  const statsResult = inspectInlineInputJson(form.inputJson)
  if (statsResult.invalid) {
    return {
      type: 'error',
      title: '当前输入不是有效的计费对象 JSON',
      description: '正式核算仅支持 JSON 对象或对象数组。若输入来自业务系统大宽表，建议先走数据接入中心或导入批次。'
    }
  }
  if (!statsResult.count) {
    return {
      type: 'info',
      title: '当前模板适合单笔联调',
      description: '填充少量样例后可直接验证；若后续要跑生产批次，建议转成导入批次以保留装载、分片与恢复能力。'
    }
  }
  const partitionCount = Math.max(1, Math.ceil(statsResult.count / 500))
  if (statsResult.count > 500) {
    return {
      type: 'warning',
      title: `当前输入共 ${statsResult.count} 条，预计拆成 ${partitionCount} 个分片执行`,
      description: '这类规模已接近批量任务口径。企业级使用建议先创建导入批次，再通过任务中心提交，便于分页预览、恢复和留痕。'
    }
  }
  return {
    type: 'info',
    title: `当前输入共 ${statsResult.count} 条，预计拆成 ${partitionCount} 个分片执行`,
    description: '当前规模可继续在线提交；如果后续需要重复执行或与上游系统对接，仍建议沉淀为导入批次。'
  }
})

async function loadBaseOptions() {
  const [dictMap, sceneResp] = await Promise.all([
    getRemoteDictOptionMap(['cost_calc_task_type', 'cost_calc_task_status']),
    optionselectScene({ status: '0', pageNum: 1, pageSize: 1000 })
  ])
  taskTypeOptions.value = dictMap.cost_calc_task_type || []
  taskStatusOptions.value = dictMap.cost_calc_task_status || []
  sceneOptions.value = sceneResp?.data || []
  const preferredSceneId = resolveWorkingCostSceneId(sceneOptions.value)
  if (!queryParams.sceneId) queryParams.sceneId = preferredSceneId
  if (!form.sceneId) form.sceneId = preferredSceneId
  if (!batchForm.sceneId) batchForm.sceneId = preferredSceneId
  if (!batchQuery.sceneId) batchQuery.sceneId = preferredSceneId
  lastQuerySceneId.value = queryParams.sceneId
  lastFormSceneId.value = form.sceneId
}

async function loadVersionOptions(sceneId, target) {
  if (!sceneId) {
    target.value = []
    return
  }
  const resp = await listVersionOptions(sceneId)
  target.value = resp.data || []
}

async function getList() {
  loading.value = true
  try {
    await loadBaseOptions()
    if (queryParams.sceneId) {
      await loadVersionOptions(queryParams.sceneId, versionOptions)
    } else {
      versionOptions.value = []
    }
    if (form.sceneId) {
      await loadVersionOptions(form.sceneId, formVersionOptions)
    } else {
      formVersionOptions.value = []
    }
    const [listResp, statsResp, overviewResp] = await Promise.all([
      listTask(queryParams),
      getTaskStats(queryParams),
      getTaskOverview(queryParams)
    ])
    taskList.value = listResp.rows || []
    total.value = listResp.total || 0
    Object.assign(stats, statsResp.data || {})
    Object.assign(overview, {
      recentTaskTrend: overviewResp?.data?.recentTaskTrend || [],
      recentPartitionTrend: overviewResp?.data?.recentPartitionTrend || [],
      topRiskTasks: overviewResp?.data?.topRiskTasks || [],
      taskStatusDistribution: overviewResp?.data?.taskStatusDistribution || [],
      inputSourceDistribution: overviewResp?.data?.inputSourceDistribution || [],
      ownerSummary: overviewResp?.data?.ownerSummary || {},
      partitionOwnerDistribution: overviewResp?.data?.partitionOwnerDistribution || [],
      topOwnerRiskTasks: overviewResp?.data?.topOwnerRiskTasks || []
    })
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
  queryParams.taskId = routeTaskContext.taskId
  queryParams.billMonth = resolveWorkingBillMonth(route.query.billMonth, form.billMonth)
  versionOptions.value = []
  getList()
}

async function handleQuerySceneChange(sceneId) {
  const confirmed = await confirmCostSceneSwitch({
    currentSceneId: lastQuerySceneId.value,
    nextSceneId: sceneId,
    sceneOptions: sceneOptions.value,
    scope: '正式核算任务筛选'
  })
  if (!confirmed) {
    queryParams.sceneId = lastQuerySceneId.value
    return
  }
  queryParams.versionId = undefined
  queryParams.sceneId = sceneId
  batchQuery.sceneId = sceneId
  lastQuerySceneId.value = sceneId
  clearCostWorkContext(['versionId'])
  syncCostWorkContext({ sceneId, billMonth: queryParams.billMonth || form.billMonth })
  await loadVersionOptions(sceneId, versionOptions)
}

async function handleFormSceneChange(sceneId) {
  const confirmed = await confirmCostSceneSwitch({
    currentSceneId: lastFormSceneId.value,
    nextSceneId: sceneId,
    sceneOptions: sceneOptions.value,
    scope: '正式核算提交场景'
  })
  if (!confirmed) {
    form.sceneId = lastFormSceneId.value
    return
  }
  form.versionId = undefined
  queryParams.versionId = undefined
  batchForm.versionId = undefined
  form.sceneId = sceneId
  batchForm.sceneId = sceneId
  batchQuery.sceneId = sceneId
  queryParams.sceneId = sceneId
  lastFormSceneId.value = sceneId
  lastQuerySceneId.value = sceneId
  clearCostWorkContext(['versionId'])
  syncCostWorkContext({ sceneId, billMonth: form.billMonth })
  await loadVersionOptions(sceneId, formVersionOptions)
  await loadVersionOptions(sceneId, versionOptions)
  await fillExample()
}

function handleInputSourceChange(value) {
  if (value === 'INLINE_JSON') {
    clearReactiveObject(selectedBatch)
    form.sourceBatchNo = ''
    return
  }
  if (!form.sourceBatchNo && batchPreview.value?.batch?.batchNo) {
    applyBatchPreview(batchPreview.value)
  }
}

async function fillExample() {
  if (!form.sceneId) {
    templateFields.value = []
    templateMessage.value = '请先选择运行场景，再按发布快照生成输入模板。'
    return
  }
  const response = await getRunInputTemplate({
    sceneId: form.sceneId,
    versionId: form.versionId,
    taskType: form.taskType
  })
  form.inputJson = response?.data?.inputJson || '{}'
  templateFields.value = response?.data?.fields?.filter(item => item.includedInTemplate) || []
  templateMessage.value = response?.data?.message || ''
}

function fillBatchExample() {
  batchForm.sceneId = form.sceneId
  batchForm.versionId = form.versionId
  batchForm.billMonth = form.billMonth
  batchForm.inputJson = normalizeBatchExample(form.inputJson)
  batchForm.remark = form.remark
}

function openBatchDialog() {
  fillBatchExample()
  batchPreviewQuery.pageNum = 1
  batchPreviewQuery.pageSize = 10
  batchPreview.value = {}
  batchDialogOpen.value = true
}

async function handleCreateBatch() {
  if (!batchForm.sceneId || !batchForm.billMonth || !batchForm.inputJson) {
    proxy.$modal.msgWarning('请填写导入批次的场景、账期和批量 JSON 数组')
    return
  }
  batchPreviewQuery.pageNum = 1
  batchPreviewQuery.pageSize = 10
  const resp = await createTaskInputBatch({ ...batchForm })
  batchPreview.value = resp.data || {}
  applyBatchPreview(batchPreview.value)
  form.inputSourceType = 'INPUT_BATCH'
  form.sceneId = selectedBatch.sceneId || form.sceneId
  form.versionId = selectedBatch.versionId || form.versionId
  form.billMonth = selectedBatch.billMonth || form.billMonth
  batchDialogOpen.value = false
  proxy.$modal.msgSuccess('导入批次已创建，可直接用于正式核算任务')
}

function openBatchPicker() {
  batchPickerOpen.value = true
  loadBatchList()
}

async function loadBatchList() {
  batchListLoading.value = true
  try {
    const resp = await listTaskInputBatch({ ...batchQuery })
    batchList.value = resp.rows || []
    batchTotal.value = resp.total || 0
  } finally {
    batchListLoading.value = false
  }
}

function resetBatchQuery() {
  batchQuery.pageNum = 1
  batchQuery.pageSize = 8
  batchQuery.batchNo = ''
  batchQuery.billMonth = resolveWorkingBillMonth(form.billMonth)
  batchQuery.batchStatus = ''
  batchQuery.sceneId = form.sceneId || batchQuery.sceneId
  loadBatchList()
}

async function handleSelectBatch(row) {
  await loadBatchPreviewById(row.batchId, { resetPreviewPage: true })
  form.inputSourceType = 'INPUT_BATCH'
  form.sourceBatchNo = row.batchNo
  form.sceneId = row.sceneId || form.sceneId
  form.versionId = row.versionId || form.versionId
  form.billMonth = row.billMonth || form.billMonth
  batchPickerOpen.value = false
}

async function previewBatchRow(row) {
  await loadBatchPreviewById(row.batchId, { resetPreviewPage: true })
}

async function loadBatchPreviewByBatchNo(batchNo) {
  if (!batchNo) return
  if (detailData.value?.task?.sourceBatchNo === batchNo && detailData.value?.inputBatch?.batch?.batchId) {
    await loadBatchPreviewById(detailData.value.inputBatch.batch.batchId, { resetPreviewPage: true })
    return
  }
  if (batchPreview.value?.batch?.batchNo === batchNo) {
    applyBatchPreview(batchPreview.value)
    return
  }
  const matched = batchList.value.find(item => item.batchNo === batchNo)
  if (matched?.batchId) {
    await loadBatchPreviewById(matched.batchId, { resetPreviewPage: true })
    return
  }
  batchPickerOpen.value = true
  batchQuery.batchNo = batchNo
  await loadBatchList()
  const current = batchList.value.find(item => item.batchNo === batchNo)
  if (current?.batchId) {
    await loadBatchPreviewById(current.batchId, { resetPreviewPage: true })
    return
  }
  proxy.$modal.msgWarning('未找到对应批次，请先通过历史批次列表查询')
}

async function loadBatchPreviewById(batchId, options = {}) {
  if (options.resetPreviewPage) {
    batchPreviewQuery.pageNum = 1
    batchPreviewQuery.pageSize = 10
  }
  const resp = await getTaskInputBatchDetail(batchId, batchPreviewQuery)
  applyBatchPreview(resp.data)
}

function applyBatchPreview(data) {
  batchPreview.value = data || {}
  clearReactiveObject(selectedBatch)
  const batch = data?.batch || {}
  Object.assign(selectedBatch, batch)
  if (batch.batchNo) {
    form.sourceBatchNo = batch.batchNo
  }
}

async function handleBatchPreviewPageChange() {
  const batchId = batchPreview.value?.batch?.batchId || selectedBatch.batchId
  if (!batchId) return
  await loadBatchPreviewById(batchId)
}

async function handleDetailBatchPageChange() {
  const batchId = detailData.value?.inputBatch?.batch?.batchId
  if (!batchId) return
  const resp = await getTaskInputBatchDetail(batchId, detailBatchQuery)
  detailData.value = {
    ...detailData.value,
    inputBatch: resp.data || {}
  }
}

async function handleSubmit() {
  if (!form.sceneId || !form.billMonth) {
    proxy.$modal.msgWarning('请填写运行场景和账期')
    return
  }
  if (form.inputSourceType === 'INLINE_JSON' && !form.inputJson) {
    proxy.$modal.msgWarning('请输入输入 JSON')
    return
  }
  if (form.inputSourceType === 'INPUT_BATCH' && !form.sourceBatchNo) {
    proxy.$modal.msgWarning('请选择导入批次')
    return
  }
  const payload = buildTaskSubmitPayload()
  const precheckPassed = await runTaskSubmitPrecheck(payload)
  if (!precheckPassed) {
    return
  }
  const resp = await submitTask(payload)
  proxy.$modal.msgSuccess('任务已提交')
  detailData.value = resp.data || {}
  detailOpen.value = true
  getList()
  const task = detailData.value.task || detailData.value
  const goNext = await confirmCostNextAction({
    message: '正式核算任务已提交。建议继续到结果中心跟踪本次任务的入账结果和异常明细。',
    confirmButtonText: '去结果中心'
  })
  if (goNext) {
    router.push({
      path: COST_MENU_ROUTES.result,
      query: {
        sceneId: task.sceneId || form.sceneId,
        versionId: task.versionId || form.versionId,
        billMonth: task.billMonth || form.billMonth,
        taskId: task.taskId
      }
    })
  }
}

function buildTaskSubmitPayload() {
  const payload = { ...form }
  if (payload.inputSourceType === 'INPUT_BATCH') {
    payload.inputJson = ''
  }
  return payload
}

async function runTaskSubmitPrecheck(payload) {
  taskPrechecking.value = true
  try {
    const resp = await precheckTask(payload)
    taskPrecheck.value = resp.data || {}
  } finally {
    taskPrechecking.value = false
  }
  if (!taskPrecheck.value?.passed) {
    proxy.$modal.msgWarning('任务创建前预检存在阻断项，请先处理后再提交')
    return false
  }
  if ((taskPrecheck.value.warningCount || 0) > 0) {
    try {
      await ElMessageBox.confirm(buildTaskPrecheckConfirmMessage(taskPrecheck.value), '任务创建前预检', {
        type: 'warning',
        confirmButtonText: '继续提交',
        cancelButtonText: '返回检查'
      })
    } catch {
      return false
    }
  }
  return true
}

function buildTaskPrecheckConfirmMessage(precheck) {
  const items = (precheck.warningItems || []).slice(0, 5)
  const detail = items.map(item => `${item.title}：${item.description}`).join('\n')
  const suffix = (precheck.warningItems || []).length > items.length ? `\n等 ${precheck.warningItems.length} 项提醒` : ''
  return `预检已通过，但存在提醒项。\n输入 ${precheck.inputCount || 0} 条，版本 ${precheck.versionNo || '-'}。\n${detail}${suffix}`
}

function openBatchLedger() {
  router.push({ path: COST_MENU_ROUTES.taskBatch, query: { sceneId: form.sceneId, versionId: form.versionId, billMonth: form.billMonth } })
}

function goSimulation() {
  router.push({
    path: COST_MENU_ROUTES.simulation,
    query: {
      sceneId: form.sceneId,
      versionId: form.versionId,
      billMonth: form.billMonth
    }
  })
}

function openAlertCenter(task) {
  if (!task?.taskId) {
    router.push(COST_MENU_ROUTES.alert)
    return
  }
  router.push({
    path: COST_MENU_ROUTES.alert,
    query: {
      sceneId: task.sceneId,
      versionId: task.versionId,
      billMonth: task.billMonth,
      taskId: task.taskId,
      alarmStatus: 'OPEN'
    }
  })
}

function openTaskCenterById(taskId, billMonth, view = 'detail') {
  router.push({
    path: COST_MENU_ROUTES.task,
    query: {
      taskId,
      billMonth,
      sceneId: queryParams.sceneId,
      versionId: queryParams.versionId || form.versionId,
      view
    }
  })
}

function openResultCenter(task) {
  if (!task?.taskId) {
    router.push(COST_MENU_ROUTES.result)
    return
  }
  router.push({
    path: COST_MENU_ROUTES.result,
    query: {
      sceneId: task.sceneId,
      versionId: task.versionId,
      billMonth: task.billMonth,
      taskId: task.taskId,
      view: 'list'
    }
  })
}

async function handleDetail(row, activeTab = 'overview') {
  detailQuery.pageNum = 1
  const resp = await getTaskDetail(row.taskId, detailQuery)
  detailData.value = resp.data || {}
  detailQuery.pageSize = detailData.value.detailPage?.pageSize || detailQuery.pageSize
  detailBatchQuery.pageNum = 1
  detailBatchQuery.pageSize = 10
  detailActiveTab.value = activeTab
  detailOpen.value = true
}

function handleTaskRowCommand(command, row) {
  const handlers = {
    result: openResultCenter,
    alert: openAlertCenter,
    cancel: handleCancel
  }
  handlers[command]?.(row)
}

async function handleOpenPartitionMonitor(row) {
  const resp = await getTaskDetail(row.taskId, { pageNum: 1, pageSize: 1 })
  partitionMonitorData.value = resp.data || {}
  partitionMonitorOpen.value = true
}

async function openTaskByRouteContext() {
  if (!routeTaskContext.taskId) return
  const task = taskList.value.find(item => item.taskId === routeTaskContext.taskId) || { taskId: routeTaskContext.taskId }
  const currentKey = `${routeTaskContext.taskId}:${routeTaskContext.view || 'detail'}`
  if (lastOpenedRouteTaskKey.value === currentKey) return
  if (routeTaskContext.view === 'partition') {
    await handleOpenPartitionMonitor(task)
  } else {
    await handleDetail(task, routeTaskContext.view === 'diagnosis' ? 'overview' : 'overview')
  }
  lastOpenedRouteTaskKey.value = currentKey
}

async function refreshCurrentDetail() {
  if (!detailData.value.task?.taskId) return
  const resp = await getTaskDetail(detailData.value.task.taskId, detailQuery)
  detailData.value = resp.data || {}
  if (detailData.value?.inputBatch?.batch?.batchId && detailBatchQuery.pageNum > 1) {
    await handleDetailBatchPageChange()
  }
  if (partitionMonitorOpen.value && partitionMonitorData.value?.task?.taskId === detailData.value.task?.taskId) {
    const partitionResp = await getTaskDetail(detailData.value.task.taskId, { pageNum: 1, pageSize: 1 })
    partitionMonitorData.value = partitionResp.data || {}
  }
}

async function handleRetry(row) {
  await retryTaskDetail(row.detailId)
  proxy.$modal.msgSuccess('已发起明细重试')
  await refreshCurrentDetail()
  getList()
}

async function handleRetryPartition(row) {
  await retryTaskPartition(row.partitionId)
  proxy.$modal.msgSuccess('已发起分片重试')
  await refreshCurrentDetail()
  if (partitionMonitorData.value?.task?.taskId && !detailData.value?.task?.taskId) {
    const resp = await getTaskDetail(partitionMonitorData.value.task.taskId, { pageNum: 1, pageSize: 1 })
    partitionMonitorData.value = resp.data || {}
  }
  getList()
}

async function handleDetailPageChange() {
  if (!detailData.value.task?.taskId) return
  const resp = await getTaskDetail(detailData.value.task.taskId, detailQuery)
  detailData.value = resp.data || {}
}

async function handleCancel(row) {
  await ElMessageBox.confirm(`确认取消任务 ${row.taskNo} 吗？`, '取消任务', { type: 'warning' })
  await cancelTask(row.taskId)
  proxy.$modal.msgSuccess('任务取消请求已提交')
  getList()
}

function resolveTaskStatus(value) {
  return taskStatusOptions.value.find(item => item.value === value)?.label || value
}

function resolveInputSource(value) {
  if (value === 'INPUT_BATCH') return '导入批次'
  return 'JSON 直传'
}

function resolveDetailTag(value) {
  if (value === 'SUCCESS') return 'success'
  if (value === 'FAILED') return 'danger'
  return 'info'
}

function resolvePartitionTag(value) {
  if (value === 'SUCCESS') return 'success'
  if (value === 'FAILED') return 'danger'
  if (value === 'RUNNING') return 'warning'
  return 'info'
}

function isTaskFailed(row) {
  return ['FAILED', 'PARTIAL_SUCCESS'].includes(row?.taskStatus) || Number(row?.failCount || 0) > 0
}

function resolveProgressStatus(value) {
  if (value === 'SUCCESS') return 'success'
  if (value === 'FAILED' || value === 'PARTIAL_SUCCESS') return 'exception'
  if (value === 'RUNNING' || value === 'INIT') return ''
  return ''
}

function normalizePercent(value) {
  const number = Number(value || 0)
  if (Number.isNaN(number)) return 0
  return Math.max(0, Math.min(100, Math.round(number)))
}

function resolvePartitionProgress(row) {
  const total = Number(row?.totalCount || 0)
  if (!total) return normalizePercent(row?.partitionStatus === 'SUCCESS' ? 100 : 0)
  const processed = Number(row?.processedCount || 0)
  return normalizePercent((processed / total) * 100)
}

function resolvePartitionPersistMode(value) {
  if (value === 'SINGLE_FALLBACK') return '逐条降级'
  return '批量写入'
}

function resolvePartitionPersistModeTag(value) {
  if (value === 'SINGLE_FALLBACK') return 'warning'
  return 'success'
}

function canRetryPartition(row) {
  return ['FAILED', 'PARTIAL_SUCCESS'].includes(row.partitionStatus) || Number(row.failCount || 0) > 0
}

function normalizeBatchExample(inputJson) {
  if (!inputJson) return '[]'
  const trimmed = inputJson.trim()
  if (!trimmed) return '[]'
  if (trimmed.startsWith('[')) return trimmed
  if (trimmed.startsWith('{')) return `[\n  ${trimmed}\n]`
  return '[]'
}

function inspectInlineInputJson(value) {
  const trimmed = (value || '').trim()
  if (!trimmed) {
    return { count: 0, invalid: false }
  }
  try {
    const parsed = JSON.parse(trimmed)
    if (Array.isArray(parsed)) {
      return { count: parsed.length, invalid: false }
    }
    if (parsed && typeof parsed === 'object') {
      return { count: 1, invalid: false }
    }
    return { count: 0, invalid: true }
  } catch (error) {
    return { count: 0, invalid: true }
  }
}

function buildSampleRangeText(pageNum, pageSize, total, currentSize) {
  const safeTotal = Number(total || 0)
  if (!safeTotal) {
    return '当前暂无可展示的样例明细。'
  }
  const safePageNum = Number(pageNum || 1)
  const safePageSize = Number(pageSize || 10)
  const start = (safePageNum - 1) * safePageSize + 1
  const end = Math.min(start + Math.max(Number(currentSize || 0), 1) - 1, safeTotal)
  return `当前展示第 ${start}-${end} 条样例，共 ${safeTotal} 条。`
}

function summarizeJson(value) {
  if (!value) return '-'
  return value.length > 120 ? `${value.slice(0, 120)}...` : value
}

function clearReactiveObject(target) {
  Object.keys(target).forEach(key => delete target[key])
}

watch(() => form.taskType, () => fillExample(), { immediate: true })

watch(
  () => [form.sceneId, form.versionId, form.billMonth],
  ([sceneId, versionId, billMonth]) => {
    syncCostWorkContext({ sceneId, versionId, billMonth })
  },
  { immediate: true }
)

watch(
  () => route.query,
  value => {
    routeTaskContext.taskId = value.taskId ? Number(value.taskId) : undefined
    routeTaskContext.view = value.view || ''
    queryParams.taskId = routeTaskContext.taskId
    if (value.sceneId) {
      queryParams.sceneId = Number(value.sceneId)
      form.sceneId = Number(value.sceneId)
      batchForm.sceneId = Number(value.sceneId)
      batchQuery.sceneId = Number(value.sceneId)
    }
    if (value.versionId) {
      const versionId = Number(value.versionId)
      queryParams.versionId = versionId
      form.versionId = versionId
      batchForm.versionId = versionId
    }
    if (value.billMonth) {
      queryParams.billMonth = value.billMonth
      form.billMonth = value.billMonth
      batchForm.billMonth = value.billMonth
      batchQuery.billMonth = value.billMonth
    }
  },
  { deep: true }
)

watch(
  () => form.sourceBatchNo,
  value => {
    if (!value) {
      clearReactiveObject(selectedBatch)
      return
    }
    if (batchPreview.value?.batch?.batchNo === value) {
      clearReactiveObject(selectedBatch)
      Object.assign(selectedBatch, batchPreview.value.batch)
    }
  }
)

onMounted(async () => {
  await getList()
  await fillExample()
  if (form.inputSourceType === 'INPUT_BATCH' && form.sourceBatchNo) {
    await loadBatchPreviewByBatchNo(form.sourceBatchNo)
  }
  await openTaskByRouteContext()
})

onActivated(async () => {
  await getList()
  await fillExample()
  if (form.inputSourceType === 'INPUT_BATCH' && form.sourceBatchNo) {
    await loadBatchPreviewByBatchNo(form.sourceBatchNo)
  }
  await openTaskByRouteContext()
})
</script>

<style scoped lang="scss">
.run-page {
  display: grid;
  gap: 16px;
  min-height: calc(100dvh - 124px);
  background: linear-gradient(
    180deg,
    color-mix(in srgb, var(--el-bg-color-page) 84%, #f4dfb9 16%) 0%,
    var(--el-bg-color-page) 260px,
    var(--el-bg-color-page) 100%
  );
}
.run-page__hero, .run-page__metric-card, .run-page__panel, .run-page__summary-card, .run-page__batch-card, .run-page__query-shell {
  border: 1px solid var(--el-border-color);
  border-radius: 16px;
  background: var(--el-bg-color-overlay);
}
.run-page__hero {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  padding: 22px 24px;
  background: color-mix(in srgb, var(--el-color-warning-light-8) 28%, var(--el-bg-color-overlay));
}
.run-page__hero-side { display: grid; gap: 10px; justify-items: end; align-content: start; }
.run-page__entry-guide {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
}
.run-page__entry-card {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 14px;
  padding: 16px 18px;
  border: 1px solid var(--el-border-color);
  border-radius: 16px;
  background: var(--el-bg-color-overlay);
}
.run-page__entry-card.is-active {
  background: color-mix(in srgb, var(--el-color-warning-light-9) 44%, var(--el-bg-color-overlay));
}
.run-page__entry-card strong {
  display: block;
  margin-bottom: 6px;
  color: var(--el-text-color-primary);
  font-size: 16px;
}
.run-page__entry-card p {
  margin: 0;
  color: var(--el-text-color-secondary);
  line-height: 1.6;
}
.run-page__eyebrow {
  font-size: 12px;
  color: var(--el-color-warning-dark-2);
  font-weight: 700;
  letter-spacing: .08em;
  text-transform: uppercase;
}
.run-page__title { margin: 8px 0 0; font-size: 28px; }
.run-page__subtitle { margin: 10px 0 0; color: var(--el-text-color-regular); line-height: 1.8; }
.run-page__metrics { display: grid; grid-template-columns: repeat(4, minmax(0, 1fr)); gap: 14px; }
.run-page__metric-card { display: grid; gap: 6px; padding: 14px 16px; }
.run-page__metric-card strong { font-size: 26px; color: var(--el-color-warning-dark-2); }
.run-page__overview-grid { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 16px; }
.run-page__query-shell {
  position: sticky;
  top: 0;
  z-index: 8;
  padding: 16px;
  background: color-mix(in srgb, var(--el-bg-color-overlay) 94%, #fff4df 6%);
  backdrop-filter: blur(12px);
}
.run-page__query-form { margin-top: 14px; }
.run-page__workspace { display: grid; grid-template-columns: minmax(0, 1fr); gap: 16px; }
.run-page__panel { padding: 16px; }
.run-page__section-head { display: flex; justify-content: space-between; align-items: center; gap: 12px; margin-bottom: 16px; }
.run-page__section-head h3 { margin: 0; font-size: 18px; }
.run-page__section-head p { margin: 6px 0 0; color: var(--el-text-color-secondary); font-size: 13px; }
.run-page__panel-badge {
  display: inline-flex;
  align-items: center;
  min-height: 32px;
  padding: 0 12px;
  border: 1px solid var(--el-border-color);
  border-radius: 999px;
  color: var(--el-text-color-regular);
  background: color-mix(in srgb, var(--el-bg-color-overlay) 86%, var(--el-color-warning-light-9) 14%);
  white-space: nowrap;
}
.run-page__distribution-grid { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 16px; }
.run-page__distribution-grid h4 { margin: 0 0 10px; font-size: 15px; color: var(--el-text-color-primary); }
.run-page__distribution-list { display: grid; gap: 10px; }
.run-page__distribution-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 10px 12px;
  border-radius: 12px;
  background: color-mix(in srgb, var(--el-color-warning-light-9) 40%, var(--el-bg-color-page));
}
.run-page__action-row { display: flex; flex-wrap: wrap; gap: 10px; margin-top: 8px; }
.run-page__strategy-alert { margin-bottom: 18px; }
.run-page__precheck-alert { margin-bottom: 18px; }
.run-page__precheck-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 8px 14px;
  margin-top: 4px;
  color: var(--el-text-color-secondary);
  font-size: 12px;
}
.run-page__precheck-list {
  display: grid;
  gap: 8px;
  margin: 10px 0 0;
  padding-left: 18px;
  color: var(--el-text-color-regular);
  line-height: 1.6;
}
.run-page__precheck-list strong {
  margin-right: 8px;
  color: var(--el-text-color-primary);
}
.run-page__template-alert { margin-top: 14px; }
.run-page__template-table { margin-top: 12px; }
.run-page__summary { display: grid; grid-template-columns: repeat(4, minmax(0, 1fr)); gap: 12px; margin: 16px 0; }
.run-page__summary-card { display: grid; gap: 6px; padding: 12px 14px; }
.run-page__summary-card strong { font-size: 24px; color: var(--el-color-warning-dark-2); }
.run-page__batch-select { display: grid; grid-template-columns: minmax(0, 1fr) auto auto auto; gap: 10px; width: 100%; }
.run-page__batch-card { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 8px 16px; margin-top: 12px; padding: 12px 14px; color: var(--el-text-color-regular); }
.run-page__batch-preview { display: grid; gap: 12px; margin-top: 16px; }
.run-page__batch-range-tip { margin: 10px 0 12px; color: var(--el-text-color-secondary); font-size: 13px; }
.run-page__detail-workbench,
.run-page__monitor-workbench {
  display: grid;
  gap: 16px;
}
.run-page__monitor-workbench { margin-bottom: 14px; }
.run-page__detail-hero {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  padding: 16px 18px;
  border: 1px solid var(--el-border-color);
  border-radius: 18px;
  background: color-mix(in srgb, var(--el-bg-color-overlay) 92%, var(--el-color-warning-light-9) 8%);
}
.run-page__detail-hero h3 {
  margin: 6px 0 0;
  font-size: 22px;
}
.run-page__detail-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 12px;
}
.run-page__detail-meta span {
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
.run-page__detail-actions {
  align-content: flex-start;
  justify-content: flex-end;
  margin-top: 0;
}
.run-page__progress-panel {
  display: grid;
  gap: 10px;
  padding: 16px 18px;
  border: 1px solid var(--el-border-color);
  border-radius: 16px;
  background: color-mix(in srgb, var(--el-bg-color-overlay) 92%, var(--el-color-primary-light-9) 8%);
}
.run-page__progress-title,
.run-page__progress-breakdown {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}
.run-page__progress-title span,
.run-page__progress-breakdown {
  color: var(--el-text-color-secondary);
  font-size: 13px;
}
.run-page__progress-title strong {
  color: var(--el-text-color-primary);
  font-size: 24px;
}
.run-page__detail-tabs {
  padding: 14px 16px 16px;
  border: 1px solid var(--el-border-color);
  border-radius: 18px;
  background: var(--el-bg-color-overlay);
}
.run-page__summary--detail {
  margin: 0;
}
.run-page__detail-section { margin-top: 18px; }
.run-page__table { margin-top: 6px; }
.run-page.is-compact-mode {
  gap: 12px;
  background: var(--el-bg-color-page);
}
.run-page.is-compact-mode .run-page__query-shell,
.run-page.is-compact-mode .run-page__panel {
  border-radius: 14px;
  box-shadow: none;
}
.run-page.is-compact-mode .run-page__query-shell .run-page__section-head > div,
.run-page.is-compact-mode .run-page__section-head p {
  display: none;
}
.run-page.is-compact-mode .run-page__query-form {
  margin-top: 0;
}
@media (max-width: 1200px) {
  .run-page__entry-guide, .run-page__metrics, .run-page__overview-grid, .run-page__workspace, .run-page__summary, .run-page__batch-card, .run-page__distribution-grid { grid-template-columns: 1fr; }
  .run-page__batch-select { grid-template-columns: 1fr; }
  .run-page__hero-side { justify-items: stretch; }
  .run-page__detail-hero { flex-direction: column; }
  .run-page__detail-actions { justify-content: flex-start; }
}
</style>

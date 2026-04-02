<template>
  <div class="app-container run-page">
    <section class="run-page__hero">
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

    <section class="run-page__metrics">
      <div v-for="item in metricItems" :key="item.label" class="run-page__metric-card">
        <span>{{ item.label }}</span>
        <strong>{{ item.value }}</strong>
        <small>{{ item.desc }}</small>
      </div>
    </section>

    <section class="run-page__overview-grid">
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
    </section>

    <el-form ref="queryRef" :model="queryParams" :inline="true" label-width="84px" v-show="showSearch">
      <el-form-item label="所属场景" prop="sceneId">
        <el-select v-model="queryParams.sceneId" clearable filterable style="width: 220px" @change="handleQuerySceneChange">
          <el-option v-for="item in sceneOptions" :key="item.sceneId" :label="`${item.sceneCode} / ${item.sceneName}`" :value="item.sceneId" />
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
        <el-input v-model="queryParams.billMonth" clearable placeholder="yyyy-MM" style="width: 160px" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <section class="run-page__workspace">
      <div class="run-page__panel">
        <div class="run-page__section-head">
          <div>
            <h3>提交任务</h3>
            <p>单笔和批量任务继续支持 JSON 直传，也可以先创建或选择导入批次，再引用批次号发起正式核算。</p>
          </div>
          <right-toolbar v-model:showSearch="showSearch" @queryTable="getList" />
        </div>

        <el-form :model="form" label-width="92px">
          <el-form-item label="运行场景" required>
            <el-select v-model="form.sceneId" filterable style="width: 100%" @change="handleFormSceneChange">
              <el-option v-for="item in sceneOptions" :key="item.sceneId" :label="`${item.sceneCode} / ${item.sceneName}`" :value="item.sceneId" />
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
            <el-input v-model="form.billMonth" placeholder="yyyy-MM" />
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
          <el-form-item label="备注">
            <el-input v-model="form.remark" type="textarea" :rows="2" maxlength="500" show-word-limit />
          </el-form-item>

          <template v-if="form.inputSourceType === 'INLINE_JSON'">
            <el-form-item label="输入 JSON" required>
              <el-input v-model="form.inputJson" type="textarea" :rows="14" maxlength="20000" show-word-limit />
            </el-form-item>
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
          </template>
        </el-form>

        <div class="run-page__action-row">
          <el-button type="primary" icon="Promotion" @click="handleSubmit" v-hasPermi="['cost:task:execute']">提交任务</el-button>
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
        </div>

        <el-table v-loading="loading" :data="taskList">
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
          <el-table-column label="进度" width="120" align="center">
            <template #default="scope">{{ scope.row.progressPercent }}%</template>
          </el-table-column>
          <el-table-column label="成功/失败" width="120" align="center">
            <template #default="scope">{{ scope.row.successCount }}/{{ scope.row.failCount }}</template>
          </el-table-column>
          <el-table-column label="操作" width="310" fixed="right" align="center">
            <template #default="scope">
              <el-button link type="primary" icon="View" @click="handleDetail(scope.row)">详情</el-button>
              <el-button link type="success" icon="Histogram" @click="handleOpenPartitionMonitor(scope.row)">分片监控</el-button>
              <el-button link type="success" icon="List" @click="openResultCenter(scope.row)">查看结果</el-button>
              <el-button link type="danger" icon="Warning" @click="openAlertCenter(scope.row)">查看告警</el-button>
              <el-button link type="warning" icon="CircleClose" @click="handleCancel(scope.row)" v-hasPermi="['cost:task:cancel']">取消</el-button>
            </template>
          </el-table-column>
        </el-table>

        <pagination v-show="total > 0" :total="total" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" @pagination="getList" />
      </div>
    </section>

    <el-drawer v-model="detailOpen" title="任务详情" size="1180px" append-to-body>
      <el-descriptions v-if="detailData.task" :column="2" border>
        <el-descriptions-item label="任务编号">{{ detailData.task.taskNo }}</el-descriptions-item>
        <el-descriptions-item label="状态">{{ resolveTaskStatus(detailData.task.taskStatus) }}</el-descriptions-item>
        <el-descriptions-item label="场景">{{ detailData.task.sceneName }}</el-descriptions-item>
        <el-descriptions-item label="版本">{{ detailData.task.versionNo }}</el-descriptions-item>
        <el-descriptions-item label="账期">{{ detailData.task.billMonth }}</el-descriptions-item>
        <el-descriptions-item label="执行节点">{{ detailData.task.executeNode || '-' }}</el-descriptions-item>
        <el-descriptions-item label="输入来源">{{ resolveInputSource(detailData.task.inputSourceType) }}</el-descriptions-item>
        <el-descriptions-item label="来源批次">{{ detailData.task.sourceBatchNo || '-' }}</el-descriptions-item>
      </el-descriptions>

      <div class="run-page__summary">
        <div class="run-page__summary-card"><span>输入总量</span><strong>{{ detailData.summary?.sourceCount || 0 }}</strong></div>
        <div class="run-page__summary-card"><span>成功数量</span><strong>{{ detailData.summary?.successCount || 0 }}</strong></div>
        <div class="run-page__summary-card"><span>失败数量</span><strong>{{ detailData.summary?.failCount || 0 }}</strong></div>
        <div class="run-page__summary-card"><span>分片数量</span><strong>{{ detailData.summary?.partitionCount || 0 }}</strong></div>
      </div>

      <div class="run-page__action-row">
        <el-button icon="Warning" @click="openAlertCenter(detailData.task)">查看当前任务告警</el-button>
        <el-button icon="List" @click="openResultCenter(detailData.task)">查看当前任务结果</el-button>
      </div>

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

      <div v-if="detailData.inputBatch?.batch" class="run-page__detail-section">
        <div class="run-page__section-head">
          <div>
            <h3>关联批次</h3>
            <p>查看任务关联的导入批次摘要与样例明细。</p>
          </div>
        </div>
        <div class="run-page__batch-card">
          <span>批次号：{{ detailData.inputBatch.batch.batchNo }}</span>
          <span>场景：{{ detailData.inputBatch.batch.sceneName || '-' }}</span>
          <span>版本：{{ detailData.inputBatch.batch.versionNo || '-' }}</span>
          <span>总量：{{ detailData.inputBatch.batch.totalCount || 0 }}</span>
        </div>
        <el-table :data="detailData.inputBatch.items || []" size="small" border>
          <el-table-column label="序号" prop="itemNo" width="80" align="center" />
          <el-table-column label="业务单号" prop="bizNo" min-width="180" />
          <el-table-column label="状态" prop="itemStatus" width="120" align="center" />
          <el-table-column label="输入摘要" min-width="320">
            <template #default="scope">{{ summarizeJson(scope.row.inputJson) }}</template>
          </el-table-column>
        </el-table>
      </div>

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
          <el-table-column label="状态" width="120" align="center">
            <template #default="scope">
              <el-tag :type="resolvePartitionTag(scope.row.partitionStatus)">{{ resolveTaskStatus(scope.row.partitionStatus) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="总量" prop="totalCount" width="90" align="center" />
          <el-table-column label="成功/失败" width="120" align="center">
            <template #default="scope">{{ scope.row.successCount || 0 }}/{{ scope.row.failCount || 0 }}</template>
          </el-table-column>
          <el-table-column label="耗时(ms)" prop="durationMs" width="110" align="center" />
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

      <div class="run-page__detail-section">
        <div class="run-page__section-head">
          <div>
            <h3>任务明细</h3>
            <p>保留明细级失败重试，兼容当前台账查看口径。</p>
          </div>
        </div>
        <el-table :data="detailData.details || []" size="small">
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
      </div>
    </el-drawer>

    <el-dialog v-model="batchDialogOpen" title="创建导入批次" width="920px" append-to-body>
      <el-form :model="batchForm" label-width="92px">
        <el-form-item label="运行场景" required>
          <el-select v-model="batchForm.sceneId" filterable style="width: 100%">
            <el-option v-for="item in sceneOptions" :key="item.sceneId" :label="`${item.sceneCode} / ${item.sceneName}`" :value="item.sceneId" />
          </el-select>
        </el-form-item>
        <el-form-item label="执行版本">
          <el-select v-model="batchForm.versionId" clearable filterable style="width: 100%">
            <el-option v-for="item in formVersionOptions" :key="item.versionId" :label="`${item.versionNo} / ${item.versionStatus}`" :value="item.versionId" />
          </el-select>
        </el-form-item>
        <el-form-item label="账期" required>
          <el-input v-model="batchForm.billMonth" placeholder="yyyy-MM" />
        </el-form-item>
        <el-form-item label="批次备注">
          <el-input v-model="batchForm.remark" type="textarea" :rows="2" maxlength="500" show-word-limit />
        </el-form-item>
        <el-form-item label="批量 JSON" required>
          <el-input v-model="batchForm.inputJson" type="textarea" :rows="16" maxlength="60000" show-word-limit placeholder="请输入 JSON 数组，例如 [{...}, {...}]" />
        </el-form-item>
      </el-form>

      <el-alert v-if="batchPreview.batch?.batchNo" class="run-page__template-alert" type="success" :closable="false" :title="`批次 ${batchPreview.batch.batchNo} 创建成功，共 ${batchPreview.batch.totalCount || 0} 条。`" />
      <el-table v-if="batchPreview.items?.length" :data="batchPreview.items.slice(0, 10)" size="small" border>
        <el-table-column label="序号" prop="itemNo" width="80" align="center" />
        <el-table-column label="业务单号" prop="bizNo" min-width="180" />
        <el-table-column label="状态" prop="itemStatus" width="120" align="center" />
        <el-table-column label="输入摘要" min-width="340">
          <template #default="scope">{{ summarizeJson(scope.row.inputJson) }}</template>
        </el-table-column>
      </el-table>

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
            <el-option v-for="item in sceneOptions" :key="item.sceneId" :label="`${item.sceneCode} / ${item.sceneName}`" :value="item.sceneId" />
          </el-select>
        </el-form-item>
        <el-form-item label="账期">
          <el-input v-model="batchQuery.billMonth" placeholder="yyyy-MM" style="width: 140px" />
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
      <div class="run-page__summary">
        <div class="run-page__summary-card"><span>分片总数</span><strong>{{ partitionMonitorData.summary?.partitionCount || 0 }}</strong></div>
        <div class="run-page__summary-card"><span>输入总量</span><strong>{{ partitionMonitorData.summary?.sourceCount || 0 }}</strong></div>
        <div class="run-page__summary-card"><span>成功数量</span><strong>{{ partitionMonitorData.summary?.successCount || 0 }}</strong></div>
        <div class="run-page__summary-card"><span>失败数量</span><strong>{{ partitionMonitorData.summary?.failCount || 0 }}</strong></div>
      </div>
      <el-table :data="partitionMonitorData.partitions || []" size="small" border>
        <el-table-column label="分片号" prop="partitionNo" width="90" align="center" />
        <el-table-column label="范围" min-width="150">
          <template #default="scope">{{ scope.row.startItemNo }} - {{ scope.row.endItemNo }}</template>
        </el-table-column>
        <el-table-column label="状态" width="120" align="center">
          <template #default="scope">
            <el-tag :type="resolvePartitionTag(scope.row.partitionStatus)">{{ resolveTaskStatus(scope.row.partitionStatus) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="总量" prop="totalCount" width="90" align="center" />
        <el-table-column label="成功/失败" width="120" align="center">
          <template #default="scope">{{ scope.row.successCount || 0 }}/{{ scope.row.failCount || 0 }}</template>
        </el-table-column>
        <el-table-column label="耗时(ms)" prop="durationMs" width="110" align="center" />
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
  retryTaskDetail,
  retryTaskPartition,
  submitTask
} from '@/api/cost/run'
import { optionselectScene } from '@/api/cost/scene'
import { resolveWorkingCostSceneId } from '@/utils/costSceneContext'
import { getRemoteDictOptionMap } from '@/utils/dictRemote'

const route = useRoute()
const router = useRouter()
const { proxy } = getCurrentInstance()

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
const batchDialogOpen = ref(false)
const batchPickerOpen = ref(false)
const batchPreview = ref({})
const batchListLoading = ref(false)
const batchList = ref([])
const batchTotal = ref(0)
const partitionMonitorOpen = ref(false)
const partitionMonitorData = ref({})
const selectedBatch = reactive({})
const stats = reactive({ taskCount: 0, runningCount: 0, successCount: 0, failedCount: 0 })
const overview = reactive({
  recentTaskTrend: [],
  recentPartitionTrend: [],
  topRiskTasks: [],
  taskStatusDistribution: [],
  inputSourceDistribution: []
})
const routeTaskContext = reactive({
  taskId: route.query.taskId ? Number(route.query.taskId) : undefined,
  view: route.query.view || ''
})
const lastOpenedRouteTaskKey = ref('')

const queryParams = reactive({
  pageNum: 1,
  pageSize: 10,
  taskId: route.query.taskId ? Number(route.query.taskId) : undefined,
  sceneId: route.query.sceneId ? Number(route.query.sceneId) : undefined,
  versionId: undefined,
  taskType: undefined,
  taskStatus: undefined,
  billMonth: ''
})

const form = reactive({
  sceneId: route.query.sceneId ? Number(route.query.sceneId) : undefined,
  versionId: undefined,
  taskType: 'FORMAL_SINGLE',
  billMonth: resolveCurrentBillMonth(),
  requestNo: '',
  inputSourceType: route.query.inputSourceType || 'INLINE_JSON',
  sourceBatchNo: route.query.sourceBatchNo || '',
  inputJson: '',
  remark: ''
})

const batchForm = reactive({
  sceneId: undefined,
  versionId: undefined,
  billMonth: resolveCurrentBillMonth(),
  inputJson: '',
  remark: ''
})

const batchQuery = reactive({
  pageNum: 1,
  pageSize: 8,
  sceneId: undefined,
  billMonth: '',
  batchNo: '',
  batchStatus: ''
})

const metricItems = computed(() => [
  { label: '任务总数', value: stats.taskCount, desc: '当前筛选范围内的正式核算任务总数' },
  { label: '执行中', value: stats.runningCount, desc: '仍在异步执行中的任务数' },
  { label: '成功任务', value: stats.successCount, desc: '全部执行成功的任务数' },
  { label: '失败/部分成功', value: stats.failedCount, desc: '需要关注或重试的任务数' }
])

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
      inputSourceDistribution: overviewResp?.data?.inputSourceDistribution || []
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
  versionOptions.value = []
  getList()
}

async function handleQuerySceneChange(sceneId) {
  queryParams.versionId = undefined
  queryParams.sceneId = sceneId
  batchQuery.sceneId = sceneId
  await loadVersionOptions(sceneId, versionOptions)
}

async function handleFormSceneChange(sceneId) {
  form.versionId = undefined
  batchForm.versionId = undefined
  form.sceneId = sceneId
  batchForm.sceneId = sceneId
  batchQuery.sceneId = sceneId
  queryParams.sceneId = sceneId
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
  batchPreview.value = {}
  batchDialogOpen.value = true
}

async function handleCreateBatch() {
  if (!batchForm.sceneId || !batchForm.billMonth || !batchForm.inputJson) {
    proxy.$modal.msgWarning('请填写导入批次的场景、账期和批量 JSON 数组')
    return
  }
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
  batchQuery.billMonth = ''
  batchQuery.batchStatus = ''
  batchQuery.sceneId = form.sceneId || batchQuery.sceneId
  loadBatchList()
}

async function handleSelectBatch(row) {
  await loadBatchPreviewById(row.batchId)
  form.inputSourceType = 'INPUT_BATCH'
  form.sourceBatchNo = row.batchNo
  form.sceneId = row.sceneId || form.sceneId
  form.versionId = row.versionId || form.versionId
  form.billMonth = row.billMonth || form.billMonth
  batchPickerOpen.value = false
}

async function previewBatchRow(row) {
  await loadBatchPreviewById(row.batchId)
}

async function loadBatchPreviewByBatchNo(batchNo) {
  if (!batchNo) return
  if (detailData.value?.task?.sourceBatchNo === batchNo && detailData.value?.inputBatch?.batch?.batchId) {
    await loadBatchPreviewById(detailData.value.inputBatch.batch.batchId)
    return
  }
  if (batchPreview.value?.batch?.batchNo === batchNo) {
    applyBatchPreview(batchPreview.value)
    return
  }
  const matched = batchList.value.find(item => item.batchNo === batchNo)
  if (matched?.batchId) {
    await loadBatchPreviewById(matched.batchId)
    return
  }
  batchPickerOpen.value = true
  batchQuery.batchNo = batchNo
  await loadBatchList()
  const current = batchList.value.find(item => item.batchNo === batchNo)
  if (current?.batchId) {
    await loadBatchPreviewById(current.batchId)
    return
  }
  proxy.$modal.msgWarning('未找到对应批次，请先通过历史批次列表查询')
}

async function loadBatchPreviewById(batchId) {
  const resp = await getTaskInputBatchDetail(batchId)
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
  const payload = { ...form }
  if (payload.inputSourceType === 'INPUT_BATCH') {
    payload.inputJson = ''
  }
  const resp = await submitTask(payload)
  proxy.$modal.msgSuccess('任务已提交')
  detailData.value = resp.data || {}
  detailOpen.value = true
  getList()
}

function openBatchLedger() {
  router.push({ path: '/cost/taskBatch', query: { sceneId: form.sceneId } })
}

function openAlertCenter(task) {
  if (!task?.taskId) {
    router.push('/cost/alert')
    return
  }
  router.push({
    path: '/cost/alert',
    query: {
      sceneId: task.sceneId,
      billMonth: task.billMonth,
      taskId: task.taskId,
      alarmStatus: 'OPEN'
    }
  })
}

function openTaskCenterById(taskId, billMonth, view = 'detail') {
  router.push({
    path: '/cost/task',
    query: {
      taskId,
      billMonth,
      sceneId: queryParams.sceneId,
      view
    }
  })
}

function openResultCenter(task) {
  if (!task?.taskId) {
    router.push('/cost/result')
    return
  }
  router.push({
    path: '/cost/result',
    query: {
      sceneId: task.sceneId,
      billMonth: task.billMonth,
      taskId: task.taskId,
      view: 'list'
    }
  })
}

async function handleDetail(row) {
  const resp = await getTaskDetail(row.taskId)
  detailData.value = resp.data || {}
  detailOpen.value = true
}

async function handleOpenPartitionMonitor(row) {
  const resp = await getTaskDetail(row.taskId)
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
    await handleDetail(task)
  }
  lastOpenedRouteTaskKey.value = currentKey
}

async function refreshCurrentDetail() {
  if (!detailData.value.task?.taskId) return
  const resp = await getTaskDetail(detailData.value.task.taskId)
  detailData.value = resp.data || {}
  if (partitionMonitorOpen.value && partitionMonitorData.value?.task?.taskId === detailData.value.task?.taskId) {
    partitionMonitorData.value = resp.data || {}
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
    const resp = await getTaskDetail(partitionMonitorData.value.task.taskId)
    partitionMonitorData.value = resp.data || {}
  }
  getList()
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

function canRetryPartition(row) {
  return ['FAILED', 'PARTIAL_SUCCESS'].includes(row.partitionStatus) || Number(row.failCount || 0) > 0
}

function resolveCurrentBillMonth() {
  const current = new Date()
  const month = String(current.getMonth() + 1).padStart(2, '0')
  return `${current.getFullYear()}-${month}`
}

function normalizeBatchExample(inputJson) {
  if (!inputJson) return '[]'
  const trimmed = inputJson.trim()
  if (!trimmed) return '[]'
  if (trimmed.startsWith('[')) return trimmed
  if (trimmed.startsWith('{')) return `[\n  ${trimmed}\n]`
  return '[]'
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
  () => route.query,
  value => {
    routeTaskContext.taskId = value.taskId ? Number(value.taskId) : undefined
    routeTaskContext.view = value.view || ''
    queryParams.taskId = routeTaskContext.taskId
    if (value.sceneId) {
      queryParams.sceneId = Number(value.sceneId)
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
.run-page { display: grid; gap: 16px; }
.run-page__hero, .run-page__metric-card, .run-page__panel, .run-page__summary-card, .run-page__batch-card {
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
.run-page__workspace { display: grid; grid-template-columns: 500px minmax(0, 1fr); gap: 16px; }
.run-page__panel { padding: 16px; }
.run-page__section-head { display: flex; justify-content: space-between; align-items: center; gap: 12px; margin-bottom: 16px; }
.run-page__section-head h3 { margin: 0; font-size: 18px; }
.run-page__section-head p { margin: 6px 0 0; color: var(--el-text-color-secondary); font-size: 13px; }
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
.run-page__template-alert { margin-top: 14px; }
.run-page__template-table { margin-top: 12px; }
.run-page__summary { display: grid; grid-template-columns: repeat(4, minmax(0, 1fr)); gap: 12px; margin: 16px 0; }
.run-page__summary-card { display: grid; gap: 6px; padding: 12px 14px; }
.run-page__summary-card strong { font-size: 24px; color: var(--el-color-warning-dark-2); }
.run-page__batch-select { display: grid; grid-template-columns: minmax(0, 1fr) auto auto auto; gap: 10px; width: 100%; }
.run-page__batch-card { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 8px 16px; margin-top: 12px; padding: 12px 14px; color: var(--el-text-color-regular); }
.run-page__detail-section { margin-top: 18px; }
@media (max-width: 1200px) {
  .run-page__metrics, .run-page__overview-grid, .run-page__workspace, .run-page__summary, .run-page__batch-card, .run-page__distribution-grid { grid-template-columns: 1fr; }
  .run-page__batch-select { grid-template-columns: 1fr; }
  .run-page__hero-side { justify-items: stretch; }
}
</style>

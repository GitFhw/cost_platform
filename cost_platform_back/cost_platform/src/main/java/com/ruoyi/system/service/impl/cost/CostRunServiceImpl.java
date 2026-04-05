package com.ruoyi.system.service.impl.cost;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruoyi.common.core.redis.RedisCache;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.system.domain.cost.CostAlarmRecord;
import com.ruoyi.system.domain.cost.CostBillPeriod;
import com.ruoyi.system.domain.cost.CostCalcInputBatch;
import com.ruoyi.system.domain.cost.CostCalcInputBatchItem;
import com.ruoyi.system.domain.cost.CostCalcTask;
import com.ruoyi.system.domain.cost.CostCalcTaskDetail;
import com.ruoyi.system.domain.cost.CostCalcTaskPartition;
import com.ruoyi.system.domain.cost.CostFeeItem;
import com.ruoyi.system.domain.cost.CostFormula;
import com.ruoyi.system.domain.cost.CostPublishSnapshot;
import com.ruoyi.system.domain.cost.CostPublishVersion;
import com.ruoyi.system.domain.cost.CostRecalcOrder;
import com.ruoyi.system.domain.cost.CostResultLedger;
import com.ruoyi.system.domain.cost.CostResultTrace;
import com.ruoyi.system.domain.cost.CostRule;
import com.ruoyi.system.domain.cost.CostScene;
import com.ruoyi.system.domain.cost.CostSimulationRecord;
import com.ruoyi.system.domain.cost.CostVariable;
import com.ruoyi.system.domain.cost.bo.CostCalcTaskSubmitBo;
import com.ruoyi.system.domain.cost.bo.CostSimulationExecuteBo;
import com.ruoyi.system.domain.cost.bo.CostCalcInputBatchCreateBo;
import com.ruoyi.system.domain.cost.bo.CostFeeCalculateBo;
import com.ruoyi.system.mapper.cost.CostBillPeriodMapper;
import com.ruoyi.system.mapper.cost.CostCalcInputBatchItemMapper;
import com.ruoyi.system.mapper.cost.CostCalcInputBatchMapper;
import com.ruoyi.system.mapper.cost.CostCalcTaskDetailMapper;
import com.ruoyi.system.mapper.cost.CostCalcTaskMapper;
import com.ruoyi.system.mapper.cost.CostCalcTaskPartitionMapper;
import com.ruoyi.system.mapper.cost.CostFeeMapper;
import com.ruoyi.system.mapper.cost.CostFormulaMapper;
import com.ruoyi.system.mapper.cost.CostPublishVersionMapper;
import com.ruoyi.system.mapper.cost.CostRecalcOrderMapper;
import com.ruoyi.system.mapper.cost.CostResultLedgerMapper;
import com.ruoyi.system.mapper.cost.CostResultTraceMapper;
import com.ruoyi.system.mapper.cost.CostRuleMapper;
import com.ruoyi.system.mapper.cost.CostSceneMapper;
import com.ruoyi.system.mapper.cost.CostSimulationRecordMapper;
import com.ruoyi.system.mapper.cost.CostVariableMapper;
import com.ruoyi.system.service.cost.ICostAlarmService;
import com.ruoyi.system.service.cost.ICostAuditService;
import com.ruoyi.system.service.cost.ICostExpressionService;
import com.ruoyi.system.service.cost.ICostRunService;
import org.apache.commons.lang3.math.NumberUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 线程五运行链服务实现
 *
 * <p>该服务围绕“发布快照 -> 试算/正式任务 -> 结果台账 -> 追溯解释”的主链路展开：
 * 1. 运行时只装载场景发布快照，不直接读取草稿配置；
 * 2. 试算与正式核算共用同一套匹配与定价内核，但落库目标不同；
 * 3. 正式任务拆成任务头、任务明细、结果台账和追溯记录，便于后续线程六继续增强幂等、并发和 Redis 锁。</p>
 *
 * @author codex
 */
@Service
public class CostRunServiceImpl implements ICostRunService
{
    private static final String STATUS_ENABLED = "0";
    private static final String SIMULATION_STATUS_SUCCESS = "SUCCESS";
    private static final String SIMULATION_STATUS_FAILED = "FAILED";
    private static final String TASK_TYPE_SIMULATION_BATCH = "SIMULATION_BATCH";
    private static final String TASK_TYPE_FORMAL_SINGLE = "FORMAL_SINGLE";
    private static final String TASK_TYPE_FORMAL_BATCH = "FORMAL_BATCH";
    private static final String INPUT_SOURCE_INLINE_JSON = "INLINE_JSON";
    private static final String INPUT_SOURCE_BATCH = "INPUT_BATCH";
    private static final String INPUT_BATCH_STATUS_READY = "READY";
    private static final String INPUT_BATCH_STATUS_SUBMITTED = "SUBMITTED";
    private static final String INPUT_BATCH_STATUS_CONSUMED = "CONSUMED";
    private static final String TASK_STATUS_INIT = "INIT";
    private static final String TASK_STATUS_RUNNING = "RUNNING";
    private static final String TASK_STATUS_SUCCESS = "SUCCESS";
    private static final String TASK_STATUS_PART_SUCCESS = "PART_SUCCESS";
    private static final String TASK_STATUS_FAILED = "FAILED";
    private static final String TASK_STATUS_CANCELLED = "CANCELLED";
    private static final String DETAIL_STATUS_INIT = "INIT";
    private static final String DETAIL_STATUS_SUCCESS = "SUCCESS";
    private static final String DETAIL_STATUS_FAILED = "FAILED";
    private static final String RESULT_STATUS_SUCCESS = "SUCCESS";
    private static final String PERIOD_STATUS_NOT_STARTED = "NOT_STARTED";
    private static final String PERIOD_STATUS_IN_PROGRESS = "IN_PROGRESS";
    private static final String PERIOD_STATUS_CLOSED = "CLOSED";
    private static final String PERIOD_STATUS_SEALED = "SEALED";
    private static final String RECALC_STATUS_RUNNING = "RUNNING";
    private static final String RECALC_STATUS_SUCCESS = "SUCCESS";
    private static final String RECALC_STATUS_FAILED = "FAILED";
    private static final String RULE_TYPE_FIXED_RATE = "FIXED_RATE";
    private static final String RULE_TYPE_FIXED_AMOUNT = "FIXED_AMOUNT";
    private static final String RULE_TYPE_FORMULA = "FORMULA";
    private static final String RULE_TYPE_TIER_RATE = "TIER_RATE";
    private static final String PRICING_MODE_GROUPED = "GROUPED";
    private static final String SNAPSHOT_SOURCE_PUBLISHED = "PUBLISHED";
    private static final String SNAPSHOT_SOURCE_DRAFT = "DRAFT";
    private static final String SOURCE_TYPE_FORMULA = "FORMULA";
    private static final String DATA_TYPE_NUMBER = "NUMBER";
    private static final String DATA_TYPE_BOOLEAN = "BOOLEAN";
    private static final String DATA_TYPE_JSON = "JSON";
    private static final String OP_EQ = "EQ";
    private static final String OP_NE = "NE";
    private static final String OP_GT = "GT";
    private static final String OP_GE = "GE";
    private static final String OP_LT = "LT";
    private static final String OP_LE = "LE";
    private static final String OP_IN = "IN";
    private static final String OP_NOT_IN = "NOT_IN";
    private static final String OP_BETWEEN = "BETWEEN";
    private static final String OP_EXPR = "EXPR";
    private static final String INTERVAL_LCRO = "LEFT_CLOSED_RIGHT_OPEN";
    private static final String INTERVAL_LORC = "LEFT_OPEN_RIGHT_CLOSED";
    private static final String RUNTIME_CACHE_PREFIX = "cost:runtime:snapshot:";
    private static final DateTimeFormatter NO_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final DecimalFormat PARTITION_FORMAT = new DecimalFormat("000");
    private static final Pattern EXPRESSION_REFERENCE_PATTERN =
            Pattern.compile("\\bV\\.([A-Za-z_][A-Za-z0-9_]*)\\b|\\b([A-Za-z_][A-Za-z0-9_]*)\\b");
    private static final Pattern FEE_REFERENCE_PATTERN =
            Pattern.compile("F\\[['\"]([A-Za-z0-9_\\-]+)['\"]\\]");
    private static final int DEFAULT_TASK_PARTITION_SIZE = 500;
    private static final int DEFAULT_TASK_PARALLELISM = 8;
    private static final String DRAFT_VERSION_LABEL = "草稿配置";

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private CostSimulationRecordMapper simulationRecordMapper;

    @Autowired
    private CostCalcTaskMapper calcTaskMapper;

    @Autowired
    private CostCalcInputBatchMapper calcInputBatchMapper;

    @Autowired
    private CostCalcInputBatchItemMapper calcInputBatchItemMapper;

    @Autowired
    private CostCalcTaskDetailMapper calcTaskDetailMapper;

    @Autowired
    private CostCalcTaskPartitionMapper calcTaskPartitionMapper;

    @Autowired
    private CostResultLedgerMapper resultLedgerMapper;

    @Autowired
    private CostResultTraceMapper resultTraceMapper;

    @Autowired
    private CostSceneMapper sceneMapper;

    @Autowired
    private CostPublishVersionMapper publishVersionMapper;

    @Autowired
    private CostBillPeriodMapper billPeriodMapper;

    @Autowired
    private CostRecalcOrderMapper recalcOrderMapper;

    @Autowired
    private CostFeeMapper feeMapper;

    @Autowired
    private CostVariableMapper variableMapper;

    @Autowired
    private CostFormulaMapper formulaMapper;

    @Autowired
    private CostRuleMapper ruleMapper;

    @Autowired
    private ThreadPoolTaskExecutor threadPoolTaskExecutor;

    @Autowired
    private RedisCache redisCache;

    @Autowired
    private ICostAuditService auditService;

    @Autowired
    private ICostAlarmService alarmService;

    @Autowired
    private ICostExpressionService expressionService;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Override
    public Map<String, Object> selectSimulationStats(Long sceneId)
    {
        List<CostSimulationRecord> records = simulationRecordMapper.selectList(Wrappers.<CostSimulationRecord>lambdaQuery()
                .eq(sceneId != null, CostSimulationRecord::getSceneId, sceneId));
        LinkedHashMap<String, Object> stats = new LinkedHashMap<>();
        stats.put("simulationCount", records.size());
        stats.put("successCount", records.stream().filter(item -> SIMULATION_STATUS_SUCCESS.equals(item.getStatus())).count());
        stats.put("failedCount", records.stream().filter(item -> SIMULATION_STATUS_FAILED.equals(item.getStatus())).count());
        stats.put("sceneCount", records.stream().map(CostSimulationRecord::getSceneId).filter(Objects::nonNull).distinct().count());
        return stats;
    }

    @Override
    public List<CostSimulationRecord> selectSimulationList(CostSimulationRecord query)
    {
        List<CostSimulationRecord> records = simulationRecordMapper.selectList(Wrappers.<CostSimulationRecord>lambdaQuery()
                .eq(query.getSceneId() != null, CostSimulationRecord::getSceneId, query.getSceneId())
                .eq(query.getVersionId() != null, CostSimulationRecord::getVersionId, query.getVersionId())
                .eq(StringUtils.isNotEmpty(query.getStatus()), CostSimulationRecord::getStatus, query.getStatus())
                .orderByDesc(CostSimulationRecord::getSimulationId));
        enrichSimulationRecords(records);
        return records;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> executeSimulation(CostSimulationExecuteBo bo)
    {
        RuntimeSnapshot snapshot = loadRuntimeSnapshot(bo.getSceneId(), bo.getVersionId(), false, true);
        Map<String, Object> input = parseObjectJson(bo.getInputJson(), "试算输入必须是 JSON 对象");
        CostSimulationRecord record = executeAndPersistSimulation(snapshot, input, "");
        return selectSimulationDetail(record.getSimulationId());
    }

    @Override
    public Map<String, Object> executeSimulationBatch(CostSimulationExecuteBo bo)
    {
        RuntimeSnapshot snapshot = loadRuntimeSnapshot(bo.getSceneId(), bo.getVersionId(), false, true);
        List<Map<String, Object>> inputs = parseArrayJson(bo.getInputJson(), "批量试算输入必须是 JSON 数组");
        if (inputs.isEmpty())
        {
            throw new ServiceException("批量试算输入不能为空数组");
        }
        validateDuplicateBizNo(inputs);
        List<CostSimulationRecord> records = new ArrayList<>();
        for (int i = 0; i < inputs.size(); i++)
        {
            Map<String, Object> input = inputs.get(i);
            String bizNo = resolveBizNo(input, i + 1);
            records.add(executeAndPersistSimulation(snapshot, input, bizNo));
        }

        enrichSimulationRecords(records);
        LinkedHashMap<String, Object> result = new LinkedHashMap<>();
        result.put("sceneId", snapshot.sceneId);
        result.put("sceneCode", snapshot.sceneCode);
        result.put("sceneName", snapshot.sceneName);
        result.put("versionId", snapshot.versionId);
        result.put("versionNo", snapshot.versionNo);
        result.put("snapshotSource", snapshot.snapshotSource);
        result.put("totalCount", records.size());
        result.put("successCount", records.stream().filter(item -> SIMULATION_STATUS_SUCCESS.equals(item.getStatus())).count());
        result.put("failedCount", records.stream().filter(item -> SIMULATION_STATUS_FAILED.equals(item.getStatus())).count());
        result.put("records", records.stream().map(this::buildSimulationBatchItem).collect(Collectors.toList()));
        return result;
    }

    private CostSimulationRecord executeAndPersistSimulation(RuntimeSnapshot snapshot, Map<String, Object> input, String bizNo)
    {
        Date now = DateUtils.getNowDate();
        String operator = resolveOperator();
        CostSimulationRecord record = new CostSimulationRecord();
        record.setSceneId(snapshot.sceneId);
        record.setVersionId(snapshot.versionId);
        record.setVersionNo(snapshot.versionNo);
        record.setSimulationNo(buildRunNo("SIM"));
        record.setInputJson(writeJson(input));
        record.setCreateBy(operator);
        record.setCreateTime(now);
        try
        {
            ExecutionResult executionResult = executeSingle(snapshot, "SIMULATION", "", input);
            record.setVariableJson(writeJson(executionResult.variableView));
            record.setExplainJson(writeJson(executionResult.explainView));
            record.setResultJson(writeJson(executionResult.resultView));
            record.setStatus(SIMULATION_STATUS_SUCCESS);
            record.setErrorMessage("");
            simulationRecordMapper.insert(record);
            return record;
        }
        catch (Exception e)
        {
            record.setVariableJson(writeJson(Collections.emptyMap()));
            record.setExplainJson(writeJson(Collections.singletonMap("error", e.getMessage())));
            record.setResultJson(writeJson(Collections.emptyMap()));
            record.setStatus(SIMULATION_STATUS_FAILED);
            record.setErrorMessage(limitLength(e.getMessage(), 1000));
            simulationRecordMapper.insert(record);
            if (StringUtils.isEmpty(bizNo))
            {
                throw e instanceof ServiceException ? (ServiceException) e : new ServiceException("试算执行失败：" + e.getMessage());
            }
            return record;
        }
    }

    private Map<String, Object> buildSimulationBatchItem(CostSimulationRecord record)
    {
        LinkedHashMap<String, Object> item = new LinkedHashMap<>();
        item.put("simulationId", record.getSimulationId());
        item.put("simulationNo", record.getSimulationNo());
        item.put("status", record.getStatus());
        item.put("bizNo", resolveString(parseObjectJson(record.getInputJson(), "试算输入必须是 JSON 对象"), "bizNo", "biz_no"));
        item.put("errorMessage", record.getErrorMessage());
        item.put("simulationTime", record.getCreateTime());
        return item;
    }

    @Override
    public Map<String, Object> selectSimulationDetail(Long simulationId)
    {
        CostSimulationRecord record = simulationRecordMapper.selectById(simulationId);
        if (record == null)
        {
            throw new ServiceException("试算记录不存在，请刷新后重试");
        }
        enrichSimulationRecords(Collections.singletonList(record));
        LinkedHashMap<String, Object> result = new LinkedHashMap<>();
        result.put("record", record);
        result.put("input", parseJsonToObject(record.getInputJson()));
        result.put("variables", parseJsonToObject(record.getVariableJson()));
        result.put("explain", parseJsonToObject(record.getExplainJson()));
        result.put("result", parseJsonToObject(record.getResultJson()));
        return result;
    }

    @Override
    public Map<String, Object> selectTaskStats(CostCalcTask query)
    {
        List<CostCalcTask> tasks = selectTaskListInternal(query);
        LinkedHashMap<String, Object> stats = new LinkedHashMap<>();
        stats.put("taskCount", tasks.size());
        stats.put("runningCount", tasks.stream().filter(item -> TASK_STATUS_RUNNING.equals(item.getTaskStatus())).count());
        stats.put("successCount", tasks.stream().filter(item -> TASK_STATUS_SUCCESS.equals(item.getTaskStatus())).count());
        stats.put("failedCount", tasks.stream().filter(item -> TASK_STATUS_FAILED.equals(item.getTaskStatus()) || TASK_STATUS_PART_SUCCESS.equals(item.getTaskStatus())).count());
        return stats;
    }

    @Override
    public Map<String, Object> selectTaskOverview(CostCalcTask query)
    {
        List<CostCalcTask> tasks = selectTaskListInternal(query);
        enrichTasks(tasks);
        List<CostCalcTaskPartition> partitions = selectTaskPartitions(tasks);
        LinkedHashMap<String, Object> overview = new LinkedHashMap<>();
        overview.put("recentTaskTrend", buildTaskTrend(tasks, 7));
        overview.put("recentPartitionTrend", buildPartitionTrend(partitions, 7));
        overview.put("topRiskTasks", buildTopRiskTasks(tasks, partitions, 5));
        overview.put("taskStatusDistribution", buildTaskStatusDistribution(tasks));
        overview.put("inputSourceDistribution", buildInputSourceDistribution(tasks));
        return overview;
    }

    @Override
    public List<CostCalcTask> selectTaskList(CostCalcTask query)
    {
        List<CostCalcTask> tasks = selectTaskListInternal(query);
        enrichTasks(tasks);
        return tasks;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> submitTask(CostCalcTaskSubmitBo bo)
    {
        RuntimeSnapshot snapshot = loadRuntimeSnapshot(bo.getSceneId(), bo.getVersionId(), true);
        List<Map<String, Object>> inputs = parseTaskInput(bo);
        validateBillMonth(bo.getBillMonth());
        CostBillPeriod period = ensureBillPeriodAvailable(snapshot.sceneId, bo.getBillMonth(), snapshot.versionId);
        if (StringUtils.isNotEmpty(bo.getRequestNo()))
        {
            CostCalcTask existing = calcTaskMapper.selectOne(Wrappers.<CostCalcTask>lambdaQuery()
                    .eq(CostCalcTask::getSceneId, snapshot.sceneId)
                    .eq(CostCalcTask::getVersionId, snapshot.versionId)
                    .eq(CostCalcTask::getBillMonth, bo.getBillMonth())
                    .eq(CostCalcTask::getRequestNo, bo.getRequestNo())
                    .last("limit 1"));
            if (existing != null)
            {
                return selectTaskDetail(existing.getTaskId());
            }
        }

        Date now = DateUtils.getNowDate();
        String operator = resolveOperator();
        CostCalcTask task = new CostCalcTask();
        task.setTaskNo(buildRunNo("TASK"));
        task.setSceneId(snapshot.sceneId);
        task.setVersionId(snapshot.versionId);
        task.setTaskType(bo.getTaskType());
        task.setBillMonth(bo.getBillMonth());
        task.setSourceCount(inputs.size());
        task.setSuccessCount(0);
        task.setFailCount(0);
        task.setTaskStatus(TASK_STATUS_INIT);
        task.setProgressPercent(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
        task.setRequestNo(firstNonBlank(bo.getRequestNo(), ""));
        task.setExecuteNode(resolveExecuteNode());
        task.setInputSourceType(resolveInputSourceType(bo));
        task.setSourceBatchNo(firstNonBlank(bo.getSourceBatchNo(), ""));
        task.setErrorMessage("");
        task.setRemark(bo.getRemark());
        task.setCreateBy(operator);
        task.setCreateTime(now);
        task.setUpdateBy(operator);
        task.setUpdateTime(now);
        calcTaskMapper.insert(task);
        markPeriodInProgress(period, task);

        List<CostCalcTaskDetail> details = buildTaskDetails(task, inputs);
        if (!details.isEmpty())
        {
            calcTaskDetailMapper.insertBatch(details);
            calcTaskPartitionMapper.insertBatch(buildTaskPartitions(task, details));
            markInputBatchSubmitted(task.getSourceBatchNo(), operator);
        }
        auditService.recordAudit(snapshot.sceneId, "CALC_TASK", task.getTaskNo(),
                "SUBMIT", "提交正式核算任务", null, task, task.getRequestNo());
        dispatchTaskAfterCommit(task.getTaskId());
        return selectTaskDetail(task.getTaskId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> createInputBatch(CostCalcInputBatchCreateBo bo)
    {
        RuntimeSnapshot snapshot = loadRuntimeSnapshot(bo.getSceneId(), bo.getVersionId(), true);
        validateBillMonth(bo.getBillMonth());
        List<Map<String, Object>> inputs = parseArrayJson(bo.getInputJson(), "导入批次输入必须是 JSON 数组");
        if (inputs.isEmpty())
        {
            throw new ServiceException("导入批次输入不能为空数组");
        }
        validateDuplicateBizNo(inputs);
        Date now = DateUtils.getNowDate();
        String operator = resolveOperator();

        CostCalcInputBatch batch = new CostCalcInputBatch();
        batch.setBatchNo(buildRunNo("INPUT"));
        batch.setSceneId(snapshot.sceneId);
        batch.setVersionId(snapshot.versionId);
        batch.setBillMonth(bo.getBillMonth());
        batch.setSourceType("JSON_IMPORT");
        batch.setBatchStatus(INPUT_BATCH_STATUS_READY);
        batch.setTotalCount(inputs.size());
        batch.setValidCount(inputs.size());
        batch.setErrorCount(0);
        batch.setRemark(bo.getRemark());
        batch.setErrorMessage("");
        batch.setCreateBy(operator);
        batch.setCreateTime(now);
        batch.setUpdateBy(operator);
        batch.setUpdateTime(now);
        calcInputBatchMapper.insert(batch);

        List<CostCalcInputBatchItem> items = new ArrayList<>();
        for (int i = 0; i < inputs.size(); i++)
        {
            Map<String, Object> input = inputs.get(i);
            CostCalcInputBatchItem item = new CostCalcInputBatchItem();
            item.setBatchId(batch.getBatchId());
            item.setBatchNo(batch.getBatchNo());
            item.setItemNo(i + 1);
            item.setBizNo(resolveBizNo(input, i + 1));
            item.setItemStatus(INPUT_BATCH_STATUS_READY);
            item.setInputJson(writeJson(input));
            item.setErrorMessage("");
            items.add(item);
        }
        calcInputBatchItemMapper.insertBatch(items);
        return selectInputBatchDetail(batch.getBatchId());
    }

    @Override
    public List<CostCalcInputBatch> selectInputBatchList(CostCalcInputBatch query)
    {
        List<CostCalcInputBatch> batches = calcInputBatchMapper.selectList(Wrappers.<CostCalcInputBatch>lambdaQuery()
                .eq(query.getSceneId() != null, CostCalcInputBatch::getSceneId, query.getSceneId())
                .eq(query.getVersionId() != null, CostCalcInputBatch::getVersionId, query.getVersionId())
                .eq(StringUtils.isNotEmpty(query.getBillMonth()), CostCalcInputBatch::getBillMonth, query.getBillMonth())
                .eq(StringUtils.isNotEmpty(query.getBatchStatus()), CostCalcInputBatch::getBatchStatus, query.getBatchStatus())
                .eq(StringUtils.isNotEmpty(query.getSourceType()), CostCalcInputBatch::getSourceType, query.getSourceType())
                .like(StringUtils.isNotEmpty(query.getBatchNo()), CostCalcInputBatch::getBatchNo, query.getBatchNo())
                .orderByDesc(CostCalcInputBatch::getBatchId));
        enrichInputBatches(batches);
        return batches;
    }

    @Override
    public Map<String, Object> selectInputBatchDetail(Long batchId)
    {
        CostCalcInputBatch batch = calcInputBatchMapper.selectById(batchId);
        enrichInputBatches(Collections.singletonList(batch));
        if (batch == null)
        {
            throw new ServiceException("输入批次不存在，请刷新后重试");
        }
        List<CostCalcInputBatchItem> items = calcInputBatchItemMapper.selectList(Wrappers.<CostCalcInputBatchItem>lambdaQuery()
                .eq(CostCalcInputBatchItem::getBatchId, batchId)
                .orderByAsc(CostCalcInputBatchItem::getItemNo)
                .orderByAsc(CostCalcInputBatchItem::getItemId));
        LinkedHashMap<String, Object> result = new LinkedHashMap<>();
        result.put("batch", batch);
        result.put("items", items);
        return result;
    }

    @Override
    public Map<String, Object> selectTaskDetail(Long taskId)
    {
        CostCalcTask task = calcTaskMapper.selectById(taskId);
        if (task == null)
        {
            throw new ServiceException("核算任务不存在，请刷新后重试");
        }
        enrichTasks(Collections.singletonList(task));
        List<CostCalcTaskDetail> details = calcTaskDetailMapper.selectList(Wrappers.<CostCalcTaskDetail>lambdaQuery()
                .eq(CostCalcTaskDetail::getTaskId, taskId)
                .orderByAsc(CostCalcTaskDetail::getPartitionNo)
                .orderByAsc(CostCalcTaskDetail::getDetailId));
        List<CostCalcTaskPartition> partitions = calcTaskPartitionMapper.selectList(Wrappers.<CostCalcTaskPartition>lambdaQuery()
                .eq(CostCalcTaskPartition::getTaskId, taskId)
                .orderByAsc(CostCalcTaskPartition::getPartitionNo)
                .orderByAsc(CostCalcTaskPartition::getPartitionId));
        LinkedHashMap<String, Object> summary = new LinkedHashMap<>();
        summary.put("sourceCount", task.getSourceCount());
        summary.put("successCount", task.getSuccessCount());
        summary.put("failCount", task.getFailCount());
        summary.put("progressPercent", task.getProgressPercent());
        summary.put("detailCount", details.size());
        summary.put("partitionCount", partitions.size());
        summary.put("failedPartitionCount", partitions.stream().filter(item -> NumberUtils.toInt(String.valueOf(item.getFailCount()), 0) > 0).count());
        summary.put("retryableCount", details.stream().filter(item -> DETAIL_STATUS_FAILED.equals(item.getDetailStatus())).count());
        summary.put("topErrors", details.stream()
                .filter(item -> DETAIL_STATUS_FAILED.equals(item.getDetailStatus()) && StringUtils.isNotEmpty(item.getErrorMessage()))
                .collect(Collectors.groupingBy(item -> limitLength(item.getErrorMessage(), 120), LinkedHashMap::new, Collectors.counting()))
                .entrySet().stream()
                .sorted((left, right) -> Long.compare(right.getValue(), left.getValue()))
                .limit(5)
                .map(entry ->
                {
                    LinkedHashMap<String, Object> error = new LinkedHashMap<>();
                    error.put("message", entry.getKey());
                    error.put("count", entry.getValue());
                    return error;
                })
                .collect(Collectors.toList()));

        LinkedHashMap<String, Object> result = new LinkedHashMap<>();
        result.put("task", task);
        result.put("summary", summary);
        result.put("partitions", partitions);
        result.put("details", details);
        if (StringUtils.isNotEmpty(task.getSourceBatchNo()))
        {
            CostCalcInputBatch inputBatch = calcInputBatchMapper.selectOne(Wrappers.<CostCalcInputBatch>lambdaQuery()
                    .eq(CostCalcInputBatch::getBatchNo, task.getSourceBatchNo())
                    .last("limit 1"));
            if (inputBatch != null)
            {
                LinkedHashMap<String, Object> inputBatchDetail = new LinkedHashMap<>();
                inputBatchDetail.put("batch", inputBatch);
                inputBatchDetail.put("items", calcInputBatchItemMapper.selectList(Wrappers.<CostCalcInputBatchItem>lambdaQuery()
                        .eq(CostCalcInputBatchItem::getBatchId, inputBatch.getBatchId())
                        .orderByAsc(CostCalcInputBatchItem::getItemNo)
                        .orderByAsc(CostCalcInputBatchItem::getItemId)
                        .last("limit 10")));
                result.put("inputBatch", inputBatchDetail);
            }
        }
        return result;
    }

    @Override
    public int retryTaskDetail(Long detailId)
    {
        CostCalcTaskDetail detail = calcTaskDetailMapper.selectById(detailId);
        if (detail == null)
        {
            throw new ServiceException("任务明细不存在，请刷新后重试");
        }
        CostCalcTask task = calcTaskMapper.selectById(detail.getTaskId());
        if (task == null)
        {
            throw new ServiceException("所属核算任务不存在，请刷新后重试");
        }
        int nextRetryCount = (detail.getRetryCount() == null ? 0 : detail.getRetryCount()) + 1;
        calcTaskDetailMapper.update(null, Wrappers.<CostCalcTaskDetail>lambdaUpdate()
                .eq(CostCalcTaskDetail::getDetailId, detailId)
                .set(CostCalcTaskDetail::getDetailStatus, DETAIL_STATUS_INIT)
                .set(CostCalcTaskDetail::getRetryCount, nextRetryCount)
                .set(CostCalcTaskDetail::getErrorMessage, ""));
        auditService.recordAudit(task.getSceneId(), "CALC_TASK_DETAIL", detail.getBizNo(),
                "RETRY", "重试正式核算明细", detail, calcTaskDetailMapper.selectById(detailId), task.getRequestNo());
        if (nextRetryCount >= 3)
        {
            createTaskAlarm(task, detail, "TASK_DETAIL_RETRY_LIMIT", "WARN",
                    "任务明细重试次数达到阈值", "业务单号 " + detail.getBizNo() + " 的重试次数已达到 " + nextRetryCount + " 次");
        }
        dispatchTaskAfterCommit(task.getTaskId());
        return 1;
    }

    @Override
    public int retryTaskPartition(Long partitionId)
    {
        CostCalcTaskPartition partition = calcTaskPartitionMapper.selectById(partitionId);
        if (partition == null)
        {
            throw new ServiceException("任务分片不存在，请刷新后重试");
        }
        CostCalcTask task = calcTaskMapper.selectById(partition.getTaskId());
        if (task == null)
        {
            throw new ServiceException("所属核算任务不存在，请刷新后重试");
        }
        List<CostCalcTaskDetail> failedDetails = calcTaskDetailMapper.selectList(Wrappers.<CostCalcTaskDetail>lambdaQuery()
                .eq(CostCalcTaskDetail::getTaskId, partition.getTaskId())
                .eq(CostCalcTaskDetail::getPartitionNo, partition.getPartitionNo())
                .eq(CostCalcTaskDetail::getDetailStatus, DETAIL_STATUS_FAILED)
                .orderByAsc(CostCalcTaskDetail::getDetailId));
        if (failedDetails.isEmpty())
        {
            return 0;
        }
        for (CostCalcTaskDetail detail : failedDetails)
        {
            int nextRetryCount = (detail.getRetryCount() == null ? 0 : detail.getRetryCount()) + 1;
            calcTaskDetailMapper.update(null, Wrappers.<CostCalcTaskDetail>lambdaUpdate()
                    .eq(CostCalcTaskDetail::getDetailId, detail.getDetailId())
                    .set(CostCalcTaskDetail::getDetailStatus, DETAIL_STATUS_INIT)
                    .set(CostCalcTaskDetail::getRetryCount, nextRetryCount)
                    .set(CostCalcTaskDetail::getErrorMessage, "")
                    .set(CostCalcTaskDetail::getResultSummary, ""));
        }
        calcTaskPartitionMapper.update(null, Wrappers.<CostCalcTaskPartition>lambdaUpdate()
                .eq(CostCalcTaskPartition::getPartitionId, partitionId)
                .set(CostCalcTaskPartition::getPartitionStatus, TASK_STATUS_INIT)
                .set(CostCalcTaskPartition::getProcessedCount, 0)
                .set(CostCalcTaskPartition::getSuccessCount, 0)
                .set(CostCalcTaskPartition::getFailCount, 0)
                .set(CostCalcTaskPartition::getStartedTime, null)
                .set(CostCalcTaskPartition::getFinishedTime, null)
                .set(CostCalcTaskPartition::getDurationMs, 0)
                .set(CostCalcTaskPartition::getLastError, ""));
        auditService.recordAudit(task.getSceneId(), "CALC_TASK_PARTITION", task.getTaskNo() + "#" + partition.getPartitionNo(),
                "RETRY", "重试正式核算分片", partition, calcTaskPartitionMapper.selectById(partitionId), task.getRequestNo());
        dispatchTaskAfterCommit(task.getTaskId());
        return 1;
    }

    @Override
    public int cancelTask(Long taskId)
    {
        CostCalcTask task = calcTaskMapper.selectById(taskId);
        if (task == null)
        {
            throw new ServiceException("核算任务不存在，请刷新后重试");
        }
        if (!TASK_STATUS_INIT.equals(task.getTaskStatus()) && !TASK_STATUS_RUNNING.equals(task.getTaskStatus()))
        {
            return 0;
        }
        int rows = calcTaskMapper.update(null, Wrappers.<CostCalcTask>lambdaUpdate()
                .eq(CostCalcTask::getTaskId, taskId)
                .set(CostCalcTask::getTaskStatus, TASK_STATUS_CANCELLED)
                .set(CostCalcTask::getErrorMessage, "任务已手工终止")
                .set(CostCalcTask::getUpdateBy, resolveOperator())
                .set(CostCalcTask::getUpdateTime, DateUtils.getNowDate()));
        calcTaskPartitionMapper.update(null, Wrappers.<CostCalcTaskPartition>lambdaUpdate()
                .eq(CostCalcTaskPartition::getTaskId, taskId)
                .in(CostCalcTaskPartition::getPartitionStatus, TASK_STATUS_INIT, TASK_STATUS_RUNNING)
                .set(CostCalcTaskPartition::getPartitionStatus, TASK_STATUS_CANCELLED)
                .set(CostCalcTaskPartition::getLastError, "任务已手工终止")
                .set(CostCalcTaskPartition::getFinishedTime, DateUtils.getNowDate())
                .set(CostCalcTaskPartition::getUpdateTime, DateUtils.getNowDate()));
        refreshBillPeriod(task.getSceneId(), task.getBillMonth(), task);
        syncRecalcByTask(task, TASK_STATUS_CANCELLED);
        auditService.recordAudit(task.getSceneId(), "CALC_TASK", task.getTaskNo(),
                "CANCEL", "取消正式核算任务", task, calcTaskMapper.selectById(taskId), task.getRequestNo());
        return rows;
    }

    @Override
    public Map<String, Object> selectResultStats(CostResultLedger query)
    {
        List<CostResultLedger> results = selectResultListInternal(query);
        BigDecimal totalAmount = results.stream()
                .map(CostResultLedger::getAmountValue)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        LinkedHashMap<String, Object> stats = new LinkedHashMap<>();
        stats.put("resultCount", results.size());
        stats.put("taskCount", results.stream().map(CostResultLedger::getTaskId).filter(Objects::nonNull).distinct().count());
        stats.put("traceCount", results.stream().map(CostResultLedger::getTraceId).filter(Objects::nonNull).distinct().count());
        stats.put("amountTotal", totalAmount.setScale(2, RoundingMode.HALF_UP));
        return stats;
    }

    @Override
    public List<CostResultLedger> selectResultList(CostResultLedger query)
    {
        List<CostResultLedger> results = selectResultListInternal(query);
        enrichResults(results);
        return results;
    }

    @Override
    public Map<String, Object> selectResultDetail(Long resultId)
    {
        CostResultLedger ledger = resultLedgerMapper.selectById(resultId);
        if (ledger == null)
        {
            throw new ServiceException("结果台账不存在，请刷新后重试");
        }
        enrichResults(Collections.singletonList(ledger));
        CostResultTrace trace = ledger.getTraceId() == null ? null : resultTraceMapper.selectById(ledger.getTraceId());
        LinkedHashMap<String, Object> result = new LinkedHashMap<>();
        result.put("ledger", ledger);
        result.put("trace", trace == null ? null : buildTraceView(trace));
        return result;
    }

    @Override
    public Map<String, Object> selectTraceDetail(Long traceId)
    {
        CostResultTrace trace = resultTraceMapper.selectById(traceId);
        if (trace == null)
        {
            throw new ServiceException("追溯记录不存在，请刷新后重试");
        }
        return buildTraceView(trace);
    }

    @Override
    public List<Map<String, Object>> selectVersionOptions(Long sceneId)
    {
        List<CostPublishVersion> versions = publishVersionMapper.selectList(Wrappers.<CostPublishVersion>lambdaQuery()
                .eq(CostPublishVersion::getSceneId, sceneId)
                .orderByDesc(CostPublishVersion::getPublishedTime)
                .orderByDesc(CostPublishVersion::getVersionId));
        return versions.stream().map(version -> {
            LinkedHashMap<String, Object> item = new LinkedHashMap<>();
            item.put("versionId", version.getVersionId());
            item.put("versionNo", version.getVersionNo());
            item.put("versionStatus", version.getVersionStatus());
            item.put("publishedTime", version.getPublishedTime());
            return item;
        }).collect(Collectors.toList());
    }

    @Override
    public Map<String, Object> buildInputTemplate(Long sceneId, Long versionId, String taskType)
    {
        String normalizedTaskType = normalizeTemplateTaskType(taskType);
        RuntimeSnapshot snapshot = loadRuntimeSnapshot(sceneId, versionId, false, isSimulationTaskType(normalizedTaskType));

        LinkedHashMap<String, Object> result = new LinkedHashMap<>();
        result.put("sceneId", snapshot.sceneId);
        result.put("sceneCode", snapshot.sceneCode);
        result.put("sceneName", snapshot.sceneName);
        result.put("versionId", snapshot.versionId);
        result.put("versionNo", snapshot.versionNo);
        result.put("snapshotSource", snapshot.snapshotSource);
        result.put("taskType", normalizedTaskType);
        result.put("message", buildInputTemplateMessage(snapshot));
        result.put("fields", buildTemplateFieldItems(snapshot));
        result.put("inputJson", buildTemplateInputJson(snapshot.variables, normalizedTaskType));
        return result;
    }

    @Override
    public Map<String, Object> buildFeeInputTemplate(Long sceneId, Long versionId, Long feeId, String feeCode, String taskType)
    {
        RuntimeSnapshot snapshot = loadRuntimeSnapshot(sceneId, versionId, false);
        RuntimeFee fee = resolveRuntimeFee(snapshot, feeId, feeCode);
        List<RuntimeFee> executionFees = resolveFeeExecutionChain(snapshot, fee);
        List<RuntimeRule> rules = snapshot.rulesByFeeCode.getOrDefault(fee.feeCode, Collections.emptyList());
        List<RuntimeRule> executionRules = executionFees.stream()
                .flatMap(item -> snapshot.rulesByFeeCode.getOrDefault(item.feeCode, Collections.emptyList()).stream())
                .collect(Collectors.toList());
        FeeTemplateContext templateContext = buildFeeTemplateContext(snapshot, executionRules);
        List<RuntimeVariable> inputVariables = templateContext.variables.values().stream()
                .filter(item -> item.includedInTemplate)
                .map(item -> item.variable)
                .sorted(Comparator.comparingInt(item -> item.sortNo == null ? 9999 : item.sortNo))
                .collect(Collectors.toList());
        String normalizedTaskType = normalizeTemplateTaskType(taskType);

        LinkedHashMap<String, Object> feeView = new LinkedHashMap<>();
        feeView.put("feeId", fee.feeId);
        feeView.put("feeCode", fee.feeCode);
        feeView.put("feeName", fee.feeName);
        feeView.put("unitCode", fee.unitCode);
        feeView.put("objectDimension", fee.objectDimension);

        LinkedHashMap<String, Object> result = new LinkedHashMap<>();
        result.put("sceneId", snapshot.sceneId);
        result.put("sceneCode", snapshot.sceneCode);
        result.put("sceneName", snapshot.sceneName);
        result.put("versionId", snapshot.versionId);
        result.put("versionNo", snapshot.versionNo);
        result.put("snapshotSource", snapshot.snapshotSource);
        result.put("taskType", normalizedTaskType);
        result.put("fee", feeView);
        result.put("ruleCount", rules.size());
        result.put("executionFeeCount", executionFees.size());
        result.put("executionFeeCodes", executionFees.stream().map(item -> item.feeCode).collect(Collectors.toList()));
        result.put("dependentFeeCodes", executionFees.stream()
                .map(item -> item.feeCode)
                .filter(code -> !StringUtils.equals(code, fee.feeCode))
                .collect(Collectors.toList()));
        result.put("fieldCount", templateContext.variables.size());
        result.put("inputFieldCount", inputVariables.size());
        result.put("message", buildFeeTemplateMessage(snapshot, rules.isEmpty()));
        result.put("fields", buildFeeTemplateFieldItems(templateContext));
        result.put("ruleSummary", templateContext.ruleSummaries);
        result.put("inputJson", buildTemplateInputJson(inputVariables, normalizedTaskType));
        return result;
    }

    @Override
    public Map<String, Object> calculateFee(CostFeeCalculateBo bo)
    {
        RuntimeSnapshot snapshot = loadRuntimeSnapshot(bo.getSceneId(), bo.getVersionId(), false);
        RuntimeFee fee = resolveRuntimeFee(snapshot, bo.getFeeId(), bo.getFeeCode());
        List<RuntimeFee> executionFees = resolveFeeExecutionChain(snapshot, fee);
        List<Map<String, Object>> inputs = parseInlineCalculationInputs(bo.getInputJson());
        String billMonth = StringUtils.isEmpty(bo.getBillMonth()) ? "" : bo.getBillMonth();
        boolean includeExplain = Boolean.TRUE.equals(bo.getIncludeExplain());
        if (StringUtils.isNotEmpty(billMonth))
        {
            validateBillMonth(billMonth);
        }

        List<Map<String, Object>> records = new ArrayList<>();
        int successCount = 0;
        int noMatchCount = 0;
        int failedCount = 0;
        long startedAt = System.currentTimeMillis();
        for (int i = 0; i < inputs.size(); i++)
        {
            Map<String, Object> input = inputs.get(i);
            long recordStartedAt = System.currentTimeMillis();
            try
            {
                ExecutionResult executionResult = executeSingle(snapshot, "FEE_CALC", billMonth, input,
                        executionFees, includeExplain);
                records.add(buildFeeCalculationRecord(input, fee, executionResult, i + 1, includeExplain,
                        System.currentTimeMillis() - recordStartedAt));
                FeeExecutionResult feeResult = findFeeExecutionResult(executionResult, fee.feeCode);
                if (feeResult == null)
                {
                    noMatchCount++;
                }
                else
                {
                    successCount++;
                }
            }
            catch (Exception e)
            {
                records.add(buildFeeCalculationFailureRecord(input, fee, i + 1, e,
                        System.currentTimeMillis() - recordStartedAt, includeExplain));
                failedCount++;
            }
        }

        LinkedHashMap<String, Object> feeView = new LinkedHashMap<>();
        feeView.put("feeId", fee.feeId);
        feeView.put("feeCode", fee.feeCode);
        feeView.put("feeName", fee.feeName);
        feeView.put("unitCode", fee.unitCode);
        feeView.put("objectDimension", fee.objectDimension);

        LinkedHashMap<String, Object> result = new LinkedHashMap<>();
        result.put("sceneId", snapshot.sceneId);
        result.put("sceneCode", snapshot.sceneCode);
        result.put("sceneName", snapshot.sceneName);
        result.put("versionId", snapshot.versionId);
        result.put("versionNo", snapshot.versionNo);
        result.put("snapshotSource", snapshot.snapshotSource);
        result.put("billMonth", billMonth);
        result.put("fee", feeView);
        result.put("executionFeeCount", executionFees.size());
        result.put("executionFeeCodes", executionFees.stream().map(item -> item.feeCode).collect(Collectors.toList()));
        result.put("dependentFeeCodes", executionFees.stream()
                .map(item -> item.feeCode)
                .filter(code -> !StringUtils.equals(code, fee.feeCode))
                .collect(Collectors.toList()));
        result.put("includeExplain", includeExplain);
        result.put("recordCount", inputs.size());
        result.put("successCount", successCount);
        result.put("noMatchCount", noMatchCount);
        result.put("failedCount", failedCount);
        result.put("durationMs", System.currentTimeMillis() - startedAt);
        result.put("records", records);
        return result;
    }

    /**
     * 任务异步执行主链路。
     *
     * <p>当前阶段先采用线程池异步执行，满足线程五“正式核算与批量任务异步化”的基础要求，
     * 后续线程六再在此基础上增强分片并发、Redis 锁和分布式节点协调。</p>
     */
    private void runTaskAsync(Long taskId)
    {
        CostCalcTask task = calcTaskMapper.selectById(taskId);
        if (task == null || TASK_STATUS_CANCELLED.equals(task.getTaskStatus()))
        {
            return;
        }
        Date startedTime = DateUtils.getNowDate();

        try
        {
            RuntimeSnapshot snapshot = loadRuntimeSnapshot(task.getSceneId(), task.getVersionId(), true);
            calcTaskMapper.update(null, Wrappers.<CostCalcTask>lambdaUpdate()
                    .eq(CostCalcTask::getTaskId, taskId)
                    .notIn(CostCalcTask::getTaskStatus, TASK_STATUS_CANCELLED)
                    .set(CostCalcTask::getTaskStatus, TASK_STATUS_RUNNING)
                    .set(CostCalcTask::getStartedTime, startedTime)
                    .set(CostCalcTask::getUpdateTime, startedTime));

            List<CostCalcTaskDetail> details = calcTaskDetailMapper.selectList(Wrappers.<CostCalcTaskDetail>lambdaQuery()
                    .eq(CostCalcTaskDetail::getTaskId, taskId)
                    .in(CostCalcTaskDetail::getDetailStatus, DETAIL_STATUS_INIT, DETAIL_STATUS_FAILED)
                    .orderByAsc(CostCalcTaskDetail::getPartitionNo)
                    .orderByAsc(CostCalcTaskDetail::getDetailId));
            if (details.isEmpty())
            {
                finishTask(taskId, startedTime);
                return;
            }
            List<List<CostCalcTaskDetail>> partitions = splitTaskPartitions(details);
            ExecutorCompletionService<PartitionExecutionResult> completionService =
                    new ExecutorCompletionService<>(threadPoolTaskExecutor.getThreadPoolExecutor());
            Map<Future<PartitionExecutionResult>, List<CostCalcTaskDetail>> futurePartitions = new LinkedHashMap<>();
            int nextPartitionIndex = 0;
            int completedCount = 0;
            int maxParallelism = resolveTaskParallelism(partitions.size());
            while (nextPartitionIndex < partitions.size() && futurePartitions.size() < maxParallelism)
            {
                List<CostCalcTaskDetail> partition = partitions.get(nextPartitionIndex++);
                markPartitionRunning(taskId, partition);
                Future<PartitionExecutionResult> future =
                        completionService.submit(() -> executeTaskPartition(taskId, snapshot, partition));
                futurePartitions.put(future, partition);
            }
            while (completedCount < partitions.size())
            {
                Future<PartitionExecutionResult> future = completionService.take();
                List<CostCalcTaskDetail> partition = futurePartitions.remove(future);
                completedCount++;
                try
                {
                    PartitionExecutionResult partitionResult = future.get();
                    finishPartition(taskId, partition, partitionResult, null);
                }
                catch (ExecutionException e)
                {
                    Throwable cause = e.getCause() == null ? e : e.getCause();
                    PartitionExecutionResult fallbackResult = markPartitionFailed(taskId, partition, cause);
                    finishPartition(taskId, partition, fallbackResult, cause);
                }
                refreshTaskProgress(taskId);
                CostCalcTask latestTask = calcTaskMapper.selectById(taskId);
                if (latestTask == null || TASK_STATUS_CANCELLED.equals(latestTask.getTaskStatus()))
                {
                    break;
                }
                if (nextPartitionIndex < partitions.size())
                {
                    List<CostCalcTaskDetail> nextPartition = partitions.get(nextPartitionIndex++);
                    markPartitionRunning(taskId, nextPartition);
                    Future<PartitionExecutionResult> nextFuture =
                            completionService.submit(() -> executeTaskPartition(taskId, snapshot, nextPartition));
                    futurePartitions.put(nextFuture, nextPartition);
                }
            }
            finishTask(taskId, startedTime);
        }
        catch (Exception e)
        {
            calcTaskMapper.update(null, Wrappers.<CostCalcTask>lambdaUpdate()
                    .eq(CostCalcTask::getTaskId, taskId)
                    .set(CostCalcTask::getTaskStatus, TASK_STATUS_FAILED)
                    .set(CostCalcTask::getErrorMessage, limitLength(e.getMessage(), 1000))
                    .set(CostCalcTask::getFinishedTime, DateUtils.getNowDate())
                    .set(CostCalcTask::getDurationMs, DateUtils.getNowDate().getTime() - startedTime.getTime())
                    .set(CostCalcTask::getUpdateTime, DateUtils.getNowDate()));
            CostCalcTask latest = calcTaskMapper.selectById(taskId);
            if (latest != null)
            {
                refreshBillPeriod(latest.getSceneId(), latest.getBillMonth(), latest);
                syncRecalcByTask(latest, TASK_STATUS_FAILED);
                createTaskAlarm(latest, null, "TASK_FAILED", "ERROR",
                        "正式核算任务执行失败", limitLength(e.getMessage(), 500));
            }
        }
    }

    /**
     * 在事务提交后再触发异步任务，避免新建任务尚未提交时工作线程读不到数据。
     */
    private void dispatchTaskAfterCommit(Long taskId)
    {
        Runnable runnable = () -> threadPoolTaskExecutor.execute(() -> runTaskAsync(taskId));
        if (TransactionSynchronizationManager.isActualTransactionActive())
        {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization()
            {
                @Override
                public void afterCommit()
                {
                    runnable.run();
                }
            });
            return;
        }
        runnable.run();
    }

    /**
     * 处理单条任务明细，并落结果台账与追溯解释。
     */
    @Transactional(rollbackFor = Exception.class)
    protected void processTaskDetail(CostCalcTask task, CostCalcTaskDetail detail, RuntimeSnapshot snapshot)
    {
        Map<String, Object> input = parseObjectJson(detail.getInputJson(), "任务明细输入必须是 JSON 对象");
        purgeExistingTaskResults(task.getTaskId(), detail.getBizNo());
        ExecutionResult executionResult = executeSingle(snapshot, task.getTaskNo(), task.getBillMonth(), input);

        List<CostResultLedger> ledgers = new ArrayList<>();
        for (FeeExecutionResult feeResult : executionResult.feeResults)
        {
            CostResultTrace trace = new CostResultTrace();
            trace.setSceneId(snapshot.sceneId);
            trace.setVersionId(snapshot.versionId);
            trace.setRuleId(feeResult.ruleId);
            trace.setTierId(feeResult.tierId);
            trace.setVariableJson(writeJson(feeResult.variableExplain));
            trace.setConditionJson(writeJson(feeResult.conditionExplain));
            trace.setPricingJson(writeJson(feeResult.pricingExplain));
            trace.setTimelineJson(writeJson(feeResult.timelineSteps));
            resultTraceMapper.insert(trace);

            CostResultLedger ledger = new CostResultLedger();
            ledger.setTaskId(task.getTaskId());
            ledger.setTaskNo(task.getTaskNo());
            ledger.setSceneId(snapshot.sceneId);
            ledger.setVersionId(snapshot.versionId);
            ledger.setFeeId(feeResult.feeId);
            ledger.setFeeCode(feeResult.feeCode);
            ledger.setFeeName(feeResult.feeName);
            ledger.setBizNo(detail.getBizNo());
            ledger.setBillMonth(task.getBillMonth());
            ledger.setObjectDimension(firstNonBlank(feeResult.objectDimension, resolveString(input, "objectDimension", "object_dimension")));
            ledger.setObjectCode(firstNonBlank(resolveString(input, "objectCode", "object_code"), detail.getBizNo()));
            ledger.setObjectName(resolveString(input, "objectName", "object_name", "name"));
            ledger.setQuantityValue(feeResult.quantityValue);
            ledger.setUnitPrice(feeResult.unitPrice);
            ledger.setAmountValue(feeResult.amountValue);
            ledger.setCurrencyCode("CNY");
            ledger.setResultStatus(RESULT_STATUS_SUCCESS);
            ledger.setTraceId(trace.getTraceId());
            resultLedgerMapper.insert(ledger);
            ledgers.add(ledger);
        }

        calcTaskDetailMapper.update(null, Wrappers.<CostCalcTaskDetail>lambdaUpdate()
                .eq(CostCalcTaskDetail::getDetailId, detail.getDetailId())
                .set(CostCalcTaskDetail::getDetailStatus, DETAIL_STATUS_SUCCESS)
                .set(CostCalcTaskDetail::getResultSummary, buildDetailSummary(ledgers))
                .set(CostCalcTaskDetail::getErrorMessage, ""));
    }

    private void purgeExistingTaskResults(Long taskId, String bizNo)
    {
        List<CostResultLedger> existing = resultLedgerMapper.selectList(Wrappers.<CostResultLedger>lambdaQuery()
                .eq(CostResultLedger::getTaskId, taskId)
                .eq(CostResultLedger::getBizNo, bizNo));
        if (existing.isEmpty())
        {
            return;
        }
        List<Long> traceIds = existing.stream().map(CostResultLedger::getTraceId).filter(Objects::nonNull).collect(Collectors.toList());
        resultLedgerMapper.deleteBatchIds(existing.stream().map(CostResultLedger::getResultId).collect(Collectors.toList()));
        if (!traceIds.isEmpty())
        {
            resultTraceMapper.deleteBatchIds(traceIds);
        }
    }

    private void refreshTaskProgress(Long taskId)
    {
        TaskExecutionSummary summary = summarizeTaskDetails(taskId);
        BigDecimal progress = summary.totalCount <= 0
                ? BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)
                : BigDecimal.valueOf(summary.processedCount * 100.0 / summary.totalCount).setScale(2, RoundingMode.HALF_UP);
        calcTaskMapper.update(null, Wrappers.<CostCalcTask>lambdaUpdate()
                .eq(CostCalcTask::getTaskId, taskId)
                .set(CostCalcTask::getSuccessCount, summary.successCount)
                .set(CostCalcTask::getFailCount, summary.failedCount)
                .set(CostCalcTask::getProgressPercent, progress)
                .set(CostCalcTask::getUpdateTime, DateUtils.getNowDate()));
    }

    private void finishTask(Long taskId, Date startedTime)
    {
        CostCalcTask latestTask = calcTaskMapper.selectById(taskId);
        if (latestTask != null && TASK_STATUS_CANCELLED.equals(latestTask.getTaskStatus()))
        {
            return;
        }
        TaskExecutionSummary summary = summarizeTaskDetails(taskId);
        String status = summary.failedCount <= 0
                ? TASK_STATUS_SUCCESS
                : (summary.successCount > 0 ? TASK_STATUS_PART_SUCCESS : TASK_STATUS_FAILED);
        Date finishedTime = DateUtils.getNowDate();
        calcTaskMapper.update(null, Wrappers.<CostCalcTask>lambdaUpdate()
                .eq(CostCalcTask::getTaskId, taskId)
                .set(CostCalcTask::getTaskStatus, status)
                .set(CostCalcTask::getSuccessCount, summary.successCount)
                .set(CostCalcTask::getFailCount, summary.failedCount)
                .set(CostCalcTask::getProgressPercent, BigDecimal.valueOf(100).setScale(2, RoundingMode.HALF_UP))
                .set(CostCalcTask::getFinishedTime, finishedTime)
                .set(CostCalcTask::getDurationMs, finishedTime.getTime() - startedTime.getTime())
                .set(CostCalcTask::getUpdateTime, finishedTime));
        CostCalcTask task = calcTaskMapper.selectById(taskId);
        if (task != null)
        {
            refreshBillPeriod(task.getSceneId(), task.getBillMonth(), task);
            syncRecalcByTask(task, status);
            auditService.recordAudit(task.getSceneId(), "CALC_TASK", task.getTaskNo(),
                    "FINISH", "正式核算任务完成", null, task, task.getRequestNo());
            if (TASK_STATUS_FAILED.equals(status) || TASK_STATUS_PART_SUCCESS.equals(status))
            {
                createTaskAlarm(task, null, "TASK_FINISHED_WITH_ERROR",
                        TASK_STATUS_FAILED.equals(status) ? "ERROR" : "WARN",
                        TASK_STATUS_FAILED.equals(status) ? "正式核算任务失败" : "正式核算任务部分成功",
                        "任务 " + task.getTaskNo() + " 完成状态为 " + status + "，成功 " + summary.successCount + " 条，失败 " + summary.failedCount + " 条。");
            }
        }
    }

    /**
     * 执行单条业务对象的统一核算内核。
     *
     * <p>该方法同时服务试算和正式核算：
     * 1. 先基于发布快照构建只读运行视图；
     * 2. 再计算变量；
     * 3. 逐费用筛选命中规则和阶梯；
     * 4. 产出费用结果、解释视图和时间线。</p>
     */
    private ExecutionResult executeSingle(RuntimeSnapshot snapshot, String taskNo, String billMonth, Map<String, Object> input)
    {
        return executeSingle(snapshot, taskNo, billMonth, input, snapshot.fees, false);
    }

    private ExecutionResult executeSingle(RuntimeSnapshot snapshot, String taskNo, String billMonth,
            Map<String, Object> input, List<RuntimeFee> targetFees)
    {
        return executeSingle(snapshot, taskNo, billMonth, input, targetFees, false);
    }

    private ExecutionResult executeSingle(RuntimeSnapshot snapshot, String taskNo, String billMonth,
            Map<String, Object> input, List<RuntimeFee> targetFees, boolean includeExplain)
    {
        List<RuntimeFee> feesToExecute = targetFees == null || targetFees.isEmpty() ? snapshot.fees : targetFees;
        LinkedHashMap<String, Object> baseContext = new LinkedHashMap<>(input);
        baseContext.put("billMonth", billMonth);
        baseContext.put("sceneCode", snapshot.sceneCode);
        baseContext.put("versionNo", snapshot.versionNo);
        LinkedHashMap<String, Object> variableValues = computeVariables(snapshot, baseContext,
                resolveExecutionVariables(snapshot, feesToExecute));
        List<FeeExecutionResult> feeResults = new ArrayList<>();
        List<Map<String, Object>> timeline = new ArrayList<>();
        ExecutionResult result = new ExecutionResult();

        LinkedHashMap<String, Object> feeResultContext = new LinkedHashMap<>();
        for (RuntimeFee fee : feesToExecute)
        {
            List<RuntimeRule> rules = snapshot.rulesByFeeCode.getOrDefault(fee.feeCode, Collections.emptyList());
            RuleMatchResult matchResult = matchRule(rules, variableValues, baseContext, includeExplain);
            if (matchResult == null || matchResult.rule == null)
            {
                if (includeExplain)
                {
                    result.skippedFeeExplains.put(fee.feeCode,
                            buildFeeNoMatchExplain(fee, rules, variableValues,
                                    matchResult == null ? Collections.emptyList() : matchResult.ruleEvaluations));
                }
                timeline.add(buildStep("FEE_SKIP", fee.feeCode, fee.feeName, "当前费用未命中任何启用规则"));
                continue;
            }
            PricingResult pricingResult = calculateAmount(matchResult.rule, matchResult.tier, variableValues, baseContext, feeResultContext, snapshot);
            pricingResult.pricingExplain.put("unitCode", fee.unitCode);
            pricingResult.pricingExplain.put("unitSemantic", buildUnitSemanticSummary(fee.unitCode));
            FeeExecutionResult feeResult = new FeeExecutionResult();
            feeResult.feeId = fee.feeId;
            feeResult.feeCode = fee.feeCode;
            feeResult.feeName = fee.feeName;
            feeResult.unitCode = fee.unitCode;
            feeResult.objectDimension = fee.objectDimension;
            feeResult.ruleId = matchResult.rule.ruleId;
            feeResult.ruleCode = matchResult.rule.ruleCode;
            feeResult.ruleName = matchResult.rule.ruleName;
            feeResult.tierId = matchResult.tier == null ? null : matchResult.tier.tierId;
            feeResult.quantityValue = pricingResult.quantityValue;
            feeResult.unitPrice = pricingResult.unitPrice;
            feeResult.amountValue = pricingResult.amountValue;
            feeResult.variableExplain = variableValues;
            feeResult.conditionExplain = matchResult.conditionExplain;
            feeResult.pricingExplain = pricingResult.pricingExplain;
            feeResult.timelineSteps = buildFeeTimeline(fee, matchResult, pricingResult);
            feeResults.add(feeResult);
            feeResultContext.put(fee.feeCode, feeResult.toExplainView());
            timeline.add(buildStep("FEE_RESULT", fee.feeCode, fee.feeName,
                    String.format(Locale.ROOT, "命中规则 %s，金额 %s", feeResult.ruleCode, feeResult.amountValue)));
        }

        LinkedHashMap<String, Object> resultView = new LinkedHashMap<>();
        resultView.put("taskNo", taskNo);
        resultView.put("sceneCode", snapshot.sceneCode);
        resultView.put("versionNo", snapshot.versionNo);
        resultView.put("snapshotSource", snapshot.snapshotSource);
        resultView.put("bizNo", resolveBizNo(input, 1));
        resultView.put("feeResults", feeResults.stream().map(FeeExecutionResult::toView).collect(Collectors.toList()));
        resultView.put("amountTotal", feeResults.stream().map(item -> item.amountValue).reduce(BigDecimal.ZERO, BigDecimal::add));

        LinkedHashMap<String, Object> explainView = new LinkedHashMap<>();
        explainView.put("timeline", timeline);
        explainView.put("matchedFees", feeResults.stream().map(FeeExecutionResult::toExplainView).collect(Collectors.toList()));

        result.variableView = variableValues;
        result.resultView = resultView;
        result.explainView = explainView;
        result.feeResults = feeResults;
        return result;
    }

    private LinkedHashMap<String, Object> computeVariables(RuntimeSnapshot snapshot, Map<String, Object> baseContext)
    {
        return computeVariables(snapshot, baseContext, snapshot.variables);
    }

    private LinkedHashMap<String, Object> computeVariables(RuntimeSnapshot snapshot, Map<String, Object> baseContext,
            List<RuntimeVariable> runtimeVariables)
    {
        LinkedHashMap<String, Object> values = new LinkedHashMap<>();
        List<RuntimeVariable> variablesToCompute = runtimeVariables == null ? snapshot.variables : runtimeVariables;
        Map<String, RuntimeVariable> variableMap = snapshot == null || snapshot.variablesByCode == null
                ? Collections.emptyMap()
                : snapshot.variablesByCode;
        LinkedHashSet<String> dependencyStack = new LinkedHashSet<>();
        for (RuntimeVariable variable : variablesToCompute)
        {
            resolveRuntimeVariableValue(snapshot, variable, baseContext, values, variableMap, dependencyStack);
        }
        return values;
    }

    private Object resolveRuntimeVariableValue(RuntimeSnapshot snapshot, RuntimeVariable variable,
            Map<String, Object> baseContext, LinkedHashMap<String, Object> computedValues,
            Map<String, RuntimeVariable> variableMap, Set<String> dependencyStack)
    {
        if (variable == null || StringUtils.isEmpty(variable.variableCode))
        {
            return null;
        }
        if (computedValues.containsKey(variable.variableCode))
        {
            return computedValues.get(variable.variableCode);
        }
        if (!dependencyStack.add(variable.variableCode))
        {
            throw new ServiceException("公式变量存在循环依赖：" + String.join(" -> ", dependencyStack) + " -> " + variable.variableCode);
        }
        try
        {
            Object value;
            if (SOURCE_TYPE_FORMULA.equals(variable.sourceType))
            {
                String formulaExpression = resolveVariableFormula(snapshot, variable);
                for (String dependencyCode : extractExpressionVariableCodes(formulaExpression, variableMap))
                {
                    if (StringUtils.equals(variable.variableCode, dependencyCode))
                    {
                        continue;
                    }
                    RuntimeVariable dependency = variableMap.get(dependencyCode);
                    if (dependency != null)
                    {
                        resolveRuntimeVariableValue(snapshot, dependency, baseContext, computedValues, variableMap, dependencyStack);
                    }
                }
                value = evaluateExpression(formulaExpression, mergeContext(baseContext, computedValues, Collections.emptyMap()));
            }
            else
            {
                value = resolveValueFromInput(baseContext, variable.dataPath, variable.variableCode, variable.defaultValue);
            }
            Object converted = convertValueByType(value, variable.dataType, variable.defaultValue);
            computedValues.put(variable.variableCode, converted);
            return converted;
        }
        finally
        {
            dependencyStack.remove(variable.variableCode);
        }
    }

    private RuleMatchResult matchRule(List<RuntimeRule> rules, Map<String, Object> variableValues,
            Map<String, Object> baseContext, boolean includeExplain)
    {
        Map<String, Object> conditionContext = mergeContext(baseContext, variableValues, Collections.emptyMap());
        RuleMatchResult fallbackResult = includeExplain ? new RuleMatchResult() : null;
        for (RuntimeRule rule : rules)
        {
            List<Map<String, Object>> conditionExplain = new ArrayList<>();
            ConditionMatchResult conditionMatchResult = matchConditions(rule, variableValues, conditionContext, conditionExplain);
            if (includeExplain)
            {
                fallbackResult.ruleEvaluations.add(buildRuleEvaluation(rule, conditionMatchResult, conditionExplain));
            }
            if (conditionMatchResult.matched)
            {
                RuleMatchResult result = new RuleMatchResult();
                result.rule = rule;
                result.matchedGroupNo = conditionMatchResult.matchedGroupNo;
                result.rule.matchedGroupNo = conditionMatchResult.matchedGroupNo;
                result.conditionExplain = conditionExplain;
                result.ruleEvaluations = includeExplain ? fallbackResult.ruleEvaluations : Collections.emptyList();
                if (RULE_TYPE_TIER_RATE.equals(rule.ruleType))
                {
                    BigDecimal quantityValue = toBigDecimal(variableValues.get(rule.quantityVariableCode));
                    result.tier = locateTier(rule.tiers, quantityValue);
                    if (result.tier == null)
                    {
                        throw new ServiceException(String.format("规则 %s 未找到可命中的阶梯区间", rule.ruleCode));
                    }
                }
                return result;
            }
        }
        return fallbackResult;
    }

    private Map<String, Object> buildRuleEvaluation(RuntimeRule rule, ConditionMatchResult matchResult,
            List<Map<String, Object>> conditionExplain)
    {
        LinkedHashMap<String, Object> item = new LinkedHashMap<>();
        item.put("ruleCode", rule.ruleCode);
        item.put("ruleName", rule.ruleName);
        item.put("ruleType", rule.ruleType);
        item.put("pricingMode", rule.pricingMode);
        item.put("priority", rule.priority);
        item.put("matched", matchResult.matched);
        item.put("matchedGroupNo", matchResult.matchedGroupNo);
        item.put("conditions", conditionExplain);
        return item;
    }

    private ConditionMatchResult matchConditions(RuntimeRule rule, Map<String, Object> variableValues,
            Map<String, Object> conditionContext, List<Map<String, Object>> explain)
    {
        ConditionMatchResult result = new ConditionMatchResult();
        if (rule.conditionGroups == null || rule.conditionGroups.isEmpty())
        {
            result.matched = true;
            result.matchedGroupNo = 1;
            return result;
        }
        List<Boolean> groupResults = new ArrayList<>(rule.conditionGroups.size());
        for (RuntimeConditionGroup group : rule.conditionGroups)
        {
            boolean groupPass = true;
            for (RuntimeCondition condition : group.conditions)
            {
                Object leftValue = variableValues.get(condition.variableCode);
                boolean pass = evaluateCondition(condition, leftValue, conditionContext);
                LinkedHashMap<String, Object> item = new LinkedHashMap<>();
                item.put("groupNo", group.groupNo);
                item.put("displayName", condition.displayName);
                item.put("variableCode", condition.variableCode);
                item.put("leftValue", leftValue);
                item.put("operatorCode", condition.operatorCode);
                item.put("compareValue", condition.compareValue);
                item.put("pass", pass);
                explain.add(item);
                groupPass = groupPass && pass;
            }
            groupResults.add(groupPass);
            if (groupPass && result.matchedGroupNo == null)
            {
                result.matchedGroupNo = group.groupNo;
            }
        }
        if ("OR".equalsIgnoreCase(rule.conditionLogic))
        {
            result.matched = groupResults.stream().anyMatch(Boolean::booleanValue);
            if (!result.matched)
            {
                result.matchedGroupNo = null;
            }
            return result;
        }
        result.matched = groupResults.stream().allMatch(Boolean::booleanValue);
        if (!result.matched)
        {
            result.matchedGroupNo = null;
        }
        return result;
    }

    private boolean evaluateCondition(RuntimeCondition condition, Object leftValue, Map<String, Object> context)
    {
        String operatorCode = condition.operatorCode;
        if (OP_EXPR.equals(operatorCode))
        {
            Object exprResult = evaluateExpression(condition.compareValue, context);
            return Boolean.TRUE.equals(convertBoolean(exprResult));
        }
        if (OP_IN.equals(operatorCode) || OP_NOT_IN.equals(operatorCode))
        {
            List<String> values = splitValues(condition.compareValue);
            boolean contains = values.contains(String.valueOf(leftValue));
            return OP_IN.equals(operatorCode) ? contains : !contains;
        }
        if (OP_BETWEEN.equals(operatorCode))
        {
            List<String> values = splitValues(condition.compareValue);
            if (values.size() < 2)
            {
                return false;
            }
            BigDecimal left = toBigDecimal(leftValue);
            BigDecimal start = toBigDecimal(values.get(0));
            BigDecimal end = toBigDecimal(values.get(1));
            if (left == null || start == null || end == null)
            {
                return false;
            }
            return left.compareTo(start) >= 0 && left.compareTo(end) <= 0;
        }
        BigDecimal leftNumber = toBigDecimal(leftValue);
        BigDecimal rightNumber = toBigDecimal(condition.compareValue);
        switch (operatorCode)
        {
            case OP_EQ:
                return Objects.equals(String.valueOf(leftValue), String.valueOf(condition.compareValue));
            case OP_NE:
                return !Objects.equals(String.valueOf(leftValue), String.valueOf(condition.compareValue));
            case OP_GT:
                return leftNumber != null && rightNumber != null && leftNumber.compareTo(rightNumber) > 0;
            case OP_GE:
                return leftNumber != null && rightNumber != null && leftNumber.compareTo(rightNumber) >= 0;
            case OP_LT:
                return leftNumber != null && rightNumber != null && leftNumber.compareTo(rightNumber) < 0;
            case OP_LE:
                return leftNumber != null && rightNumber != null && leftNumber.compareTo(rightNumber) <= 0;
            default:
                return false;
        }
    }

    private RuntimeTier locateTier(List<RuntimeTier> tiers, BigDecimal quantityValue)
    {
        if (quantityValue == null)
        {
            return null;
        }
        for (RuntimeTier tier : tiers)
        {
            BigDecimal start = tier.startValue;
            BigDecimal end = tier.endValue;
            boolean pass;
            if (INTERVAL_LORC.equals(tier.intervalMode))
            {
                pass = (start == null || quantityValue.compareTo(start) > 0)
                        && (end == null || quantityValue.compareTo(end) <= 0);
            }
            else
            {
                pass = (start == null || quantityValue.compareTo(start) >= 0)
                        && (end == null || quantityValue.compareTo(end) < 0);
            }
            if (pass)
            {
                return tier;
            }
        }
        return null;
    }

    private PricingResult calculateAmount(RuntimeRule rule, RuntimeTier tier, Map<String, Object> variableValues,
            Map<String, Object> baseContext, Map<String, Object> feeResultContext, RuntimeSnapshot snapshot)
    {
        BigDecimal quantityValue = toBigDecimal(variableValues.get(rule.quantityVariableCode));
        if (quantityValue == null)
        {
            quantityValue = BigDecimal.ONE;
        }
        PricingResult result = new PricingResult();
        result.quantityValue = quantityValue.setScale(4, RoundingMode.HALF_UP);
        result.pricingExplain = new LinkedHashMap<>();
        result.pricingExplain.put("ruleCode", rule.ruleCode);
        result.pricingExplain.put("ruleType", rule.ruleType);
        result.pricingExplain.put("quantityVariableCode", rule.quantityVariableCode);
        result.pricingExplain.put("quantityValue", result.quantityValue);
        result.pricingExplain.put("pricingMode", rule.pricingMode);
        result.pricingExplain.put("matchedGroupNo", rule.matchedGroupNo);
        if (RULE_TYPE_FIXED_RATE.equals(rule.ruleType))
        {
            BigDecimal unitPrice = resolveGroupedPricingValue(rule, "rateValue");
            result.unitPrice = defaultZero(unitPrice).setScale(6, RoundingMode.HALF_UP);
            result.amountValue = result.unitPrice.multiply(result.quantityValue).setScale(2, RoundingMode.HALF_UP);
            result.pricingExplain.put("pricingSource", "FIXED_RATE");
        }
        else if (RULE_TYPE_FIXED_AMOUNT.equals(rule.ruleType))
        {
            BigDecimal amountValue = resolveGroupedPricingValue(rule, "amountValue");
            result.unitPrice = defaultZero(amountValue).setScale(6, RoundingMode.HALF_UP);
            result.amountValue = defaultZero(amountValue).setScale(2, RoundingMode.HALF_UP);
            result.pricingExplain.put("pricingSource", "FIXED_AMOUNT");
        }
        else if (RULE_TYPE_FORMULA.equals(rule.ruleType))
        {
            RuntimeFormula formula = requireRuleFormula(snapshot, rule);
            String expression = formula.formulaExpr;
            Object amountValue = evaluateExpression(expression, mergeContext(baseContext, variableValues, feeResultContext));
            BigDecimal computed = defaultZero(toBigDecimal(amountValue)).setScale(2, RoundingMode.HALF_UP);
            result.unitPrice = computed.setScale(6, RoundingMode.HALF_UP);
            result.amountValue = computed;
            result.pricingExplain.put("pricingSource", "FORMULA");
            result.pricingExplain.put("formula", expression);
            result.pricingExplain.put("formulaCode", formula.formulaCode);
            result.pricingExplain.put("formulaName", formula.formulaName);
            result.pricingExplain.put("businessFormula", formula.businessFormula);
        }
        else if (RULE_TYPE_TIER_RATE.equals(rule.ruleType))
        {
            BigDecimal unitPrice = tier == null ? BigDecimal.ZERO : defaultZero(tier.rateValue);
            result.unitPrice = unitPrice.setScale(6, RoundingMode.HALF_UP);
            result.amountValue = result.unitPrice.multiply(result.quantityValue).setScale(2, RoundingMode.HALF_UP);
            result.pricingExplain.put("pricingSource", "TIER_RATE");
            result.pricingExplain.put("tierNo", tier == null ? null : tier.tierNo);
            result.pricingExplain.put("tierRange", tier == null ? null : tier.buildRangeSummary());
        }
        else
        {
            throw new ServiceException("暂不支持的规则类型：" + rule.ruleType);
        }
        result.pricingExplain.put("unitPrice", result.unitPrice);
        result.pricingExplain.put("amountValue", result.amountValue);
        return result;
    }

    private BigDecimal resolveGroupedPricingValue(RuntimeRule rule, String valueKey)
    {
        if (!PRICING_MODE_GROUPED.equalsIgnoreCase(rule.pricingMode))
        {
            return toBigDecimal(rule.pricingConfig.get(valueKey));
        }
        Object rawGroupPrices = rule.pricingConfig.get("groupPrices");
        if (!(rawGroupPrices instanceof List<?> groupPrices))
        {
            throw new ServiceException(String.format("规则 %s 未配置组合定价明细", rule.ruleCode));
        }
        for (Object item : groupPrices)
        {
            if (!(item instanceof Map<?, ?> rawMap))
            {
                continue;
            }
            Integer groupNo = intValue(rawMap.get("groupNo"));
            if (Objects.equals(groupNo, rule.matchedGroupNo))
            {
                BigDecimal value = toBigDecimal(rawMap.get(valueKey));
                if (value == null)
                {
                    throw new ServiceException(String.format("规则 %s 的组合组 %s 未配置定价值", rule.ruleCode, groupNo));
                }
                return value;
            }
        }
        throw new ServiceException(String.format("规则 %s 未找到命中组合组 %s 对应的定价配置", rule.ruleCode, rule.matchedGroupNo));
    }

    private List<Map<String, Object>> buildFeeTimeline(RuntimeFee fee, RuleMatchResult matchResult, PricingResult pricingResult)
    {
        List<Map<String, Object>> steps = new ArrayList<>();
        steps.add(buildStep("FEE", fee.feeCode, fee.feeName, "进入费用计算"));
        steps.add(buildStep("RULE", matchResult.rule.ruleCode, matchResult.rule.ruleName, "命中规则"));
        if (matchResult.tier != null)
        {
            steps.add(buildStep("TIER", String.valueOf(matchResult.tier.tierNo), matchResult.tier.buildRangeSummary(), "命中阶梯区间"));
        }
        steps.add(buildStep("PRICING", fee.feeCode, fee.feeName,
                buildPricingStepSummary(fee.unitCode, pricingResult)));
        return steps;
    }

    /**
     * 构建带计价单位语义的定价摘要，便于结果追溯和业务复核。
     */
    private String buildPricingStepSummary(String unitCode, PricingResult pricingResult)
    {
        String pricingSource = pricingResult.pricingExplain == null ? "" : stringValue(pricingResult.pricingExplain.get("pricingSource"));
        if (RULE_TYPE_FIXED_AMOUNT.equals(pricingSource) || "FIXED_AMOUNT".equals(pricingSource) || "元".equals(unitCode))
        {
            return String.format(Locale.ROOT, "计价单位 %s，按固定金额计价，结果 %s 元", firstNonBlank(unitCode, "元"), pricingResult.amountValue);
        }
        if (pricingResult.quantityValue == null)
        {
            return String.format(Locale.ROOT, "计价单位 %s，单价 %s，金额 %s", firstNonBlank(unitCode, "-"), pricingResult.unitPrice, pricingResult.amountValue);
        }
        String normalizedUnit = firstNonBlank(unitCode, "-");
        return String.format(Locale.ROOT, "数量 %s %s，单价 %s 元/%s，金额 %s 元",
                pricingResult.quantityValue,
                normalizedUnit,
                pricingResult.unitPrice,
                normalizedUnit,
                pricingResult.amountValue);
    }

    /**
     * 统一输出计价单位业务口径，供追溯解释和结果页直接展示。
     */
    private String buildUnitSemanticSummary(String unitCode)
    {
        if ("吨".equals(unitCode))
        {
            return "按重量吨数计价";
        }
        if ("天".equals(unitCode))
        {
            return "按天数计价";
        }
        if ("次".equals(unitCode))
        {
            return "按次数计价";
        }
        if ("航次".equals(unitCode))
        {
            return "按航次计价";
        }
        if ("人".equals(unitCode))
        {
            return "按人数计价";
        }
        if ("箱".equals(unitCode))
        {
            return "按箱量计价";
        }
        if ("元".equals(unitCode))
        {
            return "按固定金额计价";
        }
        if ("平方米*天".equals(unitCode) || "平方米·天".equals(unitCode))
        {
            return "按面积天复合量计价";
        }
        return "按当前计价单位口径计价";
    }

    private Map<String, Object> buildTraceView(CostResultTrace trace)
    {
        LinkedHashMap<String, Object> result = new LinkedHashMap<>();
        result.put("traceId", trace.getTraceId());
        result.put("sceneId", trace.getSceneId());
        result.put("versionId", trace.getVersionId());
        result.put("ruleId", trace.getRuleId());
        result.put("tierId", trace.getTierId());
        result.put("variables", parseJsonToObject(trace.getVariableJson()));
        result.put("conditions", parseJsonToObject(trace.getConditionJson()));
        result.put("pricing", parseJsonToObject(trace.getPricingJson()));
        result.put("timeline", parseJsonToObject(trace.getTimelineJson()));
        result.put("createTime", trace.getCreateTime());
        return result;
    }

    private List<Map<String, Object>> buildTemplateFieldItems(RuntimeSnapshot snapshot)
    {
        List<Map<String, Object>> fields = new ArrayList<>();
        for (RuntimeVariable variable : snapshot.variables)
        {
            LinkedHashMap<String, Object> field = new LinkedHashMap<>();
            String path = firstNonBlank(variable.dataPath, variable.variableCode);
            boolean formulaDerived = SOURCE_TYPE_FORMULA.equals(variable.sourceType);
            field.put("variableCode", variable.variableCode);
            field.put("variableName", variable.variableName);
            field.put("sourceType", variable.sourceType);
            field.put("dataType", variable.dataType);
            field.put("path", path);
            field.put("includedInTemplate", !formulaDerived && StringUtils.isNotEmpty(path));
            field.put("templateRole", formulaDerived ? "FORMULA_DERIVED" : "INPUT_REQUIRED");
            field.put("exampleValue", buildTemplateValue(variable, 1));
            fields.add(field);
        }
        return fields;
    }

    private List<Map<String, Object>> buildFeeTemplateFieldItems(FeeTemplateContext templateContext)
    {
        return templateContext.variables.values().stream()
                .sorted(Comparator.comparing((FeeTemplateVariable item) -> !item.includedInTemplate)
                        .thenComparingInt(item -> item.variable.sortNo == null ? 9999 : item.variable.sortNo)
                        .thenComparing(item -> firstNonBlank(item.variable.variableCode, "")))
                .map(item -> {
                    RuntimeVariable variable = item.variable;
                    LinkedHashMap<String, Object> field = new LinkedHashMap<>();
                    String path = firstNonBlank(variable.dataPath, variable.variableCode);
                    field.put("variableCode", variable.variableCode);
                    field.put("variableName", variable.variableName);
                    field.put("sourceType", variable.sourceType);
                    field.put("dataType", variable.dataType);
                    field.put("path", path);
                    field.put("includedInTemplate", item.includedInTemplate && StringUtils.isNotEmpty(path));
                    field.put("templateRoles", new ArrayList<>(item.templateRoles));
                    field.put("sourceRuleCodes", new ArrayList<>(item.sourceRuleCodes));
                    field.put("dependsOn", new ArrayList<>(item.dependsOn));
                    field.put("defaultValue", variable.defaultValue);
                    field.put("exampleValue", item.includedInTemplate && StringUtils.isNotEmpty(path)
                            ? buildTemplateValue(variable, 1) : null);
                    return field;
                })
                .collect(Collectors.toList());
    }

    private FeeTemplateContext buildFeeTemplateContext(RuntimeSnapshot snapshot, List<RuntimeRule> rules)
    {
        FeeTemplateContext context = new FeeTemplateContext();
        if (rules == null || rules.isEmpty())
        {
            return context;
        }
        Map<String, RuntimeVariable> variableMap = snapshot.variablesByCode == null
                ? Collections.emptyMap() : snapshot.variablesByCode;
        for (RuntimeRule rule : rules)
        {
            LinkedHashSet<String> conditionVariableCodes = new LinkedHashSet<>();
            LinkedHashSet<String> expressionVariableCodes = new LinkedHashSet<>();
            LinkedHashSet<String> formulaVariableCodes = new LinkedHashSet<>();
            if (StringUtils.isNotEmpty(rule.quantityVariableCode))
            {
                collectTemplateVariable(context, snapshot, variableMap, rule.quantityVariableCode,
                        "QUANTITY_BASIS", rule.ruleCode, new LinkedHashSet<>());
            }
            for (RuntimeCondition condition : rule.conditions)
            {
                if (StringUtils.isNotEmpty(condition.variableCode))
                {
                    conditionVariableCodes.add(condition.variableCode);
                    collectTemplateVariable(context, snapshot, variableMap, condition.variableCode,
                            "CONDITION", rule.ruleCode, new LinkedHashSet<>());
                }
                if (OP_EXPR.equalsIgnoreCase(condition.operatorCode))
                {
                    Set<String> referencedVariables = extractExpressionVariableCodes(condition.compareValue, variableMap);
                    expressionVariableCodes.addAll(referencedVariables);
                    for (String variableCode : referencedVariables)
                    {
                        collectTemplateVariable(context, snapshot, variableMap, variableCode,
                                "EXPRESSION_INPUT", rule.ruleCode, new LinkedHashSet<>());
                    }
                }
            }
            String ruleExpression = resolveRuleExpression(snapshot, rule);
            if (StringUtils.isNotEmpty(ruleExpression))
            {
                formulaVariableCodes.addAll(extractExpressionVariableCodes(ruleExpression, variableMap));
                for (String variableCode : formulaVariableCodes)
                {
                    collectTemplateVariable(context, snapshot, variableMap, variableCode,
                            "FORMULA_INPUT", rule.ruleCode, new LinkedHashSet<>());
                }
            }
            context.ruleSummaries.add(buildFeeRuleSummary(rule, conditionVariableCodes, expressionVariableCodes, formulaVariableCodes));
        }
        return context;
    }

    private void collectTemplateVariable(FeeTemplateContext context, RuntimeSnapshot snapshot, Map<String, RuntimeVariable> variableMap,
                                         String variableCode, String templateRole, String sourceRuleCode, Set<String> dependencyStack)
    {
        if (StringUtils.isEmpty(variableCode) || variableMap == null)
        {
            return;
        }
        RuntimeVariable variable = variableMap.get(variableCode);
        if (variable == null)
        {
            return;
        }
        FeeTemplateVariable templateVariable = context.variables.computeIfAbsent(variableCode,
                key -> new FeeTemplateVariable(variable));
        if (StringUtils.isNotEmpty(templateRole))
        {
            templateVariable.templateRoles.add(templateRole);
        }
        if (StringUtils.isNotEmpty(sourceRuleCode))
        {
            templateVariable.sourceRuleCodes.add(sourceRuleCode);
        }
        if (!SOURCE_TYPE_FORMULA.equalsIgnoreCase(variable.sourceType))
        {
            templateVariable.includedInTemplate = true;
            return;
        }
        templateVariable.templateRoles.add("FORMULA_DERIVED");
        if (!dependencyStack.add(variableCode))
        {
            return;
        }
        try
        {
            String formulaExpression = resolveVariableFormula(snapshot, variable);
            for (String dependencyCode : extractExpressionVariableCodes(formulaExpression, variableMap))
            {
                if (StringUtils.equals(variableCode, dependencyCode))
                {
                    continue;
                }
                templateVariable.dependsOn.add(dependencyCode);
                collectTemplateVariable(context, snapshot, variableMap, dependencyCode,
                        "FORMULA_INPUT", sourceRuleCode, dependencyStack);
            }
        }
        finally
        {
            dependencyStack.remove(variableCode);
        }
    }

    private Map<String, Object> buildFeeRuleSummary(RuntimeRule rule, Set<String> conditionVariableCodes,
                                                    Set<String> expressionVariableCodes, Set<String> formulaVariableCodes)
    {
        LinkedHashMap<String, Object> item = new LinkedHashMap<>();
        item.put("ruleCode", rule.ruleCode);
        item.put("ruleName", rule.ruleName);
        item.put("ruleType", rule.ruleType);
        item.put("pricingMode", rule.pricingMode);
        item.put("conditionLogic", rule.conditionLogic);
        item.put("priority", rule.priority);
        item.put("quantityVariableCode", rule.quantityVariableCode);
        item.put("conditionVariableCodes", new ArrayList<>(conditionVariableCodes));
        item.put("expressionVariableCodes", new ArrayList<>(expressionVariableCodes));
        item.put("formulaVariableCodes", new ArrayList<>(formulaVariableCodes));
        item.put("tierCount", rule.tiers == null ? 0 : rule.tiers.size());
        return item;
    }

    private List<RuntimeFee> resolveFeeExecutionChain(RuntimeSnapshot snapshot, RuntimeFee targetFee)
    {
        if (snapshot == null || targetFee == null)
        {
            return Collections.emptyList();
        }
        LinkedHashMap<String, RuntimeFee> orderedFees = new LinkedHashMap<>();
        collectFeeExecutionDependency(snapshot, targetFee.feeCode, orderedFees, new LinkedHashSet<>());
        return new ArrayList<>(orderedFees.values());
    }

    private void collectFeeExecutionDependency(RuntimeSnapshot snapshot, String feeCode,
            Map<String, RuntimeFee> orderedFees, Set<String> dependencyStack)
    {
        if (StringUtils.isEmpty(feeCode) || snapshot == null || snapshot.feesByCode == null)
        {
            return;
        }
        if (orderedFees.containsKey(feeCode))
        {
            return;
        }
        RuntimeFee currentFee = snapshot.feesByCode.get(feeCode);
        if (currentFee == null)
        {
            return;
        }
        if (!dependencyStack.add(feeCode))
        {
            throw new ServiceException("费用公式存在循环依赖：" + String.join(" -> ", dependencyStack) + " -> " + feeCode);
        }
        try
        {
            for (RuntimeRule rule : snapshot.rulesByFeeCode.getOrDefault(feeCode, Collections.emptyList()))
            {
                for (String dependencyFeeCode : extractExpressionFeeCodes(resolveRuleExpression(snapshot, rule)))
                {
                    if (!StringUtils.equals(feeCode, dependencyFeeCode))
                    {
                        collectFeeExecutionDependency(snapshot, dependencyFeeCode, orderedFees, dependencyStack);
                    }
                }
            }
            orderedFees.put(feeCode, currentFee);
        }
        finally
        {
            dependencyStack.remove(feeCode);
        }
    }

    private Set<String> extractExpressionFeeCodes(String expression)
    {
        LinkedHashSet<String> result = new LinkedHashSet<>();
        if (StringUtils.isEmpty(expression))
        {
            return result;
        }
        Matcher matcher = FEE_REFERENCE_PATTERN.matcher(expression);
        while (matcher.find())
        {
            String feeCode = matcher.group(1);
            if (StringUtils.isNotEmpty(feeCode))
            {
                result.add(feeCode);
            }
        }
        return result;
    }

    private List<RuntimeVariable> buildExecutionVariables(RuntimeSnapshot snapshot, List<RuntimeRule> rules)
    {
        if (snapshot == null || rules == null || rules.isEmpty())
        {
            return Collections.emptyList();
        }
        FeeTemplateContext context = buildFeeTemplateContext(snapshot, rules);
        if (context.variables.isEmpty())
        {
            return Collections.emptyList();
        }
        Set<String> variableCodes = context.variables.keySet();
        return snapshot.variables.stream()
                .filter(item -> variableCodes.contains(item.variableCode))
                .collect(Collectors.toList());
    }

    private List<RuntimeVariable> resolveExecutionVariables(RuntimeSnapshot snapshot, List<RuntimeFee> feesToExecute)
    {
        if (snapshot == null || feesToExecute == null || feesToExecute.isEmpty() || feesToExecute == snapshot.fees)
        {
            return snapshot == null ? Collections.emptyList() : snapshot.variables;
        }
        if (feesToExecute.size() == 1)
        {
            RuntimeFee fee = feesToExecute.get(0);
            return fee == null || snapshot.executionVariablesByFeeCode == null
                    ? Collections.emptyList()
                    : snapshot.executionVariablesByFeeCode.getOrDefault(fee.feeCode, Collections.emptyList());
        }
        LinkedHashSet<String> variableCodes = new LinkedHashSet<>();
        for (RuntimeFee fee : feesToExecute)
        {
            if (fee == null || snapshot.executionVariablesByFeeCode == null)
            {
                continue;
            }
            for (RuntimeVariable variable : snapshot.executionVariablesByFeeCode
                    .getOrDefault(fee.feeCode, Collections.emptyList()))
            {
                variableCodes.add(variable.variableCode);
            }
        }
        if (variableCodes.isEmpty())
        {
            return Collections.emptyList();
        }
        return snapshot.variables.stream()
                .filter(item -> variableCodes.contains(item.variableCode))
                .collect(Collectors.toList());
    }

    private RuntimeFee resolveRuntimeFee(RuntimeSnapshot snapshot, Long feeId, String feeCode)
    {
        if (feeId == null && StringUtils.isEmpty(feeCode))
        {
            throw new ServiceException("璇锋寚瀹氳垂鐢?ID 鎴?feeCode");
        }
        if (feeId != null)
        {
            for (RuntimeFee fee : snapshot.fees)
            {
                if (Objects.equals(fee.feeId, feeId))
                {
                    return fee;
                }
            }
        }
        if (StringUtils.isNotEmpty(feeCode) && snapshot.feesByCode != null && snapshot.feesByCode.containsKey(feeCode))
        {
            return snapshot.feesByCode.get(feeCode);
        }
        throw new ServiceException("指定费用在当前发布版本快照中不存在");
    }

    private String resolveRuleExpression(RuntimeSnapshot snapshot, RuntimeRule rule)
    {
        if (RULE_TYPE_FORMULA.equals(rule.ruleType))
        {
            return requireRuleFormula(snapshot, rule).formulaExpr;
        }
        return rule.amountFormula;
    }

    private Set<String> extractExpressionVariableCodes(String expression, Map<String, RuntimeVariable> variableMap)
    {
        LinkedHashSet<String> result = new LinkedHashSet<>();
        if (StringUtils.isEmpty(expression) || variableMap == null || variableMap.isEmpty())
        {
            return result;
        }
        String sanitized = expression.replaceAll("'[^']*'", " ").replaceAll("\"[^\"]*\"", " ");
        Matcher matcher = EXPRESSION_REFERENCE_PATTERN.matcher(sanitized);
        while (matcher.find())
        {
            String candidate = firstNonBlank(matcher.group(1), matcher.group(2));
            if (StringUtils.isNotEmpty(candidate) && variableMap.containsKey(candidate))
            {
                result.add(candidate);
            }
        }
        return result;
    }

    private String buildTemplateInputJson(List<RuntimeVariable> variables, String taskType)
    {
        if (TASK_TYPE_FORMAL_BATCH.equals(taskType) || TASK_TYPE_SIMULATION_BATCH.equals(taskType))
        {
            List<Map<String, Object>> samples = new ArrayList<>();
            samples.add(buildSelectedInputTemplate(variables, taskType, 1));
            samples.add(buildSelectedInputTemplate(variables, taskType, 2));
            return writeJson(samples);
        }
        return writeJson(buildSelectedInputTemplate(variables, taskType, 1));
    }

    private String normalizeTemplateTaskType(String taskType)
    {
        if (StringUtils.isEmpty(taskType))
        {
            return TASK_TYPE_FORMAL_SINGLE;
        }
        return taskType.trim().toUpperCase(Locale.ROOT);
    }

    private RuntimeSnapshot loadRuntimeSnapshot(Long sceneId, Long versionId, boolean requireFormalVersion)
    {
        return loadRuntimeSnapshot(sceneId, versionId, requireFormalVersion, false);
    }

    private RuntimeSnapshot loadRuntimeSnapshot(Long sceneId, Long versionId, boolean requireFormalVersion,
                                                boolean preferDraftWhenVersionMissing)
    {
        CostScene scene = sceneMapper.selectById(sceneId);
        if (scene == null)
        {
            throw new ServiceException("场景不存在，请刷新后重试");
        }
        if (!requireFormalVersion && preferDraftWhenVersionMissing && versionId == null)
        {
            return buildDraftRuntimeSnapshot(scene);
        }
        Long targetVersionId = versionId == null ? scene.getActiveVersionId() : versionId;
        if (targetVersionId == null && !requireFormalVersion)
        {
            return buildDraftRuntimeSnapshot(scene);
        }
        if (targetVersionId == null)
        {
            throw new ServiceException("当前场景尚未设置生效版本，无法执行线程五运行链");
        }
        CostPublishVersion version = publishVersionMapper.selectPublishVersionDetail(targetVersionId);
        if (version == null)
        {
            throw new ServiceException("发布版本不存在，请刷新后重试");
        }
        if (requireFormalVersion && versionId == null && !"ACTIVE".equals(version.getVersionStatus()))
        {
            throw new ServiceException("正式核算默认只能按当前生效版本执行");
        }
        String cacheKey = buildRuntimeCacheKey(targetVersionId);
        try
        {
            String cached = redisCache.getCacheObject(cacheKey);
            if (StringUtils.isNotEmpty(cached))
            {
                return hydrateRuntimeSnapshot(objectMapper.readValue(cached, RuntimeSnapshot.class));
            }
        }
        catch (Exception ignored)
        {
        }
        List<CostPublishSnapshot> snapshots = publishVersionMapper.selectSnapshotList(targetVersionId, null);
        if (snapshots == null || snapshots.isEmpty())
        {
            throw new ServiceException("发布快照为空，无法执行试算或正式核算");
        }

        RuntimeSnapshot snapshot = new RuntimeSnapshot();
        snapshot.sceneId = sceneId;
        snapshot.versionId = targetVersionId;
        snapshot.sceneCode = scene.getSceneCode();
        snapshot.sceneName = scene.getSceneName();
        snapshot.versionNo = version.getVersionNo();
        snapshot.snapshotSource = SNAPSHOT_SOURCE_PUBLISHED;
        Map<String, Long> feeIdByCode = feeMapper.selectList(Wrappers.<CostFeeItem>lambdaQuery()
                        .eq(CostFeeItem::getSceneId, sceneId))
                .stream()
                .collect(Collectors.toMap(CostFeeItem::getFeeCode, CostFeeItem::getFeeId, (left, right) -> left, LinkedHashMap::new));
        for (CostPublishSnapshot item : snapshots)
        {
            Map<String, Object> json = parseJsonMap(item.getSnapshotJson());
            if ("FEE".equals(item.getSnapshotType()))
            {
                RuntimeFee fee = new RuntimeFee();
                fee.feeCode = stringValue(json.get("feeCode"));
                fee.feeId = feeIdByCode.get(fee.feeCode);
                fee.feeName = stringValue(json.get("feeName"));
                fee.unitCode = stringValue(json.get("unitCode"));
                fee.objectDimension = stringValue(json.get("objectDimension"));
                fee.sortNo = intValue(json.get("sortNo"));
                snapshot.fees.add(fee);
                snapshot.feesByCode.put(fee.feeCode, fee);
            }
            else if ("VARIABLE".equals(item.getSnapshotType()))
            {
                RuntimeVariable variable = new RuntimeVariable();
                variable.variableCode = stringValue(json.get("variableCode"));
                variable.variableName = stringValue(json.get("variableName"));
                variable.sourceType = stringValue(json.get("sourceType"));
                variable.dataType = stringValue(json.get("dataType"));
                variable.dataPath = stringValue(json.get("dataPath"));
                variable.formulaExpr = stringValue(json.get("formulaExpr"));
                variable.formulaCode = stringValue(json.get("formulaCode"));
                variable.defaultValue = json.get("defaultValue");
                variable.sortNo = intValue(json.get("sortNo"));
                snapshot.variables.add(variable);
                snapshot.variablesByCode.put(variable.variableCode, variable);
            }
            else if ("FORMULA".equals(item.getSnapshotType()))
            {
                RuntimeFormula formula = new RuntimeFormula();
                formula.formulaCode = stringValue(json.get("formulaCode"));
                formula.formulaName = stringValue(json.get("formulaName"));
                formula.businessFormula = stringValue(json.get("businessFormula"));
                formula.formulaExpr = stringValue(json.get("formulaExpr"));
                formula.returnType = stringValue(json.get("returnType"));
                snapshot.formulasByCode.put(formula.formulaCode, formula);
            }
            else if ("RULE".equals(item.getSnapshotType()))
            {
                RuntimeRule rule = new RuntimeRule();
                rule.feeCode = stringValue(json.get("feeCode"));
                rule.ruleCode = stringValue(json.get("ruleCode"));
                rule.ruleName = stringValue(json.get("ruleName"));
                rule.ruleType = stringValue(json.get("ruleType"));
                rule.conditionLogic = firstNonBlank(stringValue(json.get("conditionLogic")), "AND");
                rule.priority = intValue(json.get("priority"));
                rule.quantityVariableCode = stringValue(json.get("quantityVariableCode"));
                rule.pricingMode = firstNonBlank(stringValue(json.get("pricingMode")), "TYPED");
                rule.pricingConfig = json.get("pricingJson") instanceof Map ? castMap(json.get("pricingJson")) : new LinkedHashMap<>();
                rule.amountFormula = stringValue(json.get("amountFormula"));
                rule.amountFormulaCode = stringValue(json.get("amountFormulaCode"));
                rule.amountBusinessFormula = stringValue(json.get("amountBusinessFormula"));
                rule.sortNo = intValue(json.get("sortNo"));
                snapshot.rulesByCode.put(rule.ruleCode, rule);
            }
            else if ("RULE_CONDITION".equals(item.getSnapshotType()))
            {
                RuntimeCondition condition = new RuntimeCondition();
                condition.ruleCode = stringValue(json.get("ruleCode"));
                condition.groupNo = intValue(json.get("groupNo"));
                condition.sortNo = intValue(json.get("sortNo"));
                condition.variableCode = stringValue(json.get("variableCode"));
                condition.displayName = firstNonBlank(stringValue(json.get("displayName")), condition.variableCode);
                condition.operatorCode = stringValue(json.get("operatorCode"));
                condition.compareValue = stringValue(json.get("compareValue"));
                snapshot.conditionsByRuleCode.computeIfAbsent(condition.ruleCode, key -> new ArrayList<>()).add(condition);
            }
            else if ("RULE_TIER".equals(item.getSnapshotType()))
            {
                RuntimeTier tier = new RuntimeTier();
                tier.ruleCode = stringValue(json.get("ruleCode"));
                tier.tierNo = intValue(json.get("tierNo"));
                tier.startValue = toBigDecimal(json.get("startValue"));
                tier.endValue = toBigDecimal(json.get("endValue"));
                tier.rateValue = toBigDecimal(json.get("rateValue"));
                tier.intervalMode = firstNonBlank(stringValue(json.get("intervalMode")), INTERVAL_LCRO);
                snapshot.tiersByRuleCode.computeIfAbsent(tier.ruleCode, key -> new ArrayList<>()).add(tier);
            }
        }

        snapshot = hydrateRuntimeSnapshot(snapshot);
        try
        {
            redisCache.setCacheObject(cacheKey, objectMapper.writeValueAsString(snapshot), 30, TimeUnit.MINUTES);
        }
        catch (JsonProcessingException ignored)
        {
        }
        return snapshot;
    }

    private RuntimeSnapshot hydrateRuntimeSnapshot(RuntimeSnapshot snapshot)
    {
        if (snapshot == null)
        {
            return new RuntimeSnapshot();
        }
        if (snapshot.fees == null)
        {
            snapshot.fees = new ArrayList<>();
        }
        if (snapshot.variables == null)
        {
            snapshot.variables = new ArrayList<>();
        }
        if (snapshot.feesByCode == null)
        {
            snapshot.feesByCode = new LinkedHashMap<>();
        }
        if (snapshot.variablesByCode == null)
        {
            snapshot.variablesByCode = new LinkedHashMap<>();
        }
        if (snapshot.rulesByCode == null)
        {
            snapshot.rulesByCode = new LinkedHashMap<>();
        }
        if (snapshot.rulesByFeeCode == null)
        {
            snapshot.rulesByFeeCode = new LinkedHashMap<>();
        }
        if (snapshot.executionVariablesByFeeCode == null)
        {
            snapshot.executionVariablesByFeeCode = new LinkedHashMap<>();
        }
        if (snapshot.conditionsByRuleCode == null)
        {
            snapshot.conditionsByRuleCode = new LinkedHashMap<>();
        }
        if (snapshot.tiersByRuleCode == null)
        {
            snapshot.tiersByRuleCode = new LinkedHashMap<>();
        }
        snapshot.fees.sort(Comparator.comparingInt(item -> item.sortNo == null ? 9999 : item.sortNo));
        snapshot.variables.sort(Comparator.comparingInt(item -> item.sortNo == null ? 9999 : item.sortNo));
        snapshot.feesByCode.clear();
        for (RuntimeFee fee : snapshot.fees)
        {
            snapshot.feesByCode.put(fee.feeCode, fee);
        }
        snapshot.variablesByCode.clear();
        for (RuntimeVariable variable : snapshot.variables)
        {
            snapshot.variablesByCode.put(variable.variableCode, variable);
        }
        snapshot.rulesByFeeCode.clear();
        for (RuntimeRule rule : snapshot.rulesByCode.values())
        {
            rule.conditions = snapshot.conditionsByRuleCode.getOrDefault(rule.ruleCode, Collections.emptyList()).stream()
                    .sorted(Comparator.comparingInt((RuntimeCondition item) -> item.groupNo == null ? 1 : item.groupNo)
                            .thenComparingInt(item -> item.sortNo == null ? 9999 : item.sortNo))
                    .collect(Collectors.toList());
            rule.tiers = snapshot.tiersByRuleCode.getOrDefault(rule.ruleCode, Collections.emptyList()).stream()
                    .sorted(Comparator.comparingInt(item -> item.tierNo == null ? 9999 : item.tierNo))
                    .collect(Collectors.toList());
            rule.conditionGroups = buildConditionGroups(rule.conditions);
            snapshot.rulesByFeeCode.computeIfAbsent(rule.feeCode, key -> new ArrayList<>()).add(rule);
        }
        for (List<RuntimeRule> rules : snapshot.rulesByFeeCode.values())
        {
            rules.sort(Comparator.comparingInt((RuntimeRule item) -> item.priority == null ? 0 : item.priority).reversed()
                    .thenComparingInt(item -> item.sortNo == null ? 9999 : item.sortNo));
        }
        snapshot.executionVariablesByFeeCode.clear();
        for (RuntimeFee fee : snapshot.fees)
        {
            snapshot.executionVariablesByFeeCode.put(fee.feeCode,
                    buildExecutionVariables(snapshot, snapshot.rulesByFeeCode.getOrDefault(fee.feeCode, Collections.emptyList())));
        }
        return snapshot;
    }

    private RuntimeSnapshot buildDraftRuntimeSnapshot(CostScene scene)
    {
        CostFeeItem feeQuery = new CostFeeItem();
        feeQuery.setSceneId(scene.getSceneId());
        feeQuery.setStatus(STATUS_ENABLED);
        List<CostFeeItem> feeItems = feeMapper.selectFeeOptions(feeQuery);
        Map<Long, CostFeeItem> feeById = feeItems.stream()
                .collect(Collectors.toMap(CostFeeItem::getFeeId, item -> item, (left, right) -> left, LinkedHashMap::new));

        CostVariable variableQuery = new CostVariable();
        variableQuery.setSceneId(scene.getSceneId());
        variableQuery.setStatus(STATUS_ENABLED);
        List<CostVariable> variables = variableMapper.selectVariableOptions(variableQuery);

        CostFormula formulaQuery = new CostFormula();
        formulaQuery.setSceneId(scene.getSceneId());
        formulaQuery.setStatus(STATUS_ENABLED);
        List<CostFormula> formulas = formulaMapper.selectFormulaOptions(formulaQuery);

        CostRule ruleQuery = new CostRule();
        ruleQuery.setSceneId(scene.getSceneId());
        ruleQuery.setStatus(STATUS_ENABLED);
        List<CostRule> rules = ruleMapper.selectRuleList(ruleQuery).stream()
                .filter(item -> feeById.containsKey(item.getFeeId()))
                .sorted(Comparator.comparing(CostRule::getRuleCode, Comparator.nullsLast(String::compareTo)))
                .collect(Collectors.toList());

        RuntimeSnapshot snapshot = new RuntimeSnapshot();
        snapshot.sceneId = scene.getSceneId();
        snapshot.sceneCode = scene.getSceneCode();
        snapshot.sceneName = scene.getSceneName();
        snapshot.versionId = null;
        snapshot.versionNo = DRAFT_VERSION_LABEL;
        snapshot.snapshotSource = SNAPSHOT_SOURCE_DRAFT;
        populateDraftFees(snapshot, feeItems);
        populateDraftVariables(snapshot, variables);
        populateDraftFormulas(snapshot, formulas);
        populateDraftRules(snapshot, rules);
        populateDraftConditions(snapshot, publishVersionMapper.selectRuleConditionsForPublish(scene.getSceneId()));
        populateDraftTiers(snapshot, publishVersionMapper.selectRuleTiersForPublish(scene.getSceneId()));
        return hydrateRuntimeSnapshot(snapshot);
    }

    private void populateDraftFees(RuntimeSnapshot snapshot, List<CostFeeItem> feeItems)
    {
        for (CostFeeItem fee : feeItems)
        {
            RuntimeFee runtimeFee = new RuntimeFee();
            runtimeFee.feeId = fee.getFeeId();
            runtimeFee.feeCode = fee.getFeeCode();
            runtimeFee.feeName = fee.getFeeName();
            runtimeFee.unitCode = fee.getUnitCode();
            runtimeFee.objectDimension = fee.getObjectDimension();
            runtimeFee.sortNo = fee.getSortNo();
            snapshot.fees.add(runtimeFee);
            snapshot.feesByCode.put(runtimeFee.feeCode, runtimeFee);
        }
    }

    private void populateDraftVariables(RuntimeSnapshot snapshot, List<CostVariable> variables)
    {
        for (CostVariable variable : variables)
        {
            RuntimeVariable runtimeVariable = new RuntimeVariable();
            runtimeVariable.variableCode = variable.getVariableCode();
            runtimeVariable.variableName = variable.getVariableName();
            runtimeVariable.sourceType = variable.getSourceType();
            runtimeVariable.dataType = variable.getDataType();
            runtimeVariable.dataPath = variable.getDataPath();
            runtimeVariable.formulaExpr = variable.getFormulaExpr();
            runtimeVariable.formulaCode = variable.getFormulaCode();
            runtimeVariable.defaultValue = variable.getDefaultValue();
            runtimeVariable.sortNo = variable.getSortNo();
            snapshot.variables.add(runtimeVariable);
            snapshot.variablesByCode.put(runtimeVariable.variableCode, runtimeVariable);
        }
    }

    private void populateDraftFormulas(RuntimeSnapshot snapshot, List<CostFormula> formulas)
    {
        for (CostFormula formula : formulas)
        {
            RuntimeFormula runtimeFormula = new RuntimeFormula();
            runtimeFormula.formulaCode = formula.getFormulaCode();
            runtimeFormula.formulaName = formula.getFormulaName();
            runtimeFormula.businessFormula = formula.getBusinessFormula();
            runtimeFormula.formulaExpr = formula.getFormulaExpr();
            runtimeFormula.returnType = formula.getReturnType();
            snapshot.formulasByCode.put(runtimeFormula.formulaCode, runtimeFormula);
        }
    }

    private void populateDraftRules(RuntimeSnapshot snapshot, List<CostRule> rules)
    {
        for (CostRule rule : rules)
        {
            RuntimeRule runtimeRule = new RuntimeRule();
            runtimeRule.ruleId = rule.getRuleId();
            runtimeRule.feeCode = rule.getFeeCode();
            runtimeRule.ruleCode = rule.getRuleCode();
            runtimeRule.ruleName = rule.getRuleName();
            runtimeRule.ruleType = rule.getRuleType();
            runtimeRule.conditionLogic = firstNonBlank(rule.getConditionLogic(), "AND");
            runtimeRule.priority = rule.getPriority();
            runtimeRule.quantityVariableCode = rule.getQuantityVariableCode();
            runtimeRule.pricingMode = firstNonBlank(rule.getPricingMode(), "TYPED");
            runtimeRule.pricingConfig = parseJsonMap(rule.getPricingJson());
            runtimeRule.amountFormula = rule.getAmountFormula();
            runtimeRule.amountFormulaCode = rule.getAmountFormulaCode();
            runtimeRule.amountBusinessFormula = rule.getAmountBusinessFormula();
            runtimeRule.sortNo = rule.getSortNo();
            snapshot.rulesByCode.put(runtimeRule.ruleCode, runtimeRule);
        }
    }

    private void populateDraftConditions(RuntimeSnapshot snapshot, List<Map<String, Object>> rows)
    {
        for (Map<String, Object> row : rows)
        {
            String ruleCode = stringValue(row.get("ruleCode"));
            if (!snapshot.rulesByCode.containsKey(ruleCode))
            {
                continue;
            }
            RuntimeCondition condition = new RuntimeCondition();
            condition.ruleCode = ruleCode;
            condition.groupNo = intValue(row.get("groupNo"));
            condition.sortNo = intValue(row.get("sortNo"));
            condition.variableCode = stringValue(row.get("variableCode"));
            condition.displayName = firstNonBlank(stringValue(row.get("displayName")), condition.variableCode);
            condition.operatorCode = stringValue(row.get("operatorCode"));
            condition.compareValue = stringValue(row.get("compareValue"));
            snapshot.conditionsByRuleCode.computeIfAbsent(ruleCode, key -> new ArrayList<>()).add(condition);
        }
    }

    private void populateDraftTiers(RuntimeSnapshot snapshot, List<Map<String, Object>> rows)
    {
        for (Map<String, Object> row : rows)
        {
            String ruleCode = stringValue(row.get("ruleCode"));
            if (!snapshot.rulesByCode.containsKey(ruleCode))
            {
                continue;
            }
            RuntimeTier tier = new RuntimeTier();
            tier.tierId = longValue(row.get("tierId"));
            tier.ruleCode = ruleCode;
            tier.tierNo = intValue(row.get("tierNo"));
            tier.startValue = toBigDecimal(row.get("startValue"));
            tier.endValue = toBigDecimal(row.get("endValue"));
            tier.rateValue = toBigDecimal(row.get("rateValue"));
            tier.intervalMode = firstNonBlank(stringValue(row.get("intervalMode")), INTERVAL_LCRO);
            snapshot.tiersByRuleCode.computeIfAbsent(ruleCode, key -> new ArrayList<>()).add(tier);
        }
    }

    private String buildInputTemplateMessage(RuntimeSnapshot snapshot)
    {
        if (isDraftSnapshot(snapshot))
        {
            return "当前场景尚无生效版本，已按现有配置生成输入模板；公式变量不需要手工输入，其余变量优先按 dataPath/变量编码生成命名空间结构。";
        }
        return "已按发布快照生成输入模板；公式变量不需要手工输入，其余变量优先按 dataPath/变量编码生成命名空间结构。";
    }

    private String buildFeeTemplateMessage(RuntimeSnapshot snapshot, boolean noRule)
    {
        if (noRule)
        {
            return isDraftSnapshot(snapshot)
                    ? "当前费用在现有配置下未挂载可用规则，已返回空模板。"
                    : "当前费用在该发布版本下未挂载可用规则，已返回空模板。";
        }
        return isDraftSnapshot(snapshot)
                ? "已按当前配置和费用关联规则生成接入模板，三方系统只需组装 includedInTemplate=true 的字段。"
                : "已按发布快照和费用关联规则生成接入模板，三方系统只需组装 includedInTemplate=true 的字段。";
    }

    private boolean isSimulationTaskType(String taskType)
    {
        return "SIMULATION".equals(taskType) || TASK_TYPE_SIMULATION_BATCH.equals(taskType);
    }

    private boolean isDraftSnapshot(RuntimeSnapshot snapshot)
    {
        return snapshot != null && SNAPSHOT_SOURCE_DRAFT.equals(snapshot.snapshotSource);
    }

    private List<RuntimeConditionGroup> buildConditionGroups(List<RuntimeCondition> conditions)
    {
        if (conditions == null || conditions.isEmpty())
        {
            return Collections.emptyList();
        }
        Map<Integer, List<RuntimeCondition>> grouped = conditions.stream()
                .collect(Collectors.groupingBy(item -> item.groupNo, LinkedHashMap::new, Collectors.toList()));
        List<RuntimeConditionGroup> result = new ArrayList<>();
        for (Map.Entry<Integer, List<RuntimeCondition>> entry : grouped.entrySet())
        {
            RuntimeConditionGroup group = new RuntimeConditionGroup();
            group.groupNo = entry.getKey();
            group.conditions = entry.getValue();
            result.add(group);
        }
        result.sort(Comparator.comparingInt(item -> item.groupNo == null ? 1 : item.groupNo));
        return result;
    }

    private Map<String, Object> buildSingleInputTemplate(RuntimeSnapshot snapshot, String taskType, int index)
    {
        LinkedHashMap<String, Object> template = new LinkedHashMap<>();
        template.put("bizNo", buildTemplateBizNo(taskType, index));
        template.put("objectCode", "OBJ-" + PARTITION_FORMAT.format(index));
        template.put("objectName", "示例对象" + index);
        Set<String> populatedPaths = new LinkedHashSet<>();
        for (RuntimeVariable variable : snapshot.variables)
        {
            if (SOURCE_TYPE_FORMULA.equals(variable.sourceType))
            {
                continue;
            }
            String path = firstNonBlank(variable.dataPath, variable.variableCode);
            if (StringUtils.isEmpty(path) || populatedPaths.contains(path))
            {
                continue;
            }
            populatePathValue(template, path, buildTemplateValue(variable, index));
            populatedPaths.add(path);
        }
        return template;
    }

    private Map<String, Object> buildSelectedInputTemplate(List<RuntimeVariable> variables, String taskType, int index)
    {
        LinkedHashMap<String, Object> template = new LinkedHashMap<>();
        template.put("bizNo", buildTemplateBizNo(taskType, index));
        template.put("objectCode", "OBJ-" + PARTITION_FORMAT.format(index));
        template.put("objectName", "示例对象" + index);
        Set<String> populatedPaths = new LinkedHashSet<>();
        for (RuntimeVariable variable : variables)
        {
            if (SOURCE_TYPE_FORMULA.equals(variable.sourceType))
            {
                continue;
            }
            String path = firstNonBlank(variable.dataPath, variable.variableCode);
            if (StringUtils.isEmpty(path) || populatedPaths.contains(path))
            {
                continue;
            }
            populatePathValue(template, path, buildTemplateValue(variable, index));
            populatedPaths.add(path);
        }
        return template;
    }

    private String buildTemplateBizNo(String taskType, int index)
    {
        String prefix;
        if (TASK_TYPE_FORMAL_BATCH.equals(taskType))
        {
            prefix = "BATCH";
        }
        else if (TASK_TYPE_FORMAL_SINGLE.equals(taskType))
        {
            prefix = "FORMAL";
        }
        else
        {
            prefix = "SIM";
        }
        return prefix + "-" + PARTITION_FORMAT.format(index);
    }

    private Object buildTemplateValue(RuntimeVariable variable, int index)
    {
        String dataType = StringUtils.isEmpty(variable.dataType) ? "" : variable.dataType.toUpperCase(Locale.ROOT);
        if (DATA_TYPE_NUMBER.equals(dataType))
        {
            return BigDecimal.ONE;
        }
        if (DATA_TYPE_BOOLEAN.equals(dataType))
        {
            return Boolean.TRUE;
        }
        if (DATA_TYPE_JSON.equals(dataType))
        {
            return new LinkedHashMap<>();
        }
        return firstNonBlank(variable.variableName, variable.variableCode) + index;
    }

    private void populatePathValue(Map<String, Object> root, String path, Object value)
    {
        String[] pieces = path.split("\\.");
        Map<String, Object> current = root;
        for (int i = 0; i < pieces.length; i++)
        {
            String piece = pieces[i];
            if (i == pieces.length - 1)
            {
                current.putIfAbsent(piece, value);
                return;
            }
            Object child = current.get(piece);
            if (!(child instanceof Map))
            {
                LinkedHashMap<String, Object> next = new LinkedHashMap<>();
                current.put(piece, next);
                current = next;
                continue;
            }
            current = castMap(child);
        }
    }

    private List<CostCalcTask> selectTaskListInternal(CostCalcTask query)
    {
        return calcTaskMapper.selectList(Wrappers.<CostCalcTask>lambdaQuery()
                .eq(query.getTaskId() != null, CostCalcTask::getTaskId, query.getTaskId())
                .eq(query.getSceneId() != null, CostCalcTask::getSceneId, query.getSceneId())
                .eq(query.getVersionId() != null, CostCalcTask::getVersionId, query.getVersionId())
                .eq(StringUtils.isNotEmpty(query.getTaskType()), CostCalcTask::getTaskType, query.getTaskType())
                .eq(StringUtils.isNotEmpty(query.getTaskStatus()), CostCalcTask::getTaskStatus, query.getTaskStatus())
                .eq(StringUtils.isNotEmpty(query.getBillMonth()), CostCalcTask::getBillMonth, query.getBillMonth())
                .eq(StringUtils.isNotEmpty(query.getRequestNo()), CostCalcTask::getRequestNo, query.getRequestNo())
                .like(StringUtils.isNotEmpty(query.getTaskNo()), CostCalcTask::getTaskNo, query.getTaskNo())
                .orderByDesc(CostCalcTask::getTaskId));
    }

    private List<CostResultLedger> selectResultListInternal(CostResultLedger query)
    {
        List<Long> requestTaskIds = resolveResultRequestTaskIds(query.getRequestNo());
        if (StringUtils.isNotEmpty(query.getRequestNo()) && requestTaskIds.isEmpty())
        {
            return Collections.emptyList();
        }
        return resultLedgerMapper.selectList(Wrappers.<CostResultLedger>lambdaQuery()
                .eq(query.getTaskId() != null, CostResultLedger::getTaskId, query.getTaskId())
                .in(requestTaskIds != null && !requestTaskIds.isEmpty(), CostResultLedger::getTaskId, requestTaskIds)
                .eq(query.getSceneId() != null, CostResultLedger::getSceneId, query.getSceneId())
                .eq(query.getVersionId() != null, CostResultLedger::getVersionId, query.getVersionId())
                .eq(query.getFeeId() != null, CostResultLedger::getFeeId, query.getFeeId())
                .eq(StringUtils.isNotEmpty(query.getBillMonth()), CostResultLedger::getBillMonth, query.getBillMonth())
                .eq(StringUtils.isNotEmpty(query.getTaskNo()), CostResultLedger::getTaskNo, query.getTaskNo())
                .eq(StringUtils.isNotEmpty(query.getFeeCode()), CostResultLedger::getFeeCode, query.getFeeCode())
                .eq(StringUtils.isNotEmpty(query.getObjectCode()), CostResultLedger::getObjectCode, query.getObjectCode())
                .like(StringUtils.isNotEmpty(query.getObjectName()), CostResultLedger::getObjectName, query.getObjectName())
                .eq(StringUtils.isNotEmpty(query.getBizNo()), CostResultLedger::getBizNo, query.getBizNo())
                .eq(StringUtils.isNotEmpty(query.getResultStatus()), CostResultLedger::getResultStatus, query.getResultStatus())
                .orderByDesc(CostResultLedger::getResultId));
    }

    private List<Long> resolveResultRequestTaskIds(String requestNo)
    {
        if (StringUtils.isEmpty(requestNo))
        {
            return Collections.emptyList();
        }
        return calcTaskMapper.selectList(Wrappers.<CostCalcTask>lambdaQuery()
                        .eq(CostCalcTask::getRequestNo, requestNo)
                        .orderByDesc(CostCalcTask::getTaskId))
                .stream()
                .map(CostCalcTask::getTaskId)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    private Map<Long, CostPublishVersion> selectVersionMap(Set<Long> versionIds)
    {
        if (versionIds == null || versionIds.isEmpty())
        {
            return Collections.emptyMap();
        }
        return publishVersionMapper.selectBatchIds(versionIds).stream()
                .collect(Collectors.toMap(CostPublishVersion::getVersionId, item -> item));
    }

    private void enrichSimulationRecords(List<CostSimulationRecord> records)
    {
        if (records == null || records.isEmpty())
        {
            return;
        }
        Map<Long, CostScene> sceneMap = sceneMapper.selectBatchIds(records.stream().map(CostSimulationRecord::getSceneId).filter(Objects::nonNull).collect(Collectors.toSet()))
                .stream().collect(Collectors.toMap(CostScene::getSceneId, item -> item));
        Map<Long, CostPublishVersion> versionMap = selectVersionMap(records.stream()
                .map(CostSimulationRecord::getVersionId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet()));
        for (CostSimulationRecord record : records)
        {
            CostScene scene = sceneMap.get(record.getSceneId());
            if (scene != null)
            {
                record.setSceneCode(scene.getSceneCode());
                record.setSceneName(scene.getSceneName());
            }
            CostPublishVersion version = versionMap.get(record.getVersionId());
            if (version != null)
            {
                record.setVersionNo(version.getVersionNo());
            }
            else if (record.getVersionId() == null)
            {
                record.setVersionNo(DRAFT_VERSION_LABEL);
            }
        }
    }

    private void enrichTasks(List<CostCalcTask> tasks)
    {
        if (tasks == null || tasks.isEmpty())
        {
            return;
        }
        Map<Long, CostScene> sceneMap = sceneMapper.selectBatchIds(tasks.stream().map(CostCalcTask::getSceneId).filter(Objects::nonNull).collect(Collectors.toSet()))
                .stream().collect(Collectors.toMap(CostScene::getSceneId, item -> item));
        Map<Long, CostPublishVersion> versionMap = selectVersionMap(tasks.stream()
                .map(CostCalcTask::getVersionId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet()));
        for (CostCalcTask task : tasks)
        {
            CostScene scene = sceneMap.get(task.getSceneId());
            if (scene != null)
            {
                task.setSceneCode(scene.getSceneCode());
                task.setSceneName(scene.getSceneName());
            }
            CostPublishVersion version = versionMap.get(task.getVersionId());
            if (version != null)
            {
                task.setVersionNo(version.getVersionNo());
            }
        }
    }

    private TaskExecutionSummary summarizeTaskDetails(Long taskId)
    {
        List<CostCalcTaskDetail> details = calcTaskDetailMapper.selectList(Wrappers.<CostCalcTaskDetail>lambdaQuery()
                .select(CostCalcTaskDetail::getDetailStatus)
                .eq(CostCalcTaskDetail::getTaskId, taskId));
        TaskExecutionSummary summary = new TaskExecutionSummary();
        summary.totalCount = details.size();
        for (CostCalcTaskDetail detail : details)
        {
            if (DETAIL_STATUS_SUCCESS.equals(detail.getDetailStatus()))
            {
                summary.successCount++;
            }
            else if (DETAIL_STATUS_FAILED.equals(detail.getDetailStatus()))
            {
                summary.failedCount++;
            }
        }
        summary.processedCount = summary.successCount + summary.failedCount;
        return summary;
    }

    private PartitionExecutionResult summarizePartitionDetails(Long taskId, Integer partitionNo)
    {
        List<CostCalcTaskDetail> details = calcTaskDetailMapper.selectList(Wrappers.<CostCalcTaskDetail>lambdaQuery()
                .select(CostCalcTaskDetail::getDetailStatus)
                .eq(CostCalcTaskDetail::getTaskId, taskId)
                .eq(CostCalcTaskDetail::getPartitionNo, partitionNo));
        PartitionExecutionResult summary = new PartitionExecutionResult();
        for (CostCalcTaskDetail detail : details)
        {
            if (DETAIL_STATUS_SUCCESS.equals(detail.getDetailStatus()))
            {
                summary.successCount++;
            }
            else if (DETAIL_STATUS_FAILED.equals(detail.getDetailStatus()))
            {
                summary.failedCount++;
            }
        }
        summary.processedCount = summary.successCount + summary.failedCount;
        return summary;
    }

    /**
     * 按任务集合一次性加载分片台账，避免任务总览按任务逐个查分片。
     */
    private List<CostCalcTaskPartition> selectTaskPartitions(List<CostCalcTask> tasks)
    {
        if (tasks == null || tasks.isEmpty())
        {
            return Collections.emptyList();
        }
        Set<Long> taskIds = tasks.stream().map(CostCalcTask::getTaskId).filter(Objects::nonNull).collect(Collectors.toSet());
        if (taskIds.isEmpty())
        {
            return Collections.emptyList();
        }
        return calcTaskPartitionMapper.selectList(Wrappers.<CostCalcTaskPartition>lambdaQuery()
                .in(CostCalcTaskPartition::getTaskId, taskIds)
                .orderByDesc(CostCalcTaskPartition::getPartitionId));
    }

    /**
     * 构建最近 N 天任务趋势，帮助从任务级别观察运行波动。
     */
    private List<Map<String, Object>> buildTaskTrend(List<CostCalcTask> tasks, int recentDays)
    {
        ZoneId zoneId = ZoneId.systemDefault();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        Map<LocalDate, List<CostCalcTask>> grouped = tasks.stream()
                .filter(Objects::nonNull)
                .filter(item -> resolveTaskTrendDate(item) != null)
                .collect(Collectors.groupingBy(item -> resolveTaskTrendDate(item).toInstant().atZone(zoneId).toLocalDate()));
        List<Map<String, Object>> result = new ArrayList<>();
        LocalDate end = LocalDate.now(zoneId);
        LocalDate start = end.minusDays(Math.max(recentDays - 1L, 0L));
        for (LocalDate date = start; !date.isAfter(end); date = date.plusDays(1))
        {
            List<CostCalcTask> dayTasks = grouped.getOrDefault(date, List.of());
            LinkedHashMap<String, Object> row = new LinkedHashMap<>();
            row.put("date", date.format(formatter));
            row.put("count", dayTasks.size());
            row.put("runningCount", dayTasks.stream().filter(item -> TASK_STATUS_RUNNING.equals(item.getTaskStatus())).count());
            row.put("failedCount", dayTasks.stream().filter(item -> isTaskProblematic(item.getTaskStatus())).count());
            result.add(row);
        }
        return result;
    }

    /**
     * 构建最近 N 天分片趋势，便于判断是否存在分片级失败集中爆发。
     */
    private List<Map<String, Object>> buildPartitionTrend(List<CostCalcTaskPartition> partitions, int recentDays)
    {
        ZoneId zoneId = ZoneId.systemDefault();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        Map<LocalDate, List<CostCalcTaskPartition>> grouped = partitions.stream()
                .filter(Objects::nonNull)
                .filter(item -> resolvePartitionTrendDate(item) != null)
                .collect(Collectors.groupingBy(item -> resolvePartitionTrendDate(item).toInstant().atZone(zoneId).toLocalDate()));
        List<Map<String, Object>> result = new ArrayList<>();
        LocalDate end = LocalDate.now(zoneId);
        LocalDate start = end.minusDays(Math.max(recentDays - 1L, 0L));
        for (LocalDate date = start; !date.isAfter(end); date = date.plusDays(1))
        {
            List<CostCalcTaskPartition> dayPartitions = grouped.getOrDefault(date, List.of());
            LinkedHashMap<String, Object> row = new LinkedHashMap<>();
            row.put("date", date.format(formatter));
            row.put("count", dayPartitions.size());
            row.put("failedCount", dayPartitions.stream().filter(item -> isPartitionProblematic(item)).count());
            row.put("avgDurationMs", dayPartitions.isEmpty() ? 0L : Math.round(dayPartitions.stream()
                    .map(CostCalcTaskPartition::getDurationMs)
                    .filter(Objects::nonNull)
                    .mapToLong(Long::longValue)
                    .average()
                    .orElse(0D)));
            result.add(row);
        }
        return result;
    }

    /**
     * 构建高风险任务排行，优先暴露失败量高、失败分片多的任务。
     */
    private List<Map<String, Object>> buildTopRiskTasks(List<CostCalcTask> tasks, List<CostCalcTaskPartition> partitions, int limit)
    {
        Map<Long, List<CostCalcTaskPartition>> partitionMap = partitions.stream()
                .filter(item -> item.getTaskId() != null)
                .collect(Collectors.groupingBy(CostCalcTaskPartition::getTaskId));
        return tasks.stream()
                .filter(item -> item.getTaskId() != null)
                .filter(item -> NumberUtils.toInt(String.valueOf(item.getFailCount()), 0) > 0 || isTaskProblematic(item.getTaskStatus()))
                .sorted(Comparator
                        .comparingInt((CostCalcTask item) -> item.getFailCount() == null ? 0 : item.getFailCount()).reversed()
                        .thenComparing(CostCalcTask::getTaskId, Comparator.reverseOrder()))
                .limit(limit)
                .map(task -> {
                    List<CostCalcTaskPartition> taskPartitions = partitionMap.getOrDefault(task.getTaskId(), List.of());
                    LinkedHashMap<String, Object> row = new LinkedHashMap<>();
                    row.put("taskId", task.getTaskId());
                    row.put("taskNo", task.getTaskNo());
                    row.put("sceneName", firstNonBlank(task.getSceneName(), "-"));
                    row.put("billMonth", firstNonBlank(task.getBillMonth(), "-"));
                    row.put("taskStatus", firstNonBlank(task.getTaskStatus(), TASK_STATUS_INIT));
                    row.put("failCount", task.getFailCount() == null ? 0 : task.getFailCount());
                    row.put("partitionFailCount", taskPartitions.stream().filter(this::isPartitionProblematic).count());
                    row.put("sourceCount", task.getSourceCount() == null ? 0 : task.getSourceCount());
                    return row;
                })
                .collect(Collectors.toList());
    }

    /**
     * 构建任务状态分布，帮助快速判断任务更多停留在哪个阶段。
     */
    private List<Map<String, Object>> buildTaskStatusDistribution(List<CostCalcTask> tasks)
    {
        return tasks.stream()
                .collect(Collectors.groupingBy(item -> firstNonBlank(item.getTaskStatus(), TASK_STATUS_INIT), Collectors.counting()))
                .entrySet()
                .stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .map(entry -> {
                    LinkedHashMap<String, Object> row = new LinkedHashMap<>();
                    row.put("taskStatus", entry.getKey());
                    row.put("count", entry.getValue());
                    return row;
                })
                .collect(Collectors.toList());
    }

    /**
     * 构建输入来源分布，帮助判断当前正式核算更偏 JSON 还是导入批次。
     */
    private List<Map<String, Object>> buildInputSourceDistribution(List<CostCalcTask> tasks)
    {
        return tasks.stream()
                .collect(Collectors.groupingBy(item -> firstNonBlank(item.getInputSourceType(), "INLINE_JSON"), Collectors.counting()))
                .entrySet()
                .stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .map(entry -> {
                    LinkedHashMap<String, Object> row = new LinkedHashMap<>();
                    row.put("inputSourceType", entry.getKey());
                    row.put("count", entry.getValue());
                    return row;
                })
                .collect(Collectors.toList());
    }

    private Date resolveTaskTrendDate(CostCalcTask task)
    {
        return task.getStartedTime() != null ? task.getStartedTime() : task.getCreateTime();
    }

    private Date resolvePartitionTrendDate(CostCalcTaskPartition partition)
    {
        return partition.getStartedTime() != null ? partition.getStartedTime() : partition.getCreateTime();
    }

    private boolean isTaskProblematic(String taskStatus)
    {
        return TASK_STATUS_FAILED.equals(taskStatus) || TASK_STATUS_PART_SUCCESS.equals(taskStatus) || TASK_STATUS_CANCELLED.equals(taskStatus);
    }

    private boolean isPartitionProblematic(CostCalcTaskPartition partition)
    {
        return partition != null
                && (TASK_STATUS_FAILED.equals(partition.getPartitionStatus())
                || TASK_STATUS_PART_SUCCESS.equals(partition.getPartitionStatus())
                || (partition.getFailCount() != null && partition.getFailCount() > 0));
    }

    private void enrichInputBatches(List<CostCalcInputBatch> batches)
    {
        if (batches == null || batches.isEmpty())
        {
            return;
        }
        List<CostCalcInputBatch> filtered = batches.stream().filter(Objects::nonNull).collect(Collectors.toList());
        if (filtered.isEmpty())
        {
            return;
        }
        Map<Long, CostScene> sceneMap = sceneMapper.selectBatchIds(filtered.stream().map(CostCalcInputBatch::getSceneId).filter(Objects::nonNull).collect(Collectors.toSet()))
                .stream().collect(Collectors.toMap(CostScene::getSceneId, item -> item));
        Map<Long, CostPublishVersion> versionMap = selectVersionMap(filtered.stream()
                .map(CostCalcInputBatch::getVersionId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet()));
        for (CostCalcInputBatch batch : filtered)
        {
            CostScene scene = sceneMap.get(batch.getSceneId());
            if (scene != null)
            {
                batch.setSceneCode(scene.getSceneCode());
                batch.setSceneName(scene.getSceneName());
            }
            CostPublishVersion version = versionMap.get(batch.getVersionId());
            if (version != null)
            {
                batch.setVersionNo(version.getVersionNo());
            }
        }
    }

    private void enrichResults(List<CostResultLedger> results)
    {
        if (results == null || results.isEmpty())
        {
            return;
        }
        Map<Long, CostScene> sceneMap = sceneMapper.selectBatchIds(results.stream().map(CostResultLedger::getSceneId).filter(Objects::nonNull).collect(Collectors.toSet()))
                .stream().collect(Collectors.toMap(CostScene::getSceneId, item -> item));
        Map<Long, CostPublishVersion> versionMap = selectVersionMap(results.stream()
                .map(CostResultLedger::getVersionId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet()));
        Map<Long, CostFeeItem> feeMap = feeMapper.selectBatchIds(results.stream().map(CostResultLedger::getFeeId).filter(Objects::nonNull).collect(Collectors.toSet()))
                .stream().collect(Collectors.toMap(CostFeeItem::getFeeId, item -> item));
        Map<Long, CostResultTrace> traceMap = resultTraceMapper.selectBatchIds(results.stream().map(CostResultLedger::getTraceId).filter(Objects::nonNull).collect(Collectors.toSet()))
                .stream().collect(Collectors.toMap(CostResultTrace::getTraceId, item -> item));
        for (CostResultLedger result : results)
        {
            CostScene scene = sceneMap.get(result.getSceneId());
            if (scene != null)
            {
                result.setSceneCode(scene.getSceneCode());
                result.setSceneName(scene.getSceneName());
            }
            CostPublishVersion version = versionMap.get(result.getVersionId());
            if (version != null)
            {
                result.setVersionNo(version.getVersionNo());
            }
            CostFeeItem fee = feeMap.get(result.getFeeId());
            if (fee != null)
            {
                result.setUnitCode(fee.getUnitCode());
            }
            CostResultTrace trace = traceMap.get(result.getTraceId());
            if (trace != null)
            {
                Map<String, Object> pricing = parseJsonMap(trace.getPricingJson());
                result.setMatchedGroupNo(intValue(pricing.get("matchedGroupNo")));
                result.setPricingMode(stringValue(pricing.get("pricingMode")));
                result.setPricingSource(stringValue(pricing.get("pricingSource")));
            }
        }
    }

    private List<Map<String, Object>> parseTaskInput(CostCalcTaskSubmitBo bo)
    {
        if (INPUT_SOURCE_BATCH.equals(resolveInputSourceType(bo)))
        {
            return loadInputBatchItems(bo);
        }
        if (TASK_TYPE_FORMAL_SINGLE.equals(bo.getTaskType()))
        {
            return Collections.singletonList(parseObjectJson(bo.getInputJson(), "单笔正式核算输入必须是 JSON 对象"));
        }
        if (TASK_TYPE_FORMAL_BATCH.equals(bo.getTaskType()))
        {
            List<Map<String, Object>> inputs = parseArrayJson(bo.getInputJson(), "批量任务输入必须是 JSON 数组");
            if (inputs.isEmpty())
            {
                throw new ServiceException("批量任务输入不能为空数组");
            }
            validateDuplicateBizNo(inputs);
            return inputs;
        }
        throw new ServiceException("暂不支持的任务类型：" + bo.getTaskType());
    }

    private String resolveInputSourceType(CostCalcTaskSubmitBo bo)
    {
        if (StringUtils.isNotEmpty(bo.getInputSourceType()))
        {
            return bo.getInputSourceType().trim().toUpperCase(Locale.ROOT);
        }
        return StringUtils.isNotEmpty(bo.getSourceBatchNo()) ? INPUT_SOURCE_BATCH : INPUT_SOURCE_INLINE_JSON;
    }

    private List<Map<String, Object>> loadInputBatchItems(CostCalcTaskSubmitBo bo)
    {
        if (StringUtils.isEmpty(bo.getSourceBatchNo()))
        {
            throw new ServiceException("批次导入任务缺少来源批次号");
        }
        CostCalcInputBatch batch = calcInputBatchMapper.selectOne(Wrappers.<CostCalcInputBatch>lambdaQuery()
                .eq(CostCalcInputBatch::getBatchNo, bo.getSourceBatchNo())
                .last("limit 1"));
        if (batch == null)
        {
            throw new ServiceException("来源输入批次不存在，请刷新后重试");
        }
        if (!Objects.equals(batch.getSceneId(), bo.getSceneId()))
        {
            throw new ServiceException("来源输入批次与当前场景不匹配");
        }
        if (StringUtils.isNotEmpty(bo.getBillMonth()) && !Objects.equals(batch.getBillMonth(), bo.getBillMonth()))
        {
            throw new ServiceException("来源输入批次与当前账期不匹配");
        }
        List<CostCalcInputBatchItem> items = calcInputBatchItemMapper.selectList(Wrappers.<CostCalcInputBatchItem>lambdaQuery()
                .eq(CostCalcInputBatchItem::getBatchId, batch.getBatchId())
                .orderByAsc(CostCalcInputBatchItem::getItemNo)
                .orderByAsc(CostCalcInputBatchItem::getItemId));
        if (items.isEmpty())
        {
            throw new ServiceException("来源输入批次没有可用明细");
        }
        List<Map<String, Object>> inputs = new ArrayList<>();
        for (CostCalcInputBatchItem item : items)
        {
            inputs.add(parseObjectJson(item.getInputJson(), "输入批次明细必须是 JSON 对象"));
        }
        validateDuplicateBizNo(inputs);
        return inputs;
    }

    private void markInputBatchSubmitted(String sourceBatchNo, String operator)
    {
        if (StringUtils.isEmpty(sourceBatchNo))
        {
            return;
        }
        calcInputBatchMapper.update(null, Wrappers.<CostCalcInputBatch>lambdaUpdate()
                .eq(CostCalcInputBatch::getBatchNo, sourceBatchNo)
                .set(CostCalcInputBatch::getBatchStatus, INPUT_BATCH_STATUS_SUBMITTED)
                .set(CostCalcInputBatch::getUpdateBy, operator)
                .set(CostCalcInputBatch::getUpdateTime, DateUtils.getNowDate()));
        calcInputBatchItemMapper.update(null, Wrappers.<CostCalcInputBatchItem>lambdaUpdate()
                .eq(CostCalcInputBatchItem::getBatchNo, sourceBatchNo)
                .set(CostCalcInputBatchItem::getItemStatus, INPUT_BATCH_STATUS_CONSUMED)
                .set(CostCalcInputBatchItem::getUpdateTime, DateUtils.getNowDate()));
    }

    private void validateDuplicateBizNo(List<Map<String, Object>> inputs)
    {
        Set<String> seen = new LinkedHashSet<>();
        for (int i = 0; i < inputs.size(); i++)
        {
            String bizNo = resolveBizNo(inputs.get(i), i + 1);
            if (!seen.add(bizNo))
            {
                throw new ServiceException("批量任务存在重复业务单号：" + bizNo);
            }
        }
    }

    private List<Map<String, Object>> parseInlineCalculationInputs(String inputJson)
    {
        Object parsed = parseJsonToObject(inputJson);
        if (parsed instanceof Map)
        {
            return Collections.singletonList(castMap(parsed));
        }
        if (parsed instanceof List)
        {
            List<Map<String, Object>> inputs = parseArrayJson(inputJson, "费用计算输入必须是 JSON 对象或对象数组");
            if (inputs.isEmpty())
            {
                throw new ServiceException("费用计算输入不能为空数组");
            }
            validateDuplicateBizNo(inputs);
            return inputs;
        }
        throw new ServiceException("费用计算输入必须是 JSON 对象或对象数组");
    }

    private void validateBillMonth(String billMonth)
    {
        if (!billMonth.matches("\\d{4}-\\d{2}"))
        {
            throw new ServiceException("账期格式必须为 yyyy-MM");
        }
    }

    private Map<String, Object> buildFeeCalculationRecord(Map<String, Object> input, RuntimeFee fee,
            ExecutionResult executionResult, int index, boolean includeExplain, long durationMs)
    {
        FeeExecutionResult feeResult = findFeeExecutionResult(executionResult, fee.feeCode);
        LinkedHashMap<String, Object> item = new LinkedHashMap<>();
        String bizNo = resolveBizNo(input, index);
        item.put("recordIndex", index);
        item.put("bizNo", bizNo);
        item.put("objectCode", firstNonBlank(resolveString(input, "objectCode", "object_code"), bizNo));
        item.put("objectName", resolveString(input, "objectName", "object_name", "name"));
        item.put("feeCode", fee.feeCode);
        item.put("feeName", fee.feeName);
        item.put("status", feeResult == null ? "NO_MATCH" : "SUCCESS");
        item.put("matched", feeResult != null);
        item.put("ruleCode", feeResult == null ? "" : feeResult.ruleCode);
        item.put("ruleName", feeResult == null ? "" : feeResult.ruleName);
        item.put("quantityValue", feeResult == null ? null : feeResult.quantityValue);
        item.put("unitPrice", feeResult == null ? null : feeResult.unitPrice);
        item.put("amountValue", feeResult == null ? null : feeResult.amountValue);
        item.put("matchedGroupNo", feeResult == null ? null : feeResult.pricingExplain.get("matchedGroupNo"));
        item.put("pricingMode", feeResult == null ? null : feeResult.pricingExplain.get("pricingMode"));
        item.put("pricingSource", feeResult == null ? null : feeResult.pricingExplain.get("pricingSource"));
        item.put("tierNo", feeResult == null ? null : feeResult.pricingExplain.get("tierNo"));
        item.put("tierRange", feeResult == null ? null : feeResult.pricingExplain.get("tierRange"));
        item.put("durationMs", durationMs);
        item.put("errorMessage", "");
        if (includeExplain)
        {
            item.put("explain", feeResult == null
                    ? executionResult.skippedFeeExplains.getOrDefault(fee.feeCode,
                    buildFeeNoMatchExplain(fee, Collections.emptyList(), executionResult.variableView, Collections.emptyList()))
                    : buildFeeCalculationExplain(feeResult));
        }
        return item;
    }

    private FeeExecutionResult findFeeExecutionResult(ExecutionResult executionResult, String feeCode)
    {
        if (executionResult == null || executionResult.feeResults == null || StringUtils.isEmpty(feeCode))
        {
            return null;
        }
        return executionResult.feeResults.stream()
                .filter(item -> StringUtils.equals(item.feeCode, feeCode))
                .findFirst()
                .orElse(null);
    }

    private Map<String, Object> buildFeeCalculationFailureRecord(Map<String, Object> input, RuntimeFee fee,
            int index, Exception exception, long durationMs, boolean includeExplain)
    {
        LinkedHashMap<String, Object> item = new LinkedHashMap<>();
        String bizNo = resolveBizNo(input, index);
        item.put("recordIndex", index);
        item.put("bizNo", bizNo);
        item.put("objectCode", firstNonBlank(resolveString(input, "objectCode", "object_code"), bizNo));
        item.put("objectName", resolveString(input, "objectName", "object_name", "name"));
        item.put("feeCode", fee.feeCode);
        item.put("feeName", fee.feeName);
        item.put("status", "FAILED");
        item.put("matched", false);
        item.put("ruleCode", "");
        item.put("ruleName", "");
        item.put("quantityValue", null);
        item.put("unitPrice", null);
        item.put("amountValue", null);
        item.put("matchedGroupNo", null);
        item.put("pricingMode", null);
        item.put("pricingSource", null);
        item.put("tierNo", null);
        item.put("tierRange", null);
        item.put("durationMs", durationMs);
        item.put("errorMessage", limitLength(exception.getMessage(), 1000));
        if (includeExplain)
        {
            item.put("explain", buildFeeFailureExplain(fee, exception));
        }
        return item;
    }

    private Map<String, Object> buildFeeCalculationExplain(FeeExecutionResult feeResult)
    {
        LinkedHashMap<String, Object> explain = new LinkedHashMap<>();
        explain.put("matched", true);
        explain.put("feeCode", feeResult.feeCode);
        explain.put("feeName", feeResult.feeName);
        explain.put("ruleId", feeResult.ruleId);
        explain.put("ruleCode", feeResult.ruleCode);
        explain.put("ruleName", feeResult.ruleName);
        explain.put("tierId", feeResult.tierId);
        explain.put("variables", feeResult.variableExplain);
        explain.put("conditions", feeResult.conditionExplain);
        explain.put("pricing", feeResult.pricingExplain);
        explain.put("timeline", feeResult.timelineSteps);
        return explain;
    }

    private Map<String, Object> buildFeeNoMatchExplain(RuntimeFee fee, List<RuntimeRule> rules,
            Map<String, Object> variableValues, List<Map<String, Object>> ruleEvaluations)
    {
        LinkedHashMap<String, Object> explain = new LinkedHashMap<>();
        explain.put("matched", false);
        explain.put("feeCode", fee.feeCode);
        explain.put("feeName", fee.feeName);
        explain.put("attemptedRuleCount", rules == null ? 0 : rules.size());
        explain.put("variables", variableValues);
        explain.put("candidateRules", ruleEvaluations == null ? Collections.emptyList() : ruleEvaluations);
        explain.put("timeline", Collections.singletonList(buildStep("FEE_SKIP", fee.feeCode, fee.feeName,
                rules == null || rules.isEmpty() ? "当前费用在当前运行配置下未挂载启用规则" : "当前费用未命中任何启用规则")));
        return explain;
    }

    private Map<String, Object> buildFeeFailureExplain(RuntimeFee fee, Exception exception)
    {
        LinkedHashMap<String, Object> explain = new LinkedHashMap<>();
        explain.put("matched", false);
        explain.put("feeCode", fee.feeCode);
        explain.put("feeName", fee.feeName);
        explain.put("timeline", Collections.singletonList(buildStep("FEE_FAILED", fee.feeCode, fee.feeName,
                limitLength(exception.getMessage(), 300))));
        return explain;
    }

    private String buildDetailSummary(List<CostResultLedger> ledgers)
    {
        if (ledgers.isEmpty())
        {
            return "未命中任何费用规则";
        }
        BigDecimal total = ledgers.stream().map(CostResultLedger::getAmountValue).reduce(BigDecimal.ZERO, BigDecimal::add);
        return String.format(Locale.ROOT, "已生成 %d 条费用结果，金额合计 %s", ledgers.size(), total.setScale(2, RoundingMode.HALF_UP));
    }

    private List<CostCalcTaskDetail> buildTaskDetails(CostCalcTask task, List<Map<String, Object>> inputs)
    {
        List<CostCalcTaskDetail> details = new ArrayList<>();
        int partitionSize = resolveTaskPartitionSize(inputs.size());
        for (int i = 0; i < inputs.size(); i++)
        {
            Map<String, Object> input = inputs.get(i);
            CostCalcTaskDetail detail = new CostCalcTaskDetail();
            detail.setTaskId(task.getTaskId());
            detail.setTaskNo(task.getTaskNo());
            detail.setBizNo(resolveBizNo(input, i + 1));
            detail.setPartitionNo(i / partitionSize + 1);
            detail.setDetailStatus(DETAIL_STATUS_INIT);
            detail.setRetryCount(0);
            detail.setInputJson(writeJson(input));
            detail.setResultSummary("");
            detail.setErrorMessage("");
            details.add(detail);
        }
        return details;
    }

    /**
     * 根据任务明细生成分片台账，为分片级监控、重试与失败定位提供基础。
     */
    private List<CostCalcTaskPartition> buildTaskPartitions(CostCalcTask task, List<CostCalcTaskDetail> details)
    {
        List<CostCalcTaskPartition> partitions = new ArrayList<>();
        List<List<CostCalcTaskDetail>> grouped = splitTaskPartitions(details);
        for (List<CostCalcTaskDetail> partitionDetails : grouped)
        {
            if (partitionDetails.isEmpty())
            {
                continue;
            }
            CostCalcTaskPartition partition = new CostCalcTaskPartition();
            partition.setTaskId(task.getTaskId());
            partition.setTaskNo(task.getTaskNo());
            partition.setPartitionNo(partitionDetails.get(0).getPartitionNo());
            partition.setStartItemNo(resolvePartitionStartItemNo(partition.getPartitionNo()));
            partition.setEndItemNo(resolvePartitionEndItemNo(partition.getPartitionNo(), partitionDetails.size()));
            partition.setPartitionStatus(DETAIL_STATUS_INIT);
            partition.setTotalCount(partitionDetails.size());
            partition.setProcessedCount(0);
            partition.setSuccessCount(0);
            partition.setFailCount(0);
            partition.setLastError("");
            partitions.add(partition);
        }
        return partitions;
    }

    private List<List<CostCalcTaskDetail>> splitTaskPartitions(List<CostCalcTaskDetail> details)
    {
        return new ArrayList<>(details.stream().collect(Collectors.groupingBy(
                CostCalcTaskDetail::getPartitionNo,
                LinkedHashMap::new,
                Collectors.toList())).values());
    }

    private int resolveTaskParallelism(int partitionCount)
    {
        int poolSize = threadPoolTaskExecutor.getCorePoolSize() > 0
                ? threadPoolTaskExecutor.getCorePoolSize() : DEFAULT_TASK_PARALLELISM;
        return Math.max(1, Math.min(Math.min(poolSize, DEFAULT_TASK_PARALLELISM), partitionCount));
    }

    private int resolveTaskPartitionSize(int inputSize)
    {
        return inputSize <= 0 ? DEFAULT_TASK_PARTITION_SIZE : DEFAULT_TASK_PARTITION_SIZE;
    }

    private int resolvePartitionStartItemNo(Integer partitionNo)
    {
        int safePartitionNo = partitionNo == null || partitionNo <= 0 ? 1 : partitionNo;
        return (safePartitionNo - 1) * DEFAULT_TASK_PARTITION_SIZE + 1;
    }

    private int resolvePartitionEndItemNo(Integer partitionNo, int partitionItemCount)
    {
        return resolvePartitionStartItemNo(partitionNo) + Math.max(partitionItemCount, 1) - 1;
    }

    private boolean isTaskCancelled(Long taskId)
    {
        CostCalcTask task = calcTaskMapper.selectById(taskId);
        return task == null || TASK_STATUS_CANCELLED.equals(task.getTaskStatus());
    }

    /**
     * 分片进入执行前先落运行态，便于任务中心观察分片实时进度。
     */
    private void markPartitionRunning(Long taskId, List<CostCalcTaskDetail> partitionDetails)
    {
        if (partitionDetails == null || partitionDetails.isEmpty())
        {
            return;
        }
        Integer partitionNo = partitionDetails.get(0).getPartitionNo();
        Date now = DateUtils.getNowDate();
        calcTaskPartitionMapper.update(null, Wrappers.<CostCalcTaskPartition>lambdaUpdate()
                .eq(CostCalcTaskPartition::getTaskId, taskId)
                .eq(CostCalcTaskPartition::getPartitionNo, partitionNo)
                .set(CostCalcTaskPartition::getPartitionStatus, TASK_STATUS_RUNNING)
                .set(CostCalcTaskPartition::getStartedTime, now)
                .set(CostCalcTaskPartition::getLastError, "")
                .set(CostCalcTaskPartition::getUpdateTime, now));
    }

    /**
     * 分片完成后回写统计与错误摘要，支撑后续分片级重试和监控。
     */
    private void finishPartition(Long taskId, List<CostCalcTaskDetail> partitionDetails,
            PartitionExecutionResult result, Throwable throwable)
    {
        if (partitionDetails == null || partitionDetails.isEmpty())
        {
            return;
        }
        Integer partitionNo = partitionDetails.get(0).getPartitionNo();
        PartitionExecutionResult summary = summarizePartitionDetails(taskId, partitionNo);
        String status = summary.failedCount <= 0 ? TASK_STATUS_SUCCESS
                : (summary.successCount > 0 ? TASK_STATUS_PART_SUCCESS : TASK_STATUS_FAILED);
        Date finishedTime = DateUtils.getNowDate();
        CostCalcTaskPartition partition = calcTaskPartitionMapper.selectOne(Wrappers.<CostCalcTaskPartition>lambdaQuery()
                .eq(CostCalcTaskPartition::getTaskId, taskId)
                .eq(CostCalcTaskPartition::getPartitionNo, partitionNo)
                .last("limit 1"));
        Long durationMs = partition == null || partition.getStartedTime() == null ? 0L
                : Math.max(0L, finishedTime.getTime() - partition.getStartedTime().getTime());
        calcTaskPartitionMapper.update(null, Wrappers.<CostCalcTaskPartition>lambdaUpdate()
                .eq(CostCalcTaskPartition::getTaskId, taskId)
                .eq(CostCalcTaskPartition::getPartitionNo, partitionNo)
                .set(CostCalcTaskPartition::getPartitionStatus, status)
                .set(CostCalcTaskPartition::getProcessedCount, summary.processedCount)
                .set(CostCalcTaskPartition::getSuccessCount, summary.successCount)
                .set(CostCalcTaskPartition::getFailCount, summary.failedCount)
                .set(CostCalcTaskPartition::getFinishedTime, finishedTime)
                .set(CostCalcTaskPartition::getDurationMs, durationMs)
                .set(CostCalcTaskPartition::getLastError, throwable == null ? "" : limitLength(throwable.getMessage(), 1000))
                .set(CostCalcTaskPartition::getUpdateTime, finishedTime));
    }

    protected PartitionExecutionResult executeTaskPartition(Long taskId, RuntimeSnapshot snapshot, List<CostCalcTaskDetail> details)
    {
        CostCalcTask task = calcTaskMapper.selectById(taskId);
        if (task == null || TASK_STATUS_CANCELLED.equals(task.getTaskStatus()) || details.isEmpty())
        {
            return new PartitionExecutionResult();
        }

        PartitionExecutionBundle bundle = new PartitionExecutionBundle();
        for (CostCalcTaskDetail detail : details)
        {
            if (isTaskCancelled(taskId))
            {
                break;
            }
            prepareTaskDetailExecution(task, detail, snapshot, bundle);
        }
        if (!bundle.detailUpdates.isEmpty())
        {
            transactionTemplate.executeWithoutResult(status -> persistPartitionBundle(taskId, bundle));
        }
        for (TaskDetailFailure failure : bundle.failures)
        {
            createTaskAlarm(task, failure.detail, "TASK_DETAIL_FAILED", "WARN",
                    "任务明细执行失败", "业务单号 " + failure.detail.getBizNo() + " 执行失败：" + limitLength(failure.errorMessage, 300));
        }
        return bundle.toResult();
    }

    private void prepareTaskDetailExecution(CostCalcTask task, CostCalcTaskDetail detail, RuntimeSnapshot snapshot, PartitionExecutionBundle bundle)
    {
        try
        {
            Map<String, Object> input = parseObjectJson(detail.getInputJson(), "任务明细输入必须是 JSON 对象");
            ExecutionResult executionResult = executeSingle(snapshot, task.getTaskNo(), task.getBillMonth(), input);
            List<CostResultLedger> detailLedgers = new ArrayList<>();
            for (FeeExecutionResult feeResult : executionResult.feeResults)
            {
                CostResultTrace trace = buildTraceRecord(snapshot, feeResult);
                CostResultLedger ledger = buildLedgerRecord(task, detail, snapshot, input, feeResult, trace.getTraceId());
                bundle.traceInserts.add(trace);
                bundle.ledgerInserts.add(ledger);
                detailLedgers.add(ledger);
            }
            bundle.detailUpdates.add(buildTaskDetailUpdate(detail, DETAIL_STATUS_SUCCESS, buildDetailSummary(detailLedgers), ""));
            bundle.processedCount++;
            bundle.successCount++;
        }
        catch (Exception e)
        {
            String errorMessage = limitLength(e.getMessage(), 1000);
            bundle.detailUpdates.add(buildTaskDetailUpdate(detail, DETAIL_STATUS_FAILED, "执行失败", errorMessage));
            bundle.failures.add(new TaskDetailFailure(detail, errorMessage));
            bundle.processedCount++;
            bundle.failedCount++;
        }
    }

    private void persistPartitionBundle(Long taskId, PartitionExecutionBundle bundle)
    {
        purgeExistingTaskResults(taskId, bundle.bizNos());
        if (!bundle.traceInserts.isEmpty())
        {
            resultTraceMapper.insertBatch(bundle.traceInserts);
        }
        if (!bundle.ledgerInserts.isEmpty())
        {
            resultLedgerMapper.insertBatch(bundle.ledgerInserts);
        }
        calcTaskDetailMapper.updateBatchResult(bundle.detailUpdates);
    }

    private void purgeExistingTaskResults(Long taskId, Collection<String> bizNos)
    {
        if (bizNos == null || bizNos.isEmpty())
        {
            return;
        }
        List<CostResultLedger> existing = resultLedgerMapper.selectList(Wrappers.<CostResultLedger>lambdaQuery()
                .eq(CostResultLedger::getTaskId, taskId)
                .in(CostResultLedger::getBizNo, bizNos));
        if (existing.isEmpty())
        {
            return;
        }
        List<Long> traceIds = existing.stream().map(CostResultLedger::getTraceId).filter(Objects::nonNull).collect(Collectors.toList());
        resultLedgerMapper.deleteBatchIds(existing.stream().map(CostResultLedger::getResultId).collect(Collectors.toList()));
        if (!traceIds.isEmpty())
        {
            resultTraceMapper.deleteBatchIds(traceIds);
        }
    }

    private CostResultTrace buildTraceRecord(RuntimeSnapshot snapshot, FeeExecutionResult feeResult)
    {
        CostResultTrace trace = new CostResultTrace();
        trace.setTraceId(nextSnowflakeId());
        trace.setSceneId(snapshot.sceneId);
        trace.setVersionId(snapshot.versionId);
        trace.setRuleId(feeResult.ruleId);
        trace.setTierId(feeResult.tierId);
        trace.setVariableJson(writeJson(feeResult.variableExplain));
        trace.setConditionJson(writeJson(feeResult.conditionExplain));
        trace.setPricingJson(writeJson(feeResult.pricingExplain));
        trace.setTimelineJson(writeJson(feeResult.timelineSteps));
        return trace;
    }

    private CostResultLedger buildLedgerRecord(CostCalcTask task, CostCalcTaskDetail detail, RuntimeSnapshot snapshot,
            Map<String, Object> input, FeeExecutionResult feeResult, Long traceId)
    {
        CostResultLedger ledger = new CostResultLedger();
        ledger.setResultId(nextSnowflakeId());
        ledger.setTaskId(task.getTaskId());
        ledger.setTaskNo(task.getTaskNo());
        ledger.setSceneId(snapshot.sceneId);
        ledger.setVersionId(snapshot.versionId);
        ledger.setFeeId(feeResult.feeId);
        ledger.setFeeCode(feeResult.feeCode);
        ledger.setFeeName(feeResult.feeName);
        ledger.setBizNo(detail.getBizNo());
        ledger.setBillMonth(task.getBillMonth());
        ledger.setObjectDimension(firstNonBlank(feeResult.objectDimension, resolveString(input, "objectDimension", "object_dimension")));
        ledger.setObjectCode(firstNonBlank(resolveString(input, "objectCode", "object_code"), detail.getBizNo()));
        ledger.setObjectName(resolveString(input, "objectName", "object_name", "name"));
        ledger.setQuantityValue(feeResult.quantityValue);
        ledger.setUnitPrice(feeResult.unitPrice);
        ledger.setAmountValue(feeResult.amountValue);
        ledger.setCurrencyCode("CNY");
        ledger.setResultStatus(RESULT_STATUS_SUCCESS);
        ledger.setTraceId(traceId);
        return ledger;
    }

    private CostCalcTaskDetail buildTaskDetailUpdate(CostCalcTaskDetail detail, String status, String resultSummary, String errorMessage)
    {
        CostCalcTaskDetail update = new CostCalcTaskDetail();
        update.setDetailId(detail.getDetailId());
        update.setBizNo(detail.getBizNo());
        update.setDetailStatus(status);
        update.setRetryCount(detail.getRetryCount());
        update.setResultSummary(resultSummary);
        update.setErrorMessage(errorMessage);
        return update;
    }

    private long nextSnowflakeId()
    {
        return IdWorker.getId();
    }

    private PartitionExecutionResult markPartitionFailed(Long taskId, List<CostCalcTaskDetail> partition, Throwable throwable)
    {
        CostCalcTask task = calcTaskMapper.selectById(taskId);
        if (task == null || partition == null || partition.isEmpty())
        {
            return new PartitionExecutionResult();
        }
        String errorMessage = limitLength(throwable == null ? "分片执行失败" : throwable.getMessage(), 1000);
        List<CostCalcTaskDetail> updates = partition.stream()
                .map(detail -> buildTaskDetailUpdate(detail, DETAIL_STATUS_FAILED, "分片执行失败", errorMessage))
                .collect(Collectors.toList());
        transactionTemplate.executeWithoutResult(status -> calcTaskDetailMapper.updateBatchResult(updates));
        for (CostCalcTaskDetail detail : partition)
        {
            createTaskAlarm(task, detail, "TASK_PARTITION_FAILED", "ERROR",
                    "任务分片执行失败", "分片 " + detail.getPartitionNo() + " 执行失败：" + limitLength(errorMessage, 300));
        }
        PartitionExecutionResult result = new PartitionExecutionResult();
        result.processedCount = partition.size();
        result.failedCount = partition.size();
        return result;
    }

    private Map<String, Object> parseObjectJson(String json, String errorMessage)
    {
        Object parsed = parseJsonToObject(json);
        if (!(parsed instanceof Map))
        {
            throw new ServiceException(errorMessage);
        }
        return castMap(parsed);
    }

    private List<Map<String, Object>> parseArrayJson(String json, String errorMessage)
    {
        Object parsed = parseJsonToObject(json);
        if (!(parsed instanceof List))
        {
            throw new ServiceException(errorMessage);
        }
        List<?> list = (List<?>) parsed;
        List<Map<String, Object>> result = new ArrayList<>();
        for (Object item : list)
        {
            if (!(item instanceof Map))
            {
                throw new ServiceException(errorMessage);
            }
            result.add(castMap(item));
        }
        return result;
    }

    private Object parseJsonToObject(String json)
    {
        if (StringUtils.isEmpty(json))
        {
            return new LinkedHashMap<>();
        }
        try
        {
            return objectMapper.readValue(json, Object.class);
        }
        catch (JsonProcessingException e)
        {
            throw new ServiceException("JSON 解析失败：" + e.getOriginalMessage());
        }
    }

    private Map<String, Object> parseJsonMap(String json)
    {
        if (StringUtils.isEmpty(json))
        {
            return new LinkedHashMap<>();
        }
        try
        {
            return objectMapper.readValue(json, new TypeReference<LinkedHashMap<String, Object>>() {});
        }
        catch (JsonProcessingException e)
        {
            throw new ServiceException("发布快照 JSON 解析失败");
        }
    }

    private String writeJson(Object value)
    {
        try
        {
            return objectMapper.writeValueAsString(value);
        }
        catch (JsonProcessingException e)
        {
            throw new ServiceException("JSON 序列化失败");
        }
    }

    private Object evaluateExpression(String expression, Map<String, Object> context)
    {
        if (StringUtils.isEmpty(expression))
        {
            return null;
        }
        try
        {
            return expressionService.evaluate(expression, context);
        }
        catch (Exception e)
        {
            throw new ServiceException("表达式执行失败：" + expression);
        }
    }

    private Map<String, Object> mergeContext(Map<String, Object> inputContext, Map<String, Object> variableValues, Map<String, Object> feeResultContext)
    {
        LinkedHashMap<String, Object> context = new LinkedHashMap<>();
        if (inputContext != null)
        {
            context.putAll(inputContext);
        }
        if (variableValues != null)
        {
            context.putAll(variableValues);
        }
        context.put("I", inputContext == null ? new LinkedHashMap<>() : new LinkedHashMap<>(inputContext));
        context.put("V", variableValues == null ? new LinkedHashMap<>() : new LinkedHashMap<>(variableValues));
        LinkedHashMap<String, Object> common = new LinkedHashMap<>();
        common.put("sceneCode", inputContext == null ? null : inputContext.get("sceneCode"));
        common.put("sceneName", inputContext == null ? null : inputContext.get("sceneName"));
        common.put("versionNo", inputContext == null ? null : inputContext.get("versionNo"));
        common.put("billMonth", inputContext == null ? null : inputContext.get("billMonth"));
        context.put("C", common);
        context.put("F", feeResultContext == null ? new LinkedHashMap<>() : new LinkedHashMap<>(feeResultContext));
        context.put("T", new LinkedHashMap<>());
        return context;
    }

    private String resolveVariableFormula(RuntimeSnapshot snapshot, RuntimeVariable variable)
    {
        if (StringUtils.isEmpty(variable.formulaCode))
        {
            throw new ServiceException("当前运行配置中的公式变量[" + variable.variableCode + "]未绑定公式编码，请先补齐配置后再执行");
        }
        RuntimeFormula formula = snapshot.formulasByCode.get(variable.formulaCode);
        if (formula == null || StringUtils.isEmpty(formula.formulaExpr))
        {
            throw new ServiceException("当前运行配置中的公式变量[" + variable.variableCode + "]引用的公式编码[" + variable.formulaCode + "]不存在或不可执行，请先补齐配置后再执行");
        }
        return formula.formulaExpr;
    }

    private RuntimeFormula requireRuleFormula(RuntimeSnapshot snapshot, RuntimeRule rule)
    {
        if (StringUtils.isEmpty(rule.amountFormulaCode))
        {
            throw new ServiceException("当前运行配置中的公式规则[" + rule.ruleCode + "]未绑定金额公式编码，请先补齐配置后再执行");
        }
        RuntimeFormula formula = snapshot.formulasByCode.get(rule.amountFormulaCode);
        if (formula == null || StringUtils.isEmpty(formula.formulaExpr))
        {
            throw new ServiceException("当前运行配置中的公式规则[" + rule.ruleCode + "]引用的公式编码[" + rule.amountFormulaCode + "]不存在或不可执行，请先补齐配置后再执行");
        }
        return formula;
    }

    private Object resolveValueFromInput(Map<String, Object> input, String dataPath, String variableCode, Object defaultValue)
    {
        Object value = null;
        if (StringUtils.isNotEmpty(dataPath))
        {
            value = resolveByPath(input, dataPath);
        }
        if (value == null && StringUtils.isNotEmpty(variableCode))
        {
            value = resolveByPath(input, variableCode);
        }
        return value != null ? value : defaultValue;
    }

    private Object resolveByPath(Map<String, Object> input, String path)
    {
        if (input == null || StringUtils.isEmpty(path))
        {
            return null;
        }
        String[] pieces = path.split("\\.");
        Object current = input;
        for (String piece : pieces)
        {
            if (!(current instanceof Map))
            {
                return null;
            }
            current = ((Map<?, ?>) current).get(piece);
            if (current == null)
            {
                return null;
            }
        }
        return current;
    }

    private Object convertValueByType(Object value, String dataType, Object defaultValue)
    {
        if (DATA_TYPE_NUMBER.equals(dataType))
        {
            return toBigDecimal(value == null ? defaultValue : value);
        }
        if (DATA_TYPE_BOOLEAN.equals(dataType))
        {
            return convertBoolean(value == null ? defaultValue : value);
        }
        if (DATA_TYPE_JSON.equals(dataType) && value instanceof String)
        {
            return parseJsonToObject(String.valueOf(value));
        }
        if (value == null)
        {
            return defaultValue;
        }
        return value;
    }

    private Boolean convertBoolean(Object value)
    {
        if (value == null)
        {
            return Boolean.FALSE;
        }
        if (value instanceof Boolean)
        {
            return (Boolean) value;
        }
        return "true".equalsIgnoreCase(String.valueOf(value)) || "1".equals(String.valueOf(value));
    }

    private List<String> splitValues(String compareValue)
    {
        if (StringUtils.isEmpty(compareValue))
        {
            return Collections.emptyList();
        }
        List<String> result = new ArrayList<>();
        for (String piece : compareValue.split(","))
        {
            if (StringUtils.isNotEmpty(piece))
            {
                result.add(piece.trim());
            }
        }
        return result;
    }

    private Long findFeeIdByCode(Long sceneId, String feeCode)
    {
        CostFeeItem fee = feeMapper.selectOne(Wrappers.<CostFeeItem>lambdaQuery()
                .eq(CostFeeItem::getSceneId, sceneId)
                .eq(CostFeeItem::getFeeCode, feeCode)
                .last("limit 1"));
        return fee == null ? null : fee.getFeeId();
    }

    private Map<String, Object> buildStep(String stepType, String objectCode, String objectName, String resultSummary)
    {
        LinkedHashMap<String, Object> step = new LinkedHashMap<>();
        step.put("stepType", stepType);
        step.put("objectCode", objectCode);
        step.put("objectName", objectName);
        step.put("resultSummary", resultSummary);
        return step;
    }

    private BigDecimal toBigDecimal(Object value)
    {
        if (value == null || StringUtils.isEmpty(String.valueOf(value)))
        {
            return null;
        }
        if (value instanceof BigDecimal)
        {
            return (BigDecimal) value;
        }
        try
        {
            return new BigDecimal(String.valueOf(value).trim());
        }
        catch (Exception e)
        {
            return null;
        }
    }

    private BigDecimal defaultZero(BigDecimal value)
    {
        return value == null ? BigDecimal.ZERO : value;
    }

    private Integer intValue(Object value)
    {
        if (value == null || StringUtils.isEmpty(String.valueOf(value)))
        {
            return null;
        }
        return Integer.parseInt(String.valueOf(value));
    }

    private Long longValue(Object value)
    {
        if (value == null || StringUtils.isEmpty(String.valueOf(value)))
        {
            return null;
        }
        return Long.parseLong(String.valueOf(value));
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> castMap(Object value)
    {
        return (Map<String, Object>) value;
    }

    private String resolveBizNo(Map<String, Object> input, int fallbackNo)
    {
        String bizNo = resolveString(input, "bizNo", "biz_no", "businessNo", "business_no");
        return StringUtils.isNotEmpty(bizNo) ? bizNo : "BIZ-" + PARTITION_FORMAT.format(fallbackNo);
    }

    private String resolveString(Map<String, Object> input, String... keys)
    {
        for (String key : keys)
        {
            Object value = input.get(key);
            if (value != null && StringUtils.isNotEmpty(String.valueOf(value)))
            {
                return String.valueOf(value);
            }
        }
        return "";
    }

    private String buildRunNo(String prefix)
    {
        return prefix + "-" + LocalDateTime.now().format(NO_TIME_FORMATTER) + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.ROOT);
    }

    private String resolveExecuteNode()
    {
        return firstNonBlank(System.getenv("COMPUTERNAME"), "LOCAL");
    }

    private String firstNonBlank(String first, String second)
    {
        return StringUtils.isNotEmpty(first) ? first : second;
    }

    private String resolveOperator()
    {
        try
        {
            return firstNonBlank(SecurityUtils.getUsername(), "system");
        }
        catch (Exception ignored)
        {
            return "system";
        }
    }

    private String stringValue(Object value)
    {
        return value == null ? "" : String.valueOf(value);
    }

    private String limitLength(String text, int maxLength)
    {
        if (text == null)
        {
            return "";
        }
        return text.length() <= maxLength ? text : text.substring(0, maxLength);
    }

    private CostBillPeriod ensureBillPeriodAvailable(Long sceneId, String billMonth, Long versionId)
    {
        CostBillPeriod period = billPeriodMapper.selectOne(Wrappers.<CostBillPeriod>lambdaQuery()
                .eq(CostBillPeriod::getSceneId, sceneId)
                .eq(CostBillPeriod::getBillMonth, billMonth)
                .last("limit 1"));
        if (period == null)
        {
            Date now = DateUtils.getNowDate();
            String operator = resolveOperator();
            period = new CostBillPeriod();
            period.setSceneId(sceneId);
            period.setBillMonth(billMonth);
            period.setPeriodStatus(PERIOD_STATUS_NOT_STARTED);
            period.setActiveVersionId(versionId);
            period.setResultCount(0L);
            period.setAmountTotal(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
            period.setRemark("运行链自动创建账期");
            period.setCreateBy(operator);
            period.setCreateTime(now);
            period.setUpdateBy(operator);
            period.setUpdateTime(now);
            billPeriodMapper.insert(period);
            return period;
        }
        if (PERIOD_STATUS_SEALED.equals(period.getPeriodStatus()))
        {
            throw new ServiceException("当前账期已封存，禁止直接提交正式核算任务");
        }
        if (!Objects.equals(period.getActiveVersionId(), versionId))
        {
            billPeriodMapper.update(null, Wrappers.<CostBillPeriod>lambdaUpdate()
                    .eq(CostBillPeriod::getPeriodId, period.getPeriodId())
                    .set(CostBillPeriod::getActiveVersionId, versionId)
                    .set(CostBillPeriod::getUpdateBy, resolveOperator())
                    .set(CostBillPeriod::getUpdateTime, DateUtils.getNowDate()));
            period.setActiveVersionId(versionId);
        }
        return period;
    }

    private void markPeriodInProgress(CostBillPeriod period, CostCalcTask task)
    {
        billPeriodMapper.update(null, Wrappers.<CostBillPeriod>lambdaUpdate()
                .eq(CostBillPeriod::getPeriodId, period.getPeriodId())
                .ne(CostBillPeriod::getPeriodStatus, PERIOD_STATUS_SEALED)
                .set(CostBillPeriod::getPeriodStatus, PERIOD_STATUS_IN_PROGRESS)
                .set(CostBillPeriod::getActiveVersionId, task.getVersionId())
                .set(CostBillPeriod::getLastTaskId, task.getTaskId())
                .set(CostBillPeriod::getLastTaskNo, task.getTaskNo())
                .set(CostBillPeriod::getUpdateBy, resolveOperator())
                .set(CostBillPeriod::getUpdateTime, DateUtils.getNowDate()));
    }

    private void refreshBillPeriod(Long sceneId, String billMonth, CostCalcTask task)
    {
        CostBillPeriod period = billPeriodMapper.selectOne(Wrappers.<CostBillPeriod>lambdaQuery()
                .eq(CostBillPeriod::getSceneId, sceneId)
                .eq(CostBillPeriod::getBillMonth, billMonth)
                .last("limit 1"));
        if (period == null)
        {
            return;
        }
        List<CostResultLedger> ledgers = resultLedgerMapper.selectList(Wrappers.<CostResultLedger>lambdaQuery()
                .eq(CostResultLedger::getSceneId, sceneId)
                .eq(CostResultLedger::getBillMonth, billMonth));
        BigDecimal amountTotal = ledgers.stream()
                .map(CostResultLedger::getAmountValue)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
        String periodStatus = period.getPeriodStatus();
        if (!PERIOD_STATUS_SEALED.equals(periodStatus))
        {
            if (task != null && (TASK_STATUS_RUNNING.equals(task.getTaskStatus()) || TASK_STATUS_INIT.equals(task.getTaskStatus())))
            {
                periodStatus = PERIOD_STATUS_IN_PROGRESS;
            }
            else if (!ledgers.isEmpty())
            {
                periodStatus = PERIOD_STATUS_CLOSED;
            }
            else
            {
                periodStatus = PERIOD_STATUS_NOT_STARTED;
            }
        }
        billPeriodMapper.update(null, Wrappers.<CostBillPeriod>lambdaUpdate()
                .eq(CostBillPeriod::getPeriodId, period.getPeriodId())
                .set(CostBillPeriod::getPeriodStatus, periodStatus)
                .set(CostBillPeriod::getActiveVersionId, task == null ? period.getActiveVersionId() : task.getVersionId())
                .set(CostBillPeriod::getResultCount, (long) ledgers.size())
                .set(CostBillPeriod::getAmountTotal, amountTotal)
                .set(CostBillPeriod::getLastTaskId, task == null ? period.getLastTaskId() : task.getTaskId())
                .set(CostBillPeriod::getLastTaskNo, task == null ? period.getLastTaskNo() : task.getTaskNo())
                .set(CostBillPeriod::getUpdateBy, resolveOperator())
                .set(CostBillPeriod::getUpdateTime, DateUtils.getNowDate()));
    }

    private void syncRecalcByTask(CostCalcTask task, String taskStatus)
    {
        CostRecalcOrder recalcOrder = recalcOrderMapper.selectOne(Wrappers.<CostRecalcOrder>lambdaQuery()
                .eq(CostRecalcOrder::getTargetTaskId, task.getTaskId())
                .last("limit 1"));
        if (recalcOrder == null)
        {
            return;
        }
        String recalcStatus = RECALC_STATUS_RUNNING;
        if (TASK_STATUS_SUCCESS.equals(taskStatus) || TASK_STATUS_PART_SUCCESS.equals(taskStatus))
        {
            recalcStatus = RECALC_STATUS_SUCCESS;
        }
        else if (TASK_STATUS_FAILED.equals(taskStatus) || TASK_STATUS_CANCELLED.equals(taskStatus))
        {
            recalcStatus = RECALC_STATUS_FAILED;
        }
        String diffSummaryJson = buildRecalcDiffSummary(recalcOrder, task);
        BigDecimal diffAmount = extractDiffAmount(diffSummaryJson);
        recalcOrderMapper.update(null, Wrappers.<CostRecalcOrder>lambdaUpdate()
                .eq(CostRecalcOrder::getRecalcId, recalcOrder.getRecalcId())
                .set(CostRecalcOrder::getRecalcStatus, recalcStatus)
                .set(CostRecalcOrder::getDiffSummaryJson, diffSummaryJson)
                .set(CostRecalcOrder::getDiffAmount, diffAmount)
                .set(CostRecalcOrder::getFinishTime, DateUtils.getNowDate())
                .set(CostRecalcOrder::getUpdateBy, resolveOperator())
                .set(CostRecalcOrder::getUpdateTime, DateUtils.getNowDate()));
    }

    private String buildRecalcDiffSummary(CostRecalcOrder recalcOrder, CostCalcTask task)
    {
        CostCalcTask baselineTask = recalcOrder.getBaselineTaskId() == null ? null : calcTaskMapper.selectById(recalcOrder.getBaselineTaskId());
        BigDecimal baselineAmount = sumTaskAmount(recalcOrder.getBaselineTaskId());
        BigDecimal targetAmount = sumTaskAmount(task.getTaskId());
        LinkedHashMap<String, Object> summary = new LinkedHashMap<>();
        summary.put("baselineTaskNo", baselineTask == null ? recalcOrder.getBaselineTaskNo() : baselineTask.getTaskNo());
        summary.put("targetTaskNo", task.getTaskNo());
        summary.put("baselineAmount", baselineAmount);
        summary.put("targetAmount", targetAmount);
        summary.put("diffAmount", targetAmount.subtract(baselineAmount).setScale(2, RoundingMode.HALF_UP));
        summary.put("baselineResultCount", countTaskResults(recalcOrder.getBaselineTaskId()));
        summary.put("targetResultCount", countTaskResults(task.getTaskId()));
        summary.put("taskStatus", task.getTaskStatus());
        return writeJson(summary);
    }

    private BigDecimal extractDiffAmount(String diffSummaryJson)
    {
        try
        {
            Map<String, Object> summary = objectMapper.readValue(diffSummaryJson, new TypeReference<Map<String, Object>>() {});
            BigDecimal diffAmount = toBigDecimal(summary.get("diffAmount"));
            return defaultZero(diffAmount).setScale(2, RoundingMode.HALF_UP);
        }
        catch (Exception ignored)
        {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
    }

    private BigDecimal sumTaskAmount(Long taskId)
    {
        if (taskId == null)
        {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return resultLedgerMapper.selectList(Wrappers.<CostResultLedger>lambdaQuery()
                        .eq(CostResultLedger::getTaskId, taskId))
                .stream()
                .map(CostResultLedger::getAmountValue)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
    }

    private long countTaskResults(Long taskId)
    {
        if (taskId == null)
        {
            return 0L;
        }
        return resultLedgerMapper.selectCount(Wrappers.<CostResultLedger>lambdaQuery()
                .eq(CostResultLedger::getTaskId, taskId));
    }

    private void createTaskAlarm(CostCalcTask task, CostCalcTaskDetail detail, String alarmType,
            String alarmLevel, String alarmTitle, String alarmContent)
    {
        CostAlarmRecord alarm = new CostAlarmRecord();
        alarm.setSceneId(task.getSceneId());
        alarm.setVersionId(task.getVersionId());
        alarm.setTaskId(task.getTaskId());
        alarm.setDetailId(detail == null ? null : detail.getDetailId());
        alarm.setBillMonth(task.getBillMonth());
        alarm.setAlarmType(alarmType);
        alarm.setAlarmLevel(alarmLevel);
        alarm.setAlarmTitle(alarmTitle);
        alarm.setAlarmContent(limitLength(alarmContent, 1000));
        alarm.setSourceKey(task.getTaskNo() + ":" + alarmType + ":" + (detail == null ? "TASK" : detail.getDetailId()));
        alarmService.createAlarm(alarm);
    }

    private String buildRuntimeCacheKey(Long versionId)
    {
        return RUNTIME_CACHE_PREFIX + versionId;
    }

    public static class RuntimeSnapshot
    {
        public Long sceneId;
        public Long versionId;
        public String sceneCode;
        public String sceneName;
        public String versionNo;
        public String snapshotSource;
        public List<RuntimeFee> fees = new ArrayList<>();
        public Map<String, RuntimeFee> feesByCode = new LinkedHashMap<>();
        public List<RuntimeVariable> variables = new ArrayList<>();
        public Map<String, RuntimeVariable> variablesByCode = new LinkedHashMap<>();
        public Map<String, List<RuntimeVariable>> executionVariablesByFeeCode = new LinkedHashMap<>();
        public Map<String, RuntimeFormula> formulasByCode = new LinkedHashMap<>();
        public Map<String, RuntimeRule> rulesByCode = new LinkedHashMap<>();
        public Map<String, List<RuntimeRule>> rulesByFeeCode = new LinkedHashMap<>();
        public Map<String, List<RuntimeCondition>> conditionsByRuleCode = new LinkedHashMap<>();
        public Map<String, List<RuntimeTier>> tiersByRuleCode = new LinkedHashMap<>();
    }

    public static class RuntimeFee
    {
        public Long feeId;
        public String feeCode;
        public String feeName;
        public String unitCode;
        public String objectDimension;
        public Integer sortNo;
    }

    public static class RuntimeVariable
    {
        public String variableCode;
        public String variableName;
        public String sourceType;
        public String dataType;
        public String dataPath;
        public String formulaExpr;
        public String formulaCode;
        public Object defaultValue;
        public Integer sortNo;
    }

    public static class RuntimeFormula
    {
        public String formulaCode;
        public String formulaName;
        public String businessFormula;
        public String formulaExpr;
        public String returnType;
    }

    public static class RuntimeRule
    {
        public Long ruleId;
        public String feeCode;
        public String ruleCode;
        public String ruleName;
        public String ruleType;
        public String conditionLogic;
        public Integer priority;
        public String quantityVariableCode;
        public String pricingMode;
        public Integer matchedGroupNo;
        public Map<String, Object> pricingConfig;
        public String amountFormula;
        public String amountFormulaCode;
        public String amountBusinessFormula;
        public Integer sortNo;
        public List<RuntimeCondition> conditions = Collections.emptyList();
        public List<RuntimeConditionGroup> conditionGroups = Collections.emptyList();
        public List<RuntimeTier> tiers = Collections.emptyList();
    }

    public static class RuntimeCondition
    {
        public String ruleCode;
        public Integer groupNo;
        public Integer sortNo;
        public String variableCode;
        public String displayName;
        public String operatorCode;
        public String compareValue;
    }

    public static class RuntimeConditionGroup
    {
        public Integer groupNo;
        public List<RuntimeCondition> conditions = Collections.emptyList();
    }

    public static class RuntimeTier
    {
        public Long tierId;
        public String ruleCode;
        public Integer tierNo;
        public BigDecimal startValue;
        public BigDecimal endValue;
        public BigDecimal rateValue;
        public String intervalMode;

        private String buildRangeSummary()
        {
            return String.format(Locale.ROOT, "%s ~ %s",
                    startValue == null ? "-INF" : startValue.toPlainString(),
                    endValue == null ? "+INF" : endValue.toPlainString());
        }
    }

    private static class FeeTemplateContext
    {
        private final Map<String, FeeTemplateVariable> variables = new LinkedHashMap<>();
        private final List<Map<String, Object>> ruleSummaries = new ArrayList<>();
    }

    private static class FeeTemplateVariable
    {
        private final RuntimeVariable variable;
        private final Set<String> templateRoles = new LinkedHashSet<>();
        private final Set<String> sourceRuleCodes = new LinkedHashSet<>();
        private final Set<String> dependsOn = new LinkedHashSet<>();
        private boolean includedInTemplate;

        private FeeTemplateVariable(RuntimeVariable variable)
        {
            this.variable = variable;
        }
    }

    private static class RuleMatchResult
    {
        private RuntimeRule rule;
        private RuntimeTier tier;
        private Integer matchedGroupNo;
        private List<Map<String, Object>> conditionExplain = Collections.emptyList();
        private List<Map<String, Object>> ruleEvaluations = new ArrayList<>();
    }

    private static class ConditionMatchResult
    {
        private boolean matched;
        private Integer matchedGroupNo;
    }

    private static class PricingResult
    {
        private BigDecimal quantityValue;
        private BigDecimal unitPrice;
        private BigDecimal amountValue;
        private Map<String, Object> pricingExplain;
    }

    private static class FeeExecutionResult
    {
        private Long feeId;
        private String feeCode;
        private String feeName;
        private String unitCode;
        private String objectDimension;
        private Long ruleId;
        private String ruleCode;
        private String ruleName;
        private Long tierId;
        private BigDecimal quantityValue;
        private BigDecimal unitPrice;
        private BigDecimal amountValue;
        private Map<String, Object> variableExplain;
        private List<Map<String, Object>> conditionExplain;
        private Map<String, Object> pricingExplain;
        private List<Map<String, Object>> timelineSteps;

        private Map<String, Object> toView()
        {
            LinkedHashMap<String, Object> item = new LinkedHashMap<>();
            item.put("feeId", feeId);
            item.put("feeCode", feeCode);
            item.put("feeName", feeName);
            item.put("unitCode", unitCode);
            item.put("ruleCode", ruleCode);
            item.put("ruleName", ruleName);
            item.put("quantityValue", quantityValue);
            item.put("unitPrice", unitPrice);
            item.put("amountValue", amountValue);
            return item;
        }

        private Map<String, Object> toExplainView()
        {
            LinkedHashMap<String, Object> item = new LinkedHashMap<>();
            item.put("feeCode", feeCode);
            item.put("feeName", feeName);
            item.put("unitCode", unitCode);
            item.put("ruleCode", ruleCode);
            item.put("ruleName", ruleName);
            item.put("conditions", conditionExplain);
            item.put("pricing", pricingExplain);
            item.put("timeline", timelineSteps);
            return item;
        }
    }

    private static class TaskDetailFailure
    {
        private final CostCalcTaskDetail detail;
        private final String errorMessage;

        private TaskDetailFailure(CostCalcTaskDetail detail, String errorMessage)
        {
            this.detail = detail;
            this.errorMessage = errorMessage;
        }
    }

    private static class TaskExecutionSummary
    {
        private int totalCount;
        private int processedCount;
        private int successCount;
        private int failedCount;
    }

    private static class PartitionExecutionResult
    {
        private int processedCount;
        private int successCount;
        private int failedCount;
    }

    private static class PartitionExecutionBundle
    {
        private final List<CostResultTrace> traceInserts = new ArrayList<>();
        private final List<CostResultLedger> ledgerInserts = new ArrayList<>();
        private final List<CostCalcTaskDetail> detailUpdates = new ArrayList<>();
        private final List<TaskDetailFailure> failures = new ArrayList<>();
        private int processedCount;
        private int successCount;
        private int failedCount;

        private Collection<String> bizNos()
        {
            return detailUpdates.stream()
                    .map(CostCalcTaskDetail::getBizNo)
                    .filter(StringUtils::isNotEmpty)
                    .collect(Collectors.toCollection(LinkedHashSet::new));
        }

        private PartitionExecutionResult toResult()
        {
            PartitionExecutionResult result = new PartitionExecutionResult();
            result.processedCount = processedCount;
            result.successCount = successCount;
            result.failedCount = failedCount;
            return result;
        }
    }

    private static class ExecutionResult
    {
        private Map<String, Object> variableView;
        private Map<String, Object> resultView;
        private Map<String, Object> explainView;
        private List<FeeExecutionResult> feeResults;
        private Map<String, Map<String, Object>> skippedFeeExplains = new LinkedHashMap<>();
    }
}

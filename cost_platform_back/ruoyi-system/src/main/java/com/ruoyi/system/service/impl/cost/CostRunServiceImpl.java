package com.ruoyi.system.service.impl.cost;

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
import com.ruoyi.system.domain.cost.CostCalcTask;
import com.ruoyi.system.domain.cost.CostCalcTaskDetail;
import com.ruoyi.system.domain.cost.CostFeeItem;
import com.ruoyi.system.domain.cost.CostPublishSnapshot;
import com.ruoyi.system.domain.cost.CostPublishVersion;
import com.ruoyi.system.domain.cost.CostRecalcOrder;
import com.ruoyi.system.domain.cost.CostResultLedger;
import com.ruoyi.system.domain.cost.CostResultTrace;
import com.ruoyi.system.domain.cost.CostScene;
import com.ruoyi.system.domain.cost.CostSimulationRecord;
import com.ruoyi.system.domain.cost.bo.CostCalcTaskSubmitBo;
import com.ruoyi.system.domain.cost.bo.CostSimulationExecuteBo;
import com.ruoyi.system.mapper.cost.CostBillPeriodMapper;
import com.ruoyi.system.mapper.cost.CostCalcTaskDetailMapper;
import com.ruoyi.system.mapper.cost.CostCalcTaskMapper;
import com.ruoyi.system.mapper.cost.CostFeeMapper;
import com.ruoyi.system.mapper.cost.CostPublishVersionMapper;
import com.ruoyi.system.mapper.cost.CostRecalcOrderMapper;
import com.ruoyi.system.mapper.cost.CostResultLedgerMapper;
import com.ruoyi.system.mapper.cost.CostResultTraceMapper;
import com.ruoyi.system.mapper.cost.CostSceneMapper;
import com.ruoyi.system.mapper.cost.CostSimulationRecordMapper;
import com.ruoyi.system.service.cost.ICostAlarmService;
import com.ruoyi.system.service.cost.ICostAuditService;
import com.ruoyi.system.service.cost.ICostRunService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.expression.MapAccessor;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.time.LocalDateTime;
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
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.TimeUnit;
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
    private static final String SIMULATION_STATUS_SUCCESS = "SUCCESS";
    private static final String SIMULATION_STATUS_FAILED = "FAILED";
    private static final String TASK_TYPE_FORMAL_SINGLE = "FORMAL_SINGLE";
    private static final String TASK_TYPE_FORMAL_BATCH = "FORMAL_BATCH";
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

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ExpressionParser expressionParser = new SpelExpressionParser();

    @Autowired
    private CostSimulationRecordMapper simulationRecordMapper;

    @Autowired
    private CostCalcTaskMapper calcTaskMapper;

    @Autowired
    private CostCalcTaskDetailMapper calcTaskDetailMapper;

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
    private ThreadPoolTaskExecutor threadPoolTaskExecutor;

    @Autowired
    private RedisCache redisCache;

    @Autowired
    private ICostAuditService auditService;

    @Autowired
    private ICostAlarmService alarmService;

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
        RuntimeSnapshot snapshot = loadRuntimeSnapshot(bo.getSceneId(), bo.getVersionId(), false);
        Map<String, Object> input = parseObjectJson(bo.getInputJson(), "试算输入必须是 JSON 对象");
        Date now = DateUtils.getNowDate();
        String operator = firstNonBlank(SecurityUtils.getUsername(), "system");
        CostSimulationRecord record = new CostSimulationRecord();
        record.setSceneId(snapshot.sceneId);
        record.setVersionId(snapshot.versionId);
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
            return selectSimulationDetail(record.getSimulationId());
        }
        catch (Exception e)
        {
            record.setVariableJson(writeJson(Collections.emptyMap()));
            record.setExplainJson(writeJson(Collections.singletonMap("error", e.getMessage())));
            record.setResultJson(writeJson(Collections.emptyMap()));
            record.setStatus(SIMULATION_STATUS_FAILED);
            record.setErrorMessage(limitLength(e.getMessage(), 1000));
            simulationRecordMapper.insert(record);
            throw e instanceof ServiceException ? (ServiceException) e : new ServiceException("试算执行失败：" + e.getMessage());
        }
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
        String operator = firstNonBlank(SecurityUtils.getUsername(), "system");
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
        task.setErrorMessage("");
        task.setRemark(bo.getRemark());
        task.setCreateBy(operator);
        task.setCreateTime(now);
        task.setUpdateBy(operator);
        task.setUpdateTime(now);
        calcTaskMapper.insert(task);
        markPeriodInProgress(period, task);

        AtomicInteger partitionCounter = new AtomicInteger(1);
        for (Map<String, Object> input : inputs)
        {
            CostCalcTaskDetail detail = new CostCalcTaskDetail();
            detail.setTaskId(task.getTaskId());
            detail.setTaskNo(task.getTaskNo());
            detail.setBizNo(resolveBizNo(input, partitionCounter.get()));
            detail.setPartitionNo(partitionCounter.getAndIncrement());
            detail.setDetailStatus(DETAIL_STATUS_INIT);
            detail.setRetryCount(0);
            detail.setInputJson(writeJson(input));
            detail.setResultSummary("");
            detail.setErrorMessage("");
            calcTaskDetailMapper.insert(detail);
        }
        auditService.recordAudit(snapshot.sceneId, "CALC_TASK", task.getTaskNo(),
                "SUBMIT", "提交正式核算任务", null, task, task.getRequestNo());
        dispatchTaskAfterCommit(task.getTaskId());
        return selectTaskDetail(task.getTaskId());
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
        LinkedHashMap<String, Object> summary = new LinkedHashMap<>();
        summary.put("sourceCount", task.getSourceCount());
        summary.put("successCount", task.getSuccessCount());
        summary.put("failCount", task.getFailCount());
        summary.put("progressPercent", task.getProgressPercent());
        summary.put("detailCount", details.size());
        summary.put("retryableCount", details.stream().filter(item -> DETAIL_STATUS_FAILED.equals(item.getDetailStatus())).count());

        LinkedHashMap<String, Object> result = new LinkedHashMap<>();
        result.put("task", task);
        result.put("summary", summary);
        result.put("details", details);
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
                .set(CostCalcTask::getUpdateBy, firstNonBlank(SecurityUtils.getUsername(), "system"))
                .set(CostCalcTask::getUpdateTime, DateUtils.getNowDate()));
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
            int total = details.isEmpty() ? 1 : details.size();
            int processed = 0;
            int success = 0;
            int failed = 0;
            for (CostCalcTaskDetail detail : details)
            {
                CostCalcTask latestTask = calcTaskMapper.selectById(taskId);
                if (latestTask == null || TASK_STATUS_CANCELLED.equals(latestTask.getTaskStatus()))
                {
                    break;
                }
                try
                {
                    processTaskDetail(latestTask, detail, snapshot);
                    success++;
                }
                catch (Exception e)
                {
                    failed++;
                    createTaskAlarm(latestTask, detail, "TASK_DETAIL_FAILED", "WARN",
                            "任务明细执行失败", "业务单号 " + detail.getBizNo() + " 执行失败：" + limitLength(e.getMessage(), 300));
                    calcTaskDetailMapper.update(null, Wrappers.<CostCalcTaskDetail>lambdaUpdate()
                            .eq(CostCalcTaskDetail::getDetailId, detail.getDetailId())
                            .set(CostCalcTaskDetail::getDetailStatus, DETAIL_STATUS_FAILED)
                            .set(CostCalcTaskDetail::getErrorMessage, limitLength(e.getMessage(), 1000))
                            .set(CostCalcTaskDetail::getResultSummary, "执行失败"));
                }
                processed++;
                refreshTaskProgress(taskId, total, processed, success, failed);
            }
            finishTask(taskId, startedTime, success, failed);
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

    private void refreshTaskProgress(Long taskId, int total, int processed, int success, int failed)
    {
        BigDecimal progress = BigDecimal.valueOf(processed * 100.0 / total).setScale(2, RoundingMode.HALF_UP);
        calcTaskMapper.update(null, Wrappers.<CostCalcTask>lambdaUpdate()
                .eq(CostCalcTask::getTaskId, taskId)
                .set(CostCalcTask::getSuccessCount, success)
                .set(CostCalcTask::getFailCount, failed)
                .set(CostCalcTask::getProgressPercent, progress)
                .set(CostCalcTask::getUpdateTime, DateUtils.getNowDate()));
    }

    private void finishTask(Long taskId, Date startedTime, int success, int failed)
    {
        String status = failed <= 0 ? TASK_STATUS_SUCCESS : (success > 0 ? TASK_STATUS_PART_SUCCESS : TASK_STATUS_FAILED);
        Date finishedTime = DateUtils.getNowDate();
        calcTaskMapper.update(null, Wrappers.<CostCalcTask>lambdaUpdate()
                .eq(CostCalcTask::getTaskId, taskId)
                .set(CostCalcTask::getTaskStatus, status)
                .set(CostCalcTask::getSuccessCount, success)
                .set(CostCalcTask::getFailCount, failed)
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
                        "任务 " + task.getTaskNo() + " 完成状态为 " + status + "，成功 " + success + " 条，失败 " + failed + " 条。");
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
        LinkedHashMap<String, Object> baseContext = new LinkedHashMap<>(input);
        baseContext.put("billMonth", billMonth);
        baseContext.put("sceneCode", snapshot.sceneCode);
        baseContext.put("versionNo", snapshot.versionNo);
        LinkedHashMap<String, Object> variableValues = computeVariables(snapshot, baseContext);
        List<FeeExecutionResult> feeResults = new ArrayList<>();
        List<Map<String, Object>> timeline = new ArrayList<>();

        for (RuntimeFee fee : snapshot.fees)
        {
            List<RuntimeRule> rules = snapshot.rulesByFeeCode.getOrDefault(fee.feeCode, Collections.emptyList());
            RuleMatchResult matchResult = matchRule(rules, variableValues, baseContext);
            if (matchResult == null)
            {
                timeline.add(buildStep("FEE_SKIP", fee.feeCode, fee.feeName, "当前费用未命中任何启用规则"));
                continue;
            }
            PricingResult pricingResult = calculateAmount(matchResult.rule, matchResult.tier, variableValues, baseContext);
            FeeExecutionResult feeResult = new FeeExecutionResult();
            feeResult.feeId = fee.feeId;
            feeResult.feeCode = fee.feeCode;
            feeResult.feeName = fee.feeName;
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
            timeline.add(buildStep("FEE_RESULT", fee.feeCode, fee.feeName,
                    String.format(Locale.ROOT, "命中规则 %s，金额 %s", feeResult.ruleCode, feeResult.amountValue)));
        }

        LinkedHashMap<String, Object> resultView = new LinkedHashMap<>();
        resultView.put("taskNo", taskNo);
        resultView.put("sceneCode", snapshot.sceneCode);
        resultView.put("versionNo", snapshot.versionNo);
        resultView.put("bizNo", resolveBizNo(input, 1));
        resultView.put("feeResults", feeResults.stream().map(FeeExecutionResult::toView).collect(Collectors.toList()));
        resultView.put("amountTotal", feeResults.stream().map(item -> item.amountValue).reduce(BigDecimal.ZERO, BigDecimal::add));

        LinkedHashMap<String, Object> explainView = new LinkedHashMap<>();
        explainView.put("timeline", timeline);
        explainView.put("matchedFees", feeResults.stream().map(FeeExecutionResult::toExplainView).collect(Collectors.toList()));

        ExecutionResult result = new ExecutionResult();
        result.variableView = variableValues;
        result.resultView = resultView;
        result.explainView = explainView;
        result.feeResults = feeResults;
        return result;
    }

    private LinkedHashMap<String, Object> computeVariables(RuntimeSnapshot snapshot, Map<String, Object> baseContext)
    {
        LinkedHashMap<String, Object> values = new LinkedHashMap<>();
        for (RuntimeVariable variable : snapshot.variables)
        {
            Object value;
            if (SOURCE_TYPE_FORMULA.equals(variable.sourceType) && StringUtils.isNotEmpty(variable.formulaExpr))
            {
                value = evaluateExpression(variable.formulaExpr, mergeContext(baseContext, values));
            }
            else
            {
                value = resolveValueFromInput(baseContext, variable.dataPath, variable.variableCode, variable.defaultValue);
            }
            values.put(variable.variableCode, convertValueByType(value, variable.dataType, variable.defaultValue));
        }
        return values;
    }

    private RuleMatchResult matchRule(List<RuntimeRule> rules, Map<String, Object> variableValues, Map<String, Object> baseContext)
    {
        for (RuntimeRule rule : rules)
        {
            List<Map<String, Object>> conditionExplain = new ArrayList<>();
            boolean matched = matchConditions(rule.conditions, rule.conditionLogic, variableValues, baseContext, conditionExplain);
            if (matched)
            {
                RuleMatchResult result = new RuleMatchResult();
                result.rule = rule;
                result.conditionExplain = conditionExplain;
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
        return null;
    }

    private boolean matchConditions(List<RuntimeCondition> conditions, String conditionLogic,
            Map<String, Object> variableValues, Map<String, Object> baseContext, List<Map<String, Object>> explain)
    {
        if (conditions.isEmpty())
        {
            return true;
        }
        Map<Integer, List<RuntimeCondition>> grouped = conditions.stream()
                .collect(Collectors.groupingBy(item -> item.groupNo, LinkedHashMap::new, Collectors.toList()));
        List<Boolean> groupResults = new ArrayList<>();
        for (Map.Entry<Integer, List<RuntimeCondition>> entry : grouped.entrySet())
        {
            boolean groupPass = true;
            for (RuntimeCondition condition : entry.getValue())
            {
                Object leftValue = variableValues.get(condition.variableCode);
                boolean pass = evaluateCondition(condition, leftValue, mergeContext(baseContext, variableValues));
                LinkedHashMap<String, Object> item = new LinkedHashMap<>();
                item.put("groupNo", entry.getKey());
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
        }
        if ("OR".equalsIgnoreCase(conditionLogic))
        {
            return groupResults.stream().anyMatch(Boolean::booleanValue);
        }
        return groupResults.stream().allMatch(Boolean::booleanValue);
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

    private PricingResult calculateAmount(RuntimeRule rule, RuntimeTier tier, Map<String, Object> variableValues, Map<String, Object> baseContext)
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
        if (RULE_TYPE_FIXED_RATE.equals(rule.ruleType))
        {
            BigDecimal unitPrice = toBigDecimal(rule.pricingConfig.get("rateValue"));
            result.unitPrice = defaultZero(unitPrice).setScale(6, RoundingMode.HALF_UP);
            result.amountValue = result.unitPrice.multiply(result.quantityValue).setScale(2, RoundingMode.HALF_UP);
            result.pricingExplain.put("pricingSource", "FIXED_RATE");
        }
        else if (RULE_TYPE_FIXED_AMOUNT.equals(rule.ruleType))
        {
            BigDecimal amountValue = toBigDecimal(rule.pricingConfig.get("amountValue"));
            result.unitPrice = defaultZero(amountValue).setScale(6, RoundingMode.HALF_UP);
            result.amountValue = defaultZero(amountValue).setScale(2, RoundingMode.HALF_UP);
            result.pricingExplain.put("pricingSource", "FIXED_AMOUNT");
        }
        else if (RULE_TYPE_FORMULA.equals(rule.ruleType))
        {
            Object amountValue = evaluateExpression(rule.amountFormula, mergeContext(baseContext, variableValues));
            BigDecimal computed = defaultZero(toBigDecimal(amountValue)).setScale(2, RoundingMode.HALF_UP);
            result.unitPrice = computed.setScale(6, RoundingMode.HALF_UP);
            result.amountValue = computed;
            result.pricingExplain.put("pricingSource", "FORMULA");
            result.pricingExplain.put("formula", rule.amountFormula);
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
                String.format(Locale.ROOT, "数量 %s，单价 %s，金额 %s", pricingResult.quantityValue, pricingResult.unitPrice, pricingResult.amountValue)));
        return steps;
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

    private RuntimeSnapshot loadRuntimeSnapshot(Long sceneId, Long versionId, boolean requireFormalVersion)
    {
        CostScene scene = sceneMapper.selectById(sceneId);
        if (scene == null)
        {
            throw new ServiceException("场景不存在，请刷新后重试");
        }
        Long targetVersionId = versionId == null ? scene.getActiveVersionId() : versionId;
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
                return objectMapper.readValue(cached, RuntimeSnapshot.class);
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
        for (CostPublishSnapshot item : snapshots)
        {
            Map<String, Object> json = parseJsonMap(item.getSnapshotJson());
            if ("FEE".equals(item.getSnapshotType()))
            {
                RuntimeFee fee = new RuntimeFee();
                fee.feeId = findFeeIdByCode(sceneId, stringValue(json.get("feeCode")));
                fee.feeCode = stringValue(json.get("feeCode"));
                fee.feeName = stringValue(json.get("feeName"));
                fee.objectDimension = stringValue(json.get("objectDimension"));
                fee.sortNo = intValue(json.get("sortNo"));
                snapshot.fees.add(fee);
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
                variable.defaultValue = json.get("defaultValue");
                variable.sortNo = intValue(json.get("sortNo"));
                snapshot.variables.add(variable);
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
                rule.pricingConfig = json.get("pricingJson") instanceof Map ? castMap(json.get("pricingJson")) : new LinkedHashMap<>();
                rule.amountFormula = stringValue(json.get("amountFormula"));
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

        snapshot.fees.sort(Comparator.comparingInt(item -> item.sortNo == null ? 9999 : item.sortNo));
        snapshot.variables.sort(Comparator.comparingInt(item -> item.sortNo == null ? 9999 : item.sortNo));
        for (RuntimeRule rule : snapshot.rulesByCode.values())
        {
            rule.conditions = snapshot.conditionsByRuleCode.getOrDefault(rule.ruleCode, Collections.emptyList()).stream()
                    .sorted(Comparator.comparingInt((RuntimeCondition item) -> item.groupNo == null ? 1 : item.groupNo)
                            .thenComparingInt(item -> item.sortNo == null ? 9999 : item.sortNo))
                    .collect(Collectors.toList());
            rule.tiers = snapshot.tiersByRuleCode.getOrDefault(rule.ruleCode, Collections.emptyList()).stream()
                    .sorted(Comparator.comparingInt(item -> item.tierNo == null ? 9999 : item.tierNo))
                    .collect(Collectors.toList());
            snapshot.rulesByFeeCode.computeIfAbsent(rule.feeCode, key -> new ArrayList<>()).add(rule);
        }
        for (List<RuntimeRule> rules : snapshot.rulesByFeeCode.values())
        {
            rules.sort(Comparator.comparingInt((RuntimeRule item) -> item.priority == null ? 0 : item.priority).reversed()
                    .thenComparingInt(item -> item.sortNo == null ? 9999 : item.sortNo));
        }
        try
        {
            redisCache.setCacheObject(cacheKey, objectMapper.writeValueAsString(snapshot), 30, TimeUnit.MINUTES);
        }
        catch (JsonProcessingException ignored)
        {
        }
        return snapshot;
    }

    private List<CostCalcTask> selectTaskListInternal(CostCalcTask query)
    {
        return calcTaskMapper.selectList(Wrappers.<CostCalcTask>lambdaQuery()
                .eq(query.getSceneId() != null, CostCalcTask::getSceneId, query.getSceneId())
                .eq(query.getVersionId() != null, CostCalcTask::getVersionId, query.getVersionId())
                .eq(StringUtils.isNotEmpty(query.getTaskType()), CostCalcTask::getTaskType, query.getTaskType())
                .eq(StringUtils.isNotEmpty(query.getTaskStatus()), CostCalcTask::getTaskStatus, query.getTaskStatus())
                .eq(StringUtils.isNotEmpty(query.getBillMonth()), CostCalcTask::getBillMonth, query.getBillMonth())
                .like(StringUtils.isNotEmpty(query.getTaskNo()), CostCalcTask::getTaskNo, query.getTaskNo())
                .orderByDesc(CostCalcTask::getTaskId));
    }

    private List<CostResultLedger> selectResultListInternal(CostResultLedger query)
    {
        return resultLedgerMapper.selectList(Wrappers.<CostResultLedger>lambdaQuery()
                .eq(query.getSceneId() != null, CostResultLedger::getSceneId, query.getSceneId())
                .eq(query.getVersionId() != null, CostResultLedger::getVersionId, query.getVersionId())
                .eq(StringUtils.isNotEmpty(query.getBillMonth()), CostResultLedger::getBillMonth, query.getBillMonth())
                .eq(StringUtils.isNotEmpty(query.getTaskNo()), CostResultLedger::getTaskNo, query.getTaskNo())
                .eq(StringUtils.isNotEmpty(query.getFeeCode()), CostResultLedger::getFeeCode, query.getFeeCode())
                .eq(StringUtils.isNotEmpty(query.getBizNo()), CostResultLedger::getBizNo, query.getBizNo())
                .eq(StringUtils.isNotEmpty(query.getResultStatus()), CostResultLedger::getResultStatus, query.getResultStatus())
                .orderByDesc(CostResultLedger::getResultId));
    }

    private void enrichSimulationRecords(List<CostSimulationRecord> records)
    {
        if (records == null || records.isEmpty())
        {
            return;
        }
        Map<Long, CostScene> sceneMap = sceneMapper.selectBatchIds(records.stream().map(CostSimulationRecord::getSceneId).filter(Objects::nonNull).collect(Collectors.toSet()))
                .stream().collect(Collectors.toMap(CostScene::getSceneId, item -> item));
        Map<Long, CostPublishVersion> versionMap = publishVersionMapper.selectBatchIds(records.stream().map(CostSimulationRecord::getVersionId).filter(Objects::nonNull).collect(Collectors.toSet()))
                .stream().collect(Collectors.toMap(CostPublishVersion::getVersionId, item -> item));
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
        Map<Long, CostPublishVersion> versionMap = publishVersionMapper.selectBatchIds(tasks.stream().map(CostCalcTask::getVersionId).filter(Objects::nonNull).collect(Collectors.toSet()))
                .stream().collect(Collectors.toMap(CostPublishVersion::getVersionId, item -> item));
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

    private void enrichResults(List<CostResultLedger> results)
    {
        if (results == null || results.isEmpty())
        {
            return;
        }
        Map<Long, CostScene> sceneMap = sceneMapper.selectBatchIds(results.stream().map(CostResultLedger::getSceneId).filter(Objects::nonNull).collect(Collectors.toSet()))
                .stream().collect(Collectors.toMap(CostScene::getSceneId, item -> item));
        Map<Long, CostPublishVersion> versionMap = publishVersionMapper.selectBatchIds(results.stream().map(CostResultLedger::getVersionId).filter(Objects::nonNull).collect(Collectors.toSet()))
                .stream().collect(Collectors.toMap(CostPublishVersion::getVersionId, item -> item));
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
        }
    }

    private List<Map<String, Object>> parseTaskInput(CostCalcTaskSubmitBo bo)
    {
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

    private void validateBillMonth(String billMonth)
    {
        if (!billMonth.matches("\\d{4}-\\d{2}"))
        {
            throw new ServiceException("账期格式必须为 yyyy-MM");
        }
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
            StandardEvaluationContext evaluationContext = new StandardEvaluationContext(context);
            evaluationContext.addPropertyAccessor(new MapAccessor());
            context.forEach(evaluationContext::setVariable);
            return expressionParser.parseExpression(rewriteExpression(expression)).getValue(evaluationContext);
        }
        catch (Exception e)
        {
            throw new ServiceException("表达式执行失败：" + expression);
        }
    }

    private String rewriteExpression(String expression)
    {
        return expression.replace("&&", " and ").replace("||", " or ");
    }

    private Map<String, Object> mergeContext(Map<String, Object> left, Map<String, Object> right)
    {
        LinkedHashMap<String, Object> context = new LinkedHashMap<>();
        if (left != null)
        {
            context.putAll(left);
        }
        if (right != null)
        {
            context.putAll(right);
        }
        return context;
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
        if (value == null)
        {
            return defaultValue;
        }
        if (DATA_TYPE_NUMBER.equals(dataType))
        {
            return toBigDecimal(value);
        }
        if (DATA_TYPE_BOOLEAN.equals(dataType))
        {
            return convertBoolean(value);
        }
        if (DATA_TYPE_JSON.equals(dataType) && value instanceof String)
        {
            return parseJsonToObject(String.valueOf(value));
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
            String operator = firstNonBlank(SecurityUtils.getUsername(), "system");
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
                    .set(CostBillPeriod::getUpdateBy, firstNonBlank(SecurityUtils.getUsername(), "system"))
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
                .set(CostBillPeriod::getUpdateBy, firstNonBlank(SecurityUtils.getUsername(), "system"))
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
                .set(CostBillPeriod::getUpdateBy, firstNonBlank(SecurityUtils.getUsername(), "system"))
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
                .set(CostRecalcOrder::getUpdateBy, firstNonBlank(SecurityUtils.getUsername(), "system"))
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
        public List<RuntimeFee> fees = new ArrayList<>();
        public List<RuntimeVariable> variables = new ArrayList<>();
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
        public Object defaultValue;
        public Integer sortNo;
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
        public Map<String, Object> pricingConfig;
        public String amountFormula;
        public Integer sortNo;
        public List<RuntimeCondition> conditions = Collections.emptyList();
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

    private static class RuleMatchResult
    {
        private RuntimeRule rule;
        private RuntimeTier tier;
        private List<Map<String, Object>> conditionExplain;
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
            item.put("ruleCode", ruleCode);
            item.put("ruleName", ruleName);
            item.put("conditions", conditionExplain);
            item.put("pricing", pricingExplain);
            item.put("timeline", timelineSteps);
            return item;
        }
    }

    private static class ExecutionResult
    {
        private Map<String, Object> variableView;
        private Map<String, Object> resultView;
        private Map<String, Object> explainView;
        private List<FeeExecutionResult> feeResults;
    }
}

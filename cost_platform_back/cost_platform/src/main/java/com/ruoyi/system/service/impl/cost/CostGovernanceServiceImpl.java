package com.ruoyi.system.service.impl.cost;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruoyi.common.core.redis.RedisCache;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.system.domain.cost.*;
import com.ruoyi.system.domain.cost.bo.CostBillPeriodSaveBo;
import com.ruoyi.system.domain.cost.bo.CostCalcTaskSubmitBo;
import com.ruoyi.system.domain.cost.bo.CostRecalcApplyBo;
import com.ruoyi.system.domain.cost.bo.CostRecalcApproveBo;
import com.ruoyi.system.mapper.cost.*;
import com.ruoyi.system.service.cost.ICostAlarmService;
import com.ruoyi.system.service.cost.ICostAuditService;
import com.ruoyi.system.service.cost.ICostGovernanceService;
import com.ruoyi.system.service.cost.ICostRunService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 线程六治理增强服务实现
 *
 * @author HwFan
 */
@Service
public class CostGovernanceServiceImpl implements ICostGovernanceService {
    private static final String PERIOD_STATUS_NOT_STARTED = "NOT_STARTED";
    private static final String PERIOD_STATUS_SEALED = "SEALED";
    private static final String RECALC_STATUS_PENDING = "PENDING_APPROVAL";
    private static final String RECALC_STATUS_APPROVED = "APPROVED";
    private static final String RECALC_STATUS_REJECTED = "REJECTED";
    private static final String RECALC_STATUS_RUNNING = "RUNNING";
    private static final String RUNTIME_CACHE_PREFIX = "cost:runtime:snapshot:";
    private static final String CHECK_STATUS_PASS = "PASS";
    private static final String CHECK_STATUS_FAIL = "FAIL";
    private static final String CHECK_STATUS_PENDING = "PENDING";
    private static final List<String> GO_LIVE_REQUIRED_MIGRATIONS = Arrays.asList("20260402.012", "20260402.013", "20260402.014");

    private final ObjectMapper objectMapper = new ObjectMapper();
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private CostBillPeriodMapper billPeriodMapper;
    @Autowired
    private CostRecalcOrderMapper recalcOrderMapper;
    @Autowired
    private CostSceneMapper sceneMapper;
    @Autowired
    private CostPublishVersionMapper publishVersionMapper;
    @Autowired
    private CostCalcTaskMapper calcTaskMapper;
    @Autowired
    private CostCalcTaskDetailMapper calcTaskDetailMapper;
    @Autowired
    private CostResultLedgerMapper resultLedgerMapper;
    @Autowired
    private ICostRunService runService;
    @Autowired
    private ICostAuditService auditService;
    @Autowired
    private ICostAlarmService alarmService;
    @Autowired
    private RedisCache redisCache;

    @Autowired
    private CostDistributedLockSupport distributedLockSupport;

    @Autowired
    public void setJdbcTemplate(@Qualifier("masterDataSource") DataSource masterDataSource) {
        this.jdbcTemplate = new JdbcTemplate(masterDataSource);
    }

    @Override
    public Map<String, Object> selectPeriodStats(Long sceneId) {
        List<CostBillPeriod> periods = selectPeriodList(buildPeriodQuery(sceneId));
        LinkedHashMap<String, Object> stats = new LinkedHashMap<>();
        stats.put("periodCount", periods.size());
        stats.put("sealedCount", periods.stream().filter(item -> PERIOD_STATUS_SEALED.equals(item.getPeriodStatus())).count());
        stats.put("runningCount", periods.stream().filter(item -> "IN_PROGRESS".equals(item.getPeriodStatus())).count());
        stats.put("recalcCount", recalcOrderMapper.selectCount(Wrappers.<CostRecalcOrder>lambdaQuery()
                .eq(sceneId != null, CostRecalcOrder::getSceneId, sceneId)));
        return stats;
    }

    @Override
    public List<CostBillPeriod> selectPeriodList(CostBillPeriod query) {
        List<CostBillPeriod> periods = billPeriodMapper.selectList(Wrappers.<CostBillPeriod>lambdaQuery()
                .eq(query.getSceneId() != null, CostBillPeriod::getSceneId, query.getSceneId())
                .eq(StringUtils.isNotEmpty(query.getPeriodStatus()), CostBillPeriod::getPeriodStatus, query.getPeriodStatus())
                .like(StringUtils.isNotEmpty(query.getBillMonth()), CostBillPeriod::getBillMonth, query.getBillMonth())
                .orderByDesc(CostBillPeriod::getBillMonth)
                .orderByDesc(CostBillPeriod::getPeriodId));
        enrichPeriods(periods);
        return periods;
    }

    @Override
    public Map<String, Object> selectPeriodDetail(Long periodId) {
        CostBillPeriod period = requirePeriod(periodId);
        enrichPeriods(Collections.singletonList(period));
        LinkedHashMap<String, Object> result = new LinkedHashMap<>();
        result.put("period", period);
        result.put("recalcOrders", selectRecalcList(buildRecalcQuery(period.getSceneId(), period.getBillMonth())));
        result.put("resultStats", buildResultStats(period.getSceneId(), period.getBillMonth(), period.getLastTaskId()));
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int createPeriod(CostBillPeriodSaveBo bo) {
        CostScene scene = requireScene(bo.getSceneId());
        ensureBillMonth(bo.getBillMonth());
        if (billPeriodMapper.selectCount(Wrappers.<CostBillPeriod>lambdaQuery()
                .eq(CostBillPeriod::getSceneId, bo.getSceneId())
                .eq(CostBillPeriod::getBillMonth, bo.getBillMonth())) > 0) {
            throw new ServiceException("当前场景账期已存在，请勿重复创建");
        }
        if (bo.getActiveVersionId() != null) {
            requireVersion(bo.getActiveVersionId());
        }
        CostBillPeriod period = new CostBillPeriod();
        period.setSceneId(scene.getSceneId());
        period.setBillMonth(bo.getBillMonth());
        period.setPeriodStatus(PERIOD_STATUS_NOT_STARTED);
        period.setActiveVersionId(bo.getActiveVersionId());
        period.setResultCount(0L);
        period.setAmountTotal(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
        period.setRemark(firstNonBlank(bo.getRemark(), ""));
        period.setCreateBy(firstNonBlank(SecurityUtils.getUsername(), "system"));
        period.setCreateTime(DateUtils.getNowDate());
        period.setUpdateBy(period.getCreateBy());
        period.setUpdateTime(period.getCreateTime());
        billPeriodMapper.insert(period);
        auditService.recordAudit(scene.getSceneId(), "BILL_PERIOD", scene.getSceneCode() + ":" + bo.getBillMonth(),
                "CREATE", "新建账期", null, period, "");
        return 1;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int sealPeriod(Long periodId) {
        CostBillPeriod before = requirePeriod(periodId);
        if (PERIOD_STATUS_SEALED.equals(before.getPeriodStatus())) {
            return 0;
        }
        Date now = DateUtils.getNowDate();
        int rows = billPeriodMapper.update(null, Wrappers.<CostBillPeriod>lambdaUpdate()
                .eq(CostBillPeriod::getPeriodId, periodId)
                .set(CostBillPeriod::getPeriodStatus, PERIOD_STATUS_SEALED)
                .set(CostBillPeriod::getSealedBy, firstNonBlank(SecurityUtils.getUsername(), "system"))
                .set(CostBillPeriod::getSealedTime, now)
                .set(CostBillPeriod::getUpdateBy, firstNonBlank(SecurityUtils.getUsername(), "system"))
                .set(CostBillPeriod::getUpdateTime, now));
        auditService.recordAudit(before.getSceneId(), "BILL_PERIOD",
                before.getSceneId() + ":" + before.getBillMonth(), "SEAL", "封存账期", before,
                billPeriodMapper.selectById(periodId), "");
        return rows;
    }

    @Override
    public List<CostRecalcOrder> selectRecalcList(CostRecalcOrder query) {
        List<CostRecalcOrder> orders = recalcOrderMapper.selectList(Wrappers.<CostRecalcOrder>lambdaQuery()
                .eq(query.getSceneId() != null, CostRecalcOrder::getSceneId, query.getSceneId())
                .eq(StringUtils.isNotEmpty(query.getBillMonth()), CostRecalcOrder::getBillMonth, query.getBillMonth())
                .eq(query.getVersionId() != null, CostRecalcOrder::getVersionId, query.getVersionId())
                .eq(StringUtils.isNotEmpty(query.getRecalcStatus()), CostRecalcOrder::getRecalcStatus, query.getRecalcStatus())
                .orderByDesc(CostRecalcOrder::getCreateTime)
                .orderByDesc(CostRecalcOrder::getRecalcId));
        enrichRecalcOrders(orders);
        return orders;
    }

    @Override
    public Map<String, Object> selectRecalcDetail(Long recalcId) {
        CostRecalcOrder order = requireRecalc(recalcId);
        enrichRecalcOrders(Collections.singletonList(order));
        LinkedHashMap<String, Object> result = new LinkedHashMap<>();
        result.put("order", order);
        result.put("diffSummary", parseJson(order.getDiffSummaryJson()));
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int applyRecalc(CostRecalcApplyBo bo) {
        CostScene scene = requireScene(bo.getSceneId());
        CostBillPeriod period = requirePeriod(scene.getSceneId(), bo.getBillMonth());
        CostPublishVersion version = requireVersion(bo.getVersionId());
        CostCalcTask baselineTask = requireTask(bo.getBaselineTaskId());
        if (!Objects.equals(scene.getSceneId(), baselineTask.getSceneId()) || !StringUtils.equals(bo.getBillMonth(), baselineTask.getBillMonth())) {
            throw new ServiceException("基准任务必须属于当前场景与账期");
        }
        CostRecalcOrder order = new CostRecalcOrder();
        order.setSceneId(scene.getSceneId());
        order.setBillMonth(bo.getBillMonth());
        order.setVersionId(version.getVersionId());
        order.setPeriodId(period.getPeriodId());
        order.setBaselineTaskId(baselineTask.getTaskId());
        order.setBaselineTaskNo(baselineTask.getTaskNo());
        order.setRecalcStatus(RECALC_STATUS_PENDING);
        order.setApplyReason(bo.getApplyReason());
        order.setRequestNo(firstNonBlank(bo.getRequestNo(), ""));
        order.setRemark(firstNonBlank(bo.getRemark(), ""));
        order.setDiffAmount(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
        order.setCreateBy(firstNonBlank(SecurityUtils.getUsername(), "system"));
        order.setCreateTime(DateUtils.getNowDate());
        order.setUpdateBy(order.getCreateBy());
        order.setUpdateTime(order.getCreateTime());
        recalcOrderMapper.insert(order);
        auditService.recordAudit(scene.getSceneId(), "RECALC", String.valueOf(order.getRecalcId()),
                "APPLY", "发起重算申请", null, order, bo.getRequestNo());
        return 1;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int approveRecalc(Long recalcId, CostRecalcApproveBo bo) {
        CostRecalcOrder before = requireRecalc(recalcId);
        boolean approved = Boolean.TRUE.equals(bo.getApproved());
        String status = approved ? RECALC_STATUS_APPROVED : RECALC_STATUS_REJECTED;
        Date now = DateUtils.getNowDate();
        int rows = recalcOrderMapper.update(null, Wrappers.<CostRecalcOrder>lambdaUpdate()
                .eq(CostRecalcOrder::getRecalcId, recalcId)
                .set(CostRecalcOrder::getRecalcStatus, status)
                .set(CostRecalcOrder::getApproveOpinion, firstNonBlank(bo.getApproveOpinion(), ""))
                .set(CostRecalcOrder::getApproveBy, firstNonBlank(SecurityUtils.getUsername(), "system"))
                .set(CostRecalcOrder::getApproveTime, now)
                .set(CostRecalcOrder::getUpdateBy, firstNonBlank(SecurityUtils.getUsername(), "system"))
                .set(CostRecalcOrder::getUpdateTime, now));
        auditService.recordAudit(before.getSceneId(), "RECALC", String.valueOf(before.getRecalcId()),
                approved ? "APPROVE" : "REJECT", approved ? "审核通过重算" : "驳回重算", before,
                recalcOrderMapper.selectById(recalcId), "");
        return rows;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @SuppressWarnings("unchecked")
    public int executeRecalc(Long recalcId) {
        CostRecalcOrder before = requireRecalc(recalcId);
        if (!RECALC_STATUS_APPROVED.equals(before.getRecalcStatus())) {
            throw new ServiceException("只有审核通过的重算申请才能执行");
        }
        CostCalcTask baselineTask = requireTask(before.getBaselineTaskId());
        List<CostCalcTaskDetail> details = calcTaskDetailMapper.selectList(Wrappers.<CostCalcTaskDetail>lambdaQuery()
                .eq(CostCalcTaskDetail::getTaskId, baselineTask.getTaskId())
                .orderByAsc(CostCalcTaskDetail::getPartitionNo));
        if (details.isEmpty()) {
            throw new ServiceException("基准任务没有可重算的输入明细");
        }
        List<String> inputs = details.stream().map(CostCalcTaskDetail::getInputJson).collect(Collectors.toList());
        CostCalcTaskSubmitBo submitBo = new CostCalcTaskSubmitBo();
        submitBo.setSceneId(before.getSceneId());
        submitBo.setVersionId(before.getVersionId());
        submitBo.setBillMonth(before.getBillMonth());
        submitBo.setTaskType(details.size() == 1 ? "FORMAL_SINGLE" : "FORMAL_BATCH");
        submitBo.setRequestNo(firstNonBlank(before.getRequestNo(), "RECALC-" + before.getRecalcId()));
        submitBo.setRemark("重算申请#" + before.getRecalcId());
        submitBo.setInputJson(details.size() == 1 ? inputs.get(0) : "[" + String.join(",", inputs) + "]");
        Map<String, Object> taskResult = runService.submitTask(submitBo);
        Map<String, Object> taskMap = taskResult == null ? Collections.emptyMap() : (Map<String, Object>) taskResult.get("task");
        Date now = DateUtils.getNowDate();
        int rows = recalcOrderMapper.update(null, Wrappers.<CostRecalcOrder>lambdaUpdate()
                .eq(CostRecalcOrder::getRecalcId, recalcId)
                .set(CostRecalcOrder::getRecalcStatus, RECALC_STATUS_RUNNING)
                .set(CostRecalcOrder::getTargetTaskId, longValue(taskMap.get("taskId")))
                .set(CostRecalcOrder::getTargetTaskNo, stringValue(taskMap.get("taskNo")))
                .set(CostRecalcOrder::getExecuteBy, firstNonBlank(SecurityUtils.getUsername(), "system"))
                .set(CostRecalcOrder::getExecuteTime, now)
                .set(CostRecalcOrder::getUpdateBy, firstNonBlank(SecurityUtils.getUsername(), "system"))
                .set(CostRecalcOrder::getUpdateTime, now));
        auditService.recordAudit(before.getSceneId(), "RECALC", String.valueOf(before.getRecalcId()),
                "EXECUTE", "执行重算", before, recalcOrderMapper.selectById(recalcId), submitBo.getRequestNo());
        return rows;
    }

    @Override
    public List<CostAuditLog> selectAuditList(CostAuditLog query) {
        return auditService.selectAuditList(query);
    }

    @Override
    public Map<String, Object> selectAuditStats(CostAuditLog query) {
        return auditService.selectAuditStats(query);
    }

    @Override
    public List<CostAlarmRecord> selectAlarmList(CostAlarmRecord query) {
        return alarmService.selectAlarmList(query);
    }

    @Override
    public Map<String, Object> selectAlarmStats(CostAlarmRecord query) {
        return alarmService.selectAlarmStats(query);
    }

    @Override
    public Map<String, Object> selectAlarmOverview(CostAlarmRecord query) {
        return alarmService.selectAlarmOverview(query);
    }

    @Override
    public int ackAlarm(Long alarmId) {
        return alarmService.ackAlarm(alarmId);
    }

    @Override
    public int resolveAlarm(Long alarmId) {
        return alarmService.resolveAlarm(alarmId);
    }

    @Override
    public Map<String, Object> selectGoLiveReadiness() {
        LinkedHashMap<String, Object> result = new LinkedHashMap<>();
        List<Map<String, Object>> checks = new ArrayList<>();
        try {
            checks.addAll(buildMigrationChecks());
            checks.addAll(buildTableChecks());
            checks.addAll(buildMenuChecks());
        } catch (Exception ex) {
            checks.add(buildCheckItem("SYSTEM", "上线自动校验读取失败", CHECK_STATUS_FAIL,
                    "读取 Flyway 或库表信息失败：" + limitLength(ex.getMessage(), 300),
                    "先检查目标库连接、Flyway 历史表和当前账号只读权限。"));
        }
        checks.addAll(buildManualChecks());

        long passCount = checks.stream().filter(item -> CHECK_STATUS_PASS.equals(item.get("status"))).count();
        long failCount = checks.stream().filter(item -> CHECK_STATUS_FAIL.equals(item.get("status"))).count();
        long pendingCount = checks.stream().filter(item -> CHECK_STATUS_PENDING.equals(item.get("status"))).count();

        LinkedHashMap<String, Object> summary = new LinkedHashMap<>();
        summary.put("totalCount", checks.size());
        summary.put("passCount", passCount);
        summary.put("failCount", failCount);
        summary.put("pendingCount", pendingCount);
        summary.put("ready", failCount == 0 && pendingCount == 0);
        summary.put("sceneCount", sceneMapper.selectCount(null));
        summary.put("versionCount", publishVersionMapper.selectCount(null));
        summary.put("taskCount", calcTaskMapper.selectCount(null));
        summary.put("resultCount", resultLedgerMapper.selectCount(null));

        result.put("summary", summary);
        result.put("checks", checks);
        result.put("latestMigration", safeLatestMigration());
        return result;
    }

    @Override
    public Map<String, Object> selectRuntimeCacheStats(Long sceneId, Long versionId) {
        Collection<String> keys = redisCache.keys(RUNTIME_CACHE_PREFIX + "*");
        LinkedHashMap<String, Object> result = new LinkedHashMap<>();
        result.put("cacheCount", keys == null ? 0 : keys.size());
        result.put("sceneId", sceneId);
        result.put("versionId", versionId);
        if (versionId != null) {
            String cacheKey = RUNTIME_CACHE_PREFIX + versionId;
            result.put("cacheKey", cacheKey);
            result.put("exists", Boolean.TRUE.equals(redisCache.hasKey(cacheKey)));
            result.put("expireSeconds", redisCache.getExpire(cacheKey));
        }
        result.put("cacheKeys", keys == null ? Collections.emptyList() : new ArrayList<>(keys));
        return result;
    }

    @Override
    public int refreshRuntimeCache(Long sceneId, Long versionId) {
        return distributedLockSupport.executeRuntimeCacheLock(sceneId, versionId,
                "当前正在刷新运行缓存，请稍后重试", () ->
                {
                    try {
                        List<String> keys = new ArrayList<>();
                        if (versionId != null) {
                            keys.add(RUNTIME_CACHE_PREFIX + versionId);
                        } else if (sceneId != null) {
                            List<CostPublishVersion> versions = publishVersionMapper.selectList(Wrappers.<CostPublishVersion>lambdaQuery()
                                    .eq(CostPublishVersion::getSceneId, sceneId));
                            keys.addAll(versions.stream().map(item -> RUNTIME_CACHE_PREFIX + item.getVersionId()).collect(Collectors.toList()));
                        } else {
                            Collection<String> allKeys = redisCache.keys(RUNTIME_CACHE_PREFIX + "*");
                            if (allKeys != null) {
                                keys.addAll(allKeys);
                            }
                        }
                        if (!keys.isEmpty()) {
                            redisCache.deleteObject(keys);
                        }
                        auditService.recordAudit(sceneId, "CACHE", versionId == null ? "RUNTIME" : String.valueOf(versionId),
                                "REFRESH", "刷新运行快照缓存", null, keys, "");
                        if (versionId != null || sceneId != null) {
                            String sourceKey = "CACHE:" + (versionId == null ? String.valueOf(sceneId) : versionId);
                            alarmService.autoResolveBySourceKey(sourceKey, "缓存刷新成功，自动关闭历史缓存告警");
                        }
                        return 1;
                    } catch (Exception e) {
                        CostAlarmRecord alarm = new CostAlarmRecord();
                        alarm.setSceneId(sceneId);
                        alarm.setVersionId(versionId);
                        alarm.setAlarmType("CACHE_REFRESH_FAILED");
                        alarm.setAlarmLevel("ERROR");
                        alarm.setAlarmTitle("运行快照缓存刷新失败");
                        alarm.setAlarmContent(limitLength(e.getMessage(), 1000));
                        alarm.setSourceKey("CACHE:" + (versionId == null ? String.valueOf(sceneId) : versionId));
                        alarmService.createAlarm(alarm);
                        throw e;
                    }
                });
    }

    /**
     * 构建 Flyway 迁移校验项，优先确认线程五新增脚本是否已经在目标库生效。
     */
    private List<Map<String, Object>> buildMigrationChecks() {
        List<Map<String, Object>> appliedVersions = jdbcTemplate.queryForList(
                "select version from flyway_schema_history where success = 1");
        Set<String> versionSet = appliedVersions.stream()
                .map(item -> stringValue(item.get("version")))
                .collect(Collectors.toSet());
        List<Map<String, Object>> checks = new ArrayList<>();
        GO_LIVE_REQUIRED_MIGRATIONS.forEach(version -> checks.add(buildCheckItem(
                "FLYWAY",
                "迁移脚本 " + version,
                versionSet.contains(version) ? CHECK_STATUS_PASS : CHECK_STATUS_FAIL,
                versionSet.contains(version) ? "目标库已记录该版本迁移。" : "目标库未发现该版本迁移记录，请先执行 Flyway。",
                "核对 flyway_schema_history 与应用启动日志，确认迁移按顺序执行。")));
        return checks;
    }

    /**
     * 构建运行链关键表校验项，避免迁移成功但关键库表缺失。
     */
    private List<Map<String, Object>> buildTableChecks() {
        List<Map<String, Object>> checks = new ArrayList<>();
        checks.add(buildTableCheck("cost_calc_task_partition", "正式核算分片表"));
        checks.add(buildTableCheck("cost_calc_input_batch", "导入批次头表"));
        checks.add(buildTableCheck("cost_calc_input_batch_item", "导入批次明细表"));
        return checks;
    }

    /**
     * 构建菜单校验项，帮助在目标环境快速确认 Flyway 菜单补丁是否生效。
     */
    private List<Map<String, Object>> buildMenuChecks() {
        List<Map<String, Object>> checks = new ArrayList<>();
        checks.add(buildMenuCheck("/cost/task", "正式核算菜单"));
        checks.add(buildMenuCheck("/cost/taskBatch", "导入批次菜单"));
        checks.add(buildMenuCheck("/cost/result", "结果台账菜单"));
        checks.add(buildMenuCheck("/cost/alert", "告警中心菜单"));
        return checks;
    }

    /**
     * 构建仍需人工执行的校验项，将最后两项上线阻塞清晰暴露给业务和测试。
     */
    private List<Map<String, Object>> buildManualChecks() {
        List<Map<String, Object>> checks = new ArrayList<>();
        checks.add(buildCheckItem("MANUAL", "正式全链人工回归", CHECK_STATUS_PENDING,
                "代码链路已具备，仍需按上线回归清单完成真实场景联调与证据沉淀。",
                "按《正式核算上线回归清单》逐条执行，并回填执行结果与截图链接。"));
        checks.add(buildCheckItem("MANUAL", "普通角色权限回归", CHECK_STATUS_PENDING,
                "按钮和接口都已加权限控制，仍需用普通角色实测菜单与按钮可见性。",
                "使用最小权限账号验证任务、告警、缓存刷新、分片重试等入口。"));
        return checks;
    }

    /**
     * 构建单个库表存在性校验。
     */
    private Map<String, Object> buildTableCheck(String tableName, String displayName) {
        Integer tableCount = jdbcTemplate.queryForObject(
                "select count(1) from information_schema.tables where table_schema = (select database()) and table_name = ?",
                Integer.class, tableName);
        boolean exists = tableCount != null && tableCount > 0;
        return buildCheckItem("TABLE", displayName, exists ? CHECK_STATUS_PASS : CHECK_STATUS_FAIL,
                exists ? "目标库已存在表 `" + tableName + "`。" : "目标库缺少表 `" + tableName + "`。",
                "执行 Flyway 后重新刷新本页，确认表结构已同步。");
    }

    /**
     * 构建菜单存在性校验。
     */
    private Map<String, Object> buildMenuCheck(String menuPath, String displayName) {
        Integer menuCount = jdbcTemplate.queryForObject(
                "select count(1) from sys_menu where path = ?",
                Integer.class, menuPath);
        boolean exists = menuCount != null && menuCount > 0;
        return buildCheckItem("MENU", displayName, exists ? CHECK_STATUS_PASS : CHECK_STATUS_FAIL,
                exists ? "系统菜单已存在路径 `" + menuPath + "`。" : "系统菜单缺少路径 `" + menuPath + "`。",
                "执行菜单迁移并重新登录后台，确认菜单数据与权限同时生效。");
    }

    /**
     * 查询最近一次已成功执行的 Flyway 记录，方便快速观察目标环境停留在哪个版本。
     */
    private Map<String, Object> safeLatestMigration() {
        try {
            List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                    "select version, description, installed_on from flyway_schema_history where success = 1 order by installed_rank desc limit 1");
            if (rows.isEmpty()) {
                return Collections.emptyMap();
            }
            Map<String, Object> row = rows.get(0);
            LinkedHashMap<String, Object> latest = new LinkedHashMap<>();
            latest.put("version", stringValue(row.get("version")));
            latest.put("description", stringValue(row.get("description")));
            latest.put("installedOn", row.get("installed_on"));
            return latest;
        } catch (Exception ignored) {
            return Collections.emptyMap();
        }
    }

    /**
     * 构建统一校验项结构，前端可直接按状态渲染颜色与下一步动作。
     */
    private Map<String, Object> buildCheckItem(String category, String name, String status, String detail, String nextAction) {
        LinkedHashMap<String, Object> item = new LinkedHashMap<>();
        item.put("category", category);
        item.put("name", name);
        item.put("status", status);
        item.put("detail", detail);
        item.put("nextAction", nextAction);
        return item;
    }

    private CostBillPeriod buildPeriodQuery(Long sceneId) {
        CostBillPeriod query = new CostBillPeriod();
        query.setSceneId(sceneId);
        return query;
    }

    private CostRecalcOrder buildRecalcQuery(Long sceneId, String billMonth) {
        CostRecalcOrder query = new CostRecalcOrder();
        query.setSceneId(sceneId);
        query.setBillMonth(billMonth);
        return query;
    }

    private void enrichPeriods(List<CostBillPeriod> periods) {
        Set<Long> sceneIds = periods.stream().map(CostBillPeriod::getSceneId).filter(Objects::nonNull).collect(Collectors.toSet());
        Set<Long> versionIds = periods.stream().map(CostBillPeriod::getActiveVersionId).filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, CostScene> sceneMap = sceneIds.isEmpty() ? Collections.emptyMap()
                : sceneMapper.selectBatchIds(sceneIds).stream().collect(Collectors.toMap(CostScene::getSceneId, item -> item));
        Map<Long, CostPublishVersion> versionMap = versionIds.isEmpty() ? Collections.emptyMap()
                : publishVersionMapper.selectBatchIds(versionIds).stream().collect(Collectors.toMap(CostPublishVersion::getVersionId, item -> item));
        periods.forEach(item -> {
            CostScene scene = sceneMap.get(item.getSceneId());
            if (scene != null) {
                item.setSceneCode(scene.getSceneCode());
                item.setSceneName(scene.getSceneName());
            }
            CostPublishVersion version = versionMap.get(item.getActiveVersionId());
            if (version != null) {
                item.setVersionNo(version.getVersionNo());
            }
        });
    }

    private void enrichRecalcOrders(List<CostRecalcOrder> orders) {
        Set<Long> sceneIds = orders.stream().map(CostRecalcOrder::getSceneId).filter(Objects::nonNull).collect(Collectors.toSet());
        Set<Long> versionIds = orders.stream().map(CostRecalcOrder::getVersionId).filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, CostScene> sceneMap = sceneIds.isEmpty() ? Collections.emptyMap()
                : sceneMapper.selectBatchIds(sceneIds).stream().collect(Collectors.toMap(CostScene::getSceneId, item -> item));
        Map<Long, CostPublishVersion> versionMap = versionIds.isEmpty() ? Collections.emptyMap()
                : publishVersionMapper.selectBatchIds(versionIds).stream().collect(Collectors.toMap(CostPublishVersion::getVersionId, item -> item));
        orders.forEach(item -> {
            CostScene scene = sceneMap.get(item.getSceneId());
            if (scene != null) {
                item.setSceneCode(scene.getSceneCode());
                item.setSceneName(scene.getSceneName());
            }
            CostPublishVersion version = versionMap.get(item.getVersionId());
            if (version != null) {
                item.setVersionNo(version.getVersionNo());
            }
        });
    }

    private Map<String, Object> buildResultStats(Long sceneId, String billMonth, Long taskId) {
        List<CostResultLedger> ledgers = resultLedgerMapper.selectList(Wrappers.<CostResultLedger>lambdaQuery()
                .eq(sceneId != null, CostResultLedger::getSceneId, sceneId)
                .eq(StringUtils.isNotEmpty(billMonth), CostResultLedger::getBillMonth, billMonth)
                .eq(taskId != null, CostResultLedger::getTaskId, taskId));
        BigDecimal amountTotal = ledgers.stream().map(CostResultLedger::getAmountValue).filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add).setScale(2, RoundingMode.HALF_UP);
        LinkedHashMap<String, Object> stats = new LinkedHashMap<>();
        stats.put("resultCount", ledgers.size());
        stats.put("amountTotal", amountTotal);
        stats.put("feeCount", ledgers.stream().map(CostResultLedger::getFeeCode).filter(StringUtils::isNotEmpty).distinct().count());
        return stats;
    }

    private Map<String, Object> parseJson(String text) {
        if (StringUtils.isEmpty(text)) {
            return Collections.emptyMap();
        }
        try {
            return objectMapper.readValue(text, new TypeReference<Map<String, Object>>() {
            });
        } catch (Exception ignored) {
            return Collections.emptyMap();
        }
    }

    private CostScene requireScene(Long sceneId) {
        CostScene scene = sceneMapper.selectById(sceneId);
        if (scene == null) {
            throw new ServiceException("所属场景不存在，请刷新后重试");
        }
        return scene;
    }

    private CostPublishVersion requireVersion(Long versionId) {
        CostPublishVersion version = publishVersionMapper.selectById(versionId);
        if (version == null) {
            throw new ServiceException("目标版本不存在，请刷新后重试");
        }
        return version;
    }

    private CostCalcTask requireTask(Long taskId) {
        CostCalcTask task = calcTaskMapper.selectById(taskId);
        if (task == null) {
            throw new ServiceException("基准任务不存在，请刷新后重试");
        }
        return task;
    }

    private CostBillPeriod requirePeriod(Long periodId) {
        CostBillPeriod period = billPeriodMapper.selectById(periodId);
        if (period == null) {
            throw new ServiceException("账期记录不存在，请刷新后重试");
        }
        return period;
    }

    private CostBillPeriod requirePeriod(Long sceneId, String billMonth) {
        CostBillPeriod period = billPeriodMapper.selectOne(Wrappers.<CostBillPeriod>lambdaQuery()
                .eq(CostBillPeriod::getSceneId, sceneId)
                .eq(CostBillPeriod::getBillMonth, billMonth)
                .last("limit 1"));
        if (period == null) {
            throw new ServiceException("当前场景账期不存在，请先创建账期");
        }
        return period;
    }

    private CostRecalcOrder requireRecalc(Long recalcId) {
        CostRecalcOrder order = recalcOrderMapper.selectById(recalcId);
        if (order == null) {
            throw new ServiceException("重算申请不存在，请刷新后重试");
        }
        return order;
    }

    private void ensureBillMonth(String billMonth) {
        if (StringUtils.isEmpty(billMonth) || !billMonth.matches("\\d{4}-\\d{2}")) {
            throw new ServiceException("账期格式必须为 yyyy-MM");
        }
    }

    private Long longValue(Object value) {
        if (value == null) {
            return null;
        }
        return Long.parseLong(String.valueOf(value));
    }

    private String stringValue(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private String firstNonBlank(String first, String second) {
        return StringUtils.isNotEmpty(first) ? first : second;
    }

    private String limitLength(String text, int maxLength) {
        if (text == null) {
            return "";
        }
        return text.length() <= maxLength ? text : text.substring(0, maxLength);
    }
}

package com.ruoyi.web.controller.cost;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruoyi.system.domain.cost.CostFeeItem;
import com.ruoyi.system.domain.cost.CostPublishVersion;
import com.ruoyi.system.domain.cost.CostScene;
import com.ruoyi.system.domain.cost.bo.CostCalcInputBatchCreateBo;
import com.ruoyi.system.domain.cost.bo.CostCalcTaskSubmitBo;
import com.ruoyi.system.domain.cost.bo.CostFeeCalculateBo;
import com.ruoyi.system.mapper.cost.CostFeeMapper;
import com.ruoyi.system.mapper.cost.CostPublishVersionMapper;
import com.ruoyi.system.mapper.cost.CostSceneMapper;
import com.ruoyi.system.service.cost.ICostRunService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.test.annotation.DirtiesContext;

import java.io.StringWriter;
import java.time.YearMonth;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 百万级核算性能基线手工用例。
 *
 * <p>默认不参与日常回归，只有显式传入 {@code -Dcost.perf.enabled=true}
 * 才会执行。执行时会真正写入输入批次、正式任务和结果台账，适合在独立压测环境使用。</p>
 */
@SpringBootTest(properties = {
        "logging.level.root=WARN",
        "logging.level.com.ruoyi=WARN",
        "logging.level.org.springframework=WARN",
        "logging.level.com.alibaba.druid=WARN",
        "logging.level.com.alibaba.druid.filter.stat.StatFilter=OFF",
        "logging.level.com.baomidou.mybatisplus=WARN",
        "spring.main.banner-mode=off",
        "spring.main.log-startup-info=false",
        "spring.devtools.restart.enabled=false",
        "springdoc.api-docs.enabled=false",
        "spring.datasource.druid.filter.stat.enabled=false",
        "spring.datasource.druid.filter.stat.log-slow-sql=false"
})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class CostRunPerformanceManualIT {
    private static final String DEFAULT_SCENE_CODE = "SHOUGANG-ORE-HR-001";

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ICostRunService runService;

    @Autowired
    private CostSceneMapper sceneMapper;

    @Autowired
    private CostPublishVersionMapper publishVersionMapper;

    @Autowired
    private CostFeeMapper feeMapper;

    @Autowired
    private ThreadPoolTaskExecutor threadPoolTaskExecutor;

    @Test
    @EnabledIfSystemProperty(named = "cost.perf.enabled", matches = "true")
    void shouldProduceFormalBatchPerformanceBaseline() throws Exception {
        int recordCount = Integer.getInteger("cost.perf.recordCount", 10000);
        String billMonth = System.getProperty("cost.perf.billMonth", YearMonth.now().plusMonths(6).toString());
        String scenarioLabel = System.getProperty("cost.perf.scenarioLabel", "single-" + recordCount);
        Long sceneId = requireSceneId();
        Long versionId = requireVersionId(sceneId);
        String sceneCode = resolveSceneCode();

        long buildJsonStart = System.currentTimeMillis();
        String inputJson = buildInputJson(recordCount);
        long buildJsonMs = System.currentTimeMillis() - buildJsonStart;

        CostCalcInputBatchCreateBo batchBo = new CostCalcInputBatchCreateBo();
        batchBo.setSceneId(sceneId);
        batchBo.setVersionId(versionId);
        batchBo.setBillMonth(billMonth);
        batchBo.setInputJson(inputJson);
        batchBo.setRemark("performance-baseline");

        long batchStart = System.currentTimeMillis();
        Map<String, Object> batchDetail = runService.createInputBatch(batchBo);
        long createBatchMs = System.currentTimeMillis() - batchStart;
        Map<String, Object> batch = asMap(batchDetail.get("batch"));
        String batchNo = String.valueOf(batch.get("batchNo"));
        Long batchId = toLong(batch.get("batchId"));

        CostCalcTaskSubmitBo submitBo = new CostCalcTaskSubmitBo();
        submitBo.setSceneId(sceneId);
        submitBo.setVersionId(versionId);
        submitBo.setTaskType("FORMAL_BATCH");
        submitBo.setBillMonth(billMonth);
        submitBo.setInputSourceType("INPUT_BATCH");
        submitBo.setSourceBatchNo(batchNo);
        String requestNoPrefix = System.getProperty("cost.perf.requestNoPrefix", "PERF");
        String requestNo = System.getProperty("cost.perf.requestNo",
                requestNoPrefix + "-" + recordCount + "-" + System.currentTimeMillis());
        submitBo.setRequestNo(requestNo);
        submitBo.setRemark("performance-baseline");

        long submitStart = System.currentTimeMillis();
        Map<String, Object> submitResult = runService.submitTask(submitBo);
        long submitTaskMs = System.currentTimeMillis() - submitStart;

        Map<String, Object> task = asMap(submitResult.get("task"));
        Long taskId = toLong(task.get("taskId"));
        Map<String, Object> finalDetail = waitTaskFinished(taskId, recordCount);
        Map<String, Object> finalTask = asMap(finalDetail.get("task"));

        long totalWallClockMs = toLong(finalTask.get("durationMs"));
        long successCount = toLong(finalTask.get("successCount"));
        long failCount = toLong(finalTask.get("failCount"));
        String taskStatus = String.valueOf(finalTask.get("taskStatus"));

        assertThat(taskStatus).isIn("SUCCESS", "PART_SUCCESS");
        assertThat(successCount + failCount).isEqualTo(recordCount);

        LinkedHashMap<String, Object> summary = new LinkedHashMap<>();
        summary.put("scenarioLabel", scenarioLabel);
        summary.put("sceneCode", sceneCode);
        summary.put("sceneId", sceneId);
        summary.put("versionId", versionId);
        summary.put("recordCount", recordCount);
        summary.put("billMonth", billMonth);
        summary.put("batchId", batchId);
        summary.put("batchNo", batchNo);
        summary.put("taskId", taskId);
        summary.put("requestNo", requestNo);
        summary.put("taskStatus", taskStatus);
        summary.put("successCount", successCount);
        summary.put("failCount", failCount);
        summary.put("buildJsonMs", buildJsonMs);
        summary.put("createBatchMs", createBatchMs);
        summary.put("submitTaskMs", submitTaskMs);
        summary.put("taskDurationMs", totalWallClockMs);
        summary.put("taskDurationMinutes", round(totalWallClockMs / 60000D));
        summary.put("recordsPerSecond", round(recordCount * 1000D / Math.max(totalWallClockMs, 1L)));
        summary.put("partitionCount", asMap(finalDetail.get("summary")).get("partitionCount"));
        summary.put("failedPartitionCount", asMap(finalDetail.get("summary")).get("failedPartitionCount"));

        System.out.println("COST_PERF_BASELINE=" + objectMapper.writeValueAsString(summary));
    }

    @Test
    @EnabledIfSystemProperty(named = "cost.perf.enabled", matches = "true")
    void shouldProduceConcurrentFormalBatchPerformanceBaseline() throws Exception {
        int concurrentTasks = Integer.getInteger("cost.perf.concurrentTasks", 1);
        if (concurrentTasks <= 1) {
            return;
        }

        int recordCount = Integer.getInteger("cost.perf.recordCount", 10000);
        String baseBillMonth = System.getProperty("cost.perf.billMonth", YearMonth.now().plusMonths(6).toString());
        boolean sameBillMonth = Boolean.getBoolean("cost.perf.sameBillMonth");
        String requestNoPrefix = System.getProperty("cost.perf.requestNoPrefix", "PERF-CONCURRENT");
        String scenarioLabel = System.getProperty("cost.perf.scenarioLabel",
                (sameBillMonth ? "same-bill-month-" : "multi-bill-month-") + concurrentTasks + "x" + recordCount);
        Long sceneId = requireSceneId();
        Long versionId = requireVersionId(sceneId);
        String sceneCode = resolveSceneCode();

        long buildJsonStart = System.currentTimeMillis();
        String inputJson = buildInputJson(recordCount);
        long buildJsonMs = System.currentTimeMillis() - buildJsonStart;

        List<PerfBatchSeed> batchSeeds = new ArrayList<>();
        long createBatchStart = System.currentTimeMillis();
        for (int i = 0; i < concurrentTasks; i++) {
            String billMonth = sameBillMonth
                    ? baseBillMonth
                    : YearMonth.parse(baseBillMonth).plusMonths(i).toString();
            CostCalcInputBatchCreateBo batchBo = new CostCalcInputBatchCreateBo();
            batchBo.setSceneId(sceneId);
            batchBo.setVersionId(versionId);
            batchBo.setBillMonth(billMonth);
            batchBo.setInputJson(inputJson);
            batchBo.setRemark("performance-concurrent-baseline-" + (i + 1));

            Map<String, Object> batchDetail = runService.createInputBatch(batchBo);
            Map<String, Object> batch = asMap(batchDetail.get("batch"));
            batchSeeds.add(new PerfBatchSeed(
                    toLong(batch.get("batchId")),
                    String.valueOf(batch.get("batchNo")),
                    billMonth,
                    requestNoPrefix + "-" + concurrentTasks + "-" + recordCount + "-" + (i + 1) + "-" + System.currentTimeMillis()));
        }
        long createBatchMs = System.currentTimeMillis() - createBatchStart;

        ExecutorService executorService = Executors.newFixedThreadPool(concurrentTasks);
        List<Future<PerfTaskSubmission>> futures = new ArrayList<>();
        long submitAndRunStart = System.currentTimeMillis();
        try {
            for (PerfBatchSeed batchSeed : batchSeeds) {
                futures.add(executorService.submit(new Callable<>() {
                    @Override
                    public PerfTaskSubmission call() {
                        long submitStart = System.currentTimeMillis();
                        CostCalcTaskSubmitBo submitBo = new CostCalcTaskSubmitBo();
                        submitBo.setSceneId(sceneId);
                        submitBo.setVersionId(versionId);
                        submitBo.setTaskType("FORMAL_BATCH");
                        submitBo.setBillMonth(batchSeed.billMonth);
                        submitBo.setInputSourceType("INPUT_BATCH");
                        submitBo.setSourceBatchNo(batchSeed.batchNo);
                        submitBo.setRequestNo(batchSeed.requestNo);
                        submitBo.setRemark("performance-concurrent-baseline");
                        Map<String, Object> submitResult = runService.submitTask(submitBo);
                        long submitTaskMs = System.currentTimeMillis() - submitStart;
                        Map<String, Object> task = asMap(submitResult.get("task"));
                        return new PerfTaskSubmission(batchSeed, toLong(task.get("taskId")), submitTaskMs);
                    }
                }));
            }

            List<PerfTaskSummary> taskSummaries = new ArrayList<>();
            long submitTaskMsTotal = 0L;
            long successCount = 0L;
            long failCount = 0L;
            int failedTaskCount = 0;
            long maxTaskDurationMs = 0L;
            long minTaskDurationMs = Long.MAX_VALUE;
            for (Future<PerfTaskSubmission> future : futures) {
                PerfTaskSubmission submission = future.get();
                submitTaskMsTotal += submission.submitTaskMs;
                Map<String, Object> finalDetail = waitTaskFinished(submission.taskId, recordCount);
                Map<String, Object> finalTask = asMap(finalDetail.get("task"));
                String taskStatus = String.valueOf(finalTask.get("taskStatus"));
                long taskSuccessCount = toLong(finalTask.get("successCount"));
                long taskFailCount = toLong(finalTask.get("failCount"));
                long taskDurationMs = toLong(finalTask.get("durationMs"));
                assertThat(taskStatus).isIn("SUCCESS", "PART_SUCCESS");
                assertThat(taskSuccessCount + taskFailCount).isEqualTo(recordCount);
                successCount += taskSuccessCount;
                failCount += taskFailCount;
                if (!"SUCCESS".equals(taskStatus)) {
                    failedTaskCount++;
                }
                maxTaskDurationMs = Math.max(maxTaskDurationMs, taskDurationMs);
                minTaskDurationMs = Math.min(minTaskDurationMs, taskDurationMs);
                taskSummaries.add(new PerfTaskSummary(submission, taskStatus, taskSuccessCount, taskFailCount, taskDurationMs));
            }

            long totalWallClockMs = System.currentTimeMillis() - submitAndRunStart;
            int totalRecordCount = concurrentTasks * recordCount;

            LinkedHashMap<String, Object> summary = new LinkedHashMap<>();
            summary.put("scenarioLabel", scenarioLabel);
            summary.put("sceneCode", sceneCode);
            summary.put("sceneId", sceneId);
            summary.put("versionId", versionId);
            summary.put("concurrentTasks", concurrentTasks);
            summary.put("sameBillMonth", sameBillMonth);
            summary.put("baseBillMonth", baseBillMonth);
            summary.put("recordCountPerTask", recordCount);
            summary.put("totalRecordCount", totalRecordCount);
            summary.put("buildJsonMs", buildJsonMs);
            summary.put("createBatchMs", createBatchMs);
            summary.put("submitTaskMsTotal", submitTaskMsTotal);
            summary.put("submitTaskMsAvg", round(submitTaskMsTotal * 1D / concurrentTasks));
            summary.put("wallClockMs", totalWallClockMs);
            summary.put("wallClockMinutes", round(totalWallClockMs / 60000D));
            summary.put("aggregateRecordsPerSecond", round(totalRecordCount * 1000D / Math.max(totalWallClockMs, 1L)));
            summary.put("successCount", successCount);
            summary.put("failCount", failCount);
            summary.put("failedTaskCount", failedTaskCount);
            summary.put("maxTaskDurationMs", maxTaskDurationMs);
            summary.put("minTaskDurationMs", minTaskDurationMs == Long.MAX_VALUE ? 0L : minTaskDurationMs);
            summary.put("tasks", taskSummaries);

            System.out.println("COST_PERF_CONCURRENT_BASELINE=" + objectMapper.writeValueAsString(summary));
        } finally {
            executorService.shutdownNow();
        }
    }

    @Test
    @EnabledIfSystemProperty(named = "cost.perf.enabled", matches = "true")
    void shouldProduceFeeScopeCalculationPerformanceBaseline() throws Exception {
        int recordCount = Integer.getInteger("cost.perf.recordCount", 1000);
        String billMonth = System.getProperty("cost.perf.billMonth", YearMonth.now().plusMonths(6).toString());
        String sceneCode = resolveSceneCode();
        String scope = System.getProperty("cost.perf.feeScope", "ALL").trim().toUpperCase(Locale.ROOT);
        boolean includeExplain = Boolean.getBoolean("cost.perf.includeExplain");
        Long sceneId = requireSceneId();
        Long versionId = requireVersionId(sceneId);

        CostFeeCalculateBo bo = new CostFeeCalculateBo();
        bo.setSceneId(sceneId);
        bo.setVersionId(versionId);
        bo.setBillMonth(billMonth);
        bo.setIncludeExplain(includeExplain);

        if ("SINGLE".equals(scope)) {
            bo.setFeeCode(System.getProperty("cost.perf.feeCode", "SG_FEMALE_SHIFT_LABOR"));
        } else if ("MULTI".equals(scope)) {
            bo.setFeeIds(resolvePerfFeeIds(sceneId, resolvePerfFeeCodes()));
        }

        long buildJsonStart = System.currentTimeMillis();
        String inputJson = buildInputJson(recordCount);
        long buildJsonMs = System.currentTimeMillis() - buildJsonStart;
        bo.setInputJson(inputJson);

        long calculateStart = System.currentTimeMillis();
        Map<String, Object> result = runService.calculateFee(bo);
        long durationMs = System.currentTimeMillis() - calculateStart;

        long targetFeeCount = toLong(result.get("targetFeeCount"));
        long executionFeeCount = toLong(result.get("executionFeeCount"));
        long inputCount = toLong(result.get("inputCount"));
        long outputRecordCount = toLong(result.get("recordCount"));
        long successCount = toLong(result.get("successCount"));
        long failedCount = toLong(result.get("failedCount"));
        long noMatchCount = toLong(result.get("noMatchCount"));
        boolean allFeeScope = Boolean.TRUE.equals(result.get("allFeeScope"));

        assertThat(inputCount).isEqualTo(recordCount);
        assertThat(targetFeeCount).isGreaterThan(0L);
        assertThat(executionFeeCount).isGreaterThanOrEqualTo(targetFeeCount);
        assertThat(outputRecordCount).isEqualTo(recordCount * targetFeeCount);
        assertThat(successCount + failedCount + noMatchCount).isEqualTo(outputRecordCount);
        if ("ALL".equals(scope)) {
            assertThat(allFeeScope).isTrue();
        } else {
            assertThat(allFeeScope).isFalse();
        }

        LinkedHashMap<String, Object> summary = new LinkedHashMap<>();
        summary.put("scenarioLabel", System.getProperty("cost.perf.scenarioLabel",
                "fee-scope-" + scope.toLowerCase(Locale.ROOT) + "-" + recordCount));
        summary.put("sceneCode", sceneCode);
        summary.put("sceneId", sceneId);
        summary.put("versionId", versionId);
        summary.put("billMonth", billMonth);
        summary.put("scope", scope);
        summary.put("includeExplain", includeExplain);
        summary.put("recordCount", recordCount);
        summary.put("buildJsonMs", buildJsonMs);
        summary.put("durationMs", durationMs);
        summary.put("durationMinutes", round(durationMs / 60000D));
        summary.put("inputCount", inputCount);
        summary.put("targetFeeCount", targetFeeCount);
        summary.put("executionFeeCount", executionFeeCount);
        summary.put("outputRecordCount", outputRecordCount);
        summary.put("successCount", successCount);
        summary.put("failedCount", failedCount);
        summary.put("noMatchCount", noMatchCount);
        summary.put("allFeeScope", allFeeScope);
        summary.put("recordsPerSecond", round(inputCount * 1000D / Math.max(durationMs, 1L)));
        summary.put("pricingRowsPerSecond", round(outputRecordCount * 1000D / Math.max(durationMs, 1L)));
        summary.put("targetFeeCodes", result.get("targetFeeCodes"));
        summary.put("executionFeeCodes", result.get("executionFeeCodes"));
        summary.put("dependentFeeCodes", result.get("dependentFeeCodes"));

        System.out.println("COST_PERF_FEE_SCOPE_BASELINE=" + objectMapper.writeValueAsString(summary));
    }

    private Long requireSceneId() {
        Long overrideSceneId = Long.getLong("cost.perf.sceneId");
        if (overrideSceneId != null) {
            return overrideSceneId;
        }
        String sceneCode = resolveSceneCode();
        CostScene scene = sceneMapper.selectOne(Wrappers.<CostScene>lambdaQuery()
                .eq(CostScene::getSceneCode, sceneCode)
                .last("limit 1"));
        assertThat(scene).as("missing perf baseline scene %s", sceneCode).isNotNull();
        return scene.getSceneId();
    }

    private Long requireVersionId(Long sceneId) {
        Long overrideVersionId = Long.getLong("cost.perf.versionId");
        if (overrideVersionId != null) {
            return overrideVersionId;
        }
        CostPublishVersion activeVersion = publishVersionMapper.selectActiveVersionByScene(sceneId);
        if (activeVersion != null) {
            return activeVersion.getVersionId();
        }
        CostPublishVersion latestVersion = publishVersionMapper.selectLatestVersionByScene(sceneId);
        assertThat(latestVersion).as("missing published version for scene %s", sceneId).isNotNull();
        return latestVersion.getVersionId();
    }

    private String resolveSceneCode() {
        return System.getProperty("cost.perf.sceneCode", DEFAULT_SCENE_CODE);
    }

    private List<String> resolvePerfFeeCodes() {
        String configured = System.getProperty("cost.perf.feeCodes", "SG_FEMALE_SHIFT_LABOR,SG_MANAGEMENT_FEE");
        LinkedHashSet<String> codes = new LinkedHashSet<>();
        for (String item : configured.split(",")) {
            String value = item == null ? "" : item.trim();
            if (!value.isEmpty()) {
                codes.add(value);
            }
        }
        assertThat(codes).isNotEmpty();
        return new ArrayList<>(codes);
    }

    private List<Long> resolvePerfFeeIds(Long sceneId, List<String> feeCodes) {
        Map<String, Long> feeIdMap = feeMapper.selectList(Wrappers.<CostFeeItem>lambdaQuery()
                        .eq(CostFeeItem::getSceneId, sceneId)
                        .in(CostFeeItem::getFeeCode, feeCodes))
                .stream()
                .collect(LinkedHashMap::new,
                        (acc, item) -> acc.put(item.getFeeCode(), item.getFeeId()),
                        LinkedHashMap::putAll);
        List<Long> result = new ArrayList<>();
        for (String feeCode : feeCodes) {
            Long feeId = feeIdMap.get(feeCode);
            assertThat(feeId).as("missing perf fee %s for scene %s", feeCode, sceneId).isNotNull();
            result.add(feeId);
        }
        return result;
    }

    private Map<String, Object> waitTaskFinished(Long taskId, int recordCount) throws Exception {
        long timeoutMs = Long.getLong("cost.perf.timeoutMs", resolveDefaultTimeoutMs(recordCount));
        long deadline = System.currentTimeMillis() + timeoutMs;
        Map<String, Object> detail = runService.selectTaskDetail(taskId, 1, 1);
        while (System.currentTimeMillis() < deadline) {
            detail = runService.selectTaskDetail(taskId, 1, 1);
            Map<String, Object> task = asMap(detail.get("task"));
            String taskStatus = String.valueOf(task.get("taskStatus"));
            if ("SUCCESS".equals(taskStatus) || "PART_SUCCESS".equals(taskStatus) || "FAILED".equals(taskStatus)) {
                waitExecutorIdle(deadline);
                return detail;
            }
            Thread.sleep(1000L);
        }
        return detail;
    }

    private long resolveDefaultTimeoutMs(int recordCount) {
        if (recordCount >= 1_000_000) {
            return 2L * 60L * 60L * 1000L;
        }
        if (recordCount >= 500_000) {
            return 90L * 60L * 1000L;
        }
        return 30L * 60L * 1000L;
    }

    private void waitExecutorIdle(long deadline) throws Exception {
        while (System.currentTimeMillis() < deadline) {
            if (threadPoolTaskExecutor.getActiveCount() <= 0
                    && threadPoolTaskExecutor.getThreadPoolExecutor() != null
                    && threadPoolTaskExecutor.getThreadPoolExecutor().getQueue().isEmpty()) {
                return;
            }
            Thread.sleep(200L);
        }
    }

    private String buildInputJson(int recordCount) throws Exception {
        StringWriter writer = new StringWriter(Math.max(recordCount * 180, 1024));
        JsonFactory factory = objectMapper.getFactory();
        try (JsonGenerator generator = factory.createGenerator(writer)) {
            generator.writeStartArray();
            for (int i = 0; i < recordCount; i++) {
                generator.writeStartObject();
                generator.writeStringField("bizNo", String.format(Locale.ROOT, "PERF-BIZ-%07d", i + 1));
                generator.writeStringField("objectCode", String.format(Locale.ROOT, "PERF-TEAM-%07d", i + 1));
                generator.writeStringField("objectName", String.format(Locale.ROOT, "性能基线队组%07d", i + 1));
                generator.writeNumberField("ALLOCATED_THROUGHPUT_TON", 1000);
                generator.writeNumberField("FEMALE_TEAM_HEADCOUNT", 2);
                generator.writeStringField("COVER_ACTION", "COVER");
                generator.writeNumberField("SEASONAL_SUBSIDY_EQUIV", 1);
                generator.writeNumberField("FEMALE_ACTUAL_ATTENDANCE", 6);
                generator.writeNumberField("FEMALE_REQUIRED_ATTENDANCE", 6);
                generator.writeNumberField("HOLD_COUNT", 1);
                generator.writeStringField("COVER_CARGO_TYPE", "COAL");
                generator.writeNumberField("UNIT_BEARING_AMOUNT", 100);
                generator.writeNumberField("SPECIAL_TEAM_HEADCOUNT", 1);
                generator.writeNumberField("SPECIAL_ACTUAL_ATTENDANCE", 6);
                generator.writeNumberField("SPECIAL_REQUIRED_ATTENDANCE", 6);
                generator.writeNumberField("ODD_JOB_HOURS", 2);
                generator.writeNumberField("COVER_WORKLOAD_TON", 1000);
                generator.writeNumberField("INSURANCE_TAXABLE_AMOUNT", 300);
                generator.writeNumberField("DUTY_TEAM_REQUIRED_ATTENDANCE", 6);
                generator.writeNumberField("ALL_TEAMS_REQUIRED_ATTENDANCE", 12);
                generator.writeNumberField("DUTY_TEAM_ACTUAL_ATTENDANCE", 6);
                generator.writeNumberField("OVERTIME_DAYS", 1);
                generator.writeStringField("MOORING_ACTION", "MOORING");
                generator.writeNumberField("EMPLOYER_LIABILITY_AMOUNT", 50);
                generator.writeNumberField("MOORING_HEADCOUNT", 2);
                generator.writeEndObject();
            }
            generator.writeEndArray();
        }
        return writer.toString();
    }

    private Map<String, Object> asMap(Object value) {
        return value == null
                ? Map.of()
                : objectMapper.convertValue(value, new TypeReference<LinkedHashMap<String, Object>>() {
        });
    }

    private long toLong(Object value) {
        if (value == null) {
            return 0L;
        }
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        return Long.parseLong(String.valueOf(value));
    }

    private double round(double value) {
        return Math.round(value * 100D) / 100D;
    }

    private record PerfBatchSeed(Long batchId, String batchNo, String billMonth, String requestNo) {
    }

    private record PerfTaskSubmission(PerfBatchSeed batchSeed, Long taskId, long submitTaskMs) {
    }

    private record PerfTaskSummary(PerfTaskSubmission submission, String taskStatus, long successCount, long failCount,
                                   long taskDurationMs) {
    }
}

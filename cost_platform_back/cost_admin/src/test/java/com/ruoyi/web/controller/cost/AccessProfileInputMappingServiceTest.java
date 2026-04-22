package com.ruoyi.web.controller.cost;

import com.ruoyi.system.service.cost.remote.AccessProfileInputMappingService;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class AccessProfileInputMappingServiceTest {
    @Test
    @SuppressWarnings("unchecked")
    void shouldWriteNestedTemplatePathWhenMappingByVariableCode() {
        AccessProfileInputMappingService service = new AccessProfileInputMappingService();
        Map<String, Object> template = Map.of(
                "taskType", "FORMAL_BATCH",
                "fields", List.of(Map.of(
                        "path", "job.weightTon",
                        "variableCode", "WEIGHT_TON",
                        "variableName", "重量吨",
                        "sourceType", "INPUT",
                        "includedInTemplate", true)));
        AccessProfileInputMappingService.InputBuildContext context = service.buildContext(
                template,
                "{\"WEIGHT_TON\":\"weight\"}");

        Map<String, Object> result = service.buildMappedInputResult(context, List.of(Map.of("weight", 134.54)), 1);
        Map<String, Object> mappedRecord = (Map<String, Object>) ((List<?>) result.get("mappedRecords")).get(0);
        Map<String, Object> job = (Map<String, Object>) mappedRecord.get("job");

        assertThat(result.get("missingPaths")).isEqualTo(List.of());
        assertThat(job.get("weightTon")).isEqualTo(134.54);
        assertThat(mappedRecord).doesNotContainKey("WEIGHT_TON");
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldWriteFlatVariableCodeWhenTemplatePathMissing() {
        AccessProfileInputMappingService service = new AccessProfileInputMappingService();
        Map<String, Object> template = Map.of(
                "taskType", "FORMAL_BATCH",
                "fields", List.of(Map.of(
                        "variableCode", "ODD_JOB_HOURS",
                        "variableName", "零工作业时长",
                        "sourceType", "INPUT",
                        "includedInTemplate", true)));
        AccessProfileInputMappingService.InputBuildContext context = service.buildContext(
                template,
                "{\"ODD_JOB_HOURS\":\"hours\"}");

        Map<String, Object> result = service.buildMappedInputResult(context, List.of(Map.of("hours", 8)), 1);
        Map<String, Object> mappedRecord = (Map<String, Object>) ((List<?>) result.get("mappedRecords")).get(0);

        assertThat(result.get("missingPaths")).isEqualTo(List.of());
        assertThat(mappedRecord.get("ODD_JOB_HOURS")).isEqualTo(8);
    }
}

package com.ruoyi.system.service.cost.publish;

import org.junit.jupiter.api.Test;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;

class PublishSnapshotViewServiceTest {

    private final PublishSnapshotViewService service = new PublishSnapshotViewService();

    @Test
    void shouldIgnoreLegacySchemaOnlyFieldGrowthWhenBuildingFeeDiffSummary() {
        PublishSnapshotBundle publishedBundle = new PublishSnapshotBundle();
        PublishSnapshotBundle draftBundle = new PublishSnapshotBundle();

        publishedBundle.feesByCode.put("SG_PORT_LOAD_FEE", feeSnapshot("SG_PORT_LOAD_FEE", "港口装卸费", "吨", false));
        draftBundle.feesByCode.put("SG_PORT_LOAD_FEE", feeSnapshot("SG_PORT_LOAD_FEE", "港口装卸费", "吨", true));

        publishedBundle.rulesByCode.put("PORT_LOAD_RULE", ruleSnapshot("PORT_LOAD_RULE", "SG_PORT_LOAD_FEE", false));
        draftBundle.rulesByCode.put("PORT_LOAD_RULE", ruleSnapshot("PORT_LOAD_RULE", "SG_PORT_LOAD_FEE", true));

        publishedBundle.feeRuleCodes.put("SG_PORT_LOAD_FEE", new TreeSet<>(Collections.singleton("PORT_LOAD_RULE")));
        draftBundle.feeRuleCodes.put("SG_PORT_LOAD_FEE", new TreeSet<>(Collections.singleton("PORT_LOAD_RULE")));

        publishedBundle.variablesByCode.put("ALLOCATED_THROUGHPUT_TON", variableSnapshot("ALLOCATED_THROUGHPUT_TON", false));
        draftBundle.variablesByCode.put("ALLOCATED_THROUGHPUT_TON", variableSnapshot("ALLOCATED_THROUGHPUT_TON", true));

        publishedBundle.feeReferencedVariables.put("SG_PORT_LOAD_FEE",
                new TreeSet<>(Collections.singleton("ALLOCATED_THROUGHPUT_TON")));
        draftBundle.feeReferencedVariables.put("SG_PORT_LOAD_FEE",
                new TreeSet<>(Collections.singleton("ALLOCATED_THROUGHPUT_TON")));

        assertThat(service.buildFeeDiffSummary(publishedBundle, draftBundle, null)).isEmpty();
    }

    @Test
    void shouldOnlyFlagFeeThatActuallyChangedWhenLegacySchemaNoiseExists() {
        PublishSnapshotBundle publishedBundle = new PublishSnapshotBundle();
        PublishSnapshotBundle draftBundle = new PublishSnapshotBundle();

        publishedBundle.feesByCode.put("SG_PORT_LOAD_FEE", feeSnapshot("SG_PORT_LOAD_FEE", "港口装卸费", "吨", false));
        publishedBundle.feesByCode.put("SG_MANAGEMENT_FEE", feeSnapshot("SG_MANAGEMENT_FEE", "协力单位管理费", "元", false));
        draftBundle.feesByCode.put("SG_PORT_LOAD_FEE", feeSnapshot("SG_PORT_LOAD_FEE", "港口装卸费", "船", true));
        draftBundle.feesByCode.put("SG_MANAGEMENT_FEE", feeSnapshot("SG_MANAGEMENT_FEE", "协力单位管理费", "元", true));

        publishedBundle.rulesByCode.put("PORT_LOAD_RULE", ruleSnapshot("PORT_LOAD_RULE", "SG_PORT_LOAD_FEE", false));
        publishedBundle.rulesByCode.put("MANAGEMENT_RULE", ruleSnapshot("MANAGEMENT_RULE", "SG_MANAGEMENT_FEE", false));
        draftBundle.rulesByCode.put("PORT_LOAD_RULE", ruleSnapshot("PORT_LOAD_RULE", "SG_PORT_LOAD_FEE", true));
        draftBundle.rulesByCode.put("MANAGEMENT_RULE", ruleSnapshot("MANAGEMENT_RULE", "SG_MANAGEMENT_FEE", true));

        publishedBundle.feeRuleCodes.put("SG_PORT_LOAD_FEE", new TreeSet<>(Collections.singleton("PORT_LOAD_RULE")));
        publishedBundle.feeRuleCodes.put("SG_MANAGEMENT_FEE", new TreeSet<>(Collections.singleton("MANAGEMENT_RULE")));
        draftBundle.feeRuleCodes.put("SG_PORT_LOAD_FEE", new TreeSet<>(Collections.singleton("PORT_LOAD_RULE")));
        draftBundle.feeRuleCodes.put("SG_MANAGEMENT_FEE", new TreeSet<>(Collections.singleton("MANAGEMENT_RULE")));

        List<Map<String, Object>> diffs = service.buildFeeDiffSummary(publishedBundle, draftBundle, null);

        assertThat(diffs).hasSize(1);
        assertThat(diffs.get(0).get("feeCode")).isEqualTo("SG_PORT_LOAD_FEE");
        assertThat(diffs.get(0).get("changeType")).isEqualTo("CHANGED");
        assertThat(diffs.get(0).get("ruleChangeCount")).isEqualTo(0);
        assertThat(diffs.get(0).get("variableChangeCount")).isEqualTo(0);
    }

    private Map<String, Object> feeSnapshot(String feeCode, String feeName, String unitCode, boolean expandedSchema) {
        LinkedHashMap<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("feeCode", feeCode);
        snapshot.put("feeName", feeName);
        snapshot.put("feeCategory", "劳务费");
        snapshot.put("unitCode", unitCode);
        snapshot.put("sortNo", 10);
        snapshot.put("status", "0");
        if (expandedSchema) {
            snapshot.put("factorSummary", "按业务口径汇总");
            snapshot.put("scopeDescription", "适用于试算与发布校验");
            snapshot.put("objectDimension", "协力单位");
            snapshot.put("remark", "schema-expanded");
        }
        return snapshot;
    }

    private Map<String, Object> ruleSnapshot(String ruleCode, String feeCode, boolean expandedSchema) {
        LinkedHashMap<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("ruleCode", ruleCode);
        snapshot.put("ruleName", ruleCode + "-NAME");
        snapshot.put("feeCode", feeCode);
        snapshot.put("ruleType", "FIXED_RATE");
        snapshot.put("conditionLogic", "AND");
        snapshot.put("priority", 100);
        snapshot.put("quantityVariableCode", "ALLOCATED_THROUGHPUT_TON");
        snapshot.put("pricingMode", "FIXED");
        if (expandedSchema) {
            snapshot.put("feeName", feeCode + "-FEE");
            snapshot.put("amountFormulaCode", "");
            snapshot.put("amountBusinessFormula", "");
            snapshot.put("noteTemplate", "");
            snapshot.put("remark", "schema-expanded");
        }
        return snapshot;
    }

    private Map<String, Object> variableSnapshot(String variableCode, boolean expandedSchema) {
        LinkedHashMap<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("variableCode", variableCode);
        snapshot.put("variableName", variableCode + "-NAME");
        snapshot.put("variableType", "INPUT");
        snapshot.put("sourceType", "INPUT");
        snapshot.put("dataType", "NUMBER");
        snapshot.put("defaultValue", "0");
        if (expandedSchema) {
            snapshot.put("cachePolicy", "REALTIME");
            snapshot.put("fallbackPolicy", "EMPTY");
            snapshot.put("remark", "schema-expanded");
        }
        return snapshot;
    }
}

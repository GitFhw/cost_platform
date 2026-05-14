package com.ruoyi.system.service.impl.cost;

import com.ruoyi.common.utils.StringUtils;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

class CostResultCompareSource {
    String sourceType;
    String sourceName;
    String sourceNo;
    Long taskId;
    Long simulationId;
    Long sceneId;
    String sceneName;
    Long versionId;
    String versionNo;
    String billMonth;
    long resultCount;
    BigDecimal amountTotal = BigDecimal.ZERO;
    final Map<String, CostResultCompareFeeAggregate> fees = new LinkedHashMap<>();

    void addFee(String feeCode, String feeName, BigDecimal amountValue, long count) {
        String key = StringUtils.isNotEmpty(feeCode) ? feeCode : "UNKNOWN";
        CostResultCompareFeeAggregate aggregate = fees.computeIfAbsent(key, code -> {
            CostResultCompareFeeAggregate item = new CostResultCompareFeeAggregate();
            item.feeCode = code;
            item.feeName = StringUtils.isNotEmpty(feeName) ? feeName : code;
            return item;
        });
        aggregate.feeName = StringUtils.isNotEmpty(aggregate.feeName) ? aggregate.feeName : feeName;
        aggregate.amountTotal = aggregate.amountTotal.add(amountValue == null ? BigDecimal.ZERO : amountValue);
        aggregate.resultCount += count;
    }
}

package com.ruoyi.system.domain.cost.bo;

import lombok.Data;

/**
 * Result ledger comparison query.
 *
 * @author HwFan
 */
@Data
public class CostResultCompareBo {
    private String leftSourceType;
    private Long leftTaskId;
    private Long leftSimulationId;
    private Long leftSceneId;
    private Long leftVersionId;
    private String leftBillMonth;

    private String rightSourceType;
    private Long rightTaskId;
    private Long rightSimulationId;
    private Long rightSceneId;
    private Long rightVersionId;
    private String rightBillMonth;
}

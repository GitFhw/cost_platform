package com.ruoyi.system.domain.vo;

import lombok.Data;

/**
 * 费用治理预检查结果
 *
 * @author HwFan
 */
@Data
public class CostFeeGovernanceCheckVo {
    /**
     * 费用主键
     */
    private Long feeId;

    /**
     * 场景主键
     */
    private Long sceneId;

    /**
     * 场景编码
     */
    private String sceneCode;

    /**
     * 场景名称
     */
    private String sceneName;

    /**
     * 费用编码
     */
    private String feeCode;

    /**
     * 费用名称
     */
    private String feeName;

    /**
     * 状态
     */
    private String status;

    /**
     * 规则引用数量
     */
    private Long ruleCount;

    /**
     * 变量关系数量
     */
    private Long variableRelCount;

    /**
     * 发布版本引用数量
     */
    private Long publishedVersionCount;

    /**
     * 结果台账引用数量
     */
    private Long resultLedgerCount;

    /**
     * 是否允许删除
     */
    private Boolean canDelete;

    /**
     * 是否允许停用
     */
    private Boolean canDisable;

    /**
     * 删除阻断说明
     */
    private String removeBlockingReason;

    /**
     * 停用阻断说明
     */
    private String disableBlockingReason;

    /**
     * 删除治理建议
     */
    private String removeAdvice;

    /**
     * 停用治理建议
     */
    private String disableAdvice;
}

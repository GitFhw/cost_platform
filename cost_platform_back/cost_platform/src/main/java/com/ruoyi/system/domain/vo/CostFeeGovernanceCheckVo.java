package com.ruoyi.system.domain.vo;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

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
     * 业务域
     */
    private String businessDomain;

    /**
     * 场景默认对象维度
     */
    private String sceneDefaultObjectDimension;

    /**
     * 费用分类
     */
    private String feeCategory;

    /**
     * 计价单位
     */
    private String unitCode;

    /**
     * 影响因素摘要
     */
    private String factorSummary;

    /**
     * 适用范围说明
     */
    private String scopeDescription;

    /**
     * 费用对象维度
     */
    private String objectDimension;

    /**
     * 排序号
     */
    private Integer sortNo;

    /**
     * 状态
     */
    private String status;

    /**
     * 备注
     */
    private String remark;

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

    /**
     * 关联影响明细
     */
    private List<CostGovernanceImpactVo> impactItems = new ArrayList<>();

    /**
     * Fee input contracts.
     */
    private List<CostFeeVariableContractVo> variableContracts = new ArrayList<>();

    /**
     * Linked rule summaries.
     */
    private List<CostFeeRuleSummaryVo> ruleSummaries = new ArrayList<>();

    /**
     * Publish version references.
     */
    private List<CostFeePublishRefVo> publishRefs = new ArrayList<>();

    /**
     * Recent result ledger references.
     */
    private List<CostFeeResultRefVo> resultRefs = new ArrayList<>();
}

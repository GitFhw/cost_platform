package com.ruoyi.system.domain.vo;

import lombok.Data;

/**
 * 场景治理预检查结果
 *
 * @author codex
 */
@Data
public class CostSceneGovernanceCheckVo
{
    /** 场景主键 */
    private Long sceneId;

    /** 场景编码 */
    private String sceneCode;

    /** 场景名称 */
    private String sceneName;

    /** 业务域 */
    private String businessDomain;

    /** 场景状态 */
    private String status;

    /** 当前生效版本 */
    private Long activeVersionId;

    /** 费用数量 */
    private Long feeCount;

    /** 变量组数量 */
    private Long variableGroupCount;

    /** 变量数量 */
    private Long variableCount;

    /** 规则数量 */
    private Long ruleCount;

    /** 已发布版本数量 */
    private Long publishedVersionCount;

    /** 配置对象总数 */
    private Long totalConfigCount;

    /** 是否允许删除 */
    private Boolean canDelete;

    /** 是否允许停用 */
    private Boolean canDisable;

    /** 删除阻断说明 */
    private String removeBlockingReason;

    /** 停用阻断说明 */
    private String disableBlockingReason;

    /** 删除治理建议 */
    private String removeAdvice;

    /** 停用治理建议 */
    private String disableAdvice;
}

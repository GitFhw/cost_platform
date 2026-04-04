package com.ruoyi.system.domain.vo;

import lombok.Data;

/**
 * 变量导入校验问题。
 *
 * @author codex
 */
@Data
public class CostVariableImportIssueVo
{
    /** Excel 行号 */
    private Integer rowNum;

    /** 场景编码 */
    private String sceneCode;

    /** 变量编码 */
    private String variableCode;

    /** 变量名称 */
    private String variableName;

    /** 问题描述 */
    private String message;
}

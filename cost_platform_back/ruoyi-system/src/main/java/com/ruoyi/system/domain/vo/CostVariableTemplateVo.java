package com.ruoyi.system.domain.vo;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 共享影响因素模板。
 *
 * @author codex
 */
@Data
public class CostVariableTemplateVo
{
    /** 模板编码 */
    private String templateCode;

    /** 模板名称 */
    private String templateName;

    /** 适用说明 */
    private String description;

    /** 命名空间说明 */
    private String namespaceHint;

    /** 变量数量 */
    private Integer variableCount;

    /** 模板变量清单 */
    private List<Map<String, Object>> items = new ArrayList<>();
}

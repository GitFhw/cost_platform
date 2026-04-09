package com.ruoyi.system.domain.cost.bo;

import lombok.Data;

/**
 * 公式测试请求对象。
 *
 * <p>用于公式实验室测试台执行表达式验证，
 * 支持按公式主键测试，也支持直接传入表达式做草稿预验。</p>
 *
 * @author HwFan
 */
@Data
public class CostFormulaTestBo
{
    /** 公式主键 */
    private Long formulaId;

    /** 所属场景主键 */
    private Long sceneId;

    /** 公式编码 */
    private String formulaCode;

    /** 临时表达式 */
    private String formulaExpr;

    /** 输入 JSON */
    private String inputJson;
}

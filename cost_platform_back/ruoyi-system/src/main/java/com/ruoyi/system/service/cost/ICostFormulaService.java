package com.ruoyi.system.service.cost;

import com.ruoyi.system.domain.cost.CostFormula;
import com.ruoyi.system.domain.cost.bo.CostFormulaTestBo;
import com.ruoyi.system.domain.vo.CostFormulaGovernanceCheckVo;

import java.util.List;
import java.util.Map;

/**
 * 公式实验室服务接口。
 *
 * @author codex
 */
public interface ICostFormulaService
{
    /**
     * 查询公式列表。
     */
    List<CostFormula> selectFormulaList(CostFormula formula);

    /**
     * 查询公式详情。
     */
    CostFormula selectFormulaById(Long formulaId);

    /**
     * 查询公式选择框。
     */
    List<CostFormula> selectFormulaOptions(CostFormula formula);

    /**
     * 查询公式统计。
     */
    Map<String, Object> selectFormulaStats(CostFormula formula);

    /**
     * 查询公式治理检查。
     */
    CostFormulaGovernanceCheckVo selectFormulaGovernanceCheck(Long formulaId);

    /**
     * 校验公式编码唯一性。
     */
    boolean checkFormulaCodeUnique(CostFormula formula);

    /**
     * 新增公式。
     */
    int insertFormula(CostFormula formula);

    /**
     * 修改公式。
     */
    int updateFormula(CostFormula formula);

    /**
     * 删除公式。
     */
    int deleteFormulaByIds(Long[] formulaIds);

    /**
     * 测试公式。
     */
    Map<String, Object> testFormula(CostFormulaTestBo bo, String operator);
}

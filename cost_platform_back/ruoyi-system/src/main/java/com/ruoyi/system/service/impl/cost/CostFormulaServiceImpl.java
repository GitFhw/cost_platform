package com.ruoyi.system.service.impl.cost;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.core.domain.entity.SysDictData;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.system.domain.cost.CostFormula;
import com.ruoyi.system.domain.cost.CostScene;
import com.ruoyi.system.domain.cost.bo.CostFormulaTestBo;
import com.ruoyi.system.domain.vo.CostFormulaGovernanceCheckVo;
import com.ruoyi.system.mapper.SysDictDataMapper;
import com.ruoyi.system.mapper.cost.CostFormulaMapper;
import com.ruoyi.system.mapper.cost.CostSceneMapper;
import com.ruoyi.system.service.cost.ICostExpressionService;
import com.ruoyi.system.service.cost.ICostFormulaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 公式实验室服务实现。
 *
 * <p>线程七在这里建立“公式主数据 -> 测试验证 -> 被变量/规则引用”的治理主线，
 * 先把公式作为独立资产做对，再在后续线程逐步扩大引用范围。</p>
 *
 * @author codex
 */
@Service
public class CostFormulaServiceImpl implements ICostFormulaService
{
    private static final String STATUS_ENABLED = "0";
    private static final String DICT_TYPE_FORMULA_STATUS = "cost_formula_status";
    private static final String DICT_TYPE_RETURN_TYPE = "cost_formula_return_type";

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private CostFormulaMapper formulaMapper;

    @Autowired
    private CostSceneMapper sceneMapper;

    @Autowired
    private SysDictDataMapper dictDataMapper;

    @Autowired
    private ICostExpressionService expressionService;

    @Override
    public List<CostFormula> selectFormulaList(CostFormula formula)
    {
        return formulaMapper.selectFormulaList(formula);
    }

    @Override
    public CostFormula selectFormulaById(Long formulaId)
    {
        return formulaMapper.selectById(formulaId);
    }

    @Override
    public List<CostFormula> selectFormulaOptions(CostFormula formula)
    {
        return formulaMapper.selectFormulaOptions(formula);
    }

    @Override
    public Map<String, Object> selectFormulaStats(CostFormula formula)
    {
        Map<String, Object> stats = formulaMapper.selectFormulaStats(formula);
        LinkedHashMap<String, Object> result = new LinkedHashMap<>();
        result.put("formulaCount", 0);
        result.put("enabledFormulaCount", 0);
        result.put("variableRefCount", 0);
        result.put("ruleRefCount", 0);
        if (stats == null)
        {
            return result;
        }
        stats.forEach((key, value) -> result.put(key, value == null ? 0 : value));
        return result;
    }

    @Override
    public CostFormulaGovernanceCheckVo selectFormulaGovernanceCheck(Long formulaId)
    {
        CostFormulaGovernanceCheckVo check = formulaMapper.selectFormulaGovernanceCheck(formulaId);
        if (check == null)
        {
            return null;
        }
        long variableRefCount = check.getVariableRefCount() == null ? 0 : check.getVariableRefCount();
        long ruleRefCount = check.getRuleRefCount() == null ? 0 : check.getRuleRefCount();
        long publishedVersionCount = check.getPublishedVersionCount() == null ? 0 : check.getPublishedVersionCount();
        check.setVariableRefCount(variableRefCount);
        check.setRuleRefCount(ruleRefCount);
        check.setPublishedVersionCount(publishedVersionCount);
        check.setCanDelete(variableRefCount == 0 && ruleRefCount == 0 && publishedVersionCount == 0);
        check.setCanDisable(publishedVersionCount == 0);
        check.setRemoveBlockingReason(check.getCanDelete() ? "" : buildRemoveBlockingReason(variableRefCount, ruleRefCount, publishedVersionCount));
        check.setDisableBlockingReason(check.getCanDisable() ? "" : "当前公式已进入发布版本快照，请先替换并发布新版本后再停用。");
        return check;
    }

    @Override
    public boolean checkFormulaCodeUnique(CostFormula formula)
    {
        Long formulaId = formula.getFormulaId() == null ? -1L : formula.getFormulaId();
        Long count = formulaMapper.selectCount(Wrappers.<CostFormula>lambdaQuery()
                .eq(CostFormula::getSceneId, formula.getSceneId())
                .eq(CostFormula::getFormulaCode, formula.getFormulaCode())
                .ne(formulaId != -1L, CostFormula::getFormulaId, formulaId));
        return count == null || count == 0 ? UserConstants.UNIQUE : UserConstants.NOT_UNIQUE;
    }

    @Override
    public int insertFormula(CostFormula formula)
    {
        validateFormula(formula);
        fillDefaultFields(formula);
        return formulaMapper.insert(formula);
    }

    @Override
    public int updateFormula(CostFormula formula)
    {
        validateDisableBeforeUpdate(formula);
        validateFormula(formula);
        fillDefaultFields(formula);
        return formulaMapper.updateById(formula);
    }

    @Override
    public int deleteFormulaByIds(Long[] formulaIds)
    {
        for (Long formulaId : formulaIds)
        {
            CostFormulaGovernanceCheckVo check = selectFormulaGovernanceCheck(formulaId);
            if (check != null && !Boolean.TRUE.equals(check.getCanDelete()))
            {
                throw new ServiceException(String.format("%s 不能删除：%s", check.getFormulaName(), check.getRemoveBlockingReason()));
            }
        }
        return formulaMapper.deleteBatchIds(Arrays.asList(formulaIds));
    }

    @Override
    public Map<String, Object> testFormula(CostFormulaTestBo bo, String operator)
    {
        if (bo == null)
        {
            throw new ServiceException("测试请求不能为空");
        }
        CostFormula formula = resolveFormulaForTest(bo);
        Map<String, Object> input = parseInputContext(bo.getInputJson());
        Object result = expressionService.evaluate(formula.getFormulaExpr(), input);

        LinkedHashMap<String, Object> response = new LinkedHashMap<>();
        response.put("formulaCode", formula.getFormulaCode());
        response.put("formulaName", formula.getFormulaName());
        response.put("businessFormula", formula.getBusinessFormula());
        response.put("formulaExpr", formula.getFormulaExpr());
        response.put("input", input);
        response.put("result", result);

        if (formula.getFormulaId() != null)
        {
            formula.setSampleResultJson(writeJson(result));
            formula.setLastTestTime(DateUtils.getNowDate());
            formula.setUpdateBy(operator);
            formulaMapper.updateById(formula);
        }
        return response;
    }

    /**
     * 校验公式配置。
     */
    private void validateFormula(CostFormula formula)
    {
        validateSceneEnabled(formula.getSceneId());
        validateDictValue(DICT_TYPE_FORMULA_STATUS, formula.getStatus(), "公式状态");
        validateDictValue(DICT_TYPE_RETURN_TYPE, formula.getReturnType(), "返回类型");
        expressionService.validateExpression(formula.getFormulaExpr());
        validateTestCaseJson(formula.getTestCaseJson());
        formula.setFormulaCode(StringUtils.trim(formula.getFormulaCode()));
        formula.setFormulaName(StringUtils.trim(formula.getFormulaName()));
        formula.setFormulaDesc(StringUtils.defaultString(formula.getFormulaDesc()));
        formula.setBusinessFormula(StringUtils.defaultString(formula.getBusinessFormula()));
        formula.setNamespaceScope(StringUtils.defaultIfEmpty(StringUtils.trim(formula.getNamespaceScope()), "V,C,I,F,T"));
    }

    /**
     * 停用前校验。
     */
    private void validateDisableBeforeUpdate(CostFormula formula)
    {
        if (formula.getFormulaId() == null)
        {
            return;
        }
        CostFormula current = formulaMapper.selectById(formula.getFormulaId());
        if (current == null)
        {
            throw new ServiceException("当前公式不存在，请刷新后重试");
        }
        if (STATUS_ENABLED.equals(current.getStatus()) && !STATUS_ENABLED.equals(formula.getStatus()))
        {
            CostFormulaGovernanceCheckVo check = selectFormulaGovernanceCheck(formula.getFormulaId());
            if (check != null && !Boolean.TRUE.equals(check.getCanDisable()))
            {
                throw new ServiceException(check.getDisableBlockingReason());
            }
        }
    }

    /**
     * 回填默认值。
     */
    private void fillDefaultFields(CostFormula formula)
    {
        if (formula.getSortNo() == null)
        {
            formula.setSortNo(10);
        }
        if (StringUtils.isEmpty(formula.getStatus()))
        {
            formula.setStatus(STATUS_ENABLED);
        }
        if (StringUtils.isEmpty(formula.getReturnType()))
        {
            formula.setReturnType("NUMBER");
        }
    }

    /**
     * 校验场景状态。
     */
    private void validateSceneEnabled(Long sceneId)
    {
        CostScene scene = sceneMapper.selectById(sceneId);
        if (scene == null)
        {
            throw new ServiceException("所属场景不存在，请刷新后重试");
        }
        if (!STATUS_ENABLED.equals(scene.getStatus()))
        {
            throw new ServiceException("所属场景已停用，不能维护公式");
        }
    }

    /**
     * 校验字典值合法性。
     */
    private void validateDictValue(String dictType, String dictValue, String label)
    {
        if (StringUtils.isEmpty(dictValue))
        {
            throw new ServiceException(label + "不能为空");
        }
        String dictLabel = dictDataMapper.selectDictLabel(dictType, dictValue);
        if (StringUtils.isEmpty(dictLabel))
        {
            throw new ServiceException(label + "不在系统字典范围内");
        }
    }

    /**
     * 校验测试样例 JSON。
     */
    private void validateTestCaseJson(String testCaseJson)
    {
        if (StringUtils.isEmpty(testCaseJson))
        {
            return;
        }
        try
        {
            objectMapper.readValue(testCaseJson, new TypeReference<Map<String, Object>>() {});
        }
        catch (Exception e)
        {
            throw new ServiceException("公式测试样例必须是 JSON 对象");
        }
    }

    /**
     * 解析测试上下文。
     */
    private Map<String, Object> parseInputContext(String inputJson)
    {
        if (StringUtils.isEmpty(inputJson))
        {
            LinkedHashMap<String, Object> empty = new LinkedHashMap<>();
            empty.put("I", new LinkedHashMap<>());
            return empty;
        }
        try
        {
            Map<String, Object> raw = objectMapper.readValue(inputJson, new TypeReference<Map<String, Object>>() {});
            if (raw.containsKey("V") || raw.containsKey("C") || raw.containsKey("I") || raw.containsKey("F") || raw.containsKey("T"))
            {
                return raw;
            }
            LinkedHashMap<String, Object> wrapped = new LinkedHashMap<>();
            wrapped.put("I", raw);
            return wrapped;
        }
        catch (Exception e)
        {
            throw new ServiceException("公式测试输入必须是 JSON 对象");
        }
    }

    /**
     * 解析测试时使用的公式。
     */
    private CostFormula resolveFormulaForTest(CostFormulaTestBo bo)
    {
        if (bo.getFormulaId() != null)
        {
            CostFormula formula = formulaMapper.selectById(bo.getFormulaId());
            if (formula == null)
            {
                throw new ServiceException("待测试公式不存在");
            }
            return formula;
        }
        if (bo.getSceneId() != null && StringUtils.isNotEmpty(bo.getFormulaCode()))
        {
            CostFormula formula = formulaMapper.selectOne(Wrappers.<CostFormula>lambdaQuery()
                    .eq(CostFormula::getSceneId, bo.getSceneId())
                    .eq(CostFormula::getFormulaCode, bo.getFormulaCode()));
            if (formula == null)
            {
                throw new ServiceException("公式编码不存在，请检查后重试");
            }
            return formula;
        }
        if (StringUtils.isEmpty(bo.getFormulaExpr()))
        {
            throw new ServiceException("测试时必须提供公式编码或表达式");
        }
        CostFormula temporary = new CostFormula();
        temporary.setFormulaCode(StringUtils.defaultIfEmpty(bo.getFormulaCode(), "TEMP_FORMULA"));
        temporary.setFormulaName("临时测试公式");
        temporary.setBusinessFormula("临时测试公式");
        temporary.setFormulaExpr(bo.getFormulaExpr());
        expressionService.validateExpression(temporary.getFormulaExpr());
        return temporary;
    }

    /**
     * 删除阻断说明。
     */
    private String buildRemoveBlockingReason(long variableRefCount, long ruleRefCount, long publishedVersionCount)
    {
        StringBuilder builder = new StringBuilder("当前公式已被");
        boolean appended = false;
        if (variableRefCount > 0)
        {
            builder.append(variableRefCount).append("个变量引用");
            appended = true;
        }
        if (ruleRefCount > 0)
        {
            if (appended)
            {
                builder.append("、");
            }
            builder.append(ruleRefCount).append("条规则引用");
            appended = true;
        }
        if (publishedVersionCount > 0)
        {
            if (appended)
            {
                builder.append("、");
            }
            builder.append(publishedVersionCount).append("个发布版本快照引用");
        }
        builder.append("，请先解除引用后再删除。");
        return builder.toString();
    }

    /**
     * 序列化测试结果。
     */
    private String writeJson(Object value)
    {
        try
        {
            return objectMapper.writeValueAsString(value);
        }
        catch (Exception e)
        {
            throw new ServiceException("公式测试结果序列化失败：" + e.getMessage());
        }
    }
}

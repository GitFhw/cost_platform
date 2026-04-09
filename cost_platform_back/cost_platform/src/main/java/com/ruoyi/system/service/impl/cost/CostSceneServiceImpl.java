package com.ruoyi.system.service.impl.cost;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.core.domain.entity.SysDictData;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.system.domain.cost.CostScene;
import com.ruoyi.system.domain.vo.CostSceneGovernanceCheckVo;
import com.ruoyi.system.mapper.SysDictDataMapper;
import com.ruoyi.system.mapper.cost.CostSceneMapper;
import com.ruoyi.system.service.cost.ICostSceneService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * 场景中心服务实现
 *
 * @author HwFan
 */
@Service
public class CostSceneServiceImpl implements ICostSceneService {
    private static final String DICT_TYPE_BUSINESS_DOMAIN = "cost_business_domain";
    private static final String DICT_TYPE_SCENE_TYPE = "cost_scene_type";
    private static final String DICT_TYPE_SCENE_STATUS = "cost_scene_status";

    @Autowired
    private CostSceneMapper sceneMapper;

    @Autowired
    private SysDictDataMapper dictDataMapper;

    /**
     * 查询场景列表
     *
     * @param scene 场景查询对象
     *
     * @return 场景集合
     */
    @Override
    public List<CostScene> selectSceneList(CostScene scene) {
        return sceneMapper.selectSceneList(scene);
    }

    /**
     * 查询场景详情
     *
     * @param sceneId 场景主键
     *
     * @return 场景对象
     */
    @Override
    public CostScene selectSceneById(Long sceneId) {
        return sceneMapper.selectById(sceneId);
    }

    /**
     * 查询场景选择框
     *
     * @param scene 场景查询对象
     *
     * @return 场景集合
     */
    @Override
    public List<CostScene> selectSceneOptions(CostScene scene) {
        return sceneMapper.selectSceneOptions(scene);
    }

    /**
     * 查询场景统计
     *
     * @param scene 场景查询对象
     *
     * @return 统计结果
     */
    @Override
    public Map<String, Object> selectSceneStats(CostScene scene) {
        Map<String, Object> stats = sceneMapper.selectSceneStats(scene);
        LinkedHashMap<String, Object> result = new LinkedHashMap<>();
        result.put("sceneCount", 0);
        result.put("enabledSceneCount", 0);
        result.put("businessDomainCount", 0);
        if (stats == null) {
            return result;
        }
        for (String key : result.keySet()) {
            Object value = stats.get(key);
            result.put(key, value == null ? 0 : value);
        }
        return result;
    }

    /**
     * 查询场景治理预检查结果
     *
     * @param sceneId 场景主键
     *
     * @return 结果
     */
    @Override
    public CostSceneGovernanceCheckVo selectSceneGovernanceCheck(Long sceneId) {
        CostSceneGovernanceCheckVo check = sceneMapper.selectSceneGovernanceCheck(sceneId);
        if (StringUtils.isNull(check)) {
            return null;
        }
        normalizeGovernanceCount(check);

        boolean hasPublishedVersion = check.getPublishedVersionCount() > 0;
        boolean hasActiveVersion = StringUtils.isNotNull(check.getActiveVersionId());
        boolean hasDownstreamConfig = check.getTotalConfigCount() > 0;

        check.setCanDelete(!hasActiveVersion && !hasPublishedVersion && !hasDownstreamConfig);
        check.setCanDisable(!hasActiveVersion && !hasPublishedVersion);
        check.setRemoveBlockingReason(buildRemoveBlockingReason(check, hasActiveVersion, hasPublishedVersion, hasDownstreamConfig));
        check.setDisableBlockingReason(buildDisableBlockingReason(check, hasActiveVersion, hasPublishedVersion));
        check.setRemoveAdvice(check.getCanDelete() ? "当前场景未被下游配置或发布版本占用，可直接删除。"
                : "请先清理场景下费用、变量、规则等配置，并解除已发布/生效版本后再删除。");
        check.setDisableAdvice(check.getCanDisable() ? buildDisableAdvice(check)
                : "请先处理当前生效版本或已发布版本，再执行停用。");
        return check;
    }

    /**
     * 校验场景编码是否唯一
     *
     * @param scene 场景对象
     *
     * @return 结果
     */
    @Override
    public boolean checkSceneCodeUnique(CostScene scene) {
        Long sceneId = StringUtils.isNull(scene.getSceneId()) ? -1L : scene.getSceneId();
        Long count = sceneMapper.selectCount(Wrappers.<CostScene>lambdaQuery()
                .eq(CostScene::getSceneCode, scene.getSceneCode())
                .ne(sceneId.longValue() != -1L, CostScene::getSceneId, sceneId));
        return count != null && count > 0 ? UserConstants.NOT_UNIQUE : UserConstants.UNIQUE;
    }

    /**
     * 新增场景
     *
     * @param scene 场景对象
     *
     * @return 结果
     */
    @Override
    public int insertScene(CostScene scene) {
        normalizeSceneDimension(scene);
        validateSceneDictValue(scene);
        return sceneMapper.insert(scene);
    }

    /**
     * 修改场景
     *
     * @param scene 场景对象
     *
     * @return 结果
     */
    @Override
    public int updateScene(CostScene scene) {
        normalizeSceneDimension(scene);
        validateSceneDictValue(scene);
        validateDisableBeforeUpdate(scene);
        return sceneMapper.updateById(scene);
    }

    private void normalizeSceneDimension(CostScene scene) {
        if (scene == null) {
            return;
        }
        scene.setDefaultObjectDimension(StringUtils.trim(scene.getDefaultObjectDimension()));
    }

    /**
     * 校验场景中心核心字段必须来自系统字典。
     *
     * <p>线程一的治理边界要求“业务域字典化、系统字典收口”必须在后端硬约束，
     * 不能只依赖前端下拉防止非法值写入。这里统一校验业务域、场景类型和场景状态，
     * 让场景主数据始终受控于 cost_ 前缀字典体系。</p>
     *
     * @param scene 场景对象
     */
    private void validateSceneDictValue(CostScene scene) {
        validateDictValueExists(DICT_TYPE_BUSINESS_DOMAIN, scene.getBusinessDomain(), "业务域");
        validateDictValueExists(DICT_TYPE_SCENE_TYPE, scene.getSceneType(), "场景类型");
        validateDictValueExists(DICT_TYPE_SCENE_STATUS, scene.getStatus(), "场景状态");
    }

    /**
     * 批量删除场景
     *
     * @param sceneIds 场景主键数组
     *
     * @return 结果
     */
    @Override
    public int deleteSceneByIds(Long[] sceneIds) {
        for (Long sceneId : sceneIds) {
            CostSceneGovernanceCheckVo check = selectSceneGovernanceCheck(sceneId);
            if (StringUtils.isNull(check)) {
                continue;
            }
            if (!Boolean.TRUE.equals(check.getCanDelete())) {
                throw new ServiceException(String.format("%1$s不能删除：%2$s", check.getSceneName(), check.getRemoveBlockingReason()));
            }
        }
        return sceneMapper.deleteBatchIds(Arrays.asList(sceneIds));
    }

    /**
     * 标准化治理计数
     *
     * @param check 结果
     */
    private void normalizeGovernanceCount(CostSceneGovernanceCheckVo check) {
        check.setFeeCount(nullSafeLong(check.getFeeCount()));
        check.setVariableGroupCount(nullSafeLong(check.getVariableGroupCount()));
        check.setVariableCount(nullSafeLong(check.getVariableCount()));
        check.setRuleCount(nullSafeLong(check.getRuleCount()));
        check.setPublishedVersionCount(nullSafeLong(check.getPublishedVersionCount()));
        check.setTotalConfigCount(check.getFeeCount() + check.getVariableGroupCount() + check.getVariableCount() + check.getRuleCount());
    }

    /**
     * 更新前校验停用动作
     *
     * @param scene 提交对象
     */
    private void validateDisableBeforeUpdate(CostScene scene) {
        if (StringUtils.isNull(scene.getSceneId()) || !"1".equals(scene.getStatus())) {
            return;
        }
        CostScene current = selectSceneById(scene.getSceneId());
        if (StringUtils.isNull(current) || "1".equals(current.getStatus())) {
            return;
        }
        CostSceneGovernanceCheckVo check = selectSceneGovernanceCheck(scene.getSceneId());
        if (StringUtils.isNotNull(check) && !Boolean.TRUE.equals(check.getCanDisable())) {
            throw new ServiceException(String.format("%1$s不能停用：%2$s", check.getSceneName(), check.getDisableBlockingReason()));
        }
    }

    /**
     * 构造删除阻断说明
     *
     * @param check               结果
     * @param hasActiveVersion    是否有生效版本
     * @param hasPublishedVersion 是否有发布版本
     * @param hasDownstreamConfig 是否存在下游配置
     *
     * @return 说明
     */
    private String buildRemoveBlockingReason(CostSceneGovernanceCheckVo check, boolean hasActiveVersion, boolean hasPublishedVersion,
                                             boolean hasDownstreamConfig) {
        if (!hasActiveVersion && !hasPublishedVersion && !hasDownstreamConfig) {
            return "当前场景未被下游配置或发布版本占用";
        }
        StringJoiner joiner = new StringJoiner("；");
        if (hasDownstreamConfig) {
            joiner.add(String.format("已挂载%1$d项下游配置（费用%2$d、变量组%3$d、变量%4$d、规则%5$d）", check.getTotalConfigCount(), check.getFeeCount(),
                    check.getVariableGroupCount(), check.getVariableCount(), check.getRuleCount()));
        }
        if (hasPublishedVersion) {
            joiner.add(String.format("已有%1$d个发布版本", check.getPublishedVersionCount()));
        }
        if (hasActiveVersion) {
            joiner.add(String.format("当前仍绑定生效版本%1$d", check.getActiveVersionId()));
        }
        return joiner.toString();
    }

    /**
     * 构造停用阻断说明
     *
     * @param check               结果
     * @param hasActiveVersion    是否有生效版本
     * @param hasPublishedVersion 是否有发布版本
     *
     * @return 说明
     */
    private String buildDisableBlockingReason(CostSceneGovernanceCheckVo check, boolean hasActiveVersion, boolean hasPublishedVersion) {
        if (!hasActiveVersion && !hasPublishedVersion) {
            return "当前场景未进入发布生效治理，可安全停用";
        }
        StringJoiner joiner = new StringJoiner("；");
        if (hasPublishedVersion) {
            joiner.add(String.format("已有%1$d个发布版本", check.getPublishedVersionCount()));
        }
        if (hasActiveVersion) {
            joiner.add(String.format("当前仍绑定生效版本%1$d", check.getActiveVersionId()));
        }
        return joiner.toString();
    }

    /**
     * 构造停用建议
     *
     * @param check 结果
     *
     * @return 建议
     */
    private String buildDisableAdvice(CostSceneGovernanceCheckVo check) {
        if (check.getTotalConfigCount() <= 0) {
            return "当前场景无下游配置，停用后将从后续维护和选择范围中移除。";
        }
        return String.format("当前场景下已有%1$d项配置对象，停用后将从业务选择范围中移除，但配置数据会继续保留。", check.getTotalConfigCount());
    }

    /**
     * 空值转0
     *
     * @param value 值
     *
     * @return 结果
     */
    private long nullSafeLong(Long value) {
        return StringUtils.isNull(value) ? 0L : value.longValue();
    }

    /**
     * 校验字典值是否存在且处于可用状态。
     *
     * @param dictType   字典类型
     * @param dictValue  字典值
     * @param fieldLabel 业务字段名称
     */
    private void validateDictValueExists(String dictType, String dictValue, String fieldLabel) {
        if (StringUtils.isEmpty(dictValue)) {
            return;
        }
        List<SysDictData> dictDataList = dictDataMapper.selectDictDataByType(dictType);
        boolean matched = dictDataList.stream()
                .anyMatch(item -> dictValue.equals(item.getDictValue()) && "0".equals(item.getStatus()));
        if (!matched) {
            throw new ServiceException(String.format("%s取值无效，请从系统字典 %s 中选择合法值：%s", fieldLabel, dictType, dictValue));
        }
    }
}

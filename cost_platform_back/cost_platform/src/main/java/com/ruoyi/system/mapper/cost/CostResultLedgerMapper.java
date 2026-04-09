package com.ruoyi.system.mapper.cost;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ruoyi.system.domain.cost.CostResultLedger;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.util.List;

/**
 * 结果台账 Mapper
 *
 * @author HwFan
 */
public interface CostResultLedgerMapper extends BaseMapper<CostResultLedger> {
    /**
     * 批量写入结果台账。
     */
    int insertBatch(@Param("list") List<CostResultLedger> list);

    Long countBySceneAndBillMonth(@Param("sceneId") Long sceneId, @Param("billMonth") String billMonth);

    BigDecimal sumAmountBySceneAndBillMonth(@Param("sceneId") Long sceneId, @Param("billMonth") String billMonth);

    BigDecimal sumAmountByTaskId(@Param("taskId") Long taskId);
}

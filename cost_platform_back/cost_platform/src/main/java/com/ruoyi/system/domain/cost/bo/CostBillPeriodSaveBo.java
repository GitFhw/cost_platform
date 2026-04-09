package com.ruoyi.system.domain.cost.bo;

import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 新建账期请求对象
 *
 * @author HwFan
 */
@Data
public class CostBillPeriodSaveBo
{
    @NotNull(message = "所属场景不能为空")
    private Long sceneId;

    @NotBlank(message = "账期不能为空")
    private String billMonth;

    private Long activeVersionId;

    private String remark;
}

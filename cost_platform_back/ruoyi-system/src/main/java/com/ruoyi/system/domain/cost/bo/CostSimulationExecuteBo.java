package com.ruoyi.system.domain.cost.bo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 试算执行请求
 *
 * @author codex
 */
@Data
public class CostSimulationExecuteBo
{
    /** 场景主键 */
    @NotNull(message = "试算场景不能为空")
    private Long sceneId;

    /** 发布版本主键 */
    private Long versionId;

    /** 输入业务数据 JSON */
    @NotBlank(message = "试算输入数据不能为空")
    private String inputJson;
}

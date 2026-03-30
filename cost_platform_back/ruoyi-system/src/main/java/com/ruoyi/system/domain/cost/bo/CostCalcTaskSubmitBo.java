package com.ruoyi.system.domain.cost.bo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 核算任务发起请求
 *
 * @author codex
 */
@Data
public class CostCalcTaskSubmitBo
{
    /** 场景主键 */
    @NotNull(message = "任务场景不能为空")
    private Long sceneId;

    /** 发布版本主键 */
    private Long versionId;

    /** 任务类型 */
    @NotBlank(message = "任务类型不能为空")
    private String taskType;

    /** 账期 */
    @NotBlank(message = "账期不能为空")
    private String billMonth;

    /** 幂等请求号 */
    private String requestNo;

    /** 输入数据 JSON，单笔时为对象，批量时为数组 */
    @NotBlank(message = "任务输入数据不能为空")
    private String inputJson;

    /** 备注 */
    private String remark;
}

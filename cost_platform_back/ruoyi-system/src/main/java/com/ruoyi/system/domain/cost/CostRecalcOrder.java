package com.ruoyi.system.domain.cost;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ruoyi.common.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 重算申请与执行对象 cost_recalc_order
 *
 * <p>线程六将历史账期重算拆成申请、审核、执行和差异记录四段，
 * 避免已封存账期被静默覆盖。</p>
 *
 * @author codex
 */
@Data
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
@TableName("cost_recalc_order")
public class CostRecalcOrder extends BaseEntity
{
    @TableId(value = "recalc_id", type = IdType.AUTO)
    private Long recalcId;

    @TableField("scene_id")
    private Long sceneId;

    @TableField("bill_month")
    private String billMonth;

    @TableField("version_id")
    private Long versionId;

    @TableField("period_id")
    private Long periodId;

    @TableField("baseline_task_id")
    private Long baselineTaskId;

    @TableField("baseline_task_no")
    private String baselineTaskNo;

    @TableField("target_task_id")
    private Long targetTaskId;

    @TableField("target_task_no")
    private String targetTaskNo;

    @TableField("recalc_status")
    private String recalcStatus;

    @TableField("apply_reason")
    private String applyReason;

    @TableField("approve_opinion")
    private String approveOpinion;

    @TableField("diff_summary_json")
    private String diffSummaryJson;

    @TableField("diff_amount")
    private BigDecimal diffAmount;

    @TableField("request_no")
    private String requestNo;

    @TableField("approve_by")
    private String approveBy;

    @TableField("approve_time")
    private Date approveTime;

    @TableField("execute_by")
    private String executeBy;

    @TableField("execute_time")
    private Date executeTime;

    @TableField("finish_time")
    private Date finishTime;

    @TableField(exist = false)
    private String sceneCode;

    @TableField(exist = false)
    private String sceneName;

    @TableField(exist = false)
    private String versionNo;
}

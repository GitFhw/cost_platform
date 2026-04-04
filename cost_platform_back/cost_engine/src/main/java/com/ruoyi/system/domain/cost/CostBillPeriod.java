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
 * 账期治理对象 cost_bill_period
 *
 * <p>线程六使用该表承接账期状态、封存控制、最近任务与结果汇总，
 * 为重算治理、账期封存和差异分析提供统一入口。</p>
 *
 * @author codex
 */
@Data
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
@TableName("cost_bill_period")
public class CostBillPeriod extends BaseEntity
{
    @TableId(value = "period_id", type = IdType.AUTO)
    private Long periodId;

    @TableField("scene_id")
    private Long sceneId;

    @TableField("bill_month")
    private String billMonth;

    @TableField("period_status")
    private String periodStatus;

    @TableField("active_version_id")
    private Long activeVersionId;

    @TableField("result_count")
    private Long resultCount;

    @TableField("amount_total")
    private BigDecimal amountTotal;

    @TableField("last_task_id")
    private Long lastTaskId;

    @TableField("last_task_no")
    private String lastTaskNo;

    @TableField("sealed_by")
    private String sealedBy;

    @TableField("sealed_time")
    private Date sealedTime;

    @TableField(exist = false)
    private String sceneCode;

    @TableField(exist = false)
    private String sceneName;

    @TableField(exist = false)
    private String versionNo;
}

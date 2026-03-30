package com.ruoyi.system.domain.cost;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 结果台账对象 cost_result_ledger
 *
 * <p>线程五按费用粒度写正式结果台账，便于列表按场景、账期、任务号和费用快速筛选。</p>
 *
 * @author codex
 */
@Data
@TableName("cost_result_ledger")
public class CostResultLedger implements Serializable
{
    @Serial
    private static final long serialVersionUID = 1L;

    /** 结果主键 */
    @TableId(value = "result_id", type = IdType.AUTO)
    private Long resultId;

    /** 任务主键 */
    @TableField("task_id")
    private Long taskId;

    /** 任务编号 */
    @TableField("task_no")
    private String taskNo;

    /** 场景主键 */
    @TableField("scene_id")
    private Long sceneId;

    /** 版本主键 */
    @TableField("version_id")
    private Long versionId;

    /** 费用主键 */
    @TableField("fee_id")
    private Long feeId;

    /** 费用编码 */
    @TableField("fee_code")
    private String feeCode;

    /** 费用名称 */
    @TableField("fee_name")
    private String feeName;

    /** 业务单号 */
    @TableField("biz_no")
    private String bizNo;

    /** 账期 */
    @TableField("bill_month")
    private String billMonth;

    /** 核算对象维度 */
    @TableField("object_dimension")
    private String objectDimension;

    /** 核算对象编码 */
    @TableField("object_code")
    private String objectCode;

    /** 核算对象名称 */
    @TableField("object_name")
    private String objectName;

    /** 计量值 */
    @TableField("quantity_value")
    private BigDecimal quantityValue;

    /** 命中单价 */
    @TableField("unit_price")
    private BigDecimal unitPrice;

    /** 结果金额 */
    @TableField("amount_value")
    private BigDecimal amountValue;

    /** 币种 */
    @TableField("currency_code")
    private String currencyCode;

    /** 结果状态 */
    @TableField("result_status")
    private String resultStatus;

    /** 追溯主键 */
    @TableField("trace_id")
    private Long traceId;

    /** 创建时间 */
    @TableField("create_time")
    private Date createTime;

    /** 场景编码 */
    @TableField(exist = false)
    private String sceneCode;

    /** 场景名称 */
    @TableField(exist = false)
    private String sceneName;

    /** 版本号 */
    @TableField(exist = false)
    private String versionNo;
}

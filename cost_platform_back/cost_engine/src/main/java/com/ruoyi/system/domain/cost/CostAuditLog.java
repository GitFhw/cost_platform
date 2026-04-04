package com.ruoyi.system.domain.cost;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * 配置审计日志对象 cost_audit_log
 *
 * <p>线程六开始把发布、运行链、账期治理等关键动作纳入审计台账，
 * 为后续验收、排障和追责提供统一审计视图。</p>
 *
 * @author codex
 */
@Data
@TableName("cost_audit_log")
public class CostAuditLog implements Serializable
{
    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "audit_id", type = IdType.AUTO)
    private Long auditId;

    @TableField("scene_id")
    private Long sceneId;

    @TableField("object_type")
    private String objectType;

    @TableField("object_code")
    private String objectCode;

    @TableField("action_type")
    private String actionType;

    @TableField("action_summary")
    private String actionSummary;

    @TableField("before_json")
    private String beforeJson;

    @TableField("after_json")
    private String afterJson;

    @TableField("operator_code")
    private String operatorCode;

    @TableField("operator_name")
    private String operatorName;

    @TableField("operate_time")
    private Date operateTime;

    @TableField("request_no")
    private String requestNo;

    @TableField(exist = false)
    private String sceneCode;

    @TableField(exist = false)
    private String sceneName;
}

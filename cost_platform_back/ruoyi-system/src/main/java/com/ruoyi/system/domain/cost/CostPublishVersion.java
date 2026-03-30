package com.ruoyi.system.domain.cost;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.core.domain.BaseEntity;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.util.Date;

/**
 * 发布版本对象 cost_publish_version
 *
 * @author codex
 */
@Data
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
@TableName("cost_publish_version")
public class CostPublishVersion extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 发布版本主键 */
    @TableId(value = "version_id", type = IdType.AUTO)
    private Long versionId;

    /** 所属场景主键 */
    @TableField("scene_id")
    private Long sceneId;

    /** 场景编码 */
    @Excel(name = "场景编码")
    @TableField(exist = false)
    private String sceneCode;

    /** 场景名称 */
    @Excel(name = "场景名称")
    @TableField(exist = false)
    private String sceneName;

    /** 业务域 */
    @Excel(name = "业务域", dictType = "cost_business_domain")
    @TableField(exist = false)
    private String businessDomain;

    /** 版本号 */
    @Excel(name = "版本号")
    @TableField("version_no")
    @Size(max = 64, message = "版本号长度不能超过64个字符")
    private String versionNo;

    /** 版本状态 */
    @Excel(name = "版本状态", dictType = "cost_publish_version_status")
    @TableField("version_status")
    private String versionStatus;

    /** 发布说明 */
    @Excel(name = "发布说明")
    @TableField("publish_desc")
    @Size(max = 1000, message = "发布说明长度不能超过1000个字符")
    private String publishDesc;

    /** 发布校验结果快照 */
    @TableField("validation_result_json")
    private String validationResultJson;

    /** 快照哈希 */
    @TableField("snapshot_hash")
    private String snapshotHash;

    /** 发布人 */
    @Excel(name = "发布人")
    @TableField("published_by")
    private String publishedBy;

    /** 发布时间 */
    @Excel(name = "发布时间")
    @TableField("published_time")
    private Date publishedTime;

    /** 生效操作人 */
    @Excel(name = "生效操作人")
    @TableField("activated_by")
    private String activatedBy;

    /** 生效时间 */
    @Excel(name = "生效时间")
    @TableField("activated_time")
    private Date activatedTime;

    /** 回滚操作人 */
    @Excel(name = "回滚操作人")
    @TableField("rollback_by")
    private String rollbackBy;

    /** 回滚时间 */
    @Excel(name = "回滚时间")
    @TableField("rollback_time")
    private Date rollbackTime;

    /** 是否当前生效版本 */
    @TableField(exist = false)
    private Integer activeFlag;

    /** 上一个版本主键 */
    @TableField(exist = false)
    private Long previousVersionId;

    /** 上一个版本号 */
    @TableField(exist = false)
    private String previousVersionNo;
}

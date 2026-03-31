package com.ruoyi.system.domain.cost;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.core.domain.BaseEntity;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

/**
 * 变量主数据对象 cost_variable
 *
 * @author codex
 */
@Data
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
@TableName("cost_variable")
public class CostVariable extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 变量主键 */
    @TableId(value = "variable_id", type = IdType.AUTO)
    private Long variableId;

    /** 所属场景主键 */
    @Excel(name = "场景主键")
    @TableField("scene_id")
    @NotNull(message = "所属场景不能为空")
    private Long sceneId;

    /** 所属分组主键 */
    @Excel(name = "分组主键")
    @TableField("group_id")
    private Long groupId;

    /** 检索关键词（变量编码/变量名称） */
    @TableField(exist = false)
    private String keyword;

    /** 场景编码 */
    @Excel(name = "场景编码")
    @TableField(exist = false)
    private String sceneCode;

    /** 场景名称 */
    @Excel(name = "所属场景")
    @TableField(exist = false)
    private String sceneName;

    /** 业务域 */
    @Excel(name = "业务域", dictType = "cost_business_domain")
    @TableField(exist = false)
    private String businessDomain;

    /** 分组编码 */
    @Excel(name = "分组编码")
    @TableField(exist = false)
    private String groupCode;

    /** 分组名称 */
    @Excel(name = "变量分组")
    @TableField(exist = false)
    private String groupName;

    /** 变量编码 */
    @Excel(name = "变量编码")
    @TableField("variable_code")
    @NotBlank(message = "变量编码不能为空")
    @Size(max = 64, message = "变量编码长度不能超过64个字符")
    private String variableCode;

    /** 变量名称 */
    @Excel(name = "变量名称")
    @TableField("variable_name")
    @NotBlank(message = "变量名称不能为空")
    @Size(max = 128, message = "变量名称长度不能超过128个字符")
    private String variableName;

    /** 变量类型 */
    @Excel(name = "变量类型", dictType = "cost_variable_type")
    @TableField("variable_type")
    @NotBlank(message = "变量类型不能为空")
    @Size(max = 32, message = "变量类型长度不能超过32个字符")
    private String variableType;

    /** 来源类型 */
    @Excel(name = "来源类型", dictType = "cost_variable_source_type")
    @TableField("source_type")
    @NotBlank(message = "来源类型不能为空")
    @Size(max = 32, message = "来源类型长度不能超过32个字符")
    private String sourceType;

    /** 来源系统标识 */
    @Excel(name = "来源系统")
    @TableField("source_system")
    @Size(max = 64, message = "来源系统标识长度不能超过64个字符")
    private String sourceSystem;

    /** 字典类型 */
    @Excel(name = "字典类型")
    @TableField("dict_type")
    @Size(max = 64, message = "字典类型长度不能超过64个字符")
    private String dictType;

    /** 远程接口地址 */
    @Excel(name = "远程接口")
    @TableField("remote_api")
    @Size(max = 255, message = "远程接口地址长度不能超过255个字符")
    private String remoteApi;

    /** 鉴权方式 */
    @Excel(name = "鉴权方式", dictType = "cost_variable_auth_type")
    @TableField("auth_type")
    @Size(max = 32, message = "鉴权方式长度不能超过32个字符")
    private String authType;

    /** 鉴权配置JSON */
    @Excel(name = "鉴权配置")
    @TableField("auth_config_json")
    private String authConfigJson;

    /** 远程数据路径 */
    @Excel(name = "数据路径")
    @TableField("data_path")
    @Size(max = 255, message = "数据路径长度不能超过255个字符")
    private String dataPath;

    /** 字段映射配置JSON */
    @Excel(name = "字段映射配置")
    @TableField("mapping_config_json")
    private String mappingConfigJson;

    /** 同步方式 */
    @Excel(name = "同步方式", dictType = "cost_variable_sync_mode")
    @TableField("sync_mode")
    @Size(max = 32, message = "同步方式长度不能超过32个字符")
    private String syncMode;

    /** 缓存策略 */
    @Excel(name = "缓存策略", dictType = "cost_variable_cache_policy")
    @TableField("cache_policy")
    @Size(max = 32, message = "缓存策略长度不能超过32个字符")
    private String cachePolicy;

    /** 失败兜底策略 */
    @Excel(name = "失败兜底策略", dictType = "cost_variable_fallback_policy")
    @TableField("fallback_policy")
    @Size(max = 32, message = "失败兜底策略长度不能超过32个字符")
    private String fallbackPolicy;

    /** 公式表达式 */
    @Excel(name = "公式表达式")
    @TableField("formula_expr")
    @Size(max = 2000, message = "公式表达式长度不能超过2000个字符")
    private String formulaExpr;

    /** 数据类型 */
    @Excel(name = "数据类型", dictType = "cost_variable_data_type")
    @TableField("data_type")
    @Size(max = 32, message = "数据类型长度不能超过32个字符")
    private String dataType;

    /** 默认值 */
    @Excel(name = "默认值")
    @TableField("default_value")
    @Size(max = 255, message = "默认值长度不能超过255个字符")
    private String defaultValue;

    /** 精度 */
    @Excel(name = "精度")
    @TableField("precision_scale")
    private Integer precisionScale;

    /** 状态 */
    @Excel(name = "状态", dictType = "cost_variable_status")
    @TableField("status")
    @NotBlank(message = "变量状态不能为空")
    private String status;

    /** 排序号 */
    @Excel(name = "排序号")
    @TableField("sort_no")
    private Integer sortNo;
}

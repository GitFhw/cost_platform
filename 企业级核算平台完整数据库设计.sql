-- =========================================================
-- 企业级成本核算平台完整数据库设计脚本
-- 项目：cost_platform
-- 说明：
-- 1. 本脚本用于定义企业级核算平台核心业务表
-- 2. 所有业务表统一使用 cost_ 前缀
-- 3. 所有表和字段均带中文注释
-- 4. 设计目标包含多行业、多场景、多费用、多规则、多版本
-- 5. 同时考虑月近 100 万条计费数据的正式核算性能要求
-- =========================================================

set names utf8mb4;
set foreign_key_checks = 0;

-- =========================================================
-- 一、场景与费用主数据
-- =========================================================

drop table if exists cost_scene;
create table cost_scene (
  scene_id                  bigint          not null auto_increment comment '场景主键',
  scene_code                varchar(64)     not null comment '场景编码，用于唯一标识一个核算主题或合同场景',
  scene_name                varchar(128)    not null comment '场景名称，供业务人员识别核算主题',
  business_domain           varchar(64)     not null comment '业务域字典值，对应 cost_business_domain',
  org_code                  varchar(64)     default '' comment '所属组织编码，用于按组织隔离场景',
  scene_type                varchar(32)     default 'CONTRACT' comment '场景类型，例如合同、方案、公司级核算域',
  active_version_id         bigint          default null comment '当前生效版本主键，对应 cost_publish_version.version_id',
  status                    char(1)         not null default '0' comment '场景状态（0正常 1停用 2草稿）',
  remark                    varchar(500)    default null comment '场景说明，用于补充业务口径和适用边界',
  create_by                 varchar(64)     default '' comment '创建人',
  create_time               datetime        default current_timestamp comment '创建时间',
  update_by                 varchar(64)     default '' comment '更新人',
  update_time               datetime        default current_timestamp on update current_timestamp comment '更新时间',
  primary key (scene_id),
  unique key uk_cost_scene_code (scene_code),
  key idx_cost_scene_domain_status (business_domain, status),
  key idx_cost_scene_org_status (org_code, status)
) engine=innodb default charset=utf8mb4 comment='核算平台-场景主数据表';

drop table if exists cost_fee_item;
create table cost_fee_item (
  fee_id                    bigint          not null auto_increment comment '费用主键',
  scene_id                  bigint          not null comment '所属场景主键，对应 cost_scene.scene_id',
  fee_code                  varchar(64)     not null comment '费用编码，用于唯一标识一个费用项',
  fee_name                  varchar(128)    not null comment '费用名称，供业务人员识别费用项',
  fee_category              varchar(64)     default '' comment '费用分类，例如固定薪资、补贴、港杂费、附加费',
  unit_code                 varchar(32)     default '' comment '计价单位编码，例如人、吨、箱、元',
  factor_summary            varchar(255)    default '' comment '影响因素摘要，用于帮助业务快速理解费用依赖',
  scope_description         varchar(255)    default '' comment '适用范围说明，补充当前场景下该费用的适用边界',
  object_dimension          varchar(64)     default '' comment '核算对象维度，例如人、班组、协力公司、船舶',
  sort_no                   int             not null default 0 comment '排序号，用于列表和工作台展示顺序',
  status                    char(1)         not null default '0' comment '费用状态（0正常 1停用）',
  remark                    varchar(500)    default null comment '备注',
  create_by                 varchar(64)     default '' comment '创建人',
  create_time               datetime        default current_timestamp comment '创建时间',
  update_by                 varchar(64)     default '' comment '更新人',
  update_time               datetime        default current_timestamp on update current_timestamp comment '更新时间',
  primary key (fee_id),
  unique key uk_cost_fee_scene_code (scene_id, fee_code),
  key idx_cost_fee_scene_status (scene_id, status),
  key idx_cost_fee_scene_sort (scene_id, sort_no),
  constraint fk_cost_fee_scene foreign key (scene_id) references cost_scene (scene_id)
) engine=innodb default charset=utf8mb4 comment='核算平台-费用主数据表';

-- =========================================================
-- 二、变量与影响因素
-- =========================================================

drop table if exists cost_variable_group;
create table cost_variable_group (
  group_id                  bigint          not null auto_increment comment '变量分组主键',
  scene_id                  bigint          not null comment '所属场景主键',
  group_code                varchar(64)     not null comment '变量分组编码',
  group_name                varchar(128)    not null comment '变量分组名称',
  sort_no                   int             not null default 0 comment '排序号',
  status                    char(1)         not null default '0' comment '分组状态（0正常 1停用）',
  remark                    varchar(500)    default null comment '备注',
  create_by                 varchar(64)     default '' comment '创建人',
  create_time               datetime        default current_timestamp comment '创建时间',
  update_by                 varchar(64)     default '' comment '更新人',
  update_time               datetime        default current_timestamp on update current_timestamp comment '更新时间',
  primary key (group_id),
  unique key uk_cost_var_group_scene_code (scene_id, group_code),
  key idx_cost_var_group_scene_sort (scene_id, sort_no),
  constraint fk_cost_var_group_scene foreign key (scene_id) references cost_scene (scene_id)
) engine=innodb default charset=utf8mb4 comment='核算平台-变量分组表';

drop table if exists cost_variable;
create table cost_variable (
  variable_id               bigint          not null auto_increment comment '变量主键',
  scene_id                  bigint          not null comment '所属场景主键',
  group_id                  bigint          default null comment '所属变量分组主键',
  variable_code             varchar(64)     not null comment '变量编码，用于规则、公式、快照引用',
  variable_name             varchar(128)    not null comment '变量名称，供业务人员识别',
  variable_type             varchar(32)     not null comment '变量类型，例如NUMBER、TEXT、DICT、REMOTE、FORMULA、BOOLEAN、DATE',
  source_type               varchar(32)     not null comment '变量来源，例如INPUT、DICT、REMOTE、FORMULA',
  dict_type                 varchar(64)     default '' comment '字典类型，当变量来源为字典时使用',
  remote_api                varchar(255)    default '' comment '远程接口地址或标识，当变量来源为接口时使用',
  data_path                 varchar(255)    default '' comment '取值路径，用于从外部数据中提取字段值',
  formula_expr              varchar(2000)   default null comment '公式表达式，当变量为公式变量时使用',
  data_type                 varchar(32)     default 'STRING' comment '数据类型，例如STRING、NUMBER、BOOLEAN、DATE',
  default_value             varchar(255)    default '' comment '默认值',
  precision_scale           int             default 2 comment '数值精度，用于金额或数量变量',
  status                    char(1)         not null default '0' comment '变量状态（0正常 1停用）',
  sort_no                   int             not null default 0 comment '排序号',
  remark                    varchar(500)    default null comment '备注',
  create_by                 varchar(64)     default '' comment '创建人',
  create_time               datetime        default current_timestamp comment '创建时间',
  update_by                 varchar(64)     default '' comment '更新人',
  update_time               datetime        default current_timestamp on update current_timestamp comment '更新时间',
  primary key (variable_id),
  unique key uk_cost_variable_scene_code (scene_id, variable_code),
  key idx_cost_variable_scene_group (scene_id, group_id),
  key idx_cost_variable_scene_status (scene_id, status),
  constraint fk_cost_variable_scene foreign key (scene_id) references cost_scene (scene_id),
  constraint fk_cost_variable_group foreign key (group_id) references cost_variable_group (group_id)
) engine=innodb default charset=utf8mb4 comment='核算平台-变量主数据表';

drop table if exists cost_fee_variable_rel;
create table cost_fee_variable_rel (
  rel_id                    bigint          not null auto_increment comment '费用与变量关系主键',
  scene_id                  bigint          not null comment '所属场景主键',
  fee_id                    bigint          not null comment '费用主键',
  variable_id               bigint          not null comment '变量主键',
  relation_type             varchar(32)     not null default 'OPTIONAL' comment '关系类型，例如REQUIRED、OPTIONAL、TIER_BASIS、FORMULA_INPUT',
  sort_no                   int             not null default 0 comment '排序号',
  remark                    varchar(500)    default null comment '备注',
  create_by                 varchar(64)     default '' comment '创建人',
  create_time               datetime        default current_timestamp comment '创建时间',
  update_by                 varchar(64)     default '' comment '更新人',
  update_time               datetime        default current_timestamp on update current_timestamp comment '更新时间',
  primary key (rel_id),
  unique key uk_cost_fee_var_rel (fee_id, variable_id, relation_type),
  key idx_cost_fee_var_scene_fee (scene_id, fee_id),
  key idx_cost_fee_var_scene_var (scene_id, variable_id),
  constraint fk_cost_fee_var_scene foreign key (scene_id) references cost_scene (scene_id),
  constraint fk_cost_fee_var_fee foreign key (fee_id) references cost_fee_item (fee_id),
  constraint fk_cost_fee_var_variable foreign key (variable_id) references cost_variable (variable_id)
) engine=innodb default charset=utf8mb4 comment='核算平台-费用与变量适用关系表';

-- =========================================================
-- 三、规则与条件
-- =========================================================

drop table if exists cost_rule;
create table cost_rule (
  rule_id                    bigint          not null auto_increment comment '规则主键',
  scene_id                   bigint          not null comment '所属场景主键',
  fee_id                     bigint          not null comment '所属费用主键',
  rule_code                  varchar(64)     not null comment '规则编码',
  rule_name                  varchar(128)    default '' comment '规则名称，便于业务识别',
  rule_type                  varchar(32)     not null comment '规则类型，例如FIXED_RATE、FIXED_AMOUNT、FORMULA、TIER_RATE、ALLOCATE',
  condition_logic            varchar(16)     not null default 'AND' comment '条件逻辑，例如AND、OR',
  priority                   int             not null default 0 comment '优先级，数值越大越优先',
  quantity_variable_code     varchar(64)     default '' comment '阶梯或公式依赖的数量变量编码',
  pricing_mode               varchar(32)     not null default 'TYPED' comment '定价模式，例如TYPED、ADVANCED_JSON',
  pricing_json               json            default null comment '规则定价结构化配置，承接固定费率、固定金额、阶梯明细、公式等',
  amount_formula             varchar(2000)   default null comment '金额公式表达式',
  note_template              varchar(500)    default '' comment '结果备注模板',
  status                     char(1)         not null default '0' comment '规则状态（0正常 1停用）',
  sort_no                    int             not null default 0 comment '排序号',
  remark                     varchar(500)    default null comment '备注',
  create_by                  varchar(64)     default '' comment '创建人',
  create_time                datetime        default current_timestamp comment '创建时间',
  update_by                  varchar(64)     default '' comment '更新人',
  update_time                datetime        default current_timestamp on update current_timestamp comment '更新时间',
  primary key (rule_id),
  unique key uk_cost_rule_scene_code (scene_id, rule_code),
  key idx_cost_rule_scene_fee (scene_id, fee_id),
  key idx_cost_rule_scene_status_priority (scene_id, status, priority),
  constraint fk_cost_rule_scene foreign key (scene_id) references cost_scene (scene_id),
  constraint fk_cost_rule_fee foreign key (fee_id) references cost_fee_item (fee_id)
) engine=innodb default charset=utf8mb4 comment='核算平台-费率规则主表';

drop table if exists cost_rule_condition;
create table cost_rule_condition (
  condition_id               bigint          not null auto_increment comment '规则条件主键',
  scene_id                   bigint          not null comment '所属场景主键',
  rule_id                    bigint          not null comment '所属规则主键',
  group_no                   int             not null default 1 comment '组号，用于表达同组条件',
  sort_no                    int             not null default 0 comment '排序号',
  variable_code              varchar(64)     not null comment '变量编码，指向规则判断使用的变量',
  display_name               varchar(128)    default '' comment '显示名称，用于业务页展示',
  operator_code              varchar(32)     not null comment '操作符，例如EQ、IN、GT、GTE、LT、LTE、BETWEEN',
  compare_value              varchar(1000)   default '' comment '比较值，统一以字符串存储并由前端/服务层解析',
  status                     char(1)         not null default '0' comment '条件状态（0正常 1停用）',
  remark                     varchar(500)    default null comment '备注',
  create_by                  varchar(64)     default '' comment '创建人',
  create_time                datetime        default current_timestamp comment '创建时间',
  update_by                  varchar(64)     default '' comment '更新人',
  update_time                datetime        default current_timestamp on update current_timestamp comment '更新时间',
  primary key (condition_id),
  key idx_cost_rule_cond_rule_group (rule_id, group_no, sort_no),
  key idx_cost_rule_cond_scene_var (scene_id, variable_code),
  constraint fk_cost_rule_condition_scene foreign key (scene_id) references cost_scene (scene_id),
  constraint fk_cost_rule_condition_rule foreign key (rule_id) references cost_rule (rule_id)
) engine=innodb default charset=utf8mb4 comment='核算平台-规则条件表';

drop table if exists cost_rule_tier;
create table cost_rule_tier (
  tier_id                    bigint          not null auto_increment comment '阶梯主键',
  scene_id                   bigint          not null comment '所属场景主键',
  rule_id                    bigint          not null comment '所属规则主键',
  start_value                decimal(18,4)   default null comment '阶梯起始值，允许为空表示首档负无穷起始',
  end_value                  decimal(18,4)   default null comment '阶梯截止值，允许为空表示末档无上限',
  rate_value                 decimal(18,6)   not null comment '阶梯费率或阶梯单价',
  interval_mode              varchar(16)     not null default 'LEFT_CLOSED_RIGHT_OPEN' comment '区间模式，例如LEFT_CLOSED_RIGHT_OPEN、LEFT_OPEN_RIGHT_CLOSED',
  tier_no                    int             not null default 1 comment '阶梯序号',
  status                     char(1)         not null default '0' comment '阶梯状态（0正常 1停用）',
  remark                     varchar(500)    default null comment '备注',
  create_by                  varchar(64)     default '' comment '创建人',
  create_time                datetime        default current_timestamp comment '创建时间',
  update_by                  varchar(64)     default '' comment '更新人',
  update_time                datetime        default current_timestamp on update current_timestamp comment '更新时间',
  primary key (tier_id),
  key idx_cost_rule_tier_rule_no (rule_id, tier_no),
  key idx_cost_rule_tier_scene_rule (scene_id, rule_id),
  constraint fk_cost_rule_tier_scene foreign key (scene_id) references cost_scene (scene_id),
  constraint fk_cost_rule_tier_rule foreign key (rule_id) references cost_rule (rule_id)
) engine=innodb default charset=utf8mb4 comment='核算平台-规则阶梯明细表';

-- =========================================================
-- 四、发布版本与快照
-- =========================================================

drop table if exists cost_publish_version;
create table cost_publish_version (
  version_id                 bigint          not null auto_increment comment '发布版本主键',
  scene_id                   bigint          not null comment '所属场景主键',
  version_no                 varchar(64)     not null comment '发布版本号，例如 V2026.03.001',
  version_status             varchar(32)     not null default 'PUBLISHED' comment '版本状态，例如DRAFT、PUBLISHED、ACTIVE、ROLLED_BACK',
  publish_desc               varchar(1000)   default '' comment '发布说明',
  validation_result_json     json            default null comment '发布校验结果快照',
  snapshot_hash              varchar(128)    default '' comment '快照哈希，用于快速识别配置是否变化',
  published_by               varchar(64)     default '' comment '发布人',
  published_time             datetime        default null comment '发布时间',
  activated_by               varchar(64)     default '' comment '生效操作人',
  activated_time             datetime        default null comment '生效时间',
  rollback_by                varchar(64)     default '' comment '回滚操作人',
  rollback_time              datetime        default null comment '回滚时间',
  create_by                  varchar(64)     default '' comment '创建人',
  create_time                datetime        default current_timestamp comment '创建时间',
  update_by                  varchar(64)     default '' comment '更新人',
  update_time                datetime        default current_timestamp on update current_timestamp comment '更新时间',
  primary key (version_id),
  unique key uk_cost_publish_scene_ver (scene_id, version_no),
  key idx_cost_publish_scene_status (scene_id, version_status),
  key idx_cost_publish_scene_time (scene_id, published_time),
  constraint fk_cost_publish_scene foreign key (scene_id) references cost_scene (scene_id)
) engine=innodb default charset=utf8mb4 comment='核算平台-场景发布版本表';

drop table if exists cost_publish_snapshot;
create table cost_publish_snapshot (
  snapshot_id                bigint          not null auto_increment comment '快照明细主键',
  version_id                 bigint          not null comment '所属发布版本主键',
  snapshot_type              varchar(32)     not null comment '快照对象类型，例如SCENE、FEE、VARIABLE、RULE、RULE_CONDITION、RULE_TIER',
  object_code                varchar(64)     not null comment '对象编码，用于识别被快照的业务对象',
  object_name                varchar(128)    default '' comment '对象名称，用于工作台和审计展示',
  snapshot_json              json            not null comment '业务对象快照 JSON',
  sort_no                    int             not null default 0 comment '排序号',
  create_by                  varchar(64)     default '' comment '创建人',
  create_time                datetime        default current_timestamp comment '创建时间',
  primary key (snapshot_id),
  key idx_cost_snapshot_version_type (version_id, snapshot_type),
  key idx_cost_snapshot_version_code (version_id, object_code),
  constraint fk_cost_snapshot_version foreign key (version_id) references cost_publish_version (version_id)
) engine=innodb default charset=utf8mb4 comment='核算平台-发布快照明细表';

-- =========================================================
-- 五、试算与正式核算任务
-- =========================================================

drop table if exists cost_simulation_record;
create table cost_simulation_record (
  simulation_id              bigint          not null auto_increment comment '试算记录主键',
  scene_id                   bigint          not null comment '场景主键',
  version_id                 bigint          default null comment '试算使用的版本主键，优先记录发布版本',
  simulation_no              varchar(64)     not null comment '试算编号',
  input_json                 json            not null comment '试算输入数据',
  variable_json              json            default null comment '变量计算结果',
  explain_json               json            default null comment '试算解释结果，包括命中规则、阶梯、公式等',
  result_json                json            default null comment '试算输出结果',
  status                     varchar(32)     not null default 'SUCCESS' comment '试算状态，例如SUCCESS、FAILED',
  error_message              varchar(1000)   default '' comment '失败信息',
  create_by                  varchar(64)     default '' comment '创建人',
  create_time                datetime        default current_timestamp comment '创建时间',
  primary key (simulation_id),
  unique key uk_cost_simulation_no (simulation_no),
  key idx_cost_simulation_scene_time (scene_id, create_time),
  constraint fk_cost_simulation_scene foreign key (scene_id) references cost_scene (scene_id),
  constraint fk_cost_simulation_version foreign key (version_id) references cost_publish_version (version_id)
) engine=innodb default charset=utf8mb4 comment='核算平台-试算记录表';

drop table if exists cost_calc_task;
create table cost_calc_task (
  task_id                    bigint          not null auto_increment comment '核算任务主键',
  task_no                    varchar(64)     not null comment '任务编号',
  scene_id                   bigint          not null comment '场景主键',
  version_id                 bigint          not null comment '运行使用的发布版本主键',
  task_type                  varchar(32)     not null comment '任务类型，例如FORMAL_SINGLE、FORMAL_BATCH、SIMULATION_BATCH',
  bill_month                 varchar(16)     default '' comment '账期，例如2026-03',
  source_count               int             not null default 0 comment '输入数据总量',
  success_count              int             not null default 0 comment '成功处理数量',
  fail_count                 int             not null default 0 comment '失败数量',
  task_status                varchar(32)     not null default 'INIT' comment '任务状态，例如INIT、RUNNING、SUCCESS、PART_SUCCESS、FAILED',
  progress_percent           decimal(5,2)    not null default 0 comment '任务进度百分比',
  started_time               datetime        default null comment '开始时间',
  finished_time              datetime        default null comment '结束时间',
  duration_ms                bigint          default 0 comment '任务总耗时，单位毫秒',
  request_no                 varchar(64)     default '' comment '幂等请求号',
  execute_node               varchar(128)    default '' comment '执行节点标识，用于分布式任务追踪',
  error_message              varchar(1000)   default '' comment '任务失败摘要',
  remark                     varchar(500)    default null comment '备注',
  create_by                  varchar(64)     default '' comment '创建人',
  create_time                datetime        default current_timestamp comment '创建时间',
  update_by                  varchar(64)     default '' comment '更新人',
  update_time                datetime        default current_timestamp on update current_timestamp comment '更新时间',
  primary key (task_id),
  unique key uk_cost_calc_task_no (task_no),
  key idx_cost_calc_task_scene_month (scene_id, bill_month),
  key idx_cost_calc_task_scene_status (scene_id, task_status),
  key idx_cost_calc_task_version (version_id),
  constraint fk_cost_calc_task_scene foreign key (scene_id) references cost_scene (scene_id),
  constraint fk_cost_calc_task_version foreign key (version_id) references cost_publish_version (version_id)
) engine=innodb default charset=utf8mb4 comment='核算平台-正式核算任务表';

drop table if exists cost_calc_task_detail;
create table cost_calc_task_detail (
  detail_id                  bigint          not null auto_increment comment '任务明细主键',
  task_id                    bigint          not null comment '所属任务主键',
  task_no                    varchar(64)     not null comment '任务编号冗余字段，便于快速过滤',
  biz_no                     varchar(128)    not null comment '业务单号，用于唯一标识一条待计费业务数据',
  partition_no               int             not null default 1 comment '分片号，用于批量任务并行处理',
  detail_status              varchar(32)     not null default 'INIT' comment '明细状态，例如INIT、SUCCESS、FAILED',
  retry_count                int             not null default 0 comment '重试次数',
  input_json                 json            not null comment '输入业务数据',
  result_summary             varchar(1000)   default '' comment '结果摘要，用于列表快速展示',
  error_message              varchar(1000)   default '' comment '失败信息',
  create_time                datetime        default current_timestamp comment '创建时间',
  update_time                datetime        default current_timestamp on update current_timestamp comment '更新时间',
  primary key (detail_id),
  unique key uk_cost_calc_task_detail (task_id, biz_no),
  key idx_cost_calc_task_detail_task_status (task_id, detail_status),
  key idx_cost_calc_task_detail_task_partition (task_id, partition_no),
  key idx_cost_calc_task_detail_task_no (task_no),
  constraint fk_cost_calc_task_detail_task foreign key (task_id) references cost_calc_task (task_id)
) engine=innodb default charset=utf8mb4 comment='核算平台-正式核算任务明细表';

-- =========================================================
-- 六、结果台账与追溯
-- =========================================================

drop table if exists cost_result_ledger;
create table cost_result_ledger (
  result_id                  bigint          not null auto_increment comment '结果台账主键',
  task_id                    bigint          not null comment '所属任务主键',
  task_no                    varchar(64)     not null comment '任务编号冗余字段，便于查询',
  scene_id                   bigint          not null comment '场景主键',
  version_id                 bigint          not null comment '发布版本主键',
  fee_id                     bigint          not null comment '费用主键',
  fee_code                   varchar(64)     not null comment '费用编码',
  fee_name                   varchar(128)    not null comment '费用名称',
  biz_no                     varchar(128)    not null comment '业务单号',
  bill_month                 varchar(16)     not null comment '账期，例如2026-03',
  object_dimension           varchar(64)     default '' comment '核算对象维度，例如人、班组、协力公司、船舶',
  object_code                varchar(128)    default '' comment '核算对象编码',
  object_name                varchar(128)    default '' comment '核算对象名称',
  quantity_value             decimal(18,4)   default null comment '参与费率计算的数量值',
  unit_price                 decimal(18,6)   default null comment '最终命中的单价或费率',
  amount_value               decimal(18,2)   not null comment '最终金额',
  currency_code              varchar(32)     default 'CNY' comment '币种编码',
  result_status              varchar(32)     not null default 'SUCCESS' comment '结果状态，例如SUCCESS、FAILED、ADJUSTED',
  trace_id                   bigint          default null comment '追溯记录主键，对应 cost_result_trace.trace_id',
  create_time                datetime        default current_timestamp comment '创建时间',
  primary key (result_id),
  key idx_cost_result_scene_month (scene_id, bill_month),
  key idx_cost_result_task_fee (task_id, fee_id),
  key idx_cost_result_biz_fee (biz_no, fee_code),
  key idx_cost_result_version (version_id),
  constraint fk_cost_result_task foreign key (task_id) references cost_calc_task (task_id),
  constraint fk_cost_result_scene foreign key (scene_id) references cost_scene (scene_id),
  constraint fk_cost_result_version foreign key (version_id) references cost_publish_version (version_id),
  constraint fk_cost_result_fee foreign key (fee_id) references cost_fee_item (fee_id)
) engine=innodb default charset=utf8mb4 comment='核算平台-结果台账表';

drop table if exists cost_result_trace;
create table cost_result_trace (
  trace_id                   bigint          not null auto_increment comment '结果追溯主键',
  scene_id                   bigint          not null comment '场景主键',
  version_id                 bigint          not null comment '发布版本主键',
  rule_id                    bigint          default null comment '命中的规则主键',
  tier_id                    bigint          default null comment '命中的阶梯主键',
  variable_json              json            default null comment '变量计算结果，用于解释命中过程',
  condition_json             json            default null comment '规则条件匹配结果',
  pricing_json               json            default null comment '定价过程结果，包括单价来源、阶梯摘要等',
  timeline_json              json            default null comment '执行时间线，用于回放输入、变量、规则、金额和结果过程',
  create_time                datetime        default current_timestamp comment '创建时间',
  primary key (trace_id),
  key idx_cost_trace_scene_ver (scene_id, version_id),
  key idx_cost_trace_rule (rule_id),
  key idx_cost_trace_tier (tier_id),
  constraint fk_cost_trace_scene foreign key (scene_id) references cost_scene (scene_id),
  constraint fk_cost_trace_version foreign key (version_id) references cost_publish_version (version_id),
  constraint fk_cost_trace_rule foreign key (rule_id) references cost_rule (rule_id),
  constraint fk_cost_trace_tier foreign key (tier_id) references cost_rule_tier (tier_id)
) engine=innodb default charset=utf8mb4 comment='核算平台-结果追溯解释表';

-- =========================================================
-- 七、治理与审计
-- =========================================================

drop table if exists cost_audit_log;
create table cost_audit_log (
  audit_id                   bigint          not null auto_increment comment '审计日志主键',
  scene_id                   bigint          default null comment '所属场景主键',
  object_type                varchar(32)     not null comment '对象类型，例如SCENE、FEE、VARIABLE、RULE、PUBLISH、DICT',
  object_code                varchar(64)     default '' comment '对象编码',
  action_type                varchar(32)     not null comment '操作类型，例如CREATE、UPDATE、DELETE、DISABLE、PUBLISH、ACTIVATE、ROLLBACK',
  action_summary             varchar(500)    default '' comment '操作摘要',
  before_json                json            default null comment '变更前快照',
  after_json                 json            default null comment '变更后快照',
  operator_code              varchar(64)     default '' comment '操作人编码',
  operator_name              varchar(128)    default '' comment '操作人名称',
  operate_time               datetime        default current_timestamp comment '操作时间',
  request_no                 varchar(64)     default '' comment '请求流水号',
  primary key (audit_id),
  key idx_cost_audit_scene_time (scene_id, operate_time),
  key idx_cost_audit_object (object_type, object_code),
  key idx_cost_audit_action (action_type, operate_time)
) engine=innodb default charset=utf8mb4 comment='核算平台-配置审计日志表';

set foreign_key_checks = 1;

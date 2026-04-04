-- 线程五补强：正式核算输入批次

create table if not exists cost_calc_input_batch (
  batch_id                   bigint          not null auto_increment comment '输入批次主键',
  batch_no                   varchar(64)     not null comment '输入批次号',
  scene_id                   bigint          not null comment '场景主键',
  version_id                 bigint          default null comment '版本主键',
  bill_month                 varchar(16)     not null default '' comment '账期',
  source_type                varchar(32)     not null default 'JSON_IMPORT' comment '来源类型',
  batch_status               varchar(32)     not null default 'READY' comment '批次状态，例如READY、SUBMITTED、CONSUMED',
  total_count                int             not null default 0 comment '总条数',
  valid_count                int             not null default 0 comment '有效条数',
  error_count                int             not null default 0 comment '错误条数',
  remark                     varchar(500)    default null comment '备注',
  error_message              varchar(1000)   default '' comment '错误摘要',
  create_by                  varchar(64)     default '' comment '创建人',
  create_time                datetime        default current_timestamp comment '创建时间',
  update_by                  varchar(64)     default '' comment '更新人',
  update_time                datetime        default current_timestamp on update current_timestamp comment '更新时间',
  primary key (batch_id),
  unique key uk_cost_calc_input_batch_no (batch_no),
  key idx_cost_calc_input_batch_scene_month (scene_id, bill_month),
  constraint fk_cost_calc_input_batch_scene foreign key (scene_id) references cost_scene (scene_id),
  constraint fk_cost_calc_input_batch_version foreign key (version_id) references cost_publish_version (version_id)
) engine=innodb default charset=utf8mb4 comment='核算平台-正式核算输入批次表';

create table if not exists cost_calc_input_batch_item (
  item_id                    bigint          not null auto_increment comment '输入批次明细主键',
  batch_id                   bigint          not null comment '输入批次主键',
  batch_no                   varchar(64)     not null comment '输入批次号冗余字段',
  item_no                    int             not null comment '批次内序号',
  biz_no                     varchar(128)    not null comment '业务单号',
  item_status                varchar(32)     not null default 'READY' comment '明细状态，例如READY、IMPORTED、ERROR',
  input_json                 json            not null comment '输入数据',
  error_message              varchar(1000)   default '' comment '错误摘要',
  create_time                datetime        default current_timestamp comment '创建时间',
  update_time                datetime        default current_timestamp on update current_timestamp comment '更新时间',
  primary key (item_id),
  unique key uk_cost_calc_input_batch_item (batch_id, biz_no),
  key idx_cost_calc_input_batch_item_batch (batch_id, item_no),
  constraint fk_cost_calc_input_batch_item_batch foreign key (batch_id) references cost_calc_input_batch (batch_id)
) engine=innodb default charset=utf8mb4 comment='核算平台-正式核算输入批次明细表';

set @current_schema = database();

set @ddl = (
  select if(
    exists(
      select 1
      from information_schema.columns
      where table_schema = @current_schema
        and table_name = 'cost_calc_task'
        and column_name = 'input_source_type'
    ),
    'select 1',
    'alter table cost_calc_task add column input_source_type varchar(32) not null default ''INLINE_JSON'' comment ''输入来源类型，例如INLINE_JSON、INPUT_BATCH'''
  )
);
prepare stmt from @ddl;
execute stmt;
deallocate prepare stmt;

set @ddl = (
  select if(
    exists(
      select 1
      from information_schema.columns
      where table_schema = @current_schema
        and table_name = 'cost_calc_task'
        and column_name = 'source_batch_no'
    ),
    'select 1',
    'alter table cost_calc_task add column source_batch_no varchar(64) default '''' comment ''来源批次号'''
  )
);
prepare stmt from @ddl;
execute stmt;
deallocate prepare stmt;

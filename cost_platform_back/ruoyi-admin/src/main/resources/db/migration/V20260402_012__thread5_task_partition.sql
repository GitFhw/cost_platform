-- 线程五补强：正式核算任务分片台账

create table if not exists cost_calc_task_partition (
  partition_id               bigint          not null auto_increment comment '任务分片主键',
  task_id                    bigint          not null comment '所属任务主键',
  task_no                    varchar(64)     not null comment '任务编号冗余字段',
  partition_no               int             not null comment '分片序号',
  start_item_no              int             not null default 1 comment '起始明细序号',
  end_item_no                int             not null default 1 comment '结束明细序号',
  partition_status           varchar(32)     not null default 'INIT' comment '分片状态，例如INIT、RUNNING、SUCCESS、PART_SUCCESS、FAILED、CANCELLED',
  total_count                int             not null default 0 comment '分片总条数',
  processed_count            int             not null default 0 comment '已处理条数',
  success_count              int             not null default 0 comment '成功条数',
  fail_count                 int             not null default 0 comment '失败条数',
  started_time               datetime        default null comment '开始时间',
  finished_time              datetime        default null comment '结束时间',
  duration_ms                bigint          default 0 comment '耗时毫秒',
  last_error                 varchar(1000)   default '' comment '最近错误摘要',
  create_time                datetime        default current_timestamp comment '创建时间',
  update_time                datetime        default current_timestamp on update current_timestamp comment '更新时间',
  primary key (partition_id),
  unique key uk_cost_calc_task_partition (task_id, partition_no),
  key idx_cost_calc_task_partition_task_status (task_id, partition_status),
  key idx_cost_calc_task_partition_task_no (task_no),
  constraint fk_cost_calc_task_partition_task foreign key (task_id) references cost_calc_task (task_id)
) engine=innodb default charset=utf8mb4 comment='核算平台-正式核算任务分片表';

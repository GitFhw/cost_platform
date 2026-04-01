-- 线程六：性能与治理增强

create table if not exists cost_bill_period (
  period_id bigint not null auto_increment comment '账期主键',
  scene_id bigint not null comment '所属场景主键',
  bill_month varchar(7) not null comment '账期，格式 yyyy-MM',
  period_status varchar(32) not null default 'NOT_STARTED' comment '账期状态',
  active_version_id bigint default null comment '当前账期默认版本',
  result_count bigint not null default 0 comment '当前账期结果条数',
  amount_total decimal(18, 2) not null default 0.00 comment '当前账期结果金额汇总',
  last_task_id bigint default null comment '最近一次正式任务主键',
  last_task_no varchar(64) default '' comment '最近一次正式任务编号',
  sealed_by varchar(64) default '' comment '封存操作人',
  sealed_time datetime default null comment '封存时间',
  create_by varchar(64) default '' comment '创建人',
  create_time datetime default current_timestamp comment '创建时间',
  update_by varchar(64) default '' comment '更新人',
  update_time datetime default current_timestamp on update current_timestamp comment '更新时间',
  remark varchar(500) default '' comment '备注',
  primary key (period_id),
  unique key uk_cost_bill_period_scene_month (scene_id, bill_month),
  key idx_cost_bill_period_status (period_status),
  key idx_cost_bill_period_version (active_version_id)
) engine=innodb default charset=utf8mb4 comment='核算平台-账期治理表';

create table if not exists cost_recalc_order (
  recalc_id bigint not null auto_increment comment '重算申请主键',
  scene_id bigint not null comment '所属场景主键',
  bill_month varchar(7) not null comment '目标账期',
  version_id bigint not null comment '目标发布版本主键',
  period_id bigint default null comment '账期主键',
  baseline_task_id bigint default null comment '基准任务主键',
  baseline_task_no varchar(64) default '' comment '基准任务编号',
  target_task_id bigint default null comment '重算任务主键',
  target_task_no varchar(64) default '' comment '重算任务编号',
  recalc_status varchar(32) not null default 'PENDING_APPROVAL' comment '重算状态',
  apply_reason varchar(500) default '' comment '申请原因',
  approve_opinion varchar(500) default '' comment '审核意见',
  diff_summary_json json default null comment '重算前后差异摘要',
  diff_amount decimal(18, 2) not null default 0.00 comment '重算差异金额',
  request_no varchar(64) default '' comment '幂等请求号',
  approve_by varchar(64) default '' comment '审核人',
  approve_time datetime default null comment '审核时间',
  execute_by varchar(64) default '' comment '执行人',
  execute_time datetime default null comment '执行时间',
  finish_time datetime default null comment '完成时间',
  create_by varchar(64) default '' comment '创建人',
  create_time datetime default current_timestamp comment '创建时间',
  update_by varchar(64) default '' comment '更新人',
  update_time datetime default current_timestamp on update current_timestamp comment '更新时间',
  remark varchar(500) default '' comment '备注',
  primary key (recalc_id),
  key idx_cost_recalc_scene_month (scene_id, bill_month),
  key idx_cost_recalc_status (recalc_status),
  key idx_cost_recalc_target_task (target_task_id)
) engine=innodb default charset=utf8mb4 comment='核算平台-重算申请与记录表';

create table if not exists cost_alarm_record (
  alarm_id bigint not null auto_increment comment '告警主键',
  scene_id bigint default null comment '所属场景主键',
  version_id bigint default null comment '关联版本主键',
  task_id bigint default null comment '关联任务主键',
  detail_id bigint default null comment '关联任务明细主键',
  bill_month varchar(7) default '' comment '关联账期',
  alarm_type varchar(32) not null comment '告警类型',
  alarm_level varchar(32) not null default 'WARN' comment '告警级别',
  alarm_status varchar(32) not null default 'OPEN' comment '告警状态',
  source_key varchar(128) default '' comment '来源唯一键',
  alarm_title varchar(200) default '' comment '告警标题',
  alarm_content varchar(1000) default '' comment '告警内容',
  trigger_time datetime default current_timestamp comment '触发时间',
  ack_by varchar(64) default '' comment '确认人',
  ack_time datetime default null comment '确认时间',
  resolve_by varchar(64) default '' comment '处理人',
  resolve_time datetime default null comment '处理时间',
  create_by varchar(64) default '' comment '创建人',
  create_time datetime default current_timestamp comment '创建时间',
  update_by varchar(64) default '' comment '更新人',
  update_time datetime default current_timestamp on update current_timestamp comment '更新时间',
  remark varchar(500) default '' comment '备注',
  primary key (alarm_id),
  key idx_cost_alarm_scene_time (scene_id, trigger_time),
  key idx_cost_alarm_task (task_id, detail_id),
  key idx_cost_alarm_status (alarm_status, alarm_level)
) engine=innodb default charset=utf8mb4 comment='核算平台-运行告警台账';

insert into sys_dict_type (dict_id, dict_name, dict_type, status, create_by, create_time, remark)
select 143, '核算-账期状态', 'cost_bill_period_status', '0', 'admin', sysdate(), '线程六：账期治理状态'
from dual
where not exists (select 1 from sys_dict_type where dict_type = 'cost_bill_period_status');

insert into sys_dict_type (dict_id, dict_name, dict_type, status, create_by, create_time, remark)
select 144, '核算-重算状态', 'cost_recalc_status', '0', 'admin', sysdate(), '线程六：重算申请与执行状态'
from dual
where not exists (select 1 from sys_dict_type where dict_type = 'cost_recalc_status');

insert into sys_dict_type (dict_id, dict_name, dict_type, status, create_by, create_time, remark)
select 145, '核算-告警级别', 'cost_alarm_level', '0', 'admin', sysdate(), '线程六：运行告警级别'
from dual
where not exists (select 1 from sys_dict_type where dict_type = 'cost_alarm_level');

insert into sys_dict_type (dict_id, dict_name, dict_type, status, create_by, create_time, remark)
select 146, '核算-告警状态', 'cost_alarm_status', '0', 'admin', sysdate(), '线程六：运行告警状态'
from dual
where not exists (select 1 from sys_dict_type where dict_type = 'cost_alarm_status');

insert into sys_dict_data (dict_code, dict_sort, dict_label, dict_value, dict_type, list_class, is_default, status, create_by, create_time, remark)
select 1430, 1, '未开始', 'NOT_STARTED', 'cost_bill_period_status', 'info', 'Y', '0', 'admin', sysdate(), '账期未开始'
from dual
where not exists (select 1 from sys_dict_data where dict_type = 'cost_bill_period_status' and dict_value = 'NOT_STARTED');

insert into sys_dict_data (dict_code, dict_sort, dict_label, dict_value, dict_type, list_class, is_default, status, create_by, create_time, remark)
select 1431, 2, '进行中', 'IN_PROGRESS', 'cost_bill_period_status', 'primary', 'N', '0', 'admin', sysdate(), '账期进行中'
from dual
where not exists (select 1 from sys_dict_data where dict_type = 'cost_bill_period_status' and dict_value = 'IN_PROGRESS');

insert into sys_dict_data (dict_code, dict_sort, dict_label, dict_value, dict_type, list_class, is_default, status, create_by, create_time, remark)
select 1432, 3, '已结算', 'CLOSED', 'cost_bill_period_status', 'success', 'N', '0', 'admin', sysdate(), '账期已结算'
from dual
where not exists (select 1 from sys_dict_data where dict_type = 'cost_bill_period_status' and dict_value = 'CLOSED');

insert into sys_dict_data (dict_code, dict_sort, dict_label, dict_value, dict_type, list_class, is_default, status, create_by, create_time, remark)
select 1433, 4, '已封存', 'SEALED', 'cost_bill_period_status', 'warning', 'N', '0', 'admin', sysdate(), '账期已封存'
from dual
where not exists (select 1 from sys_dict_data where dict_type = 'cost_bill_period_status' and dict_value = 'SEALED');

insert into sys_dict_data (dict_code, dict_sort, dict_label, dict_value, dict_type, list_class, is_default, status, create_by, create_time, remark)
select 1440, 1, '待审核', 'PENDING_APPROVAL', 'cost_recalc_status', 'info', 'Y', '0', 'admin', sysdate(), '重算待审核'
from dual
where not exists (select 1 from sys_dict_data where dict_type = 'cost_recalc_status' and dict_value = 'PENDING_APPROVAL');

insert into sys_dict_data (dict_code, dict_sort, dict_label, dict_value, dict_type, list_class, is_default, status, create_by, create_time, remark)
select 1441, 2, '已审核', 'APPROVED', 'cost_recalc_status', 'primary', 'N', '0', 'admin', sysdate(), '重算审核通过'
from dual
where not exists (select 1 from sys_dict_data where dict_type = 'cost_recalc_status' and dict_value = 'APPROVED');

insert into sys_dict_data (dict_code, dict_sort, dict_label, dict_value, dict_type, list_class, is_default, status, create_by, create_time, remark)
select 1442, 3, '已驳回', 'REJECTED', 'cost_recalc_status', 'danger', 'N', '0', 'admin', sysdate(), '重算审核驳回'
from dual
where not exists (select 1 from sys_dict_data where dict_type = 'cost_recalc_status' and dict_value = 'REJECTED');

insert into sys_dict_data (dict_code, dict_sort, dict_label, dict_value, dict_type, list_class, is_default, status, create_by, create_time, remark)
select 1443, 4, '执行中', 'RUNNING', 'cost_recalc_status', 'warning', 'N', '0', 'admin', sysdate(), '重算执行中'
from dual
where not exists (select 1 from sys_dict_data where dict_type = 'cost_recalc_status' and dict_value = 'RUNNING');

insert into sys_dict_data (dict_code, dict_sort, dict_label, dict_value, dict_type, list_class, is_default, status, create_by, create_time, remark)
select 1444, 5, '执行成功', 'SUCCESS', 'cost_recalc_status', 'success', 'N', '0', 'admin', sysdate(), '重算执行成功'
from dual
where not exists (select 1 from sys_dict_data where dict_type = 'cost_recalc_status' and dict_value = 'SUCCESS');

insert into sys_dict_data (dict_code, dict_sort, dict_label, dict_value, dict_type, list_class, is_default, status, create_by, create_time, remark)
select 1445, 6, '执行失败', 'FAILED', 'cost_recalc_status', 'danger', 'N', '0', 'admin', sysdate(), '重算执行失败'
from dual
where not exists (select 1 from sys_dict_data where dict_type = 'cost_recalc_status' and dict_value = 'FAILED');

insert into sys_dict_data (dict_code, dict_sort, dict_label, dict_value, dict_type, list_class, is_default, status, create_by, create_time, remark)
select 1450, 1, '提示', 'INFO', 'cost_alarm_level', 'info', 'Y', '0', 'admin', sysdate(), '提示级告警'
from dual
where not exists (select 1 from sys_dict_data where dict_type = 'cost_alarm_level' and dict_value = 'INFO');

insert into sys_dict_data (dict_code, dict_sort, dict_label, dict_value, dict_type, list_class, is_default, status, create_by, create_time, remark)
select 1451, 2, '告警', 'WARN', 'cost_alarm_level', 'warning', 'N', '0', 'admin', sysdate(), '告警级'
from dual
where not exists (select 1 from sys_dict_data where dict_type = 'cost_alarm_level' and dict_value = 'WARN');

insert into sys_dict_data (dict_code, dict_sort, dict_label, dict_value, dict_type, list_class, is_default, status, create_by, create_time, remark)
select 1452, 3, '严重', 'ERROR', 'cost_alarm_level', 'danger', 'N', '0', 'admin', sysdate(), '严重级告警'
from dual
where not exists (select 1 from sys_dict_data where dict_type = 'cost_alarm_level' and dict_value = 'ERROR');

insert into sys_dict_data (dict_code, dict_sort, dict_label, dict_value, dict_type, list_class, is_default, status, create_by, create_time, remark)
select 1460, 1, '未处理', 'OPEN', 'cost_alarm_status', 'danger', 'Y', '0', 'admin', sysdate(), '未处理告警'
from dual
where not exists (select 1 from sys_dict_data where dict_type = 'cost_alarm_status' and dict_value = 'OPEN');

insert into sys_dict_data (dict_code, dict_sort, dict_label, dict_value, dict_type, list_class, is_default, status, create_by, create_time, remark)
select 1461, 2, '已确认', 'ACKED', 'cost_alarm_status', 'warning', 'N', '0', 'admin', sysdate(), '已确认告警'
from dual
where not exists (select 1 from sys_dict_data where dict_type = 'cost_alarm_status' and dict_value = 'ACKED');

insert into sys_dict_data (dict_code, dict_sort, dict_label, dict_value, dict_type, list_class, is_default, status, create_by, create_time, remark)
select 1462, 3, '已关闭', 'RESOLVED', 'cost_alarm_status', 'success', 'N', '0', 'admin', sysdate(), '已关闭告警'
from dual
where not exists (select 1 from sys_dict_data where dict_type = 'cost_alarm_status' and dict_value = 'RESOLVED');

insert into sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
select 2010, '账期治理', 2000, 10, 'period', 'cost/period/index', '', 'CostPeriod', 1, 0, 'C', '0', '0', 'cost:period:list', 'date', 'admin', sysdate(), '线程六：账期与重算治理'
from dual
where not exists (select 1 from sys_menu where menu_id = 2010);

insert into sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
select 2011, '审计台账', 2000, 11, 'audit', 'cost/audit/index', '', 'CostAudit', 1, 0, 'C', '0', '0', 'cost:audit:list', 'example', 'admin', sysdate(), '线程六：审计与治理台账'
from dual
where not exists (select 1 from sys_menu where menu_id = 2011);

insert into sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
select 2012, '告警中心', 2000, 12, 'alert', 'cost/alert/index', '', 'CostAlert', 1, 0, 'C', '0', '0', 'cost:alarm:list', 'message', 'admin', sysdate(), '线程六：运行告警与缓存治理'
from dual
where not exists (select 1 from sys_menu where menu_id = 2012);

insert into sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
select 2250, '账期查询', 2010, 1, '#', '', '', '', 1, 0, 'F', '0', '0', 'cost:period:query', '#', 'admin', sysdate(), ''
from dual
where not exists (select 1 from sys_menu where menu_id = 2250);

insert into sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
select 2251, '新建账期', 2010, 2, '#', '', '', '', 1, 0, 'F', '0', '0', 'cost:period:add', '#', 'admin', sysdate(), ''
from dual
where not exists (select 1 from sys_menu where menu_id = 2251);

insert into sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
select 2252, '封存账期', 2010, 3, '#', '', '', '', 1, 0, 'F', '0', '0', 'cost:period:seal', '#', 'admin', sysdate(), ''
from dual
where not exists (select 1 from sys_menu where menu_id = 2252);

insert into sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
select 2253, '发起重算', 2010, 4, '#', '', '', '', 1, 0, 'F', '0', '0', 'cost:period:recalc', '#', 'admin', sysdate(), ''
from dual
where not exists (select 1 from sys_menu where menu_id = 2253);

insert into sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
select 2254, '审核重算', 2010, 5, '#', '', '', '', 1, 0, 'F', '0', '0', 'cost:period:approve', '#', 'admin', sysdate(), ''
from dual
where not exists (select 1 from sys_menu where menu_id = 2254);

insert into sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
select 2255, '执行重算', 2010, 6, '#', '', '', '', 1, 0, 'F', '0', '0', 'cost:period:execute', '#', 'admin', sysdate(), ''
from dual
where not exists (select 1 from sys_menu where menu_id = 2255);

insert into sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
select 2256, '审计查询', 2011, 1, '#', '', '', '', 1, 0, 'F', '0', '0', 'cost:audit:query', '#', 'admin', sysdate(), ''
from dual
where not exists (select 1 from sys_menu where menu_id = 2256);

insert into sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
select 2257, '告警查询', 2012, 1, '#', '', '', '', 1, 0, 'F', '0', '0', 'cost:alarm:query', '#', 'admin', sysdate(), ''
from dual
where not exists (select 1 from sys_menu where menu_id = 2257);

insert into sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
select 2258, '确认告警', 2012, 2, '#', '', '', '', 1, 0, 'F', '0', '0', 'cost:alarm:ack', '#', 'admin', sysdate(), ''
from dual
where not exists (select 1 from sys_menu where menu_id = 2258);

insert into sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
select 2259, '关闭告警', 2012, 3, '#', '', '', '', 1, 0, 'F', '0', '0', 'cost:alarm:resolve', '#', 'admin', sysdate(), ''
from dual
where not exists (select 1 from sys_menu where menu_id = 2259);

insert into sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
select 2260, '刷新缓存', 2012, 4, '#', '', '', '', 1, 0, 'F', '0', '0', 'cost:cache:refresh', '#', 'admin', sysdate(), ''
from dual
where not exists (select 1 from sys_menu where menu_id = 2260);

insert into sys_role_menu (role_id, menu_id)
select 1, 2010 from dual where not exists (select 1 from sys_role_menu where role_id = 1 and menu_id = 2010);
insert into sys_role_menu (role_id, menu_id)
select 1, 2011 from dual where not exists (select 1 from sys_role_menu where role_id = 1 and menu_id = 2011);
insert into sys_role_menu (role_id, menu_id)
select 1, 2012 from dual where not exists (select 1 from sys_role_menu where role_id = 1 and menu_id = 2012);
insert into sys_role_menu (role_id, menu_id)
select 1, 2250 from dual where not exists (select 1 from sys_role_menu where role_id = 1 and menu_id = 2250);
insert into sys_role_menu (role_id, menu_id)
select 1, 2251 from dual where not exists (select 1 from sys_role_menu where role_id = 1 and menu_id = 2251);
insert into sys_role_menu (role_id, menu_id)
select 1, 2252 from dual where not exists (select 1 from sys_role_menu where role_id = 1 and menu_id = 2252);
insert into sys_role_menu (role_id, menu_id)
select 1, 2253 from dual where not exists (select 1 from sys_role_menu where role_id = 1 and menu_id = 2253);
insert into sys_role_menu (role_id, menu_id)
select 1, 2254 from dual where not exists (select 1 from sys_role_menu where role_id = 1 and menu_id = 2254);
insert into sys_role_menu (role_id, menu_id)
select 1, 2255 from dual where not exists (select 1 from sys_role_menu where role_id = 1 and menu_id = 2255);
insert into sys_role_menu (role_id, menu_id)
select 1, 2256 from dual where not exists (select 1 from sys_role_menu where role_id = 1 and menu_id = 2256);
insert into sys_role_menu (role_id, menu_id)
select 1, 2257 from dual where not exists (select 1 from sys_role_menu where role_id = 1 and menu_id = 2257);
insert into sys_role_menu (role_id, menu_id)
select 1, 2258 from dual where not exists (select 1 from sys_role_menu where role_id = 1 and menu_id = 2258);
insert into sys_role_menu (role_id, menu_id)
select 1, 2259 from dual where not exists (select 1 from sys_role_menu where role_id = 1 and menu_id = 2259);
insert into sys_role_menu (role_id, menu_id)
select 1, 2260 from dual where not exists (select 1 from sys_role_menu where role_id = 1 and menu_id = 2260);

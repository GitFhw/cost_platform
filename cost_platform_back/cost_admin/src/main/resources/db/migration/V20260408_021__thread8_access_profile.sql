create table if not exists cost_access_profile (
    profile_id bigint primary key auto_increment comment '接入方案主键',
    scene_id bigint not null comment '所属场景主键',
    fee_id bigint null comment '目标费用主键',
    version_id bigint null comment '绑定版本主键',
    profile_code varchar(64) not null comment '方案编码',
    profile_name varchar(128) not null comment '方案名称',
    source_type varchar(32) not null default 'RAW_JSON' comment '来源类型',
    task_type varchar(32) not null default 'FORMAL_BATCH' comment '任务类型',
    request_method varchar(16) not null default 'GET' comment '请求方法',
    endpoint_url varchar(255) null comment '外部接口地址',
    auth_type varchar(32) not null default 'NONE' comment '鉴权方式',
    auth_config_json text null comment '鉴权配置JSON',
    mapping_json longtext null comment '字段映射JSON',
    sample_payload_json longtext null comment '样例原始载荷JSON',
    sample_input_json longtext null comment '样例标准计费对象JSON',
    status char(1) not null default '0' comment '状态（0正常 1停用）',
    sort_no int null default 0 comment '排序号',
    create_by varchar(64) null comment '创建人',
    create_time datetime null comment '创建时间',
    update_by varchar(64) null comment '更新人',
    update_time datetime null comment '更新时间',
    remark varchar(500) null comment '备注',
    unique key uk_cost_access_profile_scene_code (scene_id, profile_code),
    key idx_cost_access_profile_fee (fee_id),
    key idx_cost_access_profile_status (status)
) engine=innodb comment='数据接入方案';

update sys_menu
set perms = 'cost:access:list',
    remark = '线程八：数据接入与导入中心入口'
where menu_id = 2015
  and (perms is null or perms = '' or perms = 'cost:task:list');

insert into sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
select 20151, '接入方案查询', 2015, 1, '', '', '', '', 1, 0, 'F', '0', '0', 'cost:access:query', '#', 'admin', sysdate(), '线程八：接入方案查询权限'
from dual
where not exists (select 1 from sys_menu where menu_id = 20151);

insert into sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
select 20152, '接入方案新增', 2015, 2, '', '', '', '', 1, 0, 'F', '0', '0', 'cost:access:add', '#', 'admin', sysdate(), '线程八：接入方案新增权限'
from dual
where not exists (select 1 from sys_menu where menu_id = 20152);

insert into sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
select 20153, '接入方案修改', 2015, 3, '', '', '', '', 1, 0, 'F', '0', '0', 'cost:access:edit', '#', 'admin', sysdate(), '线程八：接入方案修改权限'
from dual
where not exists (select 1 from sys_menu where menu_id = 20153);

insert into sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
select 20154, '接入方案删除', 2015, 4, '', '', '', '', 1, 0, 'F', '0', '0', 'cost:access:remove', '#', 'admin', sysdate(), '线程八：接入方案删除权限'
from dual
where not exists (select 1 from sys_menu where menu_id = 20154);

insert into sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
select 20155, '接入方案列表', 2015, 5, '', '', '', '', 1, 0, 'F', '0', '0', 'cost:access:list', '#', 'admin', sysdate(), '线程八：接入方案列表权限'
from dual
where not exists (select 1 from sys_menu where menu_id = 20155);

insert into sys_role_menu (role_id, menu_id)
select 1, 20151 from dual
where not exists (select 1 from sys_role_menu where role_id = 1 and menu_id = 20151);

insert into sys_role_menu (role_id, menu_id)
select 1, 20152 from dual
where not exists (select 1 from sys_role_menu where role_id = 1 and menu_id = 20152);

insert into sys_role_menu (role_id, menu_id)
select 1, 20153 from dual
where not exists (select 1 from sys_role_menu where role_id = 1 and menu_id = 20153);

insert into sys_role_menu (role_id, menu_id)
select 1, 20154 from dual
where not exists (select 1 from sys_role_menu where role_id = 1 and menu_id = 20154);

insert into sys_role_menu (role_id, menu_id)
select 1, 20155 from dual
where not exists (select 1 from sys_role_menu where role_id = 1 and menu_id = 20155);

-- 线程一：基础治理
-- 内容：业务域字典、系统字典规划、场景中心

-- ----------------------------
-- 1、场景主数据表
-- ----------------------------
create table if not exists cost_scene (
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

-- 兼容已有旧表：如果 cost_scene 已存在，则补齐线程一新增字段与索引
set @current_schema = database();

set @ddl = (
  select if(
    exists(
      select 1
      from information_schema.columns
      where table_schema = @current_schema
        and table_name = 'cost_scene'
        and column_name = 'org_code'
    ),
    'select 1',
    'alter table cost_scene add column org_code varchar(64) default '''' comment ''所属组织编码，用于按组织隔离场景'' after business_domain'
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
        and table_name = 'cost_scene'
        and column_name = 'scene_type'
    ),
    'select 1',
    'alter table cost_scene add column scene_type varchar(32) default ''CONTRACT'' comment ''场景类型，例如合同、方案、公司级核算域'' after org_code'
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
        and table_name = 'cost_scene'
        and column_name = 'active_version_id'
    ),
    'select 1',
    'alter table cost_scene add column active_version_id bigint default null comment ''当前生效版本主键，对应 cost_publish_version.version_id'' after scene_type'
  )
);
prepare stmt from @ddl;
execute stmt;
deallocate prepare stmt;

set @ddl = (
  select if(
    exists(
      select 1
      from information_schema.statistics
      where table_schema = @current_schema
        and table_name = 'cost_scene'
        and index_name = 'uk_cost_scene_code'
    ),
    'select 1',
    'alter table cost_scene add unique key uk_cost_scene_code (scene_code)'
  )
);
prepare stmt from @ddl;
execute stmt;
deallocate prepare stmt;

set @ddl = (
  select if(
    exists(
      select 1
      from information_schema.statistics
      where table_schema = @current_schema
        and table_name = 'cost_scene'
        and index_name = 'idx_cost_scene_domain_status'
    ),
    'select 1',
    'alter table cost_scene add key idx_cost_scene_domain_status (business_domain, status)'
  )
);
prepare stmt from @ddl;
execute stmt;
deallocate prepare stmt;

set @ddl = (
  select if(
    exists(
      select 1
      from information_schema.statistics
      where table_schema = @current_schema
        and table_name = 'cost_scene'
        and index_name = 'idx_cost_scene_org_status'
    ),
    'select 1',
    'alter table cost_scene add key idx_cost_scene_org_status (org_code, status)'
  )
);
prepare stmt from @ddl;
execute stmt;
deallocate prepare stmt;

-- ----------------------------
-- 2、核算字典规划
-- ----------------------------
insert into sys_dict_type (dict_id, dict_name, dict_type, status, create_by, create_time, remark)
select 100, '核算-业务域', 'cost_business_domain', '0', 'admin', sysdate(), '线程一治理：统一承接跨行业业务域边界'
from dual
where not exists (select 1 from sys_dict_type where dict_type = 'cost_business_domain');

insert into sys_dict_type (dict_id, dict_name, dict_type, status, create_by, create_time, remark)
select 101, '核算-场景状态', 'cost_scene_status', '0', 'admin', sysdate(), '线程一治理：场景中心状态字典'
from dual
where not exists (select 1 from sys_dict_type where dict_type = 'cost_scene_status');

insert into sys_dict_type (dict_id, dict_name, dict_type, status, create_by, create_time, remark)
select 102, '核算-场景类型', 'cost_scene_type', '0', 'admin', sysdate(), '线程一治理：场景主数据类型字典'
from dual
where not exists (select 1 from sys_dict_type where dict_type = 'cost_scene_type');

insert into sys_dict_type (dict_id, dict_name, dict_type, status, create_by, create_time, remark)
select 103, '核算-货种', 'cost_cargo_type', '0', 'admin', sysdate(), '系统字典规划：用于港口、仓储等行业货类条件'
from dual
where not exists (select 1 from sys_dict_type where dict_type = 'cost_cargo_type');

insert into sys_dict_type (dict_id, dict_name, dict_type, status, create_by, create_time, remark)
select 104, '核算-内外贸', 'cost_trade_type', '0', 'admin', sysdate(), '系统字典规划：用于港口计费的内外贸属性'
from dual
where not exists (select 1 from sys_dict_type where dict_type = 'cost_trade_type');

insert into sys_dict_type (dict_id, dict_name, dict_type, status, create_by, create_time, remark)
select 105, '核算-班次', 'cost_shift_type', '0', 'admin', sysdate(), '系统字典规划：用于薪资、作业等班次因素'
from dual
where not exists (select 1 from sys_dict_type where dict_type = 'cost_shift_type');

insert into sys_dict_type (dict_id, dict_name, dict_type, status, create_by, create_time, remark)
select 106, '核算-工种', 'cost_job_type', '0', 'admin', sysdate(), '系统字典规划：用于薪资与作业费率条件'
from dual
where not exists (select 1 from sys_dict_type where dict_type = 'cost_job_type');

insert into sys_dict_type (dict_id, dict_name, dict_type, status, create_by, create_time, remark)
select 107, '核算-岗位', 'cost_post_type', '0', 'admin', sysdate(), '系统字典规划：用于岗位维度核算配置'
from dual
where not exists (select 1 from sys_dict_type where dict_type = 'cost_post_type');

insert into sys_dict_type (dict_id, dict_name, dict_type, status, create_by, create_time, remark)
select 108, '核算-客户等级', 'cost_customer_level', '0', 'admin', sysdate(), '系统字典规划：用于合同、客户差异化费率'
from dual
where not exists (select 1 from sys_dict_type where dict_type = 'cost_customer_level');

insert into sys_dict_type (dict_id, dict_name, dict_type, status, create_by, create_time, remark)
select 109, '核算-币种', 'cost_currency', '0', 'admin', sysdate(), '系统字典规划：用于多币种金额表达'
from dual
where not exists (select 1 from sys_dict_type where dict_type = 'cost_currency');

insert into sys_dict_type (dict_id, dict_name, dict_type, status, create_by, create_time, remark)
select 110, '核算-协力队', 'cost_partner_team', '0', 'admin', sysdate(), '系统字典规划：用于协力队维度核算'
from dual
where not exists (select 1 from sys_dict_type where dict_type = 'cost_partner_team');

insert into sys_dict_type (dict_id, dict_name, dict_type, status, create_by, create_time, remark)
select 111, '核算-是否矿三', 'cost_mine_three_flag', '0', 'admin', sysdate(), '系统字典规划：用于矿山业务布尔口径'
from dual
where not exists (select 1 from sys_dict_type where dict_type = 'cost_mine_three_flag');

insert into sys_dict_data (dict_code, dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
select 1000, 1, '薪资结算', 'SALARY', 'cost_business_domain', '', 'success', 'N', '0', 'admin', sysdate(), '业务域：薪资行业'
from dual
where not exists (select 1 from sys_dict_data where dict_type = 'cost_business_domain' and dict_value = 'SALARY');

insert into sys_dict_data (dict_code, dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
select 1001, 2, '港口作业', 'PORT', 'cost_business_domain', '', 'primary', 'Y', '0', 'admin', sysdate(), '业务域：港口行业'
from dual
where not exists (select 1 from sys_dict_data where dict_type = 'cost_business_domain' and dict_value = 'PORT');

insert into sys_dict_data (dict_code, dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
select 1002, 3, '仓储结算', 'STORAGE', 'cost_business_domain', '', 'warning', 'N', '0', 'admin', sysdate(), '业务域：仓储行业'
from dual
where not exists (select 1 from sys_dict_data where dict_type = 'cost_business_domain' and dict_value = 'STORAGE');

insert into sys_dict_data (dict_code, dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
select 1003, 4, '运输计费', 'TRANSPORT', 'cost_business_domain', '', 'info', 'N', '0', 'admin', sysdate(), '业务域：运输行业'
from dual
where not exists (select 1 from sys_dict_data where dict_type = 'cost_business_domain' and dict_value = 'TRANSPORT');

insert into sys_dict_data (dict_code, dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
select 1004, 5, '材料成本', 'MATERIAL', 'cost_business_domain', '', 'danger', 'N', '0', 'admin', sysdate(), '业务域：材料行业'
from dual
where not exists (select 1 from sys_dict_data where dict_type = 'cost_business_domain' and dict_value = 'MATERIAL');

insert into sys_dict_data (dict_code, dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
select 1005, 6, '制造成本', 'MANUFACTURE', 'cost_business_domain', '', 'default', 'N', '0', 'admin', sysdate(), '业务域：制造行业'
from dual
where not exists (select 1 from sys_dict_data where dict_type = 'cost_business_domain' and dict_value = 'MANUFACTURE');

insert into sys_dict_data (dict_code, dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
select 1010, 1, '正常', '0', 'cost_scene_status', '', 'success', 'Y', '0', 'admin', sysdate(), '场景可维护且可被下游使用'
from dual
where not exists (select 1 from sys_dict_data where dict_type = 'cost_scene_status' and dict_value = '0');

insert into sys_dict_data (dict_code, dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
select 1011, 2, '停用', '1', 'cost_scene_status', '', 'danger', 'N', '0', 'admin', sysdate(), '场景已停用，不再继续维护'
from dual
where not exists (select 1 from sys_dict_data where dict_type = 'cost_scene_status' and dict_value = '1');

insert into sys_dict_data (dict_code, dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
select 1012, 3, '草稿', '2', 'cost_scene_status', '', 'warning', 'N', '0', 'admin', sysdate(), '场景仍在整理中，尚未收稳'
from dual
where not exists (select 1 from sys_dict_data where dict_type = 'cost_scene_status' and dict_value = '2');

insert into sys_dict_data (dict_code, dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
select 1020, 1, '合同场景', 'CONTRACT', 'cost_scene_type', '', 'primary', 'Y', '0', 'admin', sysdate(), '以合同为边界维护核算配置'
from dual
where not exists (select 1 from sys_dict_data where dict_type = 'cost_scene_type' and dict_value = 'CONTRACT');

insert into sys_dict_data (dict_code, dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
select 1021, 2, '核算主题', 'THEME', 'cost_scene_type', '', 'success', 'N', '0', 'admin', sysdate(), '以统一核算主题组织配置'
from dual
where not exists (select 1 from sys_dict_data where dict_type = 'cost_scene_type' and dict_value = 'THEME');

insert into sys_dict_data (dict_code, dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
select 1022, 3, '业务方案', 'PLAN', 'cost_scene_type', '', 'warning', 'N', '0', 'admin', sysdate(), '以具体业务方案组织配置'
from dual
where not exists (select 1 from sys_dict_data where dict_type = 'cost_scene_type' and dict_value = 'PLAN');

insert into sys_dict_data (dict_code, dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
select 1023, 4, '公司级核算域', 'COMPANY', 'cost_scene_type', '', 'info', 'N', '0', 'admin', sysdate(), '以公司级统一口径组织配置'
from dual
where not exists (select 1 from sys_dict_data where dict_type = 'cost_scene_type' and dict_value = 'COMPANY');

-- ----------------------------
-- 3、线程一菜单入口
-- ----------------------------
insert into sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
select 2000, '核算治理', 0, 1, 'cost', null, '', '', 1, 0, 'M', '0', '0', '', 'build', 'admin', sysdate(), '核算平台基础治理目录'
from dual
where not exists (select 1 from sys_menu where menu_id = 2000);

insert into sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
select 2001, '核算字典', 2000, 1, 'dict', 'system/dict/index', '{"dictType":"cost_","scope":"cost"}', 'CostDict', 1, 0, 'C', '0', '0', 'system:dict:list', 'dict', 'admin', sysdate(), '核算字典治理入口'
from dual
where not exists (select 1 from sys_menu where menu_id = 2001);

insert into sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
select 2002, '场景中心', 2000, 2, 'scene', 'cost/scene/index', '', 'CostScene', 1, 0, 'C', '0', '0', 'cost:scene:list', 'tree-table', 'admin', sysdate(), '线程一场景主数据治理入口'
from dual
where not exists (select 1 from sys_menu where menu_id = 2002);

insert into sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
select 2100, '场景查询', 2002, 1, '#', '', '', '', 1, 0, 'F', '0', '0', 'cost:scene:query', '#', 'admin', sysdate(), ''
from dual
where not exists (select 1 from sys_menu where menu_id = 2100);

insert into sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
select 2101, '场景新增', 2002, 2, '#', '', '', '', 1, 0, 'F', '0', '0', 'cost:scene:add', '#', 'admin', sysdate(), ''
from dual
where not exists (select 1 from sys_menu where menu_id = 2101);

insert into sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
select 2102, '场景修改', 2002, 3, '#', '', '', '', 1, 0, 'F', '0', '0', 'cost:scene:edit', '#', 'admin', sysdate(), ''
from dual
where not exists (select 1 from sys_menu where menu_id = 2102);

insert into sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
select 2103, '场景删除', 2002, 4, '#', '', '', '', 1, 0, 'F', '0', '0', 'cost:scene:remove', '#', 'admin', sysdate(), ''
from dual
where not exists (select 1 from sys_menu where menu_id = 2103);

insert into sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
select 2104, '场景导出', 2002, 5, '#', '', '', '', 1, 0, 'F', '0', '0', 'cost:scene:export', '#', 'admin', sysdate(), ''
from dual
where not exists (select 1 from sys_menu where menu_id = 2104);

update sys_menu
set order_num = 1,
    update_by = 'admin',
    update_time = sysdate()
where menu_id = 2000
  and parent_id = 0;

update sys_menu
set order_num = 100,
    update_by = 'admin',
    update_time = sysdate()
where menu_id = 1
  and parent_id = 0;

update sys_menu
set order_num = 101,
    update_by = 'admin',
    update_time = sysdate()
where menu_id = 2
  and parent_id = 0;

update sys_menu
set order_num = 102,
    update_by = 'admin',
    update_time = sysdate()
where menu_id = 3
  and parent_id = 0;

update sys_menu
set order_num = 199,
    visible = '1',
    status = '1',
    update_by = 'admin',
    update_time = sysdate(),
    remark = '默认隐藏若依官网入口，保留数据以便后续恢复'
where menu_id = 4
  and parent_id = 0;

-- 线程七：公式实验室与表达式资产化

create table if not exists cost_formula
(
    formula_id             bigint          not null auto_increment comment '公式主键',
    scene_id               bigint          not null comment '所属场景主键',
    formula_code           varchar(64)     not null comment '公式编码，供变量/规则/运行链引用',
    formula_name           varchar(128)    not null comment '公式名称',
    formula_desc           varchar(500)    default '' comment '公式用途说明',
    business_formula       varchar(1000)   default '' comment '业务中文公式/口径说明',
    formula_expr           varchar(2000)   not null comment '标准执行表达式',
    namespace_scope        varchar(128)    default 'V,C,I,F,T' comment '允许引用的命名空间范围',
    return_type            varchar(32)     default 'NUMBER' comment '公式返回类型',
    test_case_json         json            default null comment '公式测试样例上下文',
    sample_result_json     json            default null comment '最近一次测试结果样例',
    last_test_time         datetime        default null comment '最近测试时间',
    status                 char(1)         default '0' comment '状态（0正常 1停用）',
    sort_no                int             default 10 comment '排序号',
    remark                 varchar(500)    default '' comment '备注',
    create_by              varchar(64)     default '' comment '创建人',
    create_time            datetime        default current_timestamp comment '创建时间',
    update_by              varchar(64)     default '' comment '更新人',
    update_time            datetime        default current_timestamp on update current_timestamp comment '更新时间',
    primary key (formula_id),
    unique key uk_cost_formula_scene_code (scene_id, formula_code),
    key idx_cost_formula_scene_status (scene_id, status)
) engine = innodb comment = '公式实验室主表';

drop procedure if exists proc_add_cost_variable_formula_code;
delimiter $$
create procedure proc_add_cost_variable_formula_code()
begin
    if not exists (
        select 1
        from information_schema.columns
        where table_schema = database()
          and table_name = 'cost_variable'
          and column_name = 'formula_code'
    ) then
        alter table cost_variable
            add column formula_code varchar(64) default '' comment '引用的公式编码' after formula_expr;
    end if;
end $$
delimiter ;
call proc_add_cost_variable_formula_code();
drop procedure if exists proc_add_cost_variable_formula_code;

drop procedure if exists proc_add_cost_rule_amount_formula_code;
delimiter $$
create procedure proc_add_cost_rule_amount_formula_code()
begin
    if not exists (
        select 1
        from information_schema.columns
        where table_schema = database()
          and table_name = 'cost_rule'
          and column_name = 'amount_formula_code'
    ) then
        alter table cost_rule
            add column amount_formula_code varchar(64) default '' comment '金额公式编码' after amount_formula;
    end if;
end $$
delimiter ;
call proc_add_cost_rule_amount_formula_code();
drop procedure if exists proc_add_cost_rule_amount_formula_code;

drop procedure if exists proc_add_cost_formula_index_variable;
delimiter $$
create procedure proc_add_cost_formula_index_variable()
begin
    if not exists (
        select 1
        from information_schema.statistics
        where table_schema = database()
          and table_name = 'cost_variable'
          and index_name = 'idx_cost_variable_scene_formula'
    ) then
        create index idx_cost_variable_scene_formula on cost_variable(scene_id, formula_code);
    end if;
end $$
delimiter ;
call proc_add_cost_formula_index_variable();
drop procedure if exists proc_add_cost_formula_index_variable;

drop procedure if exists proc_add_cost_formula_index_rule;
delimiter $$
create procedure proc_add_cost_formula_index_rule()
begin
    if not exists (
        select 1
        from information_schema.statistics
        where table_schema = database()
          and table_name = 'cost_rule'
          and index_name = 'idx_cost_rule_scene_formula'
    ) then
        create index idx_cost_rule_scene_formula on cost_rule(scene_id, amount_formula_code);
    end if;
end $$
delimiter ;
call proc_add_cost_formula_index_rule();
drop procedure if exists proc_add_cost_formula_index_rule;

insert into sys_dict_type (dict_id, dict_name, dict_type, status, create_by, create_time, remark)
select 147, '核算-公式状态', 'cost_formula_status', '0', 'admin', sysdate(), '线程七：公式实验室状态'
from dual
where not exists (select 1 from sys_dict_type where dict_type = 'cost_formula_status');

insert into sys_dict_type (dict_id, dict_name, dict_type, status, create_by, create_time, remark)
select 148, '核算-公式返回类型', 'cost_formula_return_type', '0', 'admin', sysdate(), '线程七：公式实验室返回类型'
from dual
where not exists (select 1 from sys_dict_type where dict_type = 'cost_formula_return_type');

insert into sys_dict_data (dict_code, dict_sort, dict_label, dict_value, dict_type, list_class, is_default, status, create_by, create_time, remark)
select 1470, 1, '正常', '0', 'cost_formula_status', 'success', 'Y', '0', 'admin', sysdate(), '公式可用'
from dual
where not exists (select 1 from sys_dict_data where dict_type = 'cost_formula_status' and dict_value = '0');

insert into sys_dict_data (dict_code, dict_sort, dict_label, dict_value, dict_type, list_class, is_default, status, create_by, create_time, remark)
select 1471, 2, '停用', '1', 'cost_formula_status', 'info', 'N', '0', 'admin', sysdate(), '公式停用'
from dual
where not exists (select 1 from sys_dict_data where dict_type = 'cost_formula_status' and dict_value = '1');

insert into sys_dict_data (dict_code, dict_sort, dict_label, dict_value, dict_type, list_class, is_default, status, create_by, create_time, remark)
select 1480, 1, '数值', 'NUMBER', 'cost_formula_return_type', 'primary', 'Y', '0', 'admin', sysdate(), '公式返回数值'
from dual
where not exists (select 1 from sys_dict_data where dict_type = 'cost_formula_return_type' and dict_value = 'NUMBER');

insert into sys_dict_data (dict_code, dict_sort, dict_label, dict_value, dict_type, list_class, is_default, status, create_by, create_time, remark)
select 1481, 2, '布尔', 'BOOLEAN', 'cost_formula_return_type', 'warning', 'N', '0', 'admin', sysdate(), '公式返回布尔值'
from dual
where not exists (select 1 from sys_dict_data where dict_type = 'cost_formula_return_type' and dict_value = 'BOOLEAN');

insert into sys_dict_data (dict_code, dict_sort, dict_label, dict_value, dict_type, list_class, is_default, status, create_by, create_time, remark)
select 1482, 3, '文本', 'STRING', 'cost_formula_return_type', 'info', 'N', '0', 'admin', sysdate(), '公式返回文本'
from dual
where not exists (select 1 from sys_dict_data where dict_type = 'cost_formula_return_type' and dict_value = 'STRING');

insert into sys_dict_data (dict_code, dict_sort, dict_label, dict_value, dict_type, list_class, is_default, status, create_by, create_time, remark)
select 1483, 4, 'JSON', 'JSON', 'cost_formula_return_type', 'danger', 'N', '0', 'admin', sysdate(), '公式返回结构化对象'
from dual
where not exists (select 1 from sys_dict_data where dict_type = 'cost_formula_return_type' and dict_value = 'JSON');

insert into sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
select 2013, '公式实验室', 2000, 6, 'formula', 'cost/formula/index', '', 'CostFormula', 1, 0, 'C', '0', '0', 'cost:formula:list', 'edit', 'admin', sysdate(), '线程七：公式实验室'
from dual
where not exists (select 1 from sys_menu where menu_id = 2013);

insert into sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
select 2260, '公式查询', 2013, 1, '#', '', '', '', 1, 0, 'F', '0', '0', 'cost:formula:query', '#', 'admin', sysdate(), ''
from dual
where not exists (select 1 from sys_menu where menu_id = 2260);

insert into sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
select 2261, '新增公式', 2013, 2, '#', '', '', '', 1, 0, 'F', '0', '0', 'cost:formula:add', '#', 'admin', sysdate(), ''
from dual
where not exists (select 1 from sys_menu where menu_id = 2261);

insert into sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
select 2262, '修改公式', 2013, 3, '#', '', '', '', 1, 0, 'F', '0', '0', 'cost:formula:edit', '#', 'admin', sysdate(), ''
from dual
where not exists (select 1 from sys_menu where menu_id = 2262);

insert into sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
select 2263, '删除公式', 2013, 4, '#', '', '', '', 1, 0, 'F', '0', '0', 'cost:formula:remove', '#', 'admin', sysdate(), ''
from dual
where not exists (select 1 from sys_menu where menu_id = 2263);

insert into sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
select 2264, '导出公式', 2013, 5, '#', '', '', '', 1, 0, 'F', '0', '0', 'cost:formula:export', '#', 'admin', sysdate(), ''
from dual
where not exists (select 1 from sys_menu where menu_id = 2264);

insert into sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
select 2265, '测试公式', 2013, 6, '#', '', '', '', 1, 0, 'F', '0', '0', 'cost:formula:test', '#', 'admin', sysdate(), ''
from dual
where not exists (select 1 from sys_menu where menu_id = 2265);

insert into sys_role_menu (role_id, menu_id)
select 1, 2013
from dual
where not exists (select 1 from sys_role_menu where role_id = 1 and menu_id = 2013);

insert into sys_role_menu (role_id, menu_id)
select 1, 2260
from dual
where not exists (select 1 from sys_role_menu where role_id = 1 and menu_id = 2260);

insert into sys_role_menu (role_id, menu_id)
select 1, 2261
from dual
where not exists (select 1 from sys_role_menu where role_id = 1 and menu_id = 2261);

insert into sys_role_menu (role_id, menu_id)
select 1, 2262
from dual
where not exists (select 1 from sys_role_menu where role_id = 1 and menu_id = 2262);

insert into sys_role_menu (role_id, menu_id)
select 1, 2263
from dual
where not exists (select 1 from sys_role_menu where role_id = 1 and menu_id = 2263);

insert into sys_role_menu (role_id, menu_id)
select 1, 2264
from dual
where not exists (select 1 from sys_role_menu where role_id = 1 and menu_id = 2264);

insert into sys_role_menu (role_id, menu_id)
select 1, 2265
from dual
where not exists (select 1 from sys_role_menu where role_id = 1 and menu_id = 2265);

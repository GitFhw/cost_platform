-- 0.2.0 菜单结构收口：按使用流程拆分成本核算二级目录。
-- 仅调整菜单层级和排序，不改变权限标识、组件路径和业务接口。

insert into sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
select 20200, '基础配置', 2000, 1, 'setup', null, '', 'CostSetupGroup', 1, 0, 'M', '0', '0', '', 'tree', 'admin', sysdate(), '成本核算基础配置分组'
from dual
where not exists (select 1 from sys_menu where menu_id = 20200);

insert into sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
select 20210, '计费建模', 2000, 2, 'model', null, '', 'CostModelGroup', 1, 0, 'M', '0', '0', '', 'guide', 'admin', sysdate(), '费用、变量、规则、公式和发布分组'
from dual
where not exists (select 1 from sys_menu where menu_id = 20210);

insert into sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
select 20220, '核算运行', 2000, 3, 'execution', null, '', 'CostExecutionGroup', 1, 0, 'M', '0', '0', '', 'job', 'admin', sysdate(), '数据接入、试算、正式核算和结果查看分组'
from dual
where not exists (select 1 from sys_menu where menu_id = 20220);

insert into sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
select 20230, '运行治理', 2000, 4, 'ops', null, '', 'CostOpsGroup', 1, 0, 'M', '0', '0', '', 'monitor', 'admin', sysdate(), '账期、告警和审计留痕分组'
from dual
where not exists (select 1 from sys_menu where menu_id = 20230);

update sys_menu
set parent_id = 20200,
    order_num = 1,
    update_by = 'admin',
    update_time = sysdate()
where menu_id = 2001;

update sys_menu
set parent_id = 20200,
    order_num = 2,
    update_by = 'admin',
    update_time = sysdate()
where menu_id = 2002;

update sys_menu
set parent_id = 20210,
    order_num = 1,
    update_by = 'admin',
    update_time = sysdate()
where menu_id = 2003;

update sys_menu
set parent_id = 20210,
    order_num = 2,
    update_by = 'admin',
    update_time = sysdate()
where menu_id = 2004;

update sys_menu
set parent_id = 20210,
    order_num = 3,
    update_by = 'admin',
    update_time = sysdate()
where menu_id = 2005;

update sys_menu
set parent_id = 20210,
    order_num = 4,
    update_by = 'admin',
    update_time = sysdate()
where menu_id = 2013;

update sys_menu
set parent_id = 20210,
    order_num = 5,
    update_by = 'admin',
    update_time = sysdate()
where menu_id = 2006;

update sys_menu
set parent_id = 20220,
    order_num = 1,
    update_by = 'admin',
    update_time = sysdate()
where menu_id = 2015;

update sys_menu
set parent_id = 20220,
    order_num = 2,
    update_by = 'admin',
    update_time = sysdate()
where menu_id = 2007;

update sys_menu
set parent_id = 20220,
    order_num = 3,
    update_by = 'admin',
    update_time = sysdate()
where menu_id = 2008;

update sys_menu
set parent_id = 20220,
    order_num = 4,
    update_by = 'admin',
    update_time = sysdate()
where menu_id = 2014;

update sys_menu
set parent_id = 20220,
    order_num = 5,
    update_by = 'admin',
    update_time = sysdate()
where menu_id = 2009;

update sys_menu
set parent_id = 20230,
    order_num = 1,
    update_by = 'admin',
    update_time = sysdate()
where menu_id = 2010;

update sys_menu
set parent_id = 20230,
    order_num = 2,
    update_by = 'admin',
    update_time = sysdate()
where menu_id = 2012;

update sys_menu
set parent_id = 20230,
    order_num = 3,
    update_by = 'admin',
    update_time = sysdate()
where menu_id = 2011;

insert into sys_role_menu (role_id, menu_id)
select distinct rm.role_id, 2000
from sys_role_menu rm
left join sys_role_menu existing on existing.role_id = rm.role_id and existing.menu_id = 2000
where rm.menu_id in (2001, 2002, 2003, 2004, 2005, 2006, 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015)
  and existing.role_id is null;

insert into sys_role_menu (role_id, menu_id)
select distinct rm.role_id, 20200
from sys_role_menu rm
left join sys_role_menu existing on existing.role_id = rm.role_id and existing.menu_id = 20200
where rm.menu_id in (2001, 2002)
  and existing.role_id is null;

insert into sys_role_menu (role_id, menu_id)
select distinct rm.role_id, 20210
from sys_role_menu rm
left join sys_role_menu existing on existing.role_id = rm.role_id and existing.menu_id = 20210
where rm.menu_id in (2003, 2004, 2005, 2006, 2013)
  and existing.role_id is null;

insert into sys_role_menu (role_id, menu_id)
select distinct rm.role_id, 20220
from sys_role_menu rm
left join sys_role_menu existing on existing.role_id = rm.role_id and existing.menu_id = 20220
where rm.menu_id in (2007, 2008, 2009, 2014, 2015)
  and existing.role_id is null;

insert into sys_role_menu (role_id, menu_id)
select distinct rm.role_id, 20230
from sys_role_menu rm
left join sys_role_menu existing on existing.role_id = rm.role_id and existing.menu_id = 20230
where rm.menu_id in (2010, 2011, 2012)
  and existing.role_id is null;

insert into sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
select 2015, '数据接入', 2000, 14, 'access', 'cost/access/index', '', 'CostDataAccess', 1, 0, 'C', '0', '0', 'cost:task:list', 'guide', 'admin', sysdate(), '线程八：数据接入与导入中心入口'
from dual
where not exists (select 1 from sys_menu where menu_id = 2015);

insert into sys_role_menu (role_id, menu_id)
select 1, 2015 from dual
where not exists (select 1 from sys_role_menu where role_id = 1 and menu_id = 2015);

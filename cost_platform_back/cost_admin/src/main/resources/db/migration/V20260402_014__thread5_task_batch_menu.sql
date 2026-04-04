insert into sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
select 2014, '导入批次', 2000, 13, 'taskBatch', 'cost/task/batch', '', 'CostTaskBatchLedger', 1, 0, 'C', '0', '0', 'cost:task:list', 'upload', 'admin', sysdate(), '线程五：正式核算导入批次台账入口'
from dual
where not exists (select 1 from sys_menu where menu_id = 2014);

insert into sys_role_menu (role_id, menu_id)
select 1, 2014 from dual
where not exists (select 1 from sys_role_menu where role_id = 1 and menu_id = 2014);

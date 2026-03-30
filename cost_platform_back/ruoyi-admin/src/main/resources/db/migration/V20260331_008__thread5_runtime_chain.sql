-- 线程五：运行链

-- ----------------------------
-- 1、线程五字典
-- ----------------------------
insert into sys_dict_type (dict_id, dict_name, dict_type, status, create_by, create_time, remark)
select 135, '核算-试算状态', 'cost_simulation_status', '0', 'admin', sysdate(), '线程五：试算状态字典'
from dual
where not exists (select 1 from sys_dict_type where dict_type = 'cost_simulation_status');

insert into sys_dict_type (dict_id, dict_name, dict_type, status, create_by, create_time, remark)
select 136, '核算-任务类型', 'cost_calc_task_type', '0', 'admin', sysdate(), '线程五：正式核算任务类型字典'
from dual
where not exists (select 1 from sys_dict_type where dict_type = 'cost_calc_task_type');

insert into sys_dict_type (dict_id, dict_name, dict_type, status, create_by, create_time, remark)
select 137, '核算-任务状态', 'cost_calc_task_status', '0', 'admin', sysdate(), '线程五：正式核算任务状态字典'
from dual
where not exists (select 1 from sys_dict_type where dict_type = 'cost_calc_task_status');

insert into sys_dict_type (dict_id, dict_name, dict_type, status, create_by, create_time, remark)
select 138, '核算-结果状态', 'cost_result_status', '0', 'admin', sysdate(), '线程五：结果台账状态字典'
from dual
where not exists (select 1 from sys_dict_type where dict_type = 'cost_result_status');

insert into sys_dict_data (dict_code, dict_sort, dict_label, dict_value, dict_type, list_class, is_default, status, create_by, create_time, remark)
select 1350, 1, '成功', 'SUCCESS', 'cost_simulation_status', 'success', 'Y', '0', 'admin', sysdate(), '试算成功'
from dual
where not exists (select 1 from sys_dict_data where dict_type = 'cost_simulation_status' and dict_value = 'SUCCESS');

insert into sys_dict_data (dict_code, dict_sort, dict_label, dict_value, dict_type, list_class, is_default, status, create_by, create_time, remark)
select 1351, 2, '失败', 'FAILED', 'cost_simulation_status', 'danger', 'N', '0', 'admin', sysdate(), '试算失败'
from dual
where not exists (select 1 from sys_dict_data where dict_type = 'cost_simulation_status' and dict_value = 'FAILED');

insert into sys_dict_data (dict_code, dict_sort, dict_label, dict_value, dict_type, list_class, is_default, status, create_by, create_time, remark)
select 1360, 1, '单笔正式核算', 'FORMAL_SINGLE', 'cost_calc_task_type', 'primary', 'Y', '0', 'admin', sysdate(), '单笔正式核算'
from dual
where not exists (select 1 from sys_dict_data where dict_type = 'cost_calc_task_type' and dict_value = 'FORMAL_SINGLE');

insert into sys_dict_data (dict_code, dict_sort, dict_label, dict_value, dict_type, list_class, is_default, status, create_by, create_time, remark)
select 1361, 2, '批量正式核算', 'FORMAL_BATCH', 'cost_calc_task_type', 'warning', 'N', '0', 'admin', sysdate(), '批量正式核算'
from dual
where not exists (select 1 from sys_dict_data where dict_type = 'cost_calc_task_type' and dict_value = 'FORMAL_BATCH');

insert into sys_dict_data (dict_code, dict_sort, dict_label, dict_value, dict_type, list_class, is_default, status, create_by, create_time, remark)
select 1370, 1, '待执行', 'INIT', 'cost_calc_task_status', 'info', 'Y', '0', 'admin', sysdate(), '任务初始化'
from dual
where not exists (select 1 from sys_dict_data where dict_type = 'cost_calc_task_status' and dict_value = 'INIT');

insert into sys_dict_data (dict_code, dict_sort, dict_label, dict_value, dict_type, list_class, is_default, status, create_by, create_time, remark)
select 1371, 2, '执行中', 'RUNNING', 'cost_calc_task_status', 'primary', 'N', '0', 'admin', sysdate(), '任务执行中'
from dual
where not exists (select 1 from sys_dict_data where dict_type = 'cost_calc_task_status' and dict_value = 'RUNNING');

insert into sys_dict_data (dict_code, dict_sort, dict_label, dict_value, dict_type, list_class, is_default, status, create_by, create_time, remark)
select 1372, 3, '成功', 'SUCCESS', 'cost_calc_task_status', 'success', 'N', '0', 'admin', sysdate(), '任务全部成功'
from dual
where not exists (select 1 from sys_dict_data where dict_type = 'cost_calc_task_status' and dict_value = 'SUCCESS');

insert into sys_dict_data (dict_code, dict_sort, dict_label, dict_value, dict_type, list_class, is_default, status, create_by, create_time, remark)
select 1373, 4, '部分成功', 'PART_SUCCESS', 'cost_calc_task_status', 'warning', 'N', '0', 'admin', sysdate(), '任务部分成功'
from dual
where not exists (select 1 from sys_dict_data where dict_type = 'cost_calc_task_status' and dict_value = 'PART_SUCCESS');

insert into sys_dict_data (dict_code, dict_sort, dict_label, dict_value, dict_type, list_class, is_default, status, create_by, create_time, remark)
select 1374, 5, '失败', 'FAILED', 'cost_calc_task_status', 'danger', 'N', '0', 'admin', sysdate(), '任务失败'
from dual
where not exists (select 1 from sys_dict_data where dict_type = 'cost_calc_task_status' and dict_value = 'FAILED');

insert into sys_dict_data (dict_code, dict_sort, dict_label, dict_value, dict_type, list_class, is_default, status, create_by, create_time, remark)
select 1375, 6, '已取消', 'CANCELLED', 'cost_calc_task_status', 'default', 'N', '0', 'admin', sysdate(), '任务已取消'
from dual
where not exists (select 1 from sys_dict_data where dict_type = 'cost_calc_task_status' and dict_value = 'CANCELLED');

insert into sys_dict_data (dict_code, dict_sort, dict_label, dict_value, dict_type, list_class, is_default, status, create_by, create_time, remark)
select 1380, 1, '成功', 'SUCCESS', 'cost_result_status', 'success', 'Y', '0', 'admin', sysdate(), '结果成功'
from dual
where not exists (select 1 from sys_dict_data where dict_type = 'cost_result_status' and dict_value = 'SUCCESS');

insert into sys_dict_data (dict_code, dict_sort, dict_label, dict_value, dict_type, list_class, is_default, status, create_by, create_time, remark)
select 1381, 2, '失败', 'FAILED', 'cost_result_status', 'danger', 'N', '0', 'admin', sysdate(), '结果失败'
from dual
where not exists (select 1 from sys_dict_data where dict_type = 'cost_result_status' and dict_value = 'FAILED');

insert into sys_dict_data (dict_code, dict_sort, dict_label, dict_value, dict_type, list_class, is_default, status, create_by, create_time, remark)
select 1382, 3, '调整后', 'ADJUSTED', 'cost_result_status', 'warning', 'N', '0', 'admin', sysdate(), '结果经调整'
from dual
where not exists (select 1 from sys_dict_data where dict_type = 'cost_result_status' and dict_value = 'ADJUSTED');

-- ----------------------------
-- 2、线程五菜单入口
-- ----------------------------
insert into sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
select 2007, '试算中心', 2000, 7, 'simulation', 'cost/simulation/index', '', 'CostSimulation', 1, 0, 'C', '0', '0', 'cost:simulation:list', 'edit', 'admin', sysdate(), '线程五试算中心入口'
from dual
where not exists (select 1 from sys_menu where menu_id = 2007);

insert into sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
select 2008, '正式核算', 2000, 8, 'task', 'cost/task/index', '', 'CostTask', 1, 0, 'C', '0', '0', 'cost:task:list', 'job', 'admin', sysdate(), '线程五正式核算入口'
from dual
where not exists (select 1 from sys_menu where menu_id = 2008);

insert into sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
select 2009, '结果台账', 2000, 9, 'result', 'cost/result/index', '', 'CostResult', 1, 0, 'C', '0', '0', 'cost:result:list', 'form', 'admin', sysdate(), '线程五结果台账入口'
from dual
where not exists (select 1 from sys_menu where menu_id = 2009);

insert into sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
select 2240, '试算查询', 2007, 1, '#', '', '', '', 1, 0, 'F', '0', '0', 'cost:simulation:query', '#', 'admin', sysdate(), ''
from dual
where not exists (select 1 from sys_menu where menu_id = 2240);

insert into sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
select 2241, '执行试算', 2007, 2, '#', '', '', '', 1, 0, 'F', '0', '0', 'cost:simulation:execute', '#', 'admin', sysdate(), ''
from dual
where not exists (select 1 from sys_menu where menu_id = 2241);

insert into sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
select 2242, '任务查询', 2008, 1, '#', '', '', '', 1, 0, 'F', '0', '0', 'cost:task:query', '#', 'admin', sysdate(), ''
from dual
where not exists (select 1 from sys_menu where menu_id = 2242);

insert into sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
select 2243, '提交任务', 2008, 2, '#', '', '', '', 1, 0, 'F', '0', '0', 'cost:task:execute', '#', 'admin', sysdate(), ''
from dual
where not exists (select 1 from sys_menu where menu_id = 2243);

insert into sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
select 2244, '重试明细', 2008, 3, '#', '', '', '', 1, 0, 'F', '0', '0', 'cost:task:retry', '#', 'admin', sysdate(), ''
from dual
where not exists (select 1 from sys_menu where menu_id = 2244);

insert into sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
select 2245, '取消任务', 2008, 4, '#', '', '', '', 1, 0, 'F', '0', '0', 'cost:task:cancel', '#', 'admin', sysdate(), ''
from dual
where not exists (select 1 from sys_menu where menu_id = 2245);

insert into sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
select 2246, '结果查询', 2009, 1, '#', '', '', '', 1, 0, 'F', '0', '0', 'cost:result:query', '#', 'admin', sysdate(), ''
from dual
where not exists (select 1 from sys_menu where menu_id = 2246);

insert into sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
select 2247, '追溯解释', 2009, 2, '#', '', '', '', 1, 0, 'F', '0', '0', 'cost:result:trace', '#', 'admin', sysdate(), ''
from dual
where not exists (select 1 from sys_menu where menu_id = 2247);

-- ----------------------------
-- 3、线程五菜单授权给管理员角色
-- ----------------------------
insert into sys_role_menu (role_id, menu_id)
select 1, 2007 from dual
where not exists (select 1 from sys_role_menu where role_id = 1 and menu_id = 2007);

insert into sys_role_menu (role_id, menu_id)
select 1, 2008 from dual
where not exists (select 1 from sys_role_menu where role_id = 1 and menu_id = 2008);

insert into sys_role_menu (role_id, menu_id)
select 1, 2009 from dual
where not exists (select 1 from sys_role_menu where role_id = 1 and menu_id = 2009);

insert into sys_role_menu (role_id, menu_id)
select 1, 2240 from dual
where not exists (select 1 from sys_role_menu where role_id = 1 and menu_id = 2240);

insert into sys_role_menu (role_id, menu_id)
select 1, 2241 from dual
where not exists (select 1 from sys_role_menu where role_id = 1 and menu_id = 2241);

insert into sys_role_menu (role_id, menu_id)
select 1, 2242 from dual
where not exists (select 1 from sys_role_menu where role_id = 1 and menu_id = 2242);

insert into sys_role_menu (role_id, menu_id)
select 1, 2243 from dual
where not exists (select 1 from sys_role_menu where role_id = 1 and menu_id = 2243);

insert into sys_role_menu (role_id, menu_id)
select 1, 2244 from dual
where not exists (select 1 from sys_role_menu where role_id = 1 and menu_id = 2244);

insert into sys_role_menu (role_id, menu_id)
select 1, 2245 from dual
where not exists (select 1 from sys_role_menu where role_id = 1 and menu_id = 2245);

insert into sys_role_menu (role_id, menu_id)
select 1, 2246 from dual
where not exists (select 1 from sys_role_menu where role_id = 1 and menu_id = 2246);

insert into sys_role_menu (role_id, menu_id)
select 1, 2247 from dual
where not exists (select 1 from sys_role_menu where role_id = 1 and menu_id = 2247);

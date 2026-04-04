-- 线程二：费用中心 + 变量中心 + 第三方系统接入变量

-- ----------------------------
-- 1、费用/变量字典
-- ----------------------------
insert into sys_dict_type (dict_id, dict_name, dict_type, status, create_by, create_time, remark)
select 120, '核算-费用状态', 'cost_fee_status', '0', 'admin', sysdate(), '线程二：费用中心状态字典'
from dual
where not exists (select 1 from sys_dict_type where dict_type = 'cost_fee_status');

insert into sys_dict_type (dict_id, dict_name, dict_type, status, create_by, create_time, remark)
select 121, '核算-变量状态', 'cost_variable_status', '0', 'admin', sysdate(), '线程二：变量中心状态字典'
from dual
where not exists (select 1 from sys_dict_type where dict_type = 'cost_variable_status');

insert into sys_dict_type (dict_id, dict_name, dict_type, status, create_by, create_time, remark)
select 122, '核算-变量分组状态', 'cost_variable_group_status', '0', 'admin', sysdate(), '线程二：变量分组状态字典'
from dual
where not exists (select 1 from sys_dict_type where dict_type = 'cost_variable_group_status');

insert into sys_dict_type (dict_id, dict_name, dict_type, status, create_by, create_time, remark)
select 123, '核算-变量类型', 'cost_variable_type', '0', 'admin', sysdate(), '线程二：变量类型字典'
from dual
where not exists (select 1 from sys_dict_type where dict_type = 'cost_variable_type');

insert into sys_dict_type (dict_id, dict_name, dict_type, status, create_by, create_time, remark)
select 124, '核算-变量来源类型', 'cost_variable_source_type', '0', 'admin', sysdate(), '线程二：变量来源字典'
from dual
where not exists (select 1 from sys_dict_type where dict_type = 'cost_variable_source_type');

insert into sys_dict_type (dict_id, dict_name, dict_type, status, create_by, create_time, remark)
select 125, '核算-变量数据类型', 'cost_variable_data_type', '0', 'admin', sysdate(), '线程二：变量数据类型字典'
from dual
where not exists (select 1 from sys_dict_type where dict_type = 'cost_variable_data_type');

insert into sys_dict_data (dict_code, dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
select 1200, 1, '正常', '0', 'cost_fee_status', '', 'success', 'Y', '0', 'admin', sysdate(), '费用可维护'
from dual
where not exists (select 1 from sys_dict_data where dict_type = 'cost_fee_status' and dict_value = '0');

insert into sys_dict_data (dict_code, dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
select 1201, 2, '停用', '1', 'cost_fee_status', '', 'danger', 'N', '0', 'admin', sysdate(), '费用停用'
from dual
where not exists (select 1 from sys_dict_data where dict_type = 'cost_fee_status' and dict_value = '1');

insert into sys_dict_data (dict_code, dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
select 1210, 1, '正常', '0', 'cost_variable_status', '', 'success', 'Y', '0', 'admin', sysdate(), '变量可维护'
from dual
where not exists (select 1 from sys_dict_data where dict_type = 'cost_variable_status' and dict_value = '0');

insert into sys_dict_data (dict_code, dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
select 1211, 2, '停用', '1', 'cost_variable_status', '', 'danger', 'N', '0', 'admin', sysdate(), '变量停用'
from dual
where not exists (select 1 from sys_dict_data where dict_type = 'cost_variable_status' and dict_value = '1');

insert into sys_dict_data (dict_code, dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
select 1220, 1, '正常', '0', 'cost_variable_group_status', '', 'success', 'Y', '0', 'admin', sysdate(), '分组可维护'
from dual
where not exists (select 1 from sys_dict_data where dict_type = 'cost_variable_group_status' and dict_value = '0');

insert into sys_dict_data (dict_code, dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
select 1221, 2, '停用', '1', 'cost_variable_group_status', '', 'danger', 'N', '0', 'admin', sysdate(), '分组停用'
from dual
where not exists (select 1 from sys_dict_data where dict_type = 'cost_variable_group_status' and dict_value = '1');

insert into sys_dict_data (dict_code, dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
select 1230, 1, '文本', 'TEXT', 'cost_variable_type', '', 'primary', 'Y', '0', 'admin', sysdate(), '文本变量'
from dual
where not exists (select 1 from sys_dict_data where dict_type = 'cost_variable_type' and dict_value = 'TEXT');

insert into sys_dict_data (dict_code, dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
select 1231, 2, '数值', 'NUMBER', 'cost_variable_type', '', 'success', 'N', '0', 'admin', sysdate(), '数值变量'
from dual
where not exists (select 1 from sys_dict_data where dict_type = 'cost_variable_type' and dict_value = 'NUMBER');

insert into sys_dict_data (dict_code, dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
select 1232, 3, '字典下拉', 'DICT', 'cost_variable_type', '', 'warning', 'N', '0', 'admin', sysdate(), '字典变量'
from dual
where not exists (select 1 from sys_dict_data where dict_type = 'cost_variable_type' and dict_value = 'DICT');

insert into sys_dict_data (dict_code, dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
select 1233, 4, '接口下拉', 'REMOTE', 'cost_variable_type', '', 'info', 'N', '0', 'admin', sysdate(), '第三方接口变量'
from dual
where not exists (select 1 from sys_dict_data where dict_type = 'cost_variable_type' and dict_value = 'REMOTE');

insert into sys_dict_data (dict_code, dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
select 1234, 5, '公式', 'FORMULA', 'cost_variable_type', '', 'danger', 'N', '0', 'admin', sysdate(), '公式变量'
from dual
where not exists (select 1 from sys_dict_data where dict_type = 'cost_variable_type' and dict_value = 'FORMULA');

insert into sys_dict_data (dict_code, dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
select 1235, 6, '布尔', 'BOOLEAN', 'cost_variable_type', '', 'default', 'N', '0', 'admin', sysdate(), '布尔变量'
from dual
where not exists (select 1 from sys_dict_data where dict_type = 'cost_variable_type' and dict_value = 'BOOLEAN');

insert into sys_dict_data (dict_code, dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
select 1236, 7, '日期', 'DATE', 'cost_variable_type', '', 'default', 'N', '0', 'admin', sysdate(), '日期变量'
from dual
where not exists (select 1 from sys_dict_data where dict_type = 'cost_variable_type' and dict_value = 'DATE');

insert into sys_dict_data (dict_code, dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
select 1240, 1, '手工输入', 'INPUT', 'cost_variable_source_type', '', 'primary', 'Y', '0', 'admin', sysdate(), '手工维护变量'
from dual
where not exists (select 1 from sys_dict_data where dict_type = 'cost_variable_source_type' and dict_value = 'INPUT');

insert into sys_dict_data (dict_code, dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
select 1241, 2, '字典接入', 'DICT', 'cost_variable_source_type', '', 'success', 'N', '0', 'admin', sysdate(), '字典变量'
from dual
where not exists (select 1 from sys_dict_data where dict_type = 'cost_variable_source_type' and dict_value = 'DICT');

insert into sys_dict_data (dict_code, dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
select 1242, 3, '第三方接口', 'REMOTE', 'cost_variable_source_type', '', 'warning', 'N', '0', 'admin', sysdate(), '第三方接口变量'
from dual
where not exists (select 1 from sys_dict_data where dict_type = 'cost_variable_source_type' and dict_value = 'REMOTE');

insert into sys_dict_data (dict_code, dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
select 1243, 4, '公式派生', 'FORMULA', 'cost_variable_source_type', '', 'info', 'N', '0', 'admin', sysdate(), '公式变量'
from dual
where not exists (select 1 from sys_dict_data where dict_type = 'cost_variable_source_type' and dict_value = 'FORMULA');

insert into sys_dict_data (dict_code, dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
select 1250, 1, '字符串', 'STRING', 'cost_variable_data_type', '', 'primary', 'Y', '0', 'admin', sysdate(), '字符串数据'
from dual
where not exists (select 1 from sys_dict_data where dict_type = 'cost_variable_data_type' and dict_value = 'STRING');

insert into sys_dict_data (dict_code, dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
select 1251, 2, '数值', 'NUMBER', 'cost_variable_data_type', '', 'success', 'N', '0', 'admin', sysdate(), '数值数据'
from dual
where not exists (select 1 from sys_dict_data where dict_type = 'cost_variable_data_type' and dict_value = 'NUMBER');

insert into sys_dict_data (dict_code, dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
select 1252, 3, '布尔', 'BOOLEAN', 'cost_variable_data_type', '', 'warning', 'N', '0', 'admin', sysdate(), '布尔数据'
from dual
where not exists (select 1 from sys_dict_data where dict_type = 'cost_variable_data_type' and dict_value = 'BOOLEAN');

insert into sys_dict_data (dict_code, dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
select 1253, 4, '日期', 'DATE', 'cost_variable_data_type', '', 'info', 'N', '0', 'admin', sysdate(), '日期数据'
from dual
where not exists (select 1 from sys_dict_data where dict_type = 'cost_variable_data_type' and dict_value = 'DATE');

insert into sys_dict_data (dict_code, dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
select 1254, 5, 'JSON', 'JSON', 'cost_variable_data_type', '', 'default', 'N', '0', 'admin', sysdate(), 'JSON数据'
from dual
where not exists (select 1 from sys_dict_data where dict_type = 'cost_variable_data_type' and dict_value = 'JSON');

-- ----------------------------
-- 2、线程二菜单入口
-- ----------------------------
insert into sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
select 2003, '费用中心', 2000, 3, 'fee', 'cost/fee/index', '', 'CostFee', 1, 0, 'C', '0', '0', 'cost:fee:list', 'money', 'admin', sysdate(), '线程二费用中心入口'
from dual
where not exists (select 1 from sys_menu where menu_id = 2003);

insert into sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
select 2004, '变量中心', 2000, 4, 'variable', 'cost/variable/index', '', 'CostVariable', 1, 0, 'C', '0', '0', 'cost:variable:list', 'guide', 'admin', sysdate(), '线程二变量中心入口'
from dual
where not exists (select 1 from sys_menu where menu_id = 2004);

insert into sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
select 2200, '费用查询', 2003, 1, '#', '', '', '', 1, 0, 'F', '0', '0', 'cost:fee:query', '#', 'admin', sysdate(), ''
from dual
where not exists (select 1 from sys_menu where menu_id = 2200);

insert into sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
select 2201, '费用新增', 2003, 2, '#', '', '', '', 1, 0, 'F', '0', '0', 'cost:fee:add', '#', 'admin', sysdate(), ''
from dual
where not exists (select 1 from sys_menu where menu_id = 2201);

insert into sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
select 2202, '费用修改', 2003, 3, '#', '', '', '', 1, 0, 'F', '0', '0', 'cost:fee:edit', '#', 'admin', sysdate(), ''
from dual
where not exists (select 1 from sys_menu where menu_id = 2202);

insert into sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
select 2203, '费用删除', 2003, 4, '#', '', '', '', 1, 0, 'F', '0', '0', 'cost:fee:remove', '#', 'admin', sysdate(), ''
from dual
where not exists (select 1 from sys_menu where menu_id = 2203);

insert into sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
select 2204, '费用导出', 2003, 5, '#', '', '', '', 1, 0, 'F', '0', '0', 'cost:fee:export', '#', 'admin', sysdate(), ''
from dual
where not exists (select 1 from sys_menu where menu_id = 2204);

insert into sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
select 2210, '变量查询', 2004, 1, '#', '', '', '', 1, 0, 'F', '0', '0', 'cost:variable:query', '#', 'admin', sysdate(), ''
from dual
where not exists (select 1 from sys_menu where menu_id = 2210);

insert into sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
select 2211, '变量新增', 2004, 2, '#', '', '', '', 1, 0, 'F', '0', '0', 'cost:variable:add', '#', 'admin', sysdate(), ''
from dual
where not exists (select 1 from sys_menu where menu_id = 2211);

insert into sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
select 2212, '变量修改', 2004, 3, '#', '', '', '', 1, 0, 'F', '0', '0', 'cost:variable:edit', '#', 'admin', sysdate(), ''
from dual
where not exists (select 1 from sys_menu where menu_id = 2212);

insert into sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
select 2213, '变量删除', 2004, 4, '#', '', '', '', 1, 0, 'F', '0', '0', 'cost:variable:remove', '#', 'admin', sysdate(), ''
from dual
where not exists (select 1 from sys_menu where menu_id = 2213);

insert into sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
select 2214, '变量导出', 2004, 5, '#', '', '', '', 1, 0, 'F', '0', '0', 'cost:variable:export', '#', 'admin', sysdate(), ''
from dual
where not exists (select 1 from sys_menu where menu_id = 2214);

-- 线程三：规则中心 + 阶梯规则

-- ----------------------------
-- 1、线程三字典
-- ----------------------------
insert into sys_dict_type (dict_id, dict_name, dict_type, status, create_by, create_time, remark)
select 126, '核算-规则状态', 'cost_rule_status', '0', 'admin', sysdate(), '线程三：规则状态字典'
from dual
where not exists (select 1 from sys_dict_type where dict_type = 'cost_rule_status');

insert into sys_dict_type (dict_id, dict_name, dict_type, status, create_by, create_time, remark)
select 127, '核算-规则类型', 'cost_rule_type', '0', 'admin', sysdate(), '线程三：规则类型字典'
from dual
where not exists (select 1 from sys_dict_type where dict_type = 'cost_rule_type');

insert into sys_dict_type (dict_id, dict_name, dict_type, status, create_by, create_time, remark)
select 128, '核算-规则条件逻辑', 'cost_rule_condition_logic', '0', 'admin', sysdate(), '线程三：规则条件逻辑字典'
from dual
where not exists (select 1 from sys_dict_type where dict_type = 'cost_rule_condition_logic');

insert into sys_dict_type (dict_id, dict_name, dict_type, status, create_by, create_time, remark)
select 129, '核算-规则操作符', 'cost_rule_operator', '0', 'admin', sysdate(), '线程三：规则操作符字典'
from dual
where not exists (select 1 from sys_dict_type where dict_type = 'cost_rule_operator');

insert into sys_dict_type (dict_id, dict_name, dict_type, status, create_by, create_time, remark)
select 130, '核算-阶梯区间模式', 'cost_rule_interval_mode', '0', 'admin', sysdate(), '线程三：阶梯区间模式字典'
from dual
where not exists (select 1 from sys_dict_type where dict_type = 'cost_rule_interval_mode');

insert into sys_dict_data (dict_code, dict_sort, dict_label, dict_value, dict_type, list_class, is_default, status, create_by, create_time, remark)
select 1260, 1, '正常', '0', 'cost_rule_status', 'success', 'Y', '0', 'admin', sysdate(), '规则可维护'
from dual
where not exists (select 1 from sys_dict_data where dict_type = 'cost_rule_status' and dict_value = '0');

insert into sys_dict_data (dict_code, dict_sort, dict_label, dict_value, dict_type, list_class, is_default, status, create_by, create_time, remark)
select 1261, 2, '停用', '1', 'cost_rule_status', 'danger', 'N', '0', 'admin', sysdate(), '规则停用'
from dual
where not exists (select 1 from sys_dict_data where dict_type = 'cost_rule_status' and dict_value = '1');

insert into sys_dict_data (dict_code, dict_sort, dict_label, dict_value, dict_type, list_class, is_default, status, create_by, create_time, remark)
select 1270, 1, '固定费率', 'FIXED_RATE', 'cost_rule_type', 'primary', 'Y', '0', 'admin', sysdate(), '固定费率规则'
from dual
where not exists (select 1 from sys_dict_data where dict_type = 'cost_rule_type' and dict_value = 'FIXED_RATE');

insert into sys_dict_data (dict_code, dict_sort, dict_label, dict_value, dict_type, list_class, is_default, status, create_by, create_time, remark)
select 1271, 2, '固定金额', 'FIXED_AMOUNT', 'cost_rule_type', 'success', 'N', '0', 'admin', sysdate(), '固定金额规则'
from dual
where not exists (select 1 from sys_dict_data where dict_type = 'cost_rule_type' and dict_value = 'FIXED_AMOUNT');

insert into sys_dict_data (dict_code, dict_sort, dict_label, dict_value, dict_type, list_class, is_default, status, create_by, create_time, remark)
select 1272, 3, '公式金额', 'FORMULA', 'cost_rule_type', 'warning', 'N', '0', 'admin', sysdate(), '公式金额规则'
from dual
where not exists (select 1 from sys_dict_data where dict_type = 'cost_rule_type' and dict_value = 'FORMULA');

insert into sys_dict_data (dict_code, dict_sort, dict_label, dict_value, dict_type, list_class, is_default, status, create_by, create_time, remark)
select 1273, 4, '阶梯费率', 'TIER_RATE', 'cost_rule_type', 'info', 'N', '0', 'admin', sysdate(), '阶梯费率规则'
from dual
where not exists (select 1 from sys_dict_data where dict_type = 'cost_rule_type' and dict_value = 'TIER_RATE');

insert into sys_dict_data (dict_code, dict_sort, dict_label, dict_value, dict_type, list_class, is_default, status, create_by, create_time, remark)
select 1280, 1, '且', 'AND', 'cost_rule_condition_logic', 'primary', 'Y', '0', 'admin', sysdate(), '同组条件且关系'
from dual
where not exists (select 1 from sys_dict_data where dict_type = 'cost_rule_condition_logic' and dict_value = 'AND');

insert into sys_dict_data (dict_code, dict_sort, dict_label, dict_value, dict_type, list_class, is_default, status, create_by, create_time, remark)
select 1281, 2, '或', 'OR', 'cost_rule_condition_logic', 'warning', 'N', '0', 'admin', sysdate(), '同组条件或关系'
from dual
where not exists (select 1 from sys_dict_data where dict_type = 'cost_rule_condition_logic' and dict_value = 'OR');

insert into sys_dict_data (dict_code, dict_sort, dict_label, dict_value, dict_type, list_class, is_default, status, create_by, create_time, remark)
select 1290, 1, '等于', 'EQ', 'cost_rule_operator', 'primary', 'Y', '0', 'admin', sysdate(), '等于'
from dual
where not exists (select 1 from sys_dict_data where dict_type = 'cost_rule_operator' and dict_value = 'EQ');

insert into sys_dict_data (dict_code, dict_sort, dict_label, dict_value, dict_type, list_class, is_default, status, create_by, create_time, remark)
select 1291, 2, '不等于', 'NE', 'cost_rule_operator', 'info', 'N', '0', 'admin', sysdate(), '不等于'
from dual
where not exists (select 1 from sys_dict_data where dict_type = 'cost_rule_operator' and dict_value = 'NE');

insert into sys_dict_data (dict_code, dict_sort, dict_label, dict_value, dict_type, list_class, is_default, status, create_by, create_time, remark)
select 1292, 3, '大于', 'GT', 'cost_rule_operator', 'warning', 'N', '0', 'admin', sysdate(), '大于'
from dual
where not exists (select 1 from sys_dict_data where dict_type = 'cost_rule_operator' and dict_value = 'GT');

insert into sys_dict_data (dict_code, dict_sort, dict_label, dict_value, dict_type, list_class, is_default, status, create_by, create_time, remark)
select 1293, 4, '大于等于', 'GE', 'cost_rule_operator', 'success', 'N', '0', 'admin', sysdate(), '大于等于'
from dual
where not exists (select 1 from sys_dict_data where dict_type = 'cost_rule_operator' and dict_value = 'GE');

insert into sys_dict_data (dict_code, dict_sort, dict_label, dict_value, dict_type, list_class, is_default, status, create_by, create_time, remark)
select 1294, 5, '小于', 'LT', 'cost_rule_operator', 'warning', 'N', '0', 'admin', sysdate(), '小于'
from dual
where not exists (select 1 from sys_dict_data where dict_type = 'cost_rule_operator' and dict_value = 'LT');

insert into sys_dict_data (dict_code, dict_sort, dict_label, dict_value, dict_type, list_class, is_default, status, create_by, create_time, remark)
select 1295, 6, '小于等于', 'LE', 'cost_rule_operator', 'success', 'N', '0', 'admin', sysdate(), '小于等于'
from dual
where not exists (select 1 from sys_dict_data where dict_type = 'cost_rule_operator' and dict_value = 'LE');

insert into sys_dict_data (dict_code, dict_sort, dict_label, dict_value, dict_type, list_class, is_default, status, create_by, create_time, remark)
select 1296, 7, '包含任一值', 'IN', 'cost_rule_operator', 'info', 'N', '0', 'admin', sysdate(), '多值包含'
from dual
where not exists (select 1 from sys_dict_data where dict_type = 'cost_rule_operator' and dict_value = 'IN');

insert into sys_dict_data (dict_code, dict_sort, dict_label, dict_value, dict_type, list_class, is_default, status, create_by, create_time, remark)
select 1297, 8, '不包含', 'NOT_IN', 'cost_rule_operator', 'danger', 'N', '0', 'admin', sysdate(), '多值排除'
from dual
where not exists (select 1 from sys_dict_data where dict_type = 'cost_rule_operator' and dict_value = 'NOT_IN');

insert into sys_dict_data (dict_code, dict_sort, dict_label, dict_value, dict_type, list_class, is_default, status, create_by, create_time, remark)
select 1298, 9, '区间', 'BETWEEN', 'cost_rule_operator', 'primary', 'N', '0', 'admin', sysdate(), '区间比较'
from dual
where not exists (select 1 from sys_dict_data where dict_type = 'cost_rule_operator' and dict_value = 'BETWEEN');

insert into sys_dict_data (dict_code, dict_sort, dict_label, dict_value, dict_type, list_class, is_default, status, create_by, create_time, remark)
select 1299, 10, '表达式', 'EXPR', 'cost_rule_operator', 'default', 'N', '0', 'admin', sysdate(), '表达式条件'
from dual
where not exists (select 1 from sys_dict_data where dict_type = 'cost_rule_operator' and dict_value = 'EXPR');

insert into sys_dict_data (dict_code, dict_sort, dict_label, dict_value, dict_type, list_class, is_default, status, create_by, create_time, remark)
select 1300, 1, '左闭右开', 'LEFT_CLOSED_RIGHT_OPEN', 'cost_rule_interval_mode', 'primary', 'Y', '0', 'admin', sysdate(), 'start <= x < end'
from dual
where not exists (select 1 from sys_dict_data where dict_type = 'cost_rule_interval_mode' and dict_value = 'LEFT_CLOSED_RIGHT_OPEN');

insert into sys_dict_data (dict_code, dict_sort, dict_label, dict_value, dict_type, list_class, is_default, status, create_by, create_time, remark)
select 1301, 2, '左开右闭', 'LEFT_OPEN_RIGHT_CLOSED', 'cost_rule_interval_mode', 'warning', 'N', '0', 'admin', sysdate(), 'start < x <= end'
from dual
where not exists (select 1 from sys_dict_data where dict_type = 'cost_rule_interval_mode' and dict_value = 'LEFT_OPEN_RIGHT_CLOSED');

-- ----------------------------
-- 2、线程三菜单入口
-- ----------------------------
insert into sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
select 2005, '规则中心', 2000, 5, 'rule', 'cost/rule/index', '', 'CostRule', 1, 0, 'C', '0', '0', 'cost:rule:list', 'rate', 'admin', sysdate(), '线程三规则中心入口'
from dual
where not exists (select 1 from sys_menu where menu_id = 2005);

insert into sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
select 2220, '规则查询', 2005, 1, '#', '', '', '', 1, 0, 'F', '0', '0', 'cost:rule:query', '#', 'admin', sysdate(), ''
from dual
where not exists (select 1 from sys_menu where menu_id = 2220);

insert into sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
select 2221, '规则新增', 2005, 2, '#', '', '', '', 1, 0, 'F', '0', '0', 'cost:rule:add', '#', 'admin', sysdate(), ''
from dual
where not exists (select 1 from sys_menu where menu_id = 2221);

insert into sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
select 2222, '规则修改', 2005, 3, '#', '', '', '', 1, 0, 'F', '0', '0', 'cost:rule:edit', '#', 'admin', sysdate(), ''
from dual
where not exists (select 1 from sys_menu where menu_id = 2222);

insert into sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
select 2223, '规则删除', 2005, 4, '#', '', '', '', 1, 0, 'F', '0', '0', 'cost:rule:remove', '#', 'admin', sysdate(), ''
from dual
where not exists (select 1 from sys_menu where menu_id = 2223);

insert into sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
select 2224, '规则导出', 2005, 5, '#', '', '', '', 1, 0, 'F', '0', '0', 'cost:rule:export', '#', 'admin', sysdate(), ''
from dual
where not exists (select 1 from sys_menu where menu_id = 2224);

-- ----------------------------
-- 3、线程三菜单授权给管理员角色
-- ----------------------------
insert into sys_role_menu (role_id, menu_id)
select 1, 2005
from dual
where not exists (select 1 from sys_role_menu where role_id = 1 and menu_id = 2005);

insert into sys_role_menu (role_id, menu_id)
select 1, 2220
from dual
where not exists (select 1 from sys_role_menu where role_id = 1 and menu_id = 2220);

insert into sys_role_menu (role_id, menu_id)
select 1, 2221
from dual
where not exists (select 1 from sys_role_menu where role_id = 1 and menu_id = 2221);

insert into sys_role_menu (role_id, menu_id)
select 1, 2222
from dual
where not exists (select 1 from sys_role_menu where role_id = 1 and menu_id = 2222);

insert into sys_role_menu (role_id, menu_id)
select 1, 2223
from dual
where not exists (select 1 from sys_role_menu where role_id = 1 and menu_id = 2223);

insert into sys_role_menu (role_id, menu_id)
select 1, 2224
from dual
where not exists (select 1 from sys_role_menu where role_id = 1 and menu_id = 2224);

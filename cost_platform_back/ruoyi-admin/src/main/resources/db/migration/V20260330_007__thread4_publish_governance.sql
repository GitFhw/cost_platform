-- 线程四：发布治理

-- ----------------------------
-- 1、线程四字典
-- ----------------------------
insert into sys_dict_type (dict_id, dict_name, dict_type, status, create_by, create_time, remark)
select 133, '核算-发布版本状态', 'cost_publish_version_status', '0', 'admin', sysdate(), '线程四：发布版本状态字典'
from dual
where not exists (select 1 from sys_dict_type where dict_type = 'cost_publish_version_status');

insert into sys_dict_type (dict_id, dict_name, dict_type, status, create_by, create_time, remark)
select 134, '核算-发布快照类型', 'cost_publish_snapshot_type', '0', 'admin', sysdate(), '线程四：发布快照类型字典'
from dual
where not exists (select 1 from sys_dict_type where dict_type = 'cost_publish_snapshot_type');

insert into sys_dict_data (dict_code, dict_sort, dict_label, dict_value, dict_type, list_class, is_default, status, create_by, create_time, remark)
select 1330, 1, '已发布', 'PUBLISHED', 'cost_publish_version_status', 'primary', 'Y', '0', 'admin', sysdate(), '已发布未生效'
from dual
where not exists (select 1 from sys_dict_data where dict_type = 'cost_publish_version_status' and dict_value = 'PUBLISHED');

insert into sys_dict_data (dict_code, dict_sort, dict_label, dict_value, dict_type, list_class, is_default, status, create_by, create_time, remark)
select 1331, 2, '生效中', 'ACTIVE', 'cost_publish_version_status', 'success', 'N', '0', 'admin', sysdate(), '当前生效版本'
from dual
where not exists (select 1 from sys_dict_data where dict_type = 'cost_publish_version_status' and dict_value = 'ACTIVE');

insert into sys_dict_data (dict_code, dict_sort, dict_label, dict_value, dict_type, list_class, is_default, status, create_by, create_time, remark)
select 1332, 3, '已回滚', 'ROLLED_BACK', 'cost_publish_version_status', 'warning', 'N', '0', 'admin', sysdate(), '被回滚替换的版本'
from dual
where not exists (select 1 from sys_dict_data where dict_type = 'cost_publish_version_status' and dict_value = 'ROLLED_BACK');

insert into sys_dict_data (dict_code, dict_sort, dict_label, dict_value, dict_type, list_class, is_default, status, create_by, create_time, remark)
select 1340, 1, '场景', 'SCENE', 'cost_publish_snapshot_type', 'primary', 'Y', '0', 'admin', sysdate(), '场景快照'
from dual
where not exists (select 1 from sys_dict_data where dict_type = 'cost_publish_snapshot_type' and dict_value = 'SCENE');

insert into sys_dict_data (dict_code, dict_sort, dict_label, dict_value, dict_type, list_class, is_default, status, create_by, create_time, remark)
select 1341, 2, '费用', 'FEE', 'cost_publish_snapshot_type', 'success', 'N', '0', 'admin', sysdate(), '费用快照'
from dual
where not exists (select 1 from sys_dict_data where dict_type = 'cost_publish_snapshot_type' and dict_value = 'FEE');

insert into sys_dict_data (dict_code, dict_sort, dict_label, dict_value, dict_type, list_class, is_default, status, create_by, create_time, remark)
select 1342, 3, '变量', 'VARIABLE', 'cost_publish_snapshot_type', 'warning', 'N', '0', 'admin', sysdate(), '变量快照'
from dual
where not exists (select 1 from sys_dict_data where dict_type = 'cost_publish_snapshot_type' and dict_value = 'VARIABLE');

insert into sys_dict_data (dict_code, dict_sort, dict_label, dict_value, dict_type, list_class, is_default, status, create_by, create_time, remark)
select 1343, 4, '规则', 'RULE', 'cost_publish_snapshot_type', 'info', 'N', '0', 'admin', sysdate(), '规则快照'
from dual
where not exists (select 1 from sys_dict_data where dict_type = 'cost_publish_snapshot_type' and dict_value = 'RULE');

insert into sys_dict_data (dict_code, dict_sort, dict_label, dict_value, dict_type, list_class, is_default, status, create_by, create_time, remark)
select 1344, 5, '条件', 'RULE_CONDITION', 'cost_publish_snapshot_type', 'default', 'N', '0', 'admin', sysdate(), '规则条件快照'
from dual
where not exists (select 1 from sys_dict_data where dict_type = 'cost_publish_snapshot_type' and dict_value = 'RULE_CONDITION');

insert into sys_dict_data (dict_code, dict_sort, dict_label, dict_value, dict_type, list_class, is_default, status, create_by, create_time, remark)
select 1345, 6, '阶梯', 'RULE_TIER', 'cost_publish_snapshot_type', 'danger', 'N', '0', 'admin', sysdate(), '规则阶梯快照'
from dual
where not exists (select 1 from sys_dict_data where dict_type = 'cost_publish_snapshot_type' and dict_value = 'RULE_TIER');

-- ----------------------------
-- 2、线程四菜单入口
-- ----------------------------
insert into sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
select 2006, '发布中心', 2000, 6, 'publish', 'cost/publish/index', '', 'CostPublish', 1, 0, 'C', '0', '0', 'cost:publish:list', 'chart', 'admin', sysdate(), '线程四发布治理入口'
from dual
where not exists (select 1 from sys_menu where menu_id = 2006);

insert into sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
select 2230, '发布查询', 2006, 1, '#', '', '', '', 1, 0, 'F', '0', '0', 'cost:publish:query', '#', 'admin', sysdate(), ''
from dual
where not exists (select 1 from sys_menu where menu_id = 2230);

insert into sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
select 2231, '生成版本', 2006, 2, '#', '', '', '', 1, 0, 'F', '0', '0', 'cost:publish:add', '#', 'admin', sysdate(), ''
from dual
where not exists (select 1 from sys_menu where menu_id = 2231);

insert into sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
select 2232, '设为生效', 2006, 3, '#', '', '', '', 1, 0, 'F', '0', '0', 'cost:publish:activate', '#', 'admin', sysdate(), ''
from dual
where not exists (select 1 from sys_menu where menu_id = 2232);

insert into sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
select 2233, '版本回滚', 2006, 4, '#', '', '', '', 1, 0, 'F', '0', '0', 'cost:publish:rollback', '#', 'admin', sysdate(), ''
from dual
where not exists (select 1 from sys_menu where menu_id = 2233);

-- ----------------------------
-- 3、线程四菜单授权给管理员角色
-- ----------------------------
insert into sys_role_menu (role_id, menu_id)
select 1, 2006
from dual
where not exists (select 1 from sys_role_menu where role_id = 1 and menu_id = 2006);

insert into sys_role_menu (role_id, menu_id)
select 1, 2230
from dual
where not exists (select 1 from sys_role_menu where role_id = 1 and menu_id = 2230);

insert into sys_role_menu (role_id, menu_id)
select 1, 2231
from dual
where not exists (select 1 from sys_role_menu where role_id = 1 and menu_id = 2231);

insert into sys_role_menu (role_id, menu_id)
select 1, 2232
from dual
where not exists (select 1 from sys_role_menu where role_id = 1 and menu_id = 2232);

insert into sys_role_menu (role_id, menu_id)
select 1, 2233
from dual
where not exists (select 1 from sys_role_menu where role_id = 1 and menu_id = 2233);

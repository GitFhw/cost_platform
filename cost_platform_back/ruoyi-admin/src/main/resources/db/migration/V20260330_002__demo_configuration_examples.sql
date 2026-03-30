-- 面向业务熟悉的示例配置数据
-- 覆盖：场景 -> 费用 -> 变量 -> 规则 -> 阶梯
-- 示例场景导览：
-- 1. 港口装卸作业费：演示货种、内外贸、班次、吨位与阶梯费率的组合配置
-- 2. 协力装卸薪资结算：演示岗位、工种、出勤天数、夜班次数与薪资规则配置
-- 3. 仓储保管费：演示客户等级、面积、计费天数和公式变量的配置方式

-- =========================================================
-- 一、示例字典数据
-- =========================================================

insert into sys_dict_data (dict_code, dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
select 1101, 1, '散货', 'BULK', 'cost_cargo_type', '', 'primary', 'Y', '0', 'flyway', sysdate(), '港口/仓储示例货种'
from dual
where not exists (select 1 from sys_dict_data where dict_type = 'cost_cargo_type' and dict_value = 'BULK');

insert into sys_dict_data (dict_code, dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
select 1102, 2, '煤炭', 'COAL', 'cost_cargo_type', '', 'warning', 'N', '0', 'flyway', sysdate(), '港口作业高频货种示例'
from dual
where not exists (select 1 from sys_dict_data where dict_type = 'cost_cargo_type' and dict_value = 'COAL');

insert into sys_dict_data (dict_code, dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
select 1103, 3, '集装箱', 'CONTAINER', 'cost_cargo_type', '', 'success', 'N', '0', 'flyway', sysdate(), '港口作业货种示例'
from dual
where not exists (select 1 from sys_dict_data where dict_type = 'cost_cargo_type' and dict_value = 'CONTAINER');

insert into sys_dict_data (dict_code, dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
select 1111, 1, '内贸', 'DOMESTIC', 'cost_trade_type', '', 'primary', 'Y', '0', 'flyway', sysdate(), '内外贸示例'
from dual
where not exists (select 1 from sys_dict_data where dict_type = 'cost_trade_type' and dict_value = 'DOMESTIC');

insert into sys_dict_data (dict_code, dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
select 1112, 2, '外贸', 'FOREIGN', 'cost_trade_type', '', 'success', 'N', '0', 'flyway', sysdate(), '内外贸示例'
from dual
where not exists (select 1 from sys_dict_data where dict_type = 'cost_trade_type' and dict_value = 'FOREIGN');

insert into sys_dict_data (dict_code, dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
select 1121, 1, '白班', 'DAY', 'cost_shift_type', '', 'primary', 'Y', '0', 'flyway', sysdate(), '班次示例'
from dual
where not exists (select 1 from sys_dict_data where dict_type = 'cost_shift_type' and dict_value = 'DAY');

insert into sys_dict_data (dict_code, dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
select 1122, 2, '夜班', 'NIGHT', 'cost_shift_type', '', 'warning', 'N', '0', 'flyway', sysdate(), '班次示例'
from dual
where not exists (select 1 from sys_dict_data where dict_type = 'cost_shift_type' and dict_value = 'NIGHT');

insert into sys_dict_data (dict_code, dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
select 1131, 1, '装卸工', 'STEVEDORE', 'cost_job_type', '', 'primary', 'Y', '0', 'flyway', sysdate(), '薪资场景工种示例'
from dual
where not exists (select 1 from sys_dict_data where dict_type = 'cost_job_type' and dict_value = 'STEVEDORE');

insert into sys_dict_data (dict_code, dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
select 1132, 2, '叉车工', 'FORKLIFT', 'cost_job_type', '', 'success', 'N', '0', 'flyway', sysdate(), '薪资场景工种示例'
from dual
where not exists (select 1 from sys_dict_data where dict_type = 'cost_job_type' and dict_value = 'FORKLIFT');

insert into sys_dict_data (dict_code, dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
select 1141, 1, '班组长', 'TEAM_LEADER', 'cost_post_type', '', 'warning', 'N', '0', 'flyway', sysdate(), '岗位示例'
from dual
where not exists (select 1 from sys_dict_data where dict_type = 'cost_post_type' and dict_value = 'TEAM_LEADER');

insert into sys_dict_data (dict_code, dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
select 1142, 2, '普通员工', 'WORKER', 'cost_post_type', '', 'primary', 'Y', '0', 'flyway', sysdate(), '岗位示例'
from dual
where not exists (select 1 from sys_dict_data where dict_type = 'cost_post_type' and dict_value = 'WORKER');

insert into sys_dict_data (dict_code, dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
select 1151, 1, 'A级客户', 'A', 'cost_customer_level', '', 'success', 'Y', '0', 'flyway', sysdate(), '客户等级示例'
from dual
where not exists (select 1 from sys_dict_data where dict_type = 'cost_customer_level' and dict_value = 'A');

insert into sys_dict_data (dict_code, dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
select 1152, 2, 'B级客户', 'B', 'cost_customer_level', '', 'primary', 'N', '0', 'flyway', sysdate(), '客户等级示例'
from dual
where not exists (select 1 from sys_dict_data where dict_type = 'cost_customer_level' and dict_value = 'B');

insert into sys_dict_data (dict_code, dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
select 1153, 3, 'C级客户', 'C', 'cost_customer_level', '', 'danger', 'N', '0', 'flyway', sysdate(), '客户等级示例'
from dual
where not exists (select 1 from sys_dict_data where dict_type = 'cost_customer_level' and dict_value = 'C');

-- =========================================================
-- 二、示例场景
-- =========================================================

insert into cost_scene (scene_id, scene_code, scene_name, business_domain, org_code, scene_type, active_version_id, status, remark, create_by, create_time, update_by, update_time)
select 10001, 'PORT-OPS-001', '港口装卸作业费示例场景', 'PORT', 'ORG_PORT_001', 'CONTRACT', null, '0',
       '用于演示港口行业如何围绕货种、内外贸、班次和吨位配置规则。', 'flyway', sysdate(), 'flyway', sysdate()
from dual
where not exists (select 1 from cost_scene where scene_code = 'PORT-OPS-001');

insert into cost_scene (scene_id, scene_code, scene_name, business_domain, org_code, scene_type, active_version_id, status, remark, create_by, create_time, update_by, update_time)
select 10002, 'SALARY-OUTSOURCE-001', '协力装卸薪资结算示例场景', 'SALARY', 'ORG_HR_001', 'THEME', null, '0',
       '用于演示薪资结算如何围绕岗位、工种、出勤天数和夜班次数维护费用规则。', 'flyway', sysdate(), 'flyway', sysdate()
from dual
where not exists (select 1 from cost_scene where scene_code = 'SALARY-OUTSOURCE-001');

insert into cost_scene (scene_id, scene_code, scene_name, business_domain, org_code, scene_type, active_version_id, status, remark, create_by, create_time, update_by, update_time)
select 10003, 'STORAGE-LEASE-001', '仓储保管费示例场景', 'STORAGE', 'ORG_STORAGE_001', 'PLAN', null, '0',
       '用于演示仓储行业如何通过客户等级、面积和计费天数配置保管费。', 'flyway', sysdate(), 'flyway', sysdate()
from dual
where not exists (select 1 from cost_scene where scene_code = 'STORAGE-LEASE-001');

-- =========================================================
-- 三、示例费用
-- =========================================================

insert into cost_fee_item (fee_id, scene_id, fee_code, fee_name, fee_category, unit_code, factor_summary, scope_description, object_dimension, sort_no, status, remark, create_by, create_time, update_by, update_time)
select 20001, 10001, 'PORT_LOAD_FEE', '装卸作业费', '港杂费', '吨', '受货种、内外贸与吨位影响', '适用于港口单船装卸作业场景', '船舶', 10, '0',
       '港口装卸作业主费用示例。', 'flyway', sysdate(), 'flyway', sysdate()
from dual
where not exists (select 1 from cost_fee_item where scene_id = 10001 and fee_code = 'PORT_LOAD_FEE');

insert into cost_fee_item (fee_id, scene_id, fee_code, fee_name, fee_category, unit_code, factor_summary, scope_description, object_dimension, sort_no, status, remark, create_by, create_time, update_by, update_time)
select 20002, 10001, 'PORT_NIGHT_SURCHARGE', '夜班附加费', '附加费', '航次', '受班次影响', '适用于夜班装卸作业', '船舶', 20, '0',
       '港口夜班附加费示例。', 'flyway', sysdate(), 'flyway', sysdate()
from dual
where not exists (select 1 from cost_fee_item where scene_id = 10001 and fee_code = 'PORT_NIGHT_SURCHARGE');

insert into cost_fee_item (fee_id, scene_id, fee_code, fee_name, fee_category, unit_code, factor_summary, scope_description, object_dimension, sort_no, status, remark, create_by, create_time, update_by, update_time)
select 20011, 10002, 'SALARY_BASE_PAY', '基本工资', '固定薪资', '天', '受岗位和出勤天数影响', '适用于协力装卸薪资月结', '人员', 10, '0',
       '薪资基本工资示例。', 'flyway', sysdate(), 'flyway', sysdate()
from dual
where not exists (select 1 from cost_fee_item where scene_id = 10002 and fee_code = 'SALARY_BASE_PAY');

insert into cost_fee_item (fee_id, scene_id, fee_code, fee_name, fee_category, unit_code, factor_summary, scope_description, object_dimension, sort_no, status, remark, create_by, create_time, update_by, update_time)
select 20012, 10002, 'SALARY_NIGHT_ALLOWANCE', '夜班补贴', '补贴', '班次', '受夜班次数影响', '适用于夜班排班人员', '人员', 20, '0',
       '薪资夜班补贴示例。', 'flyway', sysdate(), 'flyway', sysdate()
from dual
where not exists (select 1 from cost_fee_item where scene_id = 10002 and fee_code = 'SALARY_NIGHT_ALLOWANCE');

insert into cost_fee_item (fee_id, scene_id, fee_code, fee_name, fee_category, unit_code, factor_summary, scope_description, object_dimension, sort_no, status, remark, create_by, create_time, update_by, update_time)
select 20021, 10003, 'STORAGE_KEEP_FEE', '仓储保管费', '仓储费', '平方米*天', '受客户等级、面积和计费天数影响', '适用于仓储保管月结', '库区', 10, '0',
       '仓储保管费示例。', 'flyway', sysdate(), 'flyway', sysdate()
from dual
where not exists (select 1 from cost_fee_item where scene_id = 10003 and fee_code = 'STORAGE_KEEP_FEE');

-- =========================================================
-- 四、示例变量分组
-- =========================================================

insert into cost_variable_group (group_id, scene_id, group_code, group_name, sort_no, status, remark, create_by, create_time, update_by, update_time)
select 30001, 10001, 'PORT_BASE', '基础业务信息', 10, '0', '港口作业公共维度', 'flyway', sysdate(), 'flyway', sysdate()
from dual
where not exists (select 1 from cost_variable_group where scene_id = 10001 and group_code = 'PORT_BASE');

insert into cost_variable_group (group_id, scene_id, group_code, group_name, sort_no, status, remark, create_by, create_time, update_by, update_time)
select 30002, 10001, 'PORT_METRIC', '作业计量信息', 20, '0', '港口作业计量维度', 'flyway', sysdate(), 'flyway', sysdate()
from dual
where not exists (select 1 from cost_variable_group where scene_id = 10001 and group_code = 'PORT_METRIC');

insert into cost_variable_group (group_id, scene_id, group_code, group_name, sort_no, status, remark, create_by, create_time, update_by, update_time)
select 30011, 10002, 'SALARY_POST', '岗位与工种', 10, '0', '薪资人员属性', 'flyway', sysdate(), 'flyway', sysdate()
from dual
where not exists (select 1 from cost_variable_group where scene_id = 10002 and group_code = 'SALARY_POST');

insert into cost_variable_group (group_id, scene_id, group_code, group_name, sort_no, status, remark, create_by, create_time, update_by, update_time)
select 30012, 10002, 'SALARY_ATTEND', '出勤信息', 20, '0', '薪资出勤维度', 'flyway', sysdate(), 'flyway', sysdate()
from dual
where not exists (select 1 from cost_variable_group where scene_id = 10002 and group_code = 'SALARY_ATTEND');

insert into cost_variable_group (group_id, scene_id, group_code, group_name, sort_no, status, remark, create_by, create_time, update_by, update_time)
select 30021, 10003, 'STORAGE_CUSTOMER', '客户与仓储对象', 10, '0', '仓储客户维度', 'flyway', sysdate(), 'flyway', sysdate()
from dual
where not exists (select 1 from cost_variable_group where scene_id = 10003 and group_code = 'STORAGE_CUSTOMER');

insert into cost_variable_group (group_id, scene_id, group_code, group_name, sort_no, status, remark, create_by, create_time, update_by, update_time)
select 30022, 10003, 'STORAGE_METRIC', '计量与公式变量', 20, '0', '仓储计费维度', 'flyway', sysdate(), 'flyway', sysdate()
from dual
where not exists (select 1 from cost_variable_group where scene_id = 10003 and group_code = 'STORAGE_METRIC');

-- =========================================================
-- 五、示例变量
-- =========================================================

insert into cost_variable (variable_id, scene_id, group_id, variable_code, variable_name, variable_type, source_type, dict_type, remote_api, data_path, formula_expr, data_type, default_value, precision_scale, status, sort_no, remark, create_by, create_time, update_by, update_time)
select 40001, 10001, 30001, 'CARGO_TYPE', '货种', 'DICT', 'DICT', 'cost_cargo_type', '', '', null, 'STRING', '', 2, '0', 10,
       '港口场景的货种变量。', 'flyway', sysdate(), 'flyway', sysdate()
from dual
where not exists (select 1 from cost_variable where scene_id = 10001 and variable_code = 'CARGO_TYPE');

insert into cost_variable (variable_id, scene_id, group_id, variable_code, variable_name, variable_type, source_type, dict_type, remote_api, data_path, formula_expr, data_type, default_value, precision_scale, status, sort_no, remark, create_by, create_time, update_by, update_time)
select 40002, 10001, 30001, 'TRADE_TYPE', '内外贸', 'DICT', 'DICT', 'cost_trade_type', '', '', null, 'STRING', '', 2, '0', 20,
       '港口场景的内外贸变量。', 'flyway', sysdate(), 'flyway', sysdate()
from dual
where not exists (select 1 from cost_variable where scene_id = 10001 and variable_code = 'TRADE_TYPE');

insert into cost_variable (variable_id, scene_id, group_id, variable_code, variable_name, variable_type, source_type, dict_type, remote_api, data_path, formula_expr, data_type, default_value, precision_scale, status, sort_no, remark, create_by, create_time, update_by, update_time)
select 40003, 10001, 30001, 'WORK_SHIFT', '作业班次', 'DICT', 'DICT', 'cost_shift_type', '', '', null, 'STRING', '', 2, '0', 30,
       '港口场景的班次变量。', 'flyway', sysdate(), 'flyway', sysdate()
from dual
where not exists (select 1 from cost_variable where scene_id = 10001 and variable_code = 'WORK_SHIFT');

insert into cost_variable (variable_id, scene_id, group_id, variable_code, variable_name, variable_type, source_type, dict_type, remote_api, data_path, formula_expr, data_type, default_value, precision_scale, status, sort_no, remark, create_by, create_time, update_by, update_time)
select 40004, 10001, 30002, 'CARGO_WEIGHT', '装卸吨位', 'NUMBER', 'INPUT', '', '', '', null, 'NUMBER', '0', 4, '0', 40,
       '港口阶梯费率依赖的吨位变量。', 'flyway', sysdate(), 'flyway', sysdate()
from dual
where not exists (select 1 from cost_variable where scene_id = 10001 and variable_code = 'CARGO_WEIGHT');

insert into cost_variable (variable_id, scene_id, group_id, variable_code, variable_name, variable_type, source_type, dict_type, remote_api, data_path, formula_expr, data_type, default_value, precision_scale, status, sort_no, remark, create_by, create_time, update_by, update_time)
select 40011, 10002, 30011, 'POST_TYPE', '岗位', 'DICT', 'DICT', 'cost_post_type', '', '', null, 'STRING', '', 2, '0', 10,
       '薪资场景岗位变量。', 'flyway', sysdate(), 'flyway', sysdate()
from dual
where not exists (select 1 from cost_variable where scene_id = 10002 and variable_code = 'POST_TYPE');

insert into cost_variable (variable_id, scene_id, group_id, variable_code, variable_name, variable_type, source_type, dict_type, remote_api, data_path, formula_expr, data_type, default_value, precision_scale, status, sort_no, remark, create_by, create_time, update_by, update_time)
select 40012, 10002, 30011, 'JOB_TYPE', '工种', 'DICT', 'DICT', 'cost_job_type', '', '', null, 'STRING', '', 2, '0', 20,
       '薪资场景工种变量。', 'flyway', sysdate(), 'flyway', sysdate()
from dual
where not exists (select 1 from cost_variable where scene_id = 10002 and variable_code = 'JOB_TYPE');

insert into cost_variable (variable_id, scene_id, group_id, variable_code, variable_name, variable_type, source_type, dict_type, remote_api, data_path, formula_expr, data_type, default_value, precision_scale, status, sort_no, remark, create_by, create_time, update_by, update_time)
select 40013, 10002, 30012, 'ATTENDANCE_DAYS', '出勤天数', 'NUMBER', 'INPUT', '', '', '', null, 'NUMBER', '0', 2, '0', 30,
       '薪资基本工资计量基础。', 'flyway', sysdate(), 'flyway', sysdate()
from dual
where not exists (select 1 from cost_variable where scene_id = 10002 and variable_code = 'ATTENDANCE_DAYS');

insert into cost_variable (variable_id, scene_id, group_id, variable_code, variable_name, variable_type, source_type, dict_type, remote_api, data_path, formula_expr, data_type, default_value, precision_scale, status, sort_no, remark, create_by, create_time, update_by, update_time)
select 40014, 10002, 30012, 'NIGHT_SHIFT_COUNT', '夜班次数', 'NUMBER', 'INPUT', '', '', '', null, 'NUMBER', '0', 2, '0', 40,
       '薪资夜班补贴的数量基础。', 'flyway', sysdate(), 'flyway', sysdate()
from dual
where not exists (select 1 from cost_variable where scene_id = 10002 and variable_code = 'NIGHT_SHIFT_COUNT');

insert into cost_variable (variable_id, scene_id, group_id, variable_code, variable_name, variable_type, source_type, dict_type, remote_api, data_path, formula_expr, data_type, default_value, precision_scale, status, sort_no, remark, create_by, create_time, update_by, update_time)
select 40021, 10003, 30021, 'CUSTOMER_LEVEL', '客户等级', 'DICT', 'DICT', 'cost_customer_level', '', '', null, 'STRING', '', 2, '0', 10,
       '仓储场景的客户等级变量。', 'flyway', sysdate(), 'flyway', sysdate()
from dual
where not exists (select 1 from cost_variable where scene_id = 10003 and variable_code = 'CUSTOMER_LEVEL');

insert into cost_variable (variable_id, scene_id, group_id, variable_code, variable_name, variable_type, source_type, dict_type, remote_api, data_path, formula_expr, data_type, default_value, precision_scale, status, sort_no, remark, create_by, create_time, update_by, update_time)
select 40022, 10003, 30022, 'OCCUPIED_AREA', '占用面积', 'NUMBER', 'INPUT', '', '', '', null, 'NUMBER', '0', 4, '0', 20,
       '仓储保管费面积变量。', 'flyway', sysdate(), 'flyway', sysdate()
from dual
where not exists (select 1 from cost_variable where scene_id = 10003 and variable_code = 'OCCUPIED_AREA');

insert into cost_variable (variable_id, scene_id, group_id, variable_code, variable_name, variable_type, source_type, dict_type, remote_api, data_path, formula_expr, data_type, default_value, precision_scale, status, sort_no, remark, create_by, create_time, update_by, update_time)
select 40023, 10003, 30022, 'BILL_DAYS', '计费天数', 'NUMBER', 'INPUT', '', '', '', null, 'NUMBER', '0', 2, '0', 30,
       '仓储保管费计费天数变量。', 'flyway', sysdate(), 'flyway', sysdate()
from dual
where not exists (select 1 from cost_variable where scene_id = 10003 and variable_code = 'BILL_DAYS');

insert into cost_variable (variable_id, scene_id, group_id, variable_code, variable_name, variable_type, source_type, dict_type, remote_api, data_path, formula_expr, data_type, default_value, precision_scale, status, sort_no, remark, create_by, create_time, update_by, update_time)
select 40024, 10003, 30022, 'AREA_DAYS', '面积天数', 'FORMULA', 'FORMULA', '', '', '', 'OCCUPIED_AREA * BILL_DAYS', 'NUMBER', '0', 4, '0', 40,
       '仓储保管费的公式变量示例。', 'flyway', sysdate(), 'flyway', sysdate()
from dual
where not exists (select 1 from cost_variable where scene_id = 10003 and variable_code = 'AREA_DAYS');

-- =========================================================
-- 六、费用与变量关系
-- =========================================================

insert into cost_fee_variable_rel (rel_id, scene_id, fee_id, variable_id, relation_type, sort_no, remark, create_by, create_time, update_by, update_time)
select 50001, 10001, 20001, 40001, 'REQUIRED', 10, '装卸作业费依赖货种。', 'flyway', sysdate(), 'flyway', sysdate()
from dual
where not exists (select 1 from cost_fee_variable_rel where fee_id = 20001 and variable_id = 40001 and relation_type = 'REQUIRED');

insert into cost_fee_variable_rel (rel_id, scene_id, fee_id, variable_id, relation_type, sort_no, remark, create_by, create_time, update_by, update_time)
select 50002, 10001, 20001, 40002, 'REQUIRED', 20, '装卸作业费依赖内外贸。', 'flyway', sysdate(), 'flyway', sysdate()
from dual
where not exists (select 1 from cost_fee_variable_rel where fee_id = 20001 and variable_id = 40002 and relation_type = 'REQUIRED');

insert into cost_fee_variable_rel (rel_id, scene_id, fee_id, variable_id, relation_type, sort_no, remark, create_by, create_time, update_by, update_time)
select 50003, 10001, 20001, 40004, 'TIER_BASIS', 30, '装卸作业费阶梯依赖吨位。', 'flyway', sysdate(), 'flyway', sysdate()
from dual
where not exists (select 1 from cost_fee_variable_rel where fee_id = 20001 and variable_id = 40004 and relation_type = 'TIER_BASIS');

insert into cost_fee_variable_rel (rel_id, scene_id, fee_id, variable_id, relation_type, sort_no, remark, create_by, create_time, update_by, update_time)
select 50004, 10001, 20002, 40003, 'REQUIRED', 10, '夜班附加费依赖班次。', 'flyway', sysdate(), 'flyway', sysdate()
from dual
where not exists (select 1 from cost_fee_variable_rel where fee_id = 20002 and variable_id = 40003 and relation_type = 'REQUIRED');

insert into cost_fee_variable_rel (rel_id, scene_id, fee_id, variable_id, relation_type, sort_no, remark, create_by, create_time, update_by, update_time)
select 50011, 10002, 20011, 40011, 'REQUIRED', 10, '基本工资依赖岗位。', 'flyway', sysdate(), 'flyway', sysdate()
from dual
where not exists (select 1 from cost_fee_variable_rel where fee_id = 20011 and variable_id = 40011 and relation_type = 'REQUIRED');

insert into cost_fee_variable_rel (rel_id, scene_id, fee_id, variable_id, relation_type, sort_no, remark, create_by, create_time, update_by, update_time)
select 50012, 10002, 20011, 40013, 'FORMULA_INPUT', 20, '基本工资按出勤天数计价。', 'flyway', sysdate(), 'flyway', sysdate()
from dual
where not exists (select 1 from cost_fee_variable_rel where fee_id = 20011 and variable_id = 40013 and relation_type = 'FORMULA_INPUT');

insert into cost_fee_variable_rel (rel_id, scene_id, fee_id, variable_id, relation_type, sort_no, remark, create_by, create_time, update_by, update_time)
select 50013, 10002, 20012, 40014, 'REQUIRED', 10, '夜班补贴依赖夜班次数。', 'flyway', sysdate(), 'flyway', sysdate()
from dual
where not exists (select 1 from cost_fee_variable_rel where fee_id = 20012 and variable_id = 40014 and relation_type = 'REQUIRED');

insert into cost_fee_variable_rel (rel_id, scene_id, fee_id, variable_id, relation_type, sort_no, remark, create_by, create_time, update_by, update_time)
select 50021, 10003, 20021, 40021, 'REQUIRED', 10, '仓储保管费依赖客户等级。', 'flyway', sysdate(), 'flyway', sysdate()
from dual
where not exists (select 1 from cost_fee_variable_rel where fee_id = 20021 and variable_id = 40021 and relation_type = 'REQUIRED');

insert into cost_fee_variable_rel (rel_id, scene_id, fee_id, variable_id, relation_type, sort_no, remark, create_by, create_time, update_by, update_time)
select 50022, 10003, 20021, 40024, 'FORMULA_INPUT', 20, '仓储保管费按面积天数计价。', 'flyway', sysdate(), 'flyway', sysdate()
from dual
where not exists (select 1 from cost_fee_variable_rel where fee_id = 20021 and variable_id = 40024 and relation_type = 'FORMULA_INPUT');

-- =========================================================
-- 七、示例规则
-- =========================================================

insert into cost_rule (rule_id, scene_id, fee_id, rule_code, rule_name, rule_type, condition_logic, priority, quantity_variable_code, pricing_mode, pricing_json, amount_formula, note_template, status, sort_no, remark, create_by, create_time, update_by, update_time)
select 60001, 10001, 20001, 'PORT_LOAD_TIER_01', '港口散货内贸阶梯费率', 'TIER_RATE', 'AND', 100, 'CARGO_WEIGHT', 'TYPED',
       '{"mode":"TIER_RATE","basis":"CARGO_WEIGHT","unit":"吨","summary":"0-500吨 8.50元/吨；500-2000吨 7.80元/吨；2000吨以上 7.20元/吨"}',
       null, '按货种、内外贸和吨位命中装卸费率。', '0', 10, '港口装卸阶梯费率示例。', 'flyway', sysdate(), 'flyway', sysdate()
from dual
where not exists (select 1 from cost_rule where scene_id = 10001 and rule_code = 'PORT_LOAD_TIER_01');

insert into cost_rule (rule_id, scene_id, fee_id, rule_code, rule_name, rule_type, condition_logic, priority, quantity_variable_code, pricing_mode, pricing_json, amount_formula, note_template, status, sort_no, remark, create_by, create_time, update_by, update_time)
select 60002, 10001, 20002, 'PORT_NIGHT_FIXED_01', '港口夜班附加费', 'FIXED_AMOUNT', 'AND', 90, '', 'TYPED',
       '{"mode":"FIXED_AMOUNT","amount":200.00,"summary":"满足夜班条件时按航次加收200元"}',
       null, '夜班作业自动追加夜班附加费。', '0', 20, '港口夜班附加费示例。', 'flyway', sysdate(), 'flyway', sysdate()
from dual
where not exists (select 1 from cost_rule where scene_id = 10001 and rule_code = 'PORT_NIGHT_FIXED_01');

insert into cost_rule (rule_id, scene_id, fee_id, rule_code, rule_name, rule_type, condition_logic, priority, quantity_variable_code, pricing_mode, pricing_json, amount_formula, note_template, status, sort_no, remark, create_by, create_time, update_by, update_time)
select 60011, 10002, 20011, 'SALARY_BASE_LEADER_01', '班组长基本工资', 'FIXED_RATE', 'AND', 100, 'ATTENDANCE_DAYS', 'TYPED',
       '{"mode":"FIXED_RATE","basis":"ATTENDANCE_DAYS","unit":"天","unitPrice":220.00,"summary":"班组长按220元/天结算"}',
       null, '岗位为班组长时按220元/天计发。', '0', 10, '班组长基本工资示例。', 'flyway', sysdate(), 'flyway', sysdate()
from dual
where not exists (select 1 from cost_rule where scene_id = 10002 and rule_code = 'SALARY_BASE_LEADER_01');

insert into cost_rule (rule_id, scene_id, fee_id, rule_code, rule_name, rule_type, condition_logic, priority, quantity_variable_code, pricing_mode, pricing_json, amount_formula, note_template, status, sort_no, remark, create_by, create_time, update_by, update_time)
select 60012, 10002, 20011, 'SALARY_BASE_WORKER_01', '普通员工基本工资', 'FIXED_RATE', 'AND', 90, 'ATTENDANCE_DAYS', 'TYPED',
       '{"mode":"FIXED_RATE","basis":"ATTENDANCE_DAYS","unit":"天","unitPrice":180.00,"summary":"普通员工按180元/天结算"}',
       null, '岗位为普通员工时按180元/天计发。', '0', 20, '普通员工基本工资示例。', 'flyway', sysdate(), 'flyway', sysdate()
from dual
where not exists (select 1 from cost_rule where scene_id = 10002 and rule_code = 'SALARY_BASE_WORKER_01');

insert into cost_rule (rule_id, scene_id, fee_id, rule_code, rule_name, rule_type, condition_logic, priority, quantity_variable_code, pricing_mode, pricing_json, amount_formula, note_template, status, sort_no, remark, create_by, create_time, update_by, update_time)
select 60013, 10002, 20012, 'SALARY_NIGHT_ALLOWANCE_01', '夜班补贴规则', 'FIXED_RATE', 'AND', 80, 'NIGHT_SHIFT_COUNT', 'TYPED',
       '{"mode":"FIXED_RATE","basis":"NIGHT_SHIFT_COUNT","unit":"次","unitPrice":35.00,"summary":"夜班补贴按35元/次结算"}',
       null, '夜班次数大于0时按次数计发补贴。', '0', 30, '夜班补贴示例。', 'flyway', sysdate(), 'flyway', sysdate()
from dual
where not exists (select 1 from cost_rule where scene_id = 10002 and rule_code = 'SALARY_NIGHT_ALLOWANCE_01');

insert into cost_rule (rule_id, scene_id, fee_id, rule_code, rule_name, rule_type, condition_logic, priority, quantity_variable_code, pricing_mode, pricing_json, amount_formula, note_template, status, sort_no, remark, create_by, create_time, update_by, update_time)
select 60021, 10003, 20021, 'STORAGE_KEEP_RATE_A_B', 'A级/B级客户保管费', 'FIXED_RATE', 'AND', 100, 'AREA_DAYS', 'TYPED',
       '{"mode":"FIXED_RATE","basis":"AREA_DAYS","unit":"平方米*天","unitPrice":1.35,"summary":"A级/B级客户按1.35元/平方米/天结算"}',
       null, 'A级/B级客户适用较优保管费率。', '0', 10, '仓储A级/B级客户保管费示例。', 'flyway', sysdate(), 'flyway', sysdate()
from dual
where not exists (select 1 from cost_rule where scene_id = 10003 and rule_code = 'STORAGE_KEEP_RATE_A_B');

insert into cost_rule (rule_id, scene_id, fee_id, rule_code, rule_name, rule_type, condition_logic, priority, quantity_variable_code, pricing_mode, pricing_json, amount_formula, note_template, status, sort_no, remark, create_by, create_time, update_by, update_time)
select 60022, 10003, 20021, 'STORAGE_KEEP_RATE_C', 'C级客户保管费', 'FIXED_RATE', 'AND', 90, 'AREA_DAYS', 'TYPED',
       '{"mode":"FIXED_RATE","basis":"AREA_DAYS","unit":"平方米*天","unitPrice":1.55,"summary":"C级客户按1.55元/平方米/天结算"}',
       null, 'C级客户适用标准保管费率。', '0', 20, '仓储C级客户保管费示例。', 'flyway', sysdate(), 'flyway', sysdate()
from dual
where not exists (select 1 from cost_rule where scene_id = 10003 and rule_code = 'STORAGE_KEEP_RATE_C');

-- =========================================================
-- 八、示例规则条件
-- =========================================================

insert into cost_rule_condition (condition_id, scene_id, rule_id, group_no, sort_no, variable_code, display_name, operator_code, compare_value, status, remark, create_by, create_time, update_by, update_time)
select 70001, 10001, 60001, 1, 10, 'CARGO_TYPE', '货种', 'IN', 'BULK,COAL', '0', '仅针对散货/煤炭装卸场景。', 'flyway', sysdate(), 'flyway', sysdate()
from dual
where not exists (select 1 from cost_rule_condition where rule_id = 60001 and variable_code = 'CARGO_TYPE' and operator_code = 'IN');

insert into cost_rule_condition (condition_id, scene_id, rule_id, group_no, sort_no, variable_code, display_name, operator_code, compare_value, status, remark, create_by, create_time, update_by, update_time)
select 70002, 10001, 60001, 1, 20, 'TRADE_TYPE', '内外贸', 'EQ', 'DOMESTIC', '0', '港口内贸装卸费率示例。', 'flyway', sysdate(), 'flyway', sysdate()
from dual
where not exists (select 1 from cost_rule_condition where rule_id = 60001 and variable_code = 'TRADE_TYPE' and operator_code = 'EQ');

insert into cost_rule_condition (condition_id, scene_id, rule_id, group_no, sort_no, variable_code, display_name, operator_code, compare_value, status, remark, create_by, create_time, update_by, update_time)
select 70003, 10001, 60002, 1, 10, 'WORK_SHIFT', '作业班次', 'EQ', 'NIGHT', '0', '夜班才加收附加费。', 'flyway', sysdate(), 'flyway', sysdate()
from dual
where not exists (select 1 from cost_rule_condition where rule_id = 60002 and variable_code = 'WORK_SHIFT');

insert into cost_rule_condition (condition_id, scene_id, rule_id, group_no, sort_no, variable_code, display_name, operator_code, compare_value, status, remark, create_by, create_time, update_by, update_time)
select 70011, 10002, 60011, 1, 10, 'POST_TYPE', '岗位', 'EQ', 'TEAM_LEADER', '0', '班组长岗位规则。', 'flyway', sysdate(), 'flyway', sysdate()
from dual
where not exists (select 1 from cost_rule_condition where rule_id = 60011 and variable_code = 'POST_TYPE');

insert into cost_rule_condition (condition_id, scene_id, rule_id, group_no, sort_no, variable_code, display_name, operator_code, compare_value, status, remark, create_by, create_time, update_by, update_time)
select 70012, 10002, 60012, 1, 10, 'POST_TYPE', '岗位', 'EQ', 'WORKER', '0', '普通员工岗位规则。', 'flyway', sysdate(), 'flyway', sysdate()
from dual
where not exists (select 1 from cost_rule_condition where rule_id = 60012 and variable_code = 'POST_TYPE');

insert into cost_rule_condition (condition_id, scene_id, rule_id, group_no, sort_no, variable_code, display_name, operator_code, compare_value, status, remark, create_by, create_time, update_by, update_time)
select 70013, 10002, 60013, 1, 10, 'NIGHT_SHIFT_COUNT', '夜班次数', 'GT', '0', '0', '有夜班次数时才触发补贴。', 'flyway', sysdate(), 'flyway', sysdate()
from dual
where not exists (select 1 from cost_rule_condition where rule_id = 60013 and variable_code = 'NIGHT_SHIFT_COUNT');

insert into cost_rule_condition (condition_id, scene_id, rule_id, group_no, sort_no, variable_code, display_name, operator_code, compare_value, status, remark, create_by, create_time, update_by, update_time)
select 70021, 10003, 60021, 1, 10, 'CUSTOMER_LEVEL', '客户等级', 'IN', 'A,B', '0', 'A级/B级客户共享较优费率。', 'flyway', sysdate(), 'flyway', sysdate()
from dual
where not exists (select 1 from cost_rule_condition where rule_id = 60021 and variable_code = 'CUSTOMER_LEVEL');

insert into cost_rule_condition (condition_id, scene_id, rule_id, group_no, sort_no, variable_code, display_name, operator_code, compare_value, status, remark, create_by, create_time, update_by, update_time)
select 70022, 10003, 60022, 1, 10, 'CUSTOMER_LEVEL', '客户等级', 'EQ', 'C', '0', 'C级客户按标准费率结算。', 'flyway', sysdate(), 'flyway', sysdate()
from dual
where not exists (select 1 from cost_rule_condition where rule_id = 60022 and variable_code = 'CUSTOMER_LEVEL');

-- =========================================================
-- 九、示例阶梯
-- =========================================================

insert into cost_rule_tier (tier_id, scene_id, rule_id, start_value, end_value, rate_value, interval_mode, tier_no, status, remark, create_by, create_time, update_by, update_time)
select 80001, 10001, 60001, 0.0000, 500.0000, 8.500000, 'LEFT_CLOSED_RIGHT_OPEN', 1, '0', '0-500吨', 'flyway', sysdate(), 'flyway', sysdate()
from dual
where not exists (select 1 from cost_rule_tier where rule_id = 60001 and tier_no = 1);

insert into cost_rule_tier (tier_id, scene_id, rule_id, start_value, end_value, rate_value, interval_mode, tier_no, status, remark, create_by, create_time, update_by, update_time)
select 80002, 10001, 60001, 500.0000, 2000.0000, 7.800000, 'LEFT_CLOSED_RIGHT_OPEN', 2, '0', '500-2000吨', 'flyway', sysdate(), 'flyway', sysdate()
from dual
where not exists (select 1 from cost_rule_tier where rule_id = 60001 and tier_no = 2);

insert into cost_rule_tier (tier_id, scene_id, rule_id, start_value, end_value, rate_value, interval_mode, tier_no, status, remark, create_by, create_time, update_by, update_time)
select 80003, 10001, 60001, 2000.0000, null, 7.200000, 'LEFT_CLOSED_RIGHT_OPEN', 3, '0', '2000吨以上', 'flyway', sysdate(), 'flyway', sysdate()
from dual
where not exists (select 1 from cost_rule_tier where rule_id = 60001 and tier_no = 3);

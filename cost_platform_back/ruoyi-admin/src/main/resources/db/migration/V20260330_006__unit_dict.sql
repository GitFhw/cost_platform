-- 线程三补充：计价单位下拉字典

insert into sys_dict_type (dict_id, dict_name, dict_type, status, create_by, create_time, remark)
select 131, '核算-计价单位', 'cost_unit_code', '0', 'admin', sysdate(), '费用中心计价单位下拉字典'
from dual
where not exists (select 1 from sys_dict_type where dict_type = 'cost_unit_code');

insert into sys_dict_data (dict_code, dict_sort, dict_label, dict_value, dict_type, list_class, is_default, status, create_by, create_time, remark)
select 1310, 1, '吨', '吨', 'cost_unit_code', 'primary', 'Y', '0', 'admin', sysdate(), '常用重量单位'
from dual
where not exists (select 1 from sys_dict_data where dict_type = 'cost_unit_code' and dict_value = '吨');

insert into sys_dict_data (dict_code, dict_sort, dict_label, dict_value, dict_type, list_class, is_default, status, create_by, create_time, remark)
select 1311, 2, '天', '天', 'cost_unit_code', 'success', 'N', '0', 'admin', sysdate(), '常用时间单位'
from dual
where not exists (select 1 from sys_dict_data where dict_type = 'cost_unit_code' and dict_value = '天');

insert into sys_dict_data (dict_code, dict_sort, dict_label, dict_value, dict_type, list_class, is_default, status, create_by, create_time, remark)
select 1312, 3, '次', '次', 'cost_unit_code', 'info', 'N', '0', 'admin', sysdate(), '常用次数单位'
from dual
where not exists (select 1 from sys_dict_data where dict_type = 'cost_unit_code' and dict_value = '次');

insert into sys_dict_data (dict_code, dict_sort, dict_label, dict_value, dict_type, list_class, is_default, status, create_by, create_time, remark)
select 1313, 4, '航次', '航次', 'cost_unit_code', 'warning', 'N', '0', 'admin', sysdate(), '航运业务常用单位'
from dual
where not exists (select 1 from sys_dict_data where dict_type = 'cost_unit_code' and dict_value = '航次');

insert into sys_dict_data (dict_code, dict_sort, dict_label, dict_value, dict_type, list_class, is_default, status, create_by, create_time, remark)
select 1314, 5, '人', '人', 'cost_unit_code', 'primary', 'N', '0', 'admin', sysdate(), '人员类单位'
from dual
where not exists (select 1 from sys_dict_data where dict_type = 'cost_unit_code' and dict_value = '人');

insert into sys_dict_data (dict_code, dict_sort, dict_label, dict_value, dict_type, list_class, is_default, status, create_by, create_time, remark)
select 1315, 6, '箱', '箱', 'cost_unit_code', 'success', 'N', '0', 'admin', sysdate(), '箱量单位'
from dual
where not exists (select 1 from sys_dict_data where dict_type = 'cost_unit_code' and dict_value = '箱');

insert into sys_dict_data (dict_code, dict_sort, dict_label, dict_value, dict_type, list_class, is_default, status, create_by, create_time, remark)
select 1316, 7, '元', '元', 'cost_unit_code', 'danger', 'N', '0', 'admin', sysdate(), '金额单位'
from dual
where not exists (select 1 from sys_dict_data where dict_type = 'cost_unit_code' and dict_value = '元');

insert into sys_dict_data (dict_code, dict_sort, dict_label, dict_value, dict_type, list_class, is_default, status, create_by, create_time, remark)
select 1317, 8, '平方米*天', '平方米*天', 'cost_unit_code', 'warning', 'N', '0', 'admin', sysdate(), '仓储复合单位'
from dual
where not exists (select 1 from sys_dict_data where dict_type = 'cost_unit_code' and dict_value = '平方米*天');

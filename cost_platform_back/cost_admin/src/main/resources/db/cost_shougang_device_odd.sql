-- 首钢矿石设备费用基础配置
-- 说明：
-- 1. 设备零工作业接口当前以“作业项目ID + 作业数量”作为核心计费上下文。
-- 2. 已按真实费率落 9 个可自动计价项目。
-- 3. 板车托车费按业务要求先走手工录入，单独建费项。
-- 4. 抓车抓高当前只有项目主数据、未给出费率，本脚本暂不纳入自动计价规则。

insert into sys_dict_type (dict_id, dict_name, dict_type, status, create_by, create_time, remark)
select (select coalesce(max(dict_id), 0) + 1 from sys_dict_type), '核算-首钢设备零工作业项目', 'cost_sg_device_odd_project', '0', 'init', sysdate(), '首钢矿石设备零工作业项目字典'
from dual
where not exists (select 1 from sys_dict_type where dict_type = 'cost_sg_device_odd_project');

insert into sys_dict_data (dict_code, dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
select (select coalesce(max(dict_code), 0) + 1 from sys_dict_data), 10, '铲车零工', '665089828246167685', 'cost_sg_device_odd_project', '', 'primary', 'N', '0', 'init', sysdate(), '105元/小时'
from dual
where not exists (select 1 from sys_dict_data where dict_type = 'cost_sg_device_odd_project' and dict_value = '665089828246167685');

insert into sys_dict_data (dict_code, dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
select (select coalesce(max(dict_code), 0) + 1 from sys_dict_data), 20, '抓车零工', '665090766193205381', 'cost_sg_device_odd_project', '', 'warning', 'N', '0', 'init', sysdate(), '235元/小时'
from dual
where not exists (select 1 from sys_dict_data where dict_type = 'cost_sg_device_odd_project' and dict_value = '665090766193205381');

insert into sys_dict_data (dict_code, dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
select (select coalesce(max(dict_code), 0) + 1 from sys_dict_data), 30, '抽水零工', '14108513240606131', 'cost_sg_device_odd_project', '', 'info', 'N', '0', 'init', sysdate(), '30元/小时'
from dual
where not exists (select 1 from sys_dict_data where dict_type = 'cost_sg_device_odd_project' and dict_value = '14108513240606131');

insert into sys_dict_data (dict_code, dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
select (select coalesce(max(dict_code), 0) + 1 from sys_dict_data), 40, '洒水车零工', '14108514536662454', 'cost_sg_device_odd_project', '', 'success', 'N', '0', 'init', sysdate(), '99元/小时'
from dual
where not exists (select 1 from sys_dict_data where dict_type = 'cost_sg_device_odd_project' and dict_value = '14108514536662454');

insert into sys_dict_data (dict_code, dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
select (select coalesce(max(dict_code), 0) + 1 from sys_dict_data), 50, '清扫铲作业', '14108519400334776', 'cost_sg_device_odd_project', '', 'primary', 'N', '0', 'init', sysdate(), '0.04元/吨'
from dual
where not exists (select 1 from sys_dict_data where dict_type = 'cost_sg_device_odd_project' and dict_value = '14108519400334776');

insert into sys_dict_data (dict_code, dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
select (select coalesce(max(dict_code), 0) + 1 from sys_dict_data), 60, '板车零工', '665090887916101765', 'cost_sg_device_odd_project', '', 'warning', 'N', '0', 'init', sysdate(), '50元/小时'
from dual
where not exists (select 1 from sys_dict_data where dict_type = 'cost_sg_device_odd_project' and dict_value = '665090887916101765');

insert into sys_dict_data (dict_code, dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
select (select coalesce(max(dict_code), 0) + 1 from sys_dict_data), 70, '翻斗零工', '14108523792311734', 'cost_sg_device_odd_project', '', 'danger', 'N', '0', 'init', sysdate(), '90元/小时'
from dual
where not exists (select 1 from sys_dict_data where dict_type = 'cost_sg_device_odd_project' and dict_value = '14108523792311734');

insert into sys_dict_data (dict_code, dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
select (select coalesce(max(dict_code), 0) + 1 from sys_dict_data), 80, '倒运隔离墩', '669101113690545157', 'cost_sg_device_odd_project', '', 'info', 'N', '0', 'init', sysdate(), '5.95元/块'
from dual
where not exists (select 1 from sys_dict_data where dict_type = 'cost_sg_device_odd_project' and dict_value = '669101113690545157');

insert into sys_dict_data (dict_code, dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
select (select coalesce(max(dict_code), 0) + 1 from sys_dict_data), 90, '叉车零工', '14108525500507574', 'cost_sg_device_odd_project', '', 'success', 'N', '0', 'init', sysdate(), '105元/小时'
from dual
where not exists (select 1 from sys_dict_data where dict_type = 'cost_sg_device_odd_project' and dict_value = '14108525500507574');

insert into cost_scene (scene_code, scene_name, business_domain, org_code, scene_type, active_version_id, default_object_dimension, status, remark, create_by, create_time, update_by, update_time)
select 'SHOUGANG-ORE-DEVICE-001',
       '首钢矿石设备费用',
       'MATERIAL',
       'ORG_SHOUGANG_ORE',
       'COMPANY',
       null,
       '协力单位',
       '0',
       '首钢矿石设备零工作业费；自动链路以作业项目ID命中单价，板车托车费先走手工录入。',
       'init',
       sysdate(),
       'init',
       sysdate()
from dual
where not exists (select 1 from cost_scene where scene_code = 'SHOUGANG-ORE-DEVICE-001');

insert into cost_variable_group (scene_id, group_code, group_name, sort_no, status, remark, create_by, create_time, update_by, update_time)
select s.scene_id, 'SG_EQ_BASE', '设备零工基础输入', 10, '0', '接口主链按作业项目ID和作业数量计价', 'init', sysdate(), 'init', sysdate()
from cost_scene s
where s.scene_code = 'SHOUGANG-ORE-DEVICE-001'
  and not exists (select 1 from cost_variable_group where scene_id = s.scene_id and group_code = 'SG_EQ_BASE');

insert into cost_variable_group (scene_id, group_code, group_name, sort_no, status, remark, create_by, create_time, update_by, update_time)
select s.scene_id, 'SG_EQ_MANUAL', '设备零工手工补充', 20, '0', '板车托车费等暂不走接口的补充项', 'init', sysdate(), 'init', sysdate()
from cost_scene s
where s.scene_code = 'SHOUGANG-ORE-DEVICE-001'
  and not exists (select 1 from cost_variable_group where scene_id = s.scene_id and group_code = 'SG_EQ_MANUAL');

insert into cost_variable (scene_id, group_id, variable_code, variable_name, variable_type, source_type, dict_type, data_path, formula_expr, formula_code, data_type, default_value, precision_scale, status, sort_no, remark, create_by, create_time, update_by, update_time)
select s.scene_id, g.group_id, 'DEVICE_ODD_PROJECT_ID', '设备零工作业项目ID', 'DICT', 'DICT', 'cost_sg_device_odd_project', 'DEVICE_ODD_PROJECT_ID', null, '', 'STRING', '', 0, '0', 10, '接口作业项目ID；用于命中设备零工作业费率', 'init', sysdate(), 'init', sysdate()
from cost_scene s
join cost_variable_group g on g.scene_id = s.scene_id and g.group_code = 'SG_EQ_BASE'
where s.scene_code = 'SHOUGANG-ORE-DEVICE-001'
  and not exists (select 1 from cost_variable where scene_id = s.scene_id and variable_code = 'DEVICE_ODD_PROJECT_ID');

insert into cost_variable (scene_id, group_id, variable_code, variable_name, variable_type, source_type, dict_type, data_path, formula_expr, formula_code, data_type, default_value, precision_scale, status, sort_no, remark, create_by, create_time, update_by, update_time)
select s.scene_id, g.group_id, 'DEVICE_ODD_QUANTITY', '设备零工作业数量', 'NUMBER', 'INPUT', '', 'DEVICE_ODD_QUANTITY', null, '', 'NUMBER', '0', 4, '0', 20, '统一作业数量字段；具体计量单位由命中的作业项目决定', 'init', sysdate(), 'init', sysdate()
from cost_scene s
join cost_variable_group g on g.scene_id = s.scene_id and g.group_code = 'SG_EQ_BASE'
where s.scene_code = 'SHOUGANG-ORE-DEVICE-001'
  and not exists (select 1 from cost_variable where scene_id = s.scene_id and variable_code = 'DEVICE_ODD_QUANTITY');

insert into cost_variable (scene_id, group_id, variable_code, variable_name, variable_type, source_type, dict_type, data_path, formula_expr, formula_code, data_type, default_value, precision_scale, status, sort_no, remark, create_by, create_time, update_by, update_time)
select s.scene_id, g.group_id, 'FLATBED_TOW_COUNT', '板车托车次数', 'NUMBER', 'INPUT', '', 'FLATBED_TOW_COUNT', null, '', 'NUMBER', '0', 2, '0', 10, '板车托车费手工录入次数', 'init', sysdate(), 'init', sysdate()
from cost_scene s
join cost_variable_group g on g.scene_id = s.scene_id and g.group_code = 'SG_EQ_MANUAL'
where s.scene_code = 'SHOUGANG-ORE-DEVICE-001'
  and not exists (select 1 from cost_variable where scene_id = s.scene_id and variable_code = 'FLATBED_TOW_COUNT');

insert into cost_fee_item (scene_id, fee_code, fee_name, fee_category, unit_code, factor_summary, scope_description, object_dimension, sort_no, status, remark, create_by, create_time, update_by, update_time)
select s.scene_id, 'SG_DEVICE_ODD_JOB_FEE', '设备零工作业费', '设备费', '作业量', '按作业项目ID命中单价后乘作业数量', '适用于首钢矿石设备零工作业接口项目', '协力单位', 10, '0', '接口主链设备零工作业费；当前已覆盖 9 个有费率项目', 'init', sysdate(), 'init', sysdate()
from cost_scene s
where s.scene_code = 'SHOUGANG-ORE-DEVICE-001'
  and not exists (select 1 from cost_fee_item where scene_id = s.scene_id and fee_code = 'SG_DEVICE_ODD_JOB_FEE');

insert into cost_fee_item (scene_id, fee_code, fee_name, fee_category, unit_code, factor_summary, scope_description, object_dimension, sort_no, status, remark, create_by, create_time, update_by, update_time)
select s.scene_id, 'SG_DEVICE_FLATBED_TOW_FEE', '板车托车费', '设备费', '次', '500元/次 * 板车托车次数', '适用于手工录入板车托车费', '协力单位', 20, '0', '首期作为手工录入补充项，后续如接口确认稳定可并入作业项目计价', 'init', sysdate(), 'init', sysdate()
from cost_scene s
where s.scene_code = 'SHOUGANG-ORE-DEVICE-001'
  and not exists (select 1 from cost_fee_item where scene_id = s.scene_id and fee_code = 'SG_DEVICE_FLATBED_TOW_FEE');

insert into cost_fee_variable_rel (scene_id, fee_id, variable_id, relation_type, sort_no, remark, create_by, create_time, update_by, update_time)
select s.scene_id, f.fee_id, v.variable_id, 'REQUIRED', 10, '设备零工作业费依赖作业项目ID', 'init', sysdate(), 'init', sysdate()
from cost_scene s
join cost_fee_item f on f.scene_id = s.scene_id and f.fee_code = 'SG_DEVICE_ODD_JOB_FEE'
join cost_variable v on v.scene_id = s.scene_id and v.variable_code = 'DEVICE_ODD_PROJECT_ID'
where s.scene_code = 'SHOUGANG-ORE-DEVICE-001'
  and not exists (select 1 from cost_fee_variable_rel where fee_id = f.fee_id and variable_id = v.variable_id and relation_type = 'REQUIRED');

insert into cost_fee_variable_rel (scene_id, fee_id, variable_id, relation_type, sort_no, remark, create_by, create_time, update_by, update_time)
select s.scene_id, f.fee_id, v.variable_id, 'REQUIRED', 20, '设备零工作业费依赖作业数量', 'init', sysdate(), 'init', sysdate()
from cost_scene s
join cost_fee_item f on f.scene_id = s.scene_id and f.fee_code = 'SG_DEVICE_ODD_JOB_FEE'
join cost_variable v on v.scene_id = s.scene_id and v.variable_code = 'DEVICE_ODD_QUANTITY'
where s.scene_code = 'SHOUGANG-ORE-DEVICE-001'
  and not exists (select 1 from cost_fee_variable_rel where fee_id = f.fee_id and variable_id = v.variable_id and relation_type = 'REQUIRED' and sort_no = 20);

insert into cost_fee_variable_rel (scene_id, fee_id, variable_id, relation_type, sort_no, remark, create_by, create_time, update_by, update_time)
select s.scene_id, f.fee_id, v.variable_id, 'REQUIRED', 10, '板车托车费依赖托车次数', 'init', sysdate(), 'init', sysdate()
from cost_scene s
join cost_fee_item f on f.scene_id = s.scene_id and f.fee_code = 'SG_DEVICE_FLATBED_TOW_FEE'
join cost_variable v on v.scene_id = s.scene_id and v.variable_code = 'FLATBED_TOW_COUNT'
where s.scene_code = 'SHOUGANG-ORE-DEVICE-001'
  and not exists (select 1 from cost_fee_variable_rel where fee_id = f.fee_id and variable_id = v.variable_id and relation_type = 'REQUIRED');

insert into cost_rule (scene_id, fee_id, rule_code, rule_name, rule_type, condition_logic, priority, quantity_variable_code, pricing_mode, pricing_json, amount_formula, amount_formula_code, note_template, status, sort_no, remark, create_by, create_time, update_by, update_time)
select s.scene_id, f.fee_id, 'SG_DEVICE_ODD_JOB_GROUPED_01', '设备零工作业项目组合定价规则', 'FIXED_RATE', 'OR', 100, 'DEVICE_ODD_QUANTITY', 'GROUPED',
       '{"mode":"FIXED_RATE","basis":"DEVICE_ODD_QUANTITY","unit":"动态单位","groupPrices":[{"groupNo":1,"label":"铲车零工","projectId":"665089828246167685","unitCode":"小时","rateValue":105},{"groupNo":2,"label":"抓车零工","projectId":"665090766193205381","unitCode":"小时","rateValue":235},{"groupNo":3,"label":"抽水零工","projectId":"14108513240606131","unitCode":"小时","rateValue":30},{"groupNo":4,"label":"洒水车零工","projectId":"14108514536662454","unitCode":"小时","rateValue":99},{"groupNo":5,"label":"清扫铲作业","projectId":"14108519400334776","unitCode":"吨","rateValue":0.04},{"groupNo":6,"label":"板车零工","projectId":"665090887916101765","unitCode":"小时","rateValue":50},{"groupNo":7,"label":"翻斗零工","projectId":"14108523792311734","unitCode":"小时","rateValue":90},{"groupNo":8,"label":"倒运隔离墩","projectId":"669101113690545157","unitCode":"块","rateValue":5.95},{"groupNo":9,"label":"叉车零工","projectId":"14108525500507574","unitCode":"小时","rateValue":105}],"summary":"按设备零工作业项目ID命中单价后乘作业数量"}',
       null, null, '设备零工作业费按项目ID命中单价后乘作业数量', '0', 10, '首钢设备零工作业接口项目定价规则', 'init', sysdate(), 'init', sysdate()
from cost_scene s
join cost_fee_item f on f.scene_id = s.scene_id and f.fee_code = 'SG_DEVICE_ODD_JOB_FEE'
where s.scene_code = 'SHOUGANG-ORE-DEVICE-001'
  and not exists (select 1 from cost_rule where scene_id = s.scene_id and rule_code = 'SG_DEVICE_ODD_JOB_GROUPED_01');

insert into cost_rule (scene_id, fee_id, rule_code, rule_name, rule_type, condition_logic, priority, quantity_variable_code, pricing_mode, pricing_json, amount_formula, amount_formula_code, note_template, status, sort_no, remark, create_by, create_time, update_by, update_time)
select s.scene_id, f.fee_id, 'SG_DEVICE_FLATBED_TOW_RATE_01', '板车托车费固定费率规则', 'FIXED_RATE', 'AND', 100, 'FLATBED_TOW_COUNT', 'TYPED',
       '{"mode":"FIXED_RATE","basis":"FLATBED_TOW_COUNT","unit":"次","rateValue":500,"summary":"按板车托车次数 * 500元/次计价"}',
       null, null, '板车托车费按 500 元/次计价', '0', 20, '首钢板车托车费手工补充规则', 'init', sysdate(), 'init', sysdate()
from cost_scene s
join cost_fee_item f on f.scene_id = s.scene_id and f.fee_code = 'SG_DEVICE_FLATBED_TOW_FEE'
where s.scene_code = 'SHOUGANG-ORE-DEVICE-001'
  and not exists (select 1 from cost_rule where scene_id = s.scene_id and rule_code = 'SG_DEVICE_FLATBED_TOW_RATE_01');

insert into cost_rule_condition (scene_id, rule_id, group_no, sort_no, variable_code, display_name, operator_code, compare_value, status, remark, create_by, create_time, update_by, update_time)
select s.scene_id, r.rule_id, 1, 10, 'DEVICE_ODD_PROJECT_ID', '设备零工作业项目ID', 'EQ', '665089828246167685', '0', '组1：铲车零工', 'init', sysdate(), 'init', sysdate()
from cost_scene s
join cost_rule r on r.scene_id = s.scene_id and r.rule_code = 'SG_DEVICE_ODD_JOB_GROUPED_01'
where s.scene_code = 'SHOUGANG-ORE-DEVICE-001'
  and not exists (select 1 from cost_rule_condition where rule_id = r.rule_id and group_no = 1 and sort_no = 10 and variable_code = 'DEVICE_ODD_PROJECT_ID');

insert into cost_rule_condition (scene_id, rule_id, group_no, sort_no, variable_code, display_name, operator_code, compare_value, status, remark, create_by, create_time, update_by, update_time)
select s.scene_id, r.rule_id, 2, 10, 'DEVICE_ODD_PROJECT_ID', '设备零工作业项目ID', 'EQ', '665090766193205381', '0', '组2：抓车零工', 'init', sysdate(), 'init', sysdate()
from cost_scene s
join cost_rule r on r.scene_id = s.scene_id and r.rule_code = 'SG_DEVICE_ODD_JOB_GROUPED_01'
where s.scene_code = 'SHOUGANG-ORE-DEVICE-001'
  and not exists (select 1 from cost_rule_condition where rule_id = r.rule_id and group_no = 2 and sort_no = 10 and variable_code = 'DEVICE_ODD_PROJECT_ID');

insert into cost_rule_condition (scene_id, rule_id, group_no, sort_no, variable_code, display_name, operator_code, compare_value, status, remark, create_by, create_time, update_by, update_time)
select s.scene_id, r.rule_id, 3, 10, 'DEVICE_ODD_PROJECT_ID', '设备零工作业项目ID', 'EQ', '14108513240606131', '0', '组3：抽水零工', 'init', sysdate(), 'init', sysdate()
from cost_scene s
join cost_rule r on r.scene_id = s.scene_id and r.rule_code = 'SG_DEVICE_ODD_JOB_GROUPED_01'
where s.scene_code = 'SHOUGANG-ORE-DEVICE-001'
  and not exists (select 1 from cost_rule_condition where rule_id = r.rule_id and group_no = 3 and sort_no = 10 and variable_code = 'DEVICE_ODD_PROJECT_ID');

insert into cost_rule_condition (scene_id, rule_id, group_no, sort_no, variable_code, display_name, operator_code, compare_value, status, remark, create_by, create_time, update_by, update_time)
select s.scene_id, r.rule_id, 4, 10, 'DEVICE_ODD_PROJECT_ID', '设备零工作业项目ID', 'EQ', '14108514536662454', '0', '组4：洒水车零工', 'init', sysdate(), 'init', sysdate()
from cost_scene s
join cost_rule r on r.scene_id = s.scene_id and r.rule_code = 'SG_DEVICE_ODD_JOB_GROUPED_01'
where s.scene_code = 'SHOUGANG-ORE-DEVICE-001'
  and not exists (select 1 from cost_rule_condition where rule_id = r.rule_id and group_no = 4 and sort_no = 10 and variable_code = 'DEVICE_ODD_PROJECT_ID');

insert into cost_rule_condition (scene_id, rule_id, group_no, sort_no, variable_code, display_name, operator_code, compare_value, status, remark, create_by, create_time, update_by, update_time)
select s.scene_id, r.rule_id, 5, 10, 'DEVICE_ODD_PROJECT_ID', '设备零工作业项目ID', 'EQ', '14108519400334776', '0', '组5：清扫铲作业', 'init', sysdate(), 'init', sysdate()
from cost_scene s
join cost_rule r on r.scene_id = s.scene_id and r.rule_code = 'SG_DEVICE_ODD_JOB_GROUPED_01'
where s.scene_code = 'SHOUGANG-ORE-DEVICE-001'
  and not exists (select 1 from cost_rule_condition where rule_id = r.rule_id and group_no = 5 and sort_no = 10 and variable_code = 'DEVICE_ODD_PROJECT_ID');

insert into cost_rule_condition (scene_id, rule_id, group_no, sort_no, variable_code, display_name, operator_code, compare_value, status, remark, create_by, create_time, update_by, update_time)
select s.scene_id, r.rule_id, 6, 10, 'DEVICE_ODD_PROJECT_ID', '设备零工作业项目ID', 'EQ', '665090887916101765', '0', '组6：板车零工', 'init', sysdate(), 'init', sysdate()
from cost_scene s
join cost_rule r on r.scene_id = s.scene_id and r.rule_code = 'SG_DEVICE_ODD_JOB_GROUPED_01'
where s.scene_code = 'SHOUGANG-ORE-DEVICE-001'
  and not exists (select 1 from cost_rule_condition where rule_id = r.rule_id and group_no = 6 and sort_no = 10 and variable_code = 'DEVICE_ODD_PROJECT_ID');

insert into cost_rule_condition (scene_id, rule_id, group_no, sort_no, variable_code, display_name, operator_code, compare_value, status, remark, create_by, create_time, update_by, update_time)
select s.scene_id, r.rule_id, 7, 10, 'DEVICE_ODD_PROJECT_ID', '设备零工作业项目ID', 'EQ', '14108523792311734', '0', '组7：翻斗零工', 'init', sysdate(), 'init', sysdate()
from cost_scene s
join cost_rule r on r.scene_id = s.scene_id and r.rule_code = 'SG_DEVICE_ODD_JOB_GROUPED_01'
where s.scene_code = 'SHOUGANG-ORE-DEVICE-001'
  and not exists (select 1 from cost_rule_condition where rule_id = r.rule_id and group_no = 7 and sort_no = 10 and variable_code = 'DEVICE_ODD_PROJECT_ID');

insert into cost_rule_condition (scene_id, rule_id, group_no, sort_no, variable_code, display_name, operator_code, compare_value, status, remark, create_by, create_time, update_by, update_time)
select s.scene_id, r.rule_id, 8, 10, 'DEVICE_ODD_PROJECT_ID', '设备零工作业项目ID', 'EQ', '669101113690545157', '0', '组8：倒运隔离墩', 'init', sysdate(), 'init', sysdate()
from cost_scene s
join cost_rule r on r.scene_id = s.scene_id and r.rule_code = 'SG_DEVICE_ODD_JOB_GROUPED_01'
where s.scene_code = 'SHOUGANG-ORE-DEVICE-001'
  and not exists (select 1 from cost_rule_condition where rule_id = r.rule_id and group_no = 8 and sort_no = 10 and variable_code = 'DEVICE_ODD_PROJECT_ID');

insert into cost_rule_condition (scene_id, rule_id, group_no, sort_no, variable_code, display_name, operator_code, compare_value, status, remark, create_by, create_time, update_by, update_time)
select s.scene_id, r.rule_id, 9, 10, 'DEVICE_ODD_PROJECT_ID', '设备零工作业项目ID', 'EQ', '14108525500507574', '0', '组9：叉车零工', 'init', sysdate(), 'init', sysdate()
from cost_scene s
join cost_rule r on r.scene_id = s.scene_id and r.rule_code = 'SG_DEVICE_ODD_JOB_GROUPED_01'
where s.scene_code = 'SHOUGANG-ORE-DEVICE-001'
  and not exists (select 1 from cost_rule_condition where rule_id = r.rule_id and group_no = 9 and sort_no = 10 and variable_code = 'DEVICE_ODD_PROJECT_ID');

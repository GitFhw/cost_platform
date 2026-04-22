-- 用首钢矿石人力费用真实配置替换历史演示场景
-- 说明：
-- 1. 清理 10001/10002/10003 三套演示场景及其发布、任务、结果台账。
-- 2. 回填“首钢矿石人力费用”真实费用结构。
-- 3. 非费用项（如吞吐量二次分配、资金池预留、界面调整）不落成本平台 fee。
-- 4. 部分上游整理口径继续由第三方组织后传入，例如带缆作业人数、季节性补贴计发份额。
create temporary table if not exists tmp_reset_scene_ids as
select scene_id
from cost_scene
where scene_code in ('PORT-OPS-001', 'SALARY-OUTSOURCE-001', 'STORAGE-LEASE-001', 'SHOUGANG-ORE-HR-001');

create temporary table if not exists tmp_reset_version_ids as
select version_id
from cost_publish_version
where scene_id in (select scene_id from tmp_reset_scene_ids);

create temporary table if not exists tmp_reset_task_ids as
select task_id
from cost_calc_task
where scene_id in (select scene_id from tmp_reset_scene_ids);

create temporary table if not exists tmp_reset_batch_ids as
select batch_id
from cost_calc_input_batch
where scene_id in (select scene_id from tmp_reset_scene_ids);

update cost_scene
set active_version_id = null
where active_version_id in (select version_id from tmp_reset_version_ids);

delete from cost_result_ledger
where task_id in (select task_id from tmp_reset_task_ids)
   or scene_id in (select scene_id from tmp_reset_scene_ids);

delete from cost_result_trace
where scene_id in (select scene_id from tmp_reset_scene_ids)
   or version_id in (select version_id from tmp_reset_version_ids);

delete from cost_calc_task_partition
where task_id in (select task_id from tmp_reset_task_ids);

delete from cost_calc_task_detail
where task_id in (select task_id from tmp_reset_task_ids);

delete from cost_calc_task
where task_id in (select task_id from tmp_reset_task_ids);

delete from cost_calc_input_batch_item
where batch_id in (select batch_id from tmp_reset_batch_ids);

delete from cost_calc_input_batch
where batch_id in (select batch_id from tmp_reset_batch_ids);

delete from cost_simulation_record
where scene_id in (select scene_id from tmp_reset_scene_ids);

delete from cost_alarm_record
where scene_id in (select scene_id from tmp_reset_scene_ids);

delete from cost_audit_log
where scene_id in (select scene_id from tmp_reset_scene_ids);

delete from cost_recalc_order
where scene_id in (select scene_id from tmp_reset_scene_ids);

delete from cost_bill_period
where scene_id in (select scene_id from tmp_reset_scene_ids);

delete from cost_publish_snapshot
where version_id in (select version_id from tmp_reset_version_ids);

delete from cost_publish_version
where version_id in (select version_id from tmp_reset_version_ids);

delete from cost_formula_version
where scene_id in (select scene_id from tmp_reset_scene_ids);

delete from cost_formula
where scene_id in (select scene_id from tmp_reset_scene_ids);

delete from cost_rule_tier
where scene_id in (select scene_id from tmp_reset_scene_ids);

delete from cost_rule_condition
where scene_id in (select scene_id from tmp_reset_scene_ids);

delete from cost_rule
where scene_id in (select scene_id from tmp_reset_scene_ids);

delete from cost_fee_variable_rel
where scene_id in (select scene_id from tmp_reset_scene_ids);

delete from cost_variable
where scene_id in (select scene_id from tmp_reset_scene_ids);

delete from cost_variable_group
where scene_id in (select scene_id from tmp_reset_scene_ids);

delete from cost_fee_item
where scene_id in (select scene_id from tmp_reset_scene_ids);

delete from cost_scene
where scene_id in (select scene_id from tmp_reset_scene_ids);

drop temporary table if exists tmp_reset_batch_ids;
drop temporary table if exists tmp_reset_task_ids;
drop temporary table if exists tmp_reset_version_ids;
drop temporary table if exists tmp_reset_scene_ids;

-- 首钢矿石人力费用真实配置（不含演示场景清理）
-- 来源：cost_init_demo.sql V20260405_018__replace_demo_with_shougang_hr_costs.sql
-- 说明：用于仅初始化基础库后回填真实业务配置
insert into sys_dict_type (dict_id, dict_name, dict_type, status, create_by, create_time, remark)
select (select coalesce(max(dict_id), 0) + 1 from sys_dict_type), '核算-首钢苫盖动作', 'cost_sg_cover_action', '0', 'flyway', sysdate(), '首钢矿石人力费用苫盖动作字典'
from dual
where not exists (select 1 from sys_dict_type where dict_type = 'cost_sg_cover_action');

insert into sys_dict_type (dict_id, dict_name, dict_type, status, create_by, create_time, remark)
select (select coalesce(max(dict_id), 0) + 1 from sys_dict_type), '核算-首钢苫盖货种', 'cost_sg_cover_cargo_type', '0', 'flyway', sysdate(), '首钢矿石人力费用苫盖货种字典'
from dual
where not exists (select 1 from sys_dict_type where dict_type = 'cost_sg_cover_cargo_type');

insert into sys_dict_type (dict_id, dict_name, dict_type, status, create_by, create_time, remark)
select (select coalesce(max(dict_id), 0) + 1 from sys_dict_type), '核算-首钢带缆动作', 'cost_sg_mooring_action', '0', 'flyway', sysdate(), '首钢矿石人力费用带缆动作字典'
from dual
where not exists (select 1 from sys_dict_type where dict_type = 'cost_sg_mooring_action');

insert into sys_dict_data (dict_code, dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
select (select coalesce(max(dict_code), 0) + 1 from sys_dict_data), 1, '苫盖', 'COVER', 'cost_sg_cover_action', '', 'primary', 'Y', '0', 'flyway', sysdate(), '首钢苫盖作业'
from dual
where not exists (select 1 from sys_dict_data where dict_type = 'cost_sg_cover_action' and dict_value = 'COVER');

insert into sys_dict_data (dict_code, dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
select (select coalesce(max(dict_code), 0) + 1 from sys_dict_data), 2, '揭盖', 'UNCOVER', 'cost_sg_cover_action', '', 'warning', 'N', '0', 'flyway', sysdate(), '首钢揭盖作业'
from dual
where not exists (select 1 from sys_dict_data where dict_type = 'cost_sg_cover_action' and dict_value = 'UNCOVER');

insert into sys_dict_data (dict_code, dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
select (select coalesce(max(dict_code), 0) + 1 from sys_dict_data), 1, '焦煤', 'COAL', 'cost_sg_cover_cargo_type', '', 'warning', 'Y', '0', 'flyway', sysdate(), '首钢苫盖焦煤货种'
from dual
where not exists (select 1 from sys_dict_data where dict_type = 'cost_sg_cover_cargo_type' and dict_value = 'COAL');

insert into sys_dict_data (dict_code, dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
select (select coalesce(max(dict_code), 0) + 1 from sys_dict_data), 2, '矿石', 'ORE', 'cost_sg_cover_cargo_type', '', 'success', 'N', '0', 'flyway', sysdate(), '首钢苫盖矿石货种'
from dual
where not exists (select 1 from sys_dict_data where dict_type = 'cost_sg_cover_cargo_type' and dict_value = 'ORE');

insert into sys_dict_data (dict_code, dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
select (select coalesce(max(dict_code), 0) + 1 from sys_dict_data), 1, '带缆', 'MOOR', 'cost_sg_mooring_action', '', 'primary', 'Y', '0', 'flyway', sysdate(), '首钢带缆作业'
from dual
where not exists (select 1 from sys_dict_data where dict_type = 'cost_sg_mooring_action' and dict_value = 'MOOR');

insert into sys_dict_data (dict_code, dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
select (select coalesce(max(dict_code), 0) + 1 from sys_dict_data), 2, '解缆', 'UNMOOR', 'cost_sg_mooring_action', '', 'info', 'N', '0', 'flyway', sysdate(), '首钢解缆作业'
from dual
where not exists (select 1 from sys_dict_data where dict_type = 'cost_sg_mooring_action' and dict_value = 'UNMOOR');

insert into cost_scene (scene_code, scene_name, business_domain, org_code, scene_type, active_version_id, status, remark, create_by, create_time, update_by, update_time)
select 'SHOUGANG-ORE-HR-001',
       '首钢矿石人力费用',
       'MATERIAL',
       'ORG_SHOUGANG_ORE',
       'COMPANY',
       null,
       '0',
       '根据首钢公司人力薪资思维导图整理的矿石场景真实费用配置；首期按第三方系统组织输入字段接入。',
       'flyway',
       sysdate(),
       'flyway',
       sysdate()
from dual
where not exists (select 1 from cost_scene where scene_code = 'SHOUGANG-ORE-HR-001');

insert into cost_variable_group (scene_id, group_code, group_name, sort_no, status, remark, create_by, create_time, update_by, update_time)
select s.scene_id, 'SG_BASE', '基础作业量', 10, '0', '吞吐量、清舱、零工与加班基础计量', 'flyway', sysdate(), 'flyway', sysdate()
from cost_scene s
where s.scene_code = 'SHOUGANG-ORE-HR-001'
  and not exists (select 1 from cost_variable_group where scene_id = s.scene_id and group_code = 'SG_BASE');

insert into cost_variable_group (scene_id, group_code, group_name, sort_no, status, remark, create_by, create_time, update_by, update_time)
select s.scene_id, 'SG_ATTEND', '出勤与班组', 20, '0', '固定类劳务费和值守专班出勤维度', 'flyway', sysdate(), 'flyway', sysdate()
from cost_scene s
where s.scene_code = 'SHOUGANG-ORE-HR-001'
  and not exists (select 1 from cost_variable_group where scene_id = s.scene_id and group_code = 'SG_ATTEND');

insert into cost_variable_group (scene_id, group_code, group_name, sort_no, status, remark, create_by, create_time, update_by, update_time)
select s.scene_id, 'SG_SPECIAL', '专项作业', 30, '0', '苫盖、带缆等专项作业条件与数量', 'flyway', sysdate(), 'flyway', sysdate()
from cost_scene s
where s.scene_code = 'SHOUGANG-ORE-HR-001'
  and not exists (select 1 from cost_variable_group where scene_id = s.scene_id and group_code = 'SG_SPECIAL');

insert into cost_variable_group (scene_id, group_code, group_name, sort_no, status, remark, create_by, create_time, update_by, update_time)
select s.scene_id, 'SG_ALLOWANCE', '补贴与保险', 40, '0', '补贴、单位承担费用与保险类输入', 'flyway', sysdate(), 'flyway', sysdate()
from cost_scene s
where s.scene_code = 'SHOUGANG-ORE-HR-001'
  and not exists (select 1 from cost_variable_group where scene_id = s.scene_id and group_code = 'SG_ALLOWANCE');

insert into cost_formula (scene_id, formula_code, formula_name, formula_desc, business_formula, formula_expr, asset_type, workbench_mode, workbench_pattern, namespace_scope, return_type, status, sort_no, remark, create_by, create_time, update_by, update_time)
select s.scene_id,
       'SG_RULE_FEMALE_SHIFT_LABOR_AMOUNT',
       '女工固定类劳务费金额公式',
       '直接按女工人数和出勤占比计算固定类劳务费金额，不再依赖折算量派生变量',
       '21700 / 6 * 队女工人数 * (女工实际出勤 / 女工应出勤)',
       'round((21700 / 6) * V.FEMALE_TEAM_HEADCOUNT * (V.FEMALE_ACTUAL_ATTENDANCE / max(V.FEMALE_REQUIRED_ATTENDANCE, 1)), 2)',
       'FORMULA',
       'EXPERT',
       'IF_ELSE',
       'V,C,I,F,T',
       'NUMBER',
       '0',
       10,
       '首钢真实费用：女工固定类劳务费直接公式',
       'flyway',
       sysdate(),
       'flyway',
       sysdate()
from cost_scene s
where s.scene_code = 'SHOUGANG-ORE-HR-001'
  and not exists (select 1 from cost_formula where scene_id = s.scene_id and formula_code = 'SG_RULE_FEMALE_SHIFT_LABOR_AMOUNT');

insert into cost_formula (scene_id, formula_code, formula_name, formula_desc, business_formula, formula_expr, asset_type, workbench_mode, workbench_pattern, namespace_scope, return_type, status, sort_no, remark, create_by, create_time, update_by, update_time)
select s.scene_id,
       'SG_RULE_SPECIAL_SHIFT_LABOR_AMOUNT',
       '清料专班库场专班固定类劳务费金额公式',
       '直接按专班人数和出勤占比计算固定类劳务费金额，不再依赖折算量派生变量',
       '253000 / 30 * 专班人数 * (专班实际出勤 / 专班应出勤)',
       'round((253000 / 30) * V.SPECIAL_TEAM_HEADCOUNT * (V.SPECIAL_ACTUAL_ATTENDANCE / max(V.SPECIAL_REQUIRED_ATTENDANCE, 1)), 2)',
       'FORMULA',
       'EXPERT',
       'IF_ELSE',
       'V,C,I,F,T',
       'NUMBER',
       '0',
       20,
       '首钢真实费用：清料专班/库场专班固定类劳务费直接公式',
       'flyway',
       sysdate(),
       'flyway',
       sysdate()
from cost_scene s
where s.scene_code = 'SHOUGANG-ORE-HR-001'
  and not exists (select 1 from cost_formula where scene_id = s.scene_id and formula_code = 'SG_RULE_SPECIAL_SHIFT_LABOR_AMOUNT');

insert into cost_formula (scene_id, formula_code, formula_name, formula_desc, business_formula, formula_expr, asset_type, workbench_mode, workbench_pattern, namespace_scope, return_type, status, sort_no, remark, create_by, create_time, update_by, update_time)
select s.scene_id,
       'SG_RULE_DUTY_SHIFT_LABOR_AMOUNT',
       '值守专班劳务费金额公式',
       '按真实口径从前置费用差额中扣减后，再直接乘以值守专班分摊公式',
       '(吞吐量计件类费用 - 女工固定类劳务费 - 清料/库场专班固定类劳务费 - 清舱劳务费 - 苫盖零工劳务费 - 带缆费 - 零工费) * ((该队应出勤 / 所有队应出勤) * (该队实际出勤 / 该队应出勤))',
       'round((coalesce(F[''SG_THRPT_PIECE_FEE'']?.pricing?.amountValue, 0) - coalesce(F[''SG_FEMALE_SHIFT_LABOR'']?.pricing?.amountValue, 0) - coalesce(F[''SG_SPECIAL_SHIFT_LABOR'']?.pricing?.amountValue, 0) - coalesce(F[''SG_HOLD_CLEANING_LABOR'']?.pricing?.amountValue, 0) - coalesce(F[''SG_COVER_ODD_JOB_LABOR'']?.pricing?.amountValue, 0) - coalesce(F[''SG_MOORING_FEE'']?.pricing?.amountValue, 0) - coalesce(F[''SG_ODD_JOB_FEE'']?.pricing?.amountValue, 0)) * (V.DUTY_TEAM_REQUIRED_ATTENDANCE / max(V.ALL_TEAMS_REQUIRED_ATTENDANCE, 1)) * (V.DUTY_TEAM_ACTUAL_ATTENDANCE / max(V.DUTY_TEAM_REQUIRED_ATTENDANCE, 1)), 2)',
       'FORMULA',
       'EXPERT',
       'IF_ELSE',
       'V,C,I,F,T',
       'NUMBER',
       '0',
       30,
       '首钢真实费用：值守专班劳务费直接公式',
       'flyway',
       sysdate(),
       'flyway',
       sysdate()
from cost_scene s
where s.scene_code = 'SHOUGANG-ORE-HR-001'
  and not exists (select 1 from cost_formula where scene_id = s.scene_id and formula_code = 'SG_RULE_DUTY_SHIFT_LABOR_AMOUNT');

insert into cost_formula (scene_id, formula_code, formula_name, formula_desc, business_formula, formula_expr, asset_type, workbench_mode, workbench_pattern, namespace_scope, return_type, status, sort_no, remark, create_by, create_time, update_by, update_time)
select s.scene_id,
       'SG_RULE_SEASONAL_ALLOWANCE_AMOUNT',
       '季节性补贴金额公式',
       '仅在 12/01/02/03 账期发放，并按第三方整理后的计发份额乘以 500 元',
       '若账期属于 12/01/02/03，则 500 * 季节性补贴计发份额，否则为 0',
       'if((C.billMonth matches ''.*-(12|01|02|03)$''), round(500 * V.SEASONAL_SUBSIDY_EQUIV, 2), 0)',
       'FORMULA',
       'EXPERT',
       'IF_ELSE',
       'V,C,I,F,T',
       'NUMBER',
       '0',
       50,
       '首钢真实费用：季节性补贴；请假扣减口径由第三方预先整理为计发份额',
       'flyway',
       sysdate(),
       'flyway',
       sysdate()
from cost_scene s
where s.scene_code = 'SHOUGANG-ORE-HR-001'
  and not exists (select 1 from cost_formula where scene_id = s.scene_id and formula_code = 'SG_RULE_SEASONAL_ALLOWANCE_AMOUNT');

insert into cost_formula (scene_id, formula_code, formula_name, formula_desc, business_formula, formula_expr, asset_type, workbench_mode, workbench_pattern, namespace_scope, return_type, status, sort_no, remark, create_by, create_time, update_by, update_time)
select s.scene_id,
       'SG_RULE_MANAGEMENT_FEE_AMOUNT',
       '协力单位管理费金额公式',
       '按除保险费之外的所有已核算费用金额汇总后乘以 16.77%',
       '(吞吐量计件类费用 + 女工固定类劳务费 + 清料/库场专班固定类劳务费 + 清舱劳务费 + 苫盖零工劳务费 + 带缆费 + 零工费 + 值守专班劳务费 + 季节性补贴 + 加班费用) * 16.77%',
       'round((coalesce(F[''SG_THRPT_PIECE_FEE'']?.pricing?.amountValue, 0) + coalesce(F[''SG_FEMALE_SHIFT_LABOR'']?.pricing?.amountValue, 0) + coalesce(F[''SG_SPECIAL_SHIFT_LABOR'']?.pricing?.amountValue, 0) + coalesce(F[''SG_HOLD_CLEANING_LABOR'']?.pricing?.amountValue, 0) + coalesce(F[''SG_COVER_ODD_JOB_LABOR'']?.pricing?.amountValue, 0) + coalesce(F[''SG_MOORING_FEE'']?.pricing?.amountValue, 0) + coalesce(F[''SG_ODD_JOB_FEE'']?.pricing?.amountValue, 0) + coalesce(F[''SG_DUTY_SHIFT_LABOR'']?.pricing?.amountValue, 0) + coalesce(F[''SG_SEASONAL_ALLOWANCE'']?.pricing?.amountValue, 0) + coalesce(F[''SG_OVERTIME_FEE'']?.pricing?.amountValue, 0)) * 0.1677, 2)',
       'FORMULA',
       'EXPERT',
       'IF_ELSE',
       'V,C,I,F,T',
       'NUMBER',
       '0',
       60,
       '首钢真实费用：协力单位管理费',
       'flyway',
       sysdate(),
       'flyway',
       sysdate()
from cost_scene s
where s.scene_code = 'SHOUGANG-ORE-HR-001'
  and not exists (select 1 from cost_formula where scene_id = s.scene_id and formula_code = 'SG_RULE_MANAGEMENT_FEE_AMOUNT');

insert into cost_variable (scene_id, group_id, variable_code, variable_name, variable_type, source_type, dict_type, data_path, formula_expr, formula_code, data_type, default_value, precision_scale, status, sort_no, remark, create_by, create_time, update_by, update_time)
select s.scene_id, g.group_id, 'ALLOCATED_THROUGHPUT_TON', '吞吐量二次分配量', 'NUMBER', 'INPUT', '', 'throughput.quantity', null, '', 'NUMBER', '0', 4, '0', 10, '第三方整理后的吞吐量二次分配结果，按吨传入', 'flyway', sysdate(), 'flyway', sysdate()
from cost_scene s
join cost_variable_group g on g.scene_id = s.scene_id and g.group_code = 'SG_BASE'
where s.scene_code = 'SHOUGANG-ORE-HR-001'
  and not exists (select 1 from cost_variable where scene_id = s.scene_id and variable_code = 'ALLOCATED_THROUGHPUT_TON');

insert into cost_variable (scene_id, group_id, variable_code, variable_name, variable_type, source_type, dict_type, data_path, formula_expr, formula_code, data_type, default_value, precision_scale, status, sort_no, remark, create_by, create_time, update_by, update_time)
select s.scene_id, g.group_id, 'HOLD_COUNT', '清舱舱数', 'NUMBER', 'INPUT', '', 'holdCleaning.quantity', null, '', 'NUMBER', '0', 2, '0', 20, '清舱劳务费按舱数计价', 'flyway', sysdate(), 'flyway', sysdate()
from cost_scene s
join cost_variable_group g on g.scene_id = s.scene_id and g.group_code = 'SG_BASE'
where s.scene_code = 'SHOUGANG-ORE-HR-001'
  and not exists (select 1 from cost_variable where scene_id = s.scene_id and variable_code = 'HOLD_COUNT');

insert into cost_variable (scene_id, group_id, variable_code, variable_name, variable_type, source_type, dict_type, data_path, formula_expr, formula_code, data_type, default_value, precision_scale, status, sort_no, remark, create_by, create_time, update_by, update_time)
select s.scene_id, g.group_id, 'ODD_JOB_HOURS', '零工作业时长', 'NUMBER', 'INPUT', '', 'oddWork.quantity', null, '', 'NUMBER', '0', 2, '0', 30, '零工费按小时计价', 'flyway', sysdate(), 'flyway', sysdate()
from cost_scene s
join cost_variable_group g on g.scene_id = s.scene_id and g.group_code = 'SG_BASE'
where s.scene_code = 'SHOUGANG-ORE-HR-001'
  and not exists (select 1 from cost_variable where scene_id = s.scene_id and variable_code = 'ODD_JOB_HOURS');

insert into cost_variable (scene_id, group_id, variable_code, variable_name, variable_type, source_type, dict_type, data_path, formula_expr, formula_code, data_type, default_value, precision_scale, status, sort_no, remark, create_by, create_time, update_by, update_time)
select s.scene_id, g.group_id, 'OVERTIME_DAYS', '加班天数', 'NUMBER', 'INPUT', '', 'overtime.quantity', null, '', 'NUMBER', '0', 2, '0', 40, '加班费用按天数计价', 'flyway', sysdate(), 'flyway', sysdate()
from cost_scene s
join cost_variable_group g on g.scene_id = s.scene_id and g.group_code = 'SG_BASE'
where s.scene_code = 'SHOUGANG-ORE-HR-001'
  and not exists (select 1 from cost_variable where scene_id = s.scene_id and variable_code = 'OVERTIME_DAYS');

insert into cost_variable (scene_id, group_id, variable_code, variable_name, variable_type, source_type, dict_type, data_path, formula_expr, formula_code, data_type, default_value, precision_scale, status, sort_no, remark, create_by, create_time, update_by, update_time)
select s.scene_id, g.group_id, 'FEMALE_TEAM_HEADCOUNT', '队女工人数', 'NUMBER', 'INPUT', '', 'attendance.female.headcount', null, '', 'NUMBER', '0', 2, '0', 10, '女工固定类劳务费基础人数', 'flyway', sysdate(), 'flyway', sysdate()
from cost_scene s
join cost_variable_group g on g.scene_id = s.scene_id and g.group_code = 'SG_ATTEND'
where s.scene_code = 'SHOUGANG-ORE-HR-001'
  and not exists (select 1 from cost_variable where scene_id = s.scene_id and variable_code = 'FEMALE_TEAM_HEADCOUNT');

insert into cost_variable (scene_id, group_id, variable_code, variable_name, variable_type, source_type, dict_type, data_path, formula_expr, formula_code, data_type, default_value, precision_scale, status, sort_no, remark, create_by, create_time, update_by, update_time)
select s.scene_id, g.group_id, 'FEMALE_ACTUAL_ATTENDANCE', '女工实际出勤', 'NUMBER', 'INPUT', '', 'attendance.female.actual', null, '', 'NUMBER', '0', 2, '0', 20, '女工实际出勤量', 'flyway', sysdate(), 'flyway', sysdate()
from cost_scene s
join cost_variable_group g on g.scene_id = s.scene_id and g.group_code = 'SG_ATTEND'
where s.scene_code = 'SHOUGANG-ORE-HR-001'
  and not exists (select 1 from cost_variable where scene_id = s.scene_id and variable_code = 'FEMALE_ACTUAL_ATTENDANCE');

insert into cost_variable (scene_id, group_id, variable_code, variable_name, variable_type, source_type, dict_type, data_path, formula_expr, formula_code, data_type, default_value, precision_scale, status, sort_no, remark, create_by, create_time, update_by, update_time)
select s.scene_id, g.group_id, 'FEMALE_REQUIRED_ATTENDANCE', '女工应出勤', 'NUMBER', 'INPUT', '', 'attendance.female.required', null, '', 'NUMBER', '1', 2, '0', 30, '女工应出勤量', 'flyway', sysdate(), 'flyway', sysdate()
from cost_scene s
join cost_variable_group g on g.scene_id = s.scene_id and g.group_code = 'SG_ATTEND'
where s.scene_code = 'SHOUGANG-ORE-HR-001'
  and not exists (select 1 from cost_variable where scene_id = s.scene_id and variable_code = 'FEMALE_REQUIRED_ATTENDANCE');

insert into cost_variable (scene_id, group_id, variable_code, variable_name, variable_type, source_type, dict_type, data_path, formula_expr, formula_code, data_type, default_value, precision_scale, status, sort_no, remark, create_by, create_time, update_by, update_time)
select s.scene_id, g.group_id, 'SPECIAL_TEAM_HEADCOUNT', '专班人数', 'NUMBER', 'INPUT', '', 'attendance.special.headcount', null, '', 'NUMBER', '0', 2, '0', 40, '清料专班、库场专班人数', 'flyway', sysdate(), 'flyway', sysdate()
from cost_scene s
join cost_variable_group g on g.scene_id = s.scene_id and g.group_code = 'SG_ATTEND'
where s.scene_code = 'SHOUGANG-ORE-HR-001'
  and not exists (select 1 from cost_variable where scene_id = s.scene_id and variable_code = 'SPECIAL_TEAM_HEADCOUNT');

insert into cost_variable (scene_id, group_id, variable_code, variable_name, variable_type, source_type, dict_type, data_path, formula_expr, formula_code, data_type, default_value, precision_scale, status, sort_no, remark, create_by, create_time, update_by, update_time)
select s.scene_id, g.group_id, 'SPECIAL_ACTUAL_ATTENDANCE', '专班实际出勤', 'NUMBER', 'INPUT', '', 'attendance.special.actual', null, '', 'NUMBER', '0', 2, '0', 50, '清料专班、库场专班实际出勤量', 'flyway', sysdate(), 'flyway', sysdate()
from cost_scene s
join cost_variable_group g on g.scene_id = s.scene_id and g.group_code = 'SG_ATTEND'
where s.scene_code = 'SHOUGANG-ORE-HR-001'
  and not exists (select 1 from cost_variable where scene_id = s.scene_id and variable_code = 'SPECIAL_ACTUAL_ATTENDANCE');

insert into cost_variable (scene_id, group_id, variable_code, variable_name, variable_type, source_type, dict_type, data_path, formula_expr, formula_code, data_type, default_value, precision_scale, status, sort_no, remark, create_by, create_time, update_by, update_time)
select s.scene_id, g.group_id, 'SPECIAL_REQUIRED_ATTENDANCE', '专班应出勤', 'NUMBER', 'INPUT', '', 'attendance.special.required', null, '', 'NUMBER', '1', 2, '0', 60, '清料专班、库场专班应出勤量', 'flyway', sysdate(), 'flyway', sysdate()
from cost_scene s
join cost_variable_group g on g.scene_id = s.scene_id and g.group_code = 'SG_ATTEND'
where s.scene_code = 'SHOUGANG-ORE-HR-001'
  and not exists (select 1 from cost_variable where scene_id = s.scene_id and variable_code = 'SPECIAL_REQUIRED_ATTENDANCE');

insert into cost_variable (scene_id, group_id, variable_code, variable_name, variable_type, source_type, dict_type, data_path, formula_expr, formula_code, data_type, default_value, precision_scale, status, sort_no, remark, create_by, create_time, update_by, update_time)
select s.scene_id, g.group_id, 'DUTY_TEAM_ACTUAL_ATTENDANCE', '值守队实际出勤', 'NUMBER', 'INPUT', '', 'attendance.duty.actual', null, '', 'NUMBER', '0', 2, '0', 70, '值守专班该队实际出勤量', 'flyway', sysdate(), 'flyway', sysdate()
from cost_scene s
join cost_variable_group g on g.scene_id = s.scene_id and g.group_code = 'SG_ATTEND'
where s.scene_code = 'SHOUGANG-ORE-HR-001'
  and not exists (select 1 from cost_variable where scene_id = s.scene_id and variable_code = 'DUTY_TEAM_ACTUAL_ATTENDANCE');

insert into cost_variable (scene_id, group_id, variable_code, variable_name, variable_type, source_type, dict_type, data_path, formula_expr, formula_code, data_type, default_value, precision_scale, status, sort_no, remark, create_by, create_time, update_by, update_time)
select s.scene_id, g.group_id, 'DUTY_TEAM_REQUIRED_ATTENDANCE', '值守队应出勤', 'NUMBER', 'INPUT', '', 'attendance.duty.required', null, '', 'NUMBER', '1', 2, '0', 80, '值守专班该队应出勤量', 'flyway', sysdate(), 'flyway', sysdate()
from cost_scene s
join cost_variable_group g on g.scene_id = s.scene_id and g.group_code = 'SG_ATTEND'
where s.scene_code = 'SHOUGANG-ORE-HR-001'
  and not exists (select 1 from cost_variable where scene_id = s.scene_id and variable_code = 'DUTY_TEAM_REQUIRED_ATTENDANCE');

insert into cost_variable (scene_id, group_id, variable_code, variable_name, variable_type, source_type, dict_type, data_path, formula_expr, formula_code, data_type, default_value, precision_scale, status, sort_no, remark, create_by, create_time, update_by, update_time)
select s.scene_id, g.group_id, 'ALL_TEAMS_REQUIRED_ATTENDANCE', '所有队应出勤', 'NUMBER', 'INPUT', '', 'attendance.duty.totalRequired', null, '', 'NUMBER', '1', 2, '0', 90, '值守专班所有队应出勤量', 'flyway', sysdate(), 'flyway', sysdate()
from cost_scene s
join cost_variable_group g on g.scene_id = s.scene_id and g.group_code = 'SG_ATTEND'
where s.scene_code = 'SHOUGANG-ORE-HR-001'
  and not exists (select 1 from cost_variable where scene_id = s.scene_id and variable_code = 'ALL_TEAMS_REQUIRED_ATTENDANCE');

insert into cost_variable (scene_id, group_id, variable_code, variable_name, variable_type, source_type, dict_type, data_path, formula_expr, formula_code, data_type, default_value, precision_scale, status, sort_no, remark, create_by, create_time, update_by, update_time)
select s.scene_id, g.group_id, 'COVER_ACTION', '苫盖动作', 'DICT', 'DICT', 'cost_sg_cover_action', 'cover.action', null, '', 'STRING', '', 2, '0', 10, '苫盖零工劳务费动作条件', 'flyway', sysdate(), 'flyway', sysdate()
from cost_scene s
join cost_variable_group g on g.scene_id = s.scene_id and g.group_code = 'SG_SPECIAL'
where s.scene_code = 'SHOUGANG-ORE-HR-001'
  and not exists (select 1 from cost_variable where scene_id = s.scene_id and variable_code = 'COVER_ACTION');

insert into cost_variable (scene_id, group_id, variable_code, variable_name, variable_type, source_type, dict_type, data_path, formula_expr, formula_code, data_type, default_value, precision_scale, status, sort_no, remark, create_by, create_time, update_by, update_time)
select s.scene_id, g.group_id, 'COVER_CARGO_TYPE', '苫盖货种', 'DICT', 'DICT', 'cost_sg_cover_cargo_type', 'cover.cargoType', null, '', 'STRING', '', 2, '0', 20, '苫盖零工劳务费货种条件', 'flyway', sysdate(), 'flyway', sysdate()
from cost_scene s
join cost_variable_group g on g.scene_id = s.scene_id and g.group_code = 'SG_SPECIAL'
where s.scene_code = 'SHOUGANG-ORE-HR-001'
  and not exists (select 1 from cost_variable where scene_id = s.scene_id and variable_code = 'COVER_CARGO_TYPE');

insert into cost_variable (scene_id, group_id, variable_code, variable_name, variable_type, source_type, dict_type, data_path, formula_expr, formula_code, data_type, default_value, precision_scale, status, sort_no, remark, create_by, create_time, update_by, update_time)
select s.scene_id, g.group_id, 'COVER_WORKLOAD_TON', '苫盖作业量', 'NUMBER', 'INPUT', '', 'cover.quantity', null, '', 'NUMBER', '0', 4, '0', 30, '苫盖零工劳务费按作业量计价', 'flyway', sysdate(), 'flyway', sysdate()
from cost_scene s
join cost_variable_group g on g.scene_id = s.scene_id and g.group_code = 'SG_SPECIAL'
where s.scene_code = 'SHOUGANG-ORE-HR-001'
  and not exists (select 1 from cost_variable where scene_id = s.scene_id and variable_code = 'COVER_WORKLOAD_TON');

insert into cost_variable (scene_id, group_id, variable_code, variable_name, variable_type, source_type, dict_type, data_path, formula_expr, formula_code, data_type, default_value, precision_scale, status, sort_no, remark, create_by, create_time, update_by, update_time)
select s.scene_id, g.group_id, 'MOORING_ACTION', '带缆动作', 'DICT', 'DICT', 'cost_sg_mooring_action', 'mooring.action', null, '', 'STRING', '', 2, '0', 40, '带缆费动作条件', 'flyway', sysdate(), 'flyway', sysdate()
from cost_scene s
join cost_variable_group g on g.scene_id = s.scene_id and g.group_code = 'SG_SPECIAL'
where s.scene_code = 'SHOUGANG-ORE-HR-001'
  and not exists (select 1 from cost_variable where scene_id = s.scene_id and variable_code = 'MOORING_ACTION');

insert into cost_variable (scene_id, group_id, variable_code, variable_name, variable_type, source_type, dict_type, data_path, formula_expr, formula_code, data_type, default_value, precision_scale, status, sort_no, remark, create_by, create_time, update_by, update_time)
select s.scene_id, g.group_id, 'MOORING_HEADCOUNT', '带缆作业人数', 'NUMBER', 'INPUT', '', 'mooring.headcount', null, '', 'NUMBER', '0', 2, '0', 50, '首期由第三方按船型和动作整理后传入作业人数', 'flyway', sysdate(), 'flyway', sysdate()
from cost_scene s
join cost_variable_group g on g.scene_id = s.scene_id and g.group_code = 'SG_SPECIAL'
where s.scene_code = 'SHOUGANG-ORE-HR-001'
  and not exists (select 1 from cost_variable where scene_id = s.scene_id and variable_code = 'MOORING_HEADCOUNT');

insert into cost_variable (scene_id, group_id, variable_code, variable_name, variable_type, source_type, dict_type, data_path, formula_expr, formula_code, data_type, default_value, precision_scale, status, sort_no, remark, create_by, create_time, update_by, update_time)
select s.scene_id, g.group_id, 'SEASONAL_SUBSIDY_EQUIV', '季节性补贴计发份额', 'NUMBER', 'INPUT', '', 'allowance.seasonal.equivalent', null, '', 'NUMBER', '0', 4, '0', 10, '第三方按请假扣减规则整理后的计发份额', 'flyway', sysdate(), 'flyway', sysdate()
from cost_scene s
join cost_variable_group g on g.scene_id = s.scene_id and g.group_code = 'SG_ALLOWANCE'
where s.scene_code = 'SHOUGANG-ORE-HR-001'
  and not exists (select 1 from cost_variable where scene_id = s.scene_id and variable_code = 'SEASONAL_SUBSIDY_EQUIV');

insert into cost_variable (scene_id, group_id, variable_code, variable_name, variable_type, source_type, dict_type, data_path, formula_expr, formula_code, data_type, default_value, precision_scale, status, sort_no, remark, create_by, create_time, update_by, update_time)
select s.scene_id, g.group_id, 'UNIT_BEARING_AMOUNT', '单位承担费用', 'NUMBER', 'INPUT', '', 'insurance.unitBearing.amount', null, '', 'NUMBER', '0', 2, '0', 20, '单位承担部分的费用，支持手工录入或导入', 'flyway', sysdate(), 'flyway', sysdate()
from cost_scene s
join cost_variable_group g on g.scene_id = s.scene_id and g.group_code = 'SG_ALLOWANCE'
where s.scene_code = 'SHOUGANG-ORE-HR-001'
  and not exists (select 1 from cost_variable where scene_id = s.scene_id and variable_code = 'UNIT_BEARING_AMOUNT');

insert into cost_variable (scene_id, group_id, variable_code, variable_name, variable_type, source_type, dict_type, data_path, formula_expr, formula_code, data_type, default_value, precision_scale, status, sort_no, remark, create_by, create_time, update_by, update_time)
select s.scene_id, g.group_id, 'INSURANCE_TAXABLE_AMOUNT', '五险应税额', 'NUMBER', 'INPUT', '', 'insurance.taxable.amount', null, '', 'NUMBER', '0', 2, '0', 30, '五险应税额，支持手工录入', 'flyway', sysdate(), 'flyway', sysdate()
from cost_scene s
join cost_variable_group g on g.scene_id = s.scene_id and g.group_code = 'SG_ALLOWANCE'
where s.scene_code = 'SHOUGANG-ORE-HR-001'
  and not exists (select 1 from cost_variable where scene_id = s.scene_id and variable_code = 'INSURANCE_TAXABLE_AMOUNT');

insert into cost_variable (scene_id, group_id, variable_code, variable_name, variable_type, source_type, dict_type, data_path, formula_expr, formula_code, data_type, default_value, precision_scale, status, sort_no, remark, create_by, create_time, update_by, update_time)
select s.scene_id, g.group_id, 'EMPLOYER_LIABILITY_AMOUNT', '雇主责任险', 'NUMBER', 'INPUT', '', 'insurance.employerLiability.amount', null, '', 'NUMBER', '0', 2, '0', 40, '雇主责任险，支持手工录入', 'flyway', sysdate(), 'flyway', sysdate()
from cost_scene s
join cost_variable_group g on g.scene_id = s.scene_id and g.group_code = 'SG_ALLOWANCE'
where s.scene_code = 'SHOUGANG-ORE-HR-001'
  and not exists (select 1 from cost_variable where scene_id = s.scene_id and variable_code = 'EMPLOYER_LIABILITY_AMOUNT');

insert into cost_fee_item (scene_id, fee_code, fee_name, fee_category, unit_code, factor_summary, scope_description, object_dimension, sort_no, status, remark, create_by, create_time, update_by, update_time)
select s.scene_id, 'SG_THRPT_PIECE_FEE', '吞吐量计件类费用', '劳务费', '吨', '吞吐量二次分配量 * 0.261 元/吨', '适用于矿石场景吞吐量计件费用', '队组', 10, '0', '首钢真实费用：吞吐量计件类费用', 'flyway', sysdate(), 'flyway', sysdate()
from cost_scene s
where s.scene_code = 'SHOUGANG-ORE-HR-001'
  and not exists (select 1 from cost_fee_item where scene_id = s.scene_id and fee_code = 'SG_THRPT_PIECE_FEE');

insert into cost_fee_item (scene_id, fee_code, fee_name, fee_category, unit_code, factor_summary, scope_description, object_dimension, sort_no, status, remark, create_by, create_time, update_by, update_time)
select s.scene_id, 'SG_FEMALE_SHIFT_LABOR', '女工固定类劳务费', '固定劳务费', '元', '21700/6 * 队女工人数 * (女工实际出勤 / 女工应出勤)', '适用于倒班女工固定类劳务费', '队组', 20, '0', '首钢真实费用：女工（倒班，编制 6 人）', 'flyway', sysdate(), 'flyway', sysdate()
from cost_scene s
where s.scene_code = 'SHOUGANG-ORE-HR-001'
  and not exists (select 1 from cost_fee_item where scene_id = s.scene_id and fee_code = 'SG_FEMALE_SHIFT_LABOR');

insert into cost_fee_item (scene_id, fee_code, fee_name, fee_category, unit_code, factor_summary, scope_description, object_dimension, sort_no, status, remark, create_by, create_time, update_by, update_time)
select s.scene_id, 'SG_SPECIAL_SHIFT_LABOR', '清料专班库场专班固定类劳务费', '固定劳务费', '元', '253000/30 * 专班人数 * (专班实际出勤 / 专班应出勤)', '适用于清料专班、库场专班固定类劳务费', '队组', 30, '0', '首钢真实费用：清料专班、库场专班（白班，编制 30 人）', 'flyway', sysdate(), 'flyway', sysdate()
from cost_scene s
where s.scene_code = 'SHOUGANG-ORE-HR-001'
  and not exists (select 1 from cost_fee_item where scene_id = s.scene_id and fee_code = 'SG_SPECIAL_SHIFT_LABOR');

insert into cost_fee_item (scene_id, fee_code, fee_name, fee_category, unit_code, factor_summary, scope_description, object_dimension, sort_no, status, remark, create_by, create_time, update_by, update_time)
select s.scene_id, 'SG_HOLD_CLEANING_LABOR', '清舱劳务费', '劳务费', '舱', '清舱舱数 * 250 元/舱', '适用于值守专班清舱作业', '队组', 40, '0', '首钢真实费用：清舱劳务费', 'flyway', sysdate(), 'flyway', sysdate()
from cost_scene s
where s.scene_code = 'SHOUGANG-ORE-HR-001'
  and not exists (select 1 from cost_fee_item where scene_id = s.scene_id and fee_code = 'SG_HOLD_CLEANING_LABOR');

insert into cost_fee_item (scene_id, fee_code, fee_name, fee_category, unit_code, factor_summary, scope_description, object_dimension, sort_no, status, remark, create_by, create_time, update_by, update_time)
select s.scene_id, 'SG_COVER_ODD_JOB_LABOR', '苫盖零工劳务费', '劳务费', '吨', '按苫盖动作 + 货种组合命中费率，再乘作业量', '适用于苫盖/揭盖焦煤、矿石场景', '队组', 50, '0', '首钢真实费用：苫盖零工劳务费（很少发生）', 'flyway', sysdate(), 'flyway', sysdate()
from cost_scene s
where s.scene_code = 'SHOUGANG-ORE-HR-001'
  and not exists (select 1 from cost_fee_item where scene_id = s.scene_id and fee_code = 'SG_COVER_ODD_JOB_LABOR');

insert into cost_fee_item (scene_id, fee_code, fee_name, fee_category, unit_code, factor_summary, scope_description, object_dimension, sort_no, status, remark, create_by, create_time, update_by, update_time)
select s.scene_id, 'SG_MOORING_FEE', '带缆费', '劳务费', '人次', '按带缆动作命中单价，再乘整理后的作业人数', '适用于带缆/解缆场景；人数由第三方按船型标准整理', '队组', 60, '0', '首钢真实费用：带缆费', 'flyway', sysdate(), 'flyway', sysdate()
from cost_scene s
where s.scene_code = 'SHOUGANG-ORE-HR-001'
  and not exists (select 1 from cost_fee_item where scene_id = s.scene_id and fee_code = 'SG_MOORING_FEE');

insert into cost_fee_item (scene_id, fee_code, fee_name, fee_category, unit_code, factor_summary, scope_description, object_dimension, sort_no, status, remark, create_by, create_time, update_by, update_time)
select s.scene_id, 'SG_ODD_JOB_FEE', '零工费', '劳务费', '小时', '零工作业时长 * 5 元/小时', '适用于零工作业场景', '队组', 70, '0', '首钢真实费用：零工费', 'flyway', sysdate(), 'flyway', sysdate()
from cost_scene s
where s.scene_code = 'SHOUGANG-ORE-HR-001'
  and not exists (select 1 from cost_fee_item where scene_id = s.scene_id and fee_code = 'SG_ODD_JOB_FEE');

insert into cost_fee_item (scene_id, fee_code, fee_name, fee_category, unit_code, factor_summary, scope_description, object_dimension, sort_no, status, remark, create_by, create_time, update_by, update_time)
select s.scene_id, 'SG_DUTY_SHIFT_LABOR', '值守专班劳务费', '劳务费', '元', '按前置费用差额 * ((该队应出勤 / 所有队应出勤) * (该队实际出勤 / 该队应出勤)) 计算', '适用于值守专班倒班场景', '队组', 80, '0', '首钢真实费用：值守专班劳务费', 'flyway', sysdate(), 'flyway', sysdate()
from cost_scene s
where s.scene_code = 'SHOUGANG-ORE-HR-001'
  and not exists (select 1 from cost_fee_item where scene_id = s.scene_id and fee_code = 'SG_DUTY_SHIFT_LABOR');

insert into cost_fee_item (scene_id, fee_code, fee_name, fee_category, unit_code, factor_summary, scope_description, object_dimension, sort_no, status, remark, create_by, create_time, update_by, update_time)
select s.scene_id, 'SG_SEASONAL_ALLOWANCE', '季节性补贴', '补贴', '元', '账期命中 12/01/02/03 后按 500 元 * 计发份额计算', '适用于资金池发放中的季节性补贴', '协力单位', 90, '0', '首钢真实费用：季节性补贴；春节补贴暂不纳入', 'flyway', sysdate(), 'flyway', sysdate()
from cost_scene s
where s.scene_code = 'SHOUGANG-ORE-HR-001'
  and not exists (select 1 from cost_fee_item where scene_id = s.scene_id and fee_code = 'SG_SEASONAL_ALLOWANCE');

insert into cost_fee_item (scene_id, fee_code, fee_name, fee_category, unit_code, factor_summary, scope_description, object_dimension, sort_no, status, remark, create_by, create_time, update_by, update_time)
select s.scene_id, 'SG_OVERTIME_FEE', '加班费用', '补贴', '天', '加班天数 * 240 元', '适用于所有人加班费用', '协力单位', 100, '0', '首钢真实费用：加班费用', 'flyway', sysdate(), 'flyway', sysdate()
from cost_scene s
where s.scene_code = 'SHOUGANG-ORE-HR-001'
  and not exists (select 1 from cost_fee_item where scene_id = s.scene_id and fee_code = 'SG_OVERTIME_FEE');

insert into cost_fee_item (scene_id, fee_code, fee_name, fee_category, unit_code, factor_summary, scope_description, object_dimension, sort_no, status, remark, create_by, create_time, update_by, update_time)
select s.scene_id, 'SG_UNIT_BEARING_FEE', '单位承担部分的费用', '保险类费用', '元', '第三方组织好的直接金额', '适用于单位承担部分费用，支持手工录入/导入', '协力单位', 110, '0', '首钢真实费用：单位承担部分的费用', 'flyway', sysdate(), 'flyway', sysdate()
from cost_scene s
where s.scene_code = 'SHOUGANG-ORE-HR-001'
  and not exists (select 1 from cost_fee_item where scene_id = s.scene_id and fee_code = 'SG_UNIT_BEARING_FEE');

insert into cost_fee_item (scene_id, fee_code, fee_name, fee_category, unit_code, factor_summary, scope_description, object_dimension, sort_no, status, remark, create_by, create_time, update_by, update_time)
select s.scene_id, 'SG_INSURANCE_TAXABLE_FEE', '五险应税额', '保险类费用', '元', '第三方组织好的直接金额', '适用于五险应税额录入', '协力单位', 120, '0', '首钢真实费用：五险应税额', 'flyway', sysdate(), 'flyway', sysdate()
from cost_scene s
where s.scene_code = 'SHOUGANG-ORE-HR-001'
  and not exists (select 1 from cost_fee_item where scene_id = s.scene_id and fee_code = 'SG_INSURANCE_TAXABLE_FEE');

insert into cost_fee_item (scene_id, fee_code, fee_name, fee_category, unit_code, factor_summary, scope_description, object_dimension, sort_no, status, remark, create_by, create_time, update_by, update_time)
select s.scene_id, 'SG_EMPLOYER_LIABILITY_FEE', '雇主责任险', '保险类费用', '元', '第三方组织好的直接金额', '适用于雇主责任险录入', '协力单位', 130, '0', '首钢真实费用：雇主责任险', 'flyway', sysdate(), 'flyway', sysdate()
from cost_scene s
where s.scene_code = 'SHOUGANG-ORE-HR-001'
  and not exists (select 1 from cost_fee_item where scene_id = s.scene_id and fee_code = 'SG_EMPLOYER_LIABILITY_FEE');

insert into cost_fee_item (scene_id, fee_code, fee_name, fee_category, unit_code, factor_summary, scope_description, object_dimension, sort_no, status, remark, create_by, create_time, update_by, update_time)
select s.scene_id, 'SG_MANAGEMENT_FEE', '协力单位管理费', '管理费', '元', '除保险费之外的上述所有费用总和 * 16.77%', '适用于协力单位管理费', '协力单位', 140, '0', '首钢真实费用：协力单位管理费', 'flyway', sysdate(), 'flyway', sysdate()
from cost_scene s
where s.scene_code = 'SHOUGANG-ORE-HR-001'
  and not exists (select 1 from cost_fee_item where scene_id = s.scene_id and fee_code = 'SG_MANAGEMENT_FEE');

insert into cost_fee_variable_rel (scene_id, fee_id, variable_id, relation_type, sort_no, remark, create_by, create_time, update_by, update_time)
select s.scene_id, f.fee_id, v.variable_id, 'REQUIRED', 10, '吞吐量计件类费用依赖吞吐量二次分配量', 'flyway', sysdate(), 'flyway', sysdate()
from cost_scene s
join cost_fee_item f on f.scene_id = s.scene_id and f.fee_code = 'SG_THRPT_PIECE_FEE'
join cost_variable v on v.scene_id = s.scene_id and v.variable_code = 'ALLOCATED_THROUGHPUT_TON'
where s.scene_code = 'SHOUGANG-ORE-HR-001'
  and not exists (select 1 from cost_fee_variable_rel where fee_id = f.fee_id and variable_id = v.variable_id and relation_type = 'REQUIRED');

insert into cost_fee_variable_rel (scene_id, fee_id, variable_id, relation_type, sort_no, remark, create_by, create_time, update_by, update_time)
select s.scene_id, f.fee_id, v.variable_id, 'FORMULA_INPUT', 10, '女工固定类劳务费依赖队女工人数', 'flyway', sysdate(), 'flyway', sysdate()
from cost_scene s
join cost_fee_item f on f.scene_id = s.scene_id and f.fee_code = 'SG_FEMALE_SHIFT_LABOR'
join cost_variable v on v.scene_id = s.scene_id and v.variable_code = 'FEMALE_TEAM_HEADCOUNT'
where s.scene_code = 'SHOUGANG-ORE-HR-001'
  and not exists (select 1 from cost_fee_variable_rel where fee_id = f.fee_id and variable_id = v.variable_id and relation_type = 'FORMULA_INPUT' and sort_no = 10);

insert into cost_fee_variable_rel (scene_id, fee_id, variable_id, relation_type, sort_no, remark, create_by, create_time, update_by, update_time)
select s.scene_id, f.fee_id, v.variable_id, 'FORMULA_INPUT', 20, '女工固定类劳务费依赖女工实际出勤', 'flyway', sysdate(), 'flyway', sysdate()
from cost_scene s
join cost_fee_item f on f.scene_id = s.scene_id and f.fee_code = 'SG_FEMALE_SHIFT_LABOR'
join cost_variable v on v.scene_id = s.scene_id and v.variable_code = 'FEMALE_ACTUAL_ATTENDANCE'
where s.scene_code = 'SHOUGANG-ORE-HR-001'
  and not exists (select 1 from cost_fee_variable_rel where fee_id = f.fee_id and variable_id = v.variable_id and relation_type = 'FORMULA_INPUT' and sort_no = 20);

insert into cost_fee_variable_rel (scene_id, fee_id, variable_id, relation_type, sort_no, remark, create_by, create_time, update_by, update_time)
select s.scene_id, f.fee_id, v.variable_id, 'FORMULA_INPUT', 30, '女工固定类劳务费依赖女工应出勤', 'flyway', sysdate(), 'flyway', sysdate()
from cost_scene s
join cost_fee_item f on f.scene_id = s.scene_id and f.fee_code = 'SG_FEMALE_SHIFT_LABOR'
join cost_variable v on v.scene_id = s.scene_id and v.variable_code = 'FEMALE_REQUIRED_ATTENDANCE'
where s.scene_code = 'SHOUGANG-ORE-HR-001'
  and not exists (select 1 from cost_fee_variable_rel where fee_id = f.fee_id and variable_id = v.variable_id and relation_type = 'FORMULA_INPUT' and sort_no = 30);

insert into cost_fee_variable_rel (scene_id, fee_id, variable_id, relation_type, sort_no, remark, create_by, create_time, update_by, update_time)
select s.scene_id, f.fee_id, v.variable_id, 'FORMULA_INPUT', 10, '清料专班库场专班固定类劳务费依赖专班人数', 'flyway', sysdate(), 'flyway', sysdate()
from cost_scene s
join cost_fee_item f on f.scene_id = s.scene_id and f.fee_code = 'SG_SPECIAL_SHIFT_LABOR'
join cost_variable v on v.scene_id = s.scene_id and v.variable_code = 'SPECIAL_TEAM_HEADCOUNT'
where s.scene_code = 'SHOUGANG-ORE-HR-001'
  and not exists (select 1 from cost_fee_variable_rel where fee_id = f.fee_id and variable_id = v.variable_id and relation_type = 'FORMULA_INPUT' and sort_no = 10);

insert into cost_fee_variable_rel (scene_id, fee_id, variable_id, relation_type, sort_no, remark, create_by, create_time, update_by, update_time)
select s.scene_id, f.fee_id, v.variable_id, 'FORMULA_INPUT', 20, '清料专班库场专班固定类劳务费依赖专班实际出勤', 'flyway', sysdate(), 'flyway', sysdate()
from cost_scene s
join cost_fee_item f on f.scene_id = s.scene_id and f.fee_code = 'SG_SPECIAL_SHIFT_LABOR'
join cost_variable v on v.scene_id = s.scene_id and v.variable_code = 'SPECIAL_ACTUAL_ATTENDANCE'
where s.scene_code = 'SHOUGANG-ORE-HR-001'
  and not exists (select 1 from cost_fee_variable_rel where fee_id = f.fee_id and variable_id = v.variable_id and relation_type = 'FORMULA_INPUT' and sort_no = 20);

insert into cost_fee_variable_rel (scene_id, fee_id, variable_id, relation_type, sort_no, remark, create_by, create_time, update_by, update_time)
select s.scene_id, f.fee_id, v.variable_id, 'FORMULA_INPUT', 30, '清料专班库场专班固定类劳务费依赖专班应出勤', 'flyway', sysdate(), 'flyway', sysdate()
from cost_scene s
join cost_fee_item f on f.scene_id = s.scene_id and f.fee_code = 'SG_SPECIAL_SHIFT_LABOR'
join cost_variable v on v.scene_id = s.scene_id and v.variable_code = 'SPECIAL_REQUIRED_ATTENDANCE'
where s.scene_code = 'SHOUGANG-ORE-HR-001'
  and not exists (select 1 from cost_fee_variable_rel where fee_id = f.fee_id and variable_id = v.variable_id and relation_type = 'FORMULA_INPUT' and sort_no = 30);

insert into cost_fee_variable_rel (scene_id, fee_id, variable_id, relation_type, sort_no, remark, create_by, create_time, update_by, update_time)
select s.scene_id, f.fee_id, v.variable_id, 'REQUIRED', 10, '清舱劳务费依赖清舱数量', 'flyway', sysdate(), 'flyway', sysdate()
from cost_scene s
join cost_fee_item f on f.scene_id = s.scene_id and f.fee_code = 'SG_HOLD_CLEANING_LABOR'
join cost_variable v on v.scene_id = s.scene_id and v.variable_code = 'HOLD_COUNT'
where s.scene_code = 'SHOUGANG-ORE-HR-001'
  and not exists (select 1 from cost_fee_variable_rel where fee_id = f.fee_id and variable_id = v.variable_id and relation_type = 'REQUIRED');

insert into cost_fee_variable_rel (scene_id, fee_id, variable_id, relation_type, sort_no, remark, create_by, create_time, update_by, update_time)
select s.scene_id, f.fee_id, v.variable_id, 'REQUIRED', 10, '苫盖零工劳务费依赖苫盖动作', 'flyway', sysdate(), 'flyway', sysdate()
from cost_scene s
join cost_fee_item f on f.scene_id = s.scene_id and f.fee_code = 'SG_COVER_ODD_JOB_LABOR'
join cost_variable v on v.scene_id = s.scene_id and v.variable_code = 'COVER_ACTION'
where s.scene_code = 'SHOUGANG-ORE-HR-001'
  and not exists (select 1 from cost_fee_variable_rel where fee_id = f.fee_id and variable_id = v.variable_id and relation_type = 'REQUIRED' and sort_no = 10);

insert into cost_fee_variable_rel (scene_id, fee_id, variable_id, relation_type, sort_no, remark, create_by, create_time, update_by, update_time)
select s.scene_id, f.fee_id, v.variable_id, 'REQUIRED', 20, '苫盖零工劳务费依赖货种', 'flyway', sysdate(), 'flyway', sysdate()
from cost_scene s
join cost_fee_item f on f.scene_id = s.scene_id and f.fee_code = 'SG_COVER_ODD_JOB_LABOR'
join cost_variable v on v.scene_id = s.scene_id and v.variable_code = 'COVER_CARGO_TYPE'
where s.scene_code = 'SHOUGANG-ORE-HR-001'
  and not exists (select 1 from cost_fee_variable_rel where fee_id = f.fee_id and variable_id = v.variable_id and relation_type = 'REQUIRED' and sort_no = 20);

insert into cost_fee_variable_rel (scene_id, fee_id, variable_id, relation_type, sort_no, remark, create_by, create_time, update_by, update_time)
select s.scene_id, f.fee_id, v.variable_id, 'REQUIRED', 30, '苫盖零工劳务费按作业量计价', 'flyway', sysdate(), 'flyway', sysdate()
from cost_scene s
join cost_fee_item f on f.scene_id = s.scene_id and f.fee_code = 'SG_COVER_ODD_JOB_LABOR'
join cost_variable v on v.scene_id = s.scene_id and v.variable_code = 'COVER_WORKLOAD_TON'
where s.scene_code = 'SHOUGANG-ORE-HR-001'
  and not exists (select 1 from cost_fee_variable_rel where fee_id = f.fee_id and variable_id = v.variable_id and relation_type = 'REQUIRED' and sort_no = 30);

insert into cost_fee_variable_rel (scene_id, fee_id, variable_id, relation_type, sort_no, remark, create_by, create_time, update_by, update_time)
select s.scene_id, f.fee_id, v.variable_id, 'REQUIRED', 10, '带缆费依赖带缆动作', 'flyway', sysdate(), 'flyway', sysdate()
from cost_scene s
join cost_fee_item f on f.scene_id = s.scene_id and f.fee_code = 'SG_MOORING_FEE'
join cost_variable v on v.scene_id = s.scene_id and v.variable_code = 'MOORING_ACTION'
where s.scene_code = 'SHOUGANG-ORE-HR-001'
  and not exists (select 1 from cost_fee_variable_rel where fee_id = f.fee_id and variable_id = v.variable_id and relation_type = 'REQUIRED' and sort_no = 10);

insert into cost_fee_variable_rel (scene_id, fee_id, variable_id, relation_type, sort_no, remark, create_by, create_time, update_by, update_time)
select s.scene_id, f.fee_id, v.variable_id, 'REQUIRED', 20, '带缆费依赖第三方整理后的作业人数', 'flyway', sysdate(), 'flyway', sysdate()
from cost_scene s
join cost_fee_item f on f.scene_id = s.scene_id and f.fee_code = 'SG_MOORING_FEE'
join cost_variable v on v.scene_id = s.scene_id and v.variable_code = 'MOORING_HEADCOUNT'
where s.scene_code = 'SHOUGANG-ORE-HR-001'
  and not exists (select 1 from cost_fee_variable_rel where fee_id = f.fee_id and variable_id = v.variable_id and relation_type = 'REQUIRED' and sort_no = 20);

insert into cost_fee_variable_rel (scene_id, fee_id, variable_id, relation_type, sort_no, remark, create_by, create_time, update_by, update_time)
select s.scene_id, f.fee_id, v.variable_id, 'REQUIRED', 10, '零工费按零工工时计价', 'flyway', sysdate(), 'flyway', sysdate()
from cost_scene s
join cost_fee_item f on f.scene_id = s.scene_id and f.fee_code = 'SG_ODD_JOB_FEE'
join cost_variable v on v.scene_id = s.scene_id and v.variable_code = 'ODD_JOB_HOURS'
where s.scene_code = 'SHOUGANG-ORE-HR-001'
  and not exists (select 1 from cost_fee_variable_rel where fee_id = f.fee_id and variable_id = v.variable_id and relation_type = 'REQUIRED');

insert into cost_fee_variable_rel (scene_id, fee_id, variable_id, relation_type, sort_no, remark, create_by, create_time, update_by, update_time)
select s.scene_id, f.fee_id, v.variable_id, 'FORMULA_INPUT', 10, '值守专班劳务费依赖值守队实际出勤', 'flyway', sysdate(), 'flyway', sysdate()
from cost_scene s
join cost_fee_item f on f.scene_id = s.scene_id and f.fee_code = 'SG_DUTY_SHIFT_LABOR'
join cost_variable v on v.scene_id = s.scene_id and v.variable_code = 'DUTY_TEAM_ACTUAL_ATTENDANCE'
where s.scene_code = 'SHOUGANG-ORE-HR-001'
  and not exists (select 1 from cost_fee_variable_rel where fee_id = f.fee_id and variable_id = v.variable_id and relation_type = 'FORMULA_INPUT' and sort_no = 10);

insert into cost_fee_variable_rel (scene_id, fee_id, variable_id, relation_type, sort_no, remark, create_by, create_time, update_by, update_time)
select s.scene_id, f.fee_id, v.variable_id, 'FORMULA_INPUT', 20, '值守专班劳务费依赖值守队应出勤', 'flyway', sysdate(), 'flyway', sysdate()
from cost_scene s
join cost_fee_item f on f.scene_id = s.scene_id and f.fee_code = 'SG_DUTY_SHIFT_LABOR'
join cost_variable v on v.scene_id = s.scene_id and v.variable_code = 'DUTY_TEAM_REQUIRED_ATTENDANCE'
where s.scene_code = 'SHOUGANG-ORE-HR-001'
  and not exists (select 1 from cost_fee_variable_rel where fee_id = f.fee_id and variable_id = v.variable_id and relation_type = 'FORMULA_INPUT' and sort_no = 20);

insert into cost_fee_variable_rel (scene_id, fee_id, variable_id, relation_type, sort_no, remark, create_by, create_time, update_by, update_time)
select s.scene_id, f.fee_id, v.variable_id, 'FORMULA_INPUT', 30, '值守专班劳务费依赖所有队应出勤', 'flyway', sysdate(), 'flyway', sysdate()
from cost_scene s
join cost_fee_item f on f.scene_id = s.scene_id and f.fee_code = 'SG_DUTY_SHIFT_LABOR'
join cost_variable v on v.scene_id = s.scene_id and v.variable_code = 'ALL_TEAMS_REQUIRED_ATTENDANCE'
where s.scene_code = 'SHOUGANG-ORE-HR-001'
  and not exists (select 1 from cost_fee_variable_rel where fee_id = f.fee_id and variable_id = v.variable_id and relation_type = 'FORMULA_INPUT' and sort_no = 30);

insert into cost_fee_variable_rel (scene_id, fee_id, variable_id, relation_type, sort_no, remark, create_by, create_time, update_by, update_time)
select s.scene_id, f.fee_id, v.variable_id, 'FORMULA_INPUT', 10, '季节性补贴依赖第三方整理后的计发份额', 'flyway', sysdate(), 'flyway', sysdate()
from cost_scene s
join cost_fee_item f on f.scene_id = s.scene_id and f.fee_code = 'SG_SEASONAL_ALLOWANCE'
join cost_variable v on v.scene_id = s.scene_id and v.variable_code = 'SEASONAL_SUBSIDY_EQUIV'
where s.scene_code = 'SHOUGANG-ORE-HR-001'
  and not exists (select 1 from cost_fee_variable_rel where fee_id = f.fee_id and variable_id = v.variable_id and relation_type = 'FORMULA_INPUT');

insert into cost_fee_variable_rel (scene_id, fee_id, variable_id, relation_type, sort_no, remark, create_by, create_time, update_by, update_time)
select s.scene_id, f.fee_id, v.variable_id, 'REQUIRED', 10, '加班费用按加班天数计价', 'flyway', sysdate(), 'flyway', sysdate()
from cost_scene s
join cost_fee_item f on f.scene_id = s.scene_id and f.fee_code = 'SG_OVERTIME_FEE'
join cost_variable v on v.scene_id = s.scene_id and v.variable_code = 'OVERTIME_DAYS'
where s.scene_code = 'SHOUGANG-ORE-HR-001'
  and not exists (select 1 from cost_fee_variable_rel where fee_id = f.fee_id and variable_id = v.variable_id and relation_type = 'REQUIRED');

insert into cost_fee_variable_rel (scene_id, fee_id, variable_id, relation_type, sort_no, remark, create_by, create_time, update_by, update_time)
select s.scene_id, f.fee_id, v.variable_id, 'REQUIRED', 10, '单位承担费用按输入金额直传', 'flyway', sysdate(), 'flyway', sysdate()
from cost_scene s
join cost_fee_item f on f.scene_id = s.scene_id and f.fee_code = 'SG_UNIT_BEARING_FEE'
join cost_variable v on v.scene_id = s.scene_id and v.variable_code = 'UNIT_BEARING_AMOUNT'
where s.scene_code = 'SHOUGANG-ORE-HR-001'
  and not exists (select 1 from cost_fee_variable_rel where fee_id = f.fee_id and variable_id = v.variable_id and relation_type = 'REQUIRED');

insert into cost_fee_variable_rel (scene_id, fee_id, variable_id, relation_type, sort_no, remark, create_by, create_time, update_by, update_time)
select s.scene_id, f.fee_id, v.variable_id, 'REQUIRED', 10, '五险应税额按输入金额直传', 'flyway', sysdate(), 'flyway', sysdate()
from cost_scene s
join cost_fee_item f on f.scene_id = s.scene_id and f.fee_code = 'SG_INSURANCE_TAXABLE_FEE'
join cost_variable v on v.scene_id = s.scene_id and v.variable_code = 'INSURANCE_TAXABLE_AMOUNT'
where s.scene_code = 'SHOUGANG-ORE-HR-001'
  and not exists (select 1 from cost_fee_variable_rel where fee_id = f.fee_id and variable_id = v.variable_id and relation_type = 'REQUIRED');

insert into cost_fee_variable_rel (scene_id, fee_id, variable_id, relation_type, sort_no, remark, create_by, create_time, update_by, update_time)
select s.scene_id, f.fee_id, v.variable_id, 'REQUIRED', 10, '雇主责任险按输入金额直传', 'flyway', sysdate(), 'flyway', sysdate()
from cost_scene s
join cost_fee_item f on f.scene_id = s.scene_id and f.fee_code = 'SG_EMPLOYER_LIABILITY_FEE'
join cost_variable v on v.scene_id = s.scene_id and v.variable_code = 'EMPLOYER_LIABILITY_AMOUNT'
where s.scene_code = 'SHOUGANG-ORE-HR-001'
  and not exists (select 1 from cost_fee_variable_rel where fee_id = f.fee_id and variable_id = v.variable_id and relation_type = 'REQUIRED');

insert into cost_rule (scene_id, fee_id, rule_code, rule_name, rule_type, condition_logic, priority, quantity_variable_code, pricing_mode, pricing_json, amount_formula, amount_formula_code, note_template, status, sort_no, remark, create_by, create_time, update_by, update_time)
select s.scene_id, f.fee_id, 'SG_THRPT_PIECE_RATE_01', '吞吐量计件类费用规则', 'FIXED_RATE', 'AND', 100, 'ALLOCATED_THROUGHPUT_TON', 'TYPED',
       '{"mode":"FIXED_RATE","basis":"ALLOCATED_THROUGHPUT_TON","unit":"吨","rateValue":0.261,"summary":"按吞吐量二次分配量 * 0.261 元/吨"}',
       null, null, '吞吐量二次分配量按 0.261 元/吨计价', '0', 10, '首钢真实费用：吞吐量计件类费用', 'flyway', sysdate(), 'flyway', sysdate()
from cost_scene s
join cost_fee_item f on f.scene_id = s.scene_id and f.fee_code = 'SG_THRPT_PIECE_FEE'
where s.scene_code = 'SHOUGANG-ORE-HR-001'
  and not exists (select 1 from cost_rule where scene_id = s.scene_id and rule_code = 'SG_THRPT_PIECE_RATE_01');

insert into cost_rule (scene_id, fee_id, rule_code, rule_name, rule_type, condition_logic, priority, quantity_variable_code, pricing_mode, pricing_json, amount_formula, amount_formula_code, note_template, status, sort_no, remark, create_by, create_time, update_by, update_time)
select s.scene_id, f.fee_id, 'SG_FEMALE_SHIFT_FORMULA_01', '女工固定类劳务费公式规则', 'FORMULA', 'AND', 100, '', 'TYPED',
       null,
       '21700 / 6 * 队女工人数 * (女工实际出勤 / 女工应出勤)', 'SG_RULE_FEMALE_SHIFT_LABOR_AMOUNT', '女工固定类劳务费按人数和出勤占比直接计算', '0', 20, '首钢真实费用：女工（倒班，编制 6 人）', 'flyway', sysdate(), 'flyway', sysdate()
from cost_scene s
join cost_fee_item f on f.scene_id = s.scene_id and f.fee_code = 'SG_FEMALE_SHIFT_LABOR'
where s.scene_code = 'SHOUGANG-ORE-HR-001'
  and not exists (select 1 from cost_rule where scene_id = s.scene_id and rule_code = 'SG_FEMALE_SHIFT_FORMULA_01');

insert into cost_rule (scene_id, fee_id, rule_code, rule_name, rule_type, condition_logic, priority, quantity_variable_code, pricing_mode, pricing_json, amount_formula, amount_formula_code, note_template, status, sort_no, remark, create_by, create_time, update_by, update_time)
select s.scene_id, f.fee_id, 'SG_SPECIAL_SHIFT_FORMULA_01', '清料专班库场专班固定类劳务费公式规则', 'FORMULA', 'AND', 100, '', 'TYPED',
       null,
       '253000 / 30 * 专班人数 * (专班实际出勤 / 专班应出勤)', 'SG_RULE_SPECIAL_SHIFT_LABOR_AMOUNT', '清料专班库场专班固定类劳务费按人数和出勤占比直接计算', '0', 30, '首钢真实费用：清料专班、库场专班（白班，编制 30 人）', 'flyway', sysdate(), 'flyway', sysdate()
from cost_scene s
join cost_fee_item f on f.scene_id = s.scene_id and f.fee_code = 'SG_SPECIAL_SHIFT_LABOR'
where s.scene_code = 'SHOUGANG-ORE-HR-001'
  and not exists (select 1 from cost_rule where scene_id = s.scene_id and rule_code = 'SG_SPECIAL_SHIFT_FORMULA_01');

insert into cost_rule (scene_id, fee_id, rule_code, rule_name, rule_type, condition_logic, priority, quantity_variable_code, pricing_mode, pricing_json, amount_formula, amount_formula_code, note_template, status, sort_no, remark, create_by, create_time, update_by, update_time)
select s.scene_id, f.fee_id, 'SG_HOLD_CLEANING_RATE_01', '清舱劳务费规则', 'FIXED_RATE', 'AND', 100, 'HOLD_COUNT', 'TYPED',
       '{"mode":"FIXED_RATE","basis":"HOLD_COUNT","unit":"舱","rateValue":250,"summary":"按清舱数量 * 250 元/舱计价"}',
       null, null, '清舱劳务费按 250 元/舱计价', '0', 40, '首钢真实费用：清舱劳务费', 'flyway', sysdate(), 'flyway', sysdate()
from cost_scene s
join cost_fee_item f on f.scene_id = s.scene_id and f.fee_code = 'SG_HOLD_CLEANING_LABOR'
where s.scene_code = 'SHOUGANG-ORE-HR-001'
  and not exists (select 1 from cost_rule where scene_id = s.scene_id and rule_code = 'SG_HOLD_CLEANING_RATE_01');

insert into cost_rule (scene_id, fee_id, rule_code, rule_name, rule_type, condition_logic, priority, quantity_variable_code, pricing_mode, pricing_json, amount_formula, amount_formula_code, note_template, status, sort_no, remark, create_by, create_time, update_by, update_time)
select s.scene_id, f.fee_id, 'SG_COVER_ODD_JOB_GROUPED_01', '苫盖零工劳务费组合定价规则', 'FIXED_RATE', 'OR', 100, 'COVER_WORKLOAD_TON', 'GROUPED',
       '{"mode":"FIXED_RATE","basis":"COVER_WORKLOAD_TON","unit":"吨","groupPrices":[{"groupNo":1,"label":"苫焦煤垛","rateValue":0.016},{"groupNo":2,"label":"揭焦煤垛","rateValue":0.012},{"groupNo":3,"label":"苫矿石垛","rateValue":0.013},{"groupNo":4,"label":"揭矿石垛","rateValue":0.009}],"summary":"按苫盖/揭盖动作与货种命中单价后乘作业量"}',
       null, null, '按苫盖/揭盖动作和货种命中单价后乘作业量', '0', 50, '首钢真实费用：苫盖零工劳务费（很少发生）', 'flyway', sysdate(), 'flyway', sysdate()
from cost_scene s
join cost_fee_item f on f.scene_id = s.scene_id and f.fee_code = 'SG_COVER_ODD_JOB_LABOR'
where s.scene_code = 'SHOUGANG-ORE-HR-001'
  and not exists (select 1 from cost_rule where scene_id = s.scene_id and rule_code = 'SG_COVER_ODD_JOB_GROUPED_01');

insert into cost_rule (scene_id, fee_id, rule_code, rule_name, rule_type, condition_logic, priority, quantity_variable_code, pricing_mode, pricing_json, amount_formula, amount_formula_code, note_template, status, sort_no, remark, create_by, create_time, update_by, update_time)
select s.scene_id, f.fee_id, 'SG_MOORING_GROUPED_01', '带缆费组合定价规则', 'FIXED_RATE', 'OR', 100, 'MOORING_HEADCOUNT', 'GROUPED',
       '{"mode":"FIXED_RATE","basis":"MOORING_HEADCOUNT","unit":"人次","groupPrices":[{"groupNo":1,"label":"带缆","rateValue":16},{"groupNo":2,"label":"解缆","rateValue":10}],"summary":"按带/解缆动作命中单价后乘第三方整理后的作业人数"}',
       null, null, '按带/解缆动作命中单价后乘第三方整理后的作业人数', '0', 60, '首钢真实费用：带缆费', 'flyway', sysdate(), 'flyway', sysdate()
from cost_scene s
join cost_fee_item f on f.scene_id = s.scene_id and f.fee_code = 'SG_MOORING_FEE'
where s.scene_code = 'SHOUGANG-ORE-HR-001'
  and not exists (select 1 from cost_rule where scene_id = s.scene_id and rule_code = 'SG_MOORING_GROUPED_01');

insert into cost_rule (scene_id, fee_id, rule_code, rule_name, rule_type, condition_logic, priority, quantity_variable_code, pricing_mode, pricing_json, amount_formula, amount_formula_code, note_template, status, sort_no, remark, create_by, create_time, update_by, update_time)
select s.scene_id, f.fee_id, 'SG_ODD_JOB_RATE_01', '零工费规则', 'FIXED_RATE', 'AND', 100, 'ODD_JOB_HOURS', 'TYPED',
       '{"mode":"FIXED_RATE","basis":"ODD_JOB_HOURS","unit":"小时","rateValue":5,"summary":"按零工工时 * 5 元/小时计价"}',
       null, null, '零工费按 5 元/小时计价', '0', 70, '首钢真实费用：零工费', 'flyway', sysdate(), 'flyway', sysdate()
from cost_scene s
join cost_fee_item f on f.scene_id = s.scene_id and f.fee_code = 'SG_ODD_JOB_FEE'
where s.scene_code = 'SHOUGANG-ORE-HR-001'
  and not exists (select 1 from cost_rule where scene_id = s.scene_id and rule_code = 'SG_ODD_JOB_RATE_01');

insert into cost_rule (scene_id, fee_id, rule_code, rule_name, rule_type, condition_logic, priority, quantity_variable_code, pricing_mode, pricing_json, amount_formula, amount_formula_code, note_template, status, sort_no, remark, create_by, create_time, update_by, update_time)
select s.scene_id, f.fee_id, 'SG_DUTY_SHIFT_FORMULA_01', '值守专班劳务费公式规则', 'FORMULA', 'AND', 100, '', 'TYPED',
       null,
       '(吞吐量计件类费用 - 女工固定类劳务费 - 清料/库场专班固定类劳务费 - 清舱劳务费 - 苫盖零工劳务费 - 带缆费 - 零工费) * ((该队应出勤 / 所有队应出勤) * (该队实际出勤 / 该队应出勤))',
       'SG_RULE_DUTY_SHIFT_LABOR_AMOUNT', '值守专班劳务费按前置费用差额和多变量分摊公式直接计算', '0', 80, '首钢真实费用：值守专班劳务费', 'flyway', sysdate(), 'flyway', sysdate()
from cost_scene s
join cost_fee_item f on f.scene_id = s.scene_id and f.fee_code = 'SG_DUTY_SHIFT_LABOR'
where s.scene_code = 'SHOUGANG-ORE-HR-001'
  and not exists (select 1 from cost_rule where scene_id = s.scene_id and rule_code = 'SG_DUTY_SHIFT_FORMULA_01');

insert into cost_rule (scene_id, fee_id, rule_code, rule_name, rule_type, condition_logic, priority, quantity_variable_code, pricing_mode, pricing_json, amount_formula, amount_formula_code, note_template, status, sort_no, remark, create_by, create_time, update_by, update_time)
select s.scene_id, f.fee_id, 'SG_SEASONAL_ALLOWANCE_FORMULA_01', '季节性补贴公式规则', 'FORMULA', 'AND', 100, '', 'TYPED',
       null,
       '若账期属于 12/01/02/03，则 500 * 季节性补贴计发份额，否则为 0',
       'SG_RULE_SEASONAL_ALLOWANCE_AMOUNT', '季节性补贴按账期和计发份额计算', '0', 90, '首钢真实费用：季节性补贴；春节补贴暂不纳入', 'flyway', sysdate(), 'flyway', sysdate()
from cost_scene s
join cost_fee_item f on f.scene_id = s.scene_id and f.fee_code = 'SG_SEASONAL_ALLOWANCE'
where s.scene_code = 'SHOUGANG-ORE-HR-001'
  and not exists (select 1 from cost_rule where scene_id = s.scene_id and rule_code = 'SG_SEASONAL_ALLOWANCE_FORMULA_01');

insert into cost_rule (scene_id, fee_id, rule_code, rule_name, rule_type, condition_logic, priority, quantity_variable_code, pricing_mode, pricing_json, amount_formula, amount_formula_code, note_template, status, sort_no, remark, create_by, create_time, update_by, update_time)
select s.scene_id, f.fee_id, 'SG_OVERTIME_RATE_01', '加班费用规则', 'FIXED_RATE', 'AND', 100, 'OVERTIME_DAYS', 'TYPED',
       '{"mode":"FIXED_RATE","basis":"OVERTIME_DAYS","unit":"天","rateValue":240,"summary":"按加班天数 * 240 元/天计价"}',
       null, null, '加班费用按 240 元/天计价', '0', 100, '首钢真实费用：加班费用', 'flyway', sysdate(), 'flyway', sysdate()
from cost_scene s
join cost_fee_item f on f.scene_id = s.scene_id and f.fee_code = 'SG_OVERTIME_FEE'
where s.scene_code = 'SHOUGANG-ORE-HR-001'
  and not exists (select 1 from cost_rule where scene_id = s.scene_id and rule_code = 'SG_OVERTIME_RATE_01');

insert into cost_rule (scene_id, fee_id, rule_code, rule_name, rule_type, condition_logic, priority, quantity_variable_code, pricing_mode, pricing_json, amount_formula, amount_formula_code, note_template, status, sort_no, remark, create_by, create_time, update_by, update_time)
select s.scene_id, f.fee_id, 'SG_UNIT_BEARING_RATE_01', '单位承担费用规则', 'FIXED_RATE', 'AND', 100, 'UNIT_BEARING_AMOUNT', 'TYPED',
       '{"mode":"FIXED_RATE","basis":"UNIT_BEARING_AMOUNT","unit":"元","rateValue":1,"summary":"按录入金额原值计价"}',
       null, null, '单位承担费用按录入金额直传', '0', 110, '首钢真实费用：单位承担部分的费用', 'flyway', sysdate(), 'flyway', sysdate()
from cost_scene s
join cost_fee_item f on f.scene_id = s.scene_id and f.fee_code = 'SG_UNIT_BEARING_FEE'
where s.scene_code = 'SHOUGANG-ORE-HR-001'
  and not exists (select 1 from cost_rule where scene_id = s.scene_id and rule_code = 'SG_UNIT_BEARING_RATE_01');

insert into cost_rule (scene_id, fee_id, rule_code, rule_name, rule_type, condition_logic, priority, quantity_variable_code, pricing_mode, pricing_json, amount_formula, amount_formula_code, note_template, status, sort_no, remark, create_by, create_time, update_by, update_time)
select s.scene_id, f.fee_id, 'SG_INSURANCE_TAXABLE_RATE_01', '五险应税额规则', 'FIXED_RATE', 'AND', 100, 'INSURANCE_TAXABLE_AMOUNT', 'TYPED',
       '{"mode":"FIXED_RATE","basis":"INSURANCE_TAXABLE_AMOUNT","unit":"元","rateValue":1,"summary":"按录入金额原值计价"}',
       null, null, '五险应税额按录入金额直传', '0', 120, '首钢真实费用：五险应税额', 'flyway', sysdate(), 'flyway', sysdate()
from cost_scene s
join cost_fee_item f on f.scene_id = s.scene_id and f.fee_code = 'SG_INSURANCE_TAXABLE_FEE'
where s.scene_code = 'SHOUGANG-ORE-HR-001'
  and not exists (select 1 from cost_rule where scene_id = s.scene_id and rule_code = 'SG_INSURANCE_TAXABLE_RATE_01');

insert into cost_rule (scene_id, fee_id, rule_code, rule_name, rule_type, condition_logic, priority, quantity_variable_code, pricing_mode, pricing_json, amount_formula, amount_formula_code, note_template, status, sort_no, remark, create_by, create_time, update_by, update_time)
select s.scene_id, f.fee_id, 'SG_EMPLOYER_LIABILITY_RATE_01', '雇主责任险规则', 'FIXED_RATE', 'AND', 100, 'EMPLOYER_LIABILITY_AMOUNT', 'TYPED',
       '{"mode":"FIXED_RATE","basis":"EMPLOYER_LIABILITY_AMOUNT","unit":"元","rateValue":1,"summary":"按录入金额原值计价"}',
       null, null, '雇主责任险按录入金额直传', '0', 130, '首钢真实费用：雇主责任险', 'flyway', sysdate(), 'flyway', sysdate()
from cost_scene s
join cost_fee_item f on f.scene_id = s.scene_id and f.fee_code = 'SG_EMPLOYER_LIABILITY_FEE'
where s.scene_code = 'SHOUGANG-ORE-HR-001'
  and not exists (select 1 from cost_rule where scene_id = s.scene_id and rule_code = 'SG_EMPLOYER_LIABILITY_RATE_01');

insert into cost_rule (scene_id, fee_id, rule_code, rule_name, rule_type, condition_logic, priority, quantity_variable_code, pricing_mode, pricing_json, amount_formula, amount_formula_code, note_template, status, sort_no, remark, create_by, create_time, update_by, update_time)
select s.scene_id, f.fee_id, 'SG_MANAGEMENT_FORMULA_01', '协力单位管理费公式规则', 'FORMULA', 'AND', 100, '', 'TYPED',
       null,
       '(除保险费外的上述所有费用总和) * 16.77%',
       'SG_RULE_MANAGEMENT_FEE_AMOUNT', '协力单位管理费按非保险费用合计乘 16.77% 计算', '0', 140, '首钢真实费用：协力单位管理费', 'flyway', sysdate(), 'flyway', sysdate()
from cost_scene s
join cost_fee_item f on f.scene_id = s.scene_id and f.fee_code = 'SG_MANAGEMENT_FEE'
where s.scene_code = 'SHOUGANG-ORE-HR-001'
  and not exists (select 1 from cost_rule where scene_id = s.scene_id and rule_code = 'SG_MANAGEMENT_FORMULA_01');

insert into cost_rule_condition (scene_id, rule_id, group_no, sort_no, variable_code, display_name, operator_code, compare_value, status, remark, create_by, create_time, update_by, update_time)
select s.scene_id, r.rule_id, 1, 10, 'COVER_ACTION', '苫盖动作', 'EQ', 'COVER', '0', '组 1：苫焦煤垛', 'flyway', sysdate(), 'flyway', sysdate()
from cost_scene s
join cost_rule r on r.scene_id = s.scene_id and r.rule_code = 'SG_COVER_ODD_JOB_GROUPED_01'
where s.scene_code = 'SHOUGANG-ORE-HR-001'
  and not exists (select 1 from cost_rule_condition where rule_id = r.rule_id and group_no = 1 and sort_no = 10 and variable_code = 'COVER_ACTION');

insert into cost_rule_condition (scene_id, rule_id, group_no, sort_no, variable_code, display_name, operator_code, compare_value, status, remark, create_by, create_time, update_by, update_time)
select s.scene_id, r.rule_id, 1, 20, 'COVER_CARGO_TYPE', '苫盖货种', 'EQ', 'COAL', '0', '组 1：苫焦煤垛', 'flyway', sysdate(), 'flyway', sysdate()
from cost_scene s
join cost_rule r on r.scene_id = s.scene_id and r.rule_code = 'SG_COVER_ODD_JOB_GROUPED_01'
where s.scene_code = 'SHOUGANG-ORE-HR-001'
  and not exists (select 1 from cost_rule_condition where rule_id = r.rule_id and group_no = 1 and sort_no = 20 and variable_code = 'COVER_CARGO_TYPE');

insert into cost_rule_condition (scene_id, rule_id, group_no, sort_no, variable_code, display_name, operator_code, compare_value, status, remark, create_by, create_time, update_by, update_time)
select s.scene_id, r.rule_id, 2, 10, 'COVER_ACTION', '苫盖动作', 'EQ', 'UNCOVER', '0', '组 2：揭焦煤垛', 'flyway', sysdate(), 'flyway', sysdate()
from cost_scene s
join cost_rule r on r.scene_id = s.scene_id and r.rule_code = 'SG_COVER_ODD_JOB_GROUPED_01'
where s.scene_code = 'SHOUGANG-ORE-HR-001'
  and not exists (select 1 from cost_rule_condition where rule_id = r.rule_id and group_no = 2 and sort_no = 10 and variable_code = 'COVER_ACTION');

insert into cost_rule_condition (scene_id, rule_id, group_no, sort_no, variable_code, display_name, operator_code, compare_value, status, remark, create_by, create_time, update_by, update_time)
select s.scene_id, r.rule_id, 2, 20, 'COVER_CARGO_TYPE', '苫盖货种', 'EQ', 'COAL', '0', '组 2：揭焦煤垛', 'flyway', sysdate(), 'flyway', sysdate()
from cost_scene s
join cost_rule r on r.scene_id = s.scene_id and r.rule_code = 'SG_COVER_ODD_JOB_GROUPED_01'
where s.scene_code = 'SHOUGANG-ORE-HR-001'
  and not exists (select 1 from cost_rule_condition where rule_id = r.rule_id and group_no = 2 and sort_no = 20 and variable_code = 'COVER_CARGO_TYPE');

insert into cost_rule_condition (scene_id, rule_id, group_no, sort_no, variable_code, display_name, operator_code, compare_value, status, remark, create_by, create_time, update_by, update_time)
select s.scene_id, r.rule_id, 3, 10, 'COVER_ACTION', '苫盖动作', 'EQ', 'COVER', '0', '组 3：苫矿石垛', 'flyway', sysdate(), 'flyway', sysdate()
from cost_scene s
join cost_rule r on r.scene_id = s.scene_id and r.rule_code = 'SG_COVER_ODD_JOB_GROUPED_01'
where s.scene_code = 'SHOUGANG-ORE-HR-001'
  and not exists (select 1 from cost_rule_condition where rule_id = r.rule_id and group_no = 3 and sort_no = 10 and variable_code = 'COVER_ACTION');

insert into cost_rule_condition (scene_id, rule_id, group_no, sort_no, variable_code, display_name, operator_code, compare_value, status, remark, create_by, create_time, update_by, update_time)
select s.scene_id, r.rule_id, 3, 20, 'COVER_CARGO_TYPE', '苫盖货种', 'EQ', 'ORE', '0', '组 3：苫矿石垛', 'flyway', sysdate(), 'flyway', sysdate()
from cost_scene s
join cost_rule r on r.scene_id = s.scene_id and r.rule_code = 'SG_COVER_ODD_JOB_GROUPED_01'
where s.scene_code = 'SHOUGANG-ORE-HR-001'
  and not exists (select 1 from cost_rule_condition where rule_id = r.rule_id and group_no = 3 and sort_no = 20 and variable_code = 'COVER_CARGO_TYPE');

insert into cost_rule_condition (scene_id, rule_id, group_no, sort_no, variable_code, display_name, operator_code, compare_value, status, remark, create_by, create_time, update_by, update_time)
select s.scene_id, r.rule_id, 4, 10, 'COVER_ACTION', '苫盖动作', 'EQ', 'UNCOVER', '0', '组 4：揭矿石垛', 'flyway', sysdate(), 'flyway', sysdate()
from cost_scene s
join cost_rule r on r.scene_id = s.scene_id and r.rule_code = 'SG_COVER_ODD_JOB_GROUPED_01'
where s.scene_code = 'SHOUGANG-ORE-HR-001'
  and not exists (select 1 from cost_rule_condition where rule_id = r.rule_id and group_no = 4 and sort_no = 10 and variable_code = 'COVER_ACTION');

insert into cost_rule_condition (scene_id, rule_id, group_no, sort_no, variable_code, display_name, operator_code, compare_value, status, remark, create_by, create_time, update_by, update_time)
select s.scene_id, r.rule_id, 4, 20, 'COVER_CARGO_TYPE', '苫盖货种', 'EQ', 'ORE', '0', '组 4：揭矿石垛', 'flyway', sysdate(), 'flyway', sysdate()
from cost_scene s
join cost_rule r on r.scene_id = s.scene_id and r.rule_code = 'SG_COVER_ODD_JOB_GROUPED_01'
where s.scene_code = 'SHOUGANG-ORE-HR-001'
  and not exists (select 1 from cost_rule_condition where rule_id = r.rule_id and group_no = 4 and sort_no = 20 and variable_code = 'COVER_CARGO_TYPE');

insert into cost_rule_condition (scene_id, rule_id, group_no, sort_no, variable_code, display_name, operator_code, compare_value, status, remark, create_by, create_time, update_by, update_time)
select s.scene_id, r.rule_id, 1, 10, 'MOORING_ACTION', '带缆动作', 'EQ', 'MOOR', '0', '组 1：带缆', 'flyway', sysdate(), 'flyway', sysdate()
from cost_scene s
join cost_rule r on r.scene_id = s.scene_id and r.rule_code = 'SG_MOORING_GROUPED_01'
where s.scene_code = 'SHOUGANG-ORE-HR-001'
  and not exists (select 1 from cost_rule_condition where rule_id = r.rule_id and group_no = 1 and sort_no = 10 and variable_code = 'MOORING_ACTION');

insert into cost_rule_condition (scene_id, rule_id, group_no, sort_no, variable_code, display_name, operator_code, compare_value, status, remark, create_by, create_time, update_by, update_time)
select s.scene_id, r.rule_id, 2, 10, 'MOORING_ACTION', '带缆动作', 'EQ', 'UNMOOR', '0', '组 2：解缆', 'flyway', sysdate(), 'flyway', sysdate()
from cost_scene s
join cost_rule r on r.scene_id = s.scene_id and r.rule_code = 'SG_MOORING_GROUPED_01'
where s.scene_code = 'SHOUGANG-ORE-HR-001'
  and not exists (select 1 from cost_rule_condition where rule_id = r.rule_id and group_no = 2 and sort_no = 10 and variable_code = 'MOORING_ACTION');

-- Normalize cost-domain table collations for existing 0.3.x databases.
-- Run this before release regression if historical tables mix utf8mb4_0900_ai_ci
-- and utf8mb4_general_ci. Clean installs are covered by cost_init.sql.

set foreign_key_checks = 0;

alter table cost_access_profile convert to character set utf8mb4 collate utf8mb4_general_ci;
alter table cost_alarm_record convert to character set utf8mb4 collate utf8mb4_general_ci;
alter table cost_audit_log convert to character set utf8mb4 collate utf8mb4_general_ci;
alter table cost_bill_period convert to character set utf8mb4 collate utf8mb4_general_ci;
alter table cost_calc_input_batch convert to character set utf8mb4 collate utf8mb4_general_ci;
alter table cost_calc_input_batch_item convert to character set utf8mb4 collate utf8mb4_general_ci;
alter table cost_calc_task convert to character set utf8mb4 collate utf8mb4_general_ci;
alter table cost_calc_task_detail convert to character set utf8mb4 collate utf8mb4_general_ci;
alter table cost_calc_task_partition convert to character set utf8mb4 collate utf8mb4_general_ci;
alter table cost_fee_item convert to character set utf8mb4 collate utf8mb4_general_ci;
alter table cost_fee_variable_rel convert to character set utf8mb4 collate utf8mb4_general_ci;
alter table cost_formula convert to character set utf8mb4 collate utf8mb4_general_ci;
alter table cost_formula_version convert to character set utf8mb4 collate utf8mb4_general_ci;
alter table cost_open_app convert to character set utf8mb4 collate utf8mb4_general_ci;
alter table cost_publish_snapshot convert to character set utf8mb4 collate utf8mb4_general_ci;
alter table cost_publish_version convert to character set utf8mb4 collate utf8mb4_general_ci;
alter table cost_recalc_order convert to character set utf8mb4 collate utf8mb4_general_ci;
alter table cost_result_ledger convert to character set utf8mb4 collate utf8mb4_general_ci;
alter table cost_result_trace convert to character set utf8mb4 collate utf8mb4_general_ci;
alter table cost_rule convert to character set utf8mb4 collate utf8mb4_general_ci;
alter table cost_rule_condition convert to character set utf8mb4 collate utf8mb4_general_ci;
alter table cost_rule_tier convert to character set utf8mb4 collate utf8mb4_general_ci;
alter table cost_scene convert to character set utf8mb4 collate utf8mb4_general_ci;
alter table cost_simulation_record convert to character set utf8mb4 collate utf8mb4_general_ci;
alter table cost_variable convert to character set utf8mb4 collate utf8mb4_general_ci;
alter table cost_variable_group convert to character set utf8mb4 collate utf8mb4_general_ci;

set foreign_key_checks = 1;

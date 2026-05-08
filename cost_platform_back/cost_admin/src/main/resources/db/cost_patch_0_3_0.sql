-- cost_patch_0_3_0.sql
-- Purpose: apply 0.3.0 hardening changes to an existing cost_platform database.
-- Notes:
-- 1. Run after backing up the target database.
-- 2. The request_no unique key only applies when request_no is not empty.
-- 3. If historical duplicated non-empty request_no rows exist in the same scene/version/month, clean them before running.

set names utf8mb4;

drop procedure if exists cost_patch_0_3_0;
delimiter //
create procedure cost_patch_0_3_0()
begin
  if not exists (
    select 1
    from information_schema.columns
    where table_schema = database()
      and table_name = 'cost_calc_task'
      and column_name = 'request_no_key'
  ) then
    alter table cost_calc_task
      add column request_no_key varchar(64)
      generated always as (nullif(request_no, '')) stored
      comment '非空幂等请求号唯一键辅助列'
      after request_no;
  end if;

  if not exists (
    select 1
    from information_schema.statistics
    where table_schema = database()
      and table_name = 'cost_calc_task'
      and index_name = 'uk_cost_calc_task_request_no'
  ) then
    alter table cost_calc_task
      add unique key uk_cost_calc_task_request_no (scene_id, version_id, bill_month, request_no_key);
  end if;

  if not exists (select 1 from sys_menu where menu_id = 2248) then
    insert into sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
    values (2248, '结果导出', 2009, 3, '#', '', '', '', 1, 0, 'F', '0', '0', 'cost:result:export', '#', 'admin', sysdate(), '');
  end if;

  if not exists (select 1 from sys_role_menu where role_id = 1 and menu_id = 2248) then
    insert into sys_role_menu (role_id, menu_id) values (1, 2248);
  end if;
end//
delimiter ;

call cost_patch_0_3_0();
drop procedure if exists cost_patch_0_3_0;

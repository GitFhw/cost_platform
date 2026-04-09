set @cost_calc_input_batch_access_profile_exists = (
    select count(1)
    from information_schema.columns
    where table_schema = database()
      and table_name = 'cost_calc_input_batch'
      and column_name = 'access_profile_id'
);

set @cost_calc_input_batch_access_profile_sql = if(
    @cost_calc_input_batch_access_profile_exists > 0,
    'select 1',
    'alter table cost_calc_input_batch add column access_profile_id bigint null comment ''接入方案ID'' after error_message'
);

prepare stmt_cost_calc_input_batch_access_profile from @cost_calc_input_batch_access_profile_sql;
execute stmt_cost_calc_input_batch_access_profile;
deallocate prepare stmt_cost_calc_input_batch_access_profile;

set @cost_calc_input_batch_checkpoint_exists = (
    select count(1)
    from information_schema.columns
    where table_schema = database()
      and table_name = 'cost_calc_input_batch'
      and column_name = 'checkpoint_json'
);

set @cost_calc_input_batch_checkpoint_sql = if(
    @cost_calc_input_batch_checkpoint_exists > 0,
    'select 1',
    'alter table cost_calc_input_batch add column checkpoint_json longtext null comment ''断点检查点JSON'' after access_profile_id'
);

prepare stmt_cost_calc_input_batch_checkpoint from @cost_calc_input_batch_checkpoint_sql;
execute stmt_cost_calc_input_batch_checkpoint;
deallocate prepare stmt_cost_calc_input_batch_checkpoint;

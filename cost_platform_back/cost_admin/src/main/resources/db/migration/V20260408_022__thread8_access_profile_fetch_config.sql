set @cost_access_profile_fetch_config_exists = (
    select count(1)
    from information_schema.columns
    where table_schema = database()
      and table_name = 'cost_access_profile'
      and column_name = 'fetch_config_json'
);

set @cost_access_profile_fetch_config_sql = if(
    @cost_access_profile_fetch_config_exists > 0,
    'select 1',
    'alter table cost_access_profile add column fetch_config_json longtext null comment ''拉取策略JSON'' after auth_config_json'
);

prepare stmt_cost_access_profile_fetch_config from @cost_access_profile_fetch_config_sql;
execute stmt_cost_access_profile_fetch_config;
deallocate prepare stmt_cost_access_profile_fetch_config;

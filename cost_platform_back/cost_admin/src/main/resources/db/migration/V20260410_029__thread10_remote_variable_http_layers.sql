set @current_schema = database();

set @ddl = (
  select if(
    exists(
      select 1
      from information_schema.columns
      where table_schema = @current_schema
        and table_name = 'cost_variable'
        and column_name = 'request_method'
    ),
    'select 1',
    'alter table cost_variable add column request_method varchar(16) default ''GET'' comment ''第三方接口请求方式，例如GET、POST、PUT、DELETE'' after remote_api'
  )
);
prepare stmt from @ddl;
execute stmt;
deallocate prepare stmt;

set @ddl = (
  select if(
    exists(
      select 1
      from information_schema.columns
      where table_schema = @current_schema
        and table_name = 'cost_variable'
        and column_name = 'content_type'
    ),
    'select 1',
    'alter table cost_variable add column content_type varchar(128) default ''application/json'' comment ''第三方接口请求内容类型'' after request_method'
  )
);
prepare stmt from @ddl;
execute stmt;
deallocate prepare stmt;

set @ddl = (
  select if(
    exists(
      select 1
      from information_schema.columns
      where table_schema = @current_schema
        and table_name = 'cost_variable'
        and column_name = 'query_config_json'
    ),
    'select 1',
    'alter table cost_variable add column query_config_json json default null comment ''第三方接口查询参数配置JSON'' after content_type'
  )
);
prepare stmt from @ddl;
execute stmt;
deallocate prepare stmt;

set @ddl = (
  select if(
    exists(
      select 1
      from information_schema.columns
      where table_schema = @current_schema
        and table_name = 'cost_variable'
        and column_name = 'request_headers_json'
    ),
    'select 1',
    'alter table cost_variable add column request_headers_json json default null comment ''第三方接口请求头配置JSON'' after query_config_json'
  )
);
prepare stmt from @ddl;
execute stmt;
deallocate prepare stmt;

set @ddl = (
  select if(
    exists(
      select 1
      from information_schema.columns
      where table_schema = @current_schema
        and table_name = 'cost_variable'
        and column_name = 'body_template_json'
    ),
    'select 1',
    'alter table cost_variable add column body_template_json json default null comment ''第三方接口请求体模板JSON'' after request_headers_json'
  )
);
prepare stmt from @ddl;
execute stmt;
deallocate prepare stmt;

set @ddl = (
  select if(
    exists(
      select 1
      from information_schema.columns
      where table_schema = @current_schema
        and table_name = 'cost_variable'
        and column_name = 'response_config_json'
    ),
    'select 1',
    'alter table cost_variable add column response_config_json json default null comment ''第三方接口响应提取配置JSON'' after data_path'
  )
);
prepare stmt from @ddl;
execute stmt;
deallocate prepare stmt;

set @ddl = (
  select if(
    exists(
      select 1
      from information_schema.columns
      where table_schema = @current_schema
        and table_name = 'cost_variable'
        and column_name = 'page_config_json'
    ),
    'select 1',
    'alter table cost_variable add column page_config_json json default null comment ''第三方接口分页策略配置JSON'' after mapping_config_json'
  )
);
prepare stmt from @ddl;
execute stmt;
deallocate prepare stmt;

set @ddl = (
  select if(
    exists(
      select 1
      from information_schema.columns
      where table_schema = @current_schema
        and table_name = 'cost_variable'
        and column_name = 'adapter_type'
    ),
    'select 1',
    'alter table cost_variable add column adapter_type varchar(32) default ''STANDARD'' comment ''第三方接口适配器类型，例如STANDARD、ROOT_ARRAY、PAGE_ENVELOPE、SINGLE_OBJECT'' after page_config_json'
  )
);
prepare stmt from @ddl;
execute stmt;
deallocate prepare stmt;

set @ddl = (
  select if(
    exists(
      select 1
      from information_schema.columns
      where table_schema = @current_schema
        and table_name = 'cost_variable'
        and column_name = 'adapter_config_json'
    ),
    'select 1',
    'alter table cost_variable add column adapter_config_json json default null comment ''第三方接口特殊适配器配置JSON'' after adapter_type'
  )
);
prepare stmt from @ddl;
execute stmt;
deallocate prepare stmt;

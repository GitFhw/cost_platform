-- 线程二整改：变量中心来源治理增强

set @current_schema = database();

set @ddl = (
  select if(
    exists(
      select 1
      from information_schema.columns
      where table_schema = @current_schema
        and table_name = 'cost_variable'
        and column_name = 'source_system'
    ),
    'select 1',
    'alter table cost_variable add column source_system varchar(64) default '''' comment ''来源系统标识，例如 WMS、ERP、TMS'' after source_type'
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
        and column_name = 'auth_type'
    ),
    'select 1',
    'alter table cost_variable add column auth_type varchar(32) default ''NONE'' comment ''接口鉴权方式，例如NONE、BASIC、BEARER、API_KEY'' after remote_api'
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
        and column_name = 'auth_config_json'
    ),
    'select 1',
    'alter table cost_variable add column auth_config_json json default null comment ''接口鉴权配置 JSON，用于托管请求头、账号、密钥占位信息'' after auth_type'
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
        and column_name = 'mapping_config_json'
    ),
    'select 1',
    'alter table cost_variable add column mapping_config_json json default null comment ''字段映射配置 JSON，表达第三方字段到平台变量的映射关系'' after data_path'
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
        and column_name = 'sync_mode'
    ),
    'select 1',
    'alter table cost_variable add column sync_mode varchar(32) default ''REALTIME'' comment ''拉取方式，例如REALTIME、NEAR_REALTIME、SCHEDULED'' after mapping_config_json'
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
        and column_name = 'cache_policy'
    ),
    'select 1',
    'alter table cost_variable add column cache_policy varchar(32) default ''MANUAL_REFRESH'' comment ''缓存策略，例如NONE、TTL、MANUAL_REFRESH'' after sync_mode'
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
        and column_name = 'fallback_policy'
    ),
    'select 1',
    'alter table cost_variable add column fallback_policy varchar(32) default ''FAIL_FAST'' comment ''失败兜底策略，例如FAIL_FAST、DEFAULT_VALUE、LAST_SNAPSHOT'' after cache_policy'
  )
);
prepare stmt from @ddl;
execute stmt;
deallocate prepare stmt;

set @ddl = (
  select if(
    exists(
      select 1
      from information_schema.statistics
      where table_schema = @current_schema
        and table_name = 'cost_variable'
        and index_name = 'idx_cost_variable_scene_source'
    ),
    'select 1',
    'create index idx_cost_variable_scene_source on cost_variable(scene_id, source_type, source_system)'
  )
);
prepare stmt from @ddl;
execute stmt;
deallocate prepare stmt;

insert into sys_dict_type (dict_id, dict_name, dict_type, status, create_by, create_time, remark)
select 139, '核算-变量鉴权方式', 'cost_variable_auth_type', '0', 'admin', sysdate(), '线程二整改：第三方接入变量鉴权方式'
from dual
where not exists (select 1 from sys_dict_type where dict_type = 'cost_variable_auth_type');

insert into sys_dict_type (dict_id, dict_name, dict_type, status, create_by, create_time, remark)
select 140, '核算-变量同步方式', 'cost_variable_sync_mode', '0', 'admin', sysdate(), '线程二整改：第三方接入变量同步方式'
from dual
where not exists (select 1 from sys_dict_type where dict_type = 'cost_variable_sync_mode');

insert into sys_dict_type (dict_id, dict_name, dict_type, status, create_by, create_time, remark)
select 141, '核算-变量缓存策略', 'cost_variable_cache_policy', '0', 'admin', sysdate(), '线程二整改：第三方接入变量缓存策略'
from dual
where not exists (select 1 from sys_dict_type where dict_type = 'cost_variable_cache_policy');

insert into sys_dict_type (dict_id, dict_name, dict_type, status, create_by, create_time, remark)
select 142, '核算-变量失败兜底策略', 'cost_variable_fallback_policy', '0', 'admin', sysdate(), '线程二整改：第三方接入变量失败兜底策略'
from dual
where not exists (select 1 from sys_dict_type where dict_type = 'cost_variable_fallback_policy');

insert into sys_dict_data (dict_code, dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
select 1390, 1, '无鉴权', 'NONE', 'cost_variable_auth_type', '', 'info', 'Y', '0', 'admin', sysdate(), '无需鉴权'
from dual
where not exists (select 1 from sys_dict_data where dict_type = 'cost_variable_auth_type' and dict_value = 'NONE');

insert into sys_dict_data (dict_code, dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
select 1391, 2, 'Basic', 'BASIC', 'cost_variable_auth_type', '', 'primary', 'N', '0', 'admin', sysdate(), 'Basic 鉴权'
from dual
where not exists (select 1 from sys_dict_data where dict_type = 'cost_variable_auth_type' and dict_value = 'BASIC');

insert into sys_dict_data (dict_code, dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
select 1392, 3, 'Bearer Token', 'BEARER', 'cost_variable_auth_type', '', 'success', 'N', '0', 'admin', sysdate(), 'Bearer Token 鉴权'
from dual
where not exists (select 1 from sys_dict_data where dict_type = 'cost_variable_auth_type' and dict_value = 'BEARER');

insert into sys_dict_data (dict_code, dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
select 1393, 4, 'API Key', 'API_KEY', 'cost_variable_auth_type', '', 'warning', 'N', '0', 'admin', sysdate(), 'API Key 鉴权'
from dual
where not exists (select 1 from sys_dict_data where dict_type = 'cost_variable_auth_type' and dict_value = 'API_KEY');

insert into sys_dict_data (dict_code, dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
select 1400, 1, '实时拉取', 'REALTIME', 'cost_variable_sync_mode', '', 'primary', 'Y', '0', 'admin', sysdate(), '调用时实时拉取'
from dual
where not exists (select 1 from sys_dict_data where dict_type = 'cost_variable_sync_mode' and dict_value = 'REALTIME');

insert into sys_dict_data (dict_code, dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
select 1401, 2, '准实时缓存', 'NEAR_REALTIME', 'cost_variable_sync_mode', '', 'success', 'N', '0', 'admin', sysdate(), '按短周期缓存刷新'
from dual
where not exists (select 1 from sys_dict_data where dict_type = 'cost_variable_sync_mode' and dict_value = 'NEAR_REALTIME');

insert into sys_dict_data (dict_code, dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
select 1402, 3, '定时同步', 'SCHEDULED', 'cost_variable_sync_mode', '', 'warning', 'N', '0', 'admin', sysdate(), '按任务定时同步'
from dual
where not exists (select 1 from sys_dict_data where dict_type = 'cost_variable_sync_mode' and dict_value = 'SCHEDULED');

insert into sys_dict_data (dict_code, dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
select 1410, 1, '不缓存', 'NONE', 'cost_variable_cache_policy', '', 'info', 'N', '0', 'admin', sysdate(), '不做缓存'
from dual
where not exists (select 1 from sys_dict_data where dict_type = 'cost_variable_cache_policy' and dict_value = 'NONE');

insert into sys_dict_data (dict_code, dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
select 1411, 2, 'TTL缓存', 'TTL', 'cost_variable_cache_policy', '', 'success', 'N', '0', 'admin', sysdate(), '按失效时间缓存'
from dual
where not exists (select 1 from sys_dict_data where dict_type = 'cost_variable_cache_policy' and dict_value = 'TTL');

insert into sys_dict_data (dict_code, dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
select 1412, 3, '手动刷新', 'MANUAL_REFRESH', 'cost_variable_cache_policy', '', 'primary', 'Y', '0', 'admin', sysdate(), '仅手动刷新'
from dual
where not exists (select 1 from sys_dict_data where dict_type = 'cost_variable_cache_policy' and dict_value = 'MANUAL_REFRESH');

insert into sys_dict_data (dict_code, dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
select 1420, 1, '失败即终止', 'FAIL_FAST', 'cost_variable_fallback_policy', '', 'danger', 'Y', '0', 'admin', sysdate(), '失败后直接终止'
from dual
where not exists (select 1 from sys_dict_data where dict_type = 'cost_variable_fallback_policy' and dict_value = 'FAIL_FAST');

insert into sys_dict_data (dict_code, dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
select 1421, 2, '回退默认值', 'DEFAULT_VALUE', 'cost_variable_fallback_policy', '', 'warning', 'N', '0', 'admin', sysdate(), '失败后使用默认值'
from dual
where not exists (select 1 from sys_dict_data where dict_type = 'cost_variable_fallback_policy' and dict_value = 'DEFAULT_VALUE');

insert into sys_dict_data (dict_code, dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
select 1422, 3, '回退快照值', 'LAST_SNAPSHOT', 'cost_variable_fallback_policy', '', 'info', 'N', '0', 'admin', sysdate(), '失败后回退最近快照值'
from dual
where not exists (select 1 from sys_dict_data where dict_type = 'cost_variable_fallback_policy' and dict_value = 'LAST_SNAPSHOT');

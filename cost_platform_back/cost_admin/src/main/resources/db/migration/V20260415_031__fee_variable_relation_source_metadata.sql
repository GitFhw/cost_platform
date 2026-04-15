-- 0.2.0 费用-变量关系来源治理。
-- 将关系表拆出来源元数据，规则同步只维护 RULE_DERIVED，预留 MANUAL_REQUIRED 等人工契约来源。

set @current_schema = database();

set @ddl = (
  select if(
    exists(
      select 1
      from information_schema.columns
      where table_schema = @current_schema
        and table_name = 'cost_fee_variable_rel'
        and column_name = 'source_type'
    ),
    'select 1',
    'alter table cost_fee_variable_rel add column source_type varchar(32) not null default ''RULE_DERIVED'' comment ''来源类型，例如RULE_DERIVED、MANUAL_REQUIRED'' after relation_type'
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
        and table_name = 'cost_fee_variable_rel'
        and column_name = 'source_rule_id'
    ),
    'select 1',
    'alter table cost_fee_variable_rel add column source_rule_id bigint default null comment ''来源规则主键，规则派生关系使用'' after source_type'
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
        and table_name = 'cost_fee_variable_rel'
        and column_name = 'source_code'
    ),
    'select 1',
    'alter table cost_fee_variable_rel add column source_code varchar(128) not null default '''' comment ''来源编码，例如规则编码或手工配置编码'' after source_rule_id'
  )
);
prepare stmt from @ddl;
execute stmt;
deallocate prepare stmt;

update cost_fee_variable_rel
set source_type = 'RULE_DERIVED'
where source_type is null
   or source_type = '';

update cost_fee_variable_rel
set source_code = ''
where source_code is null;

set @ddl = (
  select if(
    exists(
      select 1
      from information_schema.statistics
      where table_schema = @current_schema
        and table_name = 'cost_fee_variable_rel'
        and index_name = 'uk_cost_fee_var_rel'
    ),
    'alter table cost_fee_variable_rel drop index uk_cost_fee_var_rel',
    'select 1'
  )
);
prepare stmt from @ddl;
execute stmt;
deallocate prepare stmt;

alter table cost_fee_variable_rel
  add unique key uk_cost_fee_var_rel (fee_id, variable_id, relation_type, source_type, source_code);

set @ddl = (
  select if(
    exists(
      select 1
      from information_schema.statistics
      where table_schema = @current_schema
        and table_name = 'cost_fee_variable_rel'
        and index_name = 'idx_cost_fee_var_source'
    ),
    'select 1',
    'alter table cost_fee_variable_rel add key idx_cost_fee_var_source (source_type, source_rule_id, source_code)'
  )
);
prepare stmt from @ddl;
execute stmt;
deallocate prepare stmt;

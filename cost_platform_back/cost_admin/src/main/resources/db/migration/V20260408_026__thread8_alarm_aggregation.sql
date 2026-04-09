set @alarm_first_trigger_exists = (
  select count(1)
  from information_schema.columns
  where table_schema = database()
    and table_name = 'cost_alarm_record'
    and column_name = 'first_trigger_time'
);
set @alarm_first_trigger_sql = if(
  @alarm_first_trigger_exists = 0,
  'alter table cost_alarm_record add column first_trigger_time datetime null comment ''首次触发时间'' after trigger_time',
  'select 1'
);
prepare stmt from @alarm_first_trigger_sql;
execute stmt;
deallocate prepare stmt;

set @alarm_latest_trigger_exists = (
  select count(1)
  from information_schema.columns
  where table_schema = database()
    and table_name = 'cost_alarm_record'
    and column_name = 'latest_trigger_time'
);
set @alarm_latest_trigger_sql = if(
  @alarm_latest_trigger_exists = 0,
  'alter table cost_alarm_record add column latest_trigger_time datetime null comment ''最近触发时间'' after first_trigger_time',
  'select 1'
);
prepare stmt from @alarm_latest_trigger_sql;
execute stmt;
deallocate prepare stmt;

set @alarm_occurrence_exists = (
  select count(1)
  from information_schema.columns
  where table_schema = database()
    and table_name = 'cost_alarm_record'
    and column_name = 'occurrence_count'
);
set @alarm_occurrence_sql = if(
  @alarm_occurrence_exists = 0,
  'alter table cost_alarm_record add column occurrence_count int not null default 1 comment ''累计触发次数'' after latest_trigger_time',
  'select 1'
);
prepare stmt from @alarm_occurrence_sql;
execute stmt;
deallocate prepare stmt;

update cost_alarm_record
set first_trigger_time = ifnull(first_trigger_time, trigger_time),
    latest_trigger_time = ifnull(latest_trigger_time, trigger_time),
    occurrence_count = ifnull(nullif(occurrence_count, 0), 1);

set @alarm_occurrence_index_exists = (
  select count(1)
  from information_schema.statistics
  where table_schema = database()
    and table_name = 'cost_alarm_record'
    and index_name = 'idx_cost_alarm_source_status'
);
set @alarm_occurrence_index_sql = if(
  @alarm_occurrence_index_exists = 0,
  'create index idx_cost_alarm_source_status on cost_alarm_record (source_key, alarm_status)',
  'select 1'
);
prepare stmt from @alarm_occurrence_index_sql;
execute stmt;
deallocate prepare stmt;

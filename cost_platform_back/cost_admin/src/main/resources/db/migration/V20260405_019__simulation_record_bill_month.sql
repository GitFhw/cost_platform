set @ddl = (
    select if(
        exists(
            select 1
            from information_schema.columns
            where table_schema = database()
              and table_name = 'cost_simulation_record'
              and column_name = 'bill_month'
        ),
        'select 1',
        'alter table cost_simulation_record add column bill_month varchar(16) not null default '''' comment ''bill month, yyyy-MM'' after version_id'
    )
);

prepare stmt from @ddl;
execute stmt;
deallocate prepare stmt;

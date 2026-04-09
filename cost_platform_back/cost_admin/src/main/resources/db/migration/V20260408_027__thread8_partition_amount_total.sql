set @ddl = (
    select if(
        exists(
            select 1
            from information_schema.columns
            where table_schema = database()
              and table_name = 'cost_calc_task_partition'
              and column_name = 'amount_total'
        ),
        'select 1',
        'alter table cost_calc_task_partition add column amount_total decimal(18,2) not null default 0.00 comment ''分片金额汇总'' after fail_count'
    )
);

prepare stmt from @ddl;
execute stmt;
deallocate prepare stmt;

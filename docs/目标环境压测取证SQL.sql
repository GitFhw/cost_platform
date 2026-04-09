-- 目标环境压测取证 SQL
-- 使用方式：
-- 1. 将 @request_no / @task_id 替换为本次压测的真实值
-- 2. 优先先按 request_no 查 task_id，再带 task_id 查分片与结果台账

set @request_no = 'TARGETPERF-SINGLE-500000-EXAMPLE';
set @task_id = 0;

-- 1) 任务头信息
select
    task_id,
    task_no,
    scene_id,
    version_id,
    task_type,
    bill_month,
    source_count,
    success_count,
    fail_count,
    task_status,
    progress_percent,
    execute_node,
    request_no,
    started_time,
    finished_time,
    duration_ms,
    create_time,
    update_time
from cost_calc_task
where request_no = @request_no
order by task_id desc;

-- 2) 如果已知 task_id，可直接看分片明细
select
    partition_id,
    task_id,
    partition_no,
    partition_status,
    total_count,
    processed_count,
    success_count,
    fail_count,
    execute_node,
    claim_time,
    started_time,
    finished_time,
    duration_ms,
    amount_total,
    persist_mode,
    recovery_hint,
    last_error_stage,
    last_error
from cost_calc_task_partition
where task_id = @task_id
order by partition_no asc;

-- 3) 分片 owner 分布
select
    execute_node,
    count(*) as partition_count,
    sum(total_count) as total_records,
    sum(success_count) as success_records,
    sum(fail_count) as fail_records,
    min(claim_time) as first_claim_time,
    max(finished_time) as last_finished_time
from cost_calc_task_partition
where task_id = @task_id
group by execute_node
order by partition_count desc, execute_node asc;

-- 4) 任务与分片状态汇总
select
    t.task_id,
    t.request_no,
    t.task_status,
    t.execute_node as task_owner,
    count(p.partition_id) as partition_count,
    sum(case when p.partition_status = 'SUCCESS' then 1 else 0 end) as success_partitions,
    sum(case when p.partition_status = 'FAILED' then 1 else 0 end) as failed_partitions,
    sum(case when p.partition_status = 'RUNNING' then 1 else 0 end) as running_partitions,
    sum(case when p.partition_status = 'INIT' then 1 else 0 end) as init_partitions,
    sum(p.total_count) as partition_total_count,
    sum(p.success_count) as partition_success_count,
    sum(p.fail_count) as partition_fail_count,
    sum(p.amount_total) as partition_amount_total
from cost_calc_task t
left join cost_calc_task_partition p on p.task_id = t.task_id
where t.task_id = @task_id
group by
    t.task_id,
    t.request_no,
    t.task_status,
    t.execute_node;

-- 5) 结果台账汇总
select
    task_id,
    count(*) as result_count,
    count(distinct fee_code) as fee_count,
    count(distinct object_code) as object_count,
    sum(amount_value) as amount_total,
    min(create_time) as first_result_time,
    max(create_time) as last_result_time
from cost_result_ledger
where task_id = @task_id
group by task_id;

-- 6) 结果台账按费用分布
select
    fee_code,
    fee_name,
    count(*) as result_count,
    sum(amount_value) as amount_total
from cost_result_ledger
where task_id = @task_id
group by fee_code, fee_name
order by fee_code asc;

-- 7) 结果台账按对象维度分布
select
    object_dimension,
    count(*) as result_count,
    count(distinct object_code) as object_count,
    sum(amount_value) as amount_total
from cost_result_ledger
where task_id = @task_id
group by object_dimension
order by object_dimension asc;

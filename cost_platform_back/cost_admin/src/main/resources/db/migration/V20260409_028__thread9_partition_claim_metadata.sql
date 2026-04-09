SET @cost_partition_execute_node_exists := (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'cost_calc_task_partition'
      AND COLUMN_NAME = 'execute_node'
);
SET @cost_partition_execute_node_sql := IF(
    @cost_partition_execute_node_exists = 0,
    'ALTER TABLE cost_calc_task_partition ADD COLUMN execute_node varchar(64) DEFAULT NULL COMMENT ''当前认领执行节点'' AFTER fail_count',
    'SELECT 1'
);
PREPARE cost_partition_execute_node_stmt FROM @cost_partition_execute_node_sql;
EXECUTE cost_partition_execute_node_stmt;
DEALLOCATE PREPARE cost_partition_execute_node_stmt;

SET @cost_partition_claim_time_exists := (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'cost_calc_task_partition'
      AND COLUMN_NAME = 'claim_time'
);
SET @cost_partition_claim_time_sql := IF(
    @cost_partition_claim_time_exists = 0,
    'ALTER TABLE cost_calc_task_partition ADD COLUMN claim_time datetime DEFAULT NULL COMMENT ''最近认领时间'' AFTER execute_node',
    'SELECT 1'
);
PREPARE cost_partition_claim_time_stmt FROM @cost_partition_claim_time_sql;
EXECUTE cost_partition_claim_time_stmt;
DEALLOCATE PREPARE cost_partition_claim_time_stmt;

SET @cost_partition_claim_index_exists := (
    SELECT COUNT(*)
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'cost_calc_task_partition'
      AND INDEX_NAME = 'idx_cost_calc_task_partition_status_claim'
);
SET @cost_partition_claim_index_sql := IF(
    @cost_partition_claim_index_exists = 0,
    'ALTER TABLE cost_calc_task_partition ADD KEY idx_cost_calc_task_partition_status_claim (partition_status, claim_time)',
    'SELECT 1'
);
PREPARE cost_partition_claim_index_stmt FROM @cost_partition_claim_index_sql;
EXECUTE cost_partition_claim_index_stmt;
DEALLOCATE PREPARE cost_partition_claim_index_stmt;

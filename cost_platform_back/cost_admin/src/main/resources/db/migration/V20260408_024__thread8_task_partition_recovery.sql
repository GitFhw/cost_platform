SET @cost_partition_recovery_persist_mode_exists := (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'cost_calc_task_partition'
      AND COLUMN_NAME = 'persist_mode'
);
SET @cost_partition_recovery_persist_mode_sql := IF(
    @cost_partition_recovery_persist_mode_exists = 0,
    'ALTER TABLE cost_calc_task_partition ADD COLUMN persist_mode varchar(32) NOT NULL DEFAULT ''BATCH'' COMMENT ''结果落库模式'' AFTER fail_count',
    'SELECT 1'
);
PREPARE cost_partition_recovery_persist_mode_stmt FROM @cost_partition_recovery_persist_mode_sql;
EXECUTE cost_partition_recovery_persist_mode_stmt;
DEALLOCATE PREPARE cost_partition_recovery_persist_mode_stmt;

SET @cost_partition_recovery_hint_exists := (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'cost_calc_task_partition'
      AND COLUMN_NAME = 'recovery_hint'
);
SET @cost_partition_recovery_hint_sql := IF(
    @cost_partition_recovery_hint_exists = 0,
    'ALTER TABLE cost_calc_task_partition ADD COLUMN recovery_hint varchar(500) NOT NULL DEFAULT '''' COMMENT ''恢复提示'' AFTER persist_mode',
    'SELECT 1'
);
PREPARE cost_partition_recovery_hint_stmt FROM @cost_partition_recovery_hint_sql;
EXECUTE cost_partition_recovery_hint_stmt;
DEALLOCATE PREPARE cost_partition_recovery_hint_stmt;

SET @cost_partition_recovery_stage_exists := (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'cost_calc_task_partition'
      AND COLUMN_NAME = 'last_error_stage'
);
SET @cost_partition_recovery_stage_sql := IF(
    @cost_partition_recovery_stage_exists = 0,
    'ALTER TABLE cost_calc_task_partition ADD COLUMN last_error_stage varchar(64) NOT NULL DEFAULT '''' COMMENT ''最近错误阶段'' AFTER recovery_hint',
    'SELECT 1'
);
PREPARE cost_partition_recovery_stage_stmt FROM @cost_partition_recovery_stage_sql;
EXECUTE cost_partition_recovery_stage_stmt;
DEALLOCATE PREPARE cost_partition_recovery_stage_stmt;

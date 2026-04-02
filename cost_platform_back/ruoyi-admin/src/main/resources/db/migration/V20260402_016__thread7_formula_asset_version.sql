drop procedure if exists proc_upgrade_cost_formula_assets;
delimiter $$
create procedure proc_upgrade_cost_formula_assets()
begin
    if not exists (
        select 1
        from information_schema.columns
        where table_schema = database()
          and table_name = 'cost_formula'
          and column_name = 'asset_type'
    ) then
        alter table cost_formula
            add column asset_type varchar(32) default 'FORMULA' comment '资产类型（FORMULA/TEMPLATE）' after formula_expr;
    end if;
end $$
delimiter ;
call proc_upgrade_cost_formula_assets();
drop procedure if exists proc_upgrade_cost_formula_assets;

create table if not exists cost_formula_version
(
    version_id             bigint          not null auto_increment comment '版本主键',
    formula_id             bigint          not null comment '公式主键',
    scene_id               bigint          not null comment '场景主键',
    formula_code           varchar(64)     not null comment '公式编码',
    formula_name           varchar(128)    not null comment '公式名称',
    asset_type             varchar(32)     default 'FORMULA' comment '资产类型（FORMULA/TEMPLATE）',
    version_no             int             not null comment '版本号',
    change_type            varchar(32)     default 'UPDATE' comment '变更类型（CREATE/UPDATE）',
    business_formula       varchar(1000)   default '' comment '业务中文公式',
    formula_expr           varchar(2000)   default '' comment '标准执行表达式',
    workbench_mode         varchar(32)     default 'GUIDED' comment '工作台模式',
    workbench_pattern      varchar(32)     default 'IF_ELSE' comment '工作台结构类型',
    template_code          varchar(64)     default '' comment '模板编码',
    workbench_config_json  json            default null comment '工作台配置',
    snapshot_json          json            default null comment '完整版本快照',
    create_by              varchar(64)     default '' comment '创建人',
    create_time            datetime        default current_timestamp comment '创建时间',
    primary key (version_id),
    unique key uk_cost_formula_version_no (formula_id, version_no),
    key idx_cost_formula_version_scene (scene_id, formula_code),
    key idx_cost_formula_version_formula (formula_id, create_time)
) engine = innodb comment = '公式版本台账';

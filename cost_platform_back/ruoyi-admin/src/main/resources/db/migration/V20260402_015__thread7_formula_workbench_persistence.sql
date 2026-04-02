-- 线程七：公式实验室工作台点选配置持久化

drop procedure if exists proc_add_cost_formula_workbench_columns;
delimiter $$
create procedure proc_add_cost_formula_workbench_columns()
begin
    if not exists (
        select 1
        from information_schema.columns
        where table_schema = database()
          and table_name = 'cost_formula'
          and column_name = 'workbench_mode'
    ) then
        alter table cost_formula
            add column workbench_mode varchar(32) default 'GUIDED' comment '工作台模式（GUIDED/EXPERT）' after formula_expr;
    end if;

    if not exists (
        select 1
        from information_schema.columns
        where table_schema = database()
          and table_name = 'cost_formula'
          and column_name = 'workbench_pattern'
    ) then
        alter table cost_formula
            add column workbench_pattern varchar(32) default 'IF_ELSE' comment '工作台结构类型' after workbench_mode;
    end if;

    if not exists (
        select 1
        from information_schema.columns
        where table_schema = database()
          and table_name = 'cost_formula'
          and column_name = 'template_code'
    ) then
        alter table cost_formula
            add column template_code varchar(64) default '' comment '工作台模板编码' after workbench_pattern;
    end if;

    if not exists (
        select 1
        from information_schema.columns
        where table_schema = database()
          and table_name = 'cost_formula'
          and column_name = 'workbench_config_json'
    ) then
        alter table cost_formula
            add column workbench_config_json json default null comment '工作台点选配置' after template_code;
    end if;
end $$
delimiter ;
call proc_add_cost_formula_workbench_columns();
drop procedure if exists proc_add_cost_formula_workbench_columns;

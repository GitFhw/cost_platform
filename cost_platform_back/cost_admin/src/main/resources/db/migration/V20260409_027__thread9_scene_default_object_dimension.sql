set @scene_dimension_column_exists = (
    select count(*)
    from information_schema.columns
    where table_schema = database()
      and table_name = 'cost_scene'
      and column_name = 'default_object_dimension'
);

set @scene_dimension_column_sql = if(
    @scene_dimension_column_exists = 0,
    "alter table cost_scene add column default_object_dimension varchar(64) default '' comment '场景默认对象维度'",
    'select 1'
);

prepare stmt_scene_dimension from @scene_dimension_column_sql;
execute stmt_scene_dimension;
deallocate prepare stmt_scene_dimension;

update cost_scene scene
join (
    select scene_id, min(object_dimension) as default_object_dimension
    from cost_fee_item
    where coalesce(object_dimension, '') <> ''
    group by scene_id
    having count(distinct object_dimension) = 1
) summary on summary.scene_id = scene.scene_id
set scene.default_object_dimension = summary.default_object_dimension
where coalesce(scene.default_object_dimension, '') = '';

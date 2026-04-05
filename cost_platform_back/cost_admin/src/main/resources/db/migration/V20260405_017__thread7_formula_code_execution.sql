-- 线程七：历史公式表达式全面迁移到 formulaCode 执行链

insert into cost_formula (
    scene_id,
    formula_code,
    formula_name,
    formula_desc,
    business_formula,
    formula_expr,
    namespace_scope,
    return_type,
    status,
    sort_no,
    remark,
    create_by,
    create_time,
    update_by,
    update_time
)
select v.scene_id,
       ifnull(nullif(v.formula_code, ''), concat('LEGACY_VAR_', upper(substr(md5(concat(v.scene_id, ':VAR:', v.variable_code)), 1, 12)))),
       concat(v.variable_name, '-历史迁移公式'),
       concat('历史公式变量迁移自 ', v.variable_code),
       v.variable_name,
       v.formula_expr,
       'V,C,I,F,T',
       case
           when upper(ifnull(v.data_type, '')) = 'BOOLEAN' then 'BOOLEAN'
           when upper(ifnull(v.data_type, '')) in ('STRING', 'TEXT', 'DICT', 'DATE', 'DATETIME') then 'STRING'
           when upper(ifnull(v.data_type, '')) = 'JSON' then 'JSON'
           else 'NUMBER'
       end,
       ifnull(v.status, '0'),
       ifnull(v.sort_no, 10),
       '历史公式变量自动迁移生成',
       'flyway',
       sysdate(),
       'flyway',
       sysdate()
from cost_variable v
where v.source_type = 'FORMULA'
  and ifnull(v.formula_expr, '') <> ''
  and not exists (
    select 1
    from cost_formula f
    where f.scene_id = v.scene_id
      and f.formula_code = ifnull(nullif(v.formula_code, ''), concat('LEGACY_VAR_', upper(substr(md5(concat(v.scene_id, ':VAR:', v.variable_code)), 1, 12))))
  );

update cost_variable v
set v.formula_code = concat('LEGACY_VAR_', upper(substr(md5(concat(v.scene_id, ':VAR:', v.variable_code)), 1, 12)))
where v.source_type = 'FORMULA'
  and ifnull(v.formula_expr, '') <> ''
  and ifnull(v.formula_code, '') = '';

insert into cost_formula (
    scene_id,
    formula_code,
    formula_name,
    formula_desc,
    business_formula,
    formula_expr,
    namespace_scope,
    return_type,
    status,
    sort_no,
    remark,
    create_by,
    create_time,
    update_by,
    update_time
)
select r.scene_id,
       ifnull(nullif(r.amount_formula_code, ''), concat('LEGACY_RULE_', upper(substr(md5(concat(r.scene_id, ':RULE:', r.rule_code)), 1, 12)))),
       concat(r.rule_name, '-历史金额公式'),
       concat('历史公式金额规则迁移自 ', r.rule_code),
       ifnull(nullif(r.rule_name, ''), r.rule_code),
       r.amount_formula,
       'V,C,I,F,T',
       'NUMBER',
       ifnull(r.status, '0'),
       ifnull(r.sort_no, 10),
       '历史公式金额规则自动迁移生成',
       'flyway',
       sysdate(),
       'flyway',
       sysdate()
from cost_rule r
where r.rule_type = 'FORMULA'
  and ifnull(r.amount_formula, '') <> ''
  and not exists (
    select 1
    from cost_formula f
    where f.scene_id = r.scene_id
      and f.formula_code = ifnull(nullif(r.amount_formula_code, ''), concat('LEGACY_RULE_', upper(substr(md5(concat(r.scene_id, ':RULE:', r.rule_code)), 1, 12))))
  );

update cost_rule r
set r.amount_formula_code = concat('LEGACY_RULE_', upper(substr(md5(concat(r.scene_id, ':RULE:', r.rule_code)), 1, 12)))
where r.rule_type = 'FORMULA'
  and ifnull(r.amount_formula, '') <> ''
  and ifnull(r.amount_formula_code, '') = '';

insert into cost_publish_snapshot (
    version_id,
    snapshot_type,
    object_code,
    object_name,
    snapshot_json,
    sort_no,
    create_by,
    create_time
)
select ps.version_id,
       'FORMULA',
       ifnull(nullif(json_unquote(json_extract(ps.snapshot_json, '$.formulaCode')), ''), concat('LEGACY_VAR_', upper(substr(md5(concat(pv.scene_id, ':VAR:', ps.object_code)), 1, 12)))),
       concat(ifnull(json_unquote(json_extract(ps.snapshot_json, '$.variableName')), ps.object_code), '-历史迁移公式'),
       json_object(
           'formulaCode', ifnull(nullif(json_unquote(json_extract(ps.snapshot_json, '$.formulaCode')), ''), concat('LEGACY_VAR_', upper(substr(md5(concat(pv.scene_id, ':VAR:', ps.object_code)), 1, 12)))),
           'formulaName', concat(ifnull(json_unquote(json_extract(ps.snapshot_json, '$.variableName')), ps.object_code), '-历史迁移公式'),
           'formulaDesc', concat('历史发布快照公式变量迁移自 ', ps.object_code),
           'businessFormula', ifnull(json_unquote(json_extract(ps.snapshot_json, '$.variableName')), ps.object_code),
           'formulaExpr', json_unquote(json_extract(ps.snapshot_json, '$.formulaExpr')),
           'namespaceScope', 'V,C,I,F,T',
           'returnType',
               case
                   when upper(ifnull(json_unquote(json_extract(ps.snapshot_json, '$.dataType')), '')) = 'BOOLEAN' then 'BOOLEAN'
                   when upper(ifnull(json_unquote(json_extract(ps.snapshot_json, '$.dataType')), '')) in ('STRING', 'TEXT', 'DICT', 'DATE', 'DATETIME') then 'STRING'
                   when upper(ifnull(json_unquote(json_extract(ps.snapshot_json, '$.dataType')), '')) = 'JSON' then 'JSON'
                   else 'NUMBER'
               end,
           'sortNo', ifnull(cast(json_unquote(json_extract(ps.snapshot_json, '$.sortNo')) as signed), 10),
           'status', ifnull(json_unquote(json_extract(ps.snapshot_json, '$.status')), '0'),
           'remark', '历史发布快照中的公式变量自动迁移生成'
       ),
       ifnull(ps.sort_no, 1000) + 500,
       'flyway',
       sysdate()
from cost_publish_snapshot ps
join cost_publish_version pv on pv.version_id = ps.version_id
where ps.snapshot_type = 'VARIABLE'
  and ifnull(json_unquote(json_extract(ps.snapshot_json, '$.sourceType')), '') = 'FORMULA'
  and ifnull(json_unquote(json_extract(ps.snapshot_json, '$.formulaExpr')), '') <> ''
  and not exists (
    select 1
    from cost_publish_snapshot s2
    where s2.version_id = ps.version_id
      and s2.snapshot_type = 'FORMULA'
      and s2.object_code = ifnull(nullif(json_unquote(json_extract(ps.snapshot_json, '$.formulaCode')), ''), concat('LEGACY_VAR_', upper(substr(md5(concat(pv.scene_id, ':VAR:', ps.object_code)), 1, 12))))
  );

update cost_publish_snapshot ps
join cost_publish_version pv on pv.version_id = ps.version_id
set ps.snapshot_json = json_set(
    ps.snapshot_json,
    '$.formulaCode',
    concat('LEGACY_VAR_', upper(substr(md5(concat(pv.scene_id, ':VAR:', ps.object_code)), 1, 12)))
)
where ps.snapshot_type = 'VARIABLE'
  and ifnull(json_unquote(json_extract(ps.snapshot_json, '$.sourceType')), '') = 'FORMULA'
  and ifnull(json_unquote(json_extract(ps.snapshot_json, '$.formulaExpr')), '') <> ''
  and ifnull(json_unquote(json_extract(ps.snapshot_json, '$.formulaCode')), '') = '';

insert into cost_publish_snapshot (
    version_id,
    snapshot_type,
    object_code,
    object_name,
    snapshot_json,
    sort_no,
    create_by,
    create_time
)
select ps.version_id,
       'FORMULA',
       ifnull(nullif(json_unquote(json_extract(ps.snapshot_json, '$.amountFormulaCode')), ''), concat('LEGACY_RULE_', upper(substr(md5(concat(pv.scene_id, ':RULE:', ps.object_code)), 1, 12)))),
       concat(ifnull(json_unquote(json_extract(ps.snapshot_json, '$.ruleName')), ps.object_code), '-历史金额公式'),
       json_object(
           'formulaCode', ifnull(nullif(json_unquote(json_extract(ps.snapshot_json, '$.amountFormulaCode')), ''), concat('LEGACY_RULE_', upper(substr(md5(concat(pv.scene_id, ':RULE:', ps.object_code)), 1, 12)))),
           'formulaName', concat(ifnull(json_unquote(json_extract(ps.snapshot_json, '$.ruleName')), ps.object_code), '-历史金额公式'),
           'formulaDesc', concat('历史发布快照公式金额规则迁移自 ', ps.object_code),
           'businessFormula', ifnull(nullif(json_unquote(json_extract(ps.snapshot_json, '$.amountBusinessFormula')), ''), ifnull(json_unquote(json_extract(ps.snapshot_json, '$.ruleName')), ps.object_code)),
           'formulaExpr', json_unquote(json_extract(ps.snapshot_json, '$.amountFormula')),
           'namespaceScope', 'V,C,I,F,T',
           'returnType', 'NUMBER',
           'sortNo', ifnull(cast(json_unquote(json_extract(ps.snapshot_json, '$.sortNo')) as signed), 10),
           'status', ifnull(json_unquote(json_extract(ps.snapshot_json, '$.status')), '0'),
           'remark', '历史发布快照中的公式金额规则自动迁移生成'
       ),
       ifnull(ps.sort_no, 2000) - 300,
       'flyway',
       sysdate()
from cost_publish_snapshot ps
join cost_publish_version pv on pv.version_id = ps.version_id
where ps.snapshot_type = 'RULE'
  and ifnull(json_unquote(json_extract(ps.snapshot_json, '$.ruleType')), '') = 'FORMULA'
  and ifnull(json_unquote(json_extract(ps.snapshot_json, '$.amountFormula')), '') <> ''
  and not exists (
    select 1
    from cost_publish_snapshot s2
    where s2.version_id = ps.version_id
      and s2.snapshot_type = 'FORMULA'
      and s2.object_code = ifnull(nullif(json_unquote(json_extract(ps.snapshot_json, '$.amountFormulaCode')), ''), concat('LEGACY_RULE_', upper(substr(md5(concat(pv.scene_id, ':RULE:', ps.object_code)), 1, 12))))
  );

update cost_publish_snapshot ps
join cost_publish_version pv on pv.version_id = ps.version_id
set ps.snapshot_json = json_set(
    ps.snapshot_json,
    '$.amountFormulaCode',
    concat('LEGACY_RULE_', upper(substr(md5(concat(pv.scene_id, ':RULE:', ps.object_code)), 1, 12)))
)
where ps.snapshot_type = 'RULE'
  and ifnull(json_unquote(json_extract(ps.snapshot_json, '$.ruleType')), '') = 'FORMULA'
  and ifnull(json_unquote(json_extract(ps.snapshot_json, '$.amountFormula')), '') <> ''
  and ifnull(json_unquote(json_extract(ps.snapshot_json, '$.amountFormulaCode')), '') = '';

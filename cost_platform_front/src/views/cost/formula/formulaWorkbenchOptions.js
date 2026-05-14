export const assetTypeOptions = [
  { label: '公式资产', value: 'FORMULA' },
  { label: '模板资产', value: 'TEMPLATE' }
]

export const builderModes = [
  { label: '业务公式', value: 'BUSINESS' },
  { label: '结构助手', value: 'GUIDED' }
]

export const conditionOperators = [
  { label: '等于', value: 'EQ' },
  { label: '不等于', value: 'NE' },
  { label: '大于', value: 'GT' },
  { label: '大于等于', value: 'GE' },
  { label: '小于', value: 'LT' },
  { label: '小于等于', value: 'LE' }
]

export const operatorButtons = [
  { label: '+', value: ' + ' },
  { label: '-', value: ' - ' },
  { label: '×', value: ' × ' },
  { label: '÷', value: ' ÷ ' },
  { label: '%', value: '%' },
  { label: '（', value: '（' },
  { label: '）', value: '）' },
  { label: '，', value: '，' }
]

export const functionButtons = [
  { label: '四舍五入', value: '四舍五入(, 2)', desc: '结果保留指定精度。' },
  { label: '最大值', value: '最大值(, )', desc: '适合保底值与上限比较。' },
  { label: '最小值', value: '最小值(, )', desc: '适合封顶值与下限比较。' },
  { label: '空值兜底', value: '空值兜底(, )', desc: '空值时回退到默认值。' }
]

export const keywordButtons = [
  { label: '如果', value: '如果 ' },
  { label: '那么', value: ' 那么 ' },
  { label: '否则', value: ' 否则 ' },
  { label: '否则如果', value: ' 否则如果 ' },
  { label: '且', value: ' 且 ' },
  { label: '并且', value: ' 并且 ' },
  { label: '或', value: ' 或 ' },
  { label: '或者', value: ' 或者 ' },
  { label: '属于', value: ' 属于 ' },
  { label: '不属于', value: ' 不属于 ' },
  { label: '等于', value: ' 等于 ' },
  { label: '不等于', value: ' 不等于 ' },
  { label: '大于', value: ' 大于 ' },
  { label: '大于等于', value: ' 大于等于 ' },
  { label: '小于', value: ' 小于 ' },
  { label: '小于等于', value: ' 小于等于 ' }
]

export const businessTemplateButtons = [
  {
    label: '条件骨架',
    value: '如果 条件 那么 结果 否则 结果',
    placeholder: '条件',
    desc: '适合先搭出如果/那么/否则结构，再逐段替换条件和结果。'
  },
  {
    label: '双分支判断',
    value: '如果 条件一 那么 结果一 否则如果 条件二 那么 结果二 否则 结果',
    placeholder: '条件一',
    desc: '适合班次、货种、作业类型等多条件组合取值场景。'
  },
  {
    label: '属于判断',
    value: '如果 字段 属于 取值1/取值2 那么 结果 否则 结果',
    placeholder: '字段',
    desc: '适合货种、动作、班次等集合判断场景。'
  },
  {
    label: '不属于判断',
    value: '如果 字段 不属于 取值1/取值2 那么 结果 否则 结果',
    placeholder: '字段',
    desc: '适合排除类条件口径。'
  },
  {
    label: '账期判断',
    value: '如果 账期属于 12/01/02/03 那么 1 否则 0',
    placeholder: '12/01/02/03',
    desc: '适合按账期月份集合做条件判断。'
  }
]

export const numberButtons = [
  { label: '7', value: '7' },
  { label: '8', value: '8' },
  { label: '9', value: '9' },
  { label: '4', value: '4' },
  { label: '5', value: '5' },
  { label: '6', value: '6' },
  { label: '1', value: '1' },
  { label: '2', value: '2' },
  { label: '3', value: '3' },
  { label: '0', value: '0' },
  { label: '00', value: '00' },
  { label: '.', value: '.' }
]

export const namespaceTokens = [
  { label: 'V.变量', value: 'V.', type: 'warning', desc: '标准变量上下文' },
  { label: 'I.输入', value: 'I.', type: 'warning', desc: '原始输入 JSON' },
  { label: 'C.上下文', value: 'C.', type: 'warning', desc: '场景/版本/账期等运行信息' },
  { label: 'F.费用结果', value: 'F.', type: 'warning', desc: '前序费用结果与试算费用对象' },
  { label: 'T.临时值', value: 'T.', type: 'warning', desc: '预留临时变量空间' }
]

export const contextFieldButtons = [
  { label: '账期', value: '账期', desc: '映射为运行上下文 C.billMonth' }
]

export const platformTemplates = [
  {
    code: 'CARGO_SHIFT_PRICE',
    name: '货种 + 班次计价',
    desc: '适合“货种 = 煤炭 且 班次 = 白班时取 2 元，否则取 1 元”这类业务口径。',
    pattern: 'IF_ELSE',
    conditions: [
      { variableCode: '', operatorCode: 'EQ', compareValue: '' },
      { variableCode: '', operatorCode: 'EQ', compareValue: '' }
    ],
    trueResultValue: '2',
    falseResultValue: '1'
  },
  {
    code: 'RANGE_RATE',
    name: '区间档位取价',
    desc: '适合按天数、重量、面积等数值做区间价。',
    pattern: 'RANGE_LOOKUP',
    ranges: [
      { startValue: '0', endValue: '10', resultValue: '1' },
      { startValue: '10', endValue: '20', resultValue: '2' }
    ],
    defaultResultValue: '0'
  },
  {
    code: 'KEEP_AMOUNT',
    name: '面积 × 天数 × 单价',
    desc: '适合仓储保管费类金额公式，直接用中文公式编排即可。',
    pattern: 'BUSINESS',
    businessFormula: '面积 × 天数 × 单价'
  }
]

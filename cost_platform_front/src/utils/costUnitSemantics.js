const DEFAULT_SEMANTIC = {
  summary: '用于表达费用的计价口径，规则中心的计量变量应与该单位保持一致。',
  quantityHint: '计量变量建议沉淀与计价单位一致的数量口径。',
  priceHint: '单价表示每计价单位对应的价格。',
  resultHint: '结果台账会结合数量、单价和金额解释当前费用的计价过程。'
}

const UNIT_SEMANTIC_MAP = {
  '吨': {
    summary: '按重量计价，适合港杂、装卸、仓储等按吨计费场景。',
    quantityHint: '计量变量应表达重量吨数，例如装卸吨数、仓储吨数。',
    priceHint: '单价表示每吨对应的价格。',
    resultHint: '结果解释建议按“重量吨数 × 每吨单价 = 金额”理解。'
  },
  '天': {
    summary: '按时间计价，适合滞期、堆存、保管等按天计费场景。',
    quantityHint: '计量变量应表达天数，例如在场天数、占用天数。',
    priceHint: '单价表示每天对应的价格。',
    resultHint: '结果解释建议按“天数 × 每天单价 = 金额”理解。'
  },
  '次': {
    summary: '按次数计价，适合装卸次数、作业次数等计费场景。',
    quantityHint: '计量变量应表达次数，例如操作次数、调用次数。',
    priceHint: '单价表示每次对应的价格。',
    resultHint: '结果解释建议按“次数 × 每次单价 = 金额”理解。'
  },
  '航次': {
    summary: '按航次计价，适合船舶、港口等围绕航次发生的费用。',
    quantityHint: '计量变量应表达航次数量，通常为 1 或多航次累计值。',
    priceHint: '单价表示每航次对应的价格。',
    resultHint: '结果解释建议按“航次数量 × 每航次单价 = 金额”理解。'
  },
  '人': {
    summary: '按人数计价，适合人工、驻场、值守等费用场景。',
    quantityHint: '计量变量应表达人数或人次，并在业务口径中说明含义。',
    priceHint: '单价表示每人对应的价格。',
    resultHint: '结果解释建议按“人数 × 每人单价 = 金额”理解。'
  },
  '箱': {
    summary: '按箱量计价，适合集装箱、箱区作业等费用场景。',
    quantityHint: '计量变量应表达箱量，例如自然箱、标箱或箱次。',
    priceHint: '单价表示每箱对应的价格。',
    resultHint: '结果解释建议按“箱量 × 每箱单价 = 金额”理解。'
  },
  '元': {
    summary: '按固定金额计价，通常用于一次性收费或不再乘数量的费用。',
    quantityHint: '固定金额类规则通常不依赖计量变量，若配置数量应仅用于解释。',
    priceHint: '单价本身就是最终金额，不再按数量累乘。',
    resultHint: '结果解释建议按“直接取固定金额”理解。'
  },
  '平方米*天': {
    summary: '按面积天复合量计价，常用于仓储保管等面积与时间共同决定的费用。',
    quantityHint: '计量变量应表达面积天数量，例如“占用面积 × 占用天数”。',
    priceHint: '单价表示每平方米*天对应的价格。',
    resultHint: '结果解释建议按“面积天数量 × 单价 = 金额”理解。'
  }
}

function normalizeUnitKey(unitCode, unitLabel) {
  const raw = String(unitLabel || unitCode || '').replace(/\s+/g, '')
  if (!raw) {
    return ''
  }
  if (['平方米*天', '平方米·天', '平方米•天', '平方米x天', '平方米X天', '平方米/天', '平方米天'].includes(raw)) {
    return '平方米*天'
  }
  return raw
}

export function getCostUnitSemantic(unitCode, unitLabel) {
  const key = normalizeUnitKey(unitCode, unitLabel)
  if (!key) {
    return {
      unitKey: '',
      unitLabel: unitLabel || unitCode || '-',
      ...DEFAULT_SEMANTIC
    }
  }
  return {
    unitKey: key,
    unitLabel: unitLabel || unitCode || key,
    ...DEFAULT_SEMANTIC,
    ...(UNIT_SEMANTIC_MAP[key] || {})
  }
}

const CHINESE_FUNCTION_REPLACERS = [
  { pattern: /四舍五入\s*\(/g, replacement: 'round(' },
  { pattern: /最大值\s*\(/g, replacement: 'max(' },
  { pattern: /最小值\s*\(/g, replacement: 'min(' },
  { pattern: /空值兜底\s*\(/g, replacement: 'coalesce(' }
]

const CHINESE_SYMBOL_REPLACERS = [
  [/（/g, '('],
  [/）/g, ')'],
  [/，/g, ','],
  [/；/g, ';'],
  [/【/g, '('],
  [/】/g, ')'],
  [/“|”/g, '\''],
  [/‘|’/g, '\''],
  [/×/g, ' * '],
  [/÷/g, ' / ']
]

const CONDITION_OPERATOR_REPLACERS = [
  [/不等于/g, ' != '],
  [/大于等于/g, ' >= '],
  [/小于等于/g, ' <= '],
  [/等于/g, ' == '],
  [/大于/g, ' > '],
  [/小于/g, ' < ']
]

const BILL_MONTH_MEMBERSHIP_PATTERN = /账期属于\s*([0-9]{1,2}(?:\s*[\/、,，]\s*[0-9]{1,2})+)/g

function createIssue(message, level = 'error', extra = {}) {
  return {
    message,
    level,
    code: extra.code || 'BUSINESS_FORMULA',
    fragment: extra.fragment || '',
    suggestion: extra.suggestion || ''
  }
}

function normalizeBusinessText(source) {
  let text = String(source || '').trim()
  CHINESE_SYMBOL_REPLACERS.forEach(([pattern, replacement]) => {
    text = text.replace(pattern, replacement)
  })
  return text
    .replace(/若账期属于/g, '如果 账期属于 ')
    .replace(/如果(?=[^\s(（])/g, '如果 ')
    .replace(/(^|[\s,，(（])若(?=[^\s])/g, '$1如果 ')
    .replace(/(^|[\s,，(（])若(?=\s)/g, '$1如果')
    .replace(/否则(?=[^\s)）])/g, '否则 ')
    .replace(/否则(?:为|取)/g, '否则 ')
    .replace(/那么(?=[^\s(（])/g, '那么 ')
    .replace(/那么(?:为|取)/g, '那么 ')
    .replace(/(^|[\s,，])则(?=[^\s])/g, '$1那么 ')
    .replace(/(^|[\s,，])则(?=\s)/g, '$1那么')
    .replace(/\s+/g, ' ')
    .trim()
}

function normalizePercentageLiterals(source) {
  return String(source || '').replace(/(-?\d+(?:\.\d+)?)\s*(?:%|％)/g, '($1 / 100)')
}

function buildVariableTokens(variableOptions = [], feeOptions = []) {
  const variableTokens = (variableOptions || [])
    .filter(item => item?.variableCode && item?.variableName)
    .map(item => ({
      type: 'variable',
      namespace: 'V',
      code: item.variableCode,
      name: String(item.variableName).trim()
    }))
    .filter(item => item.name)
  const feeTokens = (feeOptions || [])
    .filter(item => item?.feeCode && item?.feeName)
    .map(item => ({
      type: 'fee',
      namespace: 'F',
      code: item.feeCode,
      name: String(item.feeName).trim()
    }))
    .filter(item => item.name)
  return [...variableTokens, ...feeTokens]
    .sort((a, b) => b.name.length - a.name.length)
}

function replaceVariables(text, variableTokens) {
  let output = text
  const placeholders = []
  variableTokens.forEach((item, index) => {
    if (!output.includes(item.name)) {
      return
    }
    const placeholder = `__VAR_${index}__`
    output = output.split(item.name).join(placeholder)
    placeholders.push({ ...item, placeholder })
  })
  placeholders.forEach(item => {
    output = output.split(item.placeholder).join(`${item.namespace}.${item.code}`)
  })
  return {
    text: output,
    variableRefs: placeholders.filter(item => item.type === 'variable').map(item => item.code),
    feeRefs: placeholders.filter(item => item.type === 'fee').map(item => item.code)
  }
}

function stripQuotedSegments(text) {
  return String(text || '').replace(/'([^'\\]|\\.)*'/g, '')
}

function normalizePlainExpression(text) {
  let output = text
  CHINESE_FUNCTION_REPLACERS.forEach(item => {
    output = output.replace(item.pattern, item.replacement)
  })
  return normalizePercentageLiterals(output)
    .replace(/\s+/g, ' ')
    .replace(/\s*,\s*/g, ', ')
    .trim()
}

function hasUnsupportedChineseToken(expression) {
  return /[\u4e00-\u9fa5]/.test(stripQuotedSegments(expression))
}

function compileLiteralOrExpression(source, variableTokens) {
  const text = normalizeBusinessText(source)
  if (!text) {
    return {
      expression: '',
      variableRefs: [],
      feeRefs: [],
      issues: [createIssue('中文公式片段不能为空，请先补齐公式内容。', 'error', {
        code: 'EMPTY_EXPRESSION',
        suggestion: '请至少输入一个变量、费用、数字、中文函数或完整条件结构。'
      })]
    }
  }

  const replaced = replaceVariables(text, variableTokens)
  const normalized = normalizePlainExpression(replaced.text)

  if (!normalized) {
    return {
      expression: '',
      variableRefs: [],
      feeRefs: [],
      issues: [createIssue('中文公式片段不能为空，请先补齐公式内容。', 'error', {
        code: 'EMPTY_EXPRESSION',
        suggestion: '请至少输入一个变量、费用、数字、中文函数或完整条件结构。'
      })]
    }
  }

  if (/^-?\d+(?:\.\d+)?$/.test(normalized)) {
    return {
      expression: normalized,
      variableRefs: [],
      feeRefs: [],
      issues: []
    }
  }

  if (!replaced.variableRefs.length && !replaced.feeRefs.length && !/[()%*/+\-<>=,]/.test(normalized) && !/^(round|max|min|coalesce|if)\(/.test(normalized)) {
    return {
      expression: /^'.*'$/.test(normalized) ? normalized : `'${normalized.replace(/'/g, "\\'")}'`,
      variableRefs: [],
      feeRefs: [],
      issues: []
    }
  }

  if (hasUnsupportedChineseToken(normalized)) {
    const unsupportedFragments = extractUnsupportedFragments(normalized)
    return {
      expression: '',
      variableRefs: replaced.variableRefs,
      feeRefs: replaced.feeRefs,
      issues: [createIssue(`存在无法识别的中文片段：${unsupportedFragments.join('、')}，请使用变量名、费用名、中文函数或结构助手。`, 'error', {
        code: 'UNSUPPORTED_FRAGMENT',
        fragment: unsupportedFragments[0] || '',
        suggestion: '可改为变量名称、费用名称、中文函数，或切换到结构助手维护复杂逻辑。'
      })]
    }
  }

  return {
    expression: normalized,
    variableRefs: replaced.variableRefs,
    feeRefs: replaced.feeRefs,
    issues: []
  }
}

function extractUnsupportedFragments(expression) {
  const matches = stripQuotedSegments(expression).match(/[\u4e00-\u9fa5]+/g) || []
  return [...new Set(matches)]
}

function normalizeBillMonthMembership(source) {
  return String(source || '').replace(BILL_MONTH_MEMBERSHIP_PATTERN, (_, rawMonths) => {
    const monthTokens = String(rawMonths || '')
      .split(/[\/、,，]/)
      .map(item => item.trim())
      .filter(Boolean)
      .map(item => item.padStart(2, '0'))
    if (!monthTokens.length) {
      return _
    }
    return `C.billMonth matches '.*-(${[...new Set(monthTokens)].join('|')})$'`
  })
}

function normalizeConditionText(source) {
  let text = normalizeBusinessText(source)
  text = normalizeBillMonthMembership(text)
  CONDITION_OPERATOR_REPLACERS.forEach(([pattern, replacement]) => {
    text = text.replace(pattern, replacement)
  })
  text = normalizePercentageLiterals(text)
    .replace(/\b且\b/g, ' and ')
    .replace(/\b或\b/g, ' or ')
    .replace(/(?<![><!=])=(?!=)/g, ' == ')
    .replace(/\s+/g, ' ')
    .trim()
  return text
}

function compileConditionExpression(source, variableTokens) {
  const text = normalizeConditionText(source)
  const replaced = replaceVariables(text, variableTokens)
  const expression = replaced.text

  if (!expression) {
    return {
      expression: '',
      variableRefs: [],
      feeRefs: [],
      issues: [createIssue('条件片段不能为空，请先补齐“如果”后的判断内容。', 'error', {
        code: 'EMPTY_CONDITION',
        suggestion: '请在“如果”后填写完整判断条件，例如“女工应出勤 小于等于 0”。'
      })]
    }
  }

  if (!/(==|!=|>=|<=|>|<|\bmatches\b)/.test(expression)) {
    return {
      expression: '',
      variableRefs: replaced.variableRefs,
      feeRefs: replaced.feeRefs,
      issues: [createIssue('条件片段缺少比较操作符，请使用 =、!=、>、>=、<、<= 或中文比较词。', 'error', {
        code: 'MISSING_OPERATOR',
        fragment: source,
        suggestion: '请补上“等于 / 大于 / 小于等于”等比较词。'
      })]
    }
  }

  if (hasUnsupportedChineseToken(expression)) {
    const unsupportedFragments = extractUnsupportedFragments(expression)
    return {
      expression: '',
      variableRefs: replaced.variableRefs,
      feeRefs: replaced.feeRefs,
      issues: [createIssue(`条件片段存在无法识别的中文内容：${unsupportedFragments.join('、')}。`, 'error', {
        code: 'UNSUPPORTED_CONDITION_FRAGMENT',
        fragment: unsupportedFragments[0] || '',
        suggestion: '条件中请使用变量名称、费用名称、比较词和数字/字典值。'
      })]
    }
  }

  return {
    expression,
    variableRefs: replaced.variableRefs,
    feeRefs: replaced.feeRefs,
    issues: []
  }
}

function splitInlineIfElseSegments(normalized) {
  const standardMatch = normalized.match(/^如果\s+(.+?)\s*(?:,)?\s*那么\s+(.+?)\s*(?:,)?\s*否则\s+(.+)$/)
  if (standardMatch) {
    return {
      conditionText: standardMatch[1],
      trueResultText: standardMatch[2],
      falseResultText: standardMatch[3]
    }
  }
  const compactMatch = normalized.match(/^如果\s+(.+?)\s*否则\s+(.+)$/)
  if (!compactMatch) {
    return undefined
  }
  const beforeElse = String(compactMatch[1] || '').trim()
  const falseResultText = String(compactMatch[2] || '').trim()
  const inferredMatch = beforeElse.match(/^(.*?(?:!=|>=|<=|==|>|<|不等于|大于等于|小于等于|等于|大于|小于)\s*(?:'[^']*'|"[^"]*"|[^\s]+))\s+(.+)$/)
  if (!inferredMatch) {
    return undefined
  }
  return {
    conditionText: inferredMatch[1],
    trueResultText: inferredMatch[2],
    falseResultText
  }
}

function compileIfElseFormula(source, variableTokens) {
  const normalized = normalizeBusinessText(source)
  const segments = splitInlineIfElseSegments(normalized)
  if (!segments) {
    return {
      valid: false,
      expression: '',
      issues: [createIssue('条件公式请使用“如果 ... 那么 ... 否则 ...”结构。', 'error', {
        code: 'INVALID_IF_ELSE',
        fragment: source,
        suggestion: '例如：如果 女工应出勤 小于等于 0，那么 0，否则 四舍五入(金额, 2)。'
      })],
      variableRefs: [],
      feeRefs: []
    }
  }

  const conditionResult = compileConditionExpression(segments.conditionText, variableTokens)
  const trueResult = compileLiteralOrExpression(segments.trueResultText, variableTokens)
  const falseResult = compileLiteralOrExpression(segments.falseResultText, variableTokens)
  const issues = [
    ...conditionResult.issues,
    ...trueResult.issues,
    ...falseResult.issues
  ]

  return {
    valid: issues.length === 0,
    expression: issues.length ? '' : `if(${conditionResult.expression}, ${trueResult.expression}, ${falseResult.expression})`,
    issues,
    variableRefs: [...new Set([
      ...(conditionResult.variableRefs || []),
      ...(trueResult.variableRefs || []),
      ...(falseResult.variableRefs || [])
    ])],
    feeRefs: [...new Set([
      ...(conditionResult.feeRefs || []),
      ...(trueResult.feeRefs || []),
      ...(falseResult.feeRefs || [])
    ])]
  }
}

export function compileCostBusinessFormula(options = {}) {
  const businessFormula = String(options.businessFormula || '').trim()
  if (!businessFormula) {
    return {
      valid: true,
      expression: '',
      issues: [],
      variableRefs: [],
      feeRefs: []
    }
  }

  const variableTokens = buildVariableTokens(options.variableOptions, options.feeOptions)
  const normalizedFormula = normalizeBusinessText(businessFormula)
  if (/^如果/.test(normalizedFormula)) {
    return compileIfElseFormula(businessFormula, variableTokens)
  }

  const result = compileLiteralOrExpression(businessFormula, variableTokens)
  return {
    valid: result.issues.length === 0,
    expression: result.expression,
    issues: result.issues,
    variableRefs: [...new Set(result.variableRefs || [])],
    feeRefs: [...new Set(result.feeRefs || [])]
  }
}

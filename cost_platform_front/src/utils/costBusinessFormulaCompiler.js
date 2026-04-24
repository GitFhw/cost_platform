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

const CONTEXT_TOKENS = [
  {
    type: 'context',
    namespace: 'C',
    code: 'billMonth',
    name: '账期'
  }
]

const BILL_MONTH_MEMBERSHIP_PATTERN = /账期\s*属于\s*([0-9]{1,2}(?:\s*[\/、,，]\s*[0-9]{1,2})+)/g
const BILL_MONTH_EXCLUSION_PATTERN = /账期\s*不属于\s*([0-9]{1,2}(?:\s*[\/、,，]\s*[0-9]{1,2})+)/g
const GENERIC_MEMBERSHIP_PATTERN = /((?:[VCIFT]\.[A-Za-z_][A-Za-z0-9_]*)(?:\.[A-Za-z_][A-Za-z0-9_]*)?)\s*(不属于|属于)\s*((?:'[^']*'|"[^"]*"|[^\s,，)）]+)(?:\s*[\/、,，]\s*(?:'[^']*'|"[^"]*"|[^\s,，)）]+))*)/g

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
    .replace(/否则\s+如果/g, '否则如果 ')
    .replace(/否则如果(?=[^\s(（])/g, '否则如果 ')
    .replace(/并且/g, ' 且 ')
    .replace(/或者/g, ' 或 ')
    .replace(/若账期不属于/g, '如果 账期不属于 ')
    .replace(/若账期属于/g, '如果 账期属于 ')
    .replace(/如果(?=[^\s(（])/g, '如果 ')
    .replace(/(^|[\s,，(（])若(?=[^\s])/g, '$1如果 ')
    .replace(/(^|[\s,，(（])若(?=\s)/g, '$1如果')
    .replace(/否则(?!如果)(?=[^\s)）])/g, '否则 ')
    .replace(/否则\s+如果/g, '否则如果 ')
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
  const contextTokens = CONTEXT_TOKENS
    .map(item => ({
      ...item,
      name: String(item.name || '').trim()
    }))
    .filter(item => item.name)
  return [...variableTokens, ...feeTokens, ...contextTokens]
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
  return String(source || '')
    .replace(BILL_MONTH_EXCLUSION_PATTERN, (_, rawMonths) => {
      const monthTokens = String(rawMonths || '')
        .split(/[\/、,，]/)
        .map(item => item.trim())
        .filter(Boolean)
        .map(item => item.padStart(2, '0'))
      if (!monthTokens.length) {
        return _
      }
      return `!(C.billMonth matches '.*-(${[...new Set(monthTokens)].join('|')})$')`
    })
    .replace(BILL_MONTH_MEMBERSHIP_PATTERN, (_, rawMonths) => {
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

function splitMembershipValues(rawValueList) {
  return (String(rawValueList || '').match(/'[^']*'|"[^"]*"|[^\/、,，]+/g) || [])
    .map(item => item.trim())
    .filter(Boolean)
}

function normalizeMembershipValue(rawValue) {
  if (/^'.*'$/.test(rawValue) || /^".*"$/.test(rawValue)) {
    return rawValue.replace(/^"|"$/g, '\'')
  }
  if (/^-?\d+(?:\.\d+)?$/.test(rawValue)) {
    return rawValue
  }
  return `'${String(rawValue).replace(/'/g, "\\'")}'`
}

function normalizeGenericMembership(source) {
  return String(source || '').replace(GENERIC_MEMBERSHIP_PATTERN, (_, left, operator, rawValueList) => {
    const valueTokens = splitMembershipValues(rawValueList)
    if (!valueTokens.length) {
      return _
    }
    const comparator = operator === '不属于' ? ' != ' : ' == '
    const connector = operator === '不属于' ? ' and ' : ' or '
    const parts = valueTokens.map(item => `${left}${comparator}${normalizeMembershipValue(item)}`)
    if (!parts.length) {
      return _
    }
    return parts.length === 1 ? parts[0] : `(${parts.join(connector)})`
  })
}

function normalizeConditionText(source) {
  let text = normalizeBusinessText(source)
  text = normalizeBillMonthMembership(text)
  CONDITION_OPERATOR_REPLACERS.forEach(([pattern, replacement]) => {
    text = text.replace(pattern, replacement)
  })
  text = normalizePercentageLiterals(text)
    .replace(/且/g, ' and ')
    .replace(/或/g, ' or ')
    .replace(/(?<![><!=])=(?!=)/g, ' == ')
    .replace(/\s+/g, ' ')
    .trim()
  return text
}

function normalizeConditionLiteralValues(source) {
  return String(source || '').replace(
    /(==|!=|>=|<=|>|<)\s*('[^']*'|"[^"]*"|-?\d+(?:\.\d+)?|(?:[VCIFT]\.[A-Za-z_][A-Za-z0-9_]*)(?:\.[A-Za-z_][A-Za-z0-9_]*)?|true|false|null|[^\s()]+)(?=\s+(?:and|or)\s+|$|\))/g,
    (_, operator, rawValue) => {
      if (/^'.*'$/.test(rawValue) || /^".*"$/.test(rawValue)) {
        return `${operator} ${rawValue.replace(/^"|"$/g, '\'')}`
      }
      if (/^-?\d+(?:\.\d+)?$/.test(rawValue)) {
        return `${operator} ${rawValue}`
      }
      if (/^(?:[VCIFT]\.[A-Za-z_][A-Za-z0-9_]*)(?:\.[A-Za-z_][A-Za-z0-9_]*)?$/.test(rawValue)) {
        return `${operator} ${rawValue}`
      }
      if (/^(?:true|false|null)$/.test(rawValue)) {
        return `${operator} ${rawValue}`
      }
      return `${operator} '${String(rawValue).replace(/'/g, "\\'")}'`
    }
  )
}

function compileConditionExpression(source, variableTokens) {
  const text = normalizeConditionText(source)
  const replaced = replaceVariables(text, variableTokens)
  const expression = normalizeConditionLiteralValues(normalizeGenericMembership(replaced.text))
    .replace(/\s+/g, ' ')
    .trim()

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

function findTopLevelKeyword(source, keyword, startIndex = 0) {
  let depth = 0
  let quote = ''
  for (let index = startIndex; index < source.length; index += 1) {
    const char = source[index]
    if (quote) {
      if (char === quote && source[index - 1] !== '\\') {
        quote = ''
      }
      continue
    }
    if (char === '\'' || char === '"') {
      quote = char
      continue
    }
    if (char === '(' || char === '（') {
      depth += 1
      continue
    }
    if (char === ')' || char === '）') {
      depth = Math.max(0, depth - 1)
      continue
    }
    if (depth === 0 && source.startsWith(keyword, index)) {
      return index
    }
  }
  return -1
}

function trimFormulaSegment(source) {
  return String(source || '')
    .trim()
    .replace(/^[,，;；]\s*/, '')
    .replace(/\s*[,，;；]$/, '')
    .trim()
}

function findNextIfBranchSeparator(source, startIndex = 0) {
  let depth = 0
  let quote = ''
  for (let index = startIndex; index < source.length; index += 1) {
    const char = source[index]
    if (quote) {
      if (char === quote && source[index - 1] !== '\\') {
        quote = ''
      }
      continue
    }
    if (char === '\'' || char === '"') {
      quote = char
      continue
    }
    if (char === '(' || char === '（') {
      depth += 1
      continue
    }
    if (char === ')' || char === '）') {
      depth = Math.max(0, depth - 1)
      continue
    }
    if (depth !== 0) {
      continue
    }
    if (source.startsWith('否则如果', index)) {
      return {
        type: 'ELSE_IF',
        index,
        nextIndex: index + '否则如果'.length
      }
    }
    if (source.startsWith('否则', index)) {
      return {
        type: 'ELSE',
        index,
        nextIndex: index + '否则'.length
      }
    }
    if (char === ',' || char === '，' || char === ';' || char === '；') {
      const remainder = source.slice(index + 1)
      const leadingWhitespaceLength = remainder.match(/^\s*/)?.[0]?.length || 0
      const nextIndex = index + 1 + leadingWhitespaceLength
      if (source.startsWith('如果', nextIndex)) {
        return {
          type: 'NEXT_IF',
          index,
          nextIndex: nextIndex + '如果'.length
        }
      }
    }
  }
  return undefined
}

function splitInlineIfElseSegments(normalized) {
  if (!normalized.startsWith('如果')) {
    return undefined
  }
  if (findTopLevelKeyword(normalized, '那么') >= 0) {
    const branches = []
    let cursor = '如果'.length
    while (cursor <= normalized.length) {
      cursor += normalized.slice(cursor).match(/^\s*/)?.[0]?.length || 0
      const thenIndex = findTopLevelKeyword(normalized, '那么', cursor)
      if (thenIndex < 0) {
        return undefined
      }
      const conditionText = trimFormulaSegment(normalized.slice(cursor, thenIndex))
      if (!conditionText) {
        return undefined
      }
      cursor = thenIndex + '那么'.length
      cursor += normalized.slice(cursor).match(/^\s*/)?.[0]?.length || 0
      const separator = findNextIfBranchSeparator(normalized, cursor)
      if (!separator) {
        const trueResultText = trimFormulaSegment(normalized.slice(cursor))
        if (!trueResultText) {
          return undefined
        }
        branches.push({ conditionText, trueResultText })
        return {
          branches,
          falseResultText: '0',
          implicitDefault: true
        }
      }
      const trueResultText = trimFormulaSegment(normalized.slice(cursor, separator.index))
      if (!trueResultText) {
        return undefined
      }
      branches.push({ conditionText, trueResultText })
      if (separator.type === 'ELSE') {
        const falseResultText = trimFormulaSegment(normalized.slice(separator.nextIndex))
        return {
          branches,
          falseResultText: falseResultText || '0',
          implicitDefault: !falseResultText
        }
      }
      cursor = separator.nextIndex
    }
    return undefined
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
    branches: [{
      conditionText: inferredMatch[1],
      trueResultText: inferredMatch[2]
    }],
    falseResultText,
    implicitDefault: false
  }
}

function compileIfElseFormula(source, variableTokens) {
  const normalized = normalizeBusinessText(source)
  const segments = splitInlineIfElseSegments(normalized)
  if (!segments) {
    return {
      valid: false,
      expression: '',
      issues: [createIssue('条件公式请使用“如果 ... 那么 ... 否则 ...”或“如果 ... 那么 ... 否则如果 ... 那么 ...”结构。', 'error', {
        code: 'INVALID_IF_ELSE',
        fragment: source,
        suggestion: '例如：如果 女工应出勤 小于等于 0，那么 0，否则 四舍五入(金额, 2)；或 如果 班次 等于 白班，那么 1，否则如果 班次 等于 夜班，那么 2，否则 0。'
      })],
      variableRefs: [],
      feeRefs: []
    }
  }

  const compiledBranches = segments.branches.map(item => ({
    conditionResult: compileConditionExpression(item.conditionText, variableTokens),
    trueResult: compileLiteralOrExpression(item.trueResultText, variableTokens)
  }))
  const falseResult = compileLiteralOrExpression(segments.falseResultText || '0', variableTokens)
  const issues = [
    ...compiledBranches.flatMap(item => [
      ...item.conditionResult.issues,
      ...item.trueResult.issues
    ]),
    ...falseResult.issues
  ]

  let expression = ''
  if (!issues.length) {
    expression = falseResult.expression
    for (let index = compiledBranches.length - 1; index >= 0; index -= 1) {
      expression = `if(${compiledBranches[index].conditionResult.expression}, ${compiledBranches[index].trueResult.expression}, ${expression})`
    }
  }

  return {
    valid: issues.length === 0,
    expression,
    issues,
    variableRefs: [...new Set([
      ...compiledBranches.flatMap(item => [
        ...(item.conditionResult.variableRefs || []),
        ...(item.trueResult.variableRefs || [])
      ]),
      ...(falseResult.variableRefs || [])
    ])],
    feeRefs: [...new Set([
      ...compiledBranches.flatMap(item => [
        ...(item.conditionResult.feeRefs || []),
        ...(item.trueResult.feeRefs || [])
      ]),
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

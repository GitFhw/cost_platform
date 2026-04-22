const DEFAULT_NAMESPACE_SCOPE = 'V,C,I,F,T'
const KNOWN_NAMESPACES = new Set(['V', 'I', 'C', 'F', 'T'])
const PLACEHOLDER_TOKENS = [
  'if(, , )',
  'between(, , )',
  'round(, 2)',
  'coalesce(, )',
  'max(, )',
  'min(, )'
]

function normalizeNamespaceScope(namespaceScope) {
  const raw = String(namespaceScope || DEFAULT_NAMESPACE_SCOPE)
  const tokens = raw
    .split(/[,\s，]+/)
    .map(item => item.trim().toUpperCase())
    .filter(Boolean)
  return new Set(tokens.length ? tokens : DEFAULT_NAMESPACE_SCOPE.split(','))
}

function pushIssue(issues, issueSet, message) {
  if (!message || issueSet.has(message)) {
    return
  }
  issueSet.add(message)
  issues.push({ message })
}

export function validateCostExpression(options = {}) {
  const expression = String(options.expression || '').trim()
  if (!expression) {
    return {
      valid: true,
      issues: [],
      namespaces: [],
      variableRefs: [],
      feeRefs: []
    }
  }

  const issues = []
  const issueSet = new Set()
  const allowedNamespaces = normalizeNamespaceScope(options.namespaceScope)
  const variableCodes = new Set((options.variableCodes || []).filter(Boolean))
  const feeCodes = new Set((options.feeCodes || []).filter(Boolean))
  const validateVariableRefs = Boolean(options.validateVariableRefs)
  const validateFeeRefs = Boolean(options.validateFeeRefs)
  const namespaces = new Set()
  const variableRefs = new Set()
  const feeRefs = new Set()

  let inQuote = false
  let escaped = false
  const bracketStack = []

  for (let index = 0; index < expression.length; index += 1) {
    const char = expression[index]
    if (inQuote) {
      if (escaped) {
        escaped = false
        continue
      }
      if (char === '\\') {
        escaped = true
        continue
      }
      if (char === '\'') {
        inQuote = false
      }
      continue
    }
    if (char === '\'') {
      inQuote = true
      continue
    }
    if (char === '(') {
      bracketStack.push(index)
      continue
    }
    if (char === ')') {
      if (!bracketStack.length) {
        pushIssue(issues, issueSet, '存在未配对的右括号，请检查表达式括号闭合。')
      } else {
        bracketStack.pop()
      }
    }
  }

  if (inQuote) {
    pushIssue(issues, issueSet, '存在未闭合的单引号，请检查字符串常量。')
  }
  if (bracketStack.length) {
    pushIssue(issues, issueSet, '存在未闭合的左括号，请检查表达式括号闭合。')
  }

  PLACEHOLDER_TOKENS.forEach(token => {
    if (expression.includes(token)) {
      pushIssue(issues, issueSet, `检测到未补全的函数占位符 ${token}，请先补齐参数。`)
    }
  })

  const incompleteNamespacePattern = /\b([VICFT])\.(?![A-Za-z_])/g
  let incompleteMatch = incompleteNamespacePattern.exec(expression)
  while (incompleteMatch) {
    pushIssue(issues, issueSet, `检测到未补全的命名空间引用 ${incompleteMatch[1]}., 请补齐字段编码。`)
    incompleteMatch = incompleteNamespacePattern.exec(expression)
  }

  const refPattern = /\b([A-Za-z])\.([A-Za-z_][A-Za-z0-9_]*)/g
  let match = refPattern.exec(expression)
  while (match) {
    const namespace = match[1].toUpperCase()
    const originalNamespace = match[1]
    const identifier = match[2]

    if (KNOWN_NAMESPACES.has(namespace)) {
      namespaces.add(namespace)
      if (originalNamespace !== namespace) {
        pushIssue(issues, issueSet, `命名空间 ${originalNamespace}. 请统一使用大写 ${namespace}.`)
      }
      if (!allowedNamespaces.has(namespace)) {
        pushIssue(issues, issueSet, `表达式使用了未纳入命名空间范围的 ${namespace}. 引用。`)
      }
      if (namespace === 'V') {
        variableRefs.add(identifier)
        if (validateVariableRefs && !variableCodes.has(identifier)) {
          pushIssue(issues, issueSet, `变量引用 V.${identifier} 在当前场景下不存在，请先维护变量或切换场景。`)
        }
      }
      if (namespace === 'F') {
        feeRefs.add(identifier)
        if (validateFeeRefs && !feeCodes.has(identifier)) {
          pushIssue(issues, issueSet, `上下文费用引用 F.${identifier} 在当前场景下不存在，请先维护费用或切换场景。`)
        }
      }
    } else if (originalNamespace === originalNamespace.toUpperCase()) {
      pushIssue(issues, issueSet, `暂不支持命名空间 ${originalNamespace}.，请改用 V/I/C/F/T 之一。`)
    }

    match = refPattern.exec(expression)
  }

  return {
    valid: issues.length === 0,
    issues,
    namespaces: [...namespaces],
    variableRefs: [...variableRefs],
    feeRefs: [...feeRefs]
  }
}

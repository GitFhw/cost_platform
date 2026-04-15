export function normalizeJsonText(value, fallback = '') {
  if (value === null || value === undefined || value === '') {
    return fallback
  }
  if (typeof value === 'string') {
    return value
  }
  try {
    return JSON.stringify(value, null, 2)
  } catch {
    return String(value)
  }
}

export function parseJsonText(value, options = {}) {
  const { allowEmpty = true } = options
  const text = normalizeJsonText(value)
  if (!text.trim()) {
    if (allowEmpty) {
      return { valid: true, value: null, empty: true }
    }
    return { valid: false, message: 'JSON 不能为空' }
  }
  try {
    return { valid: true, value: JSON.parse(text), empty: false }
  } catch (error) {
    return { valid: false, ...buildJsonErrorDetail(error, text) }
  }
}

export function formatJsonText(value, options = {}) {
  const result = parseJsonText(value, options)
  if (!result.valid) {
    return result
  }
  if (result.empty) {
    return { ...result, text: '' }
  }
  return { ...result, text: JSON.stringify(result.value, null, 2) }
}

export function minifyJsonText(value, options = {}) {
  const result = parseJsonText(value, options)
  if (!result.valid) {
    return result
  }
  if (result.empty) {
    return { ...result, text: '' }
  }
  return { ...result, text: JSON.stringify(result.value) }
}

export function safeFormatJson(value, fallback = '{}') {
  if (value === null || value === undefined || value === '') {
    return fallback
  }
  const result = formatJsonText(value)
  return result.valid ? result.text : normalizeJsonText(value, fallback)
}

function buildJsonErrorDetail(error, text) {
  const rawMessage = error?.message || 'JSON 格式不合法'
  const location = resolveErrorLocation(rawMessage, text)
  const diagnostic = buildDiagnostic(rawMessage, text, location)
  const target = diagnostic.annotationLocation || location
  const hintText = diagnostic.hint || translateNativeError(rawMessage)
  const lineText = target.lineText || ''
  const caret = `${' '.repeat(Math.max(target.column - 1, 0))}^`
  const message = `JSON 格式不合法：第 ${target.line} 行第 ${target.column} 列，${hintText}`
  const detailMessage = [
    message,
    lineText ? `定位片段：${lineText}` : '定位片段：当前行为空或只有换行符',
    `          ${caret}`,
    `原始错误：${rawMessage}`
  ].join('\n')

  return {
    message,
    detailMessage,
    rawMessage,
    hint: hintText,
    location,
    annotationLocation: target
  }
}

function resolveErrorLocation(message, text) {
  const positionMatch = String(message || '').match(/position\s+(\d+)/i)
  if (positionMatch) {
    return positionToLocation(text, Number(positionMatch[1]))
  }

  const lineColumnMatch = String(message || '').match(/line\s+(\d+)\s+column\s+(\d+)/i)
  if (lineColumnMatch) {
    return lineColumnToLocation(text, Number(lineColumnMatch[1]), Number(lineColumnMatch[2]))
  }

  return lineColumnToLocation(text, 1, 1)
}

function positionToLocation(text, position) {
  const source = String(text || '')
  const safePosition = Number.isFinite(position) ? Math.min(Math.max(position, 0), source.length) : 0
  const before = source.slice(0, safePosition)
  const linesBefore = before.split(/\r\n|\r|\n/)
  const line = Math.max(linesBefore.length, 1)
  const column = Math.max((linesBefore[linesBefore.length - 1] || '').length + 1, 1)
  return lineColumnToLocation(source, line, column, safePosition)
}

function lineColumnToLocation(text, line, column, position) {
  const lines = String(text || '').split(/\r\n|\r|\n/)
  const safeLine = clamp(Number(line) || 1, 1, Math.max(lines.length, 1))
  const lineText = lines[safeLine - 1] || ''
  const safeColumn = clamp(Number(column) || 1, 1, Math.max(lineText.length + 1, 1))
  return {
    position: Number.isFinite(position) ? position : lineColumnToPosition(lines, safeLine, safeColumn),
    line: safeLine,
    column: safeColumn,
    row: safeLine - 1,
    columnIndex: safeColumn - 1,
    lineText
  }
}

function lineColumnToPosition(lines, line, column) {
  let position = 0
  for (let index = 0; index < line - 1; index += 1) {
    position += (lines[index] || '').length + 1
  }
  return position + column - 1
}

function buildDiagnostic(rawMessage, text, location) {
  const source = String(text || '')
  const currentChar = source[location.position] || ''
  const lowerMessage = String(rawMessage || '').toLowerCase()
  const trailingComma = resolveTrailingComma(source, location)

  if (trailingComma && /expected double-quoted property name|expected property name|unexpected token/i.test(rawMessage)) {
    const targetName = currentChar === ']' ? '数组' : '对象'
    return {
      hint: `上一项末尾多了逗号：JSON 不允许${targetName}最后一项后面再跟逗号。请删除第 ${trailingComma.line} 行末尾的逗号，或在后面补完整的下一项。`,
      annotationLocation: trailingComma
    }
  }

  if (lowerMessage.includes("expected ':'") || lowerMessage.includes('after property name')) {
    return { hint: '字段名后缺少英文冒号，请按 "字段名": 值 的格式填写。' }
  }

  if (lowerMessage.includes('double-quoted property name') || lowerMessage.includes('property name')) {
    return { hint: '对象字段名必须使用英文双引号，例如 "name": 1。请检查当前位置附近的字段名。' }
  }

  if (lowerMessage.includes("expected ',' or '}'") || lowerMessage.includes("expected ',' or ']'")) {
    return { hint: '当前值后面缺少英文逗号，或者多写了非法字符；请检查上一项和下一项之间的分隔。' }
  }

  if (lowerMessage.includes('unterminated string') || lowerMessage.includes('bad control character')) {
    return { hint: '字符串没有正确闭合，或字符串里包含未转义的换行/控制字符；请补齐英文双引号或使用转义字符。' }
  }

  if (lowerMessage.includes('bad escaped character') || lowerMessage.includes('bad unicode escape')) {
    return { hint: '字符串里的转义字符不合法；反斜杠后只能接合法转义，例如 \\\", \\\\, \\n 或 \\u4e2d。' }
  }

  if (lowerMessage.includes('unexpected non-whitespace character')) {
    return { hint: '一个 JSON 文档只能有一个根对象或根数组，末尾存在多余内容。' }
  }

  if (lowerMessage.includes('unexpected token')) {
    return { hint: '当前位置出现了 JSON 不允许的字符；请重点检查是否用了中文标点、单引号、缺逗号或多逗号。' }
  }

  return { hint: translateNativeError(rawMessage) }
}

function resolveTrailingComma(text, location) {
  const lines = String(text || '').split(/\r\n|\r|\n/)
  const currentLine = lines[location.row] || ''
  const currentTrimmed = currentLine.trim()

  if (!['}', ']'].includes(currentTrimmed[0])) {
    return null
  }

  for (let index = location.row - 1; index >= 0; index -= 1) {
    const lineText = lines[index] || ''
    const trimmed = lineText.trim()
    if (!trimmed) {
      continue
    }
    if (!trimmed.endsWith(',')) {
      return null
    }
    const commaColumnIndex = lineText.lastIndexOf(',')
    return {
      position: lineColumnToPosition(lines, index + 1, commaColumnIndex + 1),
      line: index + 1,
      column: commaColumnIndex + 1,
      row: index,
      columnIndex: commaColumnIndex,
      lineText
    }
  }

  return null
}

function translateNativeError(message) {
  return String(message || '请检查 JSON 语法').replace(/^JSON\.parse:\s*/i, '')
}

function clamp(value, min, max) {
  return Math.min(Math.max(value, min), max)
}

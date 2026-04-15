const CHANGE_TYPE_META = {
  ADDED: { label: '新增', type: 'success' },
  REMOVED: { label: '移除', type: 'danger' },
  DELETED: { label: '删除', type: 'danger' },
  CHANGED: { label: '变更', type: 'warning' },
  UPDATED: { label: '更新', type: 'warning' },
  MODIFIED: { label: '修改', type: 'warning' },
  UNCHANGED: { label: '未变化', type: 'info' },
  CREATE: { label: '新建', type: 'success' },
  UPDATE: { label: '更新', type: 'warning' },
  ROLLBACK: { label: '回滚', type: 'warning' }
}

const CHECK_LEVEL_META = {
  BLOCK: { label: '阻断', type: 'danger' },
  WARN: { label: '告警', type: 'warning' },
  PASS: { label: '通过', type: 'success' },
  INFO: { label: '提示', type: 'info' }
}

const TECHNICAL_CODE_LABELS = {
  REQUIRED: '必填输入',
  OPTIONAL: '可选输入',
  TIER_BASIS: '阶梯依据',
  FORMULA_INPUT: '公式输入',
  RULE_DERIVED: '规则派生',
  MANUAL_REQUIRED: '手工维护',
  INPUT_REQUIRED: '手工必填',
  REMOTE_REQUIRED: '接口必填',
  PUBLISHED: '已发布',
  ACTIVE: '生效中',
  ROLLED_BACK: '已回滚'
}

function normalizeCode(value) {
  return String(value || '').trim().toUpperCase()
}

export function resolveCostChangeTypeMeta(value) {
  const code = normalizeCode(value)
  return CHANGE_TYPE_META[code] || { label: value || '差异', type: 'info' }
}

export function resolveCostChangeTypeLabel(value) {
  return resolveCostChangeTypeMeta(value).label
}

export function resolveCheckLevelMeta(value) {
  const code = normalizeCode(value)
  return CHECK_LEVEL_META[code] || { label: value || '-', type: 'info' }
}

export function localizeCostTechnicalText(value) {
  if (value === null || value === undefined) {
    return ''
  }
  let text = String(value)
  Object.entries(TECHNICAL_CODE_LABELS).forEach(([code, label]) => {
    text = text.replace(new RegExp(`(^|[^A-Z0-9_])${code}($|[^A-Z0-9_])`, 'g'), `$1${label}$2`)
  })
  return text
}

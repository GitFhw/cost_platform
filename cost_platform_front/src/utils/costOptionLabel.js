const LEGACY_ORG_LABELS = {
  ORG_SHOUGANG_ORE: '首钢矿石业务组织',
  ORG_PORT_001: '港口装卸业务组织',
  ORG_HR_001: '协力薪资业务组织',
  ORG_STORAGE_001: '仓储保管业务组织'
}

function normalizeSegment(value) {
  if (value === null || value === undefined) {
    return ''
  }
  return String(value).trim()
}

export function formatNameCodeLabel(name, code) {
  const nameText = normalizeSegment(name)
  const codeText = normalizeSegment(code)
  if (nameText && codeText && nameText !== codeText) {
    return `${nameText} / ${codeText}`
  }
  return nameText || codeText
}

export function formatLegacyOrgLabel(orgCode) {
  const codeText = normalizeSegment(orgCode)
  if (!codeText) {
    return ''
  }
  const legacyName = LEGACY_ORG_LABELS[codeText]
  if (legacyName) {
    return `${legacyName} / ${codeText}`
  }
  return codeText
}

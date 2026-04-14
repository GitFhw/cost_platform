import cache from '@/plugins/cache'

const COST_WORK_CONTEXT_KEY = 'cost.workContext'

export function normalizeSceneId(value) {
  const numberValue = Number(value)
  return Number.isFinite(numberValue) && numberValue > 0 ? numberValue : undefined
}

export function normalizeVersionId(value) {
  const numberValue = Number(value)
  return Number.isFinite(numberValue) && numberValue > 0 ? numberValue : undefined
}

export function normalizeBillMonth(value) {
  if (typeof value !== 'string') {
    return undefined
  }
  const trimmed = value.trim()
  return /^\d{4}-\d{2}$/.test(trimmed) ? trimmed : undefined
}

export function resolveCurrentBillMonth() {
  const current = new Date()
  const month = String(current.getMonth() + 1).padStart(2, '0')
  return `${current.getFullYear()}-${month}`
}

function normalizeWorkContext(context = {}) {
  return {
    sceneId: normalizeSceneId(context.sceneId),
    versionId: normalizeVersionId(context.versionId),
    billMonth: normalizeBillMonth(context.billMonth)
  }
}

function persistWorkContext(context) {
  const normalized = normalizeWorkContext(context)
  if (!normalized.sceneId && !normalized.versionId && !normalized.billMonth) {
    cache.local.remove(COST_WORK_CONTEXT_KEY)
    return {}
  }
  cache.local.setJSON(COST_WORK_CONTEXT_KEY, normalized)
  return normalized
}

export function getCostWorkContext() {
  const saved = cache.local.getJSON(COST_WORK_CONTEXT_KEY)
  if (!saved || typeof saved !== 'object') {
    return {}
  }
  return normalizeWorkContext(saved)
}

export function patchCostWorkContext(patch = {}) {
  const current = getCostWorkContext()
  const next = {
    sceneId: Object.prototype.hasOwnProperty.call(patch, 'sceneId') ? normalizeSceneId(patch.sceneId) : current.sceneId,
    versionId: Object.prototype.hasOwnProperty.call(patch, 'versionId') ? normalizeVersionId(patch.versionId) : current.versionId,
    billMonth: Object.prototype.hasOwnProperty.call(patch, 'billMonth') ? normalizeBillMonth(patch.billMonth) : current.billMonth
  }
  return persistWorkContext(next)
}

export function syncCostWorkContext(context = {}) {
  const patch = {}
  const sceneId = normalizeSceneId(context.sceneId)
  const versionId = normalizeVersionId(context.versionId)
  const billMonth = normalizeBillMonth(context.billMonth)
  if (sceneId) {
    patch.sceneId = sceneId
  }
  if (versionId) {
    patch.versionId = versionId
  }
  if (billMonth) {
    patch.billMonth = billMonth
  }
  if (!Object.keys(patch).length) {
    return getCostWorkContext()
  }
  return patchCostWorkContext(patch)
}

export function clearCostWorkContext(fields = []) {
  if (!Array.isArray(fields) || !fields.length) {
    cache.local.remove(COST_WORK_CONTEXT_KEY)
    return {}
  }
  const current = getCostWorkContext()
  const next = { ...current }
  fields.forEach(field => {
    delete next[field]
  })
  return persistWorkContext(next)
}

export function resolveWorkingBillMonth(...candidates) {
  for (const candidate of candidates) {
    const normalized = normalizeBillMonth(candidate)
    if (normalized) {
      return normalized
    }
  }
  return getCostWorkContext().billMonth || resolveCurrentBillMonth()
}

export function resolveWorkingVersionId(...candidates) {
  for (const candidate of candidates) {
    const normalized = normalizeVersionId(candidate)
    if (normalized) {
      return normalized
    }
  }
  return getCostWorkContext().versionId
}

import cache from '@/plugins/cache'

const COST_SCENE_CONTEXT_KEY = 'cost.currentSceneId'

function toSceneId(value) {
  const numberValue = Number(value)
  return Number.isFinite(numberValue) && numberValue > 0 ? numberValue : undefined
}

export function getCostSceneContextId() {
  return toSceneId(cache.local.get(COST_SCENE_CONTEXT_KEY))
}

export function setCostSceneContextId(sceneId) {
  const normalized = toSceneId(sceneId)
  if (normalized) {
    cache.local.set(COST_SCENE_CONTEXT_KEY, String(normalized))
    return
  }
  cache.local.remove(COST_SCENE_CONTEXT_KEY)
}

export function resolvePreferredCostSceneId(sceneOptions = [], ...candidates) {
  const available = new Set(sceneOptions.map(item => toSceneId(item?.sceneId)).filter(Boolean))
  for (const candidate of candidates) {
    const normalized = toSceneId(candidate)
    if (normalized && available.has(normalized)) {
      return normalized
    }
  }
  return sceneOptions.length === 1 ? toSceneId(sceneOptions[0]?.sceneId) : undefined
}


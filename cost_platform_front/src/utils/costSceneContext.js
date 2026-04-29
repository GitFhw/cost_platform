import cache from '@/plugins/cache'
import { getCostWorkContext, normalizeSceneId, patchCostWorkContext } from '@/utils/costWorkContext'

const COST_SCENE_CONTEXT_KEY = 'cost.currentSceneId'

export function getCostSceneContextId() {
  const sceneId = normalizeSceneId(getCostWorkContext().sceneId)
  if (sceneId) {
    return sceneId
  }
  const legacySceneId = normalizeSceneId(cache.local.get(COST_SCENE_CONTEXT_KEY))
  if (legacySceneId) {
    patchCostWorkContext({ sceneId: legacySceneId })
  }
  return legacySceneId
}

export function setCostSceneContextId(sceneId) {
  const scene = typeof sceneId === 'object' && sceneId !== null ? sceneId : {}
  const normalized = normalizeSceneId(scene.sceneId || sceneId)
  patchCostWorkContext({
    sceneId: normalized,
    sceneCode: scene.sceneCode,
    sceneName: scene.sceneName,
    businessDomain: scene.businessDomain,
    businessDomainName: scene.businessDomainName || scene.businessDomainLabel
  })
  if (normalized) {
    cache.local.set(COST_SCENE_CONTEXT_KEY, String(normalized))
    return
  }
  cache.local.remove(COST_SCENE_CONTEXT_KEY)
}

export function resolvePreferredCostSceneId(sceneOptions = [], ...candidates) {
  const available = new Set(sceneOptions.map(item => normalizeSceneId(item?.sceneId)).filter(Boolean))
  for (const candidate of candidates) {
    const normalized = normalizeSceneId(candidate)
    if (normalized && available.has(normalized)) {
      return normalized
    }
  }
  return undefined
}

export function resolveWorkingCostSceneId(sceneOptions = [], ...candidates) {
  return resolvePreferredCostSceneId(sceneOptions, ...candidates, getCostSceneContextId())
}

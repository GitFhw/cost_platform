import { onActivated, onBeforeUnmount, onMounted, unref } from 'vue'
import {
  COST_WORK_CONTEXT_CHANGE_EVENT,
  getCostWorkContext,
  normalizeSceneId
} from '@/utils/costWorkContext'

function resolveValue(value) {
  return typeof value === 'function' ? value() : unref(value)
}

function resolveQueryParams(queryParams) {
  const target = resolveValue(queryParams)
  return target && typeof target === 'object' ? target : undefined
}

function resolveAvailableSceneId(sceneOptions, sceneId) {
  const normalized = normalizeSceneId(sceneId)
  if (!normalized) {
    return undefined
  }
  const options = resolveValue(sceneOptions)
  if (!Array.isArray(options) || !options.length) {
    return normalized
  }
  return options.some(item => normalizeSceneId(item?.sceneId) === normalized) ? normalized : undefined
}

/**
 * Keeps cached cost pages in step with the global work scene selected from scene center.
 */
export function useCostWorkSceneAutoRefresh(options = {}) {
  const {
    queryParams,
    sceneOptions,
    refresh,
    beforeRefresh,
    onSceneChange,
    resetPage = true
  } = options
  let currentSceneId = normalizeSceneId(getCostWorkContext().sceneId)
  let running = false

  async function applyContext(context = {}) {
    const nextSceneId = resolveAvailableSceneId(sceneOptions, context.sceneId)
    if (nextSceneId === currentSceneId) {
      return
    }
    currentSceneId = nextSceneId

    const query = resolveQueryParams(queryParams)
    if (query && Object.prototype.hasOwnProperty.call(query, 'sceneId')) {
      query.sceneId = nextSceneId
      if (resetPage && Object.prototype.hasOwnProperty.call(query, 'pageNum')) {
        query.pageNum = 1
      }
    }

    if (beforeRefresh) {
      await beforeRefresh(nextSceneId, context)
    }
    if (onSceneChange) {
      await onSceneChange(nextSceneId, context)
    }
    if (refresh) {
      await refresh(nextSceneId, context)
    }
  }

  function handleContextChange(event) {
    if (running) {
      return
    }
    running = true
    Promise.resolve(applyContext(event?.detail || getCostWorkContext()))
      .finally(() => {
        running = false
      })
  }

  function catchUpWhenActivated() {
    handleContextChange({ detail: getCostWorkContext() })
  }

  onMounted(() => {
    window.addEventListener(COST_WORK_CONTEXT_CHANGE_EVENT, handleContextChange)
    window.addEventListener('storage', handleContextChange)
  })

  onActivated(catchUpWhenActivated)

  onBeforeUnmount(() => {
    window.removeEventListener(COST_WORK_CONTEXT_CHANGE_EVENT, handleContextChange)
    window.removeEventListener('storage', handleContextChange)
  })
}

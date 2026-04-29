import { ElMessageBox } from 'element-plus'

function normalizeSceneId(value) {
  const numberValue = Number(value)
  return Number.isFinite(numberValue) && numberValue > 0 ? numberValue : undefined
}

function resolveSceneLabel(sceneOptions = [], sceneId) {
  const normalized = normalizeSceneId(sceneId)
  const scene = sceneOptions.find(item => normalizeSceneId(item?.sceneId) === normalized)
  if (!scene) {
    return normalized ? `场景 #${normalized}` : '未设置场景'
  }
  return [scene.sceneName, scene.sceneCode].filter(Boolean).join(' / ')
}

export async function confirmCostSceneSwitch({
  currentSceneId,
  nextSceneId,
  sceneOptions = [],
  title = '切换成本场景',
  scope = '当前页面'
} = {}) {
  const current = normalizeSceneId(currentSceneId)
  const next = normalizeSceneId(nextSceneId)
  if (!current || current === next) {
    return true
  }

  const currentLabel = resolveSceneLabel(sceneOptions, current)
  const nextLabel = resolveSceneLabel(sceneOptions, next)
  const message = [
    `${scope}将从「${currentLabel}」切换到「${nextLabel}」。`,
    '切换后会重新影响费用、变量、规则、发布版本和试算/正式核算选择，未提交的页面筛选和版本选择可能会被清空。'
  ].join('\n')

  try {
    await ElMessageBox.confirm(message, title, {
      type: 'warning',
      confirmButtonText: '确认切换',
      cancelButtonText: '取消',
      distinguishCancelAndClose: true
    })
    return true
  } catch (error) {
    return false
  }
}

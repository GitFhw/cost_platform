import { ElMessageBox } from 'element-plus'

export async function confirmCostNextAction({
  title = '下一步建议',
  message,
  confirmButtonText = '去处理',
  cancelButtonText = '稍后再说'
} = {}) {
  if (!message) {
    return false
  }
  try {
    await ElMessageBox.confirm(message, title, {
      type: 'success',
      confirmButtonText,
      cancelButtonText,
      distinguishCancelAndClose: true
    })
    return true
  } catch (error) {
    return false
  }
}

export async function chooseCostNextAction({
  title = '下一步建议',
  message,
  primaryButtonText = '去处理',
  secondaryButtonText = '稍后再说'
} = {}) {
  if (!message) {
    return undefined
  }
  try {
    await ElMessageBox.confirm(message, title, {
      type: 'success',
      confirmButtonText: primaryButtonText,
      cancelButtonText: secondaryButtonText,
      distinguishCancelAndClose: true
    })
    return 'primary'
  } catch (error) {
    return error === 'cancel' ? 'secondary' : undefined
  }
}

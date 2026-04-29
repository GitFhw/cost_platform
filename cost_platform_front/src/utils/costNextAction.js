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

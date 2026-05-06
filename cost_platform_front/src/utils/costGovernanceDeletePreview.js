import { ElMessageBox } from 'element-plus'
import { localizeCostTechnicalText } from '@/utils/costDisplayLabels'

const HISTORY_KEYWORDS = ['PUBLISH', 'SNAPSHOT', 'RESULT', 'TRACE', 'LEDGER']

export async function confirmCostDeleteImpact({ checks = [], targetLabel = '对象', targetNames = [] } = {}) {
  const normalizedChecks = checks.filter(Boolean)
  const blockedChecks = normalizedChecks.filter(item => item.canDelete === false)
  const blocked = blockedChecks.length > 0
  const html = buildDeletePreviewHtml({
    checks: normalizedChecks,
    targetLabel,
    targetNames,
    blocked
  })

  try {
    if (blocked) {
      await ElMessageBox.alert(html, '删除前治理检查', {
        type: 'warning',
        dangerouslyUseHTMLString: true,
        confirmButtonText: '我知道了',
        closeOnClickModal: false
      })
      return false
    }
    await ElMessageBox.confirm(html, '删除影响预览', {
      type: 'warning',
      dangerouslyUseHTMLString: true,
      confirmButtonText: '确认删除',
      cancelButtonText: '取消',
      closeOnClickModal: false
    })
    return true
  } catch {
    return false
  }
}

export function findFirstDeleteBlockedCheck(checks = []) {
  return checks.find(item => item?.canDelete === false)
}

function buildDeletePreviewHtml({ checks, targetLabel, targetNames, blocked }) {
  const names = targetNames.length ? targetNames : checks.map(resolveCheckName).filter(Boolean)
  const allImpacts = checks.flatMap(check => normalizeImpacts(check, resolveCheckName(check)))
  const blockedImpacts = allImpacts.filter(item => item.blocksDelete)
  const cleanupImpacts = allImpacts.filter(item => !item.blocksDelete)
  const historyImpacts = allImpacts.filter(isHistoryImpact)

  return [
    '<div class="cost-delete-preview">',
    `<p style="margin:0 0 10px;color:#303133;">即将删除 ${escapeHtml(targetLabel)}：<strong>${escapeHtml(names.join('、') || '-')}</strong></p>`,
    blocked
      ? '<p style="margin:0 0 10px;color:#b88230;">存在删除阻断，系统不会执行删除。请先处理下列引用。</p>'
      : '<p style="margin:0 0 10px;color:#67c23a;">治理检查通过。确认后系统将执行删除，并按下列影响范围处理关联数据。</p>',
    renderBlockingReasons(checks),
    renderImpactSection('阻断项', blockedImpacts, '当前没有阻断删除的引用。'),
    renderImpactSection('将清理的派生/从属关系', cleanupImpacts, '当前没有需要随删除清理的派生或从属关系。'),
    renderImpactSection('历史保留与追溯影响', historyImpacts, '当前没有发布快照、结果台账或追溯明细影响。'),
    '<p style="margin:12px 0 0;color:#909399;font-size:12px;">提示：只展示治理接口返回的代表性引用对象，完整范围以实际删除校验和数据库约束为准。</p>',
    '</div>'
  ].join('')
}

function renderBlockingReasons(checks) {
  const blocked = checks.filter(item => item?.canDelete === false)
  if (!blocked.length) {
    return ''
  }
  const rows = blocked.map(item => `<li><strong>${escapeHtml(resolveCheckName(item))}</strong>：${escapeHtml(item.removeBlockingReason || '存在删除阻断')}</li>`)
  return `<div style="margin:0 0 12px;"><div style="font-weight:700;color:#303133;margin-bottom:6px;">删除阻断原因</div><ul style="margin:0;padding-left:18px;color:#606266;line-height:1.7;">${rows.join('')}</ul></div>`
}

function renderImpactSection(title, impacts, emptyText) {
  const content = impacts.length
    ? impacts.map(renderImpactItem).join('')
    : `<div style="color:#909399;line-height:1.7;">${escapeHtml(emptyText)}</div>`
  return [
    '<div style="margin-top:12px;">',
    `<div style="font-weight:700;color:#303133;margin-bottom:6px;">${escapeHtml(title)}</div>`,
    content,
    '</div>'
  ].join('')
}

function renderImpactItem(item) {
  const examples = item.examples?.length
    ? `<div style="margin-top:4px;color:#909399;font-size:12px;">引用对象：${item.examples.map(example => escapeHtml(localizeCostTechnicalText(example))).join('；')}</div>`
    : ''
  const deleteImpact = item.deleteImpact
    ? `<div style="margin-top:4px;color:#606266;">${escapeHtml(item.deleteImpact)}</div>`
    : ''
  return [
    '<div style="border:1px solid #ebeef5;border-radius:6px;padding:8px 10px;margin-bottom:8px;background:#fafafa;">',
    `<div style="display:flex;justify-content:space-between;gap:12px;"><span style="font-weight:700;color:#303133;">${escapeHtml(item.title || item.moduleName || '关联影响')}</span><span style="white-space:nowrap;color:#606266;">${escapeHtml(String(item.count || 0))} 项</span></div>`,
    `<div style="margin-top:4px;color:#909399;font-size:12px;">${escapeHtml(item.ownerName ? `${item.ownerName} · ${item.moduleName || '关联功能'}` : (item.moduleName || '关联功能'))}</div>`,
    deleteImpact,
    examples,
    '</div>'
  ].join('')
}

function normalizeImpacts(check, ownerName) {
  return Array.isArray(check?.impactItems)
    ? check.impactItems.filter(item => item && Number(item.count || 0) > 0).map(item => ({ ...item, ownerName }))
    : []
}

function isHistoryImpact(item) {
  const impactType = String(item.impactType || '').toUpperCase()
  const text = `${item.title || ''}${item.deleteImpact || ''}${item.moduleName || ''}`
  return HISTORY_KEYWORDS.some(keyword => impactType.includes(keyword)) || /历史|发布|快照|结果|台账|追溯/.test(text)
}

function resolveCheckName(check = {}) {
  return check.sceneName || check.feeName || check.variableName || check.ruleName || check.formulaName || check.name || check.code || '-'
}

function escapeHtml(value) {
  return String(value ?? '')
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;')
}

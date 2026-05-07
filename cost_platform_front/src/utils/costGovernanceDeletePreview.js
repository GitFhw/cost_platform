import { ElMessageBox } from 'element-plus'
import { localizeCostTechnicalText } from '@/utils/costDisplayLabels'

const HISTORY_KEYWORDS = ['PUBLISH', 'SNAPSHOT', 'RESULT', 'TRACE', 'LEDGER']

export async function confirmCostDeleteImpact({ checks = [], targetLabel = '对象', targetNames = [] } = {}) {
  return confirmCostGovernanceImpact({ checks, targetLabel, targetNames, action: 'delete' })
}

export async function confirmCostDisableImpact({ checks = [], targetLabel = '对象', targetNames = [] } = {}) {
  return confirmCostGovernanceImpact({ checks, targetLabel, targetNames, action: 'disable' })
}

export async function confirmCostExportImpact({ checks = [], targetLabel = '对象', targetNames = [] } = {}) {
  const normalizedChecks = checks.filter(Boolean)
  const html = buildExportPreviewHtml({
    checks: normalizedChecks,
    targetLabel,
    targetNames
  })
  try {
    await ElMessageBox.confirm(html, '导出影响预览', {
      type: 'info',
      dangerouslyUseHTMLString: true,
      confirmButtonText: '确认导出',
      cancelButtonText: '取消',
      closeOnClickModal: false
    })
    return true
  } catch {
    return false
  }
}

async function confirmCostGovernanceImpact({ checks = [], targetLabel = '对象', targetNames = [], action }) {
  const meta = resolveActionMeta(action)
  const normalizedChecks = checks.filter(Boolean)
  const blockedChecks = normalizedChecks.filter(item => item[meta.allowKey] === false)
  const blocked = blockedChecks.length > 0
  const html = buildImpactPreviewHtml({
    checks: normalizedChecks,
    targetLabel,
    targetNames,
    blocked,
    meta
  })

  try {
    if (blocked) {
      await ElMessageBox.alert(html, meta.blockedTitle, {
        type: 'warning',
        dangerouslyUseHTMLString: true,
        confirmButtonText: '我知道了',
        closeOnClickModal: false
      })
      return false
    }
    await ElMessageBox.confirm(html, meta.confirmTitle, {
      type: 'warning',
      dangerouslyUseHTMLString: true,
      confirmButtonText: meta.confirmButtonText,
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

export function findFirstDisableBlockedCheck(checks = []) {
  return checks.find(item => item?.canDisable === false)
}

function buildImpactPreviewHtml({ checks, targetLabel, targetNames, blocked, meta }) {
  const names = targetNames.length ? targetNames : checks.map(resolveCheckName).filter(Boolean)
  const allImpacts = checks.flatMap(check => normalizeImpacts(check, resolveCheckName(check)))
  const blockedImpacts = allImpacts.filter(item => item[meta.blocksKey])
  const cleanupImpacts = allImpacts.filter(item => !item[meta.blocksKey] && !isHistoryImpact(item))
  const historyImpacts = allImpacts.filter(isHistoryImpact)

  return [
    '<div class="cost-delete-preview">',
    `<p style="margin:0 0 10px;color:#303133;">即将${escapeHtml(meta.actionLabel)} ${escapeHtml(targetLabel)}：<strong>${escapeHtml(names.join('、') || '-')}</strong></p>`,
    blocked
      ? `<p style="margin:0 0 10px;color:#b88230;">存在${escapeHtml(meta.actionLabel)}阻断，系统不会执行${escapeHtml(meta.actionLabel)}。请先处理下列引用。</p>`
      : `<p style="margin:0 0 10px;color:#67c23a;">治理检查通过。确认后系统将执行${escapeHtml(meta.actionLabel)}，并按下列影响范围处理后续配置和历史数据。</p>`,
    renderBlockingReasons(checks, meta),
    renderImpactSection('阻断项', blockedImpacts, `当前没有阻断${meta.actionLabel}的引用。`, meta),
    renderImpactSection(meta.secondaryTitle, cleanupImpacts, meta.secondaryEmptyText, meta),
    renderImpactSection(meta.historyTitle, historyImpacts, meta.historyEmptyText, meta),
    `<p style="margin:12px 0 0;color:#909399;font-size:12px;">提示：只展示治理接口返回的代表性引用对象，完整范围以实际${escapeHtml(meta.actionLabel)}校验和数据库约束为准。</p>`,
    '</div>'
  ].join('')
}

function buildExportPreviewHtml({ checks, targetLabel, targetNames }) {
  const names = targetNames.length ? targetNames : checks.map(resolveCheckName).filter(Boolean)
  const allImpacts = checks.flatMap(check => normalizeImpacts(check, resolveCheckName(check)))
  const runBlocks = checks.flatMap(check => normalizeReasonRows(check, check?.runBlockingReasons, '运行/发布阻断'))
  const runWarnings = checks.flatMap(check => normalizeReasonRows(check, check?.runWarningReasons, '运行/发布告警'))
  const activeRefs = allImpacts.filter(item => !isHistoryImpact(item))
  const historyRefs = allImpacts.filter(isHistoryImpact)

  return [
    '<div class="cost-delete-preview">',
    `<p style="margin:0 0 10px;color:#303133;">即将导出 ${escapeHtml(targetLabel)}：<strong>${escapeHtml(names.join('、') || '当前筛选结果')}</strong></p>`,
    '<p style="margin:0 0 10px;color:#606266;">导出不会修改数据；下列信息用于确认导出对象当前是否已进入规则、发布或运行链路。</p>',
    renderReasonSection('运行/发布阻断提示', runBlocks, '当前导出对象没有运行或发布阻断提示。'),
    renderReasonSection('运行/发布告警提示', runWarnings, '当前导出对象没有运行或发布告警提示。'),
    renderImpactSection('当前配置引用', activeRefs, '当前没有规则或配置引用。', { impactKey: 'deleteImpact' }),
    renderImpactSection('历史版本和运行引用', historyRefs, '当前没有发布快照、结果台账或追溯明细引用。', { impactKey: 'deleteImpact' }),
    `<p style="margin:12px 0 0;color:#909399;font-size:12px;">提示：勾选费用时仅导出勾选对象；未勾选时按当前检索条件导出。</p>`,
    '</div>'
  ].join('')
}

function normalizeReasonRows(check = {}, reasons = [], title = '') {
  return Array.isArray(reasons)
    ? reasons.filter(Boolean).map(reason => ({ ownerName: resolveCheckName(check), title, message: reason }))
    : []
}

function renderReasonSection(title, rows, emptyText) {
  const content = rows.length
    ? rows.map(item => `<li><strong>${escapeHtml(item.ownerName || '-')}</strong>：${escapeHtml(item.message)}</li>`).join('')
    : `<li style="color:#909399;">${escapeHtml(emptyText)}</li>`
  return [
    '<div style="margin-top:12px;">',
    `<div style="font-weight:700;color:#303133;margin-bottom:6px;">${escapeHtml(title)}</div>`,
    `<ul style="margin:0;padding-left:18px;color:#606266;line-height:1.7;">${content}</ul>`,
    '</div>'
  ].join('')
}

function renderBlockingReasons(checks, meta) {
  const blocked = checks.filter(item => item?.[meta.allowKey] === false)
  if (!blocked.length) {
    return ''
  }
  const rows = blocked.map(item => `<li><strong>${escapeHtml(resolveCheckName(item))}</strong>：${escapeHtml(item[meta.reasonKey] || `存在${meta.actionLabel}阻断`)}</li>`)
  return `<div style="margin:0 0 12px;"><div style="font-weight:700;color:#303133;margin-bottom:6px;">${escapeHtml(meta.actionLabel)}阻断原因</div><ul style="margin:0;padding-left:18px;color:#606266;line-height:1.7;">${rows.join('')}</ul></div>`
}

function renderImpactSection(title, impacts, emptyText, meta) {
  const content = impacts.length
    ? impacts.map(item => renderImpactItem(item, meta)).join('')
    : `<div style="color:#909399;line-height:1.7;">${escapeHtml(emptyText)}</div>`
  return [
    '<div style="margin-top:12px;">',
    `<div style="font-weight:700;color:#303133;margin-bottom:6px;">${escapeHtml(title)}</div>`,
    content,
    '</div>'
  ].join('')
}

function renderImpactItem(item, meta) {
  const examples = item.examples?.length
    ? `<div style="margin-top:4px;color:#909399;font-size:12px;">引用对象：${item.examples.map(example => escapeHtml(localizeCostTechnicalText(example))).join('；')}</div>`
    : ''
  const impactText = item[meta.impactKey]
    ? `<div style="margin-top:4px;color:#606266;">${escapeHtml(item[meta.impactKey])}</div>`
    : ''
  return [
    '<div style="border:1px solid #ebeef5;border-radius:6px;padding:8px 10px;margin-bottom:8px;background:#fafafa;">',
    `<div style="display:flex;justify-content:space-between;gap:12px;"><span style="font-weight:700;color:#303133;">${escapeHtml(item.title || item.moduleName || '关联影响')}</span><span style="white-space:nowrap;color:#606266;">${escapeHtml(String(item.count || 0))} 项</span></div>`,
    `<div style="margin-top:4px;color:#909399;font-size:12px;">${escapeHtml(item.ownerName ? `${item.ownerName} · ${item.moduleName || '关联功能'}` : (item.moduleName || '关联功能'))}</div>`,
    impactText,
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

function resolveActionMeta(action) {
  if (action === 'disable') {
    return {
      actionLabel: '停用',
      allowKey: 'canDisable',
      blocksKey: 'blocksDisable',
      reasonKey: 'disableBlockingReason',
      impactKey: 'disableImpact',
      blockedTitle: '停用前治理检查',
      confirmTitle: '停用影响预览',
      confirmButtonText: '确认停用',
      secondaryTitle: '新增配置影响',
      secondaryEmptyText: '当前没有需要提示的新增配置影响。',
      historyTitle: '历史版本和运行影响',
      historyEmptyText: '当前没有发布快照、结果台账或追溯明细影响。'
    }
  }
  return {
    actionLabel: '删除',
    allowKey: 'canDelete',
    blocksKey: 'blocksDelete',
    reasonKey: 'removeBlockingReason',
    impactKey: 'deleteImpact',
    blockedTitle: '删除前治理检查',
    confirmTitle: '删除影响预览',
    confirmButtonText: '确认删除',
    secondaryTitle: '将清理的派生/从属关系',
    secondaryEmptyText: '当前没有需要随删除清理的派生或从属关系。',
    historyTitle: '历史保留与追溯影响',
    historyEmptyText: '当前没有发布快照、结果台账或追溯明细影响。'
  }
}

function escapeHtml(value) {
  return String(value ?? '')
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;')
}

import request from '@/utils/request'

// 查询规则列表
export function listRule(query) {
  return request({
    url: '/cost/rule/list',
    method: 'get',
    params: query
  })
}

// 查询规则统计
export function getRuleStats(query) {
  return request({
    url: '/cost/rule/stats',
    method: 'get',
    params: query
  })
}

// 查询规则治理预检查
export function getRuleGovernance(ruleId) {
  return request({
    url: '/cost/rule/governance/' + ruleId,
    method: 'get'
  })
}

// 查询规则详情
export function getRule(ruleId) {
  return request({
    url: '/cost/rule/' + ruleId,
    method: 'get'
  })
}

// 新增规则
export function addRule(data) {
  return request({
    url: '/cost/rule',
    method: 'post',
    data
  })
}

// 修改规则
export function updateRule(data) {
  return request({
    url: '/cost/rule',
    method: 'put',
    data
  })
}

// 复制规则并改条件值
export function copyRule(data) {
  return request({
    url: '/cost/rule/copy',
    method: 'post',
    data
  })
}

// 阶梯命中预演
export function previewRuleTier(data) {
  return request({
    url: '/cost/rule/tierPreview',
    method: 'post',
    data
  })
}

// 规则冲突预览
export function previewRuleConflict(data) {
  return request({
    url: '/cost/rule/conflictPreview',
    method: 'post',
    data
  })
}

// 删除规则
export function delRule(ruleId) {
  return request({
    url: '/cost/rule/' + ruleId,
    method: 'delete'
  })
}

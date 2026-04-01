import request from '@/utils/request'

export function getPeriodStats(sceneId) {
  return request({
    url: '/cost/governance/period/stats',
    method: 'get',
    params: { sceneId }
  })
}

export function listPeriod(query) {
  return request({
    url: '/cost/governance/period/list',
    method: 'get',
    params: query
  })
}

export function getPeriodDetail(periodId) {
  return request({
    url: '/cost/governance/period/' + periodId,
    method: 'get'
  })
}

export function createPeriod(data) {
  return request({
    url: '/cost/governance/period',
    method: 'post',
    data
  })
}

export function sealPeriod(periodId) {
  return request({
    url: '/cost/governance/period/seal/' + periodId,
    method: 'put'
  })
}

export function listRecalc(query) {
  return request({
    url: '/cost/governance/recalc/list',
    method: 'get',
    params: query
  })
}

export function getRecalcDetail(recalcId) {
  return request({
    url: '/cost/governance/recalc/' + recalcId,
    method: 'get'
  })
}

export function applyRecalc(data) {
  return request({
    url: '/cost/governance/recalc/apply',
    method: 'post',
    data
  })
}

export function approveRecalc(recalcId, data) {
  return request({
    url: '/cost/governance/recalc/approve/' + recalcId,
    method: 'put',
    data
  })
}

export function executeRecalc(recalcId) {
  return request({
    url: '/cost/governance/recalc/execute/' + recalcId,
    method: 'put'
  })
}

export function getAuditStats(query) {
  return request({
    url: '/cost/governance/audit/stats',
    method: 'get',
    params: query
  })
}

export function listAudit(query) {
  return request({
    url: '/cost/governance/audit/list',
    method: 'get',
    params: query
  })
}

export function getAlarmStats(query) {
  return request({
    url: '/cost/governance/alarm/stats',
    method: 'get',
    params: query
  })
}

export function listAlarm(query) {
  return request({
    url: '/cost/governance/alarm/list',
    method: 'get',
    params: query
  })
}

export function ackAlarm(alarmId) {
  return request({
    url: '/cost/governance/alarm/ack/' + alarmId,
    method: 'put'
  })
}

export function resolveAlarm(alarmId) {
  return request({
    url: '/cost/governance/alarm/resolve/' + alarmId,
    method: 'put'
  })
}

export function getRuntimeCacheStats(params) {
  return request({
    url: '/cost/governance/cache/stats',
    method: 'get',
    params
  })
}

export function refreshRuntimeCache(params) {
  return request({
    url: '/cost/governance/cache/refresh',
    method: 'put',
    params
  })
}

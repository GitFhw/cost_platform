import request from '@/utils/request'

export function getSimulationStats(sceneId) {
  return request({
    url: '/cost/run/simulation/stats',
    method: 'get',
    params: { sceneId }
  })
}

export function listSimulation(query) {
  return request({
    url: '/cost/run/simulation/list',
    method: 'get',
    params: query
  })
}

export function executeSimulation(data) {
  return request({
    url: '/cost/run/simulation/execute',
    method: 'post',
    data
  })
}

export function getSimulationDetail(simulationId) {
  return request({
    url: '/cost/run/simulation/' + simulationId,
    method: 'get'
  })
}

export function getTaskStats(query) {
  return request({
    url: '/cost/run/task/stats',
    method: 'get',
    params: query
  })
}

export function listTask(query) {
  return request({
    url: '/cost/run/task/list',
    method: 'get',
    params: query
  })
}

export function submitTask(data) {
  return request({
    url: '/cost/run/task/submit',
    method: 'post',
    data
  })
}

export function getTaskDetail(taskId) {
  return request({
    url: '/cost/run/task/' + taskId,
    method: 'get'
  })
}

export function retryTaskDetail(detailId) {
  return request({
    url: '/cost/run/task/retry/' + detailId,
    method: 'put'
  })
}

export function cancelTask(taskId) {
  return request({
    url: '/cost/run/task/cancel/' + taskId,
    method: 'put'
  })
}

export function getResultStats(query) {
  return request({
    url: '/cost/run/result/stats',
    method: 'get',
    params: query
  })
}

export function listResult(query) {
  return request({
    url: '/cost/run/result/list',
    method: 'get',
    params: query
  })
}

export function getResultDetail(resultId) {
  return request({
    url: '/cost/run/result/' + resultId,
    method: 'get'
  })
}

export function getTraceDetail(traceId) {
  return request({
    url: '/cost/run/trace/' + traceId,
    method: 'get'
  })
}

export function listVersionOptions(sceneId) {
  return request({
    url: '/cost/run/version-options/' + sceneId,
    method: 'get'
  })
}

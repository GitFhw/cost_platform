import request from '@/utils/request'

export function getSimulationStats(query) {
  return request({
    url: '/cost/run/simulation/stats',
    method: 'get',
    params: query
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

export function executeSimulationBatch(data) {
  return request({
    url: '/cost/run/simulation/batch-execute',
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

export function getTaskOverview(query) {
  return request({
    url: '/cost/run/task/overview',
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

export function createTaskInputBatch(data) {
  return request({
    url: '/cost/run/task/input-batch',
    method: 'post',
    data
  })
}

export function listTaskInputBatch(query) {
  return request({
    url: '/cost/run/task/input-batch/list',
    method: 'get',
    params: query
  })
}

export function getTaskInputBatchDetail(batchId, query) {
  return request({
    url: '/cost/run/task/input-batch/' + batchId,
    method: 'get',
    params: query
  })
}

export function getTaskDetail(taskId, query) {
  return request({
    url: '/cost/run/task/' + taskId,
    method: 'get',
    params: query
  })
}

export function retryTaskDetail(detailId) {
  return request({
    url: '/cost/run/task/retry/' + detailId,
    method: 'put'
  })
}

export function retryTaskPartition(partitionId) {
  return request({
    url: '/cost/run/task/partition/retry/' + partitionId,
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

export function getRunInputTemplate(query) {
  return request({
    url: '/cost/run/input-template',
    method: 'get',
    params: query
  })
}

export function getFeeRunInputTemplate(query) {
  return request({
    url: '/cost/run/input-template/fee',
    method: 'get',
    params: query
  })
}

export function previewBuiltInput(data) {
  return request({
    url: '/cost/run/input-build/preview',
    method: 'post',
    data
  })
}

export function calculateFee(data) {
  return request({
    url: '/cost/run/fee/calculate',
    method: 'post',
    data
  })
}

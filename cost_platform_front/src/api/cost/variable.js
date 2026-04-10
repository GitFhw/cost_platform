import request from '@/utils/request'

// 查询变量列表
export function listVariable(query) {
  return request({
    url: '/cost/variable/list',
    method: 'get',
    params: query
  })
}

// 查询变量统计
export function getVariableStats(query) {
  return request({
    url: '/cost/variable/stats',
    method: 'get',
    params: query
  })
}

// 查询变量治理预检查
export function getVariableGovernance(variableId) {
  return request({
    url: '/cost/variable/governance/' + variableId,
    method: 'get'
  })
}

// 查询变量详情
export function getVariable(variableId) {
  return request({
    url: '/cost/variable/' + variableId,
    method: 'get'
  })
}

// 查询变量选择框
export function optionselectVariable(query) {
  return request({
    url: '/cost/variable/optionselect',
    method: 'get',
    params: query
  })
}

// 新增变量
export function addVariable(data) {
  return request({
    url: '/cost/variable',
    method: 'post',
    data
  })
}

// 修改变量
export function updateVariable(data) {
  return request({
    url: '/cost/variable',
    method: 'put',
    data
  })
}

// 复制变量
export function copyVariable(data) {
  return request({
    url: '/cost/variable/copy',
    method: 'post',
    data
  })
}

// 删除变量
export function delVariable(variableId) {
  return request({
    url: '/cost/variable/' + variableId,
    method: 'delete'
  })
}

// 下载导入模板
export function importVariableTemplate() {
  return request({
    url: '/cost/variable/importTemplate',
    method: 'post',
    responseType: 'blob'
  })
}

// 导入预览
export function previewVariableImport(data) {
  return request({
    url: '/cost/variable/importPreview',
    method: 'post',
    headers: { 'Content-Type': 'multipart/form-data' },
    data
  })
}

// 执行导入
export function importVariableData(data) {
  return request({
    url: '/cost/variable/importData',
    method: 'post',
    headers: { 'Content-Type': 'multipart/form-data' },
    data
  })
}

// 查询共享影响因素模板
export function listVariableTemplates() {
  return request({
    url: '/cost/variable/sharedTemplates',
    method: 'get'
  })
}

// 应用共享影响因素模板
export function applyVariableTemplate(data) {
  return request({
    url: '/cost/variable/sharedTemplates/apply',
    method: 'post',
    data
  })
}

// 测试第三方接口
export function testVariableRemote(data) {
  return request({
    url: '/cost/variable/remote/test',
    method: 'post',
    timeout: 90000,
    data
  })
}

// 预览第三方数据
export function previewVariableRemote(data) {
  return request({
    url: '/cost/variable/remote/preview',
    method: 'post',
    timeout: 90000,
    data
  })
}

// 刷新第三方缓存
export function refreshVariableRemote(data) {
  return request({
    url: '/cost/variable/remote/refresh',
    method: 'post',
    data
  })
}

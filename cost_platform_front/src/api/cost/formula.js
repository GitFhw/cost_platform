import request from '@/utils/request'

// 查询公式列表
export function listFormula(query) {
  return request({
    url: '/cost/formula/list',
    method: 'get',
    params: query
  })
}

// 查询公式统计
export function getFormulaStats(query) {
  return request({
    url: '/cost/formula/stats',
    method: 'get',
    params: query
  })
}

// 查询公式治理检查
export function getFormulaGovernance(formulaId) {
  return request({
    url: '/cost/formula/governance/' + formulaId,
    method: 'get'
  })
}

// 查询公式下拉选项
export function optionselectFormula(query) {
  return request({
    url: '/cost/formula/optionselect',
    method: 'get',
    params: query
  })
}

// 查询公式详情
export function getFormula(formulaId) {
  return request({
    url: '/cost/formula/' + formulaId,
    method: 'get'
  })
}

// 新增公式
export function addFormula(data) {
  return request({
    url: '/cost/formula',
    method: 'post',
    data
  })
}

// 修改公式
export function updateFormula(data) {
  return request({
    url: '/cost/formula',
    method: 'put',
    data
  })
}

// 删除公式
export function delFormula(formulaId) {
  return request({
    url: '/cost/formula/' + formulaId,
    method: 'delete'
  })
}

// 测试公式
export function testFormula(data) {
  return request({
    url: '/cost/formula/test',
    method: 'post',
    data
  })
}

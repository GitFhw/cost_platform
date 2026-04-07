import request from '@/utils/request'

export function listFormula(query) {
  return request({
    url: '/cost/formula/list',
    method: 'get',
    params: query
  })
}

export function getFormulaStats(query) {
  return request({
    url: '/cost/formula/stats',
    method: 'get',
    params: query
  })
}

export function getFormulaGovernance(formulaId) {
  return request({
    url: '/cost/formula/governance/' + formulaId,
    method: 'get'
  })
}

export function optionselectFormula(query) {
  return request({
    url: '/cost/formula/optionselect',
    method: 'get',
    params: query
  })
}

export function listFormulaTemplates(query) {
  return request({
    url: '/cost/formula/templateOptions',
    method: 'get',
    params: query
  })
}

export function getFormula(formulaId) {
  return request({
    url: '/cost/formula/' + formulaId,
    method: 'get'
  })
}

export function listFormulaVersions(formulaId) {
  return request({
    url: '/cost/formula/versions/' + formulaId,
    method: 'get'
  })
}

export function getFormulaVersion(versionId) {
  return request({
    url: '/cost/formula/version/' + versionId,
    method: 'get'
  })
}

export function rollbackFormulaVersion(versionId) {
  return request({
    url: '/cost/formula/version/rollback/' + versionId,
    method: 'put'
  })
}

export function addFormula(data) {
  return request({
    url: '/cost/formula',
    method: 'post',
    data
  })
}

export function updateFormula(data) {
  return request({
    url: '/cost/formula',
    method: 'put',
    data
  })
}

export function delFormula(formulaId) {
  return request({
    url: '/cost/formula/' + formulaId,
    method: 'delete'
  })
}

export function testFormula(data) {
  return request({
    url: '/cost/formula/test',
    method: 'post',
    data
  })
}

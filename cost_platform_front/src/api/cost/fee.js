import request from '@/utils/request'

// 查询费用列表
export function listFee(query) {
  return request({
    url: '/cost/fee/list',
    method: 'get',
    params: query
  })
}

// 查询费用统计
export function getFeeStats(query) {
  return request({
    url: '/cost/fee/stats',
    method: 'get',
    params: query
  })
}

// 查询费用治理预检查
export function getFeeGovernance(feeId) {
  return request({
    url: '/cost/fee/governance/' + feeId,
    method: 'get'
  })
}

// 查询费用详情
export function getFee(feeId) {
  return request({
    url: '/cost/fee/' + feeId,
    method: 'get'
  })
}

// 查询费用选择框
export function optionselectFee(query) {
  return request({
    url: '/cost/fee/optionselect',
    method: 'get',
    params: query
  })
}

// 新增费用
export function addFee(data) {
  return request({
    url: '/cost/fee',
    method: 'post',
    data
  })
}

// 修改费用
export function updateFee(data) {
  return request({
    url: '/cost/fee',
    method: 'put',
    data
  })
}

// 批量停用费用
export function disableFee(feeIds) {
  return request({
    url: '/cost/fee/disable/' + feeIds,
    method: 'put'
  })
}

// 删除费用
export function delFee(feeId) {
  return request({
    url: '/cost/fee/' + feeId,
    method: 'delete'
  })
}

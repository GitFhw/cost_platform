import request from '@/utils/request'

// 查询变量分组列表
export function listVariableGroup(query) {
  return request({
    url: '/cost/variable/group/list',
    method: 'get',
    params: query
  })
}

// 查询变量分组详情
export function getVariableGroup(groupId) {
  return request({
    url: '/cost/variable/group/' + groupId,
    method: 'get'
  })
}

// 查询变量分组选择框
export function optionselectVariableGroup(query) {
  return request({
    url: '/cost/variable/group/optionselect',
    method: 'get',
    params: query
  })
}

// 新增变量分组
export function addVariableGroup(data) {
  return request({
    url: '/cost/variable/group',
    method: 'post',
    data
  })
}

// 修改变量分组
export function updateVariableGroup(data) {
  return request({
    url: '/cost/variable/group',
    method: 'put',
    data
  })
}

// 删除变量分组
export function delVariableGroup(groupId) {
  return request({
    url: '/cost/variable/group/' + groupId,
    method: 'delete'
  })
}

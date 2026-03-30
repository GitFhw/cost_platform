import request from '@/utils/request'

// 查询场景列表
export function listScene(query) {
  return request({
    url: '/cost/scene/list',
    method: 'get',
    params: query
  })
}

// 查询场景统计
export function getSceneStats(query) {
  return request({
    url: '/cost/scene/stats',
    method: 'get',
    params: query
  })
}

// 查询场景治理预检查
export function getSceneGovernance(sceneId) {
  return request({
    url: '/cost/scene/governance/' + sceneId,
    method: 'get'
  })
}

// 查询场景详情
export function getScene(sceneId) {
  return request({
    url: '/cost/scene/' + sceneId,
    method: 'get'
  })
}

// 查询场景选择框
export function optionselectScene(query) {
  return request({
    url: '/cost/scene/optionselect',
    method: 'get',
    params: query
  })
}

// 新增场景
export function addScene(data) {
  return request({
    url: '/cost/scene',
    method: 'post',
    data: data
  })
}

// 修改场景
export function updateScene(data) {
  return request({
    url: '/cost/scene',
    method: 'put',
    data: data
  })
}

// 删除场景
export function delScene(sceneId) {
  return request({
    url: '/cost/scene/' + sceneId,
    method: 'delete'
  })
}

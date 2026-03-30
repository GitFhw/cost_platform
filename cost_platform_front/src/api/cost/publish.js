import request from '@/utils/request'

// 查询发布统计
export function getPublishStats(query) {
  return request({
    url: '/cost/publish/stats',
    method: 'get',
    params: query
  })
}

// 查询版本台账
export function listPublish(query) {
  return request({
    url: '/cost/publish/list',
    method: 'get',
    params: query
  })
}

// 查询发布前检查
export function getPublishPrecheck(sceneId) {
  return request({
    url: '/cost/publish/precheck/' + sceneId,
    method: 'get'
  })
}

// 查询版本详情
export function getPublishVersion(versionId, params) {
  return request({
    url: '/cost/publish/' + versionId,
    method: 'get',
    params
  })
}

// 查询版本差异
export function getPublishDiff(params) {
  return request({
    url: '/cost/publish/diff',
    method: 'get',
    params
  })
}

// 生成发布版本
export function addPublishVersion(data) {
  return request({
    url: '/cost/publish',
    method: 'post',
    data
  })
}

// 设为生效
export function activatePublishVersion(versionId) {
  return request({
    url: '/cost/publish/activate/' + versionId,
    method: 'put'
  })
}

// 回滚到历史版本
export function rollbackPublishVersion(versionId) {
  return request({
    url: '/cost/publish/rollback/' + versionId,
    method: 'put'
  })
}

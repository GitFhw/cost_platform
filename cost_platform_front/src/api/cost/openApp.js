import request from '@/utils/request'

export function listOpenApp(query) {
  return request({
    url: '/cost/openApp/list',
    method: 'get',
    params: query
  })
}

export function getOpenApp(appId) {
  return request({
    url: '/cost/openApp/' + appId,
    method: 'get'
  })
}

export function listOpenAppSceneOptions() {
  return request({
    url: '/cost/openApp/sceneOptions',
    method: 'get'
  })
}

export function addOpenApp(data) {
  return request({
    url: '/cost/openApp',
    method: 'post',
    data
  })
}

export function updateOpenApp(data) {
  return request({
    url: '/cost/openApp',
    method: 'put',
    data
  })
}

export function resetOpenAppSecret(appId) {
  return request({
    url: '/cost/openApp/resetSecret/' + appId,
    method: 'put'
  })
}

export function delOpenApp(appId) {
  return request({
    url: '/cost/openApp/' + appId,
    method: 'delete'
  })
}

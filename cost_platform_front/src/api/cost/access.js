import request from '@/utils/request'

export function listAccessProfile(query) {
  return request({
    url: '/cost/access/profile/list',
    method: 'get',
    params: query
  })
}

export function optionselectAccessProfile(query) {
  return request({
    url: '/cost/access/profile/options',
    method: 'get',
    params: query
  })
}

export function getAccessProfile(profileId) {
  return request({
    url: '/cost/access/profile/' + profileId,
    method: 'get'
  })
}

export function addAccessProfile(data) {
  return request({
    url: '/cost/access/profile',
    method: 'post',
    data
  })
}

export function updateAccessProfile(data) {
  return request({
    url: '/cost/access/profile',
    method: 'put',
    data
  })
}

export function removeAccessProfile(profileId) {
  return request({
    url: '/cost/access/profile/' + profileId,
    method: 'delete'
  })
}

export function previewAccessProfileFetch(profileId, data) {
  return request({
    url: '/cost/access/profile/' + profileId + '/preview-fetch',
    method: 'post',
    data
  })
}

export function createInputBatchByAccessProfile(profileId, data) {
  return request({
    url: '/cost/access/profile/' + profileId + '/input-batch',
    method: 'post',
    data
  })
}

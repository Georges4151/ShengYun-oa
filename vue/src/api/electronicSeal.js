import request from './request'

export function getSealList() {
  return request({
    url: '/api/electronic-seal/seals',
    method: 'get'
  })
}

export function getSealById(id) {
  return request({
    url: `/api/electronic-seal/seals/${id}`,
    method: 'get'
  })
}

export function createSeal(data) {
  return request({
    url: '/api/electronic-seal/seals',
    method: 'post',
    data
  })
}

export function updateSeal(id, data) {
  return request({
    url: `/api/electronic-seal/seals/${id}`,
    method: 'put',
    data
  })
}

export function deleteSeal(id) {
  return request({
    url: `/api/electronic-seal/seals/${id}`,
    method: 'delete'
  })
}

export function submitSealApplication(data) {
  return request({
    url: '/api/electronic-seal/applications',
    method: 'post',
    data
  })
}

export function getMyApplications() {
  return request({
    url: '/api/electronic-seal/applications/my',
    method: 'get'
  })
}

export function getPendingApplications() {
  return request({
    url: '/api/electronic-seal/applications/pending',
    method: 'get'
  })
}

export function approveApplication(id, comment) {
  return request({
    url: `/api/electronic-seal/applications/${id}/approve`,
    method: 'put',
    params: { comment }
  })
}

export function rejectApplication(id, comment) {
  return request({
    url: `/api/electronic-seal/applications/${id}/reject`,
    method: 'put',
    params: { comment }
  })
}

export function canUseSeal(sealId) {
  return request({
    url: `/api/electronic-seal/can-use/${sealId}`,
    method: 'get'
  })
}

export function stampContract(data) {
  return request({
    url: '/api/electronic-seal/stamp',
    method: 'post',
    data
  })
}

export function recordSealUsage(data) {
  return request({
    url: '/api/electronic-seal/usage',
    method: 'post',
    data
  })
}

export function getMyUsages() {
  return request({
    url: '/api/electronic-seal/usage/my',
    method: 'get'
  })
}

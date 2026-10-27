import request from './request'

export function getMaterialList(params) {
  return request({
    url: '/api/materials',
    method: 'get',
    params
  })
}

export function getMaterialDetail(id) {
  return request({
    url: `/api/materials/${id}`,
    method: 'get'
  })
}

export function createMaterial(data) {
  return request({
    url: '/api/materials',
    method: 'post',
    data
  })
}

export function updateMaterial(id, data) {
  return request({
    url: `/api/materials/${id}`,
    method: 'put',
    data
  })
}

export function deleteMaterial(id) {
  return request({
    url: `/api/materials/${id}`,
    method: 'delete'
  })
}

export function getMaterialStatistics() {
  return request({
    url: '/api/materials/statistics',
    method: 'get'
  })
}

export function searchMaterials(params) {
  return request({
    url: '/api/materials/search',
    method: 'get',
    params
  })
}

export function getMaterialReport(params) {
  return request({
    url: '/api/reports/material',
    method: 'get',
    params
  })
}

import request from './request'

export function getSubcontractorList(params) {
  return request({
    url: '/api/subcontractors',
    method: 'get',
    params
  })
}

export function getSubcontractorDetail(id) {
  return request({
    url: `/api/subcontractors/${id}`,
    method: 'get'
  })
}

export function createSubcontractor(data) {
  return request({
    url: '/api/subcontractors',
    method: 'post',
    data
  })
}

export function updateSubcontractor(id, data) {
  return request({
    url: `/api/subcontractors/${id}`,
    method: 'put',
    data
  })
}

export function deleteSubcontractor(id) {
  return request({
    url: `/api/subcontractors/${id}`,
    method: 'delete'
  })
}

export function getContractList(params) {
  return request({
    url: '/api/subcontractors/contracts',
    method: 'get',
    params
  })
}

export function getContractDetail(id) {
  return request({
    url: `/api/subcontractors/contracts/${id}`,
    method: 'get'
  })
}

export function createContract(data) {
  return request({
    url: '/api/subcontractors/contracts',
    method: 'post',
    data
  })
}

export function updateContract(id, data) {
  return request({
    url: `/api/subcontractors/contracts/${id}`,
    method: 'put',
    data
  })
}

export function deleteContract(id) {
  return request({
    url: `/api/subcontractors/contracts/${id}`,
    method: 'delete'
  })
}

export function getContractFileUrl(contractId) {
  return `/api/files/contract/${contractId}`
}

export function getContractFileByPath(path) {
  return request({
    url: '/api/files/contract/file',
    method: 'get',
    params: { path }
  })
}

import request from './request'

export function getProjectList(params) {
  return request({
    url: '/api/projects',
    method: 'get',
    params
  })
}

export function getProjectDetail(id) {
  return request({
    url: `/api/projects/${id}`,
    method: 'get'
  })
}

export function createProject(data) {
  return request({
    url: '/api/projects',
    method: 'post',
    data
  })
}

export function updateProject(id, data) {
  return request({
    url: `/api/projects/${id}`,
    method: 'put',
    data
  })
}

export function deleteProject(id) {
  return request({
    url: `/api/projects/${id}`,
    method: 'delete'
  })
}

export function getProjectStatistics() {
  return request({
    url: '/api/projects/statistics',
    method: 'get'
  })
}

export function getProjectCostAnalysis(id) {
  return request({
    url: `/api/projects/${id}/cost-analysis`,
    method: 'get'
  })
}

export function getActiveProjects() {
  return request({
    url: '/api/projects/active',
    method: 'get'
  })
}

export function getProjectsByStatus(status) {
  return request({
    url: '/api/projects/status/' + status,
    method: 'get'
  })
}

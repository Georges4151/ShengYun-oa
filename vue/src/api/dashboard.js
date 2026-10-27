import request from './request'

export function getDashboardStatistics() {
  return request({
    url: '/api/dashboard/statistics',
    method: 'get'
  })
}

export function getRecentProjects(limit = 5) {
  return request({
    url: '/api/dashboard/recent-projects',
    method: 'get',
    params: { limit }
  })
}

export function getPendingApprovals(limit = 10) {
  return request({
    url: '/api/dashboard/pending-approvals',
    method: 'get',
    params: { limit }
  })
}

export function getMonthlyStats() {
  return request({
    url: '/api/dashboard/monthly-stats',
    method: 'get'
  })
}

import request from './request'

export function getAccountList(params) {
  return request({
    url: '/api/accounts',
    method: 'get',
    params
  })
}

export function getAccountDetail(id) {
  return request({
    url: `/api/accounts/${id}`,
    method: 'get'
  })
}

export function createAccount(data) {
  return request({
    url: '/api/accounts',
    method: 'post',
    data
  })
}

export function updateAccount(id, data) {
  return request({
    url: `/api/accounts/${id}`,
    method: 'put',
    data
  })
}

export function deleteAccount(id) {
  return request({
    url: `/api/accounts/${id}`,
    method: 'delete'
  })
}

export function getAccountSummary(params) {
  return request({
    url: '/api/accounts/summary',
    method: 'get',
    params
  })
}

export function getMonthlyStats(year) {
  return request({
    url: `/api/accounts/monthly-stats/${year}`,
    method: 'get'
  })
}

export function searchAccounts(keyword) {
  return request({
    url: '/api/accounts/search',
    method: 'get',
    params: { keyword }
  })
}

export function getAccountsByProject(projectId) {
  return request({
    url: '/api/accounts',
    method: 'get',
    params: { projectId }
  })
}

export function getAccountsByDateRange(startDate, endDate) {
  return request({
    url: '/api/accounts',
    method: 'get',
    params: { startDate, endDate }
  })
}

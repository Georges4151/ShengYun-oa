import request from './request'

export function getSystemSettings() {
  return request({
    url: '/api/system/settings',
    method: 'get'
  })
}

export function updateSystemSettings(data) {
  return request({
    url: '/api/system/settings',
    method: 'put',
    data
  })
}

export function getOperationLogs(params) {
  return request({
    url: '/api/system/logs',
    method: 'get',
    params
  })
}

export function backupData() {
  return request({
    url: '/api/system/backup',
    method: 'post'
  })
}

export function getBackupList() {
  return request({
    url: '/api/system/backups',
    method: 'get'
  })
}

export function deleteBackup(id) {
  return request({
    url: `/api/system/backup/${id}`,
    method: 'delete'
  })
}

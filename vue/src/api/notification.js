import request from './request'

export function getNotificationList(params) {
  return request({
    url: '/api/notifications/user',
    method: 'get',
    params
  })
}

export function getUnreadNotifications() {
  return request({
    url: '/api/notifications/user/unread',
    method: 'get'
  })
}

export function getUnreadCount() {
  return request({
    url: '/api/notifications/user/unread-count',
    method: 'get'
  })
}

export function markAsRead(data) {
  return request({
    url: '/api/notifications/user/read',
    method: 'post',
    data
  })
}

export function markAllAsRead() {
  return request({
    url: '/api/notifications/user/read-all',
    method: 'post'
  })
}

export function deleteNotification(data) {
  return request({
    url: '/api/notifications/user',
    method: 'delete',
    data
  })
}

export function sendNotification(data) {
  return request({
    url: '/api/notifications',
    method: 'post',
    data
  })
}

export function getNotificationSettings() {
  return request({
    url: '/api/notifications/user/preferences',
    method: 'get'
  })
}

export function updateNotificationSettings(data) {
  return request({
    url: '/api/notifications/user/preferences',
    method: 'put',
    data
  })
}

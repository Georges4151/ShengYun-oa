import request from './request'

export function getUserList(params) {
  return request({
    url: '/api/users',
    method: 'get',
    params
  })
}

export function getUserInfo() {
  return request({
    url: '/api/mobile/user/info',
    method: 'get'
  })
}

export function createUser(data) {
  return request({
    url: '/api/users',
    method: 'post',
    data
  })
}

export function updateUser(id, data) {
  return request({
    url: `/api/users/${id}`,
    method: 'put',
    data
  })
}

export function deleteUser(id) {
  return request({
    url: `/api/users/${id}`,
    method: 'delete'
  })
}

export function resetPassword(id) {
  return request({
    url: `/api/users/${id}/reset-password`,
    method: 'put'
  })
}

export function updateUserInfo(data) {
  return request({
    url: '/api/mobile/user/info',
    method: 'put',
    data
  })
}

export function changePassword(data) {
  return request({
    url: '/api/auth/change-password',
    method: 'put',
    data
  })
}

export function getRoleList() {
  return request({
    url: '/api/roles',
    method: 'get'
  })
}

export function createRole(data) {
  return request({
    url: '/api/roles',
    method: 'post',
    data
  })
}

export function updateRole(id, data) {
  return request({
    url: `/api/roles/${id}`,
    method: 'put',
    data
  })
}

export function deleteRole(id) {
  return request({
    url: `/api/roles/${id}`,
    method: 'delete'
  })
}

export function getRolePermissions(roleId) {
  return request({
    url: `/api/roles/${roleId}/permissions`,
    method: 'get'
  })
}

export function updateRolePermissions(roleId, permissions) {
  return request({
    url: `/api/roles/${roleId}/permissions`,
    method: 'put',
    data: { permissions }
  })
}

import request from './request'

export function getReceiptList(params) {
  return request({
    url: '/api/receipts',
    method: 'get',
    params
  })
}

export function getReceiptDetail(id) {
  return request({
    url: `/api/receipts/${id}`,
    method: 'get'
  })
}

export function uploadReceipt(formData) {
  return request({
    url: '/api/receipts/upload',
    method: 'post',
    data: formData,
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  })
}

export function deleteReceipt(id) {
  return request({
    url: `/api/receipts/${id}`,
    method: 'delete'
  })
}

export function getPendingReceipts(params) {
  return request({
    url: '/api/receipts/pending',
    method: 'get',
    params
  })
}

export function getAllPendingReceipts() {
  return request({
    url: '/api/receipts/pending',
    method: 'get'
  })
}

export function getApprovalHistory(receiptId) {
  return request({
    url: `/api/approval/history/${receiptId}`,
    method: 'get'
  })
}

export function processApproval(data) {
  return request({
    url: '/api/approval/process',
    method: 'post',
    data
  })
}

export function verifyReceipt(id, data) {
  return request({
    url: `/api/receipts/${id}/verify`,
    method: 'put',
    data
  })
}

export function getReceiptsByStatus(status) {
  return request({
    url: '/api/receipts',
    method: 'get',
    params: { verificationStatus: status }
  })
}

export function getReceiptsByType(type) {
  return request({
    url: '/api/receipts',
    method: 'get',
    params: { type }
  })
}

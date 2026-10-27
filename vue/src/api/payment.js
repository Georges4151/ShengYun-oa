import request from './request'

export function getPaymentAccountList(params) {
  return request({
    url: '/api/payment/accounts',
    method: 'get',
    params
  })
}

export function getPaymentAccountDetail(id) {
  return request({
    url: `/api/payment/accounts/${id}`,
    method: 'get'
  })
}

export function createPaymentAccount(data) {
  return request({
    url: '/api/payment/accounts',
    method: 'post',
    data
  })
}

export function updatePaymentAccount(id, data) {
  return request({
    url: `/api/payment/accounts/${id}`,
    method: 'put',
    data
  })
}

export function deletePaymentAccount(id) {
  return request({
    url: `/api/payment/accounts/${id}`,
    method: 'delete'
  })
}

export function getTransactionList(params) {
  return request({
    url: '/api/payment/transactions',
    method: 'get',
    params
  })
}

export function getTransactionDetail(id) {
  return request({
    url: `/api/payment/transactions/${id}`,
    method: 'get'
  })
}

export function createTransaction(data) {
  return request({
    url: '/api/payment/transactions',
    method: 'post',
    data
  })
}

export function deleteTransaction(id) {
  return request({
    url: `/api/payment/transactions/${id}`,
    method: 'delete'
  })
}

export function getBankReconciliation(params) {
  return request({
    url: '/api/payment/reconciliation',
    method: 'get',
    params
  })
}

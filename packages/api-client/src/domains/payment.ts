/**
 * 资金域（payment）类型化 API 工厂。
 * 所有路径前缀统一 `/api/v1`，与 docs/openapi/payment.openapi.yaml paths 一致。
 * payments/execute 为高危接口，自动附加 HMAC 签名头。
 */

import type { HttpClient } from '../http-client'
import type {
  Id,
  PageResult,
  PaymentAccountVO,
  PaymentAccountCreateRequest,
  PaymentAccountUpdateRequest,
  PaymentAccountPageQuery,
  AccountVO,
  AccountCreateRequest,
  AccountUpdateRequest,
  AccountPageQuery,
  PaymentTransactionVO,
  PaymentTransactionCreateRequest,
  PaymentTransactionUpdateRequest,
  PaymentTransactionPageQuery,
  PaymentExecuteRequest,
  CostAllocationVO,
  CostAllocationCreateRequest,
  CostAllocationUpdateRequest,
  CostAllocationPageQuery
} from '@ceams/types'

export interface PaymentApi {
  paymentAccountPage(query?: PaymentAccountPageQuery): Promise<PageResult<PaymentAccountVO>>
  paymentAccountDetail(id: Id): Promise<PaymentAccountVO>
  paymentAccountCreate(body: PaymentAccountCreateRequest): Promise<Id>
  paymentAccountUpdate(id: Id, body: PaymentAccountUpdateRequest): Promise<void>
  paymentAccountRemove(id: Id): Promise<void>

  accountPage(query?: AccountPageQuery): Promise<PageResult<AccountVO>>
  accountDetail(id: Id): Promise<AccountVO>
  accountCreate(body: AccountCreateRequest): Promise<Id>
  accountUpdate(id: Id, body: AccountUpdateRequest): Promise<void>
  accountRemove(id: Id): Promise<void>
  accountConfirm(id: Id): Promise<void>
  accountCancel(id: Id): Promise<void>

  transactionPage(query?: PaymentTransactionPageQuery): Promise<PageResult<PaymentTransactionVO>>
  transactionDetail(id: Id): Promise<PaymentTransactionVO>
  transactionCreate(body: PaymentTransactionCreateRequest): Promise<Id>
  transactionUpdate(id: Id, body: PaymentTransactionUpdateRequest): Promise<void>
  transactionRemove(id: Id): Promise<void>

  paymentExecute(body: PaymentExecuteRequest): Promise<Id>

  costAllocationPage(query?: CostAllocationPageQuery): Promise<PageResult<CostAllocationVO>>
  costAllocationDetail(id: Id): Promise<CostAllocationVO>
  costAllocationCreate(body: CostAllocationCreateRequest): Promise<Id>
  costAllocationUpdate(id: Id, body: CostAllocationUpdateRequest): Promise<void>
  costAllocationRemove(id: Id): Promise<void>
}

export function createPaymentApi(client: HttpClient): PaymentApi {
  return {
    async paymentAccountPage(query = {}) {
      return client.get<PageResult<PaymentAccountVO>>('/api/v1/payment-accounts', {
        query: {
          page: query.page ?? 1,
          size: query.size ?? 20,
          name: query.name,
          type: query.type
        }
      })
    },
    async paymentAccountDetail(id) {
      return client.get<PaymentAccountVO>(`/api/v1/payment-accounts/${id}`)
    },
    async paymentAccountCreate(body) {
      return client.post<Id>('/api/v1/payment-accounts', body)
    },
    async paymentAccountUpdate(id, body) {
      return client.put<void>(`/api/v1/payment-accounts/${id}`, body)
    },
    async paymentAccountRemove(id) {
      return client.delete<void>(`/api/v1/payment-accounts/${id}`)
    },

    async accountPage(query = {}) {
      return client.get<PageResult<AccountVO>>('/api/v1/accounts', {
        query: {
          page: query.page ?? 1,
          size: query.size ?? 20,
          projectId: query.projectId,
          type: query.type,
          status: query.status
        }
      })
    },
    async accountDetail(id) {
      return client.get<AccountVO>(`/api/v1/accounts/${id}`)
    },
    async accountCreate(body) {
      return client.post<Id>('/api/v1/accounts', body)
    },
    async accountUpdate(id, body) {
      return client.put<void>(`/api/v1/accounts/${id}`, body)
    },
    async accountRemove(id) {
      return client.delete<void>(`/api/v1/accounts/${id}`)
    },
    async accountConfirm(id) {
      return client.post<void>(`/api/v1/accounts/${id}/confirm`)
    },
    async accountCancel(id) {
      return client.post<void>(`/api/v1/accounts/${id}/cancel`)
    },

    async transactionPage(query = {}) {
      return client.get<PageResult<PaymentTransactionVO>>('/api/v1/transactions', {
        query: {
          page: query.page ?? 1,
          size: query.size ?? 20,
          accountId: query.accountId,
          type: query.type
        }
      })
    },
    async transactionDetail(id) {
      return client.get<PaymentTransactionVO>(`/api/v1/transactions/${id}`)
    },
    async transactionCreate(body) {
      return client.post<Id>('/api/v1/transactions', body)
    },
    async transactionUpdate(id, body) {
      return client.put<void>(`/api/v1/transactions/${id}`, body)
    },
    async transactionRemove(id) {
      return client.delete<void>(`/api/v1/transactions/${id}`)
    },

    async paymentExecute(body) {
      return client.post<Id>('/api/v1/payments/execute', body, { signed: true })
    },

    async costAllocationPage(query = {}) {
      return client.get<PageResult<CostAllocationVO>>('/api/v1/cost-allocations', {
        query: {
          page: query.page ?? 1,
          size: query.size ?? 20,
          accountRecordId: query.accountRecordId
        }
      })
    },
    async costAllocationDetail(id) {
      return client.get<CostAllocationVO>(`/api/v1/cost-allocations/${id}`)
    },
    async costAllocationCreate(body) {
      return client.post<Id>('/api/v1/cost-allocations', body)
    },
    async costAllocationUpdate(id, body) {
      return client.put<void>(`/api/v1/cost-allocations/${id}`, body)
    },
    async costAllocationRemove(id) {
      return client.delete<void>(`/api/v1/cost-allocations/${id}`)
    }
  }
}

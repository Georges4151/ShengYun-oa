/**
 * 报销域（expense）类型化 API 工厂。
 * 所有路径前缀统一 `/api/v1`，与 docs/openapi/expense.openapi.yaml paths 一致。
 * approve / reject / verify 为高危接口，自动附加 HMAC 签名头。
 */

import type { HttpClient } from '../http-client'
import type {
  Id,
  ReceiptVO,
  ReceiptCreateRequest,
  ReceiptUpdateRequest,
  ReceiptVerifyRequest,
  ReceiptPageQuery,
  ApprovalActionRequest,
  ApprovalRecordVO,
  ReceiptVerificationVO,
  PageResult
} from '@ceams/types'

export interface ExpenseApi {
  receiptPage(query?: ReceiptPageQuery): Promise<PageResult<ReceiptVO>>
  receiptDetail(id: Id): Promise<ReceiptVO>
  receiptCreate(body: ReceiptCreateRequest): Promise<Id>
  receiptUpdate(id: Id, body: ReceiptUpdateRequest): Promise<void>
  receiptRemove(id: Id): Promise<void>
  receiptSubmit(id: Id): Promise<void>
  receiptApprove(id: Id, body: ApprovalActionRequest): Promise<void>
  receiptReject(id: Id, body: ApprovalActionRequest): Promise<void>
  receiptVerify(id: Id, body: ReceiptVerifyRequest): Promise<void>
  receiptApprovals(id: Id): Promise<ApprovalRecordVO[]>
  receiptVerifications(id: Id): Promise<ReceiptVerificationVO[]>
}

export function createExpenseApi(client: HttpClient): ExpenseApi {
  return {
    async receiptPage(query = {}) {
      return client.get<PageResult<ReceiptVO>>('/api/v1/expenses', {
        query: {
          page: query.page ?? 1,
          size: query.size ?? 20,
          receiptNumber: query.receiptNumber,
          userId: query.userId,
          projectId: query.projectId,
          verificationStatus: query.verificationStatus,
          paymentStatus: query.paymentStatus,
          approvalStatus: query.approvalStatus
        }
      })
    },
    async receiptDetail(id) {
      return client.get<ReceiptVO>(`/api/v1/expenses/${id}`)
    },
    async receiptCreate(body) {
      return client.post<Id>('/api/v1/expenses', body)
    },
    async receiptUpdate(id, body) {
      return client.put<void>(`/api/v1/expenses/${id}`, body)
    },
    async receiptRemove(id) {
      return client.delete<void>(`/api/v1/expenses/${id}`)
    },
    async receiptSubmit(id) {
      return client.post<void>(`/api/v1/expenses/${id}/submit`)
    },
    async receiptApprove(id, body) {
      return client.post<void>(`/api/v1/expenses/${id}/approve`, body, { signed: true })
    },
    async receiptReject(id, body) {
      return client.post<void>(`/api/v1/expenses/${id}/reject`, body, { signed: true })
    },
    async receiptVerify(id, body) {
      return client.post<void>(`/api/v1/expenses/${id}/verify`, body, { signed: true })
    },
    async receiptApprovals(id) {
      return client.get<ApprovalRecordVO[]>(`/api/v1/expenses/${id}/approvals`)
    },
    async receiptVerifications(id) {
      return client.get<ReceiptVerificationVO[]>(`/api/v1/expenses/${id}/verifications`)
    }
  }
}

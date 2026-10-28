/**
 * 电子印章域（seal）类型化 API 工厂。
 * 所有路径前缀统一 `/api/v1`，与 docs/openapi/seal.openapi.yaml paths 一致。
 * seals/stamp 为 HMAC 高危接口，自动附加签名头。
 */
import type { HttpClient } from '../http-client'
import type {
  Id,
  PageResult,
  ElectronicSealVO,
  ElectronicSealCreateRequest,
  ElectronicSealUpdateRequest,
  ElectronicSealPageQuery,
  ElectronicSealApplicationVO,
  ElectronicSealApplicationCreateRequest,
  ElectronicSealApplicationUpdateRequest,
  ElectronicSealApplicationPageQuery,
  StampApprovalVO,
  StampApprovalCreateRequest,
  StampApprovalUpdateRequest,
  StampApprovalApproveRequest,
  StampApprovalRejectRequest,
  StampApprovalPageQuery,
  SealStampRequest,
  SealStampResultVO,
  ElectronicSealStampVO,
  ElectronicSealStampPageQuery,
  ElectronicSealUsageVO,
  ElectronicSealUsagePageQuery
} from '@ceams/types'

export interface SealApi {
  sealPage(query?: ElectronicSealPageQuery): Promise<PageResult<ElectronicSealVO>>
  sealDetail(id: Id): Promise<ElectronicSealVO>
  sealCreate(body: ElectronicSealCreateRequest): Promise<Id>
  sealUpdate(id: Id, body: ElectronicSealUpdateRequest): Promise<void>
  sealRemove(id: Id): Promise<void>

  applicationPage(query?: ElectronicSealApplicationPageQuery): Promise<PageResult<ElectronicSealApplicationVO>>
  applicationDetail(id: Id): Promise<ElectronicSealApplicationVO>
  applicationCreate(body: ElectronicSealApplicationCreateRequest): Promise<Id>
  applicationUpdate(id: Id, body: ElectronicSealApplicationUpdateRequest): Promise<void>
  applicationCancel(id: Id): Promise<void>
  applicationRemove(id: Id): Promise<void>

  approvalPage(query?: StampApprovalPageQuery): Promise<PageResult<StampApprovalVO>>
  approvalDetail(id: Id): Promise<StampApprovalVO>
  approvalCreate(body: StampApprovalCreateRequest): Promise<Id>
  approvalUpdate(id: Id, body: StampApprovalUpdateRequest): Promise<void>
  approvalApprove(id: Id, body: StampApprovalApproveRequest): Promise<void>
  approvalReject(id: Id, body: StampApprovalRejectRequest): Promise<void>
  approvalRemove(id: Id): Promise<void>

  /** 盖章（HMAC 签名，JSON body） */
  stamp(body: SealStampRequest): Promise<SealStampResultVO>

  stampPage(query?: ElectronicSealStampPageQuery): Promise<PageResult<ElectronicSealStampVO>>
  stampDetail(id: Id): Promise<ElectronicSealStampVO>
  /**
   * 盖章 PDF 下载 URL。
   * 本阶段仅返回带鉴权的下载路径字符串；真正带鉴权的 blob 二进制下载在 M5/W-5 阶段接入。
   */
  stampDownloadUrl(id: Id): string

  usagePage(query?: ElectronicSealUsagePageQuery): Promise<PageResult<ElectronicSealUsageVO>>
  usageDetail(id: Id): Promise<ElectronicSealUsageVO>
}

export function createSealApi(client: HttpClient): SealApi {
  return {
    async sealPage(query = {}) {
      return client.get<PageResult<ElectronicSealVO>>('/api/v1/seals', {
        query: {
          page: query.page ?? 1,
          size: query.size ?? 20,
          name: query.name,
          status: query.status,
          storageType: query.storageType
        }
      })
    },
    async sealDetail(id) {
      return client.get<ElectronicSealVO>(`/api/v1/seals/${id}`)
    },
    async sealCreate(body) {
      return client.post<Id>('/api/v1/seals', body)
    },
    async sealUpdate(id, body) {
      return client.put<void>(`/api/v1/seals/${id}`, body)
    },
    async sealRemove(id) {
      return client.delete<void>(`/api/v1/seals/${id}`)
    },

    async applicationPage(query = {}) {
      return client.get<PageResult<ElectronicSealApplicationVO>>('/api/v1/seal/applications', {
        query: {
          page: query.page ?? 1,
          size: query.size ?? 20,
          sealId: query.sealId,
          status: query.status,
          applicantId: query.applicantId
        }
      })
    },
    async applicationDetail(id) {
      return client.get<ElectronicSealApplicationVO>(`/api/v1/seal/applications/${id}`)
    },
    async applicationCreate(body) {
      return client.post<Id>('/api/v1/seal/applications', body)
    },
    async applicationUpdate(id, body) {
      return client.put<void>(`/api/v1/seal/applications/${id}`, body)
    },
    async applicationCancel(id) {
      return client.post<void>(`/api/v1/seal/applications/${id}/cancel`)
    },
    async applicationRemove(id) {
      return client.delete<void>(`/api/v1/seal/applications/${id}`)
    },

    async approvalPage(query = {}) {
      return client.get<PageResult<StampApprovalVO>>('/api/v1/seal/approvals', {
        query: {
          page: query.page ?? 1,
          size: query.size ?? 20,
          stampId: query.stampId,
          sealId: query.sealId,
          status: query.status
        }
      })
    },
    async approvalDetail(id) {
      return client.get<StampApprovalVO>(`/api/v1/seal/approvals/${id}`)
    },
    async approvalCreate(body) {
      return client.post<Id>('/api/v1/seal/approvals', body)
    },
    async approvalUpdate(id, body) {
      return client.put<void>(`/api/v1/seal/approvals/${id}`, body)
    },
    async approvalApprove(id, body) {
      return client.post<void>(`/api/v1/seal/approvals/${id}/approve`, body)
    },
    async approvalReject(id, body) {
      return client.post<void>(`/api/v1/seal/approvals/${id}/reject`, body)
    },
    async approvalRemove(id) {
      return client.delete<void>(`/api/v1/seal/approvals/${id}`)
    },

    async stamp(body) {
      return client.post<SealStampResultVO>('/api/v1/seals/stamp', body, { signed: true })
    },

    async stampPage(query = {}) {
      return client.get<PageResult<ElectronicSealStampVO>>('/api/v1/seal/stamps', {
        query: {
          page: query.page ?? 1,
          size: query.size ?? 20,
          sealId: query.sealId,
          projectId: query.projectId
        }
      })
    },
    async stampDetail(id) {
      return client.get<ElectronicSealStampVO>(`/api/v1/seal/stamps/${id}`)
    },
    stampDownloadUrl(id) {
      return `/api/v1/seal/stamps/${id}/download`
    },

    async usagePage(query = {}) {
      return client.get<PageResult<ElectronicSealUsageVO>>('/api/v1/seal/usages', {
        query: {
          page: query.page ?? 1,
          size: query.size ?? 20,
          sealId: query.sealId,
          userId: query.userId
        }
      })
    },
    async usageDetail(id) {
      return client.get<ElectronicSealUsageVO>(`/api/v1/seal/usages/${id}`)
    }
  }
}

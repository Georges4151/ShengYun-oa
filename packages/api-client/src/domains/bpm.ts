/**
 * 流程审批域（bpm）类型化 API 工厂。
 * 所有路径前缀统一 `/api/v1/bpm`；模板发布、实例终止、任务改派为高危接口，
 * 由 HttpClient 自动附加 HMAC 签名头（signed: true）。
 */

import type { HttpClient } from '../http-client'
import type {
  Id,
  PageResult,
  TaskVO,
  CcRecordVO,
  InstanceVO,
  InstanceDetailVO,
  BizDetailVO,
  FlowPreviewVO,
  TemplateVO,
  TemplateVersionVO,
  InstanceStartRequest,
  InstanceActionRequest,
  TaskApproveRequest,
  TaskRejectRequest,
  TaskDelegateRequest,
  TemplateCreateRequest,
  TemplateUpdateRequest,
  TemplatePublishRequest,
  TaskPageQuery,
  InstancePageQuery,
  CcPageQuery,
  TemplatePageQuery,
  DelegateVO,
  DelegateCreateRequest,
  UserOptionVO
} from '@ceams/types'

export interface BpmApi {
  pendingTasks(query?: TaskPageQuery): Promise<PageResult<TaskVO>>
  doneTasks(query?: TaskPageQuery): Promise<PageResult<TaskVO>>
  initiated(query?: InstancePageQuery): Promise<PageResult<InstanceVO>>
  ccRecords(query?: CcPageQuery): Promise<PageResult<CcRecordVO>>

  approveTask(id: Id, body: TaskApproveRequest): Promise<void>
  rejectTask(id: Id, body: TaskRejectRequest): Promise<void>
  delegateTask(id: Id, body: TaskDelegateRequest): Promise<void>

  instanceDetail(id: Id): Promise<InstanceDetailVO>
  /** 审批单据业务详情：展示被审批业务单据的字段（label/value）与附件分组 */
  instanceBizDetail(id: Id): Promise<BizDetailVO>
  /**
   * 受控附件下载路径。
   *
   * 该接口返回文件流且要求 Authorization 头，无法用 `<img src>` / `<iframe src>` 直接访问，
   * HttpClient 仅支持文本响应解包，故此处只拼接路径，Blob 拉取由调用方用带令牌的 fetch 完成
   * （Web 端见 apps/web/src/services/http.ts 的 fetchInstanceFile）。
   */
  instanceFileUrl(id: Id, name: string): string
  /** 发起前流程预览：按模板与业务数据解析预计审批人 */
  instancePreview(body: {
    templateCode: string
    bizType?: string
    bizId?: Id
    subjectUserId?: Id
  }): Promise<FlowPreviewVO>
  startInstance(body: InstanceStartRequest): Promise<Id>
  withdrawInstance(id: Id, body: InstanceActionRequest): Promise<void>
  terminateInstance(id: Id, body: InstanceActionRequest): Promise<void>

  templatePage(query?: TemplatePageQuery): Promise<PageResult<TemplateVO>>
  templateDetail(id: Id): Promise<TemplateVO>
  templateVersions(id: Id): Promise<TemplateVersionVO[]>
  templateCreate(body: TemplateCreateRequest): Promise<Id>
  templateUpdate(id: Id, body: TemplateUpdateRequest): Promise<void>
  templateEnable(id: Id): Promise<void>
  templateDisable(id: Id): Promise<void>
  templateDelete(id: Id): Promise<void>
  templatePublish(id: Id, body: TemplatePublishRequest): Promise<Id>

  // 用户自助代理
  delegatePage(query?: { page?: number; size?: number }): Promise<PageResult<DelegateVO>>
  delegateCandidates(keyword?: string, limit?: number): Promise<UserOptionVO[]>
  delegateCreate(body: DelegateCreateRequest): Promise<Id>
  delegateCancel(id: Id): Promise<void>
}

export function createBpmApi(client: HttpClient): BpmApi {
  return {
    async pendingTasks(query = {}) {
      return client.get<PageResult<TaskVO>>('/api/v1/bpm/tasks/pending', {
        query: {
          page: query.page ?? 1,
          size: query.size ?? 20,
          bizType: query.bizType,
          overdue: query.overdue
        }
      })
    },
    async doneTasks(query = {}) {
      return client.get<PageResult<TaskVO>>('/api/v1/bpm/tasks/done', {
        query: {
          page: query.page ?? 1,
          size: query.size ?? 20,
          bizType: query.bizType
        }
      })
    },
    async initiated(query = {}) {
      return client.get<PageResult<InstanceVO>>('/api/v1/bpm/tasks/initiated', {
        query: {
          page: query.page ?? 1,
          size: query.size ?? 20,
          status: query.status,
          bizType: query.bizType
        }
      })
    },
    async ccRecords(query = {}) {
      return client.get<PageResult<CcRecordVO>>('/api/v1/bpm/tasks/cc', {
        query: {
          page: query.page ?? 1,
          size: query.size ?? 20,
          bizType: query.bizType,
          readStatus: query.readStatus
        }
      })
    },

    async approveTask(id, body) {
      return client.post<void>(`/api/v1/bpm/tasks/${id}/approve`, body)
    },
    async rejectTask(id, body) {
      return client.post<void>(`/api/v1/bpm/tasks/${id}/reject`, body)
    },
    async delegateTask(id, body) {
      return client.post<void>(`/api/v1/bpm/tasks/${id}/delegate`, body, { signed: true })
    },

    async instanceDetail(id) {
      return client.get<InstanceDetailVO>(`/api/v1/bpm/instances/${id}`)
    },
    async instanceBizDetail(id) {
      return client.get<BizDetailVO>(`/api/v1/bpm/instances/${id}/biz-detail`)
    },
    instanceFileUrl(id, name) {
      return `/api/v1/bpm/instances/${id}/files/${encodeURIComponent(name)}`
    },
    async instancePreview(body) {
      return client.post<FlowPreviewVO>('/api/v1/bpm/instances/preview', body)
    },
    async startInstance(body) {
      return client.post<Id>('/api/v1/bpm/instances', body)
    },
    async withdrawInstance(id, body) {
      return client.post<void>(`/api/v1/bpm/instances/${id}/withdraw`, body)
    },
    async terminateInstance(id, body) {
      return client.post<void>(`/api/v1/bpm/instances/${id}/terminate`, body, { signed: true })
    },

    async templatePage(query = {}) {
      return client.get<PageResult<TemplateVO>>('/api/v1/bpm/templates', {
        query: {
          page: query.page ?? 1,
          size: query.size ?? 20,
          bizType: query.bizType,
          status: query.status,
          keyword: query.keyword
        }
      })
    },
    async templateDetail(id) {
      return client.get<TemplateVO>(`/api/v1/bpm/templates/${id}`)
    },
    async templateVersions(id) {
      return client.get<TemplateVersionVO[]>(`/api/v1/bpm/templates/${id}/versions`)
    },
    async templateCreate(body) {
      return client.post<Id>('/api/v1/bpm/templates', body)
    },
    async templateUpdate(id, body) {
      return client.put<void>(`/api/v1/bpm/templates/${id}`, body)
    },
    async templateEnable(id) {
      return client.put<void>(`/api/v1/bpm/templates/${id}/enable`)
    },
    async templateDisable(id) {
      return client.put<void>(`/api/v1/bpm/templates/${id}/disable`)
    },
    async templateDelete(id) {
      return client.delete<void>(`/api/v1/bpm/templates/${id}`)
    },
    async templatePublish(id, body) {
      return client.post<Id>(`/api/v1/bpm/templates/${id}/publish`, body, { signed: true })
    },

    async delegatePage(query = {}) {
      return client.get<PageResult<DelegateVO>>('/api/v1/bpm/delegates', {
        query: { page: query.page ?? 1, size: query.size ?? 20 }
      })
    },
    async delegateCandidates(keyword, limit = 20) {
      return client.get<UserOptionVO[]>('/api/v1/bpm/delegates/candidates', {
        query: { keyword, limit }
      })
    },
    async delegateCreate(body) {
      return client.post<Id>('/api/v1/bpm/delegates', body)
    },
    async delegateCancel(id) {
      return client.post<void>(`/api/v1/bpm/delegates/${id}/cancel`)
    }
  }
}

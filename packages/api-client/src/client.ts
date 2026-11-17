/**
 * 顶层 CEAMS API 客户端工厂。
 * 通过注入 `HttpClientOptions` 构造统一的 `HttpClient`，
 * 并装配 iam / masterdata / project 等各域类型化 API（含企业微信 integration 域）。
 */

import { HttpClient } from './http-client'
import type { HttpClientOptions } from './http-client'
import { createIamApi } from './domains/iam'
import type { IamApi } from './domains/iam'
import { createMasterdataApi } from './domains/masterdata'
import type { MasterdataApi } from './domains/masterdata'
import { createProjectApi } from './domains/project'
import type { ProjectApi } from './domains/project'
import { createHrApi } from './domains/hr'
import type { HrApi } from './domains/hr'
import { createAdminApi } from './domains/admin'
import type { AdminApi } from './domains/admin'
import { createExpenseApi } from './domains/expense'
import type { ExpenseApi } from './domains/expense'
import { createPaymentApi } from './domains/payment'
import type { PaymentApi } from './domains/payment'
import { createBankApi } from './domains/bank'
import type { BankApi } from './domains/bank'
import { createSealApi } from './domains/seal'
import type { SealApi } from './domains/seal'
import { createNotificationApi } from './domains/notification'
import type { NotificationApi } from './domains/notification'
import { createReportApi } from './domains/report'
import type { ReportApi } from './domains/report'
import { createAiApi } from './domains/ai'
import type { AiApi } from './domains/ai'
import { createBpmApi } from './domains/bpm'
import type { BpmApi } from './domains/bpm'
import { createDocApi } from './domains/doc'
import type { DocApi } from './domains/doc'
import { createIntegrationApi } from './domains/integration'
import type { IntegrationApi } from './domains/integration'

export interface CeamsApiClient {
  iam: IamApi
  masterdata: MasterdataApi
  project: ProjectApi
  hr: HrApi
  admin: AdminApi
  expense: ExpenseApi
  payment: PaymentApi
  bank: BankApi
  seal: SealApi
  notification: NotificationApi
  report: ReportApi
  ai: AiApi
  bpm: BpmApi
  doc: DocApi
  integration: IntegrationApi
}

export function createCeamsApiClient(options: HttpClientOptions): CeamsApiClient {
  const client = new HttpClient(options)
  return {
    iam: createIamApi(client),
    masterdata: createMasterdataApi(client),
    project: createProjectApi(client),
    hr: createHrApi(client),
    admin: createAdminApi(client),
    expense: createExpenseApi(client),
    payment: createPaymentApi(client),
    bank: createBankApi(client),
    seal: createSealApi(client),
    notification: createNotificationApi(client),
    report: createReportApi(client),
    ai: createAiApi(client),
    bpm: createBpmApi(client),
    doc: createDocApi(client),
    integration: createIntegrationApi(client)
  }
}
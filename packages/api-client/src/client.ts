/**
 * 顶层 CEAMS API 客户端工厂。
 * 通过注入 `HttpClientOptions` 构造统一的 `HttpClient`，
 * 并装配 iam / masterdata / project 三域类型化 API。
 */

import { HttpClient } from './http-client'
import type { HttpClientOptions } from './http-client'
import { createIamApi } from './domains/iam'
import type { IamApi } from './domains/iam'
import { createMasterdataApi } from './domains/masterdata'
import type { MasterdataApi } from './domains/masterdata'
import { createProjectApi } from './domains/project'
import type { ProjectApi } from './domains/project'
import { createExpenseApi } from './domains/expense'
import type { ExpenseApi } from './domains/expense'
import { createPaymentApi } from './domains/payment'
import type { PaymentApi } from './domains/payment'
import { createBankApi } from './domains/bank'
import type { BankApi } from './domains/bank'
import { createSealApi } from './domains/seal'
import type { SealApi } from './domains/seal'

export interface CeamsApiClient {
  iam: IamApi
  masterdata: MasterdataApi
  project: ProjectApi
  expense: ExpenseApi
  payment: PaymentApi
  bank: BankApi
  seal: SealApi
}

export function createCeamsApiClient(options: HttpClientOptions): CeamsApiClient {
  const client = new HttpClient(options)
  return {
    iam: createIamApi(client),
    masterdata: createMasterdataApi(client),
    project: createProjectApi(client),
    expense: createExpenseApi(client),
    payment: createPaymentApi(client),
    bank: createBankApi(client),
    seal: createSealApi(client)
  }
}
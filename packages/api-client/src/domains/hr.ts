/**
 * 人事域（hr）类型化 API 工厂。
 * 所有路径前缀统一 `/api/v1`，与后端 HrEmployeeController / HrContractController /
 * HrAttendanceController / HrLeaveController 保持一致。
 */

import type { HttpClient } from '../http-client'
import type {
  Id,
  PageResult,
  HrEmployeeStatus,
  EmployeeVO,
  EmployeeCreateRequest,
  EmployeeUpdateRequest,
  EmployeeChangeRequest,
  EmployeeChangeVO,
  EmployeePageQuery,
  EmployeeTreeVO,
  OrgTreeNodeVO,
  OrgTreeNodeType,
  JobLevelVO,
  JobLevelCreateRequest,
  JobLevelUpdateRequest,
  PositionVO,
  PositionCreateRequest,
  PositionUpdateRequest,
  ContractVO,
  ContractCreateRequest,
  ContractUpdateRequest,
  ContractPageQuery,
  LeaveQuotaVO,
  LeaveQuotaAdjustRequest,
  LeaveQuotaInitRequest,
  LeaveQuotaQuery,
  ScheduleVO,
  ScheduleCreateRequest,
  ScheduleUpdateRequest,
  ScheduleBatchRequest,
  ScheduleListQuery,
  CheckinVO,
  CheckinClockRequest,
  CheckinPageQuery,
  CheckinSupplementVO,
  CheckinSupplementCreateRequest,
  LeaveRequestVO,
  LeaveRequestCreateRequest,
  SalaryVO,
  SalaryCreateRequest,
  SalaryPageQuery,
  SalaryChangeVO,
  SalaryChangeApplyRequest,
  SalaryChangePageQuery,
  PerformanceTemplateVO,
  PerformanceTemplateSaveRequest,
  PerformanceReviewVO,
  PerformanceBatchResultVO,
  PerformanceReviewBatchRequest,
  PerformanceSelfRequest,
  PerformanceLeaderRequest,
  PerformanceReviewRequest,
  PerformanceReviewPageQuery,
  SalaryCalcVO,
  PayrollCalcResultVO,
  PayrollCalcRequest,
  PayrollAdjustRequest,
  PayrollConfirmRequest,
  PayrollPageQuery,
  SalarySlipVO,
  SalarySlipPublishResultVO,
  SalarySlipPublishRequest,
  SalarySlipPageQuery
} from '@ceams/types'

/** 组织树过滤条件：状态 / 关键字 / 部门。 */
export interface OrgTreeQuery {
  status?: HrEmployeeStatus
  keyword?: string
  deptId?: Id
  /** 未指定 status 时是否包含已离职员工，默认 false（仅“全部员工”筛选传 true） */
  includeResigned?: boolean
}

/** 组织树子节点查询：在根节点下按父节点类型与 ID 取直接子节点。 */
export interface OrgTreeChildrenQuery extends OrgTreeQuery {
  nodeType: OrgTreeNodeType
  nodeId: Id
}

export interface HrApi {
  // 员工档案
  employeePage(query?: EmployeePageQuery): Promise<PageResult<EmployeeVO>>
  employeeDetail(id: Id): Promise<EmployeeVO>
  myProfile(): Promise<EmployeeVO>
  employeeCreate(body: EmployeeCreateRequest): Promise<Id>
  employeeUpdate(id: Id, body: EmployeeUpdateRequest): Promise<void>
  employeeChanges(id: Id): Promise<EmployeeChangeVO[]>
  /** 在职直接下属（离职前下属转交选择） */
  employeeSubordinates(id: Id): Promise<EmployeeVO[]>
  employeeRegular(id: Id, body: EmployeeChangeRequest): Promise<Id>
  employeeTransfer(id: Id, body: EmployeeChangeRequest): Promise<Id>
  employeePromotion(id: Id, body: EmployeeChangeRequest): Promise<Id>
  employeeResign(id: Id, body: EmployeeChangeRequest): Promise<Id>
  employeeTree(query?: { deptId?: Id; status?: HrEmployeeStatus }): Promise<EmployeeTreeVO[]>
  /** 组织树（部门 → 子部门 → 员工，员工按直属上级递归嵌套；排序由后端决定） */
  orgTree(query?: OrgTreeQuery): Promise<OrgTreeNodeVO[]>
  /** 组织树根节点（children 已裁剪，hasChildren 标识是否有下级，供懒加载使用） */
  orgTreeRoots(query?: OrgTreeQuery): Promise<OrgTreeNodeVO[]>
  /** 组织树直接子节点（children 已裁剪，hasChildren 标识是否有下级，供懒加载使用） */
  orgTreeChildren(query: OrgTreeChildrenQuery): Promise<OrgTreeNodeVO[]>

  // 职级字典
  jobLevelList(): Promise<JobLevelVO[]>
  /** 启用中的职级（仅 ENABLED），供下拉选择使用 */
  jobLevelEnabledList(): Promise<JobLevelVO[]>
  jobLevelCreate(body: JobLevelCreateRequest): Promise<Id>
  jobLevelUpdate(id: Id, body: JobLevelUpdateRequest): Promise<void>

  // 岗位
  positionList(): Promise<PositionVO[]>
  positionCreate(body: PositionCreateRequest): Promise<Id>
  positionUpdate(id: Id, body: PositionUpdateRequest): Promise<void>

  // 劳动合同
  contractPage(query?: ContractPageQuery): Promise<PageResult<ContractVO>>
  contractExpiring(days?: number): Promise<ContractVO[]>
  contractCreate(body: ContractCreateRequest): Promise<Id>
  contractUpdate(id: Id, body: ContractUpdateRequest): Promise<void>
  /** 提交劳动合同审批，返回 BPM 流程实例 ID。 */
  contractSubmitApproval(id: Id): Promise<Id>

  // 假期额度
  leaveQuotaList(query?: LeaveQuotaQuery): Promise<LeaveQuotaVO[]>
  leaveQuotaAdjust(id: Id, body: LeaveQuotaAdjustRequest): Promise<void>
  leaveQuotaInit(body: LeaveQuotaInitRequest): Promise<void>

  // 排班
  scheduleList(query?: ScheduleListQuery): Promise<ScheduleVO[]>
  scheduleCreate(body: ScheduleCreateRequest): Promise<Id>
  scheduleUpdate(id: Id, body: ScheduleUpdateRequest): Promise<void>
  scheduleBatch(body: ScheduleBatchRequest): Promise<number>

  // 打卡 / 补卡
  checkinClock(body: CheckinClockRequest): Promise<CheckinVO>
  myCheckins(month?: string): Promise<CheckinVO[]>
  checkinPage(query?: CheckinPageQuery): Promise<PageResult<CheckinVO>>
  checkinSupplement(body: CheckinSupplementCreateRequest): Promise<Id>
  mySupplements(): Promise<CheckinSupplementVO[]>

  // 请假
  createLeaveRequest(body: LeaveRequestCreateRequest): Promise<Id>
  myLeaveRequests(): Promise<LeaveRequestVO[]>

  // ---------- 薪资台账（M2） ----------
  /** 本人当前薪资（脱敏） */
  mySalary(): Promise<SalaryVO>
  salaryPage(query?: SalaryPageQuery): Promise<PageResult<SalaryVO>>
  salaryCreate(body: SalaryCreateRequest): Promise<Id>
  /** 发起薪资变动（提交即启动 BPM） */
  salaryChangeApply(body: SalaryChangeApplyRequest): Promise<Id>
  salaryChangePage(query?: SalaryChangePageQuery): Promise<PageResult<SalaryChangeVO>>
  mySalaryChanges(query?: { page?: number; size?: number }): Promise<PageResult<SalaryChangeVO>>

  // ---------- 绩效模板 / 考评单（M2） ----------
  performanceTemplateList(): Promise<PerformanceTemplateVO[]>
  performanceTemplateCreate(body: PerformanceTemplateSaveRequest): Promise<Id>
  performanceTemplateUpdate(id: Id, body: PerformanceTemplateSaveRequest): Promise<Id>
  performanceReviewBatch(body: PerformanceReviewBatchRequest): Promise<PerformanceBatchResultVO>
  performanceReviewPage(query?: PerformanceReviewPageQuery): Promise<PageResult<PerformanceReviewVO>>
  myPerformanceReviews(query?: { page?: number; size?: number }): Promise<PageResult<PerformanceReviewVO>>
  performanceSelf(id: Id, body: PerformanceSelfRequest): Promise<void>
  performanceLeader(id: Id, body: PerformanceLeaderRequest): Promise<void>
  /** HR 复核定稿 */
  performanceReview(id: Id, body: PerformanceReviewRequest): Promise<void>

  // ---------- 月度工资核算（M2） ----------
  payrollCalc(body: PayrollCalcRequest): Promise<PayrollCalcResultVO>
  payrollCalcPage(query?: PayrollPageQuery): Promise<PageResult<SalaryCalcVO>>
  payrollCalcAdjust(id: Id, body: PayrollAdjustRequest): Promise<SalaryCalcVO>
  payrollConfirm(body: PayrollConfirmRequest): Promise<number>

  // ---------- 工资条（M2） ----------
  salarySlipPublish(body: SalarySlipPublishRequest): Promise<SalarySlipPublishResultVO>
  salarySlipPage(query?: SalarySlipPageQuery): Promise<PageResult<SalarySlipVO>>
  /** 本人工资条（后端在接口内自动置为已读） */
  mySalarySlips(): Promise<SalarySlipVO[]>
  salarySlipMarkRead(id: Id): Promise<SalarySlipVO>
  /** 工资条导出路径（xlsx 二进制流，鉴权下载由调用方自建 fetch 完成） */
  salarySlipExportPath(calcMonth: string): string
}

export function createHrApi(client: HttpClient): HrApi {
  return {
    async employeePage(query = {}) {
      return client.get<PageResult<EmployeeVO>>('/api/v1/hr/employees', {
        query: {
          page: query.page ?? 1,
          size: query.size ?? 20,
          deptId: query.deptId,
          status: query.status,
          keyword: query.keyword
        }
      })
    },
    async employeeDetail(id) {
      return client.get<EmployeeVO>(`/api/v1/hr/employees/${id}`)
    },
    async myProfile() {
      return client.get<EmployeeVO>('/api/v1/hr/employees/me')
    },
    async employeeCreate(body) {
      return client.post<Id>('/api/v1/hr/employees', body)
    },
    async employeeUpdate(id, body) {
      return client.put<void>(`/api/v1/hr/employees/${id}`, body)
    },
    async employeeChanges(id) {
      return client.get<EmployeeChangeVO[]>(`/api/v1/hr/employees/${id}/changes`)
    },
    async employeeSubordinates(id) {
      return client.get<EmployeeVO[]>(`/api/v1/hr/employees/${id}/subordinates`)
    },
    async employeeRegular(id, body) {
      return client.post<Id>(`/api/v1/hr/employees/${id}/regular`, body)
    },
    async employeeTransfer(id, body) {
      return client.post<Id>(`/api/v1/hr/employees/${id}/transfer`, body)
    },
    async employeePromotion(id, body) {
      return client.post<Id>(`/api/v1/hr/employees/${id}/promotion`, body)
    },
    async employeeResign(id, body) {
      return client.post<Id>(`/api/v1/hr/employees/${id}/resign`, body)
    },
    async employeeTree(query = {}) {
      return client.get<EmployeeTreeVO[]>('/api/v1/hr/employees/tree', {
        query: { deptId: query.deptId, status: query.status }
      })
    },
    async orgTree(query = {}) {
      return client.get<OrgTreeNodeVO[]>('/api/v1/hr/employees/org-tree', {
        query: {
          status: query.status,
          keyword: query.keyword,
          deptId: query.deptId,
          includeResigned: query.includeResigned
        }
      })
    },
    async orgTreeRoots(query = {}) {
      return client.get<OrgTreeNodeVO[]>('/api/v1/hr/employees/org-tree/roots', {
        query: {
          status: query.status,
          keyword: query.keyword,
          deptId: query.deptId,
          includeResigned: query.includeResigned
        }
      })
    },
    async orgTreeChildren(query) {
      return client.get<OrgTreeNodeVO[]>('/api/v1/hr/employees/org-tree/children', {
        query: {
          nodeType: query.nodeType,
          nodeId: query.nodeId,
          status: query.status,
          keyword: query.keyword,
          deptId: query.deptId,
          includeResigned: query.includeResigned
        }
      })
    },

    async jobLevelList() {
      return client.get<JobLevelVO[]>('/api/v1/hr/job-levels')
    },
    async jobLevelEnabledList() {
      return client.get<JobLevelVO[]>('/api/v1/hr/job-levels/enabled')
    },
    async jobLevelCreate(body) {
      return client.post<Id>('/api/v1/hr/job-levels', body)
    },
    async jobLevelUpdate(id, body) {
      return client.put<void>(`/api/v1/hr/job-levels/${id}`, body)
    },

    async positionList() {
      return client.get<PositionVO[]>('/api/v1/hr/positions')
    },
    async positionCreate(body) {
      return client.post<Id>('/api/v1/hr/positions', body)
    },
    async positionUpdate(id, body) {
      return client.put<void>(`/api/v1/hr/positions/${id}`, body)
    },

    async contractPage(query = {}) {
      return client.get<PageResult<ContractVO>>('/api/v1/hr/contracts', {
        query: {
          page: query.page ?? 1,
          size: query.size ?? 20,
          employeeId: query.employeeId,
          status: query.status
        }
      })
    },
    async contractExpiring(days = 30) {
      return client.get<ContractVO[]>('/api/v1/hr/contracts/expiring', { query: { days } })
    },
    async contractCreate(body) {
      return client.post<Id>('/api/v1/hr/contracts', body)
    },
    async contractUpdate(id, body) {
      return client.put<void>(`/api/v1/hr/contracts/${id}`, body)
    },
    async contractSubmitApproval(id) {
      return client.post<Id>(`/api/v1/hr/contracts/${id}/submit-approval`)
    },

    async leaveQuotaList(query = {}) {
      return client.get<LeaveQuotaVO[]>('/api/v1/hr/leave-quotas', {
        query: { employeeId: query.employeeId, year: query.year }
      })
    },
    async leaveQuotaAdjust(id, body) {
      return client.put<void>(`/api/v1/hr/leave-quotas/${id}`, body)
    },
    async leaveQuotaInit(body) {
      return client.post<void>('/api/v1/hr/leave-quotas/init', body)
    },

    async scheduleList(query = {}) {
      return client.get<ScheduleVO[]>('/api/v1/hr/schedules', {
        query: { employeeId: query.employeeId, month: query.month }
      })
    },
    async scheduleCreate(body) {
      return client.post<Id>('/api/v1/hr/schedules', body)
    },
    async scheduleUpdate(id, body) {
      return client.put<void>(`/api/v1/hr/schedules/${id}`, body)
    },
    async scheduleBatch(body) {
      return client.post<number>('/api/v1/hr/schedules/batch', body)
    },

    async checkinClock(body) {
      return client.post<CheckinVO>('/api/v1/hr/checkins/clock', body)
    },
    async myCheckins(month) {
      return client.get<CheckinVO[]>('/api/v1/hr/checkins/my', { query: { month } })
    },
    async checkinPage(query = {}) {
      return client.get<PageResult<CheckinVO>>('/api/v1/hr/checkins', {
        query: {
          page: query.page ?? 1,
          size: query.size ?? 20,
          employeeId: query.employeeId,
          startDate: query.startDate,
          endDate: query.endDate,
          status: query.status
        }
      })
    },
    async checkinSupplement(body) {
      return client.post<Id>('/api/v1/hr/checkins/supplement', body)
    },
    async mySupplements() {
      return client.get<CheckinSupplementVO[]>('/api/v1/hr/checkins/supplements/my')
    },

    async createLeaveRequest(body) {
      return client.post<Id>('/api/v1/hr/leave-requests', body)
    },
    async myLeaveRequests() {
      return client.get<LeaveRequestVO[]>('/api/v1/hr/leave-requests/my')
    },

    // ---------- 薪资台账（M2） ----------
    async mySalary() {
      return client.get<SalaryVO>('/api/v1/hr/salaries/my')
    },
    async salaryPage(query = {}) {
      return client.get<PageResult<SalaryVO>>('/api/v1/hr/salaries', {
        query: {
          page: query.page ?? 1,
          size: query.size ?? 20,
          employeeId: query.employeeId,
          status: query.status
        }
      })
    },
    async salaryCreate(body) {
      return client.post<Id>('/api/v1/hr/salaries', body)
    },
    async salaryChangeApply(body) {
      return client.post<Id>('/api/v1/hr/salary-changes', body)
    },
    async salaryChangePage(query = {}) {
      return client.get<PageResult<SalaryChangeVO>>('/api/v1/hr/salary-changes', {
        query: {
          page: query.page ?? 1,
          size: query.size ?? 20,
          employeeId: query.employeeId,
          changeType: query.changeType,
          status: query.status
        }
      })
    },
    async mySalaryChanges(query = {}) {
      return client.get<PageResult<SalaryChangeVO>>('/api/v1/hr/salary-changes/my', {
        query: { page: query.page ?? 1, size: query.size ?? 20 }
      })
    },

    // ---------- 绩效模板 / 考评单（M2） ----------
    async performanceTemplateList() {
      return client.get<PerformanceTemplateVO[]>('/api/v1/hr/performance/templates')
    },
    async performanceTemplateCreate(body) {
      return client.post<Id>('/api/v1/hr/performance/templates', body)
    },
    async performanceTemplateUpdate(id, body) {
      return client.put<Id>(`/api/v1/hr/performance/templates/${id}`, body)
    },
    async performanceReviewBatch(body) {
      return client.post<PerformanceBatchResultVO>('/api/v1/hr/performance/reviews/batch', body)
    },
    async performanceReviewPage(query = {}) {
      return client.get<PageResult<PerformanceReviewVO>>('/api/v1/hr/performance/reviews', {
        query: {
          page: query.page ?? 1,
          size: query.size ?? 20,
          templateId: query.templateId,
          period: query.period,
          status: query.status,
          employeeId: query.employeeId
        }
      })
    },
    async myPerformanceReviews(query = {}) {
      return client.get<PageResult<PerformanceReviewVO>>('/api/v1/hr/performance/reviews/my', {
        query: { page: query.page ?? 1, size: query.size ?? 20 }
      })
    },
    async performanceSelf(id, body) {
      return client.post<void>(`/api/v1/hr/performance/reviews/${id}/self`, body)
    },
    async performanceLeader(id, body) {
      return client.post<void>(`/api/v1/hr/performance/reviews/${id}/leader`, body)
    },
    async performanceReview(id, body) {
      return client.post<void>(`/api/v1/hr/performance/reviews/${id}/review`, body)
    },

    // ---------- 月度工资核算（M2） ----------
    async payrollCalc(body) {
      return client.post<PayrollCalcResultVO>('/api/v1/hr/payroll/calc', body)
    },
    async payrollCalcPage(query = {}) {
      return client.get<PageResult<SalaryCalcVO>>('/api/v1/hr/payroll/calc', {
        query: {
          page: query.page ?? 1,
          size: query.size ?? 20,
          calcMonth: query.calcMonth,
          status: query.status,
          employeeId: query.employeeId
        }
      })
    },
    async payrollCalcAdjust(id, body) {
      return client.put<SalaryCalcVO>(`/api/v1/hr/payroll/calc/${id}`, body)
    },
    async payrollConfirm(body) {
      return client.post<number>('/api/v1/hr/payroll/confirm', body)
    },

    // ---------- 工资条（M2） ----------
    async salarySlipPublish(body) {
      return client.post<SalarySlipPublishResultVO>('/api/v1/hr/payroll/slips/publish', body)
    },
    async salarySlipPage(query = {}) {
      return client.get<PageResult<SalarySlipVO>>('/api/v1/hr/payroll/slips', {
        query: {
          page: query.page ?? 1,
          size: query.size ?? 20,
          calcMonth: query.calcMonth,
          employeeId: query.employeeId,
          readStatus: query.readStatus
        }
      })
    },
    async mySalarySlips() {
      return client.get<SalarySlipVO[]>('/api/v1/hr/payroll/slips/my')
    },
    async salarySlipMarkRead(id) {
      return client.put<SalarySlipVO>(`/api/v1/hr/payroll/slips/${id}/read`)
    },
    salarySlipExportPath(calcMonth) {
      return `/api/v1/hr/payroll/slips/export?calcMonth=${encodeURIComponent(calcMonth)}`
    }
  }
}

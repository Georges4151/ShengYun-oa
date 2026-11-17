/**
 * 人事域（hr）类型：员工档案 / 岗位 / 劳动合同 / 假期额度 / 排班 / 打卡 / 请假。
 *
 * 字段与后端 VO / DTO 严格一致：
 * - Id = string（后端雪花 ID 序列化为字符串）
 * - 天数 / 经纬度等 BigDecimal 一律字符串（复用 Money）
 * - 日期 yyyy-MM-dd、时间 HH:mm:ss、日期时间 ISO8601
 * 注意：枚举统一使用 Hr 前缀，避免与项目合同域的 ContractStatus 等同名类型冲突。
 */

import type { DateTime, Id, Money } from './common'

// ---------- 枚举 ----------
export type HrGender = 'MALE' | 'FEMALE' | 'UNKNOWN'
export type HrEmploymentType = 'FULL_TIME' | 'PART_TIME' | 'INTERN' | 'CONTRACTOR'
export type HrEmployeeStatus = 'PROBATION' | 'ACTIVE' | 'LEAVE' | 'TERMINATED'
export type HrChangeType = 'REGULAR' | 'TRANSFER' | 'RESIGN'
export type HrApprovalStatus =
  | 'PENDING'
  | 'APPROVED'
  | 'REJECTED'
  | 'WITHDRAWN'
  | 'CANCELLED'
  | 'TERMINATED'
export type HrContractType = 'FIXED' | 'UNFIXED' | 'INTERN'
export type HrContractStatus = 'ACTIVE' | 'EXPIRED' | 'TERMINATED'
export type HrLeaveType = 'ANNUAL' | 'SICK' | 'PERSONAL'
export type HrScheduleType = 'FIXED' | 'TEMP'
export type HrCheckinType = 'IN' | 'OUT'
export type HrCheckinSource = 'MINI' | 'DEVICE' | 'MANUAL'
export type HrCheckinStatus = 'NORMAL' | 'LATE' | 'EARLY' | 'ABSENT'

// ---------- 员工档案 ----------
export interface EmployeeVO {
  id: Id
  userId?: Id | null
  employeeNo: string
  name: string
  gender?: HrGender | null
  birthDate?: string | null
  /** 身份证号（脱敏） */
  idCard?: string | null
  /** 手机号（脱敏） */
  phone?: string | null
  /** 邮箱（脱敏） */
  email?: string | null
  deptId?: Id | null
  positionId?: Id | null
  employmentType?: HrEmploymentType | null
  hireDate?: string | null
  probationEndDate?: string | null
  regularDate?: string | null
  leaveDate?: string | null
  status: HrEmployeeStatus
  emergencyContact?: string | null
  /** 紧急联系电话（脱敏） */
  emergencyPhone?: string | null
  idCardFileId?: Id | null
  remark?: string | null
  createTime?: DateTime
}

export interface EmployeeCreateRequest {
  /** 绑定系统用户 ID（为空表示暂不绑定账号） */
  userId?: Id | null
  employeeNo: string
  name: string
  gender?: HrGender
  birthDate?: string
  /** 身份证号（明文） */
  idCard?: string
  /** 手机号（明文） */
  phone?: string
  email?: string
  deptId?: Id
  positionId?: Id
  employmentType?: HrEmploymentType
  hireDate?: string
  probationEndDate?: string
  status?: HrEmployeeStatus
  emergencyContact?: string
  /** 紧急联系电话（明文） */
  emergencyPhone?: string
  idCardFileId?: Id
  remark?: string
}

/** 修改员工档案：敏感字段留空表示不修改（后端仅处理非空白值）。 */
export interface EmployeeUpdateRequest {
  /** 传 "0" 表示解绑系统账号 */
  userId?: Id
  employeeNo?: string
  name?: string
  gender?: HrGender
  birthDate?: string
  idCard?: string
  phone?: string
  email?: string
  deptId?: Id
  positionId?: Id
  employmentType?: HrEmploymentType
  hireDate?: string
  probationEndDate?: string
  regularDate?: string
  leaveDate?: string
  status?: HrEmployeeStatus
  emergencyContact?: string
  emergencyPhone?: string
  idCardFileId?: Id
  remark?: string
}

export interface EmployeeChangeRequest {
  effectiveDate: string
  reason?: string
  /** 调岗目标部门 ID（仅调岗使用） */
  targetDeptId?: Id
  /** 调岗目标岗位 ID（仅调岗使用） */
  targetPositionId?: Id
  remark?: string
}

export interface EmployeeChangeVO {
  id: Id
  employeeId: Id
  changeType: HrChangeType
  targetDeptId?: Id | null
  targetPositionId?: Id | null
  effectiveDate: string
  reason?: string | null
  approvalStatus: HrApprovalStatus
  bpmInstanceId?: Id | null
  remark?: string | null
  createTime?: DateTime
}

export interface EmployeePageQuery {
  page?: number
  size?: number
  deptId?: Id
  status?: HrEmployeeStatus
  keyword?: string
}

// ---------- 岗位 ----------
export interface PositionVO {
  id: Id
  code: string
  name: string
  level?: string | null
  jobFamily?: string | null
  sortOrder?: number | null
  status?: string | null
  remark?: string | null
  createTime?: DateTime
}

export interface PositionCreateRequest {
  code: string
  name: string
  level?: string
  jobFamily?: string
  sortOrder?: number
  status?: string
  remark?: string
}

export interface PositionUpdateRequest {
  name?: string
  level?: string
  jobFamily?: string
  sortOrder?: number
  status?: string
  remark?: string
}

// ---------- 劳动合同 ----------
export interface ContractVO {
  id: Id
  employeeId: Id
  contractNo: string
  contractType: HrContractType
  startDate?: string | null
  endDate?: string | null
  signDate?: string | null
  fileId?: Id | null
  status: HrContractStatus
  renewalRemindDate?: string | null
  remark?: string | null
  createTime?: DateTime
}

export interface ContractCreateRequest {
  employeeId: Id
  contractNo: string
  contractType: HrContractType
  startDate?: string
  endDate?: string
  signDate?: string
  fileId?: Id
  status?: HrContractStatus
  renewalRemindDate?: string
  remark?: string
}

export interface ContractUpdateRequest {
  contractNo?: string
  contractType?: HrContractType
  startDate?: string
  endDate?: string
  signDate?: string
  fileId?: Id
  status?: HrContractStatus
  renewalRemindDate?: string
  remark?: string
}

export interface ContractPageQuery {
  page?: number
  size?: number
  employeeId?: Id
  status?: HrContractStatus
}

// ---------- 假期额度 ----------
export interface LeaveQuotaVO {
  id: Id
  employeeId: Id
  year: number
  leaveType: HrLeaveType
  totalDays: Money
  usedDays?: Money | null
  remainingDays?: Money | null
  expireDate?: string | null
}

export interface LeaveQuotaAdjustRequest {
  totalDays: Money
}

export interface LeaveQuotaInitRequest {
  employeeId: Id
  year: number
}

export interface LeaveQuotaQuery {
  employeeId?: Id
  year?: number
}

// ---------- 排班 ----------
export interface ScheduleVO {
  id: Id
  employeeId: Id
  workDate: string
  shiftCode?: string | null
  startTime?: string | null
  endTime?: string | null
  scheduleType?: HrScheduleType | null
  remark?: string | null
}

export interface ScheduleCreateRequest {
  employeeId: Id
  workDate: string
  shiftCode?: string
  startTime?: string
  endTime?: string
  scheduleType?: HrScheduleType
  remark?: string
}

export interface ScheduleUpdateRequest {
  shiftCode?: string
  startTime?: string
  endTime?: string
  scheduleType?: HrScheduleType
  remark?: string
}

export interface ScheduleBatchRequest {
  employeeIds: Id[]
  startDate: string
  endDate: string
  shiftCode?: string
  startTime?: string
  endTime?: string
}

export interface ScheduleListQuery {
  employeeId?: Id
  /** 月份 yyyy-MM，为空取当前月 */
  month?: string
}

// ---------- 打卡 ----------
export interface CheckinVO {
  id: Id
  employeeId: Id
  checkinDate: string
  checkinTime: DateTime
  checkinType: HrCheckinType
  latitude?: Money | null
  longitude?: Money | null
  wifiSsid?: string | null
  wifiBssid?: string | null
  address?: string | null
  source?: HrCheckinSource | null
  status: HrCheckinStatus
  remark?: string | null
}

export interface CheckinClockRequest {
  checkinType: HrCheckinType
  latitude?: Money
  longitude?: Money
  wifiSsid?: string
  wifiBssid?: string
  address?: string
  source?: HrCheckinSource
}

export interface CheckinPageQuery {
  page?: number
  size?: number
  employeeId?: Id
  startDate?: string
  endDate?: string
  status?: HrCheckinStatus
}

export interface CheckinSupplementVO {
  id: Id
  employeeId: Id
  checkinDate: string
  checkinTime: DateTime
  checkinType: HrCheckinType
  reason?: string | null
  approvalStatus: HrApprovalStatus
  bpmInstanceId?: Id | null
  createTime?: DateTime
}

export interface CheckinSupplementCreateRequest {
  checkinDate: string
  checkinTime: DateTime
  checkinType: HrCheckinType
  reason?: string
}

// ---------- 请假 ----------
export interface LeaveRequestVO {
  id: Id
  employeeId: Id
  leaveType: HrLeaveType
  startDate: string
  endDate: string
  days?: Money | null
  reason?: string | null
  approvalStatus: HrApprovalStatus
  bpmInstanceId?: Id | null
  createTime?: DateTime
}

export interface LeaveRequestCreateRequest {
  leaveType: HrLeaveType
  startDate: string
  endDate: string
  /** 请假天数；为空时后端按起止日期（含首尾）自动计算 */
  days?: Money
  reason?: string
}

// ---------- 薪资台账（M2） ----------
export type HrSalaryStatus = 'ACTIVE' | 'HISTORY'
export type HrSalaryChangeType = 'ADJUST' | 'PROMOTION' | 'CORRECTION'
export type HrSalaryChangeStatus = 'DRAFT' | 'APPROVING' | 'APPROVED' | 'REJECTED'

/** 薪资台账视图对象（金额为后端解密后的数值，字符串序列化）。 */
export interface SalaryVO {
  id: Id
  employeeId: Id
  employeeNo: string
  employeeName: string
  effectiveDate: string
  baseSalary: Money
  postSalary: Money
  performanceBase: Money
  allowance: Money
  status: HrSalaryStatus
  changeApplyId?: Id | null
  remark?: string | null
  createTime?: DateTime
}

export interface SalaryCreateRequest {
  employeeId: Id
  /** 生效日期 yyyy-MM-dd */
  effectiveDate: string
  baseSalary: Money
  postSalary?: Money
  performanceBase?: Money
  allowance?: Money
  remark?: string
}

export interface SalaryPageQuery {
  page?: number
  size?: number
  employeeId?: Id
  status?: HrSalaryStatus
}

/** 薪资变动申请视图对象（变更前后摘要为可读文本）。 */
export interface SalaryChangeVO {
  id: Id
  employeeId: Id
  employeeNo: string
  employeeName: string
  applicantId?: Id | null
  applicantName?: string | null
  changeType: HrSalaryChangeType
  effectiveDate: string
  /** 变更前摘要（首次建档为空） */
  beforeSummary?: string | null
  /** 变更后摘要 */
  afterSummary?: string | null
  reason?: string | null
  status: HrSalaryChangeStatus
  bpmInstanceId?: Id | null
  createTime?: DateTime
}

/** 发起薪资变动申请（提交即启动 BPM）；金额为空表示沿用当前生效台账。 */
export interface SalaryChangeApplyRequest {
  employeeId: Id
  changeType: HrSalaryChangeType
  /** 生效日期 yyyy-MM-dd */
  effectiveDate: string
  baseSalary?: Money
  postSalary?: Money
  performanceBase?: Money
  allowance?: Money
  reason?: string
}

export interface SalaryChangePageQuery {
  page?: number
  size?: number
  employeeId?: Id
  changeType?: HrSalaryChangeType
  status?: HrSalaryChangeStatus
}

// ---------- 绩效模板 / 考评单（M2） ----------
export type HrPeriodType = 'MONTH' | 'QUARTER'
export type HrPerformanceReviewStatus =
  | 'DRAFT'
  | 'SELF_SUBMITTED'
  | 'LEADER_DONE'
  | 'APPROVED'
  | 'REJECTED'

/** 绩效考评项（模板 items_json 元素，同时用于保存入参）。 */
export interface PerformanceItem {
  name: string
  /** 权重，全部考评项合计必须为 100 */
  weight: Money
  minScore?: Money | null
  maxScore?: Money | null
  description?: string | null
}

/** 绩效系数规则：分数落在哪段取哪段系数。 */
export interface PerformanceScoreRule {
  /** 区间起值（含） */
  minScore: Money
  /** 区间止值 */
  maxScore: Money
  coefficient: Money
}

/** 绩效打分项（自评 / 上级评分明细）。 */
export interface PerformanceScoreItem {
  name: string
  weight: Money
  /** 得分 0~100 */
  score: Money
}

export interface PerformanceTemplateVO {
  id: Id
  code: string
  name: string
  positionId?: Id | null
  periodType: HrPeriodType
  items?: PerformanceItem[] | null
  scoreRules?: PerformanceScoreRule[] | null
  totalWeight?: number | null
  status?: string | null
  createTime?: DateTime
  updateTime?: DateTime
}

export interface PerformanceTemplateSaveRequest {
  code: string
  name: string
  positionId?: Id
  periodType: HrPeriodType
  /** 考评项（权重合计必须为 100） */
  items: PerformanceItem[]
  /** 系数规则（区间连续且不重叠） */
  scoreRules: PerformanceScoreRule[]
  /** ENABLED / DISABLED，为空时后端按新建 / 修改语义处理 */
  status?: string
}

/** 绩效考评单视图对象。 */
export interface PerformanceReviewVO {
  id: Id
  employeeId: Id
  employeeName?: string | null
  templateId: Id
  templateName?: string | null
  /** 模板考评项快照，自评/上级打分表单据此渲染 */
  templateItems?: PerformanceItem[] | null
  period: string
  leaderId?: Id | null
  selfItems?: PerformanceScoreItem[] | null
  selfScore?: Money | null
  selfComment?: string | null
  selfSubmittedAt?: DateTime | null
  leaderItems?: PerformanceScoreItem[] | null
  leaderScore?: Money | null
  leaderComment?: string | null
  finalScore?: Money | null
  coefficient?: Money | null
  status: HrPerformanceReviewStatus
  bpmInstanceId?: Id | null
  createTime?: DateTime
  updateTime?: DateTime
}

/** 绩效考评单批量生成结果。 */
export interface PerformanceBatchResultVO {
  /** 命中范围的员工总数 */
  total: number
  /** 本次新建的考评单数量 */
  created: number
  /** 已存在考评单而跳过的数量 */
  skipped: number
}

export interface PerformanceReviewBatchRequest {
  templateId: Id
  /** 考核周期（2026-11 / 2026-Q4） */
  period: string
  /** 部门 ID，为空表示全员 */
  deptId?: Id
}

export interface PerformanceSelfRequest {
  /** 自评打分项（必须覆盖模板全部考评项） */
  items: PerformanceScoreItem[]
  comment?: string
}

export interface PerformanceLeaderRequest {
  items: PerformanceScoreItem[]
  comment?: string
}

/** HR 绩效复核定稿请求。 */
export interface PerformanceReviewRequest {
  /** 定稿分数 0~100 */
  finalScore: Money
  comment?: string
}

export interface PerformanceReviewPageQuery {
  page?: number
  size?: number
  templateId?: Id
  period?: string
  status?: HrPerformanceReviewStatus
  employeeId?: Id
}

// ---------- 月度工资核算（M2） ----------
export type HrPayrollStatus = 'DRAFT' | 'CONFIRMED' | 'LOCKED'

/** 月度工资核算明细视图对象。 */
export interface SalaryCalcVO {
  id: Id
  employeeId: Id
  employeeNo: string
  employeeName: string
  /** 核算月 yyyy-MM */
  calcMonth: string
  baseSalary: Money
  postSalary: Money
  attendanceDays: Money
  absentDays: Money
  lateCount?: number | null
  attendanceDeduction: Money
  performanceCoefficient: Money
  performanceAmount: Money
  allowance: Money
  socialInsurance: Money
  housingFund: Money
  tax: Money
  grossPay: Money
  netPay: Money
  status: HrPayrollStatus
  confirmedBy?: Id | null
  confirmedAt?: DateTime | null
  createTime?: DateTime
  updateTime?: DateTime
}

/** 核算初始化 / 重算结果：总数、新建、重算、跳过、锁定与告警清单。 */
export interface PayrollCalcResultVO {
  calcMonth: string
  total: number
  created: number
  recalculated: number
  skipped: number
  locked: number
  /** 中文告警：哪些员工因何跳过、扣款截断、实发为负等 */
  warnings: string[]
}

export interface PayrollCalcRequest {
  /** 核算月 yyyy-MM */
  calcMonth: string
  deptId?: Id
  employeeIds?: Id[]
}

/** 手工调整（仅 DRAFT）；字段为空表示保持原值。 */
export interface PayrollAdjustRequest {
  allowance?: Money
  attendanceDeduction?: Money
  performanceCoefficient?: Money
  socialInsurance?: Money
  housingFund?: Money
  tax?: Money
}

export interface PayrollConfirmRequest {
  /** 核算月 yyyy-MM */
  calcMonth: string
  /** 为空表示确认该月全部 DRAFT 记录 */
  employeeIds?: Id[]
}

export interface PayrollPageQuery {
  page?: number
  size?: number
  calcMonth?: string
  status?: HrPayrollStatus
  employeeId?: Id
}

// ---------- 工资条（M2） ----------
export type HrSalarySlipReadStatus = 'PENDING' | 'READ'

export interface SalarySlipVO {
  id: Id
  employeeId: Id
  calcId?: Id | null
  employeeNo: string
  employeeName: string
  deptId?: Id | null
  /** 核算月 yyyy-MM */
  calcMonth: string
  baseSalary: Money
  postSalary: Money
  performanceAmount: Money
  allowance: Money
  attendanceDeduction: Money
  socialInsurance: Money
  housingFund: Money
  tax: Money
  grossPay: Money
  netPay: Money
  attendanceDays: Money
  absentDays: Money
  lateCount?: number | null
  performanceCoefficient: Money
  readStatus: HrSalarySlipReadStatus
  publishedAt?: DateTime | null
  readAt?: DateTime | null
  createTime?: DateTime
}

/** 工资条发布结果：总数、发布、跳过与中文告警清单。 */
export interface SalarySlipPublishResultVO {
  calcMonth: string
  total: number
  published: number
  skipped: number
  warnings: string[]
}

export interface SalarySlipPublishRequest {
  /** 核算月 yyyy-MM */
  calcMonth: string
  /** 为空表示发布该月全部已确认核算记录 */
  employeeIds?: Id[]
}

export interface SalarySlipPageQuery {
  page?: number
  size?: number
  calcMonth?: string
  employeeId?: Id
  readStatus?: HrSalarySlipReadStatus
}

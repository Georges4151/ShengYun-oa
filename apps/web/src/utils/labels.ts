import type {
  UserStatus,
  RoleStatus,
  PermissionType,
  Platform,
  DeviceStatus,
  OperationType,
  CategoryType,
  ContactType,
  EquipmentStatus,
  PaymentStatus,
  ProjectStatus,
  BudgetStatus,
  ApprovalStatus,
  ContractStatus,
  ReceiptType,
  VerificationStatus,
  PaymentMethod,
  ReceiptApprovalStatus,
  ApprovalRecordStatus,
  AccountType,
  AccountStatus,
  AllocationStatus,
  TargetType,
  TransactionType,
  BankStatementStatus,
  ReconciliationStatus,
  DiscrepancyType,
  ResolutionStatus,
  SealStatus,
  StorageType,
  ApplicationStatus,
  SealApprovalStatus,
  FinancialSourceType,
  PushChannel,
  HrGender,
  HrEmploymentType,
  HrEmployeeStatus,
  HrChangeType,
  HrJobLevelStatus,
  HrApprovalStatus,
  HrContractType,
  HrContractStatus,
  HrLeaveType,
  HrScheduleType,
  HrCheckinType,
  HrCheckinStatus,
  HrCheckinSource,
  HrSalaryStatus,
  HrSalaryChangeType,
  HrSalaryChangeStatus,
  HrPeriodType,
  HrPerformanceReviewStatus,
  HrPayrollStatus,
  HrSalarySlipReadStatus,
  AssetCategory,
  AssetStatus,
  AssetBorrowStatus,
  MaterialStatus,
  MaterialApplyStatus,
  VehicleStatus,
  VehicleApplyStatus,
  VehicleExpenseType,
  MeetingApplyStatus,
  VisitorStatus,
  RepairCategory,
  RepairStatus,
  LibraryType,
  DocFileStatus,
  DocVersionSource,
  DocSubjectType,
  DocAction
} from '@ceams/types'

/** 枚举值 -> 中文标签映射（与三域类型字段一致）。 */

export const userStatusLabels: Record<UserStatus, string> = {
  ACTIVE: '启用',
  DISABLED: '禁用',
  LOCKED: '锁定'
}

export const roleStatusLabels: Record<RoleStatus, string> = {
  ACTIVE: '启用',
  DISABLED: '禁用'
}

export const permissionTypeLabels: Record<PermissionType, string> = {
  MENU: '菜单',
  BUTTON: '按钮',
  API: '接口'
}

export const platformLabels: Record<Platform, string> = {
  WEB: 'Web',
  DESKTOP: '桌面端',
  MINIPROGRAM: '小程序'
}

export const deviceStatusLabels: Record<DeviceStatus, string> = {
  ACTIVE: '在线',
  UNBOUND: '已解绑'
}

export const operationTypeLabels: Record<OperationType, string> = {
  LOGIN: '登录',
  LOGOUT: '登出',
  CREATE: '新增',
  UPDATE: '修改',
  DELETE: '删除',
  APPROVE: '审批',
  EXPORT: '导出',
  IMPORT: '导入',
  OTHER: '其他'
}

export const categoryTypeLabels: Record<CategoryType, string> = {
  EXPENSE: '费用',
  MATERIAL: '材料',
  PROJECT: '项目',
  CONTACT: '往来单位',
  PAYMENT: '支付',
  OTHER: '其他'
}

export const contactTypeLabels: Record<ContactType, string> = {
  SUPPLIER: '供应商',
  CUSTOMER: '客户',
  SUBCONTRACTOR: '分包商',
  OTHER: '其他'
}

export const equipmentStatusLabels: Record<EquipmentStatus, string> = {
  IDLE: '闲置',
  IN_USE: '使用中',
  LEASED: '已出租',
  MAINTENANCE: '维修中',
  SCRAPPED: '已报废'
}

export const paymentStatusLabels: Record<PaymentStatus, string> = {
  UNPAID: '未付',
  PARTIAL: '部分支付',
  PAID: '已付清'
}

export const projectStatusLabels: Record<ProjectStatus, string> = {
  PLANNING: '规划中',
  IN_PROGRESS: '进行中',
  PAUSED: '已暂停',
  COMPLETED: '已完成',
  CLOSED: '已关闭'
}

export const budgetStatusLabels: Record<BudgetStatus, string> = {
  DRAFT: '草稿',
  ACTIVE: '启用',
  CLOSED: '已关闭'
}

export const approvalStatusLabels: Record<ApprovalStatus, string> = {
  DRAFT: '草稿',
  PENDING: '审批中',
  APPROVED: '已通过',
  REJECTED: '已驳回',
  CANCELLED: '已终止'
}

export const contractStatusLabels: Record<ContractStatus, string> = {
  DRAFT: '草稿',
  ACTIVE: '履行中',
  COMPLETED: '已完成',
  TERMINATED: '已终止'
}

// ---------- 报销域（expense） ----------

export const receiptTypeLabels: Record<ReceiptType, string> = {
  TRANSPORTATION: '交通',
  ACCOMMODATION: '住宿',
  MEAL: '餐饮',
  OFFICE: '办公',
  OTHER: '其他'
}

export const verificationStatusLabels: Record<VerificationStatus, string> = {
  PENDING: '待核验',
  VERIFIED: '已核验',
  REJECTED: '已驳回'
}

export const paymentMethodLabels: Record<PaymentMethod, string> = {
  CASH: '现金',
  BANK_TRANSFER: '银行转账',
  WECHAT: '微信',
  ALIPAY: '支付宝',
  OTHER: '其他'
}

export const receiptApprovalStatusLabels: Record<ReceiptApprovalStatus, string> = {
  DRAFT: '草稿',
  PENDING: '审批中',
  APPROVED: '已通过',
  REJECTED: '已驳回',
  CANCELLED: '已终止'
}

export const approvalRecordStatusLabels: Record<ApprovalRecordStatus, string> = {
  PENDING: '待审批',
  APPROVED: '已通过',
  REJECTED: '已驳回'
}

// ---------- 资金域（payment） ----------

export const accountTypeLabels: Record<AccountType, string> = {
  INCOME: '收入',
  EXPENSE: '支出'
}

export const accountStatusLabels: Record<AccountStatus, string> = {
  DRAFT: '草稿',
  CONFIRMED: '已确认',
  CANCELLED: '已取消'
}

export const allocationStatusLabels: Record<AllocationStatus, string> = {
  UNALLOCATED: '未分摊',
  PARTIAL: '部分分摊',
  ALLOCATED: '已分摊'
}

export const targetTypeLabels: Record<TargetType, string> = {
  PROJECT: '项目',
  DEPT: '部门',
  SERVICE: '服务',
  CONTRACT: '合同'
}

// ---------- 对账域（bank） ----------

export const transactionTypeLabels: Record<TransactionType, string> = {
  INCOME: '收入',
  EXPENSE: '支出'
}

export const bankStatementStatusLabels: Record<BankStatementStatus, string> = {
  UNRECONCILED: '未对账',
  RECONCILED: '已对账',
  MATCHED: '已匹配'
}

export const reconciliationStatusLabels: Record<ReconciliationStatus, string> = {
  MATCHED: '已匹配',
  UNMATCHED: '未匹配',
  PARTIAL_MATCHED: '部分匹配'
}

export const discrepancyTypeLabels: Record<DiscrepancyType, string> = {
  MISSING_IN_SYSTEM: '系统缺失',
  MISSING_IN_BANK: '银行缺失',
  AMOUNT_MISMATCH: '金额不符',
  DATE_MISMATCH: '日期不符',
  DESCRIPTION_MISMATCH: '描述不符'
}

export const resolutionStatusLabels: Record<ResolutionStatus, string> = {
  UNRESOLVED: '未解决',
  RESOLVED: '已解决'
}

// ---------- 印章域（seal） ----------

export const sealStatusLabels: Record<SealStatus, string> = {
  ACTIVE: '启用',
  DISABLED: '停用',
  ARCHIVED: '归档'
}

export const storageTypeLabels: Record<StorageType, string> = {
  FILE: '文件',
  USB_DRIVE: 'U 盾',
  CLOUD: '云端'
}

export const applicationStatusLabels: Record<ApplicationStatus, string> = {
  PENDING: '待审批',
  APPROVED: '已通过',
  REJECTED: '已驳回',
  CANCELLED: '已取消'
}

export const sealApprovalStatusLabels: Record<SealApprovalStatus, string> = {
  PENDING: '待审批',
  APPROVED: '已通过',
  REJECTED: '已驳回'
}

// ---------- 报表域（report） ----------

export const financialSourceTypeLabels: Record<FinancialSourceType, string> = {
  ACCOUNT: '收支流水',
  PAYMENT_TRANSACTION: '付款交易',
  BANK_STATEMENT: '银行流水',
  GENERAL_LEDGER: '总账'
}

// ---------- 通知域（notification） ----------

export const pushChannelLabels: Record<PushChannel, string> = {
  SYSTEM: '系统',
  EMAIL: '邮件',
  SMS: '短信'
}

// ---------- 人事域（hr） ----------

export const hrGenderLabels: Record<HrGender, string> = {
  MALE: '男',
  FEMALE: '女',
  UNKNOWN: '未知'
}

export const hrEmploymentTypeLabels: Record<HrEmploymentType, string> = {
  FULL_TIME: '全职',
  PART_TIME: '兼职',
  INTERN: '实习',
  CONTRACTOR: '劳务/外包'
}

export const hrEmployeeStatusLabels: Record<HrEmployeeStatus, string> = {
  PROBATION: '试用期',
  ACTIVE: '在职',
  LEAVE: '休假',
  TERMINATED: '离职'
}

export const hrChangeTypeLabels: Record<HrChangeType, string> = {
  REGULAR: '转正',
  TRANSFER: '调岗',
  PROMOTION: '晋升',
  RESIGN: '离职',
  LEADER_TRANSFER: '上级转交'
}

export const hrJobLevelStatusLabels: Record<HrJobLevelStatus, string> = {
  ENABLED: '启用',
  DISABLED: '停用'
}

export const hrApprovalStatusLabels: Record<HrApprovalStatus, string> = {
  PENDING: '审批中',
  APPROVED: '已通过',
  REJECTED: '已驳回',
  WITHDRAWN: '已撤回',
  CANCELLED: '已终止',
  TERMINATED: '已终止'
}

export const hrContractTypeLabels: Record<HrContractType, string> = {
  FIXED: '固定期限',
  UNFIXED: '无固定期限',
  INTERN: '实习协议'
}

export const hrContractStatusLabels: Record<HrContractStatus, string> = {
  ACTIVE: '生效中',
  EXPIRED: '已到期',
  TERMINATED: '已终止'
}

export const hrLeaveTypeLabels: Record<HrLeaveType, string> = {
  ANNUAL: '年假',
  SICK: '病假',
  PERSONAL: '事假'
}

export const hrScheduleTypeLabels: Record<HrScheduleType, string> = {
  FIXED: '固定排班',
  TEMP: '临时排班'
}

export const hrCheckinTypeLabels: Record<HrCheckinType, string> = {
  IN: '上班',
  OUT: '下班'
}

export const hrCheckinStatusLabels: Record<HrCheckinStatus, string> = {
  NORMAL: '正常',
  LATE: '迟到',
  EARLY: '早退',
  ABSENT: '缺卡'
}

export const hrCheckinSourceLabels: Record<HrCheckinSource, string> = {
  MINI: '微信小程序',
  DEVICE: '考勤设备',
  MANUAL: '手工补录'
}

// ---------- 人事域（hr）薪资 / 绩效 / 核算（M2） ----------

export const hrSalaryStatusLabels: Record<HrSalaryStatus, string> = {
  ACTIVE: '当前生效',
  HISTORY: '历史记录'
}

export const hrSalaryChangeTypeLabels: Record<HrSalaryChangeType, string> = {
  ADJUST: '薪资调整',
  PROMOTION: '晋升调薪',
  CORRECTION: '数据更正'
}

export const hrSalaryChangeStatusLabels: Record<HrSalaryChangeStatus, string> = {
  DRAFT: '草稿',
  APPROVING: '审批中',
  APPROVED: '已通过',
  REJECTED: '已驳回'
}

export const hrPeriodTypeLabels: Record<HrPeriodType, string> = {
  MONTH: '月度考核',
  QUARTER: '季度考核'
}

export const hrPerformanceReviewStatusLabels: Record<HrPerformanceReviewStatus, string> = {
  DRAFT: '待自评',
  SELF_SUBMITTED: '自评已提交',
  LEADER_DONE: '上级已评分',
  APPROVED: '已通过',
  REJECTED: '已驳回'
}

export const hrPayrollStatusLabels: Record<HrPayrollStatus, string> = {
  DRAFT: '草稿',
  CONFIRMED: '已确认',
  LOCKED: '已锁定'
}

export const hrSalarySlipReadStatusLabels: Record<HrSalarySlipReadStatus, string> = {
  PENDING: '未读',
  READ: '已读'
}

/** 绩效模板状态（后端为字符串枚举 ENABLED / DISABLED）。 */
export const hrTemplateStatusLabels: Record<string, string> = {
  ENABLED: '启用',
  DISABLED: '停用'
}

// ---------- 行政后勤域（admin） ----------

export const assetCategoryLabels: Record<AssetCategory, string> = {
  ELECTRONIC: '电子设备',
  FURNITURE: '办公家具',
  VEHICLE_TOOL: '车辆工具',
  OTHER: '其他'
}

export const assetStatusLabels: Record<AssetStatus, string> = {
  IN_STOCK: '在库',
  BORROWED: '已借出',
  REPAIRING: '维修中',
  SCRAPPED: '已报废'
}

export const assetBorrowStatusLabels: Record<AssetBorrowStatus, string> = {
  APPROVING: '审批中',
  BORROWING: '借出中',
  RETURNED: '已归还',
  REJECTED: '已驳回'
}

export const materialStatusLabels: Record<MaterialStatus, string> = {
  ENABLED: '启用',
  DISABLED: '停用'
}

export const materialApplyStatusLabels: Record<MaterialApplyStatus, string> = {
  DRAFT: '草稿',
  APPROVING: '审批中',
  APPROVED: '已通过',
  REJECTED: '已驳回',
  ISSUED: '已发放'
}

export const vehicleStatusLabels: Record<VehicleStatus, string> = {
  IDLE: '空闲',
  USING: '使用中',
  MAINTENANCE: '维修保养'
}

export const vehicleApplyStatusLabels: Record<VehicleApplyStatus, string> = {
  APPROVING: '审批中',
  APPROVED: '已通过',
  REJECTED: '已驳回',
  USING: '使用中',
  RETURNED: '已还车',
  CANCELLED: '已取消'
}

export const vehicleExpenseTypeLabels: Record<VehicleExpenseType, string> = {
  MAINTENANCE: '维修保养',
  VIOLATION: '违章罚款',
  FUEL: '加油费用'
}

/** 会议室状态（后端为字符串枚举 ENABLED / DISABLED）。 */
export const meetingRoomStatusLabels: Record<string, string> = {
  ENABLED: '启用',
  DISABLED: '停用'
}

export const meetingApplyStatusLabels: Record<MeetingApplyStatus, string> = {
  APPROVING: '审批中',
  APPROVED: '已通过',
  REJECTED: '已驳回',
  CANCELLED: '已取消'
}

export const visitorStatusLabels: Record<VisitorStatus, string> = {
  PENDING: '待核销',
  CHECKED_IN: '已入场',
  CHECKED_OUT: '已离场',
  CANCELLED: '已取消'
}

export const repairCategoryLabels: Record<RepairCategory, string> = {
  ELECTRIC: '电气维修',
  WATER: '给排水维修',
  FURNITURE: '家具维修',
  IT: '信息技术',
  OTHER: '其他'
}

export const repairStatusLabels: Record<RepairStatus, string> = {
  SUBMITTED: '已提交',
  ASSIGNED: '已派单',
  PROCESSING: '处理中',
  DONE: '已完工',
  ACCEPTED: '已验收',
  REJECTED: '已驳回'
}

/** 将枚举标签映射转换为 el-select 可用的 options 数组。 */
export function toOptions<T extends string>(labels: Record<T, string>): { value: T; label: string }[] {
  return (Object.keys(labels) as T[]).map((value) => ({ value, label: labels[value] }))
}

// ---------- 流程域（bpm） ----------

export const bpmBizTypeLabels: Record<string, string> = {
  EXPENSE: '报销',
  CHANGE_REQUEST: '项目变更',
  HR_REGULAR: '转正',
  HR_TRANSFER: '调岗',
  HR_PROMOTION: '晋升',
  HR_RESIGN: '离职',
  HR_LEAVE: '请假',
  HR_CHECKIN_SUPPLEMENT: '补卡',
  HR_SALARY_CHANGE: '薪资变动',
  HR_SALARY_CORRECTION: '薪资更正',
  HR_PERFORMANCE: '绩效考评',
  ADMIN_ASSET_BORROW: '资产领用',
  ADMIN_MATERIAL_APPLY: '易耗品申领',
  ADMIN_VEHICLE_APPLY: '用车申请',
  ADMIN_MEETING_APPLY: '会议室预定',
  ADMIN_REPAIR: '报修',
  HR_CONTRACT: '劳动合同',
  SUBCONTRACT_CONTRACT: '分包合同'
}

export const bpmInstanceStatusLabels: Record<string, string> = {
  DRAFT: '草稿',
  RUNNING: '审批中',
  APPROVED: '已通过',
  REJECTED: '已驳回',
  WITHDRAWN: '已撤回',
  TERMINATED: '已终止'
}

export const bpmTaskStatusLabels: Record<string, string> = {
  PENDING: '待处理',
  PENDING_ASSIGNEE: '待分配审批人',
  APPROVED: '已同意',
  REJECTED: '已驳回',
  CLOSED: '已关闭'
}

export const bpmNodeStatusLabels: Record<string, string> = {
  PENDING: '未到达',
  ACTIVE: '处理中',
  COMPLETED: '已完成',
  REJECTED: '已驳回',
  CLOSED: '已关闭'
}

export const bpmNodeTypeLabels: Record<string, string> = {
  START: '开始',
  SINGLE: '单人审批',
  COUNTERSIGN: '会签',
  OR_SIGN: '或签',
  CC: '抄送',
  CONDITION: '条件',
  AUTO: '自动',
  END: '结束'
}

export const bpmTemplateStatusLabels: Record<string, string> = {
  ENABLED: '启用',
  DISABLED: '停用'
}

export const bpmTemplateVersionStatusLabels: Record<string, string> = {
  DRAFT: '草稿',
  PUBLISHED: '已发布',
  ARCHIVED: '已归档'
}

export const bpmAssigneeSourceLabels: Record<string, string> = {
  USER: '指定用户',
  ROLE: '指定角色',
  DEPT_LEADER: '部门主管',
  SUPERIOR: '直属上级',
  INITIATOR: '发起人',
  DELEGATE: '指定代理人',
  PROXY: '代理'
}

export const bpmConditionOperatorLabels: Record<string, string> = {
  '>=': '大于等于',
  '>': '大于',
  '<=': '小于等于',
  '<': '小于',
  '==': '等于',
  '!=': '不等于'
}

// ---------- 文档知识库域（doc） ----------

export const libraryTypeLabels: Record<LibraryType, string> = {
  PERSONAL: '个人库',
  DEPT: '部门库',
  PROJECT: '项目库',
  PUBLIC: '公开库'
}

export const docFileStatusLabels: Record<DocFileStatus, string> = {
  NORMAL: '正常',
  LOCKED: '编辑中'
}

export const docVersionSourceLabels: Record<DocVersionSource, string> = {
  UPLOAD: '新上传',
  ONLINE_EDIT: '在线编辑',
  ROLLBACK: '版本回滚'
}

export const docSubjectTypeLabels: Record<DocSubjectType, string> = {
  USER: '用户',
  ROLE: '角色',
  DEPT: '部门'
}

export const docActionLabels: Record<DocAction, string> = {
  PREVIEW: '预览',
  DOWNLOAD: '下载',
  EDIT: '编辑',
  DELETE: '删除'
}

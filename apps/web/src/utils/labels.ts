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
  FinancialSourceType
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
  PENDING: '待审批',
  APPROVED: '已通过',
  REJECTED: '已驳回'
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
  PENDING: '待审批',
  APPROVED: '已通过',
  REJECTED: '已驳回'
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

/** 将枚举标签映射转换为 el-select 可用的 options 数组。 */
export function toOptions<T extends string>(labels: Record<T, string>): { value: T; label: string }[] {
  return (Object.keys(labels) as T[]).map((value) => ({ value, label: labels[value] }))
}

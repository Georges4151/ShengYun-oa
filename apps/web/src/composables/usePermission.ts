import { useAuthStore } from '@/stores/auth'
import { pinia } from '@/stores'

/**
 * 按钮级权限判定。
 *
 * 后端 JWT 仅携带 ROLE_*（见 HrSalaryController 等类的注释），因此前端按
 * V30__hr_salary_permission_seed.sql 中「权限码 → 授予角色」的映射做等价判断：
 * - 164 hr:salary:view            → SYS_ADMIN、FINANCE
 * - 165 hr:salary:change          → SYS_ADMIN
 * - 166 hr:salary:confirm         → SYS_ADMIN
 * - 167 hr:performance:template   → SYS_ADMIN
 * - 168 hr:performance:operate    → SYS_ADMIN
 * - 169 hr:salary-slip:export     → SYS_ADMIN、FINANCE（导出接口同时放行 FINANCE）
 *
 * 行政后勤（V31 权限种子，170 为菜单码，实际按钮/页签使用 171～177）：
 * - 171 admin:asset:view          → SYS_ADMIN
 * - 172 admin:asset:manage        → SYS_ADMIN
 * - 173 admin:material:manage     → SYS_ADMIN
 * - 174 admin:vehicle:manage      → SYS_ADMIN
 * - 175 admin:meeting:manage      → SYS_ADMIN
 * - 176 admin:visitor:manage      → SYS_ADMIN
 * - 177 admin:repair:manage       → SYS_ADMIN
 *
 * 文档知识库（V39 权限种子 178～183）：
 * - 178 doc                      → SYS_ADMIN、STAFF、FINANCE（菜单码，路由 meta.permission）
 * - 179 doc:dir:manage           → SYS_ADMIN
 * - 180 doc:file:upload          → SYS_ADMIN、STAFF、FINANCE
 * - 181 doc:file:edit            → SYS_ADMIN、STAFF
 * - 182 doc:file:delete          → SYS_ADMIN
 * - 183 doc:permission:manage    → SYS_ADMIN
 *
 * 企业微信集成（V41 权限种子 184～185）：
 * - 184 integration:wecom:view   → SYS_ADMIN（菜单码，路由 meta.permission）
 * - 185 integration:wecom:sync   → SYS_ADMIN（手动同步 / 消息重投）
 *
 * 系统设置 · AI 设置：
 * - system:ai:config              → SYS_ADMIN（菜单码，路由 meta.permission）
 *
 * M6 报表（V42 权限种子 186～189）：
 * - 186 report                    → 菜单码（本页不登记，由路由 meta 控制）
 * - 187 report:hr-cost:view       → SYS_ADMIN
 * - 188 report:hr-performance:view → SYS_ADMIN
 * - 189 report:admin-assets:view  → SYS_ADMIN
 *
 * 员工上下级（V52 权限种子 196）：
 * - hr:employee:leader            → SYS_ADMIN（控制员工档案「直属上级」的指定与调整）
 *
 * 报表页签（历史权限码，见 BPM 报表）：
 * - bpm:report:view               → SYS_ADMIN、FINANCE
 *
 * 流程模板（V46 权限种子 190～191）：
 * - 190 bpm:template:create       → SYS_ADMIN
 * - 191 bpm:template:delete       → SYS_ADMIN
 *
 * 报销额度分级规则：
 * - expense:amount-rule:manage    → SYS_ADMIN（流程模板页签的规则维护入口）
 */
const PERMISSION_ROLES: Record<string, string[]> = {
  'hr:salary:view': ['SYS_ADMIN', 'FINANCE'],
  'hr:employee:leader': ['SYS_ADMIN'],
  'hr:salary:change': ['SYS_ADMIN'],
  'hr:salary:confirm': ['SYS_ADMIN'],
  'hr:performance:template': ['SYS_ADMIN'],
  'hr:performance:operate': ['SYS_ADMIN'],
  'hr:salary-slip:export': ['SYS_ADMIN', 'FINANCE'],
  'admin:asset:view': ['SYS_ADMIN'],
  'admin:asset:manage': ['SYS_ADMIN'],
  'admin:material:manage': ['SYS_ADMIN'],
  'admin:vehicle:manage': ['SYS_ADMIN'],
  'admin:meeting:manage': ['SYS_ADMIN'],
  'admin:visitor:manage': ['SYS_ADMIN'],
  'admin:repair:manage': ['SYS_ADMIN'],
  doc: ['SYS_ADMIN', 'STAFF', 'FINANCE'],
  'doc:dir:manage': ['SYS_ADMIN'],
  'doc:file:upload': ['SYS_ADMIN', 'STAFF', 'FINANCE'],
  'doc:file:edit': ['SYS_ADMIN', 'STAFF'],
  'doc:file:delete': ['SYS_ADMIN'],
  'doc:permission:manage': ['SYS_ADMIN'],
  'integration:wecom:view': ['SYS_ADMIN'],
  'integration:wecom:sync': ['SYS_ADMIN'],
  'system:ai:config': ['SYS_ADMIN'],
  'report:hr-cost:view': ['SYS_ADMIN'],
  'report:hr-performance:view': ['SYS_ADMIN'],
  'report:admin-assets:view': ['SYS_ADMIN'],
  'bpm:report:view': ['SYS_ADMIN', 'FINANCE'],
  'bpm:template:create': ['SYS_ADMIN'],
  'bpm:template:delete': ['SYS_ADMIN'],
  'expense:amount-rule:manage': ['SYS_ADMIN']
}

/** 当前登录用户是否具备指定权限码（优先使用后端下发的权限码，未加载时回退角色映射）。 */
export function hasPermission(code: string): boolean {
  const auth = useAuthStore(pinia)
  if (auth.permissionsLoaded) {
    return auth.permissions.includes(code)
  }
  const allowed = PERMISSION_ROLES[code]
  if (!allowed) return false
  return auth.roles.some((role) => allowed.includes(role.code))
}

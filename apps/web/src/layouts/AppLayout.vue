<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElAvatar, ElDropdown, ElDropdownItem, ElDropdownMenu, ElMenu, ElMenuItem } from 'element-plus'
import { AppShell } from '@ceams/ui-web'
import AiAssistant from '@/components/AiAssistant.vue'
import AppIcon from '@/components/AppIcon.vue'
import { hasPermission } from '@/composables/usePermission'
import { isModuleHidden } from '@/config/features'
import { useAuthStore } from '@/stores/auth'

defineOptions({ name: 'AppLayout' })

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()

interface MenuItem {
  /** 路由路径，同时也是 el-menu 的 index */
  path: string
  title: string
  /** AppIcon 图标名 */
  icon: string
  /** 后端菜单权限码；空串表示登录即可访问（如工作台） */
  permission: string
}

interface MenuGroup {
  title: string
  items: MenuItem[]
}

/**
 * 按业务域分组的主菜单（与 routes.ts 一一对应）。
 * 分组顺序：核心 → 业务 → 财务 → 系统；工作台固定置顶在「核心」组。
 */
const MENU_GROUPS: MenuGroup[] = [
  {
    title: '核心',
    items: [
      { path: '/dashboard', title: '工作台', icon: 'dashboard', permission: '' },
      { path: '/todo', title: '待办中心', icon: 'todo', permission: 'bpm' }
    ]
  },
  {
    title: '业务',
    items: [
      { path: '/masterdata', title: '主数据', icon: 'masterdata', permission: 'masterdata' },
      { path: '/project', title: '项目合同', icon: 'project', permission: 'project' },
      { path: '/hr', title: '人事', icon: 'hr', permission: 'hr' },
      { path: '/admin', title: '行政后勤', icon: 'admin', permission: 'admin' },
      { path: '/doc', title: '文档知识库', icon: 'doc', permission: 'doc' },
      { path: '/integration/wecom', title: '企业微信集成', icon: 'wecom', permission: 'integration:wecom:view' }
    ]
  },
  {
    title: '财务',
    items: [
      { path: '/expense', title: '报销', icon: 'expense', permission: 'expense' },
      { path: '/payment', title: '资金', icon: 'payment', permission: 'payment' },
      { path: '/bank', title: '对账', icon: 'bank', permission: 'bank' },
      { path: '/seal', title: '印章', icon: 'seal', permission: 'seal' },
      { path: '/report', title: '报表', icon: 'report', permission: 'report' }
    ]
  },
  {
    title: '系统',
    items: [
      { path: '/notification', title: '通知', icon: 'notification', permission: 'notification' },
      { path: '/account', title: '账号', icon: 'account', permission: 'iam' }
    ]
  }
]

/** 单个菜单项是否可见：无权限或已隐藏的模块不渲染入口。 */
function canAccess(item: MenuItem): boolean {
  if (isModuleHidden(item.path)) return false
  return item.permission === '' || hasPermission(item.permission)
}

/** 过滤后的菜单分组：组内无可见项时整组不渲染（组标题也一并隐藏）。 */
const menuGroups = computed<MenuGroup[]>(() =>
  MENU_GROUPS.map((group) => ({ title: group.title, items: group.items.filter(canAccess) })).filter(
    (group) => group.items.length > 0
  )
)

const activeMenu = computed(() => route.path)

/** 顶栏页面标题：取当前路由 meta.title。 */
const pageTitle = computed(() => (typeof route.meta.title === 'string' ? route.meta.title : 'CEAMS'))

/** 展示姓名与头像兜底（沿用 auth.user?.avatar 与姓名首字）。 */
const displayName = computed(() => auth.name || auth.username || '管理员')
const avatarText = computed(() => (auth.name || auth.username || '管').slice(0, 1))

function onSelect(path: string): void {
  if (path !== route.path) void router.push(path)
}

function onCommand(command: unknown): void {
  if (command === 'logout') {
    void auth.logout()
    void router.push('/login')
  }
}
</script>

<template>
  <AppShell title="CEAMS 建筑企业账目管理系统">
    <template #aside>
      <el-menu :default-active="activeMenu" @select="onSelect">
        <template v-for="group in menuGroups" :key="group.title">
          <div class="menu-group-title">{{ group.title }}</div>
          <el-menu-item v-for="m in group.items" :key="m.path" :index="m.path">
            <AppIcon class="menu-item-icon" :name="m.icon" :size="18" />
            <span class="menu-item-text">{{ m.title }}</span>
          </el-menu-item>
        </template>
      </el-menu>
    </template>

    <template #toolbar>
      <div class="topbar">
        <span class="topbar-title">{{ pageTitle }}</span>
        <el-dropdown trigger="click" @command="onCommand">
          <span class="user-trigger">
            <el-avatar :size="30" :src="auth.user?.avatar || undefined">{{ avatarText }}</el-avatar>
            <span class="user-name">{{ displayName }}</span>
          </span>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="logout">退出登录</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>
    </template>

    <RouterView />

    <!-- AI 助手抽屉：仅登录后的主布局挂载，登录页不出现 -->
    <AiAssistant />
  </AppShell>
</template>

<style scoped>
/* 让顶栏工具区撑满剩余宽度，实现「页面标题居左、用户信息居右」 */
:deep(.ceams-shell__toolbar) {
  flex: 1;
  min-width: 0;
  margin-left: var(--ceams-space-6);
}

.topbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;
  gap: var(--ceams-space-4);
}

.topbar-title {
  overflow: hidden;
  font-size: var(--ceams-font-size-md);
  font-weight: 600;
  color: var(--ceams-text-primary);
  text-overflow: ellipsis;
  white-space: nowrap;
}

.user-trigger {
  display: flex;
  align-items: center;
  gap: var(--ceams-space-2);
  padding: 4px 6px;
  border-radius: var(--ceams-radius-md);
  cursor: pointer;
  transition: background-color 0.16s ease;
}

.user-trigger:hover {
  background-color: var(--ceams-bg-elevated);
}

.user-name {
  font-size: var(--ceams-font-size-base);
  color: var(--ceams-text-primary);
}

/* 分组标题：小号半透明文字，弱于菜单项 */
.menu-group-title {
  padding: 14px 24px 6px;
  font-size: var(--ceams-font-size-xs);
  line-height: 1.2;
  color: rgba(255, 255, 255, 0.38);
  letter-spacing: 0.08em;
}

.menu-item-icon {
  margin-right: 10px;
  opacity: 0.9;
}
</style>

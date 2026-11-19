<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElAvatar, ElDropdown, ElDropdownItem, ElDropdownMenu, ElMenu, ElMenuItem } from 'element-plus'
import { AppShell } from '@ceams/ui-web'
import AiAssistant from '@/components/AiAssistant.vue'
import { hasPermission } from '@/composables/usePermission'
import { useAuthStore } from '@/stores/auth'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()

// 页面地图菜单（与 routes.ts 一一对应；permission 为后端菜单权限码）
const MENUS = [
  { path: '/todo', title: '待办中心', permission: 'bpm' },
  { path: '/masterdata', title: '主数据', permission: 'masterdata' },
  { path: '/project', title: '项目合同', permission: 'project' },
  { path: '/hr', title: '人事', permission: 'hr' },
  { path: '/admin', title: '行政后勤', permission: 'admin' },
  { path: '/doc', title: '文档知识库', permission: 'doc' },
  { path: '/integration/wecom', title: '企业微信集成', permission: 'integration:wecom:view' },
  { path: '/expense', title: '报销', permission: 'expense' },
  { path: '/payment', title: '资金', permission: 'payment' },
  { path: '/bank', title: '对账', permission: 'bank' },
  { path: '/seal', title: '印章', permission: 'seal' },
  { path: '/notification', title: '通知', permission: 'notification' },
  { path: '/report', title: '报表', permission: 'report' },
  { path: '/account', title: '账号', permission: 'iam' }
]

/** 按当前账号权限码过滤菜单，无权限模块不渲染入口。 */
const menus = computed(() => MENUS.filter((menu) => hasPermission(menu.permission)))

const activeMenu = computed(() => route.path)

function onSelect(path: string): void {
  void router.push(path)
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
        <el-menu-item v-for="m in menus" :key="m.path" :index="m.path">
          {{ m.title }}
        </el-menu-item>
      </el-menu>
    </template>

    <template #toolbar>
      <el-dropdown @command="onCommand">
        <span class="user-trigger">
          <el-avatar :size="28" :src="auth.user?.avatar || undefined">{{ (auth.name || auth.username || '管').slice(0, 1) }}</el-avatar>
          <span class="user-name">{{ auth.name || auth.username || '管理员' }}</span>
        </span>
        <template #dropdown>
          <el-dropdown-menu>
            <el-dropdown-item command="logout">退出登录</el-dropdown-item>
          </el-dropdown-menu>
        </template>
      </el-dropdown>
    </template>

    <RouterView />

    <!-- AI 助手抽屉：仅登录后的主布局挂载，登录页不出现 -->
    <AiAssistant />
  </AppShell>
</template>

<style scoped>
.user-trigger {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
}
.user-name {
  font-size: 14px;
  color: #374151;
}
</style>
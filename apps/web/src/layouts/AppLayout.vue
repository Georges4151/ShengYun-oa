<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElAvatar, ElDropdown, ElDropdownItem, ElDropdownMenu, ElMenu, ElMenuItem } from 'element-plus'
import { AppShell } from '@ceams/ui-web'
import AiAssistant from '@/components/AiAssistant.vue'
import { useAuthStore } from '@/stores/auth'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()

// 页面地图菜单（与 routes.ts 一一对应）
const menus = [
  { path: '/masterdata', title: '主数据' },
  { path: '/project', title: '项目合同' },
  { path: '/expense', title: '报销' },
  { path: '/payment', title: '资金' },
  { path: '/bank', title: '对账' },
  { path: '/seal', title: '印章' },
  { path: '/notification', title: '通知' },
  { path: '/report', title: '报表' },
  { path: '/account', title: '账号' }
]

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
          <el-avatar :size="28">{{ (auth.name || auth.username || '管').slice(0, 1) }}</el-avatar>
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
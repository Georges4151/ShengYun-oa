<template>
  <div class="header">
    <div class="left">
      <el-icon 
        class="collapse-btn" 
        @click="toggleSidebar"
      >
        <Fold v-if="!collapsed" />
        <Expand v-else />
      </el-icon>
    </div>
    <div class="right">
      <el-badge :value="unreadCount" :hidden="!unreadCount" class="notification-badge">
        <el-icon class="icon-btn" @click="goNotification">
          <Bell />
        </el-icon>
      </el-badge>
      <el-dropdown @command="handleCommand">
        <span class="user-info">
          <el-avatar :size="32" class="avatar">
            {{ username?.charAt(0)?.toUpperCase() || 'U' }}
          </el-avatar>
          <span class="username">{{ username }}</span>
          <el-icon><ArrowDown /></el-icon>
        </span>
        <template #dropdown>
          <el-dropdown-menu>
            <el-dropdown-item command="profile">
              <el-icon><User /></el-icon>个人中心
            </el-dropdown-item>
            <el-dropdown-item command="settings">
              <el-icon><Setting /></el-icon>系统设置
            </el-dropdown-item>
            <el-dropdown-item divided command="logout">
              <el-icon><SwitchButton /></el-icon>退出登录
            </el-dropdown-item>
          </el-dropdown-menu>
        </template>
      </el-dropdown>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessageBox } from 'element-plus'
import { useAppStore } from '@/stores/app'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const appStore = useAppStore()
const userStore = useUserStore()

const collapsed = computed(() => appStore.sidebarCollapsed)
const username = computed(() => userStore.username)
const unreadCount = computed(() => 0)

function toggleSidebar() {
  appStore.toggleSidebar()
}

function goNotification() {
  router.push('/notification/center')
}

async function handleCommand(command) {
  switch (command) {
    case 'profile':
      router.push('/user/profile')
      break
    case 'settings':
      router.push('/system/settings')
      break
    case 'logout':
      try {
        await ElMessageBox.confirm('确定要退出登录吗？', '提示', {
          confirmButtonText: '确定',
          cancelButtonText: '取消',
          type: 'warning'
        })
        userStore.logout()
      } catch {
        // 取消退出
      }
      break
  }
}
</script>

<style scoped>
.header {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 20px;
}

.left {
  display: flex;
  align-items: center;
}

.collapse-btn {
  font-size: 20px;
  cursor: pointer;
  color: var(--text-primary);
}

.collapse-btn:hover {
  color: var(--primary-color);
}

.right {
  display: flex;
  align-items: center;
  gap: 20px;
}

.notification-badge {
  cursor: pointer;
}

.icon-btn {
  font-size: 20px;
  cursor: pointer;
  color: var(--text-regular);
}

.icon-btn:hover {
  color: var(--primary-color);
}

.user-info {
  display: flex;
  align-items: center;
  cursor: pointer;
  gap: 8px;
}

.avatar {
  background-color: var(--primary-color);
  color: #fff;
}

.username {
  color: var(--text-primary);
  font-size: 14px;
}
</style>

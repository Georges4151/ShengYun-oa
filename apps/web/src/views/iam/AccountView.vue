<script setup lang="ts">
import { computed, ref } from 'vue'
import { ElTabPane, ElTabs } from 'element-plus'
import { PageContainer } from '@ceams/ui-web'
import { useAuthStore } from '@/stores/auth'
import ProfilePanel from './components/ProfilePanel.vue'
import UserList from './components/UserList.vue'
import RoleList from './components/RoleList.vue'
import PermissionList from './components/PermissionList.vue'
import DeviceList from './components/DeviceList.vue'
import OperationLogList from './components/OperationLogList.vue'
import StorageInfo from './components/StorageInfo.vue'

defineOptions({ name: 'AccountView' })

const auth = useAuthStore()

/** 「存储」tab 仅对系统管理员开放 */
const isSysAdmin = computed(() => auth.roles.some((role) => role.code === 'SYS_ADMIN'))

// 默认进入「个人资料」，便于用户直接维护头像与查看本人信息
const activeTab = ref('profile')
</script>

<template>
  <PageContainer title="账号中心" subtitle="个人资料、用户、角色、权限、设备与操作日志的统一管理">
    <el-tabs v-model="activeTab" class="account-view">
      <el-tab-pane label="个人资料" name="profile">
        <ProfilePanel />
      </el-tab-pane>
      <el-tab-pane label="用户" name="user">
        <UserList />
      </el-tab-pane>
      <el-tab-pane label="角色" name="role">
        <RoleList />
      </el-tab-pane>
      <el-tab-pane label="权限" name="permission">
        <PermissionList />
      </el-tab-pane>
      <el-tab-pane label="设备" name="device">
        <DeviceList />
      </el-tab-pane>
      <el-tab-pane label="操作日志" name="log">
        <OperationLogList />
      </el-tab-pane>
      <el-tab-pane v-if="isSysAdmin" label="存储" name="storage">
        <StorageInfo />
      </el-tab-pane>
    </el-tabs>
  </PageContainer>
</template>

<style scoped>
/* 页签栏与下方内容保持 16px 间距 */
.account-view :deep(.el-tabs__header) {
  margin-bottom: var(--ceams-space-4);
}

.account-view :deep(.el-tabs__item) {
  font-size: var(--ceams-font-size-base);
}

/* 选中页签加粗，与设计基线一致 */
.account-view :deep(.el-tabs__item.is-active) {
  font-weight: 600;
}
</style>

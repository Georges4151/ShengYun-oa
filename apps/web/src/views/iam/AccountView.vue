<script setup lang="ts">
import { computed, ref } from 'vue'
import { ElTabPane, ElTabs } from 'element-plus'
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
  <el-tabs v-model="activeTab">
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
</template>

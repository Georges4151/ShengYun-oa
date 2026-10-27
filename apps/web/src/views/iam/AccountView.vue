<script setup lang="ts">
import { ref } from 'vue'
import { ElTabPane, ElTabs } from 'element-plus'
import UserList from './components/UserList.vue'
import RoleList from './components/RoleList.vue'
import PermissionList from './components/PermissionList.vue'
import DeviceList from './components/DeviceList.vue'
import OperationLogList from './components/OperationLogList.vue'

defineOptions({ name: 'AccountView' })

const activeTab = ref('user')
</script>

<template>
  <el-tabs v-model="activeTab">
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
  </el-tabs>
</template>

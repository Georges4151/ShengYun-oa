<script setup lang="ts">
import { ref } from 'vue'
import { ElTabPane, ElTabs } from 'element-plus'
import ElectronicSealList from './components/ElectronicSealList.vue'
import ElectronicSealApplicationList from './components/ElectronicSealApplicationList.vue'
import StampApprovalList from './components/StampApprovalList.vue'
import ElectronicSealStampList from './components/ElectronicSealStampList.vue'
import ElectronicSealUsageList from './components/ElectronicSealUsageList.vue'

defineOptions({ name: 'SealView' })

const activeTab = ref('seal')
</script>

<template>
  <el-tabs v-model="activeTab">
    <el-tab-pane label="电子印章" name="seal">
      <ElectronicSealList />
    </el-tab-pane>
    <el-tab-pane label="用印申请" name="application">
      <ElectronicSealApplicationList />
    </el-tab-pane>
    <el-tab-pane label="用印审批" name="approval">
      <StampApprovalList />
    </el-tab-pane>
    <el-tab-pane label="盖章记录" name="stamp">
      <ElectronicSealStampList />
    </el-tab-pane>
    <el-tab-pane label="使用审计" name="usage">
      <ElectronicSealUsageList />
    </el-tab-pane>
  </el-tabs>
</template>

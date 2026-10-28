<script setup lang="ts">
import { ref } from 'vue'
import { ElTabPane, ElTabs } from 'element-plus'
import ReceiptList from './components/ReceiptList.vue'
import ApprovalRecordList from './components/ApprovalRecordList.vue'
import ReceiptVerificationList from './components/ReceiptVerificationList.vue'

defineOptions({ name: 'ExpenseView' })

const activeTab = ref('receipt')
</script>

<template>
  <el-tabs v-model="activeTab">
    <el-tab-pane label="票据" name="receipt">
      <ReceiptList />
    </el-tab-pane>
    <el-tab-pane label="审批记录" name="approval">
      <ApprovalRecordList />
    </el-tab-pane>
    <el-tab-pane label="核验记录" name="verification">
      <ReceiptVerificationList />
    </el-tab-pane>
  </el-tabs>
</template>

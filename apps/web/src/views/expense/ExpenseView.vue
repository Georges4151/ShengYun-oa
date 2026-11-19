<script setup lang="ts">
import { computed, ref } from 'vue'
import { ElTabPane, ElTabs } from 'element-plus'
import { useAuthStore } from '@/stores/auth'
import ReceiptList from './components/ReceiptList.vue'
import ApprovalRecordList from './components/ApprovalRecordList.vue'
import ReceiptVerificationList from './components/ReceiptVerificationList.vue'
import ReceiptPreAuditPanel from './components/ReceiptPreAuditPanel.vue'

defineOptions({ name: 'ExpenseView' })

const auth = useAuthStore()
const activeTab = ref('receipt')

/** AI 预审提示仅对财务 / 系统管理员渲染 */
const canPreAudit = computed(() =>
  auth.roles.some((role) => role.code === 'FINANCE' || role.code === 'SYS_ADMIN')
)
</script>

<template>
  <div class="expense-view">
    <!-- 页头：主标题 + 说明文字 -->
    <div class="page-header">
      <div>
        <h2 class="page-title">报销管理</h2>
        <p class="page-subtitle">集中管理票据、审批记录与核验记录，支持 AI 预审提示</p>
      </div>
    </div>

    <ReceiptPreAuditPanel v-if="canPreAudit" />

    <el-tabs v-model="activeTab" class="expense-tabs">
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
  </div>
</template>

<style scoped>
/* 页签头部与内容的间距 */
.expense-tabs :deep(.el-tabs__header) {
  margin-bottom: var(--ceams-space-4);
}
</style>

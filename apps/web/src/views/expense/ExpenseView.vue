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
  <div>
    <ReceiptPreAuditPanel v-if="canPreAudit" />

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
  </div>
</template>

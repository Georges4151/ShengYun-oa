<script setup lang="ts">
import { ref } from 'vue'
import { ElTabPane, ElTabs } from 'element-plus'
import { PageContainer } from '@ceams/ui-web'
import BankStatementList from './components/BankStatementList.vue'
import GeneralLedgerList from './components/GeneralLedgerList.vue'
import ReconciliationList from './components/ReconciliationList.vue'

defineOptions({ name: 'BankView' })

const activeTab = ref('statement')
</script>

<template>
  <PageContainer title="对账" subtitle="银行流水、总账与对账结果集中核对">
    <el-tabs v-model="activeTab" class="bank-view">
      <el-tab-pane label="银行流水" name="statement">
        <BankStatementList />
      </el-tab-pane>
      <el-tab-pane label="总账" name="ledger">
        <GeneralLedgerList />
      </el-tab-pane>
      <el-tab-pane label="对账结果" name="reconciliation">
        <ReconciliationList />
      </el-tab-pane>
    </el-tabs>
  </PageContainer>
</template>

<style scoped>
/* 页签栏与下方卡片区保持 16px 间距 */
.bank-view :deep(.el-tabs__header) {
  margin-bottom: var(--ceams-space-4, 16px);
}

.bank-view :deep(.el-tabs__item) {
  font-size: var(--ceams-font-size-base, 14px);
}

.bank-view :deep(.el-tabs__item.is-active) {
  font-weight: 600;
}
</style>

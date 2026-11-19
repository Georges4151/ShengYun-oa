<script setup lang="ts">
import { ref } from 'vue'
import { ElTabPane, ElTabs } from 'element-plus'
import { PageContainer } from '@ceams/ui-web'
import PaymentAccountList from './components/PaymentAccountList.vue'
import AccountList from './components/AccountList.vue'
import PaymentTransactionList from './components/PaymentTransactionList.vue'
import CostAllocationList from './components/CostAllocationList.vue'

defineOptions({ name: 'PaymentView' })

const activeTab = ref('account')
</script>

<template>
  <PageContainer title="资金" subtitle="付款账户、收支流水、付款交易与成本分摊统一管理">
    <el-tabs v-model="activeTab" class="payment-view">
      <el-tab-pane label="付款账户" name="account">
        <PaymentAccountList />
      </el-tab-pane>
      <el-tab-pane label="收支流水" name="ledger">
        <AccountList />
      </el-tab-pane>
      <el-tab-pane label="付款交易" name="transaction">
        <PaymentTransactionList />
      </el-tab-pane>
      <el-tab-pane label="成本分摊" name="allocation">
        <CostAllocationList />
      </el-tab-pane>
    </el-tabs>
  </PageContainer>
</template>

<style scoped>
/* 页签栏与下方卡片区保持 16px 间距 */
.payment-view :deep(.el-tabs__header) {
  margin-bottom: var(--ceams-space-4, 16px);
}

.payment-view :deep(.el-tabs__item) {
  font-size: var(--ceams-font-size-base, 14px);
}

.payment-view :deep(.el-tabs__item.is-active) {
  font-weight: 600;
}
</style>

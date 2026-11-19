<script setup lang="ts">
import { ref } from 'vue'
import { ElTabPane, ElTabs } from 'element-plus'
import PaymentAccountList from './components/PaymentAccountList.vue'
import AccountList from './components/AccountList.vue'
import PaymentTransactionList from './components/PaymentTransactionList.vue'
import CostAllocationList from './components/CostAllocationList.vue'

defineOptions({ name: 'PaymentView' })

const activeTab = ref('account')
</script>

<template>
  <el-tabs v-model="activeTab">
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
</template>

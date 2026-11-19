<script setup lang="ts">
import { ref } from 'vue'
import { ElTabPane, ElTabs } from 'element-plus'
import BankStatementList from './components/BankStatementList.vue'
import GeneralLedgerList from './components/GeneralLedgerList.vue'
import ReconciliationList from './components/ReconciliationList.vue'

defineOptions({ name: 'BankView' })

const activeTab = ref('statement')
</script>

<template>
  <el-tabs v-model="activeTab">
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
</template>

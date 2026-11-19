<script setup lang="ts">
import { ref } from 'vue'
import { ElTabPane, ElTabs } from 'element-plus'
import ProjectList from './components/ProjectList.vue'
import BudgetList from './components/BudgetList.vue'
import ChangeRequestList from './components/ChangeRequestList.vue'
import ProgressPaymentList from './components/ProgressPaymentList.vue'
import SubcontractorList from './components/SubcontractorList.vue'
import SubcontractorContractList from './components/SubcontractorContractList.vue'
import SettlementProofList from './components/SettlementProofList.vue'

defineOptions({ name: 'ProjectView' })

const activeTab = ref('project')
</script>

<template>
  <el-tabs v-model="activeTab">
    <el-tab-pane label="项目" name="project">
      <ProjectList />
    </el-tab-pane>
    <el-tab-pane label="预算" name="budget">
      <BudgetList />
    </el-tab-pane>
    <el-tab-pane label="变更" name="change">
      <ChangeRequestList />
    </el-tab-pane>
    <el-tab-pane label="进度款" name="payment">
      <ProgressPaymentList />
    </el-tab-pane>
    <el-tab-pane label="分包商" name="subcontractor">
      <SubcontractorList />
    </el-tab-pane>
    <el-tab-pane label="分包合同" name="contract">
      <SubcontractorContractList />
    </el-tab-pane>
    <el-tab-pane label="结算凭证" name="proof">
      <SettlementProofList />
    </el-tab-pane>
  </el-tabs>
</template>

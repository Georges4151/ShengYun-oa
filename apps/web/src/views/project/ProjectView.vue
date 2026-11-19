<script setup lang="ts">
import { ref } from 'vue'
import { ElTabPane, ElTabs } from 'element-plus'
import { PageContainer } from '@ceams/ui-web'
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
  <PageContainer title="项目合同" subtitle="项目、预算、变更、进度款、分包商与结算凭证统一管理">
    <el-tabs v-model="activeTab" class="project-view">
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
  </PageContainer>
</template>

<style scoped>
/* 页签栏与下方内容保持 16px 间距 */
.project-view :deep(.el-tabs__header) {
  margin-bottom: var(--ceams-space-4, 16px);
}

.project-view :deep(.el-tabs__item) {
  font-size: var(--ceams-font-size-base, 14px);
}

/* 选中页签加粗，强化当前位置 */
.project-view :deep(.el-tabs__item.is-active) {
  font-weight: 600;
}
</style>

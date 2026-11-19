<script setup lang="ts">
import { ref } from 'vue'
import { ElTabPane, ElTabs } from 'element-plus'
import { PageContainer } from '@ceams/ui-web'
import EmployeeList from './components/EmployeeList.vue'
import ContractList from './components/ContractList.vue'
import AttendancePanel from './components/AttendancePanel.vue'
import SalaryList from './components/SalaryList.vue'
import PerformancePanel from './components/PerformancePanel.vue'
import PayrollPanel from './components/PayrollPanel.vue'

defineOptions({ name: 'HrView' })

const activeTab = ref('employee')
</script>

<template>
  <PageContainer title="人事管理" subtitle="员工档案、合同台账、考勤、薪酬与绩效集中管理">
    <el-tabs v-model="activeTab" class="hr-view">
      <el-tab-pane label="员工档案" name="employee">
        <EmployeeList />
      </el-tab-pane>
      <el-tab-pane label="合同台账" name="contract">
        <ContractList />
      </el-tab-pane>
      <el-tab-pane label="考勤管理" name="attendance">
        <AttendancePanel />
      </el-tab-pane>
      <el-tab-pane label="薪酬台账" name="salary">
        <SalaryList />
      </el-tab-pane>
      <el-tab-pane label="绩效管理" name="performance">
        <PerformancePanel />
      </el-tab-pane>
      <el-tab-pane label="工资核算" name="payroll">
        <PayrollPanel />
      </el-tab-pane>
    </el-tabs>
  </PageContainer>
</template>

<style scoped>
/* 页签栏与内容区保持 16px 间距 */
.hr-view :deep(.el-tabs__header) {
  margin-bottom: var(--ceams-space-4, 16px);
}

.hr-view :deep(.el-tabs__item) {
  font-size: var(--ceams-font-size-base, 14px);
}

/* 选中页签加粗，突出当前所在模块 */
.hr-view :deep(.el-tabs__item.is-active) {
  font-weight: 600;
}
</style>

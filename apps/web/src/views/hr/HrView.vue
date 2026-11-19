<script setup lang="ts">
import { ref } from 'vue'
import { ElTabPane, ElTabs } from 'element-plus'
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
  <el-tabs v-model="activeTab">
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
</template>

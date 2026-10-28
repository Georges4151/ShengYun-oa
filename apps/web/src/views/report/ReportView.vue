<script setup lang="ts">
import { onMounted, ref } from 'vue'
import {
  ElCard,
  ElCol,
  ElRow,
  ElTable,
  ElTableColumn,
  ElTabPane,
  ElTabs,
  ElTag
} from 'element-plus'
import { ceamsApi } from '@/services/http'
import { apiErrorMessage } from '@/utils/error'
import { financialSourceTypeLabels, transactionTypeLabels } from '@/utils/labels'
import type {
  DashboardVO,
  FinancialReportItemVO,
  FinancialReportVO,
  MaterialReportVO,
  ProjectCostReportVO
} from '@ceams/types'

defineOptions({ name: 'ReportView' })

const activeTab = ref('financial')

const dashboard = ref<DashboardVO | null>(null)
const financial = ref<FinancialReportVO | null>(null)
const projectCost = ref<ProjectCostReportVO | null>(null)
const material = ref<MaterialReportVO | null>(null)

const loadedFinancial = ref(false)
const loadedProjectCost = ref(false)
const loadedMaterial = ref(false)

async function loadDashboard(): Promise<void> {
  try {
    dashboard.value = await ceamsApi.report.dashboard()
  } catch (err) {
    apiErrorMessage(err, '加载概览失败')
  }
}

async function loadFinancial(): Promise<void> {
  try {
    financial.value = await ceamsApi.report.financial()
  } catch (err) {
    apiErrorMessage(err, '加载财务报表失败')
  }
}

async function loadProjectCost(): Promise<void> {
  try {
    projectCost.value = await ceamsApi.report.projectCost()
  } catch (err) {
    apiErrorMessage(err, '加载项目成本报表失败')
  }
}

async function loadMaterial(): Promise<void> {
  try {
    material.value = await ceamsApi.report.material()
  } catch (err) {
    apiErrorMessage(err, '加载材料报表失败')
  }
}

function onTabChange(name: string | number): void {
  if (name === 'financial' && !loadedFinancial.value) {
    loadedFinancial.value = true
    void loadFinancial()
  } else if (name === 'project-cost' && !loadedProjectCost.value) {
    loadedProjectCost.value = true
    void loadProjectCost()
  } else if (name === 'material' && !loadedMaterial.value) {
    loadedMaterial.value = true
    void loadMaterial()
  }
}

onMounted(() => {
  void loadDashboard()
  loadedFinancial.value = true
  void loadFinancial()
})
</script>

<template>
  <div class="report-view">
    <el-row :gutter="16" class="card-row">
      <el-col :span="6">
        <el-card shadow="never">
          <div class="metric">
            <div class="metric-label">待审批报销</div>
            <div class="metric-value">{{ dashboard?.pendingReceiptCount ?? 0 }}</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="never">
          <div class="metric">
            <div class="metric-label">待审批用印</div>
            <div class="metric-value">{{ dashboard?.pendingSealApplicationCount ?? 0 }}</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="never">
          <div class="metric">
            <div class="metric-label">未读通知</div>
            <div class="metric-value">{{ dashboard?.unreadNotificationCount ?? 0 }}</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="never">
          <div class="metric">
            <div class="metric-label">项目（进行中 / 总数）</div>
            <div class="metric-value">
              {{ dashboard?.inProgressProjectCount ?? 0 }} / {{ dashboard?.projectCount ?? 0 }}
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="16" class="card-row">
      <el-col :span="6">
        <el-card shadow="never">
          <div class="metric">
            <div class="metric-label">本月收入</div>
            <div class="metric-value">{{ dashboard?.monthIncome ?? '0' }}</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="never">
          <div class="metric">
            <div class="metric-label">本月支出</div>
            <div class="metric-value">{{ dashboard?.monthExpense ?? '0' }}</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="never">
          <div class="metric">
            <div class="metric-label">累计收入</div>
            <div class="metric-value">{{ dashboard?.totalIncome ?? '0' }}</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="never">
          <div class="metric">
            <div class="metric-label">累计支出</div>
            <div class="metric-value">{{ dashboard?.totalExpense ?? '0' }}</div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-tabs v-model="activeTab" @tab-change="onTabChange">
      <el-tab-pane label="财务报表" name="financial">
        <div class="report-summary" v-if="financial">
          <span>合计收入：{{ financial.totalIncome }}</span>
          <span>合计支出：{{ financial.totalExpense }}</span>
          <span>净额：{{ financial.netAmount }}</span>
        </div>
        <el-table :data="financial?.items ?? []" border>
          <el-table-column prop="transactionDate" label="交易时间" width="170" />
          <el-table-column label="来源" width="110">
            <template #default="{ row }">
              <el-tag size="small">{{ financialSourceTypeLabels[(row as FinancialReportItemVO).sourceType] }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="收支" width="90">
            <template #default="{ row }">
              <el-tag size="small" :type="(row as FinancialReportItemVO).type === 'INCOME' ? 'success' : 'danger'">
                {{ transactionTypeLabels[(row as FinancialReportItemVO).type] }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="projectName" label="项目" width="150" show-overflow-tooltip />
          <el-table-column prop="accountName" label="账户" width="150" show-overflow-tooltip />
          <el-table-column prop="amount" label="金额" width="120" />
          <el-table-column prop="description" label="描述" min-width="200" show-overflow-tooltip />
        </el-table>
      </el-tab-pane>

      <el-tab-pane label="项目成本" name="project-cost">
        <div class="report-summary" v-if="projectCost">
          <span>预算合计：{{ projectCost.totalBudget }}</span>
          <span>变更合计：{{ projectCost.totalChanged }}</span>
          <span>已付合计：{{ projectCost.totalPaid }}</span>
          <span>分摊合计：{{ projectCost.totalAllocated }}</span>
        </div>
        <el-table :data="projectCost?.items ?? []" border>
          <el-table-column prop="projectName" label="项目" min-width="160" show-overflow-tooltip />
          <el-table-column prop="contractAmount" label="合同金额" width="120" />
          <el-table-column prop="budgetAmount" label="预算金额" width="120" />
          <el-table-column prop="changedAmount" label="已变更" width="120" />
          <el-table-column prop="paidAmount" label="已付进度款" width="120" />
          <el-table-column prop="allocatedAmount" label="成本分摊" width="120" />
          <el-table-column prop="actualAmount" label="实际已发生" width="120" />
        </el-table>
      </el-tab-pane>

      <el-tab-pane label="材料报表" name="material">
        <div class="report-summary" v-if="material">
          <span>材料总数：{{ material.totalCount }}</span>
          <span>金额总计：{{ material.totalAmount }}</span>
        </div>
        <el-table :data="material?.items ?? []" border>
          <el-table-column prop="materialCode" label="材料编码" width="130" />
          <el-table-column prop="materialName" label="材料名" min-width="150" show-overflow-tooltip />
          <el-table-column prop="specification" label="规格" width="140" show-overflow-tooltip />
          <el-table-column prop="unit" label="单位" width="80" />
          <el-table-column prop="currentPrice" label="当前单价" width="110" />
          <el-table-column prop="transactionCount" label="领用/采购笔数" width="130" />
          <el-table-column prop="totalAmount" label="金额合计" width="120" />
        </el-table>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<style scoped>
.card-row {
  margin-bottom: 16px;
}
.metric {
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.metric-label {
  font-size: 13px;
  color: #909399;
}
.metric-value {
  font-size: 24px;
  font-weight: 600;
  color: #303133;
}
.report-summary {
  display: flex;
  gap: 24px;
  padding: 8px 0 12px;
  color: #606266;
  font-size: 14px;
}
</style>

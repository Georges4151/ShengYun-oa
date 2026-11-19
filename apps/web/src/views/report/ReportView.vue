<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import {
  ElButton,
  ElCard,
  ElCol,
  ElMessage,
  ElRow,
  ElTable,
  ElTableColumn,
  ElTabPane,
  ElTabs,
  ElTag
} from 'element-plus'
import { ApiError } from '@ceams/api-client'
import { hasPermission } from '@/composables/usePermission'
import { ceamsApi, fetchBlob } from '@/services/http'
import { apiErrorMessage } from '@/utils/error'
import { saveBlob } from '@/utils/file'
import { financialSourceTypeLabels, transactionTypeLabels } from '@/utils/labels'
import type {
  AdminAssetReportVO,
  BpmEfficiencyReportVO,
  DashboardVO,
  FinancialReportItemVO,
  FinancialReportVO,
  HrCostReportVO,
  HrPerformanceReportVO,
  MaterialReportVO,
  ProjectCostReportVO
} from '@ceams/types'

defineOptions({ name: 'ReportView' })

const activeTab = ref('financial')

/** 报表卡片与默认查询使用的当前核算月（yyyy-MM）。 */
const currentMonth = (() => {
  const now = new Date()
  return `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}`
})()

const dashboard = ref<DashboardVO | null>(null)
const financial = ref<FinancialReportVO | null>(null)
const projectCost = ref<ProjectCostReportVO | null>(null)
const material = ref<MaterialReportVO | null>(null)

const hrCost = ref<HrCostReportVO | null>(null)
const hrPerformance = ref<HrPerformanceReportVO | null>(null)
const adminAssets = ref<AdminAssetReportVO | null>(null)
const bpmEfficiency = ref<BpmEfficiencyReportVO | null>(null)

const loadedFinancial = ref(false)
const loadedProjectCost = ref(false)
const loadedMaterial = ref(false)
const loadedHrCost = ref(false)
const loadedHrPerformance = ref(false)
const loadedAdminAssets = ref(false)
const loadedBpmEfficiency = ref(false)

interface ReportTab {
  name: string
  label: string
  permission: string
}

/** P2 报表页签：无权限或服务端返回 403 时不渲染。 */
const P2_TABS: ReportTab[] = [
  { name: 'hr-cost', label: '人力成本', permission: 'report:hr-cost:view' },
  { name: 'hr-performance', label: '绩效考核', permission: 'report:hr-performance:view' },
  { name: 'admin-assets', label: '行政资产', permission: 'report:admin-assets:view' },
  { name: 'bpm-efficiency', label: '审批效率', permission: 'bpm:report:view' }
]

const deniedTabs = ref<string[]>([])

const p2Tabs = computed(() =>
  P2_TABS.filter((tab) => hasPermission(tab.permission) && !deniedTabs.value.includes(tab.name))
)

/** 审批超时率百分数展示（后端返回 0~1 的小数）。 */
const overdueRatePercent = computed(
  () => `${(Number(bpmEfficiency.value?.overdueRate ?? 0) * 100).toFixed(2)}%`
)

/** 各页签导出配置：路径、文件名与必要查询参数（与页面加载口径一致）。 */
const EXPORT_CONFIG: Record<
  string,
  { path: string; filename: string; params?: () => Record<string, string> }
> = {
  financial: { path: '/api/v1/reports/financial/export', filename: '财务报表.xlsx' },
  'project-cost': { path: '/api/v1/reports/project-cost/export', filename: '项目成本报表.xlsx' },
  material: { path: '/api/v1/reports/material/export', filename: '材料报表.xlsx' },
  'hr-cost': {
    path: '/api/v1/reports/hr-cost/export',
    filename: '人力成本报表.xlsx',
    params: () => ({ month: currentMonth })
  },
  'hr-performance': {
    path: '/api/v1/reports/hr-performance/export',
    filename: '绩效考核报表.xlsx',
    params: () => ({ period: currentMonth })
  },
  'admin-assets': {
    path: '/api/v1/reports/admin-assets/export',
    filename: '行政资产报表.xlsx'
  },
  'bpm-efficiency': {
    path: '/api/v1/reports/bpm-efficiency/export',
    filename: '审批效率报表.xlsx'
  }
}

const exporting = ref(false)

/** 导出当前页签的 Excel（后端按同一权限与查询口径生成）。 */
async function onExport(): Promise<void> {
  const config = EXPORT_CONFIG[String(activeTab.value)]
  if (!config) return
  exporting.value = true
  try {
    const params = new URLSearchParams(config.params?.() ?? {})
    const search = params.toString()
    const blob = await fetchBlob(`${config.path}${search ? `?${search}` : ''}`)
    saveBlob(blob, config.filename)
    ElMessage.success('导出成功')
  } catch (err) {
    ElMessage.error(apiErrorMessage(err, '导出失败'))
  } finally {
    exporting.value = false
  }
}

function isForbidden(err: unknown): boolean {
  if (err instanceof ApiError) {
    return err.status === 403
  }
  return (err as { response?: { status?: number } }).response?.status === 403
}

/**
 * 统一的新报表加载封装：
 * - 403 → 提示「无权限查看该报表」并隐藏该页签（不影响既有页签）
 * - 其他错误 → 用 apiErrorMessage 归一化后提示
 */
async function loadReport<T>(
  tab: string,
  loader: () => Promise<T>,
  apply: (value: T) => void,
  fallback: string
): Promise<void> {
  try {
    apply(await loader())
  } catch (err) {
    if (isForbidden(err)) {
      if (!deniedTabs.value.includes(tab)) {
        deniedTabs.value = [...deniedTabs.value, tab]
      }
      if (activeTab.value === tab) {
        activeTab.value = 'financial'
      }
      ElMessage.warning('无权限查看该报表')
      return
    }
    ElMessage.error(apiErrorMessage(err, fallback))
  }
}

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

function loadHrCost(): Promise<void> {
  return loadReport(
    'hr-cost',
    () => ceamsApi.report.hrCost({ month: currentMonth }),
    (value) => {
      hrCost.value = value
    },
    '加载人力成本报表失败'
  )
}

function loadHrPerformance(): Promise<void> {
  return loadReport(
    'hr-performance',
    () => ceamsApi.report.hrPerformance({ period: currentMonth }),
    (value) => {
      hrPerformance.value = value
    },
    '加载绩效考核报表失败'
  )
}

function loadAdminAssets(): Promise<void> {
  return loadReport(
    'admin-assets',
    () => ceamsApi.report.adminAssets(),
    (value) => {
      adminAssets.value = value
    },
    '加载行政资产报表失败'
  )
}

function loadBpmEfficiency(): Promise<void> {
  return loadReport(
    'bpm-efficiency',
    () => ceamsApi.report.bpmEfficiency(),
    (value) => {
      bpmEfficiency.value = value
    },
    '加载审批效率报表失败'
  )
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
  } else if (name === 'hr-cost' && !loadedHrCost.value) {
    loadedHrCost.value = true
    void loadHrCost()
  } else if (name === 'hr-performance' && !loadedHrPerformance.value) {
    loadedHrPerformance.value = true
    void loadHrPerformance()
  } else if (name === 'admin-assets' && !loadedAdminAssets.value) {
    loadedAdminAssets.value = true
    void loadAdminAssets()
  } else if (name === 'bpm-efficiency' && !loadedBpmEfficiency.value) {
    loadedBpmEfficiency.value = true
    void loadBpmEfficiency()
  }
}

onMounted(() => {
  void loadDashboard()
  loadedFinancial.value = true
  void loadFinancial()

  // 指标卡片数据按权限并行加载，失败仅提示，不影响页面
  if (hasPermission('report:hr-cost:view')) {
    loadedHrCost.value = true
    void loadHrCost()
  }
  if (hasPermission('report:admin-assets:view')) {
    loadedAdminAssets.value = true
    void loadAdminAssets()
  }
  if (hasPermission('bpm:report:view')) {
    loadedBpmEfficiency.value = true
    void loadBpmEfficiency()
  }
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

    <el-row :gutter="16" class="card-row">
      <el-col v-if="hasPermission('report:hr-cost:view')" :span="6">
        <el-card shadow="never">
          <div class="metric">
            <div class="metric-label">在职人数（含试用）</div>
            <div class="metric-value">{{ hrCost?.headcount ?? 0 }}</div>
          </div>
        </el-card>
      </el-col>
      <el-col v-if="hasPermission('report:hr-cost:view')" :span="6">
        <el-card shadow="never">
          <div class="metric">
            <div class="metric-label">本月人力成本</div>
            <div class="metric-value">{{ hrCost?.netPay ?? '0' }}</div>
          </div>
        </el-card>
      </el-col>
      <el-col v-if="hasPermission('report:admin-assets:view')" :span="6">
        <el-card shadow="never">
          <div class="metric">
            <div class="metric-label">在借资产（含逾期）</div>
            <div class="metric-value">{{ adminAssets?.borrowingCount ?? 0 }}</div>
            <div class="metric-sub">逾期 {{ adminAssets?.overdueCount ?? 0 }}</div>
          </div>
        </el-card>
      </el-col>
      <el-col v-if="hasPermission('bpm:report:view')" :span="6">
        <el-card shadow="never">
          <div class="metric">
            <div class="metric-label">审批超时率</div>
            <div class="metric-value">{{ overdueRatePercent }}</div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <div class="report-toolbar">
      <el-button type="primary" :loading="exporting" @click="onExport">导出当前报表</el-button>
    </div>

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

      <el-tab-pane v-for="tab in p2Tabs" :key="tab.name" :label="tab.label" :name="tab.name">
        <template v-if="tab.name === 'hr-cost'">
          <div class="report-summary" v-if="hrCost">
            <span>月份：{{ hrCost.month }}</span>
            <span>在职人数：{{ hrCost.headcount }}</span>
            <span>工资人数：{{ hrCost.payrollHeadcount }}</span>
            <span>应发：{{ hrCost.grossPay }}</span>
            <span>实发：{{ hrCost.netPay }}</span>
            <span>人均实发：{{ hrCost.avgNetPay }}</span>
          </div>
          <el-table :data="hrCost?.byDept ?? []" border>
            <el-table-column prop="deptName" label="部门" min-width="140" show-overflow-tooltip />
            <el-table-column prop="headcount" label="在职人数" width="110" />
            <el-table-column prop="payrollHeadcount" label="工资人数" width="110" />
            <el-table-column prop="grossPay" label="应发合计" width="130" />
            <el-table-column prop="netPay" label="实发合计" width="130" />
            <el-table-column prop="socialInsurance" label="社保" width="120" />
            <el-table-column prop="housingFund" label="公积金" width="120" />
            <el-table-column prop="tax" label="个税" width="120" />
          </el-table>
        </template>

        <template v-else-if="tab.name === 'hr-performance'">
          <div class="report-summary" v-if="hrPerformance">
            <span>周期：{{ hrPerformance.period }}</span>
            <span>评审数：{{ hrPerformance.reviewCount }}</span>
            <span>定稿数：{{ hrPerformance.finalizedCount }}</span>
            <span>平均分：{{ hrPerformance.avgFinalScore }}</span>
            <span>
              分布 A/B/C/D：{{ hrPerformance.distribution.a }} / {{ hrPerformance.distribution.b }} /
              {{ hrPerformance.distribution.c }} / {{ hrPerformance.distribution.d }}
            </span>
          </div>
          <el-table :data="hrPerformance?.byDept ?? []" border>
            <el-table-column prop="deptName" label="部门" min-width="160" show-overflow-tooltip />
            <el-table-column prop="reviewCount" label="评审数" width="120" />
            <el-table-column prop="finalizedCount" label="定稿数" width="120" />
            <el-table-column prop="avgFinalScore" label="平均分" width="120" />
          </el-table>
        </template>

        <template v-else-if="tab.name === 'admin-assets'">
          <div class="report-summary" v-if="adminAssets">
            <span>总数：{{ adminAssets.totalCount }}</span>
            <span>总价值：{{ adminAssets.totalValue }}</span>
            <span>借用中：{{ adminAssets.borrowingCount }}</span>
            <span>逾期：{{ adminAssets.overdueCount }}</span>
            <span>借用率：{{ adminAssets.borrowRate }}</span>
          </div>
          <el-table :data="adminAssets?.byCategory ?? []" border>
            <el-table-column prop="category" label="分类" width="180" />
            <el-table-column prop="count" label="数量" width="120" />
            <el-table-column prop="totalValue" label="购置金额合计" width="160" />
          </el-table>
        </template>

        <template v-else-if="tab.name === 'bpm-efficiency'">
          <div class="report-summary" v-if="bpmEfficiency">
            <span>实例数：{{ bpmEfficiency.instanceCount }}</span>
            <span>通过数：{{ bpmEfficiency.approvedCount }}</span>
            <span>平均时长（h）：{{ bpmEfficiency.avgDurationHours }}</span>
            <span>超时任务：{{ bpmEfficiency.overdueTaskCount }}</span>
            <span>超时率：{{ (Number(bpmEfficiency.overdueRate) * 100).toFixed(2) }}%</span>
          </div>
          <div class="section-title">按业务类型</div>
          <el-table :data="bpmEfficiency?.byBizType ?? []" border>
            <el-table-column prop="bizType" label="业务类型" min-width="160" />
            <el-table-column prop="instanceCount" label="实例数" width="120" />
            <el-table-column prop="approvedCount" label="通过数" width="120" />
            <el-table-column prop="avgDurationHours" label="平均时长（h）" width="150" />
          </el-table>
          <div class="section-title">待办积压节点</div>
          <el-table :data="bpmEfficiency?.backlogByNode ?? []" border>
            <el-table-column prop="nodeKey" label="节点编码" min-width="200" />
            <el-table-column prop="pendingCount" label="待处理数" width="140" />
          </el-table>
        </template>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<style scoped>
.report-toolbar {
  display: flex;
  justify-content: flex-end;
  margin-bottom: 8px;
}
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
.metric-sub {
  font-size: 12px;
  color: #909399;
}
.report-summary {
  display: flex;
  gap: 24px;
  padding: 8px 0 12px;
  color: #606266;
  font-size: 14px;
}
.section-title {
  margin: 12px 0 8px;
  font-size: 14px;
  font-weight: 600;
  color: #303133;
}
</style>

<template>
  <div class="dashboard-container">
    <el-row :gutter="20" class="stats-row">
      <el-col :xs="12" :sm="12" :md="6">
        <el-card class="stats-card" shadow="hover">
          <div class="stats-content">
            <div class="stats-icon project">
              <el-icon><Folder /></el-icon>
            </div>
            <div class="stats-info">
              <div class="stats-value">{{ statistics.projectCount || 0 }}</div>
              <div class="stats-label">项目总数</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="12" :sm="12" :md="6">
        <el-card class="stats-card" shadow="hover">
          <div class="stats-content">
            <div class="stats-icon account">
              <el-icon><Wallet /></el-icon>
            </div>
            <div class="stats-info">
              <div class="stats-value">{{ statistics.accountCount || 0 }}</div>
              <div class="stats-label">账目数量</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="12" :sm="12" :md="6">
        <el-card class="stats-card" shadow="hover">
          <div class="stats-content">
            <div class="stats-icon receipt">
              <el-icon><Tickets /></el-icon>
            </div>
            <div class="stats-info">
              <div class="stats-value">{{ statistics.receiptCount || 0 }}</div>
              <div class="stats-label">票据数量</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="12" :sm="12" :md="6">
        <el-card class="stats-card" shadow="hover">
          <div class="stats-content">
            <div class="stats-icon material">
              <el-icon><Box /></el-icon>
            </div>
            <div class="stats-info">
              <div class="stats-value">{{ statistics.materialCount || 0 }}</div>
              <div class="stats-label">材料数量</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20" class="chart-row">
      <el-col :xs="24" :md="12">
        <el-card class="chart-card">
          <template #header>
            <div class="card-header">
              <span>收支趋势</span>
            </div>
          </template>
          <div ref="incomeChartRef" class="chart-container"></div>
        </el-card>
      </el-col>
      <el-col :xs="24" :md="12">
        <el-card class="chart-card">
          <template #header>
            <div class="card-header">
              <span>项目状态分布</span>
            </div>
          </template>
          <div ref="projectChartRef" class="chart-container"></div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20" class="info-row">
      <el-col :xs="24" :md="12">
        <el-card>
          <template #header>
            <div class="card-header">
              <span>待办事项</span>
              <el-button type="primary" link @click="goApproval">查看更多</el-button>
            </div>
          </template>
          <el-table :data="pendingList" style="width: 100%">
            <el-table-column prop="type" label="类型" width="100" />
            <el-table-column prop="description" label="描述" />
            <el-table-column prop="createTime" label="时间" width="180" />
            <el-table-column label="操作" width="100">
              <template #default="{ row }">
                <el-button type="primary" link @click="handlePending(row)">处理</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>
      <el-col :xs="24" :md="12">
        <el-card>
          <template #header>
            <div class="card-header">
              <span>最近通知</span>
              <el-button type="primary" link @click="goNotification">查看更多</el-button>
            </div>
          </template>
          <el-table :data="notificationList" style="width: 100%">
            <el-table-column prop="title" label="标题" />
            <el-table-column prop="createTime" label="时间" width="180" />
            <el-table-column label="操作" width="100">
              <template #default="{ row }">
                <el-button type="primary" link @click="handleNotification(row)">查看</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import * as echarts from 'echarts'
import { getDashboardStatistics } from '@/api/dashboard'
import { getPendingReceipts } from '@/api/receipt'
import { getNotificationList } from '@/api/notification'

const router = useRouter()

const statistics = ref({
  projectCount: 0,
  accountCount: 0,
  receiptCount: 0,
  materialCount: 0
})

const pendingList = ref([])
const notificationList = ref([])

const incomeChartRef = ref(null)
const projectChartRef = ref(null)
let incomeChart = null
let projectChart = null

async function fetchStatistics() {
  try {
    const res = await getDashboardStatistics()
    statistics.value = res || {}
  } catch (error) {
    console.error('获取统计数据失败:', error)
  }
}

async function fetchPendingList() {
  try {
    const res = await getPendingReceipts({ page: 1, pageSize: 5 })
    pendingList.value = (res.list || res || []).map(item => ({
      id: item.id,
      type: '票据审批',
      description: item.description || `票据 #${item.id}`,
      createTime: item.createTime || item.uploadTime
    }))
  } catch (error) {
    console.error('获取待办列表失败:', error)
  }
}

async function fetchNotifications() {
  try {
    const res = await getNotificationList({ page: 1, pageSize: 5 })
    notificationList.value = res.list || res || []
  } catch (error) {
    console.error('获取通知列表失败:', error)
  }
}

function initIncomeChart() {
  if (!incomeChartRef.value) return
  
  incomeChart = echarts.init(incomeChartRef.value)
  
  const option = {
    tooltip: {
      trigger: 'axis'
    },
    legend: {
      data: ['收入', '支出']
    },
    grid: {
      left: '3%',
      right: '4%',
      bottom: '3%',
      containLabel: true
    },
    xAxis: {
      type: 'category',
      boundaryGap: false,
      data: ['1月', '2月', '3月', '4月', '5月', '6月', '7月', '8月', '9月', '10月', '11月', '12月']
    },
    yAxis: {
      type: 'value'
    },
    series: [
      {
        name: '收入',
        type: 'line',
        smooth: true,
        data: [820, 932, 901, 934, 1290, 1330, 1320, 1450, 1580, 1620, 1780, 1890],
        itemStyle: { color: '#67c23a' },
        areaStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: 'rgba(103, 194, 58, 0.3)' },
            { offset: 1, color: 'rgba(103, 194, 58, 0.1)' }
          ])
        }
      },
      {
        name: '支出',
        type: 'line',
        smooth: true,
        data: [620, 732, 701, 734, 1090, 1130, 1120, 1250, 1380, 1420, 1580, 1690],
        itemStyle: { color: '#f56c6c' },
        areaStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: 'rgba(245, 108, 108, 0.3)' },
            { offset: 1, color: 'rgba(245, 108, 108, 0.1)' }
          ])
        }
      }
    ]
  }
  
  incomeChart.setOption(option)
}

function initProjectChart() {
  if (!projectChartRef.value) return
  
  projectChart = echarts.init(projectChartRef.value)
  
  const option = {
    tooltip: {
      trigger: 'item',
      formatter: '{a} <br/>{b}: {c} ({d}%)'
    },
    legend: {
      orient: 'vertical',
      left: 'left'
    },
    series: [
      {
        name: '项目状态',
        type: 'pie',
        radius: ['40%', '70%'],
        avoidLabelOverlap: false,
        itemStyle: {
          borderRadius: 10,
          borderColor: '#fff',
          borderWidth: 2
        },
        label: {
          show: false,
          position: 'center'
        },
        emphasis: {
          label: {
            show: true,
            fontSize: 16,
            fontWeight: 'bold'
          }
        },
        labelLine: {
          show: false
        },
        data: [
          { value: 10, name: '进行中', itemStyle: { color: '#409eff' } },
          { value: 5, name: '已完成', itemStyle: { color: '#67c23a' } },
          { value: 3, name: '暂停', itemStyle: { color: '#e6a23c' } },
          { value: 2, name: '未开始', itemStyle: { color: '#909399' } }
        ]
      }
    ]
  }
  
  projectChart.setOption(option)
}

function handleResize() {
  incomeChart?.resize()
  projectChart?.resize()
}

function goApproval() {
  router.push('/receipt/approval')
}

function goNotification() {
  router.push('/notification/center')
}

function handlePending(row) {
  router.push(`/receipt/detail/${row.id}`)
}

function handleNotification(row) {
  router.push('/notification/center')
}

onMounted(() => {
  fetchStatistics()
  fetchPendingList()
  fetchNotifications()
  
  setTimeout(() => {
    initIncomeChart()
    initProjectChart()
  }, 100)
  
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  incomeChart?.dispose()
  projectChart?.dispose()
})
</script>

<style scoped>
.dashboard-container {
  padding: 20px;
}

.stats-row {
  margin-bottom: 20px;
}

.stats-card {
  margin-bottom: 20px;
}

.stats-content {
  display: flex;
  align-items: center;
}

.stats-icon {
  width: 60px;
  height: 60px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-right: 15px;
}

.stats-icon .el-icon {
  font-size: 28px;
  color: #fff;
}

.stats-icon.project {
  background: linear-gradient(135deg, #409eff 0%, #66b1ff 100%);
}

.stats-icon.account {
  background: linear-gradient(135deg, #67c23a 0%, #85ce61 100%);
}

.stats-icon.receipt {
  background: linear-gradient(135deg, #e6a23c 0%, #ebb563 100%);
}

.stats-icon.material {
  background: linear-gradient(135deg, #f56c6c 0%, #f78989 100%);
}

.stats-info {
  flex: 1;
}

.stats-value {
  font-size: 28px;
  font-weight: bold;
  color: #303133;
}

.stats-label {
  font-size: 14px;
  color: #909399;
  margin-top: 5px;
}

.chart-row {
  margin-bottom: 20px;
}

.chart-card {
  margin-bottom: 20px;
}

.chart-container {
  height: 300px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.info-row .el-card {
  margin-bottom: 20px;
}
</style>

<template>
  <div class="page-container">
    <el-card v-loading="pageLoading">
      <template #header>
        <div class="card-header">
          <span>票据详情</span>
          <div>
            <el-button type="primary" @click="handleVerify" v-if="receipt.verificationStatus === 'pending'">验证票据</el-button>
            <el-button @click="handleBack">返回</el-button>
          </div>
        </div>
      </template>

      <el-descriptions :column="2" border>
        <el-descriptions-item label="票据ID">{{ receipt.id }}</el-descriptions-item>
        <el-descriptions-item label="票据类型">{{ receipt.type }}</el-descriptions-item>
        <el-descriptions-item label="票据编号">{{ receipt.receiptNumber || '-' }}</el-descriptions-item>
        <el-descriptions-item label="金额">{{ formatMoney(receipt.amount) }}</el-descriptions-item>
        <el-descriptions-item label="报销人">{{ receipt.reporterName }}</el-descriptions-item>
        <el-descriptions-item label="票据日期">{{ receipt.receiptDate }}</el-descriptions-item>
        <el-descriptions-item label="上传时间">{{ receipt.uploadTime }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="getStatusType(receipt.verificationStatus)">
            {{ getStatusText(receipt.verificationStatus) }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="描述" :span="2">{{ receipt.description || '-' }}</el-descriptions-item>
      </el-descriptions>

      <el-divider />

      <h4>票据图片</h4>
      <div class="image-container" v-if="receipt.filePath">
        <el-image
          :src="getImageUrl(receipt.filePath)"
          :preview-src-list="[getImageUrl(receipt.filePath)]"
          fit="contain"
          style="max-height: 500px;"
        />
      </div>
      <el-empty v-else description="暂无票据图片" />

      <el-divider />

      <h4>审批历史</h4>
      <el-timeline v-if="approvalHistory.length > 0">
        <el-timeline-item
          v-for="item in approvalHistory"
          :key="item.id"
          :timestamp="item.approvalTime"
          :type="getApprovalType(item.status)"
          placement="top"
        >
          <el-card>
            <p><strong>审批人：</strong>{{ item.approverName }}</p>
            <p><strong>状态：</strong>{{ getStatusText(item.status) }}</p>
            <p v-if="item.comment"><strong>意见：</strong>{{ item.comment }}</p>
          </el-card>
        </el-timeline-item>
      </el-timeline>
      <el-empty v-else description="暂无审批记录" />
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { getReceiptDetail, getApprovalHistory } from '@/api/receipt'

const router = useRouter()
const route = useRoute()

const pageLoading = ref(false)
const receipt = ref({})
const approvalHistory = ref([])

async function fetchData() {
  const id = route.params.id
  if (!id) return

  pageLoading.value = true
  try {
    const [receiptRes, historyRes] = await Promise.all([
      getReceiptDetail(id),
      getApprovalHistory(id)
    ])
    receipt.value = receiptRes || {}
    approvalHistory.value = historyRes || []
  } catch (error) {
    console.error('获取票据详情失败:', error)
  } finally {
    pageLoading.value = false
  }
}

function handleVerify() {
  router.push(`/receipt/verify/${receipt.value.id}`)
}

function handleBack() {
  router.push('/receipt/list')
}

function getImageUrl(path) {
  if (!path) return ''
  if (path.startsWith('http')) return path
  return `${import.meta.env.VITE_API_BASE_URL}${path}`
}

function formatMoney(value) {
  if (!value) return '¥0.00'
  return '¥' + Number(value).toLocaleString('zh-CN', { minimumFractionDigits: 2 })
}

function getStatusType(status) {
  const map = {
    'pending': 'warning',
    'approved': 'success',
    'rejected': 'danger',
    'verified': 'success'
  }
  return map[status] || 'info'
}

function getStatusText(status) {
  const map = {
    'pending': '待审核',
    'approved': '已通过',
    'rejected': '已拒绝',
    'verified': '已验证'
  }
  return map[status] || status
}

function getApprovalType(status) {
  const map = {
    'approved': 'success',
    'rejected': 'danger',
    'pending': 'warning'
  }
  return map[status] || 'info'
}

onMounted(() => {
  fetchData()
})
</script>

<style scoped>
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.image-container {
  text-align: center;
  padding: 20px;
  background: #f5f7fa;
  border-radius: 4px;
}

h4 {
  margin-bottom: 15px;
  color: #303133;
}
</style>

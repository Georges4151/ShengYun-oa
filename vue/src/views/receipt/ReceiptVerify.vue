<template>
  <div class="page-container">
    <el-card v-loading="pageLoading">
      <template #header>
        <div class="card-header">
          <span>票据验证</span>
        </div>
      </template>

      <el-row :gutter="20">
        <el-col :span="12">
          <h4>票据信息</h4>
          <el-descriptions :column="1" border>
            <el-descriptions-item label="票据ID">{{ receipt.id }}</el-descriptions-item>
            <el-descriptions-item label="票据类型">{{ receipt.type }}</el-descriptions-item>
            <el-descriptions-item label="金额">{{ formatMoney(receipt.amount) }}</el-descriptions-item>
            <el-descriptions-item label="报销人">{{ receipt.reporterName }}</el-descriptions-item>
            <el-descriptions-item label="票据日期">{{ receipt.receiptDate }}</el-descriptions-item>
            <el-descriptions-item label="描述">{{ receipt.description || '-' }}</el-descriptions-item>
          </el-descriptions>

          <div class="image-preview" v-if="receipt.filePath">
            <h4>票据图片</h4>
            <el-image
              :src="getImageUrl(receipt.filePath)"
              :preview-src-list="[getImageUrl(receipt.filePath)]"
              fit="contain"
            />
          </div>
        </el-col>

        <el-col :span="12">
          <h4>验证操作</h4>
          <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
            <el-form-item label="验证结果" prop="status">
              <el-radio-group v-model="form.status">
                <el-radio label="approved">通过</el-radio>
                <el-radio label="rejected">拒绝</el-radio>
              </el-radio-group>
            </el-form-item>
            <el-form-item label="审批意见" prop="comment">
              <el-input
                v-model="form.comment"
                type="textarea"
                :rows="4"
                placeholder="请输入审批意见"
              />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :loading="loading" @click="handleSubmit">提交</el-button>
              <el-button @click="handleCancel">取消</el-button>
            </el-form-item>
          </el-form>

          <el-divider />

          <h4>审批历史</h4>
          <el-timeline v-if="approvalHistory.length > 0">
            <el-timeline-item
              v-for="item in approvalHistory"
              :key="item.id"
              :timestamp="item.approvalTime"
              :type="getApprovalType(item.status)"
            >
              <p><strong>{{ item.approverName }}</strong> - {{ getStatusText(item.status) }}</p>
              <p v-if="item.comment">{{ item.comment }}</p>
            </el-timeline-item>
          </el-timeline>
          <el-empty v-else description="暂无审批记录" :image-size="80" />
        </el-col>
      </el-row>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getReceiptDetail, getApprovalHistory, processApproval } from '@/api/receipt'

const router = useRouter()
const route = useRoute()

const formRef = ref(null)
const pageLoading = ref(false)
const loading = ref(false)
const receipt = ref({})
const approvalHistory = ref([])

const form = reactive({
  status: 'approved',
  comment: ''
})

const rules = {
  status: [{ required: true, message: '请选择验证结果', trigger: 'change' }]
}

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
    console.error('获取数据失败:', error)
  } finally {
    pageLoading.value = false
  }
}

async function handleSubmit() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  loading.value = true
  try {
    await processApproval({
      receiptId: receipt.value.id,
      status: form.status,
      comment: form.comment
    })
    ElMessage.success('验证成功')
    router.push('/receipt/list')
  } catch (error) {
    ElMessage.error('验证失败')
  } finally {
    loading.value = false
  }
}

function handleCancel() {
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

function getStatusText(status) {
  const map = {
    'pending': '待审核',
    'approved': '已通过',
    'rejected': '已拒绝'
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

h4 {
  margin-bottom: 15px;
  color: #303133;
}

.image-preview {
  margin-top: 20px;
}

.image-preview .el-image {
  max-width: 100%;
  max-height: 300px;
}
</style>

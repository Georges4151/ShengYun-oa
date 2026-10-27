<template>
  <div class="page-container">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>上传票据</span>
        </div>
      </template>

      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-width="120px"
        style="max-width: 800px;"
      >
        <el-form-item label="票据文件" prop="file">
          <el-upload
            ref="uploadRef"
            class="upload-area"
            drag
            :auto-upload="false"
            :limit="1"
            :on-change="handleFileChange"
            :on-remove="handleFileRemove"
            accept="image/*,.pdf"
          >
            <el-icon class="el-icon--upload"><UploadFilled /></el-icon>
            <div class="el-upload__text">
              将文件拖到此处，或<em>点击上传</em>
            </div>
            <template #tip>
              <div class="el-upload__tip">
                支持上传图片或PDF文件，大小不超过10MB
              </div>
            </template>
          </el-upload>
        </el-form-item>
        <el-form-item label="票据类型" prop="type">
          <el-select v-model="form.type" placeholder="请选择类型" style="width: 100%;">
            <el-option label="发票" value="发票" />
            <el-option label="收据" value="收据" />
            <el-option label="银行回单" value="银行回单" />
            <el-option label="其他" value="其他" />
          </el-select>
        </el-form-item>
        <el-form-item label="关联项目" prop="projectId">
          <el-select v-model="form.projectId" placeholder="请选择项目" style="width: 100%;">
            <el-option v-for="item in projectList" :key="item.id" :label="item.name" :value="item.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="报销人" prop="reporterName">
          <el-input v-model="form.reporterName" placeholder="请输入报销人姓名" />
        </el-form-item>
        <el-form-item label="票据日期" prop="receiptDate">
          <el-date-picker
            v-model="form.receiptDate"
            type="date"
            placeholder="选择票据日期"
            value-format="x"
            style="width: 100%;"
          />
        </el-form-item>
        <el-form-item label="金额" prop="amount">
          <el-input-number v-model="form.amount" :min="0" :precision="2" style="width: 100%;" />
        </el-form-item>
        <el-form-item label="描述" prop="description">
          <el-input v-model="form.description" type="textarea" :rows="3" placeholder="请输入票据描述" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="loading" @click="handleSubmit">提交</el-button>
          <el-button @click="handleCancel">取消</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { uploadReceipt } from '@/api/receipt'
import { getProjectList } from '@/api/project'

const router = useRouter()

const formRef = ref(null)
const uploadRef = ref(null)
const loading = ref(false)
const projectList = ref([])
const fileList = ref([])

const form = reactive({
  type: '',
  projectId: null,
  reporterName: '',
  receiptDate: null,
  amount: 0,
  description: ''
})

const rules = {
  type: [{ required: true, message: '请选择票据类型', trigger: 'change' }],
  reporterName: [{ required: true, message: '请输入报销人姓名', trigger: 'blur' }],
  amount: [{ required: true, message: '请输入金额', trigger: 'blur' }]
}

async function fetchProjectList() {
  try {
    const res = await getProjectList({ page: 1, pageSize: 100 })
    projectList.value = res.list || res || []
  } catch (error) {
    console.error('获取项目列表失败:', error)
  }
}

function handleFileChange(file) {
  fileList.value = [file.raw]
}

function handleFileRemove() {
  fileList.value = []
}

async function handleSubmit() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  if (fileList.value.length === 0) {
    ElMessage.warning('请上传票据文件')
    return
  }

  loading.value = true
  try {
    const formData = new FormData()
    formData.append('file', fileList.value[0])
    formData.append('type', form.type)
    if (form.projectId) formData.append('projectId', form.projectId)
    if (form.reporterName) formData.append('reporterName', form.reporterName)
    if (form.receiptDate) formData.append('receiptDate', form.receiptDate)
    if (form.amount) formData.append('amount', form.amount)
    if (form.description) formData.append('description', form.description)

    await uploadReceipt(formData)
    ElMessage.success('上传成功')
    router.push('/receipt/list')
  } catch (error) {
    ElMessage.error('上传失败')
  } finally {
    loading.value = false
  }
}

function handleCancel() {
  router.push('/receipt/list')
}

onMounted(() => {
  fetchProjectList()
})
</script>

<style scoped>
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.upload-area {
  width: 100%;
}

:deep(.el-upload-dragger) {
  width: 100%;
}
</style>

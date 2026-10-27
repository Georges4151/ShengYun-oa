<template>
  <div class="page-container">
    <el-card>
      <template #header><div class="card-header"><span>批量盖章</span></div></template>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="120px">
        <el-form-item label="选择印章" prop="sealId">
          <el-select v-model="form.sealId" placeholder="请选择印章" style="width: 400px;">
            <el-option v-for="item in sealList" :key="item.id" :label="item.name" :value="item.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="上传文件" prop="files">
          <el-upload ref="uploadRef" drag multiple :auto-upload="false" :file-list="fileList" accept=".pdf">
            <el-icon class="el-icon--upload"><UploadFilled /></el-icon>
            <div class="el-upload__text">将多个PDF文件拖到此处，或<em>点击上传</em></div>
            <template #tip><div class="el-upload__tip">支持批量上传PDF文件</div></template>
          </el-upload>
        </el-form-item>
        <el-form-item label="盖章位置" prop="position">
          <el-radio-group v-model="form.position">
            <el-radio label="right-bottom">右下角</el-radio>
            <el-radio label="right-top">右上角</el-radio>
            <el-radio label="center">居中</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="loading" @click="handleSubmit">开始批量盖章</el-button>
        </el-form-item>
      </el-form>

      <el-divider />

      <h4>处理结果</h4>
      <el-table :data="resultList" border stripe>
        <el-table-column prop="fileName" label="文件名" min-width="200" />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }"><el-tag :type="row.status === '成功' ? 'success' : 'danger'">{{ row.status }}</el-tag></template>
        </el-table-column>
        <el-table-column prop="message" label="消息" min-width="200" />
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getSealList } from '@/api/electronicSeal'

const formRef = ref(null)
const uploadRef = ref(null)
const loading = ref(false)
const sealList = ref([])
const fileList = ref([])
const resultList = ref([])

const form = reactive({ sealId: null, position: 'right-bottom' })
const rules = { sealId: [{ required: true, message: '请选择印章', trigger: 'change' }] }

async function fetchSealList() {
  try { const res = await getSealList(); sealList.value = res || [] } catch (error) { console.error(error) }
}

async function handleSubmit() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  loading.value = true
  resultList.value = []
  
  // 模拟批量处理
  setTimeout(() => {
    resultList.value = [
      { fileName: '合同1.pdf', status: '成功', message: '盖章完成' },
      { fileName: '合同2.pdf', status: '成功', message: '盖章完成' },
      { fileName: '合同3.pdf', status: '失败', message: '文件已加密，无法处理' }
    ]
    ElMessage.success('批量处理完成')
    loading.value = false
  }, 2000)
}

onMounted(() => { fetchSealList() })
</script>

<style scoped>
.card-header { display: flex; justify-content: space-between; align-items: center; }
h4 { margin-bottom: 15px; color: #303133; }
</style>

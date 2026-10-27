<template>
  <div class="page-container">
    <el-card>
      <template #header><div class="card-header"><span>电子盖章</span></div></template>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="120px" style="max-width: 800px;">
        <el-form-item label="选择印章" prop="sealId">
          <el-select v-model="form.sealId" placeholder="请选择印章" style="width: 100%;" @change="checkPermission">
            <el-option v-for="item in sealList" :key="item.id" :label="item.name" :value="item.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="上传文件" prop="file">
          <el-upload ref="uploadRef" drag :auto-upload="false" :limit="1" :on-change="handleFileChange" accept=".pdf">
            <el-icon class="el-icon--upload"><UploadFilled /></el-icon>
            <div class="el-upload__text">将PDF文件拖到此处，或<em>点击上传</em></div>
          </el-upload>
        </el-form-item>
        <el-form-item label="盖章位置" prop="position">
          <el-radio-group v-model="form.position">
            <el-radio label="right-bottom">右下角</el-radio>
            <el-radio label="right-top">右上角</el-radio>
            <el-radio label="center">居中</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="备注" prop="remark"><el-input v-model="form.remark" type="textarea" :rows="2" placeholder="请输入备注" /></el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="loading" :disabled="!canUse" @click="handleSubmit">确认盖章</el-button>
        </el-form-item>
      </el-form>

      <el-alert v-if="!canUse && form.sealId" title="您没有使用该印章的权限" type="warning" :closable="false" style="margin-bottom: 20px;" />
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getSealList, canUseSeal, stampContract } from '@/api/electronicSeal'

const formRef = ref(null)
const uploadRef = ref(null)
const loading = ref(false)
const sealList = ref([])
const canUse = ref(true)
const fileList = ref([])

const form = reactive({ sealId: null, position: 'right-bottom', remark: '' })
const rules = { sealId: [{ required: true, message: '请选择印章', trigger: 'change' }] }

async function fetchSealList() {
  try { const res = await getSealList(); sealList.value = res || [] } catch (error) { console.error(error) }
}

async function checkPermission() {
  if (!form.sealId) return
  try { canUse.value = await canUseSeal(form.sealId) } catch (error) { canUse.value = false }
}

function handleFileChange(file) {
  fileList.value = [file.raw]
}

async function handleSubmit() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  if (fileList.value.length === 0) { ElMessage.warning('请上传文件'); return }
  if (!canUse.value) { ElMessage.warning('您没有使用该印章的权限'); return }

  loading.value = true
  try {
    await stampContract({ sealId: form.sealId, position: form.position, remark: form.remark })
    ElMessage.success('盖章成功')
  } catch (error) { ElMessage.error('盖章失败') }
  finally { loading.value = false }
}

onMounted(() => { fetchSealList() })
</script>

<style scoped>
.card-header { display: flex; justify-content: space-between; align-items: center; }
</style>

<template>
  <div class="page-container">
    <el-card>
      <template #header><div class="card-header"><span>基础设置</span></div></template>
      <el-form :model="settings" label-width="150px" style="max-width: 600px;">
        <el-form-item label="系统名称"><el-input v-model="settings.systemName" placeholder="请输入系统名称" /></el-form-item>
        <el-form-item label="系统Logo">
          <el-upload action="#" :show-file-list="false"><el-button type="primary">上传Logo</el-button></el-upload>
        </el-form-item>
        <el-form-item label="版权信息"><el-input v-model="settings.copyright" placeholder="请输入版权信息" /></el-form-item>
        <el-form-item label="备案号"><el-input v-model="settings.icp" placeholder="请输入备案号" /></el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="loading" @click="handleSave">保存设置</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getSystemSettings, updateSystemSettings } from '@/api/system'

const loading = ref(false)
const settings = reactive({ systemName: '财务助手', copyright: '', icp: '' })

async function fetchSettings() {
  try { const res = await getSystemSettings(); Object.assign(settings, res) } catch (error) { console.error(error) }
}

async function handleSave() {
  loading.value = true
  try { await updateSystemSettings(settings); ElMessage.success('保存成功') } catch (error) { ElMessage.error('保存失败') }
  finally { loading.value = false }
}

onMounted(() => { fetchSettings() })
</script>

<style scoped>
.card-header { display: flex; justify-content: space-between; align-items: center; }
</style>

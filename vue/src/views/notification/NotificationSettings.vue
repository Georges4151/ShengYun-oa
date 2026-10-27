<template>
  <div class="page-container">
    <el-card>
      <template #header><div class="card-header"><span>通知设置</span></div></template>
      <el-form :model="settings" label-width="150px" style="max-width: 600px;">
        <el-form-item label="接收系统通知">
          <el-switch v-model="settings.systemNotification" />
        </el-form-item>
        <el-form-item label="接收审批提醒">
          <el-switch v-model="settings.approvalReminder" />
        </el-form-item>
        <el-form-item label="接收任务提醒">
          <el-switch v-model="settings.taskReminder" />
        </el-form-item>
        <el-form-item label="邮件通知">
          <el-switch v-model="settings.emailNotification" />
        </el-form-item>
        <el-form-item label="邮件地址" v-if="settings.emailNotification">
          <el-input v-model="settings.email" placeholder="请输入邮箱地址" />
        </el-form-item>
        <el-form-item label="短信通知">
          <el-switch v-model="settings.smsNotification" />
        </el-form-item>
        <el-form-item label="手机号码" v-if="settings.smsNotification">
          <el-input v-model="settings.phone" placeholder="请输入手机号码" />
        </el-form-item>
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
import { getNotificationSettings, updateNotificationSettings } from '@/api/notification'

const loading = ref(false)
const settings = reactive({
  systemNotification: true,
  approvalReminder: true,
  taskReminder: true,
  emailNotification: false,
  email: '',
  smsNotification: false,
  phone: ''
})

async function fetchSettings() {
  try { const res = await getNotificationSettings(); Object.assign(settings, res) } catch (error) { console.error(error) }
}

async function handleSave() {
  loading.value = true
  try { await updateNotificationSettings(settings); ElMessage.success('保存成功') } catch (error) { ElMessage.error('保存失败') }
  finally { loading.value = false }
}

onMounted(() => { fetchSettings() })
</script>

<style scoped>
.card-header { display: flex; justify-content: space-between; align-items: center; }
</style>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  ElButton,
  ElCard,
  ElDivider,
  ElForm,
  ElFormItem,
  ElInput,
  ElMessage
} from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { ApiError } from '@ceams/api-client'
import type { LoginRequest } from '@ceams/types'
import { ceamsApi } from '@/services/http'
import { useAuthStore } from '@/stores/auth'
import { apiErrorMessage } from '@/utils/error'
import { getOrCreateDeviceId } from '@/utils/device'

defineOptions({ name: 'LoginView' })

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()

const formRef = ref<FormInstance>()
const loading = ref(false)
const form = reactive<LoginRequest>({ username: '', password: '' })

const rules: FormRules<LoginRequest> = {
  username: [{ required: true, message: '请输入账号', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

async function handleLogin(): Promise<void> {
  if (!formRef.value) return
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  loading.value = true
  try {
    await auth.login({
      username: form.username,
      password: form.password,
      deviceId: getOrCreateDeviceId(),
      deviceName: 'Web 浏览器',
      platform: 'WEB'
    })
    ElMessage.success('登录成功')
    const redirect = typeof route.query.redirect === 'string' ? route.query.redirect : '/'
    await router.push(redirect)
  } catch (err) {
    ElMessage.error(apiErrorMessage(err, '登录失败，请稍后重试'))
  } finally {
    loading.value = false
  }
}

// ---------- 企业微信扫码登录 ----------

const wecomLoading = ref(false)

/** 获取授权地址后整页跳转到企业微信；回调由 /login/wecom 处理。 */
async function handleWecomLogin(): Promise<void> {
  wecomLoading.value = true
  try {
    const data = await ceamsApi.integration.getWecomQrcode(getOrCreateDeviceId())
    if (!data.loginUrl) {
      ElMessage.error('企业微信授权地址为空，请联系管理员')
      return
    }
    window.location.href = data.loginUrl
  } catch (err) {
    if (err instanceof ApiError && err.status === 503) {
      ElMessage.warning('企业微信扫码登录未启用')
    } else {
      ElMessage.error(apiErrorMessage(err, '企业微信扫码登录暂不可用'))
    }
    wecomLoading.value = false
  }
}
</script>

<template>
  <div class="login-page">
    <el-card class="login-card" header="CEAMS 建筑企业账目管理系统">
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
        <el-form-item label="账号" prop="username">
          <el-input v-model="form.username" placeholder="请输入账号" autocomplete="username" />
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input
            v-model="form.password"
            type="password"
            placeholder="请输入密码"
            show-password
            autocomplete="current-password"
            @keyup.enter="handleLogin"
          />
        </el-form-item>
        <el-button type="primary" class="login-btn" :loading="loading" @click="handleLogin">
          登录
        </el-button>
        <el-divider>或</el-divider>
        <el-button class="login-btn" :loading="wecomLoading" @click="handleWecomLogin">
          企业微信扫码登录
        </el-button>
      </el-form>
    </el-card>
  </div>
</template>

<style scoped>
.login-page {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 100vh;
  background: #f5f6f7;
}
.login-card {
  width: 360px;
}
.login-btn {
  width: 100%;
}
</style>

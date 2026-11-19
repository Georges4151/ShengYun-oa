<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElButton, ElDivider, ElForm, ElFormItem, ElInput, ElMessage } from 'element-plus'
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

/** 左侧品牌区的能力点文案。 */
const capabilities = ['项目 · 合同 · 资金全链路', '审批与印章合规可追溯', '多端协同，随时随地处理待办']

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
    <!-- 左侧品牌区：深蓝渐变 + 纯 CSS 几何装饰 -->
    <section class="brand">
      <div class="brand__decor brand__decor--ring" />
      <div class="brand__decor brand__decor--ring-sm" />
      <div class="brand__decor brand__decor--grid" />
      <div class="brand__decor brand__decor--glow" />

      <div class="brand__inner">
        <div class="brand__logo">
          <span class="brand__logo-mark">C</span>
          <span class="brand__logo-text">CEAMS</span>
        </div>

        <h1 class="brand__title">CEAMS 建筑企业账目管理系统</h1>
        <p class="brand__slogan">让项目、合同与资金在同一本账上清晰流转</p>

        <ul class="brand__points">
          <li v-for="item in capabilities" :key="item">{{ item }}</li>
        </ul>
      </div>

      <p class="brand__footer">建筑企业账目管理平台 · 数据驱动经营决策</p>
    </section>

    <!-- 右侧登录表单 -->
    <section class="form-pane">
      <div class="form-wrap">
        <h2 class="form-title">欢迎登录</h2>
        <p class="form-subtitle">请使用企业账号登录，进入您的工作台</p>

        <el-form ref="formRef" :model="form" :rules="rules" label-position="top" class="login-form">
          <el-form-item label="账号" prop="username">
            <el-input
              v-model="form.username"
              size="large"
              placeholder="请输入账号"
              autocomplete="username"
              @keyup.enter="handleLogin"
            />
          </el-form-item>
          <el-form-item label="密码" prop="password">
            <el-input
              v-model="form.password"
              size="large"
              type="password"
              placeholder="请输入密码"
              show-password
              autocomplete="current-password"
              @keyup.enter="handleLogin"
            />
          </el-form-item>

          <el-button type="primary" size="large" class="submit-btn" :loading="loading" @click="handleLogin">
            登录
          </el-button>
        </el-form>

        <el-divider class="login-divider">其他登录方式</el-divider>

        <el-button size="large" class="wecom-btn" :loading="wecomLoading" @click="handleWecomLogin">
          企业微信扫码登录
        </el-button>
      </div>
    </section>
  </div>
</template>

<style scoped>
.login-page {
  display: flex;
  min-height: 100vh;
  background-color: var(--ceams-bg-card);
}

/* ============ 左侧品牌区 ============ */
.brand {
  position: relative;
  display: flex;
  flex: 0 0 46%;
  flex-direction: column;
  justify-content: space-between;
  max-width: 46%;
  padding: 48px 56px 36px;
  overflow: hidden;
  color: var(--ceams-text-inverse);
  background-image: linear-gradient(155deg, #1b2536 0%, #16273f 52%, #0f172a 100%);
}

.brand__inner {
  position: relative;
  z-index: 1;
  margin: auto 0;
}

.brand__logo {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 40px;
}

.brand__logo-mark {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  border-radius: var(--ceams-radius-md);
  background-image: linear-gradient(135deg, #2563eb 0%, #1d4ed8 100%);
  font-size: 18px;
  font-weight: 700;
  line-height: 1;
  box-shadow: 0 4px 14px rgba(37, 99, 235, 0.4);
}

.brand__logo-text {
  font-size: 16px;
  font-weight: 600;
  letter-spacing: 0.14em;
}

.brand__title {
  max-width: 420px;
  font-size: 30px;
  font-weight: 700;
  line-height: 1.35;
  letter-spacing: 0.01em;
}

.brand__slogan {
  max-width: 400px;
  margin-top: 14px;
  font-size: var(--ceams-font-size-md);
  line-height: 1.7;
  color: rgba(255, 255, 255, 0.66);
}

.brand__points {
  display: flex;
  flex-direction: column;
  gap: 16px;
  margin: 40px 0 0;
  padding: 0;
  list-style: none;
}

.brand__points li {
  position: relative;
  padding-left: 28px;
  font-size: var(--ceams-font-size-base);
  color: rgba(255, 255, 255, 0.82);
}

.brand__points li::before {
  content: '';
  position: absolute;
  top: 1px;
  left: 0;
  width: 16px;
  height: 16px;
  border: 1px solid rgba(255, 255, 255, 0.42);
  border-radius: 50%;
}

.brand__points li::after {
  content: '';
  position: absolute;
  top: 5px;
  left: 5px;
  width: 6px;
  height: 3px;
  border-bottom: 1.5px solid rgba(255, 255, 255, 0.9);
  border-left: 1.5px solid rgba(255, 255, 255, 0.9);
  transform: rotate(-45deg);
}

.brand__footer {
  position: relative;
  z-index: 1;
  font-size: var(--ceams-font-size-xs);
  color: rgba(255, 255, 255, 0.36);
}

/* 纯 CSS 几何装饰：低透明度，不占布局 */
.brand__decor {
  position: absolute;
  pointer-events: none;
}

.brand__decor--ring {
  top: -120px;
  right: -100px;
  width: 360px;
  height: 360px;
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 50%;
}

.brand__decor--ring-sm {
  right: -40px;
  bottom: -140px;
  width: 260px;
  height: 260px;
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: 50%;
}

.brand__decor--grid {
  top: 40%;
  right: -60px;
  width: 320px;
  height: 320px;
  background-image: linear-gradient(rgba(255, 255, 255, 0.05) 1px, transparent 1px),
    linear-gradient(90deg, rgba(255, 255, 255, 0.05) 1px, transparent 1px);
  background-size: 32px 32px;
  transform: rotate(12deg);
  -webkit-mask-image: radial-gradient(circle at 60% 40%, #000 0%, transparent 70%);
  mask-image: radial-gradient(circle at 60% 40%, #000 0%, transparent 70%);
}

.brand__decor--glow {
  top: -160px;
  left: -120px;
  width: 420px;
  height: 420px;
  background-image: radial-gradient(circle, rgba(37, 99, 235, 0.26) 0%, transparent 66%);
}

/* ============ 右侧表单区 ============ */
.form-pane {
  display: flex;
  flex: 1;
  align-items: center;
  justify-content: center;
  padding: 40px 24px;
  background-color: var(--ceams-bg-card);
}

.form-wrap {
  width: 100%;
  max-width: 380px;
}

.form-title {
  font-size: var(--ceams-font-size-3xl);
  font-weight: 700;
  color: var(--ceams-text-primary);
}

.form-subtitle {
  margin-top: 8px;
  margin-bottom: 28px;
  font-size: var(--ceams-font-size-sm);
  color: var(--ceams-text-secondary);
}

.login-form :deep(.el-form-item) {
  margin-bottom: 20px;
}

.login-form :deep(.el-form-item__label) {
  padding-bottom: 6px;
  font-size: var(--ceams-font-size-sm);
}

/* 大尺寸圆角输入框 */
.login-form :deep(.el-input__wrapper) {
  padding: 2px 14px;
  border-radius: var(--ceams-radius-lg);
}

.submit-btn {
  width: 100%;
  height: 44px;
  margin-top: 4px;
  border-radius: var(--ceams-radius-lg);
  font-size: var(--ceams-font-size-md);
  font-weight: 600;
}

.login-divider {
  margin: 26px 0 20px;
  font-size: var(--ceams-font-size-xs);
  color: var(--ceams-text-placeholder);
}

.wecom-btn {
  width: 100%;
  height: 44px;
  border-radius: var(--ceams-radius-lg);
  border-color: var(--ceams-border-strong);
  font-size: var(--ceams-font-size-base);
  color: var(--ceams-text-primary);
}

.wecom-btn:hover {
  border-color: var(--ceams-color-primary);
  color: var(--ceams-color-primary);
  background-color: var(--ceams-color-primary-light);
}

/* ============ 响应式：窄屏隐藏品牌区，表单居中 ============ */
@media (max-width: 960px) {
  .brand {
    display: none;
  }

  .form-pane {
    padding: 32px 20px;
  }
}
</style>

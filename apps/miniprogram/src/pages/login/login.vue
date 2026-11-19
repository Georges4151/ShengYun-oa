<script setup lang="ts">
import { ref } from 'vue'
import type { LoginRequest } from '@ceams/types'
import { apiClient } from '../../platform/http'
import { setSession } from '../../platform/auth'
import { getOrCreateDeviceId } from '../../platform/storage'
import { apiErrorMessage } from '../../utils/error'

const username = ref('')
const password = ref('')
const loading = ref(false)
const errorText = ref('')
const activeField = ref<'username' | 'password' | ''>('')

function onFieldFocus(field: 'username' | 'password'): void {
  activeField.value = field
}

function onFieldBlur(): void {
  activeField.value = ''
}

async function handleLogin(): Promise<void> {
  if (!username.value) {
    uni.showToast({ title: '请输入账号', icon: 'none' })
    return
  }
  if (!password.value) {
    uni.showToast({ title: '请输入密码', icon: 'none' })
    return
  }

  errorText.value = ''
  loading.value = true
  try {
    const body: LoginRequest = {
      username: username.value,
      password: password.value,
      deviceId: getOrCreateDeviceId(),
      deviceName: '微信小程序',
      platform: 'MINIPROGRAM'
    }
    const data = await apiClient.iam.login(body)
    setSession(data)

    // 账号绑定：登录成功后绑定当前小程序设备（失败不阻断登录主流程）
    try {
      await apiClient.iam.deviceBind({
        deviceId: getOrCreateDeviceId(),
        deviceName: '微信小程序',
        platform: 'MINIPROGRAM'
      })
    } catch {
      // 设备绑定失败不影响登录
    }

    uni.showToast({ title: '登录成功', icon: 'success' })
    setTimeout(() => uni.reLaunch({ url: '/pages/index/index' }), 600)
  } catch (err) {
    errorText.value = apiErrorMessage(err, '登录失败')
    uni.showToast({ title: errorText.value, icon: 'none' })
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <view class="login">
    <!-- 品牌区：主色渐变 + 白底圆形标志 -->
    <view class="login__hero">
      <view class="login__logo">
        <text class="login__logo-mark">C</text>
      </view>
      <text class="login__title">CEAMS</text>
      <text class="login__subtitle">建筑财务移动办公平台</text>
    </view>

    <!-- 登录表单：白卡叠压品牌区 -->
    <view class="login__panel">
      <text class="login__panel-title">账号登录</text>

      <view class="login__field">
        <text class="login__label">账号（手机号）</text>
        <input
          v-model="username"
          class="login__input"
          :class="activeField === 'username' ? 'login__input--focus' : ''"
          type="text"
          placeholder="请输入账号"
          @focus="onFieldFocus('username')"
          @blur="onFieldBlur"
        />
      </view>

      <view class="login__field">
        <text class="login__label">密码</text>
        <input
          v-model="password"
          class="login__input"
          :class="activeField === 'password' ? 'login__input--focus' : ''"
          type="password"
          placeholder="请输入密码"
          @focus="onFieldFocus('password')"
          @blur="onFieldBlur"
        />
      </view>

      <view v-if="errorText" class="login__error">
        <text class="login__error-text">{{ errorText }}</text>
      </view>

      <button class="ceams-btn ceams-btn--primary login__btn" :disabled="loading" @click="handleLogin">
        登录
      </button>
    </view>
  </view>
</template>

<style scoped>
.login {
  min-height: 100vh;
  padding-bottom: var(--ceams-space-2xl);
  background-color: var(--ceams-bg-page);
  box-sizing: border-box;
}

/* ---------------- 顶部品牌区 ---------------- */
.login__hero {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: var(--ceams-space-3xl) var(--ceams-space-xl)
    calc(var(--ceams-space-3xl) + var(--ceams-space-2xl));
  background-image: linear-gradient(
    160deg,
    var(--ceams-color-primary) 0%,
    var(--ceams-color-primary-deep) 100%
  );
  border-bottom-left-radius: var(--ceams-radius-xl);
  border-bottom-right-radius: var(--ceams-radius-xl);
  box-sizing: border-box;
}

.login__logo {
  display: flex;
  align-items: center;
  justify-content: center;
  width: calc(var(--ceams-space-3xl) + var(--ceams-space-2xl));
  height: calc(var(--ceams-space-3xl) + var(--ceams-space-2xl));
  background-color: var(--ceams-bg-card);
  border-radius: var(--ceams-radius-pill);
  box-shadow: var(--ceams-shadow-raised);
}

.login__logo-mark {
  color: var(--ceams-color-primary);
  font-size: var(--ceams-font-size-display);
  font-weight: var(--ceams-font-weight-bold);
  line-height: var(--ceams-line-height-tight);
}

.login__title {
  margin-top: var(--ceams-space-lg);
  color: var(--ceams-text-inverse);
  font-size: var(--ceams-font-size-display);
  font-weight: var(--ceams-font-weight-bold);
  letter-spacing: var(--ceams-letter-spacing-tight);
  line-height: var(--ceams-line-height-tight);
}

.login__subtitle {
  margin-top: var(--ceams-space-xs);
  color: var(--ceams-text-inverse);
  opacity: 0.72;
  font-size: var(--ceams-font-size-caption);
  line-height: var(--ceams-line-height-normal);
}

/* ---------------- 登录表单白卡 ---------------- */
.login__panel {
  margin: calc(var(--ceams-space-3xl) * -1) var(--ceams-space-xl) 0;
  padding: var(--ceams-space-xl);
  background-color: var(--ceams-bg-card);
  border-radius: var(--ceams-radius-lg);
  box-shadow: var(--ceams-shadow-raised);
  box-sizing: border-box;
}

.login__panel-title {
  display: block;
  margin-bottom: var(--ceams-space-lg);
  color: var(--ceams-text-primary);
  font-size: var(--ceams-font-size-heading);
  font-weight: var(--ceams-font-weight-semibold);
  line-height: var(--ceams-line-height-tight);
}

.login__field {
  margin-bottom: var(--ceams-space-lg);
}

.login__label {
  display: block;
  margin-bottom: var(--ceams-space-xs);
  color: var(--ceams-text-secondary);
  font-size: var(--ceams-font-size-caption);
  line-height: var(--ceams-line-height-normal);
}

.login__input {
  width: 100%;
  height: var(--ceams-control-height);
  padding: 0 var(--ceams-space-md);
  background-color: var(--ceams-bg-subtle);
  border: var(--ceams-border-width) solid var(--ceams-border);
  border-radius: var(--ceams-radius-md);
  color: var(--ceams-text-primary);
  font-size: var(--ceams-font-size-body);
  box-sizing: border-box;
}

.login__input--focus {
  background-color: var(--ceams-bg-card);
  border-color: var(--ceams-color-primary);
}

/* ---------------- 错误提示条 ---------------- */
.login__error {
  display: flex;
  align-items: center;
  margin: 0 0 var(--ceams-space-lg);
  padding: var(--ceams-space-sm) var(--ceams-space-md);
  background-color: var(--ceams-color-danger-tint);
  border-radius: var(--ceams-radius-sm);
  box-sizing: border-box;
}

.login__error-text {
  color: var(--ceams-color-danger-deep);
  font-size: var(--ceams-font-size-caption);
  line-height: var(--ceams-line-height-normal);
}

.login__btn {
  width: 100%;
}

.login__btn[disabled] {
  opacity: 0.6;
}
</style>

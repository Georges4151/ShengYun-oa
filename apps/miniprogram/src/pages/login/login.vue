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

async function handleLogin(): Promise<void> {
  if (!username.value) {
    uni.showToast({ title: '请输入账号', icon: 'none' })
    return
  }
  if (!password.value) {
    uni.showToast({ title: '请输入密码', icon: 'none' })
    return
  }

  loading.value = true
  try {
    const body: LoginRequest = { username: username.value, password: password.value }
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
    uni.showToast({ title: apiErrorMessage(err, '登录失败'), icon: 'none' })
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <view class="login">
    <text class="login__title">CEAMS 移动办公</text>
    <view class="login__field">
      <text class="login__label">账号（手机号）</text>
      <input v-model="username" class="login__input" type="text" placeholder="请输入账号" />
    </view>
    <view class="login__field">
      <text class="login__label">密码</text>
      <input v-model="password" class="login__input" type="password" placeholder="请输入密码" />
    </view>
    <button class="login__btn" :disabled="loading" @click="handleLogin">登录</button>
  </view>
</template>

<style scoped>
.login {
  padding: 80rpx 40rpx;
}
.login__title {
  display: block;
  font-size: 44rpx;
  font-weight: 600;
  text-align: center;
}
.login__field {
  margin-top: 48rpx;
}
.login__label {
  display: block;
  font-size: 26rpx;
  color: #6b7280;
  margin-bottom: 12rpx;
}
.login__input {
  padding: 24rpx;
  border: 1rpx solid #e5e7eb;
  border-radius: 12rpx;
  background-color: #ffffff;
}
.login__btn {
  margin-top: 56rpx;
  background-color: #2563eb;
  color: #ffffff;
  border-radius: 12rpx;
  font-size: 30rpx;
}
.login__btn[disabled] {
  opacity: 0.6;
}
</style>

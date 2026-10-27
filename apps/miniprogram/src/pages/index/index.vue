<script setup lang="ts">
import { ref } from 'vue'
import { loginByPhone } from '../../platform'

const phone = ref('')

async function handleLogin(): Promise<void> {
  if (!phone.value) {
    uni.showToast({ title: '请输入手机号', icon: 'none' })
    return
  }
  await loginByPhone(phone.value)
  uni.showToast({ title: '登录功能建设中', icon: 'none' })
}
</script>

<template>
  <view class="login">
    <text class="login__title">CEAMS 移动办公</text>
    <input v-model="phone" class="login__input" type="number" placeholder="请输入手机号" />
    <button class="login__btn" type="primary" @click="handleLogin">登录</button>
  </view>
</template>

<style scoped>
.login {
  padding: 60rpx 40rpx;
}
.login__title {
  display: block;
  font-size: 40rpx;
  font-weight: 600;
  text-align: center;
}
.login__input {
  margin-top: 48rpx;
  padding: 20rpx;
  border: 1rpx solid #e5e7eb;
  border-radius: 8rpx;
}
.login__btn {
  margin-top: 48rpx;
}
</style>
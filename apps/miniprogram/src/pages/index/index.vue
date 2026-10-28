<script setup lang="ts">
import { ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { clearSession, getUserName, isLoggedIn } from '../../platform/auth'

interface MenuItem {
  title: string
  path: string
}

const name = ref('')
const menus: MenuItem[] = [
  { title: '项目与进度款', path: '/pages/project/list' },
  { title: '报销待办审批', path: '/pages/expense/pending' },
  { title: '只读账目', path: '/pages/payment/list' },
  { title: '通知列表', path: '/pages/notification/list' }
]

onShow(() => {
  if (!isLoggedIn()) {
    uni.redirectTo({ url: '/pages/login/login' })
    return
  }
  name.value = getUserName()
})

function open(path: string): void {
  uni.navigateTo({ url: path })
}

function logout(): void {
  uni.showModal({
    title: '提示',
    content: '确定退出登录吗？',
    success: (res) => {
      if (res.confirm) {
        clearSession()
        uni.reLaunch({ url: '/pages/login/login' })
      }
    }
  })
}
</script>

<template>
  <view class="home">
    <view class="home__header">
      <text class="home__greeting">{{ name ? '你好，' + name : 'CEAMS 移动办公' }}</text>
      <button class="home__logout" @click="logout">退出</button>
    </view>
    <view class="home__menu">
      <view v-for="item in menus" :key="item.path" class="home__card" @click="open(item.path)">
        <text class="home__card-title">{{ item.title }}</text>
        <text class="home__card-arrow">›</text>
      </view>
    </view>
  </view>
</template>

<style scoped>
.home {
  padding: 40rpx 32rpx;
}
.home__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 40rpx;
}
.home__greeting {
  font-size: 36rpx;
  font-weight: 600;
}
.home__logout {
  font-size: 26rpx;
  color: #2563eb;
  background-color: transparent;
  padding: 0 8rpx;
  line-height: 1;
}
.home__logout::after {
  border: none;
}
.home__menu {
  display: flex;
  flex-direction: column;
  gap: 20rpx;
}
.home__card {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 32rpx 28rpx;
  background-color: #ffffff;
  border-radius: 16rpx;
  box-shadow: 0 2rpx 8rpx rgba(0, 0, 0, 0.04);
}
.home__card-title {
  font-size: 30rpx;
}
.home__card-arrow {
  font-size: 40rpx;
  color: #9ca3af;
}
</style>

<script setup lang="ts">
import { reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElButton, ElCard, ElForm, ElFormItem, ElInput } from 'element-plus'
import { useAuthStore } from '@/stores/auth'

defineOptions({ name: 'LoginView' })

const router = useRouter()
const auth = useAuthStore()

const form = reactive({ username: '', password: '' })

async function handleLogin(): Promise<void> {
  // 占位：登录接口待后端 iam 契约冻结后接入（经 @ceams/api-client）
  auth.setToken('placeholder-token')
  auth.setUsername(form.username || '管理员')
  await router.push('/')
}
</script>

<template>
  <div class="login-page">
    <el-card class="login-card" header="CEAMS 建筑企业账目管理系统">
      <el-form :model="form" label-position="top">
        <el-form-item label="账号">
          <el-input v-model="form.username" placeholder="请输入账号" />
        </el-form-item>
        <el-form-item label="密码">
          <el-input v-model="form.password" type="password" placeholder="请输入密码" show-password />
        </el-form-item>
        <el-button type="primary" class="login-btn" @click="handleLogin">登录</el-button>
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
<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElButton, ElCard, ElMessage, ElResult } from 'element-plus'
import { ApiError } from '@ceams/api-client'
import { useAuthStore } from '@/stores/auth'
import { apiErrorMessage } from '@/utils/error'

defineOptions({ name: 'WeComCallbackView' })

type CallbackStatus = 'processing' | 'error'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()

const status = ref<CallbackStatus>('processing')
const errorText = ref('')

function readQuery(key: 'code' | 'state'): string {
  const value = route.query[key]
  return typeof value === 'string' ? value : ''
}

function fail(message: string): void {
  status.value = 'error'
  errorText.value = message
}

/** 后端错误码 -> 前端提示（400 过期/未绑定、403 禁用、503 未启用）。 */
function mapError(err: unknown): string {
  if (err instanceof ApiError) {
    switch (err.status) {
      case 400:
        return '二维码已过期或账号未绑定'
      case 403:
        return '账号已禁用'
      case 503:
        return '企业微信未启用'
      default:
        break
    }
  }
  return apiErrorMessage(err, '企业微信登录失败，请返回登录页重试')
}

async function backToLogin(): Promise<void> {
  await router.replace('/login')
}

onMounted(async () => {
  const code = readQuery('code')
  const state = readQuery('state')
  if (!code || !state) {
    fail('授权参数缺失，请返回登录页重新扫码登录')
    return
  }

  try {
    // 复用 auth store：内部 setSession 落库令牌与用户，避免组件重复处理 localStorage
    await auth.loginByWecom({ code, state })
  } catch (err) {
    fail(mapError(err))
    return
  }

  ElMessage.success('登录成功')
  await router.replace('/')
})
</script>

<template>
  <div class="callback-page">
    <el-card class="callback-card" shadow="never">
      <el-result
        v-if="status === 'processing'"
        icon="info"
        title="正在登录"
        sub-title="正在校验企业微信授权信息，请稍候"
      />
      <el-result v-else icon="error" title="企业微信登录失败" :sub-title="errorText">
        <template #extra>
          <el-button type="primary" @click="backToLogin">返回登录</el-button>
        </template>
      </el-result>
    </el-card>
  </div>
</template>

<style scoped>
.callback-page {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 100vh;
  background: #f5f6f7;
}
.callback-card {
  width: 420px;
}
</style>

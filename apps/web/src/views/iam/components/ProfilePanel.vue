<script setup lang="ts">
import { computed, ref } from 'vue'
import {
  ElAvatar,
  ElButton,
  ElCard,
  ElDescriptions,
  ElDescriptionsItem,
  ElMessage,
  ElMessageBox,
  ElTag
} from 'element-plus'
import { ceamsApi } from '@/services/http'
import { useAuthStore } from '@/stores/auth'
import { apiErrorMessage } from '@/utils/error'

defineOptions({ name: 'ProfilePanel' })

/** 头像允许的 MIME 类型（与后端白名单一致：png / jpeg / webp） */
const ALLOWED_TYPES = ['image/png', 'image/jpeg', 'image/webp']
/** 头像大小上限 2MB */
const MAX_AVATAR_SIZE = 2 * 1024 * 1024
/** 无 MIME 时的扩展名兜底白名单 */
const ALLOWED_EXTENSIONS = ['png', 'jpg', 'jpeg', 'webp']

const auth = useAuthStore()

/** 隐藏的文件选择框引用 */
const fileInputRef = ref<HTMLInputElement>()
/** 上传中标记：用于禁用按钮，避免重复提交 */
const uploading = ref(false)
/** 清除头像中标记 */
const clearing = ref(false)

/** 当前用户头像地址（空字符串 / null 时回退首字） */
const avatarSrc = computed(() => auth.user?.avatar || undefined)
/** 无头像时展示的首字：优先姓名，其次账号，最后兜底「用」 */
const avatarText = computed(() => (auth.name || auth.username || '用').slice(0, 1))
/** 当前用户角色集合 */
const roles = computed(() => auth.user?.roles ?? [])

/** 取小写扩展名（不含点） */
function extensionOf(name: string): string {
  const index = name.lastIndexOf('.')
  return index < 0 ? '' : name.slice(index + 1).toLowerCase()
}

/** 点击「上传头像」：唤起隐藏的文件选择框 */
function triggerSelect(): void {
  if (uploading.value) return
  fileInputRef.value?.click()
}

/** 选择文件后的前端校验与上传流程 */
async function onFileChange(event: Event): Promise<void> {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0] ?? null
  // 立即清空，保证同一文件可重复选择
  input.value = ''
  if (!file) return

  // 类型校验：优先 MIME，浏览器未提供 MIME 时按扩展名兜底
  const typeAllowed = file.type ? ALLOWED_TYPES.includes(file.type) : ALLOWED_EXTENSIONS.includes(extensionOf(file.name))
  if (!typeAllowed) {
    ElMessage.error('头像仅支持 PNG / JPEG / WebP 格式')
    return
  }
  if (file.size > MAX_AVATAR_SIZE) {
    ElMessage.error('头像大小不能超过 2MB')
    return
  }
  if (file.size <= 0) {
    ElMessage.error('文件内容为空，请重新选择')
    return
  }

  uploading.value = true
  try {
    const data = await ceamsApi.iam.uploadMyAvatar(file, file.name)
    if (auth.user) {
      // 同步本地会话中的头像地址并持久化，顶栏头像随之刷新
      auth.setUser({ ...auth.user, avatar: data.avatar })
    }
    ElMessage.success('头像已更新')
  } catch (err) {
    ElMessage.error(apiErrorMessage(err, '头像上传失败'))
  } finally {
    uploading.value = false
  }
}

/** 恢复默认头像：二次确认后调用清除接口 */
async function onClearAvatar(): Promise<void> {
  try {
    await ElMessageBox.confirm('确定要恢复默认头像吗？', '提示', {
      type: 'warning',
      confirmButtonText: '确定',
      cancelButtonText: '取消'
    })
  } catch {
    // 用户取消，无需处理
    return
  }

  clearing.value = true
  try {
    await ceamsApi.iam.clearMyAvatar()
    if (auth.user) {
      auth.setUser({ ...auth.user, avatar: null })
    }
    ElMessage.success('已恢复默认头像')
  } catch (err) {
    ElMessage.error(apiErrorMessage(err, '恢复默认头像失败'))
  } finally {
    clearing.value = false
  }
}
</script>

<template>
  <el-card shadow="never" class="profile-card">
    <template #header>
      <span class="card-title">个人资料</span>
    </template>

    <div class="avatar-section">
      <el-avatar :size="72" :src="avatarSrc">{{ avatarText }}</el-avatar>
      <div class="avatar-actions">
        <el-button type="primary" size="small" :loading="uploading" :disabled="uploading" @click="triggerSelect">
          上传头像
        </el-button>
        <el-button v-if="auth.user?.avatar" size="small" :loading="clearing" :disabled="clearing" @click="onClearAvatar">
          恢复默认头像
        </el-button>
        <div class="avatar-tip">支持 PNG / JPEG / WebP，大小不超过 2MB</div>
      </div>
      <!-- 隐藏的文件选择框：仅由「上传头像」按钮触发 -->
      <input
        ref="fileInputRef"
        class="hidden-file-input"
        type="file"
        accept="image/png,image/jpeg,image/webp"
        @change="onFileChange"
      />
    </div>

    <el-descriptions :column="1" border>
      <el-descriptions-item label="账号">{{ auth.user?.username || '-' }}</el-descriptions-item>
      <el-descriptions-item label="姓名">{{ auth.user?.name || '-' }}</el-descriptions-item>
      <el-descriptions-item label="角色">
        <template v-if="roles.length">
          <el-tag v-for="role in roles" :key="role.id" class="role-tag" size="small" type="info">
            {{ role.name }}（{{ role.code }}）
          </el-tag>
        </template>
        <span v-else>-</span>
      </el-descriptions-item>
    </el-descriptions>
  </el-card>
</template>

<style scoped>
.profile-card {
  max-width: 720px;
}
.card-title {
  font-weight: 600;
}
.avatar-section {
  display: flex;
  align-items: center;
  gap: 20px;
  margin-bottom: 20px;
}
.avatar-actions {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
}
.avatar-tip {
  width: 100%;
  font-size: 12px;
  color: #909399;
}
.hidden-file-input {
  display: none;
}
.role-tag {
  margin-right: 6px;
}
</style>

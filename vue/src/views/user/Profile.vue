<template>
  <div class="page-container">
    <el-card>
      <template #header><div class="card-header"><span>个人中心</span></div></template>
      <el-row :gutter="20">
        <el-col :span="8">
          <div class="avatar-section">
            <el-avatar :size="120" :src="userInfo.avatar">{{ userInfo.realName?.charAt(0) }}</el-avatar>
            <el-upload action="#" :show-file-list="false" :before-upload="beforeAvatarUpload">
              <el-button type="primary" size="small" style="margin-top: 10px;">更换头像</el-button>
            </el-upload>
          </div>
        </el-col>
        <el-col :span="16">
          <el-form :model="userInfo" label-width="100px">
            <el-form-item label="用户名"><el-input v-model="userInfo.username" disabled /></el-form-item>
            <el-form-item label="姓名"><el-input v-model="userInfo.realName" /></el-form-item>
            <el-form-item label="手机号"><el-input v-model="userInfo.phone" /></el-form-item>
            <el-form-item label="邮箱"><el-input v-model="userInfo.email" /></el-form-item>
            <el-form-item label="角色"><el-tag>{{ userInfo.roleName }}</el-tag></el-form-item>
            <el-form-item label="创建时间">{{ userInfo.createTime }}</el-form-item>
            <el-form-item><el-button type="primary" :loading="saveLoading" @click="handleSaveInfo">保存信息</el-button></el-form-item>
          </el-form>
        </el-col>
      </el-row>

      <el-divider />

      <h4>修改密码</h4>
      <el-form ref="passwordFormRef" :model="passwordForm" :rules="passwordRules" label-width="100px" style="max-width: 400px;">
        <el-form-item label="原密码" prop="oldPassword"><el-input v-model="passwordForm.oldPassword" type="password" show-password /></el-form-item>
        <el-form-item label="新密码" prop="newPassword"><el-input v-model="passwordForm.newPassword" type="password" show-password /></el-form-item>
        <el-form-item label="确认密码" prop="confirmPassword"><el-input v-model="passwordForm.confirmPassword" type="password" show-password /></el-form-item>
        <el-form-item><el-button type="primary" :loading="passwordLoading" @click="handleChangePassword">修改密码</el-button></el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getUserInfo, updateUserInfo, changePassword } from '@/api/user'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()
const saveLoading = ref(false)
const passwordLoading = ref(false)
const passwordFormRef = ref(null)

const userInfo = reactive({ id: null, username: '', realName: '', phone: '', email: '', roleName: '', createTime: '', avatar: '' })
const passwordForm = reactive({ oldPassword: '', newPassword: '', confirmPassword: '' })
const passwordRules = {
  oldPassword: [{ required: true, message: '请输入原密码', trigger: 'blur' }],
  newPassword: [{ required: true, message: '请输入新密码', trigger: 'blur' }, { min: 6, message: '密码长度不能少于6位', trigger: 'blur' }],
  confirmPassword: [{ required: true, message: '请确认密码', trigger: 'blur' }, { validator: (rule, value, callback) => { if (value !== passwordForm.newPassword) callback(new Error('两次密码不一致')); else callback() }, trigger: 'blur' }]
}

async function fetchUserInfo() {
  try { const res = await getUserInfo(); Object.assign(userInfo, res) } catch (error) { console.error(error) }
}

async function handleSaveInfo() {
  saveLoading.value = true
  try { await updateUserInfo(userInfo); ElMessage.success('保存成功'); userStore.setUserInfo(userInfo) } catch (error) { ElMessage.error('保存失败') }
  finally { saveLoading.value = false }
}

async function handleChangePassword() {
  const valid = await passwordFormRef.value.validate().catch(() => false)
  if (!valid) return
  passwordLoading.value = true
  try { await changePassword(passwordForm); ElMessage.success('密码修改成功，请重新登录'); passwordFormRef.value.resetFields() } catch (error) { ElMessage.error('密码修改失败') }
  finally { passwordLoading.value = false }
}

function beforeAvatarUpload(file) {
  const isImage = file.type.startsWith('image/')
  const isLt2M = file.size / 1024 / 1024 < 2
  if (!isImage) { ElMessage.error('只能上传图片文件!'); return false }
  if (!isLt2M) { ElMessage.error('图片大小不能超过 2MB!'); return false }
  return false
}

onMounted(() => { fetchUserInfo() })
</script>

<style scoped>
.card-header { display: flex; justify-content: space-between; align-items: center; }
.avatar-section { text-align: center; }
h4 { margin-bottom: 15px; color: #303133; }
</style>

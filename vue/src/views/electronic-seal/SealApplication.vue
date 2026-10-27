<template>
  <div class="page-container">
    <el-card>
      <template #header><div class="card-header"><span>使用申请</span></div></template>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="120px" style="max-width: 800px;">
        <el-form-item label="选择印章" prop="sealId">
          <el-select v-model="form.sealId" placeholder="请选择印章" style="width: 100%;">
            <el-option v-for="item in sealList" :key="item.id" :label="item.name" :value="item.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="使用目的" prop="purpose"><el-input v-model="form.purpose" placeholder="请输入使用目的" /></el-form-item>
        <el-form-item label="关联项目" prop="projectId">
          <el-select v-model="form.projectId" placeholder="请选择项目" style="width: 100%;" clearable>
            <el-option v-for="item in projectList" :key="item.id" :label="item.name" :value="item.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="使用时间" prop="useTime"><el-date-picker v-model="form.useTime" type="datetime" placeholder="选择使用时间" value-format="YYYY-MM-DD HH:mm:ss" style="width: 100%;" /></el-form-item>
        <el-form-item label="备注" prop="remark"><el-input v-model="form.remark" type="textarea" :rows="3" placeholder="请输入备注" /></el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="loading" @click="handleSubmit">提交申请</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>

      <el-divider />

      <h4>我的申请记录</h4>
      <el-table :data="myApplications" border stripe>
        <el-table-column prop="sealName" label="印章名称" width="150" />
        <el-table-column prop="purpose" label="使用目的" min-width="200" />
        <el-table-column prop="useTime" label="使用时间" width="160" />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }"><el-tag :type="getStatusType(row.status)">{{ getStatusText(row.status) }}</el-tag></template>
        </el-table-column>
        <el-table-column prop="createTime" label="申请时间" width="160" />
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getSealList, submitSealApplication, getMyApplications } from '@/api/electronicSeal'
import { getProjectList } from '@/api/project'

const formRef = ref(null)
const loading = ref(false)
const sealList = ref([])
const projectList = ref([])
const myApplications = ref([])

const form = reactive({ sealId: null, purpose: '', projectId: null, useTime: '', remark: '' })
const rules = { sealId: [{ required: true, message: '请选择印章', trigger: 'change' }], purpose: [{ required: true, message: '请输入使用目的', trigger: 'blur' }] }

async function fetchSealList() {
  try { const res = await getSealList(); sealList.value = res || [] } catch (error) { console.error(error) }
}

async function fetchProjectList() {
  try { const res = await getProjectList({ page: 1, pageSize: 100 }); projectList.value = res.list || res || [] } catch (error) { console.error(error) }
}

async function fetchMyApplications() {
  try { const res = await getMyApplications(); myApplications.value = res || [] } catch (error) { console.error(error) }
}

async function handleSubmit() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  loading.value = true
  try { await submitSealApplication(form); ElMessage.success('申请已提交'); handleReset(); fetchMyApplications() } catch (error) { ElMessage.error('提交失败') }
  finally { loading.value = false }
}

function handleReset() {
  formRef.value?.resetFields()
}

function getStatusType(status) {
  const map = { 'pending': 'warning', 'approved': 'success', 'rejected': 'danger' }
  return map[status] || 'info'
}

function getStatusText(status) {
  const map = { 'pending': '待审批', 'approved': '已通过', 'rejected': '已拒绝' }
  return map[status] || status
}

onMounted(() => { fetchSealList(); fetchProjectList(); fetchMyApplications() })
</script>

<style scoped>
.card-header { display: flex; justify-content: space-between; align-items: center; }
h4 { margin-bottom: 15px; color: #303133; }
</style>

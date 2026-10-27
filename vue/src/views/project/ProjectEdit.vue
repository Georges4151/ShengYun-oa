<template>
  <div class="page-container">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>编辑项目</span>
        </div>
      </template>

      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-width="120px"
        style="max-width: 800px;"
        v-loading="pageLoading"
      >
        <el-form-item label="项目名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入项目名称" />
        </el-form-item>
        <el-form-item label="客户名称" prop="clientName">
          <el-input v-model="form.clientName" placeholder="请输入客户名称" />
        </el-form-item>
        <el-form-item label="项目地点" prop="location">
          <el-input v-model="form.location" placeholder="请输入项目地点" />
        </el-form-item>
        <el-form-item label="项目经理" prop="manager">
          <el-input v-model="form.manager" placeholder="请输入项目经理" />
        </el-form-item>
        <el-form-item label="预算金额" prop="budget">
          <el-input-number v-model="form.budget" :min="0" :precision="2" style="width: 100%;" />
        </el-form-item>
        <el-form-item label="项目状态" prop="status">
          <el-select v-model="form.status" placeholder="请选择状态" style="width: 100%;">
            <el-option label="未开始" value="未开始" />
            <el-option label="进行中" value="进行中" />
            <el-option label="暂停" value="暂停" />
            <el-option label="已完成" value="已完成" />
          </el-select>
        </el-form-item>
        <el-form-item label="开始日期" prop="startDate">
          <el-date-picker
            v-model="form.startDate"
            type="date"
            placeholder="选择开始日期"
            value-format="YYYY-MM-DD"
            style="width: 100%;"
          />
        </el-form-item>
        <el-form-item label="结束日期" prop="endDate">
          <el-date-picker
            v-model="form.endDate"
            type="date"
            placeholder="选择结束日期"
            value-format="YYYY-MM-DD"
            style="width: 100%;"
          />
        </el-form-item>
        <el-form-item label="项目描述" prop="description">
          <el-input v-model="form.description" type="textarea" :rows="4" placeholder="请输入项目描述" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="loading" @click="handleSubmit">保存</el-button>
          <el-button @click="handleCancel">取消</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getProjectDetail, updateProject } from '@/api/project'

const router = useRouter()
const route = useRoute()

const formRef = ref(null)
const loading = ref(false)
const pageLoading = ref(false)

const form = reactive({
  id: null,
  name: '',
  clientName: '',
  location: '',
  manager: '',
  budget: 0,
  status: '未开始',
  startDate: '',
  endDate: '',
  description: ''
})

const rules = {
  name: [{ required: true, message: '请输入项目名称', trigger: 'blur' }],
  clientName: [{ required: true, message: '请输入客户名称', trigger: 'blur' }],
  status: [{ required: true, message: '请选择项目状态', trigger: 'change' }]
}

async function fetchData() {
  const id = route.params.id
  if (!id) return

  pageLoading.value = true
  try {
    const res = await getProjectDetail(id)
    Object.assign(form, res)
  } catch (error) {
    ElMessage.error('获取项目信息失败')
  } finally {
    pageLoading.value = false
  }
}

async function handleSubmit() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  loading.value = true
  try {
    await updateProject(form.id, form)
    ElMessage.success('保存成功')
    router.push('/project/list')
  } catch (error) {
    ElMessage.error('保存失败')
  } finally {
    loading.value = false
  }
}

function handleCancel() {
  router.push('/project/list')
}

onMounted(() => {
  fetchData()
})
</script>

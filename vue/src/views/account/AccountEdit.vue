<template>
  <div class="page-container">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>编辑账目</span>
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
        <el-form-item label="关联项目" prop="projectId">
          <el-select v-model="form.projectId" placeholder="请选择项目" style="width: 100%;">
            <el-option v-for="item in projectList" :key="item.id" :label="item.name" :value="item.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="账目类型" prop="type">
          <el-radio-group v-model="form.type">
            <el-radio label="收入">收入</el-radio>
            <el-radio label="支出">支出</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="金额" prop="amount">
          <el-input-number v-model="form.amount" :min="0" :precision="2" style="width: 100%;" />
        </el-form-item>
        <el-form-item label="分类" prop="category">
          <el-select v-model="form.category" placeholder="请选择分类" style="width: 100%;">
            <el-option-group v-if="form.type === '收入'" label="收入分类">
              <el-option label="工程款" value="工程款" />
              <el-option label="材料款" value="材料款" />
              <el-option label="其他收入" value="其他收入" />
            </el-option-group>
            <el-option-group v-else label="支出分类">
              <el-option label="人工费" value="人工费" />
              <el-option label="材料费" value="材料费" />
              <el-option label="设备费" value="设备费" />
              <el-option label="管理费" value="管理费" />
              <el-option label="其他支出" value="其他支出" />
            </el-option-group>
          </el-select>
        </el-form-item>
        <el-form-item label="交易日期" prop="transactionDate">
          <el-date-picker
            v-model="form.transactionDate"
            type="date"
            placeholder="选择交易日期"
            value-format="YYYY-MM-DD"
            style="width: 100%;"
          />
        </el-form-item>
        <el-form-item label="描述" prop="description">
          <el-input v-model="form.description" type="textarea" :rows="3" placeholder="请输入描述" />
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
import { getAccountDetail, updateAccount } from '@/api/account'
import { getProjectList } from '@/api/project'

const router = useRouter()
const route = useRoute()

const formRef = ref(null)
const loading = ref(false)
const pageLoading = ref(false)
const projectList = ref([])

const form = reactive({
  id: null,
  projectId: null,
  type: '支出',
  amount: 0,
  category: '',
  transactionDate: '',
  description: ''
})

const rules = {
  type: [{ required: true, message: '请选择账目类型', trigger: 'change' }],
  amount: [{ required: true, message: '请输入金额', trigger: 'blur' }],
  category: [{ required: true, message: '请选择分类', trigger: 'change' }],
  transactionDate: [{ required: true, message: '请选择交易日期', trigger: 'change' }]
}

async function fetchProjectList() {
  try {
    const res = await getProjectList({ page: 1, pageSize: 100 })
    projectList.value = res.list || res || []
  } catch (error) {
    console.error('获取项目列表失败:', error)
  }
}

async function fetchData() {
  const id = route.params.id
  if (!id) return

  pageLoading.value = true
  try {
    const res = await getAccountDetail(id)
    Object.assign(form, res)
  } catch (error) {
    ElMessage.error('获取账目信息失败')
  } finally {
    pageLoading.value = false
  }
}

async function handleSubmit() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  loading.value = true
  try {
    await updateAccount(form.id, form)
    ElMessage.success('保存成功')
    router.push('/account/list')
  } catch (error) {
    ElMessage.error('保存失败')
  } finally {
    loading.value = false
  }
}

function handleCancel() {
  router.push('/account/list')
}

onMounted(() => {
  fetchProjectList()
  fetchData()
})
</script>

<template>
  <div class="page-container">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>通知列表</span>
          <el-button type="primary" @click="handleSend"><el-icon><Plus /></el-icon>发送通知</el-button>
        </div>
      </template>
      <el-tabs v-model="activeTab" @tab-change="fetchData">
        <el-tab-pane label="全部" name="all" />
        <el-tab-pane label="未读" name="unread" />
        <el-tab-pane label="已读" name="read" />
      </el-tabs>
      <el-table v-loading="loading" :data="tableData" border stripe>
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="title" label="标题" min-width="200" show-overflow-tooltip />
        <el-table-column prop="type" label="类型" width="100">
          <template #default="{ row }"><el-tag :type="getTypeColor(row.type)">{{ row.type }}</el-tag></template>
        </el-table-column>
        <el-table-column prop="senderName" label="发送人" width="100" />
        <el-table-column prop="createTime" label="发送时间" width="160" />
        <el-table-column prop="isRead" label="状态" width="80">
          <template #default="{ row }"><el-tag :type="row.isRead ? 'info' : 'danger'">{{ row.isRead ? '已读' : '未读' }}</el-tag></template>
        </el-table-column>
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="handleView(row)">查看</el-button>
            <el-button type="danger" link @click="handleDelete(row)" v-if="!row.isRead">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div class="pagination-container">
        <el-pagination v-model:current-page="queryParams.page" v-model:page-size="queryParams.pageSize" :total="total" layout="total, sizes, prev, pager, next" @size-change="fetchData" @current-change="fetchData" />
      </div>
    </el-card>

    <el-dialog v-model="detailVisible" title="通知详情" width="600px">
      <el-descriptions :column="1" border>
        <el-descriptions-item label="标题">{{ currentNotification.title }}</el-descriptions-item>
        <el-descriptions-item label="类型">{{ currentNotification.type }}</el-descriptions-item>
        <el-descriptions-item label="发送人">{{ currentNotification.senderName }}</el-descriptions-item>
        <el-descriptions-item label="发送时间">{{ currentNotification.createTime }}</el-descriptions-item>
        <el-descriptions-item label="内容">{{ currentNotification.content }}</el-descriptions-item>
      </el-descriptions>
    </el-dialog>

    <el-dialog v-model="sendVisible" title="发送通知" width="500px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="标题" prop="title"><el-input v-model="form.title" placeholder="请输入标题" /></el-form-item>
        <el-form-item label="类型" prop="type">
          <el-select v-model="form.type" style="width: 100%;">
            <el-option label="系统通知" value="系统通知" />
            <el-option label="审批提醒" value="审批提醒" />
            <el-option label="任务提醒" value="任务提醒" />
            <el-option label="其他" value="其他" />
          </el-select>
        </el-form-item>
        <el-form-item label="接收人" prop="receiverIds">
          <el-select v-model="form.receiverIds" multiple placeholder="请选择接收人" style="width: 100%;">
            <el-option v-for="item in userList" :key="item.id" :label="item.realName" :value="item.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="内容" prop="content"><el-input v-model="form.content" type="textarea" :rows="4" placeholder="请输入内容" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="sendVisible = false">取消</el-button>
        <el-button type="primary" :loading="sendLoading" @click="handleSendSubmit">发送</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getNotificationList, markAsRead, deleteNotification, sendNotification } from '@/api/notification'
import { getUserList } from '@/api/user'

const loading = ref(false)
const tableData = ref([])
const total = ref(0)
const activeTab = ref('all')
const detailVisible = ref(false)
const sendVisible = ref(false)
const sendLoading = ref(false)
const userList = ref([])
const currentNotification = ref({})

const queryParams = reactive({ page: 1, pageSize: 10, status: '' })
const formRef = ref(null)
const form = reactive({ title: '', type: '系统通知', receiverIds: [], content: '' })
const rules = { title: [{ required: true, message: '请输入标题', trigger: 'blur' }], content: [{ required: true, message: '请输入内容', trigger: 'blur' }] }

async function fetchUserList() {
  try { const res = await getUserList({ page: 1, pageSize: 100 }); userList.value = res.list || res || [] } catch (error) { console.error(error) }
}

async function fetchData() {
  if (activeTab.value === 'unread') queryParams.status = 'unread'
  else if (activeTab.value === 'read') queryParams.status = 'read'
  else queryParams.status = ''

  loading.value = true
  try { const res = await getNotificationList(queryParams); tableData.value = res.list || res || []; total.value = res.total || tableData.value.length } catch (error) { console.error(error) }
  finally { loading.value = false }
}

async function handleView(row) {
  currentNotification.value = row
  detailVisible.value = true
  if (!row.isRead) {
    try { await markAsRead(row.id); row.isRead = true } catch (error) { console.error(error) }
  }
}

async function handleDelete(row) {
  try {
    await ElMessageBox.confirm('确定删除该通知？', '提示', { type: 'warning' })
    await deleteNotification(row.id)
    ElMessage.success('删除成功')
    fetchData()
  } catch (error) { if (error !== 'cancel') ElMessage.error('删除失败') }
}

function handleSend() {
  Object.assign(form, { title: '', type: '系统通知', receiverIds: [], content: '' })
  sendVisible.value = true
}

async function handleSendSubmit() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  sendLoading.value = true
  try { await sendNotification(form); ElMessage.success('发送成功'); sendVisible.value = false } catch (error) { ElMessage.error('发送失败') }
  finally { sendLoading.value = false }
}

function getTypeColor(type) {
  const map = { '系统通知': 'primary', '审批提醒': 'warning', '任务提醒': 'success', '其他': 'info' }
  return map[type] || 'info'
}

onMounted(() => { fetchUserList(); fetchData() })
</script>

<style scoped>
.card-header { display: flex; justify-content: space-between; align-items: center; }
.pagination-container { margin-top: 20px; display: flex; justify-content: flex-end; }
</style>

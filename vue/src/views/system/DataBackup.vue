<template>
  <div class="page-container">
    <el-card>
      <template #header><div class="card-header"><span>数据备份</span></div></template>
      <el-alert title="数据备份功能可帮助您定期备份系统数据，防止数据丢失" type="info" :closable="false" style="margin-bottom: 20px;" />
      <el-button type="primary" :loading="backupLoading" @click="handleBackup"><el-icon><Download /></el-icon>立即备份</el-button>
      <el-divider />
      <h4>备份记录</h4>
      <el-table :data="backupList" border stripe>
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="fileName" label="文件名" min-width="200" />
        <el-table-column prop="fileSize" label="文件大小" width="120" />
        <el-table-column prop="createTime" label="备份时间" width="160" />
        <el-table-column prop="operator" label="操作人" width="100" />
        <el-table-column label="操作" width="150">
          <template #default="{ row }">
            <el-button type="primary" link @click="handleDownload(row)">下载</el-button>
            <el-button type="danger" link @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { backupData, getBackupList, deleteBackup } from '@/api/system'

const backupLoading = ref(false)
const backupList = ref([])

async function fetchBackupList() {
  try { const res = await getBackupList(); backupList.value = res || [] } catch (error) { console.error(error) }
}

async function handleBackup() {
  backupLoading.value = true
  try { await backupData(); ElMessage.success('备份成功'); fetchBackupList() } catch (error) { ElMessage.error('备份失败') }
  finally { backupLoading.value = false }
}

function handleDownload(row) {
  ElMessage.info('开始下载: ' + row.fileName)
}

async function handleDelete(row) {
  try {
    await ElMessageBox.confirm('确定删除该备份文件？', '提示', { type: 'warning' })
    await deleteBackup(row.id)
    ElMessage.success('删除成功')
    fetchBackupList()
  } catch (error) { if (error !== 'cancel') ElMessage.error('删除失败') }
}

onMounted(() => { fetchBackupList() })
</script>

<style scoped>
.card-header { display: flex; justify-content: space-between; align-items: center; }
h4 { margin-bottom: 15px; color: #303133; }
</style>

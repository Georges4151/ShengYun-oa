<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import {
  ElButton,
  ElDescriptions,
  ElDescriptionsItem,
  ElDialog,
  ElForm,
  ElFormItem,
  ElInput,
  ElMessage,
  ElMessageBox,
  ElOption,
  ElPagination,
  ElSelect,
  ElTable,
  ElTableColumn,
  ElTag
} from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { ceamsApi } from '@/services/http'
import type { Platform, UserDeviceVO } from '@ceams/types'
import { usePage } from '@/composables/usePage'
import { apiErrorMessage } from '@/utils/error'
import { deviceStatusLabels, platformLabels, toOptions } from '@/utils/labels'

defineOptions({ name: 'DeviceList' })

const platformOptions = toOptions(platformLabels)

const { page, size, total, resetPage } = usePage()
const list = ref<UserDeviceVO[]>([])
const loading = ref(false)

const dialogVisible = ref(false)
const detailVisible = ref(false)
const detail = ref<UserDeviceVO | null>(null)
const formRef = ref<FormInstance>()
const form = reactive<{
  deviceId: string
  deviceName: string
  platform?: Platform
}>({
  deviceId: '',
  deviceName: '',
  platform: undefined
})

const rules: FormRules<typeof form> = {
  deviceId: [{ required: true, message: '请输入设备指纹', trigger: 'blur' }]
}

async function load(): Promise<void> {
  loading.value = true
  try {
    const res = await ceamsApi.iam.devicePage({ page: page.value, size: size.value })
    list.value = res.records
    total.value = res.total
  } catch (err) {
    ElMessage.error(apiErrorMessage(err, '加载设备失败'))
  } finally {
    loading.value = false
  }
}

function onSearch(): void {
  resetPage()
  void load()
}

function openCreate(): void {
  Object.assign(form, { deviceId: '', deviceName: '', platform: undefined })
  dialogVisible.value = true
}

function openDetail(row: UserDeviceVO): void {
  detail.value = row
  detailVisible.value = true
}

async function submit(): Promise<void> {
  if (!formRef.value) return
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  try {
    await ceamsApi.iam.deviceBind({
      deviceId: form.deviceId,
      deviceName: form.deviceName || undefined,
      platform: form.platform
    })
    ElMessage.success('设备绑定成功')
    dialogVisible.value = false
    await load()
  } catch (err) {
    ElMessage.error(apiErrorMessage(err, '设备绑定失败'))
  }
}

async function onRemove(row: UserDeviceVO): Promise<void> {
  try {
    await ElMessageBox.confirm(`确定解绑设备「${row.deviceName}」吗？`, '提示', { type: 'warning' })
  } catch {
    return
  }
  try {
    await ceamsApi.iam.deviceRemove(row.id)
    ElMessage.success('解绑成功')
    await load()
  } catch (err) {
    ElMessage.error(apiErrorMessage(err, '解绑失败'))
  }
}

onMounted(load)
</script>

<template>
  <div>
    <div class="toolbar">
      <el-button type="primary" @click="openCreate">绑定设备</el-button>
    </div>

    <el-table v-loading="loading" :data="list" border>
      <el-table-column prop="deviceName" label="设备名称" min-width="160" />
      <el-table-column prop="deviceId" label="设备指纹" min-width="200" show-overflow-tooltip />
      <el-table-column prop="userId" label="用户 ID" width="180" />
      <el-table-column label="平台" width="110">
        <template #default="{ row }">
          {{ row.platform ? platformLabels[(row as UserDeviceVO).platform!] : '-' }}
        </template>
      </el-table-column>
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <el-tag size="small">{{ deviceStatusLabels[(row as UserDeviceVO).status] }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="lastLoginIp" label="最近登录 IP" width="140" />
      <el-table-column prop="lastLoginTime" label="最近登录时间" width="180" />
      <el-table-column label="操作" width="160" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" size="small" @click="openDetail(row as UserDeviceVO)">详情</el-button>
          <el-button link type="danger" size="small" @click="onRemove(row as UserDeviceVO)">解绑</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-pagination
      v-model:current-page="page"
      v-model:page-size="size"
      :total="total"
      :page-sizes="[10, 20, 50, 100]"
      layout="total, sizes, prev, pager, next, jumper"
      class="pagination"
      @size-change="onSearch"
      @current-change="load"
    />

    <el-dialog v-model="dialogVisible" title="绑定设备" width="480px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="设备指纹" prop="deviceId">
          <el-input v-model="form.deviceId" placeholder="请输入设备指纹" />
        </el-form-item>
        <el-form-item label="设备名称">
          <el-input v-model="form.deviceName" placeholder="设备名称（可选）" />
        </el-form-item>
        <el-form-item label="平台">
          <el-select v-model="form.platform" clearable placeholder="请选择" style="width: 100%">
            <el-option v-for="opt in platformOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submit">确定</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="detailVisible" title="设备详情" width="480px">
      <el-descriptions v-if="detail" :column="1" border>
        <el-descriptions-item label="设备名称">{{ detail.deviceName }}</el-descriptions-item>
        <el-descriptions-item label="设备指纹">{{ detail.deviceId ?? '-' }}</el-descriptions-item>
        <el-descriptions-item label="用户 ID">{{ detail.userId }}</el-descriptions-item>
        <el-descriptions-item label="平台">{{ detail.platform ? platformLabels[detail.platform] : '-' }}</el-descriptions-item>
        <el-descriptions-item label="状态">{{ deviceStatusLabels[detail.status] }}</el-descriptions-item>
        <el-descriptions-item label="最近登录 IP">{{ detail.lastLoginIp ?? '-' }}</el-descriptions-item>
        <el-descriptions-item label="绑定时间">{{ detail.bindTime ?? '-' }}</el-descriptions-item>
        <el-descriptions-item label="最近登录时间">{{ detail.lastLoginTime ?? '-' }}</el-descriptions-item>
      </el-descriptions>
    </el-dialog>
  </div>
</template>

<style scoped>
.toolbar {
  display: flex;
  gap: 8px;
  margin-bottom: 12px;
}
.pagination {
  margin-top: 12px;
  justify-content: flex-end;
}
</style>

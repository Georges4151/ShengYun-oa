<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import {
  ElAlert,
  ElButton,
  ElDatePicker,
  ElDialog,
  ElForm,
  ElFormItem,
  ElInput,
  ElInputNumber,
  ElMessage,
  ElOption,
  ElSelect,
  ElTable,
  ElTableColumn,
  ElTag,
  ElTimePicker,
  ElTimeline,
  ElTimelineItem
} from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { formatDate, formatDateTime } from '@ceams/utils'
import { ceamsApi } from '@/services/http'
import type {
  EmployeeVO,
  Id,
  MeetingApplyVO,
  MeetingApplyStatus,
  MeetingAvailabilityVO,
  MeetingRoomVO
} from '@ceams/types'
import { hasPermission } from '@/composables/usePermission'
import { apiErrorMessage } from '@/utils/error'
import { meetingApplyStatusLabels, meetingRoomStatusLabels } from '@/utils/labels'

defineOptions({ name: 'MeetingPanel' })

const canManage = computed(() => hasPermission('admin:meeting:manage'))

function applyTagType(status: MeetingApplyStatus): 'success' | 'warning' | 'danger' | 'info' {
  switch (status) {
    case 'APPROVED':
      return 'success'
    case 'APPROVING':
      return 'warning'
    case 'REJECTED':
      return 'danger'
    default:
      return 'info'
  }
}

function timeRangeText(start: string, end: string): string {
  return `${start.split('T')[1]?.slice(0, 5) ?? start} - ${end.split('T')[1]?.slice(0, 5) ?? end}`
}

// ---------- 会议室列表 ----------
const rooms = ref<MeetingRoomVO[]>([])
const roomsLoading = ref(false)
const employees = ref<EmployeeVO[]>([])

async function loadRooms(): Promise<void> {
  roomsLoading.value = true
  try {
    rooms.value = await ceamsApi.admin.meetingRoomList()
  } catch (err) {
    ElMessage.error(apiErrorMessage(err, '加载会议室列表失败'))
  } finally {
    roomsLoading.value = false
  }
}

async function loadEmployees(): Promise<void> {
  try {
    const res = await ceamsApi.hr.employeePage({ page: 1, size: 200 })
    employees.value = res.records
  } catch {
    // 参会人下拉加载失败不阻断会议室
  }
}

// ---------- 新建 / 编辑会议室 ----------
const roomFormVisible = ref(false)
const roomFormRef = ref<FormInstance>()
const editingRoomId = ref<Id | null>(null)
const roomForm = reactive({ name: '', location: '', capacity: 8, equipment: '' })
const roomFormRules: FormRules<typeof roomForm> = {
  name: [{ required: true, message: '请输入会议室名称', trigger: 'blur' }]
}

function openRoomCreate(): void {
  editingRoomId.value = null
  Object.assign(roomForm, { name: '', location: '', capacity: 8, equipment: '' })
  roomFormVisible.value = true
}

function openRoomEdit(row: MeetingRoomVO): void {
  editingRoomId.value = row.id
  Object.assign(roomForm, {
    name: row.name,
    location: row.location ?? '',
    capacity: row.capacity ?? 0,
    equipment: row.equipment ?? ''
  })
  roomFormVisible.value = true
}

async function submitRoom(): Promise<void> {
  if (!roomFormRef.value) return
  const valid = await roomFormRef.value.validate().catch(() => false)
  if (!valid) return
  try {
    if (editingRoomId.value) {
      await ceamsApi.admin.meetingRoomUpdate(editingRoomId.value, {
        name: roomForm.name.trim(),
        location: roomForm.location.trim() || undefined,
        capacity: roomForm.capacity || undefined,
        equipment: roomForm.equipment.trim() || undefined
      })
      ElMessage.success('会议室已更新')
    } else {
      await ceamsApi.admin.meetingRoomCreate({
        name: roomForm.name.trim(),
        location: roomForm.location.trim() || undefined,
        capacity: roomForm.capacity || undefined,
        equipment: roomForm.equipment.trim() || undefined
      })
      ElMessage.success('会议室已创建')
    }
    roomFormVisible.value = false
    await loadRooms()
    await loadAvailability()
  } catch (err) {
    ElMessage.error(apiErrorMessage(err, '保存会议室失败'))
  }
}

// ---------- 会议室预定 ----------
const applyVisible = ref(false)
const applyFormRef = ref<FormInstance>()
const applyForm = reactive({
  roomId: '',
  subject: '',
  date: formatDate(new Date(), ''),
  startTime: '09:00:00',
  endTime: '10:00:00',
  attendeeIds: [] as Id[]
})
const applyRules: FormRules<typeof applyForm> = {
  roomId: [{ required: true, message: '请选择会议室', trigger: 'change' }],
  subject: [{ required: true, message: '请输入会议主题', trigger: 'blur' }],
  date: [{ required: true, message: '请选择会议日期', trigger: 'change' }]
}

function openApply(row?: MeetingRoomVO): void {
  Object.assign(applyForm, {
    roomId: row?.id ?? '',
    subject: '',
    date: formatDate(new Date(), ''),
    startTime: '09:00:00',
    endTime: '10:00:00',
    attendeeIds: []
  })
  applyVisible.value = true
}

async function submitApply(): Promise<void> {
  if (!applyFormRef.value) return
  const valid = await applyFormRef.value.validate().catch(() => false)
  if (!valid) return
  if (applyForm.startTime >= applyForm.endTime) {
    ElMessage.warning('结束时间必须晚于开始时间')
    return
  }
  try {
    await ceamsApi.admin.meetingApply({
      roomId: applyForm.roomId,
      subject: applyForm.subject.trim(),
      startTime: `${applyForm.date}T${applyForm.startTime}`,
      endTime: `${applyForm.date}T${applyForm.endTime}`,
      attendeeIds: applyForm.attendeeIds.length > 0 ? applyForm.attendeeIds : undefined
    })
    ElMessage.success('会议室预定已提交，请在待办中心跟踪审批')
    applyVisible.value = false
    await Promise.all([loadMyApplies(), loadAvailability()])
  } catch (err) {
    ElMessage.error(apiErrorMessage(err, '提交会议室预定失败'))
  }
}

// ---------- 我的预定 ----------
const myApplies = ref<MeetingApplyVO[]>([])
const myLoading = ref(false)

async function loadMyApplies(): Promise<void> {
  myLoading.value = true
  try {
    myApplies.value = await ceamsApi.admin.myMeetingApplies()
  } catch (err) {
    ElMessage.error(apiErrorMessage(err, '加载我的预定失败'))
  } finally {
    myLoading.value = false
  }
}

// ---------- 当日占用 ----------
const availabilityQuery = reactive({ roomId: '', date: formatDate(new Date(), '') })
const availability = ref<MeetingAvailabilityVO | null>(null)
const availabilityLoading = ref(false)

async function loadAvailability(): Promise<void> {
  if (!availabilityQuery.roomId || !availabilityQuery.date) {
    availability.value = null
    return
  }
  availabilityLoading.value = true
  try {
    availability.value = await ceamsApi.admin.meetingAvailability(
      availabilityQuery.roomId,
      availabilityQuery.date
    )
  } catch (err) {
    ElMessage.error(apiErrorMessage(err, '加载会议室占用情况失败'))
  } finally {
    availabilityLoading.value = false
  }
}

onMounted(() => {
  void loadRooms()
  void loadEmployees()
  void loadMyApplies()
})
</script>

<template>
  <div>
    <!-- 会议室列表 -->
    <div class="section">
      <div class="section-title">会议室</div>
      <div class="toolbar">
        <el-button type="primary" @click="openApply()">预定会议室</el-button>
        <template v-if="canManage">
          <el-button @click="openRoomCreate">新增会议室</el-button>
        </template>
      </div>
      <el-table v-loading="roomsLoading" :data="rooms" border>
        <el-table-column prop="name" label="会议室名称" min-width="160" />
        <el-table-column prop="location" label="位置" min-width="140">
          <template #default="{ row }">{{ (row as MeetingRoomVO).location ?? '-' }}</template>
        </el-table-column>
        <el-table-column prop="capacity" label="容纳人数" width="100" align="right">
          <template #default="{ row }">{{ (row as MeetingRoomVO).capacity ?? '-' }}</template>
        </el-table-column>
        <el-table-column prop="equipment" label="设备" min-width="180">
          <template #default="{ row }">{{ (row as MeetingRoomVO).equipment ?? '-' }}</template>
        </el-table-column>
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag size="small" :type="(row as MeetingRoomVO).status === 'ENABLED' ? 'success' : 'info'">
              {{ meetingRoomStatusLabels[(row as MeetingRoomVO).status] }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="160" fixed="right">
          <template #default="{ row }">
            <el-button
              link
              type="primary"
              size="small"
              :disabled="(row as MeetingRoomVO).status !== 'ENABLED'"
              @click="openApply(row as MeetingRoomVO)"
            >
              预定
            </el-button>
            <el-button
              v-if="canManage"
              link
              type="primary"
              size="small"
              @click="openRoomEdit(row as MeetingRoomVO)"
            >
              编辑
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <!-- 我的预定 -->
    <div class="section">
      <div class="section-title">我的预定</div>
      <el-table v-loading="myLoading" :data="myApplies" border>
        <el-table-column prop="roomName" label="会议室" width="150">
          <template #default="{ row }">{{ (row as MeetingApplyVO).roomName ?? '-' }}</template>
        </el-table-column>
        <el-table-column prop="subject" label="会议主题" min-width="160" />
        <el-table-column label="会议时间" min-width="220">
          <template #default="{ row }">
            {{ formatDateTime((row as MeetingApplyVO).startTime) }} 至
            {{ formatDateTime((row as MeetingApplyVO).endTime) }}
          </template>
        </el-table-column>
        <el-table-column label="参会人数" width="100" align="right">
          <template #default="{ row }">{{ (row as MeetingApplyVO).attendeeIds?.length ?? 0 }}</template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag size="small" :type="applyTagType((row as MeetingApplyVO).status)">
              {{ meetingApplyStatusLabels[(row as MeetingApplyVO).status] }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="提交时间" width="170">
          <template #default="{ row }">{{ formatDateTime((row as MeetingApplyVO).createTime) }}</template>
        </el-table-column>
      </el-table>
    </div>

    <!-- 当日占用时间轴 -->
    <div class="section">
      <div class="section-title">当日占用情况</div>
      <div class="toolbar">
        <el-select
          v-model="availabilityQuery.roomId"
          placeholder="选择会议室"
          style="width: 200px"
          @change="loadAvailability"
        >
          <el-option v-for="item in rooms" :key="item.id" :label="item.name" :value="item.id" />
        </el-select>
        <el-date-picker
          v-model="availabilityQuery.date"
          type="date"
          value-format="YYYY-MM-DD"
          style="width: 160px"
          @change="loadAvailability"
        />
      </div>
      <template v-if="availabilityQuery.roomId">
        <el-alert
          v-if="availability && availability.intervals.length === 0"
          title="当日该会议室暂无占用"
          type="success"
          :closable="false"
          show-icon
        />
        <el-timeline v-else v-loading="availabilityLoading" class="timeline">
          <el-timeline-item
            v-for="(item, index) in availability?.intervals ?? []"
            :key="index"
            :timestamp="timeRangeText(item.startTime, item.endTime)"
            color="#dc2626"
          >
            <span class="occupied">{{ item.subject || '已占用' }}</span>
          </el-timeline-item>
        </el-timeline>
      </template>
      <el-alert v-else title="请选择会议室查看占用情况" type="info" :closable="false" show-icon />
    </div>

    <!-- 新建 / 编辑会议室 -->
    <el-dialog
      v-model="roomFormVisible"
      :title="editingRoomId ? '编辑会议室' : '新增会议室'"
      width="520px"
    >
      <el-form ref="roomFormRef" :model="roomForm" :rules="roomFormRules" label-width="100px">
        <el-form-item label="名称" prop="name">
          <el-input v-model="roomForm.name" placeholder="请输入会议室名称" />
        </el-form-item>
        <el-form-item label="位置">
          <el-input v-model="roomForm.location" placeholder="位置（可选）" />
        </el-form-item>
        <el-form-item label="容纳人数">
          <el-input-number v-model="roomForm.capacity" :min="1" style="width: 100%" />
        </el-form-item>
        <el-form-item label="设备">
          <el-input v-model="roomForm.equipment" placeholder="投影仪、白板等（可选）" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="roomFormVisible = false">取消</el-button>
        <el-button type="primary" @click="submitRoom">确定</el-button>
      </template>
    </el-dialog>

    <!-- 会议室预定 -->
    <el-dialog v-model="applyVisible" title="会议室预定" width="560px">
      <el-form ref="applyFormRef" :model="applyForm" :rules="applyRules" label-width="100px">
        <el-form-item label="会议室" prop="roomId">
          <el-select v-model="applyForm.roomId" placeholder="请选择会议室" style="width: 100%">
            <el-option
              v-for="item in rooms"
              :key="item.id"
              :label="`${item.name}（可容纳 ${item.capacity ?? '-'} 人）`"
              :value="item.id"
              :disabled="item.status !== 'ENABLED'"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="会议主题" prop="subject">
          <el-input v-model="applyForm.subject" placeholder="请输入会议主题" />
        </el-form-item>
        <el-form-item label="会议日期" prop="date">
          <el-date-picker
            v-model="applyForm.date"
            type="date"
            value-format="YYYY-MM-DD"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="开始时间">
          <el-time-picker
            v-model="applyForm.startTime"
            value-format="HH:mm:ss"
            format="HH:mm"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="结束时间">
          <el-time-picker
            v-model="applyForm.endTime"
            value-format="HH:mm:ss"
            format="HH:mm"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="参会人">
          <el-select
            v-model="applyForm.attendeeIds"
            multiple
            filterable
            placeholder="请选择参会人"
            style="width: 100%"
          >
            <el-option
              v-for="item in employees"
              :key="item.id"
              :label="`${item.name}（${item.employeeNo}）`"
              :value="item.userId ?? item.id"
            />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="applyVisible = false">取消</el-button>
        <el-button type="primary" @click="submitApply">提交预定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.section {
  margin-bottom: 24px;
}
.section-title {
  margin-bottom: 10px;
  font-size: 15px;
  font-weight: 600;
  color: #111827;
}
.toolbar {
  display: flex;
  gap: 8px;
  margin-bottom: 12px;
}
.timeline {
  padding-left: 4px;
}
.occupied {
  color: #dc2626;
  font-weight: 600;
}
</style>

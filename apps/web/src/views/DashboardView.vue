<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElTag } from 'element-plus'
import type { TaskVO } from '@ceams/types'
import AppIcon from '@/components/AppIcon.vue'
import { ceamsApi } from '@/services/http'
import { useAuthStore } from '@/stores/auth'
import { bpmBizTypeLabels, bpmTaskStatusLabels } from '@/utils/labels'

defineOptions({ name: 'DashboardView' })

const router = useRouter()
const auth = useAuthStore()

const displayName = computed(() => auth.name || auth.username || '同事')

/** 时段问候：5—11 点上午，12—17 点下午，其余晚上。 */
const greeting = computed(() => {
  const hour = new Date().getHours()
  if (hour >= 5 && hour < 12) return '上午好'
  if (hour >= 12 && hour < 18) return '下午好'
  return '晚上好'
})

const WEEKDAYS = ['周日', '周一', '周二', '周三', '周四', '周五', '周六']

/** 今日日期文案，如「2026年11月19日 周四」。 */
const todayText = computed(() => {
  const now = new Date()
  return `${now.getFullYear()}年${now.getMonth() + 1}月${now.getDate()}日 ${WEEKDAYS[now.getDay()]}`
})

// ---------------- 指标卡 ----------------

type StatKey = 'pending' | 'overdue' | 'unread' | 'initiated'

interface StatCard {
  key: StatKey
  label: string
  icon: string
  path: string
  /** 图标块前景色 */
  color: string
  /** 图标块底色 */
  background: string
}

const STAT_CARDS: StatCard[] = [
  { key: 'pending', label: '我的待办', icon: 'todo', path: '/todo', color: '#2563eb', background: '#eff6ff' },
  { key: 'overdue', label: '超时待办', icon: 'clock', path: '/todo', color: '#dc2626', background: '#fef2f2' },
  {
    key: 'unread',
    label: '未读通知',
    icon: 'notification',
    path: '/notification',
    color: '#d97706',
    background: '#fffbeb'
  },
  {
    key: 'initiated',
    label: '我发起的在办',
    icon: 'inbox',
    path: '/todo?tab=initiated',
    color: '#0f766e',
    background: '#f0fdfa'
  }
]

const stats = ref<Record<StatKey, number>>({ pending: 0, overdue: 0, unread: 0, initiated: 0 })

// ---------------- 快捷入口 ----------------

interface Shortcut {
  label: string
  icon: string
  path: string
}

const SHORTCUTS: Shortcut[] = [
  { label: '发起报销', icon: 'expense', path: '/expense' },
  { label: '项目合同', icon: 'project', path: '/project' },
  { label: '我的待办', icon: 'todo', path: '/todo' },
  { label: '文档知识库', icon: 'doc', path: '/doc' },
  { label: '资金台账', icon: 'payment', path: '/payment' },
  { label: '报表分析', icon: 'report', path: '/report' }
]

// ---------------- 最近待办 ----------------

const todos = ref<TaskVO[]>([])
const loading = ref(false)

/**
 * 并发拉取工作台数据：单个接口失败只影响对应卡片，不打断其他区域，也不弹错误提示。
 */
async function loadDashboard(): Promise<void> {
  loading.value = true
  const [pendingRes, overdueRes, initiatedRes, unreadRes] = await Promise.allSettled([
    ceamsApi.bpm.pendingTasks({ page: 1, size: 5 }),
    ceamsApi.bpm.pendingTasks({ page: 1, size: 1, overdue: true }),
    ceamsApi.bpm.initiated({ page: 1, size: 1, status: 'RUNNING' }),
    ceamsApi.notification.unreadCount()
  ])

  if (pendingRes.status === 'fulfilled') {
    stats.value.pending = pendingRes.value.total
    todos.value = pendingRes.value.records
  }
  if (overdueRes.status === 'fulfilled') {
    stats.value.overdue = overdueRes.value.total
  }
  if (initiatedRes.status === 'fulfilled') {
    stats.value.initiated = initiatedRes.value.total
  }
  if (unreadRes.status === 'fulfilled') {
    stats.value.unread = Number(unreadRes.value) || 0
  }

  loading.value = false
}

onMounted(() => {
  void loadDashboard()
})

function go(path: string): void {
  void router.push(path)
}

/** 业务类型中文名，未知枚举回退原值。 */
function bizTypeText(value?: string | null): string {
  if (!value) return '审批流程'
  return bpmBizTypeLabels[value] ?? value
}

/** 任务状态中文名。 */
function statusText(value: string): string {
  return bpmTaskStatusLabels[value] ?? value
}

/** 状态标签色：超时优先红色，待处理蓝色。 */
function statusTagType(task: TaskVO): 'primary' | 'warning' | 'danger' | 'info' {
  if (task.overdue) return 'danger'
  if (task.status === 'PENDING') return 'primary'
  if (task.status === 'PENDING_ASSIGNEE') return 'warning'
  return 'info'
}

/** 后端时间字符串统一裁剪为「YYYY-MM-DD HH:mm」。 */
function formatTime(value?: string | null): string {
  if (!value) return '-'
  return value.replace('T', ' ').slice(0, 16)
}
</script>

<template>
  <div class="dashboard">
    <!-- 1. 欢迎区 -->
    <section class="welcome">
      <h1 class="welcome-title">{{ greeting }}，{{ displayName }}</h1>
      <p class="welcome-sub">{{ todayText }}，今天也请从容处理每一项待办。</p>
    </section>

    <!-- 2. 指标卡 -->
    <section class="stat-grid">
      <button
        v-for="card in STAT_CARDS"
        :key="card.key"
        type="button"
        class="stat-card"
        @click="go(card.path)"
      >
        <span class="stat-icon" :style="{ color: card.color, backgroundColor: card.background }">
          <AppIcon :name="card.icon" :size="20" />
        </span>
        <span class="stat-value">{{ stats[card.key] }}</span>
        <span class="stat-label">{{ card.label }}</span>
      </button>
    </section>

    <!-- 3. 快捷入口 -->
    <section class="panel">
      <header class="panel-head">
        <h2 class="panel-title">快捷入口</h2>
      </header>
      <div class="shortcut-grid">
        <button
          v-for="item in SHORTCUTS"
          :key="item.path + item.label"
          type="button"
          class="shortcut"
          @click="go(item.path)"
        >
          <span class="shortcut-icon">
            <AppIcon :name="item.icon" :size="20" />
          </span>
          <span class="shortcut-label">{{ item.label }}</span>
        </button>
      </div>
    </section>

    <!-- 4. 最近待办 -->
    <section class="panel">
      <header class="panel-head">
        <h2 class="panel-title">最近待办</h2>
        <button type="button" class="panel-more" @click="go('/todo')">查看全部</button>
      </header>

      <p v-if="loading && todos.length === 0" class="todo-empty">正在加载…</p>
      <p v-else-if="todos.length === 0" class="todo-empty">暂无待办</p>
      <ul v-else class="todo-list">
        <li v-for="task in todos" :key="task.id" class="todo-row" @click="go('/todo')">
          <span class="todo-biz">{{ bizTypeText(task.bizType) }}</span>
          <span class="todo-node" :title="task.currentNodeName || ''">{{ task.currentNodeName || '待审批' }}</span>
          <span class="todo-initiator">{{ task.initiatorName || '未知发起人' }}</span>
          <span class="todo-time">{{ formatTime(task.dueAt || task.createTime) }}</span>
          <el-tag size="small" :type="statusTagType(task)">{{ statusText(task.status) }}</el-tag>
        </li>
      </ul>
    </section>
  </div>
</template>

<style scoped>
.dashboard {
  display: flex;
  flex-direction: column;
  gap: var(--ceams-space-5);
}

/* ============ 欢迎区 ============ */
.welcome-title {
  font-size: var(--ceams-font-size-2xl);
  font-weight: 700;
  line-height: 1.35;
  color: var(--ceams-text-primary);
}

.welcome-sub {
  margin-top: 6px;
  font-size: var(--ceams-font-size-sm);
  color: var(--ceams-text-secondary);
}

/* ============ 指标卡 ============ */
.stat-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: var(--ceams-space-4);
}

.stat-card {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 4px;
  padding: 18px;
  font-family: inherit;
  text-align: left;
  background-color: var(--ceams-bg-card);
  border: 1px solid var(--ceams-border-base);
  border-radius: var(--ceams-radius-lg);
  box-shadow: var(--ceams-shadow-card);
  cursor: pointer;
  transition: transform 0.16s ease, box-shadow 0.16s ease;
}

.stat-card:hover {
  transform: translateY(-2px);
  box-shadow: var(--ceams-shadow-raised);
}

.stat-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  margin-bottom: 6px;
  border-radius: var(--ceams-radius-md);
}

.stat-value {
  font-size: var(--ceams-font-size-3xl);
  font-weight: 700;
  line-height: 1.15;
  color: var(--ceams-text-primary);
  font-variant-numeric: tabular-nums;
}

.stat-label {
  font-size: var(--ceams-font-size-sm);
  color: var(--ceams-text-secondary);
}

/* ============ 通用面板 ============ */
.panel {
  padding: 18px;
  background-color: var(--ceams-bg-card);
  border: 1px solid var(--ceams-border-base);
  border-radius: var(--ceams-radius-lg);
  box-shadow: var(--ceams-shadow-card);
}

.panel-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: var(--ceams-space-3);
}

.panel-title {
  font-size: var(--ceams-font-size-md);
  font-weight: 600;
  color: var(--ceams-text-primary);
}

.panel-more {
  padding: 0;
  font-family: inherit;
  font-size: var(--ceams-font-size-sm);
  color: var(--ceams-color-primary);
  background: none;
  border: none;
  cursor: pointer;
}

.panel-more:hover {
  color: var(--ceams-color-primary-dark);
}

/* ============ 快捷入口 ============ */
.shortcut-grid {
  display: grid;
  grid-template-columns: repeat(6, minmax(0, 1fr));
  gap: var(--ceams-space-3);
}

.shortcut {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10px;
  padding: 18px 10px;
  font-family: inherit;
  background-color: var(--ceams-bg-elevated);
  border: 1px solid var(--ceams-border-divider);
  border-radius: var(--ceams-radius-lg);
  cursor: pointer;
  transition: transform 0.16s ease, border-color 0.16s ease, box-shadow 0.16s ease,
    background-color 0.16s ease;
}

.shortcut:hover {
  transform: translateY(-2px);
  background-color: var(--ceams-bg-card);
  border-color: var(--ceams-color-primary-lighter);
  box-shadow: var(--ceams-shadow-card);
}

.shortcut-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 40px;
  height: 40px;
  color: var(--ceams-color-primary);
  background-color: var(--ceams-color-primary-light);
  border-radius: var(--ceams-radius-md);
}

.shortcut-label {
  font-size: var(--ceams-font-size-sm);
  color: var(--ceams-text-primary);
}

/* ============ 最近待办 ============ */
.todo-list {
  display: flex;
  flex-direction: column;
  margin: 0;
  padding: 0;
  list-style: none;
}

.todo-row {
  display: grid;
  grid-template-columns: 120px minmax(0, 1fr) 110px 140px 88px;
  align-items: center;
  gap: var(--ceams-space-4);
  padding: 12px 10px;
  border-bottom: 1px solid var(--ceams-border-divider);
  cursor: pointer;
  transition: background-color 0.16s ease;
}

.todo-row:last-child {
  border-bottom: none;
}

.todo-row:hover {
  background-color: var(--ceams-color-primary-light);
}

.todo-biz {
  overflow: hidden;
  font-size: var(--ceams-font-size-sm);
  font-weight: 500;
  color: var(--ceams-color-primary-dark);
  text-overflow: ellipsis;
  white-space: nowrap;
}

.todo-node {
  overflow: hidden;
  font-size: var(--ceams-font-size-base);
  color: var(--ceams-text-primary);
  text-overflow: ellipsis;
  white-space: nowrap;
}

.todo-initiator,
.todo-time {
  font-size: var(--ceams-font-size-sm);
  color: var(--ceams-text-secondary);
  font-variant-numeric: tabular-nums;
}

.todo-empty {
  padding: 28px 0;
  font-size: var(--ceams-font-size-sm);
  color: var(--ceams-text-placeholder);
  text-align: center;
}

/* ============ 响应式 ============ */
@media (max-width: 1200px) {
  .stat-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .shortcut-grid {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
}

@media (max-width: 720px) {
  .stat-grid {
    grid-template-columns: minmax(0, 1fr);
  }

  .shortcut-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .todo-row {
    grid-template-columns: 96px minmax(0, 1fr) 88px;
  }

  .todo-initiator,
  .todo-time {
    display: none;
  }
}
</style>

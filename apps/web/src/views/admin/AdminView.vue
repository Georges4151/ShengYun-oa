<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import type { Component } from 'vue'
import { ElEmpty, ElTabPane, ElTabs } from 'element-plus'
import { hasPermission } from '@/composables/usePermission'
import AssetPanel from './components/AssetPanel.vue'
import MaterialPanel from './components/MaterialPanel.vue'
import VehiclePanel from './components/VehiclePanel.vue'
import MeetingPanel from './components/MeetingPanel.vue'
import VisitorPanel from './components/VisitorPanel.vue'
import RepairPanel from './components/RepairPanel.vue'

defineOptions({ name: 'AdminView' })

interface AdminTab {
  name: string
  label: string
  permission: string
  component: Component
}

/** 六个页签与权限码一一对应，无权限的页签不渲染。 */
const ALL_TABS: AdminTab[] = [
  { name: 'asset', label: '资产台账', permission: 'admin:asset:view', component: AssetPanel },
  { name: 'material', label: '易耗品', permission: 'admin:material:manage', component: MaterialPanel },
  { name: 'vehicle', label: '车辆', permission: 'admin:vehicle:manage', component: VehiclePanel },
  { name: 'meeting', label: '会议室', permission: 'admin:meeting:manage', component: MeetingPanel },
  { name: 'visitor', label: '访客', permission: 'admin:visitor:manage', component: VisitorPanel },
  { name: 'repair', label: '报修', permission: 'admin:repair:manage', component: RepairPanel }
]

const visibleTabs = computed(() => ALL_TABS.filter((tab) => hasPermission(tab.permission)))
const activeTab = ref('')

watch(
  visibleTabs,
  (tabs) => {
    if (!tabs.some((tab) => tab.name === activeTab.value)) {
      activeTab.value = tabs[0]?.name ?? ''
    }
  },
  { immediate: true }
)
</script>

<template>
  <div class="admin-view">
    <!-- 页头：标题 + 灰色说明 -->
    <div class="page-header">
      <div>
        <h2 class="page-title">行政后勤</h2>
        <p class="page-subtitle">统一管理资产台账、易耗品、车辆、会议室、访客与报修事项</p>
      </div>
    </div>

    <div v-if="visibleTabs.length > 0" class="admin-tabs">
      <el-tabs v-model="activeTab">
        <el-tab-pane v-for="tab in visibleTabs" :key="tab.name" :label="tab.label" :name="tab.name">
          <component :is="tab.component" />
        </el-tab-pane>
      </el-tabs>
    </div>

    <!-- 空态：居中灰色、40px 留白 -->
    <div v-else class="empty-state">
      <el-empty description="暂无可访问的行政后勤页签" />
    </div>
  </div>
</template>

<style scoped>
.admin-view {
  display: flex;
  flex-direction: column;
}

/* 页签与内容保持 16px 间距 */
.admin-tabs :deep(.el-tabs__header) {
  margin-bottom: var(--ceams-space-4);
}

.admin-tabs :deep(.el-tabs__item) {
  font-size: var(--ceams-font-size-base);
  color: var(--ceams-text-secondary);
}

/* 选中页签加粗并使用主色 */
.admin-tabs :deep(.el-tabs__item.is-active) {
  font-weight: 600;
  color: var(--ceams-color-primary);
}

.admin-tabs :deep(.el-tabs__nav-wrap::after) {
  background-color: var(--ceams-border-divider);
}

.empty-state {
  padding: var(--ceams-space-10) 0;
}

.empty-state :deep(.el-empty__description p) {
  color: var(--ceams-text-placeholder);
}
</style>

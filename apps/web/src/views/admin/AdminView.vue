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
  <el-tabs v-if="visibleTabs.length > 0" v-model="activeTab">
    <el-tab-pane v-for="tab in visibleTabs" :key="tab.name" :label="tab.label" :name="tab.name">
      <component :is="tab.component" />
    </el-tab-pane>
  </el-tabs>
  <el-empty v-else description="暂无可访问的行政后勤页签" />
</template>

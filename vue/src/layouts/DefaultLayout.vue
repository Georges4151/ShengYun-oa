<template>
  <el-container class="layout-container">
    <el-aside :width="sidebarWidth" class="sidebar-container">
      <Sidebar />
    </el-aside>
    <el-container>
      <el-header class="header-container">
        <Header />
      </el-header>
      <el-main class="main-container">
        <Breadcrumb />
        <router-view v-slot="{ Component }">
          <transition name="fade" mode="out-in">
            <component :is="Component" />
          </transition>
        </router-view>
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { computed } from 'vue'
import { useAppStore } from '@/stores/app'
import Sidebar from '@/components/layout/Sidebar.vue'
import Header from '@/components/layout/Header.vue'
import Breadcrumb from '@/components/layout/Breadcrumb.vue'

const appStore = useAppStore()

const sidebarWidth = computed(() => 
  appStore.sidebarCollapsed ? '64px' : '210px'
)
</script>

<style scoped>
.layout-container {
  height: 100vh;
}

.sidebar-container {
  background-color: #304156;
  transition: width 0.3s;
  overflow: hidden;
}

.header-container {
  height: var(--header-height);
  padding: 0;
  background-color: #fff;
  box-shadow: 0 1px 4px rgba(0, 21, 41, 0.08);
  display: flex;
  align-items: center;
}

.main-container {
  padding: 0;
  background-color: var(--bg-color-page);
  overflow-y: auto;
}

.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.3s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}
</style>

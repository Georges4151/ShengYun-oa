<template>
  <div class="sidebar">
    <div class="logo">
      <img src="/favicon.svg" alt="logo" class="logo-img" />
      <span v-show="!collapsed" class="logo-text">盛韵建筑</span>
    </div>
    <el-scrollbar>
      <el-menu
        :default-active="activeMenu"
        :collapse="collapsed"
        :unique-opened="true"
        :collapse-transition="false"
        background-color="#304156"
        text-color="#bfcbd9"
        active-text-color="#409eff"
        router
      >
        <template v-for="route in menuRoutes" :key="route.path">
          <el-sub-menu v-if="route.children && route.children.length > 1" :index="route.path">
            <template #title>
              <el-icon><component :is="route.meta?.icon" /></el-icon>
              <span>{{ route.meta?.title }}</span>
            </template>
            <el-menu-item
              v-for="child in route.children.filter(c => !c.meta?.hidden)"
              :key="child.path"
              :index="`${route.path}/${child.path}`"
            >
              <el-icon><component :is="child.meta?.icon" v-if="child.meta?.icon" /></el-icon>
              <span>{{ child.meta?.title }}</span>
            </el-menu-item>
          </el-sub-menu>
          <el-menu-item v-else :index="getMenuIndex(route)">
            <el-icon><component :is="route.meta?.icon" /></el-icon>
            <span>{{ route.meta?.title }}</span>
          </el-menu-item>
        </template>
      </el-menu>
    </el-scrollbar>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import { useAppStore } from '@/stores/app'

const route = useRoute()
const appStore = useAppStore()

const collapsed = computed(() => appStore.sidebarCollapsed)

const activeMenu = computed(() => route.path)

const menuRoutes = computed(() => {
  const routes = [
    { path: '/dashboard', meta: { title: '首页', icon: 'HomeFilled' } },
    { 
      path: '/project', 
      meta: { title: '项目管理', icon: 'Folder' },
      children: [
        { path: 'list', meta: { title: '项目列表' } },
        { path: 'add', meta: { title: '添加项目' } },
        { path: 'cost-analysis', meta: { title: '成本分析' } }
      ]
    },
    { 
      path: '/account', 
      meta: { title: '账目管理', icon: 'Wallet' },
      children: [
        { path: 'list', meta: { title: '账目列表' } },
        { path: 'add', meta: { title: '添加账目' } },
        { path: 'report', meta: { title: '财务报表' } }
      ]
    },
    { 
      path: '/receipt', 
      meta: { title: '票据管理', icon: 'Tickets' },
      children: [
        { path: 'list', meta: { title: '票据列表' } },
        { path: 'upload', meta: { title: '上传票据' } },
        { path: 'approval', meta: { title: '审批流程' } }
      ]
    },
    { 
      path: '/material', 
      meta: { title: '材料管理', icon: 'Box' },
      children: [
        { path: 'list', meta: { title: '材料列表' } },
        { path: 'add', meta: { title: '添加材料' } },
        { path: 'report', meta: { title: '材料报表' } }
      ]
    },
    { 
      path: '/subcontractor', 
      meta: { title: '分包管理', icon: 'UserFilled' },
      children: [
        { path: 'list', meta: { title: '分包商列表' } },
        { path: 'add', meta: { title: '添加分包商' } },
        { path: 'contract', meta: { title: '合同管理' } }
      ]
    },
    { 
      path: '/payment', 
      meta: { title: '支付管理', icon: 'CreditCard' },
      children: [
        { path: 'list', meta: { title: '账户管理' } },
        { path: 'transaction', meta: { title: '收支查询' } },
        { path: 'reconciliation', meta: { title: '银行对账' } }
      ]
    },
    { 
      path: '/electronic-seal', 
      meta: { title: '电子章管理', icon: 'Stamp' },
      children: [
        { path: 'management', meta: { title: '电子章管理' } },
        { path: 'application', meta: { title: '使用申请' } },
        { path: 'approval', meta: { title: '申请审批' } },
        { path: 'stamp', meta: { title: '电子盖章' } }
      ]
    },
    { 
      path: '/notification', 
      meta: { title: '通知管理', icon: 'Bell' },
      children: [
        { path: 'center', meta: { title: '通知中心' } },
        { path: 'preferences', meta: { title: '通知设置' } }
      ]
    },
    { 
      path: '/user', 
      meta: { title: '用户管理', icon: 'User' },
      children: [
        { path: 'list', meta: { title: '用户列表' } },
        { path: 'add', meta: { title: '添加用户' } }
      ]
    },
    { 
      path: '/system', 
      meta: { title: '系统设置', icon: 'Setting' },
      children: [
        { path: 'settings', meta: { title: '系统设置' } },
        { path: 'backup', meta: { title: '备份恢复' } }
      ]
    }
  ]
  return routes
})

function getMenuIndex(route) {
  if (route.children && route.children.length === 1) {
    return `${route.path}/${route.children[0].path}`
  }
  return route.path
}
</script>

<style scoped>
.sidebar {
  height: 100%;
  display: flex;
  flex-direction: column;
}

.logo {
  height: var(--header-height);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0 15px;
  background-color: #2b2f3a;
}

.logo-img {
  width: 32px;
  height: 32px;
}

.logo-text {
  margin-left: 10px;
  font-size: 16px;
  font-weight: bold;
  color: #fff;
  white-space: nowrap;
}

.el-menu {
  border-right: none;
}

.el-menu:not(.el-menu--collapse) {
  width: 210px;
}

:deep(.el-menu-item),
:deep(.el-sub-menu__title) {
  height: 50px;
  line-height: 50px;
}

:deep(.el-menu-item:hover),
:deep(.el-sub-menu__title:hover) {
  background-color: #263445 !important;
}

:deep(.el-menu-item.is-active) {
  background-color: #409eff !important;
  color: #ffffff !important;
}

:deep(.el-menu-item.is-active .el-icon) {
  color: #ffffff !important;
}
</style>

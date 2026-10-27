import type { RouteRecordRaw } from 'vue-router'
import AppLayout from '@/layouts/AppLayout.vue'
import ModulePlaceholder from '@/views/ModulePlaceholder.vue'

/**
 * 三端页面地图（占位版，ARCHITECTURE.md 第 7 节）：
 * 登录 / 主数据 / 项目合同 / 报销 / 资金 / 对账 / 印章 / 通知 / 账号。
 * 页面组件暂为占位文案，待后端契约冻结后实现真实页面。
 */
export const routes: RouteRecordRaw[] = [
  {
    path: '/login',
    name: 'login',
    component: () => import('@/views/LoginView.vue'),
    meta: { title: '登录' }
  },
  {
    path: '/',
    component: AppLayout,
    redirect: '/masterdata',
    children: [
      { path: 'masterdata', name: 'masterdata', component: ModulePlaceholder, meta: { title: '主数据' } },
      { path: 'project', name: 'project', component: ModulePlaceholder, meta: { title: '项目合同' } },
      { path: 'expense', name: 'expense', component: ModulePlaceholder, meta: { title: '报销' } },
      { path: 'payment', name: 'payment', component: ModulePlaceholder, meta: { title: '资金' } },
      { path: 'bank', name: 'bank', component: ModulePlaceholder, meta: { title: '对账' } },
      { path: 'seal', name: 'seal', component: ModulePlaceholder, meta: { title: '印章' } },
      { path: 'notification', name: 'notification', component: ModulePlaceholder, meta: { title: '通知' } },
      { path: 'account', name: 'account', component: ModulePlaceholder, meta: { title: '账号' } }
    ]
  },
  { path: '/:pathMatch(.*)*', redirect: '/' }
]
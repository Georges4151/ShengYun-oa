import type { RouteRecordRaw } from 'vue-router'
import AppLayout from '@/layouts/AppLayout.vue'
import ModulePlaceholder from '@/views/ModulePlaceholder.vue'

/**
 * 三端页面地图（ARCHITECTURE.md 第 7 节）：
 * 登录 / 主数据 / 项目合同 / 报销 / 资金 / 对账 / 印章 / 通知 / 账号。
 * 已实现域（主数据、项目合同、账号）指向真实组件，其余域保持占位。
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
      {
        path: 'masterdata',
        name: 'masterdata',
        component: () => import('@/views/masterdata/MasterdataView.vue'),
        meta: { title: '主数据' }
      },
      {
        path: 'project',
        name: 'project',
        component: () => import('@/views/project/ProjectView.vue'),
        meta: { title: '项目合同' }
      },
      { path: 'expense', name: 'expense', component: () => import('@/views/expense/ExpenseView.vue'), meta: { title: '报销' } },
      { path: 'payment', name: 'payment', component: () => import('@/views/payment/PaymentView.vue'), meta: { title: '资金' } },
      { path: 'bank', name: 'bank', component: () => import('@/views/bank/BankView.vue'), meta: { title: '对账' } },
      { path: 'seal', name: 'seal', component: () => import('@/views/seal/SealView.vue'), meta: { title: '印章' } },
      { path: 'notification', name: 'notification', component: ModulePlaceholder, meta: { title: '通知' } },
      {
        path: 'account',
        name: 'account',
        component: () => import('@/views/iam/AccountView.vue'),
        meta: { title: '账号' }
      }
    ]
  },
  { path: '/:pathMatch(.*)*', redirect: '/' }
]

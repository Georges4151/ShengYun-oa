import type { RouteRecordRaw } from 'vue-router'
import AppLayout from '@/layouts/AppLayout.vue'

/**
 * 三端页面地图（ARCHITECTURE.md 第 7 节）：
 * 登录 / 主数据 / 项目合同 / 报销 / 资金 / 对账 / 印章 / 通知 / 账号。
 * 已实现域指向真实组件。
 */
export const routes: RouteRecordRaw[] = [
  {
    path: '/login',
    name: 'login',
    component: () => import('@/views/LoginView.vue'),
    meta: { title: '登录' }
  },
  {
    // 企业微信扫码登录回调：匿名可访问，由 router/index.ts 的 PUBLIC_PATHS 放行
    path: '/login/wecom',
    name: 'wecom-callback',
    component: () => import('@/views/auth/WeComCallbackView.vue'),
    meta: { title: '企业微信登录' }
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
      {
        path: 'hr',
        name: 'hr',
        component: () => import('@/views/hr/HrView.vue'),
        meta: { title: '人事' }
      },
      {
        path: 'admin',
        name: 'admin',
        component: () => import('@/views/admin/AdminView.vue'),
        meta: { title: '行政后勤', icon: 'Box' }
      },
      {
        path: 'doc',
        name: 'doc',
        component: () => import('@/views/doc/DocView.vue'),
        meta: { title: '文档知识库', icon: 'Document', permission: 'doc' }
      },
      {
        path: 'integration/wecom',
        name: 'integration-wecom',
        component: () => import('@/views/integration/WeComView.vue'),
        meta: {
          title: '企业微信集成',
          icon: 'ChatDotRound',
          permission: 'integration:wecom:view'
        }
      },
      { path: 'todo', name: 'todo', component: () => import('@/views/bpm/BpmView.vue'), meta: { title: '待办中心' } },
      { path: 'expense', name: 'expense', component: () => import('@/views/expense/ExpenseView.vue'), meta: { title: '报销' } },
      { path: 'payment', name: 'payment', component: () => import('@/views/payment/PaymentView.vue'), meta: { title: '资金' } },
      { path: 'bank', name: 'bank', component: () => import('@/views/bank/BankView.vue'), meta: { title: '对账' } },
      { path: 'seal', name: 'seal', component: () => import('@/views/seal/SealView.vue'), meta: { title: '印章' } },
      { path: 'notification', name: 'notification', component: () => import('@/views/notification/NotificationView.vue'), meta: { title: '通知' } },
      {
        path: 'report',
        name: 'report',
        component: () => import('@/views/report/ReportView.vue'),
        meta: { title: '报表' }
      },
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

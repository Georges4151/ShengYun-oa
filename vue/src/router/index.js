import { createRouter, createWebHistory } from 'vue-router'
import { getToken } from '@/utils/auth'

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/login/Login.vue'),
    meta: { title: '登录', requiresAuth: false }
  },
  {
    path: '/',
    component: () => import('@/layouts/DefaultLayout.vue'),
    redirect: '/dashboard',
    children: [
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('@/views/dashboard/Dashboard.vue'),
        meta: { title: '首页', icon: 'HomeFilled' }
      },
      {
        path: 'project',
        name: 'Project',
        redirect: '/project/list',
        meta: { title: '项目管理', icon: 'Folder' },
        children: [
          { path: 'list', name: 'ProjectList', component: () => import('@/views/project/ProjectList.vue'), meta: { title: '项目列表' } },
          { path: 'add', name: 'ProjectAdd', component: () => import('@/views/project/ProjectAdd.vue'), meta: { title: '添加项目' } },
          { path: 'edit/:id', name: 'ProjectEdit', component: () => import('@/views/project/ProjectEdit.vue'), meta: { title: '编辑项目', hidden: true } },
          { path: 'cost-analysis/:id?', name: 'ProjectCostAnalysis', component: () => import('@/views/project/ProjectCostAnalysis.vue'), meta: { title: '项目成本分析' } }
        ]
      },
      {
        path: 'account',
        name: 'Account',
        redirect: '/account/list',
        meta: { title: '账目管理', icon: 'Wallet' },
        children: [
          { path: 'list', name: 'AccountList', component: () => import('@/views/account/AccountList.vue'), meta: { title: '账目列表' } },
          { path: 'add', name: 'AccountAdd', component: () => import('@/views/account/AccountAdd.vue'), meta: { title: '添加账目' } },
          { path: 'edit/:id', name: 'AccountEdit', component: () => import('@/views/account/AccountEdit.vue'), meta: { title: '编辑账目', hidden: true } },
          { path: 'report', name: 'FinancialReport', component: () => import('@/views/account/FinancialReport.vue'), meta: { title: '财务报表' } }
        ]
      },
      {
        path: 'receipt',
        name: 'Receipt',
        redirect: '/receipt/list',
        meta: { title: '票据管理', icon: 'Tickets' },
        children: [
          { path: 'list', name: 'ReceiptList', component: () => import('@/views/receipt/ReceiptList.vue'), meta: { title: '票据列表' } },
          { path: 'upload', name: 'ReceiptUpload', component: () => import('@/views/receipt/ReceiptUpload.vue'), meta: { title: '上传票据' } },
          { path: 'detail/:id', name: 'ReceiptDetail', component: () => import('@/views/receipt/ReceiptDetail.vue'), meta: { title: '票据详情', hidden: true } },
          { path: 'verify/:id', name: 'ReceiptVerify', component: () => import('@/views/receipt/ReceiptVerify.vue'), meta: { title: '票据验证', hidden: true } },
          { path: 'approval', name: 'ApprovalFlow', component: () => import('@/views/receipt/ApprovalFlow.vue'), meta: { title: '审批流程' } }
        ]
      },
      {
        path: 'material',
        name: 'Material',
        redirect: '/material/list',
        meta: { title: '材料管理', icon: 'Box' },
        children: [
          { path: 'list', name: 'MaterialList', component: () => import('@/views/material/MaterialList.vue'), meta: { title: '材料列表' } },
          { path: 'add', name: 'MaterialAdd', component: () => import('@/views/material/MaterialAdd.vue'), meta: { title: '添加材料' } },
          { path: 'edit/:id', name: 'MaterialEdit', component: () => import('@/views/material/MaterialEdit.vue'), meta: { title: '编辑材料', hidden: true } },
          { path: 'report', name: 'MaterialReport', component: () => import('@/views/material/MaterialReport.vue'), meta: { title: '材料报表' } }
        ]
      },
      {
        path: 'subcontractor',
        name: 'Subcontractor',
        redirect: '/subcontractor/list',
        meta: { title: '分包管理', icon: 'UserFilled' },
        children: [
          { path: 'list', name: 'SubcontractorList', component: () => import('@/views/subcontractor/SubcontractorList.vue'), meta: { title: '分包商列表' } },
          { path: 'add', name: 'SubcontractorAdd', component: () => import('@/views/subcontractor/SubcontractorAdd.vue'), meta: { title: '添加分包商' } },
          { path: 'edit/:id', name: 'SubcontractorEdit', component: () => import('@/views/subcontractor/SubcontractorEdit.vue'), meta: { title: '编辑分包商', hidden: true } },
          { path: 'contract', name: 'ContractManage', component: () => import('@/views/subcontractor/ContractManage.vue'), meta: { title: '合同管理' } }
        ]
      },
      {
        path: 'payment',
        name: 'Payment',
        redirect: '/payment/list',
        meta: { title: '支付管理', icon: 'CreditCard' },
        children: [
          { path: 'list', name: 'PaymentList', component: () => import('@/views/payment/PaymentList.vue'), meta: { title: '账户管理' } },
          { path: 'transaction', name: 'TransactionQuery', component: () => import('@/views/payment/TransactionQuery.vue'), meta: { title: '收支查询' } },
          { path: 'reconciliation', name: 'BankReconciliation', component: () => import('@/views/payment/BankReconciliation.vue'), meta: { title: '银行对账' } }
        ]
      },
      {
        path: 'electronic-seal',
        name: 'ElectronicSeal',
        redirect: '/electronic-seal/management',
        meta: { title: '电子章管理', icon: 'Stamp' },
        children: [
          { path: 'management', name: 'SealManagement', component: () => import('@/views/electronic-seal/SealManagement.vue'), meta: { title: '电子章管理' } },
          { path: 'application', name: 'SealApplication', component: () => import('@/views/electronic-seal/SealApplication.vue'), meta: { title: '使用申请' } },
          { path: 'approval', name: 'SealApproval', component: () => import('@/views/electronic-seal/SealApproval.vue'), meta: { title: '申请审批' } },
          { path: 'stamp', name: 'SealStamp', component: () => import('@/views/electronic-seal/SealStamp.vue'), meta: { title: '电子盖章' } },
          { path: 'batch-stamp', name: 'BatchStamp', component: () => import('@/views/electronic-seal/BatchStamp.vue'), meta: { title: '批量盖章' } }
        ]
      },
      {
        path: 'notification',
        name: 'Notification',
        redirect: '/notification/list',
        meta: { title: '通知管理', icon: 'Bell' },
        children: [
          { path: 'list', name: 'NotificationList', component: () => import('@/views/notification/NotificationList.vue'), meta: { title: '通知列表' } },
          { path: 'settings', name: 'NotificationSettings', component: () => import('@/views/notification/NotificationSettings.vue'), meta: { title: '通知设置' } }
        ]
      },
      {
        path: 'user',
        name: 'User',
        redirect: '/user/list',
        meta: { title: '用户管理', icon: 'User' },
        children: [
          { path: 'list', name: 'UserList', component: () => import('@/views/user/UserList.vue'), meta: { title: '用户列表' } },
          { path: 'role', name: 'RoleManage', component: () => import('@/views/user/RoleManage.vue'), meta: { title: '角色管理' } },
          { path: 'profile', name: 'Profile', component: () => import('@/views/user/Profile.vue'), meta: { title: '个人中心' } }
        ]
      },
      {
        path: 'system',
        name: 'System',
        redirect: '/system/settings',
        meta: { title: '系统设置', icon: 'Setting' },
        children: [
          { path: 'settings', name: 'BasicSettings', component: () => import('@/views/system/BasicSettings.vue'), meta: { title: '基础设置' } },
          { path: 'log', name: 'OperationLog', component: () => import('@/views/system/OperationLog.vue'), meta: { title: '操作日志' } },
          { path: 'backup', name: 'DataBackup', component: () => import('@/views/system/DataBackup.vue'), meta: { title: '数据备份' } }
        ]
      }
    ]
  },
  {
    path: '/:pathMatch(.*)*',
    name: 'NotFound',
    component: () => import('@/views/error/404.vue'),
    meta: { title: '页面不存在' }
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to, from, next) => {
  document.title = to.meta.title ? `${to.meta.title} - 盛韵建筑账目管理系统` : '盛韵建筑账目管理系统'
  
  const token = getToken()
  
  if (to.meta.requiresAuth === false) {
    next()
  } else if (!token && to.path !== '/login') {
    next('/login')
  } else if (token && to.path === '/login') {
    next('/dashboard')
  } else {
    next()
  }
})

export default router

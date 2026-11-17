import { createRouter, createWebHashHistory } from 'vue-router'

const routes = [
    {
        path: '/widget',
        name: 'Widget',
        component: () => import('@/views/widget/WidgetView.vue'),
        meta: { title: '小智AI · 在线客服' }
    },
    {
        path: '/',
        name: 'Index',
        component: () => import('@/views/Index.vue'),
        meta: { title: '小智AI · 内部测试台' }
    },
    {
        path: '/chat/:chatId',
        name: 'ChatPage',
        component: () => import('@/views/ChatPage.vue'),
        meta: { title: '小智AI · 对话测试' }
    },
    {
        path: '/customer-service/chat',
        name: 'CustomerServiceChatPage',
        component: () => import('@/views/CustomerServiceChatPage.vue'),
        meta: { title: '小智AI · Agent 调试' }
    },
    {
        path: '/admin/login',
        name: 'AdminLogin',
        component: () => import('@/views/admin/LoginView.vue'),
        meta: { title: '小智AI · 商家登录' }
    },
    {
        path: '/admin',
        component: () => import('@/views/admin/AdminLayout.vue'),
        children: [
            { path: '', name: 'AdminDashboard', component: () => import('@/views/admin/DashboardView.vue'), meta: { title: '数据概览' } },
            { path: 'inbox', name: 'AdminInbox', component: () => import('@/views/admin/TicketsView.vue'), meta: { title: '统一收件箱' } },
            { path: 'tickets', redirect: '/admin/inbox' },
            { path: 'conversations', name: 'AdminConversations', component: () => import('@/views/admin/ConversationsView.vue'), meta: { title: '会话记录' } },
            { path: 'customers', name: 'AdminCustomers', component: () => import('@/views/admin/CustomersView.vue'), meta: { title: '客户' } },
            { path: 'orders', name: 'AdminOrders', component: () => import('@/views/admin/OrdersView.vue'), meta: { title: '订单' } },
            { path: 'products', name: 'AdminProducts', component: () => import('@/views/admin/ProductsView.vue'), meta: { title: '商品' } },
            { path: 'actions', name: 'AdminActions', component: () => import('@/views/admin/ActionsView.vue'), meta: { title: '动作审批' } },
            { path: 'tool-logs', name: 'AdminToolLogs', component: () => import('@/views/admin/ToolLogsView.vue'), meta: { title: '工具调用' } },
            { path: 'traces', name: 'AdminTraces', component: () => import('@/views/admin/TracesView.vue'), meta: { title: '轨迹回放' } },
            { path: 'knowledge', name: 'AdminKnowledge', component: () => import('@/views/admin/KnowledgeView.vue'), meta: { title: '知识库' } },
            { path: 'rag-debug', redirect: '/admin/rag-workbench' },
            { path: 'rag-workbench', name: 'AdminRagDebug', component: () => import('@/views/admin/RagDebugView.vue'), meta: { title: 'RAG 工作台' } },
            { path: 'evals', name: 'AdminEvals', component: () => import('@/views/admin/EvalsView.vue'), meta: { title: '智能体评测' } },
            { path: 'agent-workflow', name: 'AdminWorkflow', component: () => import('@/views/admin/AgentWorkflowView.vue'), meta: { title: '智能体工作流' } },
            { path: 'multilingual', name: 'AdminMultilingual', component: () => import('@/views/admin/MultilingualView.vue'), meta: { title: '多语言证据' } },
            { path: 'operations', name: 'AdminOperations', component: () => import('@/views/admin/OperationsView.vue'), meta: { title: '运营指标' } },
            { path: 'audit', name: 'AdminAudit', component: () => import('@/views/admin/AuditView.vue'), meta: { title: '审计' } },
            { path: 'users', name: 'AdminUsers', component: () => import('@/views/admin/UsersView.vue'), meta: { title: '账号说明' } },
        ]
    }
]

const router = createRouter({
    history: createWebHashHistory(),
    routes,
})

router.beforeEach((to, from, next) => {
    document.title = to.meta?.title || '小智AI'
    if (to.path.startsWith('/admin') && to.path !== '/admin/login') {
        const token = localStorage.getItem('admin_token')
        if (!token) {
            next({ path: '/admin/login', query: { redirect: to.fullPath } })
            return
        }
    }
    next()
})

export default router

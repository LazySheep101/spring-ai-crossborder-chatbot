<template>
  <div class="admin-shell">
    <aside class="sidebar">
      <button class="brand" type="button" @click="router.push('/admin')">
        <span class="brand-mark">XZ</span>
        <span class="brand-copy">
          <strong>小智AI</strong>
          <small>商家客服运营平台</small>
        </span>
      </button>

      <nav class="sidebar-menu">
        <div v-for="group in navGroups" :key="group.key" class="nav-group">
          <div class="nav-group-label">{{ group.label }}</div>
          <button
            v-for="item in group.children"
            :key="item.to"
            type="button"
            class="menu-item"
            :class="{ active: isActive(item.to) }"
            @click="router.push(item.to)"
          >
            {{ item.label }}
          </button>
        </div>
      </nav>

      <div class="sidebar-footer">
        <a-button type="text" block class="side-action" @click="router.push('/widget')">
          买家 Widget
        </a-button>
        <a-button type="text" block class="side-action" @click="router.push('/')">
          内部测试台
        </a-button>
      </div>
    </aside>

    <div class="workspace">
      <header class="topbar">
        <div>
          <h1>{{ currentLabel }}</h1>
          <p>{{ currentDesc }}</p>
        </div>
        <div class="topbar-end">
          <a-tag color="blue">演示数据</a-tag>
          <span v-if="auth.displayName" class="user-chip">
            {{ auth.displayName }} · {{ auth.role }}
          </span>
          <a-button type="link" @click="router.push('/widget')">买家端</a-button>
          <a-button v-if="auth.token" type="link" danger @click="logout">退出</a-button>
        </div>
      </header>
      <main class="content" :class="{ 'content-flush': flushPage }">
        <div class="panel" :class="{ 'panel-flush': flushPage }">
          <router-view />
        </div>
      </main>
    </div>
  </div>
</template>

<script setup>
import { reactive, onMounted, computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'

const router = useRouter()
const route = useRoute()
const auth = reactive({
  token: localStorage.getItem('admin_token') || '',
  displayName: localStorage.getItem('admin_displayName') || '',
  role: localStorage.getItem('admin_role') || ''
})

const navGroups = [
  {
    key: 'workspace',
    label: '工作台',
    children: [
      { to: '/admin', label: '数据概览', desc: '待办与系统信号' },
      { to: '/admin/inbox', label: '统一收件箱', desc: '工单接管与回复' },
      { to: '/admin/conversations', label: '会话记录', desc: '历史对话' },
      { to: '/admin/actions', label: '动作审批', desc: '退货退款换货改址' },
    ],
  },
  {
    key: 'commerce',
    label: '客户与交易',
    children: [
      { to: '/admin/customers', label: '客户', desc: '客户与订单关联' },
      { to: '/admin/orders', label: '订单', desc: '订单与履约' },
      { to: '/admin/products', label: '商品', desc: '商品与库存' },
    ],
  },
  {
    key: 'ai',
    label: 'AI 与知识',
    children: [
      { to: '/admin/knowledge', label: '知识库', desc: '上传与向量化' },
      { to: '/admin/rag-workbench', label: 'RAG 工作台', desc: '检索调试' },
      { to: '/admin/traces', label: '轨迹回放', desc: 'Agent Run' },
      { to: '/admin/tool-logs', label: '工具调用', desc: '工具审计' },
      { to: '/admin/evals', label: '智能体评测', desc: '契约摘要' },
      { to: '/admin/agent-workflow', label: '智能体工作流', desc: '白名单说明' },
      { to: '/admin/multilingual', label: '多语言证据', desc: '语种与翻译回退' },
    ],
  },
  {
    key: 'ops',
    label: '运营',
    children: [
      { to: '/admin/operations', label: '运营指标', desc: '解决率与接管率' },
      { to: '/admin/audit', label: '审计', desc: '审批与工具' },
      { to: '/admin/users', label: '账号说明', desc: '演示账号' },
    ],
  },
]

const flatNav = computed(() => navGroups.flatMap(g => g.children))
const current = computed(() => flatNav.value.find(n => n.to === route.path) || flatNav.value[0])
const currentLabel = computed(() => current.value?.label || '商家后台')
const currentDesc = computed(() => current.value?.desc || '跨境智能客服运营与观测')
const flushPage = computed(() => route.path === '/admin/inbox' || route.path === '/admin/traces')

const isActive = (to) => {
  if (to === '/admin') return route.path === '/admin'
  return route.path === to || route.path.startsWith(to + '/')
}

onMounted(() => {
  if (!auth.token && router.currentRoute.value.path !== '/admin/login') {
    router.push('/admin/login')
  }
})

function logout() {
  localStorage.removeItem('admin_token')
  localStorage.removeItem('admin_displayName')
  localStorage.removeItem('admin_role')
  auth.token = ''
  router.push('/admin/login')
}
</script>

<style scoped>
.admin-shell {
  min-height: 100vh;
  display: flex;
  background: #f5f7fa;
}

.sidebar {
  width: 232px;
  flex-shrink: 0;
  background: #0d1728;
  color: #fff;
  display: flex;
  flex-direction: column;
  min-height: 100vh;
}

.brand {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 18px 16px;
  background: transparent;
  border: 0;
  color: inherit;
  cursor: pointer;
  text-align: left;
}

.brand-mark {
  width: 34px;
  height: 34px;
  border-radius: 7px;
  background: #1677ff;
  display: grid;
  place-items: center;
  font-size: 11px;
  font-weight: 800;
}

.brand-copy {
  display: flex;
  flex-direction: column;
}

.brand-copy strong {
  font-size: 14px;
}

.brand-copy small {
  margin-top: 2px;
  color: #91a0b5;
  font-size: 11px;
}

.sidebar-menu {
  flex: 1;
  padding: 8px 10px 16px;
  overflow-y: auto;
}

.nav-group {
  margin-bottom: 14px;
}

.nav-group-label {
  color: #64748b;
  font-size: 11px;
  padding: 6px 12px;
  letter-spacing: 0.04em;
}

.menu-item {
  border: 0;
  background: transparent;
  color: #c7d2e0;
  text-align: left;
  padding: 9px 12px;
  border-radius: 8px;
  cursor: pointer;
  font-size: 13px;
  width: 100%;
  margin-bottom: 2px;
}

.menu-item:hover {
  background: #13233a;
  color: #fff;
}

.menu-item.active {
  background: #19345f;
  color: #fff;
}

.sidebar-footer {
  border-top: 1px solid #1e2c42;
  padding: 10px;
}

.side-action {
  color: #c7d2e0 !important;
  text-align: left !important;
}

.workspace {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
}

.topbar {
  background: #fff;
  border-bottom: 1px solid #e4e9f0;
  padding: 14px 24px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.topbar h1 {
  margin: 0;
  font-size: 18px;
  color: #182230;
}

.topbar p {
  margin: 2px 0 0;
  font-size: 12px;
  color: #667085;
}

.topbar-end {
  display: flex;
  align-items: center;
  gap: 4px;
}

.user-chip {
  font-size: 12px;
  color: #667085;
  margin: 0 8px;
}

.content {
  padding: 20px 24px;
}

.content-flush {
  padding-bottom: 0;
}

.panel {
  background: #fff;
  border: 1px solid #e4e9f0;
  border-radius: 7px;
  padding: 18px;
  min-height: 60vh;
  box-shadow: 0 1px 2px rgba(16, 24, 40, 0.03);
}

.panel-flush {
  padding: 0;
  overflow: hidden;
}

@media (max-width: 860px) {
  .sidebar {
    display: none;
  }
}
</style>

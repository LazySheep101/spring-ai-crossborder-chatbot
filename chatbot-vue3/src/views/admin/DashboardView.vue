<template>
  <div class="dashboard">
    <div class="hero">
      <div>
        <h2>运营概览</h2>
        <p>今日待办与系统信号，快速进入作业入口。</p>
      </div>
      <a-button type="primary" @click="router.push('/widget')">打开买家 Widget</a-button>
    </div>

    <a-spin :spinning="loading">
      <div class="metric-grid">
        <div class="metric-card warn" @click="router.push('/admin/actions?status=PENDING')">
          <span class="label">待审批动作</span>
          <strong>{{ summary.pendingActions ?? '-' }}</strong>
        </div>
        <div class="metric-card" @click="router.push('/admin/inbox')">
          <span class="label">开放工单</span>
          <strong>{{ summary.openTickets ?? '-' }}</strong>
        </div>
        <div class="metric-card" @click="router.push('/admin/inbox')">
          <span class="label">人工接管中</span>
          <strong>{{ summary.humanAssignedTickets ?? '-' }}</strong>
        </div>
        <div class="metric-card danger" @click="router.push('/admin/tool-logs')">
          <span class="label">近期工具失败</span>
          <strong>{{ summary.toolFailuresRecent ?? '-' }}</strong>
        </div>
        <div class="metric-card" @click="router.push('/admin/traces')">
          <span class="label">最近 Agent Run</span>
          <strong>{{ summary.recentAgentRuns ?? '-' }}</strong>
        </div>
        <div class="metric-card" @click="router.push('/admin/orders')">
          <span class="label">订单 / 商品</span>
          <strong>{{ summary.orderCount ?? 0 }} / {{ summary.productCount ?? 0 }}</strong>
        </div>
      </div>
    </a-spin>

    <div class="section">
      <h3>快捷入口</h3>
      <div class="shortcuts">
        <button
          v-for="item in shortcuts"
          :key="item.path"
          type="button"
          class="shortcut"
          @click="router.push(item.path)"
        >
          {{ item.label }}
        </button>
      </div>
    </div>

    <div class="section tip">
      <h3>5 分钟演示剧本</h3>
      <ol>
        <li>打开买家 Widget，问物流 / 退货政策，观察引用卡</li>
        <li>发起退货申请，到「动作审批」通过或驳回</li>
        <li>要求转人工，在工单 Inbox 接管并回复</li>
        <li>到 Trace 回放整条 Agent 轨迹</li>
      </ol>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { fetchDashboardSummary } from '@/api/admin'

const router = useRouter()
const loading = ref(false)
const summary = reactive({})

const shortcuts = computed(() => summary.shortcuts || [
  { label: '统一收件箱', path: '/admin/inbox' },
  { label: '动作审批', path: '/admin/actions' },
  { label: '轨迹回放', path: '/admin/traces' },
  { label: '买家 Widget', path: '/widget' },
])

onMounted(async () => {
  loading.value = true
  try {
    const res = await fetchDashboardSummary()
    if (res.data?.success) {
      Object.assign(summary, res.data.data || {})
    } else {
      message.error(res.data?.message || '加载概览失败')
    }
  } catch (e) {
    message.error(e?.message || '加载概览失败')
  } finally {
    loading.value = false
  }
})
</script>

<style scoped>
.dashboard {
  display: grid;
  gap: 22px;
}

.hero {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16px;
}

.hero h2 {
  margin: 0;
  font-size: 20px;
  color: #182230;
}

.hero p {
  margin: 4px 0 0;
  color: #667085;
  font-size: 13px;
}

.metric-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
}

.metric-card {
  background: #f8fafc;
  border: 1px solid #e4e9f0;
  border-radius: 10px;
  padding: 16px;
  cursor: pointer;
  transition: border-color 0.15s ease, transform 0.15s ease;
}

.metric-card:hover {
  border-color: #91caff;
  transform: translateY(-1px);
}

.metric-card .label {
  display: block;
  color: #667085;
  font-size: 12px;
  margin-bottom: 8px;
}

.metric-card strong {
  font-size: 28px;
  color: #182230;
  font-weight: 700;
}

.metric-card.warn strong {
  color: #b54708;
}

.metric-card.danger strong {
  color: #c01b2e;
}

.section h3 {
  margin: 0 0 10px;
  font-size: 14px;
  color: #182230;
}

.shortcuts {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.shortcut {
  border: 1px solid #d9e4f5;
  background: #fff;
  color: #1f4d8f;
  border-radius: 18px;
  padding: 8px 14px;
  font-size: 13px;
  cursor: pointer;
}

.shortcut:hover {
  background: #f0f6ff;
}

.tip {
  background: #f8fafc;
  border: 1px solid #e4e9f0;
  border-radius: 10px;
  padding: 14px 16px;
}

.tip ol {
  margin: 0;
  padding-left: 18px;
  color: #475467;
  font-size: 13px;
  line-height: 1.8;
}

@media (max-width: 960px) {
  .metric-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 640px) {
  .metric-grid {
    grid-template-columns: 1fr;
  }
  .hero {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>

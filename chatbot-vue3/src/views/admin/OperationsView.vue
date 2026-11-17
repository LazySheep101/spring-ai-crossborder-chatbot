<template>
  <div class="ops">
    <div class="toolbar">
      <div>
        <h2>运营指标</h2>
        <p>基于本库工单与审批聚合的解决率 / 接管率（演示口径）</p>
      </div>
      <a-button :loading="loading" @click="load">刷新</a-button>
    </div>
    <a-spin :spinning="loading">
      <div class="metric-grid">
        <div class="metric"><span>工单总数</span><strong>{{ m.ticketTotal ?? '-' }}</strong></div>
        <div class="metric"><span>已解决</span><strong>{{ m.resolved ?? '-' }}</strong></div>
        <div class="metric"><span>解决率</span><strong>{{ pct(m.resolveRate) }}</strong></div>
        <div class="metric"><span>人工接管</span><strong>{{ m.humanAssigned ?? '-' }}</strong></div>
        <div class="metric"><span>接管率</span><strong>{{ pct(m.takeoverRate) }}</strong></div>
        <div class="metric"><span>待审批</span><strong>{{ m.pendingActions ?? '-' }}</strong></div>
      </div>
    </a-spin>
    <p class="note">口径：解决率 = RESOLVED / 全部工单；接管率 = HUMAN_ASSIGNED(含历史) 近似用当前开放人工单 / 全部工单，演示用。</p>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { message } from 'ant-design-vue'
import { fetchTickets, fetchActions, fetchDashboardSummary } from '@/api/admin'

const loading = ref(false)
const m = reactive({})

function pct(v) {
  if (v == null || Number.isNaN(v)) return '-'
  return `${(Number(v) * 100).toFixed(1)}%`
}

async function load() {
  loading.value = true
  try {
    const [ticketsRes, actionsRes, dashRes] = await Promise.all([
      fetchTickets(),
      fetchActions(),
      fetchDashboardSummary(),
    ])
    const tickets = ticketsRes.data?.data || []
    const actions = actionsRes.data?.data || []
    const dash = dashRes.data?.data || {}
    const total = tickets.length
    const resolved = tickets.filter(t => ['RESOLVED', 'CLOSED'].includes((t.status || '').toUpperCase())).length
    const human = tickets.filter(t => ['HUMAN_ASSIGNED', 'ASSIGNED'].includes((t.status || '').toUpperCase())).length
    const pending = actions.filter(a => a.status === 'PENDING').length
    Object.assign(m, {
      ticketTotal: total,
      resolved,
      resolveRate: total ? resolved / total : 0,
      humanAssigned: human || dash.humanAssignedTickets || 0,
      takeoverRate: total ? (human || dash.humanAssignedTickets || 0) / total : 0,
      pendingActions: pending || dash.pendingActions || 0,
    })
  } catch (e) {
    message.error(e?.message || '加载失败')
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 16px;
  gap: 12px;
}
.toolbar h2 { margin: 0; font-size: 16px; color: #182230; }
.toolbar p { margin: 4px 0 0; font-size: 12px; color: #667085; }
.metric-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
}
.metric {
  background: #f8fafc;
  border: 1px solid #e4e9f0;
  border-radius: 10px;
  padding: 16px;
}
.metric span { display: block; color: #667085; font-size: 12px; margin-bottom: 8px; }
.metric strong { font-size: 28px; color: #182230; }
.note { margin-top: 16px; font-size: 12px; color: #98a2b3; }
@media (max-width: 900px) {
  .metric-grid { grid-template-columns: 1fr 1fr; }
}
</style>

<template>
  <div class="inbox">
    <aside class="ticket-list">
      <div class="list-toolbar">
        <div>
          <h2>统一收件箱</h2>
          <p>{{ rows.length }} 条 · 与买家 Widget 同 chatId</p>
        </div>
        <a-select
          v-model:value="statusFilter"
          style="width: 150px"
          size="small"
          @change="load"
        >
          <a-select-option value="">全部状态</a-select-option>
          <a-select-option value="AI_WORKING">AI 处理中</a-select-option>
          <a-select-option value="NEEDS_APPROVAL">待审批</a-select-option>
          <a-select-option value="HUMAN_ASSIGNED">人工接管</a-select-option>
          <a-select-option value="RESOLVED">已解决</a-select-option>
        </a-select>
      </div>

      <a-spin :spinning="loading">
        <button
          v-for="row in rows"
          :key="row.id"
          type="button"
          class="ticket-card"
          :class="{ active: selected?.id === row.id }"
          @click="openTicket(row)"
        >
          <div class="ticket-card-top">
            <strong>{{ row.ticketNo }}</strong>
            <a-tag :color="statusColor(row.status)" class="status-tag">{{ statusLabel(row.status) }}</a-tag>
          </div>
          <div class="ticket-subject">{{ row.subject || '未命名工单' }}</div>
          <div class="ticket-meta">
            {{ row.customerContact || '匿名买家' }}
            · 订单 {{ row.orderNo || '-' }}
          </div>
          <p class="ticket-summary">{{ row.summary || '暂无摘要' }}</p>
        </button>
        <div v-if="!loading && !rows.length" class="empty">暂无工单</div>
      </a-spin>
      <a-alert v-if="msg" type="warning" show-icon :message="msg" closable class="mt" @close="msg = ''" />
    </aside>

    <section class="workspace-pane">
      <template v-if="!selected">
        <div class="empty-pane">
          <h3>选择工单开始作业</h3>
          <p>左侧打开工单后可查看绑定会话、人工接管并回复买家。</p>
        </div>
      </template>
      <template v-else>
        <header class="pane-header">
          <div>
            <h3>{{ selected.ticketNo }} · {{ selected.subject }}</h3>
            <p>
              会话 {{ selected.chatId || '未绑定' }}
              · 对话状态
              <a-tag :color="statusColor(detail.conversationStatus || selected.status)">
                {{ statusLabel(detail.conversationStatus || selected.status) }}
              </a-tag>
              · 负责人 {{ selected.assignee || '未分配' }}
            </p>
          </div>
          <div class="pane-actions">
            <a-button
              type="primary"
              :loading="acting"
              :disabled="selected.status === 'HUMAN_ASSIGNED' || selected.status === 'RESOLVED'"
              @click="takeover(selected.id)"
            >
              接管
            </a-button>
            <a-button
              :loading="acting"
              :disabled="selected.status === 'RESOLVED'"
              @click="resolve(selected.id)"
            >
              解决
            </a-button>
            <a-button @click="openTicket(selected)">刷新</a-button>
          </div>
        </header>

        <div ref="msgEl" class="messages">
          <div
            v-for="m in detail.messages || []"
            :key="m.id"
            class="bubble"
            :class="roleClass(m.role)"
          >
            <div class="bubble-role">{{ roleLabel(m.role) }}</div>
            <div class="bubble-body">{{ m.content }}</div>
          </div>
          <div v-if="!(detail.messages || []).length" class="empty">暂无消息，等待买家进线或 AI 回复</div>
        </div>

        <footer class="composer">
          <a-textarea
            v-model:value="replyText"
            :rows="3"
            placeholder="对买家可见的回复（写入同一 chatId，Widget 会轮询到）"
            :disabled="selected.status === 'RESOLVED' || !selected.chatId"
            @pressEnter.exact.prevent="sendReply"
          />
          <div class="composer-bar">
            <span>人工回复会以 human 角色写入会话</span>
            <a-button
              type="primary"
              :loading="replying"
              :disabled="!replyText.trim() || !selected.chatId || selected.status === 'RESOLVED'"
              @click="sendReply"
            >
              发送回复
            </a-button>
          </div>
        </footer>
      </template>
    </section>
  </div>
</template>

<script setup>
import { nextTick, onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import { message } from 'ant-design-vue'
import { fetchTickets, fetchTicketDetail, takeoverTicket, resolveTicket, replyTicket } from '@/api/admin'

const rows = ref([])
const msg = ref('')
const statusFilter = ref('')
const selected = ref(null)
const replyText = ref('')
const loading = ref(false)
const acting = ref(false)
const replying = ref(false)
const msgEl = ref(null)
const detail = reactive({ messages: [], conversationStatus: null })
let pollTimer = null

const STATUS_MAP = {
  AI_WORKING: { label: 'AI 处理中', color: 'blue' },
  NEEDS_APPROVAL: { label: '待审批', color: 'orange' },
  HUMAN_ASSIGNED: { label: '人工接管', color: 'purple' },
  ASSIGNED: { label: '已分配', color: 'purple' },
  RESOLVED: { label: '已解决', color: 'green' },
  CLOSED: { label: '已关闭', color: 'default' },
}

function statusLabel(s) {
  return STATUS_MAP[s]?.label || s || '-'
}
function statusColor(s) {
  return STATUS_MAP[s]?.color || 'default'
}
function roleClass(role) {
  const r = (role || '').toLowerCase()
  if (r === 'user' || r === 'customer') return 'user'
  if (r === 'human' || r === 'agent') return 'human'
  return 'assistant'
}
function roleLabel(role) {
  const r = (role || '').toLowerCase()
  if (r === 'user' || r === 'customer') return '买家'
  if (r === 'human' || r === 'agent') return '客服'
  return '小智AI'
}

async function load() {
  loading.value = true
  try {
    const res = await fetchTickets(statusFilter.value || undefined)
    rows.value = res.data?.data || []
    if (selected.value) {
      const fresh = rows.value.find(r => r.id === selected.value.id)
      if (fresh) selected.value = fresh
    }
  } catch (e) {
    msg.value = e?.message || '加载失败'
  } finally {
    loading.value = false
  }
}

async function openTicket(row) {
  selected.value = row
  try {
    const res = await fetchTicketDetail(row.id)
    const data = res.data?.data || {}
    detail.messages = data.messages || []
    detail.conversationStatus = data.conversationStatus
    if (data.ticket) {
      selected.value = { ...row, ...data.ticket, conversationStatus: data.conversationStatus }
    }
    await nextTick()
    msgEl.value?.scrollTo({ top: msgEl.value.scrollHeight })
  } catch (e) {
    message.error(e?.message || '加载详情失败')
  }
}

async function takeover(id) {
  acting.value = true
  msg.value = ''
  try {
    await takeoverTicket(id, { note: '已人工接管' })
    message.success('已接管')
    await load()
    const row = rows.value.find(r => r.id === id) || selected.value
    if (row) await openTicket(row)
  } catch (e) {
    msg.value = e?.response?.data?.message || e.message
  } finally {
    acting.value = false
  }
}

async function resolve(id) {
  acting.value = true
  msg.value = ''
  try {
    await resolveTicket(id, { note: '已解决' })
    message.success('工单已解决')
    await load()
    const row = rows.value.find(r => r.id === id)
    if (row) await openTicket(row)
  } catch (e) {
    msg.value = e?.response?.data?.message || e.message
  } finally {
    acting.value = false
  }
}

async function sendReply() {
  if (!selected.value || !replyText.value.trim()) return
  replying.value = true
  msg.value = ''
  try {
    await replyTicket(selected.value.id, { content: replyText.value.trim() })
    replyText.value = ''
    await openTicket(selected.value)
    message.success('已发送给买家')
  } catch (e) {
    msg.value = e?.response?.data?.message || e.message
  } finally {
    replying.value = false
  }
}

onMounted(() => {
  load()
  pollTimer = setInterval(() => {
    if (selected.value) openTicket(selected.value)
    else load()
  }, 8000)
})

onBeforeUnmount(() => {
  if (pollTimer) clearInterval(pollTimer)
})
</script>

<style scoped>
.inbox {
  display: grid;
  grid-template-columns: minmax(280px, 340px) 1fr;
  gap: 0;
  min-height: 60vh;
  overflow: hidden;
}

.ticket-list {
  border-right: 1px solid #e4e9f0;
  background: #fafbfc;
  display: flex;
  flex-direction: column;
  max-height: calc(100vh - 160px);
  overflow: hidden;
}

.list-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 12px;
  padding: 16px;
  border-bottom: 1px solid #e4e9f0;
  background: #fff;
}

.list-toolbar h2 {
  margin: 0;
  font-size: 15px;
  color: #182230;
}

.list-toolbar p {
  margin: 4px 0 0;
  font-size: 11px;
  color: #667085;
}

.ticket-card {
  display: block;
  width: 100%;
  text-align: left;
  border: 0;
  border-bottom: 1px solid #eef0f3;
  background: transparent;
  padding: 14px 16px;
  cursor: pointer;
}

.ticket-card:hover {
  background: #f0f6ff;
}

.ticket-card.active {
  background: #e6f4ff;
  border-left: 3px solid #1677ff;
  padding-left: 13px;
}

.ticket-card-top {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 8px;
}

.ticket-card-top strong {
  font-size: 13px;
  color: #182230;
}

.status-tag {
  margin: 0;
}

.ticket-subject {
  margin-top: 6px;
  font-size: 13px;
  color: #1f2937;
  font-weight: 500;
}

.ticket-meta {
  margin-top: 4px;
  font-size: 11px;
  color: #667085;
}

.ticket-summary {
  margin: 8px 0 0;
  font-size: 12px;
  color: #475467;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  line-height: 1.5;
}

.workspace-pane {
  display: flex;
  flex-direction: column;
  min-width: 0;
  background: #fff;
  max-height: calc(100vh - 160px);
}

.pane-header {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  padding: 14px 18px;
  border-bottom: 1px solid #e4e9f0;
}

.pane-header h3 {
  margin: 0;
  font-size: 15px;
  color: #182230;
}

.pane-header p {
  margin: 6px 0 0;
  font-size: 12px;
  color: #667085;
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 4px;
}

.pane-actions {
  display: flex;
  gap: 8px;
  flex-shrink: 0;
}

.messages {
  flex: 1;
  overflow-y: auto;
  padding: 18px;
  background: #f8fafc;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.bubble {
  max-width: 78%;
  padding: 10px 12px;
  border-radius: 8px;
  font-size: 13px;
  line-height: 1.6;
}

.bubble.user {
  align-self: flex-start;
  background: #fff;
  border: 1px solid #e5e7eb;
}

.bubble.assistant {
  align-self: flex-start;
  background: #eef2ff;
  color: #1f2937;
}

.bubble.human {
  align-self: flex-end;
  background: #1677ff;
  color: #fff;
}

.bubble-role {
  font-size: 11px;
  opacity: 0.75;
  margin-bottom: 4px;
}

.composer {
  border-top: 1px solid #e4e9f0;
  padding: 14px 16px;
  background: #fff;
}

.composer-bar {
  margin-top: 10px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
  font-size: 11px;
  color: #98a2b3;
}

.empty,
.empty-pane {
  color: #98a2b3;
  font-size: 13px;
  padding: 32px 16px;
  text-align: center;
}

.empty-pane {
  margin: auto;
}

.empty-pane h3 {
  margin: 0 0 8px;
  color: #344054;
  font-size: 16px;
}

.mt {
  margin: 12px;
}

@media (max-width: 900px) {
  .inbox {
    grid-template-columns: 1fr;
  }
  .ticket-list {
    max-height: 280px;
  }
}
</style>

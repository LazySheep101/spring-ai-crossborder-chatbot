<template>
  <div class="widget-shell">
    <section class="widget">
      <header class="widget-header">
        <div class="widget-brand">
          <span class="widget-mark">XZ</span>
          <div>
            <h1>小智AI 在线客服</h1>
            <p>跨境电商智能客服 · 查单 / 物流 / 退换货</p>
          </div>
        </div>
        <a-tag :color="statusColor">{{ statusText }}</a-tag>
      </header>

      <div v-if="!started" class="session-form">
        <div class="session-title">
          <h2>开始咨询</h2>
          <p>查订单时请准备下单邮箱或手机号，便于身份校验。</p>
        </div>
        <a-input v-model:value="customerContact" size="large" placeholder="邮箱或手机（可选）" />
        <a-input v-model:value="customerName" size="large" placeholder="你的称呼（可选）" />
        <a-button block type="primary" size="large" @click="startSession">开始咨询</a-button>
        <a-button type="link" block @click="router.push('/admin/login')">我是商家，进入运营后台</a-button>
      </div>

      <div v-else class="chat">
        <div ref="messagesEl" class="messages">
          <div
            v-for="(msg, index) in messages"
            :key="index"
            class="msg"
            :class="msg.role"
          >
            <div class="role-label">{{ roleLabel(msg.role) }}</div>
            <div class="msg-body" v-html="renderSafe(msg.content || (streaming && index === messages.length - 1 ? '正在回复…' : ''))"></div>
            <div v-if="msg.citations?.length" class="cite-block">
              <div class="cite-title">政策引用</div>
              <div v-for="(c, ci) in msg.citations" :key="ci" class="cite-item">
                #{{ ci + 1 }} {{ c.text }}
              </div>
            </div>
            <div v-if="msg.actionRequestId" class="card-tip action">
              审批单 #{{ msg.actionRequestId }} · {{ msg.actionType }} · {{ msg.actionStatus }}
            </div>
            <div v-if="msg.ticketNo" class="card-tip ticket">
              人工工单 {{ msg.ticketNo }}
            </div>
          </div>
        </div>
        <footer class="composer">
          <a-input-search
            v-model:value="input"
            enter-button="发送"
            placeholder="请输入你的问题"
            size="large"
            :disabled="streaming"
            :loading="streaming"
            @search="send()"
          />
        </footer>
      </div>
    </section>
  </div>
</template>

<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { fetchEventSource } from '@microsoft/fetch-event-source'
import axios from '@/axios.js'

const router = useRouter()
const started = ref(false)
const streaming = ref(false)
const input = ref('')
const streamText = ref('')
const customerContact = ref('')
const customerName = ref('')
const messages = ref([])
const messagesEl = ref(null)
const chatId = ref(localStorage.getItem('widget_chat_id') || crypto.randomUUID())
localStorage.setItem('widget_chat_id', chatId.value)

let humanPollTimer = null
const seenHumanIds = new Set()

const statusText = computed(() => {
  if (streaming.value) return '回复中'
  if (started.value) return '已连接'
  return '未连接'
})
const statusColor = computed(() => {
  if (streaming.value) return 'gold'
  return started.value ? 'green' : 'default'
})

function renderSafe(text) {
  if (!text) return ''
  return String(text)
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/\n/g, '<br/>')
}

function roleLabel(role) {
  if (role === 'user') return '我'
  if (role === 'human') return '人工客服'
  return '小智AI'
}

function mapServerMessage(m) {
  const role = (m.role || '').toLowerCase()
  if (role === 'user' || role === 'customer') return { role: 'user', content: m.content, id: m.id }
  if (role === 'human' || role === 'agent') return { role: 'human', content: m.content, id: m.id }
  return { role: 'assistant', content: m.content, id: m.id }
}

async function hydrateHistory() {
  try {
    const res = await axios.get(`/customer-service/chat/${chatId.value}/messages`)
    const msgs = res.data?.data || []
    if (!msgs.length) return false
    messages.value = msgs.map(mapServerMessage)
    for (const m of msgs) {
      if ((m.role === 'human' || m.role === 'agent') && m.id) seenHumanIds.add(m.id)
    }
    return true
  } catch (_) {
    return false
  }
}

async function startSession() {
  started.value = true
  const restored = await hydrateHistory()
  if (!restored) {
    const name = customerName.value.trim()
    const welcome = name
      ? `你好 ${name}！我是小智AI，可以帮你查订单、物流、退换货和政策问题。`
      : '你好！我是小智AI，跨境电商智能客服。可以帮你查订单、物流、退换货和政策问题。'
    messages.value = [{ role: 'assistant', content: welcome }]
  }
  await nextTick()
  scrollToBottom()
  if (humanPollTimer) clearInterval(humanPollTimer)
  humanPollTimer = setInterval(pollHumanReplies, 5000)
}

async function pollHumanReplies() {
  try {
    const res = await axios.get(`/customer-service/chat/${chatId.value}/messages`)
    const msgs = res.data?.data || []
    let added = false
    for (const m of msgs) {
      if ((m.role === 'human' || m.role === 'agent') && m.id && !seenHumanIds.has(m.id)) {
        seenHumanIds.add(m.id)
        messages.value.push({ role: 'human', content: m.content, id: m.id })
        added = true
      }
    }
    if (added) scrollToBottom()
  } catch (_) { /* ignore */ }
}

async function send(text) {
  const userText = (text || input.value).trim()
  if (!userText || streaming.value) return
  messages.value.push({ role: 'user', content: userText })
  input.value = ''
  streamText.value = ''
  streaming.value = true
  await nextTick()
  scrollToBottom()

  const last = {
    role: 'assistant',
    content: '',
    citations: [],
    actionRequestId: null,
    actionType: null,
    actionStatus: null,
    ticketNo: null,
  }
  messages.value.push(last)

  const controller = new AbortController()
  try {
    await fetchEventSource('/api/customer-service/chat/completion', {
      method: 'POST',
      signal: controller.signal,
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        message: userText,
        chatId: chatId.value,
        customerContact: customerContact.value || undefined,
      }),
      onmessage(msg) {
        if (msg.event === 'close') {
          streaming.value = false
          controller.abort()
          return
        }
        if (msg.event !== '') return
        const data = JSON.parse(msg.data)
        const type = data.type || 'text'
        if (type === 'blocked') {
          last.content = data.v || data.message || '请求已拦截'
        } else if (type === 'meta') {
          if (data.metaKind === 'citation' && data.citations) last.citations = data.citations
          if (data.metaKind === 'action') {
            last.actionRequestId = data.actionRequestId
            last.actionType = data.actionType
            last.actionStatus = data.actionStatus
          }
          if (data.metaKind === 'ticket') {
            last.ticketNo = data.ticketNo
            last.actionStatus = data.actionStatus
          }
        } else if (data.v) {
          streamText.value += data.v
          last.content = streamText.value
        }
        scrollToBottom()
      },
      onerror() {
        streaming.value = false
        throw new Error('连接中断')
      },
    })
  } catch (e) {
    if (!last.content) last.content = '回复失败，请稍后重试。'
    message.error(e?.message || '回复失败')
  } finally {
    streaming.value = false
    streamText.value = ''
  }
}

function scrollToBottom() {
  messagesEl.value?.scrollTo({ top: messagesEl.value.scrollHeight, behavior: 'smooth' })
}

onMounted(() => {
  document.title = '小智AI · 在线客服'
})

onBeforeUnmount(() => {
  if (humanPollTimer) clearInterval(humanPollTimer)
})
</script>

<style scoped>
.widget-shell {
  background: #f2f5f9;
  display: grid;
  min-height: 100vh;
  padding: 24px;
  place-items: center;
}

.widget {
  background: #fff;
  border: 1px solid #dfe5ed;
  border-radius: 8px;
  display: flex;
  flex-direction: column;
  height: min(720px, calc(100vh - 48px));
  overflow: hidden;
  width: min(440px, 100%);
  box-shadow: 0 12px 32px rgba(16, 24, 40, 0.08);
}

.widget-header {
  align-items: center;
  border-bottom: 1px solid #eef0f3;
  display: flex;
  gap: 12px;
  justify-content: space-between;
  padding: 16px 18px;
}

.widget-brand {
  display: flex;
  align-items: center;
  gap: 10px;
  min-width: 0;
}

.widget-mark {
  width: 34px;
  height: 34px;
  flex: 0 0 34px;
  display: grid;
  place-items: center;
  border-radius: 7px;
  background: #1677ff;
  color: #fff;
  font-size: 11px;
  font-weight: 800;
}

.widget-header h1 {
  color: #1f2937;
  font-size: 15px;
  margin: 0;
}

.widget-header p {
  color: #667085;
  font-size: 11px;
  margin: 3px 0 0;
}

.session-form {
  display: grid;
  align-content: start;
  gap: 12px;
  padding: 26px 22px;
}

.session-title h2 {
  margin: 0;
  color: #111827;
  font-size: 18px;
}

.session-title p {
  margin: 5px 0 0;
  color: #667085;
  font-size: 12px;
}

.chat {
  display: flex;
  flex: 1;
  flex-direction: column;
  min-height: 0;
}

.messages {
  display: flex;
  flex: 1;
  flex-direction: column;
  gap: 12px;
  overflow-y: auto;
  padding: 18px 16px;
  background: #f8fafc;
}

.msg {
  max-width: 88%;
  padding: 10px 12px;
  border-radius: 8px;
  line-height: 1.6;
  font-size: 13px;
}

.msg.user {
  align-self: flex-end;
  background: #1677ff;
  color: #fff;
}

.msg.assistant {
  align-self: flex-start;
  background: #fff;
  border: 1px solid #e5e7eb;
  color: #1f2937;
}

.msg.human {
  align-self: flex-start;
  background: #ecfdf3;
  border: 1px solid #abefc6;
  color: #085d3a;
}

.role-label {
  font-size: 11px;
  opacity: 0.75;
  margin-bottom: 4px;
}

.cite-block {
  margin-top: 8px;
  font-size: 11px;
}

.cite-title {
  color: #667085;
  margin-bottom: 4px;
}

.cite-item {
  background: #f0f5ff;
  border-radius: 6px;
  padding: 6px 8px;
  margin-top: 4px;
  color: #1f2937;
  display: -webkit-box;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.card-tip {
  margin-top: 8px;
  font-size: 11px;
  border-radius: 6px;
  padding: 6px 8px;
}

.card-tip.action {
  background: #e6f4ff;
  color: #0958d9;
}

.card-tip.ticket {
  background: #f8fafc;
  border: 1px solid #e5e7eb;
}

.composer {
  border-top: 1px solid #eef0f3;
  padding: 14px;
  background: #fff;
}

@media (max-width: 640px) {
  .widget-shell {
    padding: 0;
  }
  .widget {
    border-radius: 0;
    height: 100vh;
    width: 100%;
  }
}
</style>

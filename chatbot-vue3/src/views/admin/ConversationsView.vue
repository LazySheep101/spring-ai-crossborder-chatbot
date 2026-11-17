<template>
  <div>
    <div class="toolbar">
      <div>
        <h2>会话记录</h2>
        <p>买家 Widget、收件箱和测试台会话，按最近更新排序</p>
      </div>
      <a-button :loading="loading" @click="load">刷新</a-button>
    </div>
    <a-table
      :dataSource="rows"
      :columns="columns"
      :loading="loading"
      rowKey="chatId"
      size="middle"
      :pagination="{ pageSize: 10 }"
    >
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'status'">
          <a-tag>{{ record.status || '—' }}</a-tag>
        </template>
        <template v-else-if="column.key === 'action'">
          <a-button type="link" size="small" @click="open(record)">查看</a-button>
        </template>
      </template>
    </a-table>
    <div v-if="!loading && !rows.length" class="empty">还没有会话。先从买家 Widget 发一条消息。</div>
    <a-drawer v-model:open="openDrawer" :title="activeId || '会话'" width="520">
      <p class="hint">状态 {{ activeStatus || '—' }}</p>
      <div v-if="!(messages.length || testMessages.length)" class="empty">这条会话还没有落库消息</div>
      <div v-for="msg in messages" :key="'cs-' + msg.id" class="bubble">
        <strong>{{ msg.role }}</strong>
        <p>{{ msg.content }}</p>
      </div>
      <div v-for="msg in testMessages" :key="'t-' + msg.id" class="bubble">
        <strong>{{ msg.role }} · 测试台</strong>
        <p>{{ msg.content }}</p>
      </div>
    </a-drawer>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { message } from 'ant-design-vue'
import { fetchConversation, fetchConversations } from '@/api/admin'

const rows = ref([])
const loading = ref(false)
const openDrawer = ref(false)
const activeId = ref('')
const activeStatus = ref('')
const messages = ref([])
const testMessages = ref([])

const columns = [
  { title: '会话', dataIndex: 'chatId', key: 'chatId', ellipsis: true },
  { title: '来源', dataIndex: 'source', key: 'source', width: 90 },
  { title: '状态', dataIndex: 'status', key: 'status', width: 140 },
  { title: '摘要', dataIndex: 'summary', key: 'summary', ellipsis: true },
  { title: '最近一句', dataIndex: 'lastMessage', key: 'lastMessage', ellipsis: true },
  { title: '条数', dataIndex: 'messageCount', key: 'messageCount', width: 70 },
  { title: '', key: 'action', width: 80 },
]

async function load() {
  loading.value = true
  try {
    const res = await fetchConversations()
    rows.value = res.data?.data || []
  } catch (e) {
    message.error(e?.message || '加载失败')
  } finally {
    loading.value = false
  }
}

async function open(record) {
  activeId.value = record.chatId
  activeStatus.value = record.status
  openDrawer.value = true
  messages.value = []
  testMessages.value = []
  try {
    const res = await fetchConversation(record.chatId)
    messages.value = res.data?.data?.messages || []
    testMessages.value = res.data?.data?.testMessages || []
    activeStatus.value = res.data?.data?.status || record.status
  } catch (e) {
    message.error(e?.message || '加载会话失败')
  }
}

onMounted(load)
</script>

<style scoped>
.toolbar { display: flex; justify-content: space-between; margin-bottom: 16px; }
.toolbar h2 { margin: 0; font-size: 16px; color: #182230; }
.toolbar p { margin: 4px 0 0; font-size: 12px; color: #667085; }
.empty { color: #98a2b3; text-align: center; padding: 24px; }
.hint { color: #667085; font-size: 12px; }
.bubble { border-bottom: 1px solid #eef2f6; padding: 10px 0; }
.bubble p { margin: 4px 0 0; white-space: pre-wrap; color: #344054; }
</style>

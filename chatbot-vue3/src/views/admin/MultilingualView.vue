<template>
  <div>
    <div class="toolbar">
      <div>
        <h2>多语言证据</h2>
        <p>入站语种检测，以及 translateSnippet 的术语命中或原文回退。没有外部翻译 API。</p>
      </div>
      <a-button :loading="loading" @click="load">刷新</a-button>
    </div>
    <h3>语言事件</h3>
    <a-table
      :dataSource="events"
      :columns="eventColumns"
      :loading="loading"
      rowKey="id"
      size="middle"
      :pagination="{ pageSize: 8 }"
    >
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'fallback'">
          <a-tag :color="record.fallback ? 'orange' : 'green'">{{ record.fallback ? '回退原文' : '未回退' }}</a-tag>
        </template>
      </template>
    </a-table>
    <div v-if="!loading && !events.length" class="empty">还没有语言事件。在 Widget 发一条中文或英文消息后再看。</div>
    <h3>最近 Run 的语种</h3>
    <a-table
      :dataSource="runs"
      :columns="runColumns"
      :loading="loading"
      rowKey="runId"
      size="middle"
      :pagination="{ pageSize: 8 }"
    />
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { message } from 'ant-design-vue'
import { fetchMultilingual } from '@/api/admin'

const events = ref([])
const runs = ref([])
const loading = ref(false)
const eventColumns = [
  { title: '时间', dataIndex: 'createTime', key: 'createTime', width: 180 },
  { title: '会话', dataIndex: 'conversationId', key: 'conversationId', ellipsis: true },
  { title: '源', dataIndex: 'sourceLang', key: 'sourceLang', width: 70 },
  { title: '目标', dataIndex: 'targetLang', key: 'targetLang', width: 70 },
  { title: '来源', dataIndex: 'toolName', key: 'toolName', width: 140 },
  { title: '原因', dataIndex: 'reason', key: 'reason', width: 160 },
  { title: '回退', dataIndex: 'fallback', key: 'fallback', width: 100 },
  { title: '片段', dataIndex: 'snippet', key: 'snippet', ellipsis: true },
]
const runColumns = [
  { title: '时间', dataIndex: 'createTime', key: 'createTime', width: 180 },
  { title: '语种', dataIndex: 'sourceLang', key: 'sourceLang', width: 80 },
  { title: '分流', dataIndex: 'specialist', key: 'specialist', width: 120 },
  { title: '问题', dataIndex: 'userMessage', key: 'userMessage' },
]

async function load() {
  loading.value = true
  try {
    const res = await fetchMultilingual()
    events.value = res.data?.data?.events || []
    runs.value = res.data?.data?.runs || []
  } catch (e) {
    message.error(e?.message || '加载失败')
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.toolbar { display: flex; justify-content: space-between; margin-bottom: 16px; }
.toolbar h2 { margin: 0; font-size: 16px; color: #182230; }
.toolbar p { margin: 4px 0 0; font-size: 12px; color: #667085; }
h3 { margin: 18px 0 8px; font-size: 14px; color: #344054; }
.empty { color: #98a2b3; padding: 12px 0 4px; }
</style>

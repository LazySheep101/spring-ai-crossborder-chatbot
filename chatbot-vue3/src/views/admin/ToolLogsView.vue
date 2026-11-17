<template>
  <div>
    <div class="toolbar">
      <div>
        <h2>工具调用</h2>
        <p>工具审计日志 · 失败可用于排查 Agent 归因</p>
      </div>
      <a-button :loading="loading" @click="load">刷新</a-button>
    </div>
    <a-table
      :dataSource="rows"
      :columns="columns"
      :loading="loading"
      rowKey="id"
      size="middle"
      :pagination="{ pageSize: 12 }"
      :scroll="{ x: 960 }"
    >
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'success'">
          <a-tag :color="record.success ? 'green' : 'red'">{{ record.success ? '成功' : '失败' }}</a-tag>
        </template>
        <template v-else-if="column.key === 'latencyMs'">
          {{ record.latencyMs != null ? `${record.latencyMs}ms` : '-' }}
        </template>
      </template>
    </a-table>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { message } from 'ant-design-vue'
import { fetchToolLogs } from '@/api/admin'

const rows = ref([])
const loading = ref(false)
const columns = [
  { title: '时间', dataIndex: 'createTime', key: 'createTime', width: 170 },
  { title: '工具', dataIndex: 'toolName', key: 'toolName', width: 160 },
  { title: '结果', key: 'success', width: 90 },
  { title: '耗时', key: 'latencyMs', width: 90 },
  { title: '参数', dataIndex: 'argsSummary', key: 'argsSummary', ellipsis: true },
  { title: '结果摘要', dataIndex: 'resultSummary', key: 'resultSummary', ellipsis: true },
]

async function load() {
  loading.value = true
  try {
    const res = await fetchToolLogs(80)
    rows.value = res.data?.data || []
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
</style>

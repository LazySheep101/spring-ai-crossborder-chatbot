<template>
  <div>
    <div class="toolbar">
      <div>
        <h2>审计</h2>
        <p>审批、工具调用和收件箱接管/结单。单店演示，不是平台级审计中心。</p>
      </div>
      <a-button :loading="loading" @click="load">刷新</a-button>
    </div>
    <a-table
      :dataSource="rows"
      :columns="columns"
      :loading="loading"
      :rowKey="rowKey"
      size="middle"
      :pagination="{ pageSize: 12 }"
    >
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'success'">
          <a-tag :color="record.success === false ? 'red' : 'green'">{{ record.success === false ? '失败' : '成功' }}</a-tag>
        </template>
      </template>
    </a-table>
    <div v-if="!loading && !rows.length" class="empty">还没有审计记录</div>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { message } from 'ant-design-vue'
import { fetchAudit } from '@/api/admin'

const rows = ref([])
const loading = ref(false)
const columns = [
  { title: '时间', dataIndex: 'time', key: 'time', width: 180 },
  { title: '类型', dataIndex: 'type', key: 'type', width: 110 },
  { title: '操作者', dataIndex: 'actor', key: 'actor', width: 140 },
  { title: '摘要', dataIndex: 'summary', key: 'summary' },
  { title: '结果', dataIndex: 'success', key: 'success', width: 90 },
]

function rowKey(row) {
  return `${row.time}-${row.type}-${row.summary}`
}

async function load() {
  loading.value = true
  try {
    const res = await fetchAudit()
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
.toolbar { display: flex; justify-content: space-between; margin-bottom: 16px; }
.toolbar h2 { margin: 0; font-size: 16px; color: #182230; }
.toolbar p { margin: 4px 0 0; font-size: 12px; color: #667085; }
.empty { color: #98a2b3; text-align: center; padding: 24px; }
</style>

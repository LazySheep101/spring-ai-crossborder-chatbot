<template>
  <div>
    <div class="toolbar">
      <div>
        <h2>知识库</h2>
        <p>在后台直接上传政策文档，合并后异步向量化。中英 FAQ 已内置，不依赖这次上传。</p>
      </div>
      <div class="ops">
        <a-button type="primary" :loading="uploading" @click="pick">上传文档</a-button>
        <a-button :loading="loading" @click="load">刷新</a-button>
        <input ref="fileInput" type="file" accept=".md,.markdown,.pdf,.doc,.docx,.txt" hidden @change="onFile" />
      </div>
    </div>
    <p v-if="progressText" class="progress">{{ progressText }}</p>
    <a-table
      :dataSource="rows"
      :columns="columns"
      :loading="loading"
      rowKey="id"
      size="middle"
      :pagination="{ pageSize: 10 }"
    >
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'name'">
          {{ record.originalFileName || record.fileName || '-' }}
        </template>
        <template v-else-if="column.key === 'status'">
          <a-tag :color="statusColor(record.status)">{{ statusLabel(record.status) }}</a-tag>
        </template>
        <template v-else-if="column.key === 'action'">
          <a-button type="link" danger size="small" @click="remove(record)">删除</a-button>
        </template>
      </template>
    </a-table>
    <div v-if="!loading && !rows.length" class="empty">还没有上传文档</div>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { message } from 'ant-design-vue'
import { deleteMarkdownFile, fetchFileList } from '@/api/admin'
import { uploadKnowledgeFile } from '@/utils/chunkUpload'

const rows = ref([])
const loading = ref(false)
const uploading = ref(false)
const progressText = ref('')
const fileInput = ref(null)

const columns = [
  { title: 'ID', dataIndex: 'id', key: 'id', width: 70 },
  { title: '文件名', key: 'name' },
  { title: '状态', dataIndex: 'status', key: 'status', width: 120 },
  { title: '大小', dataIndex: 'fileSize', key: 'fileSize', width: 120 },
  { title: '更新时间', dataIndex: 'updateTime', key: 'updateTime', width: 180 },
  { title: '操作', key: 'action', width: 90 },
]

const STATUS = {
  0: { label: '上传中', color: 'default' },
  1: { label: '待处理', color: 'processing' },
  2: { label: '向量化中', color: 'processing' },
  3: { label: '已完成', color: 'green' },
  4: { label: '失败', color: 'red' },
}

function statusLabel(status) {
  return STATUS[status]?.label || status || '-'
}
function statusColor(status) {
  return STATUS[status]?.color || 'default'
}

function pick() {
  fileInput.value?.click()
}

async function onFile(event) {
  const file = event.target.files?.[0]
  event.target.value = ''
  if (!file) return
  uploading.value = true
  progressText.value = '计算 MD5…'
  try {
    await uploadKnowledgeFile(file, (percent, text) => {
      progressText.value = `${text}（${percent}%）`
    })
    message.success('上传完成，正在向量化')
    await load()
  } catch (e) {
    message.error(e?.message || '上传失败')
  } finally {
    uploading.value = false
  }
}

async function remove(record) {
  try {
    const res = await deleteMarkdownFile(record.id)
    if (res.data?.success === false) {
      message.error(res.data?.message || '删除失败')
      return
    }
    message.success('已删除')
    await load()
  } catch (e) {
    message.error(e?.message || '删除失败')
  }
}

async function load() {
  loading.value = true
  try {
    const res = await fetchFileList({ current: 1, size: 50 })
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
.toolbar { display: flex; justify-content: space-between; align-items: flex-start; margin-bottom: 16px; gap: 12px; }
.toolbar h2 { margin: 0; font-size: 16px; color: #182230; }
.toolbar p { margin: 4px 0 0; font-size: 12px; color: #667085; }
.ops { display: flex; gap: 8px; }
.progress { margin: 0 0 12px; font-size: 12px; color: #175cd3; }
.empty { color: #98a2b3; text-align: center; padding: 24px; }
</style>

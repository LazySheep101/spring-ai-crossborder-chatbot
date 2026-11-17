<template>
  <div class="traces">
    <aside class="run-list">
      <div class="toolbar">
        <div>
          <h2>轨迹回放</h2>
          <p>Agent Run 证据链</p>
        </div>
        <a-button size="small" :loading="loading" @click="load">刷新</a-button>
      </div>
      <a-spin :spinning="loading">
        <button
          v-for="row in runs"
          :key="row.runId"
          type="button"
          class="run-card"
          :class="{ active: selected === row.runId }"
          @click="open(row.runId)"
        >
          <div class="run-top">
            <strong>{{ shortId(row.runId) }}</strong>
            <a-tag :color="row.status === 'FAILED' ? 'red' : 'blue'">{{ row.status || 'OK' }}</a-tag>
          </div>
          <div class="meta">{{ row.specialist || '-' }} · {{ row.sourceLang || '-' }}</div>
          <p>{{ row.userMessage || '无用户消息摘要' }}</p>
        </button>
        <div v-if="!runs.length" class="empty">暂无 Run</div>
      </a-spin>
    </aside>
    <section class="steps-pane">
      <h3>Steps</h3>
      <div v-if="!selected" class="empty">选择一次 Run 查看 triage / tool / response</div>
      <ol v-else class="steps">
        <li
          v-for="(s, i) in steps"
          :key="s.id || i"
          :class="{ fail: s.success === false }"
        >
          <div class="step-title">
            #{{ i + 1 }} {{ s.stepType }} / {{ s.name }}
            <span>{{ s.latencyMs != null ? `${s.latencyMs}ms` : '' }}</span>
          </div>
          <pre v-if="s.detail">{{ s.detail }}</pre>
          <p v-if="s.error" class="err">{{ s.error }}</p>
        </li>
      </ol>
      <div v-if="selected && !steps.length" class="empty">该 Run 暂无步骤</div>
    </section>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { message } from 'ant-design-vue'
import { fetchTraces, fetchTraceDetail } from '@/api/admin'

const runs = ref([])
const steps = ref([])
const selected = ref('')
const loading = ref(false)

function shortId(id) {
  if (!id) return '-'
  return id.length > 10 ? `${id.slice(0, 8)}…` : id
}

async function load() {
  loading.value = true
  try {
    const res = await fetchTraces(50)
    runs.value = res.data?.data || []
  } catch (e) {
    message.error(e?.message || '加载失败')
  } finally {
    loading.value = false
  }
}

async function open(runId) {
  selected.value = runId
  try {
    const res = await fetchTraceDetail(runId)
    steps.value = res.data?.data?.steps || []
  } catch (e) {
    message.error(e?.message || '加载详情失败')
  }
}

onMounted(load)
</script>

<style scoped>
.traces {
  display: grid;
  grid-template-columns: minmax(260px, 340px) 1fr;
  gap: 0;
  min-height: 60vh;
}
.run-list {
  border-right: 1px solid #e4e9f0;
  background: #fafbfc;
  max-height: calc(100vh - 160px);
  overflow: auto;
}
.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  padding: 14px 16px;
  border-bottom: 1px solid #e4e9f0;
  background: #fff;
  position: sticky;
  top: 0;
}
.toolbar h2 { margin: 0; font-size: 15px; }
.toolbar p { margin: 4px 0 0; font-size: 11px; color: #667085; }
.run-card {
  display: block;
  width: 100%;
  text-align: left;
  border: 0;
  border-bottom: 1px solid #eef0f3;
  background: transparent;
  padding: 12px 16px;
  cursor: pointer;
}
.run-card:hover { background: #f0f6ff; }
.run-card.active { background: #e6f4ff; border-left: 3px solid #1677ff; padding-left: 13px; }
.run-top { display: flex; justify-content: space-between; align-items: center; gap: 8px; }
.meta { margin-top: 4px; font-size: 11px; color: #667085; }
.run-card p {
  margin: 6px 0 0;
  font-size: 12px;
  color: #344054;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
.steps-pane { padding: 16px 18px; overflow: auto; max-height: calc(100vh - 160px); }
.steps-pane h3 { margin: 0 0 12px; font-size: 15px; }
.steps { list-style: none; margin: 0; padding: 0; display: grid; gap: 12px; }
.steps li {
  border-left: 3px solid #52c41a;
  padding: 8px 0 8px 12px;
  background: #fafbfc;
  border-radius: 0 8px 8px 0;
}
.steps li.fail { border-left-color: #ff4d4f; }
.step-title {
  font-size: 13px;
  font-weight: 600;
  color: #182230;
  display: flex;
  justify-content: space-between;
  gap: 8px;
}
.step-title span { font-weight: 400; color: #98a2b3; font-size: 11px; }
pre {
  margin: 8px 0 0;
  white-space: pre-wrap;
  font-size: 11px;
  color: #475467;
  background: #fff;
  border: 1px solid #eef0f3;
  border-radius: 6px;
  padding: 8px;
}
.err { margin: 6px 0 0; color: #c01b2e; font-size: 12px; }
.empty { color: #98a2b3; padding: 24px 8px; font-size: 13px; text-align: center; }
@media (max-width: 900px) {
  .traces { grid-template-columns: 1fr; }
}
</style>

<template>
  <div class="rag">
    <div class="toolbar">
      <div>
        <h2>RAG 工作台</h2>
        <p>切换检索模式，查看命中与「够不够回答」标记</p>
      </div>
    </div>
    <div class="query-bar">
      <a-input v-model:value="question" size="large" placeholder="输入政策问题，如：怎么退货？" @pressEnter="run" />
      <a-select v-model:value="mode" style="width: 160px" size="large">
        <a-select-option value="current">current</a-select-option>
        <a-select-option value="vector">vector</a-select-option>
        <a-select-option value="hybrid">hybrid</a-select-option>
        <a-select-option value="rerank">hybrid+rerank</a-select-option>
      </a-select>
      <a-button type="primary" size="large" :loading="loading" @click="run">检索</a-button>
    </div>
    <div v-if="result" class="summary">
      模式 <strong>{{ result.mode }}</strong>
      · 命中 <strong>{{ result.hits }}</strong>
      ·
      <a-tag :color="result.enoughToAnswer ? 'green' : 'orange'">
        {{ result.enoughToAnswer ? '够回答' : '可能不够回答' }}
      </a-tag>
    </div>
    <div v-for="(row, idx) in (result?.documents || [])" :key="idx" class="hit">
      <div class="hit-meta">
        #{{ idx + 1 }} · id={{ row.id }}
        · rerank={{ row.rerankScore ?? '-' }}
        · rrf={{ row.rrfScore ?? '-' }}
      </div>
      <pre>{{ row.text }}</pre>
    </div>
    <div v-if="result && !(result.documents || []).length" class="empty">无命中文档</div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { message } from 'ant-design-vue'
import { ragDebug } from '@/api/admin'

const question = ref('怎么退货退款？')
const mode = ref('rerank')
const result = ref(null)
const loading = ref(false)

async function run() {
  if (!question.value.trim()) return
  loading.value = true
  try {
    const res = await ragDebug(question.value, mode.value)
    result.value = res.data?.data || null
  } catch (e) {
    message.error(e?.message || '检索失败')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.toolbar { margin-bottom: 14px; }
.toolbar h2 { margin: 0; font-size: 16px; color: #182230; }
.toolbar p { margin: 4px 0 0; font-size: 12px; color: #667085; }
.query-bar {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
  margin-bottom: 14px;
}
.query-bar .ant-input { flex: 1; min-width: 220px; }
.summary {
  margin-bottom: 14px;
  font-size: 13px;
  color: #475467;
}
.hit {
  border: 1px solid #e4e9f0;
  border-radius: 10px;
  padding: 12px 14px;
  margin-bottom: 10px;
  background: #fafbfc;
}
.hit-meta { font-size: 11px; color: #667085; margin-bottom: 8px; }
pre {
  margin: 0;
  white-space: pre-wrap;
  font-size: 12px;
  color: #344054;
  line-height: 1.6;
}
.empty { color: #98a2b3; text-align: center; padding: 24px; }
</style>

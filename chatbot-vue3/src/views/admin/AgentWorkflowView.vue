<template>
  <div>
    <div class="toolbar">
      <div>
        <h2>智能体工作流</h2>
        <p>关键词分流后的 specialist 和工具白名单。写工具不在默认分流里。</p>
      </div>
      <a-button :loading="loading" @click="load">刷新</a-button>
    </div>
    <div v-if="!loading && !rows.length" class="empty">没有工作流说明</div>
    <article v-for="row in rows" :key="row.specialistKey" class="card">
      <header>
        <strong>{{ row.label }}</strong>
        <a-tag>{{ row.riskLevel }}</a-tag>
      </header>
      <p>何时进入：{{ row.when }}</p>
      <p>边界：{{ row.boundary }}</p>
      <div class="tools">
        <a-tag v-for="tool in row.tools" :key="tool">{{ tool }}</a-tag>
      </div>
    </article>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { message } from 'ant-design-vue'
import { fetchAgentWorkflow } from '@/api/admin'

const rows = ref([])
const loading = ref(false)

async function load() {
  loading.value = true
  try {
    const res = await fetchAgentWorkflow()
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
.card { border: 1px solid #e4e9f0; border-radius: 10px; padding: 14px; margin-bottom: 10px; background: #fff; }
.card header { display: flex; justify-content: space-between; align-items: center; }
.card p { margin: 8px 0 0; font-size: 13px; color: #475467; }
.tools { margin-top: 8px; display: flex; flex-wrap: wrap; gap: 6px; }
.empty { color: #98a2b3; text-align: center; padding: 24px; }
</style>

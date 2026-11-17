<template>
  <div>
    <div class="toolbar">
      <div>
        <h2>智能体评测</h2>
        <p>进程内契约：路由白名单、写工具 PENDING 门禁、安全门、双语 FAQ、翻译回退。不调用外部模型。</p>
      </div>
      <a-button type="primary" :loading="loading" @click="load">重新跑</a-button>
    </div>
    <p v-if="summary.headline" class="headline">{{ summary.headline }}</p>
    <div class="metrics">
      <div class="metric"><span>路由</span><strong>{{ summary.routePass ?? '-' }}/{{ summary.routeTotal ?? '-' }}</strong></div>
      <div class="metric"><span>审批工具覆盖</span><strong>{{ summary.approvalOk ?? '-' }}/{{ summary.approvalExpect ?? '-' }}</strong></div>
      <div class="metric"><span>写工具 PENDING</span><strong>{{ summary.writePass ?? '-' }}/{{ summary.writeTotal ?? '-' }}</strong></div>
      <div class="metric"><span>安全门</span><strong>{{ summary.safetyPass ?? '-' }}/{{ summary.safetyTotal ?? '-' }}</strong></div>
      <div class="metric"><span>语种</span><strong>{{ summary.langPass ?? '-' }}/{{ summary.langTotal ?? '-' }}</strong></div>
      <div class="metric"><span>双语 FAQ</span><strong>{{ summary.faqPass ?? '-' }}/{{ summary.faqTotal ?? '-' }}</strong></div>
      <div class="metric"><span>翻译回退</span><strong>{{ summary.translatePass ?? '-' }}/{{ summary.translateTotal ?? '-' }}</strong></div>
    </div>
    <p class="note">{{ summary.note }}</p>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { message } from 'ant-design-vue'
import { fetchEvalSummary } from '@/api/admin'

const loading = ref(false)
const summary = reactive({})

async function load() {
  loading.value = true
  try {
    const res = await fetchEvalSummary()
    Object.assign(summary, res.data?.data || {})
  } catch (e) {
    message.error(e?.message || '评测失败')
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.toolbar { display: flex; justify-content: space-between; margin-bottom: 16px; gap: 12px; }
.toolbar h2 { margin: 0; font-size: 16px; color: #182230; }
.toolbar p { margin: 4px 0 0; font-size: 12px; color: #667085; max-width: 640px; }
.headline { font-size: 13px; color: #175cd3; }
.metrics { display: grid; grid-template-columns: repeat(auto-fill, minmax(140px, 1fr)); gap: 10px; margin: 12px 0; }
.metric { background: #f8fafc; border: 1px solid #e4e9f0; border-radius: 10px; padding: 12px; }
.metric span { display: block; font-size: 12px; color: #667085; }
.metric strong { font-size: 18px; color: #182230; }
.note { font-size: 12px; color: #667085; }
</style>

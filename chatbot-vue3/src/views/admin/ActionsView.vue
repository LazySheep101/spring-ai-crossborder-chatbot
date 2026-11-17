<template>
  <div class="actions">
    <div class="toolbar">
      <div>
        <h2>动作审批</h2>
        <p>退货 / 退款 / 换货 / 改地址 · 需主管角色</p>
      </div>
      <a-select v-model:value="status" style="width: 140px" @change="load">
        <a-select-option value="">全部</a-select-option>
        <a-select-option value="PENDING">待审批</a-select-option>
        <a-select-option value="APPROVED">已通过</a-select-option>
        <a-select-option value="REJECTED">已驳回</a-select-option>
      </a-select>
    </div>

    <a-spin :spinning="loading">
      <div v-if="!rows.length" class="empty">暂无审批单</div>
      <div v-for="row in rows" :key="row.id" class="action-card">
        <div class="action-main">
          <div class="action-title">
            <strong>#{{ row.id }} · {{ row.actionType }}</strong>
            <a-tag :color="statusColor(row.status)">{{ statusLabel(row.status) }}</a-tag>
          </div>
          <div class="meta">订单 {{ row.orderNo }} · {{ row.customerContact || '-' }}</div>
          <p class="reason">{{ row.reason || '无原因说明' }}</p>
          <div v-if="row.reviewNote" class="note">审批备注：{{ row.reviewNote }} · {{ row.reviewedBy || '' }}</div>
        </div>
        <div v-if="row.status === 'PENDING'" class="action-ops">
          <a-button type="primary" :loading="busyId === row.id" @click="review(row.id, true)">通过</a-button>
          <a-button danger :loading="busyId === row.id" @click="review(row.id, false)">驳回</a-button>
        </div>
      </div>
    </a-spin>
    <a-alert v-if="msg" type="warning" show-icon :message="msg" closable class="mt" @close="msg = ''" />
  </div>
</template>

<script setup>
import { onMounted, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { message } from 'ant-design-vue'
import { fetchActions, reviewAction } from '@/api/admin'

const route = useRoute()
const rows = ref([])
const queryStatus = Array.isArray(route.query.status) ? route.query.status[0] : route.query.status
const status = ref(queryStatus || 'PENDING')
const msg = ref('')
const loading = ref(false)
const busyId = ref(null)

watch(
  () => route.query.status,
  (v) => {
    const next = Array.isArray(v) ? v[0] : v
    status.value = next || ''
    load()
  }
)

function statusLabel(s) {
  return ({ PENDING: '待审批', APPROVED: '已通过', REJECTED: '已驳回' })[s] || s
}
function statusColor(s) {
  return ({ PENDING: 'orange', APPROVED: 'green', REJECTED: 'red' })[s] || 'default'
}

async function load() {
  loading.value = true
  try {
    const res = await fetchActions(status.value || undefined)
    rows.value = res.data?.data || []
  } catch (e) {
    msg.value = e?.message || '加载失败'
  } finally {
    loading.value = false
  }
}

async function review(id, approve) {
  msg.value = ''
  busyId.value = id
  try {
    const res = await reviewAction(id, { approve, note: approve ? '主管通过' : '主管驳回' })
    if (!res.data?.success) {
      msg.value = res.data?.message || '审批失败'
      return
    }
    message.success(approve ? '已通过' : '已驳回')
    await load()
  } catch (e) {
    msg.value = e?.response?.data?.message || e.message
  } finally {
    busyId.value = null
  }
}

onMounted(load)
</script>

<style scoped>
.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 12px;
  margin-bottom: 16px;
}
.toolbar h2 {
  margin: 0;
  font-size: 16px;
  color: #182230;
}
.toolbar p {
  margin: 4px 0 0;
  font-size: 12px;
  color: #667085;
}
.action-card {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  border: 1px solid #e4e9f0;
  border-radius: 10px;
  padding: 14px 16px;
  margin-bottom: 10px;
  background: #fafbfc;
}
.action-title {
  display: flex;
  align-items: center;
  gap: 8px;
}
.meta {
  margin-top: 6px;
  font-size: 12px;
  color: #667085;
}
.reason {
  margin: 8px 0 0;
  font-size: 13px;
  color: #344054;
}
.note {
  margin-top: 8px;
  font-size: 12px;
  color: #667085;
}
.action-ops {
  display: flex;
  gap: 8px;
  flex-shrink: 0;
  align-items: flex-start;
}
.empty {
  color: #98a2b3;
  padding: 32px;
  text-align: center;
}
.mt {
  margin-top: 12px;
}
</style>

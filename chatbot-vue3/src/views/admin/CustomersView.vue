<template>
  <div>
    <div class="toolbar">
      <div>
        <h2>客户</h2>
        <p>种子客户与订单关联，用于查单身份校验演示</p>
      </div>
      <a-button @click="load" :loading="loading">刷新</a-button>
    </div>
    <a-table
      :dataSource="rows"
      :columns="columns"
      :loading="loading"
      rowKey="id"
      size="middle"
      :pagination="{ pageSize: 10 }"
    />
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { message } from 'ant-design-vue'
import { fetchCustomers } from '@/api/admin'

const loading = ref(false)
const rows = ref([])
const columns = [
  { title: 'ID', dataIndex: 'id', key: 'id', width: 70 },
  { title: '姓名', dataIndex: 'name', key: 'name' },
  { title: '邮箱', dataIndex: 'email', key: 'email' },
  { title: '手机', dataIndex: 'phone', key: 'phone' },
  { title: '订单数', dataIndex: 'orderCount', key: 'orderCount', width: 90 },
  { title: '最近订单', dataIndex: 'latestOrderNo', key: 'latestOrderNo' },
]

async function load() {
  loading.value = true
  try {
    const res = await fetchCustomers()
    if (res.data?.success) {
      rows.value = res.data.data || []
    } else {
      message.error(res.data?.message || '加载失败')
    }
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
</style>

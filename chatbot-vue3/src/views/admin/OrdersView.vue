<template>
  <div class="orders">
    <div class="toolbar">
      <div>
        <h2>订单</h2>
        <p>演示订单与履约数据，点击行查看明细</p>
      </div>
      <a-button :loading="loading" @click="load">刷新</a-button>
    </div>
    <a-table
      :dataSource="rows"
      :columns="columns"
      :loading="loading"
      rowKey="id"
      size="middle"
      :pagination="{ pageSize: 8 }"
      :customRow="(record) => ({ onClick: () => open(record) })"
      :rowClassName="(record) => selected?.id === record.id ? 'row-active' : 'row-click'"
    >
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'customer'">
          {{ record.customerName || '-' }} / {{ record.customerEmail || '-' }}
        </template>
        <template v-else-if="column.key === 'amount'">
          {{ record.totalAmount ?? '-' }} {{ record.currency || '' }}
        </template>
      </template>
    </a-table>
    <div v-if="detail" class="detail">
      <div class="detail-head">
        <h3>订单 {{ detail.order?.orderNo }}</h3>
        <a-tag>{{ detail.order?.status }}</a-tag>
      </div>
      <div class="detail-grid">
        <div><span>客户</span><strong>{{ detail.customer?.name }} · {{ detail.customer?.email }}</strong></div>
        <div><span>金额</span><strong>{{ detail.order?.totalAmount }} {{ detail.order?.currency }}</strong></div>
        <div><span>地址</span><strong>{{ detail.order?.shippingAddress || '-' }}</strong></div>
        <div><span>物流</span><strong>{{ detail.shipment?.carrier || '-' }} · {{ detail.shipment?.trackingNo || '-' }} · {{ detail.shipment?.status || '-' }}</strong></div>
      </div>
      <a-table
        v-if="detail.items?.length"
        :dataSource="detail.items"
        :columns="itemColumns"
        rowKey="id"
        size="small"
        :pagination="false"
        class="mt"
      />
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { message } from 'ant-design-vue'
import { fetchOrders, fetchOrderDetail } from '@/api/admin'

const rows = ref([])
const selected = ref(null)
const detail = ref(null)
const loading = ref(false)
const columns = [
  { title: '订单号', dataIndex: 'orderNo', key: 'orderNo' },
  { title: '客户', key: 'customer' },
  { title: '状态', dataIndex: 'status', key: 'status', width: 110 },
  { title: '金额', key: 'amount', width: 120 },
  { title: '地址', dataIndex: 'shippingAddress', key: 'shippingAddress', ellipsis: true },
]
const itemColumns = [
  { title: 'SKU', dataIndex: 'sku', key: 'sku' },
  { title: '名称', dataIndex: 'productName', key: 'productName' },
  { title: '数量', dataIndex: 'quantity', key: 'quantity', width: 80 },
  { title: '单价', dataIndex: 'unitPrice', key: 'unitPrice', width: 100 },
]

async function load() {
  loading.value = true
  try {
    const res = await fetchOrders()
    rows.value = res.data?.data || []
  } catch (e) {
    message.error(e?.message || '加载失败')
  } finally {
    loading.value = false
  }
}

async function open(row) {
  selected.value = row
  try {
    const res = await fetchOrderDetail(row.orderNo)
    detail.value = res.data?.data || null
  } catch (e) {
    message.error(e?.message || '加载详情失败')
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
.detail {
  margin-top: 16px;
  border: 1px solid #e4e9f0;
  border-radius: 10px;
  padding: 14px 16px;
  background: #fafbfc;
}
.detail-head {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 12px;
}
.detail-head h3 { margin: 0; font-size: 15px; }
.detail-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
  font-size: 13px;
}
.detail-grid span {
  display: block;
  color: #667085;
  font-size: 11px;
  margin-bottom: 2px;
}
.detail-grid strong {
  font-weight: 500;
  color: #182230;
}
.mt { margin-top: 12px; }
:deep(.row-click) { cursor: pointer; }
:deep(.row-active) { background: #e6f4ff !important; }
</style>

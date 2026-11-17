<template>
  <div>
    <div class="toolbar">
      <h2>商品</h2>
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
import { fetchProducts } from '@/api/admin'

const loading = ref(false)
const rows = ref([])
const columns = [
  { title: 'SKU', dataIndex: 'sku', key: 'sku' },
  { title: '名称', dataIndex: 'name', key: 'name' },
  { title: '分类', dataIndex: 'category', key: 'category' },
  { title: '价格', dataIndex: 'price', key: 'price' },
  { title: '库存', dataIndex: 'stock', key: 'stock' },
]

async function load() {
  loading.value = true
  try {
    const res = await fetchProducts()
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
  align-items: center;
  margin-bottom: 16px;
}
.toolbar h2 {
  margin: 0;
  font-size: 16px;
}
</style>

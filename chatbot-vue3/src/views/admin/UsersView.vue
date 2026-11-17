<template>
  <div>
    <div class="toolbar">
      <div>
        <h2>账号说明</h2>
        <p>单店演示账号。supervisor 可以审批，agent 只能处理收件箱。</p>
      </div>
      <a-button :loading="loading" @click="load">刷新</a-button>
    </div>
    <div class="roles">
      <div v-for="role in roles" :key="role.role" class="role">
        <strong>{{ role.role }}</strong>
        <span>{{ role.can }}</span>
      </div>
    </div>
    <a-table
      :dataSource="users"
      :columns="columns"
      :loading="loading"
      rowKey="id"
      size="middle"
      :pagination="false"
    />
    <div v-if="!loading && !users.length" class="empty">还没有后台账号</div>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { message } from 'ant-design-vue'
import { fetchUsers } from '@/api/admin'

const users = ref([])
const roles = ref([])
const loading = ref(false)
const columns = [
  { title: '用户名', dataIndex: 'username', key: 'username' },
  { title: '显示名', dataIndex: 'displayName', key: 'displayName' },
  { title: '角色', dataIndex: 'role', key: 'role' },
  { title: '演示密码', dataIndex: 'demoPassword', key: 'demoPassword' },
]

async function load() {
  loading.value = true
  try {
    const res = await fetchUsers()
    users.value = res.data?.data?.users || []
    roles.value = res.data?.data?.roles || []
  } catch (e) {
    message.error(e?.message || '加载失败')
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.toolbar { margin-bottom: 16px; }
.toolbar h2 { margin: 0; font-size: 16px; color: #182230; }
.toolbar p { margin: 4px 0 0; font-size: 12px; color: #667085; }
.roles { display: flex; gap: 10px; margin-bottom: 14px; flex-wrap: wrap; }
.role { background: #f8fafc; border: 1px solid #e4e9f0; border-radius: 10px; padding: 10px 12px; min-width: 220px; }
.role strong { display: block; }
.role span { font-size: 12px; color: #667085; }
.empty { color: #98a2b3; text-align: center; padding: 24px; }
</style>

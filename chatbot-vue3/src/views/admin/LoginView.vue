<template>
  <main class="login-page">
    <section class="brand-side">
      <div class="brand-lockup">
        <span class="brand-mark">XZ</span>
        <div>
          <strong>小智AI</strong>
          <small>商家客服运营平台</small>
        </div>
      </div>
      <div class="brand-message">
        <h1>商家客服运营工作台</h1>
        <p>统一收件箱、动作审批、知识库与 Agent Trace。买家请走独立咨询组件，不经过本后台。</p>
      </div>
      <p class="version">Spring Boot · Spring AI · 混合 RAG</p>
    </section>

    <section class="form-side">
      <div class="login-form-wrap">
        <div class="form-heading">
          <h2>商家与客服登录</h2>
          <p>登录后进入运营概览，按角色处理工单与审批。</p>
        </div>
        <div class="role-note">演示账号：admin / 123456（可审批）</div>
        <a-alert v-if="error" type="error" show-icon :message="error" closable @close="error = ''" class="mb-4" />
        <div class="form-fields">
          <a-input v-model:value="username" size="large" placeholder="用户名" />
          <a-input-password v-model:value="password" size="large" placeholder="密码" @pressEnter="login" />
          <a-button type="primary" size="large" block :loading="loading" @click="login">登录运营后台</a-button>
        </div>
        <a-button type="link" block class="buyer-entry" @click="router.push('/widget')">
          我是买家，进入在线客服 Widget
        </a-button>
        <a-button type="link" block @click="router.push('/')">
          内部对话测试台
        </a-button>
      </div>
      <p class="security-note">登录状态仅保存在当前浏览器会话中。</p>
    </section>
  </main>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { adminLogin } from '@/api/admin'

const router = useRouter()
const route = useRoute()
const username = ref('admin')
const password = ref('123456')
const error = ref('')
const loading = ref(false)

async function login() {
  error.value = ''
  loading.value = true
  try {
    const res = await adminLogin({ username: username.value, password: password.value })
    if (!res.data?.success) {
      error.value = res.data?.message || '登录失败'
      return
    }
    const data = res.data.data
    localStorage.setItem('admin_token', data.token)
    localStorage.setItem('admin_displayName', data.displayName)
    localStorage.setItem('admin_role', data.role)
    router.push(route.query.redirect || '/admin')
  } catch (e) {
    error.value = e?.response?.data?.message || e.message || '登录失败'
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-page {
  min-height: 100vh;
  display: grid;
  grid-template-columns: minmax(320px, 42%) 1fr;
  background: #fff;
}

.brand-side {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  padding: 42px 48px;
  background: #0d1728;
  color: #fff;
}

.brand-lockup {
  display: flex;
  align-items: center;
  gap: 12px;
}

.brand-mark {
  width: 38px;
  height: 38px;
  display: grid;
  place-items: center;
  border-radius: 7px;
  background: #1677ff;
  color: #fff;
  font-size: 13px;
  font-weight: 800;
}

.brand-lockup div {
  display: flex;
  flex-direction: column;
}

.brand-lockup strong {
  font-size: 16px;
}

.brand-lockup small {
  margin-top: 2px;
  color: #91a0b5;
  font-size: 11px;
}

.brand-message {
  max-width: 440px;
}

.brand-message h1 {
  margin: 0;
  font-size: 36px;
  line-height: 1.25;
  font-weight: 720;
}

.brand-message p {
  margin: 16px 0 0;
  color: #a7b2c3;
  font-size: 14px;
  line-height: 1.7;
}

.version {
  margin: 0;
  color: #64748b;
  font-size: 11px;
}

.form-side {
  min-height: 100vh;
  display: grid;
  grid-template-rows: 1fr auto;
  place-items: center;
  padding: 40px;
}

.login-form-wrap {
  width: min(380px, 100%);
}

.form-heading {
  margin-bottom: 20px;
}

.form-heading h2 {
  margin: 0;
  color: #111827;
  font-size: 24px;
}

.form-heading p {
  margin: 7px 0 0;
  color: #667085;
  font-size: 13px;
}

.role-note {
  margin-bottom: 16px;
  padding: 10px 12px;
  border-radius: 8px;
  background: #f5f7fa;
  color: #667085;
  font-size: 12px;
}

.form-fields {
  display: grid;
  gap: 12px;
}

.buyer-entry {
  margin-top: 12px;
}

.security-note {
  color: #98a2b3;
  font-size: 11px;
  margin: 0 0 12px;
}

.mb-4 {
  margin-bottom: 16px;
}

@media (max-width: 860px) {
  .login-page {
    grid-template-columns: 1fr;
  }
  .brand-side {
    min-height: auto;
    padding: 28px 24px;
  }
}
</style>

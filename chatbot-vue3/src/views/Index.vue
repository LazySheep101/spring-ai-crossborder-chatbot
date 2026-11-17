<template>
  <Layout>
    <template #main-content>
      <header class="chat-header">
        <div class="header-title">
          <strong>知识库对话测试</strong>
          <span>可验证 RAG、订单、物流与人工升级</span>
        </div>
        <a-tag color="green">就绪</a-tag>
      </header>

      <div class="messages-container">
        <div class="welcome">
          <h3>跨境智能客服测试台</h3>
          <p>这里会调用后台智能客服链路，可验证 RAG 政策问答、订单、物流、商品推荐和人工升级。</p>
          <div class="examples">
            <button type="button" @click="quickStart('这件外套可以退货吗？')">这件外套可以退货吗？</button>
            <button type="button" @click="quickStart('订单 ORD-10001 现在到哪里了？')">订单 ORD-10001 现在到哪里了？</button>
            <button type="button" @click="quickStart('推荐一款保温杯')">推荐一款保温杯</button>
            <button type="button" class="primary-chip" @click="goCustomerService">进入客服 Agent 调试</button>
            <button type="button" @click="router.push('/widget')">买家 Widget</button>
            <button type="button" @click="router.push('/admin')">商家运营后台</button>
          </div>
        </div>
      </div>

      <footer class="chat-input">
        <ChatInputBox
          v-model="userMessage"
          containerClass="omni-input"
          @sendMessage="sendMessage"
          placeholder="输入消息测试智能客服，支持订单 / 物流 / 退换货"
          :showModelDropdown="true"
          :showNetworkSearch="false"
        />
      </footer>
    </template>
  </Layout>
</template>

<script setup>
import { ref } from 'vue'
import Layout from '@/layouts/Layout.vue'
import ChatInputBox from '@/components/ChatInputBox.vue'
import { newChat } from '@/api/chat'
import { useRouter } from 'vue-router'

const router = useRouter()
const userMessage = ref('')

const goCustomerService = () => {
  router.push({ name: 'CustomerServiceChatPage' })
}

const quickStart = (text) => {
  userMessage.value = text
  sendMessage()
}

const sendMessage = () => {
  if (!userMessage.value.trim()) return
  const userMessageTemp = userMessage.value.trim()

  newChat(userMessageTemp).then(res => {
    if (res.data.success) {
      router.push({
        name: 'ChatPage',
        params: { chatId: res.data.data.uuid },
        state: { firstMessage: userMessageTemp }
      })
    }
  })
}
</script>

<style scoped>
.chat-header {
  align-items: center;
  background: #fff;
  border-bottom: 1px solid #e5e7eb;
  display: flex;
  flex-shrink: 0;
  min-height: 60px;
  justify-content: space-between;
  padding: 0 24px;
  gap: 12px;
}

.header-title {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.header-title strong {
  font-size: 15px;
  color: #1f2937;
  font-weight: 600;
}

.header-title span {
  font-size: 12px;
  color: #667085;
}

.messages-container {
  display: flex;
  flex: 1;
  flex-direction: column;
  min-height: 0;
  overflow-y: auto;
  padding: 24px 20px;
}

.welcome {
  color: #667085;
  margin: auto;
  max-width: 760px;
  text-align: center;
  padding: 24px 12px;
}

.welcome h3 {
  color: #1f2937;
  font-size: 24px;
  margin: 0 0 10px;
  font-weight: 650;
}

.welcome p {
  margin: 0 auto;
  max-width: 560px;
  line-height: 1.7;
  font-size: 14px;
}

.examples {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  justify-content: center;
  margin-top: 20px;
}

.examples button {
  background: #fff;
  border: 1px solid #d9e4f5;
  border-radius: 18px;
  color: #1f4d8f;
  cursor: pointer;
  font-size: 13px;
  padding: 8px 14px;
  transition: all 0.15s ease;
}

.examples button:hover {
  background: #f0f6ff;
  border-color: #9ec3ff;
}

.examples .primary-chip {
  background: #1677ff;
  border-color: #1677ff;
  color: #fff;
}

.examples .primary-chip:hover {
  background: #4096ff;
  border-color: #4096ff;
  color: #fff;
}

.chat-input {
  background: #fff;
  border-top: 1px solid #e5e7eb;
  flex-shrink: 0;
  padding: 14px 8px 16px;
}
</style>

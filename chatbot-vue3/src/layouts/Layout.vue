<template>
  <div class="chat-layout">
    <Sidebar :sidebarOpen="sidebarOpen" @toggle-sidebar="toggleSidebar" />
    <div class="chat-main" :class="{ 'sidebar-collapsed': !sidebarOpen }">
      <slot name="main-content"></slot>
    </div>
    <div v-if="showFooterText" class="page-footnote" :class="{ 'sidebar-collapsed': !sidebarOpen }">
      内容由 AI 生成，请仔细甄别
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import Sidebar from '@/components/Sidebar.vue'

defineProps({
  showFooterText: {
    type: Boolean,
    default: true
  }
})

const sidebarOpen = ref(true)
const toggleSidebar = () => {
  sidebarOpen.value = !sidebarOpen.value
}
</script>

<style scoped>
.chat-layout {
  background: #f3f6fb;
  display: flex;
  height: 100vh;
  overflow: hidden;
}

.chat-main {
  display: flex;
  flex: 1;
  flex-direction: column;
  min-width: 0;
  min-height: 0;
  height: 100%;
  margin-left: 280px;
  transition: margin-left 0.3s ease;
  padding-bottom: 22px; /* 避开底部免责声明 */
}

.chat-main.sidebar-collapsed {
  margin-left: 0;
  /* 给左侧「打开边栏」按钮留空，避免再挡标题 */
  padding-left: 44px;
}

.page-footnote {
  position: fixed;
  bottom: 0;
  left: 280px;
  right: 0;
  text-align: center;
  font-size: 11px;
  color: #98a2b3;
  padding: 6px;
  pointer-events: none;
  transition: left 0.3s ease;
}

.page-footnote.sidebar-collapsed {
  left: 0;
}

@media (max-width: 860px) {
  .chat-main {
    margin-left: 0;
  }
  .page-footnote {
    left: 0;
  }
}
</style>

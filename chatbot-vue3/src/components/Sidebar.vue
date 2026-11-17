<template>
  <aside class="chat-sidebar" :class="{ open: sidebarOpen }">
    <div class="brand" @click="jumpToIndexPage">
      <div class="brand-row">
        <span class="brand-mark">XZ</span>
        <div>
          <h2>小智AI</h2>
          <span class="version">智能客服测试台</span>
        </div>
      </div>
    </div>

    <div class="new-chat-wrap">
      <a-button block type="primary" class="new-chat-btn" @click="jumpToIndexPage">
        <template #icon><PlusOutlined /></template>
        新对话
      </a-button>
    </div>

    <div class="tool-entry" @click="jumpToCustomerServiceChatPage">
      <CustomerServiceOutlined class="tool-icon" />
      <span>客服 Agent 对话</span>
    </div>

    <div class="conversation-list" ref="historyChatContainerRef">
      <div class="list-label">历史对话</div>
      <button
        v-for="(historyChat, index) in historyChats"
        :key="index"
        class="conv-item"
        :class="{ active: selectedChatId === historyChat.uuid }"
        type="button"
        @click="jumpToChatPage(historyChat.uuid)"
        @mouseenter="showButton = historyChat.uuid"
        @mouseleave="showButton = null"
      >
        <span class="conv-title">{{ historyChat.summary || '新对话' }}</span>
        <a-dropdown>
          <template #overlay>
            <a-menu @click="handleMenuClick(historyChat.uuid, historyChat.id, historyChat.summary, $event)">
              <a-menu-item key="rename"><EditOutlined /> 重命名</a-menu-item>
              <a-menu-item key="delete" danger><DeleteOutlined /> 删除</a-menu-item>
            </a-menu>
          </template>
          <span
            class="conv-more"
            :style="{ opacity: showButton === historyChat.uuid ? 1 : 0 }"
            @click.stop
          >
            <EllipsisOutlined />
          </span>
        </a-dropdown>
      </button>
      <a-empty v-if="!historyChats.length" description="暂无对话" :image="simpleImage" />
    </div>

    <div class="sidebar-footer">
      <a-button type="link" @click="router.push('/widget')">买家 Widget</a-button>
      <a-button type="link" @click="router.push('/admin/login')">商家后台</a-button>
      <a-button type="link" @click="toggleSidebar">{{ sidebarOpen ? '收起' : '展开' }}</a-button>
    </div>
  </aside>

  <!-- 仅在侧栏收起时显示，避免压住主区顶栏标题 -->
  <a-tooltip v-if="!sidebarOpen" placement="right">
    <template #title>
      <span>打开边栏</span>
    </template>
    <button class="sidebar-toggle" type="button" @click="toggleSidebar">
      <SvgIcon name="sidebar-close" customCss="w-5 h-5 text-gray-500" />
    </button>
  </a-tooltip>

  <!-- 删除对话确认框 -->
  <a-modal v-model:open="deleteChatModelOpen" width="400px" :centered=true title="永久删除对话" ok-text="确认" ok-type="danger" cancel-text="取消" 
    @ok="handleDeleteChatModelOk()">
      <p>删除后，该对话将不可恢复。确认删除吗？</p>
  </a-modal>

  <!-- 重命名对话弹出框 -->
  <a-modal v-model:open="renameChatModelOpen" width="600px" :centered=true title="重命名对话" ok-text="确认" cancel-text="取消"
  @ok="handleRenameChatModelOk()">
              <a-form
                ref="formRef"
                :rules="rules"
                :model="formState"
                :label-col="{ span: 2 }"
                :wrapper-col="{ span: 22 }"
                autocomplete="off"
                >
                    <a-form-item
                      label="摘要"
                      name="summary"
                    >
                      <a-input v-model:value="formState.summary" />
                    </a-form-item>
                </a-form>
  </a-modal>

</template>

<script setup>
import { ref, reactive, onMounted, onBeforeUnmount, toRaw } from 'vue'
import SvgIcon from '@/components/SvgIcon.vue'
import { EditOutlined, EllipsisOutlined, DeleteOutlined, PlusOutlined, CustomerServiceOutlined } from '@ant-design/icons-vue'
import { Empty } from 'ant-design-vue'
import { useRouter, useRoute } from 'vue-router'
import { findHistoryChatPageList, deleteChat, renameChat } from '@/api/chat'
import { message } from 'ant-design-vue'

const simpleImage = Empty.PRESENTED_IMAGE_SIMPLE

const router = useRouter()
const route = useRoute()

// 定义 props, 对外部暴露配置项
const props = defineProps({
  sidebarOpen: { type: Boolean, required: true }, // 左边栏是否展开
})

// 定义emits
const emit = defineEmits(['toggle-sidebar'])

// 历史对话
const historyChats = ref([])

// 切换侧边栏显示/隐藏
const toggleSidebar = () => {
  emit('toggle-sidebar')
}

// 当前显示右侧栏按钮的聊天 ID
const showButton = ref(null)

// 当前选中的聊天 ID
const selectedChatId = ref(null)

// 跳转到首页
const jumpToIndexPage = () => {
  router.push('/')
}

// 跳转智能客服聊天页
const jumpToCustomerServiceChatPage = () => {
  router.push({ name: 'CustomerServiceChatPage'})
}

// 左边栏滚动区域引用
const historyChatContainerRef = ref(null)

onMounted(() => {
  // 查询历史对话
  fetchHistoryChats()

  // 检查当前路由是否有 chatId 参数
  const currentChatId = route.params.chatId
  if (currentChatId) {
    // 设置当前选中的对话 ID
    selectedChatId.value = currentChatId
  }

  // 添加滚动监听事件
  if (historyChatContainerRef.value) {
    historyChatContainerRef.value.addEventListener('scroll', handleScroll)
  }
})

onBeforeUnmount(() => {
  // 移除滚动事件监听
  if (historyChatContainerRef.value) {
    historyChatContainerRef.value.removeEventListener('scroll', handleScroll);
  }
})

const handleScroll = () => {
  if (historyChatContainerRef.value) {
    // 获取滚动参数
    const { scrollTop, scrollHeight, clientHeight } = historyChatContainerRef.value
    // 距离底部距离
    const scrollPosition = scrollHeight - scrollTop - clientHeight

    // 打印滚动过程中的详细日志
    console.log('=== 滚动事件日志 ===')
    console.log('scrollPosition:', scrollPosition)

    // 当滚动到距离底部20px以内时，且有更多数据，且当前没有在加载中时，加载更多
    if (scrollPosition <= 20 && hasMore.value && !isLoadingMore.value) {
      loadMoreHistoryChats()
    }
  }
}

// 加载更多历史对话
const loadMoreHistoryChats = async () => {
  console.log('=== 开始加载更多历史对话 ===')
  console.log('当前页码:', current.value)

  // 双重检查：
  // 1. 如果当前页面已经是最后一页，则不再发送请求
  // 2. 如果已经有请求在进行中，则不再发送请求
  if (!hasMore.value) {
    console.log('=== 没有更多历史对话，不再请求 ===')
    return
  }
  
  if (isLoadingMore.value) {
    console.log('=== 已有加载请求正在进行中，不再发送新请求 ===')
    return
  }
  
  // 设置加载状态为 true，防止并发请求
  isLoadingMore.value = true

  // 计算下一页页码（向下滑动加载更早的历史对话，页码应该增加）
  const nextPageNo = current.value + 1
  console.log('=== 计算下一页页码 ===', nextPageNo)

  // 保存当前页码用于错误恢复
  const currentTemp = current.value
  // 当前需要请求的页码
  current.value = nextPageNo

  try {
    await fetchHistoryChats();
  } catch (error) {
    // 恢复页码
    current.value = currentTemp;
  }
}

// 分页相关状态
// 当前页码（默认第一页）
const current = ref(1)
// 每页展示数据量
const size = ref(20)
// 是否还有下一页数据（默认有）
const hasMore = ref(true)
// 是否正在加载中 (解决并发请求后续页数据问题)
const isLoadingMore = ref(false)

// 获取历史对话列表
const fetchHistoryChats = async () => {
  findHistoryChatPageList(current.value, size.value).then(res => {
    // 无论成功失败，请求完成后都需要重置加载状态
    isLoadingMore.value = false

    if (res.data.success) {
      const data = res.data.data

      // 判断是否还有下一页(总页数 > 当前页)
      hasMore.value = res.data.pages > current.value

      if (data && data.length > 0) {
        // 将新数据添加到历史对话列表末尾
        historyChats.value = [...historyChats.value, ...data]
      }
    }
  }).catch((error) => {
      // 错误处理，重置加载状态
      console.error('加载历史对话失败:', error)
      isLoadingMore.value = false
  })
}

// 跳转历史对话页
const jumpToChatPage = (chatId) => {
  // 更新被选中的对话 ID
  selectedChatId.value = chatId
  router.push({ name: 'ChatPage', params: { chatId } })
}

// 删除对话确认框是否展示
const deleteChatModelOpen = ref(false)
// 被删除的对话 UUID
const deleteChatUUID = ref(null)

// 重命名对话弹出框是否展示
const renameChatModelOpen = ref(false)

// 表单对象
const formState = reactive({
  id: null, // 被重命名对话 ID
  summary: '' // 被重命名对话摘要
})

// 处理菜单点击
const handleMenuClick = (uuid, id, summary, e) => {
  if (e.key === 'delete') { // 删除对话
    console.log('用户点击了删除对话：', uuid)
    // 展示确认框
    deleteChatModelOpen.value = true
    // 保存当前被删除的对话 UUID
    deleteChatUUID.value = uuid
  } else if (e.key === 'rename') { // 重命名对话
    console.log('用户点击了重命名对话：', uuid)
    
    // 展示弹出框
    renameChatModelOpen.value = true
    // 保存当前被重命名对话的 ID
    formState.id = id
    // 保存摘要
    formState.summary = summary

    console.log('表单数据：', formState)
  }
}

// 删除对话确认框确认事件
const handleDeleteChatModelOk = () => {
  deleteChat(deleteChatUUID.value).then((res) => {
      if (res.data.success) {
        message.success('删除成功')
        
        // 从 historyChats 数组中删除对应的对话
        const index = historyChats.value.findIndex(chat => chat.uuid === deleteChatUUID.value);
        if (index !== -1) {
          console.log('找到了，', index)
          historyChats.value.splice(index, 1);
        }
        
        // 跳转首页
        router.push({ name: 'Index'})
      } else {
        // 提示错误信息
        message.error(res.data.message)
      }
  }).finally(
    // 隐藏确认框
    deleteChatModelOpen.value = false
  )
}

// 表单引用
const formRef = ref(null)

// 校验规则
const rules = {
  summary: [
   { required: true, message: '请输入对话摘要', trigger: 'change' }
  ]
}

// 重命名对话弹出框确认事件
const handleRenameChatModelOk = () => {
  formRef.value
    .validate()
    .then(() => { // 校验通过
      renameChat(formState.id, formState.summary).then(res => {
          if (!res.data.success) {
            // 接口返回失败，提示错误消息
            message.error(res.data.message)

            return
          }

          // 提示操作成功
          message.success('操作成功！')
          
          // 更新 historyChats 数组中对应 id 的对话摘要
          const chatIndex = historyChats.value.findIndex(chat => chat.id === formState.id);
          if (chatIndex !== -1) {
            historyChats.value[chatIndex].summary = formState.summary;
          }
          
          // 关闭弹出框
          renameChatModelOpen.value = false
      })
    })
    .catch(error => { // 校验不通过
      console.log('error', error);
    })
}


</script>

<style scoped>
.chat-sidebar {
  position: fixed;
  left: 0;
  top: 0;
  z-index: 20;
  width: 280px;
  height: 100%;
  background: #fff;
  border-right: 1px solid #e5e7eb;
  display: flex;
  flex-direction: column;
  transform: translateX(-100%);
  transition: transform 0.3s ease;
}

.chat-sidebar.open {
  transform: translateX(0);
}

.brand {
  border-bottom: 1px solid #eef0f3;
  cursor: pointer;
  padding: 20px;
}

.brand-row {
  display: flex;
  align-items: center;
  gap: 12px;
}

.brand-mark {
  width: 36px;
  height: 36px;
  border-radius: 7px;
  background: #1677ff;
  color: #fff;
  display: grid;
  place-items: center;
  font-size: 12px;
  font-weight: 800;
}

.brand h2 {
  color: #1677ff;
  font-size: 18px;
  margin: 0;
  line-height: 1.2;
}

.version {
  color: #8c8c8c;
  font-size: 12px;
}

.new-chat-wrap {
  padding: 12px 16px 8px;
}

.new-chat-wrap :deep(.new-chat-btn.ant-btn) {
  width: 100%;
  height: 40px;
  border-radius: 8px;
  font-weight: 500;
  display: inline-flex !important;
  align-items: center;
  justify-content: center;
  gap: 6px;
}

.new-chat-wrap :deep(.new-chat-btn .ant-btn-icon),
.new-chat-wrap :deep(.new-chat-btn .anticon) {
  display: inline-flex;
  align-items: center;
  line-height: 1;
  font-size: 14px;
}

.tool-entry {
  margin: 0 16px 8px;
  padding: 10px 12px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  color: #1f2937;
  cursor: pointer;
}

.tool-icon {
  color: #1677ff;
  font-size: 14px;
}

.tool-entry:hover {
  background: #f0f5ff;
}

.conversation-list {
  flex: 1;
  overflow-y: auto;
  padding: 0 10px 12px;
}

.list-label {
  color: #9ca3af;
  font-size: 11px;
  padding: 8px 12px 6px;
}

.conv-item {
  position: relative;
  background: transparent;
  border: 0;
  border-radius: 8px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  margin-bottom: 4px;
  padding: 10px 12px;
  text-align: left;
  width: 100%;
}

.conv-item:hover,
.conv-item.active {
  background: #f0f5ff;
}

.conv-title {
  color: #1f2937;
  font-size: 13px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  flex: 1;
}

.conv-more {
  color: #9ca3af;
  display: inline-flex;
  transition: opacity 0.2s;
}

.sidebar-footer {
  border-top: 1px solid #eef0f3;
  display: flex;
  justify-content: space-between;
  padding: 8px 4px;
}

.sidebar-toggle {
  position: fixed;
  top: 16px;
  left: 0;
  z-index: 30;
  background: #fff;
  border: 1px solid #e5e7eb;
  border-left: 0;
  border-radius: 0 8px 8px 0;
  padding: 8px;
  box-shadow: 0 1px 4px rgba(15, 23, 42, 0.08);
}

@media (max-width: 860px) {
  .chat-sidebar.open {
    box-shadow: 8px 0 24px rgba(15, 23, 42, 0.12);
  }
}
</style>
<template>
  <div :class="['composer-root', containerClass]">
    <div class="composer-shell" :class="{ focused: isFocused }">
      <textarea
        ref="textareaRef"
        v-model="userMessage"
        class="composer-textarea"
        :placeholder="placeholder"
        rows="1"
        @input="autoResize"
        @keydown="handleKeydown"
        @focus="isFocused = true"
        @blur="isFocused = false"
      />

      <div class="composer-toolbar">
        <div class="toolbar-left" ref="leftContainerRef">
          <button
            v-show="showModelDropdown"
            ref="selectRef"
            type="button"
            class="chip"
            @click.stop="toggleModelDropdown"
          >
            <SvgIcon :name="currSelectedModel.icon" customCss="w-4 h-4" />
            <span>{{ currSelectedModel.name }}</span>
            <SvgIcon
              name="down-arrow"
              customCss="w-4 h-4 text-gray-500 transition-transform duration-200"
              :class="isModelDropdownOpen ? 'rotate-180' : ''"
            />
          </button>

          <div
            v-if="isModelDropdownOpen"
            ref="dropdownRef"
            class="model-menu"
            :class="dropdownPosition"
          >
            <button
              v-for="model in models"
              :key="model.id"
              type="button"
              class="model-item"
              @click="selectModel(model)"
            >
              <div class="model-item-main">
                <SvgIcon :name="model.icon" customCss="w-4 h-4" />
                <div>
                  <div class="model-name">{{ model.name }}</div>
                  <div class="model-desc">{{ model.description }}</div>
                </div>
              </div>
              <SvgIcon v-if="model.selected" name="check" customCss="w-3 h-3 text-[#1677ff]" />
            </button>
          </div>

          <button
            v-show="showNetworkSearch"
            type="button"
            class="chip"
            :class="{ active: isNetworkSearchSelected }"
            @click="toggleNetworkSearch"
          >
            <SvgIcon
              name="network"
              customCss="w-4 h-4"
              :class="isNetworkSearchSelected ? 'text-[#1677ff]' : 'text-gray-500'"
            />
            <span>联网搜索</span>
          </button>
        </div>

        <button
          type="button"
          class="send-btn"
          :disabled="!userMessage.trim()"
          title="发送"
          @click="handleSendMessage"
        >
          <SvgIcon name="up-arrow" customCss="w-4 h-4 text-white" />
        </button>
      </div>
    </div>
    <p class="composer-hint">Enter 发送 · Shift + Enter 换行</p>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, computed, nextTick } from 'vue'
import SvgIcon from '@/components/SvgIcon.vue'
import { message } from 'ant-design-vue'
import { useChatStore } from '@/stores/chatStore'

const chatStore = useChatStore()

const props = defineProps({
  modelValue: {
    type: String,
    required: true
  },
  containerClass: {
    type: String,
    default: ''
  },
  placeholder: {
    type: String,
    default: '给小智AI 发送消息'
  },
  showModelDropdown: {
    type: Boolean,
    default: true
  },
  showNetworkSearch: {
    type: Boolean,
    default: true
  },
})

const emit = defineEmits(['update:modelValue', 'sendMessage'])

const userMessage = computed({
  get() {
    return props.modelValue
  },
  set(value) {
    emit('update:modelValue', value)
  }
})

const models = computed(() => chatStore.models)
const currSelectedModel = computed(() => chatStore.selectedModel)
const isNetworkSearchSelected = computed(() => chatStore.isNetworkSearchSelected)

const isFocused = ref(false)
const isModelDropdownOpen = ref(false)
const selectRef = ref(null)
const leftContainerRef = ref(null)
const dropdownRef = ref(null)
const dropdownPosition = ref('up')
const textareaRef = ref(null)

const toggleModelDropdown = () => {
  isModelDropdownOpen.value = !isModelDropdownOpen.value
  if (isModelDropdownOpen.value) {
    nextTick(() => {
      if (leftContainerRef.value && dropdownRef.value) {
        const buttonRect = leftContainerRef.value.getBoundingClientRect()
        const dropdownHeight = dropdownRef.value.offsetHeight
        dropdownPosition.value =
          window.innerHeight - buttonRect.bottom < dropdownHeight + 12 ? 'up' : 'down'
      }
    })
  }
}

const handleClickOutside = (event) => {
  if (leftContainerRef.value && !leftContainerRef.value.contains(event.target)) {
    isModelDropdownOpen.value = false
  }
}

onMounted(() => {
  document.addEventListener('click', handleClickOutside)
  nextTick(autoResize)
})

onUnmounted(() => {
  document.removeEventListener('click', handleClickOutside)
})

const selectModel = (model) => {
  chatStore.updateSelectedModel(model)
  isModelDropdownOpen.value = false
}

const toggleNetworkSearch = () => {
  chatStore.updateNetworkSearchStatus(!chatStore.isNetworkSearchSelected)
}

const handleSendMessage = () => {
  if (!userMessage.value.trim()) {
    message.warning('消息不能为空')
    return
  }

  emit('sendMessage', {
    selectedModel: chatStore.selectedModel,
    isNetworkSearch: chatStore.isNetworkSearchSelected
  })
  userMessage.value = ''
  nextTick(autoResize)
}

const autoResize = () => {
  const textarea = textareaRef.value
  if (!textarea) return
  textarea.style.height = 'auto'
  const next = Math.min(Math.max(textarea.scrollHeight, 24), 160)
  textarea.style.height = `${next}px`
  textarea.style.overflowY = textarea.scrollHeight > 160 ? 'auto' : 'hidden'
}

const handleKeydown = (event) => {
  if (event.key === 'Enter' && !event.shiftKey) {
    event.preventDefault()
    handleSendMessage()
  }
}
</script>

<style scoped>
.composer-root {
  width: 100%;
}

.composer-shell {
  margin: 0 16px;
  padding: 12px 14px 10px;
  background: #fff;
  border: 1px solid #e4e9f0;
  border-radius: 14px;
  box-shadow: 0 8px 24px rgba(16, 24, 40, 0.06);
  transition: border-color 0.15s ease, box-shadow 0.15s ease;
}

.composer-shell.focused {
  border-color: #91caff;
  box-shadow: 0 0 0 3px rgba(22, 119, 255, 0.12), 0 8px 24px rgba(16, 24, 40, 0.06);
}

.composer-textarea {
  width: 100%;
  min-height: 24px;
  max-height: 160px;
  border: 0;
  outline: none;
  resize: none;
  background: transparent;
  font-size: 14px;
  line-height: 1.6;
  color: #182230;
}

.composer-textarea::placeholder {
  color: #98a2b3;
}

.composer-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-top: 10px;
}

.toolbar-left {
  position: relative;
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
}

.chip {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  height: 30px;
  padding: 0 10px;
  border: 1px solid #e4e9f0;
  border-radius: 999px;
  background: #f8fafc;
  color: #344054;
  font-size: 12px;
  cursor: pointer;
  white-space: nowrap;
}

.chip:hover {
  background: #f0f5ff;
  border-color: #c6d9f7;
}

.chip.active {
  background: #e6f4ff;
  border-color: #91caff;
  color: #1677ff;
}

.model-menu {
  position: absolute;
  left: 0;
  z-index: 20;
  width: 220px;
  padding: 6px;
  background: #fff;
  border: 1px solid #e4e9f0;
  border-radius: 10px;
  box-shadow: 0 12px 28px rgba(16, 24, 40, 0.12);
}

.model-menu.up {
  bottom: calc(100% + 8px);
}

.model-menu.down {
  top: calc(100% + 8px);
}

.model-item {
  width: 100%;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  padding: 8px 10px;
  border: 0;
  border-radius: 8px;
  background: transparent;
  cursor: pointer;
  text-align: left;
}

.model-item:hover {
  background: #f5f7fa;
}

.model-item-main {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
}

.model-name {
  font-size: 12px;
  color: #182230;
  font-weight: 600;
}

.model-desc {
  font-size: 11px;
  color: #98a2b3;
}

.send-btn {
  width: 34px;
  height: 34px;
  border: 0;
  border-radius: 50%;
  background: #1677ff;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  flex-shrink: 0;
  transition: background 0.15s ease, opacity 0.15s ease;
}

.send-btn:hover:not(:disabled) {
  background: #4096ff;
}

.send-btn:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}

.composer-hint {
  margin: 8px 20px 0;
  font-size: 11px;
  color: #98a2b3;
  text-align: center;
}
</style>

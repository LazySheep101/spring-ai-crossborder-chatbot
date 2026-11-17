import { defineStore } from 'pinia'
import { ref } from 'vue'

// 创建store
export const useChatStore = defineStore('chat', () => {

  // DeepSeek 官方当前模型（2026-10）：flash = V4.1-Flash；v4-pro = 更强 Pro
  const models = ref([
    { id: 1, name: 'deepseek-flash', icon: 'deepseek-logo', description: 'V4.1 Flash · 默认推荐', selected: true },
    { id: 2, name: 'deepseek-v4-pro', icon: 'deepseek-logo', description: 'V4 Pro · 更强推理', selected: false },
  ])

  // 选中的模型，默认为第一个
  const selectedModel = ref(models.value[0])

  // 联网搜索状态，默认为false
  const isNetworkSearchSelected = ref(false)

  // 更新选中的模型
  function updateSelectedModel(model) {
    models.value.forEach(m => {
      m.selected = false;
    });
    
    model.selected = true;
    selectedModel.value = model;
  }

  // 更新联网搜索状态
  function updateNetworkSearchStatus(status) {
    isNetworkSearchSelected.value = status
  }

  return { models, selectedModel, isNetworkSearchSelected, updateSelectedModel, updateNetworkSearchStatus }
},
{
  // 开启持久化
  persist: true,
})

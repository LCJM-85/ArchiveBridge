import { defineStore } from 'pinia'
import { ref } from 'vue'
import { useMenuStore } from './menu'

export const useTabStore = defineStore('tab', () => {
  const tabs = ref([
    { key: 'dashboard', title: '首页工作台', closable: false },
  ])
  const activeTab = ref('dashboard')

  function addTab(key, title) {
    if (!tabs.value.some((t) => t.key === key)) {
      tabs.value.push({ key, title, closable: key !== 'dashboard' })
    }
    activeTab.value = key
    const menuStore = useMenuStore()
    menuStore.setActive(key)
  }

  function closeTab(key) {
    const idx = tabs.value.findIndex((t) => t.key === key)
    if (idx === -1) return
    tabs.value.splice(idx, 1)

    if (activeTab.value === key) {
      const newIdx = Math.min(idx, tabs.value.length - 1)
      const target = tabs.value[newIdx]
      if (target) {
        activeTab.value = target.key
        const menuStore = useMenuStore()
        menuStore.setActive(target.key)
      }
    }
  }

  function setActiveTab(key) {
    activeTab.value = key
    const menuStore = useMenuStore()
    menuStore.setActive(key)
  }

  function closeOtherTabs(key) {
    tabs.value = tabs.value.filter((t) => t.key === key || !t.closable)
  }

  function resetTabs() {
    tabs.value = [{ key: 'dashboard', title: '首页工作台', closable: false }]
    activeTab.value = 'dashboard'
  }

  return {
    tabs,
    activeTab,
    addTab,
    closeTab,
    setActiveTab,
    closeOtherTabs,
    resetTabs,
  }
})

import { computed, ref } from 'vue'
import { defineStore } from 'pinia'

const DEFAULT_MENU_KEY = 'dashboard'

export const useMenuStore = defineStore('menu', () => {
  const activeMenu = ref(DEFAULT_MENU_KEY)

  const menuItems = [
    { key: 'dashboard', title: '首页工作台' },
    { key: 'upload', title: '档案智能采集' },
    { key: 'report', title: '智能报告生成' },
    { key: 'system', title: '系统管理' },
    { key: 'charts', title: '可视化分析大屏'},
    { key: 'data', title: '数据管理'},
    { key: 'process', title: 'ocr识别进程'},
    { key: 'user', title: '用户管理'},
    { key: 'meta', title: '元数据管理'},
    { key: 'admission', title: '招生数据管理'},
    { key: 'studentstatus', title: '学籍数据管理'},
    { key: 'graduation', title: '毕业数据管理'},
    { key: 'trend', title: '招生趋势分析'},
    { key: 'prediction', title: '智能预测'},
    { key: 'geo', title: '录取地理分布'},
    { key: 'path', title: '学科培养路径'}
  ]

  const activeTitle = computed(() => {
    const current = menuItems.find((item) => item.key === activeMenu.value)
    return current?.title || '数据概览'
  })

  function setActive(menuKey) {
    activeMenu.value = menuKey
  }

  function resetMenu() {
    activeMenu.value = DEFAULT_MENU_KEY
  }

  return {
    activeMenu,
    activeTitle,
    menuItems,
    setActive,
    resetMenu,
  }
})

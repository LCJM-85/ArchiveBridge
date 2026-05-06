import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import zhCn from 'element-plus/dist/locale/zh-cn.mjs'
import 'element-plus/dist/index.css'
import 'element-plus/theme-chalk/dark/css-vars.css'
import '@/styles/index.css'
import App from './App.vue'
import router from './router/index.js' // 导入路由实例

// 初始化主题 - 优先使用 localStorage 中的设置，否则跟随系统
const savedTheme = localStorage.getItem('theme')
if (savedTheme === 'dark') {
  document.documentElement.classList.add('dark')
} else if (!savedTheme && window.matchMedia('(prefers-color-scheme: dark)').matches) {
  document.documentElement.classList.add('dark')
  localStorage.setItem('theme', 'dark')
}

// 创建并挂载 Vue 实例
const app = createApp(App)
const pinia = createPinia()
app.use(ElementPlus, { locale: zhCn })
app.use(pinia)
app.use(router) // 注册路由
app.mount('#app')

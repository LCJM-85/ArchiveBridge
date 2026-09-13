import { ref } from 'vue'
import { ElMessage } from 'element-plus'

// Shared state keeps charts and navigation in sync when the theme changes.
const isDark = ref(localStorage.getItem('theme') === 'dark')

export function useTheme() {
  // main.js applies the saved/system preference before mounting components.
  isDark.value = document.documentElement.classList.contains('dark')
  function toggleTheme() {
    isDark.value = !isDark.value
    if (isDark.value) {
      document.documentElement.classList.add('dark')
      localStorage.setItem('theme', 'dark')
      ElMessage.success('已切换深色模式')
    } else {
      document.documentElement.classList.remove('dark')
      localStorage.setItem('theme', 'light')
      ElMessage.success('已切换浅色模式')
    }
  }

  return { isDark, toggleTheme }
}

import { ref } from 'vue'
import { ElMessage } from 'element-plus'

export function useTheme() {
  const isDark = ref(localStorage.getItem('theme') === 'dark')

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

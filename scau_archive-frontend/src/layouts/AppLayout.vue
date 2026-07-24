<template>
  <el-container class="app-layout">
    <Sidebar :visible="sidebarVisible" @close="sidebarVisible = false" />
    <!-- 移动端遮罩 -->
    <div v-if="isMobile && sidebarVisible" class="sidebar-overlay" @click="sidebarVisible = false" />
    <el-container direction="vertical" :class="{ 'sidebar-open': isMobile && sidebarVisible }">
      <Header @toggle-sidebar="toggleSidebar" />
      <Content />
    </el-container>
  </el-container>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount, provide } from 'vue'
import Sidebar from './Sidebar.vue'
import Header from './Header.vue'
import Content from './Content.vue'

const MOBILE_BREAKPOINT = 768
const isMobile = ref(window.innerWidth < MOBILE_BREAKPOINT)
const sidebarVisible = ref(!isMobile.value)

function checkMobile() {
  isMobile.value = window.innerWidth < MOBILE_BREAKPOINT
  if (!isMobile.value) sidebarVisible.value = true
}

function toggleSidebar() {
  sidebarVisible.value = !sidebarVisible.value
}

provide('isMobile', isMobile)

onMounted(() => window.addEventListener('resize', checkMobile))
onBeforeUnmount(() => window.removeEventListener('resize', checkMobile))
</script>

<style scoped>
.app-layout {
  height: 100vh;
  background: var(--app-bg);
  position: relative;
}

.sidebar-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.4);
  z-index: 999;
}

@media (max-width: 768px) {
  .app-layout :deep(.el-container) {
    width: 100%;
  }
}
</style>

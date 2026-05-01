<template>
  <el-container class="app-layout">
    <Sidebar />
    <el-container direction="vertical">
      <Header />
      <Content />
    </el-container>
  </el-container>
</template>

<script setup>
import Sidebar from './Sidebar.vue'
import Header from './Header.vue'
import Content from './Content.vue'
</script>

<style scoped>
.app-layout {
  height: 100vh;
  background: var(--app-bg);
}
</style>

<template>
  <div class="tab-bar">
    <el-tabs
      :model-value="tabStore.activeTab"
      type="card"
      closable
      @tab-click="handleClick"
      @tab-remove="tabStore.closeTab"
    >
      <el-tab-pane
        v-for="tab in tabStore.tabs"
        :key="tab.key"
        :label="tab.title"
        :name="tab.key"
        :closable="tab.closable"
      />
    </el-tabs>
  </div>
</template>

<script setup>
import { useTabStore } from '@/store/tab'

const tabStore = useTabStore()

function handleClick(tab) {
  tabStore.setActiveTab(tab.paneName)
}
</script>

<style scoped>
.tab-bar {
  background: var(--content-bg);
  border-bottom: 1px solid var(--el-border-color-light);
}

.tab-bar :deep(.el-tabs__header) {
  margin: 0;
  border-bottom: none;
}

.tab-bar :deep(.el-tabs__header .el-tabs__nav-wrap::after) {
  display: none;
}

.tab-bar :deep(.el-tabs__item) {
  height: 36px;
  line-height: 36px;
  font-size: 13px;
  padding: 0 14px;
  border: none !important;
  background: transparent;
  transition: background 0.2s;
}

.tab-bar :deep(.el-tabs__item.is-active) {
  background: var(--el-color-primary-light-9);
  color: var(--el-color-primary);
}

.tab-bar :deep(.el-tabs__item:hover) {
  color: var(--el-color-primary);
}

.tab-bar :deep(.el-tabs__item .el-icon-close) {
  width: 16px;
  height: 16px;
  line-height: 16px;
  border-radius: 50%;
  transition: all 0.2s;
}

.tab-bar :deep(.el-tabs__item .el-icon-close:hover) {
  background: var(--el-color-danger);
  color: #fff;
}

.tab-bar :deep(.el-tabs__nav) {
  border: none !important;
}

.tab-bar :deep(.el-tabs__nav-next),
.tab-bar :deep(.el-tabs__nav-prev) {
  height: 36px;
  line-height: 36px;
}
</style>

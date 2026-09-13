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
  background: var(--card-bg);
  padding: 0 16px;
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
  height: 42px;
  line-height: 42px;
  font-size: 13px;
  padding: 0 14px;
  border: none !important;
  background: transparent;
  transition: background 0.2s, color 0.2s;
  position: relative;
}

.tab-bar :deep(.el-tabs__item.is-active) {
  background: transparent;
  color: var(--color-primary);
}

/* 当前页以底部细线定位 */
.tab-bar :deep(.el-tabs__item.is-active::before) {
  content: '';
  position: absolute;
  bottom: 0;
  left: 8px;
  right: 8px;
  height: 2px;
  border-radius: 0 0 3px 3px;
  background: var(--color-primary);
}

/* 深色模式使用低亮度底色 */
.dark .tab-bar :deep(.el-tabs__item.is-active) {
  background: var(--color-primary-light);
  color: var(--color-primary);
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
  height: 42px;
  line-height: 42px;
}
</style>

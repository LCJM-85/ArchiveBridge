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
  transition: background 0.2s, color 0.2s;
  position: relative;
}

.tab-bar :deep(.el-tabs__item.is-active) {
  background: var(--el-color-primary-light-9);
  color: var(--color-primary);
}

/* 激活标签：顶部金色指示条 */
.tab-bar :deep(.el-tabs__item.is-active::before) {
  content: '';
  position: absolute;
  top: 0;
  left: 8px;
  right: 8px;
  height: 2.5px;
  border-radius: 0 0 3px 3px;
  background: linear-gradient(90deg, var(--color-gold-light), var(--color-gold));
}

/* 深色模式下亮绿底+绿字对比度不足（≈2.5:1），改用深绿底 + 亮绿字 */
.dark .tab-bar :deep(.el-tabs__item.is-active) {
  background: var(--el-color-primary-light-7);
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
  height: 36px;
  line-height: 36px;
}
</style>

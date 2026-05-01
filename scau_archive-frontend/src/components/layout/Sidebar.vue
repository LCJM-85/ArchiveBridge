<template>
  <el-aside width="220px" class="sidebar">
    <div class="logo">
      <div class="logo-icon">
        <svg viewBox="0 0 24 24" width="22" height="22" fill="none" stroke="currentColor" stroke-width="2">
          <path d="M4 19.5A2.5 2.5 0 0 1 6.5 17H20"/>
          <path d="M6.5 2H20v20H6.5A2.5 2.5 0 0 1 4 19.5v-15A2.5 2.5 0 0 1 6.5 2z"/>
          <line x1="8" y1="7" x2="16" y2="7"/>
          <line x1="8" y1="11" x2="14" y2="11"/>
        </svg>
      </div>
      <span class="logo-text">档案智能分析平台</span>
    </div>
    <el-menu
      :default-active="activeMenu"
      class="menu"
      @select="handleSelect"
    >
      <el-menu-item index="dashboard">
        <el-icon><House /></el-icon>
        <span>首页工作台</span>
      </el-menu-item>

      <el-menu-item index="upload">
        <el-icon><Upload /></el-icon>
        <span>档案智能采集</span>
      </el-menu-item>

      <el-menu-item index="process">
        <el-icon><Document /></el-icon>
        <span>OCR识别进程</span>
      </el-menu-item>

      <el-sub-menu index="data">
        <template #title>
          <el-icon><DataBoard /></el-icon>
          <span>数据管理</span>
        </template>
        <el-menu-item index="admission">
          <span>招生数据管理</span>
        </el-menu-item>
        <el-menu-item index="studentstatus">
          <span>学籍数据管理</span>
        </el-menu-item>
        <el-menu-item index="graduation">
          <span>毕业数据管理</span>
        </el-menu-item>
      </el-sub-menu>

      <el-sub-menu index="charts">
        <template #title>
          <el-icon><DataLine /></el-icon>
          <span>可视化分析大屏</span>
        </template>
        <el-menu-item index="trend">
          <span>招生趋势分析</span>
        </el-menu-item>
        <el-menu-item index="geo">
          <span>录取地理分布</span>
        </el-menu-item>
        <el-menu-item index="path">
          <span>学科培养路径</span>
        </el-menu-item>
        <el-menu-item index="prediction">
          <span>智能预测</span>
        </el-menu-item>
      </el-sub-menu>

      <el-menu-item index="report">
        <el-icon><Files /></el-icon>
        <span>智能报告生成</span>
      </el-menu-item>

      <el-sub-menu index="system">
        <template #title>
          <el-icon><Setting /></el-icon>
          <span>系统管理</span>
        </template>
        <el-menu-item index="user">
          <span>用户管理</span>
        </el-menu-item>
        <el-menu-item index="meta">
          <span>元数据管理</span>
        </el-menu-item>
      </el-sub-menu>
    </el-menu>

    <div class="sidebar-footer">
      <span class="version">v1.0</span>
    </div>
  </el-aside>
</template>

<script setup>
import { storeToRefs } from 'pinia'
import { DataLine, Files, House, Upload, Setting, DataBoard, Document } from '@element-plus/icons-vue'
import { useMenuStore } from '../../store/menu'

const menuStore = useMenuStore()
const { activeMenu } = storeToRefs(menuStore)

const handleSelect = (menuKey) => {
  menuStore.setActive(menuKey)
}
</script>

<style scoped>
.sidebar {
  background: var(--sidebar-bg);
  color: var(--sidebar-active-text);
  border-right: 1px solid var(--sidebar-border);
  display: flex;
  flex-direction: column;
  user-select: none;
  overflow: hidden;
}

.logo {
  height: 60px;
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 0 16px;
  border-bottom: 1px solid var(--sidebar-logo-border);
  flex-shrink: 0;
}

.logo-icon {
  color: var(--color-primary);
  display: flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  background: rgba(26, 122, 78, 0.15);
  border-radius: 8px;
  flex-shrink: 0;
}

.dark .logo-icon {
  background: rgba(45, 157, 110, 0.2);
}

.logo-text {
  font-size: 15px;
  font-weight: 600;
  letter-spacing: 1px;
  white-space: nowrap;
}

.sidebar .menu {
  flex: 1;
  overflow-y: auto;
  padding: 8px 0;
  border-right: none;
}

.sidebar .menu::-webkit-scrollbar {
  width: 3px;
}

.sidebar-footer {
  padding: 12px 16px;
  border-top: 1px solid var(--sidebar-logo-border);
  text-align: center;
  flex-shrink: 0;
}

.version {
  font-size: 11px;
  color: var(--sidebar-text);
  opacity: 0.5;
}
</style>

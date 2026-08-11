<template>
  <el-aside width="220px" class="sidebar" :class="{ 'sidebar-mobile': isMobile && visible }">
    <div class="logo">
      <div class="logo-icon">
        <svg viewBox="0 0 24 24" width="22" height="22" fill="none" stroke="currentColor" stroke-width="2">
          <path d="M4 19.5A2.5 2.5 0 0 1 6.5 17H20"/>
          <path d="M6.5 2H20v20H6.5A2.5 2.5 0 0 1 4 19.5v-15A2.5 2.5 0 0 1 6.5 2z"/>
          <line x1="8" y1="7" x2="16" y2="7"/>
          <line x1="8" y1="11" x2="14" y2="11"/>
        </svg>
      </div>
      <span class="logo-text">数智档桥</span>
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

      <el-sub-menu index="ai">
        <template #title>
          <el-icon><Cpu /></el-icon>
          <span>AI 智能</span>
        </template>
        <el-menu-item index="ai-assistant">
          <el-icon><ChatDotSquare /></el-icon>
          <span>AI 助手</span>
        </el-menu-item>
        <el-menu-item index="knowledge">
          <el-icon><Collection /></el-icon>
          <span>知识库</span>
        </el-menu-item>
      </el-sub-menu>

      <el-sub-menu index="system">
        <template #title>
          <el-icon><Setting /></el-icon>
          <span>系统管理</span>
        </template>
        <el-menu-item index="meta">
          <span>元数据管理</span>
        </el-menu-item>
        <el-menu-item index="college">
          <span>学院管理</span>
        </el-menu-item>
        <el-menu-item index="major">
          <span>专业管理</span>
        </el-menu-item>
        <el-menu-item index="class">
          <span>班级管理</span>
        </el-menu-item>
        <el-menu-item index="users" v-if="userRole === 'admin'">
          <span>用户管理</span>
        </el-menu-item>
      </el-sub-menu>
    </el-menu>

    <div class="sidebar-footer">
      <span class="version">v1.0</span>
    </div>
  </el-aside>
</template>

<script setup>
import { computed, inject, ref } from 'vue'
import { storeToRefs } from 'pinia'
import { DataLine, Files, House, Upload, Setting, DataBoard, Document, Cpu, Collection, ChatDotSquare } from '@element-plus/icons-vue'
import { useMenuStore } from '@/store/menu'
import { useTabStore } from '@/store/tab'
import { useUserStore } from '@/store/user'

const props = defineProps({
  visible: { type: Boolean, default: true }
})
const emit = defineEmits(['close'])
const isMobile = inject('isMobile', ref(false))

const menuStore = useMenuStore()
const tabStore = useTabStore()
const userStore = useUserStore()
const { activeMenu } = storeToRefs(menuStore)
const userRole = computed(() => userStore.role)

const handleSelect = (menuKey) => {
  const item = menuStore.menuItems.find((m) => m.key === menuKey)
  tabStore.addTab(menuKey, item?.title || menuKey)
}
</script>

<style scoped>
.sidebar {
  background: linear-gradient(180deg, #0b2a1e 0%, #09251a 55%, #07271c 100%) !important;
  color: var(--sidebar-active-text);
  border-right: 1px solid var(--sidebar-border);
  display: flex;
  flex-direction: column;
  user-select: none;
  overflow: hidden;
}

.logo {
  height: 64px;
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 0 16px;
  border-bottom: 1px solid var(--sidebar-logo-border);
  flex-shrink: 0;
  background: linear-gradient(180deg, rgba(14, 138, 95, 0.14), transparent);
}

.logo-icon {
  color: #e6cd95;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 34px;
  height: 34px;
  background: linear-gradient(140deg, #14a06f, #0b5c40);
  border: 1px solid rgba(201, 164, 92, 0.5);
  border-radius: 10px;
  box-shadow: 0 4px 14px rgba(6, 40, 28, 0.5);
  flex-shrink: 0;
}

.logo-text {
  font-family: var(--font-display);
  font-size: 16px;
  font-weight: 600;
  letter-spacing: 3px;
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

.sidebar-footer::before {
  content: '';
  display: inline-block;
  width: 5px;
  height: 5px;
  border-radius: 50%;
  background: var(--color-gold);
  margin-right: 7px;
  vertical-align: 1px;
  box-shadow: 0 0 8px rgba(201, 164, 92, 0.8);
}

/* ===== Mobile responsive ===== */
@media (max-width: 768px) {
  .sidebar {
    position: fixed;
    top: 0;
    left: 0;
    bottom: 0;
    z-index: 1000;
    transform: translateX(-100%);
    transition: transform 0.25s ease;
    box-shadow: none;
  }
  .sidebar.sidebar-mobile {
    transform: translateX(0);
    box-shadow: 4px 0 20px rgba(0, 0, 0, 0.3);
  }
}
</style>

<template>
  <div class="page-wrapper">
    <el-card shadow="never" class="page-card">
      <template #header>
        <div class="card-header">
          <div class="card-header-left">
            <el-icon size="18" color="var(--color-primary)"><Setting /></el-icon>
            <span>用户管理</span>
          </div>
        </div>
      </template>
      <div class="empty-state">
        <el-empty :image-size="160" description="用户管理模块建设中">
          <template #image>
            <div class="empty-img">
              <svg viewBox="0 0 80 80" width="120" height="120" fill="none">
                <circle cx="30" cy="28" r="8" stroke="currentColor" stroke-width="2" opacity="0.3"/>
                <circle cx="55" cy="28" r="8" stroke="currentColor" stroke-width="2" opacity="0.3"/>
                <path d="M12 62c0-10 8-18 18-18h0c6 0 11 3 14 7" stroke="currentColor" stroke-width="2" opacity="0.2" stroke-linecap="round"/>
                <path d="M48 52l4 4 8-8" stroke="currentColor" stroke-width="2" opacity="0.3" stroke-linecap="round" stroke-linejoin="round"/>
              </svg>
            </div>
          </template>
        </el-empty>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { Setting } from '@element-plus/icons-vue'
</script>

<style scoped>
.page-wrapper {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.card-header-left {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 15px;
  font-weight: 600;
  color: var(--text-primary);
}

.empty-state {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 300px;
}

.empty-img {
  opacity: 0.6;
}
</style>

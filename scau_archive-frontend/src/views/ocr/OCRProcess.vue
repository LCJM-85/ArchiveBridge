<template>
  <div class="page-wrapper">
    <el-card shadow="never" class="page-card">
      <template #header>
        <div class="card-header">
          <div class="card-header-left">
            <el-icon size="18" color="var(--color-primary)"><Document /></el-icon>
            <span>OCR识别进程</span>
          </div>
        </div>
      </template>
      <div class="empty-state">
        <el-empty :image-size="160" description="OCR识别进程模块建设中">
          <template #image>
            <div class="empty-img">
              <svg viewBox="0 0 80 80" width="120" height="120" fill="none">
                <rect x="10" y="15" width="60" height="55" rx="4" stroke="currentColor" stroke-width="2" opacity="0.3"/>
                <path d="M25 30l10 10-10 10" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" opacity="0.3"/>
                <path d="M42 45h14" stroke="currentColor" stroke-width="2" stroke-linecap="round" opacity="0.3"/>
              </svg>
            </div>
          </template>
        </el-empty>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { Document } from '@element-plus/icons-vue'
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

<template>
  <div class="page-wrapper">
    <el-card shadow="never" class="page-card">
      <template #header>
        <div class="card-header">
          <div class="card-header-left">
            <el-icon size="18" color="var(--color-primary)"><DataAnalysis /></el-icon>
            <span>智能预测分析</span>
          </div>
        </div>
      </template>
      <div class="empty-state">
        <el-empty :image-size="160" description="智能预测分析模块建设中">
          <template #image>
            <div class="empty-img">
              <svg viewBox="0 0 80 80" width="120" height="120" fill="none">
                <path d="M10 65h60" stroke="currentColor" stroke-width="1.5" opacity="0.15"/>
                <path d="M15 60l12-20 12 10 14-18 12 8" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" opacity="0.3"/>
                <path d="M65 50l-12-8" stroke="currentColor" stroke-width="1.5" stroke-dasharray="3 2" opacity="0.2"/>
                <circle cx="53" cy="40" r="2" fill="currentColor" opacity="0.3"/>
              </svg>
            </div>
          </template>
        </el-empty>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { DataAnalysis } from '@element-plus/icons-vue'
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

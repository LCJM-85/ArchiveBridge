<template>
  <div class="page-wrapper">
    <el-card shadow="never" class="page-card">
      <template #header>
        <div class="card-header">
          <div class="card-header-left">
            <el-icon size="18" color="var(--color-primary)"><TrendCharts /></el-icon>
            <span>招生趋势分析</span>
          </div>
        </div>
      </template>
      <div class="empty-state">
        <el-empty :image-size="160" description="招生趋势分析模块建设中">
          <template #image>
            <div class="empty-img">
              <svg viewBox="0 0 80 80" width="120" height="120" fill="none">
                <path d="M10 65l15-20 15 10 20-25" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" opacity="0.3"/>
                <path d="M60 30h10v35H10" stroke="currentColor" stroke-width="1.5" opacity="0.15"/>
                <circle cx="25" cy="45" r="2" fill="currentColor" opacity="0.3"/>
                <circle cx="40" cy="55" r="2" fill="currentColor" opacity="0.3"/>
                <circle cx="60" cy="30" r="2" fill="currentColor" opacity="0.3"/>
              </svg>
            </div>
          </template>
        </el-empty>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { TrendCharts } from '@element-plus/icons-vue'
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

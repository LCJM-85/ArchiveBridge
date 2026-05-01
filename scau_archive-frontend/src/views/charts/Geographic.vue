<template>
  <div class="page-wrapper">
    <el-card shadow="never" class="page-card">
      <template #header>
        <div class="card-header">
          <div class="card-header-left">
            <el-icon size="18" color="var(--color-primary)"><MapLocation /></el-icon>
            <span>录取地理分布</span>
          </div>
        </div>
      </template>
      <div class="empty-state">
        <el-empty :image-size="160" description="录取地理分布模块建设中">
          <template #image>
            <div class="empty-img">
              <svg viewBox="0 0 80 80" width="120" height="120" fill="none">
                <path d="M40 10c-8 0-15 7-15 15 0 11 15 28 15 28s15-17 15-28c0-8-7-15-15-15z" stroke="currentColor" stroke-width="2" opacity="0.3"/>
                <circle cx="40" cy="25" r="5" fill="currentColor" opacity="0.15"/>
                <path d="M20 70l10-15 15 10 15-20" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round" opacity="0.2"/>
              </svg>
            </div>
          </template>
        </el-empty>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { MapLocation } from '@element-plus/icons-vue'
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

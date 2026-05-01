<template>
  <div class="page-wrapper">
    <el-card shadow="never" class="page-card">
      <template #header>
        <div class="card-header">
          <div class="card-header-left">
            <el-icon size="18" color="var(--color-primary)"><Connection /></el-icon>
            <span>学科培养路径</span>
          </div>
        </div>
      </template>
      <div class="empty-state">
        <el-empty :image-size="160" description="学科培养路径模块建设中">
          <template #image>
            <div class="empty-img">
              <svg viewBox="0 0 80 80" width="120" height="120" fill="none">
                <circle cx="20" cy="25" r="6" stroke="currentColor" stroke-width="2" opacity="0.3"/>
                <circle cx="60" cy="25" r="6" stroke="currentColor" stroke-width="2" opacity="0.3"/>
                <circle cx="40" cy="55" r="6" stroke="currentColor" stroke-width="2" opacity="0.3"/>
                <path d="M26 25h28" stroke="currentColor" stroke-width="1.5" opacity="0.2"/>
                <path d="M26 28l8 24" stroke="currentColor" stroke-width="1.5" opacity="0.15"/>
                <path d="M54 28l-8 24" stroke="currentColor" stroke-width="1.5" opacity="0.15"/>
              </svg>
            </div>
          </template>
        </el-empty>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { Connection } from '@element-plus/icons-vue'
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

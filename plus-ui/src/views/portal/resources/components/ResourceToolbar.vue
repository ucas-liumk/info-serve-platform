<template>
  <header class="resource-toolbar">
    <div>
      <p>{{ activeView === 'mine' ? '我的资料' : '资源中心' }}</p>
      <h2>{{ total }} 份资料</h2>
    </div>
    <div class="toolbar-controls">
      <el-select :model-value="sort" class="toolbar-select" size="large" @change="emit('update:sort', $event)">
        <el-option label="最新发布" value="latest" />
        <el-option label="综合热度" value="hot" />
        <el-option label="下载最多" value="downloads" />
        <el-option label="浏览最多" value="views" />
      </el-select>
      <el-select :model-value="sizeRange" class="toolbar-select" size="large" @change="emit('update:sizeRange', $event)">
        <el-option label="全部大小" value="all" />
        <el-option label="1MB 以下" value="small" />
        <el-option label="1MB - 10MB" value="medium" />
        <el-option label="10MB 以上" value="large" />
      </el-select>
      <div class="mode-switch" aria-label="显示方式">
        <button :class="{ active: displayMode === 'grid' }" type="button" title="网格视图" @click="emit('update:displayMode', 'grid')">
          <el-icon><Grid /></el-icon>
        </button>
        <button :class="{ active: displayMode === 'list' }" type="button" title="列表视图" @click="emit('update:displayMode', 'list')">
          <el-icon><List /></el-icon>
        </button>
      </div>
    </div>
  </header>
</template>

<script setup lang="ts">
import { Grid, List } from '@element-plus/icons-vue';

defineProps<{
  total: number;
  sort: string;
  sizeRange: string;
  displayMode: 'grid' | 'list';
  activeView: 'center' | 'mine';
}>();

const emit = defineEmits<{
  (e: 'update:sort', value: string): void;
  (e: 'update:sizeRange', value: string): void;
  (e: 'update:displayMode', value: 'grid' | 'list'): void;
}>();
</script>

<style scoped>
.resource-toolbar {
  min-height: 68px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  border: 1px solid #e1e9f6;
  border-radius: 8px;
  padding: 13px 16px;
  background: #fff;
  box-shadow: 0 8px 24px rgba(11, 24, 51, 0.05);
}

.resource-toolbar p {
  margin: 0 0 4px;
  color: #53668f;
  font-size: 12px;
  font-weight: 650;
}

.resource-toolbar h2 {
  margin: 0;
  color: #0b1833;
  font-size: 20px;
  line-height: 1.2;
  font-weight: 850;
}

.toolbar-controls {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 10px;
  flex-wrap: wrap;
}

.toolbar-select {
  width: 132px;
}

.toolbar-select :deep(.el-select__wrapper) {
  min-height: 40px;
  border-radius: 8px;
  box-shadow: 0 0 0 1px #dbe5f4 inset;
}

.mode-switch {
  height: 40px;
  display: inline-flex;
  align-items: center;
  gap: 4px;
  border: 1px solid #dbe5f4;
  border-radius: 8px;
  padding: 3px;
  background: #f7faff;
}

.mode-switch button {
  width: 32px;
  height: 32px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border: 0;
  border-radius: 6px;
  background: transparent;
  color: #53668f;
  cursor: pointer;
}

.mode-switch button.active {
  background: #1260e8;
  color: #fff;
  box-shadow: 0 4px 10px rgba(18, 96, 232, 0.18);
}

.mode-switch button:hover:not(.active) {
  color: #1260e8;
  background: #edf4ff;
}

@media (max-width: 760px) {
  .resource-toolbar {
    align-items: flex-start;
    flex-direction: column;
  }

  .toolbar-controls {
    width: 100%;
    justify-content: flex-start;
  }

  .toolbar-select {
    width: min(100%, 148px);
  }
}
</style>

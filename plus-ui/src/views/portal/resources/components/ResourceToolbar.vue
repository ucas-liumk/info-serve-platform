<template>
  <header class="resource-toolbar">
    <div class="toolbar-controls">
      <label class="toolbar-filter">
        <span>排序</span>
        <el-select :model-value="sort" class="toolbar-select" size="large" @change="emit('update:sort', $event)">
          <el-option label="最新发布" value="latest" />
          <el-option label="综合热度" value="hot" />
          <el-option label="下载最多" value="downloads" />
          <el-option label="浏览最多" value="views" />
        </el-select>
      </label>
      <label class="toolbar-filter">
        <span>资料类型</span>
        <el-select :model-value="previewType" class="toolbar-select" size="large" @change="emit('update:previewType', $event)">
          <el-option label="全部类型" value="all" />
          <el-option label="Office/WPS" value="office" />
          <el-option label="PDF 文件" value="pdf" />
          <el-option label="OFD 文件" value="ofd" />
          <el-option label="文本资料" value="text" />
          <el-option label="图片素材" value="image" />
          <el-option label="视频资料" value="video" />
          <el-option label="音频资料" value="audio" />
          <el-option label="其他文件" value="file" />
        </el-select>
      </label>
      <label class="toolbar-filter">
        <span>上传时间</span>
        <el-select :model-value="uploadedWithin" class="toolbar-select" size="large" @change="emit('update:uploadedWithin', $event)">
          <el-option label="全部时间" value="all" />
          <el-option label="最近一周" value="week" />
          <el-option label="最近一月" value="month" />
          <el-option label="最近三月" value="quarter" />
          <el-option label="最近一年" value="year" />
        </el-select>
      </label>
      <label class="toolbar-filter">
        <span>文件大小</span>
        <el-select :model-value="sizeRange" class="toolbar-select" size="large" @change="emit('update:sizeRange', $event)">
          <el-option label="全部大小" value="all" />
          <el-option label="1MB 以下" value="small" />
          <el-option label="1MB - 10MB" value="medium" />
          <el-option label="10MB 以上" value="large" />
        </el-select>
      </label>
    </div>
    <div class="toolbar-right">
      <span class="total-text">共 {{ total }} 份资料</span>
      <div class="mode-switch" aria-label="显示方式" role="group">
        <button
          :class="{ active: displayMode === 'grid' }"
          type="button"
          title="网格视图"
          aria-label="切换为网格视图"
          :aria-pressed="displayMode === 'grid'"
          @click="emit('update:displayMode', 'grid')"
        >
          <el-icon><Grid /></el-icon>
        </button>
        <button
          :class="{ active: displayMode === 'list' }"
          type="button"
          title="表格视图"
          aria-label="切换为表格视图"
          :aria-pressed="displayMode === 'list'"
          @click="emit('update:displayMode', 'list')"
        >
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
  previewType: string;
  uploadedWithin: string;
  sizeRange: string;
  displayMode: 'grid' | 'list';
}>();

const emit = defineEmits<{
  (e: 'update:sort', value: string): void;
  (e: 'update:previewType', value: string): void;
  (e: 'update:uploadedWithin', value: string): void;
  (e: 'update:sizeRange', value: string): void;
  (e: 'update:displayMode', value: 'grid' | 'list'): void;
}>();
</script>

<style scoped>
.resource-toolbar {
  min-height: 64px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  border-bottom: 1px solid var(--ip-neutral-200);
  padding: 16px 24px;
  background: var(--ip-neutral-0);
}

.toolbar-controls,
.toolbar-right {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.toolbar-filter {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  color: var(--ip-neutral-600);
  font-size: var(--ip-font-hint);
  font-weight: 600;
  white-space: nowrap;
}

.toolbar-select {
  width: 128px;
}

.toolbar-select :deep(.el-select__wrapper) {
  min-height: 36px;
  border-radius: var(--ip-radius-sm);
  box-shadow: 0 0 0 1px var(--ip-neutral-200) inset;
}

.total-text {
  color: var(--ip-neutral-500);
  font-size: var(--ip-font-hint);
  font-weight: 600;
}

.mode-switch {
  height: 36px;
  display: inline-flex;
  align-items: center;
  gap: 4px;
  border: 1px solid var(--ip-neutral-200);
  border-radius: var(--ip-radius-sm);
  padding: 0;
  background: var(--ip-neutral-50);
}

.mode-switch button {
  width: 32px;
  height: 32px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border: 0;
  border-radius: var(--ip-radius-sm);
  background: transparent;
  color: var(--ip-neutral-500);
  cursor: pointer;
}

.mode-switch button.active {
  background: var(--ip-primary-600);
  color: var(--ip-neutral-0);
  box-shadow: var(--ip-shadow-sm);
}

.mode-switch button:hover:not(.active) {
  color: var(--ip-primary-600);
  background: var(--ip-primary-50);
}

.mode-switch button:focus-visible {
  outline: none;
  box-shadow: var(--ip-focus-ring);
}

@media (max-width: 767px) {
  .resource-toolbar {
    align-items: flex-start;
    flex-direction: column;
  }

  .toolbar-select {
    width: min(100%, 144px);
  }

  .resource-toolbar {
    padding: 16px;
  }
  .toolbar-right {
    width: 100%;
    justify-content: space-between;
  }
  .mode-switch {
    height: 48px;
  }
  .mode-switch button {
    width: 44px;
    height: 44px;
  }
}
</style>

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
        <span>资源类型</span>
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
  border-bottom: 1px solid #e1e9f6;
  padding: 14px 20px;
  background: #fff;
}

.toolbar-controls,
.toolbar-right {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.toolbar-filter {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  color: #53668f;
  font-size: 13px;
  font-weight: 750;
  white-space: nowrap;
}

.toolbar-select {
  width: 128px;
}

.toolbar-select :deep(.el-select__wrapper) {
  min-height: 34px;
  border-radius: 8px;
  box-shadow: 0 0 0 1px #dbe5f4 inset;
}

.total-text {
  color: #53668f;
  font-size: 13px;
  font-weight: 750;
}

.mode-switch {
  height: 34px;
  display: inline-flex;
  align-items: center;
  gap: 4px;
  border: 1px solid #dbe5f4;
  border-radius: 8px;
  padding: 3px;
  background: #f7faff;
}

.mode-switch button {
  width: 28px;
  height: 28px;
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

  .toolbar-select {
    width: min(100%, 148px);
  }
}
</style>

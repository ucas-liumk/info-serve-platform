<template>
  <div class="resource-list">
    <article v-for="item in resources" :key="item.resourceId" class="resource-row">
      <div class="row-icon">
        <el-icon><Document /></el-icon>
      </div>
      <div class="row-main">
        <div class="row-title">
          <button type="button" @click="emit('detail', item)">{{ item.title }}</button>
          <span>{{ item.categoryName || '未分类' }}</span>
          <em>{{ item.fileSuffix || item.previewType || 'file' }}</em>
        </div>
        <p>{{ item.description || item.originalName || '暂无简介' }}</p>
        <div class="row-meta">
          <span>{{ formatSize(item.fileSize) }}</span>
          <span>浏览 {{ item.viewCount || 0 }}</span>
          <span>下载 {{ item.downloadCount || 0 }}</span>
          <span>{{ item.createTime || '-' }}</span>
        </div>
      </div>
      <div class="row-actions">
        <button type="button" @click="emit('detail', item)">
          <el-icon><View /></el-icon>
          查看
        </button>
        <button type="button" @click="emit('download', item)">
          <el-icon><Download /></el-icon>
          下载
        </button>
      </div>
    </article>
  </div>
</template>

<script setup lang="ts">
import { Document, Download, View } from '@element-plus/icons-vue';
import type { InfoResource } from '@/api/infoservice/types';

defineProps<{
  resources: InfoResource[];
}>();

const emit = defineEmits<{
  (e: 'detail', resource: InfoResource): void;
  (e: 'download', resource: InfoResource): void;
}>();

const formatSize = (size?: number) => {
  if (!size) return '-';
  if (size < 1024) return `${size} B`;
  if (size < 1024 * 1024) return `${(size / 1024).toFixed(1)} KB`;
  return `${(size / 1024 / 1024).toFixed(1)} MB`;
};
</script>

<style scoped>
.resource-list {
  display: grid;
  gap: 12px;
}

.resource-row {
  min-height: 104px;
  display: grid;
  grid-template-columns: 48px minmax(0, 1fr) auto;
  align-items: center;
  gap: 14px;
  border: 1px solid #e1e9f6;
  border-radius: 8px;
  padding: 14px;
  background: #fff;
  box-shadow: 0 8px 24px rgba(11, 24, 51, 0.05);
}

.row-icon {
  width: 48px;
  height: 48px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 8px;
  background: #edf4ff;
  color: #1260e8;
  font-size: 23px;
}

.row-main {
  min-width: 0;
}

.row-title {
  min-width: 0;
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.row-title button {
  max-width: min(100%, 520px);
  overflow: hidden;
  border: 0;
  padding: 0;
  background: transparent;
  color: #0b1833;
  font-size: 16px;
  font-weight: 850;
  text-align: left;
  text-overflow: ellipsis;
  white-space: nowrap;
  cursor: pointer;
}

.row-title button:hover {
  color: #1260e8;
}

.row-title span,
.row-title em {
  height: 22px;
  display: inline-flex;
  align-items: center;
  border-radius: 6px;
  padding: 0 7px;
  font-size: 12px;
  font-style: normal;
  font-weight: 700;
}

.row-title span {
  background: #edf4ff;
  color: #1260e8;
}

.row-title em {
  background: #f7faff;
  color: #53668f;
}

.row-main p {
  margin: 8px 0 0;
  overflow: hidden;
  color: #53668f;
  font-size: 13px;
  font-weight: 650;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.row-meta {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
  margin-top: 9px;
  color: #8a97af;
  font-size: 12px;
  font-weight: 650;
}

.row-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.row-actions button {
  height: 32px;
  display: inline-flex;
  align-items: center;
  gap: 6px;
  border: 1px solid #e1e9f6;
  border-radius: 8px;
  padding: 0 10px;
  background: #fff;
  color: #25395f;
  font-weight: 700;
  cursor: pointer;
}

.row-actions button:hover {
  border-color: #1260e8;
  color: #1260e8;
  background: #edf4ff;
}

@media (max-width: 760px) {
  .resource-row {
    grid-template-columns: 42px minmax(0, 1fr);
  }

  .row-actions {
    grid-column: 1 / -1;
    justify-content: flex-start;
  }
}
</style>

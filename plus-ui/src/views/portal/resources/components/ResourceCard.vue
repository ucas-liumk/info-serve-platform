<template>
  <article class="resource-card" @click="emit('preview', resource)">
    <div :class="['file-mark', typeClass]">
      <el-icon><component :is="typeIcon" /></el-icon>
      <span>{{ typeLabel }}</span>
    </div>
    <div class="resource-body">
      <div class="resource-head">
        <span class="category">{{ resource.categoryName || '未分类' }}</span>
        <span class="suffix">{{ resource.fileSuffix || resource.previewType || 'file' }}</span>
      </div>
      <button class="title-button" type="button" @click.stop="emit('preview', resource)">
        {{ resource.title }}
      </button>
      <p>{{ resource.description || resource.originalName || '暂无简介' }}</p>
      <div class="resource-meta">
        <span>{{ formatSize(resource.fileSize) }}</span>
        <span>浏览 {{ resource.viewCount || 0 }}</span>
        <span>下载 {{ resource.downloadCount || 0 }}</span>
      </div>
      <div class="resource-actions">
        <button type="button" @click.stop="emit('preview', resource)">
          <el-icon><View /></el-icon>
          预览
        </button>
        <button type="button" @click.stop="emit('download', resource)">
          <el-icon><Download /></el-icon>
          下载
        </button>
      </div>
    </div>
  </article>
</template>

<script setup lang="ts">
import { computed } from 'vue';
import { Document, Download, Files, Headset, Picture, VideoCamera, View } from '@element-plus/icons-vue';
import type { InfoResource } from '@/api/infoservice/types';

const props = defineProps<{
  resource: InfoResource;
}>();

const emit = defineEmits<{
  (e: 'preview', resource: InfoResource): void;
  (e: 'download', resource: InfoResource): void;
}>();

const typeClass = computed(() => props.resource.previewType || 'file');

const typeLabel = computed(() => {
  const suffix = props.resource.fileSuffix || props.resource.previewType || 'FILE';
  return suffix.toUpperCase().slice(0, 5);
});

const typeIcon = computed(() => {
  switch (props.resource.previewType) {
    case 'image':
      return Picture;
    case 'video':
      return VideoCamera;
    case 'audio':
      return Headset;
    case 'pdf':
    case 'office':
    case 'ofd':
    case 'text':
      return Document;
    default:
      return Files;
  }
});

const formatSize = (size?: number) => {
  if (!size) return '-';
  if (size < 1024) return `${size} B`;
  if (size < 1024 * 1024) return `${(size / 1024).toFixed(1)} KB`;
  return `${(size / 1024 / 1024).toFixed(1)} MB`;
};
</script>

<style scoped>
.resource-card {
  min-height: 246px;
  display: flex;
  gap: 16px;
  border: 1px solid #e1e9f6;
  border-radius: 8px;
  padding: 18px;
  background: #fff;
  box-shadow: 0 8px 24px rgba(11, 24, 51, 0.05);
  transition:
    border-color 0.18s ease,
    box-shadow 0.18s ease,
    transform 0.18s ease;
}

.resource-card:hover {
  border-color: #b8cff5;
  box-shadow: 0 10px 28px rgba(11, 24, 51, 0.08);
  transform: translateY(-1px);
}

.file-mark {
  width: 74px;
  height: 94px;
  flex: 0 0 74px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-direction: column;
  gap: 8px;
  border: 1px solid #d7e5fb;
  border-radius: 8px;
  background: #edf4ff;
  color: #1260e8;
}

.file-mark .el-icon {
  font-size: 26px;
}

.file-mark span {
  max-width: 60px;
  overflow: hidden;
  font-size: 12px;
  font-weight: 800;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.file-mark.pdf {
  border-color: #ffd8d3;
  background: #fff2f1;
  color: #d93026;
}

.file-mark.image {
  border-color: #d7e5fb;
  background: #edf4ff;
  color: #1260e8;
}

.file-mark.video,
.file-mark.audio {
  border-color: #cfe4f7;
  background: #f0f8ff;
  color: #155f9c;
}

.resource-body {
  min-width: 0;
  flex: 1;
}

.resource-head,
.resource-meta,
.resource-actions {
  display: flex;
  align-items: center;
  gap: 9px;
  flex-wrap: wrap;
}

.category,
.suffix {
  height: 24px;
  display: inline-flex;
  align-items: center;
  border-radius: 6px;
  padding: 0 8px;
  font-size: 12px;
  font-weight: 700;
}

.category {
  background: #edf4ff;
  color: #1260e8;
}

.suffix {
  background: #f7faff;
  color: #53668f;
}

.title-button {
  width: 100%;
  margin: 12px 0 8px;
  overflow: hidden;
  border: 0;
  padding: 0;
  background: transparent;
  color: #0b1833;
  font-size: 18px;
  line-height: 1.32;
  font-weight: 850;
  text-align: left;
  text-overflow: ellipsis;
  white-space: nowrap;
  cursor: pointer;
}

.title-button:hover {
  color: #1260e8;
}

.resource-card p {
  min-height: 44px;
  margin: 0;
  display: -webkit-box;
  overflow: hidden;
  color: #53668f;
  font-size: 14px;
  line-height: 1.55;
  font-weight: 650;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.resource-meta {
  margin-top: 13px;
  color: #8a97af;
  font-size: 12px;
  font-weight: 650;
}

.resource-actions {
  margin-top: 15px;
}

.resource-actions button {
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

.resource-actions button:hover {
  border-color: #1260e8;
  color: #1260e8;
  background: #edf4ff;
}

@media (max-width: 520px) {
  .resource-card {
    align-items: stretch;
    flex-direction: column;
  }

  .file-mark {
    width: 100%;
    height: 70px;
    flex-basis: 70px;
    flex-direction: row;
  }
}
</style>

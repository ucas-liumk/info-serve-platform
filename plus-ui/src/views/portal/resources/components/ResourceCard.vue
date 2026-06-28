<template>
  <article class="resource-card">
    <div :class="['file-mark', typeClass]">
      <el-icon><component :is="typeIcon" /></el-icon>
      <span>{{ typeLabel }}</span>
    </div>
    <div class="resource-body">
      <div class="resource-head">
        <span class="category">{{ resource.categoryName || '未分类' }}</span>
        <span class="suffix">{{ resource.fileSuffix || resource.previewType || 'file' }}</span>
      </div>
      <button class="title-button" type="button" @click="emit('detail', resource)">
        {{ resource.title }}
      </button>
      <p>{{ resource.description || resource.originalName || '暂无简介' }}</p>
      <div class="resource-meta">
        <span>{{ formatSize(resource.fileSize) }}</span>
        <span>浏览 {{ resource.viewCount || 0 }}</span>
        <span>下载 {{ resource.downloadCount || 0 }}</span>
      </div>
      <div class="resource-actions">
        <button type="button" @click="emit('detail', resource)">
          <el-icon><View /></el-icon>
          查看
        </button>
        <button type="button" @click="emit('download', resource)">
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
  (e: 'detail', resource: InfoResource): void;
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
  border: 1px solid #dbe6f2;
  border-radius: 8px;
  padding: 18px;
  background: rgba(255, 255, 255, 0.96);
  box-shadow: 0 16px 32px rgba(12, 47, 96, 0.07);
  transition:
    border-color 0.18s ease,
    box-shadow 0.18s ease,
    transform 0.18s ease;
}

.resource-card:hover {
  border-color: #b7ccec;
  box-shadow: 0 18px 38px rgba(12, 47, 96, 0.11);
  transform: translateY(-2px);
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
  border: 1px solid #cddcee;
  border-radius: 8px;
  background: #eef5ff;
  color: #164d92;
}

.file-mark .el-icon {
  font-size: 26px;
}

.file-mark span {
  max-width: 60px;
  overflow: hidden;
  font-size: 12px;
  font-weight: 950;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.file-mark.pdf {
  border-color: #f0c9bb;
  background: #fff1ec;
  color: #a54d2f;
}

.file-mark.image {
  border-color: #dfd0a5;
  background: #fff7df;
  color: #8a640a;
}

.file-mark.video,
.file-mark.audio {
  border-color: #c6d6cd;
  background: #eff9f3;
  color: #276b49;
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
  font-weight: 850;
}

.category {
  background: #eaf3ff;
  color: #195aa1;
}

.suffix {
  background: #f3f6fa;
  color: #5a6880;
}

.title-button {
  width: 100%;
  margin: 12px 0 8px;
  overflow: hidden;
  border: 0;
  padding: 0;
  background: transparent;
  color: #071f4b;
  font-size: 18px;
  line-height: 1.32;
  font-weight: 950;
  text-align: left;
  text-overflow: ellipsis;
  white-space: nowrap;
  cursor: pointer;
}

.title-button:hover {
  color: #0f62b6;
}

.resource-card p {
  min-height: 44px;
  margin: 0;
  display: -webkit-box;
  overflow: hidden;
  color: #5f6e84;
  font-size: 14px;
  line-height: 1.55;
  font-weight: 650;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.resource-meta {
  margin-top: 13px;
  color: #75849b;
  font-size: 12px;
  font-weight: 800;
}

.resource-actions {
  margin-top: 15px;
}

.resource-actions button {
  height: 32px;
  display: inline-flex;
  align-items: center;
  gap: 6px;
  border: 1px solid #d8e3ef;
  border-radius: 7px;
  padding: 0 10px;
  background: #fff;
  color: #203353;
  font-weight: 850;
  cursor: pointer;
}

.resource-actions button:hover {
  border-color: #2f74bf;
  color: #0d579e;
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

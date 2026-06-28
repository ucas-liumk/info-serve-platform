<template>
  <el-drawer v-model="visible" size="520px" title="资料详情" class="resource-detail-drawer">
    <div v-if="resource" class="detail">
      <div class="detail-head">
        <span>{{ resource.categoryName || '未分类' }}</span>
        <em>{{ resource.fileSuffix || resource.previewType || 'file' }}</em>
      </div>
      <h2>{{ resource.title }}</h2>
      <p>{{ resource.description || '暂无简介' }}</p>
      <dl>
        <dt>文件名</dt>
        <dd>{{ resource.originalName || '-' }}</dd>
        <dt>文件类型</dt>
        <dd>{{ resource.mimeType || resource.fileSuffix || '-' }}</dd>
        <dt>文件大小</dt>
        <dd>{{ formatSize(resource.fileSize) }}</dd>
        <dt>上传者</dt>
        <dd>{{ resource.ownerName || resource.createByName || '平台用户' }}</dd>
        <dt>上传时间</dt>
        <dd>{{ resource.createTime || '-' }}</dd>
        <dt>浏览下载</dt>
        <dd>浏览 {{ resource.viewCount || 0 }} · 下载 {{ resource.downloadCount || 0 }}</dd>
      </dl>
      <div class="drawer-actions">
        <button type="button" @click="emit('preview', resource)">预览</button>
        <button type="button" @click="emit('download', resource)">下载</button>
        <button v-if="resource.canManage" class="secondary" type="button" @click="emit('edit', resource)">编辑</button>
      </div>
    </div>
  </el-drawer>
</template>

<script setup lang="ts">
import { computed } from 'vue';
import type { InfoResource } from '@/api/infoservice/types';

const props = defineProps<{
  modelValue: boolean;
  resource?: InfoResource;
}>();

const emit = defineEmits<{
  (e: 'update:modelValue', value: boolean): void;
  (e: 'preview', resource: InfoResource): void;
  (e: 'download', resource: InfoResource): void;
  (e: 'edit', resource: InfoResource): void;
}>();

const visible = computed({
  get: () => props.modelValue,
  set: (value: boolean) => emit('update:modelValue', value)
});

const formatSize = (size?: number) => {
  if (!size) return '-';
  if (size < 1024) return `${size} B`;
  if (size < 1024 * 1024) return `${(size / 1024).toFixed(1)} KB`;
  return `${(size / 1024 / 1024).toFixed(1)} MB`;
};
</script>

<style scoped>
.detail-head {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.detail-head span,
.detail-head em {
  height: 26px;
  display: inline-flex;
  align-items: center;
  border-radius: 6px;
  padding: 0 9px;
  font-size: 12px;
  font-style: normal;
  font-weight: 850;
}

.detail-head span {
  background: #eaf3ff;
  color: #195aa1;
}

.detail-head em {
  background: #f3f6fa;
  color: #5a6880;
}

.detail h2 {
  margin: 18px 0 10px;
  color: #071f4b;
  font-size: 24px;
  line-height: 1.28;
  font-weight: 950;
}

.detail p {
  margin: 0;
  color: #5d6b81;
  line-height: 1.7;
  font-weight: 650;
}

.detail dl {
  display: grid;
  grid-template-columns: 86px minmax(0, 1fr);
  gap: 13px 12px;
  margin: 24px 0;
}

.detail dt {
  color: #7b8798;
  font-weight: 850;
}

.detail dd {
  min-width: 0;
  margin: 0;
  color: #1f2d43;
  word-break: break-all;
  font-weight: 760;
}

.drawer-actions {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

.drawer-actions button {
  height: 38px;
  border: 0;
  border-radius: 8px;
  padding: 0 15px;
  background: #082b68;
  color: #fff;
  font-weight: 900;
  cursor: pointer;
}

.drawer-actions button.secondary {
  border: 1px solid #d8e3ef;
  background: #fff;
  color: #203353;
}
</style>

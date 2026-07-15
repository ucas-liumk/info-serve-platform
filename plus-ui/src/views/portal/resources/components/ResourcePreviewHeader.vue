<template>
  <header class="preview-header">
    <button class="icon-button" type="button" aria-label="返回资料列表" title="返回资料列表" @click="emit('back')"><IconBack /></button>
    <span class="type-mark">{{ typeLabel }}</span>
    <div class="file-summary">
      <strong :title="title">{{ title }}</strong>
      <span>{{ metaText }}</span>
    </div>
    <div class="header-actions">
      <button class="download-button" type="button" @click="emit('download')"><IconDownload /><span>下载原文件</span></button>
      <button v-if="canClose" class="icon-button" type="button" aria-label="关闭预览窗口" title="关闭预览窗口" @click="emit('close')">
        <IconClose />
      </button>
    </div>
  </header>
</template>

<script setup lang="ts">
import { computed } from 'vue';
import IconBack from '~icons/material-symbols/arrow-back-rounded';
import IconClose from '~icons/material-symbols/close-rounded';
import IconDownload from '~icons/material-symbols/download-rounded';
import type { InfoResource } from '@/api/infoservice/types';
import { formatFileSize } from './panelStudio';

const props = defineProps<{ resource: InfoResource; typeLabel: string; canClose: boolean }>();
const emit = defineEmits<{ back: []; download: []; close: [] }>();
const title = computed(() => props.resource.title || props.resource.originalName || '资料预览');
const metaText = computed(() => [props.resource.categoryName || '未分类', formatFileSize(props.resource.fileSize)].join(' · '));
</script>

<style scoped>
.preview-header {
  min-width: 0;
  min-height: 56px;
  display: flex;
  align-items: center;
  gap: 8px;
  border: 1px solid var(--ip-neutral-200);
  border-radius: var(--ip-radius-md);
  padding: 8px 12px;
  background: var(--ip-neutral-0);
  box-shadow: var(--ip-shadow-sm);
}
.icon-button,
.download-button {
  height: 36px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border: 1px solid var(--ip-neutral-300);
  border-radius: var(--ip-radius-sm);
  background: var(--ip-neutral-0);
  color: var(--ip-neutral-700);
  cursor: pointer;
  transition:
    border-color var(--ip-motion-fast) var(--ip-motion-ease),
    background var(--ip-motion-fast) var(--ip-motion-ease),
    color var(--ip-motion-fast) var(--ip-motion-ease);
}
.icon-button {
  width: 36px;
  flex: 0 0 auto;
}
.icon-button svg,
.download-button svg {
  width: 20px;
  height: 20px;
}
.icon-button:hover,
.download-button:hover {
  border-color: var(--ip-primary-300);
  background: var(--ip-primary-50);
  color: var(--ip-primary-700);
}
.type-mark {
  flex: 0 0 auto;
  border: 1px solid var(--ip-primary-200);
  border-radius: var(--ip-radius-sm);
  padding: 4px 8px;
  background: var(--ip-primary-50);
  color: var(--ip-primary-700);
  font-size: var(--ip-font-caption);
  font-weight: 700;
}
.file-summary {
  min-width: 0;
  display: grid;
  gap: 2px;
}
.file-summary strong {
  overflow: hidden;
  color: var(--ip-neutral-900);
  font-size: var(--ip-font-body);
  font-weight: 700;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.file-summary span {
  color: var(--ip-neutral-500);
  font-size: var(--ip-font-caption);
}
.header-actions {
  margin-left: auto;
  display: flex;
  align-items: center;
  gap: 8px;
}
.download-button {
  gap: 6px;
  padding: 0 12px;
  font-size: var(--ip-font-caption);
  font-weight: 700;
}
@media (max-width: 767px) {
  .preview-header {
    min-height: 60px;
    padding: 8px;
  }
  .icon-button,
  .download-button {
    width: 44px;
    height: 44px;
    padding: 0;
  }
  .download-button span,
  .file-summary span {
    display: none;
  }
}
</style>

<template>
  <div class="wps-toolbar" role="toolbar" aria-label="视图工具条">
    <button
      type="button"
      class="wps-tool"
      :class="{ 'is-on': panActive }"
      :disabled="disabled"
      :aria-pressed="panActive"
      title="拖拽平移（再次点击恢复选择）"
      @click="emit('toggle-pan')"
    >
      <el-icon><Rank /></el-icon>
      拖拽
    </button>
    <span class="wps-sep" aria-hidden="true"></span>
    <button
      v-for="option in PAGE_MODE_OPTIONS"
      :key="option.key"
      type="button"
      class="wps-tool"
      :class="{ 'is-on': pageMode === option.key }"
      :disabled="disabled"
      :aria-pressed="pageMode === option.key"
      :title="option.title"
      @click="emit('set-page-mode', option.key)"
    >
      <el-icon><component :is="option.icon" /></el-icon>
      {{ option.label }}
    </button>
    <span class="wps-sep" aria-hidden="true"></span>
    <button type="button" class="wps-tool" :disabled="disabled" title="逆时针旋转 90°" @click="emit('rotate-left')">
      <el-icon><RefreshLeft /></el-icon>
      左旋转
    </button>
    <button type="button" class="wps-tool" :disabled="disabled" title="顺时针旋转 90°" @click="emit('rotate-right')">
      <el-icon><RefreshRight /></el-icon>
      右旋转
    </button>
    <span class="wps-sep" aria-hidden="true"></span>
    <button type="button" class="wps-tool" :disabled="disabled" title="选中正文文字后，一键引用到我的笔记" @click="emit('quote-selection')">
      <el-icon><CollectionTag /></el-icon>
      引用到笔记
    </button>
    <button
      type="button"
      class="wps-tool"
      :class="{ 'is-on': darkActive }"
      :disabled="disabled"
      :aria-pressed="darkActive"
      :title="darkActive ? '切回浅色阅读' : '深色阅读（夜间护眼）'"
      @click="emit('toggle-theme')"
    >
      <el-icon><component :is="darkActive ? Sunny : Moon" /></el-icon>
      {{ darkActive ? '日间' : '夜间' }}
    </button>
    <button
      type="button"
      class="wps-tool"
      :class="{ 'is-on': fullscreenActive }"
      :disabled="disabled"
      :aria-pressed="fullscreenActive"
      :title="fullscreenActive ? '退出全屏' : '全屏阅读'"
      @click="emit('toggle-fullscreen')"
    >
      <el-icon><FullScreen /></el-icon>
      {{ fullscreenActive ? '退出全屏' : '全屏' }}
    </button>
  </div>
</template>

<script setup lang="ts">
import type { Component } from 'vue';
import { CollectionTag, Document, FullScreen, Moon, Rank, Reading, RefreshLeft, RefreshRight, Sunny, Tickets } from '@element-plus/icons-vue';
import type { PageViewMode } from './pdfWpsControls';

defineProps<{
  panActive: boolean;
  pageMode: PageViewMode;
  disabled: boolean;
  darkActive: boolean;
  fullscreenActive: boolean;
}>();

const emit = defineEmits<{
  'toggle-pan': [];
  'set-page-mode': [mode: PageViewMode];
  'rotate-left': [];
  'rotate-right': [];
  'quote-selection': [];
  'toggle-theme': [];
  'toggle-fullscreen': [];
}>();

interface PageModeOption {
  readonly key: PageViewMode;
  readonly label: string;
  readonly title: string;
  readonly icon: Component;
}

/** 三个页模式互斥（active 态由 pageMode 单值驱动）；语义映射见 pdfWpsControls.ts 头注 */
const PAGE_MODE_OPTIONS: readonly PageModeOption[] = Object.freeze([
  { key: 'continuous', label: '连页', title: '连页（单页纵向连续滚动）', icon: Document },
  { key: 'single', label: '单页', title: '单页（整页充满视口）', icon: Tickets },
  { key: 'double', label: '双页连续', title: '双页连续（两页并排滚动）', icon: Reading }
]);
</script>

<style scoped>
.wps-toolbar {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 2px;
  padding: 7px 10px;
  border-bottom: 1px solid var(--ip-neutral-100);
  background: var(--ip-neutral-50);
}

.wps-tool {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  border: 0;
  border-radius: var(--ip-radius-sm);
  padding: 5px 10px;
  background: transparent;
  color: var(--ip-neutral-500);
  font-size: var(--ip-font-caption);
  line-height: 1.4;
  cursor: pointer;
  transition:
    background var(--ip-motion-fast) var(--ip-motion-ease),
    color var(--ip-motion-fast) var(--ip-motion-ease);
}

.wps-tool:hover:not(:disabled) {
  background: var(--ip-primary-50);
  color: var(--ip-primary-600);
}

.wps-tool.is-on {
  background: var(--ip-primary-100);
  color: var(--ip-primary-600);
  font-weight: 700;
}

.wps-tool:disabled {
  opacity: 0.5;
  cursor: default;
}

.wps-sep {
  flex: 0 0 auto;
  width: 1px;
  height: 16px;
  margin: 0 6px;
  background: var(--ip-neutral-200);
}
</style>

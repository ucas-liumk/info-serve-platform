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
      <IconPan />
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
      <component :is="option.icon" />
      {{ option.label }}
    </button>
    <span class="wps-sep" aria-hidden="true"></span>
    <button type="button" class="wps-tool" :disabled="disabled" title="逆时针旋转 90°" @click="emit('rotate-left')">
      <IconRotateLeft />
      左旋转
    </button>
    <button type="button" class="wps-tool" :disabled="disabled" title="顺时针旋转 90°" @click="emit('rotate-right')">
      <IconRotateRight />
      右旋转
    </button>
    <span class="wps-sep" aria-hidden="true"></span>
    <button type="button" class="wps-tool" :disabled="disabled" title="选中正文文字后，一键引用到我的笔记" @click="emit('quote-selection')">
      <IconQuote />
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
      <component :is="darkActive ? IconLight : IconDark" />
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
      <IconFullscreen />
      {{ fullscreenActive ? '退出全屏' : '全屏' }}
    </button>
  </div>
</template>

<script setup lang="ts">
import type { Component } from 'vue';
import IconContinuous from '~icons/material-symbols/view-agenda-outline';
import IconDark from '~icons/material-symbols/dark-mode-outline-rounded';
import IconDouble from '~icons/material-symbols/view-column-2-outline';
import IconFullscreen from '~icons/material-symbols/fullscreen-rounded';
import IconLight from '~icons/material-symbols/light-mode-outline-rounded';
import IconPan from '~icons/material-symbols/pan-tool-outline-rounded';
import IconQuote from '~icons/material-symbols/bookmark-add-outline-rounded';
import IconRotateLeft from '~icons/material-symbols/rotate-left-rounded';
import IconRotateRight from '~icons/material-symbols/rotate-right-rounded';
import IconSingle from '~icons/material-symbols/article-outline-rounded';
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
  { key: 'continuous', label: '连页', title: '连页（单页纵向连续滚动）', icon: IconContinuous },
  { key: 'single', label: '单页', title: '单页（整页充满视口）', icon: IconSingle },
  { key: 'double', label: '双页连续', title: '双页连续（两页并排滚动）', icon: IconDouble }
]);
</script>

<style scoped>
.wps-toolbar {
  display: flex;
  align-items: center;
  justify-content: flex-start;
  gap: 4px;
  overflow-x: auto;
  padding: 8px 12px;
  border-bottom: 1px solid var(--ip-neutral-200);
  background: var(--ip-neutral-50);
}

.wps-tool {
  display: inline-flex;
  align-items: center;
  min-height: 36px;
  gap: 6px;
  flex: 0 0 auto;
  border: 1px solid transparent;
  border-radius: var(--ip-radius-sm);
  padding: 0 10px;
  background: transparent;
  color: var(--ip-neutral-600);
  font-size: var(--ip-font-caption);
  line-height: 1.4;
  cursor: pointer;
  transition:
    background var(--ip-motion-fast) var(--ip-motion-ease),
    color var(--ip-motion-fast) var(--ip-motion-ease);
}

.wps-tool:hover:not(:disabled) {
  border-color: var(--ip-primary-200);
  background: var(--ip-primary-50);
  color: var(--ip-primary-700);
}

.wps-tool.is-on {
  border-color: var(--ip-primary-200);
  background: var(--ip-primary-100);
  color: var(--ip-primary-700);
  font-weight: 700;
}

.wps-tool svg {
  width: 18px;
  height: 18px;
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
  background: var(--ip-neutral-300);
}
</style>

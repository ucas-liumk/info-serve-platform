<template>
  <header class="resource-header">
    <div class="heading">
      <span class="eyebrow">资料共享</span>
      <h1>{{ title }}</h1>
      <p>{{ subtitle }}</p>
    </div>

    <div class="header-actions">
      <div class="search-group" role="search">
        <el-input
          :model-value="keyword"
          class="resource-search"
          clearable
          aria-label="搜索资料"
          placeholder="搜索资料标题、关键词、作者或标签"
          @update:model-value="emit('update:keyword', $event)"
          @keyup.enter="emit('search')"
          @clear="emit('search')"
        >
          <template #prefix
            ><el-icon><Search /></el-icon
          ></template>
        </el-input>
        <button class="search-button" type="button" @click="emit('search')">搜索</button>
      </div>

      <div class="utility-actions">
        <PortalNotificationBell />
        <button class="upload-button" type="button" @click="emit('upload')">
          <el-icon><UploadFilled /></el-icon><span>上传资料</span>
        </button>
        <button class="mine-button" type="button" @click="emit('mine')">
          <el-icon><User /></el-icon><span>我的资料</span>
        </button>
      </div>
    </div>
  </header>
</template>

<script setup lang="ts">
import { Search, UploadFilled, User } from '@element-plus/icons-vue';
import PortalNotificationBell from '@/layout/portal/components/PortalNotificationBell.vue';

defineProps<{ title: string; subtitle: string; keyword: string }>();
const emit = defineEmits<{
  (e: 'update:keyword', value: string): void;
  (e: 'search'): void;
  (e: 'upload'): void;
  (e: 'mine'): void;
}>();
</script>

<style scoped>
.resource-header {
  display: grid;
  grid-template-columns: minmax(200px, 320px) minmax(0, 1fr);
  align-items: center;
  gap: 24px;
  border: 1px solid var(--ip-neutral-200);
  border-radius: var(--ip-radius-md);
  padding: 16px 24px;
  background: var(--ip-neutral-0);
  box-shadow: var(--ip-shadow-sm);
}

.heading {
  min-width: 0;
}
.eyebrow {
  color: var(--ip-primary-600);
  font-size: var(--ip-font-caption);
  font-weight: 700;
}
.heading h1 {
  margin: 4px 0 0;
  color: var(--ip-neutral-900);
  font-size: var(--ip-font-title);
  line-height: 1.2;
  font-weight: 700;
}
.heading p {
  margin: 4px 0 0;
  color: var(--ip-neutral-500);
  font-size: var(--ip-font-hint);
  font-weight: 500;
}
.header-actions {
  min-width: 0;
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 12px;
}
.search-group {
  min-width: 280px;
  max-width: 560px;
  flex: 1;
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 8px;
}
.resource-search {
  --el-input-height: 36px;
}
.resource-search :deep(.el-input__wrapper) {
  border-radius: var(--ip-radius-sm);
  box-shadow: 0 0 0 1px var(--ip-neutral-300) inset;
}
.resource-search :deep(.el-input__wrapper.is-focus) {
  box-shadow: 0 0 0 1px var(--ip-primary-600) inset;
}
.utility-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}
.search-button,
.upload-button,
.mine-button {
  height: 36px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  border-radius: var(--ip-radius-sm);
  padding: 0 16px;
  font-size: var(--ip-font-body);
  font-weight: 600;
  white-space: nowrap;
  cursor: pointer;
  transition:
    border-color var(--ip-motion-fast) var(--ip-motion-ease),
    background var(--ip-motion-fast) var(--ip-motion-ease),
    color var(--ip-motion-fast) var(--ip-motion-ease);
}
.search-button {
  border: 1px solid var(--ip-primary-600);
  background: var(--ip-primary-600);
  color: var(--ip-neutral-0);
}
.search-button:hover {
  background: var(--ip-primary-700);
}
.upload-button {
  border: 1px solid var(--ip-primary-300);
  background: var(--ip-neutral-0);
  color: var(--ip-primary-700);
}
.upload-button:hover {
  border-color: var(--ip-primary-600);
  background: var(--ip-primary-50);
}
.mine-button {
  border: 1px solid transparent;
  background: transparent;
  color: var(--ip-neutral-600);
}
.mine-button:hover {
  background: var(--ip-neutral-100);
  color: var(--ip-neutral-900);
}
button:focus-visible {
  outline: none;
  box-shadow: var(--ip-focus-ring);
}

@media (max-width: 1200px) {
  .resource-header {
    grid-template-columns: 1fr;
  }
  .header-actions {
    justify-content: flex-start;
  }
}

@media (max-width: 767px) {
  .resource-header {
    gap: 16px;
    padding: 16px;
  }
  .header-actions {
    align-items: stretch;
    flex-direction: column;
  }
  .search-group {
    width: 100%;
    min-width: 0;
    grid-template-columns: minmax(0, 1fr) auto;
  }
  .utility-actions {
    display: grid;
    grid-template-columns: auto 1fr 1fr;
  }
  .search-button,
  .upload-button,
  .mine-button {
    min-height: 44px;
    padding: 0 12px;
  }
}
</style>

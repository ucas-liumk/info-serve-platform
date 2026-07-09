<template>
  <div class="resource-list">
    <article v-for="item in resources" :key="item.resourceId" class="resource-row" @click="emit('preview', item)">
      <div class="row-icon">
        <el-icon><Document /></el-icon>
      </div>
      <div class="row-main">
        <div class="row-title">
          <button type="button" @click.stop="emit('preview', item)">{{ item.title }}</button>
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
        <button type="button" @click.stop="emit('preview', item)">
          <el-icon><View /></el-icon>
          预览
        </button>
        <button type="button" @click.stop="emit('download', item)">
          <el-icon><Download /></el-icon>
          下载
        </button>
        <button :class="{ active: item.favorited }" type="button" @click.stop="emit('favorite', item)">
          <el-icon>
            <StarFilled v-if="item.favorited" />
            <Star v-else />
          </el-icon>
          {{ item.favorited ? '已收藏' : '收藏' }}
        </button>

        <!-- scope=mine 时挂回自管理能力：编辑 / 替换 / 上下架 / 删除（handler 在页面侧） -->
        <el-dropdown v-if="manageable" trigger="click" placement="bottom-end" @command="(cmd: string | number | object) => onManageCommand(cmd, item)">
          <button class="row-manage" type="button" aria-label="管理" @click.stop>
            <span class="i-material-symbols:more-vert" aria-hidden="true" />
          </button>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="edit">编辑资料</el-dropdown-item>
              <el-dropdown-item command="replace">替换文件</el-dropdown-item>
              <el-dropdown-item command="status">{{ item.status === '0' ? '下架' : '上架' }}</el-dropdown-item>
              <el-dropdown-item divided command="delete">删除</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>
    </article>
  </div>
</template>

<script setup lang="ts">
import { Document, Download, Star, StarFilled, View } from '@element-plus/icons-vue';
import type { InfoResource } from '@/api/infoservice/types';

withDefaults(
  defineProps<{
    resources: InfoResource[];
    manageable?: boolean;
  }>(),
  { manageable: false }
);

const emit = defineEmits<{
  (e: 'preview', resource: InfoResource): void;
  (e: 'download', resource: InfoResource): void;
  (e: 'favorite', resource: InfoResource): void;
  (e: 'edit', resource: InfoResource): void;
  (e: 'replace', resource: InfoResource): void;
  (e: 'status', resource: InfoResource, status: string): void;
  (e: 'delete', resource: InfoResource): void;
}>();

const onManageCommand = (command: string | number | object, resource: InfoResource) => {
  if (command === 'edit') emit('edit', resource);
  else if (command === 'replace') emit('replace', resource);
  else if (command === 'status') emit('status', resource, resource.status === '0' ? '1' : '0');
  else if (command === 'delete') emit('delete', resource);
};

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
  border: 1px solid var(--resource-border, #dce5ed);
  border-radius: 8px;
  padding: 14px;
  background: #fff;
  box-shadow: 0 8px 24px rgba(11, 24, 51, 0.05);
  cursor: pointer;
}

.resource-row:hover {
  border-color: #b8c9d9;
  background: #fbfcfe;
}

.row-icon {
  width: 48px;
  height: 48px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 8px;
  background: var(--resource-accent-soft, #e7f4f0);
  color: var(--resource-accent, #2f8a7a);
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
  color: var(--resource-title, #14243a);
  font-size: 16px;
  font-weight: 850;
  text-align: left;
  text-overflow: ellipsis;
  white-space: nowrap;
  cursor: pointer;
}

.row-title button:hover {
  color: var(--resource-primary, #245f8f);
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
  background: var(--resource-accent-soft, #e7f4f0);
  color: var(--resource-accent, #2f8a7a);
}

.row-title em {
  background: #f5f7fa;
  color: var(--resource-muted, #68788c);
}

.row-main p {
  margin: 8px 0 0;
  overflow: hidden;
  color: var(--resource-muted, #68788c);
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
  color: var(--resource-weak, #96a1af);
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
  border: 1px solid var(--resource-border, #dce5ed);
  border-radius: 8px;
  padding: 0 10px;
  background: #fff;
  color: var(--resource-text, #32445c);
  font-weight: 700;
  cursor: pointer;
}

.row-actions button:hover {
  border-color: var(--resource-primary, #245f8f);
  color: var(--resource-primary, #245f8f);
  background: var(--resource-primary-soft, #eaf2f8);
}

.row-actions button.active {
  border-color: rgba(47, 138, 122, 0.38);
  background: var(--resource-accent-soft, #e7f4f0);
  color: var(--resource-accent, #2f8a7a);
}

.row-manage {
  width: 32px;
  height: 32px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border: 1px solid var(--ip-neutral-200);
  border-radius: var(--ip-radius-sm);
  padding: 0;
  background: var(--ip-neutral-0);
  color: var(--ip-neutral-500);
  font-size: var(--ip-font-title-sm);
  cursor: pointer;
  transition:
    border-color var(--ip-motion-fast) var(--ip-motion-ease),
    color var(--ip-motion-fast) var(--ip-motion-ease),
    background var(--ip-motion-fast) var(--ip-motion-ease);
}

.row-manage:hover {
  border-color: var(--ip-primary-600);
  color: var(--ip-primary-600);
  background: var(--ip-primary-50);
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

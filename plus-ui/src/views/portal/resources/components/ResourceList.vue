<template>
  <div class="table-shell">
    <table class="resource-table">
      <thead>
        <tr>
          <th scope="col">资料名称</th>
          <th scope="col">分类 / 类型</th>
          <th scope="col">文件大小</th>
          <th scope="col">浏览 / 下载</th>
          <th scope="col">发布时间</th>
          <th scope="col">操作</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="item in resources" :key="item.resourceId" @dblclick="emit('preview', item)">
          <td data-label="资料名称">
            <div class="name-cell">
              <span class="file-icon" aria-hidden="true"
                ><el-icon><Document /></el-icon
              ></span>
              <span class="name-copy">
                <button type="button" :title="item.title" @click="emit('preview', item)">{{ item.title }}</button>
                <small :title="item.description || item.originalName || '暂无简介'">{{ item.description || item.originalName || '暂无简介' }}</small>
              </span>
            </div>
          </td>
          <td data-label="分类 / 类型">
            <span class="category-tag">{{ item.categoryName || '未分类' }}</span>
            <span class="type-text">{{ typeLabel(item) }}</span>
          </td>
          <td data-label="文件大小">{{ formatSize(item.fileSize) }}</td>
          <td data-label="浏览 / 下载">
            <span class="metric">{{ item.viewCount || 0 }} / {{ item.downloadCount || 0 }}</span>
          </td>
          <td class="date-cell" data-label="发布时间">{{ formatDate(item.createTime) }}</td>
          <td data-label="操作">
            <div class="row-actions">
              <button type="button" @click="emit('preview', item)">
                <el-icon><View /></el-icon><span>预览</span>
              </button>
              <button type="button" @click="emit('download', item)">
                <el-icon><Download /></el-icon><span>下载</span>
              </button>
              <button :class="{ active: item.favorited }" type="button" @click="emit('favorite', item)">
                <el-icon><StarFilled v-if="item.favorited" /><Star v-else /></el-icon>
                <span>{{ item.favorited ? '已收藏' : '收藏' }}</span>
              </button>
            </div>
          </td>
        </tr>
      </tbody>
    </table>
  </div>
</template>

<script setup lang="ts">
import { Document, Download, Star, StarFilled, View } from '@element-plus/icons-vue';
import type { InfoResource } from '@/api/infoservice/types';

defineProps<{ resources: InfoResource[] }>();
const emit = defineEmits<{
  (e: 'preview', resource: InfoResource): void;
  (e: 'download', resource: InfoResource): void;
  (e: 'favorite', resource: InfoResource): void;
}>();

const typeLabel = (resource: InfoResource) =>
  String(resource.fileSuffix || resource.previewType || 'file')
    .replace(/^\./, '')
    .toUpperCase();

const formatSize = (size?: number) => {
  if (!size) return '-';
  if (size < 1024) return `${size} B`;
  if (size < 1024 * 1024) return `${(size / 1024).toFixed(1)} KB`;
  return `${(size / 1024 / 1024).toFixed(1)} MB`;
};

const formatDate = (value?: string) => {
  if (!value) return '-';
  return String(value).replace('T', ' ').slice(0, 16);
};
</script>

<style scoped>
.table-shell {
  overflow: hidden;
  border: 1px solid var(--ip-neutral-200);
  border-radius: var(--ip-radius-md);
  background: var(--ip-neutral-0);
}
.resource-table {
  width: 100%;
  border-collapse: collapse;
  table-layout: fixed;
  color: var(--ip-neutral-700);
  font-size: var(--ip-font-hint);
}
th {
  height: 44px;
  padding: 0 12px;
  background: var(--ip-neutral-100);
  color: var(--ip-neutral-600);
  font-weight: 600;
  text-align: left;
}
th:first-child {
  width: 30%;
}
th:nth-child(2) {
  width: 13%;
}
th:nth-child(3) {
  width: 9%;
}
th:nth-child(4) {
  width: 10%;
}
th:nth-child(5) {
  width: 14%;
}
th:last-child {
  width: 24%;
}
td {
  min-width: 0;
  height: 72px;
  border-top: 1px solid var(--ip-neutral-100);
  padding: 12px;
  vertical-align: middle;
}
tbody tr {
  transition: background var(--ip-motion-fast) var(--ip-motion-ease);
}
tbody tr:hover {
  background: var(--ip-neutral-50);
}
.name-cell {
  min-width: 0;
  display: flex;
  align-items: center;
  gap: 12px;
}
.file-icon {
  width: 40px;
  height: 40px;
  flex: 0 0 40px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: var(--ip-radius-sm);
  background: var(--ip-mod-resources-soft);
  color: var(--ip-mod-resources);
  font-size: 20px;
}
.name-copy {
  min-width: 0;
  display: grid;
  gap: 4px;
}
.name-copy button {
  max-width: 100%;
  overflow: hidden;
  border: 0;
  padding: 0;
  background: transparent;
  color: var(--ip-neutral-900);
  font-size: var(--ip-font-body);
  font-weight: 600;
  text-align: left;
  text-overflow: ellipsis;
  white-space: nowrap;
  cursor: pointer;
}
.name-copy button:hover {
  color: var(--ip-primary-600);
}
.name-copy small {
  overflow: hidden;
  color: var(--ip-neutral-500);
  font-size: var(--ip-font-caption);
  text-overflow: ellipsis;
  white-space: nowrap;
}
.category-tag {
  display: inline-flex;
  max-width: 100%;
  overflow: hidden;
  border-radius: var(--ip-radius-full);
  padding: 4px 8px;
  background: var(--ip-mod-resources-soft);
  color: var(--ip-mod-resources);
  font-size: var(--ip-font-caption);
  font-weight: 600;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.type-text {
  display: block;
  margin-top: 4px;
  color: var(--ip-neutral-400);
  font-size: var(--ip-font-caption);
}
.metric {
  white-space: nowrap;
}
.date-cell {
  white-space: nowrap;
  font-variant-numeric: tabular-nums;
}
.row-actions {
  display: flex;
  align-items: center;
  gap: 4px;
}
.row-actions button {
  min-width: 32px;
  height: 32px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 4px;
  border: 1px solid var(--ip-neutral-200);
  border-radius: var(--ip-radius-sm);
  padding: 0 8px;
  background: var(--ip-neutral-0);
  color: var(--ip-neutral-600);
  font-size: var(--ip-font-caption);
  font-weight: 600;
  white-space: nowrap;
  cursor: pointer;
}
.row-actions button:hover {
  border-color: var(--ip-primary-300);
  background: var(--ip-primary-50);
  color: var(--ip-primary-700);
}
.row-actions button.active {
  border-color: var(--ip-mod-resources-border);
  background: var(--ip-mod-resources-soft);
  color: var(--ip-mod-resources);
}
button:focus-visible {
  outline: none;
  box-shadow: var(--ip-focus-ring);
}

@media (max-width: 1199px) {
  th:nth-child(3),
  td:nth-child(3),
  th:nth-child(4),
  td:nth-child(4) {
    display: none;
  }
  th:first-child {
    width: 40%;
  }
  th:nth-child(2) {
    width: 18%;
  }
  th:nth-child(5) {
    width: 18%;
  }
  th:last-child {
    width: 24%;
  }
  .row-actions button span {
    position: absolute;
    width: 1px;
    height: 1px;
    overflow: hidden;
    clip: rect(0, 0, 0, 0);
  }
}

@media (max-width: 767px) {
  .table-shell {
    overflow: visible;
    border: 0;
    background: transparent;
  }
  .resource-table,
  tbody {
    display: block;
  }
  thead {
    position: absolute;
    width: 1px;
    height: 1px;
    overflow: hidden;
    clip: rect(0, 0, 0, 0);
  }
  tr {
    display: grid;
    grid-template-columns: 1fr 1fr;
    gap: 8px 16px;
    margin-bottom: 12px;
    border: 1px solid var(--ip-neutral-200);
    border-radius: var(--ip-radius-md);
    padding: 16px;
    background: var(--ip-neutral-0);
    box-shadow: var(--ip-shadow-sm);
  }
  td,
  td:nth-child(3),
  td:nth-child(4) {
    height: auto;
    display: grid;
    grid-template-columns: 88px minmax(0, 1fr);
    align-items: center;
    gap: 8px;
    border: 0;
    padding: 0;
  }
  td::before {
    content: attr(data-label);
    color: var(--ip-neutral-400);
    font-size: var(--ip-font-caption);
    font-weight: 600;
  }
  td:first-child,
  td:last-child {
    grid-column: 1 / -1;
    display: block;
  }
  td:first-child::before,
  td:last-child::before {
    display: none;
  }
  .date-cell {
    white-space: normal;
  }
  .row-actions {
    margin-top: 8px;
    display: grid;
    grid-template-columns: repeat(3, 1fr);
    gap: 8px;
  }
  .row-actions button {
    min-height: 44px;
  }
  .row-actions button span {
    position: static;
    width: auto;
    height: auto;
    overflow: visible;
    clip: auto;
  }
}
</style>

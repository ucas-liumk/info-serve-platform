<template>
  <section class="mine-table">
    <el-table :data="resources" :height="tableHeight" row-key="resourceId" class="resource-table">
      <el-table-column label="资料" min-width="280">
        <template #default="{ row }">
          <div class="resource-name">
            <strong>{{ row.title }}</strong>
            <span>{{ row.originalName || row.description || '暂无文件名' }}</span>
          </div>
        </template>
      </el-table-column>
      <el-table-column label="分类" prop="categoryName" width="120" />
      <el-table-column label="格式" width="96">
        <template #default="{ row }">
          <span class="suffix">{{ row.fileSuffix || row.previewType || 'file' }}</span>
        </template>
      </el-table-column>
      <el-table-column label="大小" width="110">
        <template #default="{ row }">{{ formatSize(row.fileSize) }}</template>
      </el-table-column>
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="statusType(row.status)" effect="light">{{ statusLabel(row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="数据" width="128">
        <template #default="{ row }">
          <div class="metric">
            <span>浏览 {{ row.viewCount || 0 }}</span>
            <span>下载 {{ row.downloadCount || 0 }}</span>
          </div>
        </template>
      </el-table-column>
      <el-table-column label="上传时间" prop="createTime" min-width="150" />
      <el-table-column label="操作" width="350" fixed="right">
        <template #default="{ row }">
          <div class="table-actions">
            <button type="button" @click="emit('detail', row)">查看</button>
            <button type="button" @click="emit('preview', row)">预览</button>
            <button type="button" @click="emit('download', row)">下载</button>
            <button type="button" @click="emit('edit', row)">编辑</button>
            <button type="button" @click="emit('replace', row)">重传</button>
            <button type="button" @click="emit('status', row, row.status === '0' ? '1' : '0')">
              {{ row.status === '0' ? '下架' : '发布' }}
            </button>
            <button class="danger" type="button" @click="emit('delete', row)">删除</button>
          </div>
        </template>
      </el-table-column>
    </el-table>
  </section>
</template>

<script setup lang="ts">
import type { InfoResource } from '@/api/infoservice/types';

withDefaults(
  defineProps<{
    resources: InfoResource[];
    tableHeight?: string | number;
  }>(),
  {
    tableHeight: 520
  }
);

const emit = defineEmits<{
  (e: 'detail', resource: InfoResource): void;
  (e: 'preview', resource: InfoResource): void;
  (e: 'download', resource: InfoResource): void;
  (e: 'edit', resource: InfoResource): void;
  (e: 'replace', resource: InfoResource): void;
  (e: 'status', resource: InfoResource, status: string): void;
  (e: 'delete', resource: InfoResource): void;
}>();

const statusLabel = (status?: string) => {
  if (status === '0') return '已发布';
  if (status === '1') return '已下架';
  return '待处理';
};

const statusType = (status?: string) => {
  if (status === '0') return 'success';
  if (status === '1') return 'info';
  return 'warning';
};

const formatSize = (size?: number) => {
  if (!size) return '-';
  if (size < 1024) return `${size} B`;
  if (size < 1024 * 1024) return `${(size / 1024).toFixed(1)} KB`;
  return `${(size / 1024 / 1024).toFixed(1)} MB`;
};
</script>

<style scoped>
.mine-table {
  border: 1px solid #e1e9f6;
  border-radius: 8px;
  overflow: hidden;
  background: #fff;
  box-shadow: 0 8px 24px rgba(11, 24, 51, 0.05);
}

.resource-table {
  width: 100%;
}

.mine-table :deep(.el-table th.el-table__cell) {
  background: #f7faff;
  color: #53668f;
  font-weight: 700;
}

.mine-table :deep(.el-table td.el-table__cell) {
  color: #25395f;
}

.resource-name {
  min-width: 0;
  display: grid;
  gap: 5px;
}

.resource-name strong {
  overflow: hidden;
  color: #0b1833;
  font-size: 14px;
  font-weight: 800;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.resource-name span {
  overflow: hidden;
  color: #53668f;
  font-size: 12px;
  font-weight: 650;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.suffix {
  height: 24px;
  display: inline-flex;
  align-items: center;
  border-radius: 6px;
  padding: 0 8px;
  background: #f7faff;
  color: #53668f;
  font-size: 12px;
  font-weight: 700;
}

.metric {
  display: grid;
  gap: 3px;
  color: #8a97af;
  font-size: 12px;
  font-weight: 650;
}

.table-actions {
  display: flex;
  align-items: center;
  gap: 6px;
  flex-wrap: wrap;
}

.table-actions button {
  height: 28px;
  border: 1px solid #e1e9f6;
  border-radius: 6px;
  padding: 0 8px;
  background: #fff;
  color: #25395f;
  font-size: 12px;
  font-weight: 700;
  cursor: pointer;
}

.table-actions button:hover {
  border-color: #1260e8;
  color: #1260e8;
  background: #edf4ff;
}

.table-actions button.danger:hover {
  border-color: #d93026;
  color: #d93026;
  background: #fff2f1;
}
</style>

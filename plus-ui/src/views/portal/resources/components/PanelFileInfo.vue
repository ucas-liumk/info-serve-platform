<template>
  <div class="file-info" aria-label="文件信息">
    <dl class="info-list">
      <template v-for="item in items" :key="item.label">
        <dt>{{ item.label }}</dt>
        <dd>{{ item.value }}</dd>
      </template>
    </dl>
    <section v-loading="loading" class="record-section">
      <h3>阅看记录</h3>
      <div v-if="error" class="record-state" role="alert">
        <span>{{ error }}</span>
        <button type="button" @click="emit('retry')">重新加载</button>
      </div>
      <ul v-else-if="records.length > 0" class="record-list">
        <li v-for="record in records" :key="record.recordId">
          <strong>{{ record.userName || '平台用户' }}</strong>
          <span>{{ record.actionType === 'view' ? '打开资料预览' : '查看资料' }}</span>
          <time>{{ formatDateTime(record.createTime) }}</time>
        </li>
      </ul>
      <p v-else-if="!loading" class="record-empty">暂无阅看记录</p>
    </section>
  </div>
</template>

<script setup lang="ts">
import type { ResourceViewRecord } from '@/api/infoservice/types';
import type { ResourceInfoItem } from './panelStudio';
import { formatDateTime } from './panelStudio';

defineProps<{
  items: readonly ResourceInfoItem[];
  records: ResourceViewRecord[];
  loading: boolean;
  error?: string;
}>();

const emit = defineEmits<{ retry: [] }>();
</script>

<style scoped>
.file-info {
  margin: 8px 16px;
  border: 1px solid var(--ip-neutral-200);
  border-radius: var(--ip-radius-md);
  padding: 12px 14px;
  background: var(--ip-neutral-50);
}

.info-list {
  margin: 0;
  display: grid;
  grid-template-columns: 72px minmax(0, 1fr);
  row-gap: 6px;
  font-size: var(--ip-font-caption);
}

.info-list dt {
  color: var(--ip-neutral-500);
  font-weight: 600;
}

.info-list dd {
  min-width: 0;
  margin: 0;
  overflow-wrap: anywhere;
  color: var(--ip-neutral-700);
  font-weight: 600;
}

.record-section {
  margin-top: 12px;
  border-top: 1px solid var(--ip-neutral-100);
  padding-top: 10px;
}

.record-section h3 {
  margin: 0 0 8px;
  color: var(--ip-neutral-500);
  font-size: var(--ip-font-caption);
  font-weight: 700;
}

.record-list {
  margin: 0;
  max-height: 320px;
  overflow: auto;
  padding: 0;
  list-style: none;
  display: grid;
  gap: 7px;
}

.record-list li {
  display: flex;
  align-items: baseline;
  gap: 6px;
  font-size: var(--ip-font-caption);
}

.record-list strong {
  flex: 0 0 auto;
  max-width: 96px;
  overflow: hidden;
  color: var(--ip-neutral-800);
  font-weight: 700;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.record-list span {
  flex: 1 1 auto;
  overflow: hidden;
  color: var(--ip-neutral-600);
  text-overflow: ellipsis;
  white-space: nowrap;
}

.record-list time {
  flex: 0 0 auto;
  color: var(--ip-neutral-500);
}

.record-empty,
.record-state {
  margin: 0;
  color: var(--ip-neutral-500);
  font-size: var(--ip-font-caption);
  font-weight: 600;
}

.record-state {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.record-state button {
  border: 0;
  padding: 0;
  background: transparent;
  color: var(--ip-primary-700);
  font: inherit;
  font-weight: 700;
  cursor: pointer;
}
</style>

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
      <ul v-if="records.length > 0" class="record-list">
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
}>();
</script>

<style scoped>
.file-info {
  margin: 8px 14px;
  border: 1px solid var(--resource-border);
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
  color: var(--resource-weak);
  font-weight: 750;
}

.info-list dd {
  min-width: 0;
  margin: 0;
  overflow-wrap: anywhere;
  color: var(--resource-text);
  font-weight: 760;
}

.record-section {
  margin-top: 12px;
  border-top: 1px solid var(--ip-neutral-100);
  padding-top: 10px;
}

.record-section h3 {
  margin: 0 0 8px;
  color: var(--resource-weak);
  font-size: var(--ip-font-caption);
  font-weight: 800;
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
  color: var(--resource-title);
  font-weight: 850;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.record-list span {
  flex: 1 1 auto;
  overflow: hidden;
  color: var(--resource-muted);
  text-overflow: ellipsis;
  white-space: nowrap;
}

.record-list time {
  flex: 0 0 auto;
  color: var(--resource-weak);
}

.record-empty {
  margin: 0;
  color: var(--resource-weak);
  font-size: var(--ip-font-caption);
  font-weight: 750;
}
</style>

<template>
  <el-drawer v-model="visible" class="my-resources-drawer" size="560px" :with-header="false" append-to-body>
    <section class="drawer-shell">
      <header class="drawer-head">
        <div>
          <strong>我的资源</strong>
          <span>上传、收藏、下载与浏览记录</span>
        </div>
        <button class="close-button" type="button" title="关闭" @click="visible = false">
          <el-icon><Close /></el-icon>
        </button>
      </header>

      <el-tabs v-model="active" class="resource-tabs">
        <el-tab-pane label="我上传的" name="uploads" />
        <el-tab-pane label="我收藏的" name="favorites" />
        <el-tab-pane label="我下载的" name="downloads" />
        <el-tab-pane label="浏览记录" name="history" />
      </el-tabs>

      <div v-if="activeTab === 'uploads'" class="upload-summary">
        <span>共 {{ total }} 份</span>
        <button type="button" @click="emit('upload')">
          <el-icon><UploadFilled /></el-icon>
          上传资料
        </button>
      </div>

      <div v-loading="loading" class="drawer-body">
        <template v-if="activeTab === 'uploads' || activeTab === 'favorites'">
          <article v-for="item in resources" :key="item.resourceId" class="resource-row">
            <button class="row-main" type="button" @click="emit('preview', item)">
              <span class="file-mark">{{ typeLabel(item) }}</span>
              <span class="row-text">
                <strong>{{ item.title }}</strong>
                <em>{{ item.originalName || item.description || '暂无文件名' }}</em>
                <small>{{ item.categoryName || '未分类' }} · {{ formatSize(item.fileSize) }} · {{ item.createTime || '-' }}</small>
              </span>
            </button>
            <div class="row-meta">
              <span :class="['status-pill', item.status === '0' ? 'online' : 'offline']">{{ statusLabel(item.status) }}</span>
              <span>浏览 {{ item.viewCount || 0 }}</span>
              <span>下载 {{ item.downloadCount || 0 }}</span>
            </div>
            <div class="row-actions">
              <button type="button" @click="emit('preview', item)">预览</button>
              <button type="button" @click="emit('download', item)">下载</button>
              <button v-if="activeTab === 'uploads'" type="button" @click="emit('edit', item)">编辑</button>
              <button v-if="activeTab === 'uploads'" type="button" @click="emit('replace', item)">重传</button>
              <button v-if="activeTab === 'uploads'" type="button" @click="emit('status', item, item.status === '0' ? '1' : '0')">
                {{ item.status === '0' ? '下架' : '发布' }}
              </button>
              <button v-if="activeTab === 'favorites'" type="button" @click="emit('favorite', item)">取消收藏</button>
              <button v-if="activeTab === 'uploads'" class="danger" type="button" @click="emit('delete', item)">删除</button>
            </div>
          </article>
          <el-empty v-if="!loading && resources.length === 0" :image-size="92" :description="activeTab === 'uploads' ? '暂无上传资料' : '暂无收藏记录'" />
        </template>

        <el-empty v-else :image-size="92" :description="emptyText" />
      </div>
    </section>
  </el-drawer>
</template>

<script setup lang="ts">
import { computed } from 'vue';
import { Close, UploadFilled } from '@element-plus/icons-vue';
import type { InfoResource } from '@/api/infoservice/types';

type MyResourceTab = 'uploads' | 'favorites' | 'downloads' | 'history';

const props = defineProps<{
  modelValue: boolean;
  resources: InfoResource[];
  total: number;
  loading: boolean;
  activeTab: MyResourceTab;
}>();

const emit = defineEmits<{
  (e: 'update:modelValue', value: boolean): void;
  (e: 'change-tab', value: MyResourceTab): void;
  (e: 'preview', resource: InfoResource): void;
  (e: 'download', resource: InfoResource): void;
  (e: 'edit', resource: InfoResource): void;
  (e: 'replace', resource: InfoResource): void;
  (e: 'status', resource: InfoResource, status: string): void;
  (e: 'delete', resource: InfoResource): void;
  (e: 'favorite', resource: InfoResource): void;
  (e: 'upload'): void;
}>();

const visible = computed({
  get: () => props.modelValue,
  set: (value: boolean) => emit('update:modelValue', value)
});

const active = computed<MyResourceTab>({
  get: () => props.activeTab,
  set: (value) => emit('change-tab', value)
});

const emptyText = computed(() => {
  if (props.activeTab === 'favorites') return '暂无收藏记录';
  if (props.activeTab === 'downloads') return '暂无下载记录';
  return '暂无浏览记录';
});

const statusLabel = (status?: string) => {
  if (status === '0') return '已发布';
  if (status === '1') return '已下架';
  return '待处理';
};

const typeLabel = (resource: InfoResource) => {
  const suffix = resource.fileSuffix || resource.previewType || 'file';
  return suffix.toUpperCase().slice(0, 5);
};

const formatSize = (size?: number) => {
  if (!size) return '-';
  if (size < 1024) return `${size} B`;
  if (size < 1024 * 1024) return `${(size / 1024).toFixed(1)} KB`;
  return `${(size / 1024 / 1024).toFixed(1)} MB`;
};
</script>

<style scoped>
.drawer-shell {
  min-height: 100%;
  display: grid;
  grid-template-rows: auto auto auto minmax(0, 1fr);
  background: #f5f7fa;
  color: var(--resource-text, #32445c);
  font-family: 'HarmonyOS Sans SC', 'PingFang SC', 'Microsoft YaHei', sans-serif;
}

.drawer-head {
  min-height: 78px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  border-bottom: 1px solid var(--resource-border, #dce5ed);
  padding: 18px 20px;
  background: #fff;
}

.drawer-head div {
  display: grid;
  gap: 5px;
}

.drawer-head strong {
  color: var(--resource-title, #14243a);
  font-size: 22px;
  line-height: 1.1;
  font-weight: 900;
}

.drawer-head span {
  color: var(--resource-muted, #68788c);
  font-size: 13px;
  font-weight: 700;
}

.close-button {
  width: 34px;
  height: 34px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border: 1px solid var(--resource-border, #dce5ed);
  border-radius: 8px;
  background: #fff;
  color: var(--resource-muted, #68788c);
  cursor: pointer;
}

.close-button:hover {
  border-color: var(--resource-primary, #245f8f);
  color: var(--resource-primary, #245f8f);
  background: var(--resource-primary-soft, #eaf2f8);
}

.resource-tabs {
  padding: 0 18px;
  background: #fff;
}

.resource-tabs :deep(.el-tabs__header) {
  margin: 0;
}

.resource-tabs :deep(.el-tabs__item) {
  height: 46px;
  color: var(--resource-muted, #68788c);
  font-size: 15px;
  font-weight: 800;
}

.resource-tabs :deep(.el-tabs__item.is-active) {
  color: var(--resource-primary, #245f8f);
}

.resource-tabs :deep(.el-tabs__active-bar) {
  background: var(--resource-primary, #245f8f);
}

.upload-summary {
  height: 54px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  border-bottom: 1px solid var(--resource-border, #dce5ed);
  padding: 0 20px;
  background: #fff;
}

.upload-summary span {
  color: var(--resource-muted, #68788c);
  font-size: 13px;
  font-weight: 800;
}

.upload-summary button {
  height: 32px;
  display: inline-flex;
  align-items: center;
  gap: 6px;
  border: 1px solid var(--resource-primary, #245f8f);
  border-radius: 8px;
  padding: 0 11px;
  background: var(--resource-primary, #245f8f);
  color: #fff;
  font-weight: 800;
  cursor: pointer;
}

.upload-summary button:hover {
  background: var(--resource-primary-deep, #183f63);
}

.drawer-body {
  min-height: 0;
  display: grid;
  align-content: start;
  gap: 12px;
  padding: 16px;
  overflow: auto;
}

.resource-row {
  display: grid;
  gap: 12px;
  border: 1px solid var(--resource-border, #dce5ed);
  border-radius: 8px;
  padding: 13px;
  background: #fff;
  box-shadow: 0 8px 24px rgba(11, 24, 51, 0.05);
}

.row-main {
  min-width: 0;
  display: grid;
  grid-template-columns: 54px minmax(0, 1fr);
  gap: 12px;
  border: 0;
  padding: 0;
  background: transparent;
  text-align: left;
  cursor: pointer;
}

.file-mark {
  width: 54px;
  height: 64px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border: 1px solid #d7e5fb;
  border-radius: 8px;
  background: var(--resource-primary-soft, #eaf2f8);
  color: var(--resource-primary, #245f8f);
  font-size: 12px;
  font-weight: 900;
}

.row-text {
  min-width: 0;
  display: grid;
  gap: 5px;
  align-content: center;
}

.row-text strong,
.row-text em,
.row-text small {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.row-text strong {
  color: var(--resource-title, #14243a);
  font-size: 15px;
  font-weight: 900;
}

.row-main:hover .row-text strong {
  color: var(--resource-primary, #245f8f);
}

.row-text em {
  color: var(--resource-muted, #68788c);
  font-size: 13px;
  font-style: normal;
  font-weight: 700;
}

.row-text small {
  color: var(--resource-weak, #96a1af);
  font-size: 12px;
  font-weight: 650;
}

.row-meta,
.row-actions {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
}

.row-meta {
  gap: 8px;
  color: var(--resource-weak, #96a1af);
  font-size: 12px;
  font-weight: 700;
}

.status-pill {
  height: 22px;
  display: inline-flex;
  align-items: center;
  border-radius: 6px;
  padding: 0 7px;
  background: #eef3f8;
  color: var(--resource-muted, #68788c);
}

.status-pill.online {
  background: #edf8f1;
  color: #168245;
}

.status-pill.offline {
  background: #f4f6fa;
  color: #6a7894;
}

.row-actions {
  gap: 7px;
}

.row-actions button {
  height: 30px;
  border: 1px solid var(--resource-border, #dce5ed);
  border-radius: 7px;
  padding: 0 9px;
  background: #fff;
  color: var(--resource-text, #32445c);
  font-size: 12px;
  font-weight: 800;
  cursor: pointer;
}

.row-actions button:hover {
  border-color: var(--resource-primary, #245f8f);
  background: var(--resource-primary-soft, #eaf2f8);
  color: var(--resource-primary, #245f8f);
}

.row-actions button.danger:hover {
  border-color: #d93026;
  background: #fff2f1;
  color: #d93026;
}

.my-resources-drawer :deep(.el-drawer__body) {
  padding: 0;
}

@media (max-width: 640px) {
  .my-resources-drawer :deep(.el-drawer) {
    width: 100% !important;
  }
}
</style>

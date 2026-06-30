<template>
  <div class="resource-preview-page">
    <header class="preview-topbar">
      <div class="brand-block">
        <img src="@/assets/portal/module-resource.png" alt="资源共享" />
        <div>
          <strong>资源共享</strong>
          <span>资料预览</span>
        </div>
      </div>

      <div class="topbar-actions">
        <button class="ghost-button" type="button" @click="goResourceList">
          <el-icon><Back /></el-icon>
          返回资源
        </button>
        <button class="primary-button" type="button" :disabled="!resource" @click="downloadResource">
          <el-icon><Download /></el-icon>
          下载资料
        </button>
        <button class="icon-button" type="button" title="关闭" @click="closePreview">
          <el-icon><Close /></el-icon>
        </button>
      </div>
    </header>

    <main v-loading="loading" class="preview-shell">
      <section class="preview-stage">
        <header class="stage-head">
          <div>
            <span>{{ resource?.categoryName || '未分类' }}</span>
            <strong>{{ resource?.title || '资料预览' }}</strong>
          </div>
          <em>{{ resource?.fileSuffix || resource?.previewType || 'FILE' }}</em>
        </header>

        <div class="stage-body">
          <iframe v-if="previewSrc" :src="previewSrc" title="资料预览" @error="handlePreviewFrameError"></iframe>
          <div v-else class="stage-empty">
            <el-empty :image-size="110" :description="previewError || '暂无可预览内容'" />
          </div>
        </div>
      </section>

      <aside class="info-panel">
        <section class="info-card">
          <div class="file-mark">{{ typeLabel }}</div>
          <div class="file-title">
            <h1>{{ resource?.title || '-' }}</h1>
            <p>{{ resource?.description || resource?.originalName || '暂无简介' }}</p>
          </div>
        </section>

        <section class="meta-card">
          <h2>资料信息</h2>
          <dl>
            <div>
              <dt>文件名</dt>
              <dd>{{ resource?.originalName || '-' }}</dd>
            </div>
            <div>
              <dt>分类</dt>
              <dd>{{ resource?.categoryName || '未分类' }}</dd>
            </div>
            <div>
              <dt>格式</dt>
              <dd>{{ resource?.fileSuffix || resource?.previewType || '-' }}</dd>
            </div>
            <div>
              <dt>大小</dt>
              <dd>{{ formatSize(resource?.fileSize) }}</dd>
            </div>
            <div>
              <dt>上传人</dt>
              <dd>{{ resource?.ownerName || resource?.createByName || '-' }}</dd>
            </div>
            <div>
              <dt>上传时间</dt>
              <dd>{{ resource?.createTime || '-' }}</dd>
            </div>
          </dl>
        </section>

        <section class="metric-card">
          <div>
            <strong>{{ resource?.viewCount || 0 }}</strong>
            <span>浏览</span>
          </div>
          <div>
            <strong>{{ resource?.downloadCount || 0 }}</strong>
            <span>下载</span>
          </div>
        </section>
      </aside>
    </main>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';
import { Back, Close, Download } from '@element-plus/icons-vue';
import { getToken } from '@/utils/auth';
import { getResource, getResourceKkPreviewUrl, resourceDownloadUrl } from '@/api/infoservice/portal';
import type { InfoResource } from '@/api/infoservice/types';

const route = useRoute();
const router = useRouter();
const resource = ref<InfoResource>();
const previewSrc = ref('');
const previewError = ref('');
const loading = ref(false);

const resourceId = computed(() => String(route.params.resourceId || ''));

const withAuth = (url: string) => {
  const target = new URL(url, window.location.origin);
  const token = getToken();
  if (token) target.searchParams.set('Authorization', `Bearer ${token}`);
  target.searchParams.set('clientid', import.meta.env.VITE_APP_CLIENT_ID);
  return target.toString();
};

const unwrapResponseData = <T,>(payload: any): T | undefined => {
  const body = payload?.data !== undefined ? payload.data : payload;
  if (body?.code !== undefined && body?.data !== undefined) {
    return body.data;
  }
  return body;
};

const isLoopbackHost = (host: string) => ['localhost', '127.0.0.1', '[::1]', '::1'].includes(host.toLowerCase());

const normalizePreviewSrc = (url?: string) => {
  if (!url) return '';
  try {
    const target = new URL(url, window.location.origin);
    if (isLoopbackHost(target.hostname) && !isLoopbackHost(window.location.hostname)) {
      target.hostname = window.location.hostname;
    }
    if (window.location.protocol === 'https:' && target.protocol === 'http:' && target.hostname === window.location.hostname) {
      target.protocol = 'https:';
    }
    return target.toString();
  } catch (error) {
    return url;
  }
};

const typeLabel = computed(() => {
  const suffix = resource.value?.fileSuffix || resource.value?.previewType || 'FILE';
  return suffix.toUpperCase().slice(0, 5);
});

const loadResource = async () => {
  if (!/^\d+$/.test(resourceId.value)) {
    ElMessage.error('资料不存在');
    return;
  }
  loading.value = true;
  previewError.value = '';
  try {
    const resourceRes: any = await getResource(resourceId.value);
    resource.value = unwrapResponseData<InfoResource>(resourceRes);

    const previewRes: any = await getResourceKkPreviewUrl(resourceId.value);
    previewSrc.value = normalizePreviewSrc(unwrapResponseData<string>(previewRes));
    if (!previewSrc.value) {
      previewError.value = '暂未获取到预览地址';
    }
  } catch (error) {
    previewSrc.value = '';
    previewError.value = '资料暂时无法预览';
    ElMessage.error('资料暂时无法预览');
  } finally {
    loading.value = false;
  }
};

const handlePreviewFrameError = () => {
  previewError.value = '资料暂时无法预览';
};

const goResourceList = () => {
  router.push({ name: 'InfoResources' });
};

const closePreview = () => {
  if (window.opener) {
    window.close();
    return;
  }
  goResourceList();
};

const downloadResource = () => {
  const id = resource.value?.resourceId || resourceId.value;
  if (!id) return;
  window.open(withAuth(resourceDownloadUrl(id)), '_blank');
};

const formatSize = (size?: number) => {
  if (!size) return '-';
  if (size < 1024) return `${size} B`;
  if (size < 1024 * 1024) return `${(size / 1024).toFixed(1)} KB`;
  return `${(size / 1024 / 1024).toFixed(1)} MB`;
};

onMounted(loadResource);
</script>

<style scoped>
.resource-preview-page {
  min-height: 100vh;
  --resource-primary: #1260e8;
  --resource-primary-soft: #edf4ff;
  --resource-title: #0b1833;
  --resource-text: #25395f;
  --resource-muted: #53668f;
  --resource-weak: #8a97af;
  --resource-border: #e1e9f6;
  display: grid;
  grid-template-rows: auto minmax(0, 1fr);
  gap: 16px;
  padding: 18px 28px 28px;
  background: linear-gradient(180deg, rgba(237, 244, 255, 0.9) 0%, rgba(247, 250, 255, 0.72) 320px), #f7faff;
  color: var(--resource-text);
  font-family: 'HarmonyOS Sans SC', 'PingFang SC', 'Microsoft YaHei', sans-serif;
}

.preview-topbar,
.preview-stage,
.info-card,
.meta-card,
.metric-card {
  border: 1px solid var(--resource-border);
  border-radius: 8px;
  background: #fff;
  box-shadow: 0 14px 34px rgba(35, 83, 151, 0.07);
}

.preview-topbar {
  min-height: 72px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 18px;
  padding: 14px 16px;
}

.brand-block {
  min-width: 0;
  display: flex;
  align-items: center;
  gap: 12px;
}

.brand-block img {
  width: 40px;
  height: 40px;
  object-fit: contain;
}

.brand-block div {
  min-width: 0;
  display: grid;
  gap: 4px;
}

.brand-block strong {
  color: var(--resource-title);
  font-size: 21px;
  line-height: 1.1;
  font-weight: 900;
}

.brand-block span {
  color: var(--resource-muted);
  font-size: 13px;
  font-weight: 750;
}

.topbar-actions {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.ghost-button,
.primary-button,
.icon-button {
  height: 38px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 7px;
  border-radius: 8px;
  padding: 0 13px;
  font-weight: 850;
  cursor: pointer;
}

.ghost-button,
.icon-button {
  border: 1px solid #dbe5f4;
  background: #fff;
  color: var(--resource-text);
}

.primary-button {
  border: 1px solid var(--resource-primary);
  background: var(--resource-primary);
  color: #fff;
}

.primary-button:disabled {
  opacity: 0.6;
  cursor: default;
}

.icon-button {
  width: 38px;
  padding: 0;
}

.ghost-button:hover,
.icon-button:hover {
  border-color: var(--resource-primary);
  background: var(--resource-primary-soft);
  color: var(--resource-primary);
}

.primary-button:hover:not(:disabled) {
  background: #0f55cf;
}

.preview-shell {
  min-height: 0;
  display: grid;
  grid-template-columns: minmax(0, 1fr) 340px;
  gap: 16px;
}

.preview-stage {
  min-width: 0;
  min-height: calc(100vh - 134px);
  display: grid;
  grid-template-rows: auto minmax(0, 1fr);
  overflow: hidden;
}

.stage-head {
  min-height: 68px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 14px;
  border-bottom: 1px solid var(--resource-border);
  padding: 14px 18px;
}

.stage-head div {
  min-width: 0;
  display: grid;
  gap: 5px;
}

.stage-head span {
  color: var(--resource-primary);
  font-size: 12px;
  font-weight: 850;
}

.stage-head strong {
  overflow: hidden;
  color: var(--resource-title);
  font-size: 18px;
  line-height: 1.2;
  font-weight: 900;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.stage-head em {
  flex: 0 0 auto;
  border-radius: 6px;
  padding: 5px 8px;
  background: var(--resource-primary-soft);
  color: var(--resource-primary);
  font-size: 12px;
  font-style: normal;
  font-weight: 900;
}

.stage-body {
  min-height: 0;
  background: #f2f6fc;
}

.stage-empty {
  min-height: 620px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.stage-body iframe {
  width: 100%;
  height: 100%;
  min-height: 620px;
  display: block;
  border: 0;
  background: #fff;
}

.info-panel {
  min-width: 0;
  display: grid;
  align-content: start;
  gap: 14px;
}

.info-card,
.meta-card,
.metric-card {
  padding: 16px;
}

.info-card {
  display: grid;
  grid-template-columns: 72px minmax(0, 1fr);
  gap: 14px;
}

.file-mark {
  width: 72px;
  height: 90px;
  display: flex;
  align-items: center;
  justify-content: center;
  border: 1px solid #d7e5fb;
  border-radius: 8px;
  background: var(--resource-primary-soft);
  color: var(--resource-primary);
  font-size: 13px;
  font-weight: 900;
}

.file-title {
  min-width: 0;
}

.file-title h1 {
  margin: 0;
  color: var(--resource-title);
  font-size: 19px;
  line-height: 1.3;
  font-weight: 900;
}

.file-title p {
  margin: 9px 0 0;
  display: -webkit-box;
  overflow: hidden;
  color: var(--resource-muted);
  font-size: 13px;
  line-height: 1.55;
  font-weight: 700;
  -webkit-line-clamp: 4;
  -webkit-box-orient: vertical;
}

.meta-card h2 {
  margin: 0 0 14px;
  color: var(--resource-title);
  font-size: 17px;
  font-weight: 900;
}

.meta-card dl {
  margin: 0;
  display: grid;
  gap: 12px;
}

.meta-card dl div {
  display: grid;
  gap: 5px;
}

.meta-card dt {
  color: var(--resource-weak);
  font-size: 12px;
  font-weight: 800;
}

.meta-card dd {
  min-width: 0;
  margin: 0;
  overflow-wrap: anywhere;
  color: var(--resource-text);
  font-size: 14px;
  line-height: 1.45;
  font-weight: 760;
}

.metric-card {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
}

.metric-card div {
  border-radius: 8px;
  padding: 14px 10px;
  background: #f7faff;
  text-align: center;
}

.metric-card strong {
  display: block;
  color: var(--resource-title);
  font-size: 24px;
  line-height: 1.1;
  font-weight: 900;
}

.metric-card span {
  display: block;
  margin-top: 5px;
  color: var(--resource-muted);
  font-size: 12px;
  font-weight: 800;
}

@media (max-width: 1080px) {
  .preview-shell {
    grid-template-columns: 1fr;
  }

  .preview-stage {
    min-height: 680px;
  }
}

@media (max-width: 760px) {
  .resource-preview-page {
    padding: 14px;
  }

  .preview-topbar {
    align-items: stretch;
    flex-direction: column;
  }

  .topbar-actions {
    width: 100%;
  }

  .ghost-button,
  .primary-button {
    flex: 1;
  }
}
</style>

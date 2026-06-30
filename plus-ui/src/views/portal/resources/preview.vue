<template>
  <div class="resource-preview-page">
    <main v-loading="loading" class="preview-shell">
      <section class="preview-stage">
        <div class="stage-body">
          <PdfPreviewer
            v-if="previewMode === 'pdf' && previewSrc"
            :src="previewSrc"
            :title="resource?.title || resource?.originalName || '资料预览'"
            :file-name="resource?.originalName || resource?.title || '资料预览'"
            :file-suffix="resource?.fileSuffix || resource?.previewType || 'PDF'"
            :category-name="resource?.categoryName || '未分类'"
            :file-size-text="formatSize(resource?.fileSize)"
            :owner-name="resource?.ownerName || resource?.createByName || '-'"
            :create-time="resource?.createTime || '-'"
            :view-count="resource?.viewCount || 0"
            :download-count="resource?.downloadCount || 0"
            @error="handlePreviewFrameError"
            @back="goResourceList"
            @download="downloadResource"
            @close="closePreview"
          />
          <div v-else-if="previewMode !== 'none' && previewSrc" class="native-preview">
            <header class="native-preview-toolbar">
              <div class="native-file-summary">
                <span>{{ typeLabel }}</span>
                <div>
                  <strong>{{ resource?.title || resource?.originalName || '资料预览' }}</strong>
                  <p>
                    {{ resource?.categoryName || '未分类' }}
                    <em>{{ formatSize(resource?.fileSize) }}</em>
                    <em>{{ resource?.ownerName || resource?.createByName || '-' }}</em>
                    <em>{{ resource?.createTime || '-' }}</em>
                  </p>
                </div>
              </div>
              <div class="native-actions">
                <button class="ghost-button" type="button" @click="goResourceList">
                  <el-icon><Back /></el-icon>
                  返回
                </button>
                <button class="primary-button" type="button" :disabled="!resource" @click="downloadResource">
                  <el-icon><Download /></el-icon>
                  下载
                </button>
                <button class="icon-button" type="button" title="关闭" @click="closePreview">
                  <el-icon><Close /></el-icon>
                </button>
              </div>
            </header>
            <iframe v-if="previewMode === 'iframe'" :src="previewSrc" title="资料预览" @error="handlePreviewFrameError"></iframe>
            <img
              v-else-if="previewMode === 'image'"
              class="preview-image"
              :src="previewSrc"
              :alt="resource?.title || '资料预览'"
              @error="handlePreviewFrameError"
            />
            <video
              v-else-if="previewMode === 'video'"
              class="preview-media"
              :src="previewSrc"
              controls
              preload="metadata"
              @error="handlePreviewFrameError"
            ></video>
            <audio v-else-if="previewMode === 'audio'" class="preview-audio" :src="previewSrc" controls preload="metadata" @error="handlePreviewFrameError"></audio>
          </div>
          <div v-else class="stage-empty">
            <el-empty :image-size="110" :description="previewError || '暂无可预览内容'" />
          </div>
        </div>
      </section>
    </main>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';
import { Back, Close, Download } from '@element-plus/icons-vue';
import { getResource, resourcePdfPreviewUrl, resourcePreviewUrl } from '@/api/infoservice/portal';
import type { InfoResource } from '@/api/infoservice/types';
import { getToken } from '@/utils/auth';
import { downloadPortalResource } from './download';
import PdfPreviewer from './components/PdfPreviewer.vue';

type PreviewMode = 'none' | 'pdf' | 'iframe' | 'image' | 'video' | 'audio';

const route = useRoute();
const router = useRouter();
const resource = ref<InfoResource>();
const previewMode = ref<PreviewMode>('none');
const previewSrc = ref('');
const previewError = ref('');
const loading = ref(false);

const resourceId = computed(() => String(route.params.resourceId || ''));
const NATIVE_IMAGE_SUFFIXES = ['jpg', 'jpeg', 'png', 'gif', 'webp', 'bmp', 'svg'];
const TEXT_SUFFIXES = ['txt', 'md', 'csv', 'tsv', 'json', 'xml', 'log', 'ini', 'yaml', 'yml'];
const VIDEO_SUFFIXES = ['mp4', 'webm', 'ogg', 'mov', 'm4v'];
const AUDIO_SUFFIXES = ['mp3', 'wav', 'ogg', 'm4a', 'aac', 'flac'];
const PDF_CONVERT_PREVIEW_TYPES = ['office'];
const PDF_CONVERT_SUFFIXES = [
  'doc',
  'docx',
  'docm',
  'dot',
  'dotx',
  'dotm',
  'xls',
  'xlsx',
  'xlsm',
  'xlt',
  'xltx',
  'xltm',
  'ppt',
  'pptx',
  'pptm',
  'pps',
  'ppsx',
  'wps',
  'wpt',
  'et',
  'ett',
  'dps',
  'dpt',
  'odt',
  'ods',
  'odp',
  'rtf',
  'csv',
  'tsv'
];

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

const withAuthQuery = (url: string) => {
  const target = new URL(url, window.location.origin);
  const token = getToken();
  if (token) {
    target.searchParams.set('Authorization', `Bearer ${token}`);
  }
  target.searchParams.set('clientid', import.meta.env.VITE_APP_CLIENT_ID);
  return target.toString();
};

const getSuffix = (item?: InfoResource) =>
  String(item?.fileSuffix || '')
    .replace(/^\./, '')
    .toLowerCase();

const getPreviewType = (item?: InfoResource) => String(item?.previewType || '').toLowerCase();

const getMimeType = (item?: InfoResource) => String(item?.mimeType || '').toLowerCase();

const isPdfResource = (item?: InfoResource) => {
  const previewType = getPreviewType(item);
  const suffix = getSuffix(item);
  const mimeType = getMimeType(item);
  return previewType === 'pdf' || mimeType.includes('pdf') || suffix === 'pdf';
};

const resolveNativePreviewMode = (item?: InfoResource): PreviewMode => {
  const previewType = getPreviewType(item);
  const suffix = getSuffix(item);
  const mimeType = getMimeType(item);
  if (previewType === 'image' || mimeType.startsWith('image/') || NATIVE_IMAGE_SUFFIXES.includes(suffix)) {
    return 'image';
  }
  if (previewType === 'pdf' || mimeType.includes('pdf') || suffix === 'pdf') {
    return 'iframe';
  }
  if (previewType === 'text' || mimeType.startsWith('text/') || TEXT_SUFFIXES.includes(suffix)) {
    return 'iframe';
  }
  if (previewType === 'video' || mimeType.startsWith('video/') || VIDEO_SUFFIXES.includes(suffix)) {
    return 'video';
  }
  if (previewType === 'audio' || mimeType.startsWith('audio/') || AUDIO_SUFFIXES.includes(suffix)) {
    return 'audio';
  }
  return 'none';
};

const shouldUsePdfPreview = (item?: InfoResource) => {
  const previewType = getPreviewType(item);
  const suffix = getSuffix(item);
  return PDF_CONVERT_PREVIEW_TYPES.includes(previewType) || PDF_CONVERT_SUFFIXES.includes(suffix);
};

const isOfdResource = (item?: InfoResource) => {
  const previewType = getPreviewType(item);
  const suffix = getSuffix(item);
  return previewType === 'ofd' || suffix === 'ofd';
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
  previewMode.value = 'none';
  previewSrc.value = '';
  try {
    const resourceRes: any = await getResource(resourceId.value);
    const currentResource = unwrapResponseData<InfoResource>(resourceRes);
    resource.value = currentResource;

    const nativeMode = resolveNativePreviewMode(currentResource);
    if (nativeMode !== 'none') {
      previewMode.value = isPdfResource(currentResource) ? 'pdf' : nativeMode;
      const previewUrl = isPdfResource(currentResource) ? resourcePdfPreviewUrl(resourceId.value) : resourcePreviewUrl(resourceId.value);
      previewSrc.value = normalizePreviewSrc(withAuthQuery(previewUrl));
      if (!previewSrc.value) {
        previewError.value = '暂未获取到预览地址';
      }
      return;
    }

    if (isOfdResource(currentResource)) {
      previewError.value = 'OFD 文件暂不支持浏览器预览，请下载后查看';
      return;
    }

    if (shouldUsePdfPreview(currentResource)) {
      previewMode.value = 'pdf';
      previewSrc.value = normalizePreviewSrc(withAuthQuery(resourcePdfPreviewUrl(resourceId.value)));
      if (!previewSrc.value) {
        previewError.value = '暂未获取到 PDF 预览地址';
      }
      return;
    }

    previewError.value = '当前文件暂不支持在线预览，可下载后查看';
  } catch (error) {
    previewMode.value = 'none';
    previewSrc.value = '';
    previewError.value = '资料暂时无法预览';
    ElMessage.error('资料暂时无法预览');
  } finally {
    if (!previewSrc.value) {
      previewMode.value = 'none';
    }
    loading.value = false;
  }
};

const handlePreviewFrameError = () => {
  previewMode.value = 'none';
  previewSrc.value = '';
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
  if (!resource.value) return;
  downloadPortalResource(resource.value);
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
  height: 100vh;
  --resource-primary: #245f8f;
  --resource-primary-soft: #eaf2f8;
  --resource-title: #14243a;
  --resource-text: #32445c;
  --resource-muted: #68788c;
  --resource-weak: #96a1af;
  --resource-border: #dce5ed;
  display: grid;
  grid-template-rows: minmax(0, 1fr);
  padding: 10px;
  background: #edf2f7;
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
  box-shadow: 0 14px 34px rgba(31, 54, 76, 0.07);
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
  border: 1px solid #d3dee8;
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
  background: #183f63;
}

.preview-shell {
  min-height: 0;
  display: grid;
  grid-template-columns: minmax(0, 1fr);
  gap: 0;
}

.preview-stage {
  min-width: 0;
  min-height: 0;
  display: grid;
  grid-template-rows: minmax(0, 1fr);
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
  display: flex;
  align-items: stretch;
  justify-content: stretch;
  background: #f2f6fc;
}

.stage-empty {
  min-height: 620px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.stage-body iframe {
  flex: 1 1 auto;
  align-self: stretch;
  width: 100%;
  height: 100%;
  min-height: 620px;
  display: block;
  border: 0;
  background: #fff;
}

.native-preview {
  width: 100%;
  min-height: 0;
  display: grid;
  grid-template-rows: auto minmax(0, 1fr);
  background: #f4f7fc;
}

.native-preview-toolbar {
  min-height: 64px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 14px;
  border-bottom: 1px solid var(--resource-border);
  padding: 10px 14px;
  background: rgba(255, 255, 255, 0.98);
}

.native-file-summary {
  min-width: 0;
  display: flex;
  align-items: center;
  gap: 12px;
}

.native-file-summary > span {
  flex: 0 0 auto;
  border-radius: 8px;
  padding: 8px 10px;
  background: var(--resource-primary-soft);
  color: var(--resource-primary);
  font-size: 12px;
  font-weight: 900;
}

.native-file-summary div {
  min-width: 0;
}

.native-file-summary strong {
  display: block;
  overflow: hidden;
  color: var(--resource-title);
  font-size: 16px;
  line-height: 1.25;
  font-weight: 900;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.native-file-summary p {
  margin: 6px 0 0;
  display: flex;
  align-items: center;
  gap: 8px;
  overflow: hidden;
  color: var(--resource-muted);
  font-size: 12px;
  font-weight: 800;
  white-space: nowrap;
}

.native-file-summary em {
  font-style: normal;
}

.native-actions {
  flex: 0 0 auto;
  display: flex;
  align-items: center;
  gap: 8px;
}

.preview-image {
  max-width: calc(100% - 48px);
  max-height: calc(100vh - 230px);
  display: block;
  border-radius: 8px;
  object-fit: contain;
  box-shadow: 0 16px 34px rgba(20, 36, 67, 0.12);
}

.preview-media {
  width: min(980px, calc(100% - 48px));
  max-height: calc(100vh - 230px);
  border-radius: 8px;
  background: #14243a;
  box-shadow: 0 16px 34px rgba(20, 36, 67, 0.12);
}

.preview-audio {
  width: min(720px, calc(100% - 48px));
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
  background: #f5f7fa;
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
    min-height: 0;
  }
}

@media (max-width: 760px) {
  .resource-preview-page {
    padding: 8px;
  }

  .native-preview-toolbar {
    align-items: stretch;
    flex-direction: column;
  }
}
</style>

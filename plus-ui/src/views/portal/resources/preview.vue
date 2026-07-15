<template>
  <div class="resource-preview-page">
    <ResourcePreviewHeader
      v-if="resource"
      :resource="resource"
      :type-label="typeLabel"
      :can-close="canCloseWindow"
      @back="goResourceList"
      @download="downloadResource"
      @close="closePreview"
    />
    <main v-loading="loading" class="preview-shell">
      <section class="preview-stage">
        <div class="stage-body">
          <PdfPreviewer
            v-if="previewMode === 'pdf' && previewSrc"
            class="pdf-preview-surface"
            :src="previewSrc"
            :resource-id="resourceId"
            :doc-title="resource?.title || resource?.originalName || ''"
            @error="handlePreviewFrameError"
            @quote="handleQuote"
          />
          <div v-else-if="previewMode !== 'none' && previewSrc" class="native-preview">
            <div class="native-preview-body">
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
              <audio
                v-else-if="previewMode === 'audio'"
                class="preview-audio"
                :src="previewSrc"
                controls
                preload="metadata"
                @error="handlePreviewFrameError"
              ></audio>
            </div>
          </div>
          <div v-else class="stage-empty">
            <div class="stage-state" role="status">
              <strong>{{ previewError ? '暂时无法预览' : '暂无可预览内容' }}</strong>
              <p>{{ previewError || '当前资料没有可用的在线预览地址。' }}</p>
              <div class="state-actions">
                <button class="secondary-button" type="button" @click="goResourceList"><IconBack />返回资料列表</button>
                <button v-if="previewError" class="secondary-button" type="button" @click="loadResource"><IconRefresh />重新加载</button>
                <button v-if="resource" class="secondary-button" type="button" @click="downloadResource"><IconDownload />下载原文件</button>
              </div>
            </div>
          </div>
        </div>
      </section>
      <ResourcePreviewWorkbench v-if="resource" ref="contextPanelRef" :resource="resource" :resource-id="resourceId" />
    </main>
  </div>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';
import IconBack from '~icons/material-symbols/arrow-back-rounded';
import IconDownload from '~icons/material-symbols/download-rounded';
import IconRefresh from '~icons/material-symbols/refresh-rounded';
import { getResource, resourcePdfPreviewUrl, resourcePreviewUrl } from '@/api/portal/resources';
import type { InfoResource } from '@/api/infoservice/types';
import { getToken } from '@/utils/auth';
import { downloadPortalResource } from './download';
import PdfPreviewer from './components/PdfPreviewer.vue';
import ResourcePreviewHeader from './components/ResourcePreviewHeader.vue';
import ResourcePreviewWorkbench from './components/ResourcePreviewWorkbench.vue';

type PreviewMode = 'none' | 'pdf' | 'iframe' | 'image' | 'video' | 'audio';

const route = useRoute();
const router = useRouter();
const resource = ref<InfoResource>();
const previewMode = ref<PreviewMode>('none');
const previewSrc = ref('');
const previewError = ref('');
const loading = ref(false);
const canCloseWindow = Boolean(window.opener);

const resourceId = computed(() => String(route.params.resourceId || ''));
const NATIVE_IMAGE_SUFFIXES = ['jpg', 'jpeg', 'png', 'gif', 'webp', 'bmp', 'svg'];
const TEXT_SUFFIXES = ['txt', 'md', 'csv', 'tsv', 'json', 'xml', 'log', 'ini', 'yaml', 'yml'];
const VIDEO_SUFFIXES = ['mp4', 'webm', 'ogg', 'mov', 'm4v'];
const AUDIO_SUFFIXES = ['mp3', 'wav', 'ogg', 'm4a', 'aac', 'flac'];
const PDF_CONVERT_PREVIEW_TYPES = ['office'];
const PDF_MAGIC_HEADER = '%PDF-';
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

let previewObjectUrl = '';

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

const releasePreviewObjectUrl = () => {
  if (!previewObjectUrl) return;
  URL.revokeObjectURL(previewObjectUrl);
  previewObjectUrl = '';
};

const readPreviewErrorMessage = async (blob: Blob) => {
  const text = (await blob.text().catch(() => '')).trim();
  if (!text) return 'PDF 预览服务暂不可用，请下载后查看';

  try {
    const body = JSON.parse(text);
    return body?.msg || body?.message || 'PDF 预览服务暂不可用，请下载后查看';
  } catch (error) {
    return text.slice(0, 120);
  }
};

const preparePdfPreviewSrc = async (url: string) => {
  releasePreviewObjectUrl();
  const response = await fetch(url, { credentials: 'include' });
  const blob = await response.blob();
  const header = await blob
    .slice(0, PDF_MAGIC_HEADER.length)
    .text()
    .catch(() => '');

  if (!response.ok || header !== PDF_MAGIC_HEADER) {
    throw new Error(await readPreviewErrorMessage(blob));
  }

  previewObjectUrl = URL.createObjectURL(new Blob([blob], { type: 'application/pdf' }));
  return previewObjectUrl;
};

const usePdfPreview = async () => {
  const previewUrl = normalizePreviewSrc(withAuthQuery(resourcePdfPreviewUrl(resourceId.value)));
  if (!previewUrl) {
    previewError.value = '暂未获取到 PDF 预览地址';
    return;
  }
  previewMode.value = 'pdf';
  previewSrc.value = await preparePdfPreviewSrc(previewUrl);
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
  return String(suffix).replace(/^\./, '').toUpperCase().slice(0, 6);
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
  releasePreviewObjectUrl();
  try {
    const resourceRes: any = await getResource(resourceId.value);
    const currentResource = unwrapResponseData<InfoResource>(resourceRes);
    resource.value = currentResource;

    const nativeMode = resolveNativePreviewMode(currentResource);
    if (nativeMode !== 'none') {
      if (isPdfResource(currentResource)) {
        await usePdfPreview();
        return;
      }
      previewMode.value = nativeMode;
      previewSrc.value = normalizePreviewSrc(withAuthQuery(resourcePreviewUrl(resourceId.value)));
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
      await usePdfPreview();
      return;
    }

    previewError.value = '当前文件暂不支持在线预览，可下载后查看';
  } catch (error) {
    previewMode.value = 'none';
    previewSrc.value = '';
    previewError.value = error instanceof Error && error.message ? error.message : '资料暂时无法预览';
    ElMessage.error(previewError.value);
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
  releasePreviewObjectUrl();
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

const contextPanelRef = ref<InstanceType<typeof ResourcePreviewWorkbench>>();

/** 阅读器划词引用 → 右栏「我的笔记」编辑器 */
const handleQuote = (text: string) => {
  contextPanelRef.value?.quoteSelection(text);
};

const downloadResource = () => {
  if (!resource.value) return;
  downloadPortalResource(resource.value);
};

onMounted(loadResource);
onBeforeUnmount(releasePreviewObjectUrl);
</script>

<style scoped>
.resource-preview-page {
  height: 100dvh;
  display: grid;
  grid-template-rows: auto minmax(0, 1fr);
  gap: 8px;
  padding: 8px;
  background: var(--ip-neutral-100);
  color: var(--ip-neutral-700);
}

.preview-stage {
  border: 1px solid var(--ip-neutral-200);
  border-radius: var(--ip-radius-md);
  background: var(--ip-neutral-0);
  box-shadow: var(--ip-shadow-sm);
}

.preview-shell {
  min-width: 0;
  min-height: 0;
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 8px;
}

.preview-stage {
  min-width: 0;
  min-height: 0;
  display: grid;
  grid-template-rows: minmax(0, 1fr);
  overflow: hidden;
}

.stage-body {
  width: 100%;
  height: 100%;
  min-height: 0;
  display: grid;
  background: var(--ip-neutral-100);
}

.pdf-preview-surface {
  width: 100%;
  min-width: 0;
  height: 100%;
}

.stage-empty {
  min-height: 0;
  display: flex;
  align-items: center;
  justify-content: center;
}

.native-preview {
  min-width: 0;
  min-height: 0;
  display: grid;
  grid-template-rows: minmax(0, 1fr);
  background: var(--ip-neutral-100);
}

.native-preview-body {
  min-height: 0;
  display: grid;
  place-items: center;
  padding: 16px;
}

.native-preview-body iframe {
  width: 100%;
  height: 100%;
  min-height: 0;
  display: block;
  border: 0;
  background: var(--ip-neutral-0);
}

.preview-image {
  max-width: 100%;
  max-height: 100%;
  display: block;
  border-radius: var(--ip-radius-md);
  object-fit: contain;
  box-shadow: var(--ip-shadow-lg);
}

.preview-media {
  width: min(980px, 100%);
  max-height: 100%;
  border-radius: var(--ip-radius-md);
  background: var(--ip-neutral-900);
  box-shadow: var(--ip-shadow-lg);
}

.preview-audio {
  width: min(720px, 100%);
}

.stage-state {
  display: grid;
  justify-items: center;
  gap: 8px;
  max-width: 520px;
  padding: 32px;
  text-align: center;
}
.stage-state strong {
  color: var(--ip-neutral-900);
  font-size: var(--ip-font-title-sm);
  font-weight: 700;
}
.stage-state p {
  margin: 0;
  color: var(--ip-neutral-500);
  font-size: var(--ip-font-body);
}
.state-actions {
  display: flex;
  justify-content: center;
  gap: 8px;
  margin-top: 8px;
  flex-wrap: wrap;
}
.secondary-button {
  min-height: 36px;
  display: inline-flex;
  align-items: center;
  gap: 6px;
  border: 1px solid var(--ip-neutral-300);
  border-radius: var(--ip-radius-sm);
  padding: 0 12px;
  background: var(--ip-neutral-0);
  color: var(--ip-neutral-700);
  font-size: var(--ip-font-caption);
  font-weight: 700;
  cursor: pointer;
}
.secondary-button:hover {
  border-color: var(--ip-primary-300);
  background: var(--ip-primary-50);
  color: var(--ip-primary-700);
}
.secondary-button svg {
  width: 18px;
  height: 18px;
}

@media (max-width: 767px) {
  .preview-shell {
    padding-bottom: 64px;
  }
  .state-actions {
    align-items: stretch;
    flex-direction: column;
    width: 100%;
  }
  .secondary-button {
    min-height: 44px;
    justify-content: center;
  }
}

@media (max-width: 767px) {
  .resource-preview-page {
    padding: 8px;
  }
}
</style>

<template>
  <article class="resource-card" @click="emit('preview', resource)">
    <div class="preview-stack" aria-hidden="true">
      <div :class="['preview-sheet', 'sheet-front', typeClass]">
        <img v-if="cachedThumbnailUrl" :src="cachedThumbnailUrl" :alt="resource.title" />
        <canvas v-else-if="pdfThumbnailUrl" v-show="pdfThumbnailReady" ref="thumbnailCanvasRef" class="thumbnail-canvas"></canvas>
        <img v-else-if="imageThumbnailUrl" :src="imageThumbnailUrl" :alt="resource.title" @error="imageThumbnailBroken = true" />
        <template v-if="showFallbackCover">
          <span class="cover-ribbon">{{ resource.categoryName || '资料共享' }}</span>
          <strong>{{ typeLabel }}</strong>
          <em>{{ coverTitle }}</em>
        </template>
      </div>
    </div>

    <div class="resource-body">
      <button class="title-button" type="button" @click.stop="emit('preview', resource)">
        {{ resource.title }}
      </button>
      <div class="resource-meta">
        <span>大小 {{ formatSize(resource.fileSize) }}</span>
        <span>浏览 {{ resource.viewCount || 0 }}次</span>
        <span>下载 {{ resource.downloadCount || 0 }}次</span>
      </div>
    </div>

    <div class="card-actions">
      <button class="action-button preview" type="button" @click.stop="emit('preview', resource)">
        <el-icon><View /></el-icon>
        <span>预览</span>
      </button>
      <button class="action-button" type="button" @click.stop="emit('download', resource)">
        <el-icon><Download /></el-icon>
        <span>下载</span>
      </button>
      <button
        :class="['action-button', 'favorite', { active: resource.favorited }]"
        type="button"
        :title="resource.favorited ? '取消收藏' : '收藏'"
        :aria-label="resource.favorited ? '取消收藏' : '收藏'"
        @click.stop="emit('favorite', resource)"
      >
        <el-icon>
          <StarFilled v-if="resource.favorited" />
          <Star v-else />
        </el-icon>
      </button>
    </div>
  </article>
</template>

<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, ref, watch } from 'vue';
import { Download, Star, StarFilled, View } from '@element-plus/icons-vue';
import { resourcePdfPreviewUrl, resourcePreviewUrl } from '@/api/portal/resources';
import type { InfoResource } from '@/api/infoservice/types';
import { getToken } from '@/utils/auth';
import {
  acquireThumbnailSlot,
  getCachedThumbnail,
  isThumbnailBlocked,
  markThumbnailFailed,
  setCachedThumbnail,
  THUMBNAIL_FETCH_TIMEOUT_MS
} from '../thumbnailCache';
import { getDocument, GlobalWorkerOptions } from 'pdfjs-dist/legacy/build/pdf.mjs';
import pdfWorkerUrl from 'pdfjs-dist/legacy/build/pdf.worker.mjs?url';

GlobalWorkerOptions.workerSrc = pdfWorkerUrl;

const props = defineProps<{
  resource: InfoResource;
}>();

const emit = defineEmits<{
  (e: 'preview', resource: InfoResource): void;
  (e: 'download', resource: InfoResource): void;
  (e: 'favorite', resource: InfoResource): void;
}>();

const IMAGE_SUFFIXES = ['jpg', 'jpeg', 'png', 'gif', 'webp', 'bmp', 'svg'];
const PDF_SUFFIXES = ['pdf'];
const OFFICE_SUFFIXES = [
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
  'csv',
  'tsv',
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
  'rtf'
];

const imageThumbnailBroken = ref(false);
const pdfThumbnailReady = ref(false);
const cachedThumbnailUrl = ref('');
const thumbnailCanvasRef = ref<HTMLCanvasElement>();
let renderToken = 0;
let loadingTask: ReturnType<typeof getDocument> | null = null;

const typeClass = computed(() => props.resource.previewType || 'file');
const normalizedPreviewType = computed(() => String(props.resource.previewType || '').toLowerCase());
const normalizedSuffix = computed(() =>
  String(props.resource.fileSuffix || '')
    .replace(/^\./, '')
    .toLowerCase()
);

const isImageResource = computed(() => {
  const mimeType = String(props.resource.mimeType || '').toLowerCase();
  return normalizedPreviewType.value === 'image' || mimeType.startsWith('image/') || IMAGE_SUFFIXES.includes(normalizedSuffix.value);
});

const canRenderPdfThumbnail = computed(() => {
  const mimeType = String(props.resource.mimeType || '').toLowerCase();
  return (
    normalizedPreviewType.value === 'pdf' ||
    normalizedPreviewType.value === 'office' ||
    mimeType.includes('pdf') ||
    PDF_SUFFIXES.includes(normalizedSuffix.value) ||
    OFFICE_SUFFIXES.includes(normalizedSuffix.value)
  );
});

const typeLabel = computed(() => {
  const suffix = props.resource.fileSuffix || props.resource.previewType || 'FILE';
  return suffix.toUpperCase().slice(0, 5);
});

const coverTitle = computed(() => {
  const title = props.resource.title || props.resource.originalName || '资料预览';
  return title.length > 9 ? `${title.slice(0, 9)}...` : title;
});

const buildAuthedUrl = (url: string) => {
  const target = new URL(url, window.location.origin);
  const token = getToken();
  if (token) {
    target.searchParams.set('Authorization', `Bearer ${token}`);
  }
  target.searchParams.set('clientid', import.meta.env.VITE_APP_CLIENT_ID);
  return target.toString();
};

const imageThumbnailUrl = computed(() => {
  if (!isImageResource.value || imageThumbnailBroken.value) {
    return '';
  }
  return buildAuthedUrl(resourcePreviewUrl(props.resource.resourceId));
});

const pdfThumbnailUrl = computed(() => {
  if (!canRenderPdfThumbnail.value || isImageResource.value) {
    return '';
  }
  return buildAuthedUrl(resourcePdfPreviewUrl(props.resource.resourceId));
});

const showFallbackCover = computed(() => !cachedThumbnailUrl.value && !imageThumbnailUrl.value && !pdfThumbnailReady.value);

const clearCanvas = () => {
  const canvas = thumbnailCanvasRef.value;
  if (!canvas) return;
  const context = canvas.getContext('2d');
  context?.clearRect(0, 0, canvas.width, canvas.height);
};

const cleanupDocument = async (doc: unknown) => {
  await (doc as { cleanup?: () => void | Promise<void> }).cleanup?.();
};

const renderPdfThumbnail = async () => {
  const url = pdfThumbnailUrl.value;
  renderToken += 1;
  const token = renderToken;
  pdfThumbnailReady.value = false;
  await loadingTask?.destroy();
  loadingTask = null;
  clearCanvas();
  if (!url) return;

  if (isThumbnailBlocked(props.resource)) return;

  await nextTick();
  const canvas = thumbnailCanvasRef.value;
  if (!canvas || token !== renderToken) return;

  const releaseSlot = await acquireThumbnailSlot();
  if (token !== renderToken) {
    releaseSlot();
    return;
  }
  let timeoutTimer: ReturnType<typeof setTimeout> | undefined;
  try {
    loadingTask = getDocument({
      url,
      withCredentials: false,
      disableAutoFetch: true
    });
    const timedTask = loadingTask;
    timeoutTimer = setTimeout(() => {
      void timedTask.destroy();
    }, THUMBNAIL_FETCH_TIMEOUT_MS);
    const doc = await loadingTask.promise;
    if (token !== renderToken) {
      await cleanupDocument(doc);
      return;
    }
    const page = await doc.getPage(1);
    const targetWidth = 224;
    const targetHeight = 284;
    const baseViewport = page.getViewport({ scale: 1 });
    const scale = Math.min(targetWidth / baseViewport.width, targetHeight / baseViewport.height);
    const viewport = page.getViewport({ scale });
    const context = canvas.getContext('2d');
    if (!context) {
      await cleanupDocument(doc);
      return;
    }

    canvas.width = targetWidth;
    canvas.height = targetHeight;
    context.fillStyle = getComputedStyle(document.documentElement).getPropertyValue('--ip-neutral-0').trim() || 'white';
    context.fillRect(0, 0, targetWidth, targetHeight);
    context.save();
    context.translate((targetWidth - viewport.width) / 2, (targetHeight - viewport.height) / 2);
    await page.render({ canvasContext: context, viewport }).promise;
    context.restore();
    if (token === renderToken) {
      pdfThumbnailReady.value = true;
      const dataUrl = canvas.toDataURL('image/jpeg', 0.72);
      setCachedThumbnail(props.resource, dataUrl);
      cachedThumbnailUrl.value = dataUrl;
    }
    await cleanupDocument(doc);
  } catch (error) {
    if (token === renderToken) {
      pdfThumbnailReady.value = false;
    }
    // 超时/网络失败进入负缓存：本会话内不再对该资源重试，避免反复占用连接
    markThumbnailFailed(props.resource);
    console.warn('Resource thumbnail render failed', error);
  } finally {
    clearTimeout(timeoutTimer);
    releaseSlot();
    if (token === renderToken) {
      loadingTask = null;
    }
  }
};

watch(
  () => [props.resource.resourceId, props.resource.previewType, props.resource.fileSuffix, props.resource.mimeType],
  () => {
    renderToken += 1;
    void loadingTask?.destroy();
    loadingTask = null;
    pdfThumbnailReady.value = false;
    clearCanvas();
    imageThumbnailBroken.value = false;
    cachedThumbnailUrl.value = getCachedThumbnail(props.resource);
    if (cachedThumbnailUrl.value) {
      return;
    }
    void renderPdfThumbnail();
  },
  { immediate: true }
);

onBeforeUnmount(() => {
  renderToken += 1;
  void loadingTask?.destroy();
  loadingTask = null;
});

const formatSize = (size?: number) => {
  if (!size) return '-';
  if (size < 1024) return `${size} B`;
  if (size < 1024 * 1024) return `${(size / 1024).toFixed(1)} KB`;
  return `${(size / 1024 / 1024).toFixed(1)} MB`;
};
</script>

<style scoped>
.resource-card {
  min-height: 184px;
  display: grid;
  grid-template-columns: 112px minmax(0, 1fr);
  grid-template-rows: minmax(0, 1fr) 32px;
  align-items: start;
  column-gap: 16px;
  row-gap: 8px;
  border: 1px solid var(--ip-neutral-200);
  border-radius: var(--ip-radius-md);
  padding: 16px;
  background: var(--ip-neutral-0);
  box-shadow: var(--ip-shadow-sm);
  cursor: pointer;
  transition:
    border-color var(--ip-motion-fast) var(--ip-motion-ease),
    box-shadow var(--ip-motion-fast) var(--ip-motion-ease),
    transform var(--ip-motion-fast) var(--ip-motion-ease);
}

.resource-card:hover {
  border-color: var(--ip-primary-200);
  box-shadow: var(--ip-shadow-md);
  transform: translateY(-1px);
}

.preview-stack {
  position: relative;
  grid-row: 1 / span 2;
  align-self: start;
  width: 112px;
  height: 142px;
  /* 纯装饰层（aria-hidden）：缩略图渲染后不得拦截与操作按钮重叠区域的点击 */
  pointer-events: none;
}

.preview-sheet {
  position: absolute;
  inset: 0;
  overflow: hidden;
  border: 1px solid var(--ip-neutral-200);
  border-radius: var(--ip-radius-sm);
  background: var(--ip-neutral-0);
  box-shadow: var(--ip-shadow-sm);
}

.preview-sheet::before {
  content: '';
  position: absolute;
  inset: 41% 0 auto;
  height: 38px;
  background: linear-gradient(135deg, #f6d34a 0%, #f4a812 46%, #f75f3a 100%);
}

.preview-sheet.office::before {
  background: linear-gradient(135deg, #ffdf56 0%, #ff9d2d 44%, #ff5c57 100%);
}

.preview-sheet.pdf::before,
.preview-sheet.ofd::before {
  background: linear-gradient(135deg, #fff1bd 0%, #ff5a4f 48%, #b51027 100%);
}

.preview-sheet.image::before {
  background: linear-gradient(135deg, #6ad6ff 0%, #2f8df5 48%, #2651d6 100%);
}

.preview-sheet.video::before,
.preview-sheet.audio::before {
  background: linear-gradient(135deg, #9ae6c5 0%, #2fb981 48%, #1680a8 100%);
}

.preview-sheet.text::before {
  background: linear-gradient(135deg, #e6edf7 0%, #aebbd0 48%, #64748b 100%);
}

.sheet-front {
  display: grid;
  grid-template-rows: 1fr auto auto 1fr;
  justify-items: center;
  color: var(--ip-neutral-900);
}

.sheet-front img,
.thumbnail-canvas {
  position: relative;
  z-index: 2;
  width: 100%;
  height: 100%;
  background: #fff;
  object-fit: cover;
}

.cover-ribbon,
.sheet-front strong,
.sheet-front em {
  position: relative;
  z-index: 1;
}

.cover-ribbon {
  grid-row: 2;
  min-width: 66px;
  max-width: 78px;
  overflow: hidden;
  border-radius: 999px;
  padding: 3px 8px;
  background: var(--ip-mod-resources-soft);
  color: var(--ip-mod-resources);
  font-size: var(--ip-font-caption);
  font-weight: 600;
  text-align: center;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.sheet-front strong {
  grid-row: 3;
  margin-top: 6px;
  color: var(--ip-neutral-900);
  font-size: var(--ip-font-emphasis);
  font-weight: 700;
  letter-spacing: 0;
}

.sheet-front em {
  max-width: 76px;
  margin-top: 5px;
  overflow: hidden;
  color: var(--ip-neutral-500);
  font-size: var(--ip-font-caption);
  font-style: normal;
  font-weight: 500;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.resource-body {
  min-width: 0;
  align-self: start;
  display: flex;
  flex-direction: column;
  justify-content: flex-start;
  padding-top: 22px;
}

.title-button {
  width: 100%;
  border: 0;
  padding: 0;
  background: transparent;
  overflow: hidden;
  color: var(--ip-neutral-900);
  font-size: var(--ip-font-emphasis);
  line-height: 1.34;
  font-weight: 600;
  letter-spacing: 0;
  text-align: left;
  display: -webkit-box;
  white-space: normal;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
  overflow-wrap: anywhere;
  cursor: pointer;
}

.title-button:hover {
  color: var(--ip-primary-600);
}

.resource-meta {
  margin-top: 10px;
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
  color: var(--ip-neutral-500);
  font-size: var(--ip-font-caption);
  font-weight: 400;
  line-height: 1;
  white-space: normal;
}

.resource-meta span {
  flex: 0 0 auto;
}

.card-actions {
  grid-column: 2;
  align-self: end;
  margin-top: 0;
  padding-top: 0;
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 8px;
  flex-wrap: nowrap;
  min-width: 0;
}

.action-button {
  height: 32px;
  min-width: 64px;
  flex: 0 0 auto;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 5px;
  border: 1px solid var(--ip-neutral-200);
  border-radius: var(--ip-radius-sm);
  padding: 0 8px;
  background: var(--ip-neutral-0);
  color: var(--ip-neutral-600);
  font-size: var(--ip-font-hint);
  font-weight: 600;
  cursor: pointer;
  transition:
    border-color 0.16s ease,
    background 0.16s ease,
    color 0.16s ease,
    box-shadow 0.16s ease;
}

.action-button.favorite {
  width: 32px;
  min-width: 32px;
  padding: 0;
}

.action-button:hover {
  border-color: var(--ip-primary-300);
  background: var(--ip-primary-50);
  color: var(--ip-primary-700);
}

.action-button.preview {
  border-color: var(--ip-primary-300);
  color: var(--ip-primary-700);
}

.action-button.favorite.active {
  border-color: var(--ip-mod-resources-border);
  background: var(--ip-mod-resources-soft);
  color: var(--ip-mod-resources);
}

.action-button.favorite.active:hover {
  border-color: var(--ip-mod-resources);
  background: var(--ip-mod-resources-soft);
  color: var(--ip-mod-resources);
}

@media (max-width: 1540px) {
  .resource-card {
    min-height: 178px;
    grid-template-columns: 104px minmax(0, 1fr);
    column-gap: 10px;
    row-gap: 8px;
    padding: 12px;
  }

  .preview-stack {
    width: 104px;
    height: 132px;
  }

  .title-button {
    font-size: 15px;
  }

  .resource-body {
    padding-top: 19px;
  }

  .resource-meta {
    gap: 8px;
    font-size: 11px;
  }
}

@media (max-width: 620px) {
  .resource-card {
    grid-template-columns: 104px minmax(0, 1fr);
    padding: 14px;
  }

  .card-actions {
    flex-wrap: nowrap;
  }

  .action-button {
    height: 44px;
  }
}

@media (prefers-reduced-motion: reduce) {
  .resource-card {
    transition: none;
  }
  .resource-card:hover {
    transform: none;
  }
}
</style>

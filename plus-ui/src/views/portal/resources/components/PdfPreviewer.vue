<template>
  <div ref="viewerRef" class="pdf-viewer">
    <header class="pdf-toolbar">
      <div class="pdf-file-summary">
        <span class="pdf-file-mark">{{ fileTypeLabel }}</span>
        <div class="pdf-file-copy">
          <strong>{{ title || fileName || '资料预览' }}</strong>
          <div class="pdf-file-meta">
            <span>{{ categoryName || '未分类' }}</span>
            <span v-if="fileSizeText">{{ fileSizeText }}</span>
            <span v-if="ownerName">{{ ownerName }}</span>
          </div>
        </div>
      </div>

      <div class="pdf-toolbar-center">
        <div class="search-box">
          <el-icon><Search /></el-icon>
          <input v-model.trim="searchKeyword" type="search" placeholder="查找正文" :disabled="!pdfReady" @keyup.enter="searchInDocument" />
          <button type="button" :disabled="!pdfReady || !searchKeyword || searchLoading" @click="searchInDocument">查找</button>
          <button type="button" :disabled="!searchResults.length" title="上一处" @click="goSearchResult(-1)">
            <el-icon><ArrowLeft /></el-icon>
          </button>
          <button type="button" :disabled="!searchResults.length" title="下一处" @click="goSearchResult(1)">
            <el-icon><ArrowRight /></el-icon>
          </button>
          <span>{{ searchStatus }}</span>
        </div>

        <div class="mode-switch" aria-label="预览模式">
          <button type="button" :class="{ active: viewMode === 'scroll' }" :disabled="!pdfReady" @click="setViewMode('scroll')">
            连续滚动
          </button>
          <button type="button" :class="{ active: viewMode === 'single' }" :disabled="!pdfReady" @click="setViewMode('single')">
            单页
          </button>
        </div>
      </div>

      <div class="pdf-actions">
        <button type="button" class="tool-button" :disabled="!canPrev" title="上一页" @click="goPrev">
          <el-icon><ArrowLeft /></el-icon>
        </button>
        <span class="page-count">{{ pageNumber }} / {{ pageCount || '-' }}</span>
        <button type="button" class="tool-button" :disabled="!canNext" title="下一页" @click="goNext">
          <el-icon><ArrowRight /></el-icon>
        </button>
        <span class="tool-separator"></span>
        <button type="button" class="tool-button" :disabled="!pdfReady" title="缩小" @click="zoomOut">
          <el-icon><ZoomOut /></el-icon>
        </button>
        <span class="zoom-value">{{ Math.round(zoom * 100) }}%</span>
        <button type="button" class="tool-button" :disabled="!pdfReady" title="放大" @click="zoomIn">
          <el-icon><ZoomIn /></el-icon>
        </button>
        <button type="button" class="fit-button" :disabled="!pdfReady" @click="fitWidth">
          <el-icon><FullScreen /></el-icon>
          适配宽度
        </button>
        <button type="button" class="tool-button" :disabled="!pdfReady" title="旋转" @click="rotateClockwise">
          <el-icon><RefreshRight /></el-icon>
        </button>
        <button type="button" class="tool-button" :class="{ active: isCurrentPageBookmarked }" :disabled="!pdfReady" title="收藏当前页" @click="toggleBookmarkCurrentPage">
          <el-icon><Star /></el-icon>
        </button>
        <button type="button" class="tool-button mark-highlight-button" :disabled="!pdfReady" title="高亮所选文字" @click="addTextMark('highlight')">
          <el-icon><Brush /></el-icon>
        </button>
        <button type="button" class="tool-button mark-underline-button" :disabled="!pdfReady" title="下划线所选文字" @click="addTextMark('underline')">
          <el-icon><CollectionTag /></el-icon>
        </button>
        <button type="button" class="tool-button mark-note-button" :disabled="!pdfReady" title="添加批注" @click="addNote">
          <el-icon><EditPen /></el-icon>
        </button>
        <button type="button" class="tool-button" :disabled="!pdfReady" title="全屏预览" @click="toggleFullscreen">
          <el-icon><Rank /></el-icon>
        </button>
        <!-- <button type="button" class="tool-button" :disabled="!pdfReady" title="打印" @click="printPdf">
          <el-icon><Printer /></el-icon>
        </button> -->
        <button type="button" class="tool-button" :disabled="loading || !src" title="重新加载" @click="reload">
          <el-icon><Refresh /></el-icon>
        </button>
        <span class="tool-separator"></span>
        <button type="button" class="fit-button" title="返回资源" @click="emit('back')">
          <el-icon><Back /></el-icon>
          返回
        </button>
        <button type="button" class="download-button" title="下载原文件" @click="emit('download')">
          <el-icon><Download /></el-icon>
          下载
        </button>
        <!-- <button type="button" class="tool-button" title="关闭" @click="emit('close')">
          <el-icon><Close /></el-icon>
        </button> -->
      </div>
    </header>

    <div class="pdf-layout" :class="{ 'sidebar-collapsed': sidebarCollapsed }">
      <aside v-show="pdfReady && !loading && !loadError" class="pdf-sidebar" :class="{ 'is-collapsed': sidebarCollapsed }">
        <div class="sidebar-head">
          <strong v-show="!sidebarCollapsed">文档导航</strong>
          <button type="button" :title="sidebarCollapsed ? '展开左侧导航' : '收起左侧导航'" @click="sidebarCollapsed = !sidebarCollapsed">
            <el-icon>
              <Expand v-if="sidebarCollapsed" />
              <Fold v-else />
            </el-icon>
          </button>
        </div>

        <div v-if="sidebarCollapsed" class="sidebar-rail" aria-label="文档导航">
          <button type="button" :class="{ active: sidebarTab === 'thumbs' }" title="预览" @click="openSidebar('thumbs')">
            <el-icon><Grid /></el-icon>
          </button>
          <button type="button" :class="{ active: sidebarTab === 'outline' }" title="目录" @click="openSidebar('outline')">
            <el-icon><Tickets /></el-icon>
          </button>
          <button type="button" :class="{ active: sidebarTab === 'bookmarks' }" title="收藏" @click="openSidebar('bookmarks')">
            <el-icon><Star /></el-icon>
          </button>
          <button type="button" :class="{ active: sidebarTab === 'annotations' }" title="标注" @click="openSidebar('annotations')">
            <el-icon><EditPen /></el-icon>
          </button>
        </div>

        <template v-else>
          <div class="sidebar-tabs" aria-label="文档导航">
            <button type="button" :class="{ active: sidebarTab === 'thumbs' }" @click="sidebarTab = 'thumbs'">
              <el-icon><Grid /></el-icon>
              预览
            </button>
            <button type="button" :class="{ active: sidebarTab === 'outline' }" @click="sidebarTab = 'outline'">
              <el-icon><Tickets /></el-icon>
              目录
            </button>
            <button type="button" :class="{ active: sidebarTab === 'bookmarks' }" @click="sidebarTab = 'bookmarks'">
              <el-icon><Star /></el-icon>
              收藏
            </button>
            <button type="button" :class="{ active: sidebarTab === 'annotations' }" @click="sidebarTab = 'annotations'">
              <el-icon><EditPen /></el-icon>
              标注
            </button>
          </div>

        <div v-show="sidebarTab === 'thumbs'" class="thumb-list">
          <button
            v-for="item in pageItems"
            :key="`thumb-${item.pageNumber}`"
            type="button"
            class="thumb-item"
            :class="{ active: item.pageNumber === pageNumber, bookmarked: pageHasBookmark(item.pageNumber) }"
            @click="jumpToPage(item.pageNumber)"
          >
            <canvas :ref="setThumbnailRef(item.pageNumber)" class="thumb-canvas"></canvas>
            <span>{{ item.pageNumber }}</span>
          </button>
        </div>

        <div v-show="sidebarTab === 'outline'" class="outline-list">
          <button
            v-for="item in outlineItems"
            :key="item.id"
            type="button"
            class="outline-item"
            :class="{ active: item.pageNumber === pageNumber, disabled: !item.pageNumber }"
            :style="{ paddingLeft: `${12 + item.level * 14}px` }"
            :title="item.title"
            :disabled="!item.pageNumber"
            @click="item.pageNumber && jumpToPage(item.pageNumber)"
          >
            <span>{{ item.title }}</span>
            <em v-if="item.pageNumber">{{ item.pageNumber }}</em>
          </button>
          <div v-if="!outlineItems.length" class="sidebar-empty">
            <el-icon><DocumentIcon /></el-icon>
            <strong>暂无目录</strong>
            <span>当前文档未包含书签目录</span>
          </div>
        </div>

        <div v-show="sidebarTab === 'bookmarks'" class="bookmark-list">
          <article v-for="page in sortedBookmarks" :key="`bookmark-${page}`" class="bookmark-item">
            <button type="button" @click="jumpToPage(page)">
              <el-icon><Star /></el-icon>
              第 {{ page }} 页
            </button>
            <button type="button" title="取消收藏" @click="removeBookmark(page)">
              <el-icon><Delete /></el-icon>
            </button>
          </article>
          <div v-if="!sortedBookmarks.length" class="sidebar-empty">
            <el-icon><Star /></el-icon>
            <strong>暂无收藏</strong>
            <span>点击顶部星标收藏当前页</span>
          </div>
        </div>

        <div v-show="sidebarTab === 'annotations'" class="annotation-list">
          <article v-for="item in sortedAnnotations" :key="item.id" class="annotation-item">
            <button type="button" class="annotation-main" @click="jumpToPage(item.pageNumber)">
              <span :class="`annotation-kind kind-${item.kind}`">{{ annotationKindLabel(item.kind) }}</span>
              <strong>第 {{ item.pageNumber }} 页</strong>
              <p>{{ item.note || item.text || '页面批注' }}</p>
            </button>
            <button type="button" class="annotation-delete" title="删除标注" @click="removeAnnotation(item.id)">
              <el-icon><Delete /></el-icon>
            </button>
          </article>
          <div v-if="!sortedAnnotations.length" class="sidebar-empty">
            <el-icon><EditPen /></el-icon>
            <strong>暂无标注</strong>
            <span>选中文字后可高亮、划线或添加批注</span>
          </div>
        </div>
        </template>
      </aside>

      <section ref="viewportRef" class="pdf-canvas-wrap" @scroll="handleViewportScroll">
        <div v-if="loading" class="pdf-message">
          <span class="loader-ring"></span>
          <strong>正在加载预览</strong>
          <p v-if="progressText">{{ progressText }}</p>
        </div>
        <div v-else-if="loadError" class="pdf-message pdf-message-error">
          <strong>{{ loadError }}</strong>
        </div>
        <div v-show="pdfReady && !loading && !loadError" class="pdf-pages" :class="`mode-${viewMode}`">
          <div v-show="viewMode === 'single'" ref="singleSurfaceRef" class="pdf-page-surface single-surface" :data-page-number="pageNumber">
            <canvas ref="singleCanvasRef" class="pdf-canvas"></canvas>
            <div ref="singleTextLayerRef" class="textLayer text-layer"></div>
            <div class="mark-layer">
              <template v-for="annotation in annotationsForPage(pageNumber)" :key="annotation.id">
                <div
                  v-for="(rect, rectIndex) in annotation.rects"
                  :key="`${annotation.id}-${rectIndex}`"
                  class="pdf-mark"
                  :class="`mark-${annotation.kind}`"
                  :style="rectStyle(rect)"
                  :title="annotation.note || annotation.text"
                ></div>
                <button
                  v-if="annotation.kind === 'note' && !annotation.rects.length"
                  type="button"
                  class="page-note-marker"
                  :title="annotation.note"
                  @click="sidebarTab = 'annotations'"
                >
                  批注
                </button>
              </template>
            </div>
            <span class="page-corner">{{ pageNumber }}</span>
          </div>

          <div v-show="viewMode === 'scroll'" class="page-stack">
            <article
              v-for="item in pageItems"
              :key="`page-${item.pageNumber}`"
              :ref="setPageFrameRef(item.pageNumber)"
              class="pdf-page-frame"
              :class="{ active: item.pageNumber === pageNumber }"
              :data-page-number="item.pageNumber"
            >
              <div :ref="setPageSurfaceRef(item.pageNumber)" class="pdf-page-surface" :data-page-number="item.pageNumber">
                <canvas :ref="setPageCanvasRef(item.pageNumber)" class="pdf-page-canvas"></canvas>
                <div :ref="setPageTextLayerRef(item.pageNumber)" class="textLayer text-layer"></div>
                <div class="mark-layer">
                  <template v-for="annotation in annotationsForPage(item.pageNumber)" :key="annotation.id">
                    <div
                      v-for="(rect, rectIndex) in annotation.rects"
                      :key="`${annotation.id}-${rectIndex}`"
                      class="pdf-mark"
                      :class="`mark-${annotation.kind}`"
                      :style="rectStyle(rect)"
                      :title="annotation.note || annotation.text"
                    ></div>
                    <button
                      v-if="annotation.kind === 'note' && !annotation.rects.length"
                      type="button"
                      class="page-note-marker"
                      :title="annotation.note"
                      @click="sidebarTab = 'annotations'"
                    >
                      批注
                    </button>
                  </template>
                </div>
                <span class="page-corner">{{ item.pageNumber }}</span>
              </div>
            </article>
          </div>
        </div>
      </section>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, ref, shallowRef, watch } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import {
  ArrowLeft,
  ArrowRight,
  Back,
  Brush,
  Close,
  CollectionTag,
  Delete,
  Document as DocumentIcon,
  Download,
  EditPen,
  Expand,
  Fold,
  FullScreen,
  Grid,
  Printer,
  Rank,
  Refresh,
  RefreshRight,
  Search,
  Star,
  Tickets,
  ZoomIn,
  ZoomOut
} from '@element-plus/icons-vue';
import { getDocument, GlobalWorkerOptions, TextLayer } from 'pdfjs-dist';
import pdfWorkerUrl from 'pdfjs-dist/build/pdf.worker.mjs?url';
import type { PDFDocumentLoadingTask, PDFDocumentProxy, PDFPageProxy, RenderTask } from 'pdfjs-dist/types/src/pdf';

GlobalWorkerOptions.workerSrc = pdfWorkerUrl;

type ViewMode = 'scroll' | 'single';
type SidebarTab = 'thumbs' | 'outline' | 'bookmarks' | 'annotations';
type AnnotationKind = 'highlight' | 'underline' | 'note';

interface PageItem {
  pageNumber: number;
}

interface OutlineItem {
  id: string;
  title: string;
  pageNumber?: number;
  level: number;
}

interface StoredRect {
  x: number;
  y: number;
  width: number;
  height: number;
}

interface PdfAnnotation {
  id: string;
  kind: AnnotationKind;
  pageNumber: number;
  text: string;
  note?: string;
  rects: StoredRect[];
  createdAt: number;
}

interface StoredViewerState {
  bookmarks?: number[];
  annotations?: PdfAnnotation[];
}

const props = defineProps<{
  src: string;
  title?: string;
  fileName?: string;
  fileSuffix?: string;
  categoryName?: string;
  fileSizeText?: string;
  ownerName?: string;
  createTime?: string;
  viewCount?: number;
  downloadCount?: number;
}>();

const emit = defineEmits<{
  error: [message: string];
  back: [];
  download: [];
  close: [];
}>();

const viewerRef = ref<HTMLElement>();
const singleCanvasRef = ref<HTMLCanvasElement>();
const singleSurfaceRef = ref<HTMLElement>();
const singleTextLayerRef = ref<HTMLElement>();
const viewportRef = ref<HTMLElement>();
const loadingTask = shallowRef<PDFDocumentLoadingTask>();
const pdfDoc = shallowRef<PDFDocumentProxy>();
const activeRenderTasks = new Set<RenderTask>();
const activeThumbnailTasks = new Set<RenderTask>();
const activeTextLayers = new Set<TextLayer>();

const loading = ref(false);
const loadError = ref('');
const progress = ref(0);
const pageNumber = ref(1);
const pageCount = ref(0);
const zoom = ref(1);
const rotation = ref(0);
const viewMode = ref<ViewMode>('scroll');
const sidebarTab = ref<SidebarTab>('thumbs');
const sidebarCollapsed = ref(false);
const outlineItems = ref<OutlineItem[]>([]);
const bookmarks = ref<number[]>([]);
const annotations = ref<PdfAnnotation[]>([]);
const searchKeyword = ref('');
const searchLoading = ref(false);
const searchResults = ref<number[]>([]);
const activeSearchIndex = ref(-1);

const pageCanvasRefs = new Map<number, HTMLCanvasElement>();
const pageFrameRefs = new Map<number, HTMLElement>();
const pageSurfaceRefs = new Map<number, HTMLElement>();
const pageTextLayerRefs = new Map<number, HTMLElement>();
const thumbnailRefs = new Map<number, HTMLCanvasElement>();
const pageTextCache = new Map<number, string>();

let renderToken = 0;
let thumbnailToken = 0;
let resizeObserver: ResizeObserver | undefined;
let resizeTimer = 0;
let scrollRaf = 0;

const pdfReady = computed(() => !!pdfDoc.value && pageCount.value > 0);
const pageItems = computed<PageItem[]>(() => Array.from({ length: pageCount.value }, (_, index) => ({ pageNumber: index + 1 })));
const sortedBookmarks = computed(() => [...bookmarks.value].sort((a, b) => a - b));
const sortedAnnotations = computed(() => [...annotations.value].sort((a, b) => a.pageNumber - b.pageNumber || a.createdAt - b.createdAt));
const canPrev = computed(() => pdfReady.value && pageNumber.value > 1);
const canNext = computed(() => pdfReady.value && pageNumber.value < pageCount.value);
const progressText = computed(() => (progress.value > 0 && progress.value < 100 ? `${progress.value}%` : ''));
const isCurrentPageBookmarked = computed(() => bookmarks.value.includes(pageNumber.value));
const searchStatus = computed(() => {
  if (searchLoading.value) return '查找中';
  if (!searchKeyword.value) return '';
  if (!searchResults.value.length) return '无结果';
  return `${activeSearchIndex.value + 1}/${searchResults.value.length}`;
});
const fileTypeLabel = computed(() => {
  const label = props.fileSuffix || 'PDF';
  return label.startsWith('.') ? label.toUpperCase() : `.${label}`.toUpperCase();
});

const clampPage = (target: number) => Math.min(Math.max(target, 1), pageCount.value || 1);

const buildStorageKey = () => {
  try {
    const target = new URL(props.src, window.location.origin);
    target.searchParams.delete('Authorization');
    target.searchParams.delete('clientid');
    return `infoservice:pdf-viewer:${target.pathname}`;
  } catch {
    return `infoservice:pdf-viewer:${props.src.split('?')[0]}`;
  }
};

const loadLocalState = () => {
  bookmarks.value = [];
  annotations.value = [];
  try {
    const raw = window.localStorage.getItem(buildStorageKey());
    if (!raw) return;
    const stored = JSON.parse(raw) as StoredViewerState;
    bookmarks.value = Array.isArray(stored.bookmarks) ? stored.bookmarks.filter((page) => Number.isInteger(page)) : [];
    annotations.value = Array.isArray(stored.annotations) ? stored.annotations.filter((item) => item?.id && item?.pageNumber) : [];
  } catch {
    bookmarks.value = [];
    annotations.value = [];
  }
};

const persistLocalState = () => {
  const payload: StoredViewerState = {
    bookmarks: bookmarks.value,
    annotations: annotations.value
  };
  window.localStorage.setItem(buildStorageKey(), JSON.stringify(payload));
};

const clearCanvas = (canvas?: HTMLCanvasElement) => {
  const ctx = canvas?.getContext('2d');
  if (!canvas || !ctx) return;
  ctx.clearRect(0, 0, canvas.width, canvas.height);
  canvas.removeAttribute('style');
};

const clearAllCanvases = () => {
  clearCanvas(singleCanvasRef.value);
  pageCanvasRefs.forEach((canvas) => clearCanvas(canvas));
  thumbnailRefs.forEach((canvas) => clearCanvas(canvas));
  singleTextLayerRef.value?.replaceChildren();
  pageTextLayerRefs.forEach((layer) => layer.replaceChildren());
};

const cancelTasks = (tasks: Set<RenderTask>) => {
  tasks.forEach((task) => {
    try {
      task.cancel();
    } catch {
      // PDF.js may throw when the render task has already settled.
    }
  });
  tasks.clear();
};

const cancelTextLayers = () => {
  activeTextLayers.forEach((layer) => {
    try {
      layer.cancel();
    } catch {
      // Ignore cancelled text layer work.
    }
  });
  activeTextLayers.clear();
};

const resetRenderWork = () => {
  renderToken += 1;
  cancelTasks(activeRenderTasks);
  cancelTextLayers();
  return renderToken;
};

const resetThumbnailWork = () => {
  thumbnailToken += 1;
  cancelTasks(activeThumbnailTasks);
  return thumbnailToken;
};

const destroyDocument = async () => {
  resetRenderWork();
  resetThumbnailWork();
  try {
    await loadingTask.value?.destroy();
  } catch {
    // Ignore teardown failures during URL switches.
  }
  try {
    await pdfDoc.value?.destroy();
  } catch {
    // Ignore teardown failures during URL switches.
  }
  loadingTask.value = undefined;
  pdfDoc.value = undefined;
};

const prepareCanvas = (canvas: HTMLCanvasElement, width: number, height: number) => {
  const pixelRatio = Math.min(window.devicePixelRatio || 1, 2);
  const context = canvas.getContext('2d');
  if (!context) throw new Error('Canvas 初始化失败');

  canvas.width = Math.floor(width * pixelRatio);
  canvas.height = Math.floor(height * pixelRatio);
  canvas.style.width = `${Math.floor(width)}px`;
  canvas.style.height = `${Math.floor(height)}px`;
  context.setTransform(1, 0, 0, 1, 0, 0);
  context.clearRect(0, 0, canvas.width, canvas.height);

  return {
    context,
    transform: pixelRatio !== 1 ? [pixelRatio, 0, 0, pixelRatio, 0, 0] : undefined
  };
};

const renderTextLayer = async (page: PDFPageProxy, layer: HTMLElement | undefined, viewport: any, token: number) => {
  if (!layer || token !== renderToken) return;
  layer.replaceChildren();
  layer.style.width = `${Math.floor(viewport.width)}px`;
  layer.style.height = `${Math.floor(viewport.height)}px`;
  try {
    const textLayer = new TextLayer({
      textContentSource: await page.getTextContent(),
      container: layer,
      viewport
    });
    activeTextLayers.add(textLayer);
    await textLayer.render();
    activeTextLayers.delete(textLayer);
  } catch (error: any) {
    if (error?.name === 'AbortException') return;
    console.warn('PDF text layer render failed', error);
  }
};

const renderPageToCanvas = async (targetPage: number, canvas: HTMLCanvasElement, textLayer: HTMLElement | undefined, availableWidth: number, token: number) => {
  const doc = pdfDoc.value;
  if (!doc || token !== renderToken) return;

  try {
    const page = await doc.getPage(targetPage);
    if (token !== renderToken) return;

    const baseViewport = page.getViewport({ scale: 1, rotation: rotation.value });
    const fitScale = availableWidth / baseViewport.width;
    const scale = Math.min(Math.max(fitScale * zoom.value, 0.3), 3.2);
    const viewport = page.getViewport({ scale, rotation: rotation.value });
    const { context, transform } = prepareCanvas(canvas, viewport.width, viewport.height);

    const task = page.render({
      canvasContext: context,
      viewport,
      transform
    });
    activeRenderTasks.add(task);
    await task.promise;
    activeRenderTasks.delete(task);
    await renderTextLayer(page, textLayer, viewport, token);
  } catch (error: any) {
    if (error?.name === 'RenderingCancelledException') return;
    console.error(error);
    loadError.value = 'PDF 预览渲染失败';
    emit('error', loadError.value);
  }
};

const renderThumbnailToCanvas = async (targetPage: number, canvas: HTMLCanvasElement, token: number) => {
  const doc = pdfDoc.value;
  if (!doc || token !== thumbnailToken) return;

  try {
    const page = await doc.getPage(targetPage);
    if (token !== thumbnailToken) return;

    const baseViewport = page.getViewport({ scale: 1, rotation: rotation.value });
    const scale = 96 / baseViewport.width;
    const viewport = page.getViewport({ scale, rotation: rotation.value });
    const { context, transform } = prepareCanvas(canvas, viewport.width, viewport.height);

    const task = page.render({
      canvasContext: context,
      viewport,
      transform
    });
    activeThumbnailTasks.add(task);
    await task.promise;
    activeThumbnailTasks.delete(task);
  } catch (error: any) {
    if (error?.name === 'RenderingCancelledException') return;
    console.warn('PDF thumbnail render failed', error);
  }
};

const renderSinglePage = async () => {
  const canvas = singleCanvasRef.value;
  const holder = viewportRef.value;
  if (!canvas || !holder || !pdfReady.value) return;

  const token = resetRenderWork();
  const availableWidth = Math.max(holder.clientWidth - 56, 360);
  await renderPageToCanvas(pageNumber.value, canvas, singleTextLayerRef.value, availableWidth, token);
};

const renderScrollPages = async () => {
  const holder = viewportRef.value;
  if (!holder || !pdfReady.value) return;

  const token = resetRenderWork();
  const availableWidth = Math.max(holder.clientWidth - 56, 360);

  for (const item of pageItems.value) {
    if (token !== renderToken) return;
    const canvas = pageCanvasRefs.get(item.pageNumber);
    if (canvas) {
      await renderPageToCanvas(item.pageNumber, canvas, pageTextLayerRefs.get(item.pageNumber), availableWidth, token);
    }
  }
};

const renderCurrentMode = async () => {
  await nextTick();
  if (viewMode.value === 'single') {
    await renderSinglePage();
    return;
  }
  const currentPage = pageNumber.value;
  await renderScrollPages();
  await nextTick();
  scrollToPage(currentPage, 'auto');
};

const renderThumbnails = async () => {
  await nextTick();
  const token = resetThumbnailWork();
  for (const item of pageItems.value) {
    if (token !== thumbnailToken) return;
    const canvas = thumbnailRefs.get(item.pageNumber);
    if (canvas) {
      await renderThumbnailToCanvas(item.pageNumber, canvas, token);
    }
  }
};

const resolveDestinationPage = async (doc: PDFDocumentProxy, dest: string | Array<any> | null | undefined) => {
  if (!dest) return undefined;
  const destination = Array.isArray(dest) ? dest : await doc.getDestination(dest);
  const pageRef = destination?.[0];
  if (typeof pageRef === 'number') {
    return pageRef + 1;
  }
  if (pageRef && typeof pageRef === 'object') {
    const pageIndex = await doc.getPageIndex(pageRef as any);
    return pageIndex + 1;
  }
  return undefined;
};

const loadOutline = async (doc: PDFDocumentProxy) => {
  outlineItems.value = [];
  try {
    const outline = await doc.getOutline();
    if (!outline?.length) return;

    const items: OutlineItem[] = [];
    let index = 0;
    const walk = async (nodes: any[], level: number) => {
      for (const node of nodes) {
        const page = await resolveDestinationPage(doc, node.dest);
        index += 1;
        items.push({
          id: `${level}-${index}-${node.title || 'outline'}`,
          title: node.title || '未命名目录',
          pageNumber: page,
          level
        });
        if (node.items?.length) {
          await walk(node.items, level + 1);
        }
      }
    };

    await walk(outline as any[], 0);
    outlineItems.value = items;
  } catch (error) {
    console.warn('PDF outline load failed', error);
  }
};

const loadDocument = async () => {
  await destroyDocument();
  clearAllCanvases();
  pageCanvasRefs.clear();
  pageFrameRefs.clear();
  pageSurfaceRefs.clear();
  pageTextLayerRefs.clear();
  thumbnailRefs.clear();
  pageTextCache.clear();
  searchResults.value = [];
  activeSearchIndex.value = -1;
  pageNumber.value = 1;
  pageCount.value = 0;
  progress.value = 0;
  loadError.value = '';
  outlineItems.value = [];
  loadLocalState();
  if (!props.src) {
    loadError.value = '暂未获取到预览地址';
    return;
  }

  loading.value = true;
  try {
    const task = getDocument({
      url: props.src,
      withCredentials: false,
      disableAutoFetch: false
    });
    loadingTask.value = task;
    task.onProgress = ({ loaded, total }) => {
      if (total > 0) {
        progress.value = Math.min(99, Math.round((loaded / total) * 100));
      }
    };
    const doc = await task.promise;
    pdfDoc.value = doc;
    pageCount.value = doc.numPages;
    progress.value = 100;
    loading.value = false;
    await nextTick();
    await renderCurrentMode();
    void renderThumbnails();
    void loadOutline(doc);
  } catch (error) {
    console.error(error);
    loadError.value = 'PDF 预览加载失败';
    emit('error', loadError.value);
  } finally {
    loading.value = false;
  }
};

const scrollToPage = (target: number, behavior: ScrollBehavior = 'smooth') => {
  if (viewMode.value !== 'scroll') return;
  const frame = pageFrameRefs.get(clampPage(target));
  frame?.scrollIntoView({ block: 'start', behavior });
};

const jumpToPage = (target: number) => {
  if (!pdfReady.value) return;
  pageNumber.value = clampPage(target);
  if (viewMode.value === 'scroll') {
    nextTick(() => scrollToPage(pageNumber.value));
  }
};

const goPrev = () => {
  if (!canPrev.value) return;
  jumpToPage(pageNumber.value - 1);
};

const goNext = () => {
  if (!canNext.value) return;
  jumpToPage(pageNumber.value + 1);
};

const zoomOut = () => {
  if (!pdfReady.value) return;
  zoom.value = Math.max(0.55, Number((zoom.value - 0.15).toFixed(2)));
};

const zoomIn = () => {
  if (!pdfReady.value) return;
  zoom.value = Math.min(2.4, Number((zoom.value + 0.15).toFixed(2)));
};

const fitWidth = () => {
  if (!pdfReady.value) return;
  if (zoom.value === 1) {
    void renderCurrentMode();
    return;
  }
  zoom.value = 1;
};

const rotateClockwise = () => {
  if (!pdfReady.value) return;
  rotation.value = (rotation.value + 90) % 360;
};

const reload = () => {
  void loadDocument();
};

const setViewMode = (mode: ViewMode) => {
  if (viewMode.value === mode || !pdfReady.value) return;
  viewMode.value = mode;
};

const openSidebar = (tab: SidebarTab) => {
  sidebarTab.value = tab;
  sidebarCollapsed.value = false;
};

const syncPageFromScroll = () => {
  const holder = viewportRef.value;
  if (!holder || viewMode.value !== 'scroll') return;

  const holderTop = holder.getBoundingClientRect().top;
  let bestPage = pageNumber.value;
  let bestDistance = Number.POSITIVE_INFINITY;
  pageFrameRefs.forEach((frame, targetPage) => {
    const distance = Math.abs(frame.getBoundingClientRect().top - holderTop - 18);
    if (distance < bestDistance) {
      bestDistance = distance;
      bestPage = targetPage;
    }
  });
  if (bestPage !== pageNumber.value) {
    pageNumber.value = bestPage;
  }
};

const handleViewportScroll = () => {
  if (viewMode.value !== 'scroll') return;
  window.cancelAnimationFrame(scrollRaf);
  scrollRaf = window.requestAnimationFrame(syncPageFromScroll);
};

const scheduleResizeRender = () => {
  window.clearTimeout(resizeTimer);
  resizeTimer = window.setTimeout(() => {
    if (pdfReady.value) {
      void renderCurrentMode();
    }
  }, 160);
};

const pageHasBookmark = (targetPage: number) => bookmarks.value.includes(targetPage);

const toggleBookmarkCurrentPage = () => {
  if (!pdfReady.value) return;
  const target = pageNumber.value;
  if (bookmarks.value.includes(target)) {
    bookmarks.value = bookmarks.value.filter((page) => page !== target);
  } else {
    bookmarks.value = [...bookmarks.value, target];
  }
  persistLocalState();
};

const removeBookmark = (targetPage: number) => {
  bookmarks.value = bookmarks.value.filter((page) => page !== targetPage);
  persistLocalState();
};

const getActiveSurfaces = () => {
  if (viewMode.value === 'single' && singleSurfaceRef.value) {
    return [{ pageNumber: pageNumber.value, element: singleSurfaceRef.value }];
  }
  return [...pageSurfaceRefs.entries()].map(([targetPage, element]) => ({ pageNumber: targetPage, element }));
};

const collectSelectionRects = () => {
  const selection = window.getSelection();
  const text = selection?.toString().trim() || '';
  if (!selection || selection.isCollapsed || !text) {
    return { text: '', pages: [] as Array<{ pageNumber: number; rects: StoredRect[] }> };
  }

  const surfaces = getActiveSurfaces();
  const pageRects = new Map<number, StoredRect[]>();
  for (let index = 0; index < selection.rangeCount; index += 1) {
    const range = selection.getRangeAt(index);
    for (const rect of Array.from(range.getClientRects())) {
      if (rect.width < 2 || rect.height < 2) continue;
      for (const surface of surfaces) {
        const surfaceRect = surface.element.getBoundingClientRect();
        const left = Math.max(rect.left, surfaceRect.left);
        const top = Math.max(rect.top, surfaceRect.top);
        const right = Math.min(rect.right, surfaceRect.right);
        const bottom = Math.min(rect.bottom, surfaceRect.bottom);
        const width = right - left;
        const height = bottom - top;
        if (width <= 2 || height <= 2) continue;
        const stored: StoredRect = {
          x: (left - surfaceRect.left) / surfaceRect.width,
          y: (top - surfaceRect.top) / surfaceRect.height,
          width: width / surfaceRect.width,
          height: height / surfaceRect.height
        };
        const current = pageRects.get(surface.pageNumber) || [];
        current.push(stored);
        pageRects.set(surface.pageNumber, current);
      }
    }
  }

  const pages = [...pageRects.entries()].map(([targetPage, rects]) => ({ pageNumber: targetPage, rects }));
  return { text, pages };
};

const createAnnotationId = () => `${Date.now().toString(36)}-${Math.random().toString(16).slice(2)}`;

const addTextMark = (kind: Exclude<AnnotationKind, 'note'>) => {
  const selected = collectSelectionRects();
  if (!selected.text || !selected.pages.length) {
    ElMessage.warning('请先在正文中选中文字');
    return;
  }

  const nextItems = selected.pages.map<PdfAnnotation>((page) => ({
    id: createAnnotationId(),
    kind,
    pageNumber: page.pageNumber,
    text: selected.text,
    rects: page.rects,
    createdAt: Date.now()
  }));
  annotations.value = [...annotations.value, ...nextItems];
  sidebarTab.value = 'annotations';
  persistLocalState();
  window.getSelection()?.removeAllRanges();
};

const addNote = async () => {
  if (!pdfReady.value) return;
  const selected = collectSelectionRects();
  try {
    const result = await ElMessageBox.prompt('请输入批注内容', '添加批注', {
      confirmButtonText: '保存',
      cancelButtonText: '取消',
      inputType: 'textarea',
      inputPlaceholder: selected.text ? '说明这段文字的重点或处理意见' : '说明当前页的重点或处理意见'
    });
    const note = result.value?.trim();
    if (!note) return;

    if (selected.text && selected.pages.length) {
      annotations.value = [
        ...annotations.value,
        ...selected.pages.map<PdfAnnotation>((page) => ({
          id: createAnnotationId(),
          kind: 'note',
          pageNumber: page.pageNumber,
          text: selected.text,
          note,
          rects: page.rects,
          createdAt: Date.now()
        }))
      ];
    } else {
      annotations.value = [
        ...annotations.value,
        {
          id: createAnnotationId(),
          kind: 'note',
          pageNumber: pageNumber.value,
          text: '',
          note,
          rects: [],
          createdAt: Date.now()
        }
      ];
    }
    sidebarTab.value = 'annotations';
    persistLocalState();
    window.getSelection()?.removeAllRanges();
  } catch {
    // User cancelled the prompt.
  }
};

const removeAnnotation = (annotationId: string) => {
  annotations.value = annotations.value.filter((item) => item.id !== annotationId);
  persistLocalState();
};

const annotationsForPage = (targetPage: number) => annotations.value.filter((item) => item.pageNumber === targetPage);

const annotationKindLabel = (kind: AnnotationKind) => {
  if (kind === 'highlight') return '高亮';
  if (kind === 'underline') return '划线';
  return '批注';
};

const rectStyle = (rect: StoredRect) => ({
  left: `${rect.x * 100}%`,
  top: `${rect.y * 100}%`,
  width: `${rect.width * 100}%`,
  height: `${rect.height * 100}%`
});

const getRenderedPageText = (targetPage: number) => {
  if (viewMode.value === 'single' && targetPage === pageNumber.value) {
    return singleTextLayerRef.value?.innerText || singleTextLayerRef.value?.textContent || '';
  }
  const layer = pageTextLayerRefs.get(targetPage);
  return layer?.innerText || layer?.textContent || '';
};

const loadPageText = async (targetPage: number) => {
  const renderedText = getRenderedPageText(targetPage);
  if (pageTextCache.has(targetPage)) {
    return `${pageTextCache.get(targetPage) || ''} ${renderedText}`;
  }
  const doc = pdfDoc.value;
  if (!doc) return renderedText;
  const page = await doc.getPage(targetPage);
  const textContent = await page.getTextContent();
  const text = `${textContent.items.map((item: any) => item.str || '').join(' ')} ${renderedText}`;
  pageTextCache.set(targetPage, text);
  return text;
};

const searchInDocument = async () => {
  const keyword = searchKeyword.value.trim();
  if (!pdfReady.value || !keyword) return;
  searchLoading.value = true;
  searchResults.value = [];
  activeSearchIndex.value = -1;
  try {
    const lowerKeyword = keyword.toLowerCase();
    const compactKeyword = lowerKeyword.replace(/\s+/g, '');
    const matches: number[] = [];
    for (let targetPage = 1; targetPage <= pageCount.value; targetPage += 1) {
      const text = (await loadPageText(targetPage)).toLowerCase();
      const compactText = text.replace(/\s+/g, '');
      if (text.includes(lowerKeyword) || (!!compactKeyword && compactText.includes(compactKeyword))) {
        matches.push(targetPage);
      }
    }
    searchResults.value = matches;
    if (matches.length) {
      activeSearchIndex.value = 0;
      jumpToPage(matches[0]);
    }
  } finally {
    searchLoading.value = false;
  }
};

const goSearchResult = (step: number) => {
  if (!searchResults.value.length) return;
  const nextIndex = (activeSearchIndex.value + step + searchResults.value.length) % searchResults.value.length;
  activeSearchIndex.value = nextIndex;
  jumpToPage(searchResults.value[nextIndex]);
};

const toggleFullscreen = async () => {
  try {
    if (document.fullscreenElement) {
      await document.exitFullscreen();
      return;
    }
    await viewerRef.value?.requestFullscreen();
  } catch {
    ElMessage.warning('当前浏览器未允许全屏');
  }
};

const printPdf = () => {
  const popup = window.open(props.src, '_blank', 'noopener,noreferrer');
  if (!popup) {
    ElMessage.warning('浏览器拦截了打印窗口');
    return;
  }
  window.setTimeout(() => {
    try {
      popup.print();
    } catch {
      // Some browsers only allow manual print from the opened PDF tab.
    }
  }, 1200);
};

const setPageCanvasRef = (targetPage: number) => (element: Element | null) => {
  if (element instanceof HTMLCanvasElement) {
    pageCanvasRefs.set(targetPage, element);
    return;
  }
  pageCanvasRefs.delete(targetPage);
};

const setPageFrameRef = (targetPage: number) => (element: Element | null) => {
  if (element instanceof HTMLElement) {
    pageFrameRefs.set(targetPage, element);
    return;
  }
  pageFrameRefs.delete(targetPage);
};

const setPageSurfaceRef = (targetPage: number) => (element: Element | null) => {
  if (element instanceof HTMLElement) {
    pageSurfaceRefs.set(targetPage, element);
    return;
  }
  pageSurfaceRefs.delete(targetPage);
};

const setPageTextLayerRef = (targetPage: number) => (element: Element | null) => {
  if (element instanceof HTMLElement) {
    pageTextLayerRefs.set(targetPage, element);
    return;
  }
  pageTextLayerRefs.delete(targetPage);
};

const setThumbnailRef = (targetPage: number) => (element: Element | null) => {
  if (element instanceof HTMLCanvasElement) {
    thumbnailRefs.set(targetPage, element);
    return;
  }
  thumbnailRefs.delete(targetPage);
};

watch(() => props.src, loadDocument, { immediate: true });

watch(pageNumber, () => {
  if (viewMode.value === 'single') {
    void renderSinglePage();
  }
});

watch([zoom, rotation], () => {
  if (pdfReady.value) {
    void renderCurrentMode();
    void renderThumbnails();
  }
});

watch(viewMode, async () => {
  if (!pdfReady.value) return;
  await renderCurrentMode();
});

onMounted(() => {
  if (viewportRef.value) {
    resizeObserver = new ResizeObserver(scheduleResizeRender);
    resizeObserver.observe(viewportRef.value);
  }
});

onBeforeUnmount(() => {
  window.clearTimeout(resizeTimer);
  window.cancelAnimationFrame(scrollRaf);
  resizeObserver?.disconnect();
  void destroyDocument();
});
</script>

<style scoped>
.pdf-viewer {
  width: 100%;
  height: 100%;
  min-height: 620px;
  display: grid;
  grid-template-rows: auto minmax(0, 1fr);
  background: #f4f7fc;
}

.pdf-viewer:fullscreen {
  padding: 0;
  background: #f4f7fc;
}

.pdf-toolbar {
  min-height: 76px;
  display: grid;
  grid-template-columns: minmax(260px, 0.78fr) minmax(330px, 0.86fr) minmax(0, 1.4fr);
  align-items: center;
  gap: 10px;
  border-bottom: 1px solid var(--resource-border, #dfe7f5);
  padding: 10px 12px;
  background: rgba(255, 255, 255, 0.98);
}

.pdf-file-summary {
  min-width: 0;
  display: flex;
  align-items: center;
  gap: 10px;
}

.pdf-file-mark {
  flex: 0 0 auto;
  min-width: 44px;
  border: 1px solid #d7e6ff;
  border-radius: 8px;
  padding: 7px 8px;
  background: var(--resource-primary-soft, #edf4ff);
  color: var(--resource-primary, #1260e8);
  font-size: 12px;
  line-height: 1;
  font-weight: 950;
  text-align: center;
}

.pdf-file-copy {
  min-width: 0;
  display: grid;
  gap: 6px;
}

.pdf-file-copy strong {
  overflow: hidden;
  color: #142447;
  font-size: 14px;
  line-height: 1.2;
  font-weight: 900;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.pdf-file-meta {
  min-width: 0;
  display: flex;
  align-items: center;
  gap: 4px;
  overflow: hidden;
  color: #68788c;
  font-size: 12px;
  font-weight: 800;
  white-space: nowrap;
}

.pdf-file-meta span {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
}

.pdf-file-meta span + span::before {
  content: '';
  display: inline-block;
  width: 3px;
  height: 3px;
  margin: 0 6px 2px 0;
  border-radius: 999px;
  background: #b6c2d1;
}

.pdf-toolbar-center {
  min-width: 0;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
}

.search-box {
  min-width: 0;
  height: 34px;
  display: inline-flex;
  align-items: center;
  gap: 6px;
  border: 1px solid #d8e3f3;
  border-radius: 8px;
  padding: 0 7px;
  background: #fff;
  color: #53668f;
}

.search-box input {
  width: 108px;
  min-width: 80px;
  border: 0;
  outline: 0;
  color: #25395f;
  font-size: 13px;
  font-weight: 780;
}

.search-box button {
  height: 24px;
  min-width: 26px;
  border: 0;
  border-radius: 6px;
  padding: 0 7px;
  background: #edf4ff;
  color: var(--resource-primary, #1260e8);
  font-size: 12px;
  font-weight: 850;
  white-space: nowrap;
  cursor: pointer;
}

.search-box button:first-of-type {
  min-width: 42px;
}

.search-box button:disabled {
  opacity: 0.45;
  cursor: default;
}

.search-box span {
  min-width: 38px;
  color: #8a97af;
  font-size: 12px;
  font-weight: 850;
  text-align: center;
}

.mode-switch {
  flex: 0 0 auto;
  display: inline-flex;
  align-items: center;
  border: 1px solid #d7e2f3;
  border-radius: 8px;
  padding: 3px;
  background: #f7faff;
}

.mode-switch button {
  height: 28px;
  border: 0;
  border-radius: 6px;
  padding: 0 9px;
  background: transparent;
  color: #53668f;
  font-size: 12px;
  font-weight: 850;
  cursor: pointer;
}

.mode-switch button.active {
  background: #fff;
  color: var(--resource-primary, #1260e8);
  box-shadow: 0 4px 12px rgba(38, 93, 170, 0.12);
}

.mode-switch button:disabled {
  opacity: 0.45;
  cursor: default;
}

.pdf-actions {
  min-width: 0;
  justify-self: end;
  display: inline-flex;
  align-items: center;
  justify-content: flex-end;
  gap: 6px;
  flex-wrap: wrap;
}

.tool-button,
.fit-button,
.download-button {
  height: 32px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border: 1px solid var(--resource-input-border, #d8e3f3);
  border-radius: 8px;
  background: #fff;
  color: var(--resource-text, #25395f);
  font-weight: 850;
  cursor: pointer;
}

.tool-button {
  width: 32px;
  padding: 0;
}

.fit-button,
.download-button {
  gap: 6px;
  padding: 0 9px;
  font-size: 12px;
}

.download-button {
  border-color: var(--resource-primary, #1260e8);
  background: var(--resource-primary, #1260e8);
  color: #fff;
}

.tool-button:hover:not(:disabled),
.fit-button:hover:not(:disabled),
.download-button:hover:not(:disabled),
.tool-button.active {
  border-color: var(--resource-primary, #1260e8);
  background: var(--resource-primary-soft, #edf4ff);
  color: var(--resource-primary, #1260e8);
}

.download-button:hover:not(:disabled) {
  background: #0f55cf;
  color: #fff;
}

.mark-highlight-button {
  color: #986a00;
}

.mark-underline-button {
  color: #1260e8;
}

.mark-note-button {
  color: #b84b12;
}

.tool-button:disabled,
.fit-button:disabled,
.download-button:disabled {
  opacity: 0.45;
  cursor: default;
}

.tool-separator {
  width: 1px;
  height: 20px;
  background: var(--resource-border, #dfe7f5);
}

.page-count,
.zoom-value {
  min-width: 66px;
  color: #31456e;
  font-size: 13px;
  font-weight: 850;
  text-align: center;
}

.zoom-value {
  min-width: 48px;
}

.pdf-layout {
  min-height: 0;
  display: grid;
  grid-template-columns: 236px minmax(0, 1fr);
  background: #f4f7fc;
  transition: grid-template-columns 0.18s ease;
}

.pdf-layout.sidebar-collapsed {
  grid-template-columns: 64px minmax(0, 1fr);
}

.pdf-sidebar {
  min-height: 0;
  border-right: 1px solid var(--resource-border, #dfe7f5);
  background: rgba(255, 255, 255, 0.86);
  display: grid;
  grid-template-columns: minmax(0, 1fr);
  grid-template-rows: auto auto minmax(0, 1fr);
}

.pdf-sidebar.is-collapsed {
  grid-template-rows: auto minmax(0, 1fr);
  overflow: hidden;
  padding: 0;
}

.sidebar-head {
  min-height: 42px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  border-bottom: 1px solid var(--resource-border, #dfe7f5);
  padding: 8px 10px;
}

.sidebar-head strong {
  overflow: hidden;
  color: #142447;
  font-size: 13px;
  font-weight: 900;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.sidebar-head button,
.sidebar-rail button {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border: 1px solid #d8e3f3;
  border-radius: 8px;
  background: #fff;
  color: #53668f;
  cursor: pointer;
}

.sidebar-head button {
  flex: 0 0 auto;
  width: 28px;
  height: 28px;
}

.pdf-sidebar.is-collapsed .sidebar-head {
  justify-content: center;
  padding: 8px 0;
}

.sidebar-head button:hover,
.sidebar-rail button:hover,
.sidebar-rail button.active {
  border-color: var(--resource-primary, #1260e8);
  background: var(--resource-primary-soft, #edf4ff);
  color: var(--resource-primary, #1260e8);
}

.sidebar-rail {
  min-height: 0;
  width: 100%;
  display: grid;
  align-content: start;
  justify-items: center;
  gap: 8px;
  padding: 10px 0;
}

.sidebar-rail button {
  width: 38px;
  height: 38px;
}

.sidebar-tabs {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 8px;
  padding: 10px;
  border-bottom: 1px solid var(--resource-border, #dfe7f5);
}

.sidebar-tabs button {
  height: 32px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 5px;
  border: 1px solid transparent;
  border-radius: 7px;
  background: transparent;
  color: #53668f;
  font-size: 12px;
  font-weight: 850;
  white-space: nowrap;
  cursor: pointer;
}

.sidebar-tabs button.active {
  border-color: #d7e6ff;
  background: var(--resource-primary-soft, #edf4ff);
  color: var(--resource-primary, #1260e8);
}

.thumb-list,
.outline-list,
.bookmark-list,
.annotation-list {
  min-height: 0;
  overflow: auto;
  padding: 12px 10px 16px;
}

.thumb-item {
  position: relative;
  width: 100%;
  display: grid;
  gap: 7px;
  justify-items: center;
  border: 1px solid transparent;
  border-radius: 8px;
  padding: 8px 6px;
  background: transparent;
  color: #53668f;
  font-size: 12px;
  font-weight: 850;
  cursor: pointer;
}

.thumb-item + .thumb-item {
  margin-top: 8px;
}

.thumb-item.active {
  border-color: #bdd5ff;
  background: #edf4ff;
  color: var(--resource-primary, #1260e8);
}

.thumb-item.bookmarked::after {
  content: '';
  position: absolute;
  top: 8px;
  right: 9px;
  width: 7px;
  height: 7px;
  border-radius: 999px;
  background: #ffb020;
}

.thumb-canvas {
  max-width: 132px;
  display: block;
  border-radius: 4px;
  background: #fff;
  box-shadow: 0 6px 18px rgba(27, 58, 112, 0.12);
}

.outline-list {
  padding: 10px 8px 16px;
}

.outline-item {
  width: 100%;
  min-height: 34px;
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  align-items: center;
  gap: 8px;
  border: 1px solid transparent;
  border-radius: 7px;
  background: transparent;
  color: #25395f;
  text-align: left;
  cursor: pointer;
}

.outline-item + .outline-item {
  margin-top: 4px;
}

.outline-item span {
  overflow: hidden;
  font-size: 12px;
  line-height: 1.35;
  font-weight: 800;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.outline-item em {
  min-width: 22px;
  color: #8a97af;
  font-size: 11px;
  font-style: normal;
  font-weight: 850;
  text-align: right;
}

.outline-item.active {
  border-color: #bdd5ff;
  background: #edf4ff;
  color: var(--resource-primary, #1260e8);
}

.outline-item.disabled {
  color: #98a6bd;
  cursor: default;
}

.bookmark-item,
.annotation-item {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 30px;
  gap: 6px;
  align-items: stretch;
}

.bookmark-item + .bookmark-item,
.annotation-item + .annotation-item {
  margin-top: 8px;
}

.bookmark-item button,
.annotation-main,
.annotation-delete {
  border: 1px solid #dfe7f5;
  border-radius: 8px;
  background: #fff;
  color: #25395f;
  cursor: pointer;
}

.bookmark-item button:first-child {
  min-height: 36px;
  display: inline-flex;
  align-items: center;
  gap: 7px;
  padding: 0 10px;
  font-size: 13px;
  font-weight: 850;
  text-align: left;
}

.bookmark-item button:last-child,
.annotation-delete {
  display: grid;
  place-items: center;
  color: #8a97af;
}

.annotation-main {
  min-width: 0;
  display: grid;
  gap: 5px;
  padding: 9px 10px;
  text-align: left;
}

.annotation-main strong {
  color: #142447;
  font-size: 13px;
  font-weight: 900;
}

.annotation-main p {
  margin: 0;
  display: -webkit-box;
  overflow: hidden;
  color: #53668f;
  font-size: 12px;
  line-height: 1.45;
  font-weight: 760;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
}

.annotation-kind {
  width: fit-content;
  border-radius: 999px;
  padding: 2px 7px;
  font-size: 11px;
  font-weight: 900;
}

.kind-highlight {
  background: #fff6d8;
  color: #986a00;
}

.kind-underline {
  background: #edf4ff;
  color: #1260e8;
}

.kind-note {
  background: #fff1e8;
  color: #b84b12;
}

.sidebar-empty {
  min-height: 280px;
  display: grid;
  place-items: center;
  align-content: center;
  gap: 8px;
  color: #8a97af;
  text-align: center;
}

.sidebar-empty .el-icon {
  font-size: 24px;
}

.sidebar-empty strong {
  color: #53668f;
  font-size: 13px;
  font-weight: 900;
}

.sidebar-empty span {
  max-width: 130px;
  font-size: 12px;
  line-height: 1.4;
  font-weight: 750;
}

.pdf-canvas-wrap {
  min-height: 0;
  overflow: auto;
  padding: 28px;
  background:
    linear-gradient(180deg, rgba(244, 247, 252, 0.78), rgba(238, 243, 250, 0.92)),
    #f4f7fc;
}

.pdf-pages {
  min-height: 100%;
}

.pdf-pages.mode-single {
  display: flex;
  align-items: flex-start;
  justify-content: center;
}

.pdf-page-surface {
  position: relative;
  display: block;
  border-radius: 4px;
  background: #fff;
  box-shadow: 0 16px 38px rgba(24, 46, 86, 0.14);
}

.pdf-canvas,
.pdf-page-canvas {
  position: relative;
  z-index: 1;
  display: block;
  border-radius: 4px;
  background: #fff;
}

.page-stack {
  width: 100%;
  display: grid;
  justify-items: center;
  gap: 22px;
}

.pdf-page-frame {
  position: relative;
  scroll-margin-top: 18px;
}

.pdf-page-frame.active .pdf-page-surface {
  box-shadow:
    0 0 0 2px rgba(18, 96, 232, 0.18),
    0 16px 38px rgba(24, 46, 86, 0.14);
}

.text-layer {
  position: absolute;
  inset: 0;
  z-index: 2;
  overflow: hidden;
  line-height: 1;
  opacity: 1;
  text-size-adjust: none;
  forced-color-adjust: none;
  transform-origin: 0 0;
}

.text-layer :deep(span),
.text-layer :deep(br) {
  position: absolute;
  color: transparent;
  white-space: pre;
  cursor: text;
  transform-origin: 0% 0%;
}

.text-layer :deep(::selection) {
  background: rgba(18, 96, 232, 0.28);
}

.mark-layer {
  position: absolute;
  inset: 0;
  z-index: 3;
  pointer-events: none;
}

.pdf-mark {
  position: absolute;
  border-radius: 2px;
  pointer-events: none;
}

.mark-highlight {
  background: rgba(255, 210, 64, 0.42);
}

.mark-underline {
  border-bottom: 2px solid rgba(18, 96, 232, 0.9);
  background: rgba(18, 96, 232, 0.08);
}

.mark-note {
  outline: 1px solid rgba(255, 129, 45, 0.8);
  background: rgba(255, 145, 54, 0.22);
}

.page-note-marker {
  position: absolute;
  top: 12px;
  right: 12px;
  border: 1px solid rgba(255, 145, 54, 0.34);
  border-radius: 999px;
  padding: 4px 9px;
  background: #fff1e8;
  color: #b84b12;
  font-size: 12px;
  font-weight: 900;
  pointer-events: auto;
  cursor: pointer;
  box-shadow: 0 6px 16px rgba(184, 75, 18, 0.14);
}

.page-corner {
  position: absolute;
  z-index: 4;
  right: 8px;
  bottom: 8px;
  border-radius: 999px;
  padding: 2px 8px;
  background: rgba(255, 255, 255, 0.92);
  color: #53668f;
  font-size: 11px;
  font-weight: 900;
  box-shadow: 0 4px 12px rgba(27, 58, 112, 0.12);
}

.pdf-message {
  min-height: 460px;
  display: grid;
  place-items: center;
  align-content: center;
  gap: 10px;
  color: var(--resource-muted, #53668f);
  text-align: center;
}

.pdf-message strong {
  color: var(--resource-text, #25395f);
  font-size: 15px;
  font-weight: 850;
}

.pdf-message p {
  margin: 0;
  font-size: 13px;
  font-weight: 750;
}

.pdf-message-error strong {
  color: #b42318;
}

.loader-ring {
  width: 34px;
  height: 34px;
  border: 3px solid #dbe8fb;
  border-top-color: var(--resource-primary, #1260e8);
  border-radius: 999px;
  animation: pdf-ring 0.75s linear infinite;
}

@keyframes pdf-ring {
  to {
    transform: rotate(360deg);
  }
}

@media (max-width: 1360px) {
  .pdf-toolbar {
    grid-template-columns: minmax(240px, 0.72fr) minmax(318px, 0.86fr) minmax(0, 1.45fr);
  }
}

@media (max-width: 1080px) {
  .pdf-toolbar {
    grid-template-columns: 1fr;
    align-items: stretch;
  }

  .pdf-toolbar-center,
  .pdf-actions {
    justify-content: flex-start;
  }
}

@media (max-width: 860px) {
  .pdf-layout {
    grid-template-columns: 1fr;
  }

  .pdf-sidebar {
    display: none;
  }

  .pdf-canvas-wrap {
    padding: 16px;
  }

  .search-box {
    flex: 1 1 auto;
  }
}
</style>

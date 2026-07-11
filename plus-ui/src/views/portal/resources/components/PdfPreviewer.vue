<template>
  <div class="pdf-viewer">
    <header class="pdf-toolbar">
      <div class="pdf-file-summary">
        <span class="pdf-file-mark">{{ fileTypeLabel }}</span>
        <div class="pdf-file-copy">
          <strong>{{ title || fileName || '资料预览' }}</strong>
          <div class="pdf-file-meta">
            <span>{{ categoryName || '未分类' }}</span>
            <span v-if="fileSizeText">{{ fileSizeText }}</span>
            <span v-if="ownerName">{{ ownerName }}</span>
            <span v-if="createTime">{{ createTime }}</span>
            <span>浏览 {{ viewCount || 0 }}</span>
            <span>下载 {{ downloadCount || 0 }}</span>
          </div>
        </div>
      </div>

      <div class="pdf-actions">
        <button type="button" class="ghost-button" title="返回资源" @click="emit('back')">
          <el-icon><Back /></el-icon>
          返回
        </button>
        <button type="button" class="primary-button" title="下载原文件" @click="emit('download')">
          <el-icon><Download /></el-icon>
          下载
        </button>
        <button type="button" class="icon-button" title="关闭" @click="emit('close')">
          <el-icon><Close /></el-icon>
        </button>
      </div>
    </header>

    <PdfWpsToolbar
      v-if="src"
      :pan-active="panActive"
      :page-mode="pageMode"
      :disabled="!controlsReady"
      @toggle-pan="togglePan"
      @set-page-mode="setPageMode"
      @rotate-left="rotateLeft"
      @rotate-right="rotateRight"
    />

    <section class="pdf-stage">
      <div v-if="!src" class="pdf-message">
        <strong>暂未获取到预览地址</strong>
        <p>请返回资料列表后重新打开。</p>
      </div>
      <template v-else>
        <PDFViewer
          ref="embedViewerRef"
          :key="viewerKey"
          class="embedpdf-surface"
          :config="embedConfig"
          :style="{ width: '100%', height: '100%' }"
          @init="handleInit"
          @ready="handleReady"
        />
        <div v-if="!viewerReady" class="pdf-loading-mask">
          <span class="loader-ring"></span>
          <strong>正在加载 PDF 查看器</strong>
        </div>
      </template>
    </section>

    <footer v-if="src" class="pdf-pagebar">
      <button type="button" class="page-nav" title="第一页" :disabled="!prevEnabled" @click="goFirstPage">
        <el-icon><DArrowLeft /></el-icon>
      </button>
      <button type="button" class="page-nav" title="上一页" :disabled="!prevEnabled" @click="goPrevPage">
        <el-icon><ArrowLeft /></el-icon>
      </button>
      <span class="page-indicator" aria-label="当前页 / 总页数">{{ pageIndicator }}</span>
      <button type="button" class="page-nav" title="下一页" :disabled="!nextEnabled" @click="goNextPage">
        <el-icon><ArrowRight /></el-icon>
      </button>
      <button type="button" class="page-nav" title="最后一页" :disabled="!nextEnabled" @click="goLastPage">
        <el-icon><DArrowRight /></el-icon>
      </button>
    </footer>
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, ref, watch } from 'vue';
import { ArrowLeft, ArrowRight, Back, Close, DArrowLeft, DArrowRight, Download } from '@element-plus/icons-vue';
import { PDFViewer } from '@embedpdf/vue-pdf-viewer';
import pdfiumWasmAssetUrl from '@embedpdf/pdfium/pdfium.wasm?url';
import type { PDFViewerConfig, PDFViewerExpose } from '@embedpdf/vue-pdf-viewer';
import PdfWpsToolbar from './PdfWpsToolbar.vue';
import { usePdfWpsViewer } from './usePdfWpsViewer';

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

const viewerReady = ref(false);
const embedViewerRef = ref<PDFViewerExpose | null>(null);
const localizationTimerIds = new Set<number>();
let localizationObserver: MutationObserver | null = null;

/** WPS 式控制条状态与动作（EmbedPDF 插件正门 API，见 usePdfWpsViewer.ts） */
const {
  panActive,
  pageMode,
  controlsReady,
  pageIndicator,
  prevEnabled,
  nextEnabled,
  connect: connectWpsControls,
  disconnect: disconnectWpsControls,
  togglePan,
  setPageMode,
  rotateLeft,
  rotateRight,
  goFirstPage,
  goPrevPage,
  goNextPage,
  goLastPage
} = usePdfWpsViewer();

const EMBEDPDF_LOCALE = 'zh-CN';

const EMBEDPDF_CHINESE_LABELS: Record<string, string> = {
  'document.menu': '文档菜单',
  'Document Menu': '文档菜单',
  'document.open': '打开文档',
  'document.close': '关闭文档',
  'document.print': '打印文档',
  'document.protect': '保护文档',
  'document.export': '导出文档',
  'document.fullscreen': '全屏显示',
  'capture.screenshot': '截图',
  'capture.title': '截图预览',
  'capture.cancel': '取消',
  'capture.download': '下载截图',
  'capture.dragTip': '拖拽选择截图区域',
  transparent: '透明',
  'menu.moreOptions': '更多选项',
  Menu: '菜单',
  'panel.sidebar': '侧边栏',
  Sidebar: '侧边栏',
  'page.settings': '页面设置',
  'page.single': '单页',
  'page.twoOdd': '双页（奇数页在左）',
  'page.twoEven': '双页（偶数页在左）',
  'page.vertical': '纵向滚动',
  'page.horizontal': '横向滚动',
  'page.spreadMode': '页面布局',
  'page.scrollLayout': '滚动方向',
  'page.rotation': '页面旋转',
  'Page Settings': '页面设置',
  Settings: '设置',
  'mode.view': '查看',
  View: '查看',
  'mode.insert': '插入',
  Insert: '插入',
  'mode.form': '表单',
  Form: '表单',
  Search: '搜索',
  'panel.comment': '批注',
  Comment: '批注',
  Comments: '批注',
  'mode.annotate': '批注',
  Annotate: '批注',
  'mode.shapes': '形状',
  Shapes: '形状',
  'mode.redact': '涂黑',
  Redact: '涂黑',
  'Fill and Sign': '填写和签名',
  'insert.rubberStamp': '图章',
  'insert.image': '插入图片',
  'signature.title': '签名',
  'form.textfield': '文本框',
  'form.checkbox': '复选框',
  'form.radio': '单选按钮',
  'form.select': '下拉选择',
  'form.listbox': '列表框',
  'form.toggleFillMode': '切换填写模式',
  'annotation.defaults': '默认批注样式',
  'annotation.styles': '批注样式',
  'annotation.strokeColor': '线条颜色',
  'annotation.fillColor': '填充色',
  'annotation.strokeWidth': '线条宽度',
  'annotation.opacity': '透明度',
  'annotation.borderStyle': '线条样式',
  'annotation.lineEndings': '线端样式',
  'annotation.fontFamily': '字体',
  'annotation.fontSize': '字号',
  'annotation.fontColor': '字色',
  'annotation.textAlign': '左右对齐',
  'annotation.verticalAlign': '上下对齐',
  'annotation.blendMode': '混合模式',
  'annotation.rotation': '旋转',
  'annotation.overlayText': '覆盖文字',
  'annotation.overlayTextPlaceholder': '例如：已遮蔽',
  'annotation.moreTools': '更多工具',
  'panel.annotationStyle': '标注样式',
  'history.undo': '撤销',
  'history.redo': '重做',
  Thumbnails: '缩略图',
  Outline: '目录',
  Bookmarks: '书签',
  Attachments: '附件',
  Page: '页码',
  'Page Number': '页码',
  'Previous Page': '上一页',
  'Next Page': '下一页',
  Previous: '上一个',
  Next: '下一个',
  'zoom.menu': '缩放菜单',
  'zoom.level': '缩放比例',
  'zoom.fitPage': '适合页面',
  'zoom.fitWidth': '适合宽度',
  'zoom.marquee': '框选放大',
  'Set zoom': '设置缩放比例',
  'Zoom Menu': '缩放菜单',
  'Zoom Level': '缩放比例',
  'zoom.in': '放大',
  'Zoom In': '放大',
  'zoom.out': '缩小',
  'Zoom Out': '缩小',
  'Zoom In Area': '区域放大',
  'Fit to Width': '适合宽度',
  'Fit to Page': '适合页面',
  Automatic: '自动缩放',
  'pan.toggle': '切换平移模式',
  'Toggle Pan Mode': '切换平移模式',
  'pointer.toggle': '切换选择模式',
  'Toggle Pointer Mode': '切换选择模式',
  'rotate.clockwise': '顺时针旋转',
  'rotate.counterClockwise': '逆时针旋转',
  Pan: '平移',
  Pointer: '选择',
  Select: '选择',
  'Rotate Clockwise': '顺时针旋转',
  'Rotate Counter-Clockwise': '逆时针旋转',
  'Enter Full Screen': '进入全屏',
  'Exit Full Screen': '退出全屏',
  'Open PDF': '打开 PDF',
  Download: '下载',
  'Print Document': '打印文档',
  Print: '打印',
  Save: '保存',
  Undo: '撤销',
  Redo: '重做',
  Copy: '复制',
  Screenshot: '截图',
  Close: '关闭',
  'Search document': '搜索文档',
  'search.caseSensitive': '区分大小写',
  'search.wholeWord': '全词匹配',
  'No results': '未找到结果',
  'comments.emptyState': '暂无批注'
};

const fileTypeLabel = computed(() => {
  const label = props.fileSuffix || 'PDF';
  return label.startsWith('.') ? label.toUpperCase() : `.${label}`.toUpperCase();
});

const wasmUrl = computed(() => {
  if (typeof window === 'undefined') return pdfiumWasmAssetUrl;
  return new URL(pdfiumWasmAssetUrl, window.location.origin).toString();
});

const viewerKey = computed(() => `${props.src || 'empty'}:${wasmUrl.value}`);

const embedConfig = computed<PDFViewerConfig>(() => ({
  src: props.src,
  wasmUrl: wasmUrl.value,
  worker: true,
  tabBar: 'never',
  i18n: {
    defaultLocale: EMBEDPDF_LOCALE,
    fallbackLocale: EMBEDPDF_LOCALE
  },
  fontFallback: null,
  fonts: {
    ui: null,
    signature: null
  },
  disabledCategories: ['annotation', 'redaction', 'signature'],
  stamp: {
    defaultLibrary: false,
    manifests: []
  },
  theme: {
    preference: 'light',
    light: {
      accent: {
        primary: '#245f8f'
      }
    }
  }
}));

watch(
  () => props.src,
  () => {
    viewerReady.value = false;
    stopEmbedPdfLocalization();
    disconnectWpsControls();
  }
);

const handleInit = () => {
  viewerReady.value = false;
};

const handleReady = () => {
  viewerReady.value = true;
  scheduleEmbedPdfLocalization();
  void connectWpsControls(embedViewerRef.value);
};

const translatePlainText = (value: string | null | undefined) => {
  const trimmed = value?.trim();
  if (!trimmed) return undefined;
  return EMBEDPDF_CHINESE_LABELS[trimmed];
};

const updateTranslatedAttribute = (element: Element, attributeName: string) => {
  const translated = translatePlainText(element.getAttribute(attributeName));
  if (translated) element.setAttribute(attributeName, translated);
};

const localizeControlText = (element: Element) => {
  const walker = document.createTreeWalker(element, NodeFilter.SHOW_TEXT);
  const textNodes: Text[] = [];
  let currentNode = walker.nextNode();

  while (currentNode) {
    textNodes.push(currentNode as Text);
    currentNode = walker.nextNode();
  }

  textNodes.forEach((node) => {
    const translated = translatePlainText(node.textContent);
    if (!translated) return;

    node.textContent = node.textContent?.replace(node.textContent.trim(), translated) ?? translated;
  });
};

const localizeKnownTextNodes = (root: DocumentFragment) => {
  const walker = document.createTreeWalker(root, NodeFilter.SHOW_TEXT, {
    acceptNode: (node) => (node.parentElement?.closest('style, script') ? NodeFilter.FILTER_REJECT : NodeFilter.FILTER_ACCEPT)
  });
  let currentNode = walker.nextNode();

  while (currentNode) {
    const translated = translatePlainText(currentNode.textContent);

    if (translated) {
      currentNode.textContent = currentNode.textContent?.replace(currentNode.textContent.trim(), translated) ?? translated;
    }

    currentNode = walker.nextNode();
  }
};

const localizeEmbedPdf = () => {
  const shadowRoot = embedViewerRef.value?.container?.shadowRoot;
  if (!shadowRoot) return;

  shadowRoot.querySelectorAll('[aria-label], [title], [placeholder]').forEach((element) => {
    updateTranslatedAttribute(element, 'aria-label');
    updateTranslatedAttribute(element, 'title');
    updateTranslatedAttribute(element, 'placeholder');
  });

  shadowRoot.querySelectorAll('button, [role="tab"], [role="button"], [role="menuitem"], [role="option"], label').forEach(localizeControlText);
  localizeKnownTextNodes(shadowRoot);
};

const observeEmbedPdfShadow = () => {
  const shadowRoot = embedViewerRef.value?.container?.shadowRoot;
  if (!shadowRoot || localizationObserver) return;

  localizationObserver = new MutationObserver(() => {
    localizeEmbedPdf();
  });
  localizationObserver.observe(shadowRoot, {
    childList: true,
    subtree: true,
    characterData: true,
    attributes: true,
    attributeFilter: ['aria-label', 'title', 'placeholder']
  });
};

const scheduleEmbedPdfLocalization = () => {
  if (typeof window === 'undefined') return;

  const runLocalization = () => {
    void nextTick(() => {
      localizeEmbedPdf();
      observeEmbedPdfShadow();
    });
  };

  runLocalization();
  [100, 400, 1200].forEach((delay) => {
    const timerId = window.setTimeout(runLocalization, delay);
    localizationTimerIds.add(timerId);
  });
};

const stopEmbedPdfLocalization = () => {
  localizationTimerIds.forEach((timerId) => window.clearTimeout(timerId));
  localizationTimerIds.clear();
  localizationObserver?.disconnect();
  localizationObserver = null;
};

onBeforeUnmount(() => {
  stopEmbedPdfLocalization();
  disconnectWpsControls();
});
</script>

<style scoped>
.pdf-viewer {
  display: flex;
  flex-direction: column;
  min-height: 0;
  height: 100%;
  overflow: hidden;
  background: #f5f7fb;
  color: #1f2a3d;
}

.pdf-toolbar {
  position: relative;
  z-index: 2;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  min-height: 72px;
  padding: 12px 18px;
  border-bottom: 1px solid rgba(148, 163, 184, 0.24);
  background: rgba(255, 255, 255, 0.96);
  box-shadow: 0 8px 24px rgba(15, 23, 42, 0.06);
}

.pdf-file-summary {
  display: flex;
  align-items: center;
  min-width: 0;
  gap: 12px;
}

.pdf-file-mark {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 54px;
  height: 34px;
  padding: 0 10px;
  border-radius: 7px;
  background: #e9f2fb;
  color: #245f8f;
  font-size: 13px;
  font-weight: 700;
  letter-spacing: 0;
}

.pdf-file-copy {
  display: grid;
  min-width: 0;
  gap: 5px;
}

.pdf-file-copy strong {
  overflow: hidden;
  color: #142133;
  font-size: 16px;
  line-height: 1.35;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.pdf-file-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 7px 12px;
  color: #667085;
  font-size: 12px;
  line-height: 1.4;
}

.pdf-file-meta span {
  max-width: 180px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.pdf-actions {
  display: inline-flex;
  align-items: center;
  flex-shrink: 0;
  gap: 8px;
}

.pdf-actions button {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  height: 36px;
  border: 0;
  border-radius: 7px;
  font-size: 14px;
  font-weight: 600;
  line-height: 1;
  cursor: pointer;
  transition:
    transform 0.16s ease,
    box-shadow 0.16s ease,
    background 0.16s ease;
}

.pdf-actions button:active {
  transform: translateY(1px);
}

.ghost-button {
  gap: 6px;
  padding: 0 13px;
  background: #edf3f8;
  color: #245f8f;
}

.ghost-button:hover {
  background: #dbe9f5;
}

.primary-button {
  gap: 6px;
  padding: 0 14px;
  background: #245f8f;
  color: #ffffff;
  box-shadow: 0 10px 24px rgba(36, 95, 143, 0.22);
}

.primary-button:hover {
  background: #1e527d;
}

.icon-button {
  width: 36px;
  padding: 0;
  background: #f0f3f7;
  color: #475467;
}

.icon-button:hover {
  background: #e3e8ef;
  color: #1f2a3d;
}

.pdf-stage {
  position: relative;
  flex: 1;
  min-height: 0;
  overflow: hidden;
  background: #dbe2ec;
}

.pdf-pagebar {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 6px 12px;
  border-top: 1px solid var(--ip-neutral-100);
  background: var(--ip-neutral-50);
}

.page-nav {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 26px;
  height: 26px;
  border: 0;
  border-radius: var(--ip-radius-sm);
  background: transparent;
  color: var(--ip-neutral-500);
  cursor: pointer;
  transition:
    background var(--ip-motion-fast) var(--ip-motion-ease),
    color var(--ip-motion-fast) var(--ip-motion-ease);
}

.page-nav:hover:not(:disabled) {
  background: var(--ip-primary-50);
  color: var(--ip-primary-600);
}

.page-nav:disabled {
  opacity: 0.5;
  cursor: default;
}

.page-indicator {
  border: 1px solid var(--ip-neutral-200);
  border-radius: var(--ip-radius-sm);
  padding: 3px 10px;
  background: var(--ip-neutral-0);
  color: var(--ip-neutral-700);
  font-size: var(--ip-font-caption);
  line-height: 1.4;
}

.embedpdf-surface {
  display: block;
  width: 100%;
  height: 100%;
}

.pdf-loading-mask,
.pdf-message {
  position: absolute;
  inset: 0;
  z-index: 3;
  display: grid;
  place-content: center;
  justify-items: center;
  gap: 12px;
  padding: 32px;
  background: rgba(245, 247, 251, 0.92);
  color: #344054;
  text-align: center;
}

.pdf-message {
  position: relative;
  height: 100%;
  background: #f5f7fb;
}

.pdf-message strong,
.pdf-loading-mask strong {
  color: #1f2a3d;
  font-size: 16px;
}

.pdf-message p {
  margin: 0;
  color: #667085;
}

.loader-ring {
  width: 28px;
  height: 28px;
  border: 3px solid rgba(36, 95, 143, 0.18);
  border-top-color: #245f8f;
  border-radius: 50%;
  animation: pdf-spin 0.82s linear infinite;
}

@keyframes pdf-spin {
  to {
    transform: rotate(360deg);
  }
}

@media (max-width: 900px) {
  .pdf-toolbar {
    align-items: flex-start;
    flex-direction: column;
  }

  .pdf-actions {
    width: 100%;
    justify-content: flex-end;
  }
}
</style>

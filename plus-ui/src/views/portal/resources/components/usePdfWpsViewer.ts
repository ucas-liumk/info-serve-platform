/**
 * EmbedPDF WPS 式控制组合函数：经 PDFViewerExpose.registry（Promise<PluginRegistry>）
 * 走插件正门 API 驱动拖拽/页模式/旋转/翻页，禁止 shadow DOM 操作（spec §5.6）。
 *
 * 插件 id 与能力接口均从 @embedpdf/plugin-{pan,spread,rotate,scroll,zoom} 的 d.ts 现场核实：
 * - pan    ：PanCapability.togglePan / isPanMode / onPanModeChange({ isPanMode })
 * - spread ：SpreadCapability.setSpreadMode(SpreadMode) / getSpreadMode / onSpreadChange({ spreadMode })
 * - rotate ：RotateCapability.rotateForward(顺时针) / rotateBackward(逆时针)
 * - scroll ：ScrollCapability.getCurrentPage / getTotalPages / scrollToPage({ pageNumber })
 *            / scrollToNextPage / scrollToPreviousPage / onPageChange / onLayoutReady
 * - zoom   ：ZoomCapability.requestZoom(ZoomMode.FitPage)（"单页"就近映射用）
 */
import { computed, readonly, ref } from 'vue';
import { SpreadMode, ZoomMode } from '@embedpdf/vue-pdf-viewer';
import type {
  PanCapability,
  PDFViewerExpose,
  PluginRegistry,
  RotateCapability,
  ScrollCapability,
  SelectionCapability,
  SpreadCapability,
  UICapability,
  ZoomCapability
} from '@embedpdf/vue-pdf-viewer';
import {
  canGoNext,
  canGoPrev,
  clampPageNumber,
  formatPageIndicator,
  keyOfSpreadValue,
  reconcilePageMode,
  resolvePageModeIntent
} from './pdfWpsControls';
import type { PageViewMode, SpreadModeKey } from './pdfWpsControls';

/** 与 SpreadMode 枚举的显式映射（枚举值即字符串，仍显式映射以防上游改值） */
const SPREAD_MODE_BY_KEY: Readonly<Record<SpreadModeKey, SpreadMode>> = Object.freeze({
  none: SpreadMode.None,
  odd: SpreadMode.Odd,
  even: SpreadMode.Even
});

/** getPlugin 泛型的最小结构约束（IPlugin 未从 @embedpdf/vue-pdf-viewer 再导出，结构等价） */
interface CapabilityProvider<T> {
  id: string;
  provides(): Readonly<T>;
}

interface WpsCapabilities {
  readonly pan: Readonly<PanCapability> | null;
  readonly spread: Readonly<SpreadCapability> | null;
  readonly rotate: Readonly<RotateCapability> | null;
  readonly scroll: Readonly<ScrollCapability> | null;
  readonly zoom: Readonly<ZoomCapability> | null;
  readonly selection: Readonly<SelectionCapability> | null;
  readonly ui: Readonly<UICapability> | null;
}

const getCapability = <T>(registry: PluginRegistry, pluginId: string): Readonly<T> | null => {
  try {
    const plugin = registry.getPlugin<CapabilityProvider<T>>(pluginId);
    if (!plugin) {
      console.warn(`[PdfPreviewer] EmbedPDF 插件 ${pluginId} 未注册，相关按钮不可用`);
      return null;
    }
    return plugin.provides();
  } catch (error) {
    console.error(`[PdfPreviewer] 获取 EmbedPDF 插件 ${pluginId} 能力失败`, error);
    return null;
  }
};

const resolveCapabilities = (registry: PluginRegistry): WpsCapabilities =>
  Object.freeze({
    pan: getCapability<PanCapability>(registry, 'pan'),
    spread: getCapability<SpreadCapability>(registry, 'spread'),
    rotate: getCapability<RotateCapability>(registry, 'rotate'),
    scroll: getCapability<ScrollCapability>(registry, 'scroll'),
    zoom: getCapability<ZoomCapability>(registry, 'zoom'),
    selection: getCapability<SelectionCapability>(registry, 'selection'),
    ui: getCapability<UICapability>(registry, 'ui')
  });

export const usePdfWpsViewer = () => {
  const panActive = ref(false);
  const pageMode = ref<PageViewMode>('continuous');
  const currentPage = ref(0);
  const totalPages = ref(0);
  const controlsReady = ref(false);

  let capabilities: WpsCapabilities | null = null;
  let teardown: (() => void) | null = null;
  let wireToken = 0;

  const subscribe = (caps: WpsCapabilities): (() => void) => {
    const unsubscribers = [
      caps.pan?.onPanModeChange((event) => {
        panActive.value = event.isPanMode;
      }),
      caps.spread?.onSpreadChange((event) => {
        // 用户也可能从内建"页面设置"菜单改布局，回流对齐工具条 active 态
        pageMode.value = reconcilePageMode(pageMode.value, keyOfSpreadValue(event.spreadMode));
      }),
      caps.scroll?.onPageChange((event) => {
        currentPage.value = event.pageNumber;
        totalPages.value = event.totalPages;
      }),
      caps.scroll?.onLayoutReady((event) => {
        currentPage.value = event.pageNumber;
        totalPages.value = event.totalPages;
      })
    ].filter((unsubscribe): unsubscribe is () => void => typeof unsubscribe === 'function');
    return () => unsubscribers.forEach((unsubscribe) => unsubscribe());
  };

  const syncInitialState = (caps: WpsCapabilities): void => {
    // 各读取器在文档尚未激活时会抛错：属预期时序（onLayoutReady 到达后自然补齐），静默保持默认值
    try {
      panActive.value = caps.pan?.isPanMode() ?? false;
    } catch {
      panActive.value = false;
    }
    try {
      const spread = caps.spread?.getSpreadMode();
      if (spread !== undefined) {
        pageMode.value = reconcilePageMode(pageMode.value, keyOfSpreadValue(spread));
      }
    } catch {
      pageMode.value = 'continuous';
    }
    try {
      const total = caps.scroll?.getTotalPages() ?? 0;
      if (total > 0) {
        totalPages.value = total;
        currentPage.value = clampPageNumber(caps.scroll?.getCurrentPage() ?? 1, total);
      }
    } catch {
      currentPage.value = 0;
      totalPages.value = 0;
    }
  };

  /** src 切换 / 组件卸载时清订阅、回默认态 */
  const disconnect = (): void => {
    wireToken += 1;
    teardown?.();
    teardown = null;
    capabilities = null;
    controlsReady.value = false;
    panActive.value = false;
    pageMode.value = 'continuous';
    currentPage.value = 0;
    totalPages.value = 0;
  };

  /** ready 事件后调用：await registry Promise，带令牌防 src 切换后的迟到回调 */
  const connect = async (expose: PDFViewerExpose | null): Promise<void> => {
    disconnect();
    const registryPromise = expose?.registry;
    if (!registryPromise) {
      console.warn('[PdfPreviewer] EmbedPDF 未暴露 registry，WPS 工具条不可用');
      return;
    }
    const token = wireToken;
    let registry: PluginRegistry;
    try {
      registry = await registryPromise;
    } catch (error) {
      console.error('[PdfPreviewer] 等待 EmbedPDF PluginRegistry 失败，WPS 工具条不可用', error);
      return;
    }
    if (token !== wireToken || registry.isDestroyed()) return;
    capabilities = resolveCapabilities(registry);
    teardown = subscribe(capabilities);
    syncInitialState(capabilities);
    // 浮动页码气泡（page-controls overlay）与底部翻页条重复，走官方 UI 能力关停
    safeInvoke('ui', () => capabilities?.ui?.disableOverlay('page-controls'));
    controlsReady.value = Boolean(capabilities.pan || capabilities.spread || capabilities.rotate || capabilities.scroll);
  };

  const safeInvoke = (feature: string, action: () => void): void => {
    try {
      action();
    } catch (error) {
      console.error(`[PdfPreviewer] EmbedPDF ${feature} 操作失败`, error);
    }
  };

  const withCapability = <K extends keyof WpsCapabilities>(key: K, run: (cap: NonNullable<WpsCapabilities[K]>) => void): void => {
    const cap = capabilities?.[key];
    if (!cap) {
      console.warn(`[PdfPreviewer] EmbedPDF ${key} 能力不可用，操作被忽略`);
      return;
    }
    safeInvoke(key, () => run(cap));
  };

  /** 拖拽独立 toggle：开 = 拖拽平移，再点恢复选择模式 */
  const togglePan = (): void => withCapability('pan', (pan) => pan.togglePan());

  /** 三个页模式互斥；"单页" = spread none + fitPage 的就近语义映射（详见 pdfWpsControls.ts 头注） */
  const setPageMode = (mode: PageViewMode): void =>
    withCapability('spread', (spread) => {
      const intent = resolvePageModeIntent(mode);
      spread.setSpreadMode(SPREAD_MODE_BY_KEY[intent.spread]);
      if (intent.fitPage) {
        withCapability('zoom', (zoom) => zoom.requestZoom(ZoomMode.FitPage));
      }
      pageMode.value = mode;
    });

  const rotateLeft = (): void => withCapability('rotate', (rotate) => rotate.rotateBackward());
  const rotateRight = (): void => withCapability('rotate', (rotate) => rotate.rotateForward());

  /** 读取当前划词选区文本（按行返回；无选区/能力缺失返回空数组） */
  const getSelectedLines = async (): Promise<string[]> => {
    const selection = capabilities?.selection;
    if (!selection) return [];
    try {
      return await selection.getSelectedText().toPromise();
    } catch {
      return [];
    }
  };

  const goToPage = (pageNumber: number): void =>
    withCapability('scroll', (scroll) => scroll.scrollToPage({ pageNumber: clampPageNumber(pageNumber, totalPages.value) }));
  const goFirstPage = (): void => goToPage(1);
  const goLastPage = (): void => goToPage(totalPages.value);
  const goPrevPage = (): void => withCapability('scroll', (scroll) => scroll.scrollToPreviousPage());
  const goNextPage = (): void => withCapability('scroll', (scroll) => scroll.scrollToNextPage());

  const pageIndicator = computed(() => formatPageIndicator(currentPage.value, totalPages.value));
  const prevEnabled = computed(() => controlsReady.value && canGoPrev(currentPage.value));
  const nextEnabled = computed(() => controlsReady.value && canGoNext(currentPage.value, totalPages.value));

  return {
    panActive: readonly(panActive),
    pageMode: readonly(pageMode),
    controlsReady: readonly(controlsReady),
    currentPage: readonly(currentPage),
    totalPages: readonly(totalPages),
    pageIndicator,
    prevEnabled,
    nextEnabled,
    connect,
    disconnect,
    togglePan,
    setPageMode,
    rotateLeft,
    rotateRight,
    getSelectedLines,
    goToPage,
    goFirstPage,
    goPrevPage,
    goNextPage,
    goLastPage
  };
};

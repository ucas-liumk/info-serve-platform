/**
 * WPS 式阅读器控制条纯函数（PdfPreviewer / PdfWpsToolbar 共用，vitest 全覆盖）。
 *
 * 页面视图三模式与 EmbedPDF spread 插件的就近映射（spec §5.6）：
 * - continuous 连页   = spread 'none'（默认纵向连续滚动）
 * - single     单页   = spread 'none' + fit-page 缩放（EmbedPDF 无逐页翻页模式，
 *                        用"单页充满视口"作最近语义映射）
 * - double     双页连续 = spread 'odd'（奇数页在左的双页拼排）
 */

export type PageViewMode = 'continuous' | 'single' | 'double';

/** 与 @embedpdf/plugin-spread SpreadMode 枚举值一一对应的字符串键 */
export type SpreadModeKey = 'none' | 'odd' | 'even';

export interface PageModeIntent {
  readonly spread: SpreadModeKey;
  readonly fitPage: boolean;
}

const PAGE_MODE_INTENTS: Readonly<Record<PageViewMode, PageModeIntent>> = Object.freeze({
  continuous: Object.freeze({ spread: 'none', fitPage: false }),
  single: Object.freeze({ spread: 'none', fitPage: true }),
  double: Object.freeze({ spread: 'odd', fitPage: false })
});

/** 三个页模式互斥；返回按钮点击后应下发给插件的意图（spread 模式 + 是否 fitPage） */
export const resolvePageModeIntent = (mode: PageViewMode): PageModeIntent => PAGE_MODE_INTENTS[mode];

/**
 * 由 spread 插件回报的模式反推工具条 active 态（用户也可能从内建"页面设置"菜单改布局）。
 * spread 'none' 同时对应连页/单页两个按钮：若当前已是 single 或 continuous 则保持不动，
 * 否则回落 continuous（默认模式）。
 */
export const reconcilePageMode = (current: PageViewMode, spread: SpreadModeKey): PageViewMode => {
  if (spread === 'odd' || spread === 'even') return 'double';
  return current === 'double' ? 'continuous' : current;
};

/** 把插件事件里的 SpreadMode 值规整为本模块的字符串键（未知值按 none 兜底） */
export const keyOfSpreadValue = (value: unknown): SpreadModeKey => {
  if (value === 'odd' || value === 'even') return value;
  return 'none';
};

/** 页码钳制到 [1, totalPages]；文档未加载（totalPages<1）或页码非法时回 1 */
export const clampPageNumber = (page: number, totalPages: number): number => {
  if (!Number.isFinite(totalPages) || totalPages < 1) return 1;
  if (Number.isNaN(page)) return 1;
  return Math.min(Math.max(Math.floor(page), 1), Math.floor(totalPages));
};

/** 底部翻页条"当前页 / 总页"展示文案；未加载时显示占位 */
export const formatPageIndicator = (currentPage: number, totalPages: number): string => {
  if (!Number.isFinite(totalPages) || totalPages < 1) return '- / -';
  return `${clampPageNumber(currentPage, totalPages)} / ${Math.floor(totalPages)}`;
};

export const canGoPrev = (currentPage: number): boolean => Number.isFinite(currentPage) && currentPage > 1;

export const canGoNext = (currentPage: number, totalPages: number): boolean =>
  Number.isFinite(currentPage) && Number.isFinite(totalPages) && currentPage >= 1 && currentPage < totalPages;

import { describe, expect, it } from 'vitest';
import {
  buildQuoteText,
  canGoNext,
  canGoPrev,
  clampPageNumber,
  formatPageIndicator,
  keyOfSpreadValue,
  normalizeReaderTheme,
  readingProgressKey,
  reconcilePageMode,
  resolvePageModeIntent,
  resolveRestorePage
} from './pdfWpsControls';

describe('resolvePageModeIntent', () => {
  it('maps continuous mode to spread none without zoom adjustment', () => {
    expect(resolvePageModeIntent('continuous')).toEqual({ spread: 'none', fitPage: false });
  });

  it('maps single mode to spread none plus fit-page zoom (nearest semantic)', () => {
    expect(resolvePageModeIntent('single')).toEqual({ spread: 'none', fitPage: true });
  });

  it('maps double mode to spread odd', () => {
    expect(resolvePageModeIntent('double')).toEqual({ spread: 'odd', fitPage: false });
  });
});

describe('reconcilePageMode', () => {
  it('maps odd/even spread reported by the viewer to double mode', () => {
    expect(reconcilePageMode('continuous', 'odd')).toBe('double');
    expect(reconcilePageMode('single', 'even')).toBe('double');
  });

  it('keeps single mode when spread none arrives while single is active', () => {
    expect(reconcilePageMode('single', 'none')).toBe('single');
  });

  it('keeps continuous mode when spread none arrives while continuous is active', () => {
    expect(reconcilePageMode('continuous', 'none')).toBe('continuous');
  });

  it('falls back to continuous when spread none arrives while double is active', () => {
    expect(reconcilePageMode('double', 'none')).toBe('continuous');
  });
});

describe('keyOfSpreadValue', () => {
  it('normalizes known spread values', () => {
    expect(keyOfSpreadValue('none')).toBe('none');
    expect(keyOfSpreadValue('odd')).toBe('odd');
    expect(keyOfSpreadValue('even')).toBe('even');
  });

  it('falls back to none for unknown values', () => {
    expect(keyOfSpreadValue('anything-else')).toBe('none');
    expect(keyOfSpreadValue(undefined)).toBe('none');
  });
});

describe('clampPageNumber', () => {
  it('keeps in-range pages untouched', () => {
    expect(clampPageNumber(3, 10)).toBe(3);
    expect(clampPageNumber(1, 1)).toBe(1);
  });

  it('clamps out-of-range pages into [1, totalPages]', () => {
    expect(clampPageNumber(0, 10)).toBe(1);
    expect(clampPageNumber(-4, 10)).toBe(1);
    expect(clampPageNumber(11, 10)).toBe(10);
  });

  it('floors fractional pages and rejects non-finite input', () => {
    expect(clampPageNumber(2.7, 10)).toBe(2);
    expect(clampPageNumber(Number.NaN, 10)).toBe(1);
    expect(clampPageNumber(Number.POSITIVE_INFINITY, 10)).toBe(10);
  });

  it('returns 1 when the document has no pages yet', () => {
    expect(clampPageNumber(5, 0)).toBe(1);
    expect(clampPageNumber(5, Number.NaN)).toBe(1);
  });
});

describe('formatPageIndicator', () => {
  it('renders current / total for a loaded document', () => {
    expect(formatPageIndicator(2, 16)).toBe('2 / 16');
  });

  it('clamps the displayed page into range', () => {
    expect(formatPageIndicator(99, 16)).toBe('16 / 16');
    expect(formatPageIndicator(0, 16)).toBe('1 / 16');
  });

  it('renders a placeholder before the document is loaded', () => {
    expect(formatPageIndicator(0, 0)).toBe('- / -');
    expect(formatPageIndicator(1, Number.NaN)).toBe('- / -');
  });
});

describe('canGoPrev / canGoNext', () => {
  it('disables prev on the first page and next on the last page', () => {
    expect(canGoPrev(1)).toBe(false);
    expect(canGoNext(16, 16)).toBe(false);
  });

  it('enables navigation inside the document', () => {
    expect(canGoPrev(2)).toBe(true);
    expect(canGoNext(2, 16)).toBe(true);
  });

  it('disables both directions when the document is empty', () => {
    expect(canGoPrev(0)).toBe(false);
    expect(canGoNext(0, 0)).toBe(false);
  });
});

describe('buildQuoteText 划词引用文本', () => {
  it('多行选中文本逐行加引用符，尾随来源行（标题+页码）', () => {
    expect(buildQuoteText(['第一行', '第二行'], '架构方案', 3)).toBe('> 第一行\n> 第二行\n——摘自「架构方案」第 3 页\n\n');
  });

  it('行首尾空白与空行被清理', () => {
    expect(buildQuoteText(['  要点  ', '', '   '], '文档', 1)).toBe('> 要点\n——摘自「文档」第 1 页\n\n');
  });

  it('无标题时来源行只留页码；页码非法时省略页码', () => {
    expect(buildQuoteText(['句子'], '', 2)).toBe('> 句子\n——摘自第 2 页\n\n');
    expect(buildQuoteText(['句子'], '文档', 0)).toBe('> 句子\n——摘自「文档」\n\n');
  });

  it('全空选区返回空串（调用方以此判断"未选中"）', () => {
    expect(buildQuoteText([], '文档', 1)).toBe('');
    expect(buildQuoteText(['   ', ''], '文档', 1)).toBe('');
  });
});

describe('resolveRestorePage 阅读进度恢复', () => {
  it('合法存量页码（2..total）返回该页', () => {
    expect(resolveRestorePage('5', 24)).toBe(5);
    expect(resolveRestorePage(24, 24)).toBe(24);
  });

  it('第 1 页/越界/非法值不恢复（返回 null）', () => {
    expect(resolveRestorePage('1', 24)).toBeNull();
    expect(resolveRestorePage('25', 24)).toBeNull();
    expect(resolveRestorePage('abc', 24)).toBeNull();
    expect(resolveRestorePage(null, 24)).toBeNull();
    expect(resolveRestorePage('5', 0)).toBeNull();
  });
});

describe('readingProgressKey / normalizeReaderTheme', () => {
  it('进度键按资源 id 隔离', () => {
    expect(readingProgressKey('42')).toBe('ip-reader-progress:42');
  });

  it('主题归一化仅认 dark，其余回 light', () => {
    expect(normalizeReaderTheme('dark')).toBe('dark');
    expect(normalizeReaderTheme('light')).toBe('light');
    expect(normalizeReaderTheme(null)).toBe('light');
    expect(normalizeReaderTheme('system')).toBe('light');
  });
});

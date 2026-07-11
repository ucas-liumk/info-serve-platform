import { describe, expect, it } from 'vitest';
import {
  canGoNext,
  canGoPrev,
  clampPageNumber,
  formatPageIndicator,
  keyOfSpreadValue,
  reconcilePageMode,
  resolvePageModeIntent
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

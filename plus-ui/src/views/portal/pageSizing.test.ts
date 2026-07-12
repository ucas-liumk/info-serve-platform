import { describe, expect, it } from 'vitest';
import { DEFAULT_PAGE_SIZE, normalizePageSize, PAGE_SIZE_OPTIONS, persistPageSize, readStoredPageSize } from './pageSizing';

describe('pageSizing 分页档位', () => {
  it('档位为 16/40/80，默认 16，冻结只读', () => {
    expect(PAGE_SIZE_OPTIONS).toEqual([16, 40, 80]);
    expect(DEFAULT_PAGE_SIZE).toBe(16);
    expect(Object.isFrozen(PAGE_SIZE_OPTIONS)).toBe(true);
  });

  it('既定档位原样通过（含字符串形式的存量值）', () => {
    expect(normalizePageSize(16)).toBe(16);
    expect(normalizePageSize(40)).toBe(40);
    expect(normalizePageSize('80')).toBe(80);
  });

  it('非档位/非法值回退默认档', () => {
    expect(normalizePageSize(15)).toBe(DEFAULT_PAGE_SIZE);
    expect(normalizePageSize(0)).toBe(DEFAULT_PAGE_SIZE);
    expect(normalizePageSize(null)).toBe(DEFAULT_PAGE_SIZE);
    expect(normalizePageSize(undefined)).toBe(DEFAULT_PAGE_SIZE);
    expect(normalizePageSize('abc')).toBe(DEFAULT_PAGE_SIZE);
  });
});

describe('readStoredPageSize / persistPageSize', () => {
  it('无 window 环境下读取优雅回退默认档、写入不抛错', () => {
    expect(readStoredPageSize('ip-any-key')).toBe(DEFAULT_PAGE_SIZE);
    expect(() => persistPageSize('ip-any-key', 40)).not.toThrow();
  });
});

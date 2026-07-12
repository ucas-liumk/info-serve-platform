/** 门户列表分页档位（用户定稿：默认 16，可切 40/80；资源共享与应用中心共用） */
export const PAGE_SIZE_OPTIONS: readonly number[] = Object.freeze([16, 40, 80]);
export const DEFAULT_PAGE_SIZE = 16;

/** 归一化页大小：仅接受既定档位，其余（含 NaN/字符串残值）回退默认档 */
export const normalizePageSize = (raw: unknown): number => {
  const value = Number(raw);
  return PAGE_SIZE_OPTIONS.includes(value) ? value : DEFAULT_PAGE_SIZE;
};

/** 读档位记忆：按页面各自的 storageKey 隔离；读取异常（私密模式/SSR）静默回退默认档 */
export const readStoredPageSize = (storageKey: string): number => {
  try {
    return normalizePageSize(window.localStorage.getItem(storageKey));
  } catch {
    return DEFAULT_PAGE_SIZE;
  }
};

/** 写档位记忆：写入异常静默忽略 */
export const persistPageSize = (storageKey: string, size: number): void => {
  try {
    window.localStorage.setItem(storageKey, String(size));
  } catch {
    /* 私密模式下不持久化 */
  }
};

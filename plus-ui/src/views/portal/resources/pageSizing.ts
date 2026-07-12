/** 资料列表分页档位（用户定稿：默认 16，可切 40/80） */
export const PAGE_SIZE_OPTIONS: readonly number[] = Object.freeze([16, 40, 80]);
export const DEFAULT_PAGE_SIZE = 16;

/** 归一化页大小：仅接受既定档位，其余（含 NaN/字符串残值）回退默认档 */
export const normalizePageSize = (raw: unknown): number => {
  const value = Number(raw);
  return PAGE_SIZE_OPTIONS.includes(value) ? value : DEFAULT_PAGE_SIZE;
};

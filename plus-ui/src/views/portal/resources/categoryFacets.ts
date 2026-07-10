/**
 * 栏目→分类两级树的纯函数集合（多选分面模型）。
 * 约定：selected 为已勾选分类 code 数组，空数组=全部；所有函数不可变，返回新数组。
 */
import type { CategoryTreeNode } from '@/api/infoservice/types';

export type GroupCheckState = 'none' | 'partial' | 'all';

export interface CategoryChip {
  code: string;
  name: string;
}

const ALL_SENTINEL = 'all';

const normalizeCodes = (codes: readonly string[]): string[] => {
  const seen = new Set<string>();
  const result: string[] = [];
  for (const raw of codes) {
    const code = raw.trim();
    if (!code || code === ALL_SENTINEL || seen.has(code)) {
      continue;
    }
    seen.add(code);
    result.push(code);
  }
  return result;
};

/** 勾选集 → 逗号串请求参数；空选（=全部）返回 undefined 以省略参数 */
export const encodeCategoryCodes = (codes: readonly string[]): string | undefined => {
  const normalized = normalizeCodes(codes);
  return normalized.length > 0 ? normalized.join(',') : undefined;
};

/** 逗号串 → 勾选集；空串/'all' 哨兵/空白项均视为无选择 */
export const decodeCategoryCodes = (raw?: string | null): string[] => {
  if (!raw) {
    return [];
  }
  return normalizeCodes(raw.split(','));
};

/** 栏目组计数 = 子分类计数求和 */
export const groupResourceCount = (group: CategoryTreeNode): number =>
  (group.children ?? []).reduce((sum, child) => sum + (child.resourceCount ?? 0), 0);

/** 栏目头三态：none=无子项勾选，partial=部分勾选，all=全部勾选 */
export const getGroupCheckState = (group: CategoryTreeNode, selected: readonly string[]): GroupCheckState => {
  const children = group.children ?? [];
  if (children.length === 0) {
    return 'none';
  }
  const selectedSet = new Set(selected);
  const hits = children.filter((child) => selectedSet.has(child.categoryCode)).length;
  if (hits === 0) {
    return 'none';
  }
  return hits === children.length ? 'all' : 'partial';
};

/** 单个分类勾选/反勾选 */
export const toggleCategory = (selected: readonly string[], code: string): string[] =>
  selected.includes(code) ? selected.filter((item) => item !== code) : [...selected, code];

/** 栏目头整组勾选：未全选→并集补齐；已全选→整组移除 */
export const toggleGroup = (selected: readonly string[], group: CategoryTreeNode): string[] => {
  const children = group.children ?? [];
  if (children.length === 0) {
    return [...selected];
  }
  const groupCodes = children.map((child) => child.categoryCode);
  if (getGroupCheckState(group, selected) === 'all') {
    const groupSet = new Set(groupCodes);
    return selected.filter((code) => !groupSet.has(code));
  }
  const selectedSet = new Set(selected);
  return [...selected, ...groupCodes.filter((code) => !selectedSet.has(code))];
};

/** chip 单撤：从勾选集中移除指定 code */
export const removeCategory = (selected: readonly string[], code: string): string[] => selected.filter((item) => item !== code);

const buildNameIndex = (tree: readonly CategoryTreeNode[]): Map<string, string> => {
  const index = new Map<string, string>();
  for (const group of tree) {
    for (const child of group.children ?? []) {
      index.set(child.categoryCode, child.categoryName);
    }
  }
  return index;
};

/** 已选 chip 列表（按勾选顺序）；树中已不存在的 code 以自身兜底，保证仍可撤销 */
export const buildSelectedChips = (tree: readonly CategoryTreeNode[], selected: readonly string[]): CategoryChip[] => {
  const nameIndex = buildNameIndex(tree);
  return selected.map((code) => ({ code, name: nameIndex.get(code) ?? code }));
};

/** 页面标题：空选→全部资源；单选→分类名；多选→已选 N 个分类 */
export const resolveSelectionTitle = (tree: readonly CategoryTreeNode[], selected: readonly string[]): string => {
  if (selected.length === 0) {
    return '全部资源';
  }
  if (selected.length === 1) {
    return buildNameIndex(tree).get(selected[0]) ?? '当前分类';
  }
  return `已选 ${selected.length} 个分类`;
};

import type { NavDomain, NavShortcut } from './types';

/**
 * 图标条域高亮判定：最长前缀胜出，且捷径 scope 激活时域不抢高亮。
 * 纯函数（无 vue-router 依赖），可在 node 环境单测。
 * @param path   当前路由 path
 * @param scope  当前 ?scope= 值（无则空串）
 * @param domains  域列表
 * @param shortcuts 捷径列表
 * @returns 命中的域 key；无命中或捷径激活时返回空串
 */
export function pickActiveDomain(
  path: string,
  scope: string,
  domains: NavDomain[],
  shortcuts: NavShortcut[]
): string {
  if (shortcuts.some((s) => s.query.scope === scope)) return '';
  const matches = domains
    .filter((d) => path === d.route || path.startsWith(d.route + '/'))
    .sort((a, b) => b.route.length - a.route.length);
  return matches[0]?.key ?? '';
}

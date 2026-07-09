import { describe, expect, it } from 'vitest';
import { resolveModuleNav } from './resolveModuleNav';

describe('resolveModuleNav', () => {
  it('已注册模块返回契约完整的配置', () => {
    const nav = resolveModuleNav('resources');
    expect(nav).not.toBeNull();
    expect(nav!.moduleKey).toBe('resources');
    expect(nav!.domains.map((d) => d.key)).toEqual(['center', 'community']);
    expect(nav!.domains[1].badge).toBe('soon');
    expect(nav!.shortcuts.map((s) => s.query.scope)).toEqual(['mine', 'favorites']);
    expect(nav!.category?.queryKey).toBe('category');
    // 图标是 UnoCSS Iconify 类名（保证可渲染且合 design-system §6）
    for (const d of nav!.domains) expect(d.icon).toMatch(/^i-material-symbols:/);
  });

  it('未注册模块返回 null（壳层回退纯内容区）', () => {
    expect(resolveModuleNav('forum')).toBeNull();
    expect(resolveModuleNav(undefined)).toBeNull();
  });
});

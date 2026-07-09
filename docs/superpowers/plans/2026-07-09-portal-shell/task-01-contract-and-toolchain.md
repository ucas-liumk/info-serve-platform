# Task 1: 契约类型 + 导航配置 + 解析器（TDD）+ vitest 基座 + dev 代理参数化

**Files:**
- Create: `plus-ui/vitest.config.ts`
- Create: `plus-ui/src/layout/portal-shell/types.ts`
- Create: `plus-ui/src/layout/portal-shell/configs/resources.ts`
- Create: `plus-ui/src/layout/portal-shell/resolveModuleNav.ts`
- Test: `plus-ui/src/layout/portal-shell/resolveModuleNav.test.ts`
- Modify: `plus-ui/package.json`（scripts 加 `"test": "vitest run"`）
- Modify: `plus-ui/vite.config.ts:27`（proxy target 参数化）
- Create: `plus-ui/.env.development.local`（**不入库**，vite 天然忽略 *.local）

**Interfaces:**
- Consumes: 无（首任务）。
- Produces（后续任务按此消费，勿改签名）: `types.ts` 全部接口（原文见 00-plan 契约总表）；`resolveModuleNav(moduleKey?: string): ModuleNavConfig | null`；`resourcesNav: ModuleNavConfig`（moduleKey `'resources'`，domains `center|community`，shortcuts `mine|favorites`，category.queryKey `'category'`）。

- [ ] **Step 1: vitest 基座**

`plus-ui/vitest.config.ts`：
```ts
import { defineConfig } from 'vitest/config';
import { fileURLToPath, URL } from 'node:url';

export default defineConfig({
  resolve: {
    alias: { '@': fileURLToPath(new URL('./src', import.meta.url)) }
  },
  test: {
    environment: 'node',
    include: ['src/**/*.test.ts']
  }
});
```
`plus-ui/package.json` 的 scripts 中、`"design:audit"` 行之后追加：
```json
    "test": "vitest run",
```
运行 `cd plus-ui && npm run test`，预期：现有 `importTemplates.test.ts` 被收集并通过（或因其依赖报错——若报错，把 include 收窄为 `src/layout/portal-shell/**/*.test.ts` 并在报告注明）。

- [ ] **Step 2: 写失败测试（RED）**

`plus-ui/src/layout/portal-shell/resolveModuleNav.test.ts`：
```ts
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
```

- [ ] **Step 3: 确认 RED**

Run: `cd plus-ui && npx vitest run src/layout/portal-shell/resolveModuleNav.test.ts`
Expected: FAIL（Cannot find module './resolveModuleNav'）

- [ ] **Step 4: 实现（GREEN 最小集）**

`plus-ui/src/layout/portal-shell/types.ts`：内容 = 00-plan「契约总表」代码块**逐字**（含注释）。

`plus-ui/src/layout/portal-shell/configs/resources.ts`：
```ts
import type { CategoryItem, ModuleNavConfig } from '../types';
import { listResourceCategories } from '@/api/portal/resources';

async function fetchCategories(): Promise<CategoryItem[]> {
  const res = await listResourceCategories();
  const rows = (res as any).data ?? [];
  // 字段名以 api/portal/resources.ts 实际类型为准（categoryCode/categoryName/资源计数），
  // 若类型命名不同，按实际字段映射，禁止改 API 层
  return rows.map((c: any) => ({ key: c.categoryCode, label: c.categoryName, count: c.resourceCount ?? c.count }));
}

export const resourcesNav: ModuleNavConfig = {
  moduleKey: 'resources',
  domains: [
    { key: 'center', label: '资料中心', icon: 'i-material-symbols:library-books-outline-rounded', route: '/portal/resources' },
    { key: 'community', label: '交流互动', icon: 'i-material-symbols:forum-outline-rounded', route: '/portal/resources/community', badge: 'soon' }
  ],
  shortcuts: [
    { key: 'mine', label: '我的上传', icon: 'i-material-symbols:upload-file-outline-rounded', route: '/portal/resources', query: { scope: 'mine' } },
    { key: 'favorites', label: '我的收藏', icon: 'i-material-symbols:star-outline-rounded', route: '/portal/resources', query: { scope: 'favorites' } }
  ],
  category: { title: '分类', queryKey: 'category', fetch: fetchCategories }
};
```

`plus-ui/src/layout/portal-shell/resolveModuleNav.ts`：
```ts
import type { ModuleNavConfig } from './types';
import { resourcesNav } from './configs/resources';

const registry: Record<string, ModuleNavConfig> = {
  resources: resourcesNav
};

export function resolveModuleNav(moduleKey?: string): ModuleNavConfig | null {
  if (!moduleKey) return null;
  return registry[moduleKey] ?? null;
}
```
> 若 node 环境跑测试时 `@/api/portal/resources` 的传递依赖（request/axios）报错：把 `configs/resources.ts` 的 `fetchCategories` 改为动态导入 `const { listResourceCategories } = await import('@/api/portal/resources')`（函数体内），保持配置模块本身纯净。报告注明采用了哪种形态。

- [ ] **Step 5: 确认 GREEN**

Run: `cd plus-ui && npx vitest run src/layout/portal-shell/resolveModuleNav.test.ts`
Expected: 2 passed。

- [ ] **Step 6: dev 代理参数化**

`plus-ui/vite.config.ts` 中 proxy 的 `target: 'http://localhost:8180'`（约 L27）改为：
```ts
target: env.VITE_APP_PROXY_TARGET || 'http://localhost:8180',
```
（`env` 变量在该文件顶部已有 `loadEnv` 产物，沿用；若变量名不同以文件实际为准。）
新建 `plus-ui/.env.development.local`（本机生效、不入库）：
```
VITE_APP_PROXY_TARGET = 'http://192.168.8.10:8180'
```
验证：`cd plus-ui && npm run dev` 启动后，`curl -s http://127.0.0.1:7018/dev-api/infoservice/portal/modules | head -c 80` 返回 JSON envelope（`{"code":401…` 即通，代理已达 Windows 网关）。验证后 Ctrl-C 停掉。

- [ ] **Step 7: 门禁 + 提交**

```bash
cd plus-ui && npm run build:prod && npm run design:audit && cd ..
git add plus-ui/vitest.config.ts plus-ui/package.json plus-ui/vite.config.ts plus-ui/src/layout/portal-shell
git commit -m "feat: 门户壳层契约类型与资料模块导航配置（含 vitest 基座）"
```
（`.env.development.local` 与 `.eslintrc-auto-import.json` 不入提交。）

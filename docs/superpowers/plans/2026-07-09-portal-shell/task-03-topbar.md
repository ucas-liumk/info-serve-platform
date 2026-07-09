# Task 3: TopBar（注册表驱动一级导航）

**Files:**
- Create: `plus-ui/src/layout/portal-shell/TopBar.vue`

**Interfaces:**
- Consumes: `listPortalModules`（`@/api/portal/module`，字段 moduleCode/moduleName/entryPath/status/sortOrder）；`PortalNotificationBell`、`PortalUserMenu`（`@/layout/portal/components/`）。
- Produces: `<TopBar />` 无 props 组件，T5 的 PortalShell 顶部使用。高 56px、sticky。

- [ ] **Step 1: 实现组件（完整代码）**

`plus-ui/src/layout/portal-shell/TopBar.vue`：
```vue
<template>
  <header class="ps-topbar">
    <RouterLink to="/portal" class="ps-topbar__brand" aria-label="返回门户首页">
      <span class="i-material-symbols:hub-outline-rounded ps-topbar__logo" aria-hidden="true" />
      <span class="ps-topbar__title">信息中心数智服务平台</span>
    </RouterLink>

    <nav class="ps-topbar__nav" aria-label="门户模块导航">
      <template v-for="m in modules" :key="m.moduleCode">
        <RouterLink
          v-if="m.status === '0' && m.entryPath"
          :to="m.entryPath"
          class="ps-topbar__item"
          :class="{ 'is-active': isActive(m.entryPath) }"
        >{{ m.moduleName }}</RouterLink>
        <el-tooltip v-else-if="m.status === '1'" content="敬请期待" placement="bottom">
          <span class="ps-topbar__item is-soon" aria-disabled="true">{{ m.moduleName }}</span>
        </el-tooltip>
      </template>
    </nav>

    <div class="ps-topbar__actions">
      <PortalNotificationBell />
      <PortalUserMenu />
    </div>
  </header>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue';
import { useRoute } from 'vue-router';
import { listPortalModules, type PortalModuleItem } from '@/api/portal/module';
import PortalNotificationBell from '@/layout/portal/components/PortalNotificationBell.vue';
import PortalUserMenu from '@/layout/portal/components/PortalUserMenu.vue';

const route = useRoute();
const modules = ref<PortalModuleItem[]>([]);

const isActive = (entryPath: string) => route.path === entryPath || route.path.startsWith(entryPath + '/');

onMounted(async () => {
  try {
    const res = await listPortalModules();
    modules.value = ((res as any).data ?? [])
      .filter((m: PortalModuleItem) => m.status !== '2')
      .sort((a: PortalModuleItem, b: PortalModuleItem) => (a.sortOrder ?? 0) - (b.sortOrder ?? 0));
  } catch {
    modules.value = []; // 注册表不可用：只影响导航项，铃/用户菜单照常
  }
});
</script>

<style scoped>
.ps-topbar {
  position: sticky;
  top: 0;
  z-index: 30;
  display: flex;
  align-items: center;
  gap: 24px;
  height: 56px;
  padding: 0 24px;
  background: var(--ip-neutral-0);
  border-bottom: 1px solid var(--ip-neutral-200);
  box-shadow: var(--ip-shadow-sm);
}
.ps-topbar__brand {
  display: flex;
  align-items: center;
  gap: 8px;
  color: var(--ip-neutral-900);
  text-decoration: none;
  border-radius: var(--ip-radius-sm);
}
.ps-topbar__logo { font-size: 24px; color: var(--ip-primary-600); }
.ps-topbar__title { font-size: var(--ip-font-title-sm); font-weight: 600; white-space: nowrap; }
.ps-topbar__nav { display: flex; align-items: center; gap: 4px; min-width: 0; overflow-x: auto; }
.ps-topbar__item {
  padding: 6px 12px;
  border-radius: var(--ip-radius-full);
  font-size: var(--ip-font-body);
  color: var(--ip-neutral-600);
  text-decoration: none;
  white-space: nowrap;
  transition: color var(--ip-motion-fast) var(--ip-motion-ease), background-color var(--ip-motion-fast) var(--ip-motion-ease);
}
.ps-topbar__item:hover { color: var(--ip-primary-700); background: var(--ip-primary-50); }
.ps-topbar__item.is-active { color: var(--ip-primary-700); background: var(--ip-primary-100); font-weight: 600; }
.ps-topbar__item.is-soon { color: var(--ip-neutral-400); cursor: not-allowed; }
.ps-topbar__actions { margin-left: auto; display: flex; align-items: center; gap: 12px; }
</style>
```
> 令牌名（`--ip-font-title-sm`/`--ip-primary-100` 等）以 `tokens.scss` 实际存在的档位为准——若个别档位名不同（如 title 档叫 `--ip-font-title`），就近换用真实令牌，**禁止硬编码数值兜底**。`PortalModuleItem` 类型名以 `api/portal/module.ts` 实际导出为准。

- [ ] **Step 2: 临时挂载验证（不接线路由）**

临时在 `home/index.vue` 模板首行加 `<TopBar />`（含 import），`npm run dev` 目检：模块项渲染（当前 6 个注册模块，qa/news 灰显带 tooltip）、悬停/激活态、铃与用户菜单可用、`:focus-visible` 有焦点环（Tab 键走查）。**目检完撤掉临时挂载**（该文件恢复原状，`git diff` 确认 home/index.vue 零改动）。

- [ ] **Step 3: 门禁 + 提交**

```bash
cd plus-ui && npm run build:prod && npm run design:audit && cd ..
git add plus-ui/src/layout/portal-shell/TopBar.vue
git commit -m "feat: 门户壳层顶栏（模块注册表驱动一级导航）"
```

# Task 5: PortalShell 组装 + 路由接线 + 交流互动占位页（壳层首租户）

策略：让**新建的占位页**当壳层第一个租户——先在零存量风险的页面上验证壳层全形态，资料列表页（T6）迁入时壳层已被证明可用。

**Files:**
- Create: `plus-ui/src/layout/portal-shell/PortalShell.vue`
- Create: `plus-ui/src/views/portal/resources/community.vue`
- Modify: `plus-ui/src/router/index.ts`（resources 子路由处新增 community 路由，带 meta）
- Modify: `plus-ui/src/layout/portal/index.vue`（新增 portalShell 分支）

**Interfaces:**
- Consumes: T1 `resolveModuleNav`；T3 `TopBar`；T4 `IconRail`/`CategoryRail`。
- Produces: 路由 meta 约定 `{ portalShell: true, portalModule: 'resources' }`；`/portal/resources/community` 可访问；壳层对 `resolveModuleNav` 返回 null 的路由回退为「TopBar + 纯内容区」。

- [ ] **Step 1: PortalShell 实现（完整代码）**

`plus-ui/src/layout/portal-shell/PortalShell.vue`：
```vue
<template>
  <div class="ps-shell">
    <TopBar />
    <div class="ps-shell__body">
      <IconRail v-if="nav" :domains="nav.domains" :shortcuts="nav.shortcuts" />
      <CategoryRail
        v-if="nav?.category && showCategory"
        :key="nav.moduleKey"
        :title="nav.category.title"
        :query-key="nav.category.queryKey"
        :fetch="nav.category.fetch"
        :module-key="nav.moduleKey"
      />
      <main class="ps-shell__main">
        <router-view />
      </main>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue';
import { useRoute } from 'vue-router';
import TopBar from './TopBar.vue';
import IconRail from './IconRail.vue';
import CategoryRail from './CategoryRail.vue';
import { resolveModuleNav } from './resolveModuleNav';

const route = useRoute();
const nav = computed(() => resolveModuleNav(route.meta.portalModule as string | undefined));
// 无分类语义的域不渲染分类栏（spec §2）：当前路由命中的 domain 若 badge=soon 或非 center 域，由 meta.portalNoCategory 显式声明
const showCategory = computed(() => !route.meta.portalNoCategory);
</script>

<style scoped>
.ps-shell { display: flex; flex-direction: column; min-height: 100vh; background: var(--ip-neutral-50); }
.ps-shell__body { display: flex; flex: 1; min-height: 0; }
.ps-shell__main { flex: 1; min-width: 0; padding: 16px 24px; max-width: var(--ip-layout-max); }
@media (max-width: 979px) {
  /* 试点期移动端保功能：分类栏隐藏，图标条转横条 */
  .ps-shell__body { flex-direction: column; }
  .ps-shell__body :deep(.ps-rail) { flex-direction: row; width: 100%; height: 48px; padding: 4px 8px; }
  .ps-shell__body :deep(.ps-rail__spacer) { flex: 1; }
  .ps-shell__body :deep(.ps-rail__shortcuts) { border-top: none; border-left: 1px solid var(--ip-neutral-700); padding: 0 0 0 8px; flex-direction: row; }
  .ps-shell__body :deep(.ps-rail__domains) { flex-direction: row; }
  .ps-shell__body :deep(.ps-cat) { display: none; }
}
</style>
```

- [ ] **Step 2: 占位页（完整代码，五态里的「空态」范式 + 预告文案）**

`plus-ui/src/views/portal/resources/community.vue`：
```vue
<template>
  <section class="rc-empty" aria-labelledby="rc-empty-title">
    <span class="i-material-symbols:forum-outline-rounded rc-empty__icon" aria-hidden="true" />
    <h2 id="rc-empty-title" class="rc-empty__title">交流互动 · 敬请期待</h2>
    <p class="rc-empty__desc">
      未来在这里分享你的学习笔记与读到的好内容——好内容值得被更多人看见。
      上线后将聚合大家在资料预览中留下的公开笔记。
    </p>
    <el-button type="primary" @click="$router.push('/portal/resources')">先去资料中心看看</el-button>
  </section>
</template>

<script setup lang="ts"></script>

<style scoped>
.rc-empty {
  display: flex; flex-direction: column; align-items: center; justify-content: center;
  gap: 16px; min-height: 60vh; text-align: center; padding: 48px 24px;
}
.rc-empty__icon { font-size: 64px; color: var(--ip-primary-300); }
.rc-empty__title { font-size: var(--ip-font-title); color: var(--ip-neutral-800); margin: 0; }
.rc-empty__desc { font-size: var(--ip-font-body); color: var(--ip-neutral-500); max-width: 420px; margin: 0; line-height: 1.7; }
</style>
```
> 「每屏至多一个实心 primary」（design-system §7.1）：本页唯一按钮即它。令牌档位名以 tokens.scss 实际为准。

- [ ] **Step 3: 路由与老壳分支接线**

`plus-ui/src/router/index.ts`，resources 预览路由之后插入：
```ts
      {
        path: 'resources/community',
        name: 'InfoResourceCommunity',
        component: () => import('@/views/portal/resources/community.vue'),
        meta: { title: '交流互动', portalShell: true, portalModule: 'resources', portalNoCategory: true }
      },
```
`plus-ui/src/layout/portal/index.vue`：
- 模板改为三分支：
```html
  <div class="portal-layout" :class="{ 'home-mode': isHome }">
    <PortalShell v-if="usesPortalShell" />
    <template v-else>
      <PortalSidebar v-if="!isHome && !usesOwnShell" />
      <section class="portal-main" :class="{ 'home-main': isHome, 'own-shell-main': usesOwnShell }">
        <router-view />
      </section>
    </template>
  </div>
```
- script 增加：
```ts
import PortalShell from '@/layout/portal-shell/PortalShell.vue';
const usesPortalShell = computed(() => route.meta.portalShell === true);
```
- 在 `usesOwnShell` 名单上方加注释：`// 冻结名单：新模块一律走 meta.portalShell，禁止再往这里加名字（spec 2026-07-09 §3）`。

⚠️ 注意：PortalShell 内部有 `<router-view />`，而它自身处于父路由组件内——**community 是子路由**，父组件 `layout/portal/index.vue` 渲染 PortalShell、PortalShell 再渲染子路由出口，层级正确（与现状 `portal-main > router-view` 同构）。

- [ ] **Step 4: 运行时验证（壳层全形态首秀）**

`npm run dev` 登录后访问 `http://127.0.0.1:7018/portal/resources/community`：
- [ ] TopBar 在场：模块项高亮「资料共享」（entryPath `/portal/resources` 前缀匹配）、qa/news 灰显、铃/用户菜单可用；
- [ ] IconRail 在场：资料中心/交流互动（带「待」徽标）+ 底部我的上传/我的收藏；交流互动高亮；
- [ ] 无分类栏（portalNoCategory）；
- [ ] 空态页居中、文案完整、按钮跳资料中心（旧页，仍走 usesOwnShell——此时点回 community 再验证往返）；
- [ ] Tab 走查焦点环齐全；`/portal` 首页与其余模块页零变化（逐页点开目检）；
- [ ] 940px 窗宽下图标条转横条、功能可用。

- [ ] **Step 5: 门禁 + 提交**

```bash
cd plus-ui && npm run build:prod && npm run design:audit && npm run test && cd ..
git add plus-ui/src/layout/portal-shell plus-ui/src/views/portal/resources/community.vue plus-ui/src/router/index.ts plus-ui/src/layout/portal/index.vue
git commit -m "feat: 门户壳层组装上线，交流互动占位页作首租户"
```

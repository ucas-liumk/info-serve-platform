# Task 6: 资料列表页剥壳迁入（scope/category 全 query 驱动）

背景锚点（recon 实证，行号为迁移前基线、以方法/类名定位为准）：`plus-ui/src/views/portal/resources/index.vue`（738 行）——自建侧栏 `<ResourceSidebar>` L3-8、自建顶栏 `<header class="resource-topbar">` L11-45（内含搜索框、`<PortalNotificationBell/>`、上传/我的资源按钮）、列表本体 `<main class="resource-main">` L10-89（保留）、`MyResourcesDrawer` L91-106、`ResourceUploadDialog` L108-115；`reload()` L201-220 硬编码 `scope:'public'`（L205）；`loadCategories` L196-199、`categoryCode` 默认 `'all'`、`changeCategory` L266；页面自建布局 CSS L456-738（`.resources-app` 网格与渐变背景、`.resource-topbar`）。

**Files:**
- Modify: `plus-ui/src/router/index.ts`（InfoResources 路由加 meta）
- Modify: `plus-ui/src/layout/portal/index.vue`（`usesOwnShell` 名单移除 `'InfoResources'`——仅此一项，Preview 保留）
- Modify: `plus-ui/src/views/portal/resources/index.vue`（剥壳 + query 接线）

**Interfaces:**
- Consumes: T5 已上线的壳层（meta 接入约定）；T1 契约里 `queryKey='category'`、捷径 query `scope=mine|favorites`。
- Produces: 资料中心在壳层内全功能运行；`?scope=`/`?category=` 成为页面的唯一外部输入面。

- [ ] **Step 1: 路由接线**

`router/index.ts` 的 InfoResources 路由 meta 改为：
```ts
meta: { title: '资料共享', portalShell: true, portalModule: 'resources' }
```
`layout/portal/index.vue` 的 `usesOwnShell` 计算属性中删除 `route.name === 'InfoResources' ||` 一行（其余名字不动）。

- [ ] **Step 2: 模板剥壳与功能迁位**

`resources/index.vue` 模板：
1. **删除** `<ResourceSidebar>` 整块（分类由壳层 CategoryRail 承接）与其 import；
2. **删除** `<header class="resource-topbar">` 整块，但先把其中**搜索框**与**上传按钮**两个模板节点（连同绑定）原样搬入新建的动作行（放在 `ResourceToolbar` 同一行或其上方）：
```html
      <div class="resource-actions">
        <!-- ↓ 原 topbar 的搜索框节点原样粘贴（keyword 绑定与回车/清空事件不改） -->
        <!-- ↓ 原 topbar 的上传按钮节点原样粘贴（打开 ResourceUploadDialog 的 handler 不改） -->
      </div>
```
3. **删除**「我的资源」按钮与 `<MyResourcesDrawer>` 挂载及其 import/状态（能力被壳层捷径 `?scope=mine` 取代；`MyResourcesDrawer.vue`、`ResourceSidebar.vue`、`ResourceFilterPanel.vue` 组件文件**本任务不删**，留 T7 判定清理——先证明新路径全功能）；
4. `<PortalNotificationBell/>` 随 topbar 删除（TopBar 已有铃，不得双铃）；
5. 根容器类 `.resources-app` 改为 `.resource-page`（纯内容容器）。

- [ ] **Step 3: scope/category 接 URL query（关键代码）**

script 部分：
```ts
const route = useRoute();

// scope：public（默认）| mine | favorites —— 唯一来源是 URL
const scope = computed(() => {
  const s = route.query.scope as string;
  return s === 'mine' || s === 'favorites' ? s : 'public';
});
```
1. `reload()` 中 `scope: 'public'` 改为 `scope: scope.value`；
2. `categoryCode` 不再由点击直接赋值：初始化与变更都从 query 读——
```ts
watch(
  () => [route.query.scope, route.query.category],
  () => {
    categoryCode.value = (route.query.category as string) || 'all';
    reloadFirst();
  },
  { immediate: true }
);
```
（原 `onMounted` 里的首次 `reload()` 若与 `immediate:true` 重复触发，删掉原调用，保证首屏只请求一次；`changeCategory`、drawer 相关的 `loadMyResources/myResources` 状态与函数整组删除。）
3. 列表空态文案按 scope 区分（美观五态）：`scope==='mine'` → 「还没有上传过资料」，`'favorites'` → 「还没有收藏资料」，其余保持现文案。给出真实实现（现空态节点在 grid/list 区，按现有空态写法改造）。

- [ ] **Step 4: CSS 剥离**

`<style scoped>` 中**删除**：`.resources-app`（含 grid 布局、渐变背景、局部 `--resource-*` 变量块）、`.resource-topbar` 全族、侧栏相关类。**保留**：`.resource-grid`、`.resource-results`、`.resource-content`、`.pager`、媒体查询栅格。新增最小容器与动作行样式（全令牌）：
```css
.resource-page { display: flex; flex-direction: column; gap: 16px; }
.resource-actions { display: flex; align-items: center; gap: 12px; flex-wrap: wrap; }
```

- [ ] **Step 5: 运行时功能等价清单（dev，逐项打勾）**

`npm run dev` → `/portal/resources`：
- [ ] 壳层三段在场；分类栏出分类+计数，点分类 → URL 带 `?category=` 且列表过滤；「全部」清参数；
- [ ] 折叠分类栏 → 刷新后仍收起（storage 记忆）；展开动效平滑（320ms）；
- [ ] 图标条「我的上传/我的收藏」→ URL 带 scope、列表切换、空态文案正确；点「资料中心」域回 public；
- [ ] 搜索、排序、grid/list 切换、分页全可用；上传对话框开/传/列表刷新；
- [ ] 点资料卡进预览页（独立全屏如旧）、返回后 query 态保留；收藏/取消收藏可用且 favorites 视图即时反映；
- [ ] 直接粘贴 `…/portal/resources?scope=favorites&category=<某码>` 冷加载：视图正确（可分享链接语义）；
- [ ] 940px 窗宽功能可用。

- [ ] **Step 6: 门禁 + 提交**

```bash
cd plus-ui && npm run build:prod && npm run design:audit && npm run test && cd ..
git add plus-ui/src/views/portal/resources/index.vue plus-ui/src/router/index.ts plus-ui/src/layout/portal/index.vue
git commit -m "feat: 资料共享迁入门户壳层，我的视图与分类改由 URL 驱动"
```
（本任务删掉大量硬编码 hex，design:audit 计数应显著下降——记录新计数供 T7 收紧。）

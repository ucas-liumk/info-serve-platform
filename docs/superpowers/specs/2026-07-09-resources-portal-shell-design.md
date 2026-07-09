# 资料模块门户壳层试点设计（门户前端布局统一 · 一期）

状态：定稿（2026-07-09，与用户逐节确认通过；线框存档 `.superpowers/brainstorm/63375-1783612352/content/`）
关联：`docs/design/design-system.md` v1.0（视觉正本）· `docs/architecture/bounded-contexts.md`（模块注册表/入口层原则）· spec 2026-07-07（批次 D 第三方接入将消费本契约）

## 1. 背景与目标

门户各模块前端布局各自为政：壳层 `layout/portal/index.vue` 虽有 `PortalSidebar`，但全部内容路由经硬编码 `usesOwnShell` 名单退出，五个模块页（383~738 行）各自手搓整页布局，背景/字体/导航样式多份复制；模块注册表只驱动首页卡片，未成为常驻导航。本设计建立**统一门户壳层格律**，并以资料共享模块为唯一试点落地——效果验证后其他模块按同一契约逐个迁移。

**用户裁决记录**：布局采用固定三段式（顶栏一级 + 窄图标二级条 + 可折叠分类栏）；范围仅门户端（管理后台不动）；首页完全豁免（顶栏只在模块内出现）；本轮只迁资料模块；资料二级菜单 =「资料中心 + 交流互动（书友式笔记分享，先占位）」；「我的上传/我的收藏」属个人捷径而非二级菜单，沉底陈列。

## 2. 壳层格律（三段式）

```
┌──────────────────────────────────────────────────────┐
│ TopBar：logo · 注册表模块项(一级) · 消息铃 · 用户      │
├──────┬─────────────┬─────────────────────────────────┤
│Icon  │ CategoryRail │                                 │
│Rail  │ 分类栏 ~180px │        内容区                    │
│48px  │ (可收起16px)  │                                 │
│      │              │                                 │
│域导航 │              │                                 │
│ ⋮    │              │                                 │
│─────  │              │                                 │
│捷径区 │              │                                 │
└──────┴─────────────┴─────────────────────────────────┘
```

- **TopBar（全局，一级导航）**：模块项来自模块注册表 API `/infoservice/portal/modules`（权限过滤后端已做）；`status='1'`（敬请期待）项显示禁用态徽标；当前模块高亮；消息铃与用户菜单复用首页现有组件。首页不渲染 TopBar（豁免）。
- **IconRail（48px 常驻，模块二级）**：纵向三区——**域导航区**（模块内容子域，图标+8px 小字，可带 `soon` 徽标）｜**弹性留白**｜**个人捷径区**（横线分隔沉底，承载「我的X」类过滤捷径）。语义纪律：域回答「我在哪」，捷径回答「我的东西」，禁止混排。
- **CategoryRail（~180px，从属当前域）**：标题 + 分类项；可收起为 16px 细条（专注态，内容区最大化）；无分类语义的域（如交流互动占位页）不渲染该栏。
- 收起状态与上次所在域按用户浏览器记忆（localStorage，键前缀 `portal-shell:<module>:`）。

## 3. 组件结构与接入方式

```
plus-ui/src/layout/portal-shell/
  PortalShell.vue      骨架：TopBar + IconRail + CategoryRail 插槽 + <router-view>
  TopBar.vue           一级导航
  IconRail.vue         图标条（域导航区/捷径区）
  CategoryRail.vue     分类栏（含收起态）
  types.ts             ModuleNavConfig 契约类型
```

- 接入开关：路由 `meta.portalShell: true` → 走 `PortalShell`；否则维持现状（老壳与 `usesOwnShell` 存量名单不动、不再增长）。回退 = 摘除 meta 标记。
- 老页面文件保留一个版本周期后再删。

## 4. 模块导航契约（ModuleNavConfig，后续模块与第三方复用）

```ts
interface ModuleNavConfig {
  domains: Array<{ key: string; label: string; icon: string; route: string; badge?: 'soon' }>;
  shortcuts: Array<{ key: string; label: string; icon: string; action: (router) => void }>;
  category?: {
    title: string;
    fetch: () => Promise<Array<{ key: string; label: string; count?: number }>>;
    onSelect: (key: string) => void;
  };
}
```

**资料模块声明**：
- `domains`: 资料中心（`/portal/resources`）｜交流互动（`/portal/resources/community`，`badge:'soon'`）
- `shortcuts`: 我的上传 → 资料中心 + `scope=mine`｜我的收藏 → 资料中心 + `scope=favorites`（现有查询参数，API 零改动）
- `category`: 「分类」，取现有 `/infoservice/portal/resources/categories` 接口，选中即列表过滤

契约同时文档化为 `docs/design/design-system.md` 新章节「门户壳层格律」，作为后续模块迁移与批次 D 第三方模块的前端接入契约。

## 5. 路由与页面改造

- `/portal/resources`：资料中心 = 现列表页瘦身迁入——738 行页面剥离自建顶栏/侧栏/背景（迁入壳层区位），保留列表、筛选、上传、收藏全部业务逻辑；
- `/portal/resources/preview/:id`：资料预览保持独立全屏（沉浸阅读场景，不入壳）；
- `/portal/resources/community`：交流互动占位页——图标 + 「敬请期待」+ 一句能力预告（书友式学习笔记分享）。**占位有保质期**：MVP = 公开笔记聚合流（`res_note` 表与公开笔记 API 现成），本试点验收后立即立项，占位期目标 ≤1 个迭代；
- 我的上传/收藏不设独立路由（查询参数承载），链接分享语义不变。

## 6. 响应式与设计系统对齐

- ≥1280px 全形态；1024~1280px 分类栏默认收起；<980px 沿用现有移动断点逻辑，图标条转底部横条/抽屉——试点期移动端只保功能可用，不做精修；
- 颜色/字号/间距一律取 `tokens.scss` 令牌；壳层不发明新视觉语言，视觉基调=design-system v1.0；`npm run design:audit` 棘轮门禁必绿。

## 7. 不变量与验收

- **后端零改动**；首页与其余四个模块页面像素级零变化（逐页目检）；
- 资料模块全功能等价：列表/筛选/分类/上传/预览/收藏/我的视图与改版前一致（对照现有 e2e 资料相关断言 + 手工清单）;
- 门禁：`npm run build:prod` + `npm run design:audit`；
- 回退路径：摘 `meta.portalShell` 即回旧页。

## 8. 决策记录

- [x] 布局格律：固定三段式 + 窄图标条（默认即收起态）+ 可折叠分类栏（2026-07-09，用户选定并细化）
- [x] 范围：仅门户端；首页完全豁免；本轮仅资料模块试点
- [x] 资料二级菜单：资料中心 + 交流互动（占位，MVP=公开笔记聚合，验收后立项）
- [x] 我的上传/我的收藏：个人捷径沉底，不作二级菜单
- [x] 预览页不入壳；老壳与存量 `usesOwnShell` 名单冻结不增长
- [x] 后续模块迁移与第三方接入均按 §4 契约执行

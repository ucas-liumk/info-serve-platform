# 门户壳层试点（资料模块）实施计划 · 主文档

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.
> 任务正文在同目录 `task-01`~`task-08`，按编号顺序执行，每个任务文件自包含。

**Goal:** 落地统一门户壳层（TopBar + IconRail + CategoryRail 三段式）并迁入资料共享模块作唯一试点，交流互动占位页同步上线，美观达到 design-system v1.0 门禁与用户目验双重标准。

**Architecture:** 壳层四组件 + `ModuleNavConfig` 契约新建于 `plus-ui/src/layout/portal-shell/`；接入走路由 `meta.portalShell`（老壳 `usesOwnShell` 名单冻结）；**分类与个人捷径一律 URL query 驱动**（`?category=` / `?scope=`），壳层与页面零回调耦合；交流互动占位页先做壳层首租户（零回归验证），资料列表页随后剥壳迁入。

**Tech Stack:** Vue 3 `<script setup lang="ts">` + Element Plus + UnoCSS + VueUse（useStorage）+ unplugin-icons（Iconify `~icons/*`）+ vitest（本轮引入最小配置）。

## Global Constraints（每任务隐含遵守）

1. **分支**：全部工作在 `feature/resources-portal-shell`（已存在，基于含批次 A 的 main）。提交 `<type>: <中文描述>`，禁署名尾注；工作区既有 `plus-ui/.eslintrc-auto-import.json` 改动不属于任何任务，**严禁 add/commit**（提交用 `git add <明确路径>`）。
2. **后端零改动**；首页与其余模块页**像素级零变化**（T2 触碰 home 仅限重构抽取，行为与视觉不变）。
3. **美观硬约束**（用户点名）：
   - 颜色/圆角/阴影/字号只用 `tokens.scss` 的 `--ip-*` 令牌；间距只用 4/8/12/16/24/32/48/64px 八档；
   - 动效用 `--ip-motion-base`(200ms)/`--ip-motion-panel`(320ms) + `--ip-motion-ease`，分类栏折叠必须有过渡；遵守 `prefers-reduced-motion`；
   - 所有可交互元素有 `:hover` 态与可见 `:focus-visible`（`--ip-focus-ring`），禁裸 `outline:none`；图标按钮带 `aria-label`；
   - 图标唯一来源 Iconify `~icons/*`（复用的 `PortalNotificationBell` 内部 EP 图标属存量豁免）；
   - 分类栏交付五态：加载骨架/空数据/错误重试/正常/收起（design-system §7.4）。
4. **门禁**：每任务收尾必过 `cd plus-ui && npm run build:prod && npm run design:audit`；T7 用 `--update-baseline` **收紧**棘轮基线（只降不升）。
5. **验证环境**：开发用 Mac vite dev（T1 给 `vite.config.ts` 加 `VITE_APP_PROXY_TARGET`，指 `http://192.168.8.10:8180`，Windows 活栈网关）；最终目验在 Windows 19100 容器（T8）。
6. 单文件 ≤800 行；新组件遵循现有 `<script setup lang="ts">` 惯例；localStorage 一律 `useStorage`（自动导入，勿手写 import）。

## 契约总表（T1 定义，全计划唯一权威）

```ts
// plus-ui/src/layout/portal-shell/types.ts
export interface NavDomain { key: string; label: string; icon: string; route: string; badge?: 'soon' }
export interface NavShortcut { key: string; label: string; icon: string; route: string; query: Record<string, string> }
// icon = UnoCSS presetIcons 类名（Iconify 数据源），如 'i-material-symbols:library-books-outline-rounded'
// ——纯字符串保证配置/解析器可在 node 环境单测，渲染层 <span :class="icon"> 即出图标
export interface CategoryItem { key: string; label: string; count?: number }
export interface ModuleNavConfig {
  moduleKey: string;                    // 'resources'，storage 键与高亮匹配用
  domains: NavDomain[];
  shortcuts: NavShortcut[];
  category?: { title: string; queryKey: string; fetch: () => Promise<CategoryItem[]> };
}
```

- 路由接线：`meta: { portalShell: true, portalModule: 'resources' }`；`resolveModuleNav(portalModule)` 从配置注册表取 config，未注册返回 `null`（壳层回退渲染纯内容区）。
- 捷径/分类均产生 `router.push({ query })`；页面只观察 `route.query`（`scope`/`category`），不感知壳层存在。
- storage 键：`portal-shell:<moduleKey>:cat-collapsed`（boolean）；域高亮由当前路由匹配 `domains[].route` 得出，不另存。
- TopBar 数据：`listPortalModules()`（`@/api/portal/module`）；`status==='0'` 可点、`'1'` 禁用+「敬请期待」tooltip、`'2'` 不渲染。

## 文件地图

| 文件 | 责任 | 任务 |
|---|---|---|
| `plus-ui/vitest.config.ts`、`src/layout/portal-shell/types.ts`、`configs/resources.ts`、`resolveModuleNav.ts`(+test) | 契约与解析器 | T1 |
| `src/layout/portal/components/PortalUserMenu.vue` | 用户菜单+资料/密码弹窗+退出（自 home 抽取） | T2 |
| `src/layout/portal-shell/TopBar.vue` | 一级导航 | T3 |
| `src/layout/portal-shell/IconRail.vue`、`CategoryRail.vue` | 二级图标条/分类栏 | T4 |
| `src/layout/portal-shell/PortalShell.vue`、`src/views/portal/resources/community.vue`、`router/index.ts`、`layout/portal/index.vue` | 壳层组装+首租户 | T5 |
| `src/views/portal/resources/index.vue`（738→瘦身） | 资料页迁入 | T6 |
| `plus-ui/design-audit-baseline.json`、`docs/design/design-system.md` | 棘轮收紧+契约章节 | T7 |
| —（Windows 部署与目验） | 验收 | T8 |

## 任务索引

| 任务 | 交付 | 前置 |
|---|---|---|
| T1 | vitest 基座 + 契约类型 + resources 导航配置 + 解析器（TDD）+ dev 代理参数化 | — |
| T2 | PortalUserMenu 抽取，home 行为/视觉零变化 | — |
| T3 | TopBar（注册表驱动 + 铃 + 用户菜单） | T1,T2 |
| T4 | IconRail + CategoryRail（折叠/记忆/动效/五态） | T1 |
| T5 | PortalShell 组装 + meta 接线 + 交流互动占位页上线（壳层首租户） | T3,T4 |
| T6 | 资料列表页剥壳迁入 + scope/category query 接线 | T5 |
| T7 | 棘轮基线收紧 + design-system「门户壳层格律」章节 | T6 |
| T8 | Windows 构建部署 19100 + 用户目验 + 回归清单 | T7 |

## 验收总门禁（T8 详述）

1. `npm run build:prod` + `npm run design:audit`（收紧后基线）绿；
2. 资料模块功能等价清单全过（列表/筛选/分类/上传/预览/收藏/我的上传/我的收藏/分页/排序/搜索）；
3. 首页与论坛/应用中心/应知应会/态势页逐页目检零变化；
4. **用户目验通过**（19100 真实环境，含折叠动效、悬停态、空态观感）——美观是验收项不是建议项；
5. 回退演练（终审修正语义）：摘除 resources 路由 meta.portalShell → 确认得到降级但可用的无壳列表页（列表/搜索/上传可用）；正式回退路径 = git revert 整支。

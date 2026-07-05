# 门户首页与工具即用样板 UI/UX 优化实施计划

> 关联设计稿：`docs/superpowers/specs/2026-07-05-portal-home-tools-ui-ux-sample-design.md`
> 目标页面：`plus-ui/src/views/portal/home`、`plus-ui/src/views/portal/tools`
> 日期：2026-07-05

## 1. 实施边界

本次实施只做第一批 UI/UX 样板页：

- 门户首页：保留长城背景和五张模块插画，优化信息层级、模块卡片、统计区、响应式和令牌使用。
- 工具即用：保留现有应用图标映射，优化页头、搜索、筛选、卡片栅格、主次按钮、空态和令牌使用。

不做：

- 不替换 `plus-ui/src/assets/portal/*.png`。
- 不改后端、数据库、接口契约。
- 不重构资料共享、服务论坛。
- 不处理当前未跟踪的 `required-knowledge` 相关文件。

## 2. 当前代码事实

- 首页入口：`plus-ui/src/views/portal/home/index.vue`。
- 首页组件：
  - `HomeTopbar.vue`
  - `ModuleGrid.vue`
  - `StatsBand.vue`
- 首页资产：
  - `portal-home-bg.png`
  - `home-logo.png`
  - `module-resource.png`
  - `module-tools.png`
  - `module-qa.png`
  - `module-hot.png`
  - `module-forum.png`
- 工具页入口：`plus-ui/src/views/portal/tools/index.vue`。
- 工具页组件：
  - `ToolsSidebar.vue`
  - `CategoryTabs.vue`
  - `AppCard.vue`
  - `DemandDialog.vue`
  - `MyDemandList.vue`
- 当前工具页标题仍写作“应用中心”，需要改为展示文案“工具即用”，不改代码标识。
- 当前工作树存在其他人/其他任务的未跟踪 `required-knowledge` 文件和路由相关改动，本实施不触碰、不提交这些文件。

## 3. 实施顺序

### 3.1 建立共享格式化工具

新增或更新 `plus-ui/src/utils/format.ts`：

- `formatStat(value)`：
  - `null` / `undefined` / `0` 返回 `—`。
  - 非零数字使用千分位。
  - 非数字字符串尽量安全回退。

用途：

- 首页统计带。
- 工具卡使用量、收藏量。

### 3.2 首页布局与令牌收敛

更新 `home/index.vue`：

- 保留背景图，调整遮罩透明度和背景定位。
- 将局部变量逐步映射到 `--ip-*` 令牌。
- 保留 profile/password 弹窗逻辑。

更新 `HomeTopbar.vue`：

- 保留 logo、天气/时间、用户菜单。
- 秒级刷新改为分钟级刷新。
- 字号、圆角、颜色、阴影迁移到 `--ip-*`。

更新 `ModuleGrid.vue`：

- 保留现有模块插画和注册表数据。
- 卡片高度收敛，减少过大留白。
- 栅格从固定五列改为自适应列，保证中等宽度不裁切。
- 卡片使用令牌化边框、圆角、阴影。
- 降低 hover 上浮幅度。

更新 `StatsBand.vue`：

- 接入 `formatStat`。
- 使用设计令牌收敛字号、圆角、阴影。
- 保持统计项可换行，不挤压。

### 3.3 工具即用布局与令牌收敛

更新 `tools/index.vue`：

- 展示文案从“应用中心”收敛为“工具即用”。
- 页头从重卡片改为轻表面。
- “需求反馈”保留唯一实心 primary。
- “搜索”改为 secondary 描边。
- app grid 改为 `repeat(auto-fill, minmax(320px, 1fr))`。
- 空态文案区分市场和收藏。
- 颜色、字号、圆角、阴影迁移到 `--ip-*`。

更新 `ToolsSidebar.vue`：

- 展示文案从“应用中心”收敛为“工具即用”。
- 保留 logo 和回首页入口。
- 侧栏颜色收敛到 `--ip-*` 与 `--ip-mod-appcenter-*`。

更新 `CategoryTabs.vue`：

- 保留分类和排序交互。
- 选中态、边框、字号、圆角迁移到设计令牌。

更新 `AppCard.vue`：

- 保留所有品牌 Iconify 映射。
- 兜底图标改为 appcenter soft 底 + appcenter base 字。
- 卡片等高，底部操作吸底。
- “立即使用”保持 secondary/tertiary 层级。
- 使用量/收藏量接入 `formatStat` 并弱化零值。

## 4. 验证计划

基础验证：

1. `cd plus-ui && npm run build:prod`
2. `cd plus-ui && npm run design:audit`
3. 如果审计指标下降，运行 `node ../deploy/scripts/design-audit.mjs --update-baseline`

截图验证：

1. 优先启动或复用本地前端服务。
2. 使用 `python3 deploy/scripts/ui-capture.py` 采集 `portal-home` 和 `portal-tools`。
3. 如截图因登录态跳转 `/login`，如实记录 auth 依赖，并保留 build/design audit 作为代码验证。

行为冒烟：

- 首页模块入口仍能跳转资料共享、工具即用、服务论坛。
- 工具页搜索、分类、排序、收藏、立即使用、需求反馈入口可用。
- 现有 `127.0.0.1` / `localhost` 工具 URL 主机替换逻辑不回归。

## 5. 预期提交

建议分两到三个提交：

1. `docs: 门户首页与工具即用样板实施计划`
2. `refactor: 首页样板布局与令牌收敛`
3. `refactor: 工具即用样板布局与令牌收敛`

如果代码改动较小，也可以将首页和工具页合并为一个实现提交。

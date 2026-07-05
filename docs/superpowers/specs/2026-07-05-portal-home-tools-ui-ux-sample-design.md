# 门户首页与工具即用样板 UI/UX 优化设计

## 背景

本次 UI/UX 优化采用“样板页先行”方式，不一次性重写整个门户。第一批样板页限定为：

- `portal/home` 门户首页。
- `portal/tools` 工具即用页面。

用户确认采用方向 1 的整体布局骨架，即“门户总览 + 稳定卡片栅格”，但必须保留当前界面已经认可的背景和图标资产。优化重点从“换风格”调整为“保留现有资产，重排信息层级，修正容器、间距、卡片、按钮和响应式问题”。

本设计必须遵守：

- `AGENTS.md`
- `docs/design/design-system.md`
- `docs/design/phase-b-tasks.md`
- `plus-ui/src/assets/styles/tokens.scss`

## 目标

- 保留首页长城背景和现有模块插画，提升首页第一屏的信息组织和可扫读性。
- 保留工具页已有应用图标映射，优化工具发现、筛选、卡片栅格和主次操作。
- 建立一套可扩散到资料共享、服务论坛等门户页的样板结构。
- 在样板范围内减少硬编码视觉值，优先迁移到 `--ip-*` 设计令牌。
- 通过构建、设计审计和截图回归证明改动可落地。

## 非目标

- 不替换 `plus-ui/src/assets/portal/portal-home-bg.png`。
- 不重生成或替换首页五张模块插画：
  - `module-resource.png`
  - `module-tools.png`
  - `module-qa.png`
  - `module-hot.png`
  - `module-forum.png`
- 不把工具页已有品牌图标全部改成字母块。
- 不新增后端接口、不改数据库 schema。
- 不处理资料共享、服务论坛、通知中心等非样板页面的完整重构。
- 不在本轮引入新的设计系统、字体体系或全新视觉资产包。

## 设计原则

1. **保留资产，重做秩序。** 背景、首页模块插画、工具品牌图标是当前界面的有效资产，本轮只调整它们所在的版式、容器和层级。
2. **样板优先于全站铺开。** 首页和工具页先形成可验证样板，再将规则扩散到其他门户页。
3. **令牌优先。** 颜色、字号、圆角、阴影优先使用 `--ip-*` 令牌；确有资产叠加需要的半透明白/黑遮罩可保留。
4. **工具页是工作入口，不是营销页。** 保持密度、搜索效率和卡片可比较性，避免大幅装饰化。
5. **每屏一个主行动。** 首页主行动由模块卡片承载；工具页保留“需求反馈”为唯一实心 primary，“搜索”和“立即使用”降为次级或文字层级。

## 首页样板设计

### 保留内容

首页继续使用：

- `portal-home-bg.png` 作为首屏背景。
- `home-logo.png` 作为平台标识。
- 五张 `module-*.png` 模块插画。
- `portal_module` 注册表驱动模块渲染，注册表不可用时保留现有默认模块兜底。
- 用户菜单、修改资料、修改密码、退出登录等现有交互。

### 布局结构

首页保持一屏门户感，但减少当前模块区过大留白和过强浮层感：

1. 顶部栏
   - 保留现有 logo、天气/时间、用户菜单。
   - 秒级时钟改为分钟级刷新，减少持续跳动噪音。

2. 门户总览区
   - 在背景图上保留轻遮罩，确保文字可读。
   - 平台标题、简短说明、核心服务统计或状态入口形成一个稳定总览区。
   - 不新增营销式 hero，不放大成外宣页面。

3. 模块入口区
   - 使用现有五张模块插画。
   - 卡片高度收敛，减少 340px 大卡在中等屏上的压迫感。
   - 卡片等高，标题与描述对齐，右下保留轻量箭头指示。
   - 取消或削弱过重阴影与过度上浮，改为边框 + `--ip-shadow-sm/md`。

4. 统计区
   - 统计带移到模块入口之后，作为服务概览补充。
   - 数字按设计系统零值策略处理：低价值 `0` 不直接形成“0 次”矩阵，后续可接入统一 `formatStat`。

### 响应式

- 大屏：背景完整保留，模块五列或自适应列，内容最大宽度对齐 `--ip-layout-max`。
- 1200-1460：模块卡片缩小但保持插画清晰。
- 768-1199：模块 2-3 列，统计区可换行。
- 768 以下：单列或双列，背景位置上移，避免主体内容被背景强视觉干扰。

## 工具即用样板设计

### 保留内容

工具页继续保留：

- `ToolsSidebar` 的门户侧栏与回首页入口。
- 当前 `AppCard` 中的 Iconify/simple-icons 品牌图标映射，如 Dify、Grafana、Redis、MinIO、Nginx、Gitea 等。
- 收藏、取消收藏、立即使用、需求反馈、通知入口、分页等现有交互。
- `normalizeToolUrl()` 对 `127.0.0.1` / `localhost` 的当前主机替换逻辑。

### 术语

当前页面标题仍有“应用中心”字样。样板实现时应按设计系统术语收敛为：

- 页面主标题：`工具即用`
- 收藏视图：`收藏工具`
- 空态：`暂无工具`

代码标识、路由、接口、数据库字段不因文案修改而重命名。

### 布局结构

1. 页面骨架
   - 保留左侧品牌导航栏 + 主区结构。
   - 主区使用标题/搜索/需求反馈/通知的紧凑工具条。
   - 页头从重卡片改为轻表面，避免页头、筛选、卡片三层都像卡片。

2. 搜索与行动
   - 搜索输入保留在顶部主工作流。
   - “需求反馈”为唯一实心 primary。
   - “搜索”降为 secondary 描边按钮，避免同屏两个实心主按钮。
   - 通知按钮保持图标按钮形态。

3. 分类与排序
   - `CategoryTabs` 保持现有交互。
   - 视觉上作为筛选条，不做过度装饰。
   - 选中态使用工具即用模块识别色 `--ip-mod-appcenter` 或 primary，行动色仍归 primary。

4. 应用卡片
   - 栅格改为自适应策略：`repeat(auto-fill, minmax(320px, 1fr))`，避免 1440 宽度下第三列裁切或列宽过窄。
   - 卡片等高，底部操作吸底。
   - 描述两行省略，标签最多三枚。
   - 保留品牌图标映射；只有没有图标时才使用统一兜底。
   - 兜底规则：`--ip-mod-appcenter-soft` 背景 + `--ip-mod-appcenter` 文字或线性图标，不使用随机彩色块。
   - “立即使用”建议为 secondary/tertiary 层级，不与“需求反馈”抢主行动。

5. 指标显示
   - 使用量、收藏量为 0 时弱化显示，可合并为一行弱信息。
   - 后续可接入统一 `formatStat(value)`：`0 -> "—"`，非零千分位。

### 空态与加载

本轮样板不强制完整落地五态组件套件，但应为后续 B7 预留结构：

- 列表加载中优先骨架屏，而不是长时间只显示转圈。
- 空列表文案应明确下一步：
  - 工具市场：`暂无工具，稍后再试或提交需求反馈`
  - 收藏工具：`暂无收藏工具`
- 错误状态后续统一进入 `PortalState` 套件，不在当前样板里分散实现。

## 资产策略

### 首页资产

必须保留：

- `plus-ui/src/assets/portal/portal-home-bg.png`
- `plus-ui/src/assets/portal/home-logo.png`
- `plus-ui/src/assets/portal/module-resource.png`
- `plus-ui/src/assets/portal/module-tools.png`
- `plus-ui/src/assets/portal/module-qa.png`
- `plus-ui/src/assets/portal/module-hot.png`
- `plus-ui/src/assets/portal/module-forum.png`

允许调整：

- 背景图 position、size、叠加遮罩透明度。
- 模块插画显示尺寸。
- 模块卡片容器、阴影、边框、间距。

禁止：

- 重生成背景图。
- 用新插画替换现有五个模块图。
- 把首页模块图标降级为纯 Iconify 图标或字母块。

### 工具页图标

保留 `AppCard.vue` 里的品牌图标映射。样板只调整：

- 图标容器尺寸。
- 图标背景、圆角、边框。
- fallback icon 的统一样式。

## 文件范围

第一批实现预计触碰：

- `plus-ui/src/views/portal/home/index.vue`
- `plus-ui/src/views/portal/home/components/HomeTopbar.vue`
- `plus-ui/src/views/portal/home/components/ModuleGrid.vue`
- `plus-ui/src/views/portal/home/components/StatsBand.vue`
- `plus-ui/src/views/portal/tools/index.vue`
- `plus-ui/src/views/portal/tools/components/ToolsSidebar.vue`
- `plus-ui/src/views/portal/tools/components/CategoryTabs.vue`
- `plus-ui/src/views/portal/tools/components/AppCard.vue`

可选触碰：

- `plus-ui/src/utils/format.ts`：如果本轮顺手落地 `formatStat`。

不触碰：

- 后端模块。
- 数据库脚本。
- `resources` / `forum` 页面。
- 当前未跟踪的 `plus-ui/src/views/portal/required-knowledge/`。

## 验收标准

实现完成前必须提供以下输出：

1. `cd plus-ui && npm run build:prod`
2. `cd plus-ui && npm run design:audit`
3. 若设计审计指标下降，运行：
   - `node ../deploy/scripts/design-audit.mjs --update-baseline`
4. 截图回归：
   - `python3 deploy/scripts/ui-capture.py`
   - 至少检查 `portal-home` 与 `portal-tools`
5. 行为冒烟：
   - 首页模块点击仍能进入资料共享、工具即用、服务论坛。
   - 工具页搜索、分类、排序、收藏、立即使用、需求反馈入口不回归。

## 风险与处理

- **本地截图可能因登录态失败跳到 `/login`。**
  处理：先区分代码验证和页面截图验证；需要在相同 origin 完成登录后再截图。

- **设计审计不一定一次性明显下降。**
  处理：样板页优先迁移本次触碰文件内的硬编码色值、字号、圆角和阴影；不为了指标改动非样板页。

- **工具页品牌图标颜色与令牌色可能冲突。**
  处理：品牌图标本身作为内容资产保留；容器、边框、文字、fallback 仍走设计令牌。

- **首页背景图较强，可能影响文字可读性。**
  处理：只调整遮罩和内容位置，不替换背景图。

## 后续扩散

样板验收后，再按 Phase B 推进：

1. 将首页和工具页形成的布局、按钮、卡片规则扩散到资料共享。
2. 将论坛页按同一门户骨架收敛。
3. 再做图标统一、术语收敛、五态组件套件。
4. 最后处理首页模块插画是否需要整套重制；当前用户已明确希望保留现有插画，因此不作为近期任务。

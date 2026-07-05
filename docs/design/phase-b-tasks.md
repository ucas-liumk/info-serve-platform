# 设计体系 Phase B · 任务卡集（提示词直接粘贴用）

> 使用说明：每张卡的"提示词"整段复制给执行模型即可。执行工具**必须能运行命令**（构建/审计是硬门禁，纯聊天模型出的补丁不算完成）。
> 推荐顺序：B1–B4 可并行（文件不重叠）→ 全部合并后做 B6 → B5 → B7；B8 独立随时可做。
> 每卡一个分支一个执行者；合并 main 由协调者完成，执行模型 push 分支后停手。

---

## B1 · 首页令牌迁移（home + 门户布局层）

```text
你在 /Users/macmini/info-serve 仓库工作（Windows 侧路径 E:\gallant-dev\active\info-serve）。
开工前必读并严格遵守：根目录 AGENTS.md（开发规约，红线）、docs/design/design-system.md（设计正本 v1.0）、plus-ui/src/assets/styles/tokens.scss（唯一令牌来源）。

分支纪律：git fetch origin && git switch -c refactor/design-b1-home。完成后 push 该分支并停手；禁止触碰 main、禁止合并、禁止改动本卡范围外的文件。

任务：把以下文件 <style> 中的硬编码设计值全部迁移到 --ip-* 令牌：
  plus-ui/src/views/portal/home/index.vue 及 home/components/*.vue
  plus-ui/src/layout/portal/**（含 PortalNotificationBell）

迁移规则：
1) 颜色：每个硬编码 hex/rgba 替换为视觉最接近的令牌（var(--ip-...)）；蓝色行动/选中/链接一律 primary 系；文字灰蓝按 neutral 阶归并；禁止发明新颜色、禁止在 tokens.scss 里新增令牌。半透明白/黑遮罩（rgba(255,255,255,x) 类）允许保留。
2) 字号：全部归并到 8 档字阶令牌（--ip-font-*）；字重 800/850/900 一律降为 700。
3) 圆角归 4 档（--ip-radius-*），box-shadow 归 3 档（--ip-shadow-*）。
4) 页面内旧的局部 CSS 变量（--portal-* 等）：值改为引用 --ip-* 令牌，或直接替换使用处后删除。
5) 行为与布局完全不变，只做视觉值收敛；顺手修复：首页秒级时钟改为分钟级刷新（去掉每秒跳动噪音）。

验收（全部必过，结果里贴输出）：
  a) cd plus-ui && npm run build:prod 绿；
  b) npm run design:audit —— 先跑一次记录当前值，迁移完成后运行 node ../deploy/scripts/design-audit.mjs --update-baseline 下调基线，四项指标必须全部严格小于开工前（贴前后数字）；
  c) python3 deploy/scripts/ui-capture.py（UI_CAPTURE_OUT=output/ui-capture-b1）截取 portal-home 前后对比，页面结构不得变化。

提交：refactor: 首页与门户布局令牌迁移（B1）。禁止署名尾注。
交付报告：改动文件清单、四项指标前后对比、构建输出尾行、遗留无法归并的值及理由。
```

## B2 · 工具页令牌迁移 + accent 归位

```text
（同 B1 的必读文件与分支纪律）分支：refactor/design-b2-tools

任务范围：plus-ui/src/views/portal/tools/index.vue 及 tools/components/*.vue

在 B1 的迁移规则（颜色/字阶/圆角/阴影/局部变量五条）基础上，追加本页专项：
1) 本页所有金色/琥珀色 accent（侧栏选中态、品牌卡装饰、标题竖条等）统一替换为模块识别色令牌 --ip-mod-appcenter / -soft / -border；识别色仅用于识别场景，所有按钮/链接一律 primary（同屏只允许一个实心 primary 按钮：保留"需求反馈"为 primary，"搜索"降为 secondary 描边）。
2) 修复 1440 宽度下第三列卡片被裁切：卡片栅格改为 repeat(auto-fill, minmax(320px, 1fr)) 类策略，内容区不得横向溢出。
3) 卡片等高（grid/flex 拉伸），底部"立即使用"操作区吸底对齐。
4) 应用 LOGO 无图时的字母块统一规则：--ip-mod-appcenter-soft 底 + --ip-mod-appcenter 色首字母，禁止随机彩色。

验收与交付同 B1（audit 基线四项必须下降；截图对比 portal-tools）。
提交：refactor: 工具页令牌迁移与布局修复（B2）
```

## B3 · 论坛页令牌迁移 + accent 归位

```text
（同 B1 的必读文件与分支纪律）分支：refactor/design-b3-forum

任务范围：plus-ui/src/views/portal/forum/index.vue 及 forum/components/*.vue

在 B1 迁移规则基础上，本页专项：
1) 全页赤陶红/橘红体系统一替换为 --ip-mod-forum / -soft / -border（识别场景），行动元素（搜索/发起话题/回复按钮等）一律 primary；同屏一个实心 primary（保留"发起话题"），"搜索"降为 secondary。
2) 话题卡右侧"浏览/回复/点赞"统计块改为纯文本+图标样式（不再长得像可点按钮）。
3) 品牌卡右上角的"划线"装饰（三个模块页同款视觉 bug）在本页移除。
4) 状态徽章（置顶/已关闭/板块）统一 tag 规格：soft 底 + base 字 + border 描边，高 22px。

验收与交付同 B1（截图对比 portal-forum 与 portal-forum-detail）。
提交：refactor: 论坛页令牌迁移与组件形制统一（B3）
```

## B4 · 资源页令牌迁移 + 卡片修复

```text
（同 B1 的必读文件与分支纪律）分支：refactor/design-b4-resources

任务范围：plus-ui/src/views/portal/resources/**（index/preview 及 components/*，PdfPreviewer 只迁移颜色值不动逻辑）

在 B1 迁移规则基础上，本页专项：
1) 绿色 accent 统一替换为 --ip-mod-resources / -soft / -border；行动元素一律 primary，同屏一个实心 primary（保留"上传资料"），"搜索"降为 secondary。
2) 资源卡布局修复：缩略图与操作按钮不得重叠；标题至多两行省略（-webkit-line-clamp:2），禁止孤字断行；预览/下载/收藏三按钮统一形制（primary 文字钮 / secondary / 图标钮的层级按规范 §7.1）。
3) 品牌卡"划线"装饰移除（同 B3 第 3 条）。

验收与交付同 B1（截图对比 portal-resources）。
提交：refactor: 资源页令牌迁移与卡片修复（B4）
```

## B5 · 图标体系统一（门户侧）

```text
（同 B1 的必读文件与分支纪律；前置：B1–B4 已合并，开工先 git fetch 最新 main）分支：refactor/design-b5-icons

任务：门户侧（views/portal/** 与 layout/portal/**）图标统一为 Iconify 单一来源。
1) 盘点现用图标：@element-plus/icons-vue 组件、内联 SVG、图片图标；逐一替换为 Iconify（项目已配 unplugin-icons，用法 import IconX from '~icons/<集合>/<名称>'；统一选 lucide 或 tabler 一个集合，全部线性风格）。
2) 尺寸只用 16/20/24/32；颜色只用 currentColor 继承或令牌。
3) Element Plus 组件内部图标（如 el-input prefix）保持不动；本卡只处理业务代码直接引用的图标。
4) 首页五张模块插画本卡不动（B8 单独处理），但插画加载失败的兜底改为"模块 soft 底 + Iconify 模块图标"。

验收：build:prod 绿；design:audit 不升；全站截图（ui-capture.py）逐页确认无缺图标、无风格混排。
提交：refactor: 门户图标统一 Iconify（B5）
交付报告：替换映射表（原图标 → 新图标名）、截图核对结论。
```

## B6 · 命名收敛 + 死入口清除

```text
（同 B1 的必读文件与分支纪律；前置：B1–B4 已合并）分支：fix/design-b6-naming-deadlinks

任务 A —— UI 文案按术语表收敛（术语正本：docs/architecture/bounded-contexts.md §2）：
  全局检查 plus-ui/src 用户可见文案，执行以下替换（仅改展示文案，禁止改路由/API/代码标识/数据库值）：
  「应用中心」→「工具即用」（工具页主标题等；管理后台菜单名"门户应用管理"保持）
  「资源共享」→「资料共享」；涉及资料的按钮/提示中「资源」→「资料」（如"我的资源"→"我的资料"）
  完成后 grep 复查：门户界面不得再出现"应用中心/资源共享"字样。

任务 B —— 死入口清除：
1) 新建 deploy/updates/0.3.4-menu-cleanup.sql（PG ry-cloud）：删除 sys_menu 中四个死顶级菜单及其子孙——PLUS官网(4)、测试菜单(5)、工作流(11616)、我的任务(11618)；用 parent_id 递归或按已知 id 范围删除，附回滚说明注释；同时删除对应 sys_role_menu 引用。登记 deploy/updates/MANIFEST.md（0.3.4 待投放行）。
2) 新装机同步：在 source/script/sql/postgres/postgres_portal_kernel.sql 末尾追加同样的 DELETE（幂等），并注明"清理上游演示菜单"。
3) 登录页"忘记密码?"假门：plus-ui 登录页移除该链接（无对应流程）。
4) 本地验证：将 SQL 应用到本地 PG（docker exec infosys-ruoyi-cloud-plus-postgres psql -U ruoyi -d ry-cloud -f ...），重新登录后台确认侧栏不再出现四个死菜单，浏览器无 404/空白页。

验收：build:prod 绿；design:audit 不升；后台截图（admin-home）确认侧栏干净；grep 复查文案零残留。
提交（两个）：fix: 门户文案按术语表收敛（B6a）；fix: 清除上游死菜单与登录假门（B6b）
```

## B7 · 五态组件套件（内核级）

```text
（同 B1 的必读文件与分支纪律；前置：B1–B4 已合并）分支：feat/design-b7-states

任务：按规范 §7.4 落地五态套件并接入门户。
1) 新建 plus-ui/src/components/PortalState/{EmptyState.vue, ErrorState.vue, SkeletonCards.vue, SkeletonTable.vue}：
   EmptyState(props: icon?, title, hint?, actionText?, @action)——线性插图风格（Iconify 大图标 + soft 底圆形），文案居中；
   ErrorState(props: message, @retry)；
   SkeletonCards(props: count, 卡片骨架)、SkeletonTable(rows)。全部只用 --ip-* 令牌。
2) 接入（行为不变，只替换"空白/转圈"）：
   论坛列表空态：「暂无话题，发起第一个讨论」+ 主按钮触发发帖对话框；
   资料列表空态：「暂无资料，上传第一份资料」；工具列表空态、我的资料/我的反馈空态；
   列表加载中一律骨架屏（>300ms 出现）。
3) 零值策略：新建 plus-ui/src/utils/format.ts 的 formatStat(value)（0 → "—"，千分位），首页统计带与工具卡"使用/收藏"计数接入；工具卡两个 0 计数合并为一行弱化显示。
4) 首页"服务总览"数字全部走 formatStat；单分类图表规则本卡不涉及（后台 Phase C）。

验收：build:prod 绿；design:audit 不升；把某分类筛到空来实拍空态截图（ui-capture 手动补拍该状态）。
提交：feat: 门户五态组件套件与零值策略（B7）
```

## B8 · 首页模块插画重制（图像模型 Brief）

```text
用途：门户首页五张模块卡插画，必须一次成套生成，风格完全一致。生成后交协调者审核压缩入库。

统一约束（每张都带上）：
现代企业软件插画，等距 3D 轻拟物风格，统一左上 45° 柔光光源，圆润几何体块，
主体居中占画面 55%，纯白到极浅蓝(#F0F6FF)径向渐变背景，无文字无水印，
配色严格限定：深品牌蓝 #1260E8、浅蓝 #8FB7FF、白色，外加每张指定的一个模块强调色，
细节克制、无颗粒噪点、无照片元素，输出 1024x1024 PNG。

五张主体（模块强调色）：
1 资料共享：整齐的文件夹与文档层叠，一份文档轻微浮起（强调色 #0E7A5F 青绿）
2 工具即用：组合工具箱与几何扳手/齿轮，模块化拼插感（强调色 #B45309 琥珀）
3 智能问答：对话气泡与神经网络节点连线，气泡内一个闪光点（强调色 #5B4FC9 蓝紫）
4 时事热点：上升的趋势面板与信号波纹，克制的火花点缀（强调色 #C11B4B 绯红，禁止写实火焰）
5 服务论坛：围合的多人气泡与圆桌意象，两三个抽象人形（强调色 #C0452C 赤陶）

验收标准：五张并排看光源/透视/饱和度/圆角语言完全一致；任何一张不达标则整组重生成该张直至成套。
```

---

## 协调者备忘（不粘贴给执行模型）

- 每卡合并前跑一遍卡内验收命令复核，再合 main 并同步 Windows；B1–B4 各自合并后立即让下一卡 fetch。
- audit 基线只降不升：每次合并后基线文件应变小，出现回升立即打回。
- B6b 的菜单 SQL 投放 UTM 时并入 0.3.4 更新包（MANIFEST 已有流程）。

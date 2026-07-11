# 资料共享 · 栏目→分类两级体系设计（左栏知网式分组树）

状态：v2 定稿（2026-07-10；v1 经四路代码侦察+批判核对修订，锚点均含真实文件:行号）
背景：门户壳层试验已封存（tag `experiment/portal-shell-v1`），本设计是**对现有原版页面的小步微调**，参照知网左栏分组树。样机存档 `.superpowers/brainstorm/95620-1783684824/content/section-category-tree.html`。

## 1. 需求与裁决记录（用户确认项）

- 资料页左栏承载**两级内容分类体系**：一级「栏目」为分组头，二级「分类」为组内条目。类型/时间/大小筛选**留在工具条不动**（用户明确修正）。
- 分类 checkbox **多选并集** + 顶部已选 chip 单撤 + 「清除筛选」；栏目头 checkbox 整组勾选（三态含半选）；栏目头可折叠。
- 分类条目带计数，计数=**分面语义**（响应关键词+工具条筛选，不含分类维度自身）。
- 初始数据：默认栏目**「综合资料」**承接既有 3 分类（政策制度/技术文档/常用模板），后续栏目规划管理员后台自调。
- 管理后台分类管理页升级为**栏目/分类两级树管理**，本轮必做。

## 2. 数据模型（schema 变更，实为"四件套"）

`info_resource_category`（建表 `source/script/sql/postgres/postgres_info_service.sql:6-24`）增加 `parent_id int8 DEFAULT NULL`：
- `parent_id IS NULL` = 栏目；`parent_id = <栏目id>` = 分类。两级封顶，服务层校验禁止三级。
- 资料（`info_resource.category_id`）只允许挂二级分类；栏目不直接挂资料。
- 默认栏目：**固定 category_id=300000，code `general`，名「综合资料」**（实施时核实 300000 无冲突；既有行 300001-300003）。code 与分类共用部分唯一索引 `uk_info_resource_category_code (category_code) WHERE del_flag='0'`（:23），服务层唯一校验须跨两级。
- **四个动作**：①改源种子（建表+种子行加 parent_id、插栏目行）；②`deploy/scripts/generate-initdb.py` 重新生成 `deploy/initdb-postgres/dumps/` 副本并 diff 校验；③增量 `deploy/updates/0.3.7-resource-category-tree.sql`（幂等：`ADD COLUMN IF NOT EXISTS` + 栏目行 `ON CONFLICT DO NOTHING` + 孤儿分类兜底 `UPDATE ... SET parent_id=300000 WHERE parent_id IS NULL AND category_id<>300000`）；④MANIFEST 登记（**版本定 0.3.7**——0.3.6 行已被批次 A Nacos 配置占用且版本待核定，MANIFEST.md:30/44 装包规则要求版本包完整，故错开）。
- 租户：`info_resource_category` 在租户忽略清单（application-common.yml:276），无租户列；`info_resource` 有 tenant_id——手写聚合 SQL 时租户插件对 resource 侧的自动条件需实施时验证。

## 3. 后端（模块 `source/ruoyi-modules/ruoyi-portal-resources`，Dubbo/MQ 契约零触碰）

1. **新增门户分类树接口** `GET /portal/resources/category-tree`：入参 keyword、previewType（兼容 fileType 合流，同 InfoResourceServiceImpl.java:130 语义）、uploadedWithin、sizeRange（枚举同现列表 :661-689）；**不含 categoryCode 自身；scope 固定 public 语义；sort 不参与**。返回 `[{categoryId,categoryCode,categoryName,children:[{...,resourceCount}]}]`（沿用现 Vo 命名惯例）。计数**一次 GROUP BY category_id 聚合**（禁止沿用 fillResourceCount 的 N+1，:42-47）；两级均过滤 status='0'，栏目停用则整组不出。
2. **平铺旧接口形状不变**：`GET /portal/resources/categories`（上传弹窗消费，ResourceUploadDialog.vue:9,117）与管理端 `/resource/category/options`（资料编辑下拉，admin/resources/resource/index.vue:216）均改为**只返回二级分类**，字段结构不动——保证资料只能挂二级，旧消费方零改动。
3. **列表接口 categoryCode 多值**：逗号分隔 → 解析为 code 集合；保留 'all' 哨兵跳过（isActiveFilter :657-659）与「无一命中 → eq categoryId -1L 空集」语义（:118-124）；部分命中 → IN(命中的 categoryId 集)。单值/缺省行为完全向后兼容（现门户前端自身就是单值调用方）。
4. **管理端**：新增 `GET /resource/category/treeList`（全量不分页，供树表；旧分页 /list 保留不动）；CRUD 支持 parentId（建栏目=空；建分类=必选栏目且父必须是栏目→两级封顶校验）；**净新增删除校验**（现 deleteWithValidByIds 无任何校验 :96-98）：删栏目须无子分类（含停用未删的），删分类须无挂接资料（未删的），违者报友好错误。复用现有权限串 `infoservice:resourceCategory:*`，**菜单 SQL 零改动**（menu 3020 及按钮 3021-3024 原样复用）。
5. **单元测试**（模块内新增，保持无 Spring 上下文纯单测风格，范本 ResourceConvertListenerTest）：树组装、分面计数入参映射、多值 categoryCode 解析（all/单值/多值/无命中）、两级封顶校验、删除校验。门禁命令（实测 3-10 秒）：`cd source && JAVA_HOME=$(/usr/libexec/java_home -v 17) '/Applications/IntelliJ IDEA.app/Contents/plugins/maven/lib/maven3/bin/mvn' -o test -DskipTests=false -pl ruoyi-modules/ruoyi-portal-resources -am`。

## 4. 前端

**门户（三层链路同改：index.vue:3-8 → ResourceSidebar.vue:16-21 → ResourceFilterPanel.vue）**：
- `ResourceFilterPanel` 重写为两级分组树：栏目组头（折叠 chevron + 三态 checkbox + 组计数=子分类求和）、分类条目（checkbox + 计数）、底部「清除筛选」。**样式令牌化是净新增迁移**（现组件全硬编码色 :49-135）：全部用 `--ip-*`/`--ip-motion-*`（tokens.scss:8-81，main 已有）；不新增任何硬编码色/字号/圆角（design:audit 棘轮，基线 hardcodedHex 121/rgba 73/fontSize 21/radius 14），删旧硬编码后跑 `--update-baseline` 下调并提交基线文件。
- 状态模型：`selectedCategories: string[]`，**空数组=全部**（请求省略 categoryCode）；多值传逗号串。页面标题（index.vue:184-185 现依赖单值）重定义：空→「全部资源」，单选→分类名，多选→「已选 N 个分类」。chip 条渲染于结果区顶部，单撤销；「清除筛选」清空数组。
- 计数联动（净新增，现 loadCategories 仅 onMounted/上传后触发 :450-453,386-392）：关键词/工具条变化 → **并行**发列表 + category-tree 两请求（同筛选参数、不含 categoryCode）；**勾选分类只刷列表不刷计数**（标准分面语义）。上传成功后两者都刷。
- 纯函数抽取 + vitest：树三态归并（组勾选/半选计算）、chip 增删、逗号串编解码；`package.json` 补 `"test": "vitest run"` 脚本（vitest 4.0.18 已在 devDependencies，现无 test 脚本）。**npm 工作流，禁 pnpm i**。
- 上传弹窗/工具条/卡片列表零改动（平铺接口形状不变保证）。

**管理后台（`views/admin/resources/category/index.vue` 整页范式替换）**：照部门管理页范式（dept/index.vue:44-47 row-key+tree-props、handleTree :211、展开折叠 :248-258、行内新增传父 :67-69）：全量树表（吃 treeList 接口，不分页）、**去掉 selection 批量删改行内删除**（dept/menu 惯例）、栏目行内「新增分类」按钮、弹窗增「上级栏目」选择（建栏目=空/建分类=必选，el-tree-select check-strictly 或栏目下拉）、行内 el-switch 停用保留（停用不级联，门户侧按 §3.1 过滤）。`types.ts` ResourceCategory/ResourceCategoryForm 加 parentId/children；`admin.ts` 补 treeList API。

## 5. 不变量与验收

- 工具条、卡片列表、上传/预览/收藏、我的资源抽屉及其他页面零变化；菜单/权限零变化；Nacos 配置零变更。
- 组合筛选正确性：多选并集、栏目整组勾选=其全部子分类、chip 撤销与清除全部、计数与列表总数一致性抽查、'all'/单值旧语义回归。
- 门禁：后端模块 test（§3.5 命令）；前端 `npm run build:prod` + `npm run design:audit`（棘轮不升、删旧硬编码后下调基线）+ `npm test`（vitest）。
- 发布面：portal-resources 镜像 + 前端 dist + 0.3.7 增量 SQL（PG ry-cloud）。**改完真机截图比对样机，用户认可后关单。**

## 5.5 增补：资料多分类（2026-07-10 用户新增需求）

一份资料可属于**多个分类**（分类可跨栏目，"属于多个栏目"由此传递成立；资料仍只挂二级分类）。

- **数据模型**：新关联表 `info_resource_category_link(resource_id, category_id, tenant_id, create_time)`，PK(resource_id, category_id) + category_id 索引；**带 tenant_id 列**（避免动 Nacos 租户忽略清单，插入时随资料租户自动填充）。`info_resource.category_id` 保留为**主分类**（多选的第一个，供卡片徽标/旧展示路径），关联表为筛选/计数/删除守卫的唯一事实源。增量 `0.3.8-resource-multi-category.sql`：建表 + 从 category_id 回填（ON CONFLICT DO NOTHING，幂等）+ MANIFEST 登记；种子同步。
- **后端**：Bo 加 `categoryIds`（数组，与旧 categoryId 兼容：只传单值视为 [单值]）；新增/修改资料=逐个校验可用 + category_id 取首个 + 事务内整替关联行。列表 categoryCode 筛选改 `EXISTS(关联表 IN 命中分类)`（并集；'all'/-1L 语义保留）；分面计数与管理树计数改 join 关联表 COUNT(DISTINCT resource_id)（一份资料在其每个分类下都计数，标准分面）；删除守卫计数改关联表（跨租户不变）；Vo 加 categoryIds（编辑回显）。
- **前端**：上传/编辑弹窗分类改**按栏目分组的多选下拉**（el-option-group=栏目，选项=分类，必选≥1）；管理端资料编辑同步多选；卡片徽标仍显示主分类名。
- **验证**：上传一份挂两个分类的文件 → 在两个分类勾选下均可筛出（E2E）。前置依赖：OSS endpoint 修复（否则上传本身不可用，见遗留基建问题）。

## 5.6 增补：预览页改版——WPS 式阅读器 + 右栏功能区（2026-07-11 用户定稿）

原型 v3 经用户确认（存档 `.superpowers/brainstorm/95463-1783789992/content/studio-panel-v2.html`）。本轮范围=左侧 WPS 化 + 右栏功能区框架接入「我的笔记」「交流互动」；OCR/朗读/摘要/导图为「即将上线」占位磁贴（配置驱动，慢慢扩展）；产出库与 `info_resource_artifact` 表**留待下轮**（裁决已定：产出全员共享——谁先生成大家复用）。

**左侧（PdfPreviewer.vue，基于 EmbedPDF 插件正门 API，禁新增 shadow DOM hack）**：
- 新增 WPS 式视图工具条：`✋拖拽`（pan 插件 toggle，再点恢复选择）、`连页`（spread=none）、`单页`（spread=none + fitPage 缩放，就近语义映射并注释说明）、`双页连续`（spread=odd）、`左旋/右旋`（rotate 插件）；按钮带 active 态。
- 新增底部翻页条：⏮ ◀ `当前页/总页` ▶ ⏭（scroll/viewport 插件页码状态订阅 + scrollToPage）。
- 内建工具栏保留（缩放/搜索/缩略图侧栏继续用内建 UI）；`PDFViewerExpose.registry`（Promise<PluginRegistry>）驱动，插件 id/能力接口从 `@embedpdf/plugin-{pan,spread,rotate,zoom,scroll,viewport}` 的 d.ts 现场核实。

**右栏（ResourcePreviewContextPanel.vue 整页重写为「功能区」）**：
- 题头「功能区」+ ⓘ 弹层（资料信息 dl + 阅看记录列表收编于此）。
- 磁贴 2 列网格，**配置驱动**数组 `[{key,name,icon,tone,status:'active'|'soon'}]`：我的笔记/交流互动=active，OCR 识别/语音朗读/智能摘要/思维导图=soon（不可点、「即将上线」角标）。
- 工作区默认=**交流互动**（公开笔记流 + 发布框，发布即公开；我的公开条目可编辑/删除）；「我的笔记」=编辑器（默认私有，保留公开开关）+ 我的列表，条目操作：编辑/删除/**分享到交流互动**（visibility→public）/**取消分享**（→private）——全部走现有 notes API（my/public/post/put/delete），后端零改动。
- 磁贴粉彩底色以 **tokens.scss 新增 --ip-tile-* 令牌**实现（该文件不在 design:audit 扫描面，组件内零新硬编码色）。
- 门禁：vitest + design:audit 棘轮不升 + build + eslint；真机截图对照原型验收。

## 6. 决策记录

- [x] 左栏=栏目→分类两级树；类型/时间/大小留工具条（2026-07-10 用户修正确认）
- [x] 多选+chip；栏目头三态整组勾选（用户确认）
- [x] 默认栏目「综合资料」（id 300000/code general）承接存量分类，后台自调（用户确认）
- [x] 单表 parent_id 自引用，两级封顶；资料只挂二级
- [x] 新增树接口，平铺旧接口只出二级、形状不变（上传弹窗/资料编辑下拉零改动）
- [x] 多值解析保留 'all' 哨兵与 -1L 空集语义；空选=全部
- [x] 计数分面语义：勾选分类不刷计数；scope 固定 public；GROUP BY 一次聚合
- [x] 增量版本 0.3.7（0.3.6 已被批次 A 占用）；种子"四件套"（源+dump 再生成+增量+MANIFEST）
- [x] 管理页照 dept 树表范式整页替换，去批量删；菜单/权限串零改动
- [x] 删除校验净新增：栏目须空、分类须无资料；停用不级联
- [x] T4 审查修复（2026-07-10）：①0.3.7 幂等改「create_time 水位线」方案（重放不波及新建栏目，仍保留孤儿兜底）+ INSERT 双重防冲突；②删除/升级守卫计数跨租户（TenantHelper.ignore，分类是共享字典而资料带租户列）；③update 的 parent_id 经 wrapper 显式落库（NOT_NULL 策略下 null 会被跳过），分类升栏目须无挂接资料；④编码格式校验：'all' 保留、禁逗号/空白（前后端双侧）；⑤停用栏目下的分类在平铺接口隐藏且禁止挂资料（与 C1 对齐）；⑥管理树资料数口径=全部未删（与删除校验一致）；⑦前端竞态序号防护、已提交关键词与分面口径对齐、越界空页钳回、组勾选键盘可用、树刷新失败保留旧数据。多值解析加 50 条/80 字符上限。LIKE 通配符不转义（与列表现行为一致，保持口径统一）。

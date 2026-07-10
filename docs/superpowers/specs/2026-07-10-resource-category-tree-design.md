# 资料共享 · 栏目→分类两级体系设计（左栏知网式分组树）

状态：定稿（2026-07-10，样机与两项裁决经用户确认；样机存档 `.superpowers/brainstorm/95620-1783684824/content/section-category-tree.html`）
背景：门户壳层试验已封存（tag `experiment/portal-shell-v1`），本设计是**对现有原版页面的小步微调**，参照知网左栏分组树。

## 1. 需求与裁决记录

- 资料页左栏承载**两级内容分类体系**：一级「栏目」为分组头，二级「分类」为组内条目；每个栏目含多个分类。类型/时间/大小筛选**留在工具条不动**（用户明确修正）。
- 分类选择语义：**checkbox 多选并集** + 顶部已选 chip 单撤 + 「清除筛选」；栏目头 checkbox 可整组勾选（含半选态）；栏目头可折叠。
- 分类条目带计数（右对齐灰字，选中变蓝）；计数响应关键词与工具条筛选（标准分面语义）。
- 初始数据：建默认栏目**「综合资料」**，既有 3 分类（政策制度/技术文档/常用模板）归入；后续栏目规划由管理员在后台自行调整。
- 管理后台：分类管理页升级为**栏目/分类两级树管理**（栏目增删改、分类归属调整）——栏目必须可维护，属本设计必做部分。

## 2. 数据模型（schema 变更，三件套强制）

单表自引用：`info_resource_category` 增加 `parent_id int8 DEFAULT NULL`。
- `parent_id IS NULL` = 栏目（一级）；`parent_id = <栏目id>` = 分类（二级）。层级固定两级，禁止分类再挂子级（服务层校验）。
- 资料（`info_resource.category_id`）**只允许挂分类**（二级）；栏目不直接挂资料。
- 迁移：新增栏目行「综合资料」（code `general`），既有全部分类行 `parent_id` 指向它。
- 三件套：改种子 `postgres_info_service.sql` + 新增 `deploy/updates/0.3.6-resource-category-tree.sql` + `MANIFEST.md` 登记。`info_resource_category` 已在租户忽略清单，无租户列改动。

## 3. 后端（portal-resources 服务，Dubbo/MQ 契约零触碰）

1. **分类树接口**：现门户分类接口升级为返回两级树并带计数——`GET /portal/resources/categories` 返回 `[{栏目{code,name}, children:[{code,name,count}]}]`；入参接受与列表相同的筛选参数（keyword/previewType/uploadedWithin/sizeRange，**不含 categoryCode 自身**），count 按这些约束聚合（分面语义）。旧调用（无参）兼容：返回全量计数。
2. **列表接口多值分类**：`categoryCode` 支持逗号分隔多值 → IN 查询；单值/缺省行为完全向后兼容。
3. **管理端**：分类 CRUD 支持 parent_id（建栏目=parent_id 空；建分类=必选栏目）；删除栏目须为空（有子分类则拒绝）；返回树形列表接口供管理页。
4. 单元测试：树组装/计数聚合/多值解析/两级校验（surefire 模块内）。

## 4. 前端

**门户（`views/portal/resources/`，基于 main 原版页面）**：
- `ResourceFilterPanel` 重写为两级分组树面板：栏目组头（折叠 chevron + 组 checkbox 三态 + 组计数=子分类求和）、分类条目（checkbox + 计数）、底部「清除筛选」；样式全 `--ip-*` 令牌，折叠动效 `--ip-motion-*`，视觉克制对齐知网（细分割线、蓝选中态）。
- 页面筛选状态改多值（`selectedCategories: string[]`），列表请求传逗号串；结果区顶部 chip 条（单撤销）；工具条与其余布局**零改动**。
- 计数刷新时机：关键词/工具条筛选变化后随列表一起刷新（同一批请求）。

**管理后台（`views/admin/resources/category/`）**：升级为两级树表（RuoYi tree-table 惯例）：栏目行可展开、行内新增子分类、编辑/停用/删除；分类行可改归属栏目。

## 5. 不变量与验收

- 工具条、卡片列表、上传/预览/收藏等其余功能零变化；其他页面零变化。
- 组合筛选正确性：多选并集、栏目整组勾选=其全部子分类、chip 撤销与清除全部、计数与列表总数一致性抽查。
- 兼容性：旧单值 categoryCode 调用方（若有）不受影响；未归栏目的分类不应存在（迁移兜底：任何孤儿分类归入默认栏目）。
- 门禁：前端 build:prod + design:audit（棘轮不升）+ vitest；后端模块 `-DskipTests=false test`；**改完先真机截图比对样机，用户认可后关单**。
- 发布：schema 走三件套；Nacos 配置无变更；仅 portal-resources 服务镜像 + 前端 dist 需要重建部署。

## 6. 决策记录

- [x] 左栏=栏目→分类两级树；类型/时间/大小留工具条（2026-07-10 用户修正并确认）
- [x] 多选+chip；栏目头三态整组勾选（用户确认）
- [x] 默认栏目「综合资料」承接存量分类，后台自调（用户确认）
- [x] 单表 parent_id 自引用，两级封顶；资料只挂二级
- [x] 管理后台树管理纳入本轮必做

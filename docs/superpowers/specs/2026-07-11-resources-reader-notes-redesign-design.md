# 资料共享改版：「PDF 阅读 + 专业笔记 + 社区」设计 spec

- 日期：2026-07-11
- 状态：设计定稿（brainstorming 四节逐节确认 + Fable 评审修订折入）；**2026-07-11 范围修订 v2 见下方修订记录**
- 范围：~~总体蓝图（五期）~~ → **阅读与笔记两期** + 一期详设（三栏阅读工作台）

> **修订 v2（2026-07-11，范围裁剪）**：经产品化方向讨论，info-serve 仅保留**阅读与笔记**能力（本 spec 一期全部 + 二期笔记进阶）。原三期智能文件处理（OCR/OFD）、四期社区、五期协同**移出 info-serve 路线图**：预览与智能工具由独立的「文档能力产品」承接（独立仓库、插件化、闭源孵化，产品决策记录见 §2.3），info-serve 后期经其 SDK 集成。一期详设（§3–§6）与 Plan 1 不受影响，继续有效。
- 落位：portal-resources BC 内演进（方案 A），新表 `res_` 前缀
- 配套：架构法源 `docs/architecture/bounded-contexts.md`（「笔记先依附资料」既有决策）· 规约 `AGENTS.md`

---

## 1. 产品定位与决策记录

资料共享模块升级为**多格式文档阅读、深度标注、专业笔记工作区、知识社区**的学习与知识管理平台。三栏布局：左（大纲/缩略图）+ 中（文档）+ 右（可扩展工作区）。

### 1.1 已裁决的关键决策

| 决策点 | 结论 |
|---|---|
| 设计范围 | 总体蓝图 + 一期详设；二至五期只定接口与数据模型方向 |
| 阅读器内核 | **pdfjs-dist 6.x 自研 Vue 封装**（需求原文的 react-pdf 不适用于本 Vue 3 工程；现有 @embedpdf/vue-pdf-viewer 封闭度高无法叠自建标注层，过渡期保留、二期退役） |
| OCR / OFD | 一期不做；转换管线设计为可插拔流水线，三期落地 **OFD 用 ofdrw（纯 Java）**、OCR 用离线方案（PaddleOCR vs OCRmyPDF 三期实测定夺）。离线麒麟 arm64 环境不依赖任何云 API |
| 一期笔记深度 | 基础版：Tiptap 富文本 + 原文引用块 + 笔记纸背景 + 自动保存；模板/导图/抽认卡进二期 |
| 实时协同 | 接受新增 Node 协同服务（Hocuspocus + Yjs），**五期**落地；一期单人编辑 + 乐观锁，笔记正文存 Tiptap JSON 保证升级路径 |
| 整体落位 | 方案 A：后端留在 portal-resources（`res_` 新表），前端新建全屏阅读工作台页，旧 preview 过渡后退役 |

### 1.2 现状基线（改版起点，已核实代码）

- 后端 `ruoyi-portal-resources`（8111）：`info_resource*` 五表；上传 → RabbitMQ 自发自收队列 → `ResourceConvertListener` → `DocumentPreviewConverter`（soffice headless）→ **本地磁盘缓存**（`deploy/compose/services/portal-resources.yml` bind mount）；`previewPdf` 请求线程同步轮询等待转换（最长 120s）；OFD 明确不支持；无 OCR
- 现有笔记：`info_resource_note` 纯文本 ≤2000 字、private/public 两档，前端 textarea 简易实现
- 前端：Vue 3.5 + Element Plus 2.13.5（**自带 ElSplitter**）+ pinia；`pdfjs-dist@6.1.200` 已在依赖（缩略图在用）
- 公共设施：`ruoyi-common-sse` / `ruoyi-common-websocket` / `ruoyi-common-ratelimiter` 可用；跨 BC 事件走 `portal.topic`（`PortalEventConstants`）

---

## 2. 总体蓝图与分期路线

| 期 | 内容 | 关键产出 |
|---|---|---|
| **一期（本 spec 详设）** | 三栏阅读工作台 + 阅读版本地基 | ① 文档版本固化（`res_document_version`，转换产物固化为 OSS 持久工件）；② 转换状态机 API（替代 120s 同步轮询）；③ pdfjs-dist 自研阅读器（三栏可收起可拖宽、页级虚拟化）；④ 双 selector 精确锚定标注层（高亮/下划线/批注）；⑤ Tiptap 笔记基础版（引用块双向跳转、笔记纸、自动保存）+ 旧笔记迁移 |
| 二期 | 笔记进阶 | 结构化模板（康奈尔/SWOT/麦肯锡，Tiptap 自定义节点）、模板混用与用户自定义、Markmap 导图、抽认卡、双向链接、右栏多面板 Tab/分屏；**embedpdf 双栈退役截止点** |
| ~~三期~~ **移出（v2）** | ~~文件处理增强~~ | OCR/OFD 等智能文件处理由独立文档产品承接，info-serve 后期经 SDK 集成；标签体系与全文检索仍属资料库业务，**留 info-serve 另行排期**（不在本 spec 范围） |
| ~~四期~~ **暂缓（v2）** | ~~社区~~ | 笔记快照/广场/叠加层/点赞评论整体移出路线图；若未来重启，仍须遵守：通知走 kernel 消息 scene、关注/认证归属先与 kernel 裁决、快照按「投影可再生」设计 |
| ~~五期~~ **暂缓（v2）** | ~~协同~~ | Yjs/Hocuspocus 实时协作移出路线图；一期笔记正文存 Tiptap JSON 的升级路径仍然保留 |

**远期预留**：笔记若独立成第 6 个 BC，快照与锚点结构随迁，资料域保留文件与标注（BC 文档既有决策的迁移路径）。

### 2.3 独立文档产品决策记录（v2 新增，产品建仓后此节迁走）

与 info-serve 解耦的「文档能力产品」（预览 + 工具插件生态）已裁决的六项决策：

| 决策点 | 结论 |
|---|---|
| 落位 | **新建独立仓库**，鉴权中立，info-serve 是首个客户（经 SDK 集成） |
| 能力边界 | 预览 + 标注 + 工具生态为核心；**编辑作为插件正式规划**（默认阅读、点击进入编辑、版本管理兜底；底座评估 Univer 类，P2–P3）——详见产品定位文档 `2026-07-11-wito-positioning.md` |
| 插件模型 | **混合**：前端官方插件构建时集成、运行时按许可启停；后端智能工具一律独立容器 + manifest 注册；第三方运行时加载列远期 |
| AI 依赖 | **Provider 抽象不绑模型**：LLM 类插件对接 OpenAI-compatible API（私有化指 Ollama/vLLM）；OCR=PaddleOCR 容器插件；朗读默认浏览器 Web Speech，服务端 TTS 可选插件 |
| 商业模式 | **先闭源开发、模式后定**；护栏：依赖只允许宽松 license（Apache/MIT/BSD，禁 AGPL 传染），仓库/文档按开源标准建设 |
| 与本 spec 关系 | 锚定模型、版本固化、转换管线插拔化、面板注册表四个地基设计将以解耦形态复用为产品内核；先例调研（Univer/kkFileView/Stirling-PDF）在产品 spec 阶段执行 |

### 2.1 四个地基伏笔（一期做对，后期全部复用）

1. **锚定模型**：锚点 = `version_id + sha256 校验 + TextQuoteSelector（主锚）+ 位置提示（加速）+ 归一化 rects（视觉兜底）`；标注、引用块、社区评论、快照叠加层共用同一结构，失配走重锚定与孤儿降级，**永不静默错位**
2. **笔记正文一律 Tiptap JSON**：schema 白名单渲染、禁 raw HTML 节点；模板扩展与 Yjs 升级无需数据迁移
3. **右栏面板注册表**：面板契约 `{id, title, icon, component}`；笔记/标注是首批面板，导图、抽认卡、问答、术语库、翻译台按同一契约插入，宿主不改
4. **转换管线插拔化**：上传 → [格式转换] → [文本层检测] → [OCR 补层（预留）] → 版本固化；三期 OFD/OCR 只是新增步骤

### 2.2 横切约束（每期适用）

- schema 变更三件套（种子 + `deploy/updates` 增量 + MANIFEST）；新表全部带 `tenant_id`（与 `info_resource_*` 一致，不走 excludes）
- 新增权限码 / `my-*` 查询 / 统计口径随期补齐 BC 契约；**优先 BC 内新增接口，慎动 `ruoyi-api-portal-kernel`**（动了 = 五服务同批重建）
- 测试门禁：锚定算法纯函数全量单测 + 模块单测（`-DskipTests=false`）+ `build:prod` + `design:audit` + e2e 冒烟
- 性能预算（一期）：500 页 PDF 首屏可读 < 3s，滚动不卡顿（页级虚拟化）
- 单文件 ≤800 行硬限，组件目标 ≤400 行

---

## 3. 数据模型与锚定模型（一期）

### 3.1 `res_document_version` — 阅读版本（锚定地基）

| 列 | 类型 | 说明 |
|---|---|---|
| version_id | int8 PK | 雪花 ID |
| resource_id | int8 | 索引 |
| source_oss_id | int8 | 源文件工件 |
| pdf_oss_id | int8 | 固化阅读 PDF 工件（源即 PDF 时同 source_oss_id） |
| pdf_sha256 | varchar(64) | 锚点校验基准 |
| page_count | int4 | PDFBox 提取 |
| text_layer_status | varchar(20) | `has_text`/`no_text`/`unknown`（三期 OCR 挂钩点） |
| converter | varchar(80) | `native-pdf`/`libreoffice:24.x`（三期 `ofdrw:x`/`paddleocr:x`） |
| status | varchar(20) | `converting`/`ready`/`failed` |
| fail_reason | varchar(500) | |
| is_current | char(1) | 换文件重传插新行，旧版本养活旧锚点；部分唯一索引 `(resource_id) WHERE is_current='1'` |
| tenant_id + 审计列 | | 同仓库惯例（create_by/create_time/update_by/update_time/del_flag） |

本地磁盘 preview-cache 降级为**可丢弃热缓存**，不再承载锚定语义（解决 k3s Pod 漂移与 LibreOffice 重转漂移问题）。

### 3.2 `res_annotation` — 阅读标注

| 列 | 类型 | 说明 |
|---|---|---|
| annotation_id | int8 PK | |
| resource_id / version_id | int8 | 索引 `(version_id, user_id)` |
| user_id | int8 | 行级归属：只能改删自己的 |
| type | varchar(20) | `highlight`/`underline`（可扩展） |
| color | varchar(20) | **语义色名**（yellow/green/blue/pink/purple），渲染映射 tokens.scss，不存 hex |
| page | int4 | 锚点冗余，大文档按页段拉取 |
| anchor | jsonb | 见 3.4 |
| comment | varchar(1000) | 可选批注 |
| note_id | int8 可空 | 「引用到笔记」同步创建的标注回指笔记（原文→笔记反向联动不扫 JSON） |
| tenant_id + 审计列 | | |

### 3.3 `res_note` — 笔记

| 列 | 类型 | 说明 |
|---|---|---|
| note_id | int8 PK | |
| resource_id + user_id | | 索引；一人一资料多篇，首开自动建「默认笔记」 |
| title | varchar(160) | |
| content | jsonb | Tiptap JSON，上限 512KB，节点白名单 |
| content_text | text | 保存时提取纯文本（三期 tsvector 直接可用） |
| paper_style | varchar(30) | `blank/ruled/grid/dot/cornell` |
| visibility | varchar(20) | `private`(默认)/`public`；**过渡设计**：平移现有「公开笔记」Tab，四期发布动作改走快照表 |
| tenant_id + 审计列 | | |

引用块不建表：Tiptap 自定义节点 `sourceQuote` attrs 内嵌 `{versionId, anchor, quoteText}`，笔记→原文单向跳转；原文→笔记经 `res_annotation.note_id` 回指。

### 3.4 锚点 JSON（标注/引用块/四期评论与叠加层共用）

```json
{
  "v": 1,
  "sha256": "<PDF工件指纹>",
  "page": 5,
  "quote": { "exact": "选中文字(≤1000字)", "prefix": "前文32字", "suffix": "后文32字" },
  "position": { "startItem": 12, "startOffset": 3, "endItem": 14, "endOffset": 7 },
  "rects": [ { "x": 0.12, "y": 0.34, "w": 0.40, "h": 0.02 } ]
}
```

三层容错（前端纯函数模块 `useReanchor`，全量单测）：

1. sha 匹配且 `position` 命中、exact 一致 → 快路径
2. `quote` 主锚：页内搜索 → 邻页 ±2 → 全文唯一命中（prefix/suffix 消歧重复文本）
3. `rects` 归一化坐标：文本层未就绪时先渲染视觉高亮的兜底
4. 全部未命中 → 孤儿标注列表（显示 quote 文本，可手动重锚）

### 3.5 旧数据迁移

- 增量 SQL：`info_resource_note` 存量行经 `jsonb_build_object` 包装为单段落 Tiptap doc 迁入 `res_note`（空内容迁为空文档；`content_text`、`visibility` 平移）
- 旧表代码停写、冻结一个版本周期后删除；三件套 + MANIFEST 齐发

---

## 4. 一期前端：三栏阅读工作台

### 4.1 布局与收起规则

```
┌───────────── ReaderTopBar（返回/文档名/页码跳转/缩放/栏位开关）─────────────┐
│ 左栏 20%        ║ 中栏 55%（文档）        ║ 右栏 25%（工作区）              │
│ 大纲│缩略图 Tab ║ PDF 虚拟滚动 + 标注层   ║ [笔记][标注] 面板 Tab（注册表） │
└──────────────────────────────────────────────────────────────────────┘
```

- 三栏宽度 ElSplitter 拖调（左右 min 240px、中 min 480px），宽度存 localStorage
- 左右栏可收起为 44px rail；**中栏可收起**（专注笔记模式），约束：**中栏与右栏至少一个展开**；笔记内点引用块跳原文时自动展开中栏并滚动定位
- 入口：资料卡片/详情「阅读」按钮 → `reader/:resourceId` 全屏路由（隐藏系统侧栏）；旧 preview.vue 过渡期保留

### 4.2 阅读器内核（pdfjs-dist 6.x 自研）

- 文档加载：axios blob（复用鉴权拦截器）→ `getDocument({data})`；worker `?url` 本地打包（离线 CSP，与 pdfium wasm 同模式）；HTTP Range 流式加载为可选优化项
- 打开流程状态机：`GET reader/{resourceId}/session` → `converting`（骨架屏轮询）/ `ready` / `failed`（原因 + 下载兜底）
- 页级虚拟化：按 `page_count` 与首页尺寸生成占位，渲染可视区 ±2 页，离窗销毁 canvas；缩略图懒加载可取消
- textLayer 透明文本层负责选择；缩放 50%–300% fit-width 默认；rects 归一化随缩放自适应

### 4.3 标注层交互

- 选中文字 → 浮动工具条：高亮（5 语义色）/ 下划线 / 批注 / 引用到笔记
- 标注色板作为**新设计令牌提案**进 `tokens.scss` + `docs/design/design-system.md`（过 design:audit）
- 点击已有标注 → 小卡片：改色/删除/看批注/跳关联笔记
- 右栏「标注」面板：按页分组、点击跳页、孤儿标注区（手动重锚入口）

### 4.4 笔记面板

- Tiptap 白名单：StarterKit + underline + highlight + link + 自定义节点 `SourceQuote`（表格等二期）
- 引用块卡片：竖线样式 + quote 摘录 + 页码徽标 + 跳转按钮；「引用到笔记」= 创建引用块 + 同步创建 highlight 标注（`note_id` 回指）
- 自动保存：debounce 2s + 显式保存 + 离开守卫；乐观锁（update_time 比对，冲突提示刷新；五期 Yjs 替换此层）
- 笔记纸：`paper_style` → CSS 背景（横线/方格/点阵/康奈尔分区底纹，颜色由令牌派生）；多篇笔记切换/新建

### 4.5 组件拆分（目标 ≤400 行/文件）

```
views/portal/resources/reader/
  index.vue                     布局容器 + 收起规则
  composables/                  useReaderSession / usePdfDocument / useVirtualPages /
                                useTextSelection / useAnnotations / useReanchor（纯函数）
  components/                   ReaderTopBar / OutlinePanel / ThumbnailPanel / PdfViewport /
                                PdfPageLayer（canvas+textLayer+标注overlay）/ SelectionToolbar /
                                AnnotationLayer / OrphanAnnotationList
  components/workspace/         WorkspaceHost / panels.ts（注册表）/ NotePanel /
                                NoteEditor（Tiptap 封装）/ extensions/SourceQuote.ts / PaperStyleSelect
  stores/readerStore.ts         pinia：布局态/当前版本/标注集
```

- 新依赖：`@tiptap/vue-3` + `@tiptap/starter-kit` + 少量官方扩展；pdfjs-dist 已在
- API 归位 `api/portal/resources.ts`，超 400 行拆 `api/portal/resources/reader.ts` 子目录

---

## 5. 一期后端：API、转换链路、错误处理

### 5.1 包结构（portal-resources 内新增子域，不动共享契约）

```
org.dromara.portal.resources
  controller/portal/   ReaderSessionController / ResourceAnnotationController / ResourceNoteController
  service/(+impl)      IReaderSessionService / IResourceAnnotationService / IResourceNoteService
  domain/(+bo/vo)      ResDocumentVersion / ResAnnotation / ResNote
  mq/                  ResourceConvertListener 扩展（转换后固化 + 写版本行）
  support/             DocumentPreviewConverter(复用) / PdfMetadataExtractor(PDFBox 新依赖) / AnchorValidator
```

### 5.2 API（门户侧，Sa-Token 登录 + `getPortalReadableResource` 前置）

| 端点 | 说明 |
|---|---|
| `GET /reader/{resourceId}/session` | 状态机核心：取/建 current 版本；未就绪**幂等入队**转换返回 `converting`；`ready` 返回 `{versionId, sha256, pageCount, fileUrl, textLayerStatus}` |
| `GET /reader/version/{versionId}/file` | 固化 PDF 流式输出；versionId 不可变 → `Cache-Control: immutable`；校验链 version→resource→可读 |
| `GET /reader/version/{versionId}/annotations` | 我的标注（可选按页段） |
| `POST/PUT/DELETE /reader/annotations…` | 标注 CRUD；行级归属校验；AnchorValidator 深度校验 |
| `GET /reader/{resourceId}/notes`、`GET/POST/PUT/DELETE /reader/notes/{id}` | 笔记 CRUD；PUT 乐观锁 |
| `GET /reader/{resourceId}/notes/public` | 公开笔记（现状平移，四期升级快照） |

### 5.3 转换链路扩展

- 队列为 resources 服务内部自发自收，`ResourceConvertMessage` 加字段无跨服务重建风险
- 成功路径：soffice 转换 → PDF 上传 OSS → PDFBox 提取页数/探测文本层 → 写 `res_document_version(ready, sha256)`；native PDF 走同一 listener 统一产生版本行；失败写 `failed + fail_reason`
- 幂等键 `(resource_id, source_oss_id, converter)`；MQ 重复消费安全

### 5.4 错误处理矩阵

| 场景 | 后端 | 前端 |
|---|---|---|
| 转换失败/超时 | session=`failed`+原因 | 失败卡片 + 下载原文件兜底 |
| 锚点/内容校验不过 | 400 + 字段级信息 | 工具条/表单明确提示 |
| 笔记乐观锁冲突 | 业务码（409 语义） | 「笔记已在别处更新」+ 刷新对比 |
| OSS 不可达 | 503 语义 | 重试按钮，不白屏 |
| 标注写接口 | `ruoyi-common-ratelimiter` 基础限流 | 节流提示 |
| 锚定失配 | 正常返回（重锚定在前端） | 孤儿列表可手动重锚，永不静默丢 |

安全边界：全端点登录 + 资料可读前置；标注/笔记行级 user_id 校验；anchor 逐字段校验（页码范围、长度上限、rects 0~1）；content 512KB 上限 + 节点类型白名单；Tiptap JSON 只读渲染走 schema 白名单、禁 raw HTML。

---

## 6. 测试与验收（一期 DoD）

**测试**

- 后端单测：AnchorValidator、PdfMetadataExtractor（固定 fixture PDF）、版本幂等、迁移 SQL 种子库演练；`mvn -ntp -pl ruoyi-modules/ruoyi-portal-resources -am -DskipTests=false test`
- 前端：`useReanchor` 纯函数全量单测（跨页选择、重复文本、引文未命中降级）；plus-ui 无单测设施则引入 vitest 仅覆盖纯函数模块
- e2e：参照 `appcenter-v1-e2e.mjs` 新增 reader 冒烟（上传→转换 ready→会话→标注→引用→刷新锚定命中）
- 门禁：模块编译+单测、`npm run build:prod`、`npm run design:audit` 全绿

**验收标准**

1. Word/PPT/Excel/PDF 上传后阅读页可读；OFD/无文字扫描件给明确降级提示
2. 500 页 PDF 首屏 < 3s、滚动流畅
3. 高亮/下划线/批注/引用块全链路可用，刷新后锚定命中
4. 模拟重转/pdfjs 升级场景：孤儿机制生效、无静默错位
5. 三栏收起/拖宽/持久化 + 专注模式自动展开规则正确
6. 旧笔记迁移后可见可编辑
7. 三件套 + MANIFEST 齐全；全部门禁绿

---

## 附录：评审发现处置记录

Fable 评审共 11 项发现，处置如下：Critical-1（锚点绑定漂移文档）→ 新增 `res_document_version` 与双 selector 锚定（§3）；High-2（磁盘缓存与 k3s 冲突）→ OSS 固化+缓存降级（§3.1）；High-3（120s 同步等待）→ 状态机 API（§5.2）；High-4（旧笔记迁移）→ §3.5；Medium-5（BC 契约/关注归属）→ §2 四期标注待裁决；Medium-6（租户策略）→ §2.2；Medium-7（可见性/XSS）→ §5.4；Medium-8（OFD 改 ofdrw）→ §2 三期；Medium-9（性能预算）→ §2.2/§4.2；Medium-10（测试策略）→ §6；Low-11（双栈退役/Y.Doc 真相源表述/ElSplitter 核实）→ §2 二期与五期。

# 资源共享门户重构实施计划

> 关联设计稿：`docs/superpowers/specs/2026-06-29-portal-resources-redesign-design.md`
> 目标页面：`plus-ui/src/views/portal/resources`
> 后端模块：`source/ruoyi-modules/ruoyi-infoservice`
> 日期：2026-06-29

## 1. 实施边界

本次实施面向门户端资源共享应用，交付一个与现有门户风格统一的高保真可用页面，而不是静态草图。

必须包含：

- `资源中心`：面向全员浏览公开资源，支持搜索、分类、类型、时间、排序、网格/列表切换、分页、详情抽屉、下载/预览入口。
- `我的资料`：面向当前登录用户管理自己上传的文件，支持查看状态、预览、下载、编辑、重新上传、下架/发布、删除。
- 上传流程：从门户端完成上传文件和资源信息创建，上传成功后能进入 `我的资料` 管理。
- 权限边界：普通用户不依赖后台管理端 `infoservice:resource:*` 权限完成门户上传和自有资源管理。

不在本次实施中做：

- 不重做后台资源管理列表。
- 不引入新的设计系统或第三方 UI 框架。
- 不做复杂全文检索、推荐算法、收藏夹、评分评论。
- 不修改 `pics/resurces.png`，该文件目前是未跟踪参考图。

## 2. 当前代码事实

- 前端资源页当前只有一个文件：`plus-ui/src/views/portal/resources/index.vue`。
- 前端门户 API 当前在 `plus-ui/src/api/infoservice/portal.ts`。
- 前端类型在 `plus-ui/src/api/infoservice/types.ts`。
- 后端资源门户控制器在 `source/ruoyi-modules/ruoyi-infoservice/src/main/java/org/dromara/infoservice/controller/portal/InfoResourcePortalController.java`。
- 后端资源服务在 `source/ruoyi-modules/ruoyi-infoservice/src/main/java/org/dromara/infoservice/service/impl/InfoResourceServiceImpl.java`。
- 后端资源 VO/BO/实体分别在 `domain/vo/InfoResourceVo.java`、`domain/bo/InfoResourceBo.java`、`domain/InfoResource.java`。
- `plus-ui/package.json` 提供 `build:dev`、`build:prod`、`lint:eslint`、`prettier`，没有独立 typecheck 脚本。
- 远端 Windows 环境未安装 `rg`，开发排查时使用 PowerShell 或在本机临时同步后用 `rg`。

## 3. 后端实施

### 3.1 查询契约扩展

扩展 `InfoResourceBo`，用于门户列表查询：

- `scope`：`public` 默认公开资源，`mine` 当前用户上传资源。
- `previewType` 或 `fileType`：文件类型筛选，优先复用现有 `previewType`。
- `uploadedWithin`：上传时间快捷筛选，例如 `week`、`month`、`quarter`。
- `sizeRange`：文件大小筛选，例如 `small`、`medium`、`large`。
- `sort`：排序，例如 `latest`、`hot`、`downloads`。

服务层 `portalPage` 规则：

- `scope != mine`：只返回 `status = "0"` 的公开资源。
- `scope == mine`：必须登录，只返回 `createBy = LoginHelper.getUserId()` 的资源，可包含本人未公开/已下架状态。
- 所有查询继续支持 `keyword`、`categoryId`、`categoryCode`。
- 排序由服务层白名单控制，不能直接拼接前端字段。

### 3.2 VO 字段扩展

扩展 `InfoResourceVo`：

- `createBy`：用于前端判断归属，但不要作为安全依据。
- `ownerName` 或 `createByName`：资源上传者展示名；如果用户服务查询成本较高，第一版可为空并由前端降级显示“平台用户”。
- `canManage`：后端根据当前登录用户和 `createBy` 计算，前端仅用于显示操作入口。

安全判断必须在服务端方法里重复执行，不能只依赖 `canManage`。

### 3.3 门户上传与自有资源管理接口

在 `InfoResourcePortalController` 新增门户端接口：

- `POST /infoservice/portal/resources/upload`：门户上传文件，复用现有 OSS 上传逻辑。
- `POST /infoservice/portal/resources`：创建资源记录，默认归属当前用户，初始状态按业务配置决定，建议先为 `0` 公开或 `2` 待审核二选一。
- `PUT /infoservice/portal/resources/{resourceId}`：编辑本人资源标题、分类、描述、备注、文件引用等。
- `PUT /infoservice/portal/resources/{resourceId}/status`：本人发布/下架；状态值走白名单。
- `DELETE /infoservice/portal/resources/{resourceId}`：删除本人资源；如果业务不允许物理删除，改为状态下架并在响应中明确。

服务层新增或扩展方法：

- `insertPortalByBo(InfoResourceBo bo)`
- `updateOwnByBo(InfoResourceBo bo)`
- `changeOwnStatus(Long resourceId, String status)`
- `deleteOwnById(Long resourceId)`
- `checkOwner(Long resourceId, Long userId)` 或等价私有方法。

所有自有资源操作必须校验：

- 用户已登录。
- 资源存在。
- `createBy == LoginHelper.getUserId()`。
- 状态值、文件引用、分类 ID 合法。

### 3.4 保持后台兼容

后台管理端 `InfoResourceController` 和已有权限不做破坏性修改。后台仍可管理全量资源，门户端只新增普通用户路径。

## 4. 前端实施

### 4.1 API 与类型

更新 `plus-ui/src/api/infoservice/types.ts`：

- 增加 `ResourcePortalQuery`，包含 `scope`、`keyword`、`categoryId`、`categoryCode`、`previewType`、`uploadedWithin`、`sizeRange`、`sort`、分页字段。
- 扩展 `InfoResource`：`createBy`、`ownerName/createByName`、`canManage`、可选 `status` 展示字段。
- 增加创建/更新资源 payload 类型。

更新 `plus-ui/src/api/infoservice/portal.ts`：

- `listResources(query: ResourcePortalQuery)`
- `uploadPortalResourceFile(data: FormData)`
- `createPortalResource(data)`
- `updatePortalResource(resourceId, data)`
- `changePortalResourceStatus(resourceId, status)`
- `deletePortalResource(resourceId)`

门户资源页停止调用后台管理端上传/新增接口，避免普通用户被后台权限卡住。

### 4.2 组件拆分

当前单文件资源页已经较大，高保真重构建议拆分到 `plus-ui/src/views/portal/resources/components/`：

- `ResourceFilterPanel.vue`：左侧分类、类型、上传时间过滤。
- `ResourceToolbar.vue`：排序、格式、大小、网格/列表切换、结果统计。
- `ResourceCard.vue`：资源卡片。
- `ResourceList.vue`：列表模式。
- `MyResourceTable.vue`：我的资料管理表。
- `ResourceUploadDialog.vue`：上传/编辑弹窗。
- `ResourceDetailDrawer.vue`：详情抽屉。

`index.vue` 负责页面状态编排、接口调用和视图切换，避免继续膨胀。

### 4.3 页面状态

核心状态：

- `activeView`：`center` / `mine`。
- `query.keyword`、`query.categoryCode/categoryId`、`query.previewType`、`query.uploadedWithin`、`query.sizeRange`、`query.sort`。
- `displayMode`：`grid` / `list`，我的资料默认使用表格/列表。
- `pageNum`、`pageSize`、`total`、`loading`。
- `selectedResource`、`detailVisible`、`uploadVisible`、`editMode`。

交互规则：

- 切换一级视图、分类、类型、时间、大小、排序时重置到第一页并重新拉取。
- 上传成功后刷新分类统计和当前视图；如果当前在资源中心，提示“可在我的资料中管理”。
- 未登录用户点击上传或我的资料时，按现有门户登录方式处理；如果项目没有统一登录弹窗，则显示明确的登录提示并不展示假数据。
- 预览/下载使用现有 portal preview/download URL。

### 4.4 视觉实现

视觉统一到现有门户风格：

- 主色使用现有门户蓝系，例如 `#082b68`、`#071f4b`，辅以浅蓝、白色、少量橙/金强调。
- 使用 Element Plus 组件和 `@element-plus/icons-vue` 图标。
- 卡片圆角控制在 8px 左右，阴影克制。
- 文件封面不伪造外部图片，优先用文件类型色块、图标、格式标签表达。
- 移动端左侧筛选折叠为抽屉或顶部筛选入口，资源卡改为单列。

## 5. 验证计划

后端：

- 编译 `ruoyi-infoservice` 相关模块。
- 对 `portalPage(scope=mine)`、公开资源过滤、自有资源 owner 校验补充单元测试或最小服务层测试；若项目没有测试基线，至少补充接口级手工 smoke 清单。
- 手工验证门户上传、创建、我的资料列表、编辑、下架/发布、删除、公开列表不可见未公开资源。

前端：

- 执行 `npm run lint:eslint`。
- 执行 `npm run build:dev`，必要时再跑 `npm run build:prod`。
- 用浏览器验证桌面和窄屏布局：
  - 资源中心搜索、筛选、排序、分页、网格/列表。
  - 详情抽屉、预览、下载。
  - 我的资料状态和操作。
  - 上传/编辑弹窗完整流程。

## 6. 风险与处理

- 上传者名称：如果后端用户名称查询链路不清晰，第一版用 `canManage` 保证权限，展示名可降级。
- 删除语义：若资源涉及审计或 OSS 文件复用，优先实现“下架/软删除”，避免误删文件。
- 权限混用：门户端不能继续调用后台 admin 上传/新增接口，否则普通用户无法使用。
- 未公开泄漏：`scope=public` 必须强制 `status = "0"`，不能被前端参数绕过。
- 单文件复杂度：资源页必须拆组件，否则后续维护和回归成本会过高。

## 7. 建议实施顺序

1. 后端补齐门户接口和查询字段，先跑最小接口 smoke。
2. 前端更新 API/types，替换掉 admin 上传/新增调用。
3. 拆分资源页组件并完成 `资源中心`。
4. 实现 `我的资料` 管理视图。
5. 接入上传/编辑/状态/删除流程。
6. 做桌面与窄屏视觉 QA、lint、build。

通过此计划后，再开始改业务代码。

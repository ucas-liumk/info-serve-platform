# info-serve 当前运行时架构图设计

状态：待书面复核  
日期：2026-07-13  
范围：仅生成运行时架构图，不修改业务代码、部署配置或架构决策

## 1. 目标

使用 Archify 生成当前 `windows-info-serve` 的运行时架构图，交付到：

- `docs/architecture/runtime-architecture.architecture.json`：可维护的 Archify JSON IR；
- `docs/architecture/runtime-architecture.html`：可直接在浏览器打开、切换主题和导出图片的单文件图稿。

图稿以 `docs/architecture/bounded-contexts.md` 为领域边界法源，以
`docs/superpowers/specs/2026-07-07-service-split-cloud-native-design.md` 修订后的部署形态为准，
并用 Gateway 路由、事件常量和统计聚合实现校验当前运行事实。

## 2. 当前运行时口径

当前门户运行态由 5 个独立服务进程组成：

1. `portal-kernel`：门户身份、消息、收藏、模块注册表和统计聚合；
2. `portal-appcenter`：应用中心 BC；
3. `portal-forum`：服务论坛 BC；
4. `portal-requiredknowledge`：应知应会 BC；
5. `portal-resources`：资料共享 BC，包含当前进程内的文档转换消费端。

这里的“5 个门户服务”是 Kernel 加 4 个已实现业务 BC。智能问答 QA、时事热点 News、
`preview-worker` 等尚未进入当前运行态，因此不画成已部署组件，只在说明卡中标为未来预留。

## 3. 图稿叙事与布局

图采用单一左到右主路径，并分为四层：

1. **入口层**：用户浏览器 → `nginx-web :7010` → `ruoyi-gateway :8180`；
2. **门户服务层**：Kernel 与 4 个业务 BC，置于同一门户服务边界内；
3. **通用服务与协作层**：Auth、System、File、Nacos、RabbitMQ；
4. **数据与对象存储层**：PostgreSQL、Redis、MinIO，并明确 MySQL 只服务 Nacos 配置库。

主链路只保留必要箭头，细节写入图下说明卡，避免把每个服务到每项基础设施的重复连接全部展开。

## 4. 通信方向编码

### 4.1 允许方向

- 浏览器流量经 Nginx 和 Gateway，Gateway 依据 Nacos 服务发现路由到 5 个门户服务；
- 业务服务可同步调用 Auth、System、File 等通用服务；
- `portal-kernel → AppCenter / Forum / Resources` 是唯一的业务侧同步例外，仅用于首页统计，
  采用 Dubbo、2 秒超时、不可用时降级为 0；RequiredKnowledge 当前不在统计契约中；
- 跨 BC 业务协作经 RabbitMQ `portal.topic` 异步发布和消费；
- 各服务只访问自身归属表，Resources 经 File/MinIO 处理文件对象。

### 4.2 禁止方向

图中使用安全类型组件和红色说明卡表达以下红线，不画成正常可调用箭头：

- 禁止业务 BC 之间直接同步互调；
- 禁止跨 BC SQL；
- 禁止复制 Kernel 的消息、收藏、统计和模块注册表能力；
- 禁止绕过 Gateway 暴露门户业务入口。

## 5. 图例

- 青色强调实线：用户请求主路径；
- 普通实线：允许的同步依赖；
- 紫色/虚线：RabbitMQ 异步事件；
- 玫红安全元素：禁止方向和边界红线；
- 琥珀边界：运行区域或服务分组；
- 数据库类型组件：PostgreSQL、Redis、Nacos MySQL；
- 云存储类型组件：MinIO。

## 6. 证据来源

- `AGENTS.md`：当前强制通信红线和门户服务清单；
- `docs/architecture/bounded-contexts.md`：统一语言、表前缀与 BC 归属；
- `docs/superpowers/specs/2026-07-07-service-split-cloud-native-design.md`：当前独立服务部署形态；
- `source/script/config/nacos/ruoyi-gateway.yml`：Gateway 到 5 个门户服务的真实路由；
- `source/ruoyi-api/ruoyi-api-portal-kernel/.../PortalEventConstants.java`：`portal.topic`、队列和 routing key；
- `source/ruoyi-modules/ruoyi-portal-kernel/.../InfoPortalServiceImpl.java`：统计聚合方向、2 秒超时和降级行为；
- `PORTS.md`、`RUNBOOK.md`：入口、服务和基础设施端口。

## 7. 验证

交付前执行：

1. `archify validate architecture ... --json`，验证 JSON Schema 和布局；
2. `archify render architecture ...`，生成 HTML；
3. `archify check ...`，验证 SVG/HTML artifact；
4. 浏览器打开 HTML，检查深浅主题、文字可读性、连线与边界；
5. `git diff --check`，并确认只新增设计与图稿文件。

本任务不改后端、前端、部署或行为面，因此不触发 Maven、前端构建、Compose 或 E2E 门禁。

# info-serve 当前运行时架构图设计

状态：待书面复核  
日期：2026-07-13  
范围：仅生成运行时架构图，不修改业务代码、部署配置或架构决策

## 1. 目标

使用 Archify 生成当前 `windows-info-serve` 的运行时架构图，交付到：

- `docs/architecture/runtime-architecture.architecture.json`：可维护的 Archify JSON IR；
- `docs/architecture/runtime-architecture.html`：可直接在浏览器打开、切换主题和导出图片的单文件图稿。

图稿以 `docs/architecture/bounded-contexts.md` 为领域边界法源，并用当前 Docker Compose、
Gateway 路由、事件常量和统计聚合实现校验运行事实。2026-07-07 设计中的 k3s 是目标生产形态，
不是当前仓库的主启动路径；本图只画当前 Docker Compose 运行态，不把目标形态混入当前事实。

## 2. 当前运行时口径

当前门户运行态由 5 个独立服务进程组成：

1. `portal-kernel`：门户身份、消息、收藏、模块注册表和统计聚合；
2. `portal-appcenter`：应用中心 BC；
3. `portal-forum`：服务论坛 BC；
4. `portal-requiredknowledge`：应知应会 BC；
5. `portal-resources`：资料共享 BC，包含当前进程内的文档转换消费端。

这里的“5 个门户服务”是 Kernel 加 4 个已实现业务 BC。智能问答 QA、时事热点 News、
`preview-worker` 等尚未进入当前运行态，因此不画成已部署组件，只在说明卡中标为未来预留。

当前平台底座还包含 `ruoyi-auth`、`ruoyi-system`、`ruoyi-file`、`ruoyi-monitor :8190`，
以及 Gateway、Nacos、PostgreSQL、MySQL、Redis、RabbitMQ 和 MinIO。

## 3. 图稿叙事与布局

图采用单一左到右主路径，并分为四层：

1. **入口层**：用户浏览器 → `nginx-web :7010` → `ruoyi-gateway :8180`；
2. **门户服务层**：Kernel 与 4 个业务 BC，置于同一门户服务边界内；
3. **通用服务与协作层**：Auth、System、File、Monitor、Nacos、RabbitMQ；
4. **数据与对象存储层**：PostgreSQL、Redis、MinIO，并明确 MySQL 只服务 Nacos 配置库。

主链路只保留必要箭头，细节写入图下说明卡，避免把每个服务到每项基础设施的重复连接全部展开。

## 4. 通信方向编码

### 4.1 当前实际依赖

- 浏览器流量经 Nginx 和 Gateway，Gateway 依据 Nacos 服务发现路由到 5 个门户服务；
- 所有 Java 服务连接 Nacos 获取配置并完成服务注册与发现；Nacos 单独使用 MySQL `ry-config`；
- Gateway、门户服务和平台服务按各自配置使用 PostgreSQL、Redis、RabbitMQ 等基础设施，
  图中只画经代码或 Compose 能确认的实际依赖，不把“架构允许”自动画成“已经调用”；
- AppCenter 与 Resources 使用 `RemoteFileService` 获取文件契约/元数据，并通过 `OssFactory`
  访问 MinIO 对象数据面；
- `portal-kernel → AppCenter / Forum / Resources` 是唯一允许的“通用服务反向同步调用业务 BC”例外，
  当前仅用于首页统计，
  采用 Dubbo、2 秒超时、不可用时降级为 0；RequiredKnowledge 当前不在统计契约中；
- 跨 BC 业务协作经 RabbitMQ `portal.topic` 异步发布和消费；
- 当前业务数据共享一个 PostgreSQL 实例，但各服务只能访问自身表前缀，保持逻辑隔离；
- Redis 是共享缓存、令牌与事件幂等基础设施；Monitor 通过 Spring Boot Admin 观测服务。

### 4.2 架构允许但不等于当前已发生

- 业务服务允许同步调用 Auth、System、File 等通用服务；
- 这条规则只作为边界说明卡出现，除非能从当前代码确认具体调用，否则不画成组件间实线。

### 4.3 禁止方向

图中使用安全类型组件和红色说明卡表达以下红线，不画成正常可调用箭头：

- 禁止业务 BC 之间直接同步互调；
- 禁止跨 BC SQL；
- 禁止复制 Kernel 的消息、收藏、统计和模块注册表能力；
- 禁止绕过 Gateway 暴露门户业务入口。

## 5. 图例

- 青色强调实线：用户请求主路径；
- 普通实线：经代码或配置确认的当前同步依赖；
- 紫色/虚线：RabbitMQ 异步事件；
- 玫红安全元素：禁止方向和边界红线；
- 琥珀边界：运行区域或服务分组；
- 数据库类型组件：PostgreSQL、Redis、Nacos MySQL；
- 云存储类型组件：MinIO。

## 6. 证据来源

- `AGENTS.md`：当前强制通信红线和门户服务清单；
- `docs/architecture/bounded-contexts.md`：统一语言、表前缀与 BC 归属；
- `docs/superpowers/specs/2026-07-07-service-split-cloud-native-design.md`：服务拆分法源与目标 k3s 生产形态；
- `source/script/config/nacos/ruoyi-gateway.yml`：Gateway 到 5 个门户服务的真实路由；
- `source/ruoyi-api/ruoyi-api-portal-kernel/.../PortalEventConstants.java`：`portal.topic`、队列和 routing key；
- `source/ruoyi-modules/ruoyi-portal-kernel/.../InfoPortalServiceImpl.java`：统计聚合方向、2 秒超时和降级行为；
- `source/ruoyi-modules/ruoyi-portal-appcenter`、`ruoyi-portal-resources`：File 契约与 MinIO 数据面访问；
- `deploy/docker-compose.yml`：当前 Compose 运行态、Monitor 与基础设施依赖；
- `PORTS.md`、`RUNBOOK.md`：入口、服务和基础设施端口。

## 7. 验证

交付前执行：

1. 建立事实矩阵，逐项核对组件、端口、Gateway 路由、同步/异步方向和存储依赖；
2. 设置本机 Archify CLI 路径，例如 Mac Codex 全局 Skill：
   `ARCHIFY_CLI="$HOME/.codex/skills/archify/bin/archify.mjs"`；Windows 或其他 Agent 使用其实际安装路径；
3. `node "$ARCHIFY_CLI" validate architecture docs/architecture/runtime-architecture.architecture.json --json`；
4. `node "$ARCHIFY_CLI" render architecture docs/architecture/runtime-architecture.architecture.json docs/architecture/runtime-architecture.html`；
5. `node "$ARCHIFY_CLI" check docs/architecture/runtime-architecture.html`；
6. 浏览器打开 HTML，检查深浅主题、文字可读性、连线与边界，并对 PNG、SVG 各做一次导出冒烟；
7. `git diff --check`，并确认只新增设计与图稿文件。

本任务不改后端、前端、部署或行为面，因此不触发 Maven、前端构建、Compose 或 E2E 门禁。

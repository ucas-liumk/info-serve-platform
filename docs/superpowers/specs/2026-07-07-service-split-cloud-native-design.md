# info-serve 服务拆分与云原生部署设计

状态：**定稿**（2026-07-07，四项评审修订已合入）
前置文档：`docs/architecture/bounded-contexts.md` v3（统一语言与边界纪律继续有效，部署裁决由本文修订）

## 1. 背景与驱动

bounded-contexts v3（2026-07-03）裁决「合并为模块化单体、拆前不拆」，同时预设了晋升触发器。本次拆分**不是推翻旧决策，而是触发器被命中后的正常演进**：

- **触发器④故障隔离**：生产部署中出现「一个服务挂掉导致其他都不能用」的级联故障（根因未精确定位，属业务崩溃/基础设施故障/宿主机资源耗尽多种情况混合），单体门户内任一模块异常即全门户不可用；
- **触发器③多团队节奏**：其他团队将基于通用服务独立开发模块，要求稳定契约与独立发布；
- **新增部署事实**：生产目标为**麒麟 V10 + 飞腾 2000（arm64）服务器，64GB+ 内存，内网离线，先单台后扩容**。

## 2. 决策输入（已确认）

| 决策点 | 结论 |
|---|---|
| 故障根因 | 不确定/多种情况 → 设计对三类故障全防（§7） |
| 拆分粒度 | **全拆**：portal 五个 BC 各自成服务，kernel 独立为门户内核服务 |
| 部署形态 | **k3s 单节点**（麒麟 V10 / 飞腾 2000 / arm64），按可扩展设计 |
| 团队接入 | **独立服务接入**：独立仓库独立进程，经 Nacos 注册 + 网关路由接入 |
| 硬件预算 | 64GB+ 内存，全拆可行 |
| 网络环境 | 内网隔离，发布包完全自包含（离线安装 + 镜像 tar） |
| 节点规模 | 先单台，存储与配置避免单机硬编码，预留扩容路径 |

## 3. 目标形态与新架构红线

```
                 ┌─ 麒麟 V10 / 飞腾2000 (arm64) · k3s 单节点（可扩） ─┐
                 │                                                    │
用户 ──► nginx-web(7010) ──► ruoyi-gateway(8180)                      │
                 │              │ 按路由分发（Nacos 服务发现）          │
                 │   ┌──────────┴─────────────┬───────────────┐       │
                 │   通用服务层（平台底座）      业务服务层（BC 服务）    │
                 │   ruoyi-auth        8110   portal-appcenter  8106  │
                 │   ruoyi-system      8101   portal-forum      8108  │
                 │   ruoyi-file        8114   portal-requiredknowledge 8109
                 │   portal-kernel     8107   portal-resources  8111  │
                 │   ruoyi-monitor     8190   （未来：ruoyi-ai-qa、news）
                 │                                                    │
                 │   基础设施：Nacos · PostgreSQL · MySQL(仅Nacos)      │
                 │            Redis · RabbitMQ · MinIO                │
                 └────────────────────────────────────────────────────┘
```

**新架构红线（对人与 AI 强制）**：

1. 每个服务独立进程、独立镜像、独立启停；任何业务服务宕机只影响自身功能入口，不得影响其他服务。
2. 业务服务之间**禁止直接同步互调**。同步调用只允许「业务/通用 → 通用服务」；唯一例外是 kernel → 各 BC 的统计接口（必须带超时与降级）。跨业务协作一律走 RabbitMQ 事件。
3. 通用服务以稳定契约（OpenAPI + Dubbo 接口 + 事件目录）对外开放；契约变更走版本化，不允许破坏性变更直接发布。
4. 表归属私有不变：每服务只碰自己前缀的表；同库物理部署、逻辑隔离；禁止跨服务 SQL。
5. 每个 Pod 必须声明 resources requests/limits 与健康探针，无探针无限额的服务不得进入生产 manifests。

## 4. 服务清单与端口

### 4.1 通用服务层（平台底座，其他团队的依赖基座）

| 服务 | 端口 | 职责 |
|---|---:|---|
| ruoyi-gateway | 8180 | 统一入口、路由、鉴权过滤、OpenAPI 文档聚合、**第三方限流** |
| ruoyi-auth | 8110 | 登录认证、令牌签发 |
| ruoyi-system | 8101 | 用户/角色/权限/字典 |
| ruoyi-file | 8114 | 文件上传下载（MinIO 适配） |
| **portal-kernel** | 8107 | 门户身份、统一消息、收藏、模块注册表、首页统计聚合 |
| ruoyi-monitor | 8190 | Spring Boot Admin 监控与告警 |

### 4.2 业务服务层（每 BC 一个，团队可独立认领）

| 服务 | 端口 | 说明 |
|---|---:|---|
| portal-appcenter | 8106 | 回收原 appcenter 端口；平稳 CRUD，拆分理由为团队认领与故障隔离 |
| portal-forum | 8108 | 平稳 CRUD，同上 |
| portal-requiredknowledge | 8109 | 活跃开发，独立发布节奏 |
| portal-resources | 8111 | 含 LibreOffice 文档转换（头号故障源）；转换积压常态化时再拆 preview-worker |

### 4.3 预留（触发即启用，端口与内存预算现在登记）

| 服务 | 预留端口 | 启用触发器 |
|---|---:|---|
| snail-job server | 8191-8193 | ①时事热点抓取立项；②任一业务服务扩到多副本（`@Scheduled` 会重复执行）；③文档转换补偿任务复杂化 |
| ruoyi-ai-qa | 8115 | 智能问答立项（v3 已决：出生即独立服务） |
| preview-worker | 8112 | 转换队列积压成为常态 |

### 4.4 主动排除项（不是遗漏，是决策）

| 组件 | 上游完整版有 | 排除理由 |
|---|---|---|
| seata 分布式事务 | 有 | 红线 2/4（同库+事件驱动）使其无必要；引入 seata 等于承认边界失守 |
| ruoyi-workflow（warm-flow） | 有 | 当前业务无多级审批需求；资料审核为简单状态流转。出现真实审批需求再评估 |
| ruoyi-gen 代码生成 | 有 | 开发工具而非生产服务；需要的团队本地自起，不进生产架构 |
| ELK / SkyWalking 全家桶 | 可选 | 单节点阶段过重；观测性按 §9.4 渐进启用 |

## 5. 代码结构（Maven）

```
ruoyi-modules/
  ruoyi-portal-kernel/             ← org.dromara.portal.kernel 整包迁移
  ruoyi-portal-appcenter/          ← org.dromara.portal.appcenter
  ruoyi-portal-forum/              ← org.dromara.portal.forum
  ruoyi-portal-requiredknowledge/  ← org.dromara.portal.requiredknowledge
  ruoyi-portal-resources/          ← org.dromara.portal.resources
ruoyi-api/
  ruoyi-api-portal-kernel/         ← 新增：内核 Dubbo 契约
```

- `ruoyi-api-portal-kernel` 暴露：`RemoteMessageService`（scene 化消息）、`RemoteFavoriteService`、`RemoteModuleRegistryService`、`RemotePortalUserService`，以及各 BC 反向实现的 `RemoteModuleStatsService`（Dubbo group=模块编码）。
- 依赖方向由 Maven 结构强制：BC 服务 → `ruoyi-api-portal-kernel` / `ruoyi-api-system` / `ruoyi-api-file`；**BC 服务之间零依赖**（越界直接编译不过）。
- 原 ruoyi-portal 的 ArchUnit BC 边界测试降级为各服务内部分层校验；`BcBoundaryTest` 中跨 BC 规则随物理拆分自然失效，保留分层与命名规则。
- 前端 plus-ui 不拆，仍为单 SPA；批次 A 核对各 BC controller 前缀（目标 `/portal/<bc>/**`）与历史兼容路由（`/appcenter/**`、`/portal/tools`），网关按前缀路由到对应服务，前端 API 调用零改动。

## 6. 服务间通信约定

### 6.1 同步：Nacos 注册发现 + Dubbo

仅允许两个方向：

1. 业务/通用服务 → 通用服务（auth / system / file / kernel）；
2. kernel → 各 BC 的 `RemoteModuleStatsService`（首页统计聚合，**必须**设超时并对不可用 BC 降级为占位数据——某 BC 宕机时首页仅该卡片降级，不再全灭）。

### 6.2 异步：RabbitMQ（所有跨 BC 协作）

| 约定项 | 内容 |
|---|---|
| Exchange | `portal.topic`（topic、durable）；死信 exchange `portal.dlx` |
| Routing key | `<bc>.<聚合>.<事件>`，如 `forum.reply.created`、`resources.item.published` |
| 队列命名 | `<消费方服务>.<用途>`，如 `portal-kernel.notifications`；每队列配套 `<队列名>.dlq` |
| 消息信封 | `eventId`(UUID) / `type` / `occurredAt` / `actor` / `payload`；生产者开启 confirm；消费者手动 ack + 按 eventId 幂等去重 |
| 事件目录 | `docs/api/event-catalog.md`（批次 D 建立），新事件先登记后上线 |

**首批落地**：

1. **通知流**：BC 发布领域事件 → kernel 消费写统一消息表（v3 §8「通知黑洞」的最终修复形态）；
2. **文档转换**：`portal-resources.convert` 工作队列（direct，prefetch=1），LibreOffice 转换异步化，与 Web 请求线程隔离。

## 7. 故障隔离与自愈（对三类故障全防）

| 故障类型 | 手段 |
|---|---|
| 业务服务崩溃 | 每 Pod liveness/readiness/startup 探针（actuator health）+ k3s 自动重启；网关仅该服务路由 503；前端从模块注册表感知并降级展示 |
| 基础设施崩溃 | Nacos/PG/MySQL/Redis/RabbitMQ/MinIO 全部进 k3s 管理：探针 + 自愈 + local-path 持久卷，不再「死了等人扶」 |
| 资源耗尽级联 | 每 Pod 强制 requests/limits，JVM 以 `-XX:MaxRAMPercentage` 对齐容器限额；三级 PriorityClass：基础设施(1000000) > 通用服务(100000) > 业务服务(10000)，内存压力下先驱逐业务 Pod 保底座 |

飞腾单核性能弱：startup 探针窗口放宽至 180s；JVM 启动参数按 arm64 实测调优（批次 B 产出基线）。

### 7.1 内存预算草案（64GB）

| 分组 | 预算（limits 合计） |
|---|---|
| 10 个 Java 服务（gateway/auth/system/file/kernel/monitor + 4 BC；resources 单独 2Gi） | ≈ 13 Gi |
| 基础设施（PG 4Gi、Nacos 1.5Gi、Redis 1.5Gi、RabbitMQ 1.5Gi、MinIO 1Gi、MySQL 1Gi） | ≈ 10.5 Gi |
| k3s 控制面 + 系统预留 | ≈ 2 Gi |
| **合计** | **≈ 25.5 Gi**，余量充足（含未来 snail-job/ai-qa 预留） |

## 8. API 文档与第三方接入

1. **网关聚合 Swagger**：各服务 springdoc 暴露 `/v3/api-docs`，gateway 按 Nacos 服务列表聚合分组，单一地址浏览全部通用服务 API；
2. **离线契约导出**：发布包内置通用服务 OpenAPI JSON 快照（内网环境的契约正本）；
3. **《第三方模块接入指南》**（`docs/api/third-party-integration.md`，批次 D）：Nacos 注册规范（namespace/group/服务命名）、网关路由申请流程、sa-token 认证集成、权限码申请、事件目录接入、`portal_module` 模块注册表登记、表前缀申请、发布包接入（镜像 tar + MANIFEST 登记）；
4. **网关限流（开放契约的前置条件）**：Spring Cloud Gateway `RequestRateLimiter`（Redis 令牌桶），按路由配置策略，第三方服务路由必须挂默认限流策略；热点接口辅以 `ruoyi-common-ratelimiter` 注解限流。失控的第三方服务不得拖垮通用服务——限流本身就是「互不干扰」的一部分。

第三方团队接入 = 独立仓库独立服务，注册 Nacos + 登记网关路由 + 登记模块注册表卡片，全程不修改本仓库代码。

## 9. 部署（k3s · 麒麟 V10 · arm64 · 离线）

### 9.1 目录结构

```
deploy/k3s/
  prereq/preflight.sh        ← 麒麟预检：内核版本/cgroup/firewalld/kysec/sysctl/时间同步
  airgap/                    ← k3s 二进制 + airgap 镜像 + 离线安装脚本（版本锁定）
  services/<服务名>/          ← 每服务一套 kustomize manifests，独立 apply/delete
  infra/<组件名>/             ← 基础设施 StatefulSet + local-path PVC
  backup/                    ← 备份 CronJob manifests（§9.3）
  bin/svc.sh                 ← svc.sh start|stop|restart|status|logs <服务名>
```

**每服务独立启停**：`svc.sh stop portal-forum` 只动论坛，其余服务纹丝不动。这是本设计对最初诉求的直接交付物。

### 9.2 镜像与发布包（离线自包含）

- **服务器上不装 Docker Engine**：k3s 内置 containerd，`k3s ctr images import` 导入镜像 tar；Docker 仅存在于构建机（buildx 交叉构建 arm64）与开发机（compose）；
- Java 服务基础镜像：eclipse-temurin arm64；基础设施使用官方 arm64 镜像并锁定版本；**LibreOffice arm64 为批次 B 首个验证点**；
- 发布包结构：`k3s 安装物 + arm64 镜像 tar（按服务分文件）+ kustomize manifests + SQL 增量 + MANIFEST`；单节点直接 import，不强依赖私有仓库；扩节点时启用附带的 registry manifests；
- 端口语义保留（7010/8180 对外，LoadBalancer/NodePort 固定端口，禁用 Traefik）；
- 单 namespace `info-serve`：k8s DNS 短名（`postgres:5432` 等）与现有 Nacos prod 配置写法兼容，配置改动最小化；
- 现有规约不变：SQL 三件套（种子 + `deploy/updates` 增量 + MANIFEST 登记）；Nacos 配置由 `generate-initdb.py` 生成，新增 k3s 场景。

### 9.3 数据备份（本轮必须交付）

内网离线环境数据丢失不可恢复，备份为批次 C 验收项：

- k3s CronJob 每日执行：PostgreSQL `pg_dump`、MySQL(Nacos 库) dump、Nacos 配置导出、MinIO 数据同步；
- 落独立备份卷（与数据卷分离的磁盘路径）；保留策略：日备 7 份 + 周备 4 份；
- 恢复演练纳入批次 C 故障演练清单。

### 9.4 观测性（渐进启用）

- **本轮启用**：各服务打开 `ruoyi-common-prometheus`（解注释依赖）暴露 metrics 端点；k3s 内置 metrics-server 支持 `kubectl top`；ruoyi-monitor（Spring Boot Admin）保留并配置告警通知；
- **触发再上**：Grafana/Prometheus Server/Loki/链路追踪，触发器为多团队跨服务排障出现扯皮、或单靠 SBA 无法定位生产问题。

### 9.5 开发环境

Windows/Mac 继续 docker compose；compose 按服务拆分文件并配启停脚本，开发环境同样可单服务起停。本地 Java 调试连 Nacos `dev` namespace 的模式不变。

## 10. 实施路线（四批次，每批独立可验收）

| 批次 | 内容 | 验收门禁 |
|---|---|---|
| **A 代码拆分** | portal → 5 服务；`ruoyi-api-portal-kernel`；RabbitMQ 事件 + 通知消费 + 转换队列；统计聚合降级；网关路由与兼容路由核对；每服务 Dockerfile；compose 拆分 + 启停脚本；ArchUnit 调整；PORTS.md/RUNBOOK 更新 | 全量编译 + 冒烟通过；**杀掉任一 BC 服务，其余功能正常** |
| **B arm64 制品** | buildx 多架构流水；temurin arm64 基座选型；LibreOffice arm64 验证；基础设施 arm64 镜像锁版；离线镜像打包脚本；JVM arm64 调优基线 | arm64 镜像全量构建成功；文档转换冒烟通过 |
| **C k3s 上线** | 麒麟预检脚本；k3s 离线安装；kustomize manifests + 探针 + 限额 + PriorityClass；svc.sh；**备份 CronJob 与恢复演练**；prometheus 端点启用；发布包 v2 | 故障演练：逐个杀业务 Pod / 杀基础设施 Pod / 内存压测，验证隔离与自愈；备份可恢复 |
| **D 契约与文档** | 网关 OpenAPI 聚合；OpenAPI 离线导出；第三方接入指南；事件目录；**网关限流上线**；bounded-contexts v4 修订 | 模拟第三方视角走通全部接入流程；限流策略生效验证 |

## 11. 风险登记

| 风险 | 应对 |
|---|---|
| LibreOffice arm64 兼容性 | 批次 B 首个验证点；不通过则评估 arm64 原生替代方案或转换服务独立 x86 节点 |
| 飞腾单核弱，JVM 启动慢/吞吐低 | startup 探针 180s；批次 B 出压测基线；必要时调整副本与限额 |
| 团队 k8s 技能空窗 | svc.sh 覆盖 80% 日常操作；至少 1 人掌握 kubectl 排障基本功；单节点起步降低复杂度 |
| 离线包体积（arm64 全量镜像数 GB） | 按服务分文件打包，支持增量发布（仅变更服务的镜像 tar） |
| appcenter/forum 拆分为组织性拆分 | 若多服务运维成本高于收益，这两个服务是既定的首批合回候选（决策已留档） |

## 12. 与 bounded-contexts v3 的关系

**继续有效**：统一语言（§2）、表前缀归属（§7.2）、内核能力参数化（§7.3）、模块注册表与入口层原则（§5）。

**由本文修订**（批次 D 落为 v4）：

- §4 部署裁决：触发器③④命中，全体 BC 晋升为独立服务；
- kernel 由「共享模块，永不独立成服务」修订为「门户内核服务」——多团队以独立服务接入时，内核能力必须以网络契约的唯一实例存在，否则每个新服务会再复制一份内核，「内核×2」病灶将以更大规模复发；
- §7 边界纪律：包级 ArchUnit 约束升级为物理模块隔离（编译期强制）+ 通信红线（§3）。

## 13. 决策记录

- [x] 全拆：五 BC 各自成服务，kernel 独立为通用服务（2026-07-07）
- [x] 部署形态：k3s 单节点 · 麒麟 V10 · 飞腾 2000 · arm64 · 离线（2026-07-07）
- [x] 基础设施进 k3s 统一管理（换取统一自愈与单一管理面）（2026-07-07）
- [x] 服务器不装 Docker Engine，构建机/开发机保留 Docker（2026-07-07）
- [x] seata/workflow/gen 主动排除，理由见 §4.4（2026-07-07）
- [x] snail-job 预留不启用，触发器见 §4.3（2026-07-07）
- [x] 备份 CronJob、prometheus 端点、网关限流合入本轮批次（2026-07-07 评审修订）
- [x] 观测性重组件（Grafana/Loki/Tracing）触发再上（2026-07-07）

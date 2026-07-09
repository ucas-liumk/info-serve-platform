# 批次 A：portal 五 BC 服务拆分 实施计划（主文档）

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.
> 任务正文在同目录 `task-01` ~ `task-12` 文件中，**必须按编号顺序执行**；每个任务文件自包含。

**Goal:** 把 `ruoyi-modules/ruoyi-portal` 单体拆成 5 个独立微服务（portal-kernel / portal-appcenter / portal-forum / portal-requiredknowledge / portal-resources），通知走 RabbitMQ、统计走 Dubbo 降级聚合、文档转换走工作队列，网关按深层路径分流做到**前端零改动**，并为每个服务提供独立镜像与独立 compose 启停。

**Architecture:** 先单体内解耦（消息中心归位 kernel → 通知 MQ 化 → 统计 Dubbo 化 → 转换队列化），每步保持单体可运行可验证；再按「RK 试点 → kernel → forum → resources → appcenter+删单体」顺序做纯物理搬迁。依据 spec `docs/superpowers/specs/2026-07-07-service-split-cloud-native-design.md`。

**Tech Stack:** Spring Boot 3.5.15 / Spring Cloud 2025.0.3 / RuoYi-Cloud-Plus 2.6.2 · Nacos（注册+配置，dev/prod 双 namespace）· Dubbo 3（端口自增）· RabbitMQ（spring-boot-starter-amqp，broker 已就位于 compose）· PostgreSQL 单库 `ry-cloud` · MyBatis-Plus 纯注解（无 XML）。

## Global Constraints（每个任务隐含遵守）

1. **分支纪律**：全部工作在分支 `feature/batch-a-service-split`（从最新 `origin/main` 拉出）上进行；`main` 只进合并；每任务至少一次提交，消息格式 `<type>: <中文描述>`，**禁止任何署名尾注**。
2. **编译门禁**：任何后端改动后必过 `mvn -o -ntp -Pdev -DskipTests -pl <模块> -am compile`（在 `source/` 目录下执行；离线失败去掉 `-o`）。
3. **ArchUnit 门禁**：`ruoyi-portal` 存续期间（T1–T9），凡动过它必跑 `mvn -ntp -pl ruoyi-modules/ruoyi-portal -am -DskipTests=false test`。
4. **Nacos 纪律**：配置正本是 `source/script/config/nacos/*`；新增/修改 data-id 必须①改正本文件 ②同步 `deploy/scripts/generate-initdb.py` 的 `new_configs` 数组（第 84 行）③对**运行中的环境**用 `deploy/scripts/nacos-publish.sh`（T2 创建）经 OpenAPI 发布 dev+prod 双 namespace——**严禁 SQL 直写 config_info**。
5. **本批次零 schema 变更**：不建表不改表（通知复用 `app_message`，幂等用 Redis）。若执行中发现需要建表，停下来重新评审。
6. **单文件 ≤800 行**；新代码风格与 RuoYi 惯例一致（Lombok `@Data`/`@RequiredArgsConstructor`、`R<T>` 返回、`TableDataInfo` 分页）。
7. **前端零改动**：plus-ui 的所有 API 字面路径（`/appcenter/**`、`/infoservice/**`）与 HTTP 方法保持原样；任何任务不得修改 plus-ui。
8. **Dubbo 红线**：`ruoyi-api-*` 契约一旦被消费，后续变更需提供方+全部消费方同批重建镜像（AGENTS.md §7）。
9. 端口/命名总表（全计划唯一权威，各任务不得偏离）：

| 服务 | Maven 模块 | spring.application.name / Nacos data-id / lb:// | HTTP 端口 | 镜像 |
|---|---|---|---:|---|
| 门户内核 | `ruoyi-modules/ruoyi-portal-kernel` | `portal-kernel` / `portal-kernel.yml` | 8107 | `infosys/ruoyi-cloud-plus-portal-kernel:2.6.2` |
| 应用中心 | `ruoyi-modules/ruoyi-portal-appcenter` | `portal-appcenter` | 8106 | `infosys/ruoyi-cloud-plus-portal-appcenter:2.6.2` |
| 论坛 | `ruoyi-modules/ruoyi-portal-forum` | `portal-forum` | 8108 | `infosys/ruoyi-cloud-plus-portal-forum:2.6.2` |
| 应知应会 | `ruoyi-modules/ruoyi-portal-requiredknowledge` | `portal-requiredknowledge` | 8109 | `infosys/ruoyi-cloud-plus-portal-requiredknowledge:2.6.2` |
| 资料共享 | `ruoyi-modules/ruoyi-portal-resources` | `portal-resources` | 8111 | `infosys/ruoyi-cloud-plus-portal-resources:2.6.2` |
| 契约 | `ruoyi-api/ruoyi-api-portal-kernel` | —（jar） | — | — |
| MQ 公共 | `ruoyi-common/ruoyi-common-portal-event` | —（jar） | — | — |

Java 包名保持 `org.dromara.portal.<bc>` 不变（整包 git mv，diff 最小）。

## MQ / Dubbo 契约总表（跨任务接口，T1 定义，后续消费）

- Exchange `portal.topic`（topic/durable），死信 `portal.dlx`（direct/durable）。
- Routing key：`appcenter.demand.replied` · `appcenter.application.published` · `forum.reply.created` · `resources.item.published`。
- 队列：`portal-kernel.notifications`（+`.dlq`）；`portal-resources.convert`（+`.dlq`）。
- 事件体：`org.dromara.portal.api.event.PortalNotificationEvent`（eventId/occurredAt/scene/title/content/targetType[ALL|USERS]/targetUserIds）。
- Dubbo：`org.dromara.portal.api.RemoteModuleStatsService#stats()` → `RemoteModuleStatsVo{moduleCode, Map<String,Long> metrics}`，提供方 `@DubboService(group=<模块名>)`，group 取 `appcenter|forum|resources`；消费方 kernel `@DubboReference(group=…, timeout=2000, check=false)` + try/catch 降级。
- 指标键（`org.dromara.portal.api.PortalStatsMetrics`）：`appCount`、`topicCount`、`activeAuthorCount`、`resourceCount`、`visitCount`。

## 网关目标路由表（终态，T10 达成；过渡期各任务只加自己的路由）

所有新路由 `StripPrefix=1` 不变，`order` 保证具体路径先于宽路径命中：

| id | uri | Path 断言 | order |
|---|---|---|---:|
| portal-kernel-messages | lb://portal-kernel | `/appcenter/portal/messages/**` | -10 |
| portal-kernel | lb://portal-kernel | `/infoservice/portal/stats`, `/infoservice/portal/modules/**`, `/infoservice/module/**` | -10 |
| portal-forum | lb://portal-forum | `/infoservice/forum/**`, `/infoservice/portal/forum/**` | -5 |
| portal-requiredknowledge | lb://portal-requiredknowledge | `/infoservice/required-knowledge/**`, `/infoservice/portal/required-knowledge/**` | -5 |
| portal-resources | lb://portal-resources | `/infoservice/resource/**`, `/infoservice/portal/resources/**` | -5 |
| portal-appcenter | lb://portal-appcenter | `/appcenter/**` | 0 |

原 `ruoyi-portal-appcenter`/`ruoyi-portal-infoservice` 两条旧路由在 T10 删除。auth/system/file 路由不动。

## 执行环境策略（2026-07-07 补充，subagent 必读）

本机（Mac mini，24GB）**不跑全栈运行时**（17 容器 ≈15GB+ 超出 Docker Desktop 虚拟机合理配额；且本机无 mvn，构建一律走 Docker Maven 容器）。执行分工：

- **Mac 执行**：全部代码改动、`docker run … maven:3.9-eclipse-temurin-17 mvn …` 编译/单测门禁、`bash -n` 与 `docker compose config` 静态校验、git 提交。任务文件里的 `mvn -o -ntp …` 命令一律替换为等价 Docker Maven 形式：
  `docker run --rm -v "$PWD/source":/workspace -v /Users/macmini/.m2:/root/.m2 -w /workspace maven:3.9-eclipse-temurin-17 mvn -ntp <原参数>`
  （执行前确保 Docker Desktop 已启动；仅 Maven 容器，内存无压力。）
- **Windows 正本机执行（ssh info-serve-win，规范见 lab-remote 技能）**：所有「运行时验证」步骤——nacos-publish 发布、build-images、compose 起停、curl 冒烟、e2e、故障演练。收拢为三个检查点，由主会话（编排者）亲自驱动，不派给任务 subagent：
  - **CP1**（T2 完成后）：RK 试点全链路（T2 Step 8-11）；
  - **CP2**（T7 完成后）：kernel 拆出冒烟 + 首次跨进程 MQ/Dubbo 链路（T4 Step 8 手验、T7 Step 5-6）；
  - **CP3**（T10/T11 完成后）：= T12 全量验收（T8/T9/T10 各自的运行时冒烟并入此处一次做完）。
- 任务 subagent 遇到「运行时验证」步骤时：完成其静态部分（文件产出、config 校验），把运行时部分标注「移交检查点」后即可结束任务——**不要在 Mac 上尝试起全栈**。
- Windows 侧检查点流程：`git fetch origin && git switch feature/batch-a-service-split && git pull` → 按任务步骤执行（bash 脚本经 Git Bash；不可用时 nacos-publish 改 Nacos 控制台手工发布，内容以 `source/script/config/nacos/*` 为准）。

## 任务索引与依赖

| 任务 | 内容 | 前置 |
|---|---|---|
| T1 `task-01` | 契约模块 `ruoyi-api-portal-kernel` + MQ 公共模块 `ruoyi-common-portal-event` | — |
| T2 `task-02` | **RK 试点拆出**：新服务模板全链路（模块/Nacos/网关/镜像/compose/svc.sh/nacos-publish.sh） | T1 |
| T3 `task-03` | 单体内：消息中心从 appcenter 归位 kernel + 昵称解析器下放 + ArchUnit 收紧① | — |
| T4 `task-04` | 单体内：4 个通知点 MQ 化 + kernel 消费者（DLQ/幂等）+ ArchUnit 收紧② | T1,T3 |
| T5 `task-05` | 单体内：统计 Dubbo 化（3 提供方 + kernel 降级聚合）+ ArchUnit 收紧③ | T1 |
| T6 `task-06` | 单体内：文档转换队列化 + 转换逻辑抽类（解 800 行红线） | T1 |
| T7 `task-07` | kernel 物理拆出（含消息中心、MQ 消费者、统计聚合） | T2–T6 |
| T8 `task-08` | forum 物理拆出 | T7 |
| T9 `task-09` | resources 物理拆出（LibreOffice 专用镜像） | T7 |
| T10 `task-10` | appcenter 物理拆出 + **删除 ruoyi-portal** + 网关终态路由 | T8,T9 |
| T11 `task-11` | compose 全量核对 + svc.sh 完善 + PORTS/RUNBOOK/AGENTS/CLAUDE 文档更新 | T10 |
| T12 `task-12` | 全链路验收：全量构建 → 镜像 → compose up → e2e 冒烟 → **杀服务故障演练** | T11 |

## 对 spec 的两处明示修订（已在计划中落实，验收时向用户说明）

1. spec §5 列的 `RemoteMessageService/RemoteFavoriteService/RemoteModuleRegistryService/RemotePortalUserService` 四个 Dubbo 契约**本批不实现**：通知改走 MQ 后它们在批次 A 没有任何消费方，提前定义违反 YAGNI 且触发 Dubbo 同批重建红线。留到批次 D（第三方接入）按真实消费方定义。
2. spec §5 的「批次 A 核对 controller 前缀（目标 `/portal/<bc>/**`）」侦察后确认**现状不满足且不能免费达成**（前端字面路径是 `/appcenter/**`、`/infoservice/**`）。本批按「前端零改动」优先，网关深层路径分流；URL 规范化登记为批次 D 债务。

## 验收总门禁（T12 详述）

1. `source/` 全量 `package` 成功（新 `-pl` 列表见 T11 更新后的 RUNBOOK）。
2. `python3 deploy/scripts/generate-initdb.py` 成功且产物含 5 个新 data-id。
3. `docker compose --env-file .env config --quiet` 通过（含 5 个新服务文件）。
4. e2e 冒烟通过：`APP_CENTER_BASE_URL=http://127.0.0.1:7010/prod-api node deploy/scripts/appcenter-v1-e2e.mjs`。
5. **故障演练**：逐个 `svc.sh stop portal-<bc>`，验证其余模块正常、首页统计对应卡片降级为 0 而非报错；重启后恢复。
6. RabbitMQ 管理页（8173）可见 exchange/队列/DLQ，通知链路端到端可用。

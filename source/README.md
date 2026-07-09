# info-serve 后端（source/）

基于 [RuoYi-Cloud-Plus](https://gitee.com/dromara/RuoYi-Cloud-Plus) v2.6.2 裁剪二开的后端源码，MIT 协议（见 [LICENSE](./LICENSE)）。

## 与上游的差异

- **保留**：gateway、auth、system、file（文件服务，由 ruoyi-resource 改名）、monitor、nacos。
- **新增业务模块**：门户五个限界上下文各为独立服务（批次 A 自原 `ruoyi-portal` 单体拆出）——`ruoyi-portal-kernel`（门户内核：消息通知/收藏/统计聚合/模块注册表）、`ruoyi-portal-appcenter`（应用中心）、`ruoyi-portal-forum`（服务论坛）、`ruoyi-portal-requiredknowledge`（应知应会）、`ruoyi-portal-resources`（资料共享）。跨界隔离由 Maven 模块强制，跨 BC 协作走 RabbitMQ `portal.topic` 事件；对外 API 路径保持 `/appcenter/**`、`/infoservice/**` 兼容。
- **已移除**：gen（代码生成）、job/snailjob（任务调度）、workflow/warm-flow（工作流）、seata-server、gateway-mvc、example/demo 及其配置、路由、数据库。
- 业务数据库为 PostgreSQL（`ry-cloud`）；MySQL 仅作 Nacos 配置库（`ry-config`）。

## 快速入口

- 架构与限界上下文：[`../docs/architecture/bounded-contexts.md`](../docs/architecture/bounded-contexts.md)
- 部署与协作纪律：[`../RUNBOOK.md`](../RUNBOOK.md)
- 端口清单：[`../PORTS.md`](../PORTS.md)
- 版本号唯一来源：[`../VERSION`](../VERSION)

## 本地构建

```bash
# 编译（Docker Maven，JDK 17）
docker run --rm -v "$PWD":/workspace -v ~/.m2:/root/.m2 -w /workspace \
  maven:3.9-eclipse-temurin-17 mvn -ntp -Pdev -DskipTests compile
```

完整构建与镜像打包见 `../RUNBOOK.md`。

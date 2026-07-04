# info-serve 后端（source/）

基于 [RuoYi-Cloud-Plus](https://gitee.com/dromara/RuoYi-Cloud-Plus) v2.6.2 裁剪二开的后端源码，MIT 协议（见 [LICENSE](./LICENSE)）。

## 与上游的差异

- **保留**：gateway、auth、system、resource（文件服务）、monitor、nacos。
- **新增业务模块**：`ruoyi-modules/ruoyi-portal` 门户业务服务，内部按限界上下文分包——`kernel`（门户内核：消息通知/统计）、`appcenter`（工具即用）、`resources`（资料共享）、`forum`（服务论坛）。对外 API 路径保持 `/appcenter/**`、`/infoservice/**` 兼容。
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

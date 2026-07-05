# Hop 门户应用态势管道

本目录保存可版本化的 Apache Hop 工程资产。第一阶段只做门户应用态势分析表刷新，不直接服务门户页面，也不让 DataEase 读取原始业务表。

## 目录

| 路径 | 说明 |
|---|---|
| `info-serve-portal-analytics/` | Hop 工程目录，可复制到 Hop Web 的 `/files` 挂载目录 |
| `info-serve-portal-analytics/pipelines/refresh-portal-usage-analytics.hpl` | 刷新门户应用态势分析表的 Hop pipeline |
| `info-serve-portal-analytics/metadata/rdbms/info-serve-portal.json` | 变量化 PostgreSQL 连接元数据 |
| `run-portal-analytics.sh` | 将工程同步到 Hop Web 容器并执行 pipeline |
| `verify-portal-analytics.sh` | 查询分析表和最近作业记录，用于执行后验收 |

## 执行

默认依赖已启动的 Hop Web 容器 `dataplatform-hop-web`，并要求该容器挂载：

```bash
/Users/macmini/DataPlatform/hop/projects:/files
```

执行刷新：

```bash
deploy/situation/hop/run-portal-analytics.sh
```

验证结果：

```bash
deploy/situation/hop/verify-portal-analytics.sh
```

脚本默认使用 Docker 网络内的 Postgres 服务名：

```bash
INFO_SERVE_DB_HOST=postgres
INFO_SERVE_DB_PORT=5432
INFO_SERVE_DB_NAME=ry-cloud
INFO_SERVE_DB_USER=ruoyi
```

密码不要写入仓库。需要覆盖时通过环境变量注入：

```bash
INFO_SERVE_DB_PASSWORD='***' deploy/situation/hop/run-portal-analytics.sh
```

## Hop UI 查看

`portal_analytics_*` 是写入 PostgreSQL 的分析表，不会作为 Hop UI 首页资产自动出现。Hop UI 负责维护和执行治理管道，表数据应在 DataEase 数据源或 PostgreSQL 客户端中查看。

执行 `run-portal-analytics.sh` 后，脚本会把工程注册到 Hop Web 的项目列表，并设为默认启动项目。打开 `http://127.0.0.1:18091/ui` 并刷新页面后，左下角应显示项目：

```text
info-serve-portal-analytics
```

如果 Hop Web 手动切换项目时报 `SWTException: Invalid thread access`，刷新页面即可回到该默认项目，不需要在 UI 中反复切换。

该项目下的关键资产：

- `pipelines/refresh-portal-usage-analytics.hpl`：刷新门户应用态势分析表的管道。
- `metadata/rdbms/info-serve-portal.json`：连接门户 PostgreSQL 的元数据。
- `logs/`：每次刷新产生的执行日志。

## 口径

- `portal_analytics_app_rank`：按 `app_application.use_count` 形成应用热度排行。
- `portal_analytics_resource_rank`：按 `info_resource.view_count + download_count` 形成资料流动排行。
- `portal_analytics_feedback_summary`：按 `app_demand.demand_type/status` 汇总需求反馈。
- `portal_analytics_module_daily`：按应用、资料、论坛、通知四类模块汇总活跃、访问、动作和参与用户。
- `portal_analytics_metric_snapshot`：生成累计使用、启用应用、资料资产、活跃参与、待处理反馈等大屏指标。
- `portal_analytics_job_run`：记录 Hop 执行结果、耗时和影响行数。

该管道是第一阶段的“可跑通治理链路”。后续如果增加更细的行为事件表，可把 SQL 口径替换为事件明细聚合，DataEase 大屏和门户入口不需要改。

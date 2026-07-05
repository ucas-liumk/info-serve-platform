# DataEase + Hop 态势引擎接入说明

本目录说明 info-serve 侧如何接入外部态势引擎。DataEase 和 Apache Hop 均按独立产品部署，不纳入 `deploy/docker-compose.yml` 主业务栈，避免其升级、初始化和资源占用影响门户主链路。

## 1. 组件定位

| 组件 | 定位 | 当前接入方式 |
|---|---|---|
| DataEase | 态势大屏制作后台 | 门户跳转到已发布大屏链接 |
| Apache Hop | 数据加工与治理工具 | 读统计接口或中间结果，写 `portal_analytics_*` |
| PostgreSQL `portal_analytics_*` | DataEase 稳定读取源 | 由 0.3.5 增量脚本创建 |
| OpenMetadata / DataHub | 后续元数据平台 | 暂不上，未来采集分析表、指标目录和作业日志 |

## 2. 门户链接配置

前端会优先读取运行时配置文件：

```js
// plus-ui/public/app-config.js
window.__INFO_SERVE_CONFIG__.dataeaseDashboardUrl = "http://127.0.0.1:8100/link/your-dashboard";
```

如果该值为空，则退回构建期环境变量：

```bash
VITE_APP_DATAEASE_DASHBOARD_URL="http://127.0.0.1:8100/link/your-dashboard"
```

生产部署优先改 `dist/app-config.js` 或源文件 `plus-ui/public/app-config.js` 后重新发布静态文件。不要为了换一个 DataEase 大屏链接就修改 Vue 源码。

## 3. DataEase 数据源授权

DataEase 不应连接原始业务表，只读分析表和指标目录。

推荐只读账号授权：

```sql
CREATE ROLE dataease_reader LOGIN PASSWORD 'change-me';
GRANT CONNECT ON DATABASE "ry-cloud" TO dataease_reader;
GRANT USAGE ON SCHEMA public TO dataease_reader;
GRANT SELECT ON TABLE
    portal_metric_catalog,
    portal_analytics_metric_snapshot,
    portal_analytics_module_daily,
    portal_analytics_app_rank,
    portal_analytics_resource_rank,
    portal_analytics_feedback_summary,
    portal_analytics_job_run
TO dataease_reader;
```

DataEase 中只配置该账号，不配置业务库管理员账号。

## 4. Hop 管道约定

Hop 第一阶段只负责加工和写入分析表，不直接改业务表。

建议管道命名：

| 管道 | 写入目标 |
|---|---|
| `MODULE_ACTIVE_DAILY` | `portal_analytics_module_daily` |
| `APP_HOT_RANK` | `portal_analytics_app_rank` |
| `RESOURCE_FLOW_RANK` | `portal_analytics_resource_rank` |
| `FEEDBACK_SUMMARY` | `portal_analytics_feedback_summary` |
| `METRIC_SNAPSHOT` | `portal_analytics_metric_snapshot` |

每次运行必须写入 `portal_analytics_job_run`，记录 `job_code`、`run_status`、耗时、影响行数和错误信息。

## 5. 端口建议

| 服务 | 建议宿主机端口 | 说明 |
|---|---:|---|
| DataEase | `8100` | DataEase 官方默认 Web 端口 |
| Hop Web | `8174` | 建议将 Hop Web 容器 `8080` 映射为宿主机 `8174` |

## 6. 后续接 OpenMetadata

当前已经预留以下采集对象：

- `portal_metric_catalog`：指标目录、口径、负责人、刷新周期。
- `portal_analytics_*`：DataEase 读取的数据资产。
- `portal_analytics_job_run`：数据治理任务运行事实。

如果未来要求图表级血缘，优先评估 DataEase API/数据库采集；若要求尽量使用 OpenMetadata 原生 dashboard connector，再评估是否将展示层切换为 Superset。

## 7. 官方资料

- DataEase 安装：https://dataease.cn/docs/v2/installation/online_installation/
- DataEase 嵌入：https://dataease.cn/docs/v2/embedded/iframe/
- Apache Hop Docker：https://hop.apache.org/tech-manual/latest/docker-container.html
- Apache Hop Web：https://hop.apache.org/manual/latest/hop-gui/hop-web.html

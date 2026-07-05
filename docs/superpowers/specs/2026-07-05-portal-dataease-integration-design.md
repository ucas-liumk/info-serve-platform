# DataEase 态势大屏集成设计

日期：2026-07-05

## 1. 背景与目标

当前项目需要给单位一把手提供数字化转型态势入口，但第一阶段尚未形成需求管理、任务办理、问题闭环等完整业务闭环。自研大屏界面投入高、审美风险高，且后续需要支持数据治理与元数据演进。因此第一阶段采用成熟开源大屏引擎承载展示，门户只提供稳定入口。

目标：

- DataEase 作为态势大屏制作后台，承载领导可访问的成熟大屏界面。
- 门户首页通过 `portal_module` 注册“应用态势”入口，点击跳转到已发布的大屏。
- 增加轻量态势数据治理层，为 DataEase 提供稳定、可治理的数据集。
- 第一阶段不上 OpenMetadata / DataHub，但预留后续元数据、指标口径、数据血缘接入点。
- 保留未来切换 Superset 的可能，不让门户或业务层绑定 DataEase 私有实现。

## 2. 技术选型结论

| 层次 | 当前选择 | 定位 | 后续替换/扩展 |
|---|---|---|---|
| 大屏展示 | DataEase | 中文化、低代码、领导态势大屏制作后台 | 如图表级元数据血缘成为强诉求，可评估 Superset |
| 数据加工 | Apache Hop | 可视化 ETL、指标加工、分析表刷新 | 可叠加 DolphinScheduler / SeaTunnel |
| 分析数据层 | PostgreSQL `portal_analytics_*` | DataEase 唯一读取源 | 数据量增长后可迁移 Doris / StarRocks |
| 元数据治理 | 暂不上 | 当前只预埋指标目录和作业日志 | 后续接 OpenMetadata，必要时再接 DataHub |

DataEase 不作为业务后端，不直接承接门户鉴权、业务办理或数据维护。Apache Hop 不替代元数据平台，只负责把业务统计契约加工成分析数据集。OpenMetadata / DataHub 后续接入时以分析表、指标目录、作业日志为采集对象。

## 3. 总体架构

```text
ruoyi-portal 内容 BC
  appcenter / resources / forum / kernel
        ↓ service 统计契约
态势数据治理层
  Hop 管道 + portal_analytics_* 分析表 + portal_metric_catalog
        ↓ 只读账号 / 稳定数据集
DataEase
  数据大屏、仪表板、公共链接或内网链接
        ↓
门户首页“应用态势”
  portal_module 入口跳转到成熟大屏
```

边界原则：

- DataEase 不查 `app_`、`res_`、`forum_`、`portal_` 原始业务表。
- DataEase 只读 `portal_analytics_*` 分析表或后续稳定 API 数据集。
- 跨 BC 取数仍由 `ruoyi-portal` 内核通过各 BC 的 service 接口完成，不新增跨界裸 SQL。
- 门户只保存 DataEase 大屏入口配置，不保存 DataEase 图表定义。

## 4. 门户集成设计

新增门户模块：

| 字段 | 建议值 |
|---|---|
| `module_code` | `usage-dashboard` |
| `module_name` | `应用态势` |
| `description` | `运行洞察  转型透明` |
| `entry_path` | `/portal/usage-dashboard` |
| `status` | `0` |
| `sort_order` | 建议排在工具、资料、论坛之前或之后，由后台可调 |

新增门户路由 `/portal/usage-dashboard`，页面只负责外部跳转或嵌入策略：

- 默认策略：打开 DataEase 已发布的大屏链接，优先新标签页。
- 可选策略：在门户内 iframe 嵌入，前提是 DataEase 链接允许被同源策略或反向代理安全嵌入。
- 未配置链接时：显示运维态提示，指向后台配置项，不展示自研大屏。

配置来源建议分阶段：

1. 第一版：前端环境变量或 Nacos 配置提供 `dataease.dashboardUrl`。
2. 后续：后台模块注册表扩展外部链接配置，或增加 `portal_external_entry` 配置表。

## 5. 数据治理层设计

第一阶段增加面向态势屏的分析表，而不是开放原始业务表。

建议表：

| 表名 | 用途 |
|---|---|
| `portal_metric_catalog` | 指标目录：编码、名称、口径、负责人、刷新频率、来源说明 |
| `portal_analytics_metric_snapshot` | 指标快照：累计使用、活跃用户、资料资产、反馈量等 |
| `portal_analytics_module_daily` | 模块日活跃趋势 |
| `portal_analytics_app_rank` | 应用热度排行 |
| `portal_analytics_resource_rank` | 资料流动排行 |
| `portal_analytics_feedback_summary` | 需求反馈统计 |
| `portal_analytics_job_run` | 数据加工任务运行记录 |

所有分析表需要包含：

- `tenant_id`：支持租户隔离。
- `stat_date` 或 `stat_time`：支持按日/按时点刷新。
- `metric_code`：能和指标目录关联。
- `source_version`：口径变化时可追踪。
- `create_time` / `update_time`：支撑审计。

Hop 第一阶段职责：

- 调用 ruoyi-portal 统计接口或读取授权后的中间结果。
- 执行清洗、汇总、排行、快照写入。
- 记录每次管道运行状态、耗时、影响行数和错误信息。

## 6. 元数据预留

当前不上 OpenMetadata / DataHub，但必须避免后续返工。

预留规则：

- 所有 `portal_analytics_*` 表和字段写清数据库 comment。
- 每个指标必须进入 `portal_metric_catalog`，不得只存在于 DataEase 图表 SQL 中。
- Hop 管道文件进入版本管理或部署包归档，命名与指标编码一致。
- DataEase 图表命名引用指标编码，例如 `METRIC_USAGE_TOTAL_累计使用次数`。
- DataEase 只读账号只授予分析表权限，不授予原始业务表权限。

后续接 OpenMetadata 时：

- 直接采集 PostgreSQL 分析表、字段、comment、owner。
- 采集 `portal_metric_catalog` 作为业务指标目录。
- 采集 `portal_analytics_job_run` 作为作业运行事实。
- DataEase 图表级血缘若无官方 connector，则通过 DataEase API / 数据库二次采集或人工登记补齐。

后续若切换 Superset：

- 保留 `portal_analytics_*` 和 `portal_metric_catalog` 不变。
- 门户入口配置从 DataEase 链接替换为 Superset dashboard 链接。
- Hop 管道和分析表不变，迁移成本集中在展示层。

## 7. 安全与权限

- DataEase 大屏链接第一阶段仅展示汇总数据，不放个人明细、敏感附件、未脱敏字段。
- DataEase 使用独立只读数据库账号。
- 只读账号授权范围限制为 `portal_analytics_*` 和 `portal_metric_catalog`。
- 公共链接必须评估有效期、访问密码、内网访问范围。
- 若启用 iframe 嵌入，需要明确反向代理、X-Frame-Options、CSP 与登录态边界。

## 8. 实施阶段

### 阶段 1：门户入口与外部引擎接入

- 新开干净分支实现。
- 注册 `usage-dashboard` 门户模块。
- 新增 `/portal/usage-dashboard` 路由，仅做跳转/未配置态。
- 增加 DataEase、Hop 的部署说明和端口说明。
- 不自研态势大屏 UI。

### 阶段 2：分析表与 Hop 管道

- 增加 `portal_metric_catalog` 与第一批 `portal_analytics_*` 表。
- 补齐种子 SQL、增量 SQL、`MANIFEST.md`。
- 提供第一批统计接口或治理任务。
- DataEase 连接分析表制作大屏。

### 阶段 3：元数据治理增强

- 根据实际数据资产规模决定是否接 OpenMetadata。
- 如果强依赖图表级血缘，再评估 Superset 或 DataEase connector 定制。

## 9. 验证计划

设计文档阶段：

- `git diff --check`

实现阶段：

- 前端：`cd plus-ui && npm run build:prod`
- 涉及视觉：`cd plus-ui && npm run design:audit`
- 涉及后端：`mvn -o -ntp -Pdev -DskipTests -pl ruoyi-modules/ruoyi-portal -am compile`
- 涉及 ruoyi-portal：`mvn -pl ruoyi-modules/ruoyi-portal -am -DskipTests=false test`
- 涉及 deploy：`docker compose --env-file .env config --quiet`
- 行为验证：门户首页能看到“应用态势”，点击后进入配置的 DataEase 大屏或明确未配置态。

## 10. 风险与约束

- DataEase 与 OpenMetadata / DataHub 暂无确认的官方原生 dashboard connector，图表级血缘后续可能需要定制。
- DataEase 公共链接适合汇总态势，不适合暴露敏感明细。
- 直接让 DataEase 查询原始业务表会破坏 BC 边界，不作为正式方案。
- Hop 管道如果只存在于运行环境、不进入版本管理，会削弱可追溯性，必须纳入交付资产。

## 11. 参考资料

- DataEase GitHub：https://github.com/dataease/dataease
- DataEase 安装文档：https://dataease.cn/docs/v2/installation/online_installation/
- DataEase iframe 嵌入：https://dataease.cn/docs/v2/embedded/iframe/
- Apache Hop：https://hop.apache.org/
- Apache Hop Docker：https://hop.apache.org/tech-manual/latest/docker-container.html
- OpenMetadata Dashboard Connectors：https://docs.open-metadata.org/v1.12.x/connectors/dashboard
- OpenMetadata Superset Connector：https://docs.open-metadata.org/v1.12.x/connectors/dashboard/superset
- Apache Superset：https://superset.apache.org/

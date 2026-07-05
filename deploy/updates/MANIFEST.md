# deploy/updates 迁移清单

本目录是项目业务迁移 SQL 的**唯一存放处**。命名规则：`<版本>-<主题>.sql`。

与全新安装的关系：新装机 schema 由 `source/script/sql/postgres/` 种子文件经 `deploy/scripts/generate-initdb.py` 产出，**不执行**本目录；本目录只服务存量环境升级。任何 schema 变更必须同时更新种子与增量两处（种子管新装、增量管存量）。

## 版本映射

| 版本 | 文件 | 目标库 | 状态 |
|---|---|---|---|
| 0.3.0 | `0.3.0-info-serve-menu.sql` | PG `ry-cloud` | 已投放（原 `source/script/sql/update/postgres/update_info_serve_menu_20260628.sql`，2026-07-03 迁入） |
| 0.3.0 | `0.3.0-app-demand.sql` | PG `ry-cloud` | 已投放（原 `update_app_demand_20260628.sql`） |
| 0.3.0 | `0.3.0-force-password-change.sql` | PG `ry-cloud` | 已投放（原 `update_force_password_change_20260628.sql`） |
| 0.3.3 | `0.3.3-resource-sharing-fixes.sql` | PG `ry-cloud` | 已随 0.3.3 包投放 |
| 0.3.3 | `0.3.3-auth-token-timeout.sql` | PG `ry-cloud` | 已随 0.3.3 包投放 |
| 0.3.3 | `0.3.3-nacos-common-config.sql` | MySQL `ry-config` | 已随 0.3.3 包投放（此前仅存在于发布包内、仓库无源，2026-07-03 回收入库） |
| 0.3.3 | `0.3.3-portal-notifications.sql` | PG `ry-cloud` | ⚠️ **未随任何包投放**（0.3.3 装包遗漏）。内容为"版本升级"通知种子，随下次更新决定补投或作废 |
| 0.3.4 | `0.3.4-admin-menu-modules.sql` | PG `ry-cloud` | 待投放。后台管理菜单按一级模块展示；应知应会含题库管理、考试配置、OCR 导入二级菜单 |
| 0.3.4 | `0.3.4-appcenter-categories.sql` | PG `ry-cloud` | 待投放。工具即用更名为应用中心；应用分类调整为自研/开源/离线；应知应会归入自研应用 |
| 0.3.4 | `0.3.4-appcenter-offline-package.sql` | PG `ry-cloud` | ⚠️ **已被提前打进 0.3.3 包**（包内文件名 `update_appcenter_offline_package_20260701.sql`）。0.3.4 定版时注意已升级主机勿重复投放（脚本本身幂等，可重跑） |
| 0.3.4 | `0.3.4-portal-merge.sql` | PG `ry-cloud` | 待投放。服务合并配套：防御性清理三张幻影表 |
| 0.3.4 | `0.3.4-portal-module.sql` | PG `ry-cloud` | 待投放。门户模块注册表建表+5 模块种子（与 postgres_portal_kernel.sql 同步维护） |
| 0.3.4 | `0.3.4-portal-user-module-preference.sql` | PG `ry-cloud` | 待投放。门户首页模块按用户保存显示顺序，前 6 项进入首屏（与 postgres_portal_kernel.sql 同步维护） |
| 0.3.5 | `0.3.5-portal-dataease-integration.sql` | PG `ry-cloud` | 待投放。DataEase 应用态势入口 + `portal_analytics_*` 态势分析表骨架 |

### 0.3.4 更新包非 SQL 操作清单（服务合并 + ruoyi-file 改名）

1. **Nacos 配置必须走 OpenAPI/控制台发布，禁止 SQL 直写**（Nacos 对不存在的 data_id 有负缓存，SQL 直插后服务取不到；本地验证已踩坑；可用 `deploy/scripts/` 参考会话产出的 nacos-publish 模式）。需发布：新增 `ruoyi-portal.yml`、`ruoyi-file.yml`；更新 `ruoyi-gateway.yml`（/appcenter、/infoservice → lb://ruoyi-portal；/resource → /file）、`application-common.yml`（租户忽略表新增 portal_module；移除 seata 块）、`datasource.yml`（移除 seata 代理行）、`ruoyi-monitor.yml`（移除 seata 忽略项）。可选清理旧行：`ruoyi-appcenter.yml`、`ruoyi-infoservice.yml`、`ruoyi-resource.yml`。
2. **镜像**：`PORTAL_IMAGE`（portal.Dockerfile 含 LibreOffice）替代 appcenter+infoservice 两镜像；`FILE_IMAGE` 替代 `RESOURCE_IMAGE`；**system 与 auth 也必须随包重建**（Dubbo 接口包名 org.dromara.resource.api → org.dromara.file.api，新旧镜像互不兼容）。
3. **启动顺序**：更新配置 → 换四个业务镜像（file/system/auth/portal）→ 重启 gateway。
4. 前端静态包必须同版更新（API 路径 /resource/** → /file/**）；nginx.conf 同包更新（新增 index.html no-cache，需重建 nginx-web 容器——文件级挂载绑 inode，仅 reload 不生效）。

## 历史备注

- 源码树中的日期命名副本 `update_appcenter_offline_package_20260701.sql` 与 0.3.4 文件同体，2026-07-03 删除，仅保留版本命名。
- `0.3.5-resource-sharing-fixes.sql` 仅存在于 Mac 旧快照（非仓库文件），是 0.3.3 定稿前的中间草稿（无 tenant_id 版本），从未投放，已作废。
- `source/script/sql/update/` 下保留的 `update_2.x-*.sql` 为 RuoYi 上游框架升级脚本，不属本清单管理范围。
- 装包规则（批次 4 装配脚本实现前人工遵守）：某版本的更新包应恰好包含本清单中该版本全部"待投放"文件，按文件名排序执行；投放后更新本清单状态列。

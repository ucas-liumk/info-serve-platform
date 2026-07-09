# info-serve 开发规约

适用对象：在本仓库工作的**所有人类开发者与 AI 代理**（Claude / Codex / Cursor / 任何模型）。本文件是强制规约，与默认习惯冲突时以本文为准。违反红线的改动不予合并。

配套文档：架构法源 [docs/architecture/bounded-contexts.md](docs/architecture/bounded-contexts.md) · 运行手册 [RUNBOOK.md](RUNBOOK.md) · 端口 [PORTS.md](PORTS.md) · 迁移清单 [deploy/updates/MANIFEST.md](deploy/updates/MANIFEST.md)

## 1. 仓库拓扑与分支纪律（红线）

- 唯一真相源：GitHub 仓库 `origin`（当前为 `https://github.com/ucas-liumk/info-serve-platform.git`），`origin/main` 是稳定主线。Mac `/Users/macmini/windows-info-serve` 与 Windows `E:\gallant-dev\active\info-serve` 都必须作为 GitHub 的普通工作副本使用，禁止把本机目录、Windows 裸仓库或其他镜像当作更高优先级真相源。
- **`main` 只进合并，禁止直接提交，禁止在 main 工作树上直接开发**（2026-07-04 曾因此需要 WIP 归位）。所有开发从最新 `origin/main` 拉任务分支。
- 开工第一步：`git fetch origin && git switch main && git pull --ff-only origin main && git switch -c <type>/<bc>-<主题>`；如有 GitHub Issue，分支名优先带 issue 号，例如 `fix/issue-12-appcenter-download`。
- 收工最后一步：验证通过后提交并 `git push -u origin <branch>`；需要合入主线时创建或交付 GitHub PR。**未 push 的工作视为不存在。**
- GitHub Issue 是任务单；对应 PR 描述应写 `Closes #<编号>` 或 `Refs #<编号>`。无 Issue 时，分支名、提交信息、PR 描述必须能说明任务范围。
- 提交信息：conventional 格式 `<type>: <中文描述>`（feat/fix/refactor/docs/chore/perf/test）。**禁止任何署名尾注**（Co-Authored-By 等）。
- 禁止：直接 push 到 `origin/main`、force-push 共享分支、`git clean -fd`、`git checkout -- .`、改写他人已推送的历史。
- 合并前自查 `git log origin/main..HEAD` 与 `git diff origin/main...HEAD`，附验证输出，交协调者/用户 review 后合入。

## 2. 架构边界（构建期强制）

- BC 地图法源不变：`docs/architecture/bounded-contexts.md`，变更架构必须先改该文档；部署形态修订见 `docs/superpowers/specs/2026-07-07-service-split-cloud-native-design.md`。
- 门户五 BC 已是独立服务（portal-kernel/appcenter/forum/requiredknowledge/resources，Maven 模块物理隔离，跨界 import 直接编译不过；原门户单体的包级 ArchUnit 门禁随之退役）。
- 通信红线：业务服务之间禁止直接同步互调；同步只允许「→ 通用服务」方向（唯一例外 kernel→BC 统计，带超时降级）；跨 BC 协作一律走 RabbitMQ `portal.topic`（事件常量见 ruoyi-api-portal-kernel）。
- 表前缀归属不变：`app_/info_/res_/forum_/portal_/rk_`，跨域走接口或事件，禁跨界 SQL。`app_message` 表归 portal-kernel 专属（历史前缀错配，禁止其他服务直连）。
- 门户公共能力（消息通知、收藏、统计聚合、模块注册表）只存在于 kernel，**禁止在 BC 内复制一套**。
- 新增门户模块必须：注册 `portal_module` + 提供 BC 契约（元数据 / 入口路由 / 权限码 / `my-*` 查询 / 统计接口 / 消息 scene）。

## 3. 编码红线

- **单文件 ≤ 800 行硬限**（.vue / .java 一视同仁），超过 400 行即应考虑拆分（拆法参考 `views/portal/resources/components/`）。
- 前端子组件显式 import（自动注册不含 views 子目录）；门户 API 按 `api/portal/<bc>.ts` 归位，禁止散放。
- 系统边界必须校验输入；错误不得静默吞掉；禁止硬编码密钥/密码；优先不可变数据风格。

## 4. 数据库与配置的双轨纪律

- **schema 变更三件套缺一不可**：改种子（`source/script/sql/postgres/*`，管新装机）+ 写增量（`deploy/updates/<版本>-<主题>.sql`，管存量升级）+ 登记 `MANIFEST.md`。
- 新表默认会被租户拦截器追加 `tenant_id` 条件：要么建 `tenant_id` 列，要么把表加入 `application-common.yml` 的租户忽略清单（并发布配置）。
- Nacos 配置正本在 `source/script/config/nacos/*`；改动必须**经 OpenAPI/控制台发布**到运行环境——**禁止 SQL 直写 config_info**（Nacos 负缓存会吞掉新 data_id）。新增服务专属 yml 时同步 `generate-initdb.py` 的 `new_configs`。

## 5. 验证门禁（未过验证 = 未完成）

| 改动 | 必过验证 |
|---|---|
| 任何后端 | Docker Maven 编译：`mvn -o -ntp -Pdev -DskipTests -pl <模块> -am compile`（Mac 内存紧，`-Xmx768m~1024m`） |
| 涉及 portal-* 任一模块 | `mvn -ntp -pl ruoyi-modules/<模块> -am -DskipTests=false test`（单测随模块走；根 pom 默认 `skipTests=true`，必须显式关闭） |
| 改动 Dubbo 契约（ruoyi-api-*） | 提供方+全部消费方模块同批编译与镜像重建（§7 红线） |
| 改动 MQ 事件契约（PortalEventConstants/事件体） | 发布方与消费方服务同批重建 |
| 任何前端 | `cd plus-ui && npm run build:prod` 必绿 |
| 前端样式/视觉 | `npm run design:audit` 必绿（设计令牌棘轮门禁）；新颜色/字号/圆角只允许用 `tokens.scss` 令牌，正本规范 `docs/design/design-system.md`；重大视觉改动前后跑 `deploy/scripts/ui-capture.py` 截图对比 |
| deploy 文件 | `docker compose --env-file .env config --quiet`；shell 脚本 `bash -n` |
| 行为面改动 | 本地栈冒烟：`APP_CENTER_BASE_URL=http://127.0.0.1:7010/prod-api node deploy/scripts/appcenter-v1-e2e.mjs` + 相关端点 curl |

声称"完成/修复"前必须给出验证输出；失败就如实报告失败。

## 6. 多会话 / 多代理并行

- 按 BC 认领工作，文件级不重叠；跨 BC 改动独占分支。
- **发现并发写冲突（文件被他人修改/写入被拒）：立即停手，向协调者确认分工，禁止强行覆盖。**
- 一个任务 = 一个分支 = 一个执行者；协调者负责显式分工与裁决。

## 7. 发布规约

- 版本号唯一真相：根目录 `/VERSION`。定版流程：bump VERSION → 提交 → `git tag v<版本>` → push 分支与 tag 到 GitHub → 按 `MANIFEST.md` 从 tag 装更新包 → 部署验证。
- `releases/` 产物永不入库。
- **Dubbo 接口（`org.dromara.*.api`）任何变更 ⇒ 提供方与全部消费方镜像必须同批重建**（新旧混跑接口不兼容）。
- 门户五服务镜像见 `deploy/.env` 的 `PORTAL_*_IMAGE`（kernel/appcenter/forum/requiredknowledge/resources）；`ruoyi-api-portal-kernel` 变更时五服务同批重建。
- nginx.conf / 前端 dist 更新后必须**重建** `nginx-web` 容器——文件级 bind mount 绑定 inode，替换文件后仅 reload 读不到新内容。

## 8. 环境坑速查

- Windows SSH 默认 shell 是 cmd.exe（GBK）：远程命令前加 `chcp 65001 >nul &`。Windows 开发也必须直接连接 GitHub `origin`；历史 Windows hub / 裸仓库只可作为旧资料参考，禁止作为开发与合并真相源。
- Maven 离线模式 `-o` 必须搭配 `-am`（内部构件不在本地仓库）。
- 历史债：`AppApplicationServiceImplTest`、`AppCategoryServiceImplTest` 已 `@Tag("exclude")`（用例腐烂待修），不要参考其旧断言。
- 首页统计口径由各 BC 服务接口提供，禁止在内核新增裸 SQL 统计。

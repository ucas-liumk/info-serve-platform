# info-serve 开发规约

适用对象：在本仓库工作的**所有人类开发者与 AI 代理**（Claude / Codex / Cursor / 任何模型）。本文件是强制规约，与默认习惯冲突时以本文为准。违反红线的改动不予合并。

配套文档：架构法源 [docs/architecture/bounded-contexts.md](docs/architecture/bounded-contexts.md) · 运行手册 [RUNBOOK.md](RUNBOOK.md) · 端口 [PORTS.md](PORTS.md) · 迁移清单 [deploy/updates/MANIFEST.md](deploy/updates/MANIFEST.md)

## 1. 仓库拓扑与分支纪律（红线）

- 唯一真相源：Windows 裸仓库 `E:/git/info-serve.git`（hub）。工作副本：Windows `E:\gallant-dev\active\info-serve`、Mac `/Users/macmini/info-serve`。GitHub 仅为定版后推送的备份镜像。
- **`main` 只进合并，禁止直接提交，禁止在 main 工作树上直接开发**（2026-07-04 曾因此需要 WIP 归位）。
- 开工第一步：`git fetch origin && git switch -c <type>/<bc>-<主题>`；收工最后一步：`push`。**未 push 的工作视为不存在。**
- 提交信息：conventional 格式 `<type>: <中文描述>`（feat/fix/refactor/docs/chore/perf/test）。**禁止任何署名尾注**（Co-Authored-By 等）。
- 禁止：force-push 共享分支、`git clean -fd`、`git checkout -- .`、改写他人已推送的历史。
- 合并前自查 `git log origin/main..` 与 diff，交协调者/用户 review 后合入。

## 2. 架构边界（构建期强制）

- 限界上下文地图与服务拆分判据以 `docs/architecture/bounded-contexts.md` 为唯一法源；变更架构必须先改该文档。
- `ruoyi-portal` 内按 BC 分包：`kernel` / `appcenter` / `resources` / `forum`。
  - 内容 BC 之间**禁止相互依赖**；
  - 内核对内容 BC **只可依赖其 `service` 接口**（domain/mapper/impl 全禁）；
  - 内容 BC 不得依赖内核实现类。
  - 以上由 ArchUnit（`ruoyi-portal/src/test/.../BcBoundaryTest`）强制，测试不过=不许提交。
- 表前缀归属：`app_`（工具即用）、`info_/res_`（资料）、`forum_`（论坛）、`portal_`（内核）。**跨域取数走对方 service 接口，禁止跨界 SQL。**
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
| 涉及 ruoyi-portal | ArchUnit：`mvn -pl ruoyi-modules/ruoyi-portal -am -DskipTests=false test`（根 pom 默认 `skipTests=true`，必须显式关闭） |
| 任何前端 | `cd plus-ui && npm run build:prod` 必绿 |
| deploy 文件 | `docker compose --env-file .env config --quiet`；shell 脚本 `bash -n` |
| 行为面改动 | 本地栈冒烟：`APP_CENTER_BASE_URL=http://127.0.0.1:7010/prod-api node deploy/scripts/appcenter-v1-e2e.mjs` + 相关端点 curl |

声称"完成/修复"前必须给出验证输出；失败就如实报告失败。

## 6. 多会话 / 多代理并行

- 按 BC 认领工作，文件级不重叠；跨 BC 改动独占分支。
- **发现并发写冲突（文件被他人修改/写入被拒）：立即停手，向协调者确认分工，禁止强行覆盖。**
- 一个任务 = 一个分支 = 一个执行者；协调者负责显式分工与裁决。

## 7. 发布规约

- 版本号唯一真相：根目录 `/VERSION`。定版流程：bump VERSION → 提交 → `git tag v<版本>` → 按 `MANIFEST.md` 从 tag 装更新包 → 部署验证 → 从 Mac 推 GitHub 镜像。
- `releases/` 产物永不入库。
- **Dubbo 接口（`org.dromara.*.api`）任何变更 ⇒ 提供方与全部消费方镜像必须同批重建**（新旧混跑接口不兼容）。
- nginx.conf / 前端 dist 更新后必须**重建** `nginx-web` 容器——文件级 bind mount 绑定 inode，替换文件后仅 reload 读不到新内容。

## 8. 环境坑速查

- Windows SSH 默认 shell 是 cmd.exe（GBK）：远程命令前加 `chcp 65001 >nul &`；对 Windows hub 的 git 访问需 PowerShell 包装（`remote.origin.uploadpack/receivepack = "powershell git-upload-pack/git-receive-pack"`，新 clone 记得配）。
- Maven 离线模式 `-o` 必须搭配 `-am`（内部构件不在本地仓库）。
- 历史债：`AppApplicationServiceImplTest`、`AppCategoryServiceImplTest` 已 `@Tag("exclude")`（用例腐烂待修），不要参考其旧断言。
- 首页统计口径由各 BC 服务接口提供，禁止在内核新增裸 SQL 统计。

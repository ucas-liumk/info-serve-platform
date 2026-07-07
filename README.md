# info-serve-platform

信息中心数智服务平台是一套面向单位内部信息服务场景的门户系统。它以 RuoYi-Cloud-Plus 为底座，围绕“服务门户 + 管理后台 + 可部署应用入口 + 资料共享 + 服务论坛”组织业务能力，目标是把常用信息化服务、资料沉淀、应用入口和运维说明放到一个统一入口中。

当前定版版本号以 [VERSION](./VERSION) 为唯一来源：`0.3.3`。本文档同时承担两类用途：

- GitHub 仓库介绍页：说明项目定位、架构、目录、开发规约和部署方式。
- 系统使用手册：首页右上角“使用手册”入口会读取前端静态文件 `readme.md` 并弹窗展示，内容由本 README 同步生成。

## 1. 用户入口

| 入口 | 默认地址 | 用途 |
| --- | --- | --- |
| 服务门户 | `http://<局域网IP>:7010/portal` | 面向普通用户的首页、资料共享、应用中心、服务论坛、通知与使用手册 |
| 管理后台 | `http://<局域网IP>:7010/admin` | 用户、角色、菜单、门户模块、应用中心、资料与论坛等后台管理 |
| Gateway | `http://<局域网IP>:8180` | 后端统一 API 网关 |
| Nacos | `http://127.0.0.1:8148/nacos` | 配置中心与服务注册中心 |
| MinIO 控制台 | `http://127.0.0.1:8161` | 文件对象存储管理 |
| RabbitMQ 控制台 | `http://127.0.0.1:8173` | 消息队列管理 |

初始管理员账号见 [RUNBOOK.md](./RUNBOOK.md)。首次部署后必须修改默认密码，并按实际内网环境调整访问地址、防火墙和反向代理策略。

## 2. 门户功能

### 2.1 首页

门户首页提供统一品牌头部、日期时间、通知、当前用户菜单、系统使用手册入口、模块卡片和全局统计。

- 首页模块来自 `portal_module` 注册表，支持后台配置启停、排序、入口路由和权限。
- 模块卡片当前包括资料共享、应用中心、服务论坛，以及智能问答、时事热点等占位模块。
- 右上角“使用手册”按钮会弹出本文档内容，适合给管理员和普通用户查看系统说明、部署路径和协作规约。

### 2.2 资料共享

资料共享用于沉淀内部文档、制度资料、学习材料和服务材料，支持上传、分类、审核、预览、下载与统计。文件存储走文件服务和对象存储，业务数据归属门户服务中的 Resources 限界上下文。

### 2.3 应用中心

应用中心是已部署应用和离线包的统一入口，不承担自动安装器职责。每个应用配置真实访问地址，前端打开时会将 `127.0.0.1` / `localhost` 按当前访问主机改写，便于局域网部署后从用户机器直接访问。

### 2.4 服务论坛

服务论坛用于围绕信息化服务进行主题讨论、问题反馈和回复通知。论坛属于 Portal 服务内的 Forum 限界上下文，消息通知统一写入门户内核。

### 2.5 通知与使用手册

门户通知由 `portal-kernel` 统一管理，避免各业务域复制通知表和读取逻辑。系统使用手册不走数据库，直接读取前端静态目录中的 `readme.md`：

- 开发和构建时：`npm run sync:manual` 会把根目录 `README.md` 复制到 `plus-ui/public/readme.md`。
- 生产构建后：Vite 会把它打入 `plus-ui/dist/readme.md`。
- Docker Compose 部署时：`plus-ui/dist` 挂载到 Nginx 的 `/usr/share/nginx/html`。
- 上线后如需修改手册，管理员可直接编辑宿主机 `plus-ui/dist/readme.md`；若只是原地编辑该文件，不需要新增后台界面或改数据库。

如果整体替换 `dist` 目录或修改 `nginx.conf`，应按 [AGENTS.md](./AGENTS.md) 的发布规约重建 `nginx-web` 容器。

## 3. 技术架构

### 3.1 前端

- 目录：`plus-ui/`
- 技术栈：Vue 3、Vite 7、TypeScript、Element Plus、Pinia、Vue Router、UnoCSS、Sass。
- 门户页面：`plus-ui/src/views/portal/`
- 门户壳与通知：`plus-ui/src/layout/portal/`
- 管理后台：`plus-ui/src/views/admin/`
- 设计令牌：`plus-ui/src/assets/styles/tokens.scss`
- 设计规范：`docs/design/design-system.md`

前端生产构建命令：

```bash
cd plus-ui
npm install
npm run build:prod
```

`build:prod` 会先同步系统手册，再执行 Vite 构建。

### 3.2 后端

- 目录：`source/`
- Java：17
- Spring Boot：3.5.15
- Spring Cloud：2025.0.3
- Spring Cloud Alibaba：2025.0.0.0
- Nacos Client：2.5.1
- Dubbo：3.3.6
- Sa-Token：1.45.0
- MyBatis-Plus：3.5.16
- 数据库：PostgreSQL 承载业务库，MySQL 仅承载 Nacos 配置库

当前保留的主要服务：

| 服务 | 端口 | 职责 |
| --- | ---: | --- |
| `ruoyi-gateway` | 8180 | API 网关、统一转发 |
| `ruoyi-auth` | 8110 | 登录认证、令牌签发 |
| `ruoyi-system` | 8101 | 用户、角色、部门、字典、参数等系统管理 |
| `ruoyi-portal-kernel` | 8107 | 门户内核：身份、消息通知、收藏、统计聚合、模块注册表 |
| `ruoyi-portal-appcenter` | 8106 | 应用中心：应用目录、离线包、需求收集 |
| `ruoyi-portal-forum` | 8108 | 服务论坛：版块、主题、回复 |
| `ruoyi-portal-requiredknowledge` | 8109 | 应知应会：学习栏目、题库、考试配置 |
| `ruoyi-portal-resources` | 8111 | 资料共享：上传、分类、审核、预览、下载 |
| `ruoyi-file` | 8114 | 文件服务，由上游 `ruoyi-resource` 改名 |
| `ruoyi-monitor` | 8190 | Spring Boot Admin 监控 |
| `ruoyi-nacos` | 8148 | 配置中心与注册中心 |

后端模块说明见 [source/README.md](./source/README.md)。

### 3.3 数据与中间件

| 组件 | 用途 | 端口 |
| --- | --- | ---: |
| PostgreSQL | 业务数据，默认库 `ry-cloud` | 8132 |
| MySQL | Nacos 配置库，默认库 `ry-config` | 8136 |
| Redis | 缓存、令牌、分布式能力 | 8179 |
| RabbitMQ | 门户通知等异步消息 | 8172 / 8173 |
| MinIO | 文件对象存储 | 8160 / 8161 |
| Nginx | 前端静态资源与 `/prod-api/` 反代 | 7010 |

端口以 [PORTS.md](./PORTS.md) 为唯一清单。

## 4. 限界上下文

架构法源是 [docs/architecture/bounded-contexts.md](./docs/architecture/bounded-contexts.md)。批次 A 已把门户五个限界上下文从原 `ruoyi-portal` 单体拆为独立服务（部署形态修订见 [service-split spec](./docs/superpowers/specs/2026-07-07-service-split-cloud-native-design.md)），对外 API 路径保持兼容。

| 限界上下文 | 独立服务 | 表前缀 | 职责 |
| --- | --- | --- | --- |
| Portal Kernel | `ruoyi-portal-kernel` | `portal_` | 门户身份、消息通知、收藏、统计聚合、模块注册表 |
| AppCenter | `ruoyi-portal-appcenter` | `app_` | 应用目录、离线包、需求收集 |
| Resources | `ruoyi-portal-resources` | `info_` / `res_` | 资料上传、分类、审核、预览、下载 |
| Forum | `ruoyi-portal-forum` | `forum_` | 版块、主题、回复、论坛通知 |
| RequiredKnowledge | `ruoyi-portal-requiredknowledge` | `rk_` | 应知应会：学习栏目、科目、题库、考试配置 |

边界纪律：

- 内容 BC 之间禁止相互依赖。
- Portal Kernel 对内容 BC 只能依赖其 `service` 接口，禁止依赖 domain、mapper、impl。
- 内容 BC 不得依赖 Portal Kernel 的实现类。
- 跨域取数走对方 service 接口，禁止跨界 SQL。
- 门户公共能力只存在于 Kernel，禁止在 BC 内复制一套。
- 新增门户模块必须注册 `portal_module` 并提供模块契约。

这些规则现由 Maven 模块物理隔离强制：五个限界上下文各自是独立 Maven 模块与独立服务，跨界 `import` 直接编译不过；原 `ruoyi-portal` 单体的包级 ArchUnit（`BcBoundaryTest`）随单体退役。跨 BC 协作走 RabbitMQ `portal.topic` 事件，同步只允许调用通用服务。

## 5. 仓库目录

```text
.
├── AGENTS.md                         # 强制开发规约
├── VERSION                           # 版本号唯一来源
├── README.md                         # GitHub 介绍页 + 系统使用手册源文档
├── RUNBOOK.md                        # 部署运行手册
├── PORTS.md                          # 端口清单
├── docs/
│   ├── architecture/bounded-contexts.md
│   └── design/design-system.md
├── plus-ui/                          # Vue 前端
├── source/                           # Java 后端
├── deploy/                           # Compose、Nginx、镜像构建与升级 SQL
├── data/                             # 本地运行数据，不入库
└── logs/                             # 本地运行日志，不入库
```

## 6. 本地开发

### 6.1 分支

唯一真相源是 GitHub `origin`，稳定主线是 `origin/main`。禁止在 `main` 工作树上直接开发。

推荐开工流程：

```bash
git fetch origin
git switch main
git pull --ff-only origin main
git switch -c feat/<bc>-<topic>
```

如果当前工作树已有他人未提交改动，禁止覆盖；应先协调，或使用独立 `git worktree` 从 `origin/main` 拉任务分支。

### 6.2 前端本地运行

```bash
cd plus-ui
npm install
npm run dev
```

开发服务默认端口为 `7018`，接口代理到本地 Gateway `8180`。

### 6.3 后端编译

后端使用 Docker Maven / JDK 17。Mac 内存紧时优先使用离线模式和较小堆内存：

```bash
cd source
docker run --rm \
  -v "$PWD":/workspace \
  -v "$HOME/.m2":/root/.m2 \
  -w /workspace \
  -e MAVEN_OPTS="-Xmx768m -XX:MaxMetaspaceSize=256m" \
  maven:3.9-eclipse-temurin-17 \
  mvn -o -ntp -Pdev -DskipTests -pl ruoyi-modules/ruoyi-portal-kernel -am compile
```

`<模块>` 替换为实际改动模块（门户五服务 `ruoyi-portal-kernel`/`-appcenter`/`-forum`/`-requiredknowledge`/`-resources`，或 `ruoyi-system`/`ruoyi-file` 等）。改动某个门户模块时，还必须显式关闭跳过测试跑该模块单测：

```bash
cd source
mvn -pl ruoyi-modules/ruoyi-portal-kernel -am -DskipTests=false test
```

## 7. 部署

初始化 SQL 由脚本从源码与 Nacos 配置生成：

```bash
cd deploy
python3 scripts/generate-initdb.py
```

启动：

```bash
cd deploy
docker compose --env-file .env up -d
```

停止：

```bash
cd deploy
docker compose --env-file .env stop
```

销毁容器但保留数据卷目录：

```bash
cd deploy
docker compose --env-file .env down
```

前端静态文件更新后，需要重新构建前端并按发布规约处理 `nginx-web` 容器。更多部署细节见 [RUNBOOK.md](./RUNBOOK.md)。

## 8. 数据库与配置变更规约

Schema 变更必须同时完成三件事：

1. 修改种子 SQL：`source/script/sql/postgres/*`，用于新装机。
2. 编写增量 SQL：`deploy/updates/<版本>-<主题>.sql`，用于存量升级。
3. 登记迁移清单：[deploy/updates/MANIFEST.md](./deploy/updates/MANIFEST.md)。

新表默认会被租户拦截器追加 `tenant_id` 条件：要么建 `tenant_id` 列，要么加入 `application-common.yml` 的租户忽略清单并发布配置。

Nacos 配置正本位于 `source/script/config/nacos/*`。运行环境必须通过 OpenAPI 或控制台发布配置，禁止 SQL 直写 `config_info`。

## 9. 验证门禁

提交前按改动类型执行验证：

| 改动 | 必过验证 |
| --- | --- |
| 后端代码 | Docker Maven 编译：`mvn -o -ntp -Pdev -DskipTests -pl <模块> -am compile` |
| 门户任一模块 | 单测随模块走：`mvn -ntp -pl ruoyi-modules/<模块> -am -DskipTests=false test` |
| 前端代码 | `cd plus-ui && npm run build:prod` |
| 前端样式/视觉 | `cd plus-ui && npm run design:audit` |
| deploy 文件 | `cd deploy && docker compose --env-file .env config --quiet`；shell 脚本执行 `bash -n` |
| 行为面改动 | 运行相关接口 curl 或 `APP_CENTER_BASE_URL=http://127.0.0.1:7010/prod-api node deploy/scripts/appcenter-v1-e2e.mjs` |

声称完成前必须给出验证输出；失败要如实报告。

## 10. 提交与发布纪律

- 提交信息使用 conventional 格式：`<type>: <中文描述>`。
- 禁止任何署名尾注，例如 `Co-Authored-By`。
- 禁止直接 push 到 `origin/main`。
- 禁止 force-push 共享分支。
- 禁止 `git clean -fd`、`git checkout -- .`、`git reset --hard` 覆盖他人工作。
- 收工最后一步是 push 当前任务分支；未 push 的工作视为不存在。
- 合并前自查 `git log origin/main..HEAD` 与 `git diff origin/main...HEAD`。

定版流程：

```text
bump VERSION -> 提交 -> git tag v<版本> -> push 分支和 tag -> 按 MANIFEST.md 从 tag 制作更新包 -> 部署验证
```

## 11. 运维提示

- 前端本地开发入口：`http://127.0.0.1:7018`
- Docker/Nginx 入口：`http://127.0.0.1:7010`
- 后端 Gateway：`http://127.0.0.1:8180`
- 业务库 PostgreSQL：`127.0.0.1:8132`
- Nacos 配置库 MySQL：`127.0.0.1:8136`
- 日志目录：`logs/`
- 运行数据目录：`data/`

手册内容维护：

```bash
# 开发源文档
README.md

# 构建前自动生成
plus-ui/public/readme.md

# 生产构建产物，部署后可由管理员直接编辑
plus-ui/dist/readme.md
```

编辑源文档后重新构建前端；编辑部署产物后重新打开首页手册弹窗或点击“重新加载”。

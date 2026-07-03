# RuoYi-Cloud-Plus 标准部署运行手册

## 版本与目录

- 产品定版：见根目录 [`VERSION`](./VERSION)（版本号唯一来源）
- 源码版本：`v2.6.2`
- 前端版本：`plus-ui v5.6.2-v2.6.2`
- 源码目录：`source/`
- 前端目录：`plus-ui/`
- 部署目录：`deploy/`
- 数据目录：`data/`
- 日志目录：`logs/`
- Compose project：`infosys-ruoyi-cloud-plus`

## 协作与分支纪律（2026-07-03 起）

- 唯一真相源：Windows 裸仓库 `E:/git/info-serve.git`；工作副本为 Windows `E:\gallant-dev\active\info-serve` 与 Mac `/Users/macmini/info-serve`；GitHub 为备份镜像（定版后从 Mac 推送）。
- `main` 只进合并，禁止直接提交；每个工作线程一个分支 `feat/<bc>-<topic>`，按 `docs/architecture/bounded-contexts.md` 的限界上下文认领。
- 开工第一步 `git fetch` 并新开分支；收工最后一步 push——**未 push 的工作视为不存在**。
- 定版流程：更新 `VERSION` → 提交 → 打 `git tag v<版本>` → 从 tag 构建更新包（迁移 SQL 见 `deploy/updates/MANIFEST.md`）→ 推送 GitHub 镜像。

## 数据库架构（PostgreSQL 迁移）

- 业务数据全部运行在 **PostgreSQL**：`ry-cloud`（系统/用户/角色/菜单/应用中心/信息服务）。
- **MySQL 仅作为 Nacos 配置库**（`ry-config`）。Nacos 官方不支持 PostgreSQL，故保留一个轻量 MySQL 容器专供其配置存储。
- 迁移涉及改动：`ruoyi-common-mybatis` 与 `ruoyi-snailjob-server` 启用 `org.postgresql` 驱动；`script/config/nacos/datasource.yml` 与 6 个模块 yml 切 PG 驱动/URL；`deploy/scripts/generate-initdb.py` 产出 `initdb-mysql/`（仅 Nacos）与 `initdb-postgres/`（业务库，含建库脚本 `00-init.sh` 与官方 `postgres_ry_*.sql`）。
- MyBatis-Plus 分页自动识别数据库类型，无需改方言。账号：PG `ruoyi/ruoyi123`，库见上。

## 端口

端口以 [PORTS.md](./PORTS.md) 为唯一清单（前端 70xx、后端与基础设施 81xx）。最常用：

| 服务 | 访问 |
| --- | --- |
| Web | `http://<局域网IP>:7010` |
| Gateway | `http://<局域网IP>:8180` |
| Nacos | `http://127.0.0.1:8148/nacos` |
| PostgreSQL（业务库） | `127.0.0.1:8132` |
| MySQL（仅 Nacos 配置库） | `127.0.0.1:8136` |
| Redis | `127.0.0.1:8179` |

## 构建

生成初始化脚本（产出 `deploy/initdb-mysql/` 与 `deploy/initdb-postgres/`；两目录为生成产物不入库，**新 clone 首次启动容器前必须先执行**）：

```bash
cd /Users/macmini/info-serve/deploy
python3 scripts/generate-initdb.py
```

> 只改业务代码时，按需重建对应服务（其余沿用旧镜像），例如四个业务模块：
> ```bash
> cd /Users/macmini/info-serve/source
> docker run --rm -v "$PWD":/workspace -v /Users/macmini/.m2:/root/.m2 -w /workspace \
>   -e MAVEN_OPTS="-Xmx1280m -XX:MaxMetaspaceSize=384m" \
>   maven:3.9-eclipse-temurin-17 mvn -ntp -Pprod -DskipTests \
>   -pl ruoyi-modules/ruoyi-system,ruoyi-modules/ruoyi-resource,ruoyi-modules/ruoyi-appcenter,ruoyi-modules/ruoyi-infoservice \
>   -am package
> ```
> 然后用 `deploy/build-images.sh` 中对应条目重建这些镜像。

后端首次完整构建（含全部服务）使用 Docker Maven/JDK 17：

```bash
cd /Users/macmini/info-serve/source
docker run --rm \
  -v "$PWD":/workspace \
  -v /Users/macmini/.m2:/root/.m2 \
  -w /workspace \
  maven:3.9-eclipse-temurin-17 \
  mvn -ntp -Pprod -DskipTests \
    -pl ruoyi-visual/ruoyi-nacos,ruoyi-gateway,ruoyi-auth,ruoyi-modules/ruoyi-system,ruoyi-modules/ruoyi-resource,ruoyi-modules/ruoyi-appcenter,ruoyi-modules/ruoyi-infoservice,ruoyi-visual/ruoyi-monitor \
    -am package
```

本机内存压力较大时，完整 Maven 构建可能被系统结束。可在依赖已缓存后，按模块离线补构建：

```bash
docker run --rm \
  -v /Users/macmini/info-serve/source:/workspace \
  -v /Users/macmini/.m2:/root/.m2 \
  -w /workspace \
  -e MAVEN_OPTS="-Xmx768m -XX:MaxMetaspaceSize=256m" \
  maven:3.9-eclipse-temurin-17 \
  mvn -o -ntp -Pprod -DskipTests -pl ruoyi-modules/ruoyi-infoservice -am package

docker run --rm \
  -v /Users/macmini/info-serve/source:/workspace \
  -v /Users/macmini/.m2:/root/.m2 \
  -w /workspace \
  -e MAVEN_OPTS="-Xmx768m -XX:MaxMetaspaceSize=256m" \
  maven:3.9-eclipse-temurin-17 \
  mvn -o -ntp -Pprod -DskipTests -pl ruoyi-modules/ruoyi-resource -am package
```

前端：

```bash
cd /Users/macmini/info-serve/plus-ui
npm install
npm run build:prod
```

构建本地镜像：

```bash
cd /Users/macmini/info-serve/deploy
./build-images.sh
```

## 启停

```bash
cd /Users/macmini/info-serve/deploy
docker compose --env-file .env up -d
docker compose --env-file .env stop
docker compose --env-file .env down
```

## 初始账号

- 管理员：`admin / admin123`
- 首次进入后必须修改默认密码。

## 验证记录（0.3.0 首轮验证的历史存档；当时端口方案为 19xxx，现行端口见 PORTS.md）

- `docker compose config`：已通过，输出见 `logs/compose-config.txt`
- 前端构建：已通过，输出见 `logs/npm-build-prod.log`
- 后端构建：已通过，主构建输出见 `logs/maven-targeted-package.log`，补构建输出见 `logs/maven-workflow-package-offline.log`、`logs/maven-resource-package-offline.log`
- 本地镜像：已生成 `infosys/ruoyi-cloud-plus-* :2.6.2`
- 容器状态：核心容器已启动验证，MySQL、Redis、Nacos 为 healthy
- Web 访问：`http://127.0.0.1:19100` 与 `http://192.168.8.4:19100` 返回 200
- Gateway 访问：`http://127.0.0.1:19100/prod-api/auth/code` 返回 200，验证码已临时关闭用于本地验证
- 登录验证：`admin / admin123` 已登录成功
- 核心功能页截图：
  - `screenshots/login-fixed.png`
  - `screenshots/home.png`
  - `screenshots/system-user.png`
  - `screenshots/system-role.png`
  - `screenshots/system-menu.png`
  - `screenshots/system-dept.png`
  - `screenshots/system-dict.png`
  - `screenshots/system-config.png`
  - `screenshots/tool-gen.png`
  - `screenshots/workflow-process-definition.png`
  - `screenshots/workflow-category.png`
  - `screenshots/workflow-all-task-waiting.png`

## 说明

- Nacos 配置由 `deploy/scripts/generate-initdb.py` 从 `source/script/config/nacos/` 生成到 `deploy/initdb-mysql/90-nacos-config-content.sql`，会同时更新 `dev` 与 `prod` 命名空间内容；业务库初始化生成到 `deploy/initdb-postgres/`。
- 服务内的 Nacos 地址通过环境变量和 JVM 参数覆盖为 `nacos:8848`，不使用官方 host network。
- RabbitMQ 与 MinIO 使用独立容器，不复用本机其他项目。
- 初始化 SQL 已统一添加 `SET NAMES utf8mb4`，避免中文配置和租户名称导入后乱码。
- `sys_user.nick_name` 与 `sys_social.nick_name` 初始化时扩展为 `varchar(64)`，避免演示数据导入失败。
- `ruoyi-auth.yml` 中 `security.captcha.enabled` 已临时设为 `false` 便于局域网首轮验证；正式使用前建议恢复验证码并修改默认密码。
- gen / job / workflow / snailjob 服务及其配置、路由、数据源已彻底移除；新装机不再创建 `ry-job`、`ry-workflow` 库（存量环境中的旧库不受影响，可自行归档删除）。

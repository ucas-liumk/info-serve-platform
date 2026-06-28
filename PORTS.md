# 项目端口清单

更新时间：2026-06-28

当前项目采用统一号段：

- **前端使用 70xx**
- **后端与基础设施对外端口使用 81xx**

补充：这台 Windows 当前系统保留了 `7034-7633` 端口段，所以 `71xx` 无法作为本机监听端口使用；前端改用同样清晰的 `70xx`。

说明：Docker 容器内部仍会保留部分官方默认端口，例如 PostgreSQL 容器内部仍是 `5432`，Redis 容器内部仍是 `6379`。我们统一的是 **Windows 宿主机访问端口** 和 **本地 Java 服务监听端口**。

当前以 `deploy/docker-compose.yml` 作为本项目主启动路径；`source/script/docker/*` 属于上游示例/历史脚本，仍可能保留默认端口写法，暂不作为当前开发环境标准。

## 最常用入口

| 用途 | 地址 | 配置位置 | 说明 |
| --- | --- | --- | --- |
| 前端开发服务 | `http://127.0.0.1:7018` | `plus-ui/.env.development` 的 `VITE_APP_PORT` | 本地前端开发入口 |
| 前端开发接口代理 | `http://localhost:8180` | `plus-ui/vite.config.ts` | 前端 `/dev-api` 转发到本地网关 |
| Docker/Nginx 前端入口 | `http://127.0.0.1:7010` | `deploy/docker-compose.yml` 的 `nginx-web` | 生产构建后的前端入口 |
| Gateway 网关 | `http://127.0.0.1:8180` | `source/ruoyi-gateway` / `deploy/docker-compose.yml` | 后端统一接口入口 |
| Nacos 控制台 | `http://127.0.0.1:8148/nacos` | `source/pom.xml` / `deploy/docker-compose.yml` | 配置中心/注册中心 |
| RabbitMQ 控制台 | `http://127.0.0.1:8173` | `deploy/docker-compose.yml` | 消息队列管理页 |
| MinIO 控制台 | `http://127.0.0.1:8161` | `deploy/docker-compose.yml` | 对象存储管理页 |

## 前端端口

| 服务 | 端口 | 配置位置 | 说明 |
| --- | ---: | --- | --- |
| Vite 开发服务 | `7018` | `plus-ui/.env.development` | `npm run dev` 使用 |
| Docker/Nginx Web | `7010` | `deploy/docker-compose.yml` | 宿主机访问 Nginx 容器 |
| 生产构建预览端口 | `7010` | `plus-ui/.env.production` | Vite production mode 端口记录 |

## 后端服务端口

这些是 Java 服务自己的监听端口。本地编译调试时，IDEA/命令行启动的就是这些端口。

| 服务 | 端口 | 配置位置 | 说明 |
| --- | ---: | --- | --- |
| Gateway | `8180` | `source/ruoyi-gateway/src/main/resources/application.yml` | 后端统一入口 |
| Gateway MVC | `8181` | `source/ruoyi-gateway-mvc/src/main/resources/application.yml` | MVC 网关备用模块 |
| Auth | `8110` | `source/ruoyi-auth/src/main/resources/application.yml` | 认证服务 |
| System | `8101` | `source/ruoyi-modules/ruoyi-system/src/main/resources/application.yml` | 系统管理 |
| Gen | `8102` | `source/ruoyi-modules/ruoyi-gen/src/main/resources/application.yml` | 代码生成 |
| Workflow | `8105` | `source/ruoyi-modules/ruoyi-workflow/src/main/resources/application.yml` | 工作流 |
| AppCenter | `8106` | `source/ruoyi-modules/ruoyi-appcenter/src/main/resources/application.yml` | 工具即用/应用中心 |
| InfoService | `8107` | `source/ruoyi-modules/ruoyi-infoservice/src/main/resources/application.yml` | 信息服务 |
| Job | `8113` | `source/ruoyi-modules/ruoyi-job/src/main/resources/application.yml` | 任务服务；避开本机占用的 `8103` |
| Resource | `8114` | `source/ruoyi-modules/ruoyi-resource/src/main/resources/application.yml` | 文件/资源服务；避开本机占用的 `8104` |
| Demo | `8121` | `source/ruoyi-example/ruoyi-demo/src/main/resources/application.yml` | 示例服务 |
| Test MQ | `8122` | `source/ruoyi-example/ruoyi-test-mq/src/main/resources/application.yml` | MQ 示例服务 |
| Monitor | `8190` | `source/ruoyi-visual/ruoyi-monitor/src/main/resources/application.yml` | Spring Boot Admin |
| SnailJob 控制台 | `8191` | `source/ruoyi-visual/ruoyi-snailjob-server/src/main/resources/application.yml` | 任务调度控制台 |
| SnailJob 服务端通信 | `8192` | `source/script/config/nacos/ruoyi-snailjob-server.yml` | SnailJob Netty 通信端口 |
| Job Snail 客户端 | `8193` | `source/script/config/nacos/ruoyi-job.yml` / `deploy/docker-compose.yml` | Job 服务接入 SnailJob |
| Seata Server | `8194` | `source/ruoyi-visual/ruoyi-seata-server/src/main/resources/application.yml` | 当前默认未启用 |

## 基础设施端口

这些端口由 Docker 暴露到 Windows 宿主机，供本地 Java 服务连接。

| 服务 | 宿主机端口 | 容器内部端口 | 配置位置 | 说明 |
| --- | ---: | ---: | --- | --- |
| Nacos | `8148` | `8848` | `deploy/docker-compose.yml` | 源码 Maven 默认 `nacos.server=127.0.0.1:8148` |
| Nacos gRPC 客户端端口 | `9148` | `9848` | `deploy/docker-compose.yml` | Nacos 2.x 客户端会访问主端口 + 1000 |
| Nacos gRPC 服务端端口 | `9149` | `9849` | `deploy/docker-compose.yml` | Nacos 2.x 内部通信端口，配套暴露 |
| PostgreSQL | `8132` | `5432` | `deploy/docker-compose.yml` / `source/script/config/nacos/datasource.yml` | 业务数据库 |
| MySQL | `8136` | `3306` | `deploy/docker-compose.yml` | 仅作为 Nacos 配置库 |
| Redis | `8179` | `6379` | `deploy/docker-compose.yml` / `source/script/config/nacos/application-common.yml` | 缓存 |
| RabbitMQ | `8172` | `5672` | `deploy/docker-compose.yml` / `source/script/config/nacos/application-common.yml` | 消息队列 |
| RabbitMQ 管理页 | `8173` | `15672` | `deploy/docker-compose.yml` | 浏览器管理入口 |
| MinIO API | `8160` | `9000` | `deploy/docker-compose.yml` | 文件服务 API |
| MinIO 控制台 | `8161` | `9001` | `deploy/docker-compose.yml` | 浏览器管理入口 |

## Nacos 配置规则

`deploy/scripts/generate-initdb.py` 现在按环境生成不同 Nacos 配置：

| Nacos 命名空间 | 用途 | 连接方式 |
| --- | --- | --- |
| `dev` | 本地 Java 调试 | 连接 Windows 宿主机 81xx 端口，例如 `localhost:8132`、`localhost:8179` |
| `prod` | Docker Compose 部署 | 连接 Docker 服务名和容器内部端口，例如 `postgres:5432`、`redis:6379` |

也就是说：本地调试用 `dev`，Docker 部署用 `prod`。

## Nginx 代理关系

Docker/Nginx 前端入口监听宿主机 `7010`，容器内部仍监听 `80`。

| 浏览器路径 | Nginx 转发到 |
| --- | --- |
| `/` | 前端静态文件 |
| `/prod-api/` | `ruoyi-gateway:8180` |
| `/admin/` | `ruoyi-monitor:8190/admin/` |
| `/snail-job/` | `ruoyi-snailjob-server:8191/snail-job/` |

## 查端口占用

PowerShell 推荐命令：

```powershell
Get-NetTCPConnection -LocalPort 7018
```

根据进程 ID 看是谁占用了端口：

```powershell
Get-Process -Id 进程ID
```

一次检查本项目主要端口：

```powershell
Get-NetTCPConnection -LocalPort 7010,7018,8180,8148,8132,8136,8179,8172,8173,8160,8161
```

## 改端口时改哪里

| 想改什么 | 修改位置 |
| --- | --- |
| 前端开发端口 | `plus-ui/.env.development` 的 `VITE_APP_PORT` |
| 前端代理的后端网关 | `plus-ui/vite.config.ts` 的 `server.proxy.target` |
| Docker/Nginx 前端端口 | `deploy/docker-compose.yml` 的 `nginx-web` |
| Docker Gateway 端口 | `deploy/docker-compose.yml` 的 `ruoyi-gateway` |
| 基础设施宿主机端口 | `deploy/docker-compose.yml` 对应服务的 `ports` |
| Java 服务监听端口 | 对应模块的 `src/main/resources/application.yml` |
| Nacos 默认地址 | `source/pom.xml` 的 `nacos.server` |
| Nacos dev/prod 初始化配置 | `source/script/config/nacos/*` 与 `deploy/scripts/generate-initdb.py` |

改完 Docker 端口后，需要重启对应容器：

```powershell
cd E:\gallant-dev\active\info-serve\deploy
docker compose --env-file .env up -d
```

改完前端 `VITE_APP_PORT` 后，需要重启前端开发服务：

```powershell
cd E:\gallant-dev\active\info-serve\plus-ui
npm run dev
```

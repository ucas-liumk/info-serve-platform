# 项目端口清单

更新时间：2026-07-06

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
| DataEase 态势后台 | `http://127.0.0.1:8100` | DataEase 官方安装配置 | 外部大屏引擎，应用中心管理员入口 |
| Apache Hop Web | `http://127.0.0.1:18091` | Hop Web 部署配置 | 外部数据治理/ETL，应用中心管理员入口 |
| Budibase 低代码工厂 | `http://127.0.0.1:18100` | `deploy/situation/budibase/docker-compose.yml` | 外部低代码应用工厂，应用中心管理员入口 |

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
| Auth | `8110` | `source/ruoyi-auth/src/main/resources/application.yml` | 认证服务 |
| System | `8101` | `source/ruoyi-modules/ruoyi-system/src/main/resources/application.yml` | 系统管理 |
| Portal | `8107` | `source/ruoyi-modules/ruoyi-portal/src/main/resources/application.yml` | 门户业务服务（内核/应用中心/资料共享/论坛），由 appcenter(8106)+infoservice(8107) 合并 |
| File | `8114` | `source/ruoyi-modules/ruoyi-file/src/main/resources/application.yml` | 文件服务（由 ruoyi-resource 改名）；避开本机占用的 `8104` |
| Portal-RequiredKnowledge | `8109` | `source/ruoyi-modules/ruoyi-portal-requiredknowledge/src/main/resources/application.yml` | 应知应会服务（批次 A 自 portal 拆出） |
| Monitor | `8190` | `source/ruoyi-visual/ruoyi-monitor/src/main/resources/application.yml` | Spring Boot Admin |

已移除的服务（gen `8102`、workflow `8105`、job `8113`、gateway-mvc `8181`、demo `8121`、test-mq `8122`、snailjob `8191-8193`、seata `8194`）不再占用端口，compose 中对应条目已删除。

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
| DataEase Web | `8100` | DataEase 官方安装配置 | 态势大屏制作后台，独立部署 |
| Apache Hop Web | `18091` | Hop Web 部署配置 | 态势数据治理入口，本地验证将容器 `8080` 映射到宿主机 `18091` |
| Budibase Web | `18100` | Budibase proxy `10000` | 低代码应用工厂入口，独立部署 |

## Nacos 配置规则

`deploy/scripts/generate-initdb.py` 现在按环境生成不同 Nacos 配置：

| Nacos 命名空间 | 用途 | 连接方式 |
| --- | --- | --- |
| `dev` | 本地 Java 调试 | 连接 Windows 宿主机 81xx 端口，例如 `localhost:8132`、`localhost:8179` |
| `prod` | Docker Compose 部署 | 连接 Docker 服务名和容器内部端口，例如 `postgres:5432`、`redis:6379` |

也就是说：本地调试用 `dev`，Docker 部署用 `prod`。

## Nginx 代理关系

Docker/Nginx 前端入口监听宿主机 `7010`，容器内部仍监听 `80`。

| 浏览器路径 | Nginx 行为 |
| --- | --- |
| `/` | 前端静态文件（SPA fallback 到 `index.html`） |
| `/prod-api/` | 反代 `ruoyi-gateway:8180` |
| `/admin`、`/admin/**` | SPA fallback 到 `index.html`（管理后台前端路由，非 monitor 代理） |
| `*.mjs` | 修正 MIME 为 `application/javascript`（PDF worker 需要） |

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
Get-NetTCPConnection -LocalPort 7010,7018,8180,8148,8132,8136,8179,8172,8173,8160,8161,8100,18091,18100
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

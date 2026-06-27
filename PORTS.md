# 项目端口清单

更新时间：2026-06-27

这个文件记录本项目当前使用到的端口。先记住一句话：**浏览器访问看宿主机端口，服务之间通信看容器/服务内部端口**。

## 最常用入口

| 用途 | 地址 | 配置位置 | 说明 |
| --- | --- | --- | --- |
| 前端开发服务 | `http://127.0.0.1:18880` | `plus-ui/.env.development` 的 `VITE_APP_PORT` | 本地开发时打开这个 |
| 前端开发接口代理 | `http://localhost:19080` | `plus-ui/vite.config.ts` 的 `server.proxy.target` | 前端的 `/dev-api` 会转发到这里 |
| Docker 生产 Web | `http://127.0.0.1:19100` | `deploy/docker-compose.yml` 的 `nginx-web` | 打包后由 Nginx 提供页面 |
| Docker 网关 | `http://127.0.0.1:19080` | `deploy/docker-compose.yml` 的 `ruoyi-gateway` | 后端统一入口 |
| Nacos 控制台 | `http://127.0.0.1:19848/nacos` | `deploy/docker-compose.yml` 的 `nacos` | 配置中心/注册中心 |
| MinIO 文件服务 | `http://127.0.0.1:19000` | `deploy/docker-compose.yml` 的 `minio` | 对象存储 API；当前只暴露了 API 端口 |

## Docker 暴露端口

这些端口是 Docker 映射到 Windows 宿主机上的端口，也就是你可以从浏览器、数据库工具、接口工具访问的端口。

| 服务 | 宿主机地址 | 容器内部端口 | 说明 |
| --- | --- | --- | --- |
| MySQL | `127.0.0.1:19306` | `3306` | 仅作为 Nacos 配置库 |
| PostgreSQL | `127.0.0.1:19432` | `5432` | 业务数据库 |
| Redis | `127.0.0.1:19379` | `6379` | 缓存 |
| MinIO | `0.0.0.0:19000` | `9000` | 文件服务 API |
| Nacos | `127.0.0.1:19848` | `8848` | 配置中心/注册中心 |
| Gateway | `0.0.0.0:19080` | `8080` | 后端统一网关 |
| Nginx Web | `0.0.0.0:19100` | `80` | 生产模式前端入口 |

说明：

- `127.0.0.1` 只允许本机访问。
- `0.0.0.0` 允许本机和局域网其他设备访问，比如 Mac mini 访问 Windows 的局域网 IP。
- 如果 Mac mini 要访问 Windows 上的前端开发服务，用 `http://Windows局域网IP:18880`。

## 前端相关端口

| 项目 | 当前值 | 配置位置 | 用途 |
| --- | --- | --- | --- |
| `VITE_APP_PORT` | `18880` | `plus-ui/.env.development` | Vite 开发服务器端口 |
| `VITE_APP_BASE_API` | `/dev-api` | `plus-ui/.env.development` | 前端开发环境接口前缀 |
| 代理目标 | `http://localhost:19080` | `plus-ui/vite.config.ts` | `/dev-api` 实际转发到后端网关 |
| 监控地址 | `http://localhost:9090/admin/applications` | `plus-ui/.env.development` | 前端配置项，开发环境可能需要按实际部署调整 |
| SnailJob 地址 | `http://localhost:8800/snail-job` | `plus-ui/.env.development` | 前端配置项，开发环境可能需要按实际部署调整 |

## 后端服务内部端口

这些端口主要是后端服务自己监听的端口。在 Docker Compose 中，很多服务没有直接暴露到 Windows，而是通过 Gateway 或 Nginx 访问。

| 服务 | 内部端口 | 配置位置 | 说明 |
| --- | --- | --- | --- |
| Gateway | `8080` | `source/ruoyi-gateway/src/main/resources/application.yml` / `deploy/docker-compose.yml` | Docker 暴露为 `19080` |
| Auth | `9210` | `source/ruoyi-auth/src/main/resources/application.yml` | 认证服务 |
| System | `9201` | `source/ruoyi-modules/ruoyi-system/src/main/resources/application.yml` | 系统管理 |
| Gen | `9202` | `source/ruoyi-modules/ruoyi-gen/src/main/resources/application.yml` | 代码生成 |
| Job | `9203` | `source/ruoyi-modules/ruoyi-job/src/main/resources/application.yml` | 任务服务 |
| Resource | `9204` | `source/ruoyi-modules/ruoyi-resource/src/main/resources/application.yml` | 文件/资源服务 |
| Workflow | `9205` | `source/ruoyi-modules/ruoyi-workflow/src/main/resources/application.yml` | 工作流服务 |
| AppCenter | `9206` | `source/ruoyi-modules/ruoyi-appcenter/src/main/resources/application.yml` | 工具即用/应用中心 |
| InfoService | `9207` | `source/ruoyi-modules/ruoyi-infoservice/src/main/resources/application.yml` | 信息服务 |
| Monitor | `9100` | `source/ruoyi-visual/ruoyi-monitor/src/main/resources/application.yml` | 通过 Nginx `/admin/` 代理访问 |
| SnailJob Server | `8800` | `source/ruoyi-visual/ruoyi-snailjob-server/src/main/resources/application.yml` | 通过 Nginx `/snail-job/` 代理访问 |
| Job Snail 客户端 | `19203` | `deploy/docker-compose.yml` 的 `SNAIL_PORT` | Job 服务接入 SnailJob 用 |

## Nginx 代理关系

生产模式下，`nginx-web` 容器内部监听 `80`，Windows 访问端口是 `19100`。

| 浏览器路径 | Nginx 转发到 |
| --- | --- |
| `/` | 前端静态文件 |
| `/prod-api/` | `ruoyi-gateway:8080` |
| `/admin/` | `ruoyi-monitor:9100/admin/` |
| `/snail-job/` | `ruoyi-snailjob-server:8800/snail-job/` |

## 查端口占用

PowerShell 推荐命令：

```powershell
Get-NetTCPConnection -LocalPort 18880
```

根据进程 ID 看是谁占用了端口：

```powershell
Get-Process -Id 进程ID
```

老式命令也可以：

```powershell
netstat -ano | findstr :18880
```

## 改端口时改哪里

| 想改什么 | 修改位置 |
| --- | --- |
| 前端开发端口 | `plus-ui/.env.development` 的 `VITE_APP_PORT` |
| 前端代理的后端网关 | `plus-ui/vite.config.ts` 的 `target` |
| Docker 网关宿主机端口 | `deploy/docker-compose.yml` 里的 `19080:8080` |
| Docker Web 宿主机端口 | `deploy/docker-compose.yml` 里的 `19100:80` |
| 数据库/Redis/Nacos/MinIO 暴露端口 | `deploy/docker-compose.yml` 对应服务的 `ports` |
| 后端微服务自身端口 | 对应模块的 `application.yml`，以及 `deploy/docker-compose.yml` 里的 `SERVER_PORT` |

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

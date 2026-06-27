# 项目启动操作手册

更新时间：2026-06-27

## 1. 当前项目路径

```text
项目根目录：E:\gallant-dev\active\info-serve
前端目录：E:\gallant-dev\active\info-serve\plus-ui
后端目录：E:\gallant-dev\active\info-serve\source
部署目录：E:\gallant-dev\active\info-serve\deploy
```

## 2. 推荐开发模式

建议采用前后端分离开发。IDE 工具可以有两种选择：

### 方案 A：VS Code 全栈开发，推荐先试

```text
VS Code
- 打开 plus-ui
- 负责 Vue3 / Vite 前端开发
- 也可以打开 source
- 负责 Java / Maven / Spring Boot 后端开发

Docker Desktop
- 负责 MySQL / PostgreSQL / Redis / Nacos / RabbitMQ / MinIO 等基础服务
```

适合情况：

- 不想购买 IDEA Ultimate。
- 希望前端、后端、AI 工具都集中在 VS Code / Cursor / Codex 这一类编辑器里。
- 可以接受 Java 项目的智能提示、Spring Bean 跳转、配置识别等能力弱于 IDEA Ultimate。

VS Code 后端开发建议安装扩展：

```text
Extension Pack for Java
Spring Boot Extension Pack
Maven for Java
Debugger for Java
Test Runner for Java
```

### 方案 B：VS Code + IDEA

```text
VS Code
- 打开 plus-ui
- 负责 Vue3 / Vite 前端开发

IntelliJ IDEA
- 打开 source
- 负责 Java 后端开发

Docker Desktop
- 负责基础服务
```

原因：

- 前端和后端职责清楚，工具各用擅长的。
- 后端是多模块微服务项目，用 IDEA 管理 Maven、启动类、断点调试更舒服。
- 数据库、缓存、注册中心等底座服务交给 Docker，避免手动安装一堆环境。
- Mac mini 后续通过浏览器访问 Windows 局域网 IP 测试即可。

当前建议：

```text
先用 VS Code 全栈开发。
如果后端开发中明显感觉多模块跳转、Spring 配置识别、Bean 注入分析、调试体验不够，再考虑 IDEA Ultimate。
```

## 3. 项目结构判断

当前项目是典型的前后端分离项目：

```text
plus-ui
- Vue3
- TypeScript
- Vite
- Element Plus

source
- Java 17
- Maven
- Spring Boot 3.5
- Spring Cloud
- RuoYi Cloud Plus 多模块微服务
```

前端 `vite.config.ts` 已配置：

```text
host: 0.0.0.0
port: 由 .env.development 的 VITE_APP_PORT 决定
接口代理目标：http://localhost:19080
```

当前 `.env.development` 中：

```text
VITE_APP_PORT = 80
VITE_APP_BASE_API = /dev-api
```

后端网关暴露端口：

```text
Gateway: http://localhost:19080
```

## 4. 已确认的本机环境

Windows 本机已确认：

```text
Node.js：v22.12.0
npm：10.9.0
JDK：17.0.11
Maven：3.9.8
Docker CLI：29.1.3
Docker Compose：v2.40.3-desktop.1
```

当前发现：

```text
Docker Desktop 后台暂未运行。
```

如果要启动后端依赖容器，需要先手动打开 Docker Desktop，等待左下角或顶部状态显示 Docker Engine 已运行。

## 5. 启动顺序总览

推荐顺序：

```text
1. 启动 Docker Desktop
2. 启动后端基础设施 / 容器服务
3. IDEA 打开后端 source
4. VS Code 打开前端 plus-ui
5. 启动前端开发服务器
6. Mac mini 访问 Windows 局域网 IP 测试
```

## 6. 第一步：启动 Docker Desktop

在 Windows 上手动打开 Docker Desktop。

确认 Docker 可用：

```powershell
docker ps
```

如果不报错，说明 Docker Engine 已经启动。

如果出现类似：

```text
open //./pipe/dockerDesktopLinuxEngine: The system cannot find the file specified
```

说明 Docker Desktop 后台还没有起来，需要先打开 Docker Desktop。

## 7. 第二步：启动 Docker 容器

进入部署目录：

```powershell
cd E:\gallant-dev\active\info-serve\deploy
```

检查 Compose 配置：

```powershell
docker compose --env-file .env config --quiet
```

如果没有输出，表示配置语法通过。

启动完整环境：

```powershell
docker compose --env-file .env up -d
```

查看容器状态：

```powershell
docker compose --env-file .env ps
```

停止容器：

```powershell
docker compose --env-file .env stop
```

停止并删除容器网络：

```powershell
docker compose --env-file .env down
```

注意：`down` 不会自动删除 `data` 目录里的数据库数据，但会删除容器和网络。

## 8. Docker 暴露端口

项目自带 `RUNBOOK.md` 中记录的端口：

| 服务 | 地址 |
|---|---|
| Web | `http://localhost:19100` |
| Gateway | `http://localhost:19080` |
| Nacos | `http://127.0.0.1:19848/nacos` |
| PostgreSQL | `127.0.0.1:19432` |
| MySQL | `127.0.0.1:19306` |
| Redis | `127.0.0.1:19379` |
| MinIO | `http://localhost:19000` |

Mac mini 访问 Windows 上的服务时，用 Windows 局域网 IP：

```text
Windows IP：192.168.8.10
Web：http://192.168.8.10:19100
Gateway：http://192.168.8.10:19080
前端开发服务：http://192.168.8.10:80
```

## 9. 第三步：后端用 IDEA 打开

IDEA 打开目录：

```text
E:\gallant-dev\active\info-serve\source
```

导入方式：

```text
Open Project -> 选择 source 目录 -> 识别 Maven 项目
```

推荐 JDK：

```text
JDK 17
```

主要启动类：

| 服务 | 启动类 | 默认端口 |
|---|---|---|
| 网关 | `org.dromara.gateway.RuoYiGatewayApplication` | `8080`，Docker 暴露为 `19080` |
| 认证 | `org.dromara.auth.RuoYiAuthApplication` | `9210` |
| 系统 | `org.dromara.system.RuoYiSystemApplication` | `9201` |
| 资源 | `org.dromara.resource.RuoYiResourceApplication` | `9204` |
| 应用中心 | `org.dromara.appcenter.RuoYiAppCenterApplication` | `9206` |
| 信息服务 | `org.dromara.infoservice.RuoYiInfoServiceApplication` | `9207` |
| 代码生成 | `org.dromara.gen.RuoYiGenApplication` | `9202` |
| 工作流 | `org.dromara.workflow.RuoYiWorkflowApplication` | `9205` |
| 定时任务 | `org.dromara.job.RuoYiJobApplication` | `9203` |

日常开发不一定全部启动。建议优先启动：

```text
ruoyi-gateway
ruoyi-auth
ruoyi-system
ruoyi-resource
ruoyi-appcenter
ruoyi-infoservice
```

如果 Docker Compose 已经把这些服务作为容器启动了，IDEA 中只启动你正在修改的那个服务即可。否则会端口冲突。

## 10. Maven 注意事项

本项目根 `pom.xml` 会引用本项目内部 BOM：

```text
org.dromara:ruoyi-common-alibaba-bom:2.6.2
org.dromara:ruoyi-common-bom:2.6.2
org.dromara:ruoyi-api-bom:2.6.2
```

从 Mac mini 复制到 Windows 后，Windows 本机 Maven 仓库可能没有这些内部 BOM，会报：

```text
Non-resolvable import POM
Could not find artifact org.dromara:ruoyi-common-bom:pom:2.6.2
```

已在 Windows 上执行过一次本地安装：

```powershell
cd E:\gallant-dev\active\info-serve\source
mvn -q -f ruoyi-common\ruoyi-common-alibaba-bom\pom.xml install
mvn -q -f ruoyi-common\ruoyi-common-bom\pom.xml install
mvn -q -f ruoyi-api\ruoyi-api-bom\pom.xml install
```

如果后续换机器或清理 Maven 仓库，再执行一次即可。

## 11. 第四步：前端用 VS Code 打开

VS Code 打开目录：

```text
E:\gallant-dev\active\info-serve\plus-ui
```

安装依赖：

```powershell
cd E:\gallant-dev\active\info-serve\plus-ui
npm install
```

启动开发服务器：

```powershell
npm run dev
```

当前 `vite.config.ts` 已经设置：

```text
host: 0.0.0.0
```

所以 Mac mini 可以通过 Windows 局域网 IP 访问。

如果端口 `80` 被占用，可以临时修改：

```text
plus-ui\.env.development
VITE_APP_PORT = 5173
```

然后重新执行：

```powershell
npm run dev
```

Mac mini 访问：

```text
http://192.168.8.10:5173
```

## 12. Git 注意事项

当前项目从 Mac mini 复制到 Windows 后，Git 检查出现过：

```text
fatal: detected dubious ownership in repository
```

原因是目录 owner 是 `BUILTIN/Administrators`，当前用户是 `DESKTOP-H3KBS56\liumk`。

可执行：

```powershell
git config --global --add safe.directory E:/gallant-dev/active/info-serve
```

然后再检查：

```powershell
cd E:\gallant-dev\active\info-serve
git status
```

当前工作区有大量未提交改动和新增文件，启动前建议先确认不要覆盖这些内容。

## 13. 当前检查结果

已完成：

- [x] 确认项目是前后端分离结构。
- [x] 确认前端目录：`plus-ui`。
- [x] 确认后端目录：`source`。
- [x] 确认部署目录：`deploy`。
- [x] 确认 Windows 已安装 Node / npm / JDK / Maven / Docker CLI。
- [x] 确认 Docker Desktop 后台暂未运行。
- [x] 确认前端代理目标：`http://localhost:19080`。
- [x] 确认 Docker Web 端口：`19100`。
- [x] 确认 Docker Gateway 端口：`19080`。
- [x] 确认 Maven 内部 BOM 已手动安装到本机仓库。

待执行：

- [ ] 手动打开 Docker Desktop。
- [ ] 手动执行 `docker compose --env-file .env up -d`。
- [ ] IDEA 打开 `source` 并完成 Maven 导入。
- [ ] VS Code 打开 `plus-ui`。
- [ ] 手动执行 `npm run dev`。
- [ ] Mac mini 访问 Windows 前端开发地址。
- [ ] Mac mini 访问 Windows Docker Web 地址。

## 14. 推荐日常开发流程

每天开始：

```text
1. Windows 打开 Docker Desktop
2. deploy 目录执行 docker compose up -d
3. IDEA 打开 source
4. VS Code 打开 plus-ui
5. VS Code 执行 npm run dev
6. Mac mini 浏览器访问 Windows IP 测试
```

每天结束：

```text
1. git status
2. git diff
3. 提交或保存当前工作
4. deploy 目录执行 docker compose stop
```

## 15. 后续更新规则

之后每次启动、报错、修复，都补充到本文件：

- 实际执行命令
- 实际端口
- 实际报错
- 解决方法
- 最终验证结果

这份文件作为当前项目在 Windows 主机上的启动手册持续维护。

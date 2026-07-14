# 应用中心飞腾/银河麒麟 ARM64 应用目录设计

日期：2026-07-15  
状态：已获用户方向确认，待规格复核  
目标环境：飞腾 1500、银河麒麟 V4、Linux aarch64、局域网/离线部署

## 1. 目标与边界

本次为应用中心增加一批可在飞腾/银河麒麟 ARM64 环境使用的应用，分为两条交付轨道：

1. 开源 Web 应用：12 个，每个应用是独立 Docker Compose 项目，可单独启动、停止、查看状态与日志，默认不随主业务栈启动。
2. 离线桌面软件：10 个，在应用中心以离线包形式发布，由用户下载后按页面说明手工安装，不从浏览器执行 `sudo`。

本次不修改应用中心 BC 的服务边界，不让业务服务直接控制 Docker，也不在门户进程内执行系统命令。外部应用故障不得影响 info-serve 主业务栈。

WPS 主程序及商业组件只使用有权取得和内部分发的官方包；无法确认授权时保留目录条目但不上传二进制。WPS 加载项只使用可审计源码或自有加载项，并采用官方 `wpsjs publish` 发布方式。

## 2. 方案选择

### 方案 A：全部加入主 `docker-compose.yml`

优点是一次启动；缺点是资源、故障、升级和主栈强耦合，无法满足独立停启。否决。

### 方案 B：每应用独立 Compose 项目，应用中心只维护目录入口（采用）

每个应用放在 `deploy/openapps/<app-code>/`，拥有自己的 Compose 文件、数据目录约定和生命周期脚本。应用中心只负责展示与跳转，不获得宿主机控制权限。

优点：隔离清晰、按需启动、便于离线打包、单应用升级和回滚；代价是需要统一脚本与锁文件约束版本。

### 方案 C：门户直接提供一键安装/启停

需要引入特权代理、权限校验和审计链路，安全面明显扩大。本期否决，未来如有运维平台需求再单独立项。

## 3. 开源 Web 应用目录

| 序号 | app_code | 应用 | 端口 | 默认状态 | 中文要求 |
|---:|---|---|---:|---|---|
| 1 | `stirling-pdf` | Stirling PDF | 18080 | 停止 | 默认 `zh_CN`，含简体中文 OCR 数据 |
| 2 | `drawio` | draw.io | 18082 | 停止 | 默认简体中文、离线模式 |
| 3 | `excalidraw` | Excalidraw | 18090 | 停止 | 简体中文界面与 CJK 字体 |
| 4 | `memos` | Memos | 18110 | 停止 | 简体中文界面 |
| 5 | `filebrowser` | File Browser | 18111 | 停止 | 简体中文界面 |
| 6 | `pairdrop` | PairDrop | 18112 | 停止 | 简体中文界面，局域网传输 |
| 7 | `it-tools` | IT-Tools | 18113 | 停止 | 简体中文界面 |
| 8 | `uptime-kuma` | Uptime Kuma | 18114 | 停止 | 简体中文界面 |
| 9 | `freshrss` | FreshRSS | 18115 | 停止 | 简体中文界面 |
| 10 | `gitea` | Gitea | 18116 | 停止 | 简体中文界面；本期不额外暴露 SSH 端口 |
| 11 | `jellyfin` | Jellyfin | 18118 | 停止 | 简体中文界面；不承诺飞腾硬件转码 |
| 12 | `searxng` | SearXNG | 18119 | 停止 | 简体中文界面；断网时外部搜索源不可用 |

端口 `18120-18129` 留给后续 OCRmyPDF、Syncthing、TriliumNext、Vaultwarden、Calibre-Web 等第二批候选。

所有镜像必须同时满足：

- 官方或项目官方指定镜像；
- 容器清单含 `linux/arm64`；
- 使用确定版本与镜像 digest，禁止交付时依赖漂移的 `latest`；
- 镜像可导出为离线 OCI/Docker archive；
- 项目许可证、上游地址、版本、digest 和镜像大小写入锁文件。

## 4. 独立停启与目录结构

```text
deploy/openapps/
├── README.md
├── catalog.yaml
├── bin/
│   ├── appctl
│   └── verify-all.sh
├── stirling-pdf/
│   ├── compose.yml
│   ├── .env.example
│   ├── start.sh
│   ├── stop.sh
│   ├── status.sh
│   ├── logs.sh
│   └── README.md
└── ...
```

统一命令：

```bash
deploy/openapps/bin/appctl stirling-pdf start
deploy/openapps/bin/appctl stirling-pdf stop
deploy/openapps/bin/appctl stirling-pdf status
deploy/openapps/bin/appctl stirling-pdf logs
```

每个 Compose 使用独立 project name、独立持久化目录和独立网络。`stop` 只停止容器，不删除数据卷；删除数据必须使用单独的显式命令，本期不提供自动清库入口。

每个应用设置内存/CPU 上限、日志轮转、健康检查和 `restart: unless-stopped`。但应用第一次部署后保持停止，只有管理员明确启动时才运行。

## 5. 离线包目录

目标首批目录如下：

| 序号 | package_code | 软件 | 交付判断 |
|---:|---|---|---|
| 1 | `wps-office-arm64` | WPS 365 Linux ARM64 | 仅官方授权包；授权未确认则不上二进制 |
| 2 | `wps-addins` | WPS 加载项离线发布包 | 只收源码可审计/自有加载项，使用 `wpsjs publish` |
| 3 | `localsend-arm64` | LocalSend | 需麒麟 V4 实机验证 |
| 4 | `xournalpp-arm64` | Xournal++ | 需 GTK/字体依赖验证 |
| 5 | `peazip-arm64` | PeaZip | 上游标记 ARM64 为实验性，验证失败则不上线 |
| 6 | `ocrmypdf-zh-arm64` | OCRmyPDF + 简体中文 OCR | 以依赖完整的离线包组交付 |
| 7 | `vlc-arm64` | VLC | 从可信麒麟/兼容仓库取得依赖完整包组 |
| 8 | `gimp-arm64` | GIMP | 从可信麒麟/兼容仓库取得依赖完整包组 |
| 9 | `inkscape-arm64` | Inkscape | 从可信麒麟/兼容仓库取得依赖完整包组 |
| 10 | `keepassxc-arm64` | KeePassXC | 从可信麒麟/兼容仓库取得依赖完整包组 |

离线包工作区不把二进制提交 Git：

```text
releases/offline-apps/<package-code>/<version>/
├── package/*.deb
├── install.md
├── manifest.json
├── SHA256SUMS
└── LICENSES/
```

`releases/` 按仓库规约永不入库。仓库只保存生成清单、下载来源描述、验证脚本和应用中心种子数据。实际二进制上传到系统对象存储后，由 `package_oss_id`/`package_url` 提供下载。

上线条件：aarch64 架构正确、简体中文可用、麒麟 V4 能安装和启动、依赖完整、SHA256 一致、许可证允许当前分发方式、无已知高危问题。未通过的条目保持禁用，不伪装为可下载。

## 6. 应用中心数据

新增数据同时维护：

- 新装机种子：`source/script/sql/postgres/postgres_app_center.sql`；
- 存量升级：`deploy/updates/0.3.10-appcenter-arm64-catalog.sql`；
- 迁移登记：`deploy/updates/MANIFEST.md`。

在线应用使用 `category_id=2`、`app_type=online`。访问地址可保存为 `http://127.0.0.1:<port>`，门户现有 URL 归一化逻辑会把 localhost 替换为用户正在访问的服务器主机名。

离线应用使用 `category_id=3`、`app_type=offline`。只有实际包上传成功后才填 `package_oss_id`/`package_url` 并启用；仅有候选信息时不新增可见下载卡片，避免空链接。

种子脚本使用稳定 ID 和 `ON CONFLICT`/条件更新，必须可重复执行，不覆盖管理员后续修改的开放范围和使用统计。

## 7. 安全与资源策略

- 只有 AppCenter 页面跳转到外部应用，不给外部应用主数据库凭据。
- 外部应用默认只绑定宿主机端口，不加入 info-serve 内部 Docker 网络。
- 需要认证的应用在首次启动时由管理员完成初始化；默认密码不得写入仓库。
- File Browser 只挂载专用数据目录，禁止挂载 `/`、Docker socket 或 info-serve 数据目录。
- Uptime Kuma 不挂载 Docker socket。
- SearXNG、FreshRSS 等可能访问互联网的应用必须在说明中标出联网行为。
- Jellyfin 默认关闭硬件转码；确认飞腾/麒麟驱动后再启用。
- 所有容器启用日志轮转，单文件上限 10 MiB、最多 3 份。

## 8. 安装与验证流程

1. 在有网构建机解析并锁定官方 ARM64 镜像版本和 digest。
2. 导出镜像离线包并生成 SHA256。
3. 拷贝到 UTM 麒麟环境，逐个导入。
4. 对每个应用执行：启动、健康检查、简体中文页面检查、主要操作冒烟、停止、再次启动、数据持久化检查。
5. 同一时间只启动被验证应用及其必要依赖，避免测试互相干扰。
6. 离线桌面包逐个安装、启动、确认简体中文、卸载/重装；记录依赖与错误。
7. 只有验证通过的条目才写入可见应用目录或上传离线包。

仓库门禁：

- `docker compose --env-file .env config --quiet`（主部署文件）；
- 每个 `deploy/openapps/*/compose.yml` 执行 `docker compose config --quiet`；
- 所有 shell 脚本执行 `bash -n`；
- AppCenter Maven 编译和测试；
- 前端未改动则不触发前端构建；若为离线包说明增强前端，则执行 `npm run build:prod`；
- UTM 上运行 AppCenter E2E 与 12 个应用的生命周期冒烟。

## 9. 交付结果

- 12 个独立 Web 应用部署目录和统一控制命令；
- ARM64 镜像锁文件、离线镜像归档和校验文件；
- 通过验证的在线应用全部出现在应用中心；
- 通过验证且许可允许的离线安装包可从应用中心下载；
- WPS 授权包或配置缺失时，有明确待配置项，不使用非官方替代包；
- UTM 回归报告记录每个应用的启动、中文、核心功能、停止、重启及失败原因。

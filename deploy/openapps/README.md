# ARM64 开源应用

本目录提供飞腾/银河麒麟 ARM64 环境使用的独立开源应用。它们不属于 info-serve 主业务栈，不会随 `deploy/docker-compose.yml` 自动启动。

## 使用方法

```bash
cp deploy/openapps/stirling-pdf/.env.example deploy/openapps/stirling-pdf/.env
deploy/openapps/bin/appctl stirling-pdf start
deploy/openapps/bin/appctl stirling-pdf verify
deploy/openapps/bin/appctl stirling-pdf stop
```

列出应用：

```bash
deploy/openapps/bin/appctl list
```

正式离线环境必须先运行发布包中的 `load-images.sh`。Compose 设置了 `pull_policy: never`，镜像未导入时会直接失败，不会偷偷访问互联网。

默认绑定 `0.0.0.0` 以供局域网访问。部署方必须用主机防火墙限制批准网段；单机验证可在 `.env` 中设置 `OPENAPPS_BIND_ADDR=127.0.0.1`。

目标机只有约 10 GiB 内存，验收与日常使用均按需单独启停，不应一次启动全部 12 个应用。`appctl` 会在启动前检查可用内存和镜像架构，但不会替用户停止其他应用。

应用第一次启动后按各目录 README 完成管理员账号等初始化，再到 AppCenter 后台上架。未初始化或已停止的应用应保持下架。

## 首次配置清单

| 应用 | 首次启动需要配置 |
|---|---|
| Stirling PDF | 默认无登录；如进入多人环境，应在环境变量中启用认证 |
| draw.io | 无；应用中心入口已带离线与简体中文参数 |
| Excalidraw | 无；本期为单机白板，不启用协同服务 |
| Memos | 浏览器创建首个管理员账号并切换简体中文 |
| File Browser | 从首次启动日志取得临时管理员密码，登录后立即修改 |
| PairDrop | 无；跨网段使用时另行配置 TURN |
| IT-Tools | 无；在设置中选择简体中文 |
| Uptime Kuma | 浏览器创建管理员账号并切换简体中文 |
| FreshRSS | 浏览器安装向导中选择简体中文并创建管理员 |
| Gitea | 先把 `.env` 的 `PUBLIC_BASE_URL` 改成局域网访问地址，再在浏览器安装向导中创建管理员 |
| Jellyfin | 浏览器安装向导中选择简体中文、创建管理员和媒体库 |
| SearXNG | 启动前修改 `.env` 的 `PUBLIC_BASE_URL` 与 `SEARXNG_SECRET`；外部搜索源需要联网 |

## 数据与清理

- `stop` 只停止容器，保留数据。
- `remove` 删除容器和网络，仍保留命名卷。
- 本工具不提供自动删除卷命令，防止误删用户数据。
- 所有应用限制日志为单文件 10 MiB、最多 3 份。

## 交付锁

`catalog.lock.json` 同时记录上游镜像、多架构 index digest 与 ARM64 platform digest。更新镜像时必须重新执行架构检查、麒麟启动回归和中文界面检查。

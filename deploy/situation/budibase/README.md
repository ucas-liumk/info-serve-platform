# Budibase 低代码工厂接入说明

本目录保存 Budibase 低代码工厂的独立部署资产。Budibase 不纳入主业务 `deploy/docker-compose.yml`，避免低代码运行时、初始化账号、对象存储和 CouchDB 生命周期影响门户主链路。

## 组件定位

| 组件 | 定位 | 当前接入方式 |
|---|---|---|
| AppCenter | 统一入口 | 应用中心 `治理工具` 分类，管理员可见 |
| Budibase | 快速自研应用工厂 | 独立 Docker Compose 部署，默认端口 `18100` |
| Apache Hop | 数据治理 | 后续从 Budibase API/数据源抽取低代码应用数据 |
| DataEase | 领导态势大屏 | 只读 Hop 加工后的分析层数据 |

Budibase 第一阶段用于构建需求台账、任务推进、问题闭环、满意度调查等低代码应用。不要让 Budibase 直接建或修改 `app_`、`res_`、`forum_`、`portal_` 等门户核心业务表。

## 启动

首次启动会从 `.env.example` 生成本机 `.env`，并为敏感项生成随机值：

```bash
deploy/situation/budibase/start-budibase.sh
```

验证：

```bash
deploy/situation/budibase/verify-budibase.sh
```

停止：

```bash
deploy/situation/budibase/stop-budibase.sh
```

默认入口：

```text
http://127.0.0.1:18100/
```

默认只启动 Budibase 核心服务（apps / worker / proxy / CouchDB / MinIO / Redis），不启动 LiteLLM。LiteLLM 属于 Budibase AI 辅助能力，当前低代码应用工厂阶段不需要，默认关闭可以降低本机和离线服务器内存压力。

如后续确实要启用 Budibase AI 辅助能力，先确认 Docker 内存充足，再显式开启 profile：

```bash
cd deploy/situation/budibase
docker compose --env-file .env -f docker-compose.yml --profile ai up -d
```

门户不再提供 `/portal/lowcode` 顶层入口。Budibase 管理入口由 `0.3.5-portal-budibase-integration.sql` 写入应用中心，默认 `required_role_key=superadmin`，后续可改为数据治理工程师角色键。

## 离线部署

在线机器先拉镜像：

```bash
cd deploy/situation/budibase
docker compose --env-file .env -f docker-compose.yml pull
```

然后按 `.env.example` 中的镜像清单导出镜像包，在离线服务器 `docker load` 后启动。生产环境建议把 `.env` 中的 `*:latest` 镜像改为明确版本标签，保证可复现。

## 数据治理边界

第一阶段建议这样流转：

```text
Budibase 低代码应用数据 -> Hop 清洗加工 -> portal_analytics_* / 后续 analytics_* -> DataEase 大屏
```

Budibase 只负责低代码应用运行和数据采集。指标口径、跨系统聚合、脱敏、宽表和作业日志继续由 Hop 管理，DataEase 只读取分析层。

## 官方资料

- Budibase Docker Compose: https://github.com/Budibase/budibase/blob/master/hosting/docker-compose.yaml
- Budibase hosting env: https://github.com/Budibase/budibase/blob/master/hosting/.env
- Budibase license: https://github.com/Budibase/budibase

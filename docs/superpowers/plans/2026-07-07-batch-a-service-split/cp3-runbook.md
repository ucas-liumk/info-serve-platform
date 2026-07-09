# CP3 运行时验收手册（Windows 正本机 · 批次 A 收官）

状态：待执行。前置条件：**0.3.5 发布线已合入 main**（2026-07-07 预演确认与本分支零冲突）。
本手册整合 T2/T4/T7-T10 移交的全部运行时步骤、T12 验收矩阵与终审 4 条建议，按**近零停机切换**顺序编排。执行者：主会话（经 ssh info-serve-win），全程不碰用户 IDE 工作副本。

## 0. 合流（Mac 上执行）

```bash
git fetch origin
git merge-base --is-ancestor origin/codex/deploy-github-main-20260706 origin/main || echo "0.3.5 未合入，停止"
git switch feature/batch-a-service-split
git merge origin/main   # 预演零冲突；若意外冲突以 spec/00-plan 为裁决依据
# .env 合并后核对：INFO_SERVE_VERSION=0.3.5 与 5 个 PORTAL_*_IMAGE 并存
git push
```
（选 merge 而非 rebase：AGENTS 禁 force-push 共享分支。）

## 1. Windows 侧准备（只读确认 + 独立克隆）

- SSH 纪律：复用连接、命令用 `&` 串联、避免并行会话（lab-remote 通用原则）。
- 确认活栈健康：`docker ps` 中 infosys-* 容器均 Up；确认磁盘余量 ≥10GB。
- **独立克隆**（不碰 `E:\gallant-dev\active\info-serve` 用户工作区）：
  `git clone https://github.com/ucas-liumk/info-serve-platform.git E:\gallant-dev\active\info-serve-batch-a && cd 该目录 && git switch feature/batch-a-service-split`
- ⚠️ 克隆目录仅用于构建产物；**部署操作（compose/nacos）仍指向活栈所在 deploy 环境**——数据卷路径（logs/data/initdb）以活栈 deploy 目录为准，执行前逐项核对相对路径落点。

## 2. 构建（克隆目录内）

1. `python3 deploy/scripts/generate-initdb.py`（**必跑**：终审 T10 项，消除生成物中旧 `lb://ruoyi-portal`；产物含 6 个新 data-id 的 INSERT）
2. 全量打包（RUNBOOK 11 模块 -pl 列表，Windows 本机 mvn 或 Docker Maven）
3. `deploy/build-images.sh`（11 镜像；portal-resources 用 resources.Dockerfile，构建后 `docker run --rm infosys/ruoyi-cloud-plus-portal-resources:2.6.2 soffice --version` 验 LibreOffice）

## 3. 配置发布（先服务后网关，此刻不切流量）

```bash
for f in portal-kernel portal-appcenter portal-forum portal-requiredknowledge portal-resources; do
  bash deploy/scripts/nacos-publish.sh dev  $f.yml source/script/config/nacos/$f.yml
  bash deploy/scripts/nacos-publish.sh prod $f.yml source/script/config/nacos/$f.yml
done
# ⚠️ 网关配置此步不发！旧路由继续把流量给老 portal 容器。
```
另核对（终审建议②）：存量 prod namespace 的 `application-common.yml` 中 rabbitmq host 应为 `rabbitmq`（generate-initdb 既有机制产物，理应已正确；不对则用 nacos-publish 修正）。

## 4. 新服务上线（无流量状态起动，老 portal 仍在服务）

```bash
bash deploy/bin/svc.sh start portal-kernel        # 终审建议①：kernel 先起，声明队列/绑定
# 等 kernel 注册健康（Nacos 控制台或 svc.sh logs 见"门户内核服务启动成功"）
for s in portal-appcenter portal-forum portal-requiredknowledge portal-resources; do
  bash deploy/bin/svc.sh start $s
done
```
验证：Nacos 服务列表 5 个新服务全部 UP；RabbitMQ 管理页(8173)可见 `portal.topic`/`portal.dlx`/3 队列。
⚠️ 期间对主 compose 的任何 `up -d` **不得带 --remove-orphans**（会误删仍在服务的老 portal 容器）。

## 5. 流量切换（发布网关终态配置）

```bash
bash deploy/scripts/nacos-publish.sh dev  ruoyi-gateway.yml source/script/config/nacos/ruoyi-gateway.yml
bash deploy/scripts/nacos-publish.sh prod ruoyi-gateway.yml source/script/config/nacos/ruoyi-gateway.yml
```
路由动态刷新后立即冒烟（均应 401/200，禁 404/503）：
```
/prod-api/infoservice/portal/stats            → portal-kernel
/prod-api/appcenter/portal/messages           → portal-kernel
/prod-api/appcenter/portal/apps               → portal-appcenter
/prod-api/infoservice/portal/forum/boards     → portal-forum
/prod-api/infoservice/portal/required-knowledge/catalog → portal-requiredknowledge
/prod-api/infoservice/portal/resources        → portal-resources
```
通过后退役老容器：`docker rm -f infosys-ruoyi-cloud-plus-portal`（回滚预案：重发旧版 gateway 配置即可秒级切回，老镜像保留至验收完成）。

## 6. 全量验收（= T12 Step 4-5）

1. e2e：`APP_CENTER_BASE_URL=http://127.0.0.1:7010/prod-api node deploy/scripts/appcenter-v1-e2e.mjs`
2. MQ 人工链路：论坛发回复→消息铃（forum→kernel 跨进程）；上传 docx→convert 队列消费→预览秒开
3. **故障演练矩阵**（T12 表）：逐个 `svc.sh stop <svc>` → 验其余正常 + 首页对应卡片归 0 → `start` 恢复；kernel 单独记录实测影响面（供批次 C 探针参数）
4. 演练后重跑 e2e 确认无残伤

## 7. 收尾

- MANIFEST 0.3.6 条目版本核定（合流后实际定版）；`/VERSION` 是否 bump 到 0.3.6 与用户确认
- 建 PR（feature/batch-a-service-split → main），PR 描述附：终审报告要点、故障演练实测表、spec 两处修订说明
- 合并后：Windows 用户工作区与各克隆同步；批次 A 关闭，批次 B 立项前先对齐 Windows 上已有的 arm64 buildx 工作

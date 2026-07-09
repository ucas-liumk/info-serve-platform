# Task 12: 全链路验收 —— 全量构建 / e2e 冒烟 / 杀服务故障演练

批次 A 的验收门禁（spec §10）：**全量编译 + 冒烟通过；杀掉任一 BC 服务，其余功能正常。** 未过本任务 = 批次 A 未完成，不得声称完成。

**Files:** 无新文件（只执行与记录）。

**Interfaces:**
- Consumes: T1-T11 全部产出。
- Produces: 验收记录（贴在分支收尾提交信息或 PR 描述中）+ 已 push 的完整分支。

- [ ] **Step 1: 干净全量构建**

```bash
cd /Users/macmini/windows-info-serve
docker run --rm -v "$PWD/source":/workspace -v /Users/macmini/.m2:/root/.m2 -w /workspace \
  maven:3.9-eclipse-temurin-17 mvn -ntp -Pprod -DskipTests \
  -pl ruoyi-visual/ruoyi-nacos,ruoyi-gateway,ruoyi-auth,ruoyi-modules/ruoyi-system,ruoyi-modules/ruoyi-file,ruoyi-modules/ruoyi-portal-kernel,ruoyi-modules/ruoyi-portal-appcenter,ruoyi-modules/ruoyi-portal-forum,ruoyi-modules/ruoyi-portal-requiredknowledge,ruoyi-modules/ruoyi-portal-resources,ruoyi-visual/ruoyi-monitor \
  -am package
```
预期：`BUILD SUCCESS`。再跑全部单测：
```bash
cd source
for m in ruoyi-portal-kernel ruoyi-portal-appcenter ruoyi-portal-forum ruoyi-portal-requiredknowledge ruoyi-portal-resources; do
  mvn -ntp -pl ruoyi-modules/$m -am -DskipTests=false test || exit 1
done
```
预期：五模块测试全绿（kernel 9 例、resources 4 例，其余至少编译过）。

- [ ] **Step 2: 部署产物门禁**

```bash
cd /Users/macmini/windows-info-serve
python3 deploy/scripts/generate-initdb.py
grep -c "portal-" deploy/initdb-mysql/90-nacos-config-content.sql   # 预期 ≥10（5 data-id × dev/prod）
cd deploy
docker compose --env-file .env config --quiet
for f in compose/services/*.yml; do docker compose --env-file .env -f "$f" config --quiet || exit 1; done
bash -n bin/svc.sh scripts/nacos-publish.sh
./build-images.sh
docker image ls | grep "portal-" | wc -l   # 预期 5
```

- [ ] **Step 3: 全栈起动**

```bash
cd /Users/macmini/windows-info-serve/deploy
docker compose --env-file .env up -d
for s in portal-kernel portal-appcenter portal-forum portal-requiredknowledge portal-resources; do
  bash bin/svc.sh start $s
done
sleep 90 && docker ps --format '{{.Names}}\t{{.Status}}' | grep infosys
```
预期：底座 + 5 业务服务全部 `Up`（飞腾/低配机上启动放宽等待）。

- [ ] **Step 4: e2e 冒烟 + MQ 链路人工验证**

```bash
APP_CENTER_BASE_URL=http://127.0.0.1:7010/prod-api node deploy/scripts/appcenter-v1-e2e.mjs
```
预期：e2e 通过（脚本覆盖登录/应用列表/收藏/需求/消息——消息端点现由 portal-kernel 承接，此即拆分回归的关键证据）。
人工链路（浏览器 `http://127.0.0.1:7010`）：
1. 论坛发回复 → 被回复人消息铃收到通知（forum→MQ→kernel 跨进程）；
2. 上传 docx 资料 → RabbitMQ 管理页(8173) `portal-resources.convert` 有消费记录 → 预览秒开；
3. 首页统计五个数字正常非零/合理。

- [ ] **Step 5: 故障演练（本批核心验收）**

对五个业务服务逐个执行「停 → 验 → 起」：
```bash
bash deploy/bin/svc.sh stop portal-forum
curl -s -o /dev/null -w 'forum(应503/失败) %{http_code}\n' http://127.0.0.1:7010/prod-api/infoservice/portal/forum/boards
curl -s -o /dev/null -w 'apps(应正常) %{http_code}\n'      http://127.0.0.1:7010/prod-api/appcenter/portal/apps
curl -s -o /dev/null -w 'stats(应200且话题数=0) %{http_code}\n' http://127.0.0.1:7010/prod-api/infoservice/portal/stats
bash deploy/bin/svc.sh start portal-forum
```
验收判据（每个服务同理，换对应 URL）：
| 停掉 | 必须仍正常 | 预期降级表现 |
|---|---|---|
| portal-forum | 应用中心/资料/应知应会/消息/首页 | 论坛页报错；首页话题数与活跃用户=0 |
| portal-resources | 其余全部 | 资料页报错；首页资源数与访问量=0 |
| portal-appcenter | 其余全部 | 应用中心页报错；首页应用数=0 |
| portal-requiredknowledge | 其余全部 | 应知应会页报错（不参与统计） |
| portal-kernel | 各 BC 页面浏览正常 | 首页统计/模块卡片/消息铃报错（内核属通用服务，宕机影响面本来就大——记录实测影响面供批次 C 探针参数参考） |
逐项记录实测结果。**任何「停 A 导致 B 也不可用」（kernel 除外，按上表判）= 验收失败**，回溯修复。
演练后全部恢复：`for s in ...; do bash deploy/bin/svc.sh start $s; done`，重跑 Step 4 的 e2e 确认无残伤。

- [ ] **Step 6: 收尾提交 + push（未 push 视为不存在）**

```bash
cd /Users/macmini/windows-info-serve
git add -A
git commit -m "test: 批次A全链路验收通过（构建/e2e/五服务故障演练）" --allow-empty
git push -u origin feature/batch-a-service-split
```
把验收记录（Step 1-5 关键输出摘要 + 故障演练表格实测值）整理进 PR 描述，PR 目标 `main`，标题建议 `feat: 批次A 门户五BC服务拆分`。**向用户报告时附上：对 spec 的两处修订说明（00-plan §修订）与 kernel 宕机影响面实测，供批次 B/C 计划参考。**

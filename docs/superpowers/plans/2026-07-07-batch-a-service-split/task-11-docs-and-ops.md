# Task 11: 运维脚本收尾 + 仓库规约文档更新

拆分完成后，把「事实变了」的所有文档改到位：构建命令、验证门禁、边界纪律描述、更新包登记。**文档没改 = 下一个开发者按旧地图走进沟里。**

**Files:**
- Modify: `RUNBOOK.md`、`AGENTS.md`、`CLAUDE.md`、`PORTS.md`（终检）、`deploy/updates/MANIFEST.md`、`docs/architecture/bounded-contexts.md`（§7 落地记录补记）

**Interfaces:**
- Consumes: T1-T10 全部产出。
- Produces: 与新架构一致的仓库规约；T12 按更新后的 RUNBOOK 执行全量验收。

- [ ] **Step 1: RUNBOOK.md**

1. 全量构建命令的 `-pl` 列表：删除 `ruoyi-modules/ruoyi-portal`，替换为五个新模块。终态列表（Docker Maven 与本机命令两处都改）：
```
ruoyi-visual/ruoyi-nacos,ruoyi-gateway,ruoyi-auth,ruoyi-modules/ruoyi-system,ruoyi-modules/ruoyi-file,ruoyi-modules/ruoyi-portal-kernel,ruoyi-modules/ruoyi-portal-appcenter,ruoyi-modules/ruoyi-portal-forum,ruoyi-modules/ruoyi-portal-requiredknowledge,ruoyi-modules/ruoyi-portal-resources,ruoyi-visual/ruoyi-monitor
```
（若 RUNBOOK 现文用短名列表，按其现有书写风格等价替换。）
2. 启动流程段追加「业务服务独立启停」小节：
```markdown
### 门户业务服务独立启停（批次 A 起）

平台底座（gateway/auth/system/file/monitor + 基础设施）仍由 deploy/docker-compose.yml 统一管理：
`docker compose --env-file .env up -d`

五个门户业务服务各自独立文件（deploy/compose/services/*.yml），用 svc.sh 单独操作，互不影响：
`bash deploy/bin/svc.sh <start|stop|restart|status|logs> <portal-kernel|portal-appcenter|portal-forum|portal-requiredknowledge|portal-resources>`

新装机全量启动顺序：基础设施+底座（主 compose）→ 五个业务服务（svc.sh 逐个 start，顺序任意）。
```
3. `generate-initdb.py` 说明处注明：新增服务 data-id 清单以脚本内 `new_configs` 为准；存量环境配置发布用 `deploy/scripts/nacos-publish.sh`。

- [ ] **Step 2: AGENTS.md**

1. **§2 架构边界**：把「ruoyi-portal 按 BC 分包……ArchUnit 强制」段替换为（保持原文风格）：
```markdown
- BC 地图法源不变：docs/architecture/bounded-contexts.md；部署形态修订见 docs/superpowers/specs/2026-07-07-service-split-cloud-native-design.md。
- 门户五 BC 已是独立服务（portal-kernel/appcenter/forum/requiredknowledge/resources，Maven 模块物理隔离，跨界 import 直接编译不过；原 ruoyi-portal ArchUnit 门禁随之退役）。
- 通信红线：业务服务之间禁止直接同步互调；同步只允许「→ 通用服务」方向（唯一例外 kernel→BC 统计，带超时降级）；跨 BC 协作一律走 RabbitMQ `portal.topic`（事件常量见 ruoyi-api-portal-kernel）。
- 表前缀归属不变：`app_/info_/res_/forum_/portal_/rk_`，跨域走接口或事件，禁跨界 SQL。`app_message` 表归 portal-kernel 专属（历史前缀错配，禁止其他服务直连）。
```
2. **§5 验证门禁表**：删除「涉及 ruoyi-portal 必跑 ArchUnit」一行，替换为：
```markdown
| 涉及 portal-* 任一模块 | `mvn -ntp -pl ruoyi-modules/<模块> -am -DskipTests=false test`（单测随模块走） |
| 改动 Dubbo 契约（ruoyi-api-*） | 提供方+全部消费方模块同批编译与镜像重建（§7 红线） |
| 改动 MQ 事件契约（PortalEventConstants/事件体） | 发布方与消费方服务同批重建 |
```
3. **§7 发布规约**：镜像清单说明补一句「门户五服务镜像见 deploy/.env `PORTAL_*_IMAGE`；`ruoyi-api-portal-kernel` 变更时五服务同批重建」。

- [ ] **Step 3: CLAUDE.md 红线速记第 2 条**

原文「BC 边界由 ArchUnit 强制（涉及 ruoyi-portal 必跑 `-DskipTests=false test`）；单文件 ≤800 行。」替换为：
```markdown
2. BC 边界由 Maven 模块物理隔离强制（portal 五服务禁互调，跨 BC 走 RabbitMQ，详见 AGENTS.md §2）；改动模块必跑 `-DskipTests=false test`；单文件 ≤800 行。
```

- [ ] **Step 4: MANIFEST.md 登记（本批无 SQL，仅 Nacos 配置动作）**

`deploy/updates/MANIFEST.md` 表格顶部追加一行（版本号取当前 `/VERSION` 的下一个补丁位，以实际为准）：
```markdown
| 0.3.4 | —（无 SQL） | Nacos `ry-config` | 待投放。批次 A 服务拆分：新增 data-id portal-kernel/appcenter/forum/requiredknowledge/resources.yml + 更新 ruoyi-gateway.yml，存量环境用 deploy/scripts/nacos-publish.sh 按 dev/prod 双 namespace 发布（禁 SQL 直写）；删除 data-id ruoyi-portal.yml（控制台手工，可选） |
```

- [ ] **Step 5: bounded-contexts.md §7 落地记录补记**

在 §7 开头的「落地记录（2026-07-04）」引用块之后追加一行：
```markdown
> 落地记录（批次 A）：五 BC 已晋升独立服务（spec 2026-07-07），规则 1-3 由 Maven 物理隔离与通信红线取代包级 ArchUnit；`BcBoundaryTest` 随 ruoyi-portal 退役。本文档 §4/§5 的部署裁决以 spec 为准，v4 全面修订随批次 D 执行。
```

- [ ] **Step 6: 终检 + 提交**

```bash
cd /Users/macmini/windows-info-serve
# PORTS.md 应含 8101/8106/8107/8108/8109/8110/8111/8114/8180/8190 十个后端端口行
grep -c "^| Portal" PORTS.md   # 预期 5
grep -rn "ruoyi-portal[^-]" RUNBOOK.md AGENTS.md CLAUDE.md PORTS.md && echo "文档残留" || echo "文档清查 OK"
git add -A
git commit -m "docs: 服务拆分后更新构建启停手册与仓库规约"
```

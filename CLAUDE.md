# CLAUDE.md

本仓库的**全部开发规约见根目录 [AGENTS.md](./AGENTS.md)**，对人类与所有 AI 代理强制生效，必读必遵。

最高优先级红线速记：

1. `main` 只进合并——开工先 `git fetch` 开分支，收工必 push（未 push 视为不存在）。
2. BC 边界由 Maven 模块物理隔离强制（portal 五服务禁互调，跨 BC 走 RabbitMQ，详见 AGENTS.md §2）；改动模块必跑 `-DskipTests=false test`；单文件 ≤800 行。
3. schema 变更三件套：种子 + `deploy/updates` 增量 + MANIFEST 登记；Nacos 配置禁止 SQL 直写。
4. 未过验证门禁（编译/构建/冒烟）不得声称完成。

架构：`docs/architecture/bounded-contexts.md` · 运行：`RUNBOOK.md` · 端口：`PORTS.md`

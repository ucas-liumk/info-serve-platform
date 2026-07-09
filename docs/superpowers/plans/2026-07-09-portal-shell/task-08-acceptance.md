# Task 8: Windows 构建部署 19100 + 用户目验（美观是验收项）

本任务由**主会话（编排者）驱动**（涉及 Windows 活栈与用户交互），不派实施 subagent。

**Files:** 无代码改动（构建产物与容器操作）。

**Interfaces:**
- Consumes: T1-T7 全部产出（分支已 push）。
- Produces: 用户目验签收记录；试点结论（是否推广到其他模块）。

- [ ] **Step 1: 推送与 Windows 构建**

Mac：`git push`。Windows（沿用 CP3 的克隆与经验：git-bash 全路径、`MSYS_NO_PATHCONV=1`）：
```bash
cd /e/gallant-dev/active/info-serve-batch-a
git fetch origin && git switch feature/resources-portal-shell && git pull
cd plus-ui && npm ci && npm run build:prod && npm run design:audit
```
预期：build 与 audit 双绿；产物在 `plus-ui/dist`。

- [ ] **Step 2: 切换 19100 容器指向新 dist**

```bash
export MSYS_NO_PATHCONV=1
docker rm -f infosys-batch-a-web
docker run -d --name infosys-batch-a-web \
  --network infosys-ruoyi-cloud-plus \
  -p 19100:80 \
  -v "E:/gallant-dev/active/info-serve-batch-a/plus-ui/dist:/usr/share/nginx/html:ro" \
  -v "E:/gallant-dev/active/info-serve-batch-a/deploy/nginx.conf:/etc/nginx/conf.d/default.conf:ro" \
  nginx:1.27-alpine
curl -s -o /dev/null -w '%{http_code}\n' http://127.0.0.1:19100/   # 预期 200
```
（回滚：把 `-v` 第一项换回 `E:/gallant-dev/active/info-serve/plus-ui/dist` 重建容器即回旧前端。）

- [ ] **Step 3: 回归与回退演练（编排者先行，用户目验前完成）**

- 首页/论坛/应用中心/应知应会/态势页逐页打开：与改版前零差异；
- 资料模块按 T6 Step 5 清单在 19100 真实环境复走一遍；
- 回退演练（Mac dev 上做，终审修正语义）：临时移除 InfoResources 路由的 `portalShell` meta → 确认**降级可用态**（无壳列表页，列表/搜索/上传可用）而非旧页复原 → 恢复 meta（结果记录，不提交临时改动）。正式回退 = git revert 整支。

- [ ] **Step 4: 用户目验（验收主体）**

请用户打开 `http://192.168.8.10:19100`（或 Windows 本机 `127.0.0.1:19100`），走查并逐项表态：
1. 顶栏观感（高度/留白/激活态/敬请期待灰显）；
2. 图标条：域导航与沉底捷径的层次感、「待」徽标、悬停反馈；
3. 分类栏：折叠/展开动效（320ms 是否舒服）、计数徽标、选中态；
4. 资料中心整体密度与留白节奏、空态文案（切我的上传/收藏看空态）；
5. 交流互动占位页观感与文案；
6. **总评：美观是否达标、试点是否值得推广**。
任何不满意项 → 记录为修改单，回对应任务修复后重新走本步骤。

- [ ] **Step 5: 收尾**

用户签收后：整理验收记录（含目验结论与截图如有）→ 建 PR（`feature/resources-portal-shell` → main，正文附 spec 链接、门禁数据、基线收紧前后计数、目验结论）→ 台账与记忆更新（试点结论 + 后续模块迁移排期建议）。PR 合并按仓库惯例走用户或 admin-merge 授权。
**并向用户提出两个立项提醒**（spec 明文）：①交流互动 MVP（公开笔记聚合流，占位保质期 ≤1 迭代）；②其余四模块按格律迁移的排期。

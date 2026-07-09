# Task 7: 死码清理判定 + 棘轮收紧 + design-system「门户壳层格律」章节

**Files:**
- Delete（判定后）: `plus-ui/src/views/portal/resources/components/MyResourcesDrawer.vue`、`ResourceSidebar.vue`、`ResourceFilterPanel.vue`（路径以实际为准）
- Modify: `plus-ui/design-audit-baseline.json`（`--update-baseline` 收紧）
- Modify: `docs/design/design-system.md`（新章节）

**Interfaces:**
- Consumes: T6 完成后的代码态。
- Produces: 收紧后的审计基线；壳层格律成为 design-system 正式规范（批次 D 第三方前端接入契约）。

- [ ] **Step 1: 死码判定与清理**

```bash
cd plus-ui
for c in MyResourcesDrawer ResourceSidebar ResourceFilterPanel; do echo "== $c =="; grep -rn "$c" src --include="*.vue" --include="*.ts" | grep -v "components/$c"; done
```
预期：三者引用数为 0（仅自身文件命中）→ `git rm` 各文件。若有残余引用（比如 preview 页也用了 FilterPanel），**保留被引用者**并在报告说明。同时复查 spec §3 的「老页面文件保留一个版本周期」条款：本次删的是**组件**而非页面本体——`resources/index.vue` 是原地瘦身无旧文件，条款不适用，报告注明即可。

- [ ] **Step 2: 棘轮收紧**

```bash
cd plus-ui && node ../deploy/scripts/design-audit.mjs --update-baseline && npm run design:audit
git diff plus-ui/design-audit-baseline.json
```
预期：四项计数**只降不升**（hex 应从 121 显著下降；若任何一项升高，说明壳层引入了硬编码——回修组件而不是接受基线）。记录新旧计数进报告。

- [ ] **Step 3: design-system.md 新章节**

在 `docs/design/design-system.md` 文末追加（标题层级与现文一致；若现文有编号章节则顺延编号）：

```markdown
## 门户壳层格律（Portal Shell v1，2026-07-09）

门户模块页统一由壳层承载，模块不得自建顶栏/侧栏/整页背景。法源：spec `docs/superpowers/specs/2026-07-09-resources-portal-shell-design.md`。

**三段结构**：TopBar（56px sticky，模块注册表驱动一级导航 + 消息铃 + 用户菜单）｜IconRail（48px 常驻：上=域导航，可带「待」徽标；下=个人捷径，横线分隔）｜CategoryRail（184px，可折叠至 16px，折叠态按 `portal-shell:<module>:cat-collapsed` 记忆）。

**接入方式**：路由 meta `{ portalShell: true, portalModule: '<key>', portalNoCategory?: true }` + 在 `src/layout/portal-shell/configs/` 注册 `ModuleNavConfig`（类型见 `src/layout/portal-shell/types.ts`）。旧 `usesOwnShell` 名单已冻结，禁止新增。

**交互契约**：分类与个人视图一律 URL query 驱动（`?category=`、`?scope=`），页面只观察 `route.query`，壳层与页面零回调耦合，链接可分享。

**视觉红线**：仅 `--ip-*` 令牌；折叠过渡 `--ip-motion-panel`；所有交互元素有 hover 与 `:focus-visible`；图标用 UnoCSS Iconify 类（`i-material-symbols:*` 线性）；分类栏交付加载/空/错误/正常/收起五态。

**迁移状态**：resources 已迁入（试点）；appcenter/forum/requiredknowledge/usage-dashboard 待迁；第三方模块自诞生即按本格律接入。
```

- [ ] **Step 4: 门禁 + 提交**

```bash
cd plus-ui && npm run build:prod && npm run design:audit && npm run test && cd ..
git add -A -- plus-ui/src plus-ui/design-audit-baseline.json docs/design/design-system.md
git commit -m "chore: 清理资料页死组件并收紧设计审计基线，壳层格律入规范"
```

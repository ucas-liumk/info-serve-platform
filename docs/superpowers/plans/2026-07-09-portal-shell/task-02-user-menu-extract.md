# Task 2: PortalUserMenu 抽取（home 行为与视觉零变化）

背景（recon 实证）：用户菜单**双处耦合**——下拉表现在 `HomeTopbar.vue` L19-44（el-dropdown + 头像 + profile/password/logout 三项，`emit('command')` 上抛），业务逻辑与两个弹窗在 `home/index.vue`（`handleUserCommand` L448、`logout` L428、`openProfile/loadProfile/submitProfile`、`openPasswordDialog/submitPassword`、个人信息 dialog L11-57、修改密码 dialog L59-91）。T3 的 TopBar 要复用完整用户菜单，必须先抽成自包含组件。

**Files:**
- Create: `plus-ui/src/layout/portal/components/PortalUserMenu.vue`（与 PortalNotificationBell 同目录，留在 design:audit 扫描树内）
- Modify: `plus-ui/src/views/portal/home/components/HomeTopbar.vue`（L19-44 下拉块替换为新组件；删 command emit 与相关 import）
- Modify: `plus-ui/src/views/portal/home/index.vue`（删两个 dialog 模板块、全部用户菜单 handler 与关联 state/import）

**Interfaces:**
- Consumes: `useUserStore`（avatar/nickname/logout）、现有 profile/password API 调用（随 handler 原样搬迁）。
- Produces: `<PortalUserMenu />` —— 无 props、无 emit 的自包含组件（内含下拉+两弹窗+退出确认）。T3 TopBar 直接引用。

- [ ] **Step 1: 创建组件（verbatim 搬迁，不重写）**

`PortalUserMenu.vue` 组装规则（代码从两处**原样剪切**，仅改归属）：
1. `<template>` 根 = HomeTopbar L19-44 的 `el-dropdown` 块原文（去掉对外 emit：`@command` 直接绑本组件内的 `handleUserCommand`）+ home/index.vue 的两个 `el-dialog` 块原文（个人信息 + 修改密码）。
2. `<script setup lang="ts">` = home/index.vue 中以下成员原样搬入：`handleUserCommand`、`logout`（含 ElMessageBox 确认与 `/login?redirect=` 跳转）、`openProfile/loadProfile/submitProfile`、`openPasswordDialog/submitPassword`，以及它们引用的全部 ref/reactive/表单校验规则/import。搬迁中**不改任何文案、样式类名、校验规则**。
3. `<style scoped>` = 上述模板块在两文件里对应的样式规则原样搬入（按类名在原文件 style 段检索裁剪）。
4. 搬完后在组件顶部加一行注释：`<!-- 自 HomeTopbar/home-index 抽取的自包含用户菜单；TopBar 与首页共用 -->`

- [ ] **Step 2: HomeTopbar 换用组件**

- L19-44 整块替换为 `<PortalUserMenu />`；
- `<script setup>` 删除 command emit 定义与不再使用的 icon/store import；
- 该文件其余（logo、时钟、手册入口）零改动。

- [ ] **Step 3: home/index.vue 清理**

删除：两个 dialog 模板块、Step 1 搬走的全部 handler/state/import、`<HomeTopbar @command=…>` 的监听参数（改为无参使用）。文件应净减约 120-160 行；除用户菜单链路外**不动任何其他内容**。

- [ ] **Step 4: 行为验证（手工清单，dev server）**

`cd plus-ui && npm run dev`，浏览器开 `http://127.0.0.1:7018/portal`（登录 admin/admin123，走 T1 配好的远端代理）：
- [ ] 头像与昵称显示如前；下拉三项齐全；
- [ ] 「个人信息」弹窗开、数据加载、取消/保存均可；
- [ ] 「修改密码」弹窗开、校验触发（输入不一致两次密码）；
- [ ] 「退出登录」出确认框→点取消（**勿真退出后再截图对照**——目检首页顶栏与改前一致）。
预期全为「与改前无差异」。

- [ ] **Step 5: 门禁 + 提交**

```bash
cd plus-ui && npm run build:prod && npm run design:audit && npm run test && cd ..
git add plus-ui/src/layout/portal/components/PortalUserMenu.vue plus-ui/src/views/portal/home
git commit -m "refactor: 抽取自包含门户用户菜单组件，首页行为不变"
```
（design:audit 计数只会持平或下降——hex 从 views/portal 挪进 layout/portal，同在扫描树。若意外上升，说明搬迁引入了新硬编码，回查。）

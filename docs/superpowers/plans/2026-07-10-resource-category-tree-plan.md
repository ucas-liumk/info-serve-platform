# 栏目→分类两级体系 · 实施计划

依据：`docs/superpowers/specs/2026-07-10-resource-category-tree-design.md`（v2，含全部裁决与文件:行号锚点，实施前必读）。
分支：`feature/resource-category-tree`。对比基准 origin/main（23551a6）。本地 main 过时勿用。

## 接口契约（先于实现钉死）

### C1 门户分类树 `GET /infoservice/portal/resources/category-tree`
入参（均可空）：`keyword, previewType, fileType, uploadedWithin, sizeRange`（previewType 与 fileType defaultIfBlank 合流；枚举同现列表接口）。
返回 `R<List<InfoResourceCategoryTreeVo>>`：
```json
[{ "categoryId":300000, "categoryCode":"general", "categoryName":"综合资料", "orderNum":0,
   "children":[{ "categoryId":300001, "categoryCode":"policy", "categoryName":"政策制度", "orderNum":1, "resourceCount":6 }] }]
```
两级均 status='0' 且 del_flag='0'；栏目停用整组不出；空栏目（无活跃子分类）不出；排序 order_num asc, create_time desc。计数：resource.status='0' + 入参筛选，**单条 GROUP BY category_id** 聚合（mapper XML），不含 categoryCode 条件。

### C2 列表接口 categoryCode 多值
`GET /portal/resources` 的 categoryCode 支持逗号串：split→trim→去空去'all'；空集→不加条件；解析出 code 集后查活跃分类，全无命中→`eq(category_id,-1)`空集，有命中→`in(category_id,命中集)`。单值行为与现状 bit 级一致。

### C3 平铺接口收窄为仅二级（形状不变）
`GET /portal/resources/categories` 与 `GET /resource/category/options`：加 `parent_id IS NOT NULL` 条件，返回结构/字段零变化。

### C4 管理树 `GET /resource/category/treeList`
权限 `infoservice:resourceCategory:list`。返回 `R<List<InfoResourceCategoryVo>>` 全量平铺（含 parentId 字段、含停用行、不分页），前端 handleTree 组树。Vo 加 parentId。旧 `/list` 分页接口保留不动。

### C5 CRUD 校验（服务层）
- 新增/修改：parentId 为空=栏目；非空=分类且父必须存在、是栏目（父的 parentId 为空）、未删——违者抛业务异常（两级封顶）。categoryCode 唯一校验跨两级（活跃行）。
- 删除：栏目有未删子分类→拒绝；分类有未删资料（info_resource.del_flag='0'）→拒绝；报中文友好错误。

## 任务分解（三线并行 → 门禁 → 审查 → 部署）

### T1 后端全链（schema 四件套 + Java + 单测）
Owned files：`source/script/sql/postgres/postgres_info_service.sql`、`deploy/initdb-postgres/dumps/**`（由脚本再生成）、`deploy/updates/0.3.7-resource-category-tree.sql`（新建）、`deploy/updates/MANIFEST.md`、`source/ruoyi-modules/ruoyi-portal-resources/**`。
1. 种子：建表加 `parent_id int8 default null`；插栏目行 300000/general/综合资料（ON CONFLICT DO NOTHING，先核实 300000 未被占用）；既有 300001-300003 行补 parent_id=300000。跑 `deploy/scripts/generate-initdb.py`（先读脚本头部确认用法）再生成 dumps，diff 校验只有预期变化。
2. 增量 0.3.7（幂等三段）+ MANIFEST 登记（目标库 PG ry-cloud，状态 待投放）。
3. Java：entity/Vo 加 parentId；TreeVo 新建；mapper XML 聚合查询；service 树组装/分面计数/多值解析/C5 校验；两 controller 新端点；C2/C3 改造。TDD：先写测试（树组装、多值解析 all/单/多/无命中、两级封顶、删除校验、计数入参映射——纯单测 Mockito 风格，范本 ResourceConvertListenerTest）再实现。
4. 门禁自跑：`cd source && JAVA_HOME=$(/usr/libexec/java_home -v 17) '/Applications/IntelliJ IDEA.app/Contents/plugins/maven/lib/maven3/bin/mvn' -o test -DskipTests=false -pl ruoyi-modules/ruoyi-portal-resources -am`（日志中死信 ERROR 堆栈是故意打印，勿误判）。

### T2 门户前端（含共享类型，先于 T3）
Owned files：`plus-ui/src/api/portal/resources.ts`、`plus-ui/src/api/infoservice/types.ts`（本轮该文件全部改动归 T2：ResourceCategory 加 parentId/children、ResourceCategoryForm 加 parentId、ResourcePortalQuery.categoryCode 注释逗号串、新 CategoryTreeNode 类型）、`plus-ui/src/views/portal/resources/index.vue`、`components/ResourceSidebar.vue`、`components/ResourceFilterPanel.vue`、新纯函数模块 `views/portal/resources/categoryFacets.ts` + `categoryFacets.test.ts`、`plus-ui/package.json`（加 `"test": "vitest run"`）。
1. api：`getCategoryTree(params)` → C1。
2. index.vue：`selectedCategories: string[]`（空=全部）；标题空→全部资源/单→分类名/多→已选 N 个分类；chip 条（结果区顶部，单撤+清除）；联动：keyword/工具条变化并行刷列表+树计数，勾选分类只刷列表；上传成功刷双方。
3. FilterPanel 重写（两级树/折叠/三态组勾选/计数/清除，纯 --ip-* 与 --ip-motion-* 令牌零新硬编码）；Sidebar 透传改造。
4. 纯函数（组树消费/三态计算/勾选归并/逗号串编解码）先写 vitest 再实现；门禁自跑：`cd plus-ui && npm test && npm run design:audit`（删旧硬编码后 `node ../deploy/scripts/design-audit.mjs --update-baseline` 下调基线并提交）。npm 工作流禁 pnpm。

### T3 管理前端（T2 完成后）
Owned files：`plus-ui/src/views/admin/resources/category/index.vue`、`plus-ui/src/api/infoservice/admin.ts`。
照 `views/admin/system/dept/index.vue` 范式整页替换：treeList+handleTree、row-key/tree-props、展开折叠全部、去 selection 批量删、行内「新增分类」（栏目行）/修改/删除、弹窗加「上级栏目」（建栏目=空；建分类必选；el-select 只列栏目即可）、el-switch 停用保留。admin.ts 加 `listResourceCategoryTree`。

### T4 集成门禁 + 代码审查
全量门禁复跑（T1 mvn / T2 vitest+audit / `npm run build:prod`）；对 origin/main...HEAD 全 diff 做多维审查（正确性/兼容回归/安全/规约）+ 对抗核实，CRITICAL/HIGH 必修。

### T5 部署验收（Windows）+ 截图比对
push 分支 → Windows 克隆 `E:/gallant-dev/active/info-serve-batch-a` fetch+checkout → ①PG 容器应用 0.3.7 增量；②portal-resources 重新打包出镜像并重启该容器；③plus-ui `npm ci?（已有 node_modules 则 npm run build:prod）` 出 dist，swap infosys-batch-a-web 挂载到新 dist；④截图门户资料页（登录态）比对样机 → 交用户验收。**用户认可前不关单、不发 PR。**

## 风险与回滚
- 0.3.7 增量纯加性（加列+加行），回滚= UPDATE parent_id=NULL + 删栏目行；前端回滚=swap 回 live dist（已演练）。
- 与批次 A 发布线的版本合流：MANIFEST 0.3.6（批次A Nacos）与 0.3.7 各自独立成包，互不绑定。

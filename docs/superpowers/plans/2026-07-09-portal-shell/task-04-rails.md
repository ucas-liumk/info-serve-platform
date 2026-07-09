# Task 4: IconRail + CategoryRail（折叠/记忆/动效/五态）

**Files:**
- Create: `plus-ui/src/layout/portal-shell/IconRail.vue`
- Create: `plus-ui/src/layout/portal-shell/CategoryRail.vue`

**Interfaces:**
- Consumes: T1 类型（NavDomain/NavShortcut/CategoryItem/ModuleNavConfig）。
- Produces:
  - `<IconRail :domains :shortcuts />` —— 48px 常驻；域高亮=当前路由匹配；点击=router.push（捷径带 query）。
  - `<CategoryRail :title :query-key :fetch :module-key />` —— 展开 184px/收起 16px；选中写 `?{queryKey}=key`；`全部`(key='all') 内建首项=移除该 query；折叠态记忆 `portal-shell:<moduleKey>:cat-collapsed`。

- [ ] **Step 1: IconRail 实现（完整代码）**

`plus-ui/src/layout/portal-shell/IconRail.vue`：
```vue
<template>
  <nav class="ps-rail" aria-label="模块内导航">
    <div class="ps-rail__domains">
      <component
        v-for="d in domains" :key="d.key"
        :is="d.badge === 'soon' ? 'RouterLink' : 'RouterLink'"
        :to="d.route"
        class="ps-rail__item"
        :class="{ 'is-active': isActive(d.route) }"
      >
        <span class="ps-rail__badge" v-if="d.badge === 'soon'">待</span>
        <span :class="d.icon" class="ps-rail__icon" aria-hidden="true" />
        <span class="ps-rail__label">{{ d.label }}</span>
      </component>
    </div>
    <div class="ps-rail__spacer" />
    <div class="ps-rail__shortcuts" v-if="shortcuts.length">
      <RouterLink
        v-for="s in shortcuts" :key="s.key"
        :to="{ path: s.route, query: s.query }"
        class="ps-rail__item"
        :class="{ 'is-active': isShortcutActive(s) }"
      >
        <span :class="s.icon" class="ps-rail__icon" aria-hidden="true" />
        <span class="ps-rail__label">{{ s.label }}</span>
      </RouterLink>
    </div>
  </nav>
</template>

<script setup lang="ts">
import { useRoute } from 'vue-router';
import type { NavDomain, NavShortcut } from './types';

const props = defineProps<{ domains: NavDomain[]; shortcuts: NavShortcut[] }>();
const route = useRoute();

// 域高亮：路径匹配且未处于任何捷径的 scope 态（捷径激活时域不抢高亮）
const activeScope = () => (route.query.scope as string) || '';
const isActive = (path: string) =>
  (route.path === path || route.path.startsWith(path + '/')) && !props.shortcuts.some((s) => s.query.scope === activeScope());
const isShortcutActive = (s: NavShortcut) => route.path === s.route && activeScope() === s.query.scope;
</script>

<style scoped>
.ps-rail {
  display: flex;
  flex-direction: column;
  width: 48px;
  flex-shrink: 0;
  padding: 8px 4px;
  background: var(--ip-neutral-900);
  gap: 4px;
}
.ps-rail__domains, .ps-rail__shortcuts { display: flex; flex-direction: column; gap: 4px; }
.ps-rail__spacer { flex: 1; }
.ps-rail__shortcuts { border-top: 1px solid var(--ip-neutral-700); padding-top: 8px; }
.ps-rail__item {
  position: relative;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 2px;
  padding: 8px 0;
  border-radius: var(--ip-radius-sm);
  color: var(--ip-neutral-400);
  text-decoration: none;
  transition: color var(--ip-motion-fast) var(--ip-motion-ease), background-color var(--ip-motion-fast) var(--ip-motion-ease);
}
.ps-rail__item:hover { color: var(--ip-neutral-0); background: var(--ip-neutral-700); }
.ps-rail__item.is-active { color: var(--ip-neutral-0); background: var(--ip-primary-600); }
.ps-rail__icon { font-size: 20px; }
.ps-rail__label { font-size: var(--ip-font-caption); transform: scale(0.9); white-space: nowrap; }
.ps-rail__badge {
  position: absolute; top: 2px; right: 4px;
  padding: 0 4px;
  border-radius: var(--ip-radius-full);
  background: var(--ip-warning-base);
  color: var(--ip-neutral-0);
  font-size: var(--ip-font-caption);
  transform: scale(0.8);
}
</style>
```
> 令牌档位名以 tokens.scss 实际为准（如 `--ip-warning-base` 若叫其他名就近换），禁止硬编码。

- [ ] **Step 2: CategoryRail 实现（完整代码，含五态与动效）**

`plus-ui/src/layout/portal-shell/CategoryRail.vue`：
```vue
<template>
  <aside class="ps-cat" :class="{ 'is-collapsed': collapsed }" aria-label="分类导航">
    <template v-if="!collapsed">
      <div class="ps-cat__head">
        <span class="ps-cat__title">{{ title }}</span>
        <button class="ps-cat__fold" aria-label="收起分类栏" @click="collapsed = true">
          <span class="i-material-symbols:left-panel-close-outline-rounded" aria-hidden="true" />
        </button>
      </div>

      <div v-if="loading" class="ps-cat__state">
        <el-skeleton :rows="5" animated />
      </div>
      <div v-else-if="error" class="ps-cat__state">
        <p class="ps-cat__state-text">分类加载失败</p>
        <el-button size="small" @click="load">重试</el-button>
      </div>
      <p v-else-if="!items.length" class="ps-cat__state ps-cat__state-text">暂无分类</p>

      <ul v-else class="ps-cat__list">
        <li v-for="c in withAll" :key="c.key">
          <button class="ps-cat__item" :class="{ 'is-active': activeKey === c.key }" @click="select(c.key)">
            <span class="ps-cat__label">{{ c.label }}</span>
            <span v-if="c.count != null" class="ps-cat__count">{{ c.count }}</span>
          </button>
        </li>
      </ul>
    </template>

    <button v-else class="ps-cat__expand" aria-label="展开分类栏" @click="collapsed = false">
      <span class="i-material-symbols:left-panel-open-outline-rounded" aria-hidden="true" />
    </button>
  </aside>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import type { CategoryItem } from './types';

const props = defineProps<{
  title: string;
  queryKey: string;
  fetch: () => Promise<CategoryItem[]>;
  moduleKey: string;
}>();

const route = useRoute();
const router = useRouter();
const collapsed = useStorage(`portal-shell:${props.moduleKey}:cat-collapsed`, false);
const items = ref<CategoryItem[]>([]);
const loading = ref(true);
const error = ref(false);

const withAll = computed<CategoryItem[]>(() => [{ key: 'all', label: '全部' }, ...items.value]);
const activeKey = computed(() => (route.query[props.queryKey] as string) || 'all');

const select = (key: string) => {
  const query = { ...route.query };
  if (key === 'all') delete query[props.queryKey];
  else query[props.queryKey] = key;
  router.push({ path: route.path, query });
};

const load = async () => {
  loading.value = true;
  error.value = false;
  try {
    items.value = await props.fetch();
  } catch {
    error.value = true;
  } finally {
    loading.value = false;
  }
};
onMounted(load);
</script>

<style scoped>
.ps-cat {
  width: 184px;
  flex-shrink: 0;
  background: var(--ip-neutral-50);
  border-right: 1px solid var(--ip-neutral-200);
  padding: 12px 8px;
  overflow-y: auto;
  transition: width var(--ip-motion-panel) var(--ip-motion-ease), padding var(--ip-motion-panel) var(--ip-motion-ease);
}
.ps-cat.is-collapsed { width: 16px; padding: 12px 0; overflow: hidden; }
@media (prefers-reduced-motion: reduce) { .ps-cat { transition: none; } }
.ps-cat__head { display: flex; align-items: center; justify-content: space-between; margin-bottom: 8px; padding: 0 4px; }
.ps-cat__title { font-size: var(--ip-font-caption); font-weight: 600; color: var(--ip-neutral-500); text-transform: uppercase; letter-spacing: 0.05em; }
.ps-cat__fold, .ps-cat__expand {
  display: flex; align-items: center; justify-content: center;
  border: none; background: transparent; color: var(--ip-neutral-400);
  border-radius: var(--ip-radius-sm); cursor: pointer; padding: 4px;
  transition: color var(--ip-motion-fast) var(--ip-motion-ease), background-color var(--ip-motion-fast) var(--ip-motion-ease);
}
.ps-cat__fold:hover, .ps-cat__expand:hover { color: var(--ip-primary-600); background: var(--ip-primary-50); }
.ps-cat__expand { width: 100%; height: 100%; }
.ps-cat__list { list-style: none; margin: 0; padding: 0; display: flex; flex-direction: column; gap: 2px; }
.ps-cat__item {
  display: flex; align-items: center; justify-content: space-between; gap: 8px;
  width: 100%; padding: 6px 8px;
  border: none; background: transparent; text-align: left; cursor: pointer;
  border-radius: var(--ip-radius-sm);
  font-size: var(--ip-font-body);
  color: var(--ip-neutral-700);
  transition: color var(--ip-motion-fast) var(--ip-motion-ease), background-color var(--ip-motion-fast) var(--ip-motion-ease);
}
.ps-cat__item:hover { background: var(--ip-neutral-100); }
.ps-cat__item.is-active { background: var(--ip-primary-50); color: var(--ip-primary-700); font-weight: 600; }
.ps-cat__label { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.ps-cat__count { font-size: var(--ip-font-caption); color: var(--ip-neutral-400); }
.ps-cat__item.is-active .ps-cat__count { color: var(--ip-primary-600); }
.ps-cat__state { padding: 12px 8px; display: flex; flex-direction: column; gap: 8px; align-items: flex-start; }
.ps-cat__state-text { font-size: var(--ip-font-body); color: var(--ip-neutral-400); margin: 0; }
</style>
```
> `useStorage` 全局自动导入（勿手写 import，会撞 auto-import 规则）。图标类名若 Iconify 库中不存在（构建报未知图标），在 material-symbols 中就近选形近图标并报告替换。

- [ ] **Step 3: 门禁 + 提交**

组件在 T5 组装后才有运行时挂载点，本任务以构建与审计为门禁：
```bash
cd plus-ui && npm run build:prod && npm run design:audit && npm run test && cd ..
git add plus-ui/src/layout/portal-shell/IconRail.vue plus-ui/src/layout/portal-shell/CategoryRail.vue
git commit -m "feat: 门户壳层图标条与分类栏（折叠记忆+动效+五态）"
```

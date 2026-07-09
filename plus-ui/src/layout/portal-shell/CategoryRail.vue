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

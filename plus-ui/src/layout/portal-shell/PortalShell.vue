<template>
  <div class="ps-shell">
    <TopBar />
    <div class="ps-shell__body">
      <IconRail v-if="nav" :domains="nav.domains" :shortcuts="nav.shortcuts" />
      <CategoryRail
        v-if="nav?.category && showCategory"
        :key="nav.moduleKey"
        :title="nav.category.title"
        :query-key="nav.category.queryKey"
        :fetch="nav.category.fetch"
        :module-key="nav.moduleKey"
      />
      <main class="ps-shell__main">
        <router-view />
      </main>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue';
import { useRoute } from 'vue-router';
import TopBar from './TopBar.vue';
import IconRail from './IconRail.vue';
import CategoryRail from './CategoryRail.vue';
import { resolveModuleNav } from './resolveModuleNav';

const route = useRoute();
const nav = computed(() => resolveModuleNav(route.meta.portalModule as string | undefined));
// 无分类语义的域不渲染分类栏（spec §2）：当前路由命中的 domain 若 badge=soon 或非 center 域，由 meta.portalNoCategory 显式声明
const showCategory = computed(() => !route.meta.portalNoCategory);
</script>

<style scoped>
.ps-shell { display: flex; flex-direction: column; min-height: 100vh; background: var(--ip-neutral-50); }
.ps-shell__body { display: flex; flex: 1; min-height: 0; }
.ps-shell__main { flex: 1; min-width: 0; padding: 16px 24px; max-width: var(--ip-layout-max); }
@media (max-width: 979px) {
  /* 试点期移动端保功能：分类栏隐藏，图标条转横条 */
  .ps-shell__body { flex-direction: column; }
  .ps-shell__body :deep(.ps-rail) { flex-direction: row; width: 100%; height: 48px; padding: 4px 8px; }
  .ps-shell__body :deep(.ps-rail__spacer) { flex: 1; }
  .ps-shell__body :deep(.ps-rail__shortcuts) { border-top: none; border-left: 1px solid var(--ip-neutral-700); padding: 0 0 0 8px; flex-direction: row; }
  .ps-shell__body :deep(.ps-rail__domains) { flex-direction: row; }
  .ps-shell__body :deep(.ps-cat) { display: none; }
}
</style>

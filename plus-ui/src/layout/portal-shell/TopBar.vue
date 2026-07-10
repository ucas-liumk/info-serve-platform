<template>
  <header class="ps-topbar">
    <RouterLink to="/portal" class="ps-topbar__brand" aria-label="返回门户首页">
      <span class="i-material-symbols:hub-outline ps-topbar__logo" aria-hidden="true" />
      <span class="ps-topbar__title">信息中心数智服务平台</span>
    </RouterLink>

    <nav class="ps-topbar__nav" aria-label="门户模块导航">
      <template v-for="m in modules" :key="m.moduleCode">
        <RouterLink
          v-if="m.status === '0' && m.entryPath"
          :to="m.entryPath"
          class="ps-topbar__item"
          :class="{ 'is-active': isActive(m.entryPath) }"
        >{{ m.moduleName }}</RouterLink>
        <el-tooltip v-else-if="m.status === '1'" content="敬请期待" placement="bottom">
          <span class="ps-topbar__item is-soon" aria-disabled="true">{{ m.moduleName }}</span>
        </el-tooltip>
      </template>
    </nav>

    <div class="ps-topbar__actions">
      <PortalNotificationBell />
      <PortalUserMenu />
    </div>
  </header>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue';
import { useRoute } from 'vue-router';
import { listPortalModules, type PortalModuleItem } from '@/api/portal/module';
import PortalNotificationBell from '@/layout/portal/components/PortalNotificationBell.vue';
import PortalUserMenu from '@/layout/portal/components/PortalUserMenu.vue';

const route = useRoute();
const modules = ref<PortalModuleItem[]>([]);

const isActive = (entryPath: string) => route.path === entryPath || route.path.startsWith(entryPath + '/');

onMounted(async () => {
  try {
    const res = await listPortalModules();
    modules.value = ((res as any).data ?? [])
      .filter((m: PortalModuleItem) => m.status !== '2')
      .sort((a: PortalModuleItem, b: PortalModuleItem) => (a.sortOrder ?? 0) - (b.sortOrder ?? 0));
  } catch {
    modules.value = []; // 注册表不可用：只影响导航项，铃/用户菜单照常
  }
});
</script>

<style scoped>
.ps-topbar {
  position: sticky;
  top: 0;
  z-index: 30;
  display: flex;
  align-items: center;
  gap: 24px;
  height: 56px;
  padding: 0 24px;
  background: var(--ip-neutral-0);
  box-shadow: var(--ip-shadow-sm);
}
.ps-topbar__brand {
  display: flex;
  align-items: center;
  gap: 8px;
  color: var(--ip-neutral-900);
  text-decoration: none;
  border-radius: var(--ip-radius-sm);
}
.ps-topbar__logo { font-size: var(--ip-font-title); color: var(--ip-primary-600); }
.ps-topbar__title { font-size: var(--ip-font-title-sm); font-weight: 600; white-space: nowrap; }
.ps-topbar__nav { display: flex; align-items: center; gap: 4px; min-width: 0; overflow-x: auto; }
.ps-topbar__item {
  padding: 8px 12px;
  border-radius: var(--ip-radius-full);
  font-size: var(--ip-font-body);
  color: var(--ip-neutral-600);
  text-decoration: none;
  white-space: nowrap;
  transition: color var(--ip-motion-fast) var(--ip-motion-ease), background-color var(--ip-motion-fast) var(--ip-motion-ease);
}
.ps-topbar__item:hover { color: var(--ip-primary-700); background: var(--ip-primary-50); }
.ps-topbar__item.is-active { color: var(--ip-primary-700); background: var(--ip-primary-100); font-weight: 600; }
.ps-topbar__item.is-soon { color: var(--ip-neutral-400); cursor: not-allowed; }
.ps-topbar__actions { margin-left: auto; display: flex; align-items: center; gap: 12px; }
</style>

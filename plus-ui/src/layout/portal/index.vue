<template>
  <div class="portal-layout" :class="{ 'home-mode': isHome }">
    <PortalShell v-if="usesPortalShell" />
    <template v-else>
      <PortalSidebar v-if="!isHome && !usesOwnShell" />
      <section class="portal-main" :class="{ 'home-main': isHome, 'own-shell-main': usesOwnShell }">
        <router-view />
      </section>
    </template>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue';
import { useRoute } from 'vue-router';
import PortalSidebar from './components/PortalSidebar.vue';
import PortalShell from '@/layout/portal-shell/PortalShell.vue';

const route = useRoute();
const isHome = computed(() => route.name === 'InfoPortalHome');
const usesPortalShell = computed(() => route.meta.portalShell === true);
// 冻结名单：新模块一律走 meta.portalShell，禁止再往这里加名字（spec 2026-07-09 §3）
const usesOwnShell = computed(
  () =>
    route.name === 'InfoTools' ||
    route.name === 'InfoResources' ||
    route.name === 'InfoResourcePreview' ||
    route.name === 'InfoForum' ||
    route.name === 'RequiredKnowledgeHome' ||
    route.name === 'RequiredKnowledgeSubject' ||
    route.name === 'RequiredKnowledgeExam'
);
</script>

<style scoped>
.portal-layout {
  display: flex;
  min-height: 100vh;
  background: linear-gradient(135deg, rgba(232, 205, 149, 0.24), transparent 34%), linear-gradient(180deg, #f7f8f6 0%, #eef3f2 46%, #f7f8fb 100%);
  color: #111827;
  font-family: 'HarmonyOS Sans SC', 'PingFang SC', 'Microsoft YaHei', sans-serif;
}

.portal-layout.home-mode {
  display: block;
  background: #f7fbff;
}

.portal-main {
  flex: 1;
  min-width: 0;
  min-height: 100vh;
  overflow-x: hidden;
}

.portal-main.home-main {
  min-height: 100vh;
  overflow: hidden;
}

.portal-main.own-shell-main {
  overflow: hidden;
}

@media (max-width: 980px) {
  .portal-layout {
    display: block;
  }
}
</style>

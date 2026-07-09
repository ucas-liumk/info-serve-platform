<template>
  <nav class="ps-rail" aria-label="模块内导航">
    <div class="ps-rail__domains">
      <RouterLink
        v-for="d in domains" :key="d.key"
        :to="d.route"
        class="ps-rail__item"
        :class="{ 'is-active': isActive(d) }"
      >
        <span class="ps-rail__badge" v-if="d.badge === 'soon'">待</span>
        <span :class="d.icon" class="ps-rail__icon" aria-hidden="true" />
        <span class="ps-rail__label">{{ d.label }}</span>
      </RouterLink>
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
import { computed } from 'vue';
import { useRoute } from 'vue-router';
import type { NavDomain, NavShortcut } from './types';
import { pickActiveDomain } from './pickActiveDomain';

const props = defineProps<{ domains: NavDomain[]; shortcuts: NavShortcut[] }>();
const route = useRoute();

// 域高亮：最长前缀胜出（嵌套子路由不双高亮父域），捷径 scope 激活时域不抢高亮
const activeScope = () => (route.query.scope as string) || '';
const activeDomainKey = computed(() => pickActiveDomain(route.path, activeScope(), props.domains, props.shortcuts));
const isActive = (d: NavDomain) => d.key === activeDomainKey.value;
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
  background: var(--ip-warning);
  color: var(--ip-neutral-0);
  font-size: var(--ip-font-caption);
  transform: scale(0.8);
}
</style>

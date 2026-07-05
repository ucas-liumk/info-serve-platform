<template>
  <aside class="rk-sidebar">
    <router-link class="rk-side-brand" to="/portal/required-knowledge" aria-label="应知应会首页">
      <span class="rk-side-brand__icon">
        <IconBook />
      </span>
      <span class="rk-side-brand__copy">
        <strong>应知应会</strong>
        <em>学习与考试</em>
      </span>
    </router-link>

    <button class="rk-home-button" type="button" @click="goPortalHome">
      <el-icon><House /></el-icon>
      <span>返回首页</span>
    </button>

    <nav class="rk-side-panel" aria-label="应知应会导航">
      <div class="rk-panel-title">
        <span>学习栏目</span>
        <em>{{ subjectCount }} 科目</em>
      </div>

      <router-link :class="['rk-side-link', { active: activeCode === 'overview' }]" to="/portal/required-knowledge">
        <span class="rk-link-icon"><IconOverview /></span>
        <strong>科目总览</strong>
      </router-link>

      <router-link
        v-for="group in groups"
        :key="group.code"
        :class="['rk-side-link', { active: activeCode === group.code }]"
        :to="{ path: '/portal/required-knowledge', hash: `#${group.code}` }"
      >
        <span class="rk-link-icon"><IconFolder /></span>
        <strong>{{ group.name }}</strong>
      </router-link>
    </nav>

    <section class="rk-side-summary" aria-label="学习概览">
      <div>
        <strong>{{ subjectCount }}</strong>
        <span>科目</span>
      </div>
      <div>
        <strong>{{ questionCount }}</strong>
        <span>题目</span>
      </div>
    </section>
  </aside>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router';
import { House } from '@element-plus/icons-vue';
import IconBook from '~icons/material-symbols/menu-book-outline-rounded';
import IconFolder from '~icons/material-symbols/folder-managed-outline-rounded';
import IconOverview from '~icons/material-symbols/dashboard-outline-rounded';
import { PORTAL_HOME_PATH } from '@/constants/router';
defineProps<{
  activeCode: string | 'overview';
  subjectCount: number;
  questionCount: number;
  groups: Array<{ code: string; name: string }>;
}>();

const router = useRouter();

const goPortalHome = () => {
  router.push(PORTAL_HOME_PATH);
};
</script>

<style scoped>
.rk-sidebar {
  position: sticky;
  top: 18px;
  align-self: start;
  max-height: calc(100vh - 36px);
  min-height: 0;
  display: grid;
  grid-template-rows: auto auto auto auto;
  gap: 14px;
  overflow: hidden;
}

.rk-side-brand,
.rk-home-button,
.rk-side-panel,
.rk-side-summary {
  border: 1px solid var(--rk-border);
  border-radius: var(--ip-radius-md);
  background: var(--ip-neutral-0);
  box-shadow: var(--ip-shadow-sm);
}

.rk-side-brand {
  position: relative;
  min-height: 86px;
  display: grid;
  grid-template-columns: 42px minmax(0, 1fr);
  align-items: center;
  gap: 12px;
  box-sizing: border-box;
  padding: 16px;
  overflow: hidden;
  color: inherit;
  text-decoration: none;
  background: linear-gradient(135deg, var(--rk-accent-soft), transparent 48%), var(--ip-neutral-0);
}

.rk-side-brand::after {
  content: '';
  position: absolute;
  right: -24px;
  bottom: -32px;
  width: 104px;
  height: 72px;
  border: 1px solid var(--rk-accent-border);
  border-radius: var(--ip-radius-md);
  transform: rotate(-14deg);
  pointer-events: none;
}

.rk-side-brand__icon,
.rk-link-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  color: var(--rk-accent);
  background: var(--rk-accent-soft);
}

.rk-side-brand__icon {
  position: relative;
  z-index: 1;
  width: 42px;
  height: 42px;
  border-radius: var(--ip-radius-md);
  font-size: 24px;
}

.rk-side-brand__copy {
  position: relative;
  z-index: 1;
  min-width: 0;
  display: grid;
  gap: 4px;
}

.rk-side-brand strong {
  color: var(--rk-title);
  font-size: var(--ip-font-title-sm);
  font-weight: 700;
  line-height: 1.4;
}

.rk-side-brand em,
.rk-panel-title em,
.rk-side-summary span {
  color: var(--rk-muted);
  font-style: normal;
}

.rk-side-brand em {
  font-size: var(--ip-font-hint);
  font-weight: 600;
  line-height: 1.5;
  white-space: nowrap;
}

.rk-home-button {
  width: 100%;
  height: 40px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  color: var(--rk-text);
  font-size: var(--ip-font-body);
  font-weight: 700;
  line-height: 1.5;
  cursor: pointer;
  transition:
    border-color var(--ip-motion-fast) var(--ip-motion-ease),
    background var(--ip-motion-fast) var(--ip-motion-ease),
    color var(--ip-motion-fast) var(--ip-motion-ease),
    transform var(--ip-motion-fast) var(--ip-motion-ease);
}

.rk-home-button:hover {
  border-color: var(--rk-primary);
  background: var(--rk-primary-soft);
  color: var(--rk-primary);
  transform: translateY(-1px);
}

.rk-side-panel {
  min-height: 0;
  display: flex;
  flex-direction: column;
  gap: 10px;
  padding: 14px;
  overflow: auto;
}

.rk-panel-title {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  padding: 2px 2px 6px;
}

.rk-panel-title span {
  color: var(--rk-title);
  font-size: var(--ip-font-emphasis);
  font-weight: 700;
  line-height: 1.4;
}

.rk-panel-title em {
  font-size: var(--ip-font-hint);
  font-weight: 600;
  line-height: 1.5;
}

.rk-side-link {
  min-height: 58px;
  display: grid;
  grid-template-columns: 34px minmax(0, 1fr);
  align-items: center;
  gap: 10px;
  border: 1px solid transparent;
  border-radius: var(--ip-radius-sm);
  padding: 12px;
  background: var(--ip-neutral-50);
  color: var(--rk-text);
  text-decoration: none;
  transition:
    border-color var(--ip-motion-fast) var(--ip-motion-ease),
    background var(--ip-motion-fast) var(--ip-motion-ease),
    color var(--ip-motion-fast) var(--ip-motion-ease),
    transform var(--ip-motion-fast) var(--ip-motion-ease);
}

.rk-side-link:hover,
.rk-side-link.active {
  border-color: var(--rk-accent-border);
  background: var(--rk-accent-soft);
  color: var(--rk-accent);
  transform: translateY(-1px);
}

.rk-link-icon {
  width: 34px;
  height: 34px;
  border-radius: var(--ip-radius-sm);
  font-size: 20px;
}

.rk-side-link strong {
  min-width: 0;
  color: inherit;
  font-size: var(--ip-font-body);
  font-weight: 700;
  line-height: 1.5;
}

.rk-side-summary {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 8px;
  padding: 12px;
}

.rk-side-summary div {
  min-width: 0;
  padding: 12px;
  border: 1px solid var(--rk-border);
  border-radius: var(--ip-radius-sm);
  background: var(--ip-neutral-50);
}

.rk-side-summary strong {
  display: block;
  color: var(--rk-title);
  font-size: var(--ip-font-title);
  font-weight: 700;
  font-variant-numeric: tabular-nums;
  line-height: 1.35;
}

.rk-side-summary span {
  display: block;
  margin-top: 4px;
  font-size: var(--ip-font-caption);
  font-weight: 600;
  line-height: 1.5;
}

@media (max-width: 980px) {
  .rk-sidebar {
    position: static;
    max-height: none;
  }
}
</style>

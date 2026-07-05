<template>
  <aside class="tools-sidebar">
    <div class="side-brand">
      <img :src="logoUrl" alt="工具即用" />
      <div>
        <strong>工具即用</strong>
        <span>即开即用 · 提升效率</span>
      </div>
    </div>

    <button class="home-link" type="button" @click="goHome">
      <el-icon><House /></el-icon>
      <span>返回首页</span>
    </button>

    <nav class="side-nav" aria-label="工具即用导航">
      <div class="nav-title">
        <span>工具导航</span>
        <em>{{ navItems.length }} 项</em>
      </div>
      <button
        v-for="item in navItems"
        :key="item.key"
        :class="['side-link', { active: viewMode === item.key }]"
        type="button"
        @click="switchMode(item.key)"
      >
        <span class="nav-icon">
          <el-icon><component :is="item.icon" /></el-icon>
        </span>
        <strong>{{ item.label }}</strong>
      </button>
    </nav>
  </aside>
</template>

<script setup lang="ts">
import { computed } from 'vue';
import { useRouter } from 'vue-router';
import { Grid, House, Star } from '@element-plus/icons-vue';
import logoUrl from '@/assets/portal/home-logo.png';

type ViewMode = 'market' | 'favorites';

defineProps<{ viewMode: ViewMode }>();
const emit = defineEmits<{ (e: 'switch-mode', mode: ViewMode): void }>();

const router = useRouter();

const navItems = computed(() => [
  { key: 'market' as const, label: '工具即用', icon: Grid },
  { key: 'favorites' as const, label: '收藏工具', icon: Star }
]);

const switchMode = (mode: ViewMode) => emit('switch-mode', mode);

const goHome = () => {
  router.push('/portal');
};
</script>

<style scoped>
.tools-sidebar {
  position: sticky;
  top: 18px;
  align-self: start;
  max-height: calc(100vh - 36px);
  min-height: 0;
  display: grid;
  grid-template-rows: auto auto minmax(0, 1fr);
  gap: 14px;
  overflow: hidden;
}

.side-brand {
  position: relative;
  height: 86px;
  min-height: 86px;
  display: flex;
  align-items: center;
  gap: 13px;
  box-sizing: border-box;
  border: 1px solid var(--tool-border);
  border-radius: 10px;
  padding: 16px;
  overflow: hidden;
  background: linear-gradient(135deg, var(--tool-accent-soft), transparent 42%), var(--ip-neutral-0);
  box-shadow: var(--ip-shadow-sm);
}

.side-brand::after {
  content: '';
  position: absolute;
  right: -24px;
  bottom: -34px;
  width: 110px;
  height: 72px;
  border: 1px solid var(--ip-mod-appcenter-border);
  border-radius: 10px;
  transform: rotate(-14deg);
  pointer-events: none;
}

.side-brand img {
  position: relative;
  z-index: 1;
  width: 42px;
  height: 42px;
  object-fit: contain;
}

.side-brand div {
  position: relative;
  z-index: 1;
  min-width: 0;
  display: grid;
  gap: 4px;
}

.side-brand strong {
  color: var(--tool-title);
  font-size: 20px;
  line-height: 1.15;
  font-weight: 700;
}

.side-brand span {
  color: var(--tool-muted);
  font-size: 13px;
  font-weight: 700;
  white-space: nowrap;
}

.home-link {
  width: 100%;
  height: 40px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 7px;
  border: 1px solid var(--tool-border);
  border-radius: 6px;
  padding: 0 14px;
  background: var(--ip-neutral-0);
  color: var(--tool-text);
  line-height: 1;
  font-weight: 700;
  cursor: pointer;
  transition:
    border-color 0.18s ease,
    background 0.18s ease,
    color 0.18s ease,
    box-shadow 0.18s ease,
    transform 0.18s ease;
}

.home-link .el-icon {
  color: var(--tool-muted);
  font-size: 18px;
}

.home-link:hover {
  border-color: var(--tool-primary);
  background: var(--tool-primary-soft);
  color: var(--tool-primary);
  transform: translateY(-1px);
}

.home-link:hover .el-icon {
  color: var(--tool-primary);
}

.side-nav {
  min-height: 0;
  display: flex;
  flex-direction: column;
  gap: 10px;
  border: 1px solid var(--tool-border);
  border-radius: 10px;
  padding: 14px;
  overflow: auto;
  background: rgba(255, 255, 255, 0.94);
  box-shadow: var(--ip-shadow-sm);
}

.nav-title {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  padding: 2px 2px 6px;
}

.nav-title span {
  color: var(--tool-title);
  font-size: 15px;
  font-weight: 700;
}

.nav-title em {
  color: var(--tool-muted);
  font-size: 13px;
  font-style: normal;
  font-weight: 700;
}

.side-link {
  position: relative;
  min-height: 58px;
  display: grid;
  grid-template-columns: 34px minmax(0, 1fr);
  align-items: center;
  gap: 10px;
  border: 1px solid transparent;
  border-radius: 6px;
  padding: 12px;
  background: var(--ip-neutral-50);
  color: var(--tool-text);
  font-size: 15px;
  font-weight: 700;
  text-align: left;
  cursor: pointer;
  transition:
    border-color 0.18s ease,
    background 0.18s ease,
    box-shadow 0.18s ease,
    transform 0.18s ease;
}

.nav-icon {
  width: 34px;
  height: 34px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 6px;
  background: var(--ip-neutral-0);
  color: var(--tool-accent);
  box-shadow: 0 0 0 1px var(--tool-input-border) inset;
}

.side-link strong {
  min-width: 0;
  overflow: hidden;
  color: var(--tool-title);
  line-height: 1.2;
  font-weight: 700;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.side-link:hover {
  border-color: var(--ip-neutral-300);
  background: var(--ip-neutral-100);
  transform: translateX(2px);
}

.side-link.active {
  border-color: var(--ip-mod-appcenter-border);
  background: var(--tool-accent-soft);
  box-shadow: inset 3px 0 0 var(--tool-accent);
}

.side-link.active .nav-icon {
  background: var(--tool-accent);
  color: var(--ip-neutral-0);
  box-shadow: var(--ip-shadow-md);
}

@media (max-width: 1120px) {
  .side-brand {
    height: 86px;
    min-height: 86px;
  }

  .side-brand strong {
    font-size: 22px;
  }

  .home-link {
    padding-inline: 16px;
  }

  .side-nav {
    padding: 14px;
  }

  .side-link {
    min-height: 58px;
  }
}

@media (max-width: 980px) {
  .tools-sidebar {
    position: static;
    max-height: none;
  }

  .side-brand {
    height: 86px;
    min-height: 86px;
  }

  .side-nav {
    display: grid;
    grid-template-columns: 1fr;
    gap: 10px;
    max-height: none;
  }

  .home-link {
    width: 100%;
    justify-content: center;
  }
}
</style>

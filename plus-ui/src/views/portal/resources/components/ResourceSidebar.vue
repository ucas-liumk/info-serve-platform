<template>
  <aside class="resource-sidebar" @keydown.esc="closeCompactFilters">
    <div class="side-brand">
      <img src="@/assets/portal/module-resource.png" alt="资料共享" />
      <div><strong>资料共享</strong><span>知识汇聚 · 共享价值</span></div>
    </div>

    <button class="home-button" type="button" @click="goPortalHome">
      <el-icon><House /></el-icon><span>返回首页</span>
    </button>

    <button
      v-if="isCompact"
      ref="disclosureRef"
      class="filter-disclosure"
      type="button"
      aria-controls="resource-category-filters"
      :aria-expanded="filtersExpanded"
      @click="toggleCompactFilters"
    >
      <span>资料栏目与筛选</span>
      <el-icon :class="{ expanded: filtersExpanded }"><ArrowDown /></el-icon>
    </button>

    <div id="resource-category-filters" ref="filterRegionRef" class="filter-region" :hidden="isCompact && !filtersExpanded">
      <div v-if="loadError" class="category-error" role="alert">
        <span>{{ loadError }}</span
        ><button type="button" @click="emit('retry')">重试</button>
      </div>
      <ResourceFilterPanel :tree="categoryTree" :selected="selectedCategories" @update:selected="onUpdateSelected" />
    </div>
  </aside>
</template>

<script setup lang="ts">
import { nextTick, onBeforeUnmount, onMounted, ref } from 'vue';
import { ArrowDown, House } from '@element-plus/icons-vue';
import { useRouter } from 'vue-router';
import type { CategoryTreeNode } from '@/api/infoservice/types';
import { PORTAL_HOME_PATH } from '@/constants/router';
import ResourceFilterPanel from './ResourceFilterPanel.vue';

defineProps<{ categoryTree: CategoryTreeNode[]; selectedCategories: string[]; loadError: string }>();
const emit = defineEmits<{
  (e: 'update:selected-categories', value: string[]): void;
  (e: 'retry'): void;
}>();

const router = useRouter();
const isCompact = ref(false);
const filtersExpanded = ref(true);
const disclosureRef = ref<HTMLButtonElement>();
const filterRegionRef = ref<HTMLElement>();
let mediaQuery: MediaQueryList | undefined;

const syncViewport = (event?: MediaQueryListEvent) => {
  const compact = event?.matches ?? mediaQuery?.matches ?? false;
  isCompact.value = compact;
  filtersExpanded.value = !compact;
};

const goPortalHome = () => router.push(PORTAL_HOME_PATH);
const onUpdateSelected = (value: string[]) => emit('update:selected-categories', value);

const toggleCompactFilters = async () => {
  filtersExpanded.value = !filtersExpanded.value;
  if (filtersExpanded.value) {
    await nextTick();
    filterRegionRef.value?.querySelector<HTMLElement>('button, [tabindex="0"]')?.focus();
  }
};

const closeCompactFilters = () => {
  if (!isCompact.value || !filtersExpanded.value) return;
  filtersExpanded.value = false;
  disclosureRef.value?.focus();
};

onMounted(() => {
  mediaQuery = window.matchMedia('(max-width: 980px)');
  syncViewport();
  mediaQuery.addEventListener('change', syncViewport);
});

onBeforeUnmount(() => mediaQuery?.removeEventListener('change', syncViewport));
</script>

<style scoped>
.resource-sidebar {
  position: sticky;
  top: 24px;
  align-self: start;
  max-height: calc(100vh - 48px);
  display: grid;
  grid-template-rows: auto auto minmax(0, 1fr);
  gap: 12px;
}
.side-brand {
  min-height: 80px;
  display: flex;
  align-items: center;
  gap: 12px;
  border: 1px solid var(--ip-neutral-200);
  border-radius: var(--ip-radius-md);
  padding: 16px;
  background: var(--ip-neutral-0);
  box-shadow: var(--ip-shadow-sm);
}
.side-brand img {
  width: 40px;
  height: 40px;
  object-fit: contain;
}
.side-brand div {
  min-width: 0;
  display: grid;
  gap: 4px;
}
.side-brand strong {
  color: var(--ip-neutral-900);
  font-size: var(--ip-font-title-sm);
  line-height: 1.2;
  font-weight: 700;
}
.side-brand span {
  color: var(--ip-neutral-500);
  font-size: var(--ip-font-caption);
  font-weight: 500;
  white-space: nowrap;
}
.home-button,
.filter-disclosure {
  width: 100%;
  min-height: 36px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  border: 1px solid var(--ip-neutral-200);
  border-radius: var(--ip-radius-sm);
  padding: 0 12px;
  background: var(--ip-neutral-0);
  color: var(--ip-neutral-700);
  font-size: var(--ip-font-body);
  font-weight: 600;
  cursor: pointer;
}
.home-button:hover,
.filter-disclosure:hover {
  border-color: var(--ip-primary-300);
  background: var(--ip-primary-50);
  color: var(--ip-primary-700);
}
.filter-region {
  min-height: 0;
  overflow: auto;
}
.filter-region[hidden] {
  display: none;
}
.filter-disclosure {
  display: none;
  justify-content: space-between;
}
.filter-disclosure .el-icon {
  transition: transform var(--ip-motion-fast) var(--ip-motion-ease);
}
.filter-disclosure .el-icon.expanded {
  transform: rotate(180deg);
}
.category-error {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  margin-bottom: 8px;
  border: 1px solid var(--ip-warning-border);
  border-radius: var(--ip-radius-sm);
  padding: 8px 12px;
  background: var(--ip-warning-soft);
  color: var(--ip-neutral-700);
  font-size: var(--ip-font-hint);
}
.category-error button {
  min-height: 32px;
  border: 1px solid var(--ip-warning-border);
  border-radius: var(--ip-radius-sm);
  padding: 0 12px;
  background: var(--ip-neutral-0);
  color: var(--ip-neutral-700);
  font-weight: 600;
  cursor: pointer;
}
button:focus-visible {
  outline: none;
  box-shadow: var(--ip-focus-ring);
}

@media (max-width: 980px) {
  .resource-sidebar {
    position: static;
    max-height: none;
    grid-template-columns: minmax(0, 1fr) auto;
    grid-template-rows: auto auto auto;
  }
  .side-brand {
    grid-column: 1;
  }
  .home-button {
    grid-column: 2;
    align-self: stretch;
    width: auto;
    min-width: 112px;
  }
  .filter-disclosure {
    grid-column: 1 / -1;
    display: inline-flex;
    min-height: 44px;
  }
  .filter-region {
    grid-column: 1 / -1;
    max-height: none;
    overflow: visible;
  }
}

@media (max-width: 480px) {
  .resource-sidebar {
    grid-template-columns: 1fr;
  }
  .side-brand,
  .home-button {
    grid-column: 1;
    width: 100%;
  }
  .home-button {
    min-height: 44px;
  }
}
</style>

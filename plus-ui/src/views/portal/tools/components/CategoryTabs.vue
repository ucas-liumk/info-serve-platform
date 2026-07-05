<template>
  <section class="cat-tabs">
    <div class="tab-scroll">
      <button :class="['tab', { on: model === 'all' }]" type="button" @click="select('all')">
        <span>全部工具</span>
        <b>{{ totalCount }}</b>
      </button>
      <button
        v-for="category in categories"
        :key="category.categoryCode"
        :class="['tab', { on: model === category.categoryCode }]"
        type="button"
        @click="select(category.categoryCode)"
      >
        <span>{{ category.categoryName }}</span>
        <b v-if="categoryCount(category) !== null">{{ categoryCount(category) }}</b>
      </button>
    </div>

    <el-select v-model="sortModel" class="sort" size="large" teleported>
      <el-option label="最新上架" value="latest" />
      <el-option label="最多收藏" value="hot" />
      <el-option label="最多使用" value="use" />
    </el-select>
  </section>
</template>

<script setup lang="ts">
import { computed } from 'vue';
import { PortalCategory } from '@/api/appcenter/types';

const props = defineProps<{ categories: PortalCategory[]; model: string; sort: string; total?: number }>();
const emit = defineEmits<{ (e: 'update:model', value: string): void; (e: 'update:sort', value: string): void }>();

const categoryCount = (category: PortalCategory) => {
  const count = category.appCount;
  return count === undefined || count === null ? null : Number(count);
};

const totalCount = computed(() => props.total || props.categories.reduce((sum, item) => sum + Number(item.appCount || 0), 0));
const sortModel = computed({
  get: () => props.sort,
  set: (value: string) => emit('update:sort', value)
});

const select = (code: string) => emit('update:model', code);
</script>

<style scoped>
.cat-tabs {
  min-width: 0;
  min-height: 72px;
  box-sizing: border-box;
  display: flex;
  align-items: center;
  gap: 18px;
  padding: 0 18px;
  border: 1px solid var(--ip-neutral-200);
  border-radius: 10px;
  background: rgba(255, 255, 255, 0.92);
  box-shadow: var(--ip-shadow-sm);
}

.tab-scroll {
  min-width: 0;
  display: flex;
  align-items: center;
  gap: 18px;
  overflow-x: auto;
  scrollbar-width: none;
}

.tab-scroll::-webkit-scrollbar {
  display: none;
}

.tab {
  height: 42px;
  display: inline-flex;
  align-items: center;
  gap: 10px;
  flex: 0 0 auto;
  border: 0;
  border-radius: 6px;
  padding: 0 14px;
  background: transparent;
  color: var(--ip-neutral-800);
  font-size: 14px;
  line-height: 1;
  font-weight: 700;
  white-space: nowrap;
  cursor: pointer;
  transition:
    background 0.16s ease,
    color 0.16s ease;
}

.tab b {
  color: var(--ip-neutral-600);
  font-size: 14px;
  font-weight: 700;
}

.tab.on {
  background: var(--ip-mod-appcenter-soft);
  color: var(--ip-mod-appcenter);
}

.tab.on b {
  color: var(--ip-mod-appcenter);
}

.tab:hover:not(.on) {
  background: var(--ip-primary-50);
  color: var(--ip-primary-600);
}

.sort {
  width: 132px;
  flex: 0 0 132px;
  margin-left: auto;
}

:deep(.sort .el-select__wrapper) {
  min-height: 42px;
  border-radius: 6px;
  background: var(--ip-neutral-0);
  box-shadow: 0 0 0 1px var(--ip-neutral-200) inset;
  color: var(--ip-neutral-800);
  font-weight: 700;
}

@media (max-width: 980px) {
  .cat-tabs {
    align-items: stretch;
    flex-direction: column;
    padding: 14px;
  }

  .sort {
    width: 100%;
    flex-basis: auto;
  }
}
</style>

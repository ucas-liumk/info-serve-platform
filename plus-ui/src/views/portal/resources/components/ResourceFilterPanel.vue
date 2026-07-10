<template>
  <aside class="filter-panel">
    <div class="filter-title">
      <span>资源栏目</span>
      <button v-if="tree.length > 0" class="fold-toggle" type="button" @click="toggleAllGroups">
        {{ allCollapsed ? '全部展开' : '全部收起' }}
      </button>
    </div>

    <p v-if="tree.length === 0" class="filter-empty">暂无栏目分类</p>

    <div v-for="group in tree" :key="group.categoryId" class="filter-group">
      <div
        class="group-header"
        role="button"
        tabindex="0"
        :aria-expanded="!isCollapsed(group.categoryCode)"
        @click="toggleCollapse(group.categoryCode)"
        @keydown.enter.prevent="toggleCollapse(group.categoryCode)"
        @keydown.space.prevent="toggleCollapse(group.categoryCode)"
      >
        <span :class="['chevron', { folded: isCollapsed(group.categoryCode) }]" aria-hidden="true">
          <el-icon><CaretBottom /></el-icon>
        </span>
        <button
          :class="['checkbox', groupCheckboxClass(group)]"
          type="button"
          role="checkbox"
          :aria-checked="groupAriaChecked(group)"
          :aria-label="`选择栏目「${group.categoryName}」全部分类`"
          @click.stop="onToggleGroup(group)"
          @keydown.stop
        ></button>
        <span class="group-name">{{ group.categoryName }}</span>
        <em class="count">{{ groupResourceCount(group) }}</em>
      </div>

      <div v-show="!isCollapsed(group.categoryCode)" class="group-body">
        <button
          v-for="cat in group.children ?? []"
          :key="cat.categoryId"
          :class="['category-row', { active: isSelected(cat.categoryCode) }]"
          type="button"
          role="checkbox"
          :aria-checked="isSelected(cat.categoryCode)"
          @click="onToggleCategory(cat.categoryCode)"
        >
          <span :class="['checkbox', { 'is-checked': isSelected(cat.categoryCode) }]" aria-hidden="true"></span>
          <span class="category-name">{{ cat.categoryName }}</span>
          <em class="count">{{ cat.resourceCount ?? 0 }}</em>
        </button>
      </div>
    </div>

    <div v-if="tree.length > 0" class="filter-clear">
      <button type="button" :disabled="selected.length === 0" @click="clearSelection">清除筛选</button>
    </div>
  </aside>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue';
import { CaretBottom } from '@element-plus/icons-vue';
import type { CategoryTreeNode } from '@/api/infoservice/types';
import { getGroupCheckState, groupResourceCount, toggleCategory, toggleGroup } from '../categoryFacets';

const props = defineProps<{
  tree: CategoryTreeNode[];
  selected: string[];
}>();

const emit = defineEmits<{
  (e: 'update:selected', value: string[]): void;
}>();

/** 折叠栏目 code 集合（不可变：每次替换新 Set） */
const collapsedCodes = ref<ReadonlySet<string>>(new Set());

const isCollapsed = (code: string) => collapsedCodes.value.has(code);

const toggleCollapse = (code: string) => {
  const next = new Set(collapsedCodes.value);
  if (next.has(code)) {
    next.delete(code);
  } else {
    next.add(code);
  }
  collapsedCodes.value = next;
};

const allCollapsed = computed(() => props.tree.length > 0 && props.tree.every((group) => collapsedCodes.value.has(group.categoryCode)));

const toggleAllGroups = () => {
  collapsedCodes.value = allCollapsed.value ? new Set() : new Set(props.tree.map((group) => group.categoryCode));
};

const isSelected = (code: string) => props.selected.includes(code);

const groupCheckboxClass = (group: CategoryTreeNode) => {
  const state = getGroupCheckState(group, props.selected);
  return { 'is-checked': state === 'all', 'is-partial': state === 'partial' };
};

const groupAriaChecked = (group: CategoryTreeNode): 'true' | 'false' | 'mixed' => {
  const state = getGroupCheckState(group, props.selected);
  if (state === 'all') return 'true';
  return state === 'partial' ? 'mixed' : 'false';
};

const onToggleCategory = (code: string) => emit('update:selected', toggleCategory(props.selected, code));

const onToggleGroup = (group: CategoryTreeNode) => emit('update:selected', toggleGroup(props.selected, group));

const clearSelection = () => emit('update:selected', []);
</script>

<style scoped>
.filter-panel {
  border: 1px solid var(--ip-neutral-200);
  border-radius: var(--ip-radius-md);
  background: var(--ip-neutral-0);
  box-shadow: var(--ip-shadow-md);
}

.filter-title {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 14px 16px 12px;
  border-bottom: 1px solid var(--ip-neutral-100);
  color: var(--ip-neutral-900);
  font-size: var(--ip-font-emphasis);
  font-weight: 800;
}

.fold-toggle {
  border: 0;
  padding: 0;
  background: transparent;
  color: var(--ip-neutral-500);
  font-size: var(--ip-font-caption);
  font-weight: 600;
  cursor: pointer;
  transition: color var(--ip-motion-fast) var(--ip-motion-ease);
}

.fold-toggle:hover {
  color: var(--ip-primary-600);
}

.filter-empty {
  margin: 0;
  padding: 18px 16px;
  color: var(--ip-neutral-400);
  font-size: var(--ip-font-hint);
  text-align: center;
}

.filter-group + .filter-group {
  border-top: 1px solid var(--ip-neutral-100);
}

.group-header {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 9px 16px;
  color: var(--ip-neutral-800);
  font-size: var(--ip-font-body);
  font-weight: 700;
  cursor: pointer;
  transition: background var(--ip-motion-fast) var(--ip-motion-ease);
}

.group-header:hover {
  background: var(--ip-neutral-50);
}

.group-name,
.category-name {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.chevron {
  display: inline-flex;
  align-items: center;
  color: var(--ip-neutral-400);
  font-size: var(--ip-font-caption);
  transition: transform var(--ip-motion-fast) var(--ip-motion-ease);
}

.chevron.folded {
  transform: rotate(-90deg);
}

.count {
  margin-left: auto;
  color: var(--ip-neutral-400);
  font-size: var(--ip-font-caption);
  font-style: normal;
  font-weight: 500;
}

.checkbox {
  position: relative;
  width: 14px;
  height: 14px;
  flex: 0 0 14px;
  box-sizing: border-box;
  border: 1px solid var(--ip-neutral-300);
  padding: 0;
  background: var(--ip-neutral-0);
  cursor: pointer;
  transition:
    background var(--ip-motion-fast) var(--ip-motion-ease),
    border-color var(--ip-motion-fast) var(--ip-motion-ease);
}

.checkbox.is-checked,
.checkbox.is-partial {
  border-color: var(--ip-primary-600);
  background: var(--ip-primary-600);
}

.checkbox.is-checked::after {
  content: '';
  position: absolute;
  left: 4px;
  top: 1px;
  width: 4px;
  height: 8px;
  border: solid var(--ip-neutral-0);
  border-width: 0 2px 2px 0;
  transform: rotate(45deg);
}

.checkbox.is-partial::after {
  content: '';
  position: absolute;
  left: 2px;
  right: 2px;
  top: 5px;
  height: 2px;
  background: var(--ip-neutral-0);
}

.group-body {
  padding: 0 10px 8px 30px;
}

.category-row {
  width: 100%;
  display: flex;
  align-items: center;
  gap: 8px;
  border: 0;
  border-radius: var(--ip-radius-sm);
  padding: 6px 8px;
  background: transparent;
  color: var(--ip-neutral-700);
  font-size: var(--ip-font-body);
  font-weight: 600;
  text-align: left;
  cursor: pointer;
  transition:
    background var(--ip-motion-fast) var(--ip-motion-ease),
    color var(--ip-motion-fast) var(--ip-motion-ease);
}

.category-row:hover {
  background: var(--ip-neutral-50);
  color: var(--ip-primary-600);
}

.category-row.active {
  background: var(--ip-primary-50);
  color: var(--ip-primary-600);
  font-weight: 700;
}

.category-row.active .count {
  color: var(--ip-primary-600);
}

.filter-clear {
  padding: 10px 16px 14px;
  border-top: 1px solid var(--ip-neutral-100);
}

.filter-clear button {
  width: 100%;
  border: 1px solid var(--ip-neutral-200);
  border-radius: var(--ip-radius-sm);
  padding: 7px 0;
  background: var(--ip-neutral-0);
  color: var(--ip-neutral-500);
  font-size: var(--ip-font-hint);
  font-weight: 600;
  cursor: pointer;
  transition:
    border-color var(--ip-motion-fast) var(--ip-motion-ease),
    background var(--ip-motion-fast) var(--ip-motion-ease),
    color var(--ip-motion-fast) var(--ip-motion-ease);
}

.filter-clear button:hover:not(:disabled) {
  border-color: var(--ip-primary-600);
  background: var(--ip-primary-50);
  color: var(--ip-primary-600);
}

.filter-clear button:disabled {
  cursor: not-allowed;
  opacity: 0.55;
}
</style>

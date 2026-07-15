<template>
  <aside class="filter-panel">
    <div class="filter-title">
      <span>资料栏目</span>
      <button v-if="tree.length > 0" class="fold-toggle" type="button" @click="toggleAllGroups">
        {{ allCollapsed ? '全部展开' : '全部收起' }}
      </button>
    </div>

    <p v-if="tree.length === 0" class="filter-empty">暂无栏目分类</p>

    <div v-for="group in tree" :key="group.categoryId" class="filter-group">
      <div class="group-header">
        <button
          :class="['checkbox', groupCheckboxClass(group)]"
          type="button"
          role="checkbox"
          :aria-checked="groupAriaChecked(group)"
          :aria-label="`选择栏目「${group.categoryName}」全部分类`"
          @click.stop="onToggleGroup(group)"
          @keydown.stop
        ></button>
        <button class="group-collapse" type="button" :aria-expanded="!isCollapsed(group.categoryCode)" @click="toggleCollapse(group.categoryCode)">
          <span :class="['chevron', { folded: isCollapsed(group.categoryCode) }]" aria-hidden="true">
            <el-icon><CaretBottom /></el-icon>
          </span>
          <span class="group-name">{{ group.categoryName }}</span>
          <em class="count">{{ groupResourceCount(group) }}</em>
        </button>
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
  box-shadow: var(--ip-shadow-sm);
}

.filter-title {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 12px 16px;
  border-bottom: 1px solid var(--ip-neutral-100);
  color: var(--ip-neutral-900);
  font-size: var(--ip-font-emphasis);
  font-weight: 700;
}

.filter-title > span,
.fold-toggle {
  white-space: nowrap;
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
  padding: 16px;
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
  padding: 8px 16px;
  background: var(--ip-neutral-50);
  border-left: 3px solid var(--ip-primary-500);
  color: var(--ip-neutral-900);
  font-size: var(--ip-font-body);
  font-weight: 700;
  transition: background var(--ip-motion-fast) var(--ip-motion-ease);
}

.group-header:hover {
  background: var(--ip-neutral-100);
}

.group-collapse .count {
  font-weight: 700;
  color: var(--ip-neutral-500);
}

.group-collapse {
  min-width: 0;
  flex: 1;
  display: flex;
  align-items: center;
  gap: 8px;
  border: 0;
  padding: 0;
  background: transparent;
  color: inherit;
  font: inherit;
  cursor: pointer;
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
  width: 16px;
  height: 16px;
  flex: 0 0 16px;
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
  left: 5px;
  top: 2px;
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
  margin: 4px 8px 8px 24px;
  padding-left: 8px;
  border-left: 2px solid var(--ip-neutral-100);
}

.category-row {
  width: 100%;
  display: flex;
  align-items: center;
  gap: 8px;
  border: 0;
  border-radius: var(--ip-radius-sm);
  padding: 8px;
  background: transparent;
  color: var(--ip-neutral-600);
  font-size: var(--ip-font-body);
  font-weight: 500;
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
  padding: 8px 16px 16px;
  border-top: 1px solid var(--ip-neutral-100);
}

.filter-clear button {
  width: 100%;
  border: 1px solid var(--ip-neutral-200);
  border-radius: var(--ip-radius-sm);
  min-height: 36px;
  padding: 0 8px;
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

button:focus-visible,
.group-collapse:focus-visible {
  outline: none;
  box-shadow: var(--ip-focus-ring);
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

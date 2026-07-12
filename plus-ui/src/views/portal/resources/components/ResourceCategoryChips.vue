<template>
  <div v-if="chips.length > 0" class="category-chipbar">
    <span v-for="chip in chips" :key="chip.code" class="category-chip">
      <span class="chip-name">{{ chip.name }}</span>
      <button type="button" class="chip-remove" :aria-label="`移除分类「${chip.name}」`" @click="emit('remove', chip.code)">
        <el-icon><Close /></el-icon>
      </button>
    </span>
  </div>
</template>

<script setup lang="ts">
import { Close } from '@element-plus/icons-vue';
import type { CategoryChip } from '../categoryFacets';

defineProps<{ chips: CategoryChip[] }>();

const emit = defineEmits<{
  (e: 'remove', code: string): void;
}>();
</script>

<style scoped>
.category-chipbar {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  padding: 12px 18px 0;
}

.category-chip {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  border: 1px solid var(--ip-primary-200);
  border-radius: var(--ip-radius-full);
  padding: 3px 6px 3px 12px;
  background: var(--ip-primary-50);
  color: var(--ip-primary-600);
  font-size: var(--ip-font-caption);
  font-weight: 600;
  line-height: 1.4;
}

.chip-name {
  max-width: 160px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.chip-remove {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 16px;
  height: 16px;
  border: 0;
  border-radius: var(--ip-radius-full);
  padding: 0;
  background: transparent;
  color: var(--ip-primary-600);
  font-size: var(--ip-font-caption);
  cursor: pointer;
  transition:
    background var(--ip-motion-fast) var(--ip-motion-ease),
    color var(--ip-motion-fast) var(--ip-motion-ease);
}

.chip-remove:hover {
  background: var(--ip-primary-600);
  color: var(--ip-neutral-0);
}
</style>

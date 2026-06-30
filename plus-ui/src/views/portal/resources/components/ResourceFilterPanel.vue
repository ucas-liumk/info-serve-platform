<template>
  <aside class="filter-panel">
    <div class="filter-title">
      <span>资源分类</span>
      <button type="button" @click="emit('change-category', 'all')">全部分类</button>
    </div>
    <button :class="['filter-row', { active: categoryCode === 'all' }]" type="button" @click="emit('change-category', 'all')">
      <span>
        <el-icon><FolderOpened /></el-icon>
        全部资源
      </span>
      <em>{{ total }}</em>
    </button>
    <button
      v-for="cat in categories"
      :key="cat.categoryId"
      :class="['filter-row', { active: categoryCode === cat.categoryCode }]"
      type="button"
      @click="emit('change-category', cat.categoryCode)"
    >
      <span>
        <el-icon><Collection /></el-icon>
        {{ cat.categoryName }}
      </span>
      <em>{{ cat.resourceCount || 0 }}</em>
    </button>
  </aside>
</template>

<script setup lang="ts">
import { Collection, FolderOpened } from '@element-plus/icons-vue';
import type { ResourceCategory } from '@/api/infoservice/types';

defineProps<{
  categories: ResourceCategory[];
  total: number;
  categoryCode: string;
}>();

const emit = defineEmits<{
  (e: 'change-category', code: string): void;
}>();
</script>

<style scoped>
.filter-panel {
  display: grid;
  gap: 8px;
  border: 1px solid #e1e9f6;
  border-radius: 8px;
  padding: 18px;
  background: #fff;
  box-shadow: 0 14px 34px rgba(35, 83, 151, 0.07);
}

.filter-title {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 12px;
  color: #0b1833;
  font-size: 17px;
  line-height: 1.2;
  font-weight: 850;
}

.filter-title button {
  border: 0;
  padding: 0;
  background: transparent;
  color: #1260e8;
  font-size: 12px;
  font-weight: 800;
  cursor: pointer;
}

.filter-row {
  width: 100%;
  min-height: 38px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  border: 0;
  border-radius: 6px;
  padding: 7px 8px;
  background: transparent;
  color: #25395f;
  font-size: 15px;
  font-weight: 750;
  text-align: left;
  cursor: pointer;
}

.filter-row + .filter-row {
  margin-top: 4px;
}

.filter-row span {
  min-width: 0;
  display: inline-flex;
  align-items: center;
  gap: 10px;
}

.filter-row .el-icon {
  width: 20px;
  height: 20px;
  flex: 0 0 20px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 4px;
  background: #1260e8;
  color: #fff;
  font-size: 12px;
}

.filter-row em {
  color: #53668f;
  font-size: 13px;
  font-style: normal;
  font-weight: 650;
}

.filter-row:hover,
.filter-row.active {
  background: #edf4ff;
  color: #1260e8;
}

.filter-row.active em {
  color: #1260e8;
}
</style>

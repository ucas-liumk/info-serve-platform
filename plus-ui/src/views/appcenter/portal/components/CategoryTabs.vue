<template>
  <div class="cat-tabs">
    <span :class="['tab', { on: model === 'all' }]" @click="select('all')">全部</span>
    <span
      v-for="c in categories"
      :key="c.categoryCode"
      :class="['tab', { on: model === c.categoryCode }]"
      @click="select(c.categoryCode)"
    >
      {{ c.categoryName }}
    </span>
    <el-select v-model="sortModel" class="sort" size="small" @change="$emit('update:sort', sortModel)">
      <el-option label="最近上架" value="latest" />
      <el-option label="最多推荐" value="hot" />
      <el-option label="最多使用" value="use" />
    </el-select>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue';
import { PortalCategory } from '@/api/appcenter/types';

defineProps<{ categories: PortalCategory[]; model: string }>();
const emit = defineEmits<{ (e: 'update:model', v: string): void; (e: 'update:sort', v: string): void }>();

const sortModel = ref('latest');

const select = (code: string) => emit('update:model', code);
</script>

<style scoped>
.cat-tabs {
  display: flex;
  align-items: center;
  gap: 18px;
  padding: 14px 0;
}
.tab {
  cursor: pointer;
  color: #5b6577;
}
.tab.on {
  color: #1f6feb;
  font-weight: 600;
}
.sort {
  margin-left: auto;
  width: 130px;
}
</style>

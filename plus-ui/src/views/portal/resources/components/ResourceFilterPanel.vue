<template>
  <aside class="filter-panel">
    <section class="filter-block">
      <div class="filter-title">
        <span>资源分类</span>
        <strong>{{ total }}</strong>
      </div>
      <button :class="['filter-row', { active: categoryCode === 'all' }]" type="button" @click="emit('change-category', 'all')">
        <span>全部资料</span>
        <em>{{ total }}</em>
      </button>
      <button
        v-for="cat in categories"
        :key="cat.categoryId"
        :class="['filter-row', { active: categoryCode === cat.categoryCode }]"
        type="button"
        @click="emit('change-category', cat.categoryCode)"
      >
        <span>{{ cat.categoryName }}</span>
        <em>{{ cat.resourceCount || 0 }}</em>
      </button>
    </section>

    <section class="filter-block">
      <div class="filter-title">
        <span>资源类型</span>
      </div>
      <button
        v-for="item in typeOptions"
        :key="item.value"
        :class="['filter-row icon-row', { active: previewType === item.value }]"
        type="button"
        @click="emit('update:previewType', item.value)"
      >
        <el-icon><component :is="item.icon" /></el-icon>
        <span>{{ item.label }}</span>
      </button>
    </section>

    <section class="filter-block">
      <div class="filter-title">
        <span>上传时间</span>
      </div>
      <button
        v-for="item in timeOptions"
        :key="item.value"
        :class="['filter-row', { active: uploadedWithin === item.value }]"
        type="button"
        @click="emit('update:uploadedWithin', item.value)"
      >
        <span>{{ item.label }}</span>
      </button>
    </section>
  </aside>
</template>

<script setup lang="ts">
import { Document, Files, Headset, Picture, VideoCamera } from '@element-plus/icons-vue';
import type { ResourceCategory } from '@/api/infoservice/types';

defineProps<{
  categories: ResourceCategory[];
  total: number;
  categoryCode: string;
  previewType: string;
  uploadedWithin: string;
}>();

const emit = defineEmits<{
  (e: 'change-category', code: string): void;
  (e: 'update:previewType', value: string): void;
  (e: 'update:uploadedWithin', value: string): void;
}>();

const typeOptions = [
  { label: '全部类型', value: 'all', icon: Files },
  { label: '文档资料', value: 'file', icon: Document },
  { label: 'PDF 文件', value: 'pdf', icon: Document },
  { label: '图片素材', value: 'image', icon: Picture },
  { label: '视频资料', value: 'video', icon: VideoCamera },
  { label: '音频资料', value: 'audio', icon: Headset }
];

const timeOptions = [
  { label: '不限时间', value: 'all' },
  { label: '最近一周', value: 'week' },
  { label: '最近一月', value: 'month' },
  { label: '最近三月', value: 'quarter' },
  { label: '最近一年', value: 'year' }
];
</script>

<style scoped>
.filter-panel {
  display: grid;
  gap: 16px;
}

.filter-block {
  border: 1px solid #e1e9f6;
  border-radius: 8px;
  padding: 14px;
  background: #fff;
  box-shadow: 0 8px 24px rgba(11, 24, 51, 0.05);
}

.filter-title {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 10px;
  color: #0b1833;
  font-size: 14px;
  font-weight: 800;
}

.filter-title strong {
  min-width: 28px;
  height: 22px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 6px;
  background: #edf4ff;
  color: #1260e8;
  font-size: 12px;
  font-weight: 700;
}

.filter-row {
  width: 100%;
  min-height: 36px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  border: 0;
  border-radius: 7px;
  padding: 0 10px;
  background: transparent;
  color: #25395f;
  font-size: 13px;
  font-weight: 650;
  text-align: left;
  cursor: pointer;
}

.filter-row + .filter-row {
  margin-top: 4px;
}

.filter-row em {
  color: #8a97af;
  font-style: normal;
  font-size: 12px;
}

.filter-row:hover {
  background: #f7faff;
  color: #1260e8;
}

.filter-row.active {
  background: #edf4ff;
  color: #1260e8;
  font-weight: 750;
}

.filter-row.active em {
  color: #1260e8;
}

.icon-row {
  justify-content: flex-start;
}

.icon-row .el-icon {
  color: #1260e8;
}
</style>

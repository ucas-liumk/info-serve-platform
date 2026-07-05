<template>
  <el-dialog v-model="visible" title="全部服务" width="960px" align-center append-to-body class="all-module-dialog">
    <div class="all-module-head">
      <strong>全部服务模块</strong>
      <span>拖拽调整当前用户的首页服务顺序，前 6 项进入首屏。</span>
    </div>

    <div class="all-module-grid" :aria-busy="saving">
      <button
        v-for="(item, index) in localModules"
        :key="moduleKey(item)"
        class="all-module-card"
        :class="{
          'is-featured': index < homeLimit,
          'is-dragging': dragIndex === index,
          'is-over': overIndex === index
        }"
        type="button"
        draggable="true"
        @click="openModule(item)"
        @dragstart="startDrag($event, index)"
        @dragenter.prevent="overIndex = index"
        @dragover.prevent
        @dragleave="clearOver(index)"
        @drop.prevent="dropModule(index)"
        @dragend="resetDrag"
      >
        <span class="drag-handle" aria-hidden="true">
          <IconDragIndicator />
        </span>
        <span class="module-rank">{{ index + 1 }}</span>
        <span class="module-scope">{{ index < homeLimit ? '首页' : '更多' }}</span>
        <span class="all-module-icon">
          <img :src="item.image" :alt="item.title" />
        </span>
        <span class="all-module-copy">
          <strong>{{ item.title }}</strong>
          <em>{{ item.desc || '暂无模块说明' }}</em>
        </span>
        <span class="all-module-action" aria-hidden="true">
          <IconArrowRight />
        </span>
      </button>
    </div>

    <template #footer>
      <span class="save-state">{{ saving ? '正在保存排序' : '拖拽后自动保存当前用户配置' }}</span>
      <el-button @click="visible = false">关闭</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue';
import IconArrowRight from '~icons/material-symbols/arrow-right-alt-rounded';
import IconDragIndicator from '~icons/material-symbols/drag-indicator';
import './AllModuleDialog.css';

interface HomeModule {
  code?: string;
  title: string;
  desc: string;
  image: string;
  path?: string;
  sortOrder?: number;
}

const props = withDefaults(defineProps<{ modelValue: boolean; modules: HomeModule[]; homeLimit?: number; saving?: boolean }>(), {
  homeLimit: 6,
  saving: false
});
const emit = defineEmits<{
  (e: 'update:modelValue', value: boolean): void;
  (e: 'open', item: HomeModule): void;
  (e: 'reorder', modules: HomeModule[]): void;
}>();

const localModules = ref<HomeModule[]>([]);
const dragIndex = ref<number | null>(null);
const overIndex = ref<number | null>(null);

const visible = computed({
  get: () => props.modelValue,
  set: (value: boolean) => emit('update:modelValue', value)
});

const homeLimit = computed(() => props.homeLimit);
const saving = computed(() => props.saving);
const moduleKey = (item: HomeModule) => item.code || item.title;

const syncModules = () => {
  localModules.value = [...props.modules];
};

watch(
  () => props.modules,
  () => syncModules(),
  { immediate: true }
);

watch(
  () => props.modelValue,
  (value) => {
    if (value) {
      syncModules();
    }
  }
);

const startDrag = (event: DragEvent, index: number) => {
  dragIndex.value = index;
  overIndex.value = index;
  event.dataTransfer?.setData('text/plain', String(index));
  if (event.dataTransfer) {
    event.dataTransfer.effectAllowed = 'move';
  }
};

const clearOver = (index: number) => {
  if (overIndex.value === index) {
    overIndex.value = null;
  }
};

const resetDrag = () => {
  dragIndex.value = null;
  overIndex.value = null;
};

const dropModule = (index: number) => {
  const fromIndex = dragIndex.value;
  if (fromIndex === null || fromIndex === index) {
    resetDrag();
    return;
  }

  const nextModules = [...localModules.value];
  const [movedModule] = nextModules.splice(fromIndex, 1);
  nextModules.splice(index, 0, movedModule);
  localModules.value = nextModules;
  emit('reorder', nextModules);
  resetDrag();
};

const openModule = (item: HomeModule) => {
  emit('update:modelValue', false);
  emit('open', item);
};
</script>

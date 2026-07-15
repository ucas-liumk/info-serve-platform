<template>
  <div :class="['resource-skeleton', mode]" aria-label="正在加载资料" role="status">
    <template v-if="mode === 'grid'">
      <div v-for="index in 6" :key="index" class="grid-skeleton">
        <el-skeleton animated
          ><template #template
            ><el-skeleton-item variant="image" class="cover" />
            <div class="lines">
              <el-skeleton-item variant="h3" /><el-skeleton-item variant="text" /><el-skeleton-item variant="button" /></div></template
        ></el-skeleton>
      </div>
    </template>
    <template v-else>
      <div class="table-head"></div>
      <el-skeleton v-for="index in 6" :key="index" animated :rows="1" class="table-row" />
    </template>
    <span class="sr-only">资料加载中</span>
  </div>
</template>

<script setup lang="ts">
defineProps<{ mode: 'grid' | 'list' }>();
</script>

<style scoped>
.resource-skeleton.grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 16px;
}
.grid-skeleton {
  min-height: 136px;
  border: 1px solid var(--ip-neutral-200);
  border-radius: var(--ip-radius-md);
  padding: 12px;
  background: var(--ip-neutral-0);
}
.grid-skeleton :deep(.el-skeleton__template) {
  display: grid;
  grid-template-columns: 76px minmax(0, 1fr);
  gap: 12px;
}
.cover {
  width: 76px;
  height: 108px;
  border-radius: var(--ip-radius-sm);
}
.lines {
  display: grid;
  align-content: center;
  gap: 12px;
}
.table-head {
  height: 44px;
  border-radius: var(--ip-radius-sm);
  background: var(--ip-neutral-100);
}
.table-row {
  min-height: 72px;
  border-bottom: 1px solid var(--ip-neutral-100);
  padding: 24px 16px;
}
.sr-only {
  position: absolute;
  width: 1px;
  height: 1px;
  overflow: hidden;
  clip: rect(0, 0, 0, 0);
}
@media (max-width: 1320px) {
  .resource-skeleton.grid {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
}
@media (max-width: 980px) {
  .resource-skeleton.grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}
@media (max-width: 767px) {
  .resource-skeleton.grid {
    grid-template-columns: 1fr;
  }
}
</style>

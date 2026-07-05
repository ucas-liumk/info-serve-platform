<template>
  <el-dialog v-model="visible" title="全部服务" width="920px" align-center append-to-body class="all-module-dialog">
    <div class="all-module-head">
      <strong>全部服务模块</strong>
      <span>按后台模块注册表排序展示，排序靠前 6 项进入首页首屏。</span>
    </div>
    <div class="all-module-grid">
      <button v-for="item in modules" :key="item.code || item.title" class="all-module-card" type="button" @click="openModule(item)">
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
  </el-dialog>
</template>

<script setup lang="ts">
import { computed } from 'vue';
import IconArrowRight from '~icons/material-symbols/arrow-right-alt-rounded';

interface HomeModule {
  code?: string;
  title: string;
  desc: string;
  image: string;
  path?: string;
  sortOrder?: number;
}

const props = defineProps<{ modelValue: boolean; modules: HomeModule[] }>();
const emit = defineEmits<{ (e: 'update:modelValue', value: boolean): void; (e: 'open', item: HomeModule): void }>();

const visible = computed({
  get: () => props.modelValue,
  set: (value: boolean) => emit('update:modelValue', value)
});

const openModule = (item: HomeModule) => {
  emit('update:modelValue', false);
  emit('open', item);
};
</script>

<style scoped>
:global(.all-module-dialog .el-dialog) {
  border-radius: 16px;
}

:global(.all-module-dialog .el-dialog__body) {
  padding-top: 8px;
}

.all-module-head {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 18px;
  margin-bottom: 18px;
  padding-bottom: 14px;
  border-bottom: 1px solid var(--ip-neutral-200);
}

.all-module-head strong {
  color: var(--ip-neutral-900);
  font-size: 20px;
  line-height: 1.2;
  font-weight: 700;
}

.all-module-head span {
  color: var(--ip-neutral-500);
  font-size: 13px;
  line-height: 1.5;
  font-weight: 600;
  text-align: right;
}

.all-module-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 14px;
}

.all-module-card {
  min-width: 0;
  min-height: 98px;
  display: grid;
  grid-template-columns: 54px minmax(0, 1fr) 28px;
  align-items: center;
  gap: 12px;
  box-sizing: border-box;
  border: 1px solid var(--ip-neutral-200);
  border-radius: 10px;
  padding: 14px;
  background: var(--ip-neutral-0);
  color: var(--ip-neutral-900);
  box-shadow: var(--ip-shadow-sm);
  text-align: left;
  cursor: pointer;
  transition:
    border-color var(--ip-motion-base) var(--ip-motion-ease),
    box-shadow var(--ip-motion-base) var(--ip-motion-ease),
    transform var(--ip-motion-base) var(--ip-motion-ease);
}

.all-module-card:hover {
  border-color: var(--ip-primary-200);
  box-shadow: var(--ip-shadow-md);
  transform: translateY(-2px);
}

.all-module-icon {
  width: 54px;
  height: 54px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 10px;
  background: var(--ip-primary-50);
}

.all-module-icon img {
  width: 46px;
  height: 46px;
  object-fit: contain;
}

.all-module-copy {
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 7px;
}

.all-module-copy strong {
  overflow: hidden;
  color: var(--ip-primary-900);
  font-size: 16px;
  line-height: 1.2;
  font-weight: 700;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.all-module-copy em {
  display: -webkit-box;
  overflow: hidden;
  color: var(--ip-neutral-600);
  font-size: 13px;
  line-height: 1.35;
  font-style: normal;
  font-weight: 600;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
}

.all-module-action {
  width: 28px;
  height: 28px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 999px;
  background: var(--ip-primary-50);
  color: var(--ip-primary-600);
}

.all-module-action svg {
  width: 17px;
  height: 17px;
}

@media (max-width: 1023px) {
  .all-module-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 640px) {
  .all-module-head {
    display: block;
  }

  .all-module-head span {
    display: block;
    margin-top: 8px;
    text-align: left;
  }

  .all-module-grid {
    grid-template-columns: 1fr;
  }
}
</style>

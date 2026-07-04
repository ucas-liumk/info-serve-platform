<template>
  <aside class="resource-sidebar">
    <div class="side-brand">
      <img src="@/assets/portal/module-resource.png" alt="资源共享" />
      <div>
        <strong>资源共享</strong>
        <span>知识汇聚 · 共享价值</span>
      </div>
    </div>

    <button class="home-button" type="button" @click="goPortalHome">
      <el-icon><House /></el-icon>
      <span>返回首页</span>
    </button>

    <ResourceFilterPanel
      :categories="categories"
      :total="categoryTotal"
      :category-code="categoryCode"
      @change-category="onChangeCategory"
    />
  </aside>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router';
import { House } from '@element-plus/icons-vue';
import { PORTAL_HOME_PATH } from '@/constants/router';
import { ResourceCategory } from '@/api/infoservice/types';
import ResourceFilterPanel from './ResourceFilterPanel.vue';

defineProps<{ categories: ResourceCategory[]; categoryTotal: number; categoryCode: string }>();
const emit = defineEmits<{ (e: 'change-category', code: string): void }>();

const router = useRouter();

const goPortalHome = () => {
  router.push(PORTAL_HOME_PATH);
};

const onChangeCategory = (code: string) => emit('change-category', code);
</script>

<style scoped>
.resource-sidebar {
  position: sticky;
  top: 18px;
  align-self: start;
  max-height: calc(100vh - 36px);
  display: grid;
  grid-template-rows: auto auto minmax(0, 1fr);
  gap: 14px;
  overflow: hidden;
}

.resource-sidebar :deep(.filter-panel) {
  min-height: 0;
  overflow: auto;
}

.side-brand {
  position: relative;
  height: 86px;
  min-height: 86px;
  display: flex;
  align-items: center;
  gap: 13px;
  box-sizing: border-box;
  border: 1px solid var(--resource-border);
  border-radius: 8px;
  padding: 16px;
  overflow: hidden;
  background: linear-gradient(135deg, rgba(47, 138, 122, 0.12), transparent 42%), linear-gradient(180deg, #fff 0%, #f8fafc 100%);
  box-shadow: 0 14px 34px rgba(31, 54, 76, 0.08);
}

.side-brand::after {
  content: '';
  position: absolute;
  right: -24px;
  bottom: -34px;
  width: 110px;
  height: 72px;
  border: 1px solid rgba(47, 138, 122, 0.2);
  border-radius: 8px;
  transform: rotate(-14deg);
  pointer-events: none;
}

.side-brand img {
  position: relative;
  z-index: 1;
  width: 42px;
  height: 42px;
  object-fit: contain;
}

.side-brand div {
  position: relative;
  z-index: 1;
  min-width: 0;
  display: grid;
  gap: 4px;
}

.side-brand strong {
  color: var(--resource-title);
  font-size: 22px;
  line-height: 1.15;
  font-weight: 900;
}

.side-brand span {
  color: var(--resource-muted);
  font-size: 13px;
  font-weight: 700;
  white-space: nowrap;
}

.home-button {
  width: 100%;
  height: 40px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 7px;
  border: 1px solid var(--resource-border);
  border-radius: 8px;
  background: #fff;
  color: var(--resource-text);
  font-size: 14px;
  line-height: 1;
  font-weight: 850;
  white-space: nowrap;
  cursor: pointer;
  transition:
    border-color 0.18s ease,
    background 0.18s ease,
    color 0.18s ease,
    box-shadow 0.18s ease,
    transform 0.18s ease;
}

.home-button:hover {
  border-color: var(--resource-primary);
  background: var(--resource-primary-soft);
  color: var(--resource-primary);
  transform: translateY(-1px);
}

@media (max-width: 980px) {
  .resource-sidebar {
    position: static;
    max-height: none;
  }

  .resource-sidebar :deep(.filter-panel) {
    max-height: none;
  }
}
</style>

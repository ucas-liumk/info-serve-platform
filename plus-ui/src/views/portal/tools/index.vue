<template>
  <div class="tools-market">
    <ToolsSidebar :view-mode="viewMode" @switch-mode="switchMode" />

    <main class="market-main">
      <header class="market-top">
        <div class="title-block">
          <h1>{{ pageTitle }}</h1>
          <p>{{ pageSubtitle }}</p>
        </div>

        <div class="top-actions">
          <div class="search-box">
            <el-input
              v-model="searchDraft"
              class="market-search"
              clearable
              placeholder="搜索工具名称、描述或标签"
              @keyup.enter="onSearch"
              @clear="onSearch"
            >
              <template #prefix>
                <el-icon><Search /></el-icon>
              </template>
            </el-input>
            <button class="search-button" type="button" @click="onSearch">搜索</button>
          </div>

          <button class="demand-btn" type="button" @click="openDemandDialog">
            <el-icon><DocumentAdd /></el-icon>
            <span>需求反馈</span>
          </button>

          <PortalNotificationBell />
        </div>
      </header>

      <CategoryTabs
        v-model:model="categoryCode"
        v-model:sort="sort"
        :categories="categories"
        :total="viewMode === 'market' ? total : favoriteTotal"
      />

      <section v-loading="loading" class="app-grid">
        <AppCard v-for="app in apps" :key="app.appId" :app="app" @changed="reload" />
      </section>
      <el-empty v-if="!loading && apps.length === 0" class="empty" :description="emptyText" />

      <el-pagination
        class="pager"
        background
        layout="prev, pager, next"
        :total="viewMode === 'market' ? total : favoriteTotal"
        :page-size="pageSize"
        :current-page="pageNum"
        @current-change="onPage"
      />
    </main>

    <DemandDialog ref="demandDialogRef" />
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue';
import { DocumentAdd, Search } from '@element-plus/icons-vue';
import CategoryTabs from './components/CategoryTabs.vue';
import AppCard from './components/AppCard.vue';
import ToolsSidebar from './components/ToolsSidebar.vue';
import DemandDialog from './components/DemandDialog.vue';
import { listApps, listCategories, listFavorites } from '@/api/portal/appcenter';
import { PortalApp, PortalCategory } from '@/api/appcenter/types';
import PortalNotificationBell from '@/layout/portal/components/PortalNotificationBell.vue';

type ViewMode = 'market' | 'favorites';

const apps = ref<PortalApp[]>([]);
const categories = ref<PortalCategory[]>([]);
const viewMode = ref<ViewMode>('market');
const categoryCode = ref('all');
const sort = ref('latest');
const keyword = ref('');
const searchDraft = ref('');
const pageNum = ref(1);
const pageSize = ref(15);
const total = ref(0);
const favoriteTotal = ref(0);
const loading = ref(false);
const demandDialogRef = ref<InstanceType<typeof DemandDialog>>();

const currentToolTotal = computed(() => (viewMode.value === 'market' ? total.value : favoriteTotal.value));
const activeToolCategory = computed(() => categories.value.find((item) => item.categoryCode === categoryCode.value));
const activeToolCategoryName = computed(() => (categoryCode.value === 'all' ? '全部工具' : activeToolCategory.value?.categoryName || '当前分类'));

const pageTitle = computed(() => {
  if (viewMode.value === 'favorites') return '收藏工具';
  return '工具即用';
});

const pageSubtitle = computed(() => {
  if (viewMode.value === 'favorites') return `${activeToolCategoryName.value} · 共 ${currentToolTotal.value} 个收藏工具`;
  return `${activeToolCategoryName.value} · 共 ${currentToolTotal.value} 个工具`;
});

const emptyText = computed(() => (viewMode.value === 'favorites' ? '暂无收藏工具' : '暂无工具，稍后再试或提交需求反馈'));

const reloadApps = async () => {
  const query = {
    categoryCode: categoryCode.value,
    keyword: keyword.value,
    sort: sort.value,
    pageNum: pageNum.value,
    pageSize: pageSize.value
  };
  const res: any = viewMode.value === 'favorites' ? await listFavorites(query) : await listApps(query);
  apps.value = res.rows || [];
  if (viewMode.value === 'favorites') {
    favoriteTotal.value = res.total || 0;
  } else {
    total.value = res.total || 0;
  }
};

const reload = async () => {
  loading.value = true;
  try {
    await reloadApps();
  } finally {
    loading.value = false;
  }
};

const onSearch = () => {
  keyword.value = searchDraft.value.trim();
  pageNum.value = 1;
  reload();
};

const onPage = (page: number) => {
  pageNum.value = page;
  reload();
};

const switchMode = (mode: ViewMode) => {
  viewMode.value = mode;
  pageNum.value = 1;
  if (mode !== 'market') {
    categoryCode.value = 'all';
  }
  reload();
};

const openDemandDialog = () => {
  demandDialogRef.value?.open();
};

watch([categoryCode, sort], () => {
  pageNum.value = 1;
  reload();
});

onMounted(async () => {
  const res: any = await listCategories();
  categories.value = res.data || [];
  await reload();
});
</script>

<style scoped>
.tools-market {
  min-height: 100vh;
  --tool-primary: var(--ip-primary-600);
  --tool-primary-deep: var(--ip-primary-700);
  --tool-primary-soft: var(--ip-primary-50);
  --tool-accent: var(--ip-mod-appcenter);
  --tool-accent-soft: var(--ip-mod-appcenter-soft);
  --tool-title: var(--ip-neutral-900);
  --tool-text: var(--ip-neutral-700);
  --tool-muted: var(--ip-neutral-500);
  --tool-weak: var(--ip-neutral-400);
  --tool-border: var(--ip-neutral-200);
  --tool-input-border: var(--ip-neutral-300);
  display: grid;
  grid-template-columns: 276px minmax(0, 1fr);
  gap: 22px;
  padding: 18px 28px 44px;
  background: linear-gradient(180deg, rgba(241, 244, 248, 0.95) 0%, rgba(247, 249, 252, 0.82) 320px), var(--ip-neutral-50);
  color: var(--tool-text);
  font-family: 'HarmonyOS Sans SC', 'PingFang SC', 'Microsoft YaHei', sans-serif;
}

.market-main {
  min-width: 0;
  display: grid;
  align-content: start;
  gap: 16px;
  padding: 0;
}

.market-top {
  position: relative;
  min-width: 0;
  min-height: 86px;
  display: grid;
  grid-template-columns: minmax(220px, 390px) minmax(0, 1fr);
  align-items: center;
  gap: 18px;
  box-sizing: border-box;
  border: 1px solid var(--tool-border);
  border-radius: 10px;
  padding: 14px 16px 14px 20px;
  overflow: hidden;
  background: rgba(255, 255, 255, 0.94);
  box-shadow: var(--ip-shadow-sm);
}

.market-top::before {
  content: '';
  position: absolute;
  inset: 0 auto 0 0;
  width: 4px;
  background: var(--tool-accent);
  pointer-events: none;
}

.title-block {
  position: relative;
  z-index: 1;
  min-width: 0;
}

.title-block h1 {
  margin: 0;
  color: var(--tool-title);
  font-size: 24px;
  line-height: 1.15;
  font-weight: 700;
}

.title-block p {
  margin: 6px 0 0;
  overflow: hidden;
  color: var(--tool-muted);
  font-size: 13px;
  font-weight: 700;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.top-actions {
  position: relative;
  z-index: 1;
  min-width: 0;
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 10px;
}

.search-box {
  min-width: 300px;
  max-width: 620px;
  flex: 1;
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 8px;
}

.market-search {
  min-width: 0;
  --el-input-height: 40px;
}

:deep(.market-search .el-input__wrapper) {
  padding: 0 14px;
  border-radius: 6px;
  background: var(--ip-neutral-0);
  box-shadow: 0 0 0 1px var(--tool-input-border) inset;
}

:deep(.market-search .el-input__inner) {
  color: var(--tool-text);
  font-size: 14px;
  font-weight: 700;
}

.search-button,
.demand-btn {
  height: 40px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 7px;
  border: 1px solid var(--tool-primary);
  border-radius: 6px;
  padding: 0 15px;
  background: var(--tool-primary);
  color: var(--ip-neutral-0);
  font-size: 14px;
  line-height: 1;
  font-weight: 700;
  white-space: nowrap;
  cursor: pointer;
  transition:
    background 0.18s ease,
    box-shadow 0.18s ease,
    transform 0.18s ease;
}

.search-button {
  padding: 0 16px;
  background: var(--ip-neutral-0);
  color: var(--tool-primary);
}

.search-button:hover {
  background: var(--tool-primary-soft);
  box-shadow: var(--ip-shadow-md);
  transform: translateY(-1px);
}

.demand-btn:hover {
  background: var(--tool-primary-deep);
  box-shadow: var(--ip-shadow-md);
  transform: translateY(-1px);
}

.demand-btn .el-icon {
  font-size: 18px;
}

.notification-btn {
  position: relative;
  width: 40px;
  height: 40px;
  flex: 0 0 auto;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border: 1px solid var(--tool-input-border);
  border-radius: 6px;
  background: var(--ip-neutral-0);
  color: var(--tool-muted);
  cursor: pointer;
  box-shadow: var(--ip-shadow-sm);
  transition:
    border-color 0.16s ease,
    color 0.16s ease,
    box-shadow 0.16s ease,
    transform 0.16s ease;
}

.notification-btn:hover {
  border-color: var(--ip-neutral-300);
  color: var(--tool-primary);
  background: var(--tool-primary-soft);
  box-shadow: var(--ip-shadow-md);
  transform: translateY(-1px);
}

.notification-btn .el-icon {
  font-size: 19px;
}

.notification-badge {
  position: absolute;
  top: 7px;
  right: 7px;
  min-width: 18px;
  height: 18px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 999px;
  padding: 0 5px;
  background: var(--ip-danger);
  color: var(--ip-neutral-0);
  font-size: 12px;
  line-height: 1;
  font-weight: 850;
}

:global(.tool-notification-popper) {
  border: 1px solid var(--tool-input-border) !important;
  border-radius: 10px !important;
  padding: 14px !important;
  box-shadow: var(--ip-shadow-lg) !important;
}

.notice-popover {
  color: var(--ip-neutral-900);
}

.notice-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding-bottom: 12px;
  border-bottom: 1px solid var(--tool-border);
}

.notice-head strong {
  display: block;
  color: var(--tool-title);
  font-size: 17px;
  line-height: 1.2;
  font-weight: 850;
}

.notice-head span {
  display: block;
  margin-top: 4px;
  color: #6b7a99;
  font-size: 13px;
  line-height: 1.2;
  font-weight: 650;
}

.notice-head-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.notice-refresh,
.notice-clear {
  height: 32px;
  display: inline-flex;
  align-items: center;
  gap: 6px;
  border: 1px solid var(--tool-input-border);
  border-radius: 7px;
  padding: 0 10px;
  background: #fff;
  color: var(--tool-primary);
  font-size: 13px;
  font-weight: 750;
  cursor: pointer;
}

.notice-clear {
  border-color: #ffd1d1;
  color: #d93026;
}

.notice-tabs {
  margin-top: 8px;
}

:deep(.notice-tabs .el-tabs__header) {
  margin: 0;
}

:deep(.notice-tabs .el-tabs__nav-wrap::after) {
  height: 1px;
  background: var(--tool-border);
}

.notice-list {
  min-height: 118px;
  max-height: 420px;
  display: flex;
  flex-direction: column;
  gap: 10px;
  overflow-y: auto;
  padding-top: 12px;
}

.notice-item {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 14px;
  padding: 12px;
  border: 1px solid var(--tool-border);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.96);
}

.notice-item.unread {
  border-color: #b8c9d9;
  background: #f8fafc;
}

.notice-main {
  min-width: 0;
}

.notice-main strong {
  display: block;
  color: var(--tool-title);
  font-size: 15px;
  line-height: 1.35;
  font-weight: 850;
}

.notice-main p {
  margin: 7px 0 0;
  color: #4c5f86;
  font-size: 13px;
  line-height: 1.5;
  font-weight: 600;
}

.notice-main time {
  display: block;
  margin-top: 7px;
  color: var(--tool-weak);
  font-size: 12px;
  line-height: 1.2;
  font-weight: 600;
}

.notice-actions {
  flex: 0 0 auto;
  display: flex;
  align-items: center;
  gap: 8px;
}

.notice-item button {
  height: 30px;
  display: inline-flex;
  align-items: center;
  gap: 5px;
  border: 1px solid #b8c9d9;
  border-radius: 7px;
  padding: 0 10px;
  background: #fff;
  color: var(--tool-primary);
  font-size: 13px;
  font-weight: 750;
  cursor: pointer;
}

.notice-item .notice-delete {
  border-color: #ffd1d1;
  color: #d93026;
}

.notice-pager {
  margin-top: 12px;
  justify-content: center;
}

.app-grid {
  width: 100%;
  min-width: 0;
  min-height: 620px;
  box-sizing: border-box;
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
  align-items: stretch;
  gap: 16px;
  margin-top: 14px;
}

.empty {
  margin-top: 42px;
}

.pager {
  margin-top: 28px;
  display: flex;
  justify-content: center;
}

:deep(.pager .btn-prev),
:deep(.pager .btn-next),
:deep(.pager .el-pager li) {
  min-width: 38px;
  height: 38px;
  border-radius: 6px;
  background: transparent;
  color: var(--tool-title);
  font-size: 16px;
  font-weight: 750;
}

:deep(.pager .el-pager li.is-active) {
  background: var(--tool-primary);
  color: var(--ip-neutral-0);
  box-shadow: var(--ip-shadow-md);
}

:deep(.pager .btn-prev),
:deep(.pager .btn-next) {
  background: var(--ip-neutral-0);
  box-shadow: var(--ip-shadow-sm);
}

@media (max-width: 1360px) {
  .tools-market {
    grid-template-columns: 256px minmax(0, 1fr);
    padding-inline: 22px;
  }
}

@media (max-width: 1120px) {
  .market-top {
    grid-template-columns: 1fr;
  }

  .top-actions {
    width: 100%;
    align-items: stretch;
    flex-direction: column;
    gap: 14px;
  }

  .search-box {
    width: 100%;
  }

  .market-search {
    min-width: 0;
  }

  .demand-btn {
    justify-content: center;
  }

  .notification-btn {
    align-self: flex-end;
  }
}

@media (max-width: 980px) {
  .tools-market {
    grid-template-columns: 1fr;
    padding: 16px;
  }

  .market-main {
    padding: 0;
  }
}

@media (max-width: 760px) {
  .market-main {
    padding: 0;
  }

  .app-grid {
    grid-template-columns: 1fr;
  }
}
</style>

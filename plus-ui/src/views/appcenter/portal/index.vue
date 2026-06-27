<template>
  <div class="tools-market">
    <aside class="tools-sidebar">
      <div class="side-brand">
        <img :src="logoUrl" alt="工具即用" />
        <strong>工具即用</strong>
      </div>

      <button class="home-link" type="button" @click="goHome">
        <el-icon><House /></el-icon>
        <span>返回首页</span>
      </button>

      <nav class="side-nav" aria-label="工具即用导航">
        <button
          v-for="item in navItems"
          :key="item.key"
          :class="['side-link', { active: viewMode === item.key }]"
          type="button"
          @click="switchMode(item.key)"
        >
          <el-icon><component :is="item.icon" /></el-icon>
          <span>{{ item.label }}</span>
          <b v-if="item.badge">{{ item.badge }}</b>
          <el-icon v-if="item.key === 'messages'" class="side-arrow"><ArrowRight /></el-icon>
        </button>
      </nav>

      <div class="side-visual" aria-hidden="true">
        <img :src="logoUrl" alt="" />
        <span></span>
      </div>
    </aside>

    <main class="market-main">
      <header class="market-top">
        <div class="title-block">
          <h1>{{ pageTitle }}</h1>
          <p>{{ pageSubtitle }}</p>
        </div>

        <div class="top-actions">
          <el-input
            v-model="searchDraft"
            class="market-search"
            clearable
            placeholder="搜索应用名称、描述或标签"
            @keyup.enter="onSearch"
            @clear="onSearch"
          >
            <template #suffix>
              <button class="search-trigger" type="button" title="搜索" @click="onSearch">
                <el-icon><Search /></el-icon>
              </button>
            </template>
          </el-input>

          <button class="demand-btn" type="button" @click="submitDemand">
            <el-icon><DocumentAdd /></el-icon>
            <span>应用需求</span>
          </button>
        </div>
      </header>

      <CategoryTabs
        v-if="viewMode !== 'messages'"
        v-model:model="categoryCode"
        v-model:sort="sort"
        :categories="categories"
        :total="viewMode === 'market' ? total : favoriteTotal"
      />

      <section v-if="viewMode === 'messages'" v-loading="loading" class="messages-panel">
        <article v-for="message in messages" :key="message.messageId" :class="['message-item', { unread: message.isRead !== '1' }]">
          <div>
            <strong>{{ message.title }}</strong>
            <p>{{ message.content }}</p>
          </div>
          <button type="button" @click="markMessageRead(message)">标为已读</button>
        </article>
        <el-empty v-if="!loading && messages.length === 0" description="暂无消息" />
      </section>

      <template v-else>
        <section v-loading="loading" class="app-grid">
          <AppCard v-for="app in apps" :key="app.appId" :app="app" @changed="reload" />
        </section>
        <el-empty v-if="!loading && apps.length === 0" class="empty" description="暂无应用" />

        <el-pagination
          class="pager"
          background
          layout="prev, pager, next"
          :total="viewMode === 'market' ? total : favoriteTotal"
          :page-size="pageSize"
          :current-page="pageNum"
          @current-change="onPage"
        />
      </template>
    </main>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';
import { ArrowRight, Bell, DocumentAdd, Grid, House, Search, Star } from '@element-plus/icons-vue';
import CategoryTabs from './components/CategoryTabs.vue';
import AppCard from './components/AppCard.vue';
import { listApps, listCategories, listFavorites, listMessages, readMessage, unreadCount } from '@/api/appcenter/portal';
import { PortalApp, PortalCategory, PortalMessage } from '@/api/appcenter/types';
import logoUrl from '@/assets/portal/home-logo.png';

type ViewMode = 'market' | 'favorites' | 'messages';

const apps = ref<PortalApp[]>([]);
const messages = ref<PortalMessage[]>([]);
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
const unread = ref(0);
const loading = ref(false);
const router = useRouter();

const navItems = computed(() => [
  { key: 'market' as const, label: '应用市场', icon: Grid, badge: 0 },
  { key: 'favorites' as const, label: '收藏应用', icon: Star, badge: 0 },
  { key: 'messages' as const, label: '消息中心', icon: Bell, badge: unread.value }
]);

const pageTitle = computed(() => {
  if (viewMode.value === 'favorites') return '收藏应用';
  if (viewMode.value === 'messages') return '消息中心';
  return '应用市场';
});

const pageSubtitle = computed(() => {
  if (viewMode.value === 'favorites') return '快速访问你收藏的高频工具';
  if (viewMode.value === 'messages') return '应用上架、审核与服务通知';
  return '精选开源应用 · 安全可控 · 按需试用';
});

const loadUnread = async () => {
  try {
    const res: any = await unreadCount();
    unread.value = Number(res.data || 0);
  } catch {
    unread.value = 0;
  }
};

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

const reloadMessages = async () => {
  const res: any = await listMessages({ pageNum: pageNum.value, pageSize: pageSize.value, keyword: keyword.value });
  messages.value = res.rows || res.data || [];
  total.value = res.total || messages.value.length;
};

const reload = async () => {
  loading.value = true;
  try {
    if (viewMode.value === 'messages') {
      await reloadMessages();
      await loadUnread();
    } else {
      await reloadApps();
    }
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

const goHome = () => {
  router.push('/portal');
};

const submitDemand = () => {
  ElMessage.info('应用需求提交将在后续版本开放');
};

const markMessageRead = async (message: PortalMessage) => {
  if (message.isRead !== '1') {
    await readMessage(message.messageId);
    reload();
  }
};

watch([categoryCode, sort], () => {
  if (viewMode.value === 'messages') return;
  pageNum.value = 1;
  reload();
});

onMounted(async () => {
  const res: any = await listCategories();
  categories.value = res.data || [];
  await Promise.all([reload(), loadUnread()]);
});
</script>

<style scoped>
.tools-market {
  min-height: 100vh;
  display: flex;
  background: #f7faff;
  color: #0f1f3d;
  font-family: "HarmonyOS Sans SC", "PingFang SC", "Microsoft YaHei", sans-serif;
}

.tools-sidebar {
  position: sticky;
  top: 0;
  width: 296px;
  flex: 0 0 296px;
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  border-right: 1px solid #e1e9f5;
  background: linear-gradient(180deg, #fff 0%, #f7fbff 100%);
  box-shadow: 12px 0 34px rgba(31, 84, 156, 0.04);
}

.side-brand {
  height: 112px;
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 0 32px;
}

.side-brand img {
  width: 46px;
  height: 46px;
  object-fit: contain;
}

.side-brand strong {
  color: #0a1f44;
  font-size: 26px;
  line-height: 1;
  font-weight: 800;
}

.home-link {
  height: 44px;
  display: inline-flex;
  align-items: center;
  gap: 10px;
  margin: 0 20px 10px;
  border: 1px solid #d9e5f6;
  border-radius: 8px;
  padding: 0 18px;
  background: rgba(255, 255, 255, 0.82);
  color: #49618c;
  font-size: 15px;
  line-height: 1;
  font-weight: 750;
  cursor: pointer;
  box-shadow: 0 10px 24px rgba(31, 84, 156, 0.04);
  transition: border-color 0.16s ease, background 0.16s ease, color 0.16s ease, box-shadow 0.16s ease;
}

.home-link .el-icon {
  color: #49618c;
  font-size: 18px;
}

.home-link:hover {
  border-color: #b8cff4;
  background: #f5f9ff;
  color: #1260e8;
  box-shadow: 0 12px 28px rgba(18, 96, 232, 0.08);
}

.home-link:hover .el-icon {
  color: #1260e8;
}

.side-nav {
  display: flex;
  flex-direction: column;
  gap: 18px;
  padding: 14px 20px 24px;
}

.side-link {
  position: relative;
  height: 60px;
  display: flex;
  align-items: center;
  gap: 18px;
  border: 0;
  border-radius: 8px;
  padding: 0 22px;
  background: transparent;
  color: #35476d;
  font-size: 17px;
  font-weight: 750;
  text-align: left;
  cursor: pointer;
  transition: background 0.16s ease, color 0.16s ease;
}

.side-link .el-icon {
  width: 22px;
  height: 22px;
  color: #436294;
  font-size: 22px;
}

.side-link.active {
  background: #edf4ff;
  color: #1260e8;
}

.side-link.active .el-icon {
  color: #1260e8;
}

.side-link b {
  min-width: 24px;
  height: 24px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  margin-left: auto;
  border-radius: 999px;
  background: #ff312a;
  color: #fff;
  font-size: 13px;
  font-weight: 800;
}

.side-arrow {
  margin-left: auto;
  font-size: 15px !important;
}

.side-visual {
  position: relative;
  min-height: 284px;
  margin-top: auto;
  display: flex;
  align-items: center;
  justify-content: center;
}

.side-visual img {
  position: relative;
  z-index: 1;
  width: 128px;
  height: 128px;
  object-fit: contain;
  opacity: 0.82;
  filter: drop-shadow(0 22px 34px rgba(54, 116, 219, 0.22));
}

.side-visual span {
  position: absolute;
  left: 42px;
  right: 42px;
  bottom: 46px;
  height: 92px;
  border-radius: 24px;
  background: linear-gradient(135deg, rgba(226, 239, 255, 0.8), rgba(240, 246, 255, 0.2));
  box-shadow: 0 22px 46px rgba(54, 116, 219, 0.13);
}

.market-main {
  flex: 1;
  min-width: 0;
  padding: 34px 46px 30px;
}

.market-top {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 32px;
  margin-bottom: 28px;
}

.title-block h1 {
  margin: 0;
  color: #0b1833;
  font-size: 34px;
  line-height: 1.15;
  font-weight: 850;
}

.title-block p {
  margin: 12px 0 0;
  color: #49618c;
  font-size: 17px;
  line-height: 1.2;
  font-weight: 650;
}

.top-actions {
  display: flex;
  align-items: center;
  gap: 36px;
}

.market-search {
  width: 406px;
  --el-input-height: 54px;
}

:deep(.market-search .el-input__wrapper) {
  padding: 0 16px 0 20px;
  border-radius: 9px;
  background: #fff;
  box-shadow: 0 0 0 1px #dbe5f4 inset;
}

:deep(.market-search .el-input__inner) {
  color: #20355c;
  font-size: 14px;
  font-weight: 700;
}

.search-trigger {
  width: 32px;
  height: 32px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border: 0;
  background: transparent;
  color: #385283;
  cursor: pointer;
}

.search-trigger .el-icon {
  font-size: 22px;
}

.demand-btn {
  height: 54px;
  display: inline-flex;
  align-items: center;
  gap: 10px;
  border: 1px solid #1260e8;
  border-radius: 8px;
  padding: 0 24px;
  background: #fff;
  color: #1260e8;
  font-size: 17px;
  line-height: 1;
  font-weight: 850;
  white-space: nowrap;
  cursor: pointer;
  box-shadow: 0 12px 26px rgba(18, 96, 232, 0.08);
}

.demand-btn .el-icon {
  font-size: 23px;
}

.app-grid {
  min-height: 826px;
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  align-items: start;
  gap: 14px;
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
  border-radius: 8px;
  background: transparent;
  color: #0b1833;
  font-size: 16px;
  font-weight: 750;
}

:deep(.pager .el-pager li.is-active) {
  background: #1260e8;
  color: #fff;
  box-shadow: 0 10px 22px rgba(18, 96, 232, 0.22);
}

:deep(.pager .btn-prev),
:deep(.pager .btn-next) {
  background: #fff;
  box-shadow: 0 8px 20px rgba(42, 87, 160, 0.08);
}

.messages-panel {
  min-height: 560px;
  display: flex;
  flex-direction: column;
  gap: 14px;
  margin-top: 14px;
}

.message-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 24px;
  padding: 20px 22px;
  border: 1px solid #e1e9f6;
  border-radius: 10px;
  background: rgba(255, 255, 255, 0.94);
  box-shadow: 0 8px 22px rgba(42, 87, 160, 0.05);
}

.message-item.unread {
  border-color: #bcd1f7;
}

.message-item strong {
  color: #0f1f3d;
  font-size: 17px;
  font-weight: 800;
}

.message-item p {
  margin: 8px 0 0;
  color: #4c5f86;
  font-size: 14px;
  line-height: 1.5;
  font-weight: 600;
}

.message-item button {
  height: 36px;
  border: 1px solid #b7cef7;
  border-radius: 7px;
  padding: 0 14px;
  background: #fff;
  color: #1260e8;
  font-weight: 750;
  cursor: pointer;
}

@media (max-width: 1720px) {
  .app-grid {
    grid-template-columns: repeat(4, minmax(0, 1fr));
  }
}

@media (max-width: 1500px) {
  .tools-sidebar {
    width: 270px;
    flex-basis: 270px;
  }

  .market-main {
    padding: 30px 32px;
  }

  .top-actions {
    gap: 18px;
  }

  .market-search {
    width: 360px;
  }

  .app-grid {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
}

@media (max-width: 1120px) {
  .tools-sidebar {
    width: 238px;
    flex-basis: 238px;
  }

  .side-brand {
    height: 96px;
    padding-inline: 24px;
  }

  .side-brand strong {
    font-size: 23px;
  }

  .home-link {
    margin-inline: 14px;
    padding-inline: 16px;
  }

  .side-nav {
    gap: 12px;
    padding: 18px 14px;
  }

  .side-link {
    height: 56px;
    gap: 14px;
    padding: 0 16px;
    font-size: 16px;
  }

  .market-top {
    flex-direction: column;
  }

  .top-actions {
    width: 100%;
    align-items: stretch;
    flex-direction: column;
    gap: 14px;
  }

  .market-search {
    width: 100%;
  }

  .demand-btn {
    justify-content: center;
  }

  .app-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .side-visual {
    min-height: 220px;
  }
}

@media (max-width: 760px) {
  .tools-market {
    display: block;
  }

  .tools-sidebar {
    position: relative;
    width: 100%;
    min-height: auto;
    border-right: 0;
    border-bottom: 1px solid #e1e9f5;
  }

  .side-brand {
    height: 84px;
  }

  .side-nav {
    display: grid;
    grid-template-columns: 1fr;
    gap: 10px;
    padding-top: 0;
  }

  .home-link {
    width: calc(100% - 28px);
    justify-content: center;
    margin: 0 14px 12px;
  }

  .side-visual {
    display: none;
  }

  .market-main {
    padding: 24px 18px;
  }

  .app-grid {
    grid-template-columns: 1fr;
  }
}
</style>

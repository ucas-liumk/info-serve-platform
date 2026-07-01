<template>
  <div class="tools-market">
    <aside class="tools-sidebar">
      <div class="side-brand">
        <img :src="logoUrl" alt="工具即用" />
        <div>
          <strong>工具即用</strong>
          <span>即开即用 · 提升效率</span>
        </div>
      </div>

      <button class="home-link" type="button" @click="goHome">
        <el-icon><House /></el-icon>
        <span>返回首页</span>
      </button>

      <nav class="side-nav" aria-label="工具即用导航">
        <div class="nav-title">
          <span>工具导航</span>
          <em>{{ navItems.length }} 项</em>
        </div>
        <button
          v-for="item in navItems"
          :key="item.key"
          :class="['side-link', { active: viewMode === item.key }]"
          type="button"
          @click="switchMode(item.key)"
        >
          <span class="nav-icon">
            <el-icon><component :is="item.icon" /></el-icon>
          </span>
          <strong>{{ item.label }}</strong>
        </button>
      </nav>
    </aside>

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
      <el-empty v-if="!loading && apps.length === 0" class="empty" description="暂无工具" />

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

    <el-dialog v-model="demandDialogVisible" class="portal-demand-dialog" width="760px" append-to-body align-center :close-on-click-modal="false">
      <template #header>
        <div class="demand-dialog-head">
          <span class="demand-dialog-icon">
            <el-icon><DocumentAdd /></el-icon>
          </span>
          <div>
            <strong>需求反馈</strong>
            <p>提交希望上架的应用，或反馈现有应用的改进建议。</p>
          </div>
        </div>
      </template>

      <el-tabs v-model="demandActiveTab" class="demand-tabs" @tab-change="onDemandTabChange">
        <el-tab-pane label="提交反馈" name="submit">
          <el-form ref="demandFormRef" :model="demandForm" :rules="demandRules" label-width="86px" class="portal-demand-form">
            <el-form-item label="需求类型" prop="demandType">
              <el-radio-group v-model="demandForm.demandType" class="demand-type-group" @change="onDemandTypeChange">
                <el-radio-button value="new_app">
                  <span class="demand-type-option">
                    <strong>希望上架应用</strong>
                    <em>补充新的内部工具</em>
                  </span>
                </el-radio-button>
                <el-radio-button value="suggestion">
                  <span class="demand-type-option">
                    <strong>现有应用建议</strong>
                    <em>优化已有工具体验</em>
                  </span>
                </el-radio-button>
              </el-radio-group>
            </el-form-item>
            <el-form-item :label="demandAppNameLabel" :prop="demandAppNameProp">
              <el-input
                v-if="demandForm.demandType === 'new_app'"
                v-model="demandForm.appName"
                :placeholder="demandAppNamePlaceholder"
                maxlength="100"
                show-word-limit
              />
              <el-select
                v-else
                v-model="demandForm.appId"
                class="suggestion-app-select"
                filterable
                remote
                clearable
                remote-show-suffix
                reserve-keyword
                teleported
                popper-class="portal-app-select-popper"
                :remote-method="searchSuggestionApps"
                :loading="suggestionAppLoading"
                :placeholder="demandAppNamePlaceholder"
                no-match-text="未找到已上线应用"
                no-data-text="请输入应用名称查询"
                @change="onSuggestionAppChange"
                @visible-change="onSuggestionAppVisibleChange"
              >
                <el-option v-for="app in suggestionApps" :key="app.appId" :label="app.appName" :value="app.appId">
                  <div class="suggestion-app-option">
                    <strong>{{ app.appName }}</strong>
                    <span>{{ app.categoryName || '未分类' }} · {{ app.version || 'latest' }}</span>
                  </div>
                </el-option>
              </el-select>
            </el-form-item>
            <el-form-item label="需求说明" prop="content">
              <el-input
                v-model="demandForm.content"
                type="textarea"
                :rows="5"
                maxlength="2000"
                show-word-limit
                placeholder="请描述你想解决的问题、使用场景，或希望改进的地方"
              />
            </el-form-item>
            <el-form-item label="联系方式" prop="contact">
              <el-input v-model="demandForm.contact" placeholder="选填，便于管理员进一步沟通" maxlength="100" show-word-limit />
            </el-form-item>
          </el-form>
        </el-tab-pane>

        <el-tab-pane label="我的反馈" name="mine">
          <div class="my-demand-head">
            <div>
              <strong>反馈记录</strong>
              <!-- <p>这里只显示你本人提交的反馈，管理员回复后会同步展示。</p> -->
            </div>
            <div class="my-demand-tools">
              <span>共 {{ myDemandTotal }} 条</span>
              <button type="button" @click="loadMyDemands">
                <el-icon><Refresh /></el-icon>
                <span>刷新</span>
              </button>
            </div>
          </div>

          <div v-loading="myDemandLoading" class="my-demand-list">
            <article v-for="item in myDemands" :key="item.demandId" class="my-demand-card">
              <header class="my-demand-card-head">
                <div>
                  <strong>{{ item.appName }}</strong>
                  <span>{{ getDemandTypeText(item.demandType) }}</span>
                </div>
                <el-tag :type="getDemandStatusTag(item.status)">{{ getDemandStatusText(item.status) }}</el-tag>
              </header>
              <p class="my-demand-content">{{ item.content }}</p>
              <div v-if="hasDemandReply(item)" class="my-demand-reply">
                <strong>管理员回复</strong>
                <p>{{ item.handleRemark }}</p>
                <time v-if="item.handledTime">{{ item.handledTime }}</time>
              </div>
              <footer class="my-demand-card-foot">
                <time>提交时间：{{ item.createTime || '-' }}</time>
                <button type="button" @click="removeMyDemand(item)">
                  <el-icon><Delete /></el-icon>
                  <span>删除</span>
                </button>
              </footer>
            </article>
            <el-empty v-if="!myDemandLoading && myDemands.length === 0" :image-size="92" description="暂无反馈记录" />
          </div>

          <el-pagination
            v-if="myDemandTotal > myDemandPageSize"
            class="my-demand-pager"
            small
            background
            layout="prev, pager, next"
            :total="myDemandTotal"
            :page-size="myDemandPageSize"
            :current-page="myDemandPageNum"
            @current-change="onMyDemandPage"
          />
        </el-tab-pane>
      </el-tabs>
      <template #footer>
        <div v-if="demandActiveTab === 'submit'" class="dialog-footer">
          <el-button @click="closeDemandDialog">取 消</el-button>
          <el-button type="primary" :loading="demandSubmitting" @click="submitDemandForm">提 交</el-button>
        </div>
        <div v-else class="dialog-footer">
          <el-button @click="closeDemandDialog">关 闭</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus';
import { Delete, DocumentAdd, Grid, House, Refresh, Search, Star } from '@element-plus/icons-vue';
import CategoryTabs from './components/CategoryTabs.vue';
import AppCard from './components/AppCard.vue';
import {
  listApps,
  listCategories,
  listFavorites,
  listMyDemands,
  deleteMyDemand,
  submitDemand,
} from '@/api/appcenter/portal';
import { PortalApp, PortalCategory, PortalDemandForm, PortalDemandItem } from '@/api/appcenter/types';
import PortalNotificationBell from '@/layout/portal/components/PortalNotificationBell.vue';
import logoUrl from '@/assets/portal/home-logo.png';

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
const demandDialogVisible = ref(false);
const demandActiveTab = ref<'submit' | 'mine'>('submit');
const demandSubmitting = ref(false);
const demandFormRef = ref<FormInstance>();
const suggestionApps = ref<PortalApp[]>([]);
const suggestionAppLoading = ref(false);
const myDemands = ref<PortalDemandItem[]>([]);
const myDemandLoading = ref(false);
const myDemandPageNum = ref(1);
const myDemandPageSize = ref(5);
const myDemandTotal = ref(0);
const router = useRouter();
let suggestionAppSearchSeq = 0;

const createDemandForm = (): PortalDemandForm => ({
  demandType: 'new_app',
  appId: undefined,
  appName: '',
  content: '',
  contact: ''
});

const demandForm = ref<PortalDemandForm>(createDemandForm());

const validateDemandAppName = (_rule: unknown, value: string, callback: (error?: Error) => void) => {
  if (demandForm.value.demandType === 'suggestion') {
    callback();
    return;
  }
  if (value?.trim()) {
    callback();
    return;
  }
  callback(new Error('请输入应用名称'));
};

const validateSuggestionApp = (_rule: unknown, value: number | undefined, callback: (error?: Error) => void) => {
  if (demandForm.value.demandType !== 'suggestion') {
    callback();
    return;
  }
  if (value) {
    callback();
    return;
  }
  callback(new Error('请选择已上线应用'));
};

const demandRules: FormRules<PortalDemandForm> = {
  demandType: [{ required: true, message: '请选择需求类型', trigger: 'change' }],
  appId: [{ validator: validateSuggestionApp, trigger: 'change' }],
  appName: [{ validator: validateDemandAppName, trigger: 'blur' }],
  content: [{ required: true, message: '请输入需求说明', trigger: 'blur' }]
};

const navItems = computed(() => [
  { key: 'market' as const, label: '应用中心', icon: Grid },
  { key: 'favorites' as const, label: '收藏应用', icon: Star }
]);

const currentToolTotal = computed(() => (viewMode.value === 'market' ? total.value : favoriteTotal.value));
const activeToolCategory = computed(() => categories.value.find((item) => item.categoryCode === categoryCode.value));
const activeToolCategoryName = computed(() => (categoryCode.value === 'all' ? '全部工具' : activeToolCategory.value?.categoryName || '当前分类'));

const pageTitle = computed(() => {
  if (viewMode.value === 'favorites') return '收藏应用';
  return '应用中心';
});

const pageSubtitle = computed(() => {
  if (viewMode.value === 'favorites') return `${activeToolCategoryName.value} · 共 ${currentToolTotal.value} 个收藏应用`;
  return `${activeToolCategoryName.value} · 共 ${currentToolTotal.value} 个工具`;
});

const demandAppNameLabel = computed(() => (demandForm.value.demandType === 'new_app' ? '应用名称' : '相关应用'));
const demandAppNameProp = computed(() => (demandForm.value.demandType === 'suggestion' ? 'appId' : 'appName'));

const demandAppNamePlaceholder = computed(() =>
  demandForm.value.demandType === 'new_app' ? '请输入希望上架的应用名称' : '请输入应用名称搜索并选择已上线应用'
);

const getDemandTypeText = (type: PortalDemandItem['demandType'] | PortalDemandForm['demandType']) => {
  if (type === 'new_app') return '希望上架应用';
  if (type === 'suggestion') return '现有应用建议';
  return '需求反馈';
};

const getDemandStatusText = (status: PortalDemandItem['status']) => {
  if (status === '0') return '待处理';
  if (status === '1') return '处理中';
  if (status === '2') return '已处理';
  if (status === '3') return '已关闭';
  return '未知';
};

const getDemandStatusTag = (status: PortalDemandItem['status']) => {
  if (status === '0') return 'warning';
  if (status === '1') return 'primary';
  if (status === '2') return 'success';
  if (status === '3') return 'info';
  return 'info';
};

const hasDemandReply = (item: PortalDemandItem) => Boolean(item.handleRemark?.trim());

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

const loadMyDemands = async () => {
  myDemandLoading.value = true;
  try {
    const res: any = await listMyDemands({
      pageNum: myDemandPageNum.value,
      pageSize: myDemandPageSize.value
    });
    myDemands.value = res.rows || [];
    myDemandTotal.value = res.total || 0;
  } finally {
    myDemandLoading.value = false;
  }
};

const searchSuggestionApps = async (query = '') => {
  const seq = ++suggestionAppSearchSeq;
  suggestionAppLoading.value = true;
  try {
    const res: any = await listApps({
      categoryCode: 'all',
      keyword: query.trim(),
      sort: 'latest',
      pageNum: 1,
      pageSize: 20
    });
    if (seq === suggestionAppSearchSeq) {
      suggestionApps.value = res.rows || res.data || [];
    }
  } finally {
    if (seq === suggestionAppSearchSeq) {
      suggestionAppLoading.value = false;
    }
  }
};

const onSuggestionAppVisibleChange = (visible: boolean) => {
  if (visible && demandForm.value.demandType === 'suggestion' && suggestionApps.value.length === 0) {
    void searchSuggestionApps('');
  }
};

const onSuggestionAppChange = (appId?: number) => {
  const selected = suggestionApps.value.find((app) => app.appId === appId);
  demandForm.value.appName = selected?.appName || '';
  if (!appId) {
    demandForm.value.appName = '';
  }
  void demandFormRef.value?.validateField('appId');
};

const onDemandTypeChange = () => {
  demandForm.value.appId = undefined;
  demandForm.value.appName = '';
  demandFormRef.value?.clearValidate(['appId', 'appName']);
  if (demandForm.value.demandType === 'suggestion') {
    void searchSuggestionApps('');
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

const resetDemandForm = () => {
  demandForm.value = createDemandForm();
  demandFormRef.value?.clearValidate();
};

const openDemandDialog = () => {
  resetDemandForm();
  demandActiveTab.value = 'submit';
  demandDialogVisible.value = true;
};

const closeDemandDialog = () => {
  demandDialogVisible.value = false;
};

const onDemandTabChange = (name: string | number) => {
  if (name === 'mine') {
    myDemandPageNum.value = 1;
    loadMyDemands();
  }
};

const onMyDemandPage = (page: number) => {
  myDemandPageNum.value = page;
  loadMyDemands();
};

const submitDemandForm = async () => {
  const formEl = demandFormRef.value;
  if (!formEl) return;
  const valid = await formEl.validate().catch(() => false);
  if (!valid) return;

  demandSubmitting.value = true;
  try {
    const selectedSuggestionApp = suggestionApps.value.find((app) => app.appId === demandForm.value.appId);
    const appName =
      demandForm.value.demandType === 'suggestion'
        ? selectedSuggestionApp?.appName || demandForm.value.appName.trim()
        : demandForm.value.appName.trim();
    const payload: PortalDemandForm = {
      demandType: demandForm.value.demandType,
      appName,
      content: demandForm.value.content.trim()
    };
    if (demandForm.value.demandType === 'suggestion') {
      payload.appId = demandForm.value.appId;
    }
    const contact = demandForm.value.contact?.trim();
    if (contact) payload.contact = contact;
    await submitDemand(payload);
    ElMessage.success('需求反馈已提交，我们会尽快处理');
    resetDemandForm();
    demandActiveTab.value = 'mine';
    myDemandPageNum.value = 1;
    await loadMyDemands();
  } finally {
    demandSubmitting.value = false;
  }
};

const removeMyDemand = async (item: PortalDemandItem) => {
  try {
    await ElMessageBox.confirm(`确认删除“${item.appName}”这条反馈吗？`, '删除反馈', {
      type: 'warning',
      confirmButtonText: '删 除',
      cancelButtonText: '取 消'
    });
    await deleteMyDemand(item.demandId);
    ElMessage.success('删除成功');
    if (myDemands.value.length === 1 && myDemandPageNum.value > 1) {
      myDemandPageNum.value -= 1;
    }
    await loadMyDemands();
  } catch {
    // 用户取消删除时不提示
  }
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
  --tool-primary: #245f8f;
  --tool-primary-deep: #183f63;
  --tool-primary-soft: #eaf2f8;
  --tool-accent: #b7791f;
  --tool-accent-soft: #fff3dc;
  --tool-title: #14243a;
  --tool-text: #32445c;
  --tool-muted: #68788c;
  --tool-weak: #96a1af;
  --tool-border: #dce5ed;
  --tool-input-border: #d3dee8;
  display: grid;
  grid-template-columns: 276px minmax(0, 1fr);
  gap: 22px;
  padding: 18px 28px 44px;
  background: linear-gradient(180deg, rgba(241, 244, 248, 0.95) 0%, rgba(247, 249, 252, 0.82) 320px), #f5f7fa;
  color: var(--tool-text);
  font-family: 'HarmonyOS Sans SC', 'PingFang SC', 'Microsoft YaHei', sans-serif;
}

.tools-sidebar {
  position: sticky;
  top: 18px;
  align-self: start;
  max-height: calc(100vh - 36px);
  min-height: 0;
  display: grid;
  grid-template-rows: auto auto minmax(0, 1fr);
  gap: 14px;
  overflow: hidden;
}

.side-brand {
  position: relative;
  height: 86px;
  min-height: 86px;
  display: flex;
  align-items: center;
  gap: 13px;
  box-sizing: border-box;
  border: 1px solid var(--tool-border);
  border-radius: 8px;
  padding: 16px;
  overflow: hidden;
  background: linear-gradient(135deg, rgba(183, 121, 31, 0.13), transparent 42%), linear-gradient(180deg, #fff 0%, #f8fafc 100%);
  box-shadow: 0 14px 34px rgba(31, 54, 76, 0.08);
}

.side-brand::after {
  content: '';
  position: absolute;
  right: -24px;
  bottom: -34px;
  width: 110px;
  height: 72px;
  border: 1px solid rgba(183, 121, 31, 0.22);
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
  color: var(--tool-title);
  font-size: 22px;
  line-height: 1.15;
  font-weight: 900;
}

.side-brand span {
  color: var(--tool-muted);
  font-size: 13px;
  font-weight: 700;
  white-space: nowrap;
}

.home-link {
  width: 100%;
  height: 40px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 7px;
  border: 1px solid var(--tool-border);
  border-radius: 8px;
  padding: 0 14px;
  background: #fff;
  color: var(--tool-text);
  line-height: 1;
  font-weight: 850;
  cursor: pointer;
  transition:
    border-color 0.18s ease,
    background 0.18s ease,
    color 0.18s ease,
    box-shadow 0.18s ease,
    transform 0.18s ease;
}

.home-link .el-icon {
  color: var(--tool-muted);
  font-size: 18px;
}

.home-link:hover {
  border-color: var(--tool-primary);
  background: var(--tool-primary-soft);
  color: var(--tool-primary);
  transform: translateY(-1px);
}

.home-link:hover .el-icon {
  color: var(--tool-primary);
}

.side-nav {
  min-height: 0;
  display: flex;
  flex-direction: column;
  gap: 10px;
  border: 1px solid var(--tool-border);
  border-radius: 8px;
  padding: 14px;
  overflow: auto;
  background: rgba(255, 255, 255, 0.94);
  box-shadow: 0 14px 34px rgba(31, 54, 76, 0.08);
}

.nav-title {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  padding: 2px 2px 6px;
}

.nav-title span {
  color: var(--tool-title);
  font-size: 15px;
  font-weight: 900;
}

.nav-title em {
  color: var(--tool-muted);
  font-size: 13px;
  font-style: normal;
  font-weight: 700;
}

.side-link {
  position: relative;
  min-height: 58px;
  display: grid;
  grid-template-columns: 34px minmax(0, 1fr);
  align-items: center;
  gap: 10px;
  border: 1px solid transparent;
  border-radius: 8px;
  padding: 12px;
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.9), rgba(248, 250, 252, 0.98)), #f8fafc;
  color: var(--tool-text);
  font-size: 15px;
  font-weight: 850;
  text-align: left;
  cursor: pointer;
  transition:
    border-color 0.18s ease,
    background 0.18s ease,
    box-shadow 0.18s ease,
    transform 0.18s ease;
}

.nav-icon {
  width: 34px;
  height: 34px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 8px;
  background: #fff;
  color: var(--tool-accent);
  box-shadow: 0 0 0 1px var(--tool-input-border) inset;
}

.side-link strong {
  min-width: 0;
  overflow: hidden;
  color: var(--tool-title);
  line-height: 1.2;
  font-weight: 850;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.side-link:hover {
  border-color: #b8c9d9;
  background: #f8fafc;
  transform: translateX(2px);
}

.side-link.active {
  border-color: #b8c9d9;
  background: var(--tool-accent-soft);
  box-shadow: inset 3px 0 0 var(--tool-accent);
}

.side-link.active .nav-icon {
  background: var(--tool-accent);
  color: #fff;
  box-shadow: 0 10px 20px rgba(183, 121, 31, 0.2);
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
  min-height: 86px;
  display: grid;
  grid-template-columns: minmax(220px, 390px) minmax(0, 1fr);
  align-items: center;
  gap: 18px;
  box-sizing: border-box;
  border: 1px solid var(--tool-border);
  border-radius: 8px;
  padding: 14px 16px 14px 20px;
  overflow: hidden;
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.98), rgba(248, 250, 252, 0.96));
  box-shadow: 0 14px 34px rgba(31, 54, 76, 0.08);
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
  font-size: 28px;
  line-height: 1.15;
  font-weight: 900;
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
  border-radius: 8px;
  background: #fff;
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
  border-radius: 8px;
  padding: 0 15px;
  background: var(--tool-primary);
  color: #fff;
  font-size: 14px;
  line-height: 1;
  font-weight: 850;
  white-space: nowrap;
  cursor: pointer;
  transition:
    background 0.18s ease,
    box-shadow 0.18s ease,
    transform 0.18s ease;
}

.search-button {
  padding: 0 16px;
}

.search-button:hover,
.demand-btn:hover {
  background: var(--tool-primary-deep);
  box-shadow: 0 8px 20px rgba(36, 95, 143, 0.18);
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
  border-radius: 8px;
  background: #fff;
  color: var(--tool-muted);
  cursor: pointer;
  box-shadow: 0 12px 26px rgba(31, 54, 76, 0.07);
  transition:
    border-color 0.16s ease,
    color 0.16s ease,
    box-shadow 0.16s ease,
    transform 0.16s ease;
}

.notification-btn:hover {
  border-color: #b8c9d9;
  color: var(--tool-primary);
  background: var(--tool-primary-soft);
  box-shadow: 0 14px 28px rgba(36, 95, 143, 0.12);
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
  background: #ff312a;
  color: #fff;
  font-size: 12px;
  line-height: 1;
  font-weight: 850;
}

:global(.tool-notification-popper) {
  border: 1px solid var(--tool-input-border) !important;
  border-radius: 12px !important;
  padding: 14px !important;
  box-shadow: 0 18px 46px rgba(31, 54, 76, 0.16) !important;
}

.notice-popover {
  color: #0f1f3d;
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

:global(.portal-demand-dialog) {
  width: min(760px, calc(100vw - 32px)) !important;
  overflow: hidden;
  border: 1px solid var(--tool-input-border);
  border-radius: 12px;
  background: #fff;
  box-shadow: 0 28px 70px rgba(31, 54, 76, 0.2);
}

:global(.portal-demand-dialog .el-dialog__header) {
  margin: 0;
  padding: 0;
}

:global(.portal-demand-dialog .el-dialog__headerbtn) {
  top: 18px;
  right: 18px;
  width: 34px;
  height: 34px;
  border-radius: 8px;
}

:global(.portal-demand-dialog .el-dialog__headerbtn:hover) {
  background: var(--tool-primary-soft);
}

:global(.portal-demand-dialog .el-dialog__body) {
  padding: 0 26px;
}

:global(.portal-demand-dialog .el-dialog__footer) {
  padding: 18px 26px 22px;
  border-top: 1px solid var(--tool-border);
  background: #f8fafc;
}

.demand-dialog-head {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 24px 28px 20px;
  border-bottom: 1px solid var(--tool-border);
  background: linear-gradient(135deg, var(--tool-accent-soft) 0%, rgba(255, 255, 255, 0.96) 58%);
}

.demand-dialog-icon {
  width: 46px;
  height: 46px;
  flex: 0 0 46px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 8px;
  background: linear-gradient(180deg, #d19a37 0%, var(--tool-accent) 100%);
  color: #fff;
  box-shadow:
    inset 0 1px 0 rgba(255, 255, 255, 0.46),
    0 14px 26px rgba(183, 121, 31, 0.22);
}

.demand-dialog-icon .el-icon {
  font-size: 24px;
}

.demand-dialog-head strong {
  display: block;
  color: var(--tool-title);
  font-size: 23px;
  line-height: 1.2;
  font-weight: 850;
}

.demand-dialog-head p {
  margin: 7px 0 0;
  color: var(--tool-muted);
  font-size: 14px;
  line-height: 1.4;
  font-weight: 650;
}

.demand-tabs {
  min-height: 492px;
  padding-top: 18px;
}

:deep(.demand-tabs .el-tabs__header) {
  margin: 0 0 18px;
}

:deep(.demand-tabs .el-tabs__nav-wrap::after),
:deep(.demand-tabs .el-tabs__active-bar) {
  display: none;
}

:deep(.demand-tabs .el-tabs__nav) {
  gap: 4px;
  padding: 4px;
  border: 1px solid var(--tool-input-border);
  border-radius: 8px;
  background: #f5f7fa;
}

:deep(.demand-tabs .el-tabs__item) {
  height: 36px;
  border-radius: 7px;
  padding: 0 18px;
  color: var(--tool-muted);
  font-size: 14px;
  font-weight: 800;
}

:deep(.demand-tabs .el-tabs__item.is-active) {
  background: var(--tool-accent);
  color: #fff;
  box-shadow: 0 8px 18px rgba(183, 121, 31, 0.2);
}

.demand-intro {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 18px;
  margin-bottom: 18px;
  padding: 14px 16px;
  border: 1px solid #dce5ed;
  border-radius: 8px;
  background: linear-gradient(135deg, #fff9ed 0%, #fff 100%);
}

.demand-intro strong {
  display: block;
  color: var(--tool-title);
  font-size: 15px;
  line-height: 1.2;
  font-weight: 850;
}

.demand-intro p {
  margin: 6px 0 0;
  color: var(--tool-muted);
  font-size: 13px;
  line-height: 1.55;
  font-weight: 650;
}

.demand-intro span {
  height: 30px;
  display: inline-flex;
  align-items: center;
  flex: 0 0 auto;
  border-radius: 999px;
  padding: 0 12px;
  background: var(--tool-accent-soft);
  color: var(--tool-accent);
  font-size: 12px;
  line-height: 1;
  font-weight: 800;
}

.portal-demand-form {
  padding: 2px 2px 0 0;
}

:deep(.portal-demand-form .el-form-item) {
  margin-bottom: 18px;
}

:deep(.portal-demand-form .el-form-item__label) {
  color: var(--tool-text);
  font-size: 14px;
  font-weight: 800;
}

:deep(.portal-demand-form .el-input__wrapper) {
  min-height: 42px;
  border-radius: 8px;
  background: #fff;
  box-shadow: 0 0 0 1px var(--tool-input-border) inset;
}

:deep(.portal-demand-form .el-input__wrapper.is-focus) {
  box-shadow:
    0 0 0 1px var(--tool-primary) inset,
    0 8px 18px rgba(36, 95, 143, 0.08);
}

.suggestion-app-select {
  width: 100%;
}

:deep(.suggestion-app-select .el-select__wrapper) {
  min-height: 42px;
  border-radius: 8px;
  background: #fff;
  box-shadow: 0 0 0 1px var(--tool-input-border) inset;
}

:deep(.suggestion-app-select .el-select__wrapper.is-focused) {
  box-shadow:
    0 0 0 1px var(--tool-primary) inset,
    0 8px 18px rgba(36, 95, 143, 0.08);
}

:deep(.portal-demand-form .el-input__inner),
:deep(.portal-demand-form .el-textarea__inner) {
  color: var(--tool-text);
  font-size: 14px;
  font-weight: 650;
}

:deep(.portal-demand-form .el-textarea__inner) {
  min-height: 142px !important;
  border-radius: 8px;
  padding: 12px 14px;
  box-shadow: 0 0 0 1px var(--tool-input-border) inset;
  resize: none;
}

:deep(.portal-demand-form .el-textarea__inner:focus) {
  box-shadow:
    0 0 0 1px var(--tool-primary) inset,
    0 8px 18px rgba(36, 95, 143, 0.08);
}

.demand-type-group {
  width: 100%;
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
}

:deep(.demand-type-group .el-radio-button) {
  min-width: 0;
}

:deep(.demand-type-group .el-radio-button__inner) {
  width: 100%;
  height: auto;
  display: block;
  border: 1px solid var(--tool-input-border);
  border-radius: 8px;
  padding: 13px 15px;
  background: #fff;
  color: var(--tool-text);
  text-align: left;
  box-shadow: none;
}

:deep(.demand-type-group .el-radio-button:first-child .el-radio-button__inner) {
  border-left: 1px solid var(--tool-input-border);
}

:deep(.demand-type-group .el-radio-button.is-active .el-radio-button__inner) {
  border-color: var(--tool-accent);
  background: var(--tool-accent-soft);
  color: var(--tool-accent);
  box-shadow: 0 0 0 1px var(--tool-accent);
}

.demand-type-option {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.demand-type-option strong {
  font-size: 14px;
  line-height: 1.2;
  font-weight: 850;
}

.demand-type-option em {
  color: #6b7a99;
  font-size: 12px;
  line-height: 1.2;
  font-style: normal;
  font-weight: 650;
}

:deep(.demand-type-group .el-radio-button.is-active .demand-type-option em) {
  color: var(--tool-accent);
}

:global(.portal-app-select-popper) {
  border: 1px solid var(--tool-input-border) !important;
  border-radius: 8px !important;
  box-shadow: 0 18px 42px rgba(31, 54, 76, 0.16) !important;
}

:global(.portal-app-select-popper .el-select-dropdown__item) {
  height: auto;
  padding: 9px 12px;
}

:global(.portal-app-select-popper .el-select-dropdown__item.is-selected) {
  color: var(--tool-primary);
}

.suggestion-app-option {
  display: flex;
  flex-direction: column;
  gap: 5px;
  padding: 2px 0;
}

.suggestion-app-option strong {
  color: var(--tool-title);
  font-size: 14px;
  line-height: 1.2;
  font-weight: 850;
}

.suggestion-app-option span {
  color: #6b7a99;
  font-size: 12px;
  line-height: 1.2;
  font-weight: 650;
}

.my-demand-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 18px;
  margin-bottom: 14px;
  padding: 14px 16px;
  border: 1px solid #dce5ed;
  border-radius: 8px;
  background: linear-gradient(135deg, #fff9ed 0%, #fff 100%);
}

.my-demand-head strong {
  display: block;
  color: var(--tool-title);
  font-size: 15px;
  line-height: 1.2;
  font-weight: 850;
}

.my-demand-head p {
  margin: 6px 0 0;
  color: var(--tool-muted);
  font-size: 13px;
  line-height: 1.55;
  font-weight: 650;
}

.my-demand-tools {
  display: flex;
  align-items: center;
  gap: 10px;
  flex: 0 0 auto;
}

.my-demand-tools > span {
  color: var(--tool-muted);
  font-size: 13px;
  font-weight: 750;
  white-space: nowrap;
}

.my-demand-tools button {
  height: 34px;
  display: inline-flex;
  align-items: center;
  gap: 6px;
  border: 1px solid var(--tool-input-border);
  border-radius: 8px;
  padding: 0 12px;
  background: #fff;
  color: var(--tool-primary);
  font-size: 13px;
  font-weight: 750;
  white-space: nowrap;
  cursor: pointer;
}

.my-demand-tools button:hover {
  border-color: #b8c9d9;
  background: #f8fafc;
}

.my-demand-list {
  min-height: 332px;
  max-height: 430px;
  display: flex;
  flex-direction: column;
  gap: 12px;
  overflow-y: auto;
  padding: 2px 4px 2px 0;
}

.my-demand-card {
  border: 1px solid var(--tool-border);
  border-radius: 8px;
  padding: 15px 16px;
  background: linear-gradient(180deg, #fff 0%, #fafbf7 100%);
  box-shadow: 0 8px 22px rgba(31, 54, 76, 0.06);
}

.my-demand-card-head,
.my-demand-card-foot {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 14px;
}

.my-demand-card-head strong {
  display: block;
  color: var(--tool-title);
  font-size: 16px;
  line-height: 1.35;
  font-weight: 850;
}

.my-demand-card-head span {
  display: block;
  margin-top: 4px;
  color: #6b7a99;
  font-size: 13px;
  line-height: 1.2;
  font-weight: 650;
}

.my-demand-content {
  margin: 12px 0 0;
  border-left: 3px solid #d7c4a7;
  padding-left: 12px;
  color: var(--tool-text);
  font-size: 14px;
  line-height: 1.65;
  font-weight: 600;
  white-space: pre-wrap;
}

.my-demand-reply {
  margin-top: 12px;
  border: 1px solid #d7c4a7;
  border-radius: 8px;
  padding: 11px 12px;
  background: #f8f3eb;
  color: var(--tool-text);
}

.my-demand-reply strong {
  display: block;
  color: var(--tool-primary);
  font-size: 13px;
  line-height: 1.2;
  font-weight: 850;
}

.my-demand-reply p {
  margin: 7px 0 0;
  font-size: 13px;
  line-height: 1.6;
  font-weight: 650;
  white-space: pre-wrap;
}

.my-demand-reply time {
  display: block;
  margin-top: 7px;
  color: #7c8ca7;
  font-size: 12px;
  line-height: 1.2;
}

.my-demand-card-foot {
  margin-top: 12px;
  padding-top: 12px;
  border-top: 1px solid var(--tool-border);
}

.my-demand-card-foot time {
  color: #7c8ca7;
  font-size: 12px;
  line-height: 1.2;
  font-weight: 650;
}

.my-demand-card-foot button {
  height: 30px;
  display: inline-flex;
  align-items: center;
  gap: 5px;
  border: 1px solid #ffd1d1;
  border-radius: 7px;
  padding: 0 12px;
  background: #fff;
  color: #d93026;
  font-size: 13px;
  font-weight: 750;
  cursor: pointer;
}

.my-demand-card-foot button:hover {
  background: #fff6f6;
}

.my-demand-pager {
  margin-top: 14px;
  justify-content: center;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}

:deep(.dialog-footer .el-button) {
  height: 38px;
  min-width: 86px;
  border-radius: 8px;
  font-weight: 800;
}

:deep(.dialog-footer .el-button--primary) {
  border-color: var(--tool-primary);
  background: var(--tool-primary);
  box-shadow: 0 10px 22px rgba(36, 95, 143, 0.2);
}

@media (max-width: 520px) {
  :global(.portal-demand-dialog .el-dialog__body) {
    padding: 0 16px;
  }

  :global(.portal-demand-dialog .el-dialog__footer) {
    padding: 14px 16px 18px;
  }

  .demand-dialog-head {
    align-items: flex-start;
    padding: 24px 32px 20px;
  }

  .demand-dialog-head strong {
    font-size: 22px;
  }

  .demand-dialog-head p {
    font-size: 13px;
    line-height: 1.45;
  }

  .demand-intro {
    align-items: flex-start;
    flex-direction: column;
    gap: 10px;
  }

  .portal-demand-form {
    padding-right: 0;
  }

  :deep(.portal-demand-form .el-form-item) {
    display: block;
    margin-bottom: 16px;
  }

  :deep(.portal-demand-form .el-form-item__label) {
    width: auto !important;
    height: auto;
    display: block;
    margin-bottom: 8px;
    padding: 0;
    line-height: 1.2;
    text-align: left;
  }

  :deep(.portal-demand-form .el-form-item__content) {
    margin-left: 0 !important;
  }

  .demand-type-group {
    grid-template-columns: 1fr;
  }

  .my-demand-head {
    align-items: flex-start;
    flex-direction: column;
    gap: 12px;
  }

  .my-demand-tools {
    width: 100%;
    justify-content: space-between;
  }

  .my-demand-card-head,
  .my-demand-card-foot {
    align-items: flex-start;
    flex-direction: column;
    gap: 10px;
  }
}

:deep(.pager .btn-prev),
:deep(.pager .btn-next),
:deep(.pager .el-pager li) {
  min-width: 38px;
  height: 38px;
  border-radius: 8px;
  background: transparent;
  color: var(--tool-title);
  font-size: 16px;
  font-weight: 750;
}

:deep(.pager .el-pager li.is-active) {
  background: var(--tool-primary);
  color: #fff;
  box-shadow: 0 10px 22px rgba(36, 95, 143, 0.22);
}

:deep(.pager .btn-prev),
:deep(.pager .btn-next) {
  background: #fff;
  box-shadow: 0 8px 20px rgba(31, 54, 76, 0.08);
}

@media (max-width: 1720px) {
  .app-grid {
    grid-template-columns: repeat(4, minmax(0, 1fr));
  }
}

@media (max-width: 1500px) {
  .app-grid {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
}

@media (max-width: 1360px) {
  .tools-market {
    grid-template-columns: 256px minmax(0, 1fr);
    padding-inline: 22px;
  }
}

@media (max-width: 1120px) {
  .side-brand {
    height: 86px;
    min-height: 86px;
  }

  .side-brand strong {
    font-size: 22px;
  }

  .home-link {
    padding-inline: 16px;
  }

  .side-nav {
    padding: 14px;
  }

  .side-link {
    min-height: 58px;
  }

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

  .app-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 980px) {
  .tools-market {
    grid-template-columns: 1fr;
    padding: 16px;
  }

  .tools-sidebar {
    position: static;
    max-height: none;
  }

  .side-brand {
    height: 86px;
    min-height: 86px;
  }

  .side-nav {
    display: grid;
    grid-template-columns: 1fr;
    gap: 10px;
    max-height: none;
  }

  .home-link {
    width: 100%;
    justify-content: center;
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

<template>
  <div class="resources-app">
    <ResourceSidebar
      :category-tree="categoryTree"
      :selected-categories="selectedCategories"
      :load-error="categoryLoadError"
      @update:selected-categories="changeSelectedCategories"
      @retry="loadCategoryTree"
    />

    <main class="resource-main">
      <ResourceHeader
        v-model:keyword="keyword"
        :title="resourcePageTitle"
        :subtitle="resourcePageSubtitle"
        @search="reloadWithFacets"
        @upload="openCreateDialog"
        @mine="openMyResources"
      />

      <section class="resource-content">
        <ResourceToolbar
          :total="total"
          :sort="sort"
          :preview-type="previewType"
          :uploaded-within="uploadedWithin"
          :size-range="sizeRange"
          :display-mode="displayMode"
          @update:sort="changeSort"
          @update:preview-type="changePreviewType"
          @update:uploaded-within="changeUploadedWithin"
          @update:size-range="changeSizeRange"
          @update:display-mode="displayMode = $event"
        />

        <ResourceCategoryChips :chips="selectedChips" @remove="removeChip" />

        <div class="resource-results" :aria-busy="loading">
          <ResourceLoadingSkeleton v-if="showSkeleton" :mode="displayMode" />

          <div v-else-if="loadError" class="result-error" role="alert">
            <strong>资料加载失败</strong>
            <span>请检查网络连接后重新加载，已有筛选条件会继续保留。</span>
            <button type="button" @click="reload">重新加载</button>
          </div>

          <div v-else-if="displayMode === 'grid' && resources.length > 0" class="resource-grid">
            <ResourceCard
              v-for="item in resources"
              :key="item.resourceId"
              :resource="item"
              @preview="openPreview"
              @download="openDownload"
              @favorite="toggleFavorite"
            />
          </div>
          <ResourceList
            v-else-if="resources.length > 0"
            :resources="resources"
            @preview="openPreview"
            @download="openDownload"
            @favorite="toggleFavorite"
          />

          <el-empty v-else-if="!loading" description="暂无资料">
            <button v-if="selectedCategories.length > 0 || committedKeyword" class="empty-action" type="button" @click="clearAllFilters">
              清除筛选
            </button>
          </el-empty>
        </div>
      </section>

      <el-pagination
        v-if="total > PAGE_SIZE_OPTIONS[0]"
        class="pager"
        background
        :layout="paginationLayout"
        :pager-count="mobileViewport ? 5 : 7"
        :total="total"
        :page-size="pageSize"
        :page-sizes="[...PAGE_SIZE_OPTIONS]"
        :current-page="pageNum"
        @current-change="onPage"
        @size-change="onPageSize"
      />
    </main>

    <MyResourcesDrawer
      v-model="myResourcesVisible"
      :resources="myResources"
      :total="myResourcesTotal"
      :loading="myResourcesLoading"
      :active-tab="myResourceTab"
      @change-tab="changeMyResourceTab"
      @preview="openPreview"
      @download="openDownload"
      @edit="openEditDialog"
      @replace="openReplaceDialog"
      @status="changeOwnStatus"
      @delete="deleteOwnResource"
      @favorite="toggleFavorite"
      @upload="openCreateDialog"
    />

    <ResourceUploadDialog
      v-model="uploadVisible"
      :category-tree="categoryTree"
      :resource="editingResource"
      :mode="uploadMode"
      :submitting="uploading"
      :progress="uploadProgress"
      @submit="submitResource"
    />
  </div>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage, ElMessageBox } from 'element-plus';
import { useUserStore } from '@/store/modules/user';
import { getToken } from '@/utils/auth';
import {
  changePortalResourceStatus,
  createPortalResource,
  deletePortalResource,
  favoritePortalResource,
  getResourceCategoryTree,
  listResources,
  unfavoritePortalResource,
  updatePortalResource,
  uploadPortalResourceFile
} from '@/api/portal/resources';
import type { CategoryTreeNode, InfoResource, ResourcePortalPayload, ResourceUploadProgress, ResourceUploadResult } from '@/api/infoservice/types';
import { downloadPortalResource } from './download';
import { buildSelectedChips, encodeCategoryCodes, removeCategory, resolveSelectionTitle } from './categoryFacets';
import { createLatestRequestGuard } from './resourceRequestState';
import { normalizePageSize, PAGE_SIZE_OPTIONS, persistPageSize, readStoredPageSize } from '../pageSizing';
import MyResourcesDrawer from './components/MyResourcesDrawer.vue';
import ResourceCard from './components/ResourceCard.vue';
import ResourceCategoryChips from './components/ResourceCategoryChips.vue';
import ResourceHeader from './components/ResourceHeader.vue';
import ResourceList from './components/ResourceList.vue';
import ResourceLoadingSkeleton from './components/ResourceLoadingSkeleton.vue';
import ResourceSidebar from './components/ResourceSidebar.vue';
import ResourceToolbar from './components/ResourceToolbar.vue';
import ResourceUploadDialog from './components/ResourceUploadDialog.vue';

type DisplayMode = 'grid' | 'list';
type UploadMode = 'create' | 'edit';
type MyResourceTab = 'uploads' | 'favorites' | 'downloads' | 'history';
type ResourceSubmitPayload = {
  title: string;
  categoryIds: Array<number | string>;
  description: string;
  files?: File[];
};

const router = useRouter();
const userStore = useUserStore();
const categoryTree = ref<CategoryTreeNode[]>([]);
const resources = ref<InfoResource[]>([]);
const myResources = ref<InfoResource[]>([]);
const editingResource = ref<InfoResource>();
const keyword = ref('');
const displayMode = ref<DisplayMode>('grid');
const uploadMode = ref<UploadMode>('create');
const myResourceTab = ref<MyResourceTab>('uploads');
const selectedCategories = ref<string[]>([]);
const previewType = ref('all');
const uploadedWithin = ref('all');
const sizeRange = ref('all');
const sort = ref('latest');
const PAGE_SIZE_STORAGE_KEY = 'ip-resources-page-size';

const pageNum = ref(1);
const pageSize = ref(readStoredPageSize(PAGE_SIZE_STORAGE_KEY));
const total = ref(0);
const myResourcesTotal = ref(0);
const loading = ref(false);
const showSkeleton = ref(false);
const loadError = ref('');
const categoryLoadError = ref('');
const mobileViewport = ref(false);
const myResourcesLoading = ref(false);
const myResourcesVisible = ref(false);
const uploadVisible = ref(false);
const uploading = ref(false);
const uploadProgress = ref<ResourceUploadProgress[]>([]);

const isLoggedIn = computed(() => Boolean(userStore.token || getToken()));
const resourcePageTitle = computed(() => resolveSelectionTitle(categoryTree.value, selectedCategories.value));
const resourcePageSubtitle = computed(() => `当前筛选共 ${total.value} 条资料`);
const selectedChips = computed(() => buildSelectedChips(categoryTree.value, selectedCategories.value));
const paginationLayout = computed(() => (mobileViewport.value ? 'prev, pager, next' : 'total, sizes, prev, pager, next, jumper'));

const ensureLogin = () => {
  if (isLoggedIn.value) {
    return true;
  }
  ElMessage.warning('请先登录后操作');
  return false;
};

/** 已提交的搜索关键词：勾选/排序/翻页只读它，避免输入半截的词被隐式带入列表而计数还是旧口径 */
const committedKeyword = ref('');

const listGuard = createLatestRequestGuard();
const treeGuard = createLatestRequestGuard();
let skeletonTimer: ReturnType<typeof setTimeout> | undefined;
let mobileQuery: MediaQueryList | undefined;

const syncMobileViewport = (event?: MediaQueryListEvent) => {
  mobileViewport.value = event?.matches ?? mobileQuery?.matches ?? false;
};

/** 分面计数树：同步已提交关键词+工具条筛选，但不含分类勾选自身（标准分面语义） */
const loadCategoryTree = async () => {
  const seq = treeGuard.begin();
  categoryLoadError.value = '';
  try {
    const res: any = await getResourceCategoryTree({
      keyword: committedKeyword.value,
      previewType: previewType.value,
      uploadedWithin: uploadedWithin.value,
      sizeRange: sizeRange.value
    });
    if (treeGuard.isCurrent(seq)) {
      categoryTree.value = res.data || [];
    }
  } catch (error) {
    if (treeGuard.isCurrent(seq)) {
      categoryLoadError.value = '栏目刷新失败';
    }
    console.error('[resources] 栏目加载失败', error);
  }
};

const reload = async () => {
  const seq = listGuard.begin();
  clearTimeout(skeletonTimer);
  loadError.value = '';
  loading.value = true;
  showSkeleton.value = false;
  skeletonTimer = setTimeout(() => {
    if (listGuard.isCurrent(seq)) showSkeleton.value = true;
  }, 300);
  try {
    const res: any = await listResources({
      scope: 'public',
      categoryCode: encodeCategoryCodes(selectedCategories.value),
      keyword: committedKeyword.value,
      previewType: previewType.value,
      uploadedWithin: uploadedWithin.value,
      sizeRange: sizeRange.value,
      sort: sort.value,
      pageNum: pageNum.value,
      pageSize: pageSize.value
    });
    if (!listGuard.isCurrent(seq)) {
      return;
    }
    resources.value = res.rows || [];
    total.value = res.total || 0;
    // 筛少后停留在越界深页码时钳回最后一页重取，防止“有总数却整页为空且分页器隐藏”
    const lastPage = Math.max(1, Math.ceil(total.value / pageSize.value));
    if (resources.value.length === 0 && total.value > 0 && pageNum.value > lastPage) {
      pageNum.value = lastPage;
      await reload();
    }
  } catch (error) {
    if (listGuard.isCurrent(seq)) {
      loadError.value = '资料加载失败';
    }
    console.error('[resources] 资料加载失败', error);
  } finally {
    if (listGuard.isCurrent(seq)) {
      clearTimeout(skeletonTimer);
      loading.value = false;
      showSkeleton.value = false;
    }
  }
};

const reloadFirst = () => {
  pageNum.value = 1;
  reload();
};

/** 关键词/工具条筛选变化：提交关键词，列表与分面计数并行刷新 */
const reloadWithFacets = async () => {
  committedKeyword.value = keyword.value;
  pageNum.value = 1;
  await Promise.all([reload(), loadCategoryTree()]);
};

const clearAllFilters = () => {
  keyword.value = '';
  committedKeyword.value = '';
  selectedCategories.value = [];
  previewType.value = 'all';
  uploadedWithin.value = 'all';
  sizeRange.value = 'all';
  sort.value = 'latest';
  pageNum.value = 1;
  void Promise.all([reload(), loadCategoryTree()]);
};

const loadMyResources = async () => {
  if (myResourceTab.value !== 'uploads' && myResourceTab.value !== 'favorites') {
    myResources.value = [];
    myResourcesTotal.value = 0;
    return;
  }
  if (!ensureLogin()) {
    return;
  }
  myResourcesLoading.value = true;
  try {
    const res: any = await listResources({
      scope: myResourceTab.value === 'favorites' ? 'favorites' : 'mine',
      sort: 'latest',
      status: 'all',
      pageNum: 1,
      pageSize: 50
    });
    myResources.value = res.rows || [];
    myResourcesTotal.value = res.total || 0;
  } finally {
    myResourcesLoading.value = false;
  }
};

const openMyResources = async () => {
  if (!ensureLogin()) {
    return;
  }
  myResourcesVisible.value = true;
  myResourceTab.value = 'uploads';
  await loadMyResources();
};

const changeMyResourceTab = async (tab: MyResourceTab) => {
  myResourceTab.value = tab;
  await loadMyResources();
};

/** 勾选分类只刷列表不刷计数（分面语义） */
const changeSelectedCategories = (next: string[]) => {
  selectedCategories.value = next;
  reloadFirst();
};

const removeChip = (code: string) => {
  changeSelectedCategories(removeCategory(selectedCategories.value, code));
};

const changePreviewType = (value: string) => {
  previewType.value = value;
  reloadWithFacets();
};

const changeUploadedWithin = (value: string) => {
  uploadedWithin.value = value;
  reloadWithFacets();
};

const changeSizeRange = (value: string) => {
  sizeRange.value = value;
  reloadWithFacets();
};

const changeSort = (value: string) => {
  sort.value = value;
  reloadFirst();
};

const onPage = (page: number) => {
  pageNum.value = page;
  reload();
};

const onPageSize = (size: number) => {
  pageSize.value = normalizePageSize(size);
  persistPageSize(PAGE_SIZE_STORAGE_KEY, pageSize.value);
  pageNum.value = 1;
  reload();
};

const openPreview = (resource: InfoResource) => {
  const route = router.resolve({ name: 'InfoResourcePreview', params: { resourceId: resource.resourceId } });
  window.open(route.href, '_blank');
};

const openDownload = (resource: InfoResource) => {
  downloadPortalResource(resource);
};

const updateResourceFavoriteState = (resourceId: number | string, favorited: boolean) => {
  const update = (item: InfoResource) => {
    if (String(item.resourceId) !== String(resourceId)) return;
    const currentCount = item.favoriteCount || 0;
    item.favorited = favorited;
    item.favoriteCount = favorited ? currentCount + 1 : Math.max(currentCount - 1, 0);
  };
  resources.value.forEach(update);
  myResources.value.forEach(update);
};

const toggleFavorite = async (resource: InfoResource) => {
  if (!ensureLogin()) {
    return;
  }
  const nextFavorited = !resource.favorited;
  if (nextFavorited) {
    await favoritePortalResource(resource.resourceId);
  } else {
    await unfavoritePortalResource(resource.resourceId);
  }
  updateResourceFavoriteState(resource.resourceId, nextFavorited);
  ElMessage.success(nextFavorited ? '已收藏' : '已取消收藏');
  if (myResourcesVisible.value && myResourceTab.value === 'favorites') {
    await loadMyResources();
  }
};

const openCreateDialog = () => {
  if (!ensureLogin()) {
    return;
  }
  uploadMode.value = 'create';
  editingResource.value = undefined;
  uploadProgress.value = [];
  uploadVisible.value = true;
};

const openEditDialog = (resource: InfoResource) => {
  if (!ensureLogin()) {
    return;
  }
  uploadMode.value = 'edit';
  editingResource.value = resource;
  uploadProgress.value = [];
  uploadVisible.value = true;
};

const openReplaceDialog = (resource: InfoResource) => {
  openEditDialog(resource);
};

/** 不可变更新进度列表中的一项 */
const patchProgress = (index: number, patch: Partial<ResourceUploadProgress>) => {
  uploadProgress.value = uploadProgress.value.map((item, i) => (i === index ? { ...item, ...patch } : item));
};

const uploadFile = async (file: File, progressIndex?: number) => {
  const formData = new FormData();
  formData.append('file', file);
  const upRes: any = await uploadPortalResourceFile(formData, (percent) => {
    if (progressIndex != null) {
      // 100% 表示传输完成，进入服务端处理段（OSS 写入/落库），由调用方置 processing
      patchProgress(progressIndex, { percent, status: percent >= 100 ? 'processing' : 'uploading' });
    }
  });
  return upRes.data as ResourceUploadResult;
};

const buildPayload = (base: { title: string; categoryIds: Array<number | string>; description: string }, file?: ResourceUploadResult) => {
  const currentResource = editingResource.value;
  return {
    title: base.title,
    // categoryId=主分类（首个），与后端 Bo 校验及旧展示路径兼容；categoryIds 为全量
    categoryId: base.categoryIds[0],
    categoryIds: [...base.categoryIds],
    description: base.description,
    ossId: file?.ossId ?? currentResource?.ossId,
    originalName: file?.originalName ?? currentResource?.originalName,
    fileSuffix: file?.fileSuffix ?? currentResource?.fileSuffix,
    mimeType: file?.mimeType ?? currentResource?.mimeType,
    fileSize: file?.fileSize ?? currentResource?.fileSize,
    previewType: file?.previewType ?? currentResource?.previewType,
    status: currentResource?.status || '0'
  } as ResourcePortalPayload;
};

const fileTitle = (payload: ResourceSubmitPayload, file: File, total: number) => {
  const title = payload.title?.trim();
  if (total === 1 && title) {
    return title;
  }
  return file.name.replace(/\.[^.]+$/, '') || title || '未命名资料';
};

const refreshOwnedViews = async () => {
  await Promise.all([loadCategoryTree(), reload()]);
  if (myResourcesVisible.value) {
    await loadMyResources();
  }
};

/** 任一环节失败：把仍在途的行标红为 failed，用户可见可重试（弹窗保持打开） */
const markActiveProgressFailed = () => {
  uploadProgress.value = uploadProgress.value.map((item) => (item.status === 'done' ? item : { ...item, status: 'failed' as const }));
};

const submitResource = async (payload: ResourceSubmitPayload) => {
  uploading.value = true;
  const files = payload.files || [];
  uploadProgress.value = files.map((file) => ({ name: file.name, percent: 0, status: 'pending' as const }));
  try {
    if (uploadMode.value === 'edit' && editingResource.value) {
      const uploaded = files[0] ? await uploadFile(files[0], 0) : undefined;
      await updatePortalResource(editingResource.value.resourceId, buildPayload(payload, uploaded));
      if (files[0]) {
        patchProgress(0, { percent: 100, status: 'done' });
      }
      ElMessage.success('资料已保存');
    } else {
      if (files.length === 0) {
        ElMessage.warning('请选择文件');
        return;
      }
      for (const [index, file] of files.entries()) {
        const uploaded = await uploadFile(file, index);
        await createPortalResource(
          buildPayload(
            {
              title: fileTitle(payload, file, files.length),
              categoryIds: payload.categoryIds,
              description: payload.description
            },
            uploaded
          )
        );
        patchProgress(index, { percent: 100, status: 'done' });
      }
      ElMessage.success(files.length === 1 ? '资料已发布，可在我的资料中管理' : `已发布 ${files.length} 份资料，可在我的资料中管理`);
    }
    uploadVisible.value = false;
    await refreshOwnedViews();
  } catch (error) {
    // 请求层错误提示由拦截器统一弹出；这里负责把进度行标红，避免"永远处理中"的假卡死
    markActiveProgressFailed();
    console.error('[resources] 上传/保存失败', error);
  } finally {
    uploading.value = false;
  }
};

const changeOwnStatus = async (resource: InfoResource, status: string) => {
  await changePortalResourceStatus(resource.resourceId, status);
  ElMessage.success(status === '0' ? '资料已发布' : '资料已下架');
  await refreshOwnedViews();
};

const deleteOwnResource = async (resource: InfoResource) => {
  try {
    await ElMessageBox.confirm(`确定删除“${resource.title}”吗？`, '删除资料', {
      confirmButtonText: '删除',
      cancelButtonText: '取消',
      type: 'warning'
    });
  } catch {
    return;
  }
  await deletePortalResource(resource.resourceId);
  ElMessage.success('资料已删除');
  await refreshOwnedViews();
};

onMounted(async () => {
  mobileQuery = window.matchMedia('(max-width: 767px)');
  syncMobileViewport();
  mobileQuery.addEventListener('change', syncMobileViewport);
  await Promise.all([loadCategoryTree(), reload()]);
});

onBeforeUnmount(() => {
  clearTimeout(skeletonTimer);
  mobileQuery?.removeEventListener('change', syncMobileViewport);
});
</script>

<style scoped>
.resources-app {
  min-height: 100vh;
  display: grid;
  grid-template-columns: 220px minmax(0, 1fr);
  gap: 24px;
  padding: 24px 32px 48px;
  background: linear-gradient(180deg, var(--ip-neutral-100), var(--ip-neutral-50));
  color: var(--ip-neutral-700);
}

.resources-app :deep(.el-input__wrapper),
.resources-app :deep(.el-select__wrapper) {
  border-radius: var(--ip-radius-sm);
  background: var(--ip-neutral-0);
  box-shadow: 0 0 0 1px var(--ip-neutral-300) inset;
}

.resources-app :deep(.el-input__wrapper:hover),
.resources-app :deep(.el-select__wrapper:hover),
.resources-app :deep(.el-input__wrapper.is-focus),
.resources-app :deep(.el-select__wrapper.is-focused) {
  box-shadow: 0 0 0 1px var(--ip-primary-600) inset;
}

.resources-app :deep(.el-input__inner),
.resources-app :deep(.el-select__placeholder) {
  color: var(--ip-neutral-700);
  font-weight: 500;
}

.resources-app :deep(.el-input__inner::placeholder) {
  color: var(--ip-neutral-400);
}

.resource-main {
  min-width: 0;
  display: grid;
  align-content: start;
  gap: 16px;
}

.resource-content {
  min-height: calc(100vh - 150px);
  border: 1px solid var(--ip-neutral-200);
  border-radius: var(--ip-radius-md);
  overflow: hidden;
  background: var(--ip-neutral-0);
  box-shadow: var(--ip-shadow-sm);
}

.resource-results {
  min-height: 360px;
  padding: 24px;
}

.resource-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 16px;
}

.result-error {
  min-height: 240px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-direction: column;
  gap: 12px;
  color: var(--ip-neutral-600);
  text-align: center;
}

.result-error strong {
  color: var(--ip-neutral-900);
  font-size: var(--ip-font-emphasis);
  font-weight: 700;
}
.result-error span {
  font-size: var(--ip-font-body);
}
.result-error button,
.empty-action {
  min-height: 36px;
  border: 1px solid var(--ip-primary-300);
  border-radius: var(--ip-radius-sm);
  padding: 0 16px;
  background: var(--ip-neutral-0);
  color: var(--ip-primary-700);
  font-size: var(--ip-font-body);
  font-weight: 600;
  cursor: pointer;
}
.result-error button:hover,
.empty-action:hover {
  border-color: var(--ip-primary-600);
  background: var(--ip-primary-50);
}

.pager {
  display: flex;
  justify-content: center;
  padding: 0;
}

.resources-app :deep(.el-pagination.is-background .el-pager li.is-active) {
  background: var(--ip-primary-600);
}

@media (max-width: 1199px) {
  .resources-app {
    padding-inline: 24px;
  }

  .resource-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 980px) {
  .resources-app {
    grid-template-columns: 1fr;
    gap: 16px;
    padding: 16px;
  }

  .resource-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 767px) {
  .resources-app {
    padding: 12px;
  }
  .resource-results {
    padding: 16px;
  }
  .resource-grid {
    grid-template-columns: 1fr;
  }
  .pager {
    --el-pagination-button-width: 44px;
  }
  .result-error button,
  .empty-action {
    min-height: 44px;
  }
}
</style>

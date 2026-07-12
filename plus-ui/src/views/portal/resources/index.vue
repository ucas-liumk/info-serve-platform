<template>
  <div class="resources-app">
    <ResourceSidebar :category-tree="categoryTree" :selected-categories="selectedCategories" @update:selected-categories="changeSelectedCategories" />

    <main class="resource-main">
      <header class="resource-topbar">
        <div class="title-block">
          <h1>{{ resourcePageTitle }}</h1>
          <p>{{ resourcePageSubtitle }}</p>
        </div>

        <div class="top-actions">
          <div class="search-box">
            <el-input
              v-model="keyword"
              class="resource-search"
              clearable
              placeholder="搜索资源标题、关键词、作者、标签等"
              size="large"
              @keyup.enter="reloadWithFacets"
              @clear="reloadWithFacets"
            >
              <template #prefix>
                <el-icon><Search /></el-icon>
              </template>
            </el-input>
            <button class="search-button" type="button" @click="reloadWithFacets">搜索</button>
          </div>

          <PortalNotificationBell />
          <button class="upload-button" type="button" @click="openCreateDialog">
            <el-icon><UploadFilled /></el-icon>
            <span>上传资料</span>
          </button>
          <button class="mine-button" type="button" @click="openMyResources">
            <el-icon><User /></el-icon>
            <span>我的资源</span>
          </button>
        </div>
      </header>

      <section v-loading="loading" class="resource-content">
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

        <div class="resource-results">
          <div v-if="displayMode === 'grid'" class="resource-grid">
            <ResourceCard
              v-for="item in resources"
              :key="item.resourceId"
              :resource="item"
              @preview="openPreview"
              @download="openDownload"
              @favorite="toggleFavorite"
            />
          </div>
          <ResourceList v-else :resources="resources" @preview="openPreview" @download="openDownload" @favorite="toggleFavorite" />

          <el-empty v-if="!loading && resources.length === 0" description="暂无资料" />
        </div>
      </section>

      <el-pagination
        v-if="total > PAGE_SIZE_OPTIONS[0]"
        class="pager"
        background
        layout="total, sizes, prev, pager, next, jumper"
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
import { computed, onMounted, ref } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage, ElMessageBox } from 'element-plus';
import { Search, UploadFilled, User } from '@element-plus/icons-vue';
import { useUserStore } from '@/store/modules/user';
import { getToken } from '@/utils/auth';
import PortalNotificationBell from '@/layout/portal/components/PortalNotificationBell.vue';
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
import { DEFAULT_PAGE_SIZE, normalizePageSize, PAGE_SIZE_OPTIONS } from './pageSizing';
import MyResourcesDrawer from './components/MyResourcesDrawer.vue';
import ResourceCard from './components/ResourceCard.vue';
import ResourceCategoryChips from './components/ResourceCategoryChips.vue';
import ResourceList from './components/ResourceList.vue';
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

/** 页大小记忆：仅接受既定档位，读写异常（私密模式等）静默回退默认档 */
const readStoredPageSize = (): number => {
  try {
    return normalizePageSize(window.localStorage.getItem(PAGE_SIZE_STORAGE_KEY));
  } catch {
    return DEFAULT_PAGE_SIZE;
  }
};

const persistPageSize = (size: number) => {
  try {
    window.localStorage.setItem(PAGE_SIZE_STORAGE_KEY, String(size));
  } catch {
    /* 私密模式下不持久化 */
  }
};

const pageNum = ref(1);
const pageSize = ref(readStoredPageSize());
const total = ref(0);
const myResourcesTotal = ref(0);
const loading = ref(false);
const myResourcesLoading = ref(false);
const myResourcesVisible = ref(false);
const uploadVisible = ref(false);
const uploading = ref(false);
const uploadProgress = ref<ResourceUploadProgress[]>([]);

const isLoggedIn = computed(() => Boolean(userStore.token || getToken()));
const resourcePageTitle = computed(() => resolveSelectionTitle(categoryTree.value, selectedCategories.value));
const resourcePageSubtitle = computed(() => `当前筛选共 ${total.value} 条资料`);
const selectedChips = computed(() => buildSelectedChips(categoryTree.value, selectedCategories.value));

const ensureLogin = () => {
  if (isLoggedIn.value) {
    return true;
  }
  ElMessage.warning('请先登录后操作');
  return false;
};

/** 已提交的搜索关键词：勾选/排序/翻页只读它，避免输入半截的词被隐式带入列表而计数还是旧口径 */
const committedKeyword = ref('');

/** 请求序号：丢弃过期响应，防止快速连点勾选时旧结果覆盖新结果 */
let reloadSeq = 0;
let treeSeq = 0;

/** 分面计数树：同步已提交关键词+工具条筛选，但不含分类勾选自身（标准分面语义） */
const loadCategoryTree = async () => {
  const seq = ++treeSeq;
  try {
    const res: any = await getResourceCategoryTree({
      keyword: committedKeyword.value,
      previewType: previewType.value,
      uploadedWithin: uploadedWithin.value,
      sizeRange: sizeRange.value
    });
    if (seq === treeSeq) {
      categoryTree.value = res.data || [];
    }
  } catch {
    // 计数树刷新失败保留旧数据，避免左栏闪空；错误提示由全局请求拦截器兜底
  }
};

const reload = async () => {
  const seq = ++reloadSeq;
  loading.value = true;
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
    if (seq !== reloadSeq) {
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
  } finally {
    if (seq === reloadSeq) {
      loading.value = false;
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
  await Promise.all([reload(), loadCategoryTree()]).catch(() => {
    // 列表失败由全局拦截器提示；此处仅防未处理的 Promise 拒绝
  });
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
  persistPageSize(pageSize.value);
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
  uploadVisible.value = true;
};

const openEditDialog = (resource: InfoResource) => {
  if (!ensureLogin()) {
    return;
  }
  uploadMode.value = 'edit';
  editingResource.value = resource;
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
      ElMessage.success(files.length === 1 ? '资料已发布，可在我的资源中管理' : `已发布 ${files.length} 份资料，可在我的资源中管理`);
    }
    uploadVisible.value = false;
    await refreshOwnedViews();
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
  await Promise.all([loadCategoryTree(), reload()]);
});
</script>

<style scoped>
.resources-app {
  min-height: 100vh;
  --resource-primary: #245f8f;
  --resource-primary-deep: #183f63;
  --resource-primary-soft: #eaf2f8;
  --resource-accent: #2f8a7a;
  --resource-accent-soft: #e7f4f0;
  --resource-title: #14243a;
  --resource-text: #32445c;
  --resource-muted: #68788c;
  --resource-weak: #96a1af;
  --resource-border: #dce5ed;
  --resource-input-border: #d3dee8;
  display: grid;
  grid-template-columns: 276px minmax(0, 1fr);
  gap: 22px;
  padding: 18px 28px 44px;
  background: linear-gradient(180deg, rgba(241, 244, 248, 0.95) 0%, rgba(247, 249, 252, 0.82) 320px), #f5f7fa;
  color: var(--resource-text);
  font-family: 'HarmonyOS Sans SC', 'PingFang SC', 'Microsoft YaHei', sans-serif;
}

.resources-app :deep(.el-input__wrapper),
.resources-app :deep(.el-select__wrapper) {
  border-radius: 8px;
  background: #fff;
  box-shadow: 0 0 0 1px var(--resource-input-border) inset;
}

.resources-app :deep(.el-input__wrapper:hover),
.resources-app :deep(.el-select__wrapper:hover),
.resources-app :deep(.el-input__wrapper.is-focus),
.resources-app :deep(.el-select__wrapper.is-focused) {
  box-shadow: 0 0 0 1px var(--resource-primary) inset;
}

.resources-app :deep(.el-input__inner),
.resources-app :deep(.el-select__placeholder) {
  color: var(--resource-text);
  font-weight: 650;
}

.resources-app :deep(.el-input__inner::placeholder) {
  color: var(--resource-muted);
}

.upload-button,
.mine-button,
.search-button {
  height: 40px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 7px;
  border-radius: 8px;
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

.resource-main {
  min-width: 0;
  display: grid;
  align-content: start;
  gap: 12px;
}

.resource-topbar {
  position: relative;
  min-height: 86px;
  display: grid;
  grid-template-columns: minmax(220px, 390px) minmax(0, 1fr);
  align-items: center;
  gap: 18px;
  box-sizing: border-box;
  border: 1px solid var(--resource-border);
  border-radius: 8px;
  padding: 14px 16px 14px 20px;
  overflow: hidden;
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.98), rgba(248, 250, 252, 0.96));
  box-shadow: 0 14px 34px rgba(31, 54, 76, 0.08);
}

.resource-topbar::before {
  content: '';
  position: absolute;
  inset: 0 auto 0 0;
  width: 4px;
  background: var(--resource-accent);
  pointer-events: none;
}

.title-block {
  position: relative;
  z-index: 1;
  min-width: 0;
}

.title-block h1 {
  margin: 0;
  color: var(--resource-title);
  font-size: 28px;
  line-height: 1.15;
  font-weight: 900;
}

.title-block p {
  margin: 6px 0 0;
  overflow: hidden;
  color: var(--resource-muted);
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

.resource-search {
  min-width: 0;
  --el-input-height: 40px;
}

:deep(.resource-search .el-input__wrapper) {
  padding: 0 14px;
  border-radius: 8px;
}

:deep(.resource-search .el-input__inner) {
  font-size: 14px;
}

.search-button {
  border: 1px solid var(--resource-primary);
  padding: 0 16px;
  background: var(--resource-primary);
  color: #fff;
}

.search-button:hover,
.upload-button:hover {
  background: var(--resource-primary-deep);
  box-shadow: 0 8px 20px rgba(36, 95, 143, 0.18);
  transform: translateY(-1px);
}

.upload-button {
  flex: 0 0 auto;
  border: 1px solid var(--resource-primary);
  padding: 0 15px;
  background: var(--resource-primary);
  color: #fff;
}

.mine-button {
  flex: 0 0 auto;
  border: 1px solid var(--resource-input-border);
  padding: 0 15px;
  background: #fff;
  color: var(--resource-text);
}

.mine-button:hover {
  border-color: var(--resource-primary);
  background: var(--resource-primary-soft);
  color: var(--resource-primary);
  box-shadow: 0 8px 20px rgba(36, 95, 143, 0.1);
  transform: translateY(-1px);
}

.resource-content {
  min-height: calc(100vh - 150px);
  border: 1px solid var(--resource-border);
  border-radius: 8px;
  overflow: hidden;
  background: rgba(255, 255, 255, 0.96);
  box-shadow: 0 14px 34px rgba(31, 54, 76, 0.08);
}

.resource-results {
  min-height: 360px;
  padding: 16px 18px 14px;
}

.resource-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 18px;
}

.pager {
  display: flex;
  justify-content: center;
  padding: 0;
}

.resources-app :deep(.el-pagination.is-background .el-pager li.is-active) {
  background: var(--resource-primary);
}

@media (max-width: 1360px) {
  .resources-app {
    grid-template-columns: 256px minmax(0, 1fr);
    padding-inline: 22px;
  }

  .resource-grid {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }

  .resource-topbar {
    grid-template-columns: 1fr;
  }

  .top-actions {
    justify-content: flex-start;
  }
}

@media (max-width: 980px) {
  .resources-app {
    grid-template-columns: 1fr;
    padding: 16px;
  }

  .resource-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 760px) {
  .top-actions,
  .search-box {
    width: 100%;
  }

  .top-actions {
    align-items: stretch;
    flex-direction: column;
  }

  .search-box {
    min-width: 0;
    grid-template-columns: 1fr;
  }

  .upload-button,
  .mine-button,
  .search-button {
    width: 100%;
  }

  .resource-grid {
    grid-template-columns: 1fr;
  }
}
</style>

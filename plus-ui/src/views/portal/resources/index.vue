<template>
  <div class="resources-app">
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

      <ResourceFilterPanel :categories="categories" :total="categoryTotal" :category-code="categoryCode" @change-category="changeCategory" />
    </aside>

    <main class="resource-main">
      <header class="resource-topbar">
        <div class="title-block">
          <h1>资源共享</h1>
          <p>聚合制度、模板、文档与多媒体资料</p>
        </div>

        <div class="top-actions">
          <div class="search-box">
            <el-input
              v-model="keyword"
              class="resource-search"
              clearable
              placeholder="搜索资源标题、关键词、作者、标签等"
              size="large"
              @keyup.enter="reloadFirst"
              @clear="reloadFirst"
            >
              <template #prefix>
                <el-icon><Search /></el-icon>
              </template>
            </el-input>
            <button class="search-button" type="button" @click="reloadFirst">搜索</button>
          </div>

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

        <div class="resource-results">
          <div v-if="displayMode === 'grid'" class="resource-grid">
            <ResourceCard v-for="item in resources" :key="item.resourceId" :resource="item" @preview="openPreview" @download="openDownload" />
          </div>
          <ResourceList v-else :resources="resources" @preview="openPreview" @download="openDownload" />

          <el-empty v-if="!loading && resources.length === 0" description="暂无资料" />
        </div>
      </section>

      <el-pagination
        v-if="total > pageSize"
        class="pager"
        background
        layout="prev, pager, next, jumper, total"
        :total="total"
        :page-size="pageSize"
        :current-page="pageNum"
        @current-change="onPage"
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
      @upload="openCreateDialog"
    />

    <ResourceUploadDialog
      v-model="uploadVisible"
      :categories="categories"
      :resource="editingResource"
      :mode="uploadMode"
      :submitting="uploading"
      @submit="submitResource"
    />
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage, ElMessageBox } from 'element-plus';
import { House, Search, UploadFilled, User } from '@element-plus/icons-vue';
import { PORTAL_HOME_PATH } from '@/constants/router';
import { useUserStore } from '@/store/modules/user';
import { getToken } from '@/utils/auth';
import {
  changePortalResourceStatus,
  createPortalResource,
  deletePortalResource,
  listResourceCategories,
  listResources,
  resourceDownloadUrl,
  updatePortalResource,
  uploadPortalResourceFile
} from '@/api/infoservice/portal';
import type { InfoResource, ResourceCategory, ResourcePortalPayload, ResourceUploadResult } from '@/api/infoservice/types';
import MyResourcesDrawer from './components/MyResourcesDrawer.vue';
import ResourceCard from './components/ResourceCard.vue';
import ResourceFilterPanel from './components/ResourceFilterPanel.vue';
import ResourceList from './components/ResourceList.vue';
import ResourceToolbar from './components/ResourceToolbar.vue';
import ResourceUploadDialog from './components/ResourceUploadDialog.vue';

type DisplayMode = 'grid' | 'list';
type UploadMode = 'create' | 'edit';
type MyResourceTab = 'uploads' | 'favorites' | 'downloads' | 'history';
type ResourceSubmitPayload = {
  title: string;
  categoryId: number | string | undefined;
  description: string;
  files?: File[];
};

const router = useRouter();
const userStore = useUserStore();
const categories = ref<ResourceCategory[]>([]);
const resources = ref<InfoResource[]>([]);
const myResources = ref<InfoResource[]>([]);
const editingResource = ref<InfoResource>();
const keyword = ref('');
const displayMode = ref<DisplayMode>('grid');
const uploadMode = ref<UploadMode>('create');
const myResourceTab = ref<MyResourceTab>('uploads');
const categoryCode = ref('all');
const previewType = ref('all');
const uploadedWithin = ref('all');
const sizeRange = ref('all');
const sort = ref('latest');
const pageNum = ref(1);
const pageSize = ref(12);
const total = ref(0);
const myResourcesTotal = ref(0);
const loading = ref(false);
const myResourcesLoading = ref(false);
const myResourcesVisible = ref(false);
const uploadVisible = ref(false);
const uploading = ref(false);

const isLoggedIn = computed(() => Boolean(userStore.token || getToken()));
const categoryTotal = computed(() => categories.value.reduce((sum, item) => sum + (item.resourceCount || 0), 0));

const ensureLogin = () => {
  if (isLoggedIn.value) {
    return true;
  }
  ElMessage.warning('请先登录后操作');
  return false;
};

const goPortalHome = () => {
  router.push(PORTAL_HOME_PATH);
};

const loadCategories = async () => {
  const res: any = await listResourceCategories();
  categories.value = res.data || [];
};

const reload = async () => {
  loading.value = true;
  try {
    const res: any = await listResources({
      scope: 'public',
      categoryCode: categoryCode.value,
      keyword: keyword.value,
      previewType: previewType.value,
      uploadedWithin: uploadedWithin.value,
      sizeRange: sizeRange.value,
      sort: sort.value,
      pageNum: pageNum.value,
      pageSize: pageSize.value
    });
    resources.value = res.rows || [];
    total.value = res.total || 0;
  } finally {
    loading.value = false;
  }
};

const reloadFirst = () => {
  pageNum.value = 1;
  reload();
};

const loadMyResources = async () => {
  if (myResourceTab.value !== 'uploads') {
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
      scope: 'mine',
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

const changeCategory = (code: string) => {
  categoryCode.value = code;
  reloadFirst();
};

const changePreviewType = (value: string) => {
  previewType.value = value;
  reloadFirst();
};

const changeUploadedWithin = (value: string) => {
  uploadedWithin.value = value;
  reloadFirst();
};

const changeSizeRange = (value: string) => {
  sizeRange.value = value;
  reloadFirst();
};

const changeSort = (value: string) => {
  sort.value = value;
  reloadFirst();
};

const onPage = (page: number) => {
  pageNum.value = page;
  reload();
};

const withAuth = (url: string) => {
  const target = new URL(url, window.location.origin);
  const token = getToken();
  if (token) target.searchParams.set('Authorization', `Bearer ${token}`);
  target.searchParams.set('clientid', import.meta.env.VITE_APP_CLIENT_ID);
  return target.toString();
};

const openPreview = (resource: InfoResource) => {
  const route = router.resolve({ name: 'InfoResourcePreview', params: { resourceId: resource.resourceId } });
  window.open(route.href, '_blank');
};

const openDownload = (resource: InfoResource) => {
  window.open(withAuth(resourceDownloadUrl(resource.resourceId)), '_blank');
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

const uploadFile = async (file: File) => {
  const formData = new FormData();
  formData.append('file', file);
  const upRes: any = await uploadPortalResourceFile(formData);
  return upRes.data as ResourceUploadResult;
};

const buildPayload = (base: { title: string; categoryId: number | string | undefined; description: string }, file?: ResourceUploadResult) => {
  const currentResource = editingResource.value;
  return {
    title: base.title,
    categoryId: base.categoryId,
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
  await loadCategories();
  await reload();
  if (myResourcesVisible.value) {
    await loadMyResources();
  }
};

const submitResource = async (payload: ResourceSubmitPayload) => {
  uploading.value = true;
  try {
    const files = payload.files || [];
    if (uploadMode.value === 'edit' && editingResource.value) {
      const uploaded = files[0] ? await uploadFile(files[0]) : undefined;
      await updatePortalResource(editingResource.value.resourceId, buildPayload(payload, uploaded));
      ElMessage.success('资料已保存');
    } else {
      if (files.length === 0) {
        ElMessage.warning('请选择文件');
        return;
      }
      for (const file of files) {
        const uploaded = await uploadFile(file);
        await createPortalResource(
          buildPayload(
            {
              title: fileTitle(payload, file, files.length),
              categoryId: payload.categoryId,
              description: payload.description
            },
            uploaded
          )
        );
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
  await loadCategories();
  await reload();
});
</script>

<style scoped>
.resources-app {
  min-height: 100vh;
  --resource-primary: #1260e8;
  --resource-primary-deep: #0f55cf;
  --resource-primary-soft: #edf4ff;
  --resource-title: #0b1833;
  --resource-text: #25395f;
  --resource-muted: #53668f;
  --resource-weak: #8a97af;
  --resource-border: #e1e9f6;
  --resource-input-border: #dbe5f4;
  display: grid;
  grid-template-columns: 276px minmax(0, 1fr);
  gap: 22px;
  padding: 18px 28px 44px;
  background: linear-gradient(180deg, rgba(237, 244, 255, 0.9) 0%, rgba(247, 250, 255, 0.72) 320px), #f7faff;
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
  min-height: 82px;
  display: flex;
  align-items: center;
  gap: 13px;
  border: 1px solid var(--resource-border);
  border-radius: 8px;
  padding: 16px;
  background: #fff;
  box-shadow: 0 14px 34px rgba(35, 83, 151, 0.07);
}

.side-brand img {
  width: 42px;
  height: 42px;
  object-fit: contain;
}

.side-brand div {
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
}

.home-button,
.upload-button,
.mine-button,
.search-button {
  height: 40px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 7px;
  border-radius: 8px;
  font-weight: 850;
  cursor: pointer;
  transition:
    border-color 0.18s ease,
    background 0.18s ease,
    color 0.18s ease,
    box-shadow 0.18s ease;
}

.home-button {
  width: 100%;
  border: 1px solid var(--resource-border);
  background: #fff;
  color: var(--resource-text);
}

.home-button:hover {
  border-color: var(--resource-primary);
  background: var(--resource-primary-soft);
  color: var(--resource-primary);
}

.resource-main {
  min-width: 0;
  display: grid;
  align-content: start;
  gap: 16px;
}

.resource-topbar {
  min-height: 74px;
  display: grid;
  grid-template-columns: minmax(220px, 360px) minmax(0, 1fr);
  align-items: center;
  gap: 18px;
  border: 1px solid var(--resource-border);
  border-radius: 8px;
  padding: 14px 16px 14px 20px;
  background: rgba(255, 255, 255, 0.92);
  box-shadow: 0 14px 34px rgba(35, 83, 151, 0.07);
}

.title-block {
  min-width: 0;
}

.title-block h1 {
  margin: 0;
  color: var(--resource-title);
  font-size: 27px;
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
  box-shadow: 0 8px 20px rgba(18, 96, 232, 0.18);
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
}

.resource-content {
  min-height: calc(100vh - 150px);
  border: 1px solid var(--resource-border);
  border-radius: 8px;
  overflow: hidden;
  background: #fff;
  box-shadow: 0 14px 34px rgba(35, 83, 151, 0.07);
}

.resource-results {
  min-height: 520px;
  padding: 18px;
}

.resource-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 16px;
}

.pager {
  display: flex;
  justify-content: center;
  padding: 4px 0 0;
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
    grid-template-columns: repeat(2, minmax(0, 1fr));
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

  .resource-sidebar {
    position: static;
    max-height: none;
  }

  .resource-sidebar :deep(.filter-panel) {
    max-height: none;
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

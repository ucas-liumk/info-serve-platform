<template>
  <div class="resources">
    <PortalHeader
      v-model="keyword"
      title="资源中心"
      subtitle="知识汇聚，共享价值"
      search-placeholder="搜索标题、简介或文件名"
      @search="reloadFirst"
    />

    <main class="resources-shell">
      <section class="resources-command">
        <div class="view-switch">
          <button :class="{ active: activeView === 'center' }" type="button" @click="switchView('center')">
            <strong>资源中心</strong>
            <span>公开资料</span>
          </button>
          <button :class="{ active: activeView === 'mine' }" type="button" @click="switchView('mine')">
            <strong>我的资料</strong>
            <span>上传管理</span>
          </button>
        </div>
        <div class="command-actions">
          <button class="ghost-btn" type="button" @click="reload">
            <el-icon><Refresh /></el-icon>
            刷新
          </button>
          <button class="upload-btn" type="button" @click="openCreateDialog">
            <el-icon><UploadFilled /></el-icon>
            上传资料
          </button>
        </div>
      </section>

      <div v-if="activeView === 'center'" class="resource-workspace">
        <ResourceFilterPanel
          :categories="categories"
          :total="categoryTotal"
          :category-code="categoryCode"
          :preview-type="previewType"
          :uploaded-within="uploadedWithin"
          @change-category="changeCategory"
          @update:preview-type="changePreviewType"
          @update:uploaded-within="changeUploadedWithin"
        />

        <section v-loading="loading" class="resource-content">
          <ResourceToolbar
            :total="total"
            :sort="sort"
            :size-range="sizeRange"
            :display-mode="displayMode"
            active-view="center"
            @update:sort="changeSort"
            @update:size-range="changeSizeRange"
            @update:display-mode="displayMode = $event"
          />

          <div class="resource-results">
            <div v-if="displayMode === 'grid'" class="resource-grid">
              <ResourceCard v-for="item in resources" :key="item.resourceId" :resource="item" @detail="showDetail" @download="openDownload" />
            </div>
            <ResourceList v-else :resources="resources" @detail="showDetail" @download="openDownload" />

            <el-empty v-if="!loading && resources.length === 0" description="暂无资料" />
          </div>
        </section>
      </div>

      <section v-else v-loading="loading" class="mine-workspace">
        <header class="mine-head">
          <div>
            <p>我的资料</p>
            <h2>{{ total }} 份资料</h2>
          </div>
          <div class="mine-filters">
            <el-select v-model="mineStatus" class="mine-select" size="large" @change="reloadFirst">
              <el-option label="全部状态" value="all" />
              <el-option label="已发布" value="0" />
              <el-option label="已下架" value="1" />
            </el-select>
            <el-select v-model="sort" class="mine-select" size="large" @change="reloadFirst">
              <el-option label="最新上传" value="latest" />
              <el-option label="下载最多" value="downloads" />
              <el-option label="浏览最多" value="views" />
            </el-select>
          </div>
        </header>

        <MyResourceTable
          v-if="resources.length > 0"
          :resources="resources"
          @detail="showDetail"
          @preview="openPreview"
          @download="openDownload"
          @edit="openEditDialog"
          @replace="openReplaceDialog"
          @status="changeOwnStatus"
          @delete="deleteOwnResource"
        />
        <el-empty v-if="!loading && resources.length === 0" description="暂无上传资料" />
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

    <ResourceDetailDrawer v-model="detailVisible" :resource="current" @preview="openPreview" @download="openDownload" @edit="openEditDialog" />

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
import { ElMessage, ElMessageBox } from 'element-plus';
import { Refresh, UploadFilled } from '@element-plus/icons-vue';
import PortalHeader from '@/layout/portal/components/PortalHeader.vue';
import { useUserStore } from '@/store/modules/user';
import { getToken } from '@/utils/auth';
import {
  changePortalResourceStatus,
  createPortalResource,
  deletePortalResource,
  getResource,
  listResourceCategories,
  listResources,
  resourceDownloadUrl,
  resourcePreviewUrl,
  updatePortalResource,
  uploadPortalResourceFile
} from '@/api/infoservice/portal';
import type { InfoResource, ResourceCategory, ResourcePortalPayload, ResourceUploadResult } from '@/api/infoservice/types';
import ResourceCard from './components/ResourceCard.vue';
import ResourceDetailDrawer from './components/ResourceDetailDrawer.vue';
import ResourceFilterPanel from './components/ResourceFilterPanel.vue';
import ResourceList from './components/ResourceList.vue';
import ResourceToolbar from './components/ResourceToolbar.vue';
import ResourceUploadDialog from './components/ResourceUploadDialog.vue';
import MyResourceTable from './components/MyResourceTable.vue';

type ActiveView = 'center' | 'mine';
type DisplayMode = 'grid' | 'list';
type UploadMode = 'create' | 'edit';

const userStore = useUserStore();
const categories = ref<ResourceCategory[]>([]);
const resources = ref<InfoResource[]>([]);
const current = ref<InfoResource>();
const editingResource = ref<InfoResource>();
const keyword = ref('');
const activeView = ref<ActiveView>('center');
const displayMode = ref<DisplayMode>('grid');
const uploadMode = ref<UploadMode>('create');
const categoryCode = ref('all');
const previewType = ref('all');
const uploadedWithin = ref('all');
const sizeRange = ref('all');
const mineStatus = ref('all');
const sort = ref('latest');
const pageNum = ref(1);
const pageSize = ref(12);
const total = ref(0);
const loading = ref(false);
const detailVisible = ref(false);
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

const loadCategories = async () => {
  const res: any = await listResourceCategories();
  categories.value = res.data || [];
};

const reload = async () => {
  if (activeView.value === 'mine' && !ensureLogin()) {
    return;
  }
  loading.value = true;
  try {
    const res: any = await listResources({
      scope: activeView.value === 'mine' ? 'mine' : 'public',
      categoryCode: activeView.value === 'center' ? categoryCode.value : 'all',
      keyword: keyword.value,
      previewType: activeView.value === 'center' ? previewType.value : 'all',
      uploadedWithin: activeView.value === 'center' ? uploadedWithin.value : 'all',
      sizeRange: sizeRange.value,
      sort: sort.value,
      status: activeView.value === 'mine' ? mineStatus.value : undefined,
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

const switchView = (view: ActiveView) => {
  if (view === 'mine' && !ensureLogin()) {
    return;
  }
  activeView.value = view;
  pageSize.value = view === 'mine' ? 10 : 12;
  reloadFirst();
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

const showDetail = async (resource: InfoResource) => {
  const res: any = await getResource(resource.resourceId);
  current.value = res.data || resource;
  detailVisible.value = true;
  if (activeView.value === 'center') {
    reload();
  }
};

const withAuth = (url: string) => {
  const target = new URL(url, window.location.origin);
  const token = getToken();
  if (token) target.searchParams.set('Authorization', token);
  target.searchParams.set('clientid', import.meta.env.VITE_APP_CLIENT_ID);
  return target.toString();
};

const openPreview = (resource: InfoResource) => {
  window.open(withAuth(resourcePreviewUrl(resource.resourceId)), '_blank');
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

const submitResource = async (payload: { title: string; categoryId: number | string | undefined; description: string; file?: File }) => {
  uploading.value = true;
  try {
    const uploaded = payload.file ? await uploadFile(payload.file) : undefined;
    if (uploadMode.value === 'edit' && editingResource.value) {
      await updatePortalResource(editingResource.value.resourceId, buildPayload(payload, uploaded));
      ElMessage.success('资料已保存');
    } else {
      if (!uploaded) {
        ElMessage.warning('请选择文件');
        return;
      }
      await createPortalResource(buildPayload(payload, uploaded));
      ElMessage.success(activeView.value === 'mine' ? '资料已发布' : '资料已发布，可在我的资料中管理');
    }
    uploadVisible.value = false;
    await loadCategories();
    reloadFirst();
  } finally {
    uploading.value = false;
  }
};

const changeOwnStatus = async (resource: InfoResource, status: string) => {
  await changePortalResourceStatus(resource.resourceId, status);
  ElMessage.success(status === '0' ? '资料已发布' : '资料已下架');
  await loadCategories();
  reloadFirst();
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
  await loadCategories();
  reloadFirst();
};

onMounted(async () => {
  await loadCategories();
  await reload();
});
</script>

<style scoped>
.resources {
  min-height: 100vh;
  --resource-primary: #1260e8;
  --resource-primary-soft: #edf4ff;
  --resource-title: #0b1833;
  --resource-text: #25395f;
  --resource-muted: #53668f;
  --resource-weak: #8a97af;
  --resource-border: #e1e9f6;
  --resource-input-border: #dbe5f4;
  background: linear-gradient(180deg, rgba(237, 244, 255, 0.78) 0%, rgba(247, 250, 255, 0) 360px), #f7faff;
  color: var(--resource-text);
  font-family: 'HarmonyOS Sans SC', 'PingFang SC', 'Microsoft YaHei', sans-serif;
}

.resources-shell {
  width: min(1400px, calc(100% - 64px));
  margin: 0 auto;
  padding: 0 0 32px;
}

.resources :deep(.el-input__wrapper),
.resources :deep(.el-select__wrapper) {
  border-radius: 8px;
  background: #fff;
  box-shadow: 0 0 0 1px var(--resource-input-border) inset;
}

.resources :deep(.el-input__wrapper:hover),
.resources :deep(.el-select__wrapper:hover),
.resources :deep(.el-input__wrapper.is-focus),
.resources :deep(.el-select__wrapper.is-focused) {
  box-shadow: 0 0 0 1px var(--resource-primary) inset;
}

.resources :deep(.el-input__inner),
.resources :deep(.el-select__placeholder) {
  color: var(--resource-text);
  font-weight: 650;
}

.resources :deep(.el-input__inner::placeholder) {
  color: var(--resource-muted);
}

.resources-command {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 20px;
}

.view-switch {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  border: 1px solid var(--resource-border);
  border-radius: 8px;
  padding: 4px;
  background: #fff;
  box-shadow: 0 8px 24px rgba(11, 24, 51, 0.05);
}

.view-switch button {
  min-width: 126px;
  height: 46px;
  display: grid;
  align-content: center;
  gap: 2px;
  border: 0;
  border-radius: 7px;
  padding: 0 14px;
  background: transparent;
  color: var(--resource-muted);
  text-align: left;
  cursor: pointer;
}

.view-switch button strong {
  color: inherit;
  font-size: 15px;
  font-weight: 850;
}

.view-switch button span {
  font-size: 12px;
  font-weight: 650;
}

.view-switch button.active {
  background: var(--resource-primary);
  color: #fff;
  box-shadow: 0 6px 14px rgba(18, 96, 232, 0.2);
}

.command-actions {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.upload-btn,
.ghost-btn {
  height: 40px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 7px;
  border-radius: 8px;
  padding: 0 14px;
  font-weight: 800;
  cursor: pointer;
}

.upload-btn {
  border: 0;
  background: var(--resource-primary);
  color: #fff;
  box-shadow: 0 8px 20px rgba(18, 96, 232, 0.18);
}

.ghost-btn {
  border: 1px solid var(--resource-input-border);
  background: #fff;
  color: var(--resource-text);
}

.upload-btn:hover {
  background: #0f55cf;
}

.ghost-btn:hover {
  border-color: var(--resource-primary);
  color: var(--resource-primary);
  background: var(--resource-primary-soft);
}

.resource-workspace {
  display: grid;
  grid-template-columns: 248px minmax(0, 1fr);
  gap: 20px;
  align-items: start;
}

.resource-content,
.mine-workspace {
  min-height: 560px;
  display: grid;
  gap: 16px;
}

.resource-results {
  min-height: 460px;
}

.resource-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 16px;
}

.mine-workspace {
  border: 1px solid var(--resource-border);
  border-radius: 8px;
  padding: 16px;
  background: #fff;
  box-shadow: 0 8px 24px rgba(11, 24, 51, 0.05);
}

.mine-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.mine-head p {
  margin: 0 0 4px;
  color: var(--resource-muted);
  font-size: 12px;
  font-weight: 650;
}

.mine-head h2 {
  margin: 0;
  color: var(--resource-title);
  font-size: 20px;
  line-height: 1.2;
  font-weight: 850;
}

.mine-filters {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.mine-select {
  width: 132px;
}

.pager {
  display: flex;
  justify-content: center;
  padding: 22px 0 0;
}

.resources :deep(.el-pagination.is-background .el-pager li.is-active) {
  background: var(--resource-primary);
}

@media (max-width: 1280px) {
  .resource-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 960px) {
  .resource-workspace {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 760px) {
  .resources-shell {
    width: calc(100% - 32px);
  }

  .resources-command,
  .mine-head {
    align-items: stretch;
    flex-direction: column;
  }

  .view-switch,
  .command-actions {
    width: 100%;
  }

  .view-switch button {
    flex: 1;
    min-width: 0;
  }

  .command-actions button {
    flex: 1;
  }

  .resource-grid {
    grid-template-columns: 1fr;
  }
}
</style>

<template>
  <div class="resource-page">
    <div class="resource-actions">
      <!-- ↓ 原 topbar 的搜索框节点原样粘贴（keyword 绑定与回车/清空事件不改） -->
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

      <!-- ↓ 原 topbar 的上传按钮节点原样粘贴（打开 ResourceUploadDialog 的 handler 不改） -->
      <button class="upload-button" type="button" @click="openCreateDialog">
        <el-icon><UploadFilled /></el-icon>
        <span>上传资料</span>
      </button>
    </div>

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
          <ResourceCard
            v-for="item in resources"
            :key="item.resourceId"
            :resource="item"
            :manageable="scope === 'mine'"
            @preview="openPreview"
            @download="openDownload"
            @favorite="toggleFavorite"
            @edit="openEditDialog"
            @replace="openReplaceDialog"
            @status="changeOwnStatus"
            @delete="deleteOwnResource"
          />
        </div>
        <ResourceList
          v-else
          :resources="resources"
          :manageable="scope === 'mine'"
          @preview="openPreview"
          @download="openDownload"
          @favorite="toggleFavorite"
          @edit="openEditDialog"
          @replace="openReplaceDialog"
          @status="changeOwnStatus"
          @delete="deleteOwnResource"
        />

        <el-empty v-if="!loading && resources.length === 0" :description="emptyText" />
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
import { computed, onMounted, ref, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { ElMessage, ElMessageBox } from 'element-plus';
import { Search, UploadFilled } from '@element-plus/icons-vue';
import { useUserStore } from '@/store/modules/user';
import { getToken } from '@/utils/auth';
import {
  changePortalResourceStatus,
  createPortalResource,
  deletePortalResource,
  favoritePortalResource,
  listResourceCategories,
  listResources,
  unfavoritePortalResource,
  updatePortalResource,
  uploadPortalResourceFile
} from '@/api/portal/resources';
import type { InfoResource, ResourceCategory, ResourcePortalPayload, ResourceUploadResult } from '@/api/infoservice/types';
import { downloadPortalResource } from './download';
import ResourceCard from './components/ResourceCard.vue';
import ResourceList from './components/ResourceList.vue';
import ResourceToolbar from './components/ResourceToolbar.vue';
import ResourceUploadDialog from './components/ResourceUploadDialog.vue';

type DisplayMode = 'grid' | 'list';
type UploadMode = 'create' | 'edit';
type ResourceSubmitPayload = {
  title: string;
  categoryId: number | string | undefined;
  description: string;
  files?: File[];
};

const route = useRoute();
const router = useRouter();
const userStore = useUserStore();
const categories = ref<ResourceCategory[]>([]);
const resources = ref<InfoResource[]>([]);
const editingResource = ref<InfoResource>();
const keyword = ref('');
const displayMode = ref<DisplayMode>('grid');
const uploadMode = ref<UploadMode>('create');
const categoryCode = ref('all');
const previewType = ref('all');
const uploadedWithin = ref('all');
const sizeRange = ref('all');
const sort = ref('latest');
const pageNum = ref(1);
const pageSize = ref(15);
const total = ref(0);
const loading = ref(false);
const uploadVisible = ref(false);
const uploading = ref(false);

const isLoggedIn = computed(() => Boolean(userStore.token || getToken()));

// scope：public（默认）| mine | favorites —— 唯一来源是 URL
const scope = computed(() => {
  const s = route.query.scope as string;
  return s === 'mine' || s === 'favorites' ? s : 'public';
});

const emptyText = computed(() => {
  if (scope.value === 'mine') return '还没有上传过资料';
  if (scope.value === 'favorites') return '还没有收藏资料';
  return '暂无资料';
});

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
  loading.value = true;
  try {
    const res: any = await listResources({
      scope: scope.value,
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

const openPreview = (resource: InfoResource) => {
  const target = router.resolve({ name: 'InfoResourcePreview', params: { resourceId: resource.resourceId } });
  window.open(target.href, '_blank');
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
  // 收藏视图（scope=favorites）取消收藏后即时移出列表——原抽屉刷新的等价迁移
  if (scope.value === 'favorites') {
    await reload();
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

// scope/category 唯一来源是 URL query：初始化与变更都从这里读，首屏只请求一次
watch(
  () => [route.query.scope, route.query.category],
  () => {
    categoryCode.value = (route.query.category as string) || 'all';
    reloadFirst();
  },
  { immediate: true }
);

onMounted(() => {
  loadCategories();
});
</script>

<style scoped>
.resource-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.resource-actions {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
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
  border-radius: var(--ip-radius-sm);
}

:deep(.resource-search .el-input__inner) {
  font-size: var(--ip-font-body);
}

.search-button,
.upload-button {
  height: 40px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 7px;
  border: 1px solid var(--ip-primary-600);
  border-radius: var(--ip-radius-sm);
  padding: 0 16px;
  background: var(--ip-primary-600);
  color: var(--ip-neutral-0);
  font-size: var(--ip-font-body);
  line-height: 1;
  font-weight: 600;
  white-space: nowrap;
  cursor: pointer;
  transition:
    background var(--ip-motion-base) var(--ip-motion-ease),
    border-color var(--ip-motion-base) var(--ip-motion-ease),
    box-shadow var(--ip-motion-base) var(--ip-motion-ease),
    transform var(--ip-motion-base) var(--ip-motion-ease);
}

.search-button:hover,
.upload-button:hover {
  background: var(--ip-primary-700);
  border-color: var(--ip-primary-700);
  box-shadow: var(--ip-shadow-md);
  transform: translateY(-1px);
}

.upload-button {
  flex: 0 0 auto;
}

.resource-content {
  min-height: calc(100vh - 150px);
  border: 1px solid var(--ip-neutral-200);
  border-radius: var(--ip-radius-sm);
  overflow: hidden;
  background: var(--ip-neutral-0);
  box-shadow: var(--ip-shadow-md);
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

@media (max-width: 1360px) {
  .resource-grid {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
}

@media (max-width: 980px) {
  .resource-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 760px) {
  .search-box {
    min-width: 0;
    width: 100%;
    grid-template-columns: 1fr;
  }

  .search-button,
  .upload-button {
    width: 100%;
  }

  .resource-grid {
    grid-template-columns: 1fr;
  }
}
</style>

<template>
  <div class="resources">
    <PortalHeader
      v-model="keyword"
      title="资料共享"
      subtitle="按分类聚合内部资料，支持上传共享、预览和下载。"
      search-placeholder="搜索标题、简介或文件名"
      @search="reloadFirst"
    />

    <section class="toolbar">
      <div class="category-tabs">
        <button :class="['tab', { active: categoryCode === 'all' }]" type="button" @click="changeCategory('all')">
          全部资料
          <span>{{ total }}</span>
        </button>
        <button
          v-for="cat in categories"
          :key="cat.categoryId"
          :class="['tab', { active: categoryCode === cat.categoryCode }]"
          type="button"
          @click="changeCategory(cat.categoryCode)"
        >
          {{ cat.categoryName }}
          <span>{{ cat.resourceCount || 0 }}</span>
        </button>
      </div>
      <button v-if="canUpload" class="upload-btn" type="button" @click="openUpload">
        <el-icon><UploadFilled /></el-icon>
        上传资料
      </button>
    </section>

    <section v-loading="loading" class="resource-grid">
      <article v-for="item in resources" :key="item.resourceId" class="resource-card">
        <div class="file-mark" :data-type="item.previewType">
          <el-icon><Document /></el-icon>
        </div>
        <div class="resource-main">
          <div class="resource-head">
            <span class="category">{{ item.categoryName || '未分类' }}</span>
            <span class="suffix">{{ item.fileSuffix || 'file' }}</span>
          </div>
          <h3>{{ item.title }}</h3>
          <p>{{ item.description || item.originalName }}</p>
          <div class="resource-meta">
            <span>{{ formatSize(item.fileSize) }}</span>
            <span>浏览 {{ item.viewCount || 0 }}</span>
            <span>下载 {{ item.downloadCount || 0 }}</span>
          </div>
          <div class="resource-actions">
            <button type="button" @click="showDetail(item.resourceId)">
              <el-icon><View /></el-icon>
              查看
            </button>
            <button type="button" @click="openDownload(item.resourceId)">
              <el-icon><Download /></el-icon>
              下载
            </button>
          </div>
        </div>
      </article>
    </section>

    <el-empty v-if="!loading && resources.length === 0" description="暂无资料" />
    <el-pagination
      v-if="total > pageSize"
      class="pager"
      background
      layout="prev, pager, next"
      :total="total"
      :page-size="pageSize"
      :current-page="pageNum"
      @current-change="onPage"
    />

    <el-drawer v-model="detailVisible" size="520px" title="资料详情">
      <div v-if="current" class="detail">
        <span class="category">{{ current.categoryName || '未分类' }}</span>
        <h2>{{ current.title }}</h2>
        <p>{{ current.description || '暂无简介' }}</p>
        <dl>
          <dt>文件名</dt>
          <dd>{{ current.originalName }}</dd>
          <dt>文件类型</dt>
          <dd>{{ current.fileSuffix || current.mimeType || 'file' }}</dd>
          <dt>文件大小</dt>
          <dd>{{ formatSize(current.fileSize) }}</dd>
          <dt>创建时间</dt>
          <dd>{{ current.createTime || '-' }}</dd>
        </dl>
        <div class="drawer-actions">
          <button type="button" @click="openPreview(current.resourceId)">预览</button>
          <button type="button" @click="openDownload(current.resourceId)">下载</button>
        </div>
      </div>
    </el-drawer>

    <el-dialog v-model="uploadVisible" width="560px" title="上传资料" destroy-on-close>
      <el-form ref="uploadFormRef" :model="uploadForm" :rules="uploadRules" label-width="82px">
        <el-form-item label="标题" prop="title">
          <el-input v-model="uploadForm.title" maxlength="160" placeholder="请输入资料标题" />
        </el-form-item>
        <el-form-item label="分类" prop="categoryId">
          <el-select v-model="uploadForm.categoryId" placeholder="请选择分类" class="full">
            <el-option v-for="cat in categories" :key="cat.categoryId" :label="cat.categoryName" :value="cat.categoryId" />
          </el-select>
        </el-form-item>
        <el-form-item label="简介">
          <el-input v-model="uploadForm.description" type="textarea" :rows="3" maxlength="1000" placeholder="补充资料用途或适用场景" />
        </el-form-item>
        <el-form-item label="文件" prop="file">
          <el-upload class="full" drag :auto-upload="false" :limit="1" :on-change="onFileChange" :on-remove="onFileRemove">
            <el-icon class="el-icon--upload"><UploadFilled /></el-icon>
            <div class="el-upload__text">拖入文件或点击选择</div>
          </el-upload>
        </el-form-item>
      </el-form>
      <template #footer>
        <button class="dialog-cancel" type="button" @click="uploadVisible = false">取消</button>
        <button class="dialog-submit" type="button" :disabled="uploading" @click="submitUpload">{{ uploading ? '上传中' : '发布资料' }}</button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue';
import type { FormInstance, UploadFile } from 'element-plus';
import { ElMessage } from 'element-plus';
import { Document, Download, UploadFilled, View } from '@element-plus/icons-vue';
import PortalHeader from '@/layout/portal/components/PortalHeader.vue';
import { useUserStore } from '@/store/modules/user';
import { getToken } from '@/utils/auth';
import {
  createResource,
  getResource,
  listResourceCategories,
  listResources,
  resourceDownloadUrl,
  resourcePreviewUrl,
  uploadResourceFile
} from '@/api/infoservice/portal';
import { InfoResource, ResourceCategory } from '@/api/infoservice/types';

const userStore = useUserStore();
const categories = ref<ResourceCategory[]>([]);
const resources = ref<InfoResource[]>([]);
const current = ref<InfoResource>();
const keyword = ref('');
const categoryCode = ref('all');
const pageNum = ref(1);
const pageSize = ref(12);
const total = ref(0);
const loading = ref(false);
const detailVisible = ref(false);
const uploadVisible = ref(false);
const uploading = ref(false);
const uploadFormRef = ref<FormInstance>();
const selectedFile = ref<UploadFile>();
const uploadForm = reactive({
  title: '',
  categoryId: undefined as number | undefined,
  description: '',
  file: ''
});

const uploadRules = {
  title: [{ required: true, message: '请输入资料标题', trigger: 'blur' }],
  categoryId: [{ required: true, message: '请选择分类', trigger: 'change' }],
  file: [{ required: true, message: '请选择文件', trigger: 'change' }]
};

const canUpload = computed(() => {
  const ps: string[] = userStore.permissions || [];
  return ps.includes('*:*:*') || (ps.includes('infoservice:resource:upload') && ps.includes('infoservice:resource:add'));
});

const loadCategories = async () => {
  const res: any = await listResourceCategories();
  categories.value = res.data || [];
};

const reload = async () => {
  loading.value = true;
  try {
    const res: any = await listResources({
      categoryCode: categoryCode.value,
      keyword: keyword.value,
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

const changeCategory = (code: string) => {
  categoryCode.value = code;
  reloadFirst();
};

const onPage = (page: number) => {
  pageNum.value = page;
  reload();
};

const showDetail = async (id: number) => {
  const res: any = await getResource(id);
  current.value = res.data;
  detailVisible.value = true;
  reload();
};

const withAuth = (url: string) => {
  const target = new URL(url, window.location.origin);
  const token = getToken();
  if (token) target.searchParams.set('Authorization', token);
  target.searchParams.set('clientid', import.meta.env.VITE_APP_CLIENT_ID);
  return target.toString();
};

const openPreview = (id: number) => {
  window.open(withAuth(resourcePreviewUrl(id)), '_blank');
};

const openDownload = (id: number) => {
  window.open(withAuth(resourceDownloadUrl(id)), '_blank');
};

const openUpload = () => {
  uploadForm.title = '';
  uploadForm.categoryId = categories.value[0]?.categoryId;
  uploadForm.description = '';
  uploadForm.file = '';
  selectedFile.value = undefined;
  uploadVisible.value = true;
};

const onFileChange = (file: UploadFile) => {
  selectedFile.value = file;
  uploadForm.file = file.name;
  if (!uploadForm.title) {
    uploadForm.title = file.name.replace(/\.[^.]+$/, '');
  }
  uploadFormRef.value?.validateField('file');
};

const onFileRemove = () => {
  selectedFile.value = undefined;
  uploadForm.file = '';
};

const submitUpload = async () => {
  await uploadFormRef.value?.validate();
  if (!selectedFile.value?.raw) {
    ElMessage.warning('请选择文件');
    return;
  }
  uploading.value = true;
  try {
    const formData = new FormData();
    formData.append('file', selectedFile.value.raw);
    const upRes: any = await uploadResourceFile(formData);
    const file = upRes.data;
    await createResource({
      title: uploadForm.title,
      categoryId: uploadForm.categoryId,
      description: uploadForm.description,
      ossId: file.ossId,
      originalName: file.originalName,
      fileSuffix: file.fileSuffix,
      mimeType: file.mimeType,
      fileSize: file.fileSize,
      previewType: file.previewType,
      status: '0'
    });
    ElMessage.success('资料已发布');
    uploadVisible.value = false;
    await loadCategories();
    reloadFirst();
  } finally {
    uploading.value = false;
  }
};

const formatSize = (size?: number) => {
  if (!size) return '-';
  if (size < 1024) return `${size} B`;
  if (size < 1024 * 1024) return `${(size / 1024).toFixed(1)} KB`;
  return `${(size / 1024 / 1024).toFixed(1)} MB`;
};

onMounted(async () => {
  await loadCategories();
  await reload();
});
</script>

<style scoped>
.resources {
  min-height: 100vh;
}

.toolbar {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  padding: 0 40px 18px;
}

.category-tabs {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.tab {
  height: 38px;
  display: inline-flex;
  align-items: center;
  gap: 8px;
  border: 1px solid #d7dee8;
  border-radius: 8px;
  padding: 0 13px;
  background: #fff;
  color: #243047;
  font-weight: 850;
  cursor: pointer;
}

.tab span {
  color: #7a8799;
  font-size: 12px;
}

.tab.active {
  border-color: #c98b2e;
  background: #fff4de;
  color: #7a4c13;
}

.upload-btn {
  height: 38px;
  display: inline-flex;
  align-items: center;
  gap: 8px;
  border: 0;
  border-radius: 8px;
  padding: 0 14px;
  background: #17304e;
  color: #fff;
  font-weight: 900;
  cursor: pointer;
}

.resource-grid {
  min-height: 480px;
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 16px;
  padding: 0 40px 24px;
}

.resource-card {
  min-height: 214px;
  display: flex;
  gap: 16px;
  padding: 20px;
  border: 1px solid #dde4ec;
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.92);
  box-shadow: 0 12px 28px rgba(32, 46, 69, 0.06);
}

.file-mark {
  width: 52px;
  height: 52px;
  flex: 0 0 52px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 8px;
  background: #e7f1ee;
  color: #287d76;
  font-size: 26px;
}

.file-mark[data-type='pdf'] {
  background: #fff0e8;
  color: #a44d2d;
}

.file-mark[data-type='image'] {
  background: #fff4de;
  color: #c98b2e;
}

.resource-main {
  min-width: 0;
  flex: 1;
}

.resource-head,
.resource-meta,
.resource-actions {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.category,
.suffix {
  height: 24px;
  display: inline-flex;
  align-items: center;
  border-radius: 6px;
  padding: 0 8px;
  font-size: 12px;
  font-weight: 850;
}

.category {
  background: #fff4de;
  color: #8a5d1d;
}

.suffix {
  background: #eef3f6;
  color: #516074;
}

.resource-card h3 {
  margin: 12px 0 8px;
  overflow: hidden;
  color: #172033;
  font-size: 19px;
  line-height: 1.25;
  font-weight: 950;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.resource-card p {
  min-height: 44px;
  margin: 0;
  display: -webkit-box;
  overflow: hidden;
  color: #5c6879;
  font-size: 14px;
  line-height: 1.55;
  font-weight: 650;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.resource-meta {
  margin-top: 12px;
  color: #758195;
  font-size: 12px;
  font-weight: 800;
}

.resource-actions {
  margin-top: 14px;
}

.resource-actions button {
  height: 32px;
  display: inline-flex;
  align-items: center;
  gap: 6px;
  border: 1px solid #d9e0e8;
  border-radius: 7px;
  padding: 0 10px;
  background: #fff;
  color: #223047;
  font-weight: 850;
  cursor: pointer;
}

.resource-actions button:hover {
  border-color: #c98b2e;
  color: #7a4c13;
}

.pager {
  display: flex;
  justify-content: center;
  padding-bottom: 32px;
}

.detail h2 {
  margin: 16px 0 10px;
  color: #172033;
  font-size: 24px;
  font-weight: 950;
}

.detail p {
  color: #5d6878;
  line-height: 1.7;
  font-weight: 650;
}

.detail dl {
  display: grid;
  grid-template-columns: 86px 1fr;
  gap: 12px 10px;
  margin: 22px 0;
}

.detail dt {
  color: #7b8798;
  font-weight: 850;
}

.detail dd {
  min-width: 0;
  margin: 0;
  color: #1f2d43;
  word-break: break-all;
  font-weight: 750;
}

.drawer-actions {
  display: flex;
  gap: 10px;
}

.drawer-actions button,
.dialog-submit,
.dialog-cancel {
  height: 38px;
  border-radius: 8px;
  padding: 0 14px;
  font-weight: 900;
  cursor: pointer;
}

.drawer-actions button,
.dialog-submit {
  border: 0;
  background: #17304e;
  color: #fff;
}

.dialog-cancel {
  border: 1px solid #d9e0e8;
  background: #fff;
  color: #223047;
}

.dialog-submit:disabled {
  opacity: 0.66;
  cursor: default;
}

.full {
  width: 100%;
}

@media (max-width: 1380px) {
  .resource-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 860px) {
  .toolbar {
    flex-direction: column;
  }

  .resource-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 760px) {
  .toolbar,
  .resource-grid {
    padding-inline: 18px;
  }
}
</style>
